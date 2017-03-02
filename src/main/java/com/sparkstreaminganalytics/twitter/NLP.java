package com.sparkstreaminganalytics.twitter;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

/*Pipeline idea from: 
"Manning, Christopher D., Mihai Surdeanu, John Bauer, Jenny Finkel, Steven J. Bethard, and David McClosky. 2014.
The Stanford CoreNLP Natural Language Processing Toolkit In Proceedings of the 52nd Annual Meeting of
the Association for Computational Linguistics: System Demonstrations, pp. 55-60." */

// Extended code from: http://rahular.com/twitter-sentiment-analysis/

public class NLP {
	static StanfordCoreNLP pipeline;
	
	// Initialize the pipeline, using the properties defined earlier.
	public static void init() {
		pipeline = new StanfordCoreNLP("NLPProps.properties");
	}

	// Function to find sentiment of a tweet.
	public static String findSentiment(String tweet) {
		
		int mainSentiment = 0;
		if (tweet != null && tweet.length() > 0) {
			int longest = 0;
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