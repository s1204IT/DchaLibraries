package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.view.Display;
import android.view.HardwareCanvas;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.DemoMode;
import com.android.systemui.EventLogTags;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTile;
import com.android.systemui.recent.ScreenPinningRequest;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.BackDropView;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.DismissView;
import com.android.systemui.statusbar.DragDownHelper;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationOverflowContainer;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.SpeedBumpView;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BluetoothControllerImpl;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.statusbar.policy.CastControllerImpl;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HeadsUpNotificationView;
import com.android.systemui.statusbar.policy.HotspotControllerImpl;
import com.android.systemui.statusbar.policy.KeyButtonView;
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
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PhoneStatusBar extends BaseStatusBar implements DemoMode, DragDownHelper.DragDownCallback, ActivityStarter, UnlockMethodCache.OnUnlockMethodChangedListener {
    AccessibilityController mAccessibilityController;
    private boolean mAutohideSuspended;
    private BackDropView mBackdrop;
    private ImageView mBackdropBack;
    private ImageView mBackdropFront;
    BatteryController mBatteryController;
    BluetoothControllerImpl mBluetoothController;
    BrightnessMirrorController mBrightnessMirrorController;
    private TextView mCarrierLabel;
    private int mCarrierLabelHeight;
    CastControllerImpl mCastController;
    Animator mClearButtonAnim;
    private boolean mDemoMode;
    private boolean mDemoModeAllowed;
    private DemoStatusIcons mDemoStatusIcons;
    private int mDisabledUnmodified;
    Display mDisplay;
    private DozeScrimController mDozeScrimController;
    private DozeServiceHost mDozeServiceHost;
    private boolean mDozing;
    private ExpandableNotificationRow mDraggedDownRow;
    private int mDrawCount;
    int mEdgeBorder;
    View mExpandedContents;
    boolean mExpandedVisible;
    FlashlightController mFlashlightController;
    private HandlerThread mHandlerThread;
    StatusBarHeaderView mHeader;
    HotspotControllerImpl mHotspotController;
    PhoneStatusBarPolicy mIconPolicy;
    private int mInteractingWindows;
    KeyguardBottomAreaView mKeyguardBottomArea;
    private boolean mKeyguardFadingAway;
    private long mKeyguardFadingAwayDelay;
    private long mKeyguardFadingAwayDuration;
    KeyguardIndicationController mKeyguardIndicationController;
    int mKeyguardMaxNotificationCount;
    KeyguardMonitor mKeyguardMonitor;
    KeyguardStatusBarView mKeyguardStatusBar;
    View mKeyguardStatusView;
    KeyguardUserSwitcher mKeyguardUserSwitcher;
    private ViewMediatorCallback mKeyguardViewMediatorCallback;
    private long mLastLockToAppLongPress;
    private int mLastLoggedStateFingerprint;
    private long mLastVisibilityReportUptimeMs;
    private Runnable mLaunchTransitionEndRunnable;
    private boolean mLaunchTransitionFadingAway;
    boolean mLeaveOpenOnKeyguardHide;
    private Interpolator mLinearOutSlowIn;
    LocationControllerImpl mLocationController;
    private MediaController mMediaController;
    private MediaMetadata mMediaMetadata;
    private String mMediaNotificationKey;
    private MediaSessionManager mMediaSessionManager;
    View mMoreIcon;
    private int mNavigationBarMode;
    NetworkControllerImpl mNetworkController;
    NextAlarmController mNextAlarmController;
    View mNotificationIconArea;
    IconMerger mNotificationIcons;
    NotificationPanelView mNotificationPanel;
    int mNotificationPanelGravity;
    float mNotificationPanelMinHeightFrac;
    int mPixelFormat;
    private QSPanel mQSPanel;
    RotationLockControllerImpl mRotationLockController;
    private boolean mScreenOnComingFromTouch;
    private PointF mScreenOnTouchLocation;
    private ScreenPinningRequest mScreenPinningRequest;
    private ScrimController mScrimController;
    private boolean mScrimSrcModeEnabled;
    Animator mScrollViewAnim;
    SecurityControllerImpl mSecurityController;
    LinearLayout mStatusBarContents;
    private int mStatusBarHeaderHeight;
    private int mStatusBarMode;
    PhoneStatusBarView mStatusBarView;
    StatusBarWindowView mStatusBarWindow;
    private StatusBarWindowManager mStatusBarWindowManager;
    LinearLayout mStatusIcons;
    LinearLayout mStatusIconsKeyguard;
    LinearLayout mSystemIconArea;
    LinearLayout mSystemIcons;
    private Ticker mTicker;
    private boolean mTickerEnabled;
    private View mTickerView;
    private boolean mTicking;
    boolean mTracking;
    int mTrackingPosition;
    private UnlockMethodCache mUnlockMethodCache;
    UserInfoController mUserInfoController;
    UserSwitcherController mUserSwitcherController;
    VolumeComponent mVolumeComponent;
    private boolean mWaitingForKeyguardExit;
    ZenModeController mZenModeController;
    public static final boolean DEBUG = BaseStatusBar.DEBUG;
    public static final boolean CHATTY = DEBUG;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    public static final Interpolator ALPHA_IN = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
    public static final Interpolator ALPHA_OUT = new PathInterpolator(0.0f, 0.0f, 0.8f, 1.0f);
    int mNaturalBarHeight = -1;
    int mIconSize = -1;
    int mIconHPadding = -1;
    Point mCurrentDisplaySize = new Point();
    private int mStatusBarWindowState = 0;
    Object mQueueLock = new Object();
    private boolean mCarrierLabelVisible = false;
    private boolean mShowCarrierInPanel = false;
    int[] mPositionTmp = new int[2];
    private int mNavigationBarWindowState = 0;
    int[] mAbsPos = new int[2];
    ArrayList<Runnable> mPostCollapseRunnables = new ArrayList<>();
    int mDisabled = 0;
    int mSystemUiVisibility = 0;
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
            }
        }
    };
    private final ContentObserver mHeadsUpObserver = new ContentObserver(this.mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            boolean wasUsing = PhoneStatusBar.this.mUseHeadsUp;
            PhoneStatusBar.this.mUseHeadsUp = (PhoneStatusBar.this.mDisableNotificationAlerts || Settings.Global.getInt(PhoneStatusBar.this.mContext.getContentResolver(), "heads_up_notifications_enabled", 0) == 0) ? false : true;
            PhoneStatusBar.this.mHeadsUpTicker = PhoneStatusBar.this.mUseHeadsUp && Settings.Global.getInt(PhoneStatusBar.this.mContext.getContentResolver(), "ticker_gets_heads_up", 0) != 0;
            Log.d("PhoneStatusBar", "heads up is " + (PhoneStatusBar.this.mUseHeadsUp ? "enabled" : "disabled"));
            if (wasUsing != PhoneStatusBar.this.mUseHeadsUp) {
                if (PhoneStatusBar.this.mUseHeadsUp) {
                    PhoneStatusBar.this.addHeadsUpView();
                    return;
                }
                Log.d("PhoneStatusBar", "dismissing any existing heads up notification on disable event");
                PhoneStatusBar.this.setHeadsUpVisibility(false);
                PhoneStatusBar.this.mHeadsUpNotificationView.release();
                PhoneStatusBar.this.removeHeadsUpView();
            }
        }
    };
    private final Runnable mAutohide = new Runnable() {
        @Override
        public void run() {
            int requested = PhoneStatusBar.this.mSystemUiVisibility & (-201326593);
            if (PhoneStatusBar.this.mSystemUiVisibility != requested) {
                PhoneStatusBar.this.notifyUiVisibilityChanged(requested);
            }
        }
    };
    private Interpolator mLinearInterpolator = new LinearInterpolator();
    private Interpolator mBackdropInterpolator = new AccelerateDecelerateInterpolator();
    private PorterDuffXfermode mSrcXferMode = new PorterDuffXfermode(PorterDuff.Mode.SRC);
    private PorterDuffXfermode mSrcOverXferMode = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    private MediaController.Callback mMediaListener = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            PhoneStatusBar.this.mMediaMetadata = metadata;
            PhoneStatusBar.this.updateMediaMetaData(true);
        }
    };
    private final NotificationStackScrollLayout.OnChildLocationsChangedListener mOnChildLocationsChangedListener = new NotificationStackScrollLayout.OnChildLocationsChangedListener() {
        @Override
        public void onChildLocationsChanged(NotificationStackScrollLayout stackScrollLayout) {
            PhoneStatusBar.this.userActivity();
        }
    };
    private final ArraySet<String> mCurrentlyVisibleNotifications = new ArraySet<>();
    private final ShadeUpdates mShadeUpdates = new ShadeUpdates();
    private final NotificationStackScrollLayout.OnChildLocationsChangedListener mNotificationLocationsChangedListener = new NotificationStackScrollLayout.OnChildLocationsChangedListener() {
        @Override
        public void onChildLocationsChanged(NotificationStackScrollLayout stackScrollLayout) {
            if (!PhoneStatusBar.this.mHandler.hasCallbacks(PhoneStatusBar.this.mVisibilityReporter)) {
                long nextReportUptimeMs = PhoneStatusBar.this.mLastVisibilityReportUptimeMs + 500;
                PhoneStatusBar.this.mHandler.postAtTime(PhoneStatusBar.this.mVisibilityReporter, nextReportUptimeMs);
            }
        }
    };
    private final Runnable mVisibilityReporter = new Runnable() {
        private final ArrayList<String> mTmpNewlyVisibleNotifications = new ArrayList<>();
        private final ArrayList<String> mTmpCurrentlyVisibleNotifications = new ArrayList<>();

        @Override
        public void run() {
            PhoneStatusBar.this.mLastVisibilityReportUptimeMs = SystemClock.uptimeMillis();
            ArrayList<NotificationData.Entry> activeNotifications = PhoneStatusBar.this.mNotificationData.getActiveNotifications();
            int N = activeNotifications.size();
            for (int i = 0; i < N; i++) {
                NotificationData.Entry entry = activeNotifications.get(i);
                String key = entry.notification.getKey();
                boolean previouslyVisible = PhoneStatusBar.this.mCurrentlyVisibleNotifications.contains(key);
                boolean currentlyVisible = (PhoneStatusBar.this.mStackScroller.getChildLocation(entry.row) & 29) != 0;
                if (currentlyVisible) {
                    this.mTmpCurrentlyVisibleNotifications.add(key);
                }
                if (!previouslyVisible && currentlyVisible) {
                    this.mTmpNewlyVisibleNotifications.add(key);
                }
            }
            ArraySet<String> noLongerVisibleNotifications = PhoneStatusBar.this.mCurrentlyVisibleNotifications;
            noLongerVisibleNotifications.removeAll(this.mTmpCurrentlyVisibleNotifications);
            PhoneStatusBar.this.logNotificationVisibilityChanges(this.mTmpNewlyVisibleNotifications, noLongerVisibleNotifications);
            PhoneStatusBar.this.mCurrentlyVisibleNotifications.clear();
            PhoneStatusBar.this.mCurrentlyVisibleNotifications.addAll(this.mTmpCurrentlyVisibleNotifications);
            this.mTmpNewlyVisibleNotifications.clear();
            this.mTmpCurrentlyVisibleNotifications.clear();
        }
    };
    private final View.OnClickListener mOverflowClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PhoneStatusBar.this.goToLockedShade(null);
        }
    };
    private View.OnClickListener mRecentsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PhoneStatusBar.this.awakenDreams();
            PhoneStatusBar.this.toggleRecentApps();
        }
    };
    private View.OnLongClickListener mLongPressBackRecentsListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            PhoneStatusBar.this.handleLongPressBackRecents(v);
            return true;
        }
    };
    private int mShowSearchHoldoff = 0;
    private Runnable mShowSearchPanel = new Runnable() {
        @Override
        public void run() {
            PhoneStatusBar.this.showSearchPanel();
            PhoneStatusBar.this.awakenDreams();
        }
    };
    View.OnTouchListener mHomeActionListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case 0:
                    if (!PhoneStatusBar.this.shouldDisableNavbarGestures()) {
                        PhoneStatusBar.this.mHandler.removeCallbacks(PhoneStatusBar.this.mShowSearchPanel);
                        PhoneStatusBar.this.mHandler.postDelayed(PhoneStatusBar.this.mShowSearchPanel, PhoneStatusBar.this.mShowSearchHoldoff);
                    }
                    break;
                case 1:
                case 3:
                    PhoneStatusBar.this.mHandler.removeCallbacks(PhoneStatusBar.this.mShowSearchPanel);
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
    View.OnFocusChangeListener mFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            v.setSelected(hasFocus);
        }
    };
    private final Runnable mAnimateCollapsePanels = new Runnable() {
        @Override
        public void run() {
            PhoneStatusBar.this.animateCollapsePanels();
        }
    };
    final TimeInterpolator mAccelerateInterpolator = new AccelerateInterpolator();
    final TimeInterpolator mDecelerateInterpolator = new DecelerateInterpolator();
    final int FLIP_DURATION_OUT = 125;
    final int FLIP_DURATION_IN = 225;
    final int FLIP_DURATION = 350;
    private final Runnable mCheckBarModes = new Runnable() {
        @Override
        public void run() {
            PhoneStatusBar.this.checkBarModes();
        }
    };
    Animation.AnimationListener mTickingDoneListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            PhoneStatusBar.this.mTicking = false;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }
    };
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PhoneStatusBar.DEBUG) {
                Log.v("PhoneStatusBar", "onReceive: " + intent);
            }
            String action = intent.getAction();
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                if (PhoneStatusBar.this.isCurrentProfile(getSendingUserId())) {
                    int flags = 0;
                    String reason = intent.getStringExtra("reason");
                    if (reason != null && reason.equals("recentapps")) {
                        flags = 0 | 2;
                    }
                    PhoneStatusBar.this.animateCollapsePanels(flags);
                    return;
                }
                return;
            }
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                PhoneStatusBar.this.mScreenOn = false;
                PhoneStatusBar.this.notifyNavigationBarScreenOn(false);
                PhoneStatusBar.this.notifyHeadsUpScreenOn(false);
                PhoneStatusBar.this.finishBarAnimations();
                PhoneStatusBar.this.resetUserExpandedStates();
                return;
            }
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                PhoneStatusBar.this.mScreenOn = true;
                PhoneStatusBar.this.notifyNavigationBarScreenOn(true);
                return;
            }
            if ("com.android.systemui.demo".equals(action)) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    String command = bundle.getString("command", "").trim().toLowerCase();
                    if (command.length() > 0) {
                        try {
                            PhoneStatusBar.this.dispatchDemoCommand(command, bundle);
                            return;
                        } catch (Throwable t) {
                            Log.w("PhoneStatusBar", "Error running demo command, intent=" + intent, t);
                            return;
                        }
                    }
                    return;
                }
                return;
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

    static int access$3908(PhoneStatusBar x0) {
        int i = x0.mDrawCount;
        x0.mDrawCount = i + 1;
        return i;
    }

    @Override
    public void start() {
        this.mDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        updateDisplaySize();
        this.mScrimSrcModeEnabled = this.mContext.getResources().getBoolean(R.bool.config_status_bar_scrim_behind_use_src);
        super.start();
        this.mMediaSessionManager = (MediaSessionManager) this.mContext.getSystemService("media_session");
        addNavigationBar();
        this.mIconPolicy = new PhoneStatusBarPolicy(this.mContext, this.mCastController, this.mHotspotController);
        this.mSettingsObserver.onChange(false);
        this.mHeadsUpObserver.onChange(true);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("heads_up_notifications_enabled"), true, this.mHeadsUpObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("ticker_gets_heads_up"), true, this.mHeadsUpObserver);
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(this.mContext);
        this.mUnlockMethodCache.addListener(this);
        startKeyguard();
        this.mDozeServiceHost = new DozeServiceHost();
        putComponent(DozeHost.class, this.mDozeServiceHost);
        putComponent(PhoneStatusBar.class, this);
        setControllerUsers();
        notifyUserAboutHiddenNotifications();
        this.mScreenPinningRequest = new ScreenPinningRequest(this.mContext);
    }

    protected PhoneStatusBarView makeStatusBarView() {
        ViewStub tickerStub;
        Context context = this.mContext;
        Resources res = context.getResources();
        updateDisplaySize();
        updateResources();
        this.mIconSize = res.getDimensionPixelSize(android.R.dimen.accessibility_magnification_thumbnail_container_stroke_width);
        this.mStatusBarWindow = (StatusBarWindowView) View.inflate(context, R.layout.super_status_bar, null);
        this.mStatusBarWindow.mService = this;
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
        this.mStatusBarView = (PhoneStatusBarView) this.mStatusBarWindow.findViewById(R.id.status_bar);
        this.mStatusBarView.setBar(this);
        PanelHolder holder = (PanelHolder) this.mStatusBarWindow.findViewById(R.id.panel_holder);
        this.mStatusBarView.setPanelHolder(holder);
        this.mNotificationPanel = (NotificationPanelView) this.mStatusBarWindow.findViewById(R.id.notification_panel);
        this.mNotificationPanel.setStatusBar(this);
        if (!ActivityManager.isHighEndGfx()) {
            this.mStatusBarWindow.setBackground(null);
            this.mNotificationPanel.setBackground(new FastColorDrawable(context.getResources().getColor(R.color.notification_panel_solid_background)));
        }
        this.mHeadsUpNotificationView = (HeadsUpNotificationView) View.inflate(context, R.layout.heads_up, null);
        this.mHeadsUpNotificationView.setVisibility(8);
        this.mHeadsUpNotificationView.setBar(this);
        updateShowSearchHoldoff();
        try {
            boolean showNav = this.mWindowManagerService.hasNavigationBar();
            if (DEBUG) {
                Log.v("PhoneStatusBar", "hasNavigationBar=" + showNav);
            }
            if (showNav) {
                this.mNavigationBarView = (NavigationBarView) View.inflate(context, R.layout.navigation_bar, null);
                this.mNavigationBarView.setDisabledFlags(this.mDisabled);
                this.mNavigationBarView.setBar(this);
                this.mNavigationBarView.setOnVerticalChangedListener(new NavigationBarView.OnVerticalChangedListener() {
                    @Override
                    public void onVerticalChanged(boolean isVertical) {
                        if (PhoneStatusBar.this.mSearchPanelView != null) {
                            PhoneStatusBar.this.mSearchPanelView.setHorizontal(isVertical);
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
        } catch (RemoteException e) {
        }
        this.mPixelFormat = -1;
        this.mSystemIconArea = (LinearLayout) this.mStatusBarView.findViewById(R.id.system_icon_area);
        this.mSystemIcons = (LinearLayout) this.mStatusBarView.findViewById(R.id.system_icons);
        this.mStatusIcons = (LinearLayout) this.mStatusBarView.findViewById(R.id.statusIcons);
        this.mNotificationIconArea = this.mStatusBarView.findViewById(R.id.notification_icon_area_inner);
        this.mNotificationIcons = (IconMerger) this.mStatusBarView.findViewById(R.id.notificationIcons);
        this.mMoreIcon = this.mStatusBarView.findViewById(R.id.moreIcon);
        this.mNotificationIcons.setOverflowIndicator(this.mMoreIcon);
        this.mStatusBarContents = (LinearLayout) this.mStatusBarView.findViewById(R.id.status_bar_contents);
        this.mStackScroller = (NotificationStackScrollLayout) this.mStatusBarWindow.findViewById(R.id.notification_stack_scroller);
        this.mStackScroller.setLongPressListener(getNotificationLongClicker());
        this.mStackScroller.setPhoneStatusBar(this);
        this.mKeyguardIconOverflowContainer = (NotificationOverflowContainer) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_notification_keyguard_overflow, (ViewGroup) this.mStackScroller, false);
        this.mKeyguardIconOverflowContainer.setOnActivatedListener(this);
        this.mKeyguardIconOverflowContainer.setOnClickListener(this.mOverflowClickListener);
        this.mStackScroller.addView(this.mKeyguardIconOverflowContainer);
        SpeedBumpView speedBump = (SpeedBumpView) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_notification_speed_bump, (ViewGroup) this.mStackScroller, false);
        this.mStackScroller.setSpeedBumpView(speedBump);
        this.mEmptyShadeView = (EmptyShadeView) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_no_notifications, (ViewGroup) this.mStackScroller, false);
        this.mStackScroller.setEmptyShadeView(this.mEmptyShadeView);
        this.mDismissView = (DismissView) LayoutInflater.from(this.mContext).inflate(R.layout.status_bar_notification_dismiss_all, (ViewGroup) this.mStackScroller, false);
        this.mDismissView.setOnButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhoneStatusBar.this.clearAllNotifications();
            }
        });
        this.mStackScroller.setDismissView(this.mDismissView);
        this.mExpandedContents = this.mStackScroller;
        this.mBackdrop = (BackDropView) this.mStatusBarWindow.findViewById(R.id.backdrop);
        this.mBackdropFront = (ImageView) this.mBackdrop.findViewById(R.id.backdrop_front);
        this.mBackdropBack = (ImageView) this.mBackdrop.findViewById(R.id.backdrop_back);
        ScrimView scrimBehind = (ScrimView) this.mStatusBarWindow.findViewById(R.id.scrim_behind);
        ScrimView scrimInFront = (ScrimView) this.mStatusBarWindow.findViewById(R.id.scrim_in_front);
        this.mScrimController = new ScrimController(scrimBehind, scrimInFront, this.mScrimSrcModeEnabled);
        this.mScrimController.setBackDropView(this.mBackdrop);
        this.mStatusBarView.setScrimController(this.mScrimController);
        this.mDozeScrimController = new DozeScrimController(this.mScrimController, context);
        this.mHeader = (StatusBarHeaderView) this.mStatusBarWindow.findViewById(R.id.header);
        this.mHeader.setActivityStarter(this);
        this.mKeyguardStatusBar = (KeyguardStatusBarView) this.mStatusBarWindow.findViewById(R.id.keyguard_header);
        this.mStatusIconsKeyguard = (LinearLayout) this.mKeyguardStatusBar.findViewById(R.id.statusIcons);
        this.mKeyguardStatusView = this.mStatusBarWindow.findViewById(R.id.keyguard_status_view);
        this.mKeyguardBottomArea = (KeyguardBottomAreaView) this.mStatusBarWindow.findViewById(R.id.keyguard_bottom_area);
        this.mKeyguardBottomArea.setActivityStarter(this);
        this.mKeyguardIndicationController = new KeyguardIndicationController(this.mContext, (KeyguardIndicationTextView) this.mStatusBarWindow.findViewById(R.id.keyguard_indication_text));
        this.mKeyguardBottomArea.setKeyguardIndicationController(this.mKeyguardIndicationController);
        this.mTickerEnabled = res.getBoolean(R.bool.enable_ticker);
        if (this.mTickerEnabled && (tickerStub = (ViewStub) this.mStatusBarView.findViewById(R.id.ticker_stub)) != null) {
            this.mTickerView = tickerStub.inflate();
            this.mTicker = new MyTicker(context, this.mStatusBarView);
            TickerView tickerView = (TickerView) this.mStatusBarView.findViewById(R.id.tickerText);
            tickerView.mTicker = this.mTicker;
        }
        this.mEdgeBorder = res.getDimensionPixelSize(R.dimen.status_bar_edge_ignore);
        setAreThereNotifications();
        this.mHandlerThread = new HandlerThread("PhoneStatusBar", 10);
        this.mHandlerThread.start();
        this.mLocationController = new LocationControllerImpl(this.mContext);
        this.mBatteryController = new BatteryController(this.mContext);
        this.mBatteryController.addStateChangedCallback(new BatteryController.BatteryStateChangeCallback() {
            @Override
            public void onPowerSaveChanged() {
                PhoneStatusBar.this.mHandler.post(PhoneStatusBar.this.mCheckBarModes);
                if (PhoneStatusBar.this.mDozeServiceHost != null) {
                    PhoneStatusBar.this.mDozeServiceHost.firePowerSaveChanged(PhoneStatusBar.this.mBatteryController.isPowerSave());
                }
            }

            @Override
            public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
            }
        });
        this.mNetworkController = new NetworkControllerImpl(this.mContext);
        this.mHotspotController = new HotspotControllerImpl(this.mContext);
        this.mBluetoothController = new BluetoothControllerImpl(this.mContext, this.mHandlerThread.getLooper());
        this.mSecurityController = new SecurityControllerImpl(this.mContext);
        if (this.mContext.getResources().getBoolean(R.bool.config_showRotationLock)) {
            this.mRotationLockController = new RotationLockControllerImpl(this.mContext);
        }
        this.mUserInfoController = new UserInfoController(this.mContext);
        this.mVolumeComponent = (VolumeComponent) getComponent(VolumeComponent.class);
        if (this.mVolumeComponent != null) {
            this.mZenModeController = this.mVolumeComponent.getZenController();
        }
        this.mCastController = new CastControllerImpl(this.mContext);
        SignalClusterView signalCluster = (SignalClusterView) this.mStatusBarView.findViewById(R.id.signal_cluster);
        SignalClusterView signalClusterKeyguard = (SignalClusterView) this.mKeyguardStatusBar.findViewById(R.id.signal_cluster);
        SignalClusterView signalClusterQs = (SignalClusterView) this.mHeader.findViewById(R.id.signal_cluster);
        this.mNetworkController.addSignalCluster(signalCluster);
        this.mNetworkController.addSignalCluster(signalClusterKeyguard);
        this.mNetworkController.addSignalCluster(signalClusterQs);
        signalCluster.setSecurityController(this.mSecurityController);
        signalCluster.setNetworkController(this.mNetworkController);
        signalClusterKeyguard.setSecurityController(this.mSecurityController);
        signalClusterKeyguard.setNetworkController(this.mNetworkController);
        signalClusterQs.setSecurityController(this.mSecurityController);
        signalClusterQs.setNetworkController(this.mNetworkController);
        boolean isAPhone = this.mNetworkController.hasVoiceCallingFeature();
        if (isAPhone) {
            this.mNetworkController.addEmergencyListener(new NetworkControllerImpl.EmergencyListener() {
                @Override
                public void setEmergencyCallsOnly(boolean emergencyOnly) {
                    PhoneStatusBar.this.mHeader.setShowEmergencyCallsOnly(emergencyOnly);
                }
            });
        }
        this.mCarrierLabel = (TextView) this.mStatusBarWindow.findViewById(R.id.carrier_label);
        this.mShowCarrierInPanel = this.mCarrierLabel != null;
        if (DEBUG) {
            Log.v("PhoneStatusBar", "carrierlabel=" + this.mCarrierLabel + " show=" + this.mShowCarrierInPanel);
        }
        if (this.mShowCarrierInPanel) {
            this.mCarrierLabel.setVisibility(this.mCarrierLabelVisible ? 0 : 4);
            this.mNetworkController.addCarrierLabel(new NetworkControllerImpl.CarrierLabelListener() {
                @Override
                public void setCarrierLabel(String label) {
                    PhoneStatusBar.this.mCarrierLabel.setText(label);
                    if (PhoneStatusBar.this.mNetworkController.hasMobileDataFeature()) {
                        if (TextUtils.isEmpty(label)) {
                            PhoneStatusBar.this.mCarrierLabel.setVisibility(8);
                        } else {
                            PhoneStatusBar.this.mCarrierLabel.setVisibility(0);
                        }
                    }
                }
            });
        }
        this.mFlashlightController = new FlashlightController(this.mContext);
        this.mKeyguardBottomArea.setFlashlightController(this.mFlashlightController);
        this.mKeyguardBottomArea.setPhoneStatusBar(this);
        this.mAccessibilityController = new AccessibilityController(this.mContext);
        this.mKeyguardBottomArea.setAccessibilityController(this.mAccessibilityController);
        this.mNextAlarmController = new NextAlarmController(this.mContext);
        this.mKeyguardMonitor = new KeyguardMonitor();
        if (UserSwitcherController.isUserSwitcherAvailable(UserManager.get(this.mContext))) {
            this.mUserSwitcherController = new UserSwitcherController(this.mContext, this.mKeyguardMonitor);
        }
        this.mKeyguardUserSwitcher = new KeyguardUserSwitcher(this.mContext, (ViewStub) this.mStatusBarWindow.findViewById(R.id.keyguard_user_switcher), this.mKeyguardStatusBar, this.mNotificationPanel, this.mUserSwitcherController);
        this.mQSPanel = (QSPanel) this.mStatusBarWindow.findViewById(R.id.quick_settings_panel);
        if (this.mQSPanel != null) {
            final QSTileHost qsh = new QSTileHost(this.mContext, this, this.mBluetoothController, this.mLocationController, this.mRotationLockController, this.mNetworkController, this.mZenModeController, this.mHotspotController, this.mCastController, this.mFlashlightController, this.mUserSwitcherController, this.mKeyguardMonitor, this.mSecurityController);
            this.mQSPanel.setHost(qsh);
            this.mQSPanel.setTiles(qsh.getTiles());
            this.mBrightnessMirrorController = new BrightnessMirrorController(this.mStatusBarWindow);
            this.mQSPanel.setBrightnessMirror(this.mBrightnessMirrorController);
            this.mHeader.setQSPanel(this.mQSPanel);
            qsh.setCallback(new QSTile.Host.Callback() {
                @Override
                public void onTilesChanged() {
                    PhoneStatusBar.this.mQSPanel.setTiles(qsh.getTiles());
                }
            });
        }
        this.mHeader.setUserInfoController(this.mUserInfoController);
        this.mKeyguardStatusBar.setUserInfoController(this.mUserInfoController);
        this.mUserInfoController.reloadUserInfo();
        this.mHeader.setBatteryController(this.mBatteryController);
        ((BatteryMeterView) this.mStatusBarView.findViewById(R.id.battery)).setBatteryController(this.mBatteryController);
        this.mKeyguardStatusBar.setBatteryController(this.mBatteryController);
        this.mHeader.setNextAlarmController(this.mNextAlarmController);
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        this.mBroadcastReceiver.onReceive(this.mContext, new Intent(pm.isScreenOn() ? "android.intent.action.SCREEN_ON" : "android.intent.action.SCREEN_OFF"));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("com.android.systemui.demo");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
        resetUserSetupObserver();
        startGlyphRasterizeHack();
        return this.mStatusBarView;
    }

    public void clearAllNotifications() {
        int numChildren = this.mStackScroller.getChildCount();
        ArrayList<View> viewsToHide = new ArrayList<>(numChildren);
        for (int i = 0; i < numChildren; i++) {
            View child = this.mStackScroller.getChildAt(i);
            if (this.mStackScroller.canChildBeDismissed(child) && child.getVisibility() == 0) {
                viewsToHide.add(child);
            }
        }
        if (viewsToHide.isEmpty()) {
            animateCollapsePanels(0);
        } else {
            addPostCollapseAction(new Runnable() {
                @Override
                public void run() {
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
                PhoneStatusBar.this.mStackScroller.post(new Runnable() {
                    @Override
                    public void run() {
                        PhoneStatusBar.this.mStackScroller.setDismissAllInProgress(false);
                    }
                });
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

    private void startGlyphRasterizeHack() {
        this.mStatusBarView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (PhoneStatusBar.this.mDrawCount == 1) {
                    PhoneStatusBar.this.mStatusBarView.getViewTreeObserver().removeOnPreDrawListener(this);
                    HardwareCanvas.setProperty("extraRasterBucket", Float.toString(0.95f));
                    HardwareCanvas.setProperty("extraRasterBucket", Float.toString(PhoneStatusBar.this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_time_collapsed_size) / PhoneStatusBar.this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_time_expanded_size)));
                }
                PhoneStatusBar.access$3908(PhoneStatusBar.this);
                return true;
            }
        });
    }

    @Override
    protected void setZenMode(int mode) {
        super.setZenMode(mode);
        if (this.mIconPolicy != null) {
            this.mIconPolicy.setZenMode(mode);
        }
    }

    private void startKeyguard() {
        KeyguardViewMediator keyguardViewMediator = (KeyguardViewMediator) getComponent(KeyguardViewMediator.class);
        this.mStatusBarKeyguardViewManager = keyguardViewMediator.registerStatusBar(this, this.mStatusBarWindow, this.mStatusBarWindowManager, this.mScrimController);
        this.mKeyguardViewMediatorCallback = keyguardViewMediator.getViewMediatorCallback();
    }

    @Override
    protected View getStatusBarView() {
        return this.mStatusBarView;
    }

    public StatusBarWindowView getStatusBarWindow() {
        return this.mStatusBarWindow;
    }

    @Override
    protected WindowManager.LayoutParams getSearchLayoutParams(ViewGroup.LayoutParams layoutParams) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2024, 8519936, 0 != 0 ? -1 : -3);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        }
        lp.gravity = 8388691;
        lp.setTitle("SearchPanel");
        lp.softInputMode = 49;
        return lp;
    }

    @Override
    protected void updateSearchPanel() {
        super.updateSearchPanel();
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setDelegateView(this.mSearchPanelView);
        }
    }

    @Override
    public void showSearchPanel() {
        super.showSearchPanel();
        this.mHandler.removeCallbacks(this.mShowSearchPanel);
        this.mSearchPanelView.setSystemUiVisibility(this.mSystemUiVisibility);
        if (this.mNavigationBarView != null) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) this.mNavigationBarView.getLayoutParams();
            lp.flags &= -33;
            this.mWindowManager.updateViewLayout(this.mNavigationBarView, lp);
        }
    }

    public int getStatusBarHeight() {
        if (this.mNaturalBarHeight < 0) {
            Resources res = this.mContext.getResources();
            this.mNaturalBarHeight = res.getDimensionPixelSize(android.R.dimen.accessibility_focus_highlight_stroke_width);
        }
        return this.mNaturalBarHeight;
    }

    public void awakenDreams() {
        if (this.mDreamManager != null) {
            try {
                this.mDreamManager.awaken();
            } catch (RemoteException e) {
            }
        }
    }

    private void prepareNavigationBarView() {
        this.mNavigationBarView.reorient();
        this.mNavigationBarView.getRecentsButton().setOnClickListener(this.mRecentsClickListener);
        this.mNavigationBarView.getRecentsButton().setOnTouchListener(this.mRecentsPreloadOnTouchListener);
        this.mNavigationBarView.getRecentsButton().setLongClickable(true);
        this.mNavigationBarView.getRecentsButton().setOnLongClickListener(this.mLongPressBackRecentsListener);
        this.mNavigationBarView.getBackButton().setLongClickable(true);
        this.mNavigationBarView.getBackButton().setOnLongClickListener(this.mLongPressBackRecentsListener);
        this.mNavigationBarView.getHomeButton().setOnTouchListener(this.mHomeActionListener);
        updateSearchPanel();
    }

    private void addNavigationBar() {
        if (DEBUG) {
            Log.v("PhoneStatusBar", "addNavigationBar: about to add " + this.mNavigationBarView);
        }
        if (this.mNavigationBarView != null) {
            prepareNavigationBarView();
            this.mWindowManager.addView(this.mNavigationBarView, getNavigationBarLayoutParams());
        }
    }

    private void repositionNavigationBar() {
        if (this.mNavigationBarView != null && this.mNavigationBarView.isAttachedToWindow()) {
            prepareNavigationBarView();
            this.mWindowManager.updateViewLayout(this.mNavigationBarView, getNavigationBarLayoutParams());
        }
    }

    public void notifyNavigationBarScreenOn(boolean screenOn) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.notifyScreenOn(screenOn);
        }
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

    public void addHeadsUpView() {
        int headsUpHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.heads_up_window_height);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, headsUpHeight, 2014, 8520488, -3);
        lp.flags |= 16777216;
        lp.gravity = 48;
        lp.setTitle("Heads Up");
        lp.packageName = this.mContext.getPackageName();
        lp.windowAnimations = R.style.Animation_StatusBar_HeadsUp;
        this.mWindowManager.addView(this.mHeadsUpNotificationView, lp);
    }

    public void removeHeadsUpView() {
        this.mWindowManager.removeView(this.mHeadsUpNotificationView);
    }

    public void refreshAllStatusBarIcons() {
        refreshAllIconsForLayout(this.mStatusIcons);
        refreshAllIconsForLayout(this.mStatusIconsKeyguard);
        refreshAllIconsForLayout(this.mNotificationIcons);
    }

    private void refreshAllIconsForLayout(LinearLayout ll) {
        int count = ll.getChildCount();
        for (int n = 0; n < count; n++) {
            View child = ll.getChildAt(n);
            if (child instanceof StatusBarIconView) {
                ((StatusBarIconView) child).updateDrawable();
            }
        }
    }

    @Override
    public void addIcon(String slot, int index, int viewIndex, StatusBarIcon icon) {
        StatusBarIconView view = new StatusBarIconView(this.mContext, slot, null);
        view.set(icon);
        this.mStatusIcons.addView(view, viewIndex, new LinearLayout.LayoutParams(-2, this.mIconSize));
        StatusBarIconView view2 = new StatusBarIconView(this.mContext, slot, null);
        view2.set(icon);
        this.mStatusIconsKeyguard.addView(view2, viewIndex, new LinearLayout.LayoutParams(-2, this.mIconSize));
    }

    @Override
    public void updateIcon(String slot, int index, int viewIndex, StatusBarIcon old, StatusBarIcon icon) {
        StatusBarIconView view = (StatusBarIconView) this.mStatusIcons.getChildAt(viewIndex);
        view.set(icon);
        StatusBarIconView view2 = (StatusBarIconView) this.mStatusIconsKeyguard.getChildAt(viewIndex);
        view2.set(icon);
    }

    @Override
    public void removeIcon(String slot, int index, int viewIndex) {
        this.mStatusIcons.removeViewAt(viewIndex);
        this.mStatusIconsKeyguard.removeViewAt(viewIndex);
    }

    @Override
    public void addNotification(StatusBarNotification notification, NotificationListenerService.RankingMap ranking) {
        if (DEBUG) {
            Log.d("PhoneStatusBar", "addNotification key=" + notification.getKey());
        }
        if (this.mUseHeadsUp && shouldInterrupt(notification)) {
            if (DEBUG) {
                Log.d("PhoneStatusBar", "launching notification in heads up mode");
            }
            NotificationData.Entry interruptionCandidate = new NotificationData.Entry(notification, null);
            ViewGroup holder = this.mHeadsUpNotificationView.getHolder();
            if (inflateViewsForHeadsUp(interruptionCandidate, holder)) {
                this.mHeadsUpNotificationView.showNotification(interruptionCandidate);
                return;
            }
        }
        NotificationData.Entry shadeEntry = createNotificationViews(notification);
        if (shadeEntry != null) {
            if (notification.getNotification().fullScreenIntent != null) {
                awakenDreams();
                if (DEBUG) {
                    Log.d("PhoneStatusBar", "Notification has fullScreenIntent; sending fullScreenIntent");
                }
                try {
                    EventLog.writeEvent(36002, notification.getKey());
                    notification.getNotification().fullScreenIntent.send();
                } catch (PendingIntent.CanceledException e) {
                }
            } else if (this.mHeadsUpNotificationView.getEntry() == null) {
                tick(notification, true);
            }
            addNotificationViews(shadeEntry, ranking);
            setAreThereNotifications();
            updateExpandedViewPos(-10000);
        }
    }

    public void displayNotificationFromHeadsUp(StatusBarNotification notification) {
        NotificationData.Entry shadeEntry = createNotificationViews(notification);
        if (shadeEntry != null) {
            shadeEntry.setInterruption();
            addNotificationViews(shadeEntry, null);
            setAreThereNotifications();
            updateExpandedViewPos(-10000);
        }
    }

    @Override
    public void resetHeadsUpDecayTimer() {
        this.mHandler.removeMessages(1031);
        if (this.mUseHeadsUp && this.mHeadsUpNotificationDecay > 0 && this.mHeadsUpNotificationView.isClearable()) {
            this.mHandler.sendEmptyMessageDelayed(1031, this.mHeadsUpNotificationDecay);
        }
    }

    public void scheduleHeadsUpOpen() {
        this.mHandler.removeMessages(1028);
        this.mHandler.sendEmptyMessage(1028);
    }

    public void scheduleHeadsUpClose() {
        this.mHandler.removeMessages(1029);
        this.mHandler.sendEmptyMessage(1029);
    }

    @Override
    public void scheduleHeadsUpEscalation() {
        this.mHandler.removeMessages(1030);
        this.mHandler.sendEmptyMessage(1030);
    }

    @Override
    protected void updateNotificationRanking(NotificationListenerService.RankingMap ranking) {
        this.mNotificationData.updateRanking(ranking);
        updateNotifications();
    }

    @Override
    public void removeNotification(String key, NotificationListenerService.RankingMap ranking) {
        if (this.mHeadsUpNotificationView.getEntry() != null && key.equals(this.mHeadsUpNotificationView.getEntry().notification.getKey())) {
            this.mHeadsUpNotificationView.clear();
        }
        StatusBarNotification old = removeNotificationViews(key, ranking);
        if (old != null) {
            if (this.mTickerEnabled) {
                this.mTicker.removeEntry(old);
            }
            updateExpandedViewPos(-10000);
            if (!hasActiveNotifications() && !this.mNotificationPanel.isTracking() && !this.mNotificationPanel.isQsExpanded()) {
                if (this.mState == 0) {
                    animateCollapsePanels();
                } else if (this.mState == 2) {
                    goToKeyguard();
                }
            }
        }
        setAreThereNotifications();
    }

    @Override
    protected void refreshLayout(int layoutDirection) {
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.setLayoutDirection(layoutDirection);
        }
        refreshAllStatusBarIcons();
    }

    private void updateShowSearchHoldoff() {
        this.mShowSearchHoldoff = this.mContext.getResources().getInteger(R.integer.config_show_search_delay);
    }

    public void updateNotificationShade() {
        if (this.mStackScroller != null) {
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
                boolean sensitive = (sensitiveNote && hideSensitive) || sensitivePackage;
                boolean showingPublic = sensitive && isLockscreenPublicMode();
                ent.row.setSensitive(sensitive);
                if (ent.autoRedacted && ent.legacy) {
                    if (showingPublic) {
                        ent.row.setShowingLegacyBackground(false);
                    } else {
                        ent.row.setShowingLegacyBackground(true);
                    }
                }
                toShow.add(ent.row);
            }
            ArrayList<View> toRemove = new ArrayList<>();
            for (int i2 = 0; i2 < this.mStackScroller.getChildCount(); i2++) {
                View child = this.mStackScroller.getChildAt(i2);
                if (!toShow.contains(child) && (child instanceof ExpandableNotificationRow)) {
                    toRemove.add(child);
                }
            }
            for (View remove : toRemove) {
                this.mStackScroller.removeView(remove);
            }
            for (int i3 = 0; i3 < toShow.size(); i3++) {
                ExpandableNotificationRow v = toShow.get(i3);
                if (v.getParent() == null) {
                    this.mStackScroller.addView(v);
                }
            }
            int j = 0;
            for (int i4 = 0; i4 < this.mStackScroller.getChildCount(); i4++) {
                View child2 = this.mStackScroller.getChildAt(i4);
                if (child2 instanceof ExpandableNotificationRow) {
                    if (child2 == toShow.get(j)) {
                        j++;
                    } else {
                        this.mStackScroller.changeViewPosition(toShow.get(j), i4);
                        j++;
                    }
                }
            }
            updateRowStates();
            updateSpeedbump();
            updateClearAll();
            updateEmptyShadeView();
            this.mNotificationPanel.setQsExpansionEnabled(isDeviceProvisioned() && (this.mUserSetup || this.mUserSwitcherController == null || !this.mUserSwitcherController.isSimpleUserSwitcher()));
            this.mShadeUpdates.check();
        }
    }

    private boolean packageHasVisibilityOverride(String key) {
        return this.mNotificationData.getVisibilityOverride(key) != -1000;
    }

    private void updateClearAll() {
        boolean showDismissView = this.mState != 1 && this.mNotificationData.hasActiveClearableNotifications();
        this.mStackScroller.updateDismissView(showDismissView);
    }

    private void updateEmptyShadeView() {
        boolean showEmptyShade = this.mState != 1 && this.mNotificationData.getActiveNotifications().size() == 0;
        this.mNotificationPanel.setShadeEmpty(showEmptyShade);
    }

    private void updateSpeedbump() {
        int speedbumpIndex = -1;
        int currentIndex = 0;
        ArrayList<NotificationData.Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
        int N = activeNotifications.size();
        int i = 0;
        while (true) {
            if (i >= N) {
                break;
            }
            NotificationData.Entry entry = activeNotifications.get(i);
            if (entry.row.getVisibility() != 8 && this.mNotificationData.isAmbient(entry.key)) {
                speedbumpIndex = currentIndex;
                break;
            } else {
                currentIndex++;
                i++;
            }
        }
        this.mStackScroller.updateSpeedBumpIndex(speedbumpIndex);
    }

    @Override
    protected void updateNotifications() {
        if (this.mNotificationIcons != null) {
            this.mNotificationData.filterAndSort();
            updateNotificationShade();
            updateNotificationIcons();
        }
    }

    private void updateNotificationIcons() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(this.mIconSize + (this.mIconHPadding * 2), this.mNaturalBarHeight);
        ArrayList<NotificationData.Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
        int N = activeNotifications.size();
        ArrayList<StatusBarIconView> toShow = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            NotificationData.Entry ent = activeNotifications.get(i);
            if (ent.notification.getScore() >= -10 || NotificationData.showNotificationEvenIfUnprovisioned(ent.notification)) {
                toShow.add(ent.icon);
            }
        }
        if (DEBUG) {
            Log.d("PhoneStatusBar", "refreshing icons: " + toShow.size() + " notifications, mNotificationIcons=" + this.mNotificationIcons);
        }
        ArrayList<View> toRemove = new ArrayList<>();
        for (int i2 = 0; i2 < this.mNotificationIcons.getChildCount(); i2++) {
            View child = this.mNotificationIcons.getChildAt(i2);
            if (!toShow.contains(child)) {
                toRemove.add(child);
            }
        }
        int toRemoveCount = toRemove.size();
        for (int i3 = 0; i3 < toRemoveCount; i3++) {
            this.mNotificationIcons.removeView(toRemove.get(i3));
        }
        for (int i4 = 0; i4 < toShow.size(); i4++) {
            StatusBarIconView v = toShow.get(i4);
            if (v.getParent() == null) {
                this.mNotificationIcons.addView(v, i4, params);
            }
        }
        int childCount = this.mNotificationIcons.getChildCount();
        for (int i5 = 0; i5 < childCount; i5++) {
            View actual = this.mNotificationIcons.getChildAt(i5);
            StatusBarIconView expected = toShow.get(i5);
            if (actual != expected) {
                this.mNotificationIcons.removeView(expected);
                this.mNotificationIcons.addView(expected, i5);
            }
        }
    }

    @Override
    protected void updateRowStates() {
        super.updateRowStates();
        this.mNotificationPanel.notifyVisibleChildrenChanged();
    }

    protected void updateCarrierLabelVisibility(boolean force) {
        if (this.mShowCarrierInPanel) {
            boolean makeVisible = !this.mNetworkController.isEmergencyOnly() && this.mStackScroller.getHeight() < (this.mNotificationPanel.getHeight() - this.mCarrierLabelHeight) - this.mStatusBarHeaderHeight && this.mStackScroller.getVisibility() == 0 && this.mState != 1;
            if (force || this.mCarrierLabelVisible != makeVisible) {
                this.mCarrierLabelVisible = makeVisible;
                if (DEBUG) {
                    Log.d("PhoneStatusBar", "making carrier label " + (makeVisible ? "visible" : "invisible"));
                }
                this.mCarrierLabel.animate().cancel();
                if (makeVisible) {
                    this.mCarrierLabel.setVisibility(0);
                }
                this.mCarrierLabel.animate().alpha(makeVisible ? 1.0f : 0.0f).setDuration(150L).setListener(makeVisible ? null : new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!PhoneStatusBar.this.mCarrierLabelVisible) {
                            PhoneStatusBar.this.mCarrierLabel.setVisibility(4);
                            PhoneStatusBar.this.mCarrierLabel.setAlpha(0.0f);
                        }
                    }
                }).start();
            }
        }
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
            nlo.animate().alpha(showDot ? 1.0f : 0.0f).setDuration(showDot ? 750L : 250L).setInterpolator(new AccelerateInterpolator(2.0f)).setListener(showDot ? null : new AnimatorListenerAdapter() {
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
        PlaybackState state;
        MediaSession.Token token;
        boolean metaDataChanged = false;
        synchronized (this.mNotificationData) {
            ArrayList<NotificationData.Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
            int N = activeNotifications.size();
            NotificationData.Entry mediaNotification = null;
            MediaController controller = null;
            for (int i = 0; i < N; i++) {
                NotificationData.Entry entry = activeNotifications.get(i);
                if (isMediaNotification(entry) && (token = (MediaSession.Token) entry.notification.getNotification().extras.getParcelable("android.mediaSession")) != null && (controller = new MediaController(this.mContext, token)) != null) {
                    mediaNotification = entry;
                }
            }
            if (mediaNotification == null && this.mMediaSessionManager != null) {
                List<MediaController> sessions = this.mMediaSessionManager.getActiveSessionsForUser(null, -1);
                for (MediaController aController : sessions) {
                    if (aController != null && (state = aController.getPlaybackState()) != null) {
                        switch (state.getState()) {
                            case 1:
                            case 7:
                                break;
                            default:
                                String pkg = aController.getPackageName();
                                int i2 = 0;
                                while (true) {
                                    if (i2 < N) {
                                        NotificationData.Entry entry2 = activeNotifications.get(i2);
                                        if (!entry2.notification.getPackageName().equals(pkg)) {
                                            i2++;
                                        } else {
                                            controller = aController;
                                            mediaNotification = entry2;
                                        }
                                    }
                                    break;
                                }
                                break;
                        }
                    }
                }
            }
            if (!sameSessions(this.mMediaController, controller)) {
                if (this.mMediaController != null) {
                    Log.v("PhoneStatusBar", "DEBUG_MEDIA: Disconnecting from old controller: " + this.mMediaController);
                    this.mMediaController.unregisterCallback(this.mMediaListener);
                }
                this.mMediaController = controller;
                if (this.mMediaController != null) {
                    this.mMediaController.registerCallback(this.mMediaListener);
                    this.mMediaMetadata = this.mMediaController.getMetadata();
                    String notificationKey = mediaNotification == null ? null : mediaNotification.notification.getKey();
                    if (notificationKey == null || !notificationKey.equals(this.mMediaNotificationKey)) {
                        this.mMediaNotificationKey = notificationKey;
                    }
                } else {
                    this.mMediaMetadata = null;
                    this.mMediaNotificationKey = null;
                }
                metaDataChanged = true;
            }
        }
        updateMediaMetaData(metaDataChanged);
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

    public void updateMediaMetaData(boolean metaDataChanged) {
        if (this.mBackdrop != null) {
            Bitmap artworkBitmap = null;
            if (this.mMediaMetadata != null && (artworkBitmap = this.mMediaMetadata.getBitmap("android.media.metadata.ART")) == null) {
                artworkBitmap = this.mMediaMetadata.getBitmap("android.media.metadata.ALBUM_ART");
            }
            boolean hasArtwork = artworkBitmap != null;
            if (hasArtwork && (this.mState == 1 || this.mState == 2)) {
                if (this.mBackdrop.getVisibility() != 0) {
                    this.mBackdrop.setVisibility(0);
                    this.mBackdrop.animate().alpha(1.0f);
                    metaDataChanged = true;
                }
                if (metaDataChanged) {
                    if (this.mBackdropBack.getDrawable() != null) {
                        Drawable drawable = this.mBackdropBack.getDrawable();
                        this.mBackdropFront.setImageDrawable(drawable);
                        if (this.mScrimSrcModeEnabled) {
                            this.mBackdropFront.getDrawable().mutate().setXfermode(this.mSrcOverXferMode);
                        }
                        this.mBackdropFront.setAlpha(1.0f);
                        this.mBackdropFront.setVisibility(0);
                    } else {
                        this.mBackdropFront.setVisibility(4);
                    }
                    this.mBackdropBack.setImageBitmap(artworkBitmap);
                    if (this.mScrimSrcModeEnabled) {
                        this.mBackdropBack.getDrawable().mutate().setXfermode(this.mSrcXferMode);
                    }
                    if (this.mBackdropFront.getVisibility() == 0) {
                        this.mBackdropFront.animate().setDuration(250L).alpha(0.0f).withEndAction(this.mHideBackdropFront);
                        return;
                    }
                    return;
                }
                return;
            }
            if (this.mBackdrop.getVisibility() != 8) {
                this.mBackdrop.animate().alpha(0.0f).setInterpolator(this.mBackdropInterpolator).setDuration(300L).setStartDelay(0L).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        PhoneStatusBar.this.mBackdrop.setVisibility(8);
                        PhoneStatusBar.this.mBackdropFront.animate().cancel();
                        PhoneStatusBar.this.mBackdropBack.animate().cancel();
                        PhoneStatusBar.this.mHandler.post(PhoneStatusBar.this.mHideBackdropFront);
                    }
                });
                if (this.mKeyguardFadingAway) {
                    this.mBackdrop.animate().setDuration(this.mKeyguardFadingAwayDuration / 2).setStartDelay(this.mKeyguardFadingAwayDelay).setInterpolator(this.mLinearInterpolator).start();
                }
            }
        }
    }

    public void showClock(boolean show) {
        View clock;
        if (this.mStatusBarView != null && (clock = this.mStatusBarView.findViewById(R.id.clock)) != null) {
            clock.setVisibility(show ? 0 : 8);
        }
    }

    private int adjustDisableFlags(int state) {
        if (this.mLaunchTransitionFadingAway) {
            return state;
        }
        if (this.mExpandedVisible || this.mBouncerShowing || this.mWaitingForKeyguardExit) {
            return state | 131072 | 1048576;
        }
        return state;
    }

    @Override
    public void disable(int state, boolean animate) {
        this.mDisabledUnmodified = state;
        int state2 = adjustDisableFlags(state);
        int old = this.mDisabled;
        int diff = state2 ^ old;
        this.mDisabled = state2;
        if (DEBUG) {
            Log.d("PhoneStatusBar", String.format("disable: 0x%08x -> 0x%08x (diff: 0x%08x)", Integer.valueOf(old), Integer.valueOf(state2), Integer.valueOf(diff)));
        }
        StringBuilder flagdbg = new StringBuilder();
        flagdbg.append("disable: < ");
        flagdbg.append((65536 & state2) != 0 ? "EXPAND" : "expand");
        flagdbg.append((65536 & diff) != 0 ? "* " : " ");
        flagdbg.append((131072 & state2) != 0 ? "ICONS" : "icons");
        flagdbg.append((131072 & diff) != 0 ? "* " : " ");
        flagdbg.append((262144 & state2) != 0 ? "ALERTS" : "alerts");
        flagdbg.append((262144 & diff) != 0 ? "* " : " ");
        flagdbg.append((1048576 & state2) != 0 ? "SYSTEM_INFO" : "system_info");
        flagdbg.append((1048576 & diff) != 0 ? "* " : " ");
        flagdbg.append((4194304 & state2) != 0 ? "BACK" : "back");
        flagdbg.append((4194304 & diff) != 0 ? "* " : " ");
        flagdbg.append((2097152 & state2) != 0 ? "HOME" : "home");
        flagdbg.append((2097152 & diff) != 0 ? "* " : " ");
        flagdbg.append((16777216 & state2) != 0 ? "RECENT" : "recent");
        flagdbg.append((16777216 & diff) != 0 ? "* " : " ");
        flagdbg.append((8388608 & state2) != 0 ? "CLOCK" : "clock");
        flagdbg.append((8388608 & diff) != 0 ? "* " : " ");
        flagdbg.append((33554432 & state2) != 0 ? "SEARCH" : "search");
        flagdbg.append((33554432 & diff) != 0 ? "* " : " ");
        flagdbg.append(">");
        Log.d("PhoneStatusBar", flagdbg.toString());
        if ((1048576 & diff) != 0) {
            this.mSystemIconArea.animate().cancel();
            if ((1048576 & state2) != 0) {
                animateStatusBarHide(this.mSystemIconArea, animate);
            } else {
                animateStatusBarShow(this.mSystemIconArea, animate);
            }
        }
        if ((8388608 & diff) != 0) {
            boolean show = (8388608 & state2) == 0;
            showClock(show);
        }
        if ((65536 & diff) != 0 && (65536 & state2) != 0) {
            animateCollapsePanels();
        }
        if ((56623104 & diff) != 0) {
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setDisabledFlags(state2);
            }
            if ((16777216 & state2) != 0) {
                this.mHandler.removeMessages(1020);
                this.mHandler.sendEmptyMessage(1020);
            }
        }
        if ((131072 & diff) != 0) {
            if ((131072 & state2) != 0) {
                if (this.mTicking) {
                    haltTicker();
                }
                animateStatusBarHide(this.mNotificationIconArea, animate);
            } else {
                animateStatusBarShow(this.mNotificationIconArea, animate);
            }
        }
        if ((262144 & diff) != 0) {
            this.mDisableNotificationAlerts = (262144 & state2) != 0;
            this.mHeadsUpObserver.onChange(true);
        }
    }

    private void animateStatusBarHide(final View v, boolean animate) {
        v.animate().cancel();
        if (!animate) {
            v.setAlpha(0.0f);
            v.setVisibility(4);
        } else {
            v.animate().alpha(0.0f).setDuration(160L).setStartDelay(0L).setInterpolator(ALPHA_OUT).withEndAction(new Runnable() {
                @Override
                public void run() {
                    v.setVisibility(4);
                }
            });
        }
    }

    private void animateStatusBarShow(View v, boolean animate) {
        v.animate().cancel();
        v.setVisibility(0);
        if (!animate) {
            v.setAlpha(1.0f);
            return;
        }
        v.animate().alpha(1.0f).setDuration(320L).setInterpolator(ALPHA_IN).setStartDelay(50L).withEndAction(null);
        if (this.mKeyguardFadingAway) {
            v.animate().setDuration(this.mKeyguardFadingAwayDuration).setInterpolator(this.mLinearOutSlowIn).setStartDelay(this.mKeyguardFadingAwayDelay).start();
        }
    }

    @Override
    protected BaseStatusBar.H createHandler() {
        return new H();
    }

    @Override
    public void startActivity(Intent intent, boolean dismissShade) {
        startActivityDismissingKeyguard(intent, false, dismissShade);
    }

    public void setQsExpanded(boolean expanded) {
        this.mStatusBarWindowManager.setQsExpanded(expanded);
    }

    public boolean isGoingToNotificationShade() {
        return this.mLeaveOpenOnKeyguardHide;
    }

    public boolean isQsExpanded() {
        return this.mNotificationPanel.isQsExpanded();
    }

    public boolean isScreenOnComingFromTouch() {
        return this.mScreenOnComingFromTouch;
    }

    public boolean isFalsingThresholdNeeded() {
        boolean onKeyguard = getBarState() == 1;
        boolean isCurrentlyInsecure = this.mUnlockMethodCache.isCurrentlyInsecure();
        return onKeyguard && (isCurrentlyInsecure || this.mDozing || this.mScreenOnComingFromTouch);
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

    private class H extends BaseStatusBar.H {
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
                    PhoneStatusBar.this.animateExpandSettingsPanel();
                    break;
                case 1003:
                    PhoneStatusBar.this.onLaunchTransitionTimeout();
                    break;
                case 1028:
                    PhoneStatusBar.this.setHeadsUpVisibility(true);
                    break;
                case 1029:
                    PhoneStatusBar.this.mHeadsUpNotificationView.release();
                    PhoneStatusBar.this.setHeadsUpVisibility(false);
                    break;
                case 1030:
                    PhoneStatusBar.this.escalateHeadsUp();
                    PhoneStatusBar.this.setHeadsUpVisibility(false);
                    break;
                case 1031:
                    PhoneStatusBar.this.mHeadsUpNotificationView.release();
                    PhoneStatusBar.this.setHeadsUpVisibility(false);
                    break;
            }
        }
    }

    public void escalateHeadsUp() {
        if (this.mHeadsUpNotificationView.getEntry() != null) {
            StatusBarNotification sbn = this.mHeadsUpNotificationView.getEntry().notification;
            this.mHeadsUpNotificationView.release();
            Notification notification = sbn.getNotification();
            if (notification.fullScreenIntent != null) {
                if (DEBUG) {
                    Log.d("PhoneStatusBar", "converting a heads up to fullScreen");
                }
                try {
                    EventLog.writeEvent(36003, sbn.getKey());
                    notification.fullScreenIntent.send();
                } catch (PendingIntent.CanceledException e) {
                }
            }
        }
    }

    boolean panelsEnabled() {
        int dcha_state = BenesseExtension.getDchaState();
        return dcha_state == 0 && (this.mDisabled & 65536) == 0;
    }

    void makeExpandedVisible(boolean force) {
        if (force || (!this.mExpandedVisible && panelsEnabled())) {
            this.mExpandedVisible = true;
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setSlippery(true);
            }
            updateCarrierLabelVisibility(true);
            updateExpandedViewPos(-10000);
            this.mStatusBarWindowManager.setStatusBarExpanded(true);
            this.mStatusBarView.setFocusable(false);
            visibilityChanged(true);
            this.mWaitingForKeyguardExit = false;
            disable(this.mDisabledUnmodified, force ? false : true);
            setInteracting(1, true);
        }
    }

    public void animateCollapsePanels() {
        animateCollapsePanels(0);
    }

    public void postAnimateCollapsePanels() {
        this.mHandler.post(this.mAnimateCollapsePanels);
    }

    @Override
    public void animateCollapsePanels(int flags) {
        animateCollapsePanels(flags, false);
    }

    @Override
    public void animateCollapsePanels(int flags, boolean force) {
        if (!force && (this.mState == 1 || this.mState == 2)) {
            runPostCollapseRunnables();
            return;
        }
        if ((flags & 2) == 0 && !this.mHandler.hasMessages(1020)) {
            this.mHandler.removeMessages(1020);
            this.mHandler.sendEmptyMessage(1020);
        }
        if ((flags & 1) == 0) {
            this.mHandler.removeMessages(1027);
            this.mHandler.sendEmptyMessage(1027);
        }
        if (this.mStatusBarWindow != null) {
            this.mStatusBarWindowManager.setStatusBarFocusable(false);
            this.mStatusBarWindow.cancelExpandHelper();
            this.mStatusBarView.collapseAllPanels(true);
        }
    }

    private void runPostCollapseRunnables() {
        int size = this.mPostCollapseRunnables.size();
        for (int i = 0; i < size; i++) {
            this.mPostCollapseRunnables.get(i).run();
        }
        this.mPostCollapseRunnables.clear();
    }

    @Override
    public void animateExpandNotificationsPanel() {
        if (panelsEnabled()) {
            this.mNotificationPanel.expand();
        }
    }

    @Override
    public void animateExpandSettingsPanel() {
        if (panelsEnabled() && this.mUserSetup) {
            this.mNotificationPanel.expandWithQs();
        }
    }

    public void animateCollapseQuickSettings() {
        if (this.mState == 0) {
            this.mStatusBarView.collapseAllPanels(true);
        }
    }

    void makeExpandedInvisible() {
        if (this.mExpandedVisible && this.mStatusBarWindow != null) {
            this.mStatusBarView.collapseAllPanels(false);
            if (this.mScrollViewAnim != null) {
                this.mScrollViewAnim.cancel();
            }
            if (this.mClearButtonAnim != null) {
                this.mClearButtonAnim.cancel();
            }
            this.mStackScroller.setVisibility(0);
            this.mNotificationPanel.setVisibility(8);
            this.mNotificationPanel.closeQs();
            this.mExpandedVisible = false;
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setSlippery(false);
            }
            visibilityChanged(false);
            this.mStatusBarWindowManager.setStatusBarExpanded(false);
            this.mStatusBarView.setFocusable(true);
            dismissPopups();
            runPostCollapseRunnables();
            setInteracting(1, false);
            showBouncer();
            disable(this.mDisabledUnmodified, true);
            if (!this.mStatusBarKeyguardViewManager.isShowing()) {
                WindowManagerGlobal.getInstance().trimMemory(20);
            }
        }
    }

    public boolean interceptTouchEvent(MotionEvent event) {
        if (CHATTY && event.getAction() != 2) {
            Log.d("PhoneStatusBar", String.format("panel: %s at (%f, %f) mDisabled=0x%08x", MotionEvent.actionToString(event.getAction()), Float.valueOf(event.getRawX()), Float.valueOf(event.getRawY()), Integer.valueOf(this.mDisabled)));
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
        if (hints != this.mNavigationIconHints) {
            this.mNavigationIconHints = hints;
            if (this.mNavigationBarView != null) {
                this.mNavigationBarView.setNavigationIconHints(hints);
            }
            checkBarModes();
        }
    }

    @Override
    public void setWindowState(int window, int state) {
        boolean showing = state == 0;
        if (this.mStatusBarWindow != null && window == 1 && this.mStatusBarWindowState != state) {
            this.mStatusBarWindowState = state;
            if (!showing && this.mState == 0) {
                this.mStatusBarView.collapseAllPanels(false);
            }
        }
        if (this.mNavigationBarView != null && window == 2 && this.mNavigationBarWindowState != state) {
            this.mNavigationBarWindowState = state;
        }
    }

    @Override
    public void buzzBeepBlinked() {
        if (this.mDozeServiceHost != null) {
            this.mDozeServiceHost.fireBuzzBeepBlinked();
        }
    }

    @Override
    public void notificationLightOff() {
        if (this.mDozeServiceHost != null) {
            this.mDozeServiceHost.fireNotificationLight(false);
        }
    }

    @Override
    public void notificationLightPulse(int argb, int onMillis, int offMillis) {
        if (this.mDozeServiceHost != null) {
            this.mDozeServiceHost.fireNotificationLight(true);
        }
    }

    @Override
    public void setSystemUiVisibility(int vis, int mask) {
        int oldVal = this.mSystemUiVisibility;
        int newVal = ((mask ^ (-1)) & oldVal) | (vis & mask);
        int diff = newVal ^ oldVal;
        if (DEBUG) {
            Log.d("PhoneStatusBar", String.format("setSystemUiVisibility vis=%s mask=%s oldVal=%s newVal=%s diff=%s", Integer.toHexString(vis), Integer.toHexString(mask), Integer.toHexString(oldVal), Integer.toHexString(newVal), Integer.toHexString(diff)));
        }
        if (diff != 0) {
            boolean wasRecentsVisible = (this.mSystemUiVisibility & 16384) > 0;
            this.mSystemUiVisibility = newVal;
            if ((diff & 1) != 0) {
                boolean lightsOut = (vis & 1) != 0;
                if (lightsOut) {
                    animateCollapsePanels();
                    if (this.mTicking) {
                        haltTicker();
                    }
                }
                setAreThereNotifications();
            }
            int sbMode = computeBarMode(oldVal, newVal, this.mStatusBarView.getBarTransitions(), 67108864, 1073741824);
            int nbMode = this.mNavigationBarView == null ? -1 : computeBarMode(oldVal, newVal, this.mNavigationBarView.getBarTransitions(), 134217728, Integer.MIN_VALUE);
            boolean sbModeChanged = sbMode != -1;
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
            if ((268435456 & vis) != 0) {
                this.mSystemUiVisibility &= -268435457;
            }
            if ((536870912 & vis) != 0) {
                this.mSystemUiVisibility &= -536870913;
            }
            if (wasRecentsVisible) {
                this.mSystemUiVisibility |= 16384;
            }
            notifyUiVisibilityChanged(this.mSystemUiVisibility);
        }
    }

    private int computeBarMode(int oldVis, int newVis, BarTransitions transitions, int transientFlag, int translucentFlag) {
        int oldMode = barMode(oldVis, transientFlag, translucentFlag);
        int newMode = barMode(newVis, transientFlag, translucentFlag);
        if (oldMode == newMode) {
            return -1;
        }
        return newMode;
    }

    private int barMode(int vis, int transientFlag, int translucentFlag) {
        if ((vis & transientFlag) != 0) {
            return 1;
        }
        if ((vis & translucentFlag) != 0) {
            return 2;
        }
        if ((vis & 32769) == 32769) {
            return 6;
        }
        if ((32768 & vis) != 0) {
            return 4;
        }
        return (vis & 1) != 0 ? 3 : 0;
    }

    public void checkBarModes() {
        if (!this.mDemoMode) {
            checkBarMode(this.mStatusBarMode, this.mStatusBarWindowState, this.mStatusBarView.getBarTransitions());
            if (this.mNavigationBarView != null) {
                checkBarMode(this.mNavigationBarMode, this.mNavigationBarWindowState, this.mNavigationBarView.getBarTransitions());
            }
        }
    }

    private void checkBarMode(int mode, int windowState, BarTransitions transitions) {
        boolean powerSave = this.mBatteryController.isPowerSave();
        boolean anim = ((this.mScreenOn != null && !this.mScreenOn.booleanValue()) || windowState == 2 || powerSave) ? false : true;
        if (powerSave && getBarState() == 0) {
            mode = 5;
        }
        transitions.transitionTo(mode, anim);
    }

    public void finishBarAnimations() {
        this.mStatusBarView.getBarTransitions().finishAnimations();
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.getBarTransitions().finishAnimations();
        }
    }

    @Override
    public void setInteracting(int barWindow, boolean interacting) {
        boolean changing = ((this.mInteractingWindows & barWindow) != 0) != interacting;
        this.mInteractingWindows = interacting ? this.mInteractingWindows | barWindow : this.mInteractingWindows & (barWindow ^ (-1));
        if (this.mInteractingWindows != 0) {
            suspendAutohide();
        } else {
            resumeSuspendedAutohide();
        }
        if (changing && interacting && barWindow == 2 && this.mVolumeComponent != null) {
            this.mVolumeComponent.dismissNow();
        }
        checkBarModes();
    }

    private void resumeSuspendedAutohide() {
        if (this.mAutohideSuspended) {
            scheduleAutohide();
            this.mHandler.postDelayed(this.mCheckBarModes, 500L);
        }
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
        if ((this.mSystemUiVisibility & 201326592) != 0 && event.getAction() == 4 && event.getX() == 0.0f && event.getY() == 0.0f) {
            userAutohide();
        }
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
            setSystemUiVisibility(0, 1);
        } else {
            setSystemUiVisibility(1, 1);
        }
    }

    public void notifyUiVisibilityChanged(int vis) {
        try {
            this.mWindowManagerService.statusBarVisibilityChanged(vis);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void topAppWindowChanged(boolean showMenu) {
        if (DEBUG) {
            Log.d("PhoneStatusBar", (showMenu ? "showing" : "hiding") + " the MENU button");
        }
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

    @Override
    protected void tick(StatusBarNotification n, boolean firstTime) {
        if (this.mTickerEnabled && areLightsOn() && isDeviceProvisioned() && isNotificationForCurrentProfiles(n) && n.getNotification().tickerText != null && this.mStatusBarWindow != null && this.mStatusBarWindow.getWindowToken() != null && (this.mDisabled & 655360) == 0) {
            this.mTicker.addEntry(n);
        }
    }

    private class MyTicker extends Ticker {
        MyTicker(Context context, View sb) {
            super(context, sb);
            if (!PhoneStatusBar.this.mTickerEnabled) {
                Log.w("PhoneStatusBar", "MyTicker instantiated with mTickerEnabled=false", new Throwable());
            }
        }

        @Override
        public void tickerStarting() {
            if (PhoneStatusBar.this.mTickerEnabled) {
                PhoneStatusBar.this.mTicking = true;
                PhoneStatusBar.this.mStatusBarContents.setVisibility(8);
                PhoneStatusBar.this.mTickerView.setVisibility(0);
                PhoneStatusBar.this.mTickerView.startAnimation(PhoneStatusBar.this.loadAnim(android.R.anim.ft_avd_toarrow_rectangle_path_6_animation, null));
                PhoneStatusBar.this.mStatusBarContents.startAnimation(PhoneStatusBar.this.loadAnim(android.R.anim.ft_avd_tooverflow_rectangle_1_animation, null));
            }
        }

        @Override
        public void tickerDone() {
            if (PhoneStatusBar.this.mTickerEnabled) {
                PhoneStatusBar.this.mStatusBarContents.setVisibility(0);
                PhoneStatusBar.this.mTickerView.setVisibility(8);
                PhoneStatusBar.this.mStatusBarContents.startAnimation(PhoneStatusBar.this.loadAnim(android.R.anim.ft_avd_toarrow_rectangle_path_2_animation, null));
                PhoneStatusBar.this.mTickerView.startAnimation(PhoneStatusBar.this.loadAnim(android.R.anim.ft_avd_toarrow_rectangle_path_4_animation, PhoneStatusBar.this.mTickingDoneListener));
            }
        }

        @Override
        public void tickerHalting() {
            if (PhoneStatusBar.this.mTickerEnabled) {
                if (PhoneStatusBar.this.mStatusBarContents.getVisibility() != 0) {
                    PhoneStatusBar.this.mStatusBarContents.setVisibility(0);
                    PhoneStatusBar.this.mStatusBarContents.startAnimation(PhoneStatusBar.this.loadAnim(android.R.anim.fade_in, null));
                }
                PhoneStatusBar.this.mTickerView.setVisibility(8);
            }
        }
    }

    public Animation loadAnim(int id, Animation.AnimationListener listener) {
        Animation anim = AnimationUtils.loadAnimation(this.mContext, id);
        if (listener != null) {
            anim.setAnimationListener(listener);
        }
        return anim;
    }

    public static String viewInfo(View v) {
        return "[(" + v.getLeft() + "," + v.getTop() + ")(" + v.getRight() + "," + v.getBottom() + ") " + v.getWidth() + "x" + v.getHeight() + "]";
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mQueueLock) {
            pw.println("Current Status Bar state:");
            pw.println("  mExpandedVisible=" + this.mExpandedVisible + ", mTrackingPosition=" + this.mTrackingPosition);
            pw.println("  mTickerEnabled=" + this.mTickerEnabled);
            if (this.mTickerEnabled) {
                pw.println("  mTicking=" + this.mTicking);
                pw.println("  mTickerView: " + viewInfo(this.mTickerView));
            }
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
        pw.print("  interrupting package: ");
        pw.println(hunStateToString(this.mHeadsUpNotificationView.getEntry()));
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
            pw.print(" title=" + ((Object) this.mMediaMetadata.getText("android.media.metadata.TITLE")));
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
        int N = this.mStatusIcons.getChildCount();
        pw.println("  system icons: " + N);
        for (int i = 0; i < N; i++) {
            StatusBarIconView ic = (StatusBarIconView) this.mStatusIcons.getChildAt(i);
            pw.println("    [" + i + "] icon=" + ic);
        }
        if (this.mNetworkController != null) {
            this.mNetworkController.dump(fd, pw, args);
        }
        if (this.mBluetoothController != null) {
            this.mBluetoothController.dump(fd, pw, args);
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
        pw.println("SharedPreferences:");
        for (Map.Entry<String, ?> entry : this.mContext.getSharedPreferences(this.mContext.getPackageName(), 0).getAll().entrySet()) {
            pw.print("  ");
            pw.print(entry.getKey());
            pw.print("=");
            pw.println(entry.getValue());
        }
    }

    private String hunStateToString(NotificationData.Entry entry) {
        return entry == null ? "null" : entry.notification == null ? "corrupt" : entry.notification.getPackageName();
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
        this.mStatusBarWindowManager.add(this.mStatusBarWindow, getStatusBarHeight());
    }

    @Override
    public void updateExpandedViewPos(int thingy) {
        this.mNotificationPanel.setMinimumHeight((int) (this.mNotificationPanelMinHeightFrac * this.mCurrentDisplaySize.y));
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mNotificationPanel.getLayoutParams();
        lp.gravity = this.mNotificationPanelGravity;
        this.mNotificationPanel.setLayoutParams(lp);
        updateCarrierLabelVisibility(false);
    }

    void updateDisplaySize() {
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        this.mDisplay.getSize(this.mCurrentDisplaySize);
    }

    float getDisplayDensity() {
        return this.mDisplayMetrics.density;
    }

    public void startActivityDismissingKeyguard(final Intent intent, boolean onlyProvisioned, final boolean dismissShade) {
        if (!onlyProvisioned || isDeviceProvisioned()) {
            final boolean afterKeyguardGone = PreviewInflater.wouldLaunchResolverActivity(this.mContext, intent, this.mCurrentUserId);
            final boolean keyguardShowing = this.mStatusBarKeyguardViewManager.isShowing();
            dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() {
                @Override
                public boolean onDismiss() {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (keyguardShowing && !afterKeyguardGone) {
                                    ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                                }
                                intent.setFlags(335544320);
                                PhoneStatusBar.this.mContext.startActivityAsUser(intent, new UserHandle(-2));
                                PhoneStatusBar.this.overrideActivityPendingAppTransition(keyguardShowing && !afterKeyguardGone);
                            } catch (RemoteException e) {
                            }
                        }
                    });
                    if (dismissShade) {
                        PhoneStatusBar.this.animateCollapsePanels(2, true);
                    }
                    return true;
                }
            }, afterKeyguardGone);
        }
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
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            this.mStatusBarKeyguardViewManager.dismissWithAction(action, afterKeyguardGone);
        } else {
            action.onDismiss();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (DEBUG) {
            Log.v("PhoneStatusBar", "configuration changed: " + this.mContext.getResources().getConfiguration());
        }
        updateDisplaySize();
        updateResources();
        updateClockSize();
        repositionNavigationBar();
        updateExpandedViewPos(-10000);
        updateShowSearchHoldoff();
        updateRowStates();
        this.mScreenPinningRequest.onConfigurationChanged();
    }

    @Override
    public void userSwitched(int newUserId) {
        super.userSwitched(newUserId);
        animateCollapsePanels();
        updatePublicMode();
        updateNotifications();
        resetUserSetupObserver();
        setControllerUsers();
    }

    private void setControllerUsers() {
        if (this.mZenModeController != null) {
            this.mZenModeController.setUserId(this.mCurrentUserId);
        }
    }

    private void resetUserSetupObserver() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mUserSetupObserver);
        this.mUserSetupObserver.onChange(false);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), true, this.mUserSetupObserver, this.mCurrentUserId);
    }

    public void setHeadsUpVisibility(boolean vis) {
        if (DEBUG) {
            Log.v("PhoneStatusBar", (vis ? "showing" : "hiding") + " heads up window");
        }
        Object[] objArr = new Object[2];
        objArr[0] = vis ? this.mHeadsUpNotificationView.getKey() : "";
        objArr[1] = Integer.valueOf(vis ? 1 : 0);
        EventLog.writeEvent(36001, objArr);
        this.mHeadsUpNotificationView.setVisibility(vis ? 0 : 8);
    }

    @Override
    public void onHeadsUpDismissed() {
        this.mHeadsUpNotificationView.dismiss();
    }

    void updateResources() {
        if (this.mQSPanel != null) {
            this.mQSPanel.updateResources();
        }
        loadDimens();
        this.mLinearOutSlowIn = AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.linear_out_slow_in);
        if (this.mNotificationPanel != null) {
            this.mNotificationPanel.updateResources();
        }
        if (this.mHeadsUpNotificationView != null) {
            this.mHeadsUpNotificationView.updateResources();
        }
        if (this.mBrightnessMirrorController != null) {
            this.mBrightnessMirrorController.updateResources();
        }
    }

    private void updateClockSize() {
        TextView clock;
        if (this.mStatusBarView != null && (clock = (TextView) this.mStatusBarView.findViewById(R.id.clock)) != null) {
            FontSizeUtils.updateFontSize(clock, R.dimen.status_bar_clock_size);
        }
    }

    protected void loadDimens() {
        Resources res = this.mContext.getResources();
        this.mNaturalBarHeight = res.getDimensionPixelSize(android.R.dimen.accessibility_focus_highlight_stroke_width);
        int newIconSize = res.getDimensionPixelSize(android.R.dimen.accessibility_magnification_thumbnail_container_stroke_width);
        int newIconHPadding = res.getDimensionPixelSize(R.dimen.status_bar_icon_padding);
        if (newIconHPadding != this.mIconHPadding || newIconSize != this.mIconSize) {
            this.mIconHPadding = newIconHPadding;
            this.mIconSize = newIconSize;
        }
        this.mEdgeBorder = res.getDimensionPixelSize(R.dimen.status_bar_edge_ignore);
        this.mNotificationPanelGravity = res.getInteger(R.integer.notification_panel_layout_gravity);
        if (this.mNotificationPanelGravity <= 0) {
            this.mNotificationPanelGravity = 8388659;
        }
        this.mCarrierLabelHeight = res.getDimensionPixelSize(R.dimen.carrier_label_height);
        this.mStatusBarHeaderHeight = res.getDimensionPixelSize(R.dimen.status_bar_header_height);
        this.mNotificationPanelMinHeightFrac = res.getFraction(R.dimen.notification_panel_min_height_frac, 1, 1);
        if (this.mNotificationPanelMinHeightFrac < 0.0f || this.mNotificationPanelMinHeightFrac > 1.0f) {
            this.mNotificationPanelMinHeightFrac = 0.0f;
        }
        this.mHeadsUpNotificationDecay = res.getInteger(R.integer.heads_up_notification_decay);
        this.mRowMinHeight = res.getDimensionPixelSize(R.dimen.notification_min_height);
        this.mRowMaxHeight = res.getDimensionPixelSize(R.dimen.notification_max_height);
        this.mKeyguardMaxNotificationCount = res.getInteger(R.integer.keyguard_max_notification_count);
        if (DEBUG) {
            Log.v("PhoneStatusBar", "updateResources");
        }
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
            this.mCurrentlyVisibleNotifications.clear();
        }
        this.mHandler.removeCallbacks(this.mVisibilityReporter);
        this.mStackScroller.setChildLocationsChangedListener(null);
    }

    private void startNotificationLogging() {
        this.mStackScroller.setChildLocationsChangedListener(this.mNotificationLocationsChangedListener);
        this.mNotificationLocationsChangedListener.onChildLocationsChanged(this.mStackScroller);
    }

    public void logNotificationVisibilityChanges(Collection<String> newlyVisible, Collection<String> noLongerVisible) {
        if (!newlyVisible.isEmpty() || !noLongerVisible.isEmpty()) {
            String[] newlyVisibleAr = (String[]) newlyVisible.toArray(new String[newlyVisible.size()]);
            String[] noLongerVisibleAr = (String[]) noLongerVisible.toArray(new String[noLongerVisible.size()]);
            try {
                this.mBarService.onNotificationVisibilityChanged(newlyVisibleAr, noLongerVisibleAr);
            } catch (RemoteException e) {
            }
        }
    }

    private void logStateToEventlog() {
        boolean isShowing = this.mStatusBarKeyguardViewManager.isShowing();
        boolean isOccluded = this.mStatusBarKeyguardViewManager.isOccluded();
        boolean isBouncerShowing = this.mStatusBarKeyguardViewManager.isBouncerShowing();
        boolean isSecure = this.mUnlockMethodCache.isMethodSecure();
        boolean isCurrentlyInsecure = this.mUnlockMethodCache.isCurrentlyInsecure();
        int stateFingerprint = getLoggingFingerprint(this.mState, isShowing, isOccluded, isBouncerShowing, isSecure, isCurrentlyInsecure);
        if (stateFingerprint != this.mLastLoggedStateFingerprint) {
            EventLogTags.writeSysuiStatusBarState(this.mState, isShowing ? 1 : 0, isOccluded ? 1 : 0, isBouncerShowing ? 1 : 0, isSecure ? 1 : 0, isCurrentlyInsecure ? 1 : 0);
            this.mLastLoggedStateFingerprint = stateFingerprint;
        }
    }

    private static int getLoggingFingerprint(int statusBarState, boolean keyguardShowing, boolean keyguardOccluded, boolean bouncerShowing, boolean secure, boolean currentlyInsecure) {
        return ((currentlyInsecure ? 1 : 0) << 12) | ((secure ? 1 : 0) << 11) | (statusBarState & 255) | ((keyguardShowing ? 1 : 0) << 8) | ((keyguardOccluded ? 1 : 0) << 9) | ((bouncerShowing ? 1 : 0) << 10);
    }

    void vibrate() {
        Vibrator vib = (Vibrator) this.mContext.getSystemService("vibrator");
        vib.vibrate(250L, VIBRATION_ATTRIBUTES);
    }

    @Override
    protected void haltTicker() {
        if (this.mTickerEnabled) {
            this.mTicker.halt();
        }
    }

    @Override
    protected boolean shouldDisableNavbarGestures() {
        return (isDeviceProvisioned() && !this.mExpandedVisible && (this.mDisabled & 33554432) == 0) ? false : true;
    }

    public void postStartSettingsActivity(final Intent intent, int delay) {
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PhoneStatusBar.this.handleStartSettingsActivity(intent, true);
            }
        }, delay);
    }

    public void handleStartSettingsActivity(Intent intent, boolean onlyProvisioned) {
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
        public void setColorFilter(ColorFilter cf) {
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
            boolean modeChange = command.equals("enter") || command.equals("exit");
            if ((modeChange || command.equals("volume")) && this.mVolumeComponent != null) {
                this.mVolumeComponent.dispatchDemoCommand(command, args);
            }
            if (modeChange || command.equals("clock")) {
                dispatchDemoCommandToView(command, args, R.id.clock);
            }
            if (modeChange || command.equals("battery")) {
                dispatchDemoCommandToView(command, args, R.id.battery);
            }
            if (modeChange || command.equals("status")) {
                if (this.mDemoStatusIcons == null) {
                    this.mDemoStatusIcons = new DemoStatusIcons(this.mStatusIcons, this.mIconSize);
                }
                this.mDemoStatusIcons.dispatchDemoCommand(command, args);
            }
            if (this.mNetworkController != null && (modeChange || command.equals("network"))) {
                this.mNetworkController.dispatchDemoCommand(command, args);
            }
            if (modeChange || command.equals("notifications")) {
                View notifications = this.mStatusBarView == null ? null : this.mStatusBarView.findViewById(R.id.notification_icon_area);
                if (notifications != null) {
                    String visible = args.getString("visible");
                    int vis = (this.mDemoMode && "false".equals(visible)) ? 4 : 0;
                    notifications.setVisibility(vis);
                }
            }
            if (command.equals("bars")) {
                String mode = args.getString("mode");
                if ("opaque".equals(mode)) {
                    barMode = 0;
                } else {
                    barMode = "translucent".equals(mode) ? 2 : "semi-transparent".equals(mode) ? 1 : "transparent".equals(mode) ? 4 : "warning".equals(mode) ? 5 : -1;
                }
                if (barMode != -1) {
                    if (this.mStatusBarView != null) {
                        this.mStatusBarView.getBarTransitions().transitionTo(barMode, true);
                    }
                    if (this.mNavigationBarView != null) {
                        this.mNavigationBarView.getBarTransitions().transitionTo(barMode, true);
                    }
                }
            }
        }
    }

    private void dispatchDemoCommandToView(String command, Bundle args, int id) {
        if (this.mStatusBarView != null) {
            KeyEvent.Callback callbackFindViewById = this.mStatusBarView.findViewById(id);
            if (callbackFindViewById instanceof DemoMode) {
                ((DemoMode) callbackFindViewById).dispatchDemoCommand(command, args);
            }
        }
    }

    public int getBarState() {
        return this.mState;
    }

    public void showKeyguard() {
        if (this.mLaunchTransitionFadingAway) {
            this.mNotificationPanel.animate().cancel();
            this.mNotificationPanel.setAlpha(1.0f);
            runLaunchTransitionEndRunnable();
            this.mLaunchTransitionFadingAway = false;
        }
        this.mHandler.removeMessages(1003);
        setBarState(1);
        updateKeyguardState(false, false);
        if (!this.mScreenOnFromKeyguard) {
            this.mNotificationPanel.setTouchDisabled(true);
        }
        instantExpandNotificationsPanel();
        this.mLeaveOpenOnKeyguardHide = false;
        if (this.mDraggedDownRow != null) {
            this.mDraggedDownRow.setUserLocked(false);
            this.mDraggedDownRow.notifyHeightChanged();
            this.mDraggedDownRow = null;
        }
    }

    public boolean isCollapsing() {
        return this.mNotificationPanel.isCollapsing();
    }

    public void addPostCollapseAction(Runnable r) {
        this.mPostCollapseRunnables.add(r);
    }

    public boolean isInLaunchTransition() {
        return this.mNotificationPanel.isLaunchTransitionRunning() || this.mNotificationPanel.isLaunchTransitionFinished();
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
                PhoneStatusBar.this.mNotificationPanel.setAlpha(1.0f);
                PhoneStatusBar.this.mNotificationPanel.animate().alpha(0.0f).setStartDelay(100L).setDuration(300L).withLayer().withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        PhoneStatusBar.this.mNotificationPanel.setAlpha(1.0f);
                        PhoneStatusBar.this.runLaunchTransitionEndRunnable();
                        PhoneStatusBar.this.mLaunchTransitionFadingAway = false;
                    }
                });
            }
        };
        if (this.mNotificationPanel.isLaunchTransitionRunning()) {
            this.mNotificationPanel.setLaunchTransitionEndRunnable(hideRunnable);
        } else {
            hideRunnable.run();
        }
    }

    public void startLaunchTransitionTimeout() {
        this.mHandler.sendEmptyMessageDelayed(1003, 5000L);
    }

    public void onLaunchTransitionTimeout() {
        Log.w("PhoneStatusBar", "Launch transition: Timeout!");
        this.mNotificationPanel.resetViews();
    }

    public void runLaunchTransitionEndRunnable() {
        if (this.mLaunchTransitionEndRunnable != null) {
            Runnable r = this.mLaunchTransitionEndRunnable;
            this.mLaunchTransitionEndRunnable = null;
            r.run();
        }
    }

    public boolean hideKeyguard() {
        boolean staying = this.mLeaveOpenOnKeyguardHide;
        setBarState(0);
        if (this.mLeaveOpenOnKeyguardHide) {
            this.mLeaveOpenOnKeyguardHide = false;
            this.mNotificationPanel.animateToFullShade(calculateGoingToFullShadeDelay());
            if (this.mDraggedDownRow != null) {
                this.mDraggedDownRow.setUserLocked(false);
                this.mDraggedDownRow = null;
            }
        } else {
            instantCollapseNotificationPanel();
        }
        updateKeyguardState(staying, false);
        if (this.mQSPanel != null) {
            this.mQSPanel.refreshAllTiles();
        }
        this.mHandler.removeMessages(1003);
        return staying;
    }

    public long calculateGoingToFullShadeDelay() {
        return this.mKeyguardFadingAwayDelay + this.mKeyguardFadingAwayDuration;
    }

    public void setKeyguardFadingAway(long delay, long fadeoutDuration) {
        this.mKeyguardFadingAway = true;
        this.mKeyguardFadingAwayDelay = delay;
        this.mKeyguardFadingAwayDuration = fadeoutDuration;
        this.mWaitingForKeyguardExit = false;
        disable(this.mDisabledUnmodified, true);
    }

    public boolean isKeyguardFadingAway() {
        return this.mKeyguardFadingAway;
    }

    public void finishKeyguardFadingAway() {
        this.mKeyguardFadingAway = false;
    }

    private void updatePublicMode() {
        setLockscreenPublicMode(this.mStatusBarKeyguardViewManager.isShowing() && this.mStatusBarKeyguardViewManager.isSecure(this.mCurrentUserId));
    }

    private void updateKeyguardState(boolean goingToFullShade, boolean fromShadeLocked) {
        if (this.mState == 1) {
            this.mKeyguardIndicationController.setVisible(true);
            this.mNotificationPanel.resetViews();
            this.mKeyguardUserSwitcher.setKeyguard(true, fromShadeLocked);
        } else {
            this.mKeyguardIndicationController.setVisible(false);
            this.mKeyguardUserSwitcher.setKeyguard(false, goingToFullShade || this.mState == 2 || fromShadeLocked);
        }
        if (this.mState == 1 || this.mState == 2) {
            this.mScrimController.setKeyguardShowing(true);
        } else {
            this.mScrimController.setKeyguardShowing(false);
        }
        this.mNotificationPanel.setBarState(this.mState, this.mKeyguardFadingAway, goingToFullShade);
        updateDozingState();
        updatePublicMode();
        updateStackScrollerState(goingToFullShade);
        updateNotifications();
        checkBarModes();
        updateCarrierLabelVisibility(false);
        updateMediaMetaData(false);
        this.mKeyguardMonitor.notifyKeyguardState(this.mStatusBarKeyguardViewManager.isShowing(), this.mStatusBarKeyguardViewManager.isSecure());
    }

    public void updateDozingState() {
        if (this.mState == 1 || this.mNotificationPanel.isDozing()) {
            boolean animate = !this.mDozing && this.mDozeScrimController.isPulsing();
            this.mNotificationPanel.setDozing(this.mDozing, animate);
            this.mStackScroller.setDark(this.mDozing, animate, this.mScreenOnTouchLocation);
            this.mScrimController.setDozing(this.mDozing);
            this.mDozeScrimController.setDozing(this.mDozing, animate);
        }
    }

    public void updateStackScrollerState(boolean goingToFullShade) {
        if (this.mStackScroller != null) {
            boolean onKeyguard = this.mState == 1;
            this.mStackScroller.setHideSensitive(isLockscreenPublicMode(), goingToFullShade);
            this.mStackScroller.setDimmed(onKeyguard, false);
            this.mStackScroller.setExpandingEnabled(onKeyguard ? false : true);
            ActivatableNotificationView activatedChild = this.mStackScroller.getActivatedChild();
            this.mStackScroller.setActivatedChild(null);
            if (activatedChild != null) {
                activatedChild.makeInactive(false);
            }
        }
    }

    public void userActivity() {
        if (this.mState == 1) {
            this.mKeyguardViewMediatorCallback.userActivity();
        }
    }

    public boolean interceptMediaKey(KeyEvent event) {
        return this.mState == 1 && this.mStatusBarKeyguardViewManager.interceptMediaKey(event);
    }

    public boolean onMenuPressed() {
        return this.mState == 1 && this.mStatusBarKeyguardViewManager.onMenuPressed();
    }

    public boolean onBackPressed() {
        if (this.mStatusBarKeyguardViewManager.onBackPressed()) {
            return true;
        }
        if (this.mNotificationPanel.isQsExpanded()) {
            if (this.mNotificationPanel.isQsDetailShowing()) {
                this.mNotificationPanel.closeQsDetail();
                return true;
            }
            this.mNotificationPanel.animateCloseQs();
            return true;
        }
        if (this.mState != 1 && this.mState != 2) {
            animateCollapsePanels();
            return true;
        }
        return false;
    }

    public boolean onSpacePressed() {
        if (this.mScreenOn == null || !this.mScreenOn.booleanValue() || (this.mState != 1 && this.mState != 2)) {
            return false;
        }
        animateCollapsePanels(2, true);
        return true;
    }

    private void showBouncer() {
        if (this.mState == 1 || this.mState == 2) {
            this.mWaitingForKeyguardExit = this.mStatusBarKeyguardViewManager.isShowing();
            this.mStatusBarKeyguardViewManager.dismiss();
        }
    }

    private void instantExpandNotificationsPanel() {
        makeExpandedVisible(true);
        this.mNotificationPanel.instantExpand();
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
        if (state != this.mState && this.mVisible && state == 2) {
            try {
                this.mBarService.clearNotificationEffects();
            } catch (RemoteException e) {
            }
        }
        this.mState = state;
        this.mStatusBarWindowManager.setStatusBarState(state);
    }

    @Override
    public void onActivationReset(ActivatableNotificationView view) {
        if (view == this.mStackScroller.getActivatedChild()) {
            this.mKeyguardIndicationController.hideTransientIndication();
            this.mStackScroller.setActivatedChild(null);
        }
    }

    public void onTrackingStarted() {
        runPostCollapseRunnables();
    }

    public void onClosingFinished() {
        runPostCollapseRunnables();
    }

    public void onUnlockHintStarted() {
        this.mKeyguardIndicationController.showTransientIndication(R.string.keyguard_unlock);
    }

    public void onHintFinished() {
        this.mKeyguardIndicationController.hideTransientIndicationDelayed(1200L);
    }

    public void onCameraHintStarted() {
        this.mKeyguardIndicationController.showTransientIndication(R.string.camera_hint);
    }

    public void onPhoneHintStarted() {
        this.mKeyguardIndicationController.showTransientIndication(R.string.phone_hint);
    }

    public void onTrackingStopped(boolean expand) {
        if ((this.mState == 1 || this.mState == 2) && !expand && !this.mUnlockMethodCache.isCurrentlyInsecure()) {
            showBouncer();
        }
    }

    @Override
    protected int getMaxKeyguardNotifications() {
        return this.mKeyguardMaxNotificationCount;
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
        return true;
    }

    @Override
    public void onDragDownReset() {
        this.mStackScroller.setDimmed(true, true);
    }

    @Override
    public void onThresholdReached() {
        this.mStackScroller.setDimmed(false, true);
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
            row.setUserExpanded(true);
        }
        boolean fullShadeNeedsBouncer = (userAllowsPrivateNotificationsInPublic(this.mCurrentUserId) && this.mShowLockscreenNotifications) ? false : true;
        if (isLockscreenPublicMode() && fullShadeNeedsBouncer) {
            this.mLeaveOpenOnKeyguardHide = true;
            showBouncer();
            this.mDraggedDownRow = row;
        } else {
            this.mNotificationPanel.animateToFullShade(0L);
            setBarState(2);
            updateKeyguardState(false, false);
            if (row != null) {
                row.setUserLocked(false);
            }
        }
    }

    public void goToKeyguard() {
        if (this.mState == 2) {
            this.mStackScroller.onGoToKeyguard();
            setBarState(1);
            updateKeyguardState(false, true);
        }
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
        disable(this.mDisabledUnmodified, true);
    }

    public void onScreenTurnedOff() {
        this.mScreenOnFromKeyguard = false;
        this.mScreenOnComingFromTouch = false;
        this.mScreenOnTouchLocation = null;
        this.mStackScroller.setAnimationsEnabled(false);
        updateVisibleToUser();
    }

    public void onScreenTurnedOn() {
        this.mScreenOnFromKeyguard = true;
        this.mStackScroller.setAnimationsEnabled(true);
        this.mNotificationPanel.onScreenTurnedOn();
        this.mNotificationPanel.setTouchDisabled(false);
        updateVisibleToUser();
    }

    public void handleLongPressBackRecents(View v) {
        boolean sendBackLongPress = false;
        try {
            IActivityManager activityManager = ActivityManagerNative.getDefault();
            boolean isAccessiblityEnabled = this.mAccessibilityManager.isEnabled() || !this.mContext.getPackageManager().hasSystemFeature("android.hardware.touchscreen.multitouch");
            if (activityManager.isInLockTaskMode() && !isAccessiblityEnabled) {
                long time = System.currentTimeMillis();
                if (time - this.mLastLockToAppLongPress < 200) {
                    activityManager.stopLockTaskModeOnCurrent();
                    this.mNavigationBarView.setDisabledFlags(this.mDisabled, true);
                } else if (v.getId() == R.id.back && !this.mNavigationBarView.getRecentsButton().isPressed()) {
                    sendBackLongPress = true;
                }
                this.mLastLockToAppLongPress = time;
            } else if (v.getId() == R.id.back) {
                sendBackLongPress = true;
            } else if (isAccessiblityEnabled && activityManager.isInLockTaskMode()) {
                activityManager.stopLockTaskModeOnCurrent();
                this.mNavigationBarView.setDisabledFlags(this.mDisabled, true);
            }
            if (sendBackLongPress) {
                KeyButtonView keyButtonView = (KeyButtonView) v;
                keyButtonView.sendEvent(0, 128);
                keyButtonView.sendAccessibilityEvent(2);
            }
        } catch (RemoteException e) {
            Log.d("PhoneStatusBar", "Unable to reach activity manager", e);
        }
    }

    @Override
    protected void showRecents(boolean triggeredFromAltTab) {
        this.mSystemUiVisibility |= 16384;
        notifyUiVisibilityChanged(this.mSystemUiVisibility);
        super.showRecents(triggeredFromAltTab);
    }

    @Override
    protected void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        this.mSystemUiVisibility &= -16385;
        notifyUiVisibilityChanged(this.mSystemUiVisibility);
        super.hideRecents(triggeredFromAltTab, triggeredFromHomeKey);
    }

    @Override
    protected void toggleRecents() {
        this.mSystemUiVisibility ^= 16384;
        notifyUiVisibilityChanged(this.mSystemUiVisibility);
        super.toggleRecents();
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        if (visible) {
            this.mSystemUiVisibility |= 16384;
        } else {
            this.mSystemUiVisibility &= -16385;
        }
        notifyUiVisibilityChanged(this.mSystemUiVisibility);
    }

    @Override
    public void showScreenPinningRequest() {
        if (!this.mKeyguardMonitor.isShowing()) {
            showScreenPinningRequest(true);
        }
    }

    public void showScreenPinningRequest(boolean allowCancel) {
        this.mScreenPinningRequest.showPrompt(allowCancel);
    }

    public boolean hasActiveNotifications() {
        return !this.mNotificationData.getActiveNotifications().isEmpty();
    }

    public void wakeUpIfDozing(long time, MotionEvent event) {
        if (this.mDozing && this.mDozeScrimController.isPulsing()) {
            PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
            pm.wakeUp(time);
            this.mScreenOnComingFromTouch = true;
            this.mScreenOnTouchLocation = new PointF(event.getX(), event.getY());
            this.mNotificationPanel.setTouchDisabled(false);
        }
    }

    private final class ShadeUpdates {
        private final ArraySet<String> mNewVisibleNotifications;
        private final ArraySet<String> mVisibleNotifications;

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
            boolean updates = this.mVisibleNotifications.containsAll(this.mNewVisibleNotifications) ? false : true;
            this.mVisibleNotifications.clear();
            this.mVisibleNotifications.addAll((ArraySet<? extends String>) this.mNewVisibleNotifications);
            if (updates && PhoneStatusBar.this.mDozeServiceHost != null) {
                PhoneStatusBar.this.mDozeServiceHost.fireNewNotifications();
            }
        }
    }

    private final class DozeServiceHost implements DozeHost {
        private final ArrayList<DozeHost.Callback> mCallbacks;
        private final H mHandler;
        private boolean mNotificationLightOn;

        private DozeServiceHost() {
            this.mCallbacks = new ArrayList<>();
            this.mHandler = new H();
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
            return PhoneStatusBar.this.mBatteryController != null && PhoneStatusBar.this.mBatteryController.isPowerSave();
        }

        @Override
        public boolean isNotificationLightOn() {
            return this.mNotificationLightOn;
        }

        public void handleStartDozing(Runnable ready) {
            if (!PhoneStatusBar.this.mDozing) {
                PhoneStatusBar.this.mDozing = true;
                DozeLog.traceDozing(PhoneStatusBar.this.mContext, PhoneStatusBar.this.mDozing);
                PhoneStatusBar.this.updateDozingState();
            }
            ready.run();
        }

        public void handlePulseWhileDozing(DozeHost.PulseCallback callback, int reason) {
            PhoneStatusBar.this.mDozeScrimController.pulse(callback, reason);
        }

        public void handleStopDozing() {
            if (PhoneStatusBar.this.mDozing) {
                PhoneStatusBar.this.mDozing = false;
                DozeLog.traceDozing(PhoneStatusBar.this.mContext, PhoneStatusBar.this.mDozing);
                PhoneStatusBar.this.updateDozingState();
            }
        }

        private final class H extends Handler {
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
}
