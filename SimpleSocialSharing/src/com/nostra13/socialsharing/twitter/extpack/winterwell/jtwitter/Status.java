package com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONArray;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONException;
import com.nostra13.socialsharing.twitter.extpack.winterwell.json.JSONObject;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.Twitter.ITweet;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.Twitter.KEntityType;
import com.nostra13.socialsharing.twitter.extpack.winterwell.jtwitter.Twitter.TweetEntity;



/**
 * A Twitter status post. .toString() returns the status text.
 * <p>
 * Notes: This is a finalised data object. It exposes its fields for convenient
 * access. If you want to change your status, use
 * {@link Twitter#setStatus(String)} and {@link Twitter#destroyStatus(Status)}.
 */
public final class Status implements ITweet {
	/**
	 * regex for @you mentions
	 */
	static final Pattern AT_YOU_SIR = Pattern.compile("@(\\w+)");

	private static final String FAKE = "fake";

	private static final long serialVersionUID = 1L;

	/**
	 * Convert from a json array of objects into a list of tweets.
	 * 
	 * @param json
	 *            can be empty, must not be null
	 * @throws TwitterException
	 */
	static List<Status> getStatuses(String json) throws TwitterException {
		if (json.trim().equals(""))
			return Collections.emptyList();
		try {
			List<Status> tweets = new ArrayList<Status>();
			JSONArray arr = new JSONArray(json);
			for (int i = 0; i < arr.length(); i++) {
				Object ai = arr.get(i);
				if (JSONObject.NULL.equals(ai)) {
					continue;
				}
				JSONObject obj = (JSONObject) ai;
				Status tweet = new Status(obj, null);
				tweets.add(tweet);
			}
			return tweets;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * Search results use a slightly different protocol! In particular w.r.t.
	 * user ids and info.
	 * 
	 * @param searchResults
	 * @return search results as Status objects - but with dummy users! The
	 *         dummy users have a screenname and a profile image url, but no
	 *         other information. This reflects the current behaviour of the
	 *         Twitter API.
	 */
	static List<Status> getStatusesFromSearch(Twitter tw, String json) {
		try {
			JSONObject searchResults = new JSONObject(json);
			List<Status> users = new ArrayList<Status>();
			JSONArray arr = searchResults.getJSONArray("results");
			for (int i = 0; i < arr.length(); i++) {
				JSONObject obj = arr.getJSONObject(i);
				String userScreenName = obj.getString("from_user");
				String profileImgUrl = obj.getString("profile_image_url");
				User user = new User(userScreenName);
				user.profileImageUrl = InternalUtils.URI(profileImgUrl);
				Status s = new Status(obj, user);
				users.add(s);
			}
			return users;
		} catch (JSONException e) {
			throw new TwitterException.Parsing(json, e);
		}
	}

	/**
	 * @param object
	 * @return place, location, failing which geo coordinates
	 * @throws JSONException
	 */
	static Object jsonGetLocn(JSONObject object) throws JSONException {
		String _location = InternalUtils.jsonGet("location", object);
		// no blank strings
		if (_location != null && _location.length() == 0) {
			_location = null;
		}
		JSONObject _place = object.optJSONObject("place");
		if (_location != null) {
			// normalise UT (UberTwitter?) locations
			Matcher m = InternalUtils.latLongLocn.matcher(_location);
			if (m.matches()) {
				_location = m.group(2) + "," + m.group(3);
			}
			return _location; // should we also check geo and place for extra
								// info??
		}
		// Twitter place
		if (_place != null) {
			Place place = new Place(_place);
			return place;
		}
		JSONObject geo = object.optJSONObject("geo");
		if (geo != null && geo != JSONObject.NULL) {
			JSONArray latLong = geo.getJSONArray("coordinates");
			_location = latLong.get(0) + "," + latLong.get(1);
		}
		// TODO place (when is this set?)
		return _location;
	}

	public final Date createdAt;

	private EnumMap<KEntityType, List<TweetEntity>> entities;

	private boolean favorited;

	/**
	 * Warning: use equals() not == to compare these!
	 */
	public final BigInteger id;

	/**
	 * Often null (even when this Status is a reply). This is the in-reply-to
	 * status id as reported by Twitter.
	 */
	public final BigInteger inReplyToStatusId;

	private String location;

	/**
	 * null, except for official retweets when this is the original retweeted
	 * Status.
	 */
	private Status original;

	private Place place;

	/**
	 * Represents the number of times a status has been retweeted using
	 * _new-style_ retweets. -1 if unknown.
	 */
	public final int retweetCount;
	boolean sensitive;

	/**
	 * E.g. "web" vs. "im"
	 * <p>
	 * "fake" if this Status was made locally or from an RSS feed rather than
	 * retrieved from Twitter json (as normal).
	 */
	public final String source;
	/** The actual status text. */
	public final String text;

	/**
	 * Rarely null.
	 * <p>
	 * When can this be null?<br>
	 * - If creating a "fake" tweet via
	 * {@link Status#Status(User, String, long, Date)} and supplying a null
	 * User!
	 */
	public final User user;

//	private String[] withheldIn;
	
//	/**
	// Should we have this??
//	 * @return usually null!
//	 * Otherwise, a list of country codes for where this tweet has been censored.
//	 */
//	public String[] getWithheldIn() {
//		return withheldIn;
//	}

	/**
	 * @param object
	 * @param user
	 *            Set when parsing the json returned for a User. null when
	 *            parsing the json returned for a Status.
	 * @throws TwitterException
	 */
	@SuppressWarnings("deprecation")
	Status(JSONObject object, User user) throws TwitterException {
		try {
			String _id = object.optString("id_str");
			id = new BigInteger(_id == "" ? object.get("id").toString() : _id);
			// retweet?
			JSONObject retweeted = object.optJSONObject("retweeted_status");
			if (retweeted != null) {
				original = new Status(retweeted, null);
			}
			// text!
			String _text = InternalUtils.jsonGet("text", object);
			// Twitter have started truncating RTs -- let's fix the text up if we can
			boolean truncated = object.optBoolean("truncated");
			String rtStart = null;
			if (truncated && original!=null && _text.startsWith("RT ")) {
				rtStart = "RT @"+original.getUser()+": ";
				_text = rtStart+original.getText();				
			} else {
				_text = InternalUtils.unencode(_text); // bugger - this screws up the indices in tweet entities
			}
			text = _text; 
			// date
			String c = InternalUtils.jsonGet("created_at", object);
			createdAt = InternalUtils.parseDate(c);
			// source - sometimes encoded (search), sometimes not
			// (timelines)!
			String src = InternalUtils.jsonGet("source", object);
			source = src.contains("&lt;") ? InternalUtils.unencode(src) : src;
			// threading
			String irt = InternalUtils.jsonGet("in_reply_to_status_id", object);
			if (irt == null) {
				// Twitter doesn't give in-reply-to for retweets
				// - but since we have the info, let's make it available
				inReplyToStatusId = original == null ? null : original.getId();
			} else {
				inReplyToStatusId = new BigInteger(irt);
			}
			favorited = object.optBoolean("favorited");

			// set user
			if (user != null) {
				this.user = user;
			} else {
				JSONObject jsonUser = object.optJSONObject("user");
				// null user happens in very rare circumstances, which I
				// have not pinned down yet.
				if (jsonUser == null) {
					this.user = null;
				} else if (jsonUser.length() < 3) {
					// TODO seen a bug where the jsonUser is just
					// {"id":24147187,"id_str":"24147187"}
					// Not sure when/why this happens
					String _uid = jsonUser.optString("id_str");
					BigInteger userId = new BigInteger(_uid == "" ? object.get(
							"id").toString() : _uid);
					try {
						user = new Twitter().show(userId);
					} catch (Exception e) {
						// ignore
					}
					this.user = user;
				} else {
					// normal JSON case
					this.user = new User(jsonUser, this);
				}

			}
			// location if geocoding is on
			Object _locn = Status.jsonGetLocn(object);
			location = _locn == null ? null : _locn.toString();
			if (_locn instanceof Place) {
				place = (Place) _locn;
			}

			retweetCount = object.optInt("retweet_count", -1);			
			
			// ignore this as it can be misleading: true is reliable, false isn't
			// retweeted = object.optBoolean("retweeted");
			
			// Entities (switched on by Twitter.setIncludeTweetEntities(true))
			JSONObject jsonEntities = object.optJSONObject("entities");
			// Note: Twitter filters out dud @names
			if (jsonEntities != null) {
				entities = new EnumMap<Twitter.KEntityType, List<TweetEntity>>(
						KEntityType.class);
				if (rtStart!=null) {
					// truncation! the entities returned are likely to be duds -- adjust from the original instead
					int rt = rtStart.length();
					for (KEntityType type : KEntityType.values()) {
						List<TweetEntity> es = original.getTweetEntities(type);
						if (es==null) continue;
						ArrayList rtEs = new ArrayList(es.size());
						for (TweetEntity e : es) {
							TweetEntity rte = new TweetEntity(this, e.type, 
									/* safety checks on length are paranoia (could be removed) */
									Math.min(rt+e.start, text.length()), Math.min(rt+e.end, text.length()), e.display);
							rtEs.add(rte);
						}
						entities.put(type, rtEs);
					}					
				} else {
					// normal case
					for (KEntityType type : KEntityType.values()) {
						List<TweetEntity> es = TweetEntity.parse(this, _text, type,
								jsonEntities);
						entities.put(type, es);
					}
				}								
			}
			
			// censorship flags
			// Should we have this??
//			String withheld = object.optString("withheld_in_countries");
//			if (withheld!=null && withheld.length()!=0) {
//				withheldIn = withheld.split(", ");
//			}
//			"withheld_scope": "status" or "user"
			sensitive = object.optBoolean("possibly_sensitive");
		} catch (JSONException e) {
			throw new TwitterException.Parsing(null, e);
		}
	}

	/**
	 * Create a *fake* Status object. This does not represent a real tweet!
	 * Uses: few and far between. There is no real contract as to how objects
	 * made in this way will behave.
	 * <p>
	 * If you want to post a tweet (and hence get a real Status object), use
	 * {@link Twitter#setStatus(String)}.
	 * 
	 * @param user
	 *            Can be null or bogus -- provided that's OK with your code.
	 * @param text
	 *            Can be null or bogus -- provided that's OK with your code.
	 * @param id
	 *            Can be null or bogus -- provided that's OK with your code.
	 * @param createdAt
	 *            Can be null -- provided that's OK with your code.
	 */
	@Deprecated
	public Status(User user, String text, Number id, Date createdAt) {
		this.text = text;
		this.user = user;
		this.createdAt = createdAt;
		this.id = id == null ? null
				: (id instanceof BigInteger ? (BigInteger) id : new BigInteger(
						id.toString()));
		inReplyToStatusId = null;
		source = FAKE;
		retweetCount = -1;
	}

	/**
	 * Tests by class=Status and tweet id number
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Status other = (Status) obj;
		return id.equals(other.id);
	}

	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * @return The Twitter id for this post. This is used by some API methods.
	 */
	@Override
	public BigInteger getId() {
		return id;
	}

	@Override
	public String getLocation() {
		return location;
	}

	/**
	 * @return list of \@mentioned people (there is no guarantee that these
	 *         mentions are for correct Twitter screen-names). May be empty,
	 *         never null. Screen-names are always lowercased -- unless
	 *         {@link Twitter#CASE_SENSITIVE_SCREENNAMES} is switched on.
	 */
	@Override
	public List<String> getMentions() {
		// TODO test & use this
		// List<TweetEntity> ms = entities.get(KEntityType.user_mentions);
		Matcher m = AT_YOU_SIR.matcher(text);
		List<String> list = new ArrayList<String>(2);
		while (m.find()) {
			// skip email addresses (and other poorly formatted things)
			if (m.start() != 0
					&& Character.isLetterOrDigit(text.charAt(m.start() - 1))) {
				continue;
			}
			String mention = m.group(1);
			// enforce lower case? (normally yes)
			if (!Twitter.CASE_SENSITIVE_SCREENNAMES) {
				mention = mention.toLowerCase();
			}
			list.add(mention);
		}
		return list;
	}

	/**
	 * Only set for official new-style retweets. This is the original retweeted
	 * Status. null otherwise.
	 */
	public Status getOriginal() {
		return original;
	}

	@Override
	public Place getPlace() {
		return place;
	}

	/** The actual status text. This is also returned by {@link #toString()}.
	 * NB: This can be longer than 140 chars for a retweet. */
	@Override
	public String getText() {
		return text;
	}

	@Override
	public List<TweetEntity> getTweetEntities(KEntityType type) {
		return entities == null ? null : entities.get(type);
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	/**
	 * true if this has been marked as a favourite by the authenticating user
	 */
	public boolean isFavorite() {
		return favorited;
	}

	/**
	 * A <i>self-applied</i> label for sensitive content (eg. X-rated images).
	 * Obviously, you can only rely on this label if the tweeter is reliably
	 * setting it.
	 * 
	 * @return true=kinky, false=family-friendly
	 */
	public boolean isSensitive() {
		return sensitive;
	}

	/**
	 * @return The text of this status. E.g. "Kicking fommil's arse at
	 *         Civilisation."
	 */

	@Override
	public String toString() {
		return text;
	}

	/**
	 * @return text, with the t.co urls replaced.
	 * Use-case: for filtering based on text contents, when we want to
	 * match against the full url.
	 * Note: this does NOT resolve short urls from bit.ly etc. 
	 */
	public String getDisplayText() {
		return getDisplayText2(this);
	}

	static String getDisplayText2(ITweet tweet) {
		List<TweetEntity> es = tweet.getTweetEntities(KEntityType.urls);
		String _text = tweet.getText();
		if (es==null || es.size()==0) return _text;
		StringBuilder sb = new StringBuilder(200);
		int i=0;
		for (TweetEntity entity : es) {
			sb.append(_text.substring(i, entity.start));
			sb.append(entity.displayVersion());
			i = entity.end;
		}					
		if (i < _text.length()) {
			sb.append(_text.substring(i));
		}
		return sb.toString();
	}
}