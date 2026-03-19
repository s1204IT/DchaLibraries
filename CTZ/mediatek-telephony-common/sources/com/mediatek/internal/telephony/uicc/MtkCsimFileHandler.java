package com.mediatek.internal.telephony.uicc;

import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.CsimFileHandler;
import com.android.internal.telephony.uicc.UiccCardApplication;

public final class MtkCsimFileHandler extends CsimFileHandler implements MtkIccConstants {
    static final String LOG_TAG = "MtkCsimFH";
    MtkIccFileHandler mMtkIccFh;

    public MtkCsimFileHandler(UiccCardApplication uiccCardApplication, String str, CommandsInterface commandsInterface) {
        super(uiccCardApplication, str, commandsInterface);
        this.mMtkIccFh = null;
        this.mMtkIccFh = new MtkIccFileHandler(uiccCardApplication, str, commandsInterface);
    }

    protected String getEFPath(int i) {
        logd("GetEFPath : " + i);
        if (i == 28533) {
            return "3F007FFF";
        }
        return super.getEFPath(i);
    }

    protected String getCommonIccEFPath(int i) {
        logd("getCommonIccEFPath : " + i);
        if (i == 28645) {
            return null;
        }
        return super.getCommonIccEFPath(i);
    }

    public void loadEFLinearFixedAll(int i, Message message, boolean z) {
        this.mMtkIccFh.loadEFLinearFixedAllByPath(i, message, z);
    }

    public void loadEFLinearFixedAll(int i, int i2, Message message) {
        this.mMtkIccFh.loadEFLinearFixedAllByMode(i, i2, message);
    }

    public void loadEFTransparent(int i, String str, Message message) {
        this.mMtkIccFh.loadEFTransparent(i, str, message);
    }

    public void updateEFTransparent(int i, String str, byte[] bArr, Message message) {
        this.mMtkIccFh.updateEFTransparent(i, str, bArr, message);
    }

    public void readEFLinearFixed(int i, int i2, int i3, Message message) {
        this.mMtkIccFh.readEFLinearFixed(i, i2, i3, message);
    }

    public void selectEFFile(int i, Message message) {
        this.mMtkIccFh.selectEFFile(i, message);
    }

    protected void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }
}
