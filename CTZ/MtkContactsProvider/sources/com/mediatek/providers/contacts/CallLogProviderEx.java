package com.mediatek.providers.contacts;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.UserManager;
import android.provider.CallLog;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.android.providers.contacts.CallLogDatabaseHelper;
import com.android.providers.contacts.ContactsProvider2;
import com.android.providers.contacts.DatabaseModifier;
import com.android.providers.contacts.DbModifierWithNotification;
import com.android.providers.contacts.VoicemailPermissions;
import com.android.providers.contacts.util.UserUtils;

public class CallLogProviderEx {
    private static CallLogProviderEx sCallLogProviderEx;
    private CallLogSearchSupport mCallLogSearchSupport;
    private final Context mContext;
    private VoicemailPermissions mVoicemailPermissions;
    private static final String TAG = CallLogProviderEx.class.getSimpleName();
    private static final UriMatcher sURIMatcher = new UriMatcher(-1);

    static {
        sURIMatcher.addURI("call_log", "calls/search_filter/*", 4);
        sURIMatcher.addURI("call_log", "search_suggest_query", 10001);
        sURIMatcher.addURI("call_log", "search_suggest_query/*", 10001);
        sURIMatcher.addURI("call_log", "search_suggest_shortcut/*", 10002);
        sURIMatcher.addURI("call_log", "conference_calls", 5);
        sURIMatcher.addURI("call_log", "conference_calls/#", 6);
    }

    private CallLogProviderEx(Context context) {
        this.mContext = context;
    }

    public static synchronized CallLogProviderEx getInstance(Context context) {
        if (sCallLogProviderEx == null) {
            sCallLogProviderEx = new CallLogProviderEx(context);
            sCallLogProviderEx.initialize();
        }
        return sCallLogProviderEx;
    }

    private void initialize() {
        this.mVoicemailPermissions = new VoicemailPermissions(this.mContext);
        this.mCallLogSearchSupport = new CallLogSearchSupport(this.mContext);
    }

    public Cursor queryCallLog(SQLiteDatabase sQLiteDatabase, SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String[] strArr3;
        String strStripSeparators;
        String str3;
        int intParam;
        String str4;
        Cursor cursorQuery;
        int iMatch = sURIMatcher.match(uri);
        Log.d(TAG, "queryCallLog match == " + iMatch);
        switch (iMatch) {
            case 4:
                strArr3 = strArr;
                String str5 = uri.getPathSegments().get(2);
                if (!TextUtils.isEmpty(str5) && ContactsProvider2.countPhoneNumberDigits(str5) > 0) {
                    strStripSeparators = PhoneNumberUtils.stripSeparators(str5);
                } else {
                    strStripSeparators = str5;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("number GLOB '*" + strStripSeparators + "*'");
                sb.append(" OR (name GLOB '*" + str5 + "*')");
                sQLiteQueryBuilder.appendWhere(sb);
                Log.d(TAG, " CALLS_SEARCH_FILTER query=" + str5 + ", sb=" + sb.toString());
                str3 = "_id";
                intParam = getIntParam(uri, "limit", 0);
                int intParam2 = getIntParam(uri, "offset", 0);
                if (intParam > 0) {
                    str4 = null;
                } else {
                    str4 = intParam2 + "," + intParam;
                }
                cursorQuery = sQLiteQueryBuilder.query(sQLiteDatabase, strArr3, str, strArr2, str3, null, str2, str4);
                if (cursorQuery != null) {
                    cursorQuery.setNotificationUri(this.mContext.getContentResolver(), CallLog.CONTENT_URI);
                    Log.d(TAG, "queryCallLog count == " + cursorQuery.getCount());
                }
                return cursorQuery;
            case 5:
                strArr3 = strArr;
                Log.d(TAG, "CONFERENCE_CALLS");
                sQLiteQueryBuilder.setTables("conference_calls");
                sQLiteQueryBuilder.setProjectionMap(null);
                str3 = null;
                intParam = getIntParam(uri, "limit", 0);
                int intParam22 = getIntParam(uri, "offset", 0);
                if (intParam > 0) {
                }
                cursorQuery = sQLiteQueryBuilder.query(sQLiteDatabase, strArr3, str, strArr2, str3, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 6:
                strArr3 = strArr;
                Log.d(TAG, "CONFERENCE_CALLS_ID. Uri:" + uri);
                sQLiteQueryBuilder.appendWhere("conference_call_id=" + ContentUris.parseId(uri));
                str3 = null;
                intParam = getIntParam(uri, "limit", 0);
                int intParam222 = getIntParam(uri, "offset", 0);
                if (intParam > 0) {
                }
                cursorQuery = sQLiteQueryBuilder.query(sQLiteDatabase, strArr3, str, strArr2, str3, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            default:
                switch (iMatch) {
                    case 10001:
                        Log.d(TAG, "SEARCH_SUGGESTIONS");
                        return this.mCallLogSearchSupport.handleSearchSuggestionsQuery(sQLiteDatabase, uri, getLimit(uri));
                    case 10002:
                        Log.d(TAG, "SEARCH_SHORTCUT. Uri:" + uri);
                        return this.mCallLogSearchSupport.handleSearchShortcutRefresh(sQLiteDatabase, strArr, uri.getLastPathSegment(), uri.getQueryParameter("suggest_intent_extra_data"));
                    default:
                        strArr3 = strArr;
                        break;
                }
                str3 = null;
                intParam = getIntParam(uri, "limit", 0);
                int intParam2222 = getIntParam(uri, "offset", 0);
                if (intParam > 0) {
                }
                cursorQuery = sQLiteQueryBuilder.query(sQLiteDatabase, strArr3, str, strArr2, str3, null, str2, str4);
                if (cursorQuery != null) {
                }
                return cursorQuery;
        }
    }

    public Uri insertConferenceCall(SQLiteDatabase sQLiteDatabase, Uri uri, ContentValues contentValues) {
        if (5 != sURIMatcher.match(uri)) {
            return null;
        }
        long jInsert = sQLiteDatabase.insert("conference_calls", "group_id", contentValues);
        if (jInsert < 0) {
            Log.w(TAG, "Insert Conference Call Failed, Uri:" + uri);
            return null;
        }
        return ContentUris.withAppendedId(uri, jInsert);
    }

    public int deleteConferenceCalls(SQLiteDatabase sQLiteDatabase, Uri uri, String str, String[] strArr) {
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables("conference_calls");
        Cursor cursorQuery = sQLiteQueryBuilder.query(sQLiteDatabase, new String[]{"_id"}, str, strArr, null, null, null, null);
        StringBuilder sb = new StringBuilder();
        while (cursorQuery.moveToNext()) {
            sb.append(cursorQuery.getString(0));
            sb.append(",");
        }
        try {
            sQLiteDatabase.beginTransaction();
            int iDelete = getDatabaseModifier(sQLiteDatabase).delete("conference_calls", str, strArr);
            if (sb.length() > 0) {
                sb.replace(sb.length() - 1, sb.length(), "");
                String str2 = "conference_call_id IN (" + sb.toString() + ")";
                int iDelete2 = sQLiteDatabase.delete("calls", str2, null);
                Log.d(TAG, "[deleteConferenceCalls] deleteSelection=" + str2 + "; delete related Calls count=" + iDelete2);
            }
            sQLiteDatabase.setTransactionSuccessful();
            Log.d(TAG, "[deleteConferenceCalls] delete Conference Calls. count: " + iDelete);
            return iDelete;
        } finally {
            sQLiteDatabase.endTransaction();
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    private DatabaseModifier getDatabaseModifier(SQLiteDatabase sQLiteDatabase) {
        return new DbModifierWithNotification("calls", sQLiteDatabase, this.mContext);
    }

    public String getLimit(Uri uri) {
        String queryParameter = uri.getQueryParameter("limit");
        if (queryParameter == null) {
            return null;
        }
        try {
            int i = Integer.parseInt(queryParameter);
            if (i < 0) {
                Log.w(TAG, "Invalid limit parameter: " + queryParameter);
                return null;
            }
            return String.valueOf(i);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid limit parameter: " + queryParameter);
            return null;
        }
    }

    private int getIntParam(Uri uri, String str, int i) {
        String queryParameter = uri.getQueryParameter(str);
        if (queryParameter == null) {
            return i;
        }
        try {
            return Integer.parseInt(queryParameter);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Integer required for " + str + " parameter but value '" + queryParameter + "' was found instead.", e);
        }
    }

    public static void createConferenceCallsTable(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS conference_calls");
        sQLiteDatabase.execSQL("CREATE TABLE conference_calls (_id INTEGER PRIMARY KEY AUTOINCREMENT,group_id INTEGER,conference_date INTEGER, conference_duration INTEGER DEFAULT -1 );");
    }

    public static void addDurationColumnIfNeed(SQLiteDatabase sQLiteDatabase) {
        try {
            sQLiteDatabase.rawQuery("SELECT conference_duration FROM conference_calls", null);
        } catch (SQLiteException e) {
            e.printStackTrace();
            if (e.toString().contains("no such column")) {
                sQLiteDatabase.execSQL("ALTER TABLE conference_calls ADD conference_duration INTEGER DEFAULT -1");
                Log.e(TAG, "add column conference_duration");
                updateConferenceDuration(sQLiteDatabase);
            }
        }
    }

    public static void updateConferenceDuration(SQLiteDatabase sQLiteDatabase) {
        Log.i(TAG, "updateConferenceDuration");
        sQLiteDatabase.execSQL("UPDATE conference_calls SET conference_duration = (SELECT MAX(duration) FROM calls WHERE calls.conference_call_id==conference_calls._id) WHERE EXISTS (SELECT MAX(duration) FROM calls WHERE calls.conference_call_id==conference_calls._id)");
    }

    public static void dropDialerSearchTables(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS dialer_search;");
        sQLiteDatabase.execSQL("DROP VIEW IF EXISTS view_dialer_search;");
    }

    public void removeDuplictedCallLogForUser(CallLogDatabaseHelper callLogDatabaseHelper, ContentValues contentValues) {
        SQLiteDatabase writableDatabase = callLogDatabaseHelper.getWritableDatabase();
        UserManager userManager = UserUtils.getUserManager(this.mContext);
        String[] strArr = {contentValues.getAsString("date"), contentValues.getAsString("number"), contentValues.getAsString("type")};
        if (userManager != null && userManager.getUserHandle() != 0 && DatabaseUtils.queryNumEntries(writableDatabase, "calls", "date = ? AND number = ? AND type = ?", strArr) > 0) {
            int iDelete = getDatabaseModifier(writableDatabase).delete("calls", "date = ? AND number = ? AND type = ?", strArr);
            Log.i(TAG, "removeDuplictedCallLogForUser, delete count=" + iDelete);
        }
    }
}
