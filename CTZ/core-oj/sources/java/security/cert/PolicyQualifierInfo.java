package java.security.cert;

import java.io.IOException;
import sun.misc.HexDumpEncoder;
import sun.security.util.DerValue;

public class PolicyQualifierInfo {
    private byte[] mData;
    private byte[] mEncoded;
    private String mId;
    private String pqiString;

    public PolicyQualifierInfo(byte[] bArr) throws IOException {
        this.mEncoded = (byte[]) bArr.clone();
        DerValue derValue = new DerValue(this.mEncoded);
        if (derValue.tag != 48) {
            throw new IOException("Invalid encoding for PolicyQualifierInfo");
        }
        this.mId = derValue.data.getDerValue().getOID().toString();
        byte[] byteArray = derValue.data.toByteArray();
        if (byteArray == null) {
            this.mData = null;
        } else {
            this.mData = new byte[byteArray.length];
            System.arraycopy(byteArray, 0, this.mData, 0, byteArray.length);
        }
    }

    public final String getPolicyQualifierId() {
        return this.mId;
    }

    public final byte[] getEncoded() {
        return (byte[]) this.mEncoded.clone();
    }

    public final byte[] getPolicyQualifier() {
        if (this.mData == null) {
            return null;
        }
        return (byte[]) this.mData.clone();
    }

    public String toString() {
        if (this.pqiString != null) {
            return this.pqiString;
        }
        HexDumpEncoder hexDumpEncoder = new HexDumpEncoder();
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("PolicyQualifierInfo: [\n");
        stringBuffer.append("  qualifierID: " + this.mId + "\n");
        StringBuilder sb = new StringBuilder();
        sb.append("  qualifier: ");
        sb.append(this.mData == null ? "null" : hexDumpEncoder.encodeBuffer(this.mData));
        sb.append("\n");
        stringBuffer.append(sb.toString());
        stringBuffer.append("]");
        this.pqiString = stringBuffer.toString();
        return this.pqiString;
    }
}
