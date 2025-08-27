package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.component.SetWaitingForTransitionStartEvent;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.TaskStack;
import com.android.systemui.shared.recents.utilities.AnimationProps;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class TaskStackAnimationHelper {
    private final int mEnterAndExitFromHomeTranslationOffset;
    private TaskStackView mStackView;
    private static final Interpolator ENTER_FROM_HOME_ALPHA_INTERPOLATOR = Interpolators.LINEAR;
    private static final Interpolator EXIT_TO_HOME_TRANSLATION_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f);
    private static final Interpolator DISMISS_ALL_TRANSLATION_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
    private static final Interpolator FOCUS_NEXT_TASK_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 0.0f, 1.0f);
    private static final Interpolator FOCUS_IN_FRONT_NEXT_TASK_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.0f, 1.0f);
    private static final Interpolator FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR = Interpolators.LINEAR_OUT_SLOW_IN;
    private static final Interpolator ENTER_WHILE_DOCKING_INTERPOLATOR = Interpolators.LINEAR_OUT_SLOW_IN;
    private TaskViewTransform mTmpTransform = new TaskViewTransform();
    private ArrayList<TaskViewTransform> mTmpCurrentTaskTransforms = new ArrayList<>();
    private ArrayList<TaskViewTransform> mTmpFinalTaskTransforms = new ArrayList<>();

    public TaskStackAnimationHelper(Context context, TaskStackView taskStackView) {
        this.mStackView = taskStackView;
        this.mEnterAndExitFromHomeTranslationOffset = Recents.getConfiguration().isGridEnabled ? 0 : 33;
    }

    public void prepareForEnterAnimation() {
        boolean z;
        float f;
        int i;
        int i2;
        float f2;
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        Resources resources = this.mStackView.getResources();
        Resources resources2 = this.mStackView.getContext().getApplicationContext().getResources();
        TaskStackLayoutAlgorithm stackAlgorithm = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller scroller = this.mStackView.getScroller();
        TaskStack stack = this.mStackView.getStack();
        Task launchTarget = stack.getLaunchTarget();
        if (stack.getTaskCount() == 0) {
            return;
        }
        int iHeight = stackAlgorithm.mStackRect.height();
        resources.getDimensionPixelSize(R.dimen.recents_task_stack_animation_affiliate_enter_offset);
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.recents_task_stack_animation_launched_while_docking_offset);
        if (resources2.getConfiguration().orientation != 2) {
            z = false;
        } else {
            z = true;
        }
        boolean z2 = Recents.getConfiguration().isLowRamDevice;
        TaskViewTransform taskViewTransform = null;
        if (z2 && launchState.launchedFromApp && !launchState.launchedViaDockGesture) {
            stackAlgorithm.getStackTransform(launchTarget, scroller.getStackScroll(), this.mTmpTransform, null);
            f = this.mTmpTransform.rect.top;
        } else {
            f = 0.0f;
        }
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int size = taskViews.size() - 1;
        while (size >= 0) {
            TaskView taskView = taskViews.get(size);
            Task task = taskView.getTask();
            stackAlgorithm.getStackTransform(task, scroller.getStackScroll(), this.mTmpTransform, taskViewTransform);
            if (launchState.launchedFromApp && !launchState.launchedViaDockGesture) {
                if (task.isLaunchTarget) {
                    taskView.onPrepareLaunchTargetForEnterAnimation();
                } else if (z2 && size >= taskViews.size() - 10) {
                    stackAlgorithm.getStackTransform(task, scroller.getStackScroll(), this.mTmpTransform, taskViewTransform);
                    this.mTmpTransform.rect.offset(0.0f, -f);
                    this.mTmpTransform.alpha = 0.0f;
                    this.mStackView.updateTaskViewToTransform(taskView, this.mTmpTransform, AnimationProps.IMMEDIATE);
                    stackAlgorithm.getStackTransform(task, scroller.getStackScroll(), this.mTmpTransform, null);
                    this.mTmpTransform.alpha = 1.0f;
                    i = dimensionPixelSize;
                    this.mStackView.updateTaskViewToTransform(taskView, this.mTmpTransform, new AnimationProps(336, Interpolators.FAST_OUT_SLOW_IN));
                }
                i = dimensionPixelSize;
            } else {
                i = dimensionPixelSize;
                if (launchState.launchedFromHome) {
                    if (z2) {
                        f2 = 0.0f;
                        this.mTmpTransform.rect.offset(0.0f, stackAlgorithm.getTaskRect().height() / 4);
                    } else {
                        f2 = 0.0f;
                        this.mTmpTransform.rect.offset(0.0f, iHeight);
                    }
                    this.mTmpTransform.alpha = f2;
                    this.mStackView.updateTaskViewToTransform(taskView, this.mTmpTransform, AnimationProps.IMMEDIATE);
                } else {
                    if (launchState.launchedViaDockGesture) {
                        if (!z) {
                            i2 = (int) (iHeight * 0.9f);
                        } else {
                            i2 = i;
                        }
                        this.mTmpTransform.rect.offset(0.0f, i2);
                        this.mTmpTransform.alpha = 0.0f;
                        this.mStackView.updateTaskViewToTransform(taskView, this.mTmpTransform, AnimationProps.IMMEDIATE);
                    }
                    size--;
                    dimensionPixelSize = i;
                    taskViewTransform = null;
                }
            }
            size--;
            dimensionPixelSize = i;
            taskViewTransform = null;
        }
    }

    public void startEnterAnimation(ReferenceCountedTrigger referenceCountedTrigger) {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        Resources resources = this.mStackView.getResources();
        Resources resources2 = this.mStackView.getContext().getApplicationContext().getResources();
        TaskStackLayoutAlgorithm stackAlgorithm = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller scroller = this.mStackView.getScroller();
        TaskStack stack = this.mStackView.getStack();
        stack.getLaunchTarget();
        if (stack.getTaskCount() == 0) {
            return;
        }
        boolean z = Recents.getConfiguration().isLowRamDevice;
        int integer = resources.getInteger(R.integer.recents_task_enter_from_app_duration);
        resources.getInteger(R.integer.recents_task_enter_from_affiliated_app_duration);
        int integer2 = resources2.getInteger(R.integer.long_press_dock_anim_duration);
        if (launchState.launchedFromApp && !launchState.launchedViaDockGesture && z) {
            referenceCountedTrigger.addLastDecrementRunnable(new Runnable() { // from class: com.android.systemui.recents.views.-$$Lambda$TaskStackAnimationHelper$Z1ye5IT0uybrzSDdzPdGEoeTWaY
                @Override // java.lang.Runnable
                public final void run() {
                    EventBus.getDefault().send(new SetWaitingForTransitionStartEvent(false));
                }
            });
        }
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int size = taskViews.size();
        int i = size - 1;
        int i2 = i;
        while (i2 >= 0) {
            int i3 = (size - i2) - 1;
            TaskView taskView = taskViews.get(i2);
            Task task = taskView.getTask();
            List<TaskView> list = taskViews;
            TaskStackViewScroller taskStackViewScroller = scroller;
            stackAlgorithm.getStackTransform(task, scroller.getStackScroll(), this.mTmpTransform, null);
            if (launchState.launchedFromApp && !launchState.launchedViaDockGesture) {
                if (task.isLaunchTarget) {
                    taskView.onStartLaunchTargetEnterAnimation(this.mTmpTransform, integer, this.mStackView.mScreenPinningEnabled, referenceCountedTrigger);
                }
            } else if (launchState.launchedFromHome) {
                float fMin = (Math.min(5, i3) * this.mEnterAndExitFromHomeTranslationOffset) / 300.0f;
                AnimationProps listener = new AnimationProps().setInterpolator(4, ENTER_FROM_HOME_ALPHA_INTERPOLATOR).setListener(referenceCountedTrigger.decrementOnAnimationEnd());
                if (z) {
                    listener.setInterpolator(6, Interpolators.FAST_OUT_SLOW_IN).setDuration(6, 150).setDuration(4, 150);
                } else {
                    listener.setStartDelay(4, Math.min(5, i3) * 16).setInterpolator(6, new RecentsEntrancePathInterpolator(0.0f, 0.0f, 0.2f, 1.0f, fMin)).setDuration(6, 300).setDuration(4, 100);
                }
                referenceCountedTrigger.increment();
                this.mStackView.updateTaskViewToTransform(taskView, this.mTmpTransform, listener);
                if (i2 == i) {
                    taskView.onStartFrontTaskEnterAnimation(this.mStackView.mScreenPinningEnabled);
                }
            } else if (launchState.launchedViaDockGesture) {
                AnimationProps listener2 = new AnimationProps().setDuration(6, (i2 * 33) + integer2).setInterpolator(6, ENTER_WHILE_DOCKING_INTERPOLATOR).setStartDelay(6, 48).setListener(referenceCountedTrigger.decrementOnAnimationEnd());
                referenceCountedTrigger.increment();
                this.mStackView.updateTaskViewToTransform(taskView, this.mTmpTransform, listener2);
            }
            i2--;
            taskViews = list;
            scroller = taskStackViewScroller;
        }
    }

    public void startExitToHomeAnimation(boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        AnimationProps listener;
        TaskStackLayoutAlgorithm stackAlgorithm = this.mStackView.getStackAlgorithm();
        if (this.mStackView.getStack().getTaskCount() == 0) {
            return;
        }
        int iHeight = stackAlgorithm.mStackRect.height();
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            int i2 = (size - i) - 1;
            TaskView taskView = taskViews.get(i);
            if (!this.mStackView.isIgnoredTask(taskView.getTask())) {
                if (z) {
                    int iMin = Math.min(5, i2) * this.mEnterAndExitFromHomeTranslationOffset;
                    listener = new AnimationProps().setDuration(6, 200).setListener(referenceCountedTrigger.decrementOnAnimationEnd());
                    if (Recents.getConfiguration().isLowRamDevice) {
                        listener.setInterpolator(6, Interpolators.FAST_OUT_SLOW_IN);
                    } else {
                        listener.setStartDelay(6, iMin).setInterpolator(6, EXIT_TO_HOME_TRANSLATION_INTERPOLATOR);
                    }
                    referenceCountedTrigger.increment();
                } else {
                    listener = AnimationProps.IMMEDIATE;
                }
                this.mTmpTransform.fillIn(taskView);
                if (Recents.getConfiguration().isLowRamDevice) {
                    listener.setInterpolator(4, EXIT_TO_HOME_TRANSLATION_INTERPOLATOR).setDuration(4, 200);
                    this.mTmpTransform.rect.offset(0.0f, stackAlgorithm.mTaskStackLowRamLayoutAlgorithm.getTaskRect().height() / 4);
                    this.mTmpTransform.alpha = 0.0f;
                } else {
                    this.mTmpTransform.rect.offset(0.0f, iHeight);
                }
                this.mStackView.updateTaskViewToTransform(taskView, this.mTmpTransform, listener);
            }
        }
    }

    public void startLaunchTaskAnimation(TaskView taskView, boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        Resources resources = this.mStackView.getResources();
        int integer = resources.getInteger(R.integer.recents_task_exit_to_app_duration);
        resources.getDimensionPixelSize(R.dimen.recents_task_stack_animation_affiliate_enter_offset);
        taskView.getTask();
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            final TaskView taskView2 = taskViews.get(i);
            taskView2.getTask();
            if (taskView2 == taskView) {
                taskView2.setClipViewInStack(false);
                referenceCountedTrigger.addLastDecrementRunnable(new Runnable() { // from class: com.android.systemui.recents.views.TaskStackAnimationHelper.1
                    @Override // java.lang.Runnable
                    public void run() {
                        taskView2.setClipViewInStack(true);
                    }
                });
                taskView2.onStartLaunchTargetLaunchAnimation(integer, z, referenceCountedTrigger);
            }
        }
    }

    public void startDeleteTaskAnimation(TaskView taskView, boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        if (z) {
            startTaskGridDeleteTaskAnimation(taskView, referenceCountedTrigger);
        } else {
            startTaskStackDeleteTaskAnimation(taskView, referenceCountedTrigger);
        }
    }

    public void startDeleteAllTasksAnimation(List<TaskView> list, boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        if (z) {
            for (int i = 0; i < list.size(); i++) {
                startTaskGridDeleteTaskAnimation(list.get(i), referenceCountedTrigger);
            }
            return;
        }
        startTaskStackDeleteAllTasksAnimation(list, referenceCountedTrigger);
    }

    public boolean startScrollToFocusedTaskAnimation(Task task, boolean z) {
        int iMax;
        Interpolator interpolator;
        TaskStackLayoutAlgorithm stackAlgorithm = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller scroller = this.mStackView.getScroller();
        TaskStack stack = this.mStackView.getStack();
        float stackScroll = scroller.getStackScroll();
        final float boundedStackScroll = scroller.getBoundedStackScroll(stackAlgorithm.getStackScrollForTask(task));
        boolean z2 = boundedStackScroll > stackScroll;
        boolean z3 = Float.compare(boundedStackScroll, stackScroll) != 0;
        int size = this.mStackView.getTaskViews().size();
        ArrayList<Task> tasks = stack.getTasks();
        this.mStackView.getCurrentTaskTransforms(tasks, this.mTmpCurrentTaskTransforms);
        this.mStackView.bindVisibleTaskViews(boundedStackScroll);
        stackAlgorithm.setFocusState(1);
        scroller.setStackScroll(boundedStackScroll, null);
        this.mStackView.cancelDeferredTaskViewLayoutAnimation();
        this.mStackView.getLayoutTaskTransforms(boundedStackScroll, stackAlgorithm.getFocusState(), tasks, true, this.mTmpFinalTaskTransforms);
        TaskView childViewForTask = this.mStackView.getChildViewForTask(task);
        if (childViewForTask == null) {
            Log.e("TaskStackAnimationHelper", "b/27389156 null-task-view prebind:" + size + " postbind:" + this.mStackView.getTaskViews().size() + " prescroll:" + stackScroll + " postscroll: " + boundedStackScroll);
            return false;
        }
        childViewForTask.setFocusedState(true, z);
        ReferenceCountedTrigger referenceCountedTrigger = new ReferenceCountedTrigger();
        referenceCountedTrigger.addLastDecrementRunnable(new Runnable() { // from class: com.android.systemui.recents.views.TaskStackAnimationHelper.2
            @Override // java.lang.Runnable
            public void run() {
                TaskStackAnimationHelper.this.mStackView.bindVisibleTaskViews(boundedStackScroll);
            }
        });
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int size2 = taskViews.size();
        int iIndexOf = taskViews.indexOf(childViewForTask);
        for (int i = 0; i < size2; i++) {
            TaskView taskView = taskViews.get(i);
            Task task2 = taskView.getTask();
            if (!this.mStackView.isIgnoredTask(task2)) {
                int iIndexOf2 = tasks.indexOf(task2);
                TaskViewTransform taskViewTransform = this.mTmpCurrentTaskTransforms.get(iIndexOf2);
                TaskViewTransform taskViewTransform2 = this.mTmpFinalTaskTransforms.get(iIndexOf2);
                this.mStackView.updateTaskViewToTransform(taskView, taskViewTransform, AnimationProps.IMMEDIATE);
                if (z2) {
                    iMax = calculateStaggeredAnimDuration(i);
                    interpolator = FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR;
                } else if (i < iIndexOf) {
                    iMax = 150 + (((iIndexOf - i) - 1) * 50);
                    interpolator = FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR;
                } else if (i > iIndexOf) {
                    iMax = Math.max(100, 150 - (((i - iIndexOf) - 1) * 50));
                    interpolator = FOCUS_IN_FRONT_NEXT_TASK_INTERPOLATOR;
                } else {
                    iMax = 200;
                    interpolator = FOCUS_NEXT_TASK_INTERPOLATOR;
                }
                AnimationProps listener = new AnimationProps().setDuration(6, iMax).setInterpolator(6, interpolator).setListener(referenceCountedTrigger.decrementOnAnimationEnd());
                referenceCountedTrigger.increment();
                this.mStackView.updateTaskViewToTransform(taskView, taskViewTransform2, listener);
            }
        }
        return z3;
    }

    public void startNewStackScrollAnimation(TaskStack taskStack, ReferenceCountedTrigger referenceCountedTrigger) {
        TaskStackLayoutAlgorithm stackAlgorithm = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller scroller = this.mStackView.getScroller();
        ArrayList<Task> tasks = taskStack.getTasks();
        this.mStackView.getCurrentTaskTransforms(tasks, this.mTmpCurrentTaskTransforms);
        this.mStackView.setTasks(taskStack, false);
        this.mStackView.updateLayoutAlgorithm(false);
        final float f = stackAlgorithm.mInitialScrollP;
        this.mStackView.bindVisibleTaskViews(f);
        stackAlgorithm.setFocusState(0);
        stackAlgorithm.setTaskOverridesForInitialState(taskStack, true);
        scroller.setStackScroll(f);
        this.mStackView.cancelDeferredTaskViewLayoutAnimation();
        this.mStackView.getLayoutTaskTransforms(f, stackAlgorithm.getFocusState(), tasks, false, this.mTmpFinalTaskTransforms);
        Task frontMostTask = taskStack.getFrontMostTask();
        final TaskView childViewForTask = this.mStackView.getChildViewForTask(frontMostTask);
        final TaskViewTransform taskViewTransform = this.mTmpFinalTaskTransforms.get(tasks.indexOf(frontMostTask));
        if (childViewForTask != null) {
            this.mStackView.updateTaskViewToTransform(childViewForTask, stackAlgorithm.getFrontOfStackTransform(), AnimationProps.IMMEDIATE);
        }
        referenceCountedTrigger.addLastDecrementRunnable(new Runnable() { // from class: com.android.systemui.recents.views.TaskStackAnimationHelper.3
            @Override // java.lang.Runnable
            public void run() {
                TaskStackAnimationHelper.this.mStackView.bindVisibleTaskViews(f);
                if (childViewForTask != null) {
                    TaskStackAnimationHelper.this.mStackView.updateTaskViewToTransform(childViewForTask, taskViewTransform, new AnimationProps(75, 250, TaskStackAnimationHelper.FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR));
                }
            }
        });
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            TaskView taskView = taskViews.get(i);
            Task task = taskView.getTask();
            if (!this.mStackView.isIgnoredTask(task) && (task != frontMostTask || childViewForTask == null)) {
                int iIndexOf = tasks.indexOf(task);
                if (iIndexOf == -1) {
                    Log.w("TaskStackAnimationHelper", "startNewStackScrollAnimation() task index = -1");
                } else {
                    TaskViewTransform taskViewTransform2 = this.mTmpCurrentTaskTransforms.get(iIndexOf);
                    TaskViewTransform taskViewTransform3 = this.mTmpFinalTaskTransforms.get(iIndexOf);
                    this.mStackView.updateTaskViewToTransform(taskView, taskViewTransform2, AnimationProps.IMMEDIATE);
                    int iCalculateStaggeredAnimDuration = calculateStaggeredAnimDuration(i);
                    AnimationProps listener = new AnimationProps().setDuration(6, iCalculateStaggeredAnimDuration).setInterpolator(6, FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR).setListener(referenceCountedTrigger.decrementOnAnimationEnd());
                    referenceCountedTrigger.increment();
                    this.mStackView.updateTaskViewToTransform(taskView, taskViewTransform3, listener);
                }
            }
        }
    }

    private int calculateStaggeredAnimDuration(int i) {
        return Math.max(100, ((i - 1) * 50) + 100);
    }

    private void startTaskGridDeleteTaskAnimation(final TaskView taskView, final ReferenceCountedTrigger referenceCountedTrigger) {
        referenceCountedTrigger.increment();
        referenceCountedTrigger.addLastDecrementRunnable(new Runnable() { // from class: com.android.systemui.recents.views.-$$Lambda$TaskStackAnimationHelper$8-n9X8WiqU8WjSQafJipbVZD-LA
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.mStackView.getTouchHandler().onChildDismissed(taskView);
            }
        });
        taskView.animate().setDuration(300L).scaleX(0.9f).scaleY(0.9f).alpha(0.0f).setListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.recents.views.TaskStackAnimationHelper.4
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                referenceCountedTrigger.decrement();
            }
        }).start();
    }

    private void startTaskStackDeleteTaskAnimation(final TaskView taskView, final ReferenceCountedTrigger referenceCountedTrigger) {
        final TaskStackViewTouchHandler touchHandler = this.mStackView.getTouchHandler();
        touchHandler.onBeginManualDrag(taskView);
        referenceCountedTrigger.increment();
        referenceCountedTrigger.addLastDecrementRunnable(new Runnable() { // from class: com.android.systemui.recents.views.-$$Lambda$TaskStackAnimationHelper$ax6dOg8GHbAwig9kBnwP5_DTcLA
            @Override // java.lang.Runnable
            public final void run() {
                touchHandler.onChildDismissed(taskView);
            }
        });
        final float scaledDismissSize = touchHandler.getScaledDismissSize();
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.setDuration(400L);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.recents.views.-$$Lambda$TaskStackAnimationHelper$DBVHlVbyKhFHpm00avfl8nT1DCw
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                TaskStackAnimationHelper.lambda$startTaskStackDeleteTaskAnimation$3(taskView, scaledDismissSize, touchHandler, valueAnimator);
            }
        });
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.recents.views.TaskStackAnimationHelper.5
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                referenceCountedTrigger.decrement();
            }
        });
        valueAnimatorOfFloat.start();
    }

    static /* synthetic */ void lambda$startTaskStackDeleteTaskAnimation$3(TaskView taskView, float f, TaskStackViewTouchHandler taskStackViewTouchHandler, ValueAnimator valueAnimator) {
        float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        taskView.setTranslationX(f * fFloatValue);
        taskStackViewTouchHandler.updateSwipeProgress(taskView, true, fFloatValue);
    }

    private void startTaskStackDeleteAllTasksAnimation(List<TaskView> list, final ReferenceCountedTrigger referenceCountedTrigger) {
        int measuredWidth = this.mStackView.getMeasuredWidth() - this.mStackView.getStackAlgorithm().getTaskRect().left;
        for (int size = list.size() - 1; size >= 0; size--) {
            final TaskView taskView = list.get(size);
            taskView.setClipViewInStack(false);
            AnimationProps animationProps = new AnimationProps(((r0 - size) - 1) * 33, 200, DISMISS_ALL_TRANSLATION_INTERPOLATOR, new AnimatorListenerAdapter() { // from class: com.android.systemui.recents.views.TaskStackAnimationHelper.6
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    referenceCountedTrigger.decrement();
                    taskView.setClipViewInStack(true);
                }
            });
            referenceCountedTrigger.increment();
            this.mTmpTransform.fillIn(taskView);
            this.mTmpTransform.rect.offset(measuredWidth, 0.0f);
            this.mStackView.updateTaskViewToTransform(taskView, this.mTmpTransform, animationProps);
        }
    }
}
