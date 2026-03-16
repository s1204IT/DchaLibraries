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
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MonthWeekEventsView extends SimpleWeekView {
    private static boolean mShowDetailsInMonth;
    private boolean mAnimateToday;
    private int mAnimateTodayAlpha;
    private final TodayAnimatorListener mAnimatorListener;
    private int mClickedDayColor;
    private int mClickedDayIndex;
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

    class TodayAnimatorListener extends AnimatorListenerAdapter {
        private volatile Animator mAnimator = null;
        private volatile boolean mFadingIn = false;

        TodayAnimatorListener() {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            synchronized (this) {
                if (this.mAnimator != animation) {
                    animation.removeAllListeners();
                    animation.cancel();
                    return;
                }
                if (this.mFadingIn) {
                    if (MonthWeekEventsView.this.mTodayAnimator != null) {
                        MonthWeekEventsView.this.mTodayAnimator.removeAllListeners();
                        MonthWeekEventsView.this.mTodayAnimator.cancel();
                    }
                    MonthWeekEventsView.this.mTodayAnimator = ObjectAnimator.ofInt(MonthWeekEventsView.this, "animateTodayAlpha", 255, 0);
                    this.mAnimator = MonthWeekEventsView.this.mTodayAnimator;
                    this.mFadingIn = false;
                    MonthWeekEventsView.this.mTodayAnimator.addListener(this);
                    MonthWeekEventsView.this.mTodayAnimator.setDuration(600L);
                    MonthWeekEventsView.this.mTodayAnimator.start();
                } else {
                    MonthWeekEventsView.this.mAnimateToday = false;
                    MonthWeekEventsView.this.mAnimateTodayAlpha = 0;
                    this.mAnimator.removeAllListeners();
                    this.mAnimator = null;
                    MonthWeekEventsView.this.mTodayAnimator = null;
                    MonthWeekEventsView.this.invalidate();
                }
            }
        }

        public void setAnimator(Animator animation) {
            this.mAnimator = animation;
        }

        public void setFadingIn(boolean fadingIn) {
            this.mFadingIn = fadingIn;
        }
    }

    private class FloatRef {
        float[] array;

        public FloatRef(int size) {
            this.array = new float[size];
        }
    }

    public MonthWeekEventsView(Context context) {
        super(context);
        this.mToday = new Time();
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
        this.mAnimatorListener = new TodayAnimatorListener();
    }

    public void setEvents(List<ArrayList<Event>> sortedEvents, ArrayList<Event> unsortedEvents) {
        setEvents(sortedEvents);
        createDna(unsortedEvents);
    }

    public void createDna(ArrayList<Event> unsortedEvents) {
        if (unsortedEvents == null || this.mWidth <= MIN_WEEK_WIDTH || getContext() == null) {
            this.mUnsortedEvents = unsortedEvents;
            this.mDna = null;
            return;
        }
        this.mUnsortedEvents = null;
        if (!mShowDetailsInMonth) {
            int numDays = this.mEvents.size();
            int effectiveWidth = this.mWidth - (this.mPadding * 2);
            if (this.mShowWeekNum) {
                effectiveWidth -= SPACING_WEEK_NUMBER;
            }
            DNA_ALL_DAY_WIDTH = (effectiveWidth / numDays) - (DNA_SIDE_PADDING * 2);
            this.mDNAAllDayPaint.setStrokeWidth(DNA_ALL_DAY_WIDTH);
            this.mDayXs = new int[numDays];
            for (int day = 0; day < numDays; day++) {
                this.mDayXs[day] = computeDayLeftPosition(day) + (DNA_WIDTH / 2) + DNA_SIDE_PADDING;
            }
            int top = DAY_SEPARATOR_INNER_WIDTH + DNA_MARGIN + DNA_ALL_DAY_HEIGHT + 1;
            int bottom = this.mHeight - DNA_MARGIN;
            this.mDna = Utils.createDNAStrands(this.mFirstJulianDay, unsortedEvents, top, bottom, DNA_MIN_SEGMENT_HEIGHT, this.mDayXs, getContext());
        }
    }

    public void setEvents(List<ArrayList<Event>> sortedEvents) {
        this.mEvents = sortedEvents;
        if (sortedEvents != null && sortedEvents.size() != this.mNumDays) {
            if (Log.isLoggable("MonthView", 6)) {
                Log.wtf("MonthView", "Events size must be same as days displayed: size=" + sortedEvents.size() + " days=" + this.mNumDays);
            }
            this.mEvents = null;
        }
    }

    protected void loadColors(Context context) {
        Resources res = context.getResources();
        this.mMonthWeekNumColor = res.getColor(R.color.month_week_num_color);
        this.mMonthNumColor = res.getColor(R.color.month_day_number);
        this.mMonthNumOtherColor = res.getColor(R.color.month_day_number_other);
        this.mMonthNumTodayColor = res.getColor(R.color.month_today_number);
        this.mMonthNameColor = this.mMonthNumColor;
        this.mMonthNameOtherColor = this.mMonthNumOtherColor;
        this.mMonthEventColor = res.getColor(R.color.month_event_color);
        this.mMonthDeclinedEventColor = res.getColor(R.color.agenda_item_declined_color);
        this.mMonthDeclinedExtrasColor = res.getColor(R.color.agenda_item_where_declined_text_color);
        this.mMonthEventExtraColor = res.getColor(R.color.month_event_extra_color);
        this.mMonthEventOtherColor = res.getColor(R.color.month_event_other_color);
        this.mMonthEventExtraOtherColor = res.getColor(R.color.month_event_extra_other_color);
        this.mMonthBGTodayColor = res.getColor(R.color.month_today_bgcolor);
        this.mMonthBGOtherColor = res.getColor(R.color.month_other_bgcolor);
        this.mMonthBGColor = res.getColor(R.color.month_bgcolor);
        this.mDaySeparatorInnerColor = res.getColor(R.color.month_grid_lines);
        this.mTodayAnimateColor = res.getColor(R.color.today_highlight_color);
        this.mClickedDayColor = res.getColor(R.color.day_clicked_background_color);
        this.mTodayDrawable = res.getDrawable(R.drawable.today_blue_week_holo_light);
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
    public void setWeekParams(HashMap<String, Integer> params, String tz) {
        super.setWeekParams(params, tz);
        if (params.containsKey("orientation")) {
            this.mOrientation = params.get("orientation").intValue();
        }
        updateToday(tz);
        this.mNumCells = this.mNumDays + 1;
        if (params.containsKey("animate_today") && this.mHasToday) {
            synchronized (this.mAnimatorListener) {
                if (this.mTodayAnimator != null) {
                    this.mTodayAnimator.removeAllListeners();
                    this.mTodayAnimator.cancel();
                }
                this.mTodayAnimator = ObjectAnimator.ofInt(this, "animateTodayAlpha", Math.max(this.mAnimateTodayAlpha, 80), 255);
                this.mTodayAnimator.setDuration(150L);
                this.mAnimatorListener.setAnimator(this.mTodayAnimator);
                this.mAnimatorListener.setFadingIn(true);
                this.mTodayAnimator.addListener(this.mAnimatorListener);
                this.mAnimateToday = true;
                this.mTodayAnimator.start();
            }
        }
    }

    public boolean updateToday(String tz) {
        this.mToday.timezone = tz;
        this.mToday.setToNow();
        this.mToday.normalize(true);
        int julianToday = Time.getJulianDay(this.mToday.toMillis(false), this.mToday.gmtoff);
        if (julianToday >= this.mFirstJulianDay && julianToday < this.mFirstJulianDay + this.mNumDays) {
            this.mHasToday = true;
            this.mTodayIndex = julianToday - this.mFirstJulianDay;
        } else {
            this.mHasToday = false;
            this.mTodayIndex = -1;
        }
        return this.mHasToday;
    }

    public void setAnimateTodayAlpha(int alpha) {
        this.mAnimateTodayAlpha = alpha;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        drawWeekNums(canvas);
        drawDaySeparators(canvas);
        if (this.mHasToday && this.mAnimateToday) {
            drawToday(canvas);
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

    protected void drawToday(Canvas canvas) {
        this.r.top = DAY_SEPARATOR_INNER_WIDTH + (TODAY_HIGHLIGHT_WIDTH / 2);
        this.r.bottom = this.mHeight - ((int) Math.ceil(TODAY_HIGHLIGHT_WIDTH / 2.0f));
        this.p.setStyle(Paint.Style.STROKE);
        this.p.setStrokeWidth(TODAY_HIGHLIGHT_WIDTH);
        this.r.left = computeDayLeftPosition(this.mTodayIndex) + (TODAY_HIGHLIGHT_WIDTH / 2);
        this.r.right = computeDayLeftPosition(this.mTodayIndex + 1) - ((int) Math.ceil(TODAY_HIGHLIGHT_WIDTH / 2.0f));
        this.p.setColor(this.mTodayAnimateColor | (this.mAnimateTodayAlpha << 24));
        canvas.drawRect(this.r, this.p);
        this.p.setStyle(Paint.Style.FILL);
    }

    private int computeDayLeftPosition(int day) {
        int effectiveWidth = this.mWidth;
        int xOffset = 0;
        if (this.mShowWeekNum) {
            xOffset = SPACING_WEEK_NUMBER + this.mPadding;
            effectiveWidth -= xOffset;
        }
        int x = ((day * effectiveWidth) / this.mNumDays) + xOffset;
        return x;
    }

    @Override
    protected void drawDaySeparators(Canvas canvas) {
        float[] lines = new float[32];
        int count = 24;
        int wkNumOffset = 0;
        int i = 0;
        if (this.mShowWeekNum) {
            int xOffset = SPACING_WEEK_NUMBER + this.mPadding;
            count = 24 + 4;
            int i2 = 0 + 1;
            lines[0] = xOffset;
            int i3 = i2 + 1;
            lines[i2] = 0.0f;
            int i4 = i3 + 1;
            lines[i3] = xOffset;
            i = i4 + 1;
            lines[i4] = this.mHeight;
            wkNumOffset = 0 + 1;
        }
        int count2 = count + 4;
        int i5 = i + 1;
        lines[i] = 0.0f;
        int i6 = i5 + 1;
        lines[i5] = 0.0f;
        int i7 = i6 + 1;
        lines[i6] = this.mWidth;
        lines[i7] = 0.0f;
        int y1 = this.mHeight;
        int i8 = i7 + 1;
        while (i8 < count2) {
            int x = computeDayLeftPosition((i8 / 4) - wkNumOffset);
            int i9 = i8 + 1;
            lines[i8] = x;
            int i10 = i9 + 1;
            lines[i9] = 0;
            int i11 = i10 + 1;
            lines[i10] = x;
            i8 = i11 + 1;
            lines[i11] = y1;
        }
        this.p.setColor(this.mDaySeparatorInnerColor);
        this.p.setStrokeWidth(DAY_SEPARATOR_INNER_WIDTH);
        canvas.drawLines(lines, 0, count2, this.p);
    }

    @Override
    protected void drawBackground(Canvas canvas) {
        int i = 0;
        int offset = 0;
        this.r.top = DAY_SEPARATOR_INNER_WIDTH;
        this.r.bottom = this.mHeight;
        if (this.mShowWeekNum) {
            i = 0 + 1;
            offset = 0 + 1;
        }
        if (!this.mOddMonth[i]) {
            do {
                i++;
                if (i >= this.mOddMonth.length) {
                    break;
                }
            } while (!this.mOddMonth[i]);
            this.r.right = computeDayLeftPosition(i - offset);
            this.r.left = 0;
            this.p.setColor(this.mMonthBGOtherColor);
            canvas.drawRect(this.r, this.p);
        } else {
            boolean[] zArr = this.mOddMonth;
            int i2 = this.mOddMonth.length - 1;
            if (!zArr[i2]) {
                do {
                    i2--;
                    if (i2 < offset) {
                        break;
                    }
                } while (!this.mOddMonth[i2]);
                this.r.right = this.mWidth;
                this.r.left = computeDayLeftPosition((i2 + 1) - offset);
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
        int i = 0;
        int offset = -1;
        int todayIndex = this.mTodayIndex;
        int numCount = this.mNumDays;
        if (this.mShowWeekNum) {
            int x = SIDE_PADDING_WEEK_NUMBER + this.mPadding;
            int y = this.mWeekNumAscentHeight + TOP_PADDING_WEEK_NUMBER;
            canvas.drawText(this.mDayNumbers[0], x, y, this.mWeekNumPaint);
            numCount++;
            i = 0 + 1;
            todayIndex++;
            offset = (-1) + 1;
        }
        int y2 = this.mMonthNumAscentHeight + TOP_PADDING_MONTH_NUMBER;
        boolean isFocusMonth = this.mFocusDay[i];
        boolean isBold = false;
        this.mMonthNumPaint.setColor(isFocusMonth ? this.mMonthNumColor : this.mMonthNumOtherColor);
        while (i < numCount) {
            if (this.mHasToday && todayIndex == i) {
                this.mMonthNumPaint.setColor(this.mMonthNumTodayColor);
                isBold = true;
                this.mMonthNumPaint.setFakeBoldText(true);
                if (i + 1 < numCount) {
                    isFocusMonth = !this.mFocusDay[i + 1];
                }
            } else if (this.mFocusDay[i] != isFocusMonth) {
                isFocusMonth = this.mFocusDay[i];
                this.mMonthNumPaint.setColor(isFocusMonth ? this.mMonthNumColor : this.mMonthNumOtherColor);
            }
            int x2 = computeDayLeftPosition(i - offset) - SIDE_PADDING_MONTH_NUMBER;
            canvas.drawText(this.mDayNumbers[i], x2, y2, this.mMonthNumPaint);
            if (isBold) {
                isBold = false;
                this.mMonthNumPaint.setFakeBoldText(false);
            }
            i++;
        }
    }

    protected void drawEvents(Canvas canvas) {
        int ySquare;
        int rightEdge;
        if (this.mEvents != null) {
            int day = -1;
            for (ArrayList<Event> eventDay : this.mEvents) {
                day++;
                if (eventDay != null && eventDay.size() != 0) {
                    int xSquare = computeDayLeftPosition(day) + SIDE_PADDING_MONTH_NUMBER + 1;
                    int rightEdge2 = computeDayLeftPosition(day + 1);
                    if (this.mOrientation == 1) {
                        ySquare = EVENT_Y_OFFSET_PORTRAIT + this.mMonthNumHeight + TOP_PADDING_MONTH_NUMBER;
                        rightEdge = rightEdge2 - (SIDE_PADDING_MONTH_NUMBER + 1);
                    } else {
                        ySquare = EVENT_Y_OFFSET_LANDSCAPE;
                        rightEdge = rightEdge2 - EVENT_X_OFFSET_LANDSCAPE;
                    }
                    boolean showTimes = true;
                    Iterator<Event> iter = eventDay.iterator();
                    int yTest = ySquare;
                    while (true) {
                        if (!iter.hasNext()) {
                            break;
                        }
                        Event event = iter.next();
                        int newY = drawEvent(canvas, event, xSquare, yTest, rightEdge, iter.hasNext(), true, false);
                        if (newY == yTest) {
                            showTimes = false;
                            break;
                        }
                        yTest = newY;
                    }
                    int eventCount = 0;
                    Iterator<Event> iter2 = eventDay.iterator();
                    while (iter2.hasNext()) {
                        Event event2 = iter2.next();
                        int newY2 = drawEvent(canvas, event2, xSquare, ySquare, rightEdge, iter2.hasNext(), showTimes, true);
                        if (newY2 == ySquare) {
                            break;
                        }
                        eventCount++;
                        ySquare = newY2;
                    }
                    int remaining = eventDay.size() - eventCount;
                    if (remaining > 0) {
                        drawMoreEvents(canvas, remaining, xSquare);
                    }
                }
            }
        }
    }

    protected int drawEvent(Canvas canvas, Event event, int x, int y, int rightEdge, boolean moreEvents, boolean showTimes, boolean doDraw) {
        int textX;
        int textY;
        int textRightEdge;
        Paint textPaint;
        int BORDER_SPACE = EVENT_SQUARE_BORDER + 1;
        int STROKE_WIDTH_ADJ = EVENT_SQUARE_BORDER / 2;
        boolean allDay = event.allDay;
        int eventRequiredSpace = this.mEventHeight;
        if (allDay) {
            eventRequiredSpace += BORDER_SPACE * 2;
        } else if (showTimes) {
            eventRequiredSpace += this.mExtrasHeight;
        }
        int reservedSpace = EVENT_BOTTOM_PADDING;
        if (moreEvents) {
            eventRequiredSpace += EVENT_LINE_PADDING;
            reservedSpace += this.mExtrasHeight;
        }
        if (y + eventRequiredSpace + reservedSpace > this.mHeight) {
            return y;
        }
        if (!doDraw) {
            return y + eventRequiredSpace;
        }
        boolean isDeclined = event.selfAttendeeStatus == 2;
        int color = event.color;
        if (isDeclined) {
            color = Utils.getDeclinedColorFromColor(color);
        }
        if (allDay) {
            this.r.left = x;
            this.r.right = rightEdge - STROKE_WIDTH_ADJ;
            this.r.top = y + STROKE_WIDTH_ADJ;
            this.r.bottom = ((this.mEventHeight + y) + (BORDER_SPACE * 2)) - STROKE_WIDTH_ADJ;
            textX = x + BORDER_SPACE;
            textY = this.mEventAscentHeight + y + BORDER_SPACE;
            textRightEdge = rightEdge - BORDER_SPACE;
        } else {
            this.r.left = x;
            this.r.right = EVENT_SQUARE_WIDTH + x;
            this.r.bottom = this.mEventAscentHeight + y;
            this.r.top = this.r.bottom - EVENT_SQUARE_WIDTH;
            textX = EVENT_SQUARE_WIDTH + x + EVENT_RIGHT_PADDING;
            textY = y + this.mEventAscentHeight;
            textRightEdge = rightEdge;
        }
        Paint.Style boxStyle = Paint.Style.STROKE;
        boolean solidBackground = false;
        if (event.selfAttendeeStatus != 3) {
            boxStyle = Paint.Style.FILL_AND_STROKE;
            if (allDay) {
                solidBackground = true;
            }
        }
        this.mEventSquarePaint.setStyle(boxStyle);
        this.mEventSquarePaint.setColor(color);
        canvas.drawRect(this.r, this.mEventSquarePaint);
        float avail = textRightEdge - textX;
        CharSequence text = TextUtils.ellipsize(event.title, this.mEventPaint, avail, TextUtils.TruncateAt.END);
        if (solidBackground) {
            textPaint = this.mSolidBackgroundEventPaint;
        } else if (isDeclined) {
            textPaint = this.mDeclinedEventPaint;
        } else if (allDay) {
            this.mFramedEventPaint.setColor(color);
            textPaint = this.mFramedEventPaint;
        } else {
            textPaint = this.mEventPaint;
        }
        canvas.drawText(text.toString(), textX, textY, textPaint);
        int y2 = y + this.mEventHeight;
        if (allDay) {
            y2 += BORDER_SPACE * 2;
        }
        if (showTimes && !allDay) {
            int textY2 = y2 + this.mExtrasAscentHeight;
            mStringBuilder.setLength(0);
            CharSequence text2 = DateUtils.formatDateRange(getContext(), mFormatter, event.startMillis, event.endMillis, 524289, Utils.getTimeZone(getContext(), null)).toString();
            canvas.drawText(TextUtils.ellipsize(text2, this.mEventExtrasPaint, avail, TextUtils.TruncateAt.END).toString(), textX, textY2, isDeclined ? this.mEventDeclinedExtrasPaint : this.mEventExtrasPaint);
            y2 += this.mExtrasHeight;
        }
        return y2 + EVENT_LINE_PADDING;
    }

    protected void drawMoreEvents(Canvas canvas, int remainingEvents, int x) {
        int y = this.mHeight - (this.mExtrasDescent + EVENT_BOTTOM_PADDING);
        String text = getContext().getResources().getQuantityString(R.plurals.month_more_events, remainingEvents);
        this.mEventExtrasPaint.setAntiAlias(true);
        this.mEventExtrasPaint.setFakeBoldText(true);
        canvas.drawText(String.format(text, Integer.valueOf(remainingEvents)), x, y, this.mEventExtrasPaint);
        this.mEventExtrasPaint.setFakeBoldText(false);
    }

    protected void drawDNA(Canvas canvas) {
        if (this.mDna != null) {
            for (Utils.DNAStrand strand : this.mDna.values()) {
                if (strand.color != CONFLICT_COLOR && strand.points != null && strand.points.length != 0) {
                    this.mDNATimePaint.setColor(strand.color);
                    canvas.drawLines(strand.points, this.mDNATimePaint);
                }
            }
            Utils.DNAStrand strand2 = this.mDna.get(Integer.valueOf(CONFLICT_COLOR));
            if (strand2 != null && strand2.points != null && strand2.points.length != 0) {
                this.mDNATimePaint.setColor(strand2.color);
                canvas.drawLines(strand2.points, this.mDNATimePaint);
            }
            if (this.mDayXs != null) {
                int numDays = this.mDayXs.length;
                int xOffset = (DNA_ALL_DAY_WIDTH - DNA_WIDTH) / 2;
                if (strand2 != null && strand2.allDays != null && strand2.allDays.length == numDays) {
                    for (int i = 0; i < numDays; i++) {
                        if (strand2.allDays[i] != 0) {
                            this.mDNAAllDayPaint.setColor(strand2.allDays[i]);
                            canvas.drawLine(this.mDayXs[i] + xOffset, DNA_MARGIN, this.mDayXs[i] + xOffset, DNA_MARGIN + DNA_ALL_DAY_HEIGHT, this.mDNAAllDayPaint);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void updateSelectionPositions() {
        if (this.mHasSelectedDay) {
            int selectedPosition = this.mSelectedDay - this.mWeekStart;
            if (selectedPosition < 0) {
                selectedPosition += 7;
            }
            int effectiveWidth = (this.mWidth - (this.mPadding * 2)) - SPACING_WEEK_NUMBER;
            this.mSelectedLeft = ((selectedPosition * effectiveWidth) / this.mNumDays) + this.mPadding;
            this.mSelectedRight = (((selectedPosition + 1) * effectiveWidth) / this.mNumDays) + this.mPadding;
            this.mSelectedLeft += SPACING_WEEK_NUMBER;
            this.mSelectedRight += SPACING_WEEK_NUMBER;
        }
    }

    public int getDayIndexFromLocation(float x) {
        int dayStart = this.mShowWeekNum ? SPACING_WEEK_NUMBER + this.mPadding : this.mPadding;
        if (x < dayStart || x > this.mWidth - this.mPadding) {
            return -1;
        }
        return (int) (((x - dayStart) * this.mNumDays) / ((this.mWidth - dayStart) - this.mPadding));
    }

    @Override
    public Time getDayFromLocation(float x) {
        int dayPosition = getDayIndexFromLocation(x);
        if (dayPosition == -1) {
            return null;
        }
        int day = this.mFirstJulianDay + dayPosition;
        Time time = new Time(this.mTimeZone);
        if (this.mWeek == 0) {
            if (day < 2440588) {
                day++;
            } else if (day == 2440588) {
                time.set(1, 0, 1970);
                time.normalize(true);
                return time;
            }
        }
        time.setJulianDay(day);
        return time;
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        Time hover;
        int flags;
        Context context = getContext();
        AccessibilityManager am = (AccessibilityManager) context.getSystemService("accessibility");
        if (!am.isEnabled() || !am.isTouchExplorationEnabled()) {
            return super.onHoverEvent(event);
        }
        if (event.getAction() != 10 && (hover = getDayFromLocation(event.getX())) != null && (this.mLastHoverTime == null || Time.compare(hover, this.mLastHoverTime) != 0)) {
            Long millis = Long.valueOf(hover.toMillis(true));
            String date = Utils.formatDateRange(context, millis.longValue(), millis.longValue(), 16);
            AccessibilityEvent accessEvent = AccessibilityEvent.obtain(64);
            accessEvent.getText().add(date);
            if (mShowDetailsInMonth && this.mEvents != null) {
                int dayStart = SPACING_WEEK_NUMBER + this.mPadding;
                int dayPosition = (int) (((event.getX() - dayStart) * this.mNumDays) / ((this.mWidth - dayStart) - this.mPadding));
                ArrayList<Event> events = this.mEvents.get(dayPosition);
                List<CharSequence> text = accessEvent.getText();
                for (Event e : events) {
                    text.add(e.getTitleAndLocation() + ". ");
                    if (!e.allDay) {
                        flags = 20 | 1;
                        if (DateFormat.is24HourFormat(context)) {
                            flags |= 128;
                        }
                    } else {
                        flags = 20 | 8192;
                    }
                    text.add(Utils.formatDateRange(context, e.startMillis, e.endMillis, flags) + ". ");
                }
            }
            sendAccessibilityEventUnchecked(accessEvent);
            this.mLastHoverTime = hover;
        }
        return true;
    }

    public void setClickedDay(float xLocation) {
        this.mClickedDayIndex = getDayIndexFromLocation(xLocation);
        invalidate();
    }

    public void clearClickedDay() {
        this.mClickedDayIndex = -1;
        invalidate();
    }
}
