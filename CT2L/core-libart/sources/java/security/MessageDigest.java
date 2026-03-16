package java.security;

import java.nio.ByteBuffer;
import org.apache.harmony.security.fortress.Engine;

public abstract class MessageDigest extends MessageDigestSpi {
    private static final Engine ENGINE = new Engine("MessageDigest");
    private String algorithm;
    private Provider provider;

    protected MessageDigest(String algorithm) {
        this.algorithm = algorithm;
    }

    public static MessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Engine.SpiAndProvider sap = ENGINE.getInstance(algorithm, (Object) null);
        Object spi = sap.spi;
        Provider provider = sap.provider;
        if (!(spi instanceof MessageDigest)) {
            return new MessageDigestImpl((MessageDigestSpi) sap.spi, sap.provider, algorithm);
        }
        MessageDigest result = (MessageDigest) spi;
        result.algorithm = algorithm;
        result.provider = provider;
        return result;
    }

    public static MessageDigest getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException();
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException(provider);
        }
        return getInstance(algorithm, p);
    }

    public static MessageDigest getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null");
        }
        if (algorithm == null) {
            throw new NullPointerException("algorithm == null");
        }
        Object spi = ENGINE.getInstance(algorithm, provider, null);
        if (!(spi instanceof MessageDigest)) {
            return new MessageDigestImpl((MessageDigestSpi) spi, provider, algorithm);
        }
        MessageDigest result = (MessageDigest) spi;
        result.algorithm = algorithm;
        result.provider = provider;
        return result;
    }

    public void reset() {
        engineReset();
    }

    public void update(byte arg0) {
        engineUpdate(arg0);
    }

    public void update(byte[] input, int offset, int len) {
        if (input == null || ((long) offset) + ((long) len) > input.length) {
            throw new IllegalArgumentException();
        }
        engineUpdate(input, offset, len);
    }

    public void update(byte[] input) {
        if (input == null) {
            throw new NullPointerException("input == null");
        }
        engineUpdate(input, 0, input.length);
    }

    public byte[] digest() {
        return engineDigest();
    }

    public int digest(byte[] buf, int offset, int len) throws DigestException {
        if (buf == null || ((long) offset) + ((long) len) > buf.length) {
            throw new IllegalArgumentException();
        }
        return engineDigest(buf, offset, len);
    }

    public byte[] digest(byte[] input) {
        update(input);
        return digest();
    }

    public String toString() {
        return "MESSAGE DIGEST " + this.algorithm;
    }

    public static boolean isEqual(byte[] digesta, byte[] digestb) {
        if (digesta.length != digestb.length) {
            return false;
        }
        int v = 0;
        for (int i = 0; i < digesta.length; i++) {
            v |= digesta[i] ^ digestb[i];
        }
        return v == 0;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final int getDigestLength() {
        int l = engineGetDigestLength();
        if (l == 0) {
            if (!(this instanceof Cloneable)) {
                return 0;
            }
            try {
                MessageDigest md = (MessageDigest) clone();
                return md.digest().length;
            } catch (CloneNotSupportedException e) {
                return 0;
            }
        }
        return l;
    }

    public final void update(ByteBuffer input) {
        engineUpdate(input);
    }

    private static class MessageDigestImpl extends MessageDigest {
        private MessageDigestSpi spiImpl;

        private MessageDigestImpl(MessageDigestSpi messageDigestSpi, Provider provider, String algorithm) {
            super(algorithm);
            ((MessageDigest) this).provider = provider;
            this.spiImpl = messageDigestSpi;
        }

        @Override
        protected void engineReset() {
            this.spiImpl.engineReset();
        }

        @Override
        protected byte[] engineDigest() {
            return this.spiImpl.engineDigest();
        }

        @Override
        protected int engineGetDigestLength() {
            return this.spiImpl.engineGetDigestLength();
        }

        @Override
        protected void engineUpdate(byte arg0) {
            this.spiImpl.engineUpdate(arg0);
        }

        @Override
        protected void engineUpdate(byte[] arg0, int arg1, int arg2) {
            this.spiImpl.engineUpdate(arg0, arg1, arg2);
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            MessageDigestSpi spi = (MessageDigestSpi) this.spiImpl.clone();
            return new MessageDigestImpl(spi, getProvider(), getAlgorithm());
        }
    }
}
