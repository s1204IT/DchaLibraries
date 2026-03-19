package com.mediatek.internal.telephony.phb;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.mediatek.internal.telephony.phb.IMtkIccPhoneBook;
import java.util.List;

public class MtkUiccPhoneBookController extends IMtkIccPhoneBook.Stub {
    private static final String TAG = "MtkUiccPhoneBookController";
    private Phone[] mPhone;

    public MtkUiccPhoneBookController(Phone[] phoneArr) {
        if (ServiceManager.getService("mtksimphonebook") == null) {
            ServiceManager.addService("mtksimphonebook", this);
        }
        this.mPhone = phoneArr;
    }

    @Override
    public List<MtkAdnRecord> getAdnRecordsInEf(int i) throws RemoteException {
        return getAdnRecordsInEfForSubscriber(getDefaultSubscription(), i);
    }

    @Override
    public List<MtkAdnRecord> getAdnRecordsInEfForSubscriber(int i, int i2) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getAdnRecordsInEf(i2, null);
        }
        Rlog.e(TAG, "getAdnRecordsInEf iccPbkIntMgr isnull for Subscription:" + i);
        return null;
    }

    @Override
    public int updateAdnRecordsInEfBySearchWithError(int i, int i2, String str, String str2, String str3, String str4, String str5) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).updateAdnRecordsInEfBySearchWithError(i2, str, str2, str3, str4, str5);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return 0;
    }

    @Override
    public int updateUsimPBRecordsInEfBySearchWithError(int i, int i2, String str, String str2, String str3, String str4, String[] strArr, String str5, String str6, String str7, String str8, String[] strArr2) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).updateUsimPBRecordsInEfBySearchWithError(i2, str, str2, str3, str4, strArr, str5, str6, str7, str8, strArr2);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return 0;
    }

    @Override
    public int updateAdnRecordsInEfByIndexWithError(int i, int i2, String str, String str2, int i3, String str3) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).updateAdnRecordsInEfByIndexWithError(i2, str, str2, i3, str3);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return 0;
    }

    @Override
    public int updateUsimPBRecordsInEfByIndexWithError(int i, int i2, String str, String str2, String str3, String str4, String[] strArr, int i3) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).updateUsimPBRecordsInEfByIndexWithError(i2, str, str2, str3, str4, strArr, i3);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return 0;
    }

    @Override
    public int updateUsimPBRecordsByIndexWithError(int i, int i2, MtkAdnRecord mtkAdnRecord, int i3) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).updateUsimPBRecordsByIndexWithError(i2, mtkAdnRecord, i3);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return 0;
    }

    @Override
    public int updateUsimPBRecordsBySearchWithError(int i, int i2, MtkAdnRecord mtkAdnRecord, MtkAdnRecord mtkAdnRecord2) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).updateUsimPBRecordsBySearchWithError(i2, mtkAdnRecord, mtkAdnRecord2);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return 0;
    }

    @Override
    public boolean isPhbReady(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).isPhbReady();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return false;
    }

    @Override
    public List<UsimGroup> getUsimGroups(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getUsimGroups();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return null;
    }

    @Override
    public String getUsimGroupById(int i, int i2) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getUsimGroupById(i2);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return null;
    }

    @Override
    public boolean removeUsimGroupById(int i, int i2) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).removeUsimGroupById(i2);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return false;
    }

    @Override
    public int insertUsimGroup(int i, String str) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).insertUsimGroup(str);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return -1;
    }

    @Override
    public int updateUsimGroup(int i, int i2, String str) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).updateUsimGroup(i2, str);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return -1;
    }

    @Override
    public boolean addContactToGroup(int i, int i2, int i3) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).addContactToGroup(i2, i3);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return false;
    }

    @Override
    public boolean removeContactFromGroup(int i, int i2, int i3) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).removeContactFromGroup(i2, i3);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return false;
    }

    @Override
    public boolean updateContactToGroups(int i, int i2, int[] iArr) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).updateContactToGroups(i2, iArr);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return false;
    }

    @Override
    public boolean moveContactFromGroupsToGroups(int i, int i2, int[] iArr, int[] iArr2) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).moveContactFromGroupsToGroups(i2, iArr, iArr2);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return false;
    }

    @Override
    public int hasExistGroup(int i, String str) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).hasExistGroup(str);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return -1;
    }

    @Override
    public int getUsimGrpMaxNameLen(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getUsimGrpMaxNameLen();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return -1;
    }

    @Override
    public int getUsimGrpMaxCount(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getUsimGrpMaxCount();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return -1;
    }

    @Override
    public List<AlphaTag> getUsimAasList(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getUsimAasList();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return null;
    }

    @Override
    public String getUsimAasById(int i, int i2) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getUsimAasById(i2);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return null;
    }

    @Override
    public int insertUsimAas(int i, String str) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).insertUsimAas(str);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return 0;
    }

    @Override
    public int getAnrCount(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getAnrCount();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return 0;
    }

    @Override
    public int getEmailCount(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getEmailCount();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return 0;
    }

    @Override
    public int getUsimAasMaxCount(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getUsimAasMaxCount();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return 0;
    }

    @Override
    public int getUsimAasMaxNameLen(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getUsimAasMaxNameLen();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return 0;
    }

    @Override
    public boolean updateUsimAas(int i, int i2, int i3, String str) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).updateUsimAas(i2, i3, str);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return false;
    }

    @Override
    public boolean removeUsimAasById(int i, int i2, int i3) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).removeUsimAasById(i2, i3);
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return false;
    }

    @Override
    public boolean hasSne(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).hasSne();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return false;
    }

    @Override
    public int getSneRecordLen(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getSneRecordLen();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return 0;
    }

    @Override
    public boolean isAdnAccessible(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).isAdnAccessible();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return true;
    }

    @Override
    public UsimPBMemInfo[] getPhonebookMemStorageExt(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getPhonebookMemStorageExt();
        }
        Rlog.e(TAG, "updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:" + i);
        return null;
    }

    @Override
    public int getUpbDone(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getUpbDone();
        }
        Rlog.e(TAG, "getUpbDone iccPbkIntMgr is null for Subscription:" + i);
        return -1;
    }

    @Override
    public int[] getAdnRecordsCapacity() throws RemoteException {
        return getAdnRecordsCapacityForSubscriber(getDefaultSubscription());
    }

    @Override
    public int[] getAdnRecordsCapacityForSubscriber(int i) throws RemoteException {
        IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager = getIccPhoneBookInterfaceManager(i);
        if (iccPhoneBookInterfaceManager != null) {
            return ((MtkIccPhoneBookInterfaceManager) iccPhoneBookInterfaceManager).getAdnRecordsCapacity();
        }
        Rlog.e(TAG, "getAdnRecordsCapacity iccPbkIntMgr is null for Subscription:" + i);
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
