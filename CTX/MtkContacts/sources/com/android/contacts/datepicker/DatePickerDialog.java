package com.android.contacts.datepicker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;
import com.android.contacts.datepicker.DatePicker;
import com.android.contacts.util.DateUtils;
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

    public DatePickerDialog(Context context, OnDateSetListener onDateSetListener, int i, int i2, int i3, boolean z) {
        this(context, 0, onDateSetListener, i, i2, i3, z);
    }

    public DatePickerDialog(Context context, int i, OnDateSetListener onDateSetListener, int i2, int i3, int i4, boolean z) {
        super(context, i);
        this.mCallBack = onDateSetListener;
        this.mInitialYear = i2;
        this.mInitialMonth = i3;
        this.mInitialDay = i4;
        this.mTitleDateFormat = DateFormat.getDateInstance(0);
        this.mTitleNoYearDateFormat = DateUtils.getLocalizedDateFormatWithoutYear(getContext());
        updateTitle(this.mInitialYear, this.mInitialMonth, this.mInitialDay);
        setButton(-1, context.getText(R.string.date_time_set), this);
        setButton(-2, context.getText(android.R.string.cancel), (DialogInterface.OnClickListener) null);
        View viewInflate = ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.date_picker_dialog, (ViewGroup) null);
        setView(viewInflate);
        this.mDatePicker = (DatePicker) viewInflate.findViewById(R.id.datePicker);
        this.mDatePicker.init(this.mInitialYear, this.mInitialMonth, this.mInitialDay, z, this);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (this.mCallBack != null) {
            this.mDatePicker.clearFocus();
            this.mCallBack.onDateSet(this.mDatePicker, this.mDatePicker.getYear(), this.mDatePicker.getMonth(), this.mDatePicker.getDayOfMonth());
        }
    }

    @Override
    public void onDateChanged(DatePicker datePicker, int i, int i2, int i3) {
        updateTitle(i, i2, i3);
    }

    private void updateTitle(int i, int i2, int i3) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1, i);
        calendar.set(2, i2);
        calendar.set(5, i3);
        setTitle((i == NO_YEAR ? this.mTitleNoYearDateFormat : this.mTitleDateFormat).format(calendar.getTime()));
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle bundleOnSaveInstanceState = super.onSaveInstanceState();
        bundleOnSaveInstanceState.putInt("year", this.mDatePicker.getYear());
        bundleOnSaveInstanceState.putInt("month", this.mDatePicker.getMonth());
        bundleOnSaveInstanceState.putInt("day", this.mDatePicker.getDayOfMonth());
        bundleOnSaveInstanceState.putBoolean("year_optional", this.mDatePicker.isYearOptional());
        return bundleOnSaveInstanceState;
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        int i = bundle.getInt("year");
        int i2 = bundle.getInt("month");
        int i3 = bundle.getInt("day");
        this.mDatePicker.init(i, i2, i3, bundle.getBoolean("year_optional"), this);
        updateTitle(i, i2, i3);
    }
}
