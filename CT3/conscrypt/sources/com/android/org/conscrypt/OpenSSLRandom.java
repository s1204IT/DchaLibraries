package com.android.org.conscrypt;

import java.io.Serializable;
import java.security.SecureRandomSpi;

public class OpenSSLRandom extends SecureRandomSpi implements Serializable {
    private static final long serialVersionUID = 8506210602917522860L;
    private boolean mSeeded;

    @Override
    protected void engineSetSeed(byte[] seed) {
        if (seed == null) {
            throw new NullPointerException("seed == null");
        }
        selfSeedIfNotSeeded();
        NativeCrypto.RAND_seed(seed);
    }

    @Override
    protected void engineNextBytes(byte[] bytes) {
        selfSeedIfNotSeeded();
        NativeCrypto.RAND_bytes(bytes);
    }

    @Override
    protected byte[] engineGenerateSeed(int numBytes) {
        selfSeedIfNotSeeded();
        byte[] output = new byte[numBytes];
        NativeCrypto.RAND_bytes(output);
        return output;
    }

    private void selfSeedIfNotSeeded() {
        if (NativeCrypto.isBoringSSL || this.mSeeded) {
            return;
        }
        seedOpenSSLPRNGFromLinuxRNG();
        this.mSeeded = true;
    }

    public static void seedOpenSSLPRNGFromLinuxRNG() {
        int bytesRead = NativeCrypto.RAND_load_file("/dev/urandom", 1024L);
        if (bytesRead == 1024) {
        } else {
            throw new SecurityException("Failed to read sufficient bytes from /dev/urandom. Expected: 1024, actual: " + bytesRead);
        }
    }
}
