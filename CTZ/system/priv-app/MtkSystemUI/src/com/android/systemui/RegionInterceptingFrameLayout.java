package com.android.systemui;

import android.content.Context;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

/* loaded from: classes.dex */
public class RegionInterceptingFrameLayout extends FrameLayout {
    private final ViewTreeObserver.OnComputeInternalInsetsListener mInsetsListener;

    public RegionInterceptingFrameLayout(Context context) {
        super(context);
        this.mInsetsListener = new ViewTreeObserver.OnComputeInternalInsetsListener() { // from class: com.android.systemui.-$$Lambda$RegionInterceptingFrameLayout$JlFdsR9I_9ubvsna7k1PTnmr7OI
            public final void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
                RegionInterceptingFrameLayout.lambda$new$0(this.f$0, internalInsetsInfo);
            }
        };
    }

    public RegionInterceptingFrameLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mInsetsListener = new ViewTreeObserver.OnComputeInternalInsetsListener() { // from class: com.android.systemui.-$$Lambda$RegionInterceptingFrameLayout$JlFdsR9I_9ubvsna7k1PTnmr7OI
            public final void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
                RegionInterceptingFrameLayout.lambda$new$0(this.f$0, internalInsetsInfo);
            }
        };
    }

    public RegionInterceptingFrameLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mInsetsListener = new ViewTreeObserver.OnComputeInternalInsetsListener() { // from class: com.android.systemui.-$$Lambda$RegionInterceptingFrameLayout$JlFdsR9I_9ubvsna7k1PTnmr7OI
            public final void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
                RegionInterceptingFrameLayout.lambda$new$0(this.f$0, internalInsetsInfo);
            }
        };
    }

    public RegionInterceptingFrameLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mInsetsListener = new ViewTreeObserver.OnComputeInternalInsetsListener() { // from class: com.android.systemui.-$$Lambda$RegionInterceptingFrameLayout$JlFdsR9I_9ubvsna7k1PTnmr7OI
            public final void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
                RegionInterceptingFrameLayout.lambda$new$0(this.f$0, internalInsetsInfo);
            }
        };
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnComputeInternalInsetsListener(this.mInsetsListener);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnComputeInternalInsetsListener(this.mInsetsListener);
    }

    public static /* synthetic */ void lambda$new$0(RegionInterceptingFrameLayout regionInterceptingFrameLayout, ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
        internalInsetsInfo.setTouchableInsets(3);
        internalInsetsInfo.touchableRegion.setEmpty();
        for (int i = 0; i < regionInterceptingFrameLayout.getChildCount(); i++) {
            KeyEvent.Callback childAt = regionInterceptingFrameLayout.getChildAt(i);
            if (childAt instanceof RegionInterceptableView) {
                RegionInterceptableView regionInterceptableView = (RegionInterceptableView) childAt;
                if (regionInterceptableView.shouldInterceptTouch() && regionInterceptableView.getInterceptRegion() != null) {
                    internalInsetsInfo.touchableRegion.op(regionInterceptableView.getInterceptRegion(), Region.Op.UNION);
                }
            }
        }
    }

    public interface RegionInterceptableView {
        Region getInterceptRegion();

        default boolean shouldInterceptTouch() {
            return false;
        }
    }
}
