package com.android.calendar.event;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.calendar.R;
import com.android.colorpicker.ColorPickerDialog;

public class EventColorPickerDialog extends ColorPickerDialog {
    private int mCalendarColor;

    public static EventColorPickerDialog newInstance(int[] colors, int selectedColor, int calendarColor, boolean isTablet) {
        EventColorPickerDialog ret = new EventColorPickerDialog();
        ret.initialize(R.string.event_color_picker_dialog_title, colors, selectedColor, 4, isTablet ? 1 : 2);
        ret.setCalendarColor(calendarColor);
        return ret;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.mCalendarColor = savedInstanceState.getInt("calendar_color");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("calendar_color", this.mCalendarColor);
    }

    public void setCalendarColor(int color) {
        this.mCalendarColor = color;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        this.mAlertDialog.setButton(-3, getActivity().getString(R.string.event_color_set_to_default), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog2, int which) {
                EventColorPickerDialog.this.onColorSelected(EventColorPickerDialog.this.mCalendarColor);
            }
        });
        return dialog;
    }
}
