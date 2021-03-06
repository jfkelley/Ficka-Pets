package com.game.fickapets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.widget.TextView;

/*
 * A lot of the code in setUrlDrawable borrowed from https://github.com/koush/UrlImageViewHelper
 */

public class UrlImageViewHandler {
	/* keep track of what url each view is waiting for since listView recycles views and the urls each view is waiting
	 * for can change frequently
	 */
	private static Hashtable<TextView, String> pendingViews = new Hashtable<TextView, String>();
	private static final int THUMBNAIL_IMG_WIDTH = 40;
	private static final int THUMBNAIL_IMG_HEIGHT = 40;
	
	/* Values for file system cacheing of images */
	private static Integer bytesOnDisk;
	private static Vector<CacheEntry> files;
	private static final int MAX_BYTES_ON_DISK = 10000000;
	
	private static Resources mResources;
	private static DisplayMetrics mMetrics;
	private static Context context;
	
	@SuppressWarnings("unchecked")
	public UrlImageViewHandler(Context context) {
		UrlImageViewHandler.context = context;
		Object[] atts = PersistenceHandler.getImageViewHandlerAtts(context);
		if (atts.length == 2 && atts[0] instanceof Vector<?> && atts[1] instanceof Integer) {
			files = (Vector<CacheEntry>) atts[0];
			bytesOnDisk = (Integer) atts[1];
		} else {
			System.err.print("PersistenceHandler.getImageViewHandlerAtts not working");
		}
		prepareResources(context);
	}
	
	
	
	
	
	
	
	public void setUrlDrawable(final TextView textView, final String url, final int defaultResource) {
		if (url == null || url.equals("") || textView == null) {
			return;
		}
		if (context == null) context = textView.getContext();
		
		if (imageIsCached(url)) {
			BitmapDrawable image = getImageFromCache(url);
			
			if (image != null) {
				textView.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
				return;
			}
		}
		/* image not cached, so let's download it */
		
		if (pendingViews.get(textView) != null && pendingViews.get(textView).equals(url)) {
			/* psyche. we already started downloading this url for that image */
			return;
		}
		pendingViews.put(textView, url);
		final Drawable defaultDrawable = textView.getResources().getDrawable(defaultResource);
		//imageView.setImageDrawable(defaultDrawable);
		textView.setCompoundDrawablesWithIntrinsicBounds(defaultDrawable, null, null, null);
		new ImageDownloader().execute(url, textView);
	}
	
	public void preLoadUrl(String url) {
		new ImageDownloader().execute(url, null);
	}
	
	
	
	private static boolean imageIsCached(String url) {
		return files.contains(new CacheEntry(getFilenameForUrl(url), 0));
	}
	
	private static BitmapDrawable getImageFromCache(String url) {
		BitmapDrawable image = null;
		if (imageIsCached(url)) {
			String filename = getFilenameForUrl(url);
			
			/* take cacheentry from index and add to back of vector since we just accessed it */
			CacheEntry entry = files.remove(files.indexOf(new CacheEntry(filename, 0)));
			files.add(entry);
			
			File file = context.getFileStreamPath(filename);
			
			if (file.exists()) {
				try {
					FileInputStream fis = context.openFileInput(filename);
					image = new BitmapDrawable(mResources, Bitmap.createScaledBitmap(BitmapFactory.decodeStream(fis), THUMBNAIL_IMG_WIDTH, THUMBNAIL_IMG_HEIGHT, true));
				} catch(Exception ex) {}
			}
		}
		return image;
	}
	
	private class ImageDownloader extends AsyncTask<Object, Void, Bitmap> {
		private TextView textView;
		private String url;
		
		@Override
		protected Bitmap doInBackground(Object...params) {
			if (params.length >= 2) {
				if (params[0] instanceof String) {
					url = (String) params[0];
				} else {
					System.out.println("must have a url!!!");
					return null;
				}
				if (params[1] instanceof TextView) {
					textView = (TextView) params[1];
				}
			}
			AndroidHttpClient client = AndroidHttpClient.newInstance(context.getPackageName());
			try {
				HttpGet get = new HttpGet(url);
				final HttpParams httpParams = new BasicHttpParams();
				HttpClientParams.setRedirecting(httpParams, true);
				get.setParams(httpParams);
				System.out.println("Requesting this URL: " + get.getURI());
				HttpResponse response = client.execute(get);
				int status = response.getStatusLine().getStatusCode();
				if (status != HttpURLConnection.HTTP_OK) {
					return null;
				}
				HttpEntity entity = response.getEntity();
				InputStream is = entity.getContent();
				final Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(is), THUMBNAIL_IMG_WIDTH, THUMBNAIL_IMG_HEIGHT, true);
				return bitmap;
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			} finally {
				client.close();
			}
		}
		/* it'd be way better if i had size of img after compression */
		private int getBytesInBitmap(Bitmap bitmap) {
			return bitmap.getRowBytes() * bitmap.getHeight();
		}
		

		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap == null) return;
			final int bytes = getBytesInBitmap(bitmap);
			BitmapDrawable result = new BitmapDrawable(mResources, bitmap);
			if (result != null) {
				cacheImage(result, url, bytes);
				/* imageView is null if we're pre-fetching images */
				if (textView == null) return;
				
				String pendingUrl = pendingViews.get(textView);
				/* image is removed from pendingView meaning its image has already been set */
				if (pendingUrl == null) return;
				
				BitmapDrawable imageToUse = getImageFromCache (pendingUrl);
				if (imageToUse == null) return;
				//imageView.setImageDrawable(imageToUse);
				textView.setCompoundDrawablesWithIntrinsicBounds(imageToUse, null, null, null);
				pendingViews.remove(textView);
			}
		}
	};

	
	private static void moveToFileSystem (CacheEntry entry, final BitmapDrawable image) {
		
		final String filename = getFilenameForUrl(entry.name);

		final CacheEntry fileEntry = new CacheEntry(filename, entry.bytes);
		if (!files.contains(fileEntry)) {
			files.add(fileEntry);
			bytesOnDisk += fileEntry.bytes;
			while (bytesOnDisk > MAX_BYTES_ON_DISK) {
				CacheEntry entryToRemove = files.remove(0);
				final File fileToRemove = context.getFileStreamPath(entryToRemove.name);
				if (fileToRemove.exists()) {
					new Thread(new Runnable() {
						public void run() {
							try {
								fileToRemove.delete();
							} catch(Exception ex) {}
						}
					}).start();
				}
				bytesOnDisk -= entryToRemove.bytes;
			}
		} else {
			files.remove(fileEntry);
			files.add(fileEntry);
		}
		File file = context.getFileStreamPath(filename);
		if (!file.exists()) {
			new Thread(new Runnable() {
				public void run() {
					try {
						FileOutputStream fos = context.openFileOutput(fileEntry.name, Context.MODE_PRIVATE);
						image.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos);
						saveCacheState();
					} catch(IOException ex) {}
				}
			}).start();
		}
	}
	
	private synchronized static void saveCacheState() {
		PersistenceHandler.saveImageCacheState(context, bytesOnDisk, files);
	}
	
	private static void cacheImage(BitmapDrawable image, String url, int bytes) {
		CacheEntry entry = new CacheEntry(url, bytes);
		moveToFileSystem(entry, image);
	}
	
	/* not sure what this does - just took it from the code I based this class on */
	private static void prepareResources(Context context) {
		if (mMetrics != null) {
			return;
		}
		mMetrics = new DisplayMetrics();
		
		((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
		AssetManager manager = context.getAssets();
		mResources = new Resources(manager, mMetrics, context.getResources().getConfiguration());
	}
	
	private static String getFilenameForUrl(final String url) {
		return "" + url.hashCode() + ".urlImage";
	}
	

}
