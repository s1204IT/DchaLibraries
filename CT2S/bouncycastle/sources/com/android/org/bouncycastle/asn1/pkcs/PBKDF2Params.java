package com.android.org.bouncycastle.asn1.pkcs;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
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
import java.math.BigInteger;
import java.util.Enumeration;

public class PBKDF2Params extends ASN1Object {
    private static final AlgorithmIdentifier algid_hmacWithSHA1 = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_hmacWithSHA1, (ASN1Encodable) DERNull.INSTANCE);
    private ASN1Integer iterationCount;
    private ASN1Integer keyLength;
    private ASN1OctetString octStr;
    private AlgorithmIdentifier prf;

    public static PBKDF2Params getInstance(Object obj) {
        if (obj instanceof PBKDF2Params) {
            return (PBKDF2Params) obj;
        }
        if (obj != null) {
            return new PBKDF2Params(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    public PBKDF2Params(byte[] salt, int iterationCount) {
        this.octStr = new DEROctetString(salt);
        this.iterationCount = new ASN1Integer(iterationCount);
    }

    public PBKDF2Params(byte[] salt, int iterationCount, int keyLength) {
        this(salt, iterationCount);
        this.keyLength = new ASN1Integer(keyLength);
    }

    public PBKDF2Params(byte[] salt, int iterationCount, int keyLength, AlgorithmIdentifier prf) {
        this(salt, iterationCount);
        this.keyLength = new ASN1Integer(keyLength);
        this.prf = prf;
    }

    public PBKDF2Params(byte[] salt, int iterationCount, AlgorithmIdentifier prf) {
        this(salt, iterationCount);
        this.prf = prf;
    }

    private PBKDF2Params(ASN1Sequence seq) {
        Enumeration e = seq.getObjects();
        this.octStr = (ASN1OctetString) e.nextElement();
        this.iterationCount = (ASN1Integer) e.nextElement();
        if (e.hasMoreElements()) {
            Object o = e.nextElement();
            if (o instanceof ASN1Integer) {
                this.keyLength = ASN1Integer.getInstance(o);
                if (e.hasMoreElements()) {
                    o = e.nextElement();
                } else {
                    o = null;
                }
            } else {
                this.keyLength = null;
            }
            if (o != null) {
                this.prf = AlgorithmIdentifier.getInstance(o);
            }
        }
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
        return this.prf != null ? this.prf : algid_hmacWithSHA1;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(this.octStr);
        v.add(this.iterationCount);
        if (this.keyLength != null) {
            v.add(this.keyLength);
        }
        if (this.prf != null && !this.prf.equals(algid_hmacWithSHA1)) {
            v.add(this.prf);
        }
        return new DERSequence(v);
    }
}
