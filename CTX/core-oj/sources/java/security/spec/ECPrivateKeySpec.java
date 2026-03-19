package java.security.spec;

import java.math.BigInteger;

public class ECPrivateKeySpec implements KeySpec {
    private ECParameterSpec params;
    private BigInteger s;

    public ECPrivateKeySpec(BigInteger bigInteger, ECParameterSpec eCParameterSpec) {
        if (bigInteger == null) {
            throw new NullPointerException("s is null");
        }
        if (eCParameterSpec == null) {
            throw new NullPointerException("params is null");
        }
        this.s = bigInteger;
        this.params = eCParameterSpec;
    }

    public BigInteger getS() {
        return this.s;
    }

    public ECParameterSpec getParams() {
        return this.params;
    }
}
