package com.android.contacts.interactions;

import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.CalendarContract;
import android.util.Log;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarInteractionsLoader extends AsyncTaskLoader<List<ContactInteraction>> {
    private static final String TAG = CalendarInteractionsLoader.class.getSimpleName();
    private List<ContactInteraction> mData;
    private List<String> mEmailAddresses;
    private int mMaxFutureToRetrieve;
    private int mMaxPastToRetrieve;
    private long mNumberFutureMillisecondToSearchLocalCalendar;
    private long mNumberPastMillisecondToSearchLocalCalendar;

    public CalendarInteractionsLoader(Context context, List<String> emailAddresses, int maxFutureToRetrieve, int maxPastToRetrieve, long numberFutureMillisecondToSearchLocalCalendar, long numberPastMillisecondToSearchLocalCalendar) {
        super(context);
        this.mEmailAddresses = emailAddresses;
        this.mMaxFutureToRetrieve = maxFutureToRetrieve;
        this.mMaxPastToRetrieve = maxPastToRetrieve;
        this.mNumberFutureMillisecondToSearchLocalCalendar = numberFutureMillisecondToSearchLocalCalendar;
        this.mNumberPastMillisecondToSearchLocalCalendar = numberPastMillisecondToSearchLocalCalendar;
    }

    @Override
    public List<ContactInteraction> loadInBackground() {
        if (this.mEmailAddresses == null || this.mEmailAddresses.size() < 1) {
            return Collections.emptyList();
        }
        Cursor cursor = getSharedEventsCursor(true, this.mMaxFutureToRetrieve);
        List<ContactInteraction> interactions = getInteractionsFromEventsCursor(cursor);
        Cursor cursor2 = getSharedEventsCursor(false, this.mMaxPastToRetrieve);
        List<ContactInteraction> interactions2 = getInteractionsFromEventsCursor(cursor2);
        ArrayList<ContactInteraction> allInteractions = new ArrayList<>(interactions.size() + interactions2.size());
        allInteractions.addAll(interactions);
        allInteractions.addAll(interactions2);
        Log.v(TAG, "# ContactInteraction Loaded: " + allInteractions.size());
        return allInteractions;
    }

    private Cursor getSharedEventsCursor(boolean isFuture, int limit) {
        List<String> calendarIds = getOwnedCalendarIds();
        if (calendarIds == null) {
            return null;
        }
        long timeMillis = System.currentTimeMillis();
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.addAll(this.mEmailAddresses);
        selectionArgs.addAll(calendarIds);
        String timeOperator = isFuture ? " > " : " < ";
        long pastTimeCutoff = timeMillis - this.mNumberPastMillisecondToSearchLocalCalendar;
        long futureTimeCutoff = timeMillis + this.mNumberFutureMillisecondToSearchLocalCalendar;
        String[] timeArguments = {String.valueOf(timeMillis), String.valueOf(pastTimeCutoff), String.valueOf(futureTimeCutoff)};
        selectionArgs.addAll(Arrays.asList(timeArguments));
        String orderBy = "dtstart" + (isFuture ? " ASC " : " DESC ");
        String selection = caseAndDotInsensitiveEmailComparisonClause(this.mEmailAddresses.size()) + " AND calendar_id IN " + ContactInteractionUtil.questionMarks(calendarIds.size()) + " AND dtstart" + timeOperator + " ?  AND dtstart > ?  AND dtstart < ?  AND lastSynced = 0";
        return getContext().getContentResolver().query(CalendarContract.Attendees.CONTENT_URI, null, selection, (String[]) selectionArgs.toArray(new String[selectionArgs.size()]), orderBy + " LIMIT " + limit);
    }

    private String caseAndDotInsensitiveEmailComparisonClause(int count) {
        Preconditions.checkArgument(count > 0, "Count needs to be positive");
        StringBuilder sb = new StringBuilder("(  REPLACE(attendeeEmail, '.', '') = REPLACE(?, '.', '') COLLATE NOCASE");
        for (int i = 1; i < count; i++) {
            sb.append(" OR  REPLACE(attendeeEmail, '.', '') = REPLACE(?, '.', '') COLLATE NOCASE");
        }
        return sb.append(")").toString();
    }

    private List<ContactInteraction> getInteractionsFromEventsCursor(Cursor cursor) {
        ?? arrayList;
        if (cursor != null) {
            try {
                if (cursor.getCount() == 0) {
                    arrayList = Collections.emptyList();
                } else {
                    Set<String> uniqueUris = new HashSet<>();
                    arrayList = new ArrayList();
                    while (cursor.moveToNext()) {
                        ContentValues values = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(cursor, values);
                        CalendarInteraction calendarInteraction = new CalendarInteraction(values);
                        if (!uniqueUris.contains(calendarInteraction.getIntent().getData().toString())) {
                            uniqueUris.add(calendarInteraction.getIntent().getData().toString());
                            arrayList.add(calendarInteraction);
                        }
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return arrayList;
    }

    private List<String> getOwnedCalendarIds() {
        String[] projection = {"_id", "calendar_access_level"};
        Cursor cursor = getContext().getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, projection, "visible = 1 AND calendar_access_level = ? ", new String[]{String.valueOf(700)}, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() >= 1) {
                    cursor.moveToPosition(-1);
                    List<String> calendarIds = new ArrayList<>(cursor.getCount());
                    while (cursor.moveToNext()) {
                        calendarIds.add(String.valueOf(cursor.getInt(0)));
                    }
                    if (cursor == null) {
                        return calendarIds;
                    }
                    cursor.close();
                    return calendarIds;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
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
    public void deliverResult(List<ContactInteraction> data) {
        this.mData = data;
        if (isStarted()) {
            super.deliverResult(data);
        }
    }
}
