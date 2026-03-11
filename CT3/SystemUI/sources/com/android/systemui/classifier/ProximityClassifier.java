package com.android.systemui.classifier;

import android.hardware.SensorEvent;
import android.view.MotionEvent;

public class ProximityClassifier extends GestureClassifier {
    private float mAverageNear;
    private long mGestureStartTimeNano;
    private boolean mNear;
    private long mNearDuration;
    private long mNearStartTimeNano;

    public ProximityClassifier(ClassifierData classifierData) {
    }

    @Override
    public String getTag() {
        return "PROX";
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != 8) {
            return;
        }
        update(event.values[0] < event.sensor.getMaximumRange(), event.timestamp);
    }

    @Override
    public void onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == 0) {
            this.mGestureStartTimeNano = event.getEventTimeNano();
            this.mNearStartTimeNano = event.getEventTimeNano();
            this.mNearDuration = 0L;
        }
        if (action != 1 && action != 3) {
            return;
        }
        update(this.mNear, event.getEventTimeNano());
        long duration = event.getEventTimeNano() - this.mGestureStartTimeNano;
        if (duration == 0) {
            this.mAverageNear = this.mNear ? 1.0f : 0.0f;
        } else {
            this.mAverageNear = this.mNearDuration / duration;
        }
    }

    private void update(boolean near, long timestampNano) {
        if (timestampNano > this.mNearStartTimeNano) {
            if (this.mNear) {
                this.mNearDuration += timestampNano - this.mNearStartTimeNano;
            }
            if (near) {
                this.mNearStartTimeNano = timestampNano;
            }
        }
        this.mNear = near;
    }

    @Override
    public float getFalseTouchEvaluation(int type) {
        return ProximityEvaluator.evaluate(this.mAverageNear, type);
    }
}
