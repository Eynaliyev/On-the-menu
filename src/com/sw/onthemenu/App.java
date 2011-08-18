package com.sw.onthemenu;

import android.app.Application;
import android.content.Context;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.sw.onthemenu.R;

public class App extends Application {

	private String uuid;
	private GoogleAnalyticsTracker tracker;
	
	
	public GoogleAnalyticsTracker getTracker(Context c) {
		if (tracker==null){
			tracker = GoogleAnalyticsTracker.getInstance();
			tracker.start(c.getString(R.string.analytics_code),10,this);
		}
		return tracker;
	}


	public void setTracker(GoogleAnalyticsTracker tracker) {
		this.tracker = tracker;
	}


	public String getUuid() {
		return uuid;
	}

	public void setUuid(String s) {
		uuid = s;
	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
}