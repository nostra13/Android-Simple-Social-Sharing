package com.nostra13.example.socialsharing;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.nostra13.example.socialsharing.Constants.Extra;
import com.nostra13.example.socialsharing.assist.FacebookEventObserver;
import com.nostra13.socialsharing.common.AuthListener;
import com.nostra13.socialsharing.facebook.FacebookFacade;

/**
 * Activity for sharing information with Facebook
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class FacebookActivity extends Activity {

	private FacebookFacade facebook;
	private FacebookEventObserver facebookEventObserver;

	private TextView messageView;

	private String link;
	private String linkName;
	private String linkDescription;
	private String picture;
	private Map<String, String> actionsMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.ac_facebook);

		facebook = new FacebookFacade(this, Constants.FACEBOOK_APP_ID);
		facebookEventObserver = FacebookEventObserver.newInstance();

		messageView = (TextView) findViewById(R.id.message);
		TextView linkNameView = (TextView) findViewById(R.id.link_name);
		TextView linkDescriptionView = (TextView) findViewById(R.id.link_description);
		Button postButton = (Button) findViewById(R.id.button_post);
		Button postImageButton = (Button) findViewById(R.id.button_post_image);

		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			String message = bundle.getString(Extra.POST_MESSAGE);
			link = bundle.getString(Extra.POST_LINK);
			linkName = bundle.getString(Extra.POST_LINK_NAME);
			linkDescription = bundle.getString(Extra.POST_LINK_DESCRIPTION);
			picture = bundle.getString(Extra.POST_PICTURE);
			actionsMap = new HashMap<String, String>();
			actionsMap.put(Constants.FACEBOOK_SHARE_ACTION_NAME, Constants.FACEBOOK_SHARE_ACTION_LINK);

			messageView.setText(message);
			linkNameView.setText(linkName);
			linkDescriptionView.setText(linkDescription);
		}

		postButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (facebook.isAuthorized()) {
					publishMessage();
					finish();
				} else {
					// Start authentication dialog and publish message after successful authentication
					facebook.authorize(new AuthListener() {
						@Override
						public void onAuthSucceed() {
							publishMessage();
							finish();
						}

						@Override
						public void onAuthFail(String error) { // Do noting
						}
					});
				}
			}
		});
		postImageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (facebook.isAuthorized()) {
					publishImage();
					finish();
				} else {
					// Start authentication dialog and publish image after successful authentication
					facebook.authorize(new AuthListener() {
						@Override
						public void onAuthSucceed() {
							publishImage();
							finish();
						}

						@Override
						public void onAuthFail(String error) { // Do noting
						}
					});
				}
			}
		});
	}

	private void publishMessage() {
		facebook.publishMessage(messageView.getText().toString(), link, linkName, linkDescription, picture, actionsMap);
	}

	private void publishImage() {
		Bitmap bmp = ((BitmapDrawable) getResources().getDrawable(R.drawable.ic_app)).getBitmap();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		byte[] bitmapdata = stream.toByteArray();
		facebook.publishImage(bitmapdata, Constants.FACEBOOK_SHARE_IMAGE_CAPTION);
	}

	@Override
	public void onStart() {
		super.onStart();
		facebookEventObserver.registerListeners(this);
		if (!facebook.isAuthorized()) {
			facebook.authorize();
		}
	}

	@Override
	public void onStop() {
		facebookEventObserver.unregisterListeners();
		super.onStop();
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
				facebook.logout();
				return true;
			default:
				return false;
		}
	}
}
