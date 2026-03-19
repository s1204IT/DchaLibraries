package java.security;

import java.security.Provider;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.security.jca.GetInstance;
import sun.security.jca.Providers;

public class SecureRandom extends Random {
    private static volatile SecureRandom seedGenerator = null;
    static final long serialVersionUID = 4940670005562187L;
    private String algorithm;
    private long counter;
    private MessageDigest digest;
    private Provider provider;
    private byte[] randomBytes;
    private int randomBytesUsed;
    private SecureRandomSpi secureRandomSpi;
    private byte[] state;

    public SecureRandom() {
        super(0L);
        this.provider = null;
        this.secureRandomSpi = null;
        this.digest = null;
        getDefaultPRNG(false, null);
    }

    public SecureRandom(byte[] bArr) {
        super(0L);
        this.provider = null;
        this.secureRandomSpi = null;
        this.digest = null;
        getDefaultPRNG(true, bArr);
    }

    private void getDefaultPRNG(boolean z, byte[] bArr) {
        String prngAlgorithm = getPrngAlgorithm();
        if (prngAlgorithm == null) {
            throw new IllegalStateException("No SecureRandom implementation!");
        }
        try {
            SecureRandom secureRandom = getInstance(prngAlgorithm);
            this.secureRandomSpi = secureRandom.getSecureRandomSpi();
            this.provider = secureRandom.getProvider();
            if (z) {
                this.secureRandomSpi.engineSetSeed(bArr);
            }
            if (getClass() == SecureRandom.class) {
                this.algorithm = prngAlgorithm;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected SecureRandom(SecureRandomSpi secureRandomSpi, Provider provider) {
        this(secureRandomSpi, provider, null);
    }

    private SecureRandom(SecureRandomSpi secureRandomSpi, Provider provider, String str) {
        super(0L);
        this.provider = null;
        this.secureRandomSpi = null;
        this.digest = null;
        this.secureRandomSpi = secureRandomSpi;
        this.provider = provider;
        this.algorithm = str;
    }

    public static SecureRandom getInstance(String str) throws NoSuchAlgorithmException {
        GetInstance.Instance getInstance = GetInstance.getInstance("SecureRandom", (Class<?>) SecureRandomSpi.class, str);
        return new SecureRandom((SecureRandomSpi) getInstance.impl, getInstance.provider, str);
    }

    public static SecureRandom getInstance(String str, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        GetInstance.Instance getInstance = GetInstance.getInstance("SecureRandom", (Class<?>) SecureRandomSpi.class, str, str2);
        return new SecureRandom((SecureRandomSpi) getInstance.impl, getInstance.provider, str);
    }

    public static SecureRandom getInstance(String str, Provider provider) throws NoSuchAlgorithmException {
        GetInstance.Instance getInstance = GetInstance.getInstance("SecureRandom", (Class<?>) SecureRandomSpi.class, str, provider);
        return new SecureRandom((SecureRandomSpi) getInstance.impl, getInstance.provider, str);
    }

    SecureRandomSpi getSecureRandomSpi() {
        return this.secureRandomSpi;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public String getAlgorithm() {
        return this.algorithm != null ? this.algorithm : "unknown";
    }

    public synchronized void setSeed(byte[] bArr) {
        this.secureRandomSpi.engineSetSeed(bArr);
    }

    @Override
    public void setSeed(long j) {
        if (j != 0) {
            this.secureRandomSpi.engineSetSeed(longToByteArray(j));
        }
    }

    @Override
    public synchronized void nextBytes(byte[] bArr) {
        this.secureRandomSpi.engineNextBytes(bArr);
    }

    @Override
    protected final int next(int i) {
        int i2 = (i + 7) / 8;
        byte[] bArr = new byte[i2];
        nextBytes(bArr);
        int i3 = 0;
        for (int i4 = 0; i4 < i2; i4++) {
            i3 = (i3 << 8) + (bArr[i4] & Character.DIRECTIONALITY_UNDEFINED);
        }
        return i3 >>> ((i2 * 8) - i);
    }

    public static byte[] getSeed(int i) {
        if (seedGenerator == null) {
            seedGenerator = new SecureRandom();
        }
        return seedGenerator.generateSeed(i);
    }

    public byte[] generateSeed(int i) {
        return this.secureRandomSpi.engineGenerateSeed(i);
    }

    private static byte[] longToByteArray(long j) {
        byte[] bArr = new byte[8];
        for (int i = 0; i < 8; i++) {
            bArr[i] = (byte) j;
            j >>= 8;
        }
        return bArr;
    }

    private static String getPrngAlgorithm() {
        Iterator<Provider> it = Providers.getProviderList().providers().iterator();
        while (it.hasNext()) {
            for (Provider.Service service : it.next().getServices()) {
                if (service.getType().equals("SecureRandom")) {
                    return service.getAlgorithm();
                }
            }
        }
        return null;
    }

    private static final class StrongPatternHolder {
        private static Pattern pattern = Pattern.compile("\\s*([\\S&&[^:,]]*)(\\:([\\S&&[^,]]*))?\\s*(\\,(.*))?");

        private StrongPatternHolder() {
        }
    }

    public static SecureRandom getInstanceStrong() throws NoSuchAlgorithmException {
        String str = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return Security.getProperty("securerandom.strongAlgorithms");
            }
        });
        if (str == null || str.length() == 0) {
            throw new NoSuchAlgorithmException("Null/empty securerandom.strongAlgorithms Security Property");
        }
        String strGroup = str;
        while (strGroup != null) {
            Matcher matcher = StrongPatternHolder.pattern.matcher(strGroup);
            if (matcher.matches()) {
                String strGroup2 = matcher.group(1);
                String strGroup3 = matcher.group(3);
                try {
                    if (strGroup3 == null) {
                        return getInstance(strGroup2);
                    }
                    return getInstance(strGroup2, strGroup3);
                } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                    strGroup = matcher.group(5);
                }
            } else {
                strGroup = null;
            }
        }
        throw new NoSuchAlgorithmException("No strong SecureRandom impls available: " + str);
    }
}
