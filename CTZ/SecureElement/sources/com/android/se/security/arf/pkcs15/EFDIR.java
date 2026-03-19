package com.android.se.security.arf.pkcs15;

import android.util.Log;
import com.android.se.security.arf.ASN1;
import com.android.se.security.arf.DERParser;
import com.android.se.security.arf.SecureElement;
import com.android.se.security.arf.SecureElementException;
import java.io.IOException;
import java.util.Arrays;

public class EFDIR extends EF {
    public static final byte[] EFDIR_PATH = {63, 0, 47, 0};
    public static final String TAG = "ACE ARF EF_Dir";

    public EFDIR(SecureElement secureElement) {
        super(secureElement);
    }

    private byte[] decodeDER(byte[] bArr, byte[] bArr2) throws PKCS15Exception {
        DERParser dERParser = new DERParser(bArr);
        dERParser.parseTLV(ASN1.TAG_ApplTemplate);
        dERParser.parseTLV(ASN1.TAG_ApplIdentifier);
        if (!Arrays.equals(dERParser.getTLVData(), bArr2)) {
            return null;
        }
        byte tlv = dERParser.parseTLV();
        if (tlv == 80) {
            dERParser.getTLVData();
            dERParser.parseTLV(ASN1.TAG_ApplPath);
        } else if (tlv != 81) {
            throw new PKCS15Exception("[Parser] Application Tag expected");
        }
        return dERParser.getTLVData();
    }

    public byte[] lookupAID(byte[] bArr) throws SecureElementException, IOException, PKCS15Exception {
        Log.i(TAG, "Analysing EF_DIR...");
        if (selectFile(EFDIR_PATH) != 36864) {
            throw new PKCS15Exception("EF_DIR not found!!");
        }
        byte[] bArr2 = null;
        short s = 1;
        while (s <= getFileNbRecords()) {
            short s2 = (short) (s + 1);
            byte[] bArrDecodeDER = decodeDER(readRecord(s), bArr);
            if (bArrDecodeDER == null) {
                s = s2;
                bArr2 = bArrDecodeDER;
            } else {
                return bArrDecodeDER;
            }
        }
        return bArr2;
    }
}
