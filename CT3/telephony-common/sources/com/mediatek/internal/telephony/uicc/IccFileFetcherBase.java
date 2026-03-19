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
    protected HashMap<String, Object> mData = new HashMap<>();
    protected IccFileHandler mFh;
    protected Phone mPhone;
    protected int mPhoneId;
    protected UiccController mUiccController;

    public abstract IccFileRequest onGetFilePara(String str);

    public abstract ArrayList<String> onGetKeys();

    public abstract void onParseResult(String str, byte[] bArr, ArrayList<byte[]> arrayList);

    protected IccFileFetcherBase(Context c, Phone phone) {
        log("IccFileFetcherBase Creating!");
        this.mPhone = phone;
        this.mPhoneId = this.mPhone.getPhoneId();
        this.mContext = c;
    }

    public void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
            int phoneId = intent.getIntExtra("phone", -1);
            if (this.mPhoneId != phoneId || this.mPhone.getPhoneType() != 2) {
                return;
            }
            String simStatus = intent.getStringExtra("ss");
            if (!"LOADED".equals(simStatus)) {
                return;
            }
            new Thread() {
                @Override
                public void run() {
                    IccFileFetcherBase.this.exchangeSimInfo();
                }
            }.start();
            return;
        }
        if (!"android.intent.action.RADIO_TECHNOLOGY".equals(action) || this.mData == null) {
            return;
        }
        this.mData.clear();
        log("IccFileFetcherBase hashmap is cleared!");
    }

    protected void exchangeSimInfo() {
        this.mUiccController = UiccController.getInstance();
        ArrayList<String> mKey = onGetKeys();
        Iterator<String> it = mKey.iterator();
        while (it.hasNext()) {
            String key = it.next();
            IccFileRequest mRq = onGetFilePara(key);
            if (mRq == null) {
                loge("exchangeSimInfo mPhoneId:" + this.mPhoneId + "  key: " + it + "  get Para failed!");
                return;
            }
            log("exchangeSimInfo key:" + key + " mEfid:" + mRq.mEfid + " mEfType:" + mRq.mEfType + " mAppType :" + mRq.mAppType + " mEfPath:" + mRq.mEfPath + " mData:" + mRq.mData + " mRecordNum:" + mRq.mRecordNum + " mPin2:" + mRq.mPin2);
            if (mRq.mAppType == 0) {
                this.mFh = this.mPhone.getIccCard() == null ? null : this.mPhone.getIccCard().getIccFileHandler();
            } else {
                this.mFh = this.mUiccController.getIccFileHandler(this.mPhoneId, mRq.mAppType);
            }
            if (this.mFh != null) {
                mRq.mKey = key;
                if (UsimPBMemInfo.STRING_NOT_SET.equals(mRq.mEfPath) || mRq.mEfPath == null) {
                    log("exchangeSimInfo path is null, it may get an invalid reponse!");
                }
                if (mRq.mData == null) {
                    loadSimInfo(mRq);
                } else {
                    updateSimInfo(mRq);
                }
            } else {
                log("exchangeSimInfo mFh[" + this.mPhoneId + "] is null, read failed!");
            }
        }
    }

    protected void loadSimInfo(IccFileRequest req) {
        if (req.mEfType == 0) {
            this.mFh.loadEFLinearFixedAll(req.mEfid, req.mEfPath, obtainMessage(1, req));
        } else if (req.mEfType == 1) {
            this.mFh.loadEFTransparent(req.mEfid, req.mEfPath, obtainMessage(2, req));
        } else {
            loge("loadSimInfo req.mEfType = " + req.mEfType + " is invalid!");
        }
    }

    protected void updateSimInfo(IccFileRequest req) {
        if (req.mEfType == 0) {
            this.mFh.updateEFLinearFixed(req.mEfid, req.mEfPath, req.mRecordNum, req.mData, req.mPin2, obtainMessage(3, req));
        } else if (req.mEfType == 1) {
            this.mFh.updateEFTransparent(req.mEfid, req.mEfPath, req.mData, obtainMessage(4, req));
        } else {
            loge("updateSimInfo req.mEfType = " + req.mEfType + " is invalid!");
        }
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            switch (msg.what) {
                case 0:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        log("EVENT_GET_LINEARFIXED_RECORD_SIZE_DONE Exception: " + ar.exception);
                    }
                    break;
                case 1:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    IccFileRequest sr = (IccFileRequest) ar2.userObj;
                    if (ar2.exception != null) {
                        loge("EVENT_LOAD_LINEARFIXED_ALL_DONE Exception: " + ar2.exception);
                        onParseResult(sr.mKey, null, null);
                    } else {
                        ArrayList<byte[]> datas = (ArrayList) ar2.result;
                        log("EVENT_LOAD_LINEARFIXED_ALL_DONE key: " + sr.mKey + "  datas: " + datas);
                        onParseResult(sr.mKey, null, datas);
                    }
                    break;
                case 2:
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    IccFileRequest sr2 = (IccFileRequest) ar3.userObj;
                    if (ar3.exception != null) {
                        loge("EVENT_LOAD_TRANSPARENT_DONE Exception: " + ar3.exception);
                        onParseResult(sr2.mKey, null, null);
                    } else {
                        byte[] data = (byte[]) ar3.result;
                        log("EVENT_LOAD_TRANSPARENT_DONE key: " + sr2.mKey + "  data: " + data);
                        onParseResult(sr2.mKey, data, null);
                    }
                    break;
                case 3:
                    AsyncResult ar4 = (AsyncResult) msg.obj;
                    if (ar4.exception != null) {
                        loge("EVENT_UPDATE_LINEARFIXED_DONE Exception: " + ar4.exception);
                    } else {
                        IccFileRequest sr3 = (IccFileRequest) ar4.userObj;
                        log("EVENT_UPDATE_LINEARFIXED_DONE key: " + sr3.mKey + "  data: " + sr3.mData);
                    }
                    break;
                case 4:
                    AsyncResult ar5 = (AsyncResult) msg.obj;
                    if (ar5.exception != null) {
                        loge("EVENT_UPDATE_TRANSPARENT_DONE Exception: " + ar5.exception);
                    } else {
                        IccFileRequest sr4 = (IccFileRequest) ar5.userObj;
                        log("EVENT_UPDATE_TRANSPARENT_DONE key: " + sr4.mKey + "  data: " + sr4.mData);
                    }
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        } catch (IllegalArgumentException exc) {
            loge("Exception parsing file record" + exc);
        }
    }

    protected void log(String msg) {
        Rlog.d(TAG, msg + " (phoneId " + this.mPhoneId + ")");
    }

    protected void loge(String msg) {
        Rlog.e(TAG, msg + " (phoneId " + this.mPhoneId + ")");
    }
}
