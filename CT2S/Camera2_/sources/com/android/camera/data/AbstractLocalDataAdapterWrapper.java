package com.android.camera.data;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import com.android.camera.data.LocalDataAdapter;
import com.android.camera.filmstrip.DataAdapter;
import com.android.camera.util.Callback;
import java.util.List;

public abstract class AbstractLocalDataAdapterWrapper implements LocalDataAdapter {
    protected final LocalDataAdapter mAdapter;
    protected final Context mContext;
    protected int mSuggestedHeight;
    protected int mSuggestedWidth;

    AbstractLocalDataAdapterWrapper(Context context, LocalDataAdapter wrappedAdapter) {
        if (wrappedAdapter == null) {
            throw new AssertionError("data adapter is null");
        }
        this.mContext = context;
        this.mAdapter = wrappedAdapter;
    }

    @Override
    public void suggestViewSizeBound(int w, int h) {
        this.mSuggestedWidth = w;
        this.mSuggestedHeight = h;
        this.mAdapter.suggestViewSizeBound(w, h);
    }

    @Override
    public void setListener(DataAdapter.Listener listener) {
        this.mAdapter.setListener(listener);
    }

    @Override
    public void setLocalDataListener(LocalDataAdapter.LocalDataListener listener) {
        this.mAdapter.setLocalDataListener(listener);
    }

    @Override
    public void requestLoad(Callback<Void> doneCallback) {
        this.mAdapter.requestLoad(doneCallback);
    }

    @Override
    public void requestLoadNewPhotos() {
        this.mAdapter.requestLoadNewPhotos();
    }

    @Override
    public boolean addData(LocalData data) {
        return this.mAdapter.addData(data);
    }

    @Override
    public void flush() {
        this.mAdapter.flush();
    }

    @Override
    public boolean executeDeletion() {
        return this.mAdapter.executeDeletion();
    }

    @Override
    public boolean undoDataRemoval() {
        return this.mAdapter.undoDataRemoval();
    }

    @Override
    public void refresh(Uri uri) {
        this.mAdapter.refresh(uri);
    }

    @Override
    public AsyncTask updateMetadata(int dataId) {
        return this.mAdapter.updateMetadata(dataId);
    }

    @Override
    public boolean isMetadataUpdated(int dataId) {
        return this.mAdapter.isMetadataUpdated(dataId);
    }

    @Override
    public List<AsyncTask> preloadItems(List<Integer> items) {
        return this.mAdapter.preloadItems(items);
    }

    @Override
    public void cancelItems(List<AsyncTask> loadTokens) {
        this.mAdapter.cancelItems(loadTokens);
    }

    @Override
    public List<Integer> getItemsInRange(int startPosition, int endPosition) {
        return this.mAdapter.getItemsInRange(startPosition, endPosition);
    }

    @Override
    public int getCount() {
        return this.mAdapter.getCount();
    }
}
