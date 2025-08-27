package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/* loaded from: classes.dex */
public class NearestTouchFrame extends FrameLayout {
    private final ArrayList<View> mClickableChildren;
    private final boolean mIsActive;
    private final int[] mOffset;
    private final int[] mTmpInt;
    private View mTouchingChild;

    public NearestTouchFrame(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, context.getResources().getConfiguration());
    }

    NearestTouchFrame(Context context, AttributeSet attributeSet, Configuration configuration) {
        super(context, attributeSet);
        this.mClickableChildren = new ArrayList<>();
        this.mTmpInt = new int[2];
        this.mOffset = new int[2];
        this.mIsActive = configuration.smallestScreenWidthDp < 600;
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        this.mClickableChildren.clear();
        addClickableChildren(this);
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        getLocationInWindow(this.mOffset);
    }

    private void addClickableChildren(ViewGroup viewGroup) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = viewGroup.getChildAt(i);
            if (childAt.isClickable()) {
                this.mClickableChildren.add(childAt);
            } else if (childAt instanceof ViewGroup) {
                addClickableChildren((ViewGroup) childAt);
            }
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mIsActive) {
            if (motionEvent.getAction() == 0) {
                this.mTouchingChild = findNearestChild(motionEvent);
            }
            if (this.mTouchingChild != null) {
                motionEvent.offsetLocation((this.mTouchingChild.getWidth() / 2) - motionEvent.getX(), (this.mTouchingChild.getHeight() / 2) - motionEvent.getY());
                return this.mTouchingChild.getVisibility() == 0 && this.mTouchingChild.dispatchTouchEvent(motionEvent);
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    private View findNearestChild(final MotionEvent motionEvent) {
        return (View) ((Pair) this.mClickableChildren.stream().filter(new Predicate() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$NearestTouchFrame$vZxk9QSjtyUGPsdKpTYMWyGRwT4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((View) obj).isAttachedToWindow();
            }
        }).map(new Function() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$NearestTouchFrame$7eHiQu4mAcjiUjco05vH9suDR8c
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return NearestTouchFrame.lambda$findNearestChild$1(this.f$0, motionEvent, (View) obj);
            }
        }).min(Comparator.comparingInt(new ToIntFunction() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$NearestTouchFrame$WiqrZjV5HJ63_nsMmVLS89IoMhE
            @Override // java.util.function.ToIntFunction
            public final int applyAsInt(Object obj) {
                return ((Integer) ((Pair) obj).first).intValue();
            }
        })).get()).second;
    }

    /* JADX DEBUG: Can't inline method, not implemented redirect type for insn: 0x000a: CONSTRUCTOR 
  (wrap:java.lang.Integer:0x0006: INVOKE 
  (wrap:int:0x0002: INVOKE 
  (r1v0 com.android.systemui.statusbar.phone.NearestTouchFrame)
  (r3v0 android.view.View)
  (r2v0 android.view.MotionEvent)
 DIRECT call: com.android.systemui.statusbar.phone.NearestTouchFrame.distance(android.view.View, android.view.MotionEvent):int A[MD:(android.view.View, android.view.MotionEvent):int (m), WRAPPED])
 STATIC call: java.lang.Integer.valueOf(int):java.lang.Integer A[MD:(int):java.lang.Integer (c), WRAPPED])
  (r3v0 android.view.View)
 A[MD:(F, S):void (c)] (LINE:101) call: android.util.Pair.<init>(java.lang.Object, java.lang.Object):void type: CONSTRUCTOR */
    public static /* synthetic */ Pair lambda$findNearestChild$1(NearestTouchFrame nearestTouchFrame, MotionEvent motionEvent, View view) {
        return new Pair(Integer.valueOf(nearestTouchFrame.distance(view, motionEvent)), view);
    }

    private int distance(View view, MotionEvent motionEvent) {
        view.getLocationInWindow(this.mTmpInt);
        int i = this.mTmpInt[0] - this.mOffset[0];
        int i2 = this.mTmpInt[1] - this.mOffset[1];
        return Math.max(Math.min(Math.abs(i - ((int) motionEvent.getX())), Math.abs(((int) motionEvent.getX()) - (view.getWidth() + i))), Math.min(Math.abs(i2 - ((int) motionEvent.getY())), Math.abs(((int) motionEvent.getY()) - (view.getHeight() + i2))));
    }
}
