package com.android.contacts.list;

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
import com.android.contacts.R;
import com.android.contacts.compat.DirectoryCompat;
import com.mediatek.contacts.util.Log;

public class DirectoryListLoader extends AsyncTaskLoader<Cursor> {
    private static final String[] RESULT_PROJECTION = {"_id", "directoryType", "displayName", "photoSupport"};
    private MatrixCursor mDefaultDirectoryList;
    private int mDirectorySearchMode;
    private boolean mLocalInvisibleDirectoryEnabled;
    private final ContentObserver mObserver;

    private static final class DirectoryQuery {
        public static final String[] PROJECTION = {"_id", "packageName", "typeResourceId", "displayName", "photoSupport"};

        public static Uri getDirectoryUri(int i) {
            if (i == 3 || i == 2) {
                return ContactsContract.Directory.CONTENT_URI;
            }
            return DirectoryCompat.getContentUri();
        }
    }

    public DirectoryListLoader(Context context) {
        super(context);
        this.mObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z) {
                DirectoryListLoader.this.forceLoad();
            }
        };
    }

    public void setDirectorySearchMode(int i) {
        this.mDirectorySearchMode = i;
    }

    public void setLocalInvisibleDirectoryEnabled(boolean z) {
        this.mLocalInvisibleDirectoryEnabled = z;
    }

    @Override
    protected void onStartLoading() {
        getContext().getContentResolver().registerContentObserver(DirectoryQuery.getDirectoryUri(this.mDirectorySearchMode), false, this.mObserver);
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        getContext().getContentResolver().unregisterContentObserver(this.mObserver);
    }

    @Override
    public Cursor loadInBackground() throws Throwable {
        String str;
        String str2;
        Cursor cursorQuery;
        String string;
        if (this.mDirectorySearchMode == 0) {
            return getDefaultDirectories();
        }
        MatrixCursor matrixCursor = new MatrixCursor(RESULT_PROJECTION);
        Context context = getContext();
        PackageManager packageManager = context.getPackageManager();
        Cursor cursor = null;
        switch (this.mDirectorySearchMode) {
            case 1:
                str = null;
                try {
                    try {
                        cursorQuery = context.getContentResolver().query(DirectoryQuery.getDirectoryUri(this.mDirectorySearchMode), DirectoryQuery.PROJECTION, str, null, "_id");
                        break;
                    } catch (RuntimeException e) {
                    }
                    if (cursorQuery == null) {
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return matrixCursor;
                    }
                    while (cursorQuery.moveToNext()) {
                        try {
                            long j = cursorQuery.getLong(0);
                            if (this.mLocalInvisibleDirectoryEnabled || !DirectoryCompat.isInvisibleDirectory(j)) {
                                String string2 = cursorQuery.getString(1);
                                int i = cursorQuery.getInt(2);
                                if (TextUtils.isEmpty(string2) || i == 0) {
                                    string = null;
                                    matrixCursor.addRow(new Object[]{Long.valueOf(j), string, cursorQuery.getString(3), Integer.valueOf(cursorQuery.getInt(4))});
                                } else {
                                    try {
                                        string = packageManager.getResourcesForApplication(string2).getString(i);
                                    } catch (Exception e2) {
                                        Log.e("ContactEntryListAdapter", "Cannot obtain directory type from package: " + string2);
                                        string = null;
                                    }
                                    matrixCursor.addRow(new Object[]{Long.valueOf(j), string, cursorQuery.getString(3), Integer.valueOf(cursorQuery.getInt(4))});
                                }
                                break;
                            }
                        } catch (RuntimeException e3) {
                            cursor = cursorQuery;
                            Log.w("ContactEntryListAdapter", "Runtime Exception when querying directory");
                            if (cursor != null) {
                                cursor.close();
                            }
                        } catch (Throwable th) {
                            th = th;
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            throw th;
                        }
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return matrixCursor;
                } catch (Throwable th2) {
                    th = th2;
                    cursorQuery = cursor;
                }
                break;
            case 2:
                str2 = "shortcutSupport=2";
                str = str2;
                cursorQuery = context.getContentResolver().query(DirectoryQuery.getDirectoryUri(this.mDirectorySearchMode), DirectoryQuery.PROJECTION, str, null, "_id");
                if (cursorQuery == null) {
                }
                break;
            case 3:
                str2 = "shortcutSupport IN (2, 1)";
                str = str2;
                cursorQuery = context.getContentResolver().query(DirectoryQuery.getDirectoryUri(this.mDirectorySearchMode), DirectoryQuery.PROJECTION, str, null, "_id");
                if (cursorQuery == null) {
                }
                break;
            default:
                throw new RuntimeException("Unsupported directory search mode: " + this.mDirectorySearchMode);
        }
    }

    private Cursor getDefaultDirectories() {
        if (this.mDefaultDirectoryList == null) {
            this.mDefaultDirectoryList = new MatrixCursor(RESULT_PROJECTION);
            this.mDefaultDirectoryList.addRow(new Object[]{0L, getContext().getString(R.string.contactsList), null, null});
            this.mDefaultDirectoryList.addRow(new Object[]{1L, getContext().getString(R.string.local_invisible_directory), null, null});
        }
        return this.mDefaultDirectoryList;
    }

    @Override
    protected void onReset() {
        stopLoading();
    }
}
