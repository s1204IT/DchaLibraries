package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.CancellationSignal;
import java.io.FileNotFoundException;

public class ProfileProvider extends AbstractContactsProvider {
    private final ContactsProvider2 mDelegate;

    public ProfileProvider(ContactsProvider2 delegate) {
        this.mDelegate = delegate;
    }

    public void enforceReadPermission(Uri uri) {
        if (!this.mDelegate.isValidPreAuthorizedUri(uri)) {
            this.mDelegate.getContext().enforceCallingOrSelfPermission("android.permission.READ_PROFILE", null);
        }
    }

    public void enforceWritePermission() {
        this.mDelegate.getContext().enforceCallingOrSelfPermission("android.permission.WRITE_PROFILE", null);
    }

    @Override
    protected ProfileDatabaseHelper getDatabaseHelper(Context context) {
        return ProfileDatabaseHelper.getInstance(context);
    }

    @Override
    protected ThreadLocal<ContactsTransaction> getTransactionHolder() {
        return this.mDelegate.getTransactionHolder();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return query(uri, projection, selection, selectionArgs, sortOrder, null);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        enforceReadPermission(uri);
        return this.mDelegate.queryLocal(uri, projection, selection, selectionArgs, sortOrder, -1L, cancellationSignal);
    }

    @Override
    protected Uri insertInTransaction(Uri uri, ContentValues values) {
        enforceWritePermission();
        useProfileDbForTransaction();
        return this.mDelegate.insertInTransaction(uri, values);
    }

    @Override
    protected int updateInTransaction(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        enforceWritePermission();
        useProfileDbForTransaction();
        return this.mDelegate.updateInTransaction(uri, values, selection, selectionArgs);
    }

    @Override
    protected int deleteInTransaction(Uri uri, String selection, String[] selectionArgs) {
        enforceWritePermission();
        useProfileDbForTransaction();
        return this.mDelegate.deleteInTransaction(uri, selection, selectionArgs);
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        if (mode != null && mode.contains("w")) {
            enforceWritePermission();
        } else {
            enforceReadPermission(uri);
        }
        return this.mDelegate.openAssetFileLocal(uri, mode);
    }

    private void useProfileDbForTransaction() {
        ContactsTransaction transaction = getCurrentTransaction();
        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        transaction.startTransactionForDb(db, "profile", this);
    }

    @Override
    protected void notifyChange() {
        this.mDelegate.notifyChange();
    }

    @Override
    public void onBegin() {
        this.mDelegate.onBeginTransactionInternal(true);
    }

    @Override
    public void onCommit() throws Throwable {
        this.mDelegate.onCommitTransactionInternal(true);
        sendProfileChangedBroadcast();
    }

    @Override
    public void onRollback() {
        this.mDelegate.onRollbackTransactionInternal(true);
    }

    @Override
    protected boolean yield(ContactsTransaction transaction) {
        return this.mDelegate.yield(transaction);
    }

    @Override
    public String getType(Uri uri) {
        return this.mDelegate.getType(uri);
    }

    public String toString() {
        return "ProfileProvider";
    }

    private void sendProfileChangedBroadcast() {
        Intent intent = new Intent("android.provider.Contacts.PROFILE_CHANGED");
        this.mDelegate.getContext().sendBroadcast(intent, "android.permission.READ_PROFILE");
    }
}
