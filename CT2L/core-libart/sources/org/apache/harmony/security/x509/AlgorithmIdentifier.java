package org.apache.harmony.security.x509;

import java.util.Arrays;
import org.apache.harmony.security.asn1.ASN1Any;
import org.apache.harmony.security.asn1.ASN1Oid;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.asn1.ObjectIdentifier;
import org.apache.harmony.security.utils.AlgNameMapper;

public final class AlgorithmIdentifier {
    public static final ASN1Sequence ASN1 = new ASN1Sequence(new ASN1Type[]{ASN1Oid.getInstance(), ASN1Any.getInstance()}) {
        {
            setOptional(1);
        }

        @Override
        protected Object getDecodedObject(BerInputStream in) {
            Object[] values = (Object[]) in.content;
            return new AlgorithmIdentifier(ObjectIdentifier.toString((int[]) values[0]), (byte[]) values[1]);
        }

        @Override
        protected void getValues(Object object, Object[] values) {
            AlgorithmIdentifier aID = (AlgorithmIdentifier) object;
            values[0] = ObjectIdentifier.toIntArray(aID.getAlgorithm());
            values[1] = aID.getParameters();
        }
    };
    private String algorithm;
    private String algorithmName;
    private byte[] encoding;
    private byte[] parameters;

    public AlgorithmIdentifier(String algorithm) {
        this(algorithm, null, null);
    }

    public AlgorithmIdentifier(String algorithm, byte[] parameters) {
        this(algorithm, parameters, null);
    }

    private AlgorithmIdentifier(String algorithm, byte[] parameters, byte[] encoding) {
        this.algorithm = algorithm;
        this.parameters = parameters;
        this.encoding = encoding;
    }

    public AlgorithmIdentifier(String algorithm, String algorithmName) {
        this(algorithm, null, null);
        this.algorithmName = algorithmName;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public String getAlgorithmName() {
        if (this.algorithmName == null) {
            this.algorithmName = AlgNameMapper.map2AlgName(this.algorithm);
            if (this.algorithmName == null) {
                this.algorithmName = this.algorithm;
            }
        }
        return this.algorithmName;
    }

    public byte[] getParameters() {
        return this.parameters;
    }

    public byte[] getEncoded() {
        if (this.encoding == null) {
            this.encoding = ASN1.encode(this);
        }
        return this.encoding;
    }

    public boolean equals(Object ai) {
        if (!(ai instanceof AlgorithmIdentifier)) {
            return false;
        }
        AlgorithmIdentifier algid = (AlgorithmIdentifier) ai;
        if (!this.algorithm.equals(algid.algorithm)) {
            return false;
        }
        if (this.parameters == null) {
            if (algid.parameters != null) {
                return false;
            }
        } else if (!Arrays.equals(this.parameters, algid.parameters)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return (this.parameters != null ? Arrays.hashCode(this.parameters) : 0) + (this.algorithm.hashCode() * 37);
    }

    public void dumpValue(StringBuilder sb) {
        sb.append(getAlgorithmName());
        if (this.parameters == null) {
            sb.append(", no params, ");
        } else {
            sb.append(", params unparsed, ");
        }
        sb.append("OID = ");
        sb.append(getAlgorithm());
    }
}
