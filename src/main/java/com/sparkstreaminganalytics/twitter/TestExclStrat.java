package com.sparkstreaminganalytics.twitter;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import twitter4j.Status;

public class TestExclStrat implements ExclusionStrategy {

    public boolean shouldSkipClass(Class<?> arg0) {
        return false;
    }

    public boolean shouldSkipField(FieldAttributes f) {
//    	return false;
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
	        default: return false;
    	}
    }
}
