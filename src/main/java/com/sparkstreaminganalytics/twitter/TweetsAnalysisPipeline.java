package com.sparkstreaminganalytics.twitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

// Twitter library
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.conf.ConfigurationBuilder;

// Spark
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.elasticsearch.spark.streaming.api.java.JavaEsSparkStreaming;
// Rich JSON manipulation library.
import org.json.JSONObject;

// Google API for serialisation and manipulation JSON objects.
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 * @author Veselin Kirev
 */
public class TweetsAnalysisPipeline {
	private static int nonEnglishTweets;
	private static boolean statusIsSpam;
	private static int totalStatusesPassedEnSpamFilter;

	// Gson API is a Google API used for conversion Java and JSON objects.
	// Use this if you want to get location=null as well or other nulls
	// private static Gson gson = new GsonBuilder().serializeNulls().create();
	private static Gson gson = new GsonBuilder().setExclusionStrategies(new TweetSerializationExclusionStrategy()).create();

	/**
	 * @param args
	 *            Contain the keyword for the campaign to be analysed.
	 */
	public static void main(String[] args) throws IOException, TwitterException {
		if (args.length != 1) {
			throw new IllegalArgumentException("Invalid input. Please enter exactly 1 keyword.");
		}
		// Using non-default cb to create Twitter object, where JSON store is
		// enabled.
		// Otherwise Status object cannot be converted to raw JSON.
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setJSONStoreEnabled(true);
		
		// Clean the index before starting the app.
		cleanElasticsearchIndex();

		// Initialize the NLP
		NLP.init();

		// Keyword obtained from user input.
		String keywords[] = { args[0] };
		
		// Create a Spark Stream Receiver to start receiving tweets.
		TweetStreamSparkReceiver tweetsReceiver = new TweetStreamSparkReceiver("TwitterAnalyzer", "local[2]", keywords);

		// Filter: get only tweets in English.
		JavaDStream<Status> englishStatuses = tweetsReceiver.getStream().filter(new Function<Status, Boolean>() {
			@Override
			public Boolean call(Status status) {
				// Collect statistics, before filtering.
				if (!status.getLang().equals("en")) {
					nonEnglishTweets++;
				}

				statusIsSpam = SpamFilter.isSpam(status);

				System.out.println("\n************************************\n");
				System.out.println("English filter statistics: ");
				System.out.println("Filtered non-english Tweets:" + nonEnglishTweets);

				// Filter the non-english statuses.
				if (!statusIsSpam) {
					totalStatusesPassedEnSpamFilter++;
				}

				System.out.println("\n************************************\n");
				System.out.println("Total messages passed the EN & SPAM filter: " + totalStatusesPassedEnSpamFilter);

				return status.getLang().equals("en") && !statusIsSpam;
			}
		});

		JavaDStream<String> processedTweets = englishStatuses.flatMap(new FlatMapFunction<Status, String>() {
			// Handle this exception later on instead of throwing it.
			@Override
			public Iterator<String> call(Status status) throws TwitterException {
				// Serialize the Status object into JSON object.
				String statusJSONString = gson.toJson(status);
				
				// Format the date into one that Elasticsearch accepts.
				JSONObject TweetStatusJsonObject = ElasticDateFormatter.getFormattedDate(statusJSONString);
				
				// Filter out not sensible locations and format others appropriately.
				TweetStatusJsonObject = LocationFilter.filterTweetUserLocations(status, TweetStatusJsonObject);
				
				// Add the sentiment to the Tweet JSON object
				TweetStatusJsonObject.put("sentiment", NLP.scoreToString(SentimentAnomalyDetector.getSentimentWithoutAnomalies(TweetStatusJsonObject.getString("text"))));

				// Printing stats about locations and number of unknown sentiments.
				System.out.println("Statuses with sensible locations: " + LocationFilter.sensibleLocations + " out of "
						+ totalStatusesPassedEnSpamFilter);
				System.out.println("Statuses with unknown sentiments: " + NLP.numberOfUnknowns);
				
				// Printing stats about sentiment difference.
				if (NLP.numberOfUnknowns > 0) {
					for (int notClTweetSentScoreIndex = 0; notClTweetSentScoreIndex < 5; notClTweetSentScoreIndex++) {
						for (int clTweetSentScoreIndex = 0; clTweetSentScoreIndex < 5; clTweetSentScoreIndex++) {
							System.out.println(NLP.scoreToString(notClTweetSentScoreIndex) + " -> "
									+ NLP.scoreToString(clTweetSentScoreIndex) + " : "
									+ SentimentAnomalyDetector.sentimentDifferenceMap.get(notClTweetSentScoreIndex)[clTweetSentScoreIndex]);
						}
					}
				}
				
				// Update the number of detected anomalies result of contraversial sentiments.
				SentimentAnomalyDetector.updateAnomaliesCount(TweetStatusJsonObject);
				
				// Print the total anomalies stats.
				System.out.println("\n************************************\n");
				System.out.println("Total number of anomalies detected: " + SentimentAnomalyDetector.anomaliesDetected);

				// Update approximate total engagement accumulated so far.
				TwitterMetrics.updateApproximateTotalEngagement(status, TweetStatusJsonObject, keywords[0]);
				
				
				// Put the new fields into the JSON Tweet object to be put into Elasticsearch
				TweetStatusJsonObject.put("approximateTotalEngagement", TwitterMetrics.getApproximateTotalEngagement());
				TweetStatusJsonObject.put("approximateRetweetResponseTime", TwitterMetrics.getAverageRTResponseTime(status));
				
				System.out.println("\n************************************\n");
				System.out.println("Approximate total engagement: " + TwitterMetrics.getApproximateTotalEngagement());
				
				System.out.println("\n************************************\n");
				System.out.println("Approximate RT Response Time: " + TwitterMetrics.getAverageRTResponseTime(status));
				
				System.out.println("####################################\n");
				
				
				TwitterMetrics.resetRTValidator();
				return Arrays.asList(TweetStatusJsonObject.toString() + "\n").iterator();
			}
		});

		JavaEsSparkStreaming.saveJsonToEs(processedTweets, "tweets/tweet");
		//processedTweets.print();

		tweetsReceiver.getJavaStreamingContext().start();

		try {
			tweetsReceiver.getJavaStreamingContext().awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	// Function to clean the existing Elasticsearch index, in order to be able to start
	// a clean real-time analysis session.
	private static void cleanElasticsearchIndex(){
		// Delete all documents from the elasticsearch index "tweets" of type
		// "tweet".
		// TODO: Find a way to verify that elasticsearch is actually run.
		ProcessBuilder p = new ProcessBuilder("curl", "-XPOST",
				"localhost:9200/tweets/tweet/_delete_by_query?conflicts=proceed&pretty", "-H",
				"Content-Type: application/json", "Accept: application/json", "-d",
				"{ \"query\": { \"match_all\": {} } }");
		try {
			final Process shell = p.start();
		} catch (IOException e) {
			System.err.println(
					"Something went wrong when trying to delete all documents from ES index. Start the program again.");
			e.printStackTrace();
			System.exit(0);
		}
	}
}
