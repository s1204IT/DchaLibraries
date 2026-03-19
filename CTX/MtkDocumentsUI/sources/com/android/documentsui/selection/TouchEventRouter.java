package com.android.documentsui.selection;

import android.support.v4.util.Preconditions;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;

public final class TouchEventRouter implements RecyclerView.OnItemTouchListener {
    private final ToolHandlerRegistry<RecyclerView.OnItemTouchListener> mDelegates;
    private final GestureDetector mDetector;

    public TouchEventRouter(GestureDetector gestureDetector, RecyclerView.OnItemTouchListener onItemTouchListener) {
        Preconditions.checkArgument(gestureDetector != null);
        Preconditions.checkArgument(onItemTouchListener != null);
        this.mDetector = gestureDetector;
        this.mDelegates = new ToolHandlerRegistry<>(onItemTouchListener);
    }

    public TouchEventRouter(GestureDetector gestureDetector) {
        this(gestureDetector, new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean z) {
            }
        });
    }

    public void register(int i, RecyclerView.OnItemTouchListener onItemTouchListener) {
        Preconditions.checkArgument(onItemTouchListener != null);
        this.mDelegates.set(i, onItemTouchListener);
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        return this.mDelegates.get(motionEvent).onInterceptTouchEvent(recyclerView, motionEvent) | this.mDetector.onTouchEvent(motionEvent);
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        this.mDelegates.get(motionEvent).onTouchEvent(recyclerView, motionEvent);
        this.mDetector.onTouchEvent(motionEvent);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean z) {
    }
}
