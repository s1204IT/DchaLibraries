package com.android.calendar.agenda;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CalendarContract;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.calendar.CalendarController;
import com.android.calendar.R;
import com.android.calendar.StickyHeaderListView;
import com.android.calendar.Utils;
import com.android.calendar.agenda.AgendaAdapter;
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.extension.ExtensionFactory;
import com.mediatek.calendar.extension.ICalendarThemeExt;
import com.mediatek.calendar.selectevent.AgendaChoiceActivity;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AgendaWindowAdapter extends BaseAdapter implements StickyHeaderListView.HeaderHeightListener, StickyHeaderListView.HeaderIndexer {
    private static final String[] PROJECTION = {"_id", "title", "eventLocation", "allDay", "hasAlarm", "displayColor", "rrule", "begin", "end", "event_id", "startDay", "endDay", "selfAttendeeStatus", "organizer", "ownerAccount", "canOrganizerRespond", "eventTimezone"};
    private static final Object SLOCK;
    private static int[] sTopDeviationInfo;
    private final AgendaListView mAgendaListView;
    private final LinearLayout mBlankView;
    private final Context mContext;
    private int mEmptyCursorCount;
    private final TextView mFooterView;
    private final TextView mHeaderView;
    private boolean mHideDeclined;
    private final boolean mIsTabletConfig;
    private final float mItemRightMargin;
    private DayAdapterInfo mLastUsedInfo;
    private int mNewerRequests;
    private int mNewerRequestsProcessed;
    private int mOlderRequests;
    private int mOlderRequestsProcessed;
    private final QueryHandler mQueryHandler;
    private final Resources mResources;
    private int mRowCount;
    private String mSearchQuery;
    private final int mSelectedItemBackgroundColor;
    private final int mSelectedItemTextColor;
    private final boolean mShowEventOnStart;
    private boolean mShuttingDown;
    private int mStickyHeaderSize;
    private String mTimeZone;
    private final LinkedList<DayAdapterInfo> mAdapterInfos = new LinkedList<>();
    private final ConcurrentLinkedQueue<QuerySpec> mQueryQueue = new ConcurrentLinkedQueue<>();
    private boolean mDoneSettingUpHeaderFooter = false;
    private boolean mHasBlankView = false;
    boolean mCleanQueryInitiated = false;
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            AgendaWindowAdapter.this.mTimeZone = Utils.getTimeZone(AgendaWindowAdapter.this.mContext, this);
            AgendaWindowAdapter.this.notifyDataSetChanged();
        }
    };
    private final Handler mDataChangedHandler = new Handler();
    private final Runnable mDataChangedRunnable = new Runnable() {
        @Override
        public void run() {
            AgendaWindowAdapter.this.notifyDataSetChanged();
        }
    };
    int mListViewScrollState = 0;
    private long mSelectedInstanceId = -1;
    private AgendaAdapter.ViewHolder mSelectedVH = null;
    View.OnTouchListener headerFooterOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int action = motionEvent.getAction();
            if (action != 3) {
                switch (action) {
                    case 0:
                        ICalendarThemeExt calendarTheme = ExtensionFactory.getCalendarTheme(AgendaWindowAdapter.this.mContext);
                        if (!calendarTheme.isThemeManagerEnable()) {
                            view.setBackgroundColor(AgendaWindowAdapter.this.mResources.getColor(R.color.pressed));
                        } else {
                            view.setBackgroundColor(calendarTheme.getThemeColor());
                        }
                        break;
                    case 1:
                        view.setBackgroundColor(0);
                        break;
                }
            }
            return false;
        }
    };
    private final StringBuilder mStringBuilder = new StringBuilder(50);
    private final Formatter mFormatter = new Formatter(this.mStringBuilder, Locale.getDefault());

    static int access$2504(AgendaWindowAdapter agendaWindowAdapter) {
        int i = agendaWindowAdapter.mEmptyCursorCount + 1;
        agendaWindowAdapter.mEmptyCursorCount = i;
        return i;
    }

    static int access$2608(AgendaWindowAdapter agendaWindowAdapter) {
        int i = agendaWindowAdapter.mNewerRequestsProcessed;
        agendaWindowAdapter.mNewerRequestsProcessed = i + 1;
        return i;
    }

    static int access$2708(AgendaWindowAdapter agendaWindowAdapter) {
        int i = agendaWindowAdapter.mOlderRequestsProcessed;
        agendaWindowAdapter.mOlderRequestsProcessed = i + 1;
        return i;
    }

    static int access$3212(AgendaWindowAdapter agendaWindowAdapter, int i) {
        int i2 = agendaWindowAdapter.mRowCount + i;
        agendaWindowAdapter.mRowCount = i2;
        return i2;
    }

    static {
        if (!Utils.isJellybeanOrLater()) {
            PROJECTION[5] = "calendar_color";
        }
        sTopDeviationInfo = new int[2];
        SLOCK = new Object();
    }

    public static void setTopDeviation(int[] iArr) {
        if (iArr == null || iArr.length != 2) {
            return;
        }
        sTopDeviationInfo[0] = iArr[0];
        sTopDeviationInfo[1] = iArr[1];
    }

    public int[] saveTopDeviation(Time time, long j) {
        int top;
        int firstVisiblePosition = this.mAgendaListView.getFirstVisiblePosition();
        View childAt = this.mAgendaListView.getChildAt(0);
        if (childAt != null) {
            top = childAt.getTop();
        } else {
            top = 0;
        }
        int iFindEventPositionNearestTime = findEventPositionNearestTime(time, j) + 1;
        int i = firstVisiblePosition - iFindEventPositionNearestTime;
        Log.i("AgendaWindowAdapter", "time=" + time + ", eventId=" + j + ", firstVisiblePosition=" + firstVisiblePosition + ", firstItemOnSameTime=" + iFindEventPositionNearestTime + ", itemNum=" + i + ", topDeviation=" + top);
        sTopDeviationInfo[0] = i;
        sTopDeviationInfo[1] = top;
        return sTopDeviationInfo;
    }

    private static class QuerySpec {
        int end;
        Time goToTime;
        long id = -1;
        long queryStartMillis;
        int queryType;
        String searchQuery;
        int start;

        public QuerySpec(int i) {
            this.queryType = i;
        }

        public int hashCode() {
            int iHashCode = ((((((this.end + 31) * 31) + ((int) (this.queryStartMillis ^ (this.queryStartMillis >>> 32)))) * 31) + this.queryType) * 31) + this.start;
            if (this.searchQuery != null) {
                iHashCode = (iHashCode * 31) + this.searchQuery.hashCode();
            }
            if (this.goToTime != null) {
                long millis = this.goToTime.toMillis(false);
                iHashCode = (iHashCode * 31) + ((int) (millis ^ (millis >>> 32)));
            }
            return (31 * iHashCode) + ((int) this.id);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            QuerySpec querySpec = (QuerySpec) obj;
            if (this.end != querySpec.end || this.queryStartMillis != querySpec.queryStartMillis || this.queryType != querySpec.queryType || this.start != querySpec.start || Utils.equals(this.searchQuery, querySpec.searchQuery) || this.id != querySpec.id) {
                return false;
            }
            if (this.goToTime != null) {
                if (this.goToTime.toMillis(false) != querySpec.goToTime.toMillis(false)) {
                    return false;
                }
            } else if (querySpec.goToTime != null) {
                return false;
            }
            return true;
        }
    }

    static class AgendaItem {
        boolean allDay;
        long begin;
        long end;
        long id;
        int startDay;

        AgendaItem() {
        }
    }

    static class DayAdapterInfo {
        Cursor cursor;
        AgendaByDayAdapter dayAdapter;
        int end;
        int offset;
        int size;
        int start;

        public DayAdapterInfo(Context context) {
            this.dayAdapter = new AgendaByDayAdapter(context);
        }

        public String toString() {
            Time time = new Time();
            StringBuilder sb = new StringBuilder();
            time.setJulianDay(this.start);
            time.normalize(false);
            sb.append("Start:");
            sb.append(time.toString());
            time.setJulianDay(this.end);
            time.normalize(false);
            sb.append(" End:");
            sb.append(time.toString());
            sb.append(" Offset:");
            sb.append(this.offset);
            sb.append(" Size:");
            sb.append(this.size);
            return sb.toString();
        }
    }

    public AgendaWindowAdapter(Context context, AgendaListView agendaListView, boolean z) {
        this.mStickyHeaderSize = 44;
        this.mContext = context;
        this.mResources = context.getResources();
        this.mSelectedItemBackgroundColor = this.mResources.getColor(R.color.agenda_selected_background_color);
        this.mSelectedItemTextColor = this.mResources.getColor(R.color.agenda_selected_text_color);
        this.mItemRightMargin = this.mResources.getDimension(R.dimen.agenda_item_right_margin);
        this.mIsTabletConfig = Utils.getConfigBool(this.mContext, R.bool.tablet_config);
        this.mTimeZone = Utils.getTimeZone(context, this.mTZUpdater);
        this.mAgendaListView = agendaListView;
        this.mQueryHandler = new QueryHandler(context.getContentResolver());
        this.mShowEventOnStart = z;
        if (!this.mShowEventOnStart) {
            this.mStickyHeaderSize = 0;
        }
        this.mSearchQuery = null;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mHeaderView = (TextView) layoutInflater.inflate(R.layout.agenda_header_footer, (ViewGroup) null);
        this.mFooterView = (TextView) layoutInflater.inflate(R.layout.agenda_header_footer, (ViewGroup) null);
        this.mHeaderView.setText(R.string.loading);
        this.mBlankView = (LinearLayout) layoutInflater.inflate(R.layout.agenda_blank, (ViewGroup) null);
        ((TextView) this.mBlankView.findViewById(R.id.blank_text)).setText(this.mContext.getString(R.string.no_event_tip));
        this.mAgendaListView.addHeaderView(this.mHeaderView);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public int getItemViewType(int i) {
        DayAdapterInfo adapterInfoByPosition = getAdapterInfoByPosition(i);
        if (adapterInfoByPosition != null) {
            return adapterInfoByPosition.dayAdapter.getItemViewType(i - adapterInfoByPosition.offset);
        }
        return -1;
    }

    @Override
    public boolean isEnabled(int i) {
        DayAdapterInfo adapterInfoByPosition = getAdapterInfoByPosition(i);
        if (adapterInfoByPosition != null) {
            return adapterInfoByPosition.dayAdapter.isEnabled(i - adapterInfoByPosition.offset);
        }
        return false;
    }

    @Override
    public int getCount() {
        return this.mRowCount;
    }

    @Override
    public Object getItem(int i) {
        DayAdapterInfo adapterInfoByPosition = getAdapterInfoByPosition(i);
        if (adapterInfoByPosition != null) {
            return adapterInfoByPosition.dayAdapter.getItem(i - adapterInfoByPosition.offset);
        }
        return null;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int i) {
        int cursorPosition;
        DayAdapterInfo adapterInfoByPosition = getAdapterInfoByPosition(i);
        if (adapterInfoByPosition == null || (cursorPosition = adapterInfoByPosition.dayAdapter.getCursorPosition(i - adapterInfoByPosition.offset)) == Integer.MIN_VALUE) {
            return -1L;
        }
        if (cursorPosition >= 0) {
            adapterInfoByPosition.cursor.moveToPosition(cursorPosition);
            return adapterInfoByPosition.cursor.getLong(9) << ((int) (20 + adapterInfoByPosition.cursor.getLong(7)));
        }
        return adapterInfoByPosition.dayAdapter.findJulianDayFromPosition(i);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View view2;
        if (i >= this.mRowCount - 1 && this.mNewerRequests <= this.mNewerRequestsProcessed) {
            this.mNewerRequests++;
            queueQuery(new QuerySpec(1));
        }
        if (i < 1 && this.mOlderRequests <= this.mOlderRequestsProcessed) {
            this.mOlderRequests++;
            queueQuery(new QuerySpec(0));
        }
        DayAdapterInfo adapterInfoByPosition = getAdapterInfoByPosition(i);
        int i2 = 8;
        if (adapterInfoByPosition != null) {
            int i3 = i - adapterInfoByPosition.offset;
            View view3 = adapterInfoByPosition.dayAdapter.getView(i3, view, viewGroup);
            view2 = view3;
            if (adapterInfoByPosition.dayAdapter.isDayHeaderView(i3)) {
                View viewFindViewById = view3.findViewById(R.id.top_divider_simple);
                View viewFindViewById2 = view3.findViewById(R.id.top_divider_past_present);
                view2 = view3;
                view2 = view3;
                view2 = view3;
                view2 = view3;
                if (adapterInfoByPosition.dayAdapter.isFirstDayAfterYesterday(i3)) {
                    if (viewFindViewById != null && viewFindViewById2 != null) {
                        viewFindViewById.setVisibility(8);
                        viewFindViewById2.setVisibility(0);
                        view2 = view3;
                    }
                } else if (viewFindViewById != null && viewFindViewById2 != null) {
                    viewFindViewById.setVisibility(0);
                    viewFindViewById2.setVisibility(8);
                    view2 = view3;
                }
            }
        } else {
            Log.e("AgendaWindowAdapter", "BUG: getAdapterInfoByPosition returned null!!! " + i);
            TextView textView = new TextView(this.mContext);
            textView.setText("Bug! " + i);
            view2 = textView;
        }
        if (!this.mIsTabletConfig) {
            return view2;
        }
        ?? tag = view2.getTag();
        if (tag instanceof AgendaAdapter.ViewHolder) {
            boolean z = this.mSelectedInstanceId == tag.instanceId;
            View view4 = tag.selectedMarker;
            if (z && this.mShowEventOnStart) {
                i2 = 0;
            }
            view4.setVisibility(i2);
            if (this.mShowEventOnStart) {
                GridLayout.LayoutParams layoutParams = (GridLayout.LayoutParams) tag.textContainer.getLayoutParams();
                if (!z) {
                    layoutParams.setMargins(0, 0, (int) this.mItemRightMargin, 0);
                    tag.textContainer.setLayoutParams(layoutParams);
                } else {
                    this.mSelectedVH = tag;
                    view2.setBackgroundColor(this.mSelectedItemBackgroundColor);
                    tag.title.setTextColor(this.mSelectedItemTextColor);
                    tag.when.setTextColor(this.mSelectedItemTextColor);
                    tag.where.setTextColor(this.mSelectedItemTextColor);
                    layoutParams.setMargins(0, 0, 0, 0);
                    tag.textContainer.setLayoutParams(layoutParams);
                }
            }
        }
        return view2;
    }

    private int findEventPositionNearestTime(Time time, long j) {
        DayAdapterInfo adapterInfoByTime = getAdapterInfoByTime(time);
        if (adapterInfoByTime != null) {
            return adapterInfoByTime.offset + adapterInfoByTime.dayAdapter.findEventPositionNearestTime(time, j);
        }
        return -1;
    }

    protected DayAdapterInfo getAdapterInfoByPosition(int i) {
        synchronized (this.mAdapterInfos) {
            if (this.mLastUsedInfo != null && this.mLastUsedInfo.offset <= i && i < this.mLastUsedInfo.offset + this.mLastUsedInfo.size) {
                return this.mLastUsedInfo;
            }
            for (DayAdapterInfo dayAdapterInfo : this.mAdapterInfos) {
                if (dayAdapterInfo.offset <= i && i < dayAdapterInfo.offset + dayAdapterInfo.size) {
                    this.mLastUsedInfo = dayAdapterInfo;
                    return dayAdapterInfo;
                }
            }
            return null;
        }
    }

    private DayAdapterInfo getAdapterInfoByTime(Time time) {
        Time time2 = new Time(time);
        int julianDay = Time.getJulianDay(time2.normalize(true), time2.gmtoff);
        synchronized (this.mAdapterInfos) {
            for (DayAdapterInfo dayAdapterInfo : this.mAdapterInfos) {
                if (dayAdapterInfo.start <= julianDay && julianDay <= dayAdapterInfo.end) {
                    return dayAdapterInfo;
                }
            }
            return null;
        }
    }

    public AgendaItem getAgendaItemByPosition(int i) {
        return getAgendaItemByPosition(i, true);
    }

    public AgendaItem getAgendaItemByPosition(int i, boolean z) {
        int cursorPosition;
        if (i < 0) {
            return null;
        }
        boolean z2 = true;
        int i2 = i - 1;
        DayAdapterInfo adapterInfoByPosition = getAdapterInfoByPosition(i2);
        if (adapterInfoByPosition == null || (cursorPosition = adapterInfoByPosition.dayAdapter.getCursorPosition(i2 - adapterInfoByPosition.offset)) == Integer.MIN_VALUE) {
            return null;
        }
        if (cursorPosition < 0) {
            cursorPosition = -cursorPosition;
        } else {
            z2 = false;
        }
        if (cursorPosition >= adapterInfoByPosition.cursor.getCount()) {
            return null;
        }
        AgendaItem agendaItemBuildAgendaItemFromCursor = buildAgendaItemFromCursor(adapterInfoByPosition.cursor, cursorPosition, z2);
        if (!z && !z2) {
            agendaItemBuildAgendaItemFromCursor.startDay = adapterInfoByPosition.dayAdapter.findJulianDayFromPosition(i2 - adapterInfoByPosition.offset);
        }
        return agendaItemBuildAgendaItemFromCursor;
    }

    private AgendaItem buildAgendaItemFromCursor(Cursor cursor, int i, boolean z) {
        if (i == -1) {
            cursor.moveToFirst();
        } else {
            cursor.moveToPosition(i);
        }
        AgendaItem agendaItem = new AgendaItem();
        agendaItem.begin = cursor.getLong(7);
        agendaItem.end = cursor.getLong(8);
        agendaItem.startDay = cursor.getInt(10);
        agendaItem.allDay = cursor.getInt(3) != 0;
        if (agendaItem.allDay) {
            Time time = new Time(this.mTimeZone);
            time.setJulianDay(Time.getJulianDay(agendaItem.begin, 0L));
            agendaItem.begin = time.toMillis(false);
        } else if (z) {
            Time time2 = new Time(this.mTimeZone);
            time2.set(agendaItem.begin);
            time2.hour = 0;
            time2.minute = 0;
            time2.second = 0;
            agendaItem.begin = time2.toMillis(false);
        }
        if (!z) {
            agendaItem.id = cursor.getLong(9);
            if (agendaItem.allDay) {
                Time time3 = new Time(this.mTimeZone);
                time3.setJulianDay(Time.getJulianDay(agendaItem.end, 0L));
                agendaItem.end = time3.toMillis(false);
            }
        }
        return agendaItem;
    }

    private void sendViewEvent(AgendaItem agendaItem, long j) {
        long jConvertAlldayLocalToUTC;
        long jConvertAlldayLocalToUTC2;
        if (agendaItem.allDay) {
            jConvertAlldayLocalToUTC = Utils.convertAlldayLocalToUTC(null, agendaItem.begin, this.mTimeZone);
            jConvertAlldayLocalToUTC2 = Utils.convertAlldayLocalToUTC(null, agendaItem.end, this.mTimeZone);
        } else {
            jConvertAlldayLocalToUTC = agendaItem.begin;
            jConvertAlldayLocalToUTC2 = agendaItem.end;
        }
        CalendarController.getInstance(this.mContext).sendEventRelatedEventWithExtra(this, 2L, agendaItem.id, jConvertAlldayLocalToUTC, jConvertAlldayLocalToUTC2, 0, 0, CalendarController.EventInfo.buildViewExtraLong(0, agendaItem.allDay), j);
    }

    public void refresh(Time time, long j, String str, boolean z, boolean z2) {
        if (str != null) {
            this.mSearchQuery = str;
        }
        int julianDay = Time.getJulianDay(time.toMillis(false), time.gmtoff);
        if (z || !isInRange(julianDay, julianDay)) {
            if (!this.mCleanQueryInitiated || str != null) {
                this.mSelectedInstanceId = -1L;
                this.mCleanQueryInitiated = true;
                queueQuery(julianDay, julianDay + 7, time, str, 2, j);
                this.mOlderRequests++;
                queueQuery(0, 0, time, str, 0, j);
                this.mNewerRequests++;
                queueQuery(0, 0, time, str, 1, j);
                return;
            }
            return;
        }
        if (!this.mAgendaListView.isAgendaItemVisible(time, j)) {
            int iFindEventPositionNearestTime = findEventPositionNearestTime(time, j);
            if (iFindEventPositionNearestTime > 0) {
                this.mAgendaListView.setSelectionFromTop(iFindEventPositionNearestTime + 1, this.mStickyHeaderSize);
                if (this.mListViewScrollState == 2) {
                    this.mAgendaListView.smoothScrollBy(0, 0);
                }
                if (z2) {
                    long jFindInstanceIdFromPosition = findInstanceIdFromPosition(iFindEventPositionNearestTime);
                    if (jFindInstanceIdFromPosition != getSelectedInstanceId()) {
                        setSelectedInstanceId(jFindInstanceIdFromPosition);
                        this.mDataChangedHandler.post(this.mDataChangedRunnable);
                        Cursor cursorByPosition = getCursorByPosition(iFindEventPositionNearestTime);
                        if (cursorByPosition != null) {
                            AgendaItem agendaItemBuildAgendaItemFromCursor = buildAgendaItemFromCursor(cursorByPosition, getCursorPositionByPosition(iFindEventPositionNearestTime), false);
                            this.mSelectedVH = new AgendaAdapter.ViewHolder();
                            this.mSelectedVH.allDay = agendaItemBuildAgendaItemFromCursor.allDay;
                            sendViewEvent(agendaItemBuildAgendaItemFromCursor, time.toMillis(false));
                        }
                    }
                }
            }
            Time time2 = new Time(this.mTimeZone);
            time2.set(time);
            CalendarController.getInstance(this.mContext).sendEvent(this, 1024L, time2, time2, -1L, 0);
        }
    }

    public void close() {
        this.mShuttingDown = true;
        pruneAdapterInfo(2);
        if (this.mQueryHandler != null) {
            this.mQueryHandler.cancelOperation(0);
        }
    }

    private DayAdapterInfo pruneAdapterInfo(int i) {
        DayAdapterInfo dayAdapterInfoRemoveLast;
        DayAdapterInfo dayAdapterInfoPoll;
        synchronized (this.mAdapterInfos) {
            if (!this.mAdapterInfos.isEmpty()) {
                int i2 = 0;
                if (this.mAdapterInfos.size() >= 5) {
                    if (i == 1) {
                        dayAdapterInfoRemoveLast = this.mAdapterInfos.removeFirst();
                    } else if (i == 0) {
                        dayAdapterInfoRemoveLast = this.mAdapterInfos.removeLast();
                        dayAdapterInfoRemoveLast.size = 0;
                    } else {
                        dayAdapterInfoRemoveLast = null;
                    }
                    if (dayAdapterInfoRemoveLast != null) {
                        if (dayAdapterInfoRemoveLast.cursor != null) {
                            dayAdapterInfoRemoveLast.cursor.close();
                        }
                        return dayAdapterInfoRemoveLast;
                    }
                } else {
                    dayAdapterInfoRemoveLast = null;
                }
                if (this.mRowCount == 0 || i == 2) {
                    this.mRowCount = 0;
                    do {
                        dayAdapterInfoPoll = this.mAdapterInfos.poll();
                        if (dayAdapterInfoPoll != null) {
                            dayAdapterInfoPoll.cursor.close();
                            i2 += dayAdapterInfoPoll.size;
                            dayAdapterInfoRemoveLast = dayAdapterInfoPoll;
                        }
                    } while (dayAdapterInfoPoll != null);
                    if (dayAdapterInfoRemoveLast != null) {
                        dayAdapterInfoRemoveLast.cursor = null;
                        dayAdapterInfoRemoveLast.size = i2;
                    }
                }
            } else {
                dayAdapterInfoRemoveLast = null;
            }
            return dayAdapterInfoRemoveLast;
        }
    }

    private String buildQuerySelection() {
        if (this.mHideDeclined) {
            return "visible=1 AND selfAttendeeStatus!=2";
        }
        return "visible=1";
    }

    private Uri buildQueryUri(int i, int i2, String str) {
        Uri uri;
        if (str == null) {
            uri = CalendarContract.Instances.CONTENT_BY_DAY_URI;
        } else {
            uri = CalendarContract.Instances.CONTENT_SEARCH_BY_DAY_URI;
        }
        Uri.Builder builderBuildUpon = uri.buildUpon();
        ContentUris.appendId(builderBuildUpon, i);
        ContentUris.appendId(builderBuildUpon, i2);
        if (str != null) {
            builderBuildUpon.appendPath(str);
        }
        return builderBuildUpon.build();
    }

    private boolean isInRange(int i, int i2) {
        synchronized (this.mAdapterInfos) {
            boolean z = false;
            if (this.mAdapterInfos.isEmpty()) {
                return false;
            }
            if (this.mAdapterInfos.getFirst().start <= i && i2 <= this.mAdapterInfos.getLast().end) {
                z = true;
            }
            return z;
        }
    }

    private int calculateQueryDuration(int i, int i2) {
        int i3;
        if (this.mRowCount != 0) {
            i3 = (50 * ((i2 - i) + 1)) / this.mRowCount;
        } else {
            i3 = 60;
        }
        if (i3 > 60) {
            return 60;
        }
        if (i3 < 7) {
            return 7;
        }
        return i3;
    }

    private boolean queueQuery(int i, int i2, Time time, String str, int i3, long j) {
        QuerySpec querySpec = new QuerySpec(i3);
        querySpec.goToTime = new Time(time);
        querySpec.start = i;
        querySpec.end = i2;
        querySpec.searchQuery = str;
        querySpec.id = j;
        return queueQuery(querySpec);
    }

    private boolean queueQuery(QuerySpec querySpec) {
        Boolean bool;
        if (!checkQueryRange(querySpec)) {
            return false;
        }
        querySpec.searchQuery = this.mSearchQuery;
        synchronized (SLOCK) {
            Boolean.valueOf(false);
            Boolean boolValueOf = Boolean.valueOf(this.mQueryQueue.isEmpty());
            this.mQueryQueue.add(querySpec);
            bool = true;
            if (boolValueOf.booleanValue()) {
                doQuery(querySpec);
            }
        }
        return bool.booleanValue();
    }

    private void doQuery(QuerySpec querySpec) {
        if (!this.mAdapterInfos.isEmpty()) {
            int i = this.mAdapterInfos.getFirst().start;
            int i2 = this.mAdapterInfos.getLast().end;
            int iCalculateQueryDuration = calculateQueryDuration(i, i2);
            switch (querySpec.queryType) {
                case 0:
                    querySpec.end = i - 1;
                    querySpec.start = querySpec.end - iCalculateQueryDuration;
                    break;
                case 1:
                    querySpec.start = i2 + 1;
                    querySpec.end = querySpec.start + iCalculateQueryDuration;
                    break;
            }
            if (this.mRowCount < 20 && querySpec.queryType != 2) {
                querySpec.queryType = 2;
                if (querySpec.start > i) {
                    querySpec.start = i;
                }
                if (querySpec.end < i2) {
                    querySpec.end = i2;
                }
            }
        }
        adjustQueryRange(querySpec);
        this.mQueryHandler.cancelOperation(0);
        this.mQueryHandler.startQuery(0, querySpec, buildQueryUri(querySpec.start, querySpec.end, querySpec.searchQuery), PROJECTION, buildQuerySelection(), null, "startDay ASC, begin ASC, title ASC");
    }

    private String formatDateString(int i) {
        Time time = new Time(this.mTimeZone);
        time.setJulianDay(i);
        long millis = time.toMillis(false);
        this.mStringBuilder.setLength(0);
        return DateUtils.formatDateRange(this.mContext, this.mFormatter, millis, millis, 65556, this.mTimeZone).toString();
    }

    private void updateHeaderFooter(int i, int i2) {
        this.mHeaderView.setText(this.mContext.getString(R.string.show_older_events, formatDateString(i)));
        this.mFooterView.setText(this.mContext.getString(R.string.show_newer_events, formatDateString(i2)));
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int i, Object obj, Cursor cursor) {
            int i2;
            int cursorPositionByPosition;
            boolean z;
            int i3;
            int i4;
            int i5;
            int i6;
            if (cursor == null) {
                LogUtil.e("AgendaWindowAdapter", "onQueryComplete, cursor is null.");
                if (AgendaWindowAdapter.this.mHeaderView != null) {
                    AgendaWindowAdapter.this.mHeaderView.setText(R.string.loading_failed);
                    return;
                }
                return;
            }
            QuerySpec querySpec = (QuerySpec) obj;
            if (querySpec.queryType == 2) {
                AgendaWindowAdapter.this.mCleanQueryInitiated = false;
            }
            if (AgendaWindowAdapter.this.mShuttingDown) {
                cursor.close();
                return;
            }
            int count = cursor.getCount();
            int count2 = AgendaWindowAdapter.this.mAgendaListView.getCount();
            if (count == 0 && count2 < 3 && !AgendaWindowAdapter.this.mHasBlankView) {
                AgendaWindowAdapter.this.mAgendaListView.addHeaderView(AgendaWindowAdapter.this.mBlankView);
                AgendaWindowAdapter.this.mHasBlankView = true;
            } else if ((count > 0 || count2 > 3) && AgendaWindowAdapter.this.mHasBlankView) {
                AgendaWindowAdapter.this.mAgendaListView.removeHeaderView(AgendaWindowAdapter.this.mBlankView);
                AgendaWindowAdapter.this.mHasBlankView = false;
            }
            if (!AgendaWindowAdapter.this.mDoneSettingUpHeaderFooter) {
                View.OnClickListener onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (view == AgendaWindowAdapter.this.mHeaderView) {
                            AgendaWindowAdapter.this.queueQuery(new QuerySpec(0));
                        } else {
                            AgendaWindowAdapter.this.queueQuery(new QuerySpec(1));
                        }
                    }
                };
                AgendaWindowAdapter.this.mHeaderView.setOnClickListener(onClickListener);
                AgendaWindowAdapter.this.mFooterView.setOnClickListener(onClickListener);
                AgendaWindowAdapter.this.mFooterView.setOnTouchListener(AgendaWindowAdapter.this.headerFooterOnTouchListener);
                AgendaWindowAdapter.this.mHeaderView.setOnTouchListener(AgendaWindowAdapter.this.headerFooterOnTouchListener);
                AgendaWindowAdapter.this.mAgendaListView.addFooterView(AgendaWindowAdapter.this.mFooterView);
                AgendaWindowAdapter.this.mDoneSettingUpHeaderFooter = true;
            }
            int count3 = cursor.getCount();
            if (count3 > 0 || AgendaWindowAdapter.this.mAdapterInfos.isEmpty() || querySpec.queryType == 2) {
                int iProcessNewCursor = processNewCursor(querySpec, cursor);
                int i7 = -1;
                if (querySpec.goToTime == null) {
                    AgendaWindowAdapter.this.notifyDataSetChanged();
                    if (iProcessNewCursor != 0) {
                        AgendaWindowAdapter.this.mAgendaListView.shiftSelection(iProcessNewCursor);
                    }
                    cursorPositionByPosition = -1;
                } else {
                    Time time = querySpec.goToTime;
                    AgendaWindowAdapter.this.notifyDataSetChanged();
                    int iFindEventPositionNearestTime = AgendaWindowAdapter.this.findEventPositionNearestTime(time, querySpec.id);
                    if (iFindEventPositionNearestTime >= 0) {
                        if (AgendaWindowAdapter.this.mListViewScrollState == 2) {
                            AgendaWindowAdapter.this.mAgendaListView.smoothScrollBy(0, 0);
                        }
                        AgendaWindowAdapter.this.mAgendaListView.setSelectionFromTop(iFindEventPositionNearestTime + 1 + AgendaWindowAdapter.sTopDeviationInfo[0], AgendaWindowAdapter.sTopDeviationInfo[1]);
                        Log.d("AgendaWindowAdapter", "real postion: " + (AgendaWindowAdapter.sTopDeviationInfo[0] + iFindEventPositionNearestTime + 1) + ", top deviation: " + AgendaWindowAdapter.sTopDeviationInfo[1] + ", newPosition=" + iFindEventPositionNearestTime + ", goToTime=" + time + ", data.id=" + querySpec.id);
                        Time time2 = new Time(AgendaWindowAdapter.this.mTimeZone);
                        time2.set(time);
                        i2 = iFindEventPositionNearestTime;
                        cursorPositionByPosition = -1;
                        CalendarController.getInstance(AgendaWindowAdapter.this.mContext).sendEvent(this, 1024L, time2, time2, -1L, 0);
                    } else {
                        i2 = iFindEventPositionNearestTime;
                        cursorPositionByPosition = -1;
                    }
                    i7 = i2;
                }
                if (AgendaWindowAdapter.this.mSelectedInstanceId == -1 && i7 != cursorPositionByPosition && querySpec.queryType == 2 && (querySpec.id != -1 || querySpec.goToTime != null)) {
                    AgendaWindowAdapter.this.mSelectedInstanceId = AgendaWindowAdapter.this.findInstanceIdFromPosition(i7);
                }
                if (AgendaWindowAdapter.this.mAdapterInfos.size() == 1 && AgendaWindowAdapter.this.mSelectedInstanceId != -1) {
                    cursor.moveToPosition(cursorPositionByPosition);
                    while (true) {
                        if (!cursor.moveToNext()) {
                            z = false;
                            break;
                        } else if (AgendaWindowAdapter.this.mSelectedInstanceId == cursor.getLong(0)) {
                            z = true;
                            break;
                        }
                    }
                    if (!z) {
                        AgendaWindowAdapter.this.mSelectedInstanceId = -1L;
                    }
                }
                if (AgendaWindowAdapter.this.mShowEventOnStart && querySpec.queryType == 2) {
                    Cursor cursorByPosition = null;
                    if (AgendaWindowAdapter.this.mSelectedInstanceId == -1) {
                        if (cursor.moveToFirst()) {
                            AgendaWindowAdapter.this.mSelectedInstanceId = cursor.getLong(0);
                            AgendaWindowAdapter.this.mSelectedVH = new AgendaAdapter.ViewHolder();
                            AgendaWindowAdapter.this.mSelectedVH.allDay = cursor.getInt(3) != 0;
                            cursorByPosition = cursor;
                        }
                    } else if (i7 != cursorPositionByPosition) {
                        cursorByPosition = AgendaWindowAdapter.this.getCursorByPosition(i7);
                        cursorPositionByPosition = AgendaWindowAdapter.this.getCursorPositionByPosition(i7);
                    }
                    if (cursorByPosition != null) {
                        AgendaItem agendaItemBuildAgendaItemFromCursor = AgendaWindowAdapter.this.buildAgendaItemFromCursor(cursorByPosition, cursorPositionByPosition, false);
                        long jFindStartTimeFromPosition = AgendaWindowAdapter.this.findStartTimeFromPosition(i7);
                        if (jFindStartTimeFromPosition == 0 && AgendaWindowAdapter.this.mIsTabletConfig) {
                            jFindStartTimeFromPosition = System.currentTimeMillis();
                        }
                        AgendaWindowAdapter.this.sendViewEvent(agendaItemBuildAgendaItemFromCursor, jFindStartTimeFromPosition);
                    }
                }
                boolean configBool = Utils.getConfigBool(AgendaWindowAdapter.this.mContext, R.bool.show_event_details_with_agenda);
                if (AgendaWindowAdapter.this.mIsTabletConfig && configBool && count3 == 0 && !(AgendaWindowAdapter.this.mContext instanceof AgendaChoiceActivity)) {
                    Time time3 = new Time(AgendaWindowAdapter.this.mTimeZone);
                    time3.setToNow();
                    AgendaItem agendaItem = new AgendaItem();
                    agendaItem.id = -1L;
                    agendaItem.begin = time3.toMillis(true);
                    agendaItem.end = time3.toMillis(true);
                    agendaItem.startDay = Time.getJulianDay(time3.toMillis(true), time3.gmtoff);
                    agendaItem.allDay = false;
                    AgendaWindowAdapter.this.sendViewEvent(agendaItem, time3.toMillis(true));
                }
            } else {
                cursor.close();
            }
            synchronized (AgendaWindowAdapter.SLOCK) {
                try {
                    if (count3 != 0) {
                        AgendaWindowAdapter.this.mEmptyCursorCount = 0;
                        if (querySpec.queryType == 1) {
                            AgendaWindowAdapter.access$2608(AgendaWindowAdapter.this);
                        } else if (querySpec.queryType == 0) {
                            AgendaWindowAdapter.access$2708(AgendaWindowAdapter.this);
                        }
                        i5 = ((DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getFirst()).start;
                        i6 = ((DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getLast()).end;
                    } else {
                        QuerySpec querySpec2 = (QuerySpec) AgendaWindowAdapter.this.mQueryQueue.peek();
                        if (!AgendaWindowAdapter.this.mAdapterInfos.isEmpty()) {
                            DayAdapterInfo dayAdapterInfo = (DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getFirst();
                            DayAdapterInfo dayAdapterInfo2 = (DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getLast();
                            if (dayAdapterInfo.start - 1 <= querySpec2.end && querySpec2.start < dayAdapterInfo.start) {
                                dayAdapterInfo.start = querySpec2.start;
                            }
                            if (querySpec2.start <= dayAdapterInfo2.end + 1 && dayAdapterInfo2.end < querySpec2.end) {
                                dayAdapterInfo2.end = querySpec2.end;
                            }
                            i3 = dayAdapterInfo.start;
                            i4 = dayAdapterInfo2.end;
                        } else {
                            i3 = querySpec2.start;
                            i4 = querySpec2.end;
                        }
                        switch (querySpec2.queryType) {
                            case 0:
                                i3 = querySpec2.start;
                                querySpec2.start -= 60;
                                break;
                            case 1:
                                i4 = querySpec2.end;
                                querySpec2.end += 60;
                                break;
                            case 2:
                                i3 = querySpec2.start;
                                i4 = querySpec2.end;
                                querySpec2.start -= 30;
                                querySpec2.end += 30;
                                break;
                        }
                        i5 = i3;
                        i6 = i4;
                        if (AgendaWindowAdapter.access$2504(AgendaWindowAdapter.this) > 1) {
                            AgendaWindowAdapter.this.mQueryQueue.poll();
                        }
                    }
                    AgendaWindowAdapter.this.updateHeaderFooter(i5, i6);
                    synchronized (AgendaWindowAdapter.this.mAdapterInfos) {
                        DayAdapterInfo dayAdapterInfo3 = (DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getFirst();
                        Time time4 = new Time(AgendaWindowAdapter.this.mTimeZone);
                        long jCurrentTimeMillis = System.currentTimeMillis();
                        time4.set(jCurrentTimeMillis);
                        int julianDay = Time.getJulianDay(jCurrentTimeMillis, time4.gmtoff);
                        if (dayAdapterInfo3 != null && julianDay >= dayAdapterInfo3.start && julianDay <= ((DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getLast()).end) {
                            Iterator it = AgendaWindowAdapter.this.mAdapterInfos.iterator();
                            boolean z2 = false;
                            while (it.hasNext() && !z2) {
                                DayAdapterInfo dayAdapterInfo4 = (DayAdapterInfo) it.next();
                                int i8 = 0;
                                while (true) {
                                    if (i8 >= dayAdapterInfo4.size) {
                                        break;
                                    }
                                    if (dayAdapterInfo4.dayAdapter.findJulianDayFromPosition(i8) < julianDay) {
                                        i8++;
                                    } else {
                                        dayAdapterInfo4.dayAdapter.setAsFirstDayAfterYesterday(i8);
                                        z2 = true;
                                    }
                                }
                            }
                        }
                    }
                    Iterator it2 = AgendaWindowAdapter.this.mQueryQueue.iterator();
                    while (it2.hasNext()) {
                        QuerySpec querySpec3 = (QuerySpec) it2.next();
                        if (querySpec3.queryType != 2 && AgendaWindowAdapter.this.isInRange(querySpec3.start, querySpec3.end)) {
                            it2.remove();
                        }
                        AgendaWindowAdapter.this.doQuery(querySpec3);
                        if (querySpec.goToTime != null && AgendaWindowAdapter.this.mQueryQueue.size() == 0) {
                            AgendaWindowAdapter.sTopDeviationInfo[0] = 0;
                            AgendaWindowAdapter.sTopDeviationInfo[1] = 0;
                        }
                    }
                    if (querySpec.goToTime != null) {
                        AgendaWindowAdapter.sTopDeviationInfo[0] = 0;
                        AgendaWindowAdapter.sTopDeviationInfo[1] = 0;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }

        private int processNewCursor(QuerySpec querySpec, Cursor cursor) {
            int i;
            synchronized (AgendaWindowAdapter.this.mAdapterInfos) {
                DayAdapterInfo dayAdapterInfoPruneAdapterInfo = AgendaWindowAdapter.this.pruneAdapterInfo(querySpec.queryType);
                if (dayAdapterInfoPruneAdapterInfo == null) {
                    dayAdapterInfoPruneAdapterInfo = new DayAdapterInfo(AgendaWindowAdapter.this.mContext);
                    i = 0;
                } else {
                    i = -dayAdapterInfoPruneAdapterInfo.size;
                }
                dayAdapterInfoPruneAdapterInfo.start = querySpec.start;
                dayAdapterInfoPruneAdapterInfo.end = querySpec.end;
                dayAdapterInfoPruneAdapterInfo.cursor = cursor;
                dayAdapterInfoPruneAdapterInfo.dayAdapter.changeCursor(dayAdapterInfoPruneAdapterInfo);
                dayAdapterInfoPruneAdapterInfo.size = dayAdapterInfoPruneAdapterInfo.dayAdapter.getCount();
                if (AgendaWindowAdapter.this.mAdapterInfos.isEmpty() || querySpec.end <= ((DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getFirst()).start) {
                    AgendaWindowAdapter.this.mAdapterInfos.addFirst(dayAdapterInfoPruneAdapterInfo);
                    i += dayAdapterInfoPruneAdapterInfo.size;
                } else {
                    AgendaWindowAdapter.this.mAdapterInfos.addLast(dayAdapterInfoPruneAdapterInfo);
                }
                AgendaWindowAdapter.this.mRowCount = 0;
                for (DayAdapterInfo dayAdapterInfo : AgendaWindowAdapter.this.mAdapterInfos) {
                    dayAdapterInfo.offset = AgendaWindowAdapter.this.mRowCount;
                    AgendaWindowAdapter.access$3212(AgendaWindowAdapter.this, dayAdapterInfo.size);
                }
                AgendaWindowAdapter.this.mLastUsedInfo = null;
            }
            return i;
        }
    }

    public void onResume() {
        this.mTZUpdater.run();
    }

    public void setHideDeclinedEvents(boolean z) {
        this.mHideDeclined = z;
    }

    public void setSelectedView(View view) {
        if (view != null) {
            ?? tag = view.getTag();
            if (tag instanceof AgendaAdapter.ViewHolder) {
                this.mSelectedVH = tag;
                if (this.mSelectedInstanceId != this.mSelectedVH.instanceId) {
                    this.mSelectedInstanceId = this.mSelectedVH.instanceId;
                    notifyDataSetChanged();
                }
            }
        }
    }

    public AgendaAdapter.ViewHolder getSelectedViewHolder() {
        return this.mSelectedVH;
    }

    public long getSelectedInstanceId() {
        return this.mSelectedInstanceId;
    }

    public void setSelectedInstanceId(long j) {
        this.mSelectedInstanceId = j;
        this.mSelectedVH = null;
    }

    private long findInstanceIdFromPosition(int i) {
        DayAdapterInfo adapterInfoByPosition = getAdapterInfoByPosition(i);
        if (adapterInfoByPosition != null) {
            return adapterInfoByPosition.dayAdapter.getInstanceId(i - adapterInfoByPosition.offset);
        }
        return -1L;
    }

    private long findStartTimeFromPosition(int i) {
        DayAdapterInfo adapterInfoByPosition = getAdapterInfoByPosition(i);
        if (adapterInfoByPosition != null) {
            return adapterInfoByPosition.dayAdapter.getStartTime(i - adapterInfoByPosition.offset);
        }
        return -1L;
    }

    private Cursor getCursorByPosition(int i) {
        DayAdapterInfo adapterInfoByPosition = getAdapterInfoByPosition(i);
        if (adapterInfoByPosition != null) {
            return adapterInfoByPosition.cursor;
        }
        return null;
    }

    private int getCursorPositionByPosition(int i) {
        DayAdapterInfo adapterInfoByPosition = getAdapterInfoByPosition(i);
        if (adapterInfoByPosition != null) {
            return adapterInfoByPosition.dayAdapter.getCursorPosition(i - adapterInfoByPosition.offset);
        }
        return -1;
    }

    @Override
    public int getHeaderPositionFromItemPosition(int i) {
        DayAdapterInfo adapterInfoByPosition;
        int headerPosition;
        if (!this.mIsTabletConfig || (adapterInfoByPosition = getAdapterInfoByPosition(i)) == null || (headerPosition = adapterInfoByPosition.dayAdapter.getHeaderPosition(i - adapterInfoByPosition.offset)) == -1) {
            return -1;
        }
        return headerPosition + adapterInfoByPosition.offset;
    }

    @Override
    public int getHeaderItemsNumber(int i) {
        DayAdapterInfo adapterInfoByPosition;
        if (i < 0 || !this.mIsTabletConfig || (adapterInfoByPosition = getAdapterInfoByPosition(i)) == null) {
            return -1;
        }
        return adapterInfoByPosition.dayAdapter.getHeaderItemsCount(i - adapterInfoByPosition.offset);
    }

    @Override
    public void OnHeaderHeightChanged(int i) {
        this.mStickyHeaderSize = i;
    }

    public int getStickyHeaderHeight() {
        return this.mStickyHeaderSize;
    }

    public void setScrollState(int i) {
        this.mListViewScrollState = i;
    }

    private boolean checkQueryRange(QuerySpec querySpec) {
        if (!this.mAdapterInfos.isEmpty()) {
            int i = this.mAdapterInfos.getFirst().start;
            int i2 = this.mAdapterInfos.getLast().end;
            if ((querySpec.queryType == 0 && i <= 2440588) || (querySpec.queryType == 1 && i2 >= 2465059)) {
                Log.d("AgendaWindowAdapter", "preHandleQuery: out of range, do nothing");
                return false;
            }
        }
        return true;
    }

    private void adjustQueryRange(QuerySpec querySpec) {
        if ((querySpec.queryType == 2 || querySpec.queryType == 1) && querySpec.end > 2465059) {
            querySpec.end = 2465059;
            Log.w("AgendaWindowAdapter", "limitQueryData, reset end");
        }
        if ((querySpec.queryType == 2 || querySpec.queryType == 0) && querySpec.start < 2440588) {
            querySpec.start = 2440588;
            Log.w("AgendaWindowAdapter", "limitQueryData, reset start");
        }
    }
}
