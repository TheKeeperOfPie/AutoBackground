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

package cw.kop.autobackground;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;

public class LiveWallpaperService extends GLWallpaperService {

    public static final String SET_ANIMATED = "cw.kop.autobackground.LiveWallpaperService.SET_ANIMATED";
    public static final String SET_ANIMATION_X = "cw.kop.autobackground.LiveWallpaperService.SET_ANIMATION_X";
    public static final String SET_ANIMATION_Y = "cw.kop.autobackground.LiveWallpaperService.SET_ANIMATION_Y";
    public static final String SET_FRAME_TIME = "cw.kop.autobackground.LiveWallpaperService.SET_FRAME_TIME";

    public static final String UPDATE_NOTIFICATION = "cw.kop.autobackground.LiveWallpaperService.UPDATE_NOTIFICATION";
    public static final String DOWNLOAD_WALLPAPER = "cw.kop.autobackground.LiveWallpaperService.DOWNLOAD_WALLPAPER";
    public static final String LOAD_ALBUM_ART = "cw.kop.autobackground.LiveWallpaperService.LOAD_ALBUM_ART";
    public static final String UPDATE_WALLPAPER = "cw.kop.autobackground.LiveWallpaperService.UPDATE_WALLPAPER";
    public static final String TOAST_LOCATION = "cw.kop.autobackground.LiveWallpaperService.TOAST_LOCATION";
    public static final String COPY_IMAGE = "cw.kop.autobackground.LiveWallpaperService.COPY_IMAGE";
    public static final String CYCLE_IMAGE = "cw.kop.autobackground.LiveWallpaperService.CYCLE_IMAGE";
    public static final String DELETE_IMAGE = "cw.kop.autobackground.LiveWallpaperService.DELETE_IMAGE";
    public static final String OPEN_IMAGE = "cw.kop.autobackground.LiveWallpaperService.OPEN_IMAGE";
    public static final String PIN_IMAGE = "cw.kop.autobackground.LiveWallpaperService.PIN_IMAGE";
    public static final String PREVIOUS_IMAGE = "cw.kop.autobackground.LiveWallpaperService.PREVIOUS_IMAGE";
    public static final String SHARE_IMAGE = "cw.kop.autobackground.LiveWallpaperService.SHARE_IMAGE";
    public static final String CURRENT_IMAGE = "cw.kop.autobackground.LiveWallpaperService.CURRENT_IMAGE";
    public static final String GAME_TILE0 = "cw.kop.autobackground.LiveWallpaperService.GAME_TILE0";
    public static final String GAME_TILE1 = "cw.kop.autobackground.LiveWallpaperService.GAME_TILE1";
    public static final String GAME_TILE2 = "cw.kop.autobackground.LiveWallpaperService.GAME_TILE2";
    public static final String GAME_TILE3 = "cw.kop.autobackground.LiveWallpaperService.GAME_TILE3";
    public static final String GAME_TILE4 = "cw.kop.autobackground.LiveWallpaperService.GAME_TILE4";
    public static final String GAME_TILE5 = "cw.kop.autobackground.LiveWallpaperService.GAME_TILE5";
    public static final String GAME_TILE6 = "cw.kop.autobackground.LiveWallpaperService.GAME_TILE6";
    public static final String GAME_TILE7 = "cw.kop.autobackground.LiveWallpaperService.GAME_TILE7";
    public static final String GAME_TILE8 = "cw.kop.autobackground.LiveWallpaperService.GAME_TILE8";
    public static final String GAME_TILE9 = "cw.kop.autobackground.LiveWallpaperService.GAME_TILE9";
    public static final int NUM_TO_WIN = 5;
    private static final int NOTIFICATION_ID = 0;
    private static final int NOTIFICATION_ICON_SAMPLE_SIZE = 15;
    private static final String TAG = LiveWallpaperService.class.getName();
    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            switch (action) {
                case UPDATE_NOTIFICATION:
                    startNotification(intent.getBooleanExtra("use", false));
                    break;
                case COPY_IMAGE:
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Image Location",
                            FileHandler.getBitmapLocation());
                    clipboard.setPrimaryClip(clip);
                    if (AppSettings.useToast()) {
                        Toast.makeText(context,
                                "Copied image location to clipboard",
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
                case TOAST_LOCATION:
                    Intent closeDrawer = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    context.sendBroadcast(closeDrawer);
                    Toast.makeText(context,
                            "Image Location:\n" + FileHandler.getBitmapLocation(),
                            Toast.LENGTH_LONG).show();
                    break;
                case GAME_TILE0:
                    calculateGameTiles(0);
                    break;
                case GAME_TILE1:
                    calculateGameTiles(1);
                    break;
                case GAME_TILE2:
                    calculateGameTiles(2);
                    break;
                case GAME_TILE3:
                    calculateGameTiles(3);
                    break;
                case GAME_TILE4:
                    calculateGameTiles(4);
                    break;
                case GAME_TILE5:
                    calculateGameTiles(5);
                    break;
                case GAME_TILE6:
                    calculateGameTiles(6);
                    break;
                case GAME_TILE7:
                    calculateGameTiles(7);
                    break;
                case GAME_TILE8:
                    calculateGameTiles(8);
                    break;
                case GAME_TILE9:
                    calculateGameTiles(9);
                    break;
            }
            Log.i(TAG, "Service received intent");
        }
    };
    private Target targetIcon = new Target() {

        @SuppressLint("NewApi")
        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {

            new Thread(new Runnable() {
                @Override
                public void run() {

                    if (notificationBuilder != null && notificationManager != null) {

                        if (pinned && AppSettings.usePinIndicator()) {

                            int notifyIconWidth = bitmap.getWidth();
                            int notifyIconHeight = bitmap.getHeight();

                            Drawable[] layers = new Drawable[2];
                            layers[0] = new BitmapDrawable(LiveWallpaperService.this.getResources(),
                                    bitmap);
                            layers[1] = LiveWallpaperService.this.getResources().getDrawable(R.drawable.pin_overlay);

                            LayerDrawable layerDrawable = new LayerDrawable(layers);

                            Bitmap mutableBitmap = Bitmap.createBitmap(notifyIconWidth,
                                    notifyIconHeight,
                                    Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(mutableBitmap);
                            layerDrawable.setBounds(0, 0, notifyIconWidth, notifyIconHeight);
                            layerDrawable.draw(canvas);

                            normalView.setImageViewBitmap(R.id.notification_icon, mutableBitmap);
                            if (Build.VERSION.SDK_INT >= 16) {
                                bigView.setImageViewBitmap(R.id.notification_big_icon,
                                        mutableBitmap);
                            }
                            else {
                                notificationBuilder.setLargeIcon(mutableBitmap);
                            }
                        }
                        else {
                            normalView.setImageViewBitmap(R.id.notification_icon, bitmap);
                            if (Build.VERSION.SDK_INT >= 16) {
                                bigView.setImageViewBitmap(R.id.notification_big_icon, bitmap);
                            }
                            else {
                                notificationBuilder.setLargeIcon(bitmap);
                            }
                        }
                        pushNotification();
                    }
                }
            }).start();

        }

        @Override
        public void onBitmapFailed(Drawable arg0) {
            Log.i(TAG, "Error loading bitmap into notification");
        }

        @Override
        public void onPrepareLoad(Drawable arg0) {
        }
    };
    public static SharedPreferences prefs;
    private ArrayList<Bitmap> tileBitmaps = new ArrayList<>();
    private ArrayList<Integer> tileOrder = new ArrayList<>();
    private ArrayList<Integer> usedTiles = new ArrayList<>();
    private int lastTile = 6;
    private int numFlipped = 0;
    private int tileWins = 0;
    private boolean gameSet = false;
    private int[] tileIds;
    private int tilesLoaded = 0;
    private PendingIntent pendingToastIntent;
    private PendingIntent pendingCopyIntent;
    private PendingIntent pendingCycleIntent;
    private PendingIntent pendingDeleteIntent;
    private PendingIntent pendingOpenIntent;
    private PendingIntent pendingPinIntent;
    private PendingIntent pendingPreviousIntent;
    private PendingIntent pendingShareIntent;
    private PendingIntent pendingTile0;
    private PendingIntent pendingTile1;
    private PendingIntent pendingTile2;
    private PendingIntent pendingTile3;
    private PendingIntent pendingTile4;
    private PendingIntent pendingTile5;
    private PendingIntent pendingTile6;
    private PendingIntent pendingTile7;
    private PendingIntent pendingTile8;
    private PendingIntent pendingTile9;
    private Handler handler;
    private Notification.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private RemoteViews normalView;
    private RemoteViews bigView;
    private AlarmManager alarmManager;
    private PendingIntent pendingDownloadIntent;
    private PendingIntent pendingIntervalIntent;
    private PendingIntent pendingAppIntent;
    private boolean pinned;

    public LiveWallpaperService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        AppSettings.setPrefs(prefs);

        AppSettings.resetVer1_30();
        AppSettings.resetVer1_40();
        AppSettings.resetVer2_00();

        setIntents();
        createGameIntents();

        registerReceiver(serviceReceiver, getServiceIntentFilter());
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        startAlarms();

        if (AppSettings.useNotification()) {
            startNotification(true);
        }

        Log.i(TAG, "onCreateService");
    }



    private void setIntents() {
        Intent downloadIntent = new Intent(LiveWallpaperService.DOWNLOAD_WALLPAPER);
        downloadIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        pendingDownloadIntent = PendingIntent.getBroadcast(this, 0, downloadIntent, 0);

        pendingIntervalIntent = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.UPDATE_WALLPAPER),
                0);
        pendingToastIntent = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.TOAST_LOCATION),
                0);
        pendingCopyIntent = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.COPY_IMAGE),
                0);
        pendingCycleIntent = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.CYCLE_IMAGE),
                0);
        pendingDeleteIntent = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.DELETE_IMAGE),
                0);
        pendingOpenIntent = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.OPEN_IMAGE),
                0);
        pendingPinIntent = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.PIN_IMAGE),
                0);
        pendingPreviousIntent = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.PREVIOUS_IMAGE),
                0);
        pendingShareIntent = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.SHARE_IMAGE),
                0);

        pendingAppIntent = PendingIntent.getActivity(this,
                0,
                new Intent(this, MainActivity.class),
                0);
    }

    private IntentFilter getServiceIntentFilter() {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LiveWallpaperService.UPDATE_NOTIFICATION);
        intentFilter.addAction(LiveWallpaperService.COPY_IMAGE);
        intentFilter.addAction(LiveWallpaperService.TOAST_LOCATION);
        intentFilter.addAction(LiveWallpaperService.GAME_TILE0);
        intentFilter.addAction(LiveWallpaperService.GAME_TILE1);
        intentFilter.addAction(LiveWallpaperService.GAME_TILE2);
        intentFilter.addAction(LiveWallpaperService.GAME_TILE3);
        intentFilter.addAction(LiveWallpaperService.GAME_TILE4);
        intentFilter.addAction(LiveWallpaperService.GAME_TILE5);
        intentFilter.addAction(LiveWallpaperService.GAME_TILE6);
        intentFilter.addAction(LiveWallpaperService.GAME_TILE7);
        intentFilter.addAction(LiveWallpaperService.GAME_TILE8);
        intentFilter.addAction(LiveWallpaperService.GAME_TILE9);

        return intentFilter;
    }

    private void startAlarms() {
        if (AppSettings.useTimer() && AppSettings.getTimerDuration() > 0 && PendingIntent.getBroadcast(
                getApplicationContext(),
                0,
                new Intent(LiveWallpaperService.DOWNLOAD_WALLPAPER),
                PendingIntent.FLAG_NO_CREATE) != null) {

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, AppSettings.getTimerHour());
            calendar.set(Calendar.MINUTE, AppSettings.getTimerMinute());

            alarmManager.cancel(pendingDownloadIntent);

            if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                alarmManager.setInexactRepeating(AlarmManager.RTC,
                        calendar.getTimeInMillis(),
                        AppSettings.getTimerDuration(),
                        pendingDownloadIntent);
            }
            else {
                alarmManager.setInexactRepeating(AlarmManager.RTC,
                        calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY,
                        AppSettings.getTimerDuration(),
                        pendingDownloadIntent);
            }
        }
        if (AppSettings.useInterval() && AppSettings.getIntervalDuration() > 0 && PendingIntent.getBroadcast(
                getApplicationContext(),
                0,
                new Intent(LiveWallpaperService.UPDATE_WALLPAPER),
                PendingIntent.FLAG_NO_CREATE) != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC,
                    System.currentTimeMillis() + AppSettings.getIntervalDuration(),
                    AppSettings.getIntervalDuration(),
                    pendingIntervalIntent);
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(serviceReceiver);
        notificationManager.cancel(NOTIFICATION_ID);
        alarmManager.cancel(pendingDownloadIntent);
        alarmManager.cancel(pendingIntervalIntent);
        super.onDestroy();
    }

    private void pushNotification() {

        try {
            Notification notification;
            if (Build.VERSION.SDK_INT >= 16) {
                notification = notificationBuilder.build();
                notification.bigContentView = bigView;
            }
            else {
                notification = notificationBuilder.getNotification();
            }
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
        catch (Exception e) {
            e.printStackTrace();
            if (AppSettings.useToast()) {
                Toast.makeText(LiveWallpaperService.this,
                        "Error pushing notification",
                        Toast.LENGTH_SHORT).show();
            }
        }

    }

    @SuppressLint("NewApi")
    private void notifyChangeImage() {

        if (normalView != null && bigView != null && notificationManager != null) {
            int drawable = AppSettings.getNotificationIcon();

            if (AppSettings.getNotificationTitle().equals("Location") && FileHandler.getBitmapLocation() != null) {
                normalView.setTextViewText(R.id.notification_title,
                        FileHandler.getBitmapLocation());
                normalView.setOnClickPendingIntent(R.id.notification_title, pendingToastIntent);
                if (Build.VERSION.SDK_INT >= 16) {
                    bigView.setTextViewText(R.id.notification_big_title,
                            FileHandler.getBitmapLocation());
                    bigView.setOnClickPendingIntent(R.id.notification_big_title,
                            pendingToastIntent);
                }
                else {
                    notificationBuilder.setContentTitle(FileHandler.getBitmapLocation());
                }
            }
            else {
                normalView.setOnClickPendingIntent(R.id.notification_title, null);
                bigView.setOnClickPendingIntent(R.id.notification_big_title, null);
            }

            if (AppSettings.getNotificationSummary().equals("Location") && FileHandler.getBitmapLocation() != null) {
                normalView.setTextViewText(R.id.notification_summary,
                        FileHandler.getBitmapLocation());
                normalView.setOnClickPendingIntent(R.id.notification_summary, pendingToastIntent);
                if (Build.VERSION.SDK_INT >= 16) {
                    bigView.setTextViewText(R.id.notification_big_summary,
                            FileHandler.getBitmapLocation());
                    bigView.setOnClickPendingIntent(R.id.notification_big_summary,
                            pendingToastIntent);
                }
                else {
                    notificationBuilder.setContentText(FileHandler.getBitmapLocation());
                }
            }
            else {
                normalView.setOnClickPendingIntent(R.id.notification_summary, null);
                bigView.setOnClickPendingIntent(R.id.notification_big_summary, null);
            }

            if (AppSettings.useNotificationIconFile() && AppSettings.getNotificationIconFile() != null) {

                File image = new File(AppSettings.getNotificationIconFile());

                if (image.exists() && image.isFile()) {
                    Picasso.with(LiveWallpaperService.this).load(image).resizeDimen(android.R.dimen.notification_large_icon_width,
                            android.R.dimen.notification_large_icon_height).centerCrop().into(
                            targetIcon);
                }

            }
            else if (drawable == R.drawable.ic_photo_white_24dp) {
                if (FileHandler.getCurrentBitmapFile() == null) {
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            if (!AppSettings.useHighQuality()) {
                                options.inPreferredConfig = Bitmap.Config.RGB_565;
                            }

                            if (AppSettings.useHighResolutionNotificationIcon()) {

                                options.inJustDecodeBounds = true;
                                BitmapFactory.decodeFile(FileHandler.getCurrentBitmapFile().getAbsolutePath(),
                                        options);

                                int bitWidth = options.outWidth;
                                int bitHeight = options.outHeight;
                                int minWidth = LiveWallpaperService.this.getResources().getDimensionPixelSize(
                                        android.R.dimen.notification_large_icon_width);
                                int minHeight = LiveWallpaperService.this.getResources().getDimensionPixelSize(
                                        android.R.dimen.notification_large_icon_height);
                                int sampleSize = 1;
                                if (bitHeight > minHeight || bitWidth > minWidth) {

                                    final int halfHeight = bitHeight / 2;
                                    final int halfWidth = bitWidth / 2;
                                    while ((halfHeight / sampleSize) > minHeight && (halfWidth / sampleSize) > minWidth) {
                                        sampleSize *= 2;
                                    }
                                }
                                options.inJustDecodeBounds = false;
                                options.inSampleSize = sampleSize;
                            }
                            else {
                                options.inSampleSize = NOTIFICATION_ICON_SAMPLE_SIZE;
                            }
                            Log.i(TAG, "sampleSize: " + options.inSampleSize);
                            Bitmap bitmap = BitmapFactory.decodeFile(FileHandler.getCurrentBitmapFile().getAbsolutePath(),
                                    options);
                            targetIcon.onBitmapLoaded(bitmap, null);
                        }
                        catch (OutOfMemoryError e) {
                            if (AppSettings.useToast()) {
                                Toast.makeText(LiveWallpaperService.this,
                                        "Out of memory error",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }).start();
            }
            else {
                if (pinned && AppSettings.usePinIndicator()) {
                    Drawable[] layers = new Drawable[2];
                    layers[0] = LiveWallpaperService.this.getResources().getDrawable(drawable);
                    layers[1] = LiveWallpaperService.this.getResources().getDrawable(R.drawable.pin_overlay);

                    LayerDrawable layerDrawable = new LayerDrawable(layers);

                    Bitmap mutableBitmap = Bitmap.createBitmap(layers[0].getIntrinsicWidth(),
                            layers[0].getIntrinsicHeight(),
                            Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(mutableBitmap);
                    layerDrawable.setBounds(0,
                            0,
                            layers[0].getIntrinsicWidth(),
                            layers[0].getIntrinsicHeight());
                    layerDrawable.draw(canvas);

                    normalView.setImageViewBitmap(R.id.notification_icon, mutableBitmap);
                    if (Build.VERSION.SDK_INT >= 16) {
                        bigView.setImageViewBitmap(R.id.notification_big_icon, mutableBitmap);
                    }
                    else {
                        notificationBuilder.setLargeIcon(mutableBitmap);
                    }
                }
                else {
                    normalView.setImageViewResource(R.id.notification_icon, drawable);
                    bigView.setImageViewResource(R.id.notification_big_icon, drawable);
                }
                pushNotification();
            }
        }
    }

    @SuppressLint("NewApi")
    private void startNotification(boolean useNotification) {
        if (useNotification) {
            normalView = new RemoteViews(getPackageName(), R.layout.notification_layout);
            normalView.setInt(R.id.notification_container,
                    "setBackgroundColor",
                    AppSettings.getNotificationColor());
            normalView.setImageViewResource(R.id.notification_icon, R.drawable.app_icon);
            normalView.setTextViewText(R.id.notification_title, AppSettings.getNotificationTitle());
            normalView.setInt(R.id.notification_title,
                    "setTextColor",
                    AppSettings.getNotificationTitleColor());
            normalView.setTextViewText(R.id.notification_summary,
                    AppSettings.getNotificationSummary());
            normalView.setInt(R.id.notification_summary,
                    "setTextColor",
                    AppSettings.getNotificationSummaryColor());

            Drawable coloredImageOne = LiveWallpaperService.this.getResources().getDrawable(
                    AppSettings.getNotificationOptionDrawable(0));
            Drawable coloredImageTwo = LiveWallpaperService.this.getResources().getDrawable(
                    AppSettings.getNotificationOptionDrawable(1));
            Drawable coloredImageThree = LiveWallpaperService.this.getResources().getDrawable(
                    AppSettings.getNotificationOptionDrawable(2));

            coloredImageOne.mutate().setColorFilter(AppSettings.getNotificationOptionColor(0),
                    PorterDuff.Mode.MULTIPLY);
            coloredImageTwo.mutate().setColorFilter(AppSettings.getNotificationOptionColor(1),
                    PorterDuff.Mode.MULTIPLY);
            coloredImageThree.mutate().setColorFilter(AppSettings.getNotificationOptionColor(2),
                    PorterDuff.Mode.MULTIPLY);

            Bitmap mutableBitmapOne = Bitmap.createBitmap(coloredImageOne.getIntrinsicWidth(),
                    coloredImageOne.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvasOne = new Canvas(mutableBitmapOne);
            coloredImageOne.setBounds(0,
                    0,
                    coloredImageOne.getIntrinsicWidth(),
                    coloredImageOne.getIntrinsicHeight());
            coloredImageOne.draw(canvasOne);

            Bitmap mutableBitmapTwo = Bitmap.createBitmap(coloredImageTwo.getIntrinsicWidth(),
                    coloredImageTwo.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvasTwo = new Canvas(mutableBitmapTwo);
            coloredImageTwo.setBounds(0,
                    0,
                    coloredImageTwo.getIntrinsicWidth(),
                    coloredImageTwo.getIntrinsicHeight());
            coloredImageTwo.draw(canvasTwo);

            Bitmap mutableBitmapThree = Bitmap.createBitmap(coloredImageThree.getIntrinsicWidth(),
                    coloredImageThree.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvasThree = new Canvas(mutableBitmapThree);
            coloredImageThree.setBounds(0,
                    0,
                    coloredImageThree.getIntrinsicWidth(),
                    coloredImageThree.getIntrinsicHeight());
            coloredImageThree.draw(canvasThree);

            if (AppSettings.useNotificationGame() && setupGameTiles()) {
                bigView = new RemoteViews(getPackageName(), R.layout.notification_game);
                tileIds = new int[] {
                        R.id.notification_game_tile_0,
                        R.id.notification_game_tile_1,
                        R.id.notification_game_tile_2,
                        R.id.notification_game_tile_3,
                        R.id.notification_game_tile_4,
                        R.id.notification_game_tile_5,
                        R.id.notification_game_tile_6,
                        R.id.notification_game_tile_7,
                        R.id.notification_game_tile_8,
                        R.id.notification_game_tile_9
                };
                bigView.setOnClickPendingIntent(tileIds[0], pendingTile0);
                bigView.setOnClickPendingIntent(tileIds[1], pendingTile1);
                bigView.setOnClickPendingIntent(tileIds[2], pendingTile2);
                bigView.setOnClickPendingIntent(tileIds[3], pendingTile3);
                bigView.setOnClickPendingIntent(tileIds[4], pendingTile4);
                bigView.setOnClickPendingIntent(tileIds[5], pendingTile5);
                bigView.setOnClickPendingIntent(tileIds[6], pendingTile6);
                bigView.setOnClickPendingIntent(tileIds[7], pendingTile7);
                bigView.setOnClickPendingIntent(tileIds[8], pendingTile8);
                bigView.setOnClickPendingIntent(tileIds[9], pendingTile9);
            }
            else {
                bigView = new RemoteViews(getPackageName(), R.layout.notification_big_layout);
            }
            bigView.setInt(R.id.notification_big_container,
                    "setBackgroundColor",
                    AppSettings.getNotificationColor());
            bigView.setImageViewResource(R.id.notification_big_icon, R.drawable.app_icon);
            bigView.setTextViewText(R.id.notification_big_title,
                    AppSettings.getNotificationTitle());
            bigView.setInt(R.id.notification_big_title,
                    "setTextColor",
                    AppSettings.getNotificationTitleColor());
            bigView.setTextViewText(R.id.notification_big_summary,
                    AppSettings.getNotificationSummary());
            bigView.setInt(R.id.notification_big_summary,
                    "setTextColor",
                    AppSettings.getNotificationSummaryColor());

            bigView.setImageViewBitmap(R.id.notification_button_one_image, mutableBitmapOne);
            bigView.setImageViewBitmap(R.id.notification_button_two_image, mutableBitmapTwo);
            bigView.setImageViewBitmap(R.id.notification_button_three_image, mutableBitmapThree);
            bigView.setTextViewText(R.id.notification_button_one_text,
                    AppSettings.getNotificationOptionTitle(0));
            bigView.setInt(R.id.notification_button_one_text,
                    "setTextColor",
                    AppSettings.getNotificationOptionColor(0));
            bigView.setTextViewText(R.id.notification_button_two_text,
                    AppSettings.getNotificationOptionTitle(1));
            bigView.setInt(R.id.notification_button_two_text,
                    "setTextColor",
                    AppSettings.getNotificationOptionColor(1));
            bigView.setTextViewText(R.id.notification_button_three_text,
                    AppSettings.getNotificationOptionTitle(2));
            bigView.setInt(R.id.notification_button_three_text,
                    "setTextColor",
                    AppSettings.getNotificationOptionColor(2));

            if (getIntentForNotification(AppSettings.getNotificationIconAction()) != null) {
                normalView.setOnClickPendingIntent(R.id.notification_icon,
                        getIntentForNotification(AppSettings.getNotificationIconAction()));
                bigView.setOnClickPendingIntent(R.id.notification_big_icon,
                        getIntentForNotification(AppSettings.getNotificationIconAction()));
            }
            else {
                normalView.setOnClickPendingIntent(R.id.notification_icon, pendingAppIntent);
                bigView.setOnClickPendingIntent(R.id.notification_big_icon, pendingAppIntent);
            }

            notificationBuilder = new Notification.Builder(this)
                    .setContent(normalView)
                    .setSmallIcon(R.drawable.app_icon)
                    .setOngoing(true);

            if (Build.VERSION.SDK_INT >= 16) {
                if (AppSettings.useNotificationGame()) {
                    notificationBuilder.setPriority(Notification.PRIORITY_MAX);
                }
                else {
                    notificationBuilder.setPriority(Notification.PRIORITY_MIN);
                }
                if (getIntentForNotification(AppSettings.getNotificationOptionTitle(0)) != null) {
                    bigView.setOnClickPendingIntent(R.id.notification_button_one,
                            getIntentForNotification(AppSettings.getNotificationOptionTitle(
                                    0)));
                }
                if (getIntentForNotification(AppSettings.getNotificationOptionTitle(1)) != null) {
                    bigView.setOnClickPendingIntent(R.id.notification_button_two,
                            getIntentForNotification(AppSettings.getNotificationOptionTitle(
                                    1)));
                }
                if (getIntentForNotification(AppSettings.getNotificationOptionTitle(2)) != null) {
                    bigView.setOnClickPendingIntent(R.id.notification_button_three,
                            getIntentForNotification(AppSettings.getNotificationOptionTitle(
                                    2)));
                }
            }
            else {
                notificationBuilder.setContentTitle(AppSettings.getNotificationTitle());
                notificationBuilder.setContentText(AppSettings.getNotificationSummary());
                notificationBuilder.addAction(AppSettings.getNotificationOptionDrawable(0),
                        AppSettings.getNotificationOptionTitle(0),
                        getIntentForNotification(AppSettings.getNotificationOptionTitle(
                                0)));
                notificationBuilder.addAction(AppSettings.getNotificationOptionDrawable(1),
                        AppSettings.getNotificationOptionTitle(1),
                        getIntentForNotification(AppSettings.getNotificationOptionTitle(
                                1)));
                notificationBuilder.addAction(AppSettings.getNotificationOptionDrawable(2),
                        AppSettings.getNotificationOptionTitle(2),
                        getIntentForNotification(AppSettings.getNotificationOptionTitle(
                                2)));
            }

            pushNotification();

            if (FileHandler.getCurrentBitmapFile() != null) {
                notifyChangeImage();
            }
        }
        else {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private PendingIntent getIntentForNotification(String title) {

        switch (title) {
            case "Copy":
                return pendingCopyIntent;
            case "Cycle":
                return pendingCycleIntent;
            case "Delete":
                return pendingDeleteIntent;
            case "Open":
                return pendingOpenIntent;
            case "Pin":
                return pendingPinIntent;
            case "Previous":
                return pendingPreviousIntent;
            case "Share":
                return pendingShareIntent;
        }

        return null;
    }

    private void calculateGameTiles(final int tile) {

        Log.i(TAG, "Game tile: " + tile);

        if (tileBitmaps.size() < NUM_TO_WIN) {
            setupGameTiles();
        }

        if (gameSet && tileOrder.size() == (NUM_TO_WIN * 2) && tileBitmaps.size() == NUM_TO_WIN) {

            if (!usedTiles.contains(tile) && lastTile != tile) {
                flipTile(tile);
                numFlipped++;
            }

            if (numFlipped == 2 && tile != lastTile) {
                if (tileOrder.get(tile).equals(tileOrder.get(lastTile))) {
                    setTileImage(tile, R.drawable.icon_blank);
                    setTileImage(lastTile, R.drawable.icon_blank);
                    usedTiles.add(tile);
                    usedTiles.add(lastTile);
                    tileWins++;
                }
                else {
                    setTileImage(tile, R.drawable.ic_photo_white_24dp);
                    setTileImage(lastTile, R.drawable.ic_photo_white_24dp);
                }

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= 16) {
                            notificationBuilder.setPriority(Notification.PRIORITY_MAX);
                        }
                        pushNotification();
                    }
                }, 425);

                lastTile = NUM_TO_WIN * 2;
                numFlipped = 0;
            }
            else if (numFlipped > 0 && lastTile != tile && !usedTiles.contains(tile)) {
                lastTile = tile;
            }

            if (tileWins == NUM_TO_WIN) {
                gameSet = false;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        flipAllTiles();
                    }
                }, 500);
            }
        }
    }

    private void flipTile(int tile) {
        bigView.setImageViewBitmap(tileIds[tile], tileBitmaps.get(tileOrder.get(tile)));
        pushNotification();
    }

    private void flipAllTiles() {
        for (int i = 0; i < tileIds.length; i++) {
            bigView.setImageViewBitmap(tileIds[i], tileBitmaps.get(tileOrder.get(i)));
        }

        pushNotification();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tileWins = 0;
                usedTiles.clear();
                startNotification(true);
            }
        }, 2000);
    }

    private void setTileImage(int tile, int drawable) {
        bigView.setImageViewResource(tileIds[tile], drawable);
    }

    private boolean setupGameTiles() {

        final ArrayList<File> bitmapFiles = new ArrayList<>();
        bitmapFiles.addAll(FileHandler.getBitmapList());

        if (bitmapFiles.size() >= NUM_TO_WIN) {
            Collections.shuffle(bitmapFiles);

            for (Bitmap bitmap : tileBitmaps) {
                try {
                    bitmap.recycle();
                }
                catch (Exception e) {
                }
            }

            tileBitmaps.clear();

            startLoadImageThreads(bitmapFiles);
            return true;
        }

        return false;
    }

    private void startLoadImageThreads(final ArrayList<File> files) {

        if (tileBitmaps.size() < NUM_TO_WIN) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        if (!AppSettings.useHighQuality()) {
                            options.inPreferredConfig = Bitmap.Config.RGB_565;
                        }

                        if (AppSettings.useHighResolutionNotificationIcon()) {

                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(files.get(tilesLoaded).getAbsolutePath(),
                                    options);

                            int bitWidth = options.outWidth;
                            int bitHeight = options.outHeight;
                            int minWidth = LiveWallpaperService.this.getResources().getDimensionPixelSize(
                                    android.R.dimen.notification_large_icon_width);
                            int minHeight = LiveWallpaperService.this.getResources().getDimensionPixelSize(
                                    android.R.dimen.notification_large_icon_height);
                            int sampleSize = 1;
                            if (bitHeight > minHeight || bitWidth > minWidth) {

                                final int halfHeight = bitHeight / 2;
                                final int halfWidth = bitWidth / 2;
                                while ((halfHeight / sampleSize) > minHeight && (halfWidth / sampleSize) > minWidth) {
                                    sampleSize *= 2;
                                }
                            }
                            options.inJustDecodeBounds = false;
                            options.inSampleSize = sampleSize;
                        }
                        else {
                            options.inSampleSize = NOTIFICATION_ICON_SAMPLE_SIZE;
                        }
                        Bitmap bitmap = BitmapFactory.decodeFile(files.get(tilesLoaded).getAbsolutePath(),
                                options);
                        if (tileBitmaps.size() < NUM_TO_WIN) {
                            tileBitmaps.add(bitmap);
                        }
                        setTileOrder();
                        tilesLoaded++;
                        if (tilesLoaded < NUM_TO_WIN) {
                            startLoadImageThreads(files);
                        }
                        else {
                            tilesLoaded = 0;
                        }
                    }
                    catch (OutOfMemoryError e) {
                        if (AppSettings.useToast()) {
                            Toast.makeText(LiveWallpaperService.this,
                                    "Out of memory error",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }).start();
        }
    }

    private void setTileOrder() {
        Log.i(TAG, "tileBitmaps size: " + tileBitmaps.size());

        if (tileBitmaps.size() == NUM_TO_WIN) {

            List<Integer> randomList = new ArrayList<>();
            for (int i = 0; i < NUM_TO_WIN; i++) {
                randomList.add(i);
                randomList.add(i);
            }
            Collections.shuffle(randomList);

            tileOrder.clear();

            for (int i = 0; i < NUM_TO_WIN * 2; i++) {
                tileOrder.add(randomList.get(i));
            }
            gameSet = true;

            for (int i = 0; i < NUM_TO_WIN * 2; i++) {
                setTileImage(i, R.drawable.ic_photo_white_24dp);
            }

            pushNotification();
        }
    }

    private void createGameIntents() {
        pendingTile0 = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.GAME_TILE0),
                0);
        pendingTile1 = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.GAME_TILE1),
                0);
        pendingTile2 = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.GAME_TILE2),
                0);
        pendingTile3 = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.GAME_TILE3),
                0);
        pendingTile4 = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.GAME_TILE4),
                0);
        pendingTile5 = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.GAME_TILE5),
                0);
        pendingTile6 = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.GAME_TILE6),
                0);
        pendingTile7 = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.GAME_TILE7),
                0);
        pendingTile8 = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.GAME_TILE8),
                0);
        pendingTile9 = PendingIntent.getBroadcast(this,
                0,
                new Intent(LiveWallpaperService.GAME_TILE9),
                0);

    }

    public Engine onCreateEngine() {
        return new GLWallpaperEngine();
    }

    class GLWallpaperEngine extends GLEngine {

        private static final String TAG = "WallpaperEngine";
        private final Handler handler = new Handler();
        private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = conn.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.isConnected()) {

                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && AppSettings.useWifi()) {
                        FileHandler.download(LiveWallpaperService.this);
                        unregisterReceiver(networkReceiver);
                    }
                    else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && AppSettings.useMobile()) {
                        FileHandler.download(LiveWallpaperService.this);
                        unregisterReceiver(networkReceiver);
                    }

                }
            }
        };

        private WallpaperRenderer renderer;
        private boolean toChange = false;
        private int touchCount = 0;
        private GestureDetector gestureDetector;
        private ScaleGestureDetector scaleGestureDetector;
        private List<File> previousBitmaps = new ArrayList<>();
        private long pinReleaseTime;
        private boolean downloadOnConnection = false;
        private KeyguardManager keyguardManager;

        public GLWallpaperEngine() {
            super();

            gestureDetector = new GestureDetector(getApplicationContext(),
                    new GestureDetector.SimpleOnGestureListener() {

                        @Override
                        public void onLongPress(MotionEvent e) {
                            if (AppSettings.useLongPressReset()) {
                                renderer.resetPosition();
                            }
                            super.onLongPress(e);
                        }

                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            loadNextImage();
                            return true;
                        }

                        @Override
                        public boolean onScroll(MotionEvent e1,
                                MotionEvent e2,
                                float distanceX,
                                float distanceY) {
                            if (AppSettings.useDrag() && touchCount == 2) {
                                renderer.onSwipe(distanceX,
                                        distanceY);
                                render();
                                return true;
                            }
                            return super.onScroll(e1,
                                    e2,
                                    distanceX,
                                    distanceY);
                        }


                    });

            scaleGestureDetector = new ScaleGestureDetector(LiveWallpaperService.this,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override
                        public boolean onScale(
                                ScaleGestureDetector detector) {

                            if (AppSettings.useScale()) {
                                renderer.setScaleFactor(detector.getScaleFactor());

                                render();
                                return true;
                            }
                            return false;
                        }
                    });

            pinReleaseTime = System.currentTimeMillis();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setEGLContextClientVersion(2);

            renderer = WallpaperRenderer.getInstance(LiveWallpaperService.this,
                    new WallpaperRenderer.Callback() {
                        @Override
                        public void resetMode() {
                            resetRenderMode();
                        }

                        @Override
                        public void setRenderMode(int mode) {
                            setRendererMode(mode);
                        }

                        @Override
                        public void setPreserveContext(boolean preserveContext) {
                            setPreserveEGLContextOnPause(preserveContext);
                        }

                        @Override
                        public void loadCurrent() {
                            loadCurrentImage();
                        }

                        @Override
                        public void loadPrevious() {
                            loadPreviousImage();
                        }

                        @Override
                        public void loadNext() {
                            loadNextImage();
                        }

                        @Override
                        public void loadMusic() {
                            loadMusicBitmap();
                        }

                        @Override
                        public void requestRender() {
                            render();
                        }
                    });
            renderer.setTargetFrameTime(1000 / AppSettings.getAnimationFrameRate());
            setRenderer(renderer);
            setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            keyguardManager = (KeyguardManager) LiveWallpaperService.this.getSystemService(Context.KEYGUARD_SERVICE);

            if (!isPreview()) {
                registerReceiver(updateReceiver, getEngineIntentFilter());

                IntentFilter localIntentFilter = new IntentFilter();
                localIntentFilter.addAction(SET_ANIMATED);
                localIntentFilter.addAction(SET_ANIMATION_X);
                localIntentFilter.addAction(SET_ANIMATION_Y);
                localIntentFilter.addAction(SET_FRAME_TIME);

//                LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(localReceiver, localIntentFilter);

                if (Build.VERSION.SDK_INT >= 19 && AppSettings.showAlbumArt()) {
                    Intent musicReceiverIntent = new Intent(LiveWallpaperService.this,
                            MusicReceiverService.class);
                    startService(musicReceiverIntent);
                    Log.i(TAG, "Starting service, showAlbumArt = true");
                }

                Log.i(TAG, "Registered");
            }
        }

        private IntentFilter getEngineIntentFilter() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(LiveWallpaperService.LOAD_ALBUM_ART);
            intentFilter.addAction(LiveWallpaperService.DOWNLOAD_WALLPAPER);
            intentFilter.addAction(LiveWallpaperService.CYCLE_IMAGE);
            intentFilter.addAction(LiveWallpaperService.UPDATE_WALLPAPER);
            intentFilter.addAction(LiveWallpaperService.DELETE_IMAGE);
            intentFilter.addAction(LiveWallpaperService.OPEN_IMAGE);
            intentFilter.addAction(LiveWallpaperService.PREVIOUS_IMAGE);
            intentFilter.addAction(LiveWallpaperService.PIN_IMAGE);
            intentFilter.addAction(LiveWallpaperService.SHARE_IMAGE);
            intentFilter.addAction(LiveWallpaperService.CURRENT_IMAGE);
            return intentFilter;
        }

        private void closeNotificationDrawer(Context context) {
            Intent closeDrawer = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(closeDrawer);
        }

        private void fetchAlbumArt(String artist, String album) {

            String path = null;
            String finalPath = "";

            //1. Try to get the album art from the MediaStore.Audio.Albums.ALBUM_ART column
            //Log.i(TAG, "Attempting to retrieve artwork from MediaStore ALBUM_ART column");
            String[] projection = new String[] {
                    MediaStore.Audio.Albums._ID,
                    MediaStore.Audio.Albums.ARTIST,
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.ALBUM_ART};

            Cursor cursor = getApplicationContext().getContentResolver().query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Audio.Albums.ALBUM + " ='" + album.replaceAll("'", "''") + "'"
                            + " AND "
                            + MediaStore.Audio.Albums.ARTIST + " ='" + artist.replaceAll("'",
                            "''") + "'",
                    null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                String artworkPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                if (artworkPath != null) {
                    File file = new File(artworkPath);
                    if (file.exists()) {
                        finalPath = artworkPath;
                        cursor.close();
                    }
                }
            }

            if (cursor != null) {
                cursor.close();
            }

            //2. Try to find the artwork in the MediaStore based on the trackId instead of the albumId
            //Log.d(TAG, "Attempting to retrieve artwork from MediaStore _ID column");
            projection = new String[] {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM};

            cursor = getApplicationContext().getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Audio.Albums.ALBUM + " ='" + album.replaceAll("'", "''") + "'"
                            + " AND "
                            + MediaStore.Audio.Albums.ARTIST + " ='" + artist.replaceAll("'",
                            "''") + "'",
                    null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int songId = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                Uri uri = Uri.parse("content://media/external/audio/media/" + songId + "/albumart");
                ParcelFileDescriptor pfd;
                try {
                    pfd = getApplicationContext().getContentResolver().openFileDescriptor(uri, "r");
                    if (pfd != null) {
                        finalPath = uri.toString();
                        cursor.close();
                    }
                }
                catch (Exception ignored) {
                }
            }
            if (cursor != null) {
                cursor.close();
            }

            // 3. Try to find the artwork within the folder
            //Log.d(TAG, "Attempting to retrieve artwork from folder");

            if (path != null) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash > 0) {
                    ArrayList<String> paths = new ArrayList<>();
                    String subString = path.substring(0, lastSlash + 1);
                    paths.add(subString + "AlbumArt.jpg");
                    paths.add(subString + "albumart.jpg");
                    paths.add(subString + "AlbumArt.png");
                    paths.add(subString + "albumart.png");
                    paths.add(subString + "Folder.jpg");
                    paths.add(subString + "folder.jpg");
                    paths.add(subString + "Folder.png");
                    paths.add(subString + "folder.png");
                    paths.add(subString + "Cover.jpg");
                    paths.add(subString + "cover.jpg");
                    paths.add(subString + "Cover.png");
                    paths.add(subString + "cover.png");
                    paths.add(subString + "Album.jpg");
                    paths.add(subString + "album.jpg");
                    paths.add(subString + "Album.png");
                    paths.add(subString + "album.png");

                    for (String artworkPath : paths) {
                        File file = new File(artworkPath);
                        if (file.exists()) {
                            finalPath = artworkPath;
                        }
                    }
                }
            }

            Log.i(TAG, "Final path: " + finalPath);

            Toast.makeText(LiveWallpaperService.this,
                    "Final path: " + finalPath,
                    Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            touchCount = event.getPointerCount();
            gestureDetector.onTouchEvent(event);
            scaleGestureDetector.onTouchEvent(event);
        }

        private void getNewImages() {
            ConnectivityManager connect = (ConnectivityManager) LiveWallpaperService.this.getSystemService(
                    Context.CONNECTIVITY_SERVICE);

            NetworkInfo wifi = connect.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = connect.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (wifi != null && wifi.isConnected() && AppSettings.useWifi()) {
                FileHandler.download(getApplicationContext());
                if (downloadOnConnection) {
                    unregisterReceiver(networkReceiver);
                }
            }
            else if (mobile != null && mobile.isConnected() && AppSettings.useMobile()) {
                FileHandler.download(getApplicationContext());
                if (downloadOnConnection) {
                    unregisterReceiver(networkReceiver);
                }
            }
            else if ((AppSettings.useWifi() || AppSettings.useMobile()) && AppSettings.useDownloadOnConnection()) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                registerReceiver(networkReceiver, intentFilter);
                downloadOnConnection = true;
                Log.i(TAG, "Will download on connection");
            }

        }

        @SuppressLint("NewApi")
        public void onDestroy() {
            Log.i(TAG, "onDestroy");
            if (renderer != null) {
                renderer.release();
            }
            renderer = null;
            if (!isPreview()) {
                unregisterReceiver(updateReceiver);
                LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                        localReceiver);
                if (downloadOnConnection) {
                    unregisterReceiver(networkReceiver);
                }
                if (Build.VERSION.SDK_INT >= 19) {
                    Intent musicReceiverIntent = new Intent(LiveWallpaperService.this,
                            MusicReceiverService.class);
                    stopService(musicReceiverIntent);
                }
            }
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(final boolean visible) {
            if (visible) {
                super.resume();

                if (AppSettings.preserveContext()) {
                    setPreserveEGLContextOnPause(true);
                }
                else {
                    setPreserveEGLContextOnPause(false);
                    renderer.setContextInitialized(false);
                    renderer.setLoadCurrent(true);
                }

                resetRenderMode();

                if (!keyguardManager.inKeyguardRestrictedInputMode() || AppSettings.changeWhenLocked()) {
                    if (toChange) {
                        loadNextImage();
                        toChange = false;
                    }
                    else if (AppSettings.useInterval() && AppSettings.getIntervalDuration() == 0) {
                        loadNextImage();
                    }
                }
            }
            else {
                super.pause();
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep,
                int xPixels, int yPixels) {
            super.onOffsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels);
            if (renderer != null && AppSettings.useScrolling()) {
                this.renderer.onOffsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels);
                render();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            Log.i(TAG, "onSurfaceChanged Wallpaper");
        }

        private void loadMusicBitmap() {

            // TODO: Finish album art support in renderer
//            resetRenderMode();
//
//            if (FileHandler.getMusicBitmap() == null) {
//                return;
//            }
//
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        final Bitmap bitmap = FileHandler.getMusicBitmap();
//
//                        if (bitmap != null) {
//
//                            addEvent(new Runnable() {
//
//                                @Override
//                                public void run() {
//                                    renderer.setBitmap(bitmap);
//                                }
//                            });
//
//                            if (AppSettings.useNotification()) {
//                                handler.post(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        notifyChangeImage();
//                                    }
//                                });
//                            }
//                        }
//                    }
//                    catch (OutOfMemoryError e) {
//                        if (AppSettings.useToast()) {
//                            Toast.makeText(LiveWallpaperService.this,
//                                    "Out of memory error",
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                }
//            }).start();
        }

        public void loadCurrentImage() {
            resetRenderMode();

            if (FileHandler.getCurrentBitmapFile() == null) {
                loadNextImage();
                return;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (renderer == null) {
                        return;
                    }

                    File nextImage = FileHandler.getCurrentBitmapFile();
                    if (nextImage == null) {
                        return;
                    }

                    renderer.loadNext(nextImage);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyChangeImage();
                        }
                    });
                    if (!AppSettings.shuffleImages()) {
                        FileHandler.decreaseIndex();
                    }

                    Intent loadNavPictureIntent = new Intent(MainActivity.LOAD_NAV_PICTURE);
                    LocalBroadcastManager.getInstance(LiveWallpaperService.this).sendBroadcast(
                            loadNavPictureIntent);
                }
            }).start();
        }

        private void loadPreviousImage() {
            if (pinReleaseTime > 0 && pinReleaseTime < System.currentTimeMillis()) {
                pinned = false;
            }

            if (pinned || previousBitmaps.size() == 0) {
                return;
            }

            resetRenderMode();

            if (previousBitmaps.get(0) == null) {
                return;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (renderer == null) {
                        return;
                    }

                    File nextImage = previousBitmaps.get(0);
                    if (nextImage == null) {
                        return;
                    }

                    renderer.loadNext(nextImage);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyChangeImage();
                        }
                    });
                    if (!AppSettings.shuffleImages()) {
                        FileHandler.decreaseIndex();
                    }

                    previousBitmaps.remove(0);

                    Intent loadNavPictureIntent = new Intent(MainActivity.LOAD_NAV_PICTURE);
                    LocalBroadcastManager.getInstance(LiveWallpaperService.this).sendBroadcast(
                            loadNavPictureIntent);
                }
            }).start();

        }

        private void loadNextImage() {
            if (pinReleaseTime > 0 && pinReleaseTime < System.currentTimeMillis()) {
                pinned = false;
            }

            if (pinned) {
                return;
            }

            resetRenderMode();

            previousBitmaps.add(0, FileHandler.getCurrentBitmapFile());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (renderer == null) {
                            return;
                        }

                        File nextImage = FileHandler.getNextImage();
                        if (nextImage == null) {
                            return;
                        }
//
//                        Bitmap bitmap = BitmapFactory.decodeFile(nextImage.getAbsolutePath());
//
//                        renderer.loadImageOneTexture(bitmap);

                        renderer.loadNext(nextImage);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                notifyChangeImage();
                            }
                        });

                        Intent loadNavPictureIntent = new Intent(MainActivity.LOAD_NAV_PICTURE);
                        LocalBroadcastManager.getInstance(LiveWallpaperService.this).sendBroadcast(
                                loadNavPictureIntent);
                    }
                    catch (OutOfMemoryError e) {
                        if (AppSettings.useToast()) {
                            Toast.makeText(LiveWallpaperService.this,
                                    "Out of memory error",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }).start();

        }

        private void resetRenderMode() {
            if (renderer != null) {
                if (renderer.isAnimated()) {
                    setRendererMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                }
                else {
                    setRendererMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
//                    setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                }
            }
        }

        private final BroadcastReceiver localReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                switch (action) {

                    case SET_ANIMATED:
//                        renderer.setAnimated(AppSettings.useAnimation() || AppSettings.useVerticalAnimation());
                        break;
                    case SET_ANIMATION_X:
//                        renderer.setAnimationModifierX((float) AppSettings.getAnimationSpeed() / 10);
                        break;
                    case SET_ANIMATION_Y:
//                        renderer.setAnimationModifierY((float) AppSettings.getVerticalAnimationSpeed() / 10);
                        break;
                    case SET_FRAME_TIME:
                        renderer.setTargetFrameTime(1000 / AppSettings.getAnimationFrameRate());
                        break;

                }

            }
        };

        private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                switch (action) {
                    case LiveWallpaperService.DOWNLOAD_WALLPAPER:
                        getNewImages();
                        break;
                    case LiveWallpaperService.CYCLE_IMAGE:
                        if (AppSettings.resetOnManualCycle() && AppSettings.useInterval() && AppSettings.getIntervalDuration() > 0) {
                            Intent cycleIntent = new Intent();
                            intent.setAction(LiveWallpaperService.UPDATE_WALLPAPER);
                            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                                    0,
                                    cycleIntent,
                                    0);
                            alarmManager.cancel(pendingIntent);
                            alarmManager.setInexactRepeating(AlarmManager.RTC,
                                    System.currentTimeMillis() + AppSettings.getIntervalDuration(),
                                    AppSettings.getIntervalDuration(),
                                    pendingIntent);
                        }
                        if (isVisible()) {
                            loadNextImage();
                        }
                        else {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    FileHandler.getNextImage();
                                    Intent loadNavPictureIntent = new Intent(MainActivity.LOAD_NAV_PICTURE);
                                    LocalBroadcastManager.getInstance(LiveWallpaperService.this).sendBroadcast(
                                            loadNavPictureIntent);
                                    renderer.setLoadCurrent(true);

                                    if (AppSettings.useNotification()) {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                notifyChangeImage();
                                            }
                                        });
                                    }

                                }
                            }).start();
                        }
                        break;
                    case LiveWallpaperService.DELETE_IMAGE:
                        FileHandler.deleteCurrentBitmap();
                        closeNotificationDrawer(context);
                        if (AppSettings.useToast()) {
                            Toast.makeText(LiveWallpaperService.this,
                                    "Deleted image",
                                    Toast.LENGTH_LONG).show();
                        }
                        loadNextImage();
                        break;
                    case LiveWallpaperService.OPEN_IMAGE:
                        String location = FileHandler.getBitmapLocation();
                        if (location != null) {
                            if (location.substring(0, 4).equals("http")) {
                                Intent linkIntent = new Intent(Intent.ACTION_VIEW);
                                linkIntent.setData(Uri.parse(location));
                                linkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(linkIntent);
                                closeNotificationDrawer(context);
                            }
                            else {
                                Intent galleryIntent = new Intent();
                                galleryIntent.setAction(Intent.ACTION_VIEW);
                                galleryIntent.setDataAndType(Uri.fromFile(FileHandler.getCurrentBitmapFile()),
                                        "image/*");
                                galleryIntent = Intent.createChooser(galleryIntent, "Open Image");
                                galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(galleryIntent);
                                closeNotificationDrawer(context);
                            }
                        }
                        break;
                    case LiveWallpaperService.PREVIOUS_IMAGE:
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                loadPreviousImage();
                            }
                        });
                        break;
                    case LiveWallpaperService.PIN_IMAGE:
                        if (AppSettings.getPinDuration() > 0 && !pinned) {
                            pinReleaseTime = System.currentTimeMillis() + AppSettings.getPinDuration();
                        }
                        else {
                            pinReleaseTime = 0;
                        }
                        pinned = !pinned;
                        if (AppSettings.useNotification()) {
                            notifyChangeImage();
                        }
                        break;
                    case LiveWallpaperService.SHARE_IMAGE:
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.setType("image/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM,
                                Uri.fromFile(FileHandler.getCurrentBitmapFile()));
                        shareIntent = Intent.createChooser(shareIntent, "Share Image");
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(shareIntent);
                        Intent closeDrawer = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                        context.sendBroadcast(closeDrawer);
                        break;
                    case LiveWallpaperService.UPDATE_WALLPAPER:
                        if (AppSettings.forceInterval()) {
                            loadNextImage();
                        }
                        else {
                            toChange = true;
                        }
                        break;
                    case LiveWallpaperService.LOAD_ALBUM_ART:
                        toChange = false;
                        renderer.setLoadCurrent(true);
                        if (isVisible()) {
                            loadMusicBitmap();
                        }
                        else {
                            renderer.setPlayingMusic(true);
                        }
                        break;
                    case LiveWallpaperService.CURRENT_IMAGE:
                        if (isVisible()) {
                            loadCurrentImage();
                        }
                        else {
                            renderer.setLoadCurrent(true);
                        }
                        break;
                }

            }
        };


    }
}
