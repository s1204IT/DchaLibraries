package com.android.browser.util;

import android.app.LoaderManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import java.lang.ref.WeakReference;

public abstract class ThreadedCursorAdapter<T> extends BaseAdapter {
    private Context mContext;
    private CursorAdapter mCursorAdapter;
    private Object mCursorLock = new Object();
    private long mGeneration;
    private Handler mHandler;
    private boolean mHasCursor;
    private Handler mLoadHandler;
    private T mLoadingObject;
    private int mSize;

    private class LoadContainer {
        T bind_object;
        long generation;
        boolean loaded;
        Adapter owner;
        int position;
        final ThreadedCursorAdapter this$0;
        WeakReference<View> view;

        private LoadContainer(ThreadedCursorAdapter threadedCursorAdapter) {
            this.this$0 = threadedCursorAdapter;
        }
    }

    public ThreadedCursorAdapter(Context context, Cursor cursor) {
        int i = 0;
        this.mContext = context;
        this.mHasCursor = cursor != null;
        this.mCursorAdapter = new CursorAdapter(this, context, cursor, i) {
            final ThreadedCursorAdapter this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void bindView(View view, Context context2, Cursor cursor2) {
                throw new IllegalStateException("not supported");
            }

            @Override
            public View newView(Context context2, Cursor cursor2, ViewGroup viewGroup) {
                throw new IllegalStateException("not supported");
            }

            @Override
            public void notifyDataSetChanged() {
                super.notifyDataSetChanged();
                this.this$0.mSize = getCount();
                ThreadedCursorAdapter.access$108(this.this$0);
                this.this$0.notifyDataSetChanged();
            }

            @Override
            public void notifyDataSetInvalidated() {
                super.notifyDataSetInvalidated();
                this.this$0.mSize = getCount();
                ThreadedCursorAdapter.access$108(this.this$0);
                this.this$0.notifyDataSetInvalidated();
            }
        };
        this.mSize = this.mCursorAdapter.getCount();
        HandlerThread handlerThread = new HandlerThread("threaded_adapter_" + this, 10);
        handlerThread.start();
        this.mLoadHandler = new Handler(this, handlerThread.getLooper()) {
            final ThreadedCursorAdapter this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void handleMessage(Message message) {
                this.this$0.loadRowObject(message.what, (LoadContainer) message.obj);
            }
        };
        this.mHandler = new Handler(this) {
            final ThreadedCursorAdapter this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void handleMessage(Message message) {
                View view;
                LoadContainer loadContainer = (LoadContainer) message.obj;
                if (loadContainer != null && (view = loadContainer.view.get()) != null && loadContainer.owner == this.this$0 && loadContainer.position == message.what && view.getWindowToken() != null && loadContainer.generation == this.this$0.mGeneration) {
                    loadContainer.loaded = true;
                    this.this$0.bindView(view, loadContainer.bind_object);
                }
            }
        };
    }

    static long access$108(ThreadedCursorAdapter threadedCursorAdapter) {
        long j = threadedCursorAdapter.mGeneration;
        threadedCursorAdapter.mGeneration = 1 + j;
        return j;
    }

    private T cachedLoadObject() {
        if (this.mLoadingObject == null) {
            this.mLoadingObject = getLoadingObject();
        }
        return this.mLoadingObject;
    }

    public void loadRowObject(int i, ThreadedCursorAdapter<T>.LoadContainer loadContainer) {
        if (loadContainer == null || loadContainer.position != i || loadContainer.owner != this || loadContainer.view.get() == null) {
            return;
        }
        synchronized (this.mCursorLock) {
            Cursor cursor = (Cursor) this.mCursorAdapter.getItem(i);
            if (cursor != null && !cursor.isClosed()) {
                loadContainer.bind_object = getRowObject(cursor, loadContainer.bind_object);
                this.mHandler.obtainMessage(i, loadContainer).sendToTarget();
            }
        }
    }

    public abstract void bindView(View view, T t);

    public void changeCursor(Cursor cursor) {
        this.mLoadHandler.removeCallbacksAndMessages(null);
        this.mHandler.removeCallbacksAndMessages(null);
        synchronized (this.mCursorLock) {
            this.mHasCursor = cursor != null;
            this.mCursorAdapter.changeCursor(cursor);
        }
    }

    @Override
    public int getCount() {
        return this.mSize;
    }

    @Override
    public Cursor getItem(int i) {
        return (Cursor) this.mCursorAdapter.getItem(i);
    }

    @Override
    public long getItemId(int i) {
        long itemId;
        synchronized (this.mCursorLock) {
            itemId = getItemId(getItem(i));
        }
        return itemId;
    }

    protected abstract long getItemId(Cursor cursor);

    public abstract T getLoadingObject();

    public abstract T getRowObject(Cursor cursor, T t);

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = newView(this.mContext, viewGroup);
        }
        LoadContainer loadContainer = (LoadContainer) view.getTag(2131558405);
        if (loadContainer == null) {
            loadContainer = new LoadContainer();
            loadContainer.view = new WeakReference<>(view);
            view.setTag(2131558405, loadContainer);
        }
        if (loadContainer.position == i && loadContainer.owner == this && loadContainer.loaded && loadContainer.generation == this.mGeneration) {
            bindView(view, loadContainer.bind_object);
        } else {
            bindView(view, cachedLoadObject());
            if (this.mHasCursor) {
                loadContainer.position = i;
                loadContainer.loaded = false;
                loadContainer.owner = this;
                loadContainer.generation = this.mGeneration;
                this.mLoadHandler.obtainMessage(i, loadContainer).sendToTarget();
            }
        }
        return view;
    }

    public abstract View newView(Context context, ViewGroup viewGroup);

    public void releaseCursor(LoaderManager loaderManager, int i) {
        synchronized (this.mCursorLock) {
            changeCursor(null);
            loaderManager.destroyLoader(i);
        }
    }
}
