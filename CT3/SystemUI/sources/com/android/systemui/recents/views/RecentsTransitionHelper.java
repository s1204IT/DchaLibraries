package com.android.systemui.recents.views;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.util.Log;
import android.view.AppTransitionAnimationSpec;
import android.view.IAppTransitionAnimationSpecsFuture;
import com.android.internal.annotations.GuardedBy;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.ExitRecentsWindowFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.LaunchTaskFailedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskStartedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskSucceededEvent;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentsTransitionHelper {
    private static final List<AppTransitionAnimationSpec> SPECS_WAITING = new ArrayList();
    private Context mContext;

    @GuardedBy("this")
    private List<AppTransitionAnimationSpec> mAppTransitionAnimationSpecs = SPECS_WAITING;
    private TaskViewTransform mTmpTransform = new TaskViewTransform();
    private StartScreenPinningRunnableRunnable mStartScreenPinningRunnable = new StartScreenPinningRunnableRunnable(this, null);
    private Handler mHandler = new Handler();

    public interface AnimationSpecComposer {
        List<AppTransitionAnimationSpec> composeSpecs();
    }

    private class StartScreenPinningRunnableRunnable implements Runnable {
        private int taskId;

        StartScreenPinningRunnableRunnable(RecentsTransitionHelper this$0, StartScreenPinningRunnableRunnable startScreenPinningRunnableRunnable) {
            this();
        }

        private StartScreenPinningRunnableRunnable() {
            this.taskId = -1;
        }

        @Override
        public void run() {
            EventBus.getDefault().send(new ScreenPinningRequestEvent(RecentsTransitionHelper.this.mContext, this.taskId));
        }
    }

    public RecentsTransitionHelper(Context context) {
        this.mContext = context;
    }

    public void launchTaskFromRecents(final TaskStack stack, final Task task, final TaskStackView stackView, final TaskView taskView, final boolean screenPinningRequested, Rect bounds, final int destinationStack) {
        IAppTransitionAnimationSpecsFuture transitionFuture;
        ActivityOptions.OnAnimationStartedListener animStartedListener;
        final ActivityOptions opts = ActivityOptions.makeBasic();
        if (bounds != null) {
            if (bounds.isEmpty()) {
                bounds = null;
            }
            opts.setLaunchBounds(bounds);
        }
        if (taskView != null) {
            transitionFuture = getAppTransitionFuture(new AnimationSpecComposer() {
                @Override
                public List<AppTransitionAnimationSpec> composeSpecs() {
                    return RecentsTransitionHelper.this.composeAnimationSpecs(task, stackView, destinationStack);
                }
            });
            animStartedListener = new ActivityOptions.OnAnimationStartedListener() {
                public void onAnimationStarted() {
                    EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(task));
                    EventBus.getDefault().send(new ExitRecentsWindowFirstAnimationFrameEvent());
                    stackView.cancelAllTaskViewAnimations();
                    if (!screenPinningRequested) {
                        return;
                    }
                    RecentsTransitionHelper.this.mStartScreenPinningRunnable.taskId = task.key.id;
                    RecentsTransitionHelper.this.mHandler.postDelayed(RecentsTransitionHelper.this.mStartScreenPinningRunnable, 350L);
                }
            };
        } else {
            transitionFuture = null;
            animStartedListener = new ActivityOptions.OnAnimationStartedListener() {
                public void onAnimationStarted() {
                    EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(task));
                    EventBus.getDefault().send(new ExitRecentsWindowFirstAnimationFrameEvent());
                    stackView.cancelAllTaskViewAnimations();
                }
            };
        }
        if (taskView == null) {
            startTaskActivity(stack, task, taskView, opts, transitionFuture, animStartedListener);
        } else {
            LaunchTaskStartedEvent launchStartedEvent = new LaunchTaskStartedEvent(taskView, screenPinningRequested);
            if (task.group != null && !task.group.isFrontMostTask(task)) {
                final IAppTransitionAnimationSpecsFuture iAppTransitionAnimationSpecsFuture = transitionFuture;
                final ActivityOptions.OnAnimationStartedListener onAnimationStartedListener = animStartedListener;
                launchStartedEvent.addPostAnimationCallback(new Runnable() {
                    @Override
                    public void run() {
                        RecentsTransitionHelper.this.startTaskActivity(stack, task, taskView, opts, iAppTransitionAnimationSpecsFuture, onAnimationStartedListener);
                    }
                });
                EventBus.getDefault().send(launchStartedEvent);
            } else {
                EventBus.getDefault().send(launchStartedEvent);
                startTaskActivity(stack, task, taskView, opts, transitionFuture, animStartedListener);
            }
        }
        Recents.getSystemServices().sendCloseSystemWindows("homekey");
    }

    public IRemoteCallback wrapStartedListener(final ActivityOptions.OnAnimationStartedListener listener) {
        if (listener == null) {
            return null;
        }
        return new IRemoteCallback.Stub() {
            public void sendResult(Bundle data) throws RemoteException {
                Handler handler = RecentsTransitionHelper.this.mHandler;
                final ActivityOptions.OnAnimationStartedListener onAnimationStartedListener = listener;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onAnimationStartedListener.onAnimationStarted();
                    }
                });
            }
        };
    }

    public void startTaskActivity(TaskStack stack, Task task, TaskView taskView, ActivityOptions opts, IAppTransitionAnimationSpecsFuture transitionFuture, ActivityOptions.OnAnimationStartedListener animStartedListener) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp.startActivityFromRecents(this.mContext, task.key, task.title, opts)) {
            int taskIndexFromFront = 0;
            int taskIndex = stack.indexOfStackTask(task);
            if (taskIndex > -1) {
                taskIndexFromFront = (stack.getTaskCount() - taskIndex) - 1;
            }
            EventBus.getDefault().send(new LaunchTaskSucceededEvent(taskIndexFromFront));
        } else {
            if (taskView != null) {
                taskView.dismissTask();
            }
            EventBus.getDefault().send(new LaunchTaskFailedEvent());
        }
        if (transitionFuture == null) {
            return;
        }
        ssp.overridePendingAppTransitionMultiThumbFuture(transitionFuture, wrapStartedListener(animStartedListener), true);
    }

    public IAppTransitionAnimationSpecsFuture getAppTransitionFuture(final AnimationSpecComposer composer) {
        synchronized (this) {
            this.mAppTransitionAnimationSpecs = SPECS_WAITING;
        }
        return new IAppTransitionAnimationSpecsFuture.Stub() {
            public AppTransitionAnimationSpec[] get() throws RemoteException {
                Handler handler = RecentsTransitionHelper.this.mHandler;
                final AnimationSpecComposer animationSpecComposer = composer;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (RecentsTransitionHelper.this) {
                            RecentsTransitionHelper.this.mAppTransitionAnimationSpecs = animationSpecComposer.composeSpecs();
                            RecentsTransitionHelper.this.notifyAll();
                        }
                    }
                });
                synchronized (RecentsTransitionHelper.this) {
                    while (RecentsTransitionHelper.this.mAppTransitionAnimationSpecs == RecentsTransitionHelper.SPECS_WAITING) {
                        try {
                            RecentsTransitionHelper.this.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    if (RecentsTransitionHelper.this.mAppTransitionAnimationSpecs == null) {
                        return null;
                    }
                    AppTransitionAnimationSpec[] specs = new AppTransitionAnimationSpec[RecentsTransitionHelper.this.mAppTransitionAnimationSpecs.size()];
                    RecentsTransitionHelper.this.mAppTransitionAnimationSpecs.toArray(specs);
                    RecentsTransitionHelper.this.mAppTransitionAnimationSpecs = RecentsTransitionHelper.SPECS_WAITING;
                    return specs;
                }
            }
        };
    }

    public List<AppTransitionAnimationSpec> composeDockAnimationSpec(TaskView taskView, Rect bounds) {
        this.mTmpTransform.fillIn(taskView);
        Task task = taskView.getTask();
        Bitmap thumbnail = composeTaskBitmap(taskView, this.mTmpTransform);
        return Collections.singletonList(new AppTransitionAnimationSpec(task.key.id, thumbnail, bounds));
    }

    public List<AppTransitionAnimationSpec> composeAnimationSpecs(Task task, TaskStackView stackView, int destinationStack) {
        int targetStackId = destinationStack != -1 ? destinationStack : task.key.stackId;
        if (!ActivityManager.StackId.useAnimationSpecForAppTransition(targetStackId)) {
            return null;
        }
        TaskView taskView = stackView.getChildViewForTask(task);
        TaskStackLayoutAlgorithm stackLayout = stackView.getStackAlgorithm();
        Rect offscreenTaskRect = new Rect();
        stackLayout.getFrontOfStackTransform().rect.round(offscreenTaskRect);
        List<AppTransitionAnimationSpec> specs = new ArrayList<>();
        if (targetStackId == 1 || targetStackId == 3 || targetStackId == -1) {
            if (taskView == null) {
                specs.add(composeOffscreenAnimationSpec(task, offscreenTaskRect));
            } else {
                this.mTmpTransform.fillIn(taskView);
                stackLayout.transformToScreenCoordinates(this.mTmpTransform, null);
                AppTransitionAnimationSpec spec = composeAnimationSpec(stackView, taskView, this.mTmpTransform, true);
                if (spec != null) {
                    specs.add(spec);
                }
            }
            return specs;
        }
        TaskStack stack = stackView.getStack();
        ArrayList<Task> tasks = stack.getStackTasks();
        int taskCount = tasks.size();
        for (int i = taskCount - 1; i >= 0; i--) {
            Task t = tasks.get(i);
            if (t.isFreeformTask() || targetStackId == 2) {
                TaskView tv = stackView.getChildViewForTask(t);
                if (tv == null) {
                    specs.add(composeOffscreenAnimationSpec(t, offscreenTaskRect));
                } else {
                    this.mTmpTransform.fillIn(taskView);
                    stackLayout.transformToScreenCoordinates(this.mTmpTransform, null);
                    AppTransitionAnimationSpec spec2 = composeAnimationSpec(stackView, tv, this.mTmpTransform, true);
                    if (spec2 != null) {
                        specs.add(spec2);
                    }
                }
            }
        }
        return specs;
    }

    private static AppTransitionAnimationSpec composeOffscreenAnimationSpec(Task task, Rect taskRect) {
        return new AppTransitionAnimationSpec(task.key.id, (Bitmap) null, taskRect);
    }

    public static Bitmap composeTaskBitmap(TaskView taskView, TaskViewTransform transform) {
        float scale = transform.scale;
        int fromWidth = (int) (transform.rect.width() * scale);
        int fromHeight = (int) (transform.rect.height() * scale);
        if (fromWidth == 0 || fromHeight == 0) {
            Log.e("RecentsTransitionHelper", "Could not compose thumbnail for task: " + taskView.getTask() + " at transform: " + transform);
            Bitmap b = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            b.eraseColor(0);
            return b;
        }
        Bitmap b2 = Bitmap.createBitmap(fromWidth, fromHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b2);
        c.scale(scale, scale);
        taskView.draw(c);
        c.setBitmap(null);
        return b2.createAshmemBitmap();
    }

    private static Bitmap composeHeaderBitmap(TaskView taskView, TaskViewTransform transform) {
        float scale = transform.scale;
        int headerWidth = (int) transform.rect.width();
        int headerHeight = (int) (taskView.mHeaderView.getMeasuredHeight() * scale);
        if (headerWidth == 0 || headerHeight == 0) {
            return null;
        }
        Bitmap b = Bitmap.createBitmap(headerWidth, headerHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.scale(scale, scale);
        taskView.mHeaderView.draw(c);
        c.setBitmap(null);
        return b.createAshmemBitmap();
    }

    private static AppTransitionAnimationSpec composeAnimationSpec(TaskStackView stackView, TaskView taskView, TaskViewTransform transform, boolean addHeaderBitmap) {
        Bitmap b = null;
        if (addHeaderBitmap && (b = composeHeaderBitmap(taskView, transform)) == null) {
            return null;
        }
        Rect taskRect = new Rect();
        transform.rect.round(taskRect);
        if (stackView.getStack().getStackFrontMostTask(false) != taskView.getTask()) {
            taskRect.bottom = taskRect.top + stackView.getMeasuredHeight();
        }
        return new AppTransitionAnimationSpec(taskView.getTask().key.id, b, taskRect);
    }
}
