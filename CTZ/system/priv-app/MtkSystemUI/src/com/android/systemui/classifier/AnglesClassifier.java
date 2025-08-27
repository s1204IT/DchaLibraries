package com.android.systemui.classifier;

import android.os.Build;
import android.os.SystemProperties;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/* loaded from: classes.dex */
public class AnglesClassifier extends StrokeClassifier {
    private HashMap<Stroke, Data> mStrokeMap = new HashMap<>();
    public static final boolean VERBOSE = SystemProperties.getBoolean("debug.falsing_log.ang", Build.IS_DEBUGGABLE);
    private static String TAG = "ANG";

    public AnglesClassifier(ClassifierData classifierData) {
        this.mClassifierData = classifierData;
    }

    @Override // com.android.systemui.classifier.Classifier
    public String getTag() {
        return TAG;
    }

    @Override // com.android.systemui.classifier.Classifier
    public void onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 0) {
            this.mStrokeMap.clear();
        }
        for (int i = 0; i < motionEvent.getPointerCount(); i++) {
            Stroke stroke = this.mClassifierData.getStroke(motionEvent.getPointerId(i));
            if (this.mStrokeMap.get(stroke) == null) {
                this.mStrokeMap.put(stroke, new Data());
            }
            this.mStrokeMap.get(stroke).addPoint(stroke.getPoints().get(stroke.getPoints().size() - 1));
        }
    }

    @Override // com.android.systemui.classifier.StrokeClassifier
    public float getFalseTouchEvaluation(int i, Stroke stroke) {
        Data data = this.mStrokeMap.get(stroke);
        return AnglesVarianceEvaluator.evaluate(data.getAnglesVariance(), i) + AnglesPercentageEvaluator.evaluate(data.getAnglesPercentage(), i);
    }

    private static class Data {
        private final float ANGLE_DEVIATION = 0.15707964f;
        private List<Point> mLastThreePoints = new ArrayList();
        private float mFirstAngleVariance = 0.0f;
        private float mPreviousAngle = 3.1415927f;
        private float mBiggestAngle = 0.0f;
        private float mSecondSumSquares = 0.0f;
        private float mSumSquares = 0.0f;
        private float mSecondSum = 0.0f;
        private float mSum = 0.0f;
        private float mSecondCount = 1.0f;
        private float mCount = 1.0f;
        private float mFirstLength = 0.0f;
        private float mLength = 0.0f;
        private float mStraightAngles = 0.0f;
        private float mRightAngles = 0.0f;
        private float mLeftAngles = 0.0f;
        private float mAnglesCount = 0.0f;

        public void addPoint(Point point) {
            if (this.mLastThreePoints.isEmpty() || !this.mLastThreePoints.get(this.mLastThreePoints.size() - 1).equals(point)) {
                if (!this.mLastThreePoints.isEmpty()) {
                    this.mLength += this.mLastThreePoints.get(this.mLastThreePoints.size() - 1).dist(point);
                }
                this.mLastThreePoints.add(point);
                if (this.mLastThreePoints.size() == 4) {
                    this.mLastThreePoints.remove(0);
                    float angle = this.mLastThreePoints.get(1).getAngle(this.mLastThreePoints.get(0), this.mLastThreePoints.get(2));
                    this.mAnglesCount += 1.0f;
                    double d = angle;
                    if (d < 2.9845130165391645d) {
                        this.mLeftAngles += 1.0f;
                    } else if (d <= 3.298672290640422d) {
                        this.mStraightAngles += 1.0f;
                    } else {
                        this.mRightAngles += 1.0f;
                    }
                    float f = angle - this.mPreviousAngle;
                    if (this.mBiggestAngle < angle) {
                        this.mBiggestAngle = angle;
                        this.mFirstLength = this.mLength;
                        this.mFirstAngleVariance = getAnglesVariance(this.mSumSquares, this.mSum, this.mCount);
                        this.mSecondSumSquares = 0.0f;
                        this.mSecondSum = 0.0f;
                        this.mSecondCount = 1.0f;
                    } else {
                        this.mSecondSum += f;
                        this.mSecondSumSquares += f * f;
                        this.mSecondCount = (float) (this.mSecondCount + 1.0d);
                    }
                    this.mSum += f;
                    this.mSumSquares += f * f;
                    this.mCount = (float) (this.mCount + 1.0d);
                    this.mPreviousAngle = angle;
                }
            }
        }

        public float getAnglesVariance(float f, float f2, float f3) {
            float f4 = f2 / f3;
            return (f / f3) - (f4 * f4);
        }

        public float getAnglesVariance() {
            float anglesVariance = getAnglesVariance(this.mSumSquares, this.mSum, this.mCount);
            if (AnglesClassifier.VERBOSE) {
                FalsingLog.i(AnglesClassifier.TAG, "getAnglesVariance: (first pass) " + anglesVariance);
                FalsingLog.i(AnglesClassifier.TAG, "   - mFirstLength=" + this.mFirstLength);
                FalsingLog.i(AnglesClassifier.TAG, "   - mLength=" + this.mLength);
            }
            if (this.mFirstLength < this.mLength / 2.0f) {
                anglesVariance = Math.min(anglesVariance, this.mFirstAngleVariance + getAnglesVariance(this.mSecondSumSquares, this.mSecondSum, this.mSecondCount));
                if (AnglesClassifier.VERBOSE) {
                    FalsingLog.i(AnglesClassifier.TAG, "getAnglesVariance: (second pass) " + anglesVariance);
                }
            }
            return anglesVariance;
        }

        public float getAnglesPercentage() {
            if (this.mAnglesCount == 0.0f) {
                if (AnglesClassifier.VERBOSE) {
                    FalsingLog.i(AnglesClassifier.TAG, "getAnglesPercentage: count==0, result=1");
                    return 1.0f;
                }
                return 1.0f;
            }
            float fMax = (Math.max(this.mLeftAngles, this.mRightAngles) + this.mStraightAngles) / this.mAnglesCount;
            if (AnglesClassifier.VERBOSE) {
                FalsingLog.i(AnglesClassifier.TAG, "getAnglesPercentage: left=" + this.mLeftAngles + " right=" + this.mRightAngles + " straight=" + this.mStraightAngles + " count=" + this.mAnglesCount + " result=" + fMax);
            }
            return fMax;
        }
    }
}
