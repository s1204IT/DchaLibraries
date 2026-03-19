package com.android.server.notification;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.service.notification.ZenModeConfig;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Objects;

public class CalendarTracker {
    private static final String ATTENDEE_SELECTION = "event_id = ? AND attendeeEmail = ?";
    private static final boolean DEBUG_ATTENDEES = false;
    private static final int EVENT_CHECK_LOOKAHEAD = 86400000;
    private static final String INSTANCE_ORDER_BY = "begin ASC";
    private static final String TAG = "ConditionProviders.CT";
    private Callback mCallback;
    private final ContentObserver mObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean z, Uri uri) {
            if (CalendarTracker.DEBUG) {
                Log.d(CalendarTracker.TAG, "onChange selfChange=" + z + " uri=" + uri + " u=" + CalendarTracker.this.mUserContext.getUserId());
            }
            CalendarTracker.this.mCallback.onChanged();
        }

        @Override
        public void onChange(boolean z) {
            if (CalendarTracker.DEBUG) {
                Log.d(CalendarTracker.TAG, "onChange selfChange=" + z);
            }
        }
    };
    private boolean mRegistered;
    private final Context mSystemContext;
    private final Context mUserContext;
    private static final boolean DEBUG = Log.isLoggable("ConditionProviders", 3);
    private static final String[] INSTANCE_PROJECTION = {"begin", "end", "title", "visible", "event_id", "calendar_displayName", "ownerAccount", "calendar_id", "availability"};
    private static final String[] ATTENDEE_PROJECTION = {"event_id", "attendeeEmail", "attendeeStatus"};

    public interface Callback {
        void onChanged();
    }

    public static class CheckEventResult {
        public boolean inEvent;
        public long recheckAt;
    }

    public CalendarTracker(Context context, Context context2) {
        this.mSystemContext = context;
        this.mUserContext = context2;
    }

    public void setCallback(Callback callback) {
        if (this.mCallback == callback) {
            return;
        }
        this.mCallback = callback;
        setRegistered(this.mCallback != null);
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("mCallback=");
        printWriter.println(this.mCallback);
        printWriter.print(str);
        printWriter.print("mRegistered=");
        printWriter.println(this.mRegistered);
        printWriter.print(str);
        printWriter.print("u=");
        printWriter.println(this.mUserContext.getUserId());
    }

    private ArraySet<Long> getPrimaryCalendars() throws Throwable {
        long jCurrentTimeMillis = System.currentTimeMillis();
        ArraySet<Long> arraySet = new ArraySet<>();
        Cursor cursor = null;
        try {
            Cursor cursorQuery = this.mUserContext.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, new String[]{"_id", "(account_name=ownerAccount) AS \"primary\""}, "\"primary\" = 1", null, null);
            while (cursorQuery != null) {
                try {
                    if (!cursorQuery.moveToNext()) {
                        break;
                    }
                    arraySet.add(Long.valueOf(cursorQuery.getLong(0)));
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            if (DEBUG) {
                Log.d(TAG, "getPrimaryCalendars took " + (System.currentTimeMillis() - jCurrentTimeMillis));
            }
            return arraySet;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public CheckEventResult checkEvent(ZenModeConfig.EventInfo eventInfo, long j) throws Throwable {
        Cursor cursor;
        CheckEventResult checkEventResult;
        Cursor cursor2;
        Cursor cursor3;
        ArraySet<Long> arraySet;
        boolean z;
        ZenModeConfig.EventInfo eventInfo2;
        boolean z2;
        CalendarTracker calendarTracker;
        Uri.Builder builderBuildUpon = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builderBuildUpon, j);
        long j2 = 86400000 + j;
        ContentUris.appendId(builderBuildUpon, j2);
        Cursor cursorQuery = this.mUserContext.getContentResolver().query(builderBuildUpon.build(), INSTANCE_PROJECTION, null, null, INSTANCE_ORDER_BY);
        CheckEventResult checkEventResult2 = new CheckEventResult();
        checkEventResult2.recheckAt = j2;
        try {
            try {
                ArraySet<Long> primaryCalendars = getPrimaryCalendars();
                while (cursorQuery != null) {
                    try {
                        if (!cursorQuery.moveToNext()) {
                            break;
                        }
                        long j3 = cursorQuery.getLong(0);
                        long j4 = cursorQuery.getLong(1);
                        String string = cursorQuery.getString(2);
                        boolean z3 = cursorQuery.getInt(3) == 1;
                        int i = cursorQuery.getInt(4);
                        String string2 = cursorQuery.getString(5);
                        CheckEventResult checkEventResult3 = checkEventResult2;
                        try {
                            String string3 = cursorQuery.getString(6);
                            long j5 = cursorQuery.getLong(7);
                            int i2 = cursorQuery.getInt(8);
                            boolean zContains = primaryCalendars.contains(Long.valueOf(j5));
                            if (DEBUG) {
                                arraySet = primaryCalendars;
                                cursor3 = cursorQuery;
                                try {
                                    try {
                                        z = false;
                                        Log.d(TAG, String.format("%s %s-%s v=%s a=%s eid=%s n=%s o=%s cid=%s p=%s", string, new Date(j3), new Date(j4), Boolean.valueOf(z3), availabilityToString(i2), Integer.valueOf(i), string2, string3, Long.valueOf(j5), Boolean.valueOf(zContains)));
                                    } catch (Exception e) {
                                        e = e;
                                        checkEventResult = checkEventResult3;
                                        cursor = cursor3;
                                        try {
                                            Slog.w(TAG, "error reading calendar", e);
                                            if (cursor != null) {
                                                cursor.close();
                                            }
                                            return checkEventResult;
                                        } catch (Throwable th) {
                                            th = th;
                                            if (cursor != null) {
                                                cursor.close();
                                            }
                                            throw th;
                                        }
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                    cursor = cursor3;
                                    if (cursor != null) {
                                    }
                                    throw th;
                                }
                            } else {
                                cursor3 = cursorQuery;
                                arraySet = primaryCalendars;
                                z = false;
                            }
                            boolean z4 = (j < j3 || j >= j4) ? z : true;
                            if (z3 && zContains) {
                                eventInfo2 = eventInfo;
                                if (eventInfo2.calendar == null || Objects.equals(eventInfo2.calendar, string3) || Objects.equals(eventInfo2.calendar, string2)) {
                                    z2 = true;
                                }
                                if (i2 != 1) {
                                    z = true;
                                }
                                if (z2 || !z) {
                                    checkEventResult = checkEventResult3;
                                    calendarTracker = this;
                                } else {
                                    if (DEBUG) {
                                        Log.d(TAG, "  MEETS CALENDAR & AVAILABILITY");
                                    }
                                    calendarTracker = this;
                                    if (calendarTracker.meetsAttendee(eventInfo2, i, string3)) {
                                        if (DEBUG) {
                                            Log.d(TAG, "    MEETS ATTENDEE");
                                        }
                                        if (z4) {
                                            if (DEBUG) {
                                                Log.d(TAG, "      MEETS TIME");
                                            }
                                            checkEventResult = checkEventResult3;
                                            try {
                                                checkEventResult.inEvent = true;
                                            } catch (Exception e2) {
                                                e = e2;
                                                cursor = cursor3;
                                                Slog.w(TAG, "error reading calendar", e);
                                                if (cursor != null) {
                                                }
                                                return checkEventResult;
                                            }
                                        } else {
                                            checkEventResult = checkEventResult3;
                                        }
                                        if (j3 > j && j3 < checkEventResult.recheckAt) {
                                            checkEventResult.recheckAt = j3;
                                        } else if (j4 > j && j4 < checkEventResult.recheckAt) {
                                            checkEventResult.recheckAt = j4;
                                        }
                                    } else {
                                        checkEventResult = checkEventResult3;
                                    }
                                }
                                checkEventResult2 = checkEventResult;
                                primaryCalendars = arraySet;
                                cursorQuery = cursor3;
                            } else {
                                eventInfo2 = eventInfo;
                            }
                            z2 = z;
                            if (i2 != 1) {
                            }
                            if (z2) {
                                checkEventResult = checkEventResult3;
                                calendarTracker = this;
                            }
                            checkEventResult2 = checkEventResult;
                            primaryCalendars = arraySet;
                            cursorQuery = cursor3;
                        } catch (Exception e3) {
                            e = e3;
                            cursor3 = cursorQuery;
                        }
                    } catch (Exception e4) {
                        e = e4;
                        cursor3 = cursorQuery;
                        checkEventResult = checkEventResult2;
                    }
                }
                cursor2 = cursorQuery;
                checkEventResult = checkEventResult2;
            } catch (Throwable th3) {
                th = th3;
                cursor = cursorQuery;
            }
        } catch (Exception e5) {
            e = e5;
            cursor = cursorQuery;
            checkEventResult = checkEventResult2;
        }
        if (cursor2 != null) {
            cursor = cursor2;
            cursor.close();
        }
        return checkEventResult;
    }

    private boolean meetsAttendee(ZenModeConfig.EventInfo eventInfo, int i, String str) {
        int i2;
        long jCurrentTimeMillis = System.currentTimeMillis();
        int i3 = 0;
        int i4 = 1;
        Cursor cursorQuery = this.mUserContext.getContentResolver().query(CalendarContract.Attendees.CONTENT_URI, ATTENDEE_PROJECTION, ATTENDEE_SELECTION, new String[]{Integer.toString(i), str}, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() != 0) {
                    boolean z = 0;
                    while (cursorQuery != null && cursorQuery.moveToNext()) {
                        long j = cursorQuery.getLong(i3);
                        String string = cursorQuery.getString(i4);
                        int i5 = cursorQuery.getInt(2);
                        boolean zMeetsReply = meetsReply(eventInfo.reply, i5);
                        if (DEBUG) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                            i2 = 0;
                            sb.append(String.format("status=%s, meetsReply=%s", attendeeStatusToString(i5), Boolean.valueOf(zMeetsReply)));
                            Log.d(TAG, sb.toString());
                        } else {
                            i2 = i3;
                        }
                        i3 = i2;
                        i4 = 1;
                        z |= (j == ((long) i) && Objects.equals(string, str) && zMeetsReply) ? 1 : i2;
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "meetsAttendee took " + (System.currentTimeMillis() - jCurrentTimeMillis));
                    }
                    return z;
                }
            } catch (Throwable th) {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                if (DEBUG) {
                    Log.d(TAG, "meetsAttendee took " + (System.currentTimeMillis() - jCurrentTimeMillis));
                }
                throw th;
            }
        }
        if (DEBUG) {
            Log.d(TAG, "No attendees found");
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        if (!DEBUG) {
            return true;
        }
        Log.d(TAG, "meetsAttendee took " + (System.currentTimeMillis() - jCurrentTimeMillis));
        return true;
    }

    private void setRegistered(boolean z) {
        if (this.mRegistered == z) {
            return;
        }
        ContentResolver contentResolver = this.mSystemContext.getContentResolver();
        int userId = this.mUserContext.getUserId();
        if (this.mRegistered) {
            if (DEBUG) {
                Log.d(TAG, "unregister content observer u=" + userId);
            }
            contentResolver.unregisterContentObserver(this.mObserver);
        }
        this.mRegistered = z;
        if (DEBUG) {
            Log.d(TAG, "mRegistered = " + z + " u=" + userId);
        }
        if (this.mRegistered) {
            if (DEBUG) {
                Log.d(TAG, "register content observer u=" + userId);
            }
            contentResolver.registerContentObserver(CalendarContract.Instances.CONTENT_URI, true, this.mObserver, userId);
            contentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, this.mObserver, userId);
            contentResolver.registerContentObserver(CalendarContract.Calendars.CONTENT_URI, true, this.mObserver, userId);
        }
    }

    private static String attendeeStatusToString(int i) {
        switch (i) {
            case 0:
                return "ATTENDEE_STATUS_NONE";
            case 1:
                return "ATTENDEE_STATUS_ACCEPTED";
            case 2:
                return "ATTENDEE_STATUS_DECLINED";
            case 3:
                return "ATTENDEE_STATUS_INVITED";
            case 4:
                return "ATTENDEE_STATUS_TENTATIVE";
            default:
                return "ATTENDEE_STATUS_UNKNOWN_" + i;
        }
    }

    private static String availabilityToString(int i) {
        switch (i) {
            case 0:
                return "AVAILABILITY_BUSY";
            case 1:
                return "AVAILABILITY_FREE";
            case 2:
                return "AVAILABILITY_TENTATIVE";
            default:
                return "AVAILABILITY_UNKNOWN_" + i;
        }
    }

    private static boolean meetsReply(int i, int i2) {
        switch (i) {
            case 0:
                return i2 != 2;
            case 1:
                return i2 == 1 || i2 == 4;
            case 2:
                return i2 == 1;
            default:
                return false;
        }
    }
}
