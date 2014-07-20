package cw.kop.autobackground;

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

import java.io.File;
import java.io.FileOutputStream;
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

import cw.kop.autobackground.settings.AppSettings;

public class Downloader {

	public static File currentBitmapFile = null;

    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "Downloader";
	private static FilenameFilter fileFilter = null;
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
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext, "No connection availble,\ncheck Download Settings", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            imageAsyncTask = new RetrieveImageTask(appContext);
            loadImagesFromWeb();
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
		
		String cacheDir = AppSettings.getDownloadPath(appContext);

        List<File> bitmaps = new ArrayList<File>();
        File root = new File(cacheDir);

        for (File file : root.listFiles()) {
            if (file.exists() && file.isDirectory()) {
                bitmaps.addAll(Arrays.asList(file.listFiles(fileFilter)));
            }
        }

        bitmaps.addAll(Arrays.asList(root.listFiles(fileFilter)));

        for (int i = 0; i < AppSettings.getNumSources(); i++) {

            Log.i(TAG, "Source: " + i);
            Log.i(TAG, "Source: " + AppSettings.getSourceType(i).equals("folder"));
            if (AppSettings.getSourceType(i).equals("folder") && AppSettings.useSource(i)) {
                bitmaps.addAll(Arrays.asList(new File(AppSettings.getSourceData(i)).listFiles(fileFilter)));
                Log.i(TAG, "Added folder");
            }

        }

        if (bitmaps.size() == 0) {
            Toast.makeText(appContext, "No images available", Toast.LENGTH_SHORT).show();
        }

        Log.i(TAG, "Bitmap list size: " + bitmaps.size());

		return bitmaps;
		
	}
	
	public static String getBitmapLocation() {
        if (currentBitmapFile != null) {
            if (AppSettings.getUrl(currentBitmapFile.getName()) == null) {
                return currentBitmapFile.getAbsolutePath();
            }
            return AppSettings.getUrl(currentBitmapFile.getName());
        }
        return null;
	}

	public static File getCurrentBitmapFile() {
		return currentBitmapFile;
	}
	
	public static void deleteCurrentBitmap() {
		currentBitmapFile.delete();
	}

    public static void deleteAllBitmaps(Context appContext) {
        for (File file : getBitmapList(appContext)) {
            if (file.getName().contains(AppSettings.getImagePrefix())) {
                file.delete();
            }
        }
    }

    public static void deleteBitmaps(Context appContext, String title) {

        File folder = new File(AppSettings.getDownloadPath(appContext) + "/" + title);

        if (folder.exists() && folder.isDirectory()) {
            if (folder.listFiles().length > 0) {
                for (File file : folder.listFiles()) {
                    file.delete();
                }
            }
            folder.delete();
        }
    }
	
	public static void loadImagesFromWeb() {

		imageAsyncTask.execute();
		Log.i("Downloader", "Sent Task");
    }

    public static Bitmap getBitmap(File file, Context appContext) {
        Bitmap bitmap = null;
        if (file != null && file.exists()) {

            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                if (!AppSettings.useHighQuality()) {
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                }

                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                currentBitmapFile = file;
            }
            catch (OutOfMemoryError e) {
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext, "Out of memory error", Toast.LENGTH_SHORT).show();
                }
                return null;
            }
        }
        return bitmap;
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
			currentBitmapFile = images.get(randIndex);
		}

        Log.i("RandIndex", "" + randIndex);

		if (currentBitmapFile != null && currentBitmapFile.exists()) {

			try {
				BitmapFactory.Options options = new BitmapFactory.Options();
				if (!AppSettings.useHighQuality()) {
					options.inPreferredConfig = Bitmap.Config.RGB_565;
				}
				
				bitmap = BitmapFactory.decodeFile(currentBitmapFile.getAbsolutePath(), options);
			}
			catch (OutOfMemoryError e) {
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext, "Out of memory error", Toast.LENGTH_SHORT).show();
                }
                return null;
			}
			
		}
		else {
			Log.i("TAG", "No image");
			return null;
		}
		
		return bitmap;
		
	}

    public static void setHtml(String html, String dir, int urlIndex, String baseUrl, Context appContext) {
//        Log.i(TAG, "SetHtml called with baseURL: " + baseUrl);
//        try {
//
//            if (!AppSettings.keepImages()) {
//                deleteAllBitmaps(appContext);
//                AppSettings.setNumStored(0);
//            }
//
//            index = AppSettings.getNumStored();
//
//            File toWrite = new File(dir + "/html.txt");
//            if (toWrite.exists()){
//                toWrite.delete();
//            }
//
//            BufferedWriter buf = new BufferedWriter(new FileWriter(toWrite, true));
//            buf.append(html);
//
//            Document rootDoc = Jsoup.parse(html);
//
//            Log.i(TAG, AppSettings.getSourceData(urlIndex));
//
//            Set<String> imageLinks = new HashSet<String>();
//            imageLinks.addAll(compileImageLinks(rootDoc, "a", "href"));
//            imageLinks.addAll(compileImageLinks(rootDoc, "img", "src"));
//
//            Set<String> links = new HashSet<String>();
//            links.addAll(compileLinks(rootDoc, "a", "href", baseUrl));
//            links.addAll(compileLinks(rootDoc, "img", "href", baseUrl));
//
//            for (String link : links) {
//                try {
//                    Document imageDoc = Jsoup.connect(link).get();
//                    imageLinks.addAll(compileImageLinks(imageDoc, "a", "href"));
//                    imageLinks.addAll(compileImageLinks(imageDoc, "img", "src"));
//                }
//                catch (Exception e) {
//
//                }
//            }
//
//            buf.close();
//
//            Log.i(TAG, "Num Images: " + AppSettings.getSourceNum(urlIndex));
//
//            List<String> imageList = new ArrayList<String>();
//            imageList.addAll(imageLinks);
//
//            downloadImages(imageList, AppSettings.getSourceNum(urlIndex), dir);
//
//            Log.i("Downloader", "Written");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

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
            String url = link.attr(attr);
            if (!url.contains("http")) {
                url = "http:" + url;
            }
            if (link.attr("width") != null && !link.attr("width").equals("")) {
                try {
                    if (Integer.parseInt(link.attr("width")) < AppSettings.getWidth() || Integer.parseInt(link.attr("height")) < AppSettings.getHeight()) {
                        continue;
                    }
                }
                catch (NumberFormatException e) {
                }
            }
            if (url.contains(".png")) {
                links.add(url);
            }
            else if (url.contains(".jpg")) {
                links.add(url);
            }
            else if (AppSettings.forceDownload() && url.length() > 5 && (url.contains(".com") || url.contains(".org") || url.contains(".net"))) {
                links.add(url + ".png");
                links.add(url + ".jpg");
                links.add(url);
            }
        }
        return links;
    }

//    protected static void downloadImages(List<String> links, int numImages, String cacheDir, String title) {
//
//        ArrayList<String> usedLinks = new ArrayList<String>();
//        List<Integer> randValues = new ArrayList<Integer>();
//        for (int i = 0; i < links.size(); randValues.add(i++));
//        Collections.shuffle(randValues);
//
//        num = 0;
//        int count = 0;
//
//        if (links.size() > 0) {
//            while (num < numImages && count <= randValues.size() - 1) {
//
//                String randLink = links.get(randValues.get(count));
//
//                boolean oldLink = false;
//
//                for (String link : usedLinks) {
//                    if (link.equals(randLink)) {
//                        oldLink = true;
//                    }
//                }
//                if (!oldLink) {
//                    if (getImage(randLink, cacheDir, title)) {
//                        usedLinks.add(randLink);
//                        num++;
//                    }
//                }
//                count++;
//            }
//        }
//
//
//    }

    protected static boolean getImage(String url, String cacheDir, String title, int num) {

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

                writeToFile(bitmap, url, cacheDir, title, num);

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

    protected static void writeToFile(Bitmap image, String url, String dir, String title, int imageIndex) {

        File file = new File(dir + "/" + title + "/" + title + AppSettings.getImagePrefix() + imageIndex + ".png");

        if (!file.getParentFile().exists() || !file.getParentFile().isDirectory()) {
            if (!file.getParentFile().mkdir()) {
                return;
            }
        }

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
                if (out !=null) {
                    out.close();
                }
            }
            catch(Throwable e) {
                e.printStackTrace();
            }
        }
    }

	static class RetrieveImageTask extends AsyncTask<String, String, Void> {

		private String downloadCacheDir;
		private static final String TAG = "DownloaderAsyncTask";
		private Document linkDoc;
		private Context context;
		private String imageDetails = "";
        private Set<String> usedLinks = new HashSet<String>();
        private NotificationManager notificationManager;
        private Notification.Builder notifyProgress;
        private int totalTarget = 0;
        private int totalDownloaded = 0;
		
		public RetrieveImageTask (Context appContext){
			context = appContext;
	    }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            notifyProgress = new Notification.Builder(context)
                    .setContentTitle("AutoBackground")
                    .setContentText("Downloading images...")
                    .setSmallIcon(R.drawable.ic_action_picture_dark);


        }

        protected Void doInBackground(String... params) {

            downloadCacheDir = AppSettings.getDownloadPath(context);

            List<Integer> indexes = new ArrayList<Integer>();

			for (int index = 0; index < AppSettings.getNumSources(); index++) {
                if (AppSettings.getSourceType(index).equals("website") && AppSettings.useSource(index)) {
                    indexes.add(index);
                    totalTarget += AppSettings.getSourceNum(index);
                }
            }

            for (int index : indexes) {

                try {

                    if (!AppSettings.keepImages()) {
                        deleteBitmaps(context, AppSettings.getSourceTitle(index));
                        AppSettings.setSourceNumStored(index, 0);
                    }

                    linkDoc = Jsoup.connect(AppSettings.getSourceData(index))
                            .userAgent("Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36")
                            .referrer("http://www.google.com")
                            .get();

                    Set<String> imageLinks = new HashSet<String>();
                    List<String> imageList = new ArrayList<String>();
                    imageLinks.addAll(compileImageLinks(linkDoc, "a", "href"));
                    imageLinks.addAll(compileImageLinks(linkDoc, "img", "src"));
                    imageList.addAll(imageLinks);
                    Collections.shuffle(imageList);

                    int stored = AppSettings.getSourceNumStored(index);
                    int num = stored;
                    int count = 0;
                    String title = AppSettings.getSourceTitle(index);

                    if (imageList.size() > 0) {
                        while (num < (AppSettings.getSourceNum(index) + stored) && count < imageList.size()) {

                            String randLink = imageList.get(count);

                            boolean oldLink = usedLinks.contains(randLink);

                            if (!oldLink) {
                                if (getImage(randLink, downloadCacheDir, title, num)) {
                                    usedLinks.add(randLink);
                                    num++;
                                    publishProgress("", "" + (totalDownloaded + num - stored));
                                }
                            }
                            count++;
                        }
                    }

                    int imagesDownloaded = num - stored;

                    AppSettings.setSourceNumStored(index, num);

                    publishProgress("", "" + AppSettings.getSourceNum(index));

                    imageDetails += AppSettings.getSourceTitle(index) + ": " + imagesDownloaded + " images;break;";

                    if (imagesDownloaded < AppSettings.getSourceNum(index)) {
                        publishProgress("Not enough photos found from " + AppSettings.getSourceData(index) + "\nTry lowering the resolution or changing sources");
                    }

                    totalDownloaded += imagesDownloaded;

                } catch (IOException e) {
                    publishProgress("Invalid URL: " + AppSettings.getSourceData(index));
                    Log.i(TAG, "Invalid URL");
                } catch (IllegalArgumentException e) {
                    publishProgress("Invalid URL: " + AppSettings.getSourceData(index));
                    Log.i(TAG, "Invalid URL");
                }
            }

            if (totalDownloaded == 0) {
                publishProgress("No images downloaded,\ncheck wallpaper and download settings");
            }
            else {
                publishProgress("Downloaded " + totalDownloaded + " images");
            }
			return null;
	    }

	    
	    @Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
            if (!values[0].equals("")) {
                if (AppSettings.useToast()) {
                    Toast.makeText(context, values[0], Toast.LENGTH_SHORT).show();
                }
            }
            else {
                notifyProgress.setProgress(totalTarget, Integer.parseInt(values[1]), false);
                notificationManager.cancel(NOTIFICATION_ID);
                notificationManager.notify(NOTIFICATION_ID, notifyProgress.build());
            }
		}
	    
	    @Override
	    protected void onPostExecute(Void result) {

	    	Notification.Builder notifyComplete  = new Notification.Builder(context)
                .setContentTitle("Download Completed")
                .setContentText("AutoBackground downloaded " + totalDownloaded + " images")
                .setSmallIcon(R.drawable.ic_action_picture_dark);

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

            inboxStyle.addLine("Total images in folder: " + (new File(AppSettings.getDownloadPath(context))).listFiles(fileFilter).length);

			for (String detail : imageDetails.split(";break;")) {
				inboxStyle.addLine(detail);
			}

            notifyComplete.setStyle(inboxStyle);
            notificationManager.cancel(NOTIFICATION_ID);
			notificationManager.notify(NOTIFICATION_ID, notifyComplete.build());

            Intent cycleIntent = new Intent();
            cycleIntent.setAction(LiveWallpaperService.CYCLE_IMAGE);
            cycleIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(cycleIntent);
			
			context = null;
			
	    	Log.i(TAG, "Download Finished");
	    }
	}
	
}
