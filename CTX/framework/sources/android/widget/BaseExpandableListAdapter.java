package android.widget;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

public abstract class BaseExpandableListAdapter implements ExpandableListAdapter, HeterogeneousExpandableList {
    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        this.mDataSetObservable.registerObserver(dataSetObserver);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        this.mDataSetObservable.unregisterObserver(dataSetObserver);
    }

    public void notifyDataSetInvalidated() {
        this.mDataSetObservable.notifyInvalidated();
    }

    public void notifyDataSetChanged() {
        this.mDataSetObservable.notifyChanged();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public void onGroupCollapsed(int i) {
    }

    @Override
    public void onGroupExpanded(int i) {
    }

    @Override
    public long getCombinedChildId(long j, long j2) {
        return ((j & 2147483647L) << 32) | Long.MIN_VALUE | (j2 & (-1));
    }

    @Override
    public long getCombinedGroupId(long j) {
        return (j & 2147483647L) << 32;
    }

    @Override
    public boolean isEmpty() {
        return getGroupCount() == 0;
    }

    @Override
    public int getChildType(int i, int i2) {
        return 0;
    }

    @Override
    public int getChildTypeCount() {
        return 1;
    }

    @Override
    public int getGroupType(int i) {
        return 0;
    }

    @Override
    public int getGroupTypeCount() {
        return 1;
    }
}
