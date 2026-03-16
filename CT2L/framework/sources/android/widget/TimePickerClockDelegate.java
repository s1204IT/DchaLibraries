package android.widget;

import android.app.backup.FullBackup;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RadialTimePickerView;
import android.widget.TimePicker;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;

class TimePickerClockDelegate extends TimePicker.AbstractTimePickerDelegate implements RadialTimePickerView.OnValueSelectedListener {
    static final int AM = 0;
    private static final int AMPM_INDEX = 2;
    private static final boolean DEFAULT_ENABLED_STATE = true;
    private static final int ENABLE_PICKER_INDEX = 3;
    private static final int HOURS_IN_HALF_DAY = 12;
    private static final int HOUR_INDEX = 0;
    private static final int MINUTE_INDEX = 1;
    static final int PM = 1;
    private static final String TAG = "TimePickerClockDelegate";
    private boolean mAllowAutoAdvance;
    private int mAmKeyCode;
    private final CheckedTextView mAmLabel;
    private final View mAmPmLayout;
    private final String mAmText;
    private final View.OnClickListener mClickListener;
    private String mDeletedKeyFormat;
    private final float mDisabledAlpha;
    private String mDoublePlaceholderText;
    private final View.OnFocusChangeListener mFocusListener;
    private final View mHeaderView;
    private final TextView mHourView;
    private boolean mInKbMode;
    private int mInitialHourOfDay;
    private int mInitialMinute;
    private boolean mIs24HourView;
    private boolean mIsEnabled;
    private final View.OnKeyListener mKeyListener;
    private boolean mLastAnnouncedIsHour;
    private CharSequence mLastAnnouncedText;
    private Node mLegalTimesTree;
    private final TextView mMinuteView;
    private char mPlaceholderText;
    private int mPmKeyCode;
    private final CheckedTextView mPmLabel;
    private final String mPmText;
    private final RadialTimePickerView mRadialTimePickerView;
    private String mSelectHours;
    private String mSelectMinutes;
    private final TextView mSeparatorView;
    private Calendar mTempCalendar;
    private ArrayList<Integer> mTypedTimes;

    public TimePickerClockDelegate(TimePicker delegator, Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(delegator, context);
        this.mIsEnabled = true;
        this.mTypedTimes = new ArrayList<>();
        this.mClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.hours:
                        TimePickerClockDelegate.this.setCurrentItemShowing(0, true, true);
                        break;
                    case R.id.separator:
                    case R.id.ampm_layout:
                    default:
                        return;
                    case R.id.minutes:
                        TimePickerClockDelegate.this.setCurrentItemShowing(1, true, true);
                        break;
                    case R.id.am_label:
                        TimePickerClockDelegate.this.setAmOrPm(0);
                        break;
                    case R.id.pm_label:
                        TimePickerClockDelegate.this.setAmOrPm(1);
                        break;
                }
                TimePickerClockDelegate.this.tryVibrate();
            }
        };
        this.mKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == 1) {
                    return TimePickerClockDelegate.this.processKeyUp(keyCode);
                }
                return false;
            }
        };
        this.mFocusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && TimePickerClockDelegate.this.mInKbMode && TimePickerClockDelegate.this.isTypedTimeFullyLegal()) {
                    TimePickerClockDelegate.this.finishKbMode();
                    if (TimePickerClockDelegate.this.mOnTimeChangedListener != null) {
                        TimePickerClockDelegate.this.mOnTimeChangedListener.onTimeChanged(TimePickerClockDelegate.this.mDelegator, TimePickerClockDelegate.this.mRadialTimePickerView.getCurrentHour(), TimePickerClockDelegate.this.mRadialTimePickerView.getCurrentMinute());
                    }
                }
            }
        };
        TypedArray a = this.mContext.obtainStyledAttributes(attrs, R.styleable.TimePicker, defStyleAttr, defStyleRes);
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Resources res = this.mContext.getResources();
        this.mSelectHours = res.getString(R.string.select_hours);
        this.mSelectMinutes = res.getString(R.string.select_minutes);
        String[] amPmStrings = TimePickerSpinnerDelegate.getAmPmStrings(context);
        this.mAmText = amPmStrings[0];
        this.mPmText = amPmStrings[1];
        int layoutResourceId = a.getResourceId(9, R.layout.time_picker_holo);
        View mainView = inflater.inflate(layoutResourceId, delegator);
        this.mHeaderView = mainView.findViewById(R.id.time_header);
        this.mHeaderView.setBackground(a.getDrawable(0));
        this.mHourView = (TextView) this.mHeaderView.findViewById(R.id.hours);
        this.mHourView.setOnClickListener(this.mClickListener);
        this.mHourView.setAccessibilityDelegate(new ClickActionDelegate(context, R.string.select_hours));
        this.mSeparatorView = (TextView) this.mHeaderView.findViewById(R.id.separator);
        this.mMinuteView = (TextView) this.mHeaderView.findViewById(R.id.minutes);
        this.mMinuteView.setOnClickListener(this.mClickListener);
        this.mMinuteView.setAccessibilityDelegate(new ClickActionDelegate(context, R.string.select_minutes));
        int headerTimeTextAppearance = a.getResourceId(1, 0);
        if (headerTimeTextAppearance != 0) {
            this.mHourView.setTextAppearance(context, headerTimeTextAppearance);
            this.mSeparatorView.setTextAppearance(context, headerTimeTextAppearance);
            this.mMinuteView.setTextAppearance(context, headerTimeTextAppearance);
        }
        this.mHourView.setMinWidth(computeStableWidth(this.mHourView, 24));
        this.mMinuteView.setMinWidth(computeStableWidth(this.mMinuteView, 60));
        int headerSelectedTextColor = a.getColor(11, res.getColor(R.color.timepicker_default_selector_color_material));
        this.mHourView.setTextColor(ColorStateList.addFirstIfMissing(this.mHourView.getTextColors(), 16842913, headerSelectedTextColor));
        this.mMinuteView.setTextColor(ColorStateList.addFirstIfMissing(this.mMinuteView.getTextColors(), 16842913, headerSelectedTextColor));
        this.mAmPmLayout = this.mHeaderView.findViewById(R.id.ampm_layout);
        this.mAmLabel = (CheckedTextView) this.mAmPmLayout.findViewById(R.id.am_label);
        this.mAmLabel.setText(amPmStrings[0]);
        this.mAmLabel.setOnClickListener(this.mClickListener);
        this.mPmLabel = (CheckedTextView) this.mAmPmLayout.findViewById(R.id.pm_label);
        this.mPmLabel.setText(amPmStrings[1]);
        this.mPmLabel.setOnClickListener(this.mClickListener);
        int headerAmPmTextAppearance = a.getResourceId(2, 0);
        if (headerAmPmTextAppearance != 0) {
            this.mAmLabel.setTextAppearance(context, headerAmPmTextAppearance);
            this.mPmLabel.setTextAppearance(context, headerAmPmTextAppearance);
        }
        a.recycle();
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(16842803, outValue, true);
        this.mDisabledAlpha = outValue.getFloat();
        this.mRadialTimePickerView = (RadialTimePickerView) mainView.findViewById(R.id.radial_picker);
        setupListeners();
        this.mAllowAutoAdvance = true;
        this.mDoublePlaceholderText = res.getString(R.string.time_placeholder);
        this.mDeletedKeyFormat = res.getString(R.string.deleted_key);
        this.mPlaceholderText = this.mDoublePlaceholderText.charAt(0);
        this.mPmKeyCode = -1;
        this.mAmKeyCode = -1;
        generateLegalTimesTree();
        Calendar calendar = Calendar.getInstance(this.mCurrentLocale);
        int currentHour = calendar.get(11);
        int currentMinute = calendar.get(12);
        initialize(currentHour, currentMinute, false, 0);
    }

    private static class ClickActionDelegate extends View.AccessibilityDelegate {
        private final AccessibilityNodeInfo.AccessibilityAction mClickAction;

        public ClickActionDelegate(Context context, int resId) {
            this.mClickAction = new AccessibilityNodeInfo.AccessibilityAction(16, context.getString(resId));
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.addAction(this.mClickAction);
        }
    }

    private int computeStableWidth(TextView v, int maxNumber) {
        int maxWidth = 0;
        for (int i = 0; i < maxNumber; i++) {
            String text = String.format("%02d", Integer.valueOf(i));
            v.setText(text);
            v.measure(0, 0);
            int width = v.getMeasuredWidth();
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth;
    }

    private void initialize(int hourOfDay, int minute, boolean is24HourView, int index) {
        this.mInitialHourOfDay = hourOfDay;
        this.mInitialMinute = minute;
        this.mIs24HourView = is24HourView;
        this.mInKbMode = false;
        updateUI(index);
    }

    private void setupListeners() {
        this.mHeaderView.setOnKeyListener(this.mKeyListener);
        this.mHeaderView.setOnFocusChangeListener(this.mFocusListener);
        this.mHeaderView.setFocusable(true);
        this.mRadialTimePickerView.setOnValueSelectedListener(this);
    }

    private void updateUI(int index) {
        updateRadialPicker(index);
        updateHeaderAmPm();
        updateHeaderHour(this.mInitialHourOfDay, false);
        updateHeaderSeparator();
        updateHeaderMinute(this.mInitialMinute, false);
        this.mDelegator.invalidate();
    }

    private void updateRadialPicker(int index) {
        this.mRadialTimePickerView.initialize(this.mInitialHourOfDay, this.mInitialMinute, this.mIs24HourView);
        setCurrentItemShowing(index, false, true);
    }

    private void updateHeaderAmPm() {
        if (this.mIs24HourView) {
            this.mAmPmLayout.setVisibility(8);
            return;
        }
        String dateTimePattern = DateFormat.getBestDateTimePattern(this.mCurrentLocale, "hm");
        boolean amPmAtStart = dateTimePattern.startsWith(FullBackup.APK_TREE_TOKEN);
        ViewGroup parent = (ViewGroup) this.mAmPmLayout.getParent();
        int targetIndex = amPmAtStart ? 0 : parent.getChildCount() - 1;
        int currentIndex = parent.indexOfChild(this.mAmPmLayout);
        if (targetIndex != currentIndex) {
            parent.removeView(this.mAmPmLayout);
            parent.addView(this.mAmPmLayout, targetIndex);
        }
        updateAmPmLabelStates(this.mInitialHourOfDay >= 12 ? 1 : 0);
    }

    @Override
    public void setCurrentHour(Integer currentHour) {
        if (this.mInitialHourOfDay != currentHour.intValue()) {
            this.mInitialHourOfDay = currentHour.intValue();
            updateHeaderHour(currentHour.intValue(), true);
            updateHeaderAmPm();
            this.mRadialTimePickerView.setCurrentHour(currentHour.intValue());
            this.mRadialTimePickerView.setAmOrPm(this.mInitialHourOfDay < 12 ? 0 : 1);
            this.mDelegator.invalidate();
            onTimeChanged();
        }
    }

    @Override
    public Integer getCurrentHour() {
        int currentHour = this.mRadialTimePickerView.getCurrentHour();
        if (this.mIs24HourView) {
            return Integer.valueOf(currentHour);
        }
        switch (this.mRadialTimePickerView.getAmOrPm()) {
            case 1:
                return Integer.valueOf((currentHour % 12) + 12);
            default:
                return Integer.valueOf(currentHour % 12);
        }
    }

    @Override
    public void setCurrentMinute(Integer currentMinute) {
        if (this.mInitialMinute != currentMinute.intValue()) {
            this.mInitialMinute = currentMinute.intValue();
            updateHeaderMinute(currentMinute.intValue(), true);
            this.mRadialTimePickerView.setCurrentMinute(currentMinute.intValue());
            this.mDelegator.invalidate();
            onTimeChanged();
        }
    }

    @Override
    public Integer getCurrentMinute() {
        return Integer.valueOf(this.mRadialTimePickerView.getCurrentMinute());
    }

    @Override
    public void setIs24HourView(Boolean is24HourView) {
        if (is24HourView.booleanValue() != this.mIs24HourView) {
            this.mIs24HourView = is24HourView.booleanValue();
            generateLegalTimesTree();
            int hour = this.mRadialTimePickerView.getCurrentHour();
            this.mInitialHourOfDay = hour;
            updateHeaderHour(hour, false);
            updateHeaderAmPm();
            updateRadialPicker(this.mRadialTimePickerView.getCurrentItemShowing());
            this.mDelegator.invalidate();
        }
    }

    @Override
    public boolean is24HourView() {
        return this.mIs24HourView;
    }

    @Override
    public void setOnTimeChangedListener(TimePicker.OnTimeChangedListener callback) {
        this.mOnTimeChangedListener = callback;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.mHourView.setEnabled(enabled);
        this.mMinuteView.setEnabled(enabled);
        this.mAmLabel.setEnabled(enabled);
        this.mPmLabel.setEnabled(enabled);
        this.mRadialTimePickerView.setEnabled(enabled);
        this.mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    @Override
    public int getBaseline() {
        return -1;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        updateUI(this.mRadialTimePickerView.getCurrentItemShowing());
    }

    @Override
    public Parcelable onSaveInstanceState(Parcelable superState) {
        return new SavedState(superState, getCurrentHour().intValue(), getCurrentMinute().intValue(), is24HourView(), inKbMode(), getTypedTimes(), getCurrentItemShowing());
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        setInKbMode(ss.inKbMode());
        setTypedTimes(ss.getTypesTimes());
        initialize(ss.getHour(), ss.getMinute(), ss.is24HourMode(), ss.getCurrentItemShowing());
        this.mRadialTimePickerView.invalidate();
        if (this.mInKbMode) {
            tryStartingKbMode(-1);
            this.mHourView.invalidate();
        }
    }

    @Override
    public void setCurrentLocale(Locale locale) {
        super.setCurrentLocale(locale);
        this.mTempCalendar = Calendar.getInstance(locale);
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
        String selectedDate = DateUtils.formatDateTime(this.mContext, this.mTempCalendar.getTimeInMillis(), flags);
        event.getText().add(selectedDate);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        event.setClassName(TimePicker.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        info.setClassName(TimePicker.class.getName());
    }

    private void setInKbMode(boolean inKbMode) {
        this.mInKbMode = inKbMode;
    }

    private boolean inKbMode() {
        return this.mInKbMode;
    }

    private void setTypedTimes(ArrayList<Integer> typeTimes) {
        this.mTypedTimes = typeTimes;
    }

    private ArrayList<Integer> getTypedTimes() {
        return this.mTypedTimes;
    }

    private int getCurrentItemShowing() {
        return this.mRadialTimePickerView.getCurrentItemShowing();
    }

    private void onTimeChanged() {
        this.mDelegator.sendAccessibilityEvent(4);
        if (this.mOnTimeChangedListener != null) {
            this.mOnTimeChangedListener.onTimeChanged(this.mDelegator, getCurrentHour().intValue(), getCurrentMinute().intValue());
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
        private final int mCurrentItemShowing;
        private final int mHour;
        private final boolean mInKbMode;
        private final boolean mIs24HourMode;
        private final int mMinute;
        private final ArrayList<Integer> mTypedTimes;

        private SavedState(Parcelable superState, int hour, int minute, boolean is24HourMode, boolean isKbMode, ArrayList<Integer> typedTimes, int currentItemShowing) {
            super(superState);
            this.mHour = hour;
            this.mMinute = minute;
            this.mIs24HourMode = is24HourMode;
            this.mInKbMode = isKbMode;
            this.mTypedTimes = typedTimes;
            this.mCurrentItemShowing = currentItemShowing;
        }

        private SavedState(Parcel in) {
            super(in);
            this.mHour = in.readInt();
            this.mMinute = in.readInt();
            this.mIs24HourMode = in.readInt() == 1;
            this.mInKbMode = in.readInt() == 1;
            this.mTypedTimes = in.readArrayList(getClass().getClassLoader());
            this.mCurrentItemShowing = in.readInt();
        }

        public int getHour() {
            return this.mHour;
        }

        public int getMinute() {
            return this.mMinute;
        }

        public boolean is24HourMode() {
            return this.mIs24HourMode;
        }

        public boolean inKbMode() {
            return this.mInKbMode;
        }

        public ArrayList<Integer> getTypesTimes() {
            return this.mTypedTimes;
        }

        public int getCurrentItemShowing() {
            return this.mCurrentItemShowing;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.mHour);
            dest.writeInt(this.mMinute);
            dest.writeInt(this.mIs24HourMode ? 1 : 0);
            dest.writeInt(this.mInKbMode ? 1 : 0);
            dest.writeList(this.mTypedTimes);
            dest.writeInt(this.mCurrentItemShowing);
        }
    }

    private void tryVibrate() {
        this.mDelegator.performHapticFeedback(4);
    }

    private void updateAmPmLabelStates(int amOrPm) {
        boolean isAm = amOrPm == 0;
        this.mAmLabel.setChecked(isAm);
        this.mAmLabel.setAlpha(isAm ? 1.0f : this.mDisabledAlpha);
        boolean isPm = amOrPm == 1;
        this.mPmLabel.setChecked(isPm);
        this.mPmLabel.setAlpha(isPm ? 1.0f : this.mDisabledAlpha);
    }

    @Override
    public void onValueSelected(int pickerIndex, int newValue, boolean autoAdvance) {
        switch (pickerIndex) {
            case 0:
                if (this.mAllowAutoAdvance && autoAdvance) {
                    updateHeaderHour(newValue, false);
                    setCurrentItemShowing(1, true, false);
                    this.mDelegator.announceForAccessibility(newValue + ". " + this.mSelectMinutes);
                } else {
                    updateHeaderHour(newValue, true);
                }
                break;
            case 1:
                updateHeaderMinute(newValue, true);
                break;
            case 2:
                updateAmPmLabelStates(newValue);
                break;
            case 3:
                if (!isTypedTimeFullyLegal()) {
                    this.mTypedTimes.clear();
                }
                finishKbMode();
                break;
        }
        if (this.mOnTimeChangedListener != null) {
            this.mOnTimeChangedListener.onTimeChanged(this.mDelegator, getCurrentHour().intValue(), getCurrentMinute().intValue());
        }
    }

    private void updateHeaderHour(int value, boolean announce) {
        String format;
        String bestDateTimePattern = DateFormat.getBestDateTimePattern(this.mCurrentLocale, this.mIs24HourView ? "Hm" : "hm");
        int lengthPattern = bestDateTimePattern.length();
        boolean hourWithTwoDigit = false;
        char hourFormat = 0;
        for (int i = 0; i < lengthPattern; i++) {
            char c = bestDateTimePattern.charAt(i);
            if (c == 'H' || c == 'h' || c == 'K' || c == 'k') {
                hourFormat = c;
                if (i + 1 < lengthPattern && c == bestDateTimePattern.charAt(i + 1)) {
                    hourWithTwoDigit = true;
                }
                if (!hourWithTwoDigit) {
                    format = "%02d";
                } else {
                    format = "%d";
                }
                if (!this.mIs24HourView) {
                    if (hourFormat == 'k' && value == 0) {
                        value = 24;
                    }
                } else {
                    value = modulo12(value, hourFormat == 'K');
                }
                CharSequence text = String.format(format, Integer.valueOf(value));
                this.mHourView.setText(text);
                if (!announce) {
                    tryAnnounceForAccessibility(text, true);
                    return;
                }
                return;
            }
        }
        if (!hourWithTwoDigit) {
        }
        if (!this.mIs24HourView) {
        }
        CharSequence text2 = String.format(format, Integer.valueOf(value));
        this.mHourView.setText(text2);
        if (!announce) {
        }
    }

    private void tryAnnounceForAccessibility(CharSequence text, boolean isHour) {
        if (this.mLastAnnouncedIsHour != isHour || !text.equals(this.mLastAnnouncedText)) {
            this.mDelegator.announceForAccessibility(text);
            this.mLastAnnouncedText = text;
            this.mLastAnnouncedIsHour = isHour;
        }
    }

    private static int modulo12(int n, boolean startWithZero) {
        int value = n % 12;
        if (value == 0 && !startWithZero) {
            return 12;
        }
        return value;
    }

    private void updateHeaderSeparator() {
        String separatorText;
        String bestDateTimePattern = DateFormat.getBestDateTimePattern(this.mCurrentLocale, this.mIs24HourView ? "Hm" : "hm");
        char[] hourFormats = {'H', DateFormat.HOUR, 'K', DateFormat.HOUR_OF_DAY};
        int hIndex = lastIndexOfAny(bestDateTimePattern, hourFormats);
        if (hIndex == -1) {
            separatorText = ":";
        } else {
            separatorText = Character.toString(bestDateTimePattern.charAt(hIndex + 1));
        }
        this.mSeparatorView.setText(separatorText);
    }

    private static int lastIndexOfAny(String str, char[] any) {
        int lengthAny = any.length;
        if (lengthAny > 0) {
            for (int i = str.length() - 1; i >= 0; i--) {
                char c = str.charAt(i);
                for (char c2 : any) {
                    if (c == c2) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private void updateHeaderMinute(int value, boolean announceForAccessibility) {
        if (value == 60) {
            value = 0;
        }
        CharSequence text = String.format(this.mCurrentLocale, "%02d", Integer.valueOf(value));
        this.mMinuteView.setText(text);
        if (announceForAccessibility) {
            tryAnnounceForAccessibility(text, false);
        }
    }

    private void setCurrentItemShowing(int index, boolean animateCircle, boolean announce) {
        this.mRadialTimePickerView.setCurrentItemShowing(index, animateCircle);
        if (index == 0) {
            if (announce) {
                this.mDelegator.announceForAccessibility(this.mSelectHours);
            }
        } else if (announce) {
            this.mDelegator.announceForAccessibility(this.mSelectMinutes);
        }
        this.mHourView.setSelected(index == 0);
        this.mMinuteView.setSelected(index == 1);
    }

    private void setAmOrPm(int amOrPm) {
        updateAmPmLabelStates(amOrPm);
        this.mRadialTimePickerView.setAmOrPm(amOrPm);
    }

    private boolean processKeyUp(int keyCode) {
        String deletedKeyStr;
        if (keyCode == 67) {
            if (this.mInKbMode && !this.mTypedTimes.isEmpty()) {
                int deleted = deleteLastTypedKey();
                if (deleted == getAmOrPmKeyCode(0)) {
                    deletedKeyStr = this.mAmText;
                } else if (deleted == getAmOrPmKeyCode(1)) {
                    deletedKeyStr = this.mPmText;
                } else {
                    deletedKeyStr = String.format("%d", Integer.valueOf(getValFromKeyCode(deleted)));
                }
                this.mDelegator.announceForAccessibility(String.format(this.mDeletedKeyFormat, deletedKeyStr));
                updateDisplay(true);
            }
        } else if (keyCode == 7 || keyCode == 8 || keyCode == 9 || keyCode == 10 || keyCode == 11 || keyCode == 12 || keyCode == 13 || keyCode == 14 || keyCode == 15 || keyCode == 16 || (!this.mIs24HourView && (keyCode == getAmOrPmKeyCode(0) || keyCode == getAmOrPmKeyCode(1)))) {
            if (!this.mInKbMode) {
                if (this.mRadialTimePickerView == null) {
                    Log.e(TAG, "Unable to initiate keyboard mode, TimePicker was null.");
                    return true;
                }
                this.mTypedTimes.clear();
                tryStartingKbMode(keyCode);
                return true;
            }
            if (!addKeyIfLegal(keyCode)) {
                return true;
            }
            updateDisplay(false);
            return true;
        }
        return false;
    }

    private void tryStartingKbMode(int keyCode) {
        if (keyCode == -1 || addKeyIfLegal(keyCode)) {
            this.mInKbMode = true;
            onValidationChanged(false);
            updateDisplay(false);
            this.mRadialTimePickerView.setInputEnabled(false);
        }
    }

    private boolean addKeyIfLegal(int keyCode) {
        if (this.mIs24HourView && this.mTypedTimes.size() == 4) {
            return false;
        }
        if (!this.mIs24HourView && isTypedTimeFullyLegal()) {
            return false;
        }
        this.mTypedTimes.add(Integer.valueOf(keyCode));
        if (!isTypedTimeLegalSoFar()) {
            deleteLastTypedKey();
            return false;
        }
        int val = getValFromKeyCode(keyCode);
        this.mDelegator.announceForAccessibility(String.format("%d", Integer.valueOf(val)));
        if (isTypedTimeFullyLegal()) {
            if (!this.mIs24HourView && this.mTypedTimes.size() <= 3) {
                this.mTypedTimes.add(this.mTypedTimes.size() - 1, 7);
                this.mTypedTimes.add(this.mTypedTimes.size() - 1, 7);
            }
            onValidationChanged(true);
        }
        return true;
    }

    private boolean isTypedTimeLegalSoFar() {
        Node node = this.mLegalTimesTree;
        Iterator<Integer> it = this.mTypedTimes.iterator();
        while (it.hasNext()) {
            int keyCode = it.next().intValue();
            node = node.canReach(keyCode);
            if (node == null) {
                return false;
            }
        }
        return true;
    }

    private boolean isTypedTimeFullyLegal() {
        if (this.mIs24HourView) {
            int[] values = getEnteredTime(null);
            return values[0] >= 0 && values[1] >= 0 && values[1] < 60;
        }
        return this.mTypedTimes.contains(Integer.valueOf(getAmOrPmKeyCode(0))) || this.mTypedTimes.contains(Integer.valueOf(getAmOrPmKeyCode(1)));
    }

    private int deleteLastTypedKey() {
        int deleted = this.mTypedTimes.remove(this.mTypedTimes.size() - 1).intValue();
        if (!isTypedTimeFullyLegal()) {
            onValidationChanged(false);
        }
        return deleted;
    }

    private void finishKbMode() {
        this.mInKbMode = false;
        if (!this.mTypedTimes.isEmpty()) {
            int[] values = getEnteredTime(null);
            this.mRadialTimePickerView.setCurrentHour(values[0]);
            this.mRadialTimePickerView.setCurrentMinute(values[1]);
            if (!this.mIs24HourView) {
                this.mRadialTimePickerView.setAmOrPm(values[2]);
            }
            this.mTypedTimes.clear();
        }
        updateDisplay(false);
        this.mRadialTimePickerView.setInputEnabled(true);
    }

    private void updateDisplay(boolean allowEmptyDisplay) {
        if (!allowEmptyDisplay && this.mTypedTimes.isEmpty()) {
            int hour = this.mRadialTimePickerView.getCurrentHour();
            int minute = this.mRadialTimePickerView.getCurrentMinute();
            updateHeaderHour(hour, false);
            updateHeaderMinute(minute, false);
            if (!this.mIs24HourView) {
                updateAmPmLabelStates(hour < 12 ? 0 : 1);
            }
            setCurrentItemShowing(this.mRadialTimePickerView.getCurrentItemShowing(), true, true);
            onValidationChanged(true);
            return;
        }
        boolean[] enteredZeros = {false, false};
        int[] values = getEnteredTime(enteredZeros);
        String hourFormat = enteredZeros[0] ? "%02d" : "%2d";
        String minuteFormat = enteredZeros[1] ? "%02d" : "%2d";
        String hourStr = values[0] == -1 ? this.mDoublePlaceholderText : String.format(hourFormat, Integer.valueOf(values[0])).replace(' ', this.mPlaceholderText);
        String minuteStr = values[1] == -1 ? this.mDoublePlaceholderText : String.format(minuteFormat, Integer.valueOf(values[1])).replace(' ', this.mPlaceholderText);
        this.mHourView.setText(hourStr);
        this.mHourView.setSelected(false);
        this.mMinuteView.setText(minuteStr);
        this.mMinuteView.setSelected(false);
        if (!this.mIs24HourView) {
            updateAmPmLabelStates(values[2]);
        }
    }

    private int getValFromKeyCode(int keyCode) {
        switch (keyCode) {
            case 7:
                return 0;
            case 8:
                return 1;
            case 9:
                return 2;
            case 10:
                return 3;
            case 11:
                return 4;
            case 12:
                return 5;
            case 13:
                return 6;
            case 14:
                return 7;
            case 15:
                return 8;
            case 16:
                return 9;
            default:
                return -1;
        }
    }

    private int[] getEnteredTime(boolean[] enteredZeros) {
        int amOrPm = -1;
        int startIndex = 1;
        if (!this.mIs24HourView && isTypedTimeFullyLegal()) {
            int keyCode = this.mTypedTimes.get(this.mTypedTimes.size() - 1).intValue();
            if (keyCode == getAmOrPmKeyCode(0)) {
                amOrPm = 0;
            } else if (keyCode == getAmOrPmKeyCode(1)) {
                amOrPm = 1;
            }
            startIndex = 2;
        }
        int minute = -1;
        int hour = -1;
        for (int i = startIndex; i <= this.mTypedTimes.size(); i++) {
            int val = getValFromKeyCode(this.mTypedTimes.get(this.mTypedTimes.size() - i).intValue());
            if (i == startIndex) {
                minute = val;
            } else if (i == startIndex + 1) {
                minute += val * 10;
                if (enteredZeros != null && val == 0) {
                    enteredZeros[1] = true;
                }
            } else if (i == startIndex + 2) {
                hour = val;
            } else if (i == startIndex + 3) {
                hour += val * 10;
                if (enteredZeros != null && val == 0) {
                    enteredZeros[0] = true;
                }
            }
        }
        return new int[]{hour, minute, amOrPm};
    }

    private int getAmOrPmKeyCode(int amOrPm) {
        if (this.mAmKeyCode == -1 || this.mPmKeyCode == -1) {
            KeyCharacterMap kcm = KeyCharacterMap.load(-1);
            CharSequence amText = this.mAmText.toLowerCase(this.mCurrentLocale);
            CharSequence pmText = this.mPmText.toLowerCase(this.mCurrentLocale);
            int N = Math.min(amText.length(), pmText.length());
            int i = 0;
            while (true) {
                if (i >= N) {
                    break;
                }
                char amChar = amText.charAt(i);
                char pmChar = pmText.charAt(i);
                if (amChar == pmChar) {
                    i++;
                } else {
                    KeyEvent[] events = kcm.getEvents(new char[]{amChar, pmChar});
                    if (events != null && events.length == 4) {
                        this.mAmKeyCode = events[0].getKeyCode();
                        this.mPmKeyCode = events[2].getKeyCode();
                    } else {
                        Log.e(TAG, "Unable to find keycodes for AM and PM.");
                    }
                }
            }
        }
        if (amOrPm == 0) {
            return this.mAmKeyCode;
        }
        if (amOrPm == 1) {
            return this.mPmKeyCode;
        }
        return -1;
    }

    private void generateLegalTimesTree() {
        this.mLegalTimesTree = new Node(new int[0]);
        if (this.mIs24HourView) {
            Node minuteFirstDigit = new Node(7, 8, 9, 10, 11, 12);
            Node minuteSecondDigit = new Node(7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
            minuteFirstDigit.addChild(minuteSecondDigit);
            Node firstDigit = new Node(7, 8);
            this.mLegalTimesTree.addChild(firstDigit);
            Node secondDigit = new Node(7, 8, 9, 10, 11, 12);
            firstDigit.addChild(secondDigit);
            secondDigit.addChild(minuteFirstDigit);
            secondDigit.addChild(new Node(13, 14, 15, 16));
            Node secondDigit2 = new Node(13, 14, 15, 16);
            firstDigit.addChild(secondDigit2);
            secondDigit2.addChild(minuteFirstDigit);
            Node firstDigit2 = new Node(9);
            this.mLegalTimesTree.addChild(firstDigit2);
            Node secondDigit3 = new Node(7, 8, 9, 10);
            firstDigit2.addChild(secondDigit3);
            secondDigit3.addChild(minuteFirstDigit);
            Node secondDigit4 = new Node(11, 12);
            firstDigit2.addChild(secondDigit4);
            secondDigit4.addChild(minuteSecondDigit);
            Node firstDigit3 = new Node(10, 11, 12, 13, 14, 15, 16);
            this.mLegalTimesTree.addChild(firstDigit3);
            firstDigit3.addChild(minuteFirstDigit);
            return;
        }
        Node ampm = new Node(getAmOrPmKeyCode(0), getAmOrPmKeyCode(1));
        Node firstDigit4 = new Node(8);
        this.mLegalTimesTree.addChild(firstDigit4);
        firstDigit4.addChild(ampm);
        Node secondDigit5 = new Node(7, 8, 9);
        firstDigit4.addChild(secondDigit5);
        secondDigit5.addChild(ampm);
        Node thirdDigit = new Node(7, 8, 9, 10, 11, 12);
        secondDigit5.addChild(thirdDigit);
        thirdDigit.addChild(ampm);
        Node fourthDigit = new Node(7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        thirdDigit.addChild(fourthDigit);
        fourthDigit.addChild(ampm);
        Node thirdDigit2 = new Node(13, 14, 15, 16);
        secondDigit5.addChild(thirdDigit2);
        thirdDigit2.addChild(ampm);
        Node secondDigit6 = new Node(10, 11, 12);
        firstDigit4.addChild(secondDigit6);
        Node thirdDigit3 = new Node(7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        secondDigit6.addChild(thirdDigit3);
        thirdDigit3.addChild(ampm);
        Node firstDigit5 = new Node(9, 10, 11, 12, 13, 14, 15, 16);
        this.mLegalTimesTree.addChild(firstDigit5);
        firstDigit5.addChild(ampm);
        Node secondDigit7 = new Node(7, 8, 9, 10, 11, 12);
        firstDigit5.addChild(secondDigit7);
        Node thirdDigit4 = new Node(7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        secondDigit7.addChild(thirdDigit4);
        thirdDigit4.addChild(ampm);
    }

    private class Node {
        private ArrayList<Node> mChildren = new ArrayList<>();
        private int[] mLegalKeys;

        public Node(int... legalKeys) {
            this.mLegalKeys = legalKeys;
        }

        public void addChild(Node child) {
            this.mChildren.add(child);
        }

        public boolean containsKey(int key) {
            for (int i = 0; i < this.mLegalKeys.length; i++) {
                if (this.mLegalKeys[i] == key) {
                    return true;
                }
            }
            return false;
        }

        public Node canReach(int key) {
            if (this.mChildren == null) {
                return null;
            }
            for (Node child : this.mChildren) {
                if (child.containsKey(key)) {
                    return child;
                }
            }
            return null;
        }
    }
}
