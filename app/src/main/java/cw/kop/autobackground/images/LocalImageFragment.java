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
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;

import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.R;
import cw.kop.autobackground.downloader.Downloader;
import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.sources.SourceListFragment;

public class LocalImageFragment extends Fragment implements ListView.OnItemClickListener {

    private Context appContext;
    private LocalImageAdapter imageAdapter;
    private ListView imageListView;

    private boolean setPath;
    private int position;
    private String viewPath = "";

    public LocalImageFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        setPath = bundle.getBoolean("set_path", false);
        viewPath = bundle.getString("view_path", "");
        position = bundle.getInt("position", -1);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        appContext = getActivity();
    }

    @Override
    public void onDetach() {
        appContext = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.local_image_layout, container, false);

        imageListView = (ListView) view.findViewById(R.id.image_listview);

        TextView emptyText = new TextView(appContext);
        emptyText.setText("Directory is empty");
        emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        emptyText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout emptyLayout = new LinearLayout(appContext);
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emptyLayout.setGravity(Gravity.TOP);
        emptyLayout.addView(emptyText);

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            imageListView.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
            emptyLayout.setBackgroundColor(getResources().getColor(R.color.WHITE_OPAQUE));
        }
        else {
            imageListView.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
            emptyLayout.setBackgroundColor(getResources().getColor(R.color.BLACK_OPAQUE));
        }

        ((ViewGroup) imageListView.getParent()).addView(emptyLayout, 0);

        imageListView.setEmptyView(emptyLayout);

        Button useDirectoryButton = (Button) view.findViewById(R.id.use_directory_button);
        useDirectoryButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                File dir = imageAdapter.getDirectory();
                FilenameFilter filenameFilter = Downloader.getImageFileNameFilter();
                if (setPath) {
                    AppSettings.setDownloadPath(dir.getAbsolutePath());
                    Toast.makeText(appContext, "Download path set to: \n" + AppSettings.getDownloadPath(), Toast.LENGTH_SHORT).show();
                }
                else {
                    int numImages = 0;
                    if (dir.listFiles(filenameFilter) != null) {
                        numImages = dir.listFiles(filenameFilter).length;
                    }

                    Intent returnEntryIntent = new Intent();
                    if (position > -1) {
                        returnEntryIntent.setAction(SourceListFragment.SET_ENTRY);
                        returnEntryIntent.putExtra("position", position);
                    }
                    else {
                        returnEntryIntent.setAction(SourceListFragment.ADD_ENTRY);
                    }

                    returnEntryIntent.putExtra("type", AppSettings.FOLDER);
                    returnEntryIntent.putExtra("title", dir.getName());
                    returnEntryIntent.putExtra("data", dir.getAbsolutePath());
                    returnEntryIntent.putExtra("num", numImages);

                    LocalBroadcastManager.getInstance(appContext).sendBroadcast(returnEntryIntent);

                }
                imageAdapter.setFinished();
                getActivity().onBackPressed();
            }

        });

        if (!viewPath.equals("")) {
            useDirectoryButton.setVisibility(View.GONE);
        }

        return view;
    }

    public boolean onBackPressed() {
        return imageAdapter.backDirectory();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        File dir = new File("/");

        if (!viewPath.equals("")) {
            dir = new File(viewPath);
        }

        if (imageAdapter == null) {
            imageAdapter = new LocalImageAdapter(getActivity(), dir);
        }

        imageListView.setAdapter(imageAdapter);
        if (!viewPath.equals("")) {
            imageListView.setOnItemClickListener(this);
        }
        else {
            imageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    File selectedFile = imageAdapter.getItem(position);

                    if (selectedFile.exists() && selectedFile.isDirectory()) {
                        imageAdapter.setDirectory(selectedFile);
                    }
                }
            });
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int positionInList, long id) {

        if (imageAdapter.getItem(positionInList).getName().contains(".png") || imageAdapter.getItem(positionInList).getName().contains(".jpg") || imageAdapter.getItem(positionInList).getName().contains(".jpeg")) {
            showImageDialog(positionInList);
        }
    }

    private void showImageDialog(final int index) {

        final Dialog dialog = AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME) ? new Dialog(appContext, R.style.LightDialogTheme) : new Dialog(appContext, R.style.DarkDialogTheme);

        View dialogView = View.inflate(appContext, R.layout.list_dialog, null);
        dialog.setContentView(dialogView);

        ListView dialogList = (ListView) dialogView.findViewById(R.id.dialog_list);
        dialogList.setAdapter(new ArrayAdapter<>(appContext, android.R.layout.simple_list_item_1, android.R.id.text1, getResources().getStringArray(R.array.history_menu)));
        dialogList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        openImage(index);
                        break;
                    case 1:
                        File file = imageAdapter.getItem(index);

                        if (file.exists() && file.isFile()) {
                            if (file.getAbsolutePath().equals(Downloader.getCurrentBitmapFile().getAbsolutePath())) {
                                Intent intent = new Intent();
                                intent.setAction(LiveWallpaperService.CYCLE_IMAGE);
                                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                appContext.sendBroadcast(intent);
                            }
                            file.delete();
                            imageAdapter.remove(index);
                        }
                }
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void openImage(int index) {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_VIEW);
        galleryIntent.setDataAndType(Uri.fromFile(imageAdapter.getItem(index)), "image/*");
        galleryIntent = Intent.createChooser(galleryIntent, "Open Image");
        galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(galleryIntent);
    }
}
