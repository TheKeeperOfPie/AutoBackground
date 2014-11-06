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

package cw.kop.autobackground.sources;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/5/2014.
 */
public class SourceInfoFragment extends Fragment{

    private Context appContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.source_info_fragment, container, false);

        EditText sourceTitle = (EditText) view.findViewById(R.id.source_title);
        EditText sourcePrefix = (EditText) view.findViewById(R.id.source_data_prefix);
        EditText sourceData = (EditText) view.findViewById(R.id.source_data);
        EditText sourceSuffix = (EditText) view.findViewById(R.id.source_data_suffix);
        EditText sourceNum = (EditText) view.findViewById(R.id.source_num);

        Bundle arguments = getArguments();

        sourceTitle.setText(arguments.getString("title"));
        sourcePrefix.setText(arguments.getString("prefix"));
        sourceData.setText(arguments.getString("data"));
        sourceSuffix.setText(arguments.getString("suffix"));
        sourceNum.setText(arguments.getString("num"));

        ImageView sourceImage = (ImageView) view.findViewById(R.id.source_image);
        File imageFile = new File(arguments.getString("image"));
        if (imageFile.exists() && imageFile.isFile()) {
            Picasso.with(appContext).load(imageFile).fit().centerCrop().into(sourceImage);
        }


        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            view.setBackgroundColor(getResources().getColor(R.color.LIGHT_THEME_BACKGROUND));
        }
        else {
            view.setBackgroundColor(getResources().getColor(R.color.DARK_THEME_BACKGROUND));
        }

        return view;
    }


}
