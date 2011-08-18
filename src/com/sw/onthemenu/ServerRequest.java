package com.sw.onthemenu;

import java.net.URL;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

interface ServerRequestCallback {

	public void done(String content);

	public void fail();
}

public class ServerRequest extends AsyncTask<String, Void, String> {
	public static String SERVER_URL = "http://floating-moon-709.heroku.com";
	ServerRequestCallback callback;
	private String url;

	public ServerRequest(ServerRequestCallback callback) {
		super();
		this.callback = callback;
	}

	protected String doInBackground(String... urls) {
		url = urls[0];
		String result = null;
		HttpClient httpclient = new DefaultHttpClient();

		HttpGet request = new HttpGet(url);
		Log.v(ServerRequest.class.toString(), "Call to " + url);
		ResponseHandler<String> handler = new BasicResponseHandler();

		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 6000);
		HttpConnectionParams.setSoTimeout(httpParameters, 6000);

		request.setParams(httpParameters);
		try {
			result = httpclient.execute(request, handler);
		} catch (Exception e) {
			return null;
		}
		httpclient.getConnectionManager().shutdown();
		return result;
	}

	protected void onPostExecute(String result) {
		Log.v(ServerRequest.class.toString(), "Response for " + url + " :\n" + result);
		if ((result == null) || (result.equalsIgnoreCase("fail"))) {
			if (callback != null) {
				Log.e(ServerRequest.class.toString(), "Call to fail method");
				callback.fail();
			}
			return;
		} else {
			Log.v(ServerRequest.class.toString(), "Call to success method");
			callback.done(result);
		}
	}
}