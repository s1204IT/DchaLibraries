package com.android.internal.telephony;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Binder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.IPhoneSubInfo;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UsimServiceTable;

public class PhoneSubInfoController extends IPhoneSubInfo.Stub {
    private static final boolean DBG = true;
    private static final String TAG = "PhoneSubInfoController";
    private static final boolean VDBG = false;
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private final Phone[] mPhone;

    public PhoneSubInfoController(Context context, Phone[] phone) {
        this.mPhone = phone;
        if (ServiceManager.getService("iphonesubinfo") == null) {
            ServiceManager.addService("iphonesubinfo", this);
        }
        this.mContext = context;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
    }

    public String getDeviceId(String callingPackage) {
        return getDeviceIdForPhone(SubscriptionManager.getPhoneId(getDefaultSubscription()), callingPackage);
    }

    public String getDeviceIdForPhone(int phoneId, String callingPackage) {
        if (!checkReadPhoneState(callingPackage, "getDeviceId")) {
            return null;
        }
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            phoneId = 0;
        }
        Phone phone = this.mPhone[phoneId];
        if (phone != null) {
            phone.getContext().enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", "Requires READ_PHONE_STATE");
            return phone.getDeviceId();
        }
        loge("getDeviceIdForPhone phone " + phoneId + " is null");
        return null;
    }

    public String getNaiForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            if (checkReadPhoneState(callingPackage, "getNai")) {
                return phone.getNai();
            }
            return null;
        }
        loge("getNai phone is null for Subscription:" + subId);
        return null;
    }

    public String getImeiForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            if (checkReadPhoneState(callingPackage, "getImei")) {
                return phone.getImei();
            }
            return null;
        }
        loge("getDeviceId phone is null for Subscription:" + subId);
        return null;
    }

    public String getDeviceSvn(String callingPackage) {
        return getDeviceSvnUsingSubId(getDefaultSubscription(), callingPackage);
    }

    public String getDeviceSvnUsingSubId(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            if (checkReadPhoneState(callingPackage, "getDeviceSvn")) {
                return phone.getDeviceSvn();
            }
            return null;
        }
        loge("getDeviceSvn phone is null");
        return null;
    }

    public String getSubscriberId(String callingPackage) {
        return getSubscriberIdForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getSubscriberIdForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            if (checkReadPhoneState(callingPackage, "getSubscriberId")) {
                return phone.getSubscriberId();
            }
            return null;
        }
        loge("getSubscriberId phone is null for Subscription:" + subId);
        return null;
    }

    public String getIccSerialNumber(String callingPackage) {
        return getIccSerialNumberForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getIccSerialNumberForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            if (checkReadPhoneState(callingPackage, "getIccSerialNumber")) {
                return phone.getIccSerialNumber();
            }
            return null;
        }
        loge("getIccSerialNumber phone is null for Subscription:" + subId);
        return null;
    }

    public String getLine1Number(String callingPackage) {
        return getLine1NumberForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getLine1NumberForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            if (checkReadPhoneNumber(callingPackage, "getLine1Number")) {
                return phone.getLine1Number();
            }
            return null;
        }
        loge("getLine1Number phone is null for Subscription:" + subId);
        return null;
    }

    public String getLine1AlphaTag(String callingPackage) {
        return getLine1AlphaTagForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getLine1AlphaTagForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            if (checkReadPhoneState(callingPackage, "getLine1AlphaTag")) {
                return phone.getLine1AlphaTag();
            }
            return null;
        }
        loge("getLine1AlphaTag phone is null for Subscription:" + subId);
        return null;
    }

    public String getMsisdn(String callingPackage) {
        return getMsisdnForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getMsisdnForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            if (checkReadPhoneState(callingPackage, "getMsisdn")) {
                return phone.getMsisdn();
            }
            return null;
        }
        loge("getMsisdn phone is null for Subscription:" + subId);
        return null;
    }

    public String getVoiceMailNumber(String callingPackage) {
        return getVoiceMailNumberForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getVoiceMailNumberForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            if (!checkReadPhoneState(callingPackage, "getVoiceMailNumber")) {
                return null;
            }
            String number = PhoneNumberUtils.extractNetworkPortion(phone.getVoiceMailNumber());
            return number;
        }
        loge("getVoiceMailNumber phone is null for Subscription:" + subId);
        return null;
    }

    public String getCompleteVoiceMailNumber() {
        return getCompleteVoiceMailNumberForSubscriber(getDefaultSubscription());
    }

    public String getCompleteVoiceMailNumberForSubscriber(int subId) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CALL_PRIVILEGED", "Requires CALL_PRIVILEGED");
            String number = phone.getVoiceMailNumber();
            return number;
        }
        loge("getCompleteVoiceMailNumber phone is null for Subscription:" + subId);
        return null;
    }

    public String getVoiceMailAlphaTag(String callingPackage) {
        return getVoiceMailAlphaTagForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getVoiceMailAlphaTagForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            if (checkReadPhoneState(callingPackage, "getVoiceMailAlphaTag")) {
                return phone.getVoiceMailAlphaTag();
            }
            return null;
        }
        loge("getVoiceMailAlphaTag phone is null for Subscription:" + subId);
        return null;
    }

    private Phone getPhone(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            phoneId = 0;
        }
        return this.mPhone[phoneId];
    }

    private void enforcePrivilegedPermissionOrCarrierPrivilege(Phone phone) {
        int permissionResult = this.mContext.checkCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE");
        if (permissionResult == 0) {
            return;
        }
        log("No read privileged phone permission, check carrier privilege next.");
        UiccCard uiccCard = phone.getUiccCard();
        if (uiccCard == null) {
            throw new SecurityException("No Carrier Privilege: No UICC");
        }
        if (uiccCard.getCarrierPrivilegeStatusForCurrentTransaction(this.mContext.getPackageManager()) == 1) {
        } else {
            throw new SecurityException("No Carrier Privilege.");
        }
    }

    private int getDefaultSubscription() {
        return PhoneFactory.getDefaultSubscription();
    }

    public String getIsimImpi() {
        Phone phone = getPhone(getDefaultSubscription());
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        IsimRecords isim = phone.getIsimRecords();
        if (isim != null) {
            return isim.getIsimImpi();
        }
        return null;
    }

    public String getIsimDomain() {
        Phone phone = getPhone(getDefaultSubscription());
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        IsimRecords isim = phone.getIsimRecords();
        if (isim != null) {
            return isim.getIsimDomain();
        }
        return null;
    }

    public String[] getIsimImpu() {
        Phone phone = getPhone(getDefaultSubscription());
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        IsimRecords isim = phone.getIsimRecords();
        if (isim != null) {
            return isim.getIsimImpu();
        }
        return null;
    }

    public String getIsimIst() throws RemoteException {
        Phone phone = getPhone(getDefaultSubscription());
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        IsimRecords isim = phone.getIsimRecords();
        if (isim != null) {
            return isim.getIsimIst();
        }
        return null;
    }

    public String[] getIsimPcscf() throws RemoteException {
        Phone phone = getPhone(getDefaultSubscription());
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        IsimRecords isim = phone.getIsimRecords();
        if (isim != null) {
            return isim.getIsimPcscf();
        }
        return null;
    }

    public String getIsimChallengeResponse(String nonce) throws RemoteException {
        Phone phone = getPhone(getDefaultSubscription());
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        IsimRecords isim = phone.getIsimRecords();
        if (isim != null) {
            return isim.getIsimChallengeResponse(nonce);
        }
        return null;
    }

    public String getIccSimChallengeResponse(int subId, int appType, int authType, String data) throws RemoteException {
        Phone phone = getPhone(subId);
        enforcePrivilegedPermissionOrCarrierPrivilege(phone);
        UiccCard uiccCard = phone.getUiccCard();
        if (uiccCard == null) {
            loge("getIccSimChallengeResponse() UiccCard is null");
            return null;
        }
        UiccCardApplication uiccApp = uiccCard.getApplicationByType(appType);
        if (uiccApp == null) {
            loge("getIccSimChallengeResponse() no app with specified type -- " + appType);
            return null;
        }
        loge("getIccSimChallengeResponse() found app " + uiccApp.getAid() + " specified type -- " + appType);
        if (authType != 128 && authType != 129) {
            loge("getIccSimChallengeResponse() unsupported authType: " + authType);
            return null;
        }
        return uiccApp.getIccRecords().getIccSimChallengeResponse(authType, data);
    }

    public String getGroupIdLevel1(String callingPackage) {
        return getGroupIdLevel1ForSubscriber(getDefaultSubscription(), callingPackage);
    }

    public String getGroupIdLevel1ForSubscriber(int subId, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            if (checkReadPhoneState(callingPackage, "getGroupIdLevel1")) {
                return phone.getGroupIdLevel1();
            }
            return null;
        }
        loge("getGroupIdLevel1 phone is null for Subscription:" + subId);
        return null;
    }

    private boolean checkReadPhoneState(String callingPackage, String message) {
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", message);
            return true;
        } catch (SecurityException e) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", message);
            return this.mAppOps.noteOp(51, Binder.getCallingUid(), callingPackage) == 0;
        }
    }

    private boolean checkReadPhoneNumber(String callingPackage, String message) {
        if (this.mAppOps.noteOp(15, Binder.getCallingUid(), callingPackage) == 0) {
            return true;
        }
        try {
            return checkReadPhoneState(callingPackage, message);
        } catch (SecurityException e) {
            try {
                this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SMS", message);
                return this.mAppOps.noteOp(14, Binder.getCallingUid(), callingPackage) == 0;
            } catch (SecurityException e2) {
                throw new SecurityException(message + ": Neither user " + Binder.getCallingUid() + " nor current process has android.permission.READ_PHONE_STATE or android.permission.READ_SMS.");
            }
        }
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s) {
        Rlog.e(TAG, s);
    }

    public String getIsimImpiForSubscriber(int subId) {
        Phone phone = getPhone(subId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        if (phone != null) {
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                return isim.getIsimImpi();
            }
            return null;
        }
        loge("getIsimImpi phone is null for Subscription:" + subId);
        return null;
    }

    public String getIsimDomainForSubscriber(int subId) {
        Phone phone = getPhone(subId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        if (phone != null) {
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                return isim.getIsimDomain();
            }
            return null;
        }
        loge("getIsimDomain phone is null for Subscription:" + subId);
        return null;
    }

    public String[] getIsimImpuForSubscriber(int subId) {
        Phone phone = getPhone(subId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        if (phone != null) {
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                return isim.getIsimImpu();
            }
            return null;
        }
        loge("getIsimImpu phone is null for Subscription:" + subId);
        return null;
    }

    public String getIsimIstForSubscriber(int subId) {
        Phone phone = getPhone(subId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        if (phone != null) {
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                return isim.getIsimIst();
            }
            return null;
        }
        loge("getIsimIst phone is null for Subscription:" + subId);
        return null;
    }

    public String[] getIsimPcscfForSubscriber(int subId) {
        Phone phone = getPhone(subId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        if (phone != null) {
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                return isim.getIsimPcscf();
            }
            return null;
        }
        loge("getIsimPcscf phone is null for Subscription:" + subId);
        return null;
    }

    public String getIsimGbabp() {
        return getIsimGbabpForSubscriber(getDefaultSubscription());
    }

    public String getIsimGbabpForSubscriber(int subId) {
        Phone phone = getPhone(subId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        if (phone != null) {
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                return isim.getIsimGbabp();
            }
            return null;
        }
        loge("getIsimGbabp phone is null for Subscription:" + subId);
        return null;
    }

    public void setIsimGbabp(String gbabp, Message onComplete) {
        setIsimGbabpForSubscriber(getDefaultSubscription(), gbabp, onComplete);
    }

    public void setIsimGbabpForSubscriber(int subId, String gbabp, Message onComplete) {
        Phone phone = getPhone(subId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        if (phone != null) {
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                isim.setIsimGbabp(gbabp, onComplete);
                return;
            } else {
                loge("setIsimGbabp isim is null for Subscription:" + subId);
                return;
            }
        }
        loge("setIsimGbabp phone is null for Subscription:" + subId);
    }

    private IccRecords getIccRecords(int subId) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            UiccCard uiccCard = phone.getUiccCard();
            UiccCardApplication uiccApp = uiccCard != null ? uiccCard.getApplication(1) : null;
            if (uiccApp == null) {
                return null;
            }
            IccRecords iccRecords = uiccApp.getIccRecords();
            return iccRecords;
        }
        loge("getIccRecords phone is null for Subscription:" + subId);
        return null;
    }

    public boolean getUsimService(int service, String callingPackage) {
        return getUsimServiceForSubscriber(getDefaultSubscription(), service, callingPackage);
    }

    public boolean getUsimServiceForSubscriber(int subId, int service, String callingPackage) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            if (!checkReadPhoneState(callingPackage, "getUsimService")) {
                return false;
            }
            UsimServiceTable ust = phone.getUsimServiceTable();
            if (ust != null) {
                return ust.isAvailable(service);
            }
            log("getUsimService fail due to UST is null.");
            return false;
        }
        loge("getUsimService phone is null for Subscription:" + subId);
        return false;
    }

    public String getUsimGbabp() {
        return getUsimGbabpForSubscriber(getDefaultSubscription());
    }

    public String getUsimGbabpForSubscriber(int subId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        IccRecords iccRecords = getIccRecords(subId);
        if (iccRecords != null) {
            return iccRecords.getEfGbabp();
        }
        loge("getUsimGbabp iccRecords is null for Subscription:" + subId);
        return null;
    }

    public void setUsimGbabp(String gbabp, Message onComplete) {
        setUsimGbabpForSubscriber(getDefaultSubscription(), gbabp, onComplete);
    }

    public void setUsimGbabpForSubscriber(int subId, String gbabp, Message onComplete) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        IccRecords iccRecords = getIccRecords(subId);
        if (iccRecords != null) {
            iccRecords.setEfGbabp(gbabp, onComplete);
        } else {
            loge("setUsimGbabp iccRecords is null for Subscription:" + subId);
        }
    }

    public byte[] getIsimPsismsc() {
        return getIsimPsismscForSubscriber(getDefaultSubscription());
    }

    public byte[] getIsimPsismscForSubscriber(int subId) {
        Phone phone = getPhone(subId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        if (phone != null) {
            IsimRecords isim = phone.getIsimRecords();
            if (isim != null) {
                return isim.getEfPsismsc();
            }
            return null;
        }
        loge("getIsimPsismsc phone is null for Subscription:" + subId);
        return null;
    }

    public byte[] getUsimPsismsc() {
        return getUsimPsismscForSubscriber(getDefaultSubscription());
    }

    public byte[] getUsimPsismscForSubscriber(int subId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        IccRecords iccRecords = getIccRecords(subId);
        if (iccRecords != null) {
            return iccRecords.getEfPsismsc();
        }
        loge("getUsimPsismsc iccRecords is null for Subscription:" + subId);
        return null;
    }

    public byte[] getUsimSmsp() {
        return getUsimSmspForSubscriber(getDefaultSubscription());
    }

    public byte[] getUsimSmspForSubscriber(int subId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        IccRecords iccRecords = getIccRecords(subId);
        if (iccRecords != null) {
            return iccRecords.getEfSmsp();
        }
        loge("getUsimSmsp iccRecords is null for Subscription:" + subId);
        return null;
    }

    public int getMncLength() {
        return getMncLengthForSubscriber(getDefaultSubscription());
    }

    public int getMncLengthForSubscriber(int subId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", "Requires READ_PRIVILEGED_PHONE_STATE");
        IccRecords iccRecords = getIccRecords(subId);
        if (iccRecords != null) {
            return iccRecords.getMncLength();
        }
        loge("getMncLength iccRecords is null for Subscription:" + subId);
        return 0;
    }
}
