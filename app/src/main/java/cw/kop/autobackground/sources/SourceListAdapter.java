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
import android.support.v7.widget.CardView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
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

    private static final String TAG = SourceListAdapter.class.getCanonicalName();
    private static final float OVERLAY_ALPHA = 0.85f;
    private final int colorFilterInt;
    private Activity mainActivity;
    private LayoutInflater inflater = null;
    private CardClickListener cardClickListener;
    private View.OnLongClickListener longClickListener;
    private ControllerSources controllerSources;

    public SourceListAdapter(Activity activity, CardClickListener listener, View.OnLongClickListener longClickListener, ControllerSources controllerSources) {
        this.controllerSources = controllerSources;
        mainActivity = activity;
        inflater = (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        cardClickListener = listener;
        this.longClickListener = longClickListener;
        colorFilterInt = AppSettings.getColorFilterInt(activity);
    }

    @Override
    public int getCount() {
        return controllerSources.size();
    }

    @Override
    public Source getItem(int position) {
        return controllerSources.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final Source listItem = controllerSources.get(position);

        CardView cardView;
        EditText title;
        View imageOverlay;
        ImageView downloadButton;
        ImageView deleteButton;
        ImageView viewButton;
        ImageView editButton;
        TextView sourceType;
        TextView sourceData;
        TextView sourceNum;
        TextView sourceSort;
        TextView sourceTime;
        ImageView sourceImage;

        if (convertView == null) {
            convertView = AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME) ?
                    inflater.inflate(R.layout.source_list_card, parent, false) :
                    inflater.inflate(R.layout.source_list_card_dark, parent, false);

            cardView = (CardView) convertView.findViewById(R.id.source_card);
            title = (EditText) convertView.findViewById(R.id.source_title);
            imageOverlay = convertView.findViewById(R.id.source_image_overlay);
            downloadButton = (ImageView) convertView.findViewById(R.id.source_download_button);
            deleteButton = (ImageView) convertView.findViewById(R.id.source_delete_button);
            viewButton = (ImageView) convertView.findViewById(R.id.source_view_image_button);
            editButton = (ImageView) convertView.findViewById(R.id.source_edit_button);
            sourceType = (TextView) convertView.findViewById(R.id.source_type);
            sourceData = (TextView) convertView.findViewById(R.id.source_data);
            sourceNum = (TextView) convertView.findViewById(R.id.source_num);
            sourceSort = (TextView) convertView.findViewById(R.id.source_sort);
            sourceTime = (TextView) convertView.findViewById(R.id.source_time);
            sourceImage = (ImageView) convertView.findViewById(R.id.source_image);

            downloadButton.setOnLongClickListener(longClickListener);
            deleteButton.setOnLongClickListener(longClickListener);
            viewButton.setOnLongClickListener(longClickListener);
            editButton.setOnLongClickListener(longClickListener);

            downloadButton.setImageResource(R.drawable.ic_file_download_white_24dp);
            deleteButton.setImageResource(R.drawable.ic_delete_white_24dp);
            viewButton.setImageResource(R.drawable.ic_photo_white_24dp);
            editButton.setImageResource(R.drawable.ic_edit_white_24dp);

            downloadButton.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
            deleteButton.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
            viewButton.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
            editButton.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);

            title.setClickable(false);
            convertView.setTag(new ViewHolder(cardView, title, imageOverlay, downloadButton, deleteButton, viewButton, editButton, sourceType, sourceData, sourceNum, sourceSort, sourceTime, sourceImage));

        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        cardView = viewHolder.cardView;
        title = viewHolder.title;
        imageOverlay = viewHolder.imageOverlay;
        downloadButton = viewHolder.downloadButton;
        deleteButton = viewHolder.deleteButton;
        viewButton = viewHolder.viewButton;
        editButton = viewHolder.editButton;
        sourceType = viewHolder.sourceType;
        sourceData = viewHolder.sourceData;
        sourceNum = viewHolder.sourceNum;
        sourceSort = viewHolder.sourceSort;
        sourceTime = viewHolder.sourceTime;
        sourceImage = viewHolder.sourceImage;

        Resources resources = parent.getContext().getResources();
        boolean use = listItem.isUse();
        boolean preview = listItem.isPreview();

        final View finalConvertView = convertView;

        View.OnClickListener expandClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClickListener.onExpandClick(finalConvertView, position);
            }
        };

        cardView.setOnClickListener(expandClickListener);

        cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                cardClickListener.onLongClick(position);
                return true;
            }
        });

        title.setText(listItem.getTitle());
        title.setOnClickListener(expandClickListener);

        if (use) {
            imageOverlay.setAlpha(0);
        }
        else {
            imageOverlay.setBackgroundColor(resources.getColor(AppSettings.getBackgroundColorResource()));
            imageOverlay.setAlpha(OVERLAY_ALPHA);
        }

        if (listItem.getType().equals(AppSettings.FOLDER)) {
            downloadButton.setVisibility(View.INVISIBLE);
        }
        else {
            downloadButton.setVisibility(View.VISIBLE);
        }

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClickListener.onDownloadClick(finalConvertView, controllerSources.get(position));
            }
        });
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

        ViewGroup.LayoutParams imageParams = sourceImage.getLayoutParams();

        RelativeLayout.LayoutParams titleParams = (RelativeLayout.LayoutParams) title.getLayoutParams();

        if (preview) {
            imageParams.height = (int) ((parent.getWidth() - 2f * resources.getDimensionPixelSize(R.dimen.side_margin)) / 16f * 9);
            sourceImage.setImageResource(R.drawable.ic_file_download_white_48dp);
            sourceImage.setColorFilter(AppSettings.getColorFilterInt(parent.getContext()),
                    PorterDuff.Mode.MULTIPLY);

            if (listItem.getType().equals(AppSettings.FOLDER)) {
                String[] folders = listItem.getData().split(AppSettings.DATA_SPLITTER);
                boolean needsImage = true;
                for (int index = 0; needsImage && index < folders.length; index++) {

                    File[] files = new File(folders[index]).listFiles(FileHandler.getImageFileNameFilter());

                    if (files != null && files.length > 0) {
                        needsImage = false;
                        listItem.setImageFile(files[0]);
                        sourceImage.clearColorFilter();
                        Picasso.with(parent.getContext()).load(files[0]).fit().centerCrop().into(
                                sourceImage);
                    }
                    else {
                        sourceImage.setImageResource(R.drawable.ic_not_interested_white_48dp);
                    }
                }
            }
            else {
                File folder = new File(AppSettings.getDownloadPath() + "/" + listItem.getTitle() + " " + AppSettings.getImagePrefix());
                if (folder.exists() && folder.isDirectory()) {
                    File[] files = folder.listFiles(FileHandler.getImageFileNameFilter());

                    if (files != null && files.length > 0) {
                        listItem.setImageFile(files[0]);
                        sourceImage.clearColorFilter();
                        Picasso.with(parent.getContext()).load(files[0]).fit().centerCrop().into(
                                sourceImage);
                    }
                }
            }
        }
        else {
            Picasso.with(parent.getContext()).load(android.R.color.transparent).into(sourceImage);
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
        sourceImage.setLayoutParams(imageParams);

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
        SpannableString sortPrefix = new SpannableString("Sort By: ");
        sortPrefix.setSpan(new ForegroundColorSpan(colorPrimary), 0, sortPrefix.length(),
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

        if (TextUtils.isEmpty(listItem.getSort())) {
            sourceSort.setVisibility(View.GONE);
        }
        else {
            sourceSort.setVisibility(View.VISIBLE);
            sourceSort.setText(sortPrefix);
            sourceSort.append(listItem.getSort());
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

    private static class ViewHolder {

        protected final CardView cardView;
        protected final EditText title;
        protected final View imageOverlay;
        protected final ImageView downloadButton;
        protected final ImageView deleteButton;
        protected final ImageView viewButton;
        protected final ImageView editButton;
        protected final TextView sourceType;
        protected final TextView sourceData;
        protected final TextView sourceNum;
        protected final TextView sourceSort;
        protected final TextView sourceTime;
        protected final ImageView sourceImage;

        public ViewHolder(CardView cardView,
                EditText title,
                View imageOverlay,
                ImageView downloadButton,
                ImageView deleteButton,
                ImageView viewButton,
                ImageView editButton,
                TextView sourceType,
                TextView sourceData, TextView sourceNum, TextView sourceSort, TextView sourceTime, ImageView sourceImage) {
            this.cardView = cardView;
            this.title = title;
            this.imageOverlay = imageOverlay;
            this.downloadButton = downloadButton;
            this.deleteButton = deleteButton;
            this.viewButton = viewButton;
            this.editButton = editButton;
            this.sourceType = sourceType;
            this.sourceData = sourceData;
            this.sourceNum = sourceNum;
            this.sourceSort = sourceSort;
            this.sourceTime = sourceTime;
            this.sourceImage = sourceImage;
        }
    }

    public interface CardClickListener {

        void onDownloadClick(View view, Source source);

        void onDeleteClick(View view, int index);

        void onViewImageClick(View view, int index);

        void onEditClick(View view, int index);

        void onExpandClick(View view, int position);

        void onLongClick(int position);
    }

}
