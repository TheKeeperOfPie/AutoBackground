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

package cw.kop.autobackground.tutorial;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/23/2014.
 */
public class CardFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = CardFragment.class.getCanonicalName();
    private Context appContext;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = activity;
    }

    @Override
    public void onDetach() {
        appContext = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.tutorial_card_fragment, container, false);
        View sourceCard = view.findViewById(R.id.source_card);
        sourceCard.setOnClickListener(this);

        int colorFilterInt = AppSettings.getColorFilterInt(appContext);

        TextView sourceTitle = (TextView) view.findViewById(R.id.source_title);
        sourceTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CardFragment.this.onClick(view);
            }
        });

        ImageView deleteButton = (ImageView) view.findViewById(R.id.source_delete_button);
        ImageView viewButton = (ImageView) view.findViewById(R.id.source_view_image_button);
        ImageView editButton = (ImageView) view.findViewById(R.id.source_edit_button);

        Drawable deleteDrawable = getResources().getDrawable(R.drawable.ic_delete_white_24dp);
        Drawable viewDrawable = getResources().getDrawable(R.drawable.ic_photo_white_24dp);
        Drawable editDrawable = getResources().getDrawable(R.drawable.ic_edit_white_24dp);

        deleteDrawable.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        viewDrawable.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        editDrawable.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);

        deleteButton.setImageDrawable(deleteDrawable);
        viewButton.setImageDrawable(viewDrawable);
        editButton.setImageDrawable(editDrawable);

        deleteButton.setOnClickListener(null);
        viewButton.setOnClickListener(null);
        editButton.setOnClickListener(null);

        TextView sourceType = (TextView) view.findViewById(R.id.source_type);
        TextView sourceData = (TextView) view.findViewById(R.id.source_data);
        TextView sourceNum = (TextView) view.findViewById(R.id.source_num);
        TextView sourceTime = (TextView) view.findViewById(R.id.source_time);

        int colorPrimary = getResources().getColor(R.color.BLUE_OPAQUE);
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
        sourceData.setText(dataPrefix);
        sourceNum.setText(numPrefix);
        sourceTime.setText(timePrefix);

        ImageView image = (ImageView) view.findViewById(R.id.source_image);
        Picasso.with(appContext).load(R.drawable.preview_image_0).fit().centerCrop().into(image);

        TextView titleText = (TextView) view.findViewById(R.id.title_text);
        titleText.setText("Sources");

        TextView tutorialText = (TextView) view.findViewById(R.id.tutorial_text);
        tutorialText.setText("These are the parts that make up your wallpaper. " +
                "Each represents an image source like an album from Imgur or " +
                "a subreddit.");

//        cardText.setText("These are the different parts of the wallpaper, which are represented by " +
//                "a card like the one below." +
//                "\n" +
//                "Sources are entries which represent a source of images to use for your device's wallpaper, " +
//                "such as an Album from Imgur or a Subreddit." +
//                "\n" +
//                "Each source card contains a preview image, the title, and buttons corresponding to " +
//                "delete, view, and edit actions. Also, clicking on a card will expand it and show additional information.");

        return view;
    }

    @Override
    public void onClick(View v) {
        View expandedView = v.findViewById(R.id.source_expand_container);
        if (expandedView.isShown()) {
            expandedView.setVisibility(View.GONE);
        }
        else {
            expandedView.setVisibility(View.VISIBLE);
        }
    }
}
