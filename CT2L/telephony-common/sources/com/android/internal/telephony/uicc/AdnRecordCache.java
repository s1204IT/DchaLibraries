package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import android.util.SparseArray;
import com.android.internal.telephony.gsm.UsimPhoneBookManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class AdnRecordCache extends Handler implements IccConstants {
    static final int EVENT_LOAD_ALL_ADN_LIKE_DONE = 1;
    static final int EVENT_LOAD_CP_PHONEBOOK_STATUS_DONE = 9;
    static final int EVENT_UPDATE_ADN_DONE = 2;
    static final int EVENT_UPDATE_ADN_DONE2 = 4;
    static final int EVENT_UPDATE_ANR_DONE = 5;
    static final int EVENT_UPDATE_EMAIL_DONE = 3;
    static final int EVENT_UPDATE_GRP_DONE = 6;
    static final int EVENT_UPDATE_GSD_DONE = 8;
    static final int EVENT_UPDATE_SNE_DONE = 7;
    static final String TAG = "AdnRecordCache";
    public static final String USIM_ADN_MATCH_POSITION = "usimAdnMatchPosition";
    private static final boolean VDBG = false;
    private IccFileHandler mFh;
    public int mUSIMExt1;
    private UsimPhoneBookManager mUsimPhoneBookManager;
    private Object mLock = new Object();
    private boolean mUpdateEmailSuccess = true;
    private boolean mUpdateAnrSuccess = true;
    private boolean mUpdateGrpSuccess = true;
    private boolean mUpdateSneSuccess = true;
    private boolean mUpdateGsdSuccess = true;
    public int PBInitCount = 0;
    public Map<Integer, byte[]> mAdnExt1Map = new HashMap();
    public Map<Integer, byte[]> mAdnExt2Map = new HashMap();
    private volatile boolean mSimContactsLoaded = false;
    SparseArray<ArrayList<AdnRecord>> mAdnLikeFiles = new SparseArray<>();
    SparseArray<ArrayList<Message>> mAdnLikeWaiters = new SparseArray<>();
    SparseArray<Message> mUserWriteResponse = new SparseArray<>();
    private int mCPPhoneBookTotal = -1;
    private int mCPPhoneBookUsed = -1;
    private int mCPPhoneBookFirstIndex = -1;
    private boolean mCPPhonebookLoaded = false;

    public AdnRecordCache(IccFileHandler fh) {
        this.mFh = fh;
        this.mUsimPhoneBookManager = new UsimPhoneBookManager(this.mFh, this);
    }

    public void reset() {
        this.mAdnLikeFiles.clear();
        this.mUsimPhoneBookManager.reset();
        this.mAdnExt1Map.clear();
        this.mAdnExt2Map.clear();
        clearWaiters();
        clearUserWriters();
        this.PBInitCount = 0;
        this.mUSIMExt1 = 0;
        this.mSimContactsLoaded = false;
        this.mCPPhonebookLoaded = false;
    }

    public boolean isSimContactsLoaded() {
        return this.mSimContactsLoaded;
    }

    public void setSimContactsLoaded(boolean loaded, int efid) {
        this.mSimContactsLoaded = loaded;
        if (efid == 20272) {
            if (this.mUsimPhoneBookManager.isSimRecordsEmpty()) {
                this.mSimContactsLoaded = false;
            }
        } else {
            ArrayList<AdnRecord> result = getRecordsIfLoaded(efid);
            if (result == null || result.isEmpty()) {
                this.mSimContactsLoaded = false;
            }
        }
    }

    public int getEmailFieldSize() {
        if (this.mUsimPhoneBookManager == null) {
            return 0;
        }
        return this.mUsimPhoneBookManager.getEmailNumInOneRecord(0);
    }

    public boolean isSneFieldEnable() {
        if (this.mUsimPhoneBookManager == null) {
            return false;
        }
        return this.mUsimPhoneBookManager.isSneFieldEnable();
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
        int size = this.mUserWriteResponse.size();
        for (int i = 0; i < size; i++) {
            sendErrorResponse(this.mUserWriteResponse.valueAt(i), "AdnCace reset");
        }
        this.mUserWriteResponse.clear();
    }

    public int getTotalAdnRecordsSize(int efid) {
        ArrayList<AdnRecord> oldAdnList;
        if (efid == 28474) {
            if (this.mAdnLikeFiles.get(28474) != null) {
                return this.mAdnLikeFiles.get(28474).size();
            }
            return 0;
        }
        if (efid != 20272 || (oldAdnList = this.mUsimPhoneBookManager.getUsimPhoneBook()) == null) {
            return 0;
        }
        return oldAdnList.size();
    }

    public int getUsimOneAdnEfid() {
        ArrayList<AdnRecord> oldAdnList = this.mUsimPhoneBookManager.getUsimPhoneBook();
        if (oldAdnList == null || oldAdnList.isEmpty()) {
            return -1;
        }
        return oldAdnList.get(0).mEfid;
    }

    public int getUsimOneEmailEfid() {
        ArrayList<AdnRecord> oldAdnList = this.mUsimPhoneBookManager.getUsimPhoneBook();
        if (oldAdnList == null || oldAdnList.isEmpty()) {
            return -1;
        }
        AdnRecord adn = oldAdnList.get(0);
        if (adn.emailEfids != null) {
            return adn.emailEfids[0];
        }
        return -1;
    }

    public ArrayList<AdnRecord> getRecordsIfLoaded(int efid) {
        return this.mAdnLikeFiles.get(efid);
    }

    public int extensionEfForEf(int efid) {
        switch (efid) {
            case IccConstants.EF_PBR:
                return this.mUsimPhoneBookManager.extEf;
            case 28474:
            case IccConstants.EF_MSISDN:
                return IccConstants.EF_EXT1;
            case IccConstants.EF_FDN:
                return IccConstants.EF_EXT2;
            case IccConstants.EF_SDN:
                return IccConstants.EF_EXT3;
            case IccConstants.EF_MBDN:
                return IccConstants.EF_EXT6;
            default:
                return -1;
        }
    }

    private void sendErrorResponse(Message response, String errString) {
        if (response != null) {
            Exception e = new RuntimeException(errString);
            AsyncResult.forMessage(response).exception = e;
            response.sendToTarget();
        }
    }

    public void updateAdnByIndex(int efid, AdnRecord adn, int recordIndex, String pin2, Message response) {
        int extensionEF = extensionEfForEf(efid);
        if (extensionEF < 0) {
            sendErrorResponse(response, "EF is not known ADN-like EF:" + efid);
            return;
        }
        Message pendingResponse = this.mUserWriteResponse.get(efid);
        if (pendingResponse != null) {
            sendErrorResponse(response, "Have pending update for EF:" + efid);
        } else {
            this.mUserWriteResponse.put(efid, response);
            new AdnRecordLoader(this.mFh, this).updateEF(adn, efid, extensionEF, recordIndex, pin2, obtainMessage(2, efid, recordIndex, adn));
        }
    }

    public void updateAdnBySearch(int efid, AdnRecord oldAdn, AdnRecord newAdn, String pin2, Message response) {
        ArrayList<AdnRecord> oldAdnList;
        int extensionEF = extensionEfForEf(efid);
        if (extensionEF < 0) {
            sendErrorResponse(response, "EF is not known ADN-like EF:" + efid);
            return;
        }
        if (efid == 20272) {
            oldAdnList = this.mUsimPhoneBookManager.getUsimPhoneBook();
        } else {
            oldAdnList = getRecordsIfLoaded(efid);
        }
        if (oldAdnList == null) {
            sendErrorResponse(response, "Adn list not exist for EF:" + efid);
            return;
        }
        int index = -1;
        int count = 1;
        Iterator<AdnRecord> it = oldAdnList.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            AdnRecord thisRec = it.next();
            if (oldAdn.isEqual(thisRec)) {
                index = count;
                break;
            }
            count++;
        }
        if (index == -1) {
            sendErrorResponse(response, "Adn record don't exist for " + oldAdn);
            return;
        }
        AdnRecord foundAdn = oldAdnList.get(index - 1);
        newAdn.mExtRecord = foundAdn.mExtRecord;
        if (efid == 20272) {
            efid = foundAdn.mEfid;
            index = foundAdn.mRecordNumber;
            newAdn.mEfid = efid;
            newAdn.mRecordNumber = index;
        }
        Message pendingResponse = this.mUserWriteResponse.get(efid);
        if (pendingResponse != null) {
            sendErrorResponse(response, "Have pending update for EF:" + efid);
            return;
        }
        this.mUserWriteResponse.put(efid, response);
        new AdnRecordLoader(this.mFh, this).updateEF(newAdn, efid, extensionEF, index, pin2, obtainMessage(2, efid, index, newAdn));
    }

    public void updateAdnBySearch2(int efid, AdnRecord oldAdn, AdnRecord newAdn, String pin2, Message response) {
        ArrayList<AdnRecord> oldAdnList;
        Rlog.d(TAG, "begin updateAdnBySearch2()");
        int extensionEF = extensionEfForEf(efid);
        Rlog.v(TAG, "extensionEF = " + extensionEF);
        if (extensionEF < 0) {
            sendErrorResponse(response, "EF is not known ADN-like EF:" + efid);
            return;
        }
        if (efid == 20272) {
            oldAdnList = this.mUsimPhoneBookManager.getUsimPhoneBook();
        } else {
            oldAdnList = getRecordsIfLoaded(efid);
        }
        if (oldAdnList == null || oldAdnList.isEmpty()) {
            sendErrorResponse(response, "Adn list not exist for EF:" + efid);
            return;
        }
        int index = -1;
        int count = 1;
        int emailIndex = -1;
        int emailIndex2nd = -1;
        int emailEfid2nd = -1;
        int anrIndex = -1;
        int grpIndex = -1;
        int sneIndex = -1;
        boolean needUpdateEmail = true;
        boolean needUpdateEmail2nd = true;
        boolean needUpdateAnr = true;
        boolean needUpdateGrp = true;
        boolean needUpdateSne = true;
        boolean emailType2Used = false;
        int pbrRecordNum = 0;
        if (efid == 20272) {
            int adnCountInOneEf = oldAdnList.size() / this.mAdnLikeFiles.size();
            Rlog.v(TAG, "oldAdnList.size() = " + oldAdnList.size());
            Rlog.v(TAG, "adnLikeFiles.size() = " + this.mAdnLikeFiles.size());
            Rlog.v(TAG, "adnCountInOneEf = " + adnCountInOneEf);
            int i = 0;
            while (i < oldAdnList.size()) {
                AdnRecord comparedAdn = oldAdnList.get(i);
                if (!comparedAdn.isEmpty() || (comparedAdn.isEmpty() && oldAdn.isEmpty())) {
                    Rlog.v(TAG, "comparedAdn = " + comparedAdn);
                    Rlog.v(TAG, "oldAdn = " + oldAdn);
                }
                if (oldAdn.isEqual(comparedAdn)) {
                    Rlog.d(TAG, "find an equal ADN");
                    pbrRecordNum = this.mUsimPhoneBookManager.getpbrRecordNum(count - 1);
                    if (comparedAdn.emailIndexes == null) {
                        Rlog.v(TAG, "--> 1");
                        if (newAdn.mEmails == null || newAdn.mEmails[0] == null || newAdn.mEmails[0].equals("")) {
                            needUpdateEmail = false;
                        } else {
                            needUpdateEmail = true;
                            emailIndex = -1;
                        }
                    } else if (comparedAdn.emailIndexes[0] == -1) {
                        Rlog.v(TAG, "--> 2");
                        if (newAdn.mEmails == null || newAdn.mEmails[0] == null || newAdn.mEmails[0].equals("")) {
                            Rlog.v(TAG, "--> 3");
                            needUpdateEmail = false;
                        } else if (comparedAdn.emailFileTypes[0] == 168) {
                            Rlog.v(TAG, "--> 4");
                            needUpdateEmail = true;
                            emailIndex = count - this.mUsimPhoneBookManager.getRecordOffset(pbrRecordNum);
                            ArrayList<String> emailRecords = this.mUsimPhoneBookManager.getEmailFileRecords(comparedAdn.emailFileTypes[0], pbrRecordNum, comparedAdn.emailEfids[0]);
                            if (emailRecords == null || emailIndex >= emailRecords.size()) {
                                emailIndex = -1;
                            }
                        } else {
                            Rlog.v(TAG, "--> 5");
                            needUpdateEmail = true;
                            int Recordcount = 1;
                            for (String email : this.mUsimPhoneBookManager.getEmailFileRecords(comparedAdn.emailFileTypes[0], pbrRecordNum, comparedAdn.emailEfids[0])) {
                                if (email == null || email.equals("")) {
                                    emailIndex = Recordcount;
                                    emailType2Used = true;
                                    break;
                                }
                                Recordcount++;
                            }
                        }
                    } else {
                        Rlog.v(TAG, "--> 6");
                        emailIndex = comparedAdn.emailIndexes[0];
                    }
                    if (needUpdateEmail && emailIndex == -1) {
                        i = this.mUsimPhoneBookManager.getRecordOffset(pbrRecordNum + 1) - 1;
                        count = i + 2;
                    } else {
                        if (getEmailFieldSize() > 1) {
                            if (comparedAdn.emailIndexes == null) {
                                Rlog.v(TAG, "-->2nd 1");
                                if (newAdn.mEmails == null || newAdn.mEmails.length <= 1 || newAdn.mEmails[1].equals("")) {
                                    needUpdateEmail2nd = false;
                                } else {
                                    needUpdateEmail2nd = true;
                                    emailIndex2nd = -1;
                                }
                            } else if (comparedAdn.emailIndexes.length > 1 && comparedAdn.emailIndexes[1] == -1) {
                                Rlog.v(TAG, "-->2nd 2");
                                if (newAdn.mEmails == null || newAdn.mEmails.length <= 1 || newAdn.mEmails[1].equals("")) {
                                    Rlog.v(TAG, "-->2nd 3");
                                    needUpdateEmail2nd = false;
                                } else if (comparedAdn.emailFileTypes[1] == 168) {
                                    Rlog.v(TAG, "-->2nd 4");
                                    needUpdateEmail2nd = true;
                                    emailIndex2nd = count - this.mUsimPhoneBookManager.getRecordOffset(pbrRecordNum);
                                    ArrayList<String> emailRecords2 = this.mUsimPhoneBookManager.getEmailFileRecords(comparedAdn.emailFileTypes[1], pbrRecordNum, comparedAdn.emailEfids[1]);
                                    if (emailRecords2 == null || emailIndex2nd >= emailRecords2.size()) {
                                        emailIndex2nd = -1;
                                    }
                                } else if (!emailType2Used) {
                                    Rlog.v(TAG, "-->2nd 5");
                                    needUpdateEmail2nd = true;
                                    int Recordcount2 = 1;
                                    for (String email2 : this.mUsimPhoneBookManager.getEmailFileRecords(comparedAdn.emailFileTypes[1], pbrRecordNum, comparedAdn.emailEfids[1])) {
                                        if (email2 == null || email2.equals("")) {
                                            emailIndex2nd = Recordcount2;
                                            emailType2Used = true;
                                            break;
                                        }
                                        Recordcount2++;
                                    }
                                } else if (emailType2Used) {
                                    needUpdateEmail2nd = false;
                                }
                                if (needUpdateEmail && needUpdateEmail2nd) {
                                    emailIndex2nd++;
                                }
                            } else if (comparedAdn.emailIndexes.length > 1 && comparedAdn.emailIndexes[1] != -1) {
                                Rlog.v(TAG, "-->2nd 6");
                                emailIndex2nd = comparedAdn.emailIndexes[1];
                            } else if (comparedAdn.emailIndexes.length <= 1 && newAdn.mEmails != null) {
                                if (newAdn.mEmails.length <= 1 || newAdn.mEmails[1] == null || newAdn.mEmails[1].equals("")) {
                                    Rlog.v(TAG, "-->2nd 7");
                                    needUpdateEmail2nd = false;
                                } else if (!emailType2Used) {
                                    Rlog.v(TAG, "-->2nd 8");
                                    needUpdateEmail2nd = true;
                                    int Recordcount3 = 1;
                                    int pbrFileSize = this.mUsimPhoneBookManager.getPbrFileSize();
                                    for (int num = 0; num < pbrFileSize; num++) {
                                        Set<Integer> emailFiles = this.mUsimPhoneBookManager.getEmailType2Files(num);
                                        Iterator<Integer> it = emailFiles.iterator();
                                        while (it.hasNext()) {
                                            int ef = it.next().intValue();
                                            UsimPhoneBookManager usimPhoneBookManager = this.mUsimPhoneBookManager;
                                            UsimPhoneBookManager usimPhoneBookManager2 = this.mUsimPhoneBookManager;
                                            for (String email3 : usimPhoneBookManager.getEmailFileRecords(169, num, ef)) {
                                                if (email3 == null || email3.equals("")) {
                                                    emailEfid2nd = ef;
                                                    emailIndex2nd = Recordcount3;
                                                    emailType2Used = true;
                                                    break;
                                                }
                                                Recordcount3++;
                                            }
                                        }
                                    }
                                } else if (emailType2Used) {
                                    needUpdateEmail2nd = false;
                                }
                                if (needUpdateEmail && needUpdateEmail2nd && emailEfid2nd == comparedAdn.emailEfids[0] && emailIndex2nd != -1) {
                                    emailIndex2nd++;
                                }
                            }
                        } else {
                            needUpdateEmail2nd = false;
                        }
                        if (needUpdateEmail2nd && emailIndex2nd == -1) {
                            i = this.mUsimPhoneBookManager.getRecordOffset(pbrRecordNum + 1) - 1;
                            count = i + 2;
                        } else {
                            if (comparedAdn.anrIndexes == null) {
                                Rlog.v(TAG, "anr --> 1");
                                if (newAdn.anrs == null || newAdn.anrs[0] == null || newAdn.anrs[0].equals("")) {
                                    needUpdateAnr = false;
                                } else {
                                    needUpdateAnr = true;
                                    anrIndex = -1;
                                }
                            } else if (comparedAdn.anrIndexes[0] == -1) {
                                Rlog.v(TAG, "anr --> 2");
                                if (newAdn.anrs == null || newAdn.anrs[0] == null || newAdn.anrs[0].equals("")) {
                                    Rlog.v(TAG, "anr --> 3");
                                    needUpdateAnr = false;
                                } else if (comparedAdn.anrFileTypes[0] == 168) {
                                    Rlog.v(TAG, "anr --> 4");
                                    needUpdateAnr = true;
                                    anrIndex = count - this.mUsimPhoneBookManager.getRecordOffset(pbrRecordNum);
                                    ArrayList<String> anrRecords = this.mUsimPhoneBookManager.getAnrFileRecords(comparedAdn.anrFileTypes[0], pbrRecordNum, comparedAdn.anrEfids[0]);
                                    if (anrRecords == null || anrIndex > anrRecords.size()) {
                                        anrIndex = -1;
                                    }
                                } else {
                                    Rlog.v(TAG, "anr --> 5");
                                    needUpdateAnr = true;
                                    int Recordcount4 = 1;
                                    for (String anr : this.mUsimPhoneBookManager.getAnrFileRecords(comparedAdn.anrFileTypes[0], pbrRecordNum, comparedAdn.anrEfids[0])) {
                                        if (anr == null || anr.equals("")) {
                                            anrIndex = Recordcount4;
                                            break;
                                        }
                                        Recordcount4++;
                                    }
                                }
                            } else {
                                Rlog.v(TAG, "anr --> 6");
                                if (newAdn.anrs != null && newAdn.anrs[0] != null && newAdn.anrs[0].equals(comparedAdn.anrs[0])) {
                                    needUpdateAnr = false;
                                }
                                anrIndex = comparedAdn.anrIndexes[0];
                            }
                            if (needUpdateAnr && anrIndex == -1) {
                                i = this.mUsimPhoneBookManager.getRecordOffset(pbrRecordNum + 1) - 1;
                                count = i + 2;
                            } else {
                                if (comparedAdn.sneIndexes == null) {
                                    Rlog.v(TAG, "sne --> 1");
                                    if (newAdn.snes == null || newAdn.snes[0] == null || newAdn.snes[0].equals("")) {
                                        needUpdateSne = false;
                                    } else {
                                        needUpdateSne = true;
                                        sneIndex = -1;
                                    }
                                } else if (comparedAdn.sneIndexes[0] == -1) {
                                    Rlog.v(TAG, "sne --> 2");
                                    if (newAdn.snes == null || newAdn.snes[0] == null || newAdn.snes[0].equals("")) {
                                        Rlog.v(TAG, "sne --> 3");
                                        needUpdateSne = false;
                                    } else if (comparedAdn.sneFileTypes[0] == 168) {
                                        Rlog.v(TAG, "sne --> 4");
                                        needUpdateSne = true;
                                        sneIndex = count - this.mUsimPhoneBookManager.getRecordOffset(pbrRecordNum);
                                        ArrayList<String> sneRecords = this.mUsimPhoneBookManager.getSneFileRecords(comparedAdn.sneFileTypes[0], pbrRecordNum, comparedAdn.sneEfids[0]);
                                        if (sneRecords == null || sneIndex >= sneRecords.size()) {
                                            sneIndex = -1;
                                        }
                                    } else {
                                        Rlog.v(TAG, "sne --> 5");
                                        needUpdateSne = true;
                                        int Recordcount5 = 1;
                                        for (String sne : this.mUsimPhoneBookManager.getSneFileRecords(comparedAdn.sneFileTypes[0], pbrRecordNum, comparedAdn.sneEfids[0])) {
                                            if (sne == null || sne.equals("")) {
                                                sneIndex = Recordcount5;
                                                break;
                                            }
                                            Recordcount5++;
                                        }
                                    }
                                } else {
                                    Rlog.v(TAG, "sne --> 6");
                                    sneIndex = comparedAdn.sneIndexes[0];
                                }
                                if (needUpdateSne && sneIndex == -1) {
                                    i = this.mUsimPhoneBookManager.getRecordOffset(pbrRecordNum + 1) - 1;
                                    count = i + 2;
                                } else {
                                    if (comparedAdn.grpEfid == -1) {
                                        Rlog.v(TAG, "grp --> 1");
                                        needUpdateGrp = false;
                                    } else {
                                        Rlog.v(TAG, "grp --> 2");
                                        needUpdateGrp = true;
                                        grpIndex = count - this.mUsimPhoneBookManager.getRecordOffset(pbrRecordNum);
                                    }
                                    index = count;
                                }
                            }
                        }
                    }
                } else {
                    count++;
                }
                i++;
            }
        } else {
            Iterator<AdnRecord> it2 = oldAdnList.iterator();
            while (true) {
                if (!it2.hasNext()) {
                    break;
                }
                if (oldAdn.isEqual(it2.next())) {
                    index = count;
                    break;
                }
                count++;
            }
        }
        Rlog.d(TAG, "in updateAdnBySearch2()  index: " + index + " emailIndex: " + emailIndex);
        Rlog.d(TAG, "in updateAdnBySearch2()  needUpdateEmail: " + needUpdateEmail);
        Rlog.d(TAG, "in updateAdnBySearch2()  index: " + index + " emailIndex2nd: " + emailIndex2nd);
        Rlog.d(TAG, "in updateAdnBySearch2()  needUpdateEmail2nd: " + needUpdateEmail2nd);
        Rlog.d(TAG, "in updateAdnBySearch2()  index: " + index + " anrIndex: " + anrIndex);
        Rlog.d(TAG, "in updateAdnBySearch2()  needUpdateAnr: " + needUpdateAnr);
        Rlog.d(TAG, "in updateAdnBySearch2()  index: " + index + " sneIndex: " + sneIndex);
        Rlog.d(TAG, "in updateAdnBySearch2()  needUpdateSne: " + needUpdateSne);
        Rlog.d(TAG, "in updateAdnBySearch2()  index: " + index + " grpIndex: " + grpIndex);
        Rlog.d(TAG, "in updateAdnBySearch2()  needUpdateGrp: " + needUpdateGrp);
        if (index == -1) {
            sendErrorResponse(response, "Adn record don't exist for " + oldAdn);
        } else {
            updateEfByIndexes(index, efid, extensionEF, emailEfid2nd, oldAdn, newAdn, oldAdnList, needUpdateEmail, needUpdateEmail2nd, needUpdateAnr, needUpdateSne, needUpdateGrp, pbrRecordNum, emailIndex, emailIndex2nd, anrIndex, sneIndex, grpIndex, pin2, response);
        }
    }

    private void updateEfByIndexes(int index, int efid, int extensionEF, int emailEfid2nd, AdnRecord oldAdn, AdnRecord newAdn, ArrayList<AdnRecord> oldAdnList, boolean needUpdateEmail, boolean needUpdateEmail2nd, boolean needUpdateAnr, boolean needUpdateSne, boolean needUpdateGrp, int pbrRecordNum, int emailIndex, int emailIndex2nd, int anrIndex, int sneIndex, int grpIndex, String pin2, Message response) {
        int[] foundAdnEmailIndexes = {-1, -1};
        AdnRecord foundAdn = oldAdnList.get(index - 1);
        if (efid == 20272) {
            efid = foundAdn.mEfid;
            index = foundAdn.mRecordNumber;
            int[] newEmailEfids = {-1, -1};
            if (foundAdn.emailEfids != null) {
                newEmailEfids[0] = foundAdn.emailEfids[0];
            }
            if (foundAdn.emailEfids != null && newAdn.mEmails.length > 1) {
                if (foundAdn.emailEfids.length > 1) {
                    if (foundAdn.emailEfids[1] != -1) {
                        emailEfid2nd = foundAdn.emailEfids[1];
                    }
                    newEmailEfids[1] = emailEfid2nd;
                } else {
                    newEmailEfids[1] = emailEfid2nd;
                }
            }
            if (foundAdn.emailEfids != null) {
                newAdn.setEmailEfids(newEmailEfids);
            }
            if (foundAdn.anrEfids != null) {
                newAdn.setAnrEfids(new int[]{foundAdn.anrEfids[0]});
            }
            if (foundAdn.sneEfids != null) {
                newAdn.setSneEfids(new int[]{foundAdn.sneEfids[0]});
            }
            if (foundAdn.emailIndexes != null) {
                foundAdnEmailIndexes[0] = emailIndex;
                if (foundAdn.emailIndexes.length > 1 && foundAdn.emailIndexes[1] == -1) {
                    foundAdnEmailIndexes[1] = emailIndex2nd;
                } else if (foundAdn.emailIndexes.length > 1 && foundAdn.emailIndexes[1] != -1) {
                    foundAdnEmailIndexes[1] = foundAdn.emailIndexes[1];
                } else if (foundAdn.emailIndexes.length <= 1) {
                    foundAdnEmailIndexes[1] = emailIndex2nd;
                }
                int[] newEmailIndexes = {-1, -1};
                if (newAdn.mEmails == null || newAdn.mEmails[0] == null || newAdn.mEmails[0].equals("")) {
                    newEmailIndexes[0] = -1;
                } else {
                    newEmailIndexes[0] = foundAdnEmailIndexes[0];
                }
                if (newAdn.mEmails == null || newAdn.mEmails.length <= 1 || newAdn.mEmails[1] == null || newAdn.mEmails[1].equals("")) {
                    newEmailIndexes[1] = -1;
                } else {
                    newEmailIndexes[1] = emailIndex2nd;
                }
                newAdn.setEmailIndexes(newEmailIndexes);
            }
            if (newAdn.anrs[0] == null || newAdn.anrs[0].equals("")) {
                newAdn.setAnrIndexes(new int[]{-1});
            } else {
                newAdn.setAnrIndexes(new int[]{anrIndex});
            }
            Rlog.d(TAG, " sneIndex = " + sneIndex);
            if (newAdn.snes[0] == null || newAdn.snes[0].equals("")) {
                newAdn.setSneIndexes(new int[]{-1});
            } else {
                newAdn.setSneIndexes(new int[]{sneIndex});
            }
            if (foundAdn.grpEfid != -1) {
                newAdn.setGrpEfid(foundAdn.grpEfid);
            }
            int[] emailFiletypes = {-1, -1};
            if (foundAdn.emailFileTypes != null) {
                emailFiletypes[0] = foundAdn.emailFileTypes[0];
            }
            if (foundAdn.emailFileTypes != null && emailIndex2nd != -1) {
                emailFiletypes[1] = 169;
            }
            newAdn.setEmailFileTypes(emailFiletypes);
            if (foundAdn.anrFileTypes != null) {
                newAdn.setAnrFileTypes(new int[]{foundAdn.anrFileTypes[0]});
            }
            if (foundAdn.sneFileTypes != null) {
                newAdn.setSneFileTypes(new int[]{foundAdn.sneFileTypes[0]});
            }
            newAdn.mEfid = efid;
            newAdn.mExtRecord = foundAdn.mExtRecord;
            newAdn.extRecordAnr0 = foundAdn.extRecordAnr0;
            newAdn.mRecordNumber = index;
        } else {
            newAdn.mEmails = null;
            newAdn.anrs = null;
            newAdn.grps = null;
            newAdn.snes = null;
            newAdn.mEfid = efid;
            newAdn.mExtRecord = foundAdn.mExtRecord;
            newAdn.mRecordNumber = index;
        }
        Message pendingResponse = this.mUserWriteResponse.get(efid);
        if (pendingResponse != null) {
            sendErrorResponse(response, "Have pending update for EF:" + efid);
            return;
        }
        this.mUserWriteResponse.put(efid, response);
        if (efid == 20272) {
            if (needUpdateEmail) {
                updateAdnEmail(newAdn, foundAdnEmailIndexes[0], pbrRecordNum, pin2, 0);
            } else {
                this.mUpdateEmailSuccess = true;
            }
            if (needUpdateEmail2nd) {
                updateAdnEmail(newAdn, foundAdnEmailIndexes[1], pbrRecordNum, pin2, 1);
            } else {
                this.mUpdateEmailSuccess = true;
            }
            if (needUpdateAnr) {
                updateAdnAnr(newAdn, extensionEF, anrIndex, pbrRecordNum, pin2);
            } else {
                this.mUpdateAnrSuccess = true;
            }
            if (needUpdateSne) {
                updateAdnSne(newAdn, sneIndex, pbrRecordNum, pin2);
            } else {
                this.mUpdateSneSuccess = true;
            }
            if (needUpdateGrp) {
                updateAdnGrp(newAdn, grpIndex, pbrRecordNum, pin2);
            } else {
                this.mUpdateGrpSuccess = true;
            }
        }
        new AdnRecordLoader(this.mFh, this).updateEF(newAdn, efid, extensionEF, index, pin2, obtainMessage(4, index, efid, newAdn));
    }

    public void updateAdnGrpBySearch(int efid, AdnRecord oldAdn, String pin2, byte[] addgrp, Message response) {
        Rlog.d(TAG, "begin updateAdnGrpBySearch()");
        if (efid == 20272) {
            ArrayList<AdnRecord> oldAdnList = this.mUsimPhoneBookManager.getUsimPhoneBook();
            if (oldAdnList == null) {
                sendErrorResponse(response, "Adn list not exist for EF:" + efid);
                return;
            }
            int index = -1;
            int count = 1;
            int grpIndex = -1;
            boolean needUpdateGrp = false;
            int pbrRecordNum = 0;
            int adnCountInOneEf = oldAdnList.size() / this.mAdnLikeFiles.size();
            Rlog.d(TAG, "oldAdnList.size() = " + oldAdnList.size());
            Rlog.d(TAG, "adnLikeFiles.size() = " + this.mAdnLikeFiles.size());
            Rlog.d(TAG, "adnCountInOneEf = " + adnCountInOneEf);
            Iterator<AdnRecord> it = oldAdnList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                AdnRecord comparedAdn = it.next();
                if (oldAdn.isEqual(comparedAdn)) {
                    Rlog.d(TAG, "oldAdn.isEqual(comparedAdn) ");
                    pbrRecordNum = (count - 1) / adnCountInOneEf;
                    if (comparedAdn.grpEfid != -1) {
                        needUpdateGrp = true;
                        grpIndex = count - (pbrRecordNum * adnCountInOneEf);
                    }
                    index = count;
                } else {
                    count++;
                }
            }
            Rlog.d(TAG, "in updateAdnGrpBySearch()  index: " + index + ", grpIndex: " + grpIndex);
            Rlog.d(TAG, "in updateAdnGrpBySearch()  needUpdateGrp: " + needUpdateGrp);
            if (!needUpdateGrp || index == -1) {
                sendErrorResponse(response, "Adn record don't exist for " + oldAdn);
                return;
            }
            AdnRecord foundAdn = oldAdnList.get(index - 1);
            int i = foundAdn.grpEfid;
            int index2 = foundAdn.mRecordNumber;
            foundAdn.grps = new byte[0];
            Message pendingResponse = this.mUserWriteResponse.get(efid);
            if (pendingResponse != null) {
                sendErrorResponse(response, "Have pending update for EF:" + efid);
            } else {
                this.mUserWriteResponse.put(efid, response);
                updateAdnGrp(foundAdn, foundAdn.mRecordNumber, pbrRecordNum, pin2);
            }
        }
    }

    public void updateAdnGrpTagByIndex(int efid, String grpId, String grpTag, String pin2, Message response) {
        Rlog.d(TAG, "begin updateAdnGrpTagByIndex()");
        int index = Integer.parseInt(grpId);
        int gsdEf = this.mUsimPhoneBookManager.getGsdEf(index);
        if (gsdEf == -1) {
            sendErrorResponse(response, "GSD Ef not exist");
            return;
        }
        Rlog.d(TAG, "in updateAdnGrpTagByIndex()  grpId: " + grpId);
        Rlog.d(TAG, "in updateAdnGrpTagByIndex()  grpTag: " + grpTag);
        Rlog.d(TAG, "in updateAdnGrpTagByIndex()  gsdEf: " + gsdEf);
        if (index == -1) {
            sendErrorResponse(response, "Grp record don't exist");
            return;
        }
        Message pendingResponse = this.mUserWriteResponse.get(gsdEf);
        if (pendingResponse != null) {
            sendErrorResponse(response, "Have pending update for EF:" + gsdEf);
        } else {
            this.mUserWriteResponse.put(gsdEf, response);
            updateGsdTag(gsdEf, index, grpTag, pin2);
        }
    }

    private void updateAdnGrp(AdnRecord adn, int grpIndex, int recNum, String pin2) {
        Rlog.d(TAG, "begin updateAdnGrp()");
        synchronized (this.mLock) {
            int grpEF = adn.getGrpEfid();
            if (grpEF < 0) {
                this.mUpdateGrpSuccess = false;
                return;
            }
            if (grpEF == 0) {
                Rlog.d(TAG, "USIM card doesn't contain grpEF");
                this.mUpdateGrpSuccess = true;
                return;
            }
            Rlog.d(TAG, "grpEF = " + grpEF);
            if (grpIndex == -1 || grpIndex == 0) {
                this.mUpdateGrpSuccess = false;
                return;
            }
            new AdnRecordLoader(this.mFh, this).updateGrpEF(adn, grpEF, grpIndex, pin2, obtainMessage(6, recNum, grpIndex, adn));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(TAG, "Interrupted Exception in updateAdnGrp");
            }
        }
    }

    private void updateGsdTag(int gspEf, int grpId, String grpTag, String pin2) {
        Rlog.d(TAG, "begin updateGsdTag()");
        synchronized (this.mLock) {
            if (gspEf < 0) {
                this.mUpdateGsdSuccess = false;
                return;
            }
            if (grpId == -1 || grpId == 0) {
                this.mUpdateGsdSuccess = false;
                return;
            }
            new AdnRecordLoader(this.mFh, this).updateGsdEF(gspEf, grpId, grpTag, pin2, obtainMessage(8, gspEf, grpId, grpTag));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(TAG, "Interrupted Exception in updateGsdTag");
            }
        }
    }

    private void updateAdnEmail(AdnRecord adn, int emailIndex, int recNum, String pin2, int whichone) {
        Rlog.d(TAG, "begin updateAdnEmail()");
        synchronized (this.mLock) {
            int emailEF = adn.emailEfids[whichone];
            if (emailEF < 0) {
                this.mUpdateEmailSuccess = false;
                return;
            }
            Rlog.d(TAG, "emailEF = " + emailEF);
            if (emailIndex == -1 || emailIndex == 0) {
                this.mUpdateEmailSuccess = false;
                return;
            }
            String str = adn.mEmails[whichone];
            new AdnRecordLoader(this.mFh, this).updateEmailEF(adn, emailEF, emailIndex, adn.mRecordNumber, pin2, obtainMessage(3, recNum, emailIndex, adn));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(TAG, "Interrupted Exception in updateAdnEmail");
            }
        }
    }

    public void updateAdnAnr(AdnRecord adn, int extensionEF, int anrIndex, int recNum, String pin2) {
        Rlog.d(TAG, "begin updateAdnAnr()");
        synchronized (this.mLock) {
            int anrEF = adn.getAnrEfids()[0];
            if (anrEF < 0 || extensionEF < 0) {
                this.mUpdateAnrSuccess = false;
                return;
            }
            Rlog.d(TAG, "anrEF = " + anrEF);
            Rlog.d(TAG, "extensionEF = " + extensionEF);
            if (anrIndex == -1 || anrIndex == 0) {
                this.mUpdateAnrSuccess = false;
                return;
            }
            new AdnRecordLoader(this.mFh, this).updateAnrEF(adn, anrEF, extensionEF, anrIndex, adn.mRecordNumber, pin2, obtainMessage(5, recNum, anrIndex, adn));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(TAG, "Interrupted Exception in updateAdnAnr");
            }
        }
    }

    public void updateAdnSne(AdnRecord adn, int sneIndex, int recNum, String pin2) {
        Rlog.d(TAG, "begin updateAdnSne()");
        synchronized (this.mLock) {
            int sneEF = adn.getSneEfids()[0];
            if (sneEF < 0) {
                this.mUpdateSneSuccess = false;
                return;
            }
            Rlog.d(TAG, "sneEF = " + sneEF);
            Rlog.d(TAG, "sneIndex = " + sneIndex);
            if (sneIndex == -1 || sneIndex == 0) {
                this.mUpdateSneSuccess = false;
                return;
            }
            String str = adn.getSnes()[0];
            new AdnRecordLoader(this.mFh, this).updateSneEF(adn, sneEF, sneIndex, adn.mRecordNumber, pin2, obtainMessage(7, recNum, sneIndex, adn));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(TAG, "Interrupted Exception in updateAdnSne");
            }
        }
    }

    public void requestLoadUsimGroups(int efid, Message response) {
        Map<Integer, String> result;
        if (efid == 20272) {
            result = this.mUsimPhoneBookManager.getUsimGroups();
        } else {
            result = null;
        }
        if (result != null) {
            if (response != null) {
                AsyncResult.forMessage(response).result = result;
                response.sendToTarget();
                return;
            }
            return;
        }
        if (response != null) {
            AsyncResult.forMessage(response).exception = new RuntimeException("Load USIM Groups Fail");
            response.sendToTarget();
        }
    }

    public void requestLoadAllAdnLike(int efid, int extensionEf, Message response) {
        ArrayList<AdnRecord> result;
        int firstIndex;
        int used;
        if (!this.mCPPhonebookLoaded) {
            synchronized (this.mLock) {
                Rlog.d(TAG, "start load cp phonebook");
                this.mFh.loadCPPhonebookStatus(obtainMessage(9));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(TAG, "Interrupted Exception in updateAdnSne");
                }
            }
        }
        if (!this.mCPPhonebookLoaded) {
            if (response != null) {
                this.PBInitCount = 0;
                AsyncResult.forMessage(response).exception = new RuntimeException("CP phone book is not ready");
                response.sendToTarget();
                return;
            }
            return;
        }
        if (efid == 20272) {
            result = this.mUsimPhoneBookManager.loadEfFilesFromUsim();
        } else {
            result = getRecordsIfLoaded(efid);
        }
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
                AsyncResult.forMessage(response).exception = new RuntimeException("EF is not known ADN-like EF:" + efid);
                response.sendToTarget();
                return;
            }
            return;
        }
        ArrayList<AdnRecord> USIMPhonebook = this.mUsimPhoneBookManager.getUsimPhoneBook();
        Rlog.d(TAG, "USIMPhonebook.size(): " + USIMPhonebook.size());
        if (USIMPhonebook.size() == 0) {
            firstIndex = this.mCPPhoneBookFirstIndex;
            used = this.mCPPhoneBookUsed;
        } else {
            firstIndex = this.mCPPhoneBookFirstIndex > USIMPhonebook.size() ? this.mCPPhoneBookFirstIndex - USIMPhonebook.size() : 1;
            used = this.mCPPhoneBookUsed;
            for (int i = 0; i < USIMPhonebook.size(); i++) {
                AdnRecord rec = USIMPhonebook.get(i);
                if (rec != null && !rec.isEmpty()) {
                    used--;
                }
            }
        }
        if (efid == 28475 || efid == 28489) {
            firstIndex = 1;
            used = -1;
        }
        Rlog.d(TAG, "requestLoadAllAdnLike efid= " + efid + "firstIndex= " + firstIndex + " used= " + used);
        new AdnRecordLoader(this.mFh, this).loadRecordsFromEF(efid, extensionEf, firstIndex, used, obtainMessage(1, efid, 0));
    }

    private void notifyWaiters(ArrayList<Message> waiters, AsyncResult ar) {
        if (waiters != null) {
            int s = waiters.size();
            for (int i = 0; i < s; i++) {
                Message waiter = waiters.get(i);
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
                }
                if (ar.result == null || ((ArrayList) ar.result).isEmpty()) {
                    this.PBInitCount = 0;
                }
                notifyWaiters(waiters, ar);
                return;
            case 2:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                int efid2 = msg.arg1;
                int index = msg.arg2;
                AdnRecord adn = (AdnRecord) ar2.result;
                if (ar2.exception == null) {
                    this.mAdnLikeFiles.get(efid2).set(index - 1, adn);
                    this.mUsimPhoneBookManager.invalidateCache();
                }
                Message response = this.mUserWriteResponse.get(efid2);
                if (response != null) {
                    this.mUserWriteResponse.delete(efid2);
                    AsyncResult.forMessage(response, (Object) null, ar2.exception);
                    response.sendToTarget();
                    return;
                }
                return;
            case 3:
                Rlog.d(TAG, "EVENT_UPDATE_EMAIL_DONE");
                AsyncResult ar3 = (AsyncResult) msg.obj;
                int recNum = msg.arg1;
                int index2 = msg.arg2;
                AdnRecord adn2 = (AdnRecord) ar3.userObj;
                if (ar3.exception == null) {
                    this.mUpdateEmailSuccess = true;
                    this.mUpdateEmailSuccess = this.mUsimPhoneBookManager.updateUsimAdnEmail(adn2, recNum, index2);
                } else {
                    this.mUpdateEmailSuccess = false;
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 4:
                Rlog.d(TAG, "EVENT_UPDATE_ADN_DONE2");
                AsyncResult ar4 = (AsyncResult) msg.obj;
                int index3 = msg.arg1;
                int oldEfid = msg.arg2;
                AdnRecord adn3 = (AdnRecord) ar4.result;
                Rlog.v(TAG, "oldEfid = " + oldEfid);
                boolean updateADN2Success = false;
                if (ar4.exception == null) {
                    updateADN2Success = true;
                }
                Rlog.v(TAG, "mUpdateEmailSuccess = " + this.mUpdateEmailSuccess);
                Rlog.v(TAG, "mUpdateAnrSuccess = " + this.mUpdateAnrSuccess);
                Rlog.v(TAG, "mUpdateGrpSuccess = " + this.mUpdateGrpSuccess);
                Rlog.v(TAG, "mUpdateSneSuccess = " + this.mUpdateSneSuccess);
                Rlog.v(TAG, "updateADN2Success = " + updateADN2Success);
                if (!updateADN2Success || !this.mUpdateEmailSuccess || !this.mUpdateAnrSuccess || !this.mUpdateGrpSuccess || !this.mUpdateSneSuccess) {
                    ar4.exception = new Throwable();
                }
                if (ar4.exception == null) {
                    this.mAdnLikeFiles.get(adn3.mEfid).set(adn3.mRecordNumber - 1, adn3);
                    if (oldEfid == 20272) {
                        this.mUsimPhoneBookManager.updateUsimAdn(index3 - 1, adn3);
                    }
                }
                Rlog.v(TAG, "adn = " + adn3);
                Message response2 = this.mUserWriteResponse.get(oldEfid);
                if (response2 != null) {
                    this.mUserWriteResponse.delete(oldEfid);
                    AsyncResult.forMessage(response2, (Object) null, ar4.exception);
                    response2.sendToTarget();
                    return;
                }
                return;
            case 5:
                Rlog.d(TAG, "EVENT_UPDATE_ANR_DONE");
                AsyncResult ar5 = (AsyncResult) msg.obj;
                int recNum2 = msg.arg1;
                int index4 = msg.arg2;
                AdnRecord adn4 = (AdnRecord) ar5.result;
                if (ar5.exception == null) {
                    this.mUpdateAnrSuccess = this.mUsimPhoneBookManager.updateUsimAdnAnr(adn4, recNum2, index4);
                } else {
                    this.mUpdateAnrSuccess = false;
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 6:
                Rlog.d(TAG, "EVENT_UPDATE_GRP_DONE");
                AsyncResult ar6 = (AsyncResult) msg.obj;
                int recNum3 = msg.arg1;
                int index5 = msg.arg2;
                AdnRecord adn5 = (AdnRecord) ar6.userObj;
                if (ar6.exception == null) {
                    this.mUpdateGrpSuccess = this.mUsimPhoneBookManager.updateUsimAdnGrp(adn5, recNum3, index5);
                } else {
                    this.mUpdateGrpSuccess = false;
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 7:
                Rlog.d(TAG, "EVENT_UPDATE_SNE_DONE");
                AsyncResult ar7 = (AsyncResult) msg.obj;
                int recNum4 = msg.arg1;
                int index6 = msg.arg2;
                AdnRecord adn6 = (AdnRecord) ar7.userObj;
                if (ar7.exception == null) {
                    this.mUpdateSneSuccess = this.mUsimPhoneBookManager.updateUsimAdnSne(adn6, recNum4, index6);
                } else {
                    this.mUpdateSneSuccess = false;
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 8:
                Rlog.d(TAG, "EVENT_UPDATE_GSD_DONE");
                AsyncResult ar8 = (AsyncResult) msg.obj;
                int efid3 = msg.arg1;
                int i = msg.arg2;
                if (ar8.exception == null) {
                    this.mUpdateGsdSuccess = true;
                } else {
                    this.mUpdateGsdSuccess = false;
                }
                Message response3 = this.mUserWriteResponse.get(efid3);
                if (response3 != null) {
                    this.mUserWriteResponse.delete(efid3);
                    AsyncResult.forMessage(response3, (Object) null, ar8.exception);
                    response3.sendToTarget();
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 9:
                Rlog.d(TAG, "EVENT_LOAD_CP_PHONEBOOK_STATUS_DONE");
                AsyncResult ar9 = (AsyncResult) msg.obj;
                if (ar9.exception == null) {
                    if (ar9.result != null && ((int[]) ar9.result).length == 3) {
                        this.mCPPhoneBookTotal = ((int[]) ar9.result)[0];
                        this.mCPPhoneBookUsed = ((int[]) ar9.result)[1];
                        this.mCPPhoneBookFirstIndex = ((int[]) ar9.result)[2];
                        this.mCPPhonebookLoaded = true;
                        Rlog.d(TAG, "mCPPhoneBookTotal = " + this.mCPPhoneBookTotal);
                        Rlog.d(TAG, "mCPPhoneBookUsed = " + this.mCPPhoneBookUsed);
                        Rlog.d(TAG, "mCPPhoneBookFirstIndex = " + this.mCPPhoneBookFirstIndex);
                    } else {
                        Rlog.e(TAG, "ar.result is null");
                    }
                } else {
                    Rlog.e(TAG, "exception in get cp phone book inited" + ar9.exception);
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            default:
                return;
        }
    }
}
