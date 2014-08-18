/*
 * Copyright (C) Winson Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cw.kop.autobackground.downloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import cw.kop.autobackground.MainActivity;
import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.sources.SourceListFragment;

public class Downloader {

	private static File currentBitmapFile = null;
    private static final String TAG = "Downloader";
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

	public static List<File> getBitmapList() {

        FilenameFilter filenameFilter = getImageFileNameFilter();

		String cacheDir = AppSettings.getDownloadPath();

        List<File> bitmaps = new ArrayList<File>();

        for (int i = 0; i < AppSettings.getNumSources(); i++) {

            if (AppSettings.useSource(i)) {
                String type = AppSettings.getSourceType(i);
                if (type.equals(AppSettings.WEBSITE) || type.equals(AppSettings.IMGUR) || type.equals(AppSettings.PICASA)) {
                    File folder = new File(cacheDir + "/" + AppSettings.getSourceTitle(i) + " " + AppSettings.getImagePrefix());
                    if (folder.exists() && folder.isDirectory()) {
                        bitmaps.addAll(Arrays.asList(folder.listFiles(filenameFilter)));
                    }
                } else if (AppSettings.getSourceType(i).equals(AppSettings.FOLDER)) {
                    File folder = new File(AppSettings.getSourceData(i));
                    if (folder.exists() && folder.isDirectory()) {
                        bitmaps.addAll(Arrays.asList(folder.listFiles(filenameFilter)));
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

        FilenameFilter filenameFilter = getImageFileNameFilter();

        String previousPrefix = previousName + " " + AppSettings.getImagePrefix();
        String newPrefix = newName + " " + AppSettings.getImagePrefix();
        String cacheDir = AppSettings.getDownloadPath();
        String newFileName = cacheDir + "/" + newPrefix;

        File oldFolder = new File(cacheDir + "/" + previousPrefix);
        File[] fileList = oldFolder.listFiles(filenameFilter);

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
        for (File file : getBitmapList()) {
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

	public static File getNextImage() {
		
    	List<File> images = getBitmapList();

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

    public static FilenameFilter getImageFileNameFilter() {
        return new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg");
            }
        };
    }

}
