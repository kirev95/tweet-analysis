package com.sparkstreaminganalytics.twitter;

import junit.framework.TestCase;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

public class SpamFilterTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testNoFeatures() throws TwitterException{
		String rawJSON = "{\"created_at\": \"Wed Jan 23 06:01:13 +0000 2017+\",\"source\":'<a href=\"http://twitter.com/download/iphone\"',\"displayTextRangeEnd\": 79,\"sentiment\": \"Negative\",\"hashtagEntities\": [],\"userMentionEntities\": [],\"urlEntities\": [{\"displayURL\": \"nytimes.com/2017/03/14/us/\u2026\",\"start\": 56,\"end\": 79,\"expandedURL\": \"http://www.nytimes.com/2017/03/14/us/politics/donald-trump-taxes.html?partner=rss&emc=rss&CpYz8\",\"url\": \"https://t.co/GozOKhh7pn\"}],\"id\": 842914405948014592,\"text\": \"Trump Wrote Off $100 Million in Business Losses in 2005 https://t.co/GozOKhh7pn https://t.co/Uo263oR400\",\"user\":{\"created_at\": \"Wed May 23 06:01:13 +0000 2007+\",\"description\":\"The Real Twitter API. I tweet about API changes. Don't get an answer? It's on my website.\",\"id\": 2661092896,\"statuses_count\": 8646,\"favouritesCount\": 5747,\"friendsCount\": 195,\"isDefaultProfileImage\": false,\"followersCount\": 79}}";
		Status status = TwitterObjectFactory.createStatus(rawJSON);
		assertEquals(false, SpamFilter.isSpam(status));
	}
	
	public void testOneLowerWeightedFeature() throws TwitterException{
		String rawJSON = "{\"created_at\": \"Wed Jan 23 06:01:13 +0000 2017+\",\"source\":'<a href=\"http://twitter.com/download/iphone\"',\"displayTextRangeEnd\": 79,\"sentiment\": \"Negative\",\"hashtagEntities\": [],\"userMentionEntities\": [],\"urlEntities\": [{\"displayURL\": \"nytimes.com/2017/03/14/us/\u2026\",\"start\": 56,\"end\": 79,\"expandedURL\": \"http://www.nytimes.com/2017/03/14/us/politics/donald-trump-taxes.html?partner=rss&emc=rss&CpYz8\",\"url\": \"https://t.co/GozOKhh7pn\"}],\"id\": 842914405948014592,\"text\": \"Trump Wrote Off $100 Million in Business Losses in 2005 https://t.co/GozOKhh7pn https://t.co/Uo263oR400\",\"user\":{\"created_at\": \"Wed May 23 06:01:13 +0000 2007+\",\"id\": 2661092896,\"statuses_count\": 8646,\"favouritesCount\": 5747,\"friendsCount\": 195,\"isDefaultProfileImage\": false,\"followersCount\": 79}}";
		Status status = TwitterObjectFactory.createStatus(rawJSON);
		assertEquals(false, SpamFilter.isSpam(status));
	}
	
	public void testTwoLowerWeightedFeatures() throws TwitterException{
		String rawJSON = "{\"created_at\": \"Wed Mar 23 06:01:13 +0000 2017\",\"source\":'<a href=\"http://twitter.com/download/iphone\"',\"displayTextRangeEnd\": 79,\"sentiment\": \"Negative\",\"hashtagEntities\": [],\"userMentionEntities\": [],\"urlEntities\": [{\"displayURL\": \"nytimes.com/2017/03/14/us/\u2026\",\"start\": 56,\"end\": 79,\"expandedURL\": \"http://www.nytimes.com/2017/03/14/us/politics/donald-trump-taxes.html?partner=rss&emc=rss&CpYz8\",\"url\": \"https://t.co/GozOKhh7pn\"}],\"id\": 842914405948014592,\"text\": \"Trump Wrote Off $100 Million in Business Losses in 2005 https://t.co/GozOKhh7pn https://t.co/Uo263oR400\",\"user\":{\"created_at\": \"Wed Mar 23 08:01:13 +0000 2010\",\"id\": 2661092896,\"statuses_count\": 20,\"favouritesCount\": 5747,\"friendsCount\": 195,\"isDefaultProfileImage\": false,\"followersCount\": 79}}";
		Status status = TwitterObjectFactory.createStatus(rawJSON);
		assertEquals(true, SpamFilter.isSpam(status));
	}
	
	public void testOneHigherWeightedFeature() throws TwitterException{
		String rawJSON = "{\"created_at\": \"Wed Jan 23 06:01:13 +0000 2017+\",\"source\":'<a href=\"http://hubot.com\"',\"displayTextRangeEnd\": 79,\"sentiment\": \"Negative\",\"hashtagEntities\": [],\"userMentionEntities\": [],\"urlEntities\": [{\"displayURL\": \"nytimes.com/2017/03/14/us/\u2026\",\"start\": 56,\"end\": 79,\"expandedURL\": \"http://www.nytimes.com/2017/03/14/us/politics/donald-trump-taxes.html?partner=rss&emc=rss&CpYz8\",\"url\": \"https://t.co/GozOKhh7pn\"}],\"id\": 842914405948014592,\"text\": \"Trump Wrote Off $100 Million in Business Losses in 2005 https://t.co/GozOKhh7pn https://t.co/Uo263oR400\",\"user\":{\"created_at\": \"Wed May 23 06:01:13 +0000 2007+\",\"description\":\"The Real Twitter API. I tweet about API changes. Don't get an answer? It's on my website.\",\"id\": 2661092896,\"statuses_count\": 8646,\"favouritesCount\": 5747,\"friendsCount\": 195,\"isDefaultProfileImage\": false,\"followersCount\": 79}}";
		Status status = TwitterObjectFactory.createStatus(rawJSON);
		assertEquals(true, SpamFilter.isSpam(status));
	}
	
	public void testMoreThanTwoFeatures() throws TwitterException{
		String rawJSON = "{\"created_at\": \"Wed Mar 23 06:01:13 +0000 2017\",\"source\":'<a href=\"http://twitter.com/download/iphone\"',\"displayTextRangeEnd\": 79,\"sentiment\": \"Negative\",\"hashtagEntities\": [],\"userMentionEntities\": [],\"urlEntities\": [{\"displayURL\": \"nytimes.com/2017/03/14/us/\u2026\",\"start\": 56,\"end\": 79,\"expandedURL\": \"http://www.nytimes.com/2017/03/14/us/politics/donald-trump-taxes.html?partner=rss&emc=rss&CpYz8\",\"url\": \"https://t.co/GozOKhh7pn\"}],\"id\": 842914405948014592,\"text\": \"Trump Wrote Off $100 Million in Business Losses in 2005 https://t.co/GozOKhh7pn https://t.co/Uo263oR400\",\"user\":{\"created_at\": \"Wed Mar 23 08:01:13 +0000 2017\",\"id\": 2661092896,\"statuses_count\": 20,\"favouritesCount\": 5747,\"friendsCount\": 195,\"isDefaultProfileImage\": false,\"followersCount\": 79}}";
		Status status = TwitterObjectFactory.createStatus(rawJSON);
		assertEquals(true, SpamFilter.isSpam(status));
	}
}
