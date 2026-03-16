package com.android.providers.contacts;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.UserManager;
import android.provider.CallLog;
import android.text.TextUtils;
import android.util.Log;
import com.android.providers.contacts.util.DbQueryUtils;
import com.android.providers.contacts.util.SelectionBuilder;
import com.android.providers.contacts.util.UserUtils;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class CallLogProvider extends ContentProvider {
    private static final Integer VOICEMAIL_TYPE;
    private static final HashMap<String, String> sCallsProjectionMap;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private CallLogInsertionHelper mCallLogInsertionHelper;
    private DatabaseUtils.InsertHelper mCallsInserter;
    private ContactsDatabaseHelper mDbHelper;
    private volatile CountDownLatch mReadAccessLatch;
    private boolean mUseStrictPhoneNumberComparation;
    private VoicemailPermissions mVoicemailPermissions;
    private static final String TAG = CallLogProvider.class.getSimpleName();
    private static final String EXCLUDE_VOICEMAIL_SELECTION = DbQueryUtils.getInequalityClause("type", 4);
    static final String[] CALL_LOG_SYNC_PROJECTION = {"number", "presentation", "type", "features", "date", "duration", "data_usage", "subscription_component_name", "subscription_id"};
    private static final UriMatcher sURIMatcher = new UriMatcher(-1);

    static {
        sURIMatcher.addURI("call_log", "calls", 1);
        sURIMatcher.addURI("call_log", "calls/#", 2);
        sURIMatcher.addURI("call_log", "calls/filter/*", 3);
        sCallsProjectionMap = new HashMap<>();
        sCallsProjectionMap.put("_id", "_id");
        sCallsProjectionMap.put("number", "number");
        sCallsProjectionMap.put("presentation", "presentation");
        sCallsProjectionMap.put("date", "date");
        sCallsProjectionMap.put("duration", "duration");
        sCallsProjectionMap.put("data_usage", "data_usage");
        sCallsProjectionMap.put("type", "type");
        sCallsProjectionMap.put("features", "features");
        sCallsProjectionMap.put("subscription_component_name", "subscription_component_name");
        sCallsProjectionMap.put("subscription_id", "subscription_id");
        sCallsProjectionMap.put("new", "new");
        sCallsProjectionMap.put("voicemail_uri", "voicemail_uri");
        sCallsProjectionMap.put("transcription", "transcription");
        sCallsProjectionMap.put("is_read", "is_read");
        sCallsProjectionMap.put("name", "name");
        sCallsProjectionMap.put("numbertype", "numbertype");
        sCallsProjectionMap.put("numberlabel", "numberlabel");
        sCallsProjectionMap.put("countryiso", "countryiso");
        sCallsProjectionMap.put("geocoded_location", "geocoded_location");
        sCallsProjectionMap.put("lookup_uri", "lookup_uri");
        sCallsProjectionMap.put("matched_number", "matched_number");
        sCallsProjectionMap.put("normalized_number", "normalized_number");
        sCallsProjectionMap.put("photo_id", "photo_id");
        sCallsProjectionMap.put("formatted_number", "formatted_number");
        VOICEMAIL_TYPE = new Integer(4);
    }

    @Override
    public boolean onCreate() {
        setAppOps(6, 7);
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "CallLogProvider.onCreate start");
        }
        Context context = getContext();
        this.mDbHelper = getDatabaseHelper(context);
        this.mUseStrictPhoneNumberComparation = context.getResources().getBoolean(android.R.^attr-private.defaultQueryHint);
        this.mVoicemailPermissions = new VoicemailPermissions(context);
        this.mCallLogInsertionHelper = createCallLogInsertionHelper(context);
        this.mBackgroundThread = new HandlerThread("CallLogProviderWorker", 10);
        this.mBackgroundThread.start();
        this.mBackgroundHandler = new Handler(this.mBackgroundThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                CallLogProvider.this.performBackgroundTask(msg.what);
            }
        };
        this.mReadAccessLatch = new CountDownLatch(1);
        scheduleBackgroundTask(0);
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "CallLogProvider.onCreate finish");
        }
        return true;
    }

    protected CallLogInsertionHelper createCallLogInsertionHelper(Context context) {
        return DefaultCallLogInsertionHelper.getInstance(context);
    }

    protected ContactsDatabaseHelper getDatabaseHelper(Context context) {
        return ContactsDatabaseHelper.getInstance(context);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        waitForAccess(this.mReadAccessLatch);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("calls");
        qb.setProjectionMap(sCallsProjectionMap);
        qb.setStrict(true);
        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        checkVoicemailPermissionAndAddRestriction(uri, selectionBuilder, true);
        int match = sURIMatcher.match(uri);
        switch (match) {
            case 1:
                break;
            case 2:
                selectionBuilder.addClause(DbQueryUtils.getEqualityClause("_id", parseCallIdFromUri(uri)));
                break;
            case 3:
                List<String> pathSegments = uri.getPathSegments();
                String phoneNumber = pathSegments.size() >= 2 ? pathSegments.get(2) : null;
                if (!TextUtils.isEmpty(phoneNumber)) {
                    qb.appendWhere("PHONE_NUMBERS_EQUAL(number, ");
                    qb.appendWhereEscapeString(phoneNumber);
                    qb.appendWhere(this.mUseStrictPhoneNumberComparation ? ", 1)" : ", 0)");
                } else {
                    qb.appendWhere("presentation!=1");
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
        int limit = getIntParam(uri, "limit", 0);
        int offset = getIntParam(uri, "offset", 0);
        String limitClause = null;
        if (limit > 0) {
            limitClause = offset + "," + limit;
        }
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selectionBuilder.build(), selectionArgs, null, null, sortOrder, limitClause);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), CallLog.CONTENT_URI);
        }
        return c;
    }

    private int getIntParam(Uri uri, String key, int defaultValue) {
        String valueString = uri.getQueryParameter(key);
        if (valueString == null) {
            return defaultValue;
        }
        try {
            int defaultValue2 = Integer.parseInt(valueString);
            return defaultValue2;
        } catch (NumberFormatException e) {
            String msg = "Integer required for " + key + " parameter but value '" + valueString + "' was found instead.";
            throw new IllegalArgumentException(msg, e);
        }
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match) {
            case 1:
                return "vnd.android.cursor.dir/calls";
            case 2:
                return "vnd.android.cursor.item/calls";
            case 3:
                return "vnd.android.cursor.dir/calls";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        waitForAccess(this.mReadAccessLatch);
        DbQueryUtils.checkForSupportedColumns(sCallsProjectionMap, values);
        if (hasVoicemailValue(values)) {
            checkIsAllowVoicemailRequest(uri);
            this.mVoicemailPermissions.checkCallerHasWriteAccess();
        }
        if (this.mCallsInserter == null) {
            SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
            this.mCallsInserter = new DatabaseUtils.InsertHelper(db, "calls");
        }
        ContentValues copiedValues = new ContentValues(values);
        this.mCallLogInsertionHelper.addComputedValues(copiedValues);
        long rowId = getDatabaseModifier(this.mCallsInserter).insert(copiedValues);
        if (rowId > 0) {
            return ContentUris.withAppendedId(uri, rowId);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        waitForAccess(this.mReadAccessLatch);
        DbQueryUtils.checkForSupportedColumns(sCallsProjectionMap, values);
        if (hasVoicemailValue(values)) {
            checkIsAllowVoicemailRequest(uri);
        }
        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        checkVoicemailPermissionAndAddRestriction(uri, selectionBuilder, false);
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        int matchedUriId = sURIMatcher.match(uri);
        switch (matchedUriId) {
            case 1:
                break;
            case 2:
                selectionBuilder.addClause(DbQueryUtils.getEqualityClause("_id", parseCallIdFromUri(uri)));
                break;
            default:
                throw new UnsupportedOperationException("Cannot update URL: " + uri);
        }
        return getDatabaseModifier(db).update("calls", values, selectionBuilder.build(), selectionArgs);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        waitForAccess(this.mReadAccessLatch);
        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        checkVoicemailPermissionAndAddRestriction(uri, selectionBuilder, false);
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        int matchedUriId = sURIMatcher.match(uri);
        switch (matchedUriId) {
            case 1:
                return getDatabaseModifier(db).delete("calls", selectionBuilder.build(), selectionArgs);
            default:
                throw new UnsupportedOperationException("Cannot delete that URL: " + uri);
        }
    }

    protected Context context() {
        return getContext();
    }

    private DatabaseModifier getDatabaseModifier(SQLiteDatabase db) {
        return new DbModifierWithNotification("calls", db, context());
    }

    private DatabaseModifier getDatabaseModifier(DatabaseUtils.InsertHelper insertHelper) {
        return new DbModifierWithNotification("calls", insertHelper, context());
    }

    private boolean hasVoicemailValue(ContentValues values) {
        return VOICEMAIL_TYPE.equals(values.getAsInteger("type"));
    }

    private void checkVoicemailPermissionAndAddRestriction(Uri uri, SelectionBuilder selectionBuilder, boolean isQuery) {
        if (isAllowVoicemailRequest(uri)) {
            if (isQuery) {
                this.mVoicemailPermissions.checkCallerHasReadAccess();
                return;
            } else {
                this.mVoicemailPermissions.checkCallerHasWriteAccess();
                return;
            }
        }
        selectionBuilder.addClause(EXCLUDE_VOICEMAIL_SELECTION);
    }

    private boolean isAllowVoicemailRequest(Uri uri) {
        return uri.getBooleanQueryParameter("allow_voicemails", false);
    }

    private void checkIsAllowVoicemailRequest(Uri uri) {
        if (!isAllowVoicemailRequest(uri)) {
            throw new IllegalArgumentException(String.format("Uri %s cannot be used for voicemail record. Please set '%s=true' in the uri.", uri, "allow_voicemails"));
        }
    }

    private long parseCallIdFromUri(Uri uri) {
        try {
            return Long.parseLong(uri.getPathSegments().get(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid call id in uri: " + uri, e);
        }
    }

    private void syncEntriesFromPrimaryUser(UserManager userManager) {
        int userHandle = userManager.getUserHandle();
        if (userHandle != 0 && !userManager.getUserInfo(userHandle).isManagedProfile()) {
            long lastSyncTime = getLastSyncTime();
            Uri uri = ContentProvider.maybeAddUserId(CallLog.Calls.CONTENT_URI, 0);
            Cursor cursor = getContext().getContentResolver().query(uri, CALL_LOG_SYNC_PROJECTION, EXCLUDE_VOICEMAIL_SELECTION + " AND date> ?", new String[]{String.valueOf(lastSyncTime)}, "date DESC");
            if (cursor != null) {
                try {
                    long lastSyncedEntryTime = copyEntriesFromCursor(cursor);
                    if (lastSyncedEntryTime > lastSyncTime) {
                        setLastTimeSynced(lastSyncedEntryTime);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }

    long copyEntriesFromCursor(Cursor cursor) {
        long lastSynced = 0;
        ContentValues values = new ContentValues();
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            String[] args = new String[2];
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                values.clear();
                DatabaseUtils.cursorRowToContentValues(cursor, values);
                String startTime = values.getAsString("date");
                String number = values.getAsString("number");
                if (startTime != null && number != null) {
                    if (cursor.isLast()) {
                        try {
                            lastSynced = Long.valueOf(startTime).longValue();
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Call log entry does not contain valid start time: " + startTime);
                        }
                    }
                    args[0] = startTime;
                    args[1] = number;
                    if (DatabaseUtils.queryNumEntries(db, "calls", "date = ? AND number = ?", args) <= 0) {
                        db.insert("calls", null, values);
                    }
                }
            }
            db.setTransactionSuccessful();
            return lastSynced;
        } finally {
            db.endTransaction();
        }
    }

    private long getLastSyncTime() {
        try {
            return Long.valueOf(this.mDbHelper.getProperty("call_log_last_synced", "0")).longValue();
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void setLastTimeSynced(long time) {
        this.mDbHelper.setProperty("call_log_last_synced", String.valueOf(time));
    }

    private static void waitForAccess(CountDownLatch latch) {
        if (latch == null) {
            return;
        }
        while (true) {
            try {
                latch.await();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void scheduleBackgroundTask(int task) {
        this.mBackgroundHandler.sendEmptyMessage(task);
    }

    private void performBackgroundTask(int task) {
        UserManager userManager;
        if (task == 0) {
            try {
                Context context = getContext();
                if (context != null && (userManager = UserUtils.getUserManager(context)) != null && !userManager.hasUserRestriction("no_outgoing_calls")) {
                    syncEntriesFromPrimaryUser(userManager);
                }
            } finally {
                this.mReadAccessLatch.countDown();
                this.mReadAccessLatch = null;
            }
        }
    }
}
