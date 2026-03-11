package com.android.settings.dashboard;

import android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

public class DashboardDecorator extends RecyclerView.ItemDecoration {
    private final Context mContext;
    private final Drawable mDivider;

    public DashboardDecorator(Context context) {
        this.mContext = context;
        TypedValue value = new TypedValue();
        this.mContext.getTheme().resolveAttribute(R.attr.listDivider, value, true);
        this.mDivider = this.mContext.getDrawable(value.resourceId);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int childCount = parent.getChildCount();
        for (int i = 1; i < childCount; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.ViewHolder holder = parent.getChildViewHolder(child);
            if (holder.getItemViewType() == com.android.settings.R.layout.dashboard_category) {
                if (parent.getChildViewHolder(parent.getChildAt(i - 1)).getItemViewType() == com.android.settings.R.layout.dashboard_tile) {
                    int top = getChildTop(child);
                    this.mDivider.setBounds(child.getLeft(), top, child.getRight(), this.mDivider.getIntrinsicHeight() + top);
                    this.mDivider.draw(c);
                }
            } else if (holder.getItemViewType() == com.android.settings.R.layout.condition_card) {
            }
        }
    }

    private int getChildTop(View child) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
        return child.getTop() + params.topMargin + Math.round(ViewCompat.getTranslationY(child));
    }
}
