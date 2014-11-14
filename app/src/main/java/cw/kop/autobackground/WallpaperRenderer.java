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
import android.opengl.GLUtils;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/13/2014.
 */

class WallpaperRenderer implements GLSurfaceView.Renderer {

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

    private Context serviceContext;

    private int[] maxTextureSize = new int[] {0};
    private float bitmapWidth = 0;
    private float bitmapHeight = 0;
    private float oldBitmapWidth = 0;
    private float oldBitmapHeight = 0;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float newOffsetX = 0f;
    private float newOffsetY = 0f;
    private float rawOffsetX = 0f;
    private float scaleFactor;

    private boolean animated = false;
    private float animationModifierX = 0.0f;
    private float animationModifierY = 0.0f;
    private float animationX = 0.0f;
    private float animationY = 0.0f;

    private boolean isPlayingMusic;
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

    private Callback callback;
    private static WallpaperRenderer instance;
    private volatile boolean loadNext = false;

    private OvershootInterpolator horizontalOvershootInterpolator;
    private OvershootInterpolator verticalOvershootInterpolator;
    private AccelerateInterpolator accelerateInterpolator;
    private DecelerateInterpolator decelerateInterpolator;
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

    public static WallpaperRenderer getInstance(Context context, Callback callback) {
        if (instance == null) {
            instance = new WallpaperRenderer(context.getApplicationContext(), callback);
        }

        return instance;
    }

    private WallpaperRenderer(Context context, Callback callback) {
        serviceContext = context;
        this.callback = callback;
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

        if (loadNext) {
            loadNext = false;
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

                    animationX += animationFactor * ((AppSettings.scaleAnimationSpeed()) ?
                            (animationModifierX / scaleFactor) :
                            animationModifierX);
                    offsetX = animationX;
                    newOffsetX -= animationX;
                }

                if (AppSettings.useVerticalAnimation() && bitmapHeight - (renderScreenHeight / scaleFactor) > AppSettings.getAnimationSafety()) {
                    animationY += ((AppSettings.scaleAnimationSpeed()) ?
                            (animationModifierY / scaleFactor) :
                            animationModifierY);
                    offsetY = animationY;
                    newOffsetY -= animationY;
                }

            }

            calculateBounds();

            android.opengl.Matrix.orthoM(matrixProjection,
                    0,
                    0,
                    renderScreenWidth / scaleFactor,
                    0,
                    renderScreenHeight / scaleFactor,
                    0,
                    10f);

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

            android.opengl.Matrix.orthoM(matrixProjection,
                    0,
                    0,
                    renderScreenWidth / scaleFactor,
                    0,
                    renderScreenHeight / scaleFactor,
                    0,
                    10f);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
            android.opengl.Matrix.setIdentityM(transMatrix, 0);
            android.opengl.Matrix.translateM(transMatrix, 0, offsetX, offsetY, 0f);
            renderImage();

            callback.resetMode();
        }
        else {

            if (animated) {
                if (AppSettings.useAnimation() && bitmapWidth - (renderScreenWidth / scaleFactor) > AppSettings.getAnimationSafety()) {
                    animationX += ((AppSettings.scaleAnimationSpeed()) ?
                            (animationModifierX / scaleFactor) :
                            animationModifierX);
                    offsetX = animationX;
                    if (newOffsetX < 0 && newOffsetX > (-bitmapWidth + renderScreenWidth)) {
                        newOffsetX -= ((AppSettings.scaleAnimationSpeed()) ?
                                (animationModifierX / scaleFactor) :
                                animationModifierX);
                    }
                }

                if (AppSettings.useVerticalAnimation() && bitmapHeight - (renderScreenHeight / scaleFactor) > AppSettings.getAnimationSafety()) {
                    animationY += ((AppSettings.scaleAnimationSpeed()) ?
                            (animationModifierY / scaleFactor) :
                            animationModifierY);
                    offsetY = animationY;
                    if (newOffsetY < 0 && newOffsetY > (-bitmapHeight + renderScreenHeight)) {
                        newOffsetY -= ((AppSettings.scaleAnimationSpeed()) ?
                                (animationModifierY / scaleFactor) :
                                animationModifierY);
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
                        transitionNewOffsetX + renderScreenWidth - (renderScreenWidth * horizontalOvershootInterpolator.getInterpolation(
                                1.0f - timeRatio)) :
                        transitionNewOffsetX - renderScreenWidth + (renderScreenWidth * horizontalOvershootInterpolator.getInterpolation(
                                1.0f - timeRatio)));
            }

            if (AppSettings.useVerticalOvershoot()) {
                verticalOvershootInterpolator = new OvershootInterpolator(AppSettings.getVerticalOvershootIntensity());
                transitionNewOffsetY = (AppSettings.reverseVerticalOvershoot() ?
                        transitionNewOffsetY + renderScreenHeight - (renderScreenHeight * verticalOvershootInterpolator.getInterpolation(
                                1.0f - timeRatio)) :
                        transitionNewOffsetY - renderScreenHeight + (renderScreenHeight * verticalOvershootInterpolator.getInterpolation(
                                1.0f - timeRatio)));

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

            renderTransitionTexture(oldBitmapWidth,
                    oldBitmapHeight,
                    1,
                    transitionOldOffsetX,
                    transitionOldOffsetY,
                    transitionOldAngle,
                    transitionOldScaleFactor);

            if (AppSettings.useFade()) {
                fadeInAlpha = 1.0f - timeRatio;
                GLES20.glUniform1f(mAlphaHandle, fadeInAlpha);
            }

            renderTransitionTexture(bitmapWidth,
                    bitmapHeight,
                    0,
                    transitionNewOffsetX,
                    transitionNewOffsetY,
                    transitionNewAngle,
                    transitionNewScaleFactor);
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
        android.opengl.Matrix.orthoM(matrixProjection,
                0,
                0,
                renderScreenWidth / scale,
                0,
                renderScreenHeight / scale,
                0,
                10f);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[texture]);
        android.opengl.Matrix.setIdentityM(transMatrix, 0);
        android.opengl.Matrix.translateM(transMatrix,
                0,
                renderScreenWidth / scale / 2,
                renderScreenHeight / scale / 2,
                0);
        android.opengl.Matrix.rotateM(transMatrix, 0, angle, 0.0f, 0.0f, 1.0f);
        android.opengl.Matrix.translateM(transMatrix,
                0,
                -renderScreenWidth / scale / 2,
                -renderScreenHeight / scale / 2,
                0);
        android.opengl.Matrix.translateM(transMatrix, 0, x, y, 0);

        renderImage();
    }

    private void renderImage() {

        // get handle to vertex shader's vPosition member
        int mPositionHandle = GLES20.glGetAttribLocation(program, "vPosition");

        // Enable generic vertex attribute array
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle,
                3,
                GLES20.GL_FLOAT,
                false,
                0,
                vertexBuffer);

        // Get handle to texture coordinates location
        int mTexCoordLoc = GLES20.glGetAttribLocation(program, "a_texCoord");

        // Enable generic vertex attribute array
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);

        // Prepare the texturecoordinates
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

        // Get handle to shape's transformation matrix
        int mtrxhandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

        android.opengl.Matrix.multiplyMM(matrixProjectionAndView,
                0,
                matrixView,
                0,
                transMatrix,
                0);
        android.opengl.Matrix.multiplyMM(matrixProjectionAndView,
                0,
                matrixProjection,
                0,
                transMatrix,
                0);

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, matrixProjectionAndView, 0);

        // Draw the container
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                indices.length,
                GLES20.GL_UNSIGNED_SHORT,
                drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        Log.i(TAG, "Renderer onSurfaceChanged");

        if (AppSettings.preserveContext()) {
            callback.setPreserveContext(true);
        }
        else {
            callback.setPreserveContext(false);
            contextInitialized = false;
            loadCurrent = true;
        }

        if (bitmapHeight == 0) {
            callback.loadNext();
        }

        if (width != renderScreenWidth) {
            GLES20.glViewport(0, 0, width, height);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            callback.loadCurrent();
        }

        if (loadCurrent) {
            callback.loadCurrent();
            loadCurrent = false;
        }

        if (isPlayingMusic) {
            callback.loadMusic();
            isPlayingMusic = false;
        }

        renderScreenWidth = width;
        renderScreenHeight = height;

        for (int i = 0; i < 16; i++) {
            matrixProjection[i] = 0.0f;
            matrixView[i] = 0.0f;
            matrixProjectionAndView[i] = 0.0f;
        }

        android.opengl.Matrix.orthoM(matrixProjection,
                0,
                0f,
                renderScreenWidth / scaleFactor,
                0.0f,
                renderScreenHeight / scaleFactor,
                0,
                10f);

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

        // Calculate the projection and view transformation
        android.opengl.Matrix.multiplyMM(matrixProjectionAndView,
                0,
                matrixProjection,
                0,
                matrixView,
                0);

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        int vertexShader = GLShaders.loadShader(GLES20.GL_VERTEX_SHADER,
                GLShaders.vertexShaderImage);
        int fragmentShader = GLShaders.loadShader(GLES20.GL_FRAGMENT_SHADER,
                GLShaders.fragmentShaderImage);

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

            uvs = new float[] {
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
        callback.requestRender();
    }

    public void setupContainer(float width, float height) {

        vertices = new float[] {
                0.0f, height, 0.0f,
                0.0f, 0.0f, 0.0f,
                width, 0.0f, 0.0f,
                width, height, 0.0f
        };

        indices = new short[] {0, 1, 2, 0, 2, 3};

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
            Log.i(TAG,
                    "currentBitmapFile loaded: " + FileHandler.getCurrentBitmapFile().getName());

//            Log.i(TAG,
//                    "startWidth: " + bitmap.getWidth() + " startHeight: " + bitmap.getHeight());

//            if (AppSettings.useScale()) {
//                if (bitmap.getWidth() < renderScreenWidth ||
//                        bitmap.getWidth() > maxTextureSize[0] ||
//                        bitmap.getHeight() < renderScreenHeight ||
//                        bitmap.getHeight() > maxTextureSize[0]) {
//                    nextImage = scaleBitmap(bitmap);
//                }
//            }
//            else {
//                nextImage = scaleBitmap(bitmap);
//            }

//            Log.i(TAG,
//                    "scaledWidth: " + bitmap.getWidth() + " scaledHeight: " + bitmap.getHeight());
            loadNext = true;
        }
        catch (IllegalArgumentException e) {
            Log.i(TAG, "Error loading next image");
        }

        Log.i(TAG, "Set bitmap");

    }

    private void loadTexture(Bitmap bitmap) {

        if (bitmap == null) {
            return;
        }

        try {

            Log.i(TAG, "bitmap width: " + bitmap.getWidth() + " bitmap height: " + bitmap.getHeight());

            int storeId = textureNames[0];
            textureNames[0] = textureNames[1];
            textureNames[1] = storeId;

            setupContainer(bitmapWidth, bitmapHeight);
            
            GLES20.glDeleteTextures(1, textureNames, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            checkGLError("Bind textureNames[0]");
            Log.i(TAG, "Bind texture: " + textureNames[0]);

            GLES20.glDeleteTextures(1, textureNames, 2);

            if (AppSettings.useEffects()) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[2]);

                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER,
                        GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER,
                        GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_S,
                        GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_WRAP_T,
                        GLES20.GL_CLAMP_TO_EDGE);

                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                checkGLError("Bind textureNames[2]");
                Log.i(TAG, "Bind texture: " + textureNames[2]);
                toEffect = true;
            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
            Log.i(TAG, "Render texture: " + textureNames[0]);

            if (AppSettings.getTransitionTime() > 0) {
                useTransition = true;
                transitionTime = System.currentTimeMillis() + AppSettings.getTransitionTime();
                callback.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            }
            else {
                GLES20.glDeleteTextures(1, textureNames, 1);
            }

            oldBitmapWidth = bitmapWidth;
            oldBitmapHeight = bitmapHeight;
            bitmapWidth = bitmap.getWidth();
            bitmapHeight = bitmap.getHeight();

            newOffsetX = rawOffsetX * (renderScreenWidth - bitmapWidth);
            newOffsetY = -(bitmapHeight - renderScreenHeight) / 2;

            bitmap.recycle();
            bitmap = null;
            System.gc();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            Log.i(TAG, "Error loading next image");
        }
    }

    private void checkGLError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("MyApp", op + ": glError " + error);
        }
    }

    public void loadNext() {

        BitmapFactory.Options options = new BitmapFactory.Options();
        if (!AppSettings.useHighQuality()) {
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inJustDecodeBounds = true;
        }

        try {

            File nextImage = FileHandler.getNextImage();
            if (nextImage == null) {
                return;
            }

            Bitmap checkBitmap = BitmapFactory.decodeFile(nextImage.getAbsolutePath(), options);

            if (!AppSettings.useFullResolution()) {

                int sampleSize = 1;
                if (options.outHeight > renderScreenHeight || options.outWidth > renderScreenWidth) {

                    final int halfHeight = options.outHeight / 2;
                    final int halfWidth = options.outWidth / 2;
                    while ((halfHeight / sampleSize) > renderScreenHeight && (halfWidth / sampleSize) > renderScreenWidth) {
                        sampleSize *= 2;
                    }
                }
                options.inSampleSize = sampleSize > 1 ? sampleSize / 2 : sampleSize;
            }
            options.inJustDecodeBounds = false;

            checkBitmap = BitmapFactory.decodeFile(nextImage.getAbsolutePath(), options);

            loadTexture(scaleBitmap(checkBitmap));

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Bitmap scaleBitmap(Bitmap bitmap) {
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

                bitmap = Bitmap.createBitmap(bitmap,
                        (bitWidth / 2) - (scaledWidth / 2),
                        0,
                        scaledWidth,
                        bitHeight);

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
                bitmap = Bitmap.createBitmap(bitmap,
                        0,
                        0,
                        maxTextureSize[0],
                        bitmap.getHeight());
            }
            if (bitmap.getHeight() > maxTextureSize[0]) {
                bitmap = Bitmap.createBitmap(bitmap,
                        0,
                        0,
                        bitmap.getWidth(),
                        maxTextureSize[0]);
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

    private void applyEffect(Effect setEffect, int texture, String name,
            String description) {

        setEffect.setUpdateListener(effectUpdateListener);

        GLES20.glDeleteTextures(1, textureNames, texture);
        setEffect.apply(textureNames[2],
                Math.round(renderScreenWidth),
                Math.round(renderScreenHeight),
                textureNames[texture]);
        setEffect.release();

        GLES20.glDeleteTextures(1, textureNames, 2);
        Effect resetEffect = effectFactory.createEffect(EffectFactory.EFFECT_TEMPERATURE);
        resetEffect.setParameter("scale", 0.5f);
        resetEffect.apply(textureNames[texture],
                Math.round(renderScreenWidth),
                Math.round(renderScreenHeight),
                textureNames[2]);
        resetEffect.release();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[texture]);

        toastEffect(name, description);
        Log.i(TAG, "Effect applied: " + name + "\n" + description);

    }

    private void applyManualEffects(int texture) {

        Effect effect;

        if (AppSettings.getAutoFixEffect() > 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_AUTOFIX)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_AUTOFIX);
            effect.setParameter("scale", AppSettings.getAutoFixEffect());
            applyEffect(effect,
                    texture,
                    "Auto Fix",
                    "Value:" + AppSettings.getAutoFixEffect());
        }

        if (AppSettings.getBrightnessEffect() != 1.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_BRIGHTNESS)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_BRIGHTNESS);
            effect.setParameter("brightness", AppSettings.getBrightnessEffect());
            applyEffect(effect,
                    texture,
                    "Brightness",
                    "Value:" + AppSettings.getBrightnessEffect());
        }

        if (AppSettings.getContrastEffect() != 1.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_CONTRAST)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_CONTRAST);
            effect.setParameter("contrast", AppSettings.getContrastEffect());
            applyEffect(effect,
                    texture,
                    "Contrast",
                    "Value:" + AppSettings.getContrastEffect());
        }

        if (AppSettings.getCrossProcessEffect() && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_CROSSPROCESS)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_CROSSPROCESS);
            applyEffect(effect, texture, "Cross Process", "");
        }

        if (AppSettings.getDocumentaryEffect() && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_DOCUMENTARY)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_DOCUMENTARY);
            applyEffect(effect, texture, "Documentary", "");
        }

        if (AppSettings.getDuotoneEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_DUOTONE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
            effect.setParameter("first_color", AppSettings.getDuotoneColor(1));
            effect.setParameter("second_color", AppSettings.getDuotoneColor(2));
            applyEffect(effect,
                    texture,
                    "Dual Tone",
                    "\nColor 1: " + AppSettings.getDuotoneColor(1) + "\nColor 2: " + AppSettings.getDuotoneColor(
                            2));
        }

        if (AppSettings.getFillLightEffect() > 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_FILLLIGHT)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_FILLLIGHT);
            effect.setParameter("strength", AppSettings.getFillLightEffect());
            applyEffect(effect,
                    texture,
                    "Fill Light",
                    "Value:" + AppSettings.getFillLightEffect());
        }

        if (AppSettings.getFisheyeEffect() > 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_FISHEYE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_FISHEYE);
            effect.setParameter("scale", AppSettings.getFisheyeEffect());
            applyEffect(effect,
                    texture,
                    "Fisheye",
                    "Value:" + AppSettings.getFisheyeEffect());
        }

        if (AppSettings.getGrainEffect() > 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_GRAIN)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_GRAIN);
            effect.setParameter("strength", AppSettings.getGrainEffect());
            applyEffect(effect, texture, "Grain", "Value:" + AppSettings.getGrainEffect());
        }

        if (AppSettings.getGrayscaleEffect() && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_GRAYSCALE)) {
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

        if (AppSettings.getPosterizeEffect() && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_POSTERIZE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_POSTERIZE);
            applyEffect(effect, texture, "Posterize", "");
        }

        if (AppSettings.getSaturateEffect() != 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_SATURATE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_SATURATE);
            effect.setParameter("scale", AppSettings.getSaturateEffect());
            applyEffect(effect,
                    texture,
                    "Saturate",
                    "Value:" + AppSettings.getSaturateEffect());
        }

        if (AppSettings.getSepiaEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_SEPIA)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_SEPIA);
            applyEffect(effect, texture, "Sepia", "Value:");
        }

        if (AppSettings.getSharpenEffect() > 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_SHARPEN)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_SHARPEN);
            effect.setParameter("scale", AppSettings.getSharpenEffect());
            applyEffect(effect,
                    texture,
                    "Sharpen",
                    "Value:" + AppSettings.getSharpenEffect());
        }

        if (AppSettings.getTemperatureEffect() != 0.5f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_TEMPERATURE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_TEMPERATURE);
            effect.setParameter("scale", AppSettings.getTemperatureEffect());
            applyEffect(effect,
                    texture,
                    "Temperature",
                    "Value:" + AppSettings.getTemperatureEffect());
        }

        if (AppSettings.getVignetteEffect() > 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_VIGNETTE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_VIGNETTE);
            effect.setParameter("scale", AppSettings.getVignetteEffect());
            applyEffect(effect,
                    texture,
                    "Vignette",
                    "Value:" + AppSettings.getVignetteEffect());
        }
    }

    private void applyRandomEffects(String randomEffect, int texture) {

        Random random = new Random();
        Effect effect;

        switch (randomEffect) {
            case "Completely Random": {
                String[] allEffectsList = serviceContext.getResources().getStringArray(
                        R.array.effects_list);
                String[] allEffectParameters = serviceContext.getResources().getStringArray(
                        R.array.effects_list_parameters);

                ArrayList<String> usableEffectsList = new ArrayList<>();
                ArrayList<String> usableEffectsParameters = new ArrayList<>();

                for (int i = 0; i < allEffectsList.length; i++) {
                    if (EffectFactory.isEffectSupported(allEffectsList[i])) {
                        usableEffectsList.add(allEffectsList[i]);
                        usableEffectsParameters.add(allEffectParameters[i]);
                    }
                }

                int index = random.nextInt(usableEffectsList.size());
                String effectName = usableEffectsList.get(index);
                String parameter = usableEffectsParameters.get(index);
                float value = 0.0f;

                effect = effectFactory.createEffect(effectName);
                if (usableEffectsList.get(index).equals(
                        "android.media.effect.effects.SaturateEffect")) {
                    value = (random.nextFloat() * 0.6f) - 0.3f;
                }
                else if (usableEffectsList.get(index).equals(
                        "android.media.effect.effects.ColorTemperatureEffect")) {
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
                    applyEffect(effect,
                            texture,
                            effectName.substring(effectName.indexOf("effects.") + 8),
                            ((value != 0.0f) ? "Value:" + value : ""));
                }
                break;
            }
            case "Filter Effects": {
                String[] filtersList = serviceContext.getResources().getStringArray(
                        R.array.effects_filters_list);

                int index = random.nextInt(filtersList.length);

                effect = effectFactory.createEffect(filtersList[index]);
                applyEffect(effect,
                        texture,
                        filtersList[index].substring(filtersList[index].indexOf(
                                "effects.") + 8),
                        "");
                break;
            }
            case "Dual Tone Random": {

                int firstColor = Color.argb(255,
                        random.nextInt(80),
                        random.nextInt(80),
                        random.nextInt(80));
                int secondColor = Color.argb(255,
                        random.nextInt(100) + 75,
                        random.nextInt(100) + 75,
                        random.nextInt(100) + 75);

                effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                effect.setParameter("first_color", firstColor);
                effect.setParameter("second_color", secondColor);
                applyEffect(effect,
                        texture,
                        randomEffect,
                        "\n" + firstColor + "\n" + secondColor);

                break;
            }
            case "Dual Tone Rainbow": {

                ArrayList<String> colorsList = (ArrayList<String>) Arrays.asList(
                        serviceContext.getResources().getStringArray(R.array.effects_color_list));

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
                applyEffect(effect,
                        texture,
                        randomEffect,
                        "\n" + firstColor + "\n" + secondColor);

                break;
            }
            case "Dual Tone Warm": {

                int firstColor = Color.argb(255,
                        random.nextInt(40) + 40,
                        random.nextInt(40),
                        random.nextInt(40));
                int secondColor = Color.argb(255,
                        random.nextInt(80) + 150,
                        random.nextInt(80) + 125,
                        random.nextInt(80) + 125);

                if (AppSettings.useDuotoneGray()) {
                    int grayValue = random.nextInt(50);
                    firstColor = Color.argb(255, grayValue, grayValue, grayValue);
                    Log.i(TAG, "Duotone gray");
                }

                effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                effect.setParameter("first_color", firstColor);
                effect.setParameter("second_color", secondColor);
                applyEffect(effect,
                        texture,
                        randomEffect,
                        "\n" + firstColor + "\n" + secondColor);

                break;
            }
            case "Dual Tone Cool": {

                int firstColor = Color.argb(255,
                        random.nextInt(40),
                        random.nextInt(40) + 40,
                        random.nextInt(40) + 40);
                int secondColor = Color.argb(255,
                        random.nextInt(80) + 125,
                        random.nextInt(80) + 150,
                        random.nextInt(80) + 150);

                if (AppSettings.useDuotoneGray()) {
                    int grayValue = random.nextInt(50);
                    firstColor = Color.argb(255, grayValue, grayValue, grayValue);
                    Log.i(TAG, "Duotone gray");
                }

                effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                effect.setParameter("first_color", firstColor);
                effect.setParameter("second_color", secondColor);
                applyEffect(effect,
                        texture,
                        randomEffect,
                        "\n" + firstColor + "\n" + secondColor);

                break;
            }
        }
    }

    public void setScaleFactor(float factor) {

        scaleFactor *= factor;

        float minScaleFactor;

        if (AppSettings.extendScale() || !AppSettings.fillImages()) {
            minScaleFactor = renderScreenWidth < renderScreenHeight
                    ?
                    renderScreenWidth / bitmapWidth
                    :
                    renderScreenHeight / bitmapHeight;
        }
        else {
            minScaleFactor = renderScreenWidth > renderScreenHeight
                    ?
                    renderScreenWidth / bitmapWidth
                    :
                    renderScreenHeight / bitmapHeight;
        }

        scaleFactor = Math.max(
                minScaleFactor,
                Math.min(scaleFactor,
                        5.0f));
    }


    private void toastEffect(final String effectName, final String effectValue) {
        if (AppSettings.useToast() && AppSettings.useToastEffects()) {
            Toast.makeText(serviceContext,
                    "Effect applied: " + effectName + " " + effectValue,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    public void setLoadCurrent(boolean load) {
        loadCurrent = load;
    }

    public void setTargetFrameTime(long newFrameTime) {
        this.targetFrameTime = newFrameTime;
    }

    public void setAnimationModifierX(float modifierX) {
        animationModifierX = modifierX;
    }

    public void setAnimationModifierY(float modifierY) {
        animationModifierY = modifierY;
    }

    public float getScreenHeight() {
        return renderScreenHeight;
    }

    public float getScreenWidth() {
        return renderScreenWidth;
    }

    public void release() {
        Log.i(TAG, "release");
    }

    public boolean isAnimated() {
        return animated;
    }

    public void setPlayingMusic(boolean playingMusic) {
        isPlayingMusic = playingMusic;
    }

    public interface Callback {

        void resetMode();
        void setRenderMode(int mode);
        void setPreserveContext(boolean preserveContext);
        void loadCurrent();
        void loadPrevious();
        void loadNext();
        void loadMusic();
        void requestRender();

    }

}
