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

public class EventListenerService extends WearableListenerService {

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
                    case "/image":
                        Asset profileAsset = dataMap.getAsset("faceImage");
                        lastBitmap = currentBitmap;
                        currentBitmap = loadBitmapFromAsset(profileAsset);

                        if (currentBitmap != null) {
                            Intent intent = new Intent(WatchFace.LOAD_IMAGE);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                                    intent);
                            Log.i(TAG, "Bitmap received");
                        }
                        break;
                    case "/settings":
                        WearSettings.setTimeType(dataMap.getString("time_type", WearSettings.DIGITAL));
                        WearSettings.setTimeColor(dataMap.getInt("time_color", 0xFFFFFFFF));
                        WearSettings.setTimeShadowColor(dataMap.getInt("time_shadow_color", 0xFF000000));
                        WearSettings.setTimeSize(dataMap.getFloat("time_size", 24));

                        WearSettings.setAnalogHourColor(dataMap.getInt("analog_hour_color", 0xFFFFFFFF));
                        WearSettings.setAnalogHourShadowColor(dataMap.getInt("analog_hour_shadow_color", 0xFF000000));
                        WearSettings.setAnalogMinuteColor(dataMap.getInt("analog_minute_color", 0xFFFFFFFF));
                        WearSettings.setAnalogMinuteShadowColor(dataMap.getInt("analog_minute_shadow_color", 0xFF000000));
                        WearSettings.setAnalogSecondColor(dataMap.getInt("analog_second_color", 0xFFFFFFFF));
                        WearSettings.setAnalogSecondShadowColor(dataMap.getInt("analog_second_shadow_color", 0xFF000000));

                        Intent intent = new Intent(WatchFace.LOAD_SETTINGS);
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
