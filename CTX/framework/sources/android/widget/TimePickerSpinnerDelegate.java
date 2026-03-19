package android.widget;

import android.app.backup.FullBackup;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.provider.SettingsStringUtil;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.NumberPicker;
import android.widget.TimePicker;
import com.android.internal.R;
import java.util.Calendar;
import libcore.icu.LocaleData;

class TimePickerSpinnerDelegate extends TimePicker.AbstractTimePickerDelegate {
    private static final boolean DEFAULT_ENABLED_STATE = true;
    private static final int HOURS_IN_HALF_DAY = 12;
    private final Button mAmPmButton;
    private final NumberPicker mAmPmSpinner;
    private final EditText mAmPmSpinnerInput;
    private final String[] mAmPmStrings;
    private final TextView mDivider;
    private char mHourFormat;
    private final NumberPicker mHourSpinner;
    private final EditText mHourSpinnerInput;
    private boolean mHourWithTwoDigit;
    private boolean mIs24HourView;
    private boolean mIsAm;
    private boolean mIsEnabled;
    private final NumberPicker mMinuteSpinner;
    private final EditText mMinuteSpinnerInput;
    private final Calendar mTempCalendar;

    public TimePickerSpinnerDelegate(TimePicker timePicker, Context context, AttributeSet attributeSet, int i, int i2) {
        super(timePicker, context);
        this.mIsEnabled = true;
        TypedArray typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(attributeSet, R.styleable.TimePicker, i, i2);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(13, R.layout.time_picker_legacy);
        typedArrayObtainStyledAttributes.recycle();
        LayoutInflater.from(this.mContext).inflate(resourceId, (ViewGroup) this.mDelegator, true).setSaveFromParentEnabled(false);
        this.mHourSpinner = (NumberPicker) timePicker.findViewById(R.id.hour);
        this.mHourSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i3, int i4) {
                TimePickerSpinnerDelegate.this.updateInputState();
                if (!TimePickerSpinnerDelegate.this.is24Hour() && ((i3 == 11 && i4 == 12) || (i3 == 12 && i4 == 11))) {
                    TimePickerSpinnerDelegate.this.mIsAm = !TimePickerSpinnerDelegate.this.mIsAm;
                    TimePickerSpinnerDelegate.this.updateAmPmControl();
                }
                TimePickerSpinnerDelegate.this.onTimeChanged();
            }
        });
        this.mHourSpinnerInput = (EditText) this.mHourSpinner.findViewById(R.id.numberpicker_input);
        this.mHourSpinnerInput.setImeOptions(5);
        this.mDivider = (TextView) this.mDelegator.findViewById(R.id.divider);
        if (this.mDivider != null) {
            setDividerText();
        }
        this.mMinuteSpinner = (NumberPicker) this.mDelegator.findViewById(R.id.minute);
        this.mMinuteSpinner.setMinValue(0);
        this.mMinuteSpinner.setMaxValue(59);
        this.mMinuteSpinner.setOnLongPressUpdateInterval(100L);
        this.mMinuteSpinner.setFormatter(NumberPicker.getTwoDigitFormatter());
        this.mMinuteSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i3, int i4) {
                TimePickerSpinnerDelegate.this.updateInputState();
                int minValue = TimePickerSpinnerDelegate.this.mMinuteSpinner.getMinValue();
                int maxValue = TimePickerSpinnerDelegate.this.mMinuteSpinner.getMaxValue();
                if (i3 == maxValue && i4 == minValue) {
                    int value = TimePickerSpinnerDelegate.this.mHourSpinner.getValue() + 1;
                    if (!TimePickerSpinnerDelegate.this.is24Hour() && value == 12) {
                        TimePickerSpinnerDelegate.this.mIsAm = !TimePickerSpinnerDelegate.this.mIsAm;
                        TimePickerSpinnerDelegate.this.updateAmPmControl();
                    }
                    TimePickerSpinnerDelegate.this.mHourSpinner.setValue(value);
                } else if (i3 == minValue && i4 == maxValue) {
                    int value2 = TimePickerSpinnerDelegate.this.mHourSpinner.getValue() - 1;
                    if (!TimePickerSpinnerDelegate.this.is24Hour() && value2 == 11) {
                        TimePickerSpinnerDelegate.this.mIsAm = !TimePickerSpinnerDelegate.this.mIsAm;
                        TimePickerSpinnerDelegate.this.updateAmPmControl();
                    }
                    TimePickerSpinnerDelegate.this.mHourSpinner.setValue(value2);
                }
                TimePickerSpinnerDelegate.this.onTimeChanged();
            }
        });
        this.mMinuteSpinnerInput = (EditText) this.mMinuteSpinner.findViewById(R.id.numberpicker_input);
        this.mMinuteSpinnerInput.setImeOptions(5);
        this.mAmPmStrings = getAmPmStrings(context);
        View viewFindViewById = this.mDelegator.findViewById(R.id.amPm);
        if (viewFindViewById instanceof Button) {
            this.mAmPmSpinner = null;
            this.mAmPmSpinnerInput = null;
            this.mAmPmButton = (Button) viewFindViewById;
            this.mAmPmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.requestFocus();
                    TimePickerSpinnerDelegate.this.mIsAm = !TimePickerSpinnerDelegate.this.mIsAm;
                    TimePickerSpinnerDelegate.this.updateAmPmControl();
                    TimePickerSpinnerDelegate.this.onTimeChanged();
                }
            });
        } else {
            this.mAmPmButton = null;
            this.mAmPmSpinner = (NumberPicker) viewFindViewById;
            this.mAmPmSpinner.setMinValue(0);
            this.mAmPmSpinner.setMaxValue(1);
            this.mAmPmSpinner.setDisplayedValues(this.mAmPmStrings);
            this.mAmPmSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker numberPicker, int i3, int i4) {
                    TimePickerSpinnerDelegate.this.updateInputState();
                    numberPicker.requestFocus();
                    TimePickerSpinnerDelegate.this.mIsAm = !TimePickerSpinnerDelegate.this.mIsAm;
                    TimePickerSpinnerDelegate.this.updateAmPmControl();
                    TimePickerSpinnerDelegate.this.onTimeChanged();
                }
            });
            this.mAmPmSpinnerInput = (EditText) this.mAmPmSpinner.findViewById(R.id.numberpicker_input);
            this.mAmPmSpinnerInput.setImeOptions(6);
        }
        if (isAmPmAtStart()) {
            ViewGroup viewGroup = (ViewGroup) timePicker.findViewById(R.id.timePickerLayout);
            viewGroup.removeView(viewFindViewById);
            viewGroup.addView(viewFindViewById, 0);
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) viewFindViewById.getLayoutParams();
            int marginStart = marginLayoutParams.getMarginStart();
            int marginEnd = marginLayoutParams.getMarginEnd();
            if (marginStart != marginEnd) {
                marginLayoutParams.setMarginStart(marginEnd);
                marginLayoutParams.setMarginEnd(marginStart);
            }
        }
        getHourFormatData();
        updateHourControl();
        updateMinuteControl();
        updateAmPmControl();
        this.mTempCalendar = Calendar.getInstance(this.mLocale);
        setHour(this.mTempCalendar.get(11));
        setMinute(this.mTempCalendar.get(12));
        if (!isEnabled()) {
            setEnabled(false);
        }
        setContentDescriptions();
        if (this.mDelegator.getImportantForAccessibility() == 0) {
            this.mDelegator.setImportantForAccessibility(1);
        }
    }

    @Override
    public boolean validateInput() {
        return true;
    }

    private void getHourFormatData() {
        String bestDateTimePattern = DateFormat.getBestDateTimePattern(this.mLocale, this.mIs24HourView ? "Hm" : "hm");
        int length = bestDateTimePattern.length();
        this.mHourWithTwoDigit = false;
        for (int i = 0; i < length; i++) {
            char cCharAt = bestDateTimePattern.charAt(i);
            if (cCharAt == 'H' || cCharAt == 'h' || cCharAt == 'K' || cCharAt == 'k') {
                this.mHourFormat = cCharAt;
                int i2 = i + 1;
                if (i2 < length && cCharAt == bestDateTimePattern.charAt(i2)) {
                    this.mHourWithTwoDigit = true;
                    return;
                }
                return;
            }
        }
    }

    private boolean isAmPmAtStart() {
        return DateFormat.getBestDateTimePattern(this.mLocale, "hm").startsWith(FullBackup.APK_TREE_TOKEN);
    }

    private void setDividerText() {
        String strSubstring;
        String bestDateTimePattern = DateFormat.getBestDateTimePattern(this.mLocale, this.mIs24HourView ? "Hm" : "hm");
        int iLastIndexOf = bestDateTimePattern.lastIndexOf(72);
        if (iLastIndexOf == -1) {
            iLastIndexOf = bestDateTimePattern.lastIndexOf(104);
        }
        if (iLastIndexOf == -1) {
            strSubstring = SettingsStringUtil.DELIMITER;
        } else {
            int i = iLastIndexOf + 1;
            int iIndexOf = bestDateTimePattern.indexOf(109, i);
            if (iIndexOf == -1) {
                strSubstring = Character.toString(bestDateTimePattern.charAt(i));
            } else {
                strSubstring = bestDateTimePattern.substring(i, iIndexOf);
            }
        }
        this.mDivider.setText(strSubstring);
    }

    @Override
    public void setDate(int i, int i2) {
        setCurrentHour(i, false);
        setCurrentMinute(i2, false);
        onTimeChanged();
    }

    @Override
    public void setHour(int i) {
        setCurrentHour(i, true);
    }

    private void setCurrentHour(int i, boolean z) {
        if (i == getHour()) {
            return;
        }
        resetAutofilledValue();
        if (!is24Hour()) {
            if (i >= 12) {
                this.mIsAm = false;
                if (i > 12) {
                    i -= 12;
                }
            } else {
                this.mIsAm = true;
                if (i == 0) {
                    i = 12;
                }
            }
            updateAmPmControl();
        }
        this.mHourSpinner.setValue(i);
        if (z) {
            onTimeChanged();
        }
    }

    @Override
    public int getHour() {
        int value = this.mHourSpinner.getValue();
        if (is24Hour()) {
            return value;
        }
        if (this.mIsAm) {
            return value % 12;
        }
        return (value % 12) + 12;
    }

    @Override
    public void setMinute(int i) {
        setCurrentMinute(i, true);
    }

    private void setCurrentMinute(int i, boolean z) {
        if (i == getMinute()) {
            return;
        }
        resetAutofilledValue();
        this.mMinuteSpinner.setValue(i);
        if (z) {
            onTimeChanged();
        }
    }

    @Override
    public int getMinute() {
        return this.mMinuteSpinner.getValue();
    }

    @Override
    public void setIs24Hour(boolean z) {
        if (this.mIs24HourView == z) {
            return;
        }
        int hour = getHour();
        this.mIs24HourView = z;
        getHourFormatData();
        updateHourControl();
        setCurrentHour(hour, false);
        updateMinuteControl();
        updateAmPmControl();
    }

    @Override
    public boolean is24Hour() {
        return this.mIs24HourView;
    }

    @Override
    public void setEnabled(boolean z) {
        this.mMinuteSpinner.setEnabled(z);
        if (this.mDivider != null) {
            this.mDivider.setEnabled(z);
        }
        this.mHourSpinner.setEnabled(z);
        if (this.mAmPmSpinner != null) {
            this.mAmPmSpinner.setEnabled(z);
        } else {
            this.mAmPmButton.setEnabled(z);
        }
        this.mIsEnabled = z;
    }

    @Override
    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    @Override
    public int getBaseline() {
        return this.mHourSpinner.getBaseline();
    }

    @Override
    public Parcelable onSaveInstanceState(Parcelable parcelable) {
        return new TimePicker.AbstractTimePickerDelegate.SavedState(parcelable, getHour(), getMinute(), is24Hour());
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable instanceof TimePicker.AbstractTimePickerDelegate.SavedState) {
            TimePicker.AbstractTimePickerDelegate.SavedState savedState = (TimePicker.AbstractTimePickerDelegate.SavedState) parcelable;
            setHour(savedState.getHour());
            setMinute(savedState.getMinute());
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        onPopulateAccessibilityEvent(accessibilityEvent);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        int i;
        if (this.mIs24HourView) {
            i = 129;
        } else {
            i = 65;
        }
        this.mTempCalendar.set(11, getHour());
        this.mTempCalendar.set(12, getMinute());
        accessibilityEvent.getText().add(DateUtils.formatDateTime(this.mContext, this.mTempCalendar.getTimeInMillis(), i));
    }

    @Override
    public View getHourView() {
        return this.mHourSpinnerInput;
    }

    @Override
    public View getMinuteView() {
        return this.mMinuteSpinnerInput;
    }

    @Override
    public View getAmView() {
        return this.mAmPmSpinnerInput;
    }

    @Override
    public View getPmView() {
        return this.mAmPmSpinnerInput;
    }

    private void updateInputState() {
        InputMethodManager inputMethodManagerPeekInstance = InputMethodManager.peekInstance();
        if (inputMethodManagerPeekInstance != null) {
            if (inputMethodManagerPeekInstance.isActive(this.mHourSpinnerInput)) {
                this.mHourSpinnerInput.clearFocus();
                inputMethodManagerPeekInstance.hideSoftInputFromWindow(this.mDelegator.getWindowToken(), 0);
            } else if (inputMethodManagerPeekInstance.isActive(this.mMinuteSpinnerInput)) {
                this.mMinuteSpinnerInput.clearFocus();
                inputMethodManagerPeekInstance.hideSoftInputFromWindow(this.mDelegator.getWindowToken(), 0);
            } else if (inputMethodManagerPeekInstance.isActive(this.mAmPmSpinnerInput)) {
                this.mAmPmSpinnerInput.clearFocus();
                inputMethodManagerPeekInstance.hideSoftInputFromWindow(this.mDelegator.getWindowToken(), 0);
            }
        }
    }

    private void updateAmPmControl() {
        if (is24Hour()) {
            if (this.mAmPmSpinner != null) {
                this.mAmPmSpinner.setVisibility(8);
            } else {
                this.mAmPmButton.setVisibility(8);
            }
        } else {
            int i = !this.mIsAm ? 1 : 0;
            if (this.mAmPmSpinner != null) {
                this.mAmPmSpinner.setValue(i);
                this.mAmPmSpinner.setVisibility(0);
            } else {
                this.mAmPmButton.setText(this.mAmPmStrings[i]);
                this.mAmPmButton.setVisibility(0);
            }
        }
        this.mDelegator.sendAccessibilityEvent(4);
    }

    private void onTimeChanged() {
        this.mDelegator.sendAccessibilityEvent(4);
        if (this.mOnTimeChangedListener != null) {
            this.mOnTimeChangedListener.onTimeChanged(this.mDelegator, getHour(), getMinute());
        }
        if (this.mAutoFillChangeListener != null) {
            this.mAutoFillChangeListener.onTimeChanged(this.mDelegator, getHour(), getMinute());
        }
    }

    private void updateHourControl() {
        if (is24Hour()) {
            if (this.mHourFormat == 'k') {
                this.mHourSpinner.setMinValue(1);
                this.mHourSpinner.setMaxValue(24);
            } else {
                this.mHourSpinner.setMinValue(0);
                this.mHourSpinner.setMaxValue(23);
            }
        } else if (this.mHourFormat == 'K') {
            this.mHourSpinner.setMinValue(0);
            this.mHourSpinner.setMaxValue(11);
        } else {
            this.mHourSpinner.setMinValue(1);
            this.mHourSpinner.setMaxValue(12);
        }
        this.mHourSpinner.setFormatter(this.mHourWithTwoDigit ? NumberPicker.getTwoDigitFormatter() : null);
    }

    private void updateMinuteControl() {
        if (is24Hour()) {
            this.mMinuteSpinnerInput.setImeOptions(6);
        } else {
            this.mMinuteSpinnerInput.setImeOptions(5);
        }
    }

    private void setContentDescriptions() {
        trySetContentDescription(this.mMinuteSpinner, R.id.increment, R.string.time_picker_increment_minute_button);
        trySetContentDescription(this.mMinuteSpinner, R.id.decrement, R.string.time_picker_decrement_minute_button);
        trySetContentDescription(this.mHourSpinner, R.id.increment, R.string.time_picker_increment_hour_button);
        trySetContentDescription(this.mHourSpinner, R.id.decrement, R.string.time_picker_decrement_hour_button);
        if (this.mAmPmSpinner != null) {
            trySetContentDescription(this.mAmPmSpinner, R.id.increment, R.string.time_picker_increment_set_pm_button);
            trySetContentDescription(this.mAmPmSpinner, R.id.decrement, R.string.time_picker_decrement_set_am_button);
        }
    }

    private void trySetContentDescription(View view, int i, int i2) {
        View viewFindViewById = view.findViewById(i);
        if (viewFindViewById != null) {
            viewFindViewById.setContentDescription(this.mContext.getString(i2));
        }
    }

    public static String[] getAmPmStrings(Context context) {
        String[] strArr = new String[2];
        LocaleData localeData = LocaleData.get(context.getResources().getConfiguration().locale);
        strArr[0] = localeData.amPm[0].length() > 4 ? localeData.narrowAm : localeData.amPm[0];
        strArr[1] = localeData.amPm[1].length() > 4 ? localeData.narrowPm : localeData.amPm[1];
        return strArr;
    }
}
