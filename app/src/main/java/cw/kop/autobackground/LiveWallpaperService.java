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
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.media.effect.EffectUpdateListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cw.kop.autobackground.downloader.Downloader;
import cw.kop.autobackground.settings.AppSettings;

public class LiveWallpaperService extends GLWallpaperService {

    public static final String UPDATE_NOTIFICATION = "cw.kop.autobackgrond.UPDATE_NOTIFICATION";
    public static final String DOWNLOAD_WALLPAPER = "cw.kop.autobackgrond.DOWNLOAD_WALLPAPER";
    public static final String LOAD_ALBUM_ART = "cw.kop.autobackgrond.LOAD_ALBUM_ART";
    public static final String UPDATE_WALLPAPER = "cw.kop.autobackgrond.UPDATE_WALLPAPER";
    public static final String TOAST_LOCATION = "cw.kop.autobackgrond.TOAST_LOCATION";
    public static final String COPY_IMAGE = "cw.kop.autobackgrond.COPY_IMAGE";
    public static final String CYCLE_IMAGE = "cw.kop.autobackgrond.CYCLE_IMAGE";
    public static final String DELETE_IMAGE = "cw.kop.autobackgrond.DELETE_IMAGE";
    public static final String OPEN_IMAGE = "cw.kop.autobackgrond.OPEN_IMAGE";
    public static final String PIN_IMAGE = "cw.kop.autobackgrond.PIN_IMAGE";
    public static final String PREVIOUS_IMAGE = "cw.kop.autobackgrond.PREVIOUS_IMAGE";
    public static final String SHARE_IMAGE = "cw.kop.autobackgrond.SHARE_IMAGE";
    public static final String CURRENT_IMAGE = "cw.kop.autobackgrond.CURRENT_IMAGE";
    public static final String GAME_TILE0 = "cw.kop.autobackgrond.GAME_TILE0";
    public static final String GAME_TILE1 = "cw.kop.autobackgrond.GAME_TILE1";
    public static final String GAME_TILE2 = "cw.kop.autobackgrond.GAME_TILE2";
    public static final String GAME_TILE3 = "cw.kop.autobackgrond.GAME_TILE3";
    public static final String GAME_TILE4 = "cw.kop.autobackgrond.GAME_TILE4";
    public static final String GAME_TILE5 = "cw.kop.autobackgrond.GAME_TILE5";
    public static final String GAME_TILE6 = "cw.kop.autobackgrond.GAME_TILE6";
    public static final String GAME_TILE7 = "cw.kop.autobackgrond.GAME_TILE7";
    public static final String GAME_TILE8 = "cw.kop.autobackgrond.GAME_TILE8";
    public static final String GAME_TILE9 = "cw.kop.autobackgrond.GAME_TILE9";
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
                    ClipData clip = ClipData.newPlainText("Image Location", Downloader.getBitmapLocation());
                    clipboard.setPrimaryClip(clip);
                    if (AppSettings.useToast()) {
                        Toast.makeText(context, "Copied image location to clipboard", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case TOAST_LOCATION:
                    Intent closeDrawer = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    context.sendBroadcast(closeDrawer);
                    Toast.makeText(context, "Image Location:\n" + Downloader.getBitmapLocation(), Toast.LENGTH_LONG).show();
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
                            layers[0] = new BitmapDrawable(LiveWallpaperService.this.getResources(), bitmap);
                            layers[1] = LiveWallpaperService.this.getResources().getDrawable(R.drawable.pin_overlay);

                            LayerDrawable layerDrawable = new LayerDrawable(layers);

                            Bitmap mutableBitmap = Bitmap.createBitmap(notifyIconWidth, notifyIconHeight, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(mutableBitmap);
                            layerDrawable.setBounds(0, 0, notifyIconWidth, notifyIconHeight);
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

        pendingIntervalIntent = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.UPDATE_WALLPAPER), 0);
        pendingToastIntent = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.TOAST_LOCATION), 0);
        pendingCopyIntent = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.COPY_IMAGE), 0);
        pendingCycleIntent = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.CYCLE_IMAGE), 0);
        pendingDeleteIntent = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.DELETE_IMAGE), 0);
        pendingOpenIntent = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.OPEN_IMAGE), 0);
        pendingPinIntent = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.PIN_IMAGE), 0);
        pendingPreviousIntent = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.PREVIOUS_IMAGE), 0);
        pendingShareIntent = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.SHARE_IMAGE), 0);

        pendingAppIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
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
        if (AppSettings.useTimer() && AppSettings.getTimerDuration() > 0 && PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(LiveWallpaperService.DOWNLOAD_WALLPAPER), PendingIntent.FLAG_NO_CREATE) != null) {

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, AppSettings.getTimerHour());
            calendar.set(Calendar.MINUTE, AppSettings.getTimerMinute());

            alarmManager.cancel(pendingDownloadIntent);

            if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                alarmManager.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AppSettings.getTimerDuration(), pendingDownloadIntent);
            }
            else {
                alarmManager.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY, AppSettings.getTimerDuration(), pendingDownloadIntent);
            }
        }
        if (AppSettings.useInterval() && AppSettings.getIntervalDuration() > 0 && PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(LiveWallpaperService.UPDATE_WALLPAPER), PendingIntent.FLAG_NO_CREATE) != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AppSettings.getIntervalDuration(), AppSettings.getIntervalDuration(), pendingIntervalIntent);
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
                Toast.makeText(LiveWallpaperService.this, "Error pushing notification", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @SuppressLint("NewApi")
    private void notifyChangeImage() {

        if (normalView != null && bigView != null && notificationManager != null) {
            int drawable = AppSettings.getNotificationIcon();

            if (AppSettings.getNotificationTitle().equals("Location") && Downloader.getBitmapLocation() != null) {
                normalView.setTextViewText(R.id.notification_title, Downloader.getBitmapLocation());
                normalView.setOnClickPendingIntent(R.id.notification_title, pendingToastIntent);
                if (Build.VERSION.SDK_INT >= 16) {
                    bigView.setTextViewText(R.id.notification_big_title, Downloader.getBitmapLocation());
                    bigView.setOnClickPendingIntent(R.id.notification_big_title, pendingToastIntent);
                }
                else {
                    notificationBuilder.setContentTitle(Downloader.getBitmapLocation());
                }
            }
            else {
                normalView.setOnClickPendingIntent(R.id.notification_title, null);
                bigView.setOnClickPendingIntent(R.id.notification_big_title, null);
            }

            if (AppSettings.getNotificationSummary().equals("Location") && Downloader.getBitmapLocation() != null) {
                normalView.setTextViewText(R.id.notification_summary, Downloader.getBitmapLocation());
                normalView.setOnClickPendingIntent(R.id.notification_summary, pendingToastIntent);
                if (Build.VERSION.SDK_INT >= 16) {
                    bigView.setTextViewText(R.id.notification_big_summary, Downloader.getBitmapLocation());
                    bigView.setOnClickPendingIntent(R.id.notification_big_summary, pendingToastIntent);
                }
                else {
                    notificationBuilder.setContentText(Downloader.getBitmapLocation());
                }
            }
            else {
                normalView.setOnClickPendingIntent(R.id.notification_summary, null);
                bigView.setOnClickPendingIntent(R.id.notification_big_summary, null);
            }

            if (AppSettings.useNotificationIconFile() && AppSettings.getNotificationIconFile() != null) {

                File image = new File(AppSettings.getNotificationIconFile());

                if (image.exists() && image.isFile()) {
                    Picasso.with(LiveWallpaperService.this).load(image).resizeDimen(android.R.dimen.notification_large_icon_width, android.R.dimen.notification_large_icon_height).centerCrop().into(targetIcon);
                }

            }
            else if (drawable == R.drawable.ic_action_picture || drawable == R.drawable.ic_action_picture_white) {
                if (Downloader.getCurrentBitmapFile() == null) {
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
                                BitmapFactory.decodeFile(Downloader.getCurrentBitmapFile().getAbsolutePath(), options);

                                int bitWidth = options.outWidth;
                                int bitHeight = options.outHeight;
                                int minWidth = LiveWallpaperService.this.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
                                int minHeight = LiveWallpaperService.this.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
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
                            Bitmap bitmap = BitmapFactory.decodeFile(Downloader.getCurrentBitmapFile().getAbsolutePath(), options);
                            targetIcon.onBitmapLoaded(bitmap, null);
                        }
                        catch (OutOfMemoryError e) {
                            if (AppSettings.useToast()) {
                                Toast.makeText(LiveWallpaperService.this, "Out of memory error", Toast.LENGTH_SHORT).show();
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

                    Bitmap mutableBitmap = Bitmap.createBitmap(layers[0].getIntrinsicWidth(), layers[0].getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(mutableBitmap);
                    layerDrawable.setBounds(0, 0, layers[0].getIntrinsicWidth(), layers[0].getIntrinsicHeight());
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
            normalView.setInt(R.id.notification_container, "setBackgroundColor", AppSettings.getNotificationColor());
            normalView.setImageViewResource(R.id.notification_icon, R.drawable.app_icon);
            normalView.setTextViewText(R.id.notification_title, AppSettings.getNotificationTitle());
            normalView.setInt(R.id.notification_title, "setTextColor", AppSettings.getNotificationTitleColor());
            normalView.setTextViewText(R.id.notification_summary, AppSettings.getNotificationSummary());
            normalView.setInt(R.id.notification_summary, "setTextColor", AppSettings.getNotificationSummaryColor());

            Drawable coloredImageOne = LiveWallpaperService.this.getResources().getDrawable(AppSettings.getNotificationOptionDrawable(0));
            Drawable coloredImageTwo = LiveWallpaperService.this.getResources().getDrawable(AppSettings.getNotificationOptionDrawable(1));
            Drawable coloredImageThree = LiveWallpaperService.this.getResources().getDrawable(AppSettings.getNotificationOptionDrawable(2));

            coloredImageOne.mutate().setColorFilter(AppSettings.getNotificationOptionColor(0), PorterDuff.Mode.MULTIPLY);
            coloredImageTwo.mutate().setColorFilter(AppSettings.getNotificationOptionColor(1), PorterDuff.Mode.MULTIPLY);
            coloredImageThree.mutate().setColorFilter(AppSettings.getNotificationOptionColor(2), PorterDuff.Mode.MULTIPLY);

            Bitmap mutableBitmapOne = Bitmap.createBitmap(coloredImageOne.getIntrinsicWidth(), coloredImageOne.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvasOne = new Canvas(mutableBitmapOne);
            coloredImageOne.setBounds(0, 0, coloredImageOne.getIntrinsicWidth(), coloredImageOne.getIntrinsicHeight());
            coloredImageOne.draw(canvasOne);

            Bitmap mutableBitmapTwo = Bitmap.createBitmap(coloredImageTwo.getIntrinsicWidth(), coloredImageTwo.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvasTwo = new Canvas(mutableBitmapTwo);
            coloredImageTwo.setBounds(0, 0, coloredImageTwo.getIntrinsicWidth(), coloredImageTwo.getIntrinsicHeight());
            coloredImageTwo.draw(canvasTwo);

            Bitmap mutableBitmapThree = Bitmap.createBitmap(coloredImageThree.getIntrinsicWidth(), coloredImageThree.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvasThree = new Canvas(mutableBitmapThree);
            coloredImageThree.setBounds(0, 0, coloredImageThree.getIntrinsicWidth(), coloredImageThree.getIntrinsicHeight());
            coloredImageThree.draw(canvasThree);

            if (AppSettings.useNotificationGame() && setupGameTiles()) {
                bigView = new RemoteViews(getPackageName(), R.layout.notification_game);
                tileIds = new int[]{
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
            bigView.setInt(R.id.notification_big_container, "setBackgroundColor", AppSettings.getNotificationColor());
            bigView.setImageViewResource(R.id.notification_big_icon, R.drawable.app_icon);
            bigView.setTextViewText(R.id.notification_big_title, AppSettings.getNotificationTitle());
            bigView.setInt(R.id.notification_big_title, "setTextColor", AppSettings.getNotificationTitleColor());
            bigView.setTextViewText(R.id.notification_big_summary, AppSettings.getNotificationSummary());
            bigView.setInt(R.id.notification_big_summary, "setTextColor", AppSettings.getNotificationSummaryColor());

            bigView.setImageViewBitmap(R.id.notification_button_one_image, mutableBitmapOne);
            bigView.setImageViewBitmap(R.id.notification_button_two_image, mutableBitmapTwo);
            bigView.setImageViewBitmap(R.id.notification_button_three_image, mutableBitmapThree);
            bigView.setTextViewText(R.id.notification_button_one_text, AppSettings.getNotificationOptionTitle(0));
            bigView.setInt(R.id.notification_button_one_text, "setTextColor", AppSettings.getNotificationOptionColor(0));
            bigView.setTextViewText(R.id.notification_button_two_text, AppSettings.getNotificationOptionTitle(1));
            bigView.setInt(R.id.notification_button_two_text, "setTextColor", AppSettings.getNotificationOptionColor(1));
            bigView.setTextViewText(R.id.notification_button_three_text, AppSettings.getNotificationOptionTitle(2));
            bigView.setInt(R.id.notification_button_three_text, "setTextColor", AppSettings.getNotificationOptionColor(2));

            if (getIntentForNotification(AppSettings.getNotificationIconAction()) != null) {
                normalView.setOnClickPendingIntent(R.id.notification_icon, getIntentForNotification(AppSettings.getNotificationIconAction()));
                bigView.setOnClickPendingIntent(R.id.notification_big_icon, getIntentForNotification(AppSettings.getNotificationIconAction()));
            }
            else {
                normalView.setOnClickPendingIntent(R.id.notification_icon, pendingAppIntent);
                bigView.setOnClickPendingIntent(R.id.notification_big_icon, pendingAppIntent);
            }

            notificationBuilder = new Notification.Builder(this)
                    .setContent(normalView)
                    .setSmallIcon(R.drawable.app_icon_grayscale)
                    .setOngoing(true);

            if (Build.VERSION.SDK_INT >= 16) {
                if (AppSettings.useNotificationGame()) {
                    notificationBuilder.setPriority(Notification.PRIORITY_MAX);
                }
                else {
                    notificationBuilder.setPriority(Notification.PRIORITY_MIN);
                }
                if (getIntentForNotification(AppSettings.getNotificationOptionTitle(0)) != null) {
                    bigView.setOnClickPendingIntent(R.id.notification_button_one, getIntentForNotification(AppSettings.getNotificationOptionTitle(0)));
                }
                if (getIntentForNotification(AppSettings.getNotificationOptionTitle(1)) != null) {
                    bigView.setOnClickPendingIntent(R.id.notification_button_two, getIntentForNotification(AppSettings.getNotificationOptionTitle(1)));
                }
                if (getIntentForNotification(AppSettings.getNotificationOptionTitle(2)) != null) {
                    bigView.setOnClickPendingIntent(R.id.notification_button_three, getIntentForNotification(AppSettings.getNotificationOptionTitle(2)));
                }
            }
            else {
                notificationBuilder.setContentTitle(AppSettings.getNotificationTitle());
                notificationBuilder.setContentText(AppSettings.getNotificationSummary());
                notificationBuilder.addAction(AppSettings.getNotificationOptionDrawable(0), AppSettings.getNotificationOptionTitle(0), getIntentForNotification(AppSettings.getNotificationOptionTitle(0)));
                notificationBuilder.addAction(AppSettings.getNotificationOptionDrawable(1), AppSettings.getNotificationOptionTitle(1), getIntentForNotification(AppSettings.getNotificationOptionTitle(1)));
                notificationBuilder.addAction(AppSettings.getNotificationOptionDrawable(2), AppSettings.getNotificationOptionTitle(2), getIntentForNotification(AppSettings.getNotificationOptionTitle(2)));
            }

            pushNotification();

            if (Downloader.getCurrentBitmapFile() != null) {
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
                    setTileImage(tile, R.drawable.ic_action_picture_white);
                    setTileImage(lastTile, R.drawable.ic_action_picture_white);
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
        bitmapFiles.addAll(Downloader.getBitmapList());

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
                            BitmapFactory.decodeFile(files.get(tilesLoaded).getAbsolutePath(), options);

                            int bitWidth = options.outWidth;
                            int bitHeight = options.outHeight;
                            int minWidth = LiveWallpaperService.this.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
                            int minHeight = LiveWallpaperService.this.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
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
                        Bitmap bitmap = BitmapFactory.decodeFile(files.get(tilesLoaded).getAbsolutePath(), options);
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
                            Toast.makeText(LiveWallpaperService.this, "Out of memory error", Toast.LENGTH_SHORT).show();
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
                setTileImage(i, R.drawable.ic_action_picture_white);
            }

            pushNotification();
        }
    }

    private void createGameIntents() {
        pendingTile0 = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.GAME_TILE0), 0);
        pendingTile1 = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.GAME_TILE1), 0);
        pendingTile2 = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.GAME_TILE2), 0);
        pendingTile3 = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.GAME_TILE3), 0);
        pendingTile4 = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.GAME_TILE4), 0);
        pendingTile5 = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.GAME_TILE5), 0);
        pendingTile6 = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.GAME_TILE6), 0);
        pendingTile7 = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.GAME_TILE7), 0);
        pendingTile8 = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.GAME_TILE8), 0);
        pendingTile9 = PendingIntent.getBroadcast(this, 0, new Intent(LiveWallpaperService.GAME_TILE9), 0);

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
                        Downloader.download(LiveWallpaperService.this);
                        unregisterReceiver(networkReceiver);
                    }
                    else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && AppSettings.useMobile()) {
                        Downloader.download(LiveWallpaperService.this);
                        unregisterReceiver(networkReceiver);
                    }

                }
            }
        };

        private MyGLRenderer renderer;
        private int[] maxTextureSize = new int[]{0};
        private boolean toChange = false;
        private boolean animated = false;
        private int touchCount = 0;
        private GestureDetector gestureDetector;
        private ScaleGestureDetector scaleGestureDetector;
        private float scaleFactor = 1.f;
        private List<File> previousBitmaps = new ArrayList<>();
        private long pinReleaseTime;
        private boolean downloadOnConnection = false;
        private boolean isPlayingMusic = false;
        private KeyguardManager keyguardManager;

        public GLWallpaperEngine() {
            super();

            gestureDetector = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {

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
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                                        float distanceY) {
                    if (AppSettings.useDrag() && touchCount == 2) {
                        renderer.onSwipe(distanceX, distanceY);
                        render();
                        return true;
                    }
                    return super.onScroll(e1, e2, distanceX, distanceY);
                }


            });

            scaleGestureDetector = new ScaleGestureDetector(LiveWallpaperService.this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    scaleFactor *= detector.getScaleFactor();

                    float minScaleFactor;

                    if (AppSettings.extendScale() || !AppSettings.fillImages()) {
                        minScaleFactor = renderer.renderScreenWidth < renderer.renderScreenHeight
                                ? renderer.renderScreenWidth / renderer.bitmapWidth
                                : renderer.renderScreenHeight / renderer.bitmapHeight;
                    }
                    else {
                        minScaleFactor = renderer.renderScreenWidth > renderer.renderScreenHeight
                                ? renderer.renderScreenWidth / renderer.bitmapWidth
                                : renderer.renderScreenHeight / renderer.bitmapHeight;
                    }

                    scaleFactor = Math.max(minScaleFactor, Math.min(scaleFactor, 5.0f));

                    render();
                    return true;
                }
            });

            pinReleaseTime = System.currentTimeMillis();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setEGLContextClientVersion(2);

            renderer = new MyGLRenderer();
            setRenderer(renderer);
            setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            keyguardManager = (KeyguardManager) LiveWallpaperService.this.getSystemService(Context.KEYGUARD_SERVICE);

            if (!isPreview()) {
                registerReceiver(updateReceiver, getEngineIntentFilter());

                if (Build.VERSION.SDK_INT >= 19 && AppSettings.showAlbumArt()) {
                    Intent musicReceiverIntent = new Intent(LiveWallpaperService.this, MusicReceiverService.class);
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
            String[] projection = new String[]{
                    MediaStore.Audio.Albums._ID,
                    MediaStore.Audio.Albums.ARTIST,
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.ALBUM_ART};

            Cursor cursor = getApplicationContext().getContentResolver().query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Audio.Albums.ALBUM + " ='" + album.replaceAll("'", "''") + "'"
                            + " AND "
                            + MediaStore.Audio.Albums.ARTIST + " ='" + artist.replaceAll("'", "''") + "'",
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
            projection = new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM};

            cursor = getApplicationContext().getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Audio.Albums.ALBUM + " ='" + album.replaceAll("'", "''") + "'"
                            + " AND "
                            + MediaStore.Audio.Albums.ARTIST + " ='" + artist.replaceAll("'", "''") + "'",
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

            Toast.makeText(LiveWallpaperService.this, "Final path: " + finalPath, Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (AppSettings.useDoubleTap() || AppSettings.useDrag()) {
                touchCount = event.getPointerCount();
                gestureDetector.onTouchEvent(event);
                if (AppSettings.useScale()) {
                    scaleGestureDetector.onTouchEvent(event);
                }
            }
        }

        private void getNewImages() {
            ConnectivityManager connect = (ConnectivityManager) LiveWallpaperService.this.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo wifi = connect.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = connect.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (wifi != null && wifi.isConnected() && AppSettings.useWifi()) {
                Downloader.download(getApplicationContext());
                if (downloadOnConnection) {
                    unregisterReceiver(networkReceiver);
                }
            }
            else if (mobile != null && mobile.isConnected() && AppSettings.useMobile()) {
                Downloader.download(getApplicationContext());
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
                if (downloadOnConnection) {
                    unregisterReceiver(networkReceiver);
                }
                if (Build.VERSION.SDK_INT >= 19) {
                    Intent musicReceiverIntent = new Intent(LiveWallpaperService.this, MusicReceiverService.class);
                    stopService(musicReceiverIntent);
                }
            }
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(final boolean visible) {
            if (visible) {
                super.resume();
                animated = AppSettings.useAnimation() || AppSettings.useVerticalAnimation();
                renderer.animationModifierX = AppSettings.getAnimationSpeed();
                renderer.animationModifierY = AppSettings.getVerticalAnimationSpeed();
                renderer.targetFrameTime = 1000 / AppSettings.getAnimationFrameRate();

                resetRenderMode();

                if (!keyguardManager.inKeyguardRestrictedInputMode() || AppSettings.changeWhenLocked()) {
                    if (toChange) {
                        loadNextImage();
                        toChange = false;
                    }
                    else if (AppSettings.changeOnReturn()) {
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
            resetRenderMode();

            if (Downloader.getMusicBitmap() == null) {
                return;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bitmap bitmap = Downloader.getMusicBitmap();

                        if (bitmap != null) {

                            addEvent(new Runnable() {

                                @Override
                                public void run() {
                                    renderer.setBitmap(bitmap);
                                }
                            });

                            if (AppSettings.useNotification()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyChangeImage();
                                    }
                                });
                            }
                        }
                    }
                    catch (OutOfMemoryError e) {
                        if (AppSettings.useToast()) {
                            Toast.makeText(LiveWallpaperService.this, "Out of memory error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }).start();
        }

        public void loadCurrentImage() {
            resetRenderMode();

            if (Downloader.getCurrentBitmapFile() == null) {
                loadNextImage();
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

                        System.gc();
                        try {

                            Bitmap checkBitmap;
                            File imageFile = Downloader.getCurrentBitmapFile();
                            checkBitmap = imageFile.exists() ? BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options) : Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);

                            final Bitmap bitmap = checkBitmap;

                            if (bitmap != null) {

                                addEvent(new Runnable() {

                                    @Override
                                    public void run() {
                                        renderer.setBitmap(bitmap);
                                    }
                                });

                                if (AppSettings.useNotification()) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            notifyChangeImage();
                                        }
                                    });
                                }
                            }
                        }
                        catch (NullPointerException e) {
                        }
                    }
                    catch (OutOfMemoryError e) {
                        if (AppSettings.useToast()) {
                            Toast.makeText(LiveWallpaperService.this, "Out of memory error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }).start();
        }        private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {

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
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, cycleIntent, 0);
                            alarmManager.cancel(pendingIntent);
                            alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AppSettings.getIntervalDuration(), AppSettings.getIntervalDuration(), pendingIntent);
                        }
                        if (isVisible()) {
                            loadNextImage();
                        }
                        else {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Downloader.getNextImage();
                                    renderer.loadCurrent = true;

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
                        Downloader.deleteCurrentBitmap();
                        closeNotificationDrawer(context);
                        if (AppSettings.useToast()) {
                            Toast.makeText(LiveWallpaperService.this, "Deleted image", Toast.LENGTH_LONG).show();
                        }
                        loadNextImage();
                        break;
                    case LiveWallpaperService.OPEN_IMAGE:
                        String location = Downloader.getBitmapLocation();
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
                            galleryIntent.setDataAndType(Uri.fromFile(Downloader.getCurrentBitmapFile()), "image/*");
                            galleryIntent = Intent.createChooser(galleryIntent, "Open Image");
                            galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(galleryIntent);
                            closeNotificationDrawer(context);
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
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(Downloader.getCurrentBitmapFile()));
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
                        renderer.loadCurrent = false;
                        if (isVisible()) {
                            loadMusicBitmap();
                        }
                        else {
                            isPlayingMusic = true;
                        }
                        break;
                    case LiveWallpaperService.CURRENT_IMAGE:
                        if (isVisible()) {
                            loadCurrentImage();
                        }
                        else {
                            renderer.loadCurrent = true;
                        }
                        break;
                }

            }
        };

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
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        if (!AppSettings.useHighQuality()) {
                            options.inPreferredConfig = Bitmap.Config.RGB_565;
                        }

                        System.gc();
                        try {

                            Bitmap checkBitmap;
                            File imageFile = previousBitmaps.get(0);
                            checkBitmap = imageFile.exists() ? BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options) : Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);

                            final Bitmap bitmap = checkBitmap;

                            Downloader.setCurrentBitmapFile(previousBitmaps.get(0));

                            if (bitmap != null) {

                                addEvent(new Runnable() {

                                    @Override
                                    public void run() {
                                        renderer.setBitmap(bitmap);
                                    }
                                });

                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        notifyChangeImage();
                                    }
                                });
                                if (!AppSettings.shuffleImages()) {
                                    Downloader.decreaseIndex();
                                }

                                previousBitmaps.remove(0);
                            }
                        }
                        catch (NullPointerException e) {
                        }
                    }
                    catch (OutOfMemoryError e) {
                        if (AppSettings.useToast()) {
                            Toast.makeText(LiveWallpaperService.this, "Out of memory error", Toast.LENGTH_SHORT).show();
                        }
                    }
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

            previousBitmaps.add(0, Downloader.getCurrentBitmapFile());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        if (!AppSettings.useHighQuality()) {
                            options.inPreferredConfig = Bitmap.Config.RGB_565;
                        }

                        System.gc();
                        try {

                            Bitmap checkBitmap;
                            File imageFile = Downloader.getNextImage();
                            checkBitmap = imageFile.exists() ? BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options) : Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);

                            final Bitmap bitmap = checkBitmap;

                            if (bitmap != null) {

                                addEvent(new Runnable() {

                                    @Override
                                    public void run() {
                                        renderer.setBitmap(bitmap);
                                    }
                                });

                                if (AppSettings.useNotification()) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            notifyChangeImage();
                                        }
                                    });
                                }

                                if (previousBitmaps.size() > AppSettings.getHistorySize()) {
                                    previousBitmaps.remove(previousBitmaps.size() - 1);
                                }
                            }
                        }
                        catch (NullPointerException e) {
                        }
                    }
                    catch (OutOfMemoryError e) {
                        if (AppSettings.useToast()) {
                            Toast.makeText(LiveWallpaperService.this, "Out of memory error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }).start();

        }

        private void resetRenderMode() {
            if (animated) {
                setRendererMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            }
            else {
                setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            }
        }

        private void toastEffect(final String effectName, final String effectValue) {
            if (AppSettings.useToast() && AppSettings.useToastEffects()) {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(LiveWallpaperService.this, "Effect applied: " + effectName + " " + effectValue, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        class MyGLRenderer implements GLSurfaceView.Renderer {

            private static final String TAG = "Renderer";
            public float vertices[];
            public short indices[];
            public float uvs[];
            public FloatBuffer vertexBuffer;
            public ShortBuffer drawListBuffer;
            public FloatBuffer uvBuffer;
            private float[] matrixProjection = new float[16];
            private float[] matrixView = new float[16];
            private float[] matrixProjectionAndView = new float[16];
            private float[] transMatrix = new float[16];
            private int program;
            private float renderScreenWidth = 1;
            private float renderScreenHeight = 1;
            private long startTime;
            private long endTime;
            private long frameTime;
            private long targetFrameTime;

            private float bitmapWidth = 0;
            private float bitmapHeight = 0;
            private float oldBitmapWidth = 0;
            private float oldBitmapHeight = 0;
            private float offsetX = 0f;
            private float offsetY = 0f;
            private float newOffsetX = 0f;
            private float newOffsetY = 0f;
            private float rawOffsetX = 0f;

            private float animationModifierX = 0.0f;
            private float animationModifierY = 0.0f;
            private float animationX = 0.0f;
            private float animationY = 0.0f;

            private float fadeInAlpha = 0.0f;
            private float fadeOutAlpha = 1.0f;
            private boolean useTransition = false;
            private long transitionTime;
            private int[] textureNames = new int[3];
            private boolean firstRun = true;
            private boolean loadCurrent = false;
            private boolean toEffect = false;
            private boolean contextInitialized = false;
            private EffectContext effectContext;
            private EffectFactory effectFactory;

            private OvershootInterpolator horizontalOvershootInterpolator;
            private OvershootInterpolator verticalOvershootInterpolator;
            private AccelerateInterpolator accelerateInterpolator;
            private DecelerateInterpolator decelerateInterpolator;
            private EffectUpdateListener effectUpdateListener = new EffectUpdateListener() {
                @Override
                public void onEffectUpdated(Effect effect, Object info) {

                    Log.i(TAG, "Effect info: " + info.toString());

                    if (AppSettings.useToast()) {
                        Toast.makeText(LiveWallpaperService.this, "Effect info: " + info.toString(), Toast.LENGTH_SHORT).show();
                    }

                }
            };

            public MyGLRenderer() {
                startTime = System.currentTimeMillis();
                horizontalOvershootInterpolator = new OvershootInterpolator();
                verticalOvershootInterpolator = new OvershootInterpolator();
                accelerateInterpolator = new AccelerateInterpolator();
                decelerateInterpolator = new DecelerateInterpolator();
            }

            @Override
            public void onDrawFrame(GL10 gl) {

                if (!contextInitialized) {
                    effectContext = EffectContext.createWithCurrentGlContext();
                    contextInitialized = true;
                }

                if (toEffect && effectContext != null) {
                    toEffect = false;

                    try {
                        initEffects(0);
                    }
                    catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                    GLES20.glDeleteTextures(1, textureNames, 2);
                    Log.i(TAG, "Deleted texture: " + textureNames[2]);

                    setupContainer(bitmapWidth, bitmapHeight);

                }

                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                if (useTransition) {
                    applyTransition();
                }
                else {

                    try {
                        endTime = System.currentTimeMillis();
                        frameTime = endTime - startTime;
                        if (frameTime < targetFrameTime) {
                            Thread.sleep(targetFrameTime - frameTime);
                        }
                        startTime = System.currentTimeMillis();
                    }
                    catch (InterruptedException e) {
                    }

                    if (animated) {
                        float safety = AppSettings.getAnimationSafety();
                        if (AppSettings.useAnimation() && bitmapWidth - (renderScreenWidth / scaleFactor) > safety) {
                            float animationFactor = 1;

//                            if (animationX < -bitmapWidth + renderScreenWidth + AppSettings.getAnimationSafety()) {
//                                if (animationModifierX < 0) {
//                                    animationFactor = accelerateInterpolator.getInterpolation((bitmapWidth - renderScreenWidth + animationX) / safety);
//                                }
//                                else {
//                                    animationFactor = accelerateInterpolator.getInterpolation((safety - bitmapWidth - renderScreenWidth + animationX) / safety);
//                                }
//
//                                animationFactor = animationFactor < 0.1f ? 0.1f : animationFactor;
//                            }

                            animationX += animationFactor * ((AppSettings.scaleAnimationSpeed()) ? (animationModifierX / scaleFactor) : animationModifierX);
                            offsetX = animationX;
                            newOffsetX -= animationX;
                        }

                        if (AppSettings.useVerticalAnimation() && bitmapHeight - (renderScreenHeight / scaleFactor) > AppSettings.getAnimationSafety()) {
                            animationY += ((AppSettings.scaleAnimationSpeed()) ? (animationModifierY / scaleFactor) : animationModifierY);
                            offsetY = animationY;
                            newOffsetY -= animationY;
                        }

                    }

                    calculateBounds();

                    android.opengl.Matrix.orthoM(matrixProjection, 0, 0, renderScreenWidth / scaleFactor, 0, renderScreenHeight / scaleFactor, 0, 10f);

                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
                    android.opengl.Matrix.setIdentityM(transMatrix, 0);
                    android.opengl.Matrix.translateM(transMatrix, 0, offsetX, offsetY, 0f);
                    renderImage();
                }
            }

            private void applyTransition() {

                long time = System.currentTimeMillis();

                if (time > transitionTime) {
                    useTransition = false;
                    fadeInAlpha = 0.0f;
                    fadeOutAlpha = 1.0f;
                    offsetX = newOffsetX;
                    offsetY = newOffsetY;
                    animationX = offsetX;
                    animationY = offsetY;
                    oldBitmapHeight = bitmapHeight;
                    oldBitmapWidth = bitmapWidth;
                    scaleFactor = 1.0f;

                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                    android.opengl.Matrix.orthoM(matrixProjection, 0, 0, renderScreenWidth / scaleFactor, 0, renderScreenHeight / scaleFactor, 0, 10f);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
                    android.opengl.Matrix.setIdentityM(transMatrix, 0);
                    android.opengl.Matrix.translateM(transMatrix, 0, offsetX, offsetY, 0f);
                    renderImage();

                    resetRenderMode();
                }
                else {

                    if (animated) {
                        if (AppSettings.useAnimation() && bitmapWidth - (renderScreenWidth / scaleFactor) > AppSettings.getAnimationSafety()) {
                            animationX += ((AppSettings.scaleAnimationSpeed()) ? (animationModifierX / scaleFactor) : animationModifierX);
                            offsetX = animationX;
                            if (newOffsetX < 0 && newOffsetX > (-bitmapWidth + renderScreenWidth)) {
                                newOffsetX -= ((AppSettings.scaleAnimationSpeed()) ? (animationModifierX / scaleFactor) : animationModifierX);
                            }
                        }

                        if (AppSettings.useVerticalAnimation() && bitmapHeight - (renderScreenHeight / scaleFactor) > AppSettings.getAnimationSafety()) {
                            animationY += ((AppSettings.scaleAnimationSpeed()) ? (animationModifierY / scaleFactor) : animationModifierY);
                            offsetY = animationY;
                            if (newOffsetY < 0 && newOffsetY > (-bitmapHeight + renderScreenHeight)) {
                                newOffsetY -= ((AppSettings.scaleAnimationSpeed()) ? (animationModifierY / scaleFactor) : animationModifierY);
                            }
                        }
                    }

                    calculateBounds();

                    float timeRatio = (float) (transitionTime - time) / AppSettings.getTransitionTime();
                    float transitionNewScaleFactor = 1.0f;
                    float transitionOldScaleFactor = scaleFactor;
                    float transitionOldOffsetX = offsetX;
                    float transitionNewOffsetX = newOffsetX;
                    float transitionOldOffsetY = offsetY;
                    float transitionNewOffsetY = newOffsetY;
                    float transitionOldAngle = 0f;
                    float transitionNewAngle = 0f;

                    if (AppSettings.useZoomIn()) {
                        transitionNewScaleFactor = 1.0f - timeRatio;
                        transitionNewOffsetX = bitmapWidth / transitionNewScaleFactor / 2 * timeRatio - ((bitmapWidth / transitionNewScaleFactor - renderScreenWidth / transitionNewScaleFactor) / 2.0f) - (bitmapWidth - renderScreenWidth) / transitionNewScaleFactor * (newOffsetX / (renderScreenWidth - bitmapWidth) - 0.5f);
                        transitionNewOffsetY = bitmapHeight / transitionNewScaleFactor / 2 * timeRatio - ((bitmapHeight / transitionNewScaleFactor - renderScreenHeight / transitionNewScaleFactor) / 2);
                    }


                    if (AppSettings.useZoomOut()) {
                        transitionOldScaleFactor = timeRatio;
                        transitionOldOffsetX = oldBitmapWidth / transitionOldScaleFactor / 2 * (1.0f - timeRatio) - ((oldBitmapWidth / transitionOldScaleFactor - renderScreenWidth / transitionOldScaleFactor) / 2) - (oldBitmapWidth - renderScreenWidth) / transitionOldScaleFactor * (offsetX / (renderScreenWidth - oldBitmapWidth) - 0.5f);
                        transitionOldOffsetY = oldBitmapHeight / transitionOldScaleFactor / 2 * (1.0f - timeRatio) - ((oldBitmapHeight / transitionOldScaleFactor - renderScreenHeight / transitionOldScaleFactor) / 2);
                    }

                    if (AppSettings.useOvershoot()) {
                        horizontalOvershootInterpolator = new OvershootInterpolator(AppSettings.getOvershootIntensity());
                        transitionNewOffsetX = (AppSettings.reverseOvershoot() ?
                                transitionNewOffsetX + renderScreenWidth - (renderScreenWidth * horizontalOvershootInterpolator.getInterpolation(1.0f - timeRatio)) :
                                transitionNewOffsetX - renderScreenWidth + (renderScreenWidth * horizontalOvershootInterpolator.getInterpolation(1.0f - timeRatio)));
                    }

                    if (AppSettings.useVerticalOvershoot()) {
                        verticalOvershootInterpolator = new OvershootInterpolator(AppSettings.getVerticalOvershootIntensity());
                        transitionNewOffsetY = (AppSettings.reverseVerticalOvershoot() ?
                                transitionNewOffsetY + renderScreenHeight - (renderScreenHeight * verticalOvershootInterpolator.getInterpolation(1.0f - timeRatio)) :
                                transitionNewOffsetY - renderScreenHeight + (renderScreenHeight * verticalOvershootInterpolator.getInterpolation(1.0f - timeRatio)));

                    }

                    if (AppSettings.useSpinIn()) {
                        transitionNewAngle = AppSettings.reverseSpinIn()
                                ? AppSettings.getSpinInAngle() * -timeRatio
                                : AppSettings.getSpinInAngle() * timeRatio;
                    }

                    if (AppSettings.useSpinOut()) {
                        transitionOldAngle = AppSettings.reverseSpinOut()
                                ? AppSettings.getSpinOutAngle() * -(1.0f - timeRatio)
                                : AppSettings.getSpinOutAngle() * -(1.0f - timeRatio);
                    }

                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                    int mAlphaHandle = GLES20.glGetUniformLocation(program, "opacity");

                    GLES20.glEnable(GLES20.GL_BLEND);
                    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                    GLES20.glUniform1f(mAlphaHandle, 1.0f);

                    if (AppSettings.useFade()) {
                        fadeOutAlpha = timeRatio;
                        GLES20.glUniform1f(mAlphaHandle, fadeOutAlpha);
                    }

                    renderTransitionTexture(oldBitmapWidth, oldBitmapHeight, 1, transitionOldOffsetX, transitionOldOffsetY, transitionOldAngle, transitionOldScaleFactor);

                    if (AppSettings.useFade()) {
                        fadeInAlpha = 1.0f - timeRatio;
                        GLES20.glUniform1f(mAlphaHandle, fadeInAlpha);
                    }

                    renderTransitionTexture(bitmapWidth, bitmapHeight, 0, transitionNewOffsetX, transitionNewOffsetY, transitionNewAngle, transitionNewScaleFactor);
                    GLES20.glDisable(GLES20.GL_BLEND);
                }
            }

            private void calculateBounds() {

                if (bitmapWidth * scaleFactor >= renderScreenWidth) {
                    if (offsetX < (-bitmapWidth + renderScreenWidth / scaleFactor)) {
                        animationModifierX = Math.abs(animationModifierX);
                        offsetX = -bitmapWidth + renderScreenWidth / scaleFactor;
                        animationX = offsetX;

                    }
                    else if (offsetX > 0f) {
                        animationModifierX = -Math.abs(animationModifierX);
                        offsetX = 0f;
                        animationX = offsetX;
                    }
                }

                if (bitmapHeight * scaleFactor >= renderScreenHeight) {
                    if (offsetY < (-bitmapHeight + renderScreenHeight / scaleFactor)) {
                        animationModifierY = Math.abs(animationModifierY);
                        offsetY = -bitmapHeight + renderScreenHeight / scaleFactor;
                        animationY = offsetY;
                    }
                    else if (offsetY > 0f) {
                        animationModifierY = -Math.abs(animationModifierY);
                        offsetY = 0f;
                        animationY = offsetY;
                    }
                }
            }

            private void renderTransitionTexture(float containerWidth, float containerHeight,
                                                 int texture, float x, float y, float angle,
                                                 float scale) {

                setupContainer(containerWidth, containerHeight);
                android.opengl.Matrix.orthoM(matrixProjection, 0, 0, renderScreenWidth / scale, 0, renderScreenHeight / scale, 0, 10f);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[texture]);
                android.opengl.Matrix.setIdentityM(transMatrix, 0);
                android.opengl.Matrix.translateM(transMatrix, 0, renderScreenWidth / scale / 2, renderScreenHeight / scale / 2, 0);
                android.opengl.Matrix.rotateM(transMatrix, 0, angle, 0.0f, 0.0f, 1.0f);
                android.opengl.Matrix.translateM(transMatrix, 0, -renderScreenWidth / scale / 2, -renderScreenHeight / scale / 2, 0);
                android.opengl.Matrix.translateM(transMatrix, 0, x, y, 0);

                renderImage();
            }

            private void renderImage() {

                // get handle to vertex shader's vPosition member
                int mPositionHandle = GLES20.glGetAttribLocation(program, "vPosition");

                // Enable generic vertex attribute array
                GLES20.glEnableVertexAttribArray(mPositionHandle);

                // Prepare the triangle coordinate data
                GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

                // Get handle to texture coordinates location
                int mTexCoordLoc = GLES20.glGetAttribLocation(program, "a_texCoord");

                // Enable generic vertex attribute array
                GLES20.glEnableVertexAttribArray(mTexCoordLoc);

                // Prepare the texturecoordinates
                GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

                // Get handle to shape's transformation matrix
                int mtrxhandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

                android.opengl.Matrix.multiplyMM(matrixProjectionAndView, 0, matrixView, 0, transMatrix, 0);
                android.opengl.Matrix.multiplyMM(matrixProjectionAndView, 0, matrixProjection, 0, transMatrix, 0);

                // Apply the projection and view transformation
                GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, matrixProjectionAndView, 0);

                // Draw the container
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

                // Disable vertex array
                GLES20.glDisableVertexAttribArray(mPositionHandle);
                GLES20.glDisableVertexAttribArray(mTexCoordLoc);
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {

                Log.i(TAG, "Renderer onSurfaceChanged");

                if (AppSettings.preserveContext()) {
                    setPreserveEGLContextOnPause(true);
                }
                else {
                    setPreserveEGLContextOnPause(false);
                    contextInitialized = false;
                    loadCurrent = true;
                }

                if (bitmapHeight == 0) {
                    loadNextImage();
                }

                if (width != renderScreenWidth) {
                    GLES20.glViewport(0, 0, width, height);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                    loadCurrentImage();
                }

                if (loadCurrent) {
                    loadCurrentImage();
                    loadCurrent = false;
                }

                if (isPlayingMusic) {
                    loadMusicBitmap();
                    isPlayingMusic = false;
                }

                renderScreenWidth = width;
                renderScreenHeight = height;

                for (int i = 0; i < 16; i++) {
                    matrixProjection[i] = 0.0f;
                    matrixView[i] = 0.0f;
                    matrixProjectionAndView[i] = 0.0f;
                }

                android.opengl.Matrix.orthoM(matrixProjection, 0, 0f, renderScreenWidth / scaleFactor, 0.0f, renderScreenHeight / scaleFactor, 0, 10f);

                // Set the camera position (View matrix)
                android.opengl.Matrix.setLookAtM(matrixView, 0, 0f, 0f, 1f, 0f, 0f, 0.0f, 0f, 1f, 0.0f);

                // Calculate the projection and view transformation
                android.opengl.Matrix.multiplyMM(matrixProjectionAndView, 0, matrixProjection, 0, matrixView, 0);

            }

            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {

                int vertexShader = GLShaders.loadShader(GLES20.GL_VERTEX_SHADER, GLShaders.vertexShaderImage);
                int fragmentShader = GLShaders.loadShader(GLES20.GL_FRAGMENT_SHADER, GLShaders.fragmentShaderImage);

                program = GLES20.glCreateProgram();
                GLES20.glAttachShader(program, vertexShader);
                GLES20.glAttachShader(program, fragmentShader);
                GLES20.glLinkProgram(program);
                GLES20.glUseProgram(program);

                if (firstRun) {
                    GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);

                    Log.i(TAG, "First run");
                    Log.i(TAG, "Max texture size: " + maxTextureSize[0]);

                    GLES20.glGenTextures(3, textureNames, 0);

                    uvs = new float[]{
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f
                    };

                    ByteBuffer bb = ByteBuffer.allocateDirect(uvs.length * 4);
                    bb.order(ByteOrder.nativeOrder());
                    uvBuffer = bb.asFloatBuffer();
                    uvBuffer.put(uvs);
                    uvBuffer.position(0);
                    firstRun = false;

                    setupContainer(bitmapWidth, bitmapHeight);
                }

                GLES20.glUseProgram(program);
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                Log.i(TAG, "onSurfaceCreated");
            }

            public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep,
                                         int xPixels, int yPixels) {
                if (AppSettings.forceParallax()) {
                    if (AppSettings.useDrag() || animated) {
                        float offsetDifference = (renderScreenWidth - oldBitmapWidth) * scaleFactor * (1.0f - xOffset - rawOffsetX);
                        offsetX += offsetDifference;
                        newOffsetX += offsetDifference;
                        animationX += offsetDifference;
                    }
                    else {
                        newOffsetX = (renderScreenWidth - bitmapWidth) * (1.0f - xOffset);
                        offsetX = (renderScreenWidth - oldBitmapWidth) * scaleFactor * (1.0f - xOffset);
                    }
                    rawOffsetX = 1.0f - xOffset;
                }
                else {
                    if (AppSettings.useDrag() || animated) {
                        float offsetDifference = (renderScreenWidth - oldBitmapWidth) * scaleFactor * (xOffset - rawOffsetX);
                        offsetX += offsetDifference;
                        newOffsetX += offsetDifference;
                        animationX += offsetDifference;
                    }
                    else {
                        newOffsetX = (renderScreenWidth - bitmapWidth) * (xOffset);
                        offsetX = (renderScreenWidth - oldBitmapWidth) * scaleFactor * (xOffset);
                    }
                    rawOffsetX = xOffset;
                }
            }

            public void onSwipe(float xMovement, float yMovement) {
                if (!useTransition) {
                    if (AppSettings.reverseDrag()) {
                        if (bitmapWidth * scaleFactor < renderScreenWidth
                                || offsetX + xMovement > (-bitmapWidth + renderScreenWidth / scaleFactor) && offsetX + xMovement < 0) {
                            animationX += xMovement;
                            offsetX += xMovement;
                            newOffsetX += xMovement;
                        }
                        if (bitmapHeight * scaleFactor < renderScreenHeight
                                || offsetY - yMovement > (-bitmapHeight + renderScreenHeight / scaleFactor) && offsetY - yMovement < 0) {
                            animationY -= yMovement;
                            offsetY -= yMovement;
                            newOffsetY -= yMovement;
                        }
                    }
                    else {
                        if (bitmapWidth * scaleFactor < renderScreenWidth
                                || offsetX - xMovement > (-bitmapWidth + renderScreenWidth / scaleFactor) && offsetX - xMovement < 0) {
                            animationX -= xMovement;
                            offsetX -= xMovement;
                            newOffsetX -= xMovement;
                        }
                        if (bitmapHeight * scaleFactor < renderScreenHeight
                                || offsetY + yMovement > (-bitmapHeight + renderScreenHeight / scaleFactor) && offsetY + yMovement < 0) {
                            animationY += yMovement;
                            offsetY += yMovement;
                            newOffsetY += yMovement;
                        }
                    }
                }
            }

            public void resetPosition() {
                scaleFactor = 1.0f;
                float resetOffsetX = (renderScreenWidth - bitmapWidth) * (rawOffsetX);
                offsetX = resetOffsetX;
                animationX = resetOffsetX;
                offsetY = 0;
                animationY = 0;
                render();
            }

            public void setupContainer(float width, float height) {

                vertices = new float[]{
                        0.0f, height, 0.0f,
                        0.0f, 0.0f, 0.0f,
                        width, 0.0f, 0.0f,
                        width, height, 0.0f
                };

                indices = new short[]{0, 1, 2, 0, 2, 3};

                // The vertex buffer.
                ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
                bb.order(ByteOrder.nativeOrder());
                vertexBuffer = bb.asFloatBuffer();
                vertexBuffer.put(vertices);
                vertexBuffer.position(0);

                // initialize byte buffer for the draw list
                ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
                dlb.order(ByteOrder.nativeOrder());
                drawListBuffer = dlb.asShortBuffer();
                drawListBuffer.put(indices);
                drawListBuffer.position(0);

            }

            public void setBitmap(Bitmap bitmap) {

                try {
                    Log.i(TAG, "currentBitmapFile loaded: " + Downloader.getCurrentBitmapFile().getName());

                    int storeId = textureNames[0];
                    textureNames[0] = textureNames[1];
                    textureNames[1] = storeId;

                    Log.i(TAG, "startWidth: " + bitmap.getWidth() + " startHeight: " + bitmap.getHeight());

                    if (AppSettings.useScale()) {
                        if (bitmap.getWidth() < renderScreenWidth ||
                                bitmap.getWidth() > maxTextureSize[0] ||
                                bitmap.getHeight() < renderScreenHeight ||
                                bitmap.getHeight() > maxTextureSize[0]) {
                            bitmap = scaleBitmap(bitmap);
                        }
                    }
                    else {
                        bitmap = scaleBitmap(bitmap);
                    }

                    oldBitmapWidth = bitmapWidth;
                    oldBitmapHeight = bitmapHeight;
                    bitmapWidth = bitmap.getWidth();
                    bitmapHeight = bitmap.getHeight();


                    newOffsetX = rawOffsetX * (renderScreenWidth - bitmapWidth);
                    newOffsetY = -(bitmapHeight - renderScreenHeight) / 2;

                    Log.i(TAG, "scaledWidth: " + bitmap.getWidth() + " scaledHeight: " + bitmap.getHeight());

                    setupContainer(bitmapWidth, bitmapHeight);

                    GLES20.glDeleteTextures(1, textureNames, 0);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);

                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                    checkGLError("Bind textureNames[0]");
                    Log.i(TAG, "Bind texture: " + textureNames[0]);

                    GLES20.glDeleteTextures(1, textureNames, 2);

                    if (AppSettings.useEffects()) {
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[2]);

                        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                        checkGLError("Bind textureNames[2]");
                        Log.i(TAG, "Bind texture: " + textureNames[2]);
                        toEffect = true;
                    }


                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
                    Log.i(TAG, "Render texture: " + textureNames[0]);

                    bitmap.recycle();
                    System.gc();

                    if (AppSettings.getTransitionTime() > 0) {
                        useTransition = true;
                        transitionTime = System.currentTimeMillis() + AppSettings.getTransitionTime();
                        setRendererMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                    }
                    else {
                        GLES20.glDeleteTextures(1, textureNames, 1);
                    }
                }
                catch (IllegalArgumentException e) {
                    Log.i(TAG, "Error loading next image");
                }

                Log.i(TAG, "Set bitmap");

            }

            public void checkGLError(String op) {
                int error;
                while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                    Log.e("MyApp", op + ": glError " + error);
                }
            }

            private Bitmap scaleBitmap(Bitmap bitmap) {
                int bitWidth = bitmap.getWidth();
                int bitHeight = bitmap.getHeight();

                if (bitWidth > 0 && bitHeight > 0 && maxTextureSize[0] > 0) {
                    float scaleWidth = renderScreenWidth / bitWidth;
                    float scaleHeight = renderScreenHeight / bitHeight;

                    if (bitWidth * scaleWidth > maxTextureSize[0] ||
                            bitWidth * scaleHeight > maxTextureSize[0] ||
                            bitHeight * scaleWidth > maxTextureSize[0] ||
                            bitHeight * scaleHeight > maxTextureSize[0]) {

                        float ratio = maxTextureSize[0] / renderScreenHeight;

                        int scaledWidth = Math.round(bitHeight * ratio);
                        if (scaledWidth > bitWidth || scaledWidth == 0) {
                            scaledWidth = bitWidth;
                        }

                        if (scaledWidth > maxTextureSize[0]) {
                            scaledWidth = maxTextureSize[0];
                        }

                        bitmap = Bitmap.createBitmap(bitmap, (bitWidth / 2) - (scaledWidth / 2), 0, scaledWidth, bitHeight);

                        bitWidth = bitmap.getWidth();
                        bitHeight = bitmap.getHeight();
                        scaleWidth = renderScreenWidth / bitWidth;
                        scaleHeight = renderScreenHeight / bitHeight;
                    }

                    Matrix matrix = new Matrix();

                    if (AppSettings.fillImages()) {
                        if (scaleWidth > scaleHeight) {
                            matrix.postScale(scaleWidth, scaleWidth);
                        }
                        else {
                            matrix.postScale(scaleHeight, scaleHeight);
                        }
                    }
                    else {
                        if (scaleWidth > scaleHeight) {
                            matrix.postScale(scaleHeight, scaleHeight);
                        }
                        else {
                            matrix.postScale(scaleWidth, scaleWidth);
                        }
                    }

                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitWidth, bitHeight, matrix, false);

                    if (bitmap.getWidth() > maxTextureSize[0]) {
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, maxTextureSize[0], bitmap.getHeight());
                    }
                    if (bitmap.getHeight() > maxTextureSize[0]) {
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), maxTextureSize[0]);
                    }
                }
                return bitmap;
            }

            private void initEffects(int texture) {

                Random random = new Random();

                if (effectFactory == null) {
                    effectFactory = effectContext.getFactory();
                }

                boolean randomApplied = false;

                if (random.nextDouble() <= AppSettings.getRandomEffectsFrequency()) {
                    if (AppSettings.useRandomEffects()) {
                        applyRandomEffects(AppSettings.getRandomEffect(), texture);
                        randomApplied = true;
                        if (AppSettings.useEffectsOverride()) {
                            applyManualEffects(texture);
                        }
                    }
                }

                if (random.nextDouble() > AppSettings.getEffectsFrequency()) {
                    toastEffect("Not applied", "");
                }
                else if (!randomApplied) {
                    applyManualEffects(texture);
                }
            }

            private void applyEffect(Effect setEffect, int texture, String name, String description) {

                setEffect.setUpdateListener(effectUpdateListener);

                GLES20.glDeleteTextures(1, textureNames, texture);
                setEffect.apply(textureNames[2], Math.round(renderScreenWidth), Math.round(renderScreenHeight), textureNames[texture]);
                setEffect.release();

                GLES20.glDeleteTextures(1, textureNames, 2);
                Effect resetEffect = effectFactory.createEffect(EffectFactory.EFFECT_TEMPERATURE);
                resetEffect.setParameter("scale", 0.5f);
                resetEffect.apply(textureNames[texture], Math.round(renderScreenWidth), Math.round(renderScreenHeight), textureNames[2]);
                resetEffect.release();

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[texture]);

                toastEffect(name, description);
                Log.i(TAG, "Effect applied: " + name + "\n" + description);

            }

            private void applyManualEffects(int texture) {

                Effect effect;

                if (AppSettings.getAutoFixEffect() > 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_AUTOFIX)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_AUTOFIX);
                    effect.setParameter("scale", AppSettings.getAutoFixEffect());
                    applyEffect(effect, texture, "Auto Fix", "Value:" + AppSettings.getAutoFixEffect());
                }

                if (AppSettings.getBrightnessEffect() != 1.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_BRIGHTNESS)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_BRIGHTNESS);
                    effect.setParameter("brightness", AppSettings.getBrightnessEffect());
                    applyEffect(effect, texture, "Brightness", "Value:" + AppSettings.getBrightnessEffect());
                }

                if (AppSettings.getContrastEffect() != 1.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_CONTRAST)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_CONTRAST);
                    effect.setParameter("contrast", AppSettings.getContrastEffect());
                    applyEffect(effect, texture, "Contrast", "Value:" + AppSettings.getContrastEffect());
                }

                if (AppSettings.getCrossProcessEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_CROSSPROCESS)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_CROSSPROCESS);
                    applyEffect(effect, texture, "Cross Process", "");
                }

                if (AppSettings.getDocumentaryEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_DOCUMENTARY)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_DOCUMENTARY);
                    applyEffect(effect, texture, "Documentary", "");
                }

                if (AppSettings.getDuotoneEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_DUOTONE)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                    effect.setParameter("first_color", AppSettings.getDuotoneColor(1));
                    effect.setParameter("second_color", AppSettings.getDuotoneColor(2));
                    applyEffect(effect, texture, "Dual Tone", "\nColor 1: " + AppSettings.getDuotoneColor(1) + "\nColor 2: " + AppSettings.getDuotoneColor(2));
                }

                if (AppSettings.getFillLightEffect() > 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_FILLLIGHT)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_FILLLIGHT);
                    effect.setParameter("strength", AppSettings.getFillLightEffect());
                    applyEffect(effect, texture, "Fill Light", "Value:" + AppSettings.getFillLightEffect());
                }

                if (AppSettings.getFisheyeEffect() > 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_FISHEYE)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_FISHEYE);
                    effect.setParameter("scale", AppSettings.getFisheyeEffect());
                    applyEffect(effect, texture, "Fisheye", "Value:" + AppSettings.getFisheyeEffect());
                }

                if (AppSettings.getGrainEffect() > 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_GRAIN)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_GRAIN);
                    effect.setParameter("strength", AppSettings.getGrainEffect());
                    applyEffect(effect, texture, "Grain", "Value:" + AppSettings.getGrainEffect());
                }

                if (AppSettings.getGrayscaleEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_GRAYSCALE)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
                    applyEffect(effect, texture, "Grayscale", "");
                }

                if (AppSettings.getLomoishEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_LOMOISH)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_LOMOISH);
                    applyEffect(effect, texture, "Lomoish", "");
                }

                if (AppSettings.getNegativeEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_NEGATIVE)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_NEGATIVE);
                    applyEffect(effect, texture, "Negaative", "");
                }

                if (AppSettings.getPosterizeEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_POSTERIZE)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_POSTERIZE);
                    applyEffect(effect, texture, "Posterize", "");
                }

                if (AppSettings.getSaturateEffect() != 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_SATURATE)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_SATURATE);
                    effect.setParameter("scale", AppSettings.getSaturateEffect());
                    applyEffect(effect, texture, "Saturate", "Value:" + AppSettings.getSaturateEffect());
                }

                if (AppSettings.getSepiaEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_SEPIA)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_SEPIA);
                    applyEffect(effect, texture, "Sepia", "Value:");
                }

                if (AppSettings.getSharpenEffect() > 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_SHARPEN)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_SHARPEN);
                    effect.setParameter("scale", AppSettings.getSharpenEffect());
                    applyEffect(effect, texture, "Sharpen", "Value:" + AppSettings.getSharpenEffect());
                }

                if (AppSettings.getTemperatureEffect() != 0.5f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_TEMPERATURE)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_TEMPERATURE);
                    effect.setParameter("scale", AppSettings.getTemperatureEffect());
                    applyEffect(effect, texture, "Temperature", "Value:" + AppSettings.getTemperatureEffect());
                }

                if (AppSettings.getVignetteEffect() > 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_VIGNETTE)) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_VIGNETTE);
                    effect.setParameter("scale", AppSettings.getVignetteEffect());
                    applyEffect(effect, texture, "Vignette", "Value:" + AppSettings.getVignetteEffect());
                }
            }

            private void applyRandomEffects(String randomEffect, int texture) {

                Random random = new Random();
                Effect effect;

                switch (randomEffect) {
                    case "Completely Random": {
                        String[] allEffectsList = LiveWallpaperService.this.getResources().getStringArray(R.array.effects_list);
                        String[] allEffectParameters = LiveWallpaperService.this.getResources().getStringArray(R.array.effects_list_parameters);

                        ArrayList<String> usableEffectsList = new ArrayList<>();
                        ArrayList<String> usableEffectsParameters = new ArrayList<>();

                        for (int i = 0; i < allEffectsList.length; i++) {
                            if (EffectFactory.isEffectSupported(allEffectsList[i])) {
                                usableEffectsList.add(allEffectsList[i]);
                                usableEffectsParameters.add(allEffectParameters[i]);
                            }
                        }

                        int index = (int) (Math.random() * usableEffectsList.size());
                        String effectName = usableEffectsList.get(index);
                        String parameter = usableEffectsParameters.get(index);
                        float value = 0.0f;

                        effect = effectFactory.createEffect(effectName);
                        if (usableEffectsList.get(index).equals("android.media.effect.effects.SaturateEffect")) {
                            value = (random.nextFloat() * 0.6f) - 0.3f;
                        }
                        else if (usableEffectsList.get(index).equals("android.media.effect.effects.ColorTemperatureEffect")) {
                            value = random.nextFloat();
                        }
                        else if (parameter.equals("brightness") || parameter.equals("contrast")) {
                            value = (random.nextFloat() * 0.4f) + 0.8f;
                        }
                        else if (!usableEffectsParameters.get(index).equals("none")) {
                            value = (random.nextFloat() * 0.3f) + 0.3f;
                        }

                        if (EffectFactory.isEffectSupported(effectName)) {
                            if (value != 0.0f) {
                                effect.setParameter(parameter, value);
                            }
                            applyEffect(effect, texture, effectName.substring(effectName.indexOf("effects.") + 8), ((value != 0.0f) ? "Value:" + value : ""));
                        }
                        break;
                    }
                    case "Filter Effects": {
                        String[] filtersList = LiveWallpaperService.this.getResources().getStringArray(R.array.effects_filters_list);

                        int index = random.nextInt(filtersList.length);

                        effect = effectFactory.createEffect(filtersList[index]);
                        applyEffect(effect, texture, filtersList[index].substring(filtersList[index].indexOf("effects.") + 8), "");
                        break;
                    }
                    case "Dual Tone Random": {

                        int firstColor = Color.argb(255, random.nextInt(80), random.nextInt(80), random.nextInt(80));
                        int secondColor = Color.argb(255, random.nextInt(100) + 75, random.nextInt(100) + 75, random.nextInt(100) + 75);

                        effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                        effect.setParameter("first_color", firstColor);
                        effect.setParameter("second_color", secondColor);
                        applyEffect(effect, texture, randomEffect, "\n" + firstColor + "\n" + secondColor);

                        break;
                    }
                    case "Dual Tone Rainbow": {

                        ArrayList<String> colorsList = (ArrayList<String>) Arrays.asList(LiveWallpaperService.this.getResources().getStringArray(R.array.effects_color_list));

                        Collections.shuffle(colorsList);

                        int firstColor = Color.parseColor(colorsList.get(0));
                        int secondColor = Color.parseColor(colorsList.get(1));

                        if (AppSettings.useDuotoneGray()) {
                            firstColor = Color.parseColor("gray");
                            Log.i(TAG, "Duotone gray");
                        }

                        effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                        effect.setParameter("first_color", firstColor);
                        effect.setParameter("second_color", secondColor);
                        applyEffect(effect, texture, randomEffect, "\n" + firstColor + "\n" + secondColor);

                        break;
                    }
                    case "Dual Tone Warm": {

                        int firstColor = Color.argb(255, random.nextInt(40) + 40, random.nextInt(40), random.nextInt(40));
                        int secondColor = Color.argb(255, random.nextInt(80) + 150, random.nextInt(80) + 125, random.nextInt(80) + 125);

                        if (AppSettings.useDuotoneGray()) {
                            int grayValue = random.nextInt(50);
                            firstColor = Color.argb(255, grayValue, grayValue, grayValue);
                            Log.i(TAG, "Duotone gray");
                        }

                        effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                        effect.setParameter("first_color", firstColor);
                        effect.setParameter("second_color", secondColor);
                        applyEffect(effect, texture, randomEffect, "\n" + firstColor + "\n" + secondColor);

                        break;
                    }
                    case "Dual Tone Cool": {

                        int firstColor = Color.argb(255, random.nextInt(40), random.nextInt(40) + 40, random.nextInt(40) + 40);
                        int secondColor = Color.argb(255, random.nextInt(80) + 125, random.nextInt(80) + 150, random.nextInt(80) + 150);

                        if (AppSettings.useDuotoneGray()) {
                            int grayValue = random.nextInt(50);
                            firstColor = Color.argb(255, grayValue, grayValue, grayValue);
                            Log.i(TAG, "Duotone gray");
                        }

                        effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                        effect.setParameter("first_color", firstColor);
                        effect.setParameter("second_color", secondColor);
                        applyEffect(effect, texture, randomEffect, "\n" + firstColor + "\n" + secondColor);

                        break;
                    }
                }
            }

            public void release() {
                Log.i(TAG, "release");
            }

        }




    }
}
