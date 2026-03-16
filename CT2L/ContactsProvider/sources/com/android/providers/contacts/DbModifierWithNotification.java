package com.android.providers.contacts;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.provider.CallLog;
import android.provider.VoicemailContract;
import android.util.Log;
import com.android.common.io.MoreCloseables;
import com.android.providers.contacts.util.DbQueryUtils;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DbModifierWithNotification implements DatabaseModifier {
    private static final String[] PROJECTION = {"source_package"};
    private final Uri mBaseUri;
    private final Context mContext;
    private final SQLiteDatabase mDb;
    private final DatabaseUtils.InsertHelper mInsertHelper;
    private final boolean mIsCallsTable;
    private final String mTableName;
    private final VoicemailPermissions mVoicemailPermissions;

    public DbModifierWithNotification(String tableName, SQLiteDatabase db, Context context) {
        this(tableName, db, null, context);
    }

    public DbModifierWithNotification(String tableName, DatabaseUtils.InsertHelper insertHelper, Context context) {
        this(tableName, null, insertHelper, context);
    }

    private DbModifierWithNotification(String tableName, SQLiteDatabase db, DatabaseUtils.InsertHelper insertHelper, Context context) {
        this.mTableName = tableName;
        this.mDb = db;
        this.mInsertHelper = insertHelper;
        this.mContext = context;
        this.mBaseUri = this.mTableName.equals("voicemail_status") ? VoicemailContract.Status.CONTENT_URI : VoicemailContract.Voicemails.CONTENT_URI;
        this.mIsCallsTable = this.mTableName.equals("calls");
        this.mVoicemailPermissions = new VoicemailPermissions(this.mContext);
    }

    @Override
    public long insert(String table, String nullColumnHack, ContentValues values) {
        Set<String> packagesModified = getModifiedPackages(values);
        long rowId = this.mDb.insert(table, nullColumnHack, values);
        if (rowId > 0 && packagesModified.size() != 0) {
            notifyVoicemailChangeOnInsert(ContentUris.withAppendedId(this.mBaseUri, rowId), packagesModified);
        }
        if (rowId > 0 && this.mIsCallsTable) {
            notifyCallLogChange();
        }
        return rowId;
    }

    @Override
    public long insert(ContentValues values) {
        Set<String> packagesModified = getModifiedPackages(values);
        long rowId = this.mInsertHelper.insert(values);
        if (rowId > 0 && packagesModified.size() != 0) {
            notifyVoicemailChangeOnInsert(ContentUris.withAppendedId(this.mBaseUri, rowId), packagesModified);
        }
        if (rowId > 0 && this.mIsCallsTable) {
            notifyCallLogChange();
        }
        return rowId;
    }

    private void notifyCallLogChange() {
        this.mContext.getContentResolver().notifyChange(CallLog.Calls.CONTENT_URI, (ContentObserver) null, false);
    }

    private void notifyVoicemailChangeOnInsert(Uri notificationUri, Set<String> packagesModified) {
        if (this.mIsCallsTable) {
            notifyVoicemailChange(notificationUri, packagesModified, "android.intent.action.NEW_VOICEMAIL", "android.intent.action.PROVIDER_CHANGED");
        } else {
            notifyVoicemailChange(notificationUri, packagesModified, "android.intent.action.PROVIDER_CHANGED");
        }
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        Set<String> packagesModified = getModifiedPackages(whereClause, whereArgs);
        packagesModified.addAll(getModifiedPackages(values));
        int count = this.mDb.update(table, values, whereClause, whereArgs);
        if (count > 0 && packagesModified.size() != 0) {
            notifyVoicemailChange(this.mBaseUri, packagesModified, "android.intent.action.PROVIDER_CHANGED");
        }
        if (count > 0 && this.mIsCallsTable) {
            notifyCallLogChange();
        }
        return count;
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        Set<String> packagesModified = getModifiedPackages(whereClause, whereArgs);
        int count = this.mDb.delete(table, whereClause, whereArgs);
        if (count > 0 && packagesModified.size() != 0) {
            notifyVoicemailChange(this.mBaseUri, packagesModified, "android.intent.action.PROVIDER_CHANGED");
        }
        if (count > 0 && this.mIsCallsTable) {
            notifyCallLogChange();
        }
        return count;
    }

    private Set<String> getModifiedPackages(String whereClause, String[] whereArgs) {
        Set<String> modifiedPackages = new HashSet<>();
        Cursor cursor = this.mDb.query(this.mTableName, PROJECTION, DbQueryUtils.concatenateClauses("source_package IS NOT NULL", whereClause), whereArgs, null, null, null);
        while (cursor.moveToNext()) {
            modifiedPackages.add(cursor.getString(0));
        }
        MoreCloseables.closeQuietly(cursor);
        return modifiedPackages;
    }

    private Set<String> getModifiedPackages(ContentValues values) {
        Set<String> impactedPackages = new HashSet<>();
        if (values.containsKey("source_package")) {
            impactedPackages.add(values.getAsString("source_package"));
        }
        return impactedPackages;
    }

    private void notifyVoicemailChange(Uri notificationUri, Set<String> modifiedPackages, String... intentActions) {
        this.mContext.getContentResolver().notifyChange(notificationUri, (ContentObserver) null, true);
        Collection<String> callingPackages = getCallingPackages();
        for (String intentAction : intentActions) {
            boolean includeSelfChangeExtra = intentAction.equals("android.intent.action.PROVIDER_CHANGED");
            for (ComponentName component : getBroadcastReceiverComponents(intentAction, notificationUri)) {
                if (modifiedPackages.contains(component.getPackageName()) || this.mVoicemailPermissions.packageHasReadAccess(component.getPackageName())) {
                    Intent intent = new Intent(intentAction, notificationUri);
                    intent.setComponent(component);
                    if (includeSelfChangeExtra && callingPackages != null) {
                        intent.putExtra("com.android.voicemail.extra.SELF_CHANGE", callingPackages.contains(component.getPackageName()));
                    }
                    String permissionNeeded = modifiedPackages.contains(component.getPackageName()) ? "com.android.voicemail.permission.ADD_VOICEMAIL" : "com.android.voicemail.permission.READ_VOICEMAIL";
                    this.mContext.sendBroadcast(intent, permissionNeeded);
                    Object[] objArr = new Object[5];
                    objArr[0] = intent.getAction();
                    objArr[1] = intent.getData();
                    objArr[2] = component.getClassName();
                    objArr[3] = permissionNeeded;
                    objArr[4] = intent.hasExtra("com.android.voicemail.extra.SELF_CHANGE") ? Boolean.valueOf(intent.getBooleanExtra("com.android.voicemail.extra.SELF_CHANGE", false)) : null;
                    Log.v("DbModifierWithVmNotification", String.format("Sent intent. act:%s, url:%s, comp:%s, perm:%s, self_change:%s", objArr));
                }
            }
        }
    }

    private List<ComponentName> getBroadcastReceiverComponents(String intentAction, Uri uri) {
        Intent intent = new Intent(intentAction, uri);
        List<ComponentName> receiverComponents = new ArrayList<>();
        for (ResolveInfo resolveInfo : this.mContext.getPackageManager().queryBroadcastReceivers(intent, 0)) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            receiverComponents.add(new ComponentName(activityInfo.packageName, activityInfo.name));
        }
        return receiverComponents;
    }

    private Collection<String> getCallingPackages() {
        int caller = Binder.getCallingUid();
        if (caller == 0) {
            return null;
        }
        return Lists.newArrayList(this.mContext.getPackageManager().getPackagesForUid(caller));
    }
}
