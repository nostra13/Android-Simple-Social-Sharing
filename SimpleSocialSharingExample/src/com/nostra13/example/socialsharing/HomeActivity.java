package com.nostra13.example.socialsharing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
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

		setContentView(R.layout.ac_home);
		findViewById(R.id.button_share_facebook).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startFacebookActivity();
			}
		});
		findViewById(R.id.button_share_twitter).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startTwitterActivity();
			}
		});
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