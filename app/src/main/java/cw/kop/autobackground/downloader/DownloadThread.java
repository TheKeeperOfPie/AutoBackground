package cw.kop.autobackground.downloader;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.MainActivity;
import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.sources.SourceListFragment;

/**
 * Created by TheKeeperOfPie on 8/6/2014.
 */
public class DownloadThread extends Thread {

    private static final int NOTIFICATION_ID = 1;
    private String downloadCacheDir;
    private static final String TAG = "DownloaderAsyncTask";
    private Context appContext;
    private String imageDetails = "";
    private NotificationManager notificationManager = null;
    private Notification.Builder notifyProgress;
    private int progressMax = 0;
    private int totalDownloaded = 0;
    private int totalTarget = 0;
    private int imagesDownloaded = 0;
    private HashSet<String> usedLinks;

    public DownloadThread(Context context) {
        appContext = context;
    }

    @Override
    public void run() {
        super.run();

        Looper.prepare();

        if (AppSettings.useDownloadNotification()) {
            notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
            notifyProgress = new Notification.Builder(appContext)
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
                    Downloader.deleteBitmaps(appContext, index);
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
                        return;
                    }
                } else if (AppSettings.getSourceType(index).equals(AppSettings.IMGUR)) {
                    if (!downloadImgur(AppSettings.getSourceData(index), index)) {
                        return;
                    }
                } else if (AppSettings.getSourceType(index).equals(AppSettings.PICASA)) {
                    if (!downloadPicasa(AppSettings.getSourceData(index), index)) {
                        return;
                    }
                }

                totalTarget += AppSettings.getSourceNum(index);

                updateNotification(totalTarget);

                if (imagesDownloaded == 0) {
                    sendToast("No images downloaded from " + title);
                }
                if (imagesDownloaded < AppSettings.getSourceNum(index)) {
                    sendToast("Not enough photos from " + AppSettings.getSourceData(index) + " " +
                            "Try lowering the resolution or changing sources. " +
                            "There may also have been too many duplicates.");
                }

                totalDownloaded += imagesDownloaded;

            } catch (IOException e) {
                sendToast("Invalid URL: " + AppSettings.getSourceData(index));
                Log.i(TAG, "Invalid URL");
            } catch (IllegalArgumentException e) {
                sendToast("Invalid URL: " + AppSettings.getSourceData(index));
                Log.i(TAG, "Invalid URL");
            }
        }

        finish();
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
            if (url.contains(".png") || url.contains(".jpg") || url.contains(".jpeg")) {
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

    private boolean startDownload(List<String> links, List<String> data, int index) {
        String dir = AppSettings.getDownloadPath();
        String title = AppSettings.getSourceTitle(index);
        int stored = AppSettings.getSourceNumStored(index);
        int num = stored;

        if (links.size() > 0) {
            int count = 0;
            while (num < (AppSettings.getSourceNum(index) + stored) && count < links.size()) {

                if (isInterrupted()) {
                    cancel();
                    return false;
                }

                String randLink = links.get(count);

                boolean oldLink = usedLinks.contains(randLink);

                if (!oldLink) {
                    Bitmap bitmap = getImage(randLink);
                    if (bitmap != null) {
                        writeToFile(bitmap, data.get(count), dir, title, num++);
                        usedLinks.add(randLink);
                        updateNotification(totalTarget + num - stored);
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

    private boolean downloadWebsite(String url, int index) throws IOException {


        Set<String> imageLinks = new HashSet<String>();
        List<String> imageList = new ArrayList<String>();

        Document linkDoc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2049.0 Safari/537.36")
                .referrer("http://www.google.com")
                .get();

        imageLinks.addAll(compileImageLinks(linkDoc, "a", "href"));
        imageLinks.addAll(compileImageLinks(linkDoc, "img", "href"));
        imageLinks.addAll(compileImageLinks(linkDoc, "img", "src"));
        imageList.addAll(imageLinks);

        Collections.shuffle(imageList);

        return startDownload(imageList, imageList, index);
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
            HttpGet httpGet = new HttpGet(apiUrl);
            httpGet.setHeader("Authorization", "Client-ID " + AppSettings.IMGUR_CLIENT_ID);
            httpGet.setHeader("Content-type", "application/json");

            String response = getResponse(httpGet);
            if (response == null) {
                return false;
            }

            JSONObject jsonObject = new JSONObject(response);
            JSONArray jArray = jsonObject.getJSONArray("data");

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

            return startDownload(imageList, imagePages, index);

        }
        catch (JSONException e) {
            e.printStackTrace();
            Log.i(TAG, "JSON parse error");
        }
        return true;
    }

    private boolean downloadPicasa(String url, int index) {
        try {
            HttpGet httpGet = new HttpGet("https://picasaweb.google.com/data/feed/api/user/" + AppSettings.getGoogleAccountName() + "/albumid/6043504875137421169" + "?imgmax=d");
            httpGet.setHeader("Authorization", "OAuth " + AppSettings.getGoogleAccountToken());
            httpGet.setHeader("X-GData-Client", AppSettings.PICASA_CLIENT_ID);
            httpGet.setHeader("GData-Version", "2");

            String response = getResponse(httpGet);
            if (response == null) {
                return false;
            }

            Document linkDoc = Jsoup.parse(response);

            List<String> imageList = new ArrayList<String>();

            for (Element link : linkDoc.select("media|group")) {
                imageList.add(link.select("media|content").attr("url"));
            }

            return startDownload(imageList, imageList, index);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private String getResponse(HttpGet httpGet) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        InputStream inputStream = null;
        BufferedReader reader = null;
        String result = null;
        try {
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            inputStream = entity.getContent();
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
            StringBuilder stringBuilder = new StringBuilder();

            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            result = stringBuilder.toString();
        }
        catch (Exception e) {
            return null;
        }
        finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
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
                    Log.i(TAG, "Null bitmap");
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
        Log.i(TAG, "Possible malformed URL");
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

    private void sendToast(String message) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
    }

    private void updateNotification(int value) {

        if (AppSettings.useDownloadNotification()) {
            notifyProgress.setProgress(progressMax, value, false);

            if (Build.VERSION.SDK_INT >= 16) {
                notificationManager.notify(NOTIFICATION_ID, notifyProgress.build());
            } else {
                notificationManager.notify(NOTIFICATION_ID, notifyProgress.getNotification());
            }
        }
        else {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private void cancel() {
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }

        try {
            SourceListFragment sourceListFragment = ((MainActivity) appContext).sourceListFragment; //.getFragmentManager().findFragmentByTag("source_fragment");
            if (sourceListFragment != null) {
                sourceListFragment.resetDownload();
            }
        }
        catch (ClassCastException e) {
        }

        sendToast("Download cancelled");

        appContext = null;
        Downloader.isDownloading = false;
    }

    private void finish() {
        if (AppSettings.useDownloadNotification()) {
            Notification.Builder notifyComplete = new Notification.Builder(appContext)
                    .setContentTitle("Download Completed")
                    .setContentText("AutoBackground downloaded " + totalDownloaded + " images")
                    .setSmallIcon(R.drawable.ic_action_picture_dark);

            Notification notification;

            if (Build.VERSION.SDK_INT >= 16) {
                notifyComplete.setPriority(Notification.PRIORITY_LOW);
                Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
                inboxStyle.setBigContentTitle("Downloaded Image Details:");

                inboxStyle.addLine("Total images enabled: " + Downloader.getBitmapList(appContext).size());

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
        appContext.sendBroadcast(cycleIntent);

        try {
            SourceListFragment sourceListFragment = ((MainActivity) appContext).sourceListFragment; //.getFragmentManager().findFragmentByTag("source_fragment");
            if (sourceListFragment != null) {
                sourceListFragment.resetDownload();
            }
        }
        catch (ClassCastException e) {
        }

        appContext = null;

        Log.i(TAG, "Download Finished");
        Downloader.isDownloading = false;
    }

}
