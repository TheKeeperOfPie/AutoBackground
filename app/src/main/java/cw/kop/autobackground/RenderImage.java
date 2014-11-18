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

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.animation.OvershootInterpolator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/15/2014.
 */
public class RenderImage {

    public static final String TAG = RenderImage.class.getCanonicalName();

    private static int mPositionHandle;
    private static int mTexCoordLoc;
    private static int mtrxhandle;
    private static int mAlphaHandle;
    private static int program;
    private static int[] maxTextureSize = new int[1];
    private static float renderScreenWidth;
    private static float renderScreenHeight;
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
    private Bitmap imageBitmap;
    private boolean textureLoaded = false;
    private boolean animated = false;
    private int[] textureNames;
    private float scaleFactor = 1.0f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float rawOffsetX = -1f;
    private float saveOffsetX = 0f;
    private float bitmapWidth = 1f;
    private float bitmapHeight = 1f;
    private float animationModifierX = 0f;
    private float animationModifierY = 0f;
    private float angle = 0f;
    private float alpha = 0f;
    private boolean inStartTransition = true;
    private boolean inEndTransition = false;
    private long transitionEndtime = 0;
    private boolean finished = false;

    public RenderImage(Bitmap bitmap, int textureName) {
        setBitmap(bitmap);
        textureNames = new int[] {textureName};
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

        indices = new short[] {0, 1, 2, 0, 2, 3};

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);
    }

    public static void setupRenderValues() {
        int vertexShader = GLShaders.loadShader(GLES20.GL_VERTEX_SHADER,
                GLShaders.vertexShaderImage);
        int fragmentShader = GLShaders.loadShader(GLES20.GL_FRAGMENT_SHADER,
                GLShaders.fragmentShaderImage);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        mPositionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        mTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoords");
        mtrxhandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        mAlphaHandle = GLES20.glGetUniformLocation(program, "opacity");
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
    }

    public void setDimensions(float width, float height) {
        renderScreenWidth = width;
        renderScreenHeight = height;
    }

    public void resetMatrices() {
        for (int i = 0; i < 16; i++) {
            matrixProjection[i] = 0.0f;
            matrixView[i] = 0.0f;
            matrixProjectionAndView[i] = 0.0f;
        }
    }

    public void setBitmap(Bitmap bitmap) {
        imageBitmap = bitmap;
        textureLoaded = false;
    }

    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    public void setAnimationModifierX(float animationModifierX) {
        this.animationModifierX = animationModifierX;
    }

    public void setAnimationModifierY(float animationModifierY) {
        this.animationModifierY = animationModifierY;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep,
            int xPixels, int yPixels) {
        if (AppSettings.forceParallax()) {
            if (AppSettings.useDrag() || animated) {
                offsetX += (renderScreenWidth - bitmapWidth) * scaleFactor * (1.0f - xOffset - rawOffsetX);
            }
            else {
                offsetX = (renderScreenWidth - bitmapWidth) * scaleFactor * (1.0f - xOffset);
            }
            rawOffsetX = 1.0f - xOffset;
        }
        else {
            if (AppSettings.useDrag() || animated) {
                offsetX += (renderScreenWidth - bitmapWidth) * scaleFactor * (xOffset - rawOffsetX);
            }
            else {
                offsetX = (renderScreenWidth - bitmapWidth) * scaleFactor * (xOffset);
            }
            rawOffsetX = xOffset;
        }
    }

    public void onSwipe(float xMovement, float yMovement) {
//        if (!useTransition) {
            if (AppSettings.reverseDrag()) {
                if (bitmapWidth * scaleFactor < renderScreenWidth
                        || offsetX + xMovement > (-bitmapWidth + renderScreenWidth / scaleFactor) && offsetX + xMovement < 0) {
                    offsetX += xMovement;
                }
                if (bitmapHeight * scaleFactor < renderScreenHeight || offsetY - yMovement > (-bitmapHeight + renderScreenHeight / scaleFactor) && offsetY - yMovement < 0) {
                    offsetY -= yMovement;
                }
            }
            else {
                if (bitmapWidth * scaleFactor < renderScreenWidth
                        || offsetX - xMovement > (-bitmapWidth + renderScreenWidth / scaleFactor) && offsetX - xMovement < 0) {
                    offsetX -= xMovement;
                }
                if (bitmapHeight * scaleFactor < renderScreenHeight || offsetY + yMovement > (-bitmapHeight + renderScreenHeight / scaleFactor) && offsetY + yMovement < 0) {
                    offsetY += yMovement;
                }
            }
//        }
    }

    public void loadTexture() {
        if (imageBitmap == null || textureLoaded) {
            return;
        }

        try {

            Log.i(TAG,
                    "imageBitmap width: " + imageBitmap.getWidth() + " imageBitmap height: " + imageBitmap.getHeight());


            GLES20.glGenTextures(1, textureNames, 0);
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

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, imageBitmap, 0);
            bitmapWidth = imageBitmap.getWidth();
            bitmapHeight = imageBitmap.getHeight();

            if (rawOffsetX > -1) {
                offsetX = rawOffsetX * (renderScreenWidth - bitmapWidth);
                Log.i(TAG, "rawOffsetX: " + rawOffsetX);
                Log.i(TAG, "value: " + -(bitmapWidth - renderScreenWidth));
            }
            offsetY = -(bitmapHeight / renderScreenHeight) / 2;

            vertices = new float[] {
                    0.0f, bitmapHeight, 0.0f,
                    0.0f, 0.0f, 0.0f,
                    bitmapWidth, 0.0f, 0.0f,
                    bitmapWidth, bitmapHeight, 0.0f
            };

            // The vertex buffer.
            ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(vertices);
            vertexBuffer.position(0);
            checkGLError("textImage2D: ");
            textureLoaded = true;
            WallpaperRenderer.setIsLoadingTexture(false);
            imageBitmap.recycle();
            imageBitmap = null;
            System.gc();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            Log.i(TAG, "Error loading next image");
        }
        catch (NullPointerException e) {
            e.printStackTrace();
            Log.w(TAG, "Null on loading image error");
        }
    }

    public void setOffsets(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
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

    private void checkGLError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("MyApp", op + ": glError " + error);
        }
    }

    private void calculateBounds() {

        if (bitmapWidth * scaleFactor >= renderScreenWidth) {
            if (offsetX < (-bitmapWidth + renderScreenWidth / scaleFactor)) {
                animationModifierX = Math.abs(animationModifierX);
                offsetX = -bitmapWidth + renderScreenWidth / scaleFactor;

            }
            else if (offsetX > 0f) {
                animationModifierX = -Math.abs(animationModifierX);
                offsetX = 0f;
            }
        }

        if (bitmapHeight * scaleFactor >= renderScreenHeight) {
            if (offsetY < (-bitmapHeight + renderScreenHeight / scaleFactor)) {
                animationModifierY = Math.abs(animationModifierY);
                offsetY = -bitmapHeight + renderScreenHeight / scaleFactor;
            }
            else if (offsetY > 0f) {
                animationModifierY = -Math.abs(animationModifierY);
                offsetY = 0f;
            }
        }
//        Log.i(TAG, "offsetY: " + offsetY + " Value: " + ((-bitmapHeight + renderScreenHeight / scaleFactor)));
    }

    public int getTextureName() {
        return textureNames[0];
    }

    public void renderImage() {

        if (!textureLoaded) {
            loadTexture();
        }

        GLES20.glUseProgram(program);

        if (animated) {
            float safety = AppSettings.getAnimationSafety();
            if (AppSettings.useAnimation() && bitmapWidth - (renderScreenWidth / scaleFactor) > safety) {
                float animationFactor = 1;

                offsetX += animationFactor * ((AppSettings.scaleAnimationSpeed()) ?
                        (animationModifierX / scaleFactor) :
                        animationModifierX);
            }

            if (AppSettings.useVerticalAnimation() && bitmapHeight - (renderScreenHeight / scaleFactor) > safety) {
                offsetY += ((AppSettings.scaleAnimationSpeed()) ?
                        (animationModifierY / scaleFactor) :
                        animationModifierY);
            }

        }


        calculateBounds();
        if (inStartTransition) {
            applyStartTransition();
        }
        else if (inEndTransition) {
            applyEndTransition();
        }

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glUniform1f(mAlphaHandle, alpha);

//        android.opengl.Matrix.setIdentityM(transMatrix, 0);
//        android.opengl.Matrix.translateM(transMatrix, 0, offsetX, offsetY, 0f);
        android.opengl.Matrix.orthoM(matrixProjection,
                0,
                0,
                renderScreenWidth / scaleFactor,
                0,
                renderScreenHeight / scaleFactor,
                0,
                10f);

        android.opengl.Matrix.setIdentityM(transMatrix, 0);
        android.opengl.Matrix.translateM(transMatrix,
                0,
                renderScreenWidth / scaleFactor / 2,
                renderScreenHeight / scaleFactor / 2,
                0);
        android.opengl.Matrix.rotateM(transMatrix, 0, angle, 0.0f, 0.0f, 1.0f);
        android.opengl.Matrix.translateM(transMatrix,
                0,
                -renderScreenWidth / scaleFactor / 2,
                -renderScreenHeight / scaleFactor / 2,
                0);
        android.opengl.Matrix.translateM(transMatrix, 0, offsetX, offsetY, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
        // Enable generic vertex attribute array
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle,
                3,
                GLES20.GL_FLOAT,
                false,
                0,
                vertexBuffer);

        // Enable generic vertex attribute array
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);

        // Prepare the texturecoordinates
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

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

    private void applyStartTransition() {

        long time = System.currentTimeMillis();

        if (WallpaperRenderer.isLoadingTexture()) {
            return;
        }

        if (transitionEndtime == 0) {
            transitionEndtime = time + AppSettings.getTransitionSpeed() * 100;
        }

        if (transitionEndtime < time) {
            inStartTransition = false;
            transitionEndtime = 0;
            angle = 0.0f;
            alpha = 1.0f;
            scaleFactor = 1.0f;
            return;
        }

        float timeRatio = (float) (transitionEndtime - time) / (AppSettings.getTransitionSpeed() * 100);

        if (AppSettings.useZoomIn()) {
            scaleFactor = 1.0f - timeRatio;
            offsetX = bitmapWidth / scaleFactor / 2 * timeRatio - ((bitmapWidth / scaleFactor - renderScreenWidth / scaleFactor) / 2.0f) - (bitmapWidth - renderScreenWidth) / scaleFactor * (offsetX / (renderScreenWidth - bitmapWidth) - 0.5f);
            offsetY = bitmapHeight / scaleFactor / 2 * timeRatio - ((bitmapHeight / scaleFactor - renderScreenHeight / scaleFactor) / 2);
        }

        if (AppSettings.useOvershoot()) {
            OvershootInterpolator horizontalOvershootInterpolator = new OvershootInterpolator(AppSettings.getOvershootIntensity() / 10f);
            if (AppSettings.reverseOvershoot()) {
                offsetX = rawOffsetX * (renderScreenWidth - bitmapWidth) - renderScreenWidth * horizontalOvershootInterpolator.getInterpolation(1.0f - timeRatio);
                animationModifierX = Math.abs(animationModifierX);
            }
            else {
                offsetX = rawOffsetX * (renderScreenWidth - bitmapWidth) + renderScreenWidth * horizontalOvershootInterpolator.getInterpolation(1.0f - timeRatio);
                animationModifierX = -Math.abs(animationModifierX);
            }
        }

        if (AppSettings.useVerticalOvershoot()) {
            OvershootInterpolator verticalOvershootInterpolator = new OvershootInterpolator(AppSettings.getVerticalOvershootIntensity() / 10f);
            offsetY = (AppSettings.reverseVerticalOvershoot() ?
                    (bitmapHeight - renderScreenHeight) * -0.5f + renderScreenHeight - (renderScreenHeight * verticalOvershootInterpolator.getInterpolation(
                            1.0f - timeRatio)) :
                    (bitmapHeight - renderScreenHeight) * -0.5f - renderScreenHeight + (renderScreenHeight * verticalOvershootInterpolator.getInterpolation(
                            1.0f - timeRatio)));

        }

        if (AppSettings.useSpinIn()) {
            angle = AppSettings.reverseSpinIn()
                    ? AppSettings.getSpinInAngle() / 10f * -timeRatio
                    : AppSettings.getSpinInAngle() / 10f * timeRatio;
        }

        if (AppSettings.useFade()) {
            alpha = 1.0f - timeRatio;
        }
    }

    public void startFinish() {
        inEndTransition = true;
    }

    private void applyEndTransition() {

        long time = System.currentTimeMillis();

        if (transitionEndtime == 0) {
            transitionEndtime = time + AppSettings.getTransitionSpeed() * 100;
        }

        if (transitionEndtime < time) {
            finished = true;
            GLES20.glDeleteTextures(1, textureNames, 0);
            return;
        }

        float timeRatio = (float) (transitionEndtime - time) / (AppSettings.getTransitionSpeed() * 100);

        if (AppSettings.useZoomOut()) {
            scaleFactor = timeRatio;
            //offsetX = bitmapWidth / scaleFactor / 2 * (1.0f - timeRatio) - ((bitmapWidth / scaleFactor - renderScreenWidth / scaleFactor) / 2) - (bitmapWidth - renderScreenWidth) / scaleFactor * (offsetX / (renderScreenWidth - bitmapWidth) - 0.5f);
            offsetY = bitmapHeight / scaleFactor / 2 * (1.0f - timeRatio) - ((bitmapHeight / scaleFactor - renderScreenHeight / scaleFactor) / 2);
        }

        if (AppSettings.useSpinOut()) {
            angle = AppSettings.reverseSpinOut()
                    ? AppSettings.getSpinOutAngle() / 10f * -(1.0f - timeRatio)
                    : AppSettings.getSpinOutAngle() / 10f * (1.0f - timeRatio);
            Log.i(TAG, "Angle: " + angle);
        }

        if (AppSettings.useFade()) {
            alpha = timeRatio;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public void setRawOffsetX(float rawOffsetX) {
        this.rawOffsetX = rawOffsetX;
    }
}