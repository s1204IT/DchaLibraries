package com.android.org.conscrypt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;

final class OpenSSLRSAPrivateCrtKey extends OpenSSLRSAPrivateKey implements RSAPrivateCrtKey {
    private static final long serialVersionUID = 3785291944868707197L;
    private BigInteger crtCoefficient;
    private BigInteger primeExponentP;
    private BigInteger primeExponentQ;
    private BigInteger primeP;
    private BigInteger primeQ;
    private BigInteger publicExponent;

    OpenSSLRSAPrivateCrtKey(OpenSSLKey openSSLKey) {
        super(openSSLKey);
    }

    OpenSSLRSAPrivateCrtKey(OpenSSLKey openSSLKey, byte[][] bArr) {
        super(openSSLKey, bArr);
    }

    OpenSSLRSAPrivateCrtKey(RSAPrivateCrtKeySpec rSAPrivateCrtKeySpec) throws InvalidKeySpecException {
        super(init(rSAPrivateCrtKeySpec));
    }

    private static OpenSSLKey init(RSAPrivateCrtKeySpec rSAPrivateCrtKeySpec) throws InvalidKeySpecException {
        byte[] byteArray;
        byte[] byteArray2;
        byte[] byteArray3;
        byte[] byteArray4;
        byte[] byteArray5;
        BigInteger modulus = rSAPrivateCrtKeySpec.getModulus();
        BigInteger privateExponent = rSAPrivateCrtKeySpec.getPrivateExponent();
        if (modulus == null) {
            throw new InvalidKeySpecException("modulus == null");
        }
        if (privateExponent == null) {
            throw new InvalidKeySpecException("privateExponent == null");
        }
        try {
            BigInteger publicExponent = rSAPrivateCrtKeySpec.getPublicExponent();
            BigInteger primeP = rSAPrivateCrtKeySpec.getPrimeP();
            BigInteger primeQ = rSAPrivateCrtKeySpec.getPrimeQ();
            BigInteger primeExponentP = rSAPrivateCrtKeySpec.getPrimeExponentP();
            BigInteger primeExponentQ = rSAPrivateCrtKeySpec.getPrimeExponentQ();
            BigInteger crtCoefficient = rSAPrivateCrtKeySpec.getCrtCoefficient();
            byte[] byteArray6 = modulus.toByteArray();
            byte[] byteArray7 = null;
            if (publicExponent != null) {
                byteArray = publicExponent.toByteArray();
            } else {
                byteArray = null;
            }
            byte[] byteArray8 = privateExponent.toByteArray();
            if (primeP != null) {
                byteArray2 = primeP.toByteArray();
            } else {
                byteArray2 = null;
            }
            if (primeQ != null) {
                byteArray3 = primeQ.toByteArray();
            } else {
                byteArray3 = null;
            }
            if (primeExponentP != null) {
                byteArray4 = primeExponentP.toByteArray();
            } else {
                byteArray4 = null;
            }
            if (primeExponentQ != null) {
                byteArray5 = primeExponentQ.toByteArray();
            } else {
                byteArray5 = null;
            }
            if (crtCoefficient != null) {
                byteArray7 = crtCoefficient.toByteArray();
            }
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_RSA(byteArray6, byteArray, byteArray8, byteArray2, byteArray3, byteArray4, byteArray5, byteArray7));
        } catch (Exception e) {
            throw new InvalidKeySpecException(e);
        }
    }

    static OpenSSLKey getInstance(RSAPrivateCrtKey rSAPrivateCrtKey) throws InvalidKeyException {
        byte[] byteArray;
        byte[] byteArray2;
        byte[] byteArray3;
        byte[] byteArray4;
        byte[] byteArray5;
        if (rSAPrivateCrtKey.getFormat() == null) {
            return wrapPlatformKey(rSAPrivateCrtKey);
        }
        BigInteger modulus = rSAPrivateCrtKey.getModulus();
        BigInteger privateExponent = rSAPrivateCrtKey.getPrivateExponent();
        if (modulus == null) {
            throw new InvalidKeyException("modulus == null");
        }
        if (privateExponent == null) {
            throw new InvalidKeyException("privateExponent == null");
        }
        try {
            BigInteger publicExponent = rSAPrivateCrtKey.getPublicExponent();
            BigInteger primeP = rSAPrivateCrtKey.getPrimeP();
            BigInteger primeQ = rSAPrivateCrtKey.getPrimeQ();
            BigInteger primeExponentP = rSAPrivateCrtKey.getPrimeExponentP();
            BigInteger primeExponentQ = rSAPrivateCrtKey.getPrimeExponentQ();
            BigInteger crtCoefficient = rSAPrivateCrtKey.getCrtCoefficient();
            byte[] byteArray6 = modulus.toByteArray();
            byte[] byteArray7 = null;
            if (publicExponent != null) {
                byteArray = publicExponent.toByteArray();
            } else {
                byteArray = null;
            }
            byte[] byteArray8 = privateExponent.toByteArray();
            if (primeP != null) {
                byteArray2 = primeP.toByteArray();
            } else {
                byteArray2 = null;
            }
            if (primeQ != null) {
                byteArray3 = primeQ.toByteArray();
            } else {
                byteArray3 = null;
            }
            if (primeExponentP != null) {
                byteArray4 = primeExponentP.toByteArray();
            } else {
                byteArray4 = null;
            }
            if (primeExponentQ != null) {
                byteArray5 = primeExponentQ.toByteArray();
            } else {
                byteArray5 = null;
            }
            if (crtCoefficient != null) {
                byteArray7 = crtCoefficient.toByteArray();
            }
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_RSA(byteArray6, byteArray, byteArray8, byteArray2, byteArray3, byteArray4, byteArray5, byteArray7));
        } catch (Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    @Override
    synchronized void readParams(byte[][] bArr) {
        super.readParams(bArr);
        if (bArr[1] != null) {
            this.publicExponent = new BigInteger(bArr[1]);
        }
        if (bArr[3] != null) {
            this.primeP = new BigInteger(bArr[3]);
        }
        if (bArr[4] != null) {
            this.primeQ = new BigInteger(bArr[4]);
        }
        if (bArr[5] != null) {
            this.primeExponentP = new BigInteger(bArr[5]);
        }
        if (bArr[6] != null) {
            this.primeExponentQ = new BigInteger(bArr[6]);
        }
        if (bArr[7] != null) {
            this.crtCoefficient = new BigInteger(bArr[7]);
        }
    }

    @Override
    public BigInteger getPublicExponent() {
        ensureReadParams();
        return this.publicExponent;
    }

    @Override
    public BigInteger getPrimeP() {
        ensureReadParams();
        return this.primeP;
    }

    @Override
    public BigInteger getPrimeQ() {
        ensureReadParams();
        return this.primeQ;
    }

    @Override
    public BigInteger getPrimeExponentP() {
        ensureReadParams();
        return this.primeExponentP;
    }

    @Override
    public BigInteger getPrimeExponentQ() {
        ensureReadParams();
        return this.primeExponentQ;
    }

    @Override
    public BigInteger getCrtCoefficient() {
        ensureReadParams();
        return this.crtCoefficient;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof OpenSSLRSAPrivateKey) {
            return getOpenSSLKey().equals(((OpenSSLRSAPrivateKey) obj).getOpenSSLKey());
        }
        if (obj instanceof RSAPrivateCrtKey) {
            ensureReadParams();
            RSAPrivateCrtKey rSAPrivateCrtKey = (RSAPrivateCrtKey) obj;
            return getModulus().equals(rSAPrivateCrtKey.getModulus()) && this.publicExponent.equals(rSAPrivateCrtKey.getPublicExponent()) && getPrivateExponent().equals(rSAPrivateCrtKey.getPrivateExponent()) && this.primeP.equals(rSAPrivateCrtKey.getPrimeP()) && this.primeQ.equals(rSAPrivateCrtKey.getPrimeQ()) && this.primeExponentP.equals(rSAPrivateCrtKey.getPrimeExponentP()) && this.primeExponentQ.equals(rSAPrivateCrtKey.getPrimeExponentQ()) && this.crtCoefficient.equals(rSAPrivateCrtKey.getCrtCoefficient());
        }
        if (!(obj instanceof RSAPrivateKey)) {
            return false;
        }
        ensureReadParams();
        RSAPrivateKey rSAPrivateKey = (RSAPrivateKey) obj;
        return getModulus().equals(rSAPrivateKey.getModulus()) && getPrivateExponent().equals(rSAPrivateKey.getPrivateExponent());
    }

    @Override
    public final int hashCode() {
        int iHashCode = super.hashCode();
        if (this.publicExponent != null) {
            return iHashCode ^ this.publicExponent.hashCode();
        }
        return iHashCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("OpenSSLRSAPrivateCrtKey{");
        ensureReadParams();
        sb.append("modulus=");
        sb.append(getModulus().toString(16));
        if (this.publicExponent != null) {
            sb.append(',');
            sb.append("publicExponent=");
            sb.append(this.publicExponent.toString(16));
        }
        sb.append('}');
        return sb.toString();
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        byte[] byteArray;
        byte[] byteArray2;
        byte[] byteArray3;
        byte[] byteArray4;
        objectInputStream.defaultReadObject();
        byte[] byteArray5 = this.modulus.toByteArray();
        byte[] byteArray6 = this.publicExponent == null ? null : this.publicExponent.toByteArray();
        byte[] byteArray7 = this.privateExponent.toByteArray();
        if (this.primeP != null) {
            byteArray = this.primeP.toByteArray();
        } else {
            byteArray = null;
        }
        if (this.primeQ != null) {
            byteArray2 = this.primeQ.toByteArray();
        } else {
            byteArray2 = null;
        }
        if (this.primeExponentP != null) {
            byteArray3 = this.primeExponentP.toByteArray();
        } else {
            byteArray3 = null;
        }
        if (this.primeExponentQ != null) {
            byteArray4 = this.primeExponentQ.toByteArray();
        } else {
            byteArray4 = null;
        }
        this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_RSA(byteArray5, byteArray6, byteArray7, byteArray, byteArray2, byteArray3, byteArray4, this.crtCoefficient != null ? this.crtCoefficient.toByteArray() : null));
        this.fetchedParams = true;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        ensureReadParams();
        objectOutputStream.defaultWriteObject();
    }
}
