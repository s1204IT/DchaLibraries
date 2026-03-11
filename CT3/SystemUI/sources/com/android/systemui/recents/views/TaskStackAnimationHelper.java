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
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import java.util.ArrayList;
import java.util.List;

public class TaskStackAnimationHelper {
    private TaskStackView mStackView;
    private static final Interpolator ENTER_FROM_HOME_TRANSLATION_INTERPOLATOR = Interpolators.LINEAR_OUT_SLOW_IN;
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

    public TaskStackAnimationHelper(Context context, TaskStackView stackView) {
        this.mStackView = stackView;
    }

    public void prepareForEnterAnimation() {
        boolean zIsTaskAboveTask;
        boolean zIsFreeformTask;
        int offset;
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();
        Resources res = this.mStackView.getResources();
        Resources appResources = this.mStackView.getContext().getApplicationContext().getResources();
        TaskStackLayoutAlgorithm stackLayout = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller stackScroller = this.mStackView.getScroller();
        TaskStack stack = this.mStackView.getStack();
        Task launchTargetTask = stack.getLaunchTarget();
        if (stack.getTaskCount() == 0) {
            return;
        }
        int offscreenYOffset = stackLayout.mStackRect.height();
        int taskViewAffiliateGroupEnterOffset = res.getDimensionPixelSize(R.dimen.recents_task_stack_animation_affiliate_enter_offset);
        int launchedWhileDockingOffset = res.getDimensionPixelSize(R.dimen.recents_task_stack_animation_launched_while_docking_offset);
        boolean isLandscape = appResources.getConfiguration().orientation == 2;
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        for (int i = taskViews.size() - 1; i >= 0; i--) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (launchTargetTask == null || launchTargetTask.group == null) {
                zIsTaskAboveTask = false;
            } else {
                zIsTaskAboveTask = launchTargetTask.group.isTaskAboveTask(task, launchTargetTask);
            }
            if (launchTargetTask == null || !launchTargetTask.isFreeformTask()) {
                zIsFreeformTask = false;
            } else {
                zIsFreeformTask = task.isFreeformTask();
            }
            stackLayout.getStackTransform(task, stackScroller.getStackScroll(), this.mTmpTransform, null);
            if (zIsFreeformTask) {
                tv.setVisibility(4);
            } else if (launchState.launchedFromApp && !launchState.launchedViaDockGesture) {
                if (task.isLaunchTarget) {
                    tv.onPrepareLaunchTargetForEnterAnimation();
                } else if (zIsTaskAboveTask) {
                    this.mTmpTransform.rect.offset(0.0f, taskViewAffiliateGroupEnterOffset);
                    this.mTmpTransform.alpha = 0.0f;
                    this.mStackView.updateTaskViewToTransform(tv, this.mTmpTransform, AnimationProps.IMMEDIATE);
                    tv.setClipViewInStack(false);
                }
            } else if (launchState.launchedFromHome) {
                this.mTmpTransform.rect.offset(0.0f, offscreenYOffset);
                this.mTmpTransform.alpha = 0.0f;
                this.mStackView.updateTaskViewToTransform(tv, this.mTmpTransform, AnimationProps.IMMEDIATE);
            } else if (launchState.launchedViaDockGesture) {
                if (isLandscape) {
                    offset = launchedWhileDockingOffset;
                } else {
                    offset = (int) (offscreenYOffset * 0.9f);
                }
                this.mTmpTransform.rect.offset(0.0f, offset);
                this.mTmpTransform.alpha = 0.0f;
                this.mStackView.updateTaskViewToTransform(tv, this.mTmpTransform, AnimationProps.IMMEDIATE);
            }
        }
    }

    public void startEnterAnimation(final ReferenceCountedTrigger postAnimationTrigger) {
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();
        Resources res = this.mStackView.getResources();
        Resources appRes = this.mStackView.getContext().getApplicationContext().getResources();
        TaskStackLayoutAlgorithm stackLayout = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller stackScroller = this.mStackView.getScroller();
        TaskStack stack = this.mStackView.getStack();
        Task launchTargetTask = stack.getLaunchTarget();
        if (stack.getTaskCount() == 0) {
            return;
        }
        int taskViewEnterFromAppDuration = res.getInteger(R.integer.recents_task_enter_from_app_duration);
        int taskViewEnterFromAffiliatedAppDuration = res.getInteger(R.integer.recents_task_enter_from_affiliated_app_duration);
        int dockGestureAnimDuration = appRes.getInteger(R.integer.long_press_dock_anim_duration);
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = taskViewCount - 1; i >= 0; i--) {
            int taskIndexFromFront = (taskViewCount - i) - 1;
            int taskIndexFromBack = i;
            final TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            boolean zIsTaskAboveTask = (launchTargetTask == null || launchTargetTask.group == null) ? false : launchTargetTask.group.isTaskAboveTask(task, launchTargetTask);
            stackLayout.getStackTransform(task, stackScroller.getStackScroll(), this.mTmpTransform, null);
            if (!launchState.launchedFromApp || launchState.launchedViaDockGesture) {
                if (launchState.launchedFromHome) {
                    AnimationProps taskAnimation = new AnimationProps().setInitialPlayTime(6, Math.min(5, taskIndexFromFront) * 33).setStartDelay(4, Math.min(5, taskIndexFromFront) * 16).setDuration(6, 300).setDuration(4, 100).setInterpolator(6, ENTER_FROM_HOME_TRANSLATION_INTERPOLATOR).setInterpolator(4, ENTER_FROM_HOME_ALPHA_INTERPOLATOR).setListener(postAnimationTrigger.decrementOnAnimationEnd());
                    postAnimationTrigger.increment();
                    this.mStackView.updateTaskViewToTransform(tv, this.mTmpTransform, taskAnimation);
                    if (i == taskViewCount - 1) {
                        tv.onStartFrontTaskEnterAnimation(this.mStackView.mScreenPinningEnabled);
                    }
                } else if (launchState.launchedViaDockGesture) {
                    AnimationProps taskAnimation2 = new AnimationProps().setDuration(6, (taskIndexFromBack * 33) + dockGestureAnimDuration).setInterpolator(6, ENTER_WHILE_DOCKING_INTERPOLATOR).setStartDelay(6, 48).setListener(postAnimationTrigger.decrementOnAnimationEnd());
                    postAnimationTrigger.increment();
                    this.mStackView.updateTaskViewToTransform(tv, this.mTmpTransform, taskAnimation2);
                }
            } else if (task.isLaunchTarget) {
                tv.onStartLaunchTargetEnterAnimation(this.mTmpTransform, taskViewEnterFromAppDuration, this.mStackView.mScreenPinningEnabled, postAnimationTrigger);
            } else if (zIsTaskAboveTask) {
                AnimationProps taskAnimation3 = new AnimationProps(taskViewEnterFromAffiliatedAppDuration, Interpolators.ALPHA_IN, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        postAnimationTrigger.decrement();
                        tv.setClipViewInStack(true);
                    }
                });
                postAnimationTrigger.increment();
                this.mStackView.updateTaskViewToTransform(tv, this.mTmpTransform, taskAnimation3);
            }
        }
    }

    public void startExitToHomeAnimation(boolean animated, ReferenceCountedTrigger postAnimationTrigger) {
        AnimationProps taskAnimation;
        TaskStackLayoutAlgorithm stackLayout = this.mStackView.getStackAlgorithm();
        TaskStack stack = this.mStackView.getStack();
        if (stack.getTaskCount() == 0) {
            return;
        }
        int offscreenYOffset = stackLayout.mStackRect.height();
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            int taskIndexFromFront = (taskViewCount - i) - 1;
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (!this.mStackView.isIgnoredTask(task)) {
                if (animated) {
                    int delay = Math.min(5, taskIndexFromFront) * 33;
                    taskAnimation = new AnimationProps().setStartDelay(6, delay).setDuration(6, 200).setInterpolator(6, EXIT_TO_HOME_TRANSLATION_INTERPOLATOR).setListener(postAnimationTrigger.decrementOnAnimationEnd());
                    postAnimationTrigger.increment();
                } else {
                    taskAnimation = AnimationProps.IMMEDIATE;
                }
                this.mTmpTransform.fillIn(tv);
                this.mTmpTransform.rect.offset(0.0f, offscreenYOffset);
                this.mStackView.updateTaskViewToTransform(tv, this.mTmpTransform, taskAnimation);
            }
        }
    }

    public void startLaunchTaskAnimation(TaskView launchingTaskView, boolean screenPinningRequested, ReferenceCountedTrigger postAnimationTrigger) {
        boolean currentTaskOccludesLaunchTarget;
        Resources res = this.mStackView.getResources();
        int taskViewExitToAppDuration = res.getInteger(R.integer.recents_task_exit_to_app_duration);
        int taskViewAffiliateGroupEnterOffset = res.getDimensionPixelSize(R.dimen.recents_task_stack_animation_affiliate_enter_offset);
        Task launchingTask = launchingTaskView.getTask();
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            final TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (launchingTask == null || launchingTask.group == null) {
                currentTaskOccludesLaunchTarget = false;
            } else {
                currentTaskOccludesLaunchTarget = launchingTask.group.isTaskAboveTask(task, launchingTask);
            }
            if (tv == launchingTaskView) {
                tv.setClipViewInStack(false);
                postAnimationTrigger.addLastDecrementRunnable(new Runnable() {
                    @Override
                    public void run() {
                        tv.setClipViewInStack(true);
                    }
                });
                tv.onStartLaunchTargetLaunchAnimation(taskViewExitToAppDuration, screenPinningRequested, postAnimationTrigger);
            } else if (currentTaskOccludesLaunchTarget) {
                AnimationProps taskAnimation = new AnimationProps(taskViewExitToAppDuration, Interpolators.ALPHA_OUT, postAnimationTrigger.decrementOnAnimationEnd());
                postAnimationTrigger.increment();
                this.mTmpTransform.fillIn(tv);
                this.mTmpTransform.alpha = 0.0f;
                this.mTmpTransform.rect.offset(0.0f, taskViewAffiliateGroupEnterOffset);
                this.mStackView.updateTaskViewToTransform(tv, this.mTmpTransform, taskAnimation);
            }
        }
    }

    public void startDeleteTaskAnimation(final TaskView deleteTaskView, final ReferenceCountedTrigger postAnimationTrigger) {
        final TaskStackViewTouchHandler touchHandler = this.mStackView.getTouchHandler();
        touchHandler.onBeginManualDrag(deleteTaskView);
        postAnimationTrigger.increment();
        postAnimationTrigger.addLastDecrementRunnable(new Runnable() {
            @Override
            public void run() {
                touchHandler.onChildDismissed(deleteTaskView);
            }
        });
        final float dismissSize = touchHandler.getScaledDismissSize();
        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(400L);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator arg0) {
                TaskStackAnimationHelper.m1073x884e26cc(deleteTaskView, dismissSize, touchHandler, arg0);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                postAnimationTrigger.decrement();
            }
        });
        animator.start();
    }

    static void m1073x884e26cc(TaskView deleteTaskView, float dismissSize, TaskStackViewTouchHandler touchHandler, ValueAnimator animation) {
        float progress = ((Float) animation.getAnimatedValue()).floatValue();
        deleteTaskView.setTranslationX(progress * dismissSize);
        touchHandler.updateSwipeProgress(deleteTaskView, true, progress);
    }

    public void startDeleteAllTasksAnimation(List<TaskView> taskViews, final ReferenceCountedTrigger postAnimationTrigger) {
        TaskStackLayoutAlgorithm stackLayout = this.mStackView.getStackAlgorithm();
        int offscreenXOffset = this.mStackView.getMeasuredWidth() - stackLayout.mTaskRect.left;
        int taskViewCount = taskViews.size();
        for (int i = taskViewCount - 1; i >= 0; i--) {
            final TaskView tv = taskViews.get(i);
            int taskIndexFromFront = (taskViewCount - i) - 1;
            int startDelay = taskIndexFromFront * 33;
            tv.setClipViewInStack(false);
            AnimationProps taskAnimation = new AnimationProps(startDelay, 200, DISMISS_ALL_TRANSLATION_INTERPOLATOR, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    postAnimationTrigger.decrement();
                    tv.setClipViewInStack(true);
                }
            });
            postAnimationTrigger.increment();
            this.mTmpTransform.fillIn(tv);
            this.mTmpTransform.rect.offset(offscreenXOffset, 0.0f);
            this.mStackView.updateTaskViewToTransform(tv, this.mTmpTransform, taskAnimation);
        }
    }

    public boolean startScrollToFocusedTaskAnimation(Task newFocusedTask, boolean requestViewFocus) {
        int duration;
        Interpolator interpolator;
        TaskStackLayoutAlgorithm stackLayout = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller stackScroller = this.mStackView.getScroller();
        TaskStack stack = this.mStackView.getStack();
        float curScroll = stackScroller.getStackScroll();
        final float newScroll = stackScroller.getBoundedStackScroll(stackLayout.getStackScrollForTask(newFocusedTask));
        boolean willScrollToFront = newScroll > curScroll;
        boolean willScroll = Float.compare(newScroll, curScroll) != 0;
        int taskViewCount = this.mStackView.getTaskViews().size();
        ArrayList<Task> stackTasks = stack.getStackTasks();
        this.mStackView.getCurrentTaskTransforms(stackTasks, this.mTmpCurrentTaskTransforms);
        this.mStackView.bindVisibleTaskViews(newScroll);
        stackLayout.setFocusState(1);
        stackScroller.setStackScroll(newScroll, null);
        this.mStackView.cancelDeferredTaskViewLayoutAnimation();
        this.mStackView.getLayoutTaskTransforms(newScroll, stackLayout.getFocusState(), stackTasks, true, this.mTmpFinalTaskTransforms);
        TaskView newFocusedTaskView = this.mStackView.getChildViewForTask(newFocusedTask);
        if (newFocusedTaskView == null) {
            Log.e("TaskStackAnimationHelper", "b/27389156 null-task-view prebind:" + taskViewCount + " postbind:" + this.mStackView.getTaskViews().size() + " prescroll:" + curScroll + " postscroll: " + newScroll);
            return false;
        }
        newFocusedTaskView.setFocusedState(true, requestViewFocus);
        ReferenceCountedTrigger postAnimTrigger = new ReferenceCountedTrigger();
        postAnimTrigger.addLastDecrementRunnable(new Runnable() {
            @Override
            public void run() {
                TaskStackAnimationHelper.this.mStackView.bindVisibleTaskViews(newScroll);
            }
        });
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int taskViewCount2 = taskViews.size();
        int newFocusTaskViewIndex = taskViews.indexOf(newFocusedTaskView);
        for (int i = 0; i < taskViewCount2; i++) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (!this.mStackView.isIgnoredTask(task)) {
                int taskIndex = stackTasks.indexOf(task);
                TaskViewTransform fromTransform = this.mTmpCurrentTaskTransforms.get(taskIndex);
                TaskViewTransform toTransform = this.mTmpFinalTaskTransforms.get(taskIndex);
                this.mStackView.updateTaskViewToTransform(tv, fromTransform, AnimationProps.IMMEDIATE);
                if (willScrollToFront) {
                    duration = calculateStaggeredAnimDuration(i);
                    interpolator = FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR;
                } else if (i < newFocusTaskViewIndex) {
                    duration = (((newFocusTaskViewIndex - i) - 1) * 50) + 150;
                    interpolator = FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR;
                } else if (i > newFocusTaskViewIndex) {
                    duration = Math.max(100, 150 - (((i - newFocusTaskViewIndex) - 1) * 50));
                    interpolator = FOCUS_IN_FRONT_NEXT_TASK_INTERPOLATOR;
                } else {
                    duration = 200;
                    interpolator = FOCUS_NEXT_TASK_INTERPOLATOR;
                }
                AnimationProps anim = new AnimationProps().setDuration(6, duration).setInterpolator(6, interpolator).setListener(postAnimTrigger.decrementOnAnimationEnd());
                postAnimTrigger.increment();
                this.mStackView.updateTaskViewToTransform(tv, toTransform, anim);
            }
        }
        return willScroll;
    }

    public void startNewStackScrollAnimation(TaskStack newStack, ReferenceCountedTrigger animationTrigger) {
        int taskIndex;
        TaskStackLayoutAlgorithm stackLayout = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller stackScroller = this.mStackView.getScroller();
        ArrayList<Task> stackTasks = newStack.getStackTasks();
        this.mStackView.getCurrentTaskTransforms(stackTasks, this.mTmpCurrentTaskTransforms);
        this.mStackView.setTasks(newStack, false);
        this.mStackView.updateLayoutAlgorithm(false);
        final float newScroll = stackLayout.mInitialScrollP;
        this.mStackView.bindVisibleTaskViews(newScroll);
        stackLayout.setFocusState(0);
        stackLayout.setTaskOverridesForInitialState(newStack, true);
        stackScroller.setStackScroll(newScroll);
        this.mStackView.cancelDeferredTaskViewLayoutAnimation();
        this.mStackView.getLayoutTaskTransforms(newScroll, stackLayout.getFocusState(), stackTasks, false, this.mTmpFinalTaskTransforms);
        Task frontMostTask = newStack.getStackFrontMostTask(false);
        final TaskView frontMostTaskView = this.mStackView.getChildViewForTask(frontMostTask);
        final TaskViewTransform frontMostTransform = this.mTmpFinalTaskTransforms.get(stackTasks.indexOf(frontMostTask));
        if (frontMostTaskView != null) {
            this.mStackView.updateTaskViewToTransform(frontMostTaskView, stackLayout.getFrontOfStackTransform(), AnimationProps.IMMEDIATE);
        }
        animationTrigger.addLastDecrementRunnable(new Runnable() {
            @Override
            public void run() {
                TaskStackAnimationHelper.this.mStackView.bindVisibleTaskViews(newScroll);
                if (frontMostTaskView == null) {
                    return;
                }
                TaskStackAnimationHelper.this.mStackView.updateTaskViewToTransform(frontMostTaskView, frontMostTransform, new AnimationProps(75, 250, TaskStackAnimationHelper.FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR));
            }
        });
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (!this.mStackView.isIgnoredTask(task) && ((task != frontMostTask || frontMostTaskView == null) && (taskIndex = stackTasks.indexOf(task)) != -1)) {
                TaskViewTransform fromTransform = this.mTmpCurrentTaskTransforms.get(taskIndex);
                TaskViewTransform toTransform = this.mTmpFinalTaskTransforms.get(taskIndex);
                this.mStackView.updateTaskViewToTransform(tv, fromTransform, AnimationProps.IMMEDIATE);
                int duration = calculateStaggeredAnimDuration(i);
                Interpolator interpolator = FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR;
                AnimationProps anim = new AnimationProps().setDuration(6, duration).setInterpolator(6, interpolator).setListener(animationTrigger.decrementOnAnimationEnd());
                animationTrigger.increment();
                this.mStackView.updateTaskViewToTransform(tv, toTransform, anim);
            }
        }
    }

    private int calculateStaggeredAnimDuration(int i) {
        return Math.max(100, ((i - 1) * 50) + 100);
    }
}
