package com.android.documentsui;

import android.content.AsyncTaskLoader;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.model.DocumentInfo;
import com.android.documentsui.model.RootInfo;
import java.io.FileNotFoundException;
import libcore.io.IoUtils;

public class DirectoryLoader extends AsyncTaskLoader<DirectoryResult> {
    private static final String[] SEARCH_REJECT_MIMES = {"vnd.android.document/directory"};
    private DocumentInfo mDoc;
    private final Loader<DirectoryResult>.ForceLoadContentObserver mObserver;
    private DirectoryResult mResult;
    private final RootInfo mRoot;
    private CancellationSignal mSignal;
    private final int mType;
    private final Uri mUri;
    private final int mUserSortOrder;

    public DirectoryLoader(Context context, int type, RootInfo root, DocumentInfo doc, Uri uri, int userSortOrder) {
        super(context, ProviderExecutor.forAuthority(root.authority));
        this.mObserver = new Loader.ForceLoadContentObserver(this);
        this.mType = type;
        this.mRoot = root;
        this.mDoc = doc;
        this.mUri = uri;
        this.mUserSortOrder = userSortOrder;
    }

    @Override
    public final DirectoryResult loadInBackground() throws Throwable {
        Cursor cursor;
        int userMode;
        ContentProviderClient client;
        Cursor cursor2;
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            this.mSignal = new CancellationSignal();
        }
        ContentResolver resolver = getContext().getContentResolver();
        String authority = this.mUri.getAuthority();
        DirectoryResult result = new DirectoryResult();
        if (this.mType == 2) {
            Uri docUri = DocumentsContract.buildDocumentUri(this.mRoot.authority, this.mRoot.documentId);
            try {
                this.mDoc = DocumentInfo.fromUri(resolver, docUri);
                cursor = null;
                try {
                    Uri stateUri = RecentsProvider.buildState(this.mRoot.authority, this.mRoot.rootId, this.mDoc.documentId);
                    cursor = resolver.query(stateUri, null, null, null, null);
                    userMode = cursor.moveToFirst() ? DocumentInfo.getCursorInt(cursor, "mode") : 0;
                    if (userMode == 0) {
                        result.mode = userMode;
                    } else if ((this.mDoc.flags & 16) != 0) {
                        result.mode = 2;
                    } else {
                        result.mode = 1;
                    }
                    if (this.mUserSortOrder == 0) {
                        result.sortOrder = this.mUserSortOrder;
                    } else if ((this.mDoc.flags & 32) != 0) {
                        result.sortOrder = 2;
                    } else {
                        result.sortOrder = 1;
                    }
                    if (this.mType == 2) {
                        result.sortOrder = 0;
                    }
                    Log.d("Documents", "userMode=" + userMode + ", userSortOrder=" + this.mUserSortOrder + " --> mode=" + result.mode + ", sortOrder=" + result.sortOrder);
                    client = null;
                    try {
                        try {
                            client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, authority);
                            Cursor cursor3 = client.query(this.mUri, null, null, null, getQuerySortOrder(result.sortOrder), this.mSignal);
                            cursor3.registerContentObserver(this.mObserver);
                            cursor2 = new RootCursorWrapper(this.mUri.getAuthority(), this.mRoot.rootId, cursor3, -1);
                        } catch (Exception e) {
                            e = e;
                        }
                    } catch (Throwable th) {
                        th = th;
                    }
                    try {
                        Cursor cursor4 = this.mType != 2 ? new FilteringCursorWrapper(cursor2, null, SEARCH_REJECT_MIMES) : new SortingCursorWrapper(cursor2, result.sortOrder);
                        result.client = client;
                        result.cursor = cursor4;
                        synchronized (this) {
                            this.mSignal = null;
                        }
                    } catch (Exception e2) {
                        e = e2;
                        Log.w("Documents", "Failed to query", e);
                        result.exception = e;
                        ContentProviderClient.releaseQuietly(client);
                        synchronized (this) {
                            this.mSignal = null;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        synchronized (this) {
                            this.mSignal = null;
                        }
                        throw th;
                    }
                } finally {
                    IoUtils.closeQuietly(cursor);
                }
            } catch (FileNotFoundException e3) {
                Log.w("Documents", "Failed to query", e3);
                result.exception = e3;
            }
        } else {
            cursor = null;
            Uri stateUri2 = RecentsProvider.buildState(this.mRoot.authority, this.mRoot.rootId, this.mDoc.documentId);
            cursor = resolver.query(stateUri2, null, null, null, null);
            if (cursor.moveToFirst()) {
            }
            if (userMode == 0) {
            }
            if (this.mUserSortOrder == 0) {
            }
            if (this.mType == 2) {
            }
            Log.d("Documents", "userMode=" + userMode + ", userSortOrder=" + this.mUserSortOrder + " --> mode=" + result.mode + ", sortOrder=" + result.sortOrder);
            client = null;
            client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, authority);
            Cursor cursor32 = client.query(this.mUri, null, null, null, getQuerySortOrder(result.sortOrder), this.mSignal);
            cursor32.registerContentObserver(this.mObserver);
            cursor2 = new RootCursorWrapper(this.mUri.getAuthority(), this.mRoot.rootId, cursor32, -1);
            if (this.mType != 2) {
            }
            result.client = client;
            result.cursor = cursor4;
            synchronized (this) {
            }
        }
        return result;
    }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();
        synchronized (this) {
            if (this.mSignal != null) {
                this.mSignal.cancel();
            }
        }
    }

    @Override
    public void deliverResult(DirectoryResult result) {
        if (isReset()) {
            IoUtils.closeQuietly(result);
            return;
        }
        DirectoryResult oldResult = this.mResult;
        this.mResult = result;
        if (isStarted()) {
            super.deliverResult(result);
        }
        if (oldResult != null && oldResult != result) {
            IoUtils.closeQuietly(oldResult);
        }
    }

    @Override
    protected void onStartLoading() {
        if (this.mResult != null) {
            deliverResult(this.mResult);
        }
        if (takeContentChanged() || this.mResult == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(DirectoryResult result) {
        IoUtils.closeQuietly(result);
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        IoUtils.closeQuietly(this.mResult);
        this.mResult = null;
        getContext().getContentResolver().unregisterContentObserver(this.mObserver);
    }

    public static String getQuerySortOrder(int sortOrder) {
        switch (sortOrder) {
            case 1:
                return "_display_name ASC";
            case 2:
                return "last_modified DESC";
            case 3:
                return "_size DESC";
            default:
                return null;
        }
    }
}
