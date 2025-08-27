package com.android.setupwizardlib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/* loaded from: classes.dex */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {
    private Drawable mDivider;
    private int mDividerCondition;
    private int mDividerHeight;
    private int mDividerIntrinsicHeight;

    public interface DividedViewHolder {
        boolean isDividerAllowedAbove();

        boolean isDividerAllowedBelow();
    }

    public DividerItemDecoration() {
    }

    public DividerItemDecoration(Context context) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(R.styleable.SuwDividerItemDecoration);
        Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(R.styleable.SuwDividerItemDecoration_android_listDivider);
        int dimensionPixelSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.SuwDividerItemDecoration_android_dividerHeight, 0);
        int i = typedArrayObtainStyledAttributes.getInt(R.styleable.SuwDividerItemDecoration_suwDividerCondition, 0);
        typedArrayObtainStyledAttributes.recycle();
        setDivider(drawable);
        setDividerHeight(dimensionPixelSize);
        setDividerCondition(i);
    }

    @Override // android.support.v7.widget.RecyclerView.ItemDecoration
    public void onDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.State state) {
        if (this.mDivider == null) {
            return;
        }
        int childCount = recyclerView.getChildCount();
        int width = recyclerView.getWidth();
        int i = this.mDividerHeight != 0 ? this.mDividerHeight : this.mDividerIntrinsicHeight;
        for (int i2 = 0; i2 < childCount; i2++) {
            View childAt = recyclerView.getChildAt(i2);
            if (shouldDrawDividerBelow(childAt, recyclerView)) {
                int y = ((int) ViewCompat.getY(childAt)) + childAt.getHeight();
                this.mDivider.setBounds(0, y, width, y + i);
                this.mDivider.draw(canvas);
            }
        }
    }

    @Override // android.support.v7.widget.RecyclerView.ItemDecoration
    public void getItemOffsets(Rect rect, View view, RecyclerView recyclerView, RecyclerView.State state) {
        if (shouldDrawDividerBelow(view, recyclerView)) {
            rect.bottom = this.mDividerHeight != 0 ? this.mDividerHeight : this.mDividerIntrinsicHeight;
        }
    }

    private boolean shouldDrawDividerBelow(View view, RecyclerView recyclerView) {
        RecyclerView.ViewHolder childViewHolder = recyclerView.getChildViewHolder(view);
        int layoutPosition = childViewHolder.getLayoutPosition();
        int itemCount = recyclerView.getAdapter().getItemCount() - 1;
        if (isDividerAllowedBelow(childViewHolder)) {
            if (this.mDividerCondition == 0) {
                return true;
            }
        } else if (this.mDividerCondition == 1 || layoutPosition == itemCount) {
            return false;
        }
        return layoutPosition >= itemCount || isDividerAllowedAbove(recyclerView.findViewHolderForLayoutPosition(layoutPosition + 1));
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: android.support.v7.widget.RecyclerView$ViewHolder */
    /* JADX WARN: Multi-variable type inference failed */
    protected boolean isDividerAllowedAbove(RecyclerView.ViewHolder viewHolder) {
        return !(viewHolder instanceof DividedViewHolder) || ((DividedViewHolder) viewHolder).isDividerAllowedAbove();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: android.support.v7.widget.RecyclerView$ViewHolder */
    /* JADX WARN: Multi-variable type inference failed */
    protected boolean isDividerAllowedBelow(RecyclerView.ViewHolder viewHolder) {
        return !(viewHolder instanceof DividedViewHolder) || ((DividedViewHolder) viewHolder).isDividerAllowedBelow();
    }

    public void setDivider(Drawable drawable) {
        if (drawable != null) {
            this.mDividerIntrinsicHeight = drawable.getIntrinsicHeight();
        } else {
            this.mDividerIntrinsicHeight = 0;
        }
        this.mDivider = drawable;
    }

    public Drawable getDivider() {
        return this.mDivider;
    }

    public void setDividerHeight(int i) {
        this.mDividerHeight = i;
    }

    public void setDividerCondition(int i) {
        this.mDividerCondition = i;
    }
}
