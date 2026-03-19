package com.android.internal.telephony.uicc.euicc.apdu;

import java.util.ArrayList;
import java.util.List;

public class RequestBuilder {
    private static final int CLA_STORE_DATA = 128;
    private static final int INS_STORE_DATA = 226;
    private static final int MAX_APDU_DATA_LEN = 255;
    private static final int MAX_EXT_APDU_DATA_LEN = 65535;
    private static final int P1_STORE_DATA_END = 145;
    private static final int P1_STORE_DATA_INTERM = 17;
    private final int mChannel;
    private final List<ApduCommand> mCommands = new ArrayList();
    private final int mMaxApduDataLen;

    public void addApdu(int i, int i2, int i3, int i4, int i5, String str) {
        this.mCommands.add(new ApduCommand(this.mChannel, i, i2, i3, i4, i5, str));
    }

    public void addApdu(int i, int i2, int i3, int i4, String str) {
        this.mCommands.add(new ApduCommand(this.mChannel, i, i2, i3, i4, str.length() / 2, str));
    }

    public void addApdu(int i, int i2, int i3, int i4) {
        this.mCommands.add(new ApduCommand(this.mChannel, i, i2, i3, i4, 0, ""));
    }

    public void addStoreData(String str) {
        int i;
        int i2 = this.mMaxApduDataLen * 2;
        int length = str.length() / 2;
        if (length != 0) {
            i = ((length + this.mMaxApduDataLen) - 1) / this.mMaxApduDataLen;
        } else {
            i = 1;
        }
        int i3 = 0;
        int i4 = 1;
        while (i4 < i) {
            int i5 = i3 + i2;
            addApdu(128, 226, 17, i4 - 1, str.substring(i3, i5));
            i4++;
            i3 = i5;
        }
        addApdu(128, 226, 145, i - 1, str.substring(i3));
    }

    List<ApduCommand> getCommands() {
        return this.mCommands;
    }

    RequestBuilder(int i, boolean z) {
        this.mChannel = i;
        this.mMaxApduDataLen = z ? 65535 : 255;
    }
}
