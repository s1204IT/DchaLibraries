package com.android.launcher3;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Process;
import android.os.StrictMode;
import android.os.UserHandle;
import android.util.Log;
import android.view.ActionMode;
import android.view.View;
import android.widget.Toast;
import com.android.launcher3.badge.BadgeInfo;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.uioverrides.DisplayRotationListener;
import com.android.launcher3.uioverrides.WallpaperColorInfo;
import com.android.launcher3.views.BaseDragLayer;

public abstract class BaseDraggingActivity extends BaseActivity implements WallpaperColorInfo.OnChangeListener {
    public static final Object AUTO_CANCEL_ACTION_MODE = new Object();
    public static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION = "com.android.launcher3.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";
    private static final String TAG = "BaseDraggingActivity";
    private ActionMode mCurrentActionMode;
    protected boolean mIsSafeModeEnabled;
    private OnStartCallback mOnStartCallback;
    private DisplayRotationListener mRotationListener;
    private int mThemeRes = R.style.LauncherTheme;

    public interface OnStartCallback<T extends BaseDraggingActivity> {
        void onActivityStart(T t);
    }

    public abstract ActivityOptions getActivityLaunchOptions(View view);

    public abstract BadgeInfo getBadgeInfoForItem(ItemInfo itemInfo);

    public abstract BaseDragLayer getDragLayer();

    public abstract <T extends View> T getOverviewPanel();

    public abstract View getRootView();

    public abstract void invalidateParent(ItemInfo itemInfo);

    protected abstract void reapplyUi();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mIsSafeModeEnabled = getPackageManager().isSafeMode();
        this.mRotationListener = new DisplayRotationListener(this, new Runnable() {
            @Override
            public final void run() {
                this.f$0.onDeviceRotationChanged();
            }
        });
        WallpaperColorInfo wallpaperColorInfo = WallpaperColorInfo.getInstance(this);
        wallpaperColorInfo.addOnChangeListener(this);
        int themeRes = getThemeRes(wallpaperColorInfo);
        if (themeRes != this.mThemeRes) {
            this.mThemeRes = themeRes;
            setTheme(themeRes);
        }
    }

    @Override
    public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
        if (this.mThemeRes != getThemeRes(wallpaperColorInfo)) {
            recreate();
        }
    }

    protected int getThemeRes(WallpaperColorInfo wallpaperColorInfo) {
        if (wallpaperColorInfo.isDark()) {
            if (wallpaperColorInfo.supportsDarkText()) {
                return 2131886093;
            }
            return R.style.LauncherThemeDark;
        }
        if (wallpaperColorInfo.supportsDarkText()) {
            return 2131886091;
        }
        return R.style.LauncherTheme;
    }

    @Override
    public void onActionModeStarted(ActionMode actionMode) {
        super.onActionModeStarted(actionMode);
        this.mCurrentActionMode = actionMode;
    }

    @Override
    public void onActionModeFinished(ActionMode actionMode) {
        super.onActionModeFinished(actionMode);
        this.mCurrentActionMode = null;
    }

    public boolean finishAutoCancelActionMode() {
        if (this.mCurrentActionMode != null && AUTO_CANCEL_ACTION_MODE == this.mCurrentActionMode.getTag()) {
            this.mCurrentActionMode.finish();
            return true;
        }
        return false;
    }

    public static BaseDraggingActivity fromContext(Context context) {
        if (context instanceof BaseDraggingActivity) {
            return (BaseDraggingActivity) context;
        }
        return (BaseDraggingActivity) ((ContextWrapper) context).getBaseContext();
    }

    public Rect getViewBounds(View view) {
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        return new Rect(iArr[0], iArr[1], iArr[0] + view.getWidth(), iArr[1] + view.getHeight());
    }

    public final Bundle getActivityLaunchOptionsAsBundle(View view) {
        ActivityOptions activityLaunchOptions = getActivityLaunchOptions(view);
        if (activityLaunchOptions == null) {
            return null;
        }
        return activityLaunchOptions.toBundle();
    }

    public boolean startActivitySafely(View view, Intent intent, ItemInfo itemInfo) {
        Bundle activityLaunchOptionsAsBundle;
        if (this.mIsSafeModeEnabled && !Utilities.isSystemApp(this, intent)) {
            Toast.makeText(this, R.string.safemode_shortcut_error, 0).show();
            return false;
        }
        if ((view == null || intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION)) ? false : true) {
            activityLaunchOptionsAsBundle = getActivityLaunchOptionsAsBundle(view);
        } else {
            activityLaunchOptionsAsBundle = null;
        }
        UserHandle userHandle = itemInfo != null ? itemInfo.user : null;
        intent.addFlags(268435456);
        if (view != null) {
            intent.setSourceBounds(getViewBounds(view));
        }
        try {
            if (Utilities.ATLEAST_MARSHMALLOW && (itemInfo instanceof ShortcutInfo) && (itemInfo.itemType == 1 || itemInfo.itemType == 6) && !((ShortcutInfo) itemInfo).isPromise()) {
                startShortcutIntentSafely(intent, activityLaunchOptionsAsBundle, itemInfo);
            } else if (userHandle == null || userHandle.equals(Process.myUserHandle())) {
                startActivity(intent, activityLaunchOptionsAsBundle);
            } else {
                LauncherAppsCompat.getInstance(this).startActivityForProfile(intent.getComponent(), userHandle, intent.getSourceBounds(), activityLaunchOptionsAsBundle);
            }
            getUserEventDispatcher().logAppLaunch(view, intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            Log.e(TAG, "Unable to launch. tag=" + itemInfo + " intent=" + intent, e);
            return false;
        }
    }

    private void startShortcutIntentSafely(Intent intent, Bundle bundle, ItemInfo itemInfo) {
        try {
            StrictMode.VmPolicy vmPolicy = StrictMode.getVmPolicy();
            try {
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
                if (itemInfo.itemType == 6) {
                    String deepShortcutId = ((ShortcutInfo) itemInfo).getDeepShortcutId();
                    DeepShortcutManager.getInstance(this).startShortcut(intent.getPackage(), deepShortcutId, intent.getSourceBounds(), bundle, itemInfo.user);
                } else {
                    startActivity(intent, bundle);
                }
                StrictMode.setVmPolicy(vmPolicy);
            } catch (Throwable th) {
                StrictMode.setVmPolicy(vmPolicy);
                throw th;
            }
        } catch (SecurityException e) {
            if (!onErrorStartingShortcut(intent, itemInfo)) {
                throw e;
            }
        }
    }

    protected boolean onErrorStartingShortcut(Intent intent, ItemInfo itemInfo) {
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (this.mOnStartCallback != null) {
            this.mOnStartCallback.onActivityStart(this);
            this.mOnStartCallback = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WallpaperColorInfo.getInstance(this).removeOnChangeListener(this);
        this.mRotationListener.disable();
    }

    public <T extends BaseDraggingActivity> void setOnStartCallback(OnStartCallback<T> onStartCallback) {
        this.mOnStartCallback = onStartCallback;
    }

    protected void onDeviceProfileInitiated() {
        if (this.mDeviceProfile.isVerticalBarLayout()) {
            this.mRotationListener.enable();
            this.mDeviceProfile.updateIsSeascape(getWindowManager());
        } else {
            this.mRotationListener.disable();
        }
    }

    private void onDeviceRotationChanged() {
        if (this.mDeviceProfile.updateIsSeascape(getWindowManager())) {
            reapplyUi();
        }
    }
}
