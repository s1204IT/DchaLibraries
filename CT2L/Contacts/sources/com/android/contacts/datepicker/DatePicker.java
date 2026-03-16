package com.android.contacts.datepicker;

import android.animation.LayoutTransition;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import com.android.contacts.R;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;
import libcore.icu.ICU;

public class DatePicker extends FrameLayout {
    public static int NO_YEAR = 0;
    private int mDay;
    private final NumberPicker mDayPicker;
    private boolean mHasYear;
    private int mMonth;
    private final NumberPicker mMonthPicker;
    private OnDateChangedListener mOnDateChangedListener;
    private final LinearLayout mPickerContainer;
    private int mYear;
    private boolean mYearOptional;
    private final NumberPicker mYearPicker;
    private final CheckBox mYearToggle;

    public interface OnDateChangedListener {
        void onDateChanged(DatePicker datePicker, int i, int i2, int i3);
    }

    public DatePicker(Context context) {
        this(context, null);
    }

    public DatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DatePicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        inflater.inflate(R.layout.date_picker, (ViewGroup) this, true);
        this.mPickerContainer = (LinearLayout) findViewById(R.id.parent);
        this.mDayPicker = (NumberPicker) findViewById(R.id.day);
        this.mDayPicker.setFormatter(NumberPicker.getTwoDigitFormatter());
        this.mDayPicker.setOnLongPressUpdateInterval(100L);
        this.mDayPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                DatePicker.this.mDay = newVal;
                DatePicker.this.notifyDateChanged();
            }
        });
        this.mMonthPicker = (NumberPicker) findViewById(R.id.month);
        this.mMonthPicker.setFormatter(NumberPicker.getTwoDigitFormatter());
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getShortMonths();
        if (months[0].startsWith("1")) {
            for (int i = 0; i < months.length; i++) {
                months[i] = String.valueOf(i + 1);
            }
            this.mMonthPicker.setMinValue(1);
            this.mMonthPicker.setMaxValue(12);
        } else {
            this.mMonthPicker.setMinValue(1);
            this.mMonthPicker.setMaxValue(12);
            this.mMonthPicker.setDisplayedValues(months);
        }
        this.mMonthPicker.setOnLongPressUpdateInterval(200L);
        this.mMonthPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                DatePicker.this.mMonth = newVal - 1;
                DatePicker.this.adjustMaxDay();
                DatePicker.this.notifyDateChanged();
                DatePicker.this.updateDaySpinner();
            }
        });
        this.mYearPicker = (NumberPicker) findViewById(R.id.year);
        this.mYearPicker.setOnLongPressUpdateInterval(100L);
        this.mYearPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                DatePicker.this.mYear = newVal;
                DatePicker.this.adjustMaxDay();
                DatePicker.this.notifyDateChanged();
                DatePicker.this.updateDaySpinner();
            }
        });
        this.mYearPicker.setMinValue(1900);
        this.mYearPicker.setMaxValue(2100);
        this.mYearToggle = (CheckBox) findViewById(R.id.yearToggle);
        this.mYearToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DatePicker.this.mHasYear = isChecked;
                DatePicker.this.adjustMaxDay();
                DatePicker.this.notifyDateChanged();
                DatePicker.this.updateSpinners();
            }
        });
        Calendar cal = Calendar.getInstance();
        init(cal.get(1), cal.get(2), cal.get(5), null);
        reorderPickers();
        this.mPickerContainer.setLayoutTransition(new LayoutTransition());
        if (!isEnabled()) {
            setEnabled(false);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mDayPicker.setEnabled(enabled);
        this.mMonthPicker.setEnabled(enabled);
        this.mYearPicker.setEnabled(enabled);
    }

    private void reorderPickers() {
        String skeleton = this.mHasYear ? "yyyyMMMdd" : "MMMdd";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        char[] order = ICU.getDateFormatOrder(pattern);
        this.mPickerContainer.removeAllViews();
        for (char field : order) {
            if (field == 'd') {
                this.mPickerContainer.addView(this.mDayPicker);
            } else if (field == 'M') {
                this.mPickerContainer.addView(this.mMonthPicker);
            } else {
                this.mPickerContainer.addView(this.mYearPicker);
            }
        }
    }

    private int getCurrentYear() {
        return Calendar.getInstance().get(1);
    }

    private static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        private final int mDay;
        private final boolean mHasYear;
        private final int mMonth;
        private final int mYear;
        private final boolean mYearOptional;

        private SavedState(Parcelable superState, int year, int month, int day, boolean hasYear, boolean yearOptional) {
            super(superState);
            this.mYear = year;
            this.mMonth = month;
            this.mDay = day;
            this.mHasYear = hasYear;
            this.mYearOptional = yearOptional;
        }

        private SavedState(Parcel in) {
            super(in);
            this.mYear = in.readInt();
            this.mMonth = in.readInt();
            this.mDay = in.readInt();
            this.mHasYear = in.readInt() != 0;
            this.mYearOptional = in.readInt() != 0;
        }

        public int getYear() {
            return this.mYear;
        }

        public int getMonth() {
            return this.mMonth;
        }

        public int getDay() {
            return this.mDay;
        }

        public boolean hasYear() {
            return this.mHasYear;
        }

        public boolean isYearOptional() {
            return this.mYearOptional;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.mYear);
            dest.writeInt(this.mMonth);
            dest.writeInt(this.mDay);
            dest.writeInt(this.mHasYear ? 1 : 0);
            dest.writeInt(this.mYearOptional ? 1 : 0);
        }
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, this.mYear, this.mMonth, this.mDay, this.mHasYear, this.mYearOptional);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mYear = ss.getYear();
        this.mMonth = ss.getMonth();
        this.mDay = ss.getDay();
        this.mHasYear = ss.hasYear();
        this.mYearOptional = ss.isYearOptional();
        updateSpinners();
    }

    public void init(int year, int monthOfYear, int dayOfMonth, OnDateChangedListener onDateChangedListener) {
        init(year, monthOfYear, dayOfMonth, false, onDateChangedListener);
    }

    public void init(int year, int monthOfYear, int dayOfMonth, boolean yearOptional, OnDateChangedListener onDateChangedListener) {
        this.mYear = (yearOptional && year == NO_YEAR) ? getCurrentYear() : year;
        this.mMonth = monthOfYear;
        this.mDay = dayOfMonth;
        this.mYearOptional = yearOptional;
        boolean z = (yearOptional && year == NO_YEAR) ? false : true;
        this.mHasYear = z;
        this.mOnDateChangedListener = onDateChangedListener;
        updateSpinners();
    }

    private void updateSpinners() {
        updateDaySpinner();
        this.mYearToggle.setChecked(this.mHasYear);
        this.mYearToggle.setVisibility(this.mYearOptional ? 0 : 8);
        this.mYearPicker.setValue(this.mYear);
        this.mYearPicker.setVisibility(this.mHasYear ? 0 : 8);
        this.mMonthPicker.setValue(this.mMonth + 1);
    }

    private void updateDaySpinner() {
        Calendar cal = Calendar.getInstance();
        cal.set(this.mHasYear ? this.mYear : 2000, this.mMonth, 1);
        int max = cal.getActualMaximum(5);
        this.mDayPicker.setMinValue(1);
        this.mDayPicker.setMaxValue(max);
        this.mDayPicker.setValue(this.mDay);
    }

    public int getYear() {
        return (!this.mYearOptional || this.mHasYear) ? this.mYear : NO_YEAR;
    }

    public boolean isYearOptional() {
        return this.mYearOptional;
    }

    public int getMonth() {
        return this.mMonth;
    }

    public int getDayOfMonth() {
        return this.mDay;
    }

    private void adjustMaxDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(1, this.mHasYear ? this.mYear : 2000);
        cal.set(2, this.mMonth);
        int max = cal.getActualMaximum(5);
        if (this.mDay > max) {
            this.mDay = max;
        }
    }

    private void notifyDateChanged() {
        if (this.mOnDateChangedListener != null) {
            int year = (!this.mYearOptional || this.mHasYear) ? this.mYear : NO_YEAR;
            this.mOnDateChangedListener.onDateChanged(this, year, this.mMonth, this.mDay);
        }
    }
}
