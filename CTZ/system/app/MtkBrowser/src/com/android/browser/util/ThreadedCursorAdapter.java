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
import com.android.browser.R;
import java.lang.ref.WeakReference;

/* loaded from: classes.dex */
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

    public abstract void bindView(View view, T t);

    protected abstract long getItemId(Cursor cursor);

    public abstract T getLoadingObject();

    public abstract T getRowObject(Cursor cursor, T t);

    public abstract View newView(Context context, ViewGroup viewGroup);

    static /* synthetic */ long access$108(ThreadedCursorAdapter threadedCursorAdapter) {
        long j = threadedCursorAdapter.mGeneration;
        threadedCursorAdapter.mGeneration = 1 + j;
        return j;
    }

    private class LoadContainer {
        T bind_object;
        long generation;
        boolean loaded;
        Adapter owner;
        int position;
        WeakReference<View> view;

        private LoadContainer() {
        }
    }

    public ThreadedCursorAdapter(Context context, Cursor cursor) {
        this.mContext = context;
        int i = 0;
        this.mHasCursor = cursor != null;
        this.mCursorAdapter = new CursorAdapter(context, cursor, i) { // from class: com.android.browser.util.ThreadedCursorAdapter.1
            @Override // android.widget.CursorAdapter
            public View newView(Context context2, Cursor cursor2, ViewGroup viewGroup) {
                throw new IllegalStateException("not supported");
            }

            @Override // android.widget.CursorAdapter
            public void bindView(View view, Context context2, Cursor cursor2) {
                throw new IllegalStateException("not supported");
            }

            @Override // android.widget.BaseAdapter
            public void notifyDataSetChanged() {
                super.notifyDataSetChanged();
                ThreadedCursorAdapter.this.mSize = getCount();
                ThreadedCursorAdapter.access$108(ThreadedCursorAdapter.this);
                ThreadedCursorAdapter.this.notifyDataSetChanged();
            }

            @Override // android.widget.BaseAdapter
            public void notifyDataSetInvalidated() {
                super.notifyDataSetInvalidated();
                ThreadedCursorAdapter.this.mSize = getCount();
                ThreadedCursorAdapter.access$108(ThreadedCursorAdapter.this);
                ThreadedCursorAdapter.this.notifyDataSetInvalidated();
            }
        };
        this.mSize = this.mCursorAdapter.getCount();
        HandlerThread handlerThread = new HandlerThread("threaded_adapter_" + this, 10);
        handlerThread.start();
        this.mLoadHandler = new Handler(handlerThread.getLooper()) { // from class: com.android.browser.util.ThreadedCursorAdapter.2
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                ThreadedCursorAdapter.this.loadRowObject(message.what, (LoadContainer) message.obj);
            }
        };
        this.mHandler = new Handler() { // from class: com.android.browser.util.ThreadedCursorAdapter.3
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                View view;
                LoadContainer loadContainer = (LoadContainer) message.obj;
                if (loadContainer == null || (view = loadContainer.view.get()) == null || loadContainer.owner != ThreadedCursorAdapter.this || loadContainer.position != message.what || view.getWindowToken() == null || loadContainer.generation != ThreadedCursorAdapter.this.mGeneration) {
                    return;
                }
                loadContainer.loaded = true;
                ThreadedCursorAdapter.this.bindView(view, loadContainer.bind_object);
            }
        };
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.mSize;
    }

    /* JADX DEBUG: Method merged with bridge method: getItem(I)Ljava/lang/Object; */
    @Override // android.widget.Adapter
    public Cursor getItem(int i) {
        return (Cursor) this.mCursorAdapter.getItem(i);
    }

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        long itemId;
        synchronized (this.mCursorLock) {
            itemId = getItemId(getItem(i));
        }
        return itemId;
    }

    private void loadRowObject(int i, ThreadedCursorAdapter<T>.LoadContainer loadContainer) {
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

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = newView(this.mContext, viewGroup);
        }
        LoadContainer loadContainer = (LoadContainer) view.getTag(R.id.load_object);
        if (loadContainer == null) {
            loadContainer = new LoadContainer();
            loadContainer.view = new WeakReference<>(view);
            view.setTag(R.id.load_object, loadContainer);
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

    private T cachedLoadObject() {
        if (this.mLoadingObject == null) {
            this.mLoadingObject = getLoadingObject();
        }
        return this.mLoadingObject;
    }

    public void changeCursor(Cursor cursor) {
        this.mLoadHandler.removeCallbacksAndMessages(null);
        this.mHandler.removeCallbacksAndMessages(null);
        synchronized (this.mCursorLock) {
            this.mHasCursor = cursor != null;
            this.mCursorAdapter.changeCursor(cursor);
        }
    }

    public void releaseCursor(LoaderManager loaderManager, int i) {
        synchronized (this.mCursorLock) {
            changeCursor(null);
            loaderManager.destroyLoader(i);
        }
    }
}
