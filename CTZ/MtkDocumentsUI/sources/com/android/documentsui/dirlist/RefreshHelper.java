package com.android.documentsui.dirlist;

import android.support.v4.util.Preconditions;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import com.android.documentsui.base.BooleanConsumer;
import com.android.documentsui.base.Events;

final class RefreshHelper {
    private boolean mAttached;
    private final BooleanConsumer mRefreshLayoutEnabler;

    public RefreshHelper(BooleanConsumer booleanConsumer) {
        this.mRefreshLayoutEnabler = booleanConsumer;
    }

    private boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (Events.isMouseEvent(motionEvent)) {
            if (Events.isActionDown(motionEvent)) {
                this.mRefreshLayoutEnabler.accept(false);
            }
        } else if (Events.isActionDown(motionEvent) && recyclerView.computeVerticalScrollOffset() != 0) {
            this.mRefreshLayoutEnabler.accept(false);
        }
        if (Events.isActionUp(motionEvent)) {
            this.mRefreshLayoutEnabler.accept(true);
        }
        return false;
    }

    private void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (Events.isActionUp(motionEvent)) {
            this.mRefreshLayoutEnabler.accept(true);
        }
    }

    void attach(RecyclerView recyclerView) {
        Preconditions.checkState(!this.mAttached);
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public void onTouchEvent(RecyclerView recyclerView2, MotionEvent motionEvent) {
                RefreshHelper.this.onTouchEvent(recyclerView2, motionEvent);
            }

            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView2, MotionEvent motionEvent) {
                return RefreshHelper.this.onInterceptTouchEvent(recyclerView2, motionEvent);
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean z) {
            }
        });
        this.mAttached = true;
    }
}
