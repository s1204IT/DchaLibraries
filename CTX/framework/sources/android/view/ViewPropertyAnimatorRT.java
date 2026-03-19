package android.view;

import android.animation.TimeInterpolator;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import com.android.internal.view.animation.FallbackLUTInterpolator;
import java.util.ArrayList;

class ViewPropertyAnimatorRT {
    private static final Interpolator sLinearInterpolator = new LinearInterpolator();
    private RenderNodeAnimator[] mAnimators = new RenderNodeAnimator[12];
    private final View mView;

    ViewPropertyAnimatorRT(View view) {
        this.mView = view;
    }

    public boolean startAnimation(ViewPropertyAnimator viewPropertyAnimator) {
        cancelAnimators(viewPropertyAnimator.mPendingAnimations);
        if (!canHandleAnimator(viewPropertyAnimator)) {
            return false;
        }
        doStartAnimation(viewPropertyAnimator);
        return true;
    }

    public void cancelAll() {
        for (int i = 0; i < this.mAnimators.length; i++) {
            if (this.mAnimators[i] != null) {
                this.mAnimators[i].cancel();
                this.mAnimators[i] = null;
            }
        }
    }

    private void doStartAnimation(ViewPropertyAnimator viewPropertyAnimator) {
        int size = viewPropertyAnimator.mPendingAnimations.size();
        long startDelay = viewPropertyAnimator.getStartDelay();
        long duration = viewPropertyAnimator.getDuration();
        TimeInterpolator interpolator = viewPropertyAnimator.getInterpolator();
        if (interpolator == null) {
            interpolator = sLinearInterpolator;
        }
        if (!RenderNodeAnimator.isNativeInterpolator(interpolator)) {
            interpolator = new FallbackLUTInterpolator(interpolator, duration);
        }
        for (int i = 0; i < size; i++) {
            ViewPropertyAnimator.NameValuesHolder nameValuesHolder = viewPropertyAnimator.mPendingAnimations.get(i);
            int iMapViewPropertyToRenderProperty = RenderNodeAnimator.mapViewPropertyToRenderProperty(nameValuesHolder.mNameConstant);
            RenderNodeAnimator renderNodeAnimator = new RenderNodeAnimator(iMapViewPropertyToRenderProperty, nameValuesHolder.mFromValue + nameValuesHolder.mDeltaValue);
            renderNodeAnimator.setStartDelay(startDelay);
            renderNodeAnimator.setDuration(duration);
            renderNodeAnimator.setInterpolator(interpolator);
            renderNodeAnimator.setTarget(this.mView);
            renderNodeAnimator.start();
            this.mAnimators[iMapViewPropertyToRenderProperty] = renderNodeAnimator;
        }
        viewPropertyAnimator.mPendingAnimations.clear();
    }

    private boolean canHandleAnimator(ViewPropertyAnimator viewPropertyAnimator) {
        return viewPropertyAnimator.getUpdateListener() == null && viewPropertyAnimator.getListener() == null && this.mView.isHardwareAccelerated() && !viewPropertyAnimator.hasActions();
    }

    private void cancelAnimators(ArrayList<ViewPropertyAnimator.NameValuesHolder> arrayList) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            int iMapViewPropertyToRenderProperty = RenderNodeAnimator.mapViewPropertyToRenderProperty(arrayList.get(i).mNameConstant);
            if (this.mAnimators[iMapViewPropertyToRenderProperty] != null) {
                this.mAnimators[iMapViewPropertyToRenderProperty].cancel();
                this.mAnimators[iMapViewPropertyToRenderProperty] = null;
            }
        }
    }
}
