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

import java.io.File;

/**
 * Created by TheKeeperOfPie on 9/22/2014.
 */
public class HistoryItem implements Comparable<HistoryItem>{

    private long time;
    private String url;
    private File image;

    public HistoryItem(long time, String url, File image) {
        this.time = time;
        this.url = url;
        this.image = image;
    }

    public long getTime() {
        return time;
    }

    public String getUrl() {
        return url;
    }

    public File getImage() {
        return image;
    }

    @Override
    public int compareTo(HistoryItem another) {

        long time = another.getTime() - this.getTime();

        return time == 0 ? 0 : time > 0 ? 1 : -1;
    }
}
