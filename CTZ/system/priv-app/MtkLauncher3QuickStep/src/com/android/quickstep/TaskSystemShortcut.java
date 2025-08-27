package com.android.quickstep;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.util.InstantAppResolver;
import com.android.quickstep.TaskSystemShortcut;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskThumbnailView;
import com.android.quickstep.views.TaskView;
import com.android.systemui.shared.recents.ISystemUiProxy;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecCompat;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture;
import com.android.systemui.shared.recents.view.RecentsTransition;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.ActivityOptionsCompat;
import com.android.systemui.shared.system.WindowManagerWrapper;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class TaskSystemShortcut<T extends SystemShortcut> extends SystemShortcut {
    private static final String TAG = "TaskSystemShortcut";
    protected T mSystemShortcut;

    protected TaskSystemShortcut(T t) {
        super(t.iconResId, t.labelResId);
        this.mSystemShortcut = t;
    }

    protected TaskSystemShortcut(int i, int i2) {
        super(i, i2);
    }

    @Override // com.android.launcher3.popup.SystemShortcut
    public View.OnClickListener getOnClickListener(BaseDraggingActivity baseDraggingActivity, ItemInfo itemInfo) {
        return null;
    }

    public View.OnClickListener getOnClickListener(BaseDraggingActivity baseDraggingActivity, TaskView taskView) {
        Task task = taskView.getTask();
        ShortcutInfo shortcutInfo = new ShortcutInfo();
        shortcutInfo.intent = new Intent();
        shortcutInfo.intent.setComponent(task.getTopComponent());
        shortcutInfo.user = UserHandle.of(task.key.userId);
        shortcutInfo.title = TaskUtils.getTitle(baseDraggingActivity, task);
        return getOnClickListenerForTask(baseDraggingActivity, task, shortcutInfo);
    }

    protected View.OnClickListener getOnClickListenerForTask(BaseDraggingActivity baseDraggingActivity, Task task, ItemInfo itemInfo) {
        return this.mSystemShortcut.getOnClickListener(baseDraggingActivity, itemInfo);
    }

    public static class AppInfo extends TaskSystemShortcut<SystemShortcut.AppInfo> {
        public AppInfo() {
            super(new SystemShortcut.AppInfo());
        }
    }

    public static class SplitScreen extends TaskSystemShortcut {
        private Handler mHandler;

        public SplitScreen() {
            super(R.drawable.ic_split_screen, R.string.recent_task_option_split_screen);
            this.mHandler = new Handler(Looper.getMainLooper());
        }

        @Override // com.android.quickstep.TaskSystemShortcut
        public View.OnClickListener getOnClickListener(final BaseDraggingActivity baseDraggingActivity, final TaskView taskView) {
            if (baseDraggingActivity.getDeviceProfile().isMultiWindowMode) {
                return null;
            }
            Task task = taskView.getTask();
            final int i = task.key.id;
            if (!task.isDockable) {
                return null;
            }
            final RecentsView recentsView = (RecentsView) baseDraggingActivity.getOverviewPanel();
            final TaskThumbnailView thumbnail = taskView.getThumbnail();
            return new View.OnClickListener() { // from class: com.android.quickstep.-$$Lambda$TaskSystemShortcut$SplitScreen$iqjICZKgHOyLfMMRAFiQa4a1xFY
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    TaskSystemShortcut.SplitScreen.lambda$getOnClickListener$1(this.f$0, taskView, recentsView, baseDraggingActivity, i, thumbnail, view);
                }
            };
        }

        public static /* synthetic */ void lambda$getOnClickListener$1(SplitScreen splitScreen, final TaskView taskView, final RecentsView recentsView, final BaseDraggingActivity baseDraggingActivity, final int i, TaskThumbnailView taskThumbnailView, View view) {
            final View.OnLayoutChangeListener onLayoutChangeListener = new View.OnLayoutChangeListener() { // from class: com.android.quickstep.TaskSystemShortcut.SplitScreen.1
                @Override // android.view.View.OnLayoutChangeListener
                public void onLayoutChange(View view2, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9) {
                    taskView.getRootView().removeOnLayoutChangeListener(this);
                    recentsView.removeIgnoreResetTask(taskView);
                    recentsView.dismissTask(taskView, false, false);
                }
            };
            DeviceProfile.OnDeviceProfileChangeListener onDeviceProfileChangeListener = new DeviceProfile.OnDeviceProfileChangeListener() { // from class: com.android.quickstep.TaskSystemShortcut.SplitScreen.2
                @Override // com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener
                public void onDeviceProfileChanged(DeviceProfile deviceProfile) {
                    baseDraggingActivity.removeOnDeviceProfileChangeListener(this);
                    if (deviceProfile.isMultiWindowMode) {
                        taskView.getRootView().addOnLayoutChangeListener(onLayoutChangeListener);
                    }
                }
            };
            AbstractFloatingView.closeOpenViews(baseDraggingActivity, true, 399);
            int navBarPosition = WindowManagerWrapper.getInstance().getNavBarPosition();
            if (navBarPosition == -1) {
                return;
            }
            if (ActivityManagerWrapper.getInstance().startActivityFromRecents(i, ActivityOptionsCompat.makeSplitScreenOptions(navBarPosition != 1))) {
                try {
                    RecentsModel.getInstance(baseDraggingActivity).getSystemUiProxy().onSplitScreenInvoked();
                    baseDraggingActivity.getUserEventDispatcher().logActionOnControl(0, 16);
                    baseDraggingActivity.addOnDeviceProfileChangeListener(onDeviceProfileChangeListener);
                    Runnable runnable = new Runnable() { // from class: com.android.quickstep.-$$Lambda$TaskSystemShortcut$SplitScreen$bIIgvqKYPaQItPy-_-PUOmKY14k
                        @Override // java.lang.Runnable
                        public final void run() {
                            TaskSystemShortcut.SplitScreen.lambda$getOnClickListener$0(recentsView, taskView);
                        }
                    };
                    int[] iArr = new int[2];
                    taskThumbnailView.getLocationOnScreen(iArr);
                    final Rect rect = new Rect(iArr[0], iArr[1], iArr[0] + ((int) (taskThumbnailView.getWidth() * taskView.getScaleX())), iArr[1] + ((int) (taskThumbnailView.getHeight() * taskView.getScaleY())));
                    final Bitmap bitmapDrawViewIntoHardwareBitmap = RecentsTransition.drawViewIntoHardwareBitmap(rect.width(), rect.height(), taskThumbnailView, 1.0f, ViewCompat.MEASURED_STATE_MASK);
                    WindowManagerWrapper.getInstance().overridePendingAppTransitionMultiThumbFuture(new AppTransitionAnimationSpecsFuture(splitScreen.mHandler) { // from class: com.android.quickstep.TaskSystemShortcut.SplitScreen.3
                        @Override // com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture
                        public List<AppTransitionAnimationSpecCompat> composeSpecs() {
                            return Collections.singletonList(new AppTransitionAnimationSpecCompat(i, bitmapDrawViewIntoHardwareBitmap, rect));
                        }
                    }, runnable, splitScreen.mHandler, true);
                } catch (RemoteException e) {
                    Log.w(TaskSystemShortcut.TAG, "Failed to notify SysUI of split screen: ", e);
                }
            }
        }

        static /* synthetic */ void lambda$getOnClickListener$0(RecentsView recentsView, TaskView taskView) {
            recentsView.addIgnoreResetTask(taskView);
            taskView.setAlpha(0.0f);
        }
    }

    public static class Pin extends TaskSystemShortcut {
        private static final String TAG = Pin.class.getSimpleName();
        private Handler mHandler;

        public Pin() {
            super(R.drawable.ic_pin, R.string.recent_task_option_pin);
            this.mHandler = new Handler(Looper.getMainLooper());
        }

        @Override // com.android.quickstep.TaskSystemShortcut
        public View.OnClickListener getOnClickListener(BaseDraggingActivity baseDraggingActivity, final TaskView taskView) {
            final ISystemUiProxy systemUiProxy = RecentsModel.getInstance(baseDraggingActivity).getSystemUiProxy();
            if (systemUiProxy == null || !ActivityManagerWrapper.getInstance().isScreenPinningEnabled() || ActivityManagerWrapper.getInstance().isLockToAppActive()) {
                return null;
            }
            return new View.OnClickListener() { // from class: com.android.quickstep.-$$Lambda$TaskSystemShortcut$Pin$CEaWVCwYeYN8KHoUzK4-cdTYR94
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    TaskSystemShortcut.Pin pin = this.f$0;
                    ISystemUiProxy iSystemUiProxy = systemUiProxy;
                    TaskView taskView2 = taskView;
                    taskView2.launchTask(true, new Consumer() { // from class: com.android.quickstep.-$$Lambda$TaskSystemShortcut$Pin$hWIdn00sCsRznD3WJhILU0CGvPQ
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            TaskSystemShortcut.Pin.lambda$getOnClickListener$0(iSystemUiProxy, taskView2, (Boolean) obj);
                        }
                    }, pin.mHandler);
                }
            };
        }

        static /* synthetic */ void lambda$getOnClickListener$0(ISystemUiProxy iSystemUiProxy, TaskView taskView, Boolean bool) {
            if (bool.booleanValue()) {
                try {
                    iSystemUiProxy.startScreenPinning(taskView.getTask().key.id);
                    return;
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to start screen pinning: ", e);
                    return;
                }
            }
            taskView.notifyTaskLaunchFailed(TAG);
        }
    }

    public static class Install extends TaskSystemShortcut<SystemShortcut.Install> {
        public Install() {
            super(new SystemShortcut.Install());
        }

        @Override // com.android.quickstep.TaskSystemShortcut
        protected View.OnClickListener getOnClickListenerForTask(BaseDraggingActivity baseDraggingActivity, Task task, ItemInfo itemInfo) {
            if (InstantAppResolver.newInstance(baseDraggingActivity).isInstantApp(baseDraggingActivity, task.getTopComponent().getPackageName())) {
                return ((SystemShortcut.Install) this.mSystemShortcut).createOnClickListener(baseDraggingActivity, itemInfo);
            }
            return null;
        }
    }
}
