package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import android.util.SparseArray;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.gsm.UsimPhoneBookManager;
import com.mediatek.internal.telephony.gsm.GsmVTProvider;
import com.mediatek.internal.telephony.uicc.AlphaTag;
import com.mediatek.internal.telephony.uicc.CsimPhbStorageInfo;
import com.mediatek.internal.telephony.uicc.UsimGroup;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AdnRecordCache extends Handler implements IccConstants {
    private static final int ADN_FILE_SIZE = 250;
    static final int EVENT_LOAD_ALL_ADN_LIKE_DONE = 1;
    static final int EVENT_UPDATE_ADN_DONE = 2;
    static final String LOG_TAG = "AdnRecordCache";
    public static final int MAX_PHB_NAME_LENGTH = 60;
    public static final int MAX_PHB_NUMBER_ANR_COUNT = 3;
    public static final int MAX_PHB_NUMBER_ANR_LENGTH = 20;
    public static final int MAX_PHB_NUMBER_LENGTH = 40;
    SparseArray<ArrayList<AdnRecord>> mAdnLikeFiles;
    SparseArray<ArrayList<Message>> mAdnLikeWaiters;
    private CommandsInterface mCi;
    private UiccCardApplication mCurrentApp;
    private IccFileHandler mFh;
    private final Object mLock;
    private boolean mLocked;
    private int mSlotId;
    private boolean mSuccess;
    SparseArray<Message> mUserWriteResponse;
    private UsimPhoneBookManager mUsimPhoneBookManager;

    AdnRecordCache(IccFileHandler fh) {
        this.mSlotId = -1;
        this.mAdnLikeFiles = new SparseArray<>();
        this.mAdnLikeWaiters = new SparseArray<>();
        this.mUserWriteResponse = new SparseArray<>();
        this.mLock = new Object();
        this.mSuccess = false;
        this.mLocked = false;
        this.mFh = fh;
        this.mCi = null;
        this.mCurrentApp = null;
        this.mUsimPhoneBookManager = new UsimPhoneBookManager(this.mFh, this);
    }

    AdnRecordCache(IccFileHandler fh, CommandsInterface ci, UiccCardApplication app) {
        this.mSlotId = -1;
        this.mAdnLikeFiles = new SparseArray<>();
        this.mAdnLikeWaiters = new SparseArray<>();
        this.mUserWriteResponse = new SparseArray<>();
        this.mLock = new Object();
        this.mSuccess = false;
        this.mLocked = false;
        this.mFh = fh;
        this.mCi = ci;
        this.mCurrentApp = app;
        this.mUsimPhoneBookManager = new UsimPhoneBookManager(this.mFh, this, ci, app);
        if (app != null) {
            this.mSlotId = app.getSlotId();
        }
    }

    public void reset() {
        logi("reset");
        this.mAdnLikeFiles.clear();
        this.mUsimPhoneBookManager.reset();
        clearWaiters();
        clearUserWriters();
        if (!(this.mFh instanceof CsimFileHandler)) {
            return;
        }
        CsimPhbStorageInfo.clearAdnRecordSize();
    }

    public int getSlotId() {
        return this.mSlotId;
    }

    private void clearWaiters() {
        int size = this.mAdnLikeWaiters.size();
        for (int i = 0; i < size; i++) {
            ArrayList<Message> waiters = this.mAdnLikeWaiters.valueAt(i);
            AsyncResult ar = new AsyncResult((Object) null, (Object) null, new RuntimeException("AdnCache reset"));
            notifyWaiters(waiters, ar);
        }
        this.mAdnLikeWaiters.clear();
    }

    private void clearUserWriters() {
        logi("clearUserWriters,mLocked " + this.mLocked);
        if (this.mLocked) {
            synchronized (this.mLock) {
                this.mLock.notifyAll();
            }
            this.mLocked = false;
        }
        int size = this.mUserWriteResponse.size();
        for (int i = 0; i < size; i++) {
            sendErrorResponse(this.mUserWriteResponse.valueAt(i), "AdnCace reset " + this.mUserWriteResponse.valueAt(i));
        }
        this.mUserWriteResponse.clear();
    }

    public ArrayList<AdnRecord> getRecordsIfLoaded(int efid) {
        return this.mAdnLikeFiles.get(efid);
    }

    public int extensionEfForEf(int efid) {
        switch (efid) {
            case IccConstants.EF_PBR:
                break;
            case 28474:
                break;
            case IccConstants.EF_FDN:
                break;
            case IccConstants.EF_MSISDN:
                break;
            case IccConstants.EF_SDN:
                break;
            case IccConstants.EF_MBDN:
                break;
        }
        return IccConstants.EF_EXT1;
    }

    private void sendErrorResponse(Message response, String errString) {
        sendErrorResponse(response, errString, 2);
    }

    private void sendErrorResponse(Message response, String errString, int ril_errno) {
        CommandException e = CommandException.fromRilErrno(ril_errno);
        if (response == null) {
            return;
        }
        logw(errString);
        AsyncResult.forMessage(response).exception = e;
        response.sendToTarget();
    }

    public synchronized void updateAdnByIndex(int efid, AdnRecord adn, int recordIndex, String pin2, Message response) {
        logd("updateAdnByIndex efid:" + efid + ", pin2:" + pin2 + ", recordIndex:" + recordIndex + ", adn [" + adn + "]");
        int extensionEF = extensionEfForEf(efid);
        if (extensionEF < 0) {
            sendErrorResponse(response, "EF is not known ADN-like EF:0x" + Integer.toHexString(efid).toUpperCase());
            return;
        }
        if (adn.mAlphaTag.length() > 60) {
            sendErrorResponse(response, "the input length of mAlphaTag is too long: " + adn.mAlphaTag, 1002);
            return;
        }
        for (int i = 0; i < 3; i++) {
            String anr = adn.getAdditionalNumber(i);
            if (anr != null && anr.length() > 20) {
                sendErrorResponse(response, "the input length of additional number is too long: " + anr, GsmVTProvider.SESSION_EVENT_START_COUNTER);
                return;
            }
        }
        int num_length = adn.mNumber.length();
        if (adn.mNumber.indexOf(43) != -1) {
            num_length--;
        }
        if (num_length > 40) {
            sendErrorResponse(response, "the input length of phoneNumber is too long: " + adn.mNumber, 1001);
            return;
        }
        if (!this.mUsimPhoneBookManager.checkEmailLength(adn.mEmails)) {
            sendErrorResponse(response, "the email string is too long", 1006);
            return;
        }
        if (efid == 20272) {
            ArrayList<AdnRecord> oldAdnList = this.mUsimPhoneBookManager.loadEfFilesFromUsim();
            if (oldAdnList == null) {
                sendErrorResponse(response, "Adn list not exist for EF:" + efid, 1011);
                return;
            }
            AdnRecord foundAdn = oldAdnList.get(recordIndex - 1);
            efid = foundAdn.mEfid;
            extensionEF = foundAdn.mExtRecord;
            adn.mEfid = efid;
        }
        if (!this.mUsimPhoneBookManager.checkEmailCapacityFree(recordIndex, adn.mEmails)) {
            sendErrorResponse(response, "drop the email for the limitation of the SIM card", 1005);
            return;
        }
        for (int i2 = 0; i2 < 3; i2++) {
            String anr2 = adn.getAdditionalNumber(i2);
            if (!this.mUsimPhoneBookManager.isAnrCapacityFree(anr2, recordIndex, i2)) {
                sendErrorResponse(response, "drop the additional number for the update fail: " + anr2, 1012);
                return;
            }
        }
        if (!this.mUsimPhoneBookManager.checkSneCapacityFree(recordIndex, adn.sne)) {
            sendErrorResponse(response, "drop the sne for the limitation of the SIM card", 1007);
            return;
        }
        Message pendingResponse = this.mUserWriteResponse.get(efid);
        if (pendingResponse != null) {
            sendErrorResponse(response, "Have pending update for EF:0x" + Integer.toHexString(efid).toUpperCase());
            return;
        }
        this.mUserWriteResponse.put(efid, response);
        if ((efid == 28474 || efid == 20272 || efid == 20282 || efid == 20283 || efid == 20284 || efid == 20285) && adn.mAlphaTag.length() == 0 && adn.mNumber.length() == 0) {
            this.mUsimPhoneBookManager.removeContactGroup(recordIndex);
        }
        if (this.mUserWriteResponse.size() == 0) {
            return;
        }
        synchronized (this.mLock) {
            this.mSuccess = false;
            this.mLocked = true;
            new AdnRecordLoader(this.mFh).updateEF(adn, efid, extensionEF, recordIndex, pin2, obtainMessage(2, efid, recordIndex, adn));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                return;
            }
        }
        if (!this.mSuccess) {
            return;
        }
        if (efid == 28474 || efid == 20272 || efid == 20282 || efid == 20283 || efid == 20284 || efid == 20285) {
            try {
                int mResult = this.mUsimPhoneBookManager.updateSneByAdnIndex(adn.sne, recordIndex);
                if (-30 == mResult) {
                    sendErrorResponse(response, "drop the SNE for the limitation of the SIM card", 1007);
                } else if (-40 == mResult) {
                    sendErrorResponse(response, "the sne string is too long", 1008);
                }
                for (int i3 = 0; i3 < 3; i3++) {
                    this.mUsimPhoneBookManager.updateAnrByAdnIndex(adn.getAdditionalNumber(i3), recordIndex, i3);
                }
                int success = this.mUsimPhoneBookManager.updateEmailsByAdnIndex(adn.mEmails, recordIndex);
                if (-30 == success) {
                    sendErrorResponse(response, "drop the email for the limitation of the SIM card", 1005);
                } else if (-40 == success) {
                    sendErrorResponse(response, "the email string is too long", 1006);
                } else if (-50 == success) {
                    sendErrorResponse(response, "Unkown error occurs when update email", 2);
                } else {
                    AsyncResult.forMessage(response, (Object) null, (Throwable) null);
                    response.sendToTarget();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } else if (efid == 28475) {
            AsyncResult.forMessage(response, (Object) null, (Throwable) null);
            response.sendToTarget();
        }
    }

    public synchronized int updateAdnBySearch(int efid, AdnRecord oldAdn, AdnRecord newAdn, String pin2, Message response) {
        logd("updateAdnBySearch efid:" + efid + ", pin2:" + pin2 + ", oldAdn [" + oldAdn + "], new Adn[" + newAdn + "]");
        int index = -1;
        int extensionEF = extensionEfForEf(efid);
        if (extensionEF < 0) {
            sendErrorResponse(response, "EF is not known ADN-like EF:0x" + Integer.toHexString(efid).toUpperCase());
            return -1;
        }
        if (newAdn.mAlphaTag.length() > 60) {
            sendErrorResponse(response, "the input length of mAlphaTag is too long: " + newAdn.mAlphaTag, 1002);
            return -1;
        }
        int num_length = newAdn.mNumber.length();
        if (newAdn.mNumber.indexOf(43) != -1) {
            num_length--;
        }
        if (num_length > 40) {
            sendErrorResponse(response, "the input length of phoneNumber is too long: " + newAdn.mNumber, 1001);
            return -1;
        }
        for (int i = 0; i < 3; i++) {
            String anr = newAdn.getAdditionalNumber(i);
            if (anr != null) {
                int num_length2 = anr.length();
                if (anr.indexOf(43) != -1) {
                    num_length2--;
                }
                if (num_length2 > 20) {
                    sendErrorResponse(response, "the input length of additional number is too long: " + anr, GsmVTProvider.SESSION_EVENT_START_COUNTER);
                    return -1;
                }
            }
        }
        if (!this.mUsimPhoneBookManager.checkEmailLength(newAdn.mEmails)) {
            sendErrorResponse(response, "the email string is too long", 1006);
            return -1;
        }
        ArrayList<AdnRecord> oldAdnList = efid == 20272 ? this.mUsimPhoneBookManager.loadEfFilesFromUsim() : getRecordsIfLoaded(efid);
        if (oldAdnList == null) {
            sendErrorResponse(response, "Adn list not exist for EF:" + efid, 1011);
            return -1;
        }
        int count = 1;
        Iterator<AdnRecord> it = oldAdnList.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            if (oldAdn.isEqual(it.next())) {
                index = count;
                break;
            }
            count++;
        }
        logi("updateAdnBySearch index " + index);
        if (index == -1) {
            if (oldAdn.mAlphaTag.length() == 0 && oldAdn.mNumber.length() == 0) {
                sendErrorResponse(response, "Adn record don't exist for " + oldAdn, 1003);
            } else {
                sendErrorResponse(response, "Adn record don't exist for " + oldAdn);
            }
            return index;
        }
        if (efid == 20272) {
            AdnRecord foundAdn = oldAdnList.get(index - 1);
            efid = foundAdn.mEfid;
            extensionEF = foundAdn.mExtRecord;
            index = foundAdn.mRecordNumber;
            newAdn.mEfid = efid;
            newAdn.mExtRecord = extensionEF;
            newAdn.mRecordNumber = index;
        }
        Message pendingResponse = this.mUserWriteResponse.get(efid);
        if (pendingResponse != null) {
            sendErrorResponse(response, "Have pending update for EF:0x" + Integer.toHexString(efid).toUpperCase());
            return index;
        }
        if (efid == 0) {
            sendErrorResponse(response, "Abnormal efid: " + efid);
            return index;
        }
        if (!this.mUsimPhoneBookManager.checkEmailCapacityFree(index, newAdn.mEmails)) {
            sendErrorResponse(response, "drop the email for the limitation of the SIM card", 1005);
            return index;
        }
        for (int i2 = 0; i2 < 3; i2++) {
            String anr2 = newAdn.getAdditionalNumber(i2);
            if (!this.mUsimPhoneBookManager.isAnrCapacityFree(anr2, index, i2)) {
                sendErrorResponse(response, "drop the additional number for the write fail: " + anr2, 1012);
                return index;
            }
        }
        if (!this.mUsimPhoneBookManager.checkSneCapacityFree(index, newAdn.sne)) {
            sendErrorResponse(response, "drop the sne for the limitation of the SIM card", 1007);
            return index;
        }
        this.mUserWriteResponse.put(efid, response);
        synchronized (this.mLock) {
            this.mSuccess = false;
            this.mLocked = true;
            new AdnRecordLoader(this.mFh).updateEF(newAdn, efid, extensionEF, index, pin2, obtainMessage(2, efid, index, newAdn));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                return index;
            }
        }
        if (!this.mSuccess) {
            loge("updateAdnBySearch mSuccess:" + this.mSuccess);
            return index;
        }
        int success = 0;
        if (efid == 28474 || efid == 20272 || efid == 20282 || efid == 20283 || efid == 20284 || efid == 20285) {
            int mResult = this.mUsimPhoneBookManager.updateSneByAdnIndex(newAdn.sne, index);
            if (-30 == mResult) {
                sendErrorResponse(response, "drop the SNE for the limitation of the SIM card", 1007);
            } else if (-40 == mResult) {
                sendErrorResponse(response, "the sne string is too long", 1008);
            }
            for (int i3 = 0; i3 < 3; i3++) {
                this.mUsimPhoneBookManager.updateAnrByAdnIndex(newAdn.getAdditionalNumber(i3), index, i3);
            }
            success = this.mUsimPhoneBookManager.updateEmailsByAdnIndex(newAdn.mEmails, index);
        }
        if (-30 == success) {
            sendErrorResponse(response, "drop the email for the limitation of the SIM card", 1005);
        } else if (-40 == success) {
            sendErrorResponse(response, "the email string is too long", 1006);
        } else if (-50 == success) {
            sendErrorResponse(response, "Unkown error occurs when update email", 2);
        } else {
            logd("updateAdnBySearch response:" + response);
            AsyncResult.forMessage(response, (Object) null, (Throwable) null);
            response.sendToTarget();
        }
        return index;
    }

    public void requestLoadAllAdnLike(int efid, int extensionEf, Message response) {
        ArrayList<AdnRecord> result;
        logd("requestLoadAllAdnLike efid = " + efid + ", extensionEf = " + extensionEf);
        if (efid == 20272) {
            result = this.mUsimPhoneBookManager.loadEfFilesFromUsim();
        } else {
            result = getRecordsIfLoaded(efid);
        }
        logi("requestLoadAllAdnLike result = null ?" + (result == null));
        if (result != null) {
            if (response != null) {
                AsyncResult.forMessage(response).result = result;
                response.sendToTarget();
                return;
            }
            return;
        }
        ArrayList<Message> waiters = this.mAdnLikeWaiters.get(efid);
        if (waiters != null) {
            waiters.add(response);
            return;
        }
        ArrayList<Message> waiters2 = new ArrayList<>();
        waiters2.add(response);
        this.mAdnLikeWaiters.put(efid, waiters2);
        if (extensionEf < 0) {
            if (response != null) {
                AsyncResult.forMessage(response).exception = new RuntimeException("EF is not known ADN-like EF:0x" + Integer.toHexString(efid).toUpperCase());
                response.sendToTarget();
                return;
            }
            return;
        }
        new AdnRecordLoader(this.mFh).loadAllFromEF(efid, extensionEf, obtainMessage(1, efid, 0));
    }

    private void notifyWaiters(ArrayList<Message> waiters, AsyncResult ar) {
        if (waiters == null) {
            return;
        }
        int s = waiters.size();
        for (int i = 0; i < s; i++) {
            Message waiter = waiters.get(i);
            if (waiter != null) {
                logi("NotifyWaiters: " + waiter);
                AsyncResult.forMessage(waiter, ar.result, ar.exception);
                waiter.sendToTarget();
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                AsyncResult ar = (AsyncResult) msg.obj;
                int efid = msg.arg1;
                ArrayList<Message> waiters = this.mAdnLikeWaiters.get(efid);
                this.mAdnLikeWaiters.delete(efid);
                if (ar.exception == null) {
                    this.mAdnLikeFiles.put(efid, (ArrayList) ar.result);
                } else {
                    Rlog.w(LOG_TAG, "EVENT_LOAD_ALL_ADN_LIKE_DONE exception", ar.exception);
                }
                notifyWaiters(waiters, ar);
                return;
            case 2:
                logd("EVENT_UPDATE_ADN_DONE");
                synchronized (this.mLock) {
                    if (this.mLocked) {
                        AsyncResult ar2 = (AsyncResult) msg.obj;
                        int efid2 = msg.arg1;
                        int index = msg.arg2;
                        AdnRecord adn = (AdnRecord) ar2.userObj;
                        if (ar2.exception == null && adn != null) {
                            adn.setRecordIndex(index);
                            if (adn.mEfid <= 0) {
                                adn.mEfid = efid2;
                            }
                            logd("mAdnLikeFiles changed index:" + index + ",adn:" + adn + "  efid:" + efid2);
                            if (this.mAdnLikeFiles != null && this.mAdnLikeFiles.get(efid2) != null) {
                                if (efid2 == 20283 && (this.mFh instanceof CsimFileHandler)) {
                                    index -= 250;
                                }
                                this.mAdnLikeFiles.get(efid2).set(index - 1, adn);
                                logd(" index:" + index + "   efid:" + efid2);
                            }
                            if (this.mUsimPhoneBookManager != null && efid2 != 28475) {
                                if (efid2 == 20283) {
                                    index += ADN_FILE_SIZE;
                                    logd(" index2:" + index);
                                }
                                this.mUsimPhoneBookManager.updateUsimPhonebookRecordsList(index - 1, adn);
                            }
                        }
                        Message response = this.mUserWriteResponse.get(efid2);
                        this.mUserWriteResponse.delete(efid2);
                        logi("AdnRecordCacheEx: " + ar2.exception);
                        if (ar2.exception != null && response != null) {
                            AsyncResult.forMessage(response, (Object) null, ar2.exception);
                            response.sendToTarget();
                        }
                        this.mSuccess = ar2.exception == null;
                        this.mLock.notifyAll();
                        this.mLocked = false;
                    }
                    break;
                }
                return;
            default:
                return;
        }
    }

    protected void logd(String msg) {
        Rlog.d(LOG_TAG, "[AdnRecordCache] " + msg + "(slot " + this.mSlotId + ")");
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, "[AdnRecordCache] " + msg + "(slot " + this.mSlotId + ")");
    }

    protected void logi(String msg) {
        Rlog.i(LOG_TAG, "[AdnRecordCache] " + msg + "(slot " + this.mSlotId + ")");
    }

    protected void logw(String msg) {
        Rlog.w(LOG_TAG, "[AdnRecordCache] " + msg + "(slot " + this.mSlotId + ")");
    }

    public List<UsimGroup> getUsimGroups() {
        return this.mUsimPhoneBookManager.getUsimGroups();
    }

    public String getUsimGroupById(int nGasId) {
        return this.mUsimPhoneBookManager.getUsimGroupById(nGasId);
    }

    public boolean removeUsimGroupById(int nGasId) {
        return this.mUsimPhoneBookManager.removeUsimGroupById(nGasId);
    }

    public int insertUsimGroup(String grpName) {
        return this.mUsimPhoneBookManager.insertUsimGroup(grpName);
    }

    public int updateUsimGroup(int nGasId, String grpName) {
        return this.mUsimPhoneBookManager.updateUsimGroup(nGasId, grpName);
    }

    public boolean addContactToGroup(int adnIndex, int grpIndex) {
        return this.mUsimPhoneBookManager.addContactToGroup(adnIndex, grpIndex);
    }

    public boolean removeContactFromGroup(int adnIndex, int grpIndex) {
        return this.mUsimPhoneBookManager.removeContactFromGroup(adnIndex, grpIndex);
    }

    public boolean updateContactToGroups(int adnIndex, int[] grpIdList) {
        return this.mUsimPhoneBookManager.updateContactToGroups(adnIndex, grpIdList);
    }

    public boolean moveContactFromGroupsToGroups(int adnIndex, int[] fromGrpIdList, int[] toGrpIdList) {
        return this.mUsimPhoneBookManager.moveContactFromGroupsToGroups(adnIndex, fromGrpIdList, toGrpIdList);
    }

    public int hasExistGroup(String grpName) {
        return this.mUsimPhoneBookManager.hasExistGroup(grpName);
    }

    public int getUsimGrpMaxNameLen() {
        return this.mUsimPhoneBookManager.getUsimGrpMaxNameLen();
    }

    public int getUsimGrpMaxCount() {
        return this.mUsimPhoneBookManager.getUsimGrpMaxCount();
    }

    private void dumpAdnLikeFile() {
        int size = this.mAdnLikeFiles.size();
        logd("dumpAdnLikeFile size " + size);
        for (int i = 0; i < size; i++) {
            int key = this.mAdnLikeFiles.keyAt(i);
            ArrayList<AdnRecord> records = this.mAdnLikeFiles.get(key);
            logd("dumpAdnLikeFile index " + i + " key " + key + "records size " + records.size());
            for (int j = 0; j < records.size(); j++) {
                AdnRecord record = records.get(j);
                logd("mAdnLikeFiles[" + j + "]=" + record);
            }
        }
    }

    public ArrayList<AlphaTag> getUsimAasList() {
        return this.mUsimPhoneBookManager.getUsimAasList();
    }

    public String getUsimAasById(int index) {
        return this.mUsimPhoneBookManager.getUsimAasById(index, 0);
    }

    public boolean removeUsimAasById(int index, int pbrIndex) {
        return this.mUsimPhoneBookManager.removeUsimAasById(index, pbrIndex);
    }

    public int insertUsimAas(String aasName) {
        return this.mUsimPhoneBookManager.insertUsimAas(aasName);
    }

    public boolean updateUsimAas(int index, int pbrIndex, String aasName) {
        return this.mUsimPhoneBookManager.updateUsimAas(index, pbrIndex, aasName);
    }

    public boolean updateAdnAas(int adnIndex, int aasIndex) {
        return this.mUsimPhoneBookManager.updateAdnAas(adnIndex, aasIndex);
    }

    public int getAnrCount() {
        return this.mUsimPhoneBookManager.getAnrCount();
    }

    public int getEmailCount() {
        return this.mUsimPhoneBookManager.getEmailCount();
    }

    public int getUsimAasMaxCount() {
        return this.mUsimPhoneBookManager.getUsimAasMaxCount();
    }

    public int getUsimAasMaxNameLen() {
        return this.mUsimPhoneBookManager.getUsimAasMaxNameLen();
    }

    public boolean hasSne() {
        return this.mUsimPhoneBookManager.hasSne();
    }

    public int getSneRecordLen() {
        return this.mUsimPhoneBookManager.getSneRecordLen();
    }

    public boolean isAdnAccessible() {
        return this.mUsimPhoneBookManager.isAdnAccessible();
    }

    public boolean isUsimPhbEfAndNeedReset(int fileId) {
        return this.mUsimPhoneBookManager.isUsimPhbEfAndNeedReset(fileId);
    }

    public UsimPBMemInfo[] getPhonebookMemStorageExt() {
        return this.mUsimPhoneBookManager.getPhonebookMemStorageExt();
    }
}
