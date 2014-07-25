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
import android.os.Build;
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
import cw.kop.autobackground.sources.SourceListFragment;

public class Downloader {

	private static File currentBitmapFile = null;

    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "Downloader";
	private static FilenameFilter fileFilter = null;
    private static int randIndex = 0;
    public static boolean isDownloading = false;
	
	private static RetrieveImageTask imageAsyncTask;
	
	public Downloader() {
	}
	
	public static boolean download(Context appContext) {

        if (!isDownloading) {
            isDownloading = true;
            ConnectivityManager connect = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo wifi = connect.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = connect.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (wifi != null && wifi.isConnected() && AppSettings.useWifi()) {

            }
            else if (mobile != null && mobile.isConnected() && AppSettings.useMobile()) {

            }
            else {
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext, "No connection available,\ncheck Download Settings", Toast.LENGTH_SHORT).show();
                }
                try {
                    SourceListFragment sourceListFragment = ((MainActivity) appContext).websiteFragment; //.getFragmentManager().findFragmentByTag("website_fragment");
                    if (sourceListFragment != null) {
                        sourceListFragment.resetDownload();
                    }
                }
                catch (ClassCastException e) {
                }
                return false;
            }
            imageAsyncTask = new RetrieveImageTask(appContext);
            imageAsyncTask.execute();
            if (AppSettings.useToast()) {
                Toast.makeText(appContext, "Downloading images", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        else {
            if (AppSettings.useToast()) {
                Toast.makeText(appContext, "Already downloading", Toast.LENGTH_SHORT).show();
            }
            Log.i("Downloader", "Already downloading");
            return false;
        }
	}

    public static void cancel() {
        imageAsyncTask.cancel(false);
    }

	public static List<File> getBitmapList(Context appContext) {
		
		if (fileFilter == null) {
			fileFilter = (new FilenameFilter() {

				@Override
				public boolean accept(File dir, String filename) {
                    return filename.endsWith(".jpg") || filename.endsWith(".png");
                }
			});
		}
		
		String cacheDir = AppSettings.getDownloadPath();

        List<File> bitmaps = new ArrayList<File>();

        Log.i(TAG, "Use source 1: " + AppSettings.useSource(1));

        for (int i = 0; i < AppSettings.getNumSources(); i++) {

            if (AppSettings.useSource(i)) {
                if (AppSettings.getSourceType(i).equals(AppSettings.WEBSITE)) {
                    File folder = new File(cacheDir + "/" + AppSettings.getSourceTitleTrimmed(i) + AppSettings.getImagePrefix());
                    if (folder.exists() && folder.isDirectory()) {
                        bitmaps.addAll(Arrays.asList(folder.listFiles(fileFilter)));
                    }
                } else if (AppSettings.getSourceType(i).equals(AppSettings.FOLDER)) {
                    File folder = new File(AppSettings.getSourceData(i));
                    if (folder.exists() && folder.isDirectory()) {
                        bitmaps.addAll(Arrays.asList(folder.listFiles(fileFilter)));
                    }
                }
            }
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

    public static void renameFiles(Context appContext, String previousName, String newName) {

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
        String previousPrefix = previousName.replaceAll(" ", "") + AppSettings.getImagePrefix();
        String newPrefix = newName.replaceAll(" ", "") + AppSettings.getImagePrefix();
        String cacheDir = AppSettings.getDownloadPath();
        String newFileName = cacheDir + "/" + newName.replaceAll(" ", "") + AppSettings.getImagePrefix();

        File oldFolder = new File(cacheDir + "/" + previousPrefix);
        File[] fileList = oldFolder.listFiles(fileFilter);

        File newFolder = new File(cacheDir + "/" + newPrefix);
        newFolder.mkdir();

        if (fileList != null && fileList.length > 0) {
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].getName().contains(previousPrefix)) {
                    fileList[i].renameTo(new File(newFileName + "/" + newPrefix + i + ".png"));
                }
            }
        }
        oldFolder.delete();

    }

	public static void deleteCurrentBitmap() {
		currentBitmapFile.delete();
	}

    public static void deleteAllBitmaps(Context appContext) {
        for (int i = 0; i < AppSettings.getNumSources(); i++) {
            AppSettings.setSourceSet(AppSettings.getSourceTitle(i), new HashSet<String>());
        }
        for (File file : getBitmapList(appContext)) {
            if (file.getName().contains(AppSettings.getImagePrefix())) {
                file.delete();
            }
        }
    }

    public static void deleteBitmaps(Context appContext, int position) {

        File folder = new File(AppSettings.getDownloadPath() + "/" + AppSettings.getSourceTitleTrimmed(position) + AppSettings.getImagePrefix());

        AppSettings.setSourceSet(AppSettings.getSourceTitle(position), new HashSet<String>());

        if (folder.exists() && folder.isDirectory()) {
            if (folder.listFiles().length > 0) {
                for (File file : folder.listFiles()) {
                    file.delete();
                }
            }
            folder.delete();
        }
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

    private static Set<String> compileImageLinks(Document doc, String tag, String attr) {

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
                        Log.i(TAG, "Skipped due to width/height" + link.attr("width") + "/" + link.attr("height"));
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

	static class RetrieveImageTask extends AsyncTask<String, String, Void> {

		private String downloadCacheDir;
		private static final String TAG = "DownloaderAsyncTask";
		private Document linkDoc;
		private Context context;
		private String imageDetails = "";
        private NotificationManager notificationManager = null;
        private Notification.Builder notifyProgress;
        private int totalTarget = 0;
        private int totalDownloaded = 0;
		private boolean useNotification = AppSettings.useDownloadNotification();
        private HashSet<String> usedLinks;

		public RetrieveImageTask (Context appContext){
			context = appContext;
	    }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (useNotification) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notifyProgress = new Notification.Builder(context)
                        .setContentTitle("AutoBackground")
                        .setContentText("Downloading images...")
                        .setSmallIcon(R.drawable.ic_action_picture_dark);

                if (Build.VERSION.SDK_INT >= 16) {
                    notifyProgress.setPriority(Notification.PRIORITY_MIN);
                }

            }

            downloadCacheDir = AppSettings.getDownloadPath();

            File cache = new File(downloadCacheDir);

            if (!cache.exists() || !cache.isDirectory()) {
                cache.mkdir();
            }

        }

        protected Void doInBackground(String... params) {


            List<Integer> indexes = new ArrayList<Integer>();

			for (int index = 0; index < AppSettings.getNumSources(); index++) {
                if (AppSettings.getSourceType(index).equals("website") && AppSettings.useSource(index)) {
                    indexes.add(index);
                    totalTarget += AppSettings.getSourceNum(index);
                }
            }

            for (int index : indexes) {

                try {

                    usedLinks = new HashSet<String>();
                    HashSet<String> newLinks = new HashSet<String>();
                    if (!AppSettings.keepImages()) {
                        AppSettings.setSourceNumStored(index, 0);
                    }

                    if (AppSettings.deleteOldImages()) {
                        deleteBitmaps(context, index);
                        AppSettings.setSourceSet(AppSettings.getSourceTitle(index), new HashSet<String>());
                    }

                    linkDoc = Jsoup.connect(AppSettings.getSourceData(index))
                            .userAgent("Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36")
                            .referrer("http://www.google.com")
                            .get();

                    Set<String> imageLinks = new HashSet<String>();
                    List<String> imageList = new ArrayList<String>();
                    imageLinks.addAll(compileImageLinks(linkDoc, "a", "href"));
                    imageLinks.addAll(compileImageLinks(linkDoc, "img", "href"));
                    imageLinks.addAll(compileImageLinks(linkDoc, "img", "src"));
                    imageList.addAll(imageLinks);
                    Collections.shuffle(imageList);

                    int stored = AppSettings.getSourceNumStored(index);
                    int num = stored;
                    int count = 0;
                    String title = AppSettings.getSourceTitle(index);
                    String titleTrimmed = AppSettings.getSourceTitleTrimmed(index);

                    if (AppSettings.checkDuplicates()) {
                        usedLinks.addAll(AppSettings.getSourceSet(title));
                    }

                    File file = new File(downloadCacheDir + "/" + titleTrimmed + AppSettings.getImagePrefix());

                    if (!file.exists() || !file.isDirectory()) {
                        file.mkdir();
                    }

                    if (imageList.size() > 0) {
                        while (num < (AppSettings.getSourceNum(index) + stored) && count < imageList.size()) {

                            if (isCancelled()) {
                                return null;
                            }

                            String randLink = imageList.get(count);

                            boolean oldLink = usedLinks.contains(randLink);

                            if (!oldLink) {
                                if (getImage(randLink, downloadCacheDir, titleTrimmed, num)) {
                                    usedLinks.add(randLink);
                                    newLinks.add(randLink);
                                    num++;
                                    publishProgress("", "" + (totalDownloaded + num - stored));
                                }
                            }
                            count++;
                        }
                    }

                    int imagesDownloaded = num - stored;

                    AppSettings.setSourceNumStored(index, num);
                    AppSettings.setSourceSet(title, usedLinks);

                    publishProgress("", "" + AppSettings.getSourceNum(index));

                    imageDetails += title + ": " + imagesDownloaded + " images;break;";

                    if (imagesDownloaded < AppSettings.getSourceNum(index)) {
                        publishProgress("Not enough photos from " + AppSettings.getSourceData(index) +
                                " Try lowering the resolution or changing sources. " +
                                "There may also have been too many duplicates.");
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
                publishProgress("No images downloaded. Check wallpaper and download settings");
            }
			return null;
	    }

        private boolean getImage(String url, String cacheDir, String title, int num) {

            if (Patterns.WEB_URL.matcher(url).matches()) {
                try {

                    int minWidth = AppSettings.getWidth();
                    int minHeight = AppSettings.getHeight();
                    System.gc();
                    URL imageUrl = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                    connection.connect();
                    InputStream input = connection.getInputStream();

                    if (!connection.getHeaderField("Content-Type").startsWith("image/") && !AppSettings.forceDownload()) {
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

        private void writeToFile(Bitmap image, String url, String dir, String title, int imageIndex) {

            File file = new File(dir + "/" + title + AppSettings.getImagePrefix() + "/" + title + AppSettings.getImagePrefix() + imageIndex + ".png");

            if (file.isFile()) {
                usedLinks.remove(AppSettings.getUrl(file.getName()));
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

	    @Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
            if (!values[0].equals("")) {
                if (AppSettings.useToast()) {
                    Toast.makeText(context, values[0], Toast.LENGTH_LONG).show();
                }
            }
            else if (useNotification) {
                notifyProgress.setProgress(totalTarget, Integer.parseInt(values[1]), false);

                if (Build.VERSION.SDK_INT >=16) {
                    notificationManager.notify(NOTIFICATION_ID, notifyProgress.build());
                }
                else {
                    notificationManager.notify(NOTIFICATION_ID, notifyProgress.getNotification());
                }

            }
		}

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_ID);
            }

            try {
                SourceListFragment sourceListFragment = ((MainActivity) context).websiteFragment; //.getFragmentManager().findFragmentByTag("website_fragment");
                if (sourceListFragment != null) {
                    sourceListFragment.resetDownload();
                }
            }
            catch (ClassCastException e) {
            }

            context = null;
            isDownloading = false;
        }

        @Override
	    protected void onPostExecute(Void result) {

            if (useNotification) {
                Notification.Builder notifyComplete = new Notification.Builder(context)
                        .setContentTitle("Download Completed")
                        .setContentText("AutoBackground downloaded " + totalDownloaded + " images")
                        .setSmallIcon(R.drawable.ic_action_picture_dark);

                Notification notification;

                if (Build.VERSION.SDK_INT >= 16) {
                    notifyComplete.setPriority(Notification.PRIORITY_LOW);
                    Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
                    inboxStyle.setBigContentTitle("Downloaded Image Details:");

                    inboxStyle.addLine("Total images enabled: " + Downloader.getBitmapList(context).size());

                    for (String detail : imageDetails.split(";break;")) {
                        inboxStyle.addLine(detail);
                    }

                    notifyComplete.setStyle(inboxStyle);
                    notification = notifyComplete.build();
                }
                else {
                    notification = notifyComplete.getNotification();
                }

                notificationManager.cancel(NOTIFICATION_ID);
                notificationManager.notify(NOTIFICATION_ID, notification);
            }

            Intent cycleIntent = new Intent();
            cycleIntent.setAction(LiveWallpaperService.CYCLE_IMAGE);
            cycleIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(cycleIntent);

            try {
                SourceListFragment sourceListFragment = ((MainActivity) context).websiteFragment; //.getFragmentManager().findFragmentByTag("website_fragment");
                if (sourceListFragment != null) {
                    sourceListFragment.resetDownload();
                }
            }
            catch (ClassCastException e) {
            }

            context = null;

	    	Log.i(TAG, "Download Finished");
            isDownloading = false;
	    }
	}
	
}