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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

import cw.kop.autobackground.settings.AppSettings;

/**
 * Created by TheKeeperOfPie on 11/1/2014.
 */
public class DialogFactory {

    /**
     * Provides Dialog object, mainly used to allow additional themes to be added
     *
     * @param context Context to create Dialogs with
     * @return Dialog with correct theme
     */
    public static Dialog getDialog(Context context) {

        return new Dialog(context, AppSettings.getDialogThemeResource());
    }

    /**
     * Show a Dialog constructed from R.layout.action_dialog, allows resource IDs to set as button text.
     * Pass in -1 for text parameter to disable button.
     *
     * @param context
     * @param title
     * @param summary
     * @param clickListener
     * @param textOneResource
     * @param textTwoResource
     * @param textThreeResource
     */
    public static void showActionDialog(Context context,
            String title,
            String summary,
            final ActionDialogListener clickListener,
            int textOneResource,
            int textTwoResource,
            int textThreeResource) {

        Dialog dialog = getDialog(context);
        clickListener.setDialog(dialog);

        View dialogView = View.inflate(context, R.layout.action_dialog, null);
        dialog.setContentView(dialogView);

        if (title.length() > 0) {
            TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
            dialogTitle.setVisibility(View.VISIBLE);
            dialogTitle.setText(title);

            View titleUnderline = dialogView.findViewById(R.id.dialog_underline);
            titleUnderline.setVisibility(View.VISIBLE);
        }

        if (summary.length() > 0) {
            TextView dialogSummary = (TextView) dialogView.findViewById(R.id.dialog_summary);
            dialogSummary.setText(summary);
            dialogSummary.setVisibility(View.VISIBLE);
        }

        int textColorInt = context.getResources().getColor(R.color.LIGHT_BLUE_OPAQUE);

        if (textOneResource > 0) {
            Button buttonOne = (Button) dialogView.findViewById(R.id.action_button_1);
            buttonOne.setText(context.getResources().getString(textOneResource));
            buttonOne.setTextColor(textColorInt);
            buttonOne.setVisibility(View.VISIBLE);
            buttonOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onClickLeft(v);
                }
            });
        }

        if (textTwoResource > 0) {
            Button buttonTwo = (Button) dialogView.findViewById(R.id.action_button_2);
            buttonTwo.setText(context.getResources().getString(textTwoResource));
            buttonTwo.setTextColor(textColorInt);
            buttonTwo.setVisibility(View.VISIBLE);
            buttonTwo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onClickMiddle(v);
                }
            });
        }

        if (textThreeResource > 0) {
            Button buttonThree = (Button) dialogView.findViewById(R.id.action_button_3);
            buttonThree.setText(context.getResources().getString(textThreeResource));
            buttonThree.setTextColor(textColorInt);
            buttonThree.setVisibility(View.VISIBLE);
            buttonThree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onClickRight(v);
                }
            });
        }

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                clickListener.onDismiss();
            }
        });

        dialog.show();
    }

    public static void showListDialog(Context context, String title,
            final ListDialogListener clickListener,
            int listArrayResource) {

        Dialog dialog = getDialog(context);
        clickListener.setDialog(dialog);

        View dialogView = View.inflate(context, R.layout.list_dialog, null);
        dialog.setContentView(dialogView);

        if (title.length() > 0) {
            TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
            dialogTitle.setVisibility(View.VISIBLE);
            dialogTitle.setText(title);

            View dialogUnderline = dialogView.findViewById(R.id.dialog_underline);
            dialogUnderline.setVisibility(View.VISIBLE);
        }

        ListView dialogList = (ListView) dialogView.findViewById(R.id.dialog_list);
        dialogList.setAdapter(new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                context.getResources().getStringArray(
                        listArrayResource)));
        dialogList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                clickListener.onItemClick(parent, view, position, id);
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                clickListener.onDismiss();
            }
        });

        dialog.show();
    }

    public static void showInputDialog(Context context,
            String title,
            String hint,
            String defaultText,
            final InputDialogListener listener,
            int textOneResource,
            int textTwoResource,
            int textThreeResource,
            int inputType) {


        Dialog dialog = getDialog(context);
        listener.setDialog(dialog);

        View dialogView = View.inflate(context, R.layout.input_dialog, null);
        dialog.setContentView(dialogView);


        if (title.length() > 0) {
            TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
            dialogTitle.setVisibility(View.VISIBLE);
            dialogTitle.setText(title);

            View titleUnderline = dialogView.findViewById(R.id.dialog_underline);
            titleUnderline.setVisibility(View.VISIBLE);
        }

        EditText inputField = (EditText) dialogView.findViewById(R.id.input_field);
        inputField.setInputType(inputType);
        if (hint.length() > 0) {
            inputField.setHint(hint);
        }
        if (defaultText.length() > 0) {
            inputField.setText(defaultText);
        }
        listener.setEditText(inputField);

        int textColorInt = context.getResources().getColor(R.color.LIGHT_BLUE_OPAQUE);

        if (textOneResource > 0) {
            Button buttonOne = (Button) dialogView.findViewById(R.id.action_button_1);
            buttonOne.setText(context.getResources().getString(textOneResource));
            buttonOne.setTextColor(textColorInt);
            buttonOne.setVisibility(View.VISIBLE);
            buttonOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClickLeft(v);
                }
            });
        }

        if (textTwoResource > 0) {
            Button buttonTwo = (Button) dialogView.findViewById(R.id.action_button_2);
            buttonTwo.setText(context.getResources().getString(textTwoResource));
            buttonTwo.setTextColor(textColorInt);
            buttonTwo.setVisibility(View.VISIBLE);
            buttonTwo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClickMiddle(v);
                }
            });
        }

        if (textThreeResource > 0) {
            Button buttonThree = (Button) dialogView.findViewById(R.id.action_button_3);
            buttonThree.setText(context.getResources().getString(textThreeResource));
            buttonThree.setTextColor(textColorInt);
            buttonThree.setVisibility(View.VISIBLE);
            buttonThree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClickRight(v);
                }
            });
        }

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                listener.onDismiss();
            }
        });

        dialog.show();

    }

    public static void showTimeDialog(Context context,
            String title,
            String summary,
            final TimeDialogListener listener,
            int textOneResource,
            int textTwoResource,
            int textThreeResource) {

        Dialog dialog = getDialog(context);
        listener.setDialog(dialog);

        View dialogView = View.inflate(context, R.layout.time_dialog, null);
        dialog.setContentView(dialogView);

        TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.dialog_time_picker);
        listener.setTimePicker(timePicker);

        if (title.length() > 0) {
            TextView dialogTitle = (TextView) dialogView.findViewById(R.id.dialog_title);
            dialogTitle.setVisibility(View.VISIBLE);
            dialogTitle.setText(title);

            View titleUnderline = dialogView.findViewById(R.id.dialog_underline);
            titleUnderline.setVisibility(View.VISIBLE);
        }

        if (summary.length() > 0) {
            TextView dialogSummary = (TextView) dialogView.findViewById(R.id.dialog_summary);
            dialogSummary.setText(summary);
            dialogSummary.setVisibility(View.VISIBLE);
        }

        int textColorInt = context.getResources().getColor(R.color.LIGHT_BLUE_OPAQUE);

        if (textOneResource > 0) {
            Button buttonOne = (Button) dialogView.findViewById(R.id.action_button_1);
            buttonOne.setText(context.getResources().getString(textOneResource));
            buttonOne.setTextColor(textColorInt);
            buttonOne.setVisibility(View.VISIBLE);
            buttonOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClickLeft(v);
                }
            });
        }

        if (textTwoResource > 0) {
            Button buttonTwo = (Button) dialogView.findViewById(R.id.action_button_2);
            buttonTwo.setText(context.getResources().getString(textTwoResource));
            buttonTwo.setTextColor(textColorInt);
            buttonTwo.setVisibility(View.VISIBLE);
            buttonTwo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClickMiddle(v);
                }
            });
        }

        if (textThreeResource > 0) {
            Button buttonThree = (Button) dialogView.findViewById(R.id.action_button_3);
            buttonThree.setText(context.getResources().getString(textThreeResource));
            buttonThree.setTextColor(textColorInt);
            buttonThree.setVisibility(View.VISIBLE);
            buttonThree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClickRight(v);
                }
            });
        }

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                listener.onDismiss();
            }
        });

        dialog.show();


    }

    public abstract static class DialogClickListener {

        private Dialog dialog;

        public void setDialog(Dialog dialog) {
            this.dialog = dialog;
        }

        public void dismissDialog() {
            dialog.dismiss();
        }

        public void onDismiss() {
            this.dismissDialog();
        }

    }

    public abstract static class ActionDialogListener extends DialogClickListener {

        public void onClickLeft(View v) {
            this.dismissDialog();
        }

        public void onClickMiddle(View v) {
            this.dismissDialog();
        }

        public void onClickRight(View v) {
            this.dismissDialog();
        }

    }

    public abstract static class ListDialogListener extends DialogClickListener {

        private boolean itemSelected = false;

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            this.dismissDialog();
        }

        public void onDismiss() {
            this.dismissDialog();
        }

        public boolean getItemSelected() {
            return itemSelected;
        }

        public void setItemSelected(boolean selected) {
            itemSelected = selected;
        }

    }

    public abstract static class InputDialogListener extends ActionDialogListener {

        private EditText editText;

        public void setEditText(EditText editText) {
            this.editText = editText;
        }

        public String getEditTextString() {
            return editText.getText().toString();
        }

    }

    public abstract static class TimeDialogListener extends ActionDialogListener {

        private TimePicker timePicker;

        public TimePicker getTimePicker() {
            return timePicker;
        }

        public void setTimePicker(TimePicker timePicker) {
            this.timePicker = timePicker;
        }

    }

}
