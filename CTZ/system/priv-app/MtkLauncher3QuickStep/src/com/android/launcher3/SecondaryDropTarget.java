package com.android.launcher3;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Launcher;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.logging.LoggerUtils;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.util.Themes;
import java.net.URISyntaxException;

/* loaded from: classes.dex */
public class SecondaryDropTarget extends ButtonDropTarget implements OnAlarmListener {
    private static final long CACHE_EXPIRE_TIMEOUT = 5000;
    private static final String TAG = "SecondaryDropTarget";
    private final Alarm mCacheExpireAlarm;
    protected int mCurrentAccessibilityAction;
    private final ArrayMap<UserHandle, Boolean> mUninstallDisabledCache;

    public SecondaryDropTarget(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SecondaryDropTarget(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mUninstallDisabledCache = new ArrayMap<>(1);
        this.mCurrentAccessibilityAction = -1;
        this.mCacheExpireAlarm = new Alarm();
        this.mCacheExpireAlarm.setOnAlarmListener(this);
    }

    @Override // com.android.launcher3.ButtonDropTarget, android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        setupUi(R.id.action_uninstall);
    }

    protected void setupUi(int i) {
        if (i == this.mCurrentAccessibilityAction) {
            return;
        }
        this.mCurrentAccessibilityAction = i;
        if (i == R.id.action_uninstall) {
            this.mHoverColor = getResources().getColor(R.color.uninstall_target_hover_tint);
            setDrawable(R.drawable.ic_uninstall_shadow);
            updateText(R.string.uninstall_drop_target_label);
        } else {
            this.mHoverColor = Themes.getColorAccent(getContext());
            setDrawable(R.drawable.ic_setup_shadow);
            updateText(R.string.gadget_setup_text);
        }
    }

    @Override // com.android.launcher3.OnAlarmListener
    public void onAlarm(Alarm alarm) {
        this.mUninstallDisabledCache.clear();
    }

    @Override // com.android.launcher3.ButtonDropTarget
    public int getAccessibilityAction() {
        return this.mCurrentAccessibilityAction;
    }

    @Override // com.android.launcher3.ButtonDropTarget
    public LauncherLogProto.Target getDropTargetForLogging() {
        LauncherLogProto.Target targetNewTarget = LoggerUtils.newTarget(2);
        targetNewTarget.controlType = this.mCurrentAccessibilityAction == R.id.action_uninstall ? 6 : 4;
        return targetNewTarget;
    }

    @Override // com.android.launcher3.ButtonDropTarget
    protected boolean supportsDrop(ItemInfo itemInfo) {
        return supportsAccessibilityDrop(itemInfo, getViewUnderDrag(itemInfo));
    }

    @Override // com.android.launcher3.ButtonDropTarget
    public boolean supportsAccessibilityDrop(ItemInfo itemInfo, View view) {
        if (view instanceof AppWidgetHostView) {
            if (getReconfigurableWidgetId(view) == 0) {
                return false;
            }
            setupUi(R.id.action_reconfigure);
            return true;
        }
        setupUi(R.id.action_uninstall);
        Boolean boolValueOf = this.mUninstallDisabledCache.get(itemInfo.user);
        if (boolValueOf == null) {
            Bundle userRestrictions = ((UserManager) getContext().getSystemService("user")).getUserRestrictions(itemInfo.user);
            boolValueOf = Boolean.valueOf(userRestrictions.getBoolean("no_control_apps", false) || userRestrictions.getBoolean("no_uninstall_apps", false));
            this.mUninstallDisabledCache.put(itemInfo.user, boolValueOf);
        }
        this.mCacheExpireAlarm.setAlarm(CACHE_EXPIRE_TIMEOUT);
        if (boolValueOf.booleanValue()) {
            return false;
        }
        if (itemInfo instanceof ItemInfoWithIcon) {
            ItemInfoWithIcon itemInfoWithIcon = (ItemInfoWithIcon) itemInfo;
            if ((itemInfoWithIcon.runtimeStatusFlags & ItemInfoWithIcon.FLAG_SYSTEM_MASK) != 0) {
                return (itemInfoWithIcon.runtimeStatusFlags & 128) != 0;
            }
        }
        return getUninstallTarget(itemInfo) != null;
    }

    private ComponentName getUninstallTarget(ItemInfo itemInfo) {
        UserHandle userHandle;
        Intent intent;
        LauncherActivityInfo launcherActivityInfoResolveActivity;
        if (itemInfo != null && itemInfo.itemType == 0) {
            intent = itemInfo.getIntent();
            userHandle = itemInfo.user;
        } else {
            userHandle = null;
            intent = null;
        }
        if (intent == null || (launcherActivityInfoResolveActivity = LauncherAppsCompat.getInstance(this.mLauncher).resolveActivity(intent, userHandle)) == null || (launcherActivityInfoResolveActivity.getApplicationInfo().flags & 1) != 0) {
            return null;
        }
        return launcherActivityInfoResolveActivity.getComponentName();
    }

    @Override // com.android.launcher3.ButtonDropTarget, com.android.launcher3.DropTarget
    public void onDrop(DropTarget.DragObject dragObject, DragOptions dragOptions) {
        dragObject.dragSource = new DeferredOnComplete(dragObject.dragSource, getContext());
        super.onDrop(dragObject, dragOptions);
    }

    @Override // com.android.launcher3.ButtonDropTarget
    public void completeDrop(DropTarget.DragObject dragObject) {
        ComponentName componentNamePerformDropAction = performDropAction(getViewUnderDrag(dragObject.dragInfo), dragObject.dragInfo);
        if (dragObject.dragSource instanceof DeferredOnComplete) {
            DeferredOnComplete deferredOnComplete = (DeferredOnComplete) dragObject.dragSource;
            if (componentNamePerformDropAction == null) {
                deferredOnComplete.sendFailure();
            } else {
                deferredOnComplete.mPackageName = componentNamePerformDropAction.getPackageName();
                this.mLauncher.setOnResumeCallback(deferredOnComplete);
            }
        }
    }

    private View getViewUnderDrag(ItemInfo itemInfo) {
        if ((itemInfo instanceof LauncherAppWidgetInfo) && itemInfo.container == -100 && this.mLauncher.getWorkspace().getDragInfo() != null) {
            return this.mLauncher.getWorkspace().getDragInfo().cell;
        }
        return null;
    }

    private int getReconfigurableWidgetId(View view) {
        AppWidgetHostView appWidgetHostView;
        AppWidgetProviderInfo appWidgetInfo;
        if (!(view instanceof AppWidgetHostView) || (appWidgetInfo = (appWidgetHostView = (AppWidgetHostView) view).getAppWidgetInfo()) == null || appWidgetInfo.configure == null || (LauncherAppWidgetProviderInfo.fromProviderInfo(getContext(), appWidgetInfo).getWidgetFeatures() & 1) == 0) {
            return 0;
        }
        return appWidgetHostView.getAppWidgetId();
    }

    protected ComponentName performDropAction(View view, ItemInfo itemInfo) {
        if (this.mCurrentAccessibilityAction == R.id.action_reconfigure) {
            int reconfigurableWidgetId = getReconfigurableWidgetId(view);
            if (reconfigurableWidgetId != 0) {
                this.mLauncher.getAppWidgetHost().startConfigActivity(this.mLauncher, reconfigurableWidgetId, -1);
            }
            return null;
        }
        ComponentName uninstallTarget = getUninstallTarget(itemInfo);
        if (uninstallTarget == null) {
            Toast.makeText(this.mLauncher, R.string.uninstall_system_app_text, 0).show();
            return null;
        }
        try {
            this.mLauncher.startActivity(Intent.parseUri(this.mLauncher.getString(R.string.delete_package_intent), 0).setData(Uri.fromParts("package", uninstallTarget.getPackageName(), uninstallTarget.getClassName())).putExtra("android.intent.extra.USER", itemInfo.user));
            return uninstallTarget;
        } catch (URISyntaxException e) {
            Log.e(TAG, "Failed to parse intent to start uninstall activity for item=" + itemInfo);
            return null;
        }
    }

    @Override // com.android.launcher3.ButtonDropTarget
    public void onAccessibilityDrop(View view, ItemInfo itemInfo) {
        performDropAction(view, itemInfo);
    }

    private class DeferredOnComplete implements DragSource, Launcher.OnResumeCallback {
        private final Context mContext;
        private DropTarget.DragObject mDragObject;
        private final DragSource mOriginal;
        private String mPackageName;

        public DeferredOnComplete(DragSource dragSource, Context context) {
            this.mOriginal = dragSource;
            this.mContext = context;
        }

        @Override // com.android.launcher3.DragSource
        public void onDropCompleted(View view, DropTarget.DragObject dragObject, boolean z) {
            this.mDragObject = dragObject;
        }

        @Override // com.android.launcher3.logging.UserEventDispatcher.LogContainerProvider
        public void fillInLogContainerData(View view, ItemInfo itemInfo, LauncherLogProto.Target target, LauncherLogProto.Target target2) {
            this.mOriginal.fillInLogContainerData(view, itemInfo, target, target2);
        }

        @Override // com.android.launcher3.Launcher.OnResumeCallback
        public void onLauncherResume() {
            if (LauncherAppsCompat.getInstance(this.mContext).getApplicationInfo(this.mPackageName, 8192, this.mDragObject.dragInfo.user) == null) {
                this.mDragObject.dragSource = this.mOriginal;
                this.mOriginal.onDropCompleted(SecondaryDropTarget.this, this.mDragObject, true);
                return;
            }
            sendFailure();
        }

        public void sendFailure() {
            this.mDragObject.dragSource = this.mOriginal;
            this.mDragObject.cancelled = true;
            this.mOriginal.onDropCompleted(SecondaryDropTarget.this, this.mDragObject, false);
        }
    }
}
