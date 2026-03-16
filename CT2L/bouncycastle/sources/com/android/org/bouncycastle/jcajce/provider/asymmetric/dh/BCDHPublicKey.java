package com.android.org.bouncycastle.jcajce.provider.asymmetric.dh;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.pkcs.DHParameter;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.asn1.x9.DHDomainParameters;
import com.android.org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import com.android.org.bouncycastle.crypto.params.DHPublicKeyParameters;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.KeyUtil;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

public class BCDHPublicKey implements DHPublicKey {
    static final long serialVersionUID = -216691575254424324L;
    private transient DHParameterSpec dhSpec;
    private transient SubjectPublicKeyInfo info;
    private BigInteger y;

    BCDHPublicKey(DHPublicKeySpec spec) {
        this.y = spec.getY();
        this.dhSpec = new DHParameterSpec(spec.getP(), spec.getG());
    }

    BCDHPublicKey(DHPublicKey key) {
        this.y = key.getY();
        this.dhSpec = key.getParams();
    }

    BCDHPublicKey(DHPublicKeyParameters params) {
        this.y = params.getY();
        this.dhSpec = new DHParameterSpec(params.getParameters().getP(), params.getParameters().getG(), params.getParameters().getL());
    }

    BCDHPublicKey(BigInteger y, DHParameterSpec dhSpec) {
        this.y = y;
        this.dhSpec = dhSpec;
    }

    public BCDHPublicKey(SubjectPublicKeyInfo info) {
        this.info = info;
        try {
            ASN1Integer derY = (ASN1Integer) info.parsePublicKey();
            this.y = derY.getValue();
            ASN1Sequence seq = ASN1Sequence.getInstance(info.getAlgorithm().getParameters());
            ASN1ObjectIdentifier id = info.getAlgorithm().getAlgorithm();
            if (id.equals(PKCSObjectIdentifiers.dhKeyAgreement) || isPKCSParam(seq)) {
                DHParameter params = DHParameter.getInstance(seq);
                if (params.getL() != null) {
                    this.dhSpec = new DHParameterSpec(params.getP(), params.getG(), params.getL().intValue());
                    return;
                } else {
                    this.dhSpec = new DHParameterSpec(params.getP(), params.getG());
                    return;
                }
            }
            if (id.equals(X9ObjectIdentifiers.dhpublicnumber)) {
                DHDomainParameters params2 = DHDomainParameters.getInstance(seq);
                this.dhSpec = new DHParameterSpec(params2.getP().getValue(), params2.getG().getValue());
                return;
            }
            throw new IllegalArgumentException("unknown algorithm type: " + id);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid info structure in DH public key");
        }
    }

    @Override
    public String getAlgorithm() {
        return "DH";
    }

    @Override
    public String getFormat() {
        return "X.509";
    }

    @Override
    public byte[] getEncoded() {
        return this.info != null ? KeyUtil.getEncodedSubjectPublicKeyInfo(this.info) : KeyUtil.getEncodedSubjectPublicKeyInfo(new AlgorithmIdentifier(PKCSObjectIdentifiers.dhKeyAgreement, (ASN1Encodable) new DHParameter(this.dhSpec.getP(), this.dhSpec.getG(), this.dhSpec.getL()).toASN1Primitive()), new ASN1Integer(this.y));
    }

    @Override
    public DHParameterSpec getParams() {
        return this.dhSpec;
    }

    @Override
    public BigInteger getY() {
        return this.y;
    }

    private boolean isPKCSParam(ASN1Sequence seq) {
        if (seq.size() == 2) {
            return true;
        }
        if (seq.size() > 3) {
            return false;
        }
        ASN1Integer l = ASN1Integer.getInstance(seq.getObjectAt(2));
        ASN1Integer p = ASN1Integer.getInstance(seq.getObjectAt(0));
        return l.getValue().compareTo(BigInteger.valueOf((long) p.getValue().bitLength())) <= 0;
    }

    public int hashCode() {
        return ((getY().hashCode() ^ getParams().getG().hashCode()) ^ getParams().getP().hashCode()) ^ getParams().getL();
    }

    public boolean equals(Object o) {
        if (!(o instanceof DHPublicKey)) {
            return false;
        }
        DHPublicKey other = (DHPublicKey) o;
        return getY().equals(other.getY()) && getParams().getG().equals(other.getParams().getG()) && getParams().getP().equals(other.getParams().getP()) && getParams().getL() == other.getParams().getL();
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        this.dhSpec = new DHParameterSpec((BigInteger) in.readObject(), (BigInteger) in.readObject(), in.readInt());
        this.info = null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(this.dhSpec.getP());
        out.writeObject(this.dhSpec.getG());
        out.writeInt(this.dhSpec.getL());
    }
}
