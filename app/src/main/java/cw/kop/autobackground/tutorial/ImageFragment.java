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

package cw.kop.autobackground.tutorial;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/23/2014.
 */
public class ImageFragment extends Fragment {

    private static final String TAG = ImageFragment.class.getCanonicalName();
    private Context appContext;

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
    public View onCreateView(LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tutorial_image_fragment, container, false);
        int colorFilterInt = AppSettings.getColorFilterInt(appContext);

        TextView titleText = (TextView) view.findViewById(R.id.title_text);
        titleText.setTextColor(colorFilterInt);
        titleText.setText("Wallpaper layout");

        TextView tutorialText = (TextView) view.findViewById(R.id.tutorial_text);
        tutorialText.setTextColor(colorFilterInt);
        tutorialText.setText("Would you like to use a single image or double image layout?");

        final ImageView deviceImage = (ImageView) view.findViewById(R.id.device_image);
        deviceImage.setVisibility(AppSettings.useDoubleImage() ? View.INVISIBLE : View.VISIBLE);

        final ImageView deviceDoubleImage = (ImageView) view.findViewById(R.id.device_double_image);
        deviceDoubleImage.setVisibility(AppSettings.useDoubleImage() ?
                View.VISIBLE :
                View.INVISIBLE);


        Button singleButton = (Button) view.findViewById(R.id.single_button);
        singleButton.setText("Single");
        singleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppSettings.setUseDoubleImage(false);
                deviceImage.setVisibility(View.VISIBLE);
                deviceDoubleImage.setVisibility(View.INVISIBLE);
            }
        });

        Button doubleButton = (Button) view.findViewById(R.id.double_button);
        doubleButton.setText("Double");
        doubleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppSettings.setUseDoubleImage(true);
                deviceDoubleImage.setVisibility(View.VISIBLE);
                deviceImage.setVisibility(View.INVISIBLE);
            }
        });

        return view;
    }
}
