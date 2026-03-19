package java.util;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import sun.security.util.DerValue;
import sun.util.locale.LanguageTag;

public final class UUID implements Serializable, Comparable<UUID> {
    static final boolean $assertionsDisabled = false;
    private static final long serialVersionUID = -4856846361193249489L;
    private final long leastSigBits;
    private final long mostSigBits;

    private static class Holder {
        static final SecureRandom numberGenerator = new SecureRandom();

        private Holder() {
        }
    }

    private UUID(byte[] bArr) {
        long j = 0;
        long j2 = 0;
        for (int i = 0; i < 8; i++) {
            j2 = (j2 << 8) | ((long) (bArr[i] & Character.DIRECTIONALITY_UNDEFINED));
        }
        for (int i2 = 8; i2 < 16; i2++) {
            j = (j << 8) | ((long) (bArr[i2] & Character.DIRECTIONALITY_UNDEFINED));
        }
        this.mostSigBits = j2;
        this.leastSigBits = j;
    }

    public UUID(long j, long j2) {
        this.mostSigBits = j;
        this.leastSigBits = j2;
    }

    public static UUID randomUUID() {
        byte[] bArr = new byte[16];
        Holder.numberGenerator.nextBytes(bArr);
        bArr[6] = (byte) (bArr[6] & 15);
        bArr[6] = (byte) (bArr[6] | DerValue.TAG_APPLICATION);
        bArr[8] = (byte) (bArr[8] & 63);
        bArr[8] = (byte) (bArr[8] | 128);
        return new UUID(bArr);
    }

    public static UUID nameUUIDFromBytes(byte[] bArr) {
        try {
            byte[] bArrDigest = MessageDigest.getInstance("MD5").digest(bArr);
            bArrDigest[6] = (byte) (bArrDigest[6] & 15);
            bArrDigest[6] = (byte) (bArrDigest[6] | 48);
            bArrDigest[8] = (byte) (bArrDigest[8] & 63);
            bArrDigest[8] = (byte) (bArrDigest[8] | 128);
            return new UUID(bArrDigest);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("MD5 not supported", e);
        }
    }

    public static UUID fromString(String str) {
        String[] strArrSplit = str.split(LanguageTag.SEP);
        if (strArrSplit.length != 5) {
            throw new IllegalArgumentException("Invalid UUID string: " + str);
        }
        for (int i = 0; i < 5; i++) {
            strArrSplit[i] = "0x" + strArrSplit[i];
        }
        return new UUID((((Long.decode(strArrSplit[0]).longValue() << 16) | Long.decode(strArrSplit[1]).longValue()) << 16) | Long.decode(strArrSplit[2]).longValue(), (Long.decode(strArrSplit[3]).longValue() << 48) | Long.decode(strArrSplit[4]).longValue());
    }

    public long getLeastSignificantBits() {
        return this.leastSigBits;
    }

    public long getMostSignificantBits() {
        return this.mostSigBits;
    }

    public int version() {
        return (int) ((this.mostSigBits >> 12) & 15);
    }

    public int variant() {
        return (int) ((this.leastSigBits >>> ((int) (64 - (this.leastSigBits >>> 62)))) & (this.leastSigBits >> 63));
    }

    public long timestamp() {
        if (version() != 1) {
            throw new UnsupportedOperationException("Not a time-based UUID");
        }
        return ((this.mostSigBits & 4095) << 48) | (((this.mostSigBits >> 16) & 65535) << 32) | (this.mostSigBits >>> 32);
    }

    public int clockSequence() {
        if (version() != 1) {
            throw new UnsupportedOperationException("Not a time-based UUID");
        }
        return (int) ((this.leastSigBits & 4611404543450677248L) >>> 48);
    }

    public long node() {
        if (version() != 1) {
            throw new UnsupportedOperationException("Not a time-based UUID");
        }
        return this.leastSigBits & 281474976710655L;
    }

    public String toString() {
        return digits(this.mostSigBits >> 32, 8) + LanguageTag.SEP + digits(this.mostSigBits >> 16, 4) + LanguageTag.SEP + digits(this.mostSigBits, 4) + LanguageTag.SEP + digits(this.leastSigBits >> 48, 4) + LanguageTag.SEP + digits(this.leastSigBits, 12);
    }

    private static String digits(long j, int i) {
        long j2 = 1 << (i * 4);
        return Long.toHexString((j & (j2 - 1)) | j2).substring(1);
    }

    public int hashCode() {
        long j = this.mostSigBits ^ this.leastSigBits;
        return ((int) j) ^ ((int) (j >> 32));
    }

    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != UUID.class) {
            return $assertionsDisabled;
        }
        UUID uuid = (UUID) obj;
        if (this.mostSigBits == uuid.mostSigBits && this.leastSigBits == uuid.leastSigBits) {
            return true;
        }
        return $assertionsDisabled;
    }

    @Override
    public int compareTo(UUID uuid) {
        if (this.mostSigBits >= uuid.mostSigBits) {
            if (this.mostSigBits > uuid.mostSigBits) {
                return 1;
            }
            if (this.leastSigBits >= uuid.leastSigBits) {
                return this.leastSigBits > uuid.leastSigBits ? 1 : 0;
            }
        }
        return -1;
    }
}
