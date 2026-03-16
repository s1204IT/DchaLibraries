package com.android.providers.contacts.aggregation.util;

import java.util.Arrays;

public class NameDistance {
    private final boolean[] mMatchFlags1;
    private final boolean[] mMatchFlags2;
    private final int mMaxLength;
    private final boolean mPrefixOnly;

    public NameDistance(int maxLength) {
        this.mMaxLength = maxLength;
        this.mPrefixOnly = false;
        this.mMatchFlags1 = new boolean[maxLength];
        this.mMatchFlags2 = new boolean[maxLength];
    }

    public NameDistance() {
        this.mPrefixOnly = true;
        this.mMaxLength = 0;
        this.mMatchFlags2 = null;
        this.mMatchFlags1 = null;
    }

    public float getDistance(byte[] bytes1, byte[] bytes2) {
        byte[] array2;
        byte[] array1;
        if (bytes1.length > bytes2.length) {
            array2 = bytes1;
            array1 = bytes2;
        } else {
            array2 = bytes2;
            array1 = bytes1;
        }
        int length1 = array1.length;
        if (length1 >= 3) {
            boolean prefix = true;
            int i = 0;
            while (true) {
                if (i >= array1.length) {
                    break;
                }
                if (array1[i] == array2[i]) {
                    i++;
                } else {
                    prefix = false;
                    break;
                }
            }
            if (prefix) {
                return 1.0f;
            }
        }
        if (this.mPrefixOnly) {
            return 0.0f;
        }
        if (length1 > this.mMaxLength) {
            length1 = this.mMaxLength;
        }
        int length2 = array2.length;
        if (length2 > this.mMaxLength) {
            length2 = this.mMaxLength;
        }
        Arrays.fill(this.mMatchFlags1, 0, length1, false);
        Arrays.fill(this.mMatchFlags2, 0, length2, false);
        int range = (length2 / 2) - 1;
        if (range < 0) {
            range = 0;
        }
        int matches = 0;
        for (int i2 = 0; i2 < length1; i2++) {
            byte c1 = array1[i2];
            int from = i2 - range;
            if (from < 0) {
                from = 0;
            }
            int to = i2 + range + 1;
            if (to > length2) {
                to = length2;
            }
            int j = from;
            while (true) {
                if (j >= to) {
                    break;
                }
                if (this.mMatchFlags2[j] || c1 != array2[j]) {
                    j++;
                } else {
                    boolean[] zArr = this.mMatchFlags1;
                    this.mMatchFlags2[j] = true;
                    zArr[i2] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) {
            return 0.0f;
        }
        int transpositions = 0;
        int j2 = 0;
        for (int i3 = 0; i3 < length1; i3++) {
            if (this.mMatchFlags1[i3]) {
                while (!this.mMatchFlags2[j2]) {
                    j2++;
                }
                if (array1[i3] != array2[j2]) {
                    transpositions++;
                }
                j2++;
            }
        }
        float m = matches;
        float jaro = (((m / length1) + (m / length2)) + ((m - (transpositions / 2.0f)) / m)) / 3.0f;
        if (jaro >= 0.7f) {
            int prefix2 = 0;
            for (int i4 = 0; i4 < length1 && bytes1[i4] == bytes2[i4]; i4++) {
                prefix2++;
            }
            return jaro + (Math.min(0.1f, 1.0f / length2) * prefix2 * (1.0f - jaro));
        }
        return jaro;
    }
}
