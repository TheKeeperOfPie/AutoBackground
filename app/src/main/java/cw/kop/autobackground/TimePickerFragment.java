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

package cw.kop.autobackground;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Created by TheKeeperOfPie on 12/13/2014.
 */
public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    private Context appContext;
    private TimePickerListener timePickerListener;

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

    public void setTimePickerListener(TimePickerListener listener) {
        timePickerListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        // Create a new instance of TimePickerDialog and return it
        TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), TimePickerFragment.this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
        timePickerDialog.setTitle(getArguments().getString("title", ""));
        return timePickerDialog;
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Do something with the time chosen by the user

        if (timePickerListener != null) {
            timePickerListener.onTimeSet(view, hourOfDay, minute);
        }
    }

    public interface TimePickerListener {

        void onTimeSet(TimePicker view, int hourOfDay, int minute);

    }

}