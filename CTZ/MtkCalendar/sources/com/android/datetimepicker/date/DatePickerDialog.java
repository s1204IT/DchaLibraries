package com.android.datetimepicker.date;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
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
        void onCancelDialog();

        void onDateSet(DatePickerDialog datePickerDialog, int i, int i2, int i3);
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        Log.d("DatePickerDialog", "amit dpd onCancel: ");
        if (this.mCallBack != null) {
            this.mCallBack.onCancelDialog();
            Log.d("DatePickerDialog", "amit Callback onCancelDialog: ");
        }
    }

    public static DatePickerDialog newInstance(OnDateSetListener onDateSetListener, int i, int i2, int i3) {
        DatePickerDialog datePickerDialog = new DatePickerDialog();
        datePickerDialog.initialize(onDateSetListener, i, i2, i3);
        return datePickerDialog;
    }

    public void initialize(OnDateSetListener onDateSetListener, int i, int i2, int i3) {
        this.mCallBack = onDateSetListener;
        this.mCalendar.set(1, i);
        this.mCalendar.set(2, i2);
        this.mCalendar.set(5, i3);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().getWindow().setSoftInputMode(3);
        if (bundle != null) {
            this.mCalendar.set(1, bundle.getInt("year"));
            this.mCalendar.set(2, bundle.getInt("month"));
            this.mCalendar.set(5, bundle.getInt("day"));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        int firstVisiblePosition;
        super.onSaveInstanceState(bundle);
        bundle.putInt("year", this.mCalendar.get(1));
        bundle.putInt("month", this.mCalendar.get(2));
        bundle.putInt("day", this.mCalendar.get(5));
        bundle.putInt("week_start", this.mWeekStart);
        bundle.putInt("year_start", this.mMinYear);
        bundle.putInt("year_end", this.mMaxYear);
        bundle.putInt("current_view", this.mCurrentView);
        if (this.mCurrentView == 0) {
            firstVisiblePosition = this.mDayPickerView.getMostVisiblePosition();
        } else if (this.mCurrentView == 1) {
            firstVisiblePosition = this.mYearPickerView.getFirstVisiblePosition();
            bundle.putInt("list_position_offset", this.mYearPickerView.getFirstPositionOffset());
        } else {
            firstVisiblePosition = -1;
        }
        bundle.putInt("list_position", firstVisiblePosition);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        int i;
        int i2;
        int i3;
        Log.d("DatePickerDialog", "onCreateView: ");
        getDialog().getWindow().requestFeature(1);
        View viewInflate = layoutInflater.inflate(R.layout.date_picker_dialog, (ViewGroup) null);
        this.mDayOfWeekView = (TextView) viewInflate.findViewById(R.id.date_picker_header);
        this.mMonthAndDayView = (LinearLayout) viewInflate.findViewById(R.id.date_picker_month_and_day);
        this.mMonthAndDayView.setOnClickListener(this);
        this.mSelectedMonthTextView = (TextView) viewInflate.findViewById(R.id.date_picker_month);
        this.mSelectedDayTextView = (TextView) viewInflate.findViewById(R.id.date_picker_day);
        this.mYearView = (TextView) viewInflate.findViewById(R.id.date_picker_year);
        this.mYearView.setOnClickListener(this);
        if (bundle != null) {
            this.mWeekStart = bundle.getInt("week_start");
            this.mMinYear = bundle.getInt("year_start");
            this.mMaxYear = bundle.getInt("year_end");
            i3 = bundle.getInt("current_view");
            i = bundle.getInt("list_position");
            i2 = bundle.getInt("list_position_offset");
        } else {
            i = -1;
            i2 = 0;
            i3 = 0;
        }
        Activity activity = getActivity();
        this.mDayPickerView = new SimpleDayPickerView(activity, this);
        this.mYearPickerView = new YearPickerView(activity, this);
        Resources resources = getResources();
        this.mDayPickerDescription = resources.getString(R.string.day_picker_description);
        this.mSelectDay = resources.getString(R.string.select_day);
        this.mYearPickerDescription = resources.getString(R.string.year_picker_description);
        this.mSelectYear = resources.getString(R.string.select_year);
        this.mAnimator = (AccessibleDateAnimator) viewInflate.findViewById(R.id.animator);
        this.mAnimator.addView(this.mDayPickerView);
        this.mAnimator.addView(this.mYearPickerView);
        this.mAnimator.setDateMillis(this.mCalendar.getTimeInMillis());
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setDuration(300L);
        this.mAnimator.setInAnimation(alphaAnimation);
        AlphaAnimation alphaAnimation2 = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimation2.setDuration(300L);
        this.mAnimator.setOutAnimation(alphaAnimation2);
        this.mDoneButton = (Button) viewInflate.findViewById(R.id.done);
        this.mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog.this.tryVibrate();
                if (DatePickerDialog.this.mCallBack != null) {
                    DatePickerDialog.this.mCallBack.onDateSet(DatePickerDialog.this, DatePickerDialog.this.mCalendar.get(1), DatePickerDialog.this.mCalendar.get(2), DatePickerDialog.this.mCalendar.get(5));
                }
                DatePickerDialog.this.dismiss();
            }
        });
        updateDisplay(false);
        setCurrentView(i3);
        if (i != -1) {
            if (i3 == 0) {
                this.mDayPickerView.postSetSelection(i);
            } else if (i3 == 1) {
                this.mYearPickerView.postSetSelectionFromTop(i, i2);
            }
        }
        this.mHapticFeedbackController = new HapticFeedbackController(activity);
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

    private void setCurrentView(int i) {
        long timeInMillis = this.mCalendar.getTimeInMillis();
        switch (i) {
            case 0:
                ObjectAnimator pulseAnimator = Utils.getPulseAnimator(this.mMonthAndDayView, 0.9f, 1.05f);
                if (this.mDelayAnimation) {
                    pulseAnimator.setStartDelay(500L);
                    this.mDelayAnimation = false;
                }
                this.mDayPickerView.onDateChanged();
                if (this.mCurrentView != i) {
                    this.mMonthAndDayView.setSelected(true);
                    this.mYearView.setSelected(false);
                    this.mAnimator.setDisplayedChild(0);
                    this.mCurrentView = i;
                }
                pulseAnimator.start();
                String dateTime = DateUtils.formatDateTime(getActivity(), timeInMillis, 16);
                this.mAnimator.setContentDescription(this.mDayPickerDescription + ": " + dateTime);
                Utils.tryAccessibilityAnnounce(this.mAnimator, this.mSelectDay);
                break;
            case 1:
                ObjectAnimator pulseAnimator2 = Utils.getPulseAnimator(this.mYearView, 0.85f, 1.1f);
                if (this.mDelayAnimation) {
                    pulseAnimator2.setStartDelay(500L);
                    this.mDelayAnimation = false;
                }
                this.mYearPickerView.onDateChanged();
                if (this.mCurrentView != i) {
                    this.mMonthAndDayView.setSelected(false);
                    this.mYearView.setSelected(true);
                    this.mAnimator.setDisplayedChild(1);
                    this.mCurrentView = i;
                }
                pulseAnimator2.start();
                String str = YEAR_FORMAT.format(Long.valueOf(timeInMillis));
                this.mAnimator.setContentDescription(this.mYearPickerDescription + ": " + ((Object) str));
                Utils.tryAccessibilityAnnounce(this.mAnimator, this.mSelectYear);
                break;
        }
    }

    private void updateDisplay(boolean z) {
        if (this.mDayOfWeekView != null) {
            this.mDayOfWeekView.setText(this.mCalendar.getDisplayName(7, 2, Locale.getDefault()).toUpperCase(Locale.getDefault()));
        }
        this.mSelectedMonthTextView.setText(this.mCalendar.getDisplayName(2, 1, Locale.getDefault()).toUpperCase(Locale.getDefault()));
        this.mSelectedDayTextView.setText(DAY_FORMAT.format(this.mCalendar.getTime()));
        this.mYearView.setText(YEAR_FORMAT.format(this.mCalendar.getTime()));
        long timeInMillis = this.mCalendar.getTimeInMillis();
        this.mAnimator.setDateMillis(timeInMillis);
        this.mMonthAndDayView.setContentDescription(DateUtils.formatDateTime(getActivity(), timeInMillis, 24));
        if (z) {
            Utils.tryAccessibilityAnnounce(this.mAnimator, DateUtils.formatDateTime(getActivity(), timeInMillis, 20));
        }
    }

    public void setFirstDayOfWeek(int i) {
        if (i < 1 || i > 7) {
            throw new IllegalArgumentException("Value must be between Calendar.SUNDAY and Calendar.SATURDAY");
        }
        this.mWeekStart = i;
        if (this.mDayPickerView != null) {
            this.mDayPickerView.onChange();
        }
    }

    public void setYearRange(int i, int i2) {
        if (i2 <= i) {
            throw new IllegalArgumentException("Year end must be larger than year start");
        }
        this.mMinYear = i;
        this.mMaxYear = i2;
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

    public void setOnDateSetListener(OnDateSetListener onDateSetListener) {
        this.mCallBack = onDateSetListener;
    }

    private void adjustDayInMonthIfNeeded(int i, int i2) {
        int i3 = this.mCalendar.get(5);
        int daysInMonth = Utils.getDaysInMonth(i, i2);
        if (i3 > daysInMonth) {
            this.mCalendar.set(5, daysInMonth);
        }
    }

    @Override
    public void onClick(View view) {
        tryVibrate();
        if (view.getId() == R.id.date_picker_year) {
            setCurrentView(1);
        } else if (view.getId() == R.id.date_picker_month_and_day) {
            setCurrentView(0);
        }
    }

    @Override
    public void onYearSelected(int i) {
        adjustDayInMonthIfNeeded(this.mCalendar.get(2), i);
        this.mCalendar.set(1, i);
        updatePickers();
        setCurrentView(0);
        updateDisplay(true);
    }

    @Override
    public void onDayOfMonthSelected(int i, int i2, int i3) {
        this.mCalendar.set(1, i);
        this.mCalendar.set(2, i2);
        this.mCalendar.set(5, i3);
        updatePickers();
        updateDisplay(true);
    }

    private void updatePickers() {
        Iterator<OnDateChangedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onDateChanged();
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
    public void registerOnDateChangedListener(OnDateChangedListener onDateChangedListener) {
        this.mListeners.add(onDateChangedListener);
    }

    @Override
    public void tryVibrate() {
        this.mHapticFeedbackController.tryVibrate();
    }
}
