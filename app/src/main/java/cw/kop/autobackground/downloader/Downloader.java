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
    private static final String TAG = "Downloader";
	private static FilenameFilter fileFilter = null;
    private static int randIndex = 0;
    public static boolean isDownloading = false;
	
	private static DownloadThread downloadThread;
	
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
                    SourceListFragment sourceListFragment = ((MainActivity) appContext).sourceListFragment; //.getFragmentManager().findFragmentByTag("source_fragment");
                    if (sourceListFragment != null) {
                        sourceListFragment.resetDownload();
                    }
                }
                catch (ClassCastException e) {
                }
                return true;
            }
            downloadThread = new DownloadThread(appContext);
            downloadThread.start();
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
        if (downloadThread != null) {
            downloadThread.interrupt();
            SourceListFragment sourceListFragment = ((MainActivity) appContext).sourceListFragment; //.getFragmentManager().findFragmentByTag("source_fragment");
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
                    return filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg");
                }
			});
		}
		
		String cacheDir = AppSettings.getDownloadPath();

        List<File> bitmaps = new ArrayList<File>();

        for (int i = 0; i < AppSettings.getNumSources(); i++) {

            if (AppSettings.useSource(i)) {
                String type = AppSettings.getSourceType(i);
                if (type.equals(AppSettings.WEBSITE) || type.equals(AppSettings.IMGUR) || type.equals(AppSettings.PICASA)) {
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
                    if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
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

	public static File getNextImage(Context appContext) {
		
    	List<File> images = getBitmapList(appContext);

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

        return currentBitmapFile;
	}

    public static void decreaseIndex() {
        randIndex--;
    }
	
}
