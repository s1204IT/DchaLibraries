package com.android.documentsui.roots;

import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.State;
import java.util.Collection;

public class RootsLoader extends AsyncTaskLoader<Collection<RootInfo>> {
    private final ProvidersCache mProviders;
    private final BroadcastReceiver mReceiver;
    private Collection<RootInfo> mResult;
    private final State mState;

    public RootsLoader(Context context, ProvidersCache providersCache, State state) {
        super(context);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                RootsLoader.this.onContentChanged();
            }
        };
        this.mProviders = providersCache;
        this.mState = state;
        LocalBroadcastManager.getInstance(context).registerReceiver(this.mReceiver, new IntentFilter("com.android.documentsui.action.ROOT_CHANGED"));
    }

    @Override
    public final Collection<RootInfo> loadInBackground() {
        return this.mProviders.getMatchingRootsBlocking(this.mState);
    }

    @Override
    public void deliverResult(Collection<RootInfo> collection) {
        if (isReset()) {
            return;
        }
        this.mResult = collection;
        if (isStarted()) {
            super.deliverResult(collection);
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
    protected void onReset() {
        super.onReset();
        onStopLoading();
        this.mResult = null;
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(this.mReceiver);
    }
}
