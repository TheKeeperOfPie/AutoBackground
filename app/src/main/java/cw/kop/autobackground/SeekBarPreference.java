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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final String namespace = "http://schemas.android.com/apk/res/android";

    private SeekBar seekBar;
    private TextView valueTextView;
    private Context context;

    private String suffix;
    private int defaultValue, max, value, changedValue = 0;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        suffix = attrs.getAttributeValue(namespace, "dialogMessage");
        defaultValue = attrs.getAttributeIntValue(namespace, "defaultValue", 0);
        max = attrs.getAttributeIntValue(namespace, "max", 1);

    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params;
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        valueTextView = new TextView(context);
        valueTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        valueTextView.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                14,
                context.getResources().getDisplayMetrics()));
        layout.addView(valueTextView, params);

        TextView suffixTextView = new TextView(context);
        suffixTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        suffixTextView.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                8,
                context.getResources().getDisplayMetrics()));
        suffixTextView.setText(suffix);
        layout.addView(suffixTextView, params);

        seekBar = new SeekBar(context);
        seekBar.setOnSeekBarChangeListener(this);
        layout.addView(seekBar,
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        if (shouldPersist()) {
            value = getPersistedInt(defaultValue);
        }

        seekBar.setMax(max);
        seekBar.setProgress(value);
        return layout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        seekBar.setMax(max);
        seekBar.setProgress(value);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object initial) {
        super.onSetInitialValue(restore, initial);
        if (restore) {
            value = shouldPersist() ? getPersistedInt(defaultValue) : 0;
            changedValue = shouldPersist() ? getPersistedInt(defaultValue) : 0;
        }
        else {
            value = (Integer) initial;
            changedValue = (Integer) initial;
        }

    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        valueTextView.setText("" + ((float) value / 10));
        changedValue = value;
        callChangeListener(value);
    }

    public void onStartTrackingTouch(SeekBar seek) {
    }

    public void onStopTrackingTouch(SeekBar seek) {
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            if (shouldPersist()) {
                persistInt(changedValue);
            }
        }
    }
}