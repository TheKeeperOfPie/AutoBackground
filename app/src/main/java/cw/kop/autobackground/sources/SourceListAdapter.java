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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import cw.kop.autobackground.R;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;

public class SourceListAdapter extends BaseAdapter {

    public static final String NO_SOURCES = "NO_SOURCE";
    public static final String NO_ACTIVE_SOURCES = "NO_ACTIVE_SOURCES";
    public static final String NEED_DOWNLOAD = "NEED_DOWNLOAD";
    public static final String NO_IMAGES = "NO_IMAGES";
    public static final String OKAY = "OKAY";

    private static final float OVERLAY_ALPHA = 0.85f;

    private Activity mainActivity;
    private ArrayList<HashMap<String, String>> listData;
    private HashSet<String> titles;
    private LayoutInflater inflater = null;
    private CardClickListener cardClickListener;

    public SourceListAdapter(Activity activity, CardClickListener listener) {
        mainActivity = activity;
        listData = new ArrayList<>();
        titles = new HashSet<>();
        inflater = (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        cardClickListener = listener;
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    public HashMap<String, String> getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final HashMap<String, String> listItem = listData.get(position);

        if (convertView == null) {
            convertView = AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME) ? inflater.inflate(R.layout.source_list_card, parent, false) : inflater.inflate(R.layout.source_list_card_dark, parent, false);
        }

        final View view = convertView;

        EditText title = (EditText) view.findViewById(R.id.source_title);
        title.setText(listItem.get("title"));
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClickListener.onViewClick(view, position);
            }
        });

        int colorFilterInt = AppSettings.getColorFilterInt(parent.getContext());

        Resources resources = parent.getContext().getResources();
        View imageOverlay = view.findViewById(R.id.source_image_overlay);

        int lightGrayColor = resources.getColor(R.color.LIGHT_GRAY_OPAQUE);
        int darkGrayColor = resources.getColor(R.color.DARK_GRAY_OPAQUE);

        if (Boolean.parseBoolean(listItem.get("use"))) {
            imageOverlay.setAlpha(0);
            if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
                title.setTextColor(darkGrayColor);
                title.setShadowLayer(4.0f, 0f, 0f, lightGrayColor);
            }
            else {
                title.setTextColor(lightGrayColor);
                title.setShadowLayer(4.0f, 0f, 0f, darkGrayColor);
            }
        }
        else {
            imageOverlay.setAlpha(OVERLAY_ALPHA);
            if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
                title.setTextColor(lightGrayColor);
                title.setShadowLayer(4.0f, 0f, 0f, darkGrayColor);
            }
            else {
                title.setTextColor(darkGrayColor);
                title.setShadowLayer(4.0f, 0f, 0f, lightGrayColor);
            }
        }

        ImageView deleteButton = (ImageView) view.findViewById(R.id.source_delete_button);
        ImageView viewButton = (ImageView) view.findViewById(R.id.source_view_image_button);
        ImageView editButton = (ImageView) view.findViewById(R.id.source_edit_button);

        Drawable deleteDrawable = resources.getDrawable(R.drawable.ic_delete_white_24dp);
        Drawable viewDrawable = resources.getDrawable(R.drawable.ic_photo_white_24dp);
        Drawable editDrawable = resources.getDrawable(R.drawable.ic_edit_white_24dp);

        deleteDrawable.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        viewDrawable.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        editDrawable.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);

        deleteButton.setImageDrawable(deleteDrawable);
        viewButton.setImageDrawable(viewDrawable);
        editButton.setImageDrawable(editDrawable);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClickListener.onDeleteClick(position);
            }
        });
        viewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClickListener.onViewImageClick(position);
            }
        });

        ImageView image = (ImageView) view.findViewById(R.id.source_image);

        if (Boolean.parseBoolean(listItem.get("preview"))) {

            Drawable downloadDrawable = resources.getDrawable(R.drawable.ic_file_download_white_24dp);
            downloadDrawable.setColorFilter(AppSettings.getColorFilterInt(parent.getContext()),
                    PorterDuff.Mode.MULTIPLY);
            image.setImageDrawable(downloadDrawable);

            if (listItem.get("type").equals(AppSettings.FOLDER)) {
                String[] folders = listItem.get("data").split(AppSettings.DATA_SPLITTER);
                boolean needsImage = true;
                for (int index = 0; needsImage && index < folders.length; index++) {

                    File[] files = new File(folders[index]).listFiles(FileHandler.getImageFileNameFilter());

                    if (files != null && files.length > 0) {
                        needsImage = false;
                        listItem.put("image", files[0].getAbsolutePath());
                        image.getLayoutParams().height = Math.round(TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                160,
                                resources.getDisplayMetrics()));
                        Picasso.with(parent.getContext()).load(files[0]).fit().centerCrop().into(
                                image);
                    }
                }
            }
            else {
                File folder = new File(AppSettings.getDownloadPath() + "/" + listItem.get("title") + " " + AppSettings.getImagePrefix());
                if (folder.exists() && folder.isDirectory()) {
                    File[] files = folder.listFiles(FileHandler.getImageFileNameFilter());

                    if (files != null && files.length > 0) {
                        listItem.put("image", files[0].getAbsolutePath());
                        image.getLayoutParams().height = Math.round(TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                160,
                                resources.getDisplayMetrics()));
                        Picasso.with(parent.getContext()).load(files[0]).fit().centerCrop().into(
                                image);
                    }
                }
            }
        }
        else {
            image.getLayoutParams().height = 0;
        }

        return view;
    }

    public void setActivated(int position, boolean use) {
        HashMap<String, String> changedItem = listData.get(position);
        changedItem.put("use", "" + use);
        listData.set(position, changedItem);
        notifyDataSetChanged();
    }

    public void toggleActivated(int position) {
        HashMap<String, String> changedItem = listData.get(position);
        changedItem.put("use", "" + !Boolean.parseBoolean(changedItem.get("use")));
        listData.set(position, changedItem);
    }

    public boolean setItem(int position, String type, String title, String data, boolean use,
            String num, boolean preview, boolean useTime, String time) {

        HashMap<String, String> changedItem = listData.get(position);

        if (!changedItem.get("title").equals(title)) {
            if (titles.contains(title)) {
                return false;
            }
        }

        titles.remove(changedItem.get("title"));
        changedItem.put("type", type);
        changedItem.put("title", title);
        changedItem.put("data", data);
        changedItem.put("num", "" + num);
        changedItem.put("use", "" + use);
        changedItem.put("preview", "" + preview);
        changedItem.put("use_time", "" + useTime);
        changedItem.put("time", time);
        File folder = new File(AppSettings.getDownloadPath() + "/" + title + " " + AppSettings.getImagePrefix());
        if (folder.exists() && folder.isDirectory()) {
            changedItem.put("numStored",
                    "" + folder.listFiles(FileHandler.getImageFileNameFilter()).length);
        }
        else {
            changedItem.put("numStored", "0");
        }
        listData.set(position, changedItem);
        titles.add(title);
        notifyDataSetChanged();
        return true;
    }

    public boolean addItem(String type, String title, String data, boolean use, String num, boolean preview, boolean useTime, String time) {

        if (titles.contains(title)) {
            return false;
        }

        HashMap<String, String> newItem = new HashMap<String, String>();
        newItem.put("type", type);
        newItem.put("title", title);
        newItem.put("data", data);
        newItem.put("num", "" + num);
        newItem.put("use", "" + use);
        newItem.put("numStored", "0");
        newItem.put("preview", "" + preview);
        newItem.put("use_time", "" + useTime);
        newItem.put("time", time);
        File folder = new File(AppSettings.getDownloadPath() + "/" + title + " " + AppSettings.getImagePrefix());
        if (folder.exists() && folder.isDirectory()) {
            newItem.put("numStored",
                    "" + folder.listFiles(FileHandler.getImageFileNameFilter()).length);
        }
        else {
            newItem.put("numStored", "0");
        }

        listData.add(newItem);
        titles.add(title);
        notifyDataSetChanged();

        Log.i("WLA", "listData" + listData.size());
        return true;
    }

    public void removeItem(int position) {
        titles.remove(listData.get(position).get("title"));
        listData.remove(position);
        notifyDataSetChanged();
    }

    public void updateNum() {

        FilenameFilter filenameFilter = FileHandler.getImageFileNameFilter();

        String cacheDir = AppSettings.getDownloadPath();

        if (listData != null) {
            for (HashMap<String, String> hashMap : listData) {
                if (hashMap.get("type").equals(AppSettings.FOLDER)) {

                    int numImages = 0;

                    for (String folderName : hashMap.get("data").split(AppSettings.DATA_SPLITTER)) {
                        File folder = new File(folderName);
                        if (folder.exists() && folder.isDirectory()) {
                            numImages += folder.listFiles(filenameFilter).length;
                        }
                    }

                    hashMap.put("num", "" + numImages);
                }
                else {
                    File folder = new File(cacheDir + "/" + hashMap.get("title") + " " + AppSettings.getImagePrefix());
                    if (folder.exists() && folder.isDirectory()) {
                        hashMap.put("numStored",
                                "" + folder.listFiles(filenameFilter).length);
                    }
                }
            }
            notifyDataSetChanged();
        }
    }

    public String checkSources() {

        if (listData.size() == 0) {
            return NO_SOURCES;
        }

        boolean noActive = true;
        boolean needDownload = true;

        for (int index = 0; (noActive || needDownload) && index < listData.size(); index++) {

            boolean use = listData.get(index).get("use").equals("true");

            if (noActive && use) {
                noActive = false;
            }

            if (needDownload && use && listData.get(index).get("type").equals(AppSettings.FOLDER)) {
                needDownload = false;
                Log.i("SLA", "Type: " + listData.get(index).get("type"));
            }

        }

        if (noActive) {
            return NO_ACTIVE_SOURCES;
        }

        boolean noImages = FileHandler.hasImages();

        if (noImages) {
            if (needDownload) {
                return NEED_DOWNLOAD;
            }
            return NO_IMAGES;
        }

        return OKAY;
    }

    public void sortData(final String key) {

        ArrayList<HashMap<String, String>> sortList = new ArrayList<HashMap<String, String>>();
        sortList.addAll(listData);

        Collections.sort(sortList, new Comparator<HashMap<String, String>>() {
            @Override
            public int compare(HashMap<String, String> lhs, HashMap<String, String> rhs) {

                if (key.equals("use")) {
                    boolean first = Boolean.parseBoolean(lhs.get("use"));
                    boolean second = Boolean.parseBoolean(rhs.get("use"));

                    if (first && second || (!first && !second)) {
                        return lhs.get("title").compareTo(rhs.get("title"));
                    }

                    return first ? -1 : 1;

                }

                if (key.equals("num")) {
                    return Integer.parseInt(lhs.get("num")) - Integer.parseInt(rhs.get("num"));
                }

                return lhs.get(key).compareTo(rhs.get(key));
            }
        });

        if (sortList.equals(listData)) {
            Collections.reverse(sortList);
        }
        listData = sortList;

        notifyDataSetChanged();

    }

    public void saveData() {

        AppSettings.setSources(listData);

        Log.i("WLA", "SavedListData" + listData.size());
        Log.i("WLA", "Saved Data: " + AppSettings.getNumSources());
    }


    public interface CardClickListener {

        void onDeleteClick(int index);

        void onViewImageClick(int index);

        void onViewClick(View view, int index);
    }

}
