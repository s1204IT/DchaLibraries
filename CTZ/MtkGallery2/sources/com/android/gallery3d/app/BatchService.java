package com.android.gallery3d.app;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import com.android.gallery3d.util.ThreadPool;

public class BatchService extends Service {
    private static ThreadPool mThreadPool = new ThreadPool(1, 1);
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        BatchService getService() {
            return BatchService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    public ThreadPool getThreadPool() {
        return mThreadPool;
    }
}
