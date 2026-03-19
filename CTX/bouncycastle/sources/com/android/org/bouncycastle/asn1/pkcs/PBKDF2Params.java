package com.android.org.bouncycastle.asn1.pkcs;

import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.DERNull;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.util.Arrays;
import java.math.BigInteger;
import java.util.Enumeration;

public class PBKDF2Params extends ASN1Object {
    private static final AlgorithmIdentifier algid_hmacWithSHA1 = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_hmacWithSHA1, DERNull.INSTANCE);
    private final ASN1Integer iterationCount;
    private final ASN1Integer keyLength;
    private final ASN1OctetString octStr;
    private final AlgorithmIdentifier prf;

    public static PBKDF2Params getInstance(Object obj) {
        if (obj instanceof PBKDF2Params) {
            return (PBKDF2Params) obj;
        }
        if (obj != null) {
            return new PBKDF2Params(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    public PBKDF2Params(byte[] bArr, int i) {
        this(bArr, i, 0);
    }

    public PBKDF2Params(byte[] bArr, int i, int i2) {
        this(bArr, i, i2, null);
    }

    public PBKDF2Params(byte[] bArr, int i, int i2, AlgorithmIdentifier algorithmIdentifier) {
        this.octStr = new DEROctetString(Arrays.clone(bArr));
        this.iterationCount = new ASN1Integer(i);
        if (i2 > 0) {
            this.keyLength = new ASN1Integer(i2);
        } else {
            this.keyLength = null;
        }
        this.prf = algorithmIdentifier;
    }

    public PBKDF2Params(byte[] bArr, int i, AlgorithmIdentifier algorithmIdentifier) {
        this(bArr, i, 0, algorithmIdentifier);
    }

    private PBKDF2Params(ASN1Sequence aSN1Sequence) {
        Object objNextElement;
        Enumeration objects = aSN1Sequence.getObjects();
        this.octStr = (ASN1OctetString) objects.nextElement();
        this.iterationCount = (ASN1Integer) objects.nextElement();
        if (objects.hasMoreElements()) {
            Object objNextElement2 = objects.nextElement();
            if (objNextElement2 instanceof ASN1Integer) {
                this.keyLength = ASN1Integer.getInstance(objNextElement2);
                objNextElement = objects.hasMoreElements() ? objects.nextElement() : null;
            } else {
                this.keyLength = null;
                objNextElement = objNextElement2;
            }
            if (objNextElement != null) {
                this.prf = AlgorithmIdentifier.getInstance(objNextElement);
                return;
            } else {
                this.prf = null;
                return;
            }
        }
        this.keyLength = null;
        this.prf = null;
    }

    public byte[] getSalt() {
        return this.octStr.getOctets();
    }

    public BigInteger getIterationCount() {
        return this.iterationCount.getValue();
    }

    public BigInteger getKeyLength() {
        if (this.keyLength != null) {
            return this.keyLength.getValue();
        }
        return null;
    }

    public boolean isDefaultPrf() {
        return this.prf == null || this.prf.equals(algid_hmacWithSHA1);
    }

    public AlgorithmIdentifier getPrf() {
        if (this.prf != null) {
            return this.prf;
        }
        return algid_hmacWithSHA1;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.octStr);
        aSN1EncodableVector.add(this.iterationCount);
        if (this.keyLength != null) {
            aSN1EncodableVector.add(this.keyLength);
        }
        if (this.prf != null && !this.prf.equals(algid_hmacWithSHA1)) {
            aSN1EncodableVector.add(this.prf);
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
