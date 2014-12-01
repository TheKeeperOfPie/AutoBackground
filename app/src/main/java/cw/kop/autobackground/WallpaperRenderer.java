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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.media.effect.EffectUpdateListener;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/13/2014.
 */

class WallpaperRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "Renderer";
    private EffectUpdateListener effectUpdateListener = new EffectUpdateListener() {
        @Override
        public void onEffectUpdated(Effect effect, Object info) {

            Log.i(TAG, "Effect info: " + info.toString());

            if (AppSettings.useToast()) {
                Toast.makeText(serviceContext,
                        "Effect info: " + info.toString(),
                        Toast.LENGTH_SHORT).show();
            }

        }
    };
    private static WallpaperRenderer instance;
    private static boolean isPlayingMusic;
    private float[] matrixView = new float[16];
    private float renderScreenWidth = 1;
    private float renderScreenHeight = 1;
    private long startTime;
    private long endTime;
    private long frameTime;
    private long targetFrameTime;
    private Context serviceContext;
    private float rawOffsetX = 0f;
    private boolean loadCurrent = false;
    private Callback callback;
    private volatile List<RenderImage> renderImagesTop;
    private volatile List<RenderImage> renderImagesBottom;
    private boolean isLastTop = false;
    private Handler handler;
    private RenderImage.EventListener eventListener = new RenderImage.EventListener() {
        @Override
        public void removeSelf(RenderImage image) {
            renderImagesTop.remove(image);
            renderImagesBottom.remove(image);
            if (!(AppSettings.useAnimation() || AppSettings.useVerticalAnimation())) {
                callback.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            }
        }

        @Override
        public void doneLoading() {
            if (instance.renderImagesTop.size() > 1) {
                instance.renderImagesTop.get(0).startFinish();
                Log.i(TAG, "Start finish top");
            }
            if (instance.renderImagesBottom.size() > 1) {
                instance.renderImagesBottom.get(0).startFinish();
                Log.i(TAG, "Start finish bottom");
            }
        }

        public Context getContext() {
            return serviceContext;
        }

        @Override
        public void toastEffect(final String effectName, final String effectValue) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(serviceContext,
                            "Effect applied: " + effectName + " " + effectValue,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void requestRender() {
            callback.requestRender();
        }
    };

    private WallpaperRenderer(Context context, Callback callback) {
        serviceContext = context;
        this.callback = callback;
        startTime = System.currentTimeMillis();
        renderImagesTop = new CopyOnWriteArrayList<>();
        renderImagesBottom = new CopyOnWriteArrayList<>();
        handler = new Handler(serviceContext.getMainLooper());
    }

    public static WallpaperRenderer getInstance(Context context, Callback callback) {
        if (instance == null) {
            instance = new WallpaperRenderer(context.getApplicationContext(), callback);
        }

        return instance;
    }

    @Override
    public void onDrawFrame(GL10 gl) {

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

        if (renderImagesTop.size() > 2) {
            renderImagesTop.get(0).finishImmediately();
        }
        if (renderImagesBottom.size() > 2) {
            renderImagesBottom.get(0).finishImmediately();
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (isLastTop) {
            for (RenderImage image : renderImagesBottom) {
                image.renderImage();
            }
            for (RenderImage image : renderImagesTop) {
                image.renderImage();
            }
        }
        else {
            for (RenderImage image : renderImagesTop) {
                image.renderImage();
            }
            for (RenderImage image : renderImagesBottom) {
                image.renderImage();
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        Log.i(TAG, "Renderer onSurfaceChanged");

        if (width != renderScreenWidth) {
            GLES20.glViewport(0, 0, width, height);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            for (RenderImage image : renderImagesTop) {
                image.setDimensions(width, height);
            }
            for (RenderImage image : renderImagesBottom) {
                image.setDimensions(width, height);
            }
        }

        if (loadCurrent) {
            callback.loadCurrent();
            loadCurrent = false;
        }

        renderScreenWidth = width;
        renderScreenHeight = height;

        for (RenderImage image : renderImagesTop) {
            image.resetMatrices();
        }
        for (RenderImage image : renderImagesBottom) {
            image.resetMatrices();
        }

        for (int i = 0; i < 16; i++) {
            matrixView[i] = 0.0f;
        }

        // Set the camera position (View matrix)
        android.opengl.Matrix.setLookAtM(matrixView,
                0,
                0f,
                0f,
                1f,
                0f,
                0f,
                0.0f,
                0f,
                1f,
                0.0f);

        if (renderImagesTop.size() == 0) {
            File nextImage = FileHandler.getNextImage();
            if (nextImage != null && nextImage.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(nextImage.getAbsolutePath());
                if (bitmap != null) {
                    if (AppSettings.useDoubleImage()) {
                        renderImagesTop.add(getNewImage(scaleBitmap(bitmap,
                                renderScreenWidth,
                                renderScreenHeight), 0.0f, 1.0f, 0.5f, 1.0f));
                    }
                    else {
                        renderImagesTop.add(getNewImage(scaleBitmap(bitmap,
                                renderScreenWidth,
                                renderScreenHeight), 0.0f, 1.0f, 0.0f, 1.0f));
                    }
                }
            }
        }

        if (renderImagesBottom.size() == 0 && AppSettings.useDoubleImage()) {
            File nextImage = FileHandler.getNextImage();
            if (nextImage != null && nextImage.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(nextImage.getAbsolutePath());
                if (bitmap != null) {
                    renderImagesBottom.add(getNewImage(scaleBitmap(bitmap,
                            renderScreenWidth,
                            renderScreenHeight), 0.0f, 1.0f, 0.0f, 0.5f));
                }
            }
        }

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        RenderImage.setupRenderValues();

        Log.i(TAG, "onSurfaceCreated");
    }

    private RenderImage getNewImage(Bitmap bitmap,
            float minRatioX,
            float maxRatioX,
            float minRatioY,
            float maxRatioY) {

        RenderImage image = new RenderImage(bitmap,
                getNewTextureId(),
                eventListener,
                minRatioX,
                maxRatioX,
                minRatioY,
                maxRatioY);
        image.setAnimated(AppSettings.useAnimation() || AppSettings.useVerticalAnimation());
        image.setAnimationModifierX(AppSettings.getAnimationSpeed() / 10f);
        image.setAnimationModifierY(AppSettings.getVerticalAnimationSpeed() / 10f);
        frameTime = 1000 / AppSettings.getAnimationFrameRate();
        return image;
    }

    public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep,
            int xPixels, int yPixels) {
        rawOffsetX = AppSettings.forceParallax() ? 1.0f - xOffset : xOffset;
        for (RenderImage image : renderImagesTop) {
            image.onOffsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels);
        }
        for (RenderImage image : renderImagesBottom) {
            image.onOffsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels);
        }
    }

    public void onSwipe(float xMovement, float yMovement, float positionY) {

        if (positionY > renderScreenHeight / 2 && renderImagesBottom.size() > 0) {
            for (RenderImage image : renderImagesBottom) {
                image.onSwipe(xMovement, yMovement);
            }
        }
        else if (renderImagesTop.size() > 0) {
            for (RenderImage image : renderImagesTop) {
                image.onSwipe(xMovement, yMovement);
            }
        }
    }

    public void setScaleFactor(float factor, float positionY) {

        if (positionY > renderScreenHeight / 2 && renderImagesBottom.size() > 0) {
            for (RenderImage image : renderImagesBottom) {
                image.setScaleFactor(factor);
            }
        }
        else if (renderImagesTop.size() > 0) {
            for (RenderImage image : renderImagesTop) {
                image.setScaleFactor(factor);
            }
        }
    }

    public void resetPosition() {
        for (RenderImage image : renderImagesTop) {
            image.setScaleFactor(1.0f);
            image.setRawOffsetX(rawOffsetX);
            image.setAnimated(AppSettings.useAnimation() || AppSettings.useVerticalAnimation());
        }
        for (RenderImage image : renderImagesBottom) {
            image.setScaleFactor(1.0f);
            image.setRawOffsetX(rawOffsetX);
            image.setAnimated(AppSettings.useAnimation() || AppSettings.useVerticalAnimation());
        }
        callback.requestRender();
    }

    private int getNewTextureId() {

        int[] tempIntArray = new int[1];
        GLES20.glGenTextures(1, tempIntArray, 0);
        return tempIntArray[0];

    }

    public void loadNext(File nextImage) {
        if (isLastTop) {
            loadNext(nextImage, renderScreenHeight);
        }
        else {
            loadNext(nextImage, 0);
        }
    }

    public void loadNext(File nextImage, float positionY) {

        if (nextImage == null) {
            return;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        if (!AppSettings.useHighQuality()) {
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inJustDecodeBounds = true;
        }

        try {

            long start = System.currentTimeMillis();
            Bitmap bitmap = BitmapFactory.decodeFile(nextImage.getAbsolutePath(), options);
            long finish = System.currentTimeMillis();

            if (bitmap == null) {
                return;
            }

            Log.d(TAG, "Time to load bitmap: " + (finish - start));

            RenderImage newImage;
            if (AppSettings.useDoubleImage()) {
                if (positionY < renderScreenHeight / 2) {
                    newImage = getNewImage(scaleBitmap(bitmap,
                            renderScreenWidth,
                            renderScreenHeight / 2), 0.0f, 1.0f, 0.5f, 1.0f);
                }
                else {
                    newImage = getNewImage(scaleBitmap(bitmap,
                            renderScreenWidth,
                            renderScreenHeight / 2), 0.0f, 1.0f, 0.0f, 0.5f);
                }
            }
            else {
                newImage = getNewImage(scaleBitmap(bitmap,
                        renderScreenWidth,
                        renderScreenHeight), 0.0f, 1.0f, 0.0f, 1.0f);
            }
            newImage.setRawOffsetX(rawOffsetX);
            newImage.setDimensions(renderScreenWidth, renderScreenHeight);

            callback.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

            if (AppSettings.useDoubleImage() && positionY > renderScreenHeight / 2) {
                renderImagesBottom.add(newImage);
                isLastTop = false;
            }
            else {
                renderImagesTop.add(newImage);
                isLastTop = true;
                if (!AppSettings.useDoubleImage()) {
                    if (renderImagesBottom.size() > 0) {
                        renderImagesBottom.get(0).startFinish();
                    }
                }
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Bitmap scaleBitmap(Bitmap bitmap, float targetWidth, float targetHeight) {
        if (!AppSettings.scaleImages()) {
            Log.i(TAG, "Not scaled");
            return bitmap;
        }

        int bitWidth = bitmap.getWidth();
        int bitHeight = bitmap.getHeight();

        int maxTextureSize = RenderImage.maxTextureSize[0];

        if (bitWidth > 0 && bitHeight > 0 && maxTextureSize > 0) {
            float scaleWidth = targetWidth / bitWidth;
            float scaleHeight = targetHeight / bitHeight;

            if (bitWidth * scaleWidth > maxTextureSize ||
                    bitWidth * scaleHeight > maxTextureSize ||
                    bitHeight * scaleWidth > maxTextureSize ||
                    bitHeight * scaleHeight > maxTextureSize) {

                float ratio = maxTextureSize / targetHeight;

                int scaledWidth = Math.round(bitHeight * ratio);
                if (scaledWidth > bitWidth || scaledWidth == 0) {
                    scaledWidth = bitWidth;
                }

                if (scaledWidth > maxTextureSize) {
                    scaledWidth = maxTextureSize;
                }

                bitmap = Bitmap.createBitmap(bitmap,
                        (bitWidth / 2) - (scaledWidth / 2),
                        0,
                        scaledWidth,
                        bitHeight);

                bitWidth = bitmap.getWidth();
                bitHeight = bitmap.getHeight();
                scaleWidth = targetWidth / bitWidth;
                scaleHeight = targetHeight / bitHeight;
            }

            Matrix matrix = new Matrix();
            if (scaleWidth > scaleHeight) {
                matrix.postScale(scaleWidth, scaleWidth);
            }
            else {
                matrix.postScale(scaleHeight, scaleHeight);
            }

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitWidth, bitHeight, matrix, false);

            if (bitmap.getWidth() > maxTextureSize) {
                bitmap = Bitmap.createBitmap(bitmap,
                        0,
                        0,
                        maxTextureSize,
                        bitmap.getHeight());
            }
            if (bitmap.getHeight() > maxTextureSize) {
                bitmap = Bitmap.createBitmap(bitmap,
                        0,
                        0,
                        bitmap.getWidth(),
                        maxTextureSize);
            }
        }

        return bitmap;
    }

    public void setLoadCurrent(boolean load) {
        loadCurrent = load;
    }

    public void setTargetFrameTime(long newFrameTime) {
        this.targetFrameTime = newFrameTime;
    }

    public void release() {
        Log.i(TAG, "release");
    }

    public void setPlayingMusic(boolean playingMusic) {
        isPlayingMusic = playingMusic;
    }

    public interface Callback {

        void setRenderMode(int mode);

        void loadCurrent();

        void requestRender();

    }

}
