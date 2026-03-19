package com.android.providers.contacts;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.os.UserManager;
import android.provider.CallLog;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ProviderAccessStats;
import com.android.providers.contacts.util.DbQueryUtils;
import com.android.providers.contacts.util.SelectionBuilder;
import com.android.providers.contacts.util.UserUtils;
import com.mediatek.providers.contacts.CallLogProviderEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class CallLogProvider extends ContentProvider {

    @VisibleForTesting
    static final String PARAM_KEY_QUERY_FOR_TESTING = "query_for_testing";

    @VisibleForTesting
    static final String PARAM_KEY_SET_TIME_FOR_TESTING = "set_time_for_testing";
    private static final Integer VOICEMAIL_TYPE;
    private static final ArrayMap<String, String> sCallsProjectionMap;
    private static Long sTimeForTestMillis;
    private CallLogInsertionHelper mCallLogInsertionHelper;
    private CallLogProviderEx mCallLogProviderEx;
    private DatabaseUtils.InsertHelper mCallsInserter;
    private CallLogDatabaseHelper mDbHelper;
    private volatile CountDownLatch mReadAccessLatch;
    private ContactsTaskScheduler mTaskScheduler;
    private boolean mUseStrictPhoneNumberComparation;
    private VoicemailPermissions mVoicemailPermissions;
    public static final boolean VERBOSE_LOGGING = Log.isLoggable("CallLogProvider", 2);
    private static final String EXCLUDE_VOICEMAIL_SELECTION = DbQueryUtils.getInequalityClause("type", 4);
    private static final String EXCLUDE_HIDDEN_SELECTION = DbQueryUtils.getEqualityClause("phone_account_hidden", 0);

    @VisibleForTesting
    static final String[] CALL_LOG_SYNC_PROJECTION = {"number", "presentation", "type", "features", "date", "duration", "data_usage", "subscription_component_name", "subscription_id", "add_for_all_users", "countryiso", "voicemail_uri", "geocoded_location", "is_read", "post_dial_digits", "via_number", "conference_call_id"};
    static final String[] MINIMAL_PROJECTION = {"_id"};
    private static final UriMatcher sURIMatcher = new UriMatcher(-1);
    private final ThreadLocal<Boolean> mApplyingBatch = new ThreadLocal<>();
    private final ThreadLocal<Integer> mCallingUid = new ThreadLocal<>();
    private final ProviderAccessStats mStats = new ProviderAccessStats();

    static {
        sURIMatcher.addURI("call_log", "calls", 1);
        sURIMatcher.addURI("call_log", "calls/#", 2);
        sURIMatcher.addURI("call_log", "calls/filter/*", 3);
        sURIMatcher.addURI("call_log", "calls/search_filter/*", 4);
        sURIMatcher.addURI("call_log", "search_suggest_query", 10001);
        sURIMatcher.addURI("call_log", "search_suggest_query/*", 10001);
        sURIMatcher.addURI("call_log", "search_suggest_shortcut/*", 10002);
        sURIMatcher.addURI("call_log", "conference_calls", 5);
        sURIMatcher.addURI("call_log", "conference_calls/#", 6);
        sURIMatcher.addURI("call_log_shadow", "calls", 1);
        sCallsProjectionMap = new ArrayMap<>();
        sCallsProjectionMap.put("_id", "calls._id as _id");
        sCallsProjectionMap.put("number", "number");
        sCallsProjectionMap.put("post_dial_digits", "post_dial_digits");
        sCallsProjectionMap.put("via_number", "via_number");
        sCallsProjectionMap.put("presentation", "presentation");
        sCallsProjectionMap.put("date", "date");
        sCallsProjectionMap.put("duration", "duration");
        sCallsProjectionMap.put("data_usage", "data_usage");
        sCallsProjectionMap.put("type", "type");
        sCallsProjectionMap.put("features", "features");
        sCallsProjectionMap.put("subscription_component_name", "subscription_component_name");
        sCallsProjectionMap.put("subscription_id", "subscription_id");
        sCallsProjectionMap.put("phone_account_address", "phone_account_address");
        sCallsProjectionMap.put("new", "new");
        sCallsProjectionMap.put("voicemail_uri", "voicemail_uri");
        sCallsProjectionMap.put("transcription", "transcription");
        sCallsProjectionMap.put("transcription_state", "transcription_state");
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
        sCallsProjectionMap.put("photo_uri", "photo_uri");
        sCallsProjectionMap.put("formatted_number", "formatted_number");
        sCallsProjectionMap.put("add_for_all_users", "add_for_all_users");
        sCallsProjectionMap.put("last_modified", "last_modified");
        sCallsProjectionMap.put("conference_call_id", "conference_call_id");
        sCallsProjectionMap.put("indicate_phone_or_sim_contact", "indicate_phone_or_sim_contact");
        sCallsProjectionMap.put("is_sdn_contact", "is_sdn_contact");
        VOICEMAIL_TYPE = new Integer(4);
    }

    protected boolean isShadow() {
        return false;
    }

    protected final String getProviderName() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean onCreate() {
        if (VERBOSE_LOGGING) {
            Log.v("CallLogProvider", "onCreate: " + getClass().getSimpleName() + " user=" + Process.myUserHandle().getIdentifier());
        }
        setAppOps(6, 7);
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", getProviderName() + ".onCreate start");
        }
        Context context = getContext();
        this.mDbHelper = getDatabaseHelper(context);
        this.mUseStrictPhoneNumberComparation = context.getResources().getBoolean(android.R.^attr-private.pointerIconWait);
        this.mVoicemailPermissions = new VoicemailPermissions(context);
        this.mCallLogProviderEx = CallLogProviderEx.getInstance(context);
        this.mCallLogInsertionHelper = createCallLogInsertionHelper(context);
        this.mReadAccessLatch = new CountDownLatch(1);
        this.mTaskScheduler = new ContactsTaskScheduler(getClass().getSimpleName()) {
            @Override
            public void onPerformTask(int i, Object obj) {
                CallLogProvider.this.performBackgroundTask(i, obj);
            }
        };
        this.mTaskScheduler.scheduleTask(0, null);
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", getProviderName() + ".onCreate finish");
        }
        return true;
    }

    @VisibleForTesting
    protected CallLogInsertionHelper createCallLogInsertionHelper(Context context) {
        return DefaultCallLogInsertionHelper.getInstance(context);
    }

    protected CallLogDatabaseHelper getDatabaseHelper(Context context) {
        return CallLogDatabaseHelper.getInstance(context);
    }

    protected boolean applyingBatch() {
        Boolean bool = this.mApplyingBatch.get();
        return bool != null && bool.booleanValue();
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws OperationApplicationException {
        int callingUid = Binder.getCallingUid();
        this.mCallingUid.set(Integer.valueOf(callingUid));
        this.mStats.incrementBatchStats(callingUid);
        this.mApplyingBatch.set(true);
        try {
            return super.applyBatch(arrayList);
        } finally {
            this.mApplyingBatch.set(false);
            this.mStats.finishOperation(callingUid);
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        int callingUid = Binder.getCallingUid();
        this.mCallingUid.set(Integer.valueOf(callingUid));
        this.mStats.incrementBatchStats(callingUid);
        this.mApplyingBatch.set(true);
        try {
            return super.bulkInsert(uri, contentValuesArr);
        } finally {
            this.mApplyingBatch.set(false);
            this.mStats.finishOperation(callingUid);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        int callingUid = Binder.getCallingUid();
        this.mStats.incrementQueryStats(callingUid);
        try {
            return queryInternal(uri, strArr, str, strArr2, str2);
        } finally {
            this.mStats.finishOperation(callingUid);
        }
    }

    private Cursor queryInternal(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        int intParam;
        String str3;
        Cursor cursorQuery;
        if (VERBOSE_LOGGING) {
            Log.v("CallLogProvider", "query: uri=" + uri + "  projection=" + Arrays.toString(strArr) + "  selection=[" + str + "]  args=" + Arrays.toString(strArr2) + "  order=[" + str2 + "] CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
        }
        queryForTesting(uri);
        waitForAccess(this.mReadAccessLatch);
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables("calls");
        sQLiteQueryBuilder.setProjectionMap(sCallsProjectionMap);
        sQLiteQueryBuilder.setStrict(true);
        SelectionBuilder selectionBuilder = new SelectionBuilder(str);
        checkVoicemailPermissionAndAddRestriction(uri, selectionBuilder, true);
        selectionBuilder.addClause(EXCLUDE_HIDDEN_SELECTION);
        int iMatch = sURIMatcher.match(uri);
        SQLiteDatabase readableDatabase = this.mDbHelper.getReadableDatabase();
        Log.d("CallLogProvider", "match == " + iMatch);
        if (str2 != null && str2.contains("sort_date")) {
            if (iMatch == 1) {
                sQLiteQueryBuilder.setTables("calls LEFT JOIN conference_calls ON conference_call_id=conference_calls._id");
                str2 = str2.replace("sort_date", "(CASE WHEN conference_call_id>0 THEN conference_date ELSE date END)");
            } else {
                str2 = str2.replace("sort_date", "date");
            }
        }
        String str4 = str2;
        switch (iMatch) {
            case 1:
                Log.d("CallLogProvider", "In call log providers,  selectionBuilder=" + selectionBuilder.build());
                intParam = getIntParam(uri, "limit", 0);
                int intParam2 = getIntParam(uri, "offset", 0);
                if (intParam > 0) {
                    str3 = intParam2 + "," + intParam;
                } else {
                    str3 = null;
                }
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, selectionBuilder.build(), strArr2, null, null, str4, str3);
                if (cursorQuery != null) {
                    cursorQuery.setNotificationUri(getContext().getContentResolver(), CallLog.CONTENT_URI);
                    Log.d("CallLogProvider", "query count == " + cursorQuery.getCount());
                }
                return cursorQuery;
            case 2:
                selectionBuilder.addClause(DbQueryUtils.getEqualityClause("_id", parseCallIdFromUri(uri)));
                Log.d("CallLogProvider", "In call log providers,  selectionBuilder=" + selectionBuilder.build());
                intParam = getIntParam(uri, "limit", 0);
                int intParam22 = getIntParam(uri, "offset", 0);
                if (intParam > 0) {
                }
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, selectionBuilder.build(), strArr2, null, null, str4, str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 3:
                List<String> pathSegments = uri.getPathSegments();
                String str5 = pathSegments.size() >= 2 ? pathSegments.get(2) : null;
                if (!TextUtils.isEmpty(str5)) {
                    sQLiteQueryBuilder.appendWhere("PHONE_NUMBERS_EQUAL(number, ");
                    sQLiteQueryBuilder.appendWhereEscapeString(str5);
                    sQLiteQueryBuilder.appendWhere(this.mUseStrictPhoneNumberComparation ? ", 1)" : ", 0)");
                } else {
                    sQLiteQueryBuilder.appendWhere("presentation!=1");
                }
                Log.d("CallLogProvider", "In call log providers,  selectionBuilder=" + selectionBuilder.build());
                intParam = getIntParam(uri, "limit", 0);
                int intParam222 = getIntParam(uri, "offset", 0);
                if (intParam > 0) {
                }
                cursorQuery = sQLiteQueryBuilder.query(readableDatabase, strArr, selectionBuilder.build(), strArr2, null, null, str4, str3);
                if (cursorQuery != null) {
                }
                return cursorQuery;
            case 4:
            case 5:
            case 6:
                return this.mCallLogProviderEx.queryCallLog(readableDatabase, sQLiteQueryBuilder, uri, strArr, str, strArr2, str4);
            default:
                switch (iMatch) {
                    case 10001:
                    case 10002:
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown URL " + uri);
                }
                break;
        }
    }

    private void queryForTesting(Uri uri) {
        if (!uri.getBooleanQueryParameter(PARAM_KEY_QUERY_FOR_TESTING, false)) {
            return;
        }
        if (!getCallingPackage().equals("com.android.providers.contacts")) {
            throw new IllegalArgumentException("query_for_testing set from foreign package " + getCallingPackage());
        }
        String queryParameter = uri.getQueryParameter(PARAM_KEY_SET_TIME_FOR_TESTING);
        if (queryParameter != null) {
            if (queryParameter.equals("null")) {
                sTimeForTestMillis = null;
            } else {
                sTimeForTestMillis = Long.valueOf(Long.parseLong(queryParameter));
            }
        }
    }

    @VisibleForTesting
    static Long getTimeForTestMillis() {
        return sTimeForTestMillis;
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

    @Override
    public String getType(Uri uri) {
        int iMatch = sURIMatcher.match(uri);
        if (iMatch != 10001) {
            switch (iMatch) {
                case 1:
                    return "vnd.android.cursor.dir/calls";
                case 2:
                    return "vnd.android.cursor.item/calls";
                case 3:
                    return "vnd.android.cursor.dir/calls";
                case 4:
                    return "vnd.android.cursor.dir/calls";
                case 5:
                    return "vnd.android.cursor.dir/calls";
                case 6:
                    return "vnd.android.cursor.dir/calls";
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }
        return "vnd.android.cursor.dir/calls";
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        int iIntValue = applyingBatch() ? this.mCallingUid.get().intValue() : Binder.getCallingUid();
        this.mStats.incrementInsertStats(iIntValue, applyingBatch());
        try {
            return insertInternal(uri, contentValues);
        } finally {
            this.mStats.finishOperation(iIntValue);
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int iIntValue = applyingBatch() ? this.mCallingUid.get().intValue() : Binder.getCallingUid();
        this.mStats.incrementInsertStats(iIntValue, applyingBatch());
        try {
            return updateInternal(uri, contentValues, str, strArr);
        } finally {
            this.mStats.finishOperation(iIntValue);
        }
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int iIntValue = applyingBatch() ? this.mCallingUid.get().intValue() : Binder.getCallingUid();
        this.mStats.incrementInsertStats(iIntValue, applyingBatch());
        try {
            return deleteInternal(uri, str, strArr);
        } finally {
            this.mStats.finishOperation(iIntValue);
        }
    }

    private Uri insertInternal(Uri uri, ContentValues contentValues) {
        if (VERBOSE_LOGGING) {
            Log.v("CallLogProvider", "insert: uri=" + uri + "  values=[" + contentValues + "] CPID=" + Binder.getCallingPid());
        }
        int threadPriority = Process.getThreadPriority(Process.myTid());
        Process.setThreadPriority(-2);
        Uri uriWithAppendedId = null;
        long jCurrentTimeMillis = System.currentTimeMillis();
        Log.d("CallLogProvider", "insert() + ===========");
        if (5 == sURIMatcher.match(uri)) {
            Uri uriInsertConferenceCall = this.mCallLogProviderEx.insertConferenceCall(this.mDbHelper.getWritableDatabase(), uri, contentValues);
            Log.d("CallLogProvider", "insert()  =========== Uri:" + uriInsertConferenceCall);
            Log.d("CallLogProvider", "insert()- =========== Time:" + (System.currentTimeMillis() - jCurrentTimeMillis));
            Process.setThreadPriority(threadPriority);
            return uriInsertConferenceCall;
        }
        waitForAccess(this.mReadAccessLatch);
        DbQueryUtils.checkForSupportedColumns(sCallsProjectionMap, contentValues);
        if (hasVoicemailValue(contentValues)) {
            checkIsAllowVoicemailRequest(uri);
            this.mVoicemailPermissions.checkCallerHasWriteAccess(getCallingPackage());
        }
        if (this.mCallsInserter == null) {
            this.mCallsInserter = new DatabaseUtils.InsertHelper(this.mDbHelper.getWritableDatabase(), "calls");
        }
        ContentValues contentValues2 = new ContentValues(contentValues);
        this.mCallLogInsertionHelper.addComputedValues(contentValues2);
        this.mCallLogProviderEx.removeDuplictedCallLogForUser(this.mDbHelper, contentValues);
        long jInsert = createDatabaseModifier(this.mCallsInserter).insert(contentValues2);
        if (jInsert > 0) {
            uriWithAppendedId = ContentUris.withAppendedId(uri, jInsert);
        }
        Log.d("CallLogProvider", "insert()  =========== Uri:" + uriWithAppendedId + " Time: " + (System.currentTimeMillis() - jCurrentTimeMillis));
        Process.setThreadPriority(threadPriority);
        return uriWithAppendedId;
    }

    private int updateInternal(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        if (VERBOSE_LOGGING) {
            Log.v("CallLogProvider", "update: uri=" + uri + "  selection=[" + str + "]  args=" + Arrays.toString(strArr) + "  values=[" + contentValues + "] CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
        }
        waitForAccess(this.mReadAccessLatch);
        if (6 != sURIMatcher.match(uri)) {
            DbQueryUtils.checkForSupportedColumns(sCallsProjectionMap, contentValues);
        }
        if (hasVoicemailValue(contentValues)) {
            checkIsAllowVoicemailRequest(uri);
        }
        SelectionBuilder selectionBuilder = new SelectionBuilder(str);
        checkVoicemailPermissionAndAddRestriction(uri, selectionBuilder, false);
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        int iMatch = sURIMatcher.match(uri);
        if (iMatch != 6) {
            switch (iMatch) {
                case 1:
                    break;
                case 2:
                    selectionBuilder.addClause(DbQueryUtils.getEqualityClause("_id", parseCallIdFromUri(uri)));
                    break;
                default:
                    throw new UnsupportedOperationException("Cannot update URL: " + uri);
            }
            return createDatabaseModifier(writableDatabase).update(uri, "calls", contentValues, selectionBuilder.build(), strArr);
        }
        SelectionBuilder selectionBuilder2 = new SelectionBuilder(str);
        selectionBuilder2.addClause(DbQueryUtils.getEqualityClause("_id", parseCallIdFromUri(uri)));
        return writableDatabase.update("conference_calls", contentValues, selectionBuilder2.build(), strArr);
    }

    private int deleteInternal(Uri uri, String str, String[] strArr) {
        if (VERBOSE_LOGGING) {
            Log.v("CallLogProvider", "delete: uri=" + uri + "  selection=[" + str + "]  args=" + Arrays.toString(strArr) + " CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
        }
        waitForAccess(this.mReadAccessLatch);
        SelectionBuilder selectionBuilder = new SelectionBuilder(str);
        checkVoicemailPermissionAndAddRestriction(uri, selectionBuilder, false);
        try {
            SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
            int iMatch = sURIMatcher.match(uri);
            if (iMatch == 1) {
                return createDatabaseModifier(writableDatabase).delete("calls", selectionBuilder.build(), strArr);
            }
            switch (iMatch) {
                case 5:
                    break;
                case 6:
                    SelectionBuilder selectionBuilder2 = new SelectionBuilder(str);
                    selectionBuilder2.addClause(DbQueryUtils.getEqualityClause("_id", ContentUris.parseId(uri)));
                    str = selectionBuilder2.build();
                    break;
                default:
                    throw new UnsupportedOperationException("Cannot delete that URL: " + uri);
            }
            return this.mCallLogProviderEx.deleteConferenceCalls(writableDatabase, uri, str, strArr);
        } catch (SQLiteDiskIOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    void adjustForNewPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        this.mTaskScheduler.scheduleTask(1, phoneAccountHandle);
    }

    private DatabaseModifier createDatabaseModifier(SQLiteDatabase sQLiteDatabase) {
        return new DbModifierWithNotification("calls", sQLiteDatabase, getContext());
    }

    private DatabaseModifier createDatabaseModifier(DatabaseUtils.InsertHelper insertHelper) {
        return new DbModifierWithNotification("calls", insertHelper, getContext());
    }

    private boolean hasVoicemailValue(ContentValues contentValues) {
        return VOICEMAIL_TYPE.equals(contentValues.getAsInteger("type"));
    }

    private void checkVoicemailPermissionAndAddRestriction(Uri uri, SelectionBuilder selectionBuilder, boolean z) {
        if (isAllowVoicemailRequest(uri)) {
            if (z) {
                this.mVoicemailPermissions.checkCallerHasReadAccess(getCallingPackage());
                return;
            } else {
                this.mVoicemailPermissions.checkCallerHasWriteAccess(getCallingPackage());
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

    private void syncEntries() {
        if (isShadow()) {
            return;
        }
        UserManager userManager = UserUtils.getUserManager(getContext());
        if (!CallLog.Calls.shouldHaveSharedCallLogEntries(getContext(), userManager, userManager.getUserHandle())) {
            return;
        }
        int userHandle = userManager.getUserHandle();
        if (userManager.isSystemUser()) {
            syncEntriesFrom(0, true, false);
        } else {
            syncEntriesFrom(0, false, true);
            syncEntriesFrom(userHandle, true, false);
        }
    }

    private void syncEntriesFrom(int i, boolean z, boolean z2) {
        Uri uri = z ? CallLog.Calls.SHADOW_CONTENT_URI : CallLog.Calls.CONTENT_URI;
        long lastSyncTime = getLastSyncTime(z);
        Uri uriMaybeAddUserId = ContentProvider.maybeAddUserId(uri, i);
        ContentResolver contentResolver = getContext().getContentResolver();
        StringBuilder sb = new StringBuilder();
        sb.append("(" + EXCLUDE_VOICEMAIL_SELECTION + ") AND (date> ?)");
        if (z2) {
            sb.append(" AND (add_for_all_users=1)");
        }
        sb.append(" AND (conference_call_id<0)");
        Cursor cursorQuery = contentResolver.query(uriMaybeAddUserId, CALL_LOG_SYNC_PROJECTION, sb.toString(), new String[]{String.valueOf(lastSyncTime)}, "date ASC");
        if (cursorQuery == null) {
            return;
        }
        try {
            long jCopyEntriesFromCursor = copyEntriesFromCursor(cursorQuery, lastSyncTime, z);
            if (z) {
                contentResolver.delete(uriMaybeAddUserId, "date<= ?", new String[]{String.valueOf(jCopyEntriesFromCursor)});
            }
        } finally {
            cursorQuery.close();
        }
    }

    private void adjustForNewPhoneAccountInternal(PhoneAccountHandle phoneAccountHandle) {
        PhoneAccount phoneAccount;
        String[] strArr = {phoneAccountHandle.getComponentName().flattenToString(), phoneAccountHandle.getId()};
        Cursor cursorQuery = query(CallLog.Calls.CONTENT_URI, MINIMAL_PROJECTION, "subscription_component_name =? AND subscription_id =?", strArr, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() >= 1) {
                    this.mDbHelper.getWritableDatabase().execSQL("UPDATE calls SET phone_account_hidden=0 WHERE subscription_component_name=? AND subscription_id=?;", strArr);
                } else {
                    TelecomManager telecomManagerFrom = TelecomManager.from(getContext());
                    if (telecomManagerFrom != null && (phoneAccount = telecomManagerFrom.getPhoneAccount(phoneAccountHandle)) != null && phoneAccount.getAddress() != null) {
                        this.mDbHelper.getWritableDatabase().execSQL("UPDATE calls SET phone_account_hidden=0 WHERE phone_account_address=?;", new String[]{phoneAccount.getAddress().toString()});
                    }
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    @VisibleForTesting
    long copyEntriesFromCursor(Cursor cursor, long j, boolean z) {
        ContentValues contentValues = new ContentValues();
        SQLiteDatabase writableDatabase = this.mDbHelper.getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            String[] strArr = new String[2];
            cursor.moveToPosition(-1);
            long jLongValue = 0;
            while (cursor.moveToNext()) {
                contentValues.clear();
                DatabaseUtils.cursorRowToContentValues(cursor, contentValues);
                String asString = contentValues.getAsString("date");
                String asString2 = contentValues.getAsString("number");
                if (asString != null && asString2 != null) {
                    if (cursor.isLast()) {
                        try {
                            jLongValue = Long.valueOf(asString).longValue();
                        } catch (NumberFormatException e) {
                            Log.e("CallLogProvider", "Call log entry does not contain valid start time: " + asString);
                        }
                    }
                    strArr[0] = asString;
                    strArr[1] = asString2;
                    if (DatabaseUtils.queryNumEntries(writableDatabase, "calls", "date = ? AND number = ?", strArr) <= 0) {
                        if (contentValues.get("post_dial_digits") == null) {
                            contentValues.remove("post_dial_digits");
                        }
                        if (contentValues.get("via_number") == null) {
                            contentValues.remove("via_number");
                        }
                        writableDatabase.insert("calls", null, contentValues);
                    }
                }
            }
            if (jLongValue > j) {
                setLastTimeSynced(jLongValue, z);
            }
            writableDatabase.setTransactionSuccessful();
            return jLongValue;
        } finally {
            writableDatabase.endTransaction();
        }
    }

    private static String getLastSyncTimePropertyName(boolean z) {
        if (z) {
            return "call_log_last_synced_for_shadow";
        }
        return "call_log_last_synced";
    }

    @VisibleForTesting
    long getLastSyncTime(boolean z) {
        try {
            return Long.valueOf(this.mDbHelper.getProperty(getLastSyncTimePropertyName(z), "0")).longValue();
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void setLastTimeSynced(long j, boolean z) {
        this.mDbHelper.setProperty(getLastSyncTimePropertyName(z), String.valueOf(j));
    }

    private static void waitForAccess(CountDownLatch countDownLatch) {
        if (countDownLatch == null) {
            return;
        }
        while (true) {
            try {
                countDownLatch.await();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void performBackgroundTask(int i, Object obj) {
        if (i == 0) {
            try {
                CallLogProviderEx.addDurationColumnIfNeed(this.mDbHelper.getWritableDatabase());
                syncEntries();
                return;
            } finally {
                this.mReadAccessLatch.countDown();
                this.mReadAccessLatch = null;
            }
        }
        if (i == 1) {
            adjustForNewPhoneAccountInternal((PhoneAccountHandle) obj);
        }
    }

    @Override
    public void shutdown() {
        this.mTaskScheduler.shutdownForTest();
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        this.mStats.dump(printWriter, "  ");
    }
}
