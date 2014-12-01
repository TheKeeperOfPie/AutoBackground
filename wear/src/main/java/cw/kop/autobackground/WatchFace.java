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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WatchFace extends WatchFaceActivity {

//    private final static IntentFilter intentFilter;
    private boolean isDimmed = false;
    private ImageView faceImage;
    private boolean test = false;

//    static {
//        intentFilter = new IntentFilter();
//        intentFilter.addAction(Intent.ACTION_TIME_TICK);
//        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
//        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
//    }

    TextView time, battery;
    private final String TIME_FORMAT_DISPLAYED = "KK:mm a";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.round_activity_main);

        faceImage = (ImageView) findViewById(R.id.face_image);
        faceImage.setImageDrawable(new ColorDrawable(0xFF000000));

        Intent service = new Intent(this, EventListenerService.class);
        startService(service);

//        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
//        mTimeInfoReceiver.onReceive(this, registerReceiver(null, intentFilter));
//        registerReceiver(mTimeInfoReceiver, intentFilter);

    }

//    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
//        @Override
//        public void onReceive(Context arg0, Intent intent) {
//            battery.setText(String.valueOf(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) + "%"));
//        }
//    };
//
//    private BroadcastReceiver mTimeInfoReceiver = new BroadcastReceiver(){
//        @Override
//        public void onReceive(Context arg0, Intent intent) {
//            Date date = new Date();
//            time.setText(new SimpleDateFormat(TIME_FORMAT_DISPLAYED).format(date));
//            setColorOfText();
//        }
//    };
//
//    private void setColorOfText(){
//        time.setTextColor(isDimmed ? Color.GRAY : Color.RED);
//        battery.setTextColor(isDimmed ? Color.GRAY : Color.RED);
//    }

    @Override
    public void onScreenDim() {
        isDimmed = true;
    }

    @Override
    public void onScreenAwake() {
        isDimmed = false;
    }

}