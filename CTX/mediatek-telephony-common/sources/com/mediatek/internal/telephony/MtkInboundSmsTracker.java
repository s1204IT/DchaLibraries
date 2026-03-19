package com.mediatek.internal.telephony;

import android.content.ContentValues;
import android.database.Cursor;
import com.android.internal.telephony.InboundSmsTracker;

public class MtkInboundSmsTracker extends InboundSmsTracker {
    public static final int SUB_ID_COLUMN = 10;
    private long mRecvTime;
    private int mSubId;
    private int mUploadFlag;

    public MtkInboundSmsTracker(byte[] bArr, long j, int i, boolean z, boolean z2, String str, String str2, String str3) {
        super(bArr, j, i, z, z2, str, str2, str3);
    }

    public MtkInboundSmsTracker(byte[] bArr, long j, int i, boolean z, String str, String str2, int i2, int i3, int i4, boolean z2, String str3) {
        super(bArr, j, i, z, str, str2, i2, i3, i4, z2, str3);
    }

    public MtkInboundSmsTracker(Cursor cursor, boolean z) {
        super(cursor, z);
        this.mSubId = cursor.getInt(10);
        if (!cursor.isNull(5)) {
            StringBuilder sb = new StringBuilder();
            sb.append("address=? AND reference_number=? AND count=? AND deleted=0 AND sub_id=?");
            sb.append(z ? IMtkConcatenatedSmsFwk.SQL_3GPP2_SMS : IMtkConcatenatedSmsFwk.SQL_3GPP_SMS);
            this.mDeleteWhere = sb.toString();
            this.mDeleteWhereArgs = new String[]{this.mAddress, Integer.toString(this.mReferenceNumber), Integer.toString(this.mMessageCount), Integer.toString(this.mSubId)};
        }
    }

    public ContentValues getContentValues() {
        ContentValues contentValues = super.getContentValues();
        contentValues.put("sub_id", Integer.valueOf(this.mSubId));
        return contentValues;
    }

    public boolean is3gpp2WapPdu() {
        return this.mIs3gpp2WapPdu;
    }

    public int getSubId() {
        return this.mSubId;
    }

    public void setSubId(int i) {
        this.mSubId = i;
    }

    public int getDestPort() {
        return this.mDestPort;
    }
}
