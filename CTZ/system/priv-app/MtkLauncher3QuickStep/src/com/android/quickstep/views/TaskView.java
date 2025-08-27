package com.android.quickstep.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Outline;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.android.launcher3.BaseActivity;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.R;
import com.android.quickstep.TaskSystemShortcut;
import com.android.quickstep.TaskUtils;
import com.android.quickstep.views.RecentsView;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class TaskView extends FrameLayout implements Task.TaskCallbacks, RecentsView.PageCallbacks {
    private static final long DIM_ANIM_DURATION = 700;
    private static final float EDGE_SCALE_DOWN_FACTOR = 0.03f;
    private static final float MAX_PAGE_SCRIM_ALPHA = 0.4f;
    public static final long SCALE_ICON_DURATION = 120;
    private float mCurveScale;
    private Animator mDimAlphaAnim;
    private IconView mIconView;
    private TaskThumbnailView mSnapshotView;
    private Task mTask;
    private float mZoomScale;
    private static final String TAG = TaskView.class.getSimpleName();
    private static final TimeInterpolator CURVE_INTERPOLATOR = new TimeInterpolator() { // from class: com.android.quickstep.views.-$$Lambda$TaskView$8AAwZvJUNMwmHQ9bssG34PGbAbA
        @Override // android.animation.TimeInterpolator
        public final float getInterpolation(float f) {
            return TaskView.lambda$static$0(f);
        }
    };
    public static final Property<TaskView, Float> ZOOM_SCALE = new FloatProperty<TaskView>("zoomScale") { // from class: com.android.quickstep.views.TaskView.1
        AnonymousClass1(String str) {
            super(str);
        }

        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
        @Override // android.util.FloatProperty
        public void setValue(TaskView taskView, float f) {
            taskView.setZoomScale(f);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(TaskView taskView) {
            return Float.valueOf(taskView.mZoomScale);
        }
    };

    static /* synthetic */ float lambda$static$0(float f) {
        return (((float) (-Math.cos(f * 3.141592653589793d))) / 2.0f) + 0.5f;
    }

    /* renamed from: com.android.quickstep.views.TaskView$1 */
    class AnonymousClass1 extends FloatProperty<TaskView> {
        AnonymousClass1(String str) {
            super(str);
        }

        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
        @Override // android.util.FloatProperty
        public void setValue(TaskView taskView, float f) {
            taskView.setZoomScale(f);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(TaskView taskView) {
            return Float.valueOf(taskView.mZoomScale);
        }
    }

    public TaskView(Context context) {
        this(context, null);
    }

    public TaskView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TaskView(final Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setOnClickListener(new View.OnClickListener() { // from class: com.android.quickstep.views.-$$Lambda$TaskView$QuGgavyMob9vJJgGm52abwDZX48
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                TaskView.lambda$new$1(this.f$0, context, view);
            }
        });
        setOutlineProvider(new TaskOutlineProvider(getResources()));
    }

    public static /* synthetic */ void lambda$new$1(TaskView taskView, Context context, View view) {
        if (taskView.getTask() == null) {
            return;
        }
        taskView.launchTask(true);
        BaseActivity.fromContext(context).getUserEventDispatcher().logTaskLaunchOrDismiss(0, 0, taskView.getRecentsView().indexOfChild(taskView), TaskUtils.getComponentKeyForTask(taskView.getTask().key));
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mSnapshotView = (TaskThumbnailView) findViewById(R.id.snapshot);
        this.mIconView = (IconView) findViewById(R.id.icon);
    }

    public void bind(Task task) {
        if (this.mTask != null) {
            this.mTask.removeCallback(this);
        }
        this.mTask = task;
        this.mSnapshotView.bind();
        task.addCallback(this);
        setContentDescription(task.titleDescription);
    }

    public Task getTask() {
        return this.mTask;
    }

    public TaskThumbnailView getThumbnail() {
        return this.mSnapshotView;
    }

    public IconView getIconView() {
        return this.mIconView;
    }

    public void launchTask(boolean z) {
        launchTask(z, new Consumer() { // from class: com.android.quickstep.views.-$$Lambda$TaskView$0UVTZJy5u1ZYBmK7ksAA-8MCLWE
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskView.lambda$launchTask$2(this.f$0, (Boolean) obj);
            }
        }, getHandler());
    }

    public static /* synthetic */ void lambda$launchTask$2(TaskView taskView, Boolean bool) {
        if (!bool.booleanValue()) {
            taskView.notifyTaskLaunchFailed(TAG);
        }
    }

    public void launchTask(boolean z, Consumer<Boolean> consumer, Handler handler) {
        ActivityOptions activityOptionsMakeCustomAnimation;
        if (this.mTask != null) {
            if (z) {
                activityOptionsMakeCustomAnimation = BaseDraggingActivity.fromContext(getContext()).getActivityLaunchOptions(this);
            } else {
                activityOptionsMakeCustomAnimation = ActivityOptions.makeCustomAnimation(getContext(), 0, 0);
            }
            ActivityManagerWrapper.getInstance().startActivityFromRecentsAsync(this.mTask.key, activityOptionsMakeCustomAnimation, consumer, handler);
        }
    }

    @Override // com.android.systemui.shared.recents.model.Task.TaskCallbacks
    public void onTaskDataLoaded(Task task, ThumbnailData thumbnailData) {
        this.mSnapshotView.setThumbnail(task, thumbnailData);
        this.mIconView.setDrawable(task.icon);
        this.mIconView.setOnClickListener(new View.OnClickListener() { // from class: com.android.quickstep.views.-$$Lambda$TaskView$5sxfOzE6B_OwPtWtJzye-XL5KiQ
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                TaskMenuView.showForTask(this.f$0);
            }
        });
        this.mIconView.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.android.quickstep.views.-$$Lambda$TaskView$LqQK-5y9I-rIFkI5jv25hj5g7ys
            @Override // android.view.View.OnLongClickListener
            public final boolean onLongClick(View view) {
                return TaskView.lambda$onTaskDataLoaded$4(this.f$0, view);
            }
        });
    }

    public static /* synthetic */ boolean lambda$onTaskDataLoaded$4(TaskView taskView, View view) {
        taskView.requestDisallowInterceptTouchEvent(true);
        return TaskMenuView.showForTask(taskView);
    }

    @Override // com.android.systemui.shared.recents.model.Task.TaskCallbacks
    public void onTaskDataUnloaded() {
        this.mSnapshotView.setThumbnail(null, null);
        this.mIconView.setDrawable(null);
        this.mIconView.setOnLongClickListener(null);
    }

    @Override // com.android.systemui.shared.recents.model.Task.TaskCallbacks
    public void onTaskWindowingModeChanged() {
    }

    public void animateIconToScaleAndDim(float f) {
        this.mIconView.animate().scaleX(f).scaleY(f).setDuration(120L).start();
        this.mDimAlphaAnim = ObjectAnimator.ofFloat(this.mSnapshotView, TaskThumbnailView.DIM_ALPHA_MULTIPLIER, 1.0f - f, f);
        this.mDimAlphaAnim.setDuration(DIM_ANIM_DURATION);
        this.mDimAlphaAnim.addListener(new AnimatorListenerAdapter() { // from class: com.android.quickstep.views.TaskView.2
            AnonymousClass2() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                TaskView.this.mDimAlphaAnim = null;
            }
        });
        this.mDimAlphaAnim.start();
    }

    /* renamed from: com.android.quickstep.views.TaskView$2 */
    class AnonymousClass2 extends AnimatorListenerAdapter {
        AnonymousClass2() {
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            TaskView.this.mDimAlphaAnim = null;
        }
    }

    protected void setIconScaleAndDim(float f) {
        this.mIconView.animate().cancel();
        this.mIconView.setScaleX(f);
        this.mIconView.setScaleY(f);
        if (this.mDimAlphaAnim != null) {
            this.mDimAlphaAnim.cancel();
        }
        this.mSnapshotView.setDimAlphaMultipler(f);
    }

    public void resetVisualProperties() {
        setZoomScale(1.0f);
        setTranslationX(0.0f);
        setTranslationY(0.0f);
        setTranslationZ(0.0f);
        setAlpha(1.0f);
        setIconScaleAndDim(1.0f);
    }

    @Override // com.android.quickstep.views.RecentsView.PageCallbacks
    public void onPageScroll(RecentsView.ScrollState scrollState) {
        float interpolation = CURVE_INTERPOLATOR.getInterpolation(scrollState.linearInterpolation);
        this.mSnapshotView.setDimAlpha(0.4f * interpolation);
        setCurveScale(getCurveScaleForCurveInterpolation(interpolation));
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        setPivotX((i3 - i) * 0.5f);
        setPivotY(this.mSnapshotView.getTop() + (this.mSnapshotView.getHeight() * 0.5f));
    }

    public static float getCurveScaleForInterpolation(float f) {
        return getCurveScaleForCurveInterpolation(CURVE_INTERPOLATOR.getInterpolation(f));
    }

    private static float getCurveScaleForCurveInterpolation(float f) {
        return 1.0f - (f * EDGE_SCALE_DOWN_FACTOR);
    }

    private void setCurveScale(float f) {
        this.mCurveScale = f;
        onScaleChanged();
    }

    public float getCurveScale() {
        return this.mCurveScale;
    }

    public void setZoomScale(float f) {
        this.mZoomScale = f;
        onScaleChanged();
    }

    private void onScaleChanged() {
        float f = this.mCurveScale * this.mZoomScale;
        setScaleX(f);
        setScaleY(f);
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }

    private static final class TaskOutlineProvider extends ViewOutlineProvider {
        private final int mMarginTop;
        private final float mRadius;

        TaskOutlineProvider(Resources resources) {
            this.mMarginTop = resources.getDimensionPixelSize(R.dimen.task_thumbnail_top_margin);
            this.mRadius = resources.getDimension(R.dimen.task_corner_radius);
        }

        @Override // android.view.ViewOutlineProvider
        public void getOutline(View view, Outline outline) {
            outline.setRoundRect(0, this.mMarginTop, view.getWidth(), view.getHeight(), this.mRadius);
        }
    }

    @Override // android.view.View
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.string.accessibility_close_task, getContext().getText(R.string.accessibility_close_task)));
        Context context = getContext();
        BaseDraggingActivity baseDraggingActivityFromContext = BaseDraggingActivity.fromContext(context);
        for (TaskSystemShortcut taskSystemShortcut : TaskMenuView.MENU_OPTIONS) {
            if (taskSystemShortcut.getOnClickListener(baseDraggingActivityFromContext, this) != null) {
                accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(taskSystemShortcut.labelResId, context.getText(taskSystemShortcut.labelResId)));
            }
        }
        RecentsView recentsView = getRecentsView();
        accessibilityNodeInfo.setCollectionItemInfo(AccessibilityNodeInfo.CollectionItemInfo.obtain(0, 1, (recentsView.getChildCount() - recentsView.indexOfChild(this)) - 1, 1, false));
    }

    @Override // android.view.View
    public boolean performAccessibilityAction(int i, Bundle bundle) {
        if (i == R.string.accessibility_close_task) {
            getRecentsView().dismissTask(this, true, true);
            return true;
        }
        for (TaskSystemShortcut taskSystemShortcut : TaskMenuView.MENU_OPTIONS) {
            if (i == taskSystemShortcut.labelResId) {
                View.OnClickListener onClickListener = taskSystemShortcut.getOnClickListener(BaseDraggingActivity.fromContext(getContext()), this);
                if (onClickListener != null) {
                    onClickListener.onClick(this);
                }
                return true;
            }
        }
        if (getRecentsView().performTaskAccessibilityActionExtra(i)) {
            return true;
        }
        return super.performAccessibilityAction(i, bundle);
    }

    private RecentsView getRecentsView() {
        return (RecentsView) getParent();
    }

    public void notifyTaskLaunchFailed(String str) {
        String str2 = "Failed to launch task";
        if (this.mTask != null) {
            str2 = "Failed to launch task (task=" + this.mTask.key.baseIntent + " userId=" + this.mTask.key.userId + ")";
        }
        Log.w(str, str2);
        Toast.makeText(getContext(), R.string.activity_not_available, 0).show();
    }
}
