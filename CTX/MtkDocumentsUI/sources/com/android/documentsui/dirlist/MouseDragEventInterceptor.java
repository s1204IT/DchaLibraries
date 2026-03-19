package com.android.documentsui.dirlist;

import android.support.v4.util.Preconditions;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import com.android.documentsui.base.EventHandler;
import com.android.documentsui.base.Events;
import com.android.documentsui.selection.ItemDetailsLookup;

class MouseDragEventInterceptor implements RecyclerView.OnItemTouchListener {
    private final RecyclerView.OnItemTouchListener mDelegate;
    private final ItemDetailsLookup mEventDetailsLookup;
    private final EventHandler<MotionEvent> mMouseDragListener;

    public MouseDragEventInterceptor(ItemDetailsLookup itemDetailsLookup, EventHandler<MotionEvent> eventHandler, RecyclerView.OnItemTouchListener onItemTouchListener) {
        Preconditions.checkArgument(itemDetailsLookup != null);
        Preconditions.checkArgument(eventHandler != null);
        this.mEventDetailsLookup = itemDetailsLookup;
        this.mMouseDragListener = eventHandler;
        this.mDelegate = onItemTouchListener;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (Events.isMouseDragEvent(motionEvent) && this.mEventDetailsLookup.inItemDragRegion(motionEvent)) {
            return this.mMouseDragListener.accept(motionEvent);
        }
        if (this.mDelegate != null) {
            return this.mDelegate.onInterceptTouchEvent(recyclerView, motionEvent);
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (this.mDelegate != null) {
            this.mDelegate.onTouchEvent(recyclerView, motionEvent);
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean z) {
    }
}
