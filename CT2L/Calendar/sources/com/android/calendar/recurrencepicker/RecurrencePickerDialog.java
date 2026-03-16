package com.android.calendar.recurrencepicker;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendarcommon2.EventRecurrence;
import com.android.datetimepicker.date.DatePickerDialog;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;

public class RecurrencePickerDialog extends DialogFragment implements View.OnClickListener, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener, RadioGroup.OnCheckedChangeListener, DatePickerDialog.OnDateSetListener {
    private static final int[] mFreqModelToEventRecurrence = {4, 5, 6, 7};
    private DatePickerDialog mDatePickerDialog;
    private Button mDone;
    private EditText mEndCount;
    private String mEndCountLabel;
    private String mEndDateLabel;
    private TextView mEndDateTextView;
    private String mEndNeverStr;
    private Spinner mEndSpinner;
    private EndSpinnerAdapter mEndSpinnerAdapter;
    private Spinner mFreqSpinner;
    private boolean mHidePostEndCount;
    private EditText mInterval;
    private TextView mIntervalPostText;
    private TextView mIntervalPreText;
    private LinearLayout mMonthGroup;
    private String mMonthRepeatByDayOfWeekStr;
    private String[][] mMonthRepeatByDayOfWeekStrs;
    private RadioGroup mMonthRepeatByRadioGroup;
    private TextView mPostEndCount;
    private OnRecurrenceSetListener mRecurrenceSetListener;
    private RadioButton mRepeatMonthlyByNthDayOfMonth;
    private RadioButton mRepeatMonthlyByNthDayOfWeek;
    private Switch mRepeatSwitch;
    private Resources mResources;
    private View mView;
    private LinearLayout mWeekGroup;
    private LinearLayout mWeekGroup2;
    private EventRecurrence mRecurrence = new EventRecurrence();
    private Time mTime = new Time();
    private RecurrenceModel mModel = new RecurrenceModel();
    private final int[] TIME_DAY_TO_CALENDAR_DAY = {1, 2, 3, 4, 5, 6, 7};
    private int mIntervalResId = -1;
    private ArrayList<CharSequence> mEndSpinnerArray = new ArrayList<>(3);
    private ToggleButton[] mWeekByDayButtons = new ToggleButton[7];

    public interface OnRecurrenceSetListener {
        void onRecurrenceSet(String str);
    }

    private class RecurrenceModel implements Parcelable {
        int end;
        Time endDate;
        int monthlyByDayOfWeek;
        int monthlyByMonthDay;
        int monthlyByNthDayOfWeek;
        int monthlyRepeat;
        int recurrenceState;
        int freq = 1;
        int interval = 1;
        int endCount = 5;
        boolean[] weeklyByDayOfWeek = new boolean[7];

        public String toString() {
            return "Model [freq=" + this.freq + ", interval=" + this.interval + ", end=" + this.end + ", endDate=" + this.endDate + ", endCount=" + this.endCount + ", weeklyByDayOfWeek=" + Arrays.toString(this.weeklyByDayOfWeek) + ", monthlyRepeat=" + this.monthlyRepeat + ", monthlyByMonthDay=" + this.monthlyByMonthDay + ", monthlyByDayOfWeek=" + this.monthlyByDayOfWeek + ", monthlyByNthDayOfWeek=" + this.monthlyByNthDayOfWeek + "]";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public RecurrenceModel() {
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.freq);
            dest.writeInt(this.interval);
            dest.writeInt(this.end);
            dest.writeInt(this.endDate.year);
            dest.writeInt(this.endDate.month);
            dest.writeInt(this.endDate.monthDay);
            dest.writeInt(this.endCount);
            dest.writeBooleanArray(this.weeklyByDayOfWeek);
            dest.writeInt(this.monthlyRepeat);
            dest.writeInt(this.monthlyByMonthDay);
            dest.writeInt(this.monthlyByDayOfWeek);
            dest.writeInt(this.monthlyByNthDayOfWeek);
            dest.writeInt(this.recurrenceState);
        }
    }

    class minMaxTextWatcher implements TextWatcher {
        private int mDefault;
        private int mMax;
        private int mMin;

        public minMaxTextWatcher(int min, int defaultInt, int max) {
            this.mMin = min;
            this.mMax = max;
            this.mDefault = defaultInt;
        }

        @Override
        public void afterTextChanged(Editable s) {
            int value;
            boolean updated = false;
            try {
                value = Integer.parseInt(s.toString());
            } catch (NumberFormatException e) {
                value = this.mDefault;
            }
            if (value < this.mMin) {
                value = this.mMin;
                updated = true;
            } else if (value > this.mMax) {
                updated = true;
                value = this.mMax;
            }
            if (updated) {
                s.clear();
                s.append((CharSequence) Integer.toString(value));
            }
            RecurrencePickerDialog.this.updateDoneButtonState();
            onChange(value);
        }

        void onChange(int value) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    public static boolean isSupportedMonthlyByNthDayOfWeek(int num) {
        return (num > 0 && num <= 5) || num == -1;
    }

    public static boolean canHandleRecurrenceRule(EventRecurrence er) {
        switch (er.freq) {
            case 4:
            case 5:
            case 6:
            case 7:
                if (er.count <= 0 || TextUtils.isEmpty(er.until)) {
                    int numOfByDayNum = 0;
                    for (int i = 0; i < er.bydayCount; i++) {
                        if (isSupportedMonthlyByNthDayOfWeek(er.bydayNum[i])) {
                            numOfByDayNum++;
                        }
                    }
                    if (numOfByDayNum <= 1) {
                        if ((numOfByDayNum <= 0 || er.freq == 6) && er.bymonthdayCount <= 1) {
                            if (er.freq == 6) {
                                if (er.bydayCount <= 1) {
                                    if (er.bydayCount > 0 && er.bymonthdayCount > 0) {
                                    }
                                }
                            }
                        }
                    }
                }
                break;
        }
        return false;
    }

    private static void copyEventRecurrenceToModel(EventRecurrence er, RecurrenceModel model) {
        switch (er.freq) {
            case 4:
                model.freq = 0;
                break;
            case 5:
                model.freq = 1;
                break;
            case 6:
                model.freq = 2;
                break;
            case 7:
                model.freq = 3;
                break;
            default:
                throw new IllegalStateException("freq=" + er.freq);
        }
        if (er.interval > 0) {
            model.interval = er.interval;
        }
        model.endCount = er.count;
        if (model.endCount > 0) {
            model.end = 2;
        }
        if (!TextUtils.isEmpty(er.until)) {
            if (model.endDate == null) {
                model.endDate = new Time();
            }
            try {
                model.endDate.parse(er.until);
            } catch (TimeFormatException e) {
                model.endDate = null;
            }
            if (model.end == 2 && model.endDate != null) {
                throw new IllegalStateException("freq=" + er.freq);
            }
            model.end = 1;
        }
        Arrays.fill(model.weeklyByDayOfWeek, false);
        if (er.bydayCount > 0) {
            int count = 0;
            for (int i = 0; i < er.bydayCount; i++) {
                int dayOfWeek = EventRecurrence.day2TimeDay(er.byday[i]);
                model.weeklyByDayOfWeek[dayOfWeek] = true;
                if (model.freq == 2 && isSupportedMonthlyByNthDayOfWeek(er.bydayNum[i])) {
                    model.monthlyByDayOfWeek = dayOfWeek;
                    model.monthlyByNthDayOfWeek = er.bydayNum[i];
                    model.monthlyRepeat = 1;
                    count++;
                }
            }
            if (model.freq == 2) {
                if (er.bydayCount != 1) {
                    throw new IllegalStateException("Can handle only 1 byDayOfWeek in monthly");
                }
                if (count != 1) {
                    throw new IllegalStateException("Didn't specify which nth day of week to repeat for a monthly");
                }
            }
        }
        if (model.freq == 2) {
            if (er.bymonthdayCount == 1) {
                if (model.monthlyRepeat == 1) {
                    throw new IllegalStateException("Can handle only by monthday or by nth day of week, not both");
                }
                model.monthlyByMonthDay = er.bymonthday[0];
                model.monthlyRepeat = 0;
                return;
            }
            if (er.bymonthCount > 1) {
                throw new IllegalStateException("Can handle only one bymonthday");
            }
        }
    }

    private static void copyModelToEventRecurrence(RecurrenceModel model, EventRecurrence er) {
        if (model.recurrenceState == 0) {
            throw new IllegalStateException("There's no recurrence");
        }
        er.freq = mFreqModelToEventRecurrence[model.freq];
        if (model.interval <= 1) {
            er.interval = 0;
        } else {
            er.interval = model.interval;
        }
        switch (model.end) {
            case 1:
                if (model.endDate != null) {
                    model.endDate.switchTimezone("UTC");
                    model.endDate.normalize(false);
                    er.until = model.endDate.format2445();
                    er.count = 0;
                } else {
                    throw new IllegalStateException("end = END_BY_DATE but endDate is null");
                }
                break;
            case 2:
                er.count = model.endCount;
                er.until = null;
                if (er.count <= 0) {
                    throw new IllegalStateException("count is " + er.count);
                }
                break;
            default:
                er.count = 0;
                er.until = null;
                break;
        }
        er.bydayCount = 0;
        er.bymonthdayCount = 0;
        switch (model.freq) {
            case 1:
                int count = 0;
                for (int i = 0; i < 7; i++) {
                    if (model.weeklyByDayOfWeek[i]) {
                        count++;
                    }
                }
                if (er.bydayCount < count || er.byday == null || er.bydayNum == null) {
                    er.byday = new int[count];
                    er.bydayNum = new int[count];
                }
                er.bydayCount = count;
                for (int i2 = 6; i2 >= 0; i2--) {
                    if (model.weeklyByDayOfWeek[i2]) {
                        count--;
                        er.bydayNum[count] = 0;
                        er.byday[count] = EventRecurrence.timeDay2Day(i2);
                    }
                }
                break;
            case 2:
                if (model.monthlyRepeat == 0) {
                    if (model.monthlyByMonthDay > 0) {
                        if (er.bymonthday == null || er.bymonthdayCount < 1) {
                            er.bymonthday = new int[1];
                        }
                        er.bymonthday[0] = model.monthlyByMonthDay;
                        er.bymonthdayCount = 1;
                    }
                } else if (model.monthlyRepeat == 1) {
                    if (!isSupportedMonthlyByNthDayOfWeek(model.monthlyByNthDayOfWeek)) {
                        throw new IllegalStateException("month repeat by nth week but n is " + model.monthlyByNthDayOfWeek);
                    }
                    if (er.bydayCount < 1 || er.byday == null || er.bydayNum == null) {
                        er.byday = new int[1];
                        er.bydayNum = new int[1];
                    }
                    er.bydayCount = 1;
                    er.byday[0] = EventRecurrence.timeDay2Day(model.monthlyByDayOfWeek);
                    er.bydayNum[0] = model.monthlyByNthDayOfWeek;
                }
                break;
        }
        if (!canHandleRecurrenceRule(er)) {
            throw new IllegalStateException("UI generated recurrence that it can't handle. ER:" + er.toString() + " Model: " + model.toString());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int numOfButtonsInRow1;
        int numOfButtonsInRow2;
        this.mRecurrence.wkst = EventRecurrence.timeDay2Day(Utils.getFirstDayOfWeek(getActivity()));
        getDialog().getWindow().requestFeature(1);
        boolean endCountHasFocus = false;
        if (savedInstanceState != null) {
            RecurrenceModel m = (RecurrenceModel) savedInstanceState.get("bundle_model");
            if (m != null) {
                this.mModel = m;
            }
            endCountHasFocus = savedInstanceState.getBoolean("bundle_end_count_has_focus");
        } else {
            Bundle b = getArguments();
            if (b != null) {
                this.mTime.set(b.getLong("bundle_event_start_time"));
                String tz = b.getString("bundle_event_time_zone");
                if (!TextUtils.isEmpty(tz)) {
                    this.mTime.timezone = tz;
                }
                this.mTime.normalize(false);
                this.mModel.weeklyByDayOfWeek[this.mTime.weekDay] = true;
                String rrule = b.getString("bundle_event_rrule");
                if (!TextUtils.isEmpty(rrule)) {
                    this.mModel.recurrenceState = 1;
                    this.mRecurrence.parse(rrule);
                    copyEventRecurrenceToModel(this.mRecurrence, this.mModel);
                    if (this.mRecurrence.bydayCount == 0) {
                        this.mModel.weeklyByDayOfWeek[this.mTime.weekDay] = true;
                    }
                }
            } else {
                this.mTime.setToNow();
            }
        }
        this.mResources = getResources();
        this.mView = inflater.inflate(R.layout.recurrencepicker, container, true);
        Activity activity = getActivity();
        activity.getResources().getConfiguration();
        this.mRepeatSwitch = (Switch) this.mView.findViewById(R.id.repeat_switch);
        this.mRepeatSwitch.setChecked(this.mModel.recurrenceState == 1);
        this.mRepeatSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                RecurrencePickerDialog.this.mModel.recurrenceState = isChecked ? 1 : 0;
                RecurrencePickerDialog.this.togglePickerOptions();
            }
        });
        this.mFreqSpinner = (Spinner) this.mView.findViewById(R.id.freqSpinner);
        this.mFreqSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.recurrence_freq, R.layout.recurrencepicker_freq_item);
        freqAdapter.setDropDownViewResource(R.layout.recurrencepicker_freq_item);
        this.mFreqSpinner.setAdapter((SpinnerAdapter) freqAdapter);
        this.mInterval = (EditText) this.mView.findViewById(R.id.interval);
        this.mInterval.addTextChangedListener(new minMaxTextWatcher(1, 1, 99) {
            @Override
            void onChange(int v) {
                if (RecurrencePickerDialog.this.mIntervalResId != -1 && RecurrencePickerDialog.this.mInterval.getText().toString().length() > 0) {
                    RecurrencePickerDialog.this.mModel.interval = v;
                    RecurrencePickerDialog.this.updateIntervalText();
                    RecurrencePickerDialog.this.mInterval.requestLayout();
                }
            }
        });
        this.mIntervalPreText = (TextView) this.mView.findViewById(R.id.intervalPreText);
        this.mIntervalPostText = (TextView) this.mView.findViewById(R.id.intervalPostText);
        this.mEndNeverStr = this.mResources.getString(R.string.recurrence_end_continously);
        this.mEndDateLabel = this.mResources.getString(R.string.recurrence_end_date_label);
        this.mEndCountLabel = this.mResources.getString(R.string.recurrence_end_count_label);
        this.mEndSpinnerArray.add(this.mEndNeverStr);
        this.mEndSpinnerArray.add(this.mEndDateLabel);
        this.mEndSpinnerArray.add(this.mEndCountLabel);
        this.mEndSpinner = (Spinner) this.mView.findViewById(R.id.endSpinner);
        this.mEndSpinner.setOnItemSelectedListener(this);
        this.mEndSpinnerAdapter = new EndSpinnerAdapter(getActivity(), this.mEndSpinnerArray, R.layout.recurrencepicker_freq_item, R.layout.recurrencepicker_end_text);
        this.mEndSpinnerAdapter.setDropDownViewResource(R.layout.recurrencepicker_freq_item);
        this.mEndSpinner.setAdapter((SpinnerAdapter) this.mEndSpinnerAdapter);
        this.mEndCount = (EditText) this.mView.findViewById(R.id.endCount);
        this.mEndCount.addTextChangedListener(new minMaxTextWatcher(1, 5, 730) {
            @Override
            void onChange(int v) {
                if (RecurrencePickerDialog.this.mModel.endCount != v) {
                    RecurrencePickerDialog.this.mModel.endCount = v;
                    RecurrencePickerDialog.this.updateEndCountText();
                    RecurrencePickerDialog.this.mEndCount.requestLayout();
                }
            }
        });
        this.mPostEndCount = (TextView) this.mView.findViewById(R.id.postEndCount);
        this.mEndDateTextView = (TextView) this.mView.findViewById(R.id.endDate);
        this.mEndDateTextView.setOnClickListener(this);
        if (this.mModel.endDate == null) {
            this.mModel.endDate = new Time(this.mTime);
            switch (this.mModel.freq) {
                case 0:
                case 1:
                    this.mModel.endDate.month++;
                    break;
                case 2:
                    this.mModel.endDate.month += 3;
                    break;
                case 3:
                    this.mModel.endDate.year += 3;
                    break;
            }
            this.mModel.endDate.normalize(false);
        }
        this.mWeekGroup = (LinearLayout) this.mView.findViewById(R.id.weekGroup);
        this.mWeekGroup2 = (LinearLayout) this.mView.findViewById(R.id.weekGroup2);
        new DateFormatSymbols().getWeekdays();
        this.mMonthRepeatByDayOfWeekStrs = new String[7][];
        this.mMonthRepeatByDayOfWeekStrs[0] = this.mResources.getStringArray(R.array.repeat_by_nth_sun);
        this.mMonthRepeatByDayOfWeekStrs[1] = this.mResources.getStringArray(R.array.repeat_by_nth_mon);
        this.mMonthRepeatByDayOfWeekStrs[2] = this.mResources.getStringArray(R.array.repeat_by_nth_tues);
        this.mMonthRepeatByDayOfWeekStrs[3] = this.mResources.getStringArray(R.array.repeat_by_nth_wed);
        this.mMonthRepeatByDayOfWeekStrs[4] = this.mResources.getStringArray(R.array.repeat_by_nth_thurs);
        this.mMonthRepeatByDayOfWeekStrs[5] = this.mResources.getStringArray(R.array.repeat_by_nth_fri);
        this.mMonthRepeatByDayOfWeekStrs[6] = this.mResources.getStringArray(R.array.repeat_by_nth_sat);
        int idx = Utils.getFirstDayOfWeek(getActivity());
        String[] dayOfWeekString = new DateFormatSymbols().getShortWeekdays();
        if (this.mResources.getConfiguration().screenWidthDp > 450) {
            numOfButtonsInRow1 = 7;
            numOfButtonsInRow2 = 0;
            this.mWeekGroup2.setVisibility(8);
            this.mWeekGroup2.getChildAt(3).setVisibility(8);
        } else {
            numOfButtonsInRow1 = 4;
            numOfButtonsInRow2 = 3;
            this.mWeekGroup2.setVisibility(0);
            this.mWeekGroup2.getChildAt(3).setVisibility(4);
        }
        for (int i = 0; i < 7; i++) {
            if (i >= numOfButtonsInRow1) {
                this.mWeekGroup.getChildAt(i).setVisibility(8);
            } else {
                this.mWeekByDayButtons[idx] = (ToggleButton) this.mWeekGroup.getChildAt(i);
                this.mWeekByDayButtons[idx].setTextOff(dayOfWeekString[this.TIME_DAY_TO_CALENDAR_DAY[idx]]);
                this.mWeekByDayButtons[idx].setTextOn(dayOfWeekString[this.TIME_DAY_TO_CALENDAR_DAY[idx]]);
                this.mWeekByDayButtons[idx].setOnCheckedChangeListener(this);
                idx++;
                if (idx >= 7) {
                    idx = 0;
                }
            }
        }
        for (int i2 = 0; i2 < 3; i2++) {
            if (i2 >= numOfButtonsInRow2) {
                this.mWeekGroup2.getChildAt(i2).setVisibility(8);
            } else {
                this.mWeekByDayButtons[idx] = (ToggleButton) this.mWeekGroup2.getChildAt(i2);
                this.mWeekByDayButtons[idx].setTextOff(dayOfWeekString[this.TIME_DAY_TO_CALENDAR_DAY[idx]]);
                this.mWeekByDayButtons[idx].setTextOn(dayOfWeekString[this.TIME_DAY_TO_CALENDAR_DAY[idx]]);
                this.mWeekByDayButtons[idx].setOnCheckedChangeListener(this);
                idx++;
                if (idx >= 7) {
                    idx = 0;
                }
            }
        }
        this.mMonthGroup = (LinearLayout) this.mView.findViewById(R.id.monthGroup);
        this.mMonthRepeatByRadioGroup = (RadioGroup) this.mView.findViewById(R.id.monthGroup);
        this.mMonthRepeatByRadioGroup.setOnCheckedChangeListener(this);
        this.mRepeatMonthlyByNthDayOfWeek = (RadioButton) this.mView.findViewById(R.id.repeatMonthlyByNthDayOfTheWeek);
        this.mRepeatMonthlyByNthDayOfMonth = (RadioButton) this.mView.findViewById(R.id.repeatMonthlyByNthDayOfMonth);
        this.mDone = (Button) this.mView.findViewById(R.id.done);
        this.mDone.setOnClickListener(this);
        togglePickerOptions();
        updateDialog();
        if (endCountHasFocus) {
            this.mEndCount.requestFocus();
        }
        return this.mView;
    }

    private void togglePickerOptions() {
        if (this.mModel.recurrenceState == 0) {
            this.mFreqSpinner.setEnabled(false);
            this.mEndSpinner.setEnabled(false);
            this.mIntervalPreText.setEnabled(false);
            this.mInterval.setEnabled(false);
            this.mIntervalPostText.setEnabled(false);
            this.mMonthRepeatByRadioGroup.setEnabled(false);
            this.mEndCount.setEnabled(false);
            this.mPostEndCount.setEnabled(false);
            this.mEndDateTextView.setEnabled(false);
            this.mRepeatMonthlyByNthDayOfWeek.setEnabled(false);
            this.mRepeatMonthlyByNthDayOfMonth.setEnabled(false);
            Button[] arr$ = this.mWeekByDayButtons;
            for (Button button : arr$) {
                button.setEnabled(false);
            }
        } else {
            this.mView.findViewById(R.id.options).setEnabled(true);
            this.mFreqSpinner.setEnabled(true);
            this.mEndSpinner.setEnabled(true);
            this.mIntervalPreText.setEnabled(true);
            this.mInterval.setEnabled(true);
            this.mIntervalPostText.setEnabled(true);
            this.mMonthRepeatByRadioGroup.setEnabled(true);
            this.mEndCount.setEnabled(true);
            this.mPostEndCount.setEnabled(true);
            this.mEndDateTextView.setEnabled(true);
            this.mRepeatMonthlyByNthDayOfWeek.setEnabled(true);
            this.mRepeatMonthlyByNthDayOfMonth.setEnabled(true);
            Button[] arr$2 = this.mWeekByDayButtons;
            for (Button button2 : arr$2) {
                button2.setEnabled(true);
            }
        }
        updateDoneButtonState();
    }

    private void updateDoneButtonState() {
        if (this.mModel.recurrenceState == 0) {
            this.mDone.setEnabled(true);
            return;
        }
        if (this.mInterval.getText().toString().length() == 0) {
            this.mDone.setEnabled(false);
            return;
        }
        if (this.mEndCount.getVisibility() == 0 && this.mEndCount.getText().toString().length() == 0) {
            this.mDone.setEnabled(false);
            return;
        }
        if (this.mModel.freq == 1) {
            CompoundButton[] arr$ = this.mWeekByDayButtons;
            for (CompoundButton b : arr$) {
                if (b.isChecked()) {
                    this.mDone.setEnabled(true);
                    return;
                }
            }
            this.mDone.setEnabled(false);
            return;
        }
        this.mDone.setEnabled(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("bundle_model", this.mModel);
        if (this.mEndCount.hasFocus()) {
            outState.putBoolean("bundle_end_count_has_focus", true);
        }
    }

    public void updateDialog() {
        String intervalStr = Integer.toString(this.mModel.interval);
        if (!intervalStr.equals(this.mInterval.getText().toString())) {
            this.mInterval.setText(intervalStr);
        }
        this.mFreqSpinner.setSelection(this.mModel.freq);
        this.mWeekGroup.setVisibility(this.mModel.freq == 1 ? 0 : 8);
        this.mWeekGroup2.setVisibility(this.mModel.freq == 1 ? 0 : 8);
        this.mMonthGroup.setVisibility(this.mModel.freq == 2 ? 0 : 8);
        switch (this.mModel.freq) {
            case 0:
                this.mIntervalResId = R.plurals.recurrence_interval_daily;
                break;
            case 1:
                this.mIntervalResId = R.plurals.recurrence_interval_weekly;
                for (int i = 0; i < 7; i++) {
                    this.mWeekByDayButtons[i].setChecked(this.mModel.weeklyByDayOfWeek[i]);
                }
                break;
            case 2:
                this.mIntervalResId = R.plurals.recurrence_interval_monthly;
                if (this.mModel.monthlyRepeat == 0) {
                    this.mMonthRepeatByRadioGroup.check(R.id.repeatMonthlyByNthDayOfMonth);
                } else if (this.mModel.monthlyRepeat == 1) {
                    this.mMonthRepeatByRadioGroup.check(R.id.repeatMonthlyByNthDayOfTheWeek);
                }
                if (this.mMonthRepeatByDayOfWeekStr == null) {
                    if (this.mModel.monthlyByNthDayOfWeek == 0) {
                        this.mModel.monthlyByNthDayOfWeek = (this.mTime.monthDay + 6) / 7;
                        if (this.mModel.monthlyByNthDayOfWeek >= 5) {
                            this.mModel.monthlyByNthDayOfWeek = -1;
                        }
                        this.mModel.monthlyByDayOfWeek = this.mTime.weekDay;
                    }
                    String[] monthlyByNthDayOfWeekStrs = this.mMonthRepeatByDayOfWeekStrs[this.mModel.monthlyByDayOfWeek];
                    int msgIndex = this.mModel.monthlyByNthDayOfWeek >= 0 ? this.mModel.monthlyByNthDayOfWeek : 5;
                    this.mMonthRepeatByDayOfWeekStr = monthlyByNthDayOfWeekStrs[msgIndex - 1];
                    this.mRepeatMonthlyByNthDayOfWeek.setText(this.mMonthRepeatByDayOfWeekStr);
                }
                break;
            case 3:
                this.mIntervalResId = R.plurals.recurrence_interval_yearly;
                break;
        }
        updateIntervalText();
        updateDoneButtonState();
        this.mEndSpinner.setSelection(this.mModel.end);
        if (this.mModel.end == 1) {
            String dateStr = DateUtils.formatDateTime(getActivity(), this.mModel.endDate.toMillis(false), 131072);
            this.mEndDateTextView.setText(dateStr);
        } else if (this.mModel.end == 2) {
            String countStr = Integer.toString(this.mModel.endCount);
            if (!countStr.equals(this.mEndCount.getText().toString())) {
                this.mEndCount.setText(countStr);
            }
        }
    }

    private void updateIntervalText() {
        String intervalString;
        int markerStart;
        if (this.mIntervalResId != -1 && (markerStart = (intervalString = this.mResources.getQuantityString(this.mIntervalResId, this.mModel.interval)).indexOf("%d")) != -1) {
            int postTextStart = markerStart + "%d".length();
            this.mIntervalPostText.setText(intervalString.substring(postTextStart, intervalString.length()).trim());
            this.mIntervalPreText.setText(intervalString.substring(0, markerStart).trim());
        }
    }

    private void updateEndCountText() {
        String endString = this.mResources.getQuantityString(R.plurals.recurrence_end_count, this.mModel.endCount);
        int markerStart = endString.indexOf("%d");
        if (markerStart != -1) {
            if (markerStart == 0) {
                Log.e("RecurrencePickerDialog", "No text to put in to recurrence's end spinner.");
            } else {
                int postTextStart = markerStart + "%d".length();
                this.mPostEndCount.setText(endString.substring(postTextStart, endString.length()).trim());
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == this.mFreqSpinner) {
            this.mModel.freq = position;
        } else if (parent == this.mEndSpinner) {
            switch (position) {
                case 0:
                    this.mModel.end = 0;
                    break;
                case 1:
                    this.mModel.end = 1;
                    break;
                case 2:
                    this.mModel.end = 2;
                    if (this.mModel.endCount <= 1) {
                        this.mModel.endCount = 1;
                    } else if (this.mModel.endCount > 730) {
                        this.mModel.endCount = 730;
                    }
                    updateEndCountText();
                    break;
            }
            this.mEndCount.setVisibility(this.mModel.end == 2 ? 0 : 8);
            this.mEndDateTextView.setVisibility(this.mModel.end == 1 ? 0 : 8);
            this.mPostEndCount.setVisibility((this.mModel.end != 2 || this.mHidePostEndCount) ? 8 : 0);
        }
        updateDialog();
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        if (this.mModel.endDate == null) {
            this.mModel.endDate = new Time(this.mTime.timezone);
            Time time = this.mModel.endDate;
            Time time2 = this.mModel.endDate;
            this.mModel.endDate.second = 0;
            time2.minute = 0;
            time.hour = 0;
        }
        this.mModel.endDate.year = year;
        this.mModel.endDate.month = monthOfYear;
        this.mModel.endDate.monthDay = dayOfMonth;
        this.mModel.endDate.normalize(false);
        updateDialog();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int itemIdx = -1;
        for (int i = 0; i < 7; i++) {
            if (itemIdx == -1 && buttonView == this.mWeekByDayButtons[i]) {
                itemIdx = i;
                this.mModel.weeklyByDayOfWeek[i] = isChecked;
            }
        }
        updateDialog();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.repeatMonthlyByNthDayOfMonth) {
            this.mModel.monthlyRepeat = 0;
        } else if (checkedId == R.id.repeatMonthlyByNthDayOfTheWeek) {
            this.mModel.monthlyRepeat = 1;
        }
        updateDialog();
    }

    @Override
    public void onClick(View v) {
        String rrule;
        if (this.mEndDateTextView == v) {
            if (this.mDatePickerDialog != null) {
                this.mDatePickerDialog.dismiss();
            }
            this.mDatePickerDialog = DatePickerDialog.newInstance(this, this.mModel.endDate.year, this.mModel.endDate.month, this.mModel.endDate.monthDay);
            this.mDatePickerDialog.setFirstDayOfWeek(Utils.getFirstDayOfWeekAsCalendar(getActivity()));
            this.mDatePickerDialog.setYearRange(1970, 2036);
            this.mDatePickerDialog.show(getFragmentManager(), "tag_date_picker_frag");
            return;
        }
        if (this.mDone == v) {
            if (this.mModel.recurrenceState == 0) {
                rrule = null;
            } else {
                copyModelToEventRecurrence(this.mModel, this.mRecurrence);
                rrule = this.mRecurrence.toString();
            }
            this.mRecurrenceSetListener.onRecurrenceSet(rrule);
            dismiss();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mDatePickerDialog = (DatePickerDialog) getFragmentManager().findFragmentByTag("tag_date_picker_frag");
        if (this.mDatePickerDialog != null) {
            this.mDatePickerDialog.setOnDateSetListener(this);
        }
    }

    public void setOnRecurrenceSetListener(OnRecurrenceSetListener l) {
        this.mRecurrenceSetListener = l;
    }

    private class EndSpinnerAdapter extends ArrayAdapter<CharSequence> {
        final String END_COUNT_MARKER;
        final String END_DATE_MARKER;
        private String mEndDateString;
        private LayoutInflater mInflater;
        private int mItemResourceId;
        private ArrayList<CharSequence> mStrings;
        private int mTextResourceId;
        private boolean mUseFormStrings;

        public EndSpinnerAdapter(Context context, ArrayList<CharSequence> strings, int itemResourceId, int textResourceId) {
            super(context, itemResourceId, strings);
            this.END_DATE_MARKER = "%s";
            this.END_COUNT_MARKER = "%d";
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mItemResourceId = itemResourceId;
            this.mTextResourceId = textResourceId;
            this.mStrings = strings;
            this.mEndDateString = RecurrencePickerDialog.this.getResources().getString(R.string.recurrence_end_date);
            int markerStart = this.mEndDateString.indexOf("%s");
            if (markerStart <= 0) {
                this.mUseFormStrings = true;
            } else {
                String countEndStr = RecurrencePickerDialog.this.getResources().getQuantityString(R.plurals.recurrence_end_count, 1);
                int markerStart2 = countEndStr.indexOf("%d");
                if (markerStart2 <= 0) {
                    this.mUseFormStrings = true;
                }
            }
            if (this.mUseFormStrings) {
                RecurrencePickerDialog.this.mEndSpinner.setLayoutParams(new TableLayout.LayoutParams(0, -2, 1.0f));
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = this.mInflater.inflate(this.mTextResourceId, parent, false);
            } else {
                v = convertView;
            }
            TextView item = (TextView) v.findViewById(R.id.spinner_item);
            switch (position) {
                case 0:
                    item.setText(this.mStrings.get(0));
                    break;
                case 1:
                    int markerStart = this.mEndDateString.indexOf("%s");
                    if (markerStart != -1) {
                        if (this.mUseFormStrings || markerStart == 0) {
                            item.setText(RecurrencePickerDialog.this.mEndDateLabel);
                        } else {
                            item.setText(this.mEndDateString.substring(0, markerStart).trim());
                        }
                    }
                    break;
                case 2:
                    String endString = RecurrencePickerDialog.this.mResources.getQuantityString(R.plurals.recurrence_end_count, RecurrencePickerDialog.this.mModel.endCount);
                    int markerStart2 = endString.indexOf("%d");
                    if (markerStart2 != -1) {
                        if (this.mUseFormStrings || markerStart2 == 0) {
                            item.setText(RecurrencePickerDialog.this.mEndCountLabel);
                            RecurrencePickerDialog.this.mPostEndCount.setVisibility(8);
                            RecurrencePickerDialog.this.mHidePostEndCount = true;
                        } else {
                            int postTextStart = markerStart2 + "%d".length();
                            RecurrencePickerDialog.this.mPostEndCount.setText(endString.substring(postTextStart, endString.length()).trim());
                            if (RecurrencePickerDialog.this.mModel.end == 2) {
                                RecurrencePickerDialog.this.mPostEndCount.setVisibility(0);
                            }
                            if (endString.charAt(markerStart2 - 1) == ' ') {
                                markerStart2--;
                            }
                            item.setText(endString.substring(0, markerStart2).trim());
                        }
                    }
                    break;
            }
            return v;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = this.mInflater.inflate(this.mItemResourceId, parent, false);
            } else {
                v = convertView;
            }
            TextView item = (TextView) v.findViewById(R.id.spinner_item);
            item.setText(this.mStrings.get(position));
            return v;
        }
    }
}
