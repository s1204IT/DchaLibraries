package com.android.contacts.datepicker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;
import com.android.contacts.common.util.DateUtils;
import com.android.contacts.datepicker.DatePicker;
import java.text.DateFormat;
import java.util.Calendar;

public class DatePickerDialog extends AlertDialog implements DialogInterface.OnClickListener, DatePicker.OnDateChangedListener {
    public static int NO_YEAR = DatePicker.NO_YEAR;
    private final OnDateSetListener mCallBack;
    private final DatePicker mDatePicker;
    private int mInitialDay;
    private int mInitialMonth;
    private int mInitialYear;
    private final DateFormat mTitleDateFormat;
    private final DateFormat mTitleNoYearDateFormat;

    public interface OnDateSetListener {
        void onDateSet(DatePicker datePicker, int i, int i2, int i3);
    }

    public DatePickerDialog(Context context, OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth, boolean yearOptional) {
        this(context, 5, callBack, year, monthOfYear, dayOfMonth, yearOptional);
    }

    public DatePickerDialog(Context context, int theme, OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth, boolean yearOptional) {
        super(context, theme);
        this.mCallBack = callBack;
        this.mInitialYear = year;
        this.mInitialMonth = monthOfYear;
        this.mInitialDay = dayOfMonth;
        this.mTitleDateFormat = DateFormat.getDateInstance(0);
        this.mTitleNoYearDateFormat = DateUtils.getLocalizedDateFormatWithoutYear(getContext());
        updateTitle(this.mInitialYear, this.mInitialMonth, this.mInitialDay);
        setButton(-1, context.getText(R.string.date_time_set), this);
        setButton(-2, context.getText(android.R.string.cancel), (DialogInterface.OnClickListener) null);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        View view = inflater.inflate(R.layout.date_picker_dialog, (ViewGroup) null);
        setView(view);
        this.mDatePicker = (DatePicker) view.findViewById(R.id.datePicker);
        this.mDatePicker.init(this.mInitialYear, this.mInitialMonth, this.mInitialDay, yearOptional, this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (this.mCallBack != null) {
            this.mDatePicker.clearFocus();
            this.mCallBack.onDateSet(this.mDatePicker, this.mDatePicker.getYear(), this.mDatePicker.getMonth(), this.mDatePicker.getDayOfMonth());
        }
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int month, int day) {
        updateTitle(year, month, day);
    }

    private void updateTitle(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1, year);
        calendar.set(2, month);
        calendar.set(5, day);
        DateFormat dateFormat = year == NO_YEAR ? this.mTitleNoYearDateFormat : this.mTitleDateFormat;
        setTitle(dateFormat.format(calendar.getTime()));
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt("year", this.mDatePicker.getYear());
        state.putInt("month", this.mDatePicker.getMonth());
        state.putInt("day", this.mDatePicker.getDayOfMonth());
        state.putBoolean("year_optional", this.mDatePicker.isYearOptional());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int year = savedInstanceState.getInt("year");
        int month = savedInstanceState.getInt("month");
        int day = savedInstanceState.getInt("day");
        boolean yearOptional = savedInstanceState.getBoolean("year_optional");
        this.mDatePicker.init(year, month, day, yearOptional, this);
        updateTitle(year, month, day);
    }
}
