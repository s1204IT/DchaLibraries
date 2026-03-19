package javax.crypto;

import java.security.Permission;
import java.security.spec.AlgorithmParameterSpec;

class CryptoPermission extends Permission {
    static final String ALG_NAME_WILDCARD = null;

    CryptoPermission(String str) {
        super("");
    }

    CryptoPermission(String str, int i) {
        super("");
    }

    CryptoPermission(String str, int i, AlgorithmParameterSpec algorithmParameterSpec) {
        super("");
    }

    CryptoPermission(String str, String str2) {
        super("");
    }

    CryptoPermission(String str, int i, String str2) {
        super("");
    }

    CryptoPermission(String str, int i, AlgorithmParameterSpec algorithmParameterSpec, String str2) {
        super("");
    }

    @Override
    public boolean implies(Permission permission) {
        return true;
    }

    @Override
    public String getActions() {
        return null;
    }

    final String getAlgorithm() {
        return null;
    }

    final String getExemptionMechanism() {
        return null;
    }

    final int getMaxKeySize() {
        return Integer.MAX_VALUE;
    }

    final boolean getCheckParam() {
        return false;
    }

    final AlgorithmParameterSpec getAlgorithmParameterSpec() {
        return null;
    }
}
