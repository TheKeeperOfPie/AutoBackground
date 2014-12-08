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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;

public class WearSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener, View.OnClickListener {

    private static final String TAG = WearSettingsFragment.class.getName();
    private Context appContext;
    private GoogleApiClient googleApiClient;
    private boolean isWearConnected = false;
    private Handler handler;
    private DateFormat timeFormat;
    private TextView timeText;
    private TextView dateText;
    private ImageView image;
    private View watchFace;
    private ImageView watchContainer;
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
                    loadFaceImage();
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_wear);
        handler = new Handler(appContext.getMainLooper());
        intentFilter = new IntentFilter();
        intentFilter.addAction(MainActivity.LOAD_NAV_PICTURE);
        intentFilter.addAction(MainActivity.DRAWER_OPENED);
        intentFilter.addAction(MainActivity.DRAWER_CLOSED);
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
            watchContainer = (ImageView) view.findViewById(R.id.watch_face_container);
            watchContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (getView() == null) {
                        return;
                    }
                    ViewGroup.LayoutParams watchContainerParams= watchContainer.getLayoutParams();
                    watchContainerParams.width = watchContainer.getHeight();
                    watchContainer.setLayoutParams(watchContainerParams);
                    watchContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            });
            watchFace = view.findViewById(R.id.watch_face);
            watchFace.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {


                    int height = watchContainer.getHeight();
                    ViewGroup.LayoutParams watchFaceParams = watchFace.getLayoutParams();
                    watchFaceParams.height = height;
                    watchFaceParams.width = height;
                    watchFace.setLayoutParams(watchFaceParams);
                    watchFace.setPadding(Math.round(height * 0.278f),
                            Math.round(height * 0.23f),
                            Math.round(height * 0.278f),
                            Math.round(height * 0.33f));
                    watchFace.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            });
            watchFace.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    preferenceList.setVisibility(View.VISIBLE);
                    recyclerView.setAdapter(null);
                    recyclerView.setVisibility(View.GONE);
                }
            });
        }
        else {
            view = inflater.inflate(R.layout.wear_settings_layout, container, false);
            watchContainer = (ImageView) view.findViewById(R.id.watch_face_container);
            watchContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (getView() == null) {
                        return;
                    }
                    ViewGroup.LayoutParams watchContainerParams= watchContainer.getLayoutParams();
                    watchContainerParams.height = watchContainer.getWidth();
                    watchContainer.setLayoutParams(watchContainerParams);
                    watchContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            });
            watchFace = view.findViewById(R.id.watch_face);
            watchFace.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int width = displayMetrics.widthPixels;
                    ViewGroup.LayoutParams watchFaceParams = watchFace.getLayoutParams();
                    watchFaceParams.height = width;
                    watchFaceParams.width = width;
                    watchFace.setLayoutParams(watchFaceParams);
                    watchFace.setPadding(Math.round(width * 0.278f),
                            Math.round(width * 0.23f),
                            Math.round(width * 0.278f),
                            Math.round(width * 0.33f));
                    watchFace.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            });
            watchFace.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    preferenceList.setVisibility(View.VISIBLE);
                    recyclerView.setAdapter(null);
                    recyclerView.setVisibility(View.GONE);
                }
            });
        }

        image = (ImageView) view.findViewById(R.id.face_image);

        timeFormat = android.text.format.DateFormat.getTimeFormat(appContext);
        timeText = (TextView) view.findViewById(R.id.time_digital);
        timeText.setText(timeFormat.format(new Date()));
        timeText.setTextColor(AppSettings.getWearTimeColor());
        timeText.setShadowLayer(5f, -1f, -1f, AppSettings.getWearTimeShadowColor());
        timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, AppSettings.getWearTimeSize());
        timeText.setOnClickListener(this);

        surfaceView = (SurfaceView) view.findViewById(R.id.surface_view);
        surfaceView.setZOrderOnTop(true);
        surfaceView.setOnClickListener(this);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                drawAnalog();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        if (AppSettings.getWearTimeType().equals(AppSettings.DIGITAL)) {
            surfaceView.setVisibility(View.GONE);
            timeText.setVisibility(View.VISIBLE);
        }
        else {
            surfaceView.setVisibility(View.VISIBLE);
            timeText.setVisibility(View.GONE);
        }

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

        return view;
    }

    private void drawAnalog() {

        if (!AppSettings.getWearTimeType().equals(AppSettings.ANALOG)) {
            return;
        }

        canvas = surfaceView.getHolder().lockCanvas();

        if (canvas == null) {
            return;
        }

        Time time = new Time();
        time.setToNow();

        float hour = time.hour + time.minute / 60;
        float minute = time.minute + time.second / 60;
        float second = time.second;
        float centerX = watchContainer.getWidth() * 0.5f;
        float centerY = watchContainer.getWidth() * 0.45f;
        float radius = watchContainer.getWidth() * 0.222f;

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Paint paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(AppSettings.getWearAnalogHourShadowColor());
        paint.setStrokeWidth(7.0f);
        canvas.drawLine(centerX,
                centerY,
                (float) (centerX + radius / 2 * Math.cos(Math.toRadians(hour * 15f - 90f))),
                (float) (centerY + radius / 2 * Math.sin(Math.toRadians(hour * 15f - 90f))),
                paint);
        paint.setColor(AppSettings.getWearAnalogHourColor());
        paint.setStrokeWidth(5.0f);
        canvas.drawLine(centerX,
                centerY,
                (float) (centerX + radius / 2 * Math.cos(Math.toRadians(hour * 15f - 90f))),
                (float) (centerY + radius / 2 * Math.sin(Math.toRadians(hour * 15f - 90f))),
                paint);

        paint.setColor(AppSettings.getWearAnalogMinuteShadowColor());
        paint.setStrokeWidth(5.0f);
        canvas.drawLine(centerX,
                centerY,
                (float) (centerX + radius / 1.5 * Math.cos(Math.toRadians(minute * 6f - 90f))),
                (float) (centerY + radius / 1.5 * Math.sin(Math.toRadians(minute * 6f - 90f))),
                paint);
        paint.setColor(AppSettings.getWearAnalogMinuteColor());
        paint.setStrokeWidth(3.0f);
        canvas.drawLine(centerX,
                centerY,
                (float) (centerX + radius / 1.5 * Math.cos(Math.toRadians(minute * 6f - 90f))),
                (float) (centerY + radius / 1.5 * Math.sin(Math.toRadians(minute * 6f - 90f))),
                paint);

        paint.setColor(AppSettings.getWearAnalogSecondShadowColor());
        paint.setStrokeWidth(3.0f);
        canvas.drawLine(centerX,
                centerY,
                (float) (centerX + radius * Math.cos(Math.toRadians(second * 6f - 90f))),
                (float) (centerY + radius * Math.sin(Math.toRadians(second * 6f - 90f))),
                paint);
        paint.setColor(AppSettings.getWearAnalogSecondColor());
        paint.setStrokeWidth(2.0f);
        canvas.drawLine(centerX,
                centerY,
                (float) (centerX + radius * Math.cos(Math.toRadians(second * 6f - 90f))),
                (float) (centerY + radius * Math.sin(Math.toRadians(second * 6f - 90f))),
                paint);

        surfaceView.getHolder().unlockCanvasAndPost(canvas);
    }

    private void syncSettings() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                PutDataMapRequest dataMap = PutDataMapRequest.create("/settings");
                dataMap.getDataMap().putString("time_type", AppSettings.getWearTimeType());
                dataMap.getDataMap().putInt("time_color", AppSettings.getWearTimeColor());
                dataMap.getDataMap().putInt("time_shadow_color",
                        AppSettings.getWearTimeShadowColor());
                dataMap.getDataMap().putFloat("time_size", AppSettings.getWearTimeSize());
                dataMap.getDataMap().putLong("time", new Date().getTime());
                dataMap.getDataMap().putInt("analog_hour_color",
                        AppSettings.getWearAnalogHourColor());
                dataMap.getDataMap().putInt("analog_hour_shadow_color",
                        AppSettings.getWearAnalogHourShadowColor());
                dataMap.getDataMap().putInt("analog_minute_color",
                        AppSettings.getWearAnalogMinuteColor());
                dataMap.getDataMap().putInt("analog_minute_shadow_color",
                        AppSettings.getWearAnalogMinuteShadowColor());
                dataMap.getDataMap().putInt("analog_second_color",
                        AppSettings.getWearAnalogSecondColor());
                dataMap.getDataMap().putInt("analog_second_shadow_color",
                        AppSettings.getWearAnalogSecondShadowColor());
                Wearable.DataApi.putDataItem(googleApiClient, dataMap.asPutDataRequest());
                Log.i(TAG, "syncSettings");
            }
        }).start();

    }

    private void loadFaceImage() {

        File imageFile = FileHandler.getCurrentBitmapFile();

        if (imageFile != null && imageFile.exists()) {
            Picasso.with(appContext).load(imageFile).centerCrop().fit().into(image);
            Log.i(TAG, "Loading image");
        }

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.time_digital:
                showDigitalOptions();
                break;
            case R.id.surface_view:
                showAnalogOptions();
        }

        preferenceList.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void clearHighLights() {
        preferenceList.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showDigitalOptions() {

        String[] titles = appContext.getResources().getStringArray(R.array.wear_time_digital_options);
        String[] summaries = appContext.getResources().getStringArray(R.array.wear_time_digital_options_descriptions);
        TypedArray icons = appContext.getResources().obtainTypedArray(R.array.wear_time_digital_options_icons);

        ArrayList<OptionData> optionsList = new ArrayList<>();

        for (int index = 0; index < titles.length; index++) {
            optionsList.add(new OptionData(titles[index],
                    summaries[index],
                    icons.getResourceId(index,
                            R.color.TRANSPARENT_BACKGROUND)));
        }

        RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                switch (position) {
                    case 0:
                        DialogFactory.ListDialogListener listDialogListener = new DialogFactory.ListDialogListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent,
                                    View view,
                                    int position,
                                    long id) {

                                switch (position) {
                                    case 0:
                                        AppSettings.setWearTimeType(AppSettings.DIGITAL);
                                        surfaceView.setVisibility(View.GONE);
                                        timeText.setVisibility(View.VISIBLE);
                                        recyclerView.setAdapter(null);
                                        preferenceList.setVisibility(View.VISIBLE);
                                        recyclerView.setVisibility(View.GONE);
                                        break;
                                    case 1:
                                        AppSettings.setWearTimeType(AppSettings.ANALOG);
                                        surfaceView.setVisibility(View.VISIBLE);
                                        timeText.setVisibility(View.GONE);
                                        recyclerView.setAdapter(null);
                                        preferenceList.setVisibility(View.VISIBLE);
                                        recyclerView.setVisibility(View.GONE);
                                        drawAnalog();
                                        break;
                                }
                                dismissDialog();
                            }
                        };

                        DialogFactory.showListDialog(appContext, "Watch face", listDialogListener, R.array.wear_time_types);
                        break;
                    case 1:

                        DialogFactory.ColorDialogListener colorDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setWearTimeColor(getColorPickerView().getColor());
                                timeText.setTextColor(getColorPickerView().getColor());
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext, "Enter time color:", colorDialogListener, -1, R.string.cancel_button, R.string.ok_button, AppSettings.getWearTimeColor());

                        break;
                    case 2:
                        DialogFactory.ColorDialogListener shadowColorDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setWearTimeShadowColor(getColorPickerView().getColor());
                                timeText.setShadowLayer(5.0f, -1f, -1f, getColorPickerView().getColor());
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext, "Enter shadow color:", shadowColorDialogListener, -1, R.string.cancel_button, R.string.ok_button, AppSettings.getWearTimeShadowColor());
                        break;
                    case 3:

                        DialogFactory.SeekBarDialogListener listener = new DialogFactory.SeekBarDialogListener() {

                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setWearTimeSize(getValue());
                                timeText.setTextSize(getValue());
                                this.dismissDialog();
                            }

                            @Override
                            public void onValueChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                setValueText("" + progress);
                            }
                        };

                        DialogFactory.showSeekBarDialog(appContext,
                                "Time text size",
                                "",
                                listener,
                                50,
                                (int) AppSettings.getWearTimeSize(),
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button);

                        break;
                }

            }
        };

        OptionsListAdapter titlesAdapter = new OptionsListAdapter(appContext,
                optionsList,
                -1,
                listener);

        recyclerView.setAdapter(titlesAdapter);

        icons.recycle();

    }

    private void showAnalogOptions() {


        String[] titles = appContext.getResources().getStringArray(R.array.wear_time_analog_options);
        String[] summaries = appContext.getResources().getStringArray(R.array.wear_time_analog_options_descriptions);
        TypedArray icons = appContext.getResources().obtainTypedArray(R.array.wear_time_analog_options_icons);

        ArrayList<OptionData> optionsList = new ArrayList<>();

        for (int index = 0; index < titles.length; index++) {
            optionsList.add(new OptionData(titles[index],
                    summaries[index],
                    icons.getResourceId(index,
                            R.color.TRANSPARENT_BACKGROUND)));
        }

        RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                Toast.makeText(appContext, "Selected: " + position, Toast.LENGTH_SHORT).show();

                switch (position) {
                    case 0:
                        DialogFactory.ListDialogListener listDialogListener = new DialogFactory.ListDialogListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent,
                                    View view,
                                    int position,
                                    long id) {

                                switch (position) {
                                    case 0:
                                        AppSettings.setWearTimeType(AppSettings.DIGITAL);
                                        surfaceView.setVisibility(View.GONE);
                                        timeText.setVisibility(View.VISIBLE);
                                        recyclerView.setAdapter(null);
                                        preferenceList.setVisibility(View.VISIBLE);
                                        recyclerView.setVisibility(View.GONE);
                                        break;
                                    case 1:
                                        AppSettings.setWearTimeType(AppSettings.ANALOG);
                                        surfaceView.setVisibility(View.VISIBLE);
                                        timeText.setVisibility(View.GONE);
                                        recyclerView.setAdapter(null);
                                        preferenceList.setVisibility(View.VISIBLE);
                                        recyclerView.setVisibility(View.GONE);
                                        drawAnalog();
                                        break;
                                }
                                dismissDialog();
                            }
                        };

                        DialogFactory.showListDialog(appContext, "Watch face", listDialogListener, R.array.wear_time_types);
                        break;
                    case 1:
                        DialogFactory.ColorDialogListener hourDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setWearAnalogHourColor(getColorPickerView().getColor());
                                drawAnalog();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter hour color:",
                                hourDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getWearAnalogHourColor());
                        break;
                    case 2:
                        DialogFactory.ColorDialogListener hourShadowDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setWearAnalogHourShadowColor(getColorPickerView().getColor());
                                drawAnalog();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter hour shadow color:",
                                hourShadowDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getWearAnalogHourColor());
                        break;
                    case 3:
                        DialogFactory.ColorDialogListener minuteDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setWearAnalogMinuteColor(getColorPickerView().getColor());
                                drawAnalog();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter minute color:",
                                minuteDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getWearAnalogMinuteColor());
                        break;
                    case 4:
                        DialogFactory.ColorDialogListener minuteShadowDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setWearAnalogMinuteShadowColor(getColorPickerView().getColor());
                                drawAnalog();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter minute shadow color:",
                                minuteShadowDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getWearAnalogMinuteShadowColor());
                        break;
                    case 5:
                        DialogFactory.ColorDialogListener secondDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setWearAnalogSecondColor(getColorPickerView().getColor());
                                drawAnalog();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext, "Enter second color:", secondDialogListener, -1, R.string.cancel_button, R.string.ok_button, AppSettings.getWearAnalogSecondColor());
                        break;
                    case 6:
                        DialogFactory.ColorDialogListener secondShadowDialogListener = new DialogFactory.ColorDialogListener() {
                            @Override
                            public void onClickRight(View v) {
                                AppSettings.setWearAnalogSecondShadowColor(getColorPickerView().getColor());
                                drawAnalog();
                                this.dismissDialog();
                            }
                        };

                        DialogFactory.showColorPickerDialog(appContext,
                                "Enter second shadow color:",
                                secondShadowDialogListener,
                                -1,
                                R.string.cancel_button,
                                R.string.ok_button,
                                AppSettings.getWearAnalogSecondShadowColor());
                        break;
                }

            }
        };

        OptionsListAdapter titlesAdapter = new OptionsListAdapter(appContext,
                optionsList,
                -1,
                listener);

        recyclerView.setAdapter(titlesAdapter);

        icons.recycle();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(appContext).registerReceiver(broadcastReceiver, intentFilter);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        loadFaceImage();
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(appContext).unregisterReceiver(broadcastReceiver);
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
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

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
                                Toast.makeText(appContext, "Error syncing to Wear", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }
}