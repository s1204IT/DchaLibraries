package com.android.documentsui;

import android.app.ActivityManager;
import android.content.AsyncTaskLoader;
import android.content.ContentProviderClient;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.OperationCanceledException;
import android.os.RemoteException;
import android.util.Log;
import com.android.documentsui.archives.ArchivesProvider;
import com.android.documentsui.base.DebugFlags;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.FilteringCursorWrapper;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.dirlist.DirectoryFragment;
import com.android.documentsui.files.FilesActivity;
import com.android.documentsui.picker.PickActivity;
import com.android.documentsui.roots.RootCursorWrapper;
import com.android.documentsui.selection.ContentLock;
import com.android.documentsui.sorting.SortModel;
import java.util.Iterator;
import libcore.io.IoUtils;

public class DirectoryLoader extends AsyncTaskLoader<DirectoryResult> {
    private static final String[] SEARCH_REJECT_MIMES = {"vnd.android.document/directory"};
    private DocumentInfo mDoc;
    private int mDrmLevel;
    private Features mFeatures;
    private final Lookup<String, String> mFileTypeLookup;
    private boolean mIsLoading;
    private final SortModel mModel;
    private final LockingContentObserver mObserver;
    private DirectoryResult mResult;
    private final RootInfo mRoot;
    private final boolean mSearchMode;
    private CancellationSignal mSignal;
    private final Uri mUri;

    public DirectoryLoader(Features features, Context context, RootInfo rootInfo, DocumentInfo documentInfo, Uri uri, SortModel sortModel, Lookup<String, String> lookup, ContentLock contentLock, boolean z) {
        super(context, ProviderExecutor.forAuthority(rootInfo.authority));
        this.mIsLoading = false;
        this.mFeatures = features;
        this.mRoot = rootInfo;
        this.mUri = uri;
        this.mModel = sortModel;
        this.mDoc = documentInfo;
        this.mFileTypeLookup = lookup;
        this.mSearchMode = z;
        this.mObserver = new LockingContentObserver(contentLock, new Runnable() {
            @Override
            public final void run() {
                this.f$0.onContentChanged();
            }
        });
        try {
            this.mDrmLevel = ((FilesActivity) context).getIntent().getIntExtra("android.intent.extra.drm_level", -1);
        } catch (ClassCastException e) {
            e.printStackTrace();
            this.mDrmLevel = ((PickActivity) context).getIntent().getIntExtra("android.intent.extra.drm_level", -1);
        }
    }

    @Override
    public final DirectoryResult loadInBackground() throws Throwable {
        ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow;
        Cursor cursorQuery;
        this.mIsLoading = true;
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            this.mSignal = new CancellationSignal();
        }
        ContentProviderClient contentResolver = getContext().getContentResolver();
        String authority = this.mUri.getAuthority();
        DirectoryResult directoryResult = new DirectoryResult();
        directoryResult.doc = this.mDoc;
        try {
            try {
                contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(contentResolver, authority);
                try {
                    if (this.mDoc.isInArchive()) {
                        ArchivesProvider.acquireArchive(contentProviderClientAcquireUnstableProviderOrThrow, this.mUri);
                    }
                    directoryResult.client = contentProviderClientAcquireUnstableProviderOrThrow;
                    getContext().getResources();
                    if (this.mFeatures.isContentPagingEnabled()) {
                        Bundle bundle = new Bundle();
                        this.mModel.addQuerySortArgs(bundle);
                        DebugFlags.addForcedPagingArgs(bundle);
                        cursorQuery = contentProviderClientAcquireUnstableProviderOrThrow.query(this.mUri, null, bundle, this.mSignal);
                    } else {
                        cursorQuery = contentProviderClientAcquireUnstableProviderOrThrow.query(this.mUri, null, null, null, this.mModel.getDocumentSortQuery(), this.mSignal);
                    }
                } catch (Exception e) {
                    e = e;
                    Log.w("DirectoryLoader", "Failed to query", e);
                    directoryResult.exception = e;
                    synchronized (this) {
                        this.mSignal = null;
                    }
                }
            } catch (Throwable th) {
                th = th;
                synchronized (this) {
                    this.mSignal = null;
                }
                ContentProviderClient.releaseQuietly(contentResolver);
                throw th;
            }
        } catch (Exception e2) {
            e = e2;
            contentProviderClientAcquireUnstableProviderOrThrow = null;
        } catch (Throwable th2) {
            th = th2;
            contentResolver = 0;
            synchronized (this) {
            }
        }
        if (cursorQuery == null) {
            throw new RemoteException("Provider returned null");
        }
        cursorQuery.registerContentObserver(this.mObserver);
        RootCursorWrapper rootCursorWrapper = new RootCursorWrapper(this.mUri.getAuthority(), this.mRoot.rootId, cursorQuery, -1);
        Cursor filteringCursorWrapper = (!this.mSearchMode || this.mFeatures.isFoldersInSearchResultsEnabled()) ? new FilteringCursorWrapper(rootCursorWrapper, this.mDrmLevel) : new FilteringCursorWrapper(rootCursorWrapper, null, SEARCH_REJECT_MIMES);
        if (!this.mFeatures.isContentPagingEnabled() || !filteringCursorWrapper.getExtras().containsKey("android:query-arg-sort-columns")) {
            filteringCursorWrapper = this.mModel.sortCursor(filteringCursorWrapper, this.mFileTypeLookup);
        } else if (SharedMinimal.VERBOSE) {
            Log.d("DirectoryLoader", "Skipping sort of pre-sorted cursor. Booya!");
        }
        directoryResult.cursor = filteringCursorWrapper;
        synchronized (this) {
            this.mSignal = null;
        }
        ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
        this.mIsLoading = false;
        return directoryResult;
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

    private boolean isServiceRunning() {
        Iterator<ActivityManager.RunningServiceInfo> it = ((ActivityManager) getContext().getSystemService("activity")).getRunningServices(Integer.MAX_VALUE).iterator();
        while (it.hasNext()) {
            if ("com.android.documentsui.services.FileOperationService".equals(it.next().service.getClassName())) {
                if (SharedMinimal.DEBUG) {
                    Log.d("DirectoryLoader", "FileOpeartionService is running");
                    return true;
                }
                return true;
            }
        }
        if (SharedMinimal.DEBUG) {
            Log.d("DirectoryLoader", "FileOpeartionService is not running");
            return false;
        }
        return false;
    }

    @Override
    public void deliverResult(DirectoryResult directoryResult) {
        if (isServiceRunning() && DirectoryFragment.isDeleteInProgress) {
            DirectoryFragment.delete_count--;
            if (DirectoryFragment.delete_count > 0) {
                Log.d("DirectoryLoader", "deliverResult skip delete in progress delete_count " + DirectoryFragment.delete_count);
                IoUtils.closeQuietly(directoryResult);
                return;
            }
            Log.d("DirectoryLoader", "Reset isDeleteInProgress " + DirectoryFragment.delete_count);
            DirectoryFragment.isDeleteInProgress = false;
        } else if (!isServiceRunning()) {
            DirectoryFragment.isDeleteInProgress = false;
            DirectoryFragment.delete_count = 0;
        }
        if (isReset()) {
            IoUtils.closeQuietly(directoryResult);
            return;
        }
        if (isStarted() && directoryResult != null && directoryResult.exception != null && (directoryResult.exception instanceof DeadObjectException)) {
            Log.d("DirectoryLoader", "deliverResult with client has dead, reload directory again");
            IoUtils.closeQuietly(directoryResult);
            forceLoad();
            return;
        }
        DirectoryResult directoryResult2 = this.mResult;
        this.mResult = directoryResult;
        if (isStarted()) {
            super.deliverResult(directoryResult);
        }
        if (directoryResult2 != null && directoryResult2 != directoryResult) {
            IoUtils.closeQuietly(directoryResult2);
        }
    }

    @Override
    protected void onStartLoading() {
        boolean zTakeContentChanged = takeContentChanged();
        if (this.mResult != null) {
            try {
                this.mResult.client.canonicalize(this.mUri);
                deliverResult(this.mResult);
            } catch (Exception e) {
                Log.d("DirectoryLoader", "onStartLoading with client has dead, reload to register obsever. " + e);
                zTakeContentChanged = true;
            }
        }
        Log.d("DirectoryLoader", "onStartLoading contentChanged: " + zTakeContentChanged + ", mIsLoading: " + this.mIsLoading + ", mResult: " + this.mResult);
        if (!zTakeContentChanged && this.mIsLoading) {
            return;
        }
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(DirectoryResult directoryResult) {
        if (directoryResult == null) {
            return;
        }
        if (directoryResult.exception != null && (directoryResult.exception instanceof OperationCanceledException)) {
            IoUtils.closeQuietly(directoryResult);
            Log.d("DirectoryLoader", "DirectoryLoader: loading has been canceled, no deliver result");
        } else if (!isReset() && this.mResult == null) {
            deliverResult(directoryResult);
            Log.d("DirectoryLoader", "DirectoryLoader show result when onCanceled");
        } else {
            IoUtils.closeQuietly(directoryResult);
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        IoUtils.closeQuietly(this.mResult);
        this.mResult = null;
        getContext().getContentResolver().unregisterContentObserver(this.mObserver);
    }

    private static final class LockingContentObserver extends ContentObserver {
        private final Runnable mContentChangedCallback;
        private final ContentLock mLock;

        public LockingContentObserver(ContentLock contentLock, Runnable runnable) {
            super(new Handler(Looper.getMainLooper()));
            this.mLock = contentLock;
            this.mContentChangedCallback = runnable;
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean z) {
            this.mLock.runWhenUnlocked(this.mContentChangedCallback);
        }
    }
}
