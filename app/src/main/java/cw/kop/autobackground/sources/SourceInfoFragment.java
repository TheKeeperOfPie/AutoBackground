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

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.TimeInterpolator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/5/2014.
 */
public class SourceInfoFragment extends Fragment {

    private static final int FADE_IN_TIME = 500;

    private Context appContext;
    private Drawable imageDrawable;

    private ImageView sourceImage;
    private EditText sourceTitle;
    private EditText sourcePrefix;
    private EditText sourceData;
    private EditText sourceSuffix;
    private EditText sourceNum;
    private Switch sourceUse;

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

        sourceTitle = (EditText) view.findViewById(R.id.source_title);
        sourcePrefix = (EditText) view.findViewById(R.id.source_data_prefix);
        sourceData = (EditText) view.findViewById(R.id.source_data);
        sourceSuffix = (EditText) view.findViewById(R.id.source_data_suffix);
        sourceNum = (EditText) view.findViewById(R.id.source_num);

        Bundle arguments = getArguments();

        sourceTitle.setText(arguments.getString("title"));
        sourcePrefix.setText(arguments.getString("prefix"));
        sourceData.setText(arguments.getString("data"));
        sourceSuffix.setText(arguments.getString("suffix"));
        sourceNum.setText(arguments.getString("num"));

        sourceImage = (ImageView) view.findViewById(R.id.source_image);
        if (imageDrawable != null) {
            sourceImage.setImageDrawable(imageDrawable);
        }

        sourceUse = (Switch) view.findViewById(R.id.source_use_switch);
        sourceUse.setChecked(Boolean.valueOf(arguments.getString("use")));

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

                sourceUse.setAlpha(interpolatedTime);
                sourcePrefix.setAlpha(interpolatedTime);
                sourceData.setAlpha(interpolatedTime);
                sourceSuffix.setAlpha(interpolatedTime);
                sourceNum.setAlpha(interpolatedTime);

            }
        };

        animation.setDuration(FADE_IN_TIME);
        animation.setInterpolator(new DecelerateInterpolator(3.0f));
        sourceData.startAnimation(animation);

    }

    public void setImageDrawable(Drawable drawable) {
        imageDrawable = drawable;
        if (sourceImage != null) {
            sourceImage.setImageDrawable(drawable);
            sourceImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}