package com.sparkstreaminganalytics.twitter;

import org.json.JSONObject;

import junit.framework.TestCase;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

public class TwitterMetricsTest extends TestCase {

	public TwitterMetricsTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testTotalEngagementInRetweet() throws TwitterException{
		// Retweeted Status created a minute later after the original tweet.
		// Creating the Status requires to use the default format of the tweet.
		String rawJSON = "{\"created_at\": \"Wed Jan 23 06:03:13 +0000 2017+\",\"source\":'<a href=\"http://twitter.com/download/iphone\"',\"displayTextRangeEnd\": 79,\"sentiment\": \"Negative\",\"hashtagEntities\": [],\"userMentionEntities\": [],\"urlEntities\": [{\"displayURL\": \"nytimes.com/2017/03/14/us/\u2026\",\"start\": 56,\"end\": 79,\"expandedURL\": \"http://www.nytimes.com/2017/03/14/us/politics/donald-trump-taxes.html?partner=rss&emc=rss&CpYz8\",\"url\": \"https://t.co/GozOKhh7pn\"}],\"id\": 842914405948014592,\"text\": \"Trump Wrote Off $100 Million in Business Losses in 2005 https://t.co/GozOKhh7pn https://t.co/Uo263oR400\",\"user\":{\"created_at\": \"Wed May 23 06:01:13 +0000 2007+\",\"description\":\"The Real Twitter API. I tweet about API changes. Don't get an answer? It's on my website.\",\"id\": 2661092896,\"statuses_count\": 8646,\"favouritesCount\": 5747,\"friendsCount\": 195,\"isDefaultProfileImage\": false,\"followersCount\": 79},\"retweeted_status\":{\"created_at\":\"Wed Jan 23 06:02:13 +0000 2017+\",\"displayTextRangeEnd\":139,\"hashtagEntities\":[{\"start\":28,\"end\":42,\"text\":\"Trump is the president of the USA.\"}],\"userMentionEntities\":[],\"urlEntities\":[],\"id\":844947808511655936,\"text\":\"Trump is the president of the USA.\",\"user\":{\"created_at\":\"Wed Jan 23 06:01:13 +0000 2005+\",\"statusesCount\":41986,\"description\":\"Truth siren; Reporter @TATMNews; & Host of #TrendingTopics @RealProgressUS. DM me tips to stories getting censored. Dying trying 2 make Good Samaritanship sexy\",\"isDefaultProfileImage\":false,\"favouritesCount\":54975,\"location\":\"Seattle, Washington\",\"friendsCount\":11446,\"id\":25493304,\"followersCount\":20214},\"retweet_count\":36,\"favorite_count\":64}}";
		Status status = TwitterObjectFactory.createStatus(rawJSON);
		
		// Different format of the JSON
		String jsonFormatForObject = "{\"created_at\": \"Wed Jan 23 06:03:13 +0000 2017+\",\"source\":'<a href=\"http://twitter.com/download/iphone\"',\"displayTextRangeEnd\": 79,\"sentiment\": \"Negative\",\"hashtagEntities\": [],\"userMentionEntities\": [],\"urlEntities\": [{\"displayURL\": \"nytimes.com/2017/03/14/us/\u2026\",\"start\": 56,\"end\": 79,\"expandedURL\": \"http://www.nytimes.com/2017/03/14/us/politics/donald-trump-taxes.html?partner=rss&emc=rss&CpYz8\",\"url\": \"https://t.co/GozOKhh7pn\"}],\"id\": 842914405948014592,\"text\": \"Trump Wrote Off $100 Million in Business Losses in 2005 https://t.co/GozOKhh7pn https://t.co/Uo263oR400\",\"user\":{\"created_at\": \"Wed May 23 06:01:13 +0000 2007+\",\"description\":\"The Real Twitter API. I tweet about API changes. Don't get an answer? It's on my website.\",\"id\": 2661092896,\"statuses_count\": 8646,\"favouritesCount\": 5747,\"friendsCount\": 195,\"isDefaultProfileImage\": false,\"followersCount\": 79},\"retweetedStatus\":{\"created_at\":\"Wed Jan 23 06:02:13 +0000 2017+\",\"displayTextRangeEnd\":139,\"hashtagEntities\":[{\"start\":28,\"end\":42,\"text\":\"Who is the president of the USA?\"}],\"userMentionEntities\":[],\"urlEntities\":[],\"id\":844947808511655936,\"text\":\"Trump is the president of the USA.\",\"user\":{\"created_at\":\"Wed Jan 23 06:01:13 +0000 2005+\",\"statusesCount\":41986,\"description\":\"Truth siren; Reporter @TATMNews; & Host of #TrendingTopics @RealProgressUS. DM me tips to stories getting censored. Dying trying 2 make Good Samaritanship sexy\",\"isDefaultProfileImage\":false,\"favouritesCount\":54975,\"location\":\"Seattle, Washington\",\"friendsCount\":11446,\"id\":25493304,\"followersCount\":20214},\"retweetCount\":36,\"favoriteCount\":64}}";
		JSONObject jsonObj = new JSONObject(jsonFormatForObject);
		
		// Initial value
		assertEquals(0, TwitterMetrics.getApproximateTotalEngagement());
		
		// Simulate receiving the tweet above, and update total engagement.
		TwitterMetrics.updateApproximateTotalEngagement(status, jsonObj, "Trump");
		
		// Initial value
		assertEquals(1, TwitterMetrics.getApproximateTotalEngagement());
		
		// Create new tweet, but now with the keyword in the retweet.
		jsonFormatForObject = "{\"created_at\": \"Wed Jan 23 06:03:13 +0000 2017+\",\"source\":'<a href=\"http://twitter.com/download/iphone\"',\"displayTextRangeEnd\": 79,\"sentiment\": \"Negative\",\"hashtagEntities\": [],\"userMentionEntities\": [],\"urlEntities\": [{\"displayURL\": \"nytimes.com/2017/03/14/us/\u2026\",\"start\": 56,\"end\": 79,\"expandedURL\": \"http://www.nytimes.com/2017/03/14/us/politics/donald-trump-taxes.html?partner=rss&emc=rss&CpYz8\",\"url\": \"https://t.co/GozOKhh7pn\"}],\"id\": 842914405948014592,\"text\": \"Trump Wrote Off $100 Million in Business Losses in 2005 https://t.co/GozOKhh7pn https://t.co/Uo263oR400\",\"user\":{\"created_at\": \"Wed May 23 06:01:13 +0000 2007+\",\"description\":\"The Real Twitter API. I tweet about API changes. Don't get an answer? It's on my website.\",\"id\": 2661092896,\"statuses_count\": 8646,\"favouritesCount\": 5747,\"friendsCount\": 195,\"isDefaultProfileImage\": false,\"followersCount\": 79},\"retweetedStatus\":{\"created_at\":\"Wed Jan 23 06:02:13 +0000 2017+\",\"displayTextRangeEnd\":139,\"hashtagEntities\":[{\"start\":28,\"end\":42,\"text\":\"Trump is the president of the USA.\"}],\"userMentionEntities\":[],\"urlEntities\":[],\"id\":844947808511655936,\"text\":\"Trump is the president of the USA.\",\"user\":{\"created_at\":\"Wed Jan 23 06:01:13 +0000 2005+\",\"statusesCount\":41986,\"description\":\"Truth siren; Reporter @TATMNews; & Host of #TrendingTopics @RealProgressUS. DM me tips to stories getting censored. Dying trying 2 make Good Samaritanship sexy\",\"isDefaultProfileImage\":false,\"favouritesCount\":54975,\"location\":\"Seattle, Washington\",\"friendsCount\":11446,\"id\":25493304,\"followersCount\":20214},\"retweetCount\":36,\"favoriteCount\":64}}";
		JSONObject jsonObj2 = new JSONObject(jsonFormatForObject);
		
		// Simulate receiving the tweet above, and update total engagement.
		TwitterMetrics.updateApproximateTotalEngagement(status, jsonObj2, "Trump");
		
		// Initial value
		assertEquals(103, TwitterMetrics.getApproximateTotalEngagement());
	}
}
