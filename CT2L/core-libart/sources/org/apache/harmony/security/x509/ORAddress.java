package org.apache.harmony.security.x509;

import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;

public final class ORAddress {
    public static final ASN1Sequence ASN1 = new ASN1Sequence(new ASN1Type[]{new ASN1Sequence(new ASN1Type[0]) {
        @Override
        protected void getValues(Object object, Object[] values) {
        }
    }}) {
        private final Object foo = new Object();

        @Override
        protected Object getDecodedObject(BerInputStream in) {
            return new ORAddress();
        }

        @Override
        protected void getValues(Object object, Object[] values) {
            values[0] = this.foo;
        }
    };
    private byte[] encoding;

    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this);
        }
        return this.encoding;
    }
}
