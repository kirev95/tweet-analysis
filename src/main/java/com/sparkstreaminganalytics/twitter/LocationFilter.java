package com.sparkstreaminganalytics.twitter;

import java.util.Locale;

import org.json.JSONObject;

import twitter4j.Status;

abstract public class LocationFilter {
	private static Locale[] locales = getLocaleObjects();
	private static String tmpUserLocation = "";
	private static String tmpCountryCode = "";
	private static String tmpCountryName = "";
	public static int sensibleLocations;
	
	
	public static JSONObject filterTweetUserLocations(Status status, JSONObject jsonObj){
		jsonObj.getJSONObject("user").remove("location");
	
		// For each status, try to match the location of the user
		// tweeted
		// to a known country, aggregate these and send to elastisearch.
		if (status.getUser().getLocation() != null) {
			for (Locale countryObject : locales) {
				tmpUserLocation = status.getUser().getLocation();
				tmpUserLocation = tmpUserLocation.replaceAll("\\b" + "UK" + "\\b", "GB");
				tmpCountryCode = countryObject.getCountry();
				tmpCountryName = countryObject.getDisplayCountry();
				if (tmpUserLocation.contains(tmpCountryCode) || tmpUserLocation.contains(tmpCountryName)) {
					jsonObj.getJSONObject("user").put("location", countryObject.getDisplayCountry());
					sensibleLocations++;
				}
			}
		}
		
		return jsonObj;
	}
	
	private static Locale[] getLocaleObjects() {
		String[] localeStrings = Locale.getISOCountries();
		Locale[] localesObj = new Locale[localeStrings.length];
		for (int i = 0; i < localeStrings.length; i++) {
			localesObj[i] = new Locale("", localeStrings[i]);
		}
		return localesObj;
	}
}
