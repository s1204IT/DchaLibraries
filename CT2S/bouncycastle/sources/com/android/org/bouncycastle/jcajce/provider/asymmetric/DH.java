package com.android.org.bouncycastle.jcajce.provider.asymmetric;

import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.dh.KeyFactorySpi;
import com.android.org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import com.android.org.bouncycastle.jcajce.provider.util.AsymmetricAlgorithmProvider;
import com.android.org.bouncycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;

public class DH {
    private static final String PREFIX = "com.android.org.bouncycastle.jcajce.provider.asymmetric.dh.";

    public static class Mappings extends AsymmetricAlgorithmProvider {
        @Override
        public void configure(ConfigurableProvider provider) {
            provider.addAlgorithm("KeyPairGenerator.DH", "com.android.org.bouncycastle.jcajce.provider.asymmetric.dh.KeyPairGeneratorSpi");
            provider.addAlgorithm("Alg.Alias.KeyPairGenerator.DIFFIEHELLMAN", "DH");
            provider.addAlgorithm("KeyAgreement.DH", "com.android.org.bouncycastle.jcajce.provider.asymmetric.dh.KeyAgreementSpi");
            provider.addAlgorithm("Alg.Alias.KeyAgreement.DIFFIEHELLMAN", "DH");
            provider.addAlgorithm("KeyFactory.DH", "com.android.org.bouncycastle.jcajce.provider.asymmetric.dh.KeyFactorySpi");
            provider.addAlgorithm("Alg.Alias.KeyFactory.DIFFIEHELLMAN", "DH");
            AsymmetricKeyInfoConverter keyFact = new KeyFactorySpi();
            registerOid(provider, PKCSObjectIdentifiers.dhKeyAgreement, "DH", keyFact);
            provider.addAlgorithm("AlgorithmParameters.DH", "com.android.org.bouncycastle.jcajce.provider.asymmetric.dh.AlgorithmParametersSpi");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.DIFFIEHELLMAN", "DH");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator.DIFFIEHELLMAN", "DH");
            provider.addAlgorithm("AlgorithmParameterGenerator.DH", "com.android.org.bouncycastle.jcajce.provider.asymmetric.dh.AlgorithmParameterGeneratorSpi");
        }
    }
}
