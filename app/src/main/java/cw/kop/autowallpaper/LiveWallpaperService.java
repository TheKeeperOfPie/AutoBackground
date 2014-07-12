package cw.kop.autowallpaper;

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
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
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

import cw.kop.autowallpaper.settings.AppSettings;

public class LiveWallpaperService extends GLWallpaperService {

    public static SharedPreferences prefs;
    private static final String TAG = "LiveWallpaperService";
    public static final String UPDATE_NOTIFICATION = "UPDATE_NOTIFICATION";
    public static final String CYCLE_WALLPAPER = "CYCLE_WALLPAPER";
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
    private PendingIntent pendingCycleIntent;
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
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        AppSettings.setPrefs(prefs);
        Downloader.getNextImage(appContext);

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

        Intent cycleIntent = new Intent();
        cycleIntent.setAction(LiveWallpaperService.CYCLE_WALLPAPER);
        cycleIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        pendingCycleIntent = PendingIntent.getBroadcast(this, 0, cycleIntent, 0);

        Intent deleteIntent = new Intent();
        deleteIntent.setAction(LiveWallpaperService.DELETE_WALLPAPER);
        deleteIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        pendingDeleteIntent = PendingIntent.getBroadcast(this, 0, deleteIntent, 0);

        Intent appIntent = new Intent(this, MainPreferences.class);
        pendingAppIntent = PendingIntent.getActivity(this, 0, appIntent, 0);

        Downloader.setNewTask(getApplicationContext());

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
            else if (intent.getAction().equals(LiveWallpaperService.COPY_URL)) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Image URL", Downloader.getBitmapUrl());
                clipboard.setPrimaryClip(clip);
                if (AppSettings.useToast()) {
                    Toast.makeText(context, "Copied image URL to clipboard", Toast.LENGTH_SHORT).show();
                }
            }
            Log.i("Receiver", "ServiceReceived");
        }
    };

    private Target target = new Target() {

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            if (notificationBuilder != null && notificationManager != null) {
                notificationBuilder.setLargeIcon(bitmap);
                notificationBuilder.setContentText("Drag down to expand options");

                if (Downloader.getBitmapUrl() != null) {
                    Log.i("LWS", Downloader.getBitmapUrl());
                    notificationBuilder.setContentText(Downloader.getBitmapUrl());
                }
                notificationManager.cancel(0);
                notificationManager.notify(0, notificationBuilder.build());
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
                .addAction(R.drawable.ic_action_refresh, getString(R.string.cycle), pendingCycleIntent)
                .addAction(R.drawable.ic_action_discard, getString(R.string.delete), pendingDeleteIntent);

            Notification notification = notificationBuilder.build();

            notificationManager.notify(0, notification);

            if (Downloader.getBitmapFile() != null) {
                notifyChangeImage();
            }
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
        private int[] maxTextureSize = new int[]{0};
        private int screenWidth = 0;
        private int screenHeight = 0;
        private int animationModifier = 1;
        private int animationX = 0;
        private int animationY = 0;
        private boolean toChange = false;
        private boolean animated = false;
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

            gestureDetector = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener(){
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    loadNextImage();
                    return true;
                }
            });
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
                intentFilter.addAction(LiveWallpaperService.CYCLE_WALLPAPER);
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
                else if (intent.getAction().equals(LiveWallpaperService.CYCLE_WALLPAPER)) {
                    handler.post(new Runnable(){
                        @Override
                        public void run() {
                            loadNextImage();
                        }
                    });
                }
                else if (intent.getAction().equals(LiveWallpaperService.UPDATE_WALLPAPER)) {
                    if (AppSettings.forceInterval()) {
                        loadNextImage();
                    }
                    else {
                        toChange = true;
                    }
                }
                else if (intent.getAction().equals(LiveWallpaperService.DELETE_WALLPAPER)) {
                    Downloader.deleteCurrentBitmap();
                    loadNextImage();
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
            screenWidth = width;
            screenHeight = height;
            Log.i(TAG, "onSurfaceChanged Width: " + screenWidth + " Height: " + screenHeight);
        }

        @Override
        public void onVisibilityChanged(final boolean visible) {
            if (visible) {
                super.resume();
                notifyChangeImage();
                render();
            }
            else {
                super.pause();
                if (toChange) {
                    loadNextImage();
                    toChange = false;
                }
                else if (AppSettings.changeOnLeave()) {
                    loadNextImage();
                }
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

        public void loadNextImage() {

            handler.post(new Runnable(){
                @Override
                public void run() {
                    animated = AppSettings.useAnimation();
                    animationModifier = AppSettings.getAnimationSpeed();

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
                }
            });

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
            private float renderScreenWidth;
            private float renderScreenHeight;

            private float bitmapWidth;
            private float bitmapHeight;
            private float oldBitmapWidth;
            private float oldBitmapHeight;
            private float offset = 0f;
            private float newOffset = 0f;
            private float xOffset = 0f;

            // Misc
            Context appContext;
            private Bitmap localBitmap;
            private float fadeInAlpha = 0f;
            private float fadeOutAlpha = 1.0f;
            private boolean toFade = false;
            private int[] textureNames = new int[3];
            private boolean firstRun = true;
            private boolean toEffect = false;
            private EffectContext effectContext;
            private EffectFactory effectFactory;
            private Effect effect;

            public MyGLRenderer(Context context, int width, int height) {
                appContext = context;
                renderScreenWidth = width;
                renderScreenHeight = height;

                localBitmap = Downloader.getNextImage(getApplicationContext());

                if (localBitmap == null) {
                    // If no bitmap available, set wallpaper as app_icon, prevents null pointer checks
                    int id = appContext.getResources().getIdentifier("drawable/app_icon", null, appContext.getPackageName());
                    localBitmap =  BitmapFactory.decodeResource(appContext.getResources(), id);
                }
                else {
                    localBitmap = scaleBitmap(localBitmap);
                }

                bitmapWidth = localBitmap.getWidth();
                bitmapHeight = localBitmap.getHeight();
            }

            @Override
            public void onDrawFrame(GL10 gl) {

                effectContext = EffectContext.createWithCurrentGlContext();

                if (toEffect) {
                    initEffects();
                    effect.apply(textureNames[1], Math.round(bitmapWidth), Math.round(bitmapHeight), textureNames[2]);
                    toEffect = false;
                }

                if (localBitmap != null) {
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                    // Get handle to textures locations
                    int mAlphaHandle = GLES20.glGetUniformLocation(program, "opacity");
                    int mTextureUniformHandle = GLES20.glGetUniformLocation (program, "s_texture");

                    if (toFade) {

                        GLES20.glEnable(GLES20.GL_BLEND);
                        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                        setupContainer(oldBitmapWidth, oldBitmapHeight);

                        GLES20.glUniform1f(mAlphaHandle, fadeOutAlpha);
                        fadeOutAlpha -= 0.015f;

                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
                        android.opengl.Matrix.setIdentityM(transMatrix, 0);
                        android.opengl.Matrix.translateM(transMatrix, 0, offset, 0, 0);

                        Render(mtrxProjectionAndView);

                        setupContainer(bitmapWidth, bitmapHeight);

                        GLES20.glUniform1f(mAlphaHandle, fadeInAlpha);
                        fadeInAlpha += 0.015f;

                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[1]);
                        android.opengl.Matrix.setIdentityM(transMatrix, 0);
                        android.opengl.Matrix.translateM(transMatrix, 0, newOffset, 0, 0);

                        Render(mtrxProjectionAndView);

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
                            Render(mtrxProjectionAndView);
                            setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                        }

                    }
                    else {
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
                        android.opengl.Matrix.setIdentityM(transMatrix, 0);
                        android.opengl.Matrix.translateM(transMatrix, 0, offset, 0, 0);
                        Render(mtrxProjectionAndView);
                    }

                    if (animated && !toFade) {
                        if (animationX < (-bitmapWidth + renderScreenWidth + animationModifier)) {
                            animationModifier = -Math.abs(animationModifier);
                        }
                        else if (animationX > animationModifier) {
                            animationModifier = Math.abs(animationModifier);
                        }
                        animationX -= animationModifier;
                        offset = animationX;
                        newOffset -= animationModifier;
                        if (getRendererMode() == GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                            handler.removeCallbacks(drawRunnable);
                            handler.postDelayed(drawRunnable, (long) (50.0 / Math.abs(animationModifier)));
                        }
                    }
                }

            }

            private void Render(float[] m) {

                // clear Screen and Depth Buffer,

                if (localBitmap != null) {

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

                    android.opengl.Matrix.multiplyMM(m, 0, mtrxView, 0, transMatrix, 0);

                    // Apply the projection and view transformation
                    GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

                    android.opengl.Matrix.multiplyMM(m, 0, mtrxProjection, 0, transMatrix, 0);

                    GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

                    // Draw the container
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
                localBitmap = scaleBitmap(localBitmap);
                addEvent(new Runnable() {

                    @Override
                    public void run() {
                        renderer.setupContainer(localBitmap.getWidth(), localBitmap.getHeight());
                        renderer.setBitmap(localBitmap, false, 0);
                    }
                });

                renderScreenWidth = width;
                renderScreenHeight = height;

                for(int i=0;i<16;i++)
                {
                    mtrxProjection[i] = 0.0f;
                    mtrxView[i] = 0.0f;
                    mtrxProjectionAndView[i] = 0.0f;
                }

                android.opengl.Matrix.orthoM(mtrxProjection, 0, 0f, renderScreenWidth, 0.0f, renderScreenHeight, 0, 50);
                // Set the camera position (View matrix)
                android.opengl.Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0.0f, 0f, 1f, 0.0f);

                // Calculate the projection and view transformation
                android.opengl.Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);

                render();
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

                // Set the clear color to black
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);

                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                if (firstRun) {

                    GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);

                    Log.i(TAG, "Max texture size: " + maxTextureSize[0]);

                    if (localBitmap != null) {
                        localBitmap = scaleBitmap(localBitmap);
                    }

                    Log.i(TAG, "First run");
                    // Generate Textures, if more needed, alter these numbers.
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
                }

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
                    setRendererMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                }

                // Create the container
                setupContainer(bitmapWidth, bitmapHeight);

                // Create image texture
                setBitmap(localBitmap, false, 0);

                Log.i(TAG, "Animation offset: " + animationX);

                if (animationX < renderScreenWidth - bitmapWidth) {
                    animationX = 0;
                }

                Log.i(TAG, "Animation offset: " + animationX);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
                android.opengl.Matrix.setIdentityM(transMatrix, 0);
                android.opengl.Matrix.translateM(transMatrix, 0, offset, 0, 0);
                Render(mtrxProjectionAndView);

                if (AppSettings.useEffects()) {
                    toEffect = true;
                }

                Log.i(TAG, "onSurfaceCreated");
            }

            public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
                offset = (renderScreenWidth - bitmapWidth) * (xOffset);
                this.xOffset = xOffset;
            }

            public void setupContainer(float width, float height)
            {
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
                localBitmap = scaleBitmap(bitmap);

                oldBitmapWidth = bitmapWidth;
                oldBitmapHeight = bitmapHeight;
                bitmapWidth = localBitmap.getWidth();
                bitmapHeight = localBitmap.getHeight();

                Log.i(TAG, "Offset: " + offset);
                Log.i(TAG, "screenWidth: " + renderScreenWidth + " oldBitmapWidth: " + oldBitmapWidth + " bitmapWidth: " + bitmapWidth);
                Log.i(TAG, "screenHeight: " + renderScreenHeight + " oldBitmapHeight: " + oldBitmapHeight + " bitmapHeight: " + bitmapHeight);

                newOffset = xOffset * (renderScreenWidth - bitmapWidth);

                GLES20.glDeleteTextures(1, textureNames, texture);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[texture]);
                Log.i(TAG, "Binding " + textureNames[texture]);

                // Set filtering
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

                // Set wrapping mode
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, localBitmap, 0);

                if (AppSettings.useEffects()) {
                    toEffect = true;
                }

                if (AppSettings.useFade() && fade) {
                    toFade = fade;
                    setRendererMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                }
                else if (texture == 1) {
                    textureNames[0] = textureNames[1];
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
                    onDrawFrame(null);
                }

            }



            protected Bitmap scaleBitmap(Bitmap bitmap) {
                int bitWidth = bitmap.getWidth();
                int bitHeight = bitmap.getHeight();
                float scaleWidth = ((float) screenWidth) / bitWidth;
                float scaleHeight = ((float) screenHeight) / bitHeight;

                if (bitWidth * scaleWidth > maxTextureSize[0] ||
                        bitWidth * scaleHeight > maxTextureSize[0] ||
                        bitHeight * scaleWidth > maxTextureSize[0] ||
                        bitHeight * scaleHeight > maxTextureSize[0]) {

                    int ratio = maxTextureSize[0] / screenHeight;
                    int scaledWidth = bitHeight * ratio;
                    if (scaledWidth > bitWidth || scaledWidth == 0) {
                        scaledWidth = bitWidth;
                    }

                    bitmap = Bitmap.createBitmap(bitmap, (bitWidth / 2) - (scaledWidth / 2), 0, scaledWidth, bitHeight);

                    bitWidth = bitmap.getWidth();
                    bitHeight = bitmap.getHeight();
                    scaleWidth = ((float) screenWidth) / bitWidth;
                    scaleHeight = ((float) screenHeight) / bitHeight;
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

            /**
             * Called when the engine is destroyed. Do any necessary clean up because
             * at this point your renderer instance is now done for.
             */
            public void release() {
                Log.i(TAG, "release");
                localBitmap.recycle();
            }

            private void initEffects() {

                effectFactory = effectContext.getFactory();

                if (AppSettings.getBrightnessValue() > 0) {
                    effect = effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
//                    effect.setParameter("brightness", ((float) AppSettings.getBrightnessValue()) - 0.5f);
                    Log.i(TAG, "Applied image effects");
                }

                Log.i(TAG, "bitWidth: " + Math.round(bitmapWidth));
            }

        }

    }
}
