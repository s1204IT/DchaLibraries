package android.support.v17.leanback.widget;

import android.support.v7.widget.RecyclerView;

public abstract class OnChildViewHolderSelectedListener {
    public void onChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child, int position, int subposition) {
    }

    public void onChildViewHolderSelectedAndPositioned(RecyclerView parent, RecyclerView.ViewHolder child, int position, int subposition) {
    }
}
