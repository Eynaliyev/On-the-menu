package com.sw.onthemenu;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesResult;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

import utils.actionbar.ActionBar;
import utils.actionbar.ActionBar.Action;
import utils.imageloader.ImageLoader;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Meal extends Activity {
	MealObject meal;

	@Override
	protected void onResume() {
		((App) getApplication()).getTracker(this).trackPageView("F/Meal");
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.meal_big);
		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);

		actionBar.setHomeAction(new Action() {

			@Override
			public void performAction(View view) {
				Intent intent = new Intent(Meal.this, DashboardF.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(intent);
			}

			@Override
			public int getDrawable() {
				return R.drawable.home_icon;
			}
		});
		actionBar.addAction(new Action() {

			@Override
			public void performAction(View view) {
				Intent intent = new Intent(Meal.this, Menu.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(intent);
			}

			@Override
			public int getDrawable() {
				return R.drawable.list;
			}
		});
		actionBar.setTitle("Buy a man");
		Intent intent = this.getIntent();
		meal = (MealObject) intent.getSerializableExtra("meal");
		((TextView) findViewById(R.id.mealName)).setText(meal.getName());
		String timestamp = getSharedPreferences("ordered", 0).getString(meal.getId() + "", null);
		if (timestamp != null) {
			Date date = new Date(Long.parseLong(timestamp));
			// reset all hours mins and secs to zero on start date
			Calendar startCal = GregorianCalendar.getInstance();
			startCal.setTime(date);
			startCal.set(Calendar.HOUR_OF_DAY, 0);
			startCal.set(Calendar.MINUTE, 0);
			startCal.set(Calendar.SECOND, 0);
			long startTime = startCal.getTimeInMillis();

			Calendar endCal = GregorianCalendar.getInstance();
			endCal.setTime(new Date());
			endCal.set(Calendar.HOUR_OF_DAY, 0);
			endCal.set(Calendar.MINUTE, 0);
			endCal.set(Calendar.SECOND, 0);
			long endTime = endCal.getTimeInMillis();

			int hours = (int) ((endTime - startTime) / 1000 * 60 * 60 * 3);
			if (hours > 3) {
				Editor editor = getSharedPreferences("ordered", 0).edit();
				editor.remove(meal.getId() + "");
			}
		}

		String url = meal.getAppPicture();
		if (url == null) {
			url = meal.getFoursquarePicture();
		}
		(new ImageLoader(this, -1, new ImageLoader.ImageLoadedCallback() {

			@Override
			public void fail(String url) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						findViewById(R.id.progress).setVisibility(View.GONE);
						Toast.makeText(Meal.this, getString(R.string.error_local), Toast.LENGTH_LONG);
					}
				});
			}

			@Override
			public void down(String url) {
				findViewById(R.id.progress).setVisibility(View.GONE);
			}
		}, findViewById(R.id.mealAvatar).getWidth(), findViewById(R.id.mealAvatar).getHeight())).DisplayImage(url + "?" + new GregorianCalendar().get(Calendar.DAY_OF_YEAR), this, ((ImageView) findViewById(R.id.mealAvatar)));

	}

	public void onOrderClick(View v) {
		Boolean isFirstBuy = getSharedPreferences("app", 0).getBoolean("firstOrder", true);
		if (isFirstBuy) {
			firstBuy();
		} else {
			order();
		}
	}

	public void firstBuy() {

		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.first_order);
		dialog.findViewById(R.id.cancelButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(Meal.this, "Order cancelled", Toast.LENGTH_SHORT).show();
				dialog.dismiss();

			}
		});
		dialog.findViewById(R.id.okButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				order();
				dialog.dismiss();
			}
		});
		dialog.show();
	}

	public void order() {
		Toast.makeText(Meal.this, "Ordered", Toast.LENGTH_SHORT).show();
		Editor editor = getSharedPreferences("ordered", 0).edit();
		editor.putString(meal.getId() + "", (new Date()).getTime() + "");
		editor.commit();

		(new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				AmazonSimpleDBClient sdb = new AmazonSimpleDBClient(new BasicAWSCredentials(getString(R.string.aws_access_key), getString(R.string.aws_secret_key)));
				GetAttributesRequest getRequest = new GetAttributesRequest("users", "M" + meal.getId()).withConsistentRead(true);
				GetAttributesResult getResult = sdb.getAttributes(getRequest);
				String key = "";
				List a = getResult.getAttributes();
				for (Object b : a) {
					if (((Attribute) b).getName().equalsIgnoreCase("C2DM")) {
						key = ((Attribute) b).getValue();
						break;
					}
				}
				String url = meal.getAppPicture();
				if (url == null) {
					url = meal.getFoursquarePicture();
				}
				new C2DMRequest(new ServerRequestCallback() {

					@Override
					public void fail() {
						Editor editor = getSharedPreferences("ordered", 0).edit();
						editor.remove(meal.getId() + "");
						editor.commit();

					}

					@Override
					public void done(String content) {
						Log.e("", "content" + content);
					}

				}).execute(key, meal.getId(), url, meal.getName());
				return null;

			}

		}).execute();

		Editor appEditor = getSharedPreferences("app", 0).edit();
		appEditor.putBoolean("firstOrder", false);
		// appEditor.commit();

		Intent intent = new Intent(Meal.this, Menu.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);
	}
}
