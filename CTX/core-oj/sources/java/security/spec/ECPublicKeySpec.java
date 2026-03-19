package java.security.spec;

public class ECPublicKeySpec implements KeySpec {
    private ECParameterSpec params;
    private ECPoint w;

    public ECPublicKeySpec(ECPoint eCPoint, ECParameterSpec eCParameterSpec) {
        if (eCPoint == null) {
            throw new NullPointerException("w is null");
        }
        if (eCParameterSpec == null) {
            throw new NullPointerException("params is null");
        }
        if (eCPoint == ECPoint.POINT_INFINITY) {
            throw new IllegalArgumentException("w is ECPoint.POINT_INFINITY");
        }
        this.w = eCPoint;
        this.params = eCParameterSpec;
    }

    public ECPoint getW() {
        return this.w;
    }

    public ECParameterSpec getParams() {
        return this.params;
    }
}
