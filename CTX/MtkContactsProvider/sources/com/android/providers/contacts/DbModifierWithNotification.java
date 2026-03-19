package com.android.providers.contacts;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.provider.CallLog;
import android.provider.VoicemailContract;
import android.util.ArraySet;
import com.android.common.io.MoreCloseables;
import com.android.internal.annotations.VisibleForTesting;
import com.android.providers.contacts.util.DbQueryUtils;
import com.google.android.collect.Lists;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class DbModifierWithNotification implements DatabaseModifier {
    private static final String[] PROJECTION = {"source_package"};
    private static VoicemailNotifier sVoicemailNotifierForTest;
    private final Uri mBaseUri;
    private final Context mContext;
    private final SQLiteDatabase mDb;
    private final DatabaseUtils.InsertHelper mInsertHelper;
    private boolean mIsBulkOperation;
    private final boolean mIsCallsTable;
    private final String mTableName;
    private final VoicemailNotifier mVoicemailNotifier;

    public DbModifierWithNotification(String str, SQLiteDatabase sQLiteDatabase, Context context) {
        this(str, sQLiteDatabase, null, context);
    }

    public DbModifierWithNotification(String str, DatabaseUtils.InsertHelper insertHelper, Context context) {
        this(str, null, insertHelper, context);
    }

    private DbModifierWithNotification(String str, SQLiteDatabase sQLiteDatabase, DatabaseUtils.InsertHelper insertHelper, Context context) {
        this.mIsBulkOperation = false;
        this.mTableName = str;
        this.mDb = sQLiteDatabase;
        this.mInsertHelper = insertHelper;
        this.mContext = context;
        this.mBaseUri = this.mTableName.equals("voicemail_status") ? VoicemailContract.Status.CONTENT_URI : VoicemailContract.Voicemails.CONTENT_URI;
        this.mIsCallsTable = this.mTableName.equals("calls");
        this.mVoicemailNotifier = sVoicemailNotifierForTest != null ? sVoicemailNotifierForTest : new VoicemailNotifier(this.mContext, this.mBaseUri);
    }

    @Override
    public long insert(String str, String str2, ContentValues contentValues) {
        Set<String> modifiedPackages = getModifiedPackages(contentValues);
        if (this.mIsCallsTable) {
            contentValues.put("last_modified", Long.valueOf(getTimeMillis()));
        }
        long jInsert = this.mDb.insert(str, str2, contentValues);
        if (jInsert > 0 && modifiedPackages.size() != 0) {
            notifyVoicemailChangeOnInsert(ContentUris.withAppendedId(this.mBaseUri, jInsert), modifiedPackages);
        }
        if (jInsert > 0 && this.mIsCallsTable) {
            notifyCallLogChange();
        }
        return jInsert;
    }

    @Override
    public long insert(ContentValues contentValues) {
        Set<String> modifiedPackages = getModifiedPackages(contentValues);
        if (this.mIsCallsTable) {
            contentValues.put("last_modified", Long.valueOf(getTimeMillis()));
        }
        long jInsert = this.mInsertHelper.insert(contentValues);
        if (jInsert > 0 && modifiedPackages.size() != 0) {
            notifyVoicemailChangeOnInsert(ContentUris.withAppendedId(this.mBaseUri, jInsert), modifiedPackages);
        }
        if (jInsert > 0 && this.mIsCallsTable) {
            notifyCallLogChange();
        }
        return jInsert;
    }

    private void notifyCallLogChange() {
        this.mContext.getContentResolver().notifyChange(CallLog.Calls.CONTENT_URI, (ContentObserver) null, false);
        Intent intent = new Intent("com.android.internal.action.CALL_LOG_CHANGE");
        intent.setComponent(new ComponentName("com.android.calllogbackup", "com.android.calllogbackup.CallLogChangeReceiver"));
        if (!this.mContext.getPackageManager().queryBroadcastReceivers(intent, 0).isEmpty()) {
            this.mContext.sendBroadcast(intent);
        }
    }

    private void notifyVoicemailChangeOnInsert(Uri uri, Set<String> set) {
        if (this.mIsCallsTable) {
            this.mVoicemailNotifier.addIntentActions("android.intent.action.NEW_VOICEMAIL");
        }
        notifyVoicemailChange(uri, set);
    }

    private void notifyVoicemailChange(Uri uri, Set<String> set) {
        this.mVoicemailNotifier.addUri(uri);
        this.mVoicemailNotifier.addModifiedPackages(set);
        this.mVoicemailNotifier.addIntentActions("android.intent.action.PROVIDER_CHANGED");
        if (!this.mIsBulkOperation) {
            this.mVoicemailNotifier.sendNotification();
        }
    }

    @Override
    public int update(Uri uri, String str, ContentValues contentValues, String str2, String[] strArr) {
        Set<String> modifiedPackages = getModifiedPackages(str2, strArr);
        modifiedPackages.addAll(getModifiedPackages(contentValues));
        boolean z = true;
        boolean z2 = modifiedPackages.size() != 0 && isUpdatingVoicemailColumns(contentValues);
        if (this.mIsCallsTable) {
            if (contentValues.containsKey("deleted") && !contentValues.getAsBoolean("deleted").booleanValue()) {
                contentValues.put("last_modified", Long.valueOf(getTimeMillis()));
            } else {
                updateLastModified(str, str2, strArr);
            }
            if (z2 && updateDirtyFlag(contentValues, modifiedPackages) && contentValues.containsKey("is_read") && getAsBoolean(contentValues, "is_read").booleanValue() && !contentValues.containsKey("new")) {
                contentValues.put("new", (Integer) 0);
            }
        } else {
            z = false;
        }
        if (contentValues.isEmpty()) {
            return 0;
        }
        int iUpdate = this.mDb.update(str, contentValues, str2, strArr);
        if ((iUpdate > 0 && z2) || "voicemail_status".equals(str)) {
            notifyVoicemailChange(this.mBaseUri, modifiedPackages);
        }
        if (iUpdate > 0 && this.mIsCallsTable) {
            notifyCallLogChange();
        }
        if (z) {
            this.mContext.sendBroadcast(new Intent("android.intent.action.NEW_VOICEMAIL", uri), "com.android.voicemail.permission.READ_VOICEMAIL");
        }
        return iUpdate;
    }

    private boolean updateDirtyFlag(ContentValues contentValues, Set<String> set) {
        int i;
        Integer asInteger = contentValues.getAsInteger("dirty");
        if (asInteger != null) {
            if (asInteger.intValue() == -1) {
                contentValues.remove("dirty");
                return false;
            }
            i = asInteger.intValue() == 0 ? 0 : 1;
        } else {
            i = !isSelfModifyingOrInternal(set) ? 1 : 0;
        }
        contentValues.put("dirty", Integer.valueOf(i));
        return i == 0;
    }

    private boolean isUpdatingVoicemailColumns(ContentValues contentValues) {
        Iterator<String> it = contentValues.keySet().iterator();
        while (it.hasNext()) {
            if (VoicemailContentTable.ALLOWED_COLUMNS.contains(it.next())) {
                return true;
            }
        }
        return false;
    }

    private void updateLastModified(String str, String str2, String[] strArr) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("last_modified", Long.valueOf(getTimeMillis()));
        this.mDb.update(str, contentValues, DbQueryUtils.concatenateClauses("deleted == 0", str2), strArr);
    }

    @Override
    public int delete(String str, String str2, String[] strArr) {
        boolean z;
        int iDelete;
        Set<String> modifiedPackages = getModifiedPackages(str2, strArr);
        if (modifiedPackages.size() == 0) {
            z = false;
        } else {
            z = true;
        }
        if (this.mIsCallsTable && z && !isSelfModifyingOrInternal(modifiedPackages)) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("dirty", (Integer) 1);
            contentValues.put("deleted", (Integer) 1);
            contentValues.put("last_modified", Long.valueOf(getTimeMillis()));
            iDelete = this.mDb.update(str, contentValues, str2, strArr);
        } else {
            iDelete = this.mDb.delete(str, str2, strArr);
        }
        if (iDelete > 0 && z) {
            notifyVoicemailChange(this.mBaseUri, modifiedPackages);
        }
        if (iDelete > 0 && this.mIsCallsTable) {
            notifyCallLogChange();
        }
        return iDelete;
    }

    @Override
    public void startBulkOperation() {
        this.mIsBulkOperation = true;
        this.mDb.beginTransaction();
    }

    @Override
    public void yieldBulkOperation() {
        this.mDb.yieldIfContendedSafely();
    }

    @Override
    public void finishBulkOperation() {
        this.mDb.setTransactionSuccessful();
        this.mDb.endTransaction();
        this.mIsBulkOperation = false;
        this.mVoicemailNotifier.sendNotification();
    }

    private Set<String> getModifiedPackages(String str, String[] strArr) {
        ArraySet arraySet = new ArraySet();
        Cursor cursorQuery = this.mDb.query(this.mTableName, PROJECTION, DbQueryUtils.concatenateClauses("source_package IS NOT NULL", str), strArr, null, null, null);
        while (cursorQuery.moveToNext()) {
            arraySet.add(cursorQuery.getString(0));
        }
        MoreCloseables.closeQuietly(cursorQuery);
        return arraySet;
    }

    private Set<String> getModifiedPackages(ContentValues contentValues) {
        ArraySet arraySet = new ArraySet();
        if (contentValues.containsKey("source_package")) {
            arraySet.add(contentValues.getAsString("source_package"));
        }
        return arraySet;
    }

    private boolean isSelfModifyingOrInternal(Set<String> set) {
        Collection<String> callingPackages = getCallingPackages();
        if (callingPackages != null && set.size() == 1) {
            return callingPackages.contains(Iterables.getOnlyElement(set)) || callingPackages.contains(this.mContext.getPackageName());
        }
        return false;
    }

    private Collection<String> getCallingPackages() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0) {
            return null;
        }
        return Lists.newArrayList(this.mContext.getPackageManager().getPackagesForUid(callingUid));
    }

    private static Boolean getAsBoolean(ContentValues contentValues, String str) {
        Object obj = contentValues.get(str);
        if (obj instanceof CharSequence) {
            try {
                return Boolean.valueOf(Integer.parseInt(obj.toString()) != 0);
            } catch (NumberFormatException e) {
            }
        }
        return contentValues.getAsBoolean(str);
    }

    private long getTimeMillis() {
        if (CallLogProvider.getTimeForTestMillis() == null) {
            return System.currentTimeMillis();
        }
        return CallLogProvider.getTimeForTestMillis().longValue();
    }

    @VisibleForTesting
    static void setVoicemailNotifierForTest(VoicemailNotifier voicemailNotifier) {
        sVoicemailNotifierForTest = voicemailNotifier;
    }
}
