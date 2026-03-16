package com.android.systemui.statusbar;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
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
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.widget.DateTimeView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarIconList;
import com.android.internal.util.NotificationColorUtil;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SearchPanelView;
import com.android.systemui.SwipeHelper;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.policy.HeadsUpNotificationView;
import com.android.systemui.statusbar.policy.PreviewInflater;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class BaseStatusBar extends SystemUI implements RecentsComponent.Callbacks, ActivatableNotificationView.OnActivatedListener, CommandQueue.Callbacks, ExpandableNotificationRow.ExpansionLogger, NotificationData.Environment {
    public static final boolean DEBUG = Log.isLoggable("StatusBar", 3);
    protected AccessibilityManager mAccessibilityManager;
    protected IStatusBarService mBarService;
    protected boolean mBouncerShowing;
    protected CommandQueue mCommandQueue;
    protected DevicePolicyManager mDevicePolicyManager;
    protected DismissView mDismissView;
    protected Display mDisplay;
    protected IDreamManager mDreamManager;
    protected EmptyShadeView mEmptyShadeView;
    private TimeInterpolator mFastOutLinearIn;
    private float mFontScale;
    protected int mHeadsUpNotificationDecay;
    protected HeadsUpNotificationView mHeadsUpNotificationView;
    protected NotificationOverflowContainer mKeyguardIconOverflowContainer;
    private TimeInterpolator mLinearOutSlowIn;
    private Locale mLocale;
    private NotificationColorUtil mNotificationColorUtil;
    protected NotificationData mNotificationData;
    private NotificationGuts mNotificationGutsExposed;
    PowerManager mPowerManager;
    private RecentsComponent mRecents;
    protected int mRowMaxHeight;
    protected int mRowMinHeight;
    protected Boolean mScreenOn;
    protected boolean mScreenOnFromKeyguard;
    protected SearchPanelView mSearchPanelView;
    protected boolean mShowLockscreenNotifications;
    protected NotificationStackScrollLayout mStackScroller;
    protected int mState;
    protected StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private UserManager mUserManager;
    protected boolean mVisible;
    private boolean mVisibleToUser;
    protected WindowManager mWindowManager;
    protected IWindowManager mWindowManagerService;
    protected int mZenMode;
    protected H mHandler = createHandler();
    protected int mCurrentUserId = 0;
    protected final SparseArray<UserInfo> mCurrentProfiles = new SparseArray<>();
    protected int mLayoutDirection = -1;
    protected NavigationBarView mNavigationBarView = null;
    protected boolean mUseHeadsUp = false;
    protected boolean mHeadsUpTicker = false;
    protected boolean mDisableNotificationAlerts = false;
    private boolean mLockscreenPublicMode = false;
    private final SparseBooleanArray mUsersAllowingPrivateNotifications = new SparseBooleanArray();
    private boolean mDeviceProvisioned = false;
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
            BaseStatusBar.this.updateNotifications();
        }
    };
    private RemoteViews.OnClickHandler mOnClickHandler = new RemoteViews.OnClickHandler() {
        public boolean onClickHandler(final View view, final PendingIntent pendingIntent, final Intent fillInIntent) {
            if (BaseStatusBar.DEBUG) {
                Log.v("StatusBar", "Notification click handler invoked for intent: " + pendingIntent);
            }
            logActionClick(view);
            try {
                ActivityManagerNative.getDefault().resumeAppSwitches();
            } catch (RemoteException e) {
            }
            boolean isActivity = pendingIntent.isActivity();
            if (!isActivity) {
                return super.onClickHandler(view, pendingIntent, fillInIntent);
            }
            final boolean keyguardShowing = BaseStatusBar.this.mStatusBarKeyguardViewManager.isShowing();
            final boolean afterKeyguardGone = PreviewInflater.wouldLaunchResolverActivity(BaseStatusBar.this.mContext, pendingIntent.getIntent(), BaseStatusBar.this.mCurrentUserId);
            BaseStatusBar.this.dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() {
                @Override
                public boolean onDismiss() {
                    if (keyguardShowing && !afterKeyguardGone) {
                        try {
                            ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                        } catch (RemoteException e2) {
                        }
                    }
                    boolean handled = superOnClickHandler(view, pendingIntent, fillInIntent);
                    BaseStatusBar.this.overrideActivityPendingAppTransition(keyguardShowing && !afterKeyguardGone);
                    if (handled) {
                        BaseStatusBar.this.animateCollapsePanels(2, true);
                        BaseStatusBar.this.visibilityChanged(false);
                    }
                    return handled;
                }
            }, afterKeyguardGone);
            return true;
        }

        private void logActionClick(View view) {
            ViewParent parent = view.getParent();
            String key = getNotificationKeyForParent(parent);
            if (key == null) {
                Log.w("StatusBar", "Couldn't determine notification for click.");
                return;
            }
            int index = -1;
            if (view.getId() == 16909103 && parent != null && (parent instanceof ViewGroup)) {
                ViewGroup actionGroup = (ViewGroup) parent;
                index = actionGroup.indexOfChild(view);
            }
            Log.d("StatusBar", "Clicked on button " + index + " for " + key);
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

        private boolean superOnClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent) {
            return super.onClickHandler(view, pendingIntent, fillInIntent);
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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
            if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                BaseStatusBar.this.mUsersAllowingPrivateNotifications.clear();
                BaseStatusBar.this.updateLockscreenNotificationSetting();
                BaseStatusBar.this.updateNotifications();
            } else if ("com.android.systemui.statusbar.banner_action_cancel".equals(action) || "com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                NotificationManager noMan = (NotificationManager) BaseStatusBar.this.mContext.getSystemService("notification");
                noMan.cancel(10000);
                Settings.Secure.putInt(BaseStatusBar.this.mContext.getContentResolver(), "show_note_about_notification_hiding", 0);
                if ("com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                    BaseStatusBar.this.animateCollapsePanels(2, true);
                    BaseStatusBar.this.mContext.startActivity(new Intent("android.settings.ACTION_APP_NOTIFICATION_REDACTION").addFlags(268435456));
                }
            }
        }
    };
    private final NotificationListenerService mNotificationListener = new NotificationListenerService() {
        @Override
        public void onListenerConnected() {
            if (BaseStatusBar.DEBUG) {
                Log.d("StatusBar", "onListenerConnected");
            }
            final StatusBarNotification[] notifications = getActiveNotifications();
            final NotificationListenerService.RankingMap currentRanking = getCurrentRanking();
            BaseStatusBar.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    StatusBarNotification[] arr$ = notifications;
                    for (StatusBarNotification sbn : arr$) {
                        BaseStatusBar.this.addNotification(sbn, currentRanking);
                    }
                }
            });
        }

        @Override
        public void onNotificationPosted(final StatusBarNotification sbn, final NotificationListenerService.RankingMap rankingMap) {
            if (BaseStatusBar.DEBUG) {
                Log.d("StatusBar", "onNotificationPosted: " + sbn);
            }
            BaseStatusBar.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Notification n = sbn.getNotification();
                    boolean isUpdate = BaseStatusBar.this.mNotificationData.get(sbn.getKey()) != null || BaseStatusBar.this.isHeadsUp(sbn.getKey());
                    if (n.isGroupChild() && BaseStatusBar.this.mNotificationData.isGroupWithSummary(sbn.getGroupKey())) {
                        if (BaseStatusBar.DEBUG) {
                            Log.d("StatusBar", "Ignoring group child due to existing summary: " + sbn);
                        }
                        if (isUpdate) {
                            BaseStatusBar.this.removeNotification(sbn.getKey(), rankingMap);
                            return;
                        } else {
                            BaseStatusBar.this.mNotificationData.updateRanking(rankingMap);
                            return;
                        }
                    }
                    if (isUpdate) {
                        BaseStatusBar.this.updateNotification(sbn, rankingMap);
                    } else {
                        BaseStatusBar.this.addNotification(sbn, rankingMap);
                    }
                }
            });
        }

        @Override
        public void onNotificationRemoved(final StatusBarNotification sbn, final NotificationListenerService.RankingMap rankingMap) {
            if (BaseStatusBar.DEBUG) {
                Log.d("StatusBar", "onNotificationRemoved: " + sbn);
            }
            BaseStatusBar.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    BaseStatusBar.this.removeNotification(sbn.getKey(), rankingMap);
                }
            });
        }

        @Override
        public void onNotificationRankingUpdate(final NotificationListenerService.RankingMap rankingMap) {
            if (BaseStatusBar.DEBUG) {
                Log.d("StatusBar", "onRankingUpdate");
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
                return false;
            }
            if (action == 3) {
                BaseStatusBar.this.cancelPreloadingRecents();
                return false;
            }
            if (action == 1 && !v.isPressed()) {
                BaseStatusBar.this.cancelPreloadingRecents();
                return false;
            }
            return false;
        }
    };

    public abstract void addNotification(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap);

    protected abstract void createAndAddWindows();

    protected abstract int getMaxKeyguardNotifications();

    protected abstract WindowManager.LayoutParams getSearchLayoutParams(ViewGroup.LayoutParams layoutParams);

    protected abstract View getStatusBarView();

    protected abstract void haltTicker();

    protected abstract void refreshLayout(int i);

    public abstract void removeNotification(String str, NotificationListenerService.RankingMap rankingMap);

    public abstract void resetHeadsUpDecayTimer();

    public abstract void scheduleHeadsUpEscalation();

    protected abstract void setAreThereNotifications();

    protected abstract boolean shouldDisableNavbarGestures();

    protected abstract void tick(StatusBarNotification statusBarNotification, boolean z);

    protected abstract void updateExpandedViewPos(int i);

    protected abstract void updateNotificationRanking(NotificationListenerService.RankingMap rankingMap);

    protected abstract void updateNotifications();

    @Override
    public boolean isDeviceProvisioned() {
        return this.mDeviceProvisioned;
    }

    private void updateCurrentProfilesCache() {
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
        this.mNotificationColorUtil = NotificationColorUtil.getInstance(this.mContext);
        this.mNotificationData = new NotificationData(this);
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        this.mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.checkService("dreams"));
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mSettingsObserver.onChange(false);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("device_provisioned"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("zen_mode"), false, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("lock_screen_show_notifications"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("lock_screen_allow_private_notifications"), true, this.mLockscreenSettingsObserver, -1);
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        this.mRecents = (RecentsComponent) getComponent(RecentsComponent.class);
        this.mRecents.setCallback(this);
        Configuration currentConfig = this.mContext.getResources().getConfiguration();
        this.mLocale = currentConfig.locale;
        this.mLayoutDirection = TextUtils.getLayoutDirectionFromLocale(this.mLocale);
        this.mFontScale = currentConfig.fontScale;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mLinearOutSlowIn = AnimationUtils.loadInterpolator(this.mContext, R.interpolator.linear_out_slow_in);
        this.mFastOutLinearIn = AnimationUtils.loadInterpolator(this.mContext, R.interpolator.fast_out_linear_in);
        StatusBarIconList iconList = new StatusBarIconList();
        this.mCommandQueue = new CommandQueue(this, iconList);
        int[] switches = new int[8];
        ArrayList<IBinder> binders = new ArrayList<>();
        try {
            this.mBarService.registerStatusBar(this.mCommandQueue, iconList, switches, binders);
        } catch (RemoteException e) {
        }
        createAndAddWindows();
        disable(switches[0], false);
        setSystemUiVisibility(switches[1], -1);
        topAppWindowChanged(switches[2] != 0);
        setImeWindowStatus(binders.get(0), switches[3], switches[4], switches[5] != 0);
        int N = iconList.size();
        int viewIndex = 0;
        for (int i = 0; i < N; i++) {
            StatusBarIcon icon = iconList.getIcon(i);
            if (icon != null) {
                addIcon(iconList.getSlot(i), i, viewIndex, icon);
                viewIndex++;
            }
        }
        try {
            this.mNotificationListener.registerAsSystemService(this.mContext, new ComponentName(this.mContext.getPackageName(), getClass().getCanonicalName()), -1);
        } catch (RemoteException e2) {
            Log.e("StatusBar", "Unable to register notification listener", e2);
        }
        if (DEBUG) {
            Log.d("StatusBar", String.format("init: icons=%d disabled=0x%08x lights=0x%08x menu=0x%08x imeButton=0x%08x", Integer.valueOf(iconList.size()), Integer.valueOf(switches[0]), Integer.valueOf(switches[1]), Integer.valueOf(switches[2]), Integer.valueOf(switches[3])));
        }
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        setHeadsUpUser(this.mCurrentUserId);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("com.android.systemui.statusbar.banner_action_cancel");
        filter.addAction("com.android.systemui.statusbar.banner_action_setup");
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        updateCurrentProfilesCache();
    }

    protected void notifyUserAboutHiddenNotifications() {
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), "show_note_about_notification_hiding", 1) != 0) {
            Log.d("StatusBar", "user hasn't seen notification about hidden notifications");
            LockPatternUtils lockPatternUtils = new LockPatternUtils(this.mContext);
            if (!lockPatternUtils.isSecure()) {
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
            Resources res = this.mContext.getResources();
            Notification.Builder note = new Notification.Builder(this.mContext).setSmallIcon(com.android.systemui.R.drawable.ic_android).setContentTitle(this.mContext.getString(com.android.systemui.R.string.hidden_notifications_title)).setContentText(this.mContext.getString(com.android.systemui.R.string.hidden_notifications_text)).setPriority(1).setOngoing(true).setColor(res.getColor(R.color.system_accent3_600)).setContentIntent(setupIntent).addAction(com.android.systemui.R.drawable.ic_close, this.mContext.getString(com.android.systemui.R.string.hidden_notifications_cancel), cancelIntent).addAction(com.android.systemui.R.drawable.ic_settings, this.mContext.getString(com.android.systemui.R.string.hidden_notifications_setup), setupIntent);
            NotificationManager noMan = (NotificationManager) this.mContext.getSystemService("notification");
            noMan.notify(10000, note.build());
        }
    }

    public void userSwitched(int newUserId) {
        setHeadsUpUser(newUserId);
    }

    private void setHeadsUpUser(int newUserId) {
        if (this.mHeadsUpNotificationView != null) {
            this.mHeadsUpNotificationView.setUser(newUserId);
        }
    }

    public boolean isHeadsUp(String key) {
        return this.mHeadsUpNotificationView != null && this.mHeadsUpNotificationView.isShowing(key);
    }

    @Override
    public boolean isNotificationForCurrentProfiles(StatusBarNotification n) {
        int i = this.mCurrentUserId;
        int notificationUserId = n.getUserId();
        if (DEBUG) {
        }
        return isCurrentProfile(notificationUserId);
    }

    protected boolean isCurrentProfile(int userId) {
        boolean z;
        synchronized (this.mCurrentProfiles) {
            if (userId != -1) {
                z = this.mCurrentProfiles.get(userId) != null;
            }
        }
        return z;
    }

    @Override
    public String getCurrentMediaNotificationKey() {
        return null;
    }

    protected void dismissKeyguardThenExecute(KeyguardHostView.OnDismissAction action, boolean afterKeyguardGone) {
        action.onDismiss();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        Locale locale = this.mContext.getResources().getConfiguration().locale;
        int ld = TextUtils.getLayoutDirectionFromLocale(locale);
        float fontScale = newConfig.fontScale;
        if (!locale.equals(this.mLocale) || ld != this.mLayoutDirection || fontScale != this.mFontScale) {
            if (DEBUG) {
                Log.v("StatusBar", String.format("config changed locale/LD: %s (%d) -> %s (%d)", this.mLocale, Integer.valueOf(this.mLayoutDirection), locale, Integer.valueOf(ld)));
            }
            this.mLocale = locale;
            this.mLayoutDirection = ld;
            refreshLayout(ld);
        }
    }

    protected View updateNotificationVetoButton(View row, StatusBarNotification n) {
        View vetoButton = row.findViewById(com.android.systemui.R.id.veto);
        if (n.isClearable() || (this.mHeadsUpNotificationView.getEntry() != null && this.mHeadsUpNotificationView.getEntry().row == row)) {
            final String _pkg = n.getPackageName();
            final String _tag = n.getTag();
            final int _id = n.getId();
            final int _userId = n.getUserId();
            vetoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.announceForAccessibility(BaseStatusBar.this.mContext.getString(com.android.systemui.R.string.accessibility_notification_dismissed));
                    try {
                        BaseStatusBar.this.mBarService.onNotificationClear(_pkg, _tag, _id, _userId);
                    } catch (RemoteException e) {
                    }
                }
            });
            vetoButton.setVisibility(0);
        } else {
            vetoButton.setVisibility(8);
        }
        vetoButton.setImportantForAccessibility(2);
        return vetoButton;
    }

    protected void applyColorsAndBackgrounds(StatusBarNotification sbn, NotificationData.Entry entry) {
        if (entry.expanded.getId() != 16909107) {
            if (entry.targetSdk >= 9 && entry.targetSdk < 21) {
                entry.row.setShowingLegacyBackground(true);
                entry.legacy = true;
            }
        } else {
            int color = sbn.getNotification().color;
            if (isMediaNotification(entry)) {
                ExpandableNotificationRow expandableNotificationRow = entry.row;
                if (color == 0) {
                    color = this.mContext.getResources().getColor(com.android.systemui.R.color.notification_material_background_media_default_color);
                }
                expandableNotificationRow.setTintColor(color);
            }
        }
        if (entry.icon != null) {
            if (entry.targetSdk >= 21) {
                entry.icon.setColorFilter(this.mContext.getResources().getColor(R.color.white));
            } else {
                entry.icon.setColorFilter((ColorFilter) null);
            }
        }
    }

    public boolean isMediaNotification(NotificationData.Entry entry) {
        return (entry.expandedBig == null || entry.expandedBig.findViewById(R.id.four) == null) ? false : true;
    }

    private void startAppOwnNotificationSettingsActivity(Intent intent, int notificationId, String notificationTag, int appUid) {
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("notification_tag", notificationTag);
        startNotificationGutsIntent(intent, appUid);
    }

    private void startAppNotificationSettingsActivity(String packageName, int appUid) {
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
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (keyguardShowing) {
                                ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                            }
                            TaskStackBuilder.create(BaseStatusBar.this.mContext).addNextIntentWithParentStack(intent).startActivities(null, new UserHandle(UserHandle.getUserId(appUid)));
                            BaseStatusBar.this.overrideActivityPendingAppTransition(keyguardShowing);
                        } catch (RemoteException e) {
                        }
                    }
                });
                BaseStatusBar.this.animateCollapsePanels(2, true);
                return true;
            }
        }, false);
    }

    private void inflateGuts(ExpandableNotificationRow row) {
        ViewStub stub = (ViewStub) row.findViewById(com.android.systemui.R.id.notification_guts_stub);
        if (stub != null) {
            stub.inflate();
        }
        final StatusBarNotification sbn = row.getStatusBarNotification();
        PackageManager pmUser = getPackageManagerForUser(sbn.getUser().getIdentifier());
        row.setTag(sbn.getPackageName());
        View guts = row.findViewById(com.android.systemui.R.id.notification_guts);
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
        ((ImageView) row.findViewById(R.id.icon)).setImageDrawable(pkgicon);
        row.findViewById(com.android.systemui.R.id.timestamp).setTime(sbn.getPostTime());
        ((TextView) row.findViewById(com.android.systemui.R.id.pkgname)).setText(appname);
        View settingsButton = guts.findViewById(com.android.systemui.R.id.notification_inspect_item);
        View appSettingsButton = guts.findViewById(com.android.systemui.R.id.notification_inspect_app_provided_settings);
        if (appUid >= 0) {
            final int appUidF = appUid;
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BaseStatusBar.this.startAppNotificationSettingsActivity(pkg, appUidF);
                }
            });
            Intent appSettingsQueryIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.NOTIFICATION_PREFERENCES").setPackage(pkg);
            List<ResolveInfo> infos = pmUser.queryIntentActivities(appSettingsQueryIntent, 0);
            if (infos.size() > 0) {
                appSettingsButton.setVisibility(0);
                appSettingsButton.setContentDescription(this.mContext.getResources().getString(com.android.systemui.R.string.status_bar_notification_app_settings_title, appname));
                final Intent appSettingsLaunchIntent = new Intent(appSettingsQueryIntent).setClassName(pkg, infos.get(0).activityInfo.name);
                appSettingsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BaseStatusBar.this.startAppOwnNotificationSettingsActivity(appSettingsLaunchIntent, sbn.getId(), sbn.getTag(), appUidF);
                    }
                });
                return;
            }
            appSettingsButton.setVisibility(8);
            return;
        }
        settingsButton.setVisibility(8);
        appSettingsButton.setVisibility(8);
    }

    protected SwipeHelper.LongPressListener getNotificationLongClicker() {
        return new SwipeHelper.LongPressListener() {
            @Override
            public boolean onLongPress(View v, int x, int y) {
                BaseStatusBar.this.dismissPopups();
                if (!(v instanceof ExpandableNotificationRow)) {
                    return false;
                }
                if (v.getWindowToken() != null) {
                    BaseStatusBar.this.inflateGuts((ExpandableNotificationRow) v);
                    NotificationGuts guts = (NotificationGuts) v.findViewById(com.android.systemui.R.id.notification_guts);
                    if (guts == null) {
                        return false;
                    }
                    if (guts.getVisibility() == 0) {
                        Log.e("StatusBar", "Trying to show notification guts, but already visible");
                        return false;
                    }
                    guts.setVisibility(0);
                    double horz = Math.max(guts.getWidth() - x, x);
                    double vert = Math.max(guts.getActualHeight() - y, y);
                    float r = (float) Math.hypot(horz, vert);
                    Animator a = ViewAnimationUtils.createCircularReveal(guts, x, y, 0.0f, r);
                    a.setDuration(400L);
                    a.setInterpolator(BaseStatusBar.this.mLinearOutSlowIn);
                    a.start();
                    BaseStatusBar.this.mNotificationGutsExposed = guts;
                    return true;
                }
                Log.e("StatusBar", "Trying to show notification guts, but not attached to window");
                return false;
            }
        };
    }

    public void dismissPopups() {
        if (this.mNotificationGutsExposed != null) {
            final NotificationGuts v = this.mNotificationGutsExposed;
            this.mNotificationGutsExposed = null;
            if (v.getWindowToken() != null) {
                int x = (v.getLeft() + v.getRight()) / 2;
                int y = v.getTop() + (v.getActualHeight() / 2);
                Animator a = ViewAnimationUtils.createCircularReveal(v, x, y, x, 0.0f);
                a.setDuration(200L);
                a.setInterpolator(this.mFastOutLinearIn);
                a.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        v.setVisibility(8);
                    }
                });
                a.start();
            }
        }
    }

    public void onHeadsUpDismissed() {
    }

    @Override
    public void showRecentApps(boolean triggeredFromAltTab) {
        this.mHandler.removeMessages(1019);
        this.mHandler.obtainMessage(1019, triggeredFromAltTab ? 1 : 0, 0).sendToTarget();
    }

    @Override
    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        this.mHandler.removeMessages(1020);
        this.mHandler.obtainMessage(1020, triggeredFromAltTab ? 1 : 0, triggeredFromHomeKey ? 1 : 0).sendToTarget();
    }

    @Override
    public void toggleRecentApps() {
        this.mHandler.removeMessages(1021);
        this.mHandler.sendEmptyMessage(1021);
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

    public void showNextAffiliatedTask() {
        this.mHandler.removeMessages(1024);
        this.mHandler.sendEmptyMessage(1024);
    }

    public void showPreviousAffiliatedTask() {
        this.mHandler.removeMessages(1025);
        this.mHandler.sendEmptyMessage(1025);
    }

    public void showSearchPanel() {
        if (this.mSearchPanelView != null && this.mSearchPanelView.isAssistantAvailable()) {
            this.mSearchPanelView.show(true, true);
        }
    }

    protected void updateSearchPanel() {
        boolean visible = false;
        if (this.mSearchPanelView != null) {
            visible = this.mSearchPanelView.isShowing();
            this.mWindowManager.removeView(this.mSearchPanelView);
        }
        LinearLayout tmpRoot = new LinearLayout(this.mContext);
        this.mSearchPanelView = (SearchPanelView) LayoutInflater.from(this.mContext).inflate(com.android.systemui.R.layout.status_bar_search_panel, (ViewGroup) tmpRoot, false);
        this.mSearchPanelView.setOnTouchListener(new TouchOutsideListener(1027, this.mSearchPanelView));
        this.mSearchPanelView.setVisibility(8);
        boolean vertical = this.mNavigationBarView != null && this.mNavigationBarView.isVertical();
        this.mSearchPanelView.setHorizontal(vertical);
        WindowManager.LayoutParams lp = getSearchLayoutParams(this.mSearchPanelView.getLayoutParams());
        this.mWindowManager.addView(this.mSearchPanelView, lp);
        this.mSearchPanelView.setBar(this);
        if (visible) {
            this.mSearchPanelView.show(true, false);
        }
    }

    protected H createHandler() {
        return new H();
    }

    static void sendCloseSystemWindows(Context context, String reason) {
        if (ActivityManagerNative.isSystemReady()) {
            try {
                ActivityManagerNative.getDefault().closeSystemDialogs(reason);
            } catch (RemoteException e) {
            }
        }
    }

    protected void showRecents(boolean triggeredFromAltTab) {
        if (this.mRecents != null) {
            sendCloseSystemWindows(this.mContext, "recentapps");
            this.mRecents.showRecents(triggeredFromAltTab, getStatusBarView());
        }
    }

    protected void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        if (this.mRecents != null) {
            this.mRecents.hideRecents(triggeredFromAltTab, triggeredFromHomeKey);
        }
    }

    protected void toggleRecents() {
        if (this.mRecents != null) {
            sendCloseSystemWindows(this.mContext, "recentapps");
            this.mRecents.toggleRecents(this.mDisplay, this.mLayoutDirection, getStatusBarView());
        }
    }

    protected void preloadRecents() {
        if (this.mRecents != null) {
            this.mRecents.preloadRecents();
        }
    }

    protected void cancelPreloadingRecents() {
        if (this.mRecents != null) {
            this.mRecents.cancelPreloadingRecents();
        }
    }

    protected void showRecentsNextAffiliatedTask() {
        if (this.mRecents != null) {
            this.mRecents.showNextAffiliatedTask();
        }
    }

    protected void showRecentsPreviousAffiliatedTask() {
        if (this.mRecents != null) {
            this.mRecents.showPrevAffiliatedTask();
        }
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
    }

    public void setLockscreenPublicMode(boolean publicMode) {
        this.mLockscreenPublicMode = publicMode;
    }

    public boolean isLockscreenPublicMode() {
        return this.mLockscreenPublicMode;
    }

    public boolean userAllowsPrivateNotificationsInPublic(int userHandle) {
        if (userHandle == -1) {
            return true;
        }
        if (this.mUsersAllowingPrivateNotifications.indexOfKey(userHandle) < 0) {
            boolean allowed = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", 0, userHandle) != 0;
            int dpmFlags = this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, userHandle);
            boolean allowedByDpm = (dpmFlags & 8) == 0;
            this.mUsersAllowingPrivateNotifications.append(userHandle, allowed && allowedByDpm);
            return allowed;
        }
        return this.mUsersAllowingPrivateNotifications.get(userHandle);
    }

    @Override
    public boolean shouldHideSensitiveContents(int userid) {
        return isLockscreenPublicMode() && !userAllowsPrivateNotificationsInPublic(userid);
    }

    public void onNotificationClear(StatusBarNotification notification) {
        try {
            this.mBarService.onNotificationClear(notification.getPackageName(), notification.getTag(), notification.getId(), notification.getUserId());
        } catch (RemoteException e) {
        }
    }

    protected class H extends Handler {
        protected H() {
        }

        @Override
        public void handleMessage(Message m) {
            switch (m.what) {
                case 1019:
                    BaseStatusBar.this.showRecents(m.arg1 > 0);
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
                case 1027:
                    if (BaseStatusBar.DEBUG) {
                        Log.d("StatusBar", "closing search panel");
                    }
                    if (BaseStatusBar.this.mSearchPanelView != null && BaseStatusBar.this.mSearchPanelView.isShowing()) {
                        BaseStatusBar.this.mSearchPanelView.show(false, true);
                        break;
                    }
                    break;
            }
        }
    }

    public class TouchOutsideListener implements View.OnTouchListener {
        private int mMsg;
        private StatusBarPanel mPanel;

        public TouchOutsideListener(int msg, StatusBarPanel panel) {
            this.mMsg = msg;
            this.mPanel = panel;
        }

        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            int action = ev.getAction();
            if (action != 4 && (action != 0 || this.mPanel.isInContentArea((int) ev.getX(), (int) ev.getY()))) {
                return false;
            }
            BaseStatusBar.this.mHandler.removeMessages(this.mMsg);
            BaseStatusBar.this.mHandler.sendEmptyMessage(this.mMsg);
            return true;
        }
    }

    protected void workAroundBadLayerDrawableOpacity(View v) {
    }

    private boolean inflateViews(NotificationData.Entry entry, ViewGroup parent) {
        return inflateViews(entry, parent, false);
    }

    protected boolean inflateViewsForHeadsUp(NotificationData.Entry entry, ViewGroup parent) {
        return inflateViews(entry, parent, true);
    }

    private boolean inflateViews(NotificationData.Entry entry, ViewGroup parent, boolean isHeadsUp) {
        ExpandableNotificationRow row;
        PackageManager pmUser = getPackageManagerForUser(entry.notification.getUser().getIdentifier());
        int maxHeight = this.mRowMaxHeight;
        StatusBarNotification sbn = entry.notification;
        RemoteViews contentView = sbn.getNotification().contentView;
        RemoteViews bigContentView = sbn.getNotification().bigContentView;
        if (isHeadsUp) {
            maxHeight = this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.notification_mid_height);
            bigContentView = sbn.getNotification().headsUpContentView;
        }
        if (contentView == null) {
            return false;
        }
        if (DEBUG) {
            Log.v("StatusBar", "publicNotification: " + sbn.getNotification().publicVersion);
        }
        Notification publicNotification = sbn.getNotification().publicVersion;
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
        }
        workAroundBadLayerDrawableOpacity(row);
        View vetoButton = updateNotificationVetoButton(row, sbn);
        vetoButton.setContentDescription(this.mContext.getString(com.android.systemui.R.string.accessibility_remove_notification));
        NotificationContentView expanded = (NotificationContentView) row.findViewById(com.android.systemui.R.id.expanded);
        NotificationContentView expandedPublic = (NotificationContentView) row.findViewById(com.android.systemui.R.id.expandedPublic);
        row.setDescendantFocusability(393216);
        PendingIntent contentIntent = sbn.getNotification().contentIntent;
        if (contentIntent != null) {
            NotificationClicker listener = makeClicker(contentIntent, sbn.getKey(), isHeadsUp);
            row.setOnClickListener(listener);
        } else {
            row.setOnClickListener(null);
        }
        View bigContentViewLocal = null;
        try {
            View contentViewLocal = contentView.apply(this.mContext, expanded, this.mOnClickHandler);
            if (bigContentView != null) {
                bigContentViewLocal = bigContentView.apply(this.mContext, expanded, this.mOnClickHandler);
            }
            if (contentViewLocal != null) {
                contentViewLocal.setIsRootNamespace(true);
                expanded.setContractedChild(contentViewLocal);
            }
            if (bigContentViewLocal != null) {
                bigContentViewLocal.setIsRootNamespace(true);
                expanded.setExpandedChild(bigContentViewLocal);
            }
            View publicViewLocal = null;
            if (publicNotification != null) {
                try {
                    publicViewLocal = publicNotification.contentView.apply(this.mContext, expandedPublic, this.mOnClickHandler);
                    if (publicViewLocal != null) {
                        publicViewLocal.setIsRootNamespace(true);
                        expandedPublic.setContractedChild(publicViewLocal);
                    }
                } catch (RuntimeException e) {
                    String ident = sbn.getPackageName() + "/0x" + Integer.toHexString(sbn.getId());
                    Log.e("StatusBar", "couldn't inflate public view for notification " + ident, e);
                    publicViewLocal = null;
                }
            }
            try {
                ApplicationInfo info = pmUser.getApplicationInfo(sbn.getPackageName(), 0);
                entry.targetSdk = info.targetSdkVersion;
            } catch (PackageManager.NameNotFoundException ex) {
                Log.e("StatusBar", "Failed looking up ApplicationInfo for " + sbn.getPackageName(), ex);
            }
            if (publicViewLocal == null) {
                publicViewLocal = LayoutInflater.from(this.mContext).inflate(com.android.systemui.R.layout.notification_public_default, (ViewGroup) expandedPublic, false);
                publicViewLocal.setIsRootNamespace(true);
                expandedPublic.setContractedChild(publicViewLocal);
                TextView title = (TextView) publicViewLocal.findViewById(com.android.systemui.R.id.title);
                try {
                    title.setText(pmUser.getApplicationLabel(pmUser.getApplicationInfo(entry.notification.getPackageName(), 0)));
                } catch (PackageManager.NameNotFoundException e2) {
                    title.setText(entry.notification.getPackageName());
                }
                ImageView icon = (ImageView) publicViewLocal.findViewById(com.android.systemui.R.id.icon);
                ImageView profileBadge = (ImageView) publicViewLocal.findViewById(com.android.systemui.R.id.profile_badge_line3);
                StatusBarIcon ic = new StatusBarIcon(entry.notification.getPackageName(), entry.notification.getUser(), entry.notification.getNotification().icon, entry.notification.getNotification().iconLevel, entry.notification.getNotification().number, entry.notification.getNotification().tickerText);
                Drawable iconDrawable = StatusBarIconView.getIcon(this.mContext, ic);
                icon.setImageDrawable(iconDrawable);
                if (entry.targetSdk >= 21 || this.mNotificationColorUtil.isGrayscaleIcon(iconDrawable)) {
                    icon.setBackgroundResource(R.drawable.ic_media_route_connecting_dark_10_mtrl);
                    int padding = this.mContext.getResources().getDimensionPixelSize(R.dimen.autofill_dialog_offset);
                    icon.setPadding(padding, padding, padding, padding);
                    if (sbn.getNotification().color != 0) {
                        icon.getBackground().setColorFilter(sbn.getNotification().color, PorterDuff.Mode.SRC_ATOP);
                    }
                }
                if (profileBadge != null) {
                    Drawable profileDrawable = this.mContext.getPackageManager().getUserBadgeForDensity(entry.notification.getUser(), 0);
                    if (profileDrawable != null) {
                        profileBadge.setImageDrawable(profileDrawable);
                        profileBadge.setVisibility(0);
                    } else {
                        profileBadge.setVisibility(8);
                    }
                }
                View privateTime = contentViewLocal.findViewById(R.id.KEYCODE_BRIGHTNESS_DOWN);
                DateTimeView time = publicViewLocal.findViewById(com.android.systemui.R.id.time);
                if (privateTime != null && privateTime.getVisibility() == 0) {
                    time.setVisibility(0);
                    time.setTime(entry.notification.getNotification().when);
                }
                TextView text = (TextView) publicViewLocal.findViewById(com.android.systemui.R.id.text);
                if (text != null) {
                    text.setText(com.android.systemui.R.string.notification_hidden_text);
                    text.setTextAppearance(this.mContext, com.android.systemui.R.style.TextAppearance_Material_Notification_Parenthetical);
                }
                int topPadding = Notification.Builder.calculateTopPadding(this.mContext, false, this.mContext.getResources().getConfiguration().fontScale);
                title.setPadding(0, topPadding, 0, 0);
                entry.autoRedacted = true;
            }
            entry.row = row;
            entry.row.setHeightRange(this.mRowMinHeight, maxHeight);
            entry.row.setOnActivatedListener(this);
            entry.expanded = contentViewLocal;
            entry.expandedPublic = publicViewLocal;
            entry.setBigContentView(bigContentViewLocal);
            applyColorsAndBackgrounds(sbn, entry);
            if (hasUserChangedExpansion) {
                row.setUserExpanded(userExpanded);
            }
            row.setUserLocked(userLocked);
            row.setStatusBarNotification(entry.notification);
            return true;
        } catch (RuntimeException e3) {
            String ident2 = sbn.getPackageName() + "/0x" + Integer.toHexString(sbn.getId());
            Log.e("StatusBar", "couldn't inflate view for notification " + ident2, e3);
            return false;
        }
    }

    public NotificationClicker makeClicker(PendingIntent intent, String notificationKey, boolean forHun) {
        return new NotificationClicker(intent, notificationKey, forHun);
    }

    public void startPendingIntentDismissingKeyguard(final PendingIntent intent) {
        if (isDeviceProvisioned()) {
            final boolean keyguardShowing = this.mStatusBarKeyguardViewManager.isShowing();
            final boolean afterKeyguardGone = intent.isActivity() && PreviewInflater.wouldLaunchResolverActivity(this.mContext, intent.getIntent(), this.mCurrentUserId);
            dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() {
                @Override
                public boolean onDismiss() {
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                if (keyguardShowing && !afterKeyguardGone) {
                                    ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                                }
                                ActivityManagerNative.getDefault().resumeAppSwitches();
                            } catch (RemoteException e) {
                            }
                            try {
                                intent.send();
                            } catch (PendingIntent.CanceledException e2) {
                                Log.w("StatusBar", "Sending intent failed: " + e2);
                            }
                            if (intent.isActivity()) {
                                BaseStatusBar.this.overrideActivityPendingAppTransition(keyguardShowing && !afterKeyguardGone);
                            }
                        }
                    }.start();
                    BaseStatusBar.this.animateCollapsePanels(2, true);
                    BaseStatusBar.this.visibilityChanged(false);
                    return true;
                }
            }, afterKeyguardGone);
        }
    }

    protected class NotificationClicker implements View.OnClickListener {
        private PendingIntent mIntent;
        private boolean mIsHeadsUp;
        private final String mNotificationKey;

        public NotificationClicker(PendingIntent intent, String notificationKey, boolean forHun) {
            this.mIntent = intent;
            this.mNotificationKey = notificationKey;
            this.mIsHeadsUp = forHun;
        }

        @Override
        public void onClick(View v) {
            Log.d("StatusBar", "Clicked on content of " + this.mNotificationKey);
            final boolean keyguardShowing = BaseStatusBar.this.mStatusBarKeyguardViewManager.isShowing();
            final boolean afterKeyguardGone = this.mIntent.isActivity() && PreviewInflater.wouldLaunchResolverActivity(BaseStatusBar.this.mContext, this.mIntent.getIntent(), BaseStatusBar.this.mCurrentUserId);
            BaseStatusBar.this.dismissKeyguardThenExecute(new KeyguardHostView.OnDismissAction() {
                @Override
                public boolean onDismiss() {
                    if (NotificationClicker.this.mIsHeadsUp) {
                        BaseStatusBar.this.mHeadsUpNotificationView.releaseAndClose();
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                if (keyguardShowing && !afterKeyguardGone) {
                                    ActivityManagerNative.getDefault().keyguardWaitingForActivityDrawn();
                                }
                                ActivityManagerNative.getDefault().resumeAppSwitches();
                            } catch (RemoteException e) {
                            }
                            if (NotificationClicker.this.mIntent != null) {
                                try {
                                    NotificationClicker.this.mIntent.send();
                                } catch (PendingIntent.CanceledException e2) {
                                    Log.w("StatusBar", "Sending contentIntent failed: " + e2);
                                }
                                if (NotificationClicker.this.mIntent.isActivity()) {
                                    BaseStatusBar.this.overrideActivityPendingAppTransition(keyguardShowing && !afterKeyguardGone);
                                }
                            }
                            try {
                                BaseStatusBar.this.mBarService.onNotificationClick(NotificationClicker.this.mNotificationKey);
                            } catch (RemoteException e3) {
                            }
                        }
                    }.start();
                    BaseStatusBar.this.animateCollapsePanels(2, true);
                    BaseStatusBar.this.visibilityChanged(false);
                    return NotificationClicker.this.mIntent != null && NotificationClicker.this.mIntent.isActivity();
                }
            }, afterKeyguardGone);
        }
    }

    public void animateCollapsePanels(int flags, boolean force) {
    }

    public void overrideActivityPendingAppTransition(boolean keyguardShowing) {
        if (keyguardShowing) {
            try {
                this.mWindowManagerService.overridePendingAppTransition((String) null, 0, 0, (IRemoteCallback) null);
            } catch (RemoteException e) {
                Log.w("StatusBar", "Error overriding app transition: " + e);
            }
        }
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
        this.mVisibleToUser = this.mVisible && this.mScreenOnFromKeyguard;
        if (oldVisibleToUser != this.mVisibleToUser) {
            handleVisibleToUserChanged(this.mVisibleToUser);
        }
    }

    protected void handleVisibleToUserChanged(boolean visibleToUser) {
        try {
            if (visibleToUser) {
                boolean clearNotificationEffects = this.mState == 0 || this.mState == 2;
                this.mBarService.onPanelRevealed(clearNotificationEffects);
            } else {
                this.mBarService.onPanelHidden();
            }
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
        if (DEBUG) {
            Log.d("StatusBar", "createNotificationViews(notification=" + sbn);
        }
        Notification n = sbn.getNotification();
        StatusBarIconView iconView = new StatusBarIconView(this.mContext, sbn.getPackageName() + "/0x" + Integer.toHexString(sbn.getId()), n);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        StatusBarIcon ic = new StatusBarIcon(sbn.getPackageName(), sbn.getUser(), n.icon, n.iconLevel, n.number, n.tickerText);
        if (!iconView.set(ic)) {
            handleNotificationError(sbn, "Couldn't create icon: " + ic);
            return null;
        }
        NotificationData.Entry entry = new NotificationData.Entry(sbn, iconView);
        if (!inflateViews(entry, this.mStackScroller)) {
            handleNotificationError(sbn, "Couldn't expand RemoteViews for: " + sbn);
            return null;
        }
        return entry;
    }

    protected void addNotificationViews(NotificationData.Entry entry, NotificationListenerService.RankingMap ranking) {
        if (entry != null) {
            this.mNotificationData.add(entry, ranking);
            updateNotifications();
        }
    }

    protected void updateRowStates() {
        int maxKeyguardNotifications = getMaxKeyguardNotifications();
        this.mKeyguardIconOverflowContainer.getIconsView().removeAllViews();
        ArrayList<NotificationData.Entry> activeNotifications = this.mNotificationData.getActiveNotifications();
        int N = activeNotifications.size();
        int visibleNotifications = 0;
        boolean onKeyguard = this.mState == 1;
        int i = 0;
        while (i < N) {
            NotificationData.Entry entry = activeNotifications.get(i);
            if (onKeyguard) {
                entry.row.setExpansionDisabled(true);
            } else {
                entry.row.setExpansionDisabled(false);
                if (!entry.row.isUserLocked()) {
                    boolean top = i == 0;
                    entry.row.setSystemExpanded(top);
                }
            }
            boolean showOnKeyguard = shouldShowOnKeyguard(entry.notification);
            if ((isLockscreenPublicMode() && !this.mShowLockscreenNotifications) || (onKeyguard && (visibleNotifications >= maxKeyguardNotifications || !showOnKeyguard))) {
                entry.row.setVisibility(8);
                if (onKeyguard && showOnKeyguard) {
                    this.mKeyguardIconOverflowContainer.getIconsView().addNotification(entry);
                }
            } else {
                boolean wasGone = entry.row.getVisibility() == 8;
                entry.row.setVisibility(0);
                if (wasGone) {
                    this.mStackScroller.generateAddAnimation(entry.row, true);
                }
                visibleNotifications++;
            }
            i++;
        }
        if (onKeyguard && this.mKeyguardIconOverflowContainer.getIconsView().getChildCount() > 0) {
            this.mKeyguardIconOverflowContainer.setVisibility(0);
        } else {
            this.mKeyguardIconOverflowContainer.setVisibility(8);
        }
        this.mStackScroller.changeViewPosition(this.mKeyguardIconOverflowContainer, this.mStackScroller.getChildCount() - 3);
        this.mStackScroller.changeViewPosition(this.mEmptyShadeView, this.mStackScroller.getChildCount() - 2);
        this.mStackScroller.changeViewPosition(this.mDismissView, this.mStackScroller.getChildCount() - 1);
    }

    private boolean shouldShowOnKeyguard(StatusBarNotification sbn) {
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

    private void updateLockscreenNotificationSetting() {
        boolean show = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 1, this.mCurrentUserId) != 0;
        int dpmFlags = this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mCurrentUserId);
        boolean allowedByDpm = (dpmFlags & 4) == 0;
        setShowLockscreenNotifications(show && allowedByDpm);
    }

    public void updateNotification(StatusBarNotification notification, NotificationListenerService.RankingMap ranking) {
        NotificationData.Entry oldEntry;
        if (DEBUG) {
            Log.d("StatusBar", "updateNotification(" + notification + ")");
        }
        String key = notification.getKey();
        boolean wasHeadsUp = isHeadsUp(key);
        if (wasHeadsUp) {
            oldEntry = this.mHeadsUpNotificationView.getEntry();
        } else {
            oldEntry = this.mNotificationData.get(key);
        }
        if (oldEntry != null) {
            StatusBarNotification oldNotification = oldEntry.notification;
            RemoteViews oldContentView = oldNotification.getNotification().contentView;
            Notification n = notification.getNotification();
            RemoteViews contentView = n.contentView;
            RemoteViews oldBigContentView = oldNotification.getNotification().bigContentView;
            RemoteViews bigContentView = n.bigContentView;
            RemoteViews oldHeadsUpContentView = oldNotification.getNotification().headsUpContentView;
            RemoteViews headsUpContentView = n.headsUpContentView;
            Notification oldPublicNotification = oldNotification.getNotification().publicVersion;
            RemoteViews oldPublicContentView = oldPublicNotification != null ? oldPublicNotification.contentView : null;
            Notification publicNotification = n.publicVersion;
            RemoteViews publicContentView = publicNotification != null ? publicNotification.contentView : null;
            if (DEBUG) {
                Log.d("StatusBar", "old notification: when=" + oldNotification.getNotification().when + " ongoing=" + oldNotification.isOngoing() + " expanded=" + oldEntry.expanded + " contentView=" + oldContentView + " bigContentView=" + oldBigContentView + " publicView=" + oldPublicContentView + " rowParent=" + oldEntry.row.getParent());
                Log.d("StatusBar", "new notification: when=" + n.when + " ongoing=" + oldNotification.isOngoing() + " contentView=" + contentView + " bigContentView=" + bigContentView + " publicView=" + publicContentView);
            }
            boolean contentsUnchanged = (oldEntry.expanded == null || contentView.getPackage() == null || oldContentView.getPackage() == null || !oldContentView.getPackage().equals(contentView.getPackage()) || oldContentView.getLayoutId() != contentView.getLayoutId()) ? false : true;
            boolean bigContentsUnchanged = (oldEntry.getBigContentView() == null && bigContentView == null) || !(oldEntry.getBigContentView() == null || bigContentView == null || bigContentView.getPackage() == null || oldBigContentView.getPackage() == null || !oldBigContentView.getPackage().equals(bigContentView.getPackage()) || oldBigContentView.getLayoutId() != bigContentView.getLayoutId());
            boolean headsUpContentsUnchanged = (oldHeadsUpContentView == null && headsUpContentView == null) || !(oldHeadsUpContentView == null || headsUpContentView == null || headsUpContentView.getPackage() == null || oldHeadsUpContentView.getPackage() == null || !oldHeadsUpContentView.getPackage().equals(headsUpContentView.getPackage()) || oldHeadsUpContentView.getLayoutId() != headsUpContentView.getLayoutId());
            boolean publicUnchanged = (oldPublicContentView == null && publicContentView == null) || !(oldPublicContentView == null || publicContentView == null || publicContentView.getPackage() == null || oldPublicContentView.getPackage() == null || !oldPublicContentView.getPackage().equals(publicContentView.getPackage()) || oldPublicContentView.getLayoutId() != publicContentView.getLayoutId());
            boolean updateTicker = (n.tickerText == null || TextUtils.equals(n.tickerText, oldEntry.notification.getNotification().tickerText)) ? false : true;
            boolean shouldInterrupt = shouldInterrupt(notification);
            boolean alertAgain = alertAgain(oldEntry, n);
            boolean updateSuccessful = false;
            if (contentsUnchanged && bigContentsUnchanged && headsUpContentsUnchanged && publicUnchanged) {
                if (DEBUG) {
                    Log.d("StatusBar", "reusing notification for key: " + key);
                }
                oldEntry.notification = notification;
                try {
                    if (oldEntry.icon != null) {
                        StatusBarIcon ic = new StatusBarIcon(notification.getPackageName(), notification.getUser(), n.icon, n.iconLevel, n.number, n.tickerText);
                        oldEntry.icon.setNotification(n);
                        if (!oldEntry.icon.set(ic)) {
                            handleNotificationError(notification, "Couldn't update icon: " + ic);
                            return;
                        }
                    }
                    if (wasHeadsUp) {
                        if (shouldInterrupt) {
                            updateHeadsUpViews(oldEntry, notification);
                            if (alertAgain) {
                                resetHeadsUpDecayTimer();
                            }
                        } else {
                            this.mHeadsUpNotificationView.releaseAndClose();
                            return;
                        }
                    } else if (shouldInterrupt && alertAgain) {
                        removeNotificationViews(key, ranking);
                        addNotification(notification, ranking);
                    } else {
                        updateNotificationViews(oldEntry, notification);
                    }
                    this.mNotificationData.updateRanking(ranking);
                    updateNotifications();
                    updateSuccessful = true;
                } catch (RuntimeException e) {
                    Log.w("StatusBar", "Couldn't reapply views for package " + contentView.getPackage(), e);
                }
            }
            if (!updateSuccessful) {
                if (DEBUG) {
                    Log.d("StatusBar", "not reusing notification for key: " + key);
                }
                if (wasHeadsUp) {
                    if (shouldInterrupt) {
                        if (DEBUG) {
                            Log.d("StatusBar", "rebuilding heads up for key: " + key);
                        }
                        NotificationData.Entry newEntry = new NotificationData.Entry(notification, null);
                        ViewGroup holder = this.mHeadsUpNotificationView.getHolder();
                        if (inflateViewsForHeadsUp(newEntry, holder)) {
                            this.mHeadsUpNotificationView.showNotification(newEntry);
                            if (alertAgain) {
                                resetHeadsUpDecayTimer();
                            }
                        } else {
                            Log.w("StatusBar", "Couldn't create new updated headsup for package " + contentView.getPackage());
                        }
                    } else {
                        if (DEBUG) {
                            Log.d("StatusBar", "releasing heads up for key: " + key);
                        }
                        oldEntry.notification = notification;
                        this.mHeadsUpNotificationView.releaseAndClose();
                        return;
                    }
                } else if (shouldInterrupt && alertAgain) {
                    if (DEBUG) {
                        Log.d("StatusBar", "reposting to invoke heads up for key: " + key);
                    }
                    removeNotificationViews(key, ranking);
                    addNotification(notification, ranking);
                } else {
                    if (DEBUG) {
                        Log.d("StatusBar", "rebuilding update in place for key: " + key);
                    }
                    oldEntry.notification = notification;
                    StatusBarIcon ic2 = new StatusBarIcon(notification.getPackageName(), notification.getUser(), n.icon, n.iconLevel, n.number, n.tickerText);
                    oldEntry.icon.setNotification(n);
                    oldEntry.icon.set(ic2);
                    inflateViews(oldEntry, this.mStackScroller, wasHeadsUp);
                    this.mNotificationData.updateRanking(ranking);
                    updateNotifications();
                }
            }
            updateNotificationVetoButton(oldEntry.row, notification);
            boolean isForCurrentUser = isNotificationForCurrentProfiles(notification);
            if (DEBUG) {
                Log.d("StatusBar", "notification is " + (isForCurrentUser ? "" : "not ") + "for you");
            }
            if (updateTicker && isForCurrentUser) {
                haltTicker();
                tick(notification, false);
            }
            setAreThereNotifications();
            updateExpandedViewPos(-10000);
        }
    }

    private void updateNotificationViews(NotificationData.Entry entry, StatusBarNotification notification) {
        updateNotificationViews(entry, notification, false);
    }

    private void updateHeadsUpViews(NotificationData.Entry entry, StatusBarNotification notification) {
        updateNotificationViews(entry, notification, true);
    }

    private void updateNotificationViews(NotificationData.Entry entry, StatusBarNotification notification, boolean isHeadsUp) {
        RemoteViews contentView = notification.getNotification().contentView;
        RemoteViews bigContentView = isHeadsUp ? notification.getNotification().headsUpContentView : notification.getNotification().bigContentView;
        Notification publicVersion = notification.getNotification().publicVersion;
        RemoteViews publicContentView = publicVersion != null ? publicVersion.contentView : null;
        contentView.reapply(this.mContext, entry.expanded, this.mOnClickHandler);
        if (bigContentView != null && entry.getBigContentView() != null) {
            bigContentView.reapply(this.mContext, entry.getBigContentView(), this.mOnClickHandler);
        }
        if (publicContentView != null && entry.getPublicContentView() != null) {
            publicContentView.reapply(this.mContext, entry.getPublicContentView(), this.mOnClickHandler);
        }
        PendingIntent contentIntent = notification.getNotification().contentIntent;
        if (contentIntent != null) {
            View.OnClickListener listener = makeClicker(contentIntent, notification.getKey(), isHeadsUp);
            entry.row.setOnClickListener(listener);
        } else {
            entry.row.setOnClickListener(null);
        }
        entry.row.setStatusBarNotification(notification);
        entry.row.notifyContentUpdated();
        entry.row.resetHeight();
    }

    protected void notifyHeadsUpScreenOn(boolean screenOn) {
        if (!screenOn) {
            scheduleHeadsUpEscalation();
        }
    }

    private boolean alertAgain(NotificationData.Entry oldEntry, Notification newNotification) {
        return oldEntry == null || !oldEntry.hasInterrupted() || (newNotification.flags & 8) == 0;
    }

    protected boolean shouldInterrupt(StatusBarNotification sbn) {
        if (this.mNotificationData.shouldFilterOut(sbn)) {
            if (!DEBUG) {
                return false;
            }
            Log.d("StatusBar", "Skipping HUN check for " + sbn.getKey() + " since it's filtered out.");
            return false;
        }
        if (this.mHeadsUpNotificationView.isSnoozed(sbn.getPackageName())) {
            return false;
        }
        Notification notification = sbn.getNotification();
        boolean isNoisy = ((notification.defaults & 1) == 0 && (notification.defaults & 2) == 0 && notification.sound == null && notification.vibrate == null) ? false : true;
        boolean isHighPriority = sbn.getScore() >= 10;
        boolean isFullscreen = notification.fullScreenIntent != null;
        boolean hasTicker = this.mHeadsUpTicker && !TextUtils.isEmpty(notification.tickerText);
        boolean isAllowed = notification.extras.getInt("headsup", 1) != 0;
        boolean accessibilityForcesLaunch = isFullscreen && this.mAccessibilityManager.isTouchExplorationEnabled();
        boolean interrupt = (isFullscreen || (isHighPriority && (isNoisy || hasTicker))) && isAllowed && !accessibilityForcesLaunch && this.mPowerManager.isScreenOn() && (!this.mStatusBarKeyguardViewManager.isShowing() || this.mStatusBarKeyguardViewManager.isOccluded()) && !this.mStatusBarKeyguardViewManager.isInputRestricted();
        if (interrupt) {
            try {
                interrupt = !this.mDreamManager.isDreaming();
            } catch (RemoteException e) {
                Log.d("StatusBar", "failed to query dream manager", e);
            }
        }
        if (DEBUG) {
            Log.d("StatusBar", "interrupt: " + interrupt);
        }
        return interrupt;
    }

    public void setInteracting(int barWindow, boolean interacting) {
    }

    public void setBouncerShowing(boolean bouncerShowing) {
        this.mBouncerShowing = bouncerShowing;
    }

    public boolean isBouncerShowing() {
        return this.mBouncerShowing;
    }

    public void destroy() {
        if (this.mSearchPanelView != null) {
            this.mWindowManager.removeViewImmediate(this.mSearchPanelView);
        }
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        try {
            this.mNotificationListener.unregisterAsSystemService();
        } catch (RemoteException e) {
        }
    }

    protected PackageManager getPackageManagerForUser(int userId) {
        Context contextForUser = this.mContext;
        if (userId >= 0) {
            try {
                contextForUser = this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 4, new UserHandle(userId));
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
        return this.mStatusBarKeyguardViewManager.isSecure();
    }
}
