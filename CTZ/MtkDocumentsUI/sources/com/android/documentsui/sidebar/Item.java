package com.android.documentsui.sidebar;

import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.documentsui.MenuManager;
import com.android.documentsui.R;

abstract class Item {
    private final int mLayoutId;
    final String stringId;

    abstract void bindView(View view);

    abstract boolean isRoot();

    abstract void open();

    public Item(int i, String str) {
        this.mLayoutId = i;
        this.stringId = str;
    }

    public View getView(View view, ViewGroup viewGroup) {
        if (view == null || ((Integer) view.getTag(R.id.layout_id_tag)).intValue() != this.mLayoutId) {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(this.mLayoutId, viewGroup, false);
        }
        view.setTag(R.id.layout_id_tag, Integer.valueOf(this.mLayoutId));
        bindView(view);
        return view;
    }

    boolean isDropTarget() {
        return isRoot();
    }

    boolean dropOn(DragEvent dragEvent) {
        return false;
    }

    boolean showAppDetails() {
        return false;
    }

    void createContextMenu(Menu menu, MenuInflater menuInflater, MenuManager menuManager) {
    }
}
