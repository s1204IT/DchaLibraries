package com.android.systemui.tv.pip;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Rect;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.systemui.Prefs;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.statusbar.tv.TvStatusBar;
import java.util.ArrayList;
import java.util.List;

public class PipManager {
    private static PipManager sPipManager;
    private IActivityManager mActivityManager;
    private Context mContext;
    private Rect mCurrentPipBounds;
    private Rect mDefaultPipBounds;
    private boolean mInitialized;
    private String[] mLastPackagesResourceGranted;
    private MediaSessionManager mMediaSessionManager;
    private Rect mMenuModePipBounds;
    private boolean mOnboardingShown;
    private Rect mPipBounds;
    private ComponentName mPipComponentName;
    private MediaController mPipMediaController;
    private PipRecentsOverlayManager mPipRecentsOverlayManager;
    private int mRecentsFocusChangedAnimationDurationMs;
    private Rect mRecentsFocusedPipBounds;
    private Rect mRecentsPipBounds;
    private Rect mSettingsPipBounds;
    private int mSuspendPipResizingReason;
    private static final boolean DEBUG_FORCE_ONBOARDING = SystemProperties.getBoolean("debug.tv.pip_force_onboarding", false);
    private static final List<Pair<String, String>> sSettingsPackageAndClassNamePairList = new ArrayList();
    private int mState = 0;
    private final Handler mHandler = new Handler();
    private List<Listener> mListeners = new ArrayList();
    private List<MediaListener> mMediaListeners = new ArrayList();
    private int mPipTaskId = -1;
    private final Runnable mResizePinnedStackRunnable = new Runnable() {
        @Override
        public void run() {
            PipManager.this.resizePinnedStack(PipManager.this.mState);
        }
    };
    private final Runnable mClosePipRunnable = new Runnable() {
        @Override
        public void run() {
            PipManager.this.closePip();
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!"android.intent.action.MEDIA_RESOURCE_GRANTED".equals(action)) {
                return;
            }
            String[] packageNames = intent.getStringArrayExtra("android.intent.extra.PACKAGES");
            int resourceType = intent.getIntExtra("android.intent.extra.MEDIA_RESOURCE_TYPE", -1);
            if (packageNames == null || packageNames.length <= 0 || resourceType != 0) {
                return;
            }
            PipManager.this.handleMediaResourceGranted(packageNames);
        }
    };
    private final MediaSessionManager.OnActiveSessionsChangedListener mActiveMediaSessionListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
        @Override
        public void onActiveSessionsChanged(List<MediaController> controllers) {
            PipManager.this.updateMediaController(controllers);
        }
    };
    private SystemServicesProxy.TaskStackListener mTaskStackListener = new SystemServicesProxy.TaskStackListener() {
        @Override
        public void onTaskStackChanged() {
            if (PipManager.this.mState != 0) {
                boolean hasPip = false;
                try {
                    ActivityManager.StackInfo stackInfo = PipManager.this.mActivityManager.getStackInfo(4);
                    if (stackInfo == null) {
                        Log.w("PipManager", "There is no pinned stack");
                        PipManager.this.closePipInternal(false);
                        return;
                    }
                    int i = stackInfo.taskIds.length - 1;
                    while (true) {
                        if (i < 0) {
                            break;
                        }
                        if (stackInfo.taskIds[i] != PipManager.this.mPipTaskId) {
                            i--;
                        } else {
                            hasPip = true;
                            break;
                        }
                    }
                    if (!hasPip) {
                        PipManager.this.closePipInternal(true);
                        return;
                    }
                } catch (RemoteException e) {
                    Log.e("PipManager", "getStackInfo failed", e);
                    return;
                }
            }
            if (PipManager.this.mState != 1) {
                return;
            }
            try {
                List<ActivityManager.RunningTaskInfo> runningTasks = PipManager.this.mActivityManager.getTasks(1, 0);
                if (runningTasks == null || runningTasks.size() == 0) {
                    return;
                }
                ActivityManager.RunningTaskInfo topTask = runningTasks.get(0);
                Rect bounds = PipManager.isSettingsShown(topTask.topActivity) ? PipManager.this.mSettingsPipBounds : PipManager.this.mDefaultPipBounds;
                if (PipManager.this.mPipBounds == bounds) {
                    return;
                }
                PipManager.this.mPipBounds = bounds;
                PipManager.this.resizePinnedStack(1);
            } catch (RemoteException e2) {
                Log.d("PipManager", "Failed to detect top activity", e2);
            }
        }

        @Override
        public void onActivityPinned() {
            try {
                ActivityManager.StackInfo stackInfo = PipManager.this.mActivityManager.getStackInfo(4);
                if (stackInfo == null) {
                    Log.w("PipManager", "Cannot find pinned stack");
                    return;
                }
                PipManager.this.mPipTaskId = stackInfo.taskIds[stackInfo.taskIds.length - 1];
                PipManager.this.mPipComponentName = ComponentName.unflattenFromString(stackInfo.taskNames[stackInfo.taskNames.length - 1]);
                PipManager.this.mState = 1;
                PipManager.this.mCurrentPipBounds = PipManager.this.mPipBounds;
                PipManager.this.launchPipOnboardingActivityIfNeeded();
                PipManager.this.mMediaSessionManager.addOnActiveSessionsChangedListener(PipManager.this.mActiveMediaSessionListener, null);
                PipManager.this.updateMediaController(PipManager.this.mMediaSessionManager.getActiveSessions(null));
                if (PipManager.this.mPipRecentsOverlayManager.isRecentsShown()) {
                    PipManager.this.resizePinnedStack(3);
                }
                for (int i = PipManager.this.mListeners.size() - 1; i >= 0; i--) {
                    ((Listener) PipManager.this.mListeners.get(i)).onPipEntered();
                }
                PipManager.this.updatePipVisibility(true);
            } catch (RemoteException e) {
                Log.e("PipManager", "getStackInfo failed", e);
            }
        }

        @Override
        public void onPinnedActivityRestartAttempt() {
            PipManager.this.movePipToFullscreen();
        }

        @Override
        public void onPinnedStackAnimationEnded() {
            switch (PipManager.this.mState) {
                case 1:
                    if (!PipManager.this.mPipRecentsOverlayManager.isRecentsShown()) {
                        PipManager.this.showPipOverlay();
                    } else {
                        PipManager.this.resizePinnedStack(PipManager.this.mState);
                    }
                    break;
                case 2:
                    PipManager.this.showPipMenu();
                    break;
                case 3:
                case 4:
                    PipManager.this.mPipRecentsOverlayManager.addPipRecentsOverlayView();
                    break;
            }
        }
    };

    public interface Listener {
        void onMoveToFullscreen();

        void onPipActivityClosed();

        void onPipEntered();

        void onPipResizeAboutToStart();

        void onShowPipMenu();
    }

    public interface MediaListener {
        void onMediaControllerChanged();
    }

    static {
        sSettingsPackageAndClassNamePairList.add(new Pair<>("com.android.tv.settings", null));
        sSettingsPackageAndClassNamePairList.add(new Pair<>("com.google.android.leanbacklauncher", "com.google.android.leanbacklauncher.settings.HomeScreenSettingsActivity"));
    }

    private PipManager() {
    }

    public void initialize(Context context) {
        if (this.mInitialized) {
            return;
        }
        this.mInitialized = true;
        this.mContext = context;
        Resources res = context.getResources();
        this.mDefaultPipBounds = Rect.unflattenFromString(res.getString(R.string.PERSOSUBSTATE_RUIM_HRPD_PUK_ENTRY));
        this.mSettingsPipBounds = Rect.unflattenFromString(res.getString(com.android.systemui.R.string.pip_settings_bounds));
        this.mMenuModePipBounds = Rect.unflattenFromString(res.getString(com.android.systemui.R.string.pip_menu_bounds));
        this.mRecentsPipBounds = Rect.unflattenFromString(res.getString(com.android.systemui.R.string.pip_recents_bounds));
        this.mRecentsFocusedPipBounds = Rect.unflattenFromString(res.getString(com.android.systemui.R.string.pip_recents_focused_bounds));
        this.mRecentsFocusChangedAnimationDurationMs = res.getInteger(com.android.systemui.R.integer.recents_tv_pip_focus_anim_duration);
        this.mPipBounds = this.mDefaultPipBounds;
        this.mActivityManager = ActivityManagerNative.getDefault();
        SystemServicesProxy.getInstance(context).registerTaskStackListener(this.mTaskStackListener);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_RESOURCE_GRANTED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mOnboardingShown = Prefs.getBoolean(this.mContext, "TvPictureInPictureOnboardingShown", false);
        this.mPipRecentsOverlayManager = new PipRecentsOverlayManager(context);
        this.mMediaSessionManager = (MediaSessionManager) this.mContext.getSystemService("media_session");
    }

    void onConfigurationChanged() {
        this.mPipRecentsOverlayManager.onConfigurationChanged(this.mContext);
    }

    public void showTvPictureInPictureMenu() {
        if (this.mState != 1) {
            return;
        }
        resizePinnedStack(2);
    }

    public void closePip() {
        closePipInternal(true);
    }

    public void closePipInternal(boolean removePipStack) {
        this.mState = 0;
        this.mPipTaskId = -1;
        this.mPipMediaController = null;
        this.mMediaSessionManager.removeOnActiveSessionsChangedListener(this.mActiveMediaSessionListener);
        if (removePipStack) {
            try {
                this.mActivityManager.removeStack(4);
            } catch (RemoteException e) {
                Log.e("PipManager", "removeStack failed", e);
            }
        }
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onPipActivityClosed();
        }
        this.mHandler.removeCallbacks(this.mClosePipRunnable);
        updatePipVisibility(false);
    }

    void movePipToFullscreen() {
        this.mState = 0;
        this.mPipTaskId = -1;
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onMoveToFullscreen();
        }
        resizePinnedStack(this.mState);
    }

    public void showPipOverlay() {
        PipOverlayActivity.showPipOverlay(this.mContext);
    }

    public void suspendPipResizing(int reason) {
        this.mSuspendPipResizingReason |= reason;
    }

    public void resumePipResizing(int reason) {
        if ((this.mSuspendPipResizingReason & reason) == 0) {
            return;
        }
        this.mSuspendPipResizingReason &= ~reason;
        this.mHandler.post(this.mResizePinnedStackRunnable);
    }

    void resizePinnedStack(int state) {
        boolean wasRecentsShown = this.mState == 3 || this.mState == 4;
        this.mState = state;
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onPipResizeAboutToStart();
        }
        if (this.mSuspendPipResizingReason != 0) {
            return;
        }
        switch (this.mState) {
            case 0:
                this.mCurrentPipBounds = null;
                break;
            case 1:
                this.mCurrentPipBounds = this.mPipBounds;
                break;
            case 2:
                this.mCurrentPipBounds = this.mMenuModePipBounds;
                break;
            case 3:
                this.mCurrentPipBounds = this.mRecentsPipBounds;
                break;
            case 4:
                this.mCurrentPipBounds = this.mRecentsFocusedPipBounds;
                break;
            default:
                this.mCurrentPipBounds = this.mPipBounds;
                break;
        }
        int animationDurationMs = -1;
        if (wasRecentsShown) {
            try {
                if (this.mState == 3 || this.mState == 4) {
                    animationDurationMs = this.mRecentsFocusChangedAnimationDurationMs;
                }
            } catch (RemoteException e) {
                Log.e("PipManager", "resizeStack failed", e);
                return;
            }
        }
        this.mActivityManager.resizeStack(4, this.mCurrentPipBounds, true, true, true, animationDurationMs);
    }

    public Rect getRecentsFocusedPipBounds() {
        return this.mRecentsFocusedPipBounds;
    }

    public void showPipMenu() {
        if (this.mPipRecentsOverlayManager.isRecentsShown()) {
            return;
        }
        this.mState = 2;
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            this.mListeners.get(i).onShowPipMenu();
        }
        Intent intent = new Intent(this.mContext, (Class<?>) PipMenuActivity.class);
        intent.setFlags(268435456);
        this.mContext.startActivity(intent);
    }

    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.mListeners.remove(listener);
    }

    public void addMediaListener(MediaListener listener) {
        this.mMediaListeners.add(listener);
    }

    public void removeMediaListener(MediaListener listener) {
        this.mMediaListeners.remove(listener);
    }

    public void launchPipOnboardingActivityIfNeeded() {
        if (!DEBUG_FORCE_ONBOARDING && this.mOnboardingShown) {
            return;
        }
        this.mOnboardingShown = true;
        Prefs.putBoolean(this.mContext, "TvPictureInPictureOnboardingShown", true);
        Intent intent = new Intent(this.mContext, (Class<?>) PipOnboardingActivity.class);
        intent.setFlags(268435456);
        this.mContext.startActivity(intent);
    }

    public boolean isPipShown() {
        return this.mState != 0;
    }

    public void handleMediaResourceGranted(String[] packageNames) {
        if (this.mState == 0) {
            this.mLastPackagesResourceGranted = packageNames;
            return;
        }
        boolean requestedFromLastPackages = false;
        if (this.mLastPackagesResourceGranted != null) {
            for (String packageName : this.mLastPackagesResourceGranted) {
                int length = packageNames.length;
                int i = 0;
                while (true) {
                    if (i < length) {
                        String newPackageName = packageNames[i];
                        if (!TextUtils.equals(newPackageName, packageName)) {
                            i++;
                        } else {
                            requestedFromLastPackages = true;
                            break;
                        }
                    }
                }
            }
        }
        this.mLastPackagesResourceGranted = packageNames;
        if (requestedFromLastPackages) {
            return;
        }
        closePip();
    }

    public void updateMediaController(List<MediaController> controllers) {
        MediaController mediaController = null;
        if (controllers != null && this.mState != 0 && this.mPipComponentName != null) {
            int i = controllers.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                MediaController controller = controllers.get(i);
                if (controller.getPackageName().equals(this.mPipComponentName.getPackageName())) {
                    mediaController = controller;
                    break;
                }
                i--;
            }
        }
        if (this.mPipMediaController == mediaController) {
            return;
        }
        this.mPipMediaController = mediaController;
        for (int i2 = this.mMediaListeners.size() - 1; i2 >= 0; i2--) {
            this.mMediaListeners.get(i2).onMediaControllerChanged();
        }
        if (this.mPipMediaController == null) {
            this.mHandler.postDelayed(this.mClosePipRunnable, 3000L);
        } else {
            this.mHandler.removeCallbacks(this.mClosePipRunnable);
        }
    }

    MediaController getMediaController() {
        return this.mPipMediaController;
    }

    int getPlaybackState() {
        if (this.mPipMediaController == null || this.mPipMediaController.getPlaybackState() == null) {
            return 2;
        }
        int state = this.mPipMediaController.getPlaybackState().getState();
        boolean isPlaying = state == 6 || state == 8 || state == 3 || state == 4 || state == 5 || state == 9 || state == 10;
        long actions = this.mPipMediaController.getPlaybackState().getActions();
        if (isPlaying || (4 & actions) == 0) {
            return (!isPlaying || (2 & actions) == 0) ? 2 : 0;
        }
        return 1;
    }

    public static boolean isSettingsShown(ComponentName topActivity) {
        String className;
        for (Pair<String, String> componentName : sSettingsPackageAndClassNamePairList) {
            if (topActivity.getPackageName().equals(componentName.first) && ((className = (String) componentName.second) == null || topActivity.getClassName().equals(className))) {
                return true;
            }
        }
        return false;
    }

    public static PipManager getInstance() {
        if (sPipManager == null) {
            sPipManager = new PipManager();
        }
        return sPipManager;
    }

    public PipRecentsOverlayManager getPipRecentsOverlayManager() {
        return this.mPipRecentsOverlayManager;
    }

    public void updatePipVisibility(boolean visible) {
        TvStatusBar statusBar = (TvStatusBar) ((SystemUIApplication) this.mContext).getComponent(TvStatusBar.class);
        if (statusBar == null) {
            return;
        }
        statusBar.updatePipVisibility(visible);
    }
}
