package com.android.contacts.list;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.ContactsContract;
import com.android.contacts.compat.ProviderStatusCompat;
import com.android.contactsbind.FeedbackHelper;
import com.google.common.collect.Lists;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Iterator;

public class ProviderStatusWatcher extends ContentObserver {
    private static final String[] PROJECTION = {"status"};
    private static ProviderStatusWatcher sInstance;
    private final Context mContext;
    private final Handler mHandler;
    private final ArrayList<ProviderStatusListener> mListeners;
    private LoaderTask mLoaderTask;
    private Integer mProviderStatus;
    private final Object mSignal;
    private final Runnable mStartLoadingRunnable;
    private int mStartRequestedCount;

    public interface ProviderStatusListener {
        void onProviderStatusChange();
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

    public void addListener(ProviderStatusListener providerStatusListener) {
        this.mListeners.add(providerStatusListener);
    }

    public void removeListener(ProviderStatusListener providerStatusListener) {
        this.mListeners.remove(providerStatusListener);
    }

    private void notifyListeners() {
        if (isStarted()) {
            Iterator<ProviderStatusListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onProviderStatusChange();
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

    public int getProviderStatus() {
        waitForLoaded();
        if (this.mProviderStatus == null) {
            Log.d("ProviderStatusWatcher", "[getProviderStatus]mProviderStatus is null, return STATUS_BUSY");
            return ProviderStatusCompat.STATUS_BUSY;
        }
        return this.mProviderStatus.intValue();
    }

    private void waitForLoaded() {
        Log.d("ProviderStatusWatcher", "[waitForLoaded] mProviderStatus=" + this.mProviderStatus + ", mLoaderTask=" + this.mLoaderTask);
        if (this.mProviderStatus == null) {
            if (this.mLoaderTask == null) {
                startLoading();
            }
            synchronized (this.mSignal) {
                try {
                    this.mSignal.wait(1000L);
                } catch (InterruptedException e) {
                    Log.e("ProviderStatusWatcher", "[getProviderStatus]InterruptedException", e);
                }
            }
        }
    }

    private void startLoading() {
        if (this.mLoaderTask != null) {
            Log.d("ProviderStatusWatcher", "[startLoading]mLoaderTask != null");
        }
        if (Log.ENG_DEBUG) {
            Log.d("ProviderStatusWatcher", "Start loading");
        }
        this.mLoaderTask = new LoaderTask();
        this.mLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private class LoaderTask extends AsyncTask<Void, Void, Boolean> {
        private LoaderTask() {
        }

        @Override
        protected Boolean doInBackground(Void... voidArr) {
            try {
                try {
                    Cursor cursorQuery = ProviderStatusWatcher.this.mContext.getContentResolver().query(ContactsContract.ProviderStatus.CONTENT_URI, ProviderStatusWatcher.PROJECTION, null, null, null);
                    if (cursorQuery != null) {
                        try {
                            if (cursorQuery.moveToFirst()) {
                                ProviderStatusWatcher.this.mProviderStatus = Integer.valueOf(cursorQuery.getInt(0));
                                Log.d("ProviderStatusWatcher", "[LoaderTask]mProviderStatus=" + ProviderStatusWatcher.this.mProviderStatus);
                                synchronized (ProviderStatusWatcher.this.mSignal) {
                                    ProviderStatusWatcher.this.mSignal.notifyAll();
                                }
                                return true;
                            }
                            Log.e("ProviderStatusWatcher", "[LoaderTask]cursor is empty");
                            cursorQuery.close();
                        } finally {
                            cursorQuery.close();
                        }
                    }
                    Log.e("ProviderStatusWatcher", "[LoaderTask]cursor is null");
                    synchronized (ProviderStatusWatcher.this.mSignal) {
                        ProviderStatusWatcher.this.mSignal.notifyAll();
                    }
                    return false;
                } catch (SecurityException e) {
                    FeedbackHelper.sendFeedback(ProviderStatusWatcher.this.mContext, "ProviderStatusWatcher", "Security exception when querying provider status", e);
                    synchronized (ProviderStatusWatcher.this.mSignal) {
                        ProviderStatusWatcher.this.mSignal.notifyAll();
                        return false;
                    }
                }
            } catch (Throwable th) {
                synchronized (ProviderStatusWatcher.this.mSignal) {
                    ProviderStatusWatcher.this.mSignal.notifyAll();
                    throw th;
                }
            }
        }

        @Override
        protected void onCancelled(Boolean bool) {
            cleanUp();
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            cleanUp();
            if (bool != null && bool.booleanValue()) {
                ProviderStatusWatcher.this.notifyListeners();
            }
        }

        private void cleanUp() {
            ProviderStatusWatcher.this.mLoaderTask = null;
        }
    }

    @Override
    public void onChange(boolean z, Uri uri) {
        if (ContactsContract.ProviderStatus.CONTENT_URI.equals(uri)) {
            Log.i("ProviderStatusWatcher", "Provider status changed.");
            this.mHandler.removeCallbacks(this.mStartLoadingRunnable);
            this.mHandler.post(this.mStartLoadingRunnable);
        }
    }
}
