package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.ProxyInfo;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.DatePicker;
import android.widget.DayPickerView;
import com.android.internal.R;
import com.android.internal.widget.AccessibleDateAnimator;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;

class DatePickerCalendarDelegate extends DatePicker.AbstractDatePickerDelegate implements View.OnClickListener, DatePickerController {
    private static final int ANIMATION_DURATION = 300;
    private static final int DAY_INDEX = 1;
    private static final int DEFAULT_END_YEAR = 2100;
    private static final int DEFAULT_START_YEAR = 1900;
    private static final int MONTH_AND_DAY_VIEW = 0;
    private static final int MONTH_INDEX = 0;
    private static final int UNINITIALIZED = -1;
    private static final int USE_LOCALE = 0;
    private static final int YEAR_INDEX = 2;
    private static final int YEAR_VIEW = 1;
    private AccessibleDateAnimator mAnimator;
    private Calendar mCurrentDate;
    private int mCurrentView;
    private DatePicker.OnDateChangedListener mDateChangedListener;
    private SimpleDateFormat mDayFormat;
    private TextView mDayOfWeekView;
    private String mDayPickerDescription;
    private DayPickerView mDayPickerView;
    private int mFirstDayOfWeek;
    private TextView mHeaderDayOfMonthTextView;
    private TextView mHeaderMonthTextView;
    private TextView mHeaderYearTextView;
    private boolean mIsEnabled;
    private HashSet<OnDateChangedListener> mListeners;
    private Calendar mMaxDate;
    private Calendar mMinDate;
    private LinearLayout mMonthAndDayLayout;
    private LinearLayout mMonthDayYearLayout;
    private final DayPickerView.OnDaySelectedListener mOnDaySelectedListener;
    private String mSelectDay;
    private String mSelectYear;
    private Calendar mTempDate;
    private SimpleDateFormat mYearFormat;
    private String mYearPickerDescription;
    private YearPickerView mYearPickerView;

    public DatePickerCalendarDelegate(DatePicker delegator, Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(delegator, context);
        this.mYearFormat = new SimpleDateFormat("y", Locale.getDefault());
        this.mDayFormat = new SimpleDateFormat("d", Locale.getDefault());
        this.mIsEnabled = true;
        this.mCurrentView = -1;
        this.mFirstDayOfWeek = 0;
        this.mListeners = new HashSet<>();
        this.mOnDaySelectedListener = new DayPickerView.OnDaySelectedListener() {
            @Override
            public void onDaySelected(DayPickerView view, Calendar day) {
                DatePickerCalendarDelegate.this.mCurrentDate.setTimeInMillis(day.getTimeInMillis());
                DatePickerCalendarDelegate.this.onDateChanged(true, true);
            }
        };
        Locale locale = Locale.getDefault();
        this.mMinDate = getCalendarForLocale(this.mMinDate, locale);
        this.mMaxDate = getCalendarForLocale(this.mMaxDate, locale);
        this.mTempDate = getCalendarForLocale(this.mMaxDate, locale);
        this.mCurrentDate = getCalendarForLocale(this.mCurrentDate, locale);
        this.mMinDate.set(DEFAULT_START_YEAR, 1, 1);
        this.mMaxDate.set(DEFAULT_END_YEAR, 12, 31);
        Resources res = this.mDelegator.getResources();
        TypedArray a = this.mContext.obtainStyledAttributes(attrs, R.styleable.DatePicker, defStyleAttr, defStyleRes);
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int layoutResourceId = a.getResourceId(17, R.layout.date_picker_holo);
        View mainView = inflater.inflate(layoutResourceId, (ViewGroup) null);
        this.mDelegator.addView(mainView);
        this.mDayOfWeekView = (TextView) mainView.findViewById(R.id.date_picker_header);
        LinearLayout dateLayout = (LinearLayout) mainView.findViewById(R.id.day_picker_selector_layout);
        this.mMonthDayYearLayout = (LinearLayout) mainView.findViewById(R.id.date_picker_month_day_year_layout);
        this.mMonthAndDayLayout = (LinearLayout) mainView.findViewById(R.id.date_picker_month_and_day_layout);
        this.mMonthAndDayLayout.setOnClickListener(this);
        this.mHeaderMonthTextView = (TextView) mainView.findViewById(R.id.date_picker_month);
        this.mHeaderDayOfMonthTextView = (TextView) mainView.findViewById(R.id.date_picker_day);
        this.mHeaderYearTextView = (TextView) mainView.findViewById(R.id.date_picker_year);
        this.mHeaderYearTextView.setOnClickListener(this);
        int defaultHighlightColor = this.mHeaderYearTextView.getHighlightColor();
        int dayOfWeekTextAppearanceResId = a.getResourceId(9, -1);
        if (dayOfWeekTextAppearanceResId != -1) {
            this.mDayOfWeekView.setTextAppearance(context, dayOfWeekTextAppearanceResId);
        }
        this.mDayOfWeekView.setBackground(a.getDrawable(8));
        dateLayout.setBackground(a.getDrawable(0));
        int headerSelectedTextColor = a.getColor(20, defaultHighlightColor);
        int monthTextAppearanceResId = a.getResourceId(10, -1);
        if (monthTextAppearanceResId != -1) {
            this.mHeaderMonthTextView.setTextAppearance(context, monthTextAppearanceResId);
        }
        this.mHeaderMonthTextView.setTextColor(ColorStateList.addFirstIfMissing(this.mHeaderMonthTextView.getTextColors(), 16842913, headerSelectedTextColor));
        int dayOfMonthTextAppearanceResId = a.getResourceId(11, -1);
        if (dayOfMonthTextAppearanceResId != -1) {
            this.mHeaderDayOfMonthTextView.setTextAppearance(context, dayOfMonthTextAppearanceResId);
        }
        this.mHeaderDayOfMonthTextView.setTextColor(ColorStateList.addFirstIfMissing(this.mHeaderDayOfMonthTextView.getTextColors(), 16842913, headerSelectedTextColor));
        int yearTextAppearanceResId = a.getResourceId(12, -1);
        if (yearTextAppearanceResId != -1) {
            this.mHeaderYearTextView.setTextAppearance(context, yearTextAppearanceResId);
        }
        this.mHeaderYearTextView.setTextColor(ColorStateList.addFirstIfMissing(this.mHeaderYearTextView.getTextColors(), 16842913, headerSelectedTextColor));
        this.mDayPickerView = new DayPickerView(this.mContext);
        this.mDayPickerView.setFirstDayOfWeek(this.mFirstDayOfWeek);
        this.mDayPickerView.setMinDate(this.mMinDate.getTimeInMillis());
        this.mDayPickerView.setMaxDate(this.mMaxDate.getTimeInMillis());
        this.mDayPickerView.setDate(this.mCurrentDate.getTimeInMillis());
        this.mDayPickerView.setOnDaySelectedListener(this.mOnDaySelectedListener);
        this.mYearPickerView = new YearPickerView(this.mContext);
        this.mYearPickerView.init(this);
        this.mYearPickerView.setRange(this.mMinDate, this.mMaxDate);
        int yearSelectedCircleColor = a.getColor(14, defaultHighlightColor);
        this.mYearPickerView.setYearSelectedCircleColor(yearSelectedCircleColor);
        ColorStateList calendarTextColor = a.getColorStateList(15);
        int calendarSelectedTextColor = a.getColor(18, defaultHighlightColor);
        this.mDayPickerView.setCalendarTextColor(ColorStateList.addFirstIfMissing(calendarTextColor, 16842913, calendarSelectedTextColor));
        this.mDayPickerDescription = res.getString(R.string.day_picker_description);
        this.mSelectDay = res.getString(R.string.select_day);
        this.mYearPickerDescription = res.getString(R.string.year_picker_description);
        this.mSelectYear = res.getString(R.string.select_year);
        this.mAnimator = (AccessibleDateAnimator) mainView.findViewById(R.id.animator);
        this.mAnimator.addView(this.mDayPickerView);
        this.mAnimator.addView(this.mYearPickerView);
        this.mAnimator.setDateMillis(this.mCurrentDate.getTimeInMillis());
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(300L);
        this.mAnimator.setInAnimation(animation);
        Animation animation2 = new AlphaAnimation(1.0f, 0.0f);
        animation2.setDuration(300L);
        this.mAnimator.setOutAnimation(animation2);
        updateDisplay(false);
        setCurrentView(0);
    }

    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        }
        long currentTimeMillis = oldCalendar.getTimeInMillis();
        Calendar newCalendar = Calendar.getInstance(locale);
        newCalendar.setTimeInMillis(currentTimeMillis);
        return newCalendar;
    }

    private int[] getMonthDayYearIndexes(String pattern) {
        int[] result = new int[3];
        String filteredPattern = pattern.replaceAll("'.*?'", ProxyInfo.LOCAL_EXCL_LIST);
        int dayIndex = filteredPattern.indexOf(100);
        int monthMIndex = filteredPattern.indexOf("M");
        int monthIndex = monthMIndex != -1 ? monthMIndex : filteredPattern.indexOf("L");
        int yearIndex = filteredPattern.indexOf("y");
        if (yearIndex < monthIndex) {
            result[2] = 0;
            if (monthIndex < dayIndex) {
                result[0] = 1;
                result[1] = 2;
            } else {
                result[0] = 2;
                result[1] = 1;
            }
        } else {
            result[2] = 2;
            if (monthIndex < dayIndex) {
                result[0] = 0;
                result[1] = 1;
            } else {
                result[0] = 1;
                result[1] = 0;
            }
        }
        return result;
    }

    private void updateDisplay(boolean announce) {
        if (this.mDayOfWeekView != null) {
            this.mDayOfWeekView.setText(this.mCurrentDate.getDisplayName(7, 2, Locale.getDefault()));
        }
        String bestDateTimePattern = DateFormat.getBestDateTimePattern(this.mCurrentLocale, "yMMMd");
        int[] viewIndices = getMonthDayYearIndexes(bestDateTimePattern);
        this.mMonthDayYearLayout.removeAllViews();
        if (viewIndices[2] == 0) {
            this.mMonthDayYearLayout.addView(this.mHeaderYearTextView);
            this.mMonthDayYearLayout.addView(this.mMonthAndDayLayout);
        } else {
            this.mMonthDayYearLayout.addView(this.mMonthAndDayLayout);
            this.mMonthDayYearLayout.addView(this.mHeaderYearTextView);
        }
        this.mMonthAndDayLayout.removeAllViews();
        if (viewIndices[0] > viewIndices[1]) {
            this.mMonthAndDayLayout.addView(this.mHeaderDayOfMonthTextView);
            this.mMonthAndDayLayout.addView(this.mHeaderMonthTextView);
        } else {
            this.mMonthAndDayLayout.addView(this.mHeaderMonthTextView);
            this.mMonthAndDayLayout.addView(this.mHeaderDayOfMonthTextView);
        }
        this.mHeaderMonthTextView.setText(this.mCurrentDate.getDisplayName(2, 1, Locale.getDefault()).toUpperCase(Locale.getDefault()));
        this.mHeaderDayOfMonthTextView.setText(this.mDayFormat.format(this.mCurrentDate.getTime()));
        this.mHeaderYearTextView.setText(this.mYearFormat.format(this.mCurrentDate.getTime()));
        long millis = this.mCurrentDate.getTimeInMillis();
        this.mAnimator.setDateMillis(millis);
        String monthAndDayText = DateUtils.formatDateTime(this.mContext, millis, 24);
        this.mMonthAndDayLayout.setContentDescription(monthAndDayText);
        if (announce) {
            String fullDateText = DateUtils.formatDateTime(this.mContext, millis, 20);
            this.mAnimator.announceForAccessibility(fullDateText);
        }
    }

    private void setCurrentView(int viewIndex) {
        long millis = this.mCurrentDate.getTimeInMillis();
        switch (viewIndex) {
            case 0:
                this.mDayPickerView.setDate(getSelectedDay().getTimeInMillis());
                if (this.mCurrentView != viewIndex) {
                    this.mMonthAndDayLayout.setSelected(true);
                    this.mHeaderYearTextView.setSelected(false);
                    this.mAnimator.setDisplayedChild(0);
                    this.mCurrentView = viewIndex;
                }
                String dayString = DateUtils.formatDateTime(this.mContext, millis, 16);
                this.mAnimator.setContentDescription(this.mDayPickerDescription + ": " + dayString);
                this.mAnimator.announceForAccessibility(this.mSelectDay);
                break;
            case 1:
                this.mYearPickerView.onDateChanged();
                if (this.mCurrentView != viewIndex) {
                    this.mMonthAndDayLayout.setSelected(false);
                    this.mHeaderYearTextView.setSelected(true);
                    this.mAnimator.setDisplayedChild(1);
                    this.mCurrentView = viewIndex;
                }
                CharSequence yearString = this.mYearFormat.format(Long.valueOf(millis));
                this.mAnimator.setContentDescription(this.mYearPickerDescription + ": " + ((Object) yearString));
                this.mAnimator.announceForAccessibility(this.mSelectYear);
                break;
        }
    }

    @Override
    public void init(int year, int monthOfYear, int dayOfMonth, DatePicker.OnDateChangedListener callBack) {
        this.mCurrentDate.set(1, year);
        this.mCurrentDate.set(2, monthOfYear);
        this.mCurrentDate.set(5, dayOfMonth);
        this.mDateChangedListener = callBack;
        onDateChanged(false, false);
    }

    @Override
    public void updateDate(int year, int month, int dayOfMonth) {
        this.mCurrentDate.set(1, year);
        this.mCurrentDate.set(2, month);
        this.mCurrentDate.set(5, dayOfMonth);
        onDateChanged(false, true);
    }

    private void onDateChanged(boolean fromUser, boolean callbackToClient) {
        if (callbackToClient && this.mDateChangedListener != null) {
            int year = this.mCurrentDate.get(1);
            int monthOfYear = this.mCurrentDate.get(2);
            int dayOfMonth = this.mCurrentDate.get(5);
            this.mDateChangedListener.onDateChanged(this.mDelegator, year, monthOfYear, dayOfMonth);
        }
        for (OnDateChangedListener listener : this.mListeners) {
            listener.onDateChanged();
        }
        this.mDayPickerView.setDate(getSelectedDay().getTimeInMillis());
        updateDisplay(fromUser);
        if (fromUser) {
            tryVibrate();
        }
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
    public void setMinDate(long minDate) {
        this.mTempDate.setTimeInMillis(minDate);
        if (this.mTempDate.get(1) != this.mMinDate.get(1) || this.mTempDate.get(6) == this.mMinDate.get(6)) {
            if (this.mCurrentDate.before(this.mTempDate)) {
                this.mCurrentDate.setTimeInMillis(minDate);
                onDateChanged(false, true);
            }
            this.mMinDate.setTimeInMillis(minDate);
            this.mDayPickerView.setMinDate(minDate);
            this.mYearPickerView.setRange(this.mMinDate, this.mMaxDate);
        }
    }

    @Override
    public Calendar getMinDate() {
        return this.mMinDate;
    }

    @Override
    public void setMaxDate(long maxDate) {
        this.mTempDate.setTimeInMillis(maxDate);
        if (this.mTempDate.get(1) != this.mMaxDate.get(1) || this.mTempDate.get(6) == this.mMaxDate.get(6)) {
            if (this.mCurrentDate.after(this.mTempDate)) {
                this.mCurrentDate.setTimeInMillis(maxDate);
                onDateChanged(false, true);
            }
            this.mMaxDate.setTimeInMillis(maxDate);
            this.mDayPickerView.setMaxDate(maxDate);
            this.mYearPickerView.setRange(this.mMinDate, this.mMaxDate);
        }
    }

    @Override
    public Calendar getMaxDate() {
        return this.mMaxDate;
    }

    @Override
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        this.mFirstDayOfWeek = firstDayOfWeek;
        this.mDayPickerView.setFirstDayOfWeek(firstDayOfWeek);
    }

    @Override
    public int getFirstDayOfWeek() {
        return this.mFirstDayOfWeek != 0 ? this.mFirstDayOfWeek : this.mCurrentDate.getFirstDayOfWeek();
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.mMonthAndDayLayout.setEnabled(enabled);
        this.mHeaderYearTextView.setEnabled(enabled);
        this.mAnimator.setEnabled(enabled);
        this.mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    @Override
    public CalendarView getCalendarView() {
        throw new UnsupportedOperationException("CalendarView does not exists for the new DatePicker");
    }

    @Override
    public void setCalendarViewShown(boolean shown) {
    }

    @Override
    public boolean getCalendarViewShown() {
        return false;
    }

    @Override
    public void setSpinnersShown(boolean shown) {
    }

    @Override
    public boolean getSpinnersShown() {
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        this.mYearFormat = new SimpleDateFormat("y", newConfig.locale);
        this.mDayFormat = new SimpleDateFormat("d", newConfig.locale);
    }

    @Override
    public Parcelable onSaveInstanceState(Parcelable superState) {
        int year = this.mCurrentDate.get(1);
        int month = this.mCurrentDate.get(2);
        int day = this.mCurrentDate.get(5);
        int listPosition = -1;
        int listPositionOffset = -1;
        if (this.mCurrentView == 0) {
            listPosition = this.mDayPickerView.getMostVisiblePosition();
        } else if (this.mCurrentView == 1) {
            listPosition = this.mYearPickerView.getFirstVisiblePosition();
            listPositionOffset = this.mYearPickerView.getFirstPositionOffset();
        }
        return new SavedState(superState, year, month, day, this.mMinDate.getTimeInMillis(), this.mMaxDate.getTimeInMillis(), this.mCurrentView, listPosition, listPositionOffset);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        this.mCurrentDate.set(ss.getSelectedYear(), ss.getSelectedMonth(), ss.getSelectedDay());
        this.mCurrentView = ss.getCurrentView();
        this.mMinDate.setTimeInMillis(ss.getMinDate());
        this.mMaxDate.setTimeInMillis(ss.getMaxDate());
        updateDisplay(false);
        setCurrentView(this.mCurrentView);
        int listPosition = ss.getListPosition();
        if (listPosition != -1) {
            if (this.mCurrentView == 0) {
                this.mDayPickerView.postSetSelection(listPosition);
            } else if (this.mCurrentView == 1) {
                this.mYearPickerView.postSetSelectionFromTop(listPosition, ss.getListPositionOffset());
            }
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.getText().add(this.mCurrentDate.getTime().toString());
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        event.setClassName(DatePicker.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        info.setClassName(DatePicker.class.getName());
    }

    @Override
    public void onYearSelected(int year) {
        adjustDayInMonthIfNeeded(this.mCurrentDate.get(2), year);
        this.mCurrentDate.set(1, year);
        onDateChanged(true, true);
        setCurrentView(0);
    }

    private void adjustDayInMonthIfNeeded(int month, int year) {
        int day = this.mCurrentDate.get(5);
        int daysInMonth = getDaysInMonth(month, year);
        if (day > daysInMonth) {
            this.mCurrentDate.set(5, daysInMonth);
        }
    }

    public static int getDaysInMonth(int month, int year) {
        switch (month) {
            case 0:
            case 2:
            case 4:
            case 6:
            case 7:
            case 9:
            case 11:
                return 31;
            case 1:
                return year % 4 == 0 ? 29 : 28;
            case 3:
            case 5:
            case 8:
            case 10:
                return 30;
            default:
                throw new IllegalArgumentException("Invalid Month");
        }
    }

    @Override
    public void registerOnDateChangedListener(OnDateChangedListener listener) {
        this.mListeners.add(listener);
    }

    @Override
    public Calendar getSelectedDay() {
        return this.mCurrentDate;
    }

    @Override
    public void tryVibrate() {
        this.mDelegator.performHapticFeedback(5);
    }

    @Override
    public void onClick(View v) {
        tryVibrate();
        if (v.getId() == 16909041) {
            setCurrentView(1);
        } else if (v.getId() == 16909038) {
            setCurrentView(0);
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
        private final int mCurrentView;
        private final int mListPosition;
        private final int mListPositionOffset;
        private final long mMaxDate;
        private final long mMinDate;
        private final int mSelectedDay;
        private final int mSelectedMonth;
        private final int mSelectedYear;

        private SavedState(Parcelable superState, int year, int month, int day, long minDate, long maxDate, int currentView, int listPosition, int listPositionOffset) {
            super(superState);
            this.mSelectedYear = year;
            this.mSelectedMonth = month;
            this.mSelectedDay = day;
            this.mMinDate = minDate;
            this.mMaxDate = maxDate;
            this.mCurrentView = currentView;
            this.mListPosition = listPosition;
            this.mListPositionOffset = listPositionOffset;
        }

        private SavedState(Parcel in) {
            super(in);
            this.mSelectedYear = in.readInt();
            this.mSelectedMonth = in.readInt();
            this.mSelectedDay = in.readInt();
            this.mMinDate = in.readLong();
            this.mMaxDate = in.readLong();
            this.mCurrentView = in.readInt();
            this.mListPosition = in.readInt();
            this.mListPositionOffset = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.mSelectedYear);
            dest.writeInt(this.mSelectedMonth);
            dest.writeInt(this.mSelectedDay);
            dest.writeLong(this.mMinDate);
            dest.writeLong(this.mMaxDate);
            dest.writeInt(this.mCurrentView);
            dest.writeInt(this.mListPosition);
            dest.writeInt(this.mListPositionOffset);
        }

        public int getSelectedDay() {
            return this.mSelectedDay;
        }

        public int getSelectedMonth() {
            return this.mSelectedMonth;
        }

        public int getSelectedYear() {
            return this.mSelectedYear;
        }

        public long getMinDate() {
            return this.mMinDate;
        }

        public long getMaxDate() {
            return this.mMaxDate;
        }

        public int getCurrentView() {
            return this.mCurrentView;
        }

        public int getListPosition() {
            return this.mListPosition;
        }

        public int getListPositionOffset() {
            return this.mListPositionOffset;
        }
    }
}
