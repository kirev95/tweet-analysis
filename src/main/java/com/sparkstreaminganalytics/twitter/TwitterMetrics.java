package com.sparkstreaminganalytics.twitter;

import java.util.HashMap;
import java.util.HashSet;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.json.JSONObject;

import twitter4j.Status;

abstract public class TwitterMetrics {
	private static HashSet<Long> retweetIdsSet = new HashSet<Long>();
	private static HashMap<Long, Long[]> uniqueRetweedIdToFavAndRetCountMap = new HashMap<Long, Long[]>();
	private static long approximateTotalEngagement;
	private static boolean isFirstRTWithThisID = false;
	private static double averageRetweetResponseTime = 0;
	
	public static void updateApproximateTotalEngagement(Status status, JSONObject TweetStatusJsonObject, String keyword){
		if ((status.getRetweetedStatus() != null) && (TweetStatusJsonObject.getJSONObject("retweetedStatus")
				.getJSONArray("userMentionEntities").toString().contains(keyword)
				|| TweetStatusJsonObject.getJSONArray("urlEntities").toString().contains(keyword))) {

			if (retweetIdsSet.add(TweetStatusJsonObject.getJSONObject("retweetedStatus").getLong("id"))) {
				
				// Mark the retweet as the first one received with this ID.
				isFirstRTWithThisID = true;
				
				// Add it to the map where the update will happen later on.
				uniqueRetweedIdToFavAndRetCountMap.put(TweetStatusJsonObject.getJSONObject("retweetedStatus").getLong("id"),
						new Long[2]);

				uniqueRetweedIdToFavAndRetCountMap
						.get(TweetStatusJsonObject.getJSONObject("retweetedStatus").getLong("id"))[0] = new Long(
								TweetStatusJsonObject.getJSONObject("retweetedStatus").getInt("favoriteCount"));

				uniqueRetweedIdToFavAndRetCountMap
						.get(TweetStatusJsonObject.getJSONObject("retweetedStatus").getLong("id"))[1] = new Long(
								TweetStatusJsonObject.getJSONObject("retweetedStatus").getInt("retweetCount"));

				// 2 comes from the fact that the keyword is mentioned
				// both in the tweet and in the retweet.
				approximateTotalEngagement += 2
						+ TweetStatusJsonObject.getJSONObject("retweetedStatus").getInt("favoriteCount")
						+ TweetStatusJsonObject.getJSONObject("retweetedStatus").getInt("retweetCount");

			} else {
				// Add the to the engagement only the difference of retweet and favorite counts
				// since last time. E.g. treat it as update, instead of just adding it.
				approximateTotalEngagement += 2
						+ TweetStatusJsonObject.getJSONObject("retweetedStatus").getInt("favoriteCount")
						+ TweetStatusJsonObject.getJSONObject("retweetedStatus").getInt("retweetCount")
						- uniqueRetweedIdToFavAndRetCountMap
								.get(TweetStatusJsonObject.getJSONObject("retweetedStatus").getLong("id"))[0]
						- uniqueRetweedIdToFavAndRetCountMap
								.get(TweetStatusJsonObject.getJSONObject("retweetedStatus").getLong("id"))[1];
				
				// Then update the latest retweet and favorite counts information.
				uniqueRetweedIdToFavAndRetCountMap
						.get(TweetStatusJsonObject.getJSONObject("retweetedStatus").getLong("id"))[0] = new Long(
								TweetStatusJsonObject.getJSONObject("retweetedStatus").getInt("favoriteCount"));

				uniqueRetweedIdToFavAndRetCountMap
						.get(TweetStatusJsonObject.getJSONObject("retweetedStatus").getLong("id"))[1] = new Long(
								TweetStatusJsonObject.getJSONObject("retweetedStatus").getInt("retweetCount"));

			}
		} else {
			approximateTotalEngagement++;
		}
	}
	
	public static long getApproximateTotalEngagement(){
		return approximateTotalEngagement;
	}
	
	public static double getAverageRTResponseTime(Status status){
		if(isFirstRTWithThisID && isCurrentTweetTheFirstToRetweet(status)){
			DateTime tweetTime = new DateTime(status.getCreatedAt());
			DateTime rtTime = new DateTime(status.getRetweetedStatus().getCreatedAt());
			int secondsBetween = Seconds.secondsBetween(rtTime, tweetTime).getSeconds();
			
			if(averageRetweetResponseTime == 0){
				averageRetweetResponseTime = secondsBetween;
			}
			else{
				averageRetweetResponseTime = (averageRetweetResponseTime * retweetIdsSet.size() + secondsBetween) / (double) (retweetIdsSet.size() + 1);
			}
		}
		
		return averageRetweetResponseTime;
	}
	
	public static void resetRTValidator(){
		isFirstRTWithThisID = false;
	}
	
	private static boolean isCurrentTweetTheFirstToRetweet(Status status){
		return (status.getRetweetedStatus().getRetweetCount() < 2);
	}
}
