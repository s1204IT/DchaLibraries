package com.android.calendar.agenda;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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
import com.mediatek.calendar.selectevent.AgendaChoiceActivity;
import java.util.ArrayList;
import java.util.Date;

public class AgendaFragment extends Fragment implements AbsListView.OnScrollListener, CalendarController.EventHandler {
    private Activity mActivity;
    private AgendaWindowAdapter mAdapter;
    private AgendaListView mAgendaListView;
    private CalendarController mController;
    private EventInfoFragment mEventFragment;
    private View mEventView;
    private boolean mForceReplace;
    private final long mInitialTimeMillis;
    private boolean mIsTabletConfig;
    int mJulianDayOnTop;
    private long mLastHandledEventId;
    private Time mLastHandledEventTime;
    private long mLastShownEventId;
    private boolean mOnAttachAllDay;
    private CalendarController.EventInfo mOnAttachedInfo;
    private Time mOriginalTime;
    private String mQuery;
    private boolean mShowEventDetailsWithAgenda;
    private final Runnable mTZUpdater;
    private final Time mTime;
    private String mTimeZone;
    private boolean mUsedForSearch;
    private boolean mUserScrolled;
    private static final String TAG = AgendaFragment.class.getSimpleName();
    private static boolean DEBUG = false;

    public AgendaFragment() {
        this(0L, false);
    }

    public AgendaFragment(long j, boolean z) {
        this.mUsedForSearch = false;
        this.mOnAttachedInfo = null;
        this.mOnAttachAllDay = false;
        this.mAdapter = null;
        this.mForceReplace = true;
        this.mUserScrolled = false;
        this.mLastShownEventId = -1L;
        this.mEventView = null;
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
        this.mOriginalTime = new Time();
        this.mInitialTimeMillis = j;
        this.mOriginalTime.set(j);
        this.mOriginalTime.normalize(false);
        this.mTime = new Time();
        this.mLastHandledEventTime = new Time();
        if (this.mInitialTimeMillis == 0) {
            this.mTime.setToNow();
        } else {
            this.mTime.set(this.mInitialTimeMillis);
        }
        this.mLastHandledEventTime.set(this.mTime);
        this.mUsedForSearch = z;
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
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mController = CalendarController.getInstance(this.mActivity);
        if (getActivity() instanceof AgendaChoiceActivity) {
            this.mShowEventDetailsWithAgenda = false;
        } else {
            this.mShowEventDetailsWithAgenda = Utils.getConfigBool(this.mActivity, R.bool.show_event_details_with_agenda);
        }
        this.mIsTabletConfig = Utils.getConfigBool(this.mActivity, R.bool.tablet_config);
        if (bundle != null) {
            long j = bundle.getLong("key_restore_time", -1L);
            if (j != -1) {
                this.mTime.set(j);
                if (DEBUG) {
                    Log.d(TAG, "Restoring time to " + this.mTime.toString());
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View view;
        int i = this.mActivity.getResources().getDisplayMetrics().widthPixels;
        View viewExtInflateFragmentView = extInflateFragmentView(layoutInflater);
        this.mAgendaListView = extFindListView(viewExtInflateFragmentView);
        this.mAgendaListView.setClickable(true);
        if (bundle != null) {
            long j = bundle.getLong("key_restore_instance_id", -1L);
            if (j != -1) {
                this.mAgendaListView.setSelectedInstanceId(j);
            }
            if (!this.mShowEventDetailsWithAgenda) {
                AgendaWindowAdapter.setTopDeviation(bundle.getIntArray("key_restore_top_deviation"));
            }
        }
        this.mEventView = viewExtInflateFragmentView.findViewById(R.id.agenda_event_info);
        if (!this.mShowEventDetailsWithAgenda) {
            this.mEventView.setVisibility(8);
        }
        StickyHeaderListView stickyHeaderListView = (StickyHeaderListView) viewExtInflateFragmentView.findViewById(R.id.agenda_sticky_header_list);
        if (stickyHeaderListView != 0) {
            ?? adapter = this.mAgendaListView.getAdapter();
            stickyHeaderListView.setAdapter(adapter);
            if (adapter instanceof HeaderViewListAdapter) {
                this.mAdapter = (AgendaWindowAdapter) adapter.getWrappedAdapter();
                stickyHeaderListView.setIndexer(this.mAdapter);
                stickyHeaderListView.setHeaderHeightListener(this.mAdapter);
            } else if (adapter instanceof AgendaWindowAdapter) {
                this.mAdapter = adapter;
                stickyHeaderListView.setIndexer(this.mAdapter);
                stickyHeaderListView.setHeaderHeightListener(this.mAdapter);
            } else {
                Log.wtf(TAG, "Cannot find HeaderIndexer for StickyHeaderListView");
            }
            stickyHeaderListView.setOnScrollListener(this);
            stickyHeaderListView.setHeaderSeparator(getResources().getColor(R.color.agenda_list_separator_color), 1);
            view = stickyHeaderListView;
        } else {
            view = this.mAgendaListView;
        }
        if (!this.mShowEventDetailsWithAgenda) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = i;
            view.setLayoutParams(layoutParams);
        } else {
            ViewGroup.LayoutParams layoutParams2 = view.getLayoutParams();
            layoutParams2.width = (i * 4) / 10;
            view.setLayoutParams(layoutParams2);
            ViewGroup.LayoutParams layoutParams3 = this.mEventView.getLayoutParams();
            layoutParams3.width = i - layoutParams2.width;
            this.mEventView.setLayoutParams(layoutParams3);
        }
        return viewExtInflateFragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) {
            Log.v(TAG, "OnResume to " + this.mTime.toString());
        }
        this.mAgendaListView.setHideDeclinedEvents(GeneralPreferences.getSharedPreferences(getActivity()).getBoolean("preferences_hide_declined", false));
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
    public void onSaveInstanceState(Bundle bundle) {
        long jCurrentTimeMillis;
        if (DEBUG) {
            Log.e(TAG, "OnSaveInstanceState start time: " + System.currentTimeMillis());
        }
        if (this.mAgendaListView == null) {
            return;
        }
        if (this.mShowEventDetailsWithAgenda) {
            if (this.mLastHandledEventTime != null) {
                jCurrentTimeMillis = this.mLastHandledEventTime.toMillis(true);
                this.mTime.set(this.mLastHandledEventTime);
            } else {
                jCurrentTimeMillis = System.currentTimeMillis();
                this.mTime.set(jCurrentTimeMillis);
            }
            bundle.putLong("key_restore_time", jCurrentTimeMillis);
            this.mController.setTime(jCurrentTimeMillis);
        } else {
            AgendaWindowAdapter.AgendaItem firstVisibleAgendaItem = this.mAgendaListView.getFirstVisibleAgendaItem();
            int firstVisiblePosition = this.mAgendaListView.getFirstVisiblePosition();
            int count = this.mAdapter.getCount();
            if (firstVisibleAgendaItem == null) {
                if (firstVisiblePosition == 0 && count > 0) {
                    firstVisibleAgendaItem = this.mAdapter.getAgendaItemByPosition(1, false);
                }
            } else if (firstVisibleAgendaItem.id == 0 && count > 0) {
                firstVisibleAgendaItem = this.mAdapter.getAgendaItemByPosition(firstVisiblePosition + 1, false);
            }
            if (firstVisibleAgendaItem != null) {
                long firstVisibleTime = this.mAgendaListView.getFirstVisibleTime(firstVisibleAgendaItem);
                if (firstVisibleTime > 0) {
                    this.mTime.set(firstVisibleTime);
                    this.mController.setTime(firstVisibleTime);
                    bundle.putLong("key_restore_time", firstVisibleTime);
                } else {
                    Log.i(TAG, "firstVisibleTime=" + firstVisibleTime);
                }
                this.mLastShownEventId = firstVisibleAgendaItem.id;
                this.mLastHandledEventId = this.mLastShownEventId;
                this.mLastHandledEventTime = this.mTime;
            }
            bundle.putIntArray("key_restore_top_deviation", this.mAdapter.saveTopDeviation(this.mTime, this.mLastShownEventId));
        }
        if (DEBUG) {
            Log.v(TAG, "onSaveInstanceState " + this.mTime.toString());
        }
        long selectedInstanceId = this.mAgendaListView.getSelectedInstanceId();
        if (selectedInstanceId >= 0) {
            bundle.putLong("key_restore_instance_id", selectedInstanceId);
        }
        super.onSaveInstanceState(bundle);
    }

    public void removeFragments(FragmentManager fragmentManager) {
        if (getActivity().isFinishing()) {
            return;
        }
        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        Fragment fragmentFindFragmentById = fragmentManager.findFragmentById(R.id.agenda_event_info);
        if (fragmentFindFragmentById != null) {
            fragmentTransactionBeginTransaction.remove(fragmentFindFragmentById);
        }
        fragmentTransactionBeginTransaction.commit();
    }

    @Override
    public void onPause() {
        this.mAgendaListView.onPause();
        super.onPause();
    }

    private void goTo(CalendarController.EventInfo eventInfo, boolean z) {
        if (this.mIsTabletConfig && this.mShowEventDetailsWithAgenda && eventInfo.id == -1 && eventInfo.endTime != null) {
            showEventInfo(eventInfo, false, false);
            return;
        }
        if (eventInfo.selectedTime != null) {
            this.mTime.set(eventInfo.selectedTime);
        } else if (eventInfo.startTime != null) {
            this.mTime.set(eventInfo.startTime);
        }
        if (this.mAgendaListView == null) {
            return;
        }
        this.mAgendaListView.goTo(this.mTime, eventInfo.id, this.mQuery, false, (eventInfo.extraLong & 8) != 0 && this.mShowEventDetailsWithAgenda);
        AgendaAdapter.ViewHolder selectedViewHolder = this.mAgendaListView.getSelectedViewHolder();
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("selected viewholder is null: ");
        sb.append(selectedViewHolder == null);
        Log.d(str, sb.toString());
        showEventInfo(eventInfo, selectedViewHolder != null ? selectedViewHolder.allDay : false, this.mForceReplace);
        this.mForceReplace = false;
    }

    private void search(String str, Time time) {
        this.mQuery = str;
        if (time != null) {
            this.mTime.set(time);
        }
        if (this.mAgendaListView == null) {
            return;
        }
        this.mAgendaListView.goTo(time, -1L, this.mQuery, true, false);
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
    public void handleEvent(CalendarController.EventInfo eventInfo) {
        if (eventInfo.eventType != 32) {
            if (eventInfo.eventType == 256) {
                search(eventInfo.query, eventInfo.startTime);
                return;
            } else {
                if (eventInfo.eventType == 128) {
                    eventsChanged();
                    return;
                }
                return;
            }
        }
        if (this.mIsTabletConfig && this.mShowEventDetailsWithAgenda && eventInfo.id == -1 && eventInfo.endTime != null) {
            goTo(eventInfo, false);
            return;
        }
        this.mLastHandledEventId = eventInfo.id;
        this.mLastHandledEventTime = eventInfo.selectedTime != null ? eventInfo.selectedTime : eventInfo.startTime;
        goTo(eventInfo, true);
    }

    public long getLastShowEventId() {
        return this.mLastShownEventId;
    }

    private void showEventInfo(CalendarController.EventInfo eventInfo, boolean z, boolean z2) {
        if (this.mIsTabletConfig && this.mShowEventDetailsWithAgenda && eventInfo.id == -1 && eventInfo.endTime != null) {
            this.mEventView.setVisibility(4);
            Log.d(TAG, "showEventInfo, event ID is -1, set eventView INVISIBLE ");
            return;
        }
        if (this.mIsTabletConfig && this.mShowEventDetailsWithAgenda && this.mEventView.getVisibility() == 4) {
            this.mEventView.setVisibility(0);
        }
        if (eventInfo.id == -1) {
            Log.e(TAG, "showEventInfo, event ID = " + eventInfo.id);
            if (this.mIsTabletConfig) {
                this.mAgendaListView.refresh(true);
                return;
            }
            return;
        }
        this.mLastShownEventId = eventInfo.id;
        if (this.mShowEventDetailsWithAgenda) {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager == null) {
                this.mOnAttachedInfo = eventInfo;
                this.mOnAttachAllDay = z;
                return;
            }
            FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
            if (z) {
                eventInfo.startTime.timezone = "UTC";
                eventInfo.endTime.timezone = "UTC";
            }
            if (DEBUG) {
                Log.d(TAG, "***");
                Log.d(TAG, "showEventInfo: start: " + new Date(eventInfo.startTime.toMillis(true)));
                Log.d(TAG, "showEventInfo: end: " + new Date(eventInfo.endTime.toMillis(true)));
                Log.d(TAG, "showEventInfo: all day: " + z);
                Log.d(TAG, "***");
            }
            long millis = eventInfo.startTime.toMillis(true);
            long millis2 = eventInfo.endTime.toMillis(true);
            EventInfoFragment eventInfoFragment = (EventInfoFragment) fragmentManager.findFragmentById(R.id.agenda_event_info);
            if (eventInfoFragment == null || z2 || eventInfoFragment.getStartMillis() != millis || eventInfoFragment.getEndMillis() != millis2 || eventInfoFragment.getEventId() != eventInfo.id) {
                this.mEventFragment = new EventInfoFragment((Context) this.mActivity, eventInfo.id, millis, millis2, 0, false, 1, (ArrayList<CalendarEventModel.ReminderEntry>) null);
                fragmentTransactionBeginTransaction.replace(R.id.agenda_event_info, this.mEventFragment);
                fragmentTransactionBeginTransaction.commit();
                return;
            }
            eventInfoFragment.reloadEvents();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        if (this.mAdapter != null) {
            this.mAdapter.setScrollState(i);
        }
        if (i == 1) {
            this.mUserScrolled = true;
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        int julianDayFromPosition;
        if (this.mUserScrolled && (julianDayFromPosition = this.mAgendaListView.getJulianDayFromPosition(i - this.mAgendaListView.getHeaderViewsCount())) != 0 && this.mJulianDayOnTop != julianDayFromPosition) {
            this.mJulianDayOnTop = julianDayFromPosition;
            this.mController.setTime(getTimeOnTopEvent().toMillis(true));
            if (!this.mIsTabletConfig) {
                absListView.post(new Runnable() {
                    @Override
                    public void run() {
                        Time timeOnTopEvent = AgendaFragment.this.getTimeOnTopEvent();
                        AgendaFragment.this.mController.sendEvent(this, 1024L, timeOnTopEvent, timeOnTopEvent, null, -1L, 0, 0L, null, null);
                    }
                });
            }
        }
    }

    protected View extInflateFragmentView(LayoutInflater layoutInflater) {
        return layoutInflater.inflate(R.layout.agenda_fragment, (ViewGroup) null);
    }

    protected AgendaListView extFindListView(View view) {
        return (AgendaListView) view.findViewById(R.id.agenda_events_list);
    }

    private Time getTimeOnTopEvent() {
        Time time = new Time(this.mTimeZone);
        time.setJulianDay(this.mJulianDayOnTop);
        time.hour = this.mOriginalTime.hour;
        time.minute = this.mOriginalTime.minute;
        time.second = this.mOriginalTime.second;
        return time;
    }
}
