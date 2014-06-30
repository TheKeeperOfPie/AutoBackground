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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cw.kop.autobackground.settings.AppSettings;

public class LiveWallpaperService extends GLWallpaperService {

    public static SharedPreferences prefs;
    private static final String TAG = "LiveWallpaperService";
    public static final String UPDATE_NOTIFICATION = "UPDATE_NOTIFICATION";
    public static final String REFRESH_WALLPAPER = "REFRESH_WALLPAPER";
    public static final String DOWNLOAD_WALLPAPER = "DOWNLOAD_WALLPAPER";
    public static final String UPDATE_WALLPAPER = "UPDATE_WALLPAPER";
    public static final String DELETE_WALLPAPER = "DELETE_WALLPAPER";
    public static final String COPY_URL = "COPY_URL";
    private Notification.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private Context appContext;
    private AlarmManager alarmManager;
    private PendingIntent pendingDownloadIntent;
    private PendingIntent pendingIntervalIntent;
    private PendingIntent pendingRefreshIntent;
    private PendingIntent pendingDeleteIntent;
    private PendingIntent pendingCopyIntent;
    private PendingIntent pendingAppIntent;

    public LiveWallpaperService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();

        Intent copyIntent = new Intent();
        copyIntent.setAction(LiveWallpaperService.COPY_URL);
        copyIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        pendingCopyIntent = PendingIntent.getBroadcast(this, 0, copyIntent, 0);

        Intent downloadIntent = new Intent();
        downloadIntent.setAction(LiveWallpaperService.DOWNLOAD_WALLPAPER);
        downloadIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        pendingDownloadIntent = PendingIntent.getBroadcast(this, 0, downloadIntent, 0);

        Intent intervalIntent = new Intent();
        intervalIntent.setAction(LiveWallpaperService.UPDATE_WALLPAPER);
        intervalIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        pendingIntervalIntent = PendingIntent.getBroadcast(this, 0, intervalIntent, 0);

        Intent refreshIntent = new Intent();
        refreshIntent.setAction(LiveWallpaperService.REFRESH_WALLPAPER);
        refreshIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        pendingRefreshIntent = PendingIntent.getBroadcast(this, 0, refreshIntent, 0);

        Intent deleteIntent = new Intent();
        deleteIntent.setAction(LiveWallpaperService.DELETE_WALLPAPER);
        deleteIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        pendingDeleteIntent = PendingIntent.getBroadcast(this, 0, deleteIntent, 0);

        Intent appIntent = new Intent(this, MainPreferences.class);
        pendingAppIntent = PendingIntent.getActivity(this, 0, appIntent, 0);

        Downloader.setNewTask(getApplicationContext());
        Log.i(TAG, "onCreateService");
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        AppSettings.setPrefs(prefs);
        Downloader.getNextImage(appContext);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LiveWallpaperService.UPDATE_NOTIFICATION);
        intentFilter.addAction(LiveWallpaperService.COPY_URL);

        registerReceiver(serviceReceiver, intentFilter);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (AppSettings.useNotification()) {
            startNotification(true);
        }

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        startAlarms();
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
            else if (intent.getAction().equals(LiveWallpaperService.COPY_URL)) {

                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Image URL", Downloader.getBitmapUrl());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Copied image URL to clipboard", Toast.LENGTH_SHORT).show();

            }
            Log.i("Receiver", "ServiceReceived");



        }
    };

    private Target target = new Target() {

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            notificationBuilder.setLargeIcon(bitmap);
            notificationBuilder.setContentText("Drag down to expand options");

            if (Downloader.getBitmapUrl() != null) {
                Log.i("LWS", Downloader.getBitmapUrl());
                notificationBuilder.setContentText(Downloader.getBitmapUrl());
            }
            notificationManager.cancel(0);
            notificationManager.notify(0, notificationBuilder.build());
        }

        @Override
        public void onBitmapFailed(Drawable arg0) {

        }

        @Override
        public void onPrepareLoad(Drawable arg0) {
            // TODO Auto-generated method stub

        }
    };

    public void notifyChangeImage() {

        Picasso.with(appContext).load(Downloader.getBitmapFile()).resizeDimen(android.R.dimen.notification_large_icon_height, android.R.dimen.notification_large_icon_width).into(target);

    }

    private void startNotification(boolean useNotification) {
        if (useNotification) {

            notificationBuilder  = new Notification.Builder(this)
                .setContentTitle("AutoBackground")
                .setContentText("Drag down to expand options")
                .setContentIntent(pendingAppIntent)
                .setSmallIcon(R.drawable.ic_action_picture)
                .setPriority(Notification.PRIORITY_MIN)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_copy, getString(R.string.copy), pendingCopyIntent)
                .addAction(R.drawable.ic_action_refresh, getString(R.string.cycle), pendingRefreshIntent)
                .addAction(R.drawable.ic_action_discard, getString(R.string.delete), pendingDeleteIntent);

            if (Downloader.getBitmap() != null) {
                notificationBuilder.setLargeIcon(Downloader.getBitmap());
            }

            Notification notification = notificationBuilder.build();

            notificationManager.notify(0, notification);

            notifyChangeImage();
        }
        else {
            notificationManager.cancel(0);
        }
    }

    public Engine onCreateEngine() {
        GLWallpaperEngine engine = new GLWallpaperEngine();
        return engine;
    }

    class GLWallpaperEngine extends GLEngine {

        private MyGLRenderer renderer;
        private final Handler handler = new Handler();
        private boolean visible = false;
        private boolean initialized = false;
        private float moffset = 0.0f;
        private Bitmap localBitmap;
        private int screenWidth = 0;
        private int screenHeight = 0;
        private int animationModifier = 1;
        private int animationX = 0;
        private int animationY = 0;
        private boolean toChange = false;
        private boolean animated = false;
        private boolean useOffset = true;
        private boolean surfaceChanged = false;
        private Intent intervalIntent;
        private PendingIntent pendingIntervalIntent;
        private AlarmManager alarmManager;
        private GestureDetector gestureDetector;

        public GLWallpaperEngine() {
            super();
            WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setEGLContextClientVersion(2);

            renderer = new MyGLRenderer(getApplicationContext(), screenWidth, screenHeight);
            setRenderer(renderer);
            setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

            if (!isPreview()){
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(LiveWallpaperService.DOWNLOAD_WALLPAPER);
                intentFilter.addAction(LiveWallpaperService.REFRESH_WALLPAPER);
                intentFilter.addAction(LiveWallpaperService.UPDATE_WALLPAPER);
                intentFilter.addAction(LiveWallpaperService.DELETE_WALLPAPER);
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

                else if (intent.getAction().equals(LiveWallpaperService.REFRESH_WALLPAPER)) {

                    handler.post(new Runnable(){
                        @Override
                        public void run() {
                            loadNextImage(true);
                        }
                    });
                }
                else if (intent.getAction().equals(LiveWallpaperService.UPDATE_WALLPAPER)) {

                    Log.i("LWS", "Interval");

                    if (AppSettings.forceInterval()) {
                        loadNextImage(true);
                    }
                    else {
                        toChange = true;
                    }
                }
                else if (intent.getAction().equals(LiveWallpaperService.DELETE_WALLPAPER)) {

                    Downloader.deleteCurrentBitmap();
                    loadNextImage(true);

                }
            }
        };

        private final Runnable drawRunnable = new Runnable() {
            @Override
            public void run() {
                render();
            }
        };

        private final Runnable drawAnimatedRunnable = new Runnable() {
            @Override
            public void run() {
                drawWallpaper();
                drawAnimated();
            }
        };

        public synchronized void drawWallpaper() {

            handler.removeCallbacks(drawAnimatedRunnable);
            if (visible && initialized) {
                if (animated && (localBitmap != null && localBitmap.getWidth() > screenWidth)) {
                    useOffset = false;
                    handler.post(drawAnimatedRunnable);
                }
                else {
                    useOffset = true;
                    handler.post(drawRunnable);
                }
            }
        }

        synchronized void drawAnimated() {

            Log.i("LWS", "Draw animated");
            if (animationX < (-localBitmap.getWidth() + screenWidth + animationModifier) || animationX > animationModifier) {
                animationModifier *= -1;
            }
            animationX -= animationModifier;
            if (renderer != null) {
                renderer.setOffset(animationX);
                render();
            }
            handler.removeCallbacks(drawAnimatedRunnable);
            handler.postDelayed(drawAnimatedRunnable, (long) (50.0 / Math.abs(animationModifier)));
        }

        private void getNewImages() {
            Downloader.download(getApplicationContext());
        }

        public void onDestroy() {
            super.onDestroy();
            Log.i(TAG, "onDestroy");
            handler.removeCallbacks(drawAnimatedRunnable);
            if (renderer != null) {
                renderer.release();
            }
            renderer = null;
            if (!isPreview()){
                alarmManager.cancel(pendingIntervalIntent);
                unregisterReceiver(updateReceiver);
            }
        }

        @Override
        public void onSurfaceCreated(final SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            initialized = true;
            Log.i(TAG, "onSurfaceCreated");
            drawWallpaper();
        }

        @Override
        public void onSurfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
            super.onSurfaceChanged(holder, format, width, height);
            surfaceChanged = true;
            screenWidth = width;
            screenHeight = height;
            animationX = 0;
            animationModifier = Math.abs(animationModifier);
            moffset = 0;
            renderer.setOffset(moffset);
            Log.i(TAG, "onSurfaceChanged Width: " + screenWidth + " Height: " + screenHeight + " Offset: " + moffset);
        }

        @Override
        public void onVisibilityChanged(final boolean visible) {
            super.onVisibilityChanged(visible);
            this.visible = visible;
            if (visible) {
                drawWallpaper();
            }
            else {
                handler.removeCallbacks(drawRunnable);
                handler.removeCallbacks(drawAnimatedRunnable);
                animated = AppSettings.useAnimation();
                animationModifier = AppSettings.getAnimationSpeed();
                if (toChange) {
                    loadNextImage(false);
                    toChange = false;
                }

                if (AppSettings.changeOnLeave()) {
                    loadNextImage(false);
                }
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
            super.onOffsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels);
            if (useOffset && renderer != null) {
                this.renderer.onOffsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels);
                render();
            }

        }

        public void loadNextImage(final boolean draw) {

            handler.post(new Runnable(){
                @Override
                public void run() {

                    loadNextImage();

                    if (draw && visible) {
                        drawWallpaper();
                    }

                }
            });

        }

        protected void loadNextImage() {
            localBitmap = Downloader.getNextImage(getApplicationContext());

            if (AppSettings.useNotification()) {
                notifyChangeImage();
            }

            if (localBitmap != null && renderer != null) {

                int bitWidth = localBitmap.getWidth();
                int bitHeight = localBitmap.getHeight();
                float scaleWidth = ((float) screenWidth) / bitWidth;
                float scaleHeight = ((float) screenHeight) / bitHeight;
                Matrix matrix = new Matrix();

                if (AppSettings.fillImages() && scaleWidth > scaleHeight) {
                    matrix.postScale(scaleWidth, scaleWidth);
                }
                else {
                    matrix.postScale(scaleHeight, scaleHeight);
                }

                localBitmap = Bitmap.createBitmap(localBitmap, 0, 0, bitWidth, bitHeight, matrix, false);

                if (!surfaceChanged) {
                    animationX = 0;
                    animationModifier = Math.abs(animationModifier);
                }
                else {
                    surfaceChanged = false;
                }

                Log.i(TAG, "Offset: " + moffset);

                addEvent(new Runnable() {

                    @Override
                    public void run() {
                        renderer.setBitmap(localBitmap);
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

            // Geometric variables
            public float vertices[];
            public short indices[];
            public float uvs[];
            public FloatBuffer vertexBuffer;
            public ShortBuffer drawListBuffer;
            public FloatBuffer uvBuffer;

            // Our screenresolution
            private float screenWidth;
            private float screenHeight;

            private float bitmapWidth;
            private float bitmapHeight;
            private float oldBitmapWidth;
            private float oldBitmapHeight;
            private float offset = 0;
            private float oldOffset = 0;

            // Misc
            Context appContext;
            int mProgram;
            private Bitmap localBitmap;
            private float fadeInAlpha = 0f;
            private float fadeOutAlpha = 1.0f;
            private boolean toFade = false;
            private int[] texturenames = new int[2];
            private boolean firstRun = true;

            public MyGLRenderer(Context context, int width, int height) {
                appContext = context;
                screenWidth = width;
                screenHeight = height;

                localBitmap = Downloader.getNextImage(getApplicationContext());

                if (localBitmap != null) {

                    int bitWidth = localBitmap.getWidth();
                    int bitHeight = localBitmap.getHeight();
                    float scaleWidth = ((float) screenWidth) / bitWidth;
                    float scaleHeight = ((float) screenHeight) / bitHeight;
                    Matrix matrix = new Matrix();

                    if (AppSettings.fillImages() && scaleWidth > scaleHeight) {
                        matrix.postScale(scaleWidth, scaleWidth);
                    } else {
                        matrix.postScale(scaleHeight, scaleHeight);
                    }

                    localBitmap = Bitmap.createBitmap(localBitmap, 0, 0, bitWidth, bitHeight, matrix, false);
                }

                if (localBitmap == null) {
                    // If no bitmap available, set wallpaper as app_icon, prevents null pointer checks
                    int id = appContext.getResources().getIdentifier("drawable/app_icon", null, appContext.getPackageName());
                    localBitmap =  BitmapFactory.decodeResource(appContext.getResources(), id);
                }
                bitmapWidth = localBitmap.getWidth();
                bitmapHeight = localBitmap.getHeight();
            }

            public void onPause()
            {
        /* Do stuff to pause the renderer */
            }

            public void onResume()
            {
        /* Do stuff to resume the renderer */
            }

            @Override
            public void onDrawFrame(GL10 gl) {

                if (localBitmap != null) {
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                    // Get handle to textures locations
                    int mAlphaHandle = GLES20.glGetUniformLocation(GLShaders.programImage, "opacity");
                    int mTextureUniformHandle = GLES20.glGetUniformLocation (GLShaders.programImage, "s_texture");

                    if (toFade) {

                        GLES20.glEnable(GLES20.GL_BLEND);
                        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                        setupContainer(oldBitmapWidth, oldBitmapHeight);

                        GLES20.glUniform1f(mAlphaHandle, fadeOutAlpha);
                        fadeOutAlpha -= 0.01f;

                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);
                        android.opengl.Matrix.setIdentityM(transMatrix, 0);
                        android.opengl.Matrix.translateM(transMatrix, 0, oldOffset, 0, 0);

                        Render(mtrxProjectionAndView);

                        setupContainer(bitmapWidth, bitmapHeight);

                        GLES20.glUniform1f(mAlphaHandle, fadeInAlpha);
                        fadeInAlpha += 0.01f;

                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[1]);
                        android.opengl.Matrix.setIdentityM(transMatrix, 0);
                        android.opengl.Matrix.translateM(transMatrix, 0, offset, 0, 0);

                        Render(mtrxProjectionAndView);

                        GLES20.glDisable(GLES20.GL_BLEND);

                        if (fadeInAlpha > 1.0f || fadeOutAlpha < 0.0f) {
                            fadeInAlpha = 0.0f;
                            fadeOutAlpha = 1.0f;
                            int storeId = texturenames[0];
                            texturenames[0] = texturenames[1];
                            texturenames[1] = storeId;
                            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);
                            toFade = false;
                            setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                        }

                    }
                    else {
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);
                        android.opengl.Matrix.setIdentityM(transMatrix, 0);
                        android.opengl.Matrix.translateM(transMatrix, 0, offset, 0, 0);
                        Render(mtrxProjectionAndView);
                    }

                }

            }

            private void Render(float[] m) {

                // clear Screen and Depth Buffer,

                if (localBitmap != null) {

                    // get handle to vertex shader's vPosition member
                    int mPositionHandle = GLES20.glGetAttribLocation(GLShaders.programImage, "vPosition");

                    // Enable generic vertex attribute array
                    GLES20.glEnableVertexAttribArray(mPositionHandle);

                    // Prepare the triangle coordinate data
                    GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

                    // Get handle to texture coordinates location
                    int mTexCoordLoc = GLES20.glGetAttribLocation(GLShaders.programImage, "a_texCoord");

                    // Enable generic vertex attribute array
                    GLES20.glEnableVertexAttribArray(mTexCoordLoc);

                    // Prepare the texturecoordinates
                    GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

                    // Get handle to shape's transformation matrix
                    int mtrxhandle = GLES20.glGetUniformLocation(GLShaders.programImage, "uMVPMatrix");

                    android.opengl.Matrix.multiplyMM(m, 0, mtrxView, 0, transMatrix, 0);

                    // Apply the projection and view transformation
                    GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

                    android.opengl.Matrix.multiplyMM(m, 0, mtrxProjection, 0, transMatrix, 0);

                    GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

                    // Draw the triangle
                    GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

                    // Disable vertex array
                    GLES20.glDisableVertexAttribArray(mPositionHandle);
                    GLES20.glDisableVertexAttribArray(mTexCoordLoc);
                }
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                if (height == 0) {
                    height = 1;
                }

                screenWidth = width;
                screenHeight = height;

                for(int i=0;i<16;i++)
                {
                    mtrxProjection[i] = 0.0f;
                    mtrxView[i] = 0.0f;
                    mtrxProjectionAndView[i] = 0.0f;
                }

                android.opengl.Matrix.orthoM(mtrxProjection, 0, 0f, screenWidth, 0.0f, screenHeight, 0, 50);
                // Set the camera position (View matrix)
                android.opengl.Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0.0f, 0f, 1f, 0.0f);

                // Calculate the projection and view transformation
                android.opengl.Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);

            }

            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {

                Log.i(TAG, "onSurfaceCreated called");

                if (firstRun) {
                    // Generate Textures, if more needed, alter these numbers.
                    GLES20.glGenTextures(2, texturenames, 0);
                    firstRun = false;
                }

                // Create the container
                setupContainer(bitmapWidth, bitmapHeight);

                // Create image texture
                setupImage();

                // Set the clear color to black
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);

                int vertexShader = GLShaders.loadShader(GLES20.GL_VERTEX_SHADER, GLShaders.vertexShaderImage);
                int fragmentShader = GLShaders.loadShader(GLES20.GL_FRAGMENT_SHADER, GLShaders.fragmentShaderImage);

                GLShaders.programImage = GLES20.glCreateProgram();
                GLES20.glAttachShader(GLShaders.programImage, vertexShader);
                GLES20.glAttachShader(GLShaders.programImage, fragmentShader);
                GLES20.glLinkProgram(GLShaders.programImage);

                GLES20.glUseProgram(GLShaders.programImage);

            }

            public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
                offset = (screenWidth - bitmapWidth) * (xOffset);
            }

            public void setupContainer(float width, float height)
            {
                // We have create the vertices of our view.
                vertices = new float[] {
                        0.0f,
                        (screenHeight + ((height - screenHeight) / 2)),
                        0.0f,
                        0.0f,
                        (-(height - screenHeight) / 2),
                        0.0f,
                        width,
                        (-(height - screenHeight) / 2),
                        0.0f,
                        width,
                        (screenHeight + (height - screenHeight) / 2),
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

            public void setBitmap(Bitmap bitmap) {
                localBitmap = bitmap;

                oldOffset = offset;

                offset += (bitmapWidth / 2) - (localBitmap.getWidth() / 2);

                oldBitmapWidth = bitmapWidth;
                oldBitmapHeight = bitmapHeight;

                bitmapWidth = localBitmap.getWidth();
                bitmapHeight = localBitmap.getHeight();

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[1]);
                Log.i(TAG, "Binding " + texturenames[1]);

                // Set filtering
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

                // Set wrapping mode
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, localBitmap, 0);

                toFade = true;

                setRendererMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            }

            public void setOffset(float xOffset) {
                offset = xOffset;
            }

            public void setupImage()
            {
                Log.i(TAG, "Setup image");

                // Create our UV coordinates.
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

                // Bind texture to texturename
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturenames[0]);
                Log.i(TAG, "Binding " + texturenames[0]);

                // Set filtering
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

                // Set wrapping mode
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                // Load the bitmap into the bound texture.
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, localBitmap, 0);
            }

            /**
             * Called when the engine is destroyed. Do any necessary clean up because
             * at this point your renderer instance is now done for.
             */
            public void release() {
                Log.i(TAG, "release");
                localBitmap.recycle();
            }
        }

    }
}
