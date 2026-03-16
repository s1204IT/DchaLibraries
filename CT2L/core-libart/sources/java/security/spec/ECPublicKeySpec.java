package java.security.spec;

public class ECPublicKeySpec implements KeySpec {
    private final ECParameterSpec params;
    private final ECPoint w;

    public ECPublicKeySpec(ECPoint w, ECParameterSpec params) {
        this.w = w;
        this.params = params;
        if (this.w == null) {
            throw new NullPointerException("w == null");
        }
        if (this.params == null) {
            throw new NullPointerException("params == null");
        }
        if (this.w.equals(ECPoint.POINT_INFINITY)) {
            throw new IllegalArgumentException("the w parameter is point at infinity");
        }
    }

    public ECParameterSpec getParams() {
        return this.params;
    }

    public ECPoint getW() {
        return this.w;
    }
}
