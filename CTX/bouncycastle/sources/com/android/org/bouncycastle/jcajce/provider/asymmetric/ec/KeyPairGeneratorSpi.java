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
    public KeyPairGeneratorSpi(String str) {
        super(str);
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
            this.strength = 256;
            this.certainty = 50;
            this.random = new SecureRandom();
            this.initialised = false;
            this.algorithm = "EC";
            this.configuration = BouncyCastleProvider.CONFIGURATION;
        }

        public EC(String str, ProviderConfiguration providerConfiguration) {
            super(str);
            this.engine = new ECKeyPairGenerator();
            this.ecParams = null;
            this.strength = 256;
            this.certainty = 50;
            this.random = new SecureRandom();
            this.initialised = false;
            this.algorithm = str;
            this.configuration = providerConfiguration;
        }

        @Override
        public void initialize(int i, SecureRandom secureRandom) {
            this.strength = i;
            if (secureRandom != null) {
                this.random = secureRandom;
            }
            ECGenParameterSpec eCGenParameterSpec = (ECGenParameterSpec) ecParameters.get(Integers.valueOf(i));
            if (eCGenParameterSpec == null) {
                throw new InvalidParameterException("unknown key size.");
            }
            try {
                initialize(eCGenParameterSpec, secureRandom);
            } catch (InvalidAlgorithmParameterException e) {
                throw new InvalidParameterException("key size not configurable.");
            }
        }

        @Override
        public void initialize(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            if (secureRandom == null) {
                secureRandom = this.random;
            }
            if (algorithmParameterSpec == null) {
                ECParameterSpec ecImplicitlyCa = this.configuration.getEcImplicitlyCa();
                if (ecImplicitlyCa == null) {
                    throw new InvalidAlgorithmParameterException("null parameter passed but no implicitCA set");
                }
                this.ecParams = null;
                this.param = createKeyGenParamsBC(ecImplicitlyCa, secureRandom);
            } else if (algorithmParameterSpec instanceof ECParameterSpec) {
                this.ecParams = algorithmParameterSpec;
                this.param = createKeyGenParamsBC((ECParameterSpec) algorithmParameterSpec, secureRandom);
            } else if (algorithmParameterSpec instanceof java.security.spec.ECParameterSpec) {
                this.ecParams = algorithmParameterSpec;
                this.param = createKeyGenParamsJCE((java.security.spec.ECParameterSpec) algorithmParameterSpec, secureRandom);
            } else if (algorithmParameterSpec instanceof ECGenParameterSpec) {
                initializeNamedCurve(((ECGenParameterSpec) algorithmParameterSpec).getName(), secureRandom);
            } else if (algorithmParameterSpec instanceof ECNamedCurveGenParameterSpec) {
                initializeNamedCurve(((ECNamedCurveGenParameterSpec) algorithmParameterSpec).getName(), secureRandom);
            } else {
                throw new InvalidAlgorithmParameterException("parameter object not a ECParameterSpec");
            }
            this.engine.init(this.param);
            this.initialised = true;
        }

        @Override
        public KeyPair generateKeyPair() {
            if (!this.initialised) {
                initialize(this.strength, new SecureRandom());
            }
            AsymmetricCipherKeyPair asymmetricCipherKeyPairGenerateKeyPair = this.engine.generateKeyPair();
            ECPublicKeyParameters eCPublicKeyParameters = (ECPublicKeyParameters) asymmetricCipherKeyPairGenerateKeyPair.getPublic();
            ECPrivateKeyParameters eCPrivateKeyParameters = (ECPrivateKeyParameters) asymmetricCipherKeyPairGenerateKeyPair.getPrivate();
            if (this.ecParams instanceof ECParameterSpec) {
                ECParameterSpec eCParameterSpec = (ECParameterSpec) this.ecParams;
                BCECPublicKey bCECPublicKey = new BCECPublicKey(this.algorithm, eCPublicKeyParameters, eCParameterSpec, this.configuration);
                return new KeyPair(bCECPublicKey, new BCECPrivateKey(this.algorithm, eCPrivateKeyParameters, bCECPublicKey, eCParameterSpec, this.configuration));
            }
            if (this.ecParams == null) {
                return new KeyPair(new BCECPublicKey(this.algorithm, eCPublicKeyParameters, this.configuration), new BCECPrivateKey(this.algorithm, eCPrivateKeyParameters, this.configuration));
            }
            java.security.spec.ECParameterSpec eCParameterSpec2 = (java.security.spec.ECParameterSpec) this.ecParams;
            BCECPublicKey bCECPublicKey2 = new BCECPublicKey(this.algorithm, eCPublicKeyParameters, eCParameterSpec2, this.configuration);
            return new KeyPair(bCECPublicKey2, new BCECPrivateKey(this.algorithm, eCPrivateKeyParameters, bCECPublicKey2, eCParameterSpec2, this.configuration));
        }

        protected ECKeyGenerationParameters createKeyGenParamsBC(ECParameterSpec eCParameterSpec, SecureRandom secureRandom) {
            return new ECKeyGenerationParameters(new ECDomainParameters(eCParameterSpec.getCurve(), eCParameterSpec.getG(), eCParameterSpec.getN(), eCParameterSpec.getH()), secureRandom);
        }

        protected ECKeyGenerationParameters createKeyGenParamsJCE(java.security.spec.ECParameterSpec eCParameterSpec, SecureRandom secureRandom) {
            ECCurve eCCurveConvertCurve = EC5Util.convertCurve(eCParameterSpec.getCurve());
            return new ECKeyGenerationParameters(new ECDomainParameters(eCCurveConvertCurve, EC5Util.convertPoint(eCCurveConvertCurve, eCParameterSpec.getGenerator(), false), eCParameterSpec.getOrder(), BigInteger.valueOf(eCParameterSpec.getCofactor())), secureRandom);
        }

        protected ECNamedCurveSpec createNamedCurveSpec(String str) throws InvalidAlgorithmParameterException {
            X9ECParameters domainParametersFromName = ECUtils.getDomainParametersFromName(str);
            if (domainParametersFromName == null) {
                try {
                    domainParametersFromName = ECNamedCurveTable.getByOID(new ASN1ObjectIdentifier(str));
                    if (domainParametersFromName == null && (domainParametersFromName = (X9ECParameters) this.configuration.getAdditionalECParameters().get(new ASN1ObjectIdentifier(str))) == null) {
                        throw new InvalidAlgorithmParameterException("unknown curve OID: " + str);
                    }
                } catch (IllegalArgumentException e) {
                    throw new InvalidAlgorithmParameterException("unknown curve name: " + str);
                }
            }
            return new ECNamedCurveSpec(str, domainParametersFromName.getCurve(), domainParametersFromName.getG(), domainParametersFromName.getN(), domainParametersFromName.getH(), null);
        }

        protected void initializeNamedCurve(String str, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            ECNamedCurveSpec eCNamedCurveSpecCreateNamedCurveSpec = createNamedCurveSpec(str);
            this.ecParams = eCNamedCurveSpecCreateNamedCurveSpec;
            this.param = createKeyGenParamsJCE(eCNamedCurveSpecCreateNamedCurveSpec, secureRandom);
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
