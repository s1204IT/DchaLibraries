package com.android.contacts.common.list;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import com.android.contacts.R;

public class DirectoryListLoader extends AsyncTaskLoader<Cursor> {
    private static final String[] RESULT_PROJECTION = {"_id", "directoryType", "displayName", "photoSupport"};
    private MatrixCursor mDefaultDirectoryList;
    private int mDirectorySearchMode;
    private boolean mLocalInvisibleDirectoryEnabled;
    private final ContentObserver mObserver;

    private static final class DirectoryQuery {
        public static final Uri URI = ContactsContract.Directory.CONTENT_URI;
        public static final String[] PROJECTION = {"_id", "packageName", "typeResourceId", "displayName", "photoSupport"};
    }

    public DirectoryListLoader(Context context) {
        super(context);
        this.mObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                DirectoryListLoader.this.forceLoad();
            }
        };
    }

    public void setDirectorySearchMode(int mode) {
        this.mDirectorySearchMode = mode;
    }

    public void setLocalInvisibleDirectoryEnabled(boolean flag) {
        this.mLocalInvisibleDirectoryEnabled = flag;
    }

    @Override
    protected void onStartLoading() {
        getContext().getContentResolver().registerContentObserver(ContactsContract.Directory.CONTENT_URI, false, this.mObserver);
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        getContext().getContentResolver().unregisterContentObserver(this.mObserver);
    }

    @Override
    public Cursor loadInBackground() {
        String selection;
        if (this.mDirectorySearchMode == 0) {
            return getDefaultDirectories();
        }
        MatrixCursor result = new MatrixCursor(RESULT_PROJECTION);
        Context context = getContext();
        PackageManager pm = context.getPackageManager();
        switch (this.mDirectorySearchMode) {
            case 1:
                selection = !this.mLocalInvisibleDirectoryEnabled ? "_id!=1" : null;
                break;
            case 2:
                selection = "shortcutSupport=2" + (this.mLocalInvisibleDirectoryEnabled ? "" : " AND _id!=1");
                break;
            case 3:
                selection = "shortcutSupport IN (2, 1)" + (this.mLocalInvisibleDirectoryEnabled ? "" : " AND _id!=1");
                break;
            default:
                throw new RuntimeException("Unsupported directory search mode: " + this.mDirectorySearchMode);
        }
        Cursor cursor = context.getContentResolver().query(DirectoryQuery.URI, DirectoryQuery.PROJECTION, selection, null, "_id");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    long directoryId = cursor.getLong(0);
                    String directoryType = null;
                    String packageName = cursor.getString(1);
                    int typeResourceId = cursor.getInt(2);
                    if (!TextUtils.isEmpty(packageName) && typeResourceId != 0) {
                        try {
                            directoryType = pm.getResourcesForApplication(packageName).getString(typeResourceId);
                        } catch (Exception e) {
                            Log.e("ContactEntryListAdapter", "Cannot obtain directory type from package: " + packageName);
                        }
                    }
                    String displayName = cursor.getString(3);
                    int photoSupport = cursor.getInt(4);
                    result.addRow(new Object[]{Long.valueOf(directoryId), directoryType, displayName, Integer.valueOf(photoSupport)});
                } finally {
                    cursor.close();
                }
            }
            return result;
        }
        return result;
    }

    private Cursor getDefaultDirectories() {
        if (this.mDefaultDirectoryList == null) {
            this.mDefaultDirectoryList = new MatrixCursor(RESULT_PROJECTION);
            this.mDefaultDirectoryList.addRow(new Object[]{0L, getContext().getString(R.string.contactsList), null});
            this.mDefaultDirectoryList.addRow(new Object[]{1L, getContext().getString(R.string.local_invisible_directory), null});
        }
        return this.mDefaultDirectoryList;
    }

    @Override
    protected void onReset() {
        stopLoading();
    }
}
