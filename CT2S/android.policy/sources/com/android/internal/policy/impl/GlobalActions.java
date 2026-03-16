package com.android.internal.policy.impl;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
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
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.WindowManagerPolicy;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.app.AlertController;
import com.android.internal.widget.LockPatternUtils;
import java.util.ArrayList;
import java.util.List;

class GlobalActions implements DialogInterface.OnDismissListener, DialogInterface.OnClickListener {
    private static final int DIALOG_DISMISS_DELAY = 300;
    private static final String GLOBAL_ACTION_KEY_AIRPLANE = "airplane";
    private static final String GLOBAL_ACTION_KEY_BUGREPORT = "bugreport";
    private static final String GLOBAL_ACTION_KEY_LOCKDOWN = "lockdown";
    private static final String GLOBAL_ACTION_KEY_POWER = "power";
    private static final String GLOBAL_ACTION_KEY_REBOOT = "reboot";
    private static final String GLOBAL_ACTION_KEY_SETTINGS = "settings";
    private static final String GLOBAL_ACTION_KEY_SILENT = "silent";
    private static final String GLOBAL_ACTION_KEY_USERS = "users";
    private static final int MESSAGE_DISMISS = 0;
    private static final int MESSAGE_REFRESH = 1;
    private static final int MESSAGE_SHOW = 2;
    private static final boolean SHOW_SILENT_TOGGLE = true;
    private static final String TAG = "GlobalActions";
    private MyAdapter mAdapter;
    private ContentObserver mAirplaneModeObserver;
    private ToggleAction mAirplaneModeOn;
    private ToggleAction.State mAirplaneState;
    private final AudioManager mAudioManager;
    private BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private boolean mDeviceProvisioned;
    private GlobalActionsDialog mDialog;
    private final IDreamManager mDreamManager;
    private Handler mHandler;
    private boolean mHasTelephony;
    private boolean mHasVibrator;
    private boolean mIsWaitingForEcmExit;
    private ArrayList<Action> mItems;
    private boolean mKeyguardShowing;
    PhoneStateListener mPhoneStateListener;
    private BroadcastReceiver mRingerModeReceiver;
    private final boolean mShowSilentToggle;
    private Action mSilentModeAction;
    private final WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;

    private interface Action {
        View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater);

        CharSequence getLabelForAccessibility(Context context);

        boolean isEnabled();

        void onPress();

        boolean showBeforeProvisioning();

        boolean showDuringKeyguard();
    }

    private interface LongPressAction extends Action {
        boolean onLongPress();
    }

    public GlobalActions(Context context, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        boolean z = SHOW_SILENT_TOGGLE;
        this.mKeyguardShowing = false;
        this.mDeviceProvisioned = false;
        this.mAirplaneState = ToggleAction.State.Off;
        this.mIsWaitingForEcmExit = false;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action) || "android.intent.action.SCREEN_OFF".equals(action)) {
                    String reason = intent.getStringExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY);
                    if (!PhoneWindowManager.SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS.equals(reason)) {
                        GlobalActions.this.mHandler.sendEmptyMessage(GlobalActions.MESSAGE_DISMISS);
                        return;
                    }
                    return;
                }
                if ("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED".equals(action) && !intent.getBooleanExtra("PHONE_IN_ECM_STATE", false) && GlobalActions.this.mIsWaitingForEcmExit) {
                    GlobalActions.this.mIsWaitingForEcmExit = false;
                    GlobalActions.this.changeAirplaneModeSystemSetting(GlobalActions.SHOW_SILENT_TOGGLE);
                }
            }
        };
        this.mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                if (GlobalActions.this.mHasTelephony) {
                    boolean inAirplaneMode = serviceState.getState() == 3 ? GlobalActions.SHOW_SILENT_TOGGLE : false;
                    GlobalActions.this.mAirplaneState = inAirplaneMode ? ToggleAction.State.On : ToggleAction.State.Off;
                    GlobalActions.this.mAirplaneModeOn.updateState(GlobalActions.this.mAirplaneState);
                    GlobalActions.this.mAdapter.notifyDataSetChanged();
                }
            }
        };
        this.mRingerModeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.media.RINGER_MODE_CHANGED")) {
                    GlobalActions.this.mHandler.sendEmptyMessage(1);
                }
            }
        };
        this.mAirplaneModeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                GlobalActions.this.onAirplaneModeChanged();
            }
        };
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case GlobalActions.MESSAGE_DISMISS:
                        if (GlobalActions.this.mDialog != null) {
                            GlobalActions.this.mDialog.dismiss();
                            GlobalActions.this.mDialog = null;
                        }
                        break;
                    case 1:
                        GlobalActions.this.refreshSilentMode();
                        GlobalActions.this.mAdapter.notifyDataSetChanged();
                        break;
                    case 2:
                        GlobalActions.this.handleShow();
                        break;
                }
            }
        };
        this.mContext = context;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        context.registerReceiver(this.mBroadcastReceiver, filter);
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        this.mHasTelephony = cm.isNetworkSupported(MESSAGE_DISMISS);
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        telephonyManager.listen(this.mPhoneStateListener, 1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), SHOW_SILENT_TOGGLE, this.mAirplaneModeObserver);
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        this.mHasVibrator = (vibrator == null || !vibrator.hasVibrator()) ? MESSAGE_DISMISS : true;
        this.mShowSilentToggle = this.mContext.getResources().getBoolean(R.^attr-private.interpolatorZ) ? MESSAGE_DISMISS : z;
    }

    public void showDialog(boolean keyguardShowing, boolean isDeviceProvisioned) {
        this.mKeyguardShowing = keyguardShowing;
        this.mDeviceProvisioned = isDeviceProvisioned;
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
        if (this.mAdapter.getCount() == 1 && (this.mAdapter.getItem(MESSAGE_DISMISS) instanceof SinglePressAction) && !(this.mAdapter.getItem(MESSAGE_DISMISS) instanceof LongPressAction)) {
            ((SinglePressAction) this.mAdapter.getItem(MESSAGE_DISMISS)).onPress();
            return;
        }
        WindowManager.LayoutParams attrs = this.mDialog.getWindow().getAttributes();
        attrs.setTitle(TAG);
        this.mDialog.getWindow().setAttributes(attrs);
        this.mDialog.show();
        this.mDialog.getWindow().getDecorView().setSystemUiVisibility(65536);
    }

    private GlobalActionsDialog createDialog() {
        if (!this.mHasVibrator) {
            this.mSilentModeAction = new SilentModeToggleAction();
        } else {
            this.mSilentModeAction = new SilentModeTriStateAction(this.mContext, this.mAudioManager, this.mHandler);
        }
        this.mAirplaneModeOn = new ToggleAction(R.drawable.focus_event_pressed_key_background, R.drawable.focused_application_background_static, R.string.accessibility_shortcut_warning_dialog_title, R.string.accessibility_system_action_back_label, R.string.accessibility_system_action_dismiss_notification_shade) {
            @Override
            void onToggle(boolean on) {
                if (!GlobalActions.this.mHasTelephony || !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                    GlobalActions.this.changeAirplaneModeSystemSetting(on);
                    return;
                }
                GlobalActions.this.mIsWaitingForEcmExit = GlobalActions.SHOW_SILENT_TOGGLE;
                Intent ecmDialogIntent = new Intent("android.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", (Uri) null);
                ecmDialogIntent.addFlags(268435456);
                GlobalActions.this.mContext.startActivity(ecmDialogIntent);
            }

            @Override
            protected void changeStateFromPress(boolean buttonOn) {
                if (GlobalActions.this.mHasTelephony && !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                    this.mState = buttonOn ? ToggleAction.State.TurningOn : ToggleAction.State.TurningOff;
                    GlobalActions.this.mAirplaneState = this.mState;
                }
            }

            @Override
            public boolean showDuringKeyguard() {
                return GlobalActions.SHOW_SILENT_TOGGLE;
            }

            @Override
            public boolean showBeforeProvisioning() {
                return false;
            }
        };
        onAirplaneModeChanged();
        this.mItems = new ArrayList<>();
        String[] defaultActions = this.mContext.getResources().getStringArray(R.array.config_companionPermSyncEnabledCerts);
        ArraySet<String> addedKeys = new ArraySet<>();
        for (int i = MESSAGE_DISMISS; i < defaultActions.length; i++) {
            String actionKey = defaultActions[i];
            if (!addedKeys.contains(actionKey)) {
                if (GLOBAL_ACTION_KEY_POWER.equals(actionKey)) {
                    this.mItems.add(new PowerAction());
                } else if (GLOBAL_ACTION_KEY_AIRPLANE.equals(actionKey)) {
                    this.mItems.add(this.mAirplaneModeOn);
                } else if (GLOBAL_ACTION_KEY_BUGREPORT.equals(actionKey)) {
                    if (Settings.Global.getInt(this.mContext.getContentResolver(), "bugreport_in_power_menu", MESSAGE_DISMISS) != 0 && isCurrentUserOwner()) {
                        this.mItems.add(getBugReportAction());
                    }
                } else if (GLOBAL_ACTION_KEY_SILENT.equals(actionKey)) {
                    if (this.mShowSilentToggle) {
                        this.mItems.add(this.mSilentModeAction);
                    }
                } else if (GLOBAL_ACTION_KEY_USERS.equals(actionKey)) {
                    if (SystemProperties.getBoolean("fw.power_user_switcher", false)) {
                        addUsersToMenu(this.mItems);
                    }
                } else if (GLOBAL_ACTION_KEY_SETTINGS.equals(actionKey)) {
                    this.mItems.add(getSettingsAction());
                } else if (GLOBAL_ACTION_KEY_LOCKDOWN.equals(actionKey)) {
                    this.mItems.add(getLockdownAction());
                } else if (!GLOBAL_ACTION_KEY_REBOOT.equals(actionKey)) {
                    Log.e(TAG, "Invalid global action key " + actionKey);
                }
                addedKeys.add(actionKey);
            }
        }
        this.mAdapter = new MyAdapter();
        AlertController.AlertParams params = new AlertController.AlertParams(this.mContext);
        params.mAdapter = this.mAdapter;
        params.mOnClickListener = this;
        params.mForceInverseBackground = SHOW_SILENT_TOGGLE;
        GlobalActionsDialog dialog = new GlobalActionsDialog(this.mContext, params);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getListView().setItemsCanFocus(SHOW_SILENT_TOGGLE);
        dialog.getListView().setLongClickable(SHOW_SILENT_TOGGLE);
        dialog.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Action action = GlobalActions.this.mAdapter.getItem(position);
                if (action instanceof LongPressAction) {
                    return ((LongPressAction) action).onLongPress();
                }
                return false;
            }
        });
        dialog.getWindow().setType(2009);
        dialog.setOnDismissListener(this);
        return dialog;
    }

    private final class PowerAction extends SinglePressAction implements LongPressAction {
        private PowerAction() {
            super(R.drawable.ic_lock_power_off, R.string.accessibility_shortcut_multiple_service_list);
        }

        @Override
        public boolean onLongPress() {
            GlobalActions.this.mWindowManagerFuncs.rebootSafeMode(GlobalActions.SHOW_SILENT_TOGGLE);
            return GlobalActions.SHOW_SILENT_TOGGLE;
        }

        @Override
        public boolean showDuringKeyguard() {
            return GlobalActions.SHOW_SILENT_TOGGLE;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return GlobalActions.SHOW_SILENT_TOGGLE;
        }

        @Override
        public void onPress() {
            GlobalActions.this.mWindowManagerFuncs.shutdown(false);
        }
    }

    private Action getBugReportAction() {
        return new SinglePressAction(R.drawable.frame_gallery_thumb_pressed, R.string.accessibility_shortcut_off) {
            @Override
            public void onPress() {
                AlertDialog.Builder builder = new AlertDialog.Builder(GlobalActions.this.mContext);
                builder.setTitle(R.string.accessibility_shortcut_off);
                builder.setMessage(R.string.accessibility_shortcut_on);
                builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
                builder.setPositiveButton(R.string.granularity_label_link, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!ActivityManager.isUserAMonkey()) {
                            GlobalActions.this.mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        ActivityManagerNative.getDefault().requestBugReport();
                                    } catch (RemoteException e) {
                                    }
                                }
                            }, 500L);
                        }
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.getWindow().setType(2009);
                dialog.show();
            }

            @Override
            public boolean showDuringKeyguard() {
                return GlobalActions.SHOW_SILENT_TOGGLE;
            }

            @Override
            public boolean showBeforeProvisioning() {
                return false;
            }

            @Override
            public String getStatus() {
                return GlobalActions.this.mContext.getString(R.string.accessibility_shortcut_single_service_warning, Build.VERSION.RELEASE, Build.ID);
            }
        };
    }

    private Action getSettingsAction() {
        return new SinglePressAction(R.drawable.ic_expand_more_48dp, R.string.accessibility_system_action_dpad_center_label) {
            @Override
            public void onPress() {
                Intent intent = new Intent("android.settings.SETTINGS");
                intent.addFlags(335544320);
                GlobalActions.this.mContext.startActivity(intent);
            }

            @Override
            public boolean showDuringKeyguard() {
                return GlobalActions.SHOW_SILENT_TOGGLE;
            }

            @Override
            public boolean showBeforeProvisioning() {
                return GlobalActions.SHOW_SILENT_TOGGLE;
            }
        };
    }

    private Action getLockdownAction() {
        return new SinglePressAction(R.drawable.ic_lock_lock, R.string.accessibility_system_action_dpad_down_label) {
            @Override
            public void onPress() {
                new LockPatternUtils(GlobalActions.this.mContext).requireCredentialEntry(-1);
                try {
                    WindowManagerGlobal.getWindowManagerService().lockNow((Bundle) null);
                } catch (RemoteException e) {
                    Log.e(GlobalActions.TAG, "Error while trying to lock device.", e);
                }
            }

            @Override
            public boolean showDuringKeyguard() {
                return GlobalActions.SHOW_SILENT_TOGGLE;
            }

            @Override
            public boolean showBeforeProvisioning() {
                return false;
            }
        };
    }

    private UserInfo getCurrentUser() {
        try {
            return ActivityManagerNative.getDefault().getCurrentUser();
        } catch (RemoteException e) {
            return null;
        }
    }

    private boolean isCurrentUserOwner() {
        UserInfo currentUser = getCurrentUser();
        if (currentUser == null || currentUser.isPrimary()) {
            return SHOW_SILENT_TOGGLE;
        }
        return false;
    }

    private void addUsersToMenu(ArrayList<Action> items) {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        if (um.isUserSwitcherEnabled()) {
            List<UserInfo> users = um.getUsers();
            UserInfo currentUser = getCurrentUser();
            for (final UserInfo user : users) {
                if (user.supportsSwitchTo()) {
                    boolean isCurrentUser = currentUser == null ? user.id == 0 ? true : MESSAGE_DISMISS : currentUser.id == user.id ? true : MESSAGE_DISMISS;
                    Drawable icon = user.iconPath != null ? Drawable.createFromPath(user.iconPath) : null;
                    SinglePressAction switchToUser = new SinglePressAction(R.drawable.ic_btn_square_browser_zoom_fit_page_disabled, icon, (user.name != null ? user.name : "Primary") + (isCurrentUser ? " ✔" : "")) {
                        @Override
                        public void onPress() {
                            try {
                                ActivityManagerNative.getDefault().switchUser(user.id);
                            } catch (RemoteException re) {
                                Log.e(GlobalActions.TAG, "Couldn't switch user " + re);
                            }
                        }

                        @Override
                        public boolean showDuringKeyguard() {
                            return GlobalActions.SHOW_SILENT_TOGGLE;
                        }

                        @Override
                        public boolean showBeforeProvisioning() {
                            return false;
                        }
                    };
                    items.add(switchToUser);
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
            IntentFilter filter = new IntentFilter("android.media.RINGER_MODE_CHANGED");
            this.mContext.registerReceiver(this.mRingerModeReceiver, filter);
        }
    }

    private void refreshSilentMode() {
        if (!this.mHasVibrator) {
            boolean silentModeOn = this.mAudioManager.getRingerMode() != 2 ? SHOW_SILENT_TOGGLE : false;
            ((ToggleAction) this.mSilentModeAction).updateState(silentModeOn ? ToggleAction.State.On : ToggleAction.State.Off);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (this.mShowSilentToggle) {
            try {
                this.mContext.unregisterReceiver(this.mRingerModeReceiver);
            } catch (IllegalArgumentException ie) {
                Log.w(TAG, ie);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (!(this.mAdapter.getItem(which) instanceof SilentModeTriStateAction)) {
            dialog.dismiss();
        }
        this.mAdapter.getItem(which).onPress();
    }

    private class MyAdapter extends BaseAdapter {
        private MyAdapter() {
        }

        @Override
        public int getCount() {
            int count = GlobalActions.MESSAGE_DISMISS;
            for (int i = GlobalActions.MESSAGE_DISMISS; i < GlobalActions.this.mItems.size(); i++) {
                Action action = (Action) GlobalActions.this.mItems.get(i);
                if ((!GlobalActions.this.mKeyguardShowing || action.showDuringKeyguard()) && (GlobalActions.this.mDeviceProvisioned || action.showBeforeProvisioning())) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position).isEnabled();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public Action getItem(int position) {
            int filteredPos = GlobalActions.MESSAGE_DISMISS;
            for (int i = GlobalActions.MESSAGE_DISMISS; i < GlobalActions.this.mItems.size(); i++) {
                Action action = (Action) GlobalActions.this.mItems.get(i);
                if ((!GlobalActions.this.mKeyguardShowing || action.showDuringKeyguard()) && (GlobalActions.this.mDeviceProvisioned || action.showBeforeProvisioning())) {
                    if (filteredPos == position) {
                        return action;
                    }
                    filteredPos++;
                }
            }
            throw new IllegalArgumentException("position " + position + " out of range of showable actions, filtered count=" + getCount() + ", keyguardshowing=" + GlobalActions.this.mKeyguardShowing + ", provisioned=" + GlobalActions.this.mDeviceProvisioned);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Action action = getItem(position);
            return action.create(GlobalActions.this.mContext, convertView, parent, LayoutInflater.from(GlobalActions.this.mContext));
        }
    }

    private static abstract class SinglePressAction implements Action {
        private final Drawable mIcon;
        private final int mIconResId;
        private final CharSequence mMessage;
        private final int mMessageResId;

        @Override
        public abstract void onPress();

        protected SinglePressAction(int iconResId, int messageResId) {
            this.mIconResId = iconResId;
            this.mMessageResId = messageResId;
            this.mMessage = null;
            this.mIcon = null;
        }

        protected SinglePressAction(int iconResId, Drawable icon, CharSequence message) {
            this.mIconResId = iconResId;
            this.mMessageResId = GlobalActions.MESSAGE_DISMISS;
            this.mMessage = message;
            this.mIcon = icon;
        }

        protected SinglePressAction(int iconResId, CharSequence message) {
            this.mIconResId = iconResId;
            this.mMessageResId = GlobalActions.MESSAGE_DISMISS;
            this.mMessage = message;
            this.mIcon = null;
        }

        @Override
        public boolean isEnabled() {
            return GlobalActions.SHOW_SILENT_TOGGLE;
        }

        public String getStatus() {
            return null;
        }

        @Override
        public CharSequence getLabelForAccessibility(Context context) {
            return this.mMessage != null ? this.mMessage : context.getString(this.mMessageResId);
        }

        @Override
        public View create(Context context, View convertView, ViewGroup parent, LayoutInflater inflater) {
            View v = inflater.inflate(R.layout.car_preference_category, parent, false);
            ImageView icon = (ImageView) v.findViewById(R.id.icon);
            TextView messageView = (TextView) v.findViewById(R.id.message);
            TextView statusView = (TextView) v.findViewById(R.id.expand_button_pill_colorized_layer);
            String status = getStatus();
            if (!TextUtils.isEmpty(status)) {
                statusView.setText(status);
            } else {
                statusView.setVisibility(8);
            }
            if (this.mIcon != null) {
                icon.setImageDrawable(this.mIcon);
                icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else if (this.mIconResId != 0) {
                icon.setImageDrawable(context.getDrawable(this.mIconResId));
            }
            if (this.mMessage != null) {
                messageView.setText(this.mMessage);
            } else {
                messageView.setText(this.mMessageResId);
            }
            return v;
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
            TurningOn(GlobalActions.SHOW_SILENT_TOGGLE),
            TurningOff(GlobalActions.SHOW_SILENT_TOGGLE),
            On(false);

            private final boolean inTransition;

            State(boolean intermediate) {
                this.inTransition = intermediate;
            }

            public boolean inTransition() {
                return this.inTransition;
            }
        }

        public ToggleAction(int enabledIconResId, int disabledIconResid, int message, int enabledStatusMessageResId, int disabledStatusMessageResId) {
            this.mEnabledIconResId = enabledIconResId;
            this.mDisabledIconResid = disabledIconResid;
            this.mMessageResId = message;
            this.mEnabledStatusMessageResId = enabledStatusMessageResId;
            this.mDisabledStatusMessageResId = disabledStatusMessageResId;
        }

        void willCreate() {
        }

        @Override
        public CharSequence getLabelForAccessibility(Context context) {
            return context.getString(this.mMessageResId);
        }

        @Override
        public View create(Context context, View convertView, ViewGroup parent, LayoutInflater inflater) {
            willCreate();
            View v = inflater.inflate(R.layout.car_preference_category, parent, false);
            ImageView icon = (ImageView) v.findViewById(R.id.icon);
            TextView messageView = (TextView) v.findViewById(R.id.message);
            TextView statusView = (TextView) v.findViewById(R.id.expand_button_pill_colorized_layer);
            boolean enabled = isEnabled();
            if (messageView != null) {
                messageView.setText(this.mMessageResId);
                messageView.setEnabled(enabled);
            }
            boolean on = (this.mState == State.On || this.mState == State.TurningOn) ? GlobalActions.SHOW_SILENT_TOGGLE : GlobalActions.MESSAGE_DISMISS;
            if (icon != null) {
                icon.setImageDrawable(context.getDrawable(on ? this.mEnabledIconResId : this.mDisabledIconResid));
                icon.setEnabled(enabled);
            }
            if (statusView != null) {
                statusView.setText(on ? this.mEnabledStatusMessageResId : this.mDisabledStatusMessageResId);
                statusView.setVisibility(GlobalActions.MESSAGE_DISMISS);
                statusView.setEnabled(enabled);
            }
            v.setEnabled(enabled);
            return v;
        }

        @Override
        public final void onPress() {
            if (this.mState.inTransition()) {
                Log.w(GlobalActions.TAG, "shouldn't be able to toggle when in transition");
                return;
            }
            boolean nowOn = this.mState != State.On ? GlobalActions.SHOW_SILENT_TOGGLE : false;
            onToggle(nowOn);
            changeStateFromPress(nowOn);
        }

        @Override
        public boolean isEnabled() {
            if (this.mState.inTransition()) {
                return false;
            }
            return GlobalActions.SHOW_SILENT_TOGGLE;
        }

        protected void changeStateFromPress(boolean buttonOn) {
            this.mState = buttonOn ? State.On : State.Off;
        }

        public void updateState(State state) {
            this.mState = state;
        }
    }

    private class SilentModeToggleAction extends ToggleAction {
        public SilentModeToggleAction() {
            super(R.drawable.dropdown_focused_holo_dark, R.drawable.dropdown_disabled_holo_light, R.string.accessibility_shortcut_single_service_warning_title, R.string.accessibility_shortcut_spoken_feedback, R.string.accessibility_shortcut_toogle_warning);
        }

        @Override
        void onToggle(boolean on) {
            if (on) {
                GlobalActions.this.mAudioManager.setRingerMode(GlobalActions.MESSAGE_DISMISS);
            } else {
                GlobalActions.this.mAudioManager.setRingerMode(2);
            }
        }

        @Override
        public boolean showDuringKeyguard() {
            return GlobalActions.SHOW_SILENT_TOGGLE;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return false;
        }
    }

    private static class SilentModeTriStateAction implements Action, View.OnClickListener {
        private final int[] ITEM_IDS = {R.id.expand_button_touch_container, R.id.expanded_menu, R.id.expires_on};
        private final AudioManager mAudioManager;
        private final Context mContext;
        private final Handler mHandler;

        SilentModeTriStateAction(Context context, AudioManager audioManager, Handler handler) {
            this.mAudioManager = audioManager;
            this.mHandler = handler;
            this.mContext = context;
        }

        private int ringerModeToIndex(int ringerMode) {
            return ringerMode;
        }

        private int indexToRingerMode(int index) {
            return index;
        }

        @Override
        public CharSequence getLabelForAccessibility(Context context) {
            return null;
        }

        @Override
        public View create(Context context, View convertView, ViewGroup parent, LayoutInflater inflater) {
            View v = inflater.inflate(R.layout.cascading_menu_item_layout, parent, false);
            int selectedIndex = ringerModeToIndex(this.mAudioManager.getRingerMode());
            int i = GlobalActions.MESSAGE_DISMISS;
            while (i < 3) {
                View itemView = v.findViewById(this.ITEM_IDS[i]);
                itemView.setSelected(selectedIndex == i ? GlobalActions.SHOW_SILENT_TOGGLE : GlobalActions.MESSAGE_DISMISS);
                itemView.setTag(Integer.valueOf(i));
                itemView.setOnClickListener(this);
                i++;
            }
            return v;
        }

        @Override
        public void onPress() {
        }

        @Override
        public boolean showDuringKeyguard() {
            return GlobalActions.SHOW_SILENT_TOGGLE;
        }

        @Override
        public boolean showBeforeProvisioning() {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return GlobalActions.SHOW_SILENT_TOGGLE;
        }

        void willCreate() {
        }

        @Override
        public void onClick(View v) {
            if (v.getTag() instanceof Integer) {
                int index = ((Integer) v.getTag()).intValue();
                this.mAudioManager.setRingerMode(indexToRingerMode(index));
                this.mHandler.sendEmptyMessageDelayed(GlobalActions.MESSAGE_DISMISS, 300L);
            }
        }
    }

    private void onAirplaneModeChanged() {
        boolean airplaneModeOn = SHOW_SILENT_TOGGLE;
        if (!this.mHasTelephony) {
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", MESSAGE_DISMISS) != 1) {
                airplaneModeOn = MESSAGE_DISMISS;
            }
            this.mAirplaneState = airplaneModeOn ? ToggleAction.State.On : ToggleAction.State.Off;
            this.mAirplaneModeOn.updateState(this.mAirplaneState);
        }
    }

    private void changeAirplaneModeSystemSetting(boolean on) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", on ? 1 : MESSAGE_DISMISS);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.addFlags(536870912);
        intent.putExtra("state", on);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        if (!this.mHasTelephony) {
            this.mAirplaneState = on ? ToggleAction.State.On : ToggleAction.State.Off;
        }
    }

    private static final class GlobalActionsDialog extends Dialog implements DialogInterface {
        private final MyAdapter mAdapter;
        private final AlertController mAlert;
        private boolean mCancelOnUp;
        private final Context mContext;
        private EnableAccessibilityController mEnableAccessibilityController;
        private boolean mIntercepted;
        private final int mWindowTouchSlop;

        public GlobalActionsDialog(Context context, AlertController.AlertParams params) {
            super(context, getDialogTheme(context));
            this.mContext = context;
            this.mAlert = new AlertController(this.mContext, this, getWindow());
            this.mAdapter = (MyAdapter) params.mAdapter;
            this.mWindowTouchSlop = ViewConfiguration.get(context).getScaledWindowTouchSlop();
            params.apply(this.mAlert);
        }

        private static int getDialogTheme(Context context) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.alertDialogTheme, outValue, GlobalActions.SHOW_SILENT_TOGGLE);
            return outValue.resourceId;
        }

        @Override
        protected void onStart() {
            if (EnableAccessibilityController.canEnableAccessibilityViaGesture(this.mContext)) {
                this.mEnableAccessibilityController = new EnableAccessibilityController(this.mContext, new Runnable() {
                    @Override
                    public void run() {
                        GlobalActionsDialog.this.dismiss();
                    }
                });
                super.setCanceledOnTouchOutside(false);
            } else {
                this.mEnableAccessibilityController = null;
                super.setCanceledOnTouchOutside(GlobalActions.SHOW_SILENT_TOGGLE);
            }
            super.onStart();
        }

        @Override
        protected void onStop() {
            if (this.mEnableAccessibilityController != null) {
                this.mEnableAccessibilityController.onDestroy();
            }
            super.onStop();
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            if (this.mEnableAccessibilityController != null) {
                int action = event.getActionMasked();
                if (action == 0) {
                    View decor = getWindow().getDecorView();
                    int eventX = (int) event.getX();
                    int eventY = (int) event.getY();
                    if (eventX < (-this.mWindowTouchSlop) || eventY < (-this.mWindowTouchSlop) || eventX >= decor.getWidth() + this.mWindowTouchSlop || eventY >= decor.getHeight() + this.mWindowTouchSlop) {
                        this.mCancelOnUp = GlobalActions.SHOW_SILENT_TOGGLE;
                    }
                }
                try {
                    if (!this.mIntercepted) {
                        this.mIntercepted = this.mEnableAccessibilityController.onInterceptTouchEvent(event);
                        if (this.mIntercepted) {
                            long now = SystemClock.uptimeMillis();
                            event = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, GlobalActions.MESSAGE_DISMISS);
                            event.setSource(4098);
                            this.mCancelOnUp = GlobalActions.SHOW_SILENT_TOGGLE;
                        }
                    } else {
                        boolean zOnTouchEvent = this.mEnableAccessibilityController.onTouchEvent(event);
                        if (action != 1) {
                            return zOnTouchEvent;
                        }
                        if (this.mCancelOnUp) {
                            cancel();
                        }
                        this.mCancelOnUp = false;
                        this.mIntercepted = false;
                        return zOnTouchEvent;
                    }
                } finally {
                    if (action == 1) {
                        if (this.mCancelOnUp) {
                            cancel();
                        }
                        this.mCancelOnUp = false;
                        this.mIntercepted = false;
                    }
                }
            }
            return super.dispatchTouchEvent(event);
        }

        public ListView getListView() {
            return this.mAlert.getListView();
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mAlert.installContent();
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            if (event.getEventType() == 32) {
                for (int i = GlobalActions.MESSAGE_DISMISS; i < this.mAdapter.getCount(); i++) {
                    CharSequence label = this.mAdapter.getItem(i).getLabelForAccessibility(getContext());
                    if (label != null) {
                        event.getText().add(label);
                    }
                }
            }
            return super.dispatchPopulateAccessibilityEvent(event);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            return this.mAlert.onKeyDown(keyCode, event) ? GlobalActions.SHOW_SILENT_TOGGLE : super.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            return this.mAlert.onKeyUp(keyCode, event) ? GlobalActions.SHOW_SILENT_TOGGLE : super.onKeyUp(keyCode, event);
        }
    }
}
