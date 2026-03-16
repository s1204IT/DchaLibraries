package com.android.browser.util;

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
    private HandlerThread mThread;

    public abstract void bindView(View view, T t);

    protected abstract long getItemId(Cursor cursor);

    public abstract T getLoadingObject();

    public abstract T getRowObject(Cursor cursor, T t);

    public abstract View newView(Context context, ViewGroup viewGroup);

    static long access$108(ThreadedCursorAdapter x0) {
        long j = x0.mGeneration;
        x0.mGeneration = 1 + j;
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

    public void clearThread() {
        if (this.mLoadHandler != null) {
            this.mLoadHandler.getLooper().quit();
        }
        if (this.mThread != null) {
            this.mThread.quit();
            this.mThread = null;
        }
    }

    public ThreadedCursorAdapter(Context context, Cursor c) {
        int i = 0;
        this.mContext = context;
        this.mHasCursor = c != null;
        this.mGeneration = 0L;
        this.mCursorAdapter = new CursorAdapter(context, c, i) {
            @Override
            public View newView(Context context2, Cursor cursor, ViewGroup parent) {
                throw new IllegalStateException("not supported");
            }

            @Override
            public void bindView(View view, Context context2, Cursor cursor) {
                throw new IllegalStateException("not supported");
            }

            @Override
            public void notifyDataSetChanged() {
                super.notifyDataSetChanged();
                ThreadedCursorAdapter.this.mSize = getCount();
                ThreadedCursorAdapter.access$108(ThreadedCursorAdapter.this);
                ThreadedCursorAdapter.this.notifyDataSetChanged();
            }

            @Override
            public void notifyDataSetInvalidated() {
                super.notifyDataSetInvalidated();
                ThreadedCursorAdapter.this.mSize = getCount();
                ThreadedCursorAdapter.access$108(ThreadedCursorAdapter.this);
                ThreadedCursorAdapter.this.notifyDataSetInvalidated();
            }
        };
        this.mSize = this.mCursorAdapter.getCount();
        this.mThread = new HandlerThread("threaded_adapter_" + this, 10);
        this.mThread.start();
        this.mLoadHandler = new Handler(this.mThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                ThreadedCursorAdapter.this.loadRowObject(msg.what, (LoadContainer) msg.obj);
            }
        };
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                View view;
                ThreadedCursorAdapter<T>.LoadContainer container = (LoadContainer) msg.obj;
                if (container != null && (view = container.view.get()) != null && container.owner == ThreadedCursorAdapter.this && container.position == msg.what && view.getWindowToken() != null && container.generation == ThreadedCursorAdapter.this.mGeneration) {
                    container.loaded = true;
                    ThreadedCursorAdapter.this.bindView(view, container.bind_object);
                }
            }
        };
    }

    @Override
    public int getCount() {
        return this.mSize;
    }

    @Override
    public Cursor getItem(int position) {
        return (Cursor) this.mCursorAdapter.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        long itemId;
        synchronized (this.mCursorLock) {
            itemId = getItemId(getItem(position));
        }
        return itemId;
    }

    private void loadRowObject(int position, ThreadedCursorAdapter<T>.LoadContainer container) {
        if (container != null && container.position == position && container.owner == this && container.view.get() != null) {
            synchronized (this.mCursorLock) {
                Cursor c = (Cursor) this.mCursorAdapter.getItem(position);
                if (c != null && !c.isClosed()) {
                    container.bind_object = getRowObject(c, container.bind_object);
                    this.mHandler.obtainMessage(position, container).sendToTarget();
                }
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = newView(this.mContext, parent);
        }
        ThreadedCursorAdapter<T>.LoadContainer container = (LoadContainer) convertView.getTag(R.id.load_object);
        if (container == null) {
            container = new LoadContainer();
            container.view = new WeakReference<>(convertView);
            convertView.setTag(R.id.load_object, container);
        }
        if (container.position == position && container.owner == this && container.loaded && container.generation == this.mGeneration) {
            bindView(convertView, container.bind_object);
        } else {
            bindView(convertView, cachedLoadObject());
            if (this.mHasCursor) {
                container.position = position;
                container.loaded = false;
                container.owner = this;
                container.generation = this.mGeneration;
                this.mLoadHandler.obtainMessage(position, container).sendToTarget();
            }
        }
        return convertView;
    }

    private T cachedLoadObject() {
        if (this.mLoadingObject == null) {
            this.mLoadingObject = getLoadingObject();
        }
        return this.mLoadingObject;
    }

    public void simpleChangeCursor(Cursor cursor) {
        if (this.mGeneration > 0) {
            this.mGeneration--;
        }
        changeCursor(cursor);
    }

    public void changeCursor(Cursor cursor) {
        this.mLoadHandler.removeCallbacksAndMessages(null);
        this.mHandler.removeCallbacksAndMessages(null);
        synchronized (this.mCursorLock) {
            this.mHasCursor = cursor != null;
            this.mCursorAdapter.changeCursor(cursor);
        }
    }
}
