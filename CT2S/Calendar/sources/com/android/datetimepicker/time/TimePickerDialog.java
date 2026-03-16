package com.android.datetimepicker.time;

import android.animation.ObjectAnimator;
import android.app.DialogFragment;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.datetimepicker.HapticFeedbackController;
import com.android.datetimepicker.R;
import com.android.datetimepicker.Utils;
import com.android.datetimepicker.time.RadialPickerLayout;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class TimePickerDialog extends DialogFragment implements RadialPickerLayout.OnValueSelectedListener {
    private boolean mAllowAutoAdvance;
    private int mAmKeyCode;
    private View mAmPmHitspace;
    private TextView mAmPmTextView;
    private String mAmText;
    private OnTimeSetListener mCallback;
    private String mDeletedKeyFormat;
    private TextView mDoneButton;
    private String mDoublePlaceholderText;
    private HapticFeedbackController mHapticFeedbackController;
    private String mHourPickerDescription;
    private TextView mHourSpaceView;
    private TextView mHourView;
    private boolean mInKbMode;
    private int mInitialHourOfDay;
    private int mInitialMinute;
    private boolean mIs24HourMode;
    private Node mLegalTimesTree;
    private String mMinutePickerDescription;
    private TextView mMinuteSpaceView;
    private TextView mMinuteView;
    private char mPlaceholderText;
    private int mPmKeyCode;
    private String mPmText;
    private String mSelectHours;
    private String mSelectMinutes;
    private int mSelectedColor;
    private boolean mThemeDark;
    private RadialPickerLayout mTimePicker;
    private ArrayList<Integer> mTypedTimes;
    private int mUnselectedColor;

    public interface OnTimeSetListener {
        void onTimeSet(RadialPickerLayout radialPickerLayout, int i, int i2);
    }

    public static TimePickerDialog newInstance(OnTimeSetListener callback, int hourOfDay, int minute, boolean is24HourMode) {
        TimePickerDialog ret = new TimePickerDialog();
        ret.initialize(callback, hourOfDay, minute, is24HourMode);
        return ret;
    }

    public void initialize(OnTimeSetListener callback, int hourOfDay, int minute, boolean is24HourMode) {
        this.mCallback = callback;
        this.mInitialHourOfDay = hourOfDay;
        this.mInitialMinute = minute;
        this.mIs24HourMode = is24HourMode;
        this.mInKbMode = false;
        this.mThemeDark = false;
    }

    public void setOnTimeSetListener(OnTimeSetListener callback) {
        this.mCallback = callback;
    }

    public void setStartTime(int hourOfDay, int minute) {
        this.mInitialHourOfDay = hourOfDay;
        this.mInitialMinute = minute;
        this.mInKbMode = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("hour_of_day") && savedInstanceState.containsKey("minute") && savedInstanceState.containsKey("is_24_hour_view")) {
            this.mInitialHourOfDay = savedInstanceState.getInt("hour_of_day");
            this.mInitialMinute = savedInstanceState.getInt("minute");
            this.mIs24HourMode = savedInstanceState.getBoolean("is_24_hour_view");
            this.mInKbMode = savedInstanceState.getBoolean("in_kb_mode");
            this.mThemeDark = savedInstanceState.getBoolean("dark_theme");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(1);
        View view = inflater.inflate(R.layout.time_picker_dialog, (ViewGroup) null);
        KeyboardListener keyboardListener = new KeyboardListener();
        view.findViewById(R.id.time_picker_dialog).setOnKeyListener(keyboardListener);
        Resources res = getResources();
        this.mHourPickerDescription = res.getString(R.string.hour_picker_description);
        this.mSelectHours = res.getString(R.string.select_hours);
        this.mMinutePickerDescription = res.getString(R.string.minute_picker_description);
        this.mSelectMinutes = res.getString(R.string.select_minutes);
        this.mSelectedColor = res.getColor(this.mThemeDark ? R.color.red : R.color.blue);
        this.mUnselectedColor = res.getColor(this.mThemeDark ? R.color.white : R.color.numbers_text_color);
        this.mHourView = (TextView) view.findViewById(R.id.hours);
        this.mHourView.setOnKeyListener(keyboardListener);
        this.mHourSpaceView = (TextView) view.findViewById(R.id.hour_space);
        this.mMinuteSpaceView = (TextView) view.findViewById(R.id.minutes_space);
        this.mMinuteView = (TextView) view.findViewById(R.id.minutes);
        this.mMinuteView.setOnKeyListener(keyboardListener);
        this.mAmPmTextView = (TextView) view.findViewById(R.id.ampm_label);
        this.mAmPmTextView.setOnKeyListener(keyboardListener);
        String[] amPmTexts = new DateFormatSymbols().getAmPmStrings();
        this.mAmText = amPmTexts[0];
        this.mPmText = amPmTexts[1];
        this.mHapticFeedbackController = new HapticFeedbackController(getActivity());
        this.mTimePicker = (RadialPickerLayout) view.findViewById(R.id.time_picker);
        this.mTimePicker.setOnValueSelectedListener(this);
        this.mTimePicker.setOnKeyListener(keyboardListener);
        this.mTimePicker.initialize(getActivity(), this.mHapticFeedbackController, this.mInitialHourOfDay, this.mInitialMinute, this.mIs24HourMode);
        int currentItemShowing = 0;
        if (savedInstanceState != null && savedInstanceState.containsKey("current_item_showing")) {
            currentItemShowing = savedInstanceState.getInt("current_item_showing");
        }
        setCurrentItemShowing(currentItemShowing, false, true, true);
        this.mTimePicker.invalidate();
        this.mHourView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog.this.setCurrentItemShowing(0, true, false, true);
                TimePickerDialog.this.tryVibrate();
            }
        });
        this.mMinuteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog.this.setCurrentItemShowing(1, true, false, true);
                TimePickerDialog.this.tryVibrate();
            }
        });
        this.mDoneButton = (TextView) view.findViewById(R.id.done_button);
        this.mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TimePickerDialog.this.mInKbMode && TimePickerDialog.this.isTypedTimeFullyLegal()) {
                    TimePickerDialog.this.finishKbMode(false);
                } else {
                    TimePickerDialog.this.tryVibrate();
                }
                if (TimePickerDialog.this.mCallback != null) {
                    TimePickerDialog.this.mCallback.onTimeSet(TimePickerDialog.this.mTimePicker, TimePickerDialog.this.mTimePicker.getHours(), TimePickerDialog.this.mTimePicker.getMinutes());
                }
                TimePickerDialog.this.dismiss();
            }
        });
        this.mDoneButton.setOnKeyListener(keyboardListener);
        this.mAmPmHitspace = view.findViewById(R.id.ampm_hitspace);
        if (this.mIs24HourMode) {
            this.mAmPmTextView.setVisibility(8);
            RelativeLayout.LayoutParams paramsSeparator = new RelativeLayout.LayoutParams(-2, -2);
            paramsSeparator.addRule(13);
            TextView separatorView = (TextView) view.findViewById(R.id.separator);
            separatorView.setLayoutParams(paramsSeparator);
        } else {
            this.mAmPmTextView.setVisibility(0);
            updateAmPmDisplay(this.mInitialHourOfDay < 12 ? 0 : 1);
            this.mAmPmHitspace.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TimePickerDialog.this.tryVibrate();
                    int amOrPm = TimePickerDialog.this.mTimePicker.getIsCurrentlyAmOrPm();
                    if (amOrPm == 0) {
                        amOrPm = 1;
                    } else if (amOrPm == 1) {
                        amOrPm = 0;
                    }
                    TimePickerDialog.this.updateAmPmDisplay(amOrPm);
                    TimePickerDialog.this.mTimePicker.setAmOrPm(amOrPm);
                }
            });
        }
        this.mAllowAutoAdvance = true;
        setHour(this.mInitialHourOfDay, true);
        setMinute(this.mInitialMinute);
        this.mDoublePlaceholderText = res.getString(R.string.time_placeholder);
        this.mDeletedKeyFormat = res.getString(R.string.deleted_key);
        this.mPlaceholderText = this.mDoublePlaceholderText.charAt(0);
        this.mPmKeyCode = -1;
        this.mAmKeyCode = -1;
        generateLegalTimesTree();
        if (this.mInKbMode) {
            this.mTypedTimes = savedInstanceState.getIntegerArrayList("typed_times");
            tryStartingKbMode(-1);
            this.mHourView.invalidate();
        } else if (this.mTypedTimes == null) {
            this.mTypedTimes = new ArrayList<>();
        }
        this.mTimePicker.setTheme(getActivity().getApplicationContext(), this.mThemeDark);
        int white = res.getColor(R.color.white);
        int circleBackground = res.getColor(R.color.circle_background);
        int line = res.getColor(R.color.line_background);
        int timeDisplay = res.getColor(R.color.numbers_text_color);
        ColorStateList doneTextColor = res.getColorStateList(R.color.done_text_color);
        int doneBackground = R.drawable.done_background_color;
        int darkGray = res.getColor(R.color.dark_gray);
        int lightGray = res.getColor(R.color.light_gray);
        int darkLine = res.getColor(R.color.line_dark);
        ColorStateList darkDoneTextColor = res.getColorStateList(R.color.done_text_color_dark);
        int darkDoneBackground = R.drawable.done_background_color_dark;
        view.findViewById(R.id.time_display_background).setBackgroundColor(this.mThemeDark ? darkGray : white);
        View viewFindViewById = view.findViewById(R.id.time_display);
        if (!this.mThemeDark) {
            darkGray = white;
        }
        viewFindViewById.setBackgroundColor(darkGray);
        ((TextView) view.findViewById(R.id.separator)).setTextColor(this.mThemeDark ? white : timeDisplay);
        TextView textView = (TextView) view.findViewById(R.id.ampm_label);
        if (!this.mThemeDark) {
            white = timeDisplay;
        }
        textView.setTextColor(white);
        View viewFindViewById2 = view.findViewById(R.id.line);
        if (!this.mThemeDark) {
            darkLine = line;
        }
        viewFindViewById2.setBackgroundColor(darkLine);
        TextView textView2 = this.mDoneButton;
        if (!this.mThemeDark) {
            darkDoneTextColor = doneTextColor;
        }
        textView2.setTextColor(darkDoneTextColor);
        RadialPickerLayout radialPickerLayout = this.mTimePicker;
        if (!this.mThemeDark) {
            lightGray = circleBackground;
        }
        radialPickerLayout.setBackgroundColor(lightGray);
        TextView textView3 = this.mDoneButton;
        if (!this.mThemeDark) {
            darkDoneBackground = doneBackground;
        }
        textView3.setBackgroundResource(darkDoneBackground);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mHapticFeedbackController.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mHapticFeedbackController.stop();
    }

    public void tryVibrate() {
        this.mHapticFeedbackController.tryVibrate();
    }

    private void updateAmPmDisplay(int amOrPm) {
        if (amOrPm == 0) {
            this.mAmPmTextView.setText(this.mAmText);
            Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mAmText);
            this.mAmPmHitspace.setContentDescription(this.mAmText);
        } else {
            if (amOrPm == 1) {
                this.mAmPmTextView.setText(this.mPmText);
                Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mPmText);
                this.mAmPmHitspace.setContentDescription(this.mPmText);
                return;
            }
            this.mAmPmTextView.setText(this.mDoublePlaceholderText);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (this.mTimePicker != null) {
            outState.putInt("hour_of_day", this.mTimePicker.getHours());
            outState.putInt("minute", this.mTimePicker.getMinutes());
            outState.putBoolean("is_24_hour_view", this.mIs24HourMode);
            outState.putInt("current_item_showing", this.mTimePicker.getCurrentItemShowing());
            outState.putBoolean("in_kb_mode", this.mInKbMode);
            if (this.mInKbMode) {
                outState.putIntegerArrayList("typed_times", this.mTypedTimes);
            }
            outState.putBoolean("dark_theme", this.mThemeDark);
        }
    }

    @Override
    public void onValueSelected(int pickerIndex, int newValue, boolean autoAdvance) {
        if (pickerIndex == 0) {
            setHour(newValue, false);
            String announcement = String.format("%d", Integer.valueOf(newValue));
            if (this.mAllowAutoAdvance && autoAdvance) {
                setCurrentItemShowing(1, true, true, false);
                announcement = announcement + ". " + this.mSelectMinutes;
            } else {
                this.mTimePicker.setContentDescription(this.mHourPickerDescription + ": " + newValue);
            }
            Utils.tryAccessibilityAnnounce(this.mTimePicker, announcement);
            return;
        }
        if (pickerIndex == 1) {
            setMinute(newValue);
            this.mTimePicker.setContentDescription(this.mMinutePickerDescription + ": " + newValue);
        } else if (pickerIndex == 2) {
            updateAmPmDisplay(newValue);
        } else if (pickerIndex == 3) {
            if (!isTypedTimeFullyLegal()) {
                this.mTypedTimes.clear();
            }
            finishKbMode(true);
        }
    }

    private void setHour(int value, boolean announce) {
        String format;
        if (this.mIs24HourMode) {
            format = "%02d";
        } else {
            format = "%d";
            value %= 12;
            if (value == 0) {
                value = 12;
            }
        }
        CharSequence text = String.format(format, Integer.valueOf(value));
        this.mHourView.setText(text);
        this.mHourSpaceView.setText(text);
        if (announce) {
            Utils.tryAccessibilityAnnounce(this.mTimePicker, text);
        }
    }

    private void setMinute(int value) {
        if (value == 60) {
            value = 0;
        }
        CharSequence text = String.format(Locale.getDefault(), "%02d", Integer.valueOf(value));
        Utils.tryAccessibilityAnnounce(this.mTimePicker, text);
        this.mMinuteView.setText(text);
        this.mMinuteSpaceView.setText(text);
    }

    private void setCurrentItemShowing(int index, boolean animateCircle, boolean delayLabelAnimate, boolean announce) {
        TextView labelToAnimate;
        this.mTimePicker.setCurrentItemShowing(index, animateCircle);
        if (index == 0) {
            int hours = this.mTimePicker.getHours();
            if (!this.mIs24HourMode) {
                hours %= 12;
            }
            this.mTimePicker.setContentDescription(this.mHourPickerDescription + ": " + hours);
            if (announce) {
                Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mSelectHours);
            }
            labelToAnimate = this.mHourView;
        } else {
            int minutes = this.mTimePicker.getMinutes();
            this.mTimePicker.setContentDescription(this.mMinutePickerDescription + ": " + minutes);
            if (announce) {
                Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mSelectMinutes);
            }
            labelToAnimate = this.mMinuteView;
        }
        int hourColor = index == 0 ? this.mSelectedColor : this.mUnselectedColor;
        int minuteColor = index == 1 ? this.mSelectedColor : this.mUnselectedColor;
        this.mHourView.setTextColor(hourColor);
        this.mMinuteView.setTextColor(minuteColor);
        ObjectAnimator pulseAnimator = Utils.getPulseAnimator(labelToAnimate, 0.85f, 1.1f);
        if (delayLabelAnimate) {
            pulseAnimator.setStartDelay(300L);
        }
        pulseAnimator.start();
    }

    private boolean processKeyUp(int keyCode) {
        String deletedKeyStr;
        if (keyCode == 111 || keyCode == 4) {
            dismiss();
            return true;
        }
        if (keyCode == 61) {
            if (this.mInKbMode) {
                if (!isTypedTimeFullyLegal()) {
                    return true;
                }
                finishKbMode(true);
                return true;
            }
        } else {
            if (keyCode == 66) {
                if (this.mInKbMode) {
                    if (!isTypedTimeFullyLegal()) {
                        return true;
                    }
                    finishKbMode(false);
                }
                if (this.mCallback != null) {
                    this.mCallback.onTimeSet(this.mTimePicker, this.mTimePicker.getHours(), this.mTimePicker.getMinutes());
                }
                dismiss();
                return true;
            }
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
                    Utils.tryAccessibilityAnnounce(this.mTimePicker, String.format(this.mDeletedKeyFormat, deletedKeyStr));
                    updateDisplay(true);
                }
            } else if (keyCode == 7 || keyCode == 8 || keyCode == 9 || keyCode == 10 || keyCode == 11 || keyCode == 12 || keyCode == 13 || keyCode == 14 || keyCode == 15 || keyCode == 16 || (!this.mIs24HourMode && (keyCode == getAmOrPmKeyCode(0) || keyCode == getAmOrPmKeyCode(1)))) {
                if (!this.mInKbMode) {
                    if (this.mTimePicker == null) {
                        Log.e("TimePickerDialog", "Unable to initiate keyboard mode, TimePicker was null.");
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
        }
        return false;
    }

    private void tryStartingKbMode(int keyCode) {
        if (this.mTimePicker.trySettingInputEnabled(false)) {
            if (keyCode == -1 || addKeyIfLegal(keyCode)) {
                this.mInKbMode = true;
                this.mDoneButton.setEnabled(false);
                updateDisplay(false);
            }
        }
    }

    private boolean addKeyIfLegal(int keyCode) {
        if (this.mIs24HourMode && this.mTypedTimes.size() == 4) {
            return false;
        }
        if (!this.mIs24HourMode && isTypedTimeFullyLegal()) {
            return false;
        }
        this.mTypedTimes.add(Integer.valueOf(keyCode));
        if (!isTypedTimeLegalSoFar()) {
            deleteLastTypedKey();
            return false;
        }
        int val = getValFromKeyCode(keyCode);
        Utils.tryAccessibilityAnnounce(this.mTimePicker, String.format("%d", Integer.valueOf(val)));
        if (isTypedTimeFullyLegal()) {
            if (!this.mIs24HourMode && this.mTypedTimes.size() <= 3) {
                this.mTypedTimes.add(this.mTypedTimes.size() - 1, 7);
                this.mTypedTimes.add(this.mTypedTimes.size() - 1, 7);
            }
            this.mDoneButton.setEnabled(true);
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
        if (this.mIs24HourMode) {
            int[] values = getEnteredTime(null);
            return values[0] >= 0 && values[1] >= 0 && values[1] < 60;
        }
        return this.mTypedTimes.contains(Integer.valueOf(getAmOrPmKeyCode(0))) || this.mTypedTimes.contains(Integer.valueOf(getAmOrPmKeyCode(1)));
    }

    private int deleteLastTypedKey() {
        int deleted = this.mTypedTimes.remove(this.mTypedTimes.size() - 1).intValue();
        if (!isTypedTimeFullyLegal()) {
            this.mDoneButton.setEnabled(false);
        }
        return deleted;
    }

    private void finishKbMode(boolean updateDisplays) {
        this.mInKbMode = false;
        if (!this.mTypedTimes.isEmpty()) {
            int[] values = getEnteredTime(null);
            this.mTimePicker.setTime(values[0], values[1]);
            if (!this.mIs24HourMode) {
                this.mTimePicker.setAmOrPm(values[2]);
            }
            this.mTypedTimes.clear();
        }
        if (updateDisplays) {
            updateDisplay(false);
            this.mTimePicker.trySettingInputEnabled(true);
        }
    }

    private void updateDisplay(boolean allowEmptyDisplay) {
        if (!allowEmptyDisplay && this.mTypedTimes.isEmpty()) {
            int hour = this.mTimePicker.getHours();
            int minute = this.mTimePicker.getMinutes();
            setHour(hour, true);
            setMinute(minute);
            if (!this.mIs24HourMode) {
                updateAmPmDisplay(hour < 12 ? 0 : 1);
            }
            setCurrentItemShowing(this.mTimePicker.getCurrentItemShowing(), true, true, true);
            this.mDoneButton.setEnabled(true);
            return;
        }
        Boolean[] enteredZeros = {false, false};
        int[] values = getEnteredTime(enteredZeros);
        String hourFormat = enteredZeros[0].booleanValue() ? "%02d" : "%2d";
        String minuteFormat = enteredZeros[1].booleanValue() ? "%02d" : "%2d";
        String hourStr = values[0] == -1 ? this.mDoublePlaceholderText : String.format(hourFormat, Integer.valueOf(values[0])).replace(' ', this.mPlaceholderText);
        String minuteStr = values[1] == -1 ? this.mDoublePlaceholderText : String.format(minuteFormat, Integer.valueOf(values[1])).replace(' ', this.mPlaceholderText);
        this.mHourView.setText(hourStr);
        this.mHourSpaceView.setText(hourStr);
        this.mHourView.setTextColor(this.mUnselectedColor);
        this.mMinuteView.setText(minuteStr);
        this.mMinuteSpaceView.setText(minuteStr);
        this.mMinuteView.setTextColor(this.mUnselectedColor);
        if (!this.mIs24HourMode) {
            updateAmPmDisplay(values[2]);
        }
    }

    private static int getValFromKeyCode(int keyCode) {
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

    private int[] getEnteredTime(Boolean[] enteredZeros) {
        int amOrPm = -1;
        int startIndex = 1;
        if (!this.mIs24HourMode && isTypedTimeFullyLegal()) {
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
        int[] ret = {hour, minute, amOrPm};
        return ret;
    }

    private int getAmOrPmKeyCode(int amOrPm) {
        if (this.mAmKeyCode == -1 || this.mPmKeyCode == -1) {
            KeyCharacterMap kcm = KeyCharacterMap.load(-1);
            int i = 0;
            while (true) {
                if (i >= Math.max(this.mAmText.length(), this.mPmText.length())) {
                    break;
                }
                char amChar = this.mAmText.toLowerCase(Locale.getDefault()).charAt(i);
                char pmChar = this.mPmText.toLowerCase(Locale.getDefault()).charAt(i);
                if (amChar == pmChar) {
                    i++;
                } else {
                    KeyEvent[] events = kcm.getEvents(new char[]{amChar, pmChar});
                    if (events != null && events.length == 4) {
                        this.mAmKeyCode = events[0].getKeyCode();
                        this.mPmKeyCode = events[2].getKeyCode();
                    } else {
                        Log.e("TimePickerDialog", "Unable to find keycodes for AM and PM.");
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
        if (this.mIs24HourMode) {
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

    private class KeyboardListener implements View.OnKeyListener {
        private KeyboardListener() {
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == 1) {
                return TimePickerDialog.this.processKeyUp(keyCode);
            }
            return false;
        }
    }
}
