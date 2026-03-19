package com.android.se.security.arf.pkcs15;

import android.util.Log;
import com.android.se.security.arf.ASN1;
import com.android.se.security.arf.DERParser;
import com.android.se.security.arf.SecureElement;
import com.android.se.security.arf.SecureElementException;
import java.io.IOException;
import java.util.Arrays;

public class EF {
    public static final int APDU_SUCCESS = 36864;
    private static final int BUFFER_LEN = 253;
    private static final short EF = 4;
    private static final short LINEAR_FIXED = 1;
    public static final String TAG = "SecureElementService ACE ARF";
    private static final short TRANSPARENT = 0;
    private static final short UNKNOWN = 255;
    private int mFileID;
    private short mFileNbRecords;
    private int mFileRecordSize;
    private int mFileSize;
    protected SecureElement mSEHandle;
    private short mFileType = UNKNOWN;
    private short mFileStructure = UNKNOWN;

    public EF(SecureElement secureElement) {
        this.mSEHandle = null;
        this.mSEHandle = secureElement;
    }

    public int getFileId() {
        return this.mFileID;
    }

    private void decodeFileProperties(byte[] bArr) throws SecureElementException {
        if (bArr != null) {
            if (bArr[0] == 98) {
                decodeUSIMFileProps(bArr);
            } else {
                decodeSIMFileProps(bArr);
            }
        }
    }

    private void decodeSIMFileProps(byte[] bArr) throws SecureElementException {
        if (bArr == null || bArr.length < 15) {
            throw new SecureElementException("Invalid Response data");
        }
        if (((short) (bArr[6] & 255)) == 4) {
            this.mFileType = EF;
        } else {
            this.mFileType = UNKNOWN;
        }
        if (((short) (bArr[13] & 255)) == 0) {
            this.mFileStructure = TRANSPARENT;
        } else if (((short) (bArr[13] & 255)) == 1) {
            this.mFileStructure = LINEAR_FIXED;
        } else {
            this.mFileStructure = UNKNOWN;
        }
        this.mFileSize = ((bArr[2] & 255) << 8) | (bArr[3] & 255);
        if (this.mFileType == 4 && this.mFileStructure != 0) {
            this.mFileRecordSize = bArr[14] & 255;
            this.mFileNbRecords = (short) (this.mFileSize / this.mFileRecordSize);
        }
    }

    private void decodeUSIMFileProps(byte[] bArr) throws SecureElementException {
        try {
            DERParser dERParser = new DERParser(bArr);
            dERParser.parseTLV(ASN1.TAG_FCP);
            while (!dERParser.isEndofBuffer()) {
                byte tlv = dERParser.parseTLV();
                if (tlv == -128) {
                    byte[] tLVData = dERParser.getTLVData();
                    if (tLVData != null && tLVData.length >= 2) {
                        this.mFileSize = (tLVData[1] & 255) | ((tLVData[0] & 255) << 8);
                    }
                } else if (tlv == -126) {
                    byte[] tLVData2 = dERParser.getTLVData();
                    if (tLVData2 != null && tLVData2.length >= 2) {
                        if (((short) (tLVData2[0] & 7)) == 1) {
                            this.mFileStructure = TRANSPARENT;
                        } else if (((short) (tLVData2[0] & 7)) == 2) {
                            this.mFileStructure = LINEAR_FIXED;
                        } else {
                            this.mFileStructure = UNKNOWN;
                        }
                        if (((short) (tLVData2[0] & 56)) == 56) {
                            this.mFileType = UNKNOWN;
                        } else {
                            this.mFileType = EF;
                        }
                        if (tLVData2.length == 5) {
                            this.mFileRecordSize = tLVData2[3] & 255;
                            this.mFileNbRecords = (short) (tLVData2[4] & 255);
                        }
                    }
                } else {
                    dERParser.skipTLVData();
                }
            }
        } catch (Exception e) {
            throw new SecureElementException("Invalid GetResponse");
        }
    }

    public int selectFile(byte[] bArr) throws SecureElementException, IOException {
        if (bArr == null || bArr.length == 0 || bArr.length % 2 != 0) {
            throw new SecureElementException("Incorrect path");
        }
        int length = bArr.length;
        byte[] bArrExchangeAPDU = null;
        byte[] bArr2 = {0, ASN1.TAG_Certificate, 0, 4, 2, 0, 0};
        this.mFileType = UNKNOWN;
        this.mFileStructure = UNKNOWN;
        this.mFileSize = 0;
        this.mFileRecordSize = 0;
        this.mFileNbRecords = TRANSPARENT;
        for (int i = 0; i < length; i += 2) {
            this.mFileID = ((bArr[i] & 255) << 8) | (bArr[i + 1] & 255);
            bArr2[5] = (byte) (this.mFileID >> 8);
            bArr2[6] = (byte) this.mFileID;
            bArrExchangeAPDU = this.mSEHandle.exchangeAPDU(this, bArr2);
            int i2 = bArrExchangeAPDU[bArrExchangeAPDU.length - 2] & 255;
            if (i2 != 98 && i2 != 99 && i2 != 144 && i2 != 145) {
                return (i2 << 8) | (bArrExchangeAPDU[bArrExchangeAPDU.length - 1] & 255);
            }
        }
        decodeFileProperties(bArrExchangeAPDU);
        return APDU_SUCCESS;
    }

    public byte[] readBinary(int i, int i2) throws SecureElementException, IOException {
        if (this.mFileSize == 0) {
            return null;
        }
        if (i2 == -1) {
            i2 = this.mFileSize;
        }
        if (this.mFileType != 4) {
            throw new SecureElementException("Incorrect file type");
        }
        if (this.mFileStructure != 0) {
            throw new SecureElementException("Incorrect file structure");
        }
        byte[] bArr = new byte[i2];
        byte[] bArr2 = {0, -80, 0, 0, 0};
        int i3 = 0;
        while (i2 != 0) {
            int i4 = BUFFER_LEN;
            if (i2 < BUFFER_LEN) {
                i4 = i2;
            }
            bArr2[2] = (byte) (i >> 8);
            bArr2[3] = (byte) i;
            bArr2[4] = (byte) i4;
            System.arraycopy(this.mSEHandle.exchangeAPDU(this, bArr2), 0, bArr, i3, i4);
            i2 -= i4;
            i += i4;
            i3 += i4;
        }
        return bArr;
    }

    public byte[] readRecord(short s) throws SecureElementException, IOException {
        if (this.mFileType != 4) {
            throw new SecureElementException("Incorrect file type");
        }
        if (this.mFileStructure != 1) {
            throw new SecureElementException("Incorrect file structure");
        }
        if (s < 0 || s > this.mFileNbRecords) {
            throw new SecureElementException("Incorrect record number");
        }
        Log.i(TAG, "ReadRecord [" + ((int) s) + "/" + this.mFileRecordSize + "b]");
        return Arrays.copyOf(this.mSEHandle.exchangeAPDU(this, new byte[]{0, -78, (byte) s, 4, (byte) this.mFileRecordSize}), this.mFileRecordSize);
    }

    public short getFileNbRecords() throws SecureElementException {
        if (this.mFileNbRecords < 0) {
            throw new SecureElementException("Incorrect file type");
        }
        return this.mFileNbRecords;
    }
}
