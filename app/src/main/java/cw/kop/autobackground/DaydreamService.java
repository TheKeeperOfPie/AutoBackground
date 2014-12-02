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

import android.annotation.TargetApi;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.service.dreams.DreamService;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/13/2014.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class DaydreamService extends DreamService {

    private GLSurfaceView glSurfaceView;
    private WallpaperRenderer renderer;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private int touchCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        gestureDetector = new GestureDetector(getApplicationContext(),
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public void onLongPress(MotionEvent e) {
                        if (AppSettings.useLongPressReset()) {
                            renderer.resetPosition();
                        }
                        super.onLongPress(e);
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        renderer.loadNext(FileHandler.getNextImage());
                        return super.onDoubleTap(e);
                    }

                    @Override
                    public boolean onScroll(MotionEvent e1,
                            MotionEvent e2,
                            float distanceX,
                            float distanceY) {
                        if (AppSettings.useDrag() && touchCount == 2) {
                            renderer.onSwipe(distanceX,
                                    distanceY, 0);
                            glSurfaceView.requestRender();
                            return true;
                        }
                        return super.onScroll(e1,
                                e2,
                                distanceX,
                                distanceY);
                    }


                });

        scaleGestureDetector = new ScaleGestureDetector(getApplicationContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(
                            ScaleGestureDetector detector) {

                        if (AppSettings.useScale()) {
                            renderer.setScaleFactor(detector.getScaleFactor(), 0);

                            glSurfaceView.requestRender();
                            return true;
                        }
                        return false;
                    }
                });
        glSurfaceView = new GLSurfaceView(getApplicationContext());
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        setInteractive(true);
        setFullscreen(true);
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new WallpaperRenderer(getApplicationContext(),
                new WallpaperRenderer.Callback() {

                    @Override
                    public void setRenderMode(int mode) {
                        glSurfaceView.setRenderMode(mode);
                    }

                    @Override
                    public void loadCurrent() {

                    }

                    @Override
                    public void requestRender() {
                        glSurfaceView.requestRender();
                    }
                });
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        setContentView(glSurfaceView);
        renderer.loadNext(FileHandler.getNextImage());
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        glSurfaceView.onResume();
    }

    @Override
    public void onDreamingStopped() {
        glSurfaceView.onPause();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        renderer.release();

        super.onDetachedFromWindow();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        touchCount = event.getPointerCount();
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);

        return super.dispatchTouchEvent(event);
    }
}
