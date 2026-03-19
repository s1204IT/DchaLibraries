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

    public static TimePickerDialog newInstance(OnTimeSetListener onTimeSetListener, int i, int i2, boolean z) {
        TimePickerDialog timePickerDialog = new TimePickerDialog();
        timePickerDialog.initialize(onTimeSetListener, i, i2, z);
        return timePickerDialog;
    }

    public void initialize(OnTimeSetListener onTimeSetListener, int i, int i2, boolean z) {
        this.mCallback = onTimeSetListener;
        this.mInitialHourOfDay = i;
        this.mInitialMinute = i2;
        this.mIs24HourMode = z;
        this.mInKbMode = false;
        this.mThemeDark = false;
    }

    public void setOnTimeSetListener(OnTimeSetListener onTimeSetListener) {
        this.mCallback = onTimeSetListener;
    }

    public void setStartTime(int i, int i2) {
        this.mInitialHourOfDay = i;
        this.mInitialMinute = i2;
        this.mInKbMode = false;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null && bundle.containsKey("hour_of_day") && bundle.containsKey("minute") && bundle.containsKey("is_24_hour_view")) {
            this.mInitialHourOfDay = bundle.getInt("hour_of_day");
            this.mInitialMinute = bundle.getInt("minute");
            this.mIs24HourMode = bundle.getBoolean("is_24_hour_view");
            this.mInKbMode = bundle.getBoolean("in_kb_mode");
            this.mThemeDark = bundle.getBoolean("dark_theme");
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        int i;
        getDialog().getWindow().requestFeature(1);
        View viewInflate = layoutInflater.inflate(R.layout.time_picker_dialog, (ViewGroup) null);
        KeyboardListener keyboardListener = new KeyboardListener();
        viewInflate.findViewById(R.id.time_picker_dialog).setOnKeyListener(keyboardListener);
        Resources resources = getResources();
        this.mHourPickerDescription = resources.getString(R.string.hour_picker_description);
        this.mSelectHours = resources.getString(R.string.select_hours);
        this.mMinutePickerDescription = resources.getString(R.string.minute_picker_description);
        this.mSelectMinutes = resources.getString(R.string.select_minutes);
        this.mSelectedColor = resources.getColor(this.mThemeDark ? R.color.red : R.color.blue);
        this.mUnselectedColor = resources.getColor(this.mThemeDark ? 17170443 : R.color.numbers_text_color);
        this.mHourView = (TextView) viewInflate.findViewById(R.id.hours);
        this.mHourView.setOnKeyListener(keyboardListener);
        this.mHourSpaceView = (TextView) viewInflate.findViewById(R.id.hour_space);
        this.mMinuteSpaceView = (TextView) viewInflate.findViewById(R.id.minutes_space);
        this.mMinuteView = (TextView) viewInflate.findViewById(R.id.minutes);
        this.mMinuteView.setOnKeyListener(keyboardListener);
        this.mAmPmTextView = (TextView) viewInflate.findViewById(R.id.ampm_label);
        this.mAmPmTextView.setOnKeyListener(keyboardListener);
        String[] amPmStrings = new DateFormatSymbols().getAmPmStrings();
        this.mAmText = amPmStrings[0];
        this.mPmText = amPmStrings[1];
        this.mHapticFeedbackController = new HapticFeedbackController(getActivity());
        this.mTimePicker = (RadialPickerLayout) viewInflate.findViewById(R.id.time_picker);
        this.mTimePicker.setOnValueSelectedListener(this);
        this.mTimePicker.setOnKeyListener(keyboardListener);
        this.mTimePicker.initialize(getActivity(), this.mHapticFeedbackController, this.mInitialHourOfDay, this.mInitialMinute, this.mIs24HourMode);
        if (bundle != null && bundle.containsKey("current_item_showing")) {
            i = bundle.getInt("current_item_showing");
        } else {
            i = 0;
        }
        setCurrentItemShowing(i, false, true, true);
        this.mTimePicker.invalidate();
        this.mHourView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog.this.setCurrentItemShowing(0, true, false, true);
                TimePickerDialog.this.tryVibrate();
            }
        });
        this.mMinuteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog.this.setCurrentItemShowing(1, true, false, true);
                TimePickerDialog.this.tryVibrate();
            }
        });
        this.mDoneButton = (TextView) viewInflate.findViewById(R.id.done_button);
        this.mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TimePickerDialog.this.mInKbMode && TimePickerDialog.this.isTypedTimeFullyLegal()) {
                    TimePickerDialog.this.finishKbMode(false);
                } else {
                    TimePickerDialog.this.tryVibrate();
                }
                if (TimePickerDialog.this.mCallback != null) {
                    TimePickerDialog.this.mCallback.onTimeSet(TimePickerDialog.this.mTimePicker, TimePickerDialog.this.mTimePicker.getHours(), TimePickerDialog.this.mTimePicker.getMinutes());
                }
                TimePickerDialog.this.dismissAllowingStateLoss();
            }
        });
        this.mDoneButton.setOnKeyListener(keyboardListener);
        this.mAmPmHitspace = viewInflate.findViewById(R.id.ampm_hitspace);
        if (!this.mIs24HourMode) {
            this.mAmPmTextView.setVisibility(0);
            updateAmPmDisplay(this.mInitialHourOfDay < 12 ? 0 : 1);
            this.mAmPmHitspace.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TimePickerDialog.this.tryVibrate();
                    int isCurrentlyAmOrPm = TimePickerDialog.this.mTimePicker.getIsCurrentlyAmOrPm();
                    if (isCurrentlyAmOrPm != 0) {
                        if (isCurrentlyAmOrPm == 1) {
                            isCurrentlyAmOrPm = 0;
                        }
                    } else {
                        isCurrentlyAmOrPm = 1;
                    }
                    TimePickerDialog.this.updateAmPmDisplay(isCurrentlyAmOrPm);
                    TimePickerDialog.this.mTimePicker.setAmOrPm(isCurrentlyAmOrPm);
                }
            });
        } else {
            this.mAmPmTextView.setVisibility(8);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-2, -2);
            layoutParams.addRule(13);
            ((TextView) viewInflate.findViewById(R.id.separator)).setLayoutParams(layoutParams);
        }
        this.mAllowAutoAdvance = true;
        setHour(this.mInitialHourOfDay, true);
        setMinute(this.mInitialMinute);
        this.mDoublePlaceholderText = resources.getString(R.string.time_placeholder);
        this.mDeletedKeyFormat = resources.getString(R.string.deleted_key);
        this.mPlaceholderText = this.mDoublePlaceholderText.charAt(0);
        this.mPmKeyCode = -1;
        this.mAmKeyCode = -1;
        generateLegalTimesTree();
        if (this.mInKbMode) {
            this.mTypedTimes = bundle.getIntegerArrayList("typed_times");
            tryStartingKbMode(-1);
            this.mHourView.invalidate();
        } else if (this.mTypedTimes == null) {
            this.mTypedTimes = new ArrayList<>();
        }
        this.mTimePicker.setTheme(getActivity().getApplicationContext(), this.mThemeDark);
        int color = resources.getColor(android.R.color.white);
        int color2 = resources.getColor(R.color.circle_background);
        int color3 = resources.getColor(R.color.line_background);
        int color4 = resources.getColor(R.color.numbers_text_color);
        ColorStateList colorStateList = resources.getColorStateList(R.color.done_text_color);
        int i2 = R.drawable.done_background_color;
        int color5 = resources.getColor(R.color.dark_gray);
        int color6 = resources.getColor(R.color.light_gray);
        int color7 = resources.getColor(R.color.line_dark);
        ColorStateList colorStateList2 = resources.getColorStateList(R.color.done_text_color_dark);
        int i3 = R.drawable.done_background_color_dark;
        viewInflate.findViewById(R.id.time_display_background).setBackgroundColor(this.mThemeDark ? color5 : color);
        View viewFindViewById = viewInflate.findViewById(R.id.time_display);
        if (!this.mThemeDark) {
            color5 = color;
        }
        viewFindViewById.setBackgroundColor(color5);
        ((TextView) viewInflate.findViewById(R.id.separator)).setTextColor(this.mThemeDark ? color : color4);
        TextView textView = (TextView) viewInflate.findViewById(R.id.ampm_label);
        if (!this.mThemeDark) {
            color = color4;
        }
        textView.setTextColor(color);
        View viewFindViewById2 = viewInflate.findViewById(R.id.line);
        if (this.mThemeDark) {
            color3 = color7;
        }
        viewFindViewById2.setBackgroundColor(color3);
        TextView textView2 = this.mDoneButton;
        if (!this.mThemeDark) {
            colorStateList2 = colorStateList;
        }
        textView2.setTextColor(colorStateList2);
        RadialPickerLayout radialPickerLayout = this.mTimePicker;
        if (this.mThemeDark) {
            color2 = color6;
        }
        radialPickerLayout.setBackgroundColor(color2);
        TextView textView3 = this.mDoneButton;
        if (this.mThemeDark) {
            i2 = i3;
        }
        textView3.setBackgroundResource(i2);
        return viewInflate;
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

    private void updateAmPmDisplay(int i) {
        if (i == 0) {
            this.mAmPmTextView.setText(this.mAmText);
            Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mAmText);
            this.mAmPmHitspace.setContentDescription(this.mAmText);
        } else {
            if (i == 1) {
                this.mAmPmTextView.setText(this.mPmText);
                Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mPmText);
                this.mAmPmHitspace.setContentDescription(this.mPmText);
                return;
            }
            this.mAmPmTextView.setText(this.mDoublePlaceholderText);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        if (this.mTimePicker != null) {
            bundle.putInt("hour_of_day", this.mTimePicker.getHours());
            bundle.putInt("minute", this.mTimePicker.getMinutes());
            bundle.putBoolean("is_24_hour_view", this.mIs24HourMode);
            bundle.putInt("current_item_showing", this.mTimePicker.getCurrentItemShowing());
            bundle.putBoolean("in_kb_mode", this.mInKbMode);
            if (this.mInKbMode) {
                bundle.putIntegerArrayList("typed_times", this.mTypedTimes);
            }
            bundle.putBoolean("dark_theme", this.mThemeDark);
        }
    }

    @Override
    public void onValueSelected(int i, int i2, boolean z) {
        if (i != 0) {
            if (i == 1) {
                setMinute(i2);
                this.mTimePicker.setContentDescription(this.mMinutePickerDescription + ": " + i2);
                return;
            }
            if (i == 2) {
                updateAmPmDisplay(i2);
                return;
            } else {
                if (i == 3) {
                    if (!isTypedTimeFullyLegal()) {
                        this.mTypedTimes.clear();
                    }
                    finishKbMode(true);
                    return;
                }
                return;
            }
        }
        setHour(i2, false);
        String str = String.format("%d", Integer.valueOf(i2));
        if (this.mAllowAutoAdvance && z) {
            setCurrentItemShowing(1, true, true, false);
            str = str + ". " + this.mSelectMinutes;
        } else {
            this.mTimePicker.setContentDescription(this.mHourPickerDescription + ": " + i2);
        }
        Utils.tryAccessibilityAnnounce(this.mTimePicker, str);
    }

    private void setHour(int i, boolean z) {
        String str;
        if (this.mIs24HourMode) {
            str = "%02d";
        } else {
            str = "%d";
            i %= 12;
            if (i == 0) {
                i = 12;
            }
        }
        String str2 = String.format(str, Integer.valueOf(i));
        this.mHourView.setText(str2);
        this.mHourSpaceView.setText(str2);
        if (z) {
            Utils.tryAccessibilityAnnounce(this.mTimePicker, str2);
        }
    }

    private void setMinute(int i) {
        if (i == 60) {
            i = 0;
        }
        String str = String.format(Locale.getDefault(), "%02d", Integer.valueOf(i));
        Utils.tryAccessibilityAnnounce(this.mTimePicker, str);
        this.mMinuteView.setText(str);
        this.mMinuteSpaceView.setText(str);
    }

    private void setCurrentItemShowing(int i, boolean z, boolean z2, boolean z3) {
        TextView textView;
        this.mTimePicker.setCurrentItemShowing(i, z);
        if (i == 0) {
            int hours = this.mTimePicker.getHours();
            if (!this.mIs24HourMode) {
                hours %= 12;
            }
            this.mTimePicker.setContentDescription(this.mHourPickerDescription + ": " + hours);
            if (z3) {
                Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mSelectHours);
            }
            textView = this.mHourView;
        } else {
            int minutes = this.mTimePicker.getMinutes();
            this.mTimePicker.setContentDescription(this.mMinutePickerDescription + ": " + minutes);
            if (z3) {
                Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mSelectMinutes);
            }
            textView = this.mMinuteView;
        }
        int i2 = i == 0 ? this.mSelectedColor : this.mUnselectedColor;
        int i3 = i == 1 ? this.mSelectedColor : this.mUnselectedColor;
        this.mHourView.setTextColor(i2);
        this.mMinuteView.setTextColor(i3);
        ObjectAnimator pulseAnimator = Utils.getPulseAnimator(textView, 0.85f, 1.1f);
        if (z2) {
            pulseAnimator.setStartDelay(300L);
        }
        pulseAnimator.start();
    }

    private boolean processKeyUp(int i) {
        String str;
        if (i == 111 || i == 4) {
            dismiss();
            return true;
        }
        if (i == 61) {
            if (this.mInKbMode) {
                if (isTypedTimeFullyLegal()) {
                    finishKbMode(true);
                }
                return true;
            }
        } else {
            if (i == 66) {
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
            if (i == 67) {
                if (this.mInKbMode && !this.mTypedTimes.isEmpty()) {
                    int iDeleteLastTypedKey = deleteLastTypedKey();
                    if (iDeleteLastTypedKey == getAmOrPmKeyCode(0)) {
                        str = this.mAmText;
                    } else if (iDeleteLastTypedKey == getAmOrPmKeyCode(1)) {
                        str = this.mPmText;
                    } else {
                        str = String.format("%d", Integer.valueOf(getValFromKeyCode(iDeleteLastTypedKey)));
                    }
                    Utils.tryAccessibilityAnnounce(this.mTimePicker, String.format(this.mDeletedKeyFormat, str));
                    updateDisplay(true);
                }
            } else if (i == 7 || i == 8 || i == 9 || i == 10 || i == 11 || i == 12 || i == 13 || i == 14 || i == 15 || i == 16 || (!this.mIs24HourMode && (i == getAmOrPmKeyCode(0) || i == getAmOrPmKeyCode(1)))) {
                if (!this.mInKbMode) {
                    if (this.mTimePicker == null) {
                        Log.e("TimePickerDialog", "Unable to initiate keyboard mode, TimePicker was null.");
                        return true;
                    }
                    this.mTypedTimes.clear();
                    tryStartingKbMode(i);
                    return true;
                }
                if (addKeyIfLegal(i)) {
                    updateDisplay(false);
                }
                return true;
            }
        }
        return false;
    }

    private void tryStartingKbMode(int i) {
        if (this.mTimePicker.trySettingInputEnabled(false)) {
            if (i == -1 || addKeyIfLegal(i)) {
                this.mInKbMode = true;
                this.mDoneButton.setEnabled(false);
                updateDisplay(false);
            }
        }
    }

    private boolean addKeyIfLegal(int i) {
        if ((this.mIs24HourMode && this.mTypedTimes.size() == 4) || (!this.mIs24HourMode && isTypedTimeFullyLegal())) {
            return false;
        }
        this.mTypedTimes.add(Integer.valueOf(i));
        if (!isTypedTimeLegalSoFar()) {
            deleteLastTypedKey();
            return false;
        }
        Utils.tryAccessibilityAnnounce(this.mTimePicker, String.format("%d", Integer.valueOf(getValFromKeyCode(i))));
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
        Node nodeCanReach = this.mLegalTimesTree;
        Iterator<Integer> it = this.mTypedTimes.iterator();
        while (it.hasNext()) {
            nodeCanReach = nodeCanReach.canReach(it.next().intValue());
            if (nodeCanReach == null) {
                return false;
            }
        }
        return true;
    }

    private boolean isTypedTimeFullyLegal() {
        if (!this.mIs24HourMode) {
            return this.mTypedTimes.contains(Integer.valueOf(getAmOrPmKeyCode(0))) || this.mTypedTimes.contains(Integer.valueOf(getAmOrPmKeyCode(1)));
        }
        int[] enteredTime = getEnteredTime(null);
        return enteredTime[0] >= 0 && enteredTime[1] >= 0 && enteredTime[1] < 60;
    }

    private int deleteLastTypedKey() {
        int iIntValue = this.mTypedTimes.remove(this.mTypedTimes.size() - 1).intValue();
        if (!isTypedTimeFullyLegal()) {
            this.mDoneButton.setEnabled(false);
        }
        return iIntValue;
    }

    private void finishKbMode(boolean z) {
        this.mInKbMode = false;
        if (!this.mTypedTimes.isEmpty()) {
            int[] enteredTime = getEnteredTime(null);
            this.mTimePicker.setTime(enteredTime[0], enteredTime[1]);
            if (!this.mIs24HourMode) {
                this.mTimePicker.setAmOrPm(enteredTime[2]);
            }
            this.mTypedTimes.clear();
        }
        if (z) {
            updateDisplay(false);
            this.mTimePicker.trySettingInputEnabled(true);
        }
    }

    private void updateDisplay(boolean z) {
        if (!z && this.mTypedTimes.isEmpty()) {
            int hours = this.mTimePicker.getHours();
            int minutes = this.mTimePicker.getMinutes();
            setHour(hours, true);
            setMinute(minutes);
            if (!this.mIs24HourMode) {
                updateAmPmDisplay(hours >= 12 ? 1 : 0);
            }
            setCurrentItemShowing(this.mTimePicker.getCurrentItemShowing(), true, true, true);
            this.mDoneButton.setEnabled(true);
            return;
        }
        Boolean[] boolArr = {false, false};
        int[] enteredTime = getEnteredTime(boolArr);
        String str = boolArr[0].booleanValue() ? "%02d" : "%2d";
        String str2 = boolArr[1].booleanValue() ? "%02d" : "%2d";
        String strReplace = enteredTime[0] == -1 ? this.mDoublePlaceholderText : String.format(str, Integer.valueOf(enteredTime[0])).replace(' ', this.mPlaceholderText);
        String strReplace2 = enteredTime[1] == -1 ? this.mDoublePlaceholderText : String.format(str2, Integer.valueOf(enteredTime[1])).replace(' ', this.mPlaceholderText);
        this.mHourView.setText(strReplace);
        this.mHourSpaceView.setText(strReplace);
        this.mHourView.setTextColor(this.mUnselectedColor);
        this.mMinuteView.setText(strReplace2);
        this.mMinuteSpaceView.setText(strReplace2);
        this.mMinuteView.setTextColor(this.mUnselectedColor);
        if (!this.mIs24HourMode) {
            updateAmPmDisplay(enteredTime[2]);
        }
    }

    private static int getValFromKeyCode(int i) {
        switch (i) {
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

    private int[] getEnteredTime(Boolean[] boolArr) {
        int i;
        int i2;
        if (this.mIs24HourMode || !isTypedTimeFullyLegal()) {
            i = -1;
            i2 = 1;
        } else {
            int iIntValue = this.mTypedTimes.get(this.mTypedTimes.size() - 1).intValue();
            i = iIntValue == getAmOrPmKeyCode(0) ? 0 : iIntValue == getAmOrPmKeyCode(1) ? 1 : -1;
            i2 = 2;
        }
        int i3 = -1;
        int i4 = -1;
        for (int i5 = i2; i5 <= this.mTypedTimes.size(); i5++) {
            int valFromKeyCode = getValFromKeyCode(this.mTypedTimes.get(this.mTypedTimes.size() - i5).intValue());
            if (i5 == i2) {
                i4 = valFromKeyCode;
            } else if (i5 == i2 + 1) {
                i4 += 10 * valFromKeyCode;
                if (boolArr != null && valFromKeyCode == 0) {
                    boolArr[1] = true;
                }
            } else if (i5 == i2 + 2) {
                i3 = valFromKeyCode;
            } else if (i5 == i2 + 3) {
                i3 += 10 * valFromKeyCode;
                if (boolArr != null && valFromKeyCode == 0) {
                    boolArr[0] = true;
                }
            }
        }
        return new int[]{i3, i4, i};
    }

    private int getAmOrPmKeyCode(int i) {
        if (this.mAmKeyCode == -1 || this.mPmKeyCode == -1) {
            KeyCharacterMap keyCharacterMapLoad = KeyCharacterMap.load(-1);
            int i2 = 0;
            while (true) {
                if (i2 >= Math.max(this.mAmText.length(), this.mPmText.length())) {
                    break;
                }
                char cCharAt = this.mAmText.toLowerCase(Locale.getDefault()).charAt(i2);
                char cCharAt2 = this.mPmText.toLowerCase(Locale.getDefault()).charAt(i2);
                if (cCharAt == cCharAt2) {
                    i2++;
                } else {
                    KeyEvent[] events = keyCharacterMapLoad.getEvents(new char[]{cCharAt, cCharAt2});
                    if (events != null && events.length == 4) {
                        this.mAmKeyCode = events[0].getKeyCode();
                        this.mPmKeyCode = events[2].getKeyCode();
                    } else {
                        Log.e("TimePickerDialog", "Unable to find keycodes for AM and PM.");
                    }
                }
            }
        }
        if (i == 0) {
            return this.mAmKeyCode;
        }
        if (i == 1) {
            return this.mPmKeyCode;
        }
        return -1;
    }

    private void generateLegalTimesTree() {
        this.mLegalTimesTree = new Node(new int[0]);
        if (this.mIs24HourMode) {
            Node node = new Node(7, 8, 9, 10, 11, 12);
            Node node2 = new Node(7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
            node.addChild(node2);
            Node node3 = new Node(7, 8);
            this.mLegalTimesTree.addChild(node3);
            Node node4 = new Node(7, 8, 9, 10, 11, 12);
            node3.addChild(node4);
            node4.addChild(node);
            node4.addChild(new Node(13, 14, 15, 16));
            Node node5 = new Node(13, 14, 15, 16);
            node3.addChild(node5);
            node5.addChild(node);
            Node node6 = new Node(9);
            this.mLegalTimesTree.addChild(node6);
            Node node7 = new Node(7, 8, 9, 10);
            node6.addChild(node7);
            node7.addChild(node);
            Node node8 = new Node(11, 12);
            node6.addChild(node8);
            node8.addChild(node2);
            Node node9 = new Node(10, 11, 12, 13, 14, 15, 16);
            this.mLegalTimesTree.addChild(node9);
            node9.addChild(node);
            return;
        }
        Node node10 = new Node(getAmOrPmKeyCode(0), getAmOrPmKeyCode(1));
        Node node11 = new Node(8);
        this.mLegalTimesTree.addChild(node11);
        node11.addChild(node10);
        Node node12 = new Node(7, 8, 9);
        node11.addChild(node12);
        node12.addChild(node10);
        Node node13 = new Node(7, 8, 9, 10, 11, 12);
        node12.addChild(node13);
        node13.addChild(node10);
        Node node14 = new Node(7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        node13.addChild(node14);
        node14.addChild(node10);
        Node node15 = new Node(13, 14, 15, 16);
        node12.addChild(node15);
        node15.addChild(node10);
        Node node16 = new Node(10, 11, 12);
        node11.addChild(node16);
        Node node17 = new Node(7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        node16.addChild(node17);
        node17.addChild(node10);
        Node node18 = new Node(9, 10, 11, 12, 13, 14, 15, 16);
        this.mLegalTimesTree.addChild(node18);
        node18.addChild(node10);
        Node node19 = new Node(7, 8, 9, 10, 11, 12);
        node18.addChild(node19);
        Node node20 = new Node(7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        node19.addChild(node20);
        node20.addChild(node10);
    }

    private class Node {
        private ArrayList<Node> mChildren = new ArrayList<>();
        private int[] mLegalKeys;

        public Node(int... iArr) {
            this.mLegalKeys = iArr;
        }

        public void addChild(Node node) {
            this.mChildren.add(node);
        }

        public boolean containsKey(int i) {
            for (int i2 = 0; i2 < this.mLegalKeys.length; i2++) {
                if (this.mLegalKeys[i2] == i) {
                    return true;
                }
            }
            return false;
        }

        public Node canReach(int i) {
            if (this.mChildren == null) {
                return null;
            }
            for (Node node : this.mChildren) {
                if (node.containsKey(i)) {
                    return node;
                }
            }
            return null;
        }
    }

    private class KeyboardListener implements View.OnKeyListener {
        private KeyboardListener() {
        }

        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            if (keyEvent.getAction() == 1) {
                return TimePickerDialog.this.processKeyUp(i);
            }
            return false;
        }
    }
}
