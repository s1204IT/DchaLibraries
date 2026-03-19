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

    public UiccPhoneBookController(Phone[] phoneArr) {
        if (ServiceManager.getService("simphonebook") == null) {
            ServiceManager.addService("simphonebook", this);
        }
        this.mPhone = phoneArr;
    }

    @Override
    public boolean updateAdnRecordsInEfBySearch(int i, String str, String str2, String str3, String str4, String str5) throws RemoteException {
        return updateAdnRecordsInEfBySearchForSubscriber(getDefaultSubscription(), i, str, str2, str3, str4, str5);
    }

    @Override
    public boolean updateAdnRecordsInEfBySearchForSubscriber(int i, int i2, String str, String str2, String str3, String str4, String str5) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return iccPhoneBookInterfaceManager.updateAdnRecordsInEfBySearch(i2, str, str2, str3, str4, str5);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return false;
    }

    @Override
    public boolean updateAdnRecordsInEfByIndex(int i, String str, String str2, int i2, String str3) throws RemoteException {
        return updateAdnRecordsInEfByIndexForSubscriber(getDefaultSubscription(), i, str, str2, i2, str3);
    }

    @Override
    public boolean updateAdnRecordsInEfByIndexForSubscriber(int i, int i2, String str, String str2, int i3, String str3) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return iccPhoneBookInterfaceManager.updateAdnRecordsInEfByIndex(i2, str, str2, i3, str3);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfByIndex iccPbkIntMgr is null for Subscription:" + i);
        return false;
    }

    @Override
    public int[] getAdnRecordsSize(int i) throws RemoteException {
        return getAdnRecordsSizeForSubscriber(getDefaultSubscription(), i);
    }

    @Override
    public int[] getAdnRecordsSizeForSubscriber(int i, int i2) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return iccPhoneBookInterfaceManager.getAdnRecordsSize(i2);
        }
        Rlog.e(TAG, "getAdnRecordsSize iccPbkIntMgr is null for Subscription:" + i);
        return null;
    }

    @Override
    public List<AdnRecord> getAdnRecordsInEf(int i) throws RemoteException {
        return getAdnRecordsInEfForSubscriber(getDefaultSubscription(), i);
    }

    @Override
    public List<AdnRecord> getAdnRecordsInEfForSubscriber(int i, int i2) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return iccPhoneBookInterfaceManager.getAdnRecordsInEf(i2);
        }
        Rlog.e(TAG, "getAdnRecordsInEf iccPbkIntMgr isnull for Subscription:" + i);
        return null;
    }

    private IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager(int i) {
        try {
            return this.mPhone[SubscriptionController.getInstance().getPhoneId(i)].getIccPhoneBookInterfaceManager();
        } catch (ArrayIndexOutOfBoundsException e) {
            Rlog.e(TAG, "Exception is :" + e.toString() + " For subscription :" + i);
            e.printStackTrace();
            return null;
        } catch (NullPointerException e2) {
            Rlog.e(TAG, "Exception is :" + e2.toString() + " For subscription :" + i);
            e2.printStackTrace();
            return null;
        }
    }

    private int getDefaultSubscription() {
        return PhoneFactory.getDefaultSubscription();
    }
}
