package com.android.systemui.classifier;

import android.os.Build;
import android.os.SystemProperties;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/* loaded from: classes.dex */
public class SpeedAnglesClassifier extends StrokeClassifier {
    public static final boolean VERBOSE = SystemProperties.getBoolean("debug.falsing_log.spd_ang", Build.IS_DEBUGGABLE);
    private HashMap<Stroke, Data> mStrokeMap = new HashMap<>();

    public SpeedAnglesClassifier(ClassifierData classifierData) {
        this.mClassifierData = classifierData;
    }

    @Override // com.android.systemui.classifier.Classifier
    public String getTag() {
        return "SPD_ANG";
    }

    @Override // com.android.systemui.classifier.Classifier
    public void onTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.mStrokeMap.clear();
        }
        for (int i = 0; i < motionEvent.getPointerCount(); i++) {
            Stroke stroke = this.mClassifierData.getStroke(motionEvent.getPointerId(i));
            if (this.mStrokeMap.get(stroke) == null) {
                this.mStrokeMap.put(stroke, new Data());
            }
            if (actionMasked != 1 && actionMasked != 3 && (actionMasked != 6 || i != motionEvent.getActionIndex())) {
                this.mStrokeMap.get(stroke).addPoint(stroke.getPoints().get(stroke.getPoints().size() - 1));
            }
        }
    }

    @Override // com.android.systemui.classifier.StrokeClassifier
    public float getFalseTouchEvaluation(int i, Stroke stroke) {
        Data data = this.mStrokeMap.get(stroke);
        return SpeedVarianceEvaluator.evaluate(data.getAnglesVariance()) + SpeedAnglesPercentageEvaluator.evaluate(data.getAnglesPercentage());
    }

    private static class Data {
        private final float DURATION_SCALE = 1.0E8f;
        private final float LENGTH_SCALE = 1.0f;
        private final float ANGLE_DEVIATION = 0.31415927f;
        private List<Point> mLastThreePoints = new ArrayList();
        private Point mPreviousPoint = null;
        private float mPreviousAngle = 3.1415927f;
        private float mSumSquares = 0.0f;
        private float mSum = 0.0f;
        private float mCount = 1.0f;
        private float mDist = 0.0f;
        private float mAcceleratingAngles = 0.0f;
        private float mAnglesCount = 0.0f;

        public void addPoint(Point point) {
            if (this.mPreviousPoint != null) {
                this.mDist += this.mPreviousPoint.dist(point);
            }
            this.mPreviousPoint = point;
            Point point2 = new Point(point.timeOffsetNano / 1.0E8f, this.mDist / 1.0f);
            if (this.mLastThreePoints.isEmpty() || !this.mLastThreePoints.get(this.mLastThreePoints.size() - 1).equals(point2)) {
                this.mLastThreePoints.add(point2);
                if (this.mLastThreePoints.size() == 4) {
                    this.mLastThreePoints.remove(0);
                    float angle = this.mLastThreePoints.get(1).getAngle(this.mLastThreePoints.get(0), this.mLastThreePoints.get(2));
                    this.mAnglesCount += 1.0f;
                    if (angle >= 2.8274336f) {
                        this.mAcceleratingAngles += 1.0f;
                    }
                    float f = angle - this.mPreviousAngle;
                    this.mSum += f;
                    this.mSumSquares += f * f;
                    this.mCount = (float) (this.mCount + 1.0d);
                    this.mPreviousAngle = angle;
                }
            }
        }

        public float getAnglesVariance() {
            float f = (this.mSumSquares / this.mCount) - ((this.mSum / this.mCount) * (this.mSum / this.mCount));
            if (SpeedAnglesClassifier.VERBOSE) {
                FalsingLog.i("SPD_ANG", "getAnglesVariance: sum^2=" + this.mSumSquares + " count=" + this.mCount + " result=" + f);
            }
            return f;
        }

        public float getAnglesPercentage() {
            if (this.mAnglesCount == 0.0f) {
                return 1.0f;
            }
            float f = this.mAcceleratingAngles / this.mAnglesCount;
            if (SpeedAnglesClassifier.VERBOSE) {
                FalsingLog.i("SPD_ANG", "getAnglesPercentage: angles=" + this.mAcceleratingAngles + " count=" + this.mAnglesCount + " result=" + f);
            }
            return f;
        }
    }
}
