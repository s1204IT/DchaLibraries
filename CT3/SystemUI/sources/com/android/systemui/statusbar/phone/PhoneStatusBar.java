package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProfilerInfo;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.DemoMode;
import com.android.systemui.EventLogTags;
import com.android.systemui.Interpolators;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.classifier.FalsingLog;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.qs.QSContainer;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.ScreenPinningRequest;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.AppTransitionFinishedEvent;
import com.android.systemui.recents.events.activity.UndockingTaskEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.stackdivider.WindowManagerProxy;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.BackDropView;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.DismissView;
import com.android.systemui.statusbar.DragDownHelper;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyboardShortcuts;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationOverflowContainer;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.phone.ActivityStarter;
import com.android.systemui.statusbar.phone.LockscreenWallpaper;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryControllerImpl;
import com.android.systemui.statusbar.policy.BluetoothControllerImpl;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.statusbar.policy.CastControllerImpl;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.HotspotControllerImpl;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.LocationControllerImpl;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.PreviewInflater;
import com.android.systemui.statusbar.policy.RotationLockControllerImpl;
import com.android.systemui.statusbar.policy.SecurityControllerImpl;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.volume.VolumeComponent;
import com.mediatek.multiwindow.MultiWindowManager;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.IStatusBarPlmnPlugin;
import com.mediatek.systemui.statusbar.policy.HotKnotControllerImpl;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhoneStatusBar extends BaseStatusBar implements DemoMode, DragDownHelper.DragDownCallback, ActivityStarter, UnlockMethodCache.OnUnlockMethodChangedListener, HeadsUpManager.OnHeadsUpChangedListener {
    public static final Interpolator ALPHA_IN;
    public static final Interpolator ALPHA_OUT;
    private static final boolean FREEFORM_WINDOW_MANAGEMENT;
    private static final boolean ONLY_CORE_APPS;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    AccessibilityController mAccessibilityController;
    private boolean mAutohideSuspended;
    private BackDropView mBackdrop;
    private ImageView mBackdropBack;
    private ImageView mBackdropFront;
    protected BatteryController mBatteryController;
    BluetoothControllerImpl mBluetoothController;
    BrightnessMirrorController mBrightnessMirrorController;
    CastControllerImpl mCastController;
    private boolean mDemoMode;
    private boolean mDemoModeAllowed;
    private int mDisabledUnmodified1;
    private int mDisabledUnmodified2;
    Display mDisplay;
    protected DozeScrimController mDozeScrimController;
    private DozeServiceHost mDozeServiceHost;
    private boolean mDozing;
    private boolean mDozingRequested;
    private ExpandableNotificationRow mDraggedDownRow;
    View mExpandedContents;
    boolean mExpandedVisible;
    private FalsingManager mFalsingManager;
    FingerprintUnlockController mFingerprintUnlockController;
    FlashlightController mFlashlightController;
    private PowerManager.WakeLock mGestureWakeLock;
    private HandlerThread mHandlerThread;
    BaseStatusBarHeader mHeader;
    HotKnotControllerImpl mHotKnotController;
    HotspotControllerImpl mHotspotController;
    protected StatusBarIconController mIconController;
    PhoneStatusBarPolicy mIconPolicy;
    private int mInteractingWindows;
    KeyguardBottomAreaView mKeyguardBottomArea;
    private boolean mKeyguardFadingAway;
    private long mKeyguardFadingAwayDelay;
    private long mKeyguardFadingAwayDuration;
    private boolean mKeyguardGoingAway;
    KeyguardIndicationController mKeyguardIndicationController;
    protected KeyguardMonitor mKeyguardMonitor;
    protected KeyguardStatusBarView mKeyguardStatusBar;
    View mKeyguardStatusView;
    KeyguardUserSwitcher mKeyguardUserSwitcher;
    private ViewMediatorCallback mKeyguardViewMediatorCallback;
    private int mLastCameraLaunchSource;
    private int mLastLoggedStateFingerprint;
    private long mLastVisibilityReportUptimeMs;
    private NotificationListenerService.RankingMap mLatestRankingMap;
    private boolean mLaunchCameraOnFinishedGoingToSleep;
    private boolean mLaunchCameraOnScreenTurningOn;
    private Runnable mLaunchTransitionEndRunnable;
    private boolean mLaunchTransitionFadingAway;
    boolean mLeaveOpenOnKeyguardHide;
    LightStatusBarController mLightStatusBarController;
    LocationControllerImpl mLocationController;
    protected LockscreenWallpaper mLockscreenWallpaper;
    int mMaxAllowedKeyguardNotifications;
    private int mMaxKeyguardNotifications;
    private MediaController mMediaController;
    private MediaMetadata mMediaMetadata;
    private String mMediaNotificationKey;
    private MediaSessionManager mMediaSessionManager;
    private int mNavigationBarMode;
    NetworkControllerImpl mNetworkController;
    NextAlarmController mNextAlarmController;
    private boolean mNoAnimationOnNextBarModeChange;
    protected NotificationPanelView mNotificationPanel;
    private View mPendingRemoteInputView;
    private View mPendingWorkRemoteInputView;
    int mPixelFormat;
    private QSPanel mQSPanel;
    RotationLockControllerImpl mRotationLockController;
    private ScreenPinningRequest mScreenPinningRequest;
    private boolean mScreenTurningOn;
    protected ScrimController mScrimController;
    protected boolean mScrimSrcModeEnabled;
    SecurityControllerImpl mSecurityController;
    protected boolean mStartedGoingToSleep;
    private int mStatusBarMode;
    protected PhoneStatusBarView mStatusBarView;
    protected StatusBarWindowView mStatusBarWindow;
    protected StatusBarWindowManager mStatusBarWindowManager;
    boolean mTracking;
    int mTrackingPosition;
    private UnlockMethodCache mUnlockMethodCache;
    UserInfoController mUserInfoController;
    protected UserSwitcherController mUserSwitcherController;
    private Vibrator mVibrator;
    VolumeComponent mVolumeComponent;
    private boolean mWaitingForKeyguardExit;
    private boolean mWakeUpComingFromTouch;
    private PointF mWakeUpTouchLocation;
    protected ZenModeController mZenModeController;
    int mNaturalBarHeight = -1;
    Point mCurrentDisplaySize = new Point();
    private int mStatusBarWindowState = 0;
    Object mQueueLock = new Object();
    private int mNavigationBarWindowState = 0;
    int[] mAbsPos = new int[2];
    ArrayList<Runnable> mPostCollapseRunnables = new ArrayList<>();
    int mDisabled1 = 0;
    int mDisabled2 = 0;
    int mSystemUiVisibility = 0;
    private final Rect mLastFullscreenStackBounds = new Rect();
    private final Rect mLastDockedStackBounds = new Rect();
    private int mLastDispatchedSystemUiVisibility = -1;
    DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private final GestureRecorder mGestureRec = null;
    private int mNavigationIconHints = 0;
    private boolean mUserSetup = false;
    private ContentObserver mUserSetupObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            boolean userSetup = Settings.Secure.getIntForUser(PhoneStatusBar.this.mContext.getContentResolver(), "user_setup_complete", 0, PhoneStatusBar.this.mCurrentUserId) != 0;
            if (userSetup != PhoneStatusBar.this.mUserSetup) {
                PhoneStatusBar.this.mUserSetup = userSetup;
                if (!PhoneStatusBar.this.mUserSetup && PhoneStatusBar.this.mStatusBarView != null) {
                    PhoneStatusBar.this.animateCollapseQuickSettings();
                }
                if (PhoneStatusBar.this.mKeyguardBottomArea != null) {
                    PhoneStatusBar.this.mKeyguardBottomArea.setUserSetupComplete(PhoneStatusBar.this.mUserSetup);
                }
                if (PhoneStatusBar.this.mNetworkController != null) {
                    PhoneStatusBar.this.mNetworkController.setUserSetupComplete(PhoneStatusBar.this.mUserSetup);
                }
            }
            if (PhoneStatusBar.this.mIconPolicy == null) {
                return;
            }
            PhoneStatusBar.this.mIconPolicy.setCurrentUserSetup(PhoneStatusBar.this.mUserSetup);
        }
    };
    private final ContentObserver mHeadsUpObserver = new ContentObserver(this.mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            boolean z = false;
            boolean wasUsing = PhoneStatusBar.this.mUseHeadsUp;
            PhoneStatusBar phoneStatusBar = PhoneStatusBar.this;
            boolean z2 = (PhoneStatusBar.this.mDisableNotificationAlerts || Settings.Global.getInt(PhoneStatusBar.this.mContext.getContentResolver(), "heads_up_notifications_enabled", 0) == 0) ? false : true;
            phoneStatusBar.mUseHeadsUp = z2;
            PhoneStatusBar phoneStatusBar2 = PhoneStatusBar.this;
            if (PhoneStatusBar.this.mUseHeadsUp && Settings.Global.getInt(PhoneStatusBar.this.mContext.getContentResolver(), "ticker_gets_heads_up", 0) != 0) {
                z = true;
            }
            phoneStatusBar2.mHeadsUpTicker = z;
            Log.d("PhoneStatusBar", "heads up is " + (PhoneStatusBar.this.mUseHeadsUp ? "enabled" : "disabled"));
            if (wasUsing == PhoneStatusBar.this.mUseHeadsUp || PhoneStatusBar.this.mUseHeadsUp) {
                return;
            }
            Log.d("PhoneStatusBar", "dismissing any existing heads up notification on disable event");
            PhoneStatusBar.this.mHeadsUpManager.releaseAllImmediately();
        }
    };
    private final Runnable mAutohide = new Runnable() {
        @Override
        public void run() {
            int requested = PhoneStatusBar.this.mSystemUiVisibility & (-201326593);
            if (PhoneStatusBar.this.mSystemUiVisibility == requested) {
                return;
            }
            PhoneStatusBar.this.notifyUiVisibilityChanged(requested);
        }
    };
    private PorterDuffXfermode mSrcXferMode = new PorterDuffXfermode(PorterDuff.Mode.SRC);
    private PorterDuffXfermode mSrcOverXferMode = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    private MediaController.Callback mMediaListener = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);
            if (state == null || PhoneStatusBar.this.isPlaybackActive(state.getState())) {
                return;
            }
            PhoneStatusBar.this.clearCurrentMediaNotification();
            PhoneStatusBar.this.updateMediaMetaData(true, true);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            PhoneStatusBar.this.mMediaMetadata = metadata;
            PhoneStatusBar.this.updateMediaMetaData(true, true);
        }
    };
    private final NotificationStackScrollLayout.OnChildLocationsChangedListener mOnChildLocationsChangedListener = new NotificationStackScrollLayout.OnChildLocationsChangedListener() {
        @Override
        public void onChildLocationsChanged(NotificationStackScrollLayout stackScrollLayout) {
            PhoneStatusBar.this.userActivity();
        }
    };
    private final ArraySet<NotificationVisibility> mCurrentlyVisibleNotifications = new ArraySet<>();
    private final ShadeUpdates mShadeUpdates = new ShadeUpdates(this, null);
    private final NotificationStackScrollLayout.OnChildLocationsChangedListener mNotificationLocationsChangedListener = new NotificationStackScrollLayout.OnChildLocationsChangedListener() {
        @Override
        public void onChildLocationsChanged(NotificationStackScrollLayout stackScrollLayout) {
            if (PhoneStatusBar.this.mHandler.hasCallbacks(PhoneStatusBar.this.mVisibilityReporter)) {
                return;
            }
            long nextReportUptimeMs = PhoneStatusBar.this.mLastVisibilityReportUptimeMs + 500;
            PhoneStatusBar.this.mHandler.postAtTime(PhoneStatusBar.this.mVisibilityReporter, nextReportUptimeMs);
        }
    };
    private final Runnable mVisibilityReporter = new Runnable() {
        private final ArraySet<NotificationVisibility> mTmpNewlyVisibleNotifications = new ArraySet<>();
        private final ArraySet<NotificationVisibility> mTmpCurrentlyVisibleNotifications = new ArraySet<>();
        private final ArraySet<NotificationVisibility> mTmpNoLongerVisibleNotifications = new ArraySet<>();

        @Override
        public void run() {
            PhoneStatusBar.this.mLastVisibilityReportUptimeMs = SystemClock.uptimeMillis();
            PhoneStatusBar.this.getCurrentMediaNotificationKey();
            ArrayList<NotificationData.Entry> activeNotifications = PhoneStatusBar.this.mNotificationData.getActiveNotifications();
            int N = activeNotifications.size();
            for (int i = 0; i < N; i++) {
                NotificationData.Entry entry = activeNotifications.get(i);
                String key = entry.notification.getKey();
                boolean isVisible = (PhoneStatusBar.this.mStackScroller.getChildLocation(entry.row) & 5) != 0;
                NotificationVisibility visObj = NotificationVisibility.obtain(key, i, isVisible);
                boolean previouslyVisible = PhoneStatusBar.this.mCurrentlyVisibleNotifications.contains(visObj);
                if (isVisible) {
                    this.mTmpCurrentlyVisibleNotifications.add(visObj);
                    if (!previouslyVisible) {
                        this.mTmpNewlyVisibleNotifications.add(visObj);
                    }
                } else {
                    visObj.recycle();
                }
            }
            this.mTmpNoLongerVisibleNotifications.addAll(PhoneStatusBar.this.mCurrentlyVisibleNotifications);
            this.mTmpNoLongerVisibleNotifications.removeAll((ArraySet<? extends NotificationVisibility>) this.mTmpCurrentlyVisibleNotifications);
            PhoneStatusBar.this.logNotificationVisibilityChanges(this.mTmpNewlyVisibleNotifications, this.mTmpNoLongerVisibleNotifications);
            PhoneStatusBar.this.recycleAllVisibilityObjects(PhoneStatusBar.this.mCurrentlyVisibleNotifications);
            PhoneStatusBar.this.mCurrentlyVisibleNotifications.addAll((ArraySet) this.mTmpCurrentlyVisibleNotifications);
            PhoneStatusBar.this.recycleAllVisibilityObjects(this.mTmpNoLongerVisibleNotifications);
            this.mTmpCurrentlyVisibleNotifications.clear();
            this.mTmpNewlyVisibleNotifications.clear();
            this.mTmpNoLongerVisibleNotifications.clear();
        }
    };
    private final View.OnClickListener mOverflowClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PhoneStatusBar.this.goToLockedShade(null);
        }
    };
    private HashMap<ExpandableNotificationRow, List<ExpandableNotificationRow>> mTmpChildOrderMap = new HashMap<>();
    private View.OnClickListener mRecentsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PhoneStatusBar.this.awakenDreams();
            PhoneStatusBar.this.toggleRecentApps();
        }
    };
    private View.OnClickListener mRestoreClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d("PhoneStatusBar", "mRestoreClickListener");
            SystemServicesProxy ssp = Recents.getSystemServices();
            ssp.restoreWindow();
        }
    };
    private View.OnLongClickListener mLongPressBackListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            return PhoneStatusBar.this.handleLongPressBack();
        }
    };
    private View.OnLongClickListener mRecentsLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (PhoneStatusBar.this.mRecents == null || !ActivityManager.supportsMultiWindow() || !((Divider) PhoneStatusBar.this.getComponent(Divider.class)).getView().getSnapAlgorithm().isSplitScreenFeasible()) {
                return false;
            }
            PhoneStatusBar.this.toggleSplitScreenMode(271, 286);
            return true;
        }
    };
    private final View.OnLongClickListener mLongPressHomeListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (PhoneStatusBar.this.shouldDisableNavbarGestures()) {
                return false;
            }
            MetricsLogger.action(PhoneStatusBar.this.mContext, 239);
            PhoneStatusBar.this.mAssistManager.startAssist(new Bundle());
            PhoneStatusBar.this.awakenDreams();
            if (PhoneStatusBar.this.mNavigationBarView != null) {
                PhoneStatusBar.this.mNavigationBarView.abortCurrentGesture();
                return true;
            }
            return true;
        }
    };
    private final View.OnTouchListener mHomeActionListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case 1:
                case 3:
                    PhoneStatusBar.this.awakenDreams();
                    break;
            }
            return false;
        }
    };
    private Runnable mHideBackdropFront = new Runnable() {
        @Override
        public void run() {
            PhoneStatusBar.this.mBackdropFront.setVisibility(4);
            PhoneStatusBar.this.mBackdropFront.animate().cancel();
            PhoneStatusBar.this.mBackdropFront.setImageDrawable(null);
        }
    };
    private final Runnable mAnimateCollapsePanels = new Runnable() {
        @Override
        public void run() {
            PhoneStatusBar.this.animateCollapsePanels();
        }
    };
    private final Runnable mCheckBarModes = new Runnable() {
        @Override
        public void run() {
            PhoneStatusBar.this.checkBarModes();
        }
    };
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("PhoneStatusBar", "onReceive: " + intent);
            String action = intent.getAction();
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                KeyboardShortcuts.dismiss();
                if (PhoneStatusBar.this.mRemoteInputController != null) {
                    PhoneStatusBar.this.mRemoteInputController.closeRemoteInputs();
                }
                if (!PhoneStatusBar.this.isCurrentProfile(getSendingUserId())) {
                    return;
                }
                int flags = 0;
                String reason = intent.getStringExtra("reason");
                if (reason != null && reason.equals("recentapps")) {
                    flags = 2;
                }
                PhoneStatusBar.this.animateCollapsePanels(flags);
                return;
            }
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                PhoneStatusBar.this.notifyNavigationBarScreenOn(false);
                PhoneStatusBar.this.notifyHeadsUpScreenOff();
                PhoneStatusBar.this.finishBarAnimations();
                PhoneStatusBar.this.resetUserExpandedStates();
                return;
            }
            if (!"android.intent.action.SCREEN_ON".equals(action)) {
                return;
            }
            PhoneStatusBar.this.notifyNavigationBarScreenOn(true);
        }
    };
    private BroadcastReceiver mDemoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("PhoneStatusBar", "onReceive: " + intent);
            String action = intent.getAction();
            if ("com.android.systemui.demo".equals(action)) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    return;
                }
                String command = bundle.getString("command", "").trim().toLowerCase();
                if (command.length() <= 0) {
                    return;
                }
                try {
                    PhoneStatusBar.this.dispatchDemoCommand(command, bundle);
                    return;
                } catch (Throwable t) {
                    Log.w("PhoneStatusBar", "Error running demo command, intent=" + intent, t);
                    return;
                }
            }
            if ("fake_artwork".equals(action)) {
            }
        }
    };
    Runnable mStartTracing = new Runnable() {
        @Override
        public void run() {
            PhoneStatusBar.this.vibrate();
            SystemClock.sleep(250L);
            Log.d("PhoneStatusBar", "startTracing");
            Debug.startMethodTracing("/data/statusbar-traces/trace");
            PhoneStatusBar.this.mHandler.postDelayed(PhoneStatusBar.this.mStopTracing, 10000L);
        }
    };
    Runnable mStopTracing = new Runnable() {
        @Override
        public void run() {
            Debug.stopMethodTracing();
            Log.d("PhoneStatusBar", "stopTracing");
            PhoneStatusBar.this.vibrate();
        }
    };
    private IStatusBarPlmnPlugin mStatusBarPlmnPlugin = null;
    private View mCustomizeCarrierLabel = null;

    static {
        boolean onlyCoreApps;
        boolean zHasSystemFeature;
        try {
            IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            onlyCoreApps = packageManager.isOnlyCoreApps();
            zHasSystemFeature = packageManager.hasSystemFeature("android.software.freeform_window_management", 0);
        } catch (RemoteException e) {
            onlyCoreApps = false;
            zHasSystemFeature = false;
        }
        ONLY_CORE_APPS = onlyCoreApps;
        FREEFORM_WINDOW_MANAGEMENT = zHasSystemFeature;
        ALPHA_IN = Interpolators.ALPHA_IN;
        ALPHA_OUT = Interpolators.ALPHA_OUT;
    }

    public void recycleAllVisibilityObjects(ArraySet<NotificationVisibility> array) {
        int N = array.size();
        for (int i = 0; i < N; i++) {
            array.valueAt(i).recycle();
        }
        array.clear();
    }

    @Override
    public void start() {
        this.mDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        updateDisplaySize();
        this.mScrimSrcModeEnabled = this.mContext.getResources().getBoolean(R.bool.config_status_bar_scrim_behind_use_src);
        super.start();
        this.mMediaSessionManager = (MediaSessionManager) this.mContext.getSystemService("media_session");
        addNavigationBar();
        this.mIconPolicy = new PhoneStatusBarPolicy(this.mContext, this.mIconController, this.mCastController, this.mHotspotController, this.mUserInfoController, this.mBluetoothController, this.mRotationLockController, this.mNetworkController.getDataSaverController());
        this.mIconPolicy.setCurrentUserSetup(this.mUserSetup);
        this.mSettingsObserver.onChange(false);
        this.mHeadsUpObserver.onChange(true);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("heads_up_notifications_enabled"), true, this.mHeadsUpObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("ticker_gets_heads_up"), true, this.mHeadsUpObserver);
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(this.mContext);
        this.mUnlockMethodCache.addListener(this);
        startKeyguard();
        this.mDozeServiceHost = new DozeServiceHost(this, null);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mDozeServiceHost);
        putComponent(DozeHost.class, this.mDozeServiceHost);
        putComponent(PhoneStatusBar.class, this);
        setControllerUsers();
        notifyUserAboutHiddenNotifications();
        this.mScreenPinningRequest = new ScreenPinningRequest(this.mContext);
        this.mFalsingManager = FalsingManager.getInstance(this.mContext);
    }

    protected void createIconController() {
        this.mIconController = new StatusBarIconController(this.mContext, this.mStatusBarView, this.mKeyguardStatusBar, this);
    }

    protected PhoneStatusBarView makeStatusBarView() {
        Context context = this.mContext;
        updateDisplaySize();
        updateResources();
        inflateStatusBarWindow(context);
        this.mStatusBarWindow.setService(this);
        this.mStatusBarWindow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                PhoneStatusBar.this.checkUserAutohide(v, event);
                if (event.getAction() == 0 && PhoneStatusBar.this.mExpandedVisible) {
                    PhoneStatusBar.this.animateCollapsePanels();
                }
                return PhoneStatusBar.this.mStatusBarWindow.onTouchEvent(event);
            }
        });
        this.mNotificationPanel = (NotificationPanelView) this.mStatusBarWindow.findViewById(R.id.notification_panel);
        this.mNotificationPanel.setStatusBar(this);
        this.mNotificationPanel.setGroupManager(this.mGroupManager);
        this.mStatusBarView = (PhoneStatusBarView) this.mStatusBarWindow.findViewById(R.id.status_bar);
        this.mStatusBarView.setBar(this);
        this.mStatusBarView.setPanel(this.mNotificationPanel);
        if (!ActivityManager.isHighEndGfx() && !FeatureOptions.LOW_RAM_SUPPORT) {
            this.mStatusBarWindow.setBackground(null);
            this.mNotificationPanel.setBackground(new FastColorDrawable(context.getColor(R.color.notification_panel_solid_background)));
        }
        this.mHeadsUpManager = new HeadsUpManager(context, this.mStatusBarWindow, this.mGroupManager);
        this.mHeadsUpManager.setBar(this);
        this.mHeadsUpManager.addListener(this);
        this.mHeadsUpManager.addListener(this.mNotificationPanel);
        this.mHeadsUpManager.addListener(this.mGroupManager);
        this.mNotificationPanel.setHeadsUpManager(this.mHeadsUpManager);
        this.mNotificationData.setHeadsUpManager(this.mHeadsUpManager);
        this.mGroupManager.setHeadsUpManager(this.mHeadsUpManager);
        try {
            boolean showNav = this.mWindowManagerService.hasNavigationBar();
            Log.v("PhoneStatusBar", "hasNavigationBar=" + showNav);
            if (showNav) {
                createNavigationBarView(context);
            }
        } catch (RemoteException e) {
        }
        this.mAssistManager = new AssistManager(this, context);
        this.mPixelFormat = -1;
        this.mStackScroller = (NotificationStackScrollLayout) this.mStatusBarWindow.findViewById(R.id.notification_stack_scroller);
        this.mStackScroller.setLongPressListener(getNotificationLongClicker());
        this.mStackScroller.setPhoneStatusBar(this);
        this.mStackScroller.setGroupManager(this.mGroupManager);
        this.mStackScroller.setHeadsUpManager(this.mHeadsUpManager);
        this.mGroupManager.setOnGroupChangeListener(this.mStackScroller);
        inflateOverflowContainer();
        inflateEmptyShadeView();
        inflateDismissView();
        this.mExpandedContents = this.mStackScroller;
        this.mBackdrop = (BackDropView) this.mStatusBarWindow.findViewById(R.id.backdrop);
        this.mBackdropFront = (ImageView) this.mBackdrop.findViewById(R.id.backdrop_front);
        this.mBackdropBack = (ImageView) this.mBackdrop.findViewById(R.id.backdrop_back);
        ScrimView scrimBehind = (ScrimView) this.mStatusBarWindow.findViewById(R.id.scrim_behind);
        ScrimView scrimInFront = (ScrimView) this.mStatusBarWindow.findViewById(R.id.scrim_in_front);
        View headsUpScrim = this.mStatusBarWindow.findViewById(R.id.heads_up_scrim);
        this.mScrimController = SystemUIFactory.getInstance().createScrimController(scrimBehind, scrimInFront, headsUpScrim);
        if (this.mScrimSrcModeEnabled) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    boolean asSrc = PhoneStatusBar.this.mBackdrop.getVisibility() != 0;
                    PhoneStatusBar.this.mScrimController.setDrawBehindAsSrc(asSrc);
                    PhoneStatusBar.this.mStackScroller.setDrawBackgroundAsSrc(asSrc);
                }
            };
            this.mBackdrop.setOnVisibilityChangedRunnable(runnable);
            runnable.run();
        }
        this.mHeadsUpManager.addListener(this.mScrimController);
        this.mStackScroller.setScrimController(this.mScrimController);
        this.mStatusBarView.setScrimController(this.mScrimController);
        this.mDozeScrimController = new DozeScrimController(this.mScrimController, context);
        this.mKeyguardStatusBar = (KeyguardStatusBarView) this.mStatusBarWindow.findViewById(R.id.keyguard_header);
        this.mKeyguardStatusView = this.mStatusBarWindow.findViewById(R.id.keyguard_status_view);
        this.mKeyguardBottomArea = (KeyguardBottomAreaView) this.mStatusBarWindow.findViewById(R.id.keyguard_bottom_area);
        this.mKeyguardBottomArea.setActivityStarter(this);
        this.mKeyguardBottomArea.setAssistManager(this.mAssistManager);
        this.mKeyguardIndicationController = new KeyguardIndicationController(this.mContext, (KeyguardIndicationTextView) this.mStatusBarWindow.findViewById(R.id.keyguard_indication_text), this.mKeyguardBottomArea.getLockIcon());
        this.mKeyguardBottomArea.setKeyguardIndicationController(this.mKeyguardIndicationController);
        this.mLockscreenWallpaper = new LockscreenWallpaper(this.mContext, this, this.mHandler);
        setAreThereNotifications();
        createIconController();
        this.mHandlerThread = new HandlerThread("PhoneStatusBar", 10);
        this.mHandlerThread.start();
        this.mLocationController = new LocationControllerImpl(this.mContext, this.mHandlerThread.getLooper());
        this.mBatteryController = createBatteryController();
        this.mBatteryController.addStateChangedCallback(new BatteryController.BatteryStateChangeCallback() {
            @Override
            public void onPowerSaveChanged(boolean isPowerSave) {
                PhoneStatusBar.this.mHandler.post(PhoneStatusBar.this.mCheckBarModes);
                if (PhoneStatusBar.this.mDozeServiceHost == null) {
                    return;
                }
                PhoneStatusBar.this.mDozeServiceHost.firePowerSaveChanged(isPowerSave);
            }

            @Override
            public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
            }
        });
        this.mNetworkController = new NetworkControllerImpl(this.mContext, this.mHandlerThread.getLooper());
        this.mNetworkController.setUserSetupComplete(this.mUserSetup);
        this.mHotspotController = new HotspotControllerImpl(this.mContext);
        this.mBluetoothController = new BluetoothControllerImpl(this.mContext, this.mHandlerThread.getLooper());
        this.mSecurityController = new SecurityControllerImpl(this.mContext);
        if (SIMHelper.isMtkHotKnotSupport()) {
            Log.d("PhoneStatusBar", "makeStatusBarView : HotKnotControllerImpl");
            this.mHotKnotController = new HotKnotControllerImpl(this.mContext);
        } else {
            this.mHotKnotController = null;
        }
        SIMHelper.setContext(this.mContext);
        if (this.mContext.getResources().getBoolean(R.bool.config_showRotationLock)) {
            this.mRotationLockController = new RotationLockControllerImpl(this.mContext);
        }
        this.mUserInfoController = new UserInfoController(this.mContext);
        this.mVolumeComponent = (VolumeComponent) getComponent(VolumeComponent.class);
        if (this.mVolumeComponent != null) {
            this.mZenModeController = this.mVolumeComponent.getZenController();
        }
        Log.d("PhoneStatusBar", "makeStatusBarView : CastControllerImpl +");
        this.mCastController = new CastControllerImpl(this.mContext);
        initSignalCluster(this.mStatusBarView);
        initSignalCluster(this.mKeyguardStatusBar);
        this.mStatusBarPlmnPlugin = PluginManager.getStatusBarPlmnPlugin(context);
        if (supportCustomizeCarrierLabel()) {
            this.mCustomizeCarrierLabel = this.mStatusBarPlmnPlugin.customizeCarrierLabel(this.mNotificationPanel, null);
        }
        this.mFlashlightController = new FlashlightController(this.mContext);
        this.mKeyguardBottomArea.setFlashlightController(this.mFlashlightController);
        this.mKeyguardBottomArea.setPhoneStatusBar(this);
        this.mKeyguardBottomArea.setUserSetupComplete(this.mUserSetup);
        this.mAccessibilityController = new AccessibilityController(this.mContext);
        this.mKeyguardBottomArea.setAccessibilityController(this.mAccessibilityController);
        this.mNextAlarmController = new NextAlarmController(this.mContext);
        this.mLightStatusBarController = new LightStatusBarController(this.mIconController, this.mBatteryController);
        this.mKeyguardMonitor = new KeyguardMonitor(this.mContext);
        if (UserManager.get(this.mContext).isUserSwitcherEnabled()) {
            this.mUserSwitcherController = new UserSwitcherController(this.mContext, this.mKeyguardMonitor, this.mHandler, this);
            createUserSwitcher();
        }
        AutoReinflateContainer container = (AutoReinflateContainer) this.mStatusBarWindow.findViewById(R.id.qs_auto_reinflate_container);
        if (container != null) {
            final QSTileHost qsh = SystemUIFactory.getInstance().createQSTileHost(this.mContext, this, this.mBluetoothController, this.mLocationController, this.mRotationLockController, this.mNetworkController, this.mZenModeController, this.mHotspotController, this.mCastController, this.mFlashlightController, this.mUserSwitcherController, this.mUserInfoController, this.mKeyguardMonitor, this.mSecurityController, this.mBatteryController, this.mIconController, this.mNextAlarmController, this.mHotKnotController);
            this.mBrightnessMirrorController = new BrightnessMirrorController(this.mStatusBarWindow);
            container.addInflateListener(new AutoReinflateContainer.InflateListener() {
                @Override
                public void onInflated(View v) {
                    QSContainer qsContainer = (QSContainer) v.findViewById(R.id.quick_settings_container);
                    qsContainer.setHost(qsh);
                    PhoneStatusBar.this.mQSPanel = qsContainer.getQsPanel();
                    PhoneStatusBar.this.mQSPanel.setBrightnessMirror(PhoneStatusBar.this.mBrightnessMirrorController);
                    PhoneStatusBar.this.mKeyguardStatusBar.setQSPanel(PhoneStatusBar.this.mQSPanel);
                    PhoneStatusBar.this.mHeader = qsContainer.getHeader();
                    PhoneStatusBar.this.initSignalCluster(PhoneStatusBar.this.mHeader);
                    PhoneStatusBar.this.mHeader.setActivityStarter(PhoneStatusBar.this);
                }
            });
        }
        this.mKeyguardStatusBar.setUserInfoController(this.mUserInfoController);
        this.mKeyguardStatusBar.setUserSwitcherController(this.mUserSwitcherController);
        this.mUserInfoController.reloadUserInfo();
        ((BatteryMeterView) this.mStatusBarView.findViewById(R.id.battery)).setBatteryController(this.mBatteryController);
        this.mKeyguardStatusBar.setBatteryController(this.mBatteryController);
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        this.mBroadcastReceiver.onReceive(this.mContext, new Intent(pm.isScreenOn() ? "android.intent.action.SCREEN_ON" : "android.intent.action.SCREEN_OFF"));
        this.mGestureWakeLock = pm.newWakeLock(10, "GestureWakeLock");
        this.mVibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
        IntentFilter demoFilter = new IntentFilter();
        demoFilter.addAction("com.android.systemui.demo");
        context.registerReceiverAsUser(this.mDemoReceiver, UserHandle.ALL, demoFilter, "android.permission.DUMP", null);
        resetUserSetupObserver();
        ThreadedRenderer.overrideProperty("disableProfileBars", "true");
        ThreadedRenderer.overrideProperty("ambientRatio", String.valueOf(1.5f));
        this.mStatusBarPlmnPlugin.addPlmn((LinearLayout) this.mStatusBarView.findViewById(R.id.status_bar_contents), this.mContext);
        return this.mStatusBarView;
    }

    protected BatteryController createBatteryController() {
        return new BatteryControllerImpl(this.mContext);
    }

    private void inflateOverflowContainer() {
        this.mKeyguardIconOverflowContainer = (NotificationOverflowContainer) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_notification_keyguard_overflow, (ViewGroup) this.mStackScroller, false);
        this.mKeyguardIconOverflowContainer.setOnActivatedListener(this);
        this.mKeyguardIconOverflowContainer.setOnClickListener(this.mOverflowClickListener);
        this.mStackScroller.setOverflowContainer(this.mKeyguardIconOverflowContainer);
    }

    @Override
    protected void onDensityOrFontScaleChanged() {
        super.onDensityOrFontScaleChanged();
        this.mScrimController.onDensityOrFontScaleChanged();
        this.mStatusBarView.onDensityOrFontScaleChanged();
        if (this.mBrightnessMirrorController != null) {
            this.mBrightnessMirrorController.onDensityOrFontScaleChanged();
        }
        inflateSignalClusters();
        this.mIconController.onDensityOrFontScaleChanged();
        inflateDismissView();
        updateClearAll();
        inflateEmptyShadeView();
        updateEmptyShadeView();
        inflateOverflowContainer();
        this.mStatusBarKeyguardViewManager.onDensityOrFontScaleChanged();
        this.mUserInfoController.onDensityOrFontScaleChanged();
        if (this.mUserSwitcherController != null) {
            this.mUserSwitcherController.onDensityOrFontScaleChanged();
        }
        if (this.mKeyguardUserSwitcher == null) {
            return;
        }
        this.mKeyguardUserSwitcher.onDensityOrFontScaleChanged();
    }

    private void inflateSignalClusters() {
        SignalClusterView signalClusterView = reinflateSignalCluster(this.mStatusBarView);
        this.mIconController.setSignalCluster(signalClusterView);
        reinflateSignalCluster(this.mKeyguardStatusBar);
    }

    private SignalClusterView reinflateSignalCluster(View view) {
        SignalClusterView signalCluster = (SignalClusterView) view.findViewById(R.id.signal_cluster);
        if (signalCluster == null) {
            return null;
        }
        ViewParent parent = signalCluster.getParent();
        if (parent instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) parent;
            int index = viewGroup.indexOfChild(signalCluster);
            viewGroup.removeView(signalCluster);
            SignalClusterView newCluster = (SignalClusterView) LayoutInflater.from(this.mContext).inflate(R.layout.signal_cluster_view, viewGroup, false);
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) viewGroup.getLayoutParams();
            layoutParams.setMarginsRelative(this.mContext.getResources().getDimensionPixelSize(R.dimen.signal_cluster_margin_start), 0, 0, 0);
            newCluster.setLayoutParams(layoutParams);
            newCluster.setSecurityController(this.mSecurityController);
            newCluster.setNetworkController(this.mNetworkController);
            viewGroup.addView(newCluster, index);
            return newCluster;
        }
        return signalCluster;
    }

    private void inflateEmptyShadeView() {
        this.mEmptyShadeView = (EmptyShadeView) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_no_notifications, (ViewGroup) this.mStackScroller, false);
        this.mStackScroller.setEmptyShadeView(this.mEmptyShadeView);
    }

    private void inflateDismissView() {
        this.mDismissView = (DismissView) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_notification_dismiss_all, (ViewGroup) this.mStackScroller, false);
        this.mDismissView.setOnButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MetricsLogger.action(PhoneStatusBar.this.mContext, 148);
                PhoneStatusBar.this.clearAllNotifications();
            }
        });
        this.mStackScroller.setDismissView(this.mDismissView);
    }

    protected void createUserSwitcher() {
        this.mKeyguardUserSwitcher = new KeyguardUserSwitcher(this.mContext, (ViewStub) this.mStatusBarWindow.findViewById(R.id.keyguard_user_switcher), this.mKeyguardStatusBar, this.mNotificationPanel, this.mUserSwitcherController);
    }

    protected void inflateStatusBarWindow(Context context) {
        this.mStatusBarWindow = (StatusBarWindowView) View.inflate(context, R.layout.super_status_bar, null);
    }

    protected void createNavigationBarView(Context context) {
        inflateNavigationBarView(context);
        this.mNavigationBarView.setDisabledFlags(this.mDisabled1);
        this.mNavigationBarView.setComponents(this.mRecents, (Divider) getComponent(Divider.class));
        this.mNavigationBarView.setOnVerticalChangedListener(new NavigationBarView.OnVerticalChangedListener() {
            @Override
            public void onVerticalChanged(boolean isVertical) {
                if (PhoneStatusBar.this.mAssistManager != null) {
                    PhoneStatusBar.this.mAssistManager.onConfigurationChanged();
                }
                PhoneStatusBar.this.mNotificationPanel.setQsScrimEnabled(!isVertical);
            }
        });
        this.mNavigationBarView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                PhoneStatusBar.this.checkUserAutohide(v, event);
                return false;
            }
        });
    }

    protected void inflateNavigationBarView(Context context) {
        this.mNavigationBarView = (NavigationBarView) View.inflate(context, R.layout.navigation_bar, null);
    }

    protected void initSignalCluster(View containerView) {
        SignalClusterView signalCluster = (SignalClusterView) containerView.findViewById(R.id.signal_cluster);
        if (signalCluster == null) {
            return;
        }
        signalCluster.setSecurityController(this.mSecurityController);
        signalCluster.setNetworkController(this.mNetworkController);
    }

    public void clearAllNotifications() {
        int numChildren = this.mStackScroller.getChildCount();
        ArrayList<View> viewsToHide = new ArrayList<>(numChildren);
        for (int i = 0; i < numChildren; i++) {
            View child = this.mStackScroller.getChildAt(i);
            if (child instanceof ExpandableNotificationRow) {
                if (this.mStackScroller.canChildBeDismissed(child) && child.getVisibility() == 0) {
                    viewsToHide.add(child);
                }
                ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                List<ExpandableNotificationRow> children = row.getNotificationChildren();
                if (row.areChildrenExpanded() && children != null) {
                    for (ExpandableNotificationRow childRow : children) {
                        if (childRow.getVisibility() == 0) {
                            viewsToHide.add(childRow);
                        }
                    }
                }
            }
        }
        if (viewsToHide.isEmpty()) {
            animateCollapsePanels(0);
        } else {
            addPostCollapseAction(new Runnable() {
                @Override
                public void run() {
                    PhoneStatusBar.this.mStackScroller.setDismissAllInProgress(false);
                    try {
                        PhoneStatusBar.this.mBarService.onClearAllNotifications(PhoneStatusBar.this.mCurrentUserId);
                    } catch (Exception e) {
                    }
                }
            });
            performDismissAllAnimations(viewsToHide);
        }
    }

    private void performDismissAllAnimations(ArrayList<View> hideAnimatedList) {
        Runnable animationFinishAction = new Runnable() {
            @Override
            public void run() {
                PhoneStatusBar.this.animateCollapsePanels(0);
            }
        };
        this.mStackScroller.setDismissAllInProgress(true);
        int currentDelay = 140;
        int totalDelay = 180;
        int numItems = hideAnimatedList.size();
        for (int i = numItems - 1; i >= 0; i--) {
            View view = hideAnimatedList.get(i);
            Runnable endRunnable = null;
            if (i == 0) {
                endRunnable = animationFinishAction;
            }
            this.mStackScroller.dismissViewAnimated(view, endRunnable, totalDelay, 260L);
            currentDelay = Math.max(50, currentDelay - 10);
            totalDelay += currentDelay;
        }
    }

    @Override
    protected void setZenMode(int mode) {
        super.setZenMode(mode);
        if (this.mIconPolicy == null) {
            return;
        }
        this.mIconPolicy.setZenMode(mode);
    }

    protected void startKeyguard() {
        KeyguardViewMediator keyguardViewMediator = (KeyguardViewMediator) getComponent(KeyguardViewMediator.class);
        this.mFingerprintUnlockController = new FingerprintUnlockController(this.mContext, this.mStatusBarWindowManager, this.mDozeScrimController, keyguardViewMediator, this.mScrimController, this);
        this.mStatusBarKeyguardViewManager = keyguardViewMediator.registerStatusBar(this, getBouncerContainer(), this.mStatusBarWindowManager, this.mScrimController, this.mFingerprintUnlockController);
        this.mKeyguardIndicationController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mFingerprintUnlockController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mIconPolicy.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mRemoteInputController.addCallback(this.mStatusBarKeyguardViewManager);
        this.mRemoteInputController.addCallback(new AnonymousClass31());
        this.mKeyguardViewMediatorCallback = keyguardViewMediator.getViewMediatorCallback();
        this.mLightStatusBarController.setFingerprintUnlockController(this.mFingerprintUnlockController);
    }

    class AnonymousClass31 implements RemoteInputController.Callback {
        AnonymousClass31() {
        }

        @Override
        public void onRemoteInputSent(final NotificationData.Entry entry) {
            if (PhoneStatusBar.FORCE_REMOTE_INPUT_HISTORY && PhoneStatusBar.this.mKeysKeptForRemoteInput.contains(entry.key)) {
                PhoneStatusBar.this.removeNotification(entry.key, null);
            } else {
                if (!PhoneStatusBar.this.mRemoteInputEntriesToRemoveOnCollapse.contains(entry)) {
                    return;
                }
                PhoneStatusBar.this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        this.val$this.m1484com_android_systemui_statusbar_phone_PhoneStatusBar$31_lambda$1(entry);
                    }
                }, 200L);
            }
        }

        void m1484com_android_systemui_statusbar_phone_PhoneStatusBar$31_lambda$1(NotificationData.Entry entry) {
            if (PhoneStatusBar.this.mRemoteInputEntriesToRemoveOnCollapse.remove(entry)) {
                PhoneStatusBar.this.removeNotification(entry.key, null);
            }
        }
    }

    protected View getStatusBarView() {
        return this.mStatusBarView;
    }

    public StatusBarWindowView getStatusBarWindow() {
        return this.mStatusBarWindow;
    }

    protected ViewGroup getBouncerContainer() {
        return this.mStatusBarWindow;
    }

    public int getStatusBarHeight() {
        if (this.mNaturalBarHeight < 0) {
            Resources res = this.mContext.getResources();
            this.mNaturalBarHeight = res.getDimensionPixelSize(android.R.dimen.accessibility_touch_slop);
        }
        return this.mNaturalBarHeight;
    }

    @Override
    protected void toggleSplitScreenMode(int metricsDockAction, int metricsUndockAction) {
        if (this.mRecents == null) {
            return;
        }
        int dockSide = WindowManagerProxy.getInstance().getDockSide();
        if (dockSide == -1) {
            this.mRecents.dockTopTask(-1, 0, null, metricsDockAction);
            return;
        }
        EventBus.getDefault().send(new UndockingTaskEvent());
        if (metricsUndockAction == -1) {
            return;
        }
        MetricsLogger.action(this.mContext, metricsUndockAction);
    }

    public void awakenDreams() {
        if (this.mDreamManager == null) {
            return;
        }
        try {
            this.mDreamManager.awaken();
        } catch (RemoteException e) {
        }
    }

    private void prepareNavigationBarView() {
        this.mNavigationBarView.reorient();
        ButtonDispatcher recentsButton = this.mNavigationBarView.getRecentsButton();
        recentsButton.setOnClickListener(this.mRecentsClickListener);
        recentsButton.setOnTouchListener(this.mRecentsPreloadOnTouchListener);
        recentsButton.setLongClickable(true);
        recentsButton.setOnLongClickListener(this.mRecentsLongClickListener);
        ButtonDispatcher backButton = this.mNavigationBarView.getBackButton();
        backButton.setLongClickable(true);
        backButton.setOnLongClickListener(this.mLongPressBackListener);
        ButtonDispatcher homeButton = this.mNavigationBarView.getHomeButton();
        homeButton.setOnTouchListener(this.mHomeActionListener);
        homeButton.setOnLongClickListener(this.mLongPressHomeListener);
        if (MultiWindowManager.isSupported()) {
            ButtonDispatcher restoreButton = this.mNavigationBarView.getRestoreButton();
            restoreButton.setOnClickListener(this.mRestoreClickListener);
        }
        this.mAssistManager.onConfigurationChanged();
    }

    protected void addNavigationBar() {
        Log.v("PhoneStatusBar", "addNavigationBar: about to add " + this.mNavigationBarView);
        if (this.mNavigationBarView == null) {
            return;
        }
        prepareNavigationBarView();
        this.mWindowManager.addView(this.mNavigationBarView, getNavigationBarLayoutParams());
    }

    protected void repositionNavigationBar() {
        if (this.mNavigationBarView == null || !this.mNavigationBarView.isAttachedToWindow()) {
            return;
        }
        prepareNavigationBarView();
        this.mWindowManager.updateViewLayout(this.mNavigationBarView, getNavigationBarLayoutParams());
    }

    public void notifyNavigationBarScreenOn(boolean screenOn) {
        if (this.mNavigationBarView == null) {
            return;
        }
        this.mNavigationBarView.notifyScreenOn(screenOn);
    }

    private WindowManager.LayoutParams getNavigationBarLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2019, 8650856, -3);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        }
        lp.setTitle("NavigationBar");
        lp.windowAnimations = 0;
        return lp;
    }

    @Override
    public void setIcon(String slot, StatusBarIcon icon) {
        this.mIconController.setIcon(slot, icon);
    }

    @Override
    public void removeIcon(String slot) {
        this.mIconController.removeIcon(slot);
    }

    @Override
    public void addNotification(StatusBarNotification notification, NotificationListenerService.RankingMap ranking, NotificationData.Entry oldEntry) {
        Log.d("PhoneStatusBar", "addNotification key=" + notification.getKey());
        if (notification != null && notification.getNotification() != null && (notification.getNotification().flags & 268435456) != 0) {
            Log.d("PhoneStatusBar", "Will not add the notification.flags contains FLAG_HIDE_NOTIFICATION");
            return;
        }
        this.mNotificationData.updateRanking(ranking);
        NotificationData.Entry shadeEntry = createNotificationViews(notification);
        if (shadeEntry == null) {
            return;
        }
        boolean isHeadsUped = shouldPeek(shadeEntry);
        if (isHeadsUped) {
            this.mHeadsUpManager.showNotification(shadeEntry);
            setNotificationShown(notification);
        }
        if (!isHeadsUped && notification.getNotification().fullScreenIntent != null) {
            if (shouldSuppressFullScreenIntent(notification.getKey())) {
                Log.d("PhoneStatusBar", "No Fullscreen intent: suppressed by DND: " + notification.getKey());
            } else if (this.mNotificationData.getImportance(notification.getKey()) < 5) {
                Log.d("PhoneStatusBar", "No Fullscreen intent: not important enough: " + notification.getKey());
            } else {
                awakenDreams();
                Log.d("PhoneStatusBar", "Notification has fullScreenIntent; sending fullScreenIntent");
                try {
                    EventLog.writeEvent(36002, notification.getKey());
                    notification.getNotification().fullScreenIntent.send();
                    shadeEntry.notifyFullScreenIntentLaunched();
                    MetricsLogger.count(this.mContext, "note_fullscreen", 1);
                } catch (PendingIntent.CanceledException e) {
                }
            }
        }
        addNotificationViews(shadeEntry, ranking);
        setAreThereNotifications();
    }

    private boolean shouldSuppressFullScreenIntent(String key) {
        if (isDeviceInVrMode()) {
            return true;
        }
        if (this.mPowerManager.isInteractive()) {
            return this.mNotificationData.shouldSuppressScreenOn(key);
        }
        return this.mNotificationData.shouldSuppressScreenOff(key);
    }

    @Override
    protected void updateNotificationRanking(NotificationListenerService.RankingMap ranking) {
        this.mNotificationData.updateRanking(ranking);
        updateNotifications();
    }

    @Override
    public void removeNotification(String key, NotificationListenerService.RankingMap ranking) {
        CharSequence[] newHistory;
        boolean deferRemoval = false;
        if (this.mHeadsUpManager.isHeadsUp(key)) {
            boolean ignoreEarliestRemovalTime = this.mRemoteInputController.isSpinning(key) && !FORCE_REMOTE_INPUT_HISTORY;
            deferRemoval = !this.mHeadsUpManager.removeNotification(key, ignoreEarliestRemovalTime);
        }
        if (key.equals(this.mMediaNotificationKey)) {
            clearCurrentMediaNotification();
            updateMediaMetaData(true, true);
        }
        if (FORCE_REMOTE_INPUT_HISTORY && this.mRemoteInputController.isSpinning(key)) {
            NotificationData.Entry entry = this.mNotificationData.get(key);
            StatusBarNotification sbn = entry.notification;
            Notification.Builder b = Notification.Builder.recoverBuilder(this.mContext, sbn.getNotification().clone());
            CharSequence[] oldHistory = sbn.getNotification().extras.getCharSequenceArray("android.remoteInputHistory");
            if (oldHistory == null) {
                newHistory = new CharSequence[1];
            } else {
                newHistory = new CharSequence[oldHistory.length + 1];
                for (int i = 0; i < oldHistory.length; i++) {
                    newHistory[i + 1] = oldHistory[i];
                }
            }
            newHistory[0] = String.valueOf(entry.remoteInputText);
            b.setRemoteInputHistory(newHistory);
            Notification newNotification = b.build();
            newNotification.contentView = sbn.getNotification().contentView;
            newNotification.bigContentView = sbn.getNotification().bigContentView;
            newNotification.headsUpContentView = sbn.getNotification().headsUpContentView;
            StatusBarNotification newSbn = new StatusBarNotification(sbn.getPackageName(), sbn.getOpPkg(), sbn.getId(), sbn.getTag(), sbn.getUid(), sbn.getInitialPid(), 0, newNotification, sbn.getUser(), sbn.getPostTime());
            updateNotification(newSbn, null);
            this.mKeysKeptForRemoteInput.add(entry.key);
            return;
        }
        if (deferRemoval) {
            this.mLatestRankingMap = ranking;
            this.mHeadsUpEntriesToRemoveOnSwitch.add(this.mHeadsUpManager.getEntry(key));
            return;
        }
        NotificationData.Entry entry2 = this.mNotificationData.get(key);
        if (entry2 != null && this.mRemoteInputController.isRemoteInputActive(entry2)) {
            this.mLatestRankingMap = ranking;
            this.mRemoteInputEntriesToRemoveOnCollapse.add(entry2);
            return;
        }
        if (entry2 != null && entry2.row != null) {
            entry2.row.setRemoved();
        }
        handleGroupSummaryRemoved(key, ranking);
        StatusBarNotification old = removeNotificationViews(key, ranking);
        Log.d("PhoneStatusBar", "removeNotification key=" + key + " old=" + old);
        if (old != null && !hasActiveNotifications() && !this.mNotificationPanel.isTracking() && !this.mNotificationPanel.isQsExpanded()) {
            if (this.mState == 0) {
                animateCollapsePanels();
            } else if (this.mState == 2) {
                goToKeyguard();
            }
        }
        setAreThereNotifications();
    }

    private void handleGroupSummaryRemoved(String key, NotificationListenerService.RankingMap ranking) {
        NotificationData.Entry entry = this.mNotificationData.get(key);
        if (entry == null || entry.row == null || !entry.row.isSummaryWithChildren()) {
            return;
        }
        if (entry.notification.getOverrideGroupKey() != null && !entry.row.isDismissed()) {
            return;
        }
        List<ExpandableNotificationRow> notificationChildren = entry.row.getNotificationChildren();
        ArrayList<ExpandableNotificationRow> toRemove = new ArrayList<>(notificationChildren);
        for (int i = 0; i < toRemove.size(); i++) {
            toRemove.get(i).setKeepInParent(true);
            toRemove.get(i).setRemoved();
        }
        for (int i2 = 0; i2 < toRemove.size(); i2++) {
            removeNotification(toRemove.get(i2).getStatusBarNotification().getKey(), ranking);
            this.mStackScroller.removeViewStateForView(toRemove.get(i2));
        }
    }

    @Override
    protected void performRemoveNotification(StatusBarNotification n, boolean removeView) {
        NotificationData.Entry entry = this.mNotificationData.get(n.getKey());
        if (this.mRemoteInputController.isRemoteInputActive(entry)) {
            this.mRemoteInputController.removeRemoteInput(entry);
        }
        super.performRemoveNotification(n, removeView);
    }

    @Override
    protected void refreshLayout(int layoutDirection) {
        if (this.mNavigationBarView == null) {
            return;
        }
        this.mNavigationBarView.setLayoutDirection(layoutDirection);
    }

    public void updateNotificationShade() {
        if (this.mStackScroller == null) {
            return;
        }
        if (isCollapsing()) {
            addPostCollapseAction(new Runnable() {
                @Override
                public void run() {
                    PhoneStatusBar.this.updateNotificationShade();
                }
            });
            return;
        }
        ArrayList<NotificationData.Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
        ArrayList<ExpandableNotificationRow> toShow = new ArrayList<>(activeNotifications.size());
        int N = activeNotifications.size();
        for (int i = 0; i < N; i++) {
            NotificationData.Entry ent = activeNotifications.get(i);
            int vis = ent.notification.getNotification().visibility;
            boolean hideSensitive = !userAllowsPrivateNotificationsInPublic(ent.notification.getUserId());
            boolean sensitiveNote = vis == 0;
            boolean sensitivePackage = packageHasVisibilityOverride(ent.notification.getKey());
            boolean sensitive = (sensitiveNote && hideSensitive) ? true : sensitivePackage;
            boolean showingPublic = sensitive ? isLockscreenPublicMode() : false;
            if (showingPublic) {
                updatePublicContentView(ent, ent.notification);
            }
            ent.row.setSensitive(sensitive, hideSensitive);
            if (ent.autoRedacted && ent.legacy) {
                if (showingPublic) {
                    ent.row.setShowingLegacyBackground(false);
                } else {
                    ent.row.setShowingLegacyBackground(true);
                }
            }
            if (this.mGroupManager.isChildInGroupWithSummary(ent.row.getStatusBarNotification())) {
                ExpandableNotificationRow summary = this.mGroupManager.getGroupSummary(ent.row.getStatusBarNotification());
                List<ExpandableNotificationRow> orderedChildren = this.mTmpChildOrderMap.get(summary);
                if (orderedChildren == null) {
                    orderedChildren = new ArrayList<>();
                    this.mTmpChildOrderMap.put(summary, orderedChildren);
                }
                orderedChildren.add(ent.row);
            } else {
                toShow.add(ent.row);
            }
        }
        ArrayList<ExpandableNotificationRow> toRemove = new ArrayList<>();
        for (int i2 = 0; i2 < this.mStackScroller.getChildCount(); i2++) {
            View child = this.mStackScroller.getChildAt(i2);
            if (!toShow.contains(child) && (child instanceof ExpandableNotificationRow)) {
                toRemove.add((ExpandableNotificationRow) child);
            }
        }
        for (ExpandableNotificationRow remove : toRemove) {
            if (this.mGroupManager.isChildInGroupWithSummary(remove.getStatusBarNotification())) {
                this.mStackScroller.setChildTransferInProgress(true);
            }
            if (remove.isSummaryWithChildren()) {
                remove.removeAllChildren();
            }
            this.mStackScroller.removeView(remove);
            this.mStackScroller.setChildTransferInProgress(false);
        }
        removeNotificationChildren();
        for (int i3 = 0; i3 < toShow.size(); i3++) {
            ExpandableNotificationRow v = toShow.get(i3);
            if (v.getParent() == null) {
                this.mStackScroller.addView(v);
            }
        }
        addNotificationChildrenAndSort();
        int j = 0;
        for (int i4 = 0; i4 < this.mStackScroller.getChildCount(); i4++) {
            View child2 = this.mStackScroller.getChildAt(i4);
            if (child2 instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow targetChild = toShow.get(j);
                if (child2 != targetChild) {
                    this.mStackScroller.changeViewPosition(targetChild, i4);
                }
                j++;
            }
        }
        this.mTmpChildOrderMap.clear();
        updateRowStates();
        updateSpeedbump();
        updateClearAll();
        updateEmptyShadeView();
        updateQsExpansionEnabled();
        this.mShadeUpdates.check();
    }

    private void updateQsExpansionEnabled() {
        boolean z = false;
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        if (isDeviceProvisioned() && ((this.mUserSetup || this.mUserSwitcherController == null || !this.mUserSwitcherController.isSimpleUserSwitcher()) && (this.mDisabled2 & 1) == 0 && !ONLY_CORE_APPS)) {
            z = true;
        }
        notificationPanelView.setQsExpansionEnabled(z);
    }

    private void addNotificationChildrenAndSort() {
        boolean orderChanged = false;
        for (int i = 0; i < this.mStackScroller.getChildCount(); i++) {
            View view = this.mStackScroller.getChildAt(i);
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow parent = (ExpandableNotificationRow) view;
                List<ExpandableNotificationRow> children = parent.getNotificationChildren();
                List<ExpandableNotificationRow> orderedChildren = this.mTmpChildOrderMap.get(parent);
                for (int childIndex = 0; orderedChildren != null && childIndex < orderedChildren.size(); childIndex++) {
                    ExpandableNotificationRow childView = orderedChildren.get(childIndex);
                    if (children == null || !children.contains(childView)) {
                        parent.addChildNotification(childView, childIndex);
                        this.mStackScroller.notifyGroupChildAdded(childView);
                    }
                }
                orderChanged |= parent.applyChildOrder(orderedChildren);
            }
        }
        if (!orderChanged) {
            return;
        }
        this.mStackScroller.generateChildOrderChangedEvent();
    }

    private void removeNotificationChildren() {
        ArrayList<ExpandableNotificationRow> toRemove = new ArrayList<>();
        for (int i = 0; i < this.mStackScroller.getChildCount(); i++) {
            View view = this.mStackScroller.getChildAt(i);
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow parent = (ExpandableNotificationRow) view;
                List<ExpandableNotificationRow> children = parent.getNotificationChildren();
                List<ExpandableNotificationRow> orderedChildren = this.mTmpChildOrderMap.get(parent);
                if (children != null) {
                    toRemove.clear();
                    for (ExpandableNotificationRow childRow : children) {
                        if (orderedChildren == null || !orderedChildren.contains(childRow)) {
                            if (!childRow.keepInParent()) {
                                toRemove.add(childRow);
                            }
                        }
                    }
                    for (ExpandableNotificationRow remove : toRemove) {
                        parent.removeChildNotification(remove);
                        if (this.mNotificationData.get(remove.getStatusBarNotification().getKey()) == null) {
                            this.mStackScroller.notifyGroupChildRemoved(remove, parent.getChildrenContainer());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addQsTile(ComponentName tile) {
        this.mQSPanel.getHost().addTile(tile);
    }

    @Override
    public void remQsTile(ComponentName tile) {
        this.mQSPanel.getHost().removeTile(tile);
    }

    @Override
    public void clickTile(ComponentName tile) {
        this.mQSPanel.clickTile(tile);
    }

    private boolean packageHasVisibilityOverride(String key) {
        return this.mNotificationData.getVisibilityOverride(key) == 0;
    }

    private void updateClearAll() {
        boolean zHasActiveClearableNotifications;
        if (this.mState == 1) {
            zHasActiveClearableNotifications = false;
        } else {
            zHasActiveClearableNotifications = this.mNotificationData.hasActiveClearableNotifications();
        }
        this.mStackScroller.updateDismissView(zHasActiveClearableNotifications);
    }

    private void updateEmptyShadeView() {
        boolean showEmptyShade = this.mState != 1 && this.mNotificationData.getActiveNotifications().size() == 0;
        this.mNotificationPanel.setShadeEmpty(showEmptyShade);
    }

    private void updateSpeedbump() {
        int speedbumpIndex = -1;
        int currentIndex = 0;
        int N = this.mStackScroller.getChildCount();
        int i = 0;
        while (true) {
            if (i >= N) {
                break;
            }
            View view = this.mStackScroller.getChildAt(i);
            if (view.getVisibility() != 8 && (view instanceof ExpandableNotificationRow)) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) view;
                if (this.mNotificationData.isAmbient(row.getStatusBarNotification().getKey())) {
                    speedbumpIndex = currentIndex;
                    break;
                }
                currentIndex++;
            }
            i++;
        }
        this.mStackScroller.updateSpeedBumpIndex(speedbumpIndex);
    }

    public static boolean isTopLevelChild(NotificationData.Entry entry) {
        return entry.row.getParent() instanceof NotificationStackScrollLayout;
    }

    @Override
    protected void updateNotifications() {
        this.mNotificationData.filterAndSort();
        updateNotificationShade();
        this.mIconController.updateNotificationIcons(this.mNotificationData);
    }

    public void requestNotificationUpdate() {
        updateNotifications();
    }

    @Override
    protected void setAreThereNotifications() {
        final View nlo = this.mStatusBarView.findViewById(R.id.notification_lights_out);
        boolean showDot = hasActiveNotifications() && !areLightsOn();
        if (showDot != (nlo.getAlpha() == 1.0f)) {
            if (showDot) {
                nlo.setAlpha(0.0f);
                nlo.setVisibility(0);
            }
            nlo.animate().alpha(showDot ? 1 : 0).setDuration(showDot ? 750 : 250).setInterpolator(new AccelerateInterpolator(2.0f)).setListener(showDot ? null : new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator _a) {
                    nlo.setVisibility(8);
                }
            }).start();
        }
        findAndUpdateMediaNotifications();
        updateCarrierLabelVisibility(false);
    }

    public void findAndUpdateMediaNotifications() {
        MediaSession.Token token;
        boolean metaDataChanged = false;
        synchronized (this.mNotificationData) {
            ArrayList<NotificationData.Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
            int N = activeNotifications.size();
            NotificationData.Entry mediaNotification = null;
            MediaController controller = null;
            int i = 0;
            while (true) {
                if (i >= N) {
                    break;
                }
                NotificationData.Entry entry = activeNotifications.get(i);
                if (isMediaNotification(entry) && (token = (MediaSession.Token) entry.notification.getNotification().extras.getParcelable("android.mediaSession")) != null) {
                    MediaController aController = new MediaController(this.mContext, token);
                    if (3 == getMediaControllerPlaybackState(aController)) {
                        mediaNotification = entry;
                        controller = aController;
                        break;
                    }
                }
                i++;
            }
            if (mediaNotification == null && this.mMediaSessionManager != null) {
                List<MediaController> sessions = this.mMediaSessionManager.getActiveSessionsForUser(null, -1);
                for (MediaController aController2 : sessions) {
                    if (3 == getMediaControllerPlaybackState(aController2)) {
                        String pkg = aController2.getPackageName();
                        int i2 = 0;
                        while (true) {
                            if (i2 < N) {
                                NotificationData.Entry entry2 = activeNotifications.get(i2);
                                if (!entry2.notification.getPackageName().equals(pkg)) {
                                    i2++;
                                } else {
                                    controller = aController2;
                                    mediaNotification = entry2;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (controller != null && !sameSessions(this.mMediaController, controller)) {
                clearCurrentMediaNotification();
                this.mMediaController = controller;
                this.mMediaController.registerCallback(this.mMediaListener);
                this.mMediaMetadata = this.mMediaController.getMetadata();
                if (mediaNotification != null) {
                    this.mMediaNotificationKey = mediaNotification.notification.getKey();
                }
                metaDataChanged = true;
            }
        }
        if (metaDataChanged) {
            updateNotifications();
        }
        updateMediaMetaData(metaDataChanged, true);
    }

    private int getMediaControllerPlaybackState(MediaController controller) {
        PlaybackState playbackState;
        if (controller != null && (playbackState = controller.getPlaybackState()) != null) {
            return playbackState.getState();
        }
        return 0;
    }

    public boolean isPlaybackActive(int state) {
        return (state == 1 || state == 7 || state == 0) ? false : true;
    }

    public void clearCurrentMediaNotification() {
        this.mMediaNotificationKey = null;
        this.mMediaMetadata = null;
        if (this.mMediaController != null) {
            this.mMediaController.unregisterCallback(this.mMediaListener);
        }
        this.mMediaController = null;
    }

    private boolean sameSessions(MediaController a, MediaController b) {
        if (a == b) {
            return true;
        }
        if (a == null) {
            return false;
        }
        return a.controlsSameSession(b);
    }

    public void updateMediaMetaData(boolean metaDataChanged, boolean allowEnterAnimation) {
        Bitmap lockWallpaper;
        if (this.mBackdrop == null) {
            return;
        }
        if (this.mLaunchTransitionFadingAway) {
            this.mBackdrop.setVisibility(4);
            return;
        }
        Drawable artworkDrawable = null;
        if (this.mMediaMetadata != null) {
            Bitmap artworkBitmap = this.mMediaMetadata.getBitmap("android.media.metadata.ART");
            if (artworkBitmap == null) {
                artworkBitmap = this.mMediaMetadata.getBitmap("android.media.metadata.ALBUM_ART");
            }
            if (artworkBitmap != null) {
                artworkDrawable = new BitmapDrawable(this.mBackdropBack.getResources(), artworkBitmap);
            }
        }
        boolean allowWhenShade = false;
        if (artworkDrawable == null && (lockWallpaper = this.mLockscreenWallpaper.getBitmap()) != null) {
            artworkDrawable = new LockscreenWallpaper.WallpaperDrawable(this.mBackdropBack.getResources(), lockWallpaper);
            allowWhenShade = this.mStatusBarKeyguardViewManager != null ? this.mStatusBarKeyguardViewManager.isShowing() : false;
        }
        boolean zIsOccluded = this.mStatusBarKeyguardViewManager != null ? this.mStatusBarKeyguardViewManager.isOccluded() : false;
        boolean hasArtwork = artworkDrawable != null;
        if (!hasArtwork || ((this.mState == 0 && !allowWhenShade) || this.mFingerprintUnlockController.getMode() == 2 || zIsOccluded)) {
            if (this.mBackdrop.getVisibility() != 8) {
                if (this.mFingerprintUnlockController.getMode() == 2 || zIsOccluded) {
                    this.mBackdrop.setVisibility(8);
                    this.mBackdropBack.setImageDrawable(null);
                    this.mStatusBarWindowManager.setBackdropShowing(false);
                    return;
                } else {
                    this.mStatusBarWindowManager.setBackdropShowing(false);
                    this.mBackdrop.animate().alpha(0.002f).setInterpolator(Interpolators.ACCELERATE_DECELERATE).setDuration(300L).setStartDelay(0L).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            PhoneStatusBar.this.mBackdrop.setVisibility(8);
                            PhoneStatusBar.this.mBackdropFront.animate().cancel();
                            PhoneStatusBar.this.mBackdropBack.setImageDrawable(null);
                            PhoneStatusBar.this.mHandler.post(PhoneStatusBar.this.mHideBackdropFront);
                        }
                    });
                    if (this.mKeyguardFadingAway) {
                        this.mBackdrop.animate().setDuration(this.mKeyguardFadingAwayDuration / 2).setStartDelay(this.mKeyguardFadingAwayDelay).setInterpolator(Interpolators.LINEAR).start();
                        return;
                    }
                    return;
                }
            }
            return;
        }
        if (this.mBackdrop.getVisibility() != 0) {
            this.mBackdrop.setVisibility(0);
            if (allowEnterAnimation) {
                this.mBackdrop.animate().alpha(1.0f).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        PhoneStatusBar.this.mStatusBarWindowManager.setBackdropShowing(true);
                    }
                });
            } else {
                this.mBackdrop.animate().cancel();
                this.mBackdrop.setAlpha(1.0f);
                this.mStatusBarWindowManager.setBackdropShowing(true);
            }
            metaDataChanged = true;
        }
        if (metaDataChanged) {
            if (this.mBackdropBack.getDrawable() != null) {
                Drawable drawable = this.mBackdropBack.getDrawable().getConstantState().newDrawable(this.mBackdropFront.getResources()).mutate();
                this.mBackdropFront.setImageDrawable(drawable);
                if (this.mScrimSrcModeEnabled) {
                    this.mBackdropFront.getDrawable().mutate().setXfermode(this.mSrcOverXferMode);
                }
                this.mBackdropFront.setAlpha(1.0f);
                this.mBackdropFront.setVisibility(0);
            } else {
                this.mBackdropFront.setVisibility(4);
            }
            this.mBackdropBack.setImageDrawable(artworkDrawable);
            if (this.mScrimSrcModeEnabled) {
                this.mBackdropBack.getDrawable().mutate().setXfermode(this.mSrcXferMode);
            }
            if (this.mBackdropFront.getVisibility() == 0) {
                this.mBackdropFront.animate().setDuration(250L).alpha(0.0f).withEndAction(this.mHideBackdropFront);
            }
        }
    }

    protected int adjustDisableFlags(int state) {
        if (!this.mLaunchTransitionFadingAway && !this.mKeyguardFadingAway) {
            if (this.mExpandedVisible || this.mBouncerShowing || this.mWaitingForKeyguardExit) {
                return state | 131072 | 1048576;
            }
            return state;
        }
        return state;
    }

    @Override
    public void disable(int state1, int state2, boolean animate) {
        boolean animate2 = animate & (this.mStatusBarWindowState != 2);
        this.mDisabledUnmodified1 = state1;
        this.mDisabledUnmodified2 = state2;
        int state12 = adjustDisableFlags(state1);
        int old1 = this.mDisabled1;
        int diff1 = state12 ^ old1;
        this.mDisabled1 = state12;
        int old2 = this.mDisabled2;
        int diff2 = state2 ^ old2;
        this.mDisabled2 = state2;
        Log.d("PhoneStatusBar", String.format("disable1: 0x%08x -> 0x%08x (diff1: 0x%08x)", Integer.valueOf(old1), Integer.valueOf(state12), Integer.valueOf(diff1)));
        Log.d("PhoneStatusBar", String.format("disable2: 0x%08x -> 0x%08x (diff2: 0x%08x)", Integer.valueOf(old2), Integer.valueOf(state2), Integer.valueOf(diff2)));
        StringBuilder flagdbg = new StringBuilder();
        flagdbg.append("disable: < ");
        flagdbg.append((65536 & state12) != 0 ? "EXPAND" : "expand");
        flagdbg.append((65536 & diff1) != 0 ? "* " : " ");
        flagdbg.append((131072 & state12) != 0 ? "ICONS" : "icons");
        flagdbg.append((131072 & diff1) != 0 ? "* " : " ");
        flagdbg.append((262144 & state12) != 0 ? "ALERTS" : "alerts");
        flagdbg.append((262144 & diff1) != 0 ? "* " : " ");
        flagdbg.append((1048576 & state12) != 0 ? "SYSTEM_INFO" : "system_info");
        flagdbg.append((1048576 & diff1) != 0 ? "* " : " ");
        flagdbg.append((4194304 & state12) != 0 ? "BACK" : "back");
        flagdbg.append((4194304 & diff1) != 0 ? "* " : " ");
        flagdbg.append((2097152 & state12) != 0 ? "HOME" : "home");
        flagdbg.append((2097152 & diff1) != 0 ? "* " : " ");
        flagdbg.append((16777216 & state12) != 0 ? "RECENT" : "recent");
        flagdbg.append((16777216 & diff1) != 0 ? "* " : " ");
        flagdbg.append((8388608 & state12) != 0 ? "CLOCK" : "clock");
        flagdbg.append((8388608 & diff1) != 0 ? "* " : " ");
        flagdbg.append((33554432 & state12) != 0 ? "SEARCH" : "search");
        flagdbg.append((33554432 & diff1) != 0 ? "* " : " ");
        flagdbg.append((state2 & 1) != 0 ? "QUICK_SETTINGS" : "quick_settings");
        flagdbg.append((diff2 & 1) != 0 ? "* " : " ");
        flagdbg.append(">");
        Log.d("PhoneStatusBar", flagdbg.toString());
        if ((1048576 & diff1) != 0) {
            if ((1048576 & state12) != 0) {
                this.mIconController.hideSystemIconArea(animate2);
                this.mStatusBarPlmnPlugin.setPlmnVisibility(8);
            } else {
                this.mIconController.showSystemIconArea(animate2);
                this.mStatusBarPlmnPlugin.setPlmnVisibility(0);
            }
        }
        if ((8388608 & diff1) != 0) {
            boolean visible = (8388608 & state12) == 0;
            this.mIconController.setClockVisibility(visible);
        }
        if ((65536 & diff1) != 0 && (65536 & state12) != 0) {
            animateCollapsePanels();
        }
        if ((56623104 & diff1) != 0) {
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setDisabledFlags(state12);
            }
            if ((16777216 & state12) != 0) {
                this.mHandler.removeMessages(1020);
                this.mHandler.sendEmptyMessage(1020);
            }
        }
        if ((131072 & diff1) != 0) {
            if ((131072 & state12) != 0) {
                this.mIconController.hideNotificationIconArea(animate2);
            } else {
                this.mIconController.showNotificationIconArea(animate2);
            }
        }
        if ((262144 & diff1) != 0) {
            this.mDisableNotificationAlerts = (262144 & state12) != 0;
            this.mHeadsUpObserver.onChange(true);
        }
        if ((diff2 & 1) == 0) {
            return;
        }
        updateQsExpansionEnabled();
    }

    @Override
    protected BaseStatusBar.H createHandler() {
        return new H(this, null);
    }

    @Override
    public void startActivity(Intent intent, boolean dismissShade) {
        startActivityDismissingKeyguard(intent, false, dismissShade);
    }

    @Override
    public void startActivity(Intent intent, boolean dismissShade, ActivityStarter.Callback callback) {
        startActivityDismissingKeyguard(intent, false, dismissShade, callback);
    }

    @Override
    public void preventNextAnimation() {
        overrideActivityPendingAppTransition(true);
    }

    public void setQsExpanded(boolean expanded) {
        int i;
        this.mStatusBarWindowManager.setQsExpanded(expanded);
        View view = this.mKeyguardStatusView;
        if (expanded) {
            i = 4;
        } else {
            i = 0;
        }
        view.setImportantForAccessibility(i);
    }

    public boolean isGoingToNotificationShade() {
        return this.mLeaveOpenOnKeyguardHide;
    }

    public boolean isWakeUpComingFromTouch() {
        return this.mWakeUpComingFromTouch;
    }

    public boolean isFalsingThresholdNeeded() {
        return getBarState() == 1;
    }

    public boolean isDozing() {
        return this.mDozing;
    }

    @Override
    public String getCurrentMediaNotificationKey() {
        return this.mMediaNotificationKey;
    }

    public boolean isScrimSrcModeEnabled() {
        return this.mScrimSrcModeEnabled;
    }

    public void onKeyguardViewManagerStatesUpdated() {
        logStateToEventlog();
    }

    @Override
    public void onUnlockMethodStateChanged() {
        logStateToEventlog();
    }

    @Override
    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
        if (inPinnedMode) {
            this.mStatusBarWindowManager.setHeadsUpShowing(true);
            this.mStatusBarWindowManager.setForceStatusBarVisible(true);
            if (!this.mNotificationPanel.isFullyCollapsed()) {
                return;
            }
            this.mNotificationPanel.requestLayout();
            this.mStatusBarWindowManager.setForceWindowCollapsed(true);
            this.mNotificationPanel.post(new Runnable() {
                @Override
                public void run() {
                    PhoneStatusBar.this.mStatusBarWindowManager.setForceWindowCollapsed(false);
                }
            });
            return;
        }
        if (!this.mNotificationPanel.isFullyCollapsed() || this.mNotificationPanel.isTracking()) {
            this.mStatusBarWindowManager.setHeadsUpShowing(false);
        } else {
            this.mHeadsUpManager.setHeadsUpGoingAway(true);
            this.mStackScroller.runAfterAnimationFinished(new Runnable() {
                @Override
                public void run() {
                    if (PhoneStatusBar.this.mHeadsUpManager.hasPinnedHeadsUp()) {
                        return;
                    }
                    PhoneStatusBar.this.mStatusBarWindowManager.setHeadsUpShowing(false);
                    PhoneStatusBar.this.mHeadsUpManager.setHeadsUpGoingAway(false);
                }
            });
        }
    }

    @Override
    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
        dismissVolumeDialog();
    }

    @Override
    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
    }

    @Override
    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
        if (!isHeadsUp && this.mHeadsUpEntriesToRemoveOnSwitch.contains(entry)) {
            removeNotification(entry.key, this.mLatestRankingMap);
            this.mHeadsUpEntriesToRemoveOnSwitch.remove(entry);
            if (!this.mHeadsUpEntriesToRemoveOnSwitch.isEmpty()) {
                return;
            }
            this.mLatestRankingMap = null;
            return;
        }
        updateNotificationRanking(null);
    }

    @Override
    protected void updateHeadsUp(String key, NotificationData.Entry entry, boolean shouldPeek, boolean alertAgain) {
        boolean wasHeadsUp = isHeadsUp(key);
        if (wasHeadsUp) {
            if (!shouldPeek) {
                this.mHeadsUpManager.removeNotification(key, false);
                return;
            } else {
                this.mHeadsUpManager.updateNotification(entry, alertAgain);
                return;
            }
        }
        if (!this.mUseHeadsUp || !shouldPeek || !alertAgain) {
            return;
        }
        this.mHeadsUpManager.showNotification(entry);
    }

    @Override
    protected void setHeadsUpUser(int newUserId) {
        if (this.mHeadsUpManager == null) {
            return;
        }
        this.mHeadsUpManager.setUser(newUserId);
    }

    public boolean isHeadsUp(String key) {
        return this.mHeadsUpManager.isHeadsUp(key);
    }

    @Override
    protected boolean isSnoozedPackage(StatusBarNotification sbn) {
        return this.mHeadsUpManager.isSnoozed(sbn.getPackageName());
    }

    public boolean isKeyguardCurrentlySecure() {
        return !this.mUnlockMethodCache.canSkipBouncer();
    }

    public void setPanelExpanded(boolean isExpanded) {
        this.mStatusBarWindowManager.setPanelExpanded(isExpanded);
        if (isExpanded && getBarState() != 1) {
            Log.v("PhoneStatusBar", "clearing notification effects from setPanelExpanded");
            clearNotificationEffects();
        }
        if (isExpanded) {
            return;
        }
        removeRemoteInputEntriesKeptUntilCollapsed();
    }

    private void removeRemoteInputEntriesKeptUntilCollapsed() {
        for (int i = 0; i < this.mRemoteInputEntriesToRemoveOnCollapse.size(); i++) {
            NotificationData.Entry entry = this.mRemoteInputEntriesToRemoveOnCollapse.valueAt(i);
            this.mRemoteInputController.removeRemoteInput(entry);
            removeNotification(entry.key, this.mLatestRankingMap);
        }
        this.mRemoteInputEntriesToRemoveOnCollapse.clear();
    }

    public void onScreenTurnedOff() {
        this.mFalsingManager.onScreenOff();
    }

    private class H extends BaseStatusBar.H {
        H(PhoneStatusBar this$0, H h) {
            this();
        }

        private H() {
            super();
        }

        @Override
        public void handleMessage(Message m) {
            super.handleMessage(m);
            switch (m.what) {
                case 1000:
                    PhoneStatusBar.this.animateExpandNotificationsPanel();
                    break;
                case 1001:
                    PhoneStatusBar.this.animateCollapsePanels();
                    break;
                case 1002:
                    PhoneStatusBar.this.animateExpandSettingsPanel((String) m.obj);
                    break;
                case 1003:
                    PhoneStatusBar.this.onLaunchTransitionTimeout();
                    break;
            }
        }
    }

    @Override
    public void maybeEscalateHeadsUp() {
        Collection<HeadsUpManager.HeadsUpEntry> entries = this.mHeadsUpManager.getAllEntries();
        for (HeadsUpManager.HeadsUpEntry entry : entries) {
            StatusBarNotification sbn = entry.entry.notification;
            Notification notification = sbn.getNotification();
            if (notification.fullScreenIntent != null) {
                Log.d("PhoneStatusBar", "converting a heads up to fullScreen");
                try {
                    EventLog.writeEvent(36003, sbn.getKey());
                    notification.fullScreenIntent.send();
                    entry.entry.notifyFullScreenIntentLaunched();
                } catch (PendingIntent.CanceledException e) {
                }
            }
        }
        this.mHeadsUpManager.releaseAllImmediately();
    }

    boolean panelsEnabled() {
        return (this.mDisabled1 & 65536) == 0 && !ONLY_CORE_APPS && BenesseExtension.getDchaState() == 0;
    }

    void makeExpandedVisible(boolean force) {
        if (!force && (this.mExpandedVisible || !panelsEnabled())) {
            return;
        }
        this.mExpandedVisible = true;
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setSlippery(true);
        }
        updateCarrierLabelVisibility(true);
        this.mStatusBarWindowManager.setPanelVisible(true);
        visibilityChanged(true);
        this.mWaitingForKeyguardExit = false;
        disable(this.mDisabledUnmodified1, this.mDisabledUnmodified2, force ? false : true);
        setInteracting(1, true);
    }

    public void animateCollapsePanels() {
        animateCollapsePanels(0);
    }

    public void postAnimateCollapsePanels() {
        this.mHandler.post(this.mAnimateCollapsePanels);
    }

    public void postAnimateOpenPanels() {
        this.mHandler.sendEmptyMessage(1002);
    }

    @Override
    public void animateCollapsePanels(int flags) {
        animateCollapsePanels(flags, false, false, 1.0f);
    }

    @Override
    public void animateCollapsePanels(int flags, boolean force) {
        animateCollapsePanels(flags, force, false, 1.0f);
    }

    @Override
    public void animateCollapsePanels(int flags, boolean force, boolean delayed) {
        animateCollapsePanels(flags, force, delayed, 1.0f);
    }

    public void animateCollapsePanels(int flags, boolean force, boolean delayed, float speedUpFactor) {
        if (!force && this.mState != 0) {
            runPostCollapseRunnables();
            return;
        }
        if ((flags & 2) == 0 && !this.mHandler.hasMessages(1020)) {
            this.mHandler.removeMessages(1020);
            this.mHandler.sendEmptyMessage(1020);
        }
        if (this.mStatusBarWindow == null) {
            return;
        }
        this.mStatusBarWindowManager.setStatusBarFocusable(false);
        this.mStatusBarWindow.cancelExpandHelper();
        this.mStatusBarView.collapsePanel(true, delayed, speedUpFactor);
    }

    private void runPostCollapseRunnables() {
        ArrayList<Runnable> clonedList = new ArrayList<>(this.mPostCollapseRunnables);
        this.mPostCollapseRunnables.clear();
        int size = clonedList.size();
        for (int i = 0; i < size; i++) {
            clonedList.get(i).run();
        }
    }

    @Override
    public void animateExpandNotificationsPanel() {
        if (!panelsEnabled()) {
            return;
        }
        this.mNotificationPanel.expand(true);
    }

    @Override
    public void animateExpandSettingsPanel(String subPanel) {
        if (panelsEnabled() && this.mUserSetup) {
            if (subPanel != null) {
                this.mQSPanel.openDetails(subPanel);
            }
            this.mNotificationPanel.expandWithQs();
        }
    }

    public void animateCollapseQuickSettings() {
        if (this.mState != 0) {
            return;
        }
        this.mStatusBarView.collapsePanel(true, false, 1.0f);
    }

    void makeExpandedInvisible() {
        if (!this.mExpandedVisible || this.mStatusBarWindow == null) {
            return;
        }
        this.mStatusBarView.collapsePanel(false, false, 1.0f);
        this.mNotificationPanel.closeQs();
        this.mExpandedVisible = false;
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setSlippery(false);
        }
        visibilityChanged(false);
        this.mStatusBarWindowManager.setPanelVisible(false);
        this.mStatusBarWindowManager.setForceStatusBarVisible(false);
        dismissPopups();
        runPostCollapseRunnables();
        setInteracting(1, false);
        showBouncer();
        disable(this.mDisabledUnmodified1, this.mDisabledUnmodified2, true);
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            return;
        }
        WindowManagerGlobal.getInstance().trimMemory(20);
    }

    public boolean interceptTouchEvent(MotionEvent event) {
        if (event.getAction() != 2) {
            Log.d("PhoneStatusBar", String.format("panel: %s at (%f, %f) mDisabled1=0x%08x mDisabled2=0x%08x", MotionEvent.actionToString(event.getAction()), Float.valueOf(event.getRawX()), Float.valueOf(event.getRawY()), Integer.valueOf(this.mDisabled1), Integer.valueOf(this.mDisabled2)));
        }
        if (this.mStatusBarWindowState == 0) {
            boolean upOrCancel = event.getAction() == 1 || event.getAction() == 3;
            if (upOrCancel && !this.mExpandedVisible) {
                setInteracting(1, false);
            } else {
                setInteracting(1, true);
            }
        }
        return false;
    }

    public GestureRecorder getGestureRecorder() {
        return this.mGestureRec;
    }

    private void setNavigationIconHints(int hints) {
        if (hints == this.mNavigationIconHints) {
            return;
        }
        this.mNavigationIconHints = hints;
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setNavigationIconHints(hints);
        }
        checkBarModes();
    }

    @Override
    public void setWindowState(int window, int state) {
        boolean showing = state == 0;
        if (this.mStatusBarWindow != null && window == 1 && this.mStatusBarWindowState != state) {
            this.mStatusBarWindowState = state;
            if (!showing && this.mState == 0) {
                this.mStatusBarView.collapsePanel(false, false, 1.0f);
            }
        }
        if (this.mNavigationBarView == null || window != 2 || this.mNavigationBarWindowState == state) {
            return;
        }
        this.mNavigationBarWindowState = state;
    }

    @Override
    public void buzzBeepBlinked() {
        if (this.mDozeServiceHost == null) {
            return;
        }
        this.mDozeServiceHost.fireBuzzBeepBlinked();
    }

    @Override
    public void notificationLightOff() {
        if (this.mDozeServiceHost == null) {
            return;
        }
        this.mDozeServiceHost.fireNotificationLight(false);
    }

    @Override
    public void notificationLightPulse(int argb, int onMillis, int offMillis) {
        if (this.mDozeServiceHost == null) {
            return;
        }
        this.mDozeServiceHost.fireNotificationLight(true);
    }

    @Override
    public void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenStackBounds, Rect dockedStackBounds) {
        int oldVal = this.mSystemUiVisibility;
        int newVal = ((~mask) & oldVal) | (vis & mask);
        int diff = newVal ^ oldVal;
        Log.d("PhoneStatusBar", String.format("setSystemUiVisibility vis=%s mask=%s oldVal=%s newVal=%s diff=%s", Integer.toHexString(vis), Integer.toHexString(mask), Integer.toHexString(oldVal), Integer.toHexString(newVal), Integer.toHexString(diff)));
        boolean sbModeChanged = false;
        if (diff != 0) {
            boolean wasRecentsVisible = (this.mSystemUiVisibility & 16384) > 0;
            this.mSystemUiVisibility = newVal;
            if ((diff & 1) != 0) {
                setAreThereNotifications();
            }
            if ((268435456 & vis) != 0) {
                this.mSystemUiVisibility &= -268435457;
                this.mNoAnimationOnNextBarModeChange = true;
            }
            int sbMode = computeBarMode(oldVal, newVal, this.mStatusBarView.getBarTransitions(), 67108864, 1073741824, 8);
            int nbMode = this.mNavigationBarView == null ? -1 : computeBarMode(oldVal, newVal, this.mNavigationBarView.getBarTransitions(), 134217728, Integer.MIN_VALUE, 32768);
            sbModeChanged = sbMode != -1;
            boolean nbModeChanged = nbMode != -1;
            boolean checkBarModes = false;
            if (sbModeChanged && sbMode != this.mStatusBarMode) {
                this.mStatusBarMode = sbMode;
                checkBarModes = true;
            }
            if (nbModeChanged && nbMode != this.mNavigationBarMode) {
                this.mNavigationBarMode = nbMode;
                checkBarModes = true;
            }
            if (checkBarModes) {
                checkBarModes();
            }
            if (sbModeChanged || nbModeChanged) {
                if (this.mStatusBarMode == 1 || this.mNavigationBarMode == 1) {
                    scheduleAutohide();
                } else {
                    cancelAutohide();
                }
            }
            if ((536870912 & vis) != 0) {
                this.mSystemUiVisibility &= -536870913;
            }
            if (wasRecentsVisible) {
                this.mSystemUiVisibility |= 16384;
            }
            notifyUiVisibilityChanged(this.mSystemUiVisibility);
        }
        this.mLightStatusBarController.onSystemUiVisibilityChanged(fullscreenStackVis, dockedStackVis, mask, fullscreenStackBounds, dockedStackBounds, sbModeChanged, this.mStatusBarMode);
    }

    private int computeBarMode(int oldVis, int newVis, BarTransitions transitions, int transientFlag, int translucentFlag, int transparentFlag) {
        int oldMode = barMode(oldVis, transientFlag, translucentFlag, transparentFlag);
        int newMode = barMode(newVis, transientFlag, translucentFlag, transparentFlag);
        if (oldMode == newMode) {
            return -1;
        }
        return newMode;
    }

    private int barMode(int vis, int transientFlag, int translucentFlag, int transparentFlag) {
        int lightsOutTransparent = transparentFlag | 1;
        if ((vis & transientFlag) != 0) {
            return 1;
        }
        if ((vis & translucentFlag) != 0) {
            return 2;
        }
        if ((vis & lightsOutTransparent) == lightsOutTransparent) {
            return 6;
        }
        if ((vis & transparentFlag) != 0) {
            return 4;
        }
        return (vis & 1) != 0 ? 3 : 0;
    }

    public void checkBarModes() {
        if (this.mDemoMode) {
            return;
        }
        checkBarMode(this.mStatusBarMode, this.mStatusBarWindowState, this.mStatusBarView.getBarTransitions(), this.mNoAnimationOnNextBarModeChange);
        if (this.mNavigationBarView != null) {
            checkBarMode(this.mNavigationBarMode, this.mNavigationBarWindowState, this.mNavigationBarView.getBarTransitions(), this.mNoAnimationOnNextBarModeChange);
        }
        this.mNoAnimationOnNextBarModeChange = false;
    }

    private void checkBarMode(int mode, int windowState, BarTransitions transitions, boolean noAnimation) {
        boolean powerSave = this.mBatteryController.isPowerSave();
        boolean anim = (noAnimation || !this.mDeviceInteractive || windowState == 2 || powerSave) ? false : true;
        if (powerSave && getBarState() == 0) {
            mode = 5;
        }
        if (FeatureOptions.LOW_RAM_SUPPORT && !ActivityManager.isHighEndGfx() && getBarState() != 1 && (mode == 1 || mode == 2 || mode == 4)) {
            mode = 0;
            anim = false;
        }
        transitions.transitionTo(mode, anim);
    }

    public void finishBarAnimations() {
        this.mStatusBarView.getBarTransitions().finishAnimations();
        if (this.mNavigationBarView == null) {
            return;
        }
        this.mNavigationBarView.getBarTransitions().finishAnimations();
    }

    public void setInteracting(int barWindow, boolean interacting) {
        int i;
        boolean changing = ((this.mInteractingWindows & barWindow) != 0) != interacting;
        if (interacting) {
            i = this.mInteractingWindows | barWindow;
        } else {
            i = this.mInteractingWindows & (~barWindow);
        }
        this.mInteractingWindows = i;
        if (this.mInteractingWindows != 0) {
            suspendAutohide();
        } else {
            resumeSuspendedAutohide();
        }
        if (changing && interacting && barWindow == 2) {
            dismissVolumeDialog();
        }
        checkBarModes();
    }

    private void dismissVolumeDialog() {
        if (this.mVolumeComponent == null) {
            return;
        }
        this.mVolumeComponent.dismissNow();
    }

    private void resumeSuspendedAutohide() {
        if (!this.mAutohideSuspended) {
            return;
        }
        scheduleAutohide();
        this.mHandler.postDelayed(this.mCheckBarModes, 500L);
    }

    private void suspendAutohide() {
        this.mHandler.removeCallbacks(this.mAutohide);
        this.mHandler.removeCallbacks(this.mCheckBarModes);
        this.mAutohideSuspended = (this.mSystemUiVisibility & 201326592) != 0;
    }

    private void cancelAutohide() {
        this.mAutohideSuspended = false;
        this.mHandler.removeCallbacks(this.mAutohide);
    }

    private void scheduleAutohide() {
        cancelAutohide();
        this.mHandler.postDelayed(this.mAutohide, 3000L);
    }

    public void checkUserAutohide(View v, MotionEvent event) {
        if ((this.mSystemUiVisibility & 201326592) == 0 || event.getAction() != 4 || event.getX() != 0.0f || event.getY() != 0.0f || this.mRemoteInputController.isRemoteInputActive()) {
            return;
        }
        userAutohide();
    }

    private void userAutohide() {
        cancelAutohide();
        this.mHandler.postDelayed(this.mAutohide, 350L);
    }

    private boolean areLightsOn() {
        return (this.mSystemUiVisibility & 1) == 0;
    }

    public void setLightsOn(boolean on) {
        Log.v("PhoneStatusBar", "setLightsOn(" + on + ")");
        if (on) {
            setSystemUiVisibility(0, 0, 0, 1, this.mLastFullscreenStackBounds, this.mLastDockedStackBounds);
        } else {
            setSystemUiVisibility(1, 0, 0, 1, this.mLastFullscreenStackBounds, this.mLastDockedStackBounds);
        }
    }

    public void notifyUiVisibilityChanged(int vis) {
        try {
            if (this.mLastDispatchedSystemUiVisibility == vis) {
                return;
            }
            this.mWindowManagerService.statusBarVisibilityChanged(vis);
            this.mLastDispatchedSystemUiVisibility = vis;
        } catch (RemoteException e) {
        }
    }

    @Override
    public void topAppWindowChanged(boolean showMenu) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setMenuVisibility(showMenu);
        }
        if (showMenu) {
            setLightsOn(true);
        }
    }

    @Override
    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
        int flags;
        int flags2;
        boolean imeShown = (vis & 2) != 0;
        int flags3 = this.mNavigationIconHints;
        if (backDisposition == 2 || imeShown) {
            flags = flags3 | 1;
        } else {
            flags = flags3 & (-2);
        }
        if (showImeSwitcher) {
            flags2 = flags | 2;
        } else {
            flags2 = flags & (-3);
        }
        setNavigationIconHints(flags2);
    }

    public static String viewInfo(View v) {
        return "[(" + v.getLeft() + "," + v.getTop() + ")(" + v.getRight() + "," + v.getBottom() + ") " + v.getWidth() + "x" + v.getHeight() + "]";
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mQueueLock) {
            pw.println("Current Status Bar state:");
            pw.println("  mExpandedVisible=" + this.mExpandedVisible + ", mTrackingPosition=" + this.mTrackingPosition);
            pw.println("  mTracking=" + this.mTracking);
            pw.println("  mDisplayMetrics=" + this.mDisplayMetrics);
            pw.println("  mStackScroller: " + viewInfo(this.mStackScroller));
            pw.println("  mStackScroller: " + viewInfo(this.mStackScroller) + " scroll " + this.mStackScroller.getScrollX() + "," + this.mStackScroller.getScrollY());
        }
        pw.print("  mInteractingWindows=");
        pw.println(this.mInteractingWindows);
        pw.print("  mStatusBarWindowState=");
        pw.println(StatusBarManager.windowStateToString(this.mStatusBarWindowState));
        pw.print("  mStatusBarMode=");
        pw.println(BarTransitions.modeToString(this.mStatusBarMode));
        pw.print("  mDozing=");
        pw.println(this.mDozing);
        pw.print("  mZenMode=");
        pw.println(Settings.Global.zenModeToString(this.mZenMode));
        pw.print("  mUseHeadsUp=");
        pw.println(this.mUseHeadsUp);
        dumpBarTransitions(pw, "mStatusBarView", this.mStatusBarView.getBarTransitions());
        if (this.mNavigationBarView != null) {
            pw.print("  mNavigationBarWindowState=");
            pw.println(StatusBarManager.windowStateToString(this.mNavigationBarWindowState));
            pw.print("  mNavigationBarMode=");
            pw.println(BarTransitions.modeToString(this.mNavigationBarMode));
            dumpBarTransitions(pw, "mNavigationBarView", this.mNavigationBarView.getBarTransitions());
        }
        pw.print("  mNavigationBarView=");
        if (this.mNavigationBarView == null) {
            pw.println("null");
        } else {
            this.mNavigationBarView.dump(fd, pw, args);
        }
        pw.print("  mMediaSessionManager=");
        pw.println(this.mMediaSessionManager);
        pw.print("  mMediaNotificationKey=");
        pw.println(this.mMediaNotificationKey);
        pw.print("  mMediaController=");
        pw.print(this.mMediaController);
        if (this.mMediaController != null) {
            pw.print(" state=" + this.mMediaController.getPlaybackState());
        }
        pw.println();
        pw.print("  mMediaMetadata=");
        pw.print(this.mMediaMetadata);
        if (this.mMediaMetadata != null) {
            pw.print(" title=" + this.mMediaMetadata.getText("android.media.metadata.TITLE"));
        }
        pw.println();
        pw.println("  Panels: ");
        if (this.mNotificationPanel != null) {
            pw.println("    mNotificationPanel=" + this.mNotificationPanel + " params=" + this.mNotificationPanel.getLayoutParams().debug(""));
            pw.print("      ");
            this.mNotificationPanel.dump(fd, pw, args);
        }
        DozeLog.dump(pw);
        synchronized (this.mNotificationData) {
            this.mNotificationData.dump(pw, "  ");
        }
        this.mIconController.dump(pw);
        if (this.mStatusBarWindowManager != null) {
            this.mStatusBarWindowManager.dump(fd, pw, args);
        }
        if (this.mNetworkController != null) {
            this.mNetworkController.dump(fd, pw, args);
        }
        if (this.mBluetoothController != null) {
            this.mBluetoothController.dump(fd, pw, args);
        }
        if (this.mHotspotController != null) {
            this.mHotspotController.dump(fd, pw, args);
        }
        if (this.mCastController != null) {
            this.mCastController.dump(fd, pw, args);
        }
        if (this.mUserSwitcherController != null) {
            this.mUserSwitcherController.dump(fd, pw, args);
        }
        if (this.mBatteryController != null) {
            this.mBatteryController.dump(fd, pw, args);
        }
        if (this.mNextAlarmController != null) {
            this.mNextAlarmController.dump(fd, pw, args);
        }
        if (this.mSecurityController != null) {
            this.mSecurityController.dump(fd, pw, args);
        }
        if (this.mHeadsUpManager != null) {
            this.mHeadsUpManager.dump(fd, pw, args);
        } else {
            pw.println("  mHeadsUpManager: null");
        }
        if (this.mGroupManager != null) {
            this.mGroupManager.dump(fd, pw, args);
        } else {
            pw.println("  mGroupManager: null");
        }
        if (KeyguardUpdateMonitor.getInstance(this.mContext) != null) {
            KeyguardUpdateMonitor.getInstance(this.mContext).dump(fd, pw, args);
        }
        FalsingManager.getInstance(this.mContext).dump(pw);
        FalsingLog.dump(pw);
        pw.println("SharedPreferences:");
        for (Map.Entry<String, ?> entry : Prefs.getAll(this.mContext).entrySet()) {
            pw.print("  ");
            pw.print(entry.getKey());
            pw.print("=");
            pw.println(entry.getValue());
        }
    }

    private static void dumpBarTransitions(PrintWriter pw, String var, BarTransitions transitions) {
        pw.print("  ");
        pw.print(var);
        pw.print(".BarTransitions.mMode=");
        pw.println(BarTransitions.modeToString(transitions.getMode()));
    }

    @Override
    public void createAndAddWindows() {
        addStatusBarWindow();
    }

    private void addStatusBarWindow() {
        makeStatusBarView();
        this.mStatusBarWindowManager = new StatusBarWindowManager(this.mContext);
        this.mRemoteInputController = new RemoteInputController(this.mStatusBarWindowManager, this.mHeadsUpManager);
        this.mStatusBarWindowManager.add(this.mStatusBarWindow, getStatusBarHeight());
    }

    void updateDisplaySize() {
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        this.mDisplay.getSize(this.mCurrentDisplaySize);
    }

    float getDisplayDensity() {
        return this.mDisplayMetrics.density;
    }

    public void startActivityDismissingKeyguard(Intent intent, boolean onlyProvisioned, boolean dismissShade) {
        startActivityDismissingKeyguard(intent, onlyProvisioned, dismissShade, null);
    }

    public void startActivityDismissingKeyguard(final Intent intent, boolean onlyProvisioned, boolean dismissShade, final ActivityStarter.Callback callback) {
        if (onlyProvisioned && !isDeviceProvisioned()) {
            return;
        }
        final boolean afterKeyguardGone = PreviewInflater.wouldLaunchResolverActivity(this.mContext, intent, this.mCurrentUserId);
        final boolean keyguardShowing = this.mStatusBarKeyguardViewManager.isShowing();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                PhoneStatusBar.this.mAssistManager.hideAssist();
                intent.setFlags(335544320);
                int result = -6;
                try {
                    result = ActivityManagerNative.getDefault().startActivityAsUser((IApplicationThread) null, PhoneStatusBar.this.mContext.getBasePackageName(), intent, intent.resolveTypeIfNeeded(PhoneStatusBar.this.mContext.getContentResolver()), (IBinder) null, (String) null, 0, 268435456, (ProfilerInfo) null, PhoneStatusBar.this.getActivityOptions(), UserHandle.CURRENT.getIdentifier());
                } catch (RemoteException e) {
                    Log.w("PhoneStatusBar", "Unable to start activity", e);
                }
                PhoneStatusBar.this.overrideActivityPendingAppTransition(keyguardShowing && !afterKeyguardGone);
                if (callback == null) {
                    return;
                }
                callback.onActivityStarted(result);
            }
        };
        Runnable cancelRunnable = new Runnable() {
            @Override
            public void run() {
                if (callback == null) {
                    return;
                }
                callback.onActivityStarted(-6);
            }
        };
        executeRunnableDismissingKeyguard(runnable, cancelRunnable, dismissShade, afterKeyguardGone, true);
    }

    public void executeRunnableDismissingKeyguard(final Runnable runnable, Runnable cancelAction, final boolean dismissShade, final boolean afterKeyguardGone, final boolean deferred) {
        final boolean keyguardShowing = this.mStatusBarKeyguardViewManager.isShowing();
        dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() {
            @Override
            public boolean onDismiss() {
                final boolean z = keyguardShowing;
                final boolean z2 = afterKeyguardGone;
                final Runnable runnable2 = runnable;
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (z && !z2) {
                                ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                            }
                            if (runnable2 == null) {
                                return;
                            }
                            runnable2.run();
                        } catch (RemoteException e) {
                        }
                    }
                });
                if (dismissShade) {
                    PhoneStatusBar.this.animateCollapsePanels(2, true, true);
                }
                return deferred;
            }
        }, cancelAction, afterKeyguardGone);
    }

    public void resetUserExpandedStates() {
        ArrayList<NotificationData.Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
        int notificationCount = activeNotifications.size();
        for (int i = 0; i < notificationCount; i++) {
            NotificationData.Entry entry = activeNotifications.get(i);
            if (entry.row != null) {
                entry.row.resetUserExpansion();
            }
        }
    }

    @Override
    protected void dismissKeyguardThenExecute(KeyguardHostView.OnDismissAction action, boolean afterKeyguardGone) {
        dismissKeyguardThenExecute(action, null, afterKeyguardGone);
    }

    private void dismissKeyguardThenExecute(KeyguardHostView.OnDismissAction action, Runnable cancelAction, boolean afterKeyguardGone) {
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            this.mStatusBarKeyguardViewManager.dismissWithAction(action, cancelAction, afterKeyguardGone);
        } else {
            action.onDismiss();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        updateResources();
        updateDisplaySize();
        super.onConfigurationChanged(newConfig);
        Log.v("PhoneStatusBar", "configuration changed: " + this.mContext.getResources().getConfiguration());
        repositionNavigationBar();
        updateRowStates();
        this.mScreenPinningRequest.onConfigurationChanged();
        this.mNetworkController.onConfigurationChanged();
    }

    @Override
    public void userSwitched(int newUserId) {
        super.userSwitched(newUserId);
        animateCollapsePanels();
        updatePublicMode();
        updateNotifications();
        resetUserSetupObserver();
        setControllerUsers();
        clearCurrentMediaNotification();
        this.mLockscreenWallpaper.setCurrentUser(newUserId);
        updateMediaMetaData(true, false);
    }

    private void setControllerUsers() {
        if (this.mZenModeController != null) {
            this.mZenModeController.setUserId(this.mCurrentUserId);
        }
        if (this.mSecurityController == null) {
            return;
        }
        this.mSecurityController.onUserSwitched(this.mCurrentUserId);
    }

    private void resetUserSetupObserver() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mUserSetupObserver);
        this.mUserSetupObserver.onChange(false);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), true, this.mUserSetupObserver, this.mCurrentUserId);
    }

    void updateResources() {
        if (this.mQSPanel != null) {
            this.mQSPanel.updateResources();
        }
        loadDimens();
        if (this.mNotificationPanel != null) {
            this.mNotificationPanel.updateResources();
        }
        if (this.mBrightnessMirrorController == null) {
            return;
        }
        this.mBrightnessMirrorController.updateResources();
    }

    protected void loadDimens() {
        Resources res = this.mContext.getResources();
        int oldBarHeight = this.mNaturalBarHeight;
        this.mNaturalBarHeight = res.getDimensionPixelSize(android.R.dimen.accessibility_touch_slop);
        if (this.mStatusBarWindowManager != null && this.mNaturalBarHeight != oldBarHeight) {
            this.mStatusBarWindowManager.setBarHeight(this.mNaturalBarHeight);
        }
        this.mMaxAllowedKeyguardNotifications = res.getInteger(R.integer.keyguard_max_notification_count);
        Log.v("PhoneStatusBar", "defineSlots");
    }

    @Override
    protected void handleVisibleToUserChanged(boolean visibleToUser) {
        if (visibleToUser) {
            super.handleVisibleToUserChanged(visibleToUser);
            startNotificationLogging();
        } else {
            stopNotificationLogging();
            super.handleVisibleToUserChanged(visibleToUser);
        }
    }

    private void stopNotificationLogging() {
        if (!this.mCurrentlyVisibleNotifications.isEmpty()) {
            logNotificationVisibilityChanges(Collections.emptyList(), this.mCurrentlyVisibleNotifications);
            recycleAllVisibilityObjects(this.mCurrentlyVisibleNotifications);
        }
        this.mHandler.removeCallbacks(this.mVisibilityReporter);
        this.mStackScroller.setChildLocationsChangedListener(null);
    }

    private void startNotificationLogging() {
        this.mStackScroller.setChildLocationsChangedListener(this.mNotificationLocationsChangedListener);
        this.mNotificationLocationsChangedListener.onChildLocationsChanged(this.mStackScroller);
    }

    public void logNotificationVisibilityChanges(Collection<NotificationVisibility> newlyVisible, Collection<NotificationVisibility> noLongerVisible) {
        if (newlyVisible.isEmpty() && noLongerVisible.isEmpty()) {
            return;
        }
        NotificationVisibility[] newlyVisibleAr = (NotificationVisibility[]) newlyVisible.toArray(new NotificationVisibility[newlyVisible.size()]);
        NotificationVisibility[] noLongerVisibleAr = (NotificationVisibility[]) noLongerVisible.toArray(new NotificationVisibility[noLongerVisible.size()]);
        try {
            this.mBarService.onNotificationVisibilityChanged(newlyVisibleAr, noLongerVisibleAr);
        } catch (RemoteException e) {
        }
        int N = newlyVisible.size();
        if (N <= 0) {
            return;
        }
        String[] newlyVisibleKeyAr = new String[N];
        for (int i = 0; i < N; i++) {
            newlyVisibleKeyAr[i] = newlyVisibleAr[i].key;
        }
        setNotificationsShown(newlyVisibleKeyAr);
    }

    private void logStateToEventlog() {
        boolean isShowing = this.mStatusBarKeyguardViewManager.isShowing();
        boolean isOccluded = this.mStatusBarKeyguardViewManager.isOccluded();
        boolean isBouncerShowing = this.mStatusBarKeyguardViewManager.isBouncerShowing();
        boolean isSecure = this.mUnlockMethodCache.isMethodSecure();
        boolean canSkipBouncer = this.mUnlockMethodCache.canSkipBouncer();
        int stateFingerprint = getLoggingFingerprint(this.mState, isShowing, isOccluded, isBouncerShowing, isSecure, canSkipBouncer);
        if (stateFingerprint == this.mLastLoggedStateFingerprint) {
            return;
        }
        EventLogTags.writeSysuiStatusBarState(this.mState, isShowing ? 1 : 0, isOccluded ? 1 : 0, isBouncerShowing ? 1 : 0, isSecure ? 1 : 0, canSkipBouncer ? 1 : 0);
        this.mLastLoggedStateFingerprint = stateFingerprint;
    }

    private static int getLoggingFingerprint(int statusBarState, boolean keyguardShowing, boolean keyguardOccluded, boolean bouncerShowing, boolean secure, boolean currentlyInsecure) {
        return ((currentlyInsecure ? 1 : 0) << 12) | ((secure ? 1 : 0) << 11) | (statusBarState & 255) | ((keyguardShowing ? 1 : 0) << 8) | ((keyguardOccluded ? 1 : 0) << 9) | ((bouncerShowing ? 1 : 0) << 10);
    }

    void vibrate() {
        Vibrator vib = (Vibrator) this.mContext.getSystemService("vibrator");
        vib.vibrate(250L, VIBRATION_ATTRIBUTES);
    }

    public boolean shouldDisableNavbarGestures() {
        return (isDeviceProvisioned() && (this.mDisabled1 & 33554432) == 0) ? false : true;
    }

    public void postQSRunnableDismissingKeyguard(final Runnable runnable) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PhoneStatusBar.this.mLeaveOpenOnKeyguardHide = true;
                PhoneStatusBar.this.executeRunnableDismissingKeyguard(runnable, null, false, false, false);
            }
        });
    }

    public void postStartActivityDismissingKeyguard(final PendingIntent intent) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PhoneStatusBar.this.startPendingIntentDismissingKeyguard(intent);
            }
        });
    }

    public void postStartActivityDismissingKeyguard(final Intent intent, int delay) {
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PhoneStatusBar.this.handleStartActivityDismissingKeyguard(intent, true);
            }
        }, delay);
    }

    public void handleStartActivityDismissingKeyguard(Intent intent, boolean onlyProvisioned) {
        startActivityDismissingKeyguard(intent, onlyProvisioned, true);
    }

    private static class FastColorDrawable extends Drawable {
        private final int mColor;

        public FastColorDrawable(int color) {
            this.mColor = (-16777216) | color;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawColor(this.mColor, PorterDuff.Mode.SRC);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return -1;
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
        }

        @Override
        public void setBounds(Rect bounds) {
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (this.mStatusBarWindow != null) {
            this.mWindowManager.removeViewImmediate(this.mStatusBarWindow);
            this.mStatusBarWindow = null;
        }
        if (this.mNavigationBarView != null) {
            this.mWindowManager.removeViewImmediate(this.mNavigationBarView);
            this.mNavigationBarView = null;
        }
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quitSafely();
            this.mHandlerThread = null;
        }
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        this.mContext.unregisterReceiver(this.mDemoReceiver);
        this.mAssistManager.destroy();
        SignalClusterView signalCluster = (SignalClusterView) this.mStatusBarView.findViewById(R.id.signal_cluster);
        SignalClusterView signalClusterKeyguard = (SignalClusterView) this.mKeyguardStatusBar.findViewById(R.id.signal_cluster);
        SignalClusterView signalClusterQs = (SignalClusterView) this.mHeader.findViewById(R.id.signal_cluster);
        this.mNetworkController.removeSignalCallback(signalCluster);
        this.mNetworkController.removeSignalCallback(signalClusterKeyguard);
        this.mNetworkController.removeSignalCallback(signalClusterQs);
        if (this.mQSPanel == null || this.mQSPanel.getHost() == null) {
            return;
        }
        this.mQSPanel.getHost().destroy();
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        int barMode;
        if (!this.mDemoModeAllowed) {
            this.mDemoModeAllowed = Settings.Global.getInt(this.mContext.getContentResolver(), "sysui_demo_allowed", 0) != 0;
        }
        if (this.mDemoModeAllowed) {
            if (command.equals("enter")) {
                this.mDemoMode = true;
            } else if (command.equals("exit")) {
                this.mDemoMode = false;
                checkBarModes();
            } else if (!this.mDemoMode) {
                dispatchDemoCommand("enter", new Bundle());
            }
            boolean modeChange = !command.equals("enter") ? command.equals("exit") : true;
            if ((modeChange || command.equals("volume")) && this.mVolumeComponent != null) {
                this.mVolumeComponent.dispatchDemoCommand(command, args);
            }
            if (modeChange || command.equals("clock")) {
                dispatchDemoCommandToView(command, args, R.id.clock);
            }
            if (modeChange || command.equals("battery")) {
                this.mBatteryController.dispatchDemoCommand(command, args);
            }
            if (modeChange || command.equals("status")) {
                this.mIconController.dispatchDemoCommand(command, args);
            }
            if (this.mNetworkController != null && (modeChange || command.equals("network"))) {
                this.mNetworkController.dispatchDemoCommand(command, args);
            }
            if (modeChange || command.equals("notifications")) {
                View notifications = this.mStatusBarView != null ? this.mStatusBarView.findViewById(R.id.notification_icon_area) : null;
                if (notifications != null) {
                    String visible = args.getString("visible");
                    int vis = (this.mDemoMode && "false".equals(visible)) ? 4 : 0;
                    notifications.setVisibility(vis);
                }
            }
            if (!command.equals("bars")) {
                return;
            }
            String mode = args.getString("mode");
            if ("opaque".equals(mode)) {
                barMode = 0;
            } else if ("translucent".equals(mode)) {
                barMode = 2;
            } else if ("semi-transparent".equals(mode)) {
                barMode = 1;
            } else if ("transparent".equals(mode)) {
                barMode = 4;
            } else {
                barMode = "warning".equals(mode) ? 5 : -1;
            }
            if (barMode == -1) {
                return;
            }
            if (this.mStatusBarView != null) {
                this.mStatusBarView.getBarTransitions().transitionTo(barMode, true);
            }
            if (this.mNavigationBarView == null) {
                return;
            }
            this.mNavigationBarView.getBarTransitions().transitionTo(barMode, true);
        }
    }

    private void dispatchDemoCommandToView(String command, Bundle args, int id) {
        if (this.mStatusBarView == null) {
            return;
        }
        KeyEvent.Callback callbackFindViewById = this.mStatusBarView.findViewById(id);
        if (!(callbackFindViewById instanceof DemoMode)) {
            return;
        }
        ((DemoMode) callbackFindViewById).dispatchDemoCommand(command, args);
    }

    public int getBarState() {
        return this.mState;
    }

    @Override
    public boolean isPanelFullyCollapsed() {
        return this.mNotificationPanel.isFullyCollapsed();
    }

    public void showKeyguard() {
        if (this.mLaunchTransitionFadingAway) {
            this.mNotificationPanel.animate().cancel();
            onLaunchTransitionFadingEnded();
        }
        this.mHandler.removeMessages(1003);
        if (this.mUserSwitcherController != null && this.mUserSwitcherController.useFullscreenUserSwitcher()) {
            setBarState(3);
        } else {
            setBarState(1);
        }
        updateKeyguardState(false, false);
        if (!this.mDeviceInteractive) {
            this.mNotificationPanel.setTouchDisabled(true);
        }
        if (this.mState == 1) {
            instantExpandNotificationsPanel();
        } else if (this.mState == 3) {
            instantCollapseNotificationPanel();
        }
        this.mLeaveOpenOnKeyguardHide = false;
        if (this.mDraggedDownRow != null) {
            this.mDraggedDownRow.setUserLocked(false);
            this.mDraggedDownRow.notifyHeightChanged(false);
            this.mDraggedDownRow = null;
        }
        this.mPendingRemoteInputView = null;
        this.mAssistManager.onLockscreenShown();
    }

    public void onLaunchTransitionFadingEnded() {
        this.mNotificationPanel.setAlpha(1.0f);
        this.mNotificationPanel.onAffordanceLaunchEnded();
        releaseGestureWakeLock();
        runLaunchTransitionEndRunnable();
        this.mLaunchTransitionFadingAway = false;
        this.mScrimController.forceHideScrims(false);
        updateMediaMetaData(true, true);
    }

    @Override
    public boolean isCollapsing() {
        return this.mNotificationPanel.isCollapsing();
    }

    @Override
    public void addPostCollapseAction(Runnable r) {
        this.mPostCollapseRunnables.add(r);
    }

    public boolean isInLaunchTransition() {
        if (this.mNotificationPanel.isLaunchTransitionRunning()) {
            return true;
        }
        return this.mNotificationPanel.isLaunchTransitionFinished();
    }

    public void fadeKeyguardAfterLaunchTransition(final Runnable beforeFading, Runnable endRunnable) {
        this.mHandler.removeMessages(1003);
        this.mLaunchTransitionEndRunnable = endRunnable;
        Runnable hideRunnable = new Runnable() {
            @Override
            public void run() {
                PhoneStatusBar.this.mLaunchTransitionFadingAway = true;
                if (beforeFading != null) {
                    beforeFading.run();
                }
                PhoneStatusBar.this.mScrimController.forceHideScrims(true);
                PhoneStatusBar.this.updateMediaMetaData(false, true);
                PhoneStatusBar.this.mNotificationPanel.setAlpha(1.0f);
                PhoneStatusBar.this.mStackScroller.setParentFadingOut(true);
                PhoneStatusBar.this.mNotificationPanel.animate().alpha(0.0f).setStartDelay(100L).setDuration(300L).withLayer().withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        PhoneStatusBar.this.onLaunchTransitionFadingEnded();
                    }
                });
                PhoneStatusBar.this.mIconController.appTransitionStarting(SystemClock.uptimeMillis(), 120L);
            }
        };
        if (this.mNotificationPanel.isLaunchTransitionRunning()) {
            this.mNotificationPanel.setLaunchTransitionEndRunnable(hideRunnable);
        } else {
            hideRunnable.run();
        }
    }

    public void fadeKeyguardWhilePulsing() {
        this.mNotificationPanel.animate().alpha(0.0f).setStartDelay(0L).setDuration(96L).setInterpolator(ScrimController.KEYGUARD_FADE_OUT_INTERPOLATOR).start();
    }

    public void startLaunchTransitionTimeout() {
        this.mHandler.sendEmptyMessageDelayed(1003, 5000L);
    }

    public void onLaunchTransitionTimeout() {
        Log.w("PhoneStatusBar", "Launch transition: Timeout!");
        this.mNotificationPanel.onAffordanceLaunchEnded();
        releaseGestureWakeLock();
        this.mNotificationPanel.resetViews();
    }

    private void runLaunchTransitionEndRunnable() {
        if (this.mLaunchTransitionEndRunnable == null) {
            return;
        }
        Runnable r = this.mLaunchTransitionEndRunnable;
        this.mLaunchTransitionEndRunnable = null;
        r.run();
    }

    public boolean hideKeyguard() {
        boolean staying = this.mLeaveOpenOnKeyguardHide;
        setBarState(0);
        View viewToClick = null;
        if (this.mLeaveOpenOnKeyguardHide) {
            this.mLeaveOpenOnKeyguardHide = false;
            long delay = calculateGoingToFullShadeDelay();
            this.mNotificationPanel.animateToFullShade(delay);
            if (this.mDraggedDownRow != null) {
                this.mDraggedDownRow.setUserLocked(false);
                this.mDraggedDownRow = null;
            }
            viewToClick = this.mPendingRemoteInputView;
            this.mPendingRemoteInputView = null;
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setLayoutTransitionsEnabled(false);
                this.mNavigationBarView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PhoneStatusBar.this.mNavigationBarView.setLayoutTransitionsEnabled(true);
                    }
                }, 448 + delay);
            }
        } else {
            instantCollapseNotificationPanel();
        }
        updateKeyguardState(staying, false);
        if (viewToClick != null) {
            viewToClick.callOnClick();
        }
        if (this.mQSPanel != null) {
            this.mQSPanel.refreshAllTiles();
        }
        this.mHandler.removeMessages(1003);
        releaseGestureWakeLock();
        this.mNotificationPanel.onAffordanceLaunchEnded();
        this.mNotificationPanel.animate().cancel();
        this.mNotificationPanel.setAlpha(1.0f);
        return staying;
    }

    private void releaseGestureWakeLock() {
        if (!this.mGestureWakeLock.isHeld()) {
            return;
        }
        this.mGestureWakeLock.release();
    }

    public long calculateGoingToFullShadeDelay() {
        return this.mKeyguardFadingAwayDelay + this.mKeyguardFadingAwayDuration;
    }

    public void keyguardGoingAway() {
        this.mKeyguardGoingAway = true;
        this.mIconController.appTransitionPending();
    }

    public void setKeyguardFadingAway(long startTime, long delay, long fadeoutDuration) {
        this.mKeyguardFadingAway = true;
        this.mKeyguardFadingAwayDelay = delay;
        this.mKeyguardFadingAwayDuration = fadeoutDuration;
        this.mWaitingForKeyguardExit = false;
        this.mIconController.appTransitionStarting((startTime + fadeoutDuration) - 120, 120L);
        disable(this.mDisabledUnmodified1, this.mDisabledUnmodified2, fadeoutDuration > 0);
    }

    public boolean isKeyguardFadingAway() {
        return this.mKeyguardFadingAway;
    }

    public void finishKeyguardFadingAway() {
        this.mKeyguardFadingAway = false;
        this.mKeyguardGoingAway = false;
    }

    public void stopWaitingForKeyguardExit() {
        this.mWaitingForKeyguardExit = false;
    }

    private void updatePublicMode() {
        boolean isPublic = false;
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            int i = this.mCurrentProfiles.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                UserInfo userInfo = this.mCurrentProfiles.valueAt(i);
                if (!this.mStatusBarKeyguardViewManager.isSecure(userInfo.id)) {
                    i--;
                } else {
                    isPublic = true;
                    break;
                }
            }
        }
        setLockscreenPublicMode(isPublic);
    }

    protected void updateKeyguardState(boolean goingToFullShade, boolean fromShadeLocked) {
        if (this.mState == 1) {
            this.mKeyguardIndicationController.setVisible(true);
            this.mNotificationPanel.resetViews();
            if (this.mKeyguardUserSwitcher != null) {
                this.mKeyguardUserSwitcher.setKeyguard(true, fromShadeLocked);
            }
            this.mStatusBarView.removePendingHideExpandedRunnables();
        } else {
            this.mKeyguardIndicationController.setVisible(false);
            if (this.mKeyguardUserSwitcher != null) {
                this.mKeyguardUserSwitcher.setKeyguard(false, (goingToFullShade || this.mState == 2) ? true : fromShadeLocked);
            }
        }
        if (this.mState == 1 || this.mState == 2) {
            this.mScrimController.setKeyguardShowing(true);
        } else {
            this.mScrimController.setKeyguardShowing(false);
        }
        this.mIconPolicy.notifyKeyguardShowingChanged();
        this.mNotificationPanel.setBarState(this.mState, this.mKeyguardFadingAway, goingToFullShade);
        updateDozingState();
        updatePublicMode();
        updateStackScrollerState(goingToFullShade, fromShadeLocked);
        updateNotifications();
        checkBarModes();
        updateCarrierLabelVisibility(false);
        updateMediaMetaData(false, this.mState != 1);
        this.mKeyguardMonitor.notifyKeyguardState(this.mStatusBarKeyguardViewManager.isShowing(), this.mStatusBarKeyguardViewManager.isSecure());
    }

    private void updateDozingState() {
        boolean z = false;
        boolean zIsPulsing = !this.mDozing ? this.mDozeScrimController.isPulsing() : false;
        this.mNotificationPanel.setDozing(this.mDozing, zIsPulsing);
        this.mStackScroller.setDark(this.mDozing, zIsPulsing, this.mWakeUpTouchLocation);
        this.mScrimController.setDozing(this.mDozing);
        DozeScrimController dozeScrimController = this.mDozeScrimController;
        if (this.mDozing && this.mFingerprintUnlockController.getMode() != 2) {
            z = true;
        }
        dozeScrimController.setDozing(z, zIsPulsing);
    }

    public void updateStackScrollerState(boolean goingToFullShade, boolean fromShadeLocked) {
        if (this.mStackScroller == null) {
            return;
        }
        boolean onKeyguard = this.mState == 1;
        this.mStackScroller.setHideSensitive(isLockscreenPublicMode(), goingToFullShade);
        this.mStackScroller.setDimmed(onKeyguard, fromShadeLocked);
        this.mStackScroller.setExpandingEnabled(onKeyguard ? false : true);
        ActivatableNotificationView activatedChild = this.mStackScroller.getActivatedChild();
        this.mStackScroller.setActivatedChild(null);
        if (activatedChild == null) {
            return;
        }
        activatedChild.makeInactive(false);
    }

    public void userActivity() {
        if (this.mState != 1) {
            return;
        }
        this.mKeyguardViewMediatorCallback.userActivity();
    }

    public boolean interceptMediaKey(KeyEvent event) {
        if (this.mState == 1) {
            return this.mStatusBarKeyguardViewManager.interceptMediaKey(event);
        }
        return false;
    }

    public boolean onMenuPressed() {
        if (!this.mDeviceInteractive || this.mState == 0 || !this.mStatusBarKeyguardViewManager.shouldDismissOnMenuPressed()) {
            return false;
        }
        animateCollapsePanels(2, true);
        return true;
    }

    public void endAffordanceLaunch() {
        releaseGestureWakeLock();
        this.mNotificationPanel.onAffordanceLaunchEnded();
    }

    public boolean onBackPressed() {
        if (this.mStatusBarKeyguardViewManager.onBackPressed()) {
            return true;
        }
        if (this.mNotificationPanel.isQsExpanded()) {
            if (this.mNotificationPanel.isQsDetailShowing()) {
                this.mNotificationPanel.closeQsDetail();
            } else {
                this.mNotificationPanel.animateCloseQs();
            }
            return true;
        }
        if (this.mState != 1 && this.mState != 2) {
            animateCollapsePanels();
            return true;
        }
        return false;
    }

    public boolean onSpacePressed() {
        if (!this.mDeviceInteractive || this.mState == 0) {
            return false;
        }
        animateCollapsePanels(2, true);
        return true;
    }

    private void showBouncer() {
        if (this.mState != 1 && this.mState != 2) {
            return;
        }
        this.mWaitingForKeyguardExit = this.mStatusBarKeyguardViewManager.isShowing();
        this.mStatusBarKeyguardViewManager.dismiss();
    }

    private void instantExpandNotificationsPanel() {
        makeExpandedVisible(true);
        this.mNotificationPanel.expand(false);
    }

    private void instantCollapseNotificationPanel() {
        this.mNotificationPanel.instantCollapse();
    }

    @Override
    public void onActivated(ActivatableNotificationView view) {
        EventLogTags.writeSysuiLockscreenGesture(7, 0, 0);
        this.mKeyguardIndicationController.showTransientIndication(R.string.notification_tap_again);
        ActivatableNotificationView previousView = this.mStackScroller.getActivatedChild();
        if (previousView != null) {
            previousView.makeInactive(true);
        }
        this.mStackScroller.setActivatedChild(view);
    }

    public void setBarState(int state) {
        if (state != this.mState && this.mVisible && (state == 2 || (state == 0 && isGoingToNotificationShade()))) {
            clearNotificationEffects();
        }
        if (state == 1) {
            removeRemoteInputEntriesKeptUntilCollapsed();
        }
        this.mState = state;
        this.mGroupManager.setStatusBarState(state);
        this.mFalsingManager.setStatusBarState(state);
        this.mStatusBarWindowManager.setStatusBarState(state);
        updateDozing();
    }

    @Override
    public void onActivationReset(ActivatableNotificationView view) {
        if (view != this.mStackScroller.getActivatedChild()) {
            return;
        }
        this.mKeyguardIndicationController.hideTransientIndication();
        this.mStackScroller.setActivatedChild(null);
    }

    public void onTrackingStarted() {
        runPostCollapseRunnables();
    }

    public void onClosingFinished() {
        runPostCollapseRunnables();
    }

    public void onUnlockHintStarted() {
        this.mFalsingManager.onUnlockHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(R.string.keyguard_unlock);
    }

    public void onHintFinished() {
        this.mKeyguardIndicationController.hideTransientIndicationDelayed(1200L);
    }

    public void onCameraHintStarted() {
        this.mFalsingManager.onCameraHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(R.string.camera_hint);
    }

    public void onVoiceAssistHintStarted() {
        this.mFalsingManager.onLeftAffordanceHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(R.string.voice_hint);
    }

    public void onPhoneHintStarted() {
        this.mFalsingManager.onLeftAffordanceHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(R.string.phone_hint);
    }

    public void onTrackingStopped(boolean expand) {
        if ((this.mState != 1 && this.mState != 2) || expand || this.mUnlockMethodCache.canSkipBouncer()) {
            return;
        }
        showBouncer();
    }

    @Override
    protected int getMaxKeyguardNotifications(boolean recompute) {
        if (recompute) {
            this.mMaxKeyguardNotifications = Math.max(1, this.mNotificationPanel.computeMaxKeyguardNotifications(this.mMaxAllowedKeyguardNotifications));
            return this.mMaxKeyguardNotifications;
        }
        return this.mMaxKeyguardNotifications;
    }

    public int getMaxKeyguardNotifications() {
        return getMaxKeyguardNotifications(false);
    }

    public NavigationBarView getNavigationBarView() {
        return this.mNavigationBarView;
    }

    @Override
    public boolean onDraggedDown(View startingChild, int dragLengthY) {
        if (!hasActiveNotifications()) {
            return false;
        }
        EventLogTags.writeSysuiLockscreenGesture(2, (int) (dragLengthY / this.mDisplayMetrics.density), 0);
        goToLockedShade(startingChild);
        if (startingChild instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) startingChild;
            row.onExpandedByGesture(true);
        }
        return true;
    }

    @Override
    public void onDragDownReset() {
        this.mStackScroller.setDimmed(true, true);
        this.mStackScroller.resetScrollPosition();
    }

    @Override
    public void onCrossedThreshold(boolean above) {
        this.mStackScroller.setDimmed(!above, true);
    }

    @Override
    public void onTouchSlopExceeded() {
        this.mStackScroller.removeLongPressCallback();
    }

    @Override
    public void setEmptyDragAmount(float amount) {
        this.mNotificationPanel.setEmptyDragAmount(amount);
    }

    public void goToLockedShade(View expandView) {
        ExpandableNotificationRow row = null;
        if (expandView instanceof ExpandableNotificationRow) {
            row = (ExpandableNotificationRow) expandView;
            row.setUserExpanded(true, true);
            row.setGroupExpansionChanging(true);
        }
        boolean zShouldEnforceBouncer = (userAllowsPrivateNotificationsInPublic(this.mCurrentUserId) && this.mShowLockscreenNotifications) ? this.mFalsingManager.shouldEnforceBouncer() : true;
        if (isLockscreenPublicMode() && zShouldEnforceBouncer) {
            this.mLeaveOpenOnKeyguardHide = true;
            showBouncer();
            this.mDraggedDownRow = row;
            this.mPendingRemoteInputView = null;
            return;
        }
        this.mNotificationPanel.animateToFullShade(0L);
        setBarState(2);
        updateKeyguardState(false, false);
    }

    @Override
    public void onLockedNotificationImportanceChange(KeyguardHostView.OnDismissAction dismissAction) {
        this.mLeaveOpenOnKeyguardHide = true;
        dismissKeyguardThenExecute(dismissAction, true);
    }

    @Override
    protected void onLockedRemoteInput(ExpandableNotificationRow row, View clicked) {
        this.mLeaveOpenOnKeyguardHide = true;
        showBouncer();
        this.mPendingRemoteInputView = clicked;
    }

    @Override
    protected boolean startWorkChallengeIfNecessary(int userId, IntentSender intendSender, String notificationKey) {
        this.mPendingWorkRemoteInputView = null;
        return super.startWorkChallengeIfNecessary(userId, intendSender, notificationKey);
    }

    @Override
    protected void onLockedWorkRemoteInput(int userId, ExpandableNotificationRow row, View clicked) {
        animateCollapsePanels();
        startWorkChallengeIfNecessary(userId, null, null);
        this.mPendingWorkRemoteInputView = clicked;
    }

    @Override
    protected void onWorkChallengeUnlocked() {
        if (this.mPendingWorkRemoteInputView == null) {
            return;
        }
        View view = this.mPendingWorkRemoteInputView;
        final Runnable clickPendingViewRunnable = new AnonymousClass46();
        this.mNotificationPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (PhoneStatusBar.this.mNotificationPanel.mStatusBar.getStatusBarWindow().getHeight() == PhoneStatusBar.this.mNotificationPanel.mStatusBar.getStatusBarHeight()) {
                    return;
                }
                PhoneStatusBar.this.mNotificationPanel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                PhoneStatusBar.this.mNotificationPanel.post(clickPendingViewRunnable);
            }
        });
        instantExpandNotificationsPanel();
    }

    class AnonymousClass46 implements Runnable {
        AnonymousClass46() {
        }

        @Override
        public void run() {
            if (PhoneStatusBar.this.mPendingWorkRemoteInputView == null) {
                return;
            }
            View pendingWorkRemoteInputView = PhoneStatusBar.this.mPendingWorkRemoteInputView;
            for (ViewParent p = pendingWorkRemoteInputView.getParent(); p != null; p = p.getParent()) {
                if (p instanceof ExpandableNotificationRow) {
                    final ExpandableNotificationRow row = (ExpandableNotificationRow) p;
                    ViewParent viewParent = row.getParent();
                    if (!(viewParent instanceof NotificationStackScrollLayout)) {
                        return;
                    }
                    final NotificationStackScrollLayout scrollLayout = (NotificationStackScrollLayout) viewParent;
                    row.makeActionsVisibile();
                    row.post(new Runnable() {
                        @Override
                        public void run() {
                            final NotificationStackScrollLayout notificationStackScrollLayout = scrollLayout;
                            Runnable finishScrollingCallback = new Runnable() {
                                @Override
                                public void run() {
                                    PhoneStatusBar.this.mPendingWorkRemoteInputView.callOnClick();
                                    PhoneStatusBar.this.mPendingWorkRemoteInputView = null;
                                    notificationStackScrollLayout.setFinishScrollingCallback(null);
                                }
                            };
                            if (scrollLayout.scrollTo(row)) {
                                scrollLayout.setFinishScrollingCallback(finishScrollingCallback);
                            } else {
                                finishScrollingCallback.run();
                            }
                        }
                    });
                    return;
                }
            }
        }
    }

    @Override
    public void onExpandClicked(NotificationData.Entry clickedEntry, boolean nowExpanded) {
        this.mHeadsUpManager.setExpanded(clickedEntry, nowExpanded);
        if (this.mState != 1 || !nowExpanded) {
            return;
        }
        goToLockedShade(clickedEntry.row);
    }

    public void goToKeyguard() {
        if (this.mState != 2) {
            return;
        }
        this.mStackScroller.onGoToKeyguard();
        setBarState(1);
        updateKeyguardState(false, true);
    }

    public long getKeyguardFadingAwayDelay() {
        return this.mKeyguardFadingAwayDelay;
    }

    public long getKeyguardFadingAwayDuration() {
        return this.mKeyguardFadingAwayDuration;
    }

    @Override
    public void setBouncerShowing(boolean bouncerShowing) {
        super.setBouncerShowing(bouncerShowing);
        this.mStatusBarView.setBouncerShowing(bouncerShowing);
        disable(this.mDisabledUnmodified1, this.mDisabledUnmodified2, true);
    }

    public void onStartedGoingToSleep() {
        this.mStartedGoingToSleep = true;
    }

    public void onFinishedGoingToSleep() {
        this.mNotificationPanel.onAffordanceLaunchEnded();
        releaseGestureWakeLock();
        this.mLaunchCameraOnScreenTurningOn = false;
        this.mStartedGoingToSleep = false;
        this.mDeviceInteractive = false;
        this.mWakeUpComingFromTouch = false;
        this.mWakeUpTouchLocation = null;
        this.mStackScroller.setAnimationsEnabled(false);
        updateVisibleToUser();
        if (!this.mLaunchCameraOnFinishedGoingToSleep) {
            return;
        }
        this.mLaunchCameraOnFinishedGoingToSleep = false;
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PhoneStatusBar.this.onCameraLaunchGestureDetected(PhoneStatusBar.this.mLastCameraLaunchSource);
            }
        });
    }

    public void onStartedWakingUp() {
        this.mDeviceInteractive = true;
        this.mStackScroller.setAnimationsEnabled(true);
        this.mNotificationPanel.setTouchDisabled(false);
        updateVisibleToUser();
    }

    public void onScreenTurningOn() {
        this.mScreenTurningOn = true;
        this.mFalsingManager.onScreenTurningOn();
        this.mNotificationPanel.onScreenTurningOn();
        if (!this.mLaunchCameraOnScreenTurningOn) {
            return;
        }
        this.mNotificationPanel.launchCamera(false, this.mLastCameraLaunchSource);
        this.mLaunchCameraOnScreenTurningOn = false;
    }

    private void vibrateForCameraGesture() {
        this.mVibrator.vibrate(new long[]{0, 750}, -1);
    }

    public void onScreenTurnedOn() {
        this.mScreenTurningOn = false;
        this.mDozeScrimController.onScreenTurnedOn();
    }

    public boolean handleLongPressBack() {
        try {
            IActivityManager activityManager = ActivityManagerNative.getDefault();
            if (activityManager.isInLockTaskMode()) {
                activityManager.stopSystemLockTaskMode();
                this.mNavigationBarView.setDisabledFlags(this.mDisabled1, true);
                return true;
            }
            return false;
        } catch (RemoteException e) {
            Log.d("PhoneStatusBar", "Unable to reach activity manager", e);
            return false;
        }
    }

    public void updateRecentsVisibility(boolean visible) {
        if (visible) {
            this.mSystemUiVisibility |= 16384;
        } else {
            this.mSystemUiVisibility &= -16385;
        }
        notifyUiVisibilityChanged(this.mSystemUiVisibility);
    }

    @Override
    public void showScreenPinningRequest(int taskId) {
        if (this.mKeyguardMonitor.isShowing()) {
            return;
        }
        showScreenPinningRequest(taskId, true);
    }

    public void showScreenPinningRequest(int taskId, boolean allowCancel) {
        this.mScreenPinningRequest.showPrompt(taskId, allowCancel);
    }

    public boolean hasActiveNotifications() {
        return !this.mNotificationData.getActiveNotifications().isEmpty();
    }

    public void wakeUpIfDozing(long time, MotionEvent event) {
        if (!this.mDozing || !this.mDozeScrimController.isPulsing()) {
            return;
        }
        this.mWakeUpComingFromTouch = true;
        this.mWakeUpTouchLocation = new PointF(event.getX(), event.getY());
        this.mNotificationPanel.setTouchDisabled(false);
        this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
        this.mFalsingManager.onScreenOnFromTouch();
    }

    @Override
    public void appTransitionPending() {
        if (this.mKeyguardFadingAway) {
            return;
        }
        this.mIconController.appTransitionPending();
    }

    @Override
    public void appTransitionCancelled() {
        this.mIconController.appTransitionCancelled();
        EventBus.getDefault().send(new AppTransitionFinishedEvent());
    }

    @Override
    public void appTransitionStarting(long startTime, long duration) {
        if (!this.mKeyguardGoingAway) {
            this.mIconController.appTransitionStarting(startTime, duration);
        }
        if (this.mIconPolicy == null) {
            return;
        }
        this.mIconPolicy.appTransitionStarting(startTime, duration);
    }

    @Override
    public void appTransitionFinished() {
        EventBus.getDefault().send(new AppTransitionFinishedEvent());
    }

    @Override
    public void onCameraLaunchGestureDetected(int source) {
        this.mLastCameraLaunchSource = source;
        if (this.mStartedGoingToSleep) {
            this.mLaunchCameraOnFinishedGoingToSleep = true;
            return;
        }
        if (!this.mNotificationPanel.canCameraGestureBeLaunched(this.mStatusBarKeyguardViewManager.isShowing() ? this.mExpandedVisible : false)) {
            return;
        }
        if (!this.mDeviceInteractive) {
            PowerManager pm = (PowerManager) this.mContext.getSystemService(PowerManager.class);
            pm.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:CAMERA_GESTURE");
            this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
        }
        vibrateForCameraGesture();
        if (!this.mStatusBarKeyguardViewManager.isShowing()) {
            startActivity(KeyguardBottomAreaView.INSECURE_CAMERA_INTENT, true);
            return;
        }
        if (!this.mDeviceInteractive) {
            this.mScrimController.dontAnimateBouncerChangesUntilNextFrame();
            this.mGestureWakeLock.acquire(6000L);
        }
        if (this.mScreenTurningOn || this.mStatusBarKeyguardViewManager.isScreenTurnedOn()) {
            this.mNotificationPanel.launchCamera(this.mDeviceInteractive, source);
        } else {
            this.mLaunchCameraOnScreenTurningOn = true;
        }
    }

    @Override
    public void showTvPictureInPictureMenu() {
    }

    public void notifyFpAuthModeChanged() {
        updateDozing();
    }

    public void updateDozing() {
        boolean z = true;
        if ((!this.mDozingRequested || this.mState != 1) && this.mFingerprintUnlockController.getMode() != 2) {
            z = false;
        }
        this.mDozing = z;
        updateDozingState();
    }

    private final class ShadeUpdates {
        private final ArraySet<String> mNewVisibleNotifications;
        private final ArraySet<String> mVisibleNotifications;

        ShadeUpdates(PhoneStatusBar this$0, ShadeUpdates shadeUpdates) {
            this();
        }

        private ShadeUpdates() {
            this.mVisibleNotifications = new ArraySet<>();
            this.mNewVisibleNotifications = new ArraySet<>();
        }

        public void check() {
            this.mNewVisibleNotifications.clear();
            ArrayList<NotificationData.Entry> activeNotifications = PhoneStatusBar.this.mNotificationData.getActiveNotifications();
            for (int i = 0; i < activeNotifications.size(); i++) {
                NotificationData.Entry entry = activeNotifications.get(i);
                boolean visible = entry.row != null && entry.row.getVisibility() == 0;
                if (visible) {
                    this.mNewVisibleNotifications.add(entry.key + entry.notification.getPostTime());
                }
            }
            boolean updates = !this.mVisibleNotifications.containsAll(this.mNewVisibleNotifications);
            this.mVisibleNotifications.clear();
            this.mVisibleNotifications.addAll((ArraySet<? extends String>) this.mNewVisibleNotifications);
            if (!updates || PhoneStatusBar.this.mDozeServiceHost == null) {
                return;
            }
            PhoneStatusBar.this.mDozeServiceHost.fireNewNotifications();
        }
    }

    private final class DozeServiceHost extends KeyguardUpdateMonitorCallback implements DozeHost {
        private final ArrayList<DozeHost.Callback> mCallbacks;
        private final H mHandler;
        private boolean mNotificationLightOn;

        DozeServiceHost(PhoneStatusBar this$0, DozeServiceHost dozeServiceHost) {
            this();
        }

        private DozeServiceHost() {
            this.mCallbacks = new ArrayList<>();
            this.mHandler = new H(this, null);
        }

        public String toString() {
            return "PSB.DozeServiceHost[mCallbacks=" + this.mCallbacks.size() + "]";
        }

        public void firePowerSaveChanged(boolean active) {
            for (DozeHost.Callback callback : this.mCallbacks) {
                callback.onPowerSaveChanged(active);
            }
        }

        public void fireBuzzBeepBlinked() {
            for (DozeHost.Callback callback : this.mCallbacks) {
                callback.onBuzzBeepBlinked();
            }
        }

        public void fireNotificationLight(boolean on) {
            this.mNotificationLightOn = on;
            for (DozeHost.Callback callback : this.mCallbacks) {
                callback.onNotificationLight(on);
            }
        }

        public void fireNewNotifications() {
            for (DozeHost.Callback callback : this.mCallbacks) {
                callback.onNewNotifications();
            }
        }

        @Override
        public void addCallback(DozeHost.Callback callback) {
            this.mCallbacks.add(callback);
        }

        @Override
        public void removeCallback(DozeHost.Callback callback) {
            this.mCallbacks.remove(callback);
        }

        @Override
        public void startDozing(Runnable ready) {
            this.mHandler.obtainMessage(1, ready).sendToTarget();
        }

        @Override
        public void pulseWhileDozing(DozeHost.PulseCallback callback, int reason) {
            this.mHandler.obtainMessage(2, reason, 0, callback).sendToTarget();
        }

        @Override
        public void stopDozing() {
            this.mHandler.obtainMessage(3).sendToTarget();
        }

        @Override
        public boolean isPowerSaveActive() {
            if (PhoneStatusBar.this.mBatteryController != null) {
                return PhoneStatusBar.this.mBatteryController.isPowerSave();
            }
            return false;
        }

        @Override
        public boolean isPulsingBlocked() {
            return PhoneStatusBar.this.mFingerprintUnlockController.getMode() == 1;
        }

        @Override
        public boolean isNotificationLightOn() {
            return this.mNotificationLightOn;
        }

        public void handleStartDozing(Runnable ready) {
            if (!PhoneStatusBar.this.mDozingRequested) {
                PhoneStatusBar.this.mDozingRequested = true;
                DozeLog.traceDozing(PhoneStatusBar.this.mContext, PhoneStatusBar.this.mDozing);
                PhoneStatusBar.this.updateDozing();
            }
            ready.run();
        }

        public void handlePulseWhileDozing(final DozeHost.PulseCallback callback, int reason) {
            PhoneStatusBar.this.mDozeScrimController.pulse(new DozeHost.PulseCallback() {
                @Override
                public void onPulseStarted() {
                    callback.onPulseStarted();
                    PhoneStatusBar.this.mStackScroller.setPulsing(true);
                }

                @Override
                public void onPulseFinished() {
                    callback.onPulseFinished();
                    PhoneStatusBar.this.mStackScroller.setPulsing(false);
                }
            }, reason);
        }

        public void handleStopDozing() {
            if (!PhoneStatusBar.this.mDozingRequested) {
                return;
            }
            PhoneStatusBar.this.mDozingRequested = false;
            DozeLog.traceDozing(PhoneStatusBar.this.mContext, PhoneStatusBar.this.mDozing);
            PhoneStatusBar.this.updateDozing();
        }

        private final class H extends Handler {
            H(DozeServiceHost this$1, H h) {
                this();
            }

            private H() {
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        DozeServiceHost.this.handleStartDozing((Runnable) msg.obj);
                        break;
                    case 2:
                        DozeServiceHost.this.handlePulseWhileDozing((DozeHost.PulseCallback) msg.obj, msg.arg1);
                        break;
                    case 3:
                        DozeServiceHost.this.handleStopDozing();
                        break;
                }
            }
        }
    }

    private final boolean supportCustomizeCarrierLabel() {
        if (this.mStatusBarPlmnPlugin == null || !this.mStatusBarPlmnPlugin.supportCustomizeCarrierLabel() || this.mNetworkController == null) {
            return false;
        }
        return this.mNetworkController.hasMobileDataFeature();
    }

    private final void updateCustomizeCarrierLabelVisibility(boolean force) {
        Log.d("PhoneStatusBar", "updateCustomizeCarrierLabelVisibility(), force = " + force + ", mState = " + this.mState);
        boolean makeVisible = this.mStackScroller.getVisibility() == 0 && this.mState != 1;
        this.mStatusBarPlmnPlugin.updateCarrierLabelVisibility(force, makeVisible);
    }

    protected void updateCarrierLabelVisibility(boolean force) {
        if (!supportCustomizeCarrierLabel()) {
            return;
        }
        if (this.mState == 1 || this.mNotificationPanel.isPanelVisibleBecauseOfHeadsUp()) {
            if (this.mCustomizeCarrierLabel == null) {
                return;
            }
            this.mCustomizeCarrierLabel.setVisibility(8);
            return;
        }
        updateCustomizeCarrierLabelVisibility(force);
    }
}
