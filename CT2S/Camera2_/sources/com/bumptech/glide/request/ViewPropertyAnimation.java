package com.bumptech.glide.request;

import android.graphics.drawable.Drawable;
import android.view.View;
import com.bumptech.glide.request.target.Target;

public class ViewPropertyAnimation implements GlideAnimation {
    private Animator animator;

    public interface Animator {
        void animate(View view);
    }

    public static class ViewPropertyAnimationFactory implements GlideAnimationFactory {
        private ViewPropertyAnimation animation;
        private Animator animator;

        public ViewPropertyAnimationFactory(Animator animator) {
            this.animator = animator;
        }

        @Override
        public GlideAnimation build(boolean isFromMemoryCache, boolean isFirstImage) {
            if (isFromMemoryCache || !isFirstImage) {
                return NoAnimation.get();
            }
            if (this.animation == null) {
                this.animation = new ViewPropertyAnimation(this.animator);
            }
            return this.animation;
        }
    }

    public ViewPropertyAnimation(Animator animator) {
        this.animator = animator;
    }

    @Override
    public boolean animate(Drawable previous, Object current, View view, Target target) {
        this.animator.animate(view);
        return false;
    }
}
