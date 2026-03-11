package com.android.systemui.classifier;

public class DurationCountClassifier extends StrokeClassifier {
    public DurationCountClassifier(ClassifierData classifierData) {
    }

    @Override
    public String getTag() {
        return "DUR";
    }

    @Override
    public float getFalseTouchEvaluation(int i, Stroke stroke) {
        return DurationCountEvaluator.evaluate(stroke.getDurationSeconds() / stroke.getCount());
    }
}
