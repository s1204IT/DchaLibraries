package com.android.internal.util;

public class CharSequences {
    public static CharSequence forAsciiBytes(final byte[] bArr) {
        return new CharSequence() {
            @Override
            public char charAt(int i) {
                return (char) bArr[i];
            }

            @Override
            public int length() {
                return bArr.length;
            }

            @Override
            public CharSequence subSequence(int i, int i2) {
                return CharSequences.forAsciiBytes(bArr, i, i2);
            }

            @Override
            public String toString() {
                return new String(bArr);
            }
        };
    }

    public static CharSequence forAsciiBytes(final byte[] bArr, final int i, final int i2) {
        validate(i, i2, bArr.length);
        return new CharSequence() {
            @Override
            public char charAt(int i3) {
                return (char) bArr[i3 + i];
            }

            @Override
            public int length() {
                return i2 - i;
            }

            @Override
            public CharSequence subSequence(int i3, int i4) {
                int i5 = i3 - i;
                int i6 = i4 - i;
                CharSequences.validate(i5, i6, length());
                return CharSequences.forAsciiBytes(bArr, i5, i6);
            }

            @Override
            public String toString() {
                return new String(bArr, i, length());
            }
        };
    }

    static void validate(int i, int i2, int i3) {
        if (i < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 > i3) {
            throw new IndexOutOfBoundsException();
        }
        if (i > i2) {
            throw new IndexOutOfBoundsException();
        }
    }

    public static boolean equals(CharSequence charSequence, CharSequence charSequence2) {
        if (charSequence.length() != charSequence2.length()) {
            return false;
        }
        int length = charSequence.length();
        for (int i = 0; i < length; i++) {
            if (charSequence.charAt(i) != charSequence2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static int compareToIgnoreCase(CharSequence charSequence, CharSequence charSequence2) {
        int length = charSequence.length();
        int length2 = charSequence2.length();
        int i = length < length2 ? length : length2;
        int i2 = 0;
        int i3 = 0;
        while (i2 < i) {
            int i4 = i2 + 1;
            int i5 = i3 + 1;
            int lowerCase = Character.toLowerCase(charSequence.charAt(i2)) - Character.toLowerCase(charSequence2.charAt(i3));
            if (lowerCase == 0) {
                i2 = i4;
                i3 = i5;
            } else {
                return lowerCase;
            }
        }
        return length - length2;
    }
}
