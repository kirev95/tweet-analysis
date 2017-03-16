package com.sparkstreaminganalytics.twitter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONObject;

abstract public class ElasticDateFormatter {
	private static String dateFieldName = "createdAt";
	private static String dateOriginalFormat = "MMM dd, yyyy h:mm:ss a";
	private static String dateRequiredFormat = "yyyyMMdd'T'HHmmssZ";
	private static String timeZone = "UTC";
	
	public static JSONObject getFormattedDate(String statusJSONString){
		JSONObject jsonObj = new JSONObject(statusJSONString);
		
		// Process date:
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(dateOriginalFormat);
			sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
			Date theDate = sdf.parse(jsonObj.getString(dateFieldName));
			String newstring = new SimpleDateFormat(dateRequiredFormat).format(theDate);
			jsonObj.put(dateFieldName, newstring);

		} catch (Exception e) {
			System.err.println("There was problem with parsing the date.");
		}
		
		return jsonObj;
	}
}
