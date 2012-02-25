package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.ecosystem;

import java.util.Map;

import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONException;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONObject;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.InternalUtils;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.TwitterException;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.URLConnectionHttpClient;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.Twitter.IHttpClient;



/**
 * Use the Topsy Twitter analytics services. 
 * See http://code.google.com/p/otterapi/wiki/Resources
 * TODO more methods!
 * TODO rate limit info
 * @author daniel
 *
 */
public class Topsy {

	public static final class UrlInfo {
		public final String title;
		/**
		 * Sadly, Topsy doesn't resolve equivalent links,
		 * so this is a lower-bound.
		 */
		public final int linkCount;
		public final String desc;
		public final String url;
		
		public UrlInfo(JSONObject resp) throws JSONException {
			url = resp.getString("url");
			title = resp.getString("title");
			linkCount = resp.getInt("trackback_total");
			desc = resp.getString("description");
		}

		@Override
		public String toString() {
			return url+" "+linkCount+" "+title;
		}
	}

	private IHttpClient client = new URLConnectionHttpClient();
	private String apikey;
	
	/**
	 * Use Topsy without an API-key - rate-limited to 3,000 per day.
	 */
	public Topsy() {
	}
	
	public Topsy(String apiKey) {
		this.apikey = apiKey;
	}
	
	public Topsy.UrlInfo getUrlInfo(String url) {
		Map vars = InternalUtils.asMap("url", url);
		if (apikey!=null) vars.put("apikey", apikey);
		String json = client.getPage("http://otter.topsy.com/urlinfo.json", vars, false);
		try {
			JSONObject jo = new JSONObject(json);
			JSONObject resp = jo.getJSONObject("response");
			return new Topsy.UrlInfo(resp);
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	
}
