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

package cw.kop.autobackground.settings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.MainActivity;
import cw.kop.autobackground.OptionData;
import cw.kop.autobackground.OptionsListAdapter;
import cw.kop.autobackground.R;
import cw.kop.autobackground.RecyclerViewListClickListener;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.shared.WearConstants;

public class WearSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener, View.OnTouchListener, View.OnClickListener {

    private static final String TAG = WearSettingsFragment.class.getName();
    private static final float SHADOW_RADIUS = 5f;

    private Context appContext;
    private GoogleApiClient googleApiClient;
    private boolean isWearConnected = false;
    private Handler handler;
    private DateFormat timeFormat;
    private ImageView watchBackground;
    private RelativeLayout watchContainer;
    private RecyclerView recyclerView;
    private ListView preferenceList;
    private IntentFilter intentFilter;
    private SurfaceView surfaceView;
    private Canvas canvas;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MainActivity.LOAD_NAV_PICTURE:
                    loadImageFile();
                    redraw();
                    break;
                case MainActivity.DRAWER_OPENED:
                    surfaceView.setVisibility(View.GONE);
                    break;
                case MainActivity.DRAWER_CLOSED:
                    surfaceView.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    private Bitmap imageBitmap;

    private float tickRadius = 0.80f;
    private float separatorWidth = 0f;
    private float hourRadius = 1f;
    private float minuteRadius = 1f;
    private float secondRadius = 1f;
    private float tickWidth = 5f;
    private float hourWidth = 5f;
    private float minuteWidth = 5f;
    private float secondWidth = 5f;

    private long timeOffset = 0l;
    private String timeSeparator = ":";
    private float xOffset;

    private Paint bitmapPaint;
    private Paint indicatorPaint;
    private Paint hourPaint;
    private Paint minutePaint;
    private Paint secondPaint;

    private String[] options;
    private String[] optionDescriptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_wear);
        handler = new Handler(appContext.getMainLooper());
        intentFilter = new IntentFilter();
        intentFilter.addAction(MainActivity.LOAD_NAV_PICTURE);
        intentFilter.addAction(MainActivity.DRAWER_OPENED);
        intentFilter.addAction(MainActivity.DRAWER_CLOSED);

        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(false);
        bitmapPaint.setDither(true);

        indicatorPaint = new Paint();
        indicatorPaint.setStrokeCap(Paint.Cap.BUTT);
        indicatorPaint.setTextAlign(Paint.Align.LEFT);

        hourPaint = new Paint();
        hourPaint.setStrokeCap(Paint.Cap.BUTT);
        hourPaint.setTextAlign(Paint.Align.LEFT);

        minutePaint = new Paint();
        minutePaint.setStrokeCap(Paint.Cap.BUTT);
        minutePaint.setTextAlign(Paint.Align.LEFT);

        secondPaint = new Paint();
        secondPaint.setStrokeCap(Paint.Cap.BUTT);
        secondPaint.setTextAlign(Paint.Align.LEFT);

        options = getResources().getStringArray(R.array.wear_time_options);
        optionDescriptions = getResources().getStringArray(R.array.wear_time_options_descriptions);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = activity;
    }

    @Override
    public void onDetach() {
        appContext = null;
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        googleApiClient = new GoogleApiClient.Builder(appContext)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        isWearConnected = true;
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        isWearConnected = false;
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        isWearConnected = false;
                    }
                })
                        // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        View view;

        if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
            view = inflater.inflate(R.layout.wear_settings_layout_landscape, container, false);
            watchContainer = (RelativeLayout) view.findViewById(R.id.watch_face_container);
            watchContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {


                    int height = watchBackground.getHeight();
                    ViewGroup.LayoutParams watchContainerParams = watchContainer.getLayoutParams();
                    watchContainerParams.height = height;
                    watchContainerParams.width = height;
                    watchContainer.setLayoutParams(watchContainerParams);
                    watchContainer.setPadding(Math.round(height * 0.278f),
                            Math.round(height * 0.23f),
                            Math.round(height * 0.278f),
                            Math.round(height * 0.33f));
                    watchContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    redraw();
                }
            });
            watchContainer.setOnClickListener(this);
            watchBackground = (ImageView) view.findViewById(R.id.watch_face_background);
            watchBackground.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (getView() == null) {
                        return;
                    }
                    ViewGroup.LayoutParams watchContainerParams = watchBackground.getLayoutParams();
                    watchContainerParams.width = watchBackground.getHeight();
                    watchBackground.setLayoutParams(watchContainerParams);
                    watchBackground.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }
        else {
            view = inflater.inflate(R.layout.wear_settings_layout, container, false);
            watchContainer = (RelativeLayout) view.findViewById(R.id.watch_face_container);
            watchContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            int width = displayMetrics.widthPixels;
                            ViewGroup.LayoutParams watchContainerParams = watchContainer.getLayoutParams();
                            watchContainerParams.height = width;
                            watchContainerParams.width = width;
                            watchContainer.setLayoutParams(watchContainerParams);
                            watchContainer.setPadding(Math.round(width * 0.278f),
                                    Math.round(width * 0.23f),
                                    Math.round(width * 0.278f),
                                    Math.round(width * 0.33f));
                            watchContainer.getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(this);
                            redraw();
                        }
                    });
            watchContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    preferenceList.setVisibility(View.VISIBLE);
                    recyclerView.setAdapter(null);
                    recyclerView.setVisibility(View.GONE);
                }
            });
            watchBackground = (ImageView) view.findViewById(R.id.watch_face_background);
            watchBackground.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (getView() == null) {
                        return;
                    }
                    ViewGroup.LayoutParams watchContainerParams = watchBackground.getLayoutParams();
                    watchContainerParams.height = watchBackground.getWidth();
                    watchBackground.setLayoutParams(watchContainerParams);
                    watchBackground.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }

        surfaceView = (SurfaceView) view.findViewById(R.id.surface_view);
//        surfaceView.setZOrderOnTop(true);
        surfaceView.setOnClickListener(this);
        surfaceView.setOnTouchListener(this);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                loadImageFile();
                redraw();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                loadImageFile();
                redraw();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        recyclerView = (RecyclerView) view.findViewById(R.id.watch_options_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        preferenceList = (ListView) view.findViewById(android.R.id.list);

        Button syncButton = (Button) view.findViewById(R.id.sync_button);
        syncButton.setText("Sync Settings");
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "syncButton pressed");
                syncSettings();
            }
        });

        findPreference("wear_time_type").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DialogFactory.ListDialogListener listDialogListener = new DialogFactory.ListDialogListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        switch (position) {
                            case 0:
                                AppSettings.setTimeType(AppSettings.DIGITAL);
                                recyclerView.setAdapter(null);
                                preferenceList.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                                redraw();
                                break;
                            case 1:
                                AppSettings.setTimeType(AppSettings.ANALOG);
                                recyclerView.setAdapter(null);
                                preferenceList.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                                redraw();
                                break;
                        }
                        dismissDialog();
                    }
                };

                DialogFactory.showListDialog(appContext,
                        "Watch face",
                        listDialogListener,
                        R.array.wear_time_types);
                return true;
            }
        });

        findPreference("wear_time_adjust").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DialogFactory.TimeDialogListener listener = new DialogFactory.TimeDialogListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute) {
                        Time time = new Time();
                        time.setToNow();
                        long offset = (hour - time.hour) * 3600000 + (minute - time.minute) * 60000;

                        Log.i(TAG, "Time offset set: " + offset);
                        AppSettings.setTimeOffset(offset);
                        this.dismissDialog();
                        redraw();
                    }
                };

                DialogFactory.showTimeDialog(appContext,
                        "Enter time",
                        listener,
                        -1,
                        -1);

                return true;
            }
        });

        return view;
    }

    private void setPaints() {
        timeOffset = AppSettings.getTimeOffset();
        timeSeparator = AppSettings.getSeparatorText();

        indicatorPaint.setAntiAlias(true);
        hourPaint.setAntiAlias(true);
        minutePaint.setAntiAlias(true);
        secondPaint.setAntiAlias(true);

        indicatorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        hourPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        minutePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        secondPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        indicatorPaint.setColor(AppSettings.getSeparatorColor());
        hourPaint.setColor(AppSettings.getHourColor());
        minutePaint.setColor(AppSettings.getMinuteColor());
        secondPaint.setColor(AppSettings.getSecondColor());

        indicatorPaint.setShadowLayer(SHADOW_RADIUS,
                0f,
                0f,
                AppSettings.getSeparatorShadowColor());
        hourPaint.setShadowLayer(SHADOW_RADIUS, 0f, 0f, AppSettings.getHourShadowColor());
        minutePaint.setShadowLayer(SHADOW_RADIUS,
                0f,
                0f,
                AppSettings.getMinuteShadowColor());
        secondPaint.setShadowLayer(SHADOW_RADIUS,
                0f,
                0f,
                AppSettings.getSecondShadowColor());

        tickRadius = AppSettings.getTickLengthRatio();
        hourRadius = AppSettings.getHourLengthRatio();
        minuteRadius = AppSettings.getMinuteLengthRatio();
        secondRadius = AppSettings.getSecondLengthRatio();

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        tickWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                AppSettings.getTickWidth(),
                displayMetrics);
        hourWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                AppSettings.getHourWidth(),
                displayMetrics);
        minuteWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, AppSettings.getMinuteWidth(), displayMetrics);
        secondWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                AppSettings.getSecondWidth(),
                displayMetrics);

        Log.d(TAG, "surfaceView width: " + surfaceView.getWidth() + " surfaceView height: " + surfaceView.getHeight());

        float textSize = surfaceView.getHeight() / 4;
        float radius = surfaceView.getWidth() / 2;
        float width = (float) Math.sqrt(Math.pow(radius, 2f) - Math.pow(textSize, 2f)) * 2f;
        float textScale = 1.0f;
        xOffset = radius - width / 2f;

        indicatorPaint.setTextSize(textSize);
        hourPaint.setTextSize(textSize);
        minutePaint.setTextSize(textSize);
        secondPaint.setTextSize(textSize);
        indicatorPaint.setTextScaleX(textScale);
        hourPaint.setTextScaleX(textScale);
        minutePaint.setTextScaleX(textScale);
        secondPaint.setTextScaleX(textScale);

        while (getTimeWidth() > width) {
            Log.d(TAG, "getTimeWidth: " + getTimeWidth());
            Log.d(TAG, "width: " + width);

            textScale -= 0.05f;
            indicatorPaint.setTextScaleX(textScale);
            hourPaint.setTextScaleX(textScale);
            minutePaint.setTextScaleX(textScale);
            secondPaint.setTextScaleX(textScale);
        }

        separatorWidth = indicatorPaint.measureText(timeSeparator);

        if (AppSettings.getTimeType().equals(AppSettings.DIGITAL)) {
            indicatorPaint.setStrokeWidth(1.0f);
        }
        else {
            indicatorPaint.setStrokeWidth(tickWidth);
        }
    }

    private float getTimeWidth() {

        float width = 0;
        width += hourPaint.measureText("00");
        width += minutePaint.measureText("00");
        width += secondPaint.measureText("00");
        width += indicatorPaint.measureText(timeSeparator);
        width += indicatorPaint.measureText(timeSeparator);

        return width;

    }

    private void loadImageFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                float sideLength = surfaceView.getWidth();
                Log.i(TAG, "sideLength: " + sideLength);
                if (sideLength > 0) {

                    if (FileHandler.getCurrentBitmapFile() != null) {

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(FileHandler.getCurrentBitmapFile().getAbsolutePath(), options);
                        options.inJustDecodeBounds = false;

                        int sampleSize = 1;

                        float longestLength = options.outWidth > options.outHeight ? options.outWidth : options.outHeight;

                        while (longestLength / (sampleSize + 1) > sideLength) {
                            sampleSize++;
                        }
                        options.inSampleSize = sampleSize;

                        final Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(FileHandler.getCurrentBitmapFile().getAbsolutePath(), options),
                                Math.round(sideLength),
                                Math.round(sideLength));

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (imageBitmap  != null) {
                                    try {
                                        imageBitmap.recycle();
                                    }
                                    catch (Exception e) {

                                    }
                                }
                                imageBitmap = bitmap;
                                redraw();
                            }
                        });

                    }
                }
            }
        }).start();
    }

    private void redraw() {

        if (AppSettings.getTimeType().equals(AppSettings.DIGITAL)) {
            drawDigital();
        }
        else {
            drawAnalog();
        }

    }

    private void drawDigital() {

        canvas = surfaceView.getHolder().lockCanvas();

        if (canvas == null) {
            return;
        }

        setPaints();

        Time time = new Time();
        time.setToNow();
        time.set(time.toMillis(false) + timeOffset);

        float centerX = watchContainer.getWidth() * 0.222f;
        float centerY = watchContainer.getHeight() * 0.222f;

        if (imageBitmap != null) {
            canvas.drawBitmap(imageBitmap, 0, 0, bitmapPaint);
        }

        float x = xOffset + (time.hour < 10 ?  hourPaint.measureText("0") : 0);
        float hourWidth = hourPaint.measureText("" + time.hour);
        float minuteWidth = minutePaint.measureText(String.format("%02d", time.minute));

        canvas.drawText("" + time.hour, x, centerY, hourPaint);
        canvas.drawText("" + time.hour, x, centerY, hourPaint);
        canvas.drawText("" + time.hour, x, centerY, hourPaint);
        canvas.drawText("" + time.hour, x, centerY, hourPaint);
        canvas.drawText("" + time.hour, x, centerY, hourPaint);
        x += hourPaint.measureText("" + time.hour);

        canvas.drawText(timeSeparator, x, centerY, indicatorPaint);
        canvas.drawText(timeSeparator, x, centerY, indicatorPaint);
        canvas.drawText(timeSeparator, x, centerY, indicatorPaint);
        canvas.drawText(timeSeparator, x, centerY, indicatorPaint);
        canvas.drawText(timeSeparator, x, centerY, indicatorPaint);
        x += separatorWidth;

        canvas.drawText(String.format("%02d", time.minute), x, centerY, minutePaint);
        canvas.drawText(String.format("%02d", time.minute), x, centerY, minutePaint);
        canvas.drawText(String.format("%02d", time.minute), x, centerY, minutePaint);
        canvas.drawText(String.format("%02d", time.minute), x, centerY, minutePaint);
        canvas.drawText(String.format("%02d", time.minute), x, centerY, minutePaint);
        x += minutePaint.measureText(String.format("%02d", time.minute));

        canvas.drawText(timeSeparator, x, centerY, indicatorPaint);
        canvas.drawText(timeSeparator, x, centerY, indicatorPaint);
        canvas.drawText(timeSeparator, x, centerY, indicatorPaint);
        canvas.drawText(timeSeparator, x, centerY, indicatorPaint);
        canvas.drawText(timeSeparator, x, centerY, indicatorPaint);
        x += separatorWidth;

        canvas.drawText(String.format("%02d", time.second), x, centerY,
                secondPaint);
        canvas.drawText(String.format("%02d", time.second), x, centerY,
                secondPaint);
        canvas.drawText(String.format("%02d", time.second), x, centerY,
                secondPaint);
        canvas.drawText(String.format("%02d", time.second), x, centerY,
                secondPaint);
        canvas.drawText(String.format("%02d", time.second), x, centerY,
                secondPaint);

        surfaceView.getHolder().unlockCanvasAndPost(canvas);
    }

    private void drawAnalog() {
        if (!AppSettings.getTimeType().equals(AppSettings.ANALOG)) {
            return;
        }

        canvas = surfaceView.getHolder().lockCanvas();

        if (canvas == null) {
            return;
        }

        setPaints();

        if (imageBitmap != null) {
            canvas.drawBitmap(imageBitmap, 0, 0, bitmapPaint);
        }
//        Time time = new Time();
//        time.setToNow();
//        time.set(time.toMillis(false) + AppSettings.getTimeOffset());
//
//        float hour = time.hour + time.minute / 60f;
//        float minute = time.minute + time.second / 60f;
//        float second = time.second;
        float centerX = watchContainer.getWidth() * 0.222f;
        float centerY = watchContainer.getHeight() * 0.222f;
        float radius = centerX;
        // Draw tick marks

        for (int i = 0; i < 12; i++) {
            canvas.drawLine(
                    (float) (centerX + (radius * (100f - tickRadius) / 100f) * Math.cos(Math.toRadians(i * 30f))),
                    (float) (centerY + (radius * (100f - tickRadius) / 100f) * Math.sin(Math.toRadians(i * 30f))),
                    (float) (centerX + (radius) * Math.cos(Math.toRadians(i * 30f))),
                    (float) (centerY + (radius) * Math.sin(Math.toRadians(i * 30f))),
                    indicatorPaint);
        }

        // Draw clock hands

        Path hourPath = new Path();
        hourPath.moveTo((float) (centerX + hourWidth / 2f * Math.cos(Math.toRadians(90f))),
                (float) (centerY + hourWidth / 2f * Math.sin(Math.toRadians(90f))));
        hourPath.quadTo(
                (float) (centerX - (hourWidth / 2f) * Math.cos(Math.toRadians(0f))),
                (float) (centerY - (hourWidth / 2f) * Math.sin(Math.toRadians(0f))),
                (float) (centerX + hourWidth / 2f * Math.cos(Math.toRadians(270f))),
                (float) (centerY + hourWidth / 2f * Math.sin(Math.toRadians(270f))));
        hourPath.lineTo((float) (centerX + (radius * hourRadius / 100f) * Math.cos(Math.toRadians(0f))),
                (float) (centerY + (radius * hourRadius / 100f) * Math.sin(Math.toRadians(0f))));
        hourPath.close();
        canvas.drawPath(hourPath, hourPaint);
        canvas.drawPath(hourPath, hourPaint);
        canvas.drawPath(hourPath, hourPaint);
        canvas.drawPath(hourPath, hourPaint);
        canvas.drawPath(hourPath, hourPaint);

        Path minutePath = new Path();
        minutePath.moveTo((float) (centerX + minuteWidth / 2f * Math.cos(Math.toRadians(0f))),
                (float) (centerY + minuteWidth / 2f * Math.sin(Math.toRadians(0f))));
        minutePath.quadTo(
                (float) (centerX - (minuteWidth / 2f) * Math.cos(Math.toRadians(-180f))),
                (float) (centerY - (minuteWidth / 2f) * Math.sin(Math.toRadians(-180f))),
                (float) (centerX + minuteWidth / 2f * Math.cos(Math.toRadians(90f))),
                (float) (centerY + minuteWidth / 2f * Math.sin(Math.toRadians(90f))));
        minutePath.lineTo((float) (centerX + (radius * minuteRadius / 100f) * Math.cos(Math.toRadians(-90f))),
                (float) (centerY + (radius * minuteRadius / 100f) * Math.sin(Math.toRadians(-90f))));
        minutePath.close();
        canvas.drawPath(minutePath, minutePaint);
        canvas.drawPath(minutePath, minutePaint);
        canvas.drawPath(minutePath, minutePaint);
        canvas.drawPath(minutePath, minutePaint);
        canvas.drawPath(minutePath, minutePaint);

        Path secondPath = new Path();
        secondPath.moveTo((float) (centerX + secondWidth / 2f * Math.cos(Math.toRadians(225f))),
                (float) (centerY + secondWidth / 2f * Math.sin(Math.toRadians(225f))));
        secondPath.quadTo(
                (float) (centerX - (secondWidth / 2f) * Math.cos(Math.toRadians(45f))),
                (float) (centerY - (secondWidth / 2f) * Math.sin(Math.toRadians(45f))),
                (float) (centerX + secondWidth / 2f * Math.cos(Math.toRadians(315f))),
                (float) (centerY + secondWidth / 2f * Math.sin(Math.toRadians(315f))));
        secondPath.lineTo((float) (centerX + (radius * secondRadius / 100f) * Math.cos(Math.toRadians(
                        135f))),
                (float) (centerY + (radius * secondRadius / 100f) * Math.sin(Math.toRadians(
                        135f))));
        secondPath.close();
        canvas.drawPath(secondPath, secondPaint);
        canvas.drawPath(secondPath, secondPaint);
        canvas.drawPath(secondPath, secondPaint);
        canvas.drawPath(secondPath, secondPaint);
        canvas.drawPath(secondPath, secondPaint);

        surfaceView.getHolder().unlockCanvasAndPost(canvas);
    }

    private void syncSettings() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                PutDataMapRequest dataMap = PutDataMapRequest.create(WearConstants.SETTINGS);
                dataMap.getDataMap().putLong(WearConstants.TIME, new Date().getTime());
                dataMap.getDataMap().putString(WearConstants.TIME_TYPE, AppSettings.getTimeType());
                dataMap.getDataMap().putLong(WearConstants.TIME_OFFSET, AppSettings.getTimeOffset());
                dataMap.getDataMap().putBoolean(WearConstants.USE_TIME_PALETTE,
                        AppSettings.useTimePalette());

                dataMap.getDataMap().putString(WearConstants.SEPARATOR_TEXT,
                        AppSettings.getSeparatorText());
                dataMap.getDataMap().putInt(WearConstants.SEPARATOR_COLOR,
                        AppSettings.getSeparatorColor());
                dataMap.getDataMap().putInt(WearConstants.SEPARATOR_SHADOW_COLOR,
                        AppSettings.getSeparatorShadowColor());
                dataMap.getDataMap().putInt(WearConstants.HOUR_COLOR,
                        AppSettings.getHourColor());
                dataMap.getDataMap().putInt(WearConstants.HOUR_SHADOW_COLOR,
                        AppSettings.getHourShadowColor());
                dataMap.getDataMap().putInt(WearConstants.MINUTE_COLOR,
                        AppSettings.getMinuteColor());
                dataMap.getDataMap().putInt(WearConstants.MINUTE_SHADOW_COLOR,
                        AppSettings.getMinuteShadowColor());
                dataMap.getDataMap().putInt(WearConstants.SECOND_COLOR,
                        AppSettings.getSecondColor());
                dataMap.getDataMap().putInt(WearConstants.SECOND_SHADOW_COLOR,
                        AppSettings.getSecondShadowColor());

                dataMap.getDataMap().putFloat(WearConstants.TICK_LENGTH_RATIO, AppSettings.getTickLengthRatio());
                dataMap.getDataMap().putFloat(WearConstants.HOUR_LENGTH_RATIO, AppSettings.getHourLengthRatio());
                dataMap.getDataMap().putFloat(WearConstants.MINUTE_LENGTH_RATIO, AppSettings.getMinuteLengthRatio());
                dataMap.getDataMap().putFloat(WearConstants.SECOND_LENGTH_RATIO, AppSettings.getSecondLengthRatio());
                dataMap.getDataMap().putFloat(WearConstants.TICK_WIDTH, AppSettings.getTickWidth());
                dataMap.getDataMap().putFloat(WearConstants.HOUR_WIDTH, AppSettings.getHourWidth());
                dataMap.getDataMap().putFloat(WearConstants.MINUTE_WIDTH, AppSettings.getMinuteWidth());
                dataMap.getDataMap().putFloat(WearConstants.SECOND_WIDTH, AppSettings.getSecondWidth());

                Wearable.DataApi.putDataItem(googleApiClient, dataMap.asPutDataRequest());
            }
        }).start();

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float sideLength = surfaceView.getWidth();
            float x = event.getX();
            float y = event.getY();

            if (AppSettings.getTimeType().equals(AppSettings.DIGITAL)) {

                float hourWidth = hourPaint.measureText("00");
                float minuteWidth = minutePaint.measureText("00");

                if (y > sideLength / 2) {
                    showIndicatorOptions();
                }
                else if (xOffset < x && x < xOffset + hourWidth) {
                    showHourOptions(2);
                }
                else if (xOffset + hourWidth + separatorWidth < x & x < xOffset + hourWidth + separatorWidth + minuteWidth) {
                    showMinuteOptions(2);
                }
                else if (xOffset + hourWidth + separatorWidth + minuteWidth + separatorWidth< x) {
                    showSecondOptions(2);
                }
                else {
                    showIndicatorOptions();
                }
            }
            else {

                double touchRadius = Math.hypot(Math.abs(x - sideLength / 2), Math.abs(y - sideLength / 2));

                if (touchRadius > 0.4 * sideLength) {
                    showTickOptions();
                }
                else if (y < sideLength / 2 && x < sideLength - y) {
                    showMinuteOptions(4);
                }
                else if (x > sideLength / 2) {
                    showHourOptions(4);
                }
                else {
                    showSecondOptions(4);
                }
            }

            recyclerView.setVisibility(View.VISIBLE);
            preferenceList.setVisibility(View.GONE);

        }

        return false;
    }


    private void showIndicatorOptions() {

        String[] titles = new String[] {"Separator Color",
                "Separator Shadow Color",
                "Separator"};
        String[] summaries = new String[] {"Color of separator",
                "Color of separator shadow",
                "Text to separate time sections"};
        int[] icons = new int[] {R.drawable.ic_text_format_white_24dp,
                R.drawable.ic_color_lens_white_24dp,
                R.drawable.ic_color_lens_white_24dp};
        ArrayList<OptionData> optionsList = new ArrayList<>();

        for (int index = 0; index < titles.length; index++) {
            optionsList.add(new OptionData(titles[index],
                    summaries[index],
                    icons[index]));
        }

        final RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                switch (position) {
                    case 0:
                        DialogFactory.ColorDialogListener separatorColorDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setSeparatorColor(getColorPickerView().getColor());
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter separator color:",
                                separatorColorDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getSeparatorColor());
                        break;
                    case 1:
                        DialogFactory.ColorDialogListener separatorShadowColorDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setSeparatorShadowColor(getColorPickerView().getColor());
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter separator shadow color:",
                                separatorShadowColorDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getSeparatorShadowColor());
                        break;
                    case 2:
                        DialogFactory.InputDialogListener separatorTextDialogListener = new DialogFactory.InputDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setSeparatorText(getEditTextString());
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showInputDialog(appContext,
                                "Enter separator text",
                                "",
                                AppSettings.getSeparatorText(),
                                separatorTextDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                InputType.TYPE_CLASS_TEXT);
                        break;
                }

            }
        };

        OptionsListAdapter titlesAdapter = new OptionsListAdapter(appContext,
                optionsList,
                -1,
                listener);

        recyclerView.setAdapter(titlesAdapter);
    }


    private void showTickOptions() {

        String[] titles = new String[] {"Tick Marking Color",
                "Tick Marking Shadow Color",
                "Tick Marking Length",
                "Tick Marking Width"};
        String[] summaries = new String[] {"Color of marking",
                "Color of marking shadow",
                "Marking Length",
                "Marking Width"};
        ArrayList<OptionData> optionsList = new ArrayList<>();

        for (int index = 0; index < titles.length; index++) {
            optionsList.add(new OptionData(titles[index],
                    summaries[index],
                    R.drawable.ic_color_lens_white_24dp));
        }

        RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                switch (position) {
                    case 0:
                        DialogFactory.ColorDialogListener minuteDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setSeparatorColor(getColorPickerView().getColor());
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter tick color:",
                                minuteDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getSeparatorColor());
                        break;
                    case 1:
                        DialogFactory.ColorDialogListener minuteShadowDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setSeparatorShadowColor(getColorPickerView().getColor());
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter tick shadow color:",
                                minuteShadowDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getSeparatorShadowColor());
                        break;
                    case 2:
                        DialogFactory.SeekBarDialogListener minuteLengthDialogListener = new DialogFactory.SeekBarDialogListener() {
                            @Override
                            public void onValueChanged(SeekBar seekBar,
                                    int progress,
                                    boolean fromUser) {
                                setValueText("" + (progress / 10f));
                            }

                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setTickLengthRatio(getValue() / 10f);
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showSeekBarDialog(appContext, "Tick marking length", "% of radius", minuteLengthDialogListener, 1000, Math.round(AppSettings.getTickLengthRatio() * 10f), -1, R.string.cancel_button, R.string.ok_button);
                        break;
                    case 3:
                        DialogFactory.SeekBarDialogListener minuteWidthDialogListener = new DialogFactory.SeekBarDialogListener() {
                            @Override
                            public void onValueChanged(SeekBar seekBar,
                                    int progress,
                                    boolean fromUser) {
                                setValueText("" + (progress / 10f));
                            }

                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setTickWidth(getValue() / 10f);
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showSeekBarDialog(appContext, "Tick marking width", "dp", minuteWidthDialogListener, 200, Math.round(AppSettings.getTickWidth() * 10f), -1, R.string.cancel_button, R.string.ok_button);
                        break;
                }

            }
        };

        OptionsListAdapter titlesAdapter = new OptionsListAdapter(appContext,
                optionsList,
                -1,
                listener);

        recyclerView.setAdapter(titlesAdapter);
    }

    private void showMinuteOptions(int numToShow) {

        ArrayList<OptionData> optionsList = new ArrayList<>();

        for (int index = 0; index < numToShow; index++) {
            optionsList.add(new OptionData(options[index].replaceAll("TYPE", "Minute"),
                    optionDescriptions[index].replaceAll("TYPE", "Minute"),
                    R.drawable.ic_color_lens_white_24dp));
        }

        RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                switch (position) {
                    case 0:
                        DialogFactory.ColorDialogListener minuteDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setMinuteColor(getColorPickerView().getColor());
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter minute color:",
                                minuteDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getMinuteColor());
                        break;
                    case 1:
                        DialogFactory.ColorDialogListener minuteShadowDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setMinuteShadowColor(getColorPickerView().getColor());
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter minute shadow color:",
                                minuteShadowDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getMinuteShadowColor());
                        break;
                    case 2:
                        DialogFactory.SeekBarDialogListener minuteLengthDialogListener = new DialogFactory.SeekBarDialogListener() {
                            @Override
                            public void onValueChanged(SeekBar seekBar,
                                    int progress,
                                    boolean fromUser) {
                                setValueText("" + (progress / 10f));
                            }

                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setMinuteLengthRatio(getValue() / 10f);
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showSeekBarDialog(appContext, "Minute hand length", "% of radius", minuteLengthDialogListener, 1000, Math.round(AppSettings.getMinuteLengthRatio() * 10f), -1, R.string.cancel_button, R.string.ok_button);
                        break;
                    case 3:
                        DialogFactory.SeekBarDialogListener minuteWidthDialogListener = new DialogFactory.SeekBarDialogListener() {
                            @Override
                            public void onValueChanged(SeekBar seekBar,
                                    int progress,
                                    boolean fromUser) {
                                setValueText("" + (progress / 10f));
                            }

                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setMinuteWidth(getValue() / 10f);
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showSeekBarDialog(appContext, "Minute hand width", "dp", minuteWidthDialogListener, 200, Math.round(AppSettings.getMinuteWidth() * 10f), -1, R.string.cancel_button, R.string.ok_button);
                        break;
                }

            }
        };

        OptionsListAdapter titlesAdapter = new OptionsListAdapter(appContext,
                optionsList,
                -1,
                listener);

        recyclerView.setAdapter(titlesAdapter);
    }

    private void showHourOptions(int numToShow) {

        ArrayList<OptionData> optionsList = new ArrayList<>();

        for (int index = 0; index < numToShow; index++) {
            optionsList.add(new OptionData(options[index].replaceAll("TYPE", "Hour"),
                    optionDescriptions[index].replaceAll("TYPE", "Hour"),
                    R.drawable.ic_color_lens_white_24dp));
        }

        RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                switch (position) {
                    case 0:
                        DialogFactory.ColorDialogListener hourDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setHourColor(getColorPickerView().getColor());
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter hour color:",
                                hourDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getHourColor());
                        break;
                    case 1:
                        DialogFactory.ColorDialogListener hourShadowDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setHourShadowColor(getColorPickerView().getColor());
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter hour shadow color:",
                                hourShadowDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getHourShadowColor());
                        break;
                    case 2:
                        DialogFactory.SeekBarDialogListener hourLengthDialogListener = new DialogFactory.SeekBarDialogListener() {
                            @Override
                            public void onValueChanged(SeekBar seekBar,
                                    int progress,
                                    boolean fromUser) {
                                setValueText("" + (progress / 10f));
                            }

                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setHourLengthRatio(getValue() / 10f);
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showSeekBarDialog(appContext, "Hour hand length", "% of radius", hourLengthDialogListener, 1000, Math.round(AppSettings.getHourLengthRatio() * 10f), -1, R.string.cancel_button, R.string.ok_button);
                        break;
                    case 3:
                        DialogFactory.SeekBarDialogListener hourWidthDialogListener = new DialogFactory.SeekBarDialogListener() {
                            @Override
                            public void onValueChanged(SeekBar seekBar,
                                    int progress,
                                    boolean fromUser) {
                                setValueText("" + (progress / 10f));
                            }

                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setHourWidth(getValue() / 10f);
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showSeekBarDialog(appContext, "Hour hand width", "dp", hourWidthDialogListener, 200, Math.round(AppSettings.getHourWidth() * 10f), -1, R.string.cancel_button, R.string.ok_button);
                        break;
                }

            }
        };

        OptionsListAdapter titlesAdapter = new OptionsListAdapter(appContext,
                optionsList,
                -1,
                listener);

        recyclerView.setAdapter(titlesAdapter);
    }

    private void showSecondOptions(int numToShow) {

        ArrayList<OptionData> optionsList = new ArrayList<>();

        for (int index = 0; index < numToShow; index++) {
            optionsList.add(new OptionData(options[index].replaceAll("TYPE", "Second"),
                    optionDescriptions[index].replaceAll("TYPE", "Second"),
                    R.drawable.ic_color_lens_white_24dp));
        }

        RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                switch (position) {
                    case 0:
                        DialogFactory.ColorDialogListener secondDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setSecondColor(getColorPickerView().getColor());
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter second color:",
                                secondDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getSecondColor());
                        break;
                    case 1:
                        DialogFactory.ColorDialogListener secondShadowDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setSecondShadowColor(getColorPickerView().getColor());
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter second shadow color:",
                                secondShadowDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getSecondShadowColor());
                        break;
                    case 2:
                        DialogFactory.SeekBarDialogListener secondLengthDialogListener = new DialogFactory.SeekBarDialogListener() {
                            @Override
                            public void onValueChanged(SeekBar seekBar,
                                    int progress,
                                    boolean fromUser) {
                                setValueText("" + (progress / 10f));
                            }

                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setSecondLengthRatio(getValue() / 10f);
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showSeekBarDialog(appContext, "Second hand length", "% of radius", secondLengthDialogListener, 1000, Math.round(AppSettings.getSecondLengthRatio() * 10f), -1, R.string.cancel_button, R.string.ok_button);
                        break;
                    case 3:
                        DialogFactory.SeekBarDialogListener secondWidthDialogListener = new DialogFactory.SeekBarDialogListener() {
                            @Override
                            public void onValueChanged(SeekBar seekBar,
                                    int progress,
                                    boolean fromUser) {
                                setValueText("" + (progress / 10f));
                            }

                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setSecondWidth(getValue() / 10f);
                                redraw();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showSeekBarDialog(appContext, "Second hand width", "dp", secondWidthDialogListener, 200, Math.round(AppSettings.getSecondWidth() * 10f), -1, R.string.cancel_button, R.string.ok_button);
                        break;
                }

            }
        };

        OptionsListAdapter titlesAdapter = new OptionsListAdapter(appContext,
                optionsList,
                -1,
                listener);

        recyclerView.setAdapter(titlesAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(appContext).registerReceiver(broadcastReceiver,
                intentFilter);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        loadImageFile();
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(appContext).unregisterReceiver(broadcastReceiver);
        syncSettings();
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (!((Activity) appContext).isFinishing()) {

        }
    }

    private void sendMessage(final String messagePath, final String data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(
                        googleApiClient).await();

                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            googleApiClient,
                            node.getId(),
                            messagePath,
                            data.getBytes()).await();
                    if (!result.getStatus().isSuccess()) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(appContext,
                                        "Error syncing to Wear",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.watch_face_container:
                preferenceList.setVisibility(View.VISIBLE);
                recyclerView.setAdapter(null);
                recyclerView.setVisibility(View.GONE);
                break;

        }

    }
}