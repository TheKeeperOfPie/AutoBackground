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
import android.graphics.Color;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.animation.OvershootInterpolator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/15/2014.
 */
public class RenderImage {

    public static final String TAG = RenderImage.class.getCanonicalName();

    public static int[] maxTextureSize = new int[1];
    private static int mPositionHandle;
    private static int mTexCoordLoc;
    private static int mtrxhandle;
    private static int mAlphaHandle;
    private static int program;
    private float vertices[];
    private short indices[];
    private float uvs[];
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private FloatBuffer uvBuffer;
    private float renderScreenWidth;
    private float renderScreenHeight;
    private float[] matrixProjection = new float[16];
    private float[] matrixView = new float[16];
    private float[] matrixProjectionAndView = new float[16];
    private float[] transMatrix = new float[16];
    private Bitmap imageBitmap;
    private boolean textureLoaded = false;
    private boolean animated = false;
    private int[] textureNames;
    private float scaleFactor = 1.0f;
    private float saveScaleFactor = 0f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float minRatioX = 0f;
    private float minRatioY = 1f;
    private float maxRatioX = 0f;
    private float maxRatioY = 1f;
    private float rawOffsetX = -1f;
    private float saveOffsetX = 0f;
    private float saveOffsetY = 0f;
    private float bitmapWidth = 1f;
    private float bitmapHeight = 1f;
    private float animationModifierX = 0f;
    private float animationModifierY = 0f;
    private float angle = 0f;
    private float alpha = 0f;
    private volatile boolean inStartTransition = true;
    private volatile boolean inEndTransition = false;
    private long transitionEndtime = 0;
    private EffectFactory effectFactory;
    private EventListener eventListener;

    public RenderImage(Bitmap bitmap,
            int textureName,
            EventListener eventListener,
            float minRatioX,
            float maxRatioX,
            float minRatioY,
            float maxRatioY) {
        this.minRatioX = minRatioX;
        this.maxRatioX = maxRatioX;
        this.minRatioY = minRatioY;
        this.maxRatioY = maxRatioY;
        this.eventListener = eventListener;
        setBitmap(bitmap);
        textureNames = new int[] {textureName, 0};
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
                GLShaders.VERTEX_SHADER);
        int fragmentShader = GLShaders.loadShader(GLES20.GL_FRAGMENT_SHADER,
                GLShaders.FRAGMENT_SHADER);
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
        applyScaleFactor(0.0f);
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
        bitmapWidth = imageBitmap.getWidth();
        bitmapHeight = imageBitmap.getHeight();
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

        if (AppSettings.reverseDrag()) {
            offsetX += xMovement;
            offsetY -= yMovement;
        }
        else {
            offsetX -= xMovement;
            offsetY += yMovement;
        }

    }

    public void loadTexture() {
        if (imageBitmap == null || textureLoaded) {
            return;
        }

        try {

            Log.i(TAG,
                    "imageBitmap width: " + imageBitmap.getWidth() + " imageBitmap height: " + imageBitmap.getHeight());

            GLES20.glGenTextures(2, textureNames, 0);
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

            if (AppSettings.useEffects()) {
                effectFactory = EffectContext.createWithCurrentGlContext().getFactory();
                initEffects();
            }

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
            checkGLError("texImage2D: ");
            eventListener.doneLoading();
            textureLoaded = true;
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

    private void initEffects() {

        Random random = new Random();

        boolean randomApplied = false;

        if (random.nextDouble() <= AppSettings.getRandomEffectsFrequency()) {
            if (AppSettings.useRandomEffects()) {
                applyRandomEffects(AppSettings.getRandomEffect());
                randomApplied = true;
                if (AppSettings.useEffectsOverride()) {
                    applyManualEffects();
                }
            }
        }

        if (random.nextDouble() > AppSettings.getEffectsFrequency()) {
            eventListener.toastEffect("Not applied", "");
        }
        else if (!randomApplied) {
            applyManualEffects();
        }
    }

    private void applyEffect(Effect setEffect, String name,
            String description) {

        GLES20.glGenTextures(1, textureNames, 1);
        setEffect.apply(textureNames[0],
                Math.round(renderScreenWidth),
                Math.round(renderScreenHeight),
                textureNames[1]);
        setEffect.release();
        GLES20.glDeleteTextures(1, textureNames, 0);
        textureNames[0] = textureNames[1];

        if (AppSettings.useToast() && AppSettings.useToastEffects()) {
            eventListener.toastEffect(name, description);
        }
        Log.i(TAG, "Effect applied: " + name + "\n" + description);

    }

    private void applyManualEffects() {

        Effect effect;

        if (AppSettings.getAutoFixEffect() > 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_AUTOFIX)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_AUTOFIX);
            effect.setParameter("scale", AppSettings.getAutoFixEffect());
            applyEffect(effect,
                    "Auto Fix",
                    "Value:" + AppSettings.getAutoFixEffect());
        }

        if (AppSettings.getBrightnessEffect() != 1.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_BRIGHTNESS)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_BRIGHTNESS);
            effect.setParameter("brightness", AppSettings.getBrightnessEffect());
            applyEffect(effect,
                    "Brightness",
                    "Value:" + AppSettings.getBrightnessEffect());
        }

        if (AppSettings.getContrastEffect() != 1.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_CONTRAST)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_CONTRAST);
            effect.setParameter("contrast", AppSettings.getContrastEffect());
            applyEffect(effect,
                    "Contrast",
                    "Value:" + AppSettings.getContrastEffect());
        }

        if (AppSettings.getCrossProcessEffect() && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_CROSSPROCESS)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_CROSSPROCESS);
            applyEffect(effect, "Cross Process", "");
        }

        if (AppSettings.getDocumentaryEffect() && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_DOCUMENTARY)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_DOCUMENTARY);
            applyEffect(effect, "Documentary", "");
        }

        if (AppSettings.getDuotoneEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_DUOTONE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
            effect.setParameter("first_color", AppSettings.getDuotoneColor(1));
            effect.setParameter("second_color", AppSettings.getDuotoneColor(2));
            applyEffect(effect,
                    "Dual Tone",
                    "\nColor 1: " + AppSettings.getDuotoneColor(1) + "\nColor 2: " + AppSettings.getDuotoneColor(
                            2));
        }

        if (AppSettings.getFillLightEffect() > 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_FILLLIGHT)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_FILLLIGHT);
            effect.setParameter("strength", AppSettings.getFillLightEffect());
            applyEffect(effect,
                    "Fill Light",
                    "Value:" + AppSettings.getFillLightEffect());
        }

        if (AppSettings.getFisheyeEffect() > 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_FISHEYE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_FISHEYE);
            effect.setParameter("scale", AppSettings.getFisheyeEffect());
            applyEffect(effect,
                    "Fisheye",
                    "Value:" + AppSettings.getFisheyeEffect());
        }

        if (AppSettings.getGrainEffect() > 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_GRAIN)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_GRAIN);
            effect.setParameter("strength", AppSettings.getGrainEffect());
            applyEffect(effect, "Grain", "Value:" + AppSettings.getGrainEffect());
        }

        if (AppSettings.getGrayscaleEffect() && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_GRAYSCALE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
            applyEffect(effect, "Grayscale", "");
        }

        if (AppSettings.getLomoishEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_LOMOISH)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_LOMOISH);
            applyEffect(effect, "Lomoish", "");
        }

        if (AppSettings.getNegativeEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_NEGATIVE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_NEGATIVE);
            applyEffect(effect, "Negaative", "");
        }

        if (AppSettings.getPosterizeEffect() && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_POSTERIZE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_POSTERIZE);
            applyEffect(effect, "Posterize", "");
        }

        if (AppSettings.getSaturateEffect() != 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_SATURATE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_SATURATE);
            effect.setParameter("scale", AppSettings.getSaturateEffect());
            applyEffect(effect,
                    "Saturate",
                    "Value:" + AppSettings.getSaturateEffect());
        }

        if (AppSettings.getSepiaEffect() && EffectFactory.isEffectSupported(EffectFactory.EFFECT_SEPIA)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_SEPIA);
            applyEffect(effect, "Sepia", "Value:");
        }

        if (AppSettings.getSharpenEffect() > 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_SHARPEN)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_SHARPEN);
            effect.setParameter("scale", AppSettings.getSharpenEffect());
            applyEffect(effect,
                    "Sharpen",
                    "Value:" + AppSettings.getSharpenEffect());
        }

        if (AppSettings.getTemperatureEffect() != 0.5f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_TEMPERATURE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_TEMPERATURE);
            effect.setParameter("scale", AppSettings.getTemperatureEffect());
            applyEffect(effect,
                    "Temperature",
                    "Value:" + AppSettings.getTemperatureEffect());
        }

        if (AppSettings.getVignetteEffect() > 0.0f && EffectFactory.isEffectSupported(
                EffectFactory.EFFECT_VIGNETTE)) {
            effect = effectFactory.createEffect(EffectFactory.EFFECT_VIGNETTE);
            effect.setParameter("scale", AppSettings.getVignetteEffect());
            applyEffect(effect,
                    "Vignette",
                    "Value:" + AppSettings.getVignetteEffect());
        }
    }

    private void applyRandomEffects(String randomEffect) {

        Random random = new Random();
        Effect effect;

        switch (randomEffect) {
            case "Completely Random": {
                String[] allEffectsList = eventListener.getContext().getResources().getStringArray(
                        R.array.effects_list);
                String[] allEffectParameters = eventListener.getContext().getResources().getStringArray(
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
                            effectName.substring(effectName.indexOf("effects.") + 8),
                            ((value != 0.0f) ? "Value:" + value : ""));
                }
                break;
            }
            case "Filter Effects": {
                String[] filtersList = eventListener.getContext().getResources().getStringArray(
                        R.array.effects_filters_list);

                int index = random.nextInt(filtersList.length);

                effect = effectFactory.createEffect(filtersList[index]);
                applyEffect(effect,
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
                        randomEffect,
                        "\n" + firstColor + "\n" + secondColor);

                break;
            }
            case "Dual Tone Rainbow": {

                ArrayList<String> colorsList = (ArrayList<String>) Arrays.asList(
                        eventListener.getContext().getResources().getStringArray(R.array.effects_color_list));

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
                        randomEffect,
                        "\n" + firstColor + "\n" + secondColor);

                break;
            }
        }
    }

//    private void toastEffect(final String effectName, final String effectValue) {
//        if (AppSettings.useToast() && AppSettings.useToastEffects()) {
//            Toast.makeText(eventListener.getContext(),
//                    "Effect applied: " + effectName + " " + effectValue,
//                    Toast.LENGTH_SHORT).show();
//        }
//    }

    public void applyScaleFactor(float factor) {
        scaleFactor *= factor;

        float minScaleFactor;
        float scaledRenderHeight = renderScreenHeight * (maxRatioY - minRatioY);

        if (AppSettings.extendScale()) {

            if (renderScreenWidth / bitmapWidth > scaledRenderHeight / bitmapHeight) {
                minScaleFactor = scaledRenderHeight / bitmapHeight;
            }
            else {
                minScaleFactor = renderScreenWidth / bitmapWidth;
            }
        }
        else {

            if (renderScreenWidth / bitmapWidth > scaledRenderHeight / bitmapHeight) {
                minScaleFactor = renderScreenWidth / bitmapWidth;
            }
            else {
                minScaleFactor = scaledRenderHeight / bitmapHeight;
            }
        }

        scaleFactor = Math.max(
                minScaleFactor,
                Math.min(scaleFactor,
                        5.0f));

        Log.d(TAG, "new scaleFactor: " + scaleFactor);
    }

    private void checkGLError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
        }
    }

    private void calculateBounds() {

        if (offsetX < (-bitmapWidth + renderScreenWidth / scaleFactor)) {
            animationModifierX = Math.abs(animationModifierX);
            offsetX = -bitmapWidth + renderScreenWidth / scaleFactor;

        }
        else if (offsetX > 0f) {
            animationModifierX = -Math.abs(animationModifierX);
            offsetX = 0f;
        }

        if (offsetY < (maxRatioY * renderScreenHeight / scaleFactor - bitmapHeight)) {
            animationModifierY = Math.abs(animationModifierY);
            offsetY = maxRatioY * renderScreenHeight / scaleFactor - bitmapHeight;
        }
        else if (offsetY > minRatioY * renderScreenHeight / scaleFactor) {
            animationModifierY = -Math.abs(animationModifierY);
            offsetY = minRatioY * renderScreenHeight / scaleFactor;
        }
    }

    public void renderImage() {

        if (!textureLoaded) {
            loadTexture();
        }

        if (renderScreenWidth / scaleFactor == 0) {
            return;
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

            if (AppSettings.useVerticalAnimation() && bitmapHeight - (renderScreenHeight * (maxRatioY - minRatioY) / scaleFactor) > safety) {
                offsetY += ((AppSettings.scaleAnimationSpeed()) ?
                        (animationModifierY / scaleFactor) :
                        animationModifierY);
            }

        }


        calculateBounds();


        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glScissor(0, (int) (minRatioY * renderScreenHeight), (int) renderScreenWidth,
                (int) ((maxRatioY - minRatioY) * renderScreenHeight));
        GLES20.glUniform1f(mAlphaHandle, alpha);

        if (inStartTransition) {
            applyStartTransition();
        }
        else if (inEndTransition) {
            applyEndTransition();
        }

        android.opengl.Matrix.orthoM(matrixProjection,
                0,
                0,
                renderScreenWidth / scaleFactor,
                0,
                renderScreenHeight / scaleFactor,
                0,
                1f);

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

        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    private void applyStartTransition() {

        long time = System.currentTimeMillis();

        if (transitionEndtime == 0) {
            transitionEndtime = time + AppSettings.getTransitionSpeed() * 100;
        }
        else if (transitionEndtime < time) {
            transitionEndtime = 0;
            angle = 0.0f;
            alpha = 1.0f;
            applyScaleFactor(0.0f);
            eventListener.requestRender();
            inStartTransition = false;
            return;
        }

        float timeRatio = (float) (transitionEndtime - time) / (AppSettings.getTransitionSpeed() * 100);
        float ratio = maxRatioY - minRatioY;
        float scaledRenderWidth = renderScreenWidth / scaleFactor;
        float scaledRenderHeight = renderScreenHeight / scaleFactor;

        if (AppSettings.useZoomIn()) {
            float minScaleFactor;
            float trueRenderHeight = renderScreenHeight * (maxRatioY - minRatioY);
            if (renderScreenWidth / bitmapWidth > trueRenderHeight / bitmapHeight) {
                minScaleFactor = renderScreenWidth / bitmapWidth;
            }
            else {
                minScaleFactor = trueRenderHeight / bitmapHeight;
            }

            scaleFactor = minScaleFactor * (1.0f - timeRatio);
            offsetX = rawOffsetX * (renderScreenWidth - bitmapWidth) + renderScreenWidth / 2 / scaleFactor * (timeRatio);
            offsetY = renderScreenHeight / 2 / scaleFactor * (timeRatio) + minRatioY * renderScreenHeight;
        }

        if (AppSettings.useOvershoot()) {
            OvershootInterpolator horizontalOvershootInterpolator = new OvershootInterpolator(
                    AppSettings.getOvershootIntensity() / 10f);
            if (AppSettings.reverseOvershoot()) {
                offsetX = rawOffsetX * (scaledRenderWidth - bitmapWidth) + scaledRenderWidth - scaledRenderWidth * horizontalOvershootInterpolator.getInterpolation(
                        1.0f - timeRatio);
                animationModifierX = Math.abs(animationModifierX);
            }
            else {
                offsetX = rawOffsetX * (scaledRenderWidth - bitmapWidth) - scaledRenderWidth + scaledRenderWidth * horizontalOvershootInterpolator.getInterpolation(
                        1.0f - timeRatio);
                animationModifierX = -Math.abs(animationModifierX);
            }
        }

        if (AppSettings.useVerticalOvershoot()) {
            OvershootInterpolator verticalOvershootInterpolator = new OvershootInterpolator(
                    AppSettings.getVerticalOvershootIntensity() / 10f);
            offsetY = (AppSettings.reverseVerticalOvershoot() ?
                    scaledRenderHeight * minRatioY + scaledRenderHeight * ratio - (scaledRenderHeight * ratio * verticalOvershootInterpolator.getInterpolation(
                            1.0f - timeRatio)) :
                    scaledRenderHeight * minRatioY - scaledRenderHeight * ratio + (scaledRenderHeight * ratio * verticalOvershootInterpolator.getInterpolation(
                            1.0f - timeRatio)));

        }

        if (AppSettings.useSpinIn()) {
            angle = AppSettings.reverseSpinIn()
                    ? AppSettings.getSpinInAngle() / 10f * -timeRatio
                    : AppSettings.getSpinInAngle() / 10f * timeRatio;
            GLES20.glScissor(0, 0, (int) renderScreenWidth,
                    (int) renderScreenHeight);
        }

        if (AppSettings.useFade()) {
            alpha = 1.0f - timeRatio;
        }
        else {
            alpha = 1.0f;
        }
    }

    public void startFinish() {
        inEndTransition = true;
        Log.i(TAG, "inEndTransition: " + inEndTransition);
    }

    public void finishImmediately() {
        GLES20.glDeleteTextures(2, textureNames, 0);
        eventListener.removeSelf(this);
    }

    private void applyEndTransition() {

        long time = System.currentTimeMillis();

        if (transitionEndtime == 0) {
            transitionEndtime = time + AppSettings.getTransitionSpeed() * 100;
        }
        else if (transitionEndtime < time) {
            GLES20.glDeleteTextures(2, textureNames, 0);
            eventListener.removeSelf(this);
            return;
        }

        if (saveOffsetX == 0) {
            saveOffsetX = offsetX;
            saveOffsetY = offsetY;
            saveScaleFactor = scaleFactor;
        }

        float timeRatio = (float) (transitionEndtime - time) / (AppSettings.getTransitionSpeed() * 100);

        if (AppSettings.useZoomOut()) {
            scaleFactor = saveScaleFactor * timeRatio;
            offsetX = saveOffsetX + renderScreenWidth / 2 / scaleFactor * (1.0f - timeRatio);
            offsetY = saveOffsetY + renderScreenHeight / 2 / scaleFactor * (1.0f - timeRatio);
        }

        if (AppSettings.useSpinOut()) {
            angle = AppSettings.reverseSpinOut()
                    ? AppSettings.getSpinOutAngle() / 10f * -(1.0f - timeRatio)
                    : AppSettings.getSpinOutAngle() / 10f * (1.0f - timeRatio);
        }

        if (AppSettings.useFade()) {
            alpha = timeRatio;
        }
        else {
            alpha = 0.0f;
        }
    }

    public void setRawOffsetX(float rawOffsetX) {
        this.rawOffsetX = rawOffsetX;
    }

    public interface EventListener {

        public void removeSelf(RenderImage image);

        public void doneLoading();

        public Context getContext();

        public void toastEffect(final String effectName, final String effectValue);

        public void requestRender();

    }

}