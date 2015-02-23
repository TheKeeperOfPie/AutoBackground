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

package cw.kop.autobackground.images;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 1/31/2015.
 */
public class DropboxAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private List<Entry> entries;
    private Entry topDir;
    private Entry mainDir;
    private boolean finished;
    private int colorFilterInt;

    public DropboxAdapter(Activity activity) {
        this.inflater = activity.getLayoutInflater();
        this.colorFilterInt = AppSettings.getColorFilterInt(activity);
    }

    public void setDirs(Entry topDir, Entry mainDir) {
        this.topDir = topDir;
        this.mainDir = mainDir;
        this.entries = mainDir.contents == null ? new ArrayList<Entry>() : mainDir.contents;
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Entry getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (entries.size() > 0) {

            Entry entry = entries.get(position);

            TextView fileTitle;
            TextView fileSummary;
            ImageView fileImage;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.file_row, parent, false);

                fileTitle = (TextView) convertView.findViewById(R.id.file_title);
                fileSummary = (TextView) convertView.findViewById(R.id.file_summary);
                fileImage = (ImageView) convertView.findViewById(R.id.file_image);

                fileImage.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);

                convertView.setTag(new ViewHolder(fileTitle, fileSummary, fileImage));
            }

            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            fileTitle = viewHolder.fileTitle;
            fileSummary = viewHolder.fileSummary;
            fileImage = viewHolder.fileImage;

            if (entry.isDir) {
                fileImage.setImageResource(R.drawable.ic_folder_white_24dp);
            }
            else {
                fileImage.setImageResource(R.drawable.ic_insert_drive_file_white_24dp);
            }

            fileTitle.setText(entry.fileName());
            fileSummary.setText(entry.isDir ? "" : entry.size);

            return convertView;
        }
        return null;
    }

    public Entry getMainDir() {
        return mainDir;
    }

    public void setDir(Entry dir) {
        entries = dir.contents == null ? new ArrayList<Entry>() : dir.contents;
        mainDir = dir;
        notifyDataSetChanged();
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public Boolean backDirectory() {

        return finished || topDir.path.equals(mainDir.path);

    }

    private static class ViewHolder {

        public final TextView fileTitle;
        public final TextView fileSummary;
        public final ImageView fileImage;

        public ViewHolder(TextView fileTitle,
                TextView fileSummary,
                ImageView fileImage) {
            this.fileTitle = fileTitle;
            this.fileSummary = fileSummary;
            this.fileImage = fileImage;
        }
    }

}
