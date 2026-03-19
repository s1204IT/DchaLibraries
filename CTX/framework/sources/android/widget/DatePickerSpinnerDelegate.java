package android.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.icu.util.Calendar;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import com.android.internal.R;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import libcore.icu.ICU;

class DatePickerSpinnerDelegate extends DatePicker.AbstractDatePickerDelegate {
    private static final String DATE_FORMAT = "MM/dd/yyyy";
    private static final boolean DEFAULT_CALENDAR_VIEW_SHOWN = true;
    private static final boolean DEFAULT_ENABLED_STATE = true;
    private static final int DEFAULT_END_YEAR = 2100;
    private static final boolean DEFAULT_SPINNERS_SHOWN = true;
    private static final int DEFAULT_START_YEAR = 1900;
    private final CalendarView mCalendarView;
    private final DateFormat mDateFormat;
    private final NumberPicker mDaySpinner;
    private final EditText mDaySpinnerInput;
    private boolean mIsEnabled;
    private Calendar mMaxDate;
    private Calendar mMinDate;
    private final NumberPicker mMonthSpinner;
    private final EditText mMonthSpinnerInput;
    private int mNumberOfMonths;
    private String[] mShortMonths;
    private final LinearLayout mSpinners;
    private Calendar mTempDate;
    private final NumberPicker mYearSpinner;
    private final EditText mYearSpinnerInput;

    DatePickerSpinnerDelegate(DatePicker datePicker, Context context, AttributeSet attributeSet, int i, int i2) {
        super(datePicker, context);
        this.mDateFormat = new SimpleDateFormat(DATE_FORMAT);
        this.mIsEnabled = true;
        this.mDelegator = datePicker;
        this.mContext = context;
        setCurrentLocale(Locale.getDefault());
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.DatePicker, i, i2);
        boolean z = typedArrayObtainStyledAttributes.getBoolean(6, true);
        boolean z2 = typedArrayObtainStyledAttributes.getBoolean(7, true);
        int i3 = typedArrayObtainStyledAttributes.getInt(1, 1900);
        int i4 = typedArrayObtainStyledAttributes.getInt(2, 2100);
        String string = typedArrayObtainStyledAttributes.getString(4);
        String string2 = typedArrayObtainStyledAttributes.getString(5);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(20, R.layout.date_picker_legacy);
        typedArrayObtainStyledAttributes.recycle();
        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(resourceId, (ViewGroup) this.mDelegator, true).setSaveFromParentEnabled(false);
        NumberPicker.OnValueChangeListener onValueChangeListener = new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i5, int i6) {
                DatePickerSpinnerDelegate.this.updateInputState();
                DatePickerSpinnerDelegate.this.mTempDate.setTimeInMillis(DatePickerSpinnerDelegate.this.mCurrentDate.getTimeInMillis());
                if (numberPicker == DatePickerSpinnerDelegate.this.mDaySpinner) {
                    int actualMaximum = DatePickerSpinnerDelegate.this.mTempDate.getActualMaximum(5);
                    if (i5 == actualMaximum && i6 == 1) {
                        DatePickerSpinnerDelegate.this.mTempDate.add(5, 1);
                    } else if (i5 != 1 || i6 != actualMaximum) {
                        DatePickerSpinnerDelegate.this.mTempDate.add(5, i6 - i5);
                    } else {
                        DatePickerSpinnerDelegate.this.mTempDate.add(5, -1);
                    }
                } else if (numberPicker != DatePickerSpinnerDelegate.this.mMonthSpinner) {
                    if (numberPicker == DatePickerSpinnerDelegate.this.mYearSpinner) {
                        DatePickerSpinnerDelegate.this.mTempDate.set(1, i6);
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else if (i5 == 11 && i6 == 0) {
                    DatePickerSpinnerDelegate.this.mTempDate.add(2, 1);
                } else if (i5 != 0 || i6 != 11) {
                    DatePickerSpinnerDelegate.this.mTempDate.add(2, i6 - i5);
                } else {
                    DatePickerSpinnerDelegate.this.mTempDate.add(2, -1);
                }
                DatePickerSpinnerDelegate.this.setDate(DatePickerSpinnerDelegate.this.mTempDate.get(1), DatePickerSpinnerDelegate.this.mTempDate.get(2), DatePickerSpinnerDelegate.this.mTempDate.get(5));
                DatePickerSpinnerDelegate.this.updateSpinners();
                DatePickerSpinnerDelegate.this.updateCalendarView();
                DatePickerSpinnerDelegate.this.notifyDateChanged();
            }
        };
        this.mSpinners = (LinearLayout) this.mDelegator.findViewById(R.id.pickers);
        this.mCalendarView = (CalendarView) this.mDelegator.findViewById(R.id.calendar_view);
        this.mCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView calendarView, int i5, int i6, int i7) {
                DatePickerSpinnerDelegate.this.setDate(i5, i6, i7);
                DatePickerSpinnerDelegate.this.updateSpinners();
                DatePickerSpinnerDelegate.this.notifyDateChanged();
            }
        });
        this.mDaySpinner = (NumberPicker) this.mDelegator.findViewById(R.id.day);
        this.mDaySpinner.setFormatter(NumberPicker.getTwoDigitFormatter());
        this.mDaySpinner.setOnLongPressUpdateInterval(100L);
        this.mDaySpinner.setOnValueChangedListener(onValueChangeListener);
        this.mDaySpinnerInput = (EditText) this.mDaySpinner.findViewById(R.id.numberpicker_input);
        this.mMonthSpinner = (NumberPicker) this.mDelegator.findViewById(R.id.month);
        this.mMonthSpinner.setMinValue(0);
        this.mMonthSpinner.setMaxValue(this.mNumberOfMonths - 1);
        this.mMonthSpinner.setDisplayedValues(this.mShortMonths);
        this.mMonthSpinner.setOnLongPressUpdateInterval(200L);
        this.mMonthSpinner.setOnValueChangedListener(onValueChangeListener);
        this.mMonthSpinnerInput = (EditText) this.mMonthSpinner.findViewById(R.id.numberpicker_input);
        this.mYearSpinner = (NumberPicker) this.mDelegator.findViewById(R.id.year);
        this.mYearSpinner.setOnLongPressUpdateInterval(100L);
        this.mYearSpinner.setOnValueChangedListener(onValueChangeListener);
        this.mYearSpinnerInput = (EditText) this.mYearSpinner.findViewById(R.id.numberpicker_input);
        if (!z && !z2) {
            setSpinnersShown(true);
        } else {
            setSpinnersShown(z);
            setCalendarViewShown(z2);
        }
        this.mTempDate.clear();
        if (TextUtils.isEmpty(string) || !parseDate(string, this.mTempDate)) {
            this.mTempDate.set(i3, 0, 1);
        }
        setMinDate(this.mTempDate.getTimeInMillis());
        this.mTempDate.clear();
        if (TextUtils.isEmpty(string2) || !parseDate(string2, this.mTempDate)) {
            this.mTempDate.set(i4, 11, 31);
        }
        setMaxDate(this.mTempDate.getTimeInMillis());
        this.mCurrentDate.setTimeInMillis(System.currentTimeMillis());
        init(this.mCurrentDate.get(1), this.mCurrentDate.get(2), this.mCurrentDate.get(5), null);
        reorderSpinners();
        setContentDescriptions();
        if (this.mDelegator.getImportantForAccessibility() == 0) {
            this.mDelegator.setImportantForAccessibility(1);
        }
    }

    @Override
    public void init(int i, int i2, int i3, DatePicker.OnDateChangedListener onDateChangedListener) {
        setDate(i, i2, i3);
        updateSpinners();
        updateCalendarView();
        this.mOnDateChangedListener = onDateChangedListener;
    }

    @Override
    public void updateDate(int i, int i2, int i3) {
        if (!isNewDate(i, i2, i3)) {
            return;
        }
        setDate(i, i2, i3);
        updateSpinners();
        updateCalendarView();
        notifyDateChanged();
    }

    @Override
    public int getYear() {
        return this.mCurrentDate.get(1);
    }

    @Override
    public int getMonth() {
        return this.mCurrentDate.get(2);
    }

    @Override
    public int getDayOfMonth() {
        return this.mCurrentDate.get(5);
    }

    @Override
    public void setFirstDayOfWeek(int i) {
        this.mCalendarView.setFirstDayOfWeek(i);
    }

    @Override
    public int getFirstDayOfWeek() {
        return this.mCalendarView.getFirstDayOfWeek();
    }

    @Override
    public void setMinDate(long j) {
        this.mTempDate.setTimeInMillis(j);
        if (this.mTempDate.get(1) == this.mMinDate.get(1) && this.mTempDate.get(6) == this.mMinDate.get(6)) {
            return;
        }
        this.mMinDate.setTimeInMillis(j);
        this.mCalendarView.setMinDate(j);
        if (this.mCurrentDate.before(this.mMinDate)) {
            this.mCurrentDate.setTimeInMillis(this.mMinDate.getTimeInMillis());
            updateCalendarView();
        }
        updateSpinners();
    }

    @Override
    public Calendar getMinDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(this.mCalendarView.getMinDate());
        return calendar;
    }

    @Override
    public void setMaxDate(long j) {
        this.mTempDate.setTimeInMillis(j);
        if (this.mTempDate.get(1) == this.mMaxDate.get(1) && this.mTempDate.get(6) == this.mMaxDate.get(6)) {
            return;
        }
        this.mMaxDate.setTimeInMillis(j);
        this.mCalendarView.setMaxDate(j);
        if (this.mCurrentDate.after(this.mMaxDate)) {
            this.mCurrentDate.setTimeInMillis(this.mMaxDate.getTimeInMillis());
            updateCalendarView();
        }
        updateSpinners();
    }

    @Override
    public Calendar getMaxDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(this.mCalendarView.getMaxDate());
        return calendar;
    }

    @Override
    public void setEnabled(boolean z) {
        this.mDaySpinner.setEnabled(z);
        this.mMonthSpinner.setEnabled(z);
        this.mYearSpinner.setEnabled(z);
        this.mCalendarView.setEnabled(z);
        this.mIsEnabled = z;
    }

    @Override
    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    @Override
    public CalendarView getCalendarView() {
        return this.mCalendarView;
    }

    @Override
    public void setCalendarViewShown(boolean z) {
        this.mCalendarView.setVisibility(z ? 0 : 8);
    }

    @Override
    public boolean getCalendarViewShown() {
        return this.mCalendarView.getVisibility() == 0;
    }

    @Override
    public void setSpinnersShown(boolean z) {
        this.mSpinners.setVisibility(z ? 0 : 8);
    }

    @Override
    public boolean getSpinnersShown() {
        return this.mSpinners.isShown();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        setCurrentLocale(configuration.locale);
    }

    @Override
    public Parcelable onSaveInstanceState(Parcelable parcelable) {
        return new DatePicker.AbstractDatePickerDelegate.SavedState(parcelable, getYear(), getMonth(), getDayOfMonth(), getMinDate().getTimeInMillis(), getMaxDate().getTimeInMillis());
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable instanceof DatePicker.AbstractDatePickerDelegate.SavedState) {
            DatePicker.AbstractDatePickerDelegate.SavedState savedState = (DatePicker.AbstractDatePickerDelegate.SavedState) parcelable;
            setDate(savedState.getSelectedYear(), savedState.getSelectedMonth(), savedState.getSelectedDay());
            updateSpinners();
            updateCalendarView();
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        onPopulateAccessibilityEvent(accessibilityEvent);
        return true;
    }

    @Override
    protected void setCurrentLocale(Locale locale) {
        super.setCurrentLocale(locale);
        this.mTempDate = getCalendarForLocale(this.mTempDate, locale);
        this.mMinDate = getCalendarForLocale(this.mMinDate, locale);
        this.mMaxDate = getCalendarForLocale(this.mMaxDate, locale);
        this.mCurrentDate = getCalendarForLocale(this.mCurrentDate, locale);
        this.mNumberOfMonths = this.mTempDate.getActualMaximum(2) + 1;
        this.mShortMonths = new DateFormatSymbols().getShortMonths();
        if (usingNumericMonths()) {
            this.mShortMonths = new String[this.mNumberOfMonths];
            int i = 0;
            while (i < this.mNumberOfMonths) {
                int i2 = i + 1;
                this.mShortMonths[i] = String.format("%d", Integer.valueOf(i2));
                i = i2;
            }
        }
    }

    private boolean usingNumericMonths() {
        return Character.isDigit(this.mShortMonths[0].charAt(0));
    }

    private Calendar getCalendarForLocale(Calendar calendar, Locale locale) {
        if (calendar == null) {
            return Calendar.getInstance(locale);
        }
        long timeInMillis = calendar.getTimeInMillis();
        Calendar calendar2 = Calendar.getInstance(locale);
        calendar2.setTimeInMillis(timeInMillis);
        return calendar2;
    }

    private void reorderSpinners() {
        this.mSpinners.removeAllViews();
        char[] dateFormatOrder = ICU.getDateFormatOrder(android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyMMMdd"));
        int length = dateFormatOrder.length;
        for (int i = 0; i < length; i++) {
            char c = dateFormatOrder[i];
            if (c == 'M') {
                this.mSpinners.addView(this.mMonthSpinner);
                setImeOptions(this.mMonthSpinner, length, i);
            } else if (c == 'd') {
                this.mSpinners.addView(this.mDaySpinner);
                setImeOptions(this.mDaySpinner, length, i);
            } else if (c == 'y') {
                this.mSpinners.addView(this.mYearSpinner);
                setImeOptions(this.mYearSpinner, length, i);
            } else {
                throw new IllegalArgumentException(Arrays.toString(dateFormatOrder));
            }
        }
    }

    private boolean parseDate(String str, Calendar calendar) {
        try {
            calendar.setTime(this.mDateFormat.parse(str));
            return true;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isNewDate(int i, int i2, int i3) {
        return (this.mCurrentDate.get(1) == i && this.mCurrentDate.get(2) == i2 && this.mCurrentDate.get(5) == i3) ? false : true;
    }

    private void setDate(int i, int i2, int i3) {
        this.mCurrentDate.set(i, i2, i3);
        resetAutofilledValue();
        if (this.mCurrentDate.before(this.mMinDate)) {
            this.mCurrentDate.setTimeInMillis(this.mMinDate.getTimeInMillis());
        } else if (this.mCurrentDate.after(this.mMaxDate)) {
            this.mCurrentDate.setTimeInMillis(this.mMaxDate.getTimeInMillis());
        }
    }

    private void updateSpinners() {
        if (this.mCurrentDate.equals(this.mMinDate)) {
            this.mDaySpinner.setMinValue(this.mCurrentDate.get(5));
            this.mDaySpinner.setMaxValue(this.mCurrentDate.getActualMaximum(5));
            this.mDaySpinner.setWrapSelectorWheel(false);
            this.mMonthSpinner.setDisplayedValues(null);
            this.mMonthSpinner.setMinValue(this.mCurrentDate.get(2));
            this.mMonthSpinner.setMaxValue(this.mCurrentDate.getActualMaximum(2));
            this.mMonthSpinner.setWrapSelectorWheel(false);
        } else if (this.mCurrentDate.equals(this.mMaxDate)) {
            this.mDaySpinner.setMinValue(this.mCurrentDate.getActualMinimum(5));
            this.mDaySpinner.setMaxValue(this.mCurrentDate.get(5));
            this.mDaySpinner.setWrapSelectorWheel(false);
            this.mMonthSpinner.setDisplayedValues(null);
            this.mMonthSpinner.setMinValue(this.mCurrentDate.getActualMinimum(2));
            this.mMonthSpinner.setMaxValue(this.mCurrentDate.get(2));
            this.mMonthSpinner.setWrapSelectorWheel(false);
        } else {
            this.mDaySpinner.setMinValue(1);
            this.mDaySpinner.setMaxValue(this.mCurrentDate.getActualMaximum(5));
            this.mDaySpinner.setWrapSelectorWheel(true);
            this.mMonthSpinner.setDisplayedValues(null);
            this.mMonthSpinner.setMinValue(0);
            this.mMonthSpinner.setMaxValue(11);
            this.mMonthSpinner.setWrapSelectorWheel(true);
        }
        this.mMonthSpinner.setDisplayedValues((String[]) Arrays.copyOfRange(this.mShortMonths, this.mMonthSpinner.getMinValue(), this.mMonthSpinner.getMaxValue() + 1));
        this.mYearSpinner.setMinValue(this.mMinDate.get(1));
        this.mYearSpinner.setMaxValue(this.mMaxDate.get(1));
        this.mYearSpinner.setWrapSelectorWheel(false);
        this.mYearSpinner.setValue(this.mCurrentDate.get(1));
        this.mMonthSpinner.setValue(this.mCurrentDate.get(2));
        this.mDaySpinner.setValue(this.mCurrentDate.get(5));
        if (usingNumericMonths()) {
            this.mMonthSpinnerInput.setRawInputType(2);
        }
    }

    private void updateCalendarView() {
        this.mCalendarView.setDate(this.mCurrentDate.getTimeInMillis(), false, false);
    }

    private void notifyDateChanged() {
        this.mDelegator.sendAccessibilityEvent(4);
        if (this.mOnDateChangedListener != null) {
            this.mOnDateChangedListener.onDateChanged(this.mDelegator, getYear(), getMonth(), getDayOfMonth());
        }
        if (this.mAutoFillChangeListener != null) {
            this.mAutoFillChangeListener.onDateChanged(this.mDelegator, getYear(), getMonth(), getDayOfMonth());
        }
    }

    private void setImeOptions(NumberPicker numberPicker, int i, int i2) {
        int i3;
        if (i2 < i - 1) {
            i3 = 5;
        } else {
            i3 = 6;
        }
        ((TextView) numberPicker.findViewById(R.id.numberpicker_input)).setImeOptions(i3);
    }

    private void setContentDescriptions() {
        trySetContentDescription(this.mDaySpinner, R.id.increment, R.string.date_picker_increment_day_button);
        trySetContentDescription(this.mDaySpinner, R.id.decrement, R.string.date_picker_decrement_day_button);
        trySetContentDescription(this.mMonthSpinner, R.id.increment, R.string.date_picker_increment_month_button);
        trySetContentDescription(this.mMonthSpinner, R.id.decrement, R.string.date_picker_decrement_month_button);
        trySetContentDescription(this.mYearSpinner, R.id.increment, R.string.date_picker_increment_year_button);
        trySetContentDescription(this.mYearSpinner, R.id.decrement, R.string.date_picker_decrement_year_button);
    }

    private void trySetContentDescription(View view, int i, int i2) {
        View viewFindViewById = view.findViewById(i);
        if (viewFindViewById != null) {
            viewFindViewById.setContentDescription(this.mContext.getString(i2));
        }
    }

    private void updateInputState() {
        InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
        if (inputMethodManagerPeekInstance != null) {
            if (inputMethodManagerPeekInstance.isActive(this.mYearSpinnerInput)) {
                this.mYearSpinnerInput.clearFocus();
                inputMethodManagerPeekInstance.hideSoftInputFromWindow(this.mDelegator.getWindowToken(), 0);
            } else if (inputMethodManagerPeekInstance.isActive(this.mMonthSpinnerInput)) {
                this.mMonthSpinnerInput.clearFocus();
                inputMethodManagerPeekInstance.hideSoftInputFromWindow(this.mDelegator.getWindowToken(), 0);
            } else if (inputMethodManagerPeekInstance.isActive(this.mDaySpinnerInput)) {
                this.mDaySpinnerInput.clearFocus();
                inputMethodManagerPeekInstance.hideSoftInputFromWindow(this.mDelegator.getWindowToken(), 0);
            }
        }
    }
}
