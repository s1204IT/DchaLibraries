package com.android.internal.telephony;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.uicc.AdnRecord;
import java.util.List;

public class UiccPhoneBookController extends IIccPhoneBook.Stub {
    private static final String TAG = "UiccPhoneBookController";
    private Phone[] mPhone;

    public UiccPhoneBookController(Phone[] phone) {
        if (ServiceManager.getService("simphonebook") == null) {
            ServiceManager.addService("simphonebook", this);
        }
        this.mPhone = phone;
    }

    @Override
    public boolean updateAdnRecordsInEfBySearch(int efid, String oldTag, String oldPhoneNumber, String newTag, String newPhoneNumber, String pin2) throws RemoteException {
        return updateAdnRecordsInEfBySearchForSubscriber(getDefaultSubscription(), efid, oldTag, oldPhoneNumber, newTag, newPhoneNumber, pin2);
    }

    @Override
    public boolean updateAdnRecordsInEfBySearchForSubscriber(int subId, int efid, String oldTag, String oldPhoneNumber, String newTag, String newPhoneNumber, String pin2) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.updateAdnRecordsInEfBySearch(efid, oldTag, oldPhoneNumber, newTag, newPhoneNumber, pin2);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgrProxy is null for Subscription:" + subId);
        return false;
    }

    @Override
    public boolean updateAdnRecordsInEfByIndex(int efid, String newTag, String newPhoneNumber, int index, String pin2) throws RemoteException {
        return updateAdnRecordsInEfByIndexForSubscriber(getDefaultSubscription(), efid, newTag, newPhoneNumber, index, pin2);
    }

    @Override
    public boolean updateAdnRecordsInEfByIndexForSubscriber(int subId, int efid, String newTag, String newPhoneNumber, int index, String pin2) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.updateAdnRecordsInEfByIndex(efid, newTag, newPhoneNumber, index, pin2);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfByIndex iccPbkIntMgrProxy is null for Subscription:" + subId);
        return false;
    }

    @Override
    public int[] getAdnRecordsSize(int efid) throws RemoteException {
        return getAdnRecordsSizeForSubscriber(getDefaultSubscription(), efid);
    }

    @Override
    public int[] getAdnRecordsSizeForSubscriber(int subId, int efid) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.getAdnRecordsSize(efid);
        }
        Rlog.e(TAG, "getAdnRecordsSize iccPbkIntMgrProxy is null for Subscription:" + subId);
        return null;
    }

    @Override
    public List<AdnRecord> getAdnRecordsInEf(int efid) throws RemoteException {
        return getAdnRecordsInEfForSubscriber(getDefaultSubscription(), efid);
    }

    @Override
    public List<AdnRecord> getAdnRecordsInEfForSubscriber(int subId, int efid) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.getAdnRecordsInEf(efid);
        }
        Rlog.e(TAG, "getAdnRecordsInEf iccPbkIntMgrProxy isnull for Subscription:" + subId);
        return null;
    }

    private IccPhoneBookInterfaceManagerProxy getIccPhoneBookInterfaceManagerProxy(int subId) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        try {
            return ((PhoneProxy) this.mPhone[phoneId]).getIccPhoneBookInterfaceManagerProxy();
        } catch (ArrayIndexOutOfBoundsException e) {
            Rlog.e(TAG, "Exception is :" + e.toString() + " For subscription :" + subId);
            e.printStackTrace();
            return null;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "Exception is :" + e2.toString() + " For subscription :" + subId);
            e2.printStackTrace();
            return null;
        }
    }

    private int getDefaultSubscription() {
        return PhoneFactory.getDefaultSubscription();
    }

    @Override
    public boolean updateAdnRecordsInEfBySearch2(int efid, String oldTag, String oldPhoneNumber, String[] oldEmails, String oldPhoneNumber2, String oldSne, byte[] oldGrps, String newTag, String newPhoneNumber, String[] newEmails, String newPhoneNumber2, String newSne, byte[] newGrps, String pin2) throws RemoteException {
        return updateAdnRecordsInEfBySearch2UsingSubId(getDefaultSubscription(), efid, oldTag, oldPhoneNumber, oldEmails, oldPhoneNumber2, oldSne, oldGrps, newTag, newPhoneNumber, newEmails, newPhoneNumber2, newSne, newGrps, pin2);
    }

    @Override
    public boolean updateAdnRecordsInEfBySearch2UsingSubId(int subId, int efid, String oldTag, String oldPhoneNumber, String[] oldEmails, String oldPhoneNumber2, String oldSne, byte[] oldGrps, String newTag, String newPhoneNumber, String[] newEmails, String newPhoneNumber2, String newSne, byte[] newGrps, String pin2) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.updateAdnRecordsInEfBySearch2(efid, oldTag, oldPhoneNumber, oldEmails, oldPhoneNumber2, oldSne, oldGrps, newTag, newPhoneNumber, newEmails, newPhoneNumber2, newSne, newGrps, pin2);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch2 iccPbkIntMgrProxy isnull for Subscription:" + subId);
        return false;
    }

    @Override
    public boolean updateAdnRecordsGrpInEfBySearch(int efType, String name, String number, String email, String number2, byte[] grps, byte addgrp, String pin2) throws RemoteException {
        return updateAdnRecordsGrpInEfBySearchUsingSubId(getDefaultSubscription(), efType, name, number, email, number2, grps, addgrp, pin2);
    }

    @Override
    public boolean updateAdnRecordsGrpInEfBySearchUsingSubId(int subId, int efType, String name, String number, String email, String number2, byte[] grps, byte addgrp, String pin2) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.updateAdnRecordsGrpInEfBySearch(efType, name, number, email, number2, grps, addgrp, pin2);
        }
        Rlog.e(TAG, "getGrpupdateAdnRecordsGrpInEfBySearch iccPbkIntMgrProxy isnull for Subscription:" + subId);
        return false;
    }

    @Override
    public boolean updateAdnRecordsGrpTagInEfByIndex(int efid, String grpId, String grpTag, String pin2) throws RemoteException {
        return updateAdnRecordsGrpTagInEfByIndexUsingSubId(getDefaultSubscription(), efid, grpId, grpTag, pin2);
    }

    @Override
    public boolean updateAdnRecordsGrpTagInEfByIndexUsingSubId(int subId, int efid, String grpId, String grpTag, String pin2) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.updateAdnRecordsGrpTagInEfByIndex(efid, grpId, grpTag, pin2);
        }
        Rlog.e(TAG, "updateAdnRecordsGrpTagInEfByIndex iccPbkIntMgrProxy isnull for Subscription:" + subId);
        return false;
    }

    @Override
    public String[] getGrpRecords(int efid) throws RemoteException {
        return getGrpRecordsUsingSubId(getDefaultSubscription(), efid);
    }

    @Override
    public String[] getGrpRecordsUsingSubId(int subId, int efid) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.getGrpRecords(efid);
        }
        Rlog.e(TAG, "getGrpRecords iccPbkIntMgrProxy isnull for Subscription:" + subId);
        return null;
    }

    @Override
    public int getPBInitCount() throws RemoteException {
        return getPBInitCountUsingSubId(getDefaultSubscription());
    }

    @Override
    public int getPBInitCountUsingSubId(int subId) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.getPBInitCount();
        }
        Rlog.e(TAG, "getPBInitCount iccPbkIntMgrProxy isnull for Subscription:" + subId);
        return 0;
    }

    @Override
    public int getEmailFieldSize() throws RemoteException {
        return getEmailFieldSizeUsingSubId(getDefaultSubscription());
    }

    @Override
    public int getEmailFieldSizeUsingSubId(int subId) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.getEmailFieldSize();
        }
        Rlog.e(TAG, "getEmailFieldSize iccPbkIntMgrProxy isnull for Subscription:" + subId);
        return 0;
    }

    @Override
    public boolean isSneFieldEnable() throws RemoteException {
        return isSneFieldEnableUsingSubId(getDefaultSubscription());
    }

    @Override
    public boolean isSneFieldEnableUsingSubId(int subId) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.isSneFieldEnable();
        }
        Rlog.e(TAG, "isSneFieldEnableUsingSubId iccPbkIntMgrProxy isnull for Subscription:" + subId);
        return false;
    }

    @Override
    public int getTotalAdnRecordsSize() throws RemoteException {
        return getTotalAdnRecordsSizeUsingSubId(getDefaultSubscription());
    }

    @Override
    public int getTotalAdnRecordsSizeUsingSubId(int subId) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.getTotalAdnRecordsSize();
        }
        Rlog.e(TAG, "getTotalAdnRecordsSize iccPbkIntMgrProxy isnull for Subscription:" + subId);
        return 0;
    }

    @Override
    public boolean isSimContactsLoaded() throws RemoteException {
        return isSimContactsLoadedUsingSubId(getDefaultSubscription());
    }

    @Override
    public boolean isSimContactsLoadedUsingSubId(int subId) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.isSimContactsLoaded();
        }
        Rlog.e(TAG, "isSimContactsLoaded iccPbkIntMgrProxy isnull for Subscription:" + subId);
        return false;
    }

    @Override
    public int[] getContactItemMaxLength() throws RemoteException {
        return getContactItemMaxLengthUsingSubId(getDefaultSubscription());
    }

    @Override
    public int[] getContactItemMaxLengthUsingSubId(int subId) throws RemoteException {
        IccPhoneBookInterfaceManagerProxy iccPbkIntMgrProxy = getIccPhoneBookInterfaceManagerProxy(subId);
        if (iccPbkIntMgrProxy != null) {
            return iccPbkIntMgrProxy.getContactItemMaxLength();
        }
        Rlog.e(TAG, "getContactItemMaxLength iccPbkIntMgrProxy isnull for Subscription:" + subId);
        return null;
    }
}
