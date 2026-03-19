package com.android.documentsui;

import android.app.ActivityManager;
import android.content.AsyncTaskLoader;
import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.FilteringCursorWrapper;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;
import com.android.documentsui.files.FilesActivity;
import com.android.documentsui.picker.PickActivity;
import com.android.documentsui.roots.ProvidersAccess;
import com.android.documentsui.roots.RootCursorWrapper;
import com.android.internal.annotations.GuardedBy;
import com.google.common.util.concurrent.AbstractFuture;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public class RecentsLoader extends AsyncTaskLoader<DirectoryResult> {
    private static final String[] RECENT_REJECT_MIMES = {"vnd.android.document/directory"};
    private int mDrmLevel;
    private final Lookup<String, Executor> mExecutors;
    private final Features mFeatures;
    private final Lookup<String, String> mFileTypeMap;
    private volatile boolean mFirstPassDone;
    private CountDownLatch mFirstPassLatch;
    private final ProvidersAccess mProviders;
    private final Semaphore mQueryPermits;
    private DirectoryResult mResult;
    private final State mState;

    @GuardedBy("mTasks")
    private final Map<String, RecentsTask> mTasks;
    private Handler mUiHandler;

    public RecentsLoader(Context context, ProvidersAccess providersAccess, State state, Features features, Lookup<String, Executor> lookup, Lookup<String, String> lookup2) {
        super(context);
        this.mTasks = new HashMap();
        this.mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                Log.d("Documents", "onContentChanged");
                RecentsLoader.this.onContentChanged();
            }
        };
        this.mProviders = providersAccess;
        this.mState = state;
        this.mFeatures = features;
        this.mExecutors = lookup;
        this.mFileTypeMap = lookup2;
        try {
            this.mDrmLevel = ((FilesActivity) context).getIntent().getIntExtra("android.intent.extra.drm_level", -1);
        } catch (ClassCastException e) {
            e.printStackTrace();
            this.mDrmLevel = ((PickActivity) context).getIntent().getIntExtra("android.intent.extra.drm_level", -1);
        }
        this.mQueryPermits = new Semaphore(((ActivityManager) getContext().getSystemService("activity")).isLowRamDevice() ? 2 : 4);
    }

    @Override
    public DirectoryResult loadInBackground() {
        DirectoryResult directoryResultLoadInBackgroundLocked;
        synchronized (this.mTasks) {
            directoryResultLoadInBackgroundLocked = loadInBackgroundLocked();
        }
        return directoryResultLoadInBackgroundLocked;
    }

    private DirectoryResult loadInBackgroundLocked() {
        Cursor matrixCursor;
        RecentsTask recentsTask;
        int length;
        Cursor[] cursorArr;
        int i;
        int i2;
        if (this.mFirstPassLatch == null) {
            Map<String, List<String>> mapIndexRecentsRoots = indexRecentsRoots();
            for (String str : mapIndexRecentsRoots.keySet()) {
                this.mTasks.put(str, new RecentsTask(str, mapIndexRecentsRoots.get(str)));
            }
            this.mFirstPassLatch = new CountDownLatch(this.mTasks.size());
            for (RecentsTask recentsTask2 : this.mTasks.values()) {
                this.mExecutors.lookup(recentsTask2.authority).execute(recentsTask2);
            }
            try {
                this.mFirstPassLatch.await(500L, TimeUnit.MILLISECONDS);
                this.mFirstPassDone = true;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        long jCurrentTimeMillis = System.currentTimeMillis() - 3888000000L;
        ArrayList arrayList = new ArrayList(this.mTasks.size());
        boolean z = true;
        int i3 = 0;
        for (RecentsTask recentsTask3 : this.mTasks.values()) {
            if (recentsTask3.isDone()) {
                try {
                    try {
                        cursorArr = recentsTask3.get();
                    } catch (InterruptedException e2) {
                        throw new RuntimeException(e2);
                    }
                } catch (IllegalStateException e3) {
                    e = e3;
                    length = i3;
                } catch (ExecutionException e4) {
                    length = i3;
                } catch (Exception e5) {
                    e = e5;
                    recentsTask = recentsTask3;
                    length = i3;
                }
                if (cursorArr != null && cursorArr.length != 0) {
                    length = i3 + cursorArr.length;
                    try {
                        try {
                            int length2 = cursorArr.length;
                            int i4 = 0;
                            while (i4 < length2) {
                                Cursor cursor = cursorArr[i4];
                                if (cursor == null) {
                                    i = i4;
                                    recentsTask = recentsTask3;
                                    i2 = length2;
                                } else {
                                    i = i4;
                                    recentsTask = recentsTask3;
                                    i2 = length2;
                                    try {
                                        arrayList.add(new FilteringCursorWrapper(cursor, this.mState.acceptMimes, RECENT_REJECT_MIMES, jCurrentTimeMillis) {
                                            @Override
                                            public void close() {
                                            }
                                        });
                                    } catch (Exception e6) {
                                        e = e6;
                                        Log.e("Documents", "Failed to query Recents for authority: " + recentsTask.authority + ". Skip this authority in Recents.", e);
                                        i3 = length;
                                    }
                                }
                                i4 = i + 1;
                                recentsTask3 = recentsTask;
                                length2 = i2;
                            }
                        } catch (Exception e7) {
                            e = e7;
                            recentsTask = recentsTask3;
                        }
                    } catch (IllegalStateException e8) {
                        e = e8;
                        Log.w("Documents", "cursor may have been closed when recent loader reset", e);
                    } catch (ExecutionException e9) {
                    }
                    i3 = length;
                }
            } else {
                z = false;
            }
        }
        if (SharedMinimal.DEBUG) {
            Log.d("Documents", "Found " + arrayList.size() + " of " + i3 + " recent queries done");
        }
        DirectoryResult directoryResult = new DirectoryResult();
        if (arrayList.size() > 0) {
            matrixCursor = new MergeCursor((Cursor[]) arrayList.toArray(new Cursor[arrayList.size()]));
        } else {
            matrixCursor = new MatrixCursor(new String[0]);
        }
        Cursor cursorSortCursor = this.mState.sortModel.sortCursor(new NotMovableMaskCursor(matrixCursor), this.mFileTypeMap);
        Bundle bundle = new Bundle();
        bundle.putBoolean("loading", true ^ z);
        cursorSortCursor.setExtras(bundle);
        directoryResult.cursor = new FilteringCursorWrapper(cursorSortCursor, this.mDrmLevel) {
            @Override
            public void close() {
            }
        };
        return directoryResult;
    }

    private Map<String, List<String>> indexRecentsRoots() {
        Collection<RootInfo> matchingRootsBlocking = this.mProviders.getMatchingRootsBlocking(this.mState);
        HashMap map = new HashMap();
        for (RootInfo rootInfo : matchingRootsBlocking) {
            if (rootInfo.supportsRecents()) {
                if (!map.containsKey(rootInfo.authority)) {
                    map.put(rootInfo.authority, new ArrayList());
                }
                ((List) map.get(rootInfo.authority)).add(rootInfo.rootId);
            }
        }
        return map;
    }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();
    }

    @Override
    public void deliverResult(DirectoryResult directoryResult) {
        if (isReset()) {
            IoUtils.closeQuietly(directoryResult);
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
    public void onCanceled(DirectoryResult directoryResult) {
        IoUtils.closeQuietly(directoryResult);
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        synchronized (this.mTasks) {
            Iterator<RecentsTask> it = this.mTasks.values().iterator();
            while (it.hasNext()) {
                IoUtils.closeQuietly(it.next());
            }
        }
        IoUtils.closeQuietly(this.mResult);
        this.mResult = null;
    }

    private class RecentsTask extends AbstractFuture<Cursor[]> implements Closeable, Runnable {
        public final String authority;
        private Cursor[] mCursors;
        private boolean mIsClosed = false;
        public final List<String> rootIds;

        public RecentsTask(String str, List<String> list) {
            this.authority = str;
            this.rootIds = list;
        }

        @Override
        public void run() {
            if (isCancelled()) {
                return;
            }
            try {
                RecentsLoader.this.mQueryPermits.acquire();
                try {
                    runInternal();
                } finally {
                    RecentsLoader.this.mQueryPermits.release();
                }
            } catch (InterruptedException e) {
            }
        }

        private synchronized void runInternal() {
            ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow;
            Throwable th;
            if (this.mIsClosed) {
                return;
            }
            ContentProviderClient contentProviderClient = null;
            try {
                try {
                    contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(RecentsLoader.this.getContext().getContentResolver(), this.authority);
                } catch (Exception e) {
                }
                try {
                    try {
                        Cursor[] cursorArr = new Cursor[this.rootIds.size()];
                        this.mCursors = new Cursor[this.rootIds.size()];
                        for (int i = 0; i < this.rootIds.size(); i++) {
                            Uri uriBuildRecentDocumentsUri = DocumentsContract.buildRecentDocumentsUri(this.authority, this.rootIds.get(i));
                            try {
                                if (RecentsLoader.this.mFeatures.isContentPagingEnabled()) {
                                    Bundle bundle = new Bundle();
                                    RecentsLoader.this.mState.sortModel.addQuerySortArgs(bundle);
                                    cursorArr[i] = contentProviderClientAcquireUnstableProviderOrThrow.query(uriBuildRecentDocumentsUri, null, bundle, null);
                                } else {
                                    cursorArr[i] = contentProviderClientAcquireUnstableProviderOrThrow.query(uriBuildRecentDocumentsUri, null, null, null, RecentsLoader.this.mState.sortModel.getDocumentSortQuery());
                                }
                                this.mCursors[i] = new RootCursorWrapper(this.authority, this.rootIds.get(i), cursorArr[i], 64);
                            } catch (Exception e2) {
                                Log.w("Documents", "Failed to load " + this.authority + ", " + this.rootIds.get(i), e2);
                            }
                        }
                        ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                    } catch (Throwable th2) {
                        th = th2;
                        ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
                        throw th;
                    }
                } catch (Exception e3) {
                    contentProviderClient = contentProviderClientAcquireUnstableProviderOrThrow;
                    Log.w("Documents", "Failed to acquire content resolver for authority: " + this.authority);
                    ContentProviderClient.releaseQuietly(contentProviderClient);
                }
                set(this.mCursors);
                RecentsLoader.this.mFirstPassLatch.countDown();
                if (RecentsLoader.this.mFirstPassDone && !RecentsLoader.this.mUiHandler.hasMessages(1)) {
                    RecentsLoader.this.mUiHandler.sendEmptyMessage(1);
                }
            } catch (Throwable th3) {
                contentProviderClientAcquireUnstableProviderOrThrow = contentProviderClient;
                th = th3;
            }
        }

        @Override
        public synchronized void close() throws IOException {
            if (this.mCursors == null) {
                return;
            }
            for (Cursor cursor : this.mCursors) {
                IoUtils.closeQuietly(cursor);
            }
            this.mIsClosed = true;
        }
    }

    private static class NotMovableMaskCursor extends CursorWrapper {
        private NotMovableMaskCursor(Cursor cursor) {
            super(cursor);
        }

        @Override
        public int getInt(int i) {
            int columnIndex = getWrappedCursor().getColumnIndex("flags");
            int i2 = super.getInt(i);
            return i == columnIndex ? i2 & (-1285) : i2;
        }
    }
}
