package org.apache.harmony.security.pkcs7;

import java.util.List;
import org.apache.harmony.security.asn1.ASN1SetOf;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.x501.AttributeTypeAndValue;

final class AuthenticatedAttributes {
    public static final ASN1SetOf ASN1 = new ASN1SetOf(AttributeTypeAndValue.ASN1) {
        @Override
        public Object getDecodedObject(BerInputStream in) {
            return new AuthenticatedAttributes(in.getEncoded(), (List) in.content);
        }
    };
    private final List<AttributeTypeAndValue> authenticatedAttributes;
    private byte[] encoding;

    private AuthenticatedAttributes(byte[] encoding, List<AttributeTypeAndValue> authenticatedAttributes) {
        this.encoding = encoding;
        this.authenticatedAttributes = authenticatedAttributes;
    }

    public List<AttributeTypeAndValue> getAttributes() {
        return this.authenticatedAttributes;
    }

    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this);
        }
        return this.encoding;
    }
}
