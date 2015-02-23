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

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import cw.kop.autobackground.R;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 1/31/2015.
 */
public class LocalImageAdapter extends BaseAdapter {

    private static final String TAG = LocalImageAdapter.class.getCanonicalName();
    private static final int BYTE_TO_MEBIBYTE = 1048576;
    private File topDir;
    private File startDir;
    private ArrayList<File> listFiles;
    private LayoutInflater inflater;
    private boolean finish;
    private int colorFilterInt;

    public LocalImageAdapter(Context activity, File topDir, File startDir) {
        listFiles = new ArrayList<>();
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.startDir = startDir;
        this.topDir = topDir;
        setDirectory(startDir);
        colorFilterInt = AppSettings.getColorFilterInt(activity);
    }

    @Override
    public int getCount() {
        return listFiles.size();
    }

    @Override
    public File getItem(int position) {
        return listFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (listFiles.size() > 0) {

            File file = listFiles.get(position);

            boolean isImage = FileHandler.getImageFileNameFilter().accept(null, file.getName());

            TextView fileTitle;
            TextView fileSummary;
            ImageView fileImage;
            ImageView fileImageFull;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.file_row, parent, false);

                fileTitle = (TextView) convertView.findViewById(R.id.file_title);
                fileSummary = (TextView) convertView.findViewById(R.id.file_summary);
                fileImage = (ImageView) convertView.findViewById(R.id.file_image);
                fileImageFull = (ImageView) convertView.findViewById(R.id.file_image_full);

                convertView.setTag(new ViewHolder(fileTitle, fileSummary, fileImage, fileImageFull));
            }

            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            fileTitle = viewHolder.fileTitle;
            fileSummary = viewHolder.fileSummary;
            fileImage = viewHolder.fileImage;
            fileImageFull = viewHolder.fileImageFull;

            if (isImage) {
                fileImageFull.setVisibility(View.VISIBLE);
                fileTitle.setVisibility(View.GONE);
                fileSummary.setVisibility(View.GONE);
                fileImage.setVisibility(View.GONE);

                fileImage.clearColorFilter();

                ViewGroup.LayoutParams params = fileImageFull.getLayoutParams();
                params.height = (int) ((parent.getWidth() - 2f * parent.getResources().getDimensionPixelSize(
                        R.dimen.side_margin)) / 16f * 9);
                fileImageFull.setLayoutParams(params);

                Picasso.with(parent.getContext())
                        .load(file)
                        .fit()
                        .centerCrop()
                        .into(fileImageFull);
            }
            else {

                fileImageFull.setVisibility(View.GONE);
                fileTitle.setVisibility(View.VISIBLE);
                fileSummary.setVisibility(View.VISIBLE);
                fileImage.setVisibility(View.VISIBLE);

                fileImage.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);

                if (file.isDirectory()) {
                    fileImage.setImageResource(R.drawable.ic_folder_white_24dp);
                }
                else {
                    fileImage.setImageResource(R.drawable.ic_insert_drive_file_white_24dp);
                }

                fileTitle.setText(file.getName());
                fileSummary.setText(file.isDirectory() ? file.list().length + " Files" : "" + (file.length() / BYTE_TO_MEBIBYTE) + " MiB");
            }

            return convertView;
        }
        return null;
    }

    public File getDirectory() {
        return topDir;
    }

    public void setDirectory(File selectedFile) {

        if (selectedFile != null && selectedFile.isDirectory()) {
            topDir = selectedFile;

            ArrayList<File> folders = new ArrayList<>();
            ArrayList<File> files = new ArrayList<>();

            if (selectedFile.listFiles() != null) {
                for (File file : selectedFile.listFiles()) {
                    if (file != null && file.exists()) {
                        if (file.isDirectory()) {
                            folders.add(file);
                        }
                        else {
                            files.add(file);
                        }
                    }
                }
            }

            if (folders.size() > 0) {
                Collections.sort(folders, new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        return lhs.getName().compareToIgnoreCase(rhs.getName());
                    }
                });
            }

            if (files.size() > 0) {
                Collections.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        return lhs.getName().compareToIgnoreCase(rhs.getName());
                    }
                });
            }

            folders.addAll(files);

            listFiles = folders;
            notifyDataSetChanged();
        }

    }

    public void setFinished() {
        finish = true;
    }

    public Boolean backDirectory() {

        if (finish || topDir.getAbsolutePath().equals(startDir.getAbsolutePath())) {
            return true;
        }

        File parentDir = topDir.getParentFile();

        if (parentDir != null && parentDir.exists() && parentDir.isDirectory()) {
            setDirectory(parentDir);
            return false;
        }
        return true;
    }

    public void remove(int index) {
        listFiles.remove(index);
        notifyDataSetChanged();
    }

    private static class ViewHolder {

        public final TextView fileTitle;
        public final TextView fileSummary;
        public final ImageView fileImage;
        public final ImageView fileImageFull;

        public ViewHolder(TextView fileTitle,
                TextView fileSummary,
                ImageView fileImage,
                ImageView fileImageFull) {
            this.fileTitle = fileTitle;
            this.fileSummary = fileSummary;
            this.fileImage = fileImage;
            this.fileImageFull = fileImageFull;
        }
    }

}
