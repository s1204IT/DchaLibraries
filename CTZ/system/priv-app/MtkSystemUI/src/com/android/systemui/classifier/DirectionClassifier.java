package com.android.systemui.classifier;

/* loaded from: classes.dex */
public class DirectionClassifier extends StrokeClassifier {
    public DirectionClassifier(ClassifierData classifierData) {
    }

    @Override // com.android.systemui.classifier.Classifier
    public String getTag() {
        return "DIR";
    }

    @Override // com.android.systemui.classifier.StrokeClassifier
    public float getFalseTouchEvaluation(int i, Stroke stroke) {
        Point point = stroke.getPoints().get(0);
        Point point2 = stroke.getPoints().get(stroke.getPoints().size() - 1);
        return DirectionEvaluator.evaluate(point2.x - point.x, point2.y - point.y, i);
    }
}
