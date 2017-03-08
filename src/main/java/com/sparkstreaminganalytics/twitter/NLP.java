package com.sparkstreaminganalytics.twitter;

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
	
	public static int numberOfUnknowns;
	
	// Initialize the pipeline, using the properties defined earlier.
	public static void init() {
		properties = new Properties();
		properties.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
		pipeline = new StanfordCoreNLP(properties);
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

	public static String findUsefulSentiment(String tweet){
		String notCleanedTextSentiment = findSentiment(tweet);
		String cleanedTextSentiment = findSentiment(getCleanedTextTweet(tweet));
		
		if(cleanedTextSentiment.equals(notCleanedTextSentiment)){
			return cleanedTextSentiment;
		}
		else{
			numberOfUnknowns++;
			return "Unknown";
		}
	}
	
	/*
	Manning, Christopher D., Mihai Surdeanu, John Bauer, Jenny Finkel, Steven J. Bethard, and David McClosky. 2014.
	The Stanford CoreNLP Natural Language Processing Toolkit In Proceedings of the 52nd Annual Meeting of
	the Association for Computational Linguistics: System Demonstrations, pp. 55-60.
	 */
	public static String findSentiment(String tweet) {
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
		
		// Obtain a human readable form of the sentiment score.
		switch (mainSentiment) {
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
        default:
            return "";
        }
	}
}