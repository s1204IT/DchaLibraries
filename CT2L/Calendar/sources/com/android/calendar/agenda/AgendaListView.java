package com.android.calendar.agenda;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.calendar.CalendarController;
import com.android.calendar.DeleteEventHelper;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.agenda.AgendaAdapter;
import com.android.calendar.agenda.AgendaByDayAdapter;
import com.android.calendar.agenda.AgendaWindowAdapter;

public class AgendaListView extends ListView implements AdapterView.OnItemClickListener {
    private Context mContext;
    private DeleteEventHelper mDeleteEventHelper;
    private Handler mHandler;
    private final Runnable mMidnightUpdater;
    private final Runnable mPastEventUpdater;
    private boolean mShowEventDetailsWithAgenda;
    private final Runnable mTZUpdater;
    private Time mTime;
    private String mTimeZone;
    private AgendaWindowAdapter mWindowAdapter;

    public AgendaListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHandler = null;
        this.mTZUpdater = new Runnable() {
            @Override
            public void run() {
                AgendaListView.this.mTimeZone = Utils.getTimeZone(AgendaListView.this.mContext, this);
                AgendaListView.this.mTime.switchTimezone(AgendaListView.this.mTimeZone);
            }
        };
        this.mMidnightUpdater = new Runnable() {
            @Override
            public void run() {
                AgendaListView.this.refresh(true);
                Utils.setMidnightUpdater(AgendaListView.this.mHandler, AgendaListView.this.mMidnightUpdater, AgendaListView.this.mTimeZone);
            }
        };
        this.mPastEventUpdater = new Runnable() {
            @Override
            public void run() {
                if (AgendaListView.this.updatePastEvents()) {
                    AgendaListView.this.refresh(true);
                }
                AgendaListView.this.setPastEventsUpdater();
            }
        };
        initView(context);
    }

    private void initView(Context context) {
        this.mContext = context;
        this.mTimeZone = Utils.getTimeZone(context, this.mTZUpdater);
        this.mTime = new Time(this.mTimeZone);
        setOnItemClickListener(this);
        setVerticalScrollBarEnabled(false);
        this.mWindowAdapter = new AgendaWindowAdapter(context, this, Utils.getConfigBool(context, R.bool.show_event_details_with_agenda));
        this.mWindowAdapter.setSelectedInstanceId(-1L);
        setAdapter((ListAdapter) this.mWindowAdapter);
        setCacheColorHint(context.getResources().getColor(R.color.agenda_item_not_selected));
        this.mDeleteEventHelper = new DeleteEventHelper(context, null, false);
        this.mShowEventDetailsWithAgenda = Utils.getConfigBool(this.mContext, R.bool.show_event_details_with_agenda);
        setDivider(null);
        setDividerHeight(0);
        this.mHandler = new Handler();
    }

    private void setPastEventsUpdater() {
        long now = System.currentTimeMillis();
        long roundedTime = (now / 300000) * 300000;
        this.mHandler.removeCallbacks(this.mPastEventUpdater);
        this.mHandler.postDelayed(this.mPastEventUpdater, 300000 - (now - roundedTime));
    }

    private void resetPastEventsUpdater() {
        this.mHandler.removeCallbacks(this.mPastEventUpdater);
    }

    private boolean updatePastEvents() {
        int childCount = getChildCount();
        long now = System.currentTimeMillis();
        Time time = new Time(this.mTimeZone);
        time.set(now);
        int todayJulianDay = Time.getJulianDay(now, time.gmtoff);
        for (int i = 0; i < childCount; i++) {
            View listItem = getChildAt(i);
            Object o = listItem.getTag();
            if (o instanceof AgendaByDayAdapter.ViewHolder) {
                AgendaByDayAdapter.ViewHolder holder = (AgendaByDayAdapter.ViewHolder) o;
                if (holder.julianDay <= todayJulianDay && !holder.grayed) {
                    return true;
                }
            } else if (o instanceof AgendaAdapter.ViewHolder) {
                AgendaAdapter.ViewHolder holder2 = (AgendaAdapter.ViewHolder) o;
                if (!holder2.grayed && ((!holder2.allDay && holder2.startTimeMilli <= now) || (holder2.allDay && holder2.julianDay <= todayJulianDay))) {
                    return true;
                }
            } else {
                continue;
            }
        }
        return false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mWindowAdapter.close();
    }

    @Override
    public void onItemClick(AdapterView<?> a, View v, int position, long id) throws Throwable {
        long holderStartTime;
        if (id != -1) {
            AgendaWindowAdapter.AgendaItem item = this.mWindowAdapter.getAgendaItemByPosition(position);
            long oldInstanceId = this.mWindowAdapter.getSelectedInstanceId();
            this.mWindowAdapter.setSelectedView(v);
            if (item != null) {
                if (oldInstanceId != this.mWindowAdapter.getSelectedInstanceId() || !this.mShowEventDetailsWithAgenda) {
                    long startTime = item.begin;
                    long endTime = item.end;
                    Object holder = v.getTag();
                    if (holder instanceof AgendaAdapter.ViewHolder) {
                        holderStartTime = ((AgendaAdapter.ViewHolder) holder).startTimeMilli;
                    } else {
                        holderStartTime = startTime;
                    }
                    if (item.allDay) {
                        startTime = Utils.convertAlldayLocalToUTC(this.mTime, startTime, this.mTimeZone);
                        endTime = Utils.convertAlldayLocalToUTC(this.mTime, endTime, this.mTimeZone);
                    }
                    this.mTime.set(startTime);
                    CalendarController controller = CalendarController.getInstance(this.mContext);
                    controller.sendEventRelatedEventWithExtra(this, 2L, item.id, startTime, endTime, 0, 0, CalendarController.EventInfo.buildViewExtraLong(0, item.allDay), holderStartTime);
                }
            }
        }
    }

    public void goTo(Time time, long id, String searchQuery, boolean forced, boolean refreshEventInfo) {
        if (time == null) {
            time = this.mTime;
            long goToTime = getFirstVisibleTime(null);
            if (goToTime <= 0) {
                goToTime = System.currentTimeMillis();
            }
            time.set(goToTime);
        }
        this.mTime.set(time);
        this.mTime.switchTimezone(this.mTimeZone);
        this.mTime.normalize(true);
        this.mWindowAdapter.refresh(this.mTime, id, searchQuery, forced, refreshEventInfo);
    }

    public void refresh(boolean forced) {
        this.mWindowAdapter.refresh(this.mTime, -1L, null, forced, false);
    }

    public View getFirstVisibleView() {
        Rect r = new Rect();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View listItem = getChildAt(i);
            listItem.getLocalVisibleRect(r);
            if (r.top >= 0) {
                return listItem;
            }
        }
        return null;
    }

    public AgendaAdapter.ViewHolder getSelectedViewHolder() {
        return this.mWindowAdapter.getSelectedViewHolder();
    }

    public long getFirstVisibleTime(AgendaWindowAdapter.AgendaItem item) {
        AgendaWindowAdapter.AgendaItem agendaItem = item;
        if (item == null) {
            agendaItem = getFirstVisibleAgendaItem();
        }
        if (agendaItem == null) {
            return 0L;
        }
        Time t = new Time(this.mTimeZone);
        t.set(agendaItem.begin);
        int hour = t.hour;
        int minute = t.minute;
        int second = t.second;
        t.setJulianDay(agendaItem.startDay);
        t.hour = hour;
        t.minute = minute;
        t.second = second;
        return t.normalize(false);
    }

    public AgendaWindowAdapter.AgendaItem getFirstVisibleAgendaItem() {
        View v;
        int position = getFirstVisiblePosition();
        if (this.mShowEventDetailsWithAgenda && (v = getFirstVisibleView()) != null) {
            Rect r = new Rect();
            v.getLocalVisibleRect(r);
            if (r.bottom - r.top <= this.mWindowAdapter.getStickyHeaderHeight()) {
                position++;
            }
        }
        return this.mWindowAdapter.getAgendaItemByPosition(position, false);
    }

    public int getJulianDayFromPosition(int position) {
        AgendaWindowAdapter.DayAdapterInfo info = this.mWindowAdapter.getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.findJulianDayFromPosition(position - info.offset);
        }
        return 0;
    }

    public boolean isAgendaItemVisible(Time startTime, long id) {
        View child;
        if (id == -1 || startTime == null || (child = getChildAt(0)) == null) {
            return false;
        }
        int start = getPositionForView(child);
        long milliTime = startTime.toMillis(true);
        int childCount = getChildCount();
        int eventsInAdapter = this.mWindowAdapter.getCount();
        for (int i = 0; i < childCount && i + start < eventsInAdapter; i++) {
            AgendaWindowAdapter.AgendaItem agendaItem = this.mWindowAdapter.getAgendaItemByPosition(i + start);
            if (agendaItem != null && agendaItem.id == id && agendaItem.begin == milliTime) {
                View listItem = getChildAt(i);
                if (listItem.getTop() <= getHeight() && listItem.getTop() >= this.mWindowAdapter.getStickyHeaderHeight()) {
                    return true;
                }
            }
        }
        return false;
    }

    public long getSelectedInstanceId() {
        return this.mWindowAdapter.getSelectedInstanceId();
    }

    public void setSelectedInstanceId(long id) {
        this.mWindowAdapter.setSelectedInstanceId(id);
    }

    public void shiftSelection(int offset) {
        shiftPosition(offset);
        int position = getSelectedItemPosition();
        if (position != -1) {
            setSelectionFromTop(position + offset, 0);
        }
    }

    private void shiftPosition(int offset) {
        View firstVisibleItem = getFirstVisibleView();
        if (firstVisibleItem != null) {
            Rect r = new Rect();
            firstVisibleItem.getLocalVisibleRect(r);
            int position = getPositionForView(firstVisibleItem);
            setSelectionFromTop(position + offset, r.top > 0 ? -r.top : r.top);
            return;
        }
        if (getSelectedItemPosition() >= 0) {
            setSelection(getSelectedItemPosition() + offset);
        }
    }

    public void setHideDeclinedEvents(boolean hideDeclined) {
        this.mWindowAdapter.setHideDeclinedEvents(hideDeclined);
    }

    public void onResume() {
        this.mTZUpdater.run();
        Utils.setMidnightUpdater(this.mHandler, this.mMidnightUpdater, this.mTimeZone);
        setPastEventsUpdater();
        this.mWindowAdapter.onResume();
    }

    public void onPause() {
        Utils.resetMidnightUpdater(this.mHandler, this.mMidnightUpdater);
        resetPastEventsUpdater();
    }
}
