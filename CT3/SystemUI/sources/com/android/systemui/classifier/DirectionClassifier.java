package com.android.systemui.classifier;

public class DirectionClassifier extends StrokeClassifier {
    public DirectionClassifier(ClassifierData classifierData) {
    }

    @Override
    public String getTag() {
        return "DIR";
    }

    @Override
    public float getFalseTouchEvaluation(int type, Stroke stroke) {
        Point firstPoint = stroke.getPoints().get(0);
        Point lastPoint = stroke.getPoints().get(stroke.getPoints().size() - 1);
        return DirectionEvaluator.evaluate(lastPoint.x - firstPoint.x, lastPoint.y - firstPoint.y, type);
    }
}
