package com.android.server.policy;

import android.R;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.AdapterView;
import com.android.internal.app.AlertController;
import com.android.internal.globalactions.Action;
import com.android.internal.globalactions.ActionsAdapter;
import com.android.internal.globalactions.ActionsDialog;
import com.android.internal.globalactions.LongPressAction;
import com.android.internal.globalactions.SinglePressAction;
import com.android.internal.globalactions.ToggleAction;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.EmergencyAffordanceManager;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.audio.AudioService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.policy.WindowManagerPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

class LegacyGlobalActions implements DialogInterface.OnDismissListener, DialogInterface.OnClickListener {
    private static final int DIALOG_DISMISS_DELAY = 300;
    private static final String GLOBAL_ACTION_KEY_AIRPLANE = "airplane";
    private static final String GLOBAL_ACTION_KEY_ASSIST = "assist";
    private static final String GLOBAL_ACTION_KEY_BUGREPORT = "bugreport";
    private static final String GLOBAL_ACTION_KEY_LOCKDOWN = "lockdown";
    private static final String GLOBAL_ACTION_KEY_POWER = "power";
    private static final String GLOBAL_ACTION_KEY_RESTART = "restart";
    private static final String GLOBAL_ACTION_KEY_SETTINGS = "settings";
    private static final String GLOBAL_ACTION_KEY_SILENT = "silent";
    private static final String GLOBAL_ACTION_KEY_USERS = "users";
    private static final String GLOBAL_ACTION_KEY_VOICEASSIST = "voiceassist";
    private static final int MESSAGE_DISMISS = 0;
    private static final int MESSAGE_REFRESH = 1;
    private static final int MESSAGE_SHOW = 2;
    private static final boolean SHOW_SILENT_TOGGLE = true;
    private static final String TAG = "LegacyGlobalActions";
    private ActionsAdapter mAdapter;
    private ToggleAction mAirplaneModeOn;
    private final AudioManager mAudioManager;
    private final Context mContext;
    private ActionsDialog mDialog;
    private final EmergencyAffordanceManager mEmergencyAffordanceManager;
    private boolean mHasTelephony;
    private boolean mHasVibrator;
    private ArrayList<Action> mItems;
    private final Runnable mOnDismiss;
    private final boolean mShowSilentToggle;
    private Action mSilentModeAction;
    private final WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;
    private boolean mKeyguardShowing = false;
    private boolean mDeviceProvisioned = false;
    private ToggleAction.State mAirplaneState = ToggleAction.State.Off;
    private boolean mIsWaitingForEcmExit = false;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action) || "android.intent.action.SCREEN_OFF".equals(action)) {
                if (!PhoneWindowManager.SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS.equals(intent.getStringExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY))) {
                    LegacyGlobalActions.this.mHandler.sendEmptyMessage(0);
                }
            } else if ("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED".equals(action) && !intent.getBooleanExtra("PHONE_IN_ECM_STATE", false) && LegacyGlobalActions.this.mIsWaitingForEcmExit) {
                LegacyGlobalActions.this.mIsWaitingForEcmExit = false;
                LegacyGlobalActions.this.changeAirplaneModeSystemSetting(true);
            }
        }
    };
    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            if (LegacyGlobalActions.this.mHasTelephony) {
                boolean z = serviceState.getState() == 3;
                LegacyGlobalActions.this.mAirplaneState = z ? ToggleAction.State.On : ToggleAction.State.Off;
                LegacyGlobalActions.this.mAirplaneModeOn.updateState(LegacyGlobalActions.this.mAirplaneState);
                LegacyGlobalActions.this.mAdapter.notifyDataSetChanged();
            }
        }
    };
    private BroadcastReceiver mRingerModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.media.RINGER_MODE_CHANGED")) {
                LegacyGlobalActions.this.mHandler.sendEmptyMessage(1);
            }
        }
    };
    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z) {
            LegacyGlobalActions.this.onAirplaneModeChanged();
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    if (LegacyGlobalActions.this.mDialog != null) {
                        LegacyGlobalActions.this.mDialog.dismiss();
                        LegacyGlobalActions.this.mDialog = null;
                    }
                    break;
                case 1:
                    LegacyGlobalActions.this.refreshSilentMode();
                    LegacyGlobalActions.this.mAdapter.notifyDataSetChanged();
                    break;
                case 2:
                    LegacyGlobalActions.this.handleShow();
                    break;
            }
        }
    };
    private final IDreamManager mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"));

    public LegacyGlobalActions(Context context, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs, Runnable runnable) {
        boolean z = false;
        this.mContext = context;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mOnDismiss = runnable;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
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
        this.mShowSilentToggle = !this.mContext.getResources().getBoolean(R.^attr-private.pointerIconVectorFillInverse);
        this.mEmergencyAffordanceManager = new EmergencyAffordanceManager(context);
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

    private void handleShow() {
        awakenIfNecessary();
        this.mDialog = createDialog();
        prepareDialog();
        if (this.mAdapter.getCount() == 1 && (this.mAdapter.getItem(0) instanceof SinglePressAction) && !(this.mAdapter.getItem(0) instanceof LongPressAction)) {
            this.mAdapter.getItem(0).onPress();
            return;
        }
        if (this.mDialog != null) {
            WindowManager.LayoutParams attributes = this.mDialog.getWindow().getAttributes();
            attributes.setTitle(TAG);
            this.mDialog.getWindow().setAttributes(attributes);
            this.mDialog.show();
            this.mDialog.getWindow().getDecorView().setSystemUiVisibility(65536);
        }
    }

    private ActionsDialog createDialog() {
        if (!this.mHasVibrator) {
            this.mSilentModeAction = new SilentModeToggleAction();
        } else {
            this.mSilentModeAction = new SilentModeTriStateAction(this.mContext, this.mAudioManager, this.mHandler);
        }
        this.mAirplaneModeOn = new ToggleAction(R.drawable.grid_selector_background_focus, R.drawable.highlight_disabled, R.string.config_defaultTrustAgent, R.string.config_defaultTranslationService, R.string.config_defaultTextClassifierPackage) {
            public void onToggle(boolean z) {
                if (!LegacyGlobalActions.this.mHasTelephony || !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                    LegacyGlobalActions.this.changeAirplaneModeSystemSetting(z);
                    return;
                }
                LegacyGlobalActions.this.mIsWaitingForEcmExit = true;
                Intent intent = new Intent("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", (Uri) null);
                intent.addFlags(268435456);
                LegacyGlobalActions.this.mContext.startActivity(intent);
            }

            protected void changeStateFromPress(boolean z) {
                if (LegacyGlobalActions.this.mHasTelephony && !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                    this.mState = z ? ToggleAction.State.TurningOn : ToggleAction.State.TurningOff;
                    LegacyGlobalActions.this.mAirplaneState = this.mState;
                }
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return false;
            }
        };
        onAirplaneModeChanged();
        this.mItems = new ArrayList<>();
        String[] stringArray = this.mContext.getResources().getStringArray(R.array.config_brightnessThresholdsOfPeakRefreshRate);
        ArraySet arraySet = new ArraySet();
        for (String str : stringArray) {
            if (!arraySet.contains(str)) {
                if (GLOBAL_ACTION_KEY_POWER.equals(str)) {
                    this.mItems.add(new PowerAction(this.mContext, this.mWindowManagerFuncs));
                } else if (GLOBAL_ACTION_KEY_AIRPLANE.equals(str)) {
                    this.mItems.add(this.mAirplaneModeOn);
                } else if (GLOBAL_ACTION_KEY_BUGREPORT.equals(str)) {
                    if (Settings.Global.getInt(this.mContext.getContentResolver(), "bugreport_in_power_menu", 0) != 0 && isCurrentUserOwner()) {
                        this.mItems.add(new BugReportAction());
                    }
                } else if (GLOBAL_ACTION_KEY_SILENT.equals(str)) {
                    if (this.mShowSilentToggle) {
                        this.mItems.add(this.mSilentModeAction);
                    }
                } else if ("users".equals(str)) {
                    if (SystemProperties.getBoolean("fw.power_user_switcher", false)) {
                        addUsersToMenu(this.mItems);
                    }
                } else if (GLOBAL_ACTION_KEY_SETTINGS.equals(str)) {
                    this.mItems.add(getSettingsAction());
                } else if (GLOBAL_ACTION_KEY_LOCKDOWN.equals(str)) {
                    this.mItems.add(getLockdownAction());
                } else if (GLOBAL_ACTION_KEY_VOICEASSIST.equals(str)) {
                    this.mItems.add(getVoiceAssistAction());
                } else if ("assist".equals(str)) {
                    this.mItems.add(getAssistAction());
                } else if (GLOBAL_ACTION_KEY_RESTART.equals(str)) {
                    this.mItems.add(new RestartAction(this.mContext, this.mWindowManagerFuncs));
                } else {
                    Log.e(TAG, "Invalid global action key " + str);
                }
                arraySet.add(str);
            }
        }
        if (this.mEmergencyAffordanceManager.needsEmergencyAffordance()) {
            this.mItems.add(getEmergencyAction());
        }
        this.mAdapter = new ActionsAdapter(this.mContext, this.mItems, new BooleanSupplier() {
            @Override
            public final boolean getAsBoolean() {
                return this.f$0.mDeviceProvisioned;
            }
        }, new BooleanSupplier() {
            @Override
            public final boolean getAsBoolean() {
                return this.f$0.mKeyguardShowing;
            }
        });
        AlertController.AlertParams alertParams = new AlertController.AlertParams(this.mContext);
        alertParams.mAdapter = this.mAdapter;
        alertParams.mOnClickListener = this;
        alertParams.mForceInverseBackground = true;
        ActionsDialog actionsDialog = new ActionsDialog(this.mContext, alertParams);
        actionsDialog.setCanceledOnTouchOutside(false);
        actionsDialog.getListView().setItemsCanFocus(true);
        actionsDialog.getListView().setLongClickable(true);
        actionsDialog.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long j) {
                LongPressAction item = LegacyGlobalActions.this.mAdapter.getItem(i);
                if (item instanceof LongPressAction) {
                    return item.onLongPress();
                }
                return false;
            }
        });
        actionsDialog.getWindow().setType(2009);
        actionsDialog.setOnDismissListener(this);
        return actionsDialog;
    }

    private class BugReportAction extends SinglePressAction implements LongPressAction {
        public BugReportAction() {
            super(R.drawable.highlight_selected, R.string.accessibility_autoclick_scroll_up);
        }

        public void onPress() {
            if (!ActivityManager.isUserAMonkey()) {
                LegacyGlobalActions.this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MetricsLogger.action(LegacyGlobalActions.this.mContext, 292);
                            ActivityManager.getService().requestBugReport(1);
                        } catch (RemoteException e) {
                        }
                    }
                }, 500L);
            }
        }

        public boolean onLongPress() {
            if (ActivityManager.isUserAMonkey()) {
                return false;
            }
            try {
                MetricsLogger.action(LegacyGlobalActions.this.mContext, 293);
                ActivityManager.getService().requestBugReport(0);
            } catch (RemoteException e) {
            }
            return false;
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public boolean showBeforeProvisioning() {
            return false;
        }

        public String getStatus() {
            return LegacyGlobalActions.this.mContext.getString(R.string.accessibility_autoclick_scroll_right, Build.VERSION.RELEASE, Build.ID);
        }
    }

    private Action getSettingsAction() {
        return new SinglePressAction(R.drawable.ic_media_route_connected_light_16_mtrl, R.string.config_defaultSearchSelectorPackageName) {
            public void onPress() {
                if (BenesseExtension.getDchaState() != 0) {
                    return;
                }
                Intent intent = new Intent("android.settings.SETTINGS");
                intent.addFlags(335544320);
                LegacyGlobalActions.this.mContext.startActivity(intent);
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return true;
            }
        };
    }

    private Action getEmergencyAction() {
        return new SinglePressAction(R.drawable.btn_toggle_off_focused_holo_dark, R.string.config_defaultNetworkRecommendationProviderPackage) {
            public void onPress() {
                LegacyGlobalActions.this.mEmergencyAffordanceManager.performEmergencyCall();
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return true;
            }
        };
    }

    private Action getAssistAction() {
        return new SinglePressAction(R.drawable.control_background_32dp_material, R.string.config_defaultMusicRecognitionService) {
            public void onPress() {
                Intent intent = new Intent("android.intent.action.ASSIST");
                intent.addFlags(335544320);
                LegacyGlobalActions.this.mContext.startActivity(intent);
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return true;
            }
        };
    }

    private Action getVoiceAssistAction() {
        return new SinglePressAction(R.drawable.ic_media_route_connecting_dark_16_mtrl, R.string.config_defaultSupervisionProfileOwnerComponent) {
            public void onPress() {
                Intent intent = new Intent("android.intent.action.VOICE_ASSIST");
                intent.addFlags(335544320);
                LegacyGlobalActions.this.mContext.startActivity(intent);
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return true;
            }
        };
    }

    private Action getLockdownAction() {
        return new SinglePressAction(R.drawable.ic_lock_lock, R.string.config_defaultOnDeviceSpeechRecognitionService) {
            public void onPress() {
                new LockPatternUtils(LegacyGlobalActions.this.mContext).requireCredentialEntry(-1);
                try {
                    WindowManagerGlobal.getWindowManagerService().lockNow((Bundle) null);
                } catch (RemoteException e) {
                    Log.e(LegacyGlobalActions.TAG, "Error while trying to lock device.", e);
                }
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return false;
            }
        };
    }

    private UserInfo getCurrentUser() {
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
                    int i = R.drawable.ic_jog_dial_vibrate_on;
                    StringBuilder sb = new StringBuilder();
                    sb.append(userInfo.name != null ? userInfo.name : "Primary");
                    sb.append(z ? " ✔" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    arrayList.add(new SinglePressAction(i, drawableCreateFromPath, sb.toString()) {
                        public void onPress() {
                            try {
                                ActivityManager.getService().switchUser(userInfo.id);
                            } catch (RemoteException e) {
                                Log.e(LegacyGlobalActions.TAG, "Couldn't switch user " + e);
                            }
                        }

                        public boolean showDuringKeyguard() {
                            return true;
                        }

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
        this.mDialog.getWindow().setType(2009);
        if (this.mShowSilentToggle) {
            this.mContext.registerReceiver(this.mRingerModeReceiver, new IntentFilter("android.media.RINGER_MODE_CHANGED"));
        }
    }

    private void refreshSilentMode() {
        if (!this.mHasVibrator) {
            this.mSilentModeAction.updateState(this.mAudioManager.getRingerMode() != 2 ? ToggleAction.State.On : ToggleAction.State.Off);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        if (this.mOnDismiss != null) {
            this.mOnDismiss.run();
        }
        if (this.mShowSilentToggle) {
            try {
                this.mContext.unregisterReceiver(this.mRingerModeReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, e);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (!(this.mAdapter.getItem(i) instanceof SilentModeTriStateAction)) {
            dialogInterface.dismiss();
        }
        this.mAdapter.getItem(i).onPress();
    }

    private class SilentModeToggleAction extends ToggleAction {
        public SilentModeToggleAction() {
            super(R.drawable.dialog_divider_horizontal_holo_light, R.drawable.dialog_divider_horizontal_holo_dark, R.string.config_defaultSmartspaceService, R.string.config_defaultSelectToSpeakService, R.string.config_defaultSearchUiService);
        }

        public void onToggle(boolean z) {
            if (z) {
                LegacyGlobalActions.this.mAudioManager.setRingerMode(0);
            } else {
                LegacyGlobalActions.this.mAudioManager.setRingerMode(2);
            }
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public boolean showBeforeProvisioning() {
            return false;
        }
    }

    private static class SilentModeTriStateAction implements Action, View.OnClickListener {
        private final int[] ITEM_IDS = {R.id.headers, R.id.health, R.id.help};
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

        public CharSequence getLabelForAccessibility(Context context) {
            return null;
        }

        public View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater) {
            View viewInflate = layoutInflater.inflate(R.layout.date_picker_dialog, viewGroup, false);
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

        public void onPress() {
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public boolean showBeforeProvisioning() {
            return false;
        }

        public boolean isEnabled() {
            return true;
        }

        void willCreate() {
        }

        @Override
        public void onClick(View view) {
            if (view.getTag() instanceof Integer) {
                this.mAudioManager.setRingerMode(indexToRingerMode(((Integer) view.getTag()).intValue()));
                this.mHandler.sendEmptyMessageDelayed(0, 300L);
            }
        }
    }

    private void onAirplaneModeChanged() {
        if (this.mHasTelephony) {
            return;
        }
        this.mAirplaneState = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1 ? ToggleAction.State.On : ToggleAction.State.Off;
        this.mAirplaneModeOn.updateState(this.mAirplaneState);
    }

    private void changeAirplaneModeSystemSetting(boolean z) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", z ? 1 : 0);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.addFlags(536870912);
        intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, z);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        if (!this.mHasTelephony) {
            this.mAirplaneState = z ? ToggleAction.State.On : ToggleAction.State.Off;
        }
    }
}
