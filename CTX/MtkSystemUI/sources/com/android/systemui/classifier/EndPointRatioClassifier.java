package com.android.systemui.classifier;

public class EndPointRatioClassifier extends StrokeClassifier {
    public EndPointRatioClassifier(ClassifierData classifierData) {
        this.mClassifierData = classifierData;
    }

    @Override
    public String getTag() {
        return "END_RTIO";
    }

    @Override
    public float getFalseTouchEvaluation(int i, Stroke stroke) {
        float endPointLength;
        if (stroke.getTotalLength() == 0.0f) {
            endPointLength = 1.0f;
        } else {
            endPointLength = stroke.getEndPointLength() / stroke.getTotalLength();
        }
        return EndPointRatioEvaluator.evaluate(endPointLength);
    }
}
