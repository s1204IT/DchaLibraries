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

/* loaded from: classes.dex */
public class TaskViewDrawable extends Drawable {
    private static final float ICON_SCALE_THRESHOLD = 0.95f;
    public static final FloatProperty<TaskViewDrawable> PROGRESS = new FloatProperty<TaskViewDrawable>(NotificationCompat.CATEGORY_PROGRESS) { // from class: com.android.quickstep.util.TaskViewDrawable.1
        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
        @Override // android.util.FloatProperty
        public void setValue(TaskViewDrawable taskViewDrawable, float f) {
            taskViewDrawable.setProgress(f);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
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
        this.mIconScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.quickstep.util.-$$Lambda$TaskViewDrawable$sMQSQvdEGrI6FAAV7-MsSaduq1w
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                TaskViewDrawable.lambda$animateIconScale$0(this.f$0, valueAnimator);
            }
        });
        this.mIconScaleAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.quickstep.util.TaskViewDrawable.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                TaskViewDrawable.this.mIconScaleAnimator = null;
            }
        });
        this.mIconScaleAnimator.setDuration(120L);
        this.mIconScaleAnimator.start();
    }

    public static /* synthetic */ void lambda$animateIconScale$0(TaskViewDrawable taskViewDrawable, ValueAnimator valueAnimator) {
        taskViewDrawable.mIconScale = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        if (taskViewDrawable.mProgress > ICON_SCALE_THRESHOLD) {
            float f = (taskViewDrawable.mProgress - ICON_SCALE_THRESHOLD) / 0.050000012f;
            if (f > taskViewDrawable.mIconScale) {
                taskViewDrawable.mIconScale = f;
            }
        }
        taskViewDrawable.invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
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

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return -3;
    }
}
