package com.android.calendar.month;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.calendar.Event;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.mediatek.calendar.ext.OpCalendarCustomizationFactoryBase;
import com.mediatek.calendar.extension.ExtensionFactory;
import com.mediatek.calendar.extension.ICalendarThemeExt;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MonthWeekEventsView extends SimpleWeekView {
    private static boolean mShowDetailsInMonth;
    private boolean mAnimateSelectedDay;
    private int mAnimateSelectedDayAlpha;
    private int mAnimateTodayAlpha;
    private final SelectedDayAnimatorListener mAnimatorListener;
    private int mClickedDayColor;
    private int mClickedDayIndex;
    private Context mContext;
    protected Paint mDNAAllDayPaint;
    protected Paint mDNATimePaint;
    protected int mDaySeparatorInnerColor;
    private int[] mDayXs;
    protected TextPaint mDeclinedEventPaint;
    HashMap<Integer, Utils.DNAStrand> mDna;
    protected int mEventAscentHeight;
    protected int mEventChipOutlineColor;
    protected TextPaint mEventDeclinedExtrasPaint;
    protected TextPaint mEventExtrasPaint;
    protected int mEventHeight;
    protected FloatRef mEventOutlines;
    protected TextPaint mEventPaint;
    protected Paint mEventSquarePaint;
    protected List<ArrayList<Event>> mEvents;
    protected int mExtrasAscentHeight;
    protected int mExtrasDescent;
    protected int mExtrasHeight;
    protected TextPaint mFramedEventPaint;
    protected boolean mHasToday;
    protected int mMonthBGColor;
    protected int mMonthBGOtherColor;
    protected int mMonthBGTodayColor;
    protected int mMonthBusyBitsBusyTimeColor;
    protected int mMonthBusyBitsConflictTimeColor;
    protected int mMonthDeclinedEventColor;
    protected int mMonthDeclinedExtrasColor;
    protected int mMonthEventColor;
    protected int mMonthEventExtraColor;
    protected int mMonthEventExtraOtherColor;
    protected int mMonthEventOtherColor;
    protected int mMonthNameColor;
    protected int mMonthNameOtherColor;
    protected int mMonthNumAscentHeight;
    protected int mMonthNumColor;
    protected int mMonthNumHeight;
    protected int mMonthNumOtherColor;
    protected int mMonthNumTodayColor;
    protected int mMonthWeekNumColor;
    protected int mOrientation;
    protected int mSelectedDayAnimateColor;
    private ObjectAnimator mSelectedDayAnimator;
    protected int mSelectedDayIndex;
    protected Time mSelectedDayTime;
    protected TextPaint mSolidBackgroundEventPaint;
    protected Time mToday;
    protected int mTodayAnimateColor;
    private ObjectAnimator mTodayAnimator;
    protected Drawable mTodayDrawable;
    protected int mTodayIndex;
    protected ArrayList<Event> mUnsortedEvents;
    protected int mWeekNumAscentHeight;
    protected Paint mWeekNumPaint;
    private static int TEXT_SIZE_MONTH_NUMBER = 32;
    private static int TEXT_SIZE_EVENT = 12;
    private static int TEXT_SIZE_EVENT_TITLE = 14;
    private static int TEXT_SIZE_MORE_EVENTS = 12;
    private static int TEXT_SIZE_MONTH_NAME = 14;
    private static int TEXT_SIZE_WEEK_NUM = 12;
    private static int DNA_MARGIN = 4;
    private static int DNA_ALL_DAY_HEIGHT = 4;
    private static int DNA_MIN_SEGMENT_HEIGHT = 4;
    private static int DNA_WIDTH = 8;
    private static int DNA_ALL_DAY_WIDTH = 32;
    private static int DNA_SIDE_PADDING = 6;
    private static int CONFLICT_COLOR = -16777216;
    private static int EVENT_TEXT_COLOR = -1;
    private static int DEFAULT_EDGE_SPACING = 0;
    private static int SIDE_PADDING_MONTH_NUMBER = 4;
    private static int TOP_PADDING_MONTH_NUMBER = 4;
    private static int TOP_PADDING_WEEK_NUMBER = 4;
    private static int SIDE_PADDING_WEEK_NUMBER = 20;
    private static int DAY_SEPARATOR_OUTER_WIDTH = 0;
    private static int DAY_SEPARATOR_INNER_WIDTH = 1;
    private static int DAY_SEPARATOR_VERTICAL_LENGTH = 53;
    private static int DAY_SEPARATOR_VERTICAL_LENGHT_PORTRAIT = 64;
    private static int MIN_WEEK_WIDTH = 50;
    private static int EVENT_X_OFFSET_LANDSCAPE = 38;
    private static int EVENT_Y_OFFSET_LANDSCAPE = 8;
    private static int EVENT_Y_OFFSET_PORTRAIT = 7;
    private static int EVENT_SQUARE_WIDTH = 10;
    private static int EVENT_SQUARE_BORDER = 2;
    private static int EVENT_LINE_PADDING = 2;
    private static int EVENT_RIGHT_PADDING = 4;
    private static int EVENT_BOTTOM_PADDING = 3;
    private static int TODAY_HIGHLIGHT_WIDTH = 2;
    private static int SPACING_WEEK_NUMBER = 24;
    private static boolean mInitialized = false;
    protected static StringBuilder mStringBuilder = new StringBuilder(50);
    protected static Formatter mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

    class SelectedDayAnimatorListener extends AnimatorListenerAdapter {
        private volatile Animator mAnimator = null;
        private volatile boolean mFadingIn = false;

        SelectedDayAnimatorListener() {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            synchronized (this) {
                if (this.mAnimator != animator) {
                    animator.removeAllListeners();
                    animator.cancel();
                    return;
                }
                if (this.mFadingIn) {
                    if (MonthWeekEventsView.this.mSelectedDayAnimator != null) {
                        MonthWeekEventsView.this.mSelectedDayAnimator.removeAllListeners();
                        MonthWeekEventsView.this.mSelectedDayAnimator.cancel();
                    }
                    MonthWeekEventsView.this.mSelectedDayAnimator = ObjectAnimator.ofInt(MonthWeekEventsView.this, "animateSelectedDayAlpha", 255, 0);
                    this.mAnimator = MonthWeekEventsView.this.mSelectedDayAnimator;
                    this.mFadingIn = false;
                    MonthWeekEventsView.this.mSelectedDayAnimator.addListener(this);
                    MonthWeekEventsView.this.mSelectedDayAnimator.setDuration(600L);
                    MonthWeekEventsView.this.mSelectedDayAnimator.start();
                } else {
                    MonthWeekEventsView.this.mAnimateSelectedDay = false;
                    MonthWeekEventsView.this.mAnimateSelectedDayAlpha = 0;
                    this.mAnimator.removeAllListeners();
                    this.mAnimator = null;
                    MonthWeekEventsView.this.mSelectedDayAnimator = null;
                    MonthWeekEventsView.this.invalidate();
                }
            }
        }

        public void setAnimator(Animator animator) {
            this.mAnimator = animator;
        }

        public void setFadingIn(boolean z) {
            this.mFadingIn = z;
        }
    }

    private class FloatRef {
        float[] array;

        public FloatRef(int i) {
            this.array = new float[i];
        }
    }

    public MonthWeekEventsView(Context context) {
        super(context);
        this.mToday = new Time();
        this.mSelectedDayTime = new Time();
        this.mSelectedDayIndex = -1;
        this.mHasToday = false;
        this.mTodayIndex = -1;
        this.mOrientation = 2;
        this.mEvents = null;
        this.mUnsortedEvents = null;
        this.mDna = null;
        this.mEventOutlines = new FloatRef(1120);
        this.mClickedDayIndex = -1;
        this.mEventChipOutlineColor = -1;
        this.mAnimateTodayAlpha = 0;
        this.mTodayAnimator = null;
        this.mAnimateSelectedDayAlpha = 0;
        this.mSelectedDayAnimator = null;
        this.mAnimatorListener = new SelectedDayAnimatorListener();
        this.mContext = context;
    }

    public void setEvents(List<ArrayList<Event>> list, ArrayList<Event> arrayList) {
        setEvents(list);
        createDna(arrayList);
    }

    public void createDna(ArrayList<Event> arrayList) {
        if (arrayList == null || this.mWidth <= MIN_WEEK_WIDTH || getContext() == null) {
            this.mUnsortedEvents = arrayList;
            this.mDna = null;
            return;
        }
        this.mUnsortedEvents = null;
        if (!mShowDetailsInMonth) {
            int size = this.mEvents.size();
            int i = this.mWidth - (this.mPadding * 2);
            if (this.mShowWeekNum) {
                i -= SPACING_WEEK_NUMBER;
            }
            DNA_ALL_DAY_WIDTH = (i / size) - (DNA_SIDE_PADDING * 2);
            this.mDNAAllDayPaint.setStrokeWidth(DNA_ALL_DAY_WIDTH);
            this.mDayXs = new int[size];
            for (int i2 = 0; i2 < size; i2++) {
                this.mDayXs[i2] = computeDayLeftPosition(i2) + (DNA_WIDTH / 2) + DNA_SIDE_PADDING;
            }
            this.mDna = Utils.createDNAStrands(this.mFirstJulianDay, arrayList, DAY_SEPARATOR_INNER_WIDTH + DNA_MARGIN + DNA_ALL_DAY_HEIGHT + 1, this.mHeight - DNA_MARGIN, DNA_MIN_SEGMENT_HEIGHT, this.mDayXs, getContext());
        }
    }

    public void setEvents(List<ArrayList<Event>> list) {
        this.mEvents = list;
        if (list != null && list.size() != this.mNumDays) {
            if (Log.isLoggable("MonthView", 6)) {
                Log.wtf("MonthView", "Events size must be same as days displayed: size=" + list.size() + " days=" + this.mNumDays);
            }
            this.mEvents = null;
        }
    }

    protected void loadColors(Context context) {
        Resources resources = context.getResources();
        this.mMonthWeekNumColor = resources.getColor(R.color.month_week_num_color);
        this.mMonthNumColor = resources.getColor(R.color.month_day_number);
        this.mMonthNumOtherColor = resources.getColor(R.color.month_day_number_other);
        this.mMonthNumTodayColor = resources.getColor(R.color.month_today_number);
        this.mMonthNameColor = this.mMonthNumColor;
        this.mMonthNameOtherColor = this.mMonthNumOtherColor;
        this.mMonthEventColor = resources.getColor(R.color.month_event_color);
        this.mMonthDeclinedEventColor = resources.getColor(R.color.agenda_item_declined_color);
        this.mMonthDeclinedExtrasColor = resources.getColor(R.color.agenda_item_where_declined_text_color);
        this.mMonthEventExtraColor = resources.getColor(R.color.month_event_extra_color);
        this.mMonthEventOtherColor = resources.getColor(R.color.month_event_other_color);
        this.mMonthEventExtraOtherColor = resources.getColor(R.color.month_event_extra_other_color);
        this.mMonthBGTodayColor = resources.getColor(R.color.month_today_bgcolor);
        this.mMonthBGOtherColor = resources.getColor(R.color.month_other_bgcolor);
        this.mMonthBGColor = resources.getColor(R.color.month_bgcolor);
        this.mDaySeparatorInnerColor = resources.getColor(R.color.month_grid_lines);
        ICalendarThemeExt calendarTheme = ExtensionFactory.getCalendarTheme(getContext());
        if (calendarTheme.isThemeManagerEnable()) {
            int themeColor = calendarTheme.getThemeColor();
            this.mTodayAnimateColor = themeColor;
            this.mSelectedDayAnimateColor = themeColor;
            this.mClickedDayColor = themeColor;
        } else {
            this.mTodayAnimateColor = resources.getColor(R.color.today_highlight_color);
            this.mSelectedDayAnimateColor = resources.getColor(R.color.today_highlight_color);
            this.mClickedDayColor = resources.getColor(R.color.day_clicked_background_color);
        }
        this.mTodayDrawable = resources.getDrawable(R.drawable.today_blue_week_holo_light);
    }

    @Override
    protected void initView() {
        super.initView();
        if (!mInitialized) {
            Resources resources = getContext().getResources();
            mShowDetailsInMonth = Utils.getConfigBool(getContext(), R.bool.show_details_in_month);
            TEXT_SIZE_EVENT_TITLE = resources.getInteger(R.integer.text_size_event_title);
            TEXT_SIZE_MONTH_NUMBER = resources.getInteger(R.integer.text_size_month_number);
            SIDE_PADDING_MONTH_NUMBER = resources.getInteger(R.integer.month_day_number_margin);
            CONFLICT_COLOR = resources.getColor(R.color.month_dna_conflict_time_color);
            EVENT_TEXT_COLOR = resources.getColor(R.color.calendar_event_text_color);
            if (mScale != 1.0f) {
                TOP_PADDING_MONTH_NUMBER = (int) (TOP_PADDING_MONTH_NUMBER * mScale);
                TOP_PADDING_WEEK_NUMBER = (int) (TOP_PADDING_WEEK_NUMBER * mScale);
                SIDE_PADDING_MONTH_NUMBER = (int) (SIDE_PADDING_MONTH_NUMBER * mScale);
                SIDE_PADDING_WEEK_NUMBER = (int) (SIDE_PADDING_WEEK_NUMBER * mScale);
                SPACING_WEEK_NUMBER = (int) (SPACING_WEEK_NUMBER * mScale);
                TEXT_SIZE_MONTH_NUMBER = (int) (TEXT_SIZE_MONTH_NUMBER * mScale);
                TEXT_SIZE_EVENT = (int) (TEXT_SIZE_EVENT * mScale);
                TEXT_SIZE_EVENT_TITLE = (int) (TEXT_SIZE_EVENT_TITLE * mScale);
                TEXT_SIZE_MORE_EVENTS = (int) (TEXT_SIZE_MORE_EVENTS * mScale);
                TEXT_SIZE_MONTH_NAME = (int) (TEXT_SIZE_MONTH_NAME * mScale);
                TEXT_SIZE_WEEK_NUM = (int) (TEXT_SIZE_WEEK_NUM * mScale);
                DAY_SEPARATOR_OUTER_WIDTH = (int) (DAY_SEPARATOR_OUTER_WIDTH * mScale);
                DAY_SEPARATOR_INNER_WIDTH = (int) (DAY_SEPARATOR_INNER_WIDTH * mScale);
                DAY_SEPARATOR_VERTICAL_LENGTH = (int) (DAY_SEPARATOR_VERTICAL_LENGTH * mScale);
                DAY_SEPARATOR_VERTICAL_LENGHT_PORTRAIT = (int) (DAY_SEPARATOR_VERTICAL_LENGHT_PORTRAIT * mScale);
                EVENT_X_OFFSET_LANDSCAPE = (int) (EVENT_X_OFFSET_LANDSCAPE * mScale);
                EVENT_Y_OFFSET_LANDSCAPE = (int) (EVENT_Y_OFFSET_LANDSCAPE * mScale);
                EVENT_Y_OFFSET_PORTRAIT = (int) (EVENT_Y_OFFSET_PORTRAIT * mScale);
                EVENT_SQUARE_WIDTH = (int) (EVENT_SQUARE_WIDTH * mScale);
                EVENT_SQUARE_BORDER = (int) (EVENT_SQUARE_BORDER * mScale);
                EVENT_LINE_PADDING = (int) (EVENT_LINE_PADDING * mScale);
                EVENT_BOTTOM_PADDING = (int) (EVENT_BOTTOM_PADDING * mScale);
                EVENT_RIGHT_PADDING = (int) (EVENT_RIGHT_PADDING * mScale);
                DNA_MARGIN = (int) (DNA_MARGIN * mScale);
                DNA_WIDTH = (int) (DNA_WIDTH * mScale);
                DNA_ALL_DAY_HEIGHT = (int) (DNA_ALL_DAY_HEIGHT * mScale);
                DNA_MIN_SEGMENT_HEIGHT = (int) (DNA_MIN_SEGMENT_HEIGHT * mScale);
                DNA_SIDE_PADDING = (int) (DNA_SIDE_PADDING * mScale);
                DEFAULT_EDGE_SPACING = (int) (DEFAULT_EDGE_SPACING * mScale);
                DNA_ALL_DAY_WIDTH = (int) (DNA_ALL_DAY_WIDTH * mScale);
                TODAY_HIGHLIGHT_WIDTH = (int) (TODAY_HIGHLIGHT_WIDTH * mScale);
            }
            if (!mShowDetailsInMonth) {
                TOP_PADDING_MONTH_NUMBER += DNA_ALL_DAY_HEIGHT + DNA_MARGIN;
            }
            mInitialized = true;
        }
        this.mPadding = DEFAULT_EDGE_SPACING;
        loadColors(getContext());
        this.mMonthNumPaint = new Paint();
        this.mMonthNumPaint.setFakeBoldText(false);
        this.mMonthNumPaint.setAntiAlias(true);
        this.mMonthNumPaint.setTextSize(TEXT_SIZE_MONTH_NUMBER);
        this.mMonthNumPaint.setColor(this.mMonthNumColor);
        this.mMonthNumPaint.setStyle(Paint.Style.FILL);
        this.mMonthNumPaint.setTextAlign(Paint.Align.RIGHT);
        this.mMonthNumPaint.setTypeface(Typeface.DEFAULT);
        this.mMonthNumAscentHeight = (int) ((-this.mMonthNumPaint.ascent()) + 0.5f);
        this.mMonthNumHeight = (int) ((this.mMonthNumPaint.descent() - this.mMonthNumPaint.ascent()) + 0.5f);
        this.mEventPaint = new TextPaint();
        this.mEventPaint.setFakeBoldText(true);
        this.mEventPaint.setAntiAlias(true);
        this.mEventPaint.setTextSize(TEXT_SIZE_EVENT_TITLE);
        this.mEventPaint.setColor(this.mMonthEventColor);
        this.mSolidBackgroundEventPaint = new TextPaint(this.mEventPaint);
        this.mSolidBackgroundEventPaint.setColor(EVENT_TEXT_COLOR);
        this.mFramedEventPaint = new TextPaint(this.mSolidBackgroundEventPaint);
        this.mDeclinedEventPaint = new TextPaint();
        this.mDeclinedEventPaint.setFakeBoldText(true);
        this.mDeclinedEventPaint.setAntiAlias(true);
        this.mDeclinedEventPaint.setTextSize(TEXT_SIZE_EVENT_TITLE);
        this.mDeclinedEventPaint.setColor(this.mMonthDeclinedEventColor);
        this.mEventAscentHeight = (int) ((-this.mEventPaint.ascent()) + 0.5f);
        this.mEventHeight = (int) ((this.mEventPaint.descent() - this.mEventPaint.ascent()) + 0.5f);
        this.mEventExtrasPaint = new TextPaint();
        this.mEventExtrasPaint.setFakeBoldText(false);
        this.mEventExtrasPaint.setAntiAlias(true);
        this.mEventExtrasPaint.setStrokeWidth(EVENT_SQUARE_BORDER);
        this.mEventExtrasPaint.setTextSize(TEXT_SIZE_EVENT);
        this.mEventExtrasPaint.setColor(this.mMonthEventExtraColor);
        this.mEventExtrasPaint.setStyle(Paint.Style.FILL);
        this.mEventExtrasPaint.setTextAlign(Paint.Align.LEFT);
        this.mExtrasHeight = (int) ((this.mEventExtrasPaint.descent() - this.mEventExtrasPaint.ascent()) + 0.5f);
        this.mExtrasAscentHeight = (int) ((-this.mEventExtrasPaint.ascent()) + 0.5f);
        this.mExtrasDescent = (int) (this.mEventExtrasPaint.descent() + 0.5f);
        this.mEventDeclinedExtrasPaint = new TextPaint();
        this.mEventDeclinedExtrasPaint.setFakeBoldText(false);
        this.mEventDeclinedExtrasPaint.setAntiAlias(true);
        this.mEventDeclinedExtrasPaint.setStrokeWidth(EVENT_SQUARE_BORDER);
        this.mEventDeclinedExtrasPaint.setTextSize(TEXT_SIZE_EVENT);
        this.mEventDeclinedExtrasPaint.setColor(this.mMonthDeclinedExtrasColor);
        this.mEventDeclinedExtrasPaint.setStyle(Paint.Style.FILL);
        this.mEventDeclinedExtrasPaint.setTextAlign(Paint.Align.LEFT);
        this.mWeekNumPaint = new Paint();
        this.mWeekNumPaint.setFakeBoldText(false);
        this.mWeekNumPaint.setAntiAlias(true);
        this.mWeekNumPaint.setTextSize(TEXT_SIZE_WEEK_NUM);
        this.mWeekNumPaint.setColor(this.mWeekNumColor);
        this.mWeekNumPaint.setStyle(Paint.Style.FILL);
        this.mWeekNumPaint.setTextAlign(Paint.Align.RIGHT);
        this.mWeekNumAscentHeight = (int) ((-this.mWeekNumPaint.ascent()) + 0.5f);
        this.mDNAAllDayPaint = new Paint();
        this.mDNATimePaint = new Paint();
        this.mDNATimePaint.setColor(this.mMonthBusyBitsBusyTimeColor);
        this.mDNATimePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mDNATimePaint.setStrokeWidth(DNA_WIDTH);
        this.mDNATimePaint.setAntiAlias(false);
        this.mDNAAllDayPaint.setColor(this.mMonthBusyBitsConflictTimeColor);
        this.mDNAAllDayPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mDNAAllDayPaint.setStrokeWidth(DNA_ALL_DAY_WIDTH);
        this.mDNAAllDayPaint.setAntiAlias(false);
        this.mEventSquarePaint = new Paint();
        this.mEventSquarePaint.setStrokeWidth(EVENT_SQUARE_BORDER);
        this.mEventSquarePaint.setAntiAlias(false);
    }

    @Override
    public void setWeekParams(HashMap<String, Integer> map, String str) {
        super.setWeekParams(map, str);
        if (map.containsKey("orientation")) {
            this.mOrientation = map.get("orientation").intValue();
        }
        updateToday(str);
        this.mNumCells = this.mNumDays + 1;
        if (map.containsKey("selected_day")) {
            updateSelectedDayIndex(map.get("selected_day").intValue());
        }
        if (map.containsKey("animate_selected_day") && this.mHasSelectedDay) {
            synchronized (this.mAnimatorListener) {
                if (this.mSelectedDayAnimator != null) {
                    this.mSelectedDayAnimator.removeAllListeners();
                    this.mSelectedDayAnimator.cancel();
                }
                this.mSelectedDayAnimator = ObjectAnimator.ofInt(this, "animateSelectedDayAlpha", Math.max(this.mAnimateSelectedDayAlpha, 80), 255);
                this.mSelectedDayAnimator.setDuration(150L);
                this.mAnimatorListener.setAnimator(this.mSelectedDayAnimator);
                this.mAnimatorListener.setFadingIn(true);
                this.mSelectedDayAnimator.addListener(this.mAnimatorListener);
                this.mAnimateSelectedDay = true;
                this.mSelectedDayAnimator.start();
            }
        }
    }

    public boolean updateToday(String str) {
        this.mToday.timezone = str;
        this.mToday.setToNow();
        this.mToday.normalize(true);
        int julianDay = Time.getJulianDay(this.mToday.toMillis(false), this.mToday.gmtoff);
        if (julianDay >= this.mFirstJulianDay && julianDay < this.mFirstJulianDay + this.mNumDays) {
            this.mHasToday = true;
            this.mTodayIndex = julianDay - this.mFirstJulianDay;
        } else {
            this.mHasToday = false;
            this.mTodayIndex = -1;
        }
        return this.mHasToday;
    }

    private int updateSelectedDayIndex(int i) {
        if (i < 0) {
            return -1;
        }
        this.mSelectedDayIndex = i - Utils.getFirstDayOfWeek(this.mContext);
        if (this.mSelectedDayIndex < 0) {
            this.mSelectedDayIndex += this.mNumDays;
        }
        return this.mSelectedDayIndex;
    }

    public void setAnimateSelectedDayAlpha(int i) {
        this.mAnimateSelectedDayAlpha = i;
        invalidate();
    }

    public void setAnimateTodayAlpha(int i) {
        this.mAnimateTodayAlpha = i;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        drawWeekNums(canvas);
        drawDaySeparators(canvas);
        if (this.mHasSelectedDay && this.mAnimateSelectedDay) {
            drawSelectedDay(canvas);
        }
        if (mShowDetailsInMonth) {
            drawEvents(canvas);
        } else {
            if (this.mDna == null && this.mUnsortedEvents != null) {
                createDna(this.mUnsortedEvents);
            }
            drawDNA(canvas);
        }
        drawClick(canvas);
    }

    protected void drawSelectedDay(Canvas canvas) {
        this.r.top = DAY_SEPARATOR_INNER_WIDTH + (TODAY_HIGHLIGHT_WIDTH / 2);
        this.r.bottom = this.mHeight - ((int) Math.ceil(TODAY_HIGHLIGHT_WIDTH / 2.0f));
        this.p.setStyle(Paint.Style.STROKE);
        this.p.setStrokeWidth(TODAY_HIGHLIGHT_WIDTH);
        this.r.left = computeDayLeftPosition(this.mSelectedDayIndex) + (TODAY_HIGHLIGHT_WIDTH / 2);
        this.r.right = computeDayLeftPosition(this.mSelectedDayIndex + 1) - ((int) Math.ceil(TODAY_HIGHLIGHT_WIDTH / 2.0f));
        this.p.setColor(this.mSelectedDayAnimateColor | (this.mAnimateSelectedDayAlpha << 24));
        canvas.drawRect(this.r, this.p);
        this.p.setStyle(Paint.Style.FILL);
    }

    private int computeDayLeftPosition(int i) {
        int i2;
        int i3 = this.mWidth;
        if (this.mShowWeekNum) {
            i2 = SPACING_WEEK_NUMBER + this.mPadding;
            i3 -= i2;
        } else {
            i2 = 0;
        }
        return ((i * i3) / this.mNumDays) + i2;
    }

    @Override
    protected void drawDaySeparators(Canvas canvas) {
        int i;
        int i2;
        float[] fArr = new float[32];
        int i3 = 1;
        if (this.mShowWeekNum) {
            i = 28;
            float f = SPACING_WEEK_NUMBER + this.mPadding;
            fArr[0] = f;
            fArr[1] = 0.0f;
            fArr[2] = f;
            fArr[3] = this.mHeight;
            i2 = 4;
        } else {
            i = 24;
            i2 = 0;
            i3 = 0;
        }
        int i4 = i + 4;
        int i5 = i2 + 1;
        fArr[i2] = 0.0f;
        int i6 = i5 + 1;
        fArr[i5] = 0.0f;
        int i7 = i6 + 1;
        fArr[i6] = this.mWidth;
        int i8 = i7 + 1;
        fArr[i7] = 0.0f;
        int i9 = this.mHeight;
        while (i8 < i4) {
            int i10 = i8 + 1;
            float fComputeDayLeftPosition = computeDayLeftPosition((i8 / 4) - i3);
            fArr[i8] = fComputeDayLeftPosition;
            int i11 = i10 + 1;
            fArr[i10] = 0;
            int i12 = i11 + 1;
            fArr[i11] = fComputeDayLeftPosition;
            i8 = i12 + 1;
            fArr[i12] = i9;
        }
        this.p.setColor(this.mDaySeparatorInnerColor);
        this.p.setStrokeWidth(DAY_SEPARATOR_INNER_WIDTH);
        canvas.drawLines(fArr, 0, i4, this.p);
    }

    @Override
    protected void drawBackground(Canvas canvas) {
        this.r.top = DAY_SEPARATOR_INNER_WIDTH;
        this.r.bottom = this.mHeight;
        int i = this.mShowWeekNum ? 1 : 0;
        int i2 = i;
        if (!this.mOddMonth[i]) {
            do {
                i++;
                if (i >= this.mOddMonth.length) {
                    break;
                }
            } while (!this.mOddMonth[i]);
            this.r.right = computeDayLeftPosition(i - i2);
            this.r.left = 0;
            this.p.setColor(this.mMonthBGOtherColor);
            canvas.drawRect(this.r, this.p);
        } else {
            boolean[] zArr = this.mOddMonth;
            int length = this.mOddMonth.length - 1;
            if (!zArr[length]) {
                do {
                    length--;
                    if (length < i2) {
                        break;
                    }
                } while (!this.mOddMonth[length]);
                this.r.right = this.mWidth;
                this.r.left = computeDayLeftPosition((length + 1) - i2);
                this.p.setColor(this.mMonthBGOtherColor);
                canvas.drawRect(this.r, this.p);
            }
        }
        if (this.mHasToday) {
            this.p.setColor(this.mMonthBGTodayColor);
            this.r.left = computeDayLeftPosition(this.mTodayIndex);
            this.r.right = computeDayLeftPosition(this.mTodayIndex + 1);
            canvas.drawRect(this.r, this.p);
        }
    }

    private void drawClick(Canvas canvas) {
        if (this.mClickedDayIndex != -1) {
            int alpha = this.p.getAlpha();
            this.p.setColor(this.mClickedDayColor);
            this.p.setAlpha(128);
            this.r.left = computeDayLeftPosition(this.mClickedDayIndex);
            this.r.right = computeDayLeftPosition(this.mClickedDayIndex + 1);
            this.r.top = DAY_SEPARATOR_INNER_WIDTH;
            this.r.bottom = this.mHeight;
            canvas.drawRect(this.r, this.p);
            this.p.setAlpha(alpha);
        }
    }

    @Override
    protected void drawWeekNums(Canvas canvas) {
        int i;
        int i2;
        int i3;
        int i4;
        boolean z;
        boolean z2;
        int i5 = this.mTodayIndex;
        int i6 = this.mNumDays;
        if (this.mShowWeekNum) {
            canvas.drawText(this.mDayNumbers[0], SIDE_PADDING_WEEK_NUMBER + this.mPadding, this.mWeekNumAscentHeight + TOP_PADDING_WEEK_NUMBER, this.mWeekNumPaint);
            i = i5 + 1;
            i2 = i6 + 1;
            i3 = 0;
            i4 = 1;
        } else {
            i = i5;
            i2 = i6;
            i3 = -1;
            i4 = 0;
        }
        int i7 = this.mMonthNumAscentHeight + TOP_PADDING_MONTH_NUMBER;
        boolean z3 = this.mFocusDay[i4];
        this.mMonthNumPaint.setColor(z3 ? this.mMonthNumColor : this.mMonthNumOtherColor);
        int i8 = i4;
        boolean z4 = false;
        while (i8 < i2) {
            if (this.mHasToday && i == i8) {
                this.mMonthNumPaint.setColor(this.mMonthNumTodayColor);
                this.mMonthNumPaint.setFakeBoldText(true);
                z2 = i8 + 1 < i2 ? !this.mFocusDay[r1] : z3;
                z = true;
            } else {
                if (this.mFocusDay[i8] != z3) {
                    z3 = this.mFocusDay[i8];
                    this.mMonthNumPaint.setColor(z3 ? this.mMonthNumColor : this.mMonthNumOtherColor);
                }
                z = z4;
                z2 = z3;
            }
            int iComputeDayLeftPosition = computeDayLeftPosition(i8 - i3) - SIDE_PADDING_MONTH_NUMBER;
            float f = iComputeDayLeftPosition;
            canvas.drawText(this.mDayNumbers[i8], f, i7, this.mMonthNumPaint);
            OpCalendarCustomizationFactoryBase.getOpFactory(this.mContext).makeLunarCalendar(this.mContext).drawLunarString(this.mContext, canvas, this.mMonthNumPaint, iComputeDayLeftPosition, i7, getDayFromLocation(f));
            if (z) {
                this.mMonthNumPaint.setFakeBoldText(false);
                z4 = false;
            } else {
                z4 = z;
            }
            i8++;
            z3 = z2;
        }
    }

    protected void drawEvents(Canvas canvas) {
        Iterator<ArrayList<Event>> it;
        int i;
        int i2;
        int i3;
        if (this.mEvents == null) {
            return;
        }
        int i4 = -1;
        Iterator<ArrayList<Event>> it2 = this.mEvents.iterator();
        while (it2.hasNext()) {
            ArrayList<Event> next = it2.next();
            int i5 = i4 + 1;
            if (next == null || next.size() == 0) {
                it = it2;
            } else {
                int iComputeDayLeftPosition = computeDayLeftPosition(i5) + SIDE_PADDING_MONTH_NUMBER + 1;
                int iComputeDayLeftPosition2 = computeDayLeftPosition(i5 + 1);
                if (this.mOrientation == 1) {
                    i = EVENT_Y_OFFSET_PORTRAIT + this.mMonthNumHeight + TOP_PADDING_MONTH_NUMBER;
                    i2 = iComputeDayLeftPosition2 - (SIDE_PADDING_MONTH_NUMBER + 1);
                } else {
                    i = EVENT_Y_OFFSET_LANDSCAPE;
                    i2 = iComputeDayLeftPosition2 - EVENT_X_OFFSET_LANDSCAPE;
                }
                int i6 = i2;
                int i7 = i;
                boolean z = true;
                Iterator<Event> it3 = next.iterator();
                int iDrawEvent = i7;
                while (true) {
                    i3 = 0;
                    if (it3.hasNext()) {
                        it = it2;
                        int i8 = iDrawEvent;
                        Iterator<Event> it4 = it3;
                        iDrawEvent = drawEvent(canvas, it3.next(), iComputeDayLeftPosition, iDrawEvent, i6, it3.hasNext(), true, false);
                        if (iDrawEvent != i8) {
                            it2 = it;
                            it3 = it4;
                        } else {
                            z = false;
                            break;
                        }
                    } else {
                        it = it2;
                        break;
                    }
                }
                Iterator<Event> it5 = next.iterator();
                while (it5.hasNext()) {
                    int iDrawEvent2 = drawEvent(canvas, it5.next(), iComputeDayLeftPosition, i7, i6, it5.hasNext(), z, true);
                    if (iDrawEvent2 == i7) {
                        break;
                    }
                    i3++;
                    i7 = iDrawEvent2;
                }
                int size = next.size() - i3;
                if (size > 0) {
                    drawMoreEvents(canvas, size, iComputeDayLeftPosition);
                }
            }
            i4 = i5;
            it2 = it;
        }
    }

    protected int drawEvent(Canvas canvas, Event event, int i, int i2, int i3, boolean z, boolean z2, boolean z3) {
        int i4;
        int i5;
        int i6;
        boolean z4;
        TextPaint textPaint;
        int i7 = EVENT_SQUARE_BORDER + 1;
        int i8 = EVENT_SQUARE_BORDER / 2;
        boolean z5 = event.allDay;
        int i9 = this.mEventHeight;
        if (z5) {
            i9 += i7 * 2;
        } else if (z2) {
            i9 += this.mExtrasHeight;
        }
        int i10 = EVENT_BOTTOM_PADDING;
        if (z) {
            i9 += EVENT_LINE_PADDING;
            i10 += this.mExtrasHeight;
        }
        int i11 = i2 + i9;
        if (i10 + i11 > this.mHeight) {
            return i2;
        }
        if (!z3) {
            return i11;
        }
        boolean z6 = event.selfAttendeeStatus == 2;
        int declinedColorFromColor = event.color;
        if (z6) {
            declinedColorFromColor = Utils.getDeclinedColorFromColor(declinedColorFromColor);
        }
        if (z5) {
            this.r.left = i;
            this.r.right = i3 - i8;
            this.r.top = i2 + i8;
            this.r.bottom = ((i2 + this.mEventHeight) + (i7 * 2)) - i8;
            i4 = i + i7;
            i5 = i2 + this.mEventAscentHeight + i7;
            i6 = i3 - i7;
        } else {
            this.r.left = i;
            this.r.right = EVENT_SQUARE_WIDTH + i;
            this.r.bottom = i2 + this.mEventAscentHeight;
            this.r.top = this.r.bottom - EVENT_SQUARE_WIDTH;
            i4 = i + EVENT_SQUARE_WIDTH + EVENT_RIGHT_PADDING;
            i5 = i2 + this.mEventAscentHeight;
            i6 = i3;
        }
        Paint.Style style = Paint.Style.STROKE;
        if (event.selfAttendeeStatus != 3) {
            style = Paint.Style.FILL_AND_STROKE;
            z4 = z5;
        }
        this.mEventSquarePaint.setStyle(style);
        this.mEventSquarePaint.setColor(declinedColorFromColor);
        canvas.drawRect(this.r, this.mEventSquarePaint);
        float f = i6 - i4;
        CharSequence charSequenceEllipsize = TextUtils.ellipsize(event.title, this.mEventPaint, f, TextUtils.TruncateAt.END);
        if (z4) {
            textPaint = this.mSolidBackgroundEventPaint;
        } else if (z6) {
            textPaint = this.mDeclinedEventPaint;
        } else if (z5) {
            this.mFramedEventPaint.setColor(declinedColorFromColor);
            textPaint = this.mFramedEventPaint;
        } else {
            textPaint = this.mEventPaint;
        }
        float f2 = i4;
        canvas.drawText(charSequenceEllipsize.toString(), f2, i5, textPaint);
        int i12 = i2 + this.mEventHeight;
        if (z5) {
            i12 += i7 * 2;
        }
        if (z2 && !z5) {
            int i13 = this.mExtrasAscentHeight + i12;
            mStringBuilder.setLength(0);
            canvas.drawText(TextUtils.ellipsize(DateUtils.formatDateRange(getContext(), mFormatter, event.startMillis, event.endMillis, 524289, Utils.getTimeZone(getContext(), null)).toString(), this.mEventExtrasPaint, f, TextUtils.TruncateAt.END).toString(), f2, i13, z6 ? this.mEventDeclinedExtrasPaint : this.mEventExtrasPaint);
            i12 += this.mExtrasHeight;
        }
        return i12 + EVENT_LINE_PADDING;
    }

    protected void drawMoreEvents(Canvas canvas, int i, int i2) {
        int i3 = this.mHeight - (this.mExtrasDescent + EVENT_BOTTOM_PADDING);
        String quantityString = getContext().getResources().getQuantityString(R.plurals.month_more_events, i);
        this.mEventExtrasPaint.setAntiAlias(true);
        this.mEventExtrasPaint.setFakeBoldText(true);
        canvas.drawText(String.format(quantityString, Integer.valueOf(i)), i2, i3, this.mEventExtrasPaint);
        this.mEventExtrasPaint.setFakeBoldText(false);
    }

    protected void drawDNA(Canvas canvas) {
        if (this.mDna != null) {
            for (Utils.DNAStrand dNAStrand : this.mDna.values()) {
                if (dNAStrand.color != CONFLICT_COLOR && dNAStrand.points != null && dNAStrand.points.length != 0) {
                    this.mDNATimePaint.setColor(dNAStrand.color);
                    canvas.drawLines(dNAStrand.points, this.mDNATimePaint);
                }
            }
            Utils.DNAStrand dNAStrand2 = this.mDna.get(Integer.valueOf(CONFLICT_COLOR));
            if (dNAStrand2 != null && dNAStrand2.points != null && dNAStrand2.points.length != 0) {
                this.mDNATimePaint.setColor(dNAStrand2.color);
                canvas.drawLines(dNAStrand2.points, this.mDNATimePaint);
            }
            if (this.mDayXs == null) {
                return;
            }
            int length = this.mDayXs.length;
            int i = (DNA_ALL_DAY_WIDTH - DNA_WIDTH) / 2;
            if (dNAStrand2 != null && dNAStrand2.allDays != null && dNAStrand2.allDays.length == length) {
                for (int i2 = 0; i2 < length; i2++) {
                    if (dNAStrand2.allDays[i2] != 0) {
                        this.mDNAAllDayPaint.setColor(dNAStrand2.allDays[i2]);
                        canvas.drawLine(this.mDayXs[i2] + i, DNA_MARGIN, this.mDayXs[i2] + i, DNA_MARGIN + DNA_ALL_DAY_HEIGHT, this.mDNAAllDayPaint);
                    }
                }
            }
        }
    }

    @Override
    protected void updateSelectionPositions() {
        if (this.mHasSelectedDay) {
            int i = this.mSelectedDay - this.mWeekStart;
            if (i < 0) {
                i += 7;
            }
            int i2 = (this.mWidth - (this.mPadding * 2)) - SPACING_WEEK_NUMBER;
            this.mSelectedLeft = ((i * i2) / this.mNumDays) + this.mPadding;
            this.mSelectedRight = (((i + 1) * i2) / this.mNumDays) + this.mPadding;
            this.mSelectedLeft += SPACING_WEEK_NUMBER;
            this.mSelectedRight += SPACING_WEEK_NUMBER;
        }
    }

    public int getDayIndexFromLocation(float f) {
        float f2 = this.mShowWeekNum ? SPACING_WEEK_NUMBER + this.mPadding : this.mPadding;
        if (f < f2 || f > this.mWidth - this.mPadding) {
            return -1;
        }
        return (int) (((f - f2) * this.mNumDays) / ((this.mWidth - r0) - this.mPadding));
    }

    @Override
    public Time getDayFromLocation(float f) {
        int dayIndexFromLocation = getDayIndexFromLocation(f);
        if (dayIndexFromLocation == -1) {
            return null;
        }
        int i = this.mFirstJulianDay + dayIndexFromLocation;
        Time time = new Time(this.mTimeZone);
        Utils.setJulianDayInGeneral(time, i);
        return time;
    }

    @Override
    public boolean onHoverEvent(MotionEvent motionEvent) {
        Time dayFromLocation;
        int i;
        Context context = getContext();
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        if (!accessibilityManager.isEnabled() || !accessibilityManager.isTouchExplorationEnabled()) {
            return super.onHoverEvent(motionEvent);
        }
        if (motionEvent.getAction() != 10 && (dayFromLocation = getDayFromLocation(motionEvent.getX())) != null && (this.mLastHoverTime == null || Time.compare(dayFromLocation, this.mLastHoverTime) != 0)) {
            Long lValueOf = Long.valueOf(dayFromLocation.toMillis(true));
            String dateRange = Utils.formatDateRange(context, lValueOf.longValue(), lValueOf.longValue(), 16);
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(64);
            accessibilityEventObtain.getText().add(dateRange);
            if (mShowDetailsInMonth && this.mEvents != null) {
                ArrayList<Event> arrayList = this.mEvents.get((int) (((motionEvent.getX() - (SPACING_WEEK_NUMBER + this.mPadding)) * this.mNumDays) / ((this.mWidth - r0) - this.mPadding)));
                List<CharSequence> text = accessibilityEventObtain.getText();
                for (Event event : arrayList) {
                    text.add(event.getTitleAndLocation() + ". ");
                    if (!event.allDay) {
                        i = 21;
                        if (DateFormat.is24HourFormat(context)) {
                            i = 149;
                        }
                    } else {
                        i = 8212;
                    }
                    text.add(Utils.formatDateRange(context, event.startMillis, event.endMillis, i) + ". ");
                }
            }
            sendAccessibilityEventUnchecked(accessibilityEventObtain);
            this.mLastHoverTime = dayFromLocation;
        }
        return true;
    }

    public void setClickedDay(float f) {
        this.mClickedDayIndex = getDayIndexFromLocation(f);
        invalidate();
    }

    public void clearClickedDay() {
        this.mClickedDayIndex = -1;
        invalidate();
    }
}
