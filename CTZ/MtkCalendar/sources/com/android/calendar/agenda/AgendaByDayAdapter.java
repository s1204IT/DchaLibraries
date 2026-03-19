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
import com.mediatek.calendar.ext.OpCalendarCustomizationFactoryBase;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

public class AgendaByDayAdapter extends BaseAdapter {
    private static final String TAG = AgendaByDayAdapter.class.getSimpleName();
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

    public long getInstanceId(int i) {
        if (this.mRowInfo == null || i >= this.mRowInfo.size()) {
            return -1L;
        }
        return this.mRowInfo.get(i).mInstanceId;
    }

    public long getStartTime(int i) {
        if (this.mRowInfo == null || i >= this.mRowInfo.size()) {
            return -1L;
        }
        return this.mRowInfo.get(i).mEventStartTimeMilli;
    }

    public int getHeaderPosition(int i) {
        if (this.mRowInfo == null || i >= this.mRowInfo.size()) {
            return -1;
        }
        while (i >= 0) {
            RowInfo rowInfo = this.mRowInfo.get(i);
            if (rowInfo == null || rowInfo.mType != 0) {
                i--;
            } else {
                return i;
            }
        }
        return -1;
    }

    public int getHeaderItemsCount(int i) {
        if (this.mRowInfo == null) {
            return -1;
        }
        int i2 = 0;
        for (int i3 = i + 1; i3 < this.mRowInfo.size(); i3++) {
            if (this.mRowInfo.get(i3).mType != 1) {
                return i2;
            }
            i2++;
        }
        return i2;
    }

    @Override
    public int getCount() {
        if (this.mRowInfo != null) {
            return this.mRowInfo.size();
        }
        return this.mAgendaAdapter.getCount();
    }

    @Override
    public Object getItem(int i) {
        if (this.mRowInfo != null) {
            RowInfo rowInfo = this.mRowInfo.get(i);
            if (rowInfo.mType == 0) {
                return rowInfo;
            }
            return this.mAgendaAdapter.getItem(rowInfo.mPosition);
        }
        return this.mAgendaAdapter.getItem(i);
    }

    @Override
    public long getItemId(int i) {
        if (this.mRowInfo != null) {
            RowInfo rowInfo = this.mRowInfo.get(i);
            if (rowInfo.mType == 0) {
                return -i;
            }
            return this.mAgendaAdapter.getItemId(rowInfo.mPosition);
        }
        return this.mAgendaAdapter.getItemId(i);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int i) {
        if (this.mRowInfo == null || this.mRowInfo.size() <= i) {
            return 0;
        }
        return this.mRowInfo.get(i).mType;
    }

    public boolean isDayHeaderView(int i) {
        return getItemViewType(i) == 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ?? Inflate;
        ?? viewHolder;
        View view2 = view;
        if (this.mRowInfo == null || i > this.mRowInfo.size()) {
            return this.mAgendaAdapter.getView(i, view2, viewGroup);
        }
        RowInfo rowInfo = this.mRowInfo.get(i);
        if (rowInfo.mType == 0) {
            if (view2 != null && view.getTag() != null) {
                ?? tag = view.getTag();
                if (tag instanceof ViewHolder) {
                    tag.julianDay = rowInfo.mDay;
                    Inflate = view2;
                    viewHolder = tag;
                }
            } else {
                Inflate = 0;
                viewHolder = 0;
            }
            if (viewHolder == 0) {
                viewHolder = new ViewHolder();
                Inflate = this.mInflater.inflate(R.layout.agenda_day, viewGroup, false);
                viewHolder.dayView = (TextView) Inflate.findViewById(R.id.day);
                viewHolder.dateView = (TextView) Inflate.findViewById(R.id.date);
                viewHolder.julianDay = rowInfo.mDay;
                viewHolder.grayed = false;
                Inflate.setTag(viewHolder);
            }
            String timeZone = Utils.getTimeZone(this.mContext, this.mTZUpdater);
            if (!TextUtils.equals(timeZone, this.mTmpTime.timezone)) {
                this.mTimeZone = timeZone;
                this.mTmpTime = new Time(timeZone);
            }
            Time time = this.mTmpTime;
            long julianDay = time.setJulianDay(rowInfo.mDay);
            this.mStringBuilder.setLength(0);
            String str = Utils.getDayOfWeekString(rowInfo.mDay, this.mTodayJulianDay, julianDay, this.mContext) + OpCalendarCustomizationFactoryBase.getOpFactory(this.mContext).makeLunarCalendar(this.mContext).buildLunarDate(time, null, -1L);
            this.mStringBuilder.setLength(0);
            String string = DateUtils.formatDateRange(this.mContext, this.mFormatter, julianDay, julianDay, 16, this.mTimeZone).toString();
            viewHolder.dayView.setText(str);
            viewHolder.dateView.setText(string);
            if (rowInfo.mDay > this.mTodayJulianDay) {
                Inflate.setBackgroundResource(R.drawable.agenda_item_bg_primary);
                viewHolder.grayed = false;
            } else {
                Inflate.setBackgroundResource(R.drawable.agenda_item_bg_secondary);
                viewHolder.grayed = true;
            }
            return Inflate;
        }
        if (rowInfo.mType == 1) {
            View view3 = this.mAgendaAdapter.getView(rowInfo.mPosition, view2, viewGroup);
            AgendaAdapter.ViewHolder viewHolder2 = (AgendaAdapter.ViewHolder) view3.getTag();
            TextView textView = viewHolder2.title;
            viewHolder2.startTimeMilli = rowInfo.mEventStartTimeMilli;
            boolean z = viewHolder2.allDay;
            textView.setText(textView.getText());
            if ((!z && rowInfo.mEventStartTimeMilli <= System.currentTimeMillis()) || (z && rowInfo.mDay <= this.mTodayJulianDay)) {
                view3.setBackgroundResource(R.drawable.agenda_item_bg_secondary);
                textView.setTypeface(Typeface.DEFAULT);
                viewHolder2.grayed = true;
            } else {
                view3.setBackgroundResource(R.drawable.agenda_item_bg_primary);
                textView.setTypeface(Typeface.DEFAULT_BOLD);
                viewHolder2.grayed = false;
            }
            viewHolder2.julianDay = rowInfo.mDay;
            return view3;
        }
        throw new IllegalStateException("Unknown event type:" + rowInfo.mType);
    }

    public void changeCursor(AgendaWindowAdapter.DayAdapterInfo dayAdapterInfo) {
        calculateDays(dayAdapterInfo);
        this.mAgendaAdapter.changeCursor(dayAdapterInfo.cursor);
    }

    public void calculateDays(AgendaWindowAdapter.DayAdapterInfo dayAdapterInfo) {
        boolean z;
        Time time;
        LinkedList linkedList;
        long j;
        int i;
        Time time2;
        Time time3;
        LinkedList linkedList2;
        long j2;
        AgendaByDayAdapter agendaByDayAdapter = this;
        AgendaWindowAdapter.DayAdapterInfo dayAdapterInfo2 = dayAdapterInfo;
        Cursor cursor = dayAdapterInfo2.cursor;
        ArrayList<RowInfo> arrayList = new ArrayList<>();
        Time time4 = new Time(agendaByDayAdapter.mTimeZone);
        long jCurrentTimeMillis = System.currentTimeMillis();
        time4.set(jCurrentTimeMillis);
        agendaByDayAdapter.mTodayJulianDay = Time.getJulianDay(jCurrentTimeMillis, time4.gmtoff);
        LinkedList linkedList3 = new LinkedList();
        int i2 = 0;
        int i3 = 0;
        int i4 = -1;
        while (cursor.moveToNext()) {
            int i5 = cursor.getInt(10);
            long j3 = cursor.getLong(9);
            long jConvertAlldayUtcToLocal = cursor.getLong(7);
            long jConvertAlldayUtcToLocal2 = cursor.getLong(8);
            long j4 = cursor.getLong(i2);
            boolean z2 = cursor.getInt(3) != 0 ? 1 : i2;
            if (z2 != 0) {
                jConvertAlldayUtcToLocal = Utils.convertAlldayUtcToLocal(time4, jConvertAlldayUtcToLocal, agendaByDayAdapter.mTimeZone);
                jConvertAlldayUtcToLocal2 = Utils.convertAlldayUtcToLocal(time4, jConvertAlldayUtcToLocal2, agendaByDayAdapter.mTimeZone);
            }
            long j5 = jConvertAlldayUtcToLocal2;
            int iMax = Math.max(i5, dayAdapterInfo2.start);
            long jMax = Math.max(jConvertAlldayUtcToLocal, time4.setJulianDay(iMax));
            if (iMax != i4) {
                if (i4 == -1) {
                    arrayList.add(new RowInfo(0, iMax));
                    time = time4;
                    linkedList = linkedList3;
                    j = jMax;
                    i = iMax;
                } else {
                    int i6 = i4 + 1;
                    boolean z3 = false;
                    while (i6 <= iMax) {
                        Iterator it = linkedList3.iterator();
                        boolean z4 = false;
                        while (it.hasNext()) {
                            MultipleDayInfo multipleDayInfo = (MultipleDayInfo) it.next();
                            if (multipleDayInfo.mEndDay < i6) {
                                it.remove();
                            } else {
                                if (!z4) {
                                    arrayList.add(new RowInfo(0, i6));
                                    z4 = true;
                                }
                                boolean z5 = z4;
                                long nextMidnight = Utils.getNextMidnight(time4, multipleDayInfo.mEventStartTimeMilli, agendaByDayAdapter.mTimeZone);
                                if (multipleDayInfo.mEndDay == i6) {
                                    time3 = time4;
                                    linkedList2 = linkedList3;
                                    j2 = multipleDayInfo.mEventEndTimeMilli;
                                } else {
                                    time3 = time4;
                                    linkedList2 = linkedList3;
                                    j2 = nextMidnight;
                                }
                                arrayList.add(new RowInfo(1, i6, multipleDayInfo.mPosition, multipleDayInfo.mEventId, multipleDayInfo.mEventStartTimeMilli, j2, multipleDayInfo.mInstanceId, multipleDayInfo.mAllDay));
                                multipleDayInfo.mEventStartTimeMilli = nextMidnight;
                                z4 = z5;
                                time4 = time3;
                                linkedList3 = linkedList2;
                                jMax = jMax;
                                iMax = iMax;
                                agendaByDayAdapter = this;
                            }
                        }
                        i6++;
                        z3 = z4;
                        agendaByDayAdapter = this;
                    }
                    time = time4;
                    linkedList = linkedList3;
                    j = jMax;
                    int i7 = iMax;
                    if (!z3) {
                        i = i7;
                        arrayList.add(new RowInfo(0, i));
                    } else {
                        i = i7;
                    }
                }
                i4 = i;
            } else {
                time = time4;
                linkedList = linkedList3;
                j = jMax;
                i = iMax;
            }
            int iMin = Math.min(cursor.getInt(11), dayAdapterInfo.end);
            if (iMin > i) {
                agendaByDayAdapter = this;
                time2 = time;
                long j6 = j;
                long nextMidnight2 = Utils.getNextMidnight(time2, j6, agendaByDayAdapter.mTimeZone);
                LinkedList linkedList4 = linkedList;
                linkedList4.add(new MultipleDayInfo(i3, iMin, j3, nextMidnight2, j5, j4, z2));
                arrayList.add(new RowInfo(1, i, i3, j3, j6, nextMidnight2, j4, z2));
                linkedList3 = linkedList4;
            } else {
                time2 = time;
                linkedList3 = linkedList;
                agendaByDayAdapter = this;
                arrayList.add(new RowInfo(1, i, i3, j3, j, j5, j4, z2));
            }
            i3++;
            dayAdapterInfo2 = dayAdapterInfo;
            time4 = time2;
            i2 = 0;
        }
        Time time5 = time4;
        AgendaWindowAdapter.DayAdapterInfo dayAdapterInfo3 = dayAdapterInfo2;
        if (i4 > 0) {
            boolean z6 = true;
            int i8 = i4 + 1;
            while (i8 <= dayAdapterInfo3.end) {
                Iterator it2 = linkedList3.iterator();
                boolean z7 = false;
                while (it2.hasNext()) {
                    MultipleDayInfo multipleDayInfo2 = (MultipleDayInfo) it2.next();
                    if (multipleDayInfo2.mEndDay < i8) {
                        it2.remove();
                    } else {
                        if (z7) {
                            z = false;
                        } else {
                            z = false;
                            arrayList.add(new RowInfo(0, i8));
                            z7 = z6;
                        }
                        long nextMidnight3 = Utils.getNextMidnight(time5, multipleDayInfo2.mEventStartTimeMilli, agendaByDayAdapter.mTimeZone);
                        arrayList.add(new RowInfo(1, i8, multipleDayInfo2.mPosition, multipleDayInfo2.mEventId, multipleDayInfo2.mEventStartTimeMilli, multipleDayInfo2.mEndDay == i8 ? multipleDayInfo2.mEventEndTimeMilli : nextMidnight3, multipleDayInfo2.mInstanceId, multipleDayInfo2.mAllDay));
                        multipleDayInfo2.mEventStartTimeMilli = nextMidnight3;
                        it2 = it2;
                        linkedList3 = linkedList3;
                        z6 = true;
                    }
                }
                i8++;
                z6 = true;
                dayAdapterInfo3 = dayAdapterInfo;
            }
        }
        agendaByDayAdapter.mRowInfo = arrayList;
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

        RowInfo(int i, int i2, int i3, long j, long j2, long j3, long j4, boolean z) {
            this.mType = i;
            this.mDay = i2;
            this.mPosition = i3;
            this.mEventId = j;
            this.mEventStartTimeMilli = j2;
            this.mEventEndTimeMilli = j3;
            this.mFirstDayAfterYesterday = false;
            this.mInstanceId = j4;
            this.mAllDay = z;
        }

        RowInfo(int i, int i2) {
            this.mType = i;
            this.mDay = i2;
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

        MultipleDayInfo(int i, int i2, long j, long j2, long j3, long j4, boolean z) {
            this.mPosition = i;
            this.mEndDay = i2;
            this.mEventId = j;
            this.mEventStartTimeMilli = j2;
            this.mEventEndTimeMilli = j3;
            this.mInstanceId = j4;
            this.mAllDay = z;
        }
    }

    public int findEventPositionNearestTime(Time time, long j) {
        int i;
        AgendaByDayAdapter agendaByDayAdapter = this;
        int i2 = 0;
        if (agendaByDayAdapter.mRowInfo == null) {
            return 0;
        }
        long millis = time.toMillis(false);
        int size = agendaByDayAdapter.mRowInfo.size();
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        long j2 = 2147483647L;
        long j3 = 2147483647L;
        int i6 = -1;
        int i7 = -1;
        boolean z = false;
        int i8 = 0;
        while (i2 < size) {
            RowInfo rowInfo = agendaByDayAdapter.mRowInfo.get(i2);
            if (rowInfo.mType == 0) {
                i = size;
            } else {
                i = size;
                if (rowInfo.mEventId == j) {
                    if (rowInfo.mEventStartTimeMilli == millis) {
                        return i2;
                    }
                    long jAbs = Math.abs(millis - rowInfo.mEventStartTimeMilli);
                    if (jAbs < j2) {
                        j2 = jAbs;
                        i8 = i2;
                    }
                    z = true;
                }
                if (!z) {
                    if (millis < rowInfo.mEventStartTimeMilli || millis > rowInfo.mEventEndTimeMilli) {
                        if (i6 == -1) {
                            long jAbs2 = Math.abs(millis - rowInfo.mEventStartTimeMilli);
                            if (jAbs2 < j3) {
                                j3 = jAbs2;
                                i5 = i2;
                                i3 = rowInfo.mDay;
                            }
                        }
                    } else if (rowInfo.mAllDay) {
                        if (i7 == -1) {
                            i4 = rowInfo.mDay;
                            i7 = i2;
                        }
                    } else if (i6 == -1) {
                        i6 = i2;
                    }
                }
            }
            i2++;
            size = i;
            agendaByDayAdapter = this;
        }
        return z ? i8 : i6 != -1 ? i6 : (i7 == -1 || i3 == i4) ? i5 : i7;
    }

    public boolean isFirstDayAfterYesterday(int i) {
        RowInfo rowInfo = this.mRowInfo.get(getHeaderPosition(i));
        if (rowInfo != null) {
            return rowInfo.mFirstDayAfterYesterday;
        }
        return false;
    }

    public int findJulianDayFromPosition(int i) {
        if (this.mRowInfo == null || i < 0 || i >= this.mRowInfo.size()) {
            return 0;
        }
        while (i >= 0) {
            RowInfo rowInfo = this.mRowInfo.get(i);
            if (rowInfo.mType != 0) {
                i--;
            } else {
                return rowInfo.mDay;
            }
        }
        return 0;
    }

    public void setAsFirstDayAfterYesterday(int i) {
        if (this.mRowInfo == null || i < 0 || i > this.mRowInfo.size()) {
            return;
        }
        this.mRowInfo.get(i).mFirstDayAfterYesterday = true;
    }

    public int getCursorPosition(int i) {
        int cursorPosition;
        if (this.mRowInfo != null && i >= 0) {
            RowInfo rowInfo = this.mRowInfo.get(i);
            if (rowInfo.mType == 1) {
                return rowInfo.mPosition;
            }
            int i2 = i + 1;
            if (i2 < this.mRowInfo.size() && (cursorPosition = getCursorPosition(i2)) >= 0) {
                return -cursorPosition;
            }
            return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int i) {
        return this.mRowInfo == null || i >= this.mRowInfo.size() || this.mRowInfo.get(i).mType == 1;
    }
}
