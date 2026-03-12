package com.android.systemui.recents.views;

import android.animation.ValueAnimator;
import android.graphics.Rect;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;

public class ViewAnimation {

    public static class TaskViewEnterContext {
        int currentStackViewCount;
        int currentStackViewIndex;
        boolean currentTaskOccludesLaunchTarget;
        Rect currentTaskRect;
        TaskViewTransform currentTaskTransform;
        public ReferenceCountedTrigger postAnimationTrigger;
        ValueAnimator.AnimatorUpdateListener updateListener;

        public TaskViewEnterContext(ReferenceCountedTrigger t) {
            this.postAnimationTrigger = t;
        }
    }

    public static class TaskViewExitContext {
        int offscreenTranslationY;
        ReferenceCountedTrigger postAnimationTrigger;

        public TaskViewExitContext(ReferenceCountedTrigger t) {
            this.postAnimationTrigger = t;
        }
    }
}
