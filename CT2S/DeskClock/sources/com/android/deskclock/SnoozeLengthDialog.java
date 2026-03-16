package com.android.deskclock;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

public class SnoozeLengthDialog extends DialogPreference {
    private final Context mContext;
    private TextView mNumberPickerMinutesView;
    private NumberPicker mNumberPickerView;
    private int mSnoozeMinutes;

    public SnoozeLengthDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        setDialogLayoutResource(R.layout.snooze_length_picker);
        setTitle(R.string.snooze_duration_title);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setTitle(getContext().getString(R.string.snooze_duration_title)).setCancelable(true);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        this.mNumberPickerMinutesView = (TextView) view.findViewById(R.id.title);
        this.mNumberPickerView = (NumberPicker) view.findViewById(R.id.minutes_picker);
        this.mNumberPickerView.setMinValue(1);
        this.mNumberPickerView.setMaxValue(30);
        this.mNumberPickerView.setValue(this.mSnoozeMinutes);
        updateDays();
        this.mNumberPickerView.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                SnoozeLengthDialog.this.updateDays();
            }
        });
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            String val = getPersistedString("10");
            if (val != null) {
                this.mSnoozeMinutes = Integer.parseInt(val);
                return;
            }
            return;
        }
        String val2 = (String) defaultValue;
        if (val2 != null) {
            this.mSnoozeMinutes = Integer.parseInt(val2);
        }
        persistString(val2);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    private void updateDays() {
        this.mNumberPickerMinutesView.setText(String.format(this.mContext.getResources().getQuantityText(R.plurals.snooze_picker_label, this.mNumberPickerView.getValue()).toString(), new Object[0]));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            this.mNumberPickerView.clearFocus();
            this.mSnoozeMinutes = this.mNumberPickerView.getValue();
            persistString(Integer.toString(this.mSnoozeMinutes));
            setSummary();
        }
    }

    public void setSummary() {
        setSummary(String.format(this.mContext.getResources().getQuantityText(R.plurals.snooze_duration, this.mSnoozeMinutes).toString(), Integer.valueOf(this.mSnoozeMinutes)));
    }
}
