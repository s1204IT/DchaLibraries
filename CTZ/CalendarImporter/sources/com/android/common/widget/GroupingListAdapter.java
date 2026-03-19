package com.android.common.widget;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Handler;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class GroupingListAdapter extends BaseAdapter {
    private static final long EXPANDED_GROUP_MASK = Long.MIN_VALUE;
    private static final int GROUP_METADATA_ARRAY_INCREMENT = 128;
    private static final int GROUP_METADATA_ARRAY_INITIAL_SIZE = 16;
    private static final long GROUP_OFFSET_MASK = 4294967295L;
    private static final long GROUP_SIZE_MASK = 9223372032559808512L;
    public static final int ITEM_TYPE_GROUP_HEADER = 1;
    public static final int ITEM_TYPE_IN_GROUP = 2;
    public static final int ITEM_TYPE_STANDALONE = 0;
    private Context mContext;
    private int mCount;
    private Cursor mCursor;
    private int mGroupCount;
    private long[] mGroupMetadata;
    private int mLastCachedCursorPosition;
    private int mLastCachedGroup;
    private int mLastCachedListPosition;
    private int mRowIdColumnIndex;
    private SparseIntArray mPositionCache = new SparseIntArray();
    private PositionMetadata mPositionMetadata = new PositionMetadata();
    protected ContentObserver mChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean z) {
            GroupingListAdapter.this.onContentChanged();
        }
    };
    protected DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            GroupingListAdapter.this.notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            GroupingListAdapter.this.notifyDataSetInvalidated();
        }
    };

    protected abstract void addGroups(Cursor cursor);

    protected abstract void bindChildView(View view, Context context, Cursor cursor);

    protected abstract void bindGroupView(View view, Context context, Cursor cursor, int i, boolean z);

    protected abstract void bindStandAloneView(View view, Context context, Cursor cursor);

    protected abstract View newChildView(Context context, ViewGroup viewGroup);

    protected abstract View newGroupView(Context context, ViewGroup viewGroup);

    protected abstract View newStandAloneView(Context context, ViewGroup viewGroup);

    protected static class PositionMetadata {
        int childCount;
        int cursorPosition;
        private int groupPosition;
        boolean isExpanded;
        int itemType;
        private int listPosition = -1;

        protected PositionMetadata() {
        }
    }

    public GroupingListAdapter(Context context) {
        this.mContext = context;
        resetCache();
    }

    private void resetCache() {
        this.mCount = -1;
        this.mLastCachedListPosition = -1;
        this.mLastCachedCursorPosition = -1;
        this.mLastCachedGroup = -1;
        this.mPositionMetadata.listPosition = -1;
        this.mPositionCache.clear();
    }

    protected void onContentChanged() {
    }

    public void changeCursor(Cursor cursor) {
        if (cursor == this.mCursor) {
            return;
        }
        if (this.mCursor != null) {
            this.mCursor.unregisterContentObserver(this.mChangeObserver);
            this.mCursor.unregisterDataSetObserver(this.mDataSetObserver);
            this.mCursor.close();
        }
        this.mCursor = cursor;
        resetCache();
        findGroups();
        if (cursor != null) {
            cursor.registerContentObserver(this.mChangeObserver);
            cursor.registerDataSetObserver(this.mDataSetObserver);
            this.mRowIdColumnIndex = cursor.getColumnIndexOrThrow("_id");
            notifyDataSetChanged();
            return;
        }
        notifyDataSetInvalidated();
    }

    public Cursor getCursor() {
        return this.mCursor;
    }

    private void findGroups() {
        this.mGroupCount = 0;
        this.mGroupMetadata = new long[16];
        if (this.mCursor == null) {
            return;
        }
        addGroups(this.mCursor);
    }

    protected void addGroup(int i, int i2, boolean z) {
        if (this.mGroupCount >= this.mGroupMetadata.length) {
            long[] jArr = new long[idealLongArraySize(this.mGroupMetadata.length + GROUP_METADATA_ARRAY_INCREMENT)];
            System.arraycopy(this.mGroupMetadata, 0, jArr, 0, this.mGroupCount);
            this.mGroupMetadata = jArr;
        }
        long j = ((long) i) | (((long) i2) << 32);
        if (z) {
            j |= EXPANDED_GROUP_MASK;
        }
        long[] jArr2 = this.mGroupMetadata;
        int i3 = this.mGroupCount;
        this.mGroupCount = i3 + 1;
        jArr2[i3] = j;
    }

    private int idealLongArraySize(int i) {
        return idealByteArraySize(i * 8) / 8;
    }

    private int idealByteArraySize(int i) {
        for (int i2 = 4; i2 < 32; i2++) {
            int i3 = (1 << i2) - 12;
            if (i <= i3) {
                return i3;
            }
        }
        return i;
    }

    @Override
    public int getCount() {
        if (this.mCursor == null) {
            return 0;
        }
        if (this.mCount != -1) {
            return this.mCount;
        }
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < this.mGroupCount; i3++) {
            long j = this.mGroupMetadata[i3];
            int i4 = (int) (GROUP_OFFSET_MASK & j);
            boolean z = (EXPANDED_GROUP_MASK & j) != 0;
            int i5 = (int) ((j & GROUP_SIZE_MASK) >> 32);
            int i6 = i + (i4 - i2);
            if (z) {
                i = i6 + i5 + 1;
            } else {
                i = i6 + 1;
            }
            i2 = i4 + i5;
        }
        this.mCount = (i + this.mCursor.getCount()) - i2;
        return this.mCount;
    }

    public void obtainPositionMetadata(PositionMetadata positionMetadata, int i) {
        int iValueAt;
        int iKeyAt;
        int i2;
        if (positionMetadata.listPosition == i) {
            return;
        }
        if (this.mLastCachedListPosition != -1) {
            if (i <= this.mLastCachedListPosition) {
                int iIndexOfKey = this.mPositionCache.indexOfKey(i);
                if (iIndexOfKey < 0 && (iIndexOfKey = (~iIndexOfKey) - 1) >= this.mPositionCache.size()) {
                    iIndexOfKey--;
                }
                if (iIndexOfKey >= 0) {
                    iKeyAt = this.mPositionCache.keyAt(iIndexOfKey);
                    iValueAt = this.mPositionCache.valueAt(iIndexOfKey);
                    i2 = (int) (this.mGroupMetadata[iValueAt] & GROUP_OFFSET_MASK);
                } else {
                    iValueAt = 0;
                    iKeyAt = 0;
                    i2 = 0;
                }
            } else {
                iValueAt = this.mLastCachedGroup;
                iKeyAt = this.mLastCachedListPosition;
                i2 = this.mLastCachedCursorPosition;
            }
        } else {
            iValueAt = 0;
            iKeyAt = 0;
            i2 = 0;
        }
        while (iValueAt < this.mGroupCount) {
            long j = this.mGroupMetadata[iValueAt];
            int i3 = (int) (j & GROUP_OFFSET_MASK);
            int i4 = iKeyAt + (i3 - i2);
            if (iValueAt > this.mLastCachedGroup) {
                this.mPositionCache.append(i4, iValueAt);
                this.mLastCachedListPosition = i4;
                this.mLastCachedCursorPosition = i3;
                this.mLastCachedGroup = iValueAt;
            }
            if (i < i4) {
                positionMetadata.itemType = 0;
                positionMetadata.cursorPosition = i3 - (i4 - i);
                return;
            }
            boolean z = (EXPANDED_GROUP_MASK & j) != 0;
            int i5 = (int) ((j & GROUP_SIZE_MASK) >> 32);
            if (i == i4) {
                positionMetadata.itemType = 1;
                positionMetadata.groupPosition = iValueAt;
                positionMetadata.isExpanded = z;
                positionMetadata.childCount = i5;
                positionMetadata.cursorPosition = i3;
                return;
            }
            if (z) {
                if (i < i4 + i5 + 1) {
                    positionMetadata.itemType = 2;
                    positionMetadata.cursorPosition = (i3 + (i - i4)) - 1;
                    return;
                }
                iKeyAt = i4 + i5 + 1;
            } else {
                iKeyAt = i4 + 1;
            }
            i2 = i3 + i5;
            iValueAt++;
        }
        positionMetadata.itemType = 0;
        positionMetadata.cursorPosition = i2 + (i - iKeyAt);
    }

    public boolean isGroupHeader(int i) {
        obtainPositionMetadata(this.mPositionMetadata, i);
        return this.mPositionMetadata.itemType == 1;
    }

    public int getGroupSize(int i) {
        obtainPositionMetadata(this.mPositionMetadata, i);
        return this.mPositionMetadata.childCount;
    }

    public void toggleGroup(int i) {
        obtainPositionMetadata(this.mPositionMetadata, i);
        if (this.mPositionMetadata.itemType != 1) {
            throw new IllegalArgumentException("Not a group at position " + i);
        }
        if (this.mPositionMetadata.isExpanded) {
            long[] jArr = this.mGroupMetadata;
            int i2 = this.mPositionMetadata.groupPosition;
            jArr[i2] = jArr[i2] & Long.MAX_VALUE;
        } else {
            long[] jArr2 = this.mGroupMetadata;
            int i3 = this.mPositionMetadata.groupPosition;
            jArr2[i3] = jArr2[i3] | EXPANDED_GROUP_MASK;
        }
        resetCache();
        notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int i) {
        obtainPositionMetadata(this.mPositionMetadata, i);
        return this.mPositionMetadata.itemType;
    }

    @Override
    public Object getItem(int i) {
        if (this.mCursor == null) {
            return null;
        }
        obtainPositionMetadata(this.mPositionMetadata, i);
        if (this.mCursor.moveToPosition(this.mPositionMetadata.cursorPosition)) {
            return this.mCursor;
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        if (getItem(i) != null) {
            return this.mCursor.getLong(this.mRowIdColumnIndex);
        }
        return -1L;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        obtainPositionMetadata(this.mPositionMetadata, i);
        if (view == null) {
            switch (this.mPositionMetadata.itemType) {
                case 0:
                    view = newStandAloneView(this.mContext, viewGroup);
                    break;
                case 1:
                    view = newGroupView(this.mContext, viewGroup);
                    break;
                case 2:
                    view = newChildView(this.mContext, viewGroup);
                    break;
            }
        }
        this.mCursor.moveToPosition(this.mPositionMetadata.cursorPosition);
        switch (this.mPositionMetadata.itemType) {
            case 0:
                bindStandAloneView(view, this.mContext, this.mCursor);
                return view;
            case 1:
                bindGroupView(view, this.mContext, this.mCursor, this.mPositionMetadata.childCount, this.mPositionMetadata.isExpanded);
                return view;
            case 2:
                bindChildView(view, this.mContext, this.mCursor);
                return view;
            default:
                return view;
        }
    }
}
