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
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.selectevent.AgendaChoiceActivity;

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

    public AgendaListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
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
        boolean z = context instanceof AgendaChoiceActivity;
        if (z) {
            this.mWindowAdapter = new AgendaWindowAdapter(context, this, false);
        } else {
            this.mWindowAdapter = new AgendaWindowAdapter(context, this, Utils.getConfigBool(context, R.bool.show_event_details_with_agenda));
        }
        this.mWindowAdapter.setSelectedInstanceId(-1L);
        setAdapter((ListAdapter) this.mWindowAdapter);
        setCacheColorHint(context.getResources().getColor(R.color.agenda_item_not_selected));
        this.mDeleteEventHelper = new DeleteEventHelper(context, null, false);
        if (z) {
            this.mShowEventDetailsWithAgenda = false;
        } else {
            this.mShowEventDetailsWithAgenda = Utils.getConfigBool(this.mContext, R.bool.show_event_details_with_agenda);
        }
        setDivider(null);
        setDividerHeight(0);
        this.mHandler = new Handler();
    }

    private void setPastEventsUpdater() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        this.mHandler.removeCallbacks(this.mPastEventUpdater);
        this.mHandler.postDelayed(this.mPastEventUpdater, 300000 - (jCurrentTimeMillis - ((jCurrentTimeMillis / 300000) * 300000)));
    }

    private void resetPastEventsUpdater() {
        this.mHandler.removeCallbacks(this.mPastEventUpdater);
    }

    private boolean updatePastEvents() {
        int childCount = getChildCount();
        long jCurrentTimeMillis = System.currentTimeMillis();
        Time time = new Time(this.mTimeZone);
        time.set(jCurrentTimeMillis);
        int julianDay = Time.getJulianDay(jCurrentTimeMillis, time.gmtoff);
        for (int i = 0; i < childCount; i++) {
            ?? tag = getChildAt(i).getTag();
            if (tag instanceof AgendaByDayAdapter.ViewHolder) {
                if (tag.julianDay <= julianDay && !tag.grayed) {
                    return true;
                }
            } else if ((tag instanceof AgendaAdapter.ViewHolder) && !tag.grayed) {
                if (!tag.allDay && tag.startTimeMilli <= jCurrentTimeMillis) {
                    return true;
                }
                if (tag.allDay && tag.julianDay <= julianDay) {
                    return true;
                }
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
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        long j2;
        long j3;
        if (j != -1) {
            AgendaWindowAdapter.AgendaItem agendaItemByPosition = this.mWindowAdapter.getAgendaItemByPosition(i);
            long selectedInstanceId = this.mWindowAdapter.getSelectedInstanceId();
            this.mWindowAdapter.setSelectedView(view);
            if (agendaItemByPosition != null) {
                if (selectedInstanceId != this.mWindowAdapter.getSelectedInstanceId() || !this.mShowEventDetailsWithAgenda) {
                    long j4 = agendaItemByPosition.begin;
                    long j5 = agendaItemByPosition.end;
                    ?? tag = view.getTag();
                    long j6 = tag instanceof AgendaAdapter.ViewHolder ? tag.startTimeMilli : j4;
                    if (agendaItemByPosition.allDay) {
                        long jConvertAlldayLocalToUTC = Utils.convertAlldayLocalToUTC(this.mTime, j4, this.mTimeZone);
                        long jConvertAlldayLocalToUTC2 = Utils.convertAlldayLocalToUTC(this.mTime, j5, this.mTimeZone);
                        j3 = jConvertAlldayLocalToUTC;
                        j2 = jConvertAlldayLocalToUTC2;
                    } else {
                        j2 = j5;
                        j3 = j4;
                    }
                    this.mTime.set(j3);
                    CalendarController calendarController = CalendarController.getInstance(this.mContext);
                    LogUtil.oi("AgendaListView", "onItemClick, the clicked event id is " + agendaItemByPosition.id);
                    calendarController.sendEventRelatedEventWithExtra(this, 2L, agendaItemByPosition.id, j3, j2, 0, 0, CalendarController.EventInfo.buildViewExtraLong(0, agendaItemByPosition.allDay), j6);
                }
            }
        }
    }

    public void goTo(Time time, long j, String str, boolean z, boolean z2) {
        if (time == null) {
            time = this.mTime;
            long firstVisibleTime = getFirstVisibleTime(null);
            if (firstVisibleTime <= 0) {
                firstVisibleTime = System.currentTimeMillis();
            }
            time.set(firstVisibleTime);
        }
        this.mTime.set(time);
        this.mTime.switchTimezone(this.mTimeZone);
        this.mTime.normalize(true);
        this.mWindowAdapter.refresh(this.mTime, j, str, z, z2);
    }

    public void refresh(boolean z) {
        this.mWindowAdapter.refresh(this.mTime, -1L, null, z, false);
    }

    public View getFirstVisibleView() {
        Rect rect = new Rect();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            childAt.getLocalVisibleRect(rect);
            if (rect.top >= 0) {
                return childAt;
            }
        }
        return null;
    }

    public AgendaAdapter.ViewHolder getSelectedViewHolder() {
        return this.mWindowAdapter.getSelectedViewHolder();
    }

    public long getFirstVisibleTime(AgendaWindowAdapter.AgendaItem agendaItem) {
        if (agendaItem == null) {
            agendaItem = getFirstVisibleAgendaItem();
        }
        if (agendaItem != null) {
            Time time = new Time(this.mTimeZone);
            time.set(agendaItem.begin);
            int i = time.hour;
            int i2 = time.minute;
            int i3 = time.second;
            time.setJulianDay(agendaItem.startDay);
            time.hour = i;
            time.minute = i2;
            time.second = i3;
            return time.normalize(false);
        }
        return 0L;
    }

    public AgendaWindowAdapter.AgendaItem getFirstVisibleAgendaItem() {
        View firstVisibleView;
        int firstVisiblePosition = getFirstVisiblePosition();
        if (this.mShowEventDetailsWithAgenda && (firstVisibleView = getFirstVisibleView()) != null) {
            Rect rect = new Rect();
            firstVisibleView.getLocalVisibleRect(rect);
            if (rect.bottom - rect.top <= this.mWindowAdapter.getStickyHeaderHeight()) {
                firstVisiblePosition++;
            }
        }
        return this.mWindowAdapter.getAgendaItemByPosition(firstVisiblePosition, false);
    }

    public int getJulianDayFromPosition(int i) {
        AgendaWindowAdapter.DayAdapterInfo adapterInfoByPosition = this.mWindowAdapter.getAdapterInfoByPosition(i);
        if (adapterInfoByPosition != null) {
            return adapterInfoByPosition.dayAdapter.findJulianDayFromPosition(i - adapterInfoByPosition.offset);
        }
        return 0;
    }

    public boolean isAgendaItemVisible(Time time, long j) {
        View childAt;
        if (j == -1 || time == null || (childAt = getChildAt(0)) == null) {
            return false;
        }
        int positionForView = getPositionForView(childAt);
        long millis = time.toMillis(true);
        int childCount = getChildCount();
        int count = this.mWindowAdapter.getCount();
        for (int i = 0; i < childCount; i++) {
            int i2 = i + positionForView;
            if (i2 >= count) {
                break;
            }
            AgendaWindowAdapter.AgendaItem agendaItemByPosition = this.mWindowAdapter.getAgendaItemByPosition(i2);
            if (agendaItemByPosition != null && agendaItemByPosition.id == j && agendaItemByPosition.begin == millis) {
                View childAt2 = getChildAt(i);
                if (childAt2.getTop() <= getHeight() && childAt2.getTop() >= this.mWindowAdapter.getStickyHeaderHeight()) {
                    return true;
                }
            }
        }
        return false;
    }

    public long getSelectedInstanceId() {
        return this.mWindowAdapter.getSelectedInstanceId();
    }

    public void setSelectedInstanceId(long j) {
        this.mWindowAdapter.setSelectedInstanceId(j);
    }

    public void shiftSelection(int i) {
        shiftPosition(i);
        int selectedItemPosition = getSelectedItemPosition();
        if (selectedItemPosition != -1) {
            setSelectionFromTop(selectedItemPosition + i, 0);
        }
    }

    private void shiftPosition(int i) {
        View firstVisibleView = getFirstVisibleView();
        if (firstVisibleView != null) {
            Rect rect = new Rect();
            firstVisibleView.getLocalVisibleRect(rect);
            setSelectionFromTop(getPositionForView(firstVisibleView) + i, rect.top > 0 ? -rect.top : rect.top);
        } else if (getSelectedItemPosition() >= 0) {
            setSelection(getSelectedItemPosition() + i);
        }
    }

    public void setHideDeclinedEvents(boolean z) {
        this.mWindowAdapter.setHideDeclinedEvents(z);
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

    protected long getEventIdByPosition(int i) {
        AgendaWindowAdapter.AgendaItem agendaItemByPosition;
        if (i > 0 && i <= this.mWindowAdapter.getCount() && (agendaItemByPosition = this.mWindowAdapter.getAgendaItemByPosition(i)) != null) {
            return agendaItemByPosition.id;
        }
        return -1L;
    }
}
