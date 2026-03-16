package org.apache.harmony.security.pkcs10;

import org.apache.harmony.security.asn1.ASN1BitString;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.asn1.BitString;
import org.apache.harmony.security.x509.AlgorithmIdentifier;

public final class CertificationRequest {
    public static final ASN1Sequence ASN1 = new ASN1Sequence(new ASN1Type[]{CertificationRequestInfo.ASN1, AlgorithmIdentifier.ASN1, ASN1BitString.getInstance()}) {
        @Override
        public Object getDecodedObject(BerInputStream in) {
            Object[] values = (Object[]) in.content;
            return new CertificationRequest((CertificationRequestInfo) values[0], (AlgorithmIdentifier) values[1], ((BitString) values[2]).bytes, in.getEncoded());
        }

        @Override
        protected void getValues(Object object, Object[] values) {
            CertificationRequest certReq = (CertificationRequest) object;
            values[0] = certReq.info;
            values[1] = certReq.algId;
            values[2] = new BitString(certReq.signature, 0);
        }
    };
    private AlgorithmIdentifier algId;
    private byte[] encoding;
    private CertificationRequestInfo info;
    private byte[] signature;

    public CertificationRequest(CertificationRequestInfo info, AlgorithmIdentifier algId, byte[] signature) {
        this.info = info;
        this.algId = algId;
        this.signature = (byte[]) signature.clone();
    }

    private CertificationRequest(CertificationRequestInfo info, AlgorithmIdentifier algId, byte[] signature, byte[] encoding) {
        this(info, algId, signature);
        this.encoding = encoding;
    }

    public CertificationRequestInfo getInfo() {
        return this.info;
    }

    public byte[] getSignature() {
        byte[] result = new byte[this.signature.length];
        System.arraycopy(this.signature, 0, result, 0, this.signature.length);
        return result;
    }

    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this);
        }
        return this.encoding;
    }
}
