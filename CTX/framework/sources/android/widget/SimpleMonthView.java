package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.icu.text.DisplayContext;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.IntArray;
import android.util.MathUtils;
import android.util.StateSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.internal.R;
import com.android.internal.widget.ExploreByTouchHelper;
import java.text.NumberFormat;
import java.util.Locale;
import libcore.icu.LocaleData;

class SimpleMonthView extends View {
    private static final int DAYS_IN_WEEK = 7;
    private static final int DEFAULT_SELECTED_DAY = -1;
    private static final int DEFAULT_WEEK_START = 1;
    private static final int MAX_WEEKS_IN_MONTH = 6;
    private static final String MONTH_YEAR_FORMAT = "MMMMy";
    private static final int SELECTED_HIGHLIGHT_ALPHA = 176;
    private int mActivatedDay;
    private final Calendar mCalendar;
    private int mCellWidth;
    private final NumberFormat mDayFormatter;
    private int mDayHeight;
    private final Paint mDayHighlightPaint;
    private final Paint mDayHighlightSelectorPaint;
    private int mDayOfWeekHeight;
    private final String[] mDayOfWeekLabels;
    private final TextPaint mDayOfWeekPaint;
    private int mDayOfWeekStart;
    private final TextPaint mDayPaint;
    private final Paint mDaySelectorPaint;
    private int mDaySelectorRadius;
    private ColorStateList mDayTextColor;
    private int mDaysInMonth;
    private final int mDesiredCellWidth;
    private final int mDesiredDayHeight;
    private final int mDesiredDayOfWeekHeight;
    private final int mDesiredDaySelectorRadius;
    private final int mDesiredMonthHeight;
    private int mEnabledDayEnd;
    private int mEnabledDayStart;
    private int mHighlightedDay;
    private boolean mIsTouchHighlighted;
    private final Locale mLocale;
    private int mMonth;
    private int mMonthHeight;
    private final TextPaint mMonthPaint;
    private String mMonthYearLabel;
    private OnDayClickListener mOnDayClickListener;
    private int mPaddedHeight;
    private int mPaddedWidth;
    private int mPreviouslyHighlightedDay;
    private int mToday;
    private final MonthViewTouchHelper mTouchHelper;
    private int mWeekStart;
    private int mYear;

    public interface OnDayClickListener {
        void onDayClick(SimpleMonthView simpleMonthView, Calendar calendar);
    }

    public SimpleMonthView(Context context) {
        this(context, null);
    }

    public SimpleMonthView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16843612);
    }

    public SimpleMonthView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SimpleMonthView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mMonthPaint = new TextPaint();
        this.mDayOfWeekPaint = new TextPaint();
        this.mDayPaint = new TextPaint();
        this.mDaySelectorPaint = new Paint();
        this.mDayHighlightPaint = new Paint();
        this.mDayHighlightSelectorPaint = new Paint();
        this.mDayOfWeekLabels = new String[7];
        this.mActivatedDay = -1;
        this.mToday = -1;
        this.mWeekStart = 1;
        this.mEnabledDayStart = 1;
        this.mEnabledDayEnd = 31;
        this.mHighlightedDay = -1;
        this.mPreviouslyHighlightedDay = -1;
        this.mIsTouchHighlighted = false;
        Resources resources = context.getResources();
        this.mDesiredMonthHeight = resources.getDimensionPixelSize(R.dimen.date_picker_month_height);
        this.mDesiredDayOfWeekHeight = resources.getDimensionPixelSize(R.dimen.date_picker_day_of_week_height);
        this.mDesiredDayHeight = resources.getDimensionPixelSize(R.dimen.date_picker_day_height);
        this.mDesiredCellWidth = resources.getDimensionPixelSize(R.dimen.date_picker_day_width);
        this.mDesiredDaySelectorRadius = resources.getDimensionPixelSize(R.dimen.date_picker_day_selector_radius);
        this.mTouchHelper = new MonthViewTouchHelper(this);
        setAccessibilityDelegate(this.mTouchHelper);
        setImportantForAccessibility(1);
        this.mLocale = resources.getConfiguration().locale;
        this.mCalendar = Calendar.getInstance(this.mLocale);
        this.mDayFormatter = NumberFormat.getIntegerInstance(this.mLocale);
        updateMonthYearLabel();
        updateDayOfWeekLabels();
        initPaints(resources);
    }

    private void updateMonthYearLabel() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateFormat.getBestDateTimePattern(this.mLocale, MONTH_YEAR_FORMAT), this.mLocale);
        simpleDateFormat.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE);
        this.mMonthYearLabel = simpleDateFormat.format(this.mCalendar.getTime());
    }

    private void updateDayOfWeekLabels() {
        String[] strArr = LocaleData.get(this.mLocale).tinyWeekdayNames;
        for (int i = 0; i < 7; i++) {
            this.mDayOfWeekLabels[i] = strArr[(((this.mWeekStart + i) - 1) % 7) + 1];
        }
    }

    private ColorStateList applyTextAppearance(Paint paint, int i) {
        TypedArray typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R.styleable.TextAppearance, 0, i);
        String string = typedArrayObtainStyledAttributes.getString(12);
        if (string != null) {
            paint.setTypeface(Typeface.create(string, 0));
        }
        paint.setTextSize(typedArrayObtainStyledAttributes.getDimensionPixelSize(0, (int) paint.getTextSize()));
        ColorStateList colorStateList = typedArrayObtainStyledAttributes.getColorStateList(3);
        if (colorStateList != null) {
            paint.setColor(colorStateList.getColorForState(ENABLED_STATE_SET, 0));
        }
        typedArrayObtainStyledAttributes.recycle();
        return colorStateList;
    }

    public int getMonthHeight() {
        return this.mMonthHeight;
    }

    public int getCellWidth() {
        return this.mCellWidth;
    }

    public void setMonthTextAppearance(int i) {
        applyTextAppearance(this.mMonthPaint, i);
        invalidate();
    }

    public void setDayOfWeekTextAppearance(int i) {
        applyTextAppearance(this.mDayOfWeekPaint, i);
        invalidate();
    }

    public void setDayTextAppearance(int i) {
        ColorStateList colorStateListApplyTextAppearance = applyTextAppearance(this.mDayPaint, i);
        if (colorStateListApplyTextAppearance != null) {
            this.mDayTextColor = colorStateListApplyTextAppearance;
        }
        invalidate();
    }

    private void initPaints(Resources resources) {
        String string = resources.getString(R.string.date_picker_month_typeface);
        String string2 = resources.getString(R.string.date_picker_day_of_week_typeface);
        String string3 = resources.getString(R.string.date_picker_day_typeface);
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.date_picker_month_text_size);
        int dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.date_picker_day_of_week_text_size);
        int dimensionPixelSize3 = resources.getDimensionPixelSize(R.dimen.date_picker_day_text_size);
        this.mMonthPaint.setAntiAlias(true);
        this.mMonthPaint.setTextSize(dimensionPixelSize);
        this.mMonthPaint.setTypeface(Typeface.create(string, 0));
        this.mMonthPaint.setTextAlign(Paint.Align.CENTER);
        this.mMonthPaint.setStyle(Paint.Style.FILL);
        this.mDayOfWeekPaint.setAntiAlias(true);
        this.mDayOfWeekPaint.setTextSize(dimensionPixelSize2);
        this.mDayOfWeekPaint.setTypeface(Typeface.create(string2, 0));
        this.mDayOfWeekPaint.setTextAlign(Paint.Align.CENTER);
        this.mDayOfWeekPaint.setStyle(Paint.Style.FILL);
        this.mDaySelectorPaint.setAntiAlias(true);
        this.mDaySelectorPaint.setStyle(Paint.Style.FILL);
        this.mDayHighlightPaint.setAntiAlias(true);
        this.mDayHighlightPaint.setStyle(Paint.Style.FILL);
        this.mDayHighlightSelectorPaint.setAntiAlias(true);
        this.mDayHighlightSelectorPaint.setStyle(Paint.Style.FILL);
        this.mDayPaint.setAntiAlias(true);
        this.mDayPaint.setTextSize(dimensionPixelSize3);
        this.mDayPaint.setTypeface(Typeface.create(string3, 0));
        this.mDayPaint.setTextAlign(Paint.Align.CENTER);
        this.mDayPaint.setStyle(Paint.Style.FILL);
    }

    void setMonthTextColor(ColorStateList colorStateList) {
        this.mMonthPaint.setColor(colorStateList.getColorForState(ENABLED_STATE_SET, 0));
        invalidate();
    }

    void setDayOfWeekTextColor(ColorStateList colorStateList) {
        this.mDayOfWeekPaint.setColor(colorStateList.getColorForState(ENABLED_STATE_SET, 0));
        invalidate();
    }

    void setDayTextColor(ColorStateList colorStateList) {
        this.mDayTextColor = colorStateList;
        invalidate();
    }

    void setDaySelectorColor(ColorStateList colorStateList) {
        int colorForState = colorStateList.getColorForState(StateSet.get(40), 0);
        this.mDaySelectorPaint.setColor(colorForState);
        this.mDayHighlightSelectorPaint.setColor(colorForState);
        this.mDayHighlightSelectorPaint.setAlpha(176);
        invalidate();
    }

    void setDayHighlightColor(ColorStateList colorStateList) {
        this.mDayHighlightPaint.setColor(colorStateList.getColorForState(StateSet.get(24), 0));
        invalidate();
    }

    public void setOnDayClickListener(OnDayClickListener onDayClickListener) {
        this.mOnDayClickListener = onDayClickListener;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent motionEvent) {
        return this.mTouchHelper.dispatchHoverEvent(motionEvent) || super.dispatchHoverEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int x = (int) (motionEvent.getX() + 0.5f);
        int y = (int) (motionEvent.getY() + 0.5f);
        int action = motionEvent.getAction();
        switch (action) {
            case 0:
            case 2:
                int dayAtLocation = getDayAtLocation(x, y);
                this.mIsTouchHighlighted = true;
                if (this.mHighlightedDay != dayAtLocation) {
                    this.mHighlightedDay = dayAtLocation;
                    this.mPreviouslyHighlightedDay = dayAtLocation;
                    invalidate();
                }
                return action != 0 || dayAtLocation >= 0;
            case 1:
                onDayClicked(getDayAtLocation(x, y));
                this.mHighlightedDay = -1;
                this.mIsTouchHighlighted = false;
                invalidate();
            case 3:
                this.mHighlightedDay = -1;
                this.mIsTouchHighlighted = false;
                invalidate();
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        int i2;
        int keyCode = keyEvent.getKeyCode();
        boolean zMoveOneDay = false;
        if (keyCode == 61) {
            if (keyEvent.hasNoModifiers()) {
                i2 = 2;
            } else {
                i2 = keyEvent.hasModifiers(1) ? 1 : 0;
            }
            if (i2 != 0) {
                ViewParent parent = getParent();
                View viewFocusSearch = this;
                do {
                    viewFocusSearch = viewFocusSearch.focusSearch(i2);
                    if (viewFocusSearch == null || viewFocusSearch == this) {
                        break;
                    }
                } while (viewFocusSearch.getParent() == parent);
                if (viewFocusSearch != null) {
                    viewFocusSearch.requestFocus();
                    return true;
                }
            }
        } else if (keyCode != 66) {
            switch (keyCode) {
                case 19:
                    if (keyEvent.hasNoModifiers()) {
                        ensureFocusedDay();
                        if (this.mHighlightedDay > 7) {
                            this.mHighlightedDay -= 7;
                            zMoveOneDay = true;
                        }
                    }
                    break;
                case 20:
                    if (keyEvent.hasNoModifiers()) {
                        ensureFocusedDay();
                        if (this.mHighlightedDay <= this.mDaysInMonth - 7) {
                            this.mHighlightedDay += 7;
                            zMoveOneDay = true;
                        }
                    }
                    break;
                case 21:
                    if (keyEvent.hasNoModifiers()) {
                        zMoveOneDay = moveOneDay(isLayoutRtl());
                    }
                    break;
                case 22:
                    if (keyEvent.hasNoModifiers()) {
                        zMoveOneDay = moveOneDay(!isLayoutRtl());
                    }
                    break;
                case 23:
                    if (this.mHighlightedDay != -1) {
                        onDayClicked(this.mHighlightedDay);
                    }
                    break;
            }
            return true;
        }
        if (zMoveOneDay) {
            invalidate();
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    private boolean moveOneDay(boolean z) {
        ensureFocusedDay();
        if (z) {
            if (!isLastDayOfWeek(this.mHighlightedDay) && this.mHighlightedDay < this.mDaysInMonth) {
                this.mHighlightedDay++;
                return true;
            }
        } else if (!isFirstDayOfWeek(this.mHighlightedDay) && this.mHighlightedDay > 1) {
            this.mHighlightedDay--;
            return true;
        }
        return false;
    }

    @Override
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        if (z) {
            int iFindDayOffset = findDayOffset();
            if (i == 17) {
                this.mHighlightedDay = Math.min(this.mDaysInMonth, ((findClosestRow(rect) + 1) * 7) - iFindDayOffset);
            } else if (i == 33) {
                int iFindClosestColumn = (findClosestColumn(rect) - iFindDayOffset) + (7 * ((this.mDaysInMonth + iFindDayOffset) / 7)) + 1;
                if (iFindClosestColumn > this.mDaysInMonth) {
                    iFindClosestColumn -= 7;
                }
                this.mHighlightedDay = iFindClosestColumn;
            } else if (i == 66) {
                int iFindClosestRow = findClosestRow(rect);
                this.mHighlightedDay = iFindClosestRow != 0 ? 1 + ((iFindClosestRow * 7) - iFindDayOffset) : 1;
            } else if (i == 130) {
                int iFindClosestColumn2 = (findClosestColumn(rect) - iFindDayOffset) + 1;
                if (iFindClosestColumn2 < 1) {
                    iFindClosestColumn2 += 7;
                }
                this.mHighlightedDay = iFindClosestColumn2;
            }
            ensureFocusedDay();
            invalidate();
        }
        super.onFocusChanged(z, i, rect);
    }

    private int findClosestRow(Rect rect) {
        if (rect == null) {
            return 3;
        }
        if (this.mDayHeight == 0) {
            return 0;
        }
        int iCenterY = rect.centerY();
        TextPaint textPaint = this.mDayPaint;
        int i = this.mMonthHeight + this.mDayOfWeekHeight;
        int iRound = Math.round(((int) (iCenterY - ((i + (r3 / 2)) - ((textPaint.ascent() + textPaint.descent()) / 2.0f)))) / this.mDayHeight);
        int iFindDayOffset = findDayOffset() + this.mDaysInMonth;
        return MathUtils.constrain(iRound, 0, (iFindDayOffset / 7) - (iFindDayOffset % 7 == 0 ? 1 : 0));
    }

    private int findClosestColumn(Rect rect) {
        if (rect == null) {
            return 3;
        }
        if (this.mCellWidth == 0) {
            return 0;
        }
        return isLayoutRtl() ? (7 - r3) - 1 : MathUtils.constrain((rect.centerX() - this.mPaddingLeft) / this.mCellWidth, 0, 6);
    }

    @Override
    public void getFocusedRect(Rect rect) {
        if (this.mHighlightedDay > 0) {
            getBoundsForDay(this.mHighlightedDay, rect);
        } else {
            super.getFocusedRect(rect);
        }
    }

    @Override
    protected void onFocusLost() {
        if (!this.mIsTouchHighlighted) {
            this.mPreviouslyHighlightedDay = this.mHighlightedDay;
            this.mHighlightedDay = -1;
            invalidate();
        }
        super.onFocusLost();
    }

    private void ensureFocusedDay() {
        if (this.mHighlightedDay != -1) {
            return;
        }
        if (this.mPreviouslyHighlightedDay != -1) {
            this.mHighlightedDay = this.mPreviouslyHighlightedDay;
        } else if (this.mActivatedDay != -1) {
            this.mHighlightedDay = this.mActivatedDay;
        } else {
            this.mHighlightedDay = 1;
        }
    }

    private boolean isFirstDayOfWeek(int i) {
        return ((findDayOffset() + i) - 1) % 7 == 0;
    }

    private boolean isLastDayOfWeek(int i) {
        return (findDayOffset() + i) % 7 == 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.translate(getPaddingLeft(), getPaddingTop());
        drawMonth(canvas);
        drawDaysOfWeek(canvas);
        drawDays(canvas);
        canvas.translate(-r0, -r1);
    }

    private void drawMonth(Canvas canvas) {
        canvas.drawText(this.mMonthYearLabel, this.mPaddedWidth / 2.0f, (this.mMonthHeight - (this.mMonthPaint.ascent() + this.mMonthPaint.descent())) / 2.0f, this.mMonthPaint);
    }

    public String getMonthYearLabel() {
        return this.mMonthYearLabel;
    }

    private void drawDaysOfWeek(Canvas canvas) {
        TextPaint textPaint = this.mDayOfWeekPaint;
        int i = this.mMonthHeight;
        int i2 = this.mDayOfWeekHeight;
        int i3 = this.mCellWidth;
        float fAscent = (textPaint.ascent() + textPaint.descent()) / 2.0f;
        int i4 = i + (i2 / 2);
        for (int i5 = 0; i5 < 7; i5++) {
            int i6 = (i3 * i5) + (i3 / 2);
            if (isLayoutRtl()) {
                i6 = this.mPaddedWidth - i6;
            }
            canvas.drawText(this.mDayOfWeekLabels[i5], i6, i4 - fAscent, textPaint);
        }
    }

    private void drawDays(Canvas canvas) {
        int i;
        int i2;
        int colorForState;
        TextPaint textPaint = this.mDayPaint;
        int i3 = this.mMonthHeight + this.mDayOfWeekHeight;
        int i4 = this.mDayHeight;
        int i5 = this.mCellWidth;
        float fAscent = (textPaint.ascent() + textPaint.descent()) / 2.0f;
        int iFindDayOffset = findDayOffset();
        int i6 = i3 + (i4 / 2);
        int i7 = 1;
        while (i7 <= this.mDaysInMonth) {
            int i8 = (i5 * iFindDayOffset) + (i5 / 2);
            if (isLayoutRtl()) {
                i8 = this.mPaddedWidth - i8;
            }
            boolean zIsDayEnabled = isDayEnabled(i7);
            if (zIsDayEnabled) {
                i = 8;
            } else {
                i = 0;
            }
            boolean z = this.mActivatedDay == i7;
            boolean z2 = this.mHighlightedDay == i7;
            if (z) {
                i |= 32;
                canvas.drawCircle(i8, i6, this.mDaySelectorRadius, z2 ? this.mDayHighlightSelectorPaint : this.mDaySelectorPaint);
            } else if (z2) {
                i |= 16;
                if (zIsDayEnabled) {
                    canvas.drawCircle(i8, i6, this.mDaySelectorRadius, this.mDayHighlightPaint);
                }
            }
            if ((this.mToday == i7) && !z) {
                colorForState = this.mDaySelectorPaint.getColor();
                i2 = 0;
            } else {
                i2 = 0;
                colorForState = this.mDayTextColor.getColorForState(StateSet.get(i), 0);
            }
            textPaint.setColor(colorForState);
            canvas.drawText(this.mDayFormatter.format(i7), i8, i6 - fAscent, textPaint);
            iFindDayOffset++;
            if (iFindDayOffset == 7) {
                i6 += i4;
                iFindDayOffset = i2;
            }
            i7++;
        }
    }

    private boolean isDayEnabled(int i) {
        return i >= this.mEnabledDayStart && i <= this.mEnabledDayEnd;
    }

    private boolean isValidDayOfMonth(int i) {
        return i >= 1 && i <= this.mDaysInMonth;
    }

    private static boolean isValidDayOfWeek(int i) {
        return i >= 1 && i <= 7;
    }

    private static boolean isValidMonth(int i) {
        return i >= 0 && i <= 11;
    }

    public void setSelectedDay(int i) {
        this.mActivatedDay = i;
        this.mTouchHelper.invalidateRoot();
        invalidate();
    }

    public void setFirstDayOfWeek(int i) {
        if (isValidDayOfWeek(i)) {
            this.mWeekStart = i;
        } else {
            this.mWeekStart = this.mCalendar.getFirstDayOfWeek();
        }
        updateDayOfWeekLabels();
        this.mTouchHelper.invalidateRoot();
        invalidate();
    }

    void setMonthParams(int i, int i2, int i3, int i4, int i5, int i6) {
        this.mActivatedDay = i;
        if (isValidMonth(i2)) {
            this.mMonth = i2;
        }
        this.mYear = i3;
        this.mCalendar.set(2, this.mMonth);
        this.mCalendar.set(1, this.mYear);
        this.mCalendar.set(5, 1);
        this.mDayOfWeekStart = this.mCalendar.get(7);
        if (isValidDayOfWeek(i4)) {
            this.mWeekStart = i4;
        } else {
            this.mWeekStart = this.mCalendar.getFirstDayOfWeek();
        }
        Calendar calendar = Calendar.getInstance();
        this.mToday = -1;
        this.mDaysInMonth = getDaysInMonth(this.mMonth, this.mYear);
        int i7 = 0;
        while (i7 < this.mDaysInMonth) {
            i7++;
            if (sameDay(i7, calendar)) {
                this.mToday = i7;
            }
        }
        this.mEnabledDayStart = MathUtils.constrain(i5, 1, this.mDaysInMonth);
        this.mEnabledDayEnd = MathUtils.constrain(i6, this.mEnabledDayStart, this.mDaysInMonth);
        updateMonthYearLabel();
        updateDayOfWeekLabels();
        this.mTouchHelper.invalidateRoot();
        invalidate();
    }

    private static int getDaysInMonth(int i, int i2) {
        switch (i) {
            case 0:
            case 2:
            case 4:
            case 6:
            case 7:
            case 9:
            case 11:
                return 31;
            case 1:
                return i2 % 4 == 0 ? 29 : 28;
            case 3:
            case 5:
            case 8:
            case 10:
                return 30;
            default:
                throw new IllegalArgumentException("Invalid Month");
        }
    }

    private boolean sameDay(int i, Calendar calendar) {
        return this.mYear == calendar.get(1) && this.mMonth == calendar.get(2) && i == calendar.get(5);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(resolveSize((this.mDesiredCellWidth * 7) + getPaddingStart() + getPaddingEnd(), i), resolveSize((this.mDesiredDayHeight * 6) + this.mDesiredDayOfWeekHeight + this.mDesiredMonthHeight + getPaddingTop() + getPaddingBottom(), i2));
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        requestLayout();
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (!z) {
            return;
        }
        int i5 = i3 - i;
        int i6 = i4 - i2;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int i7 = (i5 - paddingRight) - paddingLeft;
        int i8 = (i6 - paddingBottom) - paddingTop;
        if (i7 == this.mPaddedWidth || i8 == this.mPaddedHeight) {
            return;
        }
        this.mPaddedWidth = i7;
        this.mPaddedHeight = i8;
        float measuredHeight = i8 / ((getMeasuredHeight() - paddingTop) - paddingBottom);
        int i9 = this.mPaddedWidth / 7;
        this.mMonthHeight = (int) (this.mDesiredMonthHeight * measuredHeight);
        this.mDayOfWeekHeight = (int) (this.mDesiredDayOfWeekHeight * measuredHeight);
        this.mDayHeight = (int) (this.mDesiredDayHeight * measuredHeight);
        this.mCellWidth = i9;
        this.mDaySelectorRadius = Math.min(this.mDesiredDaySelectorRadius, Math.min((i9 / 2) + Math.min(paddingLeft, paddingRight), (this.mDayHeight / 2) + paddingBottom));
        this.mTouchHelper.invalidateRoot();
    }

    private int findDayOffset() {
        int i = this.mDayOfWeekStart - this.mWeekStart;
        if (this.mDayOfWeekStart < this.mWeekStart) {
            return i + 7;
        }
        return i;
    }

    private int getDayAtLocation(int i, int i2) {
        int i3;
        int paddingTop;
        int paddingLeft = i - getPaddingLeft();
        if (paddingLeft < 0 || paddingLeft >= this.mPaddedWidth || (paddingTop = i2 - getPaddingTop()) < (i3 = this.mMonthHeight + this.mDayOfWeekHeight) || paddingTop >= this.mPaddedHeight) {
            return -1;
        }
        if (isLayoutRtl()) {
            paddingLeft = this.mPaddedWidth - paddingLeft;
        }
        int iFindDayOffset = ((((paddingLeft * 7) / this.mPaddedWidth) + (((paddingTop - i3) / this.mDayHeight) * 7)) + 1) - findDayOffset();
        if (!isValidDayOfMonth(iFindDayOffset)) {
            return -1;
        }
        return iFindDayOffset;
    }

    public boolean getBoundsForDay(int i, Rect rect) {
        int paddingLeft;
        if (!isValidDayOfMonth(i)) {
            return false;
        }
        int iFindDayOffset = (i - 1) + findDayOffset();
        int i2 = iFindDayOffset % 7;
        int i3 = this.mCellWidth;
        if (isLayoutRtl()) {
            paddingLeft = (getWidth() - getPaddingRight()) - ((i2 + 1) * i3);
        } else {
            paddingLeft = getPaddingLeft() + (i2 * i3);
        }
        int i4 = this.mDayHeight;
        int paddingTop = getPaddingTop() + this.mMonthHeight + this.mDayOfWeekHeight + ((iFindDayOffset / 7) * i4);
        rect.set(paddingLeft, paddingTop, i3 + paddingLeft, i4 + paddingTop);
        return true;
    }

    private boolean onDayClicked(int i) {
        if (!isValidDayOfMonth(i) || !isDayEnabled(i)) {
            return false;
        }
        if (this.mOnDayClickListener != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(this.mYear, this.mMonth, i);
            this.mOnDayClickListener.onDayClick(this, calendar);
        }
        this.mTouchHelper.sendEventForVirtualView(i, 1);
        return true;
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent motionEvent, int i) {
        if (!isEnabled()) {
            return null;
        }
        if (getDayAtLocation((int) (motionEvent.getX() + 0.5f), (int) (motionEvent.getY() + 0.5f)) >= 0) {
            return PointerIcon.getSystemIcon(getContext(), 1002);
        }
        return super.onResolvePointerIcon(motionEvent, i);
    }

    private class MonthViewTouchHelper extends ExploreByTouchHelper {
        private static final String DATE_FORMAT = "dd MMMM yyyy";
        private final Calendar mTempCalendar;
        private final Rect mTempRect;

        public MonthViewTouchHelper(View view) {
            super(view);
            this.mTempRect = new Rect();
            this.mTempCalendar = Calendar.getInstance();
        }

        @Override
        protected int getVirtualViewAt(float f, float f2) {
            int dayAtLocation = SimpleMonthView.this.getDayAtLocation((int) (f + 0.5f), (int) (f2 + 0.5f));
            if (dayAtLocation != -1) {
                return dayAtLocation;
            }
            return Integer.MIN_VALUE;
        }

        @Override
        protected void getVisibleVirtualViews(IntArray intArray) {
            for (int i = 1; i <= SimpleMonthView.this.mDaysInMonth; i++) {
                intArray.add(i);
            }
        }

        @Override
        protected void onPopulateEventForVirtualView(int i, AccessibilityEvent accessibilityEvent) {
            accessibilityEvent.setContentDescription(getDayDescription(i));
        }

        @Override
        protected void onPopulateNodeForVirtualView(int i, AccessibilityNodeInfo accessibilityNodeInfo) {
            if (!SimpleMonthView.this.getBoundsForDay(i, this.mTempRect)) {
                this.mTempRect.setEmpty();
                accessibilityNodeInfo.setContentDescription("");
                accessibilityNodeInfo.setBoundsInParent(this.mTempRect);
                accessibilityNodeInfo.setVisibleToUser(false);
                return;
            }
            accessibilityNodeInfo.setText(getDayText(i));
            accessibilityNodeInfo.setContentDescription(getDayDescription(i));
            accessibilityNodeInfo.setBoundsInParent(this.mTempRect);
            boolean zIsDayEnabled = SimpleMonthView.this.isDayEnabled(i);
            if (zIsDayEnabled) {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
            }
            accessibilityNodeInfo.setEnabled(zIsDayEnabled);
            if (i == SimpleMonthView.this.mActivatedDay) {
                accessibilityNodeInfo.setChecked(true);
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(int i, int i2, Bundle bundle) {
            if (i2 == 16) {
                return SimpleMonthView.this.onDayClicked(i);
            }
            return false;
        }

        private CharSequence getDayDescription(int i) {
            if (SimpleMonthView.this.isValidDayOfMonth(i)) {
                this.mTempCalendar.set(SimpleMonthView.this.mYear, SimpleMonthView.this.mMonth, i);
                return DateFormat.format(DATE_FORMAT, this.mTempCalendar.getTimeInMillis());
            }
            return "";
        }

        private CharSequence getDayText(int i) {
            if (SimpleMonthView.this.isValidDayOfMonth(i)) {
                return SimpleMonthView.this.mDayFormatter.format(i);
            }
            return null;
        }
    }
}
