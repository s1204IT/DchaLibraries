package com.android.calendar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
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
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.MTKUtils;
import com.mediatek.calendar.PDebug;
import com.mediatek.calendar.extension.ExtensionFactory;
import com.mediatek.calendar.extension.ICalendarThemeExt;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;
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
    private int mAmPmLeftMargin;
    private String mAmString;
    private int mAnimateDayEventHeight;
    private int mAnimateDayHeight;
    private boolean mAnimateToday;
    private int mAnimateTodayAlpha;
    private float mAnimationDistance;
    AnimatorListenerAdapter mAnimatorListener;
    Time mBaseDate;
    private final Typeface mBold;
    private ICalendarThemeExt mCalendarThemeExt;
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
    private int mDateStrWidth2letter;
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
    private boolean mIsSelectionFocusShow;
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
    private static float mScale = 0.0f;
    private static int DEFAULT_CELL_HEIGHT = 64;
    private static int MAX_CELL_HEIGHT = 150;
    private static int MIN_Y_SPAN = 100;
    private static int THEME_ALPHA_GRID_AREA_SELECTED = 230;
    private static int EVENT_RECT_ALPHA = 160;
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
        public void onAnimationEnd(Animator animator) {
            synchronized (this) {
                if (this.mAnimator != animator) {
                    animator.removeAllListeners();
                    animator.cancel();
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

        public void setAnimator(Animator animator) {
            this.mAnimator = animator;
        }

        public void setFadingIn(boolean z) {
            this.mFadingIn = z;
        }
    }

    public DayView(Context context, CalendarController calendarController, ViewSwitcher viewSwitcher, EventLoader eventLoader, int i) {
        int i2;
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
                String timeZone = Utils.getTimeZone(DayView.this.mContext, this);
                DayView.this.mBaseDate.timezone = timeZone;
                DayView.this.mBaseDate.normalize(true);
                DayView.this.mCurrentTime.switchTimezone(timeZone);
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
            public void onAnimationStart(Animator animator) {
                DayView.this.mScrolling = true;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                DayView.this.mScrolling = false;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
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
        this.mAmPmLeftMargin = 4;
        this.mCancelCallback = new Runnable() {
            @Override
            public void run() {
                DayView.this.clearCachedEvents();
            }
        };
        this.mIsSelectionFocusShow = false;
        this.drawTextSanitizerFilter = Pattern.compile("[\t\n],");
        PDebug.Start("DayView.DayView");
        this.mContext = context;
        initAccessibilityVariables();
        this.mResources = context.getResources();
        this.mCreateNewEventString = this.mResources.getString(R.string.event_create);
        this.mNewEventHintString = this.mResources.getString(R.string.day_view_new_event_hint);
        this.mNumDays = i;
        Log.d("DayView", "DayView()[mNumDays] " + this.mNumDays);
        DATE_HEADER_FONT_SIZE = (float) ((int) this.mResources.getDimension(R.dimen.date_header_text_size));
        DAY_HEADER_FONT_SIZE = (float) ((int) this.mResources.getDimension(R.dimen.day_label_text_size));
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
            i2 = R.dimen.day_view_event_text_size;
        } else {
            i2 = R.dimen.week_view_event_text_size;
        }
        EVENT_TEXT_FONT_SIZE = (int) this.mResources.getDimension(i2);
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
        this.mCalendarThemeExt = ExtensionFactory.getCalendarTheme(this.mContext);
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
        this.mController = calendarController;
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
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mScaledPagingTouchSlop = viewConfiguration.getScaledPagingTouchSlop();
        mOnDownDelay = ViewConfiguration.getTapTimeout();
        this.OVERFLING_DISTANCE = viewConfiguration.getScaledOverflingDistance();
        init(context);
        this.mAmPmLeftMargin = (int) this.mResources.getDimension(R.dimen.ampm_left_margin);
        PDebug.End("DayView.DayView");
    }

    @Override
    protected void onAttachedToWindow() {
        if (this.mHandler == null) {
            this.mHandler = getHandler();
            if (this.mHandler != null) {
                this.mHandler.post(this.mUpdateCurrentTime);
            }
        }
    }

    private void init(Context context) {
        PDebug.Start("DayView.init");
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
        setOnCreateContextMenuListener(this);
        this.mFirstDayOfWeek = Utils.getFirstDayOfWeek(context);
        this.mCurrentTime = new Time(Utils.getTimeZone(context, this.mTZUpdater));
        long jCurrentTimeMillis = System.currentTimeMillis();
        this.mCurrentTime.set(jCurrentTimeMillis);
        this.mTodayJulianDay = Time.getJulianDay(jCurrentTimeMillis, this.mCurrentTime.gmtoff);
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
        if (this.mCalendarThemeExt.isThemeManagerEnable()) {
            mClickedColor = this.mCalendarThemeExt.getThemeColor();
        } else {
            mClickedColor = this.mResources.getColor(R.color.day_event_clicked_background_color);
        }
        mEventTextColor = this.mResources.getColor(R.color.calendar_event_text_color);
        mMoreEventsTextColor = this.mResources.getColor(R.color.month_event_other_color);
        this.mEventTextPaint.setTextSize(EVENT_TEXT_FONT_SIZE);
        this.mEventTextPaint.setTextAlign(Paint.Align.LEFT);
        this.mEventTextPaint.setAntiAlias(true);
        int color = this.mResources.getColor(R.color.calendar_grid_line_highlight_color);
        Paint paint = this.mSelectionPaint;
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);
        Paint paint2 = this.mPaint;
        paint2.setAntiAlias(true);
        this.mDayStrs = new String[14];
        this.mDayStrs2Letter = new String[14];
        for (int i = 1; i <= 7; i++) {
            int i2 = i - 1;
            this.mDayStrs[i2] = DateUtils.getDayOfWeekString(i, 20).toUpperCase();
            int i3 = i2 + 7;
            this.mDayStrs[i3] = this.mDayStrs[i2];
            this.mDayStrs2Letter[i2] = DateUtils.getDayOfWeekString(i, 30).toUpperCase();
            if (this.mDayStrs2Letter[i2].equals(this.mDayStrs[i2])) {
                this.mDayStrs2Letter[i2] = DateUtils.getDayOfWeekString(i, 50);
            }
            this.mDayStrs2Letter[i3] = this.mDayStrs2Letter[i2];
        }
        paint2.setTextSize(DATE_HEADER_FONT_SIZE);
        paint2.setTypeface(this.mBold);
        this.mDateStrWidth = computeMaxStringWidth(0, new String[]{" 28", " 30"}, paint2);
        this.mDateStrWidth2letter = this.mDateStrWidth;
        paint2.setTextSize(DAY_HEADER_FONT_SIZE);
        this.mDateStrWidth += computeMaxStringWidth(0, this.mDayStrs, paint2);
        this.mDateStrWidth2letter += computeMaxStringWidth(0, this.mDayStrs2Letter, paint2);
        paint2.setTextSize(HOURS_TEXT_SIZE);
        paint2.setTypeface(null);
        handleOnResume();
        String[] amPmStrings = new DateFormatSymbols().getAmPmStrings();
        this.mAmString = amPmStrings[0];
        this.mPmString = amPmStrings[1];
        String[] strArr = {this.mAmString, this.mPmString};
        paint2.setTextSize(AMPM_TEXT_SIZE);
        this.mHoursWidth = Math.max(HOURS_MARGIN, computeMaxStringWidth(this.mHoursWidth, strArr, paint2) + HOURS_RIGHT_MARGIN);
        this.mHoursWidth = Math.max(MIN_HOURS_WIDTH, this.mHoursWidth);
        this.mPopupView = ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.bubble_event, (ViewGroup) null);
        this.mPopupView.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
        this.mPopup = new PopupWindow(context);
        this.mPopup.setContentView(this.mPopupView);
        Resources.Theme themeNewTheme = getResources().newTheme();
        themeNewTheme.applyStyle(android.R.style.Theme.Dialog, true);
        TypedArray typedArrayObtainStyledAttributes = themeNewTheme.obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
        this.mPopup.setBackgroundDrawable(typedArrayObtainStyledAttributes.getDrawable(0));
        typedArrayObtainStyledAttributes.recycle();
        this.mPopupView.setOnClickListener(this);
        setOnLongClickListener(this);
        this.mBaseDate = new Time(Utils.getTimeZone(context, this.mTZUpdater));
        this.mBaseDate.set(System.currentTimeMillis());
        this.mEarliestStartHour = new int[this.mNumDays];
        this.mHasAllDayEvent = new boolean[this.mNumDays];
        int i4 = 25 + this.mNumDays + 1;
        this.mLines = new float[i4 * 4];
        Log.d("DayView", "init()[mLines] " + i4);
        PDebug.End("DayView.init");
    }

    @Override
    public void onClick(View view) {
        if (view == this.mPopupView) {
            switchViews(true);
        }
    }

    public void handleOnResume() {
        PDebug.Start("DayView.handleOnResume");
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
        PDebug.End("DayView.handleOnResume");
    }

    private void initAccessibilityVariables() {
        this.mAccessibilityMgr = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        this.mIsAccessibilityEnabled = this.mAccessibilityMgr != null && this.mAccessibilityMgr.isEnabled();
        this.mTouchExplorationEnabled = isTouchExplorationEnabled();
    }

    long getSelectedTimeInMillis() {
        Time time = new Time(this.mBaseDate);
        Utils.setJulianDayInGeneral(time, this.mSelectionDay);
        time.hour = this.mSelectionHour;
        return time.normalize(true);
    }

    Time getSelectedTime() {
        Time time = new Time(this.mBaseDate);
        Utils.setJulianDayInGeneral(time, this.mSelectionDay);
        time.hour = this.mSelectionHour;
        time.normalize(true);
        return time;
    }

    Time getSelectedTimeForAccessibility() {
        Time time = new Time(this.mBaseDate);
        Utils.setJulianDayInGeneral(time, this.mSelectionDay);
        time.hour = this.mSelectionHourForAccessibility;
        time.normalize(true);
        return time;
    }

    int getFirstVisibleHour() {
        return this.mFirstHour;
    }

    void setFirstVisibleHour(int i) {
        this.mFirstHour = i;
        this.mFirstHourOffset = 0;
    }

    public void setSelected(Time time, boolean z, boolean z2) {
        int i;
        boolean z3;
        this.mBaseDate.set(time);
        setSelectedHour(this.mBaseDate.hour);
        setSelectedEvent(null);
        this.mPrevSelectedEvent = null;
        setSelectedDay(Utils.getJulianDayInGeneral(this.mBaseDate, false));
        this.mSelectedEvents.clear();
        this.mComputeSelectedEvents = true;
        if (z || this.mGridAreaHeight == -1) {
            i = Integer.MIN_VALUE;
        } else {
            if (this.mBaseDate.hour < this.mFirstHour) {
                i = this.mBaseDate.hour * (mCellHeight + 1);
            } else {
                i = this.mBaseDate.hour >= ((this.mGridAreaHeight - this.mFirstHourOffset) / (mCellHeight + 1)) + this.mFirstHour ? (int) ((((this.mBaseDate.hour + 1) + (this.mBaseDate.minute / 60.0f)) * (mCellHeight + 1)) - this.mGridAreaHeight) : Integer.MIN_VALUE;
            }
            if (i > this.mMaxViewStartY) {
                i = this.mMaxViewStartY;
            } else if (i < 0 && i != Integer.MIN_VALUE) {
                i = 0;
            }
        }
        recalc();
        this.mRemeasure = true;
        invalidate();
        if (i != Integer.MIN_VALUE) {
            ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "viewStartY", this.mViewStartY, i);
            objectAnimatorOfInt.setDuration(200L);
            objectAnimatorOfInt.setInterpolator(new AccelerateDecelerateInterpolator());
            objectAnimatorOfInt.addListener(this.mAnimatorListener);
            objectAnimatorOfInt.start();
            z3 = true;
        } else {
            z3 = false;
        }
        if (z2) {
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
                if (z3) {
                    this.mTodayAnimator.setStartDelay(200L);
                }
                this.mTodayAnimator.start();
            }
        }
        sendAccessibilityEventAsNeeded(false);
    }

    public void setViewStartY(int i) {
        if (i > this.mMaxViewStartY) {
            i = this.mMaxViewStartY;
        }
        this.mViewStartY = i;
        computeFirstHour();
        invalidate();
    }

    public void setAnimateTodayAlpha(int i) {
        this.mAnimateTodayAlpha = i;
        invalidate();
    }

    public void updateTitle() {
        long j;
        Time time = new Time(this.mBaseDate);
        time.normalize(true);
        Time time2 = new Time(time);
        time2.monthDay += this.mNumDays - 1;
        time2.minute++;
        time2.normalize(true);
        if (this.mNumDays != 1) {
            j = 52;
            if (time.month != time2.month) {
                j = 65588;
            }
        } else {
            j = 20;
        }
        this.mController.sendEvent(this, 1024L, time, time2, null, -1L, 0, j, null, null);
    }

    public int compareToVisibleTimeRange(Time time) {
        int iCompare;
        int i = this.mBaseDate.hour;
        int i2 = this.mBaseDate.minute;
        int i3 = this.mBaseDate.second;
        this.mBaseDate.hour = 0;
        this.mBaseDate.minute = 0;
        this.mBaseDate.second = 0;
        int iCompare2 = Time.compare(time, this.mBaseDate);
        if (iCompare2 > 0) {
            this.mBaseDate.monthDay += this.mNumDays;
            this.mBaseDate.normalize(true);
            iCompare = Time.compare(time, this.mBaseDate);
            this.mBaseDate.monthDay -= this.mNumDays;
            this.mBaseDate.normalize(true);
            if (iCompare >= 0) {
                if (iCompare == 0) {
                    iCompare = 1;
                }
            } else {
                iCompare = 0;
            }
        } else {
            iCompare = iCompare2;
        }
        this.mBaseDate.hour = i;
        this.mBaseDate.minute = i2;
        this.mBaseDate.second = i3;
        return iCompare;
    }

    private void recalc() {
        this.mBaseDate.normalize(true);
        if (this.mNumDays == 7) {
            adjustToBeginningOfWeek(this.mBaseDate);
        }
        this.mFirstJulianDay = Utils.getJulianDayInGeneral(this.mBaseDate, false);
        this.mLastJulianDay = (this.mFirstJulianDay + this.mNumDays) - 1;
        this.mMonthLength = this.mBaseDate.getActualMaximum(4);
        this.mFirstVisibleDate = this.mBaseDate.monthDay;
        this.mFirstVisibleDayOfWeek = this.mBaseDate.weekDay;
    }

    private void adjustToBeginningOfWeek(Time time) {
        int i = time.weekDay - this.mFirstDayOfWeek;
        if (i != 0) {
            if (i < 0) {
                i += 7;
            }
            time.monthDay -= i;
            time.normalize(true);
        }
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        PDebug.EndAndStart("DayFragment.onResume->DayView.onSizeChanged", "DayView.onSizeChanged");
        this.mViewWidth = i;
        this.mHoursWidth = this.mViewWidth - (((this.mViewWidth - this.mHoursWidth) / this.mNumDays) * this.mNumDays);
        this.mViewHeight = i2;
        this.mEdgeEffectTop.setSize(this.mViewWidth, this.mViewHeight);
        this.mEdgeEffectBottom.setSize(this.mViewWidth, this.mViewHeight);
        this.mCellWidth = ((i - this.mHoursWidth) - (this.mNumDays * 1)) / this.mNumDays;
        mHorizontalSnapBackThreshold = i / 7;
        Paint paint = new Paint();
        paint.setTextSize(HOURS_TEXT_SIZE);
        this.mHoursTextHeight = (int) Math.abs(paint.ascent());
        remeasure(i, i2);
        PDebug.End("DayView.onSizeChanged");
    }

    private void remeasure(int i, int i2) {
        int iMax;
        MAX_UNEXPANDED_ALLDAY_HEIGHT = (int) (MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT * 4.0f);
        MAX_UNEXPANDED_ALLDAY_HEIGHT = Math.min(MAX_UNEXPANDED_ALLDAY_HEIGHT, i2 / 6);
        MAX_UNEXPANDED_ALLDAY_HEIGHT = Math.max(MAX_UNEXPANDED_ALLDAY_HEIGHT, ((int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT) * 2);
        this.mMaxUnexpandedAlldayEventCount = (int) (MAX_UNEXPANDED_ALLDAY_HEIGHT / MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT);
        for (int i3 = 0; i3 < this.mNumDays; i3++) {
            this.mEarliestStartHour[i3] = 25;
            this.mHasAllDayEvent[i3] = false;
        }
        int i4 = this.mMaxAlldayEvents;
        mMinCellHeight = Math.max((i2 - DAY_HEADER_HEIGHT) / 24, (int) MIN_EVENT_HEIGHT);
        if (mCellHeight < mMinCellHeight) {
            mCellHeight = mMinCellHeight;
        }
        this.mFirstCell = DAY_HEADER_HEIGHT;
        if (i4 > 0) {
            int i5 = (i2 - DAY_HEADER_HEIGHT) - MIN_HOURS_HEIGHT;
            if (i4 == 1) {
                iMax = SINGLE_ALLDAY_HEIGHT;
            } else if (i4 <= this.mMaxUnexpandedAlldayEventCount) {
                iMax = i4 * MAX_HEIGHT_OF_ONE_ALLDAY_EVENT;
                if (iMax > MAX_UNEXPANDED_ALLDAY_HEIGHT) {
                    iMax = MAX_UNEXPANDED_ALLDAY_HEIGHT;
                }
            } else if (this.mAnimateDayHeight != 0) {
                iMax = Math.max(this.mAnimateDayHeight, MAX_UNEXPANDED_ALLDAY_HEIGHT);
            } else {
                iMax = (int) (i4 * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT);
                if (!mShowAllAllDayEvents && iMax > MAX_UNEXPANDED_ALLDAY_HEIGHT) {
                    iMax = (int) (this.mMaxUnexpandedAlldayEventCount * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT);
                } else if (iMax > i5) {
                    iMax = i5;
                }
            }
            this.mFirstCell = DAY_HEADER_HEIGHT + iMax + ALLDAY_TOP_MARGIN;
        } else {
            this.mSelectionAllday = false;
            iMax = 0;
        }
        this.mAlldayHeight = iMax;
        this.mGridAreaHeight = i2 - this.mFirstCell;
        int intrinsicWidth = this.mExpandAlldayDrawable.getIntrinsicWidth();
        this.mExpandAllDayRect.left = Math.max((this.mHoursWidth - intrinsicWidth) / 2, EVENT_ALL_DAY_TEXT_LEFT_MARGIN);
        this.mExpandAllDayRect.right = Math.min(this.mExpandAllDayRect.left + intrinsicWidth, this.mHoursWidth - EVENT_ALL_DAY_TEXT_RIGHT_MARGIN);
        this.mExpandAllDayRect.bottom = this.mFirstCell - EXPAND_ALL_DAY_BOTTOM_MARGIN;
        this.mExpandAllDayRect.top = this.mExpandAllDayRect.bottom - this.mExpandAlldayDrawable.getIntrinsicHeight();
        this.mNumHours = this.mGridAreaHeight / (mCellHeight + 1);
        this.mEventGeometry.setHourHeight(mCellHeight);
        Event.computePositions(this.mEvents, (long) ((MIN_EVENT_HEIGHT * 60000.0f) / (mCellHeight / 60.0f)));
        this.mMaxViewStartY = ((24 * (mCellHeight + 1)) + 1) - this.mGridAreaHeight;
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
        int i6 = this.mNumDays * (this.mCellWidth + 1);
        if (this.mSelectedEvent != null && this.mLastPopupEventID != this.mSelectedEvent.id) {
            this.mPopup.dismiss();
        }
        this.mPopup.setWidth(i6 - 20);
        this.mPopup.setHeight(-2);
    }

    private void initView(DayView dayView) {
        if (this.mFirstHour <= 0) {
            setFirstVisibleHour(0);
            LogUtil.v("DayView", "The view overScroll,now mFirstHour is " + this.mFirstHour + ",and set it to 0 when init nextView");
        }
        dayView.setSelectedHour(this.mSelectionHour);
        dayView.mSelectedEvents.clear();
        dayView.mComputeSelectedEvents = true;
        dayView.mFirstHour = this.mFirstHour;
        dayView.mFirstHourOffset = this.mFirstHourOffset;
        dayView.remeasure(getWidth(), getHeight());
        dayView.initAllDayHeights();
        dayView.setSelectedEvent(null);
        dayView.mPrevSelectedEvent = null;
        dayView.mFirstDayOfWeek = this.mFirstDayOfWeek;
        if (dayView.mEvents.size() > 0) {
            dayView.mSelectionAllday = this.mSelectionAllday;
        } else {
            dayView.mSelectionAllday = false;
        }
        dayView.recalc();
    }

    private void switchViews(boolean z) {
        Event event = this.mSelectedEvent;
        this.mPopup.dismiss();
        this.mLastPopupEventID = -1L;
        if (this.mNumDays > 1) {
            if (z) {
                if (event == null) {
                    long selectedTimeInMillis = getSelectedTimeInMillis();
                    this.mController.sendEventRelatedEventWithExtra(this, 1L, -1L, selectedTimeInMillis, selectedTimeInMillis + 3600000, -1, -1, this.mSelectionAllday ? 16L : 0L, -1L);
                    return;
                } else {
                    if (this.mIsAccessibilityEnabled) {
                        this.mAccessibilityMgr.interrupt();
                    }
                    this.mController.sendEventRelatedEvent(this, 2L, event.id, event.startMillis, event.endMillis, 0, 0, getSelectedTimeInMillis());
                    return;
                }
            }
            if (this.mSelectedEvents.size() == 1) {
                if (this.mIsAccessibilityEnabled) {
                    this.mAccessibilityMgr.interrupt();
                }
                this.mController.sendEventRelatedEvent(this, 2L, event.id, event.startMillis, event.endMillis, 0, 0, getSelectedTimeInMillis());
                return;
            }
            return;
        }
        if (event == null) {
            long selectedTimeInMillis2 = getSelectedTimeInMillis();
            this.mController.sendEventRelatedEventWithExtra(this, 1L, -1L, selectedTimeInMillis2, selectedTimeInMillis2 + 3600000, -1, -1, this.mSelectionAllday ? 16L : 0L, -1L);
        } else {
            if (this.mIsAccessibilityEnabled) {
                this.mAccessibilityMgr.interrupt();
            }
            this.mController.sendEventRelatedEvent(this, 2L, event.id, event.startMillis, event.endMillis, 0, 0, getSelectedTimeInMillis());
        }
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        this.mScrolling = false;
        long eventTime = keyEvent.getEventTime() - keyEvent.getDownTime();
        if (i == 23 && this.mSelectionMode != 0) {
            if (this.mSelectionMode == 1) {
                this.mSelectionMode = 2;
                invalidate();
            } else if (eventTime < ViewConfiguration.getLongPressTimeout()) {
                switchViews(true);
            } else {
                this.mSelectionMode = 3;
                invalidate();
                performLongClick();
            }
        }
        return super.onKeyUp(i, keyEvent);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (this.mSelectionMode == 0) {
            if (i == 66 || i == 22 || i == 21 || i == 19 || i == 20) {
                this.mSelectionMode = 2;
                invalidate();
                return true;
            }
            if (i == 23) {
                this.mSelectionMode = 1;
                invalidate();
                return true;
            }
            if (i == 4 || i == 82) {
                return false;
            }
        }
        this.mSelectionMode = 2;
        this.mScrolling = false;
        int i2 = this.mSelectionDay;
        if (i == 4) {
            if (keyEvent.getRepeatCount() == 0) {
                keyEvent.startTracking();
                return true;
            }
            return super.onKeyDown(i, keyEvent);
        }
        switch (i) {
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
                break;
            case 21:
                if (this.mSelectedEvent != null) {
                    setSelectedEvent(this.mSelectedEvent.nextLeft);
                }
                if (this.mSelectedEvent == null) {
                    this.mLastPopupEventID = -1L;
                    i2--;
                }
                break;
            case 22:
                if (this.mSelectedEvent != null) {
                    setSelectedEvent(this.mSelectedEvent.nextRight);
                }
                if (this.mSelectedEvent == null) {
                    this.mLastPopupEventID = -1L;
                    i2++;
                }
                break;
            default:
                switch (i) {
                    case 66:
                        switchViews(true);
                        return true;
                    case 67:
                        Event event = this.mSelectedEvent;
                        if (event == null) {
                            return false;
                        }
                        this.mPopup.dismiss();
                        this.mLastPopupEventID = -1L;
                        this.mDeleteEventHelper.delete(event.startMillis, event.endMillis, event.id, -1);
                        return true;
                    default:
                        return super.onKeyDown(i, keyEvent);
                }
        }
        int i3 = i2;
        if (i3 < this.mFirstJulianDay || i3 > this.mLastJulianDay) {
            DayView dayView = (DayView) this.mViewSwitcher.getNextView();
            Time time = dayView.mBaseDate;
            time.set(this.mBaseDate);
            if (i3 < this.mFirstJulianDay) {
                time.monthDay -= this.mNumDays;
            } else {
                time.monthDay += this.mNumDays;
            }
            time.normalize(true);
            dayView.setSelectedDay(i3);
            initView(dayView);
            Time time2 = new Time(time);
            time2.monthDay += this.mNumDays - 1;
            this.mController.sendEvent(this, 32L, time, time2, -1L, 0);
            return true;
        }
        if (this.mSelectionDay != i3) {
            Time time3 = new Time(this.mBaseDate);
            Utils.setJulianDayInGeneral(time3, this.mSelectionDay);
            time3.hour = this.mSelectionHour;
            this.mController.sendEvent(this, 32L, time3, time3, -1L, 0);
        }
        setSelectedDay(i3);
        this.mSelectedEvents.clear();
        this.mComputeSelectedEvents = true;
        this.mUpdateToast = true;
        invalidate();
        return true;
    }

    @Override
    public boolean onHoverEvent(MotionEvent motionEvent) {
        if (!this.mTouchExplorationEnabled) {
            return super.onHoverEvent(motionEvent);
        }
        if (motionEvent.getAction() != 10) {
            setSelectionFromPosition((int) motionEvent.getX(), (int) motionEvent.getY(), true);
            invalidate();
        }
        return true;
    }

    private boolean isTouchExplorationEnabled() {
        return this.mIsAccessibilityEnabled && this.mAccessibilityMgr.isTouchExplorationEnabled();
    }

    private void sendAccessibilityEventAsNeeded(boolean z) {
        if (!(this.mAccessibilityMgr != null && this.mAccessibilityMgr.isEnabled())) {
            return;
        }
        boolean z2 = this.mLastSelectionDayForAccessibility != this.mSelectionDayForAccessibility;
        boolean z3 = this.mLastSelectionHourForAccessibility != this.mSelectionHourForAccessibility;
        if (z2 || z3 || this.mLastSelectedEventForAccessibility != this.mSelectedEventForAccessibility) {
            this.mLastSelectionDayForAccessibility = this.mSelectionDayForAccessibility;
            this.mLastSelectionHourForAccessibility = this.mSelectionHourForAccessibility;
            this.mLastSelectedEventForAccessibility = this.mSelectedEventForAccessibility;
            StringBuilder sb = new StringBuilder();
            if (z2) {
                sb.append(getSelectedTimeForAccessibility().format("%A "));
            }
            if (z3) {
                sb.append(getSelectedTimeForAccessibility().format(this.mIs24HourFormat ? "%k" : "%l%p"));
            }
            if (z2 || z3) {
                sb.append(". ");
            }
            if (z) {
                if (this.mEventCountTemplate == null) {
                    this.mEventCountTemplate = this.mContext.getString(R.string.template_announce_item_index);
                }
                int size = this.mSelectedEvents.size();
                if (size > 0) {
                    if (this.mSelectedEventForAccessibility == null) {
                        int i = 1;
                        for (Event event : this.mSelectedEvents) {
                            if (size > 1) {
                                mStringBuilder.setLength(0);
                                sb.append(mFormatter.format(this.mEventCountTemplate, Integer.valueOf(i), Integer.valueOf(size)));
                                sb.append(" ");
                                i++;
                            }
                            appendEventAccessibilityString(sb, event);
                        }
                    } else {
                        if (size > 1) {
                            mStringBuilder.setLength(0);
                            sb.append(mFormatter.format(this.mEventCountTemplate, Integer.valueOf(this.mSelectedEvents.indexOf(this.mSelectedEventForAccessibility) + 1), Integer.valueOf(size)));
                            sb.append(" ");
                        }
                        appendEventAccessibilityString(sb, this.mSelectedEventForAccessibility);
                    }
                } else {
                    sb.append(this.mCreateNewEventString);
                }
            }
            if (z2 || z3 || z) {
                AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(8);
                String string = sb.toString();
                accessibilityEventObtain.getText().add(string);
                accessibilityEventObtain.setAddedCount(string.length());
                sendAccessibilityEventUnchecked(accessibilityEventObtain);
            }
        }
    }

    private void appendEventAccessibilityString(StringBuilder sb, Event event) {
        int i;
        sb.append(event.getTitleAndLocation());
        sb.append(". ");
        if (event.allDay) {
            i = 8210;
        } else {
            i = 17;
            if (DateFormat.is24HourFormat(this.mContext)) {
                i = 145;
            }
        }
        sb.append(Utils.formatDateRange(this.mContext, event.startMillis, event.endMillis, i));
        sb.append(". ");
    }

    private class GotoBroadcaster implements Animation.AnimationListener {
        private final int mCounter = DayView.access$1104();
        private final Time mEnd;
        private final Time mStart;

        public GotoBroadcaster(Time time, Time time2) {
            this.mStart = time;
            this.mEnd = time2;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            ((DayView) DayView.this.mViewSwitcher.getCurrentView()).mViewStartX = 0;
            ((DayView) DayView.this.mViewSwitcher.getNextView()).mViewStartX = 0;
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

    private View switchViews(boolean z, float f, float f2, float f3) {
        float f4;
        Time time;
        this.mAnimationDistance = f2 - f;
        float fAbs = Math.abs(f) / f2;
        float f5 = 1.0f;
        if (fAbs > 1.0f) {
            fAbs = 1.0f;
        }
        if (z) {
            float f6 = 1.0f - fAbs;
            fAbs = -fAbs;
            f4 = f6;
            f5 = -1.0f;
        } else {
            f4 = fAbs - 1.0f;
        }
        Time time2 = new Time(this.mBaseDate.timezone);
        time2.set(this.mController.getTime());
        if (z) {
            time2.monthDay += this.mNumDays;
        } else {
            time2.monthDay -= this.mNumDays;
        }
        this.mController.setTime(time2.normalize(true));
        if (this.mNumDays == 7) {
            time = new Time(time2);
            adjustToBeginningOfWeek(time2);
        } else {
            time = time2;
        }
        Time time3 = new Time(time2);
        time3.monthDay += this.mNumDays - 1;
        TranslateAnimation translateAnimation = new TranslateAnimation(1, f4, 1, 0.0f, 0, 0.0f, 0, 0.0f);
        TranslateAnimation translateAnimation2 = new TranslateAnimation(1, fAbs, 1, f5, 0, 0.0f, 0, 0.0f);
        long jCalculateDuration = calculateDuration(f2 - Math.abs(f), f2, f3);
        translateAnimation.setDuration(jCalculateDuration);
        translateAnimation.setInterpolator(this.mHScrollInterpolator);
        translateAnimation2.setInterpolator(this.mHScrollInterpolator);
        translateAnimation2.setDuration(jCalculateDuration);
        translateAnimation2.setAnimationListener(new GotoBroadcaster(time2, time3));
        this.mViewSwitcher.setInAnimation(translateAnimation);
        this.mViewSwitcher.setOutAnimation(translateAnimation2);
        ((DayView) this.mViewSwitcher.getCurrentView()).cleanup();
        this.mViewSwitcher.showNext();
        DayView dayView = (DayView) this.mViewSwitcher.getCurrentView();
        dayView.setSelected(time, true, false);
        dayView.requestFocus();
        dayView.reloadEvents();
        dayView.updateTitle();
        dayView.restartCurrentTimeUpdates();
        return dayView;
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
            int i = this.mSelectionDay - this.mFirstJulianDay;
            if (i < this.mEarliestStartHour.length && i >= 0 && this.mMaxAlldayEvents > 0 && this.mEarliestStartHour[i] > this.mSelectionHour && this.mFirstHour > 0 && this.mFirstHour < 8) {
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
        PDebug.Start("DayView.reloadEvents");
        this.mTZUpdater.run();
        setSelectedEvent(null);
        this.mPrevSelectedEvent = null;
        this.mSelectedEvents.clear();
        Time time = new Time(Utils.getTimeZone(this.mContext, this.mTZUpdater));
        time.set(this.mBaseDate);
        time.hour = 0;
        time.minute = 0;
        time.second = 0;
        long jNormalize = time.normalize(true);
        if (jNormalize == this.mLastReloadMillis) {
            return;
        }
        this.mLastReloadMillis = jNormalize;
        final ArrayList<Event> arrayList = new ArrayList<>();
        this.mEventLoader.loadEventsInBackground(this.mNumDays, arrayList, this.mFirstJulianDay, new Runnable() {
            @Override
            public void run() {
                boolean z;
                PDebug.EndAndStart("EventLoader.LoadEventsRequest.processRequest->DayView.eventsLoadCallback", "DayView.eventsLoadCallback.refreshWithEvents");
                if (DayView.this.mFirstJulianDay == DayView.this.mLoadedFirstJulianDay) {
                    z = false;
                } else {
                    z = true;
                }
                DayView.this.mEvents = arrayList;
                DayView.this.mLoadedFirstJulianDay = DayView.this.mFirstJulianDay;
                if (DayView.this.mAllDayEvents != null) {
                    DayView.this.mAllDayEvents.clear();
                } else {
                    DayView.this.mAllDayEvents = new ArrayList();
                }
                for (Event event : arrayList) {
                    if (event.drawAsAllday()) {
                        DayView.this.mAllDayEvents.add(event);
                    }
                }
                if (DayView.this.mLayouts != null && DayView.this.mLayouts.length >= arrayList.size()) {
                    Arrays.fill(DayView.this.mLayouts, (Object) null);
                } else {
                    DayView.this.mLayouts = new StaticLayout[arrayList.size()];
                }
                if (DayView.this.mAllDayLayouts != null && DayView.this.mAllDayLayouts.length >= DayView.this.mAllDayEvents.size()) {
                    Arrays.fill(DayView.this.mAllDayLayouts, (Object) null);
                } else {
                    DayView.this.mAllDayLayouts = new StaticLayout[arrayList.size()];
                }
                PDebug.End("DayView.eventsLoadCallback.refreshWithEvents");
                DayView.this.computeEventRelations();
                DayView.this.mRemeasure = true;
                DayView.this.mComputeSelectedEvents = true;
                DayView.this.recalc();
                if (arrayList.isEmpty()) {
                    DayView.this.invalidate();
                    return;
                }
                if (z) {
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
        PDebug.End("DayView.reloadEvents");
    }

    public void setEventsAlpha(int i) {
        this.mEventsAlpha = i;
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
        ArrayList<Event> arrayList = this.mEvents;
        int size = arrayList.size();
        int[] iArr = new int[(this.mLastJulianDay - this.mFirstJulianDay) + 1];
        Arrays.fill(iArr, 0);
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            Event event = arrayList.get(i2);
            if (event.startDay <= this.mLastJulianDay && event.endDay >= this.mFirstJulianDay) {
                if (event.drawAsAllday()) {
                    int iMin = Math.min(event.endDay, this.mLastJulianDay);
                    for (int iMax = Math.max(event.startDay, this.mFirstJulianDay); iMax <= iMin; iMax++) {
                        int i3 = iMax - this.mFirstJulianDay;
                        int i4 = iArr[i3] + 1;
                        iArr[i3] = i4;
                        if (i < i4) {
                            i = i4;
                        }
                    }
                    int i5 = event.startDay - this.mFirstJulianDay;
                    int i6 = (event.endDay - event.startDay) + 1;
                    if (i5 < 0) {
                        i6 += i5;
                        i5 = 0;
                    }
                    if (i5 + i6 > this.mNumDays) {
                        i6 = this.mNumDays - i5;
                    }
                    while (i6 > 0) {
                        this.mHasAllDayEvent[i5] = true;
                        i5++;
                        i6--;
                    }
                } else {
                    int i7 = event.startDay - this.mFirstJulianDay;
                    int i8 = event.startTime / 60;
                    if (i7 >= 0 && i8 < this.mEarliestStartHour[i7]) {
                        this.mEarliestStartHour[i7] = i8;
                    }
                    int i9 = event.endDay - this.mFirstJulianDay;
                    int i10 = event.endTime / 60;
                    if (i9 < this.mNumDays && i10 < this.mEarliestStartHour[i9]) {
                        this.mEarliestStartHour[i9] = i10;
                    }
                }
            }
        }
        this.mMaxAlldayEvents = i;
        initAllDayHeights();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float f;
        PDebug.Start("DayView.onDraw");
        if (this.mRemeasure) {
            remeasure(getWidth(), getHeight());
            this.mRemeasure = false;
        }
        canvas.save();
        float f2 = (-this.mViewStartY) + DAY_HEADER_HEIGHT + this.mAlldayHeight;
        canvas.translate(-this.mViewStartX, f2);
        Rect rect = this.mDestRect;
        rect.top = (int) (this.mFirstCell - f2);
        rect.bottom = (int) (this.mViewHeight - f2);
        rect.left = 0;
        rect.right = this.mViewWidth;
        canvas.save();
        canvas.clipRect(rect);
        doDraw(canvas);
        canvas.restore();
        if ((this.mTouchMode & 64) != 0) {
            DayView dayView = (DayView) this.mViewSwitcher.getNextView();
            if (dayView != this) {
                if (this.mViewStartX > 0) {
                    f = this.mViewWidth;
                } else {
                    f = -this.mViewWidth;
                }
                canvas.translate(f, -f2);
                dayView.mTouchMode = 0;
                dayView.onDraw(canvas);
                canvas.translate(-f, 0.0f);
            } else {
                Log.d("DayView", "View switcher has already switched to the next view");
            }
        } else {
            canvas.translate(this.mViewStartX, -f2);
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
        PDebug.End("DayView.onDraw");
    }

    private void drawAfterScroll(Canvas canvas) {
        Paint paint = this.mPaint;
        Rect rect = this.mRect;
        drawAllDayHighlights(rect, canvas, paint);
        if (this.mMaxAlldayEvents != 0) {
            drawAllDayEvents(this.mFirstJulianDay, this.mNumDays, canvas, paint);
            drawUpperLeftCorner(rect, canvas, paint);
        }
        drawScrollLine(rect, canvas, paint);
        drawDayHeaderLoop(rect, canvas, paint);
        if (!this.mIs24HourFormat) {
            drawAmPm(canvas, paint);
        }
    }

    private void drawUpperLeftCorner(Rect rect, Canvas canvas, Paint paint) {
        setupHourTextPaint(paint);
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

    private void drawScrollLine(Rect rect, Canvas canvas, Paint paint) {
        int iComputeDayLeftPosition = computeDayLeftPosition(this.mNumDays);
        int i = this.mFirstCell - 1;
        paint.setAntiAlias(false);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(mCalendarGridLineInnerHorizontalColor);
        paint.setStrokeWidth(1.0f);
        float f = i;
        canvas.drawLine(GRID_LINE_LEFT_MARGIN, f, iComputeDayLeftPosition, f, paint);
        paint.setAntiAlias(true);
    }

    private int computeDayLeftPosition(int i) {
        return ((i * (this.mViewWidth - this.mHoursWidth)) / this.mNumDays) + this.mHoursWidth;
    }

    private void drawAllDayHighlights(Rect rect, Canvas canvas, Paint paint) {
        int i;
        if (mFutureBgColor != 0) {
            rect.top = 0;
            rect.bottom = DAY_HEADER_HEIGHT;
            rect.left = 0;
            rect.right = this.mViewWidth;
            paint.setColor(mBgColor);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(rect, paint);
            rect.top = DAY_HEADER_HEIGHT;
            rect.bottom = this.mFirstCell - 1;
            rect.left = 0;
            rect.right = this.mHoursWidth;
            canvas.drawRect(rect, paint);
            int i2 = -1;
            int i3 = this.mTodayJulianDay - this.mFirstJulianDay;
            if (i3 >= 0) {
                if (i3 >= 1 && (i = i3 + 1) < this.mNumDays) {
                    i2 = i;
                }
            } else {
                i2 = 0;
            }
            if (i2 >= 0) {
                rect.top = 0;
                rect.bottom = this.mFirstCell - 1;
                rect.left = computeDayLeftPosition(i2) + 1;
                rect.right = computeDayLeftPosition(this.mNumDays);
                paint.setColor(mFutureBgColor);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(rect, paint);
            }
        }
        if (this.mSelectionAllday && this.mSelectionMode != 0) {
            this.mRect.top = DAY_HEADER_HEIGHT + 1;
            this.mRect.bottom = ((this.mRect.top + this.mAlldayHeight) + ALLDAY_TOP_MARGIN) - 2;
            int i4 = this.mSelectionDay - this.mFirstJulianDay;
            this.mRect.left = computeDayLeftPosition(i4) + 1;
            this.mRect.right = computeDayLeftPosition(i4 + 1);
            paint.setColor(mCalendarGridAreaSelected);
            canvas.drawRect(this.mRect, paint);
        }
    }

    private void drawDayHeaderLoop(Rect rect, Canvas canvas, Paint paint) {
        String[] strArr;
        if (this.mNumDays == 1 && ONE_DAY_HEADER_HEIGHT == 0) {
            return;
        }
        paint.setTypeface(this.mBold);
        paint.setTextAlign(Paint.Align.RIGHT);
        int i = this.mFirstJulianDay;
        if (this.mDateStrWidth < this.mCellWidth) {
            strArr = this.mDayStrs;
        } else {
            strArr = this.mDayStrs2Letter;
        }
        paint.setAntiAlias(true);
        int i2 = 0;
        while (i2 < this.mNumDays) {
            int i3 = this.mFirstVisibleDayOfWeek + i2;
            if (i3 >= 14) {
                i3 -= 14;
            }
            int i4 = mCalendarDateBannerTextColor;
            if (this.mNumDays == 1) {
                if (i3 == 6) {
                    i4 = mWeek_saturdayColor;
                } else if (i3 == 0) {
                    i4 = mWeek_sundayColor;
                }
            } else {
                int i5 = i2 % 7;
                if (Utils.isSaturday(i5, this.mFirstDayOfWeek)) {
                    i4 = mWeek_saturdayColor;
                } else if (Utils.isSunday(i5, this.mFirstDayOfWeek)) {
                    i4 = mWeek_sundayColor;
                }
            }
            paint.setColor(i4);
            drawDayHeader(strArr[i3], i2, i, canvas, paint);
            i2++;
            i++;
        }
        paint.setTypeface(null);
    }

    private void drawAmPm(Canvas canvas, Paint paint) {
        paint.setColor(mCalendarAmPmLabel);
        paint.setTextSize(AMPM_TEXT_SIZE);
        paint.setTypeface(this.mBold);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.LEFT);
        String str = this.mAmString;
        if (this.mFirstHour >= 12) {
            str = this.mPmString;
        }
        canvas.drawText(str, this.mAmPmLeftMargin, this.mFirstCell + this.mFirstHourOffset + (this.mHoursTextHeight * 2) + 1, paint);
        if (this.mFirstHour < 12 && this.mFirstHour + this.mNumHours > 12) {
            canvas.drawText(this.mPmString, this.mAmPmLeftMargin, this.mFirstCell + this.mFirstHourOffset + ((12 - this.mFirstHour) * (mCellHeight + 1)) + (2 * this.mHoursTextHeight) + 1, paint);
        }
    }

    private void drawCurrentTimeLine(Rect rect, int i, int i2, Canvas canvas, Paint paint) {
        rect.left = (computeDayLeftPosition(i) - CURRENT_TIME_LINE_SIDE_BUFFER) + 1;
        rect.right = computeDayLeftPosition(i + 1) + CURRENT_TIME_LINE_SIDE_BUFFER + 1;
        rect.top = i2 - CURRENT_TIME_LINE_TOP_OFFSET;
        rect.bottom = rect.top + this.mCurrentTimeLine.getIntrinsicHeight();
        this.mCurrentTimeLine.setBounds(rect);
        this.mCurrentTimeLine.draw(canvas);
        if (this.mAnimateToday) {
            this.mCurrentTimeAnimateLine.setBounds(rect);
            this.mCurrentTimeAnimateLine.setAlpha(this.mAnimateTodayAlpha);
            this.mCurrentTimeAnimateLine.draw(canvas);
        }
    }

    private void doDraw(Canvas canvas) {
        int i;
        Paint paint = this.mPaint;
        Rect rect = this.mRect;
        if (mFutureBgColor != 0) {
            drawBgColors(rect, canvas, paint);
        }
        drawGridBackground(rect, canvas, paint);
        drawHours(rect, canvas, paint);
        int i2 = this.mFirstJulianDay;
        paint.setAntiAlias(false);
        int alpha = paint.getAlpha();
        paint.setAlpha(this.mEventsAlpha);
        int i3 = i2;
        int i4 = 0;
        while (i4 < this.mNumDays) {
            if (i3 == this.mTodayJulianDay) {
                i = (this.mCurrentTime.hour * (mCellHeight + 1)) + ((this.mCurrentTime.minute * mCellHeight) / 60) + 1;
                if (i >= this.mViewStartY && i < (this.mViewStartY + this.mViewHeight) - 2) {
                    drawCurrentTimeLine(rect, i4, i, canvas, paint);
                }
            } else {
                i = 0;
            }
            drawEvents(i3, i4, 1, canvas, paint);
            if (i3 == this.mTodayJulianDay && i >= this.mViewStartY && i < (this.mViewStartY + this.mViewHeight) - 2 && this.mAnimateToday) {
                rect.left = (computeDayLeftPosition(i4) - CURRENT_TIME_LINE_SIDE_BUFFER) + 1;
                rect.right = computeDayLeftPosition(i4 + 1) + CURRENT_TIME_LINE_SIDE_BUFFER + 1;
                rect.top = i - CURRENT_TIME_LINE_TOP_OFFSET;
                rect.bottom = rect.top + this.mCurrentTimeLine.getIntrinsicHeight();
                this.mCurrentTimeAnimateLine.setBounds(rect);
                this.mCurrentTimeAnimateLine.setAlpha(this.mAnimateTodayAlpha);
                this.mCurrentTimeAnimateLine.draw(canvas);
            }
            i4++;
            i3++;
        }
        paint.setAntiAlias(true);
        paint.setAlpha(alpha);
        drawSelectedRect(rect, canvas, paint);
    }

    private void drawSelectedRect(Rect rect, Canvas canvas, Paint paint) {
        if (this.mSelectionMode == 0 || this.mSelectionAllday || !this.mIsSelectionFocusShow) {
            return;
        }
        int i = this.mSelectionDay - this.mFirstJulianDay;
        rect.top = this.mSelectionHour * (mCellHeight + 1);
        rect.bottom = rect.top + mCellHeight + 1;
        rect.left = computeDayLeftPosition(i) + 1;
        rect.right = computeDayLeftPosition(i + 1) + 1;
        saveSelectionPosition(rect.left, rect.top, rect.right, rect.bottom);
        if (this.mCalendarThemeExt.isThemeManagerEnable()) {
            paint.setColor(this.mCalendarThemeExt.getThemeColor());
            paint.setAlpha(THEME_ALPHA_GRID_AREA_SELECTED);
        } else {
            paint.setColor(mCalendarGridAreaSelected);
        }
        rect.top++;
        rect.right--;
        paint.setAntiAlias(false);
        canvas.drawRect(rect, paint);
        paint.setColor(mNewEventHintColor);
        if (this.mNumDays > 1) {
            paint.setStrokeWidth(NEW_EVENT_WIDTH);
            int i2 = rect.right - rect.left;
            int i3 = rect.left + (i2 / 2);
            int i4 = rect.top + (mCellHeight / 2);
            int iMin = Math.min(Math.min(mCellHeight, i2) - (NEW_EVENT_MARGIN * 2), NEW_EVENT_MAX_LENGTH);
            int i5 = (mCellHeight - iMin) / 2;
            int i6 = (i2 - iMin) / 2;
            float f = i4;
            canvas.drawLine(rect.left + i6, f, rect.right - i6, f, paint);
            float f2 = i3;
            canvas.drawLine(f2, rect.top + i5, f2, rect.bottom - i5, paint);
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(NEW_EVENT_HINT_FONT_SIZE);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.defaultFromStyle(1));
        canvas.drawText(this.mNewEventHintString, rect.left + EVENT_TEXT_LEFT_MARGIN, rect.top + Math.abs(paint.getFontMetrics().ascent) + EVENT_TEXT_TOP_MARGIN, paint);
    }

    private void drawHours(Rect rect, Canvas canvas, Paint paint) {
        setupHourTextPaint(paint);
        int i = this.mHoursTextHeight + 1 + HOURS_TOP_MARGIN;
        for (int i2 = 0; i2 < 24; i2++) {
            canvas.drawText(this.mHourStrs[i2], HOURS_LEFT_MARGIN, i, paint);
            i += mCellHeight + 1;
        }
    }

    private void setupHourTextPaint(Paint paint) {
        paint.setColor(mCalendarHourLabelColor);
        paint.setTextSize(HOURS_TEXT_SIZE);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setAntiAlias(true);
    }

    private void drawDayHeader(String str, int i, int i2, Canvas canvas, Paint paint) {
        int i3 = this.mFirstVisibleDate + i;
        if (i3 > this.mMonthLength) {
            i3 -= this.mMonthLength;
        }
        paint.setAntiAlias(true);
        int i4 = this.mTodayJulianDay - this.mFirstJulianDay;
        String strValueOf = String.valueOf(i3);
        if (this.mNumDays > 1) {
            float f = DAY_HEADER_FONT_SIZE;
            float f2 = DATE_HEADER_FONT_SIZE;
            if (this.mDateStrWidth < this.mCellWidth || this.mDateStrWidth2letter >= this.mCellWidth) {
            }
            float f3 = DAY_HEADER_HEIGHT - DAY_HEADER_BOTTOM_MARGIN;
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTextSize(DATE_HEADER_FONT_SIZE);
            paint.setTypeface(i4 == i ? this.mBold : Typeface.DEFAULT);
            int iMeasureText = (int) paint.measureText(str + " " + strValueOf);
            float fComputeDayLeftPosition = computeDayLeftPosition(i + 1) - (this.mCellWidth > iMeasureText ? (this.mCellWidth - iMeasureText) / 2 : 0);
            canvas.drawText(strValueOf, fComputeDayLeftPosition, f3, paint);
            int iMeasureText2 = (int) (fComputeDayLeftPosition - paint.measureText(" " + strValueOf));
            paint.setTextSize(DAY_HEADER_FONT_SIZE);
            paint.setTypeface(Typeface.DEFAULT);
            canvas.drawText(str, (float) iMeasureText2, f3, paint);
            return;
        }
        float f4 = ONE_DAY_HEADER_HEIGHT - DAY_HEADER_ONE_DAY_BOTTOM_MARGIN;
        paint.setTextAlign(Paint.Align.LEFT);
        int iComputeDayLeftPosition = computeDayLeftPosition(i) + DAY_HEADER_ONE_DAY_LEFT_MARGIN;
        paint.setTextSize(DAY_HEADER_FONT_SIZE);
        paint.setTypeface(Typeface.DEFAULT);
        float f5 = iComputeDayLeftPosition;
        canvas.drawText(str, f5, f4, paint);
        int iMeasureText3 = (int) (f5 + paint.measureText(str) + DAY_HEADER_ONE_DAY_RIGHT_MARGIN);
        paint.setTextSize(DATE_HEADER_FONT_SIZE);
        paint.setTypeface(i4 == i ? this.mBold : Typeface.DEFAULT);
        canvas.drawText(strValueOf, iMeasureText3, f4, paint);
    }

    private void drawGridBackground(Rect rect, Canvas canvas, Paint paint) {
        Paint.Style style = paint.getStyle();
        float fComputeDayLeftPosition = computeDayLeftPosition(this.mNumDays);
        float f = mCellHeight + 1;
        float f2 = ((mCellHeight + 1) * 24) + 1;
        int i = this.mHoursWidth;
        paint.setColor(mCalendarGridLineInnerHorizontalColor);
        paint.setStrokeWidth(1.0f);
        paint.setAntiAlias(false);
        int i2 = 0;
        float f3 = 0.0f;
        for (int i3 = 0; i3 <= 24; i3++) {
            this.mLines[i2 + 0] = GRID_LINE_LEFT_MARGIN;
            this.mLines[i2 + 1] = f3;
            this.mLines[i2 + 2] = fComputeDayLeftPosition;
            this.mLines[i2 + 3] = f3;
            i2 += 4;
            f3 += f;
        }
        Log.d("DayView", "drawGridBackground() arr1 [linesIndex] " + i2);
        if (mCalendarGridLineInnerVerticalColor != mCalendarGridLineInnerHorizontalColor) {
            canvas.drawLines(this.mLines, 0, i2, paint);
            Log.d("DayView", "drawGridBackground() arr3 [linesIndex] 0");
            paint.setColor(mCalendarGridLineInnerVerticalColor);
            i2 = 0;
        }
        for (int i4 = 0; i4 <= this.mNumDays; i4++) {
            float fComputeDayLeftPosition2 = computeDayLeftPosition(i4);
            this.mLines[i2 + 0] = fComputeDayLeftPosition2;
            this.mLines[i2 + 1] = 0.0f;
            this.mLines[i2 + 2] = fComputeDayLeftPosition2;
            this.mLines[i2 + 3] = f2;
            i2 += 4;
        }
        Log.d("DayView", "drawGridBackground() arr2 [linesIndex] " + i2);
        canvas.drawLines(this.mLines, 0, i2, paint);
        paint.setStyle(style);
        paint.setAntiAlias(true);
    }

    private void drawBgColors(Rect rect, Canvas canvas, Paint paint) {
        int i = this.mTodayJulianDay - this.mFirstJulianDay;
        rect.top = this.mDestRect.top;
        rect.bottom = this.mDestRect.bottom;
        rect.left = 0;
        rect.right = this.mHoursWidth;
        paint.setColor(mBgColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false);
        canvas.drawRect(rect, paint);
        if (this.mNumDays == 1 && i == 0) {
            int i2 = (this.mCurrentTime.hour * (mCellHeight + 1)) + ((this.mCurrentTime.minute * mCellHeight) / 60) + 1;
            if (i2 < this.mViewStartY + this.mViewHeight) {
                int iMax = Math.max(i2, this.mViewStartY);
                rect.left = this.mHoursWidth;
                rect.right = this.mViewWidth;
                rect.top = iMax;
                rect.bottom = this.mViewStartY + this.mViewHeight;
                paint.setColor(mFutureBgColor);
                canvas.drawRect(rect, paint);
            }
        } else if (i >= 0 && i < this.mNumDays) {
            int i3 = (this.mCurrentTime.hour * (mCellHeight + 1)) + ((this.mCurrentTime.minute * mCellHeight) / 60) + 1;
            if (i3 < this.mViewStartY + this.mViewHeight) {
                int iMax2 = Math.max(i3, this.mViewStartY);
                rect.left = computeDayLeftPosition(i) + 1;
                rect.right = computeDayLeftPosition(i + 1);
                rect.top = iMax2;
                rect.bottom = this.mViewStartY + this.mViewHeight;
                paint.setColor(mFutureBgColor);
                canvas.drawRect(rect, paint);
            }
            int i4 = i + 1;
            if (i4 < this.mNumDays) {
                rect.left = computeDayLeftPosition(i4) + 1;
                rect.right = computeDayLeftPosition(this.mNumDays);
                rect.top = this.mDestRect.top;
                rect.bottom = this.mDestRect.bottom;
                paint.setColor(mFutureBgColor);
                canvas.drawRect(rect, paint);
            }
        } else if (i < 0) {
            rect.left = computeDayLeftPosition(0) + 1;
            rect.right = computeDayLeftPosition(this.mNumDays);
            rect.top = this.mDestRect.top;
            rect.bottom = this.mDestRect.bottom;
            paint.setColor(mFutureBgColor);
            canvas.drawRect(rect, paint);
        }
        paint.setAntiAlias(true);
    }

    private int computeMaxStringWidth(int i, String[] strArr, Paint paint) {
        float fMax = 0.0f;
        for (String str : strArr) {
            fMax = Math.max(paint.measureText(str), fMax);
        }
        int i2 = (int) (((double) fMax) + 0.5d);
        return i2 < i ? i : i2;
    }

    private void saveSelectionPosition(float f, float f2, float f3, float f4) {
        this.mPrevBox.left = (int) f;
        this.mPrevBox.right = (int) f3;
        this.mPrevBox.top = (int) f2;
        this.mPrevBox.bottom = (int) f4;
    }

    private Rect getCurrentSelectionPosition() {
        Rect rect = new Rect();
        rect.top = this.mSelectionHour * (mCellHeight + 1);
        rect.bottom = rect.top + mCellHeight + 1;
        int i = this.mSelectionDay - this.mFirstJulianDay;
        rect.left = computeDayLeftPosition(i) + 1;
        rect.right = computeDayLeftPosition(i + 1);
        return rect;
    }

    private void setupTextRect(Rect rect) {
        if (rect.bottom <= rect.top || rect.right <= rect.left) {
            rect.bottom = rect.top;
            rect.right = rect.left;
            return;
        }
        if (rect.bottom - rect.top > EVENT_TEXT_TOP_MARGIN + EVENT_TEXT_BOTTOM_MARGIN) {
            rect.top += EVENT_TEXT_TOP_MARGIN;
            rect.bottom -= EVENT_TEXT_BOTTOM_MARGIN;
        }
        if (rect.right - rect.left > EVENT_TEXT_LEFT_MARGIN + EVENT_TEXT_RIGHT_MARGIN) {
            rect.left += EVENT_TEXT_LEFT_MARGIN;
            rect.right -= EVENT_TEXT_RIGHT_MARGIN;
        }
    }

    private void setupAllDayTextRect(Rect rect) {
        if (rect.bottom <= rect.top || rect.right <= rect.left) {
            rect.bottom = rect.top;
            rect.right = rect.left;
            return;
        }
        if (rect.bottom - rect.top > EVENT_ALL_DAY_TEXT_TOP_MARGIN + EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN) {
            rect.top += EVENT_ALL_DAY_TEXT_TOP_MARGIN;
            rect.bottom -= EVENT_ALL_DAY_TEXT_BOTTOM_MARGIN;
        }
        if (rect.right - rect.left > EVENT_ALL_DAY_TEXT_LEFT_MARGIN + EVENT_ALL_DAY_TEXT_RIGHT_MARGIN) {
            rect.left += EVENT_ALL_DAY_TEXT_LEFT_MARGIN;
            rect.right -= EVENT_ALL_DAY_TEXT_RIGHT_MARGIN;
        }
    }

    private StaticLayout getEventLayout(StaticLayout[] staticLayoutArr, int i, Event event, Paint paint, Rect rect) {
        StaticLayout staticLayout;
        if (i < 0 || i >= staticLayoutArr.length) {
            return null;
        }
        StaticLayout staticLayout2 = staticLayoutArr[i];
        if (staticLayout2 == null || rect.width() != staticLayout2.getWidth()) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            if (event.title != null) {
                spannableStringBuilder.append((CharSequence) drawTextSanitizer(event.title.toString(), 499));
                spannableStringBuilder.setSpan(new StyleSpan(1), 0, spannableStringBuilder.length(), 0);
                spannableStringBuilder.append(' ');
            }
            if (event.location != null) {
                spannableStringBuilder.append((CharSequence) drawTextSanitizer(event.location.toString(), 500 - spannableStringBuilder.length()));
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
            staticLayout = new StaticLayout(spannableStringBuilder, 0, spannableStringBuilder.length(), new TextPaint(paint), rect.width(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true, null, rect.width());
            staticLayoutArr[i] = staticLayout;
        } else {
            staticLayout = staticLayout2;
        }
        staticLayout.getPaint().setAlpha(this.mEventsAlpha);
        return staticLayout;
    }

    private void drawAllDayEvents(int i, int i2, Canvas canvas, Paint paint) {
        float f;
        int i3;
        boolean z;
        int i4;
        int i5;
        int i6;
        float f2;
        int i7;
        ArrayList<Event> arrayList;
        boolean z2;
        int i8 = i;
        paint.setTextSize(NORMAL_FONT_SIZE);
        paint.setTextAlign(Paint.Align.LEFT);
        Paint paint2 = this.mEventTextPaint;
        float f3 = DAY_HEADER_HEIGHT;
        float f4 = this.mAlldayHeight + f3 + ALLDAY_TOP_MARGIN;
        paint.setColor(mCalendarGridLineInnerVerticalColor);
        int i9 = this.mHoursWidth;
        paint.setStrokeWidth(1.0f);
        this.mLines[0] = GRID_LINE_LEFT_MARGIN;
        this.mLines[1] = f3;
        this.mLines[2] = computeDayLeftPosition(this.mNumDays);
        this.mLines[3] = f3;
        int i10 = 4;
        int i11 = 0;
        while (i11 <= this.mNumDays) {
            float fComputeDayLeftPosition = computeDayLeftPosition(i11);
            int i12 = i10 + 1;
            this.mLines[i10] = fComputeDayLeftPosition;
            int i13 = i12 + 1;
            this.mLines[i12] = f3;
            int i14 = i13 + 1;
            this.mLines[i13] = fComputeDayLeftPosition;
            this.mLines[i14] = f4;
            i11++;
            i10 = i14 + 1;
        }
        paint.setAntiAlias(false);
        canvas.drawLines(this.mLines, 0, i10, paint);
        paint.setStyle(Paint.Style.FILL);
        int i15 = DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN;
        int i16 = (i8 + i2) - 1;
        ArrayList<Event> arrayList2 = this.mAllDayEvents;
        int size = arrayList2.size();
        float f5 = this.mAlldayHeight;
        float f6 = this.mMaxAlldayEvents;
        int i17 = DAY_HEADER_HEIGHT + this.mAlldayHeight + ALLDAY_TOP_MARGIN;
        this.mSkippedAlldayEvents = new int[i2];
        if (this.mMaxAlldayEvents > this.mMaxUnexpandedAlldayEventCount && !mShowAllAllDayEvents && this.mAnimateDayHeight == 0) {
            f = this.mMaxUnexpandedAlldayEventCount - 1;
            i3 = (int) (i17 - MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT);
            z = true;
        } else {
            if (this.mAnimateDayHeight != 0) {
                i17 = ALLDAY_TOP_MARGIN + DAY_HEADER_HEIGHT + this.mAnimateDayHeight;
            }
            f = f6;
            i3 = i17;
            z = false;
        }
        int alpha = paint2.getAlpha();
        paint2.setAlpha(this.mEventsAlpha);
        int i18 = 0;
        while (i18 < size) {
            Event event = arrayList2.get(i18);
            int i19 = event.startDay;
            int i20 = event.endDay;
            if (i19 > i16 || i20 < i8) {
                i4 = i18;
                i5 = alpha;
                i6 = i3;
                f2 = f5;
                i7 = size;
                arrayList = arrayList2;
                z2 = true;
            } else {
                if (i19 < i8) {
                    i19 = i8;
                }
                if (i20 > i16) {
                    i20 = i16;
                }
                i4 = i18;
                int i21 = i19 - i8;
                int i22 = alpha;
                int i23 = i20 - i8;
                i7 = size;
                arrayList = arrayList2;
                float f7 = this.mMaxAlldayEvents > this.mMaxUnexpandedAlldayEventCount ? this.mAnimateDayEventHeight : f5 / f;
                if (f7 > MAX_HEIGHT_OF_ONE_ALLDAY_EVENT) {
                    f7 = MAX_HEIGHT_OF_ONE_ALLDAY_EVENT;
                }
                event.left = computeDayLeftPosition(i21);
                z2 = true;
                event.right = computeDayLeftPosition(i23 + 1) - 1;
                f2 = f5;
                event.top = i15 + (event.getColumn() * f7);
                event.bottom = (event.top + f7) - ALL_DAY_EVENT_RECT_BOTTOM_MARGIN;
                if (this.mMaxAlldayEvents <= this.mMaxUnexpandedAlldayEventCount) {
                    i5 = i22;
                    i6 = i3;
                    Rect rectDrawEventRect = drawEventRect(event, canvas, paint, paint2, (int) event.top, (int) event.bottom);
                    setupAllDayTextRect(rectDrawEventRect);
                    drawEventText(getEventLayout(this.mAllDayLayouts, i4, event, paint2, rectDrawEventRect), rectDrawEventRect, canvas, rectDrawEventRect.top, rectDrawEventRect.bottom, true);
                    if (!this.mSelectionAllday) {
                    }
                } else {
                    float f8 = i3;
                    if (event.top < f8) {
                        if (event.bottom > f8) {
                            if (z) {
                                incrementSkipCount(this.mSkippedAlldayEvents, i21, i23);
                            } else {
                                event.bottom = f8;
                            }
                        }
                        i5 = i22;
                        i6 = i3;
                        Rect rectDrawEventRect2 = drawEventRect(event, canvas, paint, paint2, (int) event.top, (int) event.bottom);
                        setupAllDayTextRect(rectDrawEventRect2);
                        drawEventText(getEventLayout(this.mAllDayLayouts, i4, event, paint2, rectDrawEventRect2), rectDrawEventRect2, canvas, rectDrawEventRect2.top, rectDrawEventRect2.bottom, true);
                        if (!this.mSelectionAllday && this.mComputeSelectedEvents && i19 <= this.mSelectionDay && i20 >= this.mSelectionDay) {
                            this.mSelectedEvents.add(event);
                        }
                    } else {
                        incrementSkipCount(this.mSkippedAlldayEvents, i21, i23);
                    }
                    i5 = i22;
                    i6 = i3;
                }
            }
            i18 = i4 + 1;
            alpha = i5;
            i3 = i6;
            size = i7;
            arrayList2 = arrayList;
            f5 = f2;
            i8 = i;
        }
        paint2.setAlpha(alpha);
        if (mMoreAlldayEventsTextAlpha != 0 && this.mSkippedAlldayEvents != null) {
            int alpha2 = paint.getAlpha();
            paint.setAlpha(this.mEventsAlpha);
            paint.setColor((mMoreAlldayEventsTextAlpha << 24) & mMoreEventsTextColor);
            for (int i24 = 0; i24 < this.mSkippedAlldayEvents.length; i24++) {
                if (this.mSkippedAlldayEvents[i24] > 0) {
                    drawMoreAlldayEvents(canvas, this.mSkippedAlldayEvents[i24], i24, paint);
                }
            }
            paint.setAlpha(alpha2);
        }
        if (this.mSelectionAllday) {
            computeAllDayNeighbors();
            saveSelectionPosition(0.0f, 0.0f, 0.0f, 0.0f);
        }
    }

    private void incrementSkipCount(int[] iArr, int i, int i2) {
        if (iArr == null || i < 0 || i2 > iArr.length) {
            return;
        }
        while (i <= i2) {
            iArr[i] = iArr[i] + 1;
            i++;
        }
    }

    protected void drawMoreAlldayEvents(Canvas canvas, int i, int i2, Paint paint) {
        int iComputeDayLeftPosition = computeDayLeftPosition(i2) + EVENT_ALL_DAY_TEXT_LEFT_MARGIN;
        int i3 = (int) (((this.mAlldayHeight - (MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT * 0.5f)) - (0.5f * EVENT_SQUARE_WIDTH)) + DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN);
        Rect rect = this.mRect;
        rect.top = i3;
        rect.left = iComputeDayLeftPosition;
        rect.bottom = EVENT_SQUARE_WIDTH + i3;
        rect.right = EVENT_SQUARE_WIDTH + iComputeDayLeftPosition;
        paint.setColor(mMoreEventsTextColor);
        paint.setStrokeWidth(EVENT_RECT_STROKE_WIDTH);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(false);
        canvas.drawRect(rect, paint);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(EVENT_TEXT_FONT_SIZE);
        String quantityString = this.mResources.getQuantityString(R.plurals.month_more_events, i);
        canvas.drawText(String.format(quantityString, Integer.valueOf(i)), iComputeDayLeftPosition + EVENT_SQUARE_WIDTH + EVENT_LINE_PADDING, i3 + EVENT_SQUARE_WIDTH, paint);
    }

    private void computeAllDayNeighbors() {
        int column;
        int size = this.mSelectedEvents.size();
        if (size == 0 || this.mSelectedEvent != null) {
            return;
        }
        for (int i = 0; i < size; i++) {
            Event event = this.mSelectedEvents.get(i);
            event.nextUp = null;
            event.nextDown = null;
            event.nextLeft = null;
            event.nextRight = null;
        }
        if (this.mPrevSelectedEvent != null && this.mPrevSelectedEvent.drawAsAllday()) {
            column = this.mPrevSelectedEvent.getColumn();
        } else {
            column = -1;
        }
        Event event2 = null;
        int i2 = -1;
        Event event3 = null;
        for (int i3 = 0; i3 < size; i3++) {
            Event event4 = this.mSelectedEvents.get(i3);
            int column2 = event4.getColumn();
            if (column2 != column) {
                if (column2 > i2) {
                    event2 = event4;
                    i2 = column2;
                }
            } else {
                event3 = event4;
            }
            for (int i4 = 0; i4 < size; i4++) {
                if (i4 != i3) {
                    Event event5 = this.mSelectedEvents.get(i4);
                    int column3 = event5.getColumn();
                    if (column3 == column2 - 1) {
                        event4.nextUp = event5;
                    } else if (column3 == column2 + 1) {
                        event4.nextDown = event5;
                    }
                }
            }
        }
        if (event3 != null) {
            setSelectedEvent(event3);
        } else {
            setSelectedEvent(event2);
        }
    }

    private void drawEvents(int i, int i2, int i3, Canvas canvas, Paint paint) {
        EventGeometry eventGeometry;
        Paint paint2 = this.mEventTextPaint;
        int iComputeDayLeftPosition = computeDayLeftPosition(i2) + 1;
        int iComputeDayLeftPosition2 = (computeDayLeftPosition(i2 + 1) - iComputeDayLeftPosition) + 1;
        int i4 = mCellHeight;
        Rect rect = this.mSelectionRect;
        rect.top = i3 + (this.mSelectionHour * (i4 + 1));
        rect.bottom = rect.top + i4;
        rect.left = iComputeDayLeftPosition;
        rect.right = rect.left + iComputeDayLeftPosition2;
        ArrayList<Event> arrayList = this.mEvents;
        int size = arrayList.size();
        EventGeometry eventGeometry2 = this.mEventGeometry;
        int i5 = ((this.mViewStartY + this.mViewHeight) - DAY_HEADER_HEIGHT) - this.mAlldayHeight;
        int alpha = paint2.getAlpha();
        paint2.setAlpha(this.mEventsAlpha);
        int i6 = 0;
        while (i6 < size) {
            Event event = arrayList.get(i6);
            int i7 = iComputeDayLeftPosition;
            int i8 = i6;
            int i9 = iComputeDayLeftPosition;
            int i10 = alpha;
            int i11 = iComputeDayLeftPosition2;
            int i12 = iComputeDayLeftPosition2;
            int i13 = i5;
            if (!eventGeometry2.computeEventRect(i, i7, i3, i11, event) || event.bottom < this.mViewStartY || event.top > i13) {
                eventGeometry = eventGeometry2;
            } else {
                if (i == this.mSelectionDay && !this.mSelectionAllday && this.mComputeSelectedEvents && eventGeometry2.eventIntersectsSelection(event, rect)) {
                    this.mSelectedEvents.add(event);
                }
                eventGeometry = eventGeometry2;
                Rect rectDrawEventRect = drawEventRect(event, canvas, paint, paint2, this.mViewStartY, i13);
                setupTextRect(rectDrawEventRect);
                if (rectDrawEventRect.top <= i13 && rectDrawEventRect.bottom >= this.mViewStartY) {
                    drawEventText(getEventLayout(this.mLayouts, i8, event, paint2, rectDrawEventRect), rectDrawEventRect, canvas, this.mViewStartY + 4, ((this.mViewStartY + this.mViewHeight) - DAY_HEADER_HEIGHT) - this.mAlldayHeight, false);
                }
            }
            i6 = i8 + 1;
            alpha = i10;
            i5 = i13;
            eventGeometry2 = eventGeometry;
            iComputeDayLeftPosition = i9;
            iComputeDayLeftPosition2 = i12;
        }
        paint2.setAlpha(alpha);
        if (i == this.mSelectionDay && !this.mSelectionAllday && isFocused() && this.mSelectionMode != 0) {
            computeNeighbors();
        }
    }

    private void computeNeighbors() {
        int i;
        int i2;
        int i3;
        int i4;
        char c;
        int i5;
        Rect rect;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        int i11;
        int i12;
        int i13;
        int i14;
        int i15;
        int i16;
        int i17;
        int i18;
        int i19;
        DayView dayView = this;
        int size = dayView.mSelectedEvents.size();
        if (size == 0 || dayView.mSelectedEvent != null) {
            return;
        }
        for (int i20 = 0; i20 < size; i20++) {
            Event event = dayView.mSelectedEvents.get(i20);
            event.nextUp = null;
            event.nextDown = null;
            event.nextLeft = null;
            event.nextRight = null;
        }
        Event event2 = dayView.mSelectedEvents.get(0);
        Rect currentSelectionPosition = getCurrentSelectionPosition();
        if (dayView.mPrevSelectedEvent != null) {
            i = (int) dayView.mPrevSelectedEvent.top;
            i2 = (int) dayView.mPrevSelectedEvent.bottom;
            i3 = (int) dayView.mPrevSelectedEvent.left;
            i4 = (int) dayView.mPrevSelectedEvent.right;
            if (i >= dayView.mPrevBox.bottom || i2 <= dayView.mPrevBox.top || i4 <= dayView.mPrevBox.left || i3 >= dayView.mPrevBox.right) {
                dayView.mPrevSelectedEvent = null;
                i = dayView.mPrevBox.top;
                i2 = dayView.mPrevBox.bottom;
                i3 = dayView.mPrevBox.left;
                i4 = dayView.mPrevBox.right;
            } else {
                if (i < dayView.mPrevBox.top) {
                    i = dayView.mPrevBox.top;
                }
                if (i2 > dayView.mPrevBox.bottom) {
                    i2 = dayView.mPrevBox.bottom;
                }
            }
        } else {
            i = dayView.mPrevBox.top;
            i2 = dayView.mPrevBox.bottom;
            i3 = dayView.mPrevBox.left;
            i4 = dayView.mPrevBox.right;
        }
        if (i3 >= currentSelectionPosition.right) {
            i5 = (i + i2) / 2;
            c = '\b';
        } else if (i4 <= currentSelectionPosition.left) {
            i5 = (i + i2) / 2;
            c = 4;
        } else if (i2 <= currentSelectionPosition.top) {
            i5 = (i3 + i4) / 2;
            c = 1;
        } else if (i >= currentSelectionPosition.bottom) {
            i5 = (i3 + i4) / 2;
            c = 2;
        } else {
            c = 0;
            i5 = 0;
        }
        int i21 = 100000;
        int i22 = 100000;
        int i23 = 0;
        Event event3 = event2;
        while (i23 < size) {
            Event event4 = dayView.mSelectedEvents.get(i23);
            int i24 = event4.startTime;
            int i25 = event4.endTime;
            int i26 = (int) event4.left;
            Event event5 = event3;
            int i27 = (int) event4.right;
            int i28 = i25;
            int i29 = (int) event4.top;
            int i30 = i24;
            if (i29 < currentSelectionPosition.top) {
                i29 = currentSelectionPosition.top;
            }
            int i31 = (int) event4.bottom;
            if (i31 > currentSelectionPosition.bottom) {
                i31 = currentSelectionPosition.bottom;
            }
            if (c == 1) {
                rect = currentSelectionPosition;
                i7 = i26 >= i5 ? i26 - i5 : i27 <= i5 ? i5 - i27 : 0;
                i6 = i29 - i2;
            } else if (c == 2) {
                i7 = i26 >= i5 ? i26 - i5 : i27 <= i5 ? i5 - i27 : 0;
                rect = currentSelectionPosition;
                i6 = i - i31;
            } else if (c == 4) {
                rect = currentSelectionPosition;
                i7 = i31 <= i5 ? i5 - i31 : i29 >= i5 ? i29 - i5 : 0;
                i6 = i26 - i4;
            } else if (c == '\b') {
                rect = currentSelectionPosition;
                i7 = i31 <= i5 ? i5 - i31 : i29 >= i5 ? i29 - i5 : 0;
                i6 = i3 - i27;
            } else {
                rect = currentSelectionPosition;
                i6 = 0;
                i7 = 0;
            }
            if (i7 < i21) {
                i22 = i6;
                i8 = i7;
                event5 = event4;
            } else {
                if (i7 == i21) {
                    i18 = i21;
                    i19 = i22;
                    if (i6 < i19) {
                    }
                } else {
                    i18 = i21;
                    i19 = i22;
                }
                i22 = i19;
                i8 = i18;
            }
            int i32 = i8;
            int i33 = i;
            int i34 = i2;
            int i35 = i3;
            int i36 = i4;
            char c2 = c;
            int i37 = i5;
            Event event6 = null;
            Event event7 = null;
            Event event8 = null;
            Event event9 = null;
            int i38 = 10000;
            int i39 = 10000;
            int i40 = 10000;
            int i41 = 10000;
            int i42 = 0;
            while (i42 < size) {
                if (i42 == i23) {
                    i9 = size;
                    i10 = i23;
                    i11 = i42;
                    i14 = i28;
                    i12 = i30;
                } else {
                    i9 = size;
                    Event event10 = dayView.mSelectedEvents.get(i42);
                    int i43 = (int) event10.left;
                    i10 = i23;
                    int i44 = (int) event10.right;
                    i11 = i42;
                    Event event11 = event7;
                    int i45 = i30;
                    if (event10.endTime <= i45) {
                        if (i43 >= i27 || i44 <= i26) {
                            i12 = i45;
                        } else {
                            int i46 = i45 - event10.endTime;
                            if (i46 < i38) {
                                i17 = i46;
                                i12 = i45;
                            } else {
                                if (i46 == i38) {
                                    i17 = i46;
                                    int i47 = (i26 + i27) / 2;
                                    i12 = i45;
                                    int i48 = (int) event9.left;
                                    i16 = i38;
                                    int i49 = (int) event9.right;
                                    if ((i44 <= i47 ? i47 - i44 : i43 >= i47 ? i43 - i47 : 0) < (i49 <= i47 ? i47 - i49 : i48 >= i47 ? i48 - i47 : 0)) {
                                    }
                                } else {
                                    i12 = i45;
                                    i16 = i38;
                                }
                                i38 = i16;
                            }
                            event9 = event10;
                            i38 = i17;
                        }
                        i13 = i38;
                        i14 = i28;
                    } else {
                        i12 = i45;
                        i13 = i38;
                        int i50 = i28;
                        if (event10.startTime < i50 || i43 >= i27 || i44 <= i26) {
                            i14 = i50;
                        } else {
                            int i51 = event10.startTime - i50;
                            if (i51 < i39) {
                                i15 = i51;
                                i14 = i50;
                            } else if (i51 == i39) {
                                int i52 = (i26 + i27) / 2;
                                i15 = i51;
                                int i53 = (int) event6.left;
                                i14 = i50;
                                int i54 = (int) event6.right;
                                if ((i44 <= i52 ? i52 - i44 : i43 >= i52 ? i43 - i52 : 0) < (i54 <= i52 ? i52 - i54 : i53 >= i52 ? i53 - i52 : 0)) {
                                }
                            }
                            event6 = event10;
                            i39 = i15;
                        }
                        i42 = i11 + 1;
                        size = i9;
                        i23 = i10;
                        i30 = i12;
                        i28 = i14;
                        dayView = this;
                    }
                    if (i43 >= i27) {
                        int i55 = (i29 + i31) / 2;
                        int i56 = (int) event10.bottom;
                        int i57 = (int) event10.top;
                        int i58 = i56 <= i55 ? i55 - i56 : i57 >= i55 ? i57 - i55 : 0;
                        if (i58 >= i41 && (i58 != i41 || i43 - i27 >= ((int) event8.left) - i27)) {
                            event10 = event8;
                        } else {
                            i41 = i58;
                        }
                        event8 = event10;
                        event7 = event11;
                        i38 = i13;
                    } else {
                        if (i44 <= i26) {
                            int i59 = (i29 + i31) / 2;
                            int i60 = (int) event10.bottom;
                            int i61 = (int) event10.top;
                            int i62 = i60 <= i59 ? i59 - i60 : i61 >= i59 ? i61 - i59 : 0;
                            if (i62 >= i40) {
                                if (i62 == i40) {
                                    event7 = event11;
                                    if (i26 - i44 < i26 - ((int) event7.right)) {
                                        i40 = i62;
                                        event7 = event10;
                                    }
                                } else {
                                    event7 = event11;
                                }
                            }
                            i42 = i11 + 1;
                            size = i9;
                            i23 = i10;
                            i30 = i12;
                            i28 = i14;
                            dayView = this;
                        } else {
                            event7 = event11;
                        }
                        i38 = i13;
                        i42 = i11 + 1;
                        size = i9;
                        i23 = i10;
                        i30 = i12;
                        i28 = i14;
                        dayView = this;
                    }
                }
                i42 = i11 + 1;
                size = i9;
                i23 = i10;
                i30 = i12;
                i28 = i14;
                dayView = this;
            }
            event4.nextUp = event9;
            event4.nextDown = event6;
            event4.nextLeft = event7;
            event4.nextRight = event8;
            i23++;
            event3 = event5;
            currentSelectionPosition = rect;
            i21 = i32;
            i = i33;
            i2 = i34;
            i3 = i35;
            i4 = i36;
            c = c2;
            i5 = i37;
            dayView = this;
        }
        setSelectedEvent(event3);
    }

    private Rect drawEventRect(Event event, Canvas canvas, Paint paint, Paint paint2, int i, int i2) {
        int declinedColorFromColor;
        int i3;
        Rect rect = this.mRect;
        rect.top = Math.max(((int) event.top) + EVENT_RECT_TOP_MARGIN, i);
        rect.bottom = Math.min(((int) event.bottom) - EVENT_RECT_BOTTOM_MARGIN, i2);
        rect.left = ((int) event.left) + EVENT_RECT_LEFT_MARGIN;
        rect.right = (int) event.right;
        if (event == this.mClickedEvent) {
            declinedColorFromColor = mClickedColor;
        } else {
            declinedColorFromColor = event.color;
        }
        switch (event.selfAttendeeStatus) {
            case 2:
                if (event != this.mClickedEvent) {
                    declinedColorFromColor = Utils.getDeclinedColorFromColor(declinedColorFromColor);
                }
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                break;
            case 3:
                if (event != this.mClickedEvent) {
                    paint.setStyle(Paint.Style.STROKE);
                }
                break;
            default:
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                break;
        }
        boolean z = false;
        paint.setAntiAlias(false);
        int iFloor = (int) Math.floor(EVENT_RECT_STROKE_WIDTH / 2.0f);
        int iCeil = (int) Math.ceil(EVENT_RECT_STROKE_WIDTH / 2.0f);
        rect.top = Math.max(((int) event.top) + EVENT_RECT_TOP_MARGIN + iFloor, i);
        rect.bottom = Math.min((((int) event.bottom) - EVENT_RECT_BOTTOM_MARGIN) - iCeil, i2);
        rect.left += iFloor;
        rect.right -= iCeil;
        paint.setStrokeWidth(EVENT_RECT_STROKE_WIDTH);
        paint.setColor(declinedColorFromColor);
        int alpha = paint.getAlpha();
        paint.setAlpha(EVENT_RECT_ALPHA);
        canvas.drawRect(rect, paint);
        paint.setAlpha(alpha);
        paint.setStyle(Paint.Style.FILL);
        if (this.mSelectedEvent == event && this.mClickedEvent != null) {
            if (this.mSelectionMode == 1 || this.mSelectionMode == 2) {
                this.mPrevSelectedEvent = event;
                int i4 = mPressedColor;
                i3 = i4;
                z = true;
                if (z) {
                }
                paint.setAntiAlias(true);
            } else {
                i3 = 0;
                if (z) {
                    paint.setColor(i3);
                    canvas.drawRect(rect, paint);
                }
                paint.setAntiAlias(true);
            }
        }
        rect.top = ((int) event.top) + EVENT_RECT_TOP_MARGIN;
        rect.bottom = ((int) event.bottom) - EVENT_RECT_BOTTOM_MARGIN;
        rect.left = ((int) event.left) + EVENT_RECT_LEFT_MARGIN;
        rect.right = ((int) event.right) - EVENT_RECT_RIGHT_MARGIN;
        return rect;
    }

    private String drawTextSanitizer(String str, int i) {
        String strReplaceAll = this.drawTextSanitizerFilter.matcher(str).replaceAll(",");
        int length = strReplaceAll.length();
        if (i <= 0) {
            strReplaceAll = "";
        } else if (length > i) {
            strReplaceAll = strReplaceAll.substring(0, i);
        }
        return strReplaceAll.replace('\n', ' ');
    }

    private void drawEventText(StaticLayout staticLayout, Rect rect, Canvas canvas, int i, int i2, boolean z) {
        int i3 = rect.right - rect.left;
        int i4 = rect.bottom - rect.top;
        if (staticLayout == null || i3 < MIN_CELL_WIDTH_FOR_TEXT) {
            return;
        }
        int lineCount = staticLayout.getLineCount();
        int i5 = 0;
        int i6 = 0;
        while (i5 < lineCount) {
            int lineBottom = staticLayout.getLineBottom(i5);
            if (lineBottom > i4) {
                break;
            }
            i5++;
            i6 = lineBottom;
        }
        if (i6 == 0 || rect.top > i2 || rect.top + i6 + 2 < i) {
            return;
        }
        canvas.save();
        canvas.translate(rect.left, rect.top + (z ? ((rect.bottom - rect.top) - i6) / 2 : 0));
        rect.left = 0;
        rect.right = i3;
        rect.top = 0;
        rect.bottom = i6;
        canvas.clipRect(rect);
        staticLayout.draw(canvas);
        canvas.restore();
    }

    private void updateEventDetails() {
        int i;
        if (this.mSelectedEvent == null || this.mSelectionMode == 0 || this.mSelectionMode == 3) {
            this.mPopup.dismiss();
            return;
        }
        if (this.mLastPopupEventID == this.mSelectedEvent.id) {
            return;
        }
        this.mLastPopupEventID = this.mSelectedEvent.id;
        this.mHandler.removeCallbacks(this.mDismissPopup);
        Event event = this.mSelectedEvent;
        ((TextView) this.mPopupView.findViewById(R.id.event_title)).setText(event.title);
        ((ImageView) this.mPopupView.findViewById(R.id.reminder_icon)).setVisibility(event.hasAlarm ? 0 : 8);
        ((ImageView) this.mPopupView.findViewById(R.id.repeat_icon)).setVisibility(event.isRepeating ? 0 : 8);
        if (event.allDay) {
            i = 532498;
        } else {
            i = 529427;
        }
        if (DateFormat.is24HourFormat(this.mContext)) {
            i |= 128;
        }
        ((TextView) this.mPopupView.findViewById(R.id.time)).setText(Utils.formatDateRange(this.mContext, event.startMillis, event.endMillis, i));
        TextView textView = (TextView) this.mPopupView.findViewById(R.id.where);
        boolean zIsEmpty = TextUtils.isEmpty(event.location);
        textView.setVisibility(zIsEmpty ? 8 : 0);
        if (!zIsEmpty) {
            textView.setText(event.location);
        }
        this.mPopup.showAtLocation(this, 83, this.mHoursWidth, 5);
        this.mHandler.postDelayed(this.mDismissPopup, 3000L);
    }

    private void doDown(MotionEvent motionEvent) {
        boolean z = true;
        this.mTouchMode = 1;
        this.mViewStartX = 0;
        this.mOnFlingCalled = false;
        this.mHandler.removeCallbacks(this.mContinueScroll);
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        Event event = this.mSelectedEvent;
        int i = this.mSelectionDay;
        int i2 = this.mSelectionHour;
        if (setSelectionFromPosition(x, y, false)) {
            if (this.mSelectionMode == 0 || i != this.mSelectionDay || i2 != this.mSelectionHour) {
                z = false;
            }
            if (!z && this.mSelectedEvent != null) {
                this.mSavedClickedEvent = this.mSelectedEvent;
                this.mDownTouchTime = System.currentTimeMillis();
                postDelayed(this.mSetClick, mOnDownDelay);
            } else {
                eventClickCleanup();
            }
        }
        this.mSelectedEvent = event;
        this.mSelectionDay = i;
        this.mSelectionHour = i2;
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
        if (this.mMaxAlldayEvents <= this.mMaxUnexpandedAlldayEventCount) {
            return;
        }
        if (mShowAllAllDayEvents) {
            this.mAnimateDayEventHeight = Math.min((this.mViewHeight - DAY_HEADER_HEIGHT) - MIN_HOURS_HEIGHT, (int) (this.mMaxAlldayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT)) / this.mMaxAlldayEvents;
        } else {
            this.mAnimateDayEventHeight = (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;
        }
    }

    private ObjectAnimator getAllDayEventAnimator() {
        int iMin = Math.min((this.mViewHeight - DAY_HEADER_HEIGHT) - MIN_HOURS_HEIGHT, (int) (this.mMaxAlldayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT)) / this.mMaxAlldayEvents;
        int i = this.mAnimateDayEventHeight;
        if (!mShowAllAllDayEvents) {
            iMin = (int) MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT;
        }
        if (i == iMin) {
            return null;
        }
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "animateDayEventHeight", i, iMin);
        objectAnimatorOfInt.setDuration(400L);
        return objectAnimatorOfInt;
    }

    private ObjectAnimator getAllDayAnimator() {
        int iMin = Math.min((this.mViewHeight - DAY_HEADER_HEIGHT) - MIN_HOURS_HEIGHT, (int) (this.mMaxAlldayEvents * MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT));
        int i = this.mAnimateDayHeight != 0 ? this.mAnimateDayHeight : this.mAlldayHeight;
        if (!mShowAllAllDayEvents) {
            iMin = (int) ((MAX_UNEXPANDED_ALLDAY_HEIGHT - MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT) - 1.0f);
        }
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "animateDayHeight", i, iMin);
        objectAnimatorOfInt.setDuration(400L);
        objectAnimatorOfInt.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (!DayView.this.mCancellingAnimations) {
                    DayView.this.mAnimateDayHeight = 0;
                    boolean unused = DayView.mUseExpandIcon = !DayView.mShowAllAllDayEvents;
                }
                DayView.this.mRemeasure = true;
                DayView.this.invalidate();
            }
        });
        return objectAnimatorOfInt;
    }

    public void setMoreAllDayEventsTextAlpha(int i) {
        mMoreAlldayEventsTextAlpha = i;
        invalidate();
    }

    public void setAnimateDayHeight(int i) {
        this.mAnimateDayHeight = i;
        this.mRemeasure = true;
        invalidate();
    }

    public void setAnimateDayEventHeight(int i) {
        this.mAnimateDayEventHeight = i;
        this.mRemeasure = true;
        invalidate();
    }

    private void doSingleTapUp(MotionEvent motionEvent) {
        if (!this.mHandleActionUp || this.mScrolling) {
            return;
        }
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        int i = this.mSelectionDay;
        int i2 = this.mSelectionHour;
        if (this.mMaxAlldayEvents > this.mMaxUnexpandedAlldayEventCount) {
            int i3 = this.mFirstCell;
            if ((x < this.mHoursWidth && y > DAY_HEADER_HEIGHT && y < DAY_HEADER_HEIGHT + this.mAlldayHeight) || (!mShowAllAllDayEvents && this.mAnimateDayHeight == 0 && y < i3 && y >= i3 - MIN_UNEXPANDED_ALLDAY_EVENT_HEIGHT)) {
                doExpandAllDayClick();
                return;
            }
        }
        if (!setSelectionFromPosition(x, y, false)) {
            if (y < DAY_HEADER_HEIGHT) {
                Time time = new Time(this.mBaseDate);
                Utils.setJulianDayInGeneral(time, this.mSelectionDay);
                time.hour = this.mSelectionHour;
                time.normalize(true);
                this.mController.sendEvent(this, 32L, null, null, time, -1L, 2, 1L, null, null);
                return;
            }
            return;
        }
        if ((((this.mSelectionMode != 0) || this.mTouchExplorationEnabled) && i == this.mSelectionDay && i2 == this.mSelectionHour) && this.mSavedClickedEvent == null) {
            long j = this.mSelectionAllday ? 16L : 0L;
            this.mSelectionMode = 2;
            getSelectedTimeInMillis();
            this.mController.sendEventRelatedEventWithExtra(this, 1L, -1L, getSelectedTimeInMillis(), 0L, (int) motionEvent.getRawX(), (int) motionEvent.getRawY(), j, -1L);
        } else if (this.mSelectedEvent != null) {
            if (this.mIsAccessibilityEnabled) {
                this.mAccessibilityMgr.interrupt();
            }
            this.mSelectionMode = 0;
            int i4 = (int) ((this.mSelectedEvent.top + this.mSelectedEvent.bottom) / 2.0f);
            if (!this.mSelectedEvent.allDay) {
                i4 += this.mFirstCell - this.mViewStartY;
            }
            this.mClickedYLocation = i4;
            long jCurrentTimeMillis = ((long) (50 + mOnDownDelay)) - (System.currentTimeMillis() - this.mDownTouchTime);
            if (jCurrentTimeMillis > 0) {
                postDelayed(this.mClearClick, jCurrentTimeMillis);
            } else {
                post(this.mClearClick);
            }
        } else {
            Time time2 = new Time(this.mBaseDate);
            Utils.setJulianDayInGeneral(time2, this.mSelectionDay);
            time2.hour = this.mSelectionHour;
            time2.normalize(true);
            Time time3 = new Time(time2);
            time3.hour++;
            this.mSelectionMode = 2;
            this.mController.sendEvent(this, 32L, time2, time3, -1L, 0, 2L, null, null);
        }
        invalidate();
    }

    private void doLongPress(MotionEvent motionEvent) {
        eventClickCleanup();
        if (this.mScrolling || this.mStartingSpanY != 0.0f || !setSelectionFromPosition((int) motionEvent.getX(), (int) motionEvent.getY(), false)) {
            return;
        }
        this.mSelectionMode = 3;
        invalidate();
        performLongClick();
    }

    private void doScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        int i;
        cancelAnimation();
        if (this.mStartingScroll) {
            this.mInitialScrollX = 0.0f;
            this.mInitialScrollY = 0.0f;
            this.mStartingScroll = false;
        }
        this.mInitialScrollX += f;
        this.mInitialScrollY += f2;
        int i2 = (int) this.mInitialScrollX;
        int i3 = (int) this.mInitialScrollY;
        float averageY = getAverageY(motionEvent2);
        if (this.mRecalCenterHour) {
            this.mGestureCenterHour = (((this.mViewStartY + averageY) - DAY_HEADER_HEIGHT) - this.mAlldayHeight) / (mCellHeight + 1);
            this.mRecalCenterHour = false;
        }
        if (this.mTouchMode == 1) {
            int iAbs = Math.abs(i2);
            int iAbs2 = Math.abs(i3);
            this.mScrollStartY = this.mViewStartY;
            this.mPreviousDirection = 0;
            if (iAbs > iAbs2) {
                if (iAbs > mScaledPagingTouchSlop * (this.mScaleGestureDetector.isInProgress() ? 20 : 2)) {
                    this.mTouchMode = 64;
                    this.mViewStartX = i2;
                    initNextView(-this.mViewStartX);
                }
            } else {
                this.mTouchMode = 32;
            }
        } else if ((this.mTouchMode & 64) != 0) {
            this.mViewStartX = i2;
            if (i2 != 0) {
                if (i2 <= 0) {
                    i = -1;
                } else {
                    i = 1;
                }
                if (i != this.mPreviousDirection) {
                    initNextView(-this.mViewStartX);
                    this.mPreviousDirection = i;
                }
            }
        }
        if ((this.mTouchMode & 32) != 0) {
            this.mViewStartY = (int) (((this.mGestureCenterHour * (mCellHeight + 1)) - averageY) + DAY_HEADER_HEIGHT + this.mAlldayHeight);
            int i4 = (int) (this.mScrollStartY + f2);
            if (i4 < 0) {
                this.mEdgeEffectTop.onPull(f2 / this.mViewHeight);
                if (!this.mEdgeEffectBottom.isFinished()) {
                    this.mEdgeEffectBottom.onRelease();
                }
            } else if (i4 > this.mMaxViewStartY) {
                this.mEdgeEffectBottom.onPull(f2 / this.mViewHeight);
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
                this.mGestureCenterHour = (((this.mViewStartY + averageY) - DAY_HEADER_HEIGHT) - this.mAlldayHeight) / (mCellHeight + 1);
                this.mRecalCenterHour = false;
            }
            computeFirstHour();
        }
        this.mScrolling = true;
        this.mSelectionMode = 0;
        invalidate();
    }

    private float getAverageY(MotionEvent motionEvent) {
        int pointerCount = motionEvent.getPointerCount();
        float y = 0.0f;
        for (int i = 0; i < pointerCount; i++) {
            y += motionEvent.getY(i);
        }
        return y / pointerCount;
    }

    private void cancelAnimation() {
        Animation inAnimation = this.mViewSwitcher.getInAnimation();
        if (inAnimation != null) {
            inAnimation.scaleCurrentDuration(0.0f);
        }
        Animation outAnimation = this.mViewSwitcher.getOutAnimation();
        if (outAnimation != null) {
            outAnimation.scaleCurrentDuration(0.0f);
        }
    }

    private void doFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        cancelAnimation();
        this.mSelectionMode = 0;
        eventClickCleanup();
        boolean z = true;
        this.mOnFlingCalled = true;
        if ((this.mTouchMode & 64) != 0) {
            this.mTouchMode = 0;
            if (((int) motionEvent2.getX()) - ((int) motionEvent.getX()) >= 0) {
                z = false;
            }
            switchViews(z, this.mViewStartX, this.mViewWidth, f);
            this.mViewStartX = 0;
            return;
        }
        if ((this.mTouchMode & 32) == 0) {
            return;
        }
        this.mTouchMode = 0;
        this.mViewStartX = 0;
        this.mScrolling = true;
        this.mScroller.fling(0, this.mViewStartY, 0, (int) (-f2), 0, 0, 0, this.mMaxViewStartY, this.OVERFLING_DISTANCE, this.OVERFLING_DISTANCE);
        if (f2 > 0.0f && this.mViewStartY != 0) {
            this.mCallEdgeEffectOnAbsorb = true;
        } else if (f2 < 0.0f && this.mViewStartY != this.mMaxViewStartY) {
            this.mCallEdgeEffectOnAbsorb = true;
        }
        this.mHandler.post(this.mContinueScroll);
    }

    private boolean initNextView(int i) {
        boolean z;
        DayView dayView = (DayView) this.mViewSwitcher.getNextView();
        Time time = dayView.mBaseDate;
        time.set(this.mBaseDate);
        if (i > 0) {
            time.monthDay -= this.mNumDays;
            dayView.setSelectedDay(this.mSelectionDay - this.mNumDays);
            z = false;
        } else {
            time.monthDay += this.mNumDays;
            dayView.setSelectedDay(this.mSelectionDay + this.mNumDays);
            z = true;
        }
        time.normalize(true);
        initView(dayView);
        dayView.layout(getLeft(), getTop(), getRight(), getBottom());
        dayView.reloadEvents();
        return z;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        this.mHandleActionUp = false;
        this.mGestureCenterHour = (this.mViewStartY + ((scaleGestureDetector.getFocusY() - DAY_HEADER_HEIGHT) - this.mAlldayHeight)) / (mCellHeight + 1);
        this.mStartingSpanY = Math.max(MIN_Y_SPAN, Math.abs(scaleGestureDetector.getCurrentSpanY()));
        this.mCellHeightBeforeScaleGesture = mCellHeight;
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        float fMax = Math.max(MIN_Y_SPAN, Math.abs(scaleGestureDetector.getCurrentSpanY()));
        mCellHeight = (int) ((this.mCellHeightBeforeScaleGesture * fMax) / this.mStartingSpanY);
        if (mCellHeight < mMinCellHeight) {
            this.mStartingSpanY = fMax;
            mCellHeight = mMinCellHeight;
            this.mCellHeightBeforeScaleGesture = mMinCellHeight;
        } else if (mCellHeight > MAX_CELL_HEIGHT) {
            this.mStartingSpanY = fMax;
            mCellHeight = MAX_CELL_HEIGHT;
            this.mCellHeightBeforeScaleGesture = MAX_CELL_HEIGHT;
        }
        this.mViewStartY = ((int) (this.mGestureCenterHour * (mCellHeight + 1))) - ((((int) scaleGestureDetector.getFocusY()) - DAY_HEADER_HEIGHT) - this.mAlldayHeight);
        this.mMaxViewStartY = ((24 * (mCellHeight + 1)) + 1) - this.mGridAreaHeight;
        if (this.mViewStartY < 0) {
            this.mViewStartY = 0;
            this.mGestureCenterHour = (this.mViewStartY + r4) / (mCellHeight + 1);
        } else if (this.mViewStartY > this.mMaxViewStartY) {
            this.mViewStartY = this.mMaxViewStartY;
            this.mGestureCenterHour = (this.mViewStartY + r4) / (mCellHeight + 1);
        }
        computeFirstHour();
        this.mRemeasure = true;
        invalidate();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        this.mScrollStartY = this.mViewStartY;
        this.mInitialScrollY = 0.0f;
        this.mInitialScrollX = 0.0f;
        this.mStartingSpanY = 0.0f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (motionEvent.getActionMasked() == 0 || motionEvent.getActionMasked() == 1 || motionEvent.getActionMasked() == 6 || motionEvent.getActionMasked() == 5) {
            this.mRecalCenterHour = true;
        }
        if ((this.mTouchMode & 64) == 0) {
            this.mScaleGestureDetector.onTouchEvent(motionEvent);
        }
        switch (action) {
            case 0:
                this.mStartingScroll = true;
                if (motionEvent.getY() < this.mAlldayHeight + DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN) {
                    this.mTouchStartedInAlldayArea = true;
                } else {
                    this.mTouchStartedInAlldayArea = false;
                }
                this.mHandleActionUp = true;
                this.mGestureDetector.onTouchEvent(motionEvent);
                break;
            case 1:
                this.mEdgeEffectTop.onRelease();
                this.mEdgeEffectBottom.onRelease();
                this.mStartingScroll = false;
                this.mGestureDetector.onTouchEvent(motionEvent);
                if (!this.mHandleActionUp) {
                    this.mHandleActionUp = true;
                    this.mScrolling = false;
                    this.mViewStartX = 0;
                    invalidate();
                    break;
                } else if (!this.mOnFlingCalled) {
                    if (this.mScrolling) {
                        this.mScrolling = false;
                        resetSelectedHour();
                        invalidate();
                    }
                    if ((this.mTouchMode & 64) != 0) {
                        this.mTouchMode = 0;
                        if (Math.abs(this.mViewStartX) > mHorizontalSnapBackThreshold) {
                            switchViews(this.mViewStartX > 0, this.mViewStartX, this.mViewWidth, 0.0f);
                            this.mViewStartX = 0;
                        } else {
                            recalc();
                            invalidate();
                            this.mViewStartX = 0;
                        }
                    }
                    break;
                }
                break;
            case 2:
                this.mGestureDetector.onTouchEvent(motionEvent);
                break;
            case 3:
                this.mGestureDetector.onTouchEvent(motionEvent);
                this.mScrolling = false;
                resetSelectedHour();
                break;
            default:
                if (!this.mGestureDetector.onTouchEvent(motionEvent)) {
                    break;
                }
                break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        if (this.mSelectionMode != 3) {
            this.mSelectionMode = 3;
            invalidate();
        }
        long selectedTimeInMillis = getSelectedTimeInMillis();
        contextMenu.setHeaderTitle(Utils.formatDateRange(this.mContext, selectedTimeInMillis, selectedTimeInMillis, 5123));
        int size = this.mSelectedEvents.size();
        if (this.mNumDays == 1) {
            if (size >= 1) {
                MenuItem menuItemAdd = contextMenu.add(0, 5, 0, R.string.event_view);
                menuItemAdd.setOnMenuItemClickListener(this.mContextMenuHandler);
                menuItemAdd.setIcon(android.R.drawable.ic_menu_info_details);
                int eventAccessLevel = getEventAccessLevel(this.mContext, this.mSelectedEvent);
                if (eventAccessLevel == 2) {
                    MenuItem menuItemAdd2 = contextMenu.add(0, 7, 0, R.string.event_edit);
                    menuItemAdd2.setOnMenuItemClickListener(this.mContextMenuHandler);
                    menuItemAdd2.setIcon(android.R.drawable.ic_menu_edit);
                    menuItemAdd2.setAlphabeticShortcut('e');
                }
                if (eventAccessLevel >= 1) {
                    MenuItem menuItemAdd3 = contextMenu.add(0, 8, 0, R.string.event_delete);
                    menuItemAdd3.setOnMenuItemClickListener(this.mContextMenuHandler);
                    menuItemAdd3.setIcon(android.R.drawable.ic_menu_delete);
                }
                MenuItem menuItemAdd4 = contextMenu.add(0, 6, 0, R.string.event_create);
                menuItemAdd4.setOnMenuItemClickListener(this.mContextMenuHandler);
                menuItemAdd4.setIcon(android.R.drawable.ic_menu_add);
                menuItemAdd4.setAlphabeticShortcut('n');
            } else {
                MenuItem menuItemAdd5 = contextMenu.add(0, 6, 0, R.string.event_create);
                menuItemAdd5.setOnMenuItemClickListener(this.mContextMenuHandler);
                menuItemAdd5.setIcon(android.R.drawable.ic_menu_add);
                menuItemAdd5.setAlphabeticShortcut('n');
            }
        } else {
            if (size >= 1) {
                MenuItem menuItemAdd6 = contextMenu.add(0, 5, 0, R.string.event_view);
                menuItemAdd6.setOnMenuItemClickListener(this.mContextMenuHandler);
                menuItemAdd6.setIcon(android.R.drawable.ic_menu_info_details);
                int eventAccessLevel2 = getEventAccessLevel(this.mContext, this.mSelectedEvent);
                if (eventAccessLevel2 == 2) {
                    MenuItem menuItemAdd7 = contextMenu.add(0, 7, 0, R.string.event_edit);
                    menuItemAdd7.setOnMenuItemClickListener(this.mContextMenuHandler);
                    menuItemAdd7.setIcon(android.R.drawable.ic_menu_edit);
                    menuItemAdd7.setAlphabeticShortcut('e');
                }
                if (eventAccessLevel2 >= 1) {
                    MenuItem menuItemAdd8 = contextMenu.add(0, 8, 0, R.string.event_delete);
                    menuItemAdd8.setOnMenuItemClickListener(this.mContextMenuHandler);
                    menuItemAdd8.setIcon(android.R.drawable.ic_menu_delete);
                }
                if (MTKUtils.isEventShareAvailable(this.mContext)) {
                    MenuItem menuItemAdd9 = contextMenu.add(0, 9, 0, R.string.shareEvent);
                    menuItemAdd9.setOnMenuItemClickListener(this.mContextMenuHandler);
                    menuItemAdd9.setIcon(android.R.drawable.ic_menu_add);
                    menuItemAdd9.setAlphabeticShortcut('s');
                }
            }
            MenuItem menuItemAdd10 = contextMenu.add(0, 6, 0, R.string.event_create);
            menuItemAdd10.setOnMenuItemClickListener(this.mContextMenuHandler);
            menuItemAdd10.setIcon(android.R.drawable.ic_menu_add);
            menuItemAdd10.setAlphabeticShortcut('n');
            MenuItem menuItemAdd11 = contextMenu.add(0, 3, 0, R.string.show_day_view);
            menuItemAdd11.setOnMenuItemClickListener(this.mContextMenuHandler);
            menuItemAdd11.setIcon(android.R.drawable.ic_menu_day);
            menuItemAdd11.setAlphabeticShortcut('d');
        }
        this.mPopup.dismiss();
    }

    private class ContextMenuHandler implements MenuItem.OnMenuItemClickListener {
        private ContextMenuHandler() {
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
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
                        return true;
                    }
                    return true;
                case 6:
                    DayView.this.getSelectedTimeInMillis();
                    long j = 0;
                    if (DayView.this.mSelectionAllday) {
                        j = 16;
                    }
                    DayView.this.mController.sendEventRelatedEventWithExtra(this, 1L, -1L, DayView.this.getSelectedTimeInMillis(), 0L, -1, -1, j, -1L);
                    return true;
                case 7:
                    if (DayView.this.mSelectedEvent != null) {
                        DayView.this.mController.sendEventRelatedEvent(this, 8L, DayView.this.mSelectedEvent.id, DayView.this.mSelectedEvent.startMillis, DayView.this.mSelectedEvent.endMillis, 0, 0, -1L);
                        return true;
                    }
                    return true;
                case 8:
                    if (DayView.this.mSelectedEvent != null) {
                        Event event = DayView.this.mSelectedEvent;
                        long j2 = event.startMillis;
                        long j3 = event.endMillis;
                        DayView.this.mController.sendEventRelatedEvent(this, 16L, event.id, j2, j3, 0, 0, -1L);
                        return true;
                    }
                    return true;
                case 9:
                    if (DayView.this.mSelectedEvent != null) {
                        MTKUtils.sendShareEvent(DayView.this.getContext(), DayView.this.mSelectedEvent.id);
                        return true;
                    }
                    return true;
            }
        }
    }

    private static int getEventAccessLevel(Context context, Event event) {
        String string;
        int i;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursorQuery = contentResolver.query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id), new String[]{"calendar_id"}, null, null, null);
        if (cursorQuery == null) {
            return 0;
        }
        if (cursorQuery.getCount() == 0) {
            cursorQuery.close();
            return 0;
        }
        cursorQuery.moveToFirst();
        long j = cursorQuery.getLong(0);
        cursorQuery.close();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String str = "_id=" + j;
        LogUtil.v("DayView", "getEventAccessLevel, query " + uri + " , " + str);
        Cursor cursorQuery2 = contentResolver.query(uri, CALENDARS_PROJECTION, str, null, null);
        if (cursorQuery2 != null) {
            cursorQuery2.moveToFirst();
            i = cursorQuery2.getInt(1);
            string = cursorQuery2.getString(2);
            cursorQuery2.close();
        } else {
            string = null;
            i = 0;
        }
        if (i < 500) {
            return 0;
        }
        if (event.guestsCanModify) {
            return 2;
        }
        return (TextUtils.isEmpty(string) || !string.equalsIgnoreCase(event.organizer)) ? 1 : 2;
    }

    private boolean setSelectionFromPosition(int i, int i2, boolean z) {
        Event event;
        int i3;
        int i4;
        boolean z2;
        if (z) {
            event = this.mSelectedEvent;
            i3 = this.mSelectionDay;
            i4 = this.mSelectionHour;
            z2 = this.mSelectionAllday;
        } else {
            event = null;
            i3 = 0;
            i4 = 0;
            z2 = false;
        }
        if (i < this.mHoursWidth) {
            i = this.mHoursWidth;
        }
        int i5 = (i - this.mHoursWidth) / (this.mCellWidth + 1);
        if (i5 >= this.mNumDays) {
            i5 = this.mNumDays - 1;
        }
        setSelectedDay(i5 + this.mFirstJulianDay);
        if (i2 < DAY_HEADER_HEIGHT) {
            sendAccessibilityEventAsNeeded(false);
            return false;
        }
        setSelectedHour(this.mFirstHour);
        if (i2 < this.mFirstCell) {
            this.mSelectionAllday = true;
        } else {
            int i6 = i2 - this.mFirstCell;
            if (i6 < this.mFirstHourOffset) {
                setSelectedHour(this.mSelectionHour - 1);
            } else {
                setSelectedHour(this.mSelectionHour + ((i6 - this.mFirstHourOffset) / (mCellHeight + 1)));
            }
            this.mSelectionAllday = false;
        }
        findSelectedEvent(i, i2);
        sendAccessibilityEventAsNeeded(true);
        if (z) {
            this.mSelectedEvent = event;
            this.mSelectionDay = i3;
            this.mSelectionHour = i4;
            this.mSelectionAllday = z2;
        }
        return true;
    }

    private void findSelectedEvent(int i, int i2) {
        int i3;
        float f;
        int i4 = this.mSelectionDay;
        int i5 = this.mCellWidth;
        ArrayList<Event> arrayList = this.mEvents;
        int size = arrayList.size();
        int iComputeDayLeftPosition = computeDayLeftPosition(this.mSelectionDay - this.mFirstJulianDay);
        Event event = null;
        setSelectedEvent(null);
        this.mSelectedEvents.clear();
        if (!this.mSelectionAllday) {
            int i6 = i2 + (this.mViewStartY - this.mFirstCell);
            Rect rect = this.mRect;
            rect.left = i - 10;
            rect.right = i + 10;
            rect.top = i6 - 10;
            rect.bottom = i6 + 10;
            EventGeometry eventGeometry = this.mEventGeometry;
            int i7 = 0;
            while (i7 < size) {
                Event event2 = arrayList.get(i7);
                int i8 = i7;
                EventGeometry eventGeometry2 = eventGeometry;
                Rect rect2 = rect;
                if (eventGeometry.computeEventRect(i4, iComputeDayLeftPosition, 0, i5, event2) && eventGeometry2.eventIntersectsSelection(event2, rect2)) {
                    this.mSelectedEvents.add(event2);
                }
                i7 = i8 + 1;
                eventGeometry = eventGeometry2;
                rect = rect2;
            }
            EventGeometry eventGeometry3 = eventGeometry;
            if (this.mSelectedEvents.size() > 0) {
                int size2 = this.mSelectedEvents.size();
                float f2 = this.mViewWidth + this.mViewHeight;
                Event event3 = null;
                for (int i9 = 0; i9 < size2; i9++) {
                    Event event4 = this.mSelectedEvents.get(i9);
                    float fPointToEvent = eventGeometry3.pointToEvent(i, i6, event4);
                    if (fPointToEvent < f2) {
                        event3 = event4;
                        f2 = fPointToEvent;
                    }
                }
                setSelectedEvent(event3);
                int i10 = this.mSelectedEvent.startDay;
                int i11 = this.mSelectedEvent.endDay;
                if (this.mSelectionDay < i10) {
                    setSelectedDay(i10);
                } else if (this.mSelectionDay > i11) {
                    setSelectedDay(i11);
                }
                int i12 = this.mSelectedEvent.startTime / 60;
                if (this.mSelectedEvent.startTime < this.mSelectedEvent.endTime) {
                    i3 = (this.mSelectedEvent.endTime - 1) / 60;
                } else {
                    i3 = this.mSelectedEvent.endTime / 60;
                }
                if (this.mSelectionHour < i12 && this.mSelectionDay == i10) {
                    setSelectedHour(i12);
                    return;
                } else {
                    if (this.mSelectionHour > i3 && this.mSelectionDay == i11) {
                        setSelectedHour(i3);
                        return;
                    }
                    return;
                }
            }
            return;
        }
        float f3 = 10000.0f;
        float f4 = this.mAlldayHeight;
        int i13 = DAY_HEADER_HEIGHT + ALLDAY_TOP_MARGIN;
        int i14 = this.mMaxUnexpandedAlldayEventCount;
        if (this.mMaxAlldayEvents > this.mMaxUnexpandedAlldayEventCount) {
            i14--;
        }
        ArrayList<Event> arrayList2 = this.mAllDayEvents;
        int size3 = arrayList2.size();
        int i15 = 0;
        while (true) {
            if (i15 >= size3) {
                break;
            }
            Event event5 = arrayList2.get(i15);
            if (event5.drawAsAllday() && ((mShowAllAllDayEvents || event5.getColumn() < i14) && event5.startDay <= this.mSelectionDay && event5.endDay >= this.mSelectionDay)) {
                float f5 = f4 / (mShowAllAllDayEvents ? this.mMaxAlldayEvents : this.mMaxUnexpandedAlldayEventCount);
                if (f5 > MAX_HEIGHT_OF_ONE_ALLDAY_EVENT) {
                    f5 = MAX_HEIGHT_OF_ONE_ALLDAY_EVENT;
                }
                float column = i13 + (event5.getColumn() * f5);
                float f6 = f5 + column;
                float f7 = i2;
                if (column < f7 && f6 > f7) {
                    this.mSelectedEvents.add(event5);
                    event = event5;
                    break;
                }
                if (column >= f7) {
                    f = column - f7;
                } else {
                    f = f7 - f6;
                }
                if (f < f3) {
                    event = event5;
                    f3 = f;
                }
            }
            i15++;
        }
        setSelectedEvent(event);
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

    private void setSelectedEvent(Event event) {
        this.mSelectedEvent = event;
        this.mSelectedEventForAccessibility = event;
    }

    private void setSelectedHour(int i) {
        this.mSelectionHour = i;
        this.mSelectionHourForAccessibility = i;
    }

    private void setSelectedDay(int i) {
        this.mSelectionDay = i;
        this.mSelectionDayForAccessibility = i;
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
            long jCurrentTimeMillis = System.currentTimeMillis();
            DayView.this.mCurrentTime.set(jCurrentTimeMillis);
            if (!DayView.this.mPaused) {
                DayView.this.mHandler.postDelayed(DayView.this.mUpdateCurrentTime, 300000 - (jCurrentTimeMillis % 300000));
            }
            DayView.this.mTodayJulianDay = Time.getJulianDay(jCurrentTimeMillis, DayView.this.mCurrentTime.gmtoff);
            DayView.this.invalidate();
        }
    }

    class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
        CalendarGestureListener() {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            DayView.this.doSingleTapUp(motionEvent);
            DayView.this.selectionFocusShow(true);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
            DayView.this.doLongPress(motionEvent);
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            DayView.this.eventClickCleanup();
            if (DayView.this.mTouchStartedInAlldayArea) {
                if (Math.abs(f) < Math.abs(f2)) {
                    DayView.this.invalidate();
                    return false;
                }
                f2 = 0.0f;
            }
            DayView.this.doScroll(motionEvent, motionEvent2, f, f2);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            if (DayView.this.mTouchStartedInAlldayArea) {
                if (Math.abs(f) < Math.abs(f2)) {
                    return false;
                }
                f2 = 0.0f;
            }
            DayView.this.doFling(motionEvent, motionEvent2, f, f2);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            DayView.this.doDown(motionEvent);
            return true;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        int i;
        long selectedTimeInMillis = getSelectedTimeInMillis();
        if (!this.mSelectionAllday) {
            i = 3;
        } else {
            i = 2;
        }
        if (DateFormat.is24HourFormat(this.mContext)) {
            i |= 128;
        }
        this.mLongPressTitle = Utils.formatDateRange(this.mContext, selectedTimeInMillis, selectedTimeInMillis, i);
        new AlertDialog.Builder(this.mContext).setTitle(this.mLongPressTitle).setItems(this.mLongPressItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                if (i2 == 0) {
                    long j = 0;
                    if (DayView.this.mSelectionAllday) {
                        j = 16;
                    }
                    DayView.this.mController.sendEventRelatedEventWithExtra(this, 1L, -1L, DayView.this.getSelectedTimeInMillis(), 0L, -1, -1, j, -1L);
                }
            }
        }).show().setCanceledOnTouchOutside(true);
        return true;
    }

    private class ScrollInterpolator implements Interpolator {
        public ScrollInterpolator() {
        }

        @Override
        public float getInterpolation(float f) {
            float f2 = f - 1.0f;
            float f3 = (f2 * f2 * f2 * f2 * f2) + 1.0f;
            if ((1.0f - f3) * DayView.this.mAnimationDistance < 1.0f) {
                DayView.this.cancelAnimation();
            }
            return f3;
        }
    }

    private long calculateDuration(float f, float f2, float f3) {
        float f4 = f2 / 2.0f;
        return 6 * Math.round(1000.0f * Math.abs((f4 + (distanceInfluenceForSnapDuration(f / f2) * f4)) / Math.max(2200.0f, Math.abs(f3))));
    }

    private float distanceInfluenceForSnapDuration(float f) {
        return (float) Math.sin((float) (((double) (f - 0.5f)) * 0.4712389167638204d));
    }

    public void selectionFocusShow(boolean z) {
        this.mIsSelectionFocusShow = z;
    }
}
