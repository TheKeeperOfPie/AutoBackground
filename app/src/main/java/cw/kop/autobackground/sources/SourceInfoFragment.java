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
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import cw.kop.autobackground.R;
import cw.kop.autobackground.files.FileHandler;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/5/2014.
 */
public class SourceInfoFragment extends Fragment {

    private static final String TAG = SourceInfoFragment.class.getCanonicalName();
    private static final int FADE_IN_TIME = 350;
    private static final int SLIDE_EXIT_TIME = 350;

    private Context appContext;
    private Drawable imageDrawable;

    private RelativeLayout settingsContainer;
    private ImageView sourceImage;
    private EditText sourceTitle;
    private EditText sourcePrefix;
    private EditText sourceData;
    private EditText sourceSuffix;
    private EditText sourceNum;
    private Switch sourceUse;
    private Button cancelButton;
    private Button saveButton;

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

//    @Override
//    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
//
//        Animator animator = AnimatorInflater.loadAnimator(appContext, nextAnim);
//
//        animator.addListener(new Animator.AnimatorListener() {
//            @Override
//            public void onAnimationStart(Animator animation) {
//
//            }
//
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                if (sourceImage != null) {
//                    sourceImage.setVisibility(View.VISIBLE);
//                }
//            }
//
//            @Override
//            public void onAnimationCancel(Animator animation) {
//
//            }
//
//            @Override
//            public void onAnimationRepeat(Animator animation) {
//
//            }
//        });
//
//        return animator;
//    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.source_info_fragment, container, false);

        settingsContainer = (RelativeLayout) view.findViewById(R.id.source_settings_container);

        sourceTitle = (EditText) view.findViewById(R.id.source_title);
        sourcePrefix = (EditText) view.findViewById(R.id.source_data_prefix);
        sourceData = (EditText) view.findViewById(R.id.source_data);
        sourceSuffix = (EditText) view.findViewById(R.id.source_data_suffix);
        sourceNum = (EditText) view.findViewById(R.id.source_num);

        cancelButton = (Button) view.findViewById(R.id.cancel_button);
        saveButton = (Button) view.findViewById(R.id.save_button);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (FileHandler.isDownloading) {
                    Toast.makeText(appContext,
                            "Cannot edit while downloading",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Bundle saveArguments = getArguments();

                final Intent setIntent = new Intent(SourceListFragment.SET_ENTRY);
                setIntent.putExtra("type", (String) saveArguments.get("type"));
                setIntent.putExtra("title", sourceTitle.getText().toString());

                String data = sourceData.getText().toString();

                switch ((String) saveArguments.get("type")) {

                    case AppSettings.FOLDER:
                        if (!data.contains("http")) {
                            data = "http://" + data;
                        }
                        break;
                    case AppSettings.IMGUR:
                        data = sourcePrefix.getText().toString() + data;
                        break;
                    case AppSettings.TUMBLR_TAG:
                        data = "Tumblr Tag: " + data;
                        break;

                }

                setIntent.putExtra("data", data);
                setIntent.putExtra("num", Integer.parseInt(sourceNum.getText().toString()));
                setIntent.putExtra("position", (Integer) saveArguments.get("position"));
                setIntent.putExtra("use", sourceUse.isChecked());

                try {
                    InputMethodManager im = (InputMethodManager) appContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    im.hideSoftInputFromWindow(getView().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }


                final int screenHeight = getResources().getDisplayMetrics().heightPixels;
                final View fragmentView = getView();

                if (fragmentView != null) {
                    final float viewStartY = getView().getY();

                    Animation animation = new Animation() {
                        @Override
                        protected void applyTransformation(float interpolatedTime,
                                Transformation t) {
                            fragmentView.setY((screenHeight - viewStartY) * interpolatedTime + viewStartY);
                        }

                        @Override
                        public boolean willChangeBounds() {
                            return true;
                        }
                    };

                    animation.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            LocalBroadcastManager.getInstance(appContext).sendBroadcast(setIntent);
                            getFragmentManager().popBackStack();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });

                    animation.setDuration(SLIDE_EXIT_TIME);
                    getView().startAnimation(animation);
                }
                else {
                    LocalBroadcastManager.getInstance(appContext).sendBroadcast(setIntent);
                }
            }
        });

        Bundle arguments = getArguments();
        String data = arguments.getString("data");

        String hint = "";
        String prefix = "";
        String suffix = "";

        switch (arguments.getString("type")) {

            case AppSettings.IMGUR:
                if (data.contains("imgur.com/a/")) {
                    hint = "Album ID";
                    prefix = "imgur.com/a/";
                }
                else if (data.contains("imgur.com/r/")) {
                    hint = "Subreddit";
                    prefix = "imgur.com/r/";
                }
                data = data.substring(data.indexOf(prefix) + prefix.length());
                break;
            case AppSettings.TUMBLR_BLOG:
                prefix = "Blog name";
                suffix = ".tumblr.com";
                break;
            case AppSettings.TUMBLR_TAG:
                hint = "Tag";
                if (data.length() > 12) {
                    data = data.substring(12);
                }
                break;
            case AppSettings.FOLDER:
                sourceData.setClickable(false);
                sourceData.setFocusable(false);
                sourceNum.setClickable(false);
                sourceNum.setFocusable(false);

        }

        sourceTitle.setText(arguments.getString("title"));
        sourceNum.setText(arguments.getString("num"));
        sourcePrefix.setText(prefix);
        sourceData.setText(data);
        sourceData.setHint(hint);
        sourceSuffix.setText(suffix);

        if (prefix.equals("")) {
            sourcePrefix.setVisibility(View.GONE);
        }
        if (suffix.equals("")) {
            sourceSuffix.setVisibility(View.GONE);
        }

        sourceImage = (ImageView) view.findViewById(R.id.source_image);
        if (imageDrawable != null) {
            sourceImage.setImageDrawable(imageDrawable);
        }

        sourceUse = (Switch) view.findViewById(R.id.source_use_switch);
        sourceUse.setChecked(arguments.getBoolean("use"));

        if (AppSettings.getTheme().equals(AppSettings.APP_LIGHT_THEME)) {
            view.setBackgroundColor(getResources().getColor(R.color.LIGHT_THEME_BACKGROUND));
        }
        else {
            view.setBackgroundColor(getResources().getColor(R.color.DARK_THEME_BACKGROUND));
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                settingsContainer.setAlpha(interpolatedTime);
                sourceUse.setAlpha(interpolatedTime);

            }
        };

        animation.setDuration(FADE_IN_TIME);
        animation.setInterpolator(new DecelerateInterpolator(3.0f));
        settingsContainer.startAnimation(animation);

    }

    public void setImageDrawable(Drawable drawable) {
        imageDrawable = drawable;
        if (sourceImage != null) {
            sourceImage.setImageDrawable(drawable);
            sourceImage.setVisibility(View.VISIBLE);
        }
    }

    public void onBackPressed() {

        final int screenHeight = getResources().getDisplayMetrics().heightPixels;
        final View fragmentView = getView();

        if (fragmentView != null) {
            final float viewStartY = getView().getY();

            Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    fragmentView.setY((screenHeight - viewStartY) * interpolatedTime + viewStartY);
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };

            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    getFragmentManager().popBackStack();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            animation.setDuration(SLIDE_EXIT_TIME);
            getView().startAnimation(animation);
        }
        else {
            getFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}