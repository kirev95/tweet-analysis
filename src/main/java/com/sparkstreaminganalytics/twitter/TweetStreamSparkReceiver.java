package com.sparkstreaminganalytics.twitter;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.twitter.TwitterUtils;

import twitter4j.Status;

public class TweetStreamSparkReceiver {
	private String appName;
	private String master;
	private JavaReceiverInputDStream<Status> stream;
	private JavaStreamingContext jssc;
	
	public TweetStreamSparkReceiver(String reqAppName, String reqMaster, String[] keywords){
		this.appName = reqAppName;
		this.master = reqMaster;
		// Spark configuration
		SparkConf conf = new SparkConf().setAppName(this.appName).setMaster(this.master);
		// conf.set("es.index.auto.create", "true");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		// Configure log level here. For debugging comment out this code.
		jsc.setLogLevel("OFF");
		jssc = new JavaStreamingContext(jsc, Durations.seconds(10));

		stream = TwitterUtils.createStream(jssc, keywords);
	}
	
	public String getAppName(){
		return this.appName;
	}
	
	public String getMaster(){
		return this.master;
	}
	
	public JavaReceiverInputDStream<Status> getStream(){
		return this.stream;
	}
	
	public JavaStreamingContext getJavaStreamingContext(){
		return this.jssc;
	}
}
