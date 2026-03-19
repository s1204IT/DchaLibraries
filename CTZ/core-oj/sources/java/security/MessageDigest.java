package java.security;

import java.nio.ByteBuffer;
import sun.security.jca.Providers;
import sun.security.pkcs.PKCS9Attribute;

public abstract class MessageDigest extends MessageDigestSpi {
    private static final int INITIAL = 0;
    private static final int IN_PROGRESS = 1;
    private String algorithm;
    private Provider provider;
    private int state = 0;

    protected MessageDigest(String str) {
        this.algorithm = str;
    }

    public static MessageDigest getInstance(String str) throws NoSuchAlgorithmException {
        MessageDigest delegate;
        try {
            Object[] impl = Security.getImpl(str, PKCS9Attribute.MESSAGE_DIGEST_STR, (String) null);
            if (impl[0] instanceof MessageDigest) {
                delegate = (MessageDigest) impl[0];
            } else {
                delegate = new Delegate((MessageDigestSpi) impl[0], str);
            }
            delegate.provider = (Provider) impl[1];
            return delegate;
        } catch (NoSuchProviderException e) {
            throw new NoSuchAlgorithmException(str + " not found");
        }
    }

    public static MessageDigest getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        if (str2 == null || str2.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        Providers.checkBouncyCastleDeprecation(str2, PKCS9Attribute.MESSAGE_DIGEST_STR, str);
        Object[] impl = Security.getImpl(str, PKCS9Attribute.MESSAGE_DIGEST_STR, str2);
        if (impl[0] instanceof MessageDigest) {
            MessageDigest messageDigest = (MessageDigest) impl[0];
            messageDigest.provider = (Provider) impl[1];
            return messageDigest;
        }
        Delegate delegate = new Delegate((MessageDigestSpi) impl[0], str);
        ((MessageDigest) delegate).provider = (Provider) impl[1];
        return delegate;
    }

    public static MessageDigest getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        Providers.checkBouncyCastleDeprecation(provider, PKCS9Attribute.MESSAGE_DIGEST_STR, str);
        Object[] impl = Security.getImpl(str, PKCS9Attribute.MESSAGE_DIGEST_STR, provider);
        if (impl[0] instanceof MessageDigest) {
            MessageDigest messageDigest = (MessageDigest) impl[0];
            messageDigest.provider = (Provider) impl[1];
            return messageDigest;
        }
        Delegate delegate = new Delegate((MessageDigestSpi) impl[0], str);
        ((MessageDigest) delegate).provider = (Provider) impl[1];
        return delegate;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public void update(byte b) {
        engineUpdate(b);
        this.state = 1;
    }

    public void update(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            throw new IllegalArgumentException("No input buffer given");
        }
        if (bArr.length - i < i2) {
            throw new IllegalArgumentException("Input buffer too short");
        }
        engineUpdate(bArr, i, i2);
        this.state = 1;
    }

    public void update(byte[] bArr) {
        engineUpdate(bArr, 0, bArr.length);
        this.state = 1;
    }

    public final void update(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            throw new NullPointerException();
        }
        engineUpdate(byteBuffer);
        this.state = 1;
    }

    public byte[] digest() {
        byte[] bArrEngineDigest = engineDigest();
        this.state = 0;
        return bArrEngineDigest;
    }

    public int digest(byte[] bArr, int i, int i2) throws DigestException {
        if (bArr == null) {
            throw new IllegalArgumentException("No output buffer given");
        }
        if (bArr.length - i < i2) {
            throw new IllegalArgumentException("Output buffer too small for specified offset and length");
        }
        int iEngineDigest = engineDigest(bArr, i, i2);
        this.state = 0;
        return iEngineDigest;
    }

    public byte[] digest(byte[] bArr) {
        update(bArr);
        return digest();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.algorithm);
        sb.append(" Message Digest from ");
        sb.append(this.provider.getName());
        sb.append(", ");
        switch (this.state) {
            case 0:
                sb.append("<initialized>");
                break;
            case 1:
                sb.append("<in progress>");
                break;
        }
        return sb.toString();
    }

    public static boolean isEqual(byte[] bArr, byte[] bArr2) {
        if (bArr == bArr2) {
            return true;
        }
        if (bArr == null || bArr2 == null || bArr.length != bArr2.length) {
            return false;
        }
        int i = 0;
        for (int i2 = 0; i2 < bArr.length; i2++) {
            i |= bArr[i2] ^ bArr2[i2];
        }
        if (i == 0) {
            return true;
        }
        return false;
    }

    public void reset() {
        engineReset();
        this.state = 0;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final int getDigestLength() {
        int iEngineGetDigestLength = engineGetDigestLength();
        if (iEngineGetDigestLength == 0) {
            try {
                return ((MessageDigest) clone()).digest().length;
            } catch (CloneNotSupportedException e) {
                return iEngineGetDigestLength;
            }
        }
        return iEngineGetDigestLength;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }

    static class Delegate extends MessageDigest {
        private MessageDigestSpi digestSpi;

        public Delegate(MessageDigestSpi messageDigestSpi, String str) {
            super(str);
            this.digestSpi = messageDigestSpi;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            if (this.digestSpi instanceof Cloneable) {
                Delegate delegate = new Delegate((MessageDigestSpi) this.digestSpi.clone(), ((MessageDigest) this).algorithm);
                ((MessageDigest) delegate).provider = ((MessageDigest) this).provider;
                ((MessageDigest) delegate).state = ((MessageDigest) this).state;
                return delegate;
            }
            throw new CloneNotSupportedException();
        }

        @Override
        protected int engineGetDigestLength() {
            return this.digestSpi.engineGetDigestLength();
        }

        @Override
        protected void engineUpdate(byte b) {
            this.digestSpi.engineUpdate(b);
        }

        @Override
        protected void engineUpdate(byte[] bArr, int i, int i2) {
            this.digestSpi.engineUpdate(bArr, i, i2);
        }

        @Override
        protected void engineUpdate(ByteBuffer byteBuffer) {
            this.digestSpi.engineUpdate(byteBuffer);
        }

        @Override
        protected byte[] engineDigest() {
            return this.digestSpi.engineDigest();
        }

        @Override
        protected int engineDigest(byte[] bArr, int i, int i2) throws DigestException {
            return this.digestSpi.engineDigest(bArr, i, i2);
        }

        @Override
        protected void engineReset() {
            this.digestSpi.engineReset();
        }
    }
}
