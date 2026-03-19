package com.android.calendar.event;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.calendar.R;
import com.android.colorpicker.ColorPickerDialog;

public class EventColorPickerDialog extends ColorPickerDialog {
    private int mCalendarColor;

    public static EventColorPickerDialog newInstance(int[] iArr, int i, int i2, boolean z) {
        EventColorPickerDialog eventColorPickerDialog = new EventColorPickerDialog();
        eventColorPickerDialog.initialize(R.string.event_color_picker_dialog_title, iArr, i, 4, z ? 1 : 2);
        eventColorPickerDialog.setCalendarColor(i2);
        return eventColorPickerDialog;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            this.mCalendarColor = bundle.getInt("calendar_color");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("calendar_color", this.mCalendarColor);
    }

    public void setCalendarColor(int i) {
        this.mCalendarColor = i;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Dialog dialogOnCreateDialog = super.onCreateDialog(bundle);
        this.mAlertDialog.setButton(-3, getActivity().getString(R.string.event_color_set_to_default), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EventColorPickerDialog.this.onColorSelected(EventColorPickerDialog.this.mCalendarColor);
            }
        });
        return dialogOnCreateDialog;
    }
}
