package com.sw.onthemenu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import utils.Utils;
import utils.actionbar.ActionBar;
import utils.actionbar.ActionBar.Action;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.UpdateCondition;

public class ProfileF extends Activity {

	String filePath;
	AmazonS3Client s3Client;
	String userId;
	boolean edited;
	private AmazonSimpleDBClient sdb;

	@Override
	protected void onResume() {
		((App) getApplication()).getTracker(this).trackPageView("F/Profile");
		super.onResume();
	}

	public void onCreate(Bundle savedInstanceState) {
		edited = false;
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.profile);
		((TextView) findViewById(R.id.helpLabel)).setText("Every meal you order will see your photo for 15s");
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
			Toast.makeText(this, Utils.FAIL_MESSAGE, Toast.LENGTH_LONG).show();
		}

		userId = ProfileF.this.getSharedPreferences("appF", 0).getString("userId", "--");
		filePath = getExternalFilesDir(null) + File.separator + "profileF.png";

		File tmpFile = new File(filePath + ".tmp");
		if (tmpFile.exists()) {
			Log.i(ProfileF.class.toString(), "Delete the tmp profile picture");
			tmpFile.delete();
		}

		if ((new File(filePath)).isFile()) {
			Log.v(ProfileF.class.toString(), "A previous profile picture exist, setting up");
			((ImageView) findViewById(R.id.profilePicture)).setImageBitmap(BitmapFactory.decodeFile(filePath));
			((View) findViewById(R.id.noProfilePictureView)).setVisibility(View.GONE);
		}

		ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
		actionBar.setTitle("Profile");
		actionBar.addAction(new Action() {

			@Override
			public void performAction(View view) {
				Log.e("", filePath);
				Intent takePictureFromCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				takePictureFromCameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(filePath + ".tmp")));
				startActivityForResult(takePictureFromCameraIntent, 101);
			}

			@Override
			public int getDrawable() {
				return R.drawable.camera;
			}
		});
		actionBar.addAction(new Action() {

			@Override
			public void performAction(View view) {
				edited = true;
				((View) findViewById(R.id.noProfilePictureView)).setVisibility(View.VISIBLE);
				((ImageView) findViewById(R.id.profilePicture)).setImageBitmap(null);
			}

			@Override
			public int getDrawable() {
				return R.drawable.cross;
			}
		});
		actionBar.setHomeAction(new Action() {

			@Override
			public void performAction(View view) {
				Intent intent = new Intent(ProfileF.this, DashboardF.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(intent);
			}

			@Override
			public int getDrawable() {
				return R.drawable.home_icon;
			}
		});

		s3Client = new AmazonS3Client(new BasicAWSCredentials(getString(R.string.aws_access_key), getString(R.string.aws_secret_key)));
		sdb = new AmazonSimpleDBClient(new BasicAWSCredentials(getString(R.string.aws_access_key), getString(R.string.aws_secret_key)));
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 101) {
			if (resultCode == Activity.RESULT_OK) {
				edited = true;
				BitmapFactory.Options getSizeOptions = new BitmapFactory.Options();
				getSizeOptions.inJustDecodeBounds = true;
				Bitmap getSizeBitmap = BitmapFactory.decodeFile(filePath + ".tmp");
				if (getSizeBitmap == null) {
					Toast.makeText(ProfileF.this, getString(R.string.error_local), Toast.LENGTH_LONG).show();
					Log.e(ProfileF.class.toString(), "Something went wrong when accessing the saved profile pic");
					return;
				}
				BitmapFactory.Options resizeOption = new BitmapFactory.Options();
				int scale = getSizeBitmap.getHeight() / 1000;
				if (scale % 2 != 0)
					scale++;
				resizeOption.inSampleSize = scale;

				Bitmap resizeBitmap = BitmapFactory.decodeFile(filePath + ".tmp", resizeOption);
				FileOutputStream out;
				try {
					out = new FileOutputStream(filePath + ".tmp");
					resizeBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
				} catch (FileNotFoundException e) {
					Toast.makeText(ProfileF.this, getString(R.string.error_local), Toast.LENGTH_LONG).show();
					Log.e(ProfileF.class.toString(), "Can't compress the profile picture", e);
					return;
				}
				((ImageView) findViewById(R.id.profilePicture)).setImageBitmap(BitmapFactory.decodeFile(filePath + ".tmp"));
				((View) findViewById(R.id.noProfilePictureView)).setVisibility(View.GONE);

			} else {
				if (resultCode != Activity.RESULT_CANCELED) {
					Log.e(ProfileF.class.toString(), "The camera come back with the code" + resultCode);
					Toast.makeText(ProfileF.this, R.string.error_local, Toast.LENGTH_LONG).show();
				}

			}
		}
	}

	public void onSaveProfile(View v) {

		if (!edited) {
			Toast.makeText(ProfileF.this, "Profile saved", Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(ProfileF.this, DashboardF.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			return;
		}
		final ProgressDialog dialog = ProgressDialog.show(ProfileF.this, "", "Saving...", true);
		dialog.show();
		Log.v(ProfileF.class.toString(), "Saving the profile");
		(new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... params) {
				File f = new File(filePath + ".tmp");
				if (f.exists()) {
					Log.v(ProfileF.class.toString(), "A tmp file exist, saving it to Amazon S3");
					try {
						s3Client.putObject(new PutObjectRequest(getString(R.string.s3_bucket), "images/F" + userId + ".png", new File(filePath + ".tmp")));

						List<ReplaceableAttribute> attributes = new ArrayList<ReplaceableAttribute>(1);
						attributes.add(new ReplaceableAttribute().withName("appPic").withValue("https://s3.amazonaws.com/"+getString(R.string.s3_bucket)+"/images/F" + userId + ".png"));

						sdb.putAttributes(new PutAttributesRequest("users", "F" + userId, attributes));

						Log.i(ProfileF.class.toString(), "Profile URL update to " + "https://s3.amazonaws.com/"+getString(R.string.s3_bucket)+"/images/F" + userId + ".png");
					} catch (Exception e) {
						Log.e(ProfileF.class.toString(), "Something went wrong when saving the profile picture", e);
						return false;
					}
					return true;
				} else {

					try {
						s3Client.deleteObject(getString(R.string.s3_bucket), "images/F" + userId + ".png");
						List<ReplaceableAttribute> replaceableAttributes = new ArrayList<ReplaceableAttribute>(1);
						replaceableAttributes.add(new ReplaceableAttribute().withName("appPic").withValue(null).withReplace(true));
						sdb.putAttributes(new PutAttributesRequest("users", "F" + userId, replaceableAttributes));
					} catch (Exception e) {
						Log.e(ProfileF.class.toString(), "Something went wrong when saving the profile picture", e);
						return false;
					}
					(new File(filePath)).delete();
					return true;
				}

			}

			@Override
			protected void onPostExecute(Boolean result) {
				dialog.dismiss();
				if (result == true) {
					File f = new File(filePath + ".tmp");
					if (f.exists()) {
						Log.i(ProfileF.class.toString(), "Moving the tmp profile picture");
						f.renameTo(new File(filePath));
					}

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(ProfileF.this, "Profile saved", Toast.LENGTH_SHORT).show();
						}
					});
					Intent intent = new Intent(ProfileF.this, DashboardF.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					startActivity(intent);
				} else {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(ProfileF.this, getString(R.string.error_network), Toast.LENGTH_LONG).show();
						}
					});
				}

			}
		}).execute();

	}
}
