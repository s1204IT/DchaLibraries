package android.icu.text;

import android.icu.lang.UCharacterEnums;

public final class CollationKey implements Comparable<CollationKey> {

    static final boolean f65assertionsDisabled;
    private static final int MERGE_SEPERATOR_ = 2;
    private int m_hashCode_;
    private byte[] m_key_;
    private int m_length_;
    private String m_source_;

    static {
        f65assertionsDisabled = !CollationKey.class.desiredAssertionStatus();
    }

    public static final class BoundMode {
        public static final int COUNT = 3;
        public static final int LOWER = 0;
        public static final int UPPER = 1;
        public static final int UPPER_LONG = 2;

        private BoundMode() {
        }
    }

    public CollationKey(String source, byte[] key) {
        this(source, key, -1);
    }

    private CollationKey(String source, byte[] key, int length) {
        this.m_source_ = source;
        this.m_key_ = key;
        this.m_hashCode_ = 0;
        this.m_length_ = length;
    }

    public CollationKey(String source, RawCollationKey key) {
        this.m_source_ = source;
        this.m_length_ = key.size - 1;
        this.m_key_ = key.releaseBytes();
        if (!f65assertionsDisabled) {
            if (!(this.m_key_[this.m_length_] == 0)) {
                throw new AssertionError();
            }
        }
        this.m_hashCode_ = 0;
    }

    public String getSourceString() {
        return this.m_source_;
    }

    public byte[] toByteArray() {
        int length = getLength() + 1;
        byte[] result = new byte[length];
        System.arraycopy(this.m_key_, 0, result, 0, length);
        return result;
    }

    @Override
    public int compareTo(CollationKey target) {
        int i = 0;
        while (true) {
            int l = this.m_key_[i] & 255;
            int r = target.m_key_[i] & 255;
            if (l < r) {
                return -1;
            }
            if (l > r) {
                return 1;
            }
            if (l == 0) {
                return 0;
            }
            i++;
        }
    }

    public boolean equals(Object target) {
        if (!(target instanceof CollationKey)) {
            return false;
        }
        return equals((CollationKey) target);
    }

    public boolean equals(CollationKey target) {
        if (this == target) {
            return true;
        }
        if (target == null) {
            return false;
        }
        for (int i = 0; this.m_key_[i] == target.m_key_[i]; i++) {
            if (this.m_key_[i] == 0) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        if (this.m_hashCode_ == 0) {
            if (this.m_key_ == null) {
                this.m_hashCode_ = 1;
            } else {
                int size = this.m_key_.length >> 1;
                StringBuilder key = new StringBuilder(size);
                int i = 0;
                while (this.m_key_[i] != 0 && this.m_key_[i + 1] != 0) {
                    key.append((char) ((this.m_key_[i] << 8) | (this.m_key_[i + 1] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)));
                    i += 2;
                }
                if (this.m_key_[i] != 0) {
                    key.append((char) (this.m_key_[i] << 8));
                }
                this.m_hashCode_ = key.toString().hashCode();
            }
        }
        return this.m_hashCode_;
    }

    public CollationKey getBound(int boundType, int noOfLevels) {
        int offset;
        int offset2;
        int offset3 = 0;
        int keystrength = 0;
        if (noOfLevels <= 0) {
            offset = 0;
        } else {
            while (offset3 < this.m_key_.length && this.m_key_[offset3] != 0) {
                int offset4 = offset3 + 1;
                if (this.m_key_[offset3] == 1) {
                    keystrength++;
                    noOfLevels--;
                    if (noOfLevels == 0 || offset4 == this.m_key_.length || this.m_key_[offset4] == 0) {
                        offset = offset4 - 1;
                        break;
                    }
                }
                offset3 = offset4;
            }
            offset = offset3;
        }
        if (noOfLevels > 0) {
            throw new IllegalArgumentException("Source collation key has only " + keystrength + " strength level. Call getBound() again  with noOfLevels < " + keystrength);
        }
        byte[] resultkey = new byte[offset + boundType + 1];
        System.arraycopy(this.m_key_, 0, resultkey, 0, offset);
        switch (boundType) {
            case 0:
                offset2 = offset;
                break;
            case 1:
                offset2 = offset + 1;
                resultkey[offset] = 2;
                break;
            case 2:
                int offset5 = offset + 1;
                resultkey[offset] = -1;
                resultkey[offset5] = -1;
                offset2 = offset5 + 1;
                break;
            default:
                throw new IllegalArgumentException("Illegal boundType argument");
        }
        resultkey[offset2] = 0;
        return new CollationKey(null, resultkey, offset2);
    }

    public CollationKey merge(CollationKey source) {
        int rindex;
        if (source == null || source.getLength() == 0) {
            throw new IllegalArgumentException("CollationKey argument can not be null or of 0 length");
        }
        byte[] result = new byte[getLength() + source.getLength() + 2];
        int rindex2 = 0;
        int index = 0;
        int sourceindex = 0;
        while (true) {
            if (this.m_key_[index] < 0 || this.m_key_[index] >= 2) {
                result[rindex2] = this.m_key_[index];
                index++;
                rindex2++;
            } else {
                int rindex3 = rindex2 + 1;
                result[rindex2] = 2;
                while (true) {
                    rindex = rindex3;
                    if (source.m_key_[sourceindex] >= 0 && source.m_key_[sourceindex] < 2) {
                        break;
                    }
                    rindex3 = rindex + 1;
                    result[rindex] = source.m_key_[sourceindex];
                    sourceindex++;
                }
                if (this.m_key_[index] != 1 || source.m_key_[sourceindex] != 1) {
                    break;
                }
                index++;
                sourceindex++;
                result[rindex] = 1;
                rindex2 = rindex + 1;
            }
        }
        int remainingLength = this.m_length_ - index;
        if (remainingLength > 0) {
            System.arraycopy(this.m_key_, index, result, rindex, remainingLength);
            rindex += remainingLength;
        } else {
            int remainingLength2 = source.m_length_ - sourceindex;
            if (remainingLength2 > 0) {
                System.arraycopy(source.m_key_, sourceindex, result, rindex, remainingLength2);
                rindex += remainingLength2;
            }
        }
        result[rindex] = 0;
        if (!f65assertionsDisabled) {
            if (!(rindex == result.length + (-1))) {
                throw new AssertionError();
            }
        }
        return new CollationKey(null, result, rindex);
    }

    private int getLength() {
        if (this.m_length_ >= 0) {
            return this.m_length_;
        }
        int length = this.m_key_.length;
        int index = 0;
        while (true) {
            if (index >= length) {
                break;
            }
            if (this.m_key_[index] != 0) {
                index++;
            } else {
                length = index;
                break;
            }
        }
        this.m_length_ = length;
        return this.m_length_;
    }
}
