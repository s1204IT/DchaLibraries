package com.android.systemui.statusbar.stack;

public abstract class StackIndentationFunctor {
    protected int mDistanceToPeekStart;
    protected int mMaxItemsInStack;
    protected int mPeekSize;
    protected boolean mStackStartsAtPeek;
    protected int mTotalTransitionDistance;

    public abstract float getValue(float f);

    StackIndentationFunctor(int maxItemsInStack, int peekSize, int distanceToPeekStart) {
        this.mDistanceToPeekStart = distanceToPeekStart;
        this.mStackStartsAtPeek = this.mDistanceToPeekStart == 0;
        this.mMaxItemsInStack = maxItemsInStack;
        this.mPeekSize = peekSize;
        updateTotalTransitionDistance();
    }

    private void updateTotalTransitionDistance() {
        this.mTotalTransitionDistance = this.mDistanceToPeekStart + this.mPeekSize;
    }
}
