package com.android.internal.telephony;

import android.content.ContentValues;
import android.database.Cursor;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.HexDump;
import java.util.Arrays;
import java.util.Date;

public class InboundSmsTracker {
    public static final int DEST_PORT_FLAG_3GPP = 131072;

    @VisibleForTesting
    public static final int DEST_PORT_FLAG_3GPP2 = 262144;

    @VisibleForTesting
    public static final int DEST_PORT_FLAG_3GPP2_WAP_PDU = 524288;

    @VisibleForTesting
    public static final int DEST_PORT_FLAG_NO_PORT = 65536;
    private static final int DEST_PORT_MASK = 65535;

    @VisibleForTesting
    public static final String SELECT_BY_DUPLICATE_REFERENCE = "address=? AND reference_number=? AND count=? AND sequence=? AND ((date=? AND message_body=?) OR deleted=0) AND (destination_port & 524288=0)";

    @VisibleForTesting
    public static final String SELECT_BY_DUPLICATE_REFERENCE_3GPP2WAP = "address=? AND reference_number=? AND count=? AND sequence=? AND ((date=? AND message_body=?) OR deleted=0) AND (destination_port & 524288=524288)";

    @VisibleForTesting
    public static final String SELECT_BY_REFERENCE = "address=? AND reference_number=? AND count=? AND (destination_port & 524288=0) AND deleted=0";

    @VisibleForTesting
    public static final String SELECT_BY_REFERENCE_3GPP2WAP = "address=? AND reference_number=? AND count=? AND (destination_port & 524288=524288) AND deleted=0";
    protected final String mAddress;
    protected String mDeleteWhere;
    protected String[] mDeleteWhereArgs;
    protected final int mDestPort;
    private final String mDisplayAddress;
    private final boolean mIs3gpp2;
    protected final boolean mIs3gpp2WapPdu;
    private final String mMessageBody;
    protected final int mMessageCount;
    private final byte[] mPdu;
    protected final int mReferenceNumber;
    private final int mSequenceNumber;
    private final long mTimestamp;

    public InboundSmsTracker(byte[] bArr, long j, int i, boolean z, boolean z2, String str, String str2, String str3) {
        this.mPdu = bArr;
        this.mTimestamp = j;
        this.mDestPort = i;
        this.mIs3gpp2 = z;
        this.mIs3gpp2WapPdu = z2;
        this.mMessageBody = str3;
        this.mAddress = str;
        this.mDisplayAddress = str2;
        this.mReferenceNumber = -1;
        this.mSequenceNumber = getIndexOffset();
        this.mMessageCount = 1;
    }

    public InboundSmsTracker(byte[] bArr, long j, int i, boolean z, String str, String str2, int i2, int i3, int i4, boolean z2, String str3) {
        this.mPdu = bArr;
        this.mTimestamp = j;
        this.mDestPort = i;
        this.mIs3gpp2 = z;
        this.mIs3gpp2WapPdu = z2;
        this.mMessageBody = str3;
        this.mDisplayAddress = str2;
        this.mAddress = str;
        this.mReferenceNumber = i2;
        this.mSequenceNumber = i3;
        this.mMessageCount = i4;
    }

    public InboundSmsTracker(Cursor cursor, boolean z) {
        this.mPdu = HexDump.hexStringToByteArray(cursor.getString(0));
        if (cursor.isNull(2)) {
            this.mDestPort = -1;
            this.mIs3gpp2 = z;
            this.mIs3gpp2WapPdu = false;
        } else {
            int i = cursor.getInt(2);
            if ((131072 & i) != 0) {
                this.mIs3gpp2 = false;
            } else if ((262144 & i) != 0) {
                this.mIs3gpp2 = true;
            } else {
                this.mIs3gpp2 = z;
            }
            this.mIs3gpp2WapPdu = (524288 & i) != 0;
            this.mDestPort = getRealDestPort(i);
        }
        this.mTimestamp = cursor.getLong(3);
        this.mAddress = cursor.getString(6);
        this.mDisplayAddress = cursor.getString(9);
        if (cursor.getInt(5) == 1) {
            long j = cursor.getLong(7);
            this.mReferenceNumber = -1;
            this.mSequenceNumber = getIndexOffset();
            this.mMessageCount = 1;
            this.mDeleteWhere = InboundSmsHandler.SELECT_BY_ID;
            this.mDeleteWhereArgs = new String[]{Long.toString(j)};
        } else {
            this.mReferenceNumber = cursor.getInt(4);
            this.mMessageCount = cursor.getInt(5);
            this.mSequenceNumber = cursor.getInt(1);
            int indexOffset = this.mSequenceNumber - getIndexOffset();
            if (indexOffset < 0 || indexOffset >= this.mMessageCount) {
                throw new IllegalArgumentException("invalid PDU sequence " + this.mSequenceNumber + " of " + this.mMessageCount);
            }
            this.mDeleteWhere = getQueryForSegments();
            this.mDeleteWhereArgs = new String[]{this.mAddress, Integer.toString(this.mReferenceNumber), Integer.toString(this.mMessageCount)};
        }
        this.mMessageBody = cursor.getString(8);
    }

    public ContentValues getContentValues() {
        int i;
        int i2;
        ContentValues contentValues = new ContentValues();
        contentValues.put("pdu", HexDump.toHexString(this.mPdu));
        contentValues.put("date", Long.valueOf(this.mTimestamp));
        if (this.mDestPort == -1) {
            i = 65536;
        } else {
            i = this.mDestPort & 65535;
        }
        if (this.mIs3gpp2) {
            i2 = i | DEST_PORT_FLAG_3GPP2;
        } else {
            i2 = i | 131072;
        }
        if (this.mIs3gpp2WapPdu) {
            i2 |= 524288;
        }
        contentValues.put("destination_port", Integer.valueOf(i2));
        if (this.mAddress != null) {
            contentValues.put("address", this.mAddress);
            contentValues.put("display_originating_addr", this.mDisplayAddress);
            contentValues.put("reference_number", Integer.valueOf(this.mReferenceNumber));
            contentValues.put("sequence", Integer.valueOf(this.mSequenceNumber));
        }
        contentValues.put("count", Integer.valueOf(this.mMessageCount));
        contentValues.put("message_body", this.mMessageBody);
        return contentValues;
    }

    public static int getRealDestPort(int i) {
        if ((65536 & i) != 0) {
            return -1;
        }
        return i & 65535;
    }

    public void setDeleteWhere(String str, String[] strArr) {
        this.mDeleteWhere = str;
        this.mDeleteWhereArgs = strArr;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("SmsTracker{timestamp=");
        sb.append(new Date(this.mTimestamp));
        sb.append(" destPort=");
        sb.append(this.mDestPort);
        sb.append(" is3gpp2=");
        sb.append(this.mIs3gpp2);
        if (this.mAddress != null) {
            sb.append(" address=");
            sb.append(this.mAddress);
            sb.append(" display_originating_addr=");
            sb.append(this.mDisplayAddress);
            sb.append(" refNumber=");
            sb.append(this.mReferenceNumber);
            sb.append(" seqNumber=");
            sb.append(this.mSequenceNumber);
            sb.append(" msgCount=");
            sb.append(this.mMessageCount);
        }
        if (this.mDeleteWhere != null) {
            sb.append(" deleteWhere(");
            sb.append(this.mDeleteWhere);
            sb.append(") deleteArgs=(");
            sb.append(Arrays.toString(this.mDeleteWhereArgs));
            sb.append(')');
        }
        sb.append('}');
        return sb.toString();
    }

    public byte[] getPdu() {
        return this.mPdu;
    }

    public long getTimestamp() {
        return this.mTimestamp;
    }

    public int getDestPort() {
        return this.mDestPort;
    }

    public boolean is3gpp2() {
        return this.mIs3gpp2;
    }

    public String getFormat() {
        return this.mIs3gpp2 ? "3gpp2" : "3gpp";
    }

    public String getQueryForSegments() {
        return this.mIs3gpp2WapPdu ? SELECT_BY_REFERENCE_3GPP2WAP : SELECT_BY_REFERENCE;
    }

    public String getQueryForMultiPartDuplicates() {
        return this.mIs3gpp2WapPdu ? SELECT_BY_DUPLICATE_REFERENCE_3GPP2WAP : SELECT_BY_DUPLICATE_REFERENCE;
    }

    public int getIndexOffset() {
        return (this.mIs3gpp2 && this.mIs3gpp2WapPdu) ? 0 : 1;
    }

    public String getAddress() {
        return this.mAddress;
    }

    public String getDisplayAddress() {
        return this.mDisplayAddress;
    }

    public String getMessageBody() {
        return this.mMessageBody;
    }

    public int getReferenceNumber() {
        return this.mReferenceNumber;
    }

    public int getSequenceNumber() {
        return this.mSequenceNumber;
    }

    public int getMessageCount() {
        return this.mMessageCount;
    }

    public String getDeleteWhere() {
        return this.mDeleteWhere;
    }

    public String[] getDeleteWhereArgs() {
        return this.mDeleteWhereArgs;
    }
}
