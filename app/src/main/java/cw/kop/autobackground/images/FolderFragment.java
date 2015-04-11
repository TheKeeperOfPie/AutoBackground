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
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

public class FolderFragment extends Fragment implements FolderCallback {

    public static final String USE_DIRECTORY = "useDirectory";
    public static final String SHOW_DIRECTORY_TEXT = "showDirectoryText";

    private Activity activity;

    private RecyclerView recyclerFiles;
    private RecyclerView.LayoutManager layoutManager;
    private TextView directoryText;
    private Button useDirectoryButton;
    private FolderEventListener listener;
    private RecyclerView.Adapter adapter;
    private String directory;
    private TextView emptyText;

    public FolderFragment() {
        // Required empty public constructor
    }

    public void setListener(FolderEventListener listener) {
        this.listener = listener;
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_folder, container, false);


        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if (displayMetrics.heightPixels > displayMetrics.widthPixels) {
            layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL,
                    false);
        }
        else {
            layoutManager = new GridLayoutManager(activity, 2, LinearLayoutManager.VERTICAL, false);
        }

        view.setBackgroundResource(AppSettings.getBackgroundColorResource());

        recyclerFiles = (RecyclerView) view.findViewById(R.id.recycler_files);
        recyclerFiles.setLayoutManager(layoutManager);
        recyclerFiles.setHasFixedSize(true);

        emptyText = (TextView) view.findViewById(R.id.empty_text);

        directoryText = (TextView) view.findViewById(R.id.directory_text);
        directoryText.setTextColor(AppSettings.getColorFilterInt(activity));
        directoryText.setText(directory);
        directoryText.setSelected(true);

        int backgroundColor;

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            backgroundColor = getResources().getColor(R.color.LIGHT_THEME_BACKGROUND);
        }
        else {
            backgroundColor = getResources().getColor(R.color.DARK_THEME_BACKGROUND);
        }

        directoryText.setBackgroundColor(backgroundColor);
        recyclerFiles.setBackgroundColor(backgroundColor);

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

        recyclerFiles.setAdapter(adapter);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
    }

    public boolean onBackPressed() {
        return listener.onBackPressed();
    }

    public void setStartingDirectoryText(String text) {
        directory = text;
    }

    public void setDirectoryText(String text) {
        directory = text;
        directoryText.setText(text);
    }

    @Override
    public float getItemWidth() {
        if (layoutManager instanceof GridLayoutManager) {
            return recyclerFiles.getWidth() / ((GridLayoutManager) layoutManager).getSpanCount();
        }
        return recyclerFiles.getWidth();
    }

    @Override
    public void onItemClick(int position) {
        listener.onItemClick(position);
    }

    @Override
    public void setEmptyTextVisibility(int visibility) {
        emptyText.setVisibility(visibility);
    }

    public interface FolderEventListener {

        void onUseDirectoryClick();
        void onItemClick(int positionInList);
        boolean onBackPressed();

    }

}
