package com.android.launcher3.widget;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.R;

/* loaded from: classes.dex */
public class WidgetsRowViewHolder extends RecyclerView.ViewHolder {
    public final ViewGroup cellContainer;
    public final BubbleTextView title;

    public WidgetsRowViewHolder(ViewGroup viewGroup) {
        super(viewGroup);
        this.cellContainer = (ViewGroup) viewGroup.findViewById(R.id.widgets_cell_list);
        this.title = (BubbleTextView) viewGroup.findViewById(R.id.section);
        this.title.setAccessibilityDelegate(null);
    }
}
