package com.android.systemui.classifier;

import android.os.SystemClock;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class HistoryEvaluator {
    private final ArrayList<Data> mStrokes = new ArrayList<>();
    private final ArrayList<Data> mGestureWeights = new ArrayList<>();
    private long mLastUpdate = SystemClock.elapsedRealtime();

    public void addStroke(float f) {
        decayValue();
        this.mStrokes.add(new Data(f));
    }

    public void addGesture(float f) {
        decayValue();
        this.mGestureWeights.add(new Data(f));
    }

    public float getEvaluation() {
        return weightedAverage(this.mStrokes) + weightedAverage(this.mGestureWeights);
    }

    private float weightedAverage(ArrayList<Data> arrayList) {
        int size = arrayList.size();
        float f = 0.0f;
        float f2 = 0.0f;
        for (int i = 0; i < size; i++) {
            Data data = arrayList.get(i);
            f += data.evaluation * data.weight;
            f2 += data.weight;
        }
        if (f2 == 0.0f) {
            return 0.0f;
        }
        return f / f2;
    }

    private void decayValue() {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        if (jElapsedRealtime <= this.mLastUpdate) {
            return;
        }
        float fPow = (float) Math.pow(0.8999999761581421d, (jElapsedRealtime - this.mLastUpdate) / 50.0f);
        decayValue(this.mStrokes, fPow);
        decayValue(this.mGestureWeights, fPow);
        this.mLastUpdate = jElapsedRealtime;
    }

    private void decayValue(ArrayList<Data> arrayList, float f) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            arrayList.get(i).weight *= f;
        }
        while (!arrayList.isEmpty() && isZero(arrayList.get(0).weight)) {
            arrayList.remove(0);
        }
    }

    private boolean isZero(float f) {
        return f <= 1.0E-5f && f >= -1.0E-5f;
    }

    private static class Data {
        public float evaluation;
        public float weight = 1.0f;

        public Data(float f) {
            this.evaluation = f;
        }
    }
}
