package android.support.v17.leanback.widget.picker;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.picker.PickerUtility;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DatePicker extends Picker {
    private static final int[] DATE_FIELDS = {5, 2, 1};
    int mColDayIndex;
    int mColMonthIndex;
    int mColYearIndex;
    PickerUtility.DateConstant mConstant;
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
        TypedArray attributesArray = context.obtainStyledAttributes(attrs, R.styleable.lbDatePicker);
        String minDate = attributesArray.getString(R.styleable.lbDatePicker_android_minDate);
        String maxDate = attributesArray.getString(R.styleable.lbDatePicker_android_maxDate);
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
        String datePickerFormat = attributesArray.getString(R.styleable.lbDatePicker_datePickerFormat);
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

    String getBestYearMonthDayPattern(String datePickerFormat) {
        String yearPattern;
        if (PickerUtility.SUPPORTS_BEST_DATE_TIME_PATTERN) {
            yearPattern = android.text.format.DateFormat.getBestDateTimePattern(this.mConstant.locale, datePickerFormat);
        } else {
            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getContext());
            if (dateFormat instanceof SimpleDateFormat) {
                String yearPattern2 = ((SimpleDateFormat) dateFormat).toLocalizedPattern();
                yearPattern = yearPattern2;
            } else {
                yearPattern = "MM/dd/yyyy";
            }
        }
        return TextUtils.isEmpty(yearPattern) ? "MM/dd/yyyy" : yearPattern;
    }

    List<CharSequence> extractSeparators() {
        String hmaPattern = getBestYearMonthDayPattern(this.mDatePickerFormat);
        List<CharSequence> separators = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        char[] dateFormats = {'Y', 'y', 'M', 'm', 'D', 'd'};
        boolean processingQuote = false;
        char lastChar = 0;
        for (int i = 0; i < hmaPattern.length(); i++) {
            char c = hmaPattern.charAt(i);
            if (c != ' ') {
                if (c == '\'') {
                    if (!processingQuote) {
                        sb.setLength(0);
                        processingQuote = true;
                    } else {
                        processingQuote = false;
                    }
                } else {
                    if (!processingQuote && isAnyOf(c, dateFormats)) {
                        if (c != lastChar) {
                            separators.add(sb.toString());
                            sb.setLength(0);
                        }
                    } else {
                        sb.append(c);
                    }
                    lastChar = c;
                }
            }
        }
        separators.add(sb.toString());
        return separators;
    }

    private static boolean isAnyOf(char c, char[] any) {
        for (char c2 : any) {
            if (c == c2) {
                return true;
            }
        }
        return false;
    }

    public void setDatePickerFormat(String datePickerFormat) {
        if (TextUtils.isEmpty(datePickerFormat)) {
            datePickerFormat = new String(android.text.format.DateFormat.getDateFormatOrder(getContext()));
        }
        if (TextUtils.equals(this.mDatePickerFormat, datePickerFormat)) {
            return;
        }
        this.mDatePickerFormat = datePickerFormat;
        List<CharSequence> separators = extractSeparators();
        if (separators.size() != datePickerFormat.length() + 1) {
            throw new IllegalStateException("Separators size: " + separators.size() + " must equal the size of datePickerFormat: " + datePickerFormat.length() + " + 1");
        }
        setSeparators(separators);
        this.mDayColumn = null;
        this.mMonthColumn = null;
        this.mYearColumn = null;
        this.mColMonthIndex = -1;
        this.mColDayIndex = -1;
        this.mColYearIndex = -1;
        String dateFieldsPattern = datePickerFormat.toUpperCase();
        ArrayList<PickerColumn> columns = new ArrayList<>(3);
        for (int i = 0; i < dateFieldsPattern.length(); i++) {
            char cCharAt = dateFieldsPattern.charAt(i);
            if (cCharAt == 'D') {
                if (this.mDayColumn != null) {
                    throw new IllegalArgumentException("datePicker format error");
                }
                PickerColumn pickerColumn = new PickerColumn();
                this.mDayColumn = pickerColumn;
                columns.add(pickerColumn);
                this.mDayColumn.setLabelFormat("%02d");
                this.mColDayIndex = i;
            } else if (cCharAt == 'M') {
                if (this.mMonthColumn != null) {
                    throw new IllegalArgumentException("datePicker format error");
                }
                PickerColumn pickerColumn2 = new PickerColumn();
                this.mMonthColumn = pickerColumn2;
                columns.add(pickerColumn2);
                this.mMonthColumn.setStaticLabels(this.mConstant.months);
                this.mColMonthIndex = i;
            } else if (cCharAt == 'Y') {
                if (this.mYearColumn != null) {
                    throw new IllegalArgumentException("datePicker format error");
                }
                PickerColumn pickerColumn3 = new PickerColumn();
                this.mYearColumn = pickerColumn3;
                columns.add(pickerColumn3);
                this.mColYearIndex = i;
                this.mYearColumn.setLabelFormat("%d");
            } else {
                throw new IllegalArgumentException("datePicker format error");
            }
        }
        setColumns(columns);
        updateSpinners(false);
    }

    private void updateCurrentLocale() {
        this.mConstant = PickerUtility.getDateConstantInstance(Locale.getDefault(), getContext().getResources());
        this.mTempDate = PickerUtility.getCalendarForLocale(this.mTempDate, this.mConstant.locale);
        this.mMinDate = PickerUtility.getCalendarForLocale(this.mMinDate, this.mConstant.locale);
        this.mMaxDate = PickerUtility.getCalendarForLocale(this.mMaxDate, this.mConstant.locale);
        this.mCurrentDate = PickerUtility.getCalendarForLocale(this.mCurrentDate, this.mConstant.locale);
        if (this.mMonthColumn != null) {
            this.mMonthColumn.setStaticLabels(this.mConstant.months);
            setColumnAt(this.mColMonthIndex, this.mMonthColumn);
        }
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

    public void setMinDate(long minDate) {
        this.mTempDate.setTimeInMillis(minDate);
        if (this.mTempDate.get(1) == this.mMinDate.get(1) && this.mTempDate.get(6) != this.mMinDate.get(6)) {
            return;
        }
        this.mMinDate.setTimeInMillis(minDate);
        if (this.mCurrentDate.before(this.mMinDate)) {
            this.mCurrentDate.setTimeInMillis(this.mMinDate.getTimeInMillis());
        }
        updateSpinners(false);
    }

    public void setMaxDate(long maxDate) {
        this.mTempDate.setTimeInMillis(maxDate);
        if (this.mTempDate.get(1) == this.mMaxDate.get(1) && this.mTempDate.get(6) != this.mMaxDate.get(6)) {
            return;
        }
        this.mMaxDate.setTimeInMillis(maxDate);
        if (this.mCurrentDate.after(this.mMaxDate)) {
            this.mCurrentDate.setTimeInMillis(this.mMaxDate.getTimeInMillis());
        }
        updateSpinners(false);
    }

    public long getDate() {
        return this.mCurrentDate.getTimeInMillis();
    }

    private void setDate(int year, int month, int dayOfMonth) {
        this.mCurrentDate.set(year, month, dayOfMonth);
        if (this.mCurrentDate.before(this.mMinDate)) {
            this.mCurrentDate.setTimeInMillis(this.mMinDate.getTimeInMillis());
        } else if (this.mCurrentDate.after(this.mMaxDate)) {
            this.mCurrentDate.setTimeInMillis(this.mMaxDate.getTimeInMillis());
        }
    }

    public void updateDate(int year, int month, int dayOfMonth, boolean animation) {
        if (!isNewDate(year, month, dayOfMonth)) {
            return;
        }
        setDate(year, month, dayOfMonth);
        updateSpinners(animation);
    }

    private boolean isNewDate(int year, int month, int dayOfMonth) {
        return (this.mCurrentDate.get(1) == year && this.mCurrentDate.get(2) == dayOfMonth && this.mCurrentDate.get(5) == month) ? false : true;
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

    void updateSpinnersImpl(boolean animation) {
        boolean dateFieldChanged;
        int[] dateFieldIndices = {this.mColDayIndex, this.mColMonthIndex, this.mColYearIndex};
        boolean allLargerDateFieldsHaveBeenEqualToMinDate = true;
        boolean allLargerDateFieldsHaveBeenEqualToMaxDate = true;
        for (int i = DATE_FIELDS.length - 1; i >= 0; i--) {
            if (dateFieldIndices[i] >= 0) {
                int currField = DATE_FIELDS[i];
                PickerColumn currPickerColumn = getColumnAt(dateFieldIndices[i]);
                boolean dateFieldChanged2 = allLargerDateFieldsHaveBeenEqualToMinDate ? false | updateMin(currPickerColumn, this.mMinDate.get(currField)) : false | updateMin(currPickerColumn, this.mCurrentDate.getActualMinimum(currField));
                if (allLargerDateFieldsHaveBeenEqualToMaxDate) {
                    dateFieldChanged = dateFieldChanged2 | updateMax(currPickerColumn, this.mMaxDate.get(currField));
                } else {
                    dateFieldChanged = dateFieldChanged2 | updateMax(currPickerColumn, this.mCurrentDate.getActualMaximum(currField));
                }
                allLargerDateFieldsHaveBeenEqualToMinDate &= this.mCurrentDate.get(currField) == this.mMinDate.get(currField);
                allLargerDateFieldsHaveBeenEqualToMaxDate &= this.mCurrentDate.get(currField) == this.mMaxDate.get(currField);
                if (dateFieldChanged) {
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
