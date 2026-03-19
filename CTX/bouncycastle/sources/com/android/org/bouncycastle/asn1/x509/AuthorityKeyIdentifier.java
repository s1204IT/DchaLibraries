package com.android.org.bouncycastle.asn1.x509;

import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.DERTaggedObject;
import com.android.org.bouncycastle.crypto.Digest;
import com.android.org.bouncycastle.crypto.digests.AndroidDigestFactory;
import java.math.BigInteger;
import java.util.Enumeration;

public class AuthorityKeyIdentifier extends ASN1Object {
    GeneralNames certissuer;
    ASN1Integer certserno;
    ASN1OctetString keyidentifier;

    public static AuthorityKeyIdentifier getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public static AuthorityKeyIdentifier getInstance(Object obj) {
        if (obj instanceof AuthorityKeyIdentifier) {
            return (AuthorityKeyIdentifier) obj;
        }
        if (obj != null) {
            return new AuthorityKeyIdentifier(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    public static AuthorityKeyIdentifier fromExtensions(Extensions extensions) {
        return getInstance(extensions.getExtensionParsedValue(Extension.authorityKeyIdentifier));
    }

    protected AuthorityKeyIdentifier(ASN1Sequence aSN1Sequence) {
        this.keyidentifier = null;
        this.certissuer = null;
        this.certserno = null;
        Enumeration objects = aSN1Sequence.getObjects();
        while (objects.hasMoreElements()) {
            ASN1TaggedObject dERTaggedObject = DERTaggedObject.getInstance(objects.nextElement());
            switch (dERTaggedObject.getTagNo()) {
                case 0:
                    this.keyidentifier = ASN1OctetString.getInstance(dERTaggedObject, false);
                    break;
                case 1:
                    this.certissuer = GeneralNames.getInstance(dERTaggedObject, false);
                    break;
                case 2:
                    this.certserno = ASN1Integer.getInstance(dERTaggedObject, false);
                    break;
                default:
                    throw new IllegalArgumentException("illegal tag");
            }
        }
    }

    public AuthorityKeyIdentifier(SubjectPublicKeyInfo subjectPublicKeyInfo) {
        this.keyidentifier = null;
        this.certissuer = null;
        this.certserno = null;
        Digest sha1 = AndroidDigestFactory.getSHA1();
        byte[] bArr = new byte[sha1.getDigestSize()];
        byte[] bytes = subjectPublicKeyInfo.getPublicKeyData().getBytes();
        sha1.update(bytes, 0, bytes.length);
        sha1.doFinal(bArr, 0);
        this.keyidentifier = new DEROctetString(bArr);
    }

    public AuthorityKeyIdentifier(SubjectPublicKeyInfo subjectPublicKeyInfo, GeneralNames generalNames, BigInteger bigInteger) {
        this.keyidentifier = null;
        this.certissuer = null;
        this.certserno = null;
        Digest sha1 = AndroidDigestFactory.getSHA1();
        byte[] bArr = new byte[sha1.getDigestSize()];
        byte[] bytes = subjectPublicKeyInfo.getPublicKeyData().getBytes();
        sha1.update(bytes, 0, bytes.length);
        sha1.doFinal(bArr, 0);
        this.keyidentifier = new DEROctetString(bArr);
        this.certissuer = GeneralNames.getInstance(generalNames.toASN1Primitive());
        this.certserno = new ASN1Integer(bigInteger);
    }

    public AuthorityKeyIdentifier(GeneralNames generalNames, BigInteger bigInteger) {
        this((byte[]) null, generalNames, bigInteger);
    }

    public AuthorityKeyIdentifier(byte[] bArr) {
        this(bArr, (GeneralNames) null, (BigInteger) null);
    }

    public AuthorityKeyIdentifier(byte[] bArr, GeneralNames generalNames, BigInteger bigInteger) {
        this.keyidentifier = null;
        this.certissuer = null;
        this.certserno = null;
        this.keyidentifier = bArr != null ? new DEROctetString(bArr) : null;
        this.certissuer = generalNames;
        this.certserno = bigInteger != null ? new ASN1Integer(bigInteger) : null;
    }

    public byte[] getKeyIdentifier() {
        if (this.keyidentifier != null) {
            return this.keyidentifier.getOctets();
        }
        return null;
    }

    public GeneralNames getAuthorityCertIssuer() {
        return this.certissuer;
    }

    public BigInteger getAuthorityCertSerialNumber() {
        if (this.certserno != null) {
            return this.certserno.getValue();
        }
        return null;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.keyidentifier != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 0, this.keyidentifier));
        }
        if (this.certissuer != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 1, this.certissuer));
        }
        if (this.certserno != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 2, this.certserno));
        }
        return new DERSequence(aSN1EncodableVector);
    }

    public String toString() {
        return "AuthorityKeyIdentifier: KeyID(" + this.keyidentifier.getOctets() + ")";
    }
}
