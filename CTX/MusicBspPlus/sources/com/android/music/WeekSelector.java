package com.android.music;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;

public class WeekSelector extends DialogFragment {
    private Dialog dialog;
    private final DialogInterface.OnClickListener mButtonClicked = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -1) {
                MusicUtils.setIntPref(WeekSelector.this.getActivity().getApplicationContext(), "numweeks", WeekSelector.this.mCurrentSelectedPos);
            }
        }
    };
    NumberPicker.OnValueChangeListener mChangeListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker numberPicker, int i, int i2) {
            if (numberPicker == WeekSelector.this.mNumberPicker) {
                WeekSelector.this.mCurrentSelectedPos = i2;
            }
        }
    };
    private int mCurrentSelectedPos;
    private NumberPicker mNumberPicker;

    public static WeekSelector newInstance(Boolean bool) {
        WeekSelector weekSelector = new WeekSelector();
        weekSelector.setArguments(new Bundle());
        return weekSelector;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        MusicLogUtils.v("WeekSelector", "onCreateView>>");
        this.dialog = new Dialog(getActivity());
        this.dialog.setContentView(R.layout.weekpicker);
        this.dialog.setTitle(R.string.weekpicker_title);
        Button button = (Button) this.dialog.findViewById(R.id.weeks_cancel);
        button.setText(getResources().getString(R.string.cancel));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (WeekSelector.this.dialog != null) {
                    WeekSelector.this.dialog.dismiss();
                    WeekSelector.this.dialog = null;
                }
            }
        });
        Button button2 = (Button) this.dialog.findViewById(R.id.weeks_done);
        button2.setText(getResources().getString(R.string.weekpicker_set));
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MusicUtils.setIntPref(WeekSelector.this.getActivity().getApplicationContext(), "numweeks", WeekSelector.this.mCurrentSelectedPos);
                if (WeekSelector.this.dialog != null) {
                    WeekSelector.this.dialog.dismiss();
                    WeekSelector.this.dialog = null;
                }
            }
        });
        this.dialog.setCanceledOnTouchOutside(true);
        this.dialog.setCancelable(true);
        this.mNumberPicker = (NumberPicker) this.dialog.findViewById(R.id.weeks);
        this.mNumberPicker.setOnValueChangedListener(this.mChangeListener);
        this.mNumberPicker.setDisplayedValues(getResources().getStringArray(R.array.weeklist));
        int intPref = MusicUtils.getIntPref(getActivity().getApplicationContext(), "numweeks", 1);
        if (bundle != null) {
            intPref = bundle.getInt("numweeks", intPref);
        }
        this.mCurrentSelectedPos = intPref;
        this.mNumberPicker.setMinValue(1);
        this.mNumberPicker.setMaxValue(12);
        this.mNumberPicker.setValue(intPref);
        this.mNumberPicker.setWrapSelectorWheel(false);
        this.mNumberPicker.setOnLongPressUpdateInterval(200L);
        EditText editText = (EditText) this.mNumberPicker.getChildAt(0);
        if (editText != null) {
            editText.setFocusable(false);
        }
        this.dialog.show();
        return this.dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putInt("numweeks", this.mCurrentSelectedPos);
    }
}
