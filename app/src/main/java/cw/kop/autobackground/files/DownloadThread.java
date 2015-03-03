/*
 * Copyright (C) Winson Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cw.kop.autobackground.files;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cw.kop.autobackground.BuildConfig;
import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.ApiKeys;
import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.sources.Source;

/**
 * Created by TheKeeperOfPie on 8/6/2014.
 */
public class DownloadThread extends Thread {

    public static final String DROPBOX_FILE_PREFIX = "Dropbox File: ";

    private static final String CHECK_DOWNLOAD = "Check download settings";
    private static final String NO_CHOSEN_METHOD = "No download method chosen";
    private static final String NO_WIFI = "No WiFi connection";
    private static final String NO_MOBILE = "No mobile data connection";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = DownloadThread.class.getCanonicalName();
    private final DropboxAPI<AndroidAuthSession> dropboxAPI;
    private final List<Source> sources;
    private Context appContext;
    private Handler handler;
    private String imageDetails = "";
    private NotificationManager notificationManager = null;
    private Notification.Builder notifyProgress;
    private int progressMax;
    private int totalDownloaded;
    private int totalTarget;
    private int numTarget;
    private HashSet<String> usedLinks;
    private HashSet<String> usedDropboxPaths;
    private List<File> downloadedFiles;
    private boolean isAnyLowResolution;
    private boolean isLowResolution;
    private boolean isNotEnoughNew;
    private volatile boolean interrupted;

    public DownloadThread(Context context, List<Source> sources) {
        appContext = context;
        this.sources = sources;
        AppKeyPair appKeys = new AppKeyPair(ApiKeys.DROPBOX_KEY, ApiKeys.DROPBOX_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        dropboxAPI = new DropboxAPI<>(session);
        if (AppSettings.useDropboxAccount() && !AppSettings.getDropboxAccountToken().equals("")) {
            dropboxAPI.getSession().setOAuth2AccessToken(AppSettings.getDropboxAccountToken());
        }
    }

    @Override
    public void run() {
        super.run();

        handler = new Handler(appContext.getMainLooper());

        if (AppSettings.useDownloadNotification()) {
            PendingIntent pendingStopIntent = PendingIntent.getBroadcast(appContext,
                    0,
                    new Intent(LiveWallpaperService.STOP_DOWNLOAD),
                    PendingIntent.FLAG_CANCEL_CURRENT);

            notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
            notifyProgress = new Notification.Builder(appContext)
                    .setContentTitle("AutoBackground")
                    .setContentText("Downloading images...")
                    .setSmallIcon(R.drawable.ic_photo_white_24dp);

            if (Build.VERSION.SDK_INT >= 16) {
                notifyProgress.setPriority(Notification.PRIORITY_MIN);
                notifyProgress.addAction(R.drawable.ic_cancel_white_24dp,
                        "Stop Download",
                        pendingStopIntent);
            }

            updateNotification(0);
        }

        if (!AppSettings.useWifi() && !AppSettings.useMobile()) {
            imageDetails += CHECK_DOWNLOAD + AppSettings.DATA_SPLITTER + NO_CHOSEN_METHOD + AppSettings.DATA_SPLITTER;
            sendToast(NO_CHOSEN_METHOD);
            finish();
            return;
        }

        ConnectivityManager connect = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifi = connect.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = connect.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        boolean hasWifi = wifi != null && wifi.isConnected();
        boolean hasMobile = mobile != null && mobile.isConnected();

        if (((!AppSettings.useWifi() || !hasWifi)) && ((!AppSettings.useMobile() || !hasMobile))) {
            if (AppSettings.useWifi()) {
                imageDetails += NO_WIFI + AppSettings.DATA_SPLITTER;
                sendToast(NO_WIFI);
            }
            if (AppSettings.useMobile()) {
                imageDetails += NO_MOBILE + AppSettings.DATA_SPLITTER;
                sendToast(NO_MOBILE);
            }
            finish();
            return;
        }

        String downloadCacheDir = AppSettings.getDownloadPath();

        File cache = new File(downloadCacheDir);

        if (!cache.exists() || !cache.isDirectory()) {
            cache.mkdirs();
        }

        List<Source> validSources = new ArrayList<>();
        for (Source source : sources) {
            if (!source.getType().equals(AppSettings.FOLDER) && source.isUse()) {
                validSources.add(source);
                progressMax += source.getNum();
            }
        }

        usedLinks = new HashSet<>();
        usedDropboxPaths = new HashSet<>();

        if (AppSettings.checkDuplicates()) {
            Set<String> rawLinks = AppSettings.getUsedLinks();
            for (String link : rawLinks) {
                if (link.contains(DROPBOX_FILE_PREFIX)) {
                    usedDropboxPaths.add(link.substring(link.indexOf(DROPBOX_FILE_PREFIX) + DROPBOX_FILE_PREFIX.length()));
                }
                else {
                    if (link.lastIndexOf("Time:") > 0) {
                        link = link.substring(0, link.lastIndexOf("Time:"));
                    }
                    usedLinks.add(link);
                }
            }
        }

        downloadedFiles = new ArrayList<>();

        for (Source source : validSources) {

            if (isInterrupted() || interrupted) {
                cancel();
                return;
            }

            try {

                if (AppSettings.deleteOldImages()) {
                    FileHandler.deleteBitmaps(source);
                }

                String title = source.getTitle();
                File file = new File(downloadCacheDir + "/" + title + " " + AppSettings.getImagePrefix());

                if (!file.exists() || !file.isDirectory()) {
                    file.mkdirs();
                }

                String sourceType = source.getType();

                switch (sourceType) {
                    case AppSettings.WEBSITE:
                        downloadWebsite(source);
                        break;
                    case AppSettings.IMGUR_SUBREDDIT:
                        downloadImgurSubreddit(source);
                        break;
                    case AppSettings.IMGUR_ALBUM:
                        downloadImgurAlbum(source);
                        break;
                    case AppSettings.GOOGLE_ALBUM:
                        downloadPicasa(source);
                        break;
                    case AppSettings.TUMBLR_BLOG:
                        downloadTumblrBlog(source);
                        break;
                    case AppSettings.TUMBLR_TAG:
                        downloadTumblrTag(source);
                        break;
                    case AppSettings.REDDIT_SUBREDDIT:
                        downloadRedditSubreddit(source);
                        break;
                    case AppSettings.DROPBOX_FOLDER:
                        downloadDropbox(source);
                }

                if (isInterrupted() || interrupted) {
                    cancel();
                    return;
                }

                numTarget = 0;
                totalTarget += source.getNum();

                updateNotification(totalTarget);

            }
            catch (IOException | IllegalArgumentException e) {
                sendToast("Invalid data: " + source.getData());
                Log.i(TAG, "Invalid data");
            }
        }
        finish();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        interrupted = true;
        Log.i(TAG, "DownloadThread interrupted");
    }

    private Set<String> compileImageLinks(Document doc, String tag, String attr) {

        Elements downloadLinks = doc.select(tag);
        Set<String> links = new HashSet<>();

        for (Element link : downloadLinks) {
            String url = link.attr(attr);
            if (!url.contains("http")) {
                url = "http:" + url;
            }
            if (link.attr("width") != null && !link.attr("width").equals("")) {
                try {
                    if (Integer.parseInt(link.attr("width")) < AppSettings.getImageWidth() || Integer.parseInt(
                            link.attr("height")) < AppSettings.getImageHeight()) {
                        continue;
                    }
                }
                catch (NumberFormatException e) {
                }
            }
            if (url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".jpeg")) {
                links.add(url);
            }
            else if (AppSettings.forceDownload() && url.length() > 5 && (url.endsWith(".com") || url.endsWith(
                    ".org") || url.endsWith(".net"))) {
                links.add(url + ".png");
                links.add(url + ".jpg");
                links.add(url);
            }
        }
        return links;
    }

    private void startDownload(List<String> links, List<String> data, Source source) {
        String dir = AppSettings.getDownloadPath();
        String title = source.getTitle();
        int targetNum = source.getNum();
        int numDownloaded = 0;

        isLowResolution = false;
        Set<File> downloadedFiles = new HashSet<>();

        for (int count = 0; numDownloaded < targetNum && count < links.size(); count++) {
            if (isInterrupted() || interrupted) {
                removeExtras(dir, title, targetNum, downloadedFiles);
                return;
            }

            String randLink = links.get(count);
            String randData = data.get(count);

            boolean newLink = usedLinks.add(randData);

            if (newLink) {

                Bitmap bitmap = getImage(randLink);

                if (bitmap != null && !interrupted) {
                    long time = System.currentTimeMillis();
                    File file = new File(dir + "/" + title + " " + AppSettings.getImagePrefix() + "/" + title + " " + AppSettings.getImagePrefix() + " " + time + ".png");

                    if (AppSettings.useImageHistory()) {
                        AppSettings.addUsedLink(randData, time);
                        if (AppSettings.cacheThumbnails()) {
                            writeToFileWithThumbnail(bitmap,
                                    randData,
                                    dir,
                                    file,
                                    time);
                        }
                        else {
                            writeToFile(bitmap, randData, file);
                        }
                    }
                    else {
                        writeToFile(bitmap, randData, file);
                    }
                    downloadedFiles.add(file);
                    numDownloaded++;
                    updateNotification(++numTarget + totalTarget);
                }
            }
        }

        removeExtras(dir, title, targetNum, downloadedFiles);
        imageDetails += title + ": " + numDownloaded + " / " + targetNum + " images" + AppSettings.DATA_SPLITTER;

        if (numDownloaded < targetNum) {
            if (isLowResolution) {
                isAnyLowResolution = true;
            }
            else {
                isNotEnoughNew = true;
            }
        }

        totalDownloaded += numDownloaded;
    }

    private void removeExtras(String dir,
            String title,
            int targetNum,
            Set<File> downloadedFiles) {

        File mainDir = new File(dir + "/" + title + " " + AppSettings.getImagePrefix());
        FilenameFilter filenameFilter = FileHandler.getImageFileNameFilter();

        List<File> files = new ArrayList<>(Arrays.asList(mainDir.listFiles(filenameFilter)));
        files.removeAll(downloadedFiles);

        if (!AppSettings.keepImages()) {
            int extra = mainDir.list(filenameFilter).length - targetNum;
            while (extra > 0 && files.size() > 0) {
                File file = files.get(0);
                AppSettings.clearUrl(file.getName());
                file.delete();
                files.remove(file);
                extra--;
            }
        }
    }

    private void downloadWebsite(Source source) throws IOException {

        if (isInterrupted() || interrupted) {
            return;
        }

        Set<String> imageLinks = new HashSet<>();
        List<String> imageList = new ArrayList<>();

        Document linkDoc = Jsoup.connect(source.getData()).get();

        imageLinks.addAll(compileImageLinks(linkDoc, "a", "href"));
        imageLinks.addAll(compileImageLinks(linkDoc, "img", "href"));
        imageLinks.addAll(compileImageLinks(linkDoc, "img", "src"));
        imageList.addAll(imageLinks);

        Log.i(TAG, "imageLinks: " + imageList.toString());

        startDownload(imageList, imageList, source);
    }

    private void downloadImgurSubreddit(Source source) {

        if (isInterrupted() || interrupted) {
            return;
        }

        String apiUrl = "https://api.imgur.com/3/gallery/r/" + source.getData();

        Log.i(TAG, "apiUrl: " + apiUrl);

        try {
            HttpGet httpGet = new HttpGet(apiUrl);
            httpGet.setHeader("Authorization", "Client-ID " + ApiKeys.IMGUR_CLIENT_ID);
            httpGet.setHeader("Content-type", "application/json");

            String response = getResponse(httpGet);
            if (response == null) {
                return;
            }

            JSONObject jsonObject = new JSONObject(response);
            JSONArray jArray = jsonObject.getJSONArray("data");

            List<String> imageList = new ArrayList<>();
            List<String> imagePages = new ArrayList<>();

            for (int i = 0; i < jArray.length(); i++) {
                JSONObject imageObject = jArray.getJSONObject(i);

                imageList.add(imageObject.getString("link"));

                String subredditPage = imageObject.getString("reddit_comments");
                if (subredditPage != null && !subredditPage.equals("")) {
                    imagePages.add("http://reddit.com" + subredditPage);
                }
                else {
                    imagePages.add(imageObject.getString("link"));
                }
            }

            Log.i(TAG, "imageList size: " + imageList.size());

            startDownload(imageList, imagePages, source);

        }
        catch (JSONException e) {
            e.printStackTrace();
            Log.i(TAG, "JSON parse error");
        }
    }

    private void downloadImgurAlbum(Source source) {

        if (isInterrupted() || interrupted) {
            return;
        }

        String albumId = source.getData();

        if (albumId.contains("/")) {
            albumId = albumId.substring(0, albumId.indexOf("/"));
        }
        String apiUrl = "https://api.imgur.com/3/album/" + albumId + "/images";

        Log.i(TAG, "apiUrl: " + apiUrl);

        try {
            HttpGet httpGet = new HttpGet(apiUrl);
            httpGet.setHeader("Authorization", "Client-ID " + ApiKeys.IMGUR_CLIENT_ID);
            httpGet.setHeader("Content-type", "application/json");

            String response = getResponse(httpGet);
            if (response == null) {
                return;
            }

            JSONObject jsonObject = new JSONObject(response);
            JSONArray jArray = jsonObject.getJSONArray("data");

            List<String> imageList = new ArrayList<>();

            for (int i = 0; i < jArray.length(); i++) {
                JSONObject imageObject = jArray.getJSONObject(i);

                imageList.add(imageObject.getString("link"));

            }

            Log.i(TAG, "imageList size: " + imageList.size());

            startDownload(imageList, imageList, source);

        }
        catch (JSONException e) {
            e.printStackTrace();
            Log.i(TAG, "JSON parse error");
        }
    }

    private void downloadPicasa(Source source) {

        if (isInterrupted() || interrupted) {
            return;
        }

        String data = source.getData();

        if (data.contains("user/")) {
            data = data.substring(data.indexOf("user/"));
        }
        else {
            return;
        }

        HttpGet httpGet = new HttpGet("https://picasaweb.google.com/data/feed/api/" + data + "?imgmax=d");
        httpGet.setHeader("Authorization", "OAuth " + AppSettings.getGoogleAccountToken());
        httpGet.setHeader("X-GData-Client", ApiKeys.PICASA_CLIENT_ID);
        httpGet.setHeader("GData-Version", "2");

        String response = getResponse(httpGet);
        if (response == null) {
            return;
        }

        Document linkDoc = Jsoup.parse(response);

        List<String> imageList = new ArrayList<>();

        for (Element link : linkDoc.select("media|group")) {
            imageList.add(link.select("media|content").attr("url"));
        }

        startDownload(imageList, imageList, source);

    }

    private void downloadTumblrBlog(Source source) {

        if (isInterrupted() || interrupted) {
            return;
        }

        try {
            HttpGet httpGet = new HttpGet("http://api.tumblr.com/v2/blog/" + source.getData() + ".tumblr.com/posts/photo?api_key=" + ApiKeys.TUMBLR_CLIENT_ID);

            String response = getResponse(httpGet);
            if (response == null) {
                return;
            }

            JSONObject jsonObject = new JSONObject(response);
            JSONArray jArray = jsonObject.getJSONObject("response").getJSONArray("posts");

            List<String> imageList = new ArrayList<>();
            List<String> imagePages = new ArrayList<>();

            for (int i = 0; i < jArray.length(); i++) {
                JSONObject postObject = jArray.getJSONObject(i);

                String postUrl = postObject.getString("post_url");

                JSONArray imageArray = postObject.getJSONArray("photos");

                for (int imageIndex = 0; imageIndex < imageArray.length(); imageIndex++) {
                    imageList.add(imageArray.getJSONObject(imageIndex).getJSONObject("original_size").getString(
                            "url"));
                    imagePages.add(postUrl);
                }

            }

            Log.i(TAG, "imageList size: " + imageList.size());

            startDownload(imageList, imagePages, source);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void downloadTumblrTag(Source source) {

        try {
            HttpGet httpGet = new HttpGet("http://api.tumblr.com/v2/tagged?tag=" + source.getData() + "&api_key=" + ApiKeys.TUMBLR_CLIENT_ID);

            String response = getResponse(httpGet);
            if (response == null) {
                return;
            }

            JSONObject jsonObject = new JSONObject(response);
            JSONArray jArray = jsonObject.getJSONArray("response");

            List<String> imageList = new ArrayList<>();
            List<String> imagePages = new ArrayList<>();

            for (int i = 0; i < jArray.length(); i++) {
                try {
                    JSONObject postObject = jArray.getJSONObject(i);

                    String postUrl = postObject.getString("post_url");

                    JSONArray imageArray = postObject.getJSONArray("photos");

                    for (int imageIndex = 0; imageIndex < imageArray.length(); imageIndex++) {
                        imageList.add(imageArray.getJSONObject(imageIndex).getJSONObject(
                                "original_size").getString("url"));
                        imagePages.add(postUrl);
                    }
                }
                catch (JSONException e) {
                }

            }

            Log.i(TAG, "imageList size: " + imageList.size());

            startDownload(imageList, imagePages, source);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void downloadRedditSubreddit(Source source) {
        if (isInterrupted() || interrupted) {
            return;
        }

        String apiUrl = "https://reddit.com/r/" + source.getData() + "/hot/.json?limit=100";

        Log.i(TAG, "apiUrl: " + apiUrl);

        try {
            HttpGet httpGet = new HttpGet(apiUrl);
            httpGet.setHeader("User-Agent",
                    "AutoBackground/" + BuildConfig.VERSION_NAME + " by TheKeeperOfPie");

            String response = getResponse(httpGet);
            if (response == null) {
                return;
            }

            JSONObject jsonObject = new JSONObject(response);

            Log.i(TAG, "Reddit return JSON: " + jsonObject.toString());

            JSONArray jArray = jsonObject.getJSONObject("data").getJSONArray("children");

            List<String> imageList = new ArrayList<>();
            List<String> imagePages = new ArrayList<>();

            for (int i = 0; i < jArray.length(); i++) {
                JSONObject linkObject = jArray.getJSONObject(i).getJSONObject("data");
                if (i == 0) {
                    Log.i(TAG, "First object: " + linkObject.toString());
                }
                imageList.add(linkObject.getString("url"));
                imagePages.add("https://reddit.com" + linkObject.getString("permalink"));

            }

            Log.i(TAG, "imageList size: " + imageList.size());
            Log.i(TAG, "imageList " + imageList.toString());
            Log.i(TAG, "imagePages " + imagePages.toString());

            startDownload(imageList, imagePages, source);

        }
        catch (JSONException e) {
            e.printStackTrace();
            Log.i(TAG, "JSON parse error");
        }
    }

    private void downloadDropbox(Source source) {

        try {
            List<Entry> entries = dropboxAPI.metadata(source.getData(), 0, null, true, null).contents;

            List<String> imageLinks = new ArrayList<>();
            List<String> imageData = new ArrayList<>();

            for (Entry entry : entries) {
                if (!entry.isDir &&
                        !usedDropboxPaths.contains(entry.path) &&
                        (entry.path.contains(".jpg") || entry.path.contains(".jpeg") || entry.path.contains(
                                ".png"))) {
                    imageLinks.add(dropboxAPI.media(entry.path, true).url);
                    imageData.add(DROPBOX_FILE_PREFIX + entry.path);
                }
            }

            startDownload(imageLinks, imageData, source);

        }
        catch (DropboxException e) {
            e.printStackTrace();
        }

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
                stringBuilder.append(line).append("\n");
            }
            result = stringBuilder.toString();
        }
        catch (IOException e) {
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
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    private Bitmap getImage(String url) {

        if (Patterns.WEB_URL.matcher(url).matches()) {
            try {
                int minWidth = AppSettings.getImageWidth();
                int minHeight = AppSettings.getImageHeight();
                URL imageUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                connection.connect();
                InputStream input = connection.getInputStream();

                if (!connection.getHeaderField("Content-Type").startsWith("image/") && !AppSettings.forceDownload()) {
                    Log.i(TAG, "Not an image: " + url);
                    return null;
                }

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.inPreferQualityOverSpeed = true;
                options.inDither = true;

                BitmapFactory.decodeStream(input, null, options);

                input.close();

                int bitWidth = options.outWidth;
                int bitHeight = options.outHeight;
                options.inJustDecodeBounds = false;

                Log.i(TAG, "bitWidth: " + bitWidth + " bitHeight: " + bitHeight);

                if (bitWidth + 1 < minWidth || bitHeight + 1 < minHeight) {
                    isLowResolution = true;
                    return null;
                }

                int sampleSize = 1;

                if (!AppSettings.useFullResolution()) {

                    if (bitHeight > minHeight || bitWidth > minWidth) {

                        final int halfHeight = bitHeight / 2;
                        final int halfWidth = bitWidth / 2;
                        while ((halfHeight / sampleSize) > minHeight && (halfWidth / sampleSize) > minWidth) {
                            sampleSize *= 2;
                        }
                    }
                }

                options.inSampleSize = sampleSize;

                connection = (HttpURLConnection) imageUrl.openConnection();
                connection.setConnectTimeout(5000);
                connection.connect();
                input = connection.getInputStream();

                Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);

                if (bitmap == null) {
                    Log.i(TAG, "Null bitmap");
                    return null;
                }
                return bitmap;

            }
            catch (java.io.InterruptedIOException e) {
                DownloadThread.this.interrupt();
                Log.i(TAG, "Interrupted");
            }
            catch (OutOfMemoryError | IOException e) {
                DownloadThread.this.interrupt();
                e.printStackTrace();
            }
        }
        Log.i(TAG, "Possible malformed URL");
        return null;
    }

    private void writeToFile(Bitmap image, String saveData, File file) {

        if (file.isFile()) {
            file.delete();
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, out);
            AppSettings.setUrl(file.getName(), saveData);
            Log.i(TAG, file.getName() + " " + saveData);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }

        image.recycle();
        image = null;
        System.gc();
    }

    private void writeToFileWithThumbnail(Bitmap image,
            String saveData,
            String dir,
            File file,
            long time) {

        if (file.isFile()) {
            file.delete();
        }

        FileOutputStream out = null;
        FileOutputStream thumbnailOut = null;
        try {
            int bitWidth = image.getWidth();
            int bitHeight = image.getHeight();

            float thumbnailSize = (float) AppSettings.getThumbnailSize();

            Bitmap thumbnail;

            if (thumbnailSize < bitWidth && thumbnailSize < bitHeight) {
                Matrix matrix = new Matrix();
                if (bitWidth > bitHeight) {
                    matrix.postScale(thumbnailSize / bitWidth, thumbnailSize / bitWidth);
                }
                else {
                    matrix.postScale(thumbnailSize / bitHeight, thumbnailSize / bitHeight);
                }
                thumbnail = Bitmap.createBitmap(image, 0, 0, bitWidth, bitHeight, matrix, false);
            }
            else {
                thumbnail = image;
            }

            out = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, out);

            File thumbnailCache = new File(dir + "/HistoryCache");

            if (!thumbnailCache.exists() || (thumbnailCache.exists() && !thumbnailCache.isDirectory())) {
                thumbnailCache.mkdirs();
            }

            File thumbnailFile = new File(thumbnailCache.getAbsolutePath() + "/" + time + ".png");

            thumbnailOut = new FileOutputStream(thumbnailFile);

            thumbnail.compress(Bitmap.CompressFormat.PNG, 90, thumbnailOut);

            Log.i(TAG, "Thumbnail written: " + thumbnailFile.getAbsolutePath());

            AppSettings.setUrl(file.getName(), saveData);
            Log.i(TAG, file.getName() + " " + saveData);
            thumbnail.recycle();
            thumbnail = null;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (thumbnailOut != null) {
                    thumbnailOut.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        image.recycle();
        image = null;
        System.gc();
    }

    private void sendToast(final String message) {
        if (AppSettings.useToast()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateNotification(int value) {

        if (notificationManager != null && notifyProgress != null) {
            if (AppSettings.useDownloadNotification()) {
                notifyProgress.setProgress(progressMax, value, false);

                if (Build.VERSION.SDK_INT >= 16) {
                    notificationManager.notify(NOTIFICATION_ID, notifyProgress.build());
                }
                else {
                    notificationManager.notify(NOTIFICATION_ID, notifyProgress.getNotification());
                }
            }
            else {
                notificationManager.cancel(NOTIFICATION_ID);
            }
        }
    }

    private void cancel() {
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }

        Intent resetDownloadIntent = new Intent(FileHandler.DOWNLOAD_TERMINATED);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(resetDownloadIntent);

        appContext = null;
        AppSettings.checkUsedLinksSize();
        FileHandler.setIsDownloading(false);
    }

    private void finish() {
        if (AppSettings.useDownloadNotification()) {
            Notification.Builder notifyComplete = new Notification.Builder(appContext)
                    .setContentTitle("Download Completed")
                    .setContentText("AutoBackground downloaded " + totalDownloaded + " images")
                    .setSmallIcon(R.drawable.ic_photo_white_24dp);

            Notification notification;

            if (Build.VERSION.SDK_INT >= 16) {
                notifyComplete.setPriority(Notification.PRIORITY_MAX);
                Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
                inboxStyle.setBigContentTitle("Downloaded Image Details:");

                inboxStyle.addLine("Total images enabled: " + FileHandler.getBitmapList().size());

                if (isAnyLowResolution) {
                    inboxStyle.addLine("Not enough suitable resolution images");
                }

                if (isNotEnoughNew) {
                    inboxStyle.addLine("Not enough new images");
                }

                for (String detail : imageDetails.split(AppSettings.DATA_SPLITTER)) {
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

        Intent resetDownloadIntent = new Intent(FileHandler.DOWNLOAD_TERMINATED);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(resetDownloadIntent);

        Intent intent = new Intent();
        intent.setAction(LiveWallpaperService.UPDATE_NOTIFICATION);
        intent.putExtra("use", AppSettings.useNotification());
        appContext.sendBroadcast(intent);

        AppSettings.checkUsedLinksSize();
        handler.post(new Runnable() {
            @Override
            public void run() {
                appContext = null;
            }
        });

        Log.i(TAG, "Download Finished");
        FileHandler.setIsDownloading(false);
    }
}
