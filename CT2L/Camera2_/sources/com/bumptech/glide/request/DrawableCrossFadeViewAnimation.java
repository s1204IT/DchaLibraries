package com.bumptech.glide.request;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.bumptech.glide.request.target.Target;

public class DrawableCrossFadeViewAnimation implements GlideAnimation<Drawable> {
    public static final int DEFAULT_DURATION = 300;
    private Animation defaultAnimation;
    private int duration;

    private static Animation getDefaultAnimation() {
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(150L);
        return animation;
    }

    public static class DrawableCrossFadeFactory implements GlideAnimationFactory<Drawable> {
        private DrawableCrossFadeViewAnimation animation;
        private Context context;
        private Animation defaultAnimation;
        private int defaultAnimationId;
        private int duration;

        public DrawableCrossFadeFactory() {
            this(DrawableCrossFadeViewAnimation.getDefaultAnimation(), 300);
        }

        public DrawableCrossFadeFactory(int duration) {
            this(DrawableCrossFadeViewAnimation.getDefaultAnimation(), duration);
        }

        public DrawableCrossFadeFactory(Context context, int defaultAnimationId, int duration) {
            this.context = context;
            this.defaultAnimationId = defaultAnimationId;
            this.duration = duration;
        }

        public DrawableCrossFadeFactory(Animation defaultAnimation, int duration) {
            this.defaultAnimation = defaultAnimation;
            this.duration = duration;
        }

        @Override
        public GlideAnimation<Drawable> build(boolean isFromMemoryCache, boolean isFirstImage) {
            if (isFromMemoryCache) {
                return NoAnimation.get();
            }
            if (this.animation == null) {
                if (this.defaultAnimation == null) {
                    this.defaultAnimation = AnimationUtils.loadAnimation(this.context, this.defaultAnimationId);
                }
                this.animation = new DrawableCrossFadeViewAnimation(this.defaultAnimation, this.duration);
            }
            return this.animation;
        }
    }

    public DrawableCrossFadeViewAnimation(Animation defaultAnimation, int duration) {
        this.defaultAnimation = defaultAnimation;
        this.duration = duration;
    }

    @Override
    public boolean animate(Drawable previous, Drawable current, View view, Target<Drawable> target) {
        if (previous != null) {
            TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{previous, current});
            transitionDrawable.setCrossFadeEnabled(true);
            transitionDrawable.startTransition(this.duration);
            GlideAnimation<Drawable> none = NoAnimation.get();
            target.onResourceReady(transitionDrawable, none);
            return true;
        }
        view.startAnimation(this.defaultAnimation);
        return false;
    }
}
