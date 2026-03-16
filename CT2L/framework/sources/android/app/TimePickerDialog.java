package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TimePicker;
import com.android.internal.R;

public class TimePickerDialog extends AlertDialog implements DialogInterface.OnClickListener, TimePicker.OnTimeChangedListener {
    private static final String HOUR = "hour";
    private static final String IS_24_HOUR = "is24hour";
    private static final String MINUTE = "minute";
    private final int mInitialHourOfDay;
    private final int mInitialMinute;
    private final boolean mIs24HourView;
    private final TimePicker mTimePicker;
    private final OnTimeSetListener mTimeSetCallback;
    private final TimePicker.ValidationCallback mValidationCallback;

    public interface OnTimeSetListener {
        void onTimeSet(TimePicker timePicker, int i, int i2);
    }

    public TimePickerDialog(Context context, OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView) {
        this(context, 0, callBack, hourOfDay, minute, is24HourView);
    }

    static int resolveDialogTheme(Context context, int resid) {
        if (resid == 0) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(16843934, outValue, true);
            return outValue.resourceId;
        }
        return resid;
    }

    public TimePickerDialog(Context context, int theme, OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView) {
        super(context, resolveDialogTheme(context, theme));
        this.mValidationCallback = new TimePicker.ValidationCallback() {
            @Override
            public void onValidationChanged(boolean valid) {
                Button positive = TimePickerDialog.this.getButton(-1);
                if (positive != null) {
                    positive.setEnabled(valid);
                }
            }
        };
        this.mTimeSetCallback = callBack;
        this.mInitialHourOfDay = hourOfDay;
        this.mInitialMinute = minute;
        this.mIs24HourView = is24HourView;
        Context themeContext = getContext();
        LayoutInflater inflater = LayoutInflater.from(themeContext);
        View view = inflater.inflate(R.layout.time_picker_dialog, (ViewGroup) null);
        setView(view);
        setButton(-1, themeContext.getString(17039370), this);
        setButton(-2, themeContext.getString(17039360), this);
        setButtonPanelLayoutHint(1);
        this.mTimePicker = (TimePicker) view.findViewById(R.id.timePicker);
        this.mTimePicker.setIs24HourView(Boolean.valueOf(this.mIs24HourView));
        this.mTimePicker.setCurrentHour(Integer.valueOf(this.mInitialHourOfDay));
        this.mTimePicker.setCurrentMinute(Integer.valueOf(this.mInitialMinute));
        this.mTimePicker.setOnTimeChangedListener(this);
        this.mTimePicker.setValidationCallback(this.mValidationCallback);
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                cancel();
                break;
            case -1:
                if (this.mTimeSetCallback != null) {
                    this.mTimeSetCallback.onTimeSet(this.mTimePicker, this.mTimePicker.getCurrentHour().intValue(), this.mTimePicker.getCurrentMinute().intValue());
                }
                break;
        }
    }

    public void updateTime(int hourOfDay, int minuteOfHour) {
        this.mTimePicker.setCurrentHour(Integer.valueOf(hourOfDay));
        this.mTimePicker.setCurrentMinute(Integer.valueOf(minuteOfHour));
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt(HOUR, this.mTimePicker.getCurrentHour().intValue());
        state.putInt(MINUTE, this.mTimePicker.getCurrentMinute().intValue());
        state.putBoolean(IS_24_HOUR, this.mTimePicker.is24HourView());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int hour = savedInstanceState.getInt(HOUR);
        int minute = savedInstanceState.getInt(MINUTE);
        this.mTimePicker.setIs24HourView(Boolean.valueOf(savedInstanceState.getBoolean(IS_24_HOUR)));
        this.mTimePicker.setCurrentHour(Integer.valueOf(hour));
        this.mTimePicker.setCurrentMinute(Integer.valueOf(minute));
    }
}
