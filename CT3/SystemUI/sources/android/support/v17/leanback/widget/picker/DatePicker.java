package android.support.v17.leanback.widget.picker;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v17.leanback.R$styleable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class DatePicker extends Picker {
    private static int[] DATE_FIELDS = {5, 2, 1};
    int mColDayIndex;
    int mColMonthIndex;
    int mColYearIndex;
    PickerConstant mConstant;
    Calendar mCurrentDate;
    final DateFormat mDateFormat;
    private String mDatePickerFormat;
    PickerColumn mDayColumn;
    Calendar mMaxDate;
    Calendar mMinDate;
    PickerColumn mMonthColumn;
    Calendar mTempDate;
    PickerColumn mYearColumn;

    public DatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DatePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        updateCurrentLocale();
        setSeparator(this.mConstant.dateSeparator);
        TypedArray attributesArray = context.obtainStyledAttributes(attrs, R$styleable.lbDatePicker);
        String minDate = attributesArray.getString(R$styleable.lbDatePicker_android_minDate);
        String maxDate = attributesArray.getString(R$styleable.lbDatePicker_android_maxDate);
        this.mTempDate.clear();
        if (TextUtils.isEmpty(minDate) || !parseDate(minDate, this.mTempDate)) {
            this.mTempDate.set(1900, 0, 1);
        }
        this.mMinDate.setTimeInMillis(this.mTempDate.getTimeInMillis());
        this.mTempDate.clear();
        if (TextUtils.isEmpty(maxDate) || !parseDate(maxDate, this.mTempDate)) {
            this.mTempDate.set(2100, 0, 1);
        }
        this.mMaxDate.setTimeInMillis(this.mTempDate.getTimeInMillis());
        String datePickerFormat = attributesArray.getString(R$styleable.lbDatePicker_datePickerFormat);
        setDatePickerFormat(TextUtils.isEmpty(datePickerFormat) ? new String(android.text.format.DateFormat.getDateFormatOrder(context)) : datePickerFormat);
    }

    private boolean parseDate(String date, Calendar outDate) {
        try {
            outDate.setTime(this.mDateFormat.parse(date));
            return true;
        } catch (ParseException e) {
            Log.w("DatePicker", "Date: " + date + " not in format: MM/dd/yyyy");
            return false;
        }
    }

    public void setDatePickerFormat(String datePickerFormat) {
        if (TextUtils.isEmpty(datePickerFormat)) {
            datePickerFormat = new String(android.text.format.DateFormat.getDateFormatOrder(getContext()));
        }
        String datePickerFormat2 = datePickerFormat.toUpperCase();
        if (TextUtils.equals(this.mDatePickerFormat, datePickerFormat2)) {
            return;
        }
        this.mDatePickerFormat = datePickerFormat2;
        this.mDayColumn = null;
        this.mMonthColumn = null;
        this.mYearColumn = null;
        this.mColMonthIndex = -1;
        this.mColDayIndex = -1;
        this.mColYearIndex = -1;
        ArrayList<PickerColumn> columns = new ArrayList<>(3);
        for (int i = 0; i < datePickerFormat2.length(); i++) {
            switch (datePickerFormat2.charAt(i)) {
                case 'D':
                    if (this.mDayColumn != null) {
                        throw new IllegalArgumentException("datePicker format error");
                    }
                    PickerColumn pickerColumn = new PickerColumn();
                    this.mDayColumn = pickerColumn;
                    columns.add(pickerColumn);
                    this.mDayColumn.setLabelFormat("%02d");
                    this.mColDayIndex = i;
                    break;
                    break;
                case 'M':
                    if (this.mMonthColumn != null) {
                        throw new IllegalArgumentException("datePicker format error");
                    }
                    PickerColumn pickerColumn2 = new PickerColumn();
                    this.mMonthColumn = pickerColumn2;
                    columns.add(pickerColumn2);
                    this.mMonthColumn.setStaticLabels(this.mConstant.months);
                    this.mColMonthIndex = i;
                    break;
                    break;
                case 'Y':
                    if (this.mYearColumn != null) {
                        throw new IllegalArgumentException("datePicker format error");
                    }
                    PickerColumn pickerColumn3 = new PickerColumn();
                    this.mYearColumn = pickerColumn3;
                    columns.add(pickerColumn3);
                    this.mColYearIndex = i;
                    this.mYearColumn.setLabelFormat("%d");
                    break;
                    break;
                default:
                    throw new IllegalArgumentException("datePicker format error");
            }
        }
        setColumns(columns);
        updateSpinners(false);
    }

    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        }
        long currentTimeMillis = oldCalendar.getTimeInMillis();
        Calendar newCalendar = Calendar.getInstance(locale);
        newCalendar.setTimeInMillis(currentTimeMillis);
        return newCalendar;
    }

    private void updateCurrentLocale() {
        this.mConstant = new PickerConstant(Locale.getDefault(), getContext().getResources());
        this.mTempDate = getCalendarForLocale(this.mTempDate, this.mConstant.locale);
        this.mMinDate = getCalendarForLocale(this.mMinDate, this.mConstant.locale);
        this.mMaxDate = getCalendarForLocale(this.mMaxDate, this.mConstant.locale);
        this.mCurrentDate = getCalendarForLocale(this.mCurrentDate, this.mConstant.locale);
        if (this.mMonthColumn == null) {
            return;
        }
        this.mMonthColumn.setStaticLabels(this.mConstant.months);
        setColumnAt(this.mColMonthIndex, this.mMonthColumn);
    }

    @Override
    public final void onColumnValueChanged(int column, int newVal) {
        this.mTempDate.setTimeInMillis(this.mCurrentDate.getTimeInMillis());
        int oldVal = getColumnAt(column).getCurrentValue();
        if (column == this.mColDayIndex) {
            this.mTempDate.add(5, newVal - oldVal);
        } else if (column == this.mColMonthIndex) {
            this.mTempDate.add(2, newVal - oldVal);
        } else if (column == this.mColYearIndex) {
            this.mTempDate.add(1, newVal - oldVal);
        } else {
            throw new IllegalArgumentException();
        }
        setDate(this.mTempDate.get(1), this.mTempDate.get(2), this.mTempDate.get(5));
        updateSpinners(false);
    }

    private void setDate(int year, int month, int dayOfMonth) {
        this.mCurrentDate.set(year, month, dayOfMonth);
        if (this.mCurrentDate.before(this.mMinDate)) {
            this.mCurrentDate.setTimeInMillis(this.mMinDate.getTimeInMillis());
        } else {
            if (!this.mCurrentDate.after(this.mMaxDate)) {
                return;
            }
            this.mCurrentDate.setTimeInMillis(this.mMaxDate.getTimeInMillis());
        }
    }

    private static boolean updateMin(PickerColumn column, int value) {
        if (value != column.getMinValue()) {
            column.setMinValue(value);
            return true;
        }
        return false;
    }

    private static boolean updateMax(PickerColumn column, int value) {
        if (value != column.getMaxValue()) {
            column.setMaxValue(value);
            return true;
        }
        return false;
    }

    public void updateSpinnersImpl(boolean animation) {
        boolean dateFieldChanged;
        boolean dateFieldChanged2;
        int[] dateFieldIndices = {this.mColDayIndex, this.mColMonthIndex, this.mColYearIndex};
        boolean allLargerDateFieldsHaveBeenEqualToMinDate = true;
        boolean allLargerDateFieldsHaveBeenEqualToMaxDate = true;
        for (int i = DATE_FIELDS.length - 1; i >= 0; i--) {
            if (dateFieldIndices[i] >= 0) {
                int currField = DATE_FIELDS[i];
                PickerColumn currPickerColumn = getColumnAt(dateFieldIndices[i]);
                if (allLargerDateFieldsHaveBeenEqualToMinDate) {
                    dateFieldChanged = updateMin(currPickerColumn, this.mMinDate.get(currField));
                } else {
                    dateFieldChanged = updateMin(currPickerColumn, this.mCurrentDate.getActualMinimum(currField));
                }
                if (allLargerDateFieldsHaveBeenEqualToMaxDate) {
                    dateFieldChanged2 = dateFieldChanged | updateMax(currPickerColumn, this.mMaxDate.get(currField));
                } else {
                    dateFieldChanged2 = dateFieldChanged | updateMax(currPickerColumn, this.mCurrentDate.getActualMaximum(currField));
                }
                allLargerDateFieldsHaveBeenEqualToMinDate &= this.mCurrentDate.get(currField) == this.mMinDate.get(currField);
                allLargerDateFieldsHaveBeenEqualToMaxDate &= this.mCurrentDate.get(currField) == this.mMaxDate.get(currField);
                if (dateFieldChanged2) {
                    setColumnAt(dateFieldIndices[i], currPickerColumn);
                }
                setColumnValue(dateFieldIndices[i], this.mCurrentDate.get(currField), animation);
            }
        }
    }

    private void updateSpinners(final boolean animation) {
        post(new Runnable() {
            @Override
            public void run() {
                DatePicker.this.updateSpinnersImpl(animation);
            }
        });
    }
}
