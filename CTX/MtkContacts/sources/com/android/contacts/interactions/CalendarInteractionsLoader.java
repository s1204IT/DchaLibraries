package com.android.contacts.interactions;

import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.CalendarContract;
import com.android.contacts.util.PermissionsUtil;
import com.google.common.base.Preconditions;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class CalendarInteractionsLoader extends AsyncTaskLoader<List<ContactInteraction>> {
    private List<ContactInteraction> mData;
    private List<String> mEmailAddresses;
    private int mMaxFutureToRetrieve;
    private int mMaxPastToRetrieve;
    private long mNumberFutureMillisecondToSearchLocalCalendar;
    private long mNumberPastMillisecondToSearchLocalCalendar;

    public CalendarInteractionsLoader(Context context, List<String> list, int i, int i2, long j, long j2) {
        super(context);
        this.mEmailAddresses = list;
        this.mMaxFutureToRetrieve = i;
        this.mMaxPastToRetrieve = i2;
        this.mNumberFutureMillisecondToSearchLocalCalendar = j;
        this.mNumberPastMillisecondToSearchLocalCalendar = j2;
    }

    @Override
    public List<ContactInteraction> loadInBackground() {
        if (!PermissionsUtil.hasPermission(getContext(), "android.permission.READ_CALENDAR") || this.mEmailAddresses == null || this.mEmailAddresses.size() < 1) {
            return Collections.emptyList();
        }
        List<ContactInteraction> interactionsFromEventsCursor = getInteractionsFromEventsCursor(getSharedEventsCursor(true, this.mMaxFutureToRetrieve));
        List<ContactInteraction> interactionsFromEventsCursor2 = getInteractionsFromEventsCursor(getSharedEventsCursor(false, this.mMaxPastToRetrieve));
        ArrayList arrayList = new ArrayList(interactionsFromEventsCursor.size() + interactionsFromEventsCursor2.size());
        arrayList.addAll(interactionsFromEventsCursor);
        arrayList.addAll(interactionsFromEventsCursor2);
        if (Log.isLoggable("CalendarInteractions", 2)) {
            Log.v("CalendarInteractions", "# ContactInteraction Loaded: " + arrayList.size());
        }
        return arrayList;
    }

    private Cursor getSharedEventsCursor(boolean z, int i) {
        List<String> ownedCalendarIds = getOwnedCalendarIds();
        if (ownedCalendarIds == null) {
            return null;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.mEmailAddresses);
        arrayList.addAll(ownedCalendarIds);
        String str = z ? " > " : " < ";
        arrayList.addAll(Arrays.asList(String.valueOf(jCurrentTimeMillis), String.valueOf(jCurrentTimeMillis - this.mNumberPastMillisecondToSearchLocalCalendar), String.valueOf(this.mNumberFutureMillisecondToSearchLocalCalendar + jCurrentTimeMillis)));
        StringBuilder sb = new StringBuilder();
        sb.append("dtstart");
        sb.append(z ? " ASC " : " DESC ");
        String string = sb.toString();
        String str2 = caseAndDotInsensitiveEmailComparisonClause(this.mEmailAddresses.size()) + " AND calendar_id IN " + ContactInteractionUtil.questionMarks(ownedCalendarIds.size()) + " AND dtstart" + str + " ?  AND dtstart > ?  AND dtstart < ?  AND lastSynced = 0";
        return getContext().getContentResolver().query(CalendarContract.Attendees.CONTENT_URI, null, str2, (String[]) arrayList.toArray(new String[arrayList.size()]), string + " LIMIT " + i);
    }

    private String caseAndDotInsensitiveEmailComparisonClause(int i) {
        Preconditions.checkArgument(i > 0, "Count needs to be positive");
        StringBuilder sb = new StringBuilder("(  REPLACE(attendeeEmail, '.', '') = REPLACE(?, '.', '') COLLATE NOCASE");
        for (int i2 = 1; i2 < i; i2++) {
            sb.append(" OR  REPLACE(attendeeEmail, '.', '') = REPLACE(?, '.', '') COLLATE NOCASE");
        }
        sb.append(")");
        return sb.toString();
    }

    private List<ContactInteraction> getInteractionsFromEventsCursor(Cursor cursor) {
        if (cursor != null) {
            try {
                if (cursor.getCount() != 0) {
                    HashSet hashSet = new HashSet();
                    ArrayList arrayList = new ArrayList();
                    while (cursor.moveToNext()) {
                        ContentValues contentValues = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(cursor, contentValues);
                        CalendarInteraction calendarInteraction = new CalendarInteraction(contentValues);
                        if (!hashSet.contains(calendarInteraction.getIntent().getData().toString())) {
                            hashSet.add(calendarInteraction.getIntent().getData().toString());
                            arrayList.add(calendarInteraction);
                        }
                    }
                    return arrayList;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        List<ContactInteraction> listEmptyList = Collections.emptyList();
        if (cursor != null) {
            cursor.close();
        }
        return listEmptyList;
    }

    private List<String> getOwnedCalendarIds() {
        Cursor cursorQuery = getContext().getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, new String[]{"_id", "calendar_access_level"}, "visible = 1 AND calendar_access_level = ? ", new String[]{String.valueOf(700)}, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() >= 1) {
                    cursorQuery.moveToPosition(-1);
                    ArrayList arrayList = new ArrayList(cursorQuery.getCount());
                    while (cursorQuery.moveToNext()) {
                        arrayList.add(String.valueOf(cursorQuery.getInt(0)));
                    }
                    return arrayList;
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return null;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (this.mData != null) {
            deliverResult(this.mData);
        }
        if (takeContentChanged() || this.mData == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        if (this.mData != null) {
            this.mData.clear();
        }
    }

    @Override
    public void deliverResult(List<ContactInteraction> list) {
        this.mData = list;
        if (isStarted()) {
            super.deliverResult(list);
        }
    }
}
