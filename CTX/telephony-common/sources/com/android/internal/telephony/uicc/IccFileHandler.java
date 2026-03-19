package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
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
    private static final boolean VDBG = false;
    protected final String mAid;
    public final CommandsInterface mCi;
    protected final UiccCardApplication mParentApp;

    protected abstract String getEFPath(int i);

    protected abstract void logd(String str);

    protected abstract void loge(String str);

    static class LoadLinearFixedContext {
        int mCountRecords;
        int mEfid;
        boolean mLoadAll;
        Message mOnLoaded;
        String mPath;
        int mRecordNum;
        int mRecordSize;
        ArrayList<byte[]> results;

        LoadLinearFixedContext(int i, int i2, Message message) {
            this.mEfid = i;
            this.mRecordNum = i2;
            this.mOnLoaded = message;
            this.mLoadAll = false;
            this.mPath = null;
        }

        LoadLinearFixedContext(int i, int i2, String str, Message message) {
            this.mEfid = i;
            this.mRecordNum = i2;
            this.mOnLoaded = message;
            this.mLoadAll = false;
            this.mPath = str;
        }

        LoadLinearFixedContext(int i, String str, Message message) {
            this.mEfid = i;
            this.mRecordNum = 1;
            this.mLoadAll = true;
            this.mOnLoaded = message;
            this.mPath = str;
        }

        LoadLinearFixedContext(int i, Message message) {
            this.mEfid = i;
            this.mRecordNum = 1;
            this.mLoadAll = true;
            this.mOnLoaded = message;
            this.mPath = null;
        }
    }

    protected IccFileHandler(UiccCardApplication uiccCardApplication, String str, CommandsInterface commandsInterface) {
        this.mParentApp = uiccCardApplication;
        this.mAid = str;
        this.mCi = commandsInterface;
    }

    public void dispose() {
    }

    public void loadEFLinearFixed(int i, String str, int i2, Message message) {
        if (str == null) {
            str = getEFPath(i);
        }
        String str2 = str;
        this.mCi.iccIOForApp(192, i, str2, 0, 0, 15, null, null, this.mAid, obtainMessage(6, new LoadLinearFixedContext(i, i2, str2, message)));
    }

    public void loadEFLinearFixed(int i, int i2, Message message) {
        loadEFLinearFixed(i, getEFPath(i), i2, message);
    }

    public void loadEFImgLinearFixed(int i, Message message) {
        this.mCi.iccIOForApp(192, IccConstants.EF_IMG, getEFPath(IccConstants.EF_IMG), i, 4, 15, null, null, this.mAid, obtainMessage(11, new LoadLinearFixedContext(IccConstants.EF_IMG, i, message)));
    }

    public void getEFLinearRecordSize(int i, String str, Message message) {
        if (str == null) {
            str = getEFPath(i);
        }
        String str2 = str;
        this.mCi.iccIOForApp(192, i, str2, 0, 0, 15, null, null, this.mAid, obtainMessage(8, new LoadLinearFixedContext(i, str2, message)));
    }

    public void getEFLinearRecordSize(int i, Message message) {
        getEFLinearRecordSize(i, getEFPath(i), message);
    }

    public void loadEFLinearFixedAll(int i, String str, Message message) {
        if (str == null) {
            str = getEFPath(i);
        }
        String str2 = str;
        this.mCi.iccIOForApp(192, i, str2, 0, 0, 15, null, null, this.mAid, obtainMessage(6, new LoadLinearFixedContext(i, str2, message)));
    }

    public void loadEFLinearFixedAll(int i, Message message) {
        loadEFLinearFixedAll(i, getEFPath(i), message);
    }

    public void loadEFTransparent(int i, Message message) {
        this.mCi.iccIOForApp(192, i, getEFPath(i), 0, 0, 15, null, null, this.mAid, obtainMessage(4, i, 0, message));
    }

    public void loadEFTransparent(int i, int i2, Message message) {
        this.mCi.iccIOForApp(176, i, getEFPath(i), 0, 0, i2, null, null, this.mAid, obtainMessage(5, i, 0, message));
    }

    public void loadEFImgTransparent(int i, int i2, int i3, int i4, Message message) {
        Message messageObtainMessage = obtainMessage(10, i, 0, message);
        logd("IccFileHandler: loadEFImgTransparent fileid = " + i + " filePath = " + getEFPath(IccConstants.EF_IMG) + " highOffset = " + i2 + " lowOffset = " + i3 + " length = " + i4);
        this.mCi.iccIOForApp(176, i, getEFPath(IccConstants.EF_IMG), i2, i3, i4, null, null, this.mAid, messageObtainMessage);
    }

    public void updateEFLinearFixed(int i, String str, int i2, byte[] bArr, String str2, Message message) {
        this.mCi.iccIOForApp(COMMAND_UPDATE_RECORD, i, str == null ? getEFPath(i) : str, i2, 4, bArr.length, IccUtils.bytesToHexString(bArr), str2, this.mAid, message);
    }

    public void updateEFLinearFixed(int i, int i2, byte[] bArr, String str, Message message) {
        this.mCi.iccIOForApp(COMMAND_UPDATE_RECORD, i, getEFPath(i), i2, 4, bArr.length, IccUtils.bytesToHexString(bArr), str, this.mAid, message);
    }

    public void updateEFTransparent(int i, byte[] bArr, Message message) {
        this.mCi.iccIOForApp(214, i, getEFPath(i), 0, 0, bArr.length, IccUtils.bytesToHexString(bArr), null, this.mAid, message);
    }

    protected void sendResult(Message message, Object obj, Throwable th) {
        if (message == null) {
            return;
        }
        AsyncResult.forMessage(message, obj, th);
        message.sendToTarget();
    }

    protected boolean processException(Message message, AsyncResult asyncResult) {
        IccIoResult iccIoResult = (IccIoResult) asyncResult.result;
        if (asyncResult.exception != null) {
            sendResult(message, null, asyncResult.exception);
            return true;
        }
        IccException exception = iccIoResult.getException();
        if (exception != null) {
            sendResult(message, null, exception);
            return true;
        }
        return false;
    }

    @Override
    public void handleMessage(Message message) {
        Message message2;
        try {
            message2 = 7;
            try {
                switch (message.what) {
                    case 4:
                        AsyncResult asyncResult = (AsyncResult) message.obj;
                        Message message3 = (Message) asyncResult.userObj;
                        IccIoResult iccIoResult = (IccIoResult) asyncResult.result;
                        if (!processException(message3, (AsyncResult) message.obj)) {
                            byte[] bArr = iccIoResult.payload;
                            int i = message.arg1;
                            if (4 != bArr[6]) {
                                throw new IccFileTypeMismatch();
                            }
                            if (bArr[13] != 0) {
                                throw new IccFileTypeMismatch();
                            }
                            this.mCi.iccIOForApp(176, i, getEFPath(i), 0, 0, ((bArr[2] & 255) << 8) + (bArr[3] & 255), null, null, this.mAid, obtainMessage(5, i, 0, message3));
                        }
                        return;
                    case 5:
                    case 10:
                        AsyncResult asyncResult2 = (AsyncResult) message.obj;
                        Message message4 = (Message) asyncResult2.userObj;
                        IccIoResult iccIoResult2 = (IccIoResult) asyncResult2.result;
                        if (!processException(message4, (AsyncResult) message.obj)) {
                            sendResult(message4, iccIoResult2.payload, null);
                        }
                        return;
                    case 6:
                    case 11:
                        AsyncResult asyncResult3 = (AsyncResult) message.obj;
                        LoadLinearFixedContext loadLinearFixedContext = (LoadLinearFixedContext) asyncResult3.userObj;
                        IccIoResult iccIoResult3 = (IccIoResult) asyncResult3.result;
                        Message message5 = loadLinearFixedContext.mOnLoaded;
                        try {
                            if (processException(message5, (AsyncResult) message.obj)) {
                                loge("exception caught from EVENT_GET_RECORD_SIZE");
                            } else {
                                byte[] bArr2 = iccIoResult3.payload;
                                String eFPath = loadLinearFixedContext.mPath;
                                if (4 != bArr2[6]) {
                                    throw new IccFileTypeMismatch();
                                }
                                if (1 != bArr2[13]) {
                                    throw new IccFileTypeMismatch();
                                }
                                loadLinearFixedContext.mRecordSize = bArr2[14] & 255;
                                loadLinearFixedContext.mCountRecords = (((bArr2[2] & 255) << 8) + (bArr2[3] & 255)) / loadLinearFixedContext.mRecordSize;
                                if (loadLinearFixedContext.mLoadAll) {
                                    loadLinearFixedContext.results = new ArrayList<>(loadLinearFixedContext.mCountRecords);
                                }
                                if (eFPath == null) {
                                    eFPath = getEFPath(loadLinearFixedContext.mEfid);
                                }
                                this.mCi.iccIOForApp(178, loadLinearFixedContext.mEfid, eFPath, loadLinearFixedContext.mRecordNum, 4, loadLinearFixedContext.mRecordSize, null, null, this.mAid, obtainMessage(7, loadLinearFixedContext));
                            }
                            return;
                        } catch (Exception e) {
                            e = e;
                            message2 = message5;
                        }
                        break;
                    case 7:
                    case 9:
                        AsyncResult asyncResult4 = (AsyncResult) message.obj;
                        LoadLinearFixedContext loadLinearFixedContext2 = (LoadLinearFixedContext) asyncResult4.userObj;
                        IccIoResult iccIoResult4 = (IccIoResult) asyncResult4.result;
                        Message message6 = loadLinearFixedContext2.mOnLoaded;
                        try {
                            String eFPath2 = loadLinearFixedContext2.mPath;
                            if (!processException(message6, (AsyncResult) message.obj)) {
                                if (loadLinearFixedContext2.mLoadAll) {
                                    loadLinearFixedContext2.results.add(iccIoResult4.payload);
                                    loadLinearFixedContext2.mRecordNum++;
                                    if (loadLinearFixedContext2.mRecordNum > loadLinearFixedContext2.mCountRecords) {
                                        sendResult(message6, loadLinearFixedContext2.results, null);
                                    } else {
                                        if (eFPath2 == null) {
                                            eFPath2 = getEFPath(loadLinearFixedContext2.mEfid);
                                        }
                                        this.mCi.iccIOForApp(178, loadLinearFixedContext2.mEfid, eFPath2, loadLinearFixedContext2.mRecordNum, 4, loadLinearFixedContext2.mRecordSize, null, null, this.mAid, obtainMessage(7, loadLinearFixedContext2));
                                    }
                                } else {
                                    sendResult(message6, iccIoResult4.payload, null);
                                }
                            }
                            return;
                        } catch (Exception e2) {
                            e = e2;
                            message2 = message6;
                        }
                        break;
                    case 8:
                        AsyncResult asyncResult5 = (AsyncResult) message.obj;
                        LoadLinearFixedContext loadLinearFixedContext3 = (LoadLinearFixedContext) asyncResult5.userObj;
                        IccIoResult iccIoResult5 = (IccIoResult) asyncResult5.result;
                        Message message7 = loadLinearFixedContext3.mOnLoaded;
                        if (!processException(message7, (AsyncResult) message.obj)) {
                            byte[] bArr3 = iccIoResult5.payload;
                            if (4 != bArr3[6] || 1 != bArr3[13]) {
                                throw new IccFileTypeMismatch();
                            }
                            int[] iArr = new int[3];
                            iArr[0] = bArr3[14] & 255;
                            iArr[1] = ((bArr3[2] & 255) << 8) + (bArr3[3] & 255);
                            iArr[2] = iArr[1] / iArr[0];
                            sendResult(message7, iArr, null);
                        }
                        return;
                    default:
                        return;
                }
            } catch (Exception e3) {
                e = e3;
            }
        } catch (Exception e4) {
            e = e4;
            message2 = 0;
        }
        if (message2 != 0) {
            sendResult(message2, null, e);
            return;
        }
        loge("uncaught exception" + e);
    }

    protected String getCommonIccEFPath(int i) {
        if (i == 12037 || i == 12258) {
            return IccConstants.MF_SIM;
        }
        if (i == 20256) {
            return "3F007F105F50";
        }
        if (i == 20272) {
            return "3F007F105F3A";
        }
        if (i == 28480 || i == 28645) {
            return "3F007F10";
        }
        switch (i) {
            case 28474:
            case IccConstants.EF_FDN:
                return "3F007F10";
            default:
                switch (i) {
                    case IccConstants.EF_SDN:
                    case IccConstants.EF_EXT1:
                    case IccConstants.EF_EXT2:
                    case IccConstants.EF_EXT3:
                        return "3F007F10";
                    default:
                        return null;
                }
        }
    }

    public void loadEFLinearFixedAll(int i, Message message, boolean z) {
        sendResult(message, null, new IccException("Default loadEFLinearFixedAll exception."));
    }

    public void loadEFLinearFixedAll(int i, int i2, Message message) {
        sendResult(message, null, new IccException("Default loadEFLinearFixedAll exception."));
    }

    public void loadEFTransparent(int i, String str, Message message) {
        sendResult(message, null, new IccException("Default loadEFTransparent exception."));
    }

    public void updateEFTransparent(int i, String str, byte[] bArr, Message message) {
        sendResult(message, null, new IccException("Default updateEFTransparent exception."));
    }

    public void readEFLinearFixed(int i, int i2, int i3, Message message) {
        sendResult(message, null, new IccException("Default readEFLinearFixed exception."));
    }

    public void selectEFFile(int i, Message message) {
        sendResult(message, null, new IccException("Default selectEFFile exception."));
    }
}
