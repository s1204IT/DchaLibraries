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
    private long mAnimateTime;
    private boolean mAnimateToday;
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
    private final boolean mShowAgendaWithMonth;
    MonthWeekEventsView mSingleTapUpView;
    protected Time mTempTime;
    protected Time mToday;

    public MonthByWeekAdapter(Context context, HashMap<String, Integer> params, Handler handler) {
        super(context, params);
        this.mIsMiniMonth = true;
        this.mOrientation = 2;
        this.mEventDayList = new ArrayList<>();
        this.mEvents = null;
        this.mAnimateToday = false;
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
                    Time day = MonthByWeekAdapter.this.mSingleTapUpView.getDayFromLocation(MonthByWeekAdapter.this.mClickedXLocation);
                    if (Log.isLoggable("MonthByWeekAdapter", 3)) {
                        Log.d("MonthByWeekAdapter", "Touched day at Row=" + MonthByWeekAdapter.this.mSingleTapUpView.mWeek + " day=" + day.toString());
                    }
                    if (day != null) {
                        MonthByWeekAdapter.this.onDayTapped(day);
                    }
                    MonthByWeekAdapter.this.clearClickedView(MonthByWeekAdapter.this.mSingleTapUpView);
                    MonthByWeekAdapter.this.mSingleTapUpView = null;
                }
            }
        };
        this.mEventDialogHandler = handler;
        if (params.containsKey("mini_month")) {
            this.mIsMiniMonth = params.get("mini_month").intValue() != 0;
        }
        this.mShowAgendaWithMonth = Utils.getConfigBool(context, R.bool.show_agenda_with_month);
        ViewConfiguration vc = ViewConfiguration.get(context);
        mOnDownDelay = ViewConfiguration.getTapTimeout();
        mMovedPixelToCancel = vc.getScaledTouchSlop();
        mTotalClickDelay = mOnDownDelay + 100;
    }

    public void animateToday() {
        this.mAnimateToday = true;
        this.mAnimateTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        this.mGestureDetector = new GestureDetector(this.mContext, new CalendarGestureListener());
        this.mController = CalendarController.getInstance(this.mContext);
        this.mHomeTimeZone = Utils.getTimeZone(this.mContext, null);
        this.mSelectedDay.switchTimezone(this.mHomeTimeZone);
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
    public void setSelectedDay(Time selectedTime) {
        this.mSelectedDay.set(selectedTime);
        long millis = this.mSelectedDay.normalize(true);
        this.mSelectedWeek = Utils.getWeeksSinceEpochFromJulianDay(Time.getJulianDay(millis, this.mSelectedDay.gmtoff), this.mFirstDayOfWeek);
        notifyDataSetChanged();
    }

    public void setEvents(int firstJulianDay, int numDays, ArrayList<Event> events) {
        if (this.mIsMiniMonth) {
            if (Log.isLoggable("MonthByWeekAdapter", 6)) {
                Log.e("MonthByWeekAdapter", "Attempted to set events for mini view. Events only supported in full view.");
                return;
            }
            return;
        }
        this.mEvents = events;
        this.mFirstJulianDay = firstJulianDay;
        this.mQueryDays = numDays;
        ArrayList<ArrayList<Event>> eventDayList = new ArrayList<>();
        for (int i = 0; i < numDays; i++) {
            eventDayList.add(new ArrayList<>());
        }
        if (events == null || events.size() == 0) {
            if (Log.isLoggable("MonthByWeekAdapter", 3)) {
                Log.d("MonthByWeekAdapter", "No events. Returning early--go schedule something fun.");
            }
            this.mEventDayList = eventDayList;
            refresh();
            return;
        }
        for (Event event : events) {
            int startDay = event.startDay - this.mFirstJulianDay;
            int endDay = (event.endDay - this.mFirstJulianDay) + 1;
            if (startDay < numDays || endDay >= 0) {
                if (startDay < 0) {
                    startDay = 0;
                }
                if (startDay <= numDays && endDay >= 0) {
                    if (endDay > numDays) {
                        endDay = numDays;
                    }
                    for (int j = startDay; j < endDay; j++) {
                        eventDayList.get(j).add(event);
                    }
                }
            }
        }
        if (Log.isLoggable("MonthByWeekAdapter", 3)) {
            Log.d("MonthByWeekAdapter", "Processed " + events.size() + " events.");
        }
        this.mEventDayList = eventDayList;
        refresh();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MonthWeekEventsView v;
        if (this.mIsMiniMonth) {
            return super.getView(position, convertView, parent);
        }
        AbsListView.LayoutParams params = new AbsListView.LayoutParams(-1, -1);
        HashMap<String, Integer> drawingParams = null;
        boolean isAnimatingToday = false;
        if (convertView != null) {
            v = (MonthWeekEventsView) convertView;
            if (this.mAnimateToday && v.updateToday(this.mSelectedDay.timezone)) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - this.mAnimateTime > 1000) {
                    this.mAnimateToday = false;
                    this.mAnimateTime = 0L;
                } else {
                    isAnimatingToday = true;
                    v = new MonthWeekEventsView(this.mContext);
                }
            } else {
                drawingParams = (HashMap) v.getTag();
            }
        } else {
            v = new MonthWeekEventsView(this.mContext);
        }
        if (drawingParams == null) {
            drawingParams = new HashMap<>();
        }
        drawingParams.clear();
        v.setLayoutParams(params);
        v.setClickable(true);
        v.setOnTouchListener(this);
        int selectedDay = -1;
        if (this.mSelectedWeek == position) {
            selectedDay = this.mSelectedDay.weekDay;
        }
        drawingParams.put("height", Integer.valueOf((parent.getHeight() + parent.getTop()) / this.mNumWeeks));
        drawingParams.put("selected_day", Integer.valueOf(selectedDay));
        drawingParams.put("show_wk_num", Integer.valueOf(this.mShowWeekNumber ? 1 : 0));
        drawingParams.put("week_start", Integer.valueOf(this.mFirstDayOfWeek));
        drawingParams.put("num_days", Integer.valueOf(this.mDaysPerWeek));
        drawingParams.put("week", Integer.valueOf(position));
        drawingParams.put("focus_month", Integer.valueOf(this.mFocusMonth));
        drawingParams.put("orientation", Integer.valueOf(this.mOrientation));
        if (isAnimatingToday) {
            drawingParams.put("animate_today", 1);
            this.mAnimateToday = false;
        }
        v.setWeekParams(drawingParams, this.mSelectedDay.timezone);
        sendEventsToView(v);
        return v;
    }

    private void sendEventsToView(MonthWeekEventsView v) {
        if (this.mEventDayList.size() == 0) {
            if (Log.isLoggable("MonthByWeekAdapter", 3)) {
                Log.d("MonthByWeekAdapter", "No events loaded, did not pass any events to view.");
            }
            v.setEvents(null, null);
            return;
        }
        int viewJulianDay = v.getFirstJulianDay();
        int start = viewJulianDay - this.mFirstJulianDay;
        int end = start + v.mNumDays;
        if (start < 0 || end > this.mEventDayList.size()) {
            if (Log.isLoggable("MonthByWeekAdapter", 3)) {
                Log.d("MonthByWeekAdapter", "Week is outside range of loaded events. viewStart: " + viewJulianDay + " eventsStart: " + this.mFirstJulianDay);
            }
            v.setEvents(null, null);
            return;
        }
        v.setEvents(this.mEventDayList.subList(start, end), this.mEvents);
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
    protected void onDayTapped(Time day) {
        setDayParameters(day);
        if (this.mShowAgendaWithMonth || this.mIsMiniMonth) {
            this.mController.sendEvent(this.mContext, 32L, day, day, -1L, 0, 1L, null, null);
        } else {
            this.mController.sendEvent(this.mContext, 32L, day, day, -1L, -1, 5L, null, null);
        }
    }

    private void setDayParameters(Time day) {
        day.timezone = this.mHomeTimeZone;
        Time currTime = new Time(this.mHomeTimeZone);
        currTime.set(this.mController.getTime());
        day.hour = currTime.hour;
        day.minute = currTime.minute;
        day.allDay = false;
        day.normalize(true);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!(v instanceof MonthWeekEventsView)) {
            return super.onTouch(v, event);
        }
        int action = event.getAction();
        if (this.mGestureDetector.onTouchEvent(event)) {
            this.mSingleTapUpView = (MonthWeekEventsView) v;
            long delay = System.currentTimeMillis() - this.mClickTime;
            this.mListView.postDelayed(this.mDoSingleTapUp, delay > ((long) mTotalClickDelay) ? 0L : ((long) mTotalClickDelay) - delay);
            return true;
        }
        switch (action) {
            case 0:
                this.mClickedView = (MonthWeekEventsView) v;
                this.mClickedXLocation = event.getX();
                this.mClickTime = System.currentTimeMillis();
                this.mListView.postDelayed(this.mDoClick, mOnDownDelay);
                return false;
            case 1:
            case 3:
            case 8:
                clearClickedView((MonthWeekEventsView) v);
                return false;
            case 2:
                if (Math.abs(event.getX() - this.mClickedXLocation) > mMovedPixelToCancel) {
                    clearClickedView((MonthWeekEventsView) v);
                }
                return false;
            case 4:
            case 5:
            case 6:
            case 7:
            default:
                return false;
        }
    }

    protected class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
        protected CalendarGestureListener() {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (MonthByWeekAdapter.this.mLongClickedView != null) {
                Time day = MonthByWeekAdapter.this.mLongClickedView.getDayFromLocation(MonthByWeekAdapter.this.mClickedXLocation);
                if (day != null) {
                    MonthByWeekAdapter.this.mLongClickedView.performHapticFeedback(0);
                    Message message = new Message();
                    message.obj = day;
                    MonthByWeekAdapter.this.mEventDialogHandler.sendMessage(message);
                }
                MonthByWeekAdapter.this.mLongClickedView.clearClickedDay();
                MonthByWeekAdapter.this.mLongClickedView = null;
            }
        }
    }

    private void clearClickedView(MonthWeekEventsView v) {
        this.mListView.removeCallbacks(this.mDoClick);
        synchronized (v) {
            v.clearClickedDay();
        }
        this.mClickedView = null;
    }
}
