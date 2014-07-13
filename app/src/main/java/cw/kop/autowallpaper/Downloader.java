package cw.kop.autowallpaper;

import android.app.Notification;
import android.app.NotificationManager;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cw.kop.autowallpaper.settings.AppSettings;

public class Downloader {

	public static File bitmapFile = null;

    private static final String TAG = "Downloader";
	private static FilenameFilter fileFilter = null;
    private static int randIndex = 0;
    private static int index;
    private static int num = 0;
	
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
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext, "No connection availble,\ncheck Download Settings", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            imageAsyncTask = new RetrieveImageTask(appContext);
            loadImagesFromWeb(appContext);
            if (AppSettings.useToast()) {
                Toast.makeText(appContext, "Downloading images", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            if (AppSettings.useToast()) {
                Toast.makeText(appContext, "Already downloading", Toast.LENGTH_SHORT).show();
            }
            Log.i("Downloader", "Already downloading");
        }
	}
	
	private static List<File> getBitmapList(Context appContext) {
		
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

        List<File> bitmaps = new ArrayList<File>();
        bitmaps.addAll(Arrays.asList(new File(cacheDir).listFiles(fileFilter)));

        for (int i = 0; i < AppSettings.getNumSources(); i++) {

            if (AppSettings.getSourceType(i).equals("folder") && AppSettings.useSource(i)) {
                bitmaps.addAll(Arrays.asList(new File(AppSettings.getSourceData(i)).listFiles(fileFilter)));
                Log.i(TAG, "Added folder");
            }

        }

        Log.i(TAG, "Bitmap list size: " + bitmaps.size());

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

    public static void deleteAllBitmaps(Context appContext) {
        List<File> files = getBitmapList(appContext);
        for (File file : files) {
            if (file.getName().contains(AppSettings.getImagePrefix())) {
                file.delete();
            }
        }
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
		
    	List<File> images = getBitmapList(appContext);
    	
		Bitmap bitmap = null;
		
		Log.i("Downloader", "Getting next image");
		
		if (!AppSettings.shuffleImages()) {
			randIndex++;
		}
		else if (images.size() > 0){
			randIndex += (Math.random() * (images.size() - 2)) + 1;
		}
		
		if (randIndex >= images.size()) {
			randIndex -= images.size();
		}
		
		if (images.size() > 0 && randIndex < images.size()) {
			bitmapFile = images.get(randIndex);
		}

        Log.i("RandIndex", "" + randIndex);

		if (bitmapFile != null && bitmapFile.exists()) {

			try {
				BitmapFactory.Options options = new BitmapFactory.Options();
				if (!AppSettings.useHighQuality()) {
					options.inPreferredConfig = Bitmap.Config.RGB_565;
				}
				
				bitmap = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), options);
			}
			catch (OutOfMemoryError e) {
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext, "Out of memory error", Toast.LENGTH_SHORT).show();
                }
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

    public static void resetIndex() {
        index = 0;
    }

    public static void setHtml(String html, String dir, int urlIndex, String baseUrl, Context appContext) {
        Log.i(TAG, "SetHtml called with baseURL: " + baseUrl);
        try {

            if (!AppSettings.keepImages()) {
                deleteAllBitmaps(appContext);
                AppSettings.setNumStored(0);
            }

            index = AppSettings.getNumStored();

            File toWrite = new File(dir + "/html.txt");
            if (toWrite.exists()){
                toWrite.delete();
            }

            BufferedWriter buf = new BufferedWriter(new FileWriter(toWrite, true));
            buf.append(html);

            Document rootDoc = Jsoup.parse(html);

            Log.i(TAG, AppSettings.getSourceData(urlIndex));

            Set<String> imageLinks = new HashSet<String>();
            imageLinks.addAll(compileImageLinks(rootDoc, "a", "href"));
            imageLinks.addAll(compileImageLinks(rootDoc, "img", "src"));

            Set<String> links = new HashSet<String>();
            links.addAll(compileLinks(rootDoc, "a", "href", baseUrl));
            links.addAll(compileLinks(rootDoc, "img", "href", baseUrl));

            for (String link : links) {
                try {
                    Document imageDoc = Jsoup.connect(link).get();
                    imageLinks.addAll(compileImageLinks(imageDoc, "a", "href"));
                    imageLinks.addAll(compileImageLinks(imageDoc, "img", "src"));
                }
                catch (Exception e) {

                }
            }

            buf.close();

            Log.i(TAG, "Num Images: " + AppSettings.getSourceNum(urlIndex));

            List<String> imageList = new ArrayList<String>();
            imageList.addAll(imageLinks);

            downloadImages(imageList, AppSettings.getSourceNum(urlIndex), dir);

            Log.i("Downloader", "Written");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected static List<String> compileLinks(Document doc, String tag, String attr, String baseUrl) {

        Elements downloadLinks = doc.select(tag);
        List<String> links = new ArrayList<String>();

        for (Element link : downloadLinks) {
            String url = link.attr(attr);
            if (!(url.contains(".com") || url.contains(".org") || url.contains(".net"))) {
                links.add(baseUrl + url);
                Log.i(TAG, baseUrl + url);
            }
            else {
                links.add(url);
                Log.i(TAG, url);
            }
        }
        return links;

    }

    protected static Set<String> compileImageLinks(Document doc, String tag, String attr) {

        Elements downloadLinks = doc.select(tag);
        Set<String> links = new HashSet<String>();

        for (Element link : downloadLinks) {
//            Log.i(TAG, link.attr(attr));
            String url = link.attr(attr);
            if (!url.contains("http")) {
                url = "http:" + url;
            }
            if (link.attr("width") != null && !link.attr("width").equals("")) {
                try {
                    Log.i(TAG, "Width attribute: " + Integer.parseInt(link.attr("width")) + " Height attribute: " + Integer.parseInt(link.attr("width")));
                    if (Integer.parseInt(link.attr("width")) < AppSettings.getWidth() || Integer.parseInt(link.attr("height")) < AppSettings.getHeight()) {
                        continue;
                    }
                }
                catch (NumberFormatException e) {

                }
            }

            if (url.contains(".png")) {
                links.add(url);
                Log.i(TAG, link.attr(attr));
            }
            else if (url.contains(".jpg")) {
                links.add(url);
                Log.i(TAG, link.attr(attr));
            }
            else if (AppSettings.forceDownload() && url.length() > 5 && (url.contains(".com") || url.contains(".org") || url.contains(".net"))) {
                links.add(url + ".png");
                links.add(url + ".jpg");
                links.add(url);
                Log.i(TAG, url);
            }
        }
        return links;

    }

    protected static void downloadImages(List<String> links, int numImages, String cacheDir) {

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
                    if (getImage(randLink, cacheDir)) {
                        usedLinks.add(randLink);
                        num++;
                    }
                }
                count++;
            }
        }


    }

    protected static boolean getImage(String url, String cacheDir) {

        if (Patterns.WEB_URL.matcher(url).matches()) {
            try {

                int minWidth = AppSettings.getWidth();
                int minHeight = AppSettings.getHeight();
                System.gc();
                URL imageUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                connection.connect();
                InputStream input = connection.getInputStream();

                if (!connection.getHeaderField("Content-Type").startsWith("image/")) {
                    Log.i(TAG, "Not an image: " + url);
                    return false;
                }

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.inJustDecodeBounds = true;
                if (!AppSettings.useHighQuality()) {
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                }

                BitmapFactory.decodeStream(input, null, options);

                input.close();

                int bitWidth = options.outWidth;
                int bitHeight = options.outHeight;
                options.inJustDecodeBounds = false;

                Log.i(TAG, "bitWidth: " + bitWidth + " bitHeight: " + bitHeight);

                if (bitWidth + 10 < minWidth || bitHeight + 10 < minHeight) {
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

                writeToFile(bitmap, url, cacheDir);

                Log.i(TAG, "Wrote: " + imageUrl + "\nWidth: " + bitmap.getWidth() + "\nHeight: " + bitmap.getHeight() + "\nIndex: " + (index - 1));

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

    protected static void writeToFile(Bitmap image, String url, String dir) {

        File file = new File(dir + "/" + AppSettings.getImagePrefix() + index + ".png");
        if (file.isFile()) {
            file.delete();
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, out);
            AppSettings.setUrl(file.getName(), url);
            Log.i(TAG, file.getName() + " " + url);
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

	static class RetrieveImageTask extends AsyncTask<String, String, Void> {

		private String downloadCacheDir;
		private static final String TAG = "DownloaderAsyncTask";
		private Document linkDoc;
		private Context context;
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
			
			if (!AppSettings.keepImages()) {
				deleteAllBitmaps(context);
                AppSettings.setNumStored(0);
			}

            index = AppSettings.getNumStored();

			Log.i(TAG, "Num websites: " + AppSettings.getNumSources());
			
			for (int i = 0; i < AppSettings.getNumSources(); i++) {
				
				if (AppSettings.getSourceType(i).equals("website") && AppSettings.useSource(i)) {
					try {
						linkDoc = Jsoup.connect(AppSettings.getSourceData(i))
                            .userAgent("Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36")
                            .referrer("http://www.google.com")
                            .get();
						
						Log.i(TAG, AppSettings.getSourceData(i));
						
						Set<String> imageLinks = new HashSet<String>();
						imageLinks.addAll(compileImageLinks(linkDoc, "a", "href"));
						imageLinks.addAll(compileImageLinks(linkDoc, "img", "src"));
						
						Log.i(TAG, "Num Images: " + AppSettings.getSourceNum(i));

                        List<String> imageList = new ArrayList<String>();
                        imageList.addAll(imageLinks);

						downloadImages(imageList, AppSettings.getSourceNum(i), downloadCacheDir);
						
						imageDetails += AppSettings.getSourceTitle(i) + ": " + num + " images;break;";

                        if (num < AppSettings.getSourceNum(i)) {
                            publishProgress("Not enough photos found from " + AppSettings.getSourceData(i) + "\nTry lowering the resolution or changing websites");
                        }
						
					}
					catch (IOException e) {
						publishProgress("Invalid URL: " + AppSettings.getSourceData(i));
						Log.i(TAG, "Invalid URL");
					}
					catch (IllegalArgumentException e) {
						publishProgress("Invalid URL: " + AppSettings.getSourceData(i));
						Log.i(TAG, "Invalid URL");
					}
				}
			}
			
			Log.i(TAG, "Downloaded " + AppSettings.getNumStored() + " images");

            if (AppSettings.getNumStored() <= 0) {
                publishProgress("No images downloaded,\ncheck wallpaper and download settings");
            }
            else {
                publishProgress("Downloaded " + AppSettings.getNumStored() + " images");
            }
			
			return null;
	    }


	    
	    @Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
            if (AppSettings.useToast()) {
                Toast.makeText(context, values[0], Toast.LENGTH_SHORT).show();
            }
		}
	    
	    @Override
	    protected void onPostExecute(Void result) {

	    	Notification.Builder notification  = new Notification.Builder(context)
                .setContentTitle("Download Completed")
                .setContentText("AutoBackground downloaded " + index + " images")
                .setSmallIcon(R.drawable.ic_action_picture);

            Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
			inboxStyle.setBigContentTitle("Downloaded Image Details:");

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

            String cacheDir = context.getCacheDir().getAbsolutePath();

            if (AppSettings.getDownloadPath() != null) {
                cacheDir = AppSettings.getDownloadPath();
            }

            inboxStyle.addLine("Total images in folder: " + (new File(cacheDir)).listFiles(fileFilter).length);

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

            index = 0;
			
	    	Log.i(TAG, "Download Finished");
	    }
	}
	
}
