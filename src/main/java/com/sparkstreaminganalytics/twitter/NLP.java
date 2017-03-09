package com.sparkstreaminganalytics.twitter;

import java.util.HashMap;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

public class NLP {
	static StanfordCoreNLP pipeline;
	static Properties properties;
	static HashMap<Integer, Integer[]> sentimentDifferenceMap = new HashMap<Integer, Integer[]>();
	static int[] latestSentimentDifference = new int[2];
	static int latestSentimentScore = -1;
	
	public static int numberOfUnknowns;
	
	// Initialize the pipeline, using the properties defined earlier.
	public static void init() {
		properties = new Properties();
		properties.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
		pipeline = new StanfordCoreNLP(properties);
		
		for(int i=0; i<5; i++){
			sentimentDifferenceMap.put(i, new Integer[5]);
		}
		
		for(int notClTweetSentScoreIndex=0; notClTweetSentScoreIndex<5; notClTweetSentScoreIndex++){
			for(int clTweetSentScoreIndex=0; clTweetSentScoreIndex<5; clTweetSentScoreIndex++){
				sentimentDifferenceMap.get(notClTweetSentScoreIndex)[clTweetSentScoreIndex] = new Integer(0);
			}
		}
	}
	
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

	public static int findUsefulSentiment(String tweet){
		int notCleanedTextSentiment = findSentiment(tweet);
		int cleanedTextSentiment = findSentiment(getCleanedTextTweet(tweet));
		
		calculateSentimentDifference(cleanedTextSentiment, notCleanedTextSentiment);
		
		if(cleanedTextSentiment == notCleanedTextSentiment){
			return cleanedTextSentiment;
		}
		else{
			latestSentimentDifference[0] = notCleanedTextSentiment;
			latestSentimentDifference[1] = cleanedTextSentiment;
			latestSentimentScore = 5;
			numberOfUnknowns++;
			// Five(5) is the score for Unknown
			return 5;
		}
	}
	
	/*
	Manning, Christopher D., Mihai Surdeanu, John Bauer, Jenny Finkel, Steven J. Bethard, and David McClosky. 2014.
	The Stanford CoreNLP Natural Language Processing Toolkit In Proceedings of the 52nd Annual Meeting of
	the Association for Computational Linguistics: System Demonstrations, pp. 55-60.
	 */
	public static int findSentiment(String tweet) {
		int mainSentiment = 0;
		if (tweet != null && tweet.length() > 0) {
			int longest = 0;
			// Annotation are Data structures holding the results of Annotators, basically maps.
			Annotation annotation = pipeline.process(tweet);
			for (CoreMap sentence : annotation
					.get(CoreAnnotations.SentencesAnnotation.class)) {
				Tree tree = sentence
						.get(SentimentAnnotatedTree.class);
				int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
				String partText = sentence.toString();
				if (partText.length() > longest) {
					mainSentiment = sentiment;
					longest = partText.length();
				}
			}
		}
		
		return mainSentiment;
	}
	
	public static String scoreToString(int sentimentScore){
		//Obtain a human readable form of the sentiment score.
		switch (sentimentScore) {
        case 0:
            return "Very Negative";
        case 1:
            return "Negative";
        case 2:
            return "Neutral";
        case 3:
            return "Positive";
        case 4:
            return "Very Positive";
        case 5:
            return "Unknown";
        default:
            return "";
        }
	}
	
	public static void calculateSentimentDifference(int cleanedTextSentiment, int notCleanedTextSentiment){
		if(sentimentDifferenceMap.containsKey(notCleanedTextSentiment)){
			sentimentDifferenceMap.get(notCleanedTextSentiment)[cleanedTextSentiment]++;
		}
	}
}