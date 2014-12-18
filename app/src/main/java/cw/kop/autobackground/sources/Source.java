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

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("type", type);
        object.put("title", title);
        object.put("data", data);
        object.put("num", num);
        object.put("numStored", numStored);
        object.put("use", use);
        object.put("preview", preview);
        object.put("useTime", useTime);
        object.put("time", time);
        object.put("imageFile", imageFile.getAbsolutePath());
        return object;
    }

    public static Source fromJson(JSONObject object) throws JSONException {
        Source source = new Source();
        source.setType(object.has("type") ? object.getString("type") : AppSettings.WEBSITE);
        source.setTitle(object.has("title") ? object.getString("title") : "" + System.currentTimeMillis());
        source.setData(object.has("data") ? object.getString("data") : "Error loading data");
        source.setNum(object.has("num") ? object.getInt("num") : 1);
        source.setNumStored(object.has("numStored") ? object.getInt("numStored") : 0);
        source.setUse(!object.has("use") || object.getBoolean("use"));
        source.setPreview(!object.has("preview") || object.getBoolean("preview"));
        source.setUseTime(!object.has("useTime") || object.getBoolean("useTime"));
        source.setTime(object.has("time") ? object.getString("time") : "00:00 - 00:00");
        source.setImageFile(object.has("imageFile") ? new File(object.getString("imageFile")) : null);
        return source;
    }


}
