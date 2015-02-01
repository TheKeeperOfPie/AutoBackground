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

package cw.kop.autobackground.images;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

public class FolderFragment extends Fragment {

    public static final String USE_DIRECTORY = "useDirectory";
    public static final String SHOW_DIRECTORY_TEXT = "showDirectoryText";

    private Context context;

    private ListView fileListView;
    private TextView directoryText;
    private Button useDirectoryButton;
    private FolderEventListener listener;
    private BaseAdapter adapter;
    private String startDirectoryText;

    public FolderFragment() {
        // Required empty public constructor
    }

    public void setListener(FolderEventListener listener) {
        this.listener = listener;
    }

    public void setAdapter(BaseAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_folder, container, false);


        view.setBackgroundResource(AppSettings.getBackgroundColorResource());

        fileListView = (ListView) view.findViewById(R.id.image_listview);

        TextView emptyText = new TextView(context);
        emptyText.setText("Directory is empty");
        emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        emptyText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout emptyLayout = new LinearLayout(context);
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        emptyLayout.setGravity(Gravity.TOP);
        emptyLayout.addView(emptyText);

        directoryText = (TextView) view.findViewById(R.id.directory_text);
        directoryText.setTextColor(AppSettings.getColorFilterInt(context));
        directoryText.setText(startDirectoryText);
        directoryText.setSelected(true);

        int backgroundColor;

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            backgroundColor = getResources().getColor(R.color.LIGHT_THEME_BACKGROUND);
        }
        else {
            backgroundColor = getResources().getColor(R.color.DARK_THEME_BACKGROUND);
        }

        directoryText.setBackgroundColor(backgroundColor);
        fileListView.setBackgroundColor(backgroundColor);
        emptyLayout.setBackgroundColor(backgroundColor);

        ((ViewGroup) fileListView.getParent()).addView(emptyLayout, 0);
        fileListView.setEmptyView(emptyLayout);

        useDirectoryButton = (Button) view.findViewById(R.id.use_directory_button);

        if (getArguments() != null) {
            if (getArguments().getBoolean(FolderFragment.SHOW_DIRECTORY_TEXT, true)) {
                directoryText.setVisibility(View.VISIBLE);
            }
            if (getArguments().getBoolean(FolderFragment.USE_DIRECTORY, false)) {
                useDirectoryButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onUseDirectoryClick();
                    }
                });
                useDirectoryButton.setVisibility(View.VISIBLE);
            }
        }

        fileListView.setAdapter(adapter);
        fileListView.setOnItemClickListener(listener);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        context = null;
    }

    public boolean onBackPressed() {
        return listener.onBackPressed();
    }

    public void setStartingDirectoryText(String text) {
        startDirectoryText = text;
    }

    public void setDirectoryText(String text) {
        directoryText.setText(text);
    }

    public interface FolderEventListener extends AdapterView.OnItemClickListener {

        void onUseDirectoryClick();
        void onItemClick(AdapterView<?> parent, View view, int positionInList, long id);
        boolean onBackPressed();

    }

}
