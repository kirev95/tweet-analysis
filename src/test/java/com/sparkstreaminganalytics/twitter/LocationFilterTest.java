package com.sparkstreaminganalytics.twitter;

import org.json.JSONObject;

import junit.framework.TestCase;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

public class LocationFilterTest extends TestCase {
	
	public LocationFilterTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}
	
	// UK is specific case as in Java locale is it GB instead.
	// So we test if the correct replacement before matching to country name
	// is taking place.
	public void testLocationFilter() throws TwitterException{
		String rawJSON = "{\"user\":{\"location\":\"UK\"}}";
		Status status = TwitterObjectFactory.createStatus(rawJSON);
		JSONObject jsonObj = new JSONObject(rawJSON);
		JSONObject filtered = LocationFilter.filterTweetUserLocations(status, jsonObj);
		assertEquals("United Kingdom", filtered.getJSONObject("user").get("location"));
	}
	
	// Some other country without the special case like above, e.g. USA.
	public void testUSA() throws TwitterException{
		String rawJSON = "{\"user\":{\"location\":\"USA\"}}";
		Status status = TwitterObjectFactory.createStatus(rawJSON);
		JSONObject jsonObj = new JSONObject(rawJSON);
		JSONObject filtered = LocationFilter.filterTweetUserLocations(status, jsonObj);
		assertEquals("United States", filtered.getJSONObject("user").get("location"));
	}
	
	// Some location part of sentence.
	public void testUSASentence() throws TwitterException{
		String rawJSON = "{\"user\":{\"location\":\"New York, USA\"}}";
		Status status = TwitterObjectFactory.createStatus(rawJSON);
		JSONObject jsonObj = new JSONObject(rawJSON);
		JSONObject filtered = LocationFilter.filterTweetUserLocations(status, jsonObj);
		assertEquals("United States", filtered.getJSONObject("user").get("location"));
	}
	
	// Some unexisting location.
	public void testNotExistingLocation() throws TwitterException{
		String rawJSON = "{\"user\":{\"location\":\"I won\'t tell you.\"}}";
		Status status = TwitterObjectFactory.createStatus(rawJSON);
		JSONObject jsonObj = new JSONObject(rawJSON);
		JSONObject filtered = LocationFilter.filterTweetUserLocations(status, jsonObj);
		assertEquals("{}", filtered.getJSONObject("user").toString());
	}
}
