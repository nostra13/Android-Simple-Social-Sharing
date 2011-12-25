package com.nostra13.example.socialsharing;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.nostra13.example.socialsharing.Constants.Extra;
import com.nostra13.example.socialsharing.base.TwitterBaseActivity;
import com.nostra13.socialsharing.twitter.TwitterFacade;

/**
 * Activity for sharing information with Twitter
 * 
 * @author Sergey Tarasevich
 */
public class TwitterActivity extends TwitterBaseActivity {

	private TextView messageView;
	private Button postButton;

	private TwitterFacade twitter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.ac_twitter);

		Bundle bundle = getIntent().getExtras();
		final String message = bundle == null ? "" : bundle.getString(Extra.POST_MESSAGE);

		messageView = (TextView) findViewById(R.id.message);
		postButton = (Button) findViewById(R.id.button_post);

		messageView.setText(message);
		postButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (twitter.isAuthorized()) {
					twitter.publishMessage(messageView.getText().toString());
					finish();
				} else {
					twitter.authorize();
				}
			}
		});

		twitter = new TwitterFacade(this, Constants.TWITTER_CONSUMER_KEY, Constants.TWITTER_CONSUMER_SECRET);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!twitter.isAuthorized()) {
			twitter.authorize();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_facebook_twitter, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.item_logout:
				twitter.logout();
				return true;
			default:
				return false;
		}
	}
}
