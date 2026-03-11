package com.android.systemui.globalactions;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.IStopUserCallback;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.colorextraction.drawable.GradientDrawable;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.EmergencyAffordanceManager;
import com.android.internal.util.ScreenshotHelper;
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.Dependency;
import com.android.systemui.HardwareUiLayout;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.globalactions.GlobalActionsDialog;
import com.android.systemui.plugins.GlobalActions;
import com.android.systemui.volume.SystemUIInterpolators;
import java.util.ArrayList;
import java.util.List;

class GlobalActionsDialog implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private MyAdapter mAdapter;
    private ToggleAction mAirplaneModeOn;
    private final AudioManager mAudioManager;
    private final Context mContext;
    private final DevicePolicyManager mDevicePolicyManager;
    private ActionsDialog mDialog;
    private final EmergencyAffordanceManager mEmergencyAffordanceManager;
    private boolean mHasLockdownButton;
    private boolean mHasLogoutButton;
    private boolean mHasTelephony;
    private boolean mHasVibrator;
    private ArrayList<Action> mItems;
    private final KeyguardManager mKeyguardManager;
    private final LockPatternUtils mLockPatternUtils;
    private final ScreenshotHelper mScreenshotHelper;
    private final boolean mShowSilentToggle;
    private Action mSilentModeAction;
    private final GlobalActions.GlobalActionsManager mWindowManagerFuncs;
    private boolean mKeyguardShowing = false;
    private boolean mDeviceProvisioned = false;
    private ToggleAction.State mAirplaneState = ToggleAction.State.Off;
    private boolean mIsWaitingForEcmExit = false;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action) || "android.intent.action.SCREEN_OFF".equals(action)) {
                String stringExtra = intent.getStringExtra("reason");
                if (!"globalactions".equals(stringExtra)) {
                    GlobalActionsDialog.this.mHandler.sendMessage(GlobalActionsDialog.this.mHandler.obtainMessage(0, stringExtra));
                    return;
                }
                return;
            }
            if ("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED".equals(action) && !intent.getBooleanExtra("PHONE_IN_ECM_STATE", false) && GlobalActionsDialog.this.mIsWaitingForEcmExit) {
                GlobalActionsDialog.this.mIsWaitingForEcmExit = false;
                GlobalActionsDialog.this.changeAirplaneModeSystemSetting(true);
            }
        }
    };
    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            if (GlobalActionsDialog.this.mHasTelephony) {
                boolean z = serviceState.getState() == 3;
                GlobalActionsDialog.this.mAirplaneState = z ? ToggleAction.State.On : ToggleAction.State.Off;
                GlobalActionsDialog.this.mAirplaneModeOn.updateState(GlobalActionsDialog.this.mAirplaneState);
                GlobalActionsDialog.this.mAdapter.notifyDataSetChanged();
            }
        }
    };
    private BroadcastReceiver mRingerModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.media.RINGER_MODE_CHANGED")) {
                GlobalActionsDialog.this.mHandler.sendEmptyMessage(1);
            }
        }
    };
    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z) {
            GlobalActionsDialog.this.onAirplaneModeChanged();
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    if (GlobalActionsDialog.this.mDialog != null) {
                        if ("dream".equals(message.obj)) {
                            GlobalActionsDialog.this.mDialog.dismissImmediately();
                        } else {
                            GlobalActionsDialog.this.mDialog.dismiss();
                        }
                        GlobalActionsDialog.this.mDialog = null;
                    }
                    break;
                case 1:
                    GlobalActionsDialog.this.refreshSilentMode();
                    GlobalActionsDialog.this.mAdapter.notifyDataSetChanged();
                    break;
                case 2:
                    GlobalActionsDialog.this.handleShow();
                    break;
            }
        }
    };
    private final IDreamManager mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"));

    interface Action {
        View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater);

        boolean isEnabled();

        void onPress();

        boolean showBeforeProvisioning();

        boolean showDuringKeyguard();
    }

    interface LongPressAction extends Action {
        boolean onLongPress();
    }

    public GlobalActionsDialog(Context context, GlobalActions.GlobalActionsManager globalActionsManager) {
        boolean z = false;
        this.mContext = new ContextThemeWrapper(context, R.style.qs_theme);
        this.mWindowManagerFuncs = globalActionsManager;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mHasTelephony = ((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0);
        ((TelephonyManager) context.getSystemService("phone")).listen(this.mPhoneStateListener, 1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        if (vibrator != null && vibrator.hasVibrator()) {
            z = true;
        }
        this.mHasVibrator = z;
        this.mShowSilentToggle = !this.mContext.getResources().getBoolean(android.R.^attr-private.pointerIconVectorFillInverse);
        this.mEmergencyAffordanceManager = new EmergencyAffordanceManager(context);
        this.mScreenshotHelper = new ScreenshotHelper(context);
    }

    public void showDialog(boolean z, boolean z2) {
        this.mKeyguardShowing = z;
        this.mDeviceProvisioned = z2;
        if (this.mDialog != null) {
            this.mDialog.dismiss();
            this.mDialog = null;
            this.mHandler.sendEmptyMessage(2);
            return;
        }
        handleShow();
    }

    public void dismissDialog() {
        this.mHandler.removeMessages(0);
        this.mHandler.sendEmptyMessage(0);
    }

    private void awakenIfNecessary() {
        if (this.mDreamManager != null) {
            try {
                if (this.mDreamManager.isDreaming()) {
                    this.mDreamManager.awaken();
                }
            } catch (RemoteException e) {
            }
        }
    }

    public void handleShow() {
        awakenIfNecessary();
        this.mDialog = createDialog();
        prepareDialog();
        if (this.mAdapter.getCount() == 1 && (this.mAdapter.getItem(0) instanceof SinglePressAction) && !(this.mAdapter.getItem(0) instanceof LongPressAction)) {
            ((SinglePressAction) this.mAdapter.getItem(0)).onPress();
            return;
        }
        WindowManager.LayoutParams attributes = this.mDialog.getWindow().getAttributes();
        attributes.setTitle("ActionsDialog");
        attributes.layoutInDisplayCutoutMode = 1;
        this.mDialog.getWindow().setAttributes(attributes);
        this.mDialog.show();
        this.mWindowManagerFuncs.onGlobalActionsShown();
    }

    private ActionsDialog createDialog() {
        if (!this.mHasVibrator) {
            this.mSilentModeAction = new SilentModeToggleAction();
        } else {
            this.mSilentModeAction = new SilentModeTriStateAction(this.mContext, this.mAudioManager, this.mHandler);
        }
        this.mAirplaneModeOn = new ToggleAction(android.R.drawable.grid_selector_background_focus, android.R.drawable.highlight_disabled, android.R.string.config_defaultTrustAgent, android.R.string.config_defaultTranslationService, android.R.string.config_defaultTextClassifierPackage) {
            @Override
            void onToggle(boolean z) {
                if (!GlobalActionsDialog.this.mHasTelephony || !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                    GlobalActionsDialog.this.changeAirplaneModeSystemSetting(z);
                    return;
                }
                GlobalActionsDialog.this.mIsWaitingForEcmExit = true;
                Intent intent = new Intent("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", (Uri) null);
                intent.addFlags(268435456);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }

            @Override
            protected void changeStateFromPress(boolean z) {
                if (GlobalActionsDialog.this.mHasTelephony && !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                    this.mState = z ? ToggleAction.State.TurningOn : ToggleAction.State.TurningOff;
                    GlobalActionsDialog.this.mAirplaneState = this.mState;
                }
            }

            @Override
            public boolean showDuringKeyguard() {
                return true;
            }

            @Override
            public boolean showBeforeProvisioning() {
                return false;
            }
        };
        onAirplaneModeChanged();
        this.mItems = new ArrayList<>();
        String[] stringArray = this.mContext.getResources().getStringArray(android.R.array.config_brightnessThresholdsOfPeakRefreshRate);
        ArraySet arraySet = new ArraySet();
        this.mHasLogoutButton = false;
        this.mHasLockdownButton = false;
        int i = 0;
        while (true) {
            if (i >= stringArray.length) {
                break;
            }
            String str = stringArray[i];
            if (!arraySet.contains(str)) {
                if ("power".equals(str)) {
                    this.mItems.add(new PowerAction());
                } else if ("airplane".equals(str)) {
                    this.mItems.add(this.mAirplaneModeOn);
                } else if ("bugreport".equals(str)) {
                    if (Settings.Global.getInt(this.mContext.getContentResolver(), "bugreport_in_power_menu", 0) != 0 && isCurrentUserOwner()) {
                        this.mItems.add(new BugReportAction());
                    }
                } else if ("silent".equals(str)) {
                    if (this.mShowSilentToggle) {
                        this.mItems.add(this.mSilentModeAction);
                    }
                } else if ("users".equals(str)) {
                    if (SystemProperties.getBoolean("fw.power_user_switcher", false)) {
                        addUsersToMenu(this.mItems);
                    }
                } else if ("settings".equals(str)) {
                    this.mItems.add(getSettingsAction());
                } else if ("lockdown".equals(str)) {
                    if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lockdown_in_power_menu", 0, getCurrentUser().id) != 0 && shouldDisplayLockdown()) {
                        this.mItems.add(getLockdownAction());
                        this.mHasLockdownButton = true;
                    }
                } else if ("voiceassist".equals(str)) {
                    this.mItems.add(getVoiceAssistAction());
                } else if ("assist".equals(str)) {
                    this.mItems.add(getAssistAction());
                } else if ("restart".equals(str)) {
                    this.mItems.add(new RestartAction());
                } else if ("screenshot".equals(str)) {
                    this.mItems.add(new ScreenshotAction());
                } else if ("logout".equals(str)) {
                    if (this.mDevicePolicyManager.isLogoutEnabled() && getCurrentUser().id != 0) {
                        this.mItems.add(new LogoutAction());
                        this.mHasLogoutButton = true;
                    }
                } else {
                    Log.e("GlobalActionsDialog", "Invalid global action key " + str);
                }
                arraySet.add(str);
            }
            i++;
        }
        if (this.mEmergencyAffordanceManager.needsEmergencyAffordance()) {
            this.mItems.add(getEmergencyAction());
        }
        this.mAdapter = new MyAdapter();
        ActionsDialog actionsDialog = new ActionsDialog(this.mContext, this, this.mAdapter, new AdapterView.OnItemLongClickListener() {
            @Override
            public final boolean onItemLongClick(AdapterView adapterView, View view, int i2, long j) {
                return GlobalActionsDialog.lambda$createDialog$0(this.f$0, adapterView, view, i2, j);
            }
        });
        actionsDialog.setCanceledOnTouchOutside(false);
        actionsDialog.setKeyguardShowing(this.mKeyguardShowing);
        actionsDialog.setOnDismissListener(this);
        return actionsDialog;
    }

    public static boolean lambda$createDialog$0(GlobalActionsDialog globalActionsDialog, AdapterView adapterView, View view, int i, long j) {
        Action item = globalActionsDialog.mAdapter.getItem(i);
        if (item instanceof LongPressAction) {
            globalActionsDialog.mDialog.dismiss();
            return ((LongPressAction) item).onLongPress();
        }
        return false;
    }

    private boolean shouldDisplayLockdown() {
        int i = getCurrentUser().id;
        if (!this.mKeyguardManager.isDeviceSecure(i)) {
            return false;
        }
        int strongAuthForUser = this.mLockPatternUtils.getStrongAuthForUser(i);
        return strongAuthForUser == 0 || strongAuthForUser == 4;
    }

    private final class PowerAction extends SinglePressAction implements LongPressAction {
        private PowerAction() {
            super(android.R.drawable.ic_lock_power_off, android.R.string.config_defaultProfcollectReportUploaderApp);
        }

        @Override
        public boolean onLongPress() {
            if (!((UserManager) GlobalActionsDialog.this.mContext.getSystemService("user")).hasUserRestriction("no_safe_boot")) {
                GlobalActionsDialog.this.mWindowManagerFuncs.reboot(true);
                return true;
            }
            return false;
        }

        @Override
        public boolean showDuringKeyguard() {
            return true;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return true;
        }

        @Override
        public void onPress() {
            GlobalActionsDialog.this.mWindowManagerFuncs.shutdown();
        }
    }

    private final class RestartAction extends SinglePressAction implements LongPressAction {
        private RestartAction() {
            super(android.R.drawable.ic_media_route_connected_light_08_mtrl, android.R.string.config_defaultQrCodeComponent);
        }

        @Override
        public boolean onLongPress() {
            if (!((UserManager) GlobalActionsDialog.this.mContext.getSystemService("user")).hasUserRestriction("no_safe_boot")) {
                GlobalActionsDialog.this.mWindowManagerFuncs.reboot(true);
                return true;
            }
            return false;
        }

        @Override
        public boolean showDuringKeyguard() {
            return true;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return true;
        }

        @Override
        public void onPress() {
            GlobalActionsDialog.this.mWindowManagerFuncs.reboot(false);
        }
    }

    private class ScreenshotAction extends SinglePressAction {
        public ScreenshotAction() {
            super(android.R.drawable.ic_media_route_connected_light_10_mtrl, android.R.string.config_defaultRotationResolverService);
        }

        @Override
        public void onPress() {
            GlobalActionsDialog.this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    GlobalActionsDialog.this.mScreenshotHelper.takeScreenshot(1, true, true, GlobalActionsDialog.this.mHandler);
                    MetricsLogger.action(GlobalActionsDialog.this.mContext, 1282);
                }
            }, 500L);
        }

        @Override
        public boolean showDuringKeyguard() {
            return true;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return false;
        }
    }

    private class BugReportAction extends SinglePressAction implements LongPressAction {
        public BugReportAction() {
            super(android.R.drawable.highlight_selected, android.R.string.accessibility_autoclick_scroll_up);
        }

        @Override
        public void onPress() {
            if (!ActivityManager.isUserAMonkey()) {
                GlobalActionsDialog.this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MetricsLogger.action(GlobalActionsDialog.this.mContext, 292);
                            ActivityManager.getService().requestBugReport(1);
                        } catch (RemoteException e) {
                        }
                    }
                }, 500L);
            }
        }

        @Override
        public boolean onLongPress() {
            if (ActivityManager.isUserAMonkey()) {
                return false;
            }
            try {
                MetricsLogger.action(GlobalActionsDialog.this.mContext, 293);
                ActivityManager.getService().requestBugReport(0);
            } catch (RemoteException e) {
            }
            return false;
        }

        @Override
        public boolean showDuringKeyguard() {
            return true;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return false;
        }

        @Override
        public String getStatus() {
            return GlobalActionsDialog.this.mContext.getString(android.R.string.accessibility_autoclick_scroll_right, Build.VERSION.RELEASE, Build.ID);
        }
    }

    final class LogoutAction extends SinglePressAction {
        private LogoutAction() {
            super(android.R.drawable.ic_audio_ring_notif_vibrate, android.R.string.config_defaultProfcollectReportUploaderAction);
        }

        @Override
        public boolean showDuringKeyguard() {
            return true;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return false;
        }

        @Override
        public void onPress() {
            GlobalActionsDialog.this.mHandler.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    GlobalActionsDialog.LogoutAction.lambda$onPress$0(this.f$0);
                }
            }, 500L);
        }

        public static void lambda$onPress$0(LogoutAction logoutAction) {
            try {
                int i = GlobalActionsDialog.this.getCurrentUser().id;
                ActivityManager.getService().switchUser(0);
                ActivityManager.getService().stopUser(i, true, (IStopUserCallback) null);
            } catch (RemoteException e) {
                Log.e("GlobalActionsDialog", "Couldn't logout user " + e);
            }
        }
    }

    private Action getSettingsAction() {
        return new SinglePressAction(android.R.drawable.ic_media_route_connected_light_16_mtrl, android.R.string.config_defaultSearchSelectorPackageName) {
            @Override
            public void onPress() {
                if (BenesseExtension.getDchaState() != 0) {
                    return;
                }
                Intent intent = new Intent("android.settings.SETTINGS");
                intent.addFlags(335544320);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }

            @Override
            public boolean showDuringKeyguard() {
                return true;
            }

            @Override
            public boolean showBeforeProvisioning() {
                return true;
            }
        };
    }

    private Action getEmergencyAction() {
        return new SinglePressAction(android.R.drawable.btn_toggle_off_focused_holo_dark, android.R.string.config_defaultNetworkRecommendationProviderPackage) {
            @Override
            public void onPress() {
                GlobalActionsDialog.this.mEmergencyAffordanceManager.performEmergencyCall();
            }

            @Override
            public boolean showDuringKeyguard() {
                return true;
            }

            @Override
            public boolean showBeforeProvisioning() {
                return true;
            }
        };
    }

    private Action getAssistAction() {
        return new SinglePressAction(android.R.drawable.control_background_32dp_material, android.R.string.config_defaultMusicRecognitionService) {
            @Override
            public void onPress() {
                Intent intent = new Intent("android.intent.action.ASSIST");
                intent.addFlags(335544320);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }

            @Override
            public boolean showDuringKeyguard() {
                return true;
            }

            @Override
            public boolean showBeforeProvisioning() {
                return true;
            }
        };
    }

    private Action getVoiceAssistAction() {
        return new SinglePressAction(android.R.drawable.ic_media_route_connecting_dark_16_mtrl, android.R.string.config_defaultSupervisionProfileOwnerComponent) {
            @Override
            public void onPress() {
                Intent intent = new Intent("android.intent.action.VOICE_ASSIST");
                intent.addFlags(335544320);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }

            @Override
            public boolean showDuringKeyguard() {
                return true;
            }

            @Override
            public boolean showBeforeProvisioning() {
                return true;
            }
        };
    }

    class AnonymousClass6 extends SinglePressAction {
        AnonymousClass6(int i, int i2) {
            super(i, i2);
        }

        @Override
        public void onPress() {
            new LockPatternUtils(GlobalActionsDialog.this.mContext).requireStrongAuth(32, -1);
            try {
                WindowManagerGlobal.getWindowManagerService().lockNow((Bundle) null);
                new Handler((Looper) Dependency.get(Dependency.BG_LOOPER)).post(new Runnable() {
                    @Override
                    public final void run() {
                        GlobalActionsDialog.this.lockProfiles();
                    }
                });
            } catch (RemoteException e) {
                Log.e("GlobalActionsDialog", "Error while trying to lock device.", e);
            }
        }

        @Override
        public boolean showDuringKeyguard() {
            return true;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return false;
        }
    }

    private Action getLockdownAction() {
        return new AnonymousClass6(android.R.drawable.ic_ab_back_holo_light, android.R.string.config_defaultOnDeviceSpeechRecognitionService);
    }

    public void lockProfiles() {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        TrustManager trustManager = (TrustManager) this.mContext.getSystemService("trust");
        int i = getCurrentUser().id;
        for (int i2 : userManager.getEnabledProfileIds(i)) {
            if (i2 != i) {
                trustManager.setDeviceLockedForUser(i2, true);
            }
        }
    }

    public UserInfo getCurrentUser() {
        try {
            return ActivityManager.getService().getCurrentUser();
        } catch (RemoteException e) {
            return null;
        }
    }

    private boolean isCurrentUserOwner() {
        UserInfo currentUser = getCurrentUser();
        return currentUser == null || currentUser.isPrimary();
    }

    private void addUsersToMenu(ArrayList<Action> arrayList) {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        if (userManager.isUserSwitcherEnabled()) {
            List<UserInfo> users = userManager.getUsers();
            UserInfo currentUser = getCurrentUser();
            for (final UserInfo userInfo : users) {
                if (userInfo.supportsSwitchToByUser()) {
                    boolean z = false;
                    if (currentUser != null ? currentUser.id == userInfo.id : userInfo.id == 0) {
                        z = true;
                    }
                    Drawable drawableCreateFromPath = userInfo.iconPath != null ? Drawable.createFromPath(userInfo.iconPath) : null;
                    int i = android.R.drawable.ic_jog_dial_vibrate_on;
                    StringBuilder sb = new StringBuilder();
                    sb.append(userInfo.name != null ? userInfo.name : "Primary");
                    sb.append(z ? " ✔" : "");
                    arrayList.add(new SinglePressAction(i, drawableCreateFromPath, sb.toString()) {
                        @Override
                        public void onPress() {
                            try {
                                ActivityManager.getService().switchUser(userInfo.id);
                            } catch (RemoteException e) {
                                Log.e("GlobalActionsDialog", "Couldn't switch user " + e);
                            }
                        }

                        @Override
                        public boolean showDuringKeyguard() {
                            return true;
                        }

                        @Override
                        public boolean showBeforeProvisioning() {
                            return false;
                        }
                    });
                }
            }
        }
    }

    private void prepareDialog() {
        refreshSilentMode();
        this.mAirplaneModeOn.updateState(this.mAirplaneState);
        this.mAdapter.notifyDataSetChanged();
        if (this.mShowSilentToggle) {
            this.mContext.registerReceiver(this.mRingerModeReceiver, new IntentFilter("android.media.RINGER_MODE_CHANGED"));
        }
    }

    public void refreshSilentMode() {
        if (!this.mHasVibrator) {
            ((ToggleAction) this.mSilentModeAction).updateState(this.mAudioManager.getRingerMode() != 2 ? ToggleAction.State.On : ToggleAction.State.Off);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        this.mWindowManagerFuncs.onGlobalActionsHidden();
        if (this.mShowSilentToggle) {
            try {
                this.mContext.unregisterReceiver(this.mRingerModeReceiver);
            } catch (IllegalArgumentException e) {
                Log.w("GlobalActionsDialog", e);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        Action item = this.mAdapter.getItem(i);
        if (!(item instanceof SilentModeTriStateAction)) {
            dialogInterface.dismiss();
        }
        item.onPress();
    }

    class MyAdapter extends BaseAdapter {
        private MyAdapter() {
        }

        @Override
        public int getCount() {
            int i = 0;
            for (int i2 = 0; i2 < GlobalActionsDialog.this.mItems.size(); i2++) {
                Action action = (Action) GlobalActionsDialog.this.mItems.get(i2);
                if ((!GlobalActionsDialog.this.mKeyguardShowing || action.showDuringKeyguard()) && (GlobalActionsDialog.this.mDeviceProvisioned || action.showBeforeProvisioning())) {
                    i++;
                }
            }
            return i;
        }

        @Override
        public boolean isEnabled(int i) {
            return getItem(i).isEnabled();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public Action getItem(int i) {
            int i2 = 0;
            for (int i3 = 0; i3 < GlobalActionsDialog.this.mItems.size(); i3++) {
                Action action = (Action) GlobalActionsDialog.this.mItems.get(i3);
                if ((!GlobalActionsDialog.this.mKeyguardShowing || action.showDuringKeyguard()) && (GlobalActionsDialog.this.mDeviceProvisioned || action.showBeforeProvisioning())) {
                    if (i2 == i) {
                        return action;
                    }
                    i2++;
                }
            }
            throw new IllegalArgumentException("position " + i + " out of range of showable actions, filtered count=" + getCount() + ", keyguardshowing=" + GlobalActionsDialog.this.mKeyguardShowing + ", provisioned=" + GlobalActionsDialog.this.mDeviceProvisioned);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View viewCreate = getItem(i).create(GlobalActionsDialog.this.mContext, view, viewGroup, LayoutInflater.from(GlobalActionsDialog.this.mContext));
            if (i == getCount() - 1) {
                HardwareUiLayout.get(viewGroup).setDivisionView(viewCreate);
            }
            return viewCreate;
        }
    }

    private static abstract class SinglePressAction implements Action {
        private final Drawable mIcon;
        private final int mIconResId;
        private final CharSequence mMessage;
        private final int mMessageResId;

        @Override
        public abstract void onPress();

        protected SinglePressAction(int i, int i2) {
            this.mIconResId = i;
            this.mMessageResId = i2;
            this.mMessage = null;
            this.mIcon = null;
        }

        protected SinglePressAction(int i, Drawable drawable, CharSequence charSequence) {
            this.mIconResId = i;
            this.mMessageResId = 0;
            this.mMessage = charSequence;
            this.mIcon = drawable;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        public String getStatus() {
            return null;
        }

        @Override
        public View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater) {
            View viewInflate = layoutInflater.inflate(R.layout.global_actions_item, viewGroup, false);
            ImageView imageView = (ImageView) viewInflate.findViewById(android.R.id.icon);
            TextView textView = (TextView) viewInflate.findViewById(android.R.id.message);
            TextView textView2 = (TextView) viewInflate.findViewById(android.R.id.nosensor);
            String status = getStatus();
            if (!TextUtils.isEmpty(status)) {
                textView2.setText(status);
            } else {
                textView2.setVisibility(8);
            }
            if (this.mIcon != null) {
                imageView.setImageDrawable(this.mIcon);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else if (this.mIconResId != 0) {
                imageView.setImageDrawable(context.getDrawable(this.mIconResId));
            }
            if (this.mMessage != null) {
                textView.setText(this.mMessage);
            } else {
                textView.setText(this.mMessageResId);
            }
            return viewInflate;
        }
    }

    private static abstract class ToggleAction implements Action {
        protected int mDisabledIconResid;
        protected int mDisabledStatusMessageResId;
        protected int mEnabledIconResId;
        protected int mEnabledStatusMessageResId;
        protected int mMessageResId;
        protected State mState = State.Off;

        abstract void onToggle(boolean z);

        enum State {
            Off(false),
            TurningOn(true),
            TurningOff(true),
            On(false);

            private final boolean inTransition;

            State(boolean z) {
                this.inTransition = z;
            }

            public boolean inTransition() {
                return this.inTransition;
            }
        }

        public ToggleAction(int i, int i2, int i3, int i4, int i5) {
            this.mEnabledIconResId = i;
            this.mDisabledIconResid = i2;
            this.mMessageResId = i3;
            this.mEnabledStatusMessageResId = i4;
            this.mDisabledStatusMessageResId = i5;
        }

        void willCreate() {
        }

        @Override
        public View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater) {
            boolean z;
            willCreate();
            View viewInflate = layoutInflater.inflate(android.R.layout.conversation_face_pile_layout, viewGroup, false);
            ImageView imageView = (ImageView) viewInflate.findViewById(android.R.id.icon);
            TextView textView = (TextView) viewInflate.findViewById(android.R.id.message);
            TextView textView2 = (TextView) viewInflate.findViewById(android.R.id.nosensor);
            boolean zIsEnabled = isEnabled();
            if (textView != null) {
                textView.setText(this.mMessageResId);
                textView.setEnabled(zIsEnabled);
            }
            if (this.mState == State.On || this.mState == State.TurningOn) {
                z = true;
            } else {
                z = false;
            }
            if (imageView != null) {
                imageView.setImageDrawable(context.getDrawable(z ? this.mEnabledIconResId : this.mDisabledIconResid));
                imageView.setEnabled(zIsEnabled);
            }
            if (textView2 != null) {
                textView2.setText(z ? this.mEnabledStatusMessageResId : this.mDisabledStatusMessageResId);
                textView2.setVisibility(0);
                textView2.setEnabled(zIsEnabled);
            }
            viewInflate.setEnabled(zIsEnabled);
            return viewInflate;
        }

        @Override
        public final void onPress() {
            if (this.mState.inTransition()) {
                Log.w("GlobalActionsDialog", "shouldn't be able to toggle when in transition");
                return;
            }
            boolean z = this.mState != State.On;
            onToggle(z);
            changeStateFromPress(z);
        }

        @Override
        public boolean isEnabled() {
            return !this.mState.inTransition();
        }

        protected void changeStateFromPress(boolean z) {
            this.mState = z ? State.On : State.Off;
        }

        public void updateState(State state) {
            this.mState = state;
        }
    }

    private class SilentModeToggleAction extends ToggleAction {
        public SilentModeToggleAction() {
            super(android.R.drawable.dialog_divider_horizontal_holo_light, android.R.drawable.dialog_divider_horizontal_holo_dark, android.R.string.config_defaultSmartspaceService, android.R.string.config_defaultSelectToSpeakService, android.R.string.config_defaultSearchUiService);
        }

        @Override
        void onToggle(boolean z) {
            if (z) {
                GlobalActionsDialog.this.mAudioManager.setRingerMode(0);
            } else {
                GlobalActionsDialog.this.mAudioManager.setRingerMode(2);
            }
        }

        @Override
        public boolean showDuringKeyguard() {
            return true;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return false;
        }
    }

    private static class SilentModeTriStateAction implements View.OnClickListener, Action {
        private final int[] ITEM_IDS = {android.R.id.headers, android.R.id.health, android.R.id.help};
        private final AudioManager mAudioManager;
        private final Context mContext;
        private final Handler mHandler;

        SilentModeTriStateAction(Context context, AudioManager audioManager, Handler handler) {
            this.mAudioManager = audioManager;
            this.mHandler = handler;
            this.mContext = context;
        }

        private int ringerModeToIndex(int i) {
            return i;
        }

        private int indexToRingerMode(int i) {
            return i;
        }

        @Override
        public View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater) {
            View viewInflate = layoutInflater.inflate(android.R.layout.date_picker_dialog, viewGroup, false);
            int iRingerModeToIndex = ringerModeToIndex(this.mAudioManager.getRingerMode());
            int i = 0;
            while (i < 3) {
                View viewFindViewById = viewInflate.findViewById(this.ITEM_IDS[i]);
                viewFindViewById.setSelected(iRingerModeToIndex == i);
                viewFindViewById.setTag(Integer.valueOf(i));
                viewFindViewById.setOnClickListener(this);
                i++;
            }
            return viewInflate;
        }

        @Override
        public void onPress() {
        }

        @Override
        public boolean showDuringKeyguard() {
            return true;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void onClick(View view) {
            if (view.getTag() instanceof Integer) {
                this.mAudioManager.setRingerMode(indexToRingerMode(((Integer) view.getTag()).intValue()));
                this.mHandler.sendEmptyMessageDelayed(0, 300L);
            }
        }
    }

    public void onAirplaneModeChanged() {
        if (this.mHasTelephony) {
            return;
        }
        this.mAirplaneState = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1 ? ToggleAction.State.On : ToggleAction.State.Off;
        this.mAirplaneModeOn.updateState(this.mAirplaneState);
    }

    public void changeAirplaneModeSystemSetting(boolean z) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", z ? 1 : 0);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.addFlags(536870912);
        intent.putExtra("state", z);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        if (!this.mHasTelephony) {
            this.mAirplaneState = z ? ToggleAction.State.On : ToggleAction.State.Off;
        }
    }

    static final class ActionsDialog extends Dialog implements DialogInterface, ColorExtractor.OnColorsChangedListener {
        private final MyAdapter mAdapter;
        private final DialogInterface.OnClickListener mClickListener;
        private final ColorExtractor mColorExtractor;
        private final Context mContext;
        private final GradientDrawable mGradientDrawable;
        private final HardwareUiLayout mHardwareLayout;
        private boolean mKeyguardShowing;
        private final LinearLayout mListView;
        private final AdapterView.OnItemLongClickListener mLongClickListener;

        public ActionsDialog(Context context, DialogInterface.OnClickListener onClickListener, MyAdapter myAdapter, AdapterView.OnItemLongClickListener onItemLongClickListener) {
            super(context, 2131886645);
            this.mContext = context;
            this.mAdapter = myAdapter;
            this.mClickListener = onClickListener;
            this.mLongClickListener = onItemLongClickListener;
            this.mGradientDrawable = new GradientDrawable(this.mContext);
            this.mColorExtractor = (ColorExtractor) Dependency.get(SysuiColorExtractor.class);
            Window window = getWindow();
            window.requestFeature(1);
            window.getDecorView();
            window.getAttributes().systemUiVisibility |= 1792;
            window.setLayout(-1, -1);
            window.clearFlags(2);
            window.addFlags(17629472);
            window.setBackgroundDrawable(this.mGradientDrawable);
            window.setType(2020);
            setContentView(R.layout.global_actions_wrapped);
            this.mListView = (LinearLayout) findViewById(android.R.id.list);
            this.mHardwareLayout = HardwareUiLayout.get(this.mListView);
            this.mHardwareLayout.setOutsideTouchListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    this.f$0.dismiss();
                }
            });
            setTitle(android.R.string.config_defaultSystemCaptionsManagerService);
            this.mListView.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override
                public boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                    accessibilityEvent.getText().add(ActionsDialog.this.mContext.getString(android.R.string.config_defaultSystemCaptionsManagerService));
                    return true;
                }
            });
        }

        private void updateList() {
            this.mListView.removeAllViews();
            for (final int i = 0; i < this.mAdapter.getCount(); i++) {
                final View view = this.mAdapter.getView(i, null, this.mListView);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view2) {
                        GlobalActionsDialog.ActionsDialog actionsDialog = this.f$0;
                        actionsDialog.mClickListener.onClick(actionsDialog, i);
                    }
                });
                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public final boolean onLongClick(View view2) {
                        return this.f$0.mLongClickListener.onItemLongClick(null, view, i, 0L);
                    }
                });
                this.mListView.addView(view);
            }
        }

        @Override
        protected void onStart() {
            int i = 1;
            super.setCanceledOnTouchOutside(true);
            super.onStart();
            updateList();
            Point point = new Point();
            this.mContext.getDisplay().getRealSize(point);
            this.mColorExtractor.addOnColorsChangedListener(this);
            this.mGradientDrawable.setScreenSize(point.x, point.y);
            ColorExtractor colorExtractor = this.mColorExtractor;
            if (this.mKeyguardShowing) {
                i = 2;
            }
            updateColors(colorExtractor.getColors(i), false);
        }

        private void updateColors(ColorExtractor.GradientColors gradientColors, boolean z) {
            this.mGradientDrawable.setColors(gradientColors, z);
            View decorView = getWindow().getDecorView();
            if (gradientColors.supportsDarkText()) {
                decorView.setSystemUiVisibility(8208);
            } else {
                decorView.setSystemUiVisibility(0);
            }
        }

        @Override
        protected void onStop() {
            super.onStop();
            this.mColorExtractor.removeOnColorsChangedListener(this);
        }

        @Override
        public void show() {
            super.show();
            this.mGradientDrawable.setAlpha(0);
            this.mHardwareLayout.setTranslationX(getAnimTranslation());
            this.mHardwareLayout.setAlpha(0.0f);
            this.mHardwareLayout.animate().alpha(1.0f).translationX(0.0f).setDuration(300L).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    this.f$0.mGradientDrawable.setAlpha((int) (((Float) valueAnimator.getAnimatedValue()).floatValue() * 0.45f * 255.0f));
                }
            }).withEndAction(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.getWindow().getDecorView().requestAccessibilityFocus();
                }
            }).start();
        }

        @Override
        public void dismiss() {
            this.mHardwareLayout.setTranslationX(0.0f);
            this.mHardwareLayout.setAlpha(1.0f);
            this.mHardwareLayout.animate().alpha(0.0f).translationX(getAnimTranslation()).setDuration(300L).withEndAction(new Runnable() {
                @Override
                public final void run() {
                    super/*android.app.Dialog*/.dismiss();
                }
            }).setInterpolator(new SystemUIInterpolators.LogAccelerateInterpolator()).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    this.f$0.mGradientDrawable.setAlpha((int) ((1.0f - ((Float) valueAnimator.getAnimatedValue()).floatValue()) * 0.45f * 255.0f));
                }
            }).start();
        }

        void dismissImmediately() {
            super.dismiss();
        }

        private float getAnimTranslation() {
            return getContext().getResources().getDimension(R.dimen.global_actions_panel_width) / 2.0f;
        }

        public void onColorsChanged(ColorExtractor colorExtractor, int i) {
            if (this.mKeyguardShowing) {
                if ((i & 2) != 0) {
                    updateColors(colorExtractor.getColors(2), true);
                }
            } else if ((i & 1) != 0) {
                updateColors(colorExtractor.getColors(1), true);
            }
        }

        public void setKeyguardShowing(boolean z) {
            this.mKeyguardShowing = z;
        }
    }
}
