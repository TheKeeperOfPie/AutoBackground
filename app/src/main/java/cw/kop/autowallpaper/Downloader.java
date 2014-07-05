package cw.kop.autowallpaper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import cw.kop.autowallpaper.settings.AppSettings;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

public class Downloader {

	public static File bitmapFile = null;
	
	private static FilenameFilter fileFilter = null;
	private static int widthPx;
	private static int heightPx;
	private static int randIndex = 0;
	
	private static RetrieveImageTask imageAsyncTask;
	
	public Downloader() {
	}

	public static void setNewTask(Context appContext) {
		
		imageAsyncTask = new RetrieveImageTask(appContext);
		
	}
	
	public static void download(Context appContext) {
		if (imageAsyncTask != null && imageAsyncTask.getStatus() != AsyncTask.Status.RUNNING) {
	    	ConnectivityManager connect = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	    	
	    	NetworkInfo wifi = connect.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    	NetworkInfo mobile = connect.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
	    	
	    	if (wifi.isConnected() && AppSettings.useWifi()) {
	    		
	    	}
	    	else if (mobile.isConnected() && AppSettings.useMobile()) {
	    		
	    	}
	    	else {
	    		Toast.makeText(appContext, "No connection availble,\ncheck Download Settings", Toast.LENGTH_SHORT).show();
	    		return;
	    	}
			setDimensions(AppSettings.getWidth(), AppSettings.getHeight());
			imageAsyncTask = new RetrieveImageTask(appContext);
			loadImagesFromWeb(appContext);
			Toast.makeText(appContext, "Downloading images", Toast.LENGTH_SHORT).show();
		}
		else {
			Toast.makeText(appContext, "Already downloading", Toast.LENGTH_SHORT).show();
			Log.i("Downloader", "Already downloading");
		}
	}
	
	public static void setDimensions(int width, int height) {
		widthPx = width;
		heightPx = height;
		Log.i("D", "Dimensions set\nWidth: " + width + "\nHeight: " + height);
	}
	
	private static File[] getBitmapList(Context appContext) {
		
		if (fileFilter == null) {
			fileFilter = (new FilenameFilter() {

				@Override
				public boolean accept(File dir, String filename) {
					if (filename.endsWith(".jpg") || filename.endsWith(".png")) {
						return true;
					}
					return false;
				}
			});
		}
		
		String cacheDir = appContext.getCacheDir().getAbsolutePath();
    	
    	if (AppSettings.getDownloadPath() != null) {    		
    		cacheDir = AppSettings.getDownloadPath();
    	}
		
		File[] bitmaps = (new File(cacheDir)).listFiles(fileFilter);
		
		return bitmaps;
		
	}
	
	public static String getBitmapUrl() {
		return AppSettings.getUrl(bitmapFile.getName());
	}
	
	public static File getBitmapFile() {		
		return bitmapFile;
	}
	
	public static void deleteCurrentBitmap() {
		bitmapFile.delete();
	}
	
	public static void loadImagesFromWeb(Context appContext) {
		
		String cacheDir = appContext.getCacheDir().getAbsolutePath();
    	
    	if (AppSettings.getDownloadPath() != (null)) {    		
    		cacheDir = AppSettings.getDownloadPath();
    	}
		
		imageAsyncTask.execute(cacheDir);
		Log.i("Downloader", "Sent Task");
    }
	
	public static Bitmap getNextImage(Context appContext) {
		
    	File[] images = getBitmapList(appContext);
    	
		Bitmap bitmap = null;
		
		Log.i("Downloader", "Getting next image");
		
		if (!AppSettings.shuffleImages()) {
			randIndex++;
		}
		else if (images.length > 0){
			randIndex += (Math.random() * (images.length - 2)) + 1;
		}
		
		if (randIndex >= images.length) {
			randIndex -= images.length;
		}
		
		if (images != null && images.length > 0 && randIndex < images.length) {
			bitmapFile = images[randIndex];
		}
		
		if (bitmapFile != null && bitmapFile.exists()) {

			try {
				BitmapFactory.Options options = new BitmapFactory.Options();
				if (!AppSettings.useHighQuality()) {
					options.inPreferredConfig = Bitmap.Config.RGB_565;
				}
				
				bitmap = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), options);
				
				Log.i("RandIndex", "" + randIndex);
			}
			catch (OutOfMemoryError e) {
				Toast.makeText(appContext, "Out of memory error", Toast.LENGTH_SHORT).show();
                bitmap.recycle();
                return null;
			}
			
		}
		else {
			Log.i("TAG", "No image");
			return null;
		}
		
		return bitmap;
		
	}

    public static void setHtml(String html, String dir) {
        try {
            File toWrite = new File(dir + "/html.txt");
            if (toWrite.exists()){
                toWrite.delete();
            }

            BufferedWriter buf = new BufferedWriter(new FileWriter(toWrite, true));
            buf.append(html);
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	static class RetrieveImageTask extends AsyncTask<String, String, Void> {

		private String downloadCacheDir;
		private static final String TAG = "DownloaderAsyncTask";
		private Document linkDoc;
		private Context context;
		private int index = 0;
		private int num = 0;
		private int minWidth;
		private int minHeight;
		private String imageDetails = "";
		
		public RetrieveImageTask (Context appContext){
			context = appContext;
	    }
		
	    protected Void doInBackground(String... params) {
	    	downloadCacheDir = params[0];
	    	
	    	if (AppSettings.getDownloadPath() != (null)) {    		
	    		downloadCacheDir = AppSettings.getDownloadPath();
	    	}
			
	    	Log.i(TAG, downloadCacheDir);
	    	
			minWidth = AppSettings.getWidth();
			minHeight = AppSettings.getHeight();
			
			if (!AppSettings.keepImages()) {
				for (int i = 0; i < AppSettings.getNumStored(); i++) {
					File toDelete = new File(downloadCacheDir + "/image" + i + ".png");
					if (toDelete != null && toDelete.exists() && toDelete.isFile()) {
						toDelete.delete();
					}
				}
			}

			AppSettings.setNumStored(0);
			
			Log.i(TAG, "Num websites: " + AppSettings.getNumWebsites());
			
			for (int i = 0; i < AppSettings.getNumWebsites(); i++) {
				
				if (AppSettings.useWebsite(i)) {
					try {
						linkDoc = Jsoup.connect(AppSettings.getWebsiteUrl(i)).get();
						
						Log.i(TAG, AppSettings.getWebsiteUrl(i));
						
						List<String> imageLinks = new ArrayList<String>();
						imageLinks.addAll(compileLinks(linkDoc, "a", "href"));
						imageLinks.addAll(compileLinks(linkDoc, "img", "src"));
						
						Log.i(TAG, "Num Images: " + AppSettings.getNumImages(i));
						
						downloadImages(imageLinks, AppSettings.getNumImages(i));
						
						imageDetails += AppSettings.getWebsiteTitle(i) + ": " + num + " images;break;";
						
					}
					catch (IOException e) {
						publishProgress("Invalid URL: " + AppSettings.getWebsiteUrl(i));
						Log.i(TAG, "Invalid URL");
					}
					catch (IllegalArgumentException e) {
						publishProgress("Invalid URL: " + AppSettings.getWebsiteUrl(i));
						Log.i(TAG, "Invalid URL");
					}
				}
			}
			
			
			
			Log.i(TAG, "Downloaded " + AppSettings.getNumStored() + " images");
			
			publishProgress("Downloaded " + AppSettings.getNumStored() + " images");
			
			return null;
	    }

	    protected void downloadImages(List<String> links, int numImages) {

			ArrayList<String> usedLinks = new ArrayList<String>();
	    	List<Integer> randValues = new ArrayList<Integer>();
			for (int i = 0; i < links.size(); randValues.add(i++));
			Collections.shuffle(randValues);
			
			num = 0;
			int count = 0;
			
			if (links.size() > 0) {
				while (num < numImages && count <= randValues.size() - 1) {
					
					String randLink = links.get(randValues.get(count));
					
					boolean oldLink = false;
					
					for (String link : usedLinks) {
						if (link.equals(randLink)) {
							oldLink = true;
						}
					}
					if (!oldLink) {
						if (getImage(randLink)) {
							num++;
							usedLinks.add(randLink);
						}
					}
					count++;
				}
			}
	    }
	    
	    @Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			
			Toast.makeText(context, values[0], Toast.LENGTH_SHORT).show();
			
		}

	    protected List<String> compileLinks(Document doc, String tag, String attr) {
	    	
	    	Elements downloadLinks = doc.select(tag);
	    	List<String> links = new ArrayList<String>();
	    	
			for (Element link : downloadLinks) {
				if (link.attr(attr).contains(".png")) {
					links.add(link.attr("href"));
				}
				else if (link.attr(attr).contains(".jpg")) {
					links.add(link.attr("href"));
				}
				else if (AppSettings.forceDownload() && link.attr(attr).length() > 5 && link.attr(attr).contains(".com")) {
					links.add(link.attr(attr) + ".png");
					links.add(link.attr(attr) + ".jpg");
					links.add(link.attr(attr));
				}
			}
			return links;
			
	    }
	    
		protected boolean getImage(String url) {
	    	
			if (Patterns.WEB_URL.matcher(url).matches()) {
				try {
		            System.gc();
		            URL imageUrl = new URL(url);
		            HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
		            connection.connect();
		            InputStream input = connection.getInputStream();
		            
		            if (!connection.getHeaderField("Content-Type").startsWith("image/")) {
		            	Log.i(TAG, "URL not an image");
		            	return false;
		            }

		            final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
					if (!AppSettings.useHighQuality()) {
						options.inPreferredConfig = Bitmap.Config.RGB_565;
					}
		            options.inJustDecodeBounds = true;
		            
		            BitmapFactory.decodeStream(input, null, options);
		            
		            input.close();
		            int bitWidth = options.outWidth;
		            int bitHeight = options.outHeight;
		            options.inJustDecodeBounds = false;
		            
		            if (bitWidth < widthPx || bitHeight < heightPx) {
		            	return false;
		            }
		            
		            int sampleSize = 1;

		            if (bitHeight > minHeight || bitWidth > minWidth) {

		                final int halfHeight = bitHeight / 2;
		                final int halfWidth = bitWidth / 2;
		                while ((halfHeight / sampleSize) > minHeight && (halfWidth / sampleSize) > minWidth) {
		                	sampleSize *= 2;
		                }
		            }
		            
		            options.inSampleSize = sampleSize;
		            
		            connection = (HttpURLConnection) imageUrl.openConnection();
		            connection.connect();
		            input = connection.getInputStream();
		            
		            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
		            
		            if (bitmap == null) {
		            	return false;
		            }
	                
	                writeToFile(bitmap, url);
	                
	                publishProgress("Downloaded: " + imageUrl);
	                
	                //Log.i(TAG, "Wrote: " + imageUrl + "\nWidth: " + bitmap.getWidth() + "\nHeight: " + bitmap.getHeight() + "\nIndex: " + (index - 1));

                    bitmap.recycle();

	                return true;
	                
		        } 
				catch (OutOfMemoryError e) {
					return false;
				}
				catch (IOException e) {
		            return false;
		        }
			}
	    	return false;
	    }
	    
	    protected void writeToFile(Bitmap image, String url) {
	    	
	    	File toDelete = new File(downloadCacheDir + "/image" + index + ".png");
	    	if (toDelete.isFile()) {
	    		toDelete.delete();
	    	}
	    	
	    	File toWrite = new File(downloadCacheDir + "/image" + index + ".png");
	    	
	    	FileOutputStream out = null;
	    	try {
	    		out = new FileOutputStream(toWrite);
	    		image.compress(Bitmap.CompressFormat.PNG, 90, out);
                AppSettings.setUrl(toWrite.getName(), url);
                Log.i(TAG, toWrite.getName() + " " + url);
	    	}
	    	catch (Exception e) {
	    	    e.printStackTrace();
	    	}
	    	finally {
	    		try{
	    			out.close();
	    	    } 
	    		catch(Throwable e) {
	    			e.printStackTrace();
	    		}
	    	}
	    	
	    	index++;
	    	
            AppSettings.setNumStored(index);
	    	
	    }
	    
	    @Override
	    protected void onPostExecute(Void result) {

	    	Notification.Builder notification  = new Notification.Builder(context)
			        .setContentTitle("Download Completed")
			        .setContentText("AutoBackground downloaded " + index + " images")
			        .setSmallIcon(R.drawable.ic_action_picture);

            Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
			inboxStyle.setBigContentTitle("Downloaded Image Details:");
			
			for (String detail : imageDetails.split(";break;")) {
				
				inboxStyle.addLine(detail);
				
			}
			
			notification.setStyle(inboxStyle);
			
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(1, notification.build());

            Intent cycleIntent = new Intent();
            cycleIntent.setAction(LiveWallpaperService.CYCLE_WALLPAPER);
            cycleIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(cycleIntent);
			
			context = null;
			
	    	Log.i(TAG, "Download Finished");
	    }
	}
	
}
