package com.android.se.security.arf.pkcs15;

import android.util.Log;
import com.android.se.security.arf.ASN1;
import com.android.se.security.arf.DERParser;
import com.android.se.security.arf.SecureElement;
import com.android.se.security.arf.SecureElementException;
import java.io.IOException;

public class EFDODF extends EF {
    public static final String AC_OID = "1.2.840.114283.200.1.1";
    public static final String TAG = "ACE ARF EF_DODF";

    public EFDODF(SecureElement secureElement) {
        super(secureElement);
    }

    private byte[] decodeDER(byte[] bArr) throws PKCS15Exception {
        DERParser dERParser = new DERParser(bArr);
        while (!dERParser.isEndofBuffer()) {
            if (dERParser.parseTLV() == -95) {
                dERParser.parseTLV(ASN1.TAG_Sequence);
                dERParser.skipTLVData();
                dERParser.parseTLV(ASN1.TAG_Sequence);
                dERParser.skipTLVData();
                byte tlv = dERParser.parseTLV();
                if (tlv == -96) {
                    dERParser.skipTLVData();
                    tlv = dERParser.parseTLV();
                }
                if (tlv == -95) {
                    dERParser.parseTLV(ASN1.TAG_Sequence);
                    short[] sArrSaveContext = dERParser.saveContext();
                    if (dERParser.parseOID().compareTo(AC_OID) != 0) {
                        dERParser.restoreContext(sArrSaveContext);
                        dERParser.skipTLVData();
                    } else {
                        return dERParser.parsePathAttributes();
                    }
                } else {
                    throw new PKCS15Exception("[Parser] OID Tag expected");
                }
            } else {
                dERParser.skipTLVData();
            }
        }
        return null;
    }

    public byte[] analyseFile(byte[] bArr) throws SecureElementException, IOException, PKCS15Exception {
        Log.i(TAG, "Analysing EF_DODF...");
        if (selectFile(bArr) != 36864) {
            throw new PKCS15Exception("EF_DODF not found!");
        }
        return decodeDER(readBinary(0, -1));
    }
}
