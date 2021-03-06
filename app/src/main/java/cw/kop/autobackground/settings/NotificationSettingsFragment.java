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

package cw.kop.autobackground.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

import cw.kop.autobackground.DialogFactory;
import cw.kop.autobackground.DividerItemDecoration;
import cw.kop.autobackground.LiveWallpaperService;
import cw.kop.autobackground.OptionData;
import cw.kop.autobackground.OptionsListAdapter;
import cw.kop.autobackground.R;
import cw.kop.autobackground.RecyclerViewListClickListener;
import cw.kop.autobackground.files.FileHandler;

public class NotificationSettingsFragment extends PreferenceFragment implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private final static long CONVERT_MILLES_TO_MIN = 60000;
    private static final int SELECT_PHOTO = 4;
    private Context appContext;

    private ListView preferenceList;
    private RecyclerView recyclerView;
    private RelativeLayout notificationPreview;
    private ImageView notificationIcon;
    private ImageView notificationIconActionIndicator;
    private ImageView notificationIconHighlight;
    private TextView notificationTitle;
    private TextView notificationSummary;
    private ImageView notificationTitleHighlight;
    private ImageView notificationSummaryHighlight;
    private ImageView notificationPreviewHighlight;
    private ImageView optionOneImage;
    private ImageView optionTwoImage;
    private ImageView optionThreeImage;
    private TextView optionOneText;
    private TextView optionTwoText;
    private TextView optionThreeText;
    private ImageView optionOneHighlight;
    private ImageView optionTwoHighlight;
    private ImageView optionThreeHighlight;
    private Preference historySizePref;

    public NotificationSettingsFragment() {
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        preferenceList = (ListView) view.findViewById(android.R.id.list);

        historySizePref = findPreference("history_size");
        historySizePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showHistorySizeDialog();
                return true;
            }
        });

        recyclerView = (RecyclerView) view.findViewById(R.id.notification_options_list);

        notificationPreview = (RelativeLayout) view.findViewById(R.id.notification_preview);

        notificationIcon = (ImageView) view.findViewById(R.id.notification_options_icon);
        notificationIconActionIndicator = (ImageView) view.findViewById(R.id.notification_icon_action_indicator);
        notificationIconHighlight = (ImageView) view.findViewById(R.id.notification_icon_highlight);

        notificationTitle = (TextView) view.findViewById(R.id.notification_options_title);
        notificationSummary = (TextView) view.findViewById(R.id.notification_options_summary);

        notificationTitleHighlight = (ImageView) view.findViewById(R.id.notification_title_highlight);
        notificationSummaryHighlight = (ImageView) view.findViewById(R.id.notification_summary_highlight);

        View notificationBuffer = view.findViewById(R.id.notification_options_buffer);

        notificationPreviewHighlight = (ImageView) view.findViewById(R.id.notification_preview_highlight);

        RelativeLayout optionOne = (RelativeLayout) view.findViewById(R.id.notification_option_one);
        RelativeLayout optionTwo = (RelativeLayout) view.findViewById(R.id.notification_option_two);
        RelativeLayout optionThree = (RelativeLayout) view.findViewById(R.id.notification_option_three);

        optionOneImage = (ImageView) view.findViewById(R.id.notification_option_one_image);
        optionTwoImage = (ImageView) view.findViewById(R.id.notification_option_two_image);
        optionThreeImage = (ImageView) view.findViewById(R.id.notification_option_three_image);

        optionOneText = (TextView) view.findViewById(R.id.notification_option_one_text);
        optionTwoText = (TextView) view.findViewById(R.id.notification_option_two_text);
        optionThreeText = (TextView) view.findViewById(R.id.notification_option_three_text);

        optionOneHighlight = (ImageView) view.findViewById(R.id.notification_option_one_highlight);
        optionTwoHighlight = (ImageView) view.findViewById(R.id.notification_option_two_highlight);
        optionThreeHighlight = (ImageView) view.findViewById(R.id.notification_option_three_highlight);

        notificationIcon.setOnClickListener(this);

        notificationTitle.setOnClickListener(this);
        notificationSummary.setOnClickListener(this);

        notificationBuffer.setOnClickListener(this);

        optionOne.setOnClickListener(this);
        optionTwo.setOnClickListener(this);
        optionThree.setOnClickListener(this);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.addItemDecoration(new DividerItemDecoration(appContext, DividerItemDecoration.VERTICAL_LIST));

        Preference iconActionPref = findPreference("notification_icon_action");
        iconActionPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
                        String[] actionsArray = getResources().getStringArray(R.array.notification_options);

                        AppSettings.setNotificationIconAction(actionsArray[position]);

                        TypedArray drawables = getResources().obtainTypedArray(R.array.notification_options_icons);

                        if (actionsArray[position].equals("None")) {
                            notificationIconActionIndicator.setImageResource(getResources().getColor(
                                    R.color.TRANSPARENT_BACKGROUND));
                            AppSettings.setNotificationIconActionDrawable(R.color.TRANSPARENT_BACKGROUND);
                        }
                        else {
                            notificationIconActionIndicator.setImageResource(drawables.getResourceId(
                                    position,
                                    R.color.TRANSPARENT_BACKGROUND));
                            AppSettings.setNotificationIconActionDrawable(drawables.getResourceId(
                                    position,
                                    R.color.TRANSPARENT_BACKGROUND));
                        }
                        dismissDialog();
                    }
                };

                DialogFactory.showListDialog(appContext,
                        "Choose icon action:",
                        clickListener,
                        R.array.notification_options);

                return true;
            }
        });

        Log.i("NSF", "Options shown");


        if (!AppSettings.useAdvanced()) {

            PreferenceCategory notificationPreferences = (PreferenceCategory) findPreference(
                    "title_notification_settings");

            notificationPreferences.removePreference(findPreference("use_notification_game"));
            notificationPreferences.removePreference(findPreference(
                    "high_resolution_notification_icon"));

        }

        if (AppSettings.useNotificationIconFile()) {
            Log.i("NSF", "Loading file");
            File image = new File(AppSettings.getNotificationIconFile());

            if (image.exists() && image.isFile()) {
                Picasso.with(appContext).load(image).fit().centerCrop().into(notificationIcon);
                Log.i("NSF", "Loading custom image");
            }

        }
        else if (FileHandler.getCurrentBitmapFile() != null && (AppSettings.getNotificationIcon() == R.drawable.ic_photo_white_24dp)) {
            Picasso.with(appContext).load(FileHandler.getCurrentBitmapFile()).fit().centerCrop().into(
                    notificationIcon);
        }
        else {
            Log.i("NSF", "Loading default image");
            notificationIcon.setImageResource(AppSettings.getNotificationIcon());
        }

        notificationIconActionIndicator.setImageResource(AppSettings.getNotificationIconActionDrawable());

        notificationPreview.setBackgroundColor(AppSettings.getNotificationColor());


        if (AppSettings.getNotificationTitle().equals("Location")) {
            if (FileHandler.getCurrentBitmapFile() != null) {
                notificationTitle.setText(FileHandler.getCurrentBitmapFile().getAbsolutePath());
            }
            else {
                notificationTitle.setText(AppSettings.getNotificationTitle());
            }
        }
        if (AppSettings.getNotificationSummary().equals("Location")) {
            if (FileHandler.getCurrentBitmapFile() != null) {
                notificationSummary.setText(FileHandler.getCurrentBitmapFile().getAbsolutePath());
            }
            else {
                notificationSummary.setText(AppSettings.getNotificationSummary());
            }
        }

        notificationTitle.setTextColor(AppSettings.getNotificationTitleColor());
        notificationSummary.setTextColor(AppSettings.getNotificationSummaryColor());

        Drawable coloredImageOne = appContext.getResources().getDrawable(AppSettings.getNotificationOptionDrawable(
                0));
        Drawable coloredImageTwo = appContext.getResources().getDrawable(AppSettings.getNotificationOptionDrawable(
                1));
        Drawable coloredImageThree = appContext.getResources().getDrawable(AppSettings.getNotificationOptionDrawable(
                2));

        coloredImageOne.mutate().setColorFilter(AppSettings.getNotificationOptionColor(0),
                PorterDuff.Mode.MULTIPLY);
        coloredImageTwo.mutate().setColorFilter(AppSettings.getNotificationOptionColor(1),
                PorterDuff.Mode.MULTIPLY);
        coloredImageThree.mutate().setColorFilter(AppSettings.getNotificationOptionColor(2),
                PorterDuff.Mode.MULTIPLY);

        optionOneImage.setImageDrawable(coloredImageOne);
        optionTwoImage.setImageDrawable(coloredImageTwo);
        optionThreeImage.setImageDrawable(coloredImageThree);

        optionOneText.setText(AppSettings.getNotificationOptionTitle(0));
        optionOneText.setTextColor(AppSettings.getNotificationOptionColor(0));
        optionTwoText.setText(AppSettings.getNotificationOptionTitle(1));
        optionTwoText.setTextColor(AppSettings.getNotificationOptionColor(1));
        optionThreeText.setText(AppSettings.getNotificationOptionTitle(2));
        optionThreeText.setTextColor(AppSettings.getNotificationOptionColor(2));

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_notification);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        Intent intent = new Intent();
        intent.setAction(LiveWallpaperService.UPDATE_NOTIFICATION);
        intent.putExtra("use", AppSettings.useNotification());
        appContext.sendBroadcast(intent);
        super.onPause();
    }

    public boolean onBackPressed() {

        if (recyclerView.isShown()) {
            clearHighlights();
            return true;
        }

        return false;
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.notification_option_one) {
            clearHighlights();
            optionOneHighlight.setVisibility(View.VISIBLE);
            showOptionList(0);
        }
        else if (v.getId() == R.id.notification_option_two) {
            clearHighlights();
            optionTwoHighlight.setVisibility(View.VISIBLE);
            showOptionList(1);
        }
        else if (v.getId() == R.id.notification_option_three) {
            clearHighlights();
            optionThreeHighlight.setVisibility(View.VISIBLE);
            showOptionList(2);
        }
        else if (v.getId() == R.id.notification_options_title) {
            clearHighlights();
            notificationTitleHighlight.setVisibility(View.VISIBLE);
            showTitlesList(4);
        }
        else if (v.getId() == R.id.notification_options_summary) {
            clearHighlights();
            notificationSummaryHighlight.setVisibility(View.VISIBLE);
            showTitlesList(5);
        }
        else if (v.getId() == R.id.notification_options_icon) {
            clearHighlights();
            notificationIconHighlight.setVisibility(View.VISIBLE);
            showIconList();
        }
        else if (v.getId() == R.id.notification_options_buffer) {
            clearHighlights();
            notificationPreviewHighlight.setVisibility(View.VISIBLE);
            showBackgroundColorDialog();
        }

        preferenceList.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

    }

    private void showBackgroundColorDialog() {

        DialogFactory.ColorDialogListener listener = new DialogFactory.ColorDialogListener() {
            @Override
            public void onClickRight(View v) {
                AppSettings.setNotificationColor(getColorPickerView().getColor());
                notificationPreview.setBackgroundColor(getColorPickerView().getColor());
                clearHighlights();
                recyclerView.setAdapter(null);
                this.dismissDialog();
            }
        };

        DialogFactory.showColorPickerDialog(appContext,
                "Enter background color:",
                listener,
                -1,
                R.string.cancel_button,
                R.string.ok_button,
                AppSettings.getNotificationColor());

    }

    private void clearHighlights() {
        notificationIconHighlight.setVisibility(View.GONE);
        notificationPreviewHighlight.setVisibility(View.GONE);
        notificationTitleHighlight.setVisibility(View.GONE);
        notificationSummaryHighlight.setVisibility(View.GONE);
        optionOneHighlight.setVisibility(View.GONE);
        optionTwoHighlight.setVisibility(View.GONE);
        optionThreeHighlight.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        preferenceList.setVisibility(View.VISIBLE);
    }

    private void showIconList() {

        String[] iconTitles = appContext.getResources().getStringArray(R.array.notification_icon);
        String[] iconSummaries = appContext.getResources().getStringArray(R.array.notification_icon_descriptions);
        TypedArray iconIcons = appContext.getResources().obtainTypedArray(R.array.notification_icon_icons);

        ArrayList<OptionData> optionsList = new ArrayList<>();

        for (int index = 0; index < iconTitles.length; index++) {
            optionsList.add(new OptionData(iconTitles[index],
                    iconSummaries[index],
                    iconIcons.getResourceId(index,
                            R.color.TRANSPARENT_BACKGROUND)));
        }

        RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                if (title.equals("Application")) {
                    AppSettings.setNotificationIcon(R.drawable.app_icon);
                    notificationIcon.setImageResource(R.drawable.app_icon);
                    clearHighlights();
                    recyclerView.setAdapter(null);
                    AppSettings.setUseNotificationIconFile(false);
                }
                else if (title.equals("Image")) {
                    AppSettings.setNotificationIcon(drawable);
                    int imageSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            64,
                            appContext.getResources().getDisplayMetrics()));
                    Picasso.with(appContext).load(FileHandler.getCurrentBitmapFile()).resize(
                            imageSize,
                            imageSize).into(notificationIcon);
                    clearHighlights();
                    recyclerView.setAdapter(null);
                    AppSettings.setUseNotificationIconFile(false);
                }
                else if (title.equals("None")) {
                    AppSettings.setNotificationIcon(R.drawable.icon_blank);
                    notificationIcon.setImageResource(R.drawable.icon_blank);
                    clearHighlights();
                    recyclerView.setAdapter(null);
                    AppSettings.setUseNotificationIconFile(false);
                }
                else if (title.equals("Custom")) {
                    Intent imageIntent = new Intent(Intent.ACTION_PICK);
                    imageIntent.setType("image/*");
                    startActivityForResult(imageIntent, SELECT_PHOTO);
                }

            }
        };

        OptionsListAdapter titlesAdapter = new OptionsListAdapter(appContext,
                optionsList,
                -1,
                listener);

        recyclerView.setAdapter(titlesAdapter);

        iconIcons.recycle();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PHOTO && resultCode == Activity.RESULT_OK) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = appContext.getContentResolver().query(selectedImage,
                    filePathColumn,
                    null,
                    null,
                    null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();

            File image = new File(filePath);

            if (image.exists() && image.isFile()) {
                AppSettings.setNotificationIconFile(filePath);
                AppSettings.setUseNotificationIconFile(true);
                int imageSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        64,
                        appContext.getResources().getDisplayMetrics()));
                Picasso.with(appContext).load(image).resize(imageSize, imageSize).into(
                        notificationIcon);
            }
            clearHighlights();
            recyclerView.setAdapter(null);
        }

    }

    private void showTitlesList(final int optionIndex) {

        String[] titleTitles = appContext.getResources().getStringArray(R.array.notification_titles);
        String[] titleSummaries = appContext.getResources().getStringArray(R.array.notification_titles_descriptions);
        TypedArray titlesIcons = appContext.getResources().obtainTypedArray(R.array.notification_titles_icons);

        ArrayList<OptionData> optionsList = new ArrayList<>();

        for (int index = 0; index < titleTitles.length; index++) {
            optionsList.add(new OptionData(titleTitles[index],
                    titleSummaries[index],
                    titlesIcons.getResourceId(index,
                            R.color.TRANSPARENT_BACKGROUND)));
        }

        RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                if (title.equals("None")) {
                    clearHighlights();
                    recyclerView.setAdapter(null);
                    switch (optionIndex) {
                        case 4:
                            AppSettings.setNotificationTitle("");
                            notificationTitle.setText("");
                            break;
                        case 5:
                            AppSettings.setNotificationSummary("");
                            notificationSummary.setText("");
                            break;
                    }
                }
                else if (title.equals("Custom")) {
                    showDialogForText(optionIndex, drawable);
                }
                else {
                    showTitleColorDialog(optionIndex, title, drawable);
                }
            }
        };

        OptionsListAdapter titlesAdapter = new OptionsListAdapter(appContext,
                optionsList,
                optionIndex,
                listener);

        recyclerView.setAdapter(titlesAdapter);

        titlesIcons.recycle();

    }

    private void showOptionList(final int optionIndex) {

        String[] optionsTitles = appContext.getResources().getStringArray(R.array.notification_options);
        String[] optionsSummaries = appContext.getResources().getStringArray(R.array.notification_options_descriptions);
        TypedArray optionsIcons = appContext.getResources().obtainTypedArray(R.array.notification_options_icons);

        ArrayList<OptionData> optionsList = new ArrayList<>();

        for (int index = 0; index < optionsTitles.length; index++) {

            if (!AppSettings.useAdvanced() && optionsTitles[index].equals("Game")) {
                continue;
            }

            optionsList.add(new OptionData(optionsTitles[index],
                    optionsSummaries[index],
                    optionsIcons.getResourceId(index,
                            R.color.TRANSPARENT_BACKGROUND)));
        }

        RecyclerViewListClickListener listener = new RecyclerViewListClickListener() {
            @Override
            public void onClick(int position, String title, int drawable) {

                if (title.equals("None")) {
                    title = "";
                    drawable = R.color.TRANSPARENT_BACKGROUND;

                    clearHighlights();
                    AppSettings.setNotificationOptionTitle(optionIndex, title);
                    AppSettings.setNotificationOptionDrawable(optionIndex, drawable);
                    recyclerView.setAdapter(null);

                    switch (optionIndex) {
                        case 0:
                            optionOneText.setText(title);
                            optionOneImage.setImageResource(drawable);
                            break;
                        case 1:
                            optionTwoText.setText(title);
                            optionTwoImage.setImageResource(drawable);
                            break;
                        case 2:
                            optionThreeText.setText(title);
                            optionThreeImage.setImageResource(drawable);
                            break;
                    }
                }
                else if (title.equals("Pin")) {
                    showDialogForPin(optionIndex, title, drawable);
                }
                else {
                    showOptionColorDialog(optionIndex, title, drawable);
                }
            }
        };

        OptionsListAdapter optionsAdapter = new OptionsListAdapter(appContext,
                optionsList,
                optionIndex,
                listener);

        recyclerView.setAdapter(optionsAdapter);

        optionsIcons.recycle();

    }

    private void showTitleColorDialog(final int index, final String title, final int drawable) {

        DialogFactory.ColorDialogListener listener = new DialogFactory.ColorDialogListener() {

            @Override
            public void onClickRight(View v) {
                clearHighlights();
                recyclerView.setAdapter(null);
                switch (index) {
                    case 4:
                        AppSettings.setNotificationTitleColor(getColorPickerView().getColor());
                        switch (title) {
                            case "Application":
                                AppSettings.setNotificationTitle("AutoBackground");
                                notificationTitle.setText("AutoBackground");
                                break;
                            case "Location":
                                AppSettings.setNotificationTitle(title);
                                if (FileHandler.getCurrentBitmapFile() != null) {
                                    notificationTitle.setText(FileHandler.getCurrentBitmapFile().getAbsolutePath());
                                }
                                else {
                                    notificationTitle.setText(title);
                                }
                                break;
                            case "None":
                                AppSettings.setNotificationTitle("");
                                notificationTitle.setText("");
                                break;
                            default:
                                AppSettings.setNotificationTitle(title);
                                notificationTitle.setText(title);
                                break;
                        }
                        notificationTitle.setTextColor(AppSettings.getNotificationTitleColor());
                        break;
                    case 5:
                        AppSettings.setNotificationSummaryColor(getColorPickerView().getColor());
                        switch (title) {
                            case "Application":
                                AppSettings.setNotificationTitle("AutoBackground");
                                notificationSummary.setText("AutoBackground");
                                break;
                            case "None":
                                AppSettings.setNotificationSummary("");
                                notificationSummary.setText("");
                                break;
                            default:
                                AppSettings.setNotificationSummary(title);
                                notificationSummary.setText(title);
                                break;
                        }
                        notificationSummary.setTextColor(AppSettings.getNotificationSummaryColor());
                        break;
                }
                this.dismissDialog();
            }
        };

        DialogFactory.showColorPickerDialog(appContext, "Enter text color:", listener, -1,
                R.string.cancel_button, R.string.ok_button,
                index == 4 ? AppSettings.getNotificationTitleColor() :
                        AppSettings.getNotificationSummaryColor());
    }

    private void showOptionColorDialog(final int index, final String title, final int drawable) {

        DialogFactory.ColorDialogListener listener = new DialogFactory.ColorDialogListener() {
            @Override
            public void onClickRight(View v) {
                clearHighlights();
                AppSettings.setNotificationOptionTitle(index, title);
                AppSettings.setNotificationOptionDrawable(index, drawable);
                AppSettings.setNotificationOptionColor(index, getColorPickerView().getColor());
                recyclerView.setAdapter(null);

                switch (index) {
                    case 0:
                        optionOneText.setText(title);
                        optionOneText.setTextColor(AppSettings.getNotificationOptionColor(0));
                        Drawable coloredDrawableOne = appContext.getResources().getDrawable(
                                drawable);
                        coloredDrawableOne.mutate().setColorFilter(AppSettings.getNotificationOptionColor(
                                0), PorterDuff.Mode.MULTIPLY);
                        optionOneImage.setImageDrawable(coloredDrawableOne);
                        break;
                    case 1:
                        optionTwoText.setText(title);
                        optionTwoText.setTextColor(AppSettings.getNotificationOptionColor(1));
                        Drawable coloredDrawableTwo = appContext.getResources().getDrawable(
                                drawable);
                        coloredDrawableTwo.mutate().setColorFilter(AppSettings.getNotificationOptionColor(
                                1), PorterDuff.Mode.MULTIPLY);
                        optionTwoImage.setImageDrawable(coloredDrawableTwo);
                        break;
                    case 2:
                        optionThreeText.setText(title);
                        optionThreeText.setTextColor(AppSettings.getNotificationOptionColor(2));
                        Drawable coloredDrawableThree = appContext.getResources().getDrawable(
                                drawable);
                        coloredDrawableThree.mutate().setColorFilter(AppSettings.getNotificationOptionColor(
                                2), PorterDuff.Mode.MULTIPLY);
                        optionThreeImage.setImageDrawable(coloredDrawableThree);
                        break;
                }
                this.dismissDialog();
            }
        };

        DialogFactory.showColorPickerDialog(appContext,
                "Enter icon and text color:",
                listener,
                -1,
                R.string.cancel_button,
                R.string.ok_button,
                AppSettings.getNotificationOptionPreviousColor());

    }

    private void showDialogForText(final int index, final int drawable) {

        DialogFactory.InputDialogListener listener = new DialogFactory.InputDialogListener() {
            @Override
            public void onClickRight(View v) {
                showTitleColorDialog(index, getEditTextString(), drawable);
                dismissDialog();
            }
        };

        DialogFactory.showInputDialog(appContext,
                "",
                "Enter text:",
                "",
                listener,
                -1,
                R.string.cancel_button,
                R.string.ok_button,
                InputType.TYPE_CLASS_TEXT);
    }

    private void showDialogForPin(final int index, final String title, final int drawable) {

        DialogFactory.ListDialogListener clickListener = new DialogFactory.ListDialogListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                switch (position) {
                    case 0:
                        AppSettings.setPinDuration(0);
                        break;
                    case 1:
                        AppSettings.setPinDuration(5 * CONVERT_MILLES_TO_MIN);
                        break;
                    case 2:
                        AppSettings.setPinDuration(15 * CONVERT_MILLES_TO_MIN);
                        break;
                    case 3:
                        AppSettings.setPinDuration(30 * CONVERT_MILLES_TO_MIN);
                        break;
                    case 4:
                        AppSettings.setPinDuration(AlarmManager.INTERVAL_HOUR);
                        break;
                    case 5:
                        AppSettings.setPinDuration(2 * AlarmManager.INTERVAL_HOUR);
                        break;
                    case 6:
                        AppSettings.setPinDuration(6 * AlarmManager.INTERVAL_HOUR);
                        break;
                    case 7:
                        AppSettings.setPinDuration(AlarmManager.INTERVAL_HALF_DAY);
                        break;
                    default:
                }

                showOptionColorDialog(index, title, drawable);
                dismissDialog();
            }
        };

        DialogFactory.showListDialog(appContext,
                "Pin duration:",
                clickListener,
                R.array.pin_entry_menu);

    }



    private void showHistorySizeDialog() {

        DialogFactory.InputDialogListener listener = new DialogFactory.InputDialogListener() {

            @Override
            public void onClickMiddle(View v) {
                dismissDialog();
            }

            @Override
            public void onClickRight(View v) {

                String value = getEditTextString();

                if (TextUtils.isEmpty(value) ||  Integer.parseInt(value) < 0) {
                    dismissDialog();
                    return;
                }

                int inputValue = Integer.parseInt(value);

                AppSettings.setHistorySize(inputValue);
                dismissDialog();
            }

        };

        DialogFactory.showInputDialog(appContext,
                "Previous History Size",
                "Number of images in buffer",
                "" + AppSettings.getHistorySize(),
                listener,
                -1,
                R.string.cancel_button,
                R.string.ok_button,
                InputType.TYPE_CLASS_NUMBER);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals("use_notification_game")) {
            if (AppSettings.useNotificationGame()) {
                if (FileHandler.getBitmapList().size() < 5) {
                    Toast.makeText(appContext,
                            "Not enough images for game",
                            Toast.LENGTH_SHORT).show();
                    ((SwitchPreference) findPreference("use_notification_game")).setChecked(false);
                }
            }
        }

    }
}
