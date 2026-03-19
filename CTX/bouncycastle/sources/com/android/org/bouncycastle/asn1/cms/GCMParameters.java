package com.android.org.bouncycastle.asn1.cms;

import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.util.Arrays;

public class GCMParameters extends ASN1Object {
    private int icvLen;
    private byte[] nonce;

    public static GCMParameters getInstance(Object obj) {
        if (obj instanceof GCMParameters) {
            return (GCMParameters) obj;
        }
        if (obj != null) {
            return new GCMParameters(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    private GCMParameters(ASN1Sequence aSN1Sequence) {
        this.nonce = ASN1OctetString.getInstance(aSN1Sequence.getObjectAt(0)).getOctets();
        if (aSN1Sequence.size() == 2) {
            this.icvLen = ASN1Integer.getInstance(aSN1Sequence.getObjectAt(1)).getValue().intValue();
        } else {
            this.icvLen = 12;
        }
    }

    public GCMParameters(byte[] bArr, int i) {
        this.nonce = Arrays.clone(bArr);
        this.icvLen = i;
    }

    public byte[] getNonce() {
        return Arrays.clone(this.nonce);
    }

    public int getIcvLen() {
        return this.icvLen;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(new DEROctetString(this.nonce));
        if (this.icvLen != 12) {
            aSN1EncodableVector.add(new ASN1Integer(this.icvLen));
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
