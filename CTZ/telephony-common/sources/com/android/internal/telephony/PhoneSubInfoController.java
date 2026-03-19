package com.android.internal.telephony;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.IPhoneSubInfo;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;

public class PhoneSubInfoController extends IPhoneSubInfo.Stub {
    private static final boolean DBG = true;
    private static final String TAG = "PhoneSubInfoController";
    private static final boolean VDBG = false;
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private final Phone[] mPhone;

    public PhoneSubInfoController(Context context, Phone[] phoneArr) {
        this.mPhone = phoneArr;
        if (ServiceManager.getService("iphonesubinfo") == null) {
            ServiceManager.addService("iphonesubinfo", this);
        }
        this.mContext = context;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
    }

    public String getDeviceId(String str) {
        return getDeviceIdForPhone(SubscriptionManager.getPhoneId(getDefaultSubscription()), str);
    }

    public String getDeviceIdForPhone(int i, String str) {
        if (!SubscriptionManager.isValidPhoneId(i)) {
            i = 0;
        }
        Phone phone = this.mPhone[i];
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, phone.getSubId(), str, "getDeviceId")) {
                return null;
            }
            return phone.getDeviceId();
        }
        loge("getDeviceIdForPhone phone " + i + " is null");
        return null;
    }

    public String getNaiForSubscriber(int i, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getNai")) {
                return null;
            }
            return phone.getNai();
        }
        loge("getNai phone is null for Subscription:" + i);
        return null;
    }

    public String getImeiForSubscriber(int i, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getImei")) {
                return null;
            }
            return phone.getImei();
        }
        loge("getDeviceId phone is null for Subscription:" + i);
        return null;
    }

    public ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int i, int i2, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getCarrierInfoForImsiEncryption")) {
                return null;
            }
            return phone.getCarrierInfoForImsiEncryption(i2);
        }
        loge("getCarrierInfoForImsiEncryption phone is null for Subscription:" + i);
        return null;
    }

    public void setCarrierInfoForImsiEncryption(int i, String str, ImsiEncryptionInfo imsiEncryptionInfo) {
        Phone phone = getPhone(i);
        if (phone != null) {
            enforceModifyPermission();
            phone.setCarrierInfoForImsiEncryption(imsiEncryptionInfo);
        } else {
            loge("setCarrierInfoForImsiEncryption phone is null for Subscription:" + i);
        }
    }

    public void resetCarrierKeysForImsiEncryption(int i, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            enforceModifyPermission();
            phone.resetCarrierKeysForImsiEncryption();
        } else {
            loge("resetCarrierKeysForImsiEncryption phone is null for Subscription:" + i);
        }
    }

    public String getDeviceSvn(String str) {
        return getDeviceSvnUsingSubId(getDefaultSubscription(), str);
    }

    public String getDeviceSvnUsingSubId(int i, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getDeviceSvn")) {
                return null;
            }
            return phone.getDeviceSvn();
        }
        loge("getDeviceSvn phone is null");
        return null;
    }

    public String getSubscriberId(String str) {
        return getSubscriberIdForSubscriber(getDefaultSubscription(), str);
    }

    public String getSubscriberIdForSubscriber(int i, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getSubscriberId")) {
                return null;
            }
            return phone.getSubscriberId();
        }
        loge("getSubscriberId phone is null for Subscription:" + i);
        return null;
    }

    public String getIccSerialNumber(String str) {
        return getIccSerialNumberForSubscriber(getDefaultSubscription(), str);
    }

    public String getIccSerialNumberForSubscriber(int i, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getIccSerialNumber")) {
                return null;
            }
            return phone.getIccSerialNumber();
        }
        loge("getIccSerialNumber phone is null for Subscription:" + i);
        return null;
    }

    public String getLine1Number(String str) {
        return getLine1NumberForSubscriber(getDefaultSubscription(), str);
    }

    public String getLine1NumberForSubscriber(int i, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneNumber(this.mContext, i, str, "getLine1Number")) {
                return null;
            }
            return phone.getLine1Number();
        }
        loge("getLine1Number phone is null for Subscription:" + i);
        return null;
    }

    public String getLine1AlphaTag(String str) {
        return getLine1AlphaTagForSubscriber(getDefaultSubscription(), str);
    }

    public String getLine1AlphaTagForSubscriber(int i, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getLine1AlphaTag")) {
                return null;
            }
            return phone.getLine1AlphaTag();
        }
        loge("getLine1AlphaTag phone is null for Subscription:" + i);
        return null;
    }

    public String getMsisdn(String str) {
        return getMsisdnForSubscriber(getDefaultSubscription(), str);
    }

    public String getMsisdnForSubscriber(int i, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getMsisdn")) {
                return null;
            }
            return phone.getMsisdn();
        }
        loge("getMsisdn phone is null for Subscription:" + i);
        return null;
    }

    public String getVoiceMailNumber(String str) {
        return getVoiceMailNumberForSubscriber(getDefaultSubscription(), str);
    }

    public String getVoiceMailNumberForSubscriber(int i, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getVoiceMailNumber")) {
                return null;
            }
            return PhoneNumberUtils.extractNetworkPortion(phone.getVoiceMailNumber());
        }
        loge("getVoiceMailNumber phone is null for Subscription:" + i);
        return null;
    }

    public String getCompleteVoiceMailNumber() {
        return getCompleteVoiceMailNumberForSubscriber(getDefaultSubscription());
    }

    public String getCompleteVoiceMailNumberForSubscriber(int i) {
        Phone phone = getPhone(i);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CALL_PRIVILEGED", "Requires CALL_PRIVILEGED");
            return phone.getVoiceMailNumber();
        }
        loge("getCompleteVoiceMailNumber phone is null for Subscription:" + i);
        return null;
    }

    public String getVoiceMailAlphaTag(String str) {
        return getVoiceMailAlphaTagForSubscriber(getDefaultSubscription(), str);
    }

    public String getVoiceMailAlphaTagForSubscriber(int i, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getVoiceMailAlphaTag")) {
                return null;
            }
            return phone.getVoiceMailAlphaTag();
        }
        loge("getVoiceMailAlphaTag phone is null for Subscription:" + i);
        return null;
    }

    private Phone getPhone(int i) {
        int phoneId = SubscriptionManager.getPhoneId(i);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            phoneId = 0;
        }
        return this.mPhone[phoneId];
    }

    private void enforcePrivilegedPermissionOrCarrierPrivilege(int i, String str) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE") == 0) {
            return;
        }
        TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(i, str);
    }

    private void enforceModifyPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", "Requires MODIFY_PHONE_STATE");
    }

    private int getDefaultSubscription() {
        return PhoneFactory.getDefaultSubscription();
    }

    public String getIsimImpi(int i) {
        Phone phone = getPhone(i);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
            IsimRecords isimRecords = phone.getIsimRecords();
            if (isimRecords == null) {
                return null;
            }
            return isimRecords.getIsimImpi();
        }
        loge("getIsimImpi phone is null for Subscription:" + i);
        return null;
    }

    public String getIsimDomain(int i) {
        Phone phone = getPhone(i);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
            IsimRecords isimRecords = phone.getIsimRecords();
            if (isimRecords == null) {
                return null;
            }
            return isimRecords.getIsimDomain();
        }
        loge("getIsimDomain phone is null for Subscription:" + i);
        return null;
    }

    public String[] getIsimImpu(int i) {
        Phone phone = getPhone(i);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
            IsimRecords isimRecords = phone.getIsimRecords();
            if (isimRecords == null) {
                return null;
            }
            return isimRecords.getIsimImpu();
        }
        loge("getIsimImpu phone is null for Subscription:" + i);
        return null;
    }

    public String getIsimIst(int i) throws RemoteException {
        Phone phone = getPhone(i);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
            IsimRecords isimRecords = phone.getIsimRecords();
            if (isimRecords == null) {
                return null;
            }
            return isimRecords.getIsimIst();
        }
        loge("getIsimIst phone is null for Subscription:" + i);
        return null;
    }

    public String[] getIsimPcscf(int i) throws RemoteException {
        Phone phone = getPhone(i);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
            IsimRecords isimRecords = phone.getIsimRecords();
            if (isimRecords == null) {
                return null;
            }
            return isimRecords.getIsimPcscf();
        }
        loge("getIsimPcscf phone is null for Subscription:" + i);
        return null;
    }

    public String getIccSimChallengeResponse(int i, int i2, int i3, String str) throws RemoteException {
        enforcePrivilegedPermissionOrCarrierPrivilege(i, "getIccSimChallengeResponse");
        UiccCard uiccCard = getPhone(i).getUiccCard();
        if (uiccCard == null) {
            loge("getIccSimChallengeResponse() UiccCard is null");
            return null;
        }
        UiccCardApplication applicationByType = uiccCard.getApplicationByType(i2);
        if (applicationByType == null) {
            loge("getIccSimChallengeResponse() no app with specified type -- " + i2);
            return null;
        }
        loge("getIccSimChallengeResponse() found app " + applicationByType.getAid() + " specified type -- " + i2);
        if (i3 != 128 && i3 != 129) {
            loge("getIccSimChallengeResponse() unsupported authType: " + i3);
            return null;
        }
        return applicationByType.getIccRecords().getIccSimChallengeResponse(i3, str);
    }

    public String getGroupIdLevel1ForSubscriber(int i, String str) {
        Phone phone = getPhone(i);
        if (phone != null) {
            if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, i, str, "getGroupIdLevel1")) {
                return null;
            }
            return phone.getGroupIdLevel1();
        }
        loge("getGroupIdLevel1 phone is null for Subscription:" + i);
        return null;
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }

    private void loge(String str) {
        Rlog.e(TAG, str);
    }
}
