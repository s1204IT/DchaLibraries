package com.android.documentsui.sidebar;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.android.documentsui.R;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RootsAdapter extends ArrayAdapter<Item> {
    static final boolean $assertionsDisabled = false;
    private static final Map<String, Long> sIdMap = new HashMap();
    private static long sNextAvailableId;
    private final View.OnDragListener mDragListener;

    public RootsAdapter(Activity activity, List<Item> list, View.OnDragListener onDragListener) {
        super(activity, 0, list);
        this.mDragListener = onDragListener;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int i) {
        String str = getItem(i).stringId;
        if (sIdMap.containsKey(str)) {
            return sIdMap.get(str).longValue();
        }
        long j = sNextAvailableId;
        sNextAvailableId = 1 + j;
        sIdMap.put(str, Long.valueOf(j));
        return j;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Item item = getItem(i);
        View view2 = item.getView(view, viewGroup);
        if (item.isRoot()) {
            view2.setTag(R.id.item_position_tag, Integer.valueOf(i));
            view2.setOnDragListener(this.mDragListener);
        } else {
            view2.setTag(R.id.item_position_tag, null);
            view2.setOnDragListener(null);
        }
        return view2;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int i) {
        return getItemViewType(i) != 1;
    }

    @Override
    public int getItemViewType(int i) {
        Item item = getItem(i);
        if ((item instanceof RootItem) || (item instanceof AppItem)) {
            return 0;
        }
        return 1;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }
}
