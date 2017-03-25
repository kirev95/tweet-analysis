package com.sparkstreaminganalytics.twitter;

import junit.framework.TestCase;

public class NLPTest extends TestCase {
	
	protected void setUp() throws Exception {
		super.setUp();
		NLP.init();
	}
	
	public void testVeryNegativeSentiment(){
		assertEquals(0, NLP.findSentiment("This is very disappointing and I am deeply frustrated."));
	}
	
	public void testNegativeSentiment(){
		assertEquals(1, NLP.findSentiment("This is disappointing."));
	}
	
	public void testNeutralSentiment(){
		assertEquals(2, NLP.findSentiment("I am here."));
	}
	
	public void testPositiveSentiment(){
		assertEquals(3, NLP.findSentiment("I feel good."));
	}
	
	public void testVeryPositiveSentiment(){
		assertEquals(4, NLP.findSentiment("I feel incredibly awesome and I am really excited!"));
	}
}
