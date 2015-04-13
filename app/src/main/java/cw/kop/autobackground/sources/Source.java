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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 12/16/2014.
 */
public class Source {

    public static final String POSITION = "position";
    public static final String TYPE = "type";
    public static final String TITLE = "title";
    public static final String DATA = "data";
    public static final String NUM = "num";
    public static final String USE = "use";
    public static final String PREVIEW = "preview";
    public static final String USE_TIME = "useTime";
    public static final String TIME = "time";
    public static final String IMAGE_FILE = "imageFile";
    public static final String NUM_STORED = "numStored";
    public static final String SORT = "sort";

    private String type;
    private String title;
    private String data;
    private int num;
    private int numStored;
    private boolean use;
    private boolean preview;
    private boolean useTime;
    private String time;
    private File imageFile;
    private String sort;
    private boolean expanded;

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

    public boolean isUse() {
        return use;
    }

    public void setUse(boolean use) {
        this.use = use;
    }

    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
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

    public boolean isUseTime() {
        return useTime;
    }

    public void setUseTime(boolean useTime) {
        this.useTime = useTime;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(TYPE, type);
        object.put(TITLE, title);
        object.put(DATA, data);
        object.put(NUM, num);
        object.put(NUM_STORED, numStored);
        object.put(USE, use);
        object.put(PREVIEW, preview);
        object.put(USE_TIME, useTime);
        object.put(TIME, time);
        object.put(IMAGE_FILE, imageFile != null ? imageFile.getAbsolutePath() : null);
        object.put(SORT, sort);
        return object;
    }

    public static Source fromJson(JSONObject object) throws JSONException {
        Source source = new Source();
        source.setType(object.has(TYPE) ? object.getString(TYPE) : AppSettings.WEBSITE);
        source.setTitle(
                object.has(TITLE) ? object.getString(TITLE) : "" + System.currentTimeMillis());
        source.setData(object.has(DATA) ? object.getString(DATA) : "Error loading data");
        source.setNum(object.has(NUM) ? object.getInt(NUM) : 1);
        source.setNumStored(object.has(NUM_STORED) ? object.getInt(NUM_STORED) : 0);
        source.setUse(!object.has(USE) || object.getBoolean(USE));
        source.setPreview(!object.has(PREVIEW) || object.getBoolean(PREVIEW));
        source.setUseTime(!object.has(USE_TIME) || object.getBoolean(USE_TIME));
        source.setTime(object.has(TIME) ? object.getString(TIME) : "00:00 - 00:00");
        source.setImageFile(object.has(IMAGE_FILE) ? new File(object.getString(IMAGE_FILE)) : null);
        source.setSort(object.has(SORT) ? object.getString(SORT) : "");
        return source;
    }

    @Override
    public String toString() {
        try {
            return toJson().toString();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return super.toString();
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isExpanded() {
        return expanded;
    }
}
