package com.android.systemui.statusbar.stack;

import java.util.ArrayList;

public class PiecewiseLinearIndentationFunctor extends StackIndentationFunctor {
    private final ArrayList<Float> mBaseValues;
    private final float mLinearPart;

    PiecewiseLinearIndentationFunctor(int maxItemsInStack, int peekSize, int distanceToPeekStart, float linearPart) {
        super(maxItemsInStack, peekSize, distanceToPeekStart);
        this.mBaseValues = new ArrayList<>(maxItemsInStack + 1);
        initBaseValues();
        this.mLinearPart = linearPart;
    }

    private void initBaseValues() {
        int sumOfSquares = getSumOfSquares(this.mMaxItemsInStack - 1);
        int totalWeight = 0;
        this.mBaseValues.add(Float.valueOf(0.0f));
        for (int i = 0; i < this.mMaxItemsInStack - 1; i++) {
            totalWeight += ((this.mMaxItemsInStack - i) - 1) * ((this.mMaxItemsInStack - i) - 1);
            this.mBaseValues.add(Float.valueOf(totalWeight / sumOfSquares));
        }
    }

    private int getSumOfSquares(int n) {
        return (((n + 1) * n) * ((n * 2) + 1)) / 6;
    }

    @Override
    public float getValue(float itemsBefore) {
        if (this.mStackStartsAtPeek) {
            itemsBefore += 1.0f;
        }
        if (itemsBefore < 0.0f) {
            return 0.0f;
        }
        if (itemsBefore >= this.mMaxItemsInStack) {
            return this.mTotalTransitionDistance;
        }
        int below = (int) itemsBefore;
        float partialIn = itemsBefore - below;
        if (below == 0) {
            return this.mDistanceToPeekStart * partialIn;
        }
        float result = this.mDistanceToPeekStart;
        float progress = ((1.0f - partialIn) * this.mBaseValues.get(below - 1).floatValue()) + (this.mBaseValues.get(below).floatValue() * partialIn);
        return result + ((((1.0f - this.mLinearPart) * progress) + (((itemsBefore - 1.0f) / (this.mMaxItemsInStack - 1)) * this.mLinearPart)) * this.mPeekSize);
    }
}
