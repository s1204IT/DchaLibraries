package com.android.calendar.agenda;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.HeaderViewListAdapter;
import com.android.calendar.CalendarController;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.EventInfoFragment;
import com.android.calendar.GeneralPreferences;
import com.android.calendar.R;
import com.android.calendar.StickyHeaderListView;
import com.android.calendar.Utils;
import com.android.calendar.agenda.AgendaAdapter;
import com.android.calendar.agenda.AgendaWindowAdapter;
import java.util.ArrayList;
import java.util.Date;

public class AgendaFragment extends Fragment implements AbsListView.OnScrollListener, CalendarController.EventHandler {
    private Activity mActivity;
    private AgendaWindowAdapter mAdapter;
    private AgendaListView mAgendaListView;
    private CalendarController mController;
    private EventInfoFragment mEventFragment;
    private boolean mForceReplace;
    private final long mInitialTimeMillis;
    private boolean mIsTabletConfig;
    int mJulianDayOnTop;
    private long mLastHandledEventId;
    private Time mLastHandledEventTime;
    private long mLastShownEventId;
    private boolean mOnAttachAllDay;
    private CalendarController.EventInfo mOnAttachedInfo;
    private String mQuery;
    private boolean mShowEventDetailsWithAgenda;
    private final Runnable mTZUpdater;
    private final Time mTime;
    private String mTimeZone;
    private boolean mUsedForSearch;
    private static final String TAG = AgendaFragment.class.getSimpleName();
    private static boolean DEBUG = false;

    public AgendaFragment() {
        this(0L, false);
    }

    public AgendaFragment(long timeMillis, boolean usedForSearch) {
        this.mUsedForSearch = false;
        this.mOnAttachedInfo = null;
        this.mOnAttachAllDay = false;
        this.mAdapter = null;
        this.mForceReplace = true;
        this.mLastShownEventId = -1L;
        this.mJulianDayOnTop = -1;
        this.mTZUpdater = new Runnable() {
            @Override
            public void run() {
                AgendaFragment.this.mTimeZone = Utils.getTimeZone(AgendaFragment.this.getActivity(), this);
                AgendaFragment.this.mTime.switchTimezone(AgendaFragment.this.mTimeZone);
            }
        };
        this.mLastHandledEventId = -1L;
        this.mLastHandledEventTime = null;
        this.mInitialTimeMillis = timeMillis;
        this.mTime = new Time();
        this.mLastHandledEventTime = new Time();
        if (this.mInitialTimeMillis == 0) {
            this.mTime.setToNow();
        } else {
            this.mTime.set(this.mInitialTimeMillis);
        }
        this.mLastHandledEventTime.set(this.mTime);
        this.mUsedForSearch = usedForSearch;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mTimeZone = Utils.getTimeZone(activity, this.mTZUpdater);
        this.mTime.switchTimezone(this.mTimeZone);
        this.mActivity = activity;
        if (this.mOnAttachedInfo != null) {
            showEventInfo(this.mOnAttachedInfo, this.mOnAttachAllDay, true);
            this.mOnAttachedInfo = null;
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mController = CalendarController.getInstance(this.mActivity);
        this.mShowEventDetailsWithAgenda = Utils.getConfigBool(this.mActivity, R.bool.show_event_details_with_agenda);
        this.mIsTabletConfig = Utils.getConfigBool(this.mActivity, R.bool.tablet_config);
        if (icicle != null) {
            long prevTime = icicle.getLong("key_restore_time", -1L);
            if (prevTime != -1) {
                this.mTime.set(prevTime);
                if (DEBUG) {
                    Log.d(TAG, "Restoring time to " + this.mTime.toString());
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View topListView;
        int screenWidth = this.mActivity.getResources().getDisplayMetrics().widthPixels;
        View v = inflater.inflate(R.layout.agenda_fragment, (ViewGroup) null);
        this.mAgendaListView = (AgendaListView) v.findViewById(R.id.agenda_events_list);
        this.mAgendaListView.setClickable(true);
        if (savedInstanceState != null) {
            long instanceId = savedInstanceState.getLong("key_restore_instance_id", -1L);
            if (instanceId != -1) {
                this.mAgendaListView.setSelectedInstanceId(instanceId);
            }
        }
        View eventView = v.findViewById(R.id.agenda_event_info);
        if (!this.mShowEventDetailsWithAgenda) {
            eventView.setVisibility(8);
        }
        StickyHeaderListView lv = (StickyHeaderListView) v.findViewById(R.id.agenda_sticky_header_list);
        if (lv != null) {
            Adapter a = this.mAgendaListView.getAdapter();
            lv.setAdapter(a);
            if (a instanceof HeaderViewListAdapter) {
                this.mAdapter = (AgendaWindowAdapter) ((HeaderViewListAdapter) a).getWrappedAdapter();
                lv.setIndexer(this.mAdapter);
                lv.setHeaderHeightListener(this.mAdapter);
            } else if (a instanceof AgendaWindowAdapter) {
                this.mAdapter = (AgendaWindowAdapter) a;
                lv.setIndexer(this.mAdapter);
                lv.setHeaderHeightListener(this.mAdapter);
            } else {
                Log.wtf(TAG, "Cannot find HeaderIndexer for StickyHeaderListView");
            }
            lv.setOnScrollListener(this);
            lv.setHeaderSeparator(getResources().getColor(R.color.agenda_list_separator_color), 1);
            topListView = lv;
        } else {
            topListView = this.mAgendaListView;
        }
        if (!this.mShowEventDetailsWithAgenda) {
            ViewGroup.LayoutParams params = topListView.getLayoutParams();
            params.width = screenWidth;
            topListView.setLayoutParams(params);
        } else {
            ViewGroup.LayoutParams listParams = topListView.getLayoutParams();
            listParams.width = (screenWidth * 4) / 10;
            topListView.setLayoutParams(listParams);
            ViewGroup.LayoutParams detailsParams = eventView.getLayoutParams();
            detailsParams.width = screenWidth - listParams.width;
            eventView.setLayoutParams(detailsParams);
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) {
            Log.v(TAG, "OnResume to " + this.mTime.toString());
        }
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(getActivity());
        boolean hideDeclined = prefs.getBoolean("preferences_hide_declined", false);
        this.mAgendaListView.setHideDeclinedEvents(hideDeclined);
        if (this.mLastHandledEventId != -1) {
            this.mAgendaListView.goTo(this.mLastHandledEventTime, this.mLastHandledEventId, this.mQuery, true, false);
            this.mLastHandledEventTime = null;
            this.mLastHandledEventId = -1L;
        } else {
            this.mAgendaListView.goTo(this.mTime, -1L, this.mQuery, true, false);
        }
        this.mAgendaListView.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        long timeToSave;
        super.onSaveInstanceState(outState);
        if (this.mAgendaListView != null) {
            if (this.mShowEventDetailsWithAgenda) {
                if (this.mLastHandledEventTime != null) {
                    timeToSave = this.mLastHandledEventTime.toMillis(true);
                    this.mTime.set(this.mLastHandledEventTime);
                } else {
                    timeToSave = System.currentTimeMillis();
                    this.mTime.set(timeToSave);
                }
                outState.putLong("key_restore_time", timeToSave);
                this.mController.setTime(timeToSave);
            } else {
                AgendaWindowAdapter.AgendaItem item = this.mAgendaListView.getFirstVisibleAgendaItem();
                if (item != null) {
                    long firstVisibleTime = this.mAgendaListView.getFirstVisibleTime(item);
                    if (firstVisibleTime > 0) {
                        this.mTime.set(firstVisibleTime);
                        this.mController.setTime(firstVisibleTime);
                        outState.putLong("key_restore_time", firstVisibleTime);
                    }
                    this.mLastShownEventId = item.id;
                }
            }
            if (DEBUG) {
                Log.v(TAG, "onSaveInstanceState " + this.mTime.toString());
            }
            long selectedInstance = this.mAgendaListView.getSelectedInstanceId();
            if (selectedInstance >= 0) {
                outState.putLong("key_restore_instance_id", selectedInstance);
            }
        }
    }

    public void removeFragments(FragmentManager fragmentManager) {
        if (!getActivity().isFinishing()) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            Fragment f = fragmentManager.findFragmentById(R.id.agenda_event_info);
            if (f != null) {
                ft.remove(f);
            }
            ft.commit();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mAgendaListView.onPause();
    }

    private void goTo(CalendarController.EventInfo event, boolean animate) {
        if (event.selectedTime != null) {
            this.mTime.set(event.selectedTime);
        } else if (event.startTime != null) {
            this.mTime.set(event.startTime);
        }
        if (this.mAgendaListView != null) {
            this.mAgendaListView.goTo(this.mTime, event.id, this.mQuery, false, (event.extraLong & 8) != 0 && this.mShowEventDetailsWithAgenda);
            AgendaAdapter.ViewHolder vh = this.mAgendaListView.getSelectedViewHolder();
            Log.d(TAG, "selected viewholder is null: " + (vh == null));
            showEventInfo(event, vh != null ? vh.allDay : false, this.mForceReplace);
            this.mForceReplace = false;
        }
    }

    private void search(String query, Time time) {
        this.mQuery = query;
        if (time != null) {
            this.mTime.set(time);
        }
        if (this.mAgendaListView != null) {
            this.mAgendaListView.goTo(time, -1L, this.mQuery, true, false);
        }
    }

    public void eventsChanged() {
        if (this.mAgendaListView != null) {
            this.mAgendaListView.refresh(true);
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return (this.mUsedForSearch ? 256L : 0L) | 160;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
        if (event.eventType == 32) {
            this.mLastHandledEventId = event.id;
            this.mLastHandledEventTime = event.selectedTime != null ? event.selectedTime : event.startTime;
            goTo(event, true);
        } else if (event.eventType == 256) {
            search(event.query, event.startTime);
        } else if (event.eventType == 128) {
            eventsChanged();
        }
    }

    public long getLastShowEventId() {
        return this.mLastShownEventId;
    }

    private void showEventInfo(CalendarController.EventInfo event, boolean allDay, boolean replaceFragment) {
        if (event.id == -1) {
            Log.e(TAG, "showEventInfo, event ID = " + event.id);
            return;
        }
        this.mLastShownEventId = event.id;
        if (this.mShowEventDetailsWithAgenda) {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager == null) {
                this.mOnAttachedInfo = event;
                this.mOnAttachAllDay = allDay;
                return;
            }
            FragmentTransaction ft = fragmentManager.beginTransaction();
            if (allDay) {
                event.startTime.timezone = "UTC";
                event.endTime.timezone = "UTC";
            }
            if (DEBUG) {
                Log.d(TAG, "***");
                Log.d(TAG, "showEventInfo: start: " + new Date(event.startTime.toMillis(true)));
                Log.d(TAG, "showEventInfo: end: " + new Date(event.endTime.toMillis(true)));
                Log.d(TAG, "showEventInfo: all day: " + allDay);
                Log.d(TAG, "***");
            }
            long startMillis = event.startTime.toMillis(true);
            long endMillis = event.endTime.toMillis(true);
            EventInfoFragment fOld = (EventInfoFragment) fragmentManager.findFragmentById(R.id.agenda_event_info);
            if (fOld == null || replaceFragment || fOld.getStartMillis() != startMillis || fOld.getEndMillis() != endMillis || fOld.getEventId() != event.id) {
                this.mEventFragment = new EventInfoFragment((Context) this.mActivity, event.id, startMillis, endMillis, 0, false, 1, (ArrayList<CalendarEventModel.ReminderEntry>) null);
                ft.replace(R.id.agenda_event_info, this.mEventFragment);
                ft.commit();
                return;
            }
            fOld.reloadEvents();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (this.mAdapter != null) {
            this.mAdapter.setScrollState(scrollState);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        int julianDay = this.mAgendaListView.getJulianDayFromPosition(firstVisibleItem - this.mAgendaListView.getHeaderViewsCount());
        if (julianDay != 0 && this.mJulianDayOnTop != julianDay) {
            this.mJulianDayOnTop = julianDay;
            Time t = new Time(this.mTimeZone);
            t.setJulianDay(this.mJulianDayOnTop);
            this.mController.setTime(t.toMillis(true));
            if (!this.mIsTabletConfig) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        Time t2 = new Time(AgendaFragment.this.mTimeZone);
                        t2.setJulianDay(AgendaFragment.this.mJulianDayOnTop);
                        AgendaFragment.this.mController.sendEvent(this, 1024L, t2, t2, null, -1L, 0, 0L, null, null);
                    }
                });
            }
        }
    }
}
