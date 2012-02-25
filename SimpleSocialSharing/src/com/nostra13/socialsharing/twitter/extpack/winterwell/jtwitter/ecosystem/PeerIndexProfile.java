package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.ecosystem;

import java.util.ArrayList;
import java.util.List;

import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONArray;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONException;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONObject;


/**
 * See http://dev.peerindex.com/docs/profile/show
 * @author daniel
 *
 */
public class PeerIndexProfile {

	/** 0 to 100 */
	public final int peerIndex;
	public final int authority;
	public final String twitterScreenName;
	public final List<String> topics;
	public final int audience;
	public final int activity;
	public final String slug;
	public final String url;
	public final String name;
	
	public String toString() {
		return "PeerIndexProfile["+twitterScreenName+" "+peerIndex+" "
				+authority+":"+audience+":"+activity+" "+topics+"]";
	}

	PeerIndexProfile(JSONObject jo) throws JSONException {
		this.peerIndex = jo.getInt("peerindex");
		this.authority = jo.getInt("authority");
		this.audience = jo.getInt("audience");
		this.activity = jo.getInt("activity");
		this.twitterScreenName = jo.getString("twitter");
		this.topics = new ArrayList();
		JSONArray _topics = jo.getJSONArray("topics");
		for(int i=0; i<_topics.length(); i++) {
			this.topics.add((String) _topics.get(i));
		}
		this.slug = jo.getString("slug");
		this.url = jo.getString("url");
		this.name = jo.optString("name");
//		"benchmark":[{"resonance":10,"audience":-1,"name":"arts, media and entertainment","activity":34},{"resonance":10,"audience":-1,"name":"technology and internet","activity":31},{"resonance":15,"audience":-1,"name":"sports","activity":10},{"resonance":10,"audience":-1,"name":"leisure and lifestyle","activity":30}],
		// "topics_score":[{"resonance":40,"term":"running"},{"resonance":40,"term":"veterans"},{"resonance":40,"term":"ptsd"},{"resonance":0,"term":"theatre"}]
	}
}
