package com.sw.onthemenu;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesResult;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

import utils.actionbar.ActionBar;
import utils.imageloader.ImageLoader;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Order extends Activity {
	int progress;
	private ProgressBar progressBar;
	private Timer t;
	private ImageLoader imageLoader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		((App) getApplication()).getTracker(this).trackPageView("M/Order");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.order);
		progressBar = (ProgressBar) findViewById(R.id.progress_bar);
		progressBar.setMax(5000);
		progressBar.setProgress(5000);
		AlphaAnimation fadeIn = new AlphaAnimation(1, 0);
		fadeIn.setDuration(0);
		fadeIn.setFillAfter(true);
		findViewById(R.id.doneLayer).startAnimation(fadeIn);

		final ImageView avatar = (ImageView) findViewById(R.id.mealAvatar);

		imageLoader = new ImageLoader(this, -1, new ImageLoader.ImageLoadedCallback() {
			@Override
			public void fail(String url) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						findViewById(R.id.progress).setVisibility(View.GONE);
					}
				});
				final AlertDialog.Builder dialog = new AlertDialog.Builder(Order.this);

				dialog.setTitle("An error occured");
				dialog.setMessage(Order.this.getString(R.string.error_network));
				dialog.setCancelable(false);
				dialog.setPositiveButton("Try again", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								findViewById(R.id.progress).setVisibility(View.VISIBLE);
							}
						});
						imageLoader.DisplayImage(Order.this.getIntent().getStringExtra("url") + "?" + new GregorianCalendar().get(Calendar.DAY_OF_YEAR), Order.this, avatar);
					}
				});
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						dialog.show();

					}
				});
			}

			@Override
			public void down(String url) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						findViewById(R.id.progress).setVisibility(View.GONE);
					}
				});
				startTimer();
			}
		},100,100);
		imageLoader.DisplayImage(this.getIntent().getStringExtra("url"), this, avatar);
		TextView name = (TextView) findViewById(R.id.mealName);
		name.setText(getIntent().getStringExtra("name"));

	}

	public void startTimer() {
		findViewById(R.id.progress).setVisibility(View.GONE);
		progressBar = (ProgressBar) findViewById(R.id.progress_bar);
		progressBar.setMax(5000);
		progress = 5000;
		t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if (progress >= 0) {
					progressBar.setProgress(progress);
					progress -= 1;
				} else {
					Log.e("FINISH", "FINISH");
					finish();
					t.cancel();
				}
			}
		}, 0, 1);
	}

	public void finish() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				AlphaAnimation fadeIn = new AlphaAnimation(0f, 0.95f);
				fadeIn.setDuration(500);
				fadeIn.setFillAfter(true);
				findViewById(R.id.doneLayer).startAnimation(fadeIn);
				AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
				fadeOut.setDuration(500);
				fadeOut.setFillAfter(true);
				findViewById(R.id.mealAvatar).startAnimation(fadeOut);
				findViewById(R.id.nameContainer).startAnimation(fadeOut);
				findViewById(R.id.bottomBar).startAnimation(fadeOut);

			}
		});

	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}
}
