package com.android.calendar.agenda;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.agenda.AgendaAdapter;
import com.android.calendar.agenda.AgendaWindowAdapter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

public class AgendaByDayAdapter extends BaseAdapter {
    private final AgendaAdapter mAgendaAdapter;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private ArrayList<RowInfo> mRowInfo;
    private String mTimeZone;
    private Time mTmpTime;
    private int mTodayJulianDay;
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            AgendaByDayAdapter.this.mTimeZone = Utils.getTimeZone(AgendaByDayAdapter.this.mContext, this);
            AgendaByDayAdapter.this.mTmpTime = new Time(AgendaByDayAdapter.this.mTimeZone);
            AgendaByDayAdapter.this.notifyDataSetChanged();
        }
    };
    private final StringBuilder mStringBuilder = new StringBuilder(50);
    private final Formatter mFormatter = new Formatter(this.mStringBuilder, Locale.getDefault());

    static class ViewHolder {
        TextView dateView;
        TextView dayView;
        boolean grayed;
        int julianDay;

        ViewHolder() {
        }
    }

    public AgendaByDayAdapter(Context context) {
        this.mContext = context;
        this.mAgendaAdapter = new AgendaAdapter(context, R.layout.agenda_item);
        this.mInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        this.mTimeZone = Utils.getTimeZone(context, this.mTZUpdater);
        this.mTmpTime = new Time(this.mTimeZone);
    }

    public long getInstanceId(int position) {
        if (this.mRowInfo == null || position >= this.mRowInfo.size()) {
            return -1L;
        }
        return this.mRowInfo.get(position).mInstanceId;
    }

    public long getStartTime(int position) {
        if (this.mRowInfo == null || position >= this.mRowInfo.size()) {
            return -1L;
        }
        return this.mRowInfo.get(position).mEventStartTimeMilli;
    }

    public int getHeaderPosition(int position) {
        if (this.mRowInfo == null || position >= this.mRowInfo.size()) {
            return -1;
        }
        for (int i = position; i >= 0; i--) {
            RowInfo row = this.mRowInfo.get(i);
            if (row != null && row.mType == 0) {
                return i;
            }
        }
        return -1;
    }

    public int getHeaderItemsCount(int position) {
        if (this.mRowInfo == null) {
            return -1;
        }
        int count = 0;
        for (int i = position + 1; i < this.mRowInfo.size() && this.mRowInfo.get(i).mType == 1; i++) {
            count++;
        }
        return count;
    }

    @Override
    public int getCount() {
        return this.mRowInfo != null ? this.mRowInfo.size() : this.mAgendaAdapter.getCount();
    }

    @Override
    public Object getItem(int position) {
        if (this.mRowInfo != null) {
            RowInfo row = this.mRowInfo.get(position);
            if (row.mType != 0) {
                return this.mAgendaAdapter.getItem(row.mPosition);
            }
            return row;
        }
        return this.mAgendaAdapter.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        if (this.mRowInfo != null) {
            RowInfo row = this.mRowInfo.get(position);
            if (row.mType == 0) {
                return -position;
            }
            return this.mAgendaAdapter.getItemId(row.mPosition);
        }
        return this.mAgendaAdapter.getItemId(position);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (this.mRowInfo == null || this.mRowInfo.size() <= position) {
            return 0;
        }
        return this.mRowInfo.get(position).mType;
    }

    public boolean isDayHeaderView(int position) {
        return getItemViewType(position) == 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (this.mRowInfo == null || position > this.mRowInfo.size()) {
            return this.mAgendaAdapter.getView(position, convertView, parent);
        }
        RowInfo row = this.mRowInfo.get(position);
        if (row.mType == 0) {
            ViewHolder holder = null;
            View agendaDayView = null;
            if (convertView != null && convertView.getTag() != null) {
                Object tag = convertView.getTag();
                if (tag instanceof ViewHolder) {
                    agendaDayView = convertView;
                    holder = (ViewHolder) tag;
                    holder.julianDay = row.mDay;
                }
            }
            if (holder == null) {
                holder = new ViewHolder();
                agendaDayView = this.mInflater.inflate(R.layout.agenda_day, parent, false);
                holder.dayView = (TextView) agendaDayView.findViewById(R.id.day);
                holder.dateView = (TextView) agendaDayView.findViewById(R.id.date);
                holder.julianDay = row.mDay;
                holder.grayed = false;
                agendaDayView.setTag(holder);
            }
            String tz = Utils.getTimeZone(this.mContext, this.mTZUpdater);
            if (!TextUtils.equals(tz, this.mTmpTime.timezone)) {
                this.mTimeZone = tz;
                this.mTmpTime = new Time(tz);
            }
            Time date = this.mTmpTime;
            long millis = date.setJulianDay(row.mDay);
            this.mStringBuilder.setLength(0);
            String dayViewText = Utils.getDayOfWeekString(row.mDay, this.mTodayJulianDay, millis, this.mContext);
            this.mStringBuilder.setLength(0);
            String dateViewText = DateUtils.formatDateRange(this.mContext, this.mFormatter, millis, millis, 16, this.mTimeZone).toString();
            holder.dayView.setText(dayViewText);
            holder.dateView.setText(dateViewText);
            if (row.mDay > this.mTodayJulianDay) {
                agendaDayView.setBackgroundResource(R.drawable.agenda_item_bg_primary);
                holder.grayed = false;
                return agendaDayView;
            }
            agendaDayView.setBackgroundResource(R.drawable.agenda_item_bg_secondary);
            holder.grayed = true;
            return agendaDayView;
        }
        if (row.mType == 1) {
            View itemView = this.mAgendaAdapter.getView(row.mPosition, convertView, parent);
            AgendaAdapter.ViewHolder holder2 = (AgendaAdapter.ViewHolder) itemView.getTag();
            TextView title = holder2.title;
            holder2.startTimeMilli = row.mEventStartTimeMilli;
            boolean allDay = holder2.allDay;
            title.setText(title.getText());
            if ((!allDay && row.mEventStartTimeMilli <= System.currentTimeMillis()) || (allDay && row.mDay <= this.mTodayJulianDay)) {
                itemView.setBackgroundResource(R.drawable.agenda_item_bg_secondary);
                title.setTypeface(Typeface.DEFAULT);
                holder2.grayed = true;
            } else {
                itemView.setBackgroundResource(R.drawable.agenda_item_bg_primary);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                holder2.grayed = false;
            }
            holder2.julianDay = row.mDay;
            return itemView;
        }
        throw new IllegalStateException("Unknown event type:" + row.mType);
    }

    public void changeCursor(AgendaWindowAdapter.DayAdapterInfo info) {
        calculateDays(info);
        this.mAgendaAdapter.changeCursor(info.cursor);
    }

    public void calculateDays(AgendaWindowAdapter.DayAdapterInfo dayAdapterInfo) {
        Cursor cursor = dayAdapterInfo.cursor;
        ArrayList<RowInfo> rowInfo = new ArrayList<>();
        int prevStartDay = -1;
        Time tempTime = new Time(this.mTimeZone);
        long now = System.currentTimeMillis();
        tempTime.set(now);
        this.mTodayJulianDay = Time.getJulianDay(now, tempTime.gmtoff);
        LinkedList<MultipleDayInfo> multipleDayList = new LinkedList<>();
        int position = 0;
        while (cursor.moveToNext()) {
            int startDay = cursor.getInt(10);
            long id = cursor.getLong(9);
            long startTime = cursor.getLong(7);
            long endTime = cursor.getLong(8);
            long instanceId = cursor.getLong(0);
            boolean allDay = cursor.getInt(3) != 0;
            if (allDay) {
                startTime = Utils.convertAlldayUtcToLocal(tempTime, startTime, this.mTimeZone);
                endTime = Utils.convertAlldayUtcToLocal(tempTime, endTime, this.mTimeZone);
            }
            int startDay2 = Math.max(startDay, dayAdapterInfo.start);
            long adapterStartTime = tempTime.setJulianDay(startDay2);
            long startTime2 = Math.max(startTime, adapterStartTime);
            if (startDay2 != prevStartDay) {
                if (prevStartDay == -1) {
                    rowInfo.add(new RowInfo(0, startDay2));
                } else {
                    boolean dayHeaderAdded = false;
                    int currentDay = prevStartDay + 1;
                    while (currentDay <= startDay2) {
                        dayHeaderAdded = false;
                        Iterator<MultipleDayInfo> iter = multipleDayList.iterator();
                        while (iter.hasNext()) {
                            MultipleDayInfo info = iter.next();
                            if (info.mEndDay < currentDay) {
                                iter.remove();
                            } else {
                                if (!dayHeaderAdded) {
                                    rowInfo.add(new RowInfo(0, currentDay));
                                    dayHeaderAdded = true;
                                }
                                long nextMidnight = Utils.getNextMidnight(tempTime, info.mEventStartTimeMilli, this.mTimeZone);
                                long infoEndTime = info.mEndDay == currentDay ? info.mEventEndTimeMilli : nextMidnight;
                                rowInfo.add(new RowInfo(1, currentDay, info.mPosition, info.mEventId, info.mEventStartTimeMilli, infoEndTime, info.mInstanceId, info.mAllDay));
                                info.mEventStartTimeMilli = nextMidnight;
                            }
                        }
                        currentDay++;
                    }
                    if (!dayHeaderAdded) {
                        rowInfo.add(new RowInfo(0, startDay2));
                    }
                }
                prevStartDay = startDay2;
            }
            int endDay = Math.min(cursor.getInt(11), dayAdapterInfo.end);
            if (endDay > startDay2) {
                long nextMidnight2 = Utils.getNextMidnight(tempTime, startTime2, this.mTimeZone);
                multipleDayList.add(new MultipleDayInfo(position, endDay, id, nextMidnight2, endTime, instanceId, allDay));
                rowInfo.add(new RowInfo(1, startDay2, position, id, startTime2, nextMidnight2, instanceId, allDay));
            } else {
                rowInfo.add(new RowInfo(1, startDay2, position, id, startTime2, endTime, instanceId, allDay));
            }
            position++;
        }
        if (prevStartDay > 0) {
            int currentDay2 = prevStartDay + 1;
            while (currentDay2 <= dayAdapterInfo.end) {
                boolean dayHeaderAdded2 = false;
                Iterator<MultipleDayInfo> iter2 = multipleDayList.iterator();
                while (iter2.hasNext()) {
                    MultipleDayInfo info2 = iter2.next();
                    if (info2.mEndDay < currentDay2) {
                        iter2.remove();
                    } else {
                        if (!dayHeaderAdded2) {
                            rowInfo.add(new RowInfo(0, currentDay2));
                            dayHeaderAdded2 = true;
                        }
                        long nextMidnight3 = Utils.getNextMidnight(tempTime, info2.mEventStartTimeMilli, this.mTimeZone);
                        long infoEndTime2 = info2.mEndDay == currentDay2 ? info2.mEventEndTimeMilli : nextMidnight3;
                        rowInfo.add(new RowInfo(1, currentDay2, info2.mPosition, info2.mEventId, info2.mEventStartTimeMilli, infoEndTime2, info2.mInstanceId, info2.mAllDay));
                        info2.mEventStartTimeMilli = nextMidnight3;
                    }
                }
                currentDay2++;
            }
        }
        this.mRowInfo = rowInfo;
    }

    private static class RowInfo {
        final boolean mAllDay;
        final int mDay;
        final long mEventEndTimeMilli;
        final long mEventId;
        final long mEventStartTimeMilli;
        boolean mFirstDayAfterYesterday;
        final long mInstanceId;
        final int mPosition;
        final int mType;

        RowInfo(int type, int julianDay, int position, long id, long startTime, long endTime, long instanceId, boolean allDay) {
            this.mType = type;
            this.mDay = julianDay;
            this.mPosition = position;
            this.mEventId = id;
            this.mEventStartTimeMilli = startTime;
            this.mEventEndTimeMilli = endTime;
            this.mFirstDayAfterYesterday = false;
            this.mInstanceId = instanceId;
            this.mAllDay = allDay;
        }

        RowInfo(int type, int julianDay) {
            this.mType = type;
            this.mDay = julianDay;
            this.mPosition = 0;
            this.mEventId = 0L;
            this.mEventStartTimeMilli = 0L;
            this.mEventEndTimeMilli = 0L;
            this.mFirstDayAfterYesterday = false;
            this.mInstanceId = -1L;
            this.mAllDay = false;
        }
    }

    private static class MultipleDayInfo {
        final boolean mAllDay;
        final int mEndDay;
        long mEventEndTimeMilli;
        final long mEventId;
        long mEventStartTimeMilli;
        final long mInstanceId;
        final int mPosition;

        MultipleDayInfo(int position, int endDay, long id, long startTime, long endTime, long instanceId, boolean allDay) {
            this.mPosition = position;
            this.mEndDay = endDay;
            this.mEventId = id;
            this.mEventStartTimeMilli = startTime;
            this.mEventEndTimeMilli = endTime;
            this.mInstanceId = instanceId;
            this.mAllDay = allDay;
        }
    }

    public int findEventPositionNearestTime(Time time, long id) {
        if (this.mRowInfo == null) {
            return 0;
        }
        long millis = time.toMillis(false);
        long minDistance = 2147483647L;
        long idFoundMinDistance = 2147483647L;
        int minIndex = 0;
        int idFoundMinIndex = 0;
        int eventInTimeIndex = -1;
        int allDayEventInTimeIndex = -1;
        int allDayEventDay = 0;
        int minDay = 0;
        boolean idFound = false;
        int len = this.mRowInfo.size();
        for (int index = 0; index < len; index++) {
            RowInfo row = this.mRowInfo.get(index);
            if (row.mType != 0) {
                if (row.mEventId == id) {
                    if (row.mEventStartTimeMilli != millis) {
                        long distance = Math.abs(millis - row.mEventStartTimeMilli);
                        if (distance < idFoundMinDistance) {
                            idFoundMinDistance = distance;
                            idFoundMinIndex = index;
                        }
                        idFound = true;
                    } else {
                        return index;
                    }
                }
                if (!idFound) {
                    if (millis >= row.mEventStartTimeMilli && millis <= row.mEventEndTimeMilli) {
                        if (row.mAllDay) {
                            if (allDayEventInTimeIndex == -1) {
                                allDayEventInTimeIndex = index;
                                allDayEventDay = row.mDay;
                            }
                        } else if (eventInTimeIndex == -1) {
                            eventInTimeIndex = index;
                        }
                    } else if (eventInTimeIndex == -1) {
                        long distance2 = Math.abs(millis - row.mEventStartTimeMilli);
                        if (distance2 < minDistance) {
                            minDistance = distance2;
                            minIndex = index;
                            minDay = row.mDay;
                        }
                    }
                }
            }
        }
        if (!idFound) {
            if (eventInTimeIndex == -1) {
                if (allDayEventInTimeIndex == -1 || minDay == allDayEventDay) {
                    int index2 = minIndex;
                    return index2;
                }
                int index3 = allDayEventInTimeIndex;
                return index3;
            }
            int index4 = eventInTimeIndex;
            return index4;
        }
        int index5 = idFoundMinIndex;
        return index5;
    }

    public boolean isFirstDayAfterYesterday(int position) {
        int headerPos = getHeaderPosition(position);
        RowInfo row = this.mRowInfo.get(headerPos);
        if (row != null) {
            return row.mFirstDayAfterYesterday;
        }
        return false;
    }

    public int findJulianDayFromPosition(int position) {
        if (this.mRowInfo == null || position < 0) {
            return 0;
        }
        int len = this.mRowInfo.size();
        if (position >= len) {
            return 0;
        }
        for (int index = position; index >= 0; index--) {
            RowInfo row = this.mRowInfo.get(index);
            if (row.mType == 0) {
                return row.mDay;
            }
        }
        return 0;
    }

    public void setAsFirstDayAfterYesterday(int position) {
        if (this.mRowInfo != null && position >= 0 && position <= this.mRowInfo.size()) {
            RowInfo row = this.mRowInfo.get(position);
            row.mFirstDayAfterYesterday = true;
        }
    }

    public int getCursorPosition(int listPos) {
        int nextPos;
        if (this.mRowInfo != null && listPos >= 0) {
            RowInfo row = this.mRowInfo.get(listPos);
            if (row.mType == 1) {
                return row.mPosition;
            }
            int nextPos2 = listPos + 1;
            if (nextPos2 < this.mRowInfo.size() && (nextPos = getCursorPosition(nextPos2)) >= 0) {
                return -nextPos;
            }
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        if (this.mRowInfo == null || position >= this.mRowInfo.size()) {
            return true;
        }
        RowInfo row = this.mRowInfo.get(position);
        return row.mType == 1;
    }
}
