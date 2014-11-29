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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/23/2014.
 */
public class ButtonFragment extends Fragment {

    private static final String TAG = ButtonFragment.class.getCanonicalName();
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

        View view = inflater.inflate(R.layout.tutorial_button_fragment, container, false);
        int colorFilterInt = AppSettings.getColorFilterInt(appContext);

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

        TextView addTitleText = (TextView) view.findViewById(R.id.add_title_text);
        addTitleText.setTextColor(colorFilterInt);
        addTitleText.setText("Adding new sources");

        TextView addTutorialText = (TextView) view.findViewById(R.id.add_tutorial_text);
        addTutorialText.setTextColor(colorFilterInt);
        addTutorialText.setText("Easily add a new source from a variety of different places.");

        TextView buttonTitleText = (TextView) view.findViewById(R.id.button_title_text);
        buttonTitleText.setTextColor(colorFilterInt);
        buttonTitleText.setText("Source actions");

        TextView buttonTutorialText = (TextView) view.findViewById(R.id.button_tutorial_text);
        buttonTutorialText.setTextColor(colorFilterInt);
        buttonTutorialText.setText("Delete, view, and edit each source.");

        return view;
    }
}
