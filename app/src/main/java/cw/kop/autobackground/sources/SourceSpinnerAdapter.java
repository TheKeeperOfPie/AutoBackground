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

package cw.kop.autobackground.sources;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/10/2014.
 */
public class SourceSpinnerAdapter extends ArrayAdapter<String> {

    private List<String> itemList;
    private LayoutInflater inflater;
    private int textColor;
    private int backgroundColor;

    public SourceSpinnerAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);

        itemList = objects;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        textColor = AppSettings.getColorFilterInt(context);
        backgroundColor = context.getResources().getColor(AppSettings.getBackgroundColorResource());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.spinner_row, parent, false);
        }

        TextView spinnerText = (TextView) convertView.findViewById(R.id.spinner_text);
        spinnerText.setText(itemList.get(position));
        spinnerText.setTextColor(textColor);
        spinnerText.setBackgroundColor(backgroundColor);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.spinner_row, parent, false);
        }

        TextView spinnerText = (TextView) convertView.findViewById(R.id.spinner_text);
        spinnerText.setText(itemList.get(position));
        spinnerText.setTextColor(textColor);
        spinnerText.setBackgroundColor(backgroundColor);

        return convertView;
    }

}