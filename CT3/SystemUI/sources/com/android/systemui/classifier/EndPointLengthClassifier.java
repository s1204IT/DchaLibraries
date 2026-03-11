package com.android.systemui.classifier;

public class EndPointLengthClassifier extends StrokeClassifier {
    public EndPointLengthClassifier(ClassifierData classifierData) {
    }

    @Override
    public String getTag() {
        return "END_LNGTH";
    }

    @Override
    public float getFalseTouchEvaluation(int type, Stroke stroke) {
        return EndPointLengthEvaluator.evaluate(stroke.getEndPointLength());
    }
}
