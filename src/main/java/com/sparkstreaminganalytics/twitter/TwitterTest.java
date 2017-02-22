package com.sparkstreaminganalytics.twitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;
import twitter4j.auth.OAuth2Token;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

import org.apache.spark.*;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.*;
import org.apache.spark.streaming.api.java.*;
import scala.Tuple2;
import scala.util.parsing.json.JSON;

import org.apache.spark.streaming.twitter.*;
///////////////////////////////////////////////////////////////
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.twitter.TwitterUtils;
import org.json.JSONObject;

import com.google.gson.Gson;

import scala.Tuple2;
import twitter4j.Status;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author That Guy
 */
public class TwitterTest {
	private static final String appName = "TwitterAnalyzer";
	private static final String master = "local[2]";

	// Gson API is a Google API used for conversion Java and JSON objects.
	private static Gson gson = new Gson();

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
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();

		// Get authentication token.
		OAuth2Token auth = twitter.getOAuth2Token();

		// Spark configuration
		SparkConf conf = new SparkConf().setAppName(appName).setMaster(master);
		// conf.set("es.index.auto.create", "true");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		JavaStreamingContext jssc = new JavaStreamingContext(jsc, Durations.seconds(10));

		JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(jssc);

		// Filter: get only tweets in English.
		JavaDStream<Status> englishStatuses = stream.filter(new Function<Status, Boolean>() {
			@Override
			public Boolean call(Status status) {
				return status.getLang().equals("en");
			}
		});

		jssc.start();
		try {
			jssc.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
