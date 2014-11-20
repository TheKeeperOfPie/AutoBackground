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
    private float[] matrixView = new float[16];
    private float renderScreenWidth = 1;
    private float renderScreenHeight = 1;
    private long startTime;
    private long endTime;
    private long frameTime;
    private long targetFrameTime;

    private Context serviceContext;

    private int[] maxTextureSize = new int[] {0};
    private float rawOffsetX = 0f;

    private boolean animated = false;

    private int[] textureNames = new int[3];
    private boolean toEffect = false;
    private boolean contextInitialized = false;
    private boolean loadCurrent = false;
    private EffectContext effectContext;
    private EffectFactory effectFactory;

    private Callback callback;
    private static WallpaperRenderer instance;

    private volatile List<RenderImage> renderImages;
    private static volatile boolean isLoadingTexture = false;

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
    private boolean isPlayingMusic;

    public static boolean isLoadingTexture() {
        return isLoadingTexture;
    }

    public static void setIsLoadingTexture(boolean loading) {
        isLoadingTexture = loading;
        if (instance.renderImages.size() > 1) {
            instance.renderImages.get(0).startFinish();
        }
    }

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
        renderImages = new CopyOnWriteArrayList<>();
    }

    @Override
    public void onDrawFrame(GL10 gl) {

//        if (!contextInitialized) {
//            effectContext = EffectContext.createWithCurrentGlContext();
//            contextInitialized = true;
//        }
//
//        if (toEffect && effectContext != null) {
//            toEffect = false;
//
//            try {
//                initEffects(0);
//            }
//            catch (RuntimeException e) {
//                e.printStackTrace();
//            }
//            GLES20.glDeleteTextures(1, textureNames, 2);
//            Log.i(TAG, "Deleted texture: " + textureNames[2]);
//
//        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


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


        for (RenderImage image : renderImages) {
            image.renderImage();
            // Works to remove due to renderImages being an instance of CopyOnWriteArrayList
            // Iterating list is separate from volatile instance
            if (image.isFinished()) {
                renderImages.remove(image);
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        Log.i(TAG, "Renderer onSurfaceChanged");

        if (width != renderScreenWidth) {
            GLES20.glViewport(0, 0, width, height);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            for (RenderImage image : renderImages) {
                image.setDimensions(width, height);
            }
//            callback.loadCurrent();
        }

        renderScreenWidth = width;
        renderScreenHeight = height;

        for (RenderImage image : renderImages) {
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

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);

        RenderImage.setupRenderValues();
        if (renderImages.size() == 0) {
            renderImages.add(getNewImage(BitmapFactory.decodeFile(FileHandler.getNextImage().getAbsolutePath())));
        }

        Log.i(TAG, "onSurfaceCreated");
    }

    private RenderImage getNewImage(Bitmap bitmap) {
        RenderImage image = new RenderImage(bitmap, getNewTextureId());
        image.setAnimated(AppSettings.useAnimation() || AppSettings.useVerticalAnimation());
        image.setAnimationModifierX(AppSettings.getAnimationSpeed() / 10f);
        image.setAnimationModifierY(AppSettings.getVerticalAnimationSpeed() / 10f);
        return image;
    }

    public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep,
            int xPixels, int yPixels) {
        rawOffsetX = AppSettings.forceParallax() ? 1.0f - xOffset : xOffset;
        for (RenderImage image : renderImages) {
            image.onOffsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels);
        }
    }

    public void onSwipe(float xMovement, float yMovement) {
        for (RenderImage image : renderImages) {
            image.onSwipe(xMovement, yMovement);
        }
    }

    public void resetPosition() {
        for (RenderImage image : renderImages) {
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

            Log.d(TAG, "Time to load bitmap: " + (finish - start));

            RenderImage newImage = getNewImage(scaleBitmap(bitmap));
            newImage.setRawOffsetX(rawOffsetX);
            newImage.setDimensions(renderScreenWidth, renderScreenHeight);
            while (renderImages.size() > 2) {
                renderImages.remove(0);
            }
            renderImages.add(newImage);
            isLoadingTexture = true;
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
        for (RenderImage image : renderImages) {
            image.setScaleFactor(factor);
        }
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

    public void release() {
        Log.i(TAG, "release");
    }

    public boolean isAnimated() {
        return animated;
    }

    public void setPlayingMusic(boolean playingMusic) {
        isPlayingMusic = playingMusic;
    }

    public void setContextInitialized(boolean contextInitialized) {
        this.contextInitialized = contextInitialized;
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
