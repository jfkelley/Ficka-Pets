package com.game.fickapets;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Hashtable;

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
import android.widget.ImageView;

/*
 * Code taken from https://github.com/koush/UrlImageViewHelper
 */

public class UrlImageViewHandler {
	/* keep track of what url each view is waiting for since listView recycles views and the urls each view is waiting
	 * for can change frequently
	 */
	private static Hashtable<ImageView, String> pendingViews = new Hashtable<ImageView, String>();
	private static Resources mResources;
	private static DisplayMetrics mMetrics;
	
	public static void setUrlDrawable(final ImageView imageView, final String url, final int defaultResource) {
		if (url == null || url.equals("") || imageView == null) {
			return;
		}
		pendingViews.remove(imageView);
		final Drawable defaultDrawable = imageView.getResources().getDrawable(defaultResource);
		//final String imageFilename = getFilenameForUrl(url);
		imageView.setImageDrawable(defaultDrawable);
		pendingViews.put(imageView, url);
		AsyncTask<Void, Void, Drawable> imageDownloader = new AsyncTask<Void, Void, Drawable>() {
			@Override
			protected Drawable doInBackground(Void...params) {
				Context context = imageView.getContext();
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
					prepareResources(context);
					final Bitmap bitmap = BitmapFactory.decodeStream(is);
					return new BitmapDrawable(mResources, bitmap);
				} catch (Exception ex) {
					ex.printStackTrace();
					return null;
				} finally {
					client.close();
				}
			}
			
			@Override
			protected void onPostExecute(Drawable result) {
				if (result == null) result = defaultDrawable;
				String pendingUrl = pendingViews.get(imageView);
				if (pendingUrl == null || !pendingUrl.equals(url)) {
					return;
				}
				pendingViews.remove(imageView);
				imageView.setImageDrawable(result);
			}
		};
		imageDownloader.execute();
	}
	
	private static void prepareResources(Context context) {
		if (mMetrics != null) {
			return;
		}
		mMetrics = new DisplayMetrics();
		
		((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
		AssetManager manager = context.getAssets();
		mResources = new Resources(manager, mMetrics, context.getResources().getConfiguration());
	}
	
	/*private static String getFilenameForUrl(final String url) {
		return "" + url.hashCode() + ".urlImage";
	}*/
}
