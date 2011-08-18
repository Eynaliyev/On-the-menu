package com.sw.onthemenu;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesResult;

import utils.Utils;
import utils.actionbar.ActionBar;
import utils.actionbar.ActionBar.Action;
import utils.imageloader.ImageLoader;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Menu extends Activity {

	private AmazonSimpleDBClient sdb;
	private int itemsToWait;
	final List<MealObject> ids = new ArrayList<MealObject>();

	@Override
	protected void onResume() {
		((App) getApplication()).getTracker(this).trackPageView("F/Menu");
		super.onResume();
		final GridView gridview = (GridView) findViewById(R.id.menuGridView);
		gridview.invalidateViews();
	}

	public void onCreate(Bundle savedInstanceState) {
		sdb = new AmazonSimpleDBClient(new BasicAWSCredentials(getString(R.string.aws_access_key), getString(R.string.aws_secret_key)));
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.menu);
		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);

		actionBar.addAction(new Action() {

			@Override
			public void performAction(View view) {
				((LinearLayout) findViewById(R.id.progress)).setVisibility(View.VISIBLE);
				refreshList();
			}

			@Override
			public int getDrawable() {
				return R.drawable.refresh_icon;
			}
		});
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
		actionBar.setHomeAction(new Action() {

			@Override
			public void performAction(View view) {
				Intent intent = new Intent(Menu.this, DashboardF.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(intent);
			}

			@Override
			public int getDrawable() {
				return R.drawable.home_icon;
			}
		});
		actionBar.setTitle(getString(R.string.app_name));

		final GridView gridview = (GridView) findViewById(R.id.menuGridView);
		int width = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
		Resources r = getResources();
		int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, r.getDisplayMetrics());

		int columnWidth = (int) ((width - 4 * margin) / 2);
		int columnHeight = (int) (0.70 * columnWidth);
		Log.v(Menu.class.toString(), "Use " + columnWidth + "x" + columnHeight + "for the item size");
		gridview.setColumnWidth(columnWidth);
		gridview.setVerticalSpacing(margin);

		final MenuAdapter adapter = new MenuAdapter(this, columnWidth, columnHeight);
		gridview.setAdapter(adapter);
		gridview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

				List<MealObject> meals = ((MenuAdapter) gridview.getAdapter()).getMeals();
				MealObject meal = meals.get(arg2);

				Intent intent = new Intent(Menu.this, Meal.class);
				intent.putExtra("meal", meal);
				startActivity(intent);
			}
		});
		refreshList();
	}

	public void refreshList() {
		findViewById(R.id.nomeals).setVisibility(View.GONE);
		new ServerRequest(new ServerRequestCallback() {

			@Override
			public void fail() {
				((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
				Toast.makeText(Menu.this, getString(R.string.error_foursquare), Toast.LENGTH_LONG).show();
				Log.e(Menu.class.toString(), "Can't get the last check in of the user.");
			}

			@Override
			public void done(String content) {

				JSONObject json;
				try {
					json = new JSONObject(content);
				} catch (JSONException e) {
					Log.e(Menu.class.toString(), "Fail parsing the JSON response", e);
					((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
					Toast.makeText(Menu.this, getString(R.string.error_foursquare), Toast.LENGTH_LONG).show();
					return;
				}
				int returnCode = 0;
				try {
					returnCode = json.getJSONObject("meta").getInt("code");
				} catch (JSONException e) {
					Log.e(Menu.class.toString(), "No meta code", e);
					((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
					Toast.makeText(Menu.this, getString(R.string.error_foursquare), Toast.LENGTH_LONG).show();
					return;
				}

				if (returnCode != 200) {
					Log.e(Menu.class.toString(), "Code != 200");
					((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
					Toast.makeText(Menu.this, getString(R.string.error_foursquare), Toast.LENGTH_LONG).show();
					return;
				}
				int count = 0;
				try {
					count = json.getJSONObject("response").getJSONObject("user").getJSONObject("checkins").getInt("count");
				} catch (JSONException e) {
					Log.e(Menu.class.toString(), "Can't get the check ins count", e);
					((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
					Toast.makeText(Menu.this, getString(R.string.error_foursquare), Toast.LENGTH_LONG).show();
					return;
				}

				if (count == 0) {
					AlertDialog.Builder builder = new AlertDialog.Builder(Menu.this);
					builder.setMessage("You need at least one check in !").setCancelable(true).setPositiveButton("Open Foursquare", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://m.foursquare.com"));
							startActivity(intent);
							dialog.dismiss();
						}
					}).show();
					return;
				}
				int createdAt = 0;
				String venueId = "";
				try {
					createdAt = ((JSONObject) json.getJSONObject("response").getJSONObject("user").getJSONObject("checkins").getJSONArray("items").get(0)).getInt("createdAt");
					venueId = ((JSONObject) json.getJSONObject("response").getJSONObject("user").getJSONObject("checkins").getJSONArray("items").get(0)).getJSONObject("venue").getString("id");
				} catch (JSONException e) {
					Log.e(Menu.class.toString(), "Can't get the venue ID of the date of the last check in", e);
					((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
					Toast.makeText(Menu.this, getString(R.string.error_foursquare), Toast.LENGTH_LONG).show();
					return;
				}
				new ServerRequest(new ServerRequestCallback() {

					@Override
					public void fail() {
						((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
						Log.e(Menu.class.toString(), "Can't get the venue object");
						Toast.makeText(Menu.this, getString(R.string.error_foursquare), Toast.LENGTH_LONG).show();
					}

					@Override
					public void done(String content) {
						Log.e("", content + "");

						JSONObject json = null;
						try {
							json = new JSONObject(content);
						} catch (JSONException e) {
							Log.e(Menu.class.toString(), "Fail parsing the JSON response", e);

							Toast.makeText(Menu.this, getString(R.string.error_foursquare), Toast.LENGTH_LONG).show();
						}
						int returnCode = 0;
						try {
							returnCode = json.getJSONObject("meta").getInt("code");
						} catch (JSONException e) {
							Log.e(Menu.class.toString(), "Can't get the meta code", e);
							Toast.makeText(Menu.this, getString(R.string.error_foursquare), Toast.LENGTH_LONG).show();
							return;
						}
						if (returnCode != 200) {
							Log.e(Menu.class.toString(), "Code !=200");
							Toast.makeText(Menu.this, getString(R.string.error_foursquare), Toast.LENGTH_LONG).show();
							return;
						}
						final JSONArray items;
						try {
							items = json.getJSONObject("response").getJSONObject("hereNow").getJSONArray("items");
						} catch (JSONException e) {
							Log.e(Menu.class.toString(), "Can't get the meta code", e);
							Toast.makeText(Menu.this, getString(R.string.error_foursquare), Toast.LENGTH_LONG).show();
							return;
						}

						itemsToWait = items.length();

						ids.clear();
						boolean didAddOne = false;
						for (int j = 0; j < items.length(); j++) {
							final int jFinal = j;
							try {
								if (items.getJSONObject(j).getJSONObject("user").getString("gender").equals("male")) {
									didAddOne = true;
									(new AsyncTask<Void, Void, Void>() {

										@Override
										protected Void doInBackground(Void... params) {
											try {
												MealObject meal = new MealObject();
												meal.setName(items.getJSONObject(jFinal).getJSONObject("user").getString("firstName"));
												meal.setFoursquarePicture(items.getJSONObject(jFinal).getJSONObject("user").getString("photo"));
												GetAttributesRequest getRequest = new GetAttributesRequest("users", "M" + items.getJSONObject(jFinal).getJSONObject("user").getString("id")).withConsistentRead(true);
												GetAttributesResult getResult = sdb.getAttributes(getRequest);
												List a = getResult.getAttributes();
												for (Object b : a) {
													Log.e("", ((Attribute) b).getName() + " " + ((Attribute) b).getValue());
													if (((Attribute) b).getName().equalsIgnoreCase("appPic")) {

														meal.setAppPicture(((Attribute) b).getValue());
														if (meal.getAppPicture().equalsIgnoreCase("none")) {
															meal.setAppPicture(null);
														}
														break;
													}
												}

												meal.setId(items.getJSONObject(jFinal).getJSONObject("user").getString("id"));
												ids.add(meal);
											} catch (JSONException e) {
												Log.e("", "", e);
												e.printStackTrace();
											}
											doneBuildingMeal();
											return null;
										}

									}).execute();

								} else {
									doneBuildingMeal();
								}
							} catch (JSONException e) {
								Log.e("", "", e);
								e.printStackTrace();
							}
						}
						if (!didAddOne) {
							itemsToWait++;
							doneBuildingMeal();
						}

					}
				}).execute("https://api.foursquare.com/v2/venues/" + venueId + "/herenow?oauth_token=" + getSharedPreferences("appF", 0).getString("token", "no"));

			}
		}).execute("https://api.foursquare.com/v2/users/self?oauth_token=" + getSharedPreferences("appF", 0).getString("token", "no"));
	}

	public synchronized void doneBuildingMeal() {
		itemsToWait--;
		if (itemsToWait == 0) {

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					((LinearLayout) findViewById(R.id.progress)).setVisibility(View.GONE);
					if (ids.size() == 0) {
						findViewById(R.id.nomeals).setVisibility(View.VISIBLE);
					} else {
						findViewById(R.id.nomeals).setVisibility(View.GONE);
					}
					final GridView gridview = (GridView) findViewById(R.id.menuGridView);
					((MenuAdapter) gridview.getAdapter()).refresh(ids);
					gridview.invalidateViews();
				}
			});
		}
	}
}

class MenuAdapter extends BaseAdapter {
	List<MealObject> meals;
	Context context;
	Activity activity;
	ImageLoader imageLoader;
	LayoutInflater inflater;
	int width;
	int height;

	public MenuAdapter(Activity activity, int width, int height) {
		this.width = width;
		this.height = height;
		this.context = activity;
		this.activity = activity;
		this.inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		imageLoader = new ImageLoader(context, R.drawable.meal_no_image_small,null,width,height);
		meals = new ArrayList<MealObject>();

	}

	public void refresh(List<MealObject> meals) {
		this.meals = meals;

	}

	public List<MealObject> getMeals() {
		return this.meals;
	}

	@Override
	public int getCount() {
		return this.meals.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 4;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		String url = this.meals.get(position).getAppPicture();
		if (url == null) {
			url = this.meals.get(position).getFoursquarePicture();
		}
		String id = this.meals.get(position).getId();

		if (convertView == null) {
			view = inflater.inflate(R.layout.meal_small, null);
			view.setLayoutParams(new GridView.LayoutParams(this.width, height));
		} else {
			view = convertView;
		}
		imageLoader.DisplayImage(url+"?"+new GregorianCalendar().get(Calendar.DAY_OF_YEAR), this.activity, (ImageView) view.findViewById(R.id.mealImage));
		String timestamp = this.activity.getSharedPreferences("ordered", 0).getString(id + "", null);
		TextView deliveringText = ((TextView) view.findViewById(R.id.deliveringText));
		if (timestamp != null) {
			Date date = new Date(Long.parseLong(timestamp));
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
				Editor editor = this.activity.getSharedPreferences("ordered", 0).edit();
				editor.remove(id + "");

				deliveringText.setVisibility(View.GONE);
			} else {
				deliveringText.setVisibility(View.VISIBLE);
			}
		} else {
			deliveringText.setVisibility(View.GONE);
		}

		return view;
	}
}