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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import cw.kop.autobackground.settings.AppSettings;

public class FileHandler {

    public static final String DOWNLOAD_TERMINATED = "cw.kop.autobackground.files.FileHandler.DOWNLOAD_TERMINATED";
    public static volatile boolean isDownloading = false;

    private static final String TAG = "FileHandler";
    private static Bitmap musicBitmap = null;
    private static File currentBitmapFile = null;
    private static int randIndex = 0;
    private static DownloadThread downloadThread;

    public static boolean download(Context appContext) {

        if (!isDownloading) {
            ConnectivityManager connect = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo wifi = connect.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = connect.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (wifi != null && wifi.isConnected() && AppSettings.useWifi()) {
            }
            else if (mobile != null && mobile.isConnected() && AppSettings.useMobile()) {
            }
            else {
                if (AppSettings.useToast()) {
                    Toast.makeText(appContext,
                            "No connection available,\ncheck Download Settings",
                            Toast.LENGTH_SHORT).show();
                }

                Intent resetDownloadIntent = new Intent(FileHandler.DOWNLOAD_TERMINATED);
                LocalBroadcastManager.getInstance(appContext).sendBroadcast(resetDownloadIntent);
                return false;
            }
            isDownloading = true;
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

    public static void setIsDownloading(boolean value) {
        isDownloading = value;
    }

    public static void cancel(Context appContext) {
        if (downloadThread != null) {
            downloadThread.interrupt();
            Log.i(TAG, "Download interrupted");
            Intent resetDownloadIntent = new Intent(FileHandler.DOWNLOAD_TERMINATED);
            LocalBroadcastManager.getInstance(appContext).sendBroadcast(resetDownloadIntent);
        }
        isDownloading = false;
    }

    public static boolean hasImages() {

        boolean noImages = true;

        FilenameFilter filenameFilter = getImageFileNameFilter();

        String cacheDir = AppSettings.getDownloadPath();

        for (int index = 0; noImages && index < AppSettings.getNumSources(); index++) {

            if (AppSettings.useSource(index)) {
                String type = AppSettings.getSourceType(index);
                if (type.equals(AppSettings.FOLDER)) {

                    String[] folders = AppSettings.getSourceData(index).split(AppSettings.DATA_SPLITTER);

                    for (int folderIndex = 0; noImages && folderIndex < folders.length; folderIndex++) {
                        File folder = new File(folders[folderIndex]);
                        if (folder.exists() && folder.isDirectory() && folder.listFiles(
                                filenameFilter).length > 0) {
                            noImages = false;
                        }
                    }
                }
                else {
                    File folder = new File(cacheDir + "/" + AppSettings.getSourceTitle(index) + " " + AppSettings.getImagePrefix());
                    if (folder.exists() && folder.isDirectory() && folder.listFiles(filenameFilter).length > 0) {
                        noImages = false;
                    }
                }
            }
        }

        return noImages;
    }

    public static List<File> getBitmapList() {

        FilenameFilter filenameFilter = getImageFileNameFilter();

        String cacheDir = AppSettings.getDownloadPath();

        List<File> bitmaps = new ArrayList<>();

        for (int i = 0; i < AppSettings.getNumSources(); i++) {

            if (AppSettings.useSource(i)) {

                if (AppSettings.useSourceTime(i)) {
                    String[] timeArray = AppSettings.getSourceTime(i).split(":|[ -]+");

                    try {
                        int startTime = Integer.parseInt(timeArray[0] + "" + timeArray[1]);
                        int endTime = Integer.parseInt(timeArray[2] + "" + timeArray[3]);
                        int currentTime = Integer.parseInt(new SimpleDateFormat("HHmm").format(new Date()));

                        Log.i(TAG, "currentTime: " + currentTime);

                        if (startTime < endTime) {
                            if (currentTime < startTime || currentTime > endTime) {
                                continue;
                            }
                        }
                        else {
                            if (!(currentTime > startTime || currentTime < endTime)) {
                                continue;
                            }
                        }

                    }
                    catch (NumberFormatException e) {
                        Log.w(TAG, "Error parsing time");
                    }
                }

                String type = AppSettings.getSourceType(i);
                if (type.equals(AppSettings.FOLDER)) {

                    for (String folderName : AppSettings.getSourceData(i).split(AppSettings.DATA_SPLITTER)) {
                        File folder = new File(folderName);
                        if (folder.exists() && folder.isDirectory()) {
                            bitmaps.addAll(Arrays.asList(folder.listFiles(filenameFilter)));
                        }
                    }
                }
                else {
                    File folder = new File(cacheDir + "/" + AppSettings.getSourceTitle(i) + " " + AppSettings.getImagePrefix());
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

    public static void setCurrentBitmapFile(File file) {
        currentBitmapFile = file;
    }

    public static Bitmap getMusicBitmap() {
        return musicBitmap;
    }

    public static void setMusicBitmap(Bitmap bitmap) {
        musicBitmap = bitmap;
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
        Log.i(TAG, "Deleted: " + currentBitmapFile.delete());
    }

    public static void deleteAllBitmaps(Context appContext) {
        for (int i = 0; i < AppSettings.getNumSources(); i++) {
            AppSettings.setSourceSet(AppSettings.getSourceTitle(i), new HashSet<String>());
        }
        File mainDir = new File(AppSettings.getDownloadPath());

        for (File folder : mainDir.listFiles()) {
            if (folder.exists() && folder.isDirectory()) {
                for (File file : folder.listFiles()) {
                    if (file.exists() && file.isFile() && file.getName().contains(AppSettings.getImagePrefix())) {
                        file.delete();
                    }
                }
            }
        }

    }

    public static void deleteBitmaps(Context appContext, int position) {

        File folder = new File(AppSettings.getDownloadPath() + "/" + AppSettings.getSourceTitle(
                position) + " " + AppSettings.getImagePrefix());

        Log.i(TAG, folder.getAbsolutePath());

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

        Log.i("FileHandler", "Getting next image");

        if (!AppSettings.shuffleImages()) {
            randIndex++;
        }
        else if (images.size() > 0) {
            randIndex += new Random().nextInt(images.size() - 2) + 1;
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

        return currentBitmapFile;
    }

    public static void decreaseIndex() {
        randIndex--;
    }

    public static FilenameFilter getImageFileNameFilter() {
        return new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(
                        ".jpeg");
            }
        };
    }

}
