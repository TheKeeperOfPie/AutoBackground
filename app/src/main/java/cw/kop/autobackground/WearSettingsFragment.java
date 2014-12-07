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
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
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
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
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
    private int imageSize = 0;
    private File lastBitmapFile = null;
    private IntentFilter imageFilter;
    private BroadcastReceiver imageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MainActivity.LOAD_NAV_PICTURE:
                    loadFaceImage();
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_wear);
        handler = new Handler(appContext.getMainLooper());
        imageFilter = new IntentFilter();
        imageFilter.addAction(MainActivity.LOAD_NAV_PICTURE);
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
                    imageSize = height;
                    loadFaceImage();
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
                    imageSize = width;
                    loadFaceImage();
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
        timeText = (TextView) view.findViewById(R.id.time);
        timeText.setText(timeFormat.format(new Date()));
        timeText.setTextColor(AppSettings.getWearTimeColor());
        timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, AppSettings.getWearTimeSize());
        timeText.setOnClickListener(this);

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

    private void syncSettings() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                PutDataMapRequest dataMap = PutDataMapRequest.create("/settings");
                dataMap.getDataMap().putString("time_type", AppSettings.getWearTimeType());
                dataMap.getDataMap().putInt("time_color", AppSettings.getWearTimeColor());
                dataMap.getDataMap().putFloat("time_size", AppSettings.getWearTimeSize());
                dataMap.getDataMap().putLong("time", new Date().getTime());
                Wearable.DataApi.putDataItem(googleApiClient, dataMap.asPutDataRequest());
                Log.i(TAG, "syncSettings");
            }
        }).start();

    }

    private void loadFaceImage() {

        if (imageSize > 0 && FileHandler.getCurrentBitmapFile() != null) {
            if (lastBitmapFile == null || !lastBitmapFile.equals(FileHandler.getCurrentBitmapFile())) {
                final Bitmap bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(
                                FileHandler.getCurrentBitmapFile().getAbsolutePath()),
                        imageSize,
                        imageSize);
                lastBitmapFile = FileHandler.getCurrentBitmapFile();
                image.setImageBitmap(bitmap);
            }
        }

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.time:
                showIconList();
                break;
        }

        preferenceList.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void clearHighLights() {
        preferenceList.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showIconList() {

        String[] iconTitles = appContext.getResources().getStringArray(R.array.wear_time_options);
        String[] iconSummaries = appContext.getResources().getStringArray(R.array.wear_time_options_descriptions);
        TypedArray iconIcons = appContext.getResources().obtainTypedArray(R.array.wear_time_options_icons);

        ArrayList<OptionData> optionsList = new ArrayList<>();

        for (int index = 0; index < iconTitles.length; index++) {
            optionsList.add(new OptionData(iconTitles[index],
                    iconSummaries[index],
                    iconIcons.getResourceId(index,
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

                                // TODO: Analog implementation
                                switch (position) {
                                    case 0:
                                        AppSettings.setWearTimeType(AppSettings.DIGITAL);
                                        break;
                                    case 1:
                                        AppSettings.setWearTimeType(AppSettings.ANALOG);
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

        iconIcons.recycle();

    }


    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(appContext).registerReceiver(imageReceiver, imageFilter);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        loadFaceImage();
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        LocalBroadcastManager.getInstance(appContext).unregisterReceiver(imageReceiver);
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