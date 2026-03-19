package com.android.managedprovisioning.common;

import android.graphics.Rect;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewTreeObserver;
import com.android.internal.annotations.VisibleForTesting;

public class TouchTargetEnforcer {

    @VisibleForTesting
    static final int MIN_TARGET_DP = 48;
    private final float mDensity;
    private final TouchDelegateProvider mTouchDelegateProvider;

    interface TouchDelegateProvider {
        TouchDelegate getInstance(Rect rect, View view);
    }

    public static TouchDelegate lambda$EZRJZzincYeBtnngrO1qLLSebHg(Rect rect, View view) {
        return new TouchDelegate(rect, view);
    }

    public TouchTargetEnforcer(float f) {
        this(f, new TouchDelegateProvider() {
            @Override
            public final TouchDelegate getInstance(Rect rect, View view) {
                return TouchTargetEnforcer.lambda$EZRJZzincYeBtnngrO1qLLSebHg(rect, view);
            }
        });
    }

    TouchTargetEnforcer(float f, TouchDelegateProvider touchDelegateProvider) {
        this.mDensity = f;
        this.mTouchDelegateProvider = touchDelegateProvider;
    }

    public void enforce(final View view, final View view2) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public final void onGlobalLayout() {
                TouchTargetEnforcer.lambda$enforce$1(this.f$0, view, view2);
            }
        });
    }

    public static void lambda$enforce$1(final TouchTargetEnforcer touchTargetEnforcer, final View view, final View view2) {
        final int iCeil = (int) Math.ceil(touchTargetEnforcer.dpToPx(MIN_TARGET_DP));
        final int iMax = Math.max(0, iCeil - view.getHeight());
        final int iMax2 = Math.max(0, iCeil - view.getWidth());
        if (iMax <= 0 && iMax2 <= 0) {
            return;
        }
        view2.post(new Runnable() {
            @Override
            public final void run() {
                TouchTargetEnforcer.lambda$enforce$0(this.f$0, view, iCeil, iMax2, iMax, view2);
            }
        });
    }

    public static void lambda$enforce$0(TouchTargetEnforcer touchTargetEnforcer, View view, int i, int i2, int i3, View view2) {
        Rect rectCreateNewBounds = touchTargetEnforcer.createNewBounds(view, i, i2, i3);
        synchronized (view2) {
            if (view2.getTouchDelegate() == null) {
                view2.setTouchDelegate(touchTargetEnforcer.mTouchDelegateProvider.getInstance(rectCreateNewBounds, view));
                ProvisionLogger.logd(String.format("Successfully set touch delegate on ancestor %s delegating to target %s.", view2, view));
            } else {
                ProvisionLogger.logd(String.format("Ancestor %s already has an assigned touch delegate %s. Unable to assign another one. Ignoring target.", view2, view));
            }
        }
    }

    private Rect createNewBounds(View view, int i, int i2, int i3) {
        int i4 = i2 / 2;
        int i5 = i3 / 2;
        Rect rect = new Rect();
        view.getHitRect(rect);
        rect.top -= i5;
        rect.bottom += i5;
        rect.left -= i4;
        rect.right += i4;
        int i6 = i - (rect.bottom - rect.top);
        if (i6 > 0) {
            rect.bottom += i6;
        }
        int i7 = i - (rect.right - rect.left);
        if (i7 > 0) {
            rect.right += i7;
        }
        return rect;
    }

    private float dpToPx(int i) {
        return i * this.mDensity;
    }
}
