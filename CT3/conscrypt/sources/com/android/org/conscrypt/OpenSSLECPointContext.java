package com.android.org.conscrypt;

import com.android.org.conscrypt.NativeRef;
import java.math.BigInteger;
import java.security.spec.ECPoint;

final class OpenSSLECPointContext {
    private final OpenSSLECGroupContext group;
    private final NativeRef.EC_POINT pointCtx;

    OpenSSLECPointContext(OpenSSLECGroupContext group, NativeRef.EC_POINT pointCtx) {
        this.group = group;
        this.pointCtx = pointCtx;
    }

    public boolean equals(Object o) {
        throw new IllegalArgumentException("OpenSSLECPointContext.equals is not defined.");
    }

    public ECPoint getECPoint() {
        byte[][] generatorCoords = NativeCrypto.EC_POINT_get_affine_coordinates(this.group.getNativeRef(), this.pointCtx);
        BigInteger x = new BigInteger(generatorCoords[0]);
        BigInteger y = new BigInteger(generatorCoords[1]);
        return new ECPoint(x, y);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public NativeRef.EC_POINT getNativeRef() {
        return this.pointCtx;
    }

    public static OpenSSLECPointContext getInstance(int curveType, OpenSSLECGroupContext group, ECPoint javaPoint) {
        OpenSSLECPointContext point = new OpenSSLECPointContext(group, new NativeRef.EC_POINT(NativeCrypto.EC_POINT_new(group.getNativeRef())));
        NativeCrypto.EC_POINT_set_affine_coordinates(group.getNativeRef(), point.getNativeRef(), javaPoint.getAffineX().toByteArray(), javaPoint.getAffineY().toByteArray());
        return point;
    }
}
