package com.android.se.security.arf.pkcs15;

import android.util.Log;
import com.android.se.security.arf.ASN1;
import com.android.se.security.arf.DERParser;
import com.android.se.security.arf.SecureElement;
import com.android.se.security.arf.SecureElementException;
import java.io.IOException;
import java.util.Arrays;

public class EFACMain extends EF {
    public static final short REFRESHTAG_LEN = 8;
    public static final String TAG = "ACE ARF EF_ACMain";
    private byte[] mACMainPath;

    public EFACMain(SecureElement secureElement, byte[] bArr) {
        super(secureElement);
        this.mACMainPath = null;
        this.mACMainPath = bArr;
    }

    private byte[] decodeDER(byte[] bArr) throws PKCS15Exception {
        DERParser dERParser = new DERParser(bArr);
        dERParser.parseTLV(ASN1.TAG_Sequence);
        if (dERParser.parseTLV((byte) 4) != 8) {
            throw new PKCS15Exception("[Parser] RefreshTag length not valid");
        }
        byte[] tLVData = dERParser.getTLVData();
        if (!Arrays.equals(tLVData, this.mSEHandle.getRefreshTag())) {
            this.mSEHandle.setRefreshTag(tLVData);
            return dERParser.parsePathAttributes();
        }
        return null;
    }

    public byte[] analyseFile() throws SecureElementException, IOException, PKCS15Exception {
        Log.i(TAG, "Analysing EF_ACMain...");
        if (selectFile(this.mACMainPath) != 36864) {
            throw new PKCS15Exception("EF_ACMain not found!");
        }
        return decodeDER(readBinary(0, -1));
    }
}
