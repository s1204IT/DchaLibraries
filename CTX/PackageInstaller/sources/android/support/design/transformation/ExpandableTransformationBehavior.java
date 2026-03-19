package android.support.design.transformation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public abstract class ExpandableTransformationBehavior extends ExpandableBehavior {
    private AnimatorSet currentAnimation;

    protected abstract AnimatorSet onCreateExpandedStateChangeAnimation(View view, View view2, boolean z, boolean z2);

    public ExpandableTransformationBehavior() {
    }

    public ExpandableTransformationBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean onExpandedStateChange(View dependency, View child, boolean expanded, boolean animated) {
        boolean currentlyAnimating = this.currentAnimation != null;
        if (currentlyAnimating) {
            this.currentAnimation.cancel();
        }
        this.currentAnimation = onCreateExpandedStateChangeAnimation(dependency, child, expanded, currentlyAnimating);
        this.currentAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ExpandableTransformationBehavior.this.currentAnimation = null;
            }
        });
        this.currentAnimation.start();
        if (!animated) {
            this.currentAnimation.end();
        }
        return true;
    }
}
