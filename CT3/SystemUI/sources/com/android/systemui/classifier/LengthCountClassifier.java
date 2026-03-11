package com.android.systemui.classifier;

public class LengthCountClassifier extends StrokeClassifier {
    public LengthCountClassifier(ClassifierData classifierData) {
    }

    @Override
    public String getTag() {
        return "LEN_CNT";
    }

    @Override
    public float getFalseTouchEvaluation(int type, Stroke stroke) {
        return LengthCountEvaluator.evaluate(stroke.getTotalLength() / Math.max(1.0f, stroke.getCount() - 2));
    }
}
