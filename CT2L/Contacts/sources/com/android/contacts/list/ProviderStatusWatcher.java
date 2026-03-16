package com.android.contacts.list;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import com.google.common.collect.Lists;
import java.util.ArrayList;

public class ProviderStatusWatcher extends ContentObserver {
    private static final String[] PROJECTION = {"status", "data1"};
    private static ProviderStatusWatcher sInstance;
    private final Context mContext;
    private final Handler mHandler;
    private final ArrayList<ProviderStatusListener> mListeners;
    private LoaderTask mLoaderTask;
    private Status mProviderStatus;
    private final Object mSignal;
    private final Runnable mStartLoadingRunnable;
    private int mStartRequestedCount;

    public interface ProviderStatusListener {
        void onProviderStatusChange();
    }

    public static class Status {
        public final String data;
        public final int status;

        public Status(int status, String data) {
            this.status = status;
            this.data = data;
        }
    }

    public static synchronized ProviderStatusWatcher getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ProviderStatusWatcher(context);
        }
        return sInstance;
    }

    private ProviderStatusWatcher(Context context) {
        super(null);
        this.mHandler = new Handler();
        this.mSignal = new Object();
        this.mListeners = Lists.newArrayList();
        this.mStartLoadingRunnable = new Runnable() {
            @Override
            public void run() {
                ProviderStatusWatcher.this.startLoading();
            }
        };
        this.mContext = context;
    }

    public void addListener(ProviderStatusListener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(ProviderStatusListener listener) {
        this.mListeners.remove(listener);
    }

    private void notifyListeners() {
        if (isStarted()) {
            for (ProviderStatusListener listener : this.mListeners) {
                listener.onProviderStatusChange();
            }
        }
    }

    private boolean isStarted() {
        return this.mStartRequestedCount > 0;
    }

    public void start() {
        int i = this.mStartRequestedCount + 1;
        this.mStartRequestedCount = i;
        if (i == 1) {
            this.mContext.getContentResolver().registerContentObserver(ContactsContract.ProviderStatus.CONTENT_URI, false, this);
            startLoading();
        }
    }

    public void stop() {
        if (!isStarted()) {
            Log.e("ProviderStatusWatcher", "Already stopped");
            return;
        }
        int i = this.mStartRequestedCount - 1;
        this.mStartRequestedCount = i;
        if (i == 0) {
            this.mHandler.removeCallbacks(this.mStartLoadingRunnable);
            this.mContext.getContentResolver().unregisterContentObserver(this);
        }
    }

    public Status getProviderStatus() {
        waitForLoaded();
        return this.mProviderStatus == null ? new Status(1, null) : this.mProviderStatus;
    }

    private void waitForLoaded() {
        if (this.mProviderStatus == null) {
            if (this.mLoaderTask == null) {
                startLoading();
            }
            synchronized (this.mSignal) {
                try {
                    this.mSignal.wait(1000L);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private void startLoading() {
        if (this.mLoaderTask == null) {
            this.mLoaderTask = new LoaderTask();
            this.mLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    private class LoaderTask extends AsyncTask<Void, Void, Boolean> {
        private LoaderTask() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean z;
            try {
                Cursor cursor = ProviderStatusWatcher.this.mContext.getContentResolver().query(ContactsContract.ProviderStatus.CONTENT_URI, ProviderStatusWatcher.PROJECTION, null, null, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            ProviderStatusWatcher.this.mProviderStatus = new Status(cursor.getInt(0), cursor.getString(1));
                            z = true;
                            synchronized (ProviderStatusWatcher.this.mSignal) {
                                ProviderStatusWatcher.this.mSignal.notifyAll();
                            }
                        } else {
                            z = false;
                            synchronized (ProviderStatusWatcher.this.mSignal) {
                                ProviderStatusWatcher.this.mSignal.notifyAll();
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                } else {
                    z = false;
                    synchronized (ProviderStatusWatcher.this.mSignal) {
                    }
                }
                return z;
            } catch (Throwable th) {
                synchronized (ProviderStatusWatcher.this.mSignal) {
                    ProviderStatusWatcher.this.mSignal.notifyAll();
                    throw th;
                }
            }
        }

        @Override
        protected void onCancelled(Boolean result) {
            cleanUp();
        }

        @Override
        protected void onPostExecute(Boolean loaded) {
            cleanUp();
            if (loaded != null && loaded.booleanValue()) {
                ProviderStatusWatcher.this.notifyListeners();
            }
        }

        private void cleanUp() {
            ProviderStatusWatcher.this.mLoaderTask = null;
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (ContactsContract.ProviderStatus.CONTENT_URI.equals(uri)) {
            Log.i("ProviderStatusWatcher", "Provider status changed.");
            this.mHandler.removeCallbacks(this.mStartLoadingRunnable);
            this.mHandler.post(this.mStartLoadingRunnable);
        }
    }

    public static void retryUpgrade(final Context context) {
        Log.i("ProviderStatusWatcher", "retryUpgrade");
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ContentValues values = new ContentValues();
                values.put("status", (Integer) 1);
                context.getContentResolver().update(ContactsContract.ProviderStatus.CONTENT_URI, values, null, null);
                return null;
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }
}
