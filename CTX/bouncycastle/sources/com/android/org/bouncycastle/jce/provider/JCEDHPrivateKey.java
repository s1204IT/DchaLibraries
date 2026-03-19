package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1Encoding;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.pkcs.DHParameter;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.asn1.x9.DHDomainParameters;
import com.android.org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import com.android.org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl;
import com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Enumeration;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPrivateKeySpec;

public class JCEDHPrivateKey implements DHPrivateKey, PKCS12BagAttributeCarrier {
    static final long serialVersionUID = 311058815616901812L;
    private PKCS12BagAttributeCarrier attrCarrier = new PKCS12BagAttributeCarrierImpl();
    private DHParameterSpec dhSpec;
    private PrivateKeyInfo info;
    BigInteger x;

    protected JCEDHPrivateKey() {
    }

    JCEDHPrivateKey(DHPrivateKey dHPrivateKey) {
        this.x = dHPrivateKey.getX();
        this.dhSpec = dHPrivateKey.getParams();
    }

    JCEDHPrivateKey(DHPrivateKeySpec dHPrivateKeySpec) {
        this.x = dHPrivateKeySpec.getX();
        this.dhSpec = new DHParameterSpec(dHPrivateKeySpec.getP(), dHPrivateKeySpec.getG());
    }

    JCEDHPrivateKey(PrivateKeyInfo privateKeyInfo) throws IOException {
        ASN1Sequence aSN1Sequence = ASN1Sequence.getInstance(privateKeyInfo.getAlgorithmId().getParameters());
        ASN1Integer aSN1Integer = ASN1Integer.getInstance(privateKeyInfo.parsePrivateKey());
        ASN1ObjectIdentifier algorithm = privateKeyInfo.getAlgorithmId().getAlgorithm();
        this.info = privateKeyInfo;
        this.x = aSN1Integer.getValue();
        if (algorithm.equals(PKCSObjectIdentifiers.dhKeyAgreement)) {
            DHParameter dHParameter = DHParameter.getInstance(aSN1Sequence);
            if (dHParameter.getL() != null) {
                this.dhSpec = new DHParameterSpec(dHParameter.getP(), dHParameter.getG(), dHParameter.getL().intValue());
                return;
            } else {
                this.dhSpec = new DHParameterSpec(dHParameter.getP(), dHParameter.getG());
                return;
            }
        }
        if (algorithm.equals(X9ObjectIdentifiers.dhpublicnumber)) {
            DHDomainParameters dHDomainParameters = DHDomainParameters.getInstance(aSN1Sequence);
            this.dhSpec = new DHParameterSpec(dHDomainParameters.getP().getValue(), dHDomainParameters.getG().getValue());
        } else {
            throw new IllegalArgumentException("unknown algorithm type: " + algorithm);
        }
    }

    JCEDHPrivateKey(DHPrivateKeyParameters dHPrivateKeyParameters) {
        this.x = dHPrivateKeyParameters.getX();
        this.dhSpec = new DHParameterSpec(dHPrivateKeyParameters.getParameters().getP(), dHPrivateKeyParameters.getParameters().getG(), dHPrivateKeyParameters.getParameters().getL());
    }

    @Override
    public String getAlgorithm() {
        return "DH";
    }

    @Override
    public String getFormat() {
        return "PKCS#8";
    }

    @Override
    public byte[] getEncoded() {
        try {
            if (this.info != null) {
                return this.info.getEncoded(ASN1Encoding.DER);
            }
            return new PrivateKeyInfo(new AlgorithmIdentifier(PKCSObjectIdentifiers.dhKeyAgreement, new DHParameter(this.dhSpec.getP(), this.dhSpec.getG(), this.dhSpec.getL())), new ASN1Integer(getX())).getEncoded(ASN1Encoding.DER);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public DHParameterSpec getParams() {
        return this.dhSpec;
    }

    @Override
    public BigInteger getX() {
        return this.x;
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        this.x = (BigInteger) objectInputStream.readObject();
        this.dhSpec = new DHParameterSpec((BigInteger) objectInputStream.readObject(), (BigInteger) objectInputStream.readObject(), objectInputStream.readInt());
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(getX());
        objectOutputStream.writeObject(this.dhSpec.getP());
        objectOutputStream.writeObject(this.dhSpec.getG());
        objectOutputStream.writeInt(this.dhSpec.getL());
    }

    @Override
    public void setBagAttribute(ASN1ObjectIdentifier aSN1ObjectIdentifier, ASN1Encodable aSN1Encodable) {
        this.attrCarrier.setBagAttribute(aSN1ObjectIdentifier, aSN1Encodable);
    }

    @Override
    public ASN1Encodable getBagAttribute(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return this.attrCarrier.getBagAttribute(aSN1ObjectIdentifier);
    }

    @Override
    public Enumeration getBagAttributeKeys() {
        return this.attrCarrier.getBagAttributeKeys();
    }
}
