package com.sparkstreaminganalytics.twitter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import twitter4j.Status;
import twitter4j.User;

public class SpamFilter {
	private static int[] countArr = new int[8];
	private static int numberOfStatusesChecked = 0;
	private static int identifiedSpam = 0;
	// Returns true if the status is spam.
	public static boolean isSpam(Status status){
		double spamProbability = calculateSpamProbability(status);
		double threshold = 0.25;
			System.out.println("Status: " + status.toString());
			System.out.println("Spam probability: " + spamProbability);
		
		return spamProbability >= threshold;
	}
	
	private static double calculateSpamProbability(Status status) {
		numberOfStatusesChecked++;
		
		// Get the numerical value of the boolean result for each calculation.
		int isRecentlyCreated = isRecentlyCreated(status.getUser()) ? 1 : 0;
		int isCreatingLittleContent = isCreatingLittleContent(status.getUser()) ? 1 : 0;
		int hasAFewFollowers = hasAFewFollowers(status.getUser()) ? 1 : 0;
		int hasShortDescription = hasShortDescription(status.getUser()) ? 1 : 0;
		int containsBotFriendlyContentSources = containsBotFriendlyContentSources(status) ? 1 : 0;
		int hasManyHashtags = hasManyHashtags(status) ? 1 : 0;
		int hasShortContentLength = hasShortContentLength(status) ? 1 : 0;
		int requestsRetweetOrFollow = requestsRetweetOrFollow(status) ? 1 : 0;
		
		int numberOfFeatures = 8;
		
		double probability = (isRecentlyCreated + isCreatingLittleContent + hasAFewFollowers + hasShortDescription
				+ containsBotFriendlyContentSources + hasManyHashtags + hasShortContentLength
				+ requestsRetweetOrFollow) / (double)numberOfFeatures;
		
		System.out.println("####################################");
		System.out.println(isRecentlyCreated + " " + isCreatingLittleContent + " " + hasAFewFollowers + " " + hasShortDescription + " "
				+ containsBotFriendlyContentSources + " " + hasManyHashtags + " " + hasShortContentLength + " "
				+ requestsRetweetOrFollow);
		System.out.println("Count Arr: " + evaluateFeatures(isRecentlyCreated , isCreatingLittleContent , hasAFewFollowers , hasShortDescription
				, containsBotFriendlyContentSources , hasManyHashtags , hasShortContentLength
				, requestsRetweetOrFollow));
		
		System.out.println("Statuses checked: " + numberOfStatusesChecked);
		
		if(probability > 0){
			identifiedSpam++;
			System.out.println("Identified as SPAM: " + identifiedSpam);
		}
		System.out.println("####################################");
		// Assume that all features are equally important.
		
		
		return probability;
	}
	
	private static String evaluateFeatures(int ... features){	
		for(int i=0; i<8; i++){
			countArr[i] += features[i];
		}
		
		return java.util.Arrays.toString(countArr);
	}
	
	// Feature 1: Asking for RT or Follow
	// Spammers tend to ask other users to follow them and to retweet their statuses.
	private static boolean requestsRetweetOrFollow(Status status) {
		
		// Lower case the tweet text to increase the match probability.
		String text = status.getText().toLowerCase();
		
		// Common request phrases in the tweets.
		String[] requests = new String[]{"rt and follow", "rt & follow", "rt+follow", "follow and rt", "follow & rt", "follow+rt", "follow + rt", "rt + follow"};
		
		// Return true if any of the request is found in the text.
		for(String request : requests){
			if(text.contains(request)){
				return true;
			}
		}
		
		return false;
	}
	
	// Feature 2: Short content length: Spammers tend to write short statuses.
	// Mark the tweet if its length is less than 20 characters.
	private static boolean hasShortContentLength(Status status) {
		return status.getText().length() < 20;
	}
	
	// Feature 3: Many hashtags in the content - to be able to reach more people.
	// Mark tweet if more than 5 hashtags are present in the tweet content.
	private static boolean hasManyHashtags(Status status) {
		return status.getHashtagEntities().length > 5;
	}

	// Feature 4: Contain sources typical for Bots.
	private static boolean containsBotFriendlyContentSources(Status status) {
		// Lower case the tweet text to increase the match probability.
		String source = status.getSource().toLowerCase();
		
		// Common request phrases in the tweets.
		String[] botSources = new String[]{"twittbot", "easybotter", "hellotxt", "dlvr.it", "hootsuite", "buffer"};
		
		// Return true if any of the request is found in the text.
		for(String botSource : botSources){
			if(source.contains(botSource)){
				return true;
			}
		}
		
		return false;
	}
	
	// Feature 5: Spammers tend to have short description.
	private static boolean hasShortDescription(User user) {
		return (user.getDescription() == null) || (user.getDescription().length() < 20);
	}
	
	// Feature 6: Spammers followers ratio is low (followers divided  by followed users).
	private static boolean hasAFewFollowers(User user) {
		// If the user follows a 100 times more users than the users following him
		// then he is probably a spammer.
		return (user.getFollowersCount() / (double) user.getFriendsCount()) <= 0.01;
	}
	
	// Feature 7: Spammers tend to create little content before being detected.
	private static boolean isCreatingLittleContent(User user) {
		return user.getStatusesCount() <= 50;
	}
	
	// Feature 8: New Bots accounts are normally created frequently, when the other account is suspended,
	// so they are quite fresh.
	private static boolean isRecentlyCreated(User user) {
		DateTime now = new DateTime(DateTimeZone.UTC);
		DateTime userAccountStartDate = new DateTime(user.getCreatedAt());
		int days = Days.daysBetween(userAccountStartDate, now).getDays();
		return days <= 1;
	}
}
