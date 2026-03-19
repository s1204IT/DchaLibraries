package com.android.se.security.arf.pkcs15;

import android.util.Log;
import com.android.se.internal.ByteArrayConverter;
import com.android.se.security.arf.ASN1;
import com.android.se.security.arf.DERParser;
import com.android.se.security.arf.SecureElement;
import com.android.se.security.arf.SecureElementException;
import com.android.se.security.gpac.AID_REF_DO;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EFACRules extends EF {
    public static final byte[] DEFAULT_APP = new byte[0];
    public static final String TAG = "ACE ARF EF_ACRules";
    protected Map<String, byte[]> mAcConditionDataCache;

    public EFACRules(SecureElement secureElement) {
        super(secureElement);
        this.mAcConditionDataCache = new HashMap();
    }

    private void decodeDER(byte[] bArr) throws IOException, PKCS15Exception {
        byte[] tLVData;
        DERParser dERParser = new DERParser(bArr);
        while (!dERParser.isEndofBuffer()) {
            dERParser.parseTLV(ASN1.TAG_Sequence);
            byte tlv = dERParser.parseTLV();
            int i = 79;
            if (tlv == -96) {
                dERParser.parseTLV((byte) 4);
                tLVData = dERParser.getTLVData();
            } else {
                switch (tlv) {
                    case -127:
                        tLVData = null;
                        i = AID_REF_DO.TAG_DEFAULT_APPLICATION;
                        break;
                    case -126:
                        tLVData = DEFAULT_APP;
                        break;
                    default:
                        throw new PKCS15Exception("[Parser] Unexpected ACRules entry");
                }
            }
            byte[] pathAttributes = dERParser.parsePathAttributes();
            if (pathAttributes != null) {
                String strByteArrayToHexString = ByteArrayConverter.byteArrayToHexString(pathAttributes);
                EFACConditions eFACConditions = new EFACConditions(this.mSEHandle, new AID_REF_DO(i, tLVData));
                if (this.mAcConditionDataCache.containsKey(strByteArrayToHexString)) {
                    eFACConditions.addRestrictedHashesFromData(this.mAcConditionDataCache.get(strByteArrayToHexString));
                } else {
                    eFACConditions.addRestrictedHashes(pathAttributes);
                    if (eFACConditions.getData() != null) {
                        this.mAcConditionDataCache.put(strByteArrayToHexString, eFACConditions.getData());
                    }
                }
            }
        }
    }

    public void analyseFile(byte[] bArr) throws SecureElementException, IOException, PKCS15Exception {
        Log.i(TAG, "Analysing EF_ACRules...");
        this.mAcConditionDataCache.clear();
        if (selectFile(bArr) != 36864) {
            throw new PKCS15Exception("EF_ACRules not found!!");
        }
        try {
            decodeDER(readBinary(0, -1));
        } catch (PKCS15Exception e) {
            throw e;
        }
    }
}
