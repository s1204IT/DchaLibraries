package com.android.systemui.recents.tv.views;

import android.content.Context;
import android.support.v17.leanback.widget.HorizontalGridView;
import android.util.AttributeSet;
import com.android.systemui.R;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.AnimationProps;

public class TaskStackHorizontalGridView extends HorizontalGridView implements TaskStack.TaskStackCallbacks {
    private Task mFocusedTask;
    private TaskStack mStack;

    public TaskStackHorizontalGridView(Context context) {
        this(context, null);
    }

    public TaskStackHorizontalGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        EventBus.getDefault().register(this, 3);
        setWindowAlignment(0);
        setImportantForAccessibility(1);
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    public void init(TaskStack stack) {
        this.mStack = stack;
        if (this.mStack == null) {
            return;
        }
        this.mStack.setCallbacks(this);
    }

    public TaskStack getStack() {
        return this.mStack;
    }

    public Task getFocusedTask() {
        if (findFocus() != null) {
            this.mFocusedTask = ((TaskCardView) findFocus()).getTask();
        }
        return this.mFocusedTask;
    }

    public TaskCardView getChildViewForTask(Task task) {
        for (int i = 0; i < getChildCount(); i++) {
            TaskCardView tv = (TaskCardView) getChildAt(i);
            if (tv.getTask() == task) {
                return tv;
            }
        }
        return null;
    }

    public void startFocusGainAnimation() {
        for (int i = 0; i < getChildCount(); i++) {
            TaskCardView v = (TaskCardView) getChildAt(i);
            if (v.hasFocus()) {
                v.getViewFocusAnimator().changeSize(true);
            }
            v.getRecentsRowFocusAnimationHolder().startFocusGainAnimation();
        }
    }

    public void startFocusLossAnimation() {
        for (int i = 0; i < getChildCount(); i++) {
            TaskCardView v = (TaskCardView) getChildAt(i);
            if (v.hasFocus()) {
                v.getViewFocusAnimator().changeSize(false);
            }
            v.getRecentsRowFocusAnimationHolder().startFocusLossAnimation();
        }
    }

    @Override
    public void onStackTaskAdded(TaskStack stack, Task newTask) {
        ((TaskStackHorizontalViewAdapter) getAdapter()).addTaskAt(newTask, stack.indexOfStackTask(newTask));
    }

    @Override
    public void onStackTaskRemoved(TaskStack stack, Task removedTask, Task newFrontMostTask, AnimationProps animation, boolean fromDockGesture) {
        int i;
        ((TaskStackHorizontalViewAdapter) getAdapter()).removeTask(removedTask);
        if (this.mFocusedTask == removedTask) {
            this.mFocusedTask = null;
        }
        if (this.mStack.getStackTaskCount() != 0) {
            return;
        }
        boolean shouldFinishActivity = this.mStack.getStackTaskCount() == 0;
        if (!shouldFinishActivity) {
            return;
        }
        EventBus eventBus = EventBus.getDefault();
        if (fromDockGesture) {
            i = R.string.recents_empty_message;
        } else {
            i = R.string.recents_empty_message_dismissed_all;
        }
        eventBus.send(new AllTaskViewsDismissedEvent(i));
    }

    @Override
    public void onStackTasksRemoved(TaskStack stack) {
    }

    @Override
    public void onStackTasksUpdated(TaskStack stack) {
    }
}
