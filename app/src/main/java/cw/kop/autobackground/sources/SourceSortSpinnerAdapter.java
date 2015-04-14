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
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/10/2014.
 */
public class SourceSortSpinnerAdapter extends BaseAdapter {

    private List<SortData> sortData;
    private LayoutInflater inflater;
    private int backgroundColor;

    public SourceSortSpinnerAdapter(Context context, List<SortData> sortData) {
        this.sortData = sortData;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        backgroundColor = context.getResources().getColor(AppSettings.getBackgroundColorResource());
    }

    public void setSortData(List<SortData> sortData) {
        this.sortData = sortData;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return sortData.size();
    }

    @Override
    public Object getItem(int position) {
        return sortData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.spinner_row, parent, false);
        }

        TextView spinnerText = (TextView) convertView.findViewById(R.id.spinner_text);
        spinnerText.setText(sortData.get(position).getTitle());
        spinnerText.setBackgroundColor(backgroundColor);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {

        TextView spinnerText;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.spinner_row, parent, false);

            spinnerText = (TextView) convertView.findViewById(R.id.spinner_text);
            spinnerText.setBackgroundColor(backgroundColor);

            convertView.setTag(new ViewHolder(spinnerText));
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        spinnerText = viewHolder.spinnerText;

        spinnerText.setText(sortData.get(position).getTitle());

        return convertView;
    }

    private class ViewHolder {

        protected final TextView spinnerText;

        public ViewHolder(TextView spinnerText) {
            this.spinnerText = spinnerText;
        }
    }

}