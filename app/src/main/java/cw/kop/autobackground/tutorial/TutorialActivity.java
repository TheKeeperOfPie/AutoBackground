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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

import cw.kop.autobackground.R;
import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 10/30/2014.
 */
public class TutorialActivity extends FragmentActivity {

    private ViewPager viewPager;
    private TutorialPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.tutorial_activity_layout);

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        pagerAdapter = new TutorialPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        Button closeButton = (Button) findViewById(R.id.close_button);
        closeButton.setText("Close");
        closeButton.setTextColor(AppSettings.getColorFilterInt(getApplicationContext()));
        closeButton.setVisibility(View.VISIBLE);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                AppSettings.setTutorial(false, "source");
            }
        });

        Button nextButton = (Button) findViewById(R.id.next_button);
        nextButton.setText("Next");
        nextButton.setTextColor(AppSettings.getColorFilterInt(getApplicationContext()));
        nextButton.setVisibility(View.VISIBLE);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewPager.getCurrentItem() < viewPager.getAdapter().getCount()) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
                }
                else {
                    finish();
                    AppSettings.setTutorial(false, "source");
                }
            }
        });
    }
}