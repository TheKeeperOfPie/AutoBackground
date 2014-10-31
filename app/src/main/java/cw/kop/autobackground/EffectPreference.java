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
import android.preference.Preference;
import android.util.AttributeSet;

public class EffectPreference extends Preference {

    private static final String androidNamespace = "http://schemas.android.com/apk/res/android";

    private String title, summary;
    private int defaultValue, maxValue;

    public EffectPreference(Context context, AttributeSet attrs) {
        super(context, attrs);//, AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME) ? R.style.LightDialogTheme : R.style.DarkDialogTheme);

        title = context.getResources().getString(attrs.getAttributeResourceValue(androidNamespace, "title", R.string.title_effects_settings));
        summary = context.getResources().getString(attrs.getAttributeResourceValue(androidNamespace, "summary", R.string.title_effects_settings));
        maxValue = attrs.getAttributeIntValue(androidNamespace, "max", 1);
        defaultValue = attrs.getAttributeIntValue(androidNamespace, "defaultValue", 0);

        setTitle(title);
        setSummary(summary);

    }

    public String getTitle() {
        return title;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public int getDefaultValue() {
        return defaultValue;
    }
}
