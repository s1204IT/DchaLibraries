package com.android.org.bouncycastle.jcajce.provider.asymmetric.util;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.nist.NISTNamedCurves;
import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.org.bouncycastle.asn1.sec.SECNamedCurves;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.asn1.x9.X962NamedCurves;
import com.android.org.bouncycastle.asn1.x9.X9ECParameters;
import com.android.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import com.android.org.bouncycastle.crypto.params.ECDomainParameters;
import com.android.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import com.android.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import com.android.org.bouncycastle.jce.interfaces.ECPrivateKey;
import com.android.org.bouncycastle.jce.interfaces.ECPublicKey;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.org.bouncycastle.jce.spec.ECParameterSpec;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ECUtil {
    static int[] convertMidTerms(int[] k) {
        int[] res = new int[3];
        if (k.length == 1) {
            res[0] = k[0];
        } else {
            if (k.length != 3) {
                throw new IllegalArgumentException("Only Trinomials and pentanomials supported");
            }
            if (k[0] < k[1] && k[0] < k[2]) {
                res[0] = k[0];
                if (k[1] < k[2]) {
                    res[1] = k[1];
                    res[2] = k[2];
                } else {
                    res[1] = k[2];
                    res[2] = k[1];
                }
            } else if (k[1] < k[2]) {
                res[0] = k[1];
                if (k[0] < k[2]) {
                    res[1] = k[0];
                    res[2] = k[2];
                } else {
                    res[1] = k[2];
                    res[2] = k[0];
                }
            } else {
                res[0] = k[2];
                if (k[0] < k[1]) {
                    res[1] = k[0];
                    res[2] = k[1];
                } else {
                    res[1] = k[1];
                    res[2] = k[0];
                }
            }
        }
        return res;
    }

    public static AsymmetricKeyParameter generatePublicKeyParameter(PublicKey key) throws InvalidKeyException {
        if (key instanceof ECPublicKey) {
            ECPublicKey k = (ECPublicKey) key;
            ECParameterSpec s = k.getParameters();
            if (s == null) {
                ECParameterSpec s2 = BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();
                return new ECPublicKeyParameters(((BCECPublicKey) k).engineGetQ(), new ECDomainParameters(s2.getCurve(), s2.getG(), s2.getN(), s2.getH(), s2.getSeed()));
            }
            return new ECPublicKeyParameters(k.getQ(), new ECDomainParameters(s.getCurve(), s.getG(), s.getN(), s.getH(), s.getSeed()));
        }
        if (key instanceof java.security.interfaces.ECPublicKey) {
            java.security.interfaces.ECPublicKey pubKey = (java.security.interfaces.ECPublicKey) key;
            ECParameterSpec s3 = EC5Util.convertSpec(pubKey.getParams(), false);
            return new ECPublicKeyParameters(EC5Util.convertPoint(pubKey.getParams(), pubKey.getW(), false), new ECDomainParameters(s3.getCurve(), s3.getG(), s3.getN(), s3.getH(), s3.getSeed()));
        }
        try {
            byte[] bytes = key.getEncoded();
            if (bytes == null) {
                throw new InvalidKeyException("no encoding for EC public key");
            }
            PublicKey publicKey = BouncyCastleProvider.getPublicKey(SubjectPublicKeyInfo.getInstance(bytes));
            if (publicKey instanceof java.security.interfaces.ECPublicKey) {
                return generatePublicKeyParameter(publicKey);
            }
            throw new InvalidKeyException("cannot identify EC public key.");
        } catch (Exception e) {
            throw new InvalidKeyException("cannot identify EC public key: " + e.toString());
        }
    }

    public static AsymmetricKeyParameter generatePrivateKeyParameter(PrivateKey key) throws InvalidKeyException {
        if (key instanceof ECPrivateKey) {
            ECPrivateKey k = (ECPrivateKey) key;
            ECParameterSpec s = k.getParameters();
            if (s == null) {
                s = BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();
            }
            return new ECPrivateKeyParameters(k.getD(), new ECDomainParameters(s.getCurve(), s.getG(), s.getN(), s.getH(), s.getSeed()));
        }
        if (key instanceof java.security.interfaces.ECPrivateKey) {
            java.security.interfaces.ECPrivateKey privKey = (java.security.interfaces.ECPrivateKey) key;
            ECParameterSpec s2 = EC5Util.convertSpec(privKey.getParams(), false);
            return new ECPrivateKeyParameters(privKey.getS(), new ECDomainParameters(s2.getCurve(), s2.getG(), s2.getN(), s2.getH(), s2.getSeed()));
        }
        try {
            byte[] bytes = key.getEncoded();
            if (bytes == null) {
                throw new InvalidKeyException("no encoding for EC private key");
            }
            PrivateKey privateKey = BouncyCastleProvider.getPrivateKey(PrivateKeyInfo.getInstance(bytes));
            if (privateKey instanceof java.security.interfaces.ECPrivateKey) {
                return generatePrivateKeyParameter(privateKey);
            }
            throw new InvalidKeyException("can't identify EC private key.");
        } catch (Exception e) {
            throw new InvalidKeyException("cannot identify EC private key: " + e.toString());
        }
    }

    public static ASN1ObjectIdentifier getNamedCurveOid(String name) {
        ASN1ObjectIdentifier oid = X962NamedCurves.getOID(name);
        if (oid == null) {
            ASN1ObjectIdentifier oid2 = SECNamedCurves.getOID(name);
            if (oid2 == null) {
                return NISTNamedCurves.getOID(name);
            }
            return oid2;
        }
        return oid;
    }

    public static X9ECParameters getNamedCurveByOid(ASN1ObjectIdentifier oid) {
        X9ECParameters params = X962NamedCurves.getByOID(oid);
        if (params == null) {
            X9ECParameters params2 = SECNamedCurves.getByOID(oid);
            if (params2 == null) {
                return NISTNamedCurves.getByOID(oid);
            }
            return params2;
        }
        return params;
    }

    public static String getCurveName(ASN1ObjectIdentifier oid) {
        String name = X962NamedCurves.getName(oid);
        if (name == null) {
            String name2 = SECNamedCurves.getName(oid);
            if (name2 == null) {
                return NISTNamedCurves.getName(oid);
            }
            return name2;
        }
        return name;
    }
}
