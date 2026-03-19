package android.media.update;

import android.content.Context;
import android.media.update.ViewGroupProvider;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public abstract class ViewGroupHelper<T extends ViewGroupProvider> extends ViewGroup {
    public final T mProvider;

    @FunctionalInterface
    public interface ProviderCreator<T extends ViewGroupProvider> {
        T createProvider(ViewGroupHelper<T> viewGroupHelper, ViewGroupProvider viewGroupProvider, ViewGroupProvider viewGroupProvider2);
    }

    public ViewGroupHelper(ProviderCreator<T> providerCreator, Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mProvider = (T) providerCreator.createProvider(this, new SuperProvider(), new PrivateProvider());
    }

    public T getProvider() {
        return this.mProvider;
    }

    @Override
    protected void onAttachedToWindow() {
        this.mProvider.onAttachedToWindow_impl();
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mProvider.onDetachedFromWindow_impl();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return this.mProvider.getAccessibilityClassName_impl();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.mProvider.onTouchEvent_impl(motionEvent);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent motionEvent) {
        return this.mProvider.onTrackballEvent_impl(motionEvent);
    }

    @Override
    public void onFinishInflate() {
        this.mProvider.onFinishInflate_impl();
    }

    @Override
    public void setEnabled(boolean z) {
        this.mProvider.setEnabled_impl(z);
    }

    @Override
    public void onVisibilityAggregated(boolean z) {
        this.mProvider.onVisibilityAggregated_impl(z);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        this.mProvider.onLayout_impl(z, i, i2, i3, i4);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        this.mProvider.onMeasure_impl(i, i2);
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return this.mProvider.getSuggestedMinimumWidth_impl();
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return this.mProvider.getSuggestedMinimumHeight_impl();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        return this.mProvider.dispatchTouchEvent_impl(motionEvent);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return this.mProvider.checkLayoutParams_impl(layoutParams);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return this.mProvider.generateDefaultLayoutParams_impl();
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return this.mProvider.generateLayoutParams_impl(attributeSet);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return this.mProvider.generateLayoutParams_impl(layoutParams);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return this.mProvider.shouldDelayChildPressedState_impl();
    }

    @Override
    protected void measureChildWithMargins(View view, int i, int i2, int i3, int i4) {
        this.mProvider.measureChildWithMargins_impl(view, i, i2, i3, i4);
    }

    public class SuperProvider implements ViewGroupProvider {
        public SuperProvider() {
        }

        @Override
        public CharSequence getAccessibilityClassName_impl() {
            return ViewGroupHelper.super.getAccessibilityClassName();
        }

        @Override
        public boolean onTouchEvent_impl(MotionEvent motionEvent) {
            return ViewGroupHelper.super.onTouchEvent(motionEvent);
        }

        @Override
        public boolean onTrackballEvent_impl(MotionEvent motionEvent) {
            return ViewGroupHelper.super.onTrackballEvent(motionEvent);
        }

        @Override
        public void onFinishInflate_impl() {
            ViewGroupHelper.super.onFinishInflate();
        }

        @Override
        public void setEnabled_impl(boolean z) {
            ViewGroupHelper.super.setEnabled(z);
        }

        @Override
        public void onAttachedToWindow_impl() {
            ViewGroupHelper.super.onAttachedToWindow();
        }

        @Override
        public void onDetachedFromWindow_impl() {
            ViewGroupHelper.super.onDetachedFromWindow();
        }

        @Override
        public void onVisibilityAggregated_impl(boolean z) {
            ViewGroupHelper.super.onVisibilityAggregated(z);
        }

        @Override
        public void onLayout_impl(boolean z, int i, int i2, int i3, int i4) {
        }

        @Override
        public void onMeasure_impl(int i, int i2) {
            ViewGroupHelper.super.onMeasure(i, i2);
        }

        @Override
        public int getSuggestedMinimumWidth_impl() {
            return ViewGroupHelper.super.getSuggestedMinimumWidth();
        }

        @Override
        public int getSuggestedMinimumHeight_impl() {
            return ViewGroupHelper.super.getSuggestedMinimumHeight();
        }

        @Override
        public void setMeasuredDimension_impl(int i, int i2) {
            ViewGroupHelper.super.setMeasuredDimension(i, i2);
        }

        @Override
        public boolean dispatchTouchEvent_impl(MotionEvent motionEvent) {
            return ViewGroupHelper.super.dispatchTouchEvent(motionEvent);
        }

        @Override
        public boolean checkLayoutParams_impl(ViewGroup.LayoutParams layoutParams) {
            return ViewGroupHelper.super.checkLayoutParams(layoutParams);
        }

        @Override
        public ViewGroup.LayoutParams generateDefaultLayoutParams_impl() {
            return ViewGroupHelper.super.generateDefaultLayoutParams();
        }

        @Override
        public ViewGroup.LayoutParams generateLayoutParams_impl(AttributeSet attributeSet) {
            return ViewGroupHelper.super.generateLayoutParams(attributeSet);
        }

        @Override
        public ViewGroup.LayoutParams generateLayoutParams_impl(ViewGroup.LayoutParams layoutParams) {
            return ViewGroupHelper.super.generateLayoutParams(layoutParams);
        }

        @Override
        public boolean shouldDelayChildPressedState_impl() {
            return ViewGroupHelper.super.shouldDelayChildPressedState();
        }

        @Override
        public void measureChildWithMargins_impl(View view, int i, int i2, int i3, int i4) {
            ViewGroupHelper.super.measureChildWithMargins(view, i, i2, i3, i4);
        }
    }

    public class PrivateProvider implements ViewGroupProvider {
        public PrivateProvider() {
        }

        @Override
        public CharSequence getAccessibilityClassName_impl() {
            return ViewGroupHelper.this.getAccessibilityClassName();
        }

        @Override
        public boolean onTouchEvent_impl(MotionEvent motionEvent) {
            return ViewGroupHelper.this.onTouchEvent(motionEvent);
        }

        @Override
        public boolean onTrackballEvent_impl(MotionEvent motionEvent) {
            return ViewGroupHelper.this.onTrackballEvent(motionEvent);
        }

        @Override
        public void onFinishInflate_impl() {
            ViewGroupHelper.this.onFinishInflate();
        }

        @Override
        public void setEnabled_impl(boolean z) {
            ViewGroupHelper.this.setEnabled(z);
        }

        @Override
        public void onAttachedToWindow_impl() {
            ViewGroupHelper.this.onAttachedToWindow();
        }

        @Override
        public void onDetachedFromWindow_impl() {
            ViewGroupHelper.this.onDetachedFromWindow();
        }

        @Override
        public void onVisibilityAggregated_impl(boolean z) {
            ViewGroupHelper.this.onVisibilityAggregated(z);
        }

        @Override
        public void onLayout_impl(boolean z, int i, int i2, int i3, int i4) {
            ViewGroupHelper.this.onLayout(z, i, i2, i3, i4);
        }

        @Override
        public void onMeasure_impl(int i, int i2) {
            ViewGroupHelper.this.onMeasure(i, i2);
        }

        @Override
        public int getSuggestedMinimumWidth_impl() {
            return ViewGroupHelper.this.getSuggestedMinimumWidth();
        }

        @Override
        public int getSuggestedMinimumHeight_impl() {
            return ViewGroupHelper.this.getSuggestedMinimumHeight();
        }

        @Override
        public void setMeasuredDimension_impl(int i, int i2) {
            ViewGroupHelper.this.setMeasuredDimension(i, i2);
        }

        @Override
        public boolean dispatchTouchEvent_impl(MotionEvent motionEvent) {
            return ViewGroupHelper.this.dispatchTouchEvent(motionEvent);
        }

        @Override
        public boolean checkLayoutParams_impl(ViewGroup.LayoutParams layoutParams) {
            return ViewGroupHelper.this.checkLayoutParams(layoutParams);
        }

        @Override
        public ViewGroup.LayoutParams generateDefaultLayoutParams_impl() {
            return ViewGroupHelper.this.generateDefaultLayoutParams();
        }

        @Override
        public ViewGroup.LayoutParams generateLayoutParams_impl(AttributeSet attributeSet) {
            return ViewGroupHelper.this.generateLayoutParams(attributeSet);
        }

        @Override
        public ViewGroup.LayoutParams generateLayoutParams_impl(ViewGroup.LayoutParams layoutParams) {
            return ViewGroupHelper.this.generateLayoutParams(layoutParams);
        }

        @Override
        public boolean shouldDelayChildPressedState_impl() {
            return ViewGroupHelper.this.shouldDelayChildPressedState();
        }

        @Override
        public void measureChildWithMargins_impl(View view, int i, int i2, int i3, int i4) {
            ViewGroupHelper.this.measureChildWithMargins(view, i, i2, i3, i4);
        }
    }
}
