package com.sparkstreaminganalytics.twitter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import twitter4j.Status;
import twitter4j.User;

/**
*
* @author Veselin Kirev
*/
abstract public class SpamFilter {
	private static int[] countArr = new int[9];
	private static int numberOfStatusesChecked = 0;
	private static int identifiedSpam = 0;
	private static boolean isCurrentStatusSpam = false;
	private static final double threshold = 0.5;
	
	// Returns true if the status is spam.
	public static boolean isSpam(Status status){
		isCurrentStatusSpam = false;
		System.out.println("####################################");
		double spamProbability = calculateSpamProbability(status);
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
		int hasDefaultPicture = hasDefaultProfileImage(status.getUser()) ? 1 : 0;
		int hasManyHashtags = hasManyHashtags(status) ? 1 : 0;
		int hasShortContentLength = hasShortContentLength(status) ? 1 : 0;
		int requestsRetweetOrFollow = requestsRetweetOrFollow(status) ? 1 : 0;
		int containsBotFriendlyContentSources = containsBotFriendlyContentSources(status) ? 1 : 0;
		
		int numberOfFeatures = 8;
		
		double probability = 0;
		
		if(containsBotFriendlyContentSources(status)){
			probability = 1;
			identifiedSpam++;
			isCurrentStatusSpam = true;
		}
		else{
			probability = (isRecentlyCreated + isCreatingLittleContent + hasAFewFollowers + hasShortDescription
					+ hasDefaultPicture + hasManyHashtags + hasShortContentLength
					+ requestsRetweetOrFollow) / (double)numberOfFeatures;
			
			// In this way: 2 features = 0.5
			// 3 features = 0.61 etc...
			// So 2 features are enough to say that it is a spam.
			if(probability != 0){
				probability = Math.sqrt(1 / probability) * probability;
			}
			
			if(probability >= threshold){
				identifiedSpam++;
				isCurrentStatusSpam = true;
			}
		}
		
		// Print the statistics.
		System.out.println("This tweet spam statistics:");
		System.out.println("Is this status a spam?: " + (isCurrentStatusSpam ? "Yes" : "No"));
		
		System.out.format("%16s%16s%17s%19s%13s%15s%15s%20s%13s \n", "Recently created", "Little content", "A few followers", "Short description", "Default Img", "Many hashtags", "Short content", "Reqeusts RT/follow", "Bot sources");
		System.out.format("%16d%16d%17d%19d%13s%15d%15d%20d%13d \n", isRecentlyCreated , isCreatingLittleContent , hasAFewFollowers , hasShortDescription
				, hasDefaultPicture , hasManyHashtags , hasShortContentLength
				, requestsRetweetOrFollow, containsBotFriendlyContentSources);
		System.out.println("\n************************************\n");
		System.out.println("All tweets spam statistics:");
		System.out.println("\nFeatures counter: " + evaluateFeatures(isRecentlyCreated , isCreatingLittleContent , hasAFewFollowers , hasShortDescription
				, hasDefaultPicture , hasManyHashtags , hasShortContentLength
				, requestsRetweetOrFollow, containsBotFriendlyContentSources));
		
		System.out.println("Statuses checked: " + numberOfStatusesChecked);
		System.out.println("Total messages identified as SPAM so far: " + identifiedSpam);
//		System.out.println("Current average spam probability: " + avgProbabilityToSeeSpamMessage);
		
		return probability;
	}
	
	private static String evaluateFeatures(int ... features){	
		for(int i=0; i<9; i++){
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
		String[] botSources = new String[]{"bot", "tilde.town", "hubot", "huginn", "botomatic", "twittbot", "easybotter", "hellotxt", "dlvr.it", "hootsuite", "buffer"};
		
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
	
	// Feature 9: Egg pictures are the default ones given by Twitter to the new users.
	// Spammers/Bots usually have these, as e.g. a Bot creates a new default account
	// and starts spamming.
	private static boolean hasDefaultProfileImage(User user){
		return user.isDefaultProfileImage();
	}
}
