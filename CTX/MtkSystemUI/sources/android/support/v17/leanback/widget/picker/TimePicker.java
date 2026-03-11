package android.support.v17.leanback.widget.picker;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.picker.PickerUtility;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TimePicker extends Picker {
    PickerColumn mAmPmColumn;
    int mColAmPmIndex;
    int mColHourIndex;
    int mColMinuteIndex;
    private final PickerUtility.TimeConstant mConstant;
    private int mCurrentAmPmIndex;
    private int mCurrentHour;
    private int mCurrentMinute;
    PickerColumn mHourColumn;
    private boolean mIs24hFormat;
    PickerColumn mMinuteColumn;
    private String mTimePickerFormat;

    public TimePicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mConstant = PickerUtility.getTimeConstantInstance(Locale.getDefault(), context.getResources());
        TypedArray attributesArray = context.obtainStyledAttributes(attrs, R.styleable.lbTimePicker);
        this.mIs24hFormat = attributesArray.getBoolean(R.styleable.lbTimePicker_is24HourFormat, DateFormat.is24HourFormat(context));
        boolean useCurrentTime = attributesArray.getBoolean(R.styleable.lbTimePicker_useCurrentTime, true);
        updateColumns();
        updateColumnsRange();
        if (useCurrentTime) {
            Calendar currentDate = PickerUtility.getCalendarForLocale(null, this.mConstant.locale);
            setHour(currentDate.get(11));
            setMinute(currentDate.get(12));
            setAmPmValue();
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

    String getBestHourMinutePattern() {
        String hourPattern;
        if (PickerUtility.SUPPORTS_BEST_DATE_TIME_PATTERN) {
            String hourPattern2 = DateFormat.getBestDateTimePattern(this.mConstant.locale, this.mIs24hFormat ? "Hma" : "hma");
            hourPattern = hourPattern2;
        } else {
            java.text.DateFormat dateFormat = SimpleDateFormat.getTimeInstance(3, this.mConstant.locale);
            if (dateFormat instanceof SimpleDateFormat) {
                String defaultPattern = ((SimpleDateFormat) dateFormat).toPattern();
                hourPattern = defaultPattern.replace("s", "");
                if (this.mIs24hFormat) {
                    hourPattern = hourPattern.replace('h', 'H').replace("a", "");
                }
            } else {
                hourPattern = this.mIs24hFormat ? "H:mma" : "h:mma";
            }
        }
        String hourPattern3 = hourPattern;
        return TextUtils.isEmpty(hourPattern3) ? "h:mma" : hourPattern3;
    }

    List<CharSequence> extractSeparators() {
        String hmaPattern = getBestHourMinutePattern();
        List<CharSequence> separators = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        char[] timeFormats = {'H', 'h', 'K', 'k', 'm', 'M', 'a'};
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
                    if (!processingQuote && isAnyOf(c, timeFormats)) {
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

    private String extractTimeFields() {
        StringBuilder sb;
        String hmaPattern = getBestHourMinutePattern();
        boolean z = true;
        boolean isRTL = TextUtils.getLayoutDirectionFromLocale(this.mConstant.locale) == 1;
        if (hmaPattern.indexOf(97) >= 0 && hmaPattern.indexOf("a") <= hmaPattern.indexOf("m")) {
            z = false;
        }
        boolean isAmPmAtEnd = z;
        String timePickerFormat = isRTL ? "mh" : "hm";
        if (is24Hour()) {
            return timePickerFormat;
        }
        if (isAmPmAtEnd) {
            sb = new StringBuilder();
            sb.append(timePickerFormat);
            sb.append("a");
        } else {
            sb = new StringBuilder();
            sb.append("a");
            sb.append(timePickerFormat);
        }
        return sb.toString();
    }

    private void updateColumns() {
        String timePickerFormat = getBestHourMinutePattern();
        if (TextUtils.equals(timePickerFormat, this.mTimePickerFormat)) {
            return;
        }
        this.mTimePickerFormat = timePickerFormat;
        String timeFieldsPattern = extractTimeFields();
        List<CharSequence> separators = extractSeparators();
        if (separators.size() != timeFieldsPattern.length() + 1) {
            throw new IllegalStateException("Separators size: " + separators.size() + " must equal the size of timeFieldsPattern: " + timeFieldsPattern.length() + " + 1");
        }
        setSeparators(separators);
        String timeFieldsPattern2 = timeFieldsPattern.toUpperCase();
        this.mAmPmColumn = null;
        this.mMinuteColumn = null;
        this.mHourColumn = null;
        this.mColAmPmIndex = -1;
        this.mColMinuteIndex = -1;
        this.mColHourIndex = -1;
        ArrayList<PickerColumn> columns = new ArrayList<>(3);
        for (int i = 0; i < timeFieldsPattern2.length(); i++) {
            char cCharAt = timeFieldsPattern2.charAt(i);
            if (cCharAt == 'A') {
                PickerColumn pickerColumn = new PickerColumn();
                this.mAmPmColumn = pickerColumn;
                columns.add(pickerColumn);
                this.mAmPmColumn.setStaticLabels(this.mConstant.ampm);
                this.mColAmPmIndex = i;
                updateMin(this.mAmPmColumn, 0);
                updateMax(this.mAmPmColumn, 1);
            } else if (cCharAt == 'H') {
                PickerColumn pickerColumn2 = new PickerColumn();
                this.mHourColumn = pickerColumn2;
                columns.add(pickerColumn2);
                this.mHourColumn.setStaticLabels(this.mConstant.hours24);
                this.mColHourIndex = i;
            } else if (cCharAt == 'M') {
                PickerColumn pickerColumn3 = new PickerColumn();
                this.mMinuteColumn = pickerColumn3;
                columns.add(pickerColumn3);
                this.mMinuteColumn.setStaticLabels(this.mConstant.minutes);
                this.mColMinuteIndex = i;
            } else {
                throw new IllegalArgumentException("Invalid time picker format.");
            }
        }
        setColumns(columns);
    }

    private void updateColumnsRange() {
        updateMin(this.mHourColumn, !this.mIs24hFormat ? 1 : 0);
        updateMax(this.mHourColumn, this.mIs24hFormat ? 23 : 12);
        updateMin(this.mMinuteColumn, 0);
        updateMax(this.mMinuteColumn, 59);
        if (this.mAmPmColumn != null) {
            updateMin(this.mAmPmColumn, 0);
            updateMax(this.mAmPmColumn, 1);
        }
    }

    private void setAmPmValue() {
        if (!is24Hour()) {
            setColumnValue(this.mColAmPmIndex, this.mCurrentAmPmIndex, false);
        }
    }

    public void setHour(int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour: " + hour + " is not in [0-23] range in");
        }
        this.mCurrentHour = hour;
        if (!is24Hour()) {
            if (this.mCurrentHour >= 12) {
                this.mCurrentAmPmIndex = 1;
                if (this.mCurrentHour > 12) {
                    this.mCurrentHour -= 12;
                }
            } else {
                this.mCurrentAmPmIndex = 0;
                if (this.mCurrentHour == 0) {
                    this.mCurrentHour = 12;
                }
            }
            setAmPmValue();
        }
        setColumnValue(this.mColHourIndex, this.mCurrentHour, false);
    }

    public void setMinute(int minute) {
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("minute: " + minute + " is not in [0-59] range.");
        }
        this.mCurrentMinute = minute;
        setColumnValue(this.mColMinuteIndex, this.mCurrentMinute, false);
    }

    public boolean is24Hour() {
        return this.mIs24hFormat;
    }

    @Override
    public void onColumnValueChanged(int columnIndex, int newValue) {
        if (columnIndex == this.mColHourIndex) {
            this.mCurrentHour = newValue;
        } else if (columnIndex == this.mColMinuteIndex) {
            this.mCurrentMinute = newValue;
        } else {
            if (columnIndex == this.mColAmPmIndex) {
                this.mCurrentAmPmIndex = newValue;
                return;
            }
            throw new IllegalArgumentException("Invalid column index.");
        }
    }
}
