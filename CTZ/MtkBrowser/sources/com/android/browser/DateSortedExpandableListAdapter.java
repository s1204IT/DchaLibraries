package com.android.browser;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DateSorter;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class DateSortedExpandableListAdapter extends BaseExpandableListAdapter {
    private Context mContext;
    private Cursor mCursor;
    private int mDateIndex;
    private DateSorter mDateSorter;
    private int[] mItemMap;
    private int mNumberOfBins;
    DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            DateSortedExpandableListAdapter.this.mDataValid = true;
            DateSortedExpandableListAdapter.this.notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            DateSortedExpandableListAdapter.this.mDataValid = false;
            DateSortedExpandableListAdapter.this.notifyDataSetInvalidated();
        }
    };
    boolean mDataValid = false;
    private int mIdIndex = -1;

    public DateSortedExpandableListAdapter(Context context, int i) {
        this.mContext = context;
        this.mDateSorter = new DateSorter(context);
        this.mDateIndex = i;
    }

    private void buildMap() {
        int[] iArr = new int[5];
        for (int i = 0; i < 5; i++) {
            iArr[i] = 0;
        }
        this.mNumberOfBins = 0;
        int i2 = -1;
        if (this.mCursor.moveToFirst() && this.mCursor.getCount() > 0) {
            while (true) {
                if (this.mCursor.isAfterLast()) {
                    break;
                }
                int index = this.mDateSorter.getIndex(getLong(this.mDateIndex));
                if (index > i2) {
                    this.mNumberOfBins++;
                    if (index == 4) {
                        iArr[index] = this.mCursor.getCount() - this.mCursor.getPosition();
                        break;
                    }
                    i2 = index;
                }
                iArr[i2] = iArr[i2] + 1;
                this.mCursor.moveToNext();
            }
        }
        this.mItemMap = iArr;
    }

    Context getContext() {
        return this.mContext;
    }

    long getLong(int i) {
        if (this.mDataValid) {
            return this.mCursor.getLong(i);
        }
        return 0L;
    }

    private int groupPositionToBin(int i) {
        if (!this.mDataValid) {
            return -1;
        }
        if (i < 0 || i >= 5) {
            throw new AssertionError("group position out of range");
        }
        if (5 == this.mNumberOfBins || this.mNumberOfBins == 0) {
            return i;
        }
        int i2 = -1;
        while (i > -1) {
            i2++;
            if (this.mItemMap[i2] != 0) {
                i--;
            }
        }
        return i2;
    }

    boolean moveCursorToChildPosition(int i, int i2) {
        if (!this.mDataValid || this.mCursor.isClosed()) {
            return false;
        }
        int iGroupPositionToBin = groupPositionToBin(i);
        for (int i3 = 0; i3 < iGroupPositionToBin; i3++) {
            i2 += this.mItemMap[i3];
        }
        return this.mCursor.moveToPosition(i2);
    }

    public void changeCursor(Cursor cursor) {
        if (cursor == this.mCursor) {
            return;
        }
        if (this.mCursor != null) {
            this.mCursor.unregisterDataSetObserver(this.mDataSetObserver);
            this.mCursor.close();
        }
        this.mCursor = cursor;
        if (cursor != null) {
            cursor.registerDataSetObserver(this.mDataSetObserver);
            this.mIdIndex = cursor.getColumnIndexOrThrow("_id");
            this.mDataValid = true;
            buildMap();
            notifyDataSetChanged();
            return;
        }
        this.mIdIndex = -1;
        this.mDataValid = false;
        notifyDataSetInvalidated();
    }

    @Override
    public View getGroupView(int i, boolean z, View view, ViewGroup viewGroup) {
        ?? r3;
        if (!this.mDataValid) {
            throw new IllegalStateException("Data is not valid");
        }
        if (view != null) {
            boolean z2 = view instanceof TextView;
            r3 = view;
            if (!z2) {
                r3 = (TextView) LayoutInflater.from(this.mContext).inflate(R.layout.history_header, (ViewGroup) null);
            }
        }
        r3.setText(this.mDateSorter.getLabel(groupPositionToBin(i)));
        return r3;
    }

    @Override
    public View getChildView(int i, int i2, boolean z, View view, ViewGroup viewGroup) {
        if (!this.mDataValid) {
            throw new IllegalStateException("Data is not valid");
        }
        return null;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return true;
    }

    @Override
    public int getGroupCount() {
        if (this.mDataValid) {
            return this.mNumberOfBins;
        }
        return 0;
    }

    @Override
    public int getChildrenCount(int i) {
        if (this.mDataValid) {
            return this.mItemMap[groupPositionToBin(i)];
        }
        return 0;
    }

    @Override
    public Object getGroup(int i) {
        return null;
    }

    @Override
    public Object getChild(int i, int i2) {
        return null;
    }

    @Override
    public long getGroupId(int i) {
        if (this.mDataValid) {
            return i;
        }
        return 0L;
    }

    @Override
    public long getChildId(int i, int i2) {
        if (this.mDataValid && moveCursorToChildPosition(i, i2)) {
            return getLong(this.mIdIndex);
        }
        return 0L;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onGroupExpanded(int i) {
    }

    @Override
    public void onGroupCollapsed(int i) {
    }

    @Override
    public long getCombinedChildId(long j, long j2) {
        if (this.mDataValid) {
            return j2;
        }
        return 0L;
    }

    @Override
    public long getCombinedGroupId(long j) {
        if (this.mDataValid) {
            return j;
        }
        return 0L;
    }

    @Override
    public boolean isEmpty() {
        return !this.mDataValid || this.mCursor == null || this.mCursor.isClosed() || this.mCursor.getCount() == 0;
    }
}
