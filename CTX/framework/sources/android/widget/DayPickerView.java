package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.icu.util.Calendar;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.DayPickerPagerAdapter;
import com.android.internal.R;
import com.android.internal.widget.ViewPager;
import java.util.Locale;
import libcore.icu.LocaleData;

class DayPickerView extends ViewGroup {
    private static final int[] ATTRS_TEXT_COLOR = {16842904};
    private static final int DEFAULT_END_YEAR = 2100;
    private static final int DEFAULT_LAYOUT = 17367125;
    private static final int DEFAULT_START_YEAR = 1900;
    private final AccessibilityManager mAccessibilityManager;
    private final DayPickerPagerAdapter mAdapter;
    private final Calendar mMaxDate;
    private final Calendar mMinDate;
    private final ImageButton mNextButton;
    private final View.OnClickListener mOnClickListener;
    private OnDaySelectedListener mOnDaySelectedListener;
    private final ViewPager.OnPageChangeListener mOnPageChangedListener;
    private final ImageButton mPrevButton;
    private final Calendar mSelectedDay;
    private Calendar mTempCalendar;
    private final ViewPager mViewPager;

    public interface OnDaySelectedListener {
        void onDaySelected(DayPickerView dayPickerView, Calendar calendar);
    }

    public DayPickerView(Context context) {
        this(context, null);
    }

    public DayPickerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16843613);
    }

    public DayPickerView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public DayPickerView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mSelectedDay = Calendar.getInstance();
        this.mMinDate = Calendar.getInstance();
        this.mMaxDate = Calendar.getInstance();
        this.mOnPageChangedListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i3, float f, int i4) {
                float fAbs = Math.abs(0.5f - f) * 2.0f;
                DayPickerView.this.mPrevButton.setAlpha(fAbs);
                DayPickerView.this.mNextButton.setAlpha(fAbs);
            }

            @Override
            public void onPageScrollStateChanged(int i3) {
            }

            @Override
            public void onPageSelected(int i3) {
                DayPickerView.this.updateButtonVisibility(i3);
            }
        };
        this.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int i3;
                if (view != DayPickerView.this.mPrevButton) {
                    if (view != DayPickerView.this.mNextButton) {
                        return;
                    } else {
                        i3 = 1;
                    }
                } else {
                    i3 = -1;
                }
                DayPickerView.this.mViewPager.setCurrentItem(DayPickerView.this.mViewPager.getCurrentItem() + i3, !DayPickerView.this.mAccessibilityManager.isEnabled());
            }
        };
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.CalendarView, i, i2);
        int i3 = typedArrayObtainStyledAttributes.getInt(0, LocaleData.get(Locale.getDefault()).firstDayOfWeek.intValue());
        String string = typedArrayObtainStyledAttributes.getString(2);
        String string2 = typedArrayObtainStyledAttributes.getString(3);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(16, R.style.TextAppearance_Material_Widget_Calendar_Month);
        int resourceId2 = typedArrayObtainStyledAttributes.getResourceId(11, R.style.TextAppearance_Material_Widget_Calendar_DayOfWeek);
        int resourceId3 = typedArrayObtainStyledAttributes.getResourceId(12, R.style.TextAppearance_Material_Widget_Calendar_Day);
        ColorStateList colorStateList = typedArrayObtainStyledAttributes.getColorStateList(15);
        typedArrayObtainStyledAttributes.recycle();
        this.mAdapter = new DayPickerPagerAdapter(context, R.layout.date_picker_month_item_material, R.id.month_view);
        this.mAdapter.setMonthTextAppearance(resourceId);
        this.mAdapter.setDayOfWeekTextAppearance(resourceId2);
        this.mAdapter.setDayTextAppearance(resourceId3);
        this.mAdapter.setDaySelectorColor(colorStateList);
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(context).inflate(17367125, (ViewGroup) this, false);
        while (viewGroup.getChildCount() > 0) {
            View childAt = viewGroup.getChildAt(0);
            viewGroup.removeViewAt(0);
            addView(childAt);
        }
        this.mPrevButton = (ImageButton) findViewById(R.id.prev);
        this.mPrevButton.setOnClickListener(this.mOnClickListener);
        this.mNextButton = (ImageButton) findViewById(R.id.next);
        this.mNextButton.setOnClickListener(this.mOnClickListener);
        this.mViewPager = (ViewPager) findViewById(R.id.day_picker_view_pager);
        this.mViewPager.setAdapter(this.mAdapter);
        this.mViewPager.setOnPageChangeListener(this.mOnPageChangedListener);
        if (resourceId != 0) {
            TypedArray typedArrayObtainStyledAttributes2 = this.mContext.obtainStyledAttributes(null, ATTRS_TEXT_COLOR, 0, resourceId);
            ColorStateList colorStateList2 = typedArrayObtainStyledAttributes2.getColorStateList(0);
            if (colorStateList2 != null) {
                this.mPrevButton.setImageTintList(colorStateList2);
                this.mNextButton.setImageTintList(colorStateList2);
            }
            typedArrayObtainStyledAttributes2.recycle();
        }
        Calendar calendar = Calendar.getInstance();
        if (!CalendarView.parseDate(string, calendar)) {
            calendar.set(1900, 0, 1);
        }
        long timeInMillis = calendar.getTimeInMillis();
        if (!CalendarView.parseDate(string2, calendar)) {
            calendar.set(2100, 11, 31);
        }
        long timeInMillis2 = calendar.getTimeInMillis();
        if (timeInMillis2 < timeInMillis) {
            throw new IllegalArgumentException("maxDate must be >= minDate");
        }
        long jConstrain = MathUtils.constrain(System.currentTimeMillis(), timeInMillis, timeInMillis2);
        setFirstDayOfWeek(i3);
        setMinDate(timeInMillis);
        setMaxDate(timeInMillis2);
        setDate(jConstrain, false);
        this.mAdapter.setOnDaySelectedListener(new DayPickerPagerAdapter.OnDaySelectedListener() {
            @Override
            public void onDaySelected(DayPickerPagerAdapter dayPickerPagerAdapter, Calendar calendar2) {
                if (DayPickerView.this.mOnDaySelectedListener != null) {
                    DayPickerView.this.mOnDaySelectedListener.onDaySelected(DayPickerView.this, calendar2);
                }
            }
        });
    }

    private void updateButtonVisibility(int i) {
        boolean z = true;
        boolean z2 = i > 0;
        if (i >= this.mAdapter.getCount() - 1) {
            z = false;
        }
        this.mPrevButton.setVisibility(z2 ? 0 : 4);
        this.mNextButton.setVisibility(z ? 0 : 4);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        ViewPager viewPager = this.mViewPager;
        measureChild(viewPager, i, i2);
        setMeasuredDimension(viewPager.getMeasuredWidthAndState(), viewPager.getMeasuredHeightAndState());
        int measuredWidth = viewPager.getMeasuredWidth();
        int measuredHeight = viewPager.getMeasuredHeight();
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(measuredWidth, Integer.MIN_VALUE);
        int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(measuredHeight, Integer.MIN_VALUE);
        this.mPrevButton.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
        this.mNextButton.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        requestLayout();
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        ImageButton imageButton;
        ImageButton imageButton2;
        if (isLayoutRtl()) {
            imageButton = this.mNextButton;
            imageButton2 = this.mPrevButton;
        } else {
            imageButton = this.mPrevButton;
            imageButton2 = this.mNextButton;
        }
        int i5 = i3 - i;
        this.mViewPager.layout(0, 0, i5, i4 - i2);
        SimpleMonthView simpleMonthView = (SimpleMonthView) this.mViewPager.getChildAt(0);
        int monthHeight = simpleMonthView.getMonthHeight();
        int cellWidth = simpleMonthView.getCellWidth();
        int measuredWidth = imageButton.getMeasuredWidth();
        int measuredHeight = imageButton.getMeasuredHeight();
        int paddingTop = simpleMonthView.getPaddingTop() + ((monthHeight - measuredHeight) / 2);
        int paddingLeft = simpleMonthView.getPaddingLeft() + ((cellWidth - measuredWidth) / 2);
        imageButton.layout(paddingLeft, paddingTop, measuredWidth + paddingLeft, measuredHeight + paddingTop);
        int measuredWidth2 = imageButton2.getMeasuredWidth();
        int measuredHeight2 = imageButton2.getMeasuredHeight();
        int paddingTop2 = simpleMonthView.getPaddingTop() + ((monthHeight - measuredHeight2) / 2);
        int paddingRight = (i5 - simpleMonthView.getPaddingRight()) - ((cellWidth - measuredWidth2) / 2);
        imageButton2.layout(paddingRight - measuredWidth2, paddingTop2, paddingRight, measuredHeight2 + paddingTop2);
    }

    public void setDayOfWeekTextAppearance(int i) {
        this.mAdapter.setDayOfWeekTextAppearance(i);
    }

    public int getDayOfWeekTextAppearance() {
        return this.mAdapter.getDayOfWeekTextAppearance();
    }

    public void setDayTextAppearance(int i) {
        this.mAdapter.setDayTextAppearance(i);
    }

    public int getDayTextAppearance() {
        return this.mAdapter.getDayTextAppearance();
    }

    public void setDate(long j) {
        setDate(j, false);
    }

    public void setDate(long j, boolean z) {
        setDate(j, z, true);
    }

    private void setDate(long j, boolean z, boolean z2) {
        boolean z3 = true;
        if (j < this.mMinDate.getTimeInMillis()) {
            j = this.mMinDate.getTimeInMillis();
        } else if (j > this.mMaxDate.getTimeInMillis()) {
            j = this.mMaxDate.getTimeInMillis();
        } else {
            z3 = false;
        }
        getTempCalendarForTime(j);
        if (z2 || z3) {
            this.mSelectedDay.setTimeInMillis(j);
        }
        int positionFromDay = getPositionFromDay(j);
        if (positionFromDay != this.mViewPager.getCurrentItem()) {
            this.mViewPager.setCurrentItem(positionFromDay, z);
        }
        this.mAdapter.setSelectedDay(this.mTempCalendar);
    }

    public long getDate() {
        return this.mSelectedDay.getTimeInMillis();
    }

    public boolean getBoundsForDate(long j, Rect rect) {
        if (getPositionFromDay(j) != this.mViewPager.getCurrentItem()) {
            return false;
        }
        this.mTempCalendar.setTimeInMillis(j);
        return this.mAdapter.getBoundsForDate(this.mTempCalendar, rect);
    }

    public void setFirstDayOfWeek(int i) {
        this.mAdapter.setFirstDayOfWeek(i);
    }

    public int getFirstDayOfWeek() {
        return this.mAdapter.getFirstDayOfWeek();
    }

    public void setMinDate(long j) {
        this.mMinDate.setTimeInMillis(j);
        onRangeChanged();
    }

    public long getMinDate() {
        return this.mMinDate.getTimeInMillis();
    }

    public void setMaxDate(long j) {
        this.mMaxDate.setTimeInMillis(j);
        onRangeChanged();
    }

    public long getMaxDate() {
        return this.mMaxDate.getTimeInMillis();
    }

    public void onRangeChanged() {
        this.mAdapter.setRange(this.mMinDate, this.mMaxDate);
        setDate(this.mSelectedDay.getTimeInMillis(), false, false);
        updateButtonVisibility(this.mViewPager.getCurrentItem());
    }

    public void setOnDaySelectedListener(OnDaySelectedListener onDaySelectedListener) {
        this.mOnDaySelectedListener = onDaySelectedListener;
    }

    private int getDiffMonths(Calendar calendar, Calendar calendar2) {
        return (calendar2.get(2) - calendar.get(2)) + (12 * (calendar2.get(1) - calendar.get(1)));
    }

    private int getPositionFromDay(long j) {
        return MathUtils.constrain(getDiffMonths(this.mMinDate, getTempCalendarForTime(j)), 0, getDiffMonths(this.mMinDate, this.mMaxDate));
    }

    private Calendar getTempCalendarForTime(long j) {
        if (this.mTempCalendar == null) {
            this.mTempCalendar = Calendar.getInstance();
        }
        this.mTempCalendar.setTimeInMillis(j);
        return this.mTempCalendar;
    }

    public int getMostVisiblePosition() {
        return this.mViewPager.getCurrentItem();
    }

    public void setPosition(int i) {
        this.mViewPager.setCurrentItem(i, false);
    }
}
