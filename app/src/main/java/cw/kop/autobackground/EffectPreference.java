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

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import cw.kop.autobackground.settings.AppSettings;

public class EffectPreference extends DialogPreference {

    private static final String androidNamespace = "http://schemas.android.com/apk/res/android";

    private final Context appContext;
    private NumberPicker valuePicker;

    private String key;
    private int defaultValue, maxValue;

    public EffectPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        appContext = context;

        key = attrs.getAttributeValue(androidNamespace, "key");
        maxValue = attrs.getAttributeIntValue(androidNamespace, "max", 1);
        defaultValue = attrs.getAttributeIntValue(androidNamespace, "defaultValue", 0);

        Log.i("EP", "EffectPreference key: " + key);
    }

    @Override
    protected View onCreateDialogView() {
        final LinearLayout valueLayout = new LinearLayout(appContext);

        valuePicker = new CustomNumberPicker(appContext);
        valuePicker.setMaxValue(maxValue);

        TextView suffixText = new TextView(appContext);
        suffixText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        suffixText.setText("%");
        suffixText.setGravity(Gravity.CENTER);

        Log.i("EP", "Value set: " + AppSettings.getEffectValue(key));
        valuePicker.setValue(AppSettings.getEffectValue(key));

        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.75f);
        LinearLayout.LayoutParams suffixParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        suffixParams.weight = 0.25f;

        valueLayout.addView(valuePicker, valueParams);
        valueLayout.addView(suffixText, suffixParams);

        LinearLayout containerLayout = new LinearLayout(appContext);
        containerLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, appContext.getResources().getDisplayMetrics()));
        valueLayout.setPadding(padding, padding, padding, padding);

        Button defaultButton = new Button(appContext);
        defaultButton.setText("Default");
        defaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                valuePicker.setValue(defaultValue);
            }
        });

        LinearLayout.LayoutParams defaultParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);

        containerLayout.addView(valueLayout);
        containerLayout.addView(defaultButton, defaultParams);

        return containerLayout;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            AppSettings.setEffect(key, valuePicker.getValue());
        }
    }
}
