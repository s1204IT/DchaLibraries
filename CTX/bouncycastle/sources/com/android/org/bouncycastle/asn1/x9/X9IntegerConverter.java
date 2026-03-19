package com.android.org.bouncycastle.asn1.x9;

import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECFieldElement;
import java.math.BigInteger;

public class X9IntegerConverter {
    public int getByteLength(ECCurve eCCurve) {
        return (eCCurve.getFieldSize() + 7) / 8;
    }

    public int getByteLength(ECFieldElement eCFieldElement) {
        return (eCFieldElement.getFieldSize() + 7) / 8;
    }

    public byte[] integerToBytes(BigInteger bigInteger, int i) {
        byte[] byteArray = bigInteger.toByteArray();
        if (i < byteArray.length) {
            byte[] bArr = new byte[i];
            System.arraycopy(byteArray, byteArray.length - bArr.length, bArr, 0, bArr.length);
            return bArr;
        }
        if (i > byteArray.length) {
            byte[] bArr2 = new byte[i];
            System.arraycopy(byteArray, 0, bArr2, bArr2.length - byteArray.length, byteArray.length);
            return bArr2;
        }
        return byteArray;
    }
}
