package com.android.systemui.recents.tv.views;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.LaunchTvTaskEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;

public class RecentsTvView extends FrameLayout {
    private boolean mAwaitingFirstLayout;
    private View mDismissPlaceholder;
    private View mEmptyView;
    private final Handler mHandler;
    private RecyclerView.OnScrollListener mScrollListener;
    private TaskStack mStack;
    private Rect mSystemInsets;
    private TaskStackHorizontalGridView mTaskStackHorizontalView;
    private RecentsTvTransitionHelper mTransitionHelper;

    public RecentsTvView(Context context) {
        this(context, null);
    }

    public RecentsTvView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsTvView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RecentsTvView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mAwaitingFirstLayout = true;
        this.mSystemInsets = new Rect();
        this.mHandler = new Handler();
        setWillNotDraw(false);
        LayoutInflater inflater = LayoutInflater.from(context);
        this.mEmptyView = inflater.inflate(R.layout.recents_tv_empty, (ViewGroup) this, false);
        addView(this.mEmptyView);
        this.mTransitionHelper = new RecentsTvTransitionHelper(this.mContext, this.mHandler);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDismissPlaceholder = findViewById(R.id.dismiss_placeholder);
        this.mTaskStackHorizontalView = (TaskStackHorizontalGridView) findViewById(R.id.task_list);
    }

    public void init(TaskStack stack) {
        RecentsConfiguration config = Recents.getConfiguration();
        config.getLaunchState();
        this.mStack = stack;
        this.mTaskStackHorizontalView.init(stack);
        if (stack.getStackTaskCount() > 0) {
            hideEmptyView();
        } else {
            showEmptyView();
        }
        requestLayout();
    }

    public boolean launchFocusedTask() {
        Task task;
        if (this.mTaskStackHorizontalView != null && (task = this.mTaskStackHorizontalView.getFocusedTask()) != null) {
            launchTaskFomRecents(task, true);
            return true;
        }
        return false;
    }

    public boolean launchPreviousTask(boolean animate) {
        if (this.mTaskStackHorizontalView != null) {
            TaskStack stack = this.mTaskStackHorizontalView.getStack();
            Task task = stack.getLaunchTarget();
            if (task != null) {
                launchTaskFomRecents(task, animate);
                return true;
            }
            return false;
        }
        return false;
    }

    private void launchTaskFomRecents(final Task task, boolean animate) {
        if (!animate) {
            SystemServicesProxy ssp = Recents.getSystemServices();
            ssp.startActivityFromRecents(getContext(), task.key, task.title, null);
            return;
        }
        this.mTaskStackHorizontalView.requestFocus();
        Task focusedTask = this.mTaskStackHorizontalView.getFocusedTask();
        if (focusedTask != null && task != focusedTask) {
            if (this.mScrollListener != null) {
                this.mTaskStackHorizontalView.removeOnScrollListener(this.mScrollListener);
            }
            this.mScrollListener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState != 0) {
                        return;
                    }
                    TaskCardView cardView = RecentsTvView.this.mTaskStackHorizontalView.getChildViewForTask(task);
                    if (cardView != null) {
                        RecentsTvView.this.mTransitionHelper.launchTaskFromRecents(RecentsTvView.this.mStack, task, RecentsTvView.this.mTaskStackHorizontalView, cardView, null, -1);
                    } else {
                        Log.e("RecentsTvView", "Card view for task : " + task + ", returned null.");
                        SystemServicesProxy ssp2 = Recents.getSystemServices();
                        ssp2.startActivityFromRecents(RecentsTvView.this.getContext(), task.key, task.title, null);
                    }
                    RecentsTvView.this.mTaskStackHorizontalView.removeOnScrollListener(RecentsTvView.this.mScrollListener);
                }
            };
            this.mTaskStackHorizontalView.addOnScrollListener(this.mScrollListener);
            this.mTaskStackHorizontalView.setSelectedPositionSmooth(((TaskStackHorizontalViewAdapter) this.mTaskStackHorizontalView.getAdapter()).getPositionOfTask(task));
            return;
        }
        this.mTransitionHelper.launchTaskFromRecents(this.mStack, task, this.mTaskStackHorizontalView, this.mTaskStackHorizontalView.getChildViewForTask(task), null, -1);
    }

    public void showEmptyView() {
        this.mEmptyView.setVisibility(0);
        this.mTaskStackHorizontalView.setVisibility(8);
        if (!Recents.getSystemServices().isTouchExplorationEnabled()) {
            return;
        }
        this.mDismissPlaceholder.setVisibility(8);
    }

    public void hideEmptyView() {
        this.mEmptyView.setVisibility(8);
        this.mTaskStackHorizontalView.setVisibility(0);
        if (!Recents.getSystemServices().isTouchExplorationEnabled()) {
            return;
        }
        this.mDismissPlaceholder.setVisibility(0);
    }

    @Override
    protected void onAttachedToWindow() {
        EventBus.getDefault().register(this, 3);
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mSystemInsets.set(insets.getSystemWindowInsets());
        requestLayout();
        return insets;
    }

    public final void onBusEvent(LaunchTvTaskEvent event) {
        this.mTransitionHelper.launchTaskFromRecents(this.mStack, event.task, this.mTaskStackHorizontalView, event.taskView, event.targetTaskBounds, event.targetTaskStack);
    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted event) {
        EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(null));
    }

    public final void onBusEvent(RecentsVisibilityChangedEvent event) {
        if (event.visible) {
            return;
        }
        this.mAwaitingFirstLayout = true;
    }

    public TaskStackHorizontalGridView setTaskStackViewAdapter(TaskStackHorizontalViewAdapter taskStackViewAdapter) {
        if (this.mTaskStackHorizontalView != null) {
            this.mTaskStackHorizontalView.setAdapter(taskStackViewAdapter);
            taskStackViewAdapter.setTaskStackHorizontalGridView(this.mTaskStackHorizontalView);
        }
        return this.mTaskStackHorizontalView;
    }
}
