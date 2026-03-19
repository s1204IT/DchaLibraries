package com.android.common.widget;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.ArrayList;
import java.util.Iterator;

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

        public Partition(boolean z, boolean z2) {
            this.showIfEmpty = z;
            this.hasHeader = z2;
        }
    }

    public CompositeCursorAdapter(Context context) {
        this(context, 2);
    }

    public CompositeCursorAdapter(Context context, int i) {
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
        Iterator<Partition> it = this.mPartitions.iterator();
        while (it.hasNext()) {
            Cursor cursor = it.next().cursor;
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        this.mPartitions.clear();
        invalidate();
        notifyDataSetChanged();
    }

    public Partition getPartition(int i) {
        return this.mPartitions.get(i);
    }

    protected void invalidate() {
        this.mCacheValid = false;
    }

    public int getPartitionCount() {
        return this.mPartitions.size();
    }

    protected void ensureCacheValid() {
        int count;
        if (this.mCacheValid) {
            return;
        }
        this.mCount = 0;
        for (Partition partition : this.mPartitions) {
            Cursor cursor = partition.cursor;
            if (cursor != null && !cursor.isClosed()) {
                count = cursor.getCount();
            } else {
                count = 0;
            }
            if (partition.hasHeader && (count != 0 || partition.showIfEmpty)) {
                count++;
            }
            partition.count = count;
            this.mCount += count;
        }
        this.mCacheValid = true;
    }

    @Override
    public int getCount() {
        ensureCacheValid();
        return this.mCount;
    }

    public Cursor getCursor(int i) {
        return this.mPartitions.get(i).cursor;
    }

    public void changeCursor(int i, Cursor cursor) {
        Cursor cursor2 = this.mPartitions.get(i).cursor;
        if (cursor2 != cursor) {
            if (cursor2 != null && !cursor2.isClosed()) {
                cursor2.close();
            }
            this.mPartitions.get(i).cursor = cursor;
            if (cursor != null && !cursor.isClosed()) {
                this.mPartitions.get(i).idColumnIndex = cursor.getColumnIndex("_id");
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

    protected int getItemViewType(int i, int i2) {
        return 1;
    }

    @Override
    public int getItemViewType(int i) {
        ensureCacheValid();
        int size = this.mPartitions.size();
        int i2 = 0;
        int i3 = 0;
        while (i2 < size) {
            int i4 = this.mPartitions.get(i2).count + i3;
            if (i < i3 || i >= i4) {
                i2++;
                i3 = i4;
            } else {
                int i5 = i - i3;
                if (this.mPartitions.get(i2).hasHeader) {
                    i5--;
                }
                if (i5 == -1) {
                    return -1;
                }
                return getItemViewType(i2, i5);
            }
        }
        throw new ArrayIndexOutOfBoundsException(i);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View view2;
        ensureCacheValid();
        int size = this.mPartitions.size();
        int i2 = 0;
        int i3 = 0;
        while (i2 < size) {
            int i4 = this.mPartitions.get(i2).count + i3;
            if (i < i3 || i >= i4) {
                i2++;
                i3 = i4;
            } else {
                int i5 = i - i3;
                if (this.mPartitions.get(i2).hasHeader) {
                    i5--;
                }
                if (i5 == -1) {
                    view2 = getHeaderView(i2, this.mPartitions.get(i2).cursor, view, viewGroup);
                } else {
                    if (!this.mPartitions.get(i2).cursor.moveToPosition(i5)) {
                        throw new IllegalStateException("Couldn't move cursor to position " + i5);
                    }
                    view2 = getView(i2, this.mPartitions.get(i2).cursor, i5, view, viewGroup);
                }
                if (view2 == null) {
                    throw new NullPointerException("View should not be null, partition: " + i2 + " position: " + i5);
                }
                return view2;
            }
        }
        throw new ArrayIndexOutOfBoundsException(i);
    }

    protected View getHeaderView(int i, Cursor cursor, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = newHeaderView(this.mContext, i, cursor, viewGroup);
        }
        bindHeaderView(view, i, cursor);
        return view;
    }

    protected View newHeaderView(Context context, int i, Cursor cursor, ViewGroup viewGroup) {
        return null;
    }

    protected void bindHeaderView(View view, int i, Cursor cursor) {
    }

    protected View getView(int i, Cursor cursor, int i2, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = newView(this.mContext, i, cursor, i2, viewGroup);
        }
        bindView(view, i, cursor, i2);
        return view;
    }

    @Override
    public Object getItem(int i) {
        Cursor cursor;
        ensureCacheValid();
        int i2 = 0;
        for (Partition partition : this.mPartitions) {
            int i3 = partition.count + i2;
            if (i < i2 || i >= i3) {
                i2 = i3;
            } else {
                int i4 = i - i2;
                if (partition.hasHeader) {
                    i4--;
                }
                if (i4 == -1 || (cursor = partition.cursor) == null || cursor.isClosed() || !cursor.moveToPosition(i4)) {
                    return null;
                }
                return cursor;
            }
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        Cursor cursor;
        ensureCacheValid();
        int i2 = 0;
        for (Partition partition : this.mPartitions) {
            int i3 = partition.count + i2;
            if (i < i2 || i >= i3) {
                i2 = i3;
            } else {
                int i4 = i - i2;
                if (partition.hasHeader) {
                    i4--;
                }
                if (i4 == -1 || partition.idColumnIndex == -1 || (cursor = partition.cursor) == null || cursor.isClosed() || !cursor.moveToPosition(i4)) {
                    return 0L;
                }
                return cursor.getLong(partition.idColumnIndex);
            }
        }
        return 0L;
    }

    @Override
    public boolean areAllItemsEnabled() {
        Iterator<Partition> it = this.mPartitions.iterator();
        while (it.hasNext()) {
            if (it.next().hasHeader) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEnabled(int i) {
        ensureCacheValid();
        int size = this.mPartitions.size();
        int i2 = 0;
        int i3 = 0;
        while (i2 < size) {
            int i4 = this.mPartitions.get(i2).count + i3;
            if (i < i3 || i >= i4) {
                i2++;
                i3 = i4;
            } else {
                int i5 = i - i3;
                if (this.mPartitions.get(i2).hasHeader && i5 == 0) {
                    return false;
                }
                return isEnabled(i2, i5);
            }
        }
        return false;
    }

    protected boolean isEnabled(int i, int i2) {
        return true;
    }

    public void setNotificationsEnabled(boolean z) {
        this.mNotificationsEnabled = z;
        if (z && this.mNotificationNeeded) {
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
