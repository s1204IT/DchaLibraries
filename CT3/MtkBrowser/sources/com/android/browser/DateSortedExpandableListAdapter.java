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

    public DateSortedExpandableListAdapter(Context context, int dateIndex) {
        this.mContext = context;
        this.mDateSorter = new DateSorter(context);
        this.mDateIndex = dateIndex;
    }

    private void buildMap() {
        int[] array = new int[5];
        for (int j = 0; j < 5; j++) {
            array[j] = 0;
        }
        this.mNumberOfBins = 0;
        int dateIndex = -1;
        if (this.mCursor.moveToFirst() && this.mCursor.getCount() > 0) {
            while (true) {
                if (this.mCursor.isAfterLast()) {
                    break;
                }
                long date = getLong(this.mDateIndex);
                int index = this.mDateSorter.getIndex(date);
                if (index > dateIndex) {
                    this.mNumberOfBins++;
                    if (index == 4) {
                        array[index] = this.mCursor.getCount() - this.mCursor.getPosition();
                        break;
                    }
                    dateIndex = index;
                }
                array[dateIndex] = array[dateIndex] + 1;
                this.mCursor.moveToNext();
            }
        }
        this.mItemMap = array;
    }

    Context getContext() {
        return this.mContext;
    }

    long getLong(int cursorIndex) {
        if (this.mDataValid) {
            return this.mCursor.getLong(cursorIndex);
        }
        return 0L;
    }

    private int groupPositionToBin(int groupPosition) {
        if (!this.mDataValid) {
            return -1;
        }
        if (groupPosition < 0 || groupPosition >= 5) {
            throw new AssertionError("group position out of range");
        }
        if (5 == this.mNumberOfBins || this.mNumberOfBins == 0) {
            return groupPosition;
        }
        int arrayPosition = -1;
        while (groupPosition > -1) {
            arrayPosition++;
            if (this.mItemMap[arrayPosition] != 0) {
                groupPosition--;
            }
        }
        return arrayPosition;
    }

    boolean moveCursorToChildPosition(int groupPosition, int childPosition) {
        if (!this.mDataValid || this.mCursor.isClosed()) {
            return false;
        }
        int groupPosition2 = groupPositionToBin(groupPosition);
        int index = childPosition;
        for (int i = 0; i < groupPosition2; i++) {
            index += this.mItemMap[i];
        }
        return this.mCursor.moveToPosition(index);
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
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        TextView item;
        if (!this.mDataValid) {
            throw new IllegalStateException("Data is not valid");
        }
        if (convertView == null || !(convertView instanceof TextView)) {
            LayoutInflater factory = LayoutInflater.from(this.mContext);
            item = (TextView) factory.inflate(R.layout.history_header, (ViewGroup) null);
        } else {
            item = (TextView) convertView;
        }
        String label = this.mDateSorter.getLabel(groupPositionToBin(groupPosition));
        item.setText(label);
        return item;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (this.mDataValid) {
            return null;
        }
        throw new IllegalStateException("Data is not valid");
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
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
    public int getChildrenCount(int groupPosition) {
        if (this.mDataValid) {
            return this.mItemMap[groupPositionToBin(groupPosition)];
        }
        return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        if (this.mDataValid) {
            return groupPosition;
        }
        return 0L;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        if (this.mDataValid && moveCursorToChildPosition(groupPosition, childPosition)) {
            return getLong(this.mIdIndex);
        }
        return 0L;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
    }

    @Override
    public long getCombinedChildId(long groupId, long childId) {
        if (this.mDataValid) {
            return childId;
        }
        return 0L;
    }

    @Override
    public long getCombinedGroupId(long groupId) {
        if (this.mDataValid) {
            return groupId;
        }
        return 0L;
    }

    @Override
    public boolean isEmpty() {
        return !this.mDataValid || this.mCursor == null || this.mCursor.isClosed() || this.mCursor.getCount() == 0;
    }
}
