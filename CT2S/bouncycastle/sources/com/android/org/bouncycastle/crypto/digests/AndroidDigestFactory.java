package com.android.org.bouncycastle.crypto.digests;

import com.android.org.bouncycastle.crypto.Digest;

public final class AndroidDigestFactory {
    private static final AndroidDigestFactoryInterface FACTORY;
    private static final String OpenSSLFactoryClassName = AndroidDigestFactory.class.getName() + "OpenSSL";
    private static final String BouncyCastleFactoryClassName = AndroidDigestFactory.class.getName() + "BouncyCastle";

    static {
        Class<?> cls;
        try {
            cls = Class.forName(OpenSSLFactoryClassName);
            Class.forName("com.android.org.conscrypt.NativeCrypto");
        } catch (ClassNotFoundException e1) {
            try {
                cls = Class.forName(BouncyCastleFactoryClassName);
            } catch (ClassNotFoundException e) {
                AssertionError e2 = new AssertionError("Failed to load AndroidDigestFactoryInterface implementation. Looked for " + OpenSSLFactoryClassName + " and " + BouncyCastleFactoryClassName);
                e2.initCause(e1);
                throw e2;
            }
        }
        if (!AndroidDigestFactoryInterface.class.isAssignableFrom(cls)) {
            throw new AssertionError(cls + "does not implement AndroidDigestFactoryInterface");
        }
        try {
            FACTORY = (AndroidDigestFactoryInterface) cls.newInstance();
        } catch (IllegalAccessException e3) {
            throw new AssertionError(e3);
        } catch (InstantiationException e4) {
            throw new AssertionError(e4);
        }
    }

    public static Digest getMD5() {
        return FACTORY.getMD5();
    }

    public static Digest getSHA1() {
        return FACTORY.getSHA1();
    }

    public static Digest getSHA224() {
        return FACTORY.getSHA224();
    }

    public static Digest getSHA256() {
        return FACTORY.getSHA256();
    }

    public static Digest getSHA384() {
        return FACTORY.getSHA384();
    }

    public static Digest getSHA512() {
        return FACTORY.getSHA512();
    }
}
