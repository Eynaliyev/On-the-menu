package com.sw.onthemenu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import utils.Utils;
import utils.actionbar.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesResult;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sw.onthemenu.R.string;

public class DashboardF extends Activity {

	GoogleAnalyticsTracker tracker;

	@Override
	protected void onResume() {
		Log.v(DashboardF.class.toString(), "Resume app");
		((App) getApplication()).getTracker(this).trackPageView("F/Dashboard");
		super.onResume();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dashboardf);

		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar.setTitle(R.string.app_name);
		actionBar.addAction(new ActionBar.Action() {

			@Override
			public void performAction(View view) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://m.foursquare.com/places"));
				startActivity(intent);
			}

			@Override
			public int getDrawable() {
				return R.drawable.foursquare_icon;
			}
		});

		Uri uri = this.getIntent().getData();
		if (uri != null) {
			Log.i(DashboardF.class.toString(), "URL in the intent");
			final ProgressDialog progressDialog = ProgressDialog.show(this, "", "Loading...", true);
			String code = uri.getQueryParameter("code");
			if (code != null) {
				Log.v(DashboardF.class.toString(), "Code present in the URL");
				new ServerRequest(new ServerRequestCallback() {

					@Override
					public void fail() {
						progressDialog.dismiss();
						Log.e(DashboardF.class.toString(), "Not a valid code");

						AlertDialog.Builder builder = new AlertDialog.Builder(DashboardF.this);
						builder.setMessage(getString(R.string.error_foursquare)).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								requestedFoursquareLink();
							}
						});
						AlertDialog alert = builder.create();
						alert.setIcon(R.drawable.icon);
						alert.show();

					}

					@Override
					public void done(String content) {
						String token = "";
						try {
							JSONObject json = new JSONObject(content);
							token = json.getString("access_token");
						} catch (JSONException e) {
							Log.e(DashboardF.class.toString(), "Not a valid JSON response", e);
							AlertDialog.Builder builder = new AlertDialog.Builder(DashboardF.this);
							builder.setMessage(getString(R.string.error_foursquare)).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									requestedFoursquareLink();
								}
							});
							AlertDialog alert = builder.create();
							alert.setIcon(R.drawable.icon);
							alert.show();
							return;
						}
						Editor editor = getSharedPreferences("appF", 0).edit();
						editor.putString("token", token);
						editor.commit();

						new ServerRequest(new ServerRequestCallback() {

							@Override
							public void fail() {
								Toast.makeText(DashboardF.this, getString(string.error_foursquare), Toast.LENGTH_LONG);
								Log.e(Menu.class.toString(), "An error occured when trying to get the user from Foursquare");
							}

							@Override
							public void done(String content) {
								JSONObject json;
								try {
									json = new JSONObject(content);
								} catch (JSONException e) {
									Log.e(Menu.class.toString(), "Fail parsing the JSON response", e);
									((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
									Toast.makeText(DashboardF.this, getString(R.string.error_local), Toast.LENGTH_LONG).show();
									return;
								}
								int returnCode = 0;
								try {
									returnCode = json.getJSONObject("meta").getInt("code");
								} catch (JSONException e) {
									Log.e(Menu.class.toString(), "No meta code", e);
									((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
									Toast.makeText(DashboardF.this, getString(R.string.error_local), Toast.LENGTH_LONG).show();
									return;
								}

								if (returnCode != 200) {
									Log.e(Menu.class.toString(), "Code != 200");
									((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
									Toast.makeText(DashboardF.this, getString(R.string.error_local), Toast.LENGTH_LONG).show();
									return;
								}

								final String userId;
								try {
									userId = json.getJSONObject("response").getJSONObject("user").getString("id");
								} catch (JSONException e) {
									Log.e(Menu.class.toString(), "Canmt get the user ID", e);
									((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
									Toast.makeText(DashboardF.this, getString(R.string.error_local), Toast.LENGTH_LONG).show();
									return;
								}
								final String foursquarePorfilePicURL;
								try {
									foursquarePorfilePicURL = json.getJSONObject("response").getJSONObject("user").getString("photo");
								} catch (JSONException e) {
									Log.e(Menu.class.toString(), "Can't get the user ID", e);
									((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
									Toast.makeText(DashboardF.this, getString(R.string.error_local), Toast.LENGTH_LONG).show();
									return;
								}

								Editor editor = getSharedPreferences("appF", 0).edit();
								editor.putString("userId", userId);
								editor.commit();
								new AsyncTask<Void, Void, Void>() {

									@Override
									protected Void doInBackground(Void... params) {
										AmazonSimpleDBClient sdb = new AmazonSimpleDBClient(new BasicAWSCredentials(getString(R.string.aws_access_key), getString(R.string.aws_secret_key)));
										List<ReplaceableAttribute> attributes = new ArrayList<ReplaceableAttribute>(1);
										attributes.add(new ReplaceableAttribute().withName("foursquarePic").withValue(foursquarePorfilePicURL));
										attributes.add(new ReplaceableAttribute().withName("appPic").withValue("none"));
										sdb.putAttributes(new PutAttributesRequest("users", "F" + userId, attributes));
										Log.i(DashboardF.class.toString(), "Profile URL update to " + foursquarePorfilePicURL);
										progressDialog.dismiss();
										return null;
									}

								}.execute();

							}
						}).execute("https://api.foursquare.com/v2/users/self?oauth_token=" + getSharedPreferences("appF", 0).getString("token", "no"));
					}
				}).execute("https://foursquare.com/oauth2/access_token?client_id=" + getString(R.string.foursquare_client_id) + "&client_secret=" + getString(R.string.foursquare_client_secret) + "&grant_type=authorization_code&redirect_uri=foursquare-oauth-callback://f&code=" + code);

			} else {
				progressDialog.dismiss();
				Log.e(DashboardF.class.toString(), "No code in the URL");
				AlertDialog.Builder builder = new AlertDialog.Builder(DashboardF.this);
				builder.setMessage(getString(R.string.error_local)).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Log.i(DashboardF.class.toString(), "Request Foursquare auth again.");
						requestedFoursquareLink();
					}
				});
				AlertDialog alert = builder.create();
				alert.setIcon(R.drawable.iconf);
				alert.show();
			}
		} else {
			String token = getSharedPreferences("appF", 0).getString("token", null);
			if (token == null) {
				reset();
				this.requestedFoursquareLink();
			}
		}

	}

	private void requestedFoursquareLink() {
		final Dialog dialog = new Dialog(this);

		dialog.setContentView(R.layout.foursquare_request_dialog);
		dialog.setTitle("Login to Foursquare");

		dialog.setCancelable(false);
		ImageButton button = (ImageButton) dialog.findViewById(R.id.button);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://foursquare.com/oauth2/authenticate?client_id=" + getString(R.string.foursquare_client_id) + "&response_type=code&redirect_uri=foursquare-oauth-callback://f"));
				((App) getApplication()).getTracker(DashboardF.this).trackPageView("F/foursquare_request");
				startActivity(intent);
				dialog.dismiss();
			}
		});
		dialog.show();

	}

	public void loadMenu(View v) {
		Intent menuIntent = new Intent();
		menuIntent.setClass(this, Menu.class);
		menuIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(menuIntent);
	}

	public void loadProfile(View v) {
		Intent profileIntent = new Intent();
		profileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		profileIntent.setClass(this, ProfileF.class);
		startActivity(profileIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		menu.add(android.view.Menu.NONE, android.view.Menu.NONE, android.view.Menu.NONE, "Reset");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		((App) getApplication()).getTracker(this).trackPageView("F/Reset");
		final ProgressDialog progressDialog = ProgressDialog.show(DashboardF.this, "", "Loading...", true);
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {

				DashboardF.this.reset();
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						progressDialog.dismiss();

					}
				});

				Intent i = new Intent(DashboardF.this, DashboardF.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);

				return null;
			}
		}.execute();

		return true;
	}

	public void reset() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		if ((mExternalStorageAvailable == false) || (mExternalStorageWriteable == false)) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(DashboardF.this, getString(R.string.error_local), Toast.LENGTH_LONG).show();
				}
			});
		}
		File tmpFile = new File(getExternalFilesDir(null) + File.separator + "profileF.png.tmp");
		if (tmpFile.exists()) {
			tmpFile.delete();
		}

		tmpFile = new File(getExternalFilesDir(null) + File.separator + "profileF.png");
		if (tmpFile.exists()) {
			tmpFile.delete();
		}
		getSharedPreferences("ordered", 0).edit().clear().commit();
		getSharedPreferences("appF", 0).edit().clear().commit();

	}

}
