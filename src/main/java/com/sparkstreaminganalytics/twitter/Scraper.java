package com.sparkstreaminganalytics.twitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

// Twitter library
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.conf.ConfigurationBuilder;

// Spark
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.twitter.TwitterUtils;
import org.elasticsearch.spark.streaming.api.java.JavaEsSparkStreaming;
import org.json.JSONObject;

// Google API for manipulation JSON objects.
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 * @author Veselin Kirev
 */
public class Scraper {
	private static final String appName = "TwitterAnalyzer";
	private static final String master = "local[2]";
	private static int nonEnglishTweets;
	private static boolean statusIsSpam;
	private static int totalStatusesPassedEnSpamFilter;
	private static int anomaliesDetected;
	private static HashSet<Long> retweetIdsSet = new HashSet<Long>();
	private static HashMap<Long, Long[]> uniqueRetweedIdToFavAndRetCountMap = new HashMap<Long, Long[]>();
	private static long approximateTotalEngagement;

	private static Locale[] getLocaleObjects() {
		String[] localeStrings = Locale.getISOCountries();
		Locale[] localesObj = new Locale[localeStrings.length];
		for (int i = 0; i < localeStrings.length; i++) {
			localesObj[i] = new Locale("", localeStrings[i]);
		}
		return localesObj;
	}

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

		// Initialize the NLP
		NLP.init();

		// Keyword obtained from user input.
		String keywords[] = { args[0] };

		// Spark configuration
		SparkConf conf = new SparkConf().setAppName(appName).setMaster(master);
		// conf.set("es.index.auto.create", "true");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		// Configure log level here. For debugging comment out this code.
		jsc.setLogLevel("OFF");
		JavaStreamingContext jssc = new JavaStreamingContext(jsc, Durations.seconds(10));

		JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(jssc, keywords);

		// Filter: get only tweets in English.
		JavaDStream<Status> englishStatuses = stream.filter(new Function<Status, Boolean>() {
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
				JSONObject jsonObj = ElasticDateFormatter.getFormattedDate(statusJSONString);
				
				// Filter out not sensible locations and format others appropriately.
				jsonObj = LocationFilter.filterTweetUserLocations(status, jsonObj);
				
				// Add the sentiment to the Tweet JSON object
				jsonObj.put("sentiment", NLP.scoreToString(NLP.findUsefulSentiment(jsonObj.getString("text"))));

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
									+ NLP.sentimentDifferenceMap.get(notClTweetSentScoreIndex)[clTweetSentScoreIndex]);
						}
					}
				}

				// 5 is the Unknown score.
				// Anomaly detection in the sentiment score.
				if (NLP.latestSentimentScore == 5) {
					if ((NLP.latestSentimentDifference[0] == 0 && NLP.latestSentimentDifference[1] == 3)
							|| (NLP.latestSentimentDifference[0] == 0 && NLP.latestSentimentDifference[1] == 4)
							|| (NLP.latestSentimentDifference[0] == 1 && NLP.latestSentimentDifference[1] == 3)
							|| (NLP.latestSentimentDifference[0] == 1 && NLP.latestSentimentDifference[1] == 4)
							|| (NLP.latestSentimentDifference[0] == 3 && NLP.latestSentimentDifference[1] == 0)
							|| (NLP.latestSentimentDifference[0] == 3 && NLP.latestSentimentDifference[1] == 1)
							|| (NLP.latestSentimentDifference[0] == 4 && NLP.latestSentimentDifference[1] == 0)
							|| (NLP.latestSentimentDifference[0] == 4 && NLP.latestSentimentDifference[1] == 1)) {

						anomaliesDetected++;

						System.out.println("\n************************************\n");
						System.out.println("Anomaly detected, possible cause: ");
						System.out
								.println("Cleaned Mention: " + jsonObj.getJSONArray("userMentionEntities").toString());
						System.out.println("Cleaned URL: " + jsonObj.getJSONArray("urlEntities").toString());
					}
				}

				System.out.println("\n************************************\n");
				System.out.println("Total number of anomalies detected: " + anomaliesDetected);

				// if(jsonObj.getJSONArray("userMentionEntities").toString().contains(keywords[0]))
				if ((status.getRetweetedStatus() != null) && (jsonObj.getJSONObject("retweetedStatus")
						.getJSONArray("userMentionEntities").toString().contains(keywords[0])
						|| jsonObj.getJSONArray("urlEntities").toString().contains(keywords[0]))) {

					if (retweetIdsSet.add(jsonObj.getJSONObject("retweetedStatus").getLong("id"))) {

						// Add it to the map where the update will happen later on.
						uniqueRetweedIdToFavAndRetCountMap.put(jsonObj.getJSONObject("retweetedStatus").getLong("id"),
								new Long[2]);

						uniqueRetweedIdToFavAndRetCountMap
								.get(jsonObj.getJSONObject("retweetedStatus").getLong("id"))[0] = new Long(
										jsonObj.getJSONObject("retweetedStatus").getInt("favoriteCount"));

						uniqueRetweedIdToFavAndRetCountMap
								.get(jsonObj.getJSONObject("retweetedStatus").getLong("id"))[1] = new Long(
										jsonObj.getJSONObject("retweetedStatus").getInt("retweetCount"));

						// 2 comes from the fact that the keyword is mentioned
						// both in the tweet and in the retweet.
						approximateTotalEngagement += 2
								+ jsonObj.getJSONObject("retweetedStatus").getInt("favoriteCount")
								+ jsonObj.getJSONObject("retweetedStatus").getInt("retweetCount");

					} else {
						// Add the to the engagement only the difference of retweet and favorite counts
						// since last time. E.g. treat it as update, instead of just adding it.
						approximateTotalEngagement += 2
								+ jsonObj.getJSONObject("retweetedStatus").getInt("favoriteCount")
								+ jsonObj.getJSONObject("retweetedStatus").getInt("retweetCount")
								- uniqueRetweedIdToFavAndRetCountMap
										.get(jsonObj.getJSONObject("retweetedStatus").getLong("id"))[0]
								- uniqueRetweedIdToFavAndRetCountMap
										.get(jsonObj.getJSONObject("retweetedStatus").getLong("id"))[1];
						
						// Then update the latest retweet and favorite counts information.
						uniqueRetweedIdToFavAndRetCountMap
								.get(jsonObj.getJSONObject("retweetedStatus").getLong("id"))[0] = new Long(
										jsonObj.getJSONObject("retweetedStatus").getInt("favoriteCount"));

						uniqueRetweedIdToFavAndRetCountMap
								.get(jsonObj.getJSONObject("retweetedStatus").getLong("id"))[1] = new Long(
										jsonObj.getJSONObject("retweetedStatus").getInt("retweetCount"));

					}
				} else {
					approximateTotalEngagement++;
				}

				System.out.println("\n************************************\n");
				System.out.println("Approximate total engagement: " + approximateTotalEngagement);
				System.out.println("####################################\n");
				return Arrays.asList(jsonObj.toString() + "\n").iterator();
			}
		});

		//JavaEsSparkStreaming.saveJsonToEs(processedTweets, "tweets/tweet");
		processedTweets.print();

		jssc.start();

		try {
			jssc.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
