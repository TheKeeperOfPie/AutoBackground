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

package cw.kop.autobackground;

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

import java.util.ArrayList;
import java.util.Collections;

import cw.kop.autobackground.settings.AppSettings;

public class NavListAdapter extends BaseAdapter {

    private static LayoutInflater inflater = null;
    private ArrayList<String> fragmentList;
    private int[] iconImages = new int[] {
            R.drawable.ic_action_picture_white,
            R.drawable.ic_action_web_site_white,
            R.drawable.ic_action_download_white,
            R.drawable.ic_action_accounts_white,
            R.drawable.ic_action_crop_white,
            R.drawable.ic_action_chat_white,
            R.drawable.ic_action_settings_white,
            R.drawable.ic_action_view_as_list_white,
            R.drawable.ic_action_about_white};
    private int colorFilterInt;

    public NavListAdapter(Activity activity, String[] nameArray) {
        fragmentList = new ArrayList<>();
        Collections.addAll(fragmentList, nameArray);
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        switch (AppSettings.getTheme()) {
            default:
            case AppSettings.APP_LIGHT_THEME:
                colorFilterInt = activity.getResources().getColor(R.color.DARK_GRAY_OPAQUE);
                break;
            case AppSettings.APP_DARK_THEME:
            case AppSettings.APP_TRANSPARENT_THEME:
                colorFilterInt = activity.getResources().getColor(R.color.LIGHT_GRAY_OPAQUE);
                break;
        }
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
            view = inflater.inflate(R.layout.nav_row, parent, false);
        }

        ImageView fragmentImage = (ImageView) view.findViewById(R.id.fragment_image);
        TextView fragmentTitle = (TextView) view.findViewById(R.id.fragment_title);

        Drawable iconDrawable = parent.getContext().getResources().getDrawable(iconImages[position]);
        iconDrawable.setColorFilter(colorFilterInt, PorterDuff.Mode.MULTIPLY);
        fragmentImage.setImageDrawable(iconDrawable);

        fragmentTitle.setText(fragmentList.get(position));

        return view;
    }
}
