package com.android.settings;

import android.animation.Animator;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.lang.reflect.Array;

/* loaded from: classes.dex */
public class PreviewPagerAdapter extends PagerAdapter {
    private static final Interpolator FADE_IN_INTERPOLATOR = new DecelerateInterpolator();
    private static final Interpolator FADE_OUT_INTERPOLATOR = new AccelerateInterpolator();
    private int mAnimationCounter;
    private Runnable mAnimationEndAction;
    private boolean mIsLayoutRtl;
    private FrameLayout[] mPreviewFrames;
    private boolean[][] mViewStubInflated;

    static /* synthetic */ int access$208(PreviewPagerAdapter previewPagerAdapter) {
        int i = previewPagerAdapter.mAnimationCounter;
        previewPagerAdapter.mAnimationCounter = i + 1;
        return i;
    }

    static /* synthetic */ int access$210(PreviewPagerAdapter previewPagerAdapter) {
        int i = previewPagerAdapter.mAnimationCounter;
        previewPagerAdapter.mAnimationCounter = i - 1;
        return i;
    }

    public PreviewPagerAdapter(Context context, boolean z, int[] iArr, Configuration[] configurationArr) {
        this.mIsLayoutRtl = z;
        this.mPreviewFrames = new FrameLayout[iArr.length];
        this.mViewStubInflated = (boolean[][]) Array.newInstance((Class<?>) boolean.class, iArr.length, configurationArr.length);
        for (final int i = 0; i < iArr.length; i++) {
            int length = this.mIsLayoutRtl ? (iArr.length - 1) - i : i;
            this.mPreviewFrames[length] = new FrameLayout(context);
            this.mPreviewFrames[length].setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
            for (final int i2 = 0; i2 < configurationArr.length; i2++) {
                Context contextCreateConfigurationContext = context.createConfigurationContext(configurationArr[i2]);
                contextCreateConfigurationContext.getTheme().setTo(context.getTheme());
                LayoutInflater.from(contextCreateConfigurationContext);
                ViewStub viewStub = new ViewStub(contextCreateConfigurationContext);
                viewStub.setLayoutResource(iArr[i]);
                viewStub.setOnInflateListener(new ViewStub.OnInflateListener() { // from class: com.android.settings.PreviewPagerAdapter.1
                    @Override // android.view.ViewStub.OnInflateListener
                    public void onInflate(ViewStub viewStub2, View view) {
                        view.setVisibility(viewStub2.getVisibility());
                        PreviewPagerAdapter.this.mViewStubInflated[i][i2] = true;
                    }
                });
                this.mPreviewFrames[length].addView(viewStub);
            }
        }
    }

    @Override // android.support.v4.view.PagerAdapter
    public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
        viewGroup.removeView((View) obj);
    }

    @Override // android.support.v4.view.PagerAdapter
    public int getCount() {
        return this.mPreviewFrames.length;
    }

    @Override // android.support.v4.view.PagerAdapter
    public Object instantiateItem(ViewGroup viewGroup, int i) {
        viewGroup.addView(this.mPreviewFrames[i]);
        return this.mPreviewFrames[i];
    }

    @Override // android.support.v4.view.PagerAdapter
    public boolean isViewFromObject(View view, Object obj) {
        return view == obj;
    }

    public boolean isAnimating() {
        return this.mAnimationCounter > 0;
    }

    public void setAnimationEndAction(Runnable runnable) {
        this.mAnimationEndAction = runnable;
    }

    public void setPreviewLayer(int i, int i2, int i3, boolean z) {
        for (FrameLayout frameLayout : this.mPreviewFrames) {
            if (i2 >= 0) {
                View childAt = frameLayout.getChildAt(i2);
                if (this.mViewStubInflated[i3][i2]) {
                    if (frameLayout == this.mPreviewFrames[i3]) {
                        setVisibility(childAt, 4, z);
                    } else {
                        setVisibility(childAt, 4, false);
                    }
                }
            }
            View childAt2 = frameLayout.getChildAt(i);
            if (frameLayout == this.mPreviewFrames[i3]) {
                if (!this.mViewStubInflated[i3][i]) {
                    childAt2 = ((ViewStub) childAt2).inflate();
                    childAt2.setAlpha(0.0f);
                }
                setVisibility(childAt2, 0, z);
            } else {
                setVisibility(childAt2, 0, false);
            }
        }
    }

    private void setVisibility(final View view, final int i, boolean z) {
        float f = i == 0 ? 1.0f : 0.0f;
        if (!z) {
            view.setAlpha(f);
            view.setVisibility(i);
            return;
        }
        if (i == 0) {
            Interpolator interpolator = FADE_IN_INTERPOLATOR;
        } else {
            Interpolator interpolator2 = FADE_OUT_INTERPOLATOR;
        }
        if (i == 0) {
            view.animate().alpha(f).setInterpolator(FADE_IN_INTERPOLATOR).setDuration(400L).setListener(new PreviewFrameAnimatorListener()).withStartAction(new Runnable() { // from class: com.android.settings.PreviewPagerAdapter.2
                @Override // java.lang.Runnable
                public void run() {
                    view.setVisibility(i);
                }
            });
        } else {
            view.animate().alpha(f).setInterpolator(FADE_OUT_INTERPOLATOR).setDuration(400L).setListener(new PreviewFrameAnimatorListener()).withEndAction(new Runnable() { // from class: com.android.settings.PreviewPagerAdapter.3
                @Override // java.lang.Runnable
                public void run() {
                    view.setVisibility(i);
                }
            });
        }
    }

    private void runAnimationEndAction() {
        if (this.mAnimationEndAction != null && !isAnimating()) {
            this.mAnimationEndAction.run();
            this.mAnimationEndAction = null;
        }
    }

    private class PreviewFrameAnimatorListener implements Animator.AnimatorListener {
        private PreviewFrameAnimatorListener() {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
            PreviewPagerAdapter.access$208(PreviewPagerAdapter.this);
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            PreviewPagerAdapter.access$210(PreviewPagerAdapter.this);
            PreviewPagerAdapter.this.runAnimationEndAction();
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animator) {
        }
    }
}
