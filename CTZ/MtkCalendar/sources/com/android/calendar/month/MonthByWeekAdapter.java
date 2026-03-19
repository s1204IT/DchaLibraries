package com.android.calendar.month;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import com.android.calendar.CalendarController;
import com.android.calendar.Event;
import com.android.calendar.R;
import com.android.calendar.Utils;
import java.util.ArrayList;
import java.util.HashMap;

public class MonthByWeekAdapter extends SimpleWeeksAdapter {
    protected static int DEFAULT_QUERY_DAYS = 56;
    private static float mMovedPixelToCancel;
    private static int mOnDownDelay;
    private static int mTotalClickDelay;
    private boolean mAnimateSelectedDay;
    private long mAnimateTime;
    long mClickTime;
    MonthWeekEventsView mClickedView;
    float mClickedXLocation;
    protected CalendarController mController;
    private final Runnable mDoClick;
    private final Runnable mDoSingleTapUp;
    protected ArrayList<ArrayList<Event>> mEventDayList;
    private Handler mEventDialogHandler;
    protected ArrayList<Event> mEvents;
    protected int mFirstJulianDay;
    protected String mHomeTimeZone;
    protected boolean mIsMiniMonth;
    MonthWeekEventsView mLongClickedView;
    protected int mOrientation;
    protected int mQueryDays;
    protected Time mRealSelectedDay;
    protected int mRealSelectedWeek;
    private final boolean mShowAgendaWithMonth;
    MonthWeekEventsView mSingleTapUpView;
    protected Time mTempTime;
    protected Time mToday;

    public MonthByWeekAdapter(Context context, HashMap<String, Integer> map, Handler handler) {
        super(context, map);
        this.mIsMiniMonth = true;
        this.mOrientation = 2;
        this.mEventDayList = new ArrayList<>();
        this.mEvents = null;
        this.mAnimateSelectedDay = false;
        this.mAnimateTime = 0L;
        this.mDoClick = new Runnable() {
            @Override
            public void run() {
                if (MonthByWeekAdapter.this.mClickedView != null) {
                    synchronized (MonthByWeekAdapter.this.mClickedView) {
                        MonthByWeekAdapter.this.mClickedView.setClickedDay(MonthByWeekAdapter.this.mClickedXLocation);
                    }
                    MonthByWeekAdapter.this.mLongClickedView = MonthByWeekAdapter.this.mClickedView;
                    MonthByWeekAdapter.this.mClickedView = null;
                    MonthByWeekAdapter.this.mListView.invalidate();
                }
            }
        };
        this.mDoSingleTapUp = new Runnable() {
            @Override
            public void run() {
                if (MonthByWeekAdapter.this.mSingleTapUpView != null) {
                    Time dayFromLocation = MonthByWeekAdapter.this.mSingleTapUpView.getDayFromLocation(MonthByWeekAdapter.this.mClickedXLocation);
                    if (Log.isLoggable("MonthByWeekAdapter", 3)) {
                        Log.d("MonthByWeekAdapter", "Touched day at Row=" + MonthByWeekAdapter.this.mSingleTapUpView.mWeek + " day=" + dayFromLocation.toString());
                    }
                    if (dayFromLocation != null) {
                        MonthByWeekAdapter.this.onDayTapped(dayFromLocation);
                    }
                    MonthByWeekAdapter.this.clearClickedView(MonthByWeekAdapter.this.mSingleTapUpView);
                    MonthByWeekAdapter.this.mSingleTapUpView = null;
                }
            }
        };
        this.mEventDialogHandler = handler;
        if (map.containsKey("mini_month")) {
            this.mIsMiniMonth = map.get("mini_month").intValue() != 0;
        }
        this.mShowAgendaWithMonth = Utils.getConfigBool(context, R.bool.show_agenda_with_month);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mOnDownDelay = ViewConfiguration.getTapTimeout();
        mMovedPixelToCancel = viewConfiguration.getScaledTouchSlop();
        mTotalClickDelay = mOnDownDelay + 100;
    }

    public void animateSelectedDay() {
        this.mAnimateSelectedDay = true;
        this.mAnimateTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        this.mGestureDetector = new GestureDetector(this.mContext, new CalendarGestureListener());
        this.mController = CalendarController.getInstance(this.mContext);
        this.mHomeTimeZone = Utils.getTimeZone(this.mContext, null);
        this.mSelectedDay.switchTimezone(this.mHomeTimeZone);
        this.mRealSelectedDay = new Time();
        this.mRealSelectedDay.set(this.mSelectedDay);
        this.mRealSelectedWeek = this.mSelectedWeek;
        this.mToday = new Time(this.mHomeTimeZone);
        this.mToday.setToNow();
        this.mTempTime = new Time(this.mHomeTimeZone);
    }

    private void updateTimeZones() {
        this.mSelectedDay.timezone = this.mHomeTimeZone;
        this.mSelectedDay.normalize(true);
        this.mToday.timezone = this.mHomeTimeZone;
        this.mToday.setToNow();
        this.mTempTime.switchTimezone(this.mHomeTimeZone);
    }

    @Override
    public void setSelectedDay(Time time) {
        this.mSelectedDay.set(time);
        this.mSelectedWeek = Utils.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(this.mSelectedDay.normalize(true), this.mSelectedDay.gmtoff), this.mFirstDayOfWeek);
        notifyDataSetChanged();
    }

    public void setEvents(int i, int i2, ArrayList<Event> arrayList) {
        if (this.mIsMiniMonth) {
            if (Log.isLoggable("MonthByWeekAdapter", 6)) {
                Log.e("MonthByWeekAdapter", "Attempted to set events for mini view. Events only supported in full view.");
                return;
            }
            return;
        }
        this.mEvents = arrayList;
        this.mFirstJulianDay = i;
        this.mQueryDays = i2;
        ArrayList<ArrayList<Event>> arrayList2 = new ArrayList<>();
        for (int i3 = 0; i3 < i2; i3++) {
            arrayList2.add(new ArrayList<>());
        }
        if (arrayList == null || arrayList.size() == 0) {
            if (Log.isLoggable("MonthByWeekAdapter", 3)) {
                Log.d("MonthByWeekAdapter", "No events. Returning early--go schedule something fun.");
            }
            this.mEventDayList = arrayList2;
            refresh();
            return;
        }
        for (Event event : arrayList) {
            int i4 = event.startDay - this.mFirstJulianDay;
            int i5 = (event.endDay - this.mFirstJulianDay) + 1;
            if (i4 < i2 || i5 >= 0) {
                if (i4 < 0) {
                    i4 = 0;
                }
                if (i4 <= i2 && i5 >= 0) {
                    if (i5 > i2) {
                        i5 = i2;
                    }
                    while (i4 < i5) {
                        arrayList2.get(i4).add(event);
                        i4++;
                    }
                }
            }
        }
        if (Log.isLoggable("MonthByWeekAdapter", 3)) {
            Log.d("MonthByWeekAdapter", "Processed " + arrayList.size() + " events.");
        }
        this.mEventDayList = arrayList2;
        refresh();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        MonthWeekEventsView monthWeekEventsView;
        boolean z;
        boolean z2;
        boolean z3;
        if (this.mIsMiniMonth) {
            return super.getView(i, view, viewGroup);
        }
        AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(-1, -1);
        HashMap<String, Integer> map = null;
        if (view != null) {
            monthWeekEventsView = (MonthWeekEventsView) view;
            if (!this.mAnimateSelectedDay || !isHasSelectedDay(this.mRealSelectedDay, i)) {
                map = (HashMap) monthWeekEventsView.getTag();
            } else {
                if (System.currentTimeMillis() - this.mAnimateTime > 1000) {
                    this.mAnimateSelectedDay = false;
                    this.mAnimateTime = 0L;
                    z3 = false;
                } else {
                    monthWeekEventsView = new MonthWeekEventsView(this.mContext);
                    z3 = true;
                }
                z2 = z3;
                z = z3;
                if (map == null) {
                    map = new HashMap<>();
                }
                map.clear();
                monthWeekEventsView.setLayoutParams(layoutParams);
                monthWeekEventsView.setClickable(true);
                monthWeekEventsView.setOnTouchListener(this);
                setRealSelectedWeek();
                int i2 = this.mRealSelectedWeek == i ? this.mRealSelectedDay.weekDay : -1;
                if (this.mSelectedWeek == i) {
                    int i3 = this.mSelectedDay.weekDay;
                }
                map.put("height", Integer.valueOf((viewGroup.getHeight() + viewGroup.getTop()) / this.mNumWeeks));
                map.put("selected_day", Integer.valueOf(i2));
                map.put("show_wk_num", Integer.valueOf(this.mShowWeekNumber ? 1 : 0));
                map.put("week_start", Integer.valueOf(this.mFirstDayOfWeek));
                map.put("num_days", Integer.valueOf(this.mDaysPerWeek));
                map.put("week", Integer.valueOf(i));
                map.put("focus_month", Integer.valueOf(this.mFocusMonth));
                map.put("orientation", Integer.valueOf(this.mOrientation));
                if (z) {
                    map.put("animate_today", 1);
                }
                if (z2) {
                    map.put("animate_selected_day", 1);
                    this.mAnimateSelectedDay = false;
                }
                monthWeekEventsView.updateToday(this.mSelectedDay.timezone);
                monthWeekEventsView.setWeekParams(map, this.mSelectedDay.timezone);
                sendEventsToView(monthWeekEventsView);
                return monthWeekEventsView;
            }
        } else {
            monthWeekEventsView = new MonthWeekEventsView(this.mContext);
        }
        z = false;
        z2 = false;
        if (map == null) {
        }
        map.clear();
        monthWeekEventsView.setLayoutParams(layoutParams);
        monthWeekEventsView.setClickable(true);
        monthWeekEventsView.setOnTouchListener(this);
        setRealSelectedWeek();
        if (this.mRealSelectedWeek == i) {
        }
        if (this.mSelectedWeek == i) {
        }
        map.put("height", Integer.valueOf((viewGroup.getHeight() + viewGroup.getTop()) / this.mNumWeeks));
        map.put("selected_day", Integer.valueOf(i2));
        map.put("show_wk_num", Integer.valueOf(this.mShowWeekNumber ? 1 : 0));
        map.put("week_start", Integer.valueOf(this.mFirstDayOfWeek));
        map.put("num_days", Integer.valueOf(this.mDaysPerWeek));
        map.put("week", Integer.valueOf(i));
        map.put("focus_month", Integer.valueOf(this.mFocusMonth));
        map.put("orientation", Integer.valueOf(this.mOrientation));
        if (z) {
        }
        if (z2) {
        }
        monthWeekEventsView.updateToday(this.mSelectedDay.timezone);
        monthWeekEventsView.setWeekParams(map, this.mSelectedDay.timezone);
        sendEventsToView(monthWeekEventsView);
        return monthWeekEventsView;
    }

    private void sendEventsToView(MonthWeekEventsView monthWeekEventsView) {
        if (this.mEventDayList.size() == 0) {
            if (Log.isLoggable("MonthByWeekAdapter", 3)) {
                Log.d("MonthByWeekAdapter", "No events loaded, did not pass any events to view.");
            }
            monthWeekEventsView.setEvents(null, null);
            return;
        }
        int firstJulianDay = monthWeekEventsView.getFirstJulianDay();
        int i = firstJulianDay - this.mFirstJulianDay;
        int i2 = monthWeekEventsView.mNumDays + i;
        if (i < 0 || i2 > this.mEventDayList.size()) {
            if (Log.isLoggable("MonthByWeekAdapter", 3)) {
                Log.d("MonthByWeekAdapter", "Week is outside range of loaded events. viewStart: " + firstJulianDay + " eventsStart: " + this.mFirstJulianDay);
            }
            monthWeekEventsView.setEvents(null, null);
            return;
        }
        monthWeekEventsView.setEvents(this.mEventDayList.subList(i, i2), this.mEvents);
    }

    @Override
    protected void refresh() {
        this.mFirstDayOfWeek = Utils.getFirstDayOfWeek(this.mContext);
        this.mShowWeekNumber = Utils.getShowWeekNumber(this.mContext);
        this.mHomeTimeZone = Utils.getTimeZone(this.mContext, null);
        this.mOrientation = this.mContext.getResources().getConfiguration().orientation;
        updateTimeZones();
        notifyDataSetChanged();
    }

    @Override
    protected void onDayTapped(Time time) {
        setDayParameters(time);
        if (this.mShowAgendaWithMonth || this.mIsMiniMonth) {
            this.mController.sendEvent(this.mContext, 32L, time, time, -1L, 0, 1L, null, null);
        } else if (!this.mIsMiniMonth) {
            this.mController.sendEvent(this.mContext, 32L, time, time, -1L, this.mIsMiniMonth ? 0 : -1, 5L, null, null);
        } else {
            this.mController.sendEvent(this.mContext, 32L, time, time, -1L, this.mIsMiniMonth ? 0 : -1, 1L, null, null);
        }
    }

    private void setDayParameters(Time time) {
        time.timezone = this.mHomeTimeZone;
        Time time2 = new Time(this.mHomeTimeZone);
        time2.set(this.mController.getTime());
        time.hour = time2.hour;
        time.minute = time2.minute;
        time.allDay = false;
        time.normalize(true);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (!(view instanceof MonthWeekEventsView)) {
            return super.onTouch(view, motionEvent);
        }
        int action = motionEvent.getAction();
        if (this.mGestureDetector.onTouchEvent(motionEvent)) {
            this.mSingleTapUpView = view;
            long jCurrentTimeMillis = System.currentTimeMillis() - this.mClickTime;
            this.mListView.postDelayed(this.mDoSingleTapUp, jCurrentTimeMillis > ((long) mTotalClickDelay) ? 0L : ((long) mTotalClickDelay) - jCurrentTimeMillis);
            return true;
        }
        if (action != 8) {
            switch (action) {
                case 0:
                    this.mClickedView = view;
                    this.mClickedXLocation = motionEvent.getX();
                    this.mClickTime = System.currentTimeMillis();
                    this.mListView.postDelayed(this.mDoClick, mOnDownDelay);
                    return false;
                case 1:
                case 3:
                    break;
                case 2:
                    if (Math.abs(motionEvent.getX() - this.mClickedXLocation) > mMovedPixelToCancel) {
                        clearClickedView(view);
                        return false;
                    }
                    return false;
                default:
                    return false;
            }
        }
        clearClickedView(view);
        return false;
    }

    protected class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
        protected CalendarGestureListener() {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
            if (MonthByWeekAdapter.this.mLongClickedView != null) {
                Time dayFromLocation = MonthByWeekAdapter.this.mLongClickedView.getDayFromLocation(MonthByWeekAdapter.this.mClickedXLocation);
                if (dayFromLocation != null) {
                    MonthByWeekAdapter.this.mLongClickedView.performHapticFeedback(0);
                    Message message = new Message();
                    message.obj = dayFromLocation;
                    MonthByWeekAdapter.this.mEventDialogHandler.sendMessage(message);
                }
                MonthByWeekAdapter.this.mLongClickedView.clearClickedDay();
                MonthByWeekAdapter.this.mLongClickedView = null;
            }
        }
    }

    private void clearClickedView(MonthWeekEventsView monthWeekEventsView) {
        this.mListView.removeCallbacks(this.mDoClick);
        synchronized (monthWeekEventsView) {
            monthWeekEventsView.clearClickedDay();
        }
        this.mClickedView = null;
    }

    public void setRealSelectedDay(Time time) {
        if (this.mRealSelectedDay == null) {
            this.mRealSelectedDay = new Time();
        }
        this.mRealSelectedDay.set(time);
    }

    public void setRealSelectedWeek() {
        this.mRealSelectedWeek = Utils.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(this.mRealSelectedDay.normalize(true), this.mRealSelectedDay.gmtoff), this.mFirstDayOfWeek);
    }

    private boolean isHasSelectedDay(Time time, int i) {
        return i == Time.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(time.toMillis(false), time.gmtoff), Utils.getFirstDayOfWeek(this.mContext));
    }
}
