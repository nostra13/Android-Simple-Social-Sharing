package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONArray;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONException;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONObject;


/**
 * Twitter's geolocation support. Use {@link Twitter#geo()} to get one of these
 * objects.
 * <p>
 * Conceptually, this is an extension of {@link Twitter}. The methods are here
 * because Twitter was getting crowded.
 * 
 * @see Twitter#setMyLocation(double[])
 * @see Twitter#setSearchLocation(double, double, String)
 * @see Status#getLocation()
 * 
 * @author Daniel Winterstein
 * @testedby {@link Twitter_GeoTest}
 */
public class Twitter_Geo {

	private double accuracy;

	private final Twitter jtwit;

	/**
	 * Use {@link Twitter#geo()} to get one.
	 * 
	 * @param jtwit
	 */
	Twitter_Geo(Twitter jtwit) {
		assert jtwit != null;
		assert jtwit.getHttpClient().canAuthenticate();
		this.jtwit = jtwit;
	}

	public List geoSearch(double latitude, double longitude) {
		throw new RuntimeException();
	}

	public List<Place> geoSearch(String query) {
		String url = jtwit.TWITTER_URL + "/geo/search.json";
		Map vars = InternalUtils.asMap("query", query);
		if (accuracy != 0) {
			vars.put("accuracy", String.valueOf(accuracy));
		}
		String json = jtwit.getHttpClient().getPage(url, vars,
				jtwit.getHttpClient().canAuthenticate());
		try {
			JSONObject jo = new JSONObject(json);
			JSONObject jo2 = jo.getJSONObject("result");
			JSONArray arr = jo2.getJSONArray("places");
			List places = new ArrayList(arr.length());
			for (int i = 0; i < arr.length(); i++) {
				JSONObject _place = arr.getJSONObject(i);
				// interpret it - maybe pinch code from jGeoPlanet?
				// https://dev.twitter.com/docs/api/1/get/geo/id/%3Aplace_id
				Place place = new Place(_place);
				places.add(place);
			}
			return places;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	public List geoSearchByIP(String ipAddress) {
		throw new RuntimeException();
	}

	/**
	 * @param woeid
	 * @return regions from which you can get trending info
	 * @see Twitter#getTrends(Number)
	 */
	public List<Place> getTrendRegions() {
		String json = jtwit.getHttpClient().getPage(
				jtwit.TWITTER_URL + "/trends/available.json", null, false);
		try {
			JSONArray json2 = new JSONArray(json);
			List<Place> trends = new ArrayList();
			for (int i = 0; i < json2.length(); i++) {
				JSONObject ti = json2.getJSONObject(i);
				Place place = new Place(ti);
				trends.add(place);
			}
			return trends;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	public void setAccuracy(double metres) {
		this.accuracy = metres;
	}

}
