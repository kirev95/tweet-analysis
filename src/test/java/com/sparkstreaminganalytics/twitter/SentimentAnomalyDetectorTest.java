package com.sparkstreaminganalytics.twitter;

import junit.framework.TestCase;

public class SentimentAnomalyDetectorTest extends TestCase {
	String tweetText;
	public SentimentAnomalyDetectorTest(String name) {
		super(name);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		NLP.init();
		tweetText = "RT @Someone Me **<proofreading>** my #nice tweet before I send it out! https://t.co/Gc9HDXab4r";
	}

	public void testTweetCleaning(){
		//String tweetText = "RT @Someone Me **<proofreading>** my #nice tweet before I send it out! https://t.co/Gc9HDXab4r";
		assertEquals(" Me proofreading my nice tweet before I send it out! ", SentimentAnomalyDetector.getCleanedTextTweet(tweetText));
	}
	
	public void testUnknownSentiment(){
		assertEquals("Unknown", NLP.scoreToString(SentimentAnomalyDetector.getSentimentWithoutAnomalies(tweetText)));
	}
}
