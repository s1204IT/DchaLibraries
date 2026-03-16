package com.bumptech.glide.load.engine;

import android.os.Handler;
import android.util.Log;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.LogTime;
import java.util.ArrayList;
import java.util.List;

public class EngineJob implements ResourceCallback {
    private static final String TAG = "EngineJob";
    private ResourceCallback cb;
    private List<ResourceCallback> cbs;
    private boolean isCacheable;
    private boolean isCancelled;
    private boolean isComplete;
    private Key key;
    private final EngineJobListener listener;
    private Handler mainHandler;

    public EngineJob(Key key, Handler mainHandler, boolean isCacheable, EngineJobListener listener) {
        this.key = key;
        this.isCacheable = isCacheable;
        this.listener = listener;
        this.mainHandler = mainHandler;
    }

    public void addCallback(ResourceCallback cb) {
        if (this.cb == null) {
            this.cb = cb;
            return;
        }
        if (this.cbs == null) {
            this.cbs = new ArrayList(2);
            this.cbs.add(this.cb);
        }
        this.cbs.add(cb);
    }

    public void removeCallback(ResourceCallback cb) {
        if (this.cbs != null) {
            this.cbs.remove(cb);
            if (this.cbs.size() == 0) {
                cancel();
                return;
            }
            return;
        }
        if (this.cb == cb) {
            this.cb = null;
            cancel();
        }
    }

    void cancel() {
        if (!this.isComplete && !this.isCancelled) {
            this.isCancelled = true;
            this.listener.onEngineJobCancelled(this.key);
        }
    }

    boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void onResourceReady(final Resource resource) {
        final long start = LogTime.getLogTime();
        this.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Log.isLoggable(EngineJob.TAG, 2)) {
                    Log.v(EngineJob.TAG, "Posted to main thread in onResourceReady in " + LogTime.getElapsedMillis(start) + " cancelled: " + EngineJob.this.isCancelled);
                }
                if (!EngineJob.this.isCancelled) {
                    resource.setCacheable(EngineJob.this.isCacheable);
                    EngineJob.this.isComplete = true;
                    resource.acquire(1);
                    EngineJob.this.listener.onEngineJobComplete(EngineJob.this.key, resource);
                    if (EngineJob.this.cbs != null) {
                        resource.acquire(EngineJob.this.cbs.size());
                        for (ResourceCallback cb : EngineJob.this.cbs) {
                            cb.onResourceReady(resource);
                        }
                    } else {
                        resource.acquire(1);
                        EngineJob.this.cb.onResourceReady(resource);
                    }
                    resource.release();
                    if (Log.isLoggable(EngineJob.TAG, 2)) {
                        Log.v(EngineJob.TAG, "Finished resource ready in " + LogTime.getElapsedMillis(start));
                        return;
                    }
                    return;
                }
                resource.recycle();
            }
        });
    }

    @Override
    public void onException(final Exception e) {
        final long start = LogTime.getLogTime();
        this.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Log.isLoggable(EngineJob.TAG, 2)) {
                    Log.v(EngineJob.TAG, "posted to main thread in onException in " + LogTime.getElapsedMillis(start) + " cancelled: " + EngineJob.this.isCancelled);
                }
                if (!EngineJob.this.isCancelled) {
                    EngineJob.this.isComplete = true;
                    EngineJob.this.listener.onEngineJobComplete(EngineJob.this.key, null);
                    if (EngineJob.this.cbs != null) {
                        for (ResourceCallback cb : EngineJob.this.cbs) {
                            cb.onException(e);
                        }
                    } else {
                        EngineJob.this.cb.onException(e);
                    }
                    if (Log.isLoggable(EngineJob.TAG, 2)) {
                        Log.v(EngineJob.TAG, "finished onException in " + LogTime.getElapsedMillis(start));
                    }
                }
            }
        });
    }
}
