package com.android.org.bouncycastle.asn1.pkcs;

import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import java.util.Enumeration;

public class PBES2Algorithms extends AlgorithmIdentifier implements PKCSObjectIdentifiers {
    private KeyDerivationFunc func;
    private ASN1ObjectIdentifier objectId;
    private EncryptionScheme scheme;

    public PBES2Algorithms(ASN1Sequence obj) {
        super(obj);
        Enumeration e = obj.getObjects();
        this.objectId = (ASN1ObjectIdentifier) e.nextElement();
        ASN1Sequence seq = (ASN1Sequence) e.nextElement();
        Enumeration e2 = seq.getObjects();
        ASN1Sequence funcSeq = (ASN1Sequence) e2.nextElement();
        if (funcSeq.getObjectAt(0).equals(id_PBKDF2)) {
            this.func = new KeyDerivationFunc(id_PBKDF2, PBKDF2Params.getInstance(funcSeq.getObjectAt(1)));
        } else {
            this.func = KeyDerivationFunc.getInstance(funcSeq);
        }
        this.scheme = EncryptionScheme.getInstance(e2.nextElement());
    }

    @Override
    public ASN1ObjectIdentifier getObjectId() {
        return this.objectId;
    }

    public KeyDerivationFunc getKeyDerivationFunc() {
        return this.func;
    }

    public EncryptionScheme getEncryptionScheme() {
        return this.scheme;
    }

    public ASN1Primitive getASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        ASN1EncodableVector subV = new ASN1EncodableVector();
        v.add(this.objectId);
        subV.add(this.func);
        subV.add(this.scheme);
        v.add(new DERSequence(subV));
        return new DERSequence(v);
    }
}
