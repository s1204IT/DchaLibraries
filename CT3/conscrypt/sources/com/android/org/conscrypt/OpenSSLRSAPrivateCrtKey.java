package com.android.org.conscrypt;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;

public class OpenSSLRSAPrivateCrtKey extends OpenSSLRSAPrivateKey implements RSAPrivateCrtKey {
    private static final long serialVersionUID = 3785291944868707197L;
    private BigInteger crtCoefficient;
    private BigInteger primeExponentP;
    private BigInteger primeExponentQ;
    private BigInteger primeP;
    private BigInteger primeQ;
    private BigInteger publicExponent;

    OpenSSLRSAPrivateCrtKey(OpenSSLKey key) {
        super(key);
    }

    OpenSSLRSAPrivateCrtKey(OpenSSLKey key, byte[][] params) {
        super(key, params);
    }

    public OpenSSLRSAPrivateCrtKey(RSAPrivateCrtKeySpec rsaKeySpec) throws InvalidKeySpecException {
        super(init(rsaKeySpec));
    }

    private static OpenSSLKey init(RSAPrivateCrtKeySpec rsaKeySpec) throws InvalidKeySpecException {
        BigInteger modulus = rsaKeySpec.getModulus();
        BigInteger privateExponent = rsaKeySpec.getPrivateExponent();
        if (modulus == null) {
            throw new InvalidKeySpecException("modulus == null");
        }
        if (privateExponent == null) {
            throw new InvalidKeySpecException("privateExponent == null");
        }
        try {
            BigInteger publicExponent = rsaKeySpec.getPublicExponent();
            BigInteger primeP = rsaKeySpec.getPrimeP();
            BigInteger primeQ = rsaKeySpec.getPrimeQ();
            BigInteger primeExponentP = rsaKeySpec.getPrimeExponentP();
            BigInteger primeExponentQ = rsaKeySpec.getPrimeExponentQ();
            BigInteger crtCoefficient = rsaKeySpec.getCrtCoefficient();
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_RSA(modulus.toByteArray(), publicExponent == null ? null : publicExponent.toByteArray(), privateExponent.toByteArray(), primeP == null ? null : primeP.toByteArray(), primeQ == null ? null : primeQ.toByteArray(), primeExponentP == null ? null : primeExponentP.toByteArray(), primeExponentQ == null ? null : primeExponentQ.toByteArray(), crtCoefficient == null ? null : crtCoefficient.toByteArray()));
        } catch (Exception e) {
            throw new InvalidKeySpecException(e);
        }
    }

    static OpenSSLKey getInstance(RSAPrivateCrtKey rsaPrivateKey) throws InvalidKeyException {
        if (rsaPrivateKey.getFormat() == null) {
            return wrapPlatformKey(rsaPrivateKey);
        }
        BigInteger modulus = rsaPrivateKey.getModulus();
        BigInteger privateExponent = rsaPrivateKey.getPrivateExponent();
        if (modulus == null) {
            throw new InvalidKeyException("modulus == null");
        }
        if (privateExponent == null) {
            throw new InvalidKeyException("privateExponent == null");
        }
        try {
            BigInteger publicExponent = rsaPrivateKey.getPublicExponent();
            BigInteger primeP = rsaPrivateKey.getPrimeP();
            BigInteger primeQ = rsaPrivateKey.getPrimeQ();
            BigInteger primeExponentP = rsaPrivateKey.getPrimeExponentP();
            BigInteger primeExponentQ = rsaPrivateKey.getPrimeExponentQ();
            BigInteger crtCoefficient = rsaPrivateKey.getCrtCoefficient();
            return new OpenSSLKey(NativeCrypto.EVP_PKEY_new_RSA(modulus.toByteArray(), publicExponent == null ? null : publicExponent.toByteArray(), privateExponent.toByteArray(), primeP == null ? null : primeP.toByteArray(), primeQ == null ? null : primeQ.toByteArray(), primeExponentP == null ? null : primeExponentP.toByteArray(), primeExponentQ == null ? null : primeExponentQ.toByteArray(), crtCoefficient == null ? null : crtCoefficient.toByteArray()));
        } catch (Exception e) {
            throw new InvalidKeyException(e);
        }
    }

    @Override
    synchronized void readParams(byte[][] params) {
        super.readParams(params);
        if (params[1] != null) {
            this.publicExponent = new BigInteger(params[1]);
        }
        if (params[3] != null) {
            this.primeP = new BigInteger(params[3]);
        }
        if (params[4] != null) {
            this.primeQ = new BigInteger(params[4]);
        }
        if (params[5] != null) {
            this.primeExponentP = new BigInteger(params[5]);
        }
        if (params[6] != null) {
            this.primeExponentQ = new BigInteger(params[6]);
        }
        if (params[7] != null) {
            this.crtCoefficient = new BigInteger(params[7]);
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof OpenSSLRSAPrivateKey) {
            return getOpenSSLKey().equals(((OpenSSLRSAPrivateKey) o).getOpenSSLKey());
        }
        if (!(o instanceof RSAPrivateCrtKey)) {
            if (!(o instanceof RSAPrivateKey)) {
                return false;
            }
            ensureReadParams();
            RSAPrivateKey other = (RSAPrivateKey) o;
            if (getOpenSSLKey().isEngineBased()) {
                return getModulus().equals(other.getModulus());
            }
            if (getModulus().equals(other.getModulus())) {
                return getPrivateExponent().equals(other.getPrivateExponent());
            }
            return false;
        }
        ensureReadParams();
        RSAPrivateCrtKey other2 = (RSAPrivateCrtKey) o;
        if (getOpenSSLKey().isEngineBased()) {
            if (getModulus().equals(other2.getModulus())) {
                return this.publicExponent.equals(other2.getPublicExponent());
            }
            return false;
        }
        if (getModulus().equals(other2.getModulus()) && this.publicExponent.equals(other2.getPublicExponent()) && getPrivateExponent().equals(other2.getPrivateExponent()) && this.primeP.equals(other2.getPrimeP()) && this.primeQ.equals(other2.getPrimeQ()) && this.primeExponentP.equals(other2.getPrimeExponentP()) && this.primeExponentQ.equals(other2.getPrimeExponentQ())) {
            return this.crtCoefficient.equals(other2.getCrtCoefficient());
        }
        return false;
    }

    @Override
    public final int hashCode() {
        int hashCode = super.hashCode();
        if (this.publicExponent != null) {
            return hashCode ^ this.publicExponent.hashCode();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("OpenSSLRSAPrivateCrtKey{");
        boolean engineBased = getOpenSSLKey().isEngineBased();
        if (engineBased) {
            sb.append("key=");
            sb.append(getOpenSSLKey());
            sb.append(',');
        }
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

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        this.key = new OpenSSLKey(NativeCrypto.EVP_PKEY_new_RSA(this.modulus.toByteArray(), this.publicExponent == null ? null : this.publicExponent.toByteArray(), this.privateExponent.toByteArray(), this.primeP == null ? null : this.primeP.toByteArray(), this.primeQ == null ? null : this.primeQ.toByteArray(), this.primeExponentP == null ? null : this.primeExponentP.toByteArray(), this.primeExponentQ == null ? null : this.primeExponentQ.toByteArray(), this.crtCoefficient != null ? this.crtCoefficient.toByteArray() : null));
        this.fetchedParams = true;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (getOpenSSLKey().isEngineBased()) {
            throw new NotSerializableException("engine-based keys can not be serialized");
        }
        ensureReadParams();
        stream.defaultWriteObject();
    }
}
