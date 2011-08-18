package com.sw.onthemenu;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;



import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;


public class C2DMRequest extends AsyncTask<String, Void, String> {
	ServerRequestCallback callback;
	

	public C2DMRequest(ServerRequestCallback callback) {
		super();
		this.callback = callback;
	}

	protected String doInBackground(String... keys) {
		String key = keys[0];
		String id = keys[1];
		String url = keys[2];
		String name = keys[3];
		String result = null;
		HttpClient httpclient = new DefaultHttpClient();
		
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("registration_id", key));
		formparams.add(new BasicNameValuePair("data.id", id));
		formparams.add(new BasicNameValuePair("data.name", name));
		formparams.add(new BasicNameValuePair("data.url", url));
		formparams.add(new BasicNameValuePair("collapse_key", "test"));

		// set up httppost
		UrlEncodedFormEntity entity;
		try {
			entity = new UrlEncodedFormEntity(formparams, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			Log.e(C2DMRequest.class.toString(), "",e1);
			
			return null;
		}
		Log.i(C2DMRequest.class.toString(),"request made with: r_id="+key+" id="+id+" name="+name+" url="+url);
		HttpPost request = new HttpPost("https://android.apis.google.com/c2dm/send");
		request.setHeader("Authorization","GoogleLogin auth=DQAAALEAAACAyd7S-g3YuuYbW9rkZnfgwt23tSPvfL1kNeKTU1plxn_fbk97VK6-XxagowVJQ4ZnoVGThgvM9CacVoMTqiiceSgAkMzb4ZR7Vn7xcsIcBaAcwQyjf4NKAYoLbPc68lQ-3wuOnLiBxvPF4iVBns64wSJ79lAlDHOmNYIwc5sCMnrCH9DwbGx5XcvGbOj35BOm25_aN8otGZgocr2SaXeAJaFh2Tsr9Vh9JrPt67GZo_TMCALW_jfmFlqQ8Jvni_k");
		request.setEntity(entity);
		
		ResponseHandler<String> handler = new BasicResponseHandler();
		
		try {
			result = httpclient.execute(request, handler);
		} catch (Exception e) {
			return null;
		}
		httpclient.getConnectionManager().shutdown();
		return result;
	}

	protected void onPostExecute(String result) {
		Log.v(C2DMRequest.class.toString(), "Response for C2DM :\n" + result);
		if ((result == null) || (result.equalsIgnoreCase("fail"))) {
			if (callback != null) {
				Log.e(C2DMRequest.class.toString(), "Call to fail method");
				callback.fail();
			}
			return;
		} else {
			Log.v(C2DMRequest.class.toString(), "Call to success method");
			callback.done(result);
		}
	}
}