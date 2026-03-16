package java.security.spec;

import java.math.BigInteger;

public class ECPrivateKeySpec implements KeySpec {
    private final ECParameterSpec params;
    private final BigInteger s;

    public ECPrivateKeySpec(BigInteger s, ECParameterSpec params) {
        this.s = s;
        this.params = params;
        if (this.s == null) {
            throw new NullPointerException("s == null");
        }
        if (this.params == null) {
            throw new NullPointerException("params == null");
        }
    }

    public ECParameterSpec getParams() {
        return this.params;
    }

    public BigInteger getS() {
        return this.s;
    }
}
