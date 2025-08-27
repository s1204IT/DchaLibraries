package com.android.systemui.classifier;

import android.hardware.SensorEvent;
import android.view.MotionEvent;

/* loaded from: classes.dex */
public class ProximityClassifier extends GestureClassifier {
    private float mAverageNear;
    private long mGestureStartTimeNano;
    private boolean mNear;
    private long mNearDuration;
    private long mNearStartTimeNano;

    public ProximityClassifier(ClassifierData classifierData) {
    }

    @Override // com.android.systemui.classifier.Classifier
    public String getTag() {
        return "PROX";
    }

    @Override // com.android.systemui.classifier.Classifier
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == 8) {
            update(sensorEvent.values[0] < sensorEvent.sensor.getMaximumRange(), sensorEvent.timestamp);
        }
    }

    @Override // com.android.systemui.classifier.Classifier
    public void onTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.mGestureStartTimeNano = motionEvent.getEventTimeNano();
            this.mNearStartTimeNano = motionEvent.getEventTimeNano();
            this.mNearDuration = 0L;
        }
        if (actionMasked == 1 || actionMasked == 3) {
            update(this.mNear, motionEvent.getEventTimeNano());
            long eventTimeNano = motionEvent.getEventTimeNano() - this.mGestureStartTimeNano;
            if (eventTimeNano == 0) {
                this.mAverageNear = this.mNear ? 1.0f : 0.0f;
            } else {
                this.mAverageNear = this.mNearDuration / eventTimeNano;
            }
        }
    }

    private void update(boolean z, long j) {
        if (j > this.mNearStartTimeNano) {
            if (this.mNear) {
                this.mNearDuration += j - this.mNearStartTimeNano;
            }
            if (z) {
                this.mNearStartTimeNano = j;
            }
        }
        this.mNear = z;
    }

    @Override // com.android.systemui.classifier.GestureClassifier
    public float getFalseTouchEvaluation(int i) {
        return ProximityEvaluator.evaluate(this.mAverageNear, i);
    }
}
