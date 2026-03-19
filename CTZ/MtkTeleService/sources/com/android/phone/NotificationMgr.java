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
import android.content.res.Resources;
import android.net.Uri;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.telecom.DefaultDialerManager;
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
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.phone.settings.VoicemailSettingsActivity;
import java.util.Iterator;
import java.util.List;

public class NotificationMgr {
    static final int CALL_FORWARD_NOTIFICATION = 4;
    static final int DATA_DISCONNECTED_ROAMING_NOTIFICATION = 5;
    private static final boolean DBG;
    private static final String LOG_TAG = NotificationMgr.class.getSimpleName();
    static final int MMI_NOTIFICATION = 1;
    private static final String MWI_SHOULD_CHECK_VVM_CONFIGURATION_KEY_PREFIX = "mwi_should_check_vvm_configuration_state_";
    static final int NETWORK_SELECTION_NOTIFICATION = 2;
    static final String[] PHONES_PROJECTION;
    static final int SELECTED_OPERATOR_FAIL_NOTIFICATION = 6;
    private static final boolean VDBG = false;
    static final int VOICEMAIL_NOTIFICATION = 3;
    private static NotificationMgr sInstance;
    private PhoneGlobals mApp;
    private Context mContext;
    private NotificationManager mNotificationManager;
    private StatusBarManager mStatusBarManager;
    private SubscriptionManager mSubscriptionManager;
    private TelecomManager mTelecomManager;
    private TelephonyManager mTelephonyManager;
    private Toast mToast;
    private UserManager mUserManager;
    private boolean mSelectedUnavailableNotify = false;
    private ArrayMap<Integer, Boolean> mMwiVisible = new ArrayMap<>();

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
        PHONES_PROJECTION = new String[]{"number", "display_name", "_id"};
    }

    private NotificationMgr(PhoneGlobals phoneGlobals) {
        this.mApp = phoneGlobals;
        this.mContext = phoneGlobals;
        this.mNotificationManager = (NotificationManager) phoneGlobals.getSystemService("notification");
        this.mStatusBarManager = (StatusBarManager) phoneGlobals.getSystemService("statusbar");
        this.mUserManager = (UserManager) phoneGlobals.getSystemService("user");
        this.mSubscriptionManager = SubscriptionManager.from(this.mContext);
        this.mTelecomManager = TelecomManager.from(this.mContext);
        this.mTelephonyManager = (TelephonyManager) phoneGlobals.getSystemService("phone");
    }

    static NotificationMgr init(PhoneGlobals phoneGlobals) {
        NotificationMgr notificationMgr;
        synchronized (NotificationMgr.class) {
            if (sInstance == null) {
                sInstance = new NotificationMgr(phoneGlobals);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            notificationMgr = sInstance;
        }
        return notificationMgr;
    }

    void refreshMwi(int i) {
        if (i == -1 && this.mMwiVisible.keySet().size() == 1) {
            Iterator<Integer> it = this.mMwiVisible.keySet().iterator();
            if (!it.hasNext()) {
                return;
            } else {
                i = it.next().intValue();
            }
        }
        if (this.mMwiVisible.containsKey(Integer.valueOf(i)) && this.mMwiVisible.get(Integer.valueOf(i)).booleanValue()) {
            this.mApp.notifier.updatePhoneStateListeners(true);
        }
    }

    public void setShouldCheckVisualVoicemailConfigurationForMwi(int i, boolean z) {
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            Log.e(LOG_TAG, "setShouldCheckVisualVoicemailConfigurationForMwi: invalid subId" + i);
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(this.mContext).edit().putBoolean(MWI_SHOULD_CHECK_VVM_CONFIGURATION_KEY_PREFIX + i, z).apply();
    }

    private boolean shouldCheckVisualVoicemailConfigurationForMwi(int i) {
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            Log.e(LOG_TAG, "shouldCheckVisualVoicemailConfigurationForMwi: invalid subId" + i);
            return true;
        }
        return PreferenceManager.getDefaultSharedPreferences(this.mContext).getBoolean(MWI_SHOULD_CHECK_VVM_CONFIGURATION_KEY_PREFIX + i, true);
    }

    void updateMwi(int i, boolean z) {
        updateMwi(i, z, false);
    }

    void updateMwi(int i, boolean z, boolean z2) {
        Integer num;
        String string;
        String string2;
        Intent intent;
        int i2;
        List list;
        if (!PhoneGlobals.sVoiceCapable) {
            Log.w(LOG_TAG, "Called updateMwi() on non-voice-capable device! Ignoring...");
            return;
        }
        Phone phone = PhoneGlobals.getPhone(i);
        Log.i(LOG_TAG, "updateMwi(): subId " + i + " update to " + z);
        this.mMwiVisible.put(Integer.valueOf(i), Boolean.valueOf(z));
        if (z) {
            if (phone == null) {
                Log.w(LOG_TAG, "Found null phone for: " + i);
                return;
            }
            SubscriptionInfo activeSubscriptionInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(i);
            if (activeSubscriptionInfo == null) {
                Log.w(LOG_TAG, "Found null subscription info for: " + i);
                return;
            }
            int i3 = android.R.drawable.stat_notify_voicemail;
            if (this.mTelephonyManager.getPhoneCount() > 1) {
                i3 = phone.getPhoneId() == 0 ? R.drawable.stat_notify_voicemail_sub1 : R.drawable.stat_notify_voicemail_sub2;
            }
            String string3 = this.mContext.getString(R.string.notification_voicemail_title);
            String voiceMailNumber = phone.getVoiceMailNumber();
            if (DBG) {
                log("- got vm number: '" + voiceMailNumber + "'");
            }
            if (voiceMailNumber == null && !phone.getIccRecordsLoaded()) {
                if (DBG) {
                    log("- Null vm number: SIM records not loaded (yet)...");
                    return;
                }
                return;
            }
            if (TelephonyCapabilities.supportsVoiceMessageCount(phone)) {
                Integer numValueOf = Integer.valueOf(phone.getVoiceMessageCount());
                num = numValueOf;
                string3 = String.format(this.mContext.getString(R.string.notification_voicemail_title_count), numValueOf);
            } else {
                num = null;
            }
            PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(phone);
            boolean zIsEmpty = TextUtils.isEmpty(voiceMailNumber);
            if (zIsEmpty) {
                this.mContext.getString(R.string.notification_voicemail_no_vm_number);
                string2 = this.mContext.getString(R.string.notification_voicemail_no_vm_number);
                intent = new Intent("com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL");
                intent.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, i);
                intent.setClass(this.mContext, VoicemailSettingsActivity.class);
            } else {
                if (this.mTelephonyManager.getPhoneCount() > 1) {
                    string = activeSubscriptionInfo.getDisplayName().toString();
                } else {
                    string = String.format(this.mContext.getString(R.string.notification_voicemail_text_format), PhoneNumberUtils.formatNumber(voiceMailNumber));
                }
                Intent intent2 = new Intent("android.intent.action.CALL", Uri.fromParts("voicemail", "", null));
                intent2.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandleMakePstnPhoneAccountHandle);
                string2 = string;
                intent = intent2;
            }
            PendingIntent activity = PendingIntent.getActivity(this.mContext, i, intent, 0);
            Resources resources = this.mContext.getResources();
            PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(i);
            Notification.Builder builder = new Notification.Builder(this.mContext);
            builder.setSmallIcon(i3).setWhen(System.currentTimeMillis()).setColor(activeSubscriptionInfo.getIconTint()).setContentTitle(string3).setContentText(string2).setContentIntent(activity).setColor(resources.getColor(R.color.dialer_theme_color)).setOngoing(carrierConfigForSubId.getBoolean("voicemail_notification_persistent_bool")).setChannel("voiceMail").setOnlyAlertOnce(z2);
            Notification notificationBuild = builder.build();
            List users = this.mUserManager.getUsers(true);
            int i4 = 0;
            while (i4 < users.size()) {
                UserInfo userInfo = (UserInfo) users.get(i4);
                UserHandle userHandle = userInfo.getUserHandle();
                if (this.mUserManager.hasUserRestriction("no_outgoing_calls", userHandle) || userInfo.isManagedProfile()) {
                    i2 = i4;
                    list = users;
                } else {
                    i2 = i4;
                    list = users;
                    if (!maybeSendVoicemailNotificationUsingDefaultDialer(phone, num, voiceMailNumber, activity, zIsEmpty, userHandle, z2)) {
                        this.mNotificationManager.notifyAsUser(Integer.toString(i), 3, notificationBuild, userHandle);
                    }
                }
                i4 = i2 + 1;
                users = list;
            }
            return;
        }
        List users2 = this.mUserManager.getUsers(true);
        for (int i5 = 0; i5 < users2.size(); i5++) {
            UserInfo userInfo2 = (UserInfo) users2.get(i5);
            UserHandle userHandle2 = userInfo2.getUserHandle();
            if (!this.mUserManager.hasUserRestriction("no_outgoing_calls", userHandle2) && !userInfo2.isManagedProfile()) {
                if (!maybeSendVoicemailNotificationUsingDefaultDialer(phone, 0, null, null, false, userHandle2, z2)) {
                    this.mNotificationManager.cancelAsUser(Integer.toString(i), 3, userHandle2);
                }
            }
        }
    }

    private boolean maybeSendVoicemailNotificationUsingDefaultDialer(Phone phone, Integer num, String str, PendingIntent pendingIntent, boolean z, UserHandle userHandle, boolean z2) {
        String str2;
        if (shouldManageNotificationThroughDefaultDialer(userHandle)) {
            Intent showVoicemailIntentForDefaultDialer = getShowVoicemailIntentForDefaultDialer(userHandle);
            showVoicemailIntentForDefaultDialer.setFlags(268435456);
            showVoicemailIntentForDefaultDialer.setAction("android.telephony.action.SHOW_VOICEMAIL_NOTIFICATION");
            showVoicemailIntentForDefaultDialer.putExtra("android.telephony.extra.PHONE_ACCOUNT_HANDLE", PhoneUtils.makePstnPhoneAccountHandle(phone));
            showVoicemailIntentForDefaultDialer.putExtra("android.telephony.extra.IS_REFRESH", z2);
            if (num != null) {
                showVoicemailIntentForDefaultDialer.putExtra("android.telephony.extra.NOTIFICATION_COUNT", num);
            }
            if (num == null || num.intValue() > 0) {
                if (!TextUtils.isEmpty(str)) {
                    showVoicemailIntentForDefaultDialer.putExtra("android.telephony.extra.VOICEMAIL_NUMBER", str);
                }
                if (pendingIntent != null) {
                    if (z) {
                        str2 = "android.telephony.extra.LAUNCH_VOICEMAIL_SETTINGS_INTENT";
                    } else {
                        str2 = "android.telephony.extra.CALL_VOICEMAIL_INTENT";
                    }
                    showVoicemailIntentForDefaultDialer.putExtra(str2, pendingIntent);
                }
            }
            this.mContext.sendBroadcastAsUser(showVoicemailIntentForDefaultDialer, userHandle, "android.permission.READ_PHONE_STATE");
            return true;
        }
        return false;
    }

    private Intent getShowVoicemailIntentForDefaultDialer(UserHandle userHandle) {
        return new Intent("android.telephony.action.SHOW_VOICEMAIL_NOTIFICATION").setPackage(DefaultDialerManager.getDefaultDialerApplication(this.mContext, userHandle.getIdentifier()));
    }

    private boolean shouldManageNotificationThroughDefaultDialer(UserHandle userHandle) {
        Intent showVoicemailIntentForDefaultDialer = getShowVoicemailIntentForDefaultDialer(userHandle);
        return showVoicemailIntentForDefaultDialer != null && this.mContext.getPackageManager().queryBroadcastReceivers(showVoicemailIntentForDefaultDialer, 0).size() > 0;
    }

    void updateCfi(int i, boolean z) {
        updateCfi(i, z, false);
    }

    void updateCfi(int i, boolean z, boolean z2) {
        String string;
        StringBuilder sb = new StringBuilder();
        sb.append("updateCfi: subId= ");
        sb.append(i);
        sb.append(", visible=");
        sb.append(z ? "Y" : "N");
        logi(sb.toString());
        if (z) {
            SubscriptionInfo activeSubscriptionInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(i);
            if (activeSubscriptionInfo == null) {
                Log.w(LOG_TAG, "Found null subscription info for: " + i);
                return;
            }
            int i2 = R.drawable.stat_sys_phone_call_forward;
            if (this.mTelephonyManager.getPhoneCount() > 1) {
                i2 = SubscriptionManager.getSlotIndex(i) == 0 ? R.drawable.stat_sys_phone_call_forward_sub1 : R.drawable.stat_sys_phone_call_forward_sub2;
                string = activeSubscriptionInfo.getDisplayName().toString();
            } else {
                string = this.mContext.getString(R.string.labelCF);
            }
            Notification.Builder onlyAlertOnce = new Notification.Builder(this.mContext).setSmallIcon(i2).setColor(activeSubscriptionInfo.getIconTint()).setContentTitle(string).setContentText(this.mContext.getString(R.string.sum_cfu_enabled_indicator)).setShowWhen(false).setOngoing(true).setChannel("callForward").setOnlyAlertOnce(z2);
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addFlags(335544320);
            intent.setClassName("com.android.phone", "com.android.phone.CallFeaturesSetting");
            SubscriptionInfoHelper.addExtrasToIntent(intent, this.mSubscriptionManager.getActiveSubscriptionInfo(i));
            onlyAlertOnce.setContentIntent(PendingIntent.getActivity(this.mContext, i, intent, 0));
            this.mNotificationManager.notifyAsUser(Integer.toString(i), 4, onlyAlertOnce.build(), UserHandle.ALL);
            return;
        }
        for (UserInfo userInfo : this.mUserManager.getUsers(true)) {
            if (!userInfo.isManagedProfile()) {
                this.mNotificationManager.cancelAsUser(Integer.toString(i), 4, userInfo.getUserHandle());
            }
        }
    }

    void showDataDisconnectedRoaming(int i) {
        if (DBG) {
            log("showDataDisconnectedRoaming()...");
        }
        Intent intent = new Intent(this.mContext, (Class<?>) MobileNetworkSettings.class);
        intent.putExtra("android.provider.extra.SUB_ID", i);
        intent.addFlags(536870912);
        PendingIntent activity = PendingIntent.getActivity(this.mContext, i, intent, 0);
        CharSequence text = this.mContext.getText(R.string.roaming_reenable_message);
        this.mNotificationManager.notifyAsUser(null, 5, new Notification.BigTextStyle(new Notification.Builder(this.mContext).setSmallIcon(android.R.drawable.stat_sys_warning).setContentTitle(this.mContext.getText(R.string.roaming_notification_title)).setColor(this.mContext.getResources().getColor(R.color.dialer_theme_color)).setContentText(text).setChannel("mobileDataAlertNew").setContentIntent(activity)).bigText(text).build(), UserHandle.ALL);
    }

    void hideDataDisconnectedRoaming() {
        if (DBG) {
            log("hideDataDisconnectedRoaming()...");
        }
        this.mNotificationManager.cancel(5);
    }

    private void showNetworkSelection(String str, int i) {
        if (DBG) {
            log("showNetworkSelection(" + str + ")...");
        }
        Notification.Builder channel = new Notification.Builder(this.mContext).setSmallIcon(android.R.drawable.stat_sys_warning).setContentTitle(this.mContext.getString(R.string.notification_network_selection_title)).setContentText(this.mContext.getString(R.string.notification_network_selection_text, str)).setShowWhen(false).setOngoing(true).setChannel("alert");
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setFlags(270532608);
        intent.setComponent(new ComponentName(this.mContext.getString(R.string.mobile_network_settings_package), this.mContext.getString(R.string.mobile_network_settings_class)));
        intent.putExtra(GsmUmtsOptions.EXTRA_SUB_ID, i);
        channel.setContentIntent(PendingIntent.getActivity(this.mContext, 0, intent, 0));
        this.mNotificationManager.notifyAsUser(null, 6, channel.build(), UserHandle.ALL);
    }

    private void cancelNetworkSelection() {
        if (DBG) {
            log("cancelNetworkSelection()...");
        }
        this.mNotificationManager.cancelAsUser(null, 6, UserHandle.ALL);
    }

    void updateNetworkSelection(int i, int i2) {
        boolean isManualSelection;
        int phoneId = SubscriptionManager.getPhoneId(i2);
        Phone phone = SubscriptionManager.isValidPhoneId(phoneId) ? PhoneFactory.getPhone(phoneId) : PhoneFactory.getDefaultPhone();
        if (TelephonyCapabilities.supportsNetworkSelection(phone)) {
            if (SubscriptionManager.isValidSubscriptionId(i2)) {
                SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);
                String string = defaultSharedPreferences.getString("network_selection_name_key" + i2, "");
                if (TextUtils.isEmpty(string)) {
                    string = defaultSharedPreferences.getString("network_selection_key" + i2, "");
                }
                if (!(!this.mContext.getResources().getBoolean(android.R.^attr-private.showAtTop))) {
                    isManualSelection = phone.getServiceStateTracker().mSS.getIsManualSelection();
                } else {
                    isManualSelection = !TextUtils.isEmpty(string);
                }
                if (DBG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("updateNetworkSelection()...state = ");
                    sb.append(i);
                    sb.append(" new network ");
                    sb.append(isManualSelection ? string : "");
                    log(sb.toString());
                }
                if (i == 1 && isManualSelection) {
                    showNetworkSelection(string, i2);
                    this.mSelectedUnavailableNotify = true;
                    return;
                } else {
                    if (this.mSelectedUnavailableNotify) {
                        cancelNetworkSelection();
                        this.mSelectedUnavailableNotify = false;
                        return;
                    }
                    return;
                }
            }
            if (DBG) {
                log("updateNetworkSelection()...state = " + i + " not updating network due to invalid subId " + i2);
            }
        }
    }

    void postTransientNotification(int i, CharSequence charSequence) {
        if (this.mToast != null) {
            this.mToast.cancel();
        }
        this.mToast = Toast.makeText(this.mContext, charSequence, 1);
        this.mToast.show();
    }

    private void log(String str) {
        Log.d(LOG_TAG, str);
    }

    private void logi(String str) {
        Log.i(LOG_TAG, str);
    }
}
