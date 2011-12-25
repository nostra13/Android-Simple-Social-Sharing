package com.nostra13.example.socialsharing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.nostra13.example.socialsharing.Constants.Extra;
import com.nostra13.socialsharing.common.PostListener;
import com.nostra13.socialsharing.facebook.FacebookEvents;
import com.nostra13.socialsharing.twitter.TwitterEvents;

/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class HomeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.ac_social_sharing);
	}

	@Override
	public void onStart() {
		super.onStart();
		FacebookEvents.addPostListener(facebookPostListener);
		TwitterEvents.addPostListener(twitterPostListener);
	}

	@Override
	protected void onStop() {
		super.onStop();
		FacebookEvents.removePostListener(facebookPostListener);
		TwitterEvents.removePostListener(twitterPostListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_home, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.item_share_facebook:
				startFacebookActivity();
				return true;
			case R.id.item_share_twitter:
				startTwitterActivity();
				return true;
			default:
				return false;
		}
	}

	private void startFacebookActivity() {
		Intent intent = new Intent(this, FacebookActivity.class);
		intent.putExtra(Extra.POST_MESSAGE, Constants.FACEBOOK_SHARE_MESSAGE);
		intent.putExtra(Extra.POST_LINK, Constants.FACEBOOK_SHARE_LINK);
		intent.putExtra(Extra.POST_LINK_NAME, Constants.FACEBOOK_SHARE_LINK_NAME);
		intent.putExtra(Extra.POST_LINK_DESCRIPTION, Constants.FACEBOOK_SHARE_LINK_DESCRIPTION);
		startActivity(intent);
	}

	private void startTwitterActivity() {
		Intent intent = new Intent(this, TwitterActivity.class);
		intent.putExtra(Extra.POST_MESSAGE, Constants.TWITTER_SHARE_MESSAGE);
		startActivity(intent);
	}

	private PostListener facebookPostListener = new PostListener() {

		@Override
		public void onPostPublishingFailed() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(HomeActivity.this, R.string.facebook_post_publishing_failed, Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public void onPostPublished() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(HomeActivity.this, R.string.facebook_post_published, Toast.LENGTH_SHORT).show();
				}
			});
		}
	};

	private PostListener twitterPostListener = new PostListener() {
		@Override
		public void onPostPublishingFailed() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(HomeActivity.this, R.string.twitter_post_publishing_failed, Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public void onPostPublished() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(HomeActivity.this, R.string.twitter_post_published, Toast.LENGTH_SHORT).show();
				}
			});
		}
	};
}