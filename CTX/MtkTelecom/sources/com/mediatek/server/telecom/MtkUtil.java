package com.mediatek.server.telecom;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.sip.SipManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.telecom.DisconnectCause;
import android.telecom.Log;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallState;
import com.android.server.telecom.PhoneAccountRegistrar;
import com.android.server.telecom.R;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.TelephonyUtil;
import com.android.server.telecom.components.ErrorDialogActivity;
import com.mediatek.internal.telephony.MtkSubscriptionInfo;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class MtkUtil {
    private static final String TAG = MtkUtil.class.getSimpleName();
    private static Boolean sSipSupport = null;
    private static final ComponentName AOSP_PSTN_COMPONENT_NAME = new ComponentName("com.android.phone", "com.android.services.telephony.TelephonyConnectionService");
    private static final ComponentName MTK_PSTN_COMPONENT_NAME = new ComponentName("com.android.phone", "com.mediatek.services.telephony.MtkTelephonyConnectionService");

    public static boolean canVoiceRecord(String str, String str2) {
        if (checkCallingPermission(MtkTelecomGlobals.getInstance().getContext(), "android.permission.RECORD_AUDIO", str, str2)) {
            return true;
        }
        MtkTelecomGlobals.getInstance().showToast(R.string.denied_required_permission);
        return false;
    }

    private static boolean checkCallingPermission(Context context, String str, String str2, String str3) {
        try {
            context.enforceCallingPermission(str, str3);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService("appops");
            if (appOpsManager == null) {
                Log.w(TAG, "[checkCallingPermission]Failed to get AppOpsManager", new Object[0]);
                return false;
            }
            String strPermissionToOp = AppOpsManager.permissionToOp(str);
            int iNoteOp = appOpsManager.noteOp(strPermissionToOp, Binder.getCallingUid(), str2);
            Log.d(TAG, "[checkCallingPermission]permission: " + str + " -> op: " + strPermissionToOp + ", checking mode = " + iNoteOp, new Object[0]);
            return iNoteOp == 0;
        } catch (SecurityException e) {
            Log.e(TAG, e, "[checkCallerPermission]Permission checking failed for SDK level >= 23 caller. Permission: " + str, new Object[0]);
            return false;
        }
    }

    public static PhoneAccountHandle compatConvertPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle) {
        if (phoneAccountHandle != null && Objects.equals(phoneAccountHandle.getComponentName(), MTK_PSTN_COMPONENT_NAME)) {
            return new PhoneAccountHandle(AOSP_PSTN_COMPONENT_NAME, phoneAccountHandle.getId(), phoneAccountHandle.getUserHandle());
        }
        return phoneAccountHandle;
    }

    public static String dumpBundle(final Bundle bundle) {
        if (bundle == null) {
            return "null Bundle";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("Bundle[");
        bundle.keySet().stream().forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                MtkUtil.lambda$dumpBundle$0(sb, bundle, (String) obj);
            }
        });
        sb.append("]");
        return sb.toString();
    }

    static void lambda$dumpBundle$0(StringBuilder sb, Bundle bundle, String str) {
        sb.append(str.toString());
        sb.append(": ");
        sb.append(bundle.get(str.toString()));
        sb.append(", ");
    }

    public static boolean isImsCallIntent(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return false;
        }
        Uri data = intent.getData();
        boolean z = PhoneNumberUtils.isUriNumber(data.getSchemeSpecificPart()) && "tel".equals(data.getScheme());
        if (z) {
            Log.d(TAG, "[isImsCallIntent] Dealing with IMS number with @", new Object[0]);
        }
        return z;
    }

    public static int getSubIdForPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle) {
        PhoneAccount phoneAccount;
        if (!TelephonyUtil.isPstnComponentName(phoneAccountHandle.getComponentName())) {
            return -1;
        }
        TelephonyManager telephonyManager = (TelephonyManager) MtkTelecomGlobals.getInstance().getContext().getSystemService("phone");
        TelecomManager telecomManager = (TelecomManager) MtkTelecomGlobals.getInstance().getContext().getSystemService("telecom");
        TelecomSystem telecomSystem = TelecomSystem.getInstance();
        if (telecomSystem != null) {
            phoneAccount = telecomSystem.getPhoneAccountRegistrar().getPhoneAccountOfCurrentUser(phoneAccountHandle);
        } else {
            phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
        }
        if (phoneAccount == null) {
            Log.w(TAG, "[getSubId]PhoneAccount not registered, try SubscriptionManager, the iccId = " + phoneAccountHandle.getId(), new Object[0]);
            MtkSubscriptionInfo subInfoForIccId = MtkSubscriptionManager.getSubInfoForIccId((String) null, phoneAccountHandle.getId());
            if (subInfoForIccId == null) {
                return -1;
            }
            int subscriptionId = subInfoForIccId.getSubscriptionId();
            Log.d(TAG, "[getPhoneId]get subId from SubscriptionManager: " + subscriptionId, new Object[0]);
            return subscriptionId;
        }
        return telephonyManager.getSubIdForPhoneAccount(phoneAccount);
    }

    public static boolean isConferenceInvitation(Bundle bundle) {
        if (bundle == null) {
            return false;
        }
        return bundle.getBoolean("mediatek.telecom.extra.EXTRA_START_VOLTE_CONFERENCE", false);
    }

    public static boolean isIncomingConferenceCall(Bundle bundle) {
        boolean z;
        if (bundle != null && bundle.containsKey("mediatek.telecom.extra.EXTRA_INCOMING_VOLTE_CONFERENCE")) {
            z = bundle.getBoolean("mediatek.telecom.extra.EXTRA_INCOMING_VOLTE_CONFERENCE", false);
        } else {
            z = false;
        }
        Log.d(TAG, "[isIncomingConferenceCall]: " + z, new Object[0]);
        return z;
    }

    public static List<String> getConferenceInvitationNumbers(Bundle bundle) {
        if (bundle != null && bundle.containsKey("mediatek.telecom.extra.VOLTE_CONFERENCE_NUMBERS")) {
            return bundle.getStringArrayList("mediatek.telecom.extra.VOLTE_CONFERENCE_NUMBERS");
        }
        return null;
    }

    private static boolean checkConferencePermission(String str, String str2) {
        try {
            ReflectionHelper.callStaticMethod("com.mediatek.cta.CtaUtils", "enforceCheckPermission", str, "com.mediatek.permission.CTA_CONFERENCE_CALL", str2);
            return true;
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof SecurityException) {
                Log.w(TAG, "[canConference]No permission to make conference call for %s via %s", new Object[]{str, str2});
            } else {
                Log.w(TAG, "[canConference]Unexpected exception happened: %s", new Object[]{e.getTargetException()});
                e.getTargetException().printStackTrace();
            }
            return false;
        }
    }

    public static boolean canConference(String str, String str2) {
        if (checkConferencePermission(str, str2)) {
            return true;
        }
        MtkTelecomGlobals.getInstance().showToast(R.string.denied_required_permission);
        return false;
    }

    public static int cliValidityToPresentation(int i) {
        switch (i) {
            case CallState.NEW:
                return 1;
            case 1:
                return 2;
            case CallState.SELECT_PHONE_ACCOUNT:
                return 3;
            default:
                return -1;
        }
    }

    public static void showOutgoingFailedToast(String str) {
        Log.i(TAG, "[showOutgoingFailedToast] call failed: %s", new Object[]{str});
        MtkTelecomGlobals.getInstance().showToast(R.string.outgoing_call_failed);
    }

    public static List<PhoneAccountHandle> getSimPhoneAccountHandles() {
        ArrayList arrayList = new ArrayList();
        PhoneAccountRegistrar phoneAccountRegistrar = TelecomSystem.getInstance().getPhoneAccountRegistrar();
        if (phoneAccountRegistrar != null) {
            arrayList.addAll(phoneAccountRegistrar.getSimPhoneAccountsOfCurrentUser());
        }
        return arrayList;
    }

    public static PhoneAccountHandle getPhoneAccountHandleWithSlotId(Context context, int i, PhoneAccountHandle phoneAccountHandle) {
        PhoneAccountHandle next;
        if (SubscriptionManager.isValidSlotIndex(i)) {
            SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(i);
            List<PhoneAccountHandle> simPhoneAccountHandles = getSimPhoneAccountHandles();
            if (activeSubscriptionInfoForSimSlotIndex != null && simPhoneAccountHandles != null && !simPhoneAccountHandles.isEmpty()) {
                Iterator<PhoneAccountHandle> it = simPhoneAccountHandles.iterator();
                while (it.hasNext()) {
                    next = it.next();
                    if (Objects.equals(next.getId(), activeSubscriptionInfoForSimSlotIndex.getIccId())) {
                        break;
                    }
                }
                next = phoneAccountHandle;
            } else {
                next = phoneAccountHandle;
            }
        }
        Log.d(TAG, "getPhoneAccountHandleWithSlotId()... slotId = %s; account changed: %s => %s", new Object[]{Integer.valueOf(i), phoneAccountHandle, next});
        return next;
    }

    public static boolean isInDsdaMode() {
        return MtkTelephonyManagerEx.getDefault().isInDsdaMode();
    }

    public static boolean isMmiWithDataOff(DisconnectCause disconnectCause) {
        if (disconnectCause != null) {
            int code = disconnectCause.getCode();
            String reason = disconnectCause.getReason();
            if (code == 1 && !TextUtils.isEmpty(reason) && reason.contains("disconnect.reason.volte.ss.data.off")) {
                return true;
            }
        }
        return false;
    }

    public static void showNoDataDialog(Context context, PhoneAccountHandle phoneAccountHandle) {
        String string;
        int subIdForPhoneAccountHandle = getSubIdForPhoneAccountHandle(phoneAccountHandle);
        if (subIdForPhoneAccountHandle != -1) {
            Context contextCreatePackageContext = null;
            try {
                contextCreatePackageContext = context.createPackageContext("com.android.phone", 2);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, e, "showNoDataDialog() - Can't find Telephony package.", new Object[0]);
            }
            if (contextCreatePackageContext != null) {
                string = contextCreatePackageContext.getString(contextCreatePackageContext.getResources().getIdentifier("volte_ss_not_available_tips_data", "string", "com.android.phone"), getSubDisplayName(context, subIdForPhoneAccountHandle));
            } else {
                string = context.getString(R.string.volte_ss_not_available_tips, getSubDisplayName(context, subIdForPhoneAccountHandle));
            }
            Intent intent = new Intent(context, (Class<?>) ErrorDialogActivity.class);
            intent.putExtra("error_message_string", string);
            intent.setFlags(268435456);
            context.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }

    public static boolean isSipSupported() {
        if (sSipSupport == null) {
            sSipSupport = Boolean.valueOf(SipManager.isVoipSupported(MtkTelecomGlobals.getInstance().getContext()));
        }
        return sSipSupport.booleanValue();
    }

    private static String getSubDisplayName(Context context, int i) {
        String string = "";
        SubscriptionInfo activeSubscriptionInfo = SubscriptionManager.from(context).getActiveSubscriptionInfo(i);
        if (activeSubscriptionInfo != null) {
            string = activeSubscriptionInfo.getDisplayName().toString();
        }
        if (TextUtils.isEmpty(string)) {
            Log.d(TAG, "getSubDisplayName()... subId / subInfo: " + i + " / " + activeSubscriptionInfo, new Object[0]);
        }
        return string;
    }

    public static boolean isInSingleVideoCallMode(Call call) {
        boolean z;
        PersistableBundle configForSubId;
        Context context = MtkTelecomGlobals.getInstance().getContext();
        PhoneAccountHandle targetPhoneAccount = call.getTargetPhoneAccount();
        if (context != null && targetPhoneAccount != null && (configForSubId = ((CarrierConfigManager) context.getSystemService("carrier_config")).getConfigForSubId(getSubIdForPhoneAccountHandle(targetPhoneAccount))) != null) {
            z = configForSubId.getBoolean("mtk_allow_one_video_call_only_bool");
        } else {
            z = false;
        }
        Log.d(TAG, "isInSingleVideoCallMode()...result = %s", new Object[]{Boolean.valueOf(z)});
        return z;
    }
}
