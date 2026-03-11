package com.android.quicksearchbox;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.Log;

public class Suggestions {
    protected final String mQuery;
    private SourceResult mResult;
    private Source mSource;
    private boolean mClosed = false;
    private final DataSetObservable mDataSetObservable = new DataSetObservable();
    private int mRefCount = 0;
    private boolean mDone = false;

    public Suggestions(String str, Source source) {
        this.mQuery = str;
        this.mSource = source;
    }

    private void close() {
        if (this.mClosed) {
            throw new IllegalStateException("Double close()");
        }
        this.mClosed = true;
        this.mDataSetObservable.unregisterAll();
        if (this.mResult != null) {
            this.mResult.close();
        }
        this.mResult = null;
    }

    public void acquire() {
        this.mRefCount++;
    }

    public void addResults(SourceResult sourceResult) {
        if (isClosed()) {
            sourceResult.close();
            return;
        }
        if (this.mQuery.equals(sourceResult.getUserQuery())) {
            this.mResult = sourceResult;
            notifyDataSetChanged();
            return;
        }
        throw new IllegalArgumentException("Got result for wrong query: " + this.mQuery + " != " + sourceResult.getUserQuery());
    }

    public void done() {
        this.mDone = true;
    }

    protected void finalize() {
        if (this.mClosed) {
            return;
        }
        Log.e("QSB.Suggestions", "LEAK! Finalized without being closed: Suggestions[" + getQuery() + "]");
    }

    public String getQuery() {
        return this.mQuery;
    }

    public SourceResult getResult() {
        return this.mResult;
    }

    public int getResultCount() {
        if (isClosed()) {
            throw new IllegalStateException("Called getSourceCount() when closed.");
        }
        if (this.mResult == null) {
            return 0;
        }
        return this.mResult.getCount();
    }

    public SourceResult getWebResult() {
        return this.mResult;
    }

    public boolean isClosed() {
        return this.mClosed;
    }

    public boolean isDone() {
        return this.mDone || this.mResult != null;
    }

    protected void notifyDataSetChanged() {
        this.mDataSetObservable.notifyChanged();
    }

    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        if (this.mClosed) {
            throw new IllegalStateException("registerDataSetObserver() when closed");
        }
        this.mDataSetObservable.registerObserver(dataSetObserver);
    }

    public void release() {
        this.mRefCount--;
        if (this.mRefCount <= 0) {
            close();
        }
    }

    public String toString() {
        return "Suggestions@" + hashCode() + "{source=" + this.mSource + ",getResultCount()=" + getResultCount() + "}";
    }

    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        this.mDataSetObservable.unregisterObserver(dataSetObserver);
    }
}
