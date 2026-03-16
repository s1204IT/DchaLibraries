package javax.obex;

public final class ApplicationParameter {
    private int mMaxLength = 1000;
    private byte[] mArray = new byte[this.mMaxLength];
    private int mLength = 0;

    public static class TRIPLET_LENGTH {
        public static final byte FILTER_LENGTH = 8;
        public static final byte FORMAT_LENGTH = 1;
        public static final byte LISTSTARTOFFSET_LENGTH = 2;
        public static final byte MAXLISTCOUNT_LENGTH = 2;
        public static final byte NEWMISSEDCALLS_LENGTH = 1;
        public static final byte ORDER_LENGTH = 1;
        public static final byte PHONEBOOKSIZE_LENGTH = 2;
        public static final byte SEARCH_ATTRIBUTE_LENGTH = 1;
    }

    public static class TRIPLET_TAGID {
        public static final byte FILTER_TAGID = 6;
        public static final byte FORMAT_TAGID = 7;
        public static final byte LISTSTARTOFFSET_TAGID = 5;
        public static final byte MAXLISTCOUNT_TAGID = 4;
        public static final byte NEWMISSEDCALLS_TAGID = 9;
        public static final byte ORDER_TAGID = 1;
        public static final byte PHONEBOOKSIZE_TAGID = 8;
        public static final byte SEARCH_ATTRIBUTE_TAGID = 3;
        public static final byte SEARCH_VALUE_TAGID = 2;
    }

    public static class TRIPLET_VALUE {

        public static class FORMAT {
            public static final byte VCARD_VERSION_21 = 0;
            public static final byte VCARD_VERSION_30 = 1;
        }

        public static class ORDER {
            public static final byte ORDER_BY_ALPHANUMERIC = 1;
            public static final byte ORDER_BY_INDEX = 0;
            public static final byte ORDER_BY_PHONETIC = 2;
        }

        public static class SEARCHATTRIBUTE {
            public static final byte SEARCH_BY_NAME = 0;
            public static final byte SEARCH_BY_NUMBER = 1;
            public static final byte SEARCH_BY_SOUND = 2;
        }
    }

    public void addAPPHeader(byte tag, byte len, byte[] value) {
        if (this.mLength + len + 2 > this.mMaxLength) {
            byte[] array_tmp = new byte[this.mLength + (len * 4)];
            System.arraycopy(this.mArray, 0, array_tmp, 0, this.mLength);
            this.mArray = array_tmp;
            this.mMaxLength = this.mLength + (len * 4);
        }
        byte[] bArr = this.mArray;
        int i = this.mLength;
        this.mLength = i + 1;
        bArr[i] = tag;
        byte[] bArr2 = this.mArray;
        int i2 = this.mLength;
        this.mLength = i2 + 1;
        bArr2[i2] = len;
        System.arraycopy(value, 0, this.mArray, this.mLength, (int) len);
        this.mLength += len;
    }

    public byte[] getAPPparam() {
        byte[] para = new byte[this.mLength];
        System.arraycopy(this.mArray, 0, para, 0, this.mLength);
        return para;
    }
}
