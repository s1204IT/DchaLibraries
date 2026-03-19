package com.android.documentsui.selection;

import android.support.v4.util.Preconditions;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

public final class GestureRouter<T extends GestureDetector.OnGestureListener & GestureDetector.OnDoubleTapListener> implements GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private final ToolHandlerRegistry<T> mDelegates;

    public GestureRouter(T t) {
        Preconditions.checkNotNull(t);
        this.mDelegates = new ToolHandlerRegistry<>(t);
    }

    public GestureRouter() {
        this(new GestureDetector.SimpleOnGestureListener());
    }

    public void register(int i, T t) {
        this.mDelegates.set(i, t);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        return this.mDelegates.get(motionEvent).onSingleTapConfirmed(motionEvent);
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        return this.mDelegates.get(motionEvent).onDoubleTap(motionEvent);
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return this.mDelegates.get(motionEvent).onDoubleTapEvent(motionEvent);
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return this.mDelegates.get(motionEvent).onDown(motionEvent);
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
        this.mDelegates.get(motionEvent).onShowPress(motionEvent);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return this.mDelegates.get(motionEvent).onSingleTapUp(motionEvent);
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return this.mDelegates.get(motionEvent2).onScroll(motionEvent, motionEvent2, f, f2);
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        this.mDelegates.get(motionEvent).onLongPress(motionEvent);
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return this.mDelegates.get(motionEvent2).onFling(motionEvent, motionEvent2, f, f2);
    }
}
