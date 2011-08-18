package utils.imageloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;



public class ImageLoader {
	public interface ImageLoadedCallback {
		public void down(String url);

		public void fail(String url);
	}

	ImageLoadedCallback callback;
	MemoryCache memoryCache = new MemoryCache();
	FileCache fileCache;
	boolean useCache = false;
	int height;
	int width;
	private Map<ImageView, String> imageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());

	public ImageLoader(Context c, int stubId, ImageLoadedCallback callback, int width, int height) {
		this(c, stubId);
		this.callback = callback;
		this.height = height;
		this.width = width;
	}

	public ImageLoader(Context context, int stubId) {
		// Make the background thead low priority. This way it will not affect
		// the UI performance
		this.stubId = stubId;
		photoLoaderThread.setPriority(Thread.NORM_PRIORITY - 1);

		fileCache = new FileCache(context);
	}

	final int stubId;

	public void DisplayImage(String url, Activity activity, ImageView imageView) {
		imageViews.put(imageView, url);
		Bitmap bitmap = memoryCache.get(url);
		if ((bitmap != null) && (useCache)) {
			if (callback != null) {
				callback.down(url);
			}
			imageView.setImageBitmap(bitmap);
		} else {
			queuePhoto(url, activity, imageView);
			if (stubId != -1)
				imageView.setImageResource(stubId);
		}
	}

	private void queuePhoto(String url, Activity activity, ImageView imageView) {
		// This ImageView may be used for other images before. So there may be
		// some old tasks in the queue. We need to discard them.
		photosQueue.Clean(imageView);
		PhotoToLoad p = new PhotoToLoad(url, imageView);
		synchronized (photosQueue.photosToLoad) {
			photosQueue.photosToLoad.push(p);
			photosQueue.photosToLoad.notifyAll();
		}

		// start thread if it's not started yet
		if (photoLoaderThread.getState() == Thread.State.NEW)
			photoLoaderThread.start();
	}

	private Bitmap getBitmap(String url) {
		File f = fileCache.getFile(url);

		// from SD cache
		Bitmap b = decodeFile(f);
		if (b != null)
			return b;

		// from web
		try {
			Bitmap bitmap = null;
			URL imageUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
			InputStream is = conn.getInputStream();
			OutputStream os = new FileOutputStream(f);
			copyStream(is, os);
			os.close();
			bitmap = decodeFile(f);
			return bitmap;
		} catch (Exception ex) {
			if (callback != null) {
				callback.down(url);
			}
			return null;
		}
	}

	// decodes image and scales it to reduce memory consumption
	private Bitmap decodeFile(File f) {
		try {
			int scale = 0;
			if ((height != 0) && (width != 0)) {

				// decode image size
				BitmapFactory.Options o = new BitmapFactory.Options();
				o.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(new FileInputStream(f), null, o);

				int scaleHeight = o.outHeight / height;
				int scaleWidth = o.outWidth / width;

				if (scaleHeight % 2 != 0)
					scaleHeight++;

				if (scaleWidth % 2 != 0)
					scaleWidth++;
				if (scaleHeight > scaleWidth) {
					scale = scaleWidth;
				} else {
					scale = scaleHeight;
				}
				if (scale < 0)
					scale = 0;
			}
			// decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException e) {
		}
		return null;
	}

	// Task for the queue
	private class PhotoToLoad {
		public String url;
		public ImageView imageView;

		public PhotoToLoad(String u, ImageView i) {
			url = u;
			imageView = i;
		}
	}

	PhotosQueue photosQueue = new PhotosQueue();

	public void stopThread() {
		photoLoaderThread.interrupt();
	}

	// stores list of photos to download
	class PhotosQueue {
		private Stack<PhotoToLoad> photosToLoad = new Stack<PhotoToLoad>();

		// removes all instances of this ImageView
		public void Clean(ImageView image) {
			for (int j = 0; j < photosToLoad.size();) {
				if (photosToLoad.get(j).imageView == image)
					photosToLoad.remove(j);
				else
					++j;
			}
		}
	}

	class PhotosLoader extends Thread {
		public void run() {
			try {
				while (true) {
					// thread waits until there are any images to load in the
					// queue
					if (photosQueue.photosToLoad.size() == 0)
						synchronized (photosQueue.photosToLoad) {
							photosQueue.photosToLoad.wait();
						}
					if (photosQueue.photosToLoad.size() != 0) {
						PhotoToLoad photoToLoad;
						synchronized (photosQueue.photosToLoad) {
							photoToLoad = photosQueue.photosToLoad.pop();
						}
						Bitmap bmp = getBitmap(photoToLoad.url);
						if (bmp != null) {
							memoryCache.put(photoToLoad.url, bmp);
							String tag = imageViews.get(photoToLoad.imageView);
							if (tag != null && tag.equals(photoToLoad.url)) {
								BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad.imageView, photoToLoad.url);
								Activity a = (Activity) photoToLoad.imageView.getContext();
								a.runOnUiThread(bd);
							}
						} else {
							if (callback != null) {
								callback.fail(photoToLoad.url);
							}

						}
					}
					if (Thread.interrupted())
						break;
				}
			} catch (InterruptedException e) {
				// allow thread to exit
			}
		}
	}

	PhotosLoader photoLoaderThread = new PhotosLoader();

	// Used to display bitmap in the UI thread
	class BitmapDisplayer implements Runnable {
		Bitmap bitmap;
		ImageView imageView;
		String url;

		public BitmapDisplayer(Bitmap b, ImageView i, String url) {
			bitmap = b;
			imageView = i;
			this.url = url;
		}

		public void run() {
			if (bitmap != null) {
				if (callback != null) {
					callback.down(url);
				}
				imageView.setImageBitmap(bitmap);
			} else {
				if (stubId != -1)
					imageView.setImageResource(stubId);
			}
		}
	}

	public void clearCache() {
		memoryCache.clear();
		fileCache.clear();
	}

	public static void copyStream(InputStream is, OutputStream os) {
		final int buffer_size = 1024;
		try {
			byte[] bytes = new byte[buffer_size];
			for (;;) {
				int count = is.read(bytes, 0, buffer_size);
				if (count == -1)
					break;
				os.write(bytes, 0, count);
			}
		} catch (Exception ex) {
		}
	}
}
