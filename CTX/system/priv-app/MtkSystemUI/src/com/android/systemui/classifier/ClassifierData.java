package com.android.systemui.classifier;

import android.util.SparseArray;
import android.view.MotionEvent;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class ClassifierData {
    private final float mDpi;
    private SparseArray<Stroke> mCurrentStrokes = new SparseArray<>();
    private ArrayList<Stroke> mEndingStrokes = new ArrayList<>();

    public ClassifierData(float f) {
        this.mDpi = f;
    }

    public void update(MotionEvent motionEvent) {
        this.mEndingStrokes.clear();
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.mCurrentStrokes.clear();
        }
        for (int i = 0; i < motionEvent.getPointerCount(); i++) {
            int pointerId = motionEvent.getPointerId(i);
            if (this.mCurrentStrokes.get(pointerId) == null) {
                this.mCurrentStrokes.put(pointerId, new Stroke(motionEvent.getEventTimeNano(), this.mDpi));
            }
            this.mCurrentStrokes.get(pointerId).addPoint(motionEvent.getX(i), motionEvent.getY(i), motionEvent.getEventTimeNano());
            if (actionMasked == 1 || actionMasked == 3 || (actionMasked == 6 && i == motionEvent.getActionIndex())) {
                this.mEndingStrokes.add(getStroke(pointerId));
            }
        }
    }

    public void cleanUp(MotionEvent motionEvent) {
        this.mEndingStrokes.clear();
        int actionMasked = motionEvent.getActionMasked();
        for (int i = 0; i < motionEvent.getPointerCount(); i++) {
            int pointerId = motionEvent.getPointerId(i);
            if (actionMasked == 1 || actionMasked == 3 || (actionMasked == 6 && i == motionEvent.getActionIndex())) {
                this.mCurrentStrokes.remove(pointerId);
            }
        }
    }

    public ArrayList<Stroke> getEndingStrokes() {
        return this.mEndingStrokes;
    }

    public Stroke getStroke(int i) {
        return this.mCurrentStrokes.get(i);
    }
}
