package com.sparkstreaminganalytics.twitter;

import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONObject;

import twitter4j.Status;

abstract public class TwitterMetrics {
	private static HashSet<Long> retweetIdsSet = new HashSet<Long>();
	private static HashMap<Long, Long[]> uniqueRetweedIdToFavAndRetCountMap = new HashMap<Long, Long[]>();
	private static long approximateTotalEngagement;
	
	public static void updateApproximateTotalEngagement(Status status, JSONObject TweetStatusJsonObject, String keyword){
		if ((status.getRetweetedStatus() != null) && (TweetStatusJsonObject.getJSONObject("retweetedStatus")
				.getJSONArray("userMentionEntities").toString().contains(keyword)
				|| TweetStatusJsonObject.getJSONArray("urlEntities").toString().contains(keyword))) {

			if (retweetIdsSet.add(TweetStatusJsonObject.getJSONObject("retweetedStatus").getLong("id"))) {

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
}
