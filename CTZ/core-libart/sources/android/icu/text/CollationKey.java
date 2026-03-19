package android.icu.text;

import android.icu.lang.UCharacterEnums;

public final class CollationKey implements Comparable<CollationKey> {
    static final boolean $assertionsDisabled = false;
    private static final int MERGE_SEPERATOR_ = 2;
    private int m_hashCode_;
    private byte[] m_key_;
    private int m_length_;
    private String m_source_;

    public static final class BoundMode {

        @Deprecated
        public static final int COUNT = 3;
        public static final int LOWER = 0;
        public static final int UPPER = 1;
        public static final int UPPER_LONG = 2;

        private BoundMode() {
        }
    }

    public CollationKey(String str, byte[] bArr) {
        this(str, bArr, -1);
    }

    private CollationKey(String str, byte[] bArr, int i) {
        this.m_source_ = str;
        this.m_key_ = bArr;
        this.m_hashCode_ = 0;
        this.m_length_ = i;
    }

    public CollationKey(String str, RawCollationKey rawCollationKey) {
        this.m_source_ = str;
        this.m_length_ = rawCollationKey.size - 1;
        this.m_key_ = rawCollationKey.releaseBytes();
        this.m_hashCode_ = 0;
    }

    public String getSourceString() {
        return this.m_source_;
    }

    public byte[] toByteArray() {
        int length = getLength() + 1;
        byte[] bArr = new byte[length];
        System.arraycopy(this.m_key_, 0, bArr, 0, length);
        return bArr;
    }

    @Override
    public int compareTo(CollationKey collationKey) {
        int i = 0;
        while (true) {
            int i2 = this.m_key_[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            int i3 = collationKey.m_key_[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            if (i2 < i3) {
                return -1;
            }
            if (i2 > i3) {
                return 1;
            }
            if (i2 != 0) {
                i++;
            } else {
                return 0;
            }
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof CollationKey)) {
            return false;
        }
        return equals((CollationKey) obj);
    }

    public boolean equals(CollationKey collationKey) {
        if (this == collationKey) {
            return true;
        }
        if (collationKey == null) {
            return false;
        }
        for (int i = 0; this.m_key_[i] == collationKey.m_key_[i]; i++) {
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
                StringBuilder sb = new StringBuilder(this.m_key_.length >> 1);
                int i = 0;
                while (this.m_key_[i] != 0) {
                    int i2 = i + 1;
                    if (this.m_key_[i2] == 0) {
                        break;
                    }
                    sb.append((char) ((this.m_key_[i] << 8) | (this.m_key_[i2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)));
                    i += 2;
                }
                if (this.m_key_[i] != 0) {
                    sb.append((char) (this.m_key_[i] << 8));
                }
                this.m_hashCode_ = sb.toString().hashCode();
            }
        }
        return this.m_hashCode_;
    }

    public CollationKey getBound(int i, int i2) {
        int i3;
        int i4;
        int i5;
        if (i2 > 0) {
            i3 = i2;
            i4 = 0;
            i5 = 0;
            while (i4 < this.m_key_.length && this.m_key_[i4] != 0) {
                int i6 = i4 + 1;
                if (this.m_key_[i4] == 1) {
                    i5++;
                    i3--;
                    if (i3 == 0 || i6 == this.m_key_.length || this.m_key_[i6] == 0) {
                        i4 = i6 - 1;
                        break;
                    }
                }
                i4 = i6;
            }
        } else {
            i3 = i2;
            i4 = 0;
            i5 = 0;
        }
        if (i3 > 0) {
            throw new IllegalArgumentException("Source collation key has only " + i5 + " strength level. Call getBound() again  with noOfLevels < " + i5);
        }
        byte[] bArr = new byte[i4 + i + 1];
        System.arraycopy(this.m_key_, 0, bArr, 0, i4);
        switch (i) {
            case 0:
                break;
            case 1:
                bArr[i4] = 2;
                i4++;
                break;
            case 2:
                int i7 = i4 + 1;
                bArr[i4] = -1;
                i4 = i7 + 1;
                bArr[i7] = -1;
                break;
            default:
                throw new IllegalArgumentException("Illegal boundType argument");
        }
        bArr[i4] = 0;
        return new CollationKey(null, bArr, i4);
    }

    public CollationKey merge(CollationKey collationKey) {
        int i;
        if (collationKey == null || collationKey.getLength() == 0) {
            throw new IllegalArgumentException("CollationKey argument can not be null or of 0 length");
        }
        byte[] bArr = new byte[getLength() + collationKey.getLength() + 2];
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (true) {
            if (this.m_key_[i2] < 0 || this.m_key_[i2] >= 2) {
                bArr[i3] = this.m_key_[i2];
                i3++;
                i2++;
            } else {
                i = i3 + 1;
                bArr[i3] = 2;
                while (true) {
                    if (collationKey.m_key_[i4] >= 0 && collationKey.m_key_[i4] < 2) {
                        break;
                    }
                    bArr[i] = collationKey.m_key_[i4];
                    i++;
                    i4++;
                }
                if (this.m_key_[i2] != 1 || collationKey.m_key_[i4] != 1) {
                    break;
                }
                i2++;
                i4++;
                i3 = i + 1;
                bArr[i] = 1;
            }
        }
        int i5 = this.m_length_ - i2;
        if (i5 > 0) {
            System.arraycopy(this.m_key_, i2, bArr, i, i5);
            i += i5;
        } else {
            int i6 = collationKey.m_length_ - i4;
            if (i6 > 0) {
                System.arraycopy(collationKey.m_key_, i4, bArr, i, i6);
                i += i6;
            }
        }
        bArr[i] = 0;
        return new CollationKey(null, bArr, i);
    }

    private int getLength() {
        if (this.m_length_ >= 0) {
            return this.m_length_;
        }
        int length = this.m_key_.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            if (this.m_key_[i] != 0) {
                i++;
            } else {
                length = i;
                break;
            }
        }
        this.m_length_ = length;
        return this.m_length_;
    }
}
