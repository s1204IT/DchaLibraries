package com.android.calendar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.EdgeEffect;
import android.widget.ImageView;
import android.widget.OverScroller;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DayView extends View implements ScaleGestureDetector.OnScaleGestureListener, View.OnClickListener, View.OnCreateContextMenuListener, View.OnLongClickListener {
    private static int mBgColor;
    private static int mCalendarAmPmLabel;
    private static int mCalendarDateBannerTextColor;
    private static int mCalendarGridAreaSelected;
    private static int mCalendarGridLineInnerHorizontalColor;
    private static int mCalendarGridLineInnerVerticalColor;
    private static int mCalendarHourLabelColor;
    private static int mClickedColor;
    private static int mEventTextColor;
    private static int mFutureBgColor;
    private static int mFutureBgColorRes;
    private static int mMoreEventsTextColor;
    private static int mNewEventHintColor;
    private static int mOnDownDelay;
    private static int mPressedColor;
    private static int mWeek_saturdayColor;
    private static int mWeek_sundayColor;
    private final int OVERFLING_DISTANCE;
    private final Pattern drawTextSanitizerFilter;
    protected Drawable mAcceptedOrTentativeEventBoxDrawable;
    private AccessibilityManager mAccessibilityMgr;
    private ArrayList<Event> mAllDayEvents;
    private StaticLayout[] mAllDayLayouts;
    ObjectAnimator mAlldayAnimator;
    ObjectAnimator mAlldayEventAnimator;
    private int mAlldayHeight;
    private String mAmString;
    private int mAnimateDayEventHeight;
    private int mAnimateDayHeight;
    private boolean mAnimateToday;
    private int mAnimateTodayAlpha;
    private float mAnimationDistance;
    AnimatorListenerAdapter mAnimatorListener;
    Time mBaseDate;
    private final Typeface mBold;
    private boolean mCallEdgeEffectOnAbsorb;
    private final Runnable mCancelCallback;
    private boolean mCancellingAnimations;
    private int mCellHeightBeforeScaleGesture;
    private int mCellWidth;
    private final Runnable mClearClick;
    private Event mClickedEvent;
    private int mClickedYLocation;
    protected final Drawable mCollapseAlldayDrawable;
    private boolean mComputeSelectedEvents;
    protected Context mContext;
    private final ContextMenuHandler mContextMenuHandler;
    private final ContinueScroll mContinueScroll;
    private final CalendarController mController;
    private final String mCreateNewEventString;
    private Time mCurrentTime;
    protected final Drawable mCurrentTimeAnimateLine;
    protected final Drawable mCurrentTimeLine;
    private int mDateStrWidth;
    private String[] mDayStrs;
    private String[] mDayStrs2Letter;
    private final DeleteEventHelper mDeleteEventHelper;
    private final Rect mDestRect;
    private final DismissPopup mDismissPopup;
    private long mDownTouchTime;
    private int[] mEarliestStartHour;
    private final EdgeEffect mEdgeEffectBottom;
    private final EdgeEffect mEdgeEffectTop;
    private String mEventCountTemplate;
    protected final EventGeometry mEventGeometry;
    private final EventLoader mEventLoader;
    private final Paint mEventTextPaint;
    private ArrayList<Event> mEvents;
    private int mEventsAlpha;
    private ObjectAnimator mEventsCrossFadeAnimation;
    private final Rect mExpandAllDayRect;
    protected final Drawable mExpandAlldayDrawable;
    private int mFirstCell;
    private int mFirstDayOfWeek;
    private int mFirstHour;
    private int mFirstHourOffset;
    private int mFirstJulianDay;
    private int mFirstVisibleDate;
    private int mFirstVisibleDayOfWeek;
    private float mGestureCenterHour;
    private final GestureDetector mGestureDetector;
    private int mGridAreaHeight;
    private final ScrollInterpolator mHScrollInterpolator;
    private boolean mHandleActionUp;
    private Handler mHandler;
    private boolean[] mHasAllDayEvent;
    private String[] mHourStrs;
    private int mHoursTextHeight;
    private int mHoursWidth;
    private float mInitialScrollX;
    private float mInitialScrollY;
    private boolean mIs24HourFormat;
    private boolean mIsAccessibilityEnabled;
    private int mLastJulianDay;
    private long mLastPopupEventID;
    private long mLastReloadMillis;
    private Event mLastSelectedEventForAccessibility;
    private int mLastSelectionDayForAccessibility;
    private int mLastSelectionHourForAccessibility;
    private float mLastVelocity;
    private StaticLayout[] mLayouts;
    private float[] mLines;
    private int mLoadedFirstJulianDay;
    private final CharSequence[] mLongPressItems;
    private String mLongPressTitle;
    private int mMaxAlldayEvents;
    private int mMaxUnexpandedAlldayEventCount;
    private int mMaxViewStartY;
    private int mMonthLength;
    ObjectAnimator mMoreAlldayEventsAnimator;
    private final String mNewEventHintString;
    protected int mNumDays;
    private int mNumHours;
    private boolean mOnFlingCalled;
    private final Paint mPaint;
    protected boolean mPaused;
    private String mPmString;
    private PopupWindow mPopup;
    private View mPopupView;
    private final Rect mPrevBox;
    private Event mPrevSelectedEvent;
    private int mPreviousDirection;
    private boolean mRecalCenterHour;
    private final Rect mRect;
    private boolean mRemeasure;
    protected final Resources mResources;
    private Event mSavedClickedEvent;
    ScaleGestureDetector mScaleGestureDetector;
    private int mScrollStartY;
    private final OverScroller mScroller;
    private boolean mScrolling;
    private Event mSelectedEvent;
    private Event mSelectedEventForAccessibility;
    private final ArrayList<Event> mSelectedEvents;
    boolean mSelectionAllday;
    private int mSelectionDay;
    private int mSelectionDayForAccessibility;
    private int mSelectionHour;
    private int mSelectionHourForAccessibility;
    private int mSelectionMode;
    private final Paint mSelectionPaint;
    private final Rect mSelectionRect;
    private final Runnable mSetClick;
    private int[] mSkippedAlldayEvents;
    private boolean mStartingScroll;
    private float mStartingSpanY;
    private final Runnable mTZUpdater;
    ObjectAnimator mTodayAnimator;
    private final TodayAnimatorListener mTodayAnimatorListener;
    protected final Drawable mTodayHeaderDrawable;
    private int mTodayJulianDay;
    private boolean mTouchExplorationEnabled;
    private int mTouchMode;
    private boolean mTouchStartedInAlldayArea;
    private final UpdateCurrentTime mUpdateCurrentTime;
    private boolean mUpdateToast;
    private int mViewHeight;
    private int mViewStartX;
    private int mViewStartY;
    private final ViewSwitcher mViewSwitcher;
    private int mViewWidth;
    private static String TAG = "DayView";
    private static boolean DEBUG = false;
    private static boolean DEBUG_SCALING = false;
    private static float mScale = 0.0f;
    private static int DEFAULT_CELL_HEIGHT = 64;
    private static int MAX_CELL_HEIGHT = 150;
    private static int MIN_Y_SPAN = 100;
    private static final String[] CALENDARS_PROJECTION = {"_id", "calendar_access_level", "ownerAccount"};
    private static int mHorizontalSnapBackThreshold = 128;
    protected static StringBuilder mStringBuilder = new StringBuilder(50);
    protected static Formatter mFormatter = new Formatter(mStringBuilder, Locale.getDefault());
    private static float GRID_LINE_LEFT_MARGIN = 0.0f;
    private static int SINGLE_ALLDAY_HEIGHT = 34;
    private static float MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT = 28.0f;
    private static int MAX_UNEXPANDED_ALLDAY_HEIGHT = (int) (MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT * 4.0f);
    private static int MIN_HOURS_HEIGHT = 180;
    private static int ALLDAY_TOP_MARGIN = 1;
    private static int MAX_HEIGHT_OF_ONE_ALLDAY_EVENT = 34;
    private static int HOURS_TOP_MARGIN = 2;
    private static int HOURS_LEFT_MARGIN = 2;
    private static int HOURS_RIGHT_MARGIN = 4;
    private static int HOURS_MARGIN = HOURS_LEFT_MARGIN + HOURS_RIGHT_MARGIN;
    private static int NEW_EVENT_MARGIN = 4;
    private static int NEW_EVENT_WIDTH = 2;
    private static int NEW_EVENT_MAX_LENGTH = 16;
    private static int CURRENT_TIME_LINE_SIDE_BUFFER = 4;
    private static int CURRENT_TIME_LINE_TOP_OFFSET = 2;
    private static int DAY_HEADER_ONE_DAY_LEFT_MARGIN = 0;
    private static int DAY_HEADER_ONE_DAY_RIGHT_MARGIN = 5;
    private static int DAY_HEADER_ONE_DAY_BOTTOM_MARGIN = 6;
    private static int DAY_HEADER_RIGHT_MARGIN = 4;
    private static int DAY_HEADER_BOTTOM_MARGIN = 3;
    private static float DAY_HEADER_FONT_SIZE = 14.0f;
    private static float DATE_HEADER_FONT_SIZE = 32.0f;
    private static float NORMAL_FONT_SIZE = 12.0f;
    private static float EVENT_TEXT_FONT_SIZE = 12.0f;
    private static float HOURS_TEXT_SIZE = 12.0f;
    private static float AMPM_TEXT_SIZE = 9.0f;
    private static int MIN_HOURS_WIDTH = 96;
    private static int MIN_CELL_WIDTH_FOR_TEXT = 20;
    private static float MIN_EVENT_HEIGHT = 24.0f;
    private static int CALENDAR_COLOR_SQUARE_SIZE = 10;
    private static int EVENT_RECT_TOP_MARGIN = 1;
    private static int EVENT_RECT_BOTTOM_MARGIN = 0;
    private static int EVENT_RECT_LEFT_MARGIN = 1;
    private static int EVENT_RECT_RIGHT_MARGIN = 0;
    private static int EVENT_RECT_STROKE_WIDTH = 2;
    private static int EVENT_TEXT_TOP_MARGIN = 2;
    private static int EVENT_TEXT_BOTTOM_MARGIN = 2;
    private static int EVENT_TEXT_LEFT_MARGIN = 6;
    private static int EVENT_TEXT_RIGHT_MARGIN = 6;
    private static int ALL_DAY_EVENT_RECT_BOTTOM_MARGIN = 1;
    private static int EVENT_ALL_DAY_TEXT_TOP_MARGIN = EVENT_TEXT_TOP_MARGIN;
    private static int EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN = EVENT_TEXT_BOTTOM_MARGIN;
    private static int EVENT_ALL_DAY_TEXT_LEFT_MARGIN = EVENT_TEXT_LEFT_MARGIN;
    private static int EVENT_ALL_DAY_TEXT_RIGHT_MARGIN = EVENT_TEXT_RIGHT_MARGIN;
    private static int EXPAND_ALL_DAY_BOTTOM_MARGIN = 10;
    private static int EVENT_SQUARE_WIDTH = 10;
    private static int EVENT_LINE_PADDING = 4;
    private static int NEW_EVENT_HINT_FONT_SIZE = 12;
    private static int mMoreAlldayEventsTextAlpha = 76;
    private static int mCellHeight = 0;
    private static int mMinCellHeight = 32;
    private static int mScaledPagingTouchSlop = 0;
    private static boolean mUseExpandIcon = true;
    private static int DAY_HEADER_HEIGHT = 45;
    private static int MULTI_DAY_HEADER_HEIGHT = DAY_HEADER_HEIGHT;
    private static int ONE_DAY_HEADER_HEIGHT = DAY_HEADER_HEIGHT;
    private static boolean mShowAllAllDayEvents = false;
    private static int sCounter = 0;

    static int access$1104() {
        int i = sCounter + 1;
        sCounter = i;
        return i;
    }

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
                if (!this.mFadingIn) {
                    DayView.this.mAnimateToday = false;
                    DayView.this.mAnimateTodayAlpha = 0;
                    this.mAnimator.removeAllListeners();
                    this.mAnimator = null;
                    DayView.this.mTodayAnimator = null;
                    DayView.this.invalidate();
                } else {
                    if (DayView.this.mTodayAnimator != null) {
                        DayView.this.mTodayAnimator.removeAllListeners();
                        DayView.this.mTodayAnimator.cancel();
                    }
                    DayView.this.mTodayAnimator = ObjectAnimator.ofInt(DayView.this, "animateTodayAlpha", 255, 0);
                    this.mAnimator = DayView.this.mTodayAnimator;
                    this.mFadingIn = false;
                    DayView.this.mTodayAnimator.addListener(this);
                    DayView.this.mTodayAnimator.setDuration(600L);
                    DayView.this.mTodayAnimator.start();
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

    public DayView(Context context, CalendarController controller, ViewSwitcher viewSwitcher, EventLoader eventLoader, int numDays) {
        int eventTextSizeId;
        super(context);
        this.mStartingScroll = false;
        this.mPaused = true;
        this.mContinueScroll = new ContinueScroll();
        this.mUpdateCurrentTime = new UpdateCurrentTime();
        this.mBold = Typeface.DEFAULT_BOLD;
        this.mLoadedFirstJulianDay = -1;
        this.mEventsAlpha = 255;
        this.mTZUpdater = new Runnable() {
            @Override
            public void run() {
                String tz = Utils.getTimeZone(DayView.this.mContext, this);
                DayView.this.mBaseDate.timezone = tz;
                DayView.this.mBaseDate.normalize(true);
                DayView.this.mCurrentTime.switchTimezone(tz);
                DayView.this.invalidate();
            }
        };
        this.mSetClick = new Runnable() {
            @Override
            public void run() {
                DayView.this.mClickedEvent = DayView.this.mSavedClickedEvent;
                DayView.this.mSavedClickedEvent = null;
                DayView.this.invalidate();
            }
        };
        this.mClearClick = new Runnable() {
            @Override
            public void run() {
                if (DayView.this.mClickedEvent != null) {
                    DayView.this.mController.sendEventRelatedEvent(this, 2L, DayView.this.mClickedEvent.id, DayView.this.mClickedEvent.startMillis, DayView.this.mClickedEvent.endMillis, DayView.this.getWidth() / 2, DayView.this.mClickedYLocation, DayView.this.getSelectedTimeInMillis());
                }
                DayView.this.mClickedEvent = null;
                DayView.this.invalidate();
            }
        };
        this.mTodayAnimatorListener = new TodayAnimatorListener();
        this.mAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                DayView.this.mScrolling = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                DayView.this.mScrolling = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                DayView.this.mScrolling = false;
                DayView.this.resetSelectedHour();
                DayView.this.invalidate();
            }
        };
        this.mEvents = new ArrayList<>();
        this.mAllDayEvents = new ArrayList<>();
        this.mLayouts = null;
        this.mAllDayLayouts = null;
        this.mRect = new Rect();
        this.mDestRect = new Rect();
        this.mSelectionRect = new Rect();
        this.mExpandAllDayRect = new Rect();
        this.mPaint = new Paint();
        this.mEventTextPaint = new Paint();
        this.mSelectionPaint = new Paint();
        this.mDismissPopup = new DismissPopup();
        this.mRemeasure = true;
        this.mAnimationDistance = 0.0f;
        this.mGridAreaHeight = -1;
        this.mStartingSpanY = 0.0f;
        this.mGestureCenterHour = 0.0f;
        this.mRecalCenterHour = false;
        this.mHandleActionUp = true;
        this.mAnimateDayHeight = 0;
        this.mAnimateDayEventHeight = (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;
        this.mMaxUnexpandedAlldayEventCount = 4;
        this.mNumDays = 7;
        this.mNumHours = 10;
        this.mFirstHour = -1;
        this.mSelectedEvents = new ArrayList<>();
        this.mPrevBox = new Rect();
        this.mContextMenuHandler = new ContextMenuHandler();
        this.mTouchMode = 0;
        this.mSelectionMode = 0;
        this.mScrolling = false;
        this.mAnimateToday = false;
        this.mAnimateTodayAlpha = 0;
        this.mCancellingAnimations = false;
        this.mTouchStartedInAlldayArea = false;
        this.mAccessibilityMgr = null;
        this.mIsAccessibilityEnabled = false;
        this.mTouchExplorationEnabled = false;
        this.mCancelCallback = new Runnable() {
            @Override
            public void run() {
                DayView.this.clearCachedEvents();
            }
        };
        this.drawTextSanitizerFilter = Pattern.compile("[\t\n],");
        this.mContext = context;
        initAccessibilityVariables();
        this.mResources = context.getResources();
        this.mCreateNewEventString = this.mResources.getString(R.string.event_create);
        this.mNewEventHintString = this.mResources.getString(R.string.day_view_new_event_hint);
        this.mNumDays = numDays;
        DATE_HEADER_FONT_SIZE = (int) this.mResources.getDimension(R.dimen.date_header_text_size);
        DAY_HEADER_FONT_SIZE = (int) this.mResources.getDimension(R.dimen.day_label_text_size);
        ONE_DAY_HEADER_HEIGHT = (int) this.mResources.getDimension(R.dimen.one_day_header_height);
        DAY_HEADER_BOTTOM_MARGIN = (int) this.mResources.getDimension(R.dimen.day_header_bottom_margin);
        EXPAND_ALL_DAY_BOTTOM_MARGIN = (int) this.mResources.getDimension(R.dimen.all_day_bottom_margin);
        HOURS_TEXT_SIZE = (int) this.mResources.getDimension(R.dimen.hours_text_size);
        AMPM_TEXT_SIZE = (int) this.mResources.getDimension(R.dimen.ampm_text_size);
        MIN_HOURS_WIDTH = (int) this.mResources.getDimension(R.dimen.min_hours_width);
        HOURS_LEFT_MARGIN = (int) this.mResources.getDimension(R.dimen.hours_left_margin);
        HOURS_RIGHT_MARGIN = (int) this.mResources.getDimension(R.dimen.hours_right_margin);
        MULTI_DAY_HEADER_HEIGHT = (int) this.mResources.getDimension(R.dimen.day_header_height);
        if (this.mNumDays == 1) {
            eventTextSizeId = R.dimen.day_view_event_text_size;
        } else {
            eventTextSizeId = R.dimen.week_view_event_text_size;
        }
        EVENT_TEXT_FONT_SIZE = (int) this.mResources.getDimension(eventTextSizeId);
        NEW_EVENT_HINT_FONT_SIZE = (int) this.mResources.getDimension(R.dimen.new_event_hint_text_size);
        MIN_EVENT_HEIGHT = this.mResources.getDimension(R.dimen.event_min_height);
        MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT = MIN_EVENT_HEIGHT;
        EVENT_TEXT_TOP_MARGIN = (int) this.mResources.getDimension(R.dimen.event_text_vertical_margin);
        EVENT_TEXT_BOTTOM_MARGIN = EVENT_TEXT_TOP_MARGIN;
        EVENT_ALL_DAY_TEXT_TOP_MARGIN = EVENT_TEXT_TOP_MARGIN;
        EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN = EVENT_TEXT_TOP_MARGIN;
        EVENT_TEXT_LEFT_MARGIN = (int) this.mResources.getDimension(R.dimen.event_text_horizontal_margin);
        EVENT_TEXT_RIGHT_MARGIN = EVENT_TEXT_LEFT_MARGIN;
        EVENT_ALL_DAY_TEXT_LEFT_MARGIN = EVENT_TEXT_LEFT_MARGIN;
        EVENT_ALL_DAY_TEXT_RIGHT_MARGIN = EVENT_TEXT_LEFT_MARGIN;
        if (mScale == 0.0f) {
            mScale = this.mResources.getDisplayMetrics().density;
            if (mScale != 1.0f) {
                SINGLE_ALLDAY_HEIGHT = (int) (SINGLE_ALLDAY_HEIGHT * mScale);
                ALLDAY_TOP_MARGIN = (int) (ALLDAY_TOP_MARGIN * mScale);
                MAX_HEIGHT_OF_ONE_ALLDAY_EVENT = (int) (MAX_HEIGHT_OF_ONE_ALLDAY_EVENT * mScale);
                NORMAL_FONT_SIZE *= mScale;
                GRID_LINE_LEFT_MARGIN *= mScale;
                HOURS_TOP_MARGIN = (int) (HOURS_TOP_MARGIN * mScale);
                MIN_CELL_WIDTH_FOR_TEXT = (int) (MIN_CELL_WIDTH_FOR_TEXT * mScale);
                MAX_UNEXPANDED_ALLDAY_HEIGHT = (int) (MAX_UNEXPANDED_ALLDAY_HEIGHT * mScale);
                this.mAnimateDayEventHeight = (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;
                CURRENT_TIME_LINE_SIDE_BUFFER = (int) (CURRENT_TIME_LINE_SIDE_BUFFER * mScale);
                CURRENT_TIME_LINE_TOP_OFFSET = (int) (CURRENT_TIME_LINE_TOP_OFFSET * mScale);
                MIN_Y_SPAN = (int) (MIN_Y_SPAN * mScale);
                MAX_CELL_HEIGHT = (int) (MAX_CELL_HEIGHT * mScale);
                DEFAULT_CELL_HEIGHT = (int) (DEFAULT_CELL_HEIGHT * mScale);
                DAY_HEADER_HEIGHT = (int) (DAY_HEADER_HEIGHT * mScale);
                DAY_HEADER_RIGHT_MARGIN = (int) (DAY_HEADER_RIGHT_MARGIN * mScale);
                DAY_HEADER_ONE_DAY_LEFT_MARGIN = (int) (DAY_HEADER_ONE_DAY_LEFT_MARGIN * mScale);
                DAY_HEADER_ONE_DAY_RIGHT_MARGIN = (int) (DAY_HEADER_ONE_DAY_RIGHT_MARGIN * mScale);
                DAY_HEADER_ONE_DAY_BOTTOM_MARGIN = (int) (DAY_HEADER_ONE_DAY_BOTTOM_MARGIN * mScale);
                CALENDAR_COLOR_SQUARE_SIZE = (int) (CALENDAR_COLOR_SQUARE_SIZE * mScale);
                EVENT_RECT_TOP_MARGIN = (int) (EVENT_RECT_TOP_MARGIN * mScale);
                EVENT_RECT_BOTTOM_MARGIN = (int) (EVENT_RECT_BOTTOM_MARGIN * mScale);
                ALL_DAY_EVENT_RECT_BOTTOM_MARGIN = (int) (ALL_DAY_EVENT_RECT_BOTTOM_MARGIN * mScale);
                EVENT_RECT_LEFT_MARGIN = (int) (EVENT_RECT_LEFT_MARGIN * mScale);
                EVENT_RECT_RIGHT_MARGIN = (int) (EVENT_RECT_RIGHT_MARGIN * mScale);
                EVENT_RECT_STROKE_WIDTH = (int) (EVENT_RECT_STROKE_WIDTH * mScale);
                EVENT_SQUARE_WIDTH = (int) (EVENT_SQUARE_WIDTH * mScale);
                EVENT_LINE_PADDING = (int) (EVENT_LINE_PADDING * mScale);
                NEW_EVENT_MARGIN = (int) (NEW_EVENT_MARGIN * mScale);
                NEW_EVENT_WIDTH = (int) (NEW_EVENT_WIDTH * mScale);
                NEW_EVENT_MAX_LENGTH = (int) (NEW_EVENT_MAX_LENGTH * mScale);
            }
        }
        HOURS_MARGIN = HOURS_LEFT_MARGIN + HOURS_RIGHT_MARGIN;
        DAY_HEADER_HEIGHT = this.mNumDays == 1 ? ONE_DAY_HEADER_HEIGHT : MULTI_DAY_HEADER_HEIGHT;
        this.mCurrentTimeLine = this.mResources.getDrawable(R.drawable.timeline_indicator_holo_light);
        this.mCurrentTimeAnimateLine = this.mResources.getDrawable(R.drawable.timeline_indicator_activated_holo_light);
        this.mTodayHeaderDrawable = this.mResources.getDrawable(R.drawable.today_blue_week_holo_light);
        this.mExpandAlldayDrawable = this.mResources.getDrawable(R.drawable.ic_expand_holo_light);
        this.mCollapseAlldayDrawable = this.mResources.getDrawable(R.drawable.ic_collapse_holo_light);
        mNewEventHintColor = this.mResources.getColor(R.color.new_event_hint_text_color);
        this.mAcceptedOrTentativeEventBoxDrawable = this.mResources.getDrawable(R.drawable.panel_month_event_holo_light);
        this.mEventLoader = eventLoader;
        this.mEventGeometry = new EventGeometry();
        this.mEventGeometry.setMinEventHeight(MIN_EVENT_HEIGHT);
        this.mEventGeometry.setHourGap(1.0f);
        this.mEventGeometry.setCellMargin(1);
        this.mLongPressItems = new CharSequence[]{this.mResources.getString(R.string.new_event_dialog_option)};
        this.mLongPressTitle = this.mResources.getString(R.string.new_event_dialog_label);
        this.mDeleteEventHelper = new DeleteEventHelper(context, null, false);
        this.mLastPopupEventID = -1L;
        this.mController = controller;
        this.mViewSwitcher = viewSwitcher;
        this.mGestureDetector = new GestureDetector(context, new CalendarGestureListener());
        this.mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);
        if (mCellHeight == 0) {
            mCellHeight = Utils.getSharedPreference(this.mContext, "preferences_default_cell_height", DEFAULT_CELL_HEIGHT);
        }
        this.mScroller = new OverScroller(context);
        this.mHScrollInterpolator = new ScrollInterpolator();
        this.mEdgeEffectTop = new EdgeEffect(context);
        this.mEdgeEffectBottom = new EdgeEffect(context);
        ViewConfiguration vc = ViewConfiguration.get(context);
        mScaledPagingTouchSlop = vc.getScaledPagingTouchSlop();
        mOnDownDelay = ViewConfiguration.getTapTimeout();
        this.OVERFLING_DISTANCE = vc.getScaledOverflingDistance();
        init(context);
    }

    @Override
    protected void onAttachedToWindow() {
        if (this.mHandler == null) {
            this.mHandler = getHandler();
            this.mHandler.post(this.mUpdateCurrentTime);
        }
    }

    private void init(Context context) {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
        setOnCreateContextMenuListener(this);
        this.mFirstDayOfWeek = Utils.getFirstDayOfWeek(context);
        this.mCurrentTime = new Time(Utils.getTimeZone(context, this.mTZUpdater));
        long currentTime = System.currentTimeMillis();
        this.mCurrentTime.set(currentTime);
        this.mTodayJulianDay = Time.getJulianDay(currentTime, this.mCurrentTime.gmtoff);
        mWeek_saturdayColor = this.mResources.getColor(R.color.week_saturday);
        mWeek_sundayColor = this.mResources.getColor(R.color.week_sunday);
        mCalendarDateBannerTextColor = this.mResources.getColor(R.color.calendar_date_banner_text_color);
        mFutureBgColorRes = this.mResources.getColor(R.color.calendar_future_bg_color);
        mBgColor = this.mResources.getColor(R.color.calendar_hour_background);
        mCalendarAmPmLabel = this.mResources.getColor(R.color.calendar_ampm_label);
        mCalendarGridAreaSelected = this.mResources.getColor(R.color.calendar_grid_area_selected);
        mCalendarGridLineInnerHorizontalColor = this.mResources.getColor(R.color.calendar_grid_line_inner_horizontal_color);
        mCalendarGridLineInnerVerticalColor = this.mResources.getColor(R.color.calendar_grid_line_inner_vertical_color);
        mCalendarHourLabelColor = this.mResources.getColor(R.color.calendar_hour_label);
        mPressedColor = this.mResources.getColor(R.color.pressed);
        mClickedColor = this.mResources.getColor(R.color.day_event_clicked_background_color);
        mEventTextColor = this.mResources.getColor(R.color.calendar_event_text_color);
        mMoreEventsTextColor = this.mResources.getColor(R.color.month_event_other_color);
        this.mEventTextPaint.setTextSize(EVENT_TEXT_FONT_SIZE);
        this.mEventTextPaint.setTextAlign(Paint.Align.LEFT);
        this.mEventTextPaint.setAntiAlias(true);
        int gridLineColor = this.mResources.getColor(R.color.calendar_grid_line_highlight_color);
        Paint p = this.mSelectionPaint;
        p.setColor(gridLineColor);
        p.setStyle(Paint.Style.FILL);
        p.setAntiAlias(false);
        Paint p2 = this.mPaint;
        p2.setAntiAlias(true);
        this.mDayStrs = new String[14];
        this.mDayStrs2Letter = new String[14];
        for (int i = 1; i <= 7; i++) {
            int index = i - 1;
            this.mDayStrs[index] = DateUtils.getDayOfWeekString(i, 20).toUpperCase();
            this.mDayStrs[index + 7] = this.mDayStrs[index];
            this.mDayStrs2Letter[index] = DateUtils.getDayOfWeekString(i, 30).toUpperCase();
            if (this.mDayStrs2Letter[index].equals(this.mDayStrs[index])) {
                this.mDayStrs2Letter[index] = DateUtils.getDayOfWeekString(i, 50);
            }
            this.mDayStrs2Letter[index + 7] = this.mDayStrs2Letter[index];
        }
        p2.setTextSize(DATE_HEADER_FONT_SIZE);
        p2.setTypeface(this.mBold);
        String[] dateStrs = {" 28", " 30"};
        this.mDateStrWidth = computeMaxStringWidth(0, dateStrs, p2);
        p2.setTextSize(DAY_HEADER_FONT_SIZE);
        this.mDateStrWidth += computeMaxStringWidth(0, this.mDayStrs, p2);
        p2.setTextSize(HOURS_TEXT_SIZE);
        p2.setTypeface(null);
        handleOnResume();
        this.mAmString = DateUtils.getAMPMString(0).toUpperCase();
        this.mPmString = DateUtils.getAMPMString(1).toUpperCase();
        String[] ampm = {this.mAmString, this.mPmString};
        p2.setTextSize(AMPM_TEXT_SIZE);
        this.mHoursWidth = Math.max(HOURS_MARGIN, computeMaxStringWidth(this.mHoursWidth, ampm, p2) + HOURS_RIGHT_MARGIN);
        this.mHoursWidth = Math.max(MIN_HOURS_WIDTH, this.mHoursWidth);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mPopupView = inflater.inflate(R.layout.bubble_event, (ViewGroup) null);
        this.mPopupView.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
        this.mPopup = new PopupWindow(context);
        this.mPopup.setContentView(this.mPopupView);
        Resources.Theme dialogTheme = getResources().newTheme();
        dialogTheme.applyStyle(android.R.style.Theme.Dialog, true);
        TypedArray ta = dialogTheme.obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
        this.mPopup.setBackgroundDrawable(ta.getDrawable(0));
        ta.recycle();
        this.mPopupView.setOnClickListener(this);
        setOnLongClickListener(this);
        this.mBaseDate = new Time(Utils.getTimeZone(context, this.mTZUpdater));
        long millis = System.currentTimeMillis();
        this.mBaseDate.set(millis);
        this.mEarliestStartHour = new int[this.mNumDays];
        this.mHasAllDayEvent = new boolean[this.mNumDays];
        int maxGridLines = this.mNumDays + 1 + 25;
        this.mLines = new float[maxGridLines * 4];
    }

    @Override
    public void onClick(View v) {
        if (v == this.mPopupView) {
            switchViews(true);
        }
    }

    public void handleOnResume() {
        initAccessibilityVariables();
        if (Utils.getSharedPreference(this.mContext, "preferences_tardis_1", false)) {
            mFutureBgColor = 0;
        } else {
            mFutureBgColor = mFutureBgColorRes;
        }
        this.mIs24HourFormat = DateFormat.is24HourFormat(this.mContext);
        this.mHourStrs = this.mIs24HourFormat ? CalendarData.s24Hours : CalendarData.s12HoursNoAmPm;
        this.mFirstDayOfWeek = Utils.getFirstDayOfWeek(this.mContext);
        this.mLastSelectionDayForAccessibility = 0;
        this.mLastSelectionHourForAccessibility = 0;
        this.mLastSelectedEventForAccessibility = null;
        this.mSelectionMode = 0;
    }

    private void initAccessibilityVariables() {
        this.mAccessibilityMgr = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        this.mIsAccessibilityEnabled = this.mAccessibilityMgr != null && this.mAccessibilityMgr.isEnabled();
        this.mTouchExplorationEnabled = isTouchExplorationEnabled();
    }

    long getSelectedTimeInMillis() {
        Time time = new Time(this.mBaseDate);
        time.setJulianDay(this.mSelectionDay);
        time.hour = this.mSelectionHour;
        return time.normalize(true);
    }

    Time getSelectedTime() {
        Time time = new Time(this.mBaseDate);
        time.setJulianDay(this.mSelectionDay);
        time.hour = this.mSelectionHour;
        time.normalize(true);
        return time;
    }

    Time getSelectedTimeForAccessibility() {
        Time time = new Time(this.mBaseDate);
        time.setJulianDay(this.mSelectionDayForAccessibility);
        time.hour = this.mSelectionHourForAccessibility;
        time.normalize(true);
        return time;
    }

    int getFirstVisibleHour() {
        return this.mFirstHour;
    }

    void setFirstVisibleHour(int firstHour) {
        this.mFirstHour = firstHour;
        this.mFirstHourOffset = 0;
    }

    public void setSelected(Time time, boolean ignoreTime, boolean animateToday) {
        this.mBaseDate.set(time);
        setSelectedHour(this.mBaseDate.hour);
        setSelectedEvent(null);
        this.mPrevSelectedEvent = null;
        long millis = this.mBaseDate.toMillis(false);
        setSelectedDay(Time.getJulianDay(millis, this.mBaseDate.gmtoff));
        this.mSelectedEvents.clear();
        this.mComputeSelectedEvents = true;
        int gotoY = Integer.MIN_VALUE;
        if (!ignoreTime && this.mGridAreaHeight != -1) {
            int lastHour = 0;
            if (this.mBaseDate.hour < this.mFirstHour) {
                gotoY = this.mBaseDate.hour * (mCellHeight + 1);
            } else {
                lastHour = ((this.mGridAreaHeight - this.mFirstHourOffset) / (mCellHeight + 1)) + this.mFirstHour;
                if (this.mBaseDate.hour >= lastHour) {
                    gotoY = (int) ((((this.mBaseDate.hour + 1) + (this.mBaseDate.minute / 60.0f)) * (mCellHeight + 1)) - this.mGridAreaHeight);
                }
            }
            if (DEBUG) {
                Log.e(TAG, "Go " + gotoY + " 1st " + this.mFirstHour + ":" + this.mFirstHourOffset + "CH " + (mCellHeight + 1) + " lh " + lastHour + " gh " + this.mGridAreaHeight + " ymax " + this.mMaxViewStartY);
            }
            if (gotoY > this.mMaxViewStartY) {
                gotoY = this.mMaxViewStartY;
            } else if (gotoY < 0 && gotoY != Integer.MIN_VALUE) {
                gotoY = 0;
            }
        }
        recalc();
        this.mRemeasure = true;
        invalidate();
        boolean delayAnimateToday = false;
        if (gotoY != Integer.MIN_VALUE) {
            ValueAnimator scrollAnim = ObjectAnimator.ofInt(this, "viewStartY", this.mViewStartY, gotoY);
            scrollAnim.setDuration(200L);
            scrollAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            scrollAnim.addListener(this.mAnimatorListener);
            scrollAnim.start();
            delayAnimateToday = true;
        }
        if (animateToday) {
            synchronized (this.mTodayAnimatorListener) {
                if (this.mTodayAnimator != null) {
                    this.mTodayAnimator.removeAllListeners();
                    this.mTodayAnimator.cancel();
                }
                this.mTodayAnimator = ObjectAnimator.ofInt(this, "animateTodayAlpha", this.mAnimateTodayAlpha, 255);
                this.mAnimateToday = true;
                this.mTodayAnimatorListener.setFadingIn(true);
                this.mTodayAnimatorListener.setAnimator(this.mTodayAnimator);
                this.mTodayAnimator.addListener(this.mTodayAnimatorListener);
                this.mTodayAnimator.setDuration(150L);
                if (delayAnimateToday) {
                    this.mTodayAnimator.setStartDelay(200L);
                }
                this.mTodayAnimator.start();
            }
        }
        sendAccessibilityEventAsNeeded(false);
    }

    public void setViewStartY(int viewStartY) {
        if (viewStartY > this.mMaxViewStartY) {
            viewStartY = this.mMaxViewStartY;
        }
        this.mViewStartY = viewStartY;
        computeFirstHour();
        invalidate();
    }

    public void setAnimateTodayAlpha(int todayAlpha) {
        this.mAnimateTodayAlpha = todayAlpha;
        invalidate();
    }

    public void updateTitle() {
        Time start = new Time(this.mBaseDate);
        start.normalize(true);
        Time end = new Time(start);
        end.monthDay += this.mNumDays - 1;
        end.minute++;
        end.normalize(true);
        long formatFlags = 20;
        if (this.mNumDays != 1) {
            formatFlags = 20 | 32;
            if (start.month != end.month) {
                formatFlags |= 65536;
            }
        }
        this.mController.sendEvent(this, 1024L, start, end, null, -1L, 0, formatFlags, null, null);
    }

    public int compareToVisibleTimeRange(Time time) {
        int savedHour = this.mBaseDate.hour;
        int savedMinute = this.mBaseDate.minute;
        int savedSec = this.mBaseDate.second;
        this.mBaseDate.hour = 0;
        this.mBaseDate.minute = 0;
        this.mBaseDate.second = 0;
        if (DEBUG) {
            Log.d(TAG, "Begin " + this.mBaseDate.toString());
            Log.d(TAG, "Diff  " + time.toString());
        }
        int diff = Time.compare(time, this.mBaseDate);
        if (diff > 0) {
            this.mBaseDate.monthDay += this.mNumDays;
            this.mBaseDate.normalize(true);
            diff = Time.compare(time, this.mBaseDate);
            if (DEBUG) {
                Log.d(TAG, "End   " + this.mBaseDate.toString());
            }
            this.mBaseDate.monthDay -= this.mNumDays;
            this.mBaseDate.normalize(true);
            if (diff < 0) {
                diff = 0;
            } else if (diff == 0) {
                diff = 1;
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Diff: " + diff);
        }
        this.mBaseDate.hour = savedHour;
        this.mBaseDate.minute = savedMinute;
        this.mBaseDate.second = savedSec;
        return diff;
    }

    private void recalc() {
        if (this.mNumDays == 7) {
            adjustToBeginningOfWeek(this.mBaseDate);
        }
        long start = this.mBaseDate.toMillis(false);
        this.mFirstJulianDay = Time.getJulianDay(start, this.mBaseDate.gmtoff);
        this.mLastJulianDay = (this.mFirstJulianDay + this.mNumDays) - 1;
        this.mMonthLength = this.mBaseDate.getActualMaximum(4);
        this.mFirstVisibleDate = this.mBaseDate.monthDay;
        this.mFirstVisibleDayOfWeek = this.mBaseDate.weekDay;
    }

    private void adjustToBeginningOfWeek(Time time) {
        int dayOfWeek = time.weekDay;
        int diff = dayOfWeek - this.mFirstDayOfWeek;
        if (diff != 0) {
            if (diff < 0) {
                diff += 7;
            }
            time.monthDay -= diff;
            time.normalize(true);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        this.mViewWidth = width;
        this.mViewHeight = height;
        this.mEdgeEffectTop.setSize(this.mViewWidth, this.mViewHeight);
        this.mEdgeEffectBottom.setSize(this.mViewWidth, this.mViewHeight);
        int gridAreaWidth = width - this.mHoursWidth;
        this.mCellWidth = (gridAreaWidth - (this.mNumDays * 1)) / this.mNumDays;
        mHorizontalSnapBackThreshold = width / 7;
        Paint p = new Paint();
        p.setTextSize(HOURS_TEXT_SIZE);
        this.mHoursTextHeight = (int) Math.abs(p.ascent());
        remeasure(width, height);
    }

    private void remeasure(int width, int height) {
        MAX_UNEXPANDED_ALLDAY_HEIGHT = (int) (MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT * 4.0f);
        MAX_UNEXPANDED_ALLDAY_HEIGHT = Math.min(MAX_UNEXPANDED_ALLDAY_HEIGHT, height / 6);
        MAX_UNEXPANDED_ALLDAY_HEIGHT = Math.max(MAX_UNEXPANDED_ALLDAY_HEIGHT, ((int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT) * 2);
        this.mMaxUnexpandedAlldayEventCount = (int) (MAX_UNEXPANDED_ALLDAY_HEIGHT / MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT);
        for (int day = 0; day < this.mNumDays; day++) {
            this.mEarliestStartHour[day] = 25;
            this.mHasAllDayEvent[day] = false;
        }
        int maxAllDayEvents = this.mMaxAlldayEvents;
        mMinCellHeight = Math.max((height - DAY_HEADER_HEIGHT) / 24, (int) MIN_EVENT_HEIGHT);
        if (mCellHeight < mMinCellHeight) {
            mCellHeight = mMinCellHeight;
        }
        this.mFirstCell = DAY_HEADER_HEIGHT;
        int allDayHeight = 0;
        if (maxAllDayEvents > 0) {
            int maxAllAllDayHeight = (height - DAY_HEADER_HEIGHT) - MIN_HOURS_HEIGHT;
            if (maxAllDayEvents == 1) {
                allDayHeight = SINGLE_ALLDAY_HEIGHT;
            } else if (maxAllDayEvents <= this.mMaxUnexpandedAlldayEventCount) {
                allDayHeight = maxAllDayEvents * MAX_HEIGHT_OF_ONE_ALLDAY_EVENT;
                if (allDayHeight > MAX_UNEXPANDED_ALLDAY_HEIGHT) {
                    allDayHeight = MAX_UNEXPANDED_ALLDAY_HEIGHT;
                }
            } else if (this.mAnimateDayHeight != 0) {
                allDayHeight = Math.max(this.mAnimateDayHeight, MAX_UNEXPANDED_ALLDAY_HEIGHT);
            } else {
                allDayHeight = (int) (maxAllDayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT);
                if (!mShowAllAllDayEvents && allDayHeight > MAX_UNEXPANDED_ALLDAY_HEIGHT) {
                    allDayHeight = (int) (this.mMaxUnexpandedAlldayEventCount * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT);
                } else if (allDayHeight > maxAllAllDayHeight) {
                    allDayHeight = maxAllAllDayHeight;
                }
            }
            this.mFirstCell = DAY_HEADER_HEIGHT + allDayHeight + ALLDAY_TOP_MARGIN;
        } else {
            this.mSelectionAllday = false;
        }
        this.mAlldayHeight = allDayHeight;
        this.mGridAreaHeight = height - this.mFirstCell;
        int allDayIconWidth = this.mExpandAlldayDrawable.getIntrinsicWidth();
        this.mExpandAllDayRect.left = Math.max((this.mHoursWidth - allDayIconWidth) / 2, EVENT_ALL_DAY_TEXT_LEFT_MARGIN);
        this.mExpandAllDayRect.right = Math.min(this.mExpandAllDayRect.left + allDayIconWidth, this.mHoursWidth - EVENT_ALL_DAY_TEXT_RIGHT_MARGIN);
        this.mExpandAllDayRect.bottom = this.mFirstCell - EXPAND_ALL_DAY_BOTTOM_MARGIN;
        this.mExpandAllDayRect.top = this.mExpandAllDayRect.bottom - this.mExpandAlldayDrawable.getIntrinsicHeight();
        this.mNumHours = this.mGridAreaHeight / (mCellHeight + 1);
        this.mEventGeometry.setHourHeight(mCellHeight);
        long minimumDurationMillis = (long) ((MIN_EVENT_HEIGHT * 60000.0f) / (mCellHeight / 60.0f));
        Event.computePositions(this.mEvents, minimumDurationMillis);
        this.mMaxViewStartY = (((mCellHeight + 1) * 24) + 1) - this.mGridAreaHeight;
        if (DEBUG) {
            Log.e(TAG, "mViewStartY: " + this.mViewStartY);
            Log.e(TAG, "mMaxViewStartY: " + this.mMaxViewStartY);
        }
        if (this.mViewStartY > this.mMaxViewStartY) {
            this.mViewStartY = this.mMaxViewStartY;
            computeFirstHour();
        }
        if (this.mFirstHour == -1) {
            initFirstHour();
            this.mFirstHourOffset = 0;
        }
        if (this.mFirstHourOffset >= mCellHeight + 1) {
            this.mFirstHourOffset = (mCellHeight + 1) - 1;
        }
        this.mViewStartY = (this.mFirstHour * (mCellHeight + 1)) - this.mFirstHourOffset;
        int eventAreaWidth = this.mNumDays * (this.mCellWidth + 1);
        if (this.mSelectedEvent != null && this.mLastPopupEventID != this.mSelectedEvent.id) {
            this.mPopup.dismiss();
        }
        this.mPopup.setWidth(eventAreaWidth - 20);
        this.mPopup.setHeight(-2);
    }

    private void initView(DayView view) {
        view.setSelectedHour(this.mSelectionHour);
        view.mSelectedEvents.clear();
        view.mComputeSelectedEvents = true;
        view.mFirstHour = this.mFirstHour;
        view.mFirstHourOffset = this.mFirstHourOffset;
        view.remeasure(getWidth(), getHeight());
        view.initAllDayHeights();
        view.setSelectedEvent(null);
        view.mPrevSelectedEvent = null;
        view.mFirstDayOfWeek = this.mFirstDayOfWeek;
        if (view.mEvents.size() > 0) {
            view.mSelectionAllday = this.mSelectionAllday;
        } else {
            view.mSelectionAllday = false;
        }
        view.recalc();
    }

    private void switchViews(boolean trackBallSelection) {
        Event selectedEvent = this.mSelectedEvent;
        this.mPopup.dismiss();
        this.mLastPopupEventID = -1L;
        if (this.mNumDays > 1) {
            if (trackBallSelection) {
                if (selectedEvent == null) {
                    long startMillis = getSelectedTimeInMillis();
                    long endMillis = startMillis + 3600000;
                    long extraLong = 0;
                    if (this.mSelectionAllday) {
                        extraLong = 16;
                    }
                    this.mController.sendEventRelatedEventWithExtra(this, 1L, -1L, startMillis, endMillis, -1, -1, extraLong, -1L);
                    return;
                }
                if (this.mIsAccessibilityEnabled) {
                    this.mAccessibilityMgr.interrupt();
                }
                this.mController.sendEventRelatedEvent(this, 2L, selectedEvent.id, selectedEvent.startMillis, selectedEvent.endMillis, 0, 0, getSelectedTimeInMillis());
                return;
            }
            if (this.mSelectedEvents.size() == 1) {
                if (this.mIsAccessibilityEnabled) {
                    this.mAccessibilityMgr.interrupt();
                }
                this.mController.sendEventRelatedEvent(this, 2L, selectedEvent.id, selectedEvent.startMillis, selectedEvent.endMillis, 0, 0, getSelectedTimeInMillis());
                return;
            }
            return;
        }
        if (selectedEvent == null) {
            long startMillis2 = getSelectedTimeInMillis();
            long endMillis2 = startMillis2 + 3600000;
            long extraLong2 = 0;
            if (this.mSelectionAllday) {
                extraLong2 = 16;
            }
            this.mController.sendEventRelatedEventWithExtra(this, 1L, -1L, startMillis2, endMillis2, -1, -1, extraLong2, -1L);
            return;
        }
        if (this.mIsAccessibilityEnabled) {
            this.mAccessibilityMgr.interrupt();
        }
        this.mController.sendEventRelatedEvent(this, 2L, selectedEvent.id, selectedEvent.startMillis, selectedEvent.endMillis, 0, 0, getSelectedTimeInMillis());
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        this.mScrolling = false;
        long duration = event.getEventTime() - event.getDownTime();
        switch (keyCode) {
            case 23:
                if (this.mSelectionMode != 0) {
                    if (this.mSelectionMode == 1) {
                        this.mSelectionMode = 2;
                        invalidate();
                    } else if (duration < ViewConfiguration.getLongPressTimeout()) {
                        switchViews(true);
                    } else {
                        this.mSelectionMode = 3;
                        invalidate();
                        performLongClick();
                    }
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean redraw;
        if (this.mSelectionMode == 0) {
            if (keyCode == 66 || keyCode == 22 || keyCode == 21 || keyCode == 19 || keyCode == 20) {
                this.mSelectionMode = 2;
                invalidate();
                return true;
            }
            if (keyCode == 23) {
                this.mSelectionMode = 1;
                invalidate();
                return true;
            }
        }
        this.mSelectionMode = 2;
        this.mScrolling = false;
        int selectionDay = this.mSelectionDay;
        switch (keyCode) {
            case 4:
                if (event.getRepeatCount() == 0) {
                    event.startTracking();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
            case 19:
                if (this.mSelectedEvent != null) {
                    setSelectedEvent(this.mSelectedEvent.nextUp);
                }
                if (this.mSelectedEvent == null) {
                    this.mLastPopupEventID = -1L;
                    if (!this.mSelectionAllday) {
                        setSelectedHour(this.mSelectionHour - 1);
                        adjustHourSelection();
                        this.mSelectedEvents.clear();
                        this.mComputeSelectedEvents = true;
                    }
                }
                redraw = true;
                break;
            case 20:
                if (this.mSelectedEvent != null) {
                    setSelectedEvent(this.mSelectedEvent.nextDown);
                }
                if (this.mSelectedEvent == null) {
                    this.mLastPopupEventID = -1L;
                    if (this.mSelectionAllday) {
                        this.mSelectionAllday = false;
                    } else {
                        setSelectedHour(this.mSelectionHour + 1);
                        adjustHourSelection();
                        this.mSelectedEvents.clear();
                        this.mComputeSelectedEvents = true;
                    }
                }
                redraw = true;
                break;
            case 21:
                if (this.mSelectedEvent != null) {
                    setSelectedEvent(this.mSelectedEvent.nextLeft);
                }
                if (this.mSelectedEvent == null) {
                    this.mLastPopupEventID = -1L;
                    selectionDay--;
                }
                redraw = true;
                break;
            case 22:
                if (this.mSelectedEvent != null) {
                    setSelectedEvent(this.mSelectedEvent.nextRight);
                }
                if (this.mSelectedEvent == null) {
                    this.mLastPopupEventID = -1L;
                    selectionDay++;
                }
                redraw = true;
                break;
            case 66:
                switchViews(true);
                return true;
            case 67:
                Event selectedEvent = this.mSelectedEvent;
                if (selectedEvent == null) {
                    return false;
                }
                this.mPopup.dismiss();
                this.mLastPopupEventID = -1L;
                long begin = selectedEvent.startMillis;
                long end = selectedEvent.endMillis;
                long id = selectedEvent.id;
                this.mDeleteEventHelper.delete(begin, end, id, -1);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
        if (selectionDay < this.mFirstJulianDay || selectionDay > this.mLastJulianDay) {
            DayView view = (DayView) this.mViewSwitcher.getNextView();
            Time date = view.mBaseDate;
            date.set(this.mBaseDate);
            if (selectionDay < this.mFirstJulianDay) {
                date.monthDay -= this.mNumDays;
            } else {
                date.monthDay += this.mNumDays;
            }
            date.normalize(true);
            view.setSelectedDay(selectionDay);
            initView(view);
            Time end2 = new Time(date);
            end2.monthDay += this.mNumDays - 1;
            this.mController.sendEvent(this, 32L, date, end2, -1L, 0);
            return true;
        }
        if (this.mSelectionDay != selectionDay) {
            Time date2 = new Time(this.mBaseDate);
            date2.setJulianDay(selectionDay);
            date2.hour = this.mSelectionHour;
            this.mController.sendEvent(this, 32L, date2, date2, -1L, 0);
        }
        setSelectedDay(selectionDay);
        this.mSelectedEvents.clear();
        this.mComputeSelectedEvents = true;
        this.mUpdateToast = true;
        if (redraw) {
            invalidate();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (DEBUG) {
            int action = event.getAction();
            switch (action) {
                case 7:
                    Log.e(TAG, "ACTION_HOVER_MOVE");
                    break;
                case 8:
                default:
                    Log.e(TAG, "Unknown hover event action. " + event);
                    break;
                case 9:
                    Log.e(TAG, "ACTION_HOVER_ENTER");
                    break;
                case 10:
                    Log.e(TAG, "ACTION_HOVER_EXIT");
                    break;
            }
        }
        if (!this.mTouchExplorationEnabled) {
            return super.onHoverEvent(event);
        }
        if (event.getAction() == 10) {
            return true;
        }
        setSelectionFromPosition((int) event.getX(), (int) event.getY(), true);
        invalidate();
        return true;
    }

    private boolean isTouchExplorationEnabled() {
        return this.mIsAccessibilityEnabled && this.mAccessibilityMgr.isTouchExplorationEnabled();
    }

    private void sendAccessibilityEventAsNeeded(boolean speakEvents) {
        if (this.mIsAccessibilityEnabled) {
            boolean dayChanged = this.mLastSelectionDayForAccessibility != this.mSelectionDayForAccessibility;
            boolean hourChanged = this.mLastSelectionHourForAccessibility != this.mSelectionHourForAccessibility;
            if (dayChanged || hourChanged || this.mLastSelectedEventForAccessibility != this.mSelectedEventForAccessibility) {
                this.mLastSelectionDayForAccessibility = this.mSelectionDayForAccessibility;
                this.mLastSelectionHourForAccessibility = this.mSelectionHourForAccessibility;
                this.mLastSelectedEventForAccessibility = this.mSelectedEventForAccessibility;
                StringBuilder b = new StringBuilder();
                if (dayChanged) {
                    b.append(getSelectedTimeForAccessibility().format("%A "));
                }
                if (hourChanged) {
                    b.append(getSelectedTimeForAccessibility().format(this.mIs24HourFormat ? "%k" : "%l%p"));
                }
                if (dayChanged || hourChanged) {
                    b.append(". ");
                }
                if (speakEvents) {
                    if (this.mEventCountTemplate == null) {
                        this.mEventCountTemplate = this.mContext.getString(R.string.template_announce_item_index);
                    }
                    int numEvents = this.mSelectedEvents.size();
                    if (numEvents > 0) {
                        if (this.mSelectedEventForAccessibility == null) {
                            int i = 1;
                            for (Event calEvent : this.mSelectedEvents) {
                                if (numEvents > 1) {
                                    mStringBuilder.setLength(0);
                                    b.append(mFormatter.format(this.mEventCountTemplate, Integer.valueOf(i), Integer.valueOf(numEvents)));
                                    b.append(" ");
                                    i++;
                                }
                                appendEventAccessibilityString(b, calEvent);
                            }
                        } else {
                            if (numEvents > 1) {
                                mStringBuilder.setLength(0);
                                b.append(mFormatter.format(this.mEventCountTemplate, Integer.valueOf(this.mSelectedEvents.indexOf(this.mSelectedEventForAccessibility) + 1), Integer.valueOf(numEvents)));
                                b.append(" ");
                            }
                            appendEventAccessibilityString(b, this.mSelectedEventForAccessibility);
                        }
                    } else {
                        b.append(this.mCreateNewEventString);
                    }
                }
                if (dayChanged || hourChanged || speakEvents) {
                    AccessibilityEvent event = AccessibilityEvent.obtain(8);
                    CharSequence msg = b.toString();
                    event.getText().add(msg);
                    event.setAddedCount(msg.length());
                    sendAccessibilityEventUnchecked(event);
                }
            }
        }
    }

    private void appendEventAccessibilityString(StringBuilder b, Event calEvent) {
        int flags;
        b.append(calEvent.getTitleAndLocation());
        b.append(". ");
        if (calEvent.allDay) {
            flags = 16 | 8194;
        } else {
            flags = 16 | 1;
            if (DateFormat.is24HourFormat(this.mContext)) {
                flags |= 128;
            }
        }
        String when = Utils.formatDateRange(this.mContext, calEvent.startMillis, calEvent.endMillis, flags);
        b.append(when);
        b.append(". ");
    }

    private class GotoBroadcaster implements Animation.AnimationListener {
        private final int mCounter = DayView.access$1104();
        private final Time mEnd;
        private final Time mStart;

        public GotoBroadcaster(Time start, Time end) {
            this.mStart = start;
            this.mEnd = end;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            DayView view = (DayView) DayView.this.mViewSwitcher.getCurrentView();
            view.mViewStartX = 0;
            DayView view2 = (DayView) DayView.this.mViewSwitcher.getNextView();
            view2.mViewStartX = 0;
            if (this.mCounter == DayView.sCounter) {
                DayView.this.mController.sendEvent(this, 32L, this.mStart, this.mEnd, null, -1L, 0, 1L, null, null);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }
    }

    private View switchViews(boolean forward, float xOffSet, float width, float velocity) {
        float inFromXValue;
        float inToXValue;
        float outFromXValue;
        float outToXValue;
        this.mAnimationDistance = width - xOffSet;
        if (DEBUG) {
            Log.d(TAG, "switchViews(" + forward + ") O:" + xOffSet + " Dist:" + this.mAnimationDistance);
        }
        float progress = Math.abs(xOffSet) / width;
        if (progress > 1.0f) {
            progress = 1.0f;
        }
        if (forward) {
            inFromXValue = 1.0f - progress;
            inToXValue = 0.0f;
            outFromXValue = -progress;
            outToXValue = -1.0f;
        } else {
            inFromXValue = progress - 1.0f;
            inToXValue = 0.0f;
            outFromXValue = progress;
            outToXValue = 1.0f;
        }
        Time start = new Time(this.mBaseDate.timezone);
        start.set(this.mController.getTime());
        if (forward) {
            start.monthDay += this.mNumDays;
        } else {
            start.monthDay -= this.mNumDays;
        }
        this.mController.setTime(start.normalize(true));
        Time newSelected = start;
        if (this.mNumDays == 7) {
            newSelected = new Time(start);
            adjustToBeginningOfWeek(start);
        }
        Time end = new Time(start);
        end.monthDay += this.mNumDays - 1;
        TranslateAnimation inAnimation = new TranslateAnimation(1, inFromXValue, 1, inToXValue, 0, 0.0f, 0, 0.0f);
        TranslateAnimation outAnimation = new TranslateAnimation(1, outFromXValue, 1, outToXValue, 0, 0.0f, 0, 0.0f);
        long duration = calculateDuration(width - Math.abs(xOffSet), width, velocity);
        inAnimation.setDuration(duration);
        inAnimation.setInterpolator(this.mHScrollInterpolator);
        outAnimation.setInterpolator(this.mHScrollInterpolator);
        outAnimation.setDuration(duration);
        outAnimation.setAnimationListener(new GotoBroadcaster(start, end));
        this.mViewSwitcher.setInAnimation(inAnimation);
        this.mViewSwitcher.setOutAnimation(outAnimation);
        ((DayView) this.mViewSwitcher.getCurrentView()).cleanup();
        this.mViewSwitcher.showNext();
        DayView view = (DayView) this.mViewSwitcher.getCurrentView();
        view.setSelected(newSelected, true, false);
        view.requestFocus();
        view.reloadEvents();
        view.updateTitle();
        view.restartCurrentTimeUpdates();
        return view;
    }

    private void resetSelectedHour() {
        if (this.mSelectionHour < this.mFirstHour + 1) {
            setSelectedHour(this.mFirstHour + 1);
            setSelectedEvent(null);
            this.mSelectedEvents.clear();
            this.mComputeSelectedEvents = true;
            return;
        }
        if (this.mSelectionHour > (this.mFirstHour + this.mNumHours) - 3) {
            setSelectedHour((this.mFirstHour + this.mNumHours) - 3);
            setSelectedEvent(null);
            this.mSelectedEvents.clear();
            this.mComputeSelectedEvents = true;
        }
    }

    private void initFirstHour() {
        this.mFirstHour = this.mSelectionHour - (this.mNumHours / 5);
        if (this.mFirstHour < 0) {
            this.mFirstHour = 0;
        } else if (this.mFirstHour + this.mNumHours > 24) {
            this.mFirstHour = 24 - this.mNumHours;
        }
    }

    private void computeFirstHour() {
        this.mFirstHour = (((this.mViewStartY + mCellHeight) + 1) - 1) / (mCellHeight + 1);
        this.mFirstHourOffset = (this.mFirstHour * (mCellHeight + 1)) - this.mViewStartY;
    }

    private void adjustHourSelection() {
        if (this.mSelectionHour < 0) {
            setSelectedHour(0);
            if (this.mMaxAlldayEvents > 0) {
                this.mPrevSelectedEvent = null;
                this.mSelectionAllday = true;
            }
        }
        if (this.mSelectionHour > 23) {
            setSelectedHour(23);
        }
        if (this.mSelectionHour < this.mFirstHour + 1) {
            int daynum = this.mSelectionDay - this.mFirstJulianDay;
            if (daynum < this.mEarliestStartHour.length && daynum >= 0 && this.mMaxAlldayEvents > 0 && this.mEarliestStartHour[daynum] > this.mSelectionHour && this.mFirstHour > 0 && this.mFirstHour < 8) {
                this.mPrevSelectedEvent = null;
                this.mSelectionAllday = true;
                setSelectedHour(this.mFirstHour + 1);
                return;
            } else if (this.mFirstHour > 0) {
                this.mFirstHour--;
                this.mViewStartY -= mCellHeight + 1;
                if (this.mViewStartY < 0) {
                    this.mViewStartY = 0;
                    return;
                }
                return;
            }
        }
        if (this.mSelectionHour > (this.mFirstHour + this.mNumHours) - 3) {
            if (this.mFirstHour < 24 - this.mNumHours) {
                this.mFirstHour++;
                this.mViewStartY += mCellHeight + 1;
                if (this.mViewStartY > this.mMaxViewStartY) {
                    this.mViewStartY = this.mMaxViewStartY;
                    return;
                }
                return;
            }
            if (this.mFirstHour == 24 - this.mNumHours && this.mFirstHourOffset > 0) {
                this.mViewStartY = this.mMaxViewStartY;
            }
        }
    }

    void clearCachedEvents() {
        this.mLastReloadMillis = 0L;
    }

    void reloadEvents() {
        this.mTZUpdater.run();
        setSelectedEvent(null);
        this.mPrevSelectedEvent = null;
        this.mSelectedEvents.clear();
        Time weekStart = new Time(Utils.getTimeZone(this.mContext, this.mTZUpdater));
        weekStart.set(this.mBaseDate);
        weekStart.hour = 0;
        weekStart.minute = 0;
        weekStart.second = 0;
        long millis = weekStart.normalize(true);
        if (millis != this.mLastReloadMillis) {
            this.mLastReloadMillis = millis;
            final ArrayList<Event> events = new ArrayList<>();
            this.mEventLoader.loadEventsInBackground(this.mNumDays, events, this.mFirstJulianDay, new Runnable() {
                @Override
                public void run() {
                    boolean fadeinEvents = DayView.this.mFirstJulianDay != DayView.this.mLoadedFirstJulianDay;
                    DayView.this.mEvents = events;
                    DayView.this.mLoadedFirstJulianDay = DayView.this.mFirstJulianDay;
                    if (DayView.this.mAllDayEvents == null) {
                        DayView.this.mAllDayEvents = new ArrayList();
                    } else {
                        DayView.this.mAllDayEvents.clear();
                    }
                    for (Event e : events) {
                        if (e.drawAsAllday()) {
                            DayView.this.mAllDayEvents.add(e);
                        }
                    }
                    if (DayView.this.mLayouts == null || DayView.this.mLayouts.length < events.size()) {
                        DayView.this.mLayouts = new StaticLayout[events.size()];
                    } else {
                        Arrays.fill(DayView.this.mLayouts, (Object) null);
                    }
                    if (DayView.this.mAllDayLayouts == null || DayView.this.mAllDayLayouts.length < DayView.this.mAllDayEvents.size()) {
                        DayView.this.mAllDayLayouts = new StaticLayout[events.size()];
                    } else {
                        Arrays.fill(DayView.this.mAllDayLayouts, (Object) null);
                    }
                    DayView.this.computeEventRelations();
                    DayView.this.mRemeasure = true;
                    DayView.this.mComputeSelectedEvents = true;
                    DayView.this.recalc();
                    if (fadeinEvents) {
                        if (DayView.this.mEventsCrossFadeAnimation == null) {
                            DayView.this.mEventsCrossFadeAnimation = ObjectAnimator.ofInt(DayView.this, "EventsAlpha", 0, 255);
                            DayView.this.mEventsCrossFadeAnimation.setDuration(400L);
                        }
                        DayView.this.mEventsCrossFadeAnimation.start();
                        return;
                    }
                    DayView.this.invalidate();
                }
            }, this.mCancelCallback);
        }
    }

    public void setEventsAlpha(int alpha) {
        this.mEventsAlpha = alpha;
        invalidate();
    }

    public int getEventsAlpha() {
        return this.mEventsAlpha;
    }

    public void stopEventsAnimation() {
        if (this.mEventsCrossFadeAnimation != null) {
            this.mEventsCrossFadeAnimation.cancel();
        }
        this.mEventsAlpha = 255;
    }

    private void computeEventRelations() {
        int maxAllDayEvents = 0;
        ArrayList<Event> events = this.mEvents;
        int len = events.size();
        int[] eventsCount = new int[(this.mLastJulianDay - this.mFirstJulianDay) + 1];
        Arrays.fill(eventsCount, 0);
        for (int ii = 0; ii < len; ii++) {
            Event event = events.get(ii);
            if (event.startDay <= this.mLastJulianDay && event.endDay >= this.mFirstJulianDay) {
                if (event.drawAsAllday()) {
                    int firstDay = Math.max(event.startDay, this.mFirstJulianDay);
                    int lastDay = Math.min(event.endDay, this.mLastJulianDay);
                    for (int day = firstDay; day <= lastDay; day++) {
                        int i = day - this.mFirstJulianDay;
                        int count = eventsCount[i] + 1;
                        eventsCount[i] = count;
                        if (maxAllDayEvents < count) {
                            maxAllDayEvents = count;
                        }
                    }
                    int daynum = event.startDay - this.mFirstJulianDay;
                    int durationDays = (event.endDay - event.startDay) + 1;
                    if (daynum < 0) {
                        durationDays += daynum;
                        daynum = 0;
                    }
                    if (daynum + durationDays > this.mNumDays) {
                        durationDays = this.mNumDays - daynum;
                    }
                    int day2 = daynum;
                    while (durationDays > 0) {
                        this.mHasAllDayEvent[day2] = true;
                        day2++;
                        durationDays--;
                    }
                } else {
                    int daynum2 = event.startDay - this.mFirstJulianDay;
                    int hour = event.startTime / 60;
                    if (daynum2 >= 0 && hour < this.mEarliestStartHour[daynum2]) {
                        this.mEarliestStartHour[daynum2] = hour;
                    }
                    int daynum3 = event.endDay - this.mFirstJulianDay;
                    int hour2 = event.endTime / 60;
                    if (daynum3 < this.mNumDays && hour2 < this.mEarliestStartHour[daynum3]) {
                        this.mEarliestStartHour[daynum3] = hour2;
                    }
                }
            }
        }
        this.mMaxAlldayEvents = maxAllDayEvents;
        initAllDayHeights();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float xTranslate;
        if (this.mRemeasure) {
            remeasure(getWidth(), getHeight());
            this.mRemeasure = false;
        }
        canvas.save();
        float yTranslate = (-this.mViewStartY) + DAY_HEADER_HEIGHT + this.mAlldayHeight;
        canvas.translate(-this.mViewStartX, yTranslate);
        Rect dest = this.mDestRect;
        dest.top = (int) (this.mFirstCell - yTranslate);
        dest.bottom = (int) (this.mViewHeight - yTranslate);
        dest.left = 0;
        dest.right = this.mViewWidth;
        canvas.save();
        canvas.clipRect(dest);
        doDraw(canvas);
        canvas.restore();
        if ((this.mTouchMode & 64) != 0) {
            if (this.mViewStartX > 0) {
                xTranslate = this.mViewWidth;
            } else {
                xTranslate = -this.mViewWidth;
            }
            canvas.translate(xTranslate, -yTranslate);
            DayView nextView = (DayView) this.mViewSwitcher.getNextView();
            nextView.mTouchMode = 0;
            nextView.onDraw(canvas);
            canvas.translate(-xTranslate, 0.0f);
        } else {
            canvas.translate(this.mViewStartX, -yTranslate);
        }
        drawAfterScroll(canvas);
        if (this.mComputeSelectedEvents && this.mUpdateToast) {
            updateEventDetails();
            this.mUpdateToast = false;
        }
        this.mComputeSelectedEvents = false;
        if (!this.mEdgeEffectTop.isFinished()) {
            if (DAY_HEADER_HEIGHT != 0) {
                canvas.translate(0.0f, DAY_HEADER_HEIGHT);
            }
            if (this.mEdgeEffectTop.draw(canvas)) {
                invalidate();
            }
            if (DAY_HEADER_HEIGHT != 0) {
                canvas.translate(0.0f, -DAY_HEADER_HEIGHT);
            }
        }
        if (!this.mEdgeEffectBottom.isFinished()) {
            canvas.rotate(180.0f, this.mViewWidth / 2, this.mViewHeight / 2);
            if (this.mEdgeEffectBottom.draw(canvas)) {
                invalidate();
            }
        }
        canvas.restore();
    }

    private void drawAfterScroll(Canvas canvas) {
        Paint p = this.mPaint;
        Rect r = this.mRect;
        drawAllDayHighlights(r, canvas, p);
        if (this.mMaxAlldayEvents != 0) {
            drawAllDayEvents(this.mFirstJulianDay, this.mNumDays, canvas, p);
            drawUpperLeftCorner(r, canvas, p);
        }
        drawScrollLine(r, canvas, p);
        drawDayHeaderLoop(r, canvas, p);
        if (!this.mIs24HourFormat) {
            drawAmPm(canvas, p);
        }
    }

    private void drawUpperLeftCorner(Rect r, Canvas canvas, Paint p) {
        setupHourTextPaint(p);
        if (this.mMaxAlldayEvents > this.mMaxUnexpandedAlldayEventCount) {
            if (mUseExpandIcon) {
                this.mExpandAlldayDrawable.setBounds(this.mExpandAllDayRect);
                this.mExpandAlldayDrawable.draw(canvas);
            } else {
                this.mCollapseAlldayDrawable.setBounds(this.mExpandAllDayRect);
                this.mCollapseAlldayDrawable.draw(canvas);
            }
        }
    }

    private void drawScrollLine(Rect r, Canvas canvas, Paint p) {
        int right = computeDayLeftPosition(this.mNumDays);
        int y = this.mFirstCell - 1;
        p.setAntiAlias(false);
        p.setStyle(Paint.Style.FILL);
        p.setColor(mCalendarGridLineInnerHorizontalColor);
        p.setStrokeWidth(1.0f);
        canvas.drawLine(GRID_LINE_LEFT_MARGIN, y, right, y, p);
        p.setAntiAlias(true);
    }

    private int computeDayLeftPosition(int day) {
        int effectiveWidth = this.mViewWidth - this.mHoursWidth;
        return ((day * effectiveWidth) / this.mNumDays) + this.mHoursWidth;
    }

    private void drawAllDayHighlights(Rect r, Canvas canvas, Paint p) {
        if (mFutureBgColor != 0) {
            r.top = 0;
            r.bottom = DAY_HEADER_HEIGHT;
            r.left = 0;
            r.right = this.mViewWidth;
            p.setColor(mBgColor);
            p.setStyle(Paint.Style.FILL);
            canvas.drawRect(r, p);
            r.top = DAY_HEADER_HEIGHT;
            r.bottom = this.mFirstCell - 1;
            r.left = 0;
            r.right = this.mHoursWidth;
            canvas.drawRect(r, p);
            int startIndex = -1;
            int todayIndex = this.mTodayJulianDay - this.mFirstJulianDay;
            if (todayIndex < 0) {
                startIndex = 0;
            } else if (todayIndex >= 1 && todayIndex + 1 < this.mNumDays) {
                startIndex = todayIndex + 1;
            }
            if (startIndex >= 0) {
                r.top = 0;
                r.bottom = this.mFirstCell - 1;
                r.left = computeDayLeftPosition(startIndex) + 1;
                r.right = computeDayLeftPosition(this.mNumDays);
                p.setColor(mFutureBgColor);
                p.setStyle(Paint.Style.FILL);
                canvas.drawRect(r, p);
            }
        }
        if (this.mSelectionAllday && this.mSelectionMode != 0) {
            this.mRect.top = DAY_HEADER_HEIGHT + 1;
            this.mRect.bottom = ((this.mRect.top + this.mAlldayHeight) + ALLDAY_TOP_MARGIN) - 2;
            int daynum = this.mSelectionDay - this.mFirstJulianDay;
            this.mRect.left = computeDayLeftPosition(daynum) + 1;
            this.mRect.right = computeDayLeftPosition(daynum + 1);
            p.setColor(mCalendarGridAreaSelected);
            canvas.drawRect(this.mRect, p);
        }
    }

    private void drawDayHeaderLoop(Rect r, Canvas canvas, Paint p) {
        String[] dayNames;
        if (this.mNumDays != 1 || ONE_DAY_HEADER_HEIGHT != 0) {
            p.setTypeface(this.mBold);
            p.setTextAlign(Paint.Align.RIGHT);
            int cell = this.mFirstJulianDay;
            if (this.mDateStrWidth < this.mCellWidth) {
                dayNames = this.mDayStrs;
            } else {
                dayNames = this.mDayStrs2Letter;
            }
            p.setAntiAlias(true);
            int day = 0;
            while (day < this.mNumDays) {
                int dayOfWeek = day + this.mFirstVisibleDayOfWeek;
                if (dayOfWeek >= 14) {
                    dayOfWeek -= 14;
                }
                int color = mCalendarDateBannerTextColor;
                if (this.mNumDays == 1) {
                    if (dayOfWeek == 6) {
                        color = mWeek_saturdayColor;
                    } else if (dayOfWeek == 0) {
                        color = mWeek_sundayColor;
                    }
                } else {
                    int column = day % 7;
                    if (Utils.isSaturday(column, this.mFirstDayOfWeek)) {
                        color = mWeek_saturdayColor;
                    } else if (Utils.isSunday(column, this.mFirstDayOfWeek)) {
                        color = mWeek_sundayColor;
                    }
                }
                p.setColor(color);
                drawDayHeader(dayNames[dayOfWeek], day, cell, canvas, p);
                day++;
                cell++;
            }
            p.setTypeface(null);
        }
    }

    private void drawAmPm(Canvas canvas, Paint p) {
        p.setColor(mCalendarAmPmLabel);
        p.setTextSize(AMPM_TEXT_SIZE);
        p.setTypeface(this.mBold);
        p.setAntiAlias(true);
        p.setTextAlign(Paint.Align.RIGHT);
        String text = this.mAmString;
        if (this.mFirstHour >= 12) {
            text = this.mPmString;
        }
        int y = this.mFirstCell + this.mFirstHourOffset + (this.mHoursTextHeight * 2) + 1;
        canvas.drawText(text, HOURS_LEFT_MARGIN, y, p);
        if (this.mFirstHour < 12 && this.mFirstHour + this.mNumHours > 12) {
            String text2 = this.mPmString;
            int y2 = this.mFirstCell + this.mFirstHourOffset + ((12 - this.mFirstHour) * (mCellHeight + 1)) + (this.mHoursTextHeight * 2) + 1;
            canvas.drawText(text2, HOURS_LEFT_MARGIN, y2, p);
        }
    }

    private void drawCurrentTimeLine(Rect r, int day, int top, Canvas canvas, Paint p) {
        r.left = (computeDayLeftPosition(day) - CURRENT_TIME_LINE_SIDE_BUFFER) + 1;
        r.right = computeDayLeftPosition(day + 1) + CURRENT_TIME_LINE_SIDE_BUFFER + 1;
        r.top = top - CURRENT_TIME_LINE_TOP_OFFSET;
        r.bottom = r.top + this.mCurrentTimeLine.getIntrinsicHeight();
        this.mCurrentTimeLine.setBounds(r);
        this.mCurrentTimeLine.draw(canvas);
        if (this.mAnimateToday) {
            this.mCurrentTimeAnimateLine.setBounds(r);
            this.mCurrentTimeAnimateLine.setAlpha(this.mAnimateTodayAlpha);
            this.mCurrentTimeAnimateLine.draw(canvas);
        }
    }

    private void doDraw(Canvas canvas) {
        int lineY;
        Paint p = this.mPaint;
        Rect r = this.mRect;
        if (mFutureBgColor != 0) {
            drawBgColors(r, canvas, p);
        }
        drawGridBackground(r, canvas, p);
        drawHours(r, canvas, p);
        int cell = this.mFirstJulianDay;
        p.setAntiAlias(false);
        int alpha = p.getAlpha();
        p.setAlpha(this.mEventsAlpha);
        int day = 0;
        while (day < this.mNumDays) {
            drawEvents(cell, day, 1, canvas, p);
            if (cell == this.mTodayJulianDay && (lineY = (this.mCurrentTime.hour * (mCellHeight + 1)) + ((this.mCurrentTime.minute * mCellHeight) / 60) + 1) >= this.mViewStartY && lineY < (this.mViewStartY + this.mViewHeight) - 2) {
                drawCurrentTimeLine(r, day, lineY, canvas, p);
            }
            day++;
            cell++;
        }
        p.setAntiAlias(true);
        p.setAlpha(alpha);
        drawSelectedRect(r, canvas, p);
    }

    private void drawSelectedRect(Rect r, Canvas canvas, Paint p) {
        if (this.mSelectionMode != 0 && !this.mSelectionAllday) {
            int daynum = this.mSelectionDay - this.mFirstJulianDay;
            r.top = this.mSelectionHour * (mCellHeight + 1);
            r.bottom = r.top + mCellHeight + 1;
            r.left = computeDayLeftPosition(daynum) + 1;
            r.right = computeDayLeftPosition(daynum + 1) + 1;
            saveSelectionPosition(r.left, r.top, r.right, r.bottom);
            p.setColor(mCalendarGridAreaSelected);
            r.top++;
            r.right--;
            p.setAntiAlias(false);
            canvas.drawRect(r, p);
            p.setColor(mNewEventHintColor);
            if (this.mNumDays > 1) {
                p.setStrokeWidth(NEW_EVENT_WIDTH);
                int width = r.right - r.left;
                int midX = r.left + (width / 2);
                int midY = r.top + (mCellHeight / 2);
                int length = Math.min(Math.min(mCellHeight, width) - (NEW_EVENT_MARGIN * 2), NEW_EVENT_MAX_LENGTH);
                int verticalPadding = (mCellHeight - length) / 2;
                int horizontalPadding = (width - length) / 2;
                canvas.drawLine(r.left + horizontalPadding, midY, r.right - horizontalPadding, midY, p);
                canvas.drawLine(midX, r.top + verticalPadding, midX, r.bottom - verticalPadding, p);
                return;
            }
            p.setStyle(Paint.Style.FILL);
            p.setTextSize(NEW_EVENT_HINT_FONT_SIZE);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTypeface(Typeface.defaultFromStyle(1));
            canvas.drawText(this.mNewEventHintString, r.left + EVENT_TEXT_LEFT_MARGIN, r.top + Math.abs(p.getFontMetrics().ascent) + EVENT_TEXT_TOP_MARGIN, p);
        }
    }

    private void drawHours(Rect r, Canvas canvas, Paint p) {
        setupHourTextPaint(p);
        int y = this.mHoursTextHeight + 1 + HOURS_TOP_MARGIN;
        for (int i = 0; i < 24; i++) {
            String time = this.mHourStrs[i];
            canvas.drawText(time, HOURS_LEFT_MARGIN, y, p);
            y += mCellHeight + 1;
        }
    }

    private void setupHourTextPaint(Paint p) {
        p.setColor(mCalendarHourLabelColor);
        p.setTextSize(HOURS_TEXT_SIZE);
        p.setTypeface(Typeface.DEFAULT);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setAntiAlias(true);
    }

    private void drawDayHeader(String dayStr, int day, int cell, Canvas canvas, Paint p) {
        int dateNum = this.mFirstVisibleDate + day;
        if (dateNum > this.mMonthLength) {
            dateNum -= this.mMonthLength;
        }
        p.setAntiAlias(true);
        int todayIndex = this.mTodayJulianDay - this.mFirstJulianDay;
        String dateNumStr = String.valueOf(dateNum);
        if (this.mNumDays > 1) {
            float y = DAY_HEADER_HEIGHT - DAY_HEADER_BOTTOM_MARGIN;
            int x = computeDayLeftPosition(day + 1) - DAY_HEADER_RIGHT_MARGIN;
            p.setTextAlign(Paint.Align.RIGHT);
            p.setTextSize(DATE_HEADER_FONT_SIZE);
            p.setTypeface(todayIndex == day ? this.mBold : Typeface.DEFAULT);
            canvas.drawText(dateNumStr, x, y, p);
            int x2 = (int) (x - p.measureText(" " + dateNumStr));
            p.setTextSize(DAY_HEADER_FONT_SIZE);
            p.setTypeface(Typeface.DEFAULT);
            canvas.drawText(dayStr, x2, y, p);
            return;
        }
        float y2 = ONE_DAY_HEADER_HEIGHT - DAY_HEADER_ONE_DAY_BOTTOM_MARGIN;
        p.setTextAlign(Paint.Align.LEFT);
        int x3 = computeDayLeftPosition(day) + DAY_HEADER_ONE_DAY_LEFT_MARGIN;
        p.setTextSize(DAY_HEADER_FONT_SIZE);
        p.setTypeface(Typeface.DEFAULT);
        canvas.drawText(dayStr, x3, y2, p);
        int x4 = (int) (x3 + p.measureText(dayStr) + DAY_HEADER_ONE_DAY_RIGHT_MARGIN);
        p.setTextSize(DATE_HEADER_FONT_SIZE);
        p.setTypeface(todayIndex == day ? this.mBold : Typeface.DEFAULT);
        canvas.drawText(dateNumStr, x4, y2, p);
    }

    private void drawGridBackground(Rect r, Canvas canvas, Paint p) {
        int linesIndex;
        Paint.Style savedStyle = p.getStyle();
        float stopX = computeDayLeftPosition(this.mNumDays);
        float deltaY = mCellHeight + 1;
        float stopY = ((mCellHeight + 1) * 24) + 1;
        float f = this.mHoursWidth;
        p.setColor(mCalendarGridLineInnerHorizontalColor);
        p.setStrokeWidth(1.0f);
        p.setAntiAlias(false);
        float y = 0.0f;
        int linesIndex2 = 0;
        for (int hour = 0; hour <= 24; hour++) {
            int linesIndex3 = linesIndex2 + 1;
            this.mLines[linesIndex2] = GRID_LINE_LEFT_MARGIN;
            int linesIndex4 = linesIndex3 + 1;
            this.mLines[linesIndex3] = y;
            int linesIndex5 = linesIndex4 + 1;
            this.mLines[linesIndex4] = stopX;
            linesIndex2 = linesIndex5 + 1;
            this.mLines[linesIndex5] = y;
            y += deltaY;
        }
        if (mCalendarGridLineInnerVerticalColor != mCalendarGridLineInnerHorizontalColor) {
            canvas.drawLines(this.mLines, 0, linesIndex2, p);
            linesIndex = 0;
            p.setColor(mCalendarGridLineInnerVerticalColor);
        } else {
            linesIndex = linesIndex2;
        }
        for (int day = 0; day <= this.mNumDays; day++) {
            float x = computeDayLeftPosition(day);
            int linesIndex6 = linesIndex + 1;
            this.mLines[linesIndex] = x;
            int linesIndex7 = linesIndex6 + 1;
            this.mLines[linesIndex6] = 0.0f;
            int linesIndex8 = linesIndex7 + 1;
            this.mLines[linesIndex7] = x;
            linesIndex = linesIndex8 + 1;
            this.mLines[linesIndex8] = stopY;
        }
        canvas.drawLines(this.mLines, 0, linesIndex, p);
        p.setStyle(savedStyle);
        p.setAntiAlias(true);
    }

    private void drawBgColors(Rect r, Canvas canvas, Paint p) {
        int todayIndex = this.mTodayJulianDay - this.mFirstJulianDay;
        r.top = this.mDestRect.top;
        r.bottom = this.mDestRect.bottom;
        r.left = 0;
        r.right = this.mHoursWidth;
        p.setColor(mBgColor);
        p.setStyle(Paint.Style.FILL);
        p.setAntiAlias(false);
        canvas.drawRect(r, p);
        if (this.mNumDays == 1 && todayIndex == 0) {
            int lineY = (this.mCurrentTime.hour * (mCellHeight + 1)) + ((this.mCurrentTime.minute * mCellHeight) / 60) + 1;
            if (lineY < this.mViewStartY + this.mViewHeight) {
                int lineY2 = Math.max(lineY, this.mViewStartY);
                r.left = this.mHoursWidth;
                r.right = this.mViewWidth;
                r.top = lineY2;
                r.bottom = this.mViewStartY + this.mViewHeight;
                p.setColor(mFutureBgColor);
                canvas.drawRect(r, p);
            }
        } else if (todayIndex >= 0 && todayIndex < this.mNumDays) {
            int lineY3 = (this.mCurrentTime.hour * (mCellHeight + 1)) + ((this.mCurrentTime.minute * mCellHeight) / 60) + 1;
            if (lineY3 < this.mViewStartY + this.mViewHeight) {
                int lineY4 = Math.max(lineY3, this.mViewStartY);
                r.left = computeDayLeftPosition(todayIndex) + 1;
                r.right = computeDayLeftPosition(todayIndex + 1);
                r.top = lineY4;
                r.bottom = this.mViewStartY + this.mViewHeight;
                p.setColor(mFutureBgColor);
                canvas.drawRect(r, p);
            }
            if (todayIndex + 1 < this.mNumDays) {
                r.left = computeDayLeftPosition(todayIndex + 1) + 1;
                r.right = computeDayLeftPosition(this.mNumDays);
                r.top = this.mDestRect.top;
                r.bottom = this.mDestRect.bottom;
                p.setColor(mFutureBgColor);
                canvas.drawRect(r, p);
            }
        } else if (todayIndex < 0) {
            r.left = computeDayLeftPosition(0) + 1;
            r.right = computeDayLeftPosition(this.mNumDays);
            r.top = this.mDestRect.top;
            r.bottom = this.mDestRect.bottom;
            p.setColor(mFutureBgColor);
            canvas.drawRect(r, p);
        }
        p.setAntiAlias(true);
    }

    private int computeMaxStringWidth(int currentMax, String[] strings, Paint p) {
        float maxWidthF = 0.0f;
        for (String str : strings) {
            float width = p.measureText(str);
            maxWidthF = Math.max(width, maxWidthF);
        }
        int maxWidth = (int) (((double) maxWidthF) + 0.5d);
        if (maxWidth < currentMax) {
            return currentMax;
        }
        return maxWidth;
    }

    private void saveSelectionPosition(float left, float top, float right, float bottom) {
        this.mPrevBox.left = (int) left;
        this.mPrevBox.right = (int) right;
        this.mPrevBox.top = (int) top;
        this.mPrevBox.bottom = (int) bottom;
    }

    private Rect getCurrentSelectionPosition() {
        Rect box = new Rect();
        box.top = this.mSelectionHour * (mCellHeight + 1);
        box.bottom = box.top + mCellHeight + 1;
        int daynum = this.mSelectionDay - this.mFirstJulianDay;
        box.left = computeDayLeftPosition(daynum) + 1;
        box.right = computeDayLeftPosition(daynum + 1);
        return box;
    }

    private void setupTextRect(Rect r) {
        if (r.bottom <= r.top || r.right <= r.left) {
            r.bottom = r.top;
            r.right = r.left;
            return;
        }
        if (r.bottom - r.top > EVENT_TEXT_TOP_MARGIN + EVENT_TEXT_BOTTOM_MARGIN) {
            r.top += EVENT_TEXT_TOP_MARGIN;
            r.bottom -= EVENT_TEXT_BOTTOM_MARGIN;
        }
        if (r.right - r.left > EVENT_TEXT_LEFT_MARGIN + EVENT_TEXT_RIGHT_MARGIN) {
            r.left += EVENT_TEXT_LEFT_MARGIN;
            r.right -= EVENT_TEXT_RIGHT_MARGIN;
        }
    }

    private void setupAllDayTextRect(Rect r) {
        if (r.bottom <= r.top || r.right <= r.left) {
            r.bottom = r.top;
            r.right = r.left;
            return;
        }
        if (r.bottom - r.top > EVENT_ALL_DAY_TEXT_TOP_MARGIN + EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN) {
            r.top += EVENT_ALL_DAY_TEXT_TOP_MARGIN;
            r.bottom -= EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN;
        }
        if (r.right - r.left > EVENT_ALL_DAY_TEXT_LEFT_MARGIN + EVENT_ALL_DAY_TEXT_RIGHT_MARGIN) {
            r.left += EVENT_ALL_DAY_TEXT_LEFT_MARGIN;
            r.right -= EVENT_ALL_DAY_TEXT_RIGHT_MARGIN;
        }
    }

    private StaticLayout getEventLayout(StaticLayout[] layouts, int i, Event event, Paint paint, Rect r) {
        if (i < 0 || i >= layouts.length) {
            return null;
        }
        StaticLayout layout = layouts[i];
        if (layout == null || r.width() != layout.getWidth()) {
            SpannableStringBuilder bob = new SpannableStringBuilder();
            if (event.title != null) {
                bob.append((CharSequence) drawTextSanitizer(event.title.toString(), 499));
                bob.setSpan(new StyleSpan(1), 0, bob.length(), 0);
                bob.append(' ');
            }
            if (event.location != null) {
                bob.append((CharSequence) drawTextSanitizer(event.location.toString(), 500 - bob.length()));
            }
            switch (event.selfAttendeeStatus) {
                case 2:
                    paint.setColor(mEventTextColor);
                    paint.setAlpha(192);
                    break;
                case 3:
                    paint.setColor(event.color);
                    break;
                default:
                    paint.setColor(mEventTextColor);
                    break;
            }
            layout = new StaticLayout(bob, 0, bob.length(), new TextPaint(paint), r.width(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true, null, r.width());
            layouts[i] = layout;
        }
        layout.getPaint().setAlpha(this.mEventsAlpha);
        return layout;
    }

    private void drawAllDayEvents(int firstDay, int numDays, Canvas canvas, Paint p) {
        p.setTextSize(NORMAL_FONT_SIZE);
        p.setTextAlign(Paint.Align.LEFT);
        Paint eventTextPaint = this.mEventTextPaint;
        float startY = DAY_HEADER_HEIGHT;
        float stopY = this.mAlldayHeight + startY + ALLDAY_TOP_MARGIN;
        p.setColor(mCalendarGridLineInnerVerticalColor);
        float f = this.mHoursWidth;
        p.setStrokeWidth(1.0f);
        int linesIndex = 0 + 1;
        this.mLines[0] = GRID_LINE_LEFT_MARGIN;
        int linesIndex2 = linesIndex + 1;
        this.mLines[linesIndex] = startY;
        int linesIndex3 = linesIndex2 + 1;
        this.mLines[linesIndex2] = computeDayLeftPosition(this.mNumDays);
        int linesIndex4 = linesIndex3 + 1;
        this.mLines[linesIndex3] = startY;
        for (int day = 0; day <= this.mNumDays; day++) {
            float x = computeDayLeftPosition(day);
            int linesIndex5 = linesIndex4 + 1;
            this.mLines[linesIndex4] = x;
            int linesIndex6 = linesIndex5 + 1;
            this.mLines[linesIndex5] = startY;
            int linesIndex7 = linesIndex6 + 1;
            this.mLines[linesIndex6] = x;
            linesIndex4 = linesIndex7 + 1;
            this.mLines[linesIndex7] = stopY;
        }
        p.setAntiAlias(false);
        canvas.drawLines(this.mLines, 0, linesIndex4, p);
        p.setStyle(Paint.Style.FILL);
        int y = DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN;
        int lastDay = (firstDay + numDays) - 1;
        ArrayList<Event> events = this.mAllDayEvents;
        int numEvents = events.size();
        boolean hasMoreEvents = false;
        float drawHeight = this.mAlldayHeight;
        float numRectangles = this.mMaxAlldayEvents;
        int allDayEventClip = DAY_HEADER_HEIGHT + this.mAlldayHeight + ALLDAY_TOP_MARGIN;
        this.mSkippedAlldayEvents = new int[numDays];
        if (this.mMaxAlldayEvents > this.mMaxUnexpandedAlldayEventCount && !mShowAllAllDayEvents && this.mAnimateDayHeight == 0) {
            numRectangles = this.mMaxUnexpandedAlldayEventCount - 1;
            allDayEventClip = (int) (allDayEventClip - MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT);
            hasMoreEvents = true;
        } else if (this.mAnimateDayHeight != 0) {
            allDayEventClip = DAY_HEADER_HEIGHT + this.mAnimateDayHeight + ALLDAY_TOP_MARGIN;
        }
        int alpha = eventTextPaint.getAlpha();
        eventTextPaint.setAlpha(this.mEventsAlpha);
        for (int i = 0; i < numEvents; i++) {
            Event event = events.get(i);
            int startDay = event.startDay;
            int endDay = event.endDay;
            if (startDay <= lastDay && endDay >= firstDay) {
                if (startDay < firstDay) {
                    startDay = firstDay;
                }
                if (endDay > lastDay) {
                    endDay = lastDay;
                }
                int startIndex = startDay - firstDay;
                int endIndex = endDay - firstDay;
                float height = this.mMaxAlldayEvents > this.mMaxUnexpandedAlldayEventCount ? this.mAnimateDayEventHeight : drawHeight / numRectangles;
                if (height > MAX_HEIGHT_OF_ONE_ALLDAY_EVENT) {
                    height = MAX_HEIGHT_OF_ONE_ALLDAY_EVENT;
                }
                event.left = computeDayLeftPosition(startIndex);
                event.right = computeDayLeftPosition(endIndex + 1) - 1;
                event.top = y + (event.getColumn() * height);
                event.bottom = (event.top + height) - ALL_DAY_EVENT_RECT_BOTTOM_MARGIN;
                if (this.mMaxAlldayEvents <= this.mMaxUnexpandedAlldayEventCount) {
                    Rect r = drawEventRect(event, canvas, p, eventTextPaint, (int) event.top, (int) event.bottom);
                    setupAllDayTextRect(r);
                    StaticLayout layout = getEventLayout(this.mAllDayLayouts, i, event, eventTextPaint, r);
                    drawEventText(layout, r, canvas, r.top, r.bottom, true);
                    if (!this.mSelectionAllday && this.mComputeSelectedEvents && startDay <= this.mSelectionDay && endDay >= this.mSelectionDay) {
                        this.mSelectedEvents.add(event);
                    }
                } else if (event.top >= allDayEventClip) {
                    incrementSkipCount(this.mSkippedAlldayEvents, startIndex, endIndex);
                } else if (event.bottom > allDayEventClip) {
                    if (hasMoreEvents) {
                        incrementSkipCount(this.mSkippedAlldayEvents, startIndex, endIndex);
                    } else {
                        event.bottom = allDayEventClip;
                        Rect r2 = drawEventRect(event, canvas, p, eventTextPaint, (int) event.top, (int) event.bottom);
                        setupAllDayTextRect(r2);
                        StaticLayout layout2 = getEventLayout(this.mAllDayLayouts, i, event, eventTextPaint, r2);
                        drawEventText(layout2, r2, canvas, r2.top, r2.bottom, true);
                        if (!this.mSelectionAllday) {
                        }
                    }
                }
            }
        }
        eventTextPaint.setAlpha(alpha);
        if (mMoreAlldayEventsTextAlpha != 0 && this.mSkippedAlldayEvents != null) {
            int alpha2 = p.getAlpha();
            p.setAlpha(this.mEventsAlpha);
            p.setColor((mMoreAlldayEventsTextAlpha << 24) & mMoreEventsTextColor);
            for (int i2 = 0; i2 < this.mSkippedAlldayEvents.length; i2++) {
                if (this.mSkippedAlldayEvents[i2] > 0) {
                    drawMoreAlldayEvents(canvas, this.mSkippedAlldayEvents[i2], i2, p);
                }
            }
            p.setAlpha(alpha2);
        }
        if (this.mSelectionAllday) {
            computeAllDayNeighbors();
            saveSelectionPosition(0.0f, 0.0f, 0.0f, 0.0f);
        }
    }

    private void incrementSkipCount(int[] counts, int startIndex, int endIndex) {
        if (counts != null && startIndex >= 0 && endIndex <= counts.length) {
            for (int i = startIndex; i <= endIndex; i++) {
                counts[i] = counts[i] + 1;
            }
        }
    }

    protected void drawMoreAlldayEvents(Canvas canvas, int remainingEvents, int day, Paint p) {
        int x = computeDayLeftPosition(day) + EVENT_ALL_DAY_TEXT_LEFT_MARGIN;
        int y = (int) (((this.mAlldayHeight - (MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT * 0.5f)) - (EVENT_SQUARE_WIDTH * 0.5f)) + DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN);
        Rect r = this.mRect;
        r.top = y;
        r.left = x;
        r.bottom = EVENT_SQUARE_WIDTH + y;
        r.right = EVENT_SQUARE_WIDTH + x;
        p.setColor(mMoreEventsTextColor);
        p.setStrokeWidth(EVENT_RECT_STROKE_WIDTH);
        p.setStyle(Paint.Style.STROKE);
        p.setAntiAlias(false);
        canvas.drawRect(r, p);
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setTextSize(EVENT_TEXT_FONT_SIZE);
        String text = this.mResources.getQuantityString(R.plurals.month_more_events, remainingEvents);
        canvas.drawText(String.format(text, Integer.valueOf(remainingEvents)), x + EVENT_SQUARE_WIDTH + EVENT_LINE_PADDING, y + EVENT_SQUARE_WIDTH, p);
    }

    private void computeAllDayNeighbors() {
        int len = this.mSelectedEvents.size();
        if (len != 0 && this.mSelectedEvent == null) {
            for (int ii = 0; ii < len; ii++) {
                Event ev = this.mSelectedEvents.get(ii);
                ev.nextUp = null;
                ev.nextDown = null;
                ev.nextLeft = null;
                ev.nextRight = null;
            }
            int startPosition = -1;
            if (this.mPrevSelectedEvent != null && this.mPrevSelectedEvent.drawAsAllday()) {
                startPosition = this.mPrevSelectedEvent.getColumn();
            }
            int maxPosition = -1;
            Event startEvent = null;
            Event maxPositionEvent = null;
            for (int ii2 = 0; ii2 < len; ii2++) {
                Event ev2 = this.mSelectedEvents.get(ii2);
                int position = ev2.getColumn();
                if (position == startPosition) {
                    startEvent = ev2;
                } else if (position > maxPosition) {
                    maxPositionEvent = ev2;
                    maxPosition = position;
                }
                for (int jj = 0; jj < len; jj++) {
                    if (jj != ii2) {
                        Event neighbor = this.mSelectedEvents.get(jj);
                        int neighborPosition = neighbor.getColumn();
                        if (neighborPosition == position - 1) {
                            ev2.nextUp = neighbor;
                        } else if (neighborPosition == position + 1) {
                            ev2.nextDown = neighbor;
                        }
                    }
                }
            }
            if (startEvent != null) {
                setSelectedEvent(startEvent);
            } else {
                setSelectedEvent(maxPositionEvent);
            }
        }
    }

    private void drawEvents(int date, int dayIndex, int top, Canvas canvas, Paint p) {
        Paint eventTextPaint = this.mEventTextPaint;
        int left = computeDayLeftPosition(dayIndex) + 1;
        int cellWidth = (computeDayLeftPosition(dayIndex + 1) - left) + 1;
        int cellHeight = mCellHeight;
        Rect selectionArea = this.mSelectionRect;
        selectionArea.top = (this.mSelectionHour * (cellHeight + 1)) + top;
        selectionArea.bottom = selectionArea.top + cellHeight;
        selectionArea.left = left;
        selectionArea.right = selectionArea.left + cellWidth;
        ArrayList<Event> events = this.mEvents;
        int numEvents = events.size();
        EventGeometry geometry = this.mEventGeometry;
        int viewEndY = ((this.mViewStartY + this.mViewHeight) - DAY_HEADER_HEIGHT) - this.mAlldayHeight;
        int alpha = eventTextPaint.getAlpha();
        eventTextPaint.setAlpha(this.mEventsAlpha);
        for (int i = 0; i < numEvents; i++) {
            Event event = events.get(i);
            if (geometry.computeEventRect(date, left, top, cellWidth, event) && event.bottom >= this.mViewStartY && event.top <= viewEndY) {
                if (date == this.mSelectionDay && !this.mSelectionAllday && this.mComputeSelectedEvents && geometry.eventIntersectsSelection(event, selectionArea)) {
                    this.mSelectedEvents.add(event);
                }
                Rect r = drawEventRect(event, canvas, p, eventTextPaint, this.mViewStartY, viewEndY);
                setupTextRect(r);
                if (r.top <= viewEndY && r.bottom >= this.mViewStartY) {
                    StaticLayout layout = getEventLayout(this.mLayouts, i, event, eventTextPaint, r);
                    drawEventText(layout, r, canvas, this.mViewStartY + 4, ((this.mViewStartY + this.mViewHeight) - DAY_HEADER_HEIGHT) - this.mAlldayHeight, false);
                }
            }
        }
        eventTextPaint.setAlpha(alpha);
        if (date == this.mSelectionDay && !this.mSelectionAllday && isFocused() && this.mSelectionMode != 0) {
            computeNeighbors();
        }
    }

    private void computeNeighbors() {
        int prevTop;
        int prevBottom;
        int prevLeft;
        int prevRight;
        int len = this.mSelectedEvents.size();
        if (len != 0 && this.mSelectedEvent == null) {
            for (int ii = 0; ii < len; ii++) {
                Event ev = this.mSelectedEvents.get(ii);
                ev.nextUp = null;
                ev.nextDown = null;
                ev.nextLeft = null;
                ev.nextRight = null;
            }
            Event startEvent = this.mSelectedEvents.get(0);
            int startEventDistance1 = 100000;
            int startEventDistance2 = 100000;
            int prevLocation = 0;
            int prevCenter = 0;
            Rect box = getCurrentSelectionPosition();
            if (this.mPrevSelectedEvent != null) {
                prevTop = (int) this.mPrevSelectedEvent.top;
                prevBottom = (int) this.mPrevSelectedEvent.bottom;
                prevLeft = (int) this.mPrevSelectedEvent.left;
                prevRight = (int) this.mPrevSelectedEvent.right;
                if (prevTop >= this.mPrevBox.bottom || prevBottom <= this.mPrevBox.top || prevRight <= this.mPrevBox.left || prevLeft >= this.mPrevBox.right) {
                    this.mPrevSelectedEvent = null;
                    prevTop = this.mPrevBox.top;
                    prevBottom = this.mPrevBox.bottom;
                    prevLeft = this.mPrevBox.left;
                    prevRight = this.mPrevBox.right;
                } else {
                    if (prevTop < this.mPrevBox.top) {
                        prevTop = this.mPrevBox.top;
                    }
                    if (prevBottom > this.mPrevBox.bottom) {
                        prevBottom = this.mPrevBox.bottom;
                    }
                }
            } else {
                prevTop = this.mPrevBox.top;
                prevBottom = this.mPrevBox.bottom;
                prevLeft = this.mPrevBox.left;
                prevRight = this.mPrevBox.right;
            }
            if (prevLeft >= box.right) {
                prevLocation = 8;
                prevCenter = (prevTop + prevBottom) / 2;
            } else if (prevRight <= box.left) {
                prevLocation = 4;
                prevCenter = (prevTop + prevBottom) / 2;
            } else if (prevBottom <= box.top) {
                prevLocation = 1;
                prevCenter = (prevLeft + prevRight) / 2;
            } else if (prevTop >= box.bottom) {
                prevLocation = 2;
                prevCenter = (prevLeft + prevRight) / 2;
            }
            for (int ii2 = 0; ii2 < len; ii2++) {
                Event ev2 = this.mSelectedEvents.get(ii2);
                int startTime = ev2.startTime;
                int endTime = ev2.endTime;
                int left = (int) ev2.left;
                int right = (int) ev2.right;
                int top = (int) ev2.top;
                if (top < box.top) {
                    top = box.top;
                }
                int bottom = (int) ev2.bottom;
                if (bottom > box.bottom) {
                    bottom = box.bottom;
                }
                int upDistanceMin = 10000;
                int downDistanceMin = 10000;
                int leftDistanceMin = 10000;
                int rightDistanceMin = 10000;
                Event upEvent = null;
                Event downEvent = null;
                Event leftEvent = null;
                Event rightEvent = null;
                int distance1 = 0;
                int distance2 = 0;
                if (prevLocation == 1) {
                    if (left >= prevCenter) {
                        distance1 = left - prevCenter;
                    } else if (right <= prevCenter) {
                        distance1 = prevCenter - right;
                    }
                    distance2 = top - prevBottom;
                } else if (prevLocation == 2) {
                    if (left >= prevCenter) {
                        distance1 = left - prevCenter;
                    } else if (right <= prevCenter) {
                        distance1 = prevCenter - right;
                    }
                    distance2 = prevTop - bottom;
                } else if (prevLocation == 4) {
                    if (bottom <= prevCenter) {
                        distance1 = prevCenter - bottom;
                    } else if (top >= prevCenter) {
                        distance1 = top - prevCenter;
                    }
                    distance2 = left - prevRight;
                } else if (prevLocation == 8) {
                    if (bottom <= prevCenter) {
                        distance1 = prevCenter - bottom;
                    } else if (top >= prevCenter) {
                        distance1 = top - prevCenter;
                    }
                    distance2 = prevLeft - right;
                }
                if (distance1 < startEventDistance1 || (distance1 == startEventDistance1 && distance2 < startEventDistance2)) {
                    startEvent = ev2;
                    startEventDistance1 = distance1;
                    startEventDistance2 = distance2;
                }
                for (int jj = 0; jj < len; jj++) {
                    if (jj != ii2) {
                        Event neighbor = this.mSelectedEvents.get(jj);
                        int neighborLeft = (int) neighbor.left;
                        int neighborRight = (int) neighbor.right;
                        if (neighbor.endTime <= startTime) {
                            if (neighborLeft < right && neighborRight > left) {
                                int distance = startTime - neighbor.endTime;
                                if (distance < upDistanceMin) {
                                    upDistanceMin = distance;
                                    upEvent = neighbor;
                                } else if (distance == upDistanceMin) {
                                    int center = (left + right) / 2;
                                    int currentDistance = 0;
                                    int currentLeft = (int) upEvent.left;
                                    int currentRight = (int) upEvent.right;
                                    if (currentRight <= center) {
                                        currentDistance = center - currentRight;
                                    } else if (currentLeft >= center) {
                                        currentDistance = currentLeft - center;
                                    }
                                    int neighborDistance = 0;
                                    if (neighborRight <= center) {
                                        neighborDistance = center - neighborRight;
                                    } else if (neighborLeft >= center) {
                                        neighborDistance = neighborLeft - center;
                                    }
                                    if (neighborDistance < currentDistance) {
                                        upDistanceMin = distance;
                                        upEvent = neighbor;
                                    }
                                }
                            }
                        } else if (neighbor.startTime >= endTime && neighborLeft < right && neighborRight > left) {
                            int distance3 = neighbor.startTime - endTime;
                            if (distance3 < downDistanceMin) {
                                downDistanceMin = distance3;
                                downEvent = neighbor;
                            } else if (distance3 == downDistanceMin) {
                                int center2 = (left + right) / 2;
                                int currentDistance2 = 0;
                                int currentLeft2 = (int) downEvent.left;
                                int currentRight2 = (int) downEvent.right;
                                if (currentRight2 <= center2) {
                                    currentDistance2 = center2 - currentRight2;
                                } else if (currentLeft2 >= center2) {
                                    currentDistance2 = currentLeft2 - center2;
                                }
                                int neighborDistance2 = 0;
                                if (neighborRight <= center2) {
                                    neighborDistance2 = center2 - neighborRight;
                                } else if (neighborLeft >= center2) {
                                    neighborDistance2 = neighborLeft - center2;
                                }
                                if (neighborDistance2 < currentDistance2) {
                                    downDistanceMin = distance3;
                                    downEvent = neighbor;
                                }
                            }
                        }
                        if (neighborLeft >= right) {
                            int center3 = (top + bottom) / 2;
                            int distance4 = 0;
                            int neighborBottom = (int) neighbor.bottom;
                            int neighborTop = (int) neighbor.top;
                            if (neighborBottom <= center3) {
                                distance4 = center3 - neighborBottom;
                            } else if (neighborTop >= center3) {
                                distance4 = neighborTop - center3;
                            }
                            if (distance4 < rightDistanceMin) {
                                rightDistanceMin = distance4;
                                rightEvent = neighbor;
                            } else if (distance4 == rightDistanceMin) {
                                int neighborDistance3 = neighborLeft - right;
                                int currentDistance3 = ((int) rightEvent.left) - right;
                                if (neighborDistance3 < currentDistance3) {
                                    rightDistanceMin = distance4;
                                    rightEvent = neighbor;
                                }
                            }
                        } else if (neighborRight <= left) {
                            int center4 = (top + bottom) / 2;
                            int distance5 = 0;
                            int neighborBottom2 = (int) neighbor.bottom;
                            int neighborTop2 = (int) neighbor.top;
                            if (neighborBottom2 <= center4) {
                                distance5 = center4 - neighborBottom2;
                            } else if (neighborTop2 >= center4) {
                                distance5 = neighborTop2 - center4;
                            }
                            if (distance5 < leftDistanceMin) {
                                leftDistanceMin = distance5;
                                leftEvent = neighbor;
                            } else if (distance5 == leftDistanceMin) {
                                int neighborDistance4 = left - neighborRight;
                                int currentDistance4 = left - ((int) leftEvent.right);
                                if (neighborDistance4 < currentDistance4) {
                                    leftDistanceMin = distance5;
                                    leftEvent = neighbor;
                                }
                            }
                        }
                    }
                }
                ev2.nextUp = upEvent;
                ev2.nextDown = downEvent;
                ev2.nextLeft = leftEvent;
                ev2.nextRight = rightEvent;
            }
            setSelectedEvent(startEvent);
        }
    }

    private Rect drawEventRect(Event event, Canvas canvas, Paint p, Paint eventTextPaint, int visibleTop, int visibleBot) {
        int color;
        Rect r = this.mRect;
        r.top = Math.max(((int) event.top) + EVENT_RECT_TOP_MARGIN, visibleTop);
        r.bottom = Math.min(((int) event.bottom) - EVENT_RECT_BOTTOM_MARGIN, visibleBot);
        r.left = ((int) event.left) + EVENT_RECT_LEFT_MARGIN;
        r.right = (int) event.right;
        if (event == this.mClickedEvent) {
            color = mClickedColor;
        } else {
            color = event.color;
        }
        switch (event.selfAttendeeStatus) {
            case 2:
                if (event != this.mClickedEvent) {
                    color = Utils.getDeclinedColorFromColor(color);
                }
                p.setStyle(Paint.Style.FILL_AND_STROKE);
                break;
            case 3:
                if (event != this.mClickedEvent) {
                    p.setStyle(Paint.Style.STROKE);
                }
                break;
            default:
                p.setStyle(Paint.Style.FILL_AND_STROKE);
                break;
        }
        p.setAntiAlias(false);
        int floorHalfStroke = (int) Math.floor(EVENT_RECT_STROKE_WIDTH / 2.0f);
        int ceilHalfStroke = (int) Math.ceil(EVENT_RECT_STROKE_WIDTH / 2.0f);
        r.top = Math.max(((int) event.top) + EVENT_RECT_TOP_MARGIN + floorHalfStroke, visibleTop);
        r.bottom = Math.min((((int) event.bottom) - EVENT_RECT_BOTTOM_MARGIN) - ceilHalfStroke, visibleBot);
        r.left += floorHalfStroke;
        r.right -= ceilHalfStroke;
        p.setStrokeWidth(EVENT_RECT_STROKE_WIDTH);
        p.setColor(color);
        int alpha = p.getAlpha();
        p.setAlpha(this.mEventsAlpha);
        canvas.drawRect(r, p);
        p.setAlpha(alpha);
        p.setStyle(Paint.Style.FILL);
        if (this.mSelectedEvent == event && this.mClickedEvent != null) {
            boolean paintIt = false;
            int color2 = 0;
            if (this.mSelectionMode == 1 || this.mSelectionMode == 2) {
                this.mPrevSelectedEvent = event;
                color2 = mPressedColor;
                paintIt = true;
            }
            if (paintIt) {
                p.setColor(color2);
                canvas.drawRect(r, p);
            }
            p.setAntiAlias(true);
        }
        r.top = ((int) event.top) + EVENT_RECT_TOP_MARGIN;
        r.bottom = ((int) event.bottom) - EVENT_RECT_BOTTOM_MARGIN;
        r.left = ((int) event.left) + EVENT_RECT_LEFT_MARGIN;
        r.right = ((int) event.right) - EVENT_RECT_RIGHT_MARGIN;
        return r;
    }

    private String drawTextSanitizer(String string, int maxEventTextLen) {
        Matcher m = this.drawTextSanitizerFilter.matcher(string);
        String string2 = m.replaceAll(",");
        int len = string2.length();
        if (maxEventTextLen <= 0) {
            string2 = "";
        } else if (len > maxEventTextLen) {
            string2 = string2.substring(0, maxEventTextLen);
        }
        return string2.replace('\n', ' ');
    }

    private void drawEventText(StaticLayout eventLayout, Rect rect, Canvas canvas, int top, int bottom, boolean center) {
        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;
        if (eventLayout != null && width >= MIN_CELL_WIDTH_FOR_TEXT) {
            int totalLineHeight = 0;
            int lineCount = eventLayout.getLineCount();
            for (int i = 0; i < lineCount; i++) {
                int lineBottom = eventLayout.getLineBottom(i);
                if (lineBottom > height) {
                    break;
                }
                totalLineHeight = lineBottom;
            }
            if (totalLineHeight != 0 && rect.top <= bottom && rect.top + totalLineHeight + 2 >= top) {
                canvas.save();
                int padding = center ? ((rect.bottom - rect.top) - totalLineHeight) / 2 : 0;
                canvas.translate(rect.left, rect.top + padding);
                rect.left = 0;
                rect.right = width;
                rect.top = 0;
                rect.bottom = totalLineHeight;
                canvas.clipRect(rect);
                eventLayout.draw(canvas);
                canvas.restore();
            }
        }
    }

    private void updateEventDetails() {
        int flags;
        if (this.mSelectedEvent == null || this.mSelectionMode == 0 || this.mSelectionMode == 3) {
            this.mPopup.dismiss();
            return;
        }
        if (this.mLastPopupEventID != this.mSelectedEvent.id) {
            this.mLastPopupEventID = this.mSelectedEvent.id;
            this.mHandler.removeCallbacks(this.mDismissPopup);
            Event event = this.mSelectedEvent;
            TextView titleView = (TextView) this.mPopupView.findViewById(R.id.event_title);
            titleView.setText(event.title);
            ImageView imageView = (ImageView) this.mPopupView.findViewById(R.id.reminder_icon);
            imageView.setVisibility(event.hasAlarm ? 0 : 8);
            ImageView imageView2 = (ImageView) this.mPopupView.findViewById(R.id.repeat_icon);
            imageView2.setVisibility(event.isRepeating ? 0 : 8);
            if (event.allDay) {
                flags = 532498;
            } else {
                flags = 529427;
            }
            if (DateFormat.is24HourFormat(this.mContext)) {
                flags |= 128;
            }
            String timeRange = Utils.formatDateRange(this.mContext, event.startMillis, event.endMillis, flags);
            TextView timeView = (TextView) this.mPopupView.findViewById(R.id.time);
            timeView.setText(timeRange);
            TextView whereView = (TextView) this.mPopupView.findViewById(R.id.where);
            boolean empty = TextUtils.isEmpty(event.location);
            whereView.setVisibility(empty ? 8 : 0);
            if (!empty) {
                whereView.setText(event.location);
            }
            this.mPopup.showAtLocation(this, 83, this.mHoursWidth, 5);
            this.mHandler.postDelayed(this.mDismissPopup, 3000L);
        }
    }

    private void doDown(MotionEvent ev) {
        this.mTouchMode = 1;
        this.mViewStartX = 0;
        this.mOnFlingCalled = false;
        this.mHandler.removeCallbacks(this.mContinueScroll);
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        Event oldSelectedEvent = this.mSelectedEvent;
        int oldSelectionDay = this.mSelectionDay;
        int oldSelectionHour = this.mSelectionHour;
        if (setSelectionFromPosition(x, y, false)) {
            boolean pressedSelected = this.mSelectionMode != 0 && oldSelectionDay == this.mSelectionDay && oldSelectionHour == this.mSelectionHour;
            if (!pressedSelected && this.mSelectedEvent != null) {
                this.mSavedClickedEvent = this.mSelectedEvent;
                this.mDownTouchTime = System.currentTimeMillis();
                postDelayed(this.mSetClick, mOnDownDelay);
            } else {
                eventClickCleanup();
            }
        }
        this.mSelectedEvent = oldSelectedEvent;
        this.mSelectionDay = oldSelectionDay;
        this.mSelectionHour = oldSelectionHour;
        invalidate();
    }

    private void doExpandAllDayClick() {
        mShowAllAllDayEvents = !mShowAllAllDayEvents;
        ObjectAnimator.setFrameDelay(0L);
        if (this.mAnimateDayHeight == 0) {
            this.mAnimateDayHeight = mShowAllAllDayEvents ? this.mAlldayHeight - ((int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT) : this.mAlldayHeight;
        }
        this.mCancellingAnimations = true;
        if (this.mAlldayAnimator != null) {
            this.mAlldayAnimator.cancel();
        }
        if (this.mAlldayEventAnimator != null) {
            this.mAlldayEventAnimator.cancel();
        }
        if (this.mMoreAlldayEventsAnimator != null) {
            this.mMoreAlldayEventsAnimator.cancel();
        }
        this.mCancellingAnimations = false;
        this.mAlldayAnimator = getAllDayAnimator();
        this.mAlldayEventAnimator = getAllDayEventAnimator();
        int[] iArr = new int[2];
        iArr[0] = mShowAllAllDayEvents ? 76 : 0;
        iArr[1] = mShowAllAllDayEvents ? 0 : 76;
        this.mMoreAlldayEventsAnimator = ObjectAnimator.ofInt(this, "moreAllDayEventsTextAlpha", iArr);
        this.mAlldayAnimator.setStartDelay(mShowAllAllDayEvents ? 200L : 0L);
        this.mAlldayAnimator.start();
        this.mMoreAlldayEventsAnimator.setStartDelay(mShowAllAllDayEvents ? 0L : 400L);
        this.mMoreAlldayEventsAnimator.setDuration(200L);
        this.mMoreAlldayEventsAnimator.start();
        if (this.mAlldayEventAnimator != null) {
            this.mAlldayEventAnimator.setStartDelay(mShowAllAllDayEvents ? 200L : 0L);
            this.mAlldayEventAnimator.start();
        }
    }

    public void initAllDayHeights() {
        if (this.mMaxAlldayEvents > this.mMaxUnexpandedAlldayEventCount) {
            if (mShowAllAllDayEvents) {
                int maxADHeight = (this.mViewHeight - DAY_HEADER_HEIGHT) - MIN_HOURS_HEIGHT;
                this.mAnimateDayEventHeight = Math.min(maxADHeight, (int) (this.mMaxAlldayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT)) / this.mMaxAlldayEvents;
            } else {
                this.mAnimateDayEventHeight = (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;
            }
        }
    }

    private ObjectAnimator getAllDayEventAnimator() {
        int maxADHeight = (this.mViewHeight - DAY_HEADER_HEIGHT) - MIN_HOURS_HEIGHT;
        int fitHeight = Math.min(maxADHeight, (int) (this.mMaxAlldayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT)) / this.mMaxAlldayEvents;
        int currentHeight = this.mAnimateDayEventHeight;
        int desiredHeight = mShowAllAllDayEvents ? fitHeight : (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;
        if (currentHeight == desiredHeight) {
            return null;
        }
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "animateDayEventHeight", currentHeight, desiredHeight);
        animator.setDuration(400L);
        return animator;
    }

    private ObjectAnimator getAllDayAnimator() {
        int maxADHeight = (this.mViewHeight - DAY_HEADER_HEIGHT) - MIN_HOURS_HEIGHT;
        int maxADHeight2 = Math.min(maxADHeight, (int) (this.mMaxAlldayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT));
        int currentHeight = this.mAnimateDayHeight != 0 ? this.mAnimateDayHeight : this.mAlldayHeight;
        int desiredHeight = mShowAllAllDayEvents ? maxADHeight2 : (int) ((MAX_UNEXPANDED_ALLDAY_HEIGHT - MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT) - 1.0f);
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "animateDayHeight", currentHeight, desiredHeight);
        animator.setDuration(400L);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!DayView.this.mCancellingAnimations) {
                    DayView.this.mAnimateDayHeight = 0;
                    boolean unused = DayView.mUseExpandIcon = DayView.mShowAllAllDayEvents ? false : true;
                }
                DayView.this.mRemeasure = true;
                DayView.this.invalidate();
            }
        });
        return animator;
    }

    public void setMoreAllDayEventsTextAlpha(int alpha) {
        mMoreAlldayEventsTextAlpha = alpha;
        invalidate();
    }

    public void setAnimateDayHeight(int height) {
        this.mAnimateDayHeight = height;
        this.mRemeasure = true;
        invalidate();
    }

    public void setAnimateDayEventHeight(int height) {
        this.mAnimateDayEventHeight = height;
        this.mRemeasure = true;
        invalidate();
    }

    private void doSingleTapUp(MotionEvent ev) {
        if (this.mHandleActionUp && !this.mScrolling) {
            int x = (int) ev.getX();
            int y = (int) ev.getY();
            int selectedDay = this.mSelectionDay;
            int selectedHour = this.mSelectionHour;
            if (this.mMaxAlldayEvents > this.mMaxUnexpandedAlldayEventCount) {
                int bottom = this.mFirstCell;
                if ((x < this.mHoursWidth && y > DAY_HEADER_HEIGHT && y < DAY_HEADER_HEIGHT + this.mAlldayHeight) || (!mShowAllAllDayEvents && this.mAnimateDayHeight == 0 && y < bottom && y >= bottom - MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT)) {
                    doExpandAllDayClick();
                    return;
                }
            }
            boolean validPosition = setSelectionFromPosition(x, y, false);
            if (!validPosition) {
                if (y < DAY_HEADER_HEIGHT) {
                    Time selectedTime = new Time(this.mBaseDate);
                    selectedTime.setJulianDay(this.mSelectionDay);
                    selectedTime.hour = this.mSelectionHour;
                    selectedTime.normalize(true);
                    this.mController.sendEvent(this, 32L, null, null, selectedTime, -1L, 2, 1L, null, null);
                    return;
                }
                return;
            }
            boolean hasSelection = this.mSelectionMode != 0;
            boolean pressedSelected = (hasSelection || this.mTouchExplorationEnabled) && selectedDay == this.mSelectionDay && selectedHour == this.mSelectionHour;
            if (pressedSelected && this.mSavedClickedEvent == null) {
                long extraLong = 0;
                if (this.mSelectionAllday) {
                    extraLong = 16;
                }
                this.mSelectionMode = 2;
                this.mController.sendEventRelatedEventWithExtra(this, 1L, -1L, getSelectedTimeInMillis(), 0L, (int) ev.getRawX(), (int) ev.getRawY(), extraLong, -1L);
            } else if (this.mSelectedEvent != null) {
                if (this.mIsAccessibilityEnabled) {
                    this.mAccessibilityMgr.interrupt();
                }
                this.mSelectionMode = 0;
                int yLocation = (int) ((this.mSelectedEvent.top + this.mSelectedEvent.bottom) / 2.0f);
                if (!this.mSelectedEvent.allDay) {
                    yLocation += this.mFirstCell - this.mViewStartY;
                }
                this.mClickedYLocation = yLocation;
                long clearDelay = ((long) (mOnDownDelay + 50)) - (System.currentTimeMillis() - this.mDownTouchTime);
                if (clearDelay > 0) {
                    postDelayed(this.mClearClick, clearDelay);
                } else {
                    post(this.mClearClick);
                }
            } else {
                Time startTime = new Time(this.mBaseDate);
                startTime.setJulianDay(this.mSelectionDay);
                startTime.hour = this.mSelectionHour;
                startTime.normalize(true);
                Time endTime = new Time(startTime);
                endTime.hour++;
                this.mSelectionMode = 2;
                this.mController.sendEvent(this, 32L, startTime, endTime, -1L, 0, 2L, null, null);
            }
            invalidate();
        }
    }

    private void doLongPress(MotionEvent ev) {
        eventClickCleanup();
        if (!this.mScrolling && this.mStartingSpanY == 0.0f) {
            int x = (int) ev.getX();
            int y = (int) ev.getY();
            boolean validPosition = setSelectionFromPosition(x, y, false);
            if (validPosition) {
                this.mSelectionMode = 3;
                invalidate();
                performLongClick();
            }
        }
    }

    private void doScroll(MotionEvent e1, MotionEvent e2, float deltaX, float deltaY) {
        cancelAnimation();
        if (this.mStartingScroll) {
            this.mInitialScrollX = 0.0f;
            this.mInitialScrollY = 0.0f;
            this.mStartingScroll = false;
        }
        this.mInitialScrollX += deltaX;
        this.mInitialScrollY += deltaY;
        int distanceX = (int) this.mInitialScrollX;
        int distanceY = (int) this.mInitialScrollY;
        float focusY = getAverageY(e2);
        if (this.mRecalCenterHour) {
            this.mGestureCenterHour = (((this.mViewStartY + focusY) - DAY_HEADER_HEIGHT) - this.mAlldayHeight) / (mCellHeight + 1);
            this.mRecalCenterHour = false;
        }
        if (this.mTouchMode == 1) {
            int absDistanceX = Math.abs(distanceX);
            int absDistanceY = Math.abs(distanceY);
            this.mScrollStartY = this.mViewStartY;
            this.mPreviousDirection = 0;
            if (absDistanceX > absDistanceY) {
                int slopFactor = this.mScaleGestureDetector.isInProgress() ? 20 : 2;
                if (absDistanceX > mScaledPagingTouchSlop * slopFactor) {
                    this.mTouchMode = 64;
                    this.mViewStartX = distanceX;
                    initNextView(-this.mViewStartX);
                }
            } else {
                this.mTouchMode = 32;
            }
        } else if ((this.mTouchMode & 64) != 0) {
            this.mViewStartX = distanceX;
            if (distanceX != 0) {
                int direction = distanceX > 0 ? 1 : -1;
                if (direction != this.mPreviousDirection) {
                    initNextView(-this.mViewStartX);
                    this.mPreviousDirection = direction;
                }
            }
        }
        if ((this.mTouchMode & 32) != 0) {
            this.mViewStartY = (int) (((this.mGestureCenterHour * (mCellHeight + 1)) - focusY) + DAY_HEADER_HEIGHT + this.mAlldayHeight);
            int pulledToY = (int) (this.mScrollStartY + deltaY);
            if (pulledToY < 0) {
                this.mEdgeEffectTop.onPull(deltaY / this.mViewHeight);
                if (!this.mEdgeEffectBottom.isFinished()) {
                    this.mEdgeEffectBottom.onRelease();
                }
            } else if (pulledToY > this.mMaxViewStartY) {
                this.mEdgeEffectBottom.onPull(deltaY / this.mViewHeight);
                if (!this.mEdgeEffectTop.isFinished()) {
                    this.mEdgeEffectTop.onRelease();
                }
            }
            if (this.mViewStartY < 0) {
                this.mViewStartY = 0;
                this.mRecalCenterHour = true;
            } else if (this.mViewStartY > this.mMaxViewStartY) {
                this.mViewStartY = this.mMaxViewStartY;
                this.mRecalCenterHour = true;
            }
            if (this.mRecalCenterHour) {
                this.mGestureCenterHour = (((this.mViewStartY + focusY) - DAY_HEADER_HEIGHT) - this.mAlldayHeight) / (mCellHeight + 1);
                this.mRecalCenterHour = false;
            }
            computeFirstHour();
        }
        this.mScrolling = true;
        this.mSelectionMode = 0;
        invalidate();
    }

    private float getAverageY(MotionEvent me) {
        int count = me.getPointerCount();
        float focusY = 0.0f;
        for (int i = 0; i < count; i++) {
            focusY += me.getY(i);
        }
        return focusY / count;
    }

    private void cancelAnimation() {
        Animation in = this.mViewSwitcher.getInAnimation();
        if (in != null) {
            in.scaleCurrentDuration(0.0f);
        }
        Animation out = this.mViewSwitcher.getOutAnimation();
        if (out != null) {
            out.scaleCurrentDuration(0.0f);
        }
    }

    private void doFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        cancelAnimation();
        this.mSelectionMode = 0;
        eventClickCleanup();
        this.mOnFlingCalled = true;
        if ((this.mTouchMode & 64) != 0) {
            this.mTouchMode = 0;
            if (DEBUG) {
                Log.d(TAG, "doFling: velocityX " + velocityX);
            }
            int deltaX = ((int) e2.getX()) - ((int) e1.getX());
            switchViews(deltaX < 0, this.mViewStartX, this.mViewWidth, velocityX);
            this.mViewStartX = 0;
            return;
        }
        if ((this.mTouchMode & 32) == 0) {
            if (DEBUG) {
                Log.d(TAG, "doFling: no fling");
                return;
            }
            return;
        }
        this.mTouchMode = 0;
        this.mViewStartX = 0;
        if (DEBUG) {
            Log.d(TAG, "doFling: mViewStartY" + this.mViewStartY + " velocityY " + velocityY);
        }
        this.mScrolling = true;
        this.mScroller.fling(0, this.mViewStartY, 0, (int) (-velocityY), 0, 0, 0, this.mMaxViewStartY, this.OVERFLING_DISTANCE, this.OVERFLING_DISTANCE);
        if (velocityY > 0.0f && this.mViewStartY != 0) {
            this.mCallEdgeEffectOnAbsorb = true;
        } else if (velocityY < 0.0f && this.mViewStartY != this.mMaxViewStartY) {
            this.mCallEdgeEffectOnAbsorb = true;
        }
        this.mHandler.post(this.mContinueScroll);
    }

    private boolean initNextView(int deltaX) {
        boolean switchForward;
        DayView view = (DayView) this.mViewSwitcher.getNextView();
        Time date = view.mBaseDate;
        date.set(this.mBaseDate);
        if (deltaX > 0) {
            date.monthDay -= this.mNumDays;
            view.setSelectedDay(this.mSelectionDay - this.mNumDays);
            switchForward = false;
        } else {
            date.monthDay += this.mNumDays;
            view.setSelectedDay(this.mSelectionDay + this.mNumDays);
            switchForward = true;
        }
        date.normalize(true);
        initView(view);
        view.layout(getLeft(), getTop(), getRight(), getBottom());
        view.reloadEvents();
        return switchForward;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        this.mHandleActionUp = false;
        float gestureCenterInPixels = (detector.getFocusY() - DAY_HEADER_HEIGHT) - this.mAlldayHeight;
        this.mGestureCenterHour = (this.mViewStartY + gestureCenterInPixels) / (mCellHeight + 1);
        this.mStartingSpanY = Math.max(MIN_Y_SPAN, Math.abs(detector.getCurrentSpanY()));
        this.mCellHeightBeforeScaleGesture = mCellHeight;
        if (DEBUG_SCALING) {
            float ViewStartHour = this.mViewStartY / (mCellHeight + 1);
            Log.d(TAG, "onScaleBegin: mGestureCenterHour:" + this.mGestureCenterHour + "\tViewStartHour: " + ViewStartHour + "\tmViewStartY:" + this.mViewStartY + "\tmCellHeight:" + mCellHeight + " SpanY:" + detector.getCurrentSpanY());
            return true;
        }
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float spanY = Math.max(MIN_Y_SPAN, Math.abs(detector.getCurrentSpanY()));
        mCellHeight = (int) ((this.mCellHeightBeforeScaleGesture * spanY) / this.mStartingSpanY);
        if (mCellHeight < mMinCellHeight) {
            this.mStartingSpanY = spanY;
            mCellHeight = mMinCellHeight;
            this.mCellHeightBeforeScaleGesture = mMinCellHeight;
        } else if (mCellHeight > MAX_CELL_HEIGHT) {
            this.mStartingSpanY = spanY;
            mCellHeight = MAX_CELL_HEIGHT;
            this.mCellHeightBeforeScaleGesture = MAX_CELL_HEIGHT;
        }
        int gestureCenterInPixels = (((int) detector.getFocusY()) - DAY_HEADER_HEIGHT) - this.mAlldayHeight;
        this.mViewStartY = ((int) (this.mGestureCenterHour * (mCellHeight + 1))) - gestureCenterInPixels;
        this.mMaxViewStartY = (((mCellHeight + 1) * 24) + 1) - this.mGridAreaHeight;
        if (DEBUG_SCALING) {
            float ViewStartHour = this.mViewStartY / (mCellHeight + 1);
            Log.d(TAG, "onScale: mGestureCenterHour:" + this.mGestureCenterHour + "\tViewStartHour: " + ViewStartHour + "\tmViewStartY:" + this.mViewStartY + "\tmCellHeight:" + mCellHeight + " SpanY:" + detector.getCurrentSpanY());
        }
        if (this.mViewStartY < 0) {
            this.mViewStartY = 0;
            this.mGestureCenterHour = (this.mViewStartY + gestureCenterInPixels) / (mCellHeight + 1);
        } else if (this.mViewStartY > this.mMaxViewStartY) {
            this.mViewStartY = this.mMaxViewStartY;
            this.mGestureCenterHour = (this.mViewStartY + gestureCenterInPixels) / (mCellHeight + 1);
        }
        computeFirstHour();
        this.mRemeasure = true;
        invalidate();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        this.mScrollStartY = this.mViewStartY;
        this.mInitialScrollY = 0.0f;
        this.mInitialScrollX = 0.0f;
        this.mStartingSpanY = 0.0f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (DEBUG) {
            Log.e(TAG, "" + action + " ev.getPointerCount() = " + ev.getPointerCount());
        }
        if (ev.getActionMasked() == 0 || ev.getActionMasked() == 1 || ev.getActionMasked() == 6 || ev.getActionMasked() == 5) {
            this.mRecalCenterHour = true;
        }
        if ((this.mTouchMode & 64) == 0) {
            this.mScaleGestureDetector.onTouchEvent(ev);
        }
        switch (action) {
            case 0:
                this.mStartingScroll = true;
                if (DEBUG) {
                    Log.e(TAG, "ACTION_DOWN ev.getDownTime = " + ev.getDownTime() + " Cnt=" + ev.getPointerCount());
                }
                int bottom = this.mAlldayHeight + DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN;
                if (ev.getY() < bottom) {
                    this.mTouchStartedInAlldayArea = true;
                } else {
                    this.mTouchStartedInAlldayArea = false;
                }
                this.mHandleActionUp = true;
                this.mGestureDetector.onTouchEvent(ev);
                break;
            case 1:
                if (DEBUG) {
                    Log.e(TAG, "ACTION_UP Cnt=" + ev.getPointerCount() + this.mHandleActionUp);
                }
                this.mEdgeEffectTop.onRelease();
                this.mEdgeEffectBottom.onRelease();
                this.mStartingScroll = false;
                this.mGestureDetector.onTouchEvent(ev);
                if (!this.mHandleActionUp) {
                    this.mHandleActionUp = true;
                    this.mViewStartX = 0;
                    invalidate();
                } else if (!this.mOnFlingCalled) {
                    if (this.mScrolling) {
                        this.mScrolling = false;
                        resetSelectedHour();
                        invalidate();
                    }
                    if ((this.mTouchMode & 64) != 0) {
                        this.mTouchMode = 0;
                        if (Math.abs(this.mViewStartX) > mHorizontalSnapBackThreshold) {
                            if (DEBUG) {
                                Log.d(TAG, "- horizontal scroll: switch views");
                            }
                            switchViews(this.mViewStartX > 0, this.mViewStartX, this.mViewWidth, 0.0f);
                            this.mViewStartX = 0;
                        } else {
                            if (DEBUG) {
                                Log.d(TAG, "- horizontal scroll: snap back");
                            }
                            recalc();
                            invalidate();
                            this.mViewStartX = 0;
                        }
                    }
                }
                break;
            case 2:
                if (DEBUG) {
                    Log.e(TAG, "ACTION_MOVE Cnt=" + ev.getPointerCount() + this);
                }
                this.mGestureDetector.onTouchEvent(ev);
                break;
            case 3:
                if (DEBUG) {
                    Log.e(TAG, "ACTION_CANCEL");
                }
                this.mGestureDetector.onTouchEvent(ev);
                this.mScrolling = false;
                resetSelectedHour();
                break;
            default:
                if (DEBUG) {
                    Log.e(TAG, "Not MotionEvent " + ev.toString());
                }
                if (!this.mGestureDetector.onTouchEvent(ev)) {
                    break;
                }
                break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        if (this.mSelectionMode != 3) {
            this.mSelectionMode = 3;
            invalidate();
        }
        long startMillis = getSelectedTimeInMillis();
        String title = Utils.formatDateRange(this.mContext, startMillis, startMillis, 5123);
        menu.setHeaderTitle(title);
        int numSelectedEvents = this.mSelectedEvents.size();
        if (this.mNumDays == 1) {
            if (numSelectedEvents >= 1) {
                MenuItem item = menu.add(0, 5, 0, R.string.event_view);
                item.setOnMenuItemClickListener(this.mContextMenuHandler);
                item.setIcon(android.R.drawable.ic_menu_info_details);
                int accessLevel = getEventAccessLevel(this.mContext, this.mSelectedEvent);
                if (accessLevel == 2) {
                    MenuItem item2 = menu.add(0, 7, 0, R.string.event_edit);
                    item2.setOnMenuItemClickListener(this.mContextMenuHandler);
                    item2.setIcon(android.R.drawable.ic_menu_edit);
                    item2.setAlphabeticShortcut('e');
                }
                if (accessLevel >= 1) {
                    MenuItem item3 = menu.add(0, 8, 0, R.string.event_delete);
                    item3.setOnMenuItemClickListener(this.mContextMenuHandler);
                    item3.setIcon(android.R.drawable.ic_menu_delete);
                }
                MenuItem item4 = menu.add(0, 6, 0, R.string.event_create);
                item4.setOnMenuItemClickListener(this.mContextMenuHandler);
                item4.setIcon(android.R.drawable.ic_menu_add);
                item4.setAlphabeticShortcut('n');
            } else {
                MenuItem item5 = menu.add(0, 6, 0, R.string.event_create);
                item5.setOnMenuItemClickListener(this.mContextMenuHandler);
                item5.setIcon(android.R.drawable.ic_menu_add);
                item5.setAlphabeticShortcut('n');
            }
        } else {
            if (numSelectedEvents >= 1) {
                MenuItem item6 = menu.add(0, 5, 0, R.string.event_view);
                item6.setOnMenuItemClickListener(this.mContextMenuHandler);
                item6.setIcon(android.R.drawable.ic_menu_info_details);
                int accessLevel2 = getEventAccessLevel(this.mContext, this.mSelectedEvent);
                if (accessLevel2 == 2) {
                    MenuItem item7 = menu.add(0, 7, 0, R.string.event_edit);
                    item7.setOnMenuItemClickListener(this.mContextMenuHandler);
                    item7.setIcon(android.R.drawable.ic_menu_edit);
                    item7.setAlphabeticShortcut('e');
                }
                if (accessLevel2 >= 1) {
                    MenuItem item8 = menu.add(0, 8, 0, R.string.event_delete);
                    item8.setOnMenuItemClickListener(this.mContextMenuHandler);
                    item8.setIcon(android.R.drawable.ic_menu_delete);
                }
            }
            MenuItem item9 = menu.add(0, 6, 0, R.string.event_create);
            item9.setOnMenuItemClickListener(this.mContextMenuHandler);
            item9.setIcon(android.R.drawable.ic_menu_add);
            item9.setAlphabeticShortcut('n');
            MenuItem item10 = menu.add(0, 3, 0, R.string.show_day_view);
            item10.setOnMenuItemClickListener(this.mContextMenuHandler);
            item10.setIcon(android.R.drawable.ic_menu_day);
            item10.setAlphabeticShortcut('d');
        }
        this.mPopup.dismiss();
    }

    private class ContextMenuHandler implements MenuItem.OnMenuItemClickListener {
        private ContextMenuHandler() {
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case 2:
                    DayView.this.mController.sendEvent(this, 32L, DayView.this.getSelectedTime(), null, -1L, 1);
                    return true;
                case 3:
                    DayView.this.mController.sendEvent(this, 32L, DayView.this.getSelectedTime(), null, -1L, 2);
                    return true;
                case 4:
                default:
                    return false;
                case 5:
                    if (DayView.this.mSelectedEvent != null) {
                        DayView.this.mController.sendEventRelatedEvent(this, 4L, DayView.this.mSelectedEvent.id, DayView.this.mSelectedEvent.startMillis, DayView.this.mSelectedEvent.endMillis, 0, 0, -1L);
                    }
                    return true;
                case 6:
                    long startMillis = DayView.this.getSelectedTimeInMillis();
                    long endMillis = startMillis + 3600000;
                    DayView.this.mController.sendEventRelatedEvent(this, 1L, -1L, startMillis, endMillis, 0, 0, -1L);
                    return true;
                case 7:
                    if (DayView.this.mSelectedEvent != null) {
                        DayView.this.mController.sendEventRelatedEvent(this, 8L, DayView.this.mSelectedEvent.id, DayView.this.mSelectedEvent.startMillis, DayView.this.mSelectedEvent.endMillis, 0, 0, -1L);
                    }
                    return true;
                case 8:
                    if (DayView.this.mSelectedEvent != null) {
                        Event selectedEvent = DayView.this.mSelectedEvent;
                        long begin = selectedEvent.startMillis;
                        long end = selectedEvent.endMillis;
                        long id = selectedEvent.id;
                        DayView.this.mController.sendEventRelatedEvent(this, 16L, id, begin, end, 0, 0, -1L);
                    }
                    return true;
            }
        }
    }

    private static int getEventAccessLevel(Context context, Event e) {
        ContentResolver cr = context.getContentResolver();
        int accessLevel = 0;
        Cursor cursor = cr.query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, e.id), new String[]{"calendar_id"}, null, null, null);
        if (cursor == null) {
            return 0;
        }
        if (cursor.getCount() == 0) {
            cursor.close();
            return 0;
        }
        cursor.moveToFirst();
        long calId = cursor.getLong(0);
        cursor.close();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String where = String.format("_id=%d", Long.valueOf(calId));
        Cursor cursor2 = cr.query(uri, CALENDARS_PROJECTION, where, null, null);
        String calendarOwnerAccount = null;
        if (cursor2 != null) {
            cursor2.moveToFirst();
            accessLevel = cursor2.getInt(1);
            calendarOwnerAccount = cursor2.getString(2);
            cursor2.close();
        }
        if (accessLevel < 500) {
            return 0;
        }
        if (e.guestsCanModify) {
            return 2;
        }
        if (!TextUtils.isEmpty(calendarOwnerAccount) && calendarOwnerAccount.equalsIgnoreCase(e.organizer)) {
            return 2;
        }
        return 1;
    }

    private boolean setSelectionFromPosition(int x, int y, boolean keepOldSelection) {
        Event savedEvent = null;
        int savedDay = 0;
        int savedHour = 0;
        boolean savedAllDay = false;
        if (keepOldSelection) {
            savedEvent = this.mSelectedEvent;
            savedDay = this.mSelectionDay;
            savedHour = this.mSelectionHour;
            savedAllDay = this.mSelectionAllday;
        }
        if (x < this.mHoursWidth) {
            x = this.mHoursWidth;
        }
        int day = (x - this.mHoursWidth) / (this.mCellWidth + 1);
        if (day >= this.mNumDays) {
            day = this.mNumDays - 1;
        }
        setSelectedDay(day + this.mFirstJulianDay);
        if (y < DAY_HEADER_HEIGHT) {
            sendAccessibilityEventAsNeeded(false);
            return false;
        }
        setSelectedHour(this.mFirstHour);
        if (y < this.mFirstCell) {
            this.mSelectionAllday = true;
        } else {
            int adjustedY = y - this.mFirstCell;
            if (adjustedY < this.mFirstHourOffset) {
                setSelectedHour(this.mSelectionHour - 1);
            } else {
                setSelectedHour(this.mSelectionHour + ((adjustedY - this.mFirstHourOffset) / (mCellHeight + 1)));
            }
            this.mSelectionAllday = false;
        }
        findSelectedEvent(x, y);
        sendAccessibilityEventAsNeeded(true);
        if (keepOldSelection) {
            this.mSelectedEvent = savedEvent;
            this.mSelectionDay = savedDay;
            this.mSelectionHour = savedHour;
            this.mSelectionAllday = savedAllDay;
        }
        return true;
    }

    private void findSelectedEvent(int x, int y) {
        int endHour;
        float yDistance;
        int date = this.mSelectionDay;
        int cellWidth = this.mCellWidth;
        ArrayList<Event> events = this.mEvents;
        int numEvents = events.size();
        int left = computeDayLeftPosition(this.mSelectionDay - this.mFirstJulianDay);
        setSelectedEvent(null);
        this.mSelectedEvents.clear();
        if (this.mSelectionAllday) {
            float minYdistance = 10000.0f;
            Event closestEvent = null;
            float drawHeight = this.mAlldayHeight;
            int yOffset = DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN;
            int maxUnexpandedColumn = this.mMaxUnexpandedAlldayEventCount;
            if (this.mMaxAlldayEvents > this.mMaxUnexpandedAlldayEventCount) {
                maxUnexpandedColumn--;
            }
            ArrayList<Event> events2 = this.mAllDayEvents;
            int numEvents2 = events2.size();
            int i = 0;
            while (true) {
                if (i >= numEvents2) {
                    break;
                }
                Event event = events2.get(i);
                if (event.drawAsAllday() && ((mShowAllAllDayEvents || event.getColumn() < maxUnexpandedColumn) && event.startDay <= this.mSelectionDay && event.endDay >= this.mSelectionDay)) {
                    float numRectangles = mShowAllAllDayEvents ? this.mMaxAlldayEvents : this.mMaxUnexpandedAlldayEventCount;
                    float height = drawHeight / numRectangles;
                    if (height > MAX_HEIGHT_OF_ONE_ALLDAY_EVENT) {
                        height = MAX_HEIGHT_OF_ONE_ALLDAY_EVENT;
                    }
                    float eventTop = yOffset + (event.getColumn() * height);
                    float eventBottom = eventTop + height;
                    if (eventTop < y && eventBottom > y) {
                        this.mSelectedEvents.add(event);
                        closestEvent = event;
                        break;
                    }
                    if (eventTop >= y) {
                        yDistance = eventTop - y;
                    } else {
                        yDistance = y - eventBottom;
                    }
                    if (yDistance < minYdistance) {
                        minYdistance = yDistance;
                        closestEvent = event;
                    }
                }
                i++;
            }
            setSelectedEvent(closestEvent);
            return;
        }
        int y2 = y + (this.mViewStartY - this.mFirstCell);
        Rect region = this.mRect;
        region.left = x - 10;
        region.right = x + 10;
        region.top = y2 - 10;
        region.bottom = y2 + 10;
        EventGeometry geometry = this.mEventGeometry;
        for (int i2 = 0; i2 < numEvents; i2++) {
            Event event2 = events.get(i2);
            if (geometry.computeEventRect(date, left, 0, cellWidth, event2) && geometry.eventIntersectsSelection(event2, region)) {
                this.mSelectedEvents.add(event2);
            }
        }
        if (this.mSelectedEvents.size() > 0) {
            int len = this.mSelectedEvents.size();
            Event closestEvent2 = null;
            float minDist = this.mViewWidth + this.mViewHeight;
            for (int index = 0; index < len; index++) {
                Event ev = this.mSelectedEvents.get(index);
                float dist = geometry.pointToEvent(x, y2, ev);
                if (dist < minDist) {
                    minDist = dist;
                    closestEvent2 = ev;
                }
            }
            setSelectedEvent(closestEvent2);
            int startDay = this.mSelectedEvent.startDay;
            int endDay = this.mSelectedEvent.endDay;
            if (this.mSelectionDay < startDay) {
                setSelectedDay(startDay);
            } else if (this.mSelectionDay > endDay) {
                setSelectedDay(endDay);
            }
            int startHour = this.mSelectedEvent.startTime / 60;
            if (this.mSelectedEvent.startTime < this.mSelectedEvent.endTime) {
                endHour = (this.mSelectedEvent.endTime - 1) / 60;
            } else {
                endHour = this.mSelectedEvent.endTime / 60;
            }
            if (this.mSelectionHour < startHour && this.mSelectionDay == startDay) {
                setSelectedHour(startHour);
            } else if (this.mSelectionHour > endHour && this.mSelectionDay == endDay) {
                setSelectedHour(endHour);
            }
        }
    }

    private class ContinueScroll implements Runnable {
        private ContinueScroll() {
        }

        @Override
        public void run() {
            DayView.this.mScrolling = DayView.this.mScrolling && DayView.this.mScroller.computeScrollOffset();
            if (!DayView.this.mScrolling || DayView.this.mPaused) {
                DayView.this.resetSelectedHour();
                DayView.this.invalidate();
                return;
            }
            DayView.this.mViewStartY = DayView.this.mScroller.getCurrY();
            if (DayView.this.mCallEdgeEffectOnAbsorb) {
                if (DayView.this.mViewStartY < 0) {
                    DayView.this.mEdgeEffectTop.onAbsorb((int) DayView.this.mLastVelocity);
                    DayView.this.mCallEdgeEffectOnAbsorb = false;
                } else if (DayView.this.mViewStartY > DayView.this.mMaxViewStartY) {
                    DayView.this.mEdgeEffectBottom.onAbsorb((int) DayView.this.mLastVelocity);
                    DayView.this.mCallEdgeEffectOnAbsorb = false;
                }
                DayView.this.mLastVelocity = DayView.this.mScroller.getCurrVelocity();
            }
            if (DayView.this.mScrollStartY == 0 || DayView.this.mScrollStartY == DayView.this.mMaxViewStartY) {
                if (DayView.this.mViewStartY < 0) {
                    DayView.this.mViewStartY = 0;
                } else if (DayView.this.mViewStartY > DayView.this.mMaxViewStartY) {
                    DayView.this.mViewStartY = DayView.this.mMaxViewStartY;
                }
            }
            DayView.this.computeFirstHour();
            DayView.this.mHandler.post(this);
            DayView.this.invalidate();
        }
    }

    public void cleanup() {
        if (this.mPopup != null) {
            this.mPopup.dismiss();
        }
        this.mPaused = true;
        this.mLastPopupEventID = -1L;
        if (this.mHandler != null) {
            this.mHandler.removeCallbacks(this.mDismissPopup);
            this.mHandler.removeCallbacks(this.mUpdateCurrentTime);
        }
        Utils.setSharedPreference(this.mContext, "preferences_default_cell_height", mCellHeight);
        eventClickCleanup();
        this.mRemeasure = false;
        this.mScrolling = false;
    }

    private void eventClickCleanup() {
        removeCallbacks(this.mClearClick);
        removeCallbacks(this.mSetClick);
        this.mClickedEvent = null;
        this.mSavedClickedEvent = null;
    }

    private void setSelectedEvent(Event e) {
        this.mSelectedEvent = e;
        this.mSelectedEventForAccessibility = e;
    }

    private void setSelectedHour(int h) {
        this.mSelectionHour = h;
        this.mSelectionHourForAccessibility = h;
    }

    private void setSelectedDay(int d) {
        this.mSelectionDay = d;
        this.mSelectionDayForAccessibility = d;
    }

    public void restartCurrentTimeUpdates() {
        this.mPaused = false;
        if (this.mHandler != null) {
            this.mHandler.removeCallbacks(this.mUpdateCurrentTime);
            this.mHandler.post(this.mUpdateCurrentTime);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cleanup();
        super.onDetachedFromWindow();
    }

    class DismissPopup implements Runnable {
        DismissPopup() {
        }

        @Override
        public void run() {
            if (DayView.this.mPopup != null) {
                DayView.this.mPopup.dismiss();
            }
        }
    }

    class UpdateCurrentTime implements Runnable {
        UpdateCurrentTime() {
        }

        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            DayView.this.mCurrentTime.set(currentTime);
            if (!DayView.this.mPaused) {
                DayView.this.mHandler.postDelayed(DayView.this.mUpdateCurrentTime, 300000 - (currentTime % 300000));
            }
            DayView.this.mTodayJulianDay = Time.getJulianDay(currentTime, DayView.this.mCurrentTime.gmtoff);
            DayView.this.invalidate();
        }
    }

    class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
        CalendarGestureListener() {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            if (DayView.DEBUG) {
                Log.e(DayView.TAG, "GestureDetector.onSingleTapUp");
            }
            DayView.this.doSingleTapUp(ev);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent ev) {
            if (DayView.DEBUG) {
                Log.e(DayView.TAG, "GestureDetector.onLongPress");
            }
            DayView.this.doLongPress(ev);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (DayView.DEBUG) {
                Log.e(DayView.TAG, "GestureDetector.onScroll");
            }
            DayView.this.eventClickCleanup();
            if (DayView.this.mTouchStartedInAlldayArea) {
                if (Math.abs(distanceX) < Math.abs(distanceY)) {
                    DayView.this.invalidate();
                    return false;
                }
                distanceY = 0.0f;
            }
            DayView.this.doScroll(e1, e2, distanceX, distanceY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (DayView.DEBUG) {
                Log.e(DayView.TAG, "GestureDetector.onFling");
            }
            if (DayView.this.mTouchStartedInAlldayArea) {
                if (Math.abs(velocityX) < Math.abs(velocityY)) {
                    return false;
                }
                velocityY = 0.0f;
            }
            DayView.this.doFling(e1, e2, velocityX, velocityY);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            if (DayView.DEBUG) {
                Log.e(DayView.TAG, "GestureDetector.onDown");
            }
            DayView.this.doDown(ev);
            return true;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        int flags = 2;
        long time = getSelectedTimeInMillis();
        if (!this.mSelectionAllday) {
            flags = 2 | 1;
        }
        if (DateFormat.is24HourFormat(this.mContext)) {
            flags |= 128;
        }
        this.mLongPressTitle = Utils.formatDateRange(this.mContext, time, time, flags);
        new AlertDialog.Builder(this.mContext).setTitle(this.mLongPressTitle).setItems(this.mLongPressItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    long extraLong = 0;
                    if (DayView.this.mSelectionAllday) {
                        extraLong = 16;
                    }
                    DayView.this.mController.sendEventRelatedEventWithExtra(this, 1L, -1L, DayView.this.getSelectedTimeInMillis(), 0L, -1, -1, extraLong, -1L);
                }
            }
        }).show().setCanceledOnTouchOutside(true);
        return true;
    }

    private class ScrollInterpolator implements Interpolator {
        public ScrollInterpolator() {
        }

        @Override
        public float getInterpolation(float t) {
            float t2 = t - 1.0f;
            float t3 = (t2 * t2 * t2 * t2 * t2) + 1.0f;
            if ((1.0f - t3) * DayView.this.mAnimationDistance < 1.0f) {
                DayView.this.cancelAnimation();
            }
            return t3;
        }
    }

    private long calculateDuration(float delta, float width, float velocity) {
        float halfScreenSize = width / 2.0f;
        float distanceRatio = delta / width;
        float distanceInfluenceForSnapDuration = distanceInfluenceForSnapDuration(distanceRatio);
        float distance = halfScreenSize + (halfScreenSize * distanceInfluenceForSnapDuration);
        float velocity2 = Math.max(2200.0f, Math.abs(velocity));
        long duration = Math.round(1000.0f * Math.abs(distance / velocity2)) * 6;
        if (DEBUG) {
            Log.e(TAG, "halfScreenSize:" + halfScreenSize + " delta:" + delta + " distanceRatio:" + distanceRatio + " distance:" + distance + " velocity:" + velocity2 + " duration:" + duration + " distanceInfluenceForSnapDuration:" + distanceInfluenceForSnapDuration);
        }
        return duration;
    }

    private float distanceInfluenceForSnapDuration(float f) {
        return (float) Math.sin((float) (((double) (f - 0.5f)) * 0.4712389167638204d));
    }
}
