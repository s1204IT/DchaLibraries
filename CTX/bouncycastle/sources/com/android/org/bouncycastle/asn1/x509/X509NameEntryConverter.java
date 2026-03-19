package com.android.org.bouncycastle.asn1.x509;

import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.DERPrintableString;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;

public abstract class X509NameEntryConverter {
    public abstract ASN1Primitive getConvertedValue(ASN1ObjectIdentifier aSN1ObjectIdentifier, String str);

    protected ASN1Primitive convertHexEncoded(String str, int i) throws IOException {
        String lowerCase = Strings.toLowerCase(str);
        byte[] bArr = new byte[(lowerCase.length() - i) / 2];
        for (int i2 = 0; i2 != bArr.length; i2++) {
            int i3 = (i2 * 2) + i;
            char cCharAt = lowerCase.charAt(i3);
            char cCharAt2 = lowerCase.charAt(i3 + 1);
            if (cCharAt < 'a') {
                bArr[i2] = (byte) ((cCharAt - '0') << 4);
            } else {
                bArr[i2] = (byte) (((cCharAt - 'a') + 10) << 4);
            }
            if (cCharAt2 < 'a') {
                bArr[i2] = (byte) (((byte) (cCharAt2 - '0')) | bArr[i2]);
            } else {
                bArr[i2] = (byte) (((byte) ((cCharAt2 - 'a') + 10)) | bArr[i2]);
            }
        }
        return new ASN1InputStream(bArr).readObject();
    }

    protected boolean canBePrintable(String str) {
        return DERPrintableString.isPrintableString(str);
    }
}
