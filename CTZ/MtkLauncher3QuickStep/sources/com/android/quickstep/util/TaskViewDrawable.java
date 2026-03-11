package com.android.quickstep.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.app.NotificationCompat;
import android.util.FloatProperty;
import android.view.View;
import com.android.launcher3.Utilities;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskThumbnailView;
import com.android.quickstep.views.TaskView;

public class TaskViewDrawable extends Drawable {
    private static final float ICON_SCALE_THRESHOLD = 0.95f;
    public static final FloatProperty<TaskViewDrawable> PROGRESS = new FloatProperty<TaskViewDrawable>(NotificationCompat.CATEGORY_PROGRESS) {
        @Override
        public void setValue(TaskViewDrawable taskViewDrawable, float f) {
            taskViewDrawable.setProgress(f);
        }

        @Override
        public Float get(TaskViewDrawable taskViewDrawable) {
            return Float.valueOf(taskViewDrawable.mProgress);
        }
    };
    private final ClipAnimationHelper mClipAnimationHelper;
    private float mIconScale;
    private ValueAnimator mIconScaleAnimator;
    private final View mIconView;
    private final RecentsView mParent;
    private boolean mPassedIconScaleThreshold;
    private final TaskThumbnailView mThumbnailView;
    private float mProgress = 1.0f;
    private final int[] mIconPos = new int[2];

    public TaskViewDrawable(TaskView taskView, RecentsView recentsView) {
        this.mParent = recentsView;
        this.mIconView = taskView.getIconView();
        this.mIconScale = this.mIconView.getScaleX();
        Utilities.getDescendantCoordRelativeToAncestor(this.mIconView, recentsView, this.mIconPos, true);
        this.mThumbnailView = taskView.getThumbnail();
        this.mClipAnimationHelper = new ClipAnimationHelper();
        this.mClipAnimationHelper.fromTaskThumbnailView(this.mThumbnailView, recentsView);
    }

    public void setProgress(float f) {
        this.mProgress = f;
        this.mParent.invalidate();
        boolean z = f <= ICON_SCALE_THRESHOLD;
        if (this.mPassedIconScaleThreshold != z) {
            this.mPassedIconScaleThreshold = z;
            animateIconScale(this.mPassedIconScaleThreshold ? 0.0f : 1.0f);
        }
    }

    private void animateIconScale(float f) {
        if (this.mIconScaleAnimator != null) {
            this.mIconScaleAnimator.cancel();
        }
        this.mIconScaleAnimator = ValueAnimator.ofFloat(this.mIconScale, f);
        this.mIconScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                TaskViewDrawable.lambda$animateIconScale$0(this.f$0, valueAnimator);
            }
        });
        this.mIconScaleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                TaskViewDrawable.this.mIconScaleAnimator = null;
            }
        });
        this.mIconScaleAnimator.setDuration(120L);
        this.mIconScaleAnimator.start();
    }

    public static void lambda$animateIconScale$0(TaskViewDrawable taskViewDrawable, ValueAnimator valueAnimator) {
        taskViewDrawable.mIconScale = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        if (taskViewDrawable.mProgress > ICON_SCALE_THRESHOLD) {
            float f = (taskViewDrawable.mProgress - ICON_SCALE_THRESHOLD) / 0.050000012f;
            if (f > taskViewDrawable.mIconScale) {
                taskViewDrawable.mIconScale = f;
            }
        }
        taskViewDrawable.invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        canvas.translate(this.mParent.getScrollX(), this.mParent.getScrollY());
        this.mClipAnimationHelper.drawForProgress(this.mThumbnailView, canvas, this.mProgress);
        canvas.restore();
        canvas.save();
        canvas.translate(this.mIconPos[0], this.mIconPos[1]);
        canvas.scale(this.mIconScale, this.mIconScale, this.mIconView.getWidth() / 2, this.mIconView.getHeight() / 2);
        this.mIconView.draw(canvas);
        canvas.restore();
    }

    public ClipAnimationHelper getClipAnimationHelper() {
        return this.mClipAnimationHelper;
    }

    @Override
    public void setAlpha(int i) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return -3;
    }
}
