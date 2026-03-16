package org.apache.harmony.security.pkcs10;

import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.apache.harmony.security.asn1.ASN1Implicit;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1SetOf;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.x501.AttributeTypeAndValue;
import org.apache.harmony.security.x501.Name;
import org.apache.harmony.security.x509.SubjectPublicKeyInfo;

public final class CertificationRequestInfo {
    public static final ASN1Sequence ASN1 = new ASN1Sequence(new ASN1Type[]{ASN1Integer.getInstance(), Name.ASN1, SubjectPublicKeyInfo.ASN1, new ASN1Implicit(0, new ASN1SetOf(AttributeTypeAndValue.ASN1))}) {
        @Override
        protected Object getDecodedObject(BerInputStream in) {
            Object[] values = (Object[]) in.content;
            return new CertificationRequestInfo(ASN1Integer.toIntValue(values[0]), (Name) values[1], (SubjectPublicKeyInfo) values[2], (List) values[3], in.getEncoded());
        }

        @Override
        protected void getValues(Object object, Object[] values) {
            CertificationRequestInfo certReqInfo = (CertificationRequestInfo) object;
            values[0] = ASN1Integer.fromIntValue(certReqInfo.version);
            values[1] = certReqInfo.subject;
            values[2] = certReqInfo.subjectPublicKeyInfo;
            values[3] = certReqInfo.attributes;
        }
    };
    private final List<?> attributes;
    private byte[] encoding;
    private final Name subject;
    private final SubjectPublicKeyInfo subjectPublicKeyInfo;
    private final int version;

    private CertificationRequestInfo(int version, Name subject, SubjectPublicKeyInfo subjectPublicKeyInfo, List<?> attributes, byte[] encoding) {
        this.version = version;
        this.subject = subject;
        this.subjectPublicKeyInfo = subjectPublicKeyInfo;
        this.attributes = attributes;
        this.encoding = encoding;
    }

    public Name getSubject() {
        return this.subject;
    }

    public int getVersion() {
        return this.version;
    }

    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this);
        }
        return this.encoding;
    }

    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("-- CertificationRequestInfo:");
        res.append("\n version: ");
        res.append(this.version);
        res.append("\n subject: ");
        res.append(this.subject.getName(X500Principal.CANONICAL));
        res.append("\n subjectPublicKeyInfo: ");
        res.append("\n\t algorithm: ");
        res.append(this.subjectPublicKeyInfo.getAlgorithmIdentifier().getAlgorithm());
        res.append("\n\t public key: ").append(this.subjectPublicKeyInfo.getPublicKey());
        res.append("\n attributes: ");
        if (this.attributes != null) {
            res.append(this.attributes.toString());
        } else {
            res.append("none");
        }
        res.append("\n-- CertificationRequestInfo End\n");
        return res.toString();
    }
}
