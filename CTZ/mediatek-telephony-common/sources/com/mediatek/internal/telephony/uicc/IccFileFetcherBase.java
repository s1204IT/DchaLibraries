package com.mediatek.internal.telephony.uicc;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.MtkIccUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public abstract class IccFileFetcherBase extends Handler {
    protected static final int APP_TYPE_3GPP = 1;
    protected static final int APP_TYPE_3GPP2 = 2;
    protected static final int APP_TYPE_ACTIVE = 0;
    protected static final int APP_TYPE_IMS = 3;
    protected static final int EF_TYPE_LINEARFIXED = 0;
    protected static final int EF_TYPE_TRANSPARENT = 1;
    protected static final int EVENT_GET_LINEARFIXED_RECORD_SIZE_DONE = 0;
    protected static final int EVENT_LOAD_LINEARFIXED_ALL_DONE = 1;
    protected static final int EVENT_LOAD_TRANSPARENT_DONE = 2;
    protected static final int EVENT_UPDATE_LINEARFIXED_DONE = 3;
    protected static final int EVENT_UPDATE_TRANSPARENT_DONE = 4;
    protected static final int INVALID_INDEX = -1;
    private static final String TAG = "IccFileFetcherBase";
    protected Context mContext;
    protected Phone mPhone;
    protected int mPhoneId;
    protected UiccController mUiccController;
    protected IccFileHandler mFh = null;
    protected HashMap<String, Object> mData = new HashMap<>();

    public abstract IccFileRequest onGetFilePara(String str);

    public abstract ArrayList<String> onGetKeys();

    public abstract void onParseResult(String str, byte[] bArr, ArrayList<byte[]> arrayList);

    protected IccFileFetcherBase(Context context, Phone phone) {
        log("IccFileFetcherBase Creating!");
        this.mPhone = phone;
        this.mPhoneId = this.mPhone.getPhoneId();
        this.mContext = context;
    }

    public void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
            if (this.mPhoneId == intent.getIntExtra("phone", -1) && this.mPhone.getPhoneType() == 2 && "LOADED".equals(intent.getStringExtra("ss"))) {
                new Thread() {
                    @Override
                    public void run() {
                        IccFileFetcherBase.this.exchangeSimInfo();
                    }
                }.start();
                return;
            }
            return;
        }
        if ("android.intent.action.RADIO_TECHNOLOGY".equals(action) && this.mData != null) {
            this.mData.clear();
            log("IccFileFetcherBase hashmap is cleared!");
        }
    }

    protected void exchangeSimInfo() {
        this.mUiccController = UiccController.getInstance();
        Iterator<String> it = onGetKeys().iterator();
        while (it.hasNext()) {
            String next = it.next();
            IccFileRequest iccFileRequestOnGetFilePara = onGetFilePara(next);
            if (iccFileRequestOnGetFilePara == null) {
                loge("exchangeSimInfo mPhoneId:" + this.mPhoneId + "  key: " + it + "  get Para failed!");
                return;
            }
            log("exchangeSimInfo key:" + next + " mEfid:" + iccFileRequestOnGetFilePara.mEfid + " mEfType:" + iccFileRequestOnGetFilePara.mEfType + " mAppType :" + iccFileRequestOnGetFilePara.mAppType + " mEfPath:" + iccFileRequestOnGetFilePara.mEfPath + " mData:" + MtkIccUtils.bytesToHexString(iccFileRequestOnGetFilePara.mData) + " mRecordNum:" + iccFileRequestOnGetFilePara.mRecordNum + " mPin2:" + iccFileRequestOnGetFilePara.mPin2);
            if (iccFileRequestOnGetFilePara.mAppType == 0) {
                this.mFh = this.mPhone.getIccFileHandler();
            } else {
                this.mFh = this.mUiccController.getIccFileHandler(this.mPhoneId, iccFileRequestOnGetFilePara.mAppType);
            }
            if (this.mFh != null) {
                iccFileRequestOnGetFilePara.mKey = next;
                if ("".equals(iccFileRequestOnGetFilePara.mEfPath) || iccFileRequestOnGetFilePara.mEfPath == null) {
                    log("exchangeSimInfo path is null, it may get an invalid reponse!");
                }
                if (iccFileRequestOnGetFilePara.mData == null) {
                    loadSimInfo(iccFileRequestOnGetFilePara);
                } else {
                    updateSimInfo(iccFileRequestOnGetFilePara);
                }
            } else {
                log("exchangeSimInfo mFh[" + this.mPhoneId + "] is null, read failed!");
            }
        }
    }

    protected void loadSimInfo(IccFileRequest iccFileRequest) {
        if (iccFileRequest.mEfType == 0) {
            this.mFh.loadEFLinearFixedAll(iccFileRequest.mEfid, iccFileRequest.mEfPath, obtainMessage(1, iccFileRequest));
            return;
        }
        if (iccFileRequest.mEfType == 1) {
            this.mFh.loadEFTransparent(iccFileRequest.mEfid, iccFileRequest.mEfPath, obtainMessage(2, iccFileRequest));
            return;
        }
        loge("loadSimInfo req.mEfType = " + iccFileRequest.mEfType + " is invalid!");
    }

    protected void updateSimInfo(IccFileRequest iccFileRequest) {
        if (this.mFh == null) {
            log("updateSimInfo mFh[" + this.mPhoneId + "] is null, updateSimInfo failed!");
            return;
        }
        if (iccFileRequest.mEfType == 0) {
            this.mFh.updateEFLinearFixed(iccFileRequest.mEfid, iccFileRequest.mEfPath, iccFileRequest.mRecordNum, iccFileRequest.mData, iccFileRequest.mPin2, obtainMessage(3, iccFileRequest));
            return;
        }
        if (iccFileRequest.mEfType == 1) {
            this.mFh.updateEFTransparent(iccFileRequest.mEfid, iccFileRequest.mEfPath, iccFileRequest.mData, obtainMessage(4, iccFileRequest));
            return;
        }
        loge("updateSimInfo req.mEfType = " + iccFileRequest.mEfType + " is invalid!");
    }

    @Override
    public void handleMessage(Message message) {
        try {
            switch (message.what) {
                case 0:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception != null) {
                        log("EVENT_GET_LINEARFIXED_RECORD_SIZE_DONE Exception: " + asyncResult.exception);
                    }
                    break;
                case 1:
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    IccFileRequest iccFileRequest = (IccFileRequest) asyncResult2.userObj;
                    if (asyncResult2.exception != null) {
                        loge("EVENT_LOAD_LINEARFIXED_ALL_DONE Exception: " + asyncResult2.exception);
                        onParseResult(iccFileRequest.mKey, null, null);
                    } else {
                        ArrayList<byte[]> arrayList = (ArrayList) asyncResult2.result;
                        log("EVENT_LOAD_LINEARFIXED_ALL_DONE key: " + iccFileRequest.mKey + "  datas: " + arrayList);
                        onParseResult(iccFileRequest.mKey, null, arrayList);
                    }
                    break;
                case 2:
                    AsyncResult asyncResult3 = (AsyncResult) message.obj;
                    IccFileRequest iccFileRequest2 = (IccFileRequest) asyncResult3.userObj;
                    if (asyncResult3.exception != null) {
                        loge("EVENT_LOAD_TRANSPARENT_DONE Exception: " + asyncResult3.exception);
                        onParseResult(iccFileRequest2.mKey, null, null);
                    } else {
                        byte[] bArr = (byte[]) asyncResult3.result;
                        log("EVENT_LOAD_TRANSPARENT_DONE key: " + iccFileRequest2.mKey + "  data: " + MtkIccUtils.bytesToHexString(bArr));
                        onParseResult(iccFileRequest2.mKey, bArr, null);
                    }
                    break;
                case 3:
                    AsyncResult asyncResult4 = (AsyncResult) message.obj;
                    if (asyncResult4.exception != null) {
                        loge("EVENT_UPDATE_LINEARFIXED_DONE Exception: " + asyncResult4.exception);
                    } else {
                        IccFileRequest iccFileRequest3 = (IccFileRequest) asyncResult4.userObj;
                        log("EVENT_UPDATE_LINEARFIXED_DONE key: " + iccFileRequest3.mKey + "  data: " + MtkIccUtils.bytesToHexString(iccFileRequest3.mData));
                    }
                    break;
                case 4:
                    AsyncResult asyncResult5 = (AsyncResult) message.obj;
                    if (asyncResult5.exception != null) {
                        loge("EVENT_UPDATE_TRANSPARENT_DONE Exception: " + asyncResult5.exception);
                    } else {
                        IccFileRequest iccFileRequest4 = (IccFileRequest) asyncResult5.userObj;
                        log("EVENT_UPDATE_TRANSPARENT_DONE key: " + iccFileRequest4.mKey + "  data: " + MtkIccUtils.bytesToHexString(iccFileRequest4.mData));
                    }
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        } catch (IllegalArgumentException e) {
            loge("Exception parsing file record" + e);
        }
    }

    protected void log(String str) {
        Rlog.d(TAG, str + " (phoneId " + this.mPhoneId + ")");
    }

    protected void loge(String str) {
        Rlog.e(TAG, str + " (phoneId " + this.mPhoneId + ")");
    }
}
