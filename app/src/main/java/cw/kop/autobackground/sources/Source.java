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

package cw.kop.autobackground.sources;

import android.text.format.Time;

import java.io.File;
import java.io.Serializable;

/**
 * Created by TheKeeperOfPie on 12/9/2014.
 */
public class Source implements Serializable{

    private String type;
    private String title;
    private String data;
    private int num;
    private boolean preview;
    private boolean use;
    private boolean useTime;
    private String time;
    private int numStored;
    private File imageFile;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public boolean preview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public boolean use() {
        return use;
    }

    public void setUse(boolean use) {
        this.use = use;
    }

    public boolean usetime() {
        return useTime;
    }

    public void setUseTime(boolean useTime) {
        this.useTime = useTime;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getNumStored() {
        return numStored;
    }

    public void setNumStored(int numStored) {
        this.numStored = numStored;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }
}