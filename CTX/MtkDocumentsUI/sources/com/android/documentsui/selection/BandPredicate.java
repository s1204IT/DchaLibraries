package com.android.documentsui.selection;

import android.view.MotionEvent;

public abstract class BandPredicate {
    public abstract boolean canInitiate(MotionEvent motionEvent);
}
