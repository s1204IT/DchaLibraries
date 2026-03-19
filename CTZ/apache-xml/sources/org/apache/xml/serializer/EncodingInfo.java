package org.apache.xml.serializer;

public final class EncodingInfo {
    final String javaName;
    private InEncoding m_encoding;
    private final char m_highCharInContiguousGroup;
    final String name;

    private interface InEncoding {
        boolean isInEncoding(char c);

        boolean isInEncoding(char c, char c2);
    }

    public boolean isInEncoding(char c) {
        if (this.m_encoding == null) {
            this.m_encoding = new EncodingImpl();
        }
        return this.m_encoding.isInEncoding(c);
    }

    public boolean isInEncoding(char c, char c2) {
        if (this.m_encoding == null) {
            this.m_encoding = new EncodingImpl();
        }
        return this.m_encoding.isInEncoding(c, c2);
    }

    public EncodingInfo(String str, String str2, char c) {
        this.name = str;
        this.javaName = str2;
        this.m_highCharInContiguousGroup = c;
    }

    private class EncodingImpl implements InEncoding {
        private static final int RANGE = 128;
        private InEncoding m_after;
        private final boolean[] m_alreadyKnown;
        private InEncoding m_before;
        private final String m_encoding;
        private final int m_explFirst;
        private final int m_explLast;
        private final int m_first;
        private final boolean[] m_isInEncoding;
        private final int m_last;

        @Override
        public boolean isInEncoding(char c) {
            int codePoint = Encodings.toCodePoint(c);
            if (codePoint < this.m_explFirst) {
                if (this.m_before == null) {
                    this.m_before = EncodingInfo.this.new EncodingImpl(this.m_encoding, this.m_first, this.m_explFirst - 1, codePoint);
                }
                return this.m_before.isInEncoding(c);
            }
            if (this.m_explLast < codePoint) {
                if (this.m_after == null) {
                    this.m_after = EncodingInfo.this.new EncodingImpl(this.m_encoding, this.m_explLast + 1, this.m_last, codePoint);
                }
                return this.m_after.isInEncoding(c);
            }
            int i = codePoint - this.m_explFirst;
            if (!this.m_alreadyKnown[i]) {
                boolean zInEncoding = EncodingInfo.inEncoding(c, this.m_encoding);
                this.m_alreadyKnown[i] = true;
                this.m_isInEncoding[i] = zInEncoding;
                return zInEncoding;
            }
            return this.m_isInEncoding[i];
        }

        @Override
        public boolean isInEncoding(char c, char c2) {
            int codePoint = Encodings.toCodePoint(c, c2);
            if (codePoint < this.m_explFirst) {
                if (this.m_before == null) {
                    this.m_before = EncodingInfo.this.new EncodingImpl(this.m_encoding, this.m_first, this.m_explFirst - 1, codePoint);
                }
                return this.m_before.isInEncoding(c, c2);
            }
            if (this.m_explLast < codePoint) {
                if (this.m_after == null) {
                    this.m_after = EncodingInfo.this.new EncodingImpl(this.m_encoding, this.m_explLast + 1, this.m_last, codePoint);
                }
                return this.m_after.isInEncoding(c, c2);
            }
            int i = codePoint - this.m_explFirst;
            if (!this.m_alreadyKnown[i]) {
                boolean zInEncoding = EncodingInfo.inEncoding(c, c2, this.m_encoding);
                this.m_alreadyKnown[i] = true;
                this.m_isInEncoding[i] = zInEncoding;
                return zInEncoding;
            }
            return this.m_isInEncoding[i];
        }

        private EncodingImpl(EncodingInfo encodingInfo) {
            this(encodingInfo.javaName, 0, Integer.MAX_VALUE, 0);
        }

        private EncodingImpl(String str, int i, int i2, int i3) {
            this.m_alreadyKnown = new boolean[128];
            this.m_isInEncoding = new boolean[128];
            this.m_first = i;
            this.m_last = i2;
            this.m_explFirst = i3;
            this.m_explLast = i3 + 127;
            this.m_encoding = str;
            if (EncodingInfo.this.javaName != null) {
                if (this.m_explFirst >= 0 && this.m_explFirst <= 127 && ("UTF8".equals(EncodingInfo.this.javaName) || "UTF-16".equals(EncodingInfo.this.javaName) || "ASCII".equals(EncodingInfo.this.javaName) || "US-ASCII".equals(EncodingInfo.this.javaName) || "Unicode".equals(EncodingInfo.this.javaName) || "UNICODE".equals(EncodingInfo.this.javaName) || EncodingInfo.this.javaName.startsWith("ISO8859"))) {
                    for (int i4 = 1; i4 < 127; i4++) {
                        int i5 = i4 - this.m_explFirst;
                        if (i5 >= 0 && i5 < 128) {
                            this.m_alreadyKnown[i5] = true;
                            this.m_isInEncoding[i5] = true;
                        }
                    }
                }
                if (EncodingInfo.this.javaName == null) {
                    for (int i6 = 0; i6 < this.m_alreadyKnown.length; i6++) {
                        this.m_alreadyKnown[i6] = true;
                        this.m_isInEncoding[i6] = true;
                    }
                }
            }
        }
    }

    private static boolean inEncoding(char c, String str) {
        try {
            return inEncoding(c, new String(new char[]{c}).getBytes(str));
        } catch (Exception e) {
            return str == null;
        }
    }

    private static boolean inEncoding(char c, char c2, String str) {
        try {
            return inEncoding(c, new String(new char[]{c, c2}).getBytes(str));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean inEncoding(char c, byte[] bArr) {
        if (bArr == null || bArr.length == 0 || bArr[0] == 0) {
            return false;
        }
        if (bArr[0] == 63 && c != '?') {
            return false;
        }
        return true;
    }

    public final char getHighChar() {
        return this.m_highCharInContiguousGroup;
    }
}
