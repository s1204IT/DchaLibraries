package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import com.android.internal.telephony.CommandsInterface;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.EFResponseData;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
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
    protected static final int EVENT_GET_BINARY_SIZE_DONE_EX = 101;
    protected static final int EVENT_GET_EF_LINEAR_RECORD_SIZE_DONE = 8;
    protected static final int EVENT_GET_RECORD_SIZE_DONE = 6;
    protected static final int EVENT_GET_RECORD_SIZE_IMG_DONE = 11;
    protected static final int EVENT_READ_BINARY_DONE = 5;
    protected static final int EVENT_READ_ICON_DONE = 10;
    protected static final int EVENT_READ_IMG_DONE = 9;
    protected static final int EVENT_READ_RECORD_DONE = 7;
    protected static final int EVENT_SELECT_EF_FILE = 100;
    protected static final int GET_RESPONSE_EF_IMG_SIZE_BYTES = 10;
    protected static final int GET_RESPONSE_EF_SIZE_BYTES = 15;
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
        boolean mLoadAll;
        int mMode;
        Message mOnLoaded;
        String mPath;
        int mRecordNum;
        int mRecordSize;
        ArrayList<byte[]> results;

        LoadLinearFixedContext(int efid, int recordNum, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = recordNum;
            this.mOnLoaded = onLoaded;
            this.mLoadAll = false;
            this.mPath = null;
            this.mMode = -1;
        }

        LoadLinearFixedContext(int efid, int recordNum, String path, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = recordNum;
            this.mOnLoaded = onLoaded;
            this.mLoadAll = false;
            this.mPath = path;
            this.mMode = -1;
        }

        LoadLinearFixedContext(int efid, String path, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = 1;
            this.mLoadAll = true;
            this.mOnLoaded = onLoaded;
            this.mPath = path;
            this.mMode = -1;
        }

        LoadLinearFixedContext(int efid, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = 1;
            this.mLoadAll = true;
            this.mOnLoaded = onLoaded;
            this.mPath = null;
            this.mMode = -1;
        }
    }

    static class LoadTransparentContext {
        int mEfid;
        Message mOnLoaded;
        String mPath;

        LoadTransparentContext(int efid, String path, Message onLoaded) {
            this.mEfid = efid;
            this.mPath = path;
            this.mOnLoaded = onLoaded;
        }
    }

    protected IccFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        this.mParentApp = app;
        this.mAid = aid;
        this.mCi = ci;
    }

    public void dispose() {
    }

    public void loadEFLinearFixed(int fileid, String path, int recordNum, Message onLoaded) {
        String efPath = path == null ? getEFPath(fileid) : path;
        Message response = obtainMessage(6, new LoadLinearFixedContext(fileid, recordNum, efPath, onLoaded));
        this.mCi.iccIOForApp(192, fileid, efPath, 0, 0, 15, null, null, this.mAid, response);
    }

    public void loadEFLinearFixed(int fileid, int recordNum, Message onLoaded) {
        loadEFLinearFixed(fileid, getEFPath(fileid), recordNum, onLoaded);
    }

    public void loadEFImgLinearFixed(int recordNum, Message onLoaded) {
        Message response = obtainMessage(11, new LoadLinearFixedContext(IccConstants.EF_IMG, recordNum, onLoaded));
        this.mCi.iccIOForApp(192, IccConstants.EF_IMG, getEFPath(IccConstants.EF_IMG), recordNum, 4, 15, null, null, this.mAid, response);
    }

    public void getEFLinearRecordSize(int fileid, String path, Message onLoaded) {
        String efPath = path == null ? getEFPath(fileid) : path;
        Message response = obtainMessage(8, new LoadLinearFixedContext(fileid, efPath, onLoaded));
        this.mCi.iccIOForApp(192, fileid, efPath, 0, 0, 15, null, null, this.mAid, response);
    }

    public void getEFLinearRecordSize(int fileid, Message onLoaded) {
        getEFLinearRecordSize(fileid, getEFPath(fileid), onLoaded);
    }

    public void loadEFLinearFixedAll(int fileid, String path, Message onLoaded) {
        String efPath = path == null ? getEFPath(fileid) : path;
        Message response = obtainMessage(6, new LoadLinearFixedContext(fileid, efPath, onLoaded));
        this.mCi.iccIOForApp(192, fileid, efPath, 0, 0, 15, null, null, this.mAid, response);
    }

    public void loadEFLinearFixedAll(int fileid, Message onLoaded) {
        loadEFLinearFixedAll(fileid, getEFPath(fileid), onLoaded);
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

    public void updateEFLinearFixed(int fileid, String path, int recordNum, byte[] data, String pin2, Message onComplete) {
        String efPath = path == null ? getEFPath(fileid) : path;
        this.mCi.iccIOForApp(COMMAND_UPDATE_RECORD, fileid, efPath, recordNum, 4, data.length, IccUtils.bytesToHexString(data), pin2, this.mAid, onComplete);
    }

    public void updateEFLinearFixed(int fileid, int recordNum, byte[] data, String pin2, Message onComplete) {
        this.mCi.iccIOForApp(COMMAND_UPDATE_RECORD, fileid, getEFPath(fileid), recordNum, 4, data.length, IccUtils.bytesToHexString(data), pin2, this.mAid, onComplete);
    }

    public void updateEFTransparent(int fileid, byte[] data, Message onComplete) {
        this.mCi.iccIOForApp(214, fileid, getEFPath(fileid), 0, 0, data.length, IccUtils.bytesToHexString(data), null, this.mAid, onComplete);
    }

    public void getPhbRecordInfo(Message response) {
        this.mCi.queryPhbStorageInfo(0, response);
    }

    private void sendResult(Message response, Object result, Throwable ex) {
        if (response == null) {
            return;
        }
        AsyncResult.forMessage(response, result, ex);
        response.sendToTarget();
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
        Message response = null;
        try {
            switch (msg.what) {
                case 4:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Message response2 = (Message) ar.userObj;
                    IccIoResult result = (IccIoResult) ar.result;
                    if (processException(response2, (AsyncResult) msg.obj)) {
                        return;
                    }
                    byte[] data = result.payload;
                    int fileid = msg.arg1;
                    if (4 != data[6]) {
                        throw new IccFileTypeMismatch();
                    }
                    if (data[13] != 0) {
                        throw new IccFileTypeMismatch();
                    }
                    int size = ((data[2] & PplMessageManager.Type.INVALID) << 8) + (data[3] & PplMessageManager.Type.INVALID);
                    this.mCi.iccIOForApp(176, fileid, getEFPath(fileid), 0, 0, size, null, null, this.mAid, obtainMessage(5, fileid, 0, response2));
                    return;
                case 5:
                case 10:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    Message response3 = (Message) ar2.userObj;
                    IccIoResult result2 = (IccIoResult) ar2.result;
                    if (processException(response3, (AsyncResult) msg.obj)) {
                        return;
                    }
                    sendResult(response3, result2.payload, null);
                    return;
                case 6:
                case 11:
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    LoadLinearFixedContext lc = (LoadLinearFixedContext) ar3.userObj;
                    IccIoResult result3 = (IccIoResult) ar3.result;
                    response = lc.mOnLoaded;
                    if (processException(response, (AsyncResult) msg.obj)) {
                        return;
                    }
                    byte[] data2 = result3.payload;
                    String path = lc.mPath;
                    try {
                        if (4 != data2[6]) {
                            throw new IccFileTypeMismatch();
                        }
                        if (1 != data2[13]) {
                            throw new IccFileTypeMismatch();
                        }
                        lc.mRecordSize = data2[14] & PplMessageManager.Type.INVALID;
                        int size2 = ((data2[2] & PplMessageManager.Type.INVALID) << 8) + (data2[3] & PplMessageManager.Type.INVALID);
                        lc.mCountRecords = size2 / lc.mRecordSize;
                        if (lc.mLoadAll) {
                            lc.results = new ArrayList<>(lc.mCountRecords);
                        }
                        if (lc.mMode != -1) {
                            this.mCi.iccIOForApp(178, lc.mEfid, getSmsEFPath(lc.mMode), lc.mRecordNum, 4, lc.mRecordSize, null, null, this.mAid, obtainMessage(7, lc));
                            return;
                        } else {
                            this.mCi.iccIOForApp(178, lc.mEfid, path == null ? getEFPath(lc.mEfid) : path, lc.mRecordNum, 4, lc.mRecordSize, null, null, this.mAid, obtainMessage(7, lc));
                            return;
                        }
                    } catch (Exception e) {
                        exc = e;
                        break;
                    }
                    break;
                case 7:
                case 9:
                    AsyncResult ar4 = (AsyncResult) msg.obj;
                    LoadLinearFixedContext lc2 = (LoadLinearFixedContext) ar4.userObj;
                    IccIoResult result4 = (IccIoResult) ar4.result;
                    Message response4 = lc2.mOnLoaded;
                    String path2 = lc2.mPath;
                    if (processException(response4, (AsyncResult) msg.obj)) {
                        return;
                    }
                    if (!lc2.mLoadAll) {
                        sendResult(response4, result4.payload, null);
                        return;
                    }
                    lc2.results.add(result4.payload);
                    lc2.mRecordNum++;
                    if (lc2.mRecordNum > lc2.mCountRecords) {
                        sendResult(response4, lc2.results, null);
                        return;
                    } else {
                        if (lc2.mMode != -1) {
                            this.mCi.iccIOForApp(178, lc2.mEfid, getSmsEFPath(lc2.mMode), lc2.mRecordNum, 4, lc2.mRecordSize, null, null, this.mAid, obtainMessage(7, lc2));
                            return;
                        }
                        if (path2 == null) {
                            path2 = getEFPath(lc2.mEfid);
                        }
                        this.mCi.iccIOForApp(178, lc2.mEfid, path2, lc2.mRecordNum, 4, lc2.mRecordSize, null, null, this.mAid, obtainMessage(7, lc2));
                        return;
                    }
                case 8:
                    AsyncResult ar5 = (AsyncResult) msg.obj;
                    LoadLinearFixedContext lc3 = (LoadLinearFixedContext) ar5.userObj;
                    IccIoResult result5 = (IccIoResult) ar5.result;
                    Message response5 = lc3.mOnLoaded;
                    if (processException(response5, (AsyncResult) msg.obj)) {
                        return;
                    }
                    byte[] data3 = result5.payload;
                    if (4 != data3[6] || 1 != data3[13]) {
                        throw new IccFileTypeMismatch();
                    }
                    int[] recordSize = new int[3];
                    recordSize[0] = data3[14] & PplMessageManager.Type.INVALID;
                    recordSize[1] = ((data3[2] & PplMessageManager.Type.INVALID) << 8) + (data3[3] & PplMessageManager.Type.INVALID);
                    recordSize[2] = recordSize[1] / recordSize[0];
                    sendResult(response5, recordSize, null);
                    return;
                case 100:
                    AsyncResult ar6 = (AsyncResult) msg.obj;
                    Message response6 = (Message) ar6.userObj;
                    IccIoResult result6 = (IccIoResult) ar6.result;
                    if (processException(response6, (AsyncResult) msg.obj)) {
                        loge("EVENT_SELECT_EF_FILE exception");
                        return;
                    }
                    byte[] data4 = result6.payload;
                    if (4 != data4[6]) {
                        throw new IccFileTypeMismatch();
                    }
                    Object efData = new EFResponseData(data4);
                    sendResult(response6, efData, null);
                    return;
                case 101:
                    AsyncResult ar7 = (AsyncResult) msg.obj;
                    LoadTransparentContext tc = (LoadTransparentContext) ar7.userObj;
                    IccIoResult result7 = (IccIoResult) ar7.result;
                    Message response7 = tc.mOnLoaded;
                    String path3 = tc.mPath;
                    if (processException(response7, (AsyncResult) msg.obj)) {
                        return;
                    }
                    byte[] data5 = result7.payload;
                    if (4 != data5[6]) {
                        throw new IccFileTypeMismatch();
                    }
                    if (data5[13] != 0) {
                        throw new IccFileTypeMismatch();
                    }
                    int size3 = ((data5[2] & PplMessageManager.Type.INVALID) << 8) + (data5[3] & PplMessageManager.Type.INVALID);
                    if (path3 == null) {
                        path3 = getEFPath(tc.mEfid);
                    }
                    this.mCi.iccIOForApp(176, tc.mEfid, path3, 0, 0, size3, null, null, this.mAid, obtainMessage(5, tc.mEfid, 0, response7));
                    return;
                default:
                    return;
            }
        } catch (Exception e2) {
            exc = e2;
        }
        if (response != null) {
            loge("caught exception:" + exc);
            sendResult(response, null, exc);
        } else {
            loge("uncaught exception" + exc);
        }
    }

    protected String getCommonIccEFPath(int efid) {
        switch (efid) {
            case 12037:
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

    public void loadEFLinearFixedAll(int fileid, Message onLoaded, boolean is7FFF) {
        Message response = obtainMessage(6, new LoadLinearFixedContext(fileid, onLoaded));
        this.mCi.iccIOForApp(192, fileid, getEFPath(fileid), 0, 0, 15, null, null, this.mAid, response);
    }

    public void loadEFLinearFixedAll(int fileid, int mode, Message onLoaded) {
        LoadLinearFixedContext lc = new LoadLinearFixedContext(fileid, onLoaded);
        lc.mMode = mode;
        Message response = obtainMessage(6, lc);
        this.mCi.iccIOForApp(192, fileid, getSmsEFPath(mode), 0, 0, 15, null, null, this.mAid, response);
    }

    protected String getSmsEFPath(int mode) {
        if (mode == 1) {
            return "3F007F10";
        }
        if (mode != 2) {
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        return "3F007F25";
    }

    public void loadEFTransparent(int fileid, String path, Message onLoaded) {
        String efPath = path == null ? getEFPath(fileid) : path;
        Message response = obtainMessage(101, new LoadTransparentContext(fileid, efPath, onLoaded));
        this.mCi.iccIOForApp(192, fileid, efPath, 0, 0, 15, null, null, this.mAid, response);
    }

    public void updateEFTransparent(int fileid, String path, byte[] data, Message onComplete) {
        String efPath = path == null ? getEFPath(fileid) : path;
        this.mCi.iccIOForApp(214, fileid, efPath, 0, 0, data.length, IccUtils.bytesToHexString(data), null, this.mAid, onComplete);
    }

    public void readEFLinearFixed(int fileid, int recordNum, int recordSize, Message onLoaded) {
        this.mCi.iccIOForApp(178, fileid, getEFPath(fileid), recordNum, 4, recordSize, null, null, this.mAid, onLoaded);
    }

    public void selectEFFile(int fileid, Message onLoaded) {
        Message response = obtainMessage(100, fileid, 0, onLoaded);
        this.mCi.iccIOForApp(192, fileid, getEFPath(fileid), 0, 0, 15, null, null, this.mAid, response);
    }
}
