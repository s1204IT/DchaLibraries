package com.android.internal.telephony;

import android.os.RemoteException;
import com.android.internal.telephony.uicc.AdnRecord;
import java.util.List;

public class IccPhoneBookInterfaceManagerProxy {
    private IccPhoneBookInterfaceManager mIccPhoneBookInterfaceManager;

    public IccPhoneBookInterfaceManagerProxy(IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager) {
        this.mIccPhoneBookInterfaceManager = iccPhoneBookInterfaceManager;
    }

    public void setmIccPhoneBookInterfaceManager(IccPhoneBookInterfaceManager iccPhoneBookInterfaceManager) {
        this.mIccPhoneBookInterfaceManager = iccPhoneBookInterfaceManager;
    }

    public boolean updateAdnRecordsInEfBySearch(int efid, String oldTag, String oldPhoneNumber, String newTag, String newPhoneNumber, String pin2) {
        return this.mIccPhoneBookInterfaceManager.updateAdnRecordsInEfBySearch(efid, oldTag, oldPhoneNumber, newTag, newPhoneNumber, pin2);
    }

    public boolean updateAdnRecordsInEfBySearch2(int efid, String oldTag, String oldPhoneNumber, String[] oldEmails, String oldPhoneNumber2, String oldSne, byte[] oldGrps, String newTag, String newPhoneNumber, String[] newEmails, String newPhoneNumber2, String newSne, byte[] newGrps, String pin2) throws RemoteException {
        return this.mIccPhoneBookInterfaceManager.updateAdnRecordsInEfBySearch2(efid, oldTag, oldPhoneNumber, oldEmails, oldPhoneNumber2, oldSne, oldGrps, newTag, newPhoneNumber, newEmails, newPhoneNumber2, newSne, newGrps, pin2);
    }

    public boolean updateAdnRecordsGrpInEfBySearch(int efType, String name, String number, String email, String number2, byte[] grps, byte addgrp, String pin2) throws RemoteException {
        return this.mIccPhoneBookInterfaceManager.updateAdnRecordsGrpInEfBySearch(efType, name, number, email, number2, grps, addgrp, pin2);
    }

    public String[] getGrpRecords(int efid) throws RemoteException {
        return this.mIccPhoneBookInterfaceManager.getGrpRecords(efid);
    }

    public boolean updateAdnRecordsInEfByIndex(int efid, String newTag, String newPhoneNumber, int index, String pin2) {
        return this.mIccPhoneBookInterfaceManager.updateAdnRecordsInEfByIndex(efid, newTag, newPhoneNumber, index, pin2);
    }

    public boolean updateAdnRecordsGrpTagInEfByIndex(int efid, String grpId, String grpTag, String pin2) throws RemoteException {
        return this.mIccPhoneBookInterfaceManager.updateAdnRecordsGrpTagInEfByIndex(efid, grpId, grpTag, pin2);
    }

    public int[] getAdnRecordsSize(int efid) {
        return this.mIccPhoneBookInterfaceManager.getAdnRecordsSize(efid);
    }

    public List<AdnRecord> getAdnRecordsInEf(int efid) {
        return this.mIccPhoneBookInterfaceManager.getAdnRecordsInEf(efid);
    }

    public int getPBInitCount() throws RemoteException {
        return this.mIccPhoneBookInterfaceManager.getPBInitCount();
    }

    public int getEmailFieldSize() throws RemoteException {
        return this.mIccPhoneBookInterfaceManager.getEmailFieldSize();
    }

    public boolean isSneFieldEnable() throws RemoteException {
        return this.mIccPhoneBookInterfaceManager.isSneFieldEnable();
    }

    public int getTotalAdnRecordsSize() throws RemoteException {
        return this.mIccPhoneBookInterfaceManager.getTotalAdnRecordsSize();
    }

    public boolean isSimContactsLoaded() throws RemoteException {
        return this.mIccPhoneBookInterfaceManager.isSimContactsLoaded();
    }

    public int[] getContactItemMaxLength() {
        return this.mIccPhoneBookInterfaceManager.getContactItemMaxLength();
    }
}
