package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.ecosystem;

import java.util.HashMap;
import java.util.Map;

import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONArray;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONObject;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.InternalUtils;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.URLConnectionHttpClient;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.Twitter.IHttpClient;


public class Klout {

	final String API_KEY;
	
	public Klout(String apiKey) {
		this.API_KEY = apiKey;
	}
	
	IHttpClient client = new URLConnectionHttpClient();

	public Map<String,Double> getScore(String... userNames) {
		String unames = InternalUtils.join(userNames);
		Map vars = InternalUtils.asMap("key", API_KEY, "users", unames);
		String json = client.getPage("http://api.klout.com/1/klout.json", vars, false);
		JSONObject jo = new JSONObject(json);
		JSONArray users = jo.getJSONArray("users");
		Map<String,Double> scores = new HashMap(users.length());
		for(int i=0,n=users.length(); i<n; i++) {
			JSONObject u = users.getJSONObject(i);
			scores.put(u.getString("twitter_screen_name"), u.getDouble("kscore"));
		}
		return scores;
	}
}
