package com.android.org.bouncycastle.x509.extension;

import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PublicKey;

public class SubjectKeyIdentifierStructure extends SubjectKeyIdentifier {
    public SubjectKeyIdentifierStructure(byte[] encodedValue) throws IOException {
        super((ASN1OctetString) X509ExtensionUtil.fromExtensionValue(encodedValue));
    }

    private static ASN1OctetString fromPublicKey(PublicKey pubKey) throws InvalidKeyException {
        try {
            SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(pubKey.getEncoded());
            return (ASN1OctetString) new SubjectKeyIdentifier(info).toASN1Object();
        } catch (Exception e) {
            throw new InvalidKeyException("Exception extracting key details: " + e.toString());
        }
    }

    public SubjectKeyIdentifierStructure(PublicKey pubKey) throws InvalidKeyException {
        super(fromPublicKey(pubKey));
    }
}
