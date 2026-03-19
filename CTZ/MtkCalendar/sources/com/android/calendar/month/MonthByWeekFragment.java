package com.android.calendar.month;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Resources;
import android.database.ContentObserver;
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
    private final ContentObserver mCalendarsObserver;
    private ContentResolver mContentResolver;
    private final Time mDesiredDay;
    private CreateEventDialogFragment mEventDialog;
    private Handler mEventDialogHandler;
    private Uri mEventUri;
    private int mEventsLoadingDelay;
    protected int mFirstLoadedJulianDay;
    protected boolean mHideDeclined;
    private boolean mIsAccountChanged;
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

    private Uri updateUri() {
        SimpleWeekView simpleWeekView = (SimpleWeekView) this.mListView.getChildAt(0);
        if (simpleWeekView != null) {
            this.mFirstLoadedJulianDay = simpleWeekView.getFirstJulianDay();
        }
        this.mTempTime.setJulianDay(this.mFirstLoadedJulianDay - 1);
        long millis = this.mTempTime.toMillis(true);
        this.mLastLoadedJulianDay = this.mFirstLoadedJulianDay + ((this.mNumWeeks + 2) * 7);
        this.mTempTime.setJulianDay(this.mLastLoadedJulianDay + 1);
        long millis2 = this.mTempTime.toMillis(true);
        Uri.Builder builderBuildUpon = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builderBuildUpon, millis);
        ContentUris.appendId(builderBuildUpon, millis2);
        return builderBuildUpon.build();
    }

    private void updateLoadedDays() {
        List<String> pathSegments = this.mEventUri.getPathSegments();
        int size = pathSegments.size();
        if (size <= 2) {
            return;
        }
        long j = Long.parseLong(pathSegments.get(size - 2));
        long j2 = Long.parseLong(pathSegments.get(size - 1));
        this.mTempTime.set(j);
        this.mFirstLoadedJulianDay = Time.getJulianDay(j, this.mTempTime.gmtoff);
        this.mTempTime.set(j2);
        this.mLastLoadedJulianDay = Time.getJulianDay(j2, this.mTempTime.gmtoff);
    }

    protected String updateWhere() {
        if (!this.mHideDeclined) {
            return "visible=1";
        }
        return "visible=1 AND selfAttendeeStatus!=2";
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
        this.mMinimumTwoMonthFlingVelocity = ViewConfiguration.get(activity).getScaledMaximumFlingVelocity() / 2;
        Resources resources = activity.getResources();
        this.mShowCalendarControls = Utils.getConfigBool(activity, R.bool.show_calendar_controls);
        if (this.mShowCalendarControls) {
            this.mEventsLoadingDelay = resources.getInteger(R.integer.calendar_controls_animation_time);
        }
        mShowDetailsInMonth = resources.getBoolean(R.bool.show_details_in_month);
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
        HashMap<String, Integer> map = new HashMap<>();
        map.put("num_weeks", Integer.valueOf(this.mNumWeeks));
        map.put("week_numbers", Integer.valueOf(this.mShowWeekNumber ? 1 : 0));
        map.put("week_start", Integer.valueOf(this.mFirstDayOfWeek));
        map.put("mini_month", Integer.valueOf(this.mIsMiniMonth ? 1 : 0));
        map.put("selected_day", Integer.valueOf(Time.getJulianDay(this.mSelectedDay.toMillis(true), this.mSelectedDay.gmtoff)));
        map.put("days_per_week", Integer.valueOf(this.mDaysPerWeek));
        if (this.mAdapter == null) {
            this.mAdapter = new MonthByWeekAdapter(getActivity(), map, this.mEventDialogHandler);
            this.mAdapter.registerDataSetObserver(this.mObserver);
        } else {
            this.mAdapter.updateParams(map);
        }
        this.mAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate;
        if (this.mIsMiniMonth) {
            viewInflate = layoutInflater.inflate(R.layout.month_by_week, viewGroup, false);
        } else {
            viewInflate = layoutInflater.inflate(R.layout.full_month_by_week, viewGroup, false);
        }
        this.mDayNamesHeader = (ViewGroup) viewInflate.findViewById(R.id.day_names);
        return viewInflate;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
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

    public MonthByWeekFragment(long j, boolean z) {
        super(j);
        this.mDesiredDay = new Time();
        this.mShouldLoad = true;
        this.mEventDialogHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                FragmentManager fragmentManager = MonthByWeekFragment.this.getFragmentManager();
                if (fragmentManager != null) {
                    Time time = (Time) message.obj;
                    MonthByWeekFragment.this.mEventDialog = new CreateEventDialogFragment(time);
                    MonthByWeekFragment.this.mEventDialog.show(fragmentManager, "event_dialog");
                }
            }
        };
        this.mTZUpdater = new Runnable() {
            @Override
            public void run() {
                String timeZone = Utils.getTimeZone(MonthByWeekFragment.this.mContext, MonthByWeekFragment.this.mTZUpdater);
                MonthByWeekFragment.this.mSelectedDay.timezone = timeZone;
                MonthByWeekFragment.this.mSelectedDay.normalize(true);
                MonthByWeekFragment.this.mTempTime.timezone = timeZone;
                MonthByWeekFragment.this.mFirstDayOfMonth.timezone = timeZone;
                MonthByWeekFragment.this.mFirstDayOfMonth.normalize(true);
                MonthByWeekFragment.this.mFirstVisibleDay.timezone = timeZone;
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
        this.mIsAccountChanged = false;
        this.mCalendarsObserver = new ContentObserver(new Handler()) {
            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }

            @Override
            public void onChange(boolean z2) {
                Log.v("MonthFragment", "mCalendarsObserver, onChange");
                MonthByWeekFragment.this.mIsAccountChanged = true;
            }
        };
        this.mIsMiniMonth = z;
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
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader cursorLoader;
        if (this.mIsMiniMonth) {
            return null;
        }
        synchronized (this.mUpdateLoader) {
            this.mFirstLoadedJulianDay = Time.getJulianDay(this.mSelectedDay.toMillis(true), this.mSelectedDay.gmtoff) - ((this.mNumWeeks * 7) / 2);
            this.mEventUri = updateUri();
            cursorLoader = new CursorLoader(getActivity(), this.mEventUri, Event.EVENT_PROJECTION, updateWhere(), null, "startDay,startMinute,title");
            cursorLoader.setUpdateThrottle(500L);
        }
        if (Log.isLoggable("MonthFragment", 3)) {
            Log.d("MonthFragment", "Returning new loader with uri: " + this.mEventUri);
        }
        return cursorLoader;
    }

    @Override
    public void doResumeUpdates() {
        this.mFirstDayOfWeek = Utils.getFirstDayOfWeek(this.mContext);
        this.mShowWeekNumber = Utils.getShowWeekNumber(this.mContext);
        boolean z = this.mHideDeclined;
        this.mHideDeclined = Utils.getHideDeclinedEvents(this.mContext);
        if (z != this.mHideDeclined && this.mLoader != null) {
            this.mLoader.setSelection(updateWhere());
        }
        this.mDaysPerWeek = Utils.getDaysPerWeek(this.mContext);
        updateHeader();
        this.mAdapter.setSelectedDay(this.mSelectedDay);
        this.mTZUpdater.run();
        this.mTodayUpdater.run();
        if (this.mIsAccountChanged) {
            this.mUpdateLoader.run();
            this.mIsAccountChanged = false;
        }
        goTo(this.mSelectedDay.toMillis(true), false, true, false);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        synchronized (this.mUpdateLoader) {
            if (Log.isLoggable("MonthFragment", 3)) {
                Log.d("MonthFragment", "Found " + cursor.getCount() + " cursor entries for uri " + this.mEventUri);
            }
            CursorLoader cursorLoader = (CursorLoader) loader;
            if (this.mEventUri == null) {
                this.mEventUri = cursorLoader.getUri();
                updateLoadedDays();
            }
            if (cursorLoader.getUri().compareTo(this.mEventUri) != 0) {
                return;
            }
            ArrayList<Event> arrayList = new ArrayList<>();
            Event.buildEventsFromCursor(arrayList, cursor, this.mContext, this.mFirstLoadedJulianDay, this.mLastLoadedJulianDay);
            ((MonthByWeekAdapter) this.mAdapter).setEvents(this.mFirstLoadedJulianDay, (this.mLastLoadedJulianDay - this.mFirstLoadedJulianDay) + 1, arrayList);
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
    public void handleEvent(CalendarController.EventInfo eventInfo) {
        boolean z;
        if (eventInfo.eventType != 32) {
            if (eventInfo.eventType == 128) {
                eventsChanged();
            }
        } else {
            if (this.mAdapter == null) {
                return;
            }
            if (this.mDaysPerWeek * this.mNumWeeks * 2 >= Math.abs((Time.getJulianDay(eventInfo.selectedTime.toMillis(true), eventInfo.selectedTime.gmtoff) - Time.getJulianDay(this.mFirstVisibleDay.toMillis(true), this.mFirstVisibleDay.gmtoff)) - ((this.mDaysPerWeek * this.mNumWeeks) / 2))) {
                z = true;
            } else {
                z = false;
            }
            this.mDesiredDay.set(eventInfo.selectedTime);
            this.mDesiredDay.normalize(true);
            boolean z2 = (eventInfo.extraLong & 8) != 0;
            int i = ((eventInfo.extraLong & 8) > 0L ? 1 : ((eventInfo.extraLong & 8) == 0L ? 0 : -1));
            boolean zGoTo = goTo(eventInfo.selectedTime.toMillis(true), z, true, false);
            if (z2) {
                ((MonthByWeekAdapter) this.mAdapter).setRealSelectedDay(eventInfo.selectedTime);
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((MonthByWeekAdapter) MonthByWeekFragment.this.mAdapter).animateSelectedDay();
                        MonthByWeekFragment.this.mAdapter.notifyDataSetChanged();
                    }
                }, zGoTo ? 500L : 0L);
            }
        }
    }

    @Override
    protected void setMonthDisplayed(Time time, boolean z) {
        boolean z2;
        super.setMonthDisplayed(time, z);
        if (time.year == this.mDesiredDay.year && time.month == this.mDesiredDay.month) {
            this.mSelectedDay.set(this.mDesiredDay);
            this.mAdapter.setSelectedDay(this.mDesiredDay);
            z2 = true;
        } else {
            this.mSelectedDay.set(time);
            this.mAdapter.setSelectedDay(time);
            z2 = false;
        }
        CalendarController calendarController = CalendarController.getInstance(this.mContext);
        if (this.mSelectedDay.minute >= 30) {
            this.mSelectedDay.minute = 30;
        } else {
            this.mSelectedDay.minute = 0;
        }
        long jNormalize = this.mSelectedDay.normalize(true);
        if (jNormalize != calendarController.getTime() && this.mUserScrolled) {
            calendarController.setTime(jNormalize + (z2 ? 0L : (604800000 * ((long) this.mNumWeeks)) / 3));
        }
        if (!this.mIsMiniMonth) {
            calendarController.sendEvent(this, 1024L, time, time, this.mSelectedDay, -1L, 0, 52L, null, null);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        synchronized (this.mUpdateLoader) {
            try {
                if (i != 0) {
                    this.mShouldLoad = false;
                    stopLoader();
                    this.mDesiredDay.setToNow();
                } else {
                    this.mHandler.removeCallbacks(this.mUpdateLoader);
                    this.mShouldLoad = true;
                    this.mHandler.postDelayed(this.mUpdateLoader, 200L);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        this.mUserScrolled = false;
        if (i == 1) {
            this.mUserScrolled = true;
        }
        this.mScrollStateChangedRunnable.doScrollStateChange(absListView, i);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        this.mDesiredDay.setToNow();
        return false;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mContentResolver = this.mContext.getContentResolver();
        this.mContentResolver.registerContentObserver(CalendarContract.Calendars.CONTENT_URI, true, this.mCalendarsObserver);
    }

    @Override
    public void onDestroy() {
        this.mContentResolver.unregisterContentObserver(this.mCalendarsObserver);
        super.onDestroy();
    }
}
