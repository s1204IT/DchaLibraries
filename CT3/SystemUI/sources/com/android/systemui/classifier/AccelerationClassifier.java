package com.android.systemui.classifier;

import android.view.MotionEvent;
import java.util.HashMap;

public class AccelerationClassifier extends StrokeClassifier {
    private final HashMap<Stroke, Data> mStrokeMap = new HashMap<>();

    public AccelerationClassifier(ClassifierData classifierData) {
        this.mClassifierData = classifierData;
    }

    @Override
    public String getTag() {
        return "ACC";
    }

    @Override
    public void onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == 0) {
            this.mStrokeMap.clear();
        }
        for (int i = 0; i < event.getPointerCount(); i++) {
            Stroke stroke = this.mClassifierData.getStroke(event.getPointerId(i));
            Point point = stroke.getPoints().get(stroke.getPoints().size() - 1);
            if (this.mStrokeMap.get(stroke) == null) {
                this.mStrokeMap.put(stroke, new Data(point));
            } else {
                this.mStrokeMap.get(stroke).addPoint(point);
            }
        }
    }

    @Override
    public float getFalseTouchEvaluation(int type, Stroke stroke) {
        Data data = this.mStrokeMap.get(stroke);
        return SpeedRatioEvaluator.evaluate(data.maxSpeedRatio) + DistanceRatioEvaluator.evaluate(data.maxDistanceRatio);
    }

    private static class Data {
        public Point previousPoint;
        public float previousDistance = 0.0f;
        public float previousSpeed = 0.0f;
        public float maxSpeedRatio = 0.0f;
        public float maxDistanceRatio = 0.0f;

        public Data(Point point) {
            this.previousPoint = point;
        }

        public void addPoint(Point point) {
            float distance = this.previousPoint.dist(point);
            float duration = (point.timeOffsetNano - this.previousPoint.timeOffsetNano) + 1;
            float speed = distance / duration;
            if (this.previousDistance != 0.0f) {
                this.maxDistanceRatio = Math.max(this.maxDistanceRatio, distance / this.previousDistance);
            }
            if (this.previousSpeed != 0.0f) {
                this.maxSpeedRatio = Math.max(this.maxSpeedRatio, speed / this.previousSpeed);
            }
            this.previousDistance = distance;
            this.previousSpeed = speed;
            this.previousPoint = point;
        }
    }
}
