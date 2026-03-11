package com.android.systemui.statusbar;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.TaskStackBuilder;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.DejankUtils;
import com.android.systemui.Interpolators;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SwipeHelper;
import com.android.systemui.SystemUI;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.recents.Recents;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationGuts;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.PreviewInflater;
import com.android.systemui.statusbar.policy.RemoteInputView;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class BaseStatusBar extends SystemUI implements CommandQueue.Callbacks, ActivatableNotificationView.OnActivatedListener, ExpandableNotificationRow.ExpansionLogger, NotificationData.Environment, ExpandableNotificationRow.OnExpandClickListener, NotificationGuts.OnGutsClosedListener {
    protected AccessibilityManager mAccessibilityManager;
    protected boolean mAllowLockscreenRemoteInput;
    protected AssistManager mAssistManager;
    protected IStatusBarService mBarService;
    protected boolean mBouncerShowing;
    protected CommandQueue mCommandQueue;
    private int mDensity;
    protected boolean mDeviceInteractive;
    protected DevicePolicyManager mDevicePolicyManager;
    protected DismissView mDismissView;
    protected Display mDisplay;
    protected IDreamManager mDreamManager;
    protected EmptyShadeView mEmptyShadeView;
    private float mFontScale;
    protected HeadsUpManager mHeadsUpManager;
    protected NotificationOverflowContainer mKeyguardIconOverflowContainer;
    private KeyguardManager mKeyguardManager;
    private Locale mLocale;
    private LockPatternUtils mLockPatternUtils;
    protected NotificationData mNotificationData;
    private NotificationGuts mNotificationGutsExposed;
    protected PowerManager mPowerManager;
    protected RecentsComponent mRecents;
    protected RemoteInputController mRemoteInputController;
    protected boolean mShowLockscreenNotifications;
    protected NotificationStackScrollLayout mStackScroller;
    protected int mState;
    protected StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private UserManager mUserManager;
    protected boolean mVisible;
    private boolean mVisibleToUser;
    protected boolean mVrMode;
    protected WindowManager mWindowManager;
    protected IWindowManager mWindowManagerService;
    protected int mZenMode;
    public static final boolean ENABLE_REMOTE_INPUT = SystemProperties.getBoolean("debug.enable_remote_input", true);
    public static final boolean ENABLE_CHILD_NOTIFICATIONS = SystemProperties.getBoolean("debug.child_notifs", true);
    public static final boolean FORCE_REMOTE_INPUT_HISTORY = SystemProperties.getBoolean("debug.force_remoteinput_history", false);
    private static boolean ENABLE_LOCK_SCREEN_ALLOW_REMOTE_INPUT = false;
    protected H mHandler = createHandler();
    protected NotificationGroupManager mGroupManager = new NotificationGroupManager();
    protected int mCurrentUserId = 0;
    protected final SparseArray<UserInfo> mCurrentProfiles = new SparseArray<>();
    protected int mLayoutDirection = -1;
    protected NavigationBarView mNavigationBarView = null;
    protected ArraySet<NotificationData.Entry> mHeadsUpEntriesToRemoveOnSwitch = new ArraySet<>();
    protected ArraySet<NotificationData.Entry> mRemoteInputEntriesToRemoveOnCollapse = new ArraySet<>();
    protected ArraySet<String> mKeysKeptForRemoteInput = new ArraySet<>();
    protected boolean mUseHeadsUp = false;
    protected boolean mHeadsUpTicker = false;
    protected boolean mDisableNotificationAlerts = false;
    private boolean mLockscreenPublicMode = false;
    private final SparseBooleanArray mUsersAllowingPrivateNotifications = new SparseBooleanArray();
    private final SparseBooleanArray mUsersAllowingNotifications = new SparseBooleanArray();
    private boolean mDeviceProvisioned = false;
    private NotificationClicker mNotificationClicker = new NotificationClicker(this, null);
    private final IVrStateCallbacks mVrStateCallbacks = new IVrStateCallbacks.Stub() {
        public void onVrStateChanged(boolean enabled) {
            BaseStatusBar.this.mVrMode = enabled;
        }
    };
    protected final ContentObserver mSettingsObserver = new ContentObserver(this.mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            boolean provisioned = Settings.Global.getInt(BaseStatusBar.this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
            if (provisioned != BaseStatusBar.this.mDeviceProvisioned) {
                BaseStatusBar.this.mDeviceProvisioned = provisioned;
                BaseStatusBar.this.updateNotifications();
            }
            int mode = Settings.Global.getInt(BaseStatusBar.this.mContext.getContentResolver(), "zen_mode", 0);
            BaseStatusBar.this.setZenMode(mode);
            BaseStatusBar.this.updateLockscreenNotificationSetting();
        }
    };
    private final ContentObserver mLockscreenSettingsObserver = new ContentObserver(this.mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            BaseStatusBar.this.mUsersAllowingPrivateNotifications.clear();
            BaseStatusBar.this.mUsersAllowingNotifications.clear();
            BaseStatusBar.this.updateNotifications();
        }
    };
    private RemoteViews.OnClickHandler mOnClickHandler = new RemoteViews.OnClickHandler() {
        public boolean onClickHandler(final View view, final PendingIntent pendingIntent, final Intent fillInIntent) {
            if (handleRemoteInput(view, pendingIntent, fillInIntent)) {
                return true;
            }
            logActionClick(view);
            try {
                ActivityManagerNative.getDefault().resumeAppSwitches();
            } catch (RemoteException e) {
            }
            boolean isActivity = pendingIntent.isActivity();
            if (isActivity) {
                final boolean keyguardShowing = BaseStatusBar.this.mStatusBarKeyguardViewManager.isShowing();
                final boolean afterKeyguardGone = PreviewInflater.wouldLaunchResolverActivity(BaseStatusBar.this.mContext, pendingIntent.getIntent(), BaseStatusBar.this.mCurrentUserId);
                BaseStatusBar.this.dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() {
                    @Override
                    public boolean onDismiss() {
                        if (keyguardShowing && !afterKeyguardGone) {
                            try {
                                ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                                ActivityManagerNative.getDefault().resumeAppSwitches();
                            } catch (RemoteException e2) {
                            }
                        }
                        boolean handled = superOnClickHandler(view, pendingIntent, fillInIntent);
                        BaseStatusBar.this.overrideActivityPendingAppTransition(keyguardShowing && !afterKeyguardGone);
                        if (handled) {
                            BaseStatusBar.this.animateCollapsePanels(2, true);
                            BaseStatusBar.this.visibilityChanged(false);
                            BaseStatusBar.this.mAssistManager.hideAssist();
                        }
                        return handled;
                    }
                }, afterKeyguardGone);
                return true;
            }
            return superOnClickHandler(view, pendingIntent, fillInIntent);
        }

        private void logActionClick(View view) {
            ViewParent parent = view.getParent();
            String key = getNotificationKeyForParent(parent);
            if (key == null) {
                Log.w("StatusBar", "Couldn't determine notification for click.");
                return;
            }
            int index = -1;
            if (view.getId() == 16909215 && parent != null && (parent instanceof ViewGroup)) {
                ViewGroup actionGroup = (ViewGroup) parent;
                index = actionGroup.indexOfChild(view);
            }
            try {
                BaseStatusBar.this.mBarService.onNotificationActionClick(key, index);
            } catch (RemoteException e) {
            }
        }

        private String getNotificationKeyForParent(ViewParent parent) {
            while (parent != null) {
                if (parent instanceof ExpandableNotificationRow) {
                    return ((ExpandableNotificationRow) parent).getStatusBarNotification().getKey();
                }
                parent = parent.getParent();
            }
            return null;
        }

        public boolean superOnClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent) {
            return super.onClickHandler(view, pendingIntent, fillInIntent, 1);
        }

        private boolean handleRemoteInput(View view, PendingIntent pendingIntent, Intent fillInIntent) {
            Object tag = view.getTag(R.id.italic);
            RemoteInput[] inputs = null;
            if (tag instanceof RemoteInput[]) {
                inputs = (RemoteInput[]) tag;
            }
            if (inputs == null) {
                return false;
            }
            RemoteInput input = null;
            for (RemoteInput i : inputs) {
                if (i.getAllowFreeFormInput()) {
                    input = i;
                }
            }
            if (input == null) {
                return false;
            }
            ViewParent p = view.getParent();
            RemoteInputView riv = null;
            while (true) {
                if (p == null) {
                    break;
                }
                if (p instanceof View) {
                    View pv = (View) p;
                    if (pv.isRootNamespace()) {
                        riv = (RemoteInputView) pv.findViewWithTag(RemoteInputView.VIEW_TAG);
                        break;
                    }
                }
                p = p.getParent();
            }
            ExpandableNotificationRow row = null;
            while (true) {
                if (p == null) {
                    break;
                }
                if (p instanceof ExpandableNotificationRow) {
                    row = (ExpandableNotificationRow) p;
                    break;
                }
                p = p.getParent();
            }
            if (riv == null || row == null) {
                return false;
            }
            row.setUserExpanded(true);
            if (!BaseStatusBar.this.mAllowLockscreenRemoteInput) {
                if (BaseStatusBar.this.isLockscreenPublicMode()) {
                    BaseStatusBar.this.onLockedRemoteInput(row, view);
                    return true;
                }
                int userId = pendingIntent.getCreatorUserHandle().getIdentifier();
                if (BaseStatusBar.this.mUserManager.getUserInfo(userId).isManagedProfile() && BaseStatusBar.this.mKeyguardManager.isDeviceLocked(userId)) {
                    BaseStatusBar.this.onLockedWorkRemoteInput(userId, row, view);
                    return true;
                }
            }
            riv.setVisibility(0);
            int cx = view.getLeft() + (view.getWidth() / 2);
            int cy = view.getTop() + (view.getHeight() / 2);
            int w = riv.getWidth();
            int h = riv.getHeight();
            int r = Math.max(Math.max(cx + cy, (h - cy) + cx), Math.max((w - cx) + cy, (w - cx) + (h - cy)));
            ViewAnimationUtils.createCircularReveal(riv, cx, cy, 0.0f, r).start();
            riv.setPendingIntent(pendingIntent);
            riv.setRemoteInput(inputs, input);
            riv.focus();
            return true;
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            UserInfo user;
            String action = intent.getAction();
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                BaseStatusBar.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                BaseStatusBar.this.updateCurrentProfilesCache();
                Log.v("StatusBar", "userId " + BaseStatusBar.this.mCurrentUserId + " is in the house");
                BaseStatusBar.this.updateLockscreenNotificationSetting();
                BaseStatusBar.this.userSwitched(BaseStatusBar.this.mCurrentUserId);
                return;
            }
            if ("android.intent.action.USER_ADDED".equals(action)) {
                BaseStatusBar.this.updateCurrentProfilesCache();
                return;
            }
            if ("android.intent.action.USER_PRESENT".equals(action)) {
                List<ActivityManager.RecentTaskInfo> recentTask = null;
                try {
                    recentTask = ActivityManagerNative.getDefault().getRecentTasks(1, 5, BaseStatusBar.this.mCurrentUserId).getList();
                } catch (RemoteException e) {
                }
                if (recentTask == null || recentTask.size() <= 0 || (user = BaseStatusBar.this.mUserManager.getUserInfo(recentTask.get(0).userId)) == null || !user.isManagedProfile()) {
                    return;
                }
                Toast toast = Toast.makeText(BaseStatusBar.this.mContext, com.android.systemui.R.string.managed_profile_foreground_toast, 0);
                TextView text = (TextView) toast.getView().findViewById(R.id.message);
                text.setCompoundDrawablesRelativeWithIntrinsicBounds(com.android.systemui.R.drawable.stat_sys_managed_profile_status, 0, 0, 0);
                int paddingPx = BaseStatusBar.this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.managed_profile_toast_padding);
                text.setCompoundDrawablePadding(paddingPx);
                toast.show();
                return;
            }
            if ("com.android.systemui.statusbar.banner_action_cancel".equals(action) || "com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                NotificationManager noMan = (NotificationManager) BaseStatusBar.this.mContext.getSystemService("notification");
                noMan.cancel(com.android.systemui.R.id.notification_hidden);
                Settings.Secure.putInt(BaseStatusBar.this.mContext.getContentResolver(), "show_note_about_notification_hiding", 0);
                if (!"com.android.systemui.statusbar.banner_action_setup".equals(action) || BenesseExtension.getDchaState() != 0) {
                    return;
                }
                BaseStatusBar.this.animateCollapsePanels(2, true);
                BaseStatusBar.this.mContext.startActivity(new Intent("android.settings.ACTION_APP_NOTIFICATION_REDACTION").addFlags(268435456));
                return;
            }
            if (!"com.android.systemui.statusbar.work_challenge_unlocked_notification_action".equals(action)) {
                return;
            }
            IntentSender intentSender = (IntentSender) intent.getParcelableExtra("android.intent.extra.INTENT");
            String notificationKey = intent.getStringExtra("android.intent.extra.INDEX");
            if (intentSender != null) {
                try {
                    BaseStatusBar.this.mContext.startIntentSender(intentSender, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e2) {
                }
            }
            if (notificationKey != null) {
                try {
                    BaseStatusBar.this.mBarService.onNotificationClick(notificationKey);
                } catch (RemoteException e3) {
                }
            }
            BaseStatusBar.this.onWorkChallengeUnlocked();
        }
    };
    private final BroadcastReceiver mAllUsersReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!"android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action) || !BaseStatusBar.this.isCurrentProfile(getSendingUserId())) {
                return;
            }
            BaseStatusBar.this.mUsersAllowingPrivateNotifications.clear();
            BaseStatusBar.this.updateLockscreenNotificationSetting();
            BaseStatusBar.this.updateNotifications();
        }
    };
    private final NotificationListenerService mNotificationListener = new NotificationListenerService() {
        @Override
        public void onListenerConnected() {
            final StatusBarNotification[] notifications = getActiveNotifications();
            final NotificationListenerService.RankingMap currentRanking = getCurrentRanking();
            BaseStatusBar.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (StatusBarNotification sbn : notifications) {
                        BaseStatusBar.this.addNotification(sbn, currentRanking, null);
                    }
                }
            });
        }

        @Override
        public void onNotificationPosted(final StatusBarNotification sbn, final NotificationListenerService.RankingMap rankingMap) {
            Log.d("StatusBar", "onNotificationPosted: " + sbn);
            if (sbn == null) {
                return;
            }
            BaseStatusBar.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    BaseStatusBar.this.processForRemoteInput(sbn.getNotification());
                    String key = sbn.getKey();
                    BaseStatusBar.this.mKeysKeptForRemoteInput.remove(key);
                    boolean isUpdate = BaseStatusBar.this.mNotificationData.get(key) != null;
                    if (!BaseStatusBar.ENABLE_CHILD_NOTIFICATIONS && BaseStatusBar.this.mGroupManager.isChildInGroupWithSummary(sbn)) {
                        if (isUpdate) {
                            BaseStatusBar.this.removeNotification(key, rankingMap);
                            return;
                        } else {
                            BaseStatusBar.this.mNotificationData.updateRanking(rankingMap);
                            return;
                        }
                    }
                    if (isUpdate) {
                        BaseStatusBar.this.updateNotification(sbn, rankingMap);
                    } else {
                        BaseStatusBar.this.addNotification(sbn, rankingMap, null);
                    }
                }
            });
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification sbn, final NotificationListenerService.RankingMap rankingMap) {
            Log.d("StatusBar", "onNotificationRemoved: " + sbn);
            if (sbn == null) {
                return;
            }
            final String key = sbn.getKey();
            BaseStatusBar.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    BaseStatusBar.this.removeNotification(key, rankingMap);
                }
            });
        }

        @Override
        public void onNotificationRankingUpdate(final NotificationListenerService.RankingMap rankingMap) {
            if (rankingMap == null) {
                return;
            }
            BaseStatusBar.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    BaseStatusBar.this.updateNotificationRanking(rankingMap);
                }
            });
        }
    };
    protected View.OnTouchListener mRecentsPreloadOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction() & 255;
            if (action == 0) {
                BaseStatusBar.this.preloadRecents();
            } else if (action == 3) {
                BaseStatusBar.this.cancelPreloadingRecents();
            } else if (action == 1 && !v.isPressed()) {
                BaseStatusBar.this.cancelPreloadingRecents();
            }
            return false;
        }
    };

    public abstract void addNotification(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap, NotificationData.Entry entry);

    protected abstract void createAndAddWindows();

    protected abstract int getMaxKeyguardNotifications(boolean z);

    public abstract boolean isPanelFullyCollapsed();

    protected abstract boolean isSnoozedPackage(StatusBarNotification statusBarNotification);

    public abstract void maybeEscalateHeadsUp();

    protected abstract void refreshLayout(int i);

    public abstract void removeNotification(String str, NotificationListenerService.RankingMap rankingMap);

    protected abstract void setAreThereNotifications();

    protected abstract void setHeadsUpUser(int i);

    protected abstract void toggleSplitScreenMode(int i, int i2);

    protected abstract void updateHeadsUp(String str, NotificationData.Entry entry, boolean z, boolean z2);

    protected abstract void updateNotificationRanking(NotificationListenerService.RankingMap rankingMap);

    protected abstract void updateNotifications();

    @Override
    public boolean isDeviceProvisioned() {
        if (!this.mDeviceProvisioned) {
            Log.d("StatusBar", "mDeviceProvisioned is false, so get DEVICE_PROVISIONED from db again !!");
            boolean provisioned = Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
            if (provisioned != this.mDeviceProvisioned) {
                Log.d("StatusBar", "mDeviceProvisioned is changed, re-call onchange!");
                this.mSettingsObserver.onChange(false);
            }
            return provisioned;
        }
        if ("eng".equalsIgnoreCase(Build.TYPE)) {
            Log.d("StatusBar", "mDeviceProvisioned is true");
        }
        return this.mDeviceProvisioned;
    }

    public boolean isDeviceInVrMode() {
        return this.mVrMode;
    }

    public void updateCurrentProfilesCache() {
        synchronized (this.mCurrentProfiles) {
            this.mCurrentProfiles.clear();
            if (this.mUserManager != null) {
                for (UserInfo user : this.mUserManager.getProfiles(this.mCurrentUserId)) {
                    this.mCurrentProfiles.put(user.id, user);
                }
            }
        }
    }

    @Override
    public void start() {
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mNotificationData = new NotificationData(this);
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        this.mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.checkService("dreams"));
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("device_provisioned"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("zen_mode"), false, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("lock_screen_show_notifications"), false, this.mSettingsObserver, -1);
        if (ENABLE_LOCK_SCREEN_ALLOW_REMOTE_INPUT) {
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("lock_screen_allow_remote_input"), false, this.mSettingsObserver, -1);
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("lock_screen_allow_private_notifications"), true, this.mLockscreenSettingsObserver, -1);
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        this.mRecents = (RecentsComponent) getComponent(Recents.class);
        Configuration currentConfig = this.mContext.getResources().getConfiguration();
        this.mLocale = currentConfig.locale;
        this.mLayoutDirection = TextUtils.getLayoutDirectionFromLocale(this.mLocale);
        this.mFontScale = currentConfig.fontScale;
        this.mDensity = currentConfig.densityDpi;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mCommandQueue = new CommandQueue(this);
        int[] switches = new int[9];
        ArrayList<IBinder> binders = new ArrayList<>();
        ArrayList<String> iconSlots = new ArrayList<>();
        ArrayList<StatusBarIcon> icons = new ArrayList<>();
        Rect fullscreenStackBounds = new Rect();
        Rect dockedStackBounds = new Rect();
        try {
            this.mBarService.registerStatusBar(this.mCommandQueue, iconSlots, icons, switches, binders, fullscreenStackBounds, dockedStackBounds);
        } catch (RemoteException e) {
        }
        createAndAddWindows();
        this.mSettingsObserver.onChange(false);
        disable(switches[0], switches[6], false);
        setSystemUiVisibility(switches[1], switches[7], switches[8], -1, fullscreenStackBounds, dockedStackBounds);
        topAppWindowChanged(switches[2] != 0);
        setImeWindowStatus(binders.get(0), switches[3], switches[4], switches[5] != 0);
        int N = iconSlots.size();
        for (int i = 0; i < N; i++) {
            setIcon(iconSlots.get(i), icons.get(i));
        }
        try {
            this.mNotificationListener.registerAsSystemService(this.mContext, new ComponentName(this.mContext.getPackageName(), getClass().getCanonicalName()), -1);
        } catch (RemoteException e2) {
            Log.e("StatusBar", "Unable to register notification listener", e2);
        }
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        setHeadsUpUser(this.mCurrentUserId);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_PRESENT");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        IntentFilter internalFilter = new IntentFilter();
        internalFilter.addAction("com.android.systemui.statusbar.work_challenge_unlocked_notification_action");
        internalFilter.addAction("com.android.systemui.statusbar.banner_action_cancel");
        internalFilter.addAction("com.android.systemui.statusbar.banner_action_setup");
        this.mContext.registerReceiver(this.mBroadcastReceiver, internalFilter, "com.android.systemui.permission.SELF", null);
        IntentFilter allUsersFilter = new IntentFilter();
        allUsersFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        this.mContext.registerReceiverAsUser(this.mAllUsersReceiver, UserHandle.ALL, allUsersFilter, null, null);
        updateCurrentProfilesCache();
        IVrManager vrManager = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
        try {
            vrManager.registerListener(this.mVrStateCallbacks);
        } catch (RemoteException e3) {
            Slog.e("StatusBar", "Failed to register VR mode state listener: " + e3);
        }
    }

    protected void notifyUserAboutHiddenNotifications() {
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), "show_note_about_notification_hiding", 1) != 0) {
            Log.d("StatusBar", "user hasn't seen notification about hidden notifications");
            if (!this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser())) {
                Log.d("StatusBar", "insecure lockscreen, skipping notification");
                Settings.Secure.putInt(this.mContext.getContentResolver(), "show_note_about_notification_hiding", 0);
                return;
            }
            Log.d("StatusBar", "disabling lockecreen notifications and alerting the user");
            Settings.Secure.putInt(this.mContext.getContentResolver(), "lock_screen_show_notifications", 0);
            Settings.Secure.putInt(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", 0);
            String packageName = this.mContext.getPackageName();
            PendingIntent cancelIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.statusbar.banner_action_cancel").setPackage(packageName), 268435456);
            PendingIntent setupIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.statusbar.banner_action_setup").setPackage(packageName), 268435456);
            Notification.Builder note = new Notification.Builder(this.mContext).setSmallIcon(com.android.systemui.R.drawable.ic_android).setContentTitle(this.mContext.getString(com.android.systemui.R.string.hidden_notifications_title)).setContentText(this.mContext.getString(com.android.systemui.R.string.hidden_notifications_text)).setPriority(1).setOngoing(true).setColor(this.mContext.getColor(R.color.system_accent3_600)).setContentIntent(setupIntent).addAction(com.android.systemui.R.drawable.ic_close, this.mContext.getString(com.android.systemui.R.string.hidden_notifications_cancel), cancelIntent).addAction(com.android.systemui.R.drawable.ic_settings, this.mContext.getString(com.android.systemui.R.string.hidden_notifications_setup), setupIntent);
            overrideNotificationAppName(this.mContext, note);
            NotificationManager noMan = (NotificationManager) this.mContext.getSystemService("notification");
            noMan.notify(com.android.systemui.R.id.notification_hidden, note.build());
        }
    }

    public void userSwitched(int newUserId) {
        setHeadsUpUser(newUserId);
    }

    @Override
    public boolean isNotificationForCurrentProfiles(StatusBarNotification n) {
        int i = this.mCurrentUserId;
        int notificationUserId = n.getUserId();
        return isCurrentProfile(notificationUserId);
    }

    protected void setNotificationShown(StatusBarNotification n) {
        setNotificationsShown(new String[]{n.getKey()});
    }

    protected void setNotificationsShown(String[] keys) {
        try {
            this.mNotificationListener.setNotificationsShown(keys);
        } catch (RuntimeException e) {
            Log.d("StatusBar", "failed setNotificationsShown: ", e);
        }
    }

    public boolean isCurrentProfile(int userId) {
        boolean z = true;
        synchronized (this.mCurrentProfiles) {
            if (userId != -1) {
                if (this.mCurrentProfiles.get(userId) == null) {
                    z = false;
                }
            }
        }
        return z;
    }

    @Override
    public String getCurrentMediaNotificationKey() {
        return null;
    }

    @Override
    public NotificationGroupManager getGroupManager() {
        return this.mGroupManager;
    }

    protected void dismissKeyguardThenExecute(KeyguardHostView.OnDismissAction action, boolean afterKeyguardGone) {
        action.onDismiss();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        Locale locale = this.mContext.getResources().getConfiguration().locale;
        int ld = TextUtils.getLayoutDirectionFromLocale(locale);
        float fontScale = newConfig.fontScale;
        int density = newConfig.densityDpi;
        if (density != this.mDensity || this.mFontScale != fontScale) {
            onDensityOrFontScaleChanged();
            this.mDensity = density;
            this.mFontScale = fontScale;
        }
        if (locale.equals(this.mLocale) && ld == this.mLayoutDirection) {
            return;
        }
        this.mLocale = locale;
        this.mLayoutDirection = ld;
        refreshLayout(ld);
    }

    protected void onDensityOrFontScaleChanged() {
        ArrayList<NotificationData.Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
        for (int i = 0; i < activeNotifications.size(); i++) {
            NotificationData.Entry entry = activeNotifications.get(i);
            boolean exposedGuts = entry.row.getGuts() == this.mNotificationGutsExposed;
            entry.row.reInflateViews();
            if (exposedGuts) {
                this.mNotificationGutsExposed = entry.row.getGuts();
                bindGuts(entry.row);
            }
            entry.cacheContentViews(this.mContext, null);
            inflateViews(entry, this.mStackScroller);
        }
    }

    protected void bindDismissListener(final ExpandableNotificationRow row) {
        row.setOnDismissListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.announceForAccessibility(BaseStatusBar.this.mContext.getString(com.android.systemui.R.string.accessibility_notification_dismissed));
                BaseStatusBar.this.performRemoveNotification(row.getStatusBarNotification(), false);
            }
        });
    }

    protected void performRemoveNotification(StatusBarNotification n, boolean removeView) {
        String pkg = n.getPackageName();
        String tag = n.getTag();
        int id = n.getId();
        int userId = n.getUserId();
        try {
            this.mBarService.onNotificationClear(pkg, tag, id, userId);
            if (FORCE_REMOTE_INPUT_HISTORY && this.mKeysKeptForRemoteInput.contains(n.getKey())) {
                this.mKeysKeptForRemoteInput.remove(n.getKey());
                removeView = true;
            }
            if (this.mRemoteInputEntriesToRemoveOnCollapse.remove(this.mNotificationData.get(n.getKey()))) {
                removeView = true;
            }
            if (!removeView) {
                return;
            }
            removeNotification(n.getKey(), null);
        } catch (RemoteException e) {
        }
    }

    protected void applyColorsAndBackgrounds(StatusBarNotification sbn, NotificationData.Entry entry) {
        if (entry.getContentView().getId() != 16909232 && entry.targetSdk >= 9 && entry.targetSdk < 21) {
            entry.row.setShowingLegacyBackground(true);
            entry.legacy = true;
        }
        if (entry.icon == null) {
            return;
        }
        entry.icon.setTag(com.android.systemui.R.id.icon_is_pre_L, Boolean.valueOf(entry.targetSdk < 21));
    }

    public boolean isMediaNotification(NotificationData.Entry entry) {
        return (entry.getExpandedContentView() == null || entry.getExpandedContentView().findViewById(R.id.language_picker_item) == null) ? false : true;
    }

    public void startAppNotificationSettingsActivity(String packageName, int appUid) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("app_package", packageName);
        intent.putExtra("app_uid", appUid);
        startNotificationGutsIntent(intent, appUid);
    }

    private void startNotificationGutsIntent(final Intent intent, final int appUid) {
        final boolean keyguardShowing = this.mStatusBarKeyguardViewManager.isShowing();
        dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() {
            @Override
            public boolean onDismiss() {
                final boolean z = keyguardShowing;
                final Intent intent2 = intent;
                final int i = appUid;
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (z) {
                                ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                            }
                            TaskStackBuilder.create(BaseStatusBar.this.mContext).addNextIntentWithParentStack(intent2).startActivities(BaseStatusBar.this.getActivityOptions(), new UserHandle(UserHandle.getUserId(i)));
                            BaseStatusBar.this.overrideActivityPendingAppTransition(z);
                        } catch (RemoteException e) {
                        }
                    }
                });
                BaseStatusBar.this.animateCollapsePanels(2, true);
                return true;
            }
        }, false);
    }

    public void bindGuts(final ExpandableNotificationRow row) {
        row.inflateGuts();
        final StatusBarNotification sbn = row.getStatusBarNotification();
        PackageManager pmUser = getPackageManagerForUser(this.mContext, sbn.getUser().getIdentifier());
        row.setTag(sbn.getPackageName());
        final NotificationGuts guts = row.getGuts();
        guts.setClosedListener(this);
        final String pkg = sbn.getPackageName();
        String appname = pkg;
        Drawable pkgicon = null;
        int appUid = -1;
        try {
            ApplicationInfo info = pmUser.getApplicationInfo(pkg, 8704);
            if (info != null) {
                appname = String.valueOf(pmUser.getApplicationLabel(info));
                pkgicon = pmUser.getApplicationIcon(info);
                appUid = info.uid;
            }
        } catch (PackageManager.NameNotFoundException e) {
            pkgicon = pmUser.getDefaultActivityIcon();
        }
        ((ImageView) guts.findViewById(com.android.systemui.R.id.app_icon)).setImageDrawable(pkgicon);
        ((TextView) guts.findViewById(com.android.systemui.R.id.pkgname)).setText(appname);
        TextView settingsButton = (TextView) guts.findViewById(com.android.systemui.R.id.more_settings);
        if (appUid >= 0) {
            final int appUidF = appUid;
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MetricsLogger.action(BaseStatusBar.this.mContext, 205);
                    guts.resetFalsingCheck();
                    BaseStatusBar.this.startAppNotificationSettingsActivity(pkg, appUidF);
                }
            });
            settingsButton.setText(com.android.systemui.R.string.notification_more_settings);
        } else {
            settingsButton.setVisibility(8);
        }
        guts.bindImportance(pmUser, sbn, this.mNotificationData.getImportance(sbn.getKey()));
        TextView doneButton = (TextView) guts.findViewById(com.android.systemui.R.id.done);
        doneButton.setText(com.android.systemui.R.string.notification_done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (guts.hasImportanceChanged() && BaseStatusBar.this.isLockscreenPublicMode() && (BaseStatusBar.this.mState == 1 || BaseStatusBar.this.mState == 2)) {
                    final StatusBarNotification statusBarNotification = sbn;
                    final ExpandableNotificationRow expandableNotificationRow = row;
                    final NotificationGuts notificationGuts = guts;
                    KeyguardHostView.OnDismissAction dismissAction = new KeyguardHostView.OnDismissAction() {
                        @Override
                        public boolean onDismiss() {
                            BaseStatusBar.this.saveImportanceCloseControls(statusBarNotification, expandableNotificationRow, notificationGuts, v);
                            return true;
                        }
                    };
                    BaseStatusBar.this.onLockedNotificationImportanceChange(dismissAction);
                    return;
                }
                BaseStatusBar.this.saveImportanceCloseControls(sbn, row, guts, v);
            }
        });
    }

    public void saveImportanceCloseControls(StatusBarNotification sbn, ExpandableNotificationRow row, NotificationGuts guts, View done) {
        guts.resetFalsingCheck();
        guts.saveImportance(sbn);
        int[] rowLocation = new int[2];
        int[] doneLocation = new int[2];
        row.getLocationOnScreen(rowLocation);
        done.getLocationOnScreen(doneLocation);
        int centerX = done.getWidth() / 2;
        int centerY = done.getHeight() / 2;
        int x = (doneLocation[0] - rowLocation[0]) + centerX;
        int y = (doneLocation[1] - rowLocation[1]) + centerY;
        dismissPopups(x, y);
    }

    protected SwipeHelper.LongPressListener getNotificationLongClicker() {
        return new SwipeHelper.LongPressListener() {
            @Override
            public boolean onLongPress(View v, final int x, final int y) {
                if (!(v instanceof ExpandableNotificationRow)) {
                    return false;
                }
                if (v.getWindowToken() == null) {
                    Log.e("StatusBar", "Trying to show notification guts, but not attached to window");
                    return false;
                }
                final ExpandableNotificationRow row = (ExpandableNotificationRow) v;
                BaseStatusBar.this.bindGuts(row);
                final NotificationGuts guts = row.getGuts();
                if (guts == null) {
                    return false;
                }
                if (guts.getVisibility() == 0) {
                    BaseStatusBar.this.dismissPopups(x, y);
                    return false;
                }
                MetricsLogger.action(BaseStatusBar.this.mContext, 204);
                guts.setVisibility(4);
                guts.post(new Runnable() {
                    @Override
                    public void run() {
                        BaseStatusBar.this.dismissPopups(-1, -1, false, false);
                        guts.setVisibility(0);
                        double horz = Math.max(guts.getWidth() - x, x);
                        double vert = Math.max(guts.getHeight() - y, y);
                        float r = (float) Math.hypot(horz, vert);
                        Animator a = ViewAnimationUtils.createCircularReveal(guts, x, y, 0.0f, r);
                        a.setDuration(360L);
                        a.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
                        final ExpandableNotificationRow expandableNotificationRow = row;
                        a.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                expandableNotificationRow.resetTranslation();
                            }
                        });
                        a.start();
                        guts.setExposed(true, BaseStatusBar.this.mState == 1);
                        row.closeRemoteInput();
                        BaseStatusBar.this.mStackScroller.onHeightChanged(null, true);
                        BaseStatusBar.this.mNotificationGutsExposed = guts;
                    }
                });
                return true;
            }
        };
    }

    public NotificationGuts getExposedGuts() {
        return this.mNotificationGutsExposed;
    }

    public void dismissPopups() {
        dismissPopups(-1, -1, true, false);
    }

    public void dismissPopups(int x, int y) {
        dismissPopups(x, y, true, false);
    }

    public void dismissPopups(int x, int y, boolean resetGear, boolean animate) {
        if (this.mNotificationGutsExposed != null) {
            this.mNotificationGutsExposed.closeControls(x, y, true);
        }
        if (!resetGear) {
            return;
        }
        this.mStackScroller.resetExposedGearView(animate, true);
    }

    @Override
    public void onGutsClosed(NotificationGuts guts) {
        this.mStackScroller.onHeightChanged(null, true);
        this.mNotificationGutsExposed = null;
    }

    @Override
    public void showRecentApps(boolean triggeredFromAltTab, boolean fromHome) {
        this.mHandler.removeMessages(1019);
        this.mHandler.obtainMessage(1019, triggeredFromAltTab ? 1 : 0, fromHome ? 1 : 0).sendToTarget();
    }

    @Override
    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        this.mHandler.removeMessages(1020);
        this.mHandler.obtainMessage(1020, triggeredFromAltTab ? 1 : 0, triggeredFromHomeKey ? 1 : 0).sendToTarget();
    }

    @Override
    public void toggleRecentApps() {
        toggleRecents();
    }

    @Override
    public void toggleSplitScreen() {
        toggleSplitScreenMode(-1, -1);
    }

    @Override
    public void preloadRecentApps() {
        this.mHandler.removeMessages(1022);
        this.mHandler.sendEmptyMessage(1022);
    }

    @Override
    public void cancelPreloadRecentApps() {
        this.mHandler.removeMessages(1023);
        this.mHandler.sendEmptyMessage(1023);
    }

    @Override
    public void dismissKeyboardShortcutsMenu() {
        this.mHandler.removeMessages(1027);
        this.mHandler.sendEmptyMessage(1027);
    }

    @Override
    public void toggleKeyboardShortcutsMenu(int deviceId) {
        this.mHandler.removeMessages(1026);
        this.mHandler.obtainMessage(1026, deviceId, 0).sendToTarget();
    }

    protected H createHandler() {
        return new H();
    }

    protected void sendCloseSystemWindows(String reason) {
        if (!ActivityManagerNative.isSystemReady()) {
            return;
        }
        try {
            ActivityManagerNative.getDefault().closeSystemDialogs(reason);
        } catch (RemoteException e) {
        }
    }

    protected void showRecents(boolean triggeredFromAltTab, boolean fromHome) {
        if (this.mRecents == null) {
            return;
        }
        sendCloseSystemWindows("recentapps");
        this.mRecents.showRecents(triggeredFromAltTab, fromHome);
    }

    protected void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        if (this.mRecents == null) {
            return;
        }
        this.mRecents.hideRecents(triggeredFromAltTab, triggeredFromHomeKey);
    }

    protected void toggleRecents() {
        if (this.mRecents == null) {
            return;
        }
        this.mRecents.toggleRecents(this.mDisplay);
    }

    protected void preloadRecents() {
        if (this.mRecents == null) {
            return;
        }
        this.mRecents.preloadRecents();
    }

    protected void toggleKeyboardShortcuts(int deviceId) {
        KeyboardShortcuts.toggle(this.mContext, deviceId);
    }

    protected void dismissKeyboardShortcuts() {
        KeyboardShortcuts.dismiss();
    }

    protected void cancelPreloadingRecents() {
        if (this.mRecents == null) {
            return;
        }
        this.mRecents.cancelPreloadingRecents();
    }

    protected void showRecentsNextAffiliatedTask() {
        if (this.mRecents == null) {
            return;
        }
        this.mRecents.showNextAffiliatedTask();
    }

    protected void showRecentsPreviousAffiliatedTask() {
        if (this.mRecents == null) {
            return;
        }
        this.mRecents.showPrevAffiliatedTask();
    }

    public void setLockscreenPublicMode(boolean publicMode) {
        this.mLockscreenPublicMode = publicMode;
    }

    public boolean isLockscreenPublicMode() {
        return this.mLockscreenPublicMode;
    }

    protected void onWorkChallengeUnlocked() {
    }

    public boolean userAllowsNotificationsInPublic(int userHandle) {
        if (userHandle == -1) {
            return true;
        }
        if (this.mUsersAllowingNotifications.indexOfKey(userHandle) < 0) {
            boolean allowed = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 0, userHandle) != 0;
            this.mUsersAllowingNotifications.append(userHandle, allowed);
            return allowed;
        }
        return this.mUsersAllowingNotifications.get(userHandle);
    }

    public boolean userAllowsPrivateNotificationsInPublic(int userHandle) {
        if (userHandle == -1) {
            return true;
        }
        if (this.mUsersAllowingPrivateNotifications.indexOfKey(userHandle) < 0) {
            boolean allowedByUser = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", 0, userHandle) != 0;
            boolean allowedByDpm = adminAllowsUnredactedNotifications(userHandle);
            boolean z = allowedByUser ? allowedByDpm : false;
            this.mUsersAllowingPrivateNotifications.append(userHandle, z);
            return z;
        }
        return this.mUsersAllowingPrivateNotifications.get(userHandle);
    }

    private boolean adminAllowsUnredactedNotifications(int userHandle) {
        if (userHandle == -1) {
            return true;
        }
        int dpmFlags = this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, userHandle);
        return (dpmFlags & 8) == 0;
    }

    @Override
    public boolean shouldHideNotifications(int userid) {
        return isLockscreenPublicMode() && !userAllowsNotificationsInPublic(userid);
    }

    @Override
    public boolean shouldHideNotifications(String key) {
        return isLockscreenPublicMode() && this.mNotificationData.getVisibilityOverride(key) == -1;
    }

    @Override
    public boolean onSecureLockScreen() {
        return isLockscreenPublicMode();
    }

    public void onPanelLaidOut() {
        if (this.mState != 1) {
            return;
        }
        int maxBefore = getMaxKeyguardNotifications(false);
        int maxNotifications = getMaxKeyguardNotifications(true);
        if (maxBefore == maxNotifications) {
            return;
        }
        updateRowStates();
    }

    protected void onLockedNotificationImportanceChange(KeyguardHostView.OnDismissAction dismissAction) {
    }

    protected void onLockedRemoteInput(ExpandableNotificationRow row, View clickedView) {
    }

    protected void onLockedWorkRemoteInput(int userId, ExpandableNotificationRow row, View clicked) {
    }

    @Override
    public void onExpandClicked(NotificationData.Entry clickedEntry, boolean nowExpanded) {
    }

    public class H extends Handler {
        protected H() {
        }

        @Override
        public void handleMessage(Message m) {
            switch (m.what) {
                case 1019:
                    BaseStatusBar.this.showRecents(m.arg1 > 0, m.arg2 != 0);
                    break;
                case 1020:
                    BaseStatusBar.this.hideRecents(m.arg1 > 0, m.arg2 > 0);
                    break;
                case 1021:
                    BaseStatusBar.this.toggleRecents();
                    break;
                case 1022:
                    BaseStatusBar.this.preloadRecents();
                    break;
                case 1023:
                    BaseStatusBar.this.cancelPreloadingRecents();
                    break;
                case 1024:
                    BaseStatusBar.this.showRecentsNextAffiliatedTask();
                    break;
                case 1025:
                    BaseStatusBar.this.showRecentsPreviousAffiliatedTask();
                    break;
                case 1026:
                    BaseStatusBar.this.toggleKeyboardShortcuts(m.arg1);
                    break;
                case 1027:
                    BaseStatusBar.this.dismissKeyboardShortcuts();
                    break;
            }
        }
    }

    protected void workAroundBadLayerDrawableOpacity(View v) {
    }

    protected boolean inflateViews(NotificationData.Entry entry, ViewGroup parent) {
        ExpandableNotificationRow row;
        PackageManager pmUser = getPackageManagerForUser(this.mContext, entry.notification.getUser().getIdentifier());
        StatusBarNotification sbn = entry.notification;
        entry.cacheContentViews(this.mContext, null);
        RemoteViews contentView = entry.cachedContentView;
        RemoteViews bigContentView = entry.cachedBigContentView;
        RemoteViews headsUpContentView = entry.cachedHeadsUpContentView;
        RemoteViews publicContentView = entry.cachedPublicContentView;
        if (contentView == null) {
            Log.v("StatusBar", "no contentView for: " + sbn.getNotification());
            return false;
        }
        boolean hasUserChangedExpansion = false;
        boolean userExpanded = false;
        boolean userLocked = false;
        if (entry.row != null) {
            row = entry.row;
            hasUserChangedExpansion = row.hasUserChangedExpansion();
            userExpanded = row.isUserExpanded();
            userLocked = row.isUserLocked();
            entry.reset();
            if (hasUserChangedExpansion) {
                row.setUserExpanded(userExpanded);
            }
        } else {
            LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
            row = (ExpandableNotificationRow) inflater.inflate(com.android.systemui.R.layout.status_bar_notification_row, parent, false);
            row.setExpansionLogger(this, entry.notification.getKey());
            row.setGroupManager(this.mGroupManager);
            row.setHeadsUpManager(this.mHeadsUpManager);
            row.setRemoteInputController(this.mRemoteInputController);
            row.setOnExpandClickListener(this);
            String pkg = sbn.getPackageName();
            String appname = pkg;
            try {
                ApplicationInfo info = pmUser.getApplicationInfo(pkg, 8704);
                if (info != null) {
                    appname = String.valueOf(pmUser.getApplicationLabel(info));
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
            row.setAppName(appname);
        }
        workAroundBadLayerDrawableOpacity(row);
        bindDismissListener(row);
        NotificationContentView contentContainer = row.getPrivateLayout();
        NotificationContentView contentContainerPublic = row.getPublicLayout();
        row.setLayoutDirection(3);
        row.setDescendantFocusability(393216);
        if (ENABLE_REMOTE_INPUT) {
            row.setDescendantFocusability(131072);
        }
        this.mNotificationClicker.register(row, sbn);
        View bigContentViewLocal = null;
        View headsUpContentViewLocal = null;
        View publicViewLocal = null;
        try {
            View contentViewLocal = contentView.apply(sbn.getPackageContext(this.mContext), contentContainer, this.mOnClickHandler);
            if (bigContentView != null) {
                bigContentViewLocal = bigContentView.apply(sbn.getPackageContext(this.mContext), contentContainer, this.mOnClickHandler);
            }
            if (headsUpContentView != null) {
                headsUpContentViewLocal = headsUpContentView.apply(sbn.getPackageContext(this.mContext), contentContainer, this.mOnClickHandler);
            }
            if (publicContentView != null) {
                publicViewLocal = publicContentView.apply(sbn.getPackageContext(this.mContext), contentContainerPublic, this.mOnClickHandler);
            }
            if (contentViewLocal != null) {
                contentViewLocal.setIsRootNamespace(true);
                contentContainer.setContractedChild(contentViewLocal);
            }
            if (bigContentViewLocal != null) {
                bigContentViewLocal.setIsRootNamespace(true);
                contentContainer.setExpandedChild(bigContentViewLocal);
            }
            if (headsUpContentViewLocal != null) {
                headsUpContentViewLocal.setIsRootNamespace(true);
                contentContainer.setHeadsUpChild(headsUpContentViewLocal);
            }
            if (publicViewLocal != null) {
                publicViewLocal.setIsRootNamespace(true);
                contentContainerPublic.setContractedChild(publicViewLocal);
            }
            try {
                entry.targetSdk = pmUser.getApplicationInfo(sbn.getPackageName(), 0).targetSdkVersion;
            } catch (PackageManager.NameNotFoundException ex) {
                Log.e("StatusBar", "Failed looking up ApplicationInfo for " + sbn.getPackageName(), ex);
            }
            entry.autoRedacted = entry.notification.getNotification().publicVersion == null;
            entry.row = row;
            entry.row.setOnActivatedListener(this);
            entry.row.setExpandable(bigContentViewLocal != null);
            applyColorsAndBackgrounds(sbn, entry);
            if (hasUserChangedExpansion) {
                row.setUserExpanded(userExpanded);
            }
            row.setUserLocked(userLocked);
            row.onNotificationUpdated(entry);
            return true;
        } catch (RuntimeException e2) {
            String ident = sbn.getPackageName() + "/0x" + Integer.toHexString(sbn.getId());
            Log.e("StatusBar", "couldn't inflate view for notification " + ident, e2);
            return false;
        }
    }

    public void processForRemoteInput(Notification n) {
        RemoteInput[] remoteInputs;
        if (!ENABLE_REMOTE_INPUT || n.extras == null || !n.extras.containsKey("android.wearable.EXTENSIONS")) {
            return;
        }
        if (n.actions != null && n.actions.length != 0) {
            return;
        }
        Notification.Action viableAction = null;
        Notification.WearableExtender we = new Notification.WearableExtender(n);
        List<Notification.Action> actions = we.getActions();
        int numActions = actions.size();
        for (int i = 0; i < numActions; i++) {
            Notification.Action action = actions.get(i);
            if (action != null && (remoteInputs = action.getRemoteInputs()) != null) {
                int length = remoteInputs.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length) {
                        break;
                    }
                    RemoteInput ri = remoteInputs[i2];
                    if (!ri.getAllowFreeFormInput()) {
                        i2++;
                    } else {
                        viableAction = action;
                        break;
                    }
                }
                if (viableAction != null) {
                    break;
                }
            }
        }
        if (viableAction == null) {
            return;
        }
        Notification.Builder rebuilder = Notification.Builder.recoverBuilder(this.mContext, n);
        rebuilder.setActions(viableAction);
        rebuilder.build();
    }

    public void startPendingIntentDismissingKeyguard(final PendingIntent intent) {
        final boolean zWouldLaunchResolverActivity;
        if (isDeviceProvisioned()) {
            final boolean keyguardShowing = this.mStatusBarKeyguardViewManager.isShowing();
            if (!intent.isActivity()) {
                zWouldLaunchResolverActivity = false;
            } else {
                zWouldLaunchResolverActivity = PreviewInflater.wouldLaunchResolverActivity(this.mContext, intent.getIntent(), this.mCurrentUserId);
            }
            dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() {
                @Override
                public boolean onDismiss() {
                    final boolean z = keyguardShowing;
                    final boolean z2 = zWouldLaunchResolverActivity;
                    final PendingIntent pendingIntent = intent;
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                if (z && !z2) {
                                    ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                                }
                                ActivityManagerNative.getDefault().resumeAppSwitches();
                            } catch (RemoteException e) {
                            }
                            try {
                                pendingIntent.send(null, 0, null, null, null, null, BaseStatusBar.this.getActivityOptions());
                            } catch (PendingIntent.CanceledException e2) {
                                Log.w("StatusBar", "Sending intent failed: " + e2);
                            }
                            if (!pendingIntent.isActivity()) {
                                return;
                            }
                            BaseStatusBar.this.mAssistManager.hideAssist();
                            BaseStatusBar baseStatusBar = BaseStatusBar.this;
                            boolean z3 = z && !z2;
                            baseStatusBar.overrideActivityPendingAppTransition(z3);
                        }
                    }.start();
                    BaseStatusBar.this.animateCollapsePanels(2, true, true);
                    BaseStatusBar.this.visibilityChanged(false);
                    return true;
                }
            }, zWouldLaunchResolverActivity);
        }
    }

    public void addPostCollapseAction(Runnable r) {
    }

    public boolean isCollapsing() {
        return false;
    }

    private final class NotificationClicker implements View.OnClickListener {
        NotificationClicker(BaseStatusBar this$0, NotificationClicker notificationClicker) {
            this();
        }

        private NotificationClicker() {
        }

        @Override
        public void onClick(View v) {
            PendingIntent intent;
            boolean zWouldLaunchResolverActivity;
            if (!(v instanceof ExpandableNotificationRow)) {
                Log.e("StatusBar", "NotificationClicker called on a view that is not a notification row.");
                return;
            }
            final ExpandableNotificationRow row = (ExpandableNotificationRow) v;
            StatusBarNotification sbn = row.getStatusBarNotification();
            if (sbn == null) {
                Log.e("StatusBar", "NotificationClicker called on an unclickable notification,");
                return;
            }
            if (row.getSettingsRow() != null && row.getSettingsRow().isVisible()) {
                row.animateTranslateNotification(0.0f);
                return;
            }
            Notification notification = sbn.getNotification();
            if (notification.contentIntent != null) {
                intent = notification.contentIntent;
            } else {
                intent = notification.fullScreenIntent;
            }
            String notificationKey = sbn.getKey();
            row.setJustClicked(true);
            DejankUtils.postAfterTraversal(new Runnable() {
                @Override
                public void run() {
                    row.setJustClicked(false);
                }
            });
            boolean keyguardShowing = BaseStatusBar.this.mStatusBarKeyguardViewManager.isShowing();
            if (!intent.isActivity()) {
                zWouldLaunchResolverActivity = false;
            } else {
                zWouldLaunchResolverActivity = PreviewInflater.wouldLaunchResolverActivity(BaseStatusBar.this.mContext, intent.getIntent(), BaseStatusBar.this.mCurrentUserId);
            }
            BaseStatusBar.this.dismissKeyguardThenExecute(new AnonymousClass2(notificationKey, row, sbn, keyguardShowing, zWouldLaunchResolverActivity, intent), zWouldLaunchResolverActivity);
        }

        class AnonymousClass2 implements KeyguardHostView.OnDismissAction {
            final boolean val$afterKeyguardGone;
            final PendingIntent val$intent;
            final boolean val$keyguardShowing;
            final String val$notificationKey;
            final ExpandableNotificationRow val$row;
            final StatusBarNotification val$sbn;

            AnonymousClass2(String val$notificationKey, ExpandableNotificationRow val$row, StatusBarNotification val$sbn, boolean val$keyguardShowing, boolean val$afterKeyguardGone, PendingIntent val$intent) {
                this.val$notificationKey = val$notificationKey;
                this.val$row = val$row;
                this.val$sbn = val$sbn;
                this.val$keyguardShowing = val$keyguardShowing;
                this.val$afterKeyguardGone = val$afterKeyguardGone;
                this.val$intent = val$intent;
            }

            @Override
            public boolean onDismiss() {
                if (BaseStatusBar.this.mHeadsUpManager != null && BaseStatusBar.this.mHeadsUpManager.isHeadsUp(this.val$notificationKey)) {
                    if (BaseStatusBar.this.isPanelFullyCollapsed()) {
                        HeadsUpManager.setIsClickedNotification(this.val$row, true);
                    }
                    BaseStatusBar.this.mHeadsUpManager.releaseImmediately(this.val$notificationKey);
                }
                StatusBarNotification parentToCancel = null;
                if (NotificationClicker.this.shouldAutoCancel(this.val$sbn) && BaseStatusBar.this.mGroupManager.isOnlyChildInGroup(this.val$sbn)) {
                    StatusBarNotification summarySbn = BaseStatusBar.this.mGroupManager.getLogicalGroupSummary(this.val$sbn).getStatusBarNotification();
                    if (NotificationClicker.this.shouldAutoCancel(summarySbn)) {
                        parentToCancel = summarySbn;
                    }
                }
                StatusBarNotification parentToCancelFinal = parentToCancel;
                new AnonymousClass1(this.val$keyguardShowing, this.val$afterKeyguardGone, this.val$intent, this.val$notificationKey, parentToCancelFinal).start();
                BaseStatusBar.this.animateCollapsePanels(2, true, true);
                BaseStatusBar.this.visibilityChanged(false);
                return true;
            }

            class AnonymousClass1 extends Thread {
                final boolean val$afterKeyguardGone;
                final PendingIntent val$intent;
                final boolean val$keyguardShowing;
                final String val$notificationKey;
                final StatusBarNotification val$parentToCancelFinal;

                AnonymousClass1(boolean val$keyguardShowing, boolean val$afterKeyguardGone, PendingIntent val$intent, String val$notificationKey, StatusBarNotification val$parentToCancelFinal) {
                    this.val$keyguardShowing = val$keyguardShowing;
                    this.val$afterKeyguardGone = val$afterKeyguardGone;
                    this.val$intent = val$intent;
                    this.val$notificationKey = val$notificationKey;
                    this.val$parentToCancelFinal = val$parentToCancelFinal;
                }

                @Override
                public void run() {
                    try {
                        if (this.val$keyguardShowing && !this.val$afterKeyguardGone) {
                            ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                        }
                        ActivityManagerNative.getDefault().resumeAppSwitches();
                    } catch (RemoteException e) {
                    }
                    if (this.val$intent != null) {
                        if (this.val$intent.isActivity()) {
                            int userId = this.val$intent.getCreatorUserHandle().getIdentifier();
                            if (BaseStatusBar.this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId) && BaseStatusBar.this.mKeyguardManager.isDeviceLocked(userId) && BaseStatusBar.this.startWorkChallengeIfNecessary(userId, this.val$intent.getIntentSender(), this.val$notificationKey)) {
                                return;
                            }
                        }
                        try {
                            this.val$intent.send(null, 0, null, null, null, null, BaseStatusBar.this.getActivityOptions());
                        } catch (PendingIntent.CanceledException e2) {
                            Log.w("StatusBar", "Sending contentIntent failed: " + e2);
                        }
                        if (this.val$intent.isActivity()) {
                            BaseStatusBar.this.mAssistManager.hideAssist();
                            BaseStatusBar baseStatusBar = BaseStatusBar.this;
                            boolean z = this.val$keyguardShowing && !this.val$afterKeyguardGone;
                            baseStatusBar.overrideActivityPendingAppTransition(z);
                        }
                    }
                    try {
                        BaseStatusBar.this.mBarService.onNotificationClick(this.val$notificationKey);
                    } catch (RemoteException e3) {
                    }
                    if (this.val$parentToCancelFinal == null) {
                        return;
                    }
                    H h = BaseStatusBar.this.mHandler;
                    final StatusBarNotification statusBarNotification = this.val$parentToCancelFinal;
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            final StatusBarNotification statusBarNotification2 = statusBarNotification;
                            Runnable removeRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    BaseStatusBar.this.performRemoveNotification(statusBarNotification2, true);
                                }
                            };
                            if (BaseStatusBar.this.isCollapsing()) {
                                BaseStatusBar.this.addPostCollapseAction(removeRunnable);
                            } else {
                                removeRunnable.run();
                            }
                        }
                    });
                }
            }
        }

        public boolean shouldAutoCancel(StatusBarNotification sbn) {
            int flags = sbn.getNotification().flags;
            return (flags & 16) == 16 && (flags & 64) == 0;
        }

        public void register(ExpandableNotificationRow row, StatusBarNotification sbn) {
            Notification notification = sbn.getNotification();
            if (notification.contentIntent != null || notification.fullScreenIntent != null) {
                row.setOnClickListener(this);
            } else {
                row.setOnClickListener(null);
            }
        }
    }

    public void animateCollapsePanels(int flags, boolean force) {
    }

    public void animateCollapsePanels(int flags, boolean force, boolean delayed) {
    }

    public void overrideActivityPendingAppTransition(boolean keyguardShowing) {
        if (!keyguardShowing) {
            return;
        }
        try {
            this.mWindowManagerService.overridePendingAppTransition((String) null, 0, 0, (IRemoteCallback) null);
        } catch (RemoteException e) {
            Log.w("StatusBar", "Error overriding app transition: " + e);
        }
    }

    protected boolean startWorkChallengeIfNecessary(int userId, IntentSender intendSender, String notificationKey) {
        Intent newIntent = this.mKeyguardManager.createConfirmDeviceCredentialIntent(null, null, userId);
        if (newIntent == null) {
            return false;
        }
        Intent callBackIntent = new Intent("com.android.systemui.statusbar.work_challenge_unlocked_notification_action");
        callBackIntent.putExtra("android.intent.extra.INTENT", intendSender);
        callBackIntent.putExtra("android.intent.extra.INDEX", notificationKey);
        callBackIntent.setPackage(this.mContext.getPackageName());
        PendingIntent callBackPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, callBackIntent, 1409286144);
        newIntent.putExtra("android.intent.extra.INTENT", callBackPendingIntent.getIntentSender());
        try {
            ActivityManagerNative.getDefault().startConfirmDeviceCredentialIntent(newIntent);
            return true;
        } catch (RemoteException e) {
            return true;
        }
    }

    public Bundle getActivityOptions() {
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchStackId(1);
        return options.toBundle();
    }

    protected void visibilityChanged(boolean visible) {
        if (this.mVisible != visible) {
            this.mVisible = visible;
            if (!visible) {
                dismissPopups();
            }
        }
        updateVisibleToUser();
    }

    protected void updateVisibleToUser() {
        boolean oldVisibleToUser = this.mVisibleToUser;
        this.mVisibleToUser = this.mVisible ? this.mDeviceInteractive : false;
        if (oldVisibleToUser == this.mVisibleToUser) {
            return;
        }
        handleVisibleToUserChanged(this.mVisibleToUser);
    }

    protected void handleVisibleToUserChanged(boolean visibleToUser) {
        boolean clearNotificationEffects;
        try {
            if (visibleToUser) {
                boolean pinnedHeadsUp = this.mHeadsUpManager.hasPinnedHeadsUp();
                if (isPanelFullyCollapsed()) {
                    clearNotificationEffects = false;
                } else {
                    clearNotificationEffects = this.mState == 0 || this.mState == 2;
                }
                int notificationLoad = this.mNotificationData.getActiveNotifications().size();
                if (pinnedHeadsUp && isPanelFullyCollapsed()) {
                    notificationLoad = 1;
                } else {
                    MetricsLogger.histogram(this.mContext, "note_load", notificationLoad);
                }
                this.mBarService.onPanelRevealed(clearNotificationEffects, notificationLoad);
                return;
            }
            this.mBarService.onPanelHidden();
        } catch (RemoteException e) {
        }
    }

    public void clearNotificationEffects() {
        try {
            this.mBarService.clearNotificationEffects();
        } catch (RemoteException e) {
        }
    }

    void handleNotificationError(StatusBarNotification n, String message) {
        removeNotification(n.getKey(), null);
        try {
            this.mBarService.onNotificationError(n.getPackageName(), n.getTag(), n.getId(), n.getUid(), n.getInitialPid(), message, n.getUserId());
        } catch (RemoteException e) {
        }
    }

    protected StatusBarNotification removeNotificationViews(String key, NotificationListenerService.RankingMap ranking) {
        NotificationData.Entry entry = this.mNotificationData.remove(key, ranking);
        if (entry == null) {
            Log.w("StatusBar", "removeNotification for unknown key: " + key);
            return null;
        }
        updateNotifications();
        return entry.notification;
    }

    protected NotificationData.Entry createNotificationViews(StatusBarNotification sbn) {
        StatusBarIconView iconView = createIcon(sbn);
        if (iconView == null) {
            return null;
        }
        NotificationData.Entry entry = new NotificationData.Entry(sbn, iconView);
        if (!inflateViews(entry, this.mStackScroller)) {
            handleNotificationError(sbn, "Couldn't expand RemoteViews for: " + sbn);
            return null;
        }
        return entry;
    }

    public StatusBarIconView createIcon(StatusBarNotification sbn) {
        Notification n = sbn.getNotification();
        StatusBarIconView iconView = new StatusBarIconView(this.mContext, sbn.getPackageName() + "/0x" + Integer.toHexString(sbn.getId()), n);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        Icon smallIcon = n.getSmallIcon();
        if (smallIcon == null) {
            handleNotificationError(sbn, "No small icon in notification from " + sbn.getPackageName());
            return null;
        }
        StatusBarIcon ic = new StatusBarIcon(sbn.getUser(), sbn.getPackageName(), smallIcon, n.iconLevel, n.number, StatusBarIconView.contentDescForNotification(this.mContext, n));
        if (!iconView.set(ic)) {
            handleNotificationError(sbn, "Couldn't create icon: " + ic);
            return null;
        }
        return iconView;
    }

    protected void addNotificationViews(NotificationData.Entry entry, NotificationListenerService.RankingMap ranking) {
        if (entry == null) {
            return;
        }
        this.mNotificationData.add(entry, ranking);
        updateNotifications();
    }

    protected void updateRowStates() {
        this.mKeyguardIconOverflowContainer.getIconsView().removeAllViews();
        ArrayList<NotificationData.Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
        int N = activeNotifications.size();
        int visibleNotifications = 0;
        boolean onKeyguard = this.mState == 1;
        int maxNotifications = 0;
        if (onKeyguard) {
            maxNotifications = getMaxKeyguardNotifications(true);
        }
        for (int i = 0; i < N; i++) {
            NotificationData.Entry entry = activeNotifications.get(i);
            boolean childNotification = this.mGroupManager.isChildInGroupWithSummary(entry.notification);
            if (onKeyguard) {
                entry.row.setOnKeyguard(true);
            } else {
                entry.row.setOnKeyguard(false);
                entry.row.setSystemExpanded(visibleNotifications == 0 && !childNotification);
            }
            boolean suppressedSummary = this.mGroupManager.isSummaryOfSuppressedGroup(entry.notification) && !entry.row.isRemoved();
            boolean childWithVisibleSummary = childNotification && this.mGroupManager.getGroupSummary(entry.notification).getVisibility() == 0;
            boolean showOnKeyguard = shouldShowOnKeyguard(entry.notification);
            if (suppressedSummary || ((isLockscreenPublicMode() && !this.mShowLockscreenNotifications) || (onKeyguard && !childWithVisibleSummary && (visibleNotifications >= maxNotifications || !showOnKeyguard)))) {
                entry.row.setVisibility(8);
                if (onKeyguard && showOnKeyguard && !childNotification && !suppressedSummary) {
                    this.mKeyguardIconOverflowContainer.getIconsView().addNotification(entry);
                }
            } else {
                boolean wasGone = entry.row.getVisibility() == 8;
                entry.row.setVisibility(0);
                if (!childNotification && !entry.row.isRemoved()) {
                    if (wasGone) {
                        this.mStackScroller.generateAddAnimation(entry.row, !showOnKeyguard);
                    }
                    visibleNotifications++;
                }
            }
        }
        NotificationStackScrollLayout notificationStackScrollLayout = this.mStackScroller;
        boolean z = onKeyguard && this.mKeyguardIconOverflowContainer.getIconsView().getChildCount() > 0;
        notificationStackScrollLayout.updateOverflowContainerVisibility(z);
        this.mStackScroller.changeViewPosition(this.mDismissView, this.mStackScroller.getChildCount() - 1);
        this.mStackScroller.changeViewPosition(this.mEmptyShadeView, this.mStackScroller.getChildCount() - 2);
        this.mStackScroller.changeViewPosition(this.mKeyguardIconOverflowContainer, this.mStackScroller.getChildCount() - 3);
    }

    public boolean shouldShowOnKeyguard(StatusBarNotification sbn) {
        return this.mShowLockscreenNotifications && !this.mNotificationData.isAmbient(sbn.getKey());
    }

    protected void setZenMode(int mode) {
        if (isDeviceProvisioned()) {
            this.mZenMode = mode;
            updateNotifications();
        }
    }

    protected void setShowLockscreenNotifications(boolean show) {
        this.mShowLockscreenNotifications = show;
    }

    protected void setLockScreenAllowRemoteInput(boolean allowLockscreenRemoteInput) {
        this.mAllowLockscreenRemoteInput = allowLockscreenRemoteInput;
    }

    public void updateLockscreenNotificationSetting() {
        boolean show = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 1, this.mCurrentUserId) != 0;
        this.mUsersAllowingNotifications.put(this.mCurrentUserId, show);
        int dpmFlags = this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mCurrentUserId);
        boolean allowedByDpm = (dpmFlags & 4) == 0;
        if (!show) {
            allowedByDpm = false;
        }
        setShowLockscreenNotifications(allowedByDpm);
        if (ENABLE_LOCK_SCREEN_ALLOW_REMOTE_INPUT) {
            boolean remoteInput = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_allow_remote_input", 0, this.mCurrentUserId) != 0;
            boolean remoteInputDpm = (dpmFlags & 64) == 0;
            if (!remoteInput) {
                remoteInputDpm = false;
            }
            setLockScreenAllowRemoteInput(remoteInputDpm);
            return;
        }
        setLockScreenAllowRemoteInput(false);
    }

    public void updateNotification(StatusBarNotification notification, NotificationListenerService.RankingMap ranking) {
        Log.d("StatusBar", "updateNotification(" + notification + ")");
        String key = notification.getKey();
        NotificationData.Entry entry = this.mNotificationData.get(key);
        if (entry == null) {
            return;
        }
        this.mHeadsUpEntriesToRemoveOnSwitch.remove(entry);
        this.mRemoteInputEntriesToRemoveOnCollapse.remove(entry);
        Notification n = notification.getNotification();
        this.mNotificationData.updateRanking(ranking);
        boolean applyInPlace = entry.cacheContentViews(this.mContext, notification.getNotification());
        boolean shouldPeek = shouldPeek(entry, notification);
        boolean alertAgain = alertAgain(entry, n);
        StatusBarNotification oldNotification = entry.notification;
        entry.notification = notification;
        this.mGroupManager.onEntryUpdated(entry, oldNotification);
        boolean updateSuccessful = false;
        if (applyInPlace) {
            try {
                if (entry.icon != null) {
                    StatusBarIcon ic = new StatusBarIcon(notification.getUser(), notification.getPackageName(), n.getSmallIcon(), n.iconLevel, n.number, StatusBarIconView.contentDescForNotification(this.mContext, n));
                    entry.icon.setNotification(n);
                    if (!entry.icon.set(ic)) {
                        handleNotificationError(notification, "Couldn't update icon: " + ic);
                        return;
                    }
                }
                updateNotificationViews(entry, notification);
                updateSuccessful = true;
            } catch (RuntimeException e) {
                Log.w("StatusBar", "Couldn't reapply views for package " + notification.getPackageName(), e);
            }
        }
        if (!updateSuccessful) {
            StatusBarIcon ic2 = new StatusBarIcon(notification.getUser(), notification.getPackageName(), n.getSmallIcon(), n.iconLevel, n.number, StatusBarIconView.contentDescForNotification(this.mContext, n));
            entry.icon.setNotification(n);
            entry.icon.set(ic2);
            inflateViews(entry, this.mStackScroller);
        }
        updateHeadsUp(key, entry, shouldPeek, alertAgain);
        updateNotifications();
        if (!notification.isClearable()) {
            this.mStackScroller.snapViewIfNeeded(entry.row);
        }
        setAreThereNotifications();
    }

    private void updateNotificationViews(NotificationData.Entry entry, StatusBarNotification sbn) {
        RemoteViews contentView = entry.cachedContentView;
        RemoteViews bigContentView = entry.cachedBigContentView;
        RemoteViews headsUpContentView = entry.cachedHeadsUpContentView;
        RemoteViews publicContentView = entry.cachedPublicContentView;
        contentView.reapply(this.mContext, entry.getContentView(), this.mOnClickHandler);
        if (bigContentView != null && entry.getExpandedContentView() != null) {
            bigContentView.reapply(sbn.getPackageContext(this.mContext), entry.getExpandedContentView(), this.mOnClickHandler);
        }
        View headsUpChild = entry.getHeadsUpContentView();
        if (headsUpContentView != null && headsUpChild != null) {
            headsUpContentView.reapply(sbn.getPackageContext(this.mContext), headsUpChild, this.mOnClickHandler);
        }
        if (publicContentView != null && entry.getPublicContentView() != null) {
            publicContentView.reapply(sbn.getPackageContext(this.mContext), entry.getPublicContentView(), this.mOnClickHandler);
        }
        this.mNotificationClicker.register(entry.row, sbn);
        entry.row.onNotificationUpdated(entry);
        entry.row.resetHeight();
    }

    protected void updatePublicContentView(NotificationData.Entry entry, StatusBarNotification sbn) {
        int i;
        RemoteViews publicContentView = entry.cachedPublicContentView;
        View inflatedView = entry.getPublicContentView();
        if (!entry.autoRedacted || publicContentView == null || inflatedView == null) {
            return;
        }
        boolean disabledByPolicy = !adminAllowsUnredactedNotifications(entry.notification.getUserId());
        Context context = this.mContext;
        if (disabledByPolicy) {
            i = R.string.aerr_close;
        } else {
            i = R.string.aerr_application_repeated;
        }
        String notificationHiddenText = context.getString(i);
        TextView titleView = (TextView) inflatedView.findViewById(R.id.title);
        if (titleView == null || titleView.getText().toString().equals(notificationHiddenText)) {
            return;
        }
        publicContentView.setTextViewText(R.id.title, notificationHiddenText);
        publicContentView.reapply(sbn.getPackageContext(this.mContext), inflatedView, this.mOnClickHandler);
        entry.row.onNotificationUpdated(entry);
    }

    public void notifyHeadsUpScreenOff() {
        maybeEscalateHeadsUp();
    }

    private boolean alertAgain(NotificationData.Entry oldEntry, Notification newNotification) {
        return oldEntry == null || !oldEntry.hasInterrupted() || (newNotification.flags & 8) == 0;
    }

    protected boolean shouldPeek(NotificationData.Entry entry) {
        return shouldPeek(entry, entry.notification);
    }

    protected boolean shouldPeek(NotificationData.Entry entry, StatusBarNotification sbn) {
        if (!this.mUseHeadsUp || isDeviceInVrMode()) {
            return false;
        }
        if (this.mNotificationData.shouldFilterOut(sbn)) {
            Log.d("StatusBar", "No peeking: filtered notification: " + sbn.getKey());
            return false;
        }
        boolean inUse = this.mPowerManager.isScreenOn() && (!this.mStatusBarKeyguardViewManager.isShowing() || this.mStatusBarKeyguardViewManager.isOccluded()) && !this.mStatusBarKeyguardViewManager.isInputRestricted();
        if (inUse) {
            try {
                inUse = !this.mDreamManager.isDreaming();
            } catch (RemoteException e) {
                Log.d("StatusBar", "failed to query dream manager", e);
            }
        }
        if (!inUse) {
            Log.d("StatusBar", "No peeking: not in use: " + sbn.getKey());
            return false;
        }
        if (this.mNotificationData.shouldSuppressScreenOn(sbn.getKey())) {
            Log.d("StatusBar", "No peeking: suppressed by DND: " + sbn.getKey());
            return false;
        }
        if (entry.hasJustLaunchedFullScreenIntent()) {
            Log.d("StatusBar", "No peeking: recent fullscreen: " + sbn.getKey());
            return false;
        }
        if (isSnoozedPackage(sbn)) {
            Log.d("StatusBar", "No peeking: snoozed package: " + sbn.getKey());
            return false;
        }
        if (this.mNotificationData.getImportance(sbn.getKey()) < 4) {
            Log.d("StatusBar", "No peeking: unimportant notification: " + sbn.getKey());
            return false;
        }
        if (sbn.getNotification().fullScreenIntent != null) {
            if (!this.mAccessibilityManager.isTouchExplorationEnabled()) {
                return true;
            }
            Log.d("StatusBar", "No peeking: accessible fullscreen: " + sbn.getKey());
            return false;
        }
        Log.d("StatusBar", "shouldPeek: true");
        return true;
    }

    public void setBouncerShowing(boolean bouncerShowing) {
        this.mBouncerShowing = bouncerShowing;
    }

    public boolean isBouncerShowing() {
        return this.mBouncerShowing;
    }

    public void destroy() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        try {
            this.mNotificationListener.unregisterAsSystemService();
        } catch (RemoteException e) {
        }
    }

    public static PackageManager getPackageManagerForUser(Context context, int userId) {
        Context contextForUser = context;
        if (userId >= 0) {
            try {
                contextForUser = context.createPackageContextAsUser(context.getPackageName(), 4, new UserHandle(userId));
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return contextForUser.getPackageManager();
    }

    @Override
    public void logNotificationExpansion(String key, boolean userAction, boolean expanded) {
        try {
            this.mBarService.onNotificationExpansionChanged(key, userAction, expanded);
        } catch (RemoteException e) {
        }
    }

    public boolean isKeyguardSecure() {
        if (this.mStatusBarKeyguardViewManager == null) {
            Slog.w("StatusBar", "isKeyguardSecure() called before startKeyguard(), returning false", new Throwable());
            return false;
        }
        return this.mStatusBarKeyguardViewManager.isSecure();
    }

    @Override
    public void showAssistDisclosure() {
        if (this.mAssistManager == null) {
            return;
        }
        this.mAssistManager.showDisclosure();
    }

    @Override
    public void startAssist(Bundle args) {
        if (this.mAssistManager == null) {
            return;
        }
        this.mAssistManager.startAssist(args);
    }

    public boolean isCameraAllowedByAdmin() {
        if (this.mDevicePolicyManager.getCameraDisabled(null, this.mCurrentUserId)) {
            return false;
        }
        return (isKeyguardShowing() && isKeyguardSecure() && (this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mCurrentUserId) & 2) != 0) ? false : true;
    }

    public boolean isKeyguardShowing() {
        if (this.mStatusBarKeyguardViewManager == null) {
            Slog.i("StatusBar", "isKeyguardShowing() called before startKeyguard(), returning true");
            return true;
        }
        return this.mStatusBarKeyguardViewManager.isShowing();
    }
}
