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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by TheKeeperOfPie on 11/23/2014.
 */
public class TutorialPagerAdapter extends FragmentStatePagerAdapter {

    private static final int NUM_ITEMS = 8;

    public TutorialPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new IntroFragment();
            case 1:
                return new CardFragment();
            case 2:
                return new ButtonFragment();
            case 3:
                return new ActionBarFragment();
            case 4:
                return new ImageFragment();
            case 5:
                return new ThemeFragment();
            case 6:
                return new FabricFragment();
            case 7:
                return new FinishFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }


}
