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

package cw.kop.autobackground.tutorial;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/23/2014.
 */
public class FinishFragment extends Fragment {

    private static final String TAG = FinishFragment.class.getCanonicalName();
    private Context appContext;

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

    @Override
    public View onCreateView(LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tutorial_finish_fragment, container, false);

        TextView titleText = (TextView) view.findViewById(R.id.title_text);
        titleText.setText("That's it.");

        TextView tutorialText = (TextView) view.findViewById(R.id.tutorial_text);
        tutorialText.setText("Now you're ready to use AutoBackground. I'd suggest adding a new source first " +
                "and then hitting download." +
                "\n" +
                "\n" +
                "If you have any questions, concern, suggestions, or whatever else, feel free to " +
                "email me at ");
        SpannableString emailString = new SpannableString("chiuwinson@gmail.com");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Log.i(TAG, "Clicked");
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "chiuwinson@gmail.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "AutoBackground Feedback");
                startActivity(Intent.createChooser(emailIntent, "Send email"));
            }
        };
        emailString.setSpan(clickableSpan, 0, emailString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tutorialText.append(emailString);
        tutorialText.append(".");
        tutorialText.setMovementMethod(LinkMovementMethod.getInstance());

        return view;
    }
}
