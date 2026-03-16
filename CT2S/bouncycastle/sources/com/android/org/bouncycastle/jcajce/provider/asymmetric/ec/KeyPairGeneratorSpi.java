package com.android.org.bouncycastle.jcajce.provider.asymmetric.ec;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.x9.ECNamedCurveTable;
import com.android.org.bouncycastle.asn1.x9.X9ECParameters;
import com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.android.org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import com.android.org.bouncycastle.crypto.params.ECDomainParameters;
import com.android.org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import com.android.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import com.android.org.bouncycastle.jcajce.provider.config.ProviderConfiguration;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.org.bouncycastle.jce.spec.ECNamedCurveGenParameterSpec;
import com.android.org.bouncycastle.jce.spec.ECNamedCurveSpec;
import com.android.org.bouncycastle.jce.spec.ECParameterSpec;
import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECPoint;
import com.android.org.bouncycastle.util.Integers;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.Hashtable;

public abstract class KeyPairGeneratorSpi extends KeyPairGenerator {
    public KeyPairGeneratorSpi(String algorithmName) {
        super(algorithmName);
    }

    public static class EC extends KeyPairGeneratorSpi {
        private static Hashtable ecParameters = new Hashtable();
        String algorithm;
        int certainty;
        ProviderConfiguration configuration;
        Object ecParams;
        ECKeyPairGenerator engine;
        boolean initialised;
        ECKeyGenerationParameters param;
        SecureRandom random;
        int strength;

        static {
            ecParameters.put(Integers.valueOf(192), new ECGenParameterSpec("prime192v1"));
            ecParameters.put(Integers.valueOf(239), new ECGenParameterSpec("prime239v1"));
            ecParameters.put(Integers.valueOf(256), new ECGenParameterSpec("prime256v1"));
            ecParameters.put(Integers.valueOf(224), new ECGenParameterSpec("P-224"));
            ecParameters.put(Integers.valueOf(384), new ECGenParameterSpec("P-384"));
            ecParameters.put(Integers.valueOf(521), new ECGenParameterSpec("P-521"));
        }

        public EC() {
            super("EC");
            this.engine = new ECKeyPairGenerator();
            this.ecParams = null;
            this.strength = 239;
            this.certainty = 50;
            this.random = new SecureRandom();
            this.initialised = false;
            this.algorithm = "EC";
            this.configuration = BouncyCastleProvider.CONFIGURATION;
        }

        public EC(String algorithm, ProviderConfiguration configuration) {
            super(algorithm);
            this.engine = new ECKeyPairGenerator();
            this.ecParams = null;
            this.strength = 239;
            this.certainty = 50;
            this.random = new SecureRandom();
            this.initialised = false;
            this.algorithm = algorithm;
            this.configuration = configuration;
        }

        @Override
        public void initialize(int strength, SecureRandom random) {
            this.strength = strength;
            if (random != null) {
                this.random = random;
            }
            ECGenParameterSpec ecParams = (ECGenParameterSpec) ecParameters.get(Integers.valueOf(strength));
            if (ecParams != null) {
                try {
                    initialize(ecParams, random);
                    return;
                } catch (InvalidAlgorithmParameterException e) {
                    throw new InvalidParameterException("key size not configurable.");
                }
            }
            throw new InvalidParameterException("unknown key size.");
        }

        @Override
        public void initialize(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
            String curveName;
            if (random == null) {
                random = this.random;
            }
            if (params instanceof ECParameterSpec) {
                ECParameterSpec p = (ECParameterSpec) params;
                this.ecParams = params;
                this.param = new ECKeyGenerationParameters(new ECDomainParameters(p.getCurve(), p.getG(), p.getN()), random);
                this.engine.init(this.param);
                this.initialised = true;
                return;
            }
            if (params instanceof java.security.spec.ECParameterSpec) {
                java.security.spec.ECParameterSpec p2 = (java.security.spec.ECParameterSpec) params;
                this.ecParams = params;
                ECCurve curve = EC5Util.convertCurve(p2.getCurve());
                ECPoint g = EC5Util.convertPoint(curve, p2.getGenerator(), false);
                this.param = new ECKeyGenerationParameters(new ECDomainParameters(curve, g, p2.getOrder(), BigInteger.valueOf(p2.getCofactor())), random);
                this.engine.init(this.param);
                this.initialised = true;
                return;
            }
            if ((params instanceof ECGenParameterSpec) || (params instanceof ECNamedCurveGenParameterSpec)) {
                if (params instanceof ECGenParameterSpec) {
                    curveName = ((ECGenParameterSpec) params).getName();
                } else {
                    curveName = ((ECNamedCurveGenParameterSpec) params).getName();
                }
                X9ECParameters ecP = ECNamedCurveTable.getByName(curveName);
                if (ecP == null) {
                    try {
                        ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier(curveName);
                        ecP = ECNamedCurveTable.getByOID(oid);
                        if (ecP == null) {
                            throw new InvalidAlgorithmParameterException("unknown curve OID: " + curveName);
                        }
                    } catch (IllegalArgumentException e) {
                        throw new InvalidAlgorithmParameterException("unknown curve name: " + curveName);
                    }
                }
                this.ecParams = new ECNamedCurveSpec(curveName, ecP.getCurve(), ecP.getG(), ecP.getN(), ecP.getH(), null);
                java.security.spec.ECParameterSpec p3 = (java.security.spec.ECParameterSpec) this.ecParams;
                ECCurve curve2 = EC5Util.convertCurve(p3.getCurve());
                ECPoint g2 = EC5Util.convertPoint(curve2, p3.getGenerator(), false);
                this.param = new ECKeyGenerationParameters(new ECDomainParameters(curve2, g2, p3.getOrder(), BigInteger.valueOf(p3.getCofactor())), random);
                this.engine.init(this.param);
                this.initialised = true;
                return;
            }
            if (params == null && this.configuration.getEcImplicitlyCa() != null) {
                ECParameterSpec p4 = this.configuration.getEcImplicitlyCa();
                this.ecParams = params;
                this.param = new ECKeyGenerationParameters(new ECDomainParameters(p4.getCurve(), p4.getG(), p4.getN()), random);
                this.engine.init(this.param);
                this.initialised = true;
                return;
            }
            if (params == null && this.configuration.getEcImplicitlyCa() == null) {
                throw new InvalidAlgorithmParameterException("null parameter passed but no implicitCA set");
            }
            throw new InvalidAlgorithmParameterException("parameter object not a ECParameterSpec");
        }

        @Override
        public KeyPair generateKeyPair() {
            if (!this.initialised) {
                initialize(this.strength, new SecureRandom());
            }
            AsymmetricCipherKeyPair pair = this.engine.generateKeyPair();
            ECPublicKeyParameters pub = (ECPublicKeyParameters) pair.getPublic();
            ECPrivateKeyParameters priv = (ECPrivateKeyParameters) pair.getPrivate();
            if (this.ecParams instanceof ECParameterSpec) {
                ECParameterSpec p = (ECParameterSpec) this.ecParams;
                BCECPublicKey pubKey = new BCECPublicKey(this.algorithm, pub, p, this.configuration);
                return new KeyPair(pubKey, new BCECPrivateKey(this.algorithm, priv, pubKey, p, this.configuration));
            }
            if (this.ecParams == null) {
                return new KeyPair(new BCECPublicKey(this.algorithm, pub, this.configuration), new BCECPrivateKey(this.algorithm, priv, this.configuration));
            }
            java.security.spec.ECParameterSpec p2 = (java.security.spec.ECParameterSpec) this.ecParams;
            BCECPublicKey pubKey2 = new BCECPublicKey(this.algorithm, pub, p2, this.configuration);
            return new KeyPair(pubKey2, new BCECPrivateKey(this.algorithm, priv, pubKey2, p2, this.configuration));
        }
    }

    public static class ECDSA extends EC {
        public ECDSA() {
            super("ECDSA", BouncyCastleProvider.CONFIGURATION);
        }
    }

    public static class ECDH extends EC {
        public ECDH() {
            super("ECDH", BouncyCastleProvider.CONFIGURATION);
        }
    }

    public static class ECDHC extends EC {
        public ECDHC() {
            super("ECDHC", BouncyCastleProvider.CONFIGURATION);
        }
    }

    public static class ECMQV extends EC {
        public ECMQV() {
            super("ECMQV", BouncyCastleProvider.CONFIGURATION);
        }
    }
}
