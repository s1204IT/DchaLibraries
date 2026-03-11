package com.android.systemui.classifier;

import android.view.MotionEvent;

public class PointerCountClassifier extends GestureClassifier {
    private int mCount = 0;

    public PointerCountClassifier(ClassifierData classifierData) {
    }

    @Override
    public String getTag() {
        return "PTR_CNT";
    }

    @Override
    public void onTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.mCount = 1;
        }
        if (actionMasked == 5) {
            this.mCount++;
        }
    }

    @Override
    public float getFalseTouchEvaluation(int i) {
        return PointerCountEvaluator.evaluate(this.mCount);
    }
}
