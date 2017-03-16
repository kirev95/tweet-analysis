package com.sparkstreaminganalytics.twitter;

import java.util.HashMap;

import org.json.JSONObject;

abstract public class SentimentAnomalyDetector {
	static HashMap<Integer, Integer[]> sentimentDifferenceMap = new HashMap<Integer, Integer[]>();
	static int[] latestSentimentDifference = new int[2];
	static int latestSentimentScore = -1;
	static int anomaliesDetected = 0;
	
	// Clean the tweet data to enhance the efficiency of the sentiment analysis.
	private static String getCleanedTextTweet(String originalTweet){
		String cleanTweet = originalTweet.replaceAll("\n", "")
				  // Remove Retweets
			      .replaceAll("RT\\s+", "")
			      // Remove Mentions which follow a word
			      .replaceAll("\\s+@\\w+", "")
			      // Remove Mentions at the begining
			      .replaceAll("@\\w+", "")
			      // Remove URL
			      .replaceAll("((www\\.[^\\s]+)|(https?://[^\\s]+))", "")
			      // Remove other useless symbols, inlcuding the # from the hashtag
				  .replaceAll("[*#@<>]", "");
		
		return cleanTweet;
	}

	public static int getSentimentWithoutAnomalies(String tweet){
		int notCleanedTextSentiment = NLP.findSentiment(tweet);
		int cleanedTextSentiment = NLP.findSentiment(getCleanedTextTweet(tweet));
		
		calculateSentimentDifference(cleanedTextSentiment, notCleanedTextSentiment);
		
		if(cleanedTextSentiment == notCleanedTextSentiment){
			return cleanedTextSentiment;
		}
		else{
			latestSentimentDifference[0] = notCleanedTextSentiment;
			latestSentimentDifference[1] = cleanedTextSentiment;
			latestSentimentScore = 5;
			NLP.numberOfUnknowns++;
			// Five(5) is the score for Unknown
			return 5;
		}
	}
	
	public static void updateAnomaliesCount(JSONObject jsonObj){
		// Anomaly detection in the sentiment score.
		// The numbers reflect the sentiment score obtained from the findSentiment function
		// based on the NLP Stanford sentiment pipeline. Look NLP Stanford documentation
		// for more details. 5 is the Unknown score, and is an extention.
		if (latestSentimentScore == 5) {
			if ((latestSentimentDifference[0] == 0 && latestSentimentDifference[1] == 3)
					|| (latestSentimentDifference[0] == 0 && latestSentimentDifference[1] == 4)
					|| (latestSentimentDifference[0] == 1 && latestSentimentDifference[1] == 3)
					|| (latestSentimentDifference[0] == 1 && latestSentimentDifference[1] == 4)
					|| (latestSentimentDifference[0] == 3 && latestSentimentDifference[1] == 0)
					|| (latestSentimentDifference[0] == 3 && latestSentimentDifference[1] == 1)
					|| (latestSentimentDifference[0] == 4 && latestSentimentDifference[1] == 0)
					|| (latestSentimentDifference[0] == 4 && latestSentimentDifference[1] == 1)) {
	
				anomaliesDetected++;
				
				System.out.println("\n************************************\n");
				System.out.println("Anomaly detected, possible cause: ");
				System.out
						.println("Cleaned Mention: " + jsonObj.getJSONArray("userMentionEntities").toString());
				System.out.println("Cleaned URL: " + jsonObj.getJSONArray("urlEntities").toString());
			}
		}
	}
	
	public static void calculateSentimentDifference(int cleanedTextSentiment, int notCleanedTextSentiment){
		if(sentimentDifferenceMap.containsKey(notCleanedTextSentiment)){
			sentimentDifferenceMap.get(notCleanedTextSentiment)[cleanedTextSentiment]++;
		}
	}
}
