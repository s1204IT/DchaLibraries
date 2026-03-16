package com.android.internal.telephony;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.IPhoneSubInfo;

public class PhoneSubInfoController extends IPhoneSubInfo.Stub {
    private static final String TAG = "PhoneSubInfoController";
    private Phone[] mPhone;

    public PhoneSubInfoController(Phone[] phone) {
        this.mPhone = phone;
        if (ServiceManager.getService("iphonesubinfo") == null) {
            ServiceManager.addService("iphonesubinfo", this);
        }
    }

    public String getDeviceId() {
        return getDeviceIdForPhone(SubscriptionManager.getPhoneId(getDefaultSubscription()));
    }

    public String getDeviceIdForPhone(int phoneId) {
        Phone phone = getPhone(phoneId);
        if (phone != null) {
            phone.getContext().enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", "Requires READ_PHONE_STATE");
            return phone.getDeviceId();
        }
        Rlog.e(TAG, "getDeviceIdForPhone phone " + phoneId + " is null");
        return null;
    }

    public String getNaiForSubscriber(int subId) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getNai();
        }
        Rlog.e(TAG, "getNai phoneSubInfoProxy is null for Subscription:" + subId);
        return null;
    }

    public String getImeiForSubscriber(int subId) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getImei();
        }
        Rlog.e(TAG, "getDeviceId phoneSubInfoProxy is null for Subscription:" + subId);
        return null;
    }

    public String getDeviceSvn() {
        return getDeviceSvnUsingSubId(getDefaultSubscription());
    }

    public String getDeviceSvnUsingSubId(int subId) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getDeviceSvn();
        }
        Rlog.e(TAG, "getDeviceSvn phoneSubInfoProxy is null");
        return null;
    }

    public String getSubscriberId() {
        return getSubscriberIdForSubscriber(getDefaultSubscription());
    }

    public String getSubscriberIdForSubscriber(int subId) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getSubscriberId();
        }
        Rlog.e(TAG, "getSubscriberId phoneSubInfoProxy is null for Subscription:" + subId);
        return null;
    }

    public String getIccSerialNumber() {
        return getIccSerialNumberForSubscriber(getDefaultSubscription());
    }

    public String getIccSerialNumberForSubscriber(int subId) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getIccSerialNumber();
        }
        Rlog.e(TAG, "getIccSerialNumber phoneSubInfoProxy is null for Subscription:" + subId);
        return null;
    }

    public String getLine1Number() {
        return getLine1NumberForSubscriber(getDefaultSubscription());
    }

    public String getLine1NumberForSubscriber(int subId) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getLine1Number();
        }
        Rlog.e(TAG, "getLine1Number phoneSubInfoProxy is null for Subscription:" + subId);
        return null;
    }

    public String getLine1AlphaTag() {
        return getLine1AlphaTagForSubscriber(getDefaultSubscription());
    }

    public String getLine1AlphaTagForSubscriber(int subId) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getLine1AlphaTag();
        }
        Rlog.e(TAG, "getLine1AlphaTag phoneSubInfoProxy is null for Subscription:" + subId);
        return null;
    }

    public String getMsisdn() {
        return getMsisdnForSubscriber(getDefaultSubscription());
    }

    public String getMsisdnForSubscriber(int subId) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getMsisdn();
        }
        Rlog.e(TAG, "getMsisdn phoneSubInfoProxy is null for Subscription:" + subId);
        return null;
    }

    public String getVoiceMailNumber() {
        return getVoiceMailNumberForSubscriber(getDefaultSubscription());
    }

    public String getVoiceMailNumberForSubscriber(int subId) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getVoiceMailNumber();
        }
        Rlog.e(TAG, "getVoiceMailNumber phoneSubInfoProxy is null for Subscription:" + subId);
        return null;
    }

    public String getCompleteVoiceMailNumber() {
        return getCompleteVoiceMailNumberForSubscriber(getDefaultSubscription());
    }

    public String getCompleteVoiceMailNumberForSubscriber(int subId) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getCompleteVoiceMailNumber();
        }
        Rlog.e(TAG, "getCompleteVoiceMailNumber phoneSubInfoProxy is null for Subscription:" + subId);
        return null;
    }

    public String getVoiceMailAlphaTag() {
        return getVoiceMailAlphaTagForSubscriber(getDefaultSubscription());
    }

    public String getVoiceMailAlphaTagForSubscriber(int subId) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getVoiceMailAlphaTag();
        }
        Rlog.e(TAG, "getVoiceMailAlphaTag phoneSubInfoProxy is null for Subscription:" + subId);
        return null;
    }

    private PhoneSubInfoProxy getPhoneSubInfoProxy(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        try {
            return getPhone(phoneId).getPhoneSubInfoProxy();
        } catch (NullPointerException e) {
            Rlog.e(TAG, "Exception is :" + e.toString() + " For subId :" + subId);
            e.printStackTrace();
            return null;
        }
    }

    private PhoneProxy getPhone(int phoneId) {
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            phoneId = 0;
        }
        return (PhoneProxy) this.mPhone[phoneId];
    }

    private int getDefaultSubscription() {
        return PhoneFactory.getDefaultSubscription();
    }

    public String getIsimImpi() {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(getDefaultSubscription());
        return phoneSubInfoProxy.getIsimImpi();
    }

    public String getIsimDomain() {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(getDefaultSubscription());
        return phoneSubInfoProxy.getIsimDomain();
    }

    public String[] getIsimImpu() {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(getDefaultSubscription());
        return phoneSubInfoProxy.getIsimImpu();
    }

    public String getIsimIst() throws RemoteException {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(getDefaultSubscription());
        return phoneSubInfoProxy.getIsimIst();
    }

    public String[] getIsimPcscf() throws RemoteException {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(getDefaultSubscription());
        return phoneSubInfoProxy.getIsimPcscf();
    }

    public String getIsimChallengeResponse(String nonce) throws RemoteException {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(getDefaultSubscription());
        return phoneSubInfoProxy.getIsimChallengeResponse(nonce);
    }

    public String getIccSimChallengeResponse(int subId, int appType, String data) throws RemoteException {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        return phoneSubInfoProxy.getIccSimChallengeResponse(subId, appType, data);
    }

    public String getGroupIdLevel1() {
        return getGroupIdLevel1ForSubscriber(getDefaultSubscription());
    }

    public String getGroupIdLevel1ForSubscriber(int subId) {
        PhoneSubInfoProxy phoneSubInfoProxy = getPhoneSubInfoProxy(subId);
        if (phoneSubInfoProxy != null) {
            return phoneSubInfoProxy.getGroupIdLevel1();
        }
        Rlog.e(TAG, "getGroupIdLevel1 phoneSubInfoProxy is null for Subscription:" + subId);
        return null;
    }
}
