package com.android.org.bouncycastle.crypto.util;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.DHParameter;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.RSAPublicKey;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.asn1.x509.DSAParameter;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import com.android.org.bouncycastle.asn1.x9.DHPublicKey;
import com.android.org.bouncycastle.asn1.x9.DomainParameters;
import com.android.org.bouncycastle.asn1.x9.ECNamedCurveTable;
import com.android.org.bouncycastle.asn1.x9.ValidationParams;
import com.android.org.bouncycastle.asn1.x9.X962Parameters;
import com.android.org.bouncycastle.asn1.x9.X9ECParameters;
import com.android.org.bouncycastle.asn1.x9.X9ECPoint;
import com.android.org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import com.android.org.bouncycastle.crypto.ec.CustomNamedCurves;
import com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import com.android.org.bouncycastle.crypto.params.DHParameters;
import com.android.org.bouncycastle.crypto.params.DHPublicKeyParameters;
import com.android.org.bouncycastle.crypto.params.DHValidationParameters;
import com.android.org.bouncycastle.crypto.params.DSAParameters;
import com.android.org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import com.android.org.bouncycastle.crypto.params.ECDomainParameters;
import com.android.org.bouncycastle.crypto.params.ECNamedDomainParameters;
import com.android.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.android.org.bouncycastle.crypto.params.RSAKeyParameters;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

public class PublicKeyFactory {
    public static AsymmetricKeyParameter createKey(byte[] bArr) throws IOException {
        return createKey(SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(bArr)));
    }

    public static AsymmetricKeyParameter createKey(InputStream inputStream) throws IOException {
        return createKey(SubjectPublicKeyInfo.getInstance(new ASN1InputStream(inputStream).readObject()));
    }

    public static AsymmetricKeyParameter createKey(SubjectPublicKeyInfo subjectPublicKeyInfo) throws IOException {
        X9ECParameters x9ECParameters;
        ECDomainParameters eCDomainParameters;
        AlgorithmIdentifier algorithm = subjectPublicKeyInfo.getAlgorithm();
        if (algorithm.getAlgorithm().equals(PKCSObjectIdentifiers.rsaEncryption) || algorithm.getAlgorithm().equals(X509ObjectIdentifiers.id_ea_rsa)) {
            RSAPublicKey rSAPublicKey = RSAPublicKey.getInstance(subjectPublicKeyInfo.parsePublicKey());
            return new RSAKeyParameters(false, rSAPublicKey.getModulus(), rSAPublicKey.getPublicExponent());
        }
        DSAParameters dSAParameters = null;
        if (algorithm.getAlgorithm().equals(X9ObjectIdentifiers.dhpublicnumber)) {
            BigInteger y = DHPublicKey.getInstance(subjectPublicKeyInfo.parsePublicKey()).getY();
            DomainParameters domainParameters = DomainParameters.getInstance(algorithm.getParameters());
            BigInteger p = domainParameters.getP();
            BigInteger g = domainParameters.getG();
            BigInteger q = domainParameters.getQ();
            BigInteger j = domainParameters.getJ() != null ? domainParameters.getJ() : null;
            ValidationParams validationParams = domainParameters.getValidationParams();
            return new DHPublicKeyParameters(y, new DHParameters(p, g, q, j, validationParams != null ? new DHValidationParameters(validationParams.getSeed(), validationParams.getPgenCounter().intValue()) : null));
        }
        if (algorithm.getAlgorithm().equals(PKCSObjectIdentifiers.dhKeyAgreement)) {
            DHParameter dHParameter = DHParameter.getInstance(algorithm.getParameters());
            ASN1Integer aSN1Integer = (ASN1Integer) subjectPublicKeyInfo.parsePublicKey();
            BigInteger l = dHParameter.getL();
            return new DHPublicKeyParameters(aSN1Integer.getValue(), new DHParameters(dHParameter.getP(), dHParameter.getG(), null, l != null ? l.intValue() : 0));
        }
        if (algorithm.getAlgorithm().equals(X9ObjectIdentifiers.id_dsa) || algorithm.getAlgorithm().equals(OIWObjectIdentifiers.dsaWithSHA1)) {
            ASN1Integer aSN1Integer2 = (ASN1Integer) subjectPublicKeyInfo.parsePublicKey();
            ASN1Encodable parameters = algorithm.getParameters();
            if (parameters != null) {
                DSAParameter dSAParameter = DSAParameter.getInstance(parameters.toASN1Primitive());
                dSAParameters = new DSAParameters(dSAParameter.getP(), dSAParameter.getQ(), dSAParameter.getG());
            }
            return new DSAPublicKeyParameters(aSN1Integer2.getValue(), dSAParameters);
        }
        if (algorithm.getAlgorithm().equals(X9ObjectIdentifiers.id_ecPublicKey)) {
            X962Parameters x962Parameters = X962Parameters.getInstance(algorithm.getParameters());
            if (x962Parameters.isNamedCurve()) {
                ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) x962Parameters.getParameters();
                x9ECParameters = CustomNamedCurves.getByOID(aSN1ObjectIdentifier);
                if (x9ECParameters == null) {
                    x9ECParameters = ECNamedCurveTable.getByOID(aSN1ObjectIdentifier);
                }
                eCDomainParameters = new ECNamedDomainParameters(aSN1ObjectIdentifier, x9ECParameters.getCurve(), x9ECParameters.getG(), x9ECParameters.getN(), x9ECParameters.getH(), x9ECParameters.getSeed());
            } else {
                x9ECParameters = X9ECParameters.getInstance(x962Parameters.getParameters());
                eCDomainParameters = new ECDomainParameters(x9ECParameters.getCurve(), x9ECParameters.getG(), x9ECParameters.getN(), x9ECParameters.getH(), x9ECParameters.getSeed());
            }
            return new ECPublicKeyParameters(new X9ECPoint(x9ECParameters.getCurve(), new DEROctetString(subjectPublicKeyInfo.getPublicKeyData().getBytes())).getPoint(), eCDomainParameters);
        }
        throw new RuntimeException("algorithm identifier in key not recognised");
    }
}
