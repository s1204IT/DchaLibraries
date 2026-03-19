package com.android.calendar.recurrencepicker;

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
    private long mTimeDuration;
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

    public static class RecurrenceModel implements Parcelable {
        public static final Parcelable.Creator<RecurrenceModel> CREATOR = new Parcelable.Creator<RecurrenceModel>() {
            @Override
            public RecurrenceModel createFromParcel(Parcel parcel) {
                return new RecurrenceModel(parcel);
            }

            @Override
            public RecurrenceModel[] newArray(int i) {
                return new RecurrenceModel[i];
            }
        };
        int end;
        int endCount;
        Time endDate;
        int freq;
        int interval;
        int monthlyByDayOfWeek;
        int monthlyByMonthDay;
        int monthlyByNthDayOfWeek;
        int monthlyRepeat;
        int recurrenceState;
        boolean[] weeklyByDayOfWeek;

        public String toString() {
            return "Model [freq=" + this.freq + ", interval=" + this.interval + ", end=" + this.end + ", endDate=" + this.endDate + ", endCount=" + this.endCount + ", weeklyByDayOfWeek=" + Arrays.toString(this.weeklyByDayOfWeek) + ", monthlyRepeat=" + this.monthlyRepeat + ", monthlyByMonthDay=" + this.monthlyByMonthDay + ", monthlyByDayOfWeek=" + this.monthlyByDayOfWeek + ", monthlyByNthDayOfWeek=" + this.monthlyByNthDayOfWeek + "]";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public RecurrenceModel() {
            this.freq = 1;
            this.interval = 1;
            this.endCount = 5;
            this.weeklyByDayOfWeek = new boolean[7];
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.freq);
            parcel.writeInt(this.interval);
            parcel.writeInt(this.end);
            parcel.writeInt(this.endDate.year);
            parcel.writeInt(this.endDate.month);
            parcel.writeInt(this.endDate.monthDay);
            parcel.writeInt(this.endCount);
            parcel.writeBooleanArray(this.weeklyByDayOfWeek);
            parcel.writeInt(this.monthlyRepeat);
            parcel.writeInt(this.monthlyByMonthDay);
            parcel.writeInt(this.monthlyByDayOfWeek);
            parcel.writeInt(this.monthlyByNthDayOfWeek);
            parcel.writeInt(this.recurrenceState);
        }

        private RecurrenceModel(Parcel parcel) {
            this.freq = 1;
            this.interval = 1;
            this.endCount = 5;
            this.weeklyByDayOfWeek = new boolean[7];
            this.freq = parcel.readInt();
            this.interval = parcel.readInt();
            this.end = parcel.readInt();
            if (this.endDate == null) {
                this.endDate = new Time();
            }
            this.endDate.year = parcel.readInt();
            this.endDate.month = parcel.readInt();
            this.endDate.monthDay = parcel.readInt();
            this.endCount = parcel.readInt();
            parcel.readBooleanArray(this.weeklyByDayOfWeek);
            this.monthlyRepeat = parcel.readInt();
            this.monthlyByMonthDay = parcel.readInt();
            this.monthlyByDayOfWeek = parcel.readInt();
            this.monthlyByNthDayOfWeek = parcel.readInt();
            this.recurrenceState = parcel.readInt();
        }
    }

    class minMaxTextWatcher implements TextWatcher {
        private int mDefault;
        private int mMax;
        private int mMin;

        public minMaxTextWatcher(int i, int i2, int i3) {
            this.mMin = i;
            this.mMax = i3;
            this.mDefault = i2;
        }

        @Override
        public void afterTextChanged(Editable editable) {
            int i;
            try {
                i = Integer.parseInt(editable.toString());
            } catch (NumberFormatException e) {
                i = this.mDefault;
            }
            boolean z = true;
            if (i < this.mMin) {
                i = this.mMin;
            } else if (i > this.mMax) {
                i = this.mMax;
            } else {
                z = false;
            }
            if (z) {
                editable.clear();
                editable.append((CharSequence) Integer.toString(i));
            }
            RecurrencePickerDialog.this.updateDoneButtonState();
            onChange(i);
        }

        void onChange(int i) {
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }
    }

    public static boolean isSupportedMonthlyByNthDayOfWeek(int i) {
        return (i > 0 && i <= 5) || i == -1;
    }

    public static boolean canHandleRecurrenceRule(EventRecurrence eventRecurrence) {
        switch (eventRecurrence.freq) {
            case 4:
            case 5:
            case 6:
            case 7:
                if (eventRecurrence.count <= 0 || TextUtils.isEmpty(eventRecurrence.until)) {
                    int i = 0;
                    for (int i2 = 0; i2 < eventRecurrence.bydayCount; i2++) {
                        if (isSupportedMonthlyByNthDayOfWeek(eventRecurrence.bydayNum[i2])) {
                            i++;
                        }
                    }
                    if (i <= 1) {
                        if ((i <= 0 || eventRecurrence.freq == 6) && eventRecurrence.bymonthdayCount <= 1) {
                            if (eventRecurrence.freq == 6) {
                                if (eventRecurrence.bydayCount <= 1) {
                                    if (eventRecurrence.bydayCount <= 0 || eventRecurrence.bymonthdayCount <= 0) {
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

    private static void copyEventRecurrenceToModel(EventRecurrence eventRecurrence, RecurrenceModel recurrenceModel) {
        switch (eventRecurrence.freq) {
            case 4:
                recurrenceModel.freq = 0;
                break;
            case 5:
                recurrenceModel.freq = 1;
                break;
            case 6:
                recurrenceModel.freq = 2;
                break;
            case 7:
                recurrenceModel.freq = 3;
                break;
            default:
                throw new IllegalStateException("freq=" + eventRecurrence.freq);
        }
        if (eventRecurrence.interval > 0) {
            recurrenceModel.interval = eventRecurrence.interval;
        }
        recurrenceModel.endCount = eventRecurrence.count;
        if (recurrenceModel.endCount > 0) {
            recurrenceModel.end = 2;
        }
        if (!TextUtils.isEmpty(eventRecurrence.until)) {
            if (recurrenceModel.endDate == null) {
                recurrenceModel.endDate = new Time();
            }
            try {
                recurrenceModel.endDate.parse(eventRecurrence.until);
            } catch (TimeFormatException e) {
                recurrenceModel.endDate = null;
            }
            if (recurrenceModel.end == 2 && recurrenceModel.endDate != null) {
                throw new IllegalStateException("freq=" + eventRecurrence.freq);
            }
            recurrenceModel.end = 1;
        }
        Arrays.fill(recurrenceModel.weeklyByDayOfWeek, false);
        if (eventRecurrence.bydayCount > 0) {
            int i = 0;
            for (int i2 = 0; i2 < eventRecurrence.bydayCount; i2++) {
                int iDay2TimeDay = EventRecurrence.day2TimeDay(eventRecurrence.byday[i2]);
                recurrenceModel.weeklyByDayOfWeek[iDay2TimeDay] = true;
                if (recurrenceModel.freq == 2 && isSupportedMonthlyByNthDayOfWeek(eventRecurrence.bydayNum[i2])) {
                    recurrenceModel.monthlyByDayOfWeek = iDay2TimeDay;
                    recurrenceModel.monthlyByNthDayOfWeek = eventRecurrence.bydayNum[i2];
                    recurrenceModel.monthlyRepeat = 1;
                    i++;
                }
            }
            if (recurrenceModel.freq == 2) {
                if (eventRecurrence.bydayCount != 1) {
                    throw new IllegalStateException("Can handle only 1 byDayOfWeek in monthly");
                }
                if (i != 1) {
                    throw new IllegalStateException("Didn't specify which nth day of week to repeat for a monthly");
                }
            }
        }
        if (recurrenceModel.freq == 2) {
            if (eventRecurrence.bymonthdayCount == 1) {
                if (recurrenceModel.monthlyRepeat == 1) {
                    throw new IllegalStateException("Can handle only by monthday or by nth day of week, not both");
                }
                recurrenceModel.monthlyByMonthDay = eventRecurrence.bymonthday[0];
                recurrenceModel.monthlyRepeat = 0;
                return;
            }
            if (eventRecurrence.bymonthCount > 1) {
                throw new IllegalStateException("Can handle only one bymonthday");
            }
        }
    }

    private static void copyModelToEventRecurrence(RecurrenceModel recurrenceModel, EventRecurrence eventRecurrence) {
        if (recurrenceModel.recurrenceState == 0) {
            throw new IllegalStateException("There's no recurrence");
        }
        eventRecurrence.freq = mFreqModelToEventRecurrence[recurrenceModel.freq];
        if (recurrenceModel.interval <= 1) {
            eventRecurrence.interval = 0;
        } else {
            eventRecurrence.interval = recurrenceModel.interval;
        }
        switch (recurrenceModel.end) {
            case 1:
                if (recurrenceModel.endDate != null) {
                    recurrenceModel.endDate.normalize(false);
                    eventRecurrence.until = recurrenceModel.endDate.format2445();
                    eventRecurrence.count = 0;
                } else {
                    throw new IllegalStateException("end = END_BY_DATE but endDate is null");
                }
                break;
            case 2:
                eventRecurrence.count = recurrenceModel.endCount;
                eventRecurrence.until = null;
                if (eventRecurrence.count <= 0) {
                    throw new IllegalStateException("count is " + eventRecurrence.count);
                }
                break;
            default:
                eventRecurrence.count = 0;
                eventRecurrence.until = null;
                break;
        }
        eventRecurrence.bydayCount = 0;
        eventRecurrence.bymonthdayCount = 0;
        switch (recurrenceModel.freq) {
            case 1:
                int i = 0;
                for (int i2 = 0; i2 < 7; i2++) {
                    if (recurrenceModel.weeklyByDayOfWeek[i2]) {
                        i++;
                    }
                }
                if (eventRecurrence.bydayCount < i || eventRecurrence.byday == null || eventRecurrence.bydayNum == null) {
                    eventRecurrence.byday = new int[i];
                    eventRecurrence.bydayNum = new int[i];
                }
                eventRecurrence.bydayCount = i;
                for (int i3 = 6; i3 >= 0; i3--) {
                    if (recurrenceModel.weeklyByDayOfWeek[i3]) {
                        i--;
                        eventRecurrence.bydayNum[i] = 0;
                        eventRecurrence.byday[i] = EventRecurrence.timeDay2Day(i3);
                    }
                }
                break;
            case 2:
                if (recurrenceModel.monthlyRepeat == 0) {
                    if (recurrenceModel.monthlyByMonthDay > 0) {
                        if (eventRecurrence.bymonthday == null || eventRecurrence.bymonthdayCount < 1) {
                            eventRecurrence.bymonthday = new int[1];
                        }
                        eventRecurrence.bymonthday[0] = recurrenceModel.monthlyByMonthDay;
                        eventRecurrence.bymonthdayCount = 1;
                    }
                } else if (recurrenceModel.monthlyRepeat == 1) {
                    if (!isSupportedMonthlyByNthDayOfWeek(recurrenceModel.monthlyByNthDayOfWeek)) {
                        throw new IllegalStateException("month repeat by nth week but n is " + recurrenceModel.monthlyByNthDayOfWeek);
                    }
                    if (eventRecurrence.bydayCount < 1 || eventRecurrence.byday == null || eventRecurrence.bydayNum == null) {
                        eventRecurrence.byday = new int[1];
                        eventRecurrence.bydayNum = new int[1];
                    }
                    eventRecurrence.bydayCount = 1;
                    eventRecurrence.byday[0] = EventRecurrence.timeDay2Day(recurrenceModel.monthlyByDayOfWeek);
                    eventRecurrence.bydayNum[0] = recurrenceModel.monthlyByNthDayOfWeek;
                }
                break;
        }
        if (!canHandleRecurrenceRule(eventRecurrence)) {
            throw new IllegalStateException("UI generated recurrence that it can't handle. ER:" + eventRecurrence.toString() + " Model: " + recurrenceModel.toString());
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        boolean z;
        this.mRecurrence.wkst = EventRecurrence.timeDay2Day(Utils.getFirstDayOfWeek(getActivity()));
        int i = 1;
        getDialog().getWindow().requestFeature(1);
        if (bundle != null) {
            RecurrenceModel recurrenceModel = (RecurrenceModel) bundle.get("bundle_model");
            if (recurrenceModel != null) {
                this.mModel = recurrenceModel;
            }
            z = bundle.getBoolean("bundle_end_count_has_focus");
            this.mTimeDuration = bundle.getLong("budle_event_time_duration", 0L);
            if (this.mModel != null && this.mModel.recurrenceState != 0) {
                copyModelToEventRecurrence(this.mModel, this.mRecurrence);
            }
        } else {
            Bundle arguments = getArguments();
            if (arguments != null) {
                this.mTime.set(arguments.getLong("bundle_event_start_time"));
                String string = arguments.getString("bundle_event_time_zone");
                if (!TextUtils.isEmpty(string)) {
                    this.mTime.timezone = string;
                }
                this.mTime.normalize(false);
                this.mTimeDuration = arguments.getLong("budle_event_time_duration", 0L);
                this.mModel.weeklyByDayOfWeek[this.mTime.weekDay] = true;
                String string2 = arguments.getString("bundle_event_rrule");
                if (!TextUtils.isEmpty(string2)) {
                    this.mModel.recurrenceState = 1;
                    this.mRecurrence.parse(string2);
                    copyEventRecurrenceToModel(this.mRecurrence, this.mModel);
                    if (this.mRecurrence.bydayCount == 0) {
                        this.mModel.weeklyByDayOfWeek[this.mTime.weekDay] = true;
                    }
                }
            } else {
                this.mTime.setToNow();
            }
            z = false;
        }
        this.mResources = getResources();
        this.mView = layoutInflater.inflate(R.layout.recurrencepicker, viewGroup, true);
        this.mRepeatSwitch = (Switch) this.mView.findViewById(R.id.repeat_switch);
        if (this.mModel != null) {
            this.mRepeatSwitch.setChecked(this.mModel.recurrenceState == 1);
        }
        this.mRepeatSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean z2) {
                RecurrencePickerDialog.this.mModel.recurrenceState = z2 ? 1 : 0;
                RecurrencePickerDialog.this.togglePickerOptions();
            }
        });
        this.mFreqSpinner = (Spinner) this.mView.findViewById(R.id.freqSpinner);
        this.mFreqSpinner.setOnItemSelectedListener(this);
        CharSequence[] textArray = getActivity().getResources().getTextArray(R.array.recurrence_freq);
        int minRepeatMode = getMinRepeatMode(this.mTimeDuration);
        if (minRepeatMode > 0) {
            textArray = (CharSequence[]) Arrays.copyOfRange(textArray, minRepeatMode - 4, 4);
        }
        ArrayAdapter arrayAdapter = new ArrayAdapter(getActivity(), R.layout.recurrencepicker_freq_item, textArray);
        arrayAdapter.setDropDownViewResource(R.layout.recurrencepicker_freq_item);
        this.mFreqSpinner.setAdapter((SpinnerAdapter) arrayAdapter);
        this.mInterval = (EditText) this.mView.findViewById(R.id.interval);
        this.mInterval.addTextChangedListener(new minMaxTextWatcher(i, i, 99) {
            @Override
            void onChange(int i2) {
                if (RecurrencePickerDialog.this.mIntervalResId != -1 && RecurrencePickerDialog.this.mInterval.getText().toString().length() > 0) {
                    RecurrencePickerDialog.this.mModel.interval = i2;
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
        this.mEndCount.addTextChangedListener(new minMaxTextWatcher(i, 5, 730) {
            @Override
            void onChange(int i2) {
                if (RecurrencePickerDialog.this.mModel.endCount != i2) {
                    RecurrencePickerDialog.this.mModel.endCount = i2;
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
        int firstDayOfWeek = Utils.getFirstDayOfWeek(getActivity());
        String[] shortWeekdays = new DateFormatSymbols().getShortWeekdays();
        this.mWeekGroup2.setVisibility(0);
        this.mWeekGroup2.getChildAt(3).setVisibility(4);
        int i2 = firstDayOfWeek;
        for (int i3 = 0; i3 < 7; i3++) {
            if (i3 >= 4) {
                this.mWeekGroup.getChildAt(i3).setVisibility(8);
            } else {
                this.mWeekByDayButtons[i2] = (ToggleButton) this.mWeekGroup.getChildAt(i3);
                this.mWeekByDayButtons[i2].setTextOff(shortWeekdays[this.TIME_DAY_TO_CALENDAR_DAY[i2]]);
                this.mWeekByDayButtons[i2].setTextOn(shortWeekdays[this.TIME_DAY_TO_CALENDAR_DAY[i2]]);
                this.mWeekByDayButtons[i2].setOnCheckedChangeListener(this);
                i2++;
                if (i2 >= 7) {
                    i2 = 0;
                }
            }
        }
        for (int i4 = 0; i4 < 3; i4++) {
            if (i4 >= 3) {
                this.mWeekGroup2.getChildAt(i4).setVisibility(8);
            } else {
                this.mWeekByDayButtons[i2] = (ToggleButton) this.mWeekGroup2.getChildAt(i4);
                this.mWeekByDayButtons[i2].setTextOff(shortWeekdays[this.TIME_DAY_TO_CALENDAR_DAY[i2]]);
                this.mWeekByDayButtons[i2].setTextOn(shortWeekdays[this.TIME_DAY_TO_CALENDAR_DAY[i2]]);
                this.mWeekByDayButtons[i2].setOnCheckedChangeListener(this);
                int i5 = i2 + 1;
                i2 = i5 >= 7 ? 0 : i5;
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
        if (z) {
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
            for (ToggleButton toggleButton : this.mWeekByDayButtons) {
                toggleButton.setEnabled(false);
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
            for (ToggleButton toggleButton2 : this.mWeekByDayButtons) {
                toggleButton2.setEnabled(true);
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
            for (ToggleButton toggleButton : this.mWeekByDayButtons) {
                if (toggleButton.isChecked()) {
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
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("bundle_model", this.mModel);
        if (this.mEndCount.hasFocus()) {
            bundle.putBoolean("bundle_end_count_has_focus", true);
        }
        bundle.putLong("budle_event_time_duration", this.mTimeDuration);
    }

    public void updateDialog() {
        String string = Integer.toString(this.mModel.interval);
        if (!string.equals(this.mInterval.getText().toString())) {
            this.mInterval.setText(string);
        }
        int minRepeatMode = getMinRepeatMode(this.mTimeDuration);
        int i = this.mModel.freq;
        if (minRepeatMode > 0) {
            i -= minRepeatMode - 4;
        }
        this.mFreqSpinner.setSelection(i);
        this.mWeekGroup.setVisibility(this.mModel.freq == 1 ? 0 : 8);
        this.mWeekGroup2.setVisibility(this.mModel.freq == 1 ? 0 : 8);
        this.mMonthGroup.setVisibility(this.mModel.freq == 2 ? 0 : 8);
        switch (this.mModel.freq) {
            case 0:
                this.mIntervalResId = R.plurals.recurrence_interval_daily;
                break;
            case 1:
                this.mIntervalResId = R.plurals.recurrence_interval_weekly;
                for (int i2 = 0; i2 < 7; i2++) {
                    this.mWeekByDayButtons[i2].setChecked(this.mModel.weeklyByDayOfWeek[i2]);
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
                    this.mMonthRepeatByDayOfWeekStr = this.mMonthRepeatByDayOfWeekStrs[this.mModel.monthlyByDayOfWeek][(this.mModel.monthlyByNthDayOfWeek >= 0 ? this.mModel.monthlyByNthDayOfWeek : 5) - 1];
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
            this.mEndDateTextView.setText(DateUtils.formatDateTime(getActivity(), this.mModel.endDate.toMillis(false), 131072));
        } else if (this.mModel.end == 2) {
            String string2 = Integer.toString(this.mModel.endCount);
            if (!string2.equals(this.mEndCount.getText().toString())) {
                this.mEndCount.setText(string2);
            }
        }
    }

    private void updateIntervalText() {
        String quantityString;
        int iIndexOf;
        if (this.mIntervalResId != -1 && (iIndexOf = (quantityString = this.mResources.getQuantityString(this.mIntervalResId, this.mModel.interval)).indexOf("%d")) != -1) {
            this.mIntervalPostText.setText(quantityString.substring("%d".length() + iIndexOf, quantityString.length()).trim());
            this.mIntervalPreText.setText(quantityString.substring(0, iIndexOf).trim());
        }
    }

    private void updateEndCountText() {
        String quantityString = this.mResources.getQuantityString(R.plurals.recurrence_end_count, this.mModel.endCount);
        int iIndexOf = quantityString.indexOf("%d");
        if (iIndexOf != -1) {
            if (iIndexOf == 0) {
                Log.e("RecurrencePickerDialog", "No text to put in to recurrence's end spinner.");
            } else {
                this.mPostEndCount.setText(quantityString.substring(iIndexOf + "%d".length(), quantityString.length()).trim());
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        if (adapterView == this.mFreqSpinner) {
            this.mModel.freq = i;
            int minRepeatMode = getMinRepeatMode(this.mTimeDuration);
            if (minRepeatMode > 0) {
                this.mModel.freq += minRepeatMode - 4;
            }
        } else if (adapterView == this.mEndSpinner) {
            switch (i) {
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
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int i, int i2, int i3) {
        if (this.mModel.endDate == null) {
            if (this.mModel.endDate.allDay) {
                this.mModel.endDate = new Time("UTC");
            } else {
                this.mModel.endDate = new Time(this.mTime.timezone);
            }
            Time time = this.mModel.endDate;
            Time time2 = this.mModel.endDate;
            this.mModel.endDate.second = 0;
            time2.minute = 0;
            time.hour = 0;
        }
        this.mModel.endDate.year = i;
        this.mModel.endDate.month = i2;
        this.mModel.endDate.monthDay = i3;
        this.mModel.endDate.normalize(false);
        updateDialog();
    }

    @Override
    public void onCancelDialog() {
        Log.d("RecurrencePickerDialog", "do nothing");
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        int i = -1;
        for (int i2 = 0; i2 < 7; i2++) {
            if (i == -1 && compoundButton == this.mWeekByDayButtons[i2]) {
                this.mModel.weeklyByDayOfWeek[i2] = z;
                i = i2;
            }
        }
        updateDialog();
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        if (i == R.id.repeatMonthlyByNthDayOfMonth) {
            this.mModel.monthlyRepeat = 0;
        } else if (i == R.id.repeatMonthlyByNthDayOfTheWeek) {
            this.mModel.monthlyRepeat = 1;
        }
        updateDialog();
    }

    @Override
    public void onClick(View view) {
        String string;
        if (this.mEndDateTextView == view) {
            if (this.mDatePickerDialog != null) {
                this.mDatePickerDialog.dismiss();
            }
            this.mDatePickerDialog = DatePickerDialog.newInstance(this, this.mModel.endDate.year, this.mModel.endDate.month, this.mModel.endDate.monthDay);
            this.mDatePickerDialog.setFirstDayOfWeek(Utils.getFirstDayOfWeekAsCalendar(getActivity()));
            this.mDatePickerDialog.setYearRange(1970, 2036);
            this.mDatePickerDialog.show(getFragmentManager(), "tag_date_picker_frag");
            return;
        }
        if (this.mDone == view) {
            if (this.mModel.recurrenceState == 0) {
                string = null;
            } else {
                copyModelToEventRecurrence(this.mModel, this.mRecurrence);
                string = this.mRecurrence.toString();
            }
            this.mRecurrenceSetListener.onRecurrenceSet(string);
            dismiss();
        }
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mDatePickerDialog = (DatePickerDialog) getFragmentManager().findFragmentByTag("tag_date_picker_frag");
        if (this.mDatePickerDialog != null) {
            this.mDatePickerDialog.setOnDateSetListener(this);
        }
    }

    public void setOnRecurrenceSetListener(OnRecurrenceSetListener onRecurrenceSetListener) {
        this.mRecurrenceSetListener = onRecurrenceSetListener;
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

        public EndSpinnerAdapter(Context context, ArrayList<CharSequence> arrayList, int i, int i2) {
            super(context, i, arrayList);
            this.END_DATE_MARKER = "%s";
            this.END_COUNT_MARKER = "%d";
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mItemResourceId = i;
            this.mTextResourceId = i2;
            this.mStrings = arrayList;
            this.mEndDateString = RecurrencePickerDialog.this.getResources().getString(R.string.recurrence_end_date);
            if (this.mEndDateString.indexOf("%s") <= 0 || RecurrencePickerDialog.this.getResources().getQuantityString(R.plurals.recurrence_end_count, 1).indexOf("%d") <= 0) {
                this.mUseFormStrings = true;
            }
            if (this.mUseFormStrings) {
                RecurrencePickerDialog.this.mEndSpinner.setLayoutParams(new TableLayout.LayoutParams(0, -2, 1.0f));
            }
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = this.mInflater.inflate(this.mTextResourceId, viewGroup, false);
            }
            TextView textView = (TextView) view.findViewById(R.id.spinner_item);
            switch (i) {
                case 0:
                    textView.setText(this.mStrings.get(0));
                    break;
                case 1:
                    int iIndexOf = this.mEndDateString.indexOf("%s");
                    if (iIndexOf != -1) {
                        if (this.mUseFormStrings || iIndexOf == 0) {
                            textView.setText(RecurrencePickerDialog.this.mEndDateLabel);
                        } else {
                            textView.setText(this.mEndDateString.substring(0, iIndexOf).trim());
                        }
                    }
                    break;
                case 2:
                    String quantityString = RecurrencePickerDialog.this.mResources.getQuantityString(R.plurals.recurrence_end_count, RecurrencePickerDialog.this.mModel.endCount);
                    int iIndexOf2 = quantityString.indexOf("%d");
                    if (iIndexOf2 != -1) {
                        if (this.mUseFormStrings || iIndexOf2 == 0) {
                            textView.setText(RecurrencePickerDialog.this.mEndCountLabel);
                            RecurrencePickerDialog.this.mPostEndCount.setVisibility(8);
                            RecurrencePickerDialog.this.mHidePostEndCount = true;
                        } else {
                            RecurrencePickerDialog.this.mPostEndCount.setText(quantityString.substring("%d".length() + iIndexOf2, quantityString.length()).trim());
                            if (RecurrencePickerDialog.this.mModel.end == 2) {
                                RecurrencePickerDialog.this.mPostEndCount.setVisibility(0);
                            }
                            if (quantityString.charAt(iIndexOf2 - 1) == ' ') {
                                iIndexOf2--;
                            }
                            textView.setText(quantityString.substring(0, iIndexOf2).trim());
                        }
                    }
                    break;
            }
            return view;
        }

        @Override
        public View getDropDownView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = this.mInflater.inflate(this.mItemResourceId, viewGroup, false);
            }
            ((TextView) view.findViewById(R.id.spinner_item)).setText(this.mStrings.get(i));
            return view;
        }
    }

    public static int getMinRepeatMode(long j) {
        if (j < 0 || j > 31449600000L) {
            return 0;
        }
        if (j > 2678400000L) {
            return 7;
        }
        if (j > 604800000) {
            return 6;
        }
        if (j > 86400000) {
            return 5;
        }
        return 4;
    }
}
