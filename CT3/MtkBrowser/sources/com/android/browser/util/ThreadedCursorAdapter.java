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

    private class LoadContainer {
        T bind_object;
        long generation;
        boolean loaded;
        Adapter owner;
        int position;
        WeakReference<View> view;

        LoadContainer(ThreadedCursorAdapter this$0, LoadContainer loadContainer) {
            this();
        }

        private LoadContainer() {
        }
    }

    public ThreadedCursorAdapter(Context context, Cursor c) {
        int i = 0;
        this.mContext = context;
        this.mHasCursor = c != null;
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
                ThreadedCursorAdapter.this.mGeneration++;
                ThreadedCursorAdapter.this.notifyDataSetChanged();
            }

            @Override
            public void notifyDataSetInvalidated() {
                super.notifyDataSetInvalidated();
                ThreadedCursorAdapter.this.mSize = getCount();
                ThreadedCursorAdapter.this.mGeneration++;
                ThreadedCursorAdapter.this.notifyDataSetInvalidated();
            }
        };
        this.mSize = this.mCursorAdapter.getCount();
        HandlerThread thread = new HandlerThread("threaded_adapter_" + this, 10);
        thread.start();
        this.mLoadHandler = new Handler(thread.getLooper()) {
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
                if (container == null || (view = container.view.get()) == null || container.owner != ThreadedCursorAdapter.this || container.position != msg.what || view.getWindowToken() == null || container.generation != ThreadedCursorAdapter.this.mGeneration) {
                    return;
                }
                container.loaded = true;
                ThreadedCursorAdapter.this.bindView(view, container.bind_object);
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

    public void loadRowObject(int position, ThreadedCursorAdapter<T>.LoadContainer container) {
        if (container == null || container.position != position || container.owner != this || container.view.get() == null) {
            return;
        }
        synchronized (this.mCursorLock) {
            Cursor c = (Cursor) this.mCursorAdapter.getItem(position);
            if (c == null || c.isClosed()) {
                return;
            }
            container.bind_object = getRowObject(c, container.bind_object);
            this.mHandler.obtainMessage(position, container).sendToTarget();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LoadContainer loadContainer = null;
        if (convertView == null) {
            convertView = newView(this.mContext, parent);
        }
        ThreadedCursorAdapter<T>.LoadContainer container = (LoadContainer) convertView.getTag(R.id.load_object);
        if (container == null) {
            container = new LoadContainer(this, loadContainer);
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

    public void changeCursor(Cursor cursor) {
        this.mLoadHandler.removeCallbacksAndMessages(null);
        this.mHandler.removeCallbacksAndMessages(null);
        synchronized (this.mCursorLock) {
            this.mHasCursor = cursor != null;
            this.mCursorAdapter.changeCursor(cursor);
        }
    }

    public void releaseCursor(LoaderManager lm, int id) {
        synchronized (this.mCursorLock) {
            changeCursor(null);
            lm.destroyLoader(id);
        }
    }
}
