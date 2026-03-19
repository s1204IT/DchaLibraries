package com.mediatek.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccFileTypeMismatch;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.ArrayList;

public final class MtkIccFileHandler extends IccFileHandler {
    protected static final int EVENT_GET_BINARY_SIZE_DONE_EX = 101;
    protected static final int EVENT_GET_RECORD_SIZE_DONE_EX = 102;
    protected static final int EVENT_READ_RECORD_DONE_EX = 103;
    protected static final int EVENT_SELECT_EF_FILE = 100;
    static final String LOG_TAG = "MtkIccFileHandler";

    public MtkIccFileHandler(UiccCardApplication uiccCardApplication, String str, CommandsInterface commandsInterface) {
        super(uiccCardApplication, str, commandsInterface);
        logd("SelectFileHandlerEx constructor");
    }

    static class MtkLoadLinearFixedContext {
        int mCountRecords;
        int mEfid;
        Message mOnLoaded;
        int mRecordSize;
        ArrayList<byte[]> results;
        int mRecordNum = 1;
        boolean mLoadAll = true;
        String mPath = null;
        int mMode = -1;

        MtkLoadLinearFixedContext(int i, Message message) {
            this.mEfid = i;
            this.mOnLoaded = message;
        }
    }

    static class MtkLoadTransparentContext {
        int mEfid;
        Message mOnLoaded;
        String mPath;

        MtkLoadTransparentContext(int i, String str, Message message) {
            this.mEfid = i;
            this.mPath = str;
            this.mOnLoaded = message;
        }
    }

    public void handleMessage(Message message) throws IccFileTypeMismatch {
        Message message2;
        try {
            switch (message.what) {
                case 100:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    Message message3 = (Message) asyncResult.userObj;
                    try {
                        IccIoResult iccIoResult = (IccIoResult) asyncResult.result;
                        if (processException(message3, (AsyncResult) message.obj)) {
                            loge("EVENT_SELECT_EF_FILE exception");
                        } else {
                            byte[] bArr = iccIoResult.payload;
                            if (4 != bArr[6]) {
                                throw new IccFileTypeMismatch();
                            }
                            sendResult(message3, new EFResponseData(bArr), null);
                        }
                        return;
                    } catch (Exception e) {
                        e = e;
                        message2 = message3;
                    }
                    break;
                case 101:
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    MtkLoadTransparentContext mtkLoadTransparentContext = (MtkLoadTransparentContext) asyncResult2.userObj;
                    IccIoResult iccIoResult2 = (IccIoResult) asyncResult2.result;
                    Message message4 = mtkLoadTransparentContext.mOnLoaded;
                    try {
                        String eFPath = mtkLoadTransparentContext.mPath;
                        if (!processException(message4, (AsyncResult) message.obj)) {
                            byte[] bArr2 = iccIoResult2.payload;
                            if (4 != bArr2[6]) {
                                throw new IccFileTypeMismatch();
                            }
                            if (bArr2[13] != 0) {
                                throw new IccFileTypeMismatch();
                            }
                            int i = ((bArr2[2] & PplMessageManager.Type.INVALID) << 8) + (bArr2[3] & PplMessageManager.Type.INVALID);
                            if (eFPath == null) {
                                eFPath = getEFPath(mtkLoadTransparentContext.mEfid);
                            }
                            this.mCi.iccIOForApp(176, mtkLoadTransparentContext.mEfid, eFPath, 0, 0, i, (String) null, (String) null, this.mAid, obtainMessage(5, mtkLoadTransparentContext.mEfid, 0, message4));
                        }
                        return;
                    } catch (Exception e2) {
                        e = e2;
                        message2 = message4;
                    }
                    break;
                case 102:
                    AsyncResult asyncResult3 = (AsyncResult) message.obj;
                    MtkLoadLinearFixedContext mtkLoadLinearFixedContext = (MtkLoadLinearFixedContext) asyncResult3.userObj;
                    IccIoResult iccIoResult3 = (IccIoResult) asyncResult3.result;
                    message2 = mtkLoadLinearFixedContext.mOnLoaded;
                    try {
                        if (!processException(message2, (AsyncResult) message.obj)) {
                            byte[] bArr3 = iccIoResult3.payload;
                            String eFPath2 = mtkLoadLinearFixedContext.mPath;
                            if (4 != bArr3[6]) {
                                throw new IccFileTypeMismatch();
                            }
                            if (1 != bArr3[13]) {
                                throw new IccFileTypeMismatch();
                            }
                            mtkLoadLinearFixedContext.mRecordSize = bArr3[14] & PplMessageManager.Type.INVALID;
                            mtkLoadLinearFixedContext.mCountRecords = (((bArr3[2] & PplMessageManager.Type.INVALID) << 8) + (bArr3[3] & PplMessageManager.Type.INVALID)) / mtkLoadLinearFixedContext.mRecordSize;
                            if (mtkLoadLinearFixedContext.mLoadAll) {
                                mtkLoadLinearFixedContext.results = new ArrayList<>(mtkLoadLinearFixedContext.mCountRecords);
                            }
                            if (mtkLoadLinearFixedContext.mMode != -1) {
                                this.mCi.iccIOForApp(178, mtkLoadLinearFixedContext.mEfid, getSmsEFPath(mtkLoadLinearFixedContext.mMode), mtkLoadLinearFixedContext.mRecordNum, 4, mtkLoadLinearFixedContext.mRecordSize, (String) null, (String) null, this.mAid, obtainMessage(EVENT_READ_RECORD_DONE_EX, mtkLoadLinearFixedContext));
                            } else {
                                if (eFPath2 == null) {
                                    eFPath2 = getEFPath(mtkLoadLinearFixedContext.mEfid);
                                }
                                this.mCi.iccIOForApp(178, mtkLoadLinearFixedContext.mEfid, eFPath2, mtkLoadLinearFixedContext.mRecordNum, 4, mtkLoadLinearFixedContext.mRecordSize, (String) null, (String) null, this.mAid, obtainMessage(EVENT_READ_RECORD_DONE_EX, mtkLoadLinearFixedContext));
                            }
                        }
                        return;
                    } catch (Exception e3) {
                        e = e3;
                    }
                    break;
                case EVENT_READ_RECORD_DONE_EX:
                    AsyncResult asyncResult4 = (AsyncResult) message.obj;
                    MtkLoadLinearFixedContext mtkLoadLinearFixedContext2 = (MtkLoadLinearFixedContext) asyncResult4.userObj;
                    IccIoResult iccIoResult4 = (IccIoResult) asyncResult4.result;
                    Message message5 = mtkLoadLinearFixedContext2.mOnLoaded;
                    try {
                        String eFPath3 = mtkLoadLinearFixedContext2.mPath;
                        if (!processException(message5, (AsyncResult) message.obj)) {
                            if (mtkLoadLinearFixedContext2.mLoadAll) {
                                mtkLoadLinearFixedContext2.results.add(iccIoResult4.payload);
                                mtkLoadLinearFixedContext2.mRecordNum++;
                                if (mtkLoadLinearFixedContext2.mRecordNum > mtkLoadLinearFixedContext2.mCountRecords) {
                                    sendResult(message5, mtkLoadLinearFixedContext2.results, null);
                                } else if (mtkLoadLinearFixedContext2.mMode != -1) {
                                    this.mCi.iccIOForApp(178, mtkLoadLinearFixedContext2.mEfid, getSmsEFPath(mtkLoadLinearFixedContext2.mMode), mtkLoadLinearFixedContext2.mRecordNum, 4, mtkLoadLinearFixedContext2.mRecordSize, (String) null, (String) null, this.mAid, obtainMessage(EVENT_READ_RECORD_DONE_EX, mtkLoadLinearFixedContext2));
                                } else {
                                    if (eFPath3 == null) {
                                        eFPath3 = getEFPath(mtkLoadLinearFixedContext2.mEfid);
                                    }
                                    this.mCi.iccIOForApp(178, mtkLoadLinearFixedContext2.mEfid, eFPath3, mtkLoadLinearFixedContext2.mRecordNum, 4, mtkLoadLinearFixedContext2.mRecordSize, (String) null, (String) null, this.mAid, obtainMessage(EVENT_READ_RECORD_DONE_EX, mtkLoadLinearFixedContext2));
                                }
                            } else {
                                sendResult(message5, iccIoResult4.payload, null);
                            }
                        }
                        return;
                    } catch (Exception e4) {
                        e = e4;
                        message2 = message5;
                    }
                    break;
                default:
                    super.handleMessage(message);
                    return;
            }
        } catch (Exception e5) {
            e = e5;
            message2 = null;
        }
        if (message2 == null) {
            loge("uncaught exception" + e);
            return;
        }
        loge("caught exception:" + e);
        sendResult(message2, null, e);
    }

    public void loadEFLinearFixedAllByPath(int i, Message message, boolean z) {
        this.mCi.iccIOForApp(192, i, getEFPath(i), 0, 0, 15, (String) null, (String) null, this.mAid, obtainMessage(102, new MtkLoadLinearFixedContext(i, message)));
    }

    public void loadEFLinearFixedAllByMode(int i, int i2, Message message) {
        MtkLoadLinearFixedContext mtkLoadLinearFixedContext = new MtkLoadLinearFixedContext(i, message);
        mtkLoadLinearFixedContext.mMode = i2;
        this.mCi.iccIOForApp(192, i, getSmsEFPath(i2), 0, 0, 15, (String) null, (String) null, this.mAid, obtainMessage(102, mtkLoadLinearFixedContext));
    }

    protected String getSmsEFPath(int i) {
        if (i == 1) {
            return "3F007F10";
        }
        if (i != 2) {
            return "";
        }
        return "3F007F25";
    }

    public void loadEFTransparent(int i, String str, Message message) {
        if (str == null) {
            str = getEFPath(i);
        }
        String str2 = str;
        this.mCi.iccIOForApp(192, i, str2, 0, 0, 15, (String) null, (String) null, this.mAid, obtainMessage(101, new MtkLoadTransparentContext(i, str2, message)));
    }

    public void updateEFTransparent(int i, String str, byte[] bArr, Message message) {
        if (str == null) {
            str = getEFPath(i);
        }
        this.mCi.iccIOForApp(214, i, str, 0, 0, bArr.length, IccUtils.bytesToHexString(bArr), (String) null, this.mAid, message);
    }

    public void readEFLinearFixed(int i, int i2, int i3, Message message) {
        this.mCi.iccIOForApp(178, i, getEFPath(i), i2, 4, i3, (String) null, (String) null, this.mAid, message);
    }

    public void selectEFFile(int i, Message message) {
        this.mCi.iccIOForApp(192, i, getEFPath(i), 0, 0, 15, (String) null, (String) null, this.mAid, obtainMessage(100, i, 0, message));
    }

    protected String getEFPath(int i) {
        String commonIccEFPath = getCommonIccEFPath(i);
        if (commonIccEFPath == null) {
            Rlog.e(LOG_TAG, "Error: EF Path being returned in null");
        }
        return commonIccEFPath;
    }

    protected void logd(String str) {
        Rlog.i(LOG_TAG, str);
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }
}
