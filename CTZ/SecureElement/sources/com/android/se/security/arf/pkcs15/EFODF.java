package com.android.se.security.arf.pkcs15;

import android.util.Log;
import com.android.se.internal.ByteArrayConverter;
import com.android.se.security.arf.ASN1;
import com.android.se.security.arf.DERParser;
import com.android.se.security.arf.SecureElement;
import com.android.se.security.arf.SecureElementException;
import java.io.IOException;

public class EFODF extends EF {
    public static final byte[] EFODF_PATH = {ASN1.TAG_ApplLabel, 49};
    byte[] mCDFPath;
    public final String mTag;

    public EFODF(SecureElement secureElement) {
        super(secureElement);
        this.mTag = "SecureElement-ARF-EFODF";
        this.mCDFPath = null;
    }

    private byte[] decodeDER(byte[] bArr) throws PKCS15Exception {
        DERParser dERParser = new DERParser(bArr);
        while (!dERParser.isEndofBuffer()) {
            if (dERParser.parseTLV() == -91) {
                this.mCDFPath = dERParser.parsePathAttributes();
                Log.i("SecureElement-ARF-EFODF", "ODF content found mCDFPath :" + ByteArrayConverter.byteArrayToHexString(this.mCDFPath));
            } else {
                dERParser.skipTLVData();
            }
        }
        DERParser dERParser2 = new DERParser(bArr);
        while (!dERParser2.isEndofBuffer()) {
            if (dERParser2.parseTLV() == -89) {
                return dERParser2.parsePathAttributes();
            }
            dERParser2.skipTLVData();
        }
        return null;
    }

    public byte[] getCDFPath() {
        return this.mCDFPath;
    }

    public byte[] analyseFile(byte[] bArr) throws SecureElementException, IOException, PKCS15Exception {
        byte[] bArr2;
        Log.i("SecureElement-ARF-EFODF", "Analysing EF_ODF...");
        if (bArr != null) {
            bArr2 = new byte[bArr.length + EFODF_PATH.length];
            System.arraycopy(bArr, 0, bArr2, 0, bArr.length);
            System.arraycopy(EFODF_PATH, 0, bArr2, bArr.length, EFODF_PATH.length);
        } else {
            bArr2 = EFODF_PATH;
        }
        if (selectFile(bArr2) != 36864) {
            try {
                synchronized (this) {
                    wait(1000L);
                }
            } catch (InterruptedException e) {
                Log.e("SecureElement-ARF-EFODF", "Interupted while waiting for EF_ODF : " + e);
            }
            if (selectFile(bArr2) != 36864) {
                throw new PKCS15Exception("EF_ODF not found!!");
            }
        }
        return decodeDER(readBinary(0, -1));
    }
}
