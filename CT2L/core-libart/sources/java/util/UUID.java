package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import libcore.io.Memory;

public final class UUID implements Serializable, Comparable<UUID> {
    private static SecureRandom rng = null;
    private static final long serialVersionUID = -4856846361193249489L;
    private transient int clockSequence;
    private transient int hash;
    private long leastSigBits;
    private long mostSigBits;
    private transient long node;
    private transient long timestamp;
    private transient int variant;
    private transient int version;

    public UUID(long mostSigBits, long leastSigBits) {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
        init();
    }

    private void init() {
        int msbHash = (int) (this.mostSigBits ^ (this.mostSigBits >>> 32));
        int lsbHash = (int) (this.leastSigBits ^ (this.leastSigBits >>> 32));
        this.hash = msbHash ^ lsbHash;
        if ((this.leastSigBits & Long.MIN_VALUE) == 0) {
            this.variant = 0;
        } else if ((this.leastSigBits & 4611686018427387904L) != 0) {
            this.variant = (int) ((this.leastSigBits & (-2305843009213693952L)) >>> 61);
        } else {
            this.variant = 2;
        }
        this.version = (int) ((this.mostSigBits & 61440) >>> 12);
        if (this.variant == 2 || this.version == 1) {
            long timeLow = (this.mostSigBits & (-4294967296L)) >>> 32;
            long timeMid = (this.mostSigBits & 4294901760L) << 16;
            long timeHigh = (this.mostSigBits & 4095) << 48;
            this.timestamp = timeLow | timeMid | timeHigh;
            this.clockSequence = (int) ((this.leastSigBits & 4611404543450677248L) >>> 48);
            this.node = this.leastSigBits & 281474976710655L;
        }
    }

    public static UUID randomUUID() {
        byte[] data = new byte[16];
        synchronized (UUID.class) {
            if (rng == null) {
                rng = new SecureRandom();
            }
        }
        rng.nextBytes(data);
        return makeUuid(data, 4);
    }

    public static UUID nameUUIDFromBytes(byte[] name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return makeUuid(md.digest(name), 3);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static UUID makeUuid(byte[] hash, int version) {
        long msb = Memory.peekLong(hash, 0, ByteOrder.BIG_ENDIAN);
        long lsb = Memory.peekLong(hash, 8, ByteOrder.BIG_ENDIAN);
        return new UUID((msb & (-61441)) | (((long) version) << 12), (lsb & 4611686018427387903L) | Long.MIN_VALUE);
    }

    public static UUID fromString(String uuid) {
        if (uuid == null) {
            throw new NullPointerException("uuid == null");
        }
        String[] parts = uuid.split("-");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid UUID: " + uuid);
        }
        long m1 = Long.parsePositiveLong(parts[0], 16);
        long m2 = Long.parsePositiveLong(parts[1], 16);
        long m3 = Long.parsePositiveLong(parts[2], 16);
        long lsb1 = Long.parsePositiveLong(parts[3], 16);
        long lsb2 = Long.parsePositiveLong(parts[4], 16);
        long msb = (m1 << 32) | (m2 << 16) | m3;
        long lsb = (lsb1 << 48) | lsb2;
        return new UUID(msb, lsb);
    }

    public long getLeastSignificantBits() {
        return this.leastSigBits;
    }

    public long getMostSignificantBits() {
        return this.mostSigBits;
    }

    public int version() {
        return this.version;
    }

    public int variant() {
        return this.variant;
    }

    public long timestamp() {
        if (this.version != 1) {
            throw new UnsupportedOperationException();
        }
        return this.timestamp;
    }

    public int clockSequence() {
        if (this.version != 1) {
            throw new UnsupportedOperationException();
        }
        return this.clockSequence;
    }

    public long node() {
        if (this.version != 1) {
            throw new UnsupportedOperationException();
        }
        return this.node;
    }

    @Override
    public int compareTo(UUID uuid) {
        if (uuid == this) {
            return 0;
        }
        if (this.mostSigBits != uuid.mostSigBits) {
            return this.mostSigBits >= uuid.mostSigBits ? 1 : -1;
        }
        if (this.leastSigBits != uuid.leastSigBits) {
            return this.leastSigBits >= uuid.leastSigBits ? 1 : -1;
        }
        return 0;
    }

    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (!(object instanceof UUID)) {
            return false;
        }
        UUID that = (UUID) object;
        return this.leastSigBits == that.leastSigBits && this.mostSigBits == that.mostSigBits;
    }

    public int hashCode() {
        return this.hash;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(36);
        String msbStr = Long.toHexString(this.mostSigBits);
        if (msbStr.length() < 16) {
            int diff = 16 - msbStr.length();
            for (int i = 0; i < diff; i++) {
                builder.append('0');
            }
        }
        builder.append(msbStr);
        builder.insert(8, '-');
        builder.insert(13, '-');
        builder.append('-');
        String lsbStr = Long.toHexString(this.leastSigBits);
        if (lsbStr.length() < 16) {
            int diff2 = 16 - lsbStr.length();
            for (int i2 = 0; i2 < diff2; i2++) {
                builder.append('0');
            }
        }
        builder.append(lsbStr);
        builder.insert(23, '-');
        return builder.toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init();
    }
}
