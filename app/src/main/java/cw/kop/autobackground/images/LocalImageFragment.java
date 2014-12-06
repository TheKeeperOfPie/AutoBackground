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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.R;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;
import cw.kop.autobackground.sources.SourceInfoFragment;

public class LocalImageFragment extends Fragment implements ListView.OnItemClickListener {

    private static final int FADE_IN_TIME = 350;
    private Context appContext;
    private LocalImageAdapter imageAdapter;
    private ListView imageListView;
    private TextView directoryText;

    private boolean use;
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
        use = bundle.getBoolean("use", false);
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

        final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.local_image_layout,
                container,
                false);

        imageListView = (ListView) view.findViewById(R.id.image_listview);

        TextView emptyText = new TextView(appContext);
        emptyText.setText("Directory is empty");
        emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        emptyText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout emptyLayout = new LinearLayout(appContext);
        emptyLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        emptyLayout.setGravity(Gravity.TOP);
        emptyLayout.addView(emptyText);

        directoryText = (TextView) view.findViewById(R.id.directory_text);
        directoryText.setTextColor(AppSettings.getColorFilterInt(appContext));
        directoryText.setSelected(true);

        int backgroundColor;

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            backgroundColor = getResources().getColor(R.color.LIGHT_THEME_BACKGROUND);
        }
        else {
            backgroundColor = getResources().getColor(R.color.DARK_THEME_BACKGROUND);
        }

        directoryText.setBackgroundColor(backgroundColor);
        imageListView.setBackgroundColor(backgroundColor);
        emptyLayout.setBackgroundColor(backgroundColor);

        ((ViewGroup) imageListView.getParent()).addView(emptyLayout, 0);

        imageListView.setEmptyView(emptyLayout);

        Button useDirectoryButton = (Button) view.findViewById(R.id.use_directory_button);
        useDirectoryButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DialogFactory.ActionDialogListener listener = new DialogFactory.ActionDialogListener() {
                    @Override
                    public void onClickMiddle(View v) {
                        setAppDirectory(false);
                        this.dismissDialog();
                    }

                    @Override
                    public void onClickRight(View v) {
                        setAppDirectory(true);
                        this.dismissDialog();
                    }
                };

                DialogFactory.showActionDialog(appContext,
                        "",
                        "Include subdirectories?",
                        listener,
                        R.string.cancel_button,
                        R.string.no_button,
                        R.string.yes_button);

            }

        });

        if (!viewPath.equals("")) {
            directoryText.setVisibility(View.GONE);
            useDirectoryButton.setVisibility(View.GONE);
            view.findViewById(R.id.button_container).setVisibility(View.GONE);
        }

        return view;
    }

    private void setAppDirectory(boolean useSubdirectories) {
        final File dir = imageAdapter.getDirectory();
        FilenameFilter filenameFilter = FileHandler.getImageFileNameFilter();
        if (setPath) {
            AppSettings.setDownloadPath(dir.getAbsolutePath());
            Toast.makeText(appContext,
                    "Download path set to: \n" + AppSettings.getDownloadPath(),
                    Toast.LENGTH_SHORT).show();
        }
        else {
            int numImages = 0;
            if (dir.listFiles(filenameFilter) != null) {
                numImages = dir.listFiles(filenameFilter).length;
            }

            final StringBuilder stringBuilder = new StringBuilder();

            if (useSubdirectories) {

                Toast.makeText(appContext, "Loading subdirectories...", Toast.LENGTH_SHORT).show();

                final int finalNumImages = numImages;
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        ArrayList<String> folderNames = getAllDirectories(dir);

                        for (String folderName : folderNames) {
                            stringBuilder.append(folderName);
                            stringBuilder.append(AppSettings.DATA_SPLITTER);
                            Log.i("LIF", folderName);
                        }

                        if (getTargetFragment() != null) {

                            ((SourceInfoFragment) getTargetFragment()).setData(AppSettings.FOLDER,
                                    dir.getName(),
                                    "",
                                    stringBuilder.toString(),
                                    "",
                                    finalNumImages);
                        }
                    }
                }).start();
            }
            else {
                ((SourceInfoFragment) getTargetFragment()).setData(AppSettings.FOLDER,
                        dir.getName(),
                        "",
                        dir.getAbsolutePath(),
                        "",
                        numImages);
            }

        }
        imageAdapter.setFinished();
        getActivity().onBackPressed();
    }

    private ArrayList<String> getAllDirectories(File dir) {

        ArrayList<String> directoryList = new ArrayList<>();

        File[] fileList = dir.listFiles();

        if (fileList != null) {
            if (dir.listFiles(FileHandler.getImageFileNameFilter()).length > 0) {
                directoryList.add(dir.getAbsolutePath());
            }

            for (File folder : fileList) {
                if (folder.isDirectory()) {
                    directoryList.addAll(getAllDirectories(folder));
                }
            }
        }

        return directoryList;

    }

    public boolean onBackPressed() {

        boolean endDirectory = imageAdapter.backDirectory();
        directoryText.setText(imageAdapter.getDirectory().getAbsolutePath());

        return endDirectory;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (imageAdapter == null) {
            if (viewPath.equals("")) {
                imageAdapter = new LocalImageAdapter(getActivity(), new File(File.separator), true);
            }
            else {
                imageAdapter = new LocalImageAdapter(getActivity(), new File(viewPath), false);
            }
        }

        imageListView.setAdapter(imageAdapter);
        if (viewPath.equals("")) {
            imageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    File selectedFile = imageAdapter.getItem(position);

                    if (selectedFile.exists() && selectedFile.isDirectory()) {
                        imageAdapter.setDirectory(selectedFile);
                        directoryText.setText(imageAdapter.getDirectory().getAbsolutePath());
                    }
                }
            });
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            if (externalStorageDirectory.exists()) {
                imageAdapter.setDirectory(externalStorageDirectory);
            }
        }
        else {
            imageListView.setOnItemClickListener(this);
        }

        directoryText.setText(imageAdapter.getDirectory().getAbsolutePath());
    }

    @Override
    public void onStart() {
        super.onStart();

        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                imageListView.setAlpha(interpolatedTime);
            }
        };

        if (!viewPath.equals("")) {
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    imageListView.getChildAt(0).setAlpha(1.0f);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }

        animation.setDuration(FADE_IN_TIME);
        animation.setInterpolator(new DecelerateInterpolator(3.0f));
//        imageListView.startAnimation(animation);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int positionInList, long id) {

        File selectedFile = imageAdapter.getItem(positionInList);

        if (selectedFile.exists() && selectedFile.isDirectory()) {
            imageAdapter.setDirectory(selectedFile);
            directoryText.setText(imageAdapter.getDirectory().getAbsolutePath());
        }
        else if (imageAdapter.getItem(positionInList).getName().contains(".png") || imageAdapter.getItem(
                positionInList).getName().contains(".jpg") || imageAdapter.getItem(positionInList).getName().contains(
                ".jpeg")) {
            showImageDialog(positionInList);
        }
    }

    private void showImageDialog(final int index) {

        DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        openImage(index);
                        break;
                    case 1:
                        File file = imageAdapter.getItem(index);

                        if (file.exists() && file.isFile()) {
                            if (FileHandler.getCurrentBitmapFile() != null && file.getAbsolutePath().equals(FileHandler.getCurrentBitmapFile().getAbsolutePath())) {
                                Intent intent = new Intent();
                                intent.setAction(LiveWallpaperService.CYCLE_IMAGE);
                                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                appContext.sendBroadcast(intent);
                            }
                            file.delete();
                            imageAdapter.remove(index);
                        }
                }
                dismissDialog();
            }
        };

        DialogFactory.showListDialog(appContext,
                "",
                clickListener,
                R.array.history_menu);
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
