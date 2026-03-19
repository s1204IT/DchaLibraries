package com.android.se.security.arf;

import android.util.Log;
import com.android.se.security.arf.pkcs15.PKCS15Exception;
import java.util.Arrays;

public class DERParser {
    private byte[] mDERBuffer;
    private short mDERIndex;
    private short mDERSize;
    private short mTLVDataSize;
    public final String mTag = "SecureElement-DERParser";

    public DERParser(byte[] bArr) throws PKCS15Exception {
        this.mTLVDataSize = (short) 0;
        this.mDERBuffer = bArr;
        this.mDERIndex = (short) 0;
        this.mDERSize = (short) 0;
        if (this.mDERBuffer == null) {
            return;
        }
        this.mDERSize = (short) this.mDERBuffer.length;
        this.mTLVDataSize = this.mDERSize;
        if (this.mDERSize != 0 && this.mDERBuffer[this.mDERIndex] == -1) {
            this.mTLVDataSize = (short) 0;
            do {
                short s = (short) (this.mDERIndex + 1);
                this.mDERIndex = s;
                if (s >= this.mDERSize) {
                    return;
                }
            } while (this.mDERBuffer[this.mDERIndex] == -1);
            throw new PKCS15Exception("[Parser] Incorrect file format");
        }
    }

    private int readIntBase128() {
        byte[] bArr;
        short s;
        int i = 0;
        do {
            i = (i << 7) + (this.mDERBuffer[this.mDERIndex] & 127);
            bArr = this.mDERBuffer;
            s = this.mDERIndex;
            this.mDERIndex = (short) (s + 1);
        } while ((bArr[s] & ASN1.TAG_ContextSpecPrim0) != 0);
        return i;
    }

    private short getTLVSize() throws PKCS15Exception {
        if (isEndofBuffer()) {
            throw new PKCS15Exception("[Parser] Cannot retreive size");
        }
        byte[] bArr = this.mDERBuffer;
        short s = this.mDERIndex;
        this.mDERIndex = (short) (s + 1);
        int i = bArr[s] & 255;
        if (i >= 128) {
            int i2 = 0;
            for (int i3 = i - 128; i3 > 0; i3--) {
                if (!isEndofBuffer()) {
                    byte[] bArr2 = this.mDERBuffer;
                    short s2 = this.mDERIndex;
                    this.mDERIndex = (short) (s2 + 1);
                    i2 = (i2 << 8) + (bArr2[s2] & 255);
                } else {
                    throw new PKCS15Exception("[Parser] Cannot retreive size");
                }
            }
            i = i2;
        }
        if (this.mDERIndex + i > this.mDERSize) {
            throw new PKCS15Exception("[Parser] Not enough data");
        }
        return (short) i;
    }

    private byte getTLVType() throws PKCS15Exception {
        if (isEndofBuffer()) {
            throw new PKCS15Exception("[Parser] Cannot retreive type");
        }
        byte[] bArr = this.mDERBuffer;
        short s = this.mDERIndex;
        this.mDERIndex = (short) (s + 1);
        return bArr[s];
    }

    public boolean isEndofBuffer() throws PKCS15Exception {
        if (this.mDERIndex == this.mDERSize) {
            return true;
        }
        if (this.mDERBuffer[this.mDERIndex] == -1) {
            do {
                short s = (short) (this.mDERIndex + 1);
                this.mDERIndex = s;
                if (s >= this.mDERSize) {
                    return true;
                }
            } while (this.mDERBuffer[this.mDERIndex] == -1);
            throw new PKCS15Exception("[Parser] Incorrect file format");
        }
        return false;
    }

    public byte parseTLV() throws PKCS15Exception {
        byte tLVType = getTLVType();
        this.mTLVDataSize = getTLVSize();
        return tLVType;
    }

    public short parseTLV(byte b) throws PKCS15Exception {
        byte tLVType = getTLVType();
        if (tLVType == b) {
            this.mTLVDataSize = getTLVSize();
            return this.mTLVDataSize;
        }
        Log.e("SecureElement-DERParser", "parseTLV expected: " + ((int) b) + " got:" + ((int) tLVType));
        Log.e("SecureElement-DERParser", "parseTLV mDERIndex: " + ((int) this.mDERIndex) + " mDERSize:" + ((int) this.mDERSize));
        throw new PKCS15Exception("[Parser] Unexpected type");
    }

    public void skipTLVData() {
        this.mDERIndex = (short) (this.mDERIndex + this.mTLVDataSize);
    }

    public byte[] getTLVData() {
        byte[] bArrCopyOfRange = Arrays.copyOfRange(this.mDERBuffer, (int) this.mDERIndex, this.mDERIndex + this.mTLVDataSize);
        this.mDERIndex = (short) (this.mDERIndex + this.mTLVDataSize);
        return bArrCopyOfRange;
    }

    public short[] saveContext() {
        return new short[]{this.mDERIndex, this.mTLVDataSize};
    }

    public void restoreContext(short[] sArr) throws PKCS15Exception {
        if (sArr == null || sArr.length != 2) {
            throw new PKCS15Exception("[Parser] Invalid context");
        }
        if (sArr[0] < 0 || sArr[0] > this.mDERSize) {
            throw new PKCS15Exception("[Parser] Index out of bound");
        }
        this.mDERIndex = sArr[0];
        this.mTLVDataSize = sArr[1];
    }

    public String parseOID() throws PKCS15Exception {
        if (parseTLV((byte) 6) == 0) {
            throw new PKCS15Exception("[Parser] OID Length is null");
        }
        int i = this.mDERIndex + this.mTLVDataSize;
        StringBuffer stringBuffer = new StringBuffer();
        int intBase128 = readIntBase128();
        if (intBase128 <= 79) {
            stringBuffer.append(intBase128 / 40);
            stringBuffer.append('.');
            stringBuffer.append(intBase128 % 40);
        } else {
            stringBuffer.append("2.");
            stringBuffer.append(intBase128 - 80);
        }
        while (this.mDERIndex < i) {
            stringBuffer.append('.');
            stringBuffer.append(readIntBase128());
        }
        Log.i("SecureElement-DERParser", "Found OID: " + stringBuffer.toString());
        return stringBuffer.toString();
    }

    public byte[] parsePathAttributes() throws PKCS15Exception {
        parseTLV(ASN1.TAG_Sequence);
        parseTLV((byte) 4);
        return getTLVData();
    }
}
