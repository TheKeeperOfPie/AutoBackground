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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/23/2014.
 */
public class FabricFragment extends Fragment {

    private static final String TAG = FabricFragment.class.getCanonicalName();
    private Context appContext;
    private TextView fabricText;

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

        View view = inflater.inflate(R.layout.tutorial_fabric_fragment, container, false);
        int colorFilterInt = AppSettings.getColorFilterInt(appContext);

        fabricText = (TextView) view.findViewById(R.id.fabric_text);
        fabricText.setTextColor(colorFilterInt);
        resetFabricText(AppSettings.useFabric());

        CheckBox fabricCheckBox = (CheckBox) view.findViewById(R.id.fabric_check_box);
        fabricCheckBox.setChecked(AppSettings.useFabric());
        fabricCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppSettings.setUseFabric(isChecked);
                resetFabricText(isChecked);
            }
        });

        TextView titleText = (TextView) view.findViewById(R.id.title_text);
        titleText.setTextColor(colorFilterInt);
        titleText.setText("Crash reporting");

        TextView tutorialText = (TextView) view.findViewById(R.id.tutorial_text);
        tutorialText.setTextColor(colorFilterInt);
        tutorialText.setText(
                "Would you like to help the developer and report usage and crash data? " +
                        "No personal data is collected.");

        return view;
    }

    private void resetFabricText(boolean use) {

        SpannableString text;
        if (use) {
            text = new SpannableString("Thanks!");
            text.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.BLUE_OPAQUE)),
                    0,
                    text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        else {
            text = new SpannableString("Send reports?");
        }
        fabricText.setText(text);
        fabricText.invalidate();

    }

}