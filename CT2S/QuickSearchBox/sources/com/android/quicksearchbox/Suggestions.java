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

    public Suggestions(String query, Source source) {
        this.mQuery = query;
        this.mSource = source;
    }

    public void acquire() {
        this.mRefCount++;
    }

    public void release() {
        this.mRefCount--;
        if (this.mRefCount <= 0) {
            close();
        }
    }

    public void done() {
        this.mDone = true;
    }

    public boolean isDone() {
        return this.mDone || this.mResult != null;
    }

    public void addResults(SourceResult result) {
        if (isClosed()) {
            result.close();
        } else {
            if (!this.mQuery.equals(result.getUserQuery())) {
                throw new IllegalArgumentException("Got result for wrong query: " + this.mQuery + " != " + result.getUserQuery());
            }
            this.mResult = result;
            notifyDataSetChanged();
        }
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        if (this.mClosed) {
            throw new IllegalStateException("registerDataSetObserver() when closed");
        }
        this.mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        this.mDataSetObservable.unregisterObserver(observer);
    }

    protected void notifyDataSetChanged() {
        this.mDataSetObservable.notifyChanged();
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

    public boolean isClosed() {
        return this.mClosed;
    }

    protected void finalize() {
        if (!this.mClosed) {
            Log.e("QSB.Suggestions", "LEAK! Finalized without being closed: Suggestions[" + getQuery() + "]");
        }
    }

    public String getQuery() {
        return this.mQuery;
    }

    public SourceResult getResult() {
        return this.mResult;
    }

    public SourceResult getWebResult() {
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

    public String toString() {
        return "Suggestions@" + hashCode() + "{source=" + this.mSource + ",getResultCount()=" + getResultCount() + "}";
    }
}
