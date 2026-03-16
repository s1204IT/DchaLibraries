package com.android.calendar.agenda;

import android.app.Activity;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.TextView;
import com.android.calendar.CalendarController;
import com.android.calendar.R;
import com.android.calendar.StickyHeaderListView;
import com.android.calendar.Utils;
import com.android.calendar.agenda.AgendaAdapter;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AgendaWindowAdapter extends BaseAdapter implements StickyHeaderListView.HeaderHeightListener, StickyHeaderListView.HeaderIndexer {
    private static final String[] PROJECTION = {"_id", "title", "eventLocation", "allDay", "hasAlarm", "displayColor", "rrule", "begin", "end", "event_id", "startDay", "endDay", "selfAttendeeStatus", "organizer", "ownerAccount", "canOrganizerRespond", "eventTimezone"};
    private final AgendaListView mAgendaListView;
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
    private final StringBuilder mStringBuilder = new StringBuilder(50);
    private final Formatter mFormatter = new Formatter(this.mStringBuilder, Locale.getDefault());

    static int access$2104(AgendaWindowAdapter x0) {
        int i = x0.mEmptyCursorCount + 1;
        x0.mEmptyCursorCount = i;
        return i;
    }

    static int access$2208(AgendaWindowAdapter x0) {
        int i = x0.mNewerRequestsProcessed;
        x0.mNewerRequestsProcessed = i + 1;
        return i;
    }

    static int access$2308(AgendaWindowAdapter x0) {
        int i = x0.mOlderRequestsProcessed;
        x0.mOlderRequestsProcessed = i + 1;
        return i;
    }

    static int access$2812(AgendaWindowAdapter x0, int x1) {
        int i = x0.mRowCount + x1;
        x0.mRowCount = i;
        return i;
    }

    static {
        if (!Utils.isJellybeanOrLater()) {
            PROJECTION[5] = "calendar_color";
        }
    }

    private static class QuerySpec {
        int end;
        Time goToTime;
        long id = -1;
        long queryStartMillis;
        int queryType;
        String searchQuery;
        int start;

        public QuerySpec(int queryType) {
            this.queryType = queryType;
        }

        public int hashCode() {
            int result = this.end + 31;
            int result2 = (((((result * 31) + ((int) (this.queryStartMillis ^ (this.queryStartMillis >>> 32)))) * 31) + this.queryType) * 31) + this.start;
            if (this.searchQuery != null) {
                result2 = (result2 * 31) + this.searchQuery.hashCode();
            }
            if (this.goToTime != null) {
                long goToTimeMillis = this.goToTime.toMillis(false);
                result2 = (result2 * 31) + ((int) ((goToTimeMillis >>> 32) ^ goToTimeMillis));
            }
            return (result2 * 31) + ((int) this.id);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                QuerySpec other = (QuerySpec) obj;
                if (this.end == other.end && this.queryStartMillis == other.queryStartMillis && this.queryType == other.queryType && this.start == other.start && !Utils.equals(this.searchQuery, other.searchQuery) && this.id == other.id) {
                    return this.goToTime != null ? this.goToTime.toMillis(false) == other.goToTime.toMillis(false) : other.goToTime == null;
                }
                return false;
            }
            return false;
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
            sb.append("Start:").append(time.toString());
            time.setJulianDay(this.end);
            time.normalize(false);
            sb.append(" End:").append(time.toString());
            sb.append(" Offset:").append(this.offset);
            sb.append(" Size:").append(this.size);
            return sb.toString();
        }
    }

    public AgendaWindowAdapter(Context context, AgendaListView agendaListView, boolean showEventOnStart) {
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
        this.mShowEventOnStart = showEventOnStart;
        if (!this.mShowEventOnStart) {
            this.mStickyHeaderSize = 0;
        }
        this.mSearchQuery = null;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mHeaderView = (TextView) inflater.inflate(R.layout.agenda_header_footer, (ViewGroup) null);
        this.mFooterView = (TextView) inflater.inflate(R.layout.agenda_header_footer, (ViewGroup) null);
        this.mHeaderView.setText(R.string.loading);
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
    public int getItemViewType(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getItemViewType(position - info.offset);
        }
        return -1;
    }

    @Override
    public boolean isEnabled(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.isEnabled(position - info.offset);
        }
        return false;
    }

    @Override
    public int getCount() {
        return this.mRowCount;
    }

    @Override
    public Object getItem(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getItem(position - info.offset);
        }
        return null;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        int curPos;
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info == null || (curPos = info.dayAdapter.getCursorPosition(position - info.offset)) == Integer.MIN_VALUE) {
            return -1L;
        }
        if (curPos >= 0) {
            info.cursor.moveToPosition(curPos);
            return info.cursor.getLong(9) << ((int) (20 + info.cursor.getLong(7)));
        }
        return info.dayAdapter.findJulianDayFromPosition(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (position >= this.mRowCount - 1 && this.mNewerRequests <= this.mNewerRequestsProcessed) {
            this.mNewerRequests++;
            queueQuery(new QuerySpec(1));
        }
        if (position < 1 && this.mOlderRequests <= this.mOlderRequestsProcessed) {
            this.mOlderRequests++;
            queueQuery(new QuerySpec(0));
        }
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            int offset = position - info.offset;
            v = info.dayAdapter.getView(offset, convertView, parent);
            if (info.dayAdapter.isDayHeaderView(offset)) {
                View simpleDivider = v.findViewById(R.id.top_divider_simple);
                View pastPresentDivider = v.findViewById(R.id.top_divider_past_present);
                if (info.dayAdapter.isFirstDayAfterYesterday(offset)) {
                    if (simpleDivider != null && pastPresentDivider != null) {
                        simpleDivider.setVisibility(8);
                        pastPresentDivider.setVisibility(0);
                    }
                } else if (simpleDivider != null && pastPresentDivider != null) {
                    simpleDivider.setVisibility(0);
                    pastPresentDivider.setVisibility(8);
                }
            }
        } else {
            Log.e("AgendaWindowAdapter", "BUG: getAdapterInfoByPosition returned null!!! " + position);
            TextView tv = new TextView(this.mContext);
            tv.setText("Bug! " + position);
            v = tv;
        }
        if (this.mIsTabletConfig) {
            Object yy = v.getTag();
            if (yy instanceof AgendaAdapter.ViewHolder) {
                AgendaAdapter.ViewHolder vh = (AgendaAdapter.ViewHolder) yy;
                boolean selected = this.mSelectedInstanceId == vh.instanceId;
                vh.selectedMarker.setVisibility((selected && this.mShowEventOnStart) ? 0 : 8);
                if (this.mShowEventOnStart) {
                    GridLayout.LayoutParams lp = (GridLayout.LayoutParams) vh.textContainer.getLayoutParams();
                    if (selected) {
                        this.mSelectedVH = vh;
                        v.setBackgroundColor(this.mSelectedItemBackgroundColor);
                        vh.title.setTextColor(this.mSelectedItemTextColor);
                        vh.when.setTextColor(this.mSelectedItemTextColor);
                        vh.where.setTextColor(this.mSelectedItemTextColor);
                        lp.setMargins(0, 0, 0, 0);
                        vh.textContainer.setLayoutParams(lp);
                    } else {
                        lp.setMargins(0, 0, (int) this.mItemRightMargin, 0);
                        vh.textContainer.setLayoutParams(lp);
                    }
                }
            }
        }
        return v;
    }

    private int findEventPositionNearestTime(Time time, long id) {
        DayAdapterInfo info = getAdapterInfoByTime(time);
        if (info == null) {
            return -1;
        }
        int pos = info.offset + info.dayAdapter.findEventPositionNearestTime(time, id);
        return pos;
    }

    protected DayAdapterInfo getAdapterInfoByPosition(int position) {
        synchronized (this.mAdapterInfos) {
            if (this.mLastUsedInfo != null && this.mLastUsedInfo.offset <= position && position < this.mLastUsedInfo.offset + this.mLastUsedInfo.size) {
                return this.mLastUsedInfo;
            }
            for (DayAdapterInfo info : this.mAdapterInfos) {
                if (info.offset <= position && position < info.offset + info.size) {
                    this.mLastUsedInfo = info;
                    return info;
                }
            }
            return null;
        }
    }

    private DayAdapterInfo getAdapterInfoByTime(Time time) {
        Time tmpTime = new Time(time);
        long timeInMillis = tmpTime.normalize(true);
        int day = Time.getJulianDay(timeInMillis, tmpTime.gmtoff);
        synchronized (this.mAdapterInfos) {
            for (DayAdapterInfo info : this.mAdapterInfos) {
                if (info.start <= day && day <= info.end) {
                    return info;
                }
            }
            return null;
        }
    }

    public AgendaItem getAgendaItemByPosition(int positionInListView) {
        return getAgendaItemByPosition(positionInListView, true);
    }

    public AgendaItem getAgendaItemByPosition(int positionInListView, boolean returnEventStartDay) {
        int positionInAdapter;
        DayAdapterInfo info;
        int cursorPosition;
        AgendaItem item = null;
        if (positionInListView >= 0 && (info = getAdapterInfoByPosition(positionInListView - 1)) != null && (cursorPosition = info.dayAdapter.getCursorPosition(positionInAdapter - info.offset)) != Integer.MIN_VALUE) {
            boolean isDayHeader = false;
            if (cursorPosition < 0) {
                cursorPosition = -cursorPosition;
                isDayHeader = true;
            }
            if (cursorPosition < info.cursor.getCount()) {
                item = buildAgendaItemFromCursor(info.cursor, cursorPosition, isDayHeader);
                if (!returnEventStartDay && !isDayHeader) {
                    item.startDay = info.dayAdapter.findJulianDayFromPosition(positionInAdapter - info.offset);
                }
            }
        }
        return item;
    }

    private AgendaItem buildAgendaItemFromCursor(Cursor cursor, int cursorPosition, boolean isDayHeader) {
        if (cursorPosition == -1) {
            cursor.moveToFirst();
        } else {
            cursor.moveToPosition(cursorPosition);
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
        } else if (isDayHeader) {
            Time time2 = new Time(this.mTimeZone);
            time2.set(agendaItem.begin);
            time2.hour = 0;
            time2.minute = 0;
            time2.second = 0;
            agendaItem.begin = time2.toMillis(false);
        }
        if (!isDayHeader) {
            agendaItem.id = cursor.getLong(9);
            if (agendaItem.allDay) {
                Time time3 = new Time(this.mTimeZone);
                time3.setJulianDay(Time.getJulianDay(agendaItem.end, 0L));
                agendaItem.end = time3.toMillis(false);
            }
        }
        return agendaItem;
    }

    private void sendViewEvent(AgendaItem item, long selectedTime) {
        long startTime;
        long endTime;
        if (item.allDay) {
            startTime = Utils.convertAlldayLocalToUTC(null, item.begin, this.mTimeZone);
            endTime = Utils.convertAlldayLocalToUTC(null, item.end, this.mTimeZone);
        } else {
            startTime = item.begin;
            endTime = item.end;
        }
        CalendarController.getInstance(this.mContext).sendEventRelatedEventWithExtra(this, 2L, item.id, startTime, endTime, 0, 0, CalendarController.EventInfo.buildViewExtraLong(0, item.allDay), selectedTime);
    }

    public void refresh(Time goToTime, long id, String searchQuery, boolean forced, boolean refreshEventInfo) {
        if (searchQuery != null) {
            this.mSearchQuery = searchQuery;
        }
        int startDay = Time.getJulianDay(goToTime.toMillis(false), goToTime.gmtoff);
        if (!forced && isInRange(startDay, startDay)) {
            if (!this.mAgendaListView.isAgendaItemVisible(goToTime, id)) {
                int gotoPosition = findEventPositionNearestTime(goToTime, id);
                if (gotoPosition > 0) {
                    this.mAgendaListView.setSelectionFromTop(gotoPosition + 1, this.mStickyHeaderSize);
                    if (this.mListViewScrollState == 2) {
                        this.mAgendaListView.smoothScrollBy(0, 0);
                    }
                    if (refreshEventInfo) {
                        long newInstanceId = findInstanceIdFromPosition(gotoPosition);
                        if (newInstanceId != getSelectedInstanceId()) {
                            setSelectedInstanceId(newInstanceId);
                            this.mDataChangedHandler.post(this.mDataChangedRunnable);
                            Cursor tempCursor = getCursorByPosition(gotoPosition);
                            if (tempCursor != null) {
                                int tempCursorPosition = getCursorPositionByPosition(gotoPosition);
                                AgendaItem item = buildAgendaItemFromCursor(tempCursor, tempCursorPosition, false);
                                this.mSelectedVH = new AgendaAdapter.ViewHolder();
                                this.mSelectedVH.allDay = item.allDay;
                                sendViewEvent(item, goToTime.toMillis(false));
                            }
                        }
                    }
                }
                Time actualTime = new Time(this.mTimeZone);
                actualTime.set(goToTime);
                CalendarController.getInstance(this.mContext).sendEvent(this, 1024L, actualTime, actualTime, -1L, 0);
                return;
            }
            return;
        }
        if (!this.mCleanQueryInitiated || searchQuery != null) {
            int endDay = startDay + 7;
            this.mSelectedInstanceId = -1L;
            this.mCleanQueryInitiated = true;
            queueQuery(startDay, endDay, goToTime, searchQuery, 2, id);
            this.mOlderRequests++;
            queueQuery(0, 0, goToTime, searchQuery, 0, id);
            this.mNewerRequests++;
            queueQuery(0, 0, goToTime, searchQuery, 1, id);
        }
    }

    public void close() {
        this.mShuttingDown = true;
        pruneAdapterInfo(2);
        if (this.mQueryHandler != null) {
            this.mQueryHandler.cancelOperation(0);
        }
    }

    private DayAdapterInfo pruneAdapterInfo(int queryType) {
        DayAdapterInfo info;
        synchronized (this.mAdapterInfos) {
            DayAdapterInfo recycleMe = null;
            if (!this.mAdapterInfos.isEmpty()) {
                if (this.mAdapterInfos.size() >= 5) {
                    if (queryType == 1) {
                        recycleMe = this.mAdapterInfos.removeFirst();
                    } else if (queryType == 0) {
                        recycleMe = this.mAdapterInfos.removeLast();
                        recycleMe.size = 0;
                    }
                    if (recycleMe != null) {
                        if (recycleMe.cursor != null) {
                            recycleMe.cursor.close();
                        }
                        return recycleMe;
                    }
                }
                if (this.mRowCount == 0 || queryType == 2) {
                    this.mRowCount = 0;
                    int deletedRows = 0;
                    do {
                        info = this.mAdapterInfos.poll();
                        if (info != null) {
                            info.cursor.close();
                            deletedRows += info.size;
                            recycleMe = info;
                        }
                    } while (info != null);
                    if (recycleMe != null) {
                        recycleMe.cursor = null;
                        recycleMe.size = deletedRows;
                    }
                }
            }
            return recycleMe;
        }
    }

    private String buildQuerySelection() {
        return this.mHideDeclined ? "visible=1 AND selfAttendeeStatus!=2" : "visible=1";
    }

    private Uri buildQueryUri(int start, int end, String searchQuery) {
        Uri rootUri = searchQuery == null ? CalendarContract.Instances.CONTENT_BY_DAY_URI : CalendarContract.Instances.CONTENT_SEARCH_BY_DAY_URI;
        Uri.Builder builder = rootUri.buildUpon();
        ContentUris.appendId(builder, start);
        ContentUris.appendId(builder, end);
        if (searchQuery != null) {
            builder.appendPath(searchQuery);
        }
        return builder.build();
    }

    private boolean isInRange(int start, int end) {
        synchronized (this.mAdapterInfos) {
            if (this.mAdapterInfos.isEmpty()) {
                return false;
            }
            return this.mAdapterInfos.getFirst().start <= start && end <= this.mAdapterInfos.getLast().end;
        }
    }

    private int calculateQueryDuration(int start, int end) {
        int queryDuration = 60;
        if (this.mRowCount != 0) {
            queryDuration = (((end - start) + 1) * 50) / this.mRowCount;
        }
        if (queryDuration > 60) {
            return 60;
        }
        if (queryDuration < 7) {
            return 7;
        }
        return queryDuration;
    }

    private boolean queueQuery(int start, int end, Time goToTime, String searchQuery, int queryType, long id) {
        QuerySpec queryData = new QuerySpec(queryType);
        queryData.goToTime = new Time(goToTime);
        queryData.start = start;
        queryData.end = end;
        queryData.searchQuery = searchQuery;
        queryData.id = id;
        return queueQuery(queryData);
    }

    private boolean queueQuery(QuerySpec queryData) {
        Boolean queuedQuery;
        queryData.searchQuery = this.mSearchQuery;
        synchronized (this.mQueryQueue) {
            Boolean.valueOf(false);
            Boolean doQueryNow = Boolean.valueOf(this.mQueryQueue.isEmpty());
            this.mQueryQueue.add(queryData);
            queuedQuery = true;
            if (doQueryNow.booleanValue()) {
                doQuery(queryData);
            }
        }
        return queuedQuery.booleanValue();
    }

    private void doQuery(QuerySpec queryData) {
        if (!this.mAdapterInfos.isEmpty()) {
            int start = this.mAdapterInfos.getFirst().start;
            int end = this.mAdapterInfos.getLast().end;
            int queryDuration = calculateQueryDuration(start, end);
            switch (queryData.queryType) {
                case 0:
                    queryData.end = start - 1;
                    queryData.start = queryData.end - queryDuration;
                    break;
                case 1:
                    queryData.start = end + 1;
                    queryData.end = queryData.start + queryDuration;
                    break;
            }
            if (this.mRowCount < 20 && queryData.queryType != 2) {
                queryData.queryType = 2;
                if (queryData.start > start) {
                    queryData.start = start;
                }
                if (queryData.end < end) {
                    queryData.end = end;
                }
            }
        }
        this.mQueryHandler.cancelOperation(0);
        Uri queryUri = buildQueryUri(queryData.start, queryData.end, queryData.searchQuery);
        this.mQueryHandler.startQuery(0, queryData, queryUri, PROJECTION, buildQuerySelection(), null, "startDay ASC, begin ASC, title ASC");
    }

    private String formatDateString(int julianDay) {
        Time time = new Time(this.mTimeZone);
        time.setJulianDay(julianDay);
        long millis = time.toMillis(false);
        this.mStringBuilder.setLength(0);
        return DateUtils.formatDateRange(this.mContext, this.mFormatter, millis, millis, 65556, this.mTimeZone).toString();
    }

    private void updateHeaderFooter(int start, int end) {
        this.mHeaderView.setText(this.mContext.getString(R.string.show_older_events, formatDateString(start)));
        this.mFooterView.setText(this.mContext.getString(R.string.show_newer_events, formatDateString(end)));
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            int totalAgendaRangeStart;
            int totalAgendaRangeEnd;
            QuerySpec data = (QuerySpec) cookie;
            if (cursor == null) {
                if (AgendaWindowAdapter.this.mAgendaListView != null && (AgendaWindowAdapter.this.mAgendaListView.getContext() instanceof Activity)) {
                    ((Activity) AgendaWindowAdapter.this.mAgendaListView.getContext()).finish();
                    return;
                }
                return;
            }
            if (data.queryType == 2) {
                AgendaWindowAdapter.this.mCleanQueryInitiated = false;
            }
            if (AgendaWindowAdapter.this.mShuttingDown) {
                cursor.close();
                return;
            }
            int cursorSize = cursor.getCount();
            if (cursorSize > 0 || AgendaWindowAdapter.this.mAdapterInfos.isEmpty() || data.queryType == 2) {
                int listPositionOffset = processNewCursor(data, cursor);
                int newPosition = -1;
                if (data.goToTime == null) {
                    AgendaWindowAdapter.this.notifyDataSetChanged();
                    if (listPositionOffset != 0) {
                        AgendaWindowAdapter.this.mAgendaListView.shiftSelection(listPositionOffset);
                    }
                } else {
                    Time goToTime = data.goToTime;
                    AgendaWindowAdapter.this.notifyDataSetChanged();
                    newPosition = AgendaWindowAdapter.this.findEventPositionNearestTime(goToTime, data.id);
                    if (newPosition >= 0) {
                        if (AgendaWindowAdapter.this.mListViewScrollState == 2) {
                            AgendaWindowAdapter.this.mAgendaListView.smoothScrollBy(0, 0);
                        }
                        AgendaWindowAdapter.this.mAgendaListView.setSelectionFromTop(newPosition + 1, AgendaWindowAdapter.this.mStickyHeaderSize);
                        Time actualTime = new Time(AgendaWindowAdapter.this.mTimeZone);
                        actualTime.set(goToTime);
                        CalendarController.getInstance(AgendaWindowAdapter.this.mContext).sendEvent(this, 1024L, actualTime, actualTime, -1L, 0);
                    }
                }
                if (AgendaWindowAdapter.this.mSelectedInstanceId == -1 && newPosition != -1 && data.queryType == 2 && (data.id != -1 || data.goToTime != null)) {
                    AgendaWindowAdapter.this.mSelectedInstanceId = AgendaWindowAdapter.this.findInstanceIdFromPosition(newPosition);
                }
                if (AgendaWindowAdapter.this.mAdapterInfos.size() == 1 && AgendaWindowAdapter.this.mSelectedInstanceId != -1) {
                    boolean found = false;
                    cursor.moveToPosition(-1);
                    while (true) {
                        if (!cursor.moveToNext()) {
                            break;
                        } else if (AgendaWindowAdapter.this.mSelectedInstanceId == cursor.getLong(0)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        AgendaWindowAdapter.this.mSelectedInstanceId = -1L;
                    }
                }
                if (AgendaWindowAdapter.this.mShowEventOnStart && data.queryType == 2) {
                    Cursor tempCursor = null;
                    int tempCursorPosition = -1;
                    if (AgendaWindowAdapter.this.mSelectedInstanceId == -1) {
                        if (cursor.moveToFirst()) {
                            AgendaWindowAdapter.this.mSelectedInstanceId = cursor.getLong(0);
                            AgendaWindowAdapter.this.mSelectedVH = new AgendaAdapter.ViewHolder();
                            AgendaWindowAdapter.this.mSelectedVH.allDay = cursor.getInt(3) != 0;
                            tempCursor = cursor;
                        }
                    } else if (newPosition != -1) {
                        tempCursor = AgendaWindowAdapter.this.getCursorByPosition(newPosition);
                        tempCursorPosition = AgendaWindowAdapter.this.getCursorPositionByPosition(newPosition);
                    }
                    if (tempCursor != null) {
                        AgendaItem item = AgendaWindowAdapter.this.buildAgendaItemFromCursor(tempCursor, tempCursorPosition, false);
                        long selectedTime = AgendaWindowAdapter.this.findStartTimeFromPosition(newPosition);
                        AgendaWindowAdapter.this.sendViewEvent(item, selectedTime);
                    }
                }
            } else {
                cursor.close();
            }
            if (!AgendaWindowAdapter.this.mDoneSettingUpHeaderFooter) {
                View.OnClickListener headerFooterOnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (v == AgendaWindowAdapter.this.mHeaderView) {
                            AgendaWindowAdapter.this.queueQuery(new QuerySpec(0));
                        } else {
                            AgendaWindowAdapter.this.queueQuery(new QuerySpec(1));
                        }
                    }
                };
                AgendaWindowAdapter.this.mHeaderView.setOnClickListener(headerFooterOnClickListener);
                AgendaWindowAdapter.this.mFooterView.setOnClickListener(headerFooterOnClickListener);
                AgendaWindowAdapter.this.mAgendaListView.addFooterView(AgendaWindowAdapter.this.mFooterView);
                AgendaWindowAdapter.this.mDoneSettingUpHeaderFooter = true;
            }
            synchronized (AgendaWindowAdapter.this.mQueryQueue) {
                if (cursorSize != 0) {
                    AgendaWindowAdapter.this.mEmptyCursorCount = 0;
                    if (data.queryType == 1) {
                        AgendaWindowAdapter.access$2208(AgendaWindowAdapter.this);
                    } else if (data.queryType == 0) {
                        AgendaWindowAdapter.access$2308(AgendaWindowAdapter.this);
                    }
                    totalAgendaRangeStart = ((DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getFirst()).start;
                    totalAgendaRangeEnd = ((DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getLast()).end;
                } else {
                    QuerySpec querySpec = (QuerySpec) AgendaWindowAdapter.this.mQueryQueue.peek();
                    if (!AgendaWindowAdapter.this.mAdapterInfos.isEmpty()) {
                        DayAdapterInfo first = (DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getFirst();
                        DayAdapterInfo last = (DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getLast();
                        if (first.start - 1 <= querySpec.end && querySpec.start < first.start) {
                            first.start = querySpec.start;
                        }
                        if (querySpec.start <= last.end + 1 && last.end < querySpec.end) {
                            last.end = querySpec.end;
                        }
                        totalAgendaRangeStart = first.start;
                        totalAgendaRangeEnd = last.end;
                    } else {
                        totalAgendaRangeStart = querySpec.start;
                        totalAgendaRangeEnd = querySpec.end;
                    }
                    switch (querySpec.queryType) {
                        case 0:
                            totalAgendaRangeStart = querySpec.start;
                            querySpec.start -= 60;
                            break;
                        case 1:
                            totalAgendaRangeEnd = querySpec.end;
                            querySpec.end += 60;
                            break;
                        case 2:
                            totalAgendaRangeStart = querySpec.start;
                            totalAgendaRangeEnd = querySpec.end;
                            querySpec.start -= 30;
                            querySpec.end += 30;
                            break;
                    }
                    if (AgendaWindowAdapter.access$2104(AgendaWindowAdapter.this) > 1) {
                        AgendaWindowAdapter.this.mQueryQueue.poll();
                    }
                }
                AgendaWindowAdapter.this.updateHeaderFooter(totalAgendaRangeStart, totalAgendaRangeEnd);
                synchronized (AgendaWindowAdapter.this.mAdapterInfos) {
                    DayAdapterInfo info = (DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getFirst();
                    Time time = new Time(AgendaWindowAdapter.this.mTimeZone);
                    long now = System.currentTimeMillis();
                    time.set(now);
                    int JulianToday = Time.getJulianDay(now, time.gmtoff);
                    if (info != null && JulianToday >= info.start && JulianToday <= ((DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getLast()).end) {
                        Iterator<DayAdapterInfo> iter = AgendaWindowAdapter.this.mAdapterInfos.iterator();
                        boolean foundDay = false;
                        while (iter.hasNext() && !foundDay) {
                            DayAdapterInfo info2 = iter.next();
                            int i = 0;
                            while (true) {
                                if (i >= info2.size) {
                                    break;
                                }
                                if (info2.dayAdapter.findJulianDayFromPosition(i) < JulianToday) {
                                    i++;
                                } else {
                                    info2.dayAdapter.setAsFirstDayAfterYesterday(i);
                                    foundDay = true;
                                }
                            }
                        }
                    }
                }
                Iterator<QuerySpec> it = AgendaWindowAdapter.this.mQueryQueue.iterator();
                while (it.hasNext()) {
                    QuerySpec queryData = it.next();
                    if (queryData.queryType == 2 || !AgendaWindowAdapter.this.isInRange(queryData.start, queryData.end)) {
                        AgendaWindowAdapter.this.doQuery(queryData);
                    } else {
                        it.remove();
                    }
                }
            }
        }

        private int processNewCursor(QuerySpec data, Cursor cursor) {
            int listPositionOffset;
            synchronized (AgendaWindowAdapter.this.mAdapterInfos) {
                DayAdapterInfo info = AgendaWindowAdapter.this.pruneAdapterInfo(data.queryType);
                listPositionOffset = 0;
                if (info == null) {
                    info = new DayAdapterInfo(AgendaWindowAdapter.this.mContext);
                } else {
                    listPositionOffset = -info.size;
                }
                info.start = data.start;
                info.end = data.end;
                info.cursor = cursor;
                info.dayAdapter.changeCursor(info);
                info.size = info.dayAdapter.getCount();
                if (AgendaWindowAdapter.this.mAdapterInfos.isEmpty() || data.end <= ((DayAdapterInfo) AgendaWindowAdapter.this.mAdapterInfos.getFirst()).start) {
                    AgendaWindowAdapter.this.mAdapterInfos.addFirst(info);
                    listPositionOffset += info.size;
                } else {
                    AgendaWindowAdapter.this.mAdapterInfos.addLast(info);
                }
                AgendaWindowAdapter.this.mRowCount = 0;
                for (DayAdapterInfo info3 : AgendaWindowAdapter.this.mAdapterInfos) {
                    info3.offset = AgendaWindowAdapter.this.mRowCount;
                    AgendaWindowAdapter.access$2812(AgendaWindowAdapter.this, info3.size);
                }
                AgendaWindowAdapter.this.mLastUsedInfo = null;
            }
            return listPositionOffset;
        }
    }

    public void onResume() {
        this.mTZUpdater.run();
    }

    public void setHideDeclinedEvents(boolean hideDeclined) {
        this.mHideDeclined = hideDeclined;
    }

    public void setSelectedView(View v) {
        if (v != null) {
            Object vh = v.getTag();
            if (vh instanceof AgendaAdapter.ViewHolder) {
                this.mSelectedVH = (AgendaAdapter.ViewHolder) vh;
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

    public void setSelectedInstanceId(long selectedInstanceId) {
        this.mSelectedInstanceId = selectedInstanceId;
        this.mSelectedVH = null;
    }

    private long findInstanceIdFromPosition(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getInstanceId(position - info.offset);
        }
        return -1L;
    }

    private long findStartTimeFromPosition(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getStartTime(position - info.offset);
        }
        return -1L;
    }

    private Cursor getCursorByPosition(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.cursor;
        }
        return null;
    }

    private int getCursorPositionByPosition(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getCursorPosition(position - info.offset);
        }
        return -1;
    }

    @Override
    public int getHeaderPositionFromItemPosition(int position) {
        DayAdapterInfo info;
        int pos;
        if (!this.mIsTabletConfig || (info = getAdapterInfoByPosition(position)) == null || (pos = info.dayAdapter.getHeaderPosition(position - info.offset)) == -1) {
            return -1;
        }
        return info.offset + pos;
    }

    @Override
    public int getHeaderItemsNumber(int headerPosition) {
        DayAdapterInfo info;
        if (headerPosition < 0 || !this.mIsTabletConfig || (info = getAdapterInfoByPosition(headerPosition)) == null) {
            return -1;
        }
        return info.dayAdapter.getHeaderItemsCount(headerPosition - info.offset);
    }

    @Override
    public void OnHeaderHeightChanged(int height) {
        this.mStickyHeaderSize = height;
    }

    public int getStickyHeaderHeight() {
        return this.mStickyHeaderSize;
    }

    public void setScrollState(int state) {
        this.mListViewScrollState = state;
    }
}
