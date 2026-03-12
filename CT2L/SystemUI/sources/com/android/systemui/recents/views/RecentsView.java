package com.android.systemui.recents.views;

import android.app.ActivityOptions;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsPackageMonitor;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.TaskStackView;
import com.android.systemui.recents.views.ViewAnimation;
import java.util.ArrayList;

public class RecentsView extends FrameLayout implements RecentsPackageMonitor.PackageCallbacks, TaskStackView.TaskStackViewCallbacks {
    RecentsViewCallbacks mCb;
    RecentsConfiguration mConfig;
    DebugOverlayView mDebugOverlay;
    LayoutInflater mInflater;
    View mSearchBar;
    ArrayList<TaskStack> mStacks;

    public interface RecentsViewCallbacks {
        void onAllTaskViewsDismissed();

        void onExitToHomeAnimationTriggered();

        void onScreenPinningRequest();

        void onTaskLaunchFailed();

        void onTaskViewClicked();
    }

    public RecentsView(Context context) {
        super(context);
    }

    public RecentsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RecentsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mConfig = RecentsConfiguration.getInstance();
        this.mInflater = LayoutInflater.from(context);
    }

    public void setCallbacks(RecentsViewCallbacks cb) {
        this.mCb = cb;
    }

    public void setTaskStacks(ArrayList<TaskStack> stacks) {
        int numStacks = stacks.size();
        ArrayList<TaskStackView> stackViews = new ArrayList<>();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar) {
                stackViews.add((TaskStackView) child);
            }
        }
        int numTaskStacksToKeep = 0;
        if (this.mConfig.launchedReuseTaskStackViews) {
            numTaskStacksToKeep = Math.min(childCount, numStacks);
        }
        for (int i2 = stackViews.size() - 1; i2 >= numTaskStacksToKeep; i2--) {
            removeView(stackViews.get(i2));
            stackViews.remove(i2);
        }
        for (int i3 = 0; i3 < numTaskStacksToKeep; i3++) {
            TaskStackView tsv = stackViews.get(i3);
            tsv.reset();
            tsv.setStack(stacks.get(i3));
        }
        this.mStacks = stacks;
        for (int i4 = stackViews.size(); i4 < numStacks; i4++) {
            TaskStack stack = stacks.get(i4);
            TaskStackView stackView = new TaskStackView(getContext(), stack);
            stackView.setCallbacks(this);
            addView(stackView);
        }
        if (this.mConfig.debugModeEnabled) {
            for (int i5 = childCount - 1; i5 >= 0; i5--) {
                View v = getChildAt(i5);
                if (v != this.mSearchBar) {
                    ((TaskStackView) v).setDebugOverlay(this.mDebugOverlay);
                }
            }
        }
        requestLayout();
    }

    public boolean launchFocusedTask() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar) {
                TaskStackView stackView = (TaskStackView) child;
                TaskStack stack = stackView.mStack;
                int taskCount = stackView.getChildCount();
                for (int j = 0; j < taskCount; j++) {
                    TaskView tv = (TaskView) stackView.getChildAt(j);
                    Task task = tv.getTask();
                    if (tv.isFocusedTask()) {
                        onTaskViewClicked(stackView, tv, stack, task, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean launchPreviousTask() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar) {
                TaskStackView stackView = (TaskStackView) child;
                TaskStack stack = stackView.mStack;
                ArrayList<Task> tasks = stack.getTasks();
                if (tasks.isEmpty()) {
                    continue;
                } else {
                    int taskCount = tasks.size();
                    for (int j = 0; j < taskCount; j++) {
                        if (tasks.get(j).isLaunchTarget) {
                            Task task = tasks.get(j);
                            TaskView tv = stackView.getChildViewForTask(task);
                            onTaskViewClicked(stackView, tv, stack, task, false);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void startEnterRecentsAnimation(ViewAnimation.TaskViewEnterContext ctx) {
        ctx.postAnimationTrigger.increment();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar) {
                TaskStackView stackView = (TaskStackView) child;
                stackView.startEnterRecentsAnimation(ctx);
            }
        }
        ctx.postAnimationTrigger.decrement();
    }

    public void startExitToHomeAnimation(ViewAnimation.TaskViewExitContext ctx) {
        ctx.postAnimationTrigger.increment();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar) {
                TaskStackView stackView = (TaskStackView) child;
                stackView.startExitToHomeAnimation(ctx);
            }
        }
        ctx.postAnimationTrigger.decrement();
        this.mCb.onExitToHomeAnimationTriggered();
    }

    public void setSearchBar(View searchBar) {
        if (this.mSearchBar != null && indexOfChild(this.mSearchBar) > -1) {
            removeView(this.mSearchBar);
        }
        if (searchBar != null) {
            this.mSearchBar = searchBar;
            addView(this.mSearchBar);
        }
    }

    public boolean hasSearchBar() {
        return this.mSearchBar != null;
    }

    public void setSearchBarVisibility(int visibility) {
        if (this.mSearchBar != null) {
            this.mSearchBar.setVisibility(visibility);
            this.mSearchBar.bringToFront();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        if (this.mSearchBar != null) {
            Rect searchBarSpaceBounds = new Rect();
            this.mConfig.getSearchBarBounds(width, height, this.mConfig.systemInsets.top, searchBarSpaceBounds);
            this.mSearchBar.measure(View.MeasureSpec.makeMeasureSpec(searchBarSpaceBounds.width(), 1073741824), View.MeasureSpec.makeMeasureSpec(searchBarSpaceBounds.height(), 1073741824));
        }
        Rect taskStackBounds = new Rect();
        this.mConfig.getTaskStackBounds(width, height, this.mConfig.systemInsets.top, this.mConfig.systemInsets.right, taskStackBounds);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar && child.getVisibility() != 8) {
                TaskStackView tsv = (TaskStackView) child;
                tsv.setStackInsetRect(taskStackBounds);
                tsv.measure(widthMeasureSpec, heightMeasureSpec);
            }
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mSearchBar != null) {
            Rect searchBarSpaceBounds = new Rect();
            this.mConfig.getSearchBarBounds(getMeasuredWidth(), getMeasuredHeight(), this.mConfig.systemInsets.top, searchBarSpaceBounds);
            this.mSearchBar.layout(searchBarSpaceBounds.left, searchBarSpaceBounds.top, searchBarSpaceBounds.right, searchBarSpaceBounds.bottom);
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar && child.getVisibility() != 8) {
                child.layout(left, top, child.getMeasuredWidth() + left, child.getMeasuredHeight() + top);
            }
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mConfig.updateSystemInsets(insets.getSystemWindowInsets());
        requestLayout();
        return insets.consumeSystemWindowInsets();
    }

    public void onUserInteraction() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar) {
                TaskStackView stackView = (TaskStackView) child;
                stackView.onUserInteraction();
            }
        }
    }

    public void focusNextTask(boolean forward) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar) {
                TaskStackView stackView = (TaskStackView) child;
                stackView.focusNextTask(forward, true);
                return;
            }
        }
    }

    public void dismissFocusedTask() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar) {
                TaskStackView stackView = (TaskStackView) child;
                stackView.dismissFocusedTask();
                return;
            }
        }
    }

    public boolean unfilterFilteredStacks() {
        if (this.mStacks != null) {
            boolean stacksUnfiltered = false;
            int numStacks = this.mStacks.size();
            for (int i = 0; i < numStacks; i++) {
                TaskStack stack = this.mStacks.get(i);
                if (stack.hasFilteredTasks()) {
                    stack.unfilterTasks();
                    stacksUnfiltered = true;
                }
            }
            return stacksUnfiltered;
        }
        return false;
    }

    @Override
    public void onTaskViewClicked(TaskStackView stackView, TaskView tv, TaskStack stack, final Task task, final boolean lockToTask) {
        View sourceView;
        TaskViewTransform transform;
        Bitmap b;
        if (this.mCb != null) {
            this.mCb.onTaskViewClicked();
        }
        TaskViewTransform transform2 = new TaskViewTransform();
        int offsetX = 0;
        int offsetY = 0;
        float stackScroll = stackView.getScroller().getStackScroll();
        if (tv == null) {
            sourceView = stackView;
            transform = stackView.getStackAlgorithm().getStackTransform(task, stackScroll, transform2, (TaskViewTransform) null);
            offsetX = transform.rect.left;
            offsetY = this.mConfig.displayRect.height();
        } else {
            sourceView = tv.mThumbnailView;
            transform = stackView.getStackAlgorithm().getStackTransform(task, stackScroll, transform2, (TaskViewTransform) null);
        }
        final SystemServicesProxy ssp = RecentsTaskLoader.getInstance().getSystemServicesProxy();
        ActivityOptions opts = null;
        if (task.thumbnail != null && task.thumbnail.getWidth() > 0 && task.thumbnail.getHeight() > 0) {
            if (tv != null) {
                if (tv.isFocusedTask()) {
                    tv.unsetFocusedTask();
                }
                float scale = tv.getScaleX();
                int fromHeaderWidth = (int) (tv.mHeaderView.getMeasuredWidth() * scale);
                int fromHeaderHeight = (int) (tv.mHeaderView.getMeasuredHeight() * scale);
                b = Bitmap.createBitmap(fromHeaderWidth, fromHeaderHeight, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(b);
                c.scale(tv.getScaleX(), tv.getScaleY());
                tv.mHeaderView.draw(c);
                c.setBitmap(null);
            } else {
                b = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
            }
            ActivityOptions.OnAnimationStartedListener animStartedListener = null;
            if (lockToTask) {
                animStartedListener = new ActivityOptions.OnAnimationStartedListener() {
                    boolean mTriggered = false;

                    public void onAnimationStarted() {
                        if (!this.mTriggered) {
                            RecentsView.this.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    RecentsView.this.mCb.onScreenPinningRequest();
                                }
                            }, 350L);
                            this.mTriggered = true;
                        }
                    }
                };
            }
            opts = ActivityOptions.makeThumbnailAspectScaleUpAnimation(sourceView, b, offsetX, offsetY, transform.rect.width(), transform.rect.height(), sourceView.getHandler(), animStartedListener);
        }
        final ActivityOptions launchOpts = opts;
        Runnable launchRunnable = new Runnable() {
            @Override
            public void run() {
                if (task.isActive) {
                    ssp.moveTaskToFront(task.key.id, launchOpts);
                    return;
                }
                if (ssp.startActivityFromRecents(RecentsView.this.getContext(), task.key.id, task.activityLabel, launchOpts)) {
                    if (launchOpts == null && lockToTask) {
                        RecentsView.this.mCb.onScreenPinningRequest();
                        return;
                    }
                    return;
                }
                RecentsView.this.onTaskViewDismissed(task);
                if (RecentsView.this.mCb != null) {
                    RecentsView.this.mCb.onTaskLaunchFailed();
                }
            }
        };
        if (tv == null) {
            launchRunnable.run();
        } else if (!task.group.isFrontMostTask(task)) {
            stackView.startLaunchTaskAnimation(tv, launchRunnable, lockToTask);
        } else {
            stackView.startLaunchTaskAnimation(tv, null, lockToTask);
            launchRunnable.run();
        }
    }

    @Override
    public void onTaskViewAppInfoClicked(Task t) {
        Intent baseIntent = t.key.baseIntent;
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", baseIntent.getComponent().getPackageName(), null));
        intent.setComponent(intent.resolveActivity(getContext().getPackageManager()));
        TaskStackBuilder.create(getContext()).addNextIntentWithParentStack(intent).startActivities(null, new UserHandle(t.key.userId));
    }

    @Override
    public void onTaskViewDismissed(Task t) {
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        loader.deleteTaskData(t, false);
        loader.getSystemServicesProxy().removeTask(t.key.id);
    }

    @Override
    public void onAllTaskViewsDismissed() {
        this.mCb.onAllTaskViewsDismissed();
    }

    public void onRecentsHidden() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar) {
                TaskStackView stackView = (TaskStackView) child;
                stackView.onRecentsHidden();
            }
        }
    }

    @Override
    public void onPackagesChanged(RecentsPackageMonitor monitor, String packageName, int userId) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != this.mSearchBar) {
                TaskStackView stackView = (TaskStackView) child;
                stackView.onPackagesChanged(monitor, packageName, userId);
            }
        }
    }
}
