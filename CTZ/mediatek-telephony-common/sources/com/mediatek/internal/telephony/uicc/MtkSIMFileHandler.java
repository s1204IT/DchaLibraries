package com.mediatek.internal.telephony.uicc;

import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.SIMFileHandler;
import com.android.internal.telephony.uicc.UiccCardApplication;

public final class MtkSIMFileHandler extends SIMFileHandler {
    static final String LOG_TAG_EX = "MtkSIMFH";
    MtkIccFileHandler mMtkIccFh;

    public MtkSIMFileHandler(UiccCardApplication uiccCardApplication, String str, CommandsInterface commandsInterface) {
        super(uiccCardApplication, str, commandsInterface);
        this.mMtkIccFh = null;
        this.mMtkIccFh = new MtkIccFileHandler(uiccCardApplication, str, commandsInterface);
    }

    protected String getEFPath(int i) {
        if (i == 20278) {
            return "7FFF7F665F30";
        }
        if (i == 28450) {
            return "3F007F25";
        }
        if (i == 28482) {
            return "3F007F10";
        }
        if (i == 28599 || i == 28614) {
            return "3F007F20";
        }
        Rlog.d(LOG_TAG_EX, "SIM aosp default getEFPath.");
        return super.getEFPath(i);
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
}
