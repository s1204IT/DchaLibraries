package android.support.v7.widget.util;

import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;

public abstract class SortedListAdapterCallback<T2> extends SortedList.Callback<T2> {
    final RecyclerView.Adapter mAdapter;

    public SortedListAdapterCallback(RecyclerView.Adapter adapter) {
        this.mAdapter = adapter;
    }

    @Override
    public void onInserted(int position, int count) {
        this.mAdapter.notifyItemRangeInserted(position, count);
    }

    @Override
    public void onRemoved(int position, int count) {
        this.mAdapter.notifyItemRangeRemoved(position, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
        this.mAdapter.notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onChanged(int position, int count) {
        this.mAdapter.notifyItemRangeChanged(position, count);
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
        this.mAdapter.notifyItemRangeChanged(position, count, payload);
    }
}
