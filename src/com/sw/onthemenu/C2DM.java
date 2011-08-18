package com.sw.onthemenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

import utils.Utils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

public class C2DM extends BroadcastReceiver {

	
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
			handleRegistration(context, intent);
		} else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
			handleMessage(context, intent);
		}
	}

	private void handleMessage(final Context context, final Intent intent) {

		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);

		int icon = R.drawable.ic_stat_notification_icon;
		CharSequence tickerText = "Hello";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		CharSequence contentTitle = intent.getStringExtra("name");
		CharSequence contentText = "Check out this new client for 15s";
		Log.i(C2DM.class.toString(),"building notification intent url="+intent.getStringExtra("url")+" name="+intent.getStringExtra("name")+" id="+intent.getStringExtra("id")+"");
		Intent notificationIntent = new Intent(context, Order.class);
		notificationIntent.putExtra("id", intent.getStringExtra("id"));
		notificationIntent.putExtra("name", intent.getStringExtra("name"));
		notificationIntent.putExtra("url", intent.getStringExtra("url"));
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		mNotificationManager.notify(new Random().nextInt(), notification);

	}

	private void handleRegistration(Context context, Intent intent) {
		Log.e("", "registrationDone");
		AmazonSimpleDBClient sdb = new AmazonSimpleDBClient(new BasicAWSCredentials(context.getString(R.string.aws_access_key), context.getString(R.string.aws_secret_key)));
		String registration = intent.getStringExtra("registration_id");
		Log.e("", "registrationDone " + registration);
		if (intent.getStringExtra("error") != null) {
			Toast.makeText(context, "Can't access the web, please restart check your connectivity and restart the app", Toast.LENGTH_LONG);
			Log.e("", "fail");
		} else if (intent.getStringExtra("unregistered") != null) {
			Log.e("C2DM unregistered", registration);
		} else if (registration != null) {
			Log.e("C2DM registered", registration);
			List<ReplaceableAttribute> attributes = new ArrayList<ReplaceableAttribute>(1);
			attributes.add(new ReplaceableAttribute().withName("C2DM").withValue(registration));
			sdb.putAttributes(new PutAttributesRequest("users", "M" + context.getSharedPreferences("appM", 0).getString("userId", "--"), attributes));
		}
	}
}
