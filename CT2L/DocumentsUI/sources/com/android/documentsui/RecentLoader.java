package com.android.documentsui;

import android.app.ActivityManager;
import android.content.AsyncTaskLoader;
import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.DocumentsActivity;
import com.android.documentsui.model.RootInfo;
import com.google.android.collect.Maps;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractFuture;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public class RecentLoader extends AsyncTaskLoader<DirectoryResult> {
    private static final String[] RECENT_REJECT_MIMES = {"vnd.android.document/directory"};
    private volatile boolean mFirstPassDone;
    private CountDownLatch mFirstPassLatch;
    private final Semaphore mQueryPermits;
    private DirectoryResult mResult;
    private final RootsCache mRoots;
    private final int mSortOrder;
    private final DocumentsActivity.State mState;
    private final HashMap<RootInfo, RecentTask> mTasks;

    public class RecentTask extends AbstractFuture<Cursor> implements Closeable, Runnable {
        public final String authority;
        private Cursor mWithRoot;
        public final String rootId;

        public RecentTask(String authority, String rootId) {
            this.authority = authority;
            this.rootId = rootId;
        }

        @Override
        public void run() {
            if (!isCancelled()) {
                try {
                    RecentLoader.this.mQueryPermits.acquire();
                    try {
                        runInternal();
                    } finally {
                        RecentLoader.this.mQueryPermits.release();
                    }
                } catch (InterruptedException e) {
                }
            }
        }

        public void runInternal() {
            ContentProviderClient client = null;
            try {
                client = DocumentsApplication.acquireUnstableProviderOrThrow(RecentLoader.this.getContext().getContentResolver(), this.authority);
                Uri uri = DocumentsContract.buildRecentDocumentsUri(this.authority, this.rootId);
                Cursor cursor = client.query(uri, null, null, null, DirectoryLoader.getQuerySortOrder(2));
                this.mWithRoot = new RootCursorWrapper(this.authority, this.rootId, cursor, 64);
            } catch (Exception e) {
                Log.w("Documents", "Failed to load " + this.authority + ", " + this.rootId, e);
            } finally {
                ContentProviderClient.releaseQuietly(client);
            }
            set(this.mWithRoot);
            RecentLoader.this.mFirstPassLatch.countDown();
            if (RecentLoader.this.mFirstPassDone) {
                RecentLoader.this.onContentChanged();
            }
        }

        @Override
        public void close() throws IOException {
            IoUtils.closeQuietly(this.mWithRoot);
        }
    }

    public RecentLoader(Context context, RootsCache roots, DocumentsActivity.State state) {
        super(context);
        this.mTasks = Maps.newHashMap();
        this.mSortOrder = 2;
        this.mRoots = roots;
        this.mState = state;
        ActivityManager am = (ActivityManager) getContext().getSystemService("activity");
        this.mQueryPermits = new Semaphore(am.isLowRamDevice() ? 2 : 4);
    }

    @Override
    public DirectoryResult loadInBackground() {
        Cursor merged;
        if (this.mFirstPassLatch == null) {
            Collection<RootInfo> roots = this.mRoots.getMatchingRootsBlocking(this.mState);
            for (RootInfo root : roots) {
                if ((root.flags & 4) != 0) {
                    this.mTasks.put(root, new RecentTask(root.authority, root.rootId));
                }
            }
            this.mFirstPassLatch = new CountDownLatch(this.mTasks.size());
            for (RecentTask task : this.mTasks.values()) {
                ProviderExecutor.forAuthority(task.authority).execute(task);
            }
            try {
                this.mFirstPassLatch.await(500L, TimeUnit.MILLISECONDS);
                this.mFirstPassDone = true;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        long rejectBefore = System.currentTimeMillis() - 3888000000L;
        boolean allDone = true;
        List<Cursor> cursors = Lists.newArrayList();
        for (RecentTask task2 : this.mTasks.values()) {
            if (task2.isDone()) {
                try {
                    Cursor cursor = task2.get();
                    if (cursor != null) {
                        FilteringCursorWrapper filtered = new FilteringCursorWrapper(cursor, this.mState.acceptMimes, RECENT_REJECT_MIMES, rejectBefore) {
                            @Override
                            public void close() {
                            }
                        };
                        cursors.add(filtered);
                    }
                } catch (InterruptedException e2) {
                    throw new RuntimeException(e2);
                } catch (ExecutionException e3) {
                }
            } else {
                allDone = false;
            }
        }
        Log.d("Documents", "Found " + cursors.size() + " of " + this.mTasks.size() + " recent queries done");
        DirectoryResult result = new DirectoryResult();
        result.sortOrder = 2;
        final Bundle extras = new Bundle();
        if (!allDone) {
            extras.putBoolean("loading", true);
        }
        if (cursors.size() > 0) {
            merged = new MergeCursor((Cursor[]) cursors.toArray(new Cursor[cursors.size()]));
        } else {
            merged = new MatrixCursor(new String[0]);
        }
        SortingCursorWrapper sorted = new SortingCursorWrapper(merged, result.sortOrder) {
            @Override
            public Bundle getExtras() {
                return extras;
            }
        };
        result.cursor = sorted;
        return result;
    }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();
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
        for (RecentTask task : this.mTasks.values()) {
            IoUtils.closeQuietly(task);
        }
        IoUtils.closeQuietly(this.mResult);
        this.mResult = null;
    }
}
