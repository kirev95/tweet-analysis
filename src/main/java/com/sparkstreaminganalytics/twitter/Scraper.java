package com.sparkstreaminganalytics.twitter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.spark.*;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.*;
import org.apache.spark.streaming.api.java.*;
import scala.Tuple2;

import org.elasticsearch.spark.streaming.api.java.JavaEsSparkStreaming;
import org.json.JSONObject;
import org.spark_project.guava.collect.ImmutableList; 

public class Scraper{
	private static final String appName = "TwitterAnalyzer";
	private static final String master = "local[2]";
	
	public static void main(String[] args) throws InterruptedException{
		
		String json1 = "{\"created_at\":\"Sat Dec 26 11:55:29 +0000 2016\", \"id\":802495994143252483,\"source\":\"\\u003ca href=\\\"http:\\/\\/twitter.com\\/download\\/android\\\" rel=\\\"nofollow\\\"\\u003eTwitter for Android\\u003c\\/a\\u003e\",\"user\":{\"id\":3,\"name\":\"Orval\\u2122\",\"followers_count\":267,\"friends_count\":296},\"geo\":null,\"coordinates\":null,\"place\":{\"place_type\":\"admin\",\"full_name\":\"Arizona, USA\",\"country\":\"United States\"},\"retweet_count\":0,\"favorite_count\":0,\"entities\":{\"hashtags\":[{\"text\":\"USCvsCLEM\",\"indices\":[8,18]}],\"urls\":[{\"url\":\"https:\\/\\/t.co\\/eI4p3k51JK\",\"expanded_url\":\"https:\\/\\/cards.twitter.com\\/cards\\/18ce548tywy\\/2nxml\",\"display_url\":\"cards.twitter.com\\/cards\\/18ce548t\\u2026\",\"indices\":[117,140]}]},\"timestamp_ms\":\"1483032250000\"}"; 
		String json2 = "{\"created_at\":\"Sat Dec 26 12:55:14 +0000 2016\", \"id\":802495994143252484,\"source\":\"\\u003ca href=\\\"http:\\/\\/twitter.com\\/download\\/android\\\" rel=\\\"nofollow\\\"\\u003eTwitter for Android\\u003c\\/a\\u003e\",\"user\":{\"id\":4,\"name\":\"Orval\\u2122\",\"followers_count\":267,\"friends_count\":296},\"geo\":null,\"coordinates\":null,\"place\":{\"place_type\":\"admin\",\"full_name\":\"Arizona, USA\",\"country\":\"United States\"},\"retweet_count\":0,\"favorite_count\":0,\"entities\":{\"hashtags\":[{\"text\":\"Test\",\"indices\":[8,13]}],\"urls\":[{\"url\":\"https:\\/\\/t.co\\/eI4p3k51JK\",\"expanded_url\":\"https:\\/\\/cards.twitter.com\\/cards\\/18ce548tywy\\/2nxml\",\"display_url\":\"cards.twitter.com\\/cards\\/18ce548t\\u2026\",\"indices\":[114,137]}]},\"timestamp_ms\":\"1482032250000\"}"; 
		
		//JSONObject jsonObj = new JSONObject(json2);
		//jsonObj.remove("created_at");
		//System.out.println(jsonObj.toString());
//	}

		
		SparkConf conf = new SparkConf().setAppName(appName).setMaster(master);
		conf.set("es.index.auto.create", "true");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		JavaStreamingContext jssc = new JavaStreamingContext(jsc, Durations.seconds(10));
		
		
		//String json1 = "{\"reason\" : \"business\",\"airport\" : \"SFO\"}";  
		//String json2 = "{\"participants\" : 5,\"airport\" : \"OTP\"}";

		JavaRDD<String> stringRDD = jsc.parallelize(ImmutableList.of(json1, json2));
		Queue<JavaRDD<String>> microbatches = new LinkedList<JavaRDD<String>>();      
		microbatches.add(stringRDD);
		JavaDStream<String> stringDStream = jssc.queueStream(microbatches);
		
		
		// My fix applied here. Put stringDStream instead of stringRDD.
		JavaEsSparkStreaming.saveJsonToEs(stringDStream, "twitter/tweet");    
		
//		jssc.start();  
		
//		JavaReceiverInputDStream<String> lines = jssc.socketTextStream("localhost", 9999);
//		
//		// Split each line into words
//		JavaDStream<Map<String,?> words = lines.flatMap(
//		  new FlatMapFunction<String, String>() { // Note this is an Anonymous Java class!!!
//		    @Override
//		    public Iterator<String> call(String x) {
//		    	return Arrays.asList(x.split(" ")).iterator();
//		    }
//		  });
//		
//		// Make pairs from just the words. The Map Reduce mapper principle is used here.
//		JavaPairDStream<String, Integer> pairs = words.mapToPair(
//				new PairFunction<String, String, Integer>(){
//					@Override
//					public Tuple2<String, Integer> call(String s){
//						return new Tuple2<>(s, 1);
//					}
//				});
//		
//		// Count each word in each batch
//		JavaPairDStream<String, Integer> wordCounts = pairs.reduceByKey(
//		  new Function2<Integer, Integer, Integer>() {
//		    @Override public Integer call(Integer i1, Integer i2) {
//		      return i1 + i2;
//		    }
//		  });
//
//		// Print the first ten elements of each RDD generated in this DStream to the console
//		wordCounts.print();
		
		jssc.start();              // Start the computation
		jssc.awaitTermination();   // Wait for the computation to terminate
}
}
