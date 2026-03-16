package org.apache.harmony.security.x509;

import org.apache.harmony.security.asn1.ASN1BitString;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.asn1.BitString;
import org.apache.harmony.security.utils.Array;

public final class CertificateList {
    public static final ASN1Sequence ASN1 = new ASN1Sequence(new ASN1Type[]{TBSCertList.ASN1, AlgorithmIdentifier.ASN1, ASN1BitString.getInstance()}) {
        @Override
        protected Object getDecodedObject(BerInputStream in) {
            Object[] values = (Object[]) in.content;
            return new CertificateList((TBSCertList) values[0], (AlgorithmIdentifier) values[1], ((BitString) values[2]).bytes, in.getEncoded());
        }

        @Override
        protected void getValues(Object object, Object[] values) {
            CertificateList certificateList = (CertificateList) object;
            values[0] = certificateList.tbsCertList;
            values[1] = certificateList.signatureAlgorithm;
            values[2] = new BitString(certificateList.signatureValue, 0);
        }
    };
    private byte[] encoding;
    private final AlgorithmIdentifier signatureAlgorithm;
    private final byte[] signatureValue;
    private final TBSCertList tbsCertList;

    public CertificateList(TBSCertList tbsCertList, AlgorithmIdentifier signatureAlgorithm, byte[] signatureValue) {
        this.tbsCertList = tbsCertList;
        this.signatureAlgorithm = signatureAlgorithm;
        this.signatureValue = new byte[signatureValue.length];
        System.arraycopy(signatureValue, 0, this.signatureValue, 0, signatureValue.length);
    }

    private CertificateList(TBSCertList tbsCertList, AlgorithmIdentifier signatureAlgorithm, byte[] signatureValue, byte[] encoding) {
        this(tbsCertList, signatureAlgorithm, signatureValue);
        this.encoding = encoding;
    }

    public TBSCertList getTbsCertList() {
        return this.tbsCertList;
    }

    public byte[] getSignatureValue() {
        byte[] result = new byte[this.signatureValue.length];
        System.arraycopy(this.signatureValue, 0, result, 0, this.signatureValue.length);
        return result;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        this.tbsCertList.dumpValue(result);
        result.append("\nSignature Value:\n");
        result.append(Array.toString(this.signatureValue, ""));
        return result.toString();
    }

    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this);
        }
        return this.encoding;
    }
}
