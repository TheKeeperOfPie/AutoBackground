/*
 * Copyright (C) Winson Chiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cw.kop.autobackground;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import cw.kop.autobackground.settings.AppSettings;

public class NavListAdapter extends BaseAdapter {

    private static LayoutInflater inflater = null;
    private ArrayList<String> fragmentList;
    private int[] lightImages = new int[] {
            R.drawable.ic_action_picture,
            R.drawable.ic_action_web_site,
            R.drawable.ic_action_download,
            R.drawable.ic_action_accounts,
            R.drawable.ic_action_crop,
            R.drawable.ic_action_chat,
            R.drawable.ic_action_settings,
            R.drawable.ic_action_view_as_list,
            R.drawable.ic_action_about};
    private int[] darkImages = new int[] {
            R.drawable.ic_action_picture_dark,
            R.drawable.ic_action_web_site_dark,
            R.drawable.ic_action_download_dark,
            R.drawable.ic_action_accounts_dark,
            R.drawable.ic_action_crop_dark,
            R.drawable.ic_action_chat_dark,
            R.drawable.ic_action_settings_dark,
            R.drawable.ic_action_view_as_list_dark,
            R.drawable.ic_action_about_dark};

    public NavListAdapter(Activity activity, String[] nameArray) {
        fragmentList = new ArrayList<String>();
        Collections.addAll(fragmentList, nameArray);
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }

    @Override
    public Object getItem(int position) {
        return fragmentList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (convertView == null) {
            view = inflater.inflate(R.layout.nav_row, null);
        }

        ImageView fragmentImage = (ImageView) view.findViewById(R.id.fragment_image);
        TextView fragmentTitle = (TextView) view.findViewById(R.id.fragment_title);

        if (AppSettings.getTheme() == R.style.AppLightTheme) {
            fragmentImage.setImageResource(lightImages[position]);
        }
        else {
            fragmentImage.setImageResource(darkImages[position]);
        }
        fragmentTitle.setText(fragmentList.get(position));

        return view;
    }
}
