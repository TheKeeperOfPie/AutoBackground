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
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

    private static final String TAG = SourceListAdapter.class.getCanonicalName();
    private static final float OVERLAY_ALPHA = 0.85f;
    private Activity mainActivity;
    private ArrayList<Source> listData;
    private HashSet<String> titles;
    private LayoutInflater inflater = null;
    private CardClickListener cardClickListener;
    private boolean isRemoving = false;

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

    public Source getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final Source listItem = listData.get(position);

        EditText title;
        View imageOverlay;
        ImageView deleteButton;
        ImageView viewButton;
        ImageView editButton;
        TextView sourceType;
        TextView sourceData;
        TextView sourceNum;
        TextView sourceTime;
        ImageView image;

        if (convertView == null) {
            convertView = AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME) ?
                    inflater.inflate(R.layout.source_list_card, parent, false) :
                    inflater.inflate(R.layout.source_list_card_dark, parent, false);


            title = (EditText) convertView.findViewById(R.id.source_title);
            imageOverlay = convertView.findViewById(R.id.source_image_overlay);
            deleteButton = (ImageView) convertView.findViewById(R.id.source_delete_button);
            viewButton = (ImageView) convertView.findViewById(R.id.source_view_image_button);
            editButton = (ImageView) convertView.findViewById(R.id.source_edit_button);
            sourceType = (TextView) convertView.findViewById(R.id.source_type);
            sourceData = (TextView) convertView.findViewById(R.id.source_data);
            sourceNum = (TextView) convertView.findViewById(R.id.source_num);
            sourceTime = (TextView) convertView.findViewById(R.id.source_time);
            image = (ImageView) convertView.findViewById(R.id.source_image);

            convertView.setTag(new ViewHolder(title, imageOverlay, deleteButton, viewButton, editButton, sourceType, sourceData, sourceNum, sourceTime, image));

        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        title = viewHolder.title;
        imageOverlay = viewHolder.imageOverlay;
        deleteButton = viewHolder.deleteButton;
        viewButton = viewHolder.viewButton;
        editButton = viewHolder.editButton;
        sourceType = viewHolder.sourceType;
        sourceData = viewHolder.sourceData;
        sourceNum = viewHolder.sourceNum;
        sourceTime = viewHolder.sourceTime;
        image = viewHolder.image;

        Resources resources = parent.getContext().getResources();
        int colorFilterInt = AppSettings.getColorFilterInt(parent.getContext());
        int lightGrayColor = resources.getColor(R.color.LIGHT_GRAY_OPAQUE);
        int darkGrayColor = resources.getColor(R.color.DARK_GRAY_OPAQUE);
        boolean use = listItem.isUse();
        boolean preview = listItem.isPreview();

        final View finalConvertView = convertView;
        title.setText(listItem.getTitle());
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClickListener.onExpandClick(finalConvertView, position);
            }
        });


        if (use) {
            imageOverlay.setAlpha(0);
        }
        else {
            imageOverlay.setBackgroundColor(resources.getColor(AppSettings.getBackgroundColorResource()));
            imageOverlay.setAlpha(OVERLAY_ALPHA);
        }

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
                cardClickListener.onDeleteClick(finalConvertView, position);
            }
        });
        viewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClickListener.onViewImageClick(finalConvertView, position);
            }
        });
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClickListener.onEditClick(finalConvertView, position);
            }
        });

        ViewGroup.LayoutParams imageParams = image.getLayoutParams();

        RelativeLayout.LayoutParams titleParams = (RelativeLayout.LayoutParams) title.getLayoutParams();

        if (preview) {
            imageParams.height = (int) ((parent.getWidth() - 2f * resources.getDimensionPixelSize(R.dimen.side_margin)) / 16f * 9);
            Drawable downloadDrawable = resources.getDrawable(R.drawable.ic_file_download_white_48dp);
            downloadDrawable.setColorFilter(AppSettings.getColorFilterInt(parent.getContext()),
                    PorterDuff.Mode.MULTIPLY);
            image.setImageDrawable(downloadDrawable);

            if (listItem.getType().equals(AppSettings.FOLDER)) {
                String[] folders = listItem.getData().split(AppSettings.DATA_SPLITTER);
                boolean needsImage = true;
                for (int index = 0; needsImage && index < folders.length; index++) {

                    File[] files = new File(folders[index]).listFiles(FileHandler.getImageFileNameFilter());

                    if (files != null && files.length > 0) {
                        needsImage = false;
                        listItem.setImageFile(files[0]);
                        Picasso.with(parent.getContext()).load(files[0]).fit().centerCrop().into(
                                image);
                    }
                }
            }
            else {
                File folder = new File(AppSettings.getDownloadPath() + "/" + listItem.getTitle() + " " + AppSettings.getImagePrefix());
                if (folder.exists() && folder.isDirectory()) {
                    File[] files = folder.listFiles(FileHandler.getImageFileNameFilter());

                    if (files != null && files.length > 0) {
                        listItem.setImageFile(files[0]);
                        Picasso.with(parent.getContext()).load(files[0]).fit().centerCrop().into(
                                image);
                    }
                }
            }
        }
        else {
            Picasso.with(parent.getContext()).load(android.R.color.transparent).into(image);
            imageParams.height = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                    28,
                    resources.getDisplayMetrics()) + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    24,
                    resources.getDisplayMetrics()));
        }

        if (!preview) {
            title.setTextColor(AppSettings.getColorFilterInt(parent.getContext()));
            title.setShadowLayer(0f, 0f, 0f, 0x00000000);
        }
        else if (use) {
            title.setTextColor(resources.getColor(R.color.WHITE_OPAQUE));
            title.setShadowLayer(5.0f, -1f, -1f, 0xFF000000);
        }
        else {
            title.setTextColor(AppSettings.getColorFilterInt(parent.getContext()));
            title.setShadowLayer(0f, 0f, 0f, 0x00000000);
        }

        title.setLayoutParams(titleParams);
        image.setLayoutParams(imageParams);

        int colorPrimary = resources.getColor(R.color.BLUE_OPAQUE);
        SpannableString typePrefix = new SpannableString("Type: ");
        typePrefix.setSpan(new ForegroundColorSpan(colorPrimary), 0, typePrefix.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString dataPrefix = new SpannableString("Data: ");
        dataPrefix.setSpan(new ForegroundColorSpan(colorPrimary), 0, dataPrefix.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString numPrefix = new SpannableString("Number of Images: ");
        numPrefix.setSpan(new ForegroundColorSpan(colorPrimary), 0, numPrefix.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString timePrefix = new SpannableString("Active Time: ");
        timePrefix.setSpan(new ForegroundColorSpan(colorPrimary), 0, timePrefix.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        sourceType.setText(typePrefix);
        sourceType.append(listItem.getType());
        sourceData.setText(dataPrefix);
        if (listItem.getType().equals(AppSettings.FOLDER)) {
            sourceData.append(Arrays.toString(listItem.getData().split(AppSettings.DATA_SPLITTER)));
        }
        else {
            sourceData.append(listItem.getData());
        }
        sourceNum.setText(numPrefix);
        if (listItem.getType().equals(AppSettings.FOLDER)) {
            sourceNum.append("" + listItem.getNum());
        }
        else {
            sourceNum.append(listItem.getNumStored() + " / " + listItem.getNum());
        }
        sourceTime.setText(timePrefix);

        if (listItem.isUseTime()) {
            sourceTime.append(listItem.getTime());
        }
        else {
            sourceTime.append("N/A");
        }

        return finalConvertView;
    }

    public void setActivated(int position, boolean use) {
        Source changedItem = listData.get(position);
        changedItem.setUse(use);
        listData.set(position, changedItem);
        notifyDataSetChanged();
    }

    public void toggleActivated(int position) {
        Source changedItem = listData.get(position);
        changedItem.setUse(!changedItem.isUse());
        listData.set(position, changedItem);
    }

    public boolean setItem(int position, Source source) {

        Source oldSource = listData.get(position);

        if (!oldSource.getTitle().equals(source.getTitle())) {
            if (titles.contains(source.getTitle())) {
                return false;
            }
        }
        titles.remove(oldSource.getTitle());
        File folder = new File(AppSettings.getDownloadPath() + "/" + source.getTitle() + " " + AppSettings.getImagePrefix());
        if (folder.exists() && folder.isDirectory()) {
            source.setNumStored(folder.listFiles(FileHandler.getImageFileNameFilter()).length);
        }
        else {
            source.setNumStored(0);
        }
        listData.set(position, source);
        titles.add(source.getTitle());
        notifyDataSetChanged();
        saveData();
        return true;
    }

    public boolean addItem(Source source, boolean save) {

        if (titles.contains(source.getTitle())) {
            return false;
        }

        File folder = new File(AppSettings.getDownloadPath() + "/" + source.getTitle() + " " + AppSettings.getImagePrefix());
        if (folder.exists() && folder.isDirectory()) {
            source.setNumStored(folder.listFiles(FileHandler.getImageFileNameFilter()).length);
        }
        else {
            source.setNumStored(0);
        }

        listData.add(source);
        titles.add(source.getTitle());
        notifyDataSetChanged();
        if (save) {
            saveData();
        }
        return true;
    }

    public void removeItem(final int position) {

        titles.remove(listData.get(position).getTitle());
        listData.remove(position);
        notifyDataSetChanged();
    }

    public void updateNum() {

        FilenameFilter filenameFilter = FileHandler.getImageFileNameFilter();

        String cacheDir = AppSettings.getDownloadPath();

        if (listData != null) {
            for (Source hashMap : listData) {
                if (hashMap.getType().equals(AppSettings.FOLDER)) {

                    int numImages = 0;

                    for (String folderName : hashMap.getData().split(AppSettings.DATA_SPLITTER)) {
                        File folder = new File(folderName);
                        if (folder.exists() && folder.isDirectory()) {
                            numImages += folder.listFiles(filenameFilter).length;
                        }
                    }

                    hashMap.setNum(numImages);
                }
                else {
                    File folder = new File(cacheDir + "/" + hashMap.getTitle() + " " + AppSettings.getImagePrefix());
                    if (folder.exists() && folder.isDirectory()) {
                        hashMap.setNumStored(folder.listFiles(filenameFilter).length);
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

            boolean use = listData.get(index).isUse();

            if (noActive && use) {
                noActive = false;
            }

            if (needDownload && use && listData.get(index).getType().equals(AppSettings.FOLDER)) {
                needDownload = false;
                Log.i("SLA", "Type: " + listData.get(index).getType());
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

        ArrayList<Source> sortList = new ArrayList<Source>();
        sortList.addAll(listData);

        Collections.sort(sortList, new Comparator<Source>() {
            @Override
            public int compare(Source lhs, Source rhs) {

                if (key.equals("use")) {
                    boolean first = lhs.isUse();
                    boolean second = rhs.isUse();

                    if (first && second || (!first && !second)) {
                        return lhs.getTitle().compareTo(rhs.getTitle());
                    }

                    return first ? -1 : 1;

                }

                if (key.equals("num")) {
                    return lhs.getNum() - rhs.getNum();
                }

                return -1; // TODO: Fix lhs.get(key).compareTo(rhs.get(key));
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
        Log.i("WLA", "Saved Data: " + AppSettings.getNumberSources());
    }

    private static class ViewHolder {

        public final EditText title;
        public final View imageOverlay;
        public final ImageView deleteButton;
        public final ImageView viewButton;
        public final ImageView editButton;
        public final TextView sourceType;
        public final TextView sourceData;
        public final TextView sourceNum;
        public final TextView sourceTime;
        public final ImageView image;

        public ViewHolder(EditText title,
                View imageOverlay,
                ImageView deleteButton,
                ImageView viewButton,
                ImageView editButton,
                TextView sourceType,
                TextView sourceData, TextView sourceNum, TextView sourceTime, ImageView image) {
            this.title = title;
            this.imageOverlay = imageOverlay;
            this.deleteButton = deleteButton;
            this.viewButton = viewButton;
            this.editButton = editButton;
            this.sourceType = sourceType;
            this.sourceData = sourceData;
            this.sourceNum = sourceNum;
            this.sourceTime = sourceTime;
            this.image = image;
        }
    }

    public interface CardClickListener {

        void onDeleteClick(View view, int index);

        void onViewImageClick(View view, int index);

        void onEditClick(View view, int index);

        void onExpandClick(View view, int position);
    }

}
