package com.sparkstreaminganalytics.twitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

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

		// Keywords.
		String keywords[] = { "Demexit" };

		// Spark configuration
		SparkConf conf = new SparkConf().setAppName(appName).setMaster(master);
		// conf.set("es.index.auto.create", "true");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		// Configure log level here. For debugging comment out this code.
		jsc.setLogLevel("OFF");
		JavaStreamingContext jssc = new JavaStreamingContext(jsc, Durations.seconds(10));

		JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(jssc, keywords);

		// Filter: get only tweets in English.
		JavaDStream<Status> englishStatuses = stream.filter(new Function<Status, Boolean>() {
			@Override
			public Boolean call(Status status) {
				return status.getLang().equals("en");
			}
		});

		JavaDStream<String> cleanedTweets = englishStatuses.flatMap(new FlatMapFunction<Status, String>() {
			// Handle this exception later on instead of throwing it.
			@Override
			public Iterator<String> call(Status status) throws TwitterException { 
				String statusJSONString = gson.toJson(status);
				JsonElement jsonElement = gson.fromJson(statusJSONString, JsonElement.class);
				JsonObject statusJSONObject = jsonElement.getAsJsonObject();
				return Arrays.asList(statusJSONObject.toString() + "\n").iterator();
			}
		});

		cleanedTweets.print();

		jssc.start();
		try {
			jssc.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
