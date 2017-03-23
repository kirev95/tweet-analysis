package com.sparkstreaminganalytics.twitter;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import twitter4j.Status;

public class TweetSerializationExclusionStrategy implements ExclusionStrategy {
	
	// Do not skip classes.
    public boolean shouldSkipClass(Class<?> arg0) {
        return false;
    }
    
    // Define which fields to be skipped while serialising a Status object
    // into a JSON object for efficiency.
    public boolean shouldSkipField(FieldAttributes f) {
    	switch (f.getName()) {
	        case "source":  return true;
	        case "isTruncated":  return true;
	        case "inReplyToStatusId":  return true;
	        case "inReplyToUserId":  return true;
	        case "isFavorited":  return true;
	        case "isRetweeted":  return true;
	        case "isPossiblySensitive":  return true;
	        case "lang":  return true;
	        case "contributorsIDs":  return true;
	        case "mediaEntities":  return true;
	        case "extendedMediaEntities":  return true;
	        case "symbolEntities":  return true;
	        case "currentUserRetweetId":  return true;
	        case "quotedStatus":  return true;
	        case "quotedStatusId":  return true;
	        case "displayTextRangeStart": return true;
	        case "isGeoEnabled": return true;
	        case "showAllInlineMedia": return true;
	        case "isVerified": return true;
	        case "translator": return true;
	        case "profileTextColor": return true;
	        case "descriptionURLEntities": return true;
	        case "screenName": return true;
	        case "profileUseBackgroundImage": return true;
	        case "profileBackgroundTiled": return true;
	        case "profileBackgroundImageUrl": return true;
	        case "isProtected": return true;
	        case "isFollowRequestSent": return true;
	        case "isContributorsEnabled": return true;
	        case "isDefaultProfile": return true;
	        case "profileImageUrl": return true;
	        case "profileSidebarBorderColor": return true;
	        case "utcOffset": return true;
	        case "profileBackgroundImageUrlHttps": return true;
	        case "profileBackgroundColor": return true;
	        case "timeZone": return true;
	        case "profileLinkColor": return true;
	        case "profileBannerImageUrl": return true;
	        case "listedCount": return true;
	        case "profileSidebarFillColor": return true;
	        case "name": return true;
	        case "profileImageUrlHttps": return true;
	        case "url": return true;
	        default: return false;
    	}
    }
}
