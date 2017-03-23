package com.sparkstreaminganalytics.twitter;

import junit.framework.TestCase;

public class ElasticDateFormatterTest extends TestCase {
	String originalFormat;
	String requiredFormat;
	
	
	public ElasticDateFormatterTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		this.originalFormat =  "{\"createdAt\":\"Mar 22, 2017 6:32:54 AM\"}";
		this.requiredFormat = "20170322T063254+0000";
	}
	
   public void testDateConversion() {
      assertEquals((ElasticDateFormatter.getFormattedDate(originalFormat)).getString("createdAt"), requiredFormat);
   }

}
