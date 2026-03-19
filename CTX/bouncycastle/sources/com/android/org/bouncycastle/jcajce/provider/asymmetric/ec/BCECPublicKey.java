package com.android.org.bouncycastle.jcajce.provider.asymmetric.ec;

import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.asn1.x9.X962Parameters;
import com.android.org.bouncycastle.asn1.x9.X9ECPoint;
import com.android.org.bouncycastle.asn1.x9.X9IntegerConverter;
import com.android.org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import com.android.org.bouncycastle.crypto.params.ECDomainParameters;
import com.android.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.KeyUtil;
import com.android.org.bouncycastle.jcajce.provider.config.ProviderConfiguration;
import com.android.org.bouncycastle.jce.interfaces.ECPointEncoder;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.util.Strings;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;

public class BCECPublicKey implements ECPublicKey, com.android.org.bouncycastle.jce.interfaces.ECPublicKey, ECPointEncoder {
    static final long serialVersionUID = 2422789860422731812L;
    private String algorithm;
    private transient ProviderConfiguration configuration;
    private transient ECPublicKeyParameters ecPublicKey;
    private transient ECParameterSpec ecSpec;
    private boolean withCompression;

    public BCECPublicKey(String str, BCECPublicKey bCECPublicKey) {
        this.algorithm = "EC";
        this.algorithm = str;
        this.ecPublicKey = bCECPublicKey.ecPublicKey;
        this.ecSpec = bCECPublicKey.ecSpec;
        this.withCompression = bCECPublicKey.withCompression;
        this.configuration = bCECPublicKey.configuration;
    }

    public BCECPublicKey(String str, ECPublicKeySpec eCPublicKeySpec, ProviderConfiguration providerConfiguration) {
        this.algorithm = "EC";
        this.algorithm = str;
        this.ecSpec = eCPublicKeySpec.getParams();
        this.ecPublicKey = new ECPublicKeyParameters(EC5Util.convertPoint(this.ecSpec, eCPublicKeySpec.getW(), false), EC5Util.getDomainParameters(providerConfiguration, eCPublicKeySpec.getParams()));
        this.configuration = providerConfiguration;
    }

    public BCECPublicKey(String str, com.android.org.bouncycastle.jce.spec.ECPublicKeySpec eCPublicKeySpec, ProviderConfiguration providerConfiguration) {
        this.algorithm = "EC";
        this.algorithm = str;
        if (eCPublicKeySpec.getParams() != null) {
            EllipticCurve ellipticCurveConvertCurve = EC5Util.convertCurve(eCPublicKeySpec.getParams().getCurve(), eCPublicKeySpec.getParams().getSeed());
            this.ecPublicKey = new ECPublicKeyParameters(eCPublicKeySpec.getQ(), ECUtil.getDomainParameters(providerConfiguration, eCPublicKeySpec.getParams()));
            this.ecSpec = EC5Util.convertSpec(ellipticCurveConvertCurve, eCPublicKeySpec.getParams());
        } else {
            this.ecPublicKey = new ECPublicKeyParameters(providerConfiguration.getEcImplicitlyCa().getCurve().createPoint(eCPublicKeySpec.getQ().getAffineXCoord().toBigInteger(), eCPublicKeySpec.getQ().getAffineYCoord().toBigInteger()), EC5Util.getDomainParameters(providerConfiguration, (ECParameterSpec) null));
            this.ecSpec = null;
        }
        this.configuration = providerConfiguration;
    }

    public BCECPublicKey(String str, ECPublicKeyParameters eCPublicKeyParameters, ECParameterSpec eCParameterSpec, ProviderConfiguration providerConfiguration) {
        this.algorithm = "EC";
        ECDomainParameters parameters = eCPublicKeyParameters.getParameters();
        this.algorithm = str;
        this.ecPublicKey = eCPublicKeyParameters;
        if (eCParameterSpec == null) {
            this.ecSpec = createSpec(EC5Util.convertCurve(parameters.getCurve(), parameters.getSeed()), parameters);
        } else {
            this.ecSpec = eCParameterSpec;
        }
        this.configuration = providerConfiguration;
    }

    public BCECPublicKey(String str, ECPublicKeyParameters eCPublicKeyParameters, com.android.org.bouncycastle.jce.spec.ECParameterSpec eCParameterSpec, ProviderConfiguration providerConfiguration) {
        this.algorithm = "EC";
        ECDomainParameters parameters = eCPublicKeyParameters.getParameters();
        this.algorithm = str;
        if (eCParameterSpec == null) {
            this.ecSpec = createSpec(EC5Util.convertCurve(parameters.getCurve(), parameters.getSeed()), parameters);
        } else {
            this.ecSpec = EC5Util.convertSpec(EC5Util.convertCurve(eCParameterSpec.getCurve(), eCParameterSpec.getSeed()), eCParameterSpec);
        }
        this.ecPublicKey = eCPublicKeyParameters;
        this.configuration = providerConfiguration;
    }

    public BCECPublicKey(String str, ECPublicKeyParameters eCPublicKeyParameters, ProviderConfiguration providerConfiguration) {
        this.algorithm = "EC";
        this.algorithm = str;
        this.ecPublicKey = eCPublicKeyParameters;
        this.ecSpec = null;
        this.configuration = providerConfiguration;
    }

    public BCECPublicKey(ECPublicKey eCPublicKey, ProviderConfiguration providerConfiguration) {
        this.algorithm = "EC";
        this.algorithm = eCPublicKey.getAlgorithm();
        this.ecSpec = eCPublicKey.getParams();
        this.ecPublicKey = new ECPublicKeyParameters(EC5Util.convertPoint(this.ecSpec, eCPublicKey.getW(), false), EC5Util.getDomainParameters(providerConfiguration, eCPublicKey.getParams()));
    }

    BCECPublicKey(String str, SubjectPublicKeyInfo subjectPublicKeyInfo, ProviderConfiguration providerConfiguration) {
        this.algorithm = "EC";
        this.algorithm = str;
        this.configuration = providerConfiguration;
        populateFromPubKeyInfo(subjectPublicKeyInfo);
    }

    private ECParameterSpec createSpec(EllipticCurve ellipticCurve, ECDomainParameters eCDomainParameters) {
        return new ECParameterSpec(ellipticCurve, new ECPoint(eCDomainParameters.getG().getAffineXCoord().toBigInteger(), eCDomainParameters.getG().getAffineYCoord().toBigInteger()), eCDomainParameters.getN(), eCDomainParameters.getH().intValue());
    }

    private void populateFromPubKeyInfo(SubjectPublicKeyInfo subjectPublicKeyInfo) {
        X962Parameters x962Parameters = X962Parameters.getInstance(subjectPublicKeyInfo.getAlgorithm().getParameters());
        ECCurve curve = EC5Util.getCurve(this.configuration, x962Parameters);
        this.ecSpec = EC5Util.convertToSpec(x962Parameters, curve);
        byte[] bytes = subjectPublicKeyInfo.getPublicKeyData().getBytes();
        ASN1OctetString dEROctetString = new DEROctetString(bytes);
        if (bytes[0] == 4 && bytes[1] == bytes.length - 2 && ((bytes[2] == 2 || bytes[2] == 3) && new X9IntegerConverter().getByteLength(curve) >= bytes.length - 3)) {
            try {
                dEROctetString = (ASN1OctetString) ASN1Primitive.fromByteArray(bytes);
            } catch (IOException e) {
                throw new IllegalArgumentException("error recovering public key");
            }
        }
        this.ecPublicKey = new ECPublicKeyParameters(new X9ECPoint(curve, dEROctetString).getPoint(), ECUtil.getDomainParameters(this.configuration, x962Parameters));
    }

    @Override
    public String getAlgorithm() {
        return this.algorithm;
    }

    @Override
    public String getFormat() {
        return "X.509";
    }

    @Override
    public byte[] getEncoded() {
        return KeyUtil.getEncodedSubjectPublicKeyInfo(new SubjectPublicKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, ECUtils.getDomainParametersFromName(this.ecSpec, this.withCompression)), ASN1OctetString.getInstance(new X9ECPoint(this.ecPublicKey.getQ(), this.withCompression).toASN1Primitive()).getOctets()));
    }

    @Override
    public ECParameterSpec getParams() {
        return this.ecSpec;
    }

    @Override
    public com.android.org.bouncycastle.jce.spec.ECParameterSpec getParameters() {
        if (this.ecSpec == null) {
            return null;
        }
        return EC5Util.convertSpec(this.ecSpec, this.withCompression);
    }

    @Override
    public ECPoint getW() {
        com.android.org.bouncycastle.math.ec.ECPoint q = this.ecPublicKey.getQ();
        return new ECPoint(q.getAffineXCoord().toBigInteger(), q.getAffineYCoord().toBigInteger());
    }

    @Override
    public com.android.org.bouncycastle.math.ec.ECPoint getQ() {
        com.android.org.bouncycastle.math.ec.ECPoint q = this.ecPublicKey.getQ();
        if (this.ecSpec == null) {
            return q.getDetachedPoint();
        }
        return q;
    }

    ECPublicKeyParameters engineGetKeyParameters() {
        return this.ecPublicKey;
    }

    com.android.org.bouncycastle.jce.spec.ECParameterSpec engineGetSpec() {
        if (this.ecSpec != null) {
            return EC5Util.convertSpec(this.ecSpec, this.withCompression);
        }
        return this.configuration.getEcImplicitlyCa();
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        String strLineSeparator = Strings.lineSeparator();
        com.android.org.bouncycastle.math.ec.ECPoint q = this.ecPublicKey.getQ();
        stringBuffer.append("EC Public Key");
        stringBuffer.append(strLineSeparator);
        stringBuffer.append("            X: ");
        stringBuffer.append(q.getAffineXCoord().toBigInteger().toString(16));
        stringBuffer.append(strLineSeparator);
        stringBuffer.append("            Y: ");
        stringBuffer.append(q.getAffineYCoord().toBigInteger().toString(16));
        stringBuffer.append(strLineSeparator);
        return stringBuffer.toString();
    }

    @Override
    public void setPointFormat(String str) {
        this.withCompression = !"UNCOMPRESSED".equalsIgnoreCase(str);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BCECPublicKey)) {
            return false;
        }
        BCECPublicKey bCECPublicKey = (BCECPublicKey) obj;
        return this.ecPublicKey.getQ().equals(bCECPublicKey.ecPublicKey.getQ()) && engineGetSpec().equals(bCECPublicKey.engineGetSpec());
    }

    public int hashCode() {
        return this.ecPublicKey.getQ().hashCode() ^ engineGetSpec().hashCode();
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        byte[] bArr = (byte[]) objectInputStream.readObject();
        this.configuration = BouncyCastleProvider.CONFIGURATION;
        populateFromPubKeyInfo(SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(bArr)));
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(getEncoded());
    }
}
