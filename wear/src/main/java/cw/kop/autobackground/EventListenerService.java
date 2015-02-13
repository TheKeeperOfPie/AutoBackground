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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import cw.kop.autobackground.shared.WearConstants;

public class EventListenerService extends WearableListenerService {

    public static final String LOAD_IMAGE = "cw.kop.autobackground.EventListenerService.LOAD_IMAGE";
    public static final String LOAD_SETTINGS = "cw.kop.autobackground.EventListenerService.LOAD_SETTINGS";
    private static final String TAG = EventListenerService.class.getCanonicalName();
    private static final int TIMEOUT_MS = 2000;
    private static Bitmap currentBitmap = null;
    private static Bitmap lastBitmap = null;
    private GoogleApiClient googleApiClient;

    public EventListenerService() {
    }

    public static Bitmap getBitmap() {
        return currentBitmap;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                        // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();

        Log.i(TAG, "EventListenerService created");

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        Log.i(TAG, "Message received");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            DataItem dataItem = event.getDataItem();
            DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                switch (dataItem.getUri().getPath()) {
                    case WearConstants.IMAGE:
                        Asset profileAsset = dataMap.getAsset("faceImage");
                        lastBitmap = currentBitmap;
                        currentBitmap = loadBitmapFromAsset(profileAsset);

                        if (currentBitmap != null) {
                            Intent intent = new Intent(LOAD_IMAGE);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                                    intent);
                            Log.i(TAG, "Bitmap received");
                        }
                        break;
                    case WearConstants.SETTINGS:
                        WearSettings.setTimeType(dataMap.getString(WearConstants.TIME_TYPE,
                                WearSettings.DIGITAL));
                        WearSettings.setTimeOffset(dataMap.getLong(WearConstants.TIME_OFFSET, 0));
                        WearSettings.setUseTimePalette(dataMap.getBoolean(WearConstants.USE_TIME_PALETTE,
                                false));

                        WearSettings.setSeparatorText(dataMap.getString(WearConstants.SEPARATOR_TEXT,
                                ":"));
                        WearSettings.setSeparatorColor(dataMap.getInt(WearConstants.SEPARATOR_COLOR,
                                0xFFFFFFFF));
                        WearSettings.setSeparatorShadowColor(dataMap.getInt(WearConstants.
                                        SEPARATOR_SHADOW_COLOR,
                                0xFF000000));
                        WearSettings.setHourColor(dataMap.getInt(WearConstants.HOUR_COLOR,
                                0xFFFFFFFF));
                        WearSettings.setHourShadowColor(dataMap.getInt(WearConstants.HOUR_SHADOW_COLOR,
                                0xFF000000));
                        WearSettings.setMinuteColor(dataMap.getInt(WearConstants.MINUTE_COLOR,
                                0xFFFFFFFF));
                        WearSettings.setMinuteShadowColor(dataMap.getInt(WearConstants.MINUTE_SHADOW_COLOR,
                                0xFF000000));
                        WearSettings.setSecondColor(dataMap.getInt(WearConstants.SECOND_COLOR,
                                0xFFFFFFFF));
                        WearSettings.setSecondShadowColor(dataMap.getInt(WearConstants.SECOND_SHADOW_COLOR,
                                0xFF000000));

                        WearSettings.setHourLengthRatio(dataMap.getFloat(WearConstants.HOUR_LENGTH_RATIO,
                                50f));
                        WearSettings.setMinuteLengthRatio(dataMap.getFloat(WearConstants.
                                        MINUTE_LENGTH_RATIO,
                                66f));
                        WearSettings.setSecondLengthRatio(dataMap.getFloat(WearConstants.
                                        SECOND_LENGTH_RATIO,
                                100f));

                        WearSettings.setHourWidth(dataMap.getFloat(WearConstants.HOUR_WIDTH, 5.0f));
                        WearSettings.setMinuteWidth(dataMap.getFloat(WearConstants.MINUTE_WIDTH, 3.0f));
                        WearSettings.setSecondWidth(dataMap.getFloat(WearConstants.SECOND_WIDTH, 2.0f));

                        Intent intent = new Intent(LOAD_SETTINGS);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                                intent);
                        Log.i(TAG, "Settings received");
                        break;
                }

            }
        }

        Log.i(TAG, "Data changed");

        super.onDataChanged(dataEvents);
    }


    public static void recycleLast() {
        if (lastBitmap != null) {
            lastBitmap.recycle();
            lastBitmap = null;
        }
    }

    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                googleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                googleApiClient, asset).await().getInputStream();
        googleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }
}
