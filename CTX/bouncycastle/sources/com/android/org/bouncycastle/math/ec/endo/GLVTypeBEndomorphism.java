package com.android.org.bouncycastle.math.ec.endo;

import com.android.org.bouncycastle.math.ec.ECConstants;
import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECPointMap;
import com.android.org.bouncycastle.math.ec.ScaleXPointMap;
import java.math.BigInteger;

public class GLVTypeBEndomorphism implements GLVEndomorphism {
    protected final ECCurve curve;
    protected final GLVTypeBParameters parameters;
    protected final ECPointMap pointMap;

    public GLVTypeBEndomorphism(ECCurve eCCurve, GLVTypeBParameters gLVTypeBParameters) {
        this.curve = eCCurve;
        this.parameters = gLVTypeBParameters;
        this.pointMap = new ScaleXPointMap(eCCurve.fromBigInteger(gLVTypeBParameters.getBeta()));
    }

    @Override
    public BigInteger[] decomposeScalar(BigInteger bigInteger) {
        int bits = this.parameters.getBits();
        BigInteger bigIntegerCalculateB = calculateB(bigInteger, this.parameters.getG1(), bits);
        BigInteger bigIntegerCalculateB2 = calculateB(bigInteger, this.parameters.getG2(), bits);
        GLVTypeBParameters gLVTypeBParameters = this.parameters;
        return new BigInteger[]{bigInteger.subtract(bigIntegerCalculateB.multiply(gLVTypeBParameters.getV1A()).add(bigIntegerCalculateB2.multiply(gLVTypeBParameters.getV2A()))), bigIntegerCalculateB.multiply(gLVTypeBParameters.getV1B()).add(bigIntegerCalculateB2.multiply(gLVTypeBParameters.getV2B())).negate()};
    }

    @Override
    public ECPointMap getPointMap() {
        return this.pointMap;
    }

    @Override
    public boolean hasEfficientPointMap() {
        return true;
    }

    protected BigInteger calculateB(BigInteger bigInteger, BigInteger bigInteger2, int i) {
        boolean z;
        if (bigInteger2.signum() >= 0) {
            z = false;
        } else {
            z = true;
        }
        BigInteger bigIntegerMultiply = bigInteger.multiply(bigInteger2.abs());
        boolean zTestBit = bigIntegerMultiply.testBit(i - 1);
        BigInteger bigIntegerShiftRight = bigIntegerMultiply.shiftRight(i);
        if (zTestBit) {
            bigIntegerShiftRight = bigIntegerShiftRight.add(ECConstants.ONE);
        }
        return z ? bigIntegerShiftRight.negate() : bigIntegerShiftRight;
    }
}
