package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.ecosystem;

import java.util.Map;

import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONException;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONObject;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.InternalUtils;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.TwitterException;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.URLConnectionHttpClient;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.User;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.Twitter.IHttpClient;



/**
 * Access the PeerIndex ranking system
 * @author daniel
 * @testedby {@link PeerIndexTest}
 */
public class PeerIndex {

	final String API_KEY;
	
	public PeerIndex(String apiKey) {
		this.API_KEY = apiKey;
	}
	
	IHttpClient client = new URLConnectionHttpClient();
	
	/**
	 * @param screenName a Twitter screen-name
	 * @return
	 */
	public PeerIndexProfile getProfile(User user) {
		Map vars = InternalUtils.asMap(
				"id", user.screenName==null? user.id : user.screenName,
				"api_key", API_KEY
		);
		String json = client.getPage("http://api.peerindex.net/v2/profile/show.json", 
				vars, false);
		try {
			JSONObject jo = new JSONObject(json);
			return new PeerIndexProfile(jo);
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}
}
