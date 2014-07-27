package cw.kop.autobackground.downloader;

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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.MainActivity;
import cw.kop.autobackground.R;
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
                return true;
            }
            imageAsyncTask = new RetrieveImageTask(appContext);
            imageAsyncTask.execute();
            if (AppSettings.useToast()) {
                Toast.makeText(appContext, "Downloading images", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        else {
            return false;
        }
	}

    public static void cancel(Context appContext) {
        if (imageAsyncTask != null) {
            imageAsyncTask.cancel(false);
            SourceListFragment sourceListFragment = ((MainActivity) appContext).websiteFragment; //.getFragmentManager().findFragmentByTag("website_fragment");
            if (sourceListFragment != null) {
                sourceListFragment.resetDownload();
            }
        }
        isDownloading = false;
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

        for (int i = 0; i < AppSettings.getNumSources(); i++) {

            if (AppSettings.useSource(i)) {
                String type = AppSettings.getSourceType(i);
                if (type.equals(AppSettings.WEBSITE) || type.equals(AppSettings.IMGUR)) {
                    File folder = new File(cacheDir + "/" + AppSettings.getSourceTitle(i) + " " + AppSettings.getImagePrefix());
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
        String previousPrefix = previousName + " " + AppSettings.getImagePrefix();
        String newPrefix = newName + " " + AppSettings.getImagePrefix();
        String cacheDir = AppSettings.getDownloadPath();
        String newFileName = cacheDir + "/" + newPrefix;

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

        File folder = new File(AppSettings.getDownloadPath() + "/" + AppSettings.getSourceTitle(position) + " " + AppSettings.getImagePrefix());

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

    public static Bitmap getCurrentImage(Context appContext) {
        Bitmap bitmap = null;
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
		else if (randIndex < 0) {
            randIndex += images.size();
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

    public static void decreaseIndex() {
        randIndex--;
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
        private int progressMax = 0;
        private int totalDownloaded = 0;
        private int totalTarget = 0;
        private int imagesDownloaded = 0;
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
                if (!AppSettings.getSourceType(index).equals(AppSettings.FOLDER) && AppSettings.useSource(index)) {
                    indexes.add(index);
                    progressMax += AppSettings.getSourceNum(index);
                }
            }

            for (int index : indexes) {

                try {

                    usedLinks = new HashSet<String>();
                    if (!AppSettings.keepImages()) {
                        AppSettings.setSourceNumStored(index, 0);
                    }

                    if (AppSettings.deleteOldImages()) {
                        deleteBitmaps(context, index);
                        AppSettings.setSourceSet(AppSettings.getSourceTitle(index), new HashSet<String>());
                    }

                    String title = AppSettings.getSourceTitle(index);
                    File file = new File(downloadCacheDir + "/" + title + " " + AppSettings.getImagePrefix());

                    if (!file.exists() || !file.isDirectory()) {
                        file.mkdir();
                    }


                    if (AppSettings.checkDuplicates()) {
                        usedLinks.addAll(AppSettings.getSourceSet(title));
                    }

                    if (AppSettings.getSourceType(index).equals(AppSettings.WEBSITE)) {
                        if (!downloadWebsite(AppSettings.getSourceData(index), index)) {
                            return null;
                        }
                    }
                    else if (AppSettings.getSourceType(index).equals(AppSettings.IMGUR)) {
                        if (!downloadImgur(AppSettings.getSourceData(index), index)) {
                            return null;
                        }
                    }

                    totalTarget += AppSettings.getSourceNum(index);

                    publishProgress("", "" + totalTarget);

                    if (imagesDownloaded == 0) {
                        publishProgress("Error with " + title + " source. No images downloaded.");
                    }
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

        private boolean downloadWebsite(String url, int index) throws IOException {


            Set<String> imageLinks = new HashSet<String>();
            List<String> imageList = new ArrayList<String>();

            linkDoc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36")
                    .referrer("http://www.google.com")
                    .get();

            imageLinks.addAll(compileImageLinks(linkDoc, "a", "href"));
            imageLinks.addAll(compileImageLinks(linkDoc, "img", "href"));
            imageLinks.addAll(compileImageLinks(linkDoc, "img", "src"));
            imageList.addAll(imageLinks);

            Collections.shuffle(imageList);
            String dir = AppSettings.getDownloadPath();
            String title = AppSettings.getSourceTitle(index);
            int stored = AppSettings.getSourceNumStored(index);
            int num = stored;

            if (imageList.size() > 0) {
                int count = 0;
                while (num < (AppSettings.getSourceNum(index) + stored) && count < imageList.size()) {

                    if (isCancelled()) {
                        return false;
                    }

                    String randLink = imageList.get(count);

                    boolean oldLink = usedLinks.contains(randLink);

                    if (!oldLink) {
                        Bitmap bitmap = getImage(randLink);
                        if (bitmap != null) {
                            writeToFile(bitmap, randLink, dir, title, num++);
                            usedLinks.add(randLink);
                            publishProgress("", "" + (totalTarget + num - stored));
                        }
                    }
                    count++;
                }
            }

            imagesDownloaded = num - stored;

            imageDetails += title + ": " + imagesDownloaded + " images;break;";

            AppSettings.setSourceNumStored(index, num);
            AppSettings.setSourceSet(title, usedLinks);

            return true;
        }

        private boolean downloadImgur(String url, int index) {


            boolean isSubreddit = url.contains("imgur.com/r/");
            boolean isAlbum = url.contains("imgur.com/a/");
            String apiUrl = url;

            if (isAlbum) {
                String albumId = url.substring(url.indexOf("imgur.com/a/") + 12);
                if (albumId.contains("/")) {
                    albumId = albumId.substring(0, albumId.indexOf("/"));
                }
                apiUrl = "https://api.imgur.com/3/album/" + albumId + "/images";
            }
            else if (isSubreddit) {
                apiUrl = "https://api.imgur.com/3/gallery/r/" + url.substring(url.indexOf("imgur.com/r/") + 12);
            }

            Log.i(TAG, "apiUrl: " + apiUrl);

            try {
                HttpParams httpParameters = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
                HttpConnectionParams.setSoTimeout(httpParameters, 5000);
                DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
                HttpGet httpGet = new HttpGet(apiUrl);
                httpGet.setHeader("Authorization", "Client-ID " + AppSettings.IMGUR_CLIENT_ID);
                httpGet.setHeader("Content-type", "application/json");

                InputStream inputStream = null;
                String result = null;
                try {
                    HttpResponse response = httpClient.execute(httpGet);
                    HttpEntity entity = response.getEntity();

                    inputStream = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                    StringBuilder stringBuilder = new StringBuilder();

                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line + "\n");
                    }
                    result = stringBuilder.toString();
                } catch (Exception e) {
                    return true;
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (Exception e) {
                        return true;
                    }
                }


                JSONObject jsonObject = new JSONObject(result);
                JSONArray jArray = jsonObject.getJSONArray("data");

                String dir = AppSettings.getDownloadPath();
                String title = AppSettings.getSourceTitle(index);
                int stored = AppSettings.getSourceNumStored(index);
                int num = stored;

                List<String> imageList = new ArrayList<String>();
                List<String> imagePages = new ArrayList<String>();

                for (int i=0; i < jArray.length(); i++)
                {
                    JSONObject imageObject = jArray.getJSONObject(i);

                    imageList.add(imageObject.getString("link"));

                    if (isSubreddit) {
                        String subredditPage = imageObject.getString("reddit_comments");
                        if (subredditPage != null && !subredditPage.equals("")) {
                            imagePages.add("http://reddit.com" + subredditPage);
                        }
                        else {
                            imagePages.add(imageObject.getString("link"));
                        }
                    }

                }

                Log.i(TAG, "imageList size: " + imageList.size());

                if (imageList.size() > 0) {
                    int count = 0;
                    while (num < (AppSettings.getSourceNum(index) + stored) && count < imageList.size()) {

                        if (isCancelled()) {
                            return false;
                        }

                        String link = imageList.get(count);
                        if (!usedLinks.contains(link)) {
                            Bitmap bitmap = getImage(link);
                            if (bitmap != null) {
                                String data = link;

                                if (isSubreddit) {
                                    data = imagePages.get(count);
                                }

                                writeToFile(bitmap, data, dir, title, num++);
                                usedLinks.add(link);
                                publishProgress("", "" + (totalTarget + num - stored));
                            }
                        }
                        count++;
                    }
                }

                imagesDownloaded = num - stored;

                imageDetails += title + ": " + imagesDownloaded + " images;break;";

                AppSettings.setSourceNumStored(index, num);
                AppSettings.setSourceSet(title, usedLinks);

            }
            catch (JSONException e) {
                e.printStackTrace();
                Log.i(TAG, "JSON parse error");
            }
            return true;
        }

        private Bitmap getImage(String url) {

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
                        return null;
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
                        return null;
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
                    connection.setConnectTimeout(5000);
                    connection.setConnectTimeout(30000);
                    connection.connect();
                    input = connection.getInputStream();

                    Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);

                    if (bitmap == null) {
                        return null;
                    }
                    return bitmap;

                }
                catch (OutOfMemoryError e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        private boolean writeToFile(Bitmap image, String saveData, String dir, String title, int imageIndex) {

            boolean returnValue = false;

            File file = new File(dir + "/" + title + " " + AppSettings.getImagePrefix() + "/" + title + " " + AppSettings.getImagePrefix() + imageIndex + ".png");

            if (file.isFile()) {
                usedLinks.remove(AppSettings.getUrl(file.getName()));
                file.delete();
            }

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
                image.compress(Bitmap.CompressFormat.PNG, 90, out);
                AppSettings.setUrl(file.getName(), saveData);
                Log.i(TAG, file.getName() + " " + saveData);
                returnValue = true;
            }
            catch (Exception e) {
                e.printStackTrace();
                returnValue = false;
            }
            finally {
                try{
                    if (out !=null) {
                        out.close();
                    }
                }
                catch(Throwable e) {
                    e.printStackTrace();
                    returnValue = false;
                }
            }

            image.recycle();
            return returnValue;
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
                if (Integer.parseInt(values[1]) <= progressMax) {
                    notifyProgress.setProgress(progressMax, Integer.parseInt(values[1]), false);
                }

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
