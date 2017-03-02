package com.sparkstreaminganalytics.twitter;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

// Twitter library
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

// Spark
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.twitter.TwitterUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.spark.streaming.api.java.JavaEsSparkStreaming;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.json.JSONObject;

// Google API for manipulation JSON objects.
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 *
 * @author Veselin Kirev
 */
public class Scraper {
	private static final String appName = "TwitterAnalyzer";
	private static final String master = "local[2]";
	private static Locale[] locales = getLocaleObjects();
	private static String tmpUserLocation = "";
	private static String tmpCountryCode = "";
	private static String tmpCountryName = "";
	private static String elasticDateFormat = "yyyyMMdd'T'HHmmssZ";

	private static Locale[] getLocaleObjects(){
		String[] localeStrings = Locale.getISOCountries();
		Locale[] localesObj = new Locale[localeStrings.length];
		for (int i=0; i<localeStrings.length; i++)
		{
			localesObj[i] = new Locale("", localeStrings[i]);
		}
		return localesObj;
	}
	// Gson API is a Google API used for conversion Java and JSON objects.
	// Use this if you want to get location=null as well or other nulls
	// private static Gson gson = new GsonBuilder().serializeNulls().create();
	private static Gson gson = new GsonBuilder().setExclusionStrategies(new TestExclStrat()).create();

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) throws IOException, TwitterException {
		// Using non-default cb to create Twitter object, where JSON store is
		// enabled.
		// Otherwise Status object cannot be converted to raw JSON.
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setJSONStoreEnabled(true);
		TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
		
//		TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
//		        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
//		
//		//DeleteIndexResponse deleteResponse = client.admin().indices().delete(new DeleteIndexRequest("twitter")).actionGet();
//		GetResponse response = client.prepareGet("twitter", "tweet", "836336600493641728").get();
	

		// Keywords.
		String keywords[] = { "trump" };

		// Spark configuration
		SparkConf conf = new SparkConf().setAppName(appName).setMaster(master);
		//conf.set("es.index.auto.create", "true");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		// Configure log level here. For debugging comment out this code.
		jsc.setLogLevel("OFF");
		JavaStreamingContext jssc = new JavaStreamingContext(jsc, Durations.seconds(10));

		JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(jssc, keywords);
		System.out.println("");
		
		// Filter: get only tweets in English.
		JavaDStream<Status> englishStatuses = stream.filter(new Function<Status, Boolean>() {
			@Override
			public Boolean call(Status status) {
				return status.getLang().equals("en");
			}
		});
		
		// 
		JavaDStream<String> cleanedTweets = englishStatuses.flatMap(new FlatMapFunction<Status, String>() {
			// Handle this exception later on instead of throwing it.
			@Override
			public Iterator<String> call(Status status) throws TwitterException {

				//long timestamp = status.getCreatedAt().getTime();
				//System.out.println(timestamp);
				String statusJSONString = gson.toJson(status);
				
				// Add timestamp to the object, converting the Date object to timestamp_ms
				JSONObject jsonObj = new JSONObject(statusJSONString);
				
				//jsonObj.put("timestamp", timestamp);
				
				//Process date:
				try{
		            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy h:mm:ss a");
		            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		            Date theDate = sdf.parse(jsonObj.getString("createdAt"));
		            String newstring = new SimpleDateFormat(elasticDateFormat).format(theDate);
		            System.out.println(newstring);
		            jsonObj.put("createdAt", newstring);
		            
		        }
		        catch(Exception e){
		            System.err.println("There was problem with parsing the date.");
		        }
				
				
				jsonObj.getJSONObject("user").remove("location");
				
				
				// For each status, try to match the location of the user tweeted
				// to a known country, aggregate these and send to elastisearch.
				if(status.getUser().getLocation() != null){
					for (Locale countryObject : locales)
					{
						tmpUserLocation = status.getUser().getLocation();
						tmpUserLocation.replaceAll("\\b" + "UK" + "\\b", "GB");
						tmpCountryCode = countryObject.getCountry();
						
						tmpCountryName = countryObject.getDisplayCountry();
						
						if(tmpUserLocation.contains(tmpCountryCode)
							|| tmpUserLocation.contains(tmpCountryName))
						{
							jsonObj.getJSONObject("user").put("location", countryObject.getDisplayCountry());
						}
					}
				}
				return Arrays.asList(jsonObj.toString() + "\n").iterator();
			}
		});
		JavaEsSparkStreaming.saveJsonToEs(cleanedTweets, "tweets/tweet");    
		//cleanedTweets.print();

		jssc.start();
		
		
		try {
			jssc.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
