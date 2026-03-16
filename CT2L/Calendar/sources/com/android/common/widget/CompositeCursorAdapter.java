package com.android.common.widget;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.ArrayList;

public abstract class CompositeCursorAdapter extends BaseAdapter {
    private boolean mCacheValid;
    private final Context mContext;
    private int mCount;
    private boolean mNotificationNeeded;
    private boolean mNotificationsEnabled;
    private ArrayList<Partition> mPartitions;

    protected abstract void bindView(View view, int i, Cursor cursor, int i2);

    protected abstract View newView(Context context, int i, Cursor cursor, int i2, ViewGroup viewGroup);

    public static class Partition {
        int count;
        Cursor cursor;
        boolean hasHeader;
        int idColumnIndex;
        boolean showIfEmpty;

        public Partition(boolean showIfEmpty, boolean hasHeader) {
            this.showIfEmpty = showIfEmpty;
            this.hasHeader = hasHeader;
        }
    }

    public CompositeCursorAdapter(Context context) {
        this(context, 2);
    }

    public CompositeCursorAdapter(Context context, int initialCapacity) {
        this.mCount = 0;
        this.mCacheValid = true;
        this.mNotificationsEnabled = true;
        this.mContext = context;
        this.mPartitions = new ArrayList<>();
    }

    public Context getContext() {
        return this.mContext;
    }

    public void addPartition(Partition partition) {
        this.mPartitions.add(partition);
        invalidate();
        notifyDataSetChanged();
    }

    public void close() {
        for (Partition partition : this.mPartitions) {
            Cursor cursor = partition.cursor;
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        this.mPartitions.clear();
        invalidate();
        notifyDataSetChanged();
    }

    public Partition getPartition(int partitionIndex) {
        return this.mPartitions.get(partitionIndex);
    }

    protected void invalidate() {
        this.mCacheValid = false;
    }

    public int getPartitionCount() {
        return this.mPartitions.size();
    }

    protected void ensureCacheValid() {
        int count;
        if (!this.mCacheValid) {
            this.mCount = 0;
            for (Partition partition : this.mPartitions) {
                Cursor cursor = partition.cursor;
                if (cursor == null || cursor.isClosed()) {
                    count = 0;
                } else {
                    count = cursor.getCount();
                }
                if (partition.hasHeader && (count != 0 || partition.showIfEmpty)) {
                    count++;
                }
                partition.count = count;
                this.mCount += count;
            }
            this.mCacheValid = true;
        }
    }

    @Override
    public int getCount() {
        ensureCacheValid();
        return this.mCount;
    }

    public Cursor getCursor(int partition) {
        return this.mPartitions.get(partition).cursor;
    }

    public void changeCursor(int partition, Cursor cursor) {
        Cursor prevCursor = this.mPartitions.get(partition).cursor;
        if (prevCursor != cursor) {
            if (prevCursor != null && !prevCursor.isClosed()) {
                prevCursor.close();
            }
            this.mPartitions.get(partition).cursor = cursor;
            if (cursor != null) {
                this.mPartitions.get(partition).idColumnIndex = cursor.getColumnIndex("_id");
            }
            invalidate();
            notifyDataSetChanged();
        }
    }

    @Override
    public int getViewTypeCount() {
        return getItemViewTypeCount() + 1;
    }

    public int getItemViewTypeCount() {
        return 1;
    }

    protected int getItemViewType(int partition, int position) {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        ensureCacheValid();
        int start = 0;
        int n = this.mPartitions.size();
        for (int i = 0; i < n; i++) {
            int end = start + this.mPartitions.get(i).count;
            if (position >= start && position < end) {
                int offset = position - start;
                if (this.mPartitions.get(i).hasHeader) {
                    offset--;
                }
                if (offset == -1) {
                    return -1;
                }
                return getItemViewType(i, offset);
            }
            start = end;
        }
        throw new ArrayIndexOutOfBoundsException(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        ensureCacheValid();
        int start = 0;
        int n = this.mPartitions.size();
        for (int i = 0; i < n; i++) {
            int end = start + this.mPartitions.get(i).count;
            if (position >= start && position < end) {
                int offset = position - start;
                if (this.mPartitions.get(i).hasHeader) {
                    offset--;
                }
                if (offset == -1) {
                    view = getHeaderView(i, this.mPartitions.get(i).cursor, convertView, parent);
                } else {
                    if (!this.mPartitions.get(i).cursor.moveToPosition(offset)) {
                        throw new IllegalStateException("Couldn't move cursor to position " + offset);
                    }
                    view = getView(i, this.mPartitions.get(i).cursor, offset, convertView, parent);
                }
                if (view == null) {
                    throw new NullPointerException("View should not be null, partition: " + i + " position: " + offset);
                }
                return view;
            }
            start = end;
        }
        throw new ArrayIndexOutOfBoundsException(position);
    }

    protected View getHeaderView(int partition, Cursor cursor, View convertView, ViewGroup parent) {
        View view = convertView != null ? convertView : newHeaderView(this.mContext, partition, cursor, parent);
        bindHeaderView(view, partition, cursor);
        return view;
    }

    protected View newHeaderView(Context context, int partition, Cursor cursor, ViewGroup parent) {
        return null;
    }

    protected void bindHeaderView(View view, int partition, Cursor cursor) {
    }

    protected View getView(int partition, Cursor cursor, int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = newView(this.mContext, partition, cursor, position, parent);
        }
        bindView(view, partition, cursor, position);
        return view;
    }

    @Override
    public Object getItem(int position) {
        ensureCacheValid();
        int start = 0;
        for (Partition mPartition : this.mPartitions) {
            int end = start + mPartition.count;
            if (position >= start && position < end) {
                int offset = position - start;
                if (mPartition.hasHeader) {
                    offset--;
                }
                if (offset == -1) {
                    return null;
                }
                Cursor cursor = mPartition.cursor;
                if (cursor == null || cursor.isClosed() || !cursor.moveToPosition(offset)) {
                    return null;
                }
                return cursor;
            }
            start = end;
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        Cursor cursor;
        ensureCacheValid();
        int start = 0;
        for (Partition mPartition : this.mPartitions) {
            int end = start + mPartition.count;
            if (position >= start && position < end) {
                int offset = position - start;
                if (mPartition.hasHeader) {
                    offset--;
                }
                if (offset == -1 || mPartition.idColumnIndex == -1 || (cursor = mPartition.cursor) == null || cursor.isClosed() || !cursor.moveToPosition(offset)) {
                    return 0L;
                }
                return cursor.getLong(mPartition.idColumnIndex);
            }
            start = end;
        }
        return 0L;
    }

    @Override
    public boolean areAllItemsEnabled() {
        for (Partition mPartition : this.mPartitions) {
            if (mPartition.hasHeader) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        ensureCacheValid();
        int start = 0;
        int n = this.mPartitions.size();
        for (int i = 0; i < n; i++) {
            int end = start + this.mPartitions.get(i).count;
            if (position >= start && position < end) {
                int offset = position - start;
                if (this.mPartitions.get(i).hasHeader && offset == 0) {
                    return false;
                }
                return isEnabled(i, offset);
            }
            start = end;
        }
        return false;
    }

    protected boolean isEnabled(int partition, int position) {
        return true;
    }

    public void setNotificationsEnabled(boolean flag) {
        this.mNotificationsEnabled = flag;
        if (flag && this.mNotificationNeeded) {
            notifyDataSetChanged();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        if (this.mNotificationsEnabled) {
            this.mNotificationNeeded = false;
            super.notifyDataSetChanged();
        } else {
            this.mNotificationNeeded = true;
        }
    }
}
