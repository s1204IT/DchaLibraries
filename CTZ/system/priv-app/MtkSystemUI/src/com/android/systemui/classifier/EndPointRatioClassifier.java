package com.android.systemui.classifier;

/* loaded from: classes.dex */
public class EndPointRatioClassifier extends StrokeClassifier {
    public EndPointRatioClassifier(ClassifierData classifierData) {
        this.mClassifierData = classifierData;
    }

    @Override // com.android.systemui.classifier.Classifier
    public String getTag() {
        return "END_RTIO";
    }

    @Override // com.android.systemui.classifier.StrokeClassifier
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
