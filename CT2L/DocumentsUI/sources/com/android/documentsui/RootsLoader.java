package com.android.documentsui;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import com.android.documentsui.DocumentsActivity;
import com.android.documentsui.model.RootInfo;
import java.util.Collection;

public class RootsLoader extends AsyncTaskLoader<Collection<RootInfo>> {
    private final Loader<Collection<RootInfo>>.ForceLoadContentObserver mObserver;
    private Collection<RootInfo> mResult;
    private final RootsCache mRoots;
    private final DocumentsActivity.State mState;

    public RootsLoader(Context context, RootsCache roots, DocumentsActivity.State state) {
        super(context);
        this.mObserver = new Loader.ForceLoadContentObserver(this);
        this.mRoots = roots;
        this.mState = state;
        getContext().getContentResolver().registerContentObserver(RootsCache.sNotificationUri, false, this.mObserver);
    }

    @Override
    public final Collection<RootInfo> loadInBackground() {
        return this.mRoots.getMatchingRootsBlocking(this.mState);
    }

    @Override
    public void deliverResult(Collection<RootInfo> result) {
        if (!isReset()) {
            Collection<RootInfo> collection = this.mResult;
            this.mResult = result;
            if (isStarted()) {
                super.deliverResult(result);
            }
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
        getContext().getContentResolver().unregisterContentObserver(this.mObserver);
    }
}
