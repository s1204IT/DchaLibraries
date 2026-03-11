package com.android.launcher3.anim;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.util.SparseArray;
import android.view.animation.Interpolator;
import com.android.launcher3.LauncherAnimUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AnimatorSetBuilder {
    public static final int ANIM_OVERVIEW_FADE = 4;
    public static final int ANIM_OVERVIEW_SCALE = 3;
    public static final int ANIM_VERTICAL_PROGRESS = 0;
    public static final int ANIM_WORKSPACE_FADE = 2;
    public static final int ANIM_WORKSPACE_SCALE = 1;
    protected final ArrayList<Animator> mAnims = new ArrayList<>();
    private final SparseArray<Interpolator> mInterpolators = new SparseArray<>();
    private List<Runnable> mOnFinishRunnables = new ArrayList();

    public void startTag(Object obj) {
    }

    public void play(Animator animator) {
        this.mAnims.add(animator);
    }

    public void addOnFinishRunnable(Runnable runnable) {
        this.mOnFinishRunnables.add(runnable);
    }

    public AnimatorSet build() {
        AnimatorSet animatorSetCreateAnimatorSet = LauncherAnimUtils.createAnimatorSet();
        animatorSetCreateAnimatorSet.playTogether(this.mAnims);
        if (!this.mOnFinishRunnables.isEmpty()) {
            animatorSetCreateAnimatorSet.addListener(new AnimationSuccessListener() {
                @Override
                public void onAnimationSuccess(Animator animator) {
                    Iterator it = AnimatorSetBuilder.this.mOnFinishRunnables.iterator();
                    while (it.hasNext()) {
                        ((Runnable) it.next()).run();
                    }
                    AnimatorSetBuilder.this.mOnFinishRunnables.clear();
                }
            });
        }
        return animatorSetCreateAnimatorSet;
    }

    public Interpolator getInterpolator(int i, Interpolator interpolator) {
        return this.mInterpolators.get(i, interpolator);
    }

    public void setInterpolator(int i, Interpolator interpolator) {
        this.mInterpolators.put(i, interpolator);
    }
}
