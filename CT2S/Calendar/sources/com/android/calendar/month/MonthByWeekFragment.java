package com.android.calendar.month;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import com.android.calendar.CalendarController;
import com.android.calendar.Event;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.event.CreateEventDialogFragment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MonthByWeekFragment extends SimpleDayPickerFragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnTouchListener, AbsListView.OnScrollListener, CalendarController.EventHandler {
    protected static boolean mShowDetailsInMonth = false;
    private final Time mDesiredDay;
    private CreateEventDialogFragment mEventDialog;
    private Handler mEventDialogHandler;
    private Uri mEventUri;
    private int mEventsLoadingDelay;
    protected int mFirstLoadedJulianDay;
    protected boolean mHideDeclined;
    private boolean mIsDetached;
    protected boolean mIsMiniMonth;
    protected int mLastLoadedJulianDay;
    private CursorLoader mLoader;
    Runnable mLoadingRunnable;
    protected float mMinimumTwoMonthFlingVelocity;
    private volatile boolean mShouldLoad;
    private boolean mShowCalendarControls;
    private final Runnable mTZUpdater;
    private final Runnable mUpdateLoader;
    private boolean mUserScrolled;

    private Uri updateUri() {
        SimpleWeekView child = (SimpleWeekView) this.mListView.getChildAt(0);
        if (child != null) {
            int julianDay = child.getFirstJulianDay();
            this.mFirstLoadedJulianDay = julianDay;
        }
        this.mTempTime.setJulianDay(this.mFirstLoadedJulianDay - 1);
        long start = this.mTempTime.toMillis(true);
        this.mLastLoadedJulianDay = this.mFirstLoadedJulianDay + ((this.mNumWeeks + 2) * 7);
        this.mTempTime.setJulianDay(this.mLastLoadedJulianDay + 1);
        long end = this.mTempTime.toMillis(true);
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, start);
        ContentUris.appendId(builder, end);
        return builder.build();
    }

    private void updateLoadedDays() {
        List<String> pathSegments = this.mEventUri.getPathSegments();
        int size = pathSegments.size();
        if (size > 2) {
            long first = Long.parseLong(pathSegments.get(size - 2));
            long last = Long.parseLong(pathSegments.get(size - 1));
            this.mTempTime.set(first);
            this.mFirstLoadedJulianDay = Time.getJulianDay(first, this.mTempTime.gmtoff);
            this.mTempTime.set(last);
            this.mLastLoadedJulianDay = Time.getJulianDay(last, this.mTempTime.gmtoff);
        }
    }

    protected String updateWhere() {
        if (this.mHideDeclined || !mShowDetailsInMonth) {
            String where = "visible=1 AND selfAttendeeStatus!=2";
            return where;
        }
        return "visible=1";
    }

    private void stopLoader() {
        synchronized (this.mUpdateLoader) {
            this.mHandler.removeCallbacks(this.mUpdateLoader);
            if (this.mLoader != null) {
                this.mLoader.stopLoading();
                if (Log.isLoggable("MonthFragment", 3)) {
                    Log.d("MonthFragment", "Stopped loader from loading");
                }
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mTZUpdater.run();
        if (this.mAdapter != null) {
            this.mAdapter.setSelectedDay(this.mSelectedDay);
        }
        this.mIsDetached = false;
        ViewConfiguration viewConfig = ViewConfiguration.get(activity);
        this.mMinimumTwoMonthFlingVelocity = viewConfig.getScaledMaximumFlingVelocity() / 2;
        Resources res = activity.getResources();
        this.mShowCalendarControls = Utils.getConfigBool(activity, R.bool.show_calendar_controls);
        if (this.mShowCalendarControls) {
            this.mEventsLoadingDelay = res.getInteger(R.integer.calendar_controls_animation_time);
        }
        mShowDetailsInMonth = res.getBoolean(R.bool.show_details_in_month);
    }

    @Override
    public void onDetach() {
        this.mIsDetached = true;
        super.onDetach();
        if (this.mShowCalendarControls && this.mListView != null) {
            this.mListView.removeCallbacks(this.mLoadingRunnable);
        }
    }

    @Override
    protected void setUpAdapter() {
        this.mFirstDayOfWeek = Utils.getFirstDayOfWeek(this.mContext);
        this.mShowWeekNumber = Utils.getShowWeekNumber(this.mContext);
        HashMap<String, Integer> weekParams = new HashMap<>();
        weekParams.put("num_weeks", Integer.valueOf(this.mNumWeeks));
        weekParams.put("week_numbers", Integer.valueOf(this.mShowWeekNumber ? 1 : 0));
        weekParams.put("week_start", Integer.valueOf(this.mFirstDayOfWeek));
        weekParams.put("mini_month", Integer.valueOf(this.mIsMiniMonth ? 1 : 0));
        weekParams.put("selected_day", Integer.valueOf(Time.getJulianDay(this.mSelectedDay.toMillis(true), this.mSelectedDay.gmtoff)));
        weekParams.put("days_per_week", Integer.valueOf(this.mDaysPerWeek));
        if (this.mAdapter == null) {
            this.mAdapter = new MonthByWeekAdapter(getActivity(), weekParams, this.mEventDialogHandler);
            this.mAdapter.registerDataSetObserver(this.mObserver);
        } else {
            this.mAdapter.updateParams(weekParams);
        }
        this.mAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v;
        if (this.mIsMiniMonth) {
            v = inflater.inflate(R.layout.month_by_week, container, false);
        } else {
            v = inflater.inflate(R.layout.full_month_by_week, container, false);
        }
        this.mDayNamesHeader = (ViewGroup) v.findViewById(R.id.day_names);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mListView.setSelector(new StateListDrawable());
        this.mListView.setOnTouchListener(this);
        if (!this.mIsMiniMonth) {
            this.mListView.setBackgroundColor(getResources().getColor(R.color.month_bgcolor));
        }
        if (this.mShowCalendarControls) {
            this.mListView.postDelayed(this.mLoadingRunnable, this.mEventsLoadingDelay);
        } else {
            this.mLoader = (CursorLoader) getLoaderManager().initLoader(0, null, this);
        }
        this.mAdapter.setListView(this.mListView);
    }

    public MonthByWeekFragment() {
        this(System.currentTimeMillis(), true);
    }

    public MonthByWeekFragment(long initialTime, boolean isMiniMonth) {
        super(initialTime);
        this.mDesiredDay = new Time();
        this.mShouldLoad = true;
        this.mUserScrolled = false;
        this.mEventDialogHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                FragmentManager manager = MonthByWeekFragment.this.getFragmentManager();
                if (manager != null) {
                    Time day = (Time) msg.obj;
                    MonthByWeekFragment.this.mEventDialog = new CreateEventDialogFragment(day);
                    MonthByWeekFragment.this.mEventDialog.show(manager, "event_dialog");
                }
            }
        };
        this.mTZUpdater = new Runnable() {
            @Override
            public void run() {
                String tz = Utils.getTimeZone(MonthByWeekFragment.this.mContext, MonthByWeekFragment.this.mTZUpdater);
                MonthByWeekFragment.this.mSelectedDay.timezone = tz;
                MonthByWeekFragment.this.mSelectedDay.normalize(true);
                MonthByWeekFragment.this.mTempTime.timezone = tz;
                MonthByWeekFragment.this.mFirstDayOfMonth.timezone = tz;
                MonthByWeekFragment.this.mFirstDayOfMonth.normalize(true);
                MonthByWeekFragment.this.mFirstVisibleDay.timezone = tz;
                MonthByWeekFragment.this.mFirstVisibleDay.normalize(true);
                if (MonthByWeekFragment.this.mAdapter != null) {
                    MonthByWeekFragment.this.mAdapter.refresh();
                }
            }
        };
        this.mUpdateLoader = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (MonthByWeekFragment.this.mShouldLoad && MonthByWeekFragment.this.mLoader != null) {
                        MonthByWeekFragment.this.stopLoader();
                        MonthByWeekFragment.this.mEventUri = MonthByWeekFragment.this.updateUri();
                        MonthByWeekFragment.this.mLoader.setUri(MonthByWeekFragment.this.mEventUri);
                        MonthByWeekFragment.this.mLoader.startLoading();
                        MonthByWeekFragment.this.mLoader.onContentChanged();
                        if (Log.isLoggable("MonthFragment", 3)) {
                            Log.d("MonthFragment", "Started loader with uri: " + MonthByWeekFragment.this.mEventUri);
                        }
                    }
                }
            }
        };
        this.mLoadingRunnable = new Runnable() {
            @Override
            public void run() {
                if (!MonthByWeekFragment.this.mIsDetached) {
                    MonthByWeekFragment.this.mLoader = (CursorLoader) MonthByWeekFragment.this.getLoaderManager().initLoader(0, null, MonthByWeekFragment.this);
                }
            }
        };
        this.mIsMiniMonth = isMiniMonth;
    }

    @Override
    protected void setUpHeader() {
        if (this.mIsMiniMonth) {
            super.setUpHeader();
            return;
        }
        this.mDayLabels = new String[7];
        for (int i = 1; i <= 7; i++) {
            this.mDayLabels[i - 1] = DateUtils.getDayOfWeekString(i, 20).toUpperCase();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = null;
        if (!this.mIsMiniMonth) {
            synchronized (this.mUpdateLoader) {
                this.mFirstLoadedJulianDay = Time.getJulianDay(this.mSelectedDay.toMillis(true), this.mSelectedDay.gmtoff) - ((this.mNumWeeks * 7) / 2);
                this.mEventUri = updateUri();
                String where = updateWhere();
                loader = new CursorLoader(getActivity(), this.mEventUri, Event.EVENT_PROJECTION, where, null, "startDay,startMinute,title");
                loader.setUpdateThrottle(500L);
            }
            if (Log.isLoggable("MonthFragment", 3)) {
                Log.d("MonthFragment", "Returning new loader with uri: " + this.mEventUri);
            }
        }
        return loader;
    }

    @Override
    public void doResumeUpdates() {
        this.mFirstDayOfWeek = Utils.getFirstDayOfWeek(this.mContext);
        this.mShowWeekNumber = Utils.getShowWeekNumber(this.mContext);
        boolean prevHideDeclined = this.mHideDeclined;
        this.mHideDeclined = Utils.getHideDeclinedEvents(this.mContext);
        if (prevHideDeclined != this.mHideDeclined && this.mLoader != null) {
            this.mLoader.setSelection(updateWhere());
        }
        this.mDaysPerWeek = Utils.getDaysPerWeek(this.mContext);
        updateHeader();
        this.mAdapter.setSelectedDay(this.mSelectedDay);
        this.mTZUpdater.run();
        this.mTodayUpdater.run();
        goTo(this.mSelectedDay.toMillis(true), false, true, false);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        synchronized (this.mUpdateLoader) {
            if (Log.isLoggable("MonthFragment", 3)) {
                Log.d("MonthFragment", "Found " + data.getCount() + " cursor entries for uri " + this.mEventUri);
            }
            CursorLoader cLoader = (CursorLoader) loader;
            if (this.mEventUri == null) {
                this.mEventUri = cLoader.getUri();
                updateLoadedDays();
            }
            if (cLoader.getUri().compareTo(this.mEventUri) == 0) {
                ArrayList<Event> events = new ArrayList<>();
                Event.buildEventsFromCursor(events, data, this.mContext, this.mFirstLoadedJulianDay, this.mLastLoadedJulianDay);
                ((MonthByWeekAdapter) this.mAdapter).setEvents(this.mFirstLoadedJulianDay, (this.mLastLoadedJulianDay - this.mFirstLoadedJulianDay) + 1, events);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    public void eventsChanged() {
        if (this.mLoader != null) {
            this.mLoader.forceLoad();
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return 160L;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
        if (event.eventType != 32) {
            if (event.eventType == 128) {
                eventsChanged();
                return;
            }
            return;
        }
        boolean animate = true;
        if (this.mDaysPerWeek * this.mNumWeeks * 2 < Math.abs((Time.getJulianDay(event.selectedTime.toMillis(true), event.selectedTime.gmtoff) - Time.getJulianDay(this.mFirstVisibleDay.toMillis(true), this.mFirstVisibleDay.gmtoff)) - ((this.mDaysPerWeek * this.mNumWeeks) / 2))) {
            animate = false;
        }
        this.mDesiredDay.set(event.selectedTime);
        this.mDesiredDay.normalize(true);
        boolean animateToday = (event.extraLong & 8) != 0;
        boolean delayAnimation = goTo(event.selectedTime.toMillis(true), animate, true, false);
        if (animateToday) {
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ((MonthByWeekAdapter) MonthByWeekFragment.this.mAdapter).animateToday();
                    MonthByWeekFragment.this.mAdapter.notifyDataSetChanged();
                }
            }, delayAnimation ? 500L : 0L);
        }
    }

    @Override
    protected void setMonthDisplayed(Time time, boolean updateHighlight) throws Throwable {
        super.setMonthDisplayed(time, updateHighlight);
        if (!this.mIsMiniMonth) {
            boolean useSelected = false;
            if (time.year == this.mDesiredDay.year && time.month == this.mDesiredDay.month) {
                this.mSelectedDay.set(this.mDesiredDay);
                this.mAdapter.setSelectedDay(this.mDesiredDay);
                useSelected = true;
            } else {
                this.mSelectedDay.set(time);
                this.mAdapter.setSelectedDay(time);
            }
            CalendarController controller = CalendarController.getInstance(this.mContext);
            if (this.mSelectedDay.minute >= 30) {
                this.mSelectedDay.minute = 30;
            } else {
                this.mSelectedDay.minute = 0;
            }
            long newTime = this.mSelectedDay.normalize(true);
            if (newTime != controller.getTime() && this.mUserScrolled) {
                long offset = useSelected ? 0L : (604800000 * ((long) this.mNumWeeks)) / 3;
                controller.setTime(newTime + offset);
            }
            controller.sendEvent(this, 1024L, time, time, time, -1L, 0, 52L, null, null);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        synchronized (this.mUpdateLoader) {
            if (scrollState != 0) {
                this.mShouldLoad = false;
                stopLoader();
                this.mDesiredDay.setToNow();
            } else {
                this.mHandler.removeCallbacks(this.mUpdateLoader);
                this.mShouldLoad = true;
                this.mHandler.postDelayed(this.mUpdateLoader, 200L);
            }
        }
        if (scrollState == 1) {
            this.mUserScrolled = true;
        }
        this.mScrollStateChangedRunnable.doScrollStateChange(view, scrollState);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        this.mDesiredDay.setToNow();
        return false;
    }
}
