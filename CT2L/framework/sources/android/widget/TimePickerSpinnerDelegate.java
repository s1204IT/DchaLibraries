package android.widget;

import android.app.backup.FullBackup;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.NumberPicker;
import android.widget.TimePicker;
import com.android.internal.R;
import java.util.Calendar;
import java.util.Locale;
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
    private Calendar mTempCalendar;

    public TimePickerSpinnerDelegate(TimePicker delegator, Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(delegator, context);
        this.mIsEnabled = true;
        TypedArray a = this.mContext.obtainStyledAttributes(attrs, R.styleable.TimePicker, defStyleAttr, defStyleRes);
        int layoutResourceId = a.getResourceId(10, R.layout.time_picker_legacy);
        a.recycle();
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        inflater.inflate(layoutResourceId, (ViewGroup) this.mDelegator, true);
        this.mHourSpinner = (NumberPicker) delegator.findViewById(R.id.hour);
        this.mHourSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker spinner, int oldVal, int newVal) {
                TimePickerSpinnerDelegate.this.updateInputState();
                if (!TimePickerSpinnerDelegate.this.is24HourView() && ((oldVal == 11 && newVal == 12) || (oldVal == 12 && newVal == 11))) {
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
            public void onValueChange(NumberPicker spinner, int oldVal, int newVal) {
                TimePickerSpinnerDelegate.this.updateInputState();
                int minValue = TimePickerSpinnerDelegate.this.mMinuteSpinner.getMinValue();
                int maxValue = TimePickerSpinnerDelegate.this.mMinuteSpinner.getMaxValue();
                if (oldVal == maxValue && newVal == minValue) {
                    int newHour = TimePickerSpinnerDelegate.this.mHourSpinner.getValue() + 1;
                    if (!TimePickerSpinnerDelegate.this.is24HourView() && newHour == 12) {
                        TimePickerSpinnerDelegate.this.mIsAm = TimePickerSpinnerDelegate.this.mIsAm ? false : true;
                        TimePickerSpinnerDelegate.this.updateAmPmControl();
                    }
                    TimePickerSpinnerDelegate.this.mHourSpinner.setValue(newHour);
                } else if (oldVal == minValue && newVal == maxValue) {
                    int newHour2 = TimePickerSpinnerDelegate.this.mHourSpinner.getValue() - 1;
                    if (!TimePickerSpinnerDelegate.this.is24HourView() && newHour2 == 11) {
                        TimePickerSpinnerDelegate.this.mIsAm = TimePickerSpinnerDelegate.this.mIsAm ? false : true;
                        TimePickerSpinnerDelegate.this.updateAmPmControl();
                    }
                    TimePickerSpinnerDelegate.this.mHourSpinner.setValue(newHour2);
                }
                TimePickerSpinnerDelegate.this.onTimeChanged();
            }
        });
        this.mMinuteSpinnerInput = (EditText) this.mMinuteSpinner.findViewById(R.id.numberpicker_input);
        this.mMinuteSpinnerInput.setImeOptions(5);
        this.mAmPmStrings = getAmPmStrings(context);
        View amPmView = this.mDelegator.findViewById(R.id.amPm);
        if (amPmView instanceof Button) {
            this.mAmPmSpinner = null;
            this.mAmPmSpinnerInput = null;
            this.mAmPmButton = (Button) amPmView;
            this.mAmPmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View button) {
                    button.requestFocus();
                    TimePickerSpinnerDelegate.this.mIsAm = !TimePickerSpinnerDelegate.this.mIsAm;
                    TimePickerSpinnerDelegate.this.updateAmPmControl();
                    TimePickerSpinnerDelegate.this.onTimeChanged();
                }
            });
        } else {
            this.mAmPmButton = null;
            this.mAmPmSpinner = (NumberPicker) amPmView;
            this.mAmPmSpinner.setMinValue(0);
            this.mAmPmSpinner.setMaxValue(1);
            this.mAmPmSpinner.setDisplayedValues(this.mAmPmStrings);
            this.mAmPmSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    TimePickerSpinnerDelegate.this.updateInputState();
                    picker.requestFocus();
                    TimePickerSpinnerDelegate.this.mIsAm = !TimePickerSpinnerDelegate.this.mIsAm;
                    TimePickerSpinnerDelegate.this.updateAmPmControl();
                    TimePickerSpinnerDelegate.this.onTimeChanged();
                }
            });
            this.mAmPmSpinnerInput = (EditText) this.mAmPmSpinner.findViewById(R.id.numberpicker_input);
            this.mAmPmSpinnerInput.setImeOptions(6);
        }
        if (isAmPmAtStart()) {
            ViewGroup amPmParent = (ViewGroup) delegator.findViewById(R.id.timePickerLayout);
            amPmParent.removeView(amPmView);
            amPmParent.addView(amPmView, 0);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) amPmView.getLayoutParams();
            int startMargin = lp.getMarginStart();
            int endMargin = lp.getMarginEnd();
            if (startMargin != endMargin) {
                lp.setMarginStart(endMargin);
                lp.setMarginEnd(startMargin);
            }
        }
        getHourFormatData();
        updateHourControl();
        updateMinuteControl();
        updateAmPmControl();
        setCurrentHour(Integer.valueOf(this.mTempCalendar.get(11)));
        setCurrentMinute(Integer.valueOf(this.mTempCalendar.get(12)));
        if (!isEnabled()) {
            setEnabled(false);
        }
        setContentDescriptions();
        if (this.mDelegator.getImportantForAccessibility() == 0) {
            this.mDelegator.setImportantForAccessibility(1);
        }
    }

    private void getHourFormatData() {
        String bestDateTimePattern = DateFormat.getBestDateTimePattern(this.mCurrentLocale, this.mIs24HourView ? "Hm" : "hm");
        int lengthPattern = bestDateTimePattern.length();
        this.mHourWithTwoDigit = false;
        for (int i = 0; i < lengthPattern; i++) {
            char c = bestDateTimePattern.charAt(i);
            if (c == 'H' || c == 'h' || c == 'K' || c == 'k') {
                this.mHourFormat = c;
                if (i + 1 < lengthPattern && c == bestDateTimePattern.charAt(i + 1)) {
                    this.mHourWithTwoDigit = true;
                    return;
                }
                return;
            }
        }
    }

    private boolean isAmPmAtStart() {
        String bestDateTimePattern = DateFormat.getBestDateTimePattern(this.mCurrentLocale, "hm");
        return bestDateTimePattern.startsWith(FullBackup.APK_TREE_TOKEN);
    }

    private void setDividerText() {
        String separatorText;
        String skeleton = this.mIs24HourView ? "Hm" : "hm";
        String bestDateTimePattern = DateFormat.getBestDateTimePattern(this.mCurrentLocale, skeleton);
        int hourIndex = bestDateTimePattern.lastIndexOf(72);
        if (hourIndex == -1) {
            hourIndex = bestDateTimePattern.lastIndexOf(104);
        }
        if (hourIndex == -1) {
            separatorText = ":";
        } else {
            int minuteIndex = bestDateTimePattern.indexOf(109, hourIndex + 1);
            if (minuteIndex == -1) {
                separatorText = Character.toString(bestDateTimePattern.charAt(hourIndex + 1));
            } else {
                separatorText = bestDateTimePattern.substring(hourIndex + 1, minuteIndex);
            }
        }
        this.mDivider.setText(separatorText);
    }

    @Override
    public void setCurrentHour(Integer currentHour) {
        setCurrentHour(currentHour, true);
    }

    private void setCurrentHour(Integer currentHour, boolean notifyTimeChanged) {
        if (currentHour != null && currentHour != getCurrentHour()) {
            if (!is24HourView()) {
                if (currentHour.intValue() >= 12) {
                    this.mIsAm = false;
                    if (currentHour.intValue() > 12) {
                        currentHour = Integer.valueOf(currentHour.intValue() - 12);
                    }
                } else {
                    this.mIsAm = true;
                    if (currentHour.intValue() == 0) {
                        currentHour = 12;
                    }
                }
                updateAmPmControl();
            }
            this.mHourSpinner.setValue(currentHour.intValue());
            if (notifyTimeChanged) {
                onTimeChanged();
            }
        }
    }

    @Override
    public Integer getCurrentHour() {
        int currentHour = this.mHourSpinner.getValue();
        if (is24HourView()) {
            return Integer.valueOf(currentHour);
        }
        if (this.mIsAm) {
            return Integer.valueOf(currentHour % 12);
        }
        return Integer.valueOf((currentHour % 12) + 12);
    }

    @Override
    public void setCurrentMinute(Integer currentMinute) {
        if (currentMinute != getCurrentMinute()) {
            this.mMinuteSpinner.setValue(currentMinute.intValue());
            onTimeChanged();
        }
    }

    @Override
    public Integer getCurrentMinute() {
        return Integer.valueOf(this.mMinuteSpinner.getValue());
    }

    @Override
    public void setIs24HourView(Boolean is24HourView) {
        if (this.mIs24HourView != is24HourView.booleanValue()) {
            int currentHour = getCurrentHour().intValue();
            this.mIs24HourView = is24HourView.booleanValue();
            getHourFormatData();
            updateHourControl();
            setCurrentHour(Integer.valueOf(currentHour), false);
            updateMinuteControl();
            updateAmPmControl();
        }
    }

    @Override
    public boolean is24HourView() {
        return this.mIs24HourView;
    }

    @Override
    public void setOnTimeChangedListener(TimePicker.OnTimeChangedListener onTimeChangedListener) {
        this.mOnTimeChangedListener = onTimeChangedListener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.mMinuteSpinner.setEnabled(enabled);
        if (this.mDivider != null) {
            this.mDivider.setEnabled(enabled);
        }
        this.mHourSpinner.setEnabled(enabled);
        if (this.mAmPmSpinner != null) {
            this.mAmPmSpinner.setEnabled(enabled);
        } else {
            this.mAmPmButton.setEnabled(enabled);
        }
        this.mIsEnabled = enabled;
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
    public void onConfigurationChanged(Configuration newConfig) {
        setCurrentLocale(newConfig.locale);
    }

    @Override
    public Parcelable onSaveInstanceState(Parcelable superState) {
        return new SavedState(superState, getCurrentHour().intValue(), getCurrentMinute().intValue());
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        setCurrentHour(Integer.valueOf(ss.getHour()));
        setCurrentMinute(Integer.valueOf(ss.getMinute()));
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        int flags;
        if (this.mIs24HourView) {
            flags = 1 | 128;
        } else {
            flags = 1 | 64;
        }
        this.mTempCalendar.set(11, getCurrentHour().intValue());
        this.mTempCalendar.set(12, getCurrentMinute().intValue());
        String selectedDateUtterance = DateUtils.formatDateTime(this.mContext, this.mTempCalendar.getTimeInMillis(), flags);
        event.getText().add(selectedDateUtterance);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        event.setClassName(TimePicker.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        info.setClassName(TimePicker.class.getName());
    }

    private void updateInputState() {
        InputMethodManager inputMethodManager = InputMethodManager.peekInstance();
        if (inputMethodManager != null) {
            if (inputMethodManager.isActive(this.mHourSpinnerInput)) {
                this.mHourSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(this.mDelegator.getWindowToken(), 0);
            } else if (inputMethodManager.isActive(this.mMinuteSpinnerInput)) {
                this.mMinuteSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(this.mDelegator.getWindowToken(), 0);
            } else if (inputMethodManager.isActive(this.mAmPmSpinnerInput)) {
                this.mAmPmSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(this.mDelegator.getWindowToken(), 0);
            }
        }
    }

    private void updateAmPmControl() {
        if (is24HourView()) {
            if (this.mAmPmSpinner != null) {
                this.mAmPmSpinner.setVisibility(8);
            } else {
                this.mAmPmButton.setVisibility(8);
            }
        } else {
            int index = this.mIsAm ? 0 : 1;
            if (this.mAmPmSpinner != null) {
                this.mAmPmSpinner.setValue(index);
                this.mAmPmSpinner.setVisibility(0);
            } else {
                this.mAmPmButton.setText(this.mAmPmStrings[index]);
                this.mAmPmButton.setVisibility(0);
            }
        }
        this.mDelegator.sendAccessibilityEvent(4);
    }

    @Override
    public void setCurrentLocale(Locale locale) {
        super.setCurrentLocale(locale);
        this.mTempCalendar = Calendar.getInstance(locale);
    }

    private void onTimeChanged() {
        this.mDelegator.sendAccessibilityEvent(4);
        if (this.mOnTimeChangedListener != null) {
            this.mOnTimeChangedListener.onTimeChanged(this.mDelegator, getCurrentHour().intValue(), getCurrentMinute().intValue());
        }
    }

    private void updateHourControl() {
        if (is24HourView()) {
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
        if (is24HourView()) {
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

    private void trySetContentDescription(View root, int viewId, int contDescResId) {
        View target = root.findViewById(viewId);
        if (target != null) {
            target.setContentDescription(this.mContext.getString(contDescResId));
        }
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
        private final int mHour;
        private final int mMinute;

        private SavedState(Parcelable superState, int hour, int minute) {
            super(superState);
            this.mHour = hour;
            this.mMinute = minute;
        }

        private SavedState(Parcel in) {
            super(in);
            this.mHour = in.readInt();
            this.mMinute = in.readInt();
        }

        public int getHour() {
            return this.mHour;
        }

        public int getMinute() {
            return this.mMinute;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.mHour);
            dest.writeInt(this.mMinute);
        }
    }

    public static String[] getAmPmStrings(Context context) {
        String[] result = new String[2];
        LocaleData d = LocaleData.get(context.getResources().getConfiguration().locale);
        result[0] = d.amPm[0].length() > 2 ? d.narrowAm : d.amPm[0];
        result[1] = d.amPm[1].length() > 2 ? d.narrowPm : d.amPm[1];
        return result;
    }
}
