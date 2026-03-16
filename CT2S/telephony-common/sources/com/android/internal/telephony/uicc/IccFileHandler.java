package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import java.util.ArrayList;

public abstract class IccFileHandler extends Handler implements IccConstants {
    protected static final int COMMAND_GET_RESPONSE = 192;
    protected static final int COMMAND_READ_BINARY = 176;
    protected static final int COMMAND_READ_RECORD = 178;
    protected static final int COMMAND_SEEK = 162;
    protected static final int COMMAND_UPDATE_BINARY = 214;
    protected static final int COMMAND_UPDATE_RECORD = 220;
    protected static final int EF_TYPE_CYCLIC = 3;
    protected static final int EF_TYPE_LINEAR_FIXED = 1;
    protected static final int EF_TYPE_TRANSPARENT = 0;
    protected static final int EVENT_GET_BINARY_SIZE_DONE = 4;
    protected static final int EVENT_GET_EF_LINEAR_RECORD_SIZE_DONE = 8;
    protected static final int EVENT_GET_RECORD_SIZE_DONE = 6;
    protected static final int EVENT_GET_RECORD_SIZE_IMG_DONE = 11;
    protected static final int EVENT_READ_BINARY_DONE = 5;
    protected static final int EVENT_READ_ICON_DONE = 10;
    protected static final int EVENT_READ_IMG_DONE = 9;
    protected static final int EVENT_READ_RECORD_DONE = 7;
    protected static final int GET_RESPONSE_EF_IMG_SIZE_BYTES = 10;
    protected static final int GET_RESPONSE_EF_SIZE_BYTES = 15;
    static final String LOG_TAG = "IccFileHandler";
    protected static final int READ_RECORD_MODE_ABSOLUTE = 4;
    protected static final int RESPONSE_DATA_ACCESS_CONDITION_1 = 8;
    protected static final int RESPONSE_DATA_ACCESS_CONDITION_2 = 9;
    protected static final int RESPONSE_DATA_ACCESS_CONDITION_3 = 10;
    protected static final int RESPONSE_DATA_FILE_ID_1 = 4;
    protected static final int RESPONSE_DATA_FILE_ID_2 = 5;
    protected static final int RESPONSE_DATA_FILE_SIZE_1 = 2;
    protected static final int RESPONSE_DATA_FILE_SIZE_2 = 3;
    protected static final int RESPONSE_DATA_FILE_STATUS = 11;
    protected static final int RESPONSE_DATA_FILE_TYPE = 6;
    protected static final int RESPONSE_DATA_LENGTH = 12;
    protected static final int RESPONSE_DATA_RECORD_LENGTH = 14;
    protected static final int RESPONSE_DATA_RFU_1 = 0;
    protected static final int RESPONSE_DATA_RFU_2 = 1;
    protected static final int RESPONSE_DATA_RFU_3 = 7;
    protected static final int RESPONSE_DATA_STRUCTURE = 13;
    protected static final int TYPE_DF = 2;
    protected static final int TYPE_EF = 4;
    protected static final int TYPE_MF = 1;
    protected static final int TYPE_RFU = 0;
    protected final String mAid;
    protected final CommandsInterface mCi;
    protected final UiccCardApplication mParentApp;

    protected abstract String getEFPath(int i);

    protected abstract void logd(String str);

    protected abstract void loge(String str);

    static class LoadLinearFixedContext {
        int mCountRecords;
        int mEfid;
        int mFirstIndex;
        ArrayList<Integer> mIndexes;
        boolean mLoadAll;
        int mNeedLoadNum;
        Message mOnLoaded;
        int mRecordLoaded;
        int mRecordNum;
        int mRecordSize;
        ArrayList<byte[]> results;

        LoadLinearFixedContext(int efid, int recordNum, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = recordNum;
            this.mOnLoaded = onLoaded;
            this.mLoadAll = false;
            this.mNeedLoadNum = -1;
        }

        LoadLinearFixedContext(int efid, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = 1;
            this.mLoadAll = true;
            this.mOnLoaded = onLoaded;
            this.mNeedLoadNum = -1;
        }

        LoadLinearFixedContext(int efid, int firstIndex, int needLoadNum, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = firstIndex;
            this.mLoadAll = true;
            this.mFirstIndex = firstIndex;
            this.mOnLoaded = onLoaded;
            this.mNeedLoadNum = needLoadNum;
        }

        LoadLinearFixedContext(int efid, ArrayList<Integer> indexes, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = 1;
            this.mLoadAll = true;
            this.mOnLoaded = onLoaded;
            this.mIndexes = indexes;
            this.mNeedLoadNum = -1;
        }
    }

    protected IccFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        this.mParentApp = app;
        this.mAid = aid;
        this.mCi = ci;
    }

    public void dispose() {
    }

    public void loadEFLinearFixed(int fileid, int recordNum, Message onLoaded) {
        Message response = obtainMessage(6, new LoadLinearFixedContext(fileid, recordNum, onLoaded));
        this.mCi.iccIOForApp(192, fileid, getEFPath(fileid), 0, 0, 15, null, null, this.mAid, response);
    }

    public void loadEFImgLinearFixed(int recordNum, Message onLoaded) {
        Message response = obtainMessage(11, new LoadLinearFixedContext(IccConstants.EF_IMG, recordNum, onLoaded));
        this.mCi.iccIOForApp(192, IccConstants.EF_IMG, getEFPath(IccConstants.EF_IMG), recordNum, 4, 10, null, null, this.mAid, response);
    }

    public void loadEFLinearFixedWithRecordSize(int fileid, int recordNum, int recordSize, Message onLoaded) {
        LoadLinearFixedContext lc = new LoadLinearFixedContext(fileid, recordNum, onLoaded);
        lc.mEfid = fileid;
        lc.mRecordNum = recordNum;
        lc.mRecordSize = recordSize;
        this.mCi.iccIOForApp(178, lc.mEfid, getEFPath(lc.mEfid), lc.mRecordNum, 4, lc.mRecordSize, null, null, this.mAid, obtainMessage(7, lc));
    }

    public void getEFLinearRecordSize(int fileid, Message onLoaded) {
        Message response = obtainMessage(8, new LoadLinearFixedContext(fileid, onLoaded));
        this.mCi.iccIOForApp(192, fileid, getEFPath(fileid), 0, 0, 15, null, null, this.mAid, response);
    }

    public void loadEFLinearFixedAll(int fileid, Message onLoaded) {
        Message response = obtainMessage(6, new LoadLinearFixedContext(fileid, onLoaded));
        this.mCi.iccIOForApp(192, fileid, getEFPath(fileid), 0, 0, 15, null, null, this.mAid, response);
    }

    public void loadEFLinearFixedAll(int fileid, int recordNum, int countRecords, Message onLoaded) {
        Message response = obtainMessage(6, new LoadLinearFixedContext(fileid, recordNum, countRecords, onLoaded));
        this.mCi.iccIOForApp(192, fileid, getEFPath(fileid), 0, 0, 15, null, null, this.mAid, response);
    }

    public void loadEFLinearFixedByIndexes(int fileid, ArrayList<Integer> indexes, Message onLoaded) {
        Message response = obtainMessage(6, new LoadLinearFixedContext(fileid, indexes, onLoaded));
        this.mCi.iccIOForApp(192, fileid, getEFPath(fileid), 0, 0, 15, null, null, this.mAid, response);
    }

    public void loadEFTransparent(int fileid, Message onLoaded) {
        Message response = obtainMessage(4, fileid, 0, onLoaded);
        this.mCi.iccIOForApp(192, fileid, getEFPath(fileid), 0, 0, 15, null, null, this.mAid, response);
    }

    public void loadEFTransparent(int fileid, int size, Message onLoaded) {
        Message response = obtainMessage(5, fileid, 0, onLoaded);
        this.mCi.iccIOForApp(176, fileid, getEFPath(fileid), 0, 0, size, null, null, this.mAid, response);
    }

    public void loadEFImgTransparent(int fileid, int highOffset, int lowOffset, int length, Message onLoaded) {
        Message response = obtainMessage(10, fileid, 0, onLoaded);
        logd("IccFileHandler: loadEFImgTransparent fileid = " + fileid + " filePath = " + getEFPath(IccConstants.EF_IMG) + " highOffset = " + highOffset + " lowOffset = " + lowOffset + " length = " + length);
        this.mCi.iccIOForApp(176, fileid, getEFPath(IccConstants.EF_IMG), highOffset, lowOffset, length, null, null, this.mAid, response);
    }

    public void updateEFLinearFixed(int fileid, int recordNum, byte[] data, String pin2, Message onComplete) {
        this.mCi.iccIOForApp(COMMAND_UPDATE_RECORD, fileid, getEFPath(fileid), recordNum, 4, data.length, IccUtils.bytesToHexString(data), pin2, this.mAid, onComplete);
    }

    public void updateEFTransparent(int fileid, byte[] data, Message onComplete) {
        this.mCi.iccIOForApp(214, fileid, getEFPath(fileid), 0, 0, data.length, IccUtils.bytesToHexString(data), null, this.mAid, onComplete);
    }

    public void loadCPPhonebookStatus(Message onComplete) {
        this.mCi.getCPPhonebookStatus(onComplete);
    }

    private void sendResult(Message response, Object result, Throwable ex) {
        if (response != null) {
            AsyncResult.forMessage(response, result, ex);
            response.sendToTarget();
        }
    }

    private boolean processException(Message response, AsyncResult ar) {
        IccIoResult result = (IccIoResult) ar.result;
        if (ar.exception != null) {
            sendResult(response, null, ar.exception);
            return true;
        }
        IccException iccException = result.getException();
        if (iccException == null) {
            return false;
        }
        sendResult(response, null, iccException);
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            switch (msg.what) {
                case 4:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Message response = (Message) ar.userObj;
                    IccIoResult result = (IccIoResult) ar.result;
                    if (!processException(response, (AsyncResult) msg.obj)) {
                        byte[] data = result.payload;
                        int fileid = msg.arg1;
                        if (4 != data[6]) {
                            throw new IccFileTypeMismatch();
                        }
                        if (data[13] != 0) {
                            throw new IccFileTypeMismatch();
                        }
                        int size = ((data[2] & 255) << 8) + (data[3] & 255);
                        this.mCi.iccIOForApp(176, fileid, getEFPath(fileid), 0, 0, size, null, null, this.mAid, obtainMessage(5, fileid, 0, response));
                        return;
                    }
                    return;
                case 5:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    Message response2 = (Message) ar2.userObj;
                    IccIoResult result2 = (IccIoResult) ar2.result;
                    if (!processException(response2, (AsyncResult) msg.obj)) {
                        sendResult(response2, result2.payload, null);
                        return;
                    }
                    return;
                case 6:
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    LoadLinearFixedContext lc = (LoadLinearFixedContext) ar3.userObj;
                    IccIoResult result3 = (IccIoResult) ar3.result;
                    Message response3 = lc.mOnLoaded;
                    if (!processException(response3, (AsyncResult) msg.obj)) {
                        byte[] data2 = result3.payload;
                        if (4 != data2[6]) {
                            throw new IccFileTypeMismatch();
                        }
                        if (1 != data2[13]) {
                            throw new IccFileTypeMismatch();
                        }
                        lc.mRecordSize = data2[14] & 255;
                        int size2 = ((data2[2] & 255) << 8) + (data2[3] & 255);
                        lc.mCountRecords = size2 / lc.mRecordSize;
                        if (lc.mLoadAll) {
                            lc.results = new ArrayList<>(lc.mCountRecords);
                        }
                        if (lc.mNeedLoadNum == 0 || lc.mFirstIndex > lc.mCountRecords) {
                            for (int i = 0; i < lc.mCountRecords; i++) {
                                byte[] nullData = new byte[lc.mRecordSize];
                                for (int j = 0; j < lc.mRecordSize; j++) {
                                    nullData[j] = -1;
                                }
                                lc.results.add(i, nullData);
                            }
                            Rlog.d(LOG_TAG, "send null data if mNeedLoadNum is 0");
                            sendResult(response3, lc.results, null);
                            return;
                        }
                        if (lc.mIndexes != null) {
                            Rlog.d(LOG_TAG, "load record with indexes");
                            for (int i2 = 0; i2 < lc.mCountRecords; i2++) {
                                byte[] nullData2 = new byte[lc.mRecordSize];
                                for (int j2 = 0; j2 < lc.mRecordSize; j2++) {
                                    nullData2[j2] = -1;
                                }
                                lc.results.add(i2, nullData2);
                            }
                            if (lc.mIndexes.isEmpty()) {
                                sendResult(response3, lc.results, null);
                                return;
                            }
                            CommandsInterface commandsInterface = this.mCi;
                            int i3 = lc.mEfid;
                            String eFPath = getEFPath(lc.mEfid);
                            int iIntValue = lc.mIndexes.get(lc.mRecordNum - 1).intValue();
                            int size3 = lc.mRecordSize;
                            commandsInterface.iccIOForApp(178, i3, eFPath, iIntValue, 4, size3, null, null, this.mAid, obtainMessage(7, lc));
                            return;
                        }
                        CommandsInterface commandsInterface2 = this.mCi;
                        int i4 = lc.mEfid;
                        String eFPath2 = getEFPath(lc.mEfid);
                        int i5 = lc.mRecordNum;
                        int size4 = lc.mRecordSize;
                        commandsInterface2.iccIOForApp(178, i4, eFPath2, i5, 4, size4, null, null, this.mAid, obtainMessage(7, lc));
                        return;
                    }
                    return;
                case 7:
                    AsyncResult ar4 = (AsyncResult) msg.obj;
                    LoadLinearFixedContext lc2 = (LoadLinearFixedContext) ar4.userObj;
                    IccIoResult result4 = (IccIoResult) ar4.result;
                    Message response4 = lc2.mOnLoaded;
                    if (!processException(response4, (AsyncResult) msg.obj)) {
                        if (!lc2.mLoadAll) {
                            sendResult(response4, result4.payload, null);
                            return;
                        }
                        if (lc2.mIndexes != null) {
                            lc2.results.set(lc2.mIndexes.get(lc2.mRecordNum - 1).intValue() - 1, result4.payload);
                            lc2.mRecordNum++;
                            if (lc2.mRecordNum <= lc2.mIndexes.size()) {
                                this.mCi.iccIOForApp(178, lc2.mEfid, getEFPath(lc2.mEfid), lc2.mIndexes.get(lc2.mRecordNum - 1).intValue(), 4, lc2.mRecordSize, null, null, this.mAid, obtainMessage(7, lc2));
                                return;
                            } else {
                                sendResult(response4, lc2.results, null);
                                return;
                            }
                        }
                        lc2.results.add(result4.payload);
                        if (!isNoDataRecord(result4.payload)) {
                            lc2.mRecordLoaded++;
                        }
                        lc2.mRecordNum++;
                        if (lc2.mNeedLoadNum != -1 && lc2.mRecordLoaded >= lc2.mNeedLoadNum) {
                            for (int i6 = 0; i6 < lc2.mFirstIndex - 1; i6++) {
                                byte[] nullData3 = new byte[lc2.mRecordSize];
                                for (int j3 = 0; j3 < lc2.mRecordSize; j3++) {
                                    nullData3[j3] = -1;
                                }
                                lc2.results.add(i6, nullData3);
                            }
                            for (int i7 = lc2.mRecordNum - 1; i7 < lc2.mCountRecords; i7++) {
                                byte[] nullData4 = new byte[lc2.mRecordSize];
                                for (int j4 = 0; j4 < lc2.mRecordSize; j4++) {
                                    nullData4[j4] = -1;
                                }
                                lc2.results.add(nullData4);
                            }
                            sendResult(response4, lc2.results, null);
                            return;
                        }
                        if (lc2.mRecordNum > lc2.mCountRecords) {
                            for (int i8 = 0; i8 < lc2.mFirstIndex - 1; i8++) {
                                byte[] nullData5 = new byte[lc2.mRecordSize];
                                for (int j5 = 0; j5 < lc2.mRecordSize; j5++) {
                                    nullData5[j5] = -1;
                                }
                                lc2.results.add(i8, nullData5);
                            }
                            sendResult(response4, lc2.results, null);
                            return;
                        }
                        this.mCi.iccIOForApp(178, lc2.mEfid, getEFPath(lc2.mEfid), lc2.mRecordNum, 4, lc2.mRecordSize, null, null, this.mAid, obtainMessage(7, lc2));
                        return;
                    }
                    return;
                case 8:
                    AsyncResult ar5 = (AsyncResult) msg.obj;
                    LoadLinearFixedContext lc3 = (LoadLinearFixedContext) ar5.userObj;
                    IccIoResult result5 = (IccIoResult) ar5.result;
                    Message response5 = lc3.mOnLoaded;
                    if (!processException(response5, (AsyncResult) msg.obj)) {
                        byte[] data3 = result5.payload;
                        if (4 != data3[6] || 1 != data3[13]) {
                            throw new IccFileTypeMismatch();
                        }
                        int[] recordSize = new int[3];
                        recordSize[0] = data3[14] & 255;
                        recordSize[1] = ((data3[2] & 255) << 8) + (data3[3] & 255);
                        recordSize[2] = recordSize[1] / recordSize[0];
                        sendResult(response5, recordSize, null);
                        return;
                    }
                    return;
                case 9:
                    AsyncResult ar6 = (AsyncResult) msg.obj;
                    LoadLinearFixedContext lc4 = (LoadLinearFixedContext) ar6.userObj;
                    IccIoResult result6 = (IccIoResult) ar6.result;
                    Message response6 = lc4.mOnLoaded;
                    IccException iccException = result6.getException();
                    if (iccException != null) {
                        sendResult(response6, result6.payload, ar6.exception);
                        return;
                    } else {
                        sendResult(response6, result6.payload, null);
                        return;
                    }
                case 10:
                    AsyncResult ar7 = (AsyncResult) msg.obj;
                    Message response7 = (Message) ar7.userObj;
                    IccIoResult result7 = (IccIoResult) ar7.result;
                    IccException iccException2 = result7.getException();
                    if (iccException2 != null) {
                        sendResult(response7, result7.payload, ar7.exception);
                        return;
                    } else {
                        sendResult(response7, result7.payload, null);
                        return;
                    }
                case 11:
                    logd("IccFileHandler: get record size img done");
                    AsyncResult ar8 = (AsyncResult) msg.obj;
                    LoadLinearFixedContext lc5 = (LoadLinearFixedContext) ar8.userObj;
                    IccIoResult result8 = (IccIoResult) ar8.result;
                    Message response8 = lc5.mOnLoaded;
                    if (ar8.exception != null) {
                        sendResult(response8, null, ar8.exception);
                        return;
                    }
                    Throwable iccException3 = result8.getException();
                    if (iccException3 != null) {
                        sendResult(response8, null, iccException3);
                        return;
                    }
                    byte[] data4 = result8.payload;
                    lc5.mRecordSize = data4[14] & 255;
                    if (4 != data4[6] || 1 != data4[13]) {
                        loge("IccFileHandler: File type mismatch: Throw Exception");
                        throw new IccFileTypeMismatch();
                    }
                    logd("IccFileHandler: read EF IMG");
                    this.mCi.iccIOForApp(178, lc5.mEfid, getEFPath(lc5.mEfid), lc5.mRecordNum, 4, lc5.mRecordSize, null, null, this.mAid, obtainMessage(9, lc5));
                    return;
                default:
                    return;
            }
        } catch (Exception exc) {
            if (0 != 0) {
                sendResult(null, null, exc);
            } else {
                loge("uncaught exception" + exc);
            }
        }
    }

    private boolean isNoDataRecord(byte[] payload) {
        if (payload == null) {
            return true;
        }
        for (byte b : payload) {
            if (b != -1) {
                return false;
            }
        }
        return true;
    }

    protected String getCommonIccEFPath(int efid) {
        switch (efid) {
            case IccConstants.EF_PL:
            case IccConstants.EF_ICCID:
                return IccConstants.MF_SIM;
            case IccConstants.EF_IMG:
                return "3F007F105F50";
            case IccConstants.EF_PBR:
                return "3F007F105F3A";
            case 28474:
            case IccConstants.EF_FDN:
            case IccConstants.EF_MSISDN:
            case IccConstants.EF_SDN:
            case IccConstants.EF_EXT1:
            case IccConstants.EF_EXT2:
            case IccConstants.EF_EXT3:
            case 28645:
                return "3F007F10";
            default:
                return null;
        }
    }
}
