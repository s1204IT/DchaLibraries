package java.security.cert;

import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import sun.security.jca.GetInstance;

public class CertPathBuilder {
    private static final String CPB_TYPE = "certpathbuilder.type";
    private final String algorithm;
    private final CertPathBuilderSpi builderSpi;
    private final Provider provider;

    protected CertPathBuilder(CertPathBuilderSpi certPathBuilderSpi, Provider provider, String str) {
        this.builderSpi = certPathBuilderSpi;
        this.provider = provider;
        this.algorithm = str;
    }

    public static CertPathBuilder getInstance(String str) throws NoSuchAlgorithmException {
        GetInstance.Instance getInstance = GetInstance.getInstance("CertPathBuilder", (Class<?>) CertPathBuilderSpi.class, str);
        return new CertPathBuilder((CertPathBuilderSpi) getInstance.impl, getInstance.provider, str);
    }

    public static CertPathBuilder getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        GetInstance.Instance getInstance = GetInstance.getInstance("CertPathBuilder", (Class<?>) CertPathBuilderSpi.class, str, str2);
        return new CertPathBuilder((CertPathBuilderSpi) getInstance.impl, getInstance.provider, str);
    }

    public static CertPathBuilder getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        GetInstance.Instance getInstance = GetInstance.getInstance("CertPathBuilder", (Class<?>) CertPathBuilderSpi.class, str, provider);
        return new CertPathBuilder((CertPathBuilderSpi) getInstance.impl, getInstance.provider, str);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final CertPathBuilderResult build(CertPathParameters certPathParameters) throws CertPathBuilderException, InvalidAlgorithmParameterException {
        return this.builderSpi.engineBuild(certPathParameters);
    }

    public static final String getDefaultType() {
        String str = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return Security.getProperty(CertPathBuilder.CPB_TYPE);
            }
        });
        return str == null ? "PKIX" : str;
    }

    public final CertPathChecker getRevocationChecker() {
        return this.builderSpi.engineGetRevocationChecker();
    }
}
