package com.android.org.bouncycastle.asn1.pkcs;

import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import java.util.Enumeration;

public class EncryptedPrivateKeyInfo extends ASN1Object {
    private AlgorithmIdentifier algId;
    private ASN1OctetString data;

    private EncryptedPrivateKeyInfo(ASN1Sequence aSN1Sequence) {
        Enumeration objects = aSN1Sequence.getObjects();
        this.algId = AlgorithmIdentifier.getInstance(objects.nextElement());
        this.data = ASN1OctetString.getInstance(objects.nextElement());
    }

    public EncryptedPrivateKeyInfo(AlgorithmIdentifier algorithmIdentifier, byte[] bArr) {
        this.algId = algorithmIdentifier;
        this.data = new DEROctetString(bArr);
    }

    public static EncryptedPrivateKeyInfo getInstance(Object obj) {
        if (obj instanceof EncryptedPrivateKeyInfo) {
            return (EncryptedPrivateKeyInfo) obj;
        }
        if (obj != null) {
            return new EncryptedPrivateKeyInfo(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    public AlgorithmIdentifier getEncryptionAlgorithm() {
        return this.algId;
    }

    public byte[] getEncryptedData() {
        return this.data.getOctets();
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.algId);
        aSN1EncodableVector.add(this.data);
        return new DERSequence(aSN1EncodableVector);
    }
}
