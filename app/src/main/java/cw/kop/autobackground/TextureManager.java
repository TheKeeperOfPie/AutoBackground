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

import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/14/2014.
 */
public class TextureManager {

//    public void setTexture(Bitmap bitmap) {
//
//
//
//        if (nextImage == null) {
//            return;
//        }
//
//        try {
//
//            int storeId = textureNames[0];
//            textureNames[0] = textureNames[1];
//            textureNames[1] = storeId;
//
//            setupContainer(bitmapWidth, bitmapHeight);
//
//            GLES20.glDeleteTextures(1, textureNames, 0);
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
//
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
//                    GLES20.GL_TEXTURE_MIN_FILTER,
//                    GLES20.GL_LINEAR);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
//                    GLES20.GL_TEXTURE_MAG_FILTER,
//                    GLES20.GL_LINEAR);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
//                    GLES20.GL_TEXTURE_WRAP_S,
//                    GLES20.GL_CLAMP_TO_EDGE);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
//                    GLES20.GL_TEXTURE_WRAP_T,
//                    GLES20.GL_CLAMP_TO_EDGE);
//
//            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, nextImage, 0);
//            checkGLError("Bind textureNames[0]");
//            Log.i(TAG, "Bind texture: " + textureNames[0]);
//
//            GLES20.glDeleteTextures(1, textureNames, 2);
//
//            if (AppSettings.useEffects()) {
//                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[2]);
//
//                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
//                        GLES20.GL_TEXTURE_MIN_FILTER,
//                        GLES20.GL_LINEAR);
//                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
//                        GLES20.GL_TEXTURE_MAG_FILTER,
//                        GLES20.GL_LINEAR);
//                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
//                        GLES20.GL_TEXTURE_WRAP_S,
//                        GLES20.GL_CLAMP_TO_EDGE);
//                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
//                        GLES20.GL_TEXTURE_WRAP_T,
//                        GLES20.GL_CLAMP_TO_EDGE);
//
//                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, nextImage, 0);
//                checkGLError("Bind textureNames[2]");
//                Log.i(TAG, "Bind texture: " + textureNames[2]);
//                toEffect = true;
//            }
//
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
//            Log.i(TAG, "Render texture: " + textureNames[0]);
//
//            if (AppSettings.getTransitionTime() > 0) {
//                useTransition = true;
//                transitionTime = System.currentTimeMillis() + AppSettings.getTransitionTime();
//                callback.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
//            }
//            else {
//                GLES20.glDeleteTextures(1, textureNames, 1);
//            }
//        }
//        catch (IllegalArgumentException e) {
//            e.printStackTrace();
//        }
//    }

}
