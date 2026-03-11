package com.android.quickstep;

import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.RectF;
import android.os.UserHandle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.util.ComponentKey;
import com.android.quickstep.TaskUtils;
import com.android.quickstep.util.ClipAnimationHelper;
import com.android.quickstep.util.MultiValueUpdateListener;
import com.android.quickstep.util.RemoteAnimationTargetSet;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import com.android.systemui.shared.system.TransactionCompat;
import java.util.List;
import java.util.function.BiConsumer;

public class TaskUtils {
    private static final String TAG = "TaskUtils";

    public static CharSequence getTitle(Context context, Task task) {
        LauncherAppsCompat launcherAppsCompat = LauncherAppsCompat.getInstance(context);
        UserManagerCompat userManagerCompat = UserManagerCompat.getInstance(context);
        PackageManager packageManager = context.getPackageManager();
        UserHandle userHandleOf = UserHandle.of(task.key.userId);
        ApplicationInfo applicationInfo = launcherAppsCompat.getApplicationInfo(task.getTopComponent().getPackageName(), 0, userHandleOf);
        if (applicationInfo == null) {
            Log.e(TAG, "Failed to get title for task " + task);
            return "";
        }
        return userManagerCompat.getBadgedLabelForUser(applicationInfo.loadLabel(packageManager), userHandleOf);
    }

    public static ComponentKey getComponentKeyForTask(Task.TaskKey taskKey) {
        return new ComponentKey(taskKey.getComponent(), UserHandle.of(taskKey.userId));
    }

    public static TaskView findTaskViewToLaunch(BaseDraggingActivity baseDraggingActivity, View view, RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr) {
        int i;
        TaskView taskView;
        if (view instanceof TaskView) {
            return (TaskView) view;
        }
        RecentsView recentsView = (RecentsView) baseDraggingActivity.getOverviewPanel();
        int i2 = 0;
        if (view.getTag() instanceof ItemInfo) {
            ItemInfo itemInfo = (ItemInfo) view.getTag();
            ComponentName targetComponent = itemInfo.getTargetComponent();
            int identifier = itemInfo.user.getIdentifier();
            if (targetComponent != null) {
                for (int i3 = 0; i3 < recentsView.getChildCount(); i3++) {
                    TaskView pageAt = recentsView.getPageAt(i3);
                    if (recentsView.isTaskViewVisible(pageAt)) {
                        Task.TaskKey taskKey = pageAt.getTask().key;
                        if (targetComponent.equals(taskKey.getComponent()) && identifier == taskKey.userId) {
                            return pageAt;
                        }
                    }
                }
            }
        }
        if (remoteAnimationTargetCompatArr == null) {
            return null;
        }
        int length = remoteAnimationTargetCompatArr.length;
        while (true) {
            if (i2 < length) {
                RemoteAnimationTargetCompat remoteAnimationTargetCompat = remoteAnimationTargetCompatArr[i2];
                if (remoteAnimationTargetCompat.mode != 0) {
                    i2++;
                } else {
                    i = remoteAnimationTargetCompat.taskId;
                    break;
                }
            } else {
                i = -1;
                break;
            }
        }
        if (i == -1 || (taskView = recentsView.getTaskView(i)) == null || !recentsView.isTaskViewVisible(taskView)) {
            return null;
        }
        return taskView;
    }

    public static ValueAnimator getRecentsWindowAnimator(TaskView taskView, boolean z, RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, ClipAnimationHelper clipAnimationHelper) {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.setInterpolator(Interpolators.TOUCH_RESPONSE_INTERPOLATOR);
        valueAnimatorOfFloat.addUpdateListener(new AnonymousClass1(remoteAnimationTargetCompatArr, clipAnimationHelper, z, taskView));
        return valueAnimatorOfFloat;
    }

    class AnonymousClass1 extends MultiValueUpdateListener {
        private long mFrameNumber;
        private Surface mSurface;
        final RemoteAnimationTargetSet mTargetSet;
        final RectF mThumbnailRect;
        final ClipAnimationHelper val$inOutHelper;
        final boolean val$skipViewChanges;
        final RemoteAnimationTargetCompat[] val$targets;
        final TaskView val$v;
        final MultiValueUpdateListener.FloatProp mViewAlpha = new MultiValueUpdateListener.FloatProp(1.0f, 0.0f, 75.0f, 75.0f, Interpolators.LINEAR);
        final MultiValueUpdateListener.FloatProp mTaskAlpha = new MultiValueUpdateListener.FloatProp(0.0f, 1.0f, 0.0f, 75.0f, Interpolators.LINEAR);

        AnonymousClass1(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, ClipAnimationHelper clipAnimationHelper, boolean z, TaskView taskView) {
            this.val$targets = remoteAnimationTargetCompatArr;
            this.val$inOutHelper = clipAnimationHelper;
            this.val$skipViewChanges = z;
            this.val$v = taskView;
            this.mTargetSet = new RemoteAnimationTargetSet(this.val$targets, 0);
            ClipAnimationHelper clipAnimationHelper2 = this.val$inOutHelper;
            final boolean z2 = this.val$skipViewChanges;
            clipAnimationHelper2.setTaskTransformCallback(new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    TaskUtils.AnonymousClass1.lambda$new$0(this.f$0, z2, (TransactionCompat) obj, (RemoteAnimationTargetCompat) obj2);
                }
            });
            this.val$inOutHelper.prepareAnimation(true);
            this.val$inOutHelper.fromTaskThumbnailView(this.val$v.getThumbnail(), (RecentsView) this.val$v.getParent(), this.mTargetSet.apps.length == 0 ? null : this.mTargetSet.apps[0]);
            this.mThumbnailRect = new RectF(this.val$inOutHelper.getTargetRect());
            this.mThumbnailRect.offset(-this.val$v.getTranslationX(), -this.val$v.getTranslationY());
            Utilities.scaleRectFAboutCenter(this.mThumbnailRect, 1.0f / this.val$v.getScaleX());
        }

        public static void lambda$new$0(AnonymousClass1 anonymousClass1, boolean z, TransactionCompat transactionCompat, RemoteAnimationTargetCompat remoteAnimationTargetCompat) {
            transactionCompat.setAlpha(remoteAnimationTargetCompat.leash, anonymousClass1.mTaskAlpha.value);
            if (!z) {
                transactionCompat.deferTransactionUntil(remoteAnimationTargetCompat.leash, anonymousClass1.mSurface, anonymousClass1.mFrameNumber);
            }
        }

        @Override
        public void onUpdate(float f) {
            this.mSurface = com.android.systemui.shared.recents.utilities.Utilities.getSurface(this.val$v);
            this.mFrameNumber = this.mSurface != null ? com.android.systemui.shared.recents.utilities.Utilities.getNextFrameNumber(this.mSurface) : -1L;
            if (this.mFrameNumber == -1) {
                Log.w(TaskUtils.TAG, "Failed to animate, surface got destroyed.");
                return;
            }
            RectF rectFApplyTransform = this.val$inOutHelper.applyTransform(this.mTargetSet, 1.0f - f);
            if (!this.val$skipViewChanges) {
                float fWidth = rectFApplyTransform.width() / this.mThumbnailRect.width();
                this.val$v.setScaleX(fWidth);
                this.val$v.setScaleY(fWidth);
                this.val$v.setTranslationX(rectFApplyTransform.centerX() - this.mThumbnailRect.centerX());
                this.val$v.setTranslationY(rectFApplyTransform.centerY() - this.mThumbnailRect.centerY());
                this.val$v.setAlpha(this.mViewAlpha.value);
            }
        }
    }

    public static boolean taskIsATargetWithMode(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, int i, int i2) {
        for (RemoteAnimationTargetCompat remoteAnimationTargetCompat : remoteAnimationTargetCompatArr) {
            if (remoteAnimationTargetCompat.mode == i2 && remoteAnimationTargetCompat.taskId == i) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkCurrentOrManagedUserId(int i, Context context) {
        if (i == UserHandle.myUserId()) {
            return true;
        }
        List<UserHandle> userProfiles = UserManagerCompat.getInstance(context).getUserProfiles();
        for (int size = userProfiles.size() - 1; size >= 0; size--) {
            if (i == userProfiles.get(size).getIdentifier()) {
                return true;
            }
        }
        return false;
    }
}
