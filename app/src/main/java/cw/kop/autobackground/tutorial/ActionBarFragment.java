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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.reflect.Field;

import cw.kop.autobackground.MenuWrapper;
import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/23/2014.
 */
public class ActionBarFragment extends Fragment {

    private static final String TAG = ActionBarFragment.class.getCanonicalName();
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

        View view = inflater.inflate(R.layout.tutorial_action_bar_fragment, container, false);
        int colorFilterInt = AppSettings.getColorFilterInt(appContext);

        Toolbar buttonToolbar = (Toolbar) view.findViewById(R.id.button_toolbar);
        buttonToolbar.setTitleTextAppearance(appContext, R.style.ToolbarTitle);
        buttonToolbar.inflateMenu(R.menu.source_list_menu);

        Menu menu = buttonToolbar.getMenu();
        Drawable refreshIcon = getResources().getDrawable(R.drawable.ic_refresh_white_24dp);
        Drawable downloadIcon = getResources().getDrawable(R.drawable.ic_file_download_white_24dp);
        Drawable storageIcon = getResources().getDrawable(R.drawable.ic_sort_white_24dp);
        refreshIcon.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        downloadIcon.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        storageIcon.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        menu.getItem(0).setIcon(refreshIcon);
        menu.getItem(1).setIcon(downloadIcon);
        menu.getItem(2).setIcon(storageIcon);

        TextView buttonTitleText = (TextView) view.findViewById(R.id.button_title_text);
        buttonTitleText.setText("ActionBar buttons");

        TextView buttonTutorialText = (TextView) view.findViewById(R.id.button_tutorial_text);
        buttonTutorialText.setText("Cycle wallpaper, download new images, and sort sources. " +
                "Hit download after adding some sources to fetch the images.");

        Toolbar settingToolbar = (Toolbar) view.findViewById(R.id.setting_toolbar);
        Drawable navIcon = getResources().getDrawable(R.drawable.drawer_menu_white);
        navIcon.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        settingToolbar.setNavigationIcon(navIcon);

        TextView settingTitleText = (TextView) view.findViewById(R.id.setting_title_text);
        settingTitleText.setText("More settings");

        TextView settingTutorialText = (TextView) view.findViewById(R.id.setting_tutorial_text);
        settingTutorialText.setText("Open the drawer to access additional settings.");

        return view;
    }
}
