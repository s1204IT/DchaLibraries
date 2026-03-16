package com.android.contacts.interactions;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import junit.framework.Assert;

public class TestLoaderManager extends LoaderManager {
    private LoaderManager mDelegate;
    private final HashSet<Integer> mFinishedLoaders = new HashSet<>();

    public void setDelegate(LoaderManager delegate) {
        if (delegate == null || (this.mDelegate != null && this.mDelegate != delegate)) {
            throw new IllegalArgumentException("TestLoaderManager cannot be shared");
        }
        this.mDelegate = delegate;
    }

    synchronized void waitForLoaders(int... loaderIds) {
        List<Loader<?>> loaders = new ArrayList<>(loaderIds.length);
        int len$ = loaderIds.length;
        int i$ = 0;
        while (true) {
            if (i$ < len$) {
                int loaderId = loaderIds[i$];
                if (!this.mFinishedLoaders.contains(Integer.valueOf(loaderId))) {
                    AsyncTaskLoader<?> loader = (AsyncTaskLoader) this.mDelegate.getLoader(loaderId);
                    if (loader == null) {
                        Assert.fail("Loader does not exist: " + loaderId);
                        break;
                    }
                    loaders.add(loader);
                }
                i$++;
            } else {
                waitForLoaders((Loader<?>[]) loaders.toArray(new Loader[0]));
                break;
            }
        }
    }

    public static void waitForLoaders(Loader<?>... loaders) {
        Thread[] waitThreads = new Thread[loaders.length];
        for (int i = 0; i < loaders.length; i++) {
            final AsyncTaskLoader<?> loader = (AsyncTaskLoader) loaders[i];
            waitThreads[i] = new Thread("LoaderWaitingThread" + i) {
                @Override
                public void run() {
                    try {
                        loader.waitForLoader();
                    } catch (Throwable e) {
                        Log.e("TestLoaderManager", "Exception while waiting for loader: " + loader.getId(), e);
                        Assert.fail("Exception while waiting for loader: " + loader.getId());
                    }
                }
            };
            waitThreads[i].start();
        }
        for (Thread thread : waitThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public <D> Loader<D> initLoader(final int id, Bundle args, final LoaderManager.LoaderCallbacks<D> callback) {
        return this.mDelegate.initLoader(id, args, new LoaderManager.LoaderCallbacks<D>() {
            @Override
            public Loader<D> onCreateLoader(int id2, Bundle args2) {
                return callback.onCreateLoader(id2, args2);
            }

            @Override
            public void onLoadFinished(Loader<D> loader, D data) {
                callback.onLoadFinished(loader, data);
                synchronized (this) {
                    TestLoaderManager.this.mFinishedLoaders.add(Integer.valueOf(id));
                }
            }

            @Override
            public void onLoaderReset(Loader<D> loader) {
                callback.onLoaderReset(loader);
            }
        });
    }

    @Override
    public <D> Loader<D> restartLoader(int id, Bundle args, LoaderManager.LoaderCallbacks<D> callback) {
        return this.mDelegate.restartLoader(id, args, callback);
    }

    @Override
    public void destroyLoader(int id) {
        this.mDelegate.destroyLoader(id);
    }

    @Override
    public <D> Loader<D> getLoader(int id) {
        return this.mDelegate.getLoader(id);
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mDelegate.dump(prefix, fd, writer, args);
    }
}
