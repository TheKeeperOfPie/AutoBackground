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
import android.support.v7.widget.Toolbar;
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

        View view = AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME) ?
                inflater.inflate(R.layout.tutorial_button_fragment, container, false) :
                inflater.inflate(R.layout.tutorial_button_fragment_dark, container, false);
        int colorFilterInt = AppSettings.getColorFilterInt(appContext);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar_actions);

        toolbar.inflateMenu(R.menu.menu_source);
        toolbar.getMenu().findItem(R.id.item_source_download).getIcon().setColorFilter(
                colorFilterInt,
                PorterDuff.Mode.MULTIPLY);
        toolbar.getMenu().findItem(R.id.item_source_delete).getIcon().setColorFilter(colorFilterInt,
                PorterDuff.Mode.MULTIPLY);
        toolbar.getMenu().findItem(R.id.item_source_view).getIcon().setColorFilter(colorFilterInt,
                PorterDuff.Mode.MULTIPLY);
        toolbar.getMenu().findItem(R.id.item_source_edit).getIcon().setColorFilter(colorFilterInt,
                PorterDuff.Mode.MULTIPLY);

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
        buttonTutorialText.setText("Download, delete, view, and edit each source.");

        return view;
    }
}
