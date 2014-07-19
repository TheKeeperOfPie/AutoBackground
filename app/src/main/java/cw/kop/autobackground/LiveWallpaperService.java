package cw.kop.autobackground;

import android.app.AlarmManager;
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
import android.graphics.Bitmap;
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
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cw.kop.autobackground.settings.AppSettings;

public class LiveWallpaperService extends GLWallpaperService {

    public static SharedPreferences prefs;
    private static final String TAG = "LiveWallpaperService";
    public static final String UPDATE_NOTIFICATION = "UPDATE_NOTIFICATION";
    public static final String DOWNLOAD_WALLPAPER = "DOWNLOAD_WALLPAPER";
    public static final String UPDATE_WALLPAPER = "UPDATE_WALLPAPER";
    public static final String TOAST_LOCATION = "TOAST_LOCATION";

    public static final String COPY_IMAGE = "COPY_IMAGE";
    public static final String CYCLE_IMAGE = "CYCLE_IMAGE";
    public static final String DELETE_IMAGE = "DELETE_IMAGE";
    public static final String OPEN_IMAGE = "OPEN_IMAGE";
    public static final String PIN_IMAGE = "PIN_IMAGE";
    public static final String PREVIOUS_IMAGE = "PREVIOUS_IMAGE";
    public static final String SHARE_IMAGE = "SHARE_IMAGE";

    private PendingIntent pendingToastIntent;
    private PendingIntent pendingCopyIntent;
    private PendingIntent pendingCycleIntent;
    private PendingIntent pendingDeleteIntent;
    private PendingIntent pendingOpenIntent;
    private PendingIntent pendingPinIntent;
    private PendingIntent pendingPreviousIntent;
    private PendingIntent pendingShareIntent;

    private Notification.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private RemoteViews normalView;
    private RemoteViews bigView;
    private Context appContext;
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

        appContext = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        AppSettings.setPrefs(prefs);
        Downloader.getNextImage(appContext);

        Intent downloadIntent = new Intent();
        downloadIntent.setAction(LiveWallpaperService.DOWNLOAD_WALLPAPER);
        downloadIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        pendingDownloadIntent = PendingIntent.getBroadcast(this, 0, downloadIntent, 0);

        Intent intervalIntent = new Intent();
        intervalIntent.setAction(LiveWallpaperService.UPDATE_WALLPAPER);
        pendingIntervalIntent = PendingIntent.getBroadcast(this, 0, intervalIntent, 0);

        Intent toastIntent = new Intent();
        toastIntent.setAction(LiveWallpaperService.TOAST_LOCATION);
        pendingToastIntent = PendingIntent.getBroadcast(this, 0, toastIntent, 0);

        Intent copyIntent = new Intent();
        copyIntent.setAction(LiveWallpaperService.COPY_IMAGE);
        pendingCopyIntent = PendingIntent.getBroadcast(this, 0, copyIntent, 0);

        Intent cycleIntent = new Intent();
        cycleIntent.setAction(LiveWallpaperService.CYCLE_IMAGE);
        pendingCycleIntent = PendingIntent.getBroadcast(this, 0, cycleIntent, 0);

        Intent deleteIntent = new Intent();
        deleteIntent.setAction(LiveWallpaperService.DELETE_IMAGE);
        pendingDeleteIntent = PendingIntent.getBroadcast(this, 0, deleteIntent, 0);

        Intent openIntent = new Intent();
        openIntent.setAction(LiveWallpaperService.OPEN_IMAGE);
        pendingOpenIntent = PendingIntent.getBroadcast(this, 0, openIntent, 0);

        Intent pinIntent = new Intent();
        pinIntent.setAction(LiveWallpaperService.PIN_IMAGE);
        pendingPinIntent = PendingIntent.getBroadcast(this, 0, pinIntent, 0);

        Intent previousIntent = new Intent();
        previousIntent.setAction(LiveWallpaperService.PREVIOUS_IMAGE);
        pendingPreviousIntent = PendingIntent.getBroadcast(this, 0, previousIntent, 0);

        Intent shareIntent = new Intent();
        shareIntent.setAction(LiveWallpaperService.SHARE_IMAGE);
        pendingShareIntent = PendingIntent.getBroadcast(this, 0, shareIntent, 0);

        Intent appIntent = new Intent(this, MainPreferences.class);
        pendingAppIntent = PendingIntent.getActivity(this, 0, appIntent, 0);

        Downloader.setNewTask(getApplicationContext());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LiveWallpaperService.UPDATE_NOTIFICATION);
        intentFilter.addAction(LiveWallpaperService.COPY_IMAGE);
        intentFilter.addAction(LiveWallpaperService.TOAST_LOCATION);

        registerReceiver(serviceReceiver, intentFilter);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (AppSettings.useNotification()) {
            startNotification(true);
        }

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        startAlarms();
        Log.i(TAG, "onCreateService");
    }

    private void startAlarms() {
        if (AppSettings.useTimer() && AppSettings.getTimerDuration() > 0 && PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(LiveWallpaperService.DOWNLOAD_WALLPAPER), PendingIntent.FLAG_NO_CREATE) != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AppSettings.getTimerDuration(), AppSettings.getTimerDuration(), pendingDownloadIntent);
        }
        if (AppSettings.useInterval() && AppSettings.getIntervalDuration() > 0 && PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(LiveWallpaperService.UPDATE_WALLPAPER), PendingIntent.FLAG_NO_CREATE) != null) {
            alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AppSettings.getIntervalDuration(), AppSettings.getIntervalDuration(), pendingIntervalIntent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancel(0);
        unregisterReceiver(serviceReceiver);
        alarmManager.cancel(pendingDownloadIntent);
        alarmManager.cancel(pendingIntervalIntent);
    }

    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(LiveWallpaperService.UPDATE_NOTIFICATION)) {
                startNotification(intent.getBooleanExtra("use", false));
            }
            else if (intent.getAction().equals(LiveWallpaperService.COPY_IMAGE)) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Image URL", Downloader.getBitmapLocation());
                clipboard.setPrimaryClip(clip);
                if (AppSettings.useToast()) {
                    Toast.makeText(context, "Copied image URL to clipboard", Toast.LENGTH_SHORT).show();
                }
            }
            else if(intent.getAction().equals(LiveWallpaperService.TOAST_LOCATION)) {
                Intent closeDrawer = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(closeDrawer);
                Toast.makeText(context, "Image Location:\n" + Downloader.getBitmapLocation(), Toast.LENGTH_LONG).show();
            }
            Log.i("Receiver", "ServiceReceived");
        }
    };

    private Target target = new Target() {

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            if (notificationBuilder != null && notificationManager != null) {

                if (pinned) {

                    int notifyIconWidth = bitmap.getWidth();
                    int notifyIconHeight = bitmap.getHeight();

                    Drawable[] layers = new Drawable[2];
                    layers[0] = new BitmapDrawable(appContext.getResources(), bitmap);
                    layers[1] = appContext.getResources().getDrawable(R.drawable.pin_overlay);

                    LayerDrawable layerDrawable = new LayerDrawable(layers);

                    Bitmap mutableBitmap = Bitmap.createBitmap(notifyIconWidth, notifyIconHeight, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(mutableBitmap);
                    layerDrawable.setBounds(0, 0, notifyIconWidth, notifyIconHeight);
                    layerDrawable.draw(canvas);

                    normalView.setImageViewBitmap(R.id.notification_icon, mutableBitmap);
                    bigView.setImageViewBitmap(R.id.notification_big_icon, mutableBitmap);
//                    notificationBuilder.setLargeIcon(mutableBitmap);
                }
                else {
                    normalView.setImageViewBitmap(R.id.notification_icon, bitmap);
                    bigView.setImageViewBitmap(R.id.notification_big_icon, bitmap);
//                    notificationBuilder.setLargeIcon(bitmap);
                }

                Notification notification = notificationBuilder.build();

                notification.bigContentView = bigView;

                notificationManager.cancel(0);
                notificationManager.notify(0, notification);
            }
        }

        @Override
        public void onBitmapFailed(Drawable arg0) {
            Log.i(TAG, "Error loading bitmap into notification");
        }

        @Override
        public void onPrepareLoad(Drawable arg0) {
        }
    };

    public void notifyChangeImage() {

        int drawable = AppSettings.getNotificationIcon();

        if (AppSettings.getNotificationTitle().equals("Location") && Downloader.getBitmapLocation() != null) {
            normalView.setTextViewText(R.id.notification_title, Downloader.getBitmapLocation());
            normalView.setOnClickPendingIntent(R.id.notification_title, pendingToastIntent);
            bigView.setTextViewText(R.id.notification_big_title, Downloader.getBitmapLocation());
            bigView.setOnClickPendingIntent(R.id.notification_big_title, pendingToastIntent);
        }
        else {
            normalView.setOnClickPendingIntent(R.id.notification_title, null);
            bigView.setOnClickPendingIntent(R.id.notification_big_title, null);
        }

        if (AppSettings.getNotificationSummary().equals("Location") && Downloader.getBitmapLocation() != null) {
            normalView.setTextViewText(R.id.notification_summary, Downloader.getBitmapLocation());
            normalView.setOnClickPendingIntent(R.id.notification_summary, pendingToastIntent);
            bigView.setTextViewText(R.id.notification_big_summary, Downloader.getBitmapLocation());
            bigView.setOnClickPendingIntent(R.id.notification_big_summary, pendingToastIntent);
        }
        else {
            normalView.setOnClickPendingIntent(R.id.notification_summary, null);
            bigView.setOnClickPendingIntent(R.id.notification_big_summary, null);
        }

        if (AppSettings.useNotificationIconFile() && AppSettings.getNotificationIconFile() != null) {

            File image = new File(AppSettings.getNotificationIconFile());

            if (image.exists() && image.isFile()) {
                Picasso.with(appContext).load(image).resizeDimen(android.R.dimen.notification_large_icon_width, android.R.dimen.notification_large_icon_height).into(target);
            }

        }
        else if (drawable == R.drawable.ic_action_picture || drawable == R.drawable.ic_action_picture_dark){
            Picasso.with(appContext).load(Downloader.getCurrentBitmapFile()).resizeDimen(android.R.dimen.notification_large_icon_width, android.R.dimen.notification_large_icon_height).into(target);
        }
        else {
            normalView.setImageViewResource(R.id.notification_icon, drawable);
            bigView.setImageViewResource(R.id.notification_big_icon, drawable);


            Notification notification = notificationBuilder.build();

            notification.bigContentView = bigView;

            notificationManager.cancel(0);
            notificationManager.notify(0, notification);
        }



    }

    private void startNotification(boolean useNotification) {
        if (useNotification) {
            notificationManager.cancel(0);

            normalView = new RemoteViews(getPackageName(), R.layout.notification_layout);
            normalView.setInt(R.id.notification_container, "setBackgroundColor", AppSettings.getNotificationColor());
            normalView.setImageViewResource(R.id.notification_icon, R.drawable.app_icon);
            normalView.setTextViewText(R.id.notification_title, AppSettings.getNotificationTitle());
            normalView.setInt(R.id.notification_title, "setTextColor", AppSettings.getNotificationTitleColor());
            normalView.setTextViewText(R.id.notification_summary, AppSettings.getNotificationSummary());
            normalView.setInt(R.id.notification_summary, "setTextColor", AppSettings.getNotificationSummaryColor());

            Drawable coloredImageOne = appContext.getResources().getDrawable(getWhiteDrawable(AppSettings.getNotificationOptionDrawable(0)));
            Drawable coloredImageTwo = appContext.getResources().getDrawable(getWhiteDrawable(AppSettings.getNotificationOptionDrawable(1)));;
            Drawable coloredImageThree = appContext.getResources().getDrawable(getWhiteDrawable(AppSettings.getNotificationOptionDrawable(2)));

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

            bigView = new RemoteViews(getPackageName(), R.layout.notification_big_layout);
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

            if (getIntentForNotification(AppSettings.getNotificationOptionTitle(0)) != null) {
                bigView.setOnClickPendingIntent(R.id.notification_button_one, getIntentForNotification(AppSettings.getNotificationOptionTitle(0)));
            }
            if (getIntentForNotification(AppSettings.getNotificationOptionTitle(1)) != null) {
                bigView.setOnClickPendingIntent(R.id.notification_button_two, getIntentForNotification(AppSettings.getNotificationOptionTitle(1)));
            }
            if (getIntentForNotification(AppSettings.getNotificationOptionTitle(2)) != null) {
                bigView.setOnClickPendingIntent(R.id.notification_button_three, getIntentForNotification(AppSettings.getNotificationOptionTitle(2)));
            }

            notificationBuilder  = new Notification.Builder(this)
                    .setContent(normalView)
                    .setContentIntent(pendingAppIntent)
                    .setSmallIcon(R.drawable.ic_action_picture_dark)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setOngoing(true);

            Notification notification = notificationBuilder.build();

            notification.bigContentView = bigView;

            notificationManager.notify(0, notification);

            if (Downloader.getCurrentBitmapFile() != null) {
                notifyChangeImage();
            }
        }
        else {
            notificationManager.cancel(0);
        }
    }

    private int getWhiteDrawable(int drawable) {

        if (drawable == R.drawable.ic_action_copy) {
            return R.drawable.ic_action_copy_dark;
        }
        else if (drawable == R.drawable.ic_action_refresh) {
            return R.drawable.ic_action_refresh_dark;
        }
        else if (drawable == R.drawable.ic_action_discard) {
            return R.drawable.ic_action_discard_dark;
        }
        else if (drawable == R.drawable.ic_action_picture) {
            return R.drawable.ic_action_picture_dark;
        }
        else if (drawable == R.drawable.ic_action_make_available_offline) {
            return R.drawable.ic_action_make_available_offline_dark;
        }
        else if (drawable == R.drawable.ic_action_back) {
            return R.drawable.ic_action_back_dark;
        }
        else if (drawable == R.drawable.ic_action_share) {
            return R.drawable.ic_action_share_dark;
        }
        else if (drawable == R.drawable.ic_action_backspace) {
            return R.drawable.ic_action_backspace_dark;
        }
        else {
            return drawable;
        }
    }

    private PendingIntent getIntentForNotification(String title) {

        if (title.equals("Copy")) {
            return pendingCopyIntent;
        }
        else if (title.equals("Cycle")) {
            return pendingCycleIntent;
        }
        else if (title.equals("Delete")) {
            return pendingDeleteIntent;
        }
        else if (title.equals("Open")) {
            return pendingOpenIntent;
        }
        else if (title.equals("Pin")) {
            return pendingPinIntent;
        }
        else if (title.equals("Previous")) {
            return pendingPreviousIntent;
        }
        else if (title.equals("Share")) {
            return pendingShareIntent;
        }
        return null;
    }

    public Engine onCreateEngine() {
        GLWallpaperEngine engine = new GLWallpaperEngine();
        return engine;
    }

    class GLWallpaperEngine extends GLEngine {

        private MyGLRenderer renderer;
        private final Handler handler = new Handler();
        private int[] maxTextureSize = new int[]{0};
        private int animationModifier = 1;
        private int animationX = 0;
        private int animationY = 0;
        private boolean toChange = false;
        private boolean animated = false;
        private Intent intervalIntent;
        private PendingIntent pendingIntervalIntent;
        private AlarmManager alarmManager;
        private GestureDetector gestureDetector;
        private List<File> previousBitmaps = null;
        private long pinReleaseTime;

        public GLWallpaperEngine() {
            super();

            gestureDetector = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener(){
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    loadNextImage();
                    return true;
                }
            });

            previousBitmaps = new ArrayList<File>();
            pinReleaseTime = System.currentTimeMillis();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setEGLContextClientVersion(2);

            renderer = new MyGLRenderer(getApplicationContext());
            setRenderer(renderer);
            setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

            if (!isPreview()){
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(LiveWallpaperService.DOWNLOAD_WALLPAPER);
                intentFilter.addAction(LiveWallpaperService.CYCLE_IMAGE);
                intentFilter.addAction(LiveWallpaperService.UPDATE_WALLPAPER);
                intentFilter.addAction(LiveWallpaperService.DELETE_IMAGE);
                intentFilter.addAction(LiveWallpaperService.OPEN_IMAGE);
                intentFilter.addAction(LiveWallpaperService.PREVIOUS_IMAGE);
                intentFilter.addAction(LiveWallpaperService.PIN_IMAGE);
                intentFilter.addAction(LiveWallpaperService.SHARE_IMAGE);
                registerReceiver(updateReceiver, intentFilter);
                Log.i("WE", "Registered");

                alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);

                intervalIntent = new Intent();
                intervalIntent.setAction(LiveWallpaperService.UPDATE_WALLPAPER);
                intervalIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                pendingIntervalIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intervalIntent, 0);
            }
        }

        private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction().equals(LiveWallpaperService.DOWNLOAD_WALLPAPER)) {
                    getNewImages();
                }
                else if (intent.getAction().equals(LiveWallpaperService.CYCLE_IMAGE)) {
                    handler.post(new Runnable(){
                        @Override
                        public void run() {
                            loadNextImage();
                        }
                    });
                }
                else if (intent.getAction().equals(LiveWallpaperService.DELETE_IMAGE)) {
                    Downloader.deleteCurrentBitmap();
                    loadNextImage();
                }
                else if (intent.getAction().equals(LiveWallpaperService.OPEN_IMAGE)) {
                    Intent galleryIntent = new Intent();
                    galleryIntent.setAction(Intent.ACTION_VIEW);
                    galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    galleryIntent.setDataAndType(Uri.parse("file://" + Downloader.getCurrentBitmapFile()), "image/*");
                    context.startActivity(galleryIntent);
                    Intent closeDrawer = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    context.sendBroadcast(closeDrawer);
                }
                else if (intent.getAction().equals(LiveWallpaperService.PREVIOUS_IMAGE)) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            loadPreviousImage();
                        }
                    });
                }
                else if (intent.getAction().equals(LiveWallpaperService.PIN_IMAGE)) {
                    if (AppSettings.getPinDuration() > 0 && !pinned) {
                        pinReleaseTime = System.currentTimeMillis() + AppSettings.getPinDuration();
                    }
                    else {
                        pinReleaseTime = 0;
                    }
                    pinned = !pinned;
                    notifyChangeImage();
                }
                else if (intent.getAction().equals(LiveWallpaperService.SHARE_IMAGE)) {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.setType("image/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(Downloader.getCurrentBitmapFile()));
                    shareIntent = Intent.createChooser(shareIntent, "Share Image");
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(shareIntent);
                    Intent closeDrawer = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    context.sendBroadcast(closeDrawer);
                }
                else if (intent.getAction().equals(LiveWallpaperService.UPDATE_WALLPAPER)) {
                    if (AppSettings.forceInterval()) {
                        loadNextImage();
                    }
                    else {
                        toChange = true;
                    }
                }

            }
        };

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (AppSettings.useDoubleTap()) {
                gestureDetector.onTouchEvent(event);
            }
        }

        private final Runnable drawRunnable = new Runnable() {
            @Override
            public void run() {
                render();
            }
        };

        private void getNewImages() {
            Downloader.download(getApplicationContext());
        }

        public void onDestroy() {
            super.onDestroy();
            Log.i(TAG, "onDestroy");
            if (renderer != null) {
                renderer.release();
            }
            renderer = null;
            if (!isPreview()){
                unregisterReceiver(updateReceiver);
            }
        }

        @Override
        public void onSurfaceCreated(final SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            Log.i(TAG, "onSurfaceCreated");
        }

        @Override
        public void onSurfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onVisibilityChanged(final boolean visible) {
            if (visible) {
                super.resume();
                notifyChangeImage();
                if (toChange) {
                    loadNextImage();
                    toChange = false;
                }
                else if (AppSettings.changeOnReturn()) {
                    loadNextImage();
                }
            }
            else {
                super.pause();
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
            super.onOffsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels);
            if (!animated && renderer != null) {
                this.renderer.onOffsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels);
                handler.post(drawRunnable);
            }

        }

        private void loadPreviousImage() {

            handler.post(new Runnable(){
                @Override
                public void run() {

                    if (previousBitmaps.size() > 0) {
                        animated = AppSettings.useAnimation();
                        animationModifier = AppSettings.getAnimationSpeed();
                        renderer.targetFrameTime = 1000 / AppSettings.getAnimationFrameRate();

                        if (animated) {
                            setRendererMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                        } else {
                            setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                        }

                        final Bitmap bitmap = Downloader.getBitmap(previousBitmaps.get(0), getApplicationContext());


                        if (bitmap != null && renderer != null) {

                            if (AppSettings.useNotification()) {
                                notifyChangeImage();
                            }

                            addEvent(new Runnable() {

                                @Override
                                public void run() {
                                    renderer.setBitmap(bitmap, true, 1);
                                }
                            });

                        }
                        previousBitmaps.remove(0);
                    }
                }
            });

        }

        private void loadNextImage() {

            handler.post(new Runnable(){
                @Override
                public void run() {

                    if (pinReleaseTime > 0 && pinReleaseTime < System.currentTimeMillis()) {
                        pinned = false;
                    }

                    if (pinned) {
                        return;
                    }

                    animated = AppSettings.useAnimation();
                    animationModifier = AppSettings.getAnimationSpeed();
                    renderer.targetFrameTime = 1000 / AppSettings.getAnimationFrameRate();

                    if (animated) {
                        setRendererMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                    }
                    else {
                        setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                    }

                    previousBitmaps.add(0, Downloader.getCurrentBitmapFile());

                    final Bitmap bitmap = Downloader.getNextImage(getApplicationContext());

                    if (bitmap != null && renderer != null) {

                        if (AppSettings.useNotification()) {
                            notifyChangeImage();
                        }

                        addEvent(new Runnable() {

                            @Override
                            public void run() {
                                renderer.setBitmap(bitmap, true, 1);
                            }
                        });

                    }

                    if (previousBitmaps.size() > AppSettings.getHistorySize()) {
                        previousBitmaps.remove(previousBitmaps.size() - 1);
                    }
                }
            });

        }


        private void toastEffect(final String effectName, final String effectValue) {
            if (AppSettings.useToast() && AppSettings.useToastEffects()) {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(appContext, "Effect applied: " + effectName + " " + effectValue, Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Toasted");
                    }
                });
            }
        }

        class MyGLRenderer implements GLSurfaceView.Renderer {

            private static final String TAG = "Renderer";

            // Our matrices
            private float[] mtrxProjection = new float[16];
            private float[] mtrxView = new float[16];
            private float[] mtrxProjectionAndView = new float[16];
            private float[] transMatrix = new float[16];
            private int program;

            // Geometric variables
            public float vertices[];
            public short indices[];
            public float uvs[];
            public FloatBuffer vertexBuffer;
            public ShortBuffer drawListBuffer;
            public FloatBuffer uvBuffer;

            // Our screenresolution
            private float renderScreenWidth = 1;
            private float renderScreenHeight = 1;
            private long startTime;
            private long endTime;
            private long frameTime;
            private long targetFrameTime;

            private float bitmapWidth;
            private float bitmapHeight;
            private float oldBitmapWidth;
            private float oldBitmapHeight;
            private float scaledWidth = 1f;
            private float scaledHeight = 1f;
            private float offset = 0f;
            private float newOffset = 0f;
            private float xOffset = 0f;

            // Misc
            Context appContext;
            private float fadeInAlpha = 0.0f;
            private float fadeOutAlpha = 1.0f;
            private boolean toFade = false;
            private int[] textureNames = new int[3];
            private boolean firstRun = true;
            private boolean toEffect = false;
            private boolean contextInitialized = false;
            private EffectContext effectContext;
            private EffectFactory effectFactory;

            public MyGLRenderer(Context context) {
                startTime = System.currentTimeMillis();
                appContext = context;
            }

            @Override
            public void onDrawFrame(GL10 gl) {

                if (!contextInitialized) {
                    effectContext = EffectContext.createWithCurrentGlContext();
                    setPreserveEGLContextOnPause(true);
                    contextInitialized = true;
                }

                if (toEffect && effectContext != null) {

                    if (toFade) {
                        initEffects(1);
                    }
                    else {
                        initEffects(0);
                    }
                    Log.i(TAG, "Applied image effects");

                    toEffect = false;

                    GLES20.glDeleteTextures(1, textureNames, 2);
                }

                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                // Get handle to textures locations
                int mAlphaHandle = GLES20.glGetUniformLocation(program, "opacity");
                int mTextureUniformHandle = GLES20.glGetUniformLocation (program, "s_texture");

                if (toFade) {

                    GLES20.glEnable(GLES20.GL_BLEND);
                    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                    setupContainer(oldBitmapWidth, oldBitmapHeight);

                    GLES20.glUniform1f(mAlphaHandle, fadeOutAlpha);
                    fadeOutAlpha -= 0.03f;

                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
                    android.opengl.Matrix.setIdentityM(transMatrix, 0);
                    android.opengl.Matrix.translateM(transMatrix, 0, offset, 0, 0);

                    renderImage();

                    setupContainer(bitmapWidth, bitmapHeight);

                    GLES20.glUniform1f(mAlphaHandle, fadeInAlpha);
                    fadeInAlpha += 0.03f;

                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[1]);
                    android.opengl.Matrix.setIdentityM(transMatrix, 0);
                    android.opengl.Matrix.translateM(transMatrix, 0, newOffset, 0, 0);

                    renderImage();

                    GLES20.glDisable(GLES20.GL_BLEND);

                    if (fadeInAlpha > 1.0f || fadeOutAlpha < 0.0f) {
                        toFade = false;
                        fadeInAlpha = 0.0f;
                        fadeOutAlpha = 1.0f;
                        offset = newOffset;
                        animationX = (int) offset;
                        int storeId = textureNames[0];
                        textureNames[0] = textureNames[1];
                        textureNames[1] = storeId;
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
                        android.opengl.Matrix.setIdentityM(transMatrix, 0);
                        android.opengl.Matrix.translateM(transMatrix, 0, offset, 0, 0);
                        GLES20.glDeleteTextures(1, textureNames, 1);
                        render();

                        if (!animated) {
                            setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                        }
                    }

                }
                else {

                    try {
                        endTime = System.currentTimeMillis();
                        frameTime = endTime - startTime;
                        if (frameTime < targetFrameTime)
                            Thread.sleep(targetFrameTime - frameTime);
                        startTime = System.currentTimeMillis();
                    }
                    catch (InterruptedException e) {

                    }

                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
                    android.opengl.Matrix.setIdentityM(transMatrix, 0);
                    android.opengl.Matrix.translateM(transMatrix, 0, offset, 0, 0);
                    renderImage();

                    if (animated) {
                        if (animationX < (-bitmapWidth + renderScreenWidth + animationModifier)) {
                            animationModifier = -Math.abs(animationModifier);
                        }
                        else if (animationX > animationModifier) {
                            animationModifier = Math.abs(animationModifier);
                        }
                        animationX -= animationModifier;
                        offset = animationX;
                        newOffset -= animationModifier;
                    }
                }
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

                android.opengl.Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxView, 0, transMatrix, 0);
                android.opengl.Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, transMatrix, 0);

                // Apply the projection and view transformation
                GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, mtrxProjectionAndView, 0);

                // Draw the container
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

                // Disable vertex array
                GLES20.glDisableVertexAttribArray(mPositionHandle);
                GLES20.glDisableVertexAttribArray(mTexCoordLoc);
            }


            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {

                Log.i(TAG, "Renderer onSurfaceChanged");

                Log.i(TAG, "Width: " + renderScreenWidth + "Height: " + renderScreenHeight);

                if (width != renderScreenWidth) {
                    Log.i(TAG, "Rescale");
                    GLES20.glViewport(0, 0, width, height);
                }

                renderScreenWidth = width;
                renderScreenHeight = height;

                for(int i=0;i<16;i++)
                {
                    mtrxProjection[i] = 0.0f;
                    mtrxView[i] = 0.0f;
                    mtrxProjectionAndView[i] = 0.0f;
                }

                android.opengl.Matrix.orthoM(mtrxProjection, 0, 0f, width, 0.0f, height, 0, 50);

                // Set the camera position (View matrix)
                android.opengl.Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0.0f, 0f, 1f, 0.0f);

                // Calculate the projection and view transformation
                android.opengl.Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);

            }

            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {

                if (firstRun) {

                    int vertexShader = GLShaders.loadShader(GLES20.GL_VERTEX_SHADER, GLShaders.vertexShaderImage);
                    int fragmentShader = GLShaders.loadShader(GLES20.GL_FRAGMENT_SHADER, GLShaders.fragmentShaderImage);

                    program = GLES20.glCreateProgram();
                    GLES20.glAttachShader(program, vertexShader);
                    GLES20.glAttachShader(program, fragmentShader);
                    GLES20.glLinkProgram(program);
                    GLES20.glUseProgram(program);

                    GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);

                    Log.i(TAG, "Max texture size: " + maxTextureSize[0]);

                    Log.i(TAG, "First run");
                    GLES20.glGenTextures(3, textureNames, 0);

                    uvs = new float[] {
                            0.0f, 0.0f,
                            0.0f, 1.0f,
                            1.0f, 1.0f,
                            1.0f, 0.0f
                    };

                    // The texture buffer
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

                if (toFade) {
                    Log.i(TAG, "Fade reset");
                    fadeInAlpha = 0.0f;
                    fadeOutAlpha = 1.0f;
                    offset = newOffset;
                    int storeId = textureNames[0];
                    textureNames[0] = textureNames[1];
                    textureNames[1] = storeId;
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
                    toFade = false;
                }

                if (animated) {
                    setRendererMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                }
                else {
                    setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                }

                if (animationX < renderScreenWidth - bitmapWidth) {
                    animationX = 0;
                }

                if (AppSettings.useEffects()) {
                    toEffect = true;
                }

                Log.i(TAG, "onSurfaceCreated");
            }

            public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
                offset = (renderScreenWidth - bitmapWidth) * (xOffset);
                this.xOffset = xOffset;
            }

            public void setupContainer(float width, float height) {
                vertices = new float[] {
                        0.0f,
                        (renderScreenHeight + ((height - renderScreenHeight) / 2)),
                        0.0f,
                        0.0f,
                        (-(height - renderScreenHeight) / 2),
                        0.0f,
                        width,
                        (-(height - renderScreenHeight) / 2),
                        0.0f,
                        width,
                        (renderScreenHeight + (height - renderScreenHeight) / 2),
                        0.0f
                };

                indices = new short[] {0, 1, 2, 0, 2, 3}; // render order of vertices

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

            public void setBitmap(Bitmap bitmap, boolean fade, int texture) {

                Log.i(TAG, "startWidth: " + bitmap.getWidth() + " startHeight: " + bitmap.getHeight());

                Bitmap scaled = scaleBitmap(bitmap);

                oldBitmapWidth = bitmapWidth;
                oldBitmapHeight = bitmapHeight;
                bitmapWidth = scaled.getWidth();
                bitmapHeight = scaled.getHeight();

                newOffset = xOffset * (renderScreenWidth - bitmapWidth);

                Log.i(TAG, "scaledWidth: " + scaled.getWidth() + " scaledHeight: " + scaled.getHeight());

                setupContainer(bitmapWidth, bitmapHeight);

                GLES20.glDeleteTextures(1, textureNames, texture);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[texture]);

                // Set filtering"
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

                // Set wrapping mode
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, scaled, 0);

                if (texture == 1 && AppSettings.useEffects()) {
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[2]);

                    // Set filtering
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

                    // Set wrapping mode
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, scaled, 0);
                    toEffect = true;
                }

                bitmap.recycle();

                if (AppSettings.useFade() && fade && isVisible()) {
                    Log.i(TAG, "Fade set");
                    toFade = fade;
                    setRendererMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                }
                else if (texture == 1 && isVisible()) {
                    Log.i(TAG, "Syncing textures");
                    int storeId = textureNames[0];
                    textureNames[0] = textureNames[1];
                    textureNames[1] = storeId;
                    offset = newOffset;
                    render();
                }

            }

            protected Bitmap scaleBitmap(Bitmap bitmap) {
                int bitWidth = bitmap.getWidth();
                int bitHeight = bitmap.getHeight();
                float scaleWidth = renderScreenWidth / bitWidth;
                float scaleHeight = renderScreenHeight / bitHeight;

                if (bitWidth * scaleWidth > maxTextureSize[0] ||
                        bitWidth * scaleHeight > maxTextureSize[0] ||
                        bitHeight * scaleWidth > maxTextureSize[0] ||
                        bitHeight * scaleHeight > maxTextureSize[0]) {

                    int ratio = Math.round(maxTextureSize[0] / renderScreenHeight);
                    int scaledWidth = bitHeight * ratio;
                    if (scaledWidth > bitWidth || scaledWidth == 0) {
                        scaledWidth = bitWidth;
                    }

                    bitmap = Bitmap.createBitmap(bitmap, (bitWidth / 2) - (scaledWidth / 2), 0, scaledWidth, bitHeight);

                    bitWidth = bitmap.getWidth();
                    bitHeight = bitmap.getHeight();
                    scaleWidth = renderScreenWidth / bitWidth;
                    scaleHeight = renderScreenHeight / bitHeight;
                }

                Matrix matrix = new Matrix();

                if (AppSettings.fillImages() && scaleWidth > scaleHeight) {
                    matrix.postScale(scaleWidth, scaleWidth);
                }
                else {
                    matrix.postScale(scaleHeight, scaleHeight);
                }

                return Bitmap.createBitmap(bitmap, 0, 0, bitWidth, bitHeight, matrix, false);
            }

            private void initEffects(int texture) {

                Random random = new Random();

                if (random.nextDouble() > AppSettings.getEffectsFrequency()) {
                    toastEffect("Not applied", "");
                    return;
                }

                if (effectFactory == null) {
                    effectFactory = effectContext.getFactory();
                }

                Effect effect;

                if (AppSettings.useRandomEffects()) {
                    applyRandomEffects(AppSettings.getRandomEffect(), texture);
                }

                if (!AppSettings.useRandomEffects() || AppSettings.useEffectsOverride()) {
                    if (AppSettings.getAutoFixEffect() > 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_AUTOFIX)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_AUTOFIX);
                        effect.setParameter("scale", AppSettings.getAutoFixEffect());
                        applyEffect(effect, texture);
                        toastEffect("Auto Fix", "Value:" + AppSettings.getAutoFixEffect());
                    }

                    if (AppSettings.getBrightnessEffect() != 1.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_BRIGHTNESS)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_BRIGHTNESS);
                        effect.setParameter("brightness", AppSettings.getBrightnessEffect());
                        applyEffect(effect, texture);
                        toastEffect("Brightness", "Value:" + AppSettings.getBrightnessEffect());
                    }

                    if (AppSettings.getContrastEffect() != 1.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_CONTRAST)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_CONTRAST);
                        effect.setParameter("contrast", AppSettings.getContrastEffect());
                        applyEffect(effect, texture);
                        toastEffect("Contrast", "Value:" + AppSettings.getContrastEffect());
                    }

                    if (AppSettings.getCrossProcessEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_CROSSPROCESS)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_CROSSPROCESS);
                        applyEffect(effect, texture);
                        toastEffect("Cross Process", "");
                    }

                    if (AppSettings.getDocumentaryEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_DOCUMENTARY)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_DOCUMENTARY);
                        applyEffect(effect, texture);
                        toastEffect("Documentary", "");
                    }

                    if (AppSettings.getFillLightEffect() > 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_FILLLIGHT)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_FILLLIGHT);
                        effect.setParameter("strength", AppSettings.getFillLightEffect());
                        applyEffect(effect, texture);
                        toastEffect("Fill Light", "Value:" + AppSettings.getFillLightEffect());
                    }

                    if (AppSettings.getFisheyeEffect() > 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_FISHEYE)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_FISHEYE);
                        effect.setParameter("scale", AppSettings.getFisheyeEffect());
                        applyEffect(effect, texture);
                        toastEffect("Fisheye", "Value:" + AppSettings.getFisheyeEffect());
                    }

                    if (AppSettings.getGrainEffect() > 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_GRAIN)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_GRAIN);
                        effect.setParameter("strength", AppSettings.getGrainEffect());
                        applyEffect(effect, texture);
                        toastEffect("Grain", "Value:" + AppSettings.getGrainEffect());
                    }

                    if (AppSettings.getGrayscaleEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_GRAYSCALE)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
                        applyEffect(effect, texture);
                        toastEffect("Grayscale", "");
                    }

                    if (AppSettings.getLomoishEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_LOMOISH)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_LOMOISH);
                        applyEffect(effect, texture);
                        toastEffect("Lomoish", "");
                    }

                    if (AppSettings.getNegativeEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_NEGATIVE)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_NEGATIVE);
                        applyEffect(effect, texture);
                        toastEffect("Negaative", "");
                    }

                    if (AppSettings.getPosterizeEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_POSTERIZE)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_POSTERIZE);
                        applyEffect(effect, texture);
                        toastEffect("Posterize", "");
                    }

                    if (AppSettings.getSaturateEffect() != 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_SATURATE)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_SATURATE);
                        effect.setParameter("scale", AppSettings.getSaturateEffect());
                        applyEffect(effect, texture);
                        toastEffect("Saturate", "Value:" + AppSettings.getSaturateEffect());
                    }

                    if (AppSettings.getSepiaEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_SEPIA)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_SEPIA);
                        applyEffect(effect, texture);
                        toastEffect("Sepia", "Value:");
                    }

                    if (AppSettings.getSharpenEffect() > 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_SHARPEN)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_SHARPEN);
                        effect.setParameter("scale", AppSettings.getSharpenEffect());
                        applyEffect(effect, texture);
                        toastEffect("Sharpen", "Value:" + AppSettings.getSharpenEffect());
                    }

                    if (AppSettings.getTemperatureEffect() != 0.5f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_TEMPERATURE)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_TEMPERATURE);
                        effect.setParameter("scale", AppSettings.getTemperatureEffect());
                        applyEffect(effect, texture);
                        toastEffect("Temperature", "Value:" + AppSettings.getTemperatureEffect());
                    }

                    if (AppSettings.getVignetteEffect() > 0.0f && EffectFactory.isEffectSupported(EffectFactory.EFFECT_VIGNETTE)) {
                        effect = effectFactory.createEffect(EffectFactory.EFFECT_VIGNETTE);
                        effect.setParameter("scale", AppSettings.getVignetteEffect());
                        applyEffect(effect, texture);
                        toastEffect("Vignette", "Value:" + AppSettings.getVignetteEffect());
                    }
                }
            }

            private void applyEffect(Effect setEffect, int texture) {

                GLES20.glDeleteTextures(1, textureNames, texture);
                setEffect.apply(textureNames[2], Math.round(renderScreenWidth), Math.round(renderScreenHeight), textureNames[texture]);
                GLES20.glDeleteTextures(1, textureNames, 2);
                setEffect.apply(textureNames[texture], Math.round(renderScreenWidth), Math.round(renderScreenHeight), textureNames[2]);
                setEffect.release();

            }

            private void applyRandomEffects(String randomEffect, int texture) {

                Random random = new Random();

                Effect effect;

                if (randomEffect.equals("Completely Random")) {
                    String[] effectsList = appContext.getResources().getStringArray(R.array.effects_list);
                    String[] effectParameters = appContext.getResources().getStringArray(R.array.effects_list_parameters);

                    int index = (int) (Math.random() * effectsList.length);
                    String effectName = effectsList[index];
                    String parameter = effectParameters[index];
                    float value = 3.0f;

                    effect = effectFactory.createEffect(effectName);
                    if (effectsList[index].equals("android.media.effect.effects.SaturateEffect")) {
                        value = (float) (Math.random() * 0.6f) - 0.3f;
                    }
                    else if (effectsList[index].equals("android.media.effect.effects.ColorTemperatureEffect")) {
                        value = (float) Math.random();
                    }
                    else if (parameter.equals("brightness") || parameter.equals("contrast")) {
                        value = (float) (Math.random() * 0.4f) + 0.8f;
                    }
                    else if (!effectParameters[index].equals("none")) {
                        value = (float) (Math.random() * 0.3f) + 0.3f;
                    }

                    if (EffectFactory.isEffectSupported(effectName)) {
                        if (value < 3.0f) {
                            effect.setParameter(parameter, value);
                        }
                        applyEffect(effect, texture);
                    }

                    Log.i(TAG, "Effect applied: " + effectsList[index]);
                    toastEffect(effectName.substring(effectName.indexOf("effects.") + 8), "");
                }
                else if (randomEffect.equals("Filter Effects")){
                    String[] filtersList = appContext.getResources().getStringArray(R.array.effects_filters_list);

                    int index = random.nextInt(filtersList.length);

                    effect = effectFactory.createEffect(filtersList[index]);
                    applyEffect(effect, texture);
                    toastEffect(filtersList[index].substring(filtersList[index].indexOf("effects.") + 8), "");
                }
                else if (randomEffect.equals("Dual Tone Random")) {

                    int firstColor = Color.argb(255, random.nextInt(80), random.nextInt(80), random.nextInt(80));
                    int secondColor = Color.argb(255, random.nextInt(100) + 75, random.nextInt(100) + 75, random.nextInt(100) + 75);

                    effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                    effect.setParameter("first_color", firstColor);
                    effect.setParameter("second_color", secondColor);
                    applyEffect(effect, texture);

                    toastEffect(randomEffect, "\n" + firstColor + "\n" + secondColor);

                }
                else if (randomEffect.equals("Dual Tone Rainbow")) {

                    ArrayList<String> colorsList = (ArrayList<String>) Arrays.asList(appContext.getResources().getStringArray(R.array.effects_color_list));

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
                    applyEffect(effect, texture);

                    toastEffect(randomEffect, "\n" + firstColor + "\n" + secondColor);

                }
                else if (randomEffect.equals("Dual Tone Warm")) {

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
                    applyEffect(effect, texture);

                    toastEffect(randomEffect, "\n" + firstColor + "\n" + secondColor);

                }
                else if (randomEffect.equals("Dual Tone Cool")) {

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
                    applyEffect(effect, texture);

                    toastEffect(randomEffect, "\n" + firstColor + "\n" + secondColor);

                }
            }

            /**
             * Called when the engine is destroyed. Do any necessary clean up because
             * at this point your renderer instance is now done for.
             */
            public void release() {
                Log.i(TAG, "release");
            }

        }

    }
}
