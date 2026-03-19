package com.mediatek.android.mms.pdu;

import com.google.android.mms.pdu.PduPart;

public class MtkPduPart extends PduPart {
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String CONTENT_ID = "Content-ID";
    public static final String CONTENT_LOCATION = "Content-Location";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String PARA_NAME = "Name";
    public static final int P_DATE = 146;
    public static final int P_TRANSFER_ENCODING = 167;
    public static final int P_X_WAP_CONTENT_URI = 176;
    private boolean mNeedUpdate = true;

    public byte[] getXWapContentUri() {
        return (byte[]) this.mPartHeader.get(176);
    }

    public void setXWapContentUri(byte[] bArr) {
        if (bArr == null) {
            throw new NullPointerException("null content-id");
        }
        this.mPartHeader.put(176, bArr);
    }

    public long getDate() {
        return ((Long) this.mPartHeader.get(Integer.valueOf(P_DATE))).longValue();
    }

    public void setDate(long j) {
        this.mPartHeader.put(Integer.valueOf(P_DATE), Long.valueOf(j));
    }

    public boolean needUpdate() {
        return this.mNeedUpdate;
    }

    public void setNeedUpdate(boolean z) {
        this.mNeedUpdate = z;
    }
}
