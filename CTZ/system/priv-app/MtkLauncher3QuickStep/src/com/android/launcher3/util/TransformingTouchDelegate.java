package com.android.launcher3.util;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;

/* loaded from: classes.dex */
public class TransformingTouchDelegate extends TouchDelegate {
    private static final Rect sTempRect = new Rect();
    private final RectF mBounds;
    private boolean mDelegateTargeted;
    private View mDelegateView;
    private final RectF mTouchCheckBounds;
    private float mTouchExtension;
    private boolean mWasTouchOutsideBounds;

    public TransformingTouchDelegate(View view) {
        super(sTempRect, view);
        this.mDelegateView = view;
        this.mBounds = new RectF();
        this.mTouchCheckBounds = new RectF();
    }

    public void setBounds(int i, int i2, int i3, int i4) {
        this.mBounds.set(i, i2, i3, i4);
        updateTouchBounds();
    }

    public void extendTouchBounds(float f) {
        this.mTouchExtension = f;
        updateTouchBounds();
    }

    private void updateTouchBounds() {
        this.mTouchCheckBounds.set(this.mBounds);
        this.mTouchCheckBounds.inset(-this.mTouchExtension, -this.mTouchExtension);
    }

    public void setDelegateView(View view) {
        this.mDelegateView = view;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:10:0x0039  */
    @Override // android.view.TouchDelegate
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean z = true;
        switch (motionEvent.getAction()) {
            case 0:
                this.mDelegateTargeted = this.mTouchCheckBounds.contains(motionEvent.getX(), motionEvent.getY());
                if (this.mDelegateTargeted) {
                    this.mWasTouchOutsideBounds = !this.mBounds.contains(motionEvent.getX(), motionEvent.getY());
                    break;
                } else {
                    z = false;
                    break;
                }
            case 1:
            case 3:
                z = this.mDelegateTargeted;
                this.mDelegateTargeted = false;
                break;
            case 2:
                z = this.mDelegateTargeted;
                break;
        }
        if (!z) {
            return false;
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        if (this.mWasTouchOutsideBounds) {
            motionEvent.setLocation(this.mBounds.centerX(), this.mBounds.centerY());
        } else {
            motionEvent.offsetLocation(-this.mBounds.left, -this.mBounds.top);
        }
        boolean zDispatchTouchEvent = this.mDelegateView.dispatchTouchEvent(motionEvent);
        motionEvent.setLocation(x, y);
        return zDispatchTouchEvent;
    }
}
