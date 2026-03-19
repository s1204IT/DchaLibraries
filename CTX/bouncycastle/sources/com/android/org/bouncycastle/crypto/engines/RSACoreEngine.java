package com.android.org.bouncycastle.crypto.engines;

import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;
import com.android.org.bouncycastle.crypto.params.RSAKeyParameters;
import com.android.org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import java.math.BigInteger;

class RSACoreEngine {
    private boolean forEncryption;
    private RSAKeyParameters key;

    RSACoreEngine() {
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof ParametersWithRandom) {
            this.key = (RSAKeyParameters) ((ParametersWithRandom) cipherParameters).getParameters();
        } else {
            this.key = (RSAKeyParameters) cipherParameters;
        }
        this.forEncryption = z;
    }

    public int getInputBlockSize() {
        int iBitLength = this.key.getModulus().bitLength();
        if (this.forEncryption) {
            return ((iBitLength + 7) / 8) - 1;
        }
        return (iBitLength + 7) / 8;
    }

    public int getOutputBlockSize() {
        int iBitLength = this.key.getModulus().bitLength();
        if (this.forEncryption) {
            return (iBitLength + 7) / 8;
        }
        return ((iBitLength + 7) / 8) - 1;
    }

    public BigInteger convertInput(byte[] bArr, int i, int i2) {
        if (i2 > getInputBlockSize() + 1) {
            throw new DataLengthException("input too large for RSA cipher.");
        }
        if (i2 == getInputBlockSize() + 1 && !this.forEncryption) {
            throw new DataLengthException("input too large for RSA cipher.");
        }
        if (i != 0 || i2 != bArr.length) {
            byte[] bArr2 = new byte[i2];
            System.arraycopy(bArr, i, bArr2, 0, i2);
            bArr = bArr2;
        }
        BigInteger bigInteger = new BigInteger(1, bArr);
        if (bigInteger.compareTo(this.key.getModulus()) >= 0) {
            throw new DataLengthException("input too large for RSA cipher.");
        }
        return bigInteger;
    }

    public byte[] convertOutput(BigInteger bigInteger) {
        byte[] byteArray = bigInteger.toByteArray();
        if (this.forEncryption) {
            if (byteArray[0] == 0 && byteArray.length > getOutputBlockSize()) {
                byte[] bArr = new byte[byteArray.length - 1];
                System.arraycopy(byteArray, 1, bArr, 0, bArr.length);
                return bArr;
            }
            if (byteArray.length < getOutputBlockSize()) {
                byte[] bArr2 = new byte[getOutputBlockSize()];
                System.arraycopy(byteArray, 0, bArr2, bArr2.length - byteArray.length, byteArray.length);
                return bArr2;
            }
        } else if (byteArray[0] == 0) {
            byte[] bArr3 = new byte[byteArray.length - 1];
            System.arraycopy(byteArray, 1, bArr3, 0, bArr3.length);
            return bArr3;
        }
        return byteArray;
    }

    public BigInteger processBlock(BigInteger bigInteger) {
        if (this.key instanceof RSAPrivateCrtKeyParameters) {
            RSAPrivateCrtKeyParameters rSAPrivateCrtKeyParameters = (RSAPrivateCrtKeyParameters) this.key;
            BigInteger p = rSAPrivateCrtKeyParameters.getP();
            BigInteger q = rSAPrivateCrtKeyParameters.getQ();
            BigInteger dp = rSAPrivateCrtKeyParameters.getDP();
            BigInteger dq = rSAPrivateCrtKeyParameters.getDQ();
            BigInteger qInv = rSAPrivateCrtKeyParameters.getQInv();
            BigInteger bigIntegerModPow = bigInteger.remainder(p).modPow(dp, p);
            BigInteger bigIntegerModPow2 = bigInteger.remainder(q).modPow(dq, q);
            return bigIntegerModPow.subtract(bigIntegerModPow2).multiply(qInv).mod(p).multiply(q).add(bigIntegerModPow2);
        }
        return bigInteger.modPow(this.key.getExponent(), this.key.getModulus());
    }
}
