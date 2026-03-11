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
    public void onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == 0) {
            this.mCount = 1;
        }
        if (action != 5) {
            return;
        }
        this.mCount++;
    }

    @Override
    public float getFalseTouchEvaluation(int type) {
        return PointerCountEvaluator.evaluate(this.mCount);
    }
}
