package com.android.contacts.util.concurrent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mediatek.contacts.util.Log;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;

public abstract class ListenableFutureLoader<D> extends Loader<D> {
    private ListenableFuture<D> mFuture;
    private D mLoadedData;
    private final LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mReceiver;
    private final IntentFilter mReloadFilter;
    private final Executor mUiExecutor;

    protected abstract ListenableFuture<D> loadData();

    public ListenableFutureLoader(Context context, IntentFilter intentFilter) {
        super(context);
        this.mUiExecutor = ContactsExecutors.newUiThreadExecutor();
        this.mReloadFilter = intentFilter;
        this.mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    protected void onStartLoading() {
        if (this.mReloadFilter != null && this.mReceiver == null) {
            this.mReceiver = new ForceLoadReceiver();
            this.mLocalBroadcastManager.registerReceiver(this.mReceiver, this.mReloadFilter);
        }
        if (this.mLoadedData != null) {
            deliverResult(this.mLoadedData);
        }
        if (this.mFuture == null) {
            takeContentChanged();
            forceLoad();
        } else if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    protected void onForceLoad() {
        this.mFuture = loadData();
        Futures.addCallback(this.mFuture, new FutureCallback<D>() {
            @Override
            public void onSuccess(D d) {
                if (ListenableFutureLoader.this.mLoadedData == null || !ListenableFutureLoader.this.isSameData(ListenableFutureLoader.this.mLoadedData, d)) {
                    ListenableFutureLoader.this.deliverResult(d);
                }
                ListenableFutureLoader.this.mLoadedData = d;
                ListenableFutureLoader.this.commitContentChanged();
            }

            @Override
            public void onFailure(Throwable th) {
                if (th instanceof CancellationException) {
                    Log.i("FutureLoader", "Loading cancelled", th);
                    ListenableFutureLoader.this.rollbackContentChanged();
                } else {
                    Log.e("FutureLoader", "Loading failed", th);
                }
            }
        }, this.mUiExecutor);
    }

    @Override
    protected void onStopLoading() {
        if (this.mFuture != null) {
            this.mFuture.cancel(false);
            this.mFuture = null;
        }
    }

    @Override
    protected void onReset() {
        this.mFuture = null;
        this.mLoadedData = null;
        if (this.mReceiver != null) {
            this.mLocalBroadcastManager.unregisterReceiver(this.mReceiver);
        }
    }

    protected boolean isSameData(D d, D d2) {
        return false;
    }

    public class ForceLoadReceiver extends BroadcastReceiver {
        public ForceLoadReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ListenableFutureLoader.this.onContentChanged();
        }
    }
}
