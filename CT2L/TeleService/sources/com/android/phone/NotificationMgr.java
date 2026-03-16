package com.android.phone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.phone.settings.VoicemailNotificationSettingsUtil;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NotificationMgr {
    private static final boolean DBG;
    private static final String LOG_TAG = NotificationMgr.class.getSimpleName();
    static final String[] PHONES_PROJECTION;
    private static NotificationMgr sInstance;
    private PhoneGlobals mApp;
    private Context mContext;
    private NotificationManager mNotificationManager;
    private Phone mPhone;
    private StatusBarManager mStatusBarManager;
    private SubscriptionManager mSubscriptionManager;
    private TelecomManager mTelecomManager;
    private TelephonyManager mTelephonyManager;
    private Toast mToast;
    private UserManager mUserManager;
    private boolean mSelectedUnavailableNotify = false;
    private ArrayMap<Integer, Boolean> mMwiVisible = new ArrayMap<>();
    public StatusBarHelper statusBarHelper = new StatusBarHelper();

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
        PHONES_PROJECTION = new String[]{"number", "display_name", "_id"};
    }

    private NotificationMgr(PhoneGlobals app) {
        this.mApp = app;
        this.mContext = app;
        this.mNotificationManager = (NotificationManager) app.getSystemService("notification");
        this.mStatusBarManager = (StatusBarManager) app.getSystemService("statusbar");
        this.mUserManager = (UserManager) app.getSystemService("user");
        this.mPhone = app.mCM.getDefaultPhone();
        this.mSubscriptionManager = SubscriptionManager.from(this.mContext);
        this.mTelecomManager = TelecomManager.from(this.mContext);
        this.mTelephonyManager = (TelephonyManager) app.getSystemService("phone");
    }

    static NotificationMgr init(PhoneGlobals app) {
        NotificationMgr notificationMgr;
        synchronized (NotificationMgr.class) {
            if (sInstance == null) {
                sInstance = new NotificationMgr(app);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            notificationMgr = sInstance;
        }
        return notificationMgr;
    }

    public class StatusBarHelper {
        private boolean mIsExpandedViewEnabled;
        private boolean mIsNotificationEnabled;
        private boolean mIsSystemBarNavigationEnabled;

        private StatusBarHelper() {
            this.mIsNotificationEnabled = true;
            this.mIsExpandedViewEnabled = true;
            this.mIsSystemBarNavigationEnabled = true;
        }

        public void enableNotificationAlerts(boolean enable) {
            if (this.mIsNotificationEnabled != enable) {
                this.mIsNotificationEnabled = enable;
                updateStatusBar();
            }
        }

        private void updateStatusBar() {
            int state = this.mIsExpandedViewEnabled ? 0 : 0 | 65536;
            if (!this.mIsNotificationEnabled) {
                state |= 262144;
            }
            if (!this.mIsSystemBarNavigationEnabled) {
                state = state | 2097152 | 16777216 | 4194304 | 33554432;
            }
            if (NotificationMgr.DBG) {
                NotificationMgr.this.log("updateStatusBar: state = 0x" + Integer.toHexString(state));
            }
            NotificationMgr.this.mStatusBarManager.disable(state);
        }
    }

    void refreshMwi(int subId) {
        boolean mwiVisible;
        if (subId == -1 && this.mMwiVisible.keySet().size() == 1) {
            Set<Integer> keySet = this.mMwiVisible.keySet();
            Iterator<Integer> keyIt = keySet.iterator();
            if (keyIt.hasNext()) {
                subId = keyIt.next().intValue();
            } else {
                return;
            }
        }
        if (this.mMwiVisible.containsKey(Integer.valueOf(subId)) && (mwiVisible = this.mMwiVisible.get(Integer.valueOf(subId)).booleanValue())) {
            updateMwi(subId, mwiVisible, false);
        }
    }

    void updateMwi(int subId, boolean visible) {
        updateMwi(subId, visible, true);
    }

    void updateMwi(int subId, boolean visible, boolean enableNotificationSound) {
        String notificationText;
        Intent intent;
        if (!PhoneGlobals.sVoiceCapable) {
            Log.w(LOG_TAG, "Called updateMwi() on non-voice-capable device! Ignoring...");
            return;
        }
        Log.i(LOG_TAG, "updateMwi(): subId " + subId + " update to " + visible);
        this.mMwiVisible.put(Integer.valueOf(subId), Boolean.valueOf(visible));
        if (visible) {
            Phone phone = PhoneGlobals.getPhone(subId);
            if (phone == null) {
                Log.w(LOG_TAG, "Found null phone for: " + subId);
                return;
            }
            SubscriptionInfo subInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(subId);
            if (subInfo == null) {
                Log.w(LOG_TAG, "Found null subscription info for: " + subId);
                return;
            }
            String notificationTitle = this.mContext.getString(R.string.notification_voicemail_title);
            String vmNumber = phone.getVoiceMailNumber();
            if (DBG) {
                log("- got vm number: '" + vmNumber + "'");
            }
            if (vmNumber == null && !phone.getIccRecordsLoaded()) {
                if (DBG) {
                    log("- Null vm number: SIM records not loaded (yet)...");
                    return;
                }
                return;
            }
            if (TelephonyCapabilities.supportsVoiceMessageCount(phone)) {
                int vmCount = phone.getVoiceMessageCount();
                String titleFormat = this.mContext.getString(R.string.notification_voicemail_title_count);
                notificationTitle = String.format(titleFormat, Integer.valueOf(vmCount));
            }
            PhoneAccountHandle phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(phone);
            if (TextUtils.isEmpty(vmNumber)) {
                this.mContext.getString(R.string.notification_voicemail_no_vm_number);
                notificationText = this.mContext.getString(R.string.notification_voicemail_no_vm_number);
                intent = new Intent("com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL");
                intent.putExtra("com.android.phone.SetupVoicemail", true);
                intent.putExtra("com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId", subId);
                intent.setClass(this.mContext, CallFeaturesSetting.class);
            } else {
                if (this.mTelephonyManager.getPhoneCount() > 1) {
                    notificationText = subInfo.getDisplayName().toString();
                } else {
                    notificationText = String.format(this.mContext.getString(R.string.notification_voicemail_text_format), PhoneNumberUtils.formatNumber(vmNumber));
                }
                intent = new Intent("android.intent.action.CALL", Uri.fromParts("voicemail", "", null));
                intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle);
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(this.mContext, subId, intent, 0);
            Uri ringtoneUri = null;
            if (enableNotificationSound) {
                ringtoneUri = VoicemailNotificationSettingsUtil.getRingtoneUri(this.mPhone);
            }
            Notification.Builder builder = new Notification.Builder(this.mContext);
            builder.setSmallIcon(android.R.drawable.stat_notify_voicemail).setWhen(System.currentTimeMillis()).setColor(subInfo.getIconTint()).setContentTitle(notificationTitle).setContentText(notificationText).setContentIntent(pendingIntent).setSound(ringtoneUri).setColor(this.mContext.getResources().getColor(R.color.dialer_theme_color)).setOngoing(true);
            if (VoicemailNotificationSettingsUtil.isVibrationEnabled(phone)) {
                builder.setDefaults(2);
            }
            Notification notification = builder.build();
            List<UserInfo> users = this.mUserManager.getUsers(true);
            for (int i = 0; i < users.size(); i++) {
                UserInfo user = users.get(i);
                UserHandle userHandle = user.getUserHandle();
                if (!this.mUserManager.hasUserRestriction("no_outgoing_calls", userHandle) && !user.isManagedProfile()) {
                    this.mNotificationManager.notifyAsUser(Integer.toString(subId), 3, notification, userHandle);
                }
            }
            return;
        }
        this.mNotificationManager.cancelAsUser(Integer.toString(subId), 3, UserHandle.ALL);
    }

    void updateCfi(int subId, boolean visible) {
        String notificationTitle;
        if (DBG) {
            log("updateCfi(): " + visible);
        }
        if (visible) {
            SubscriptionInfo subInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(subId);
            if (subInfo == null) {
                Log.w(LOG_TAG, "Found null subscription info for: " + subId);
                return;
            }
            if (this.mTelephonyManager.getPhoneCount() > 1) {
                notificationTitle = subInfo.getDisplayName().toString();
            } else {
                notificationTitle = this.mContext.getString(R.string.labelCF);
            }
            Notification.Builder builder = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.stat_sys_phone_call_forward).setColor(subInfo.getIconTint()).setContentTitle(notificationTitle).setContentText(this.mContext.getString(R.string.sum_cfu_enabled_indicator)).setShowWhen(false).setOngoing(true);
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addFlags(335544320);
            intent.setClassName("com.android.phone", "com.android.phone.CallFeaturesSetting");
            SubscriptionInfoHelper.addExtrasToIntent(intent, this.mSubscriptionManager.getActiveSubscriptionInfo(subId));
            PendingIntent contentIntent = PendingIntent.getActivity(this.mContext, subId, intent, 0);
            List<UserInfo> users = this.mUserManager.getUsers(true);
            for (int i = 0; i < users.size(); i++) {
                UserInfo user = users.get(i);
                if (!user.isManagedProfile()) {
                    UserHandle userHandle = user.getUserHandle();
                    builder.setContentIntent(userHandle.isOwner() ? contentIntent : null);
                    this.mNotificationManager.notifyAsUser(Integer.toString(subId), 4, builder.build(), userHandle);
                }
            }
            return;
        }
        this.mNotificationManager.cancelAsUser(Integer.toString(subId), 4, UserHandle.ALL);
    }

    void showDataDisconnectedRoaming() {
        if (DBG) {
            log("showDataDisconnectedRoaming()...");
        }
        Intent intent = new Intent(this.mContext, (Class<?>) MobileNetworkSettings.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this.mContext, 0, intent, 0);
        CharSequence contentText = this.mContext.getText(R.string.roaming_reenable_message);
        Notification.Builder builder = new Notification.Builder(this.mContext).setSmallIcon(android.R.drawable.stat_sys_warning).setContentTitle(this.mContext.getText(R.string.roaming)).setColor(this.mContext.getResources().getColor(R.color.dialer_theme_color)).setContentText(contentText);
        List<UserInfo> users = this.mUserManager.getUsers(true);
        for (int i = 0; i < users.size(); i++) {
            UserInfo user = users.get(i);
            if (!user.isManagedProfile()) {
                UserHandle userHandle = user.getUserHandle();
                builder.setContentIntent(userHandle.isOwner() ? contentIntent : null);
                Notification notif = new Notification.BigTextStyle(builder).bigText(contentText).build();
                this.mNotificationManager.notifyAsUser(null, 5, notif, userHandle);
            }
        }
    }

    void hideDataDisconnectedRoaming() {
        if (DBG) {
            log("hideDataDisconnectedRoaming()...");
        }
        this.mNotificationManager.cancel(5);
    }

    private void showNetworkSelection(String operator) {
        if (DBG) {
            log("showNetworkSelection(" + operator + ")...");
        }
        Notification.Builder builder = new Notification.Builder(this.mContext).setSmallIcon(android.R.drawable.stat_sys_warning).setContentTitle(this.mContext.getString(R.string.notification_network_selection_title)).setContentText(this.mContext.getString(R.string.notification_network_selection_text, operator)).setShowWhen(false).setOngoing(true);
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setFlags(270532608);
        intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.NetworkSetting"));
        PendingIntent contentIntent = PendingIntent.getActivity(this.mContext, 0, intent, 0);
        List<UserInfo> users = this.mUserManager.getUsers(true);
        for (int i = 0; i < users.size(); i++) {
            UserInfo user = users.get(i);
            if (!user.isManagedProfile()) {
                UserHandle userHandle = user.getUserHandle();
                builder.setContentIntent(userHandle.isOwner() ? contentIntent : null);
                this.mNotificationManager.notifyAsUser(null, 6, builder.build(), userHandle);
            }
        }
    }

    private void cancelNetworkSelection() {
        if (DBG) {
            log("cancelNetworkSelection()...");
        }
        this.mNotificationManager.cancelAsUser(null, 6, UserHandle.ALL);
    }

    void updateNetworkSelection(int serviceState) {
        if (TelephonyCapabilities.supportsNetworkSelection(this.mPhone)) {
            int subId = this.mPhone.getSubId();
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
                String networkSelection = sp.getString("network_selection_name_key" + subId, "");
                if (TextUtils.isEmpty(networkSelection)) {
                    networkSelection = sp.getString("network_selection_key" + subId, "");
                }
                if (DBG) {
                    log("updateNetworkSelection()...state = " + serviceState + " new network " + networkSelection);
                }
                if (serviceState == 1 && !TextUtils.isEmpty(networkSelection)) {
                    if (!this.mSelectedUnavailableNotify) {
                        showNetworkSelection(networkSelection);
                        this.mSelectedUnavailableNotify = true;
                        return;
                    }
                    return;
                }
                if (this.mSelectedUnavailableNotify) {
                    cancelNetworkSelection();
                    this.mSelectedUnavailableNotify = false;
                    return;
                }
                return;
            }
            if (DBG) {
                log("updateNetworkSelection()...state = " + serviceState + " not updating network due to invalid subId " + subId);
            }
        }
    }

    void postTransientNotification(int notifyId, CharSequence msg) {
        if (this.mToast != null) {
            this.mToast.cancel();
        }
        this.mToast = Toast.makeText(this.mContext, msg, 1);
        this.mToast.show();
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
