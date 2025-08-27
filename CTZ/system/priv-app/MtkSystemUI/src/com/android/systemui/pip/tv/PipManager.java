package com.android.systemui.pip.tv;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ParceledListSlice;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Debug;
import android.os.Handler;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.IPinnedStackController;
import android.view.IPinnedStackListener;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.systemui.R;
import com.android.systemui.pip.BasePipManager;
import com.android.systemui.pip.tv.PipManager;
import com.android.systemui.recents.misc.SysUiTaskStackChangeListener;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class PipManager implements BasePipManager {
    static final boolean DEBUG = Log.isLoggable("PipManager", 3);
    private static PipManager sPipManager;
    private static List<Pair<String, String>> sSettingsPackageAndClassNamePairList;
    private IActivityManager mActivityManager;
    private Context mContext;
    private Rect mCurrentPipBounds;
    private ParceledListSlice mCustomActions;
    private int mImeHeightAdjustment;
    private boolean mImeVisible;
    private boolean mInitialized;
    private String[] mLastPackagesResourceGranted;
    private MediaSessionManager mMediaSessionManager;
    private Rect mMenuModePipBounds;
    private Rect mPipBounds;
    private ComponentName mPipComponentName;
    private MediaController mPipMediaController;
    private PipNotification mPipNotification;
    private Rect mSettingsPipBounds;
    private int mSuspendPipResizingReason;
    private IWindowManager mWindowManager;
    private int mState = 0;
    private int mResumeResizePinnedStackRunnableState = 0;
    private final Handler mHandler = new Handler();
    private List<Listener> mListeners = new ArrayList();
    private List<MediaListener> mMediaListeners = new ArrayList();
    private Rect mDefaultPipBounds = new Rect();
    private int mLastOrientation = 0;
    private int mPipTaskId = -1;
    private int mPinnedStackId = -1;
    private final PinnedStackListener mPinnedStackListener = new PinnedStackListener();
    private final Runnable mResizePinnedStackRunnable = new Runnable() { // from class: com.android.systemui.pip.tv.PipManager.1
        @Override // java.lang.Runnable
        public void run() {
            PipManager.this.resizePinnedStack(PipManager.this.mResumeResizePinnedStackRunnableState);
        }
    };
    private final Runnable mClosePipRunnable = new Runnable() { // from class: com.android.systemui.pip.tv.PipManager.2
        @Override // java.lang.Runnable
        public void run() {
            PipManager.this.closePip();
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.systemui.pip.tv.PipManager.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.MEDIA_RESOURCE_GRANTED".equals(intent.getAction())) {
                String[] stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.PACKAGES");
                int intExtra = intent.getIntExtra("android.intent.extra.MEDIA_RESOURCE_TYPE", -1);
                if (stringArrayExtra != null && stringArrayExtra.length > 0 && intExtra == 0) {
                    PipManager.this.handleMediaResourceGranted(stringArrayExtra);
                }
            }
        }
    };
    private final MediaSessionManager.OnActiveSessionsChangedListener mActiveMediaSessionListener = new MediaSessionManager.OnActiveSessionsChangedListener() { // from class: com.android.systemui.pip.tv.PipManager.4
        @Override // android.media.session.MediaSessionManager.OnActiveSessionsChangedListener
        public void onActiveSessionsChanged(List<MediaController> list) {
            PipManager.this.updateMediaController(list);
        }
    };
    private SysUiTaskStackChangeListener mTaskStackListener = new SysUiTaskStackChangeListener() { // from class: com.android.systemui.pip.tv.PipManager.5
        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onTaskStackChanged() {
            if (PipManager.DEBUG) {
                Log.d("PipManager", "onTaskStackChanged()");
            }
            if (PipManager.this.getState() != 0) {
                ActivityManager.StackInfo pinnedStackInfo = PipManager.this.getPinnedStackInfo();
                boolean z = false;
                if (pinnedStackInfo == null || pinnedStackInfo.taskIds == null) {
                    Log.w("PipManager", "There is nothing in pinned stack");
                    PipManager.this.closePipInternal(false);
                    return;
                }
                int length = pinnedStackInfo.taskIds.length - 1;
                while (true) {
                    if (length < 0) {
                        break;
                    }
                    if (pinnedStackInfo.taskIds[length] != PipManager.this.mPipTaskId) {
                        length--;
                    } else {
                        z = true;
                        break;
                    }
                }
                if (!z) {
                    PipManager.this.closePipInternal(true);
                    return;
                }
            }
            if (PipManager.this.getState() == 1) {
                Rect rect = PipManager.this.isSettingsShown() ? PipManager.this.mSettingsPipBounds : PipManager.this.mDefaultPipBounds;
                if (PipManager.this.mPipBounds != rect) {
                    PipManager.this.mPipBounds = rect;
                    PipManager.this.resizePinnedStack(1);
                }
            }
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onActivityPinned(String str, int i, int i2, int i3) {
            if (PipManager.DEBUG) {
                Log.d("PipManager", "onActivityPinned()");
            }
            ActivityManager.StackInfo pinnedStackInfo = PipManager.this.getPinnedStackInfo();
            if (pinnedStackInfo == null) {
                Log.w("PipManager", "Cannot find pinned stack");
                return;
            }
            if (PipManager.DEBUG) {
                Log.d("PipManager", "PINNED_STACK:" + pinnedStackInfo);
            }
            PipManager.this.mPinnedStackId = pinnedStackInfo.stackId;
            PipManager.this.mPipTaskId = pinnedStackInfo.taskIds[pinnedStackInfo.taskIds.length - 1];
            PipManager.this.mPipComponentName = ComponentName.unflattenFromString(pinnedStackInfo.taskNames[pinnedStackInfo.taskNames.length - 1]);
            PipManager.this.mState = 1;
            PipManager.this.mCurrentPipBounds = PipManager.this.mPipBounds;
            PipManager.this.mMediaSessionManager.addOnActiveSessionsChangedListener(PipManager.this.mActiveMediaSessionListener, null);
            PipManager.this.updateMediaController(PipManager.this.mMediaSessionManager.getActiveSessions(null));
            for (int size = PipManager.this.mListeners.size() - 1; size >= 0; size--) {
                ((Listener) PipManager.this.mListeners.get(size)).onPipEntered();
            }
            PipManager.this.updatePipVisibility(true);
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onPinnedActivityRestartAttempt(boolean z) {
            if (PipManager.DEBUG) {
                Log.d("PipManager", "onPinnedActivityRestartAttempt()");
            }
            PipManager.this.movePipToFullscreen();
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onPinnedStackAnimationEnded() {
            if (PipManager.DEBUG) {
                Log.d("PipManager", "onPinnedStackAnimationEnded()");
            }
            if (PipManager.this.getState() == 2) {
                PipManager.this.showPipMenu();
            }
        }
    };

    public interface Listener {
        void onMoveToFullscreen();

        void onPipActivityClosed();

        void onPipEntered();

        void onPipMenuActionsChanged(ParceledListSlice parceledListSlice);

        void onPipResizeAboutToStart();

        void onShowPipMenu();
    }

    public interface MediaListener {
        void onMediaControllerChanged();
    }

    private class PinnedStackListener extends IPinnedStackListener.Stub {
        private PinnedStackListener() {
        }

        public void onListenerRegistered(IPinnedStackController iPinnedStackController) {
        }

        public void onImeVisibilityChanged(boolean z, int i) {
            if (PipManager.this.mState == 1 && PipManager.this.mImeVisible != z) {
                if (z) {
                    PipManager.this.mPipBounds.offset(0, -i);
                    PipManager.this.mImeHeightAdjustment = i;
                } else {
                    PipManager.this.mPipBounds.offset(0, PipManager.this.mImeHeightAdjustment);
                }
                PipManager.this.mImeVisible = z;
                PipManager.this.resizePinnedStack(1);
            }
        }

        public void onShelfVisibilityChanged(boolean z, int i) {
        }

        public void onMinimizedStateChanged(boolean z) {
        }

        public void onMovementBoundsChanged(Rect rect, final Rect rect2, Rect rect3, boolean z, boolean z2, int i) {
            PipManager.this.mHandler.post(new Runnable() { // from class: com.android.systemui.pip.tv.-$$Lambda$PipManager$PinnedStackListener$_5-G38rv0jyzaZL9eAwuaWlTGU4
                @Override // java.lang.Runnable
                public final void run() {
                    PipManager.this.mDefaultPipBounds.set(rect2);
                }
            });
        }

        public void onActionsChanged(ParceledListSlice parceledListSlice) {
            PipManager.this.mCustomActions = parceledListSlice;
            PipManager.this.mHandler.post(new Runnable() { // from class: com.android.systemui.pip.tv.-$$Lambda$PipManager$PinnedStackListener$KRSqnvGtvsFkEwCqcSExZLuYv1k
                @Override // java.lang.Runnable
                public final void run() {
                    PipManager.PinnedStackListener.lambda$onActionsChanged$1(this.f$0);
                }
            });
        }

        public static /* synthetic */ void lambda$onActionsChanged$1(PinnedStackListener pinnedStackListener) {
            for (int size = PipManager.this.mListeners.size() - 1; size >= 0; size--) {
                ((Listener) PipManager.this.mListeners.get(size)).onPipMenuActionsChanged(PipManager.this.mCustomActions);
            }
        }
    }

    private PipManager() {
    }

    @Override // com.android.systemui.pip.BasePipManager
    public void initialize(Context context) throws Resources.NotFoundException {
        if (this.mInitialized) {
            return;
        }
        this.mInitialized = true;
        this.mContext = context;
        this.mActivityManager = ActivityManager.getService();
        this.mWindowManager = WindowManagerGlobal.getWindowManagerService();
        ActivityManagerWrapper.getInstance().registerTaskStackListener(this.mTaskStackListener);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_RESOURCE_GRANTED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        if (sSettingsPackageAndClassNamePairList == null) {
            String[] stringArray = this.mContext.getResources().getStringArray(R.array.tv_pip_settings_class_name);
            sSettingsPackageAndClassNamePairList = new ArrayList();
            if (stringArray != null) {
                for (int i = 0; i < stringArray.length; i++) {
                    String[] strArrSplit = stringArray[i].split("/");
                    Pair<String, String> pairCreate = null;
                    switch (strArrSplit.length) {
                        case 1:
                            pairCreate = Pair.create(strArrSplit[0], null);
                            break;
                        case 2:
                            if (strArrSplit[1] != null && strArrSplit[1].startsWith(".")) {
                                pairCreate = Pair.create(strArrSplit[0], strArrSplit[0] + strArrSplit[1]);
                                break;
                            }
                            break;
                    }
                    if (pairCreate != null) {
                        sSettingsPackageAndClassNamePairList.add(pairCreate);
                    } else {
                        Log.w("PipManager", "Ignoring malformed settings name " + stringArray[i]);
                    }
                }
            }
        }
        Configuration configuration = this.mContext.getResources().getConfiguration();
        this.mLastOrientation = configuration.orientation;
        loadConfigurationsAndApply(configuration);
        this.mMediaSessionManager = (MediaSessionManager) this.mContext.getSystemService("media_session");
        try {
            this.mWindowManager.registerPinnedStackListener(0, this.mPinnedStackListener);
        } catch (RemoteException e) {
            Log.e("PipManager", "Failed to register pinned stack listener", e);
        }
        this.mPipNotification = new PipNotification(context);
    }

    private void loadConfigurationsAndApply(Configuration configuration) {
        if (this.mLastOrientation != configuration.orientation) {
            this.mLastOrientation = configuration.orientation;
            return;
        }
        Resources resources = this.mContext.getResources();
        this.mSettingsPipBounds = Rect.unflattenFromString(resources.getString(R.string.pip_settings_bounds));
        this.mMenuModePipBounds = Rect.unflattenFromString(resources.getString(R.string.pip_menu_bounds));
        this.mPipBounds = isSettingsShown() ? this.mSettingsPipBounds : this.mDefaultPipBounds;
        resizePinnedStack(getPinnedStackInfo() == null ? 0 : 1);
    }

    @Override // com.android.systemui.pip.BasePipManager
    public void onConfigurationChanged(Configuration configuration) {
        loadConfigurationsAndApply(configuration);
        this.mPipNotification.onConfigurationChanged(this.mContext);
    }

    @Override // com.android.systemui.pip.BasePipManager
    public void showPictureInPictureMenu() {
        if (getState() == 1) {
            resizePinnedStack(2);
        }
    }

    public void closePip() {
        closePipInternal(true);
    }

    private void closePipInternal(boolean z) {
        this.mState = 0;
        this.mPipTaskId = -1;
        this.mPipMediaController = null;
        this.mMediaSessionManager.removeOnActiveSessionsChangedListener(this.mActiveMediaSessionListener);
        if (z) {
            try {
                try {
                    this.mActivityManager.removeStack(this.mPinnedStackId);
                } catch (RemoteException e) {
                    Log.e("PipManager", "removeStack failed", e);
                }
            } finally {
                this.mPinnedStackId = -1;
            }
        }
        for (int size = this.mListeners.size() - 1; size >= 0; size--) {
            this.mListeners.get(size).onPipActivityClosed();
        }
        this.mHandler.removeCallbacks(this.mClosePipRunnable);
        updatePipVisibility(false);
    }

    void movePipToFullscreen() {
        this.mPipTaskId = -1;
        for (int size = this.mListeners.size() - 1; size >= 0; size--) {
            this.mListeners.get(size).onMoveToFullscreen();
        }
        resizePinnedStack(0);
        updatePipVisibility(false);
    }

    public void suspendPipResizing(int i) {
        if (DEBUG) {
            Log.d("PipManager", "suspendPipResizing() reason=" + i + " callers=" + Debug.getCallers(2));
        }
        this.mSuspendPipResizingReason = i | this.mSuspendPipResizingReason;
    }

    public void resumePipResizing(int i) {
        if ((this.mSuspendPipResizingReason & i) == 0) {
            return;
        }
        if (DEBUG) {
            Log.d("PipManager", "resumePipResizing() reason=" + i + " callers=" + Debug.getCallers(2));
        }
        this.mSuspendPipResizingReason = (~i) & this.mSuspendPipResizingReason;
        this.mHandler.post(this.mResizePinnedStackRunnable);
    }

    void resizePinnedStack(int i) {
        boolean z;
        if (DEBUG) {
            Log.d("PipManager", "resizePinnedStack() state=" + i, new Exception());
        }
        if (this.mState != 0) {
            z = false;
        } else {
            z = true;
        }
        for (int size = this.mListeners.size() - 1; size >= 0; size--) {
            this.mListeners.get(size).onPipResizeAboutToStart();
        }
        if (this.mSuspendPipResizingReason != 0) {
            this.mResumeResizePinnedStackRunnableState = i;
            if (DEBUG) {
                Log.d("PipManager", "resizePinnedStack() deferring mSuspendPipResizingReason=" + this.mSuspendPipResizingReason + " mResumeResizePinnedStackRunnableState=" + this.mResumeResizePinnedStackRunnableState);
                return;
            }
            return;
        }
        this.mState = i;
        switch (this.mState) {
            case 0:
                this.mCurrentPipBounds = null;
                if (z) {
                    return;
                }
                break;
            case 1:
                this.mCurrentPipBounds = this.mPipBounds;
                break;
            case 2:
                this.mCurrentPipBounds = this.mMenuModePipBounds;
                break;
            default:
                this.mCurrentPipBounds = this.mPipBounds;
                break;
        }
        try {
            this.mActivityManager.resizeStack(this.mPinnedStackId, this.mCurrentPipBounds, true, true, true, -1);
        } catch (RemoteException e) {
            Log.e("PipManager", "resizeStack failed", e);
        }
    }

    private int getState() {
        if (this.mSuspendPipResizingReason != 0) {
            return this.mResumeResizePinnedStackRunnableState;
        }
        return this.mState;
    }

    private void showPipMenu() {
        if (DEBUG) {
            Log.d("PipManager", "showPipMenu()");
        }
        this.mState = 2;
        for (int size = this.mListeners.size() - 1; size >= 0; size--) {
            this.mListeners.get(size).onShowPipMenu();
        }
        Intent intent = new Intent(this.mContext, (Class<?>) PipMenuActivity.class);
        intent.setFlags(268435456);
        intent.putExtra("custom_actions", (Parcelable) this.mCustomActions);
        this.mContext.startActivity(intent);
    }

    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.mListeners.remove(listener);
    }

    public void addMediaListener(MediaListener mediaListener) {
        this.mMediaListeners.add(mediaListener);
    }

    public void removeMediaListener(MediaListener mediaListener) {
        this.mMediaListeners.remove(mediaListener);
    }

    public boolean isPipShown() {
        return this.mState != 0;
    }

    private ActivityManager.StackInfo getPinnedStackInfo() {
        try {
            return this.mActivityManager.getStackInfo(2, 0);
        } catch (RemoteException e) {
            Log.e("PipManager", "getStackInfo failed", e);
            return null;
        }
    }

    private void handleMediaResourceGranted(String[] strArr) {
        if (getState() == 0) {
            this.mLastPackagesResourceGranted = strArr;
            return;
        }
        boolean z = false;
        if (this.mLastPackagesResourceGranted != null) {
            boolean z2 = false;
            for (String str : this.mLastPackagesResourceGranted) {
                int length = strArr.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    if (TextUtils.equals(strArr[i], str)) {
                        z2 = true;
                        break;
                    }
                    i++;
                }
            }
            z = z2;
        }
        this.mLastPackagesResourceGranted = strArr;
        if (!z) {
            closePip();
        }
    }

    private void updateMediaController(List<MediaController> list) {
        MediaController mediaController;
        if (list != null && getState() != 0 && this.mPipComponentName != null) {
            for (int size = list.size() - 1; size >= 0; size--) {
                mediaController = list.get(size);
                if (mediaController.getPackageName().equals(this.mPipComponentName.getPackageName())) {
                    break;
                }
            }
            mediaController = null;
        } else {
            mediaController = null;
        }
        if (this.mPipMediaController != mediaController) {
            this.mPipMediaController = mediaController;
            for (int size2 = this.mMediaListeners.size() - 1; size2 >= 0; size2--) {
                this.mMediaListeners.get(size2).onMediaControllerChanged();
            }
            if (this.mPipMediaController == null) {
                this.mHandler.postDelayed(this.mClosePipRunnable, 3000L);
            } else {
                this.mHandler.removeCallbacks(this.mClosePipRunnable);
            }
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
        boolean z = state == 6 || state == 8 || state == 3 || state == 4 || state == 5 || state == 9 || state == 10;
        long actions = this.mPipMediaController.getPlaybackState().getActions();
        if (z || (4 & actions) == 0) {
            return (!z || (2 & actions) == 0) ? 2 : 0;
        }
        return 1;
    }

    private boolean isSettingsShown() {
        String str;
        try {
            List tasks = this.mActivityManager.getTasks(1);
            if (tasks.isEmpty()) {
                return false;
            }
            ComponentName componentName = ((ActivityManager.RunningTaskInfo) tasks.get(0)).topActivity;
            for (Pair<String, String> pair : sSettingsPackageAndClassNamePairList) {
                if (componentName.getPackageName().equals((String) pair.first) && ((str = (String) pair.second) == null || componentName.getClassName().equals(str))) {
                    return true;
                }
            }
            return false;
        } catch (RemoteException e) {
            Log.d("PipManager", "Failed to detect top activity", e);
            return false;
        }
    }

    public static PipManager getInstance() {
        if (sPipManager == null) {
            sPipManager = new PipManager();
        }
        return sPipManager;
    }

    private void updatePipVisibility(boolean z) {
        SystemServicesProxy.getInstance(this.mContext).setPipVisibility(z);
    }

    @Override // com.android.systemui.pip.BasePipManager
    public void dump(PrintWriter printWriter) {
    }
}
