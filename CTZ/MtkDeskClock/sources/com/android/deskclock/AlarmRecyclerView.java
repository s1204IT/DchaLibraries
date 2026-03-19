package com.android.deskclock;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class AlarmRecyclerView extends RecyclerView {
    private boolean mIgnoreRequestLayout;

    public AlarmRecyclerView(Context context) {
        this(context, null);
    }

    public AlarmRecyclerView(Context context, @Nullable AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AlarmRecyclerView(Context context, @Nullable AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                return recyclerView.getItemAnimator().isRunning();
            }
        });
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        this.mIgnoreRequestLayout = true;
        super.onLayout(z, i, i2, i3, i4);
        this.mIgnoreRequestLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!this.mIgnoreRequestLayout) {
            if (getItemAnimator() == null || !getItemAnimator().isRunning()) {
                super.requestLayout();
            }
        }
    }
}
