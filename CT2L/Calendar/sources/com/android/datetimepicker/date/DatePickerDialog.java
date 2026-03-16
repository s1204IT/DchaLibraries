package com.android.datetimepicker.date;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.datetimepicker.HapticFeedbackController;
import com.android.datetimepicker.R;
import com.android.datetimepicker.Utils;
import com.android.datetimepicker.date.MonthAdapter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

public class DatePickerDialog extends DialogFragment implements View.OnClickListener, DatePickerController {
    private AccessibleDateAnimator mAnimator;
    private OnDateSetListener mCallBack;
    private TextView mDayOfWeekView;
    private String mDayPickerDescription;
    private DayPickerView mDayPickerView;
    private Button mDoneButton;
    private HapticFeedbackController mHapticFeedbackController;
    private Calendar mMaxDate;
    private Calendar mMinDate;
    private LinearLayout mMonthAndDayView;
    private String mSelectDay;
    private String mSelectYear;
    private TextView mSelectedDayTextView;
    private TextView mSelectedMonthTextView;
    private String mYearPickerDescription;
    private YearPickerView mYearPickerView;
    private TextView mYearView;
    private static SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy", Locale.getDefault());
    private static SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("dd", Locale.getDefault());
    private final Calendar mCalendar = Calendar.getInstance();
    private HashSet<OnDateChangedListener> mListeners = new HashSet<>();
    private int mCurrentView = -1;
    private int mWeekStart = this.mCalendar.getFirstDayOfWeek();
    private int mMinYear = 1900;
    private int mMaxYear = 2100;
    private boolean mDelayAnimation = true;

    public interface OnDateChangedListener {
        void onDateChanged();
    }

    public interface OnDateSetListener {
        void onDateSet(DatePickerDialog datePickerDialog, int i, int i2, int i3);
    }

    public static DatePickerDialog newInstance(OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth) {
        DatePickerDialog ret = new DatePickerDialog();
        ret.initialize(callBack, year, monthOfYear, dayOfMonth);
        return ret;
    }

    public void initialize(OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth) {
        this.mCallBack = callBack;
        this.mCalendar.set(1, year);
        this.mCalendar.set(2, monthOfYear);
        this.mCalendar.set(5, dayOfMonth);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        activity.getWindow().setSoftInputMode(3);
        if (savedInstanceState != null) {
            this.mCalendar.set(1, savedInstanceState.getInt("year"));
            this.mCalendar.set(2, savedInstanceState.getInt("month"));
            this.mCalendar.set(5, savedInstanceState.getInt("day"));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("year", this.mCalendar.get(1));
        outState.putInt("month", this.mCalendar.get(2));
        outState.putInt("day", this.mCalendar.get(5));
        outState.putInt("week_start", this.mWeekStart);
        outState.putInt("year_start", this.mMinYear);
        outState.putInt("year_end", this.mMaxYear);
        outState.putInt("current_view", this.mCurrentView);
        int listPosition = -1;
        if (this.mCurrentView == 0) {
            listPosition = this.mDayPickerView.getMostVisiblePosition();
        } else if (this.mCurrentView == 1) {
            listPosition = this.mYearPickerView.getFirstVisiblePosition();
            outState.putInt("list_position_offset", this.mYearPickerView.getFirstPositionOffset());
        }
        outState.putInt("list_position", listPosition);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("DatePickerDialog", "onCreateView: ");
        getDialog().getWindow().requestFeature(1);
        View view = inflater.inflate(R.layout.date_picker_dialog, (ViewGroup) null);
        this.mDayOfWeekView = (TextView) view.findViewById(R.id.date_picker_header);
        this.mMonthAndDayView = (LinearLayout) view.findViewById(R.id.date_picker_month_and_day);
        this.mMonthAndDayView.setOnClickListener(this);
        this.mSelectedMonthTextView = (TextView) view.findViewById(R.id.date_picker_month);
        this.mSelectedDayTextView = (TextView) view.findViewById(R.id.date_picker_day);
        this.mYearView = (TextView) view.findViewById(R.id.date_picker_year);
        this.mYearView.setOnClickListener(this);
        int listPosition = -1;
        int listPositionOffset = 0;
        int currentView = 0;
        if (savedInstanceState != null) {
            this.mWeekStart = savedInstanceState.getInt("week_start");
            this.mMinYear = savedInstanceState.getInt("year_start");
            this.mMaxYear = savedInstanceState.getInt("year_end");
            currentView = savedInstanceState.getInt("current_view");
            listPosition = savedInstanceState.getInt("list_position");
            listPositionOffset = savedInstanceState.getInt("list_position_offset");
        }
        Activity activity = getActivity();
        this.mDayPickerView = new SimpleDayPickerView(activity, this);
        this.mYearPickerView = new YearPickerView(activity, this);
        Resources res = getResources();
        this.mDayPickerDescription = res.getString(R.string.day_picker_description);
        this.mSelectDay = res.getString(R.string.select_day);
        this.mYearPickerDescription = res.getString(R.string.year_picker_description);
        this.mSelectYear = res.getString(R.string.select_year);
        this.mAnimator = (AccessibleDateAnimator) view.findViewById(R.id.animator);
        this.mAnimator.addView(this.mDayPickerView);
        this.mAnimator.addView(this.mYearPickerView);
        this.mAnimator.setDateMillis(this.mCalendar.getTimeInMillis());
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(300L);
        this.mAnimator.setInAnimation(animation);
        Animation animation2 = new AlphaAnimation(1.0f, 0.0f);
        animation2.setDuration(300L);
        this.mAnimator.setOutAnimation(animation2);
        this.mDoneButton = (Button) view.findViewById(R.id.done);
        this.mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog.this.tryVibrate();
                if (DatePickerDialog.this.mCallBack != null) {
                    DatePickerDialog.this.mCallBack.onDateSet(DatePickerDialog.this, DatePickerDialog.this.mCalendar.get(1), DatePickerDialog.this.mCalendar.get(2), DatePickerDialog.this.mCalendar.get(5));
                }
                DatePickerDialog.this.dismiss();
            }
        });
        updateDisplay(false);
        setCurrentView(currentView);
        if (listPosition != -1) {
            if (currentView == 0) {
                this.mDayPickerView.postSetSelection(listPosition);
            } else if (currentView == 1) {
                this.mYearPickerView.postSetSelectionFromTop(listPosition, listPositionOffset);
            }
        }
        this.mHapticFeedbackController = new HapticFeedbackController(activity);
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

    private void setCurrentView(int viewIndex) {
        long millis = this.mCalendar.getTimeInMillis();
        switch (viewIndex) {
            case 0:
                ObjectAnimator pulseAnimator = Utils.getPulseAnimator(this.mMonthAndDayView, 0.9f, 1.05f);
                if (this.mDelayAnimation) {
                    pulseAnimator.setStartDelay(500L);
                    this.mDelayAnimation = false;
                }
                this.mDayPickerView.onDateChanged();
                if (this.mCurrentView != viewIndex) {
                    this.mMonthAndDayView.setSelected(true);
                    this.mYearView.setSelected(false);
                    this.mAnimator.setDisplayedChild(0);
                    this.mCurrentView = viewIndex;
                }
                pulseAnimator.start();
                String dayString = DateUtils.formatDateTime(getActivity(), millis, 16);
                this.mAnimator.setContentDescription(this.mDayPickerDescription + ": " + dayString);
                Utils.tryAccessibilityAnnounce(this.mAnimator, this.mSelectDay);
                break;
            case 1:
                ObjectAnimator pulseAnimator2 = Utils.getPulseAnimator(this.mYearView, 0.85f, 1.1f);
                if (this.mDelayAnimation) {
                    pulseAnimator2.setStartDelay(500L);
                    this.mDelayAnimation = false;
                }
                this.mYearPickerView.onDateChanged();
                if (this.mCurrentView != viewIndex) {
                    this.mMonthAndDayView.setSelected(false);
                    this.mYearView.setSelected(true);
                    this.mAnimator.setDisplayedChild(1);
                    this.mCurrentView = viewIndex;
                }
                pulseAnimator2.start();
                CharSequence yearString = YEAR_FORMAT.format(Long.valueOf(millis));
                this.mAnimator.setContentDescription(this.mYearPickerDescription + ": " + ((Object) yearString));
                Utils.tryAccessibilityAnnounce(this.mAnimator, this.mSelectYear);
                break;
        }
    }

    private void updateDisplay(boolean announce) {
        if (this.mDayOfWeekView != null) {
            this.mDayOfWeekView.setText(this.mCalendar.getDisplayName(7, 2, Locale.getDefault()).toUpperCase(Locale.getDefault()));
        }
        this.mSelectedMonthTextView.setText(this.mCalendar.getDisplayName(2, 1, Locale.getDefault()).toUpperCase(Locale.getDefault()));
        this.mSelectedDayTextView.setText(DAY_FORMAT.format(this.mCalendar.getTime()));
        this.mYearView.setText(YEAR_FORMAT.format(this.mCalendar.getTime()));
        long millis = this.mCalendar.getTimeInMillis();
        this.mAnimator.setDateMillis(millis);
        String monthAndDayText = DateUtils.formatDateTime(getActivity(), millis, 24);
        this.mMonthAndDayView.setContentDescription(monthAndDayText);
        if (announce) {
            String fullDateText = DateUtils.formatDateTime(getActivity(), millis, 20);
            Utils.tryAccessibilityAnnounce(this.mAnimator, fullDateText);
        }
    }

    public void setFirstDayOfWeek(int startOfWeek) {
        if (startOfWeek < 1 || startOfWeek > 7) {
            throw new IllegalArgumentException("Value must be between Calendar.SUNDAY and Calendar.SATURDAY");
        }
        this.mWeekStart = startOfWeek;
        if (this.mDayPickerView != null) {
            this.mDayPickerView.onChange();
        }
    }

    public void setYearRange(int startYear, int endYear) {
        if (endYear <= startYear) {
            throw new IllegalArgumentException("Year end must be larger than year start");
        }
        this.mMinYear = startYear;
        this.mMaxYear = endYear;
        if (this.mDayPickerView != null) {
            this.mDayPickerView.onChange();
        }
    }

    @Override
    public Calendar getMinDate() {
        return this.mMinDate;
    }

    @Override
    public Calendar getMaxDate() {
        return this.mMaxDate;
    }

    public void setOnDateSetListener(OnDateSetListener listener) {
        this.mCallBack = listener;
    }

    private void adjustDayInMonthIfNeeded(int month, int year) {
        int day = this.mCalendar.get(5);
        int daysInMonth = Utils.getDaysInMonth(month, year);
        if (day > daysInMonth) {
            this.mCalendar.set(5, daysInMonth);
        }
    }

    @Override
    public void onClick(View v) {
        tryVibrate();
        if (v.getId() == R.id.date_picker_year) {
            setCurrentView(1);
        } else if (v.getId() == R.id.date_picker_month_and_day) {
            setCurrentView(0);
        }
    }

    @Override
    public void onYearSelected(int year) {
        adjustDayInMonthIfNeeded(this.mCalendar.get(2), year);
        this.mCalendar.set(1, year);
        updatePickers();
        setCurrentView(0);
        updateDisplay(true);
    }

    @Override
    public void onDayOfMonthSelected(int year, int month, int day) {
        this.mCalendar.set(1, year);
        this.mCalendar.set(2, month);
        this.mCalendar.set(5, day);
        updatePickers();
        updateDisplay(true);
    }

    private void updatePickers() {
        Iterator<OnDateChangedListener> iterator = this.mListeners.iterator();
        while (iterator.hasNext()) {
            iterator.next().onDateChanged();
        }
    }

    @Override
    public MonthAdapter.CalendarDay getSelectedDay() {
        return new MonthAdapter.CalendarDay(this.mCalendar);
    }

    @Override
    public int getMinYear() {
        return this.mMinYear;
    }

    @Override
    public int getMaxYear() {
        return this.mMaxYear;
    }

    @Override
    public int getFirstDayOfWeek() {
        return this.mWeekStart;
    }

    @Override
    public void registerOnDateChangedListener(OnDateChangedListener listener) {
        this.mListeners.add(listener);
    }

    @Override
    public void tryVibrate() {
        this.mHapticFeedbackController.tryVibrate();
    }
}
