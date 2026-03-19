package com.mediatek.internal.telephony.uicc;

import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UsimFileHandler;

public final class MtkUsimFileHandler extends UsimFileHandler {
    static final String LOG_TAG_EX = "MtkUsimFH";
    MtkIccFileHandler mMtkIccFh;

    public MtkUsimFileHandler(UiccCardApplication uiccCardApplication, String str, CommandsInterface commandsInterface) {
        super(uiccCardApplication, str, commandsInterface);
        this.mMtkIccFh = null;
        this.mMtkIccFh = new MtkIccFileHandler(uiccCardApplication, str, commandsInterface);
    }

    protected String getEFPath(int i) {
        if (i == 20278) {
            return "7FFF7F665F30";
        }
        if (i == 28433) {
            return "3F007F20";
        }
        if (i == 28482 || i == 28489 || i == 28599) {
            return "3F007FFF";
        }
        if (i != 28645) {
            switch (i) {
                case 28435:
                case 28436:
                case 28437:
                case 28438:
                case 28439:
                case 28440:
                    return "3F007F20";
                default:
                    Rlog.d(LOG_TAG_EX, "Usim aosp default getEFPath.");
                    return super.getEFPath(i);
            }
        }
        return "3F007F10";
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
