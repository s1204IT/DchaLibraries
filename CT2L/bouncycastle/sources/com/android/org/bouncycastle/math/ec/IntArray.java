package com.android.org.bouncycastle.math.ec;

import com.android.org.bouncycastle.util.Arrays;
import java.math.BigInteger;

class IntArray {
    private static final String ZEROES = "00000000000000000000000000000000";
    private int[] m_ints;
    private static final int[] INTERLEAVE_TABLE = {0, 1, 4, 5, 16, 17, 20, 21, 64, 65, 68, 69, 80, 81, 84, 85, 256, 257, 260, 261, 272, 273, 276, 277, 320, 321, 324, 325, 336, 337, 340, 341, 1024, 1025, 1028, 1029, 1040, 1041, 1044, 1045, 1088, 1089, 1092, 1093, 1104, 1105, 1108, 1109, 1280, 1281, 1284, 1285, 1296, 1297, 1300, 1301, 1344, 1345, 1348, 1349, 1360, 1361, 1364, 1365, 4096, 4097, 4100, 4101, 4112, 4113, 4116, 4117, 4160, 4161, 4164, 4165, 4176, 4177, 4180, 4181, 4352, 4353, 4356, 4357, 4368, 4369, 4372, 4373, 4416, 4417, 4420, 4421, 4432, 4433, 4436, 4437, 5120, 5121, 5124, 5125, 5136, 5137, 5140, 5141, 5184, 5185, 5188, 5189, 5200, 5201, 5204, 5205, 5376, 5377, 5380, 5381, 5392, 5393, 5396, 5397, 5440, 5441, 5444, 5445, 5456, 5457, 5460, 5461, 16384, 16385, 16388, 16389, 16400, 16401, 16404, 16405, 16448, 16449, 16452, 16453, 16464, 16465, 16468, 16469, 16640, 16641, 16644, 16645, 16656, 16657, 16660, 16661, 16704, 16705, 16708, 16709, 16720, 16721, 16724, 16725, 17408, 17409, 17412, 17413, 17424, 17425, 17428, 17429, 17472, 17473, 17476, 17477, 17488, 17489, 17492, 17493, 17664, 17665, 17668, 17669, 17680, 17681, 17684, 17685, 17728, 17729, 17732, 17733, 17744, 17745, 17748, 17749, 20480, 20481, 20484, 20485, 20496, 20497, 20500, 20501, 20544, 20545, 20548, 20549, 20560, 20561, 20564, 20565, 20736, 20737, 20740, 20741, 20752, 20753, 20756, 20757, 20800, 20801, 20804, 20805, 20816, 20817, 20820, 20821, 21504, 21505, 21508, 21509, 21520, 21521, 21524, 21525, 21568, 21569, 21572, 21573, 21584, 21585, 21588, 21589, 21760, 21761, 21764, 21765, 21776, 21777, 21780, 21781, 21824, 21825, 21828, 21829, 21840, 21841, 21844, 21845};
    private static final byte[] bitLengths = {0, 1, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8};

    public static int getWordLength(int bits) {
        return (bits + 31) >>> 5;
    }

    public IntArray(int intLen) {
        this.m_ints = new int[intLen];
    }

    public IntArray(int[] ints) {
        this.m_ints = ints;
    }

    public IntArray(BigInteger bigInt) {
        int barrI;
        if (bigInt == null || bigInt.signum() < 0) {
            throw new IllegalArgumentException("invalid F2m field value");
        }
        if (bigInt.signum() == 0) {
            this.m_ints = new int[]{0};
            return;
        }
        byte[] barr = bigInt.toByteArray();
        int barrLen = barr.length;
        int barrStart = 0;
        if (barr[0] == 0) {
            barrLen--;
            barrStart = 1;
        }
        int intLen = (barrLen + 3) / 4;
        this.m_ints = new int[intLen];
        int iarrJ = intLen - 1;
        int rem = (barrLen % 4) + barrStart;
        int temp = 0;
        int barrI2 = barrStart;
        if (barrStart < rem) {
            while (barrI2 < rem) {
                int barrBarrI = barr[barrI2] & 255;
                temp = (temp << 8) | barrBarrI;
                barrI2++;
            }
            this.m_ints[iarrJ] = temp;
            iarrJ--;
        }
        while (iarrJ >= 0) {
            int temp2 = 0;
            int i = 0;
            while (true) {
                barrI = barrI2;
                if (i < 4) {
                    barrI2 = barrI + 1;
                    int barrBarrI2 = barr[barrI] & 255;
                    temp2 = (temp2 << 8) | barrBarrI2;
                    i++;
                }
            }
            this.m_ints[iarrJ] = temp2;
            iarrJ--;
            barrI2 = barrI;
        }
    }

    public boolean isZero() {
        int[] a = this.m_ints;
        for (int i : a) {
            if (i != 0) {
                return false;
            }
        }
        return true;
    }

    public int getUsedLength() {
        return getUsedLengthFrom(this.m_ints.length);
    }

    public int getUsedLengthFrom(int from) {
        int[] a = this.m_ints;
        int from2 = Math.min(from, a.length);
        if (from2 < 1) {
            return 0;
        }
        if (a[0] != 0) {
            do {
                from2--;
            } while (a[from2] == 0);
            return from2 + 1;
        }
        do {
            from2--;
            if (a[from2] != 0) {
                return from2 + 1;
            }
        } while (from2 > 0);
        return 0;
    }

    public int degree() {
        int i = this.m_ints.length;
        while (i != 0) {
            i--;
            int w = this.m_ints[i];
            if (w != 0) {
                return (i << 5) + bitLength(w);
            }
        }
        return 0;
    }

    private static int bitLength(int w) {
        int t = w >>> 16;
        if (t == 0) {
            int t2 = w >>> 8;
            return t2 == 0 ? bitLengths[w] : bitLengths[t2] + 8;
        }
        int u = t >>> 8;
        return u == 0 ? bitLengths[t] + Tnaf.POW_2_WIDTH : bitLengths[u] + 24;
    }

    private int[] resizedInts(int newLen) {
        int[] newInts = new int[newLen];
        System.arraycopy(this.m_ints, 0, newInts, 0, Math.min(this.m_ints.length, newLen));
        return newInts;
    }

    public BigInteger toBigInteger() {
        int barrI;
        int usedLen = getUsedLength();
        if (usedLen == 0) {
            return ECConstants.ZERO;
        }
        int highestInt = this.m_ints[usedLen - 1];
        byte[] temp = new byte[4];
        boolean trailingZeroBytesDone = false;
        int j = 3;
        int barrI2 = 0;
        while (j >= 0) {
            byte thisByte = (byte) (highestInt >>> (j * 8));
            if (trailingZeroBytesDone || thisByte != 0) {
                trailingZeroBytesDone = true;
                barrI = barrI2 + 1;
                temp[barrI2] = thisByte;
            } else {
                barrI = barrI2;
            }
            j--;
            barrI2 = barrI;
        }
        int barrLen = ((usedLen - 1) * 4) + barrI2;
        byte[] barr = new byte[barrLen];
        for (int j2 = 0; j2 < barrI2; j2++) {
            barr[j2] = temp[j2];
        }
        int iarrJ = usedLen - 2;
        int barrI3 = barrI2;
        while (iarrJ >= 0) {
            int j3 = 3;
            int barrI4 = barrI3;
            while (j3 >= 0) {
                barr[barrI4] = (byte) (this.m_ints[iarrJ] >>> (j3 * 8));
                j3--;
                barrI4++;
            }
            iarrJ--;
            barrI3 = barrI4;
        }
        return new BigInteger(1, barr);
    }

    private static int shiftLeft(int[] x, int count) {
        int prev = 0;
        for (int i = 0; i < count; i++) {
            int next = x[i];
            x[i] = (next << 1) | prev;
            prev = next >>> 31;
        }
        return prev;
    }

    public void addOneShifted(int shift) {
        if (shift >= this.m_ints.length) {
            this.m_ints = resizedInts(shift + 1);
        }
        int[] iArr = this.m_ints;
        iArr[shift] = iArr[shift] ^ 1;
    }

    private void addShiftedByBits(IntArray other, int bits) {
        int words = bits >>> 5;
        int shift = bits & 31;
        if (shift == 0) {
            addShiftedByWords(other, words);
            return;
        }
        int otherUsedLen = other.getUsedLength();
        if (otherUsedLen != 0) {
            int minLen = otherUsedLen + words + 1;
            if (minLen > this.m_ints.length) {
                this.m_ints = resizedInts(minLen);
            }
            int shiftInv = 32 - shift;
            int prev = 0;
            for (int i = 0; i < otherUsedLen; i++) {
                int next = other.m_ints[i];
                int[] iArr = this.m_ints;
                int i2 = i + words;
                iArr[i2] = iArr[i2] ^ ((next << shift) | prev);
                prev = next >>> shiftInv;
            }
            int[] iArr2 = this.m_ints;
            int i3 = otherUsedLen + words;
            iArr2[i3] = iArr2[i3] ^ prev;
        }
    }

    private static int addShiftedByBits(int[] x, int[] y, int count, int shift) {
        int shiftInv = 32 - shift;
        int prev = 0;
        for (int i = 0; i < count; i++) {
            int next = y[i];
            x[i] = x[i] ^ ((next << shift) | prev);
            prev = next >>> shiftInv;
        }
        return prev;
    }

    private static int addShiftedByBits(int[] x, int xOff, int[] y, int yOff, int count, int shift) {
        int shiftInv = 32 - shift;
        int prev = 0;
        for (int i = 0; i < count; i++) {
            int next = y[yOff + i];
            int i2 = xOff + i;
            x[i2] = x[i2] ^ ((next << shift) | prev);
            prev = next >>> shiftInv;
        }
        return prev;
    }

    public void addShiftedByWords(IntArray other, int words) {
        int otherUsedLen = other.getUsedLength();
        if (otherUsedLen != 0) {
            int minLen = otherUsedLen + words;
            if (minLen > this.m_ints.length) {
                this.m_ints = resizedInts(minLen);
            }
            for (int i = 0; i < otherUsedLen; i++) {
                int[] iArr = this.m_ints;
                int i2 = words + i;
                iArr[i2] = iArr[i2] ^ other.m_ints[i];
            }
        }
    }

    private static void addShiftedByWords(int[] x, int xOff, int[] y, int count) {
        for (int i = 0; i < count; i++) {
            int i2 = xOff + i;
            x[i2] = x[i2] ^ y[i];
        }
    }

    private static void add(int[] x, int[] y, int count) {
        for (int i = 0; i < count; i++) {
            x[i] = x[i] ^ y[i];
        }
    }

    private static void distribute(int[] x, int dst1, int dst2, int src, int count) {
        for (int i = 0; i < count; i++) {
            int v = x[src + i];
            int i2 = dst1 + i;
            x[i2] = x[i2] ^ v;
            int i3 = dst2 + i;
            x[i3] = x[i3] ^ v;
        }
    }

    public int getLength() {
        return this.m_ints.length;
    }

    public void flipWord(int bit, int word) {
        int len = this.m_ints.length;
        int n = bit >>> 5;
        if (n < len) {
            int shift = bit & 31;
            if (shift == 0) {
                int[] iArr = this.m_ints;
                iArr[n] = iArr[n] ^ word;
                return;
            }
            int[] iArr2 = this.m_ints;
            iArr2[n] = iArr2[n] ^ (word << shift);
            int n2 = n + 1;
            if (n2 < len) {
                int[] iArr3 = this.m_ints;
                iArr3[n2] = iArr3[n2] ^ (word >>> (32 - shift));
            }
        }
    }

    public int getWord(int bit) {
        int len = this.m_ints.length;
        int n = bit >>> 5;
        if (n >= len) {
            return 0;
        }
        int shift = bit & 31;
        if (shift == 0) {
            return this.m_ints[n];
        }
        int result = this.m_ints[n] >>> shift;
        int n2 = n + 1;
        if (n2 < len) {
            return result | (this.m_ints[n2] << (32 - shift));
        }
        return result;
    }

    public boolean testBitZero() {
        return this.m_ints.length > 0 && (this.m_ints[0] & 1) != 0;
    }

    public boolean testBit(int n) {
        int theInt = n >>> 5;
        int theBit = n & 31;
        int tester = 1 << theBit;
        return (this.m_ints[theInt] & tester) != 0;
    }

    public void flipBit(int n) {
        int theInt = n >>> 5;
        int theBit = n & 31;
        int flipper = 1 << theBit;
        int[] iArr = this.m_ints;
        iArr[theInt] = iArr[theInt] ^ flipper;
    }

    public void setBit(int n) {
        int theInt = n >>> 5;
        int theBit = n & 31;
        int setter = 1 << theBit;
        int[] iArr = this.m_ints;
        iArr[theInt] = iArr[theInt] | setter;
    }

    public void clearBit(int n) {
        int theInt = n >>> 5;
        int theBit = n & 31;
        int setter = 1 << theBit;
        int[] iArr = this.m_ints;
        iArr[theInt] = iArr[theInt] & (setter ^ (-1));
    }

    public IntArray multiply(IntArray other, int m) {
        int aLen = getUsedLength();
        if (aLen == 0) {
            return new IntArray(1);
        }
        int bLen = other.getUsedLength();
        if (bLen == 0) {
            return new IntArray(1);
        }
        IntArray A = this;
        IntArray B = other;
        if (aLen > bLen) {
            A = other;
            B = this;
            aLen = bLen;
            bLen = aLen;
        }
        if (aLen == 1) {
            int a = A.m_ints[0];
            int[] b = B.m_ints;
            int[] c = new int[aLen + bLen];
            if ((a & 1) != 0) {
                add(c, b, bLen);
            }
            int k = 1;
            while (true) {
                a >>>= 1;
                if (a != 0) {
                    if ((a & 1) != 0) {
                        addShiftedByBits(c, b, bLen, k);
                    }
                    k++;
                } else {
                    return new IntArray(c);
                }
            }
        } else {
            int complexity = aLen <= 8 ? 1 : 2;
            int width = 1 << complexity;
            int shifts = 32 >>> complexity;
            int bExt = bLen;
            if ((B.m_ints[bLen - 1] >>> (33 - shifts)) != 0) {
                bExt++;
            }
            int cLen = bExt + aLen;
            int[] c2 = new int[cLen << width];
            System.arraycopy(B.m_ints, 0, c2, 0, bLen);
            interleave(A.m_ints, 0, c2, bExt, aLen, complexity);
            int[] ci = new int[1 << width];
            for (int i = 1; i < ci.length; i++) {
                ci[i] = ci[i - 1] + cLen;
            }
            int MASK = (1 << width) - 1;
            int k2 = 0;
            while (true) {
                for (int aPos = 0; aPos < aLen; aPos++) {
                    int index = (c2[bExt + aPos] >>> k2) & MASK;
                    if (index != 0) {
                        addShiftedByWords(c2, ci[index] + aPos, c2, bExt);
                    }
                }
                k2 += width;
                if (k2 >= 32) {
                    break;
                }
                shiftLeft(c2, bExt);
            }
            int ciPos = ci.length;
            int pow2 = ciPos >>> 1;
            int offset = 32;
            while (true) {
                ciPos--;
                if (ciPos > 1) {
                    if (ciPos == pow2) {
                        offset -= shifts;
                        addShiftedByBits(c2, ci[1], c2, ci[pow2], cLen, offset);
                        pow2 >>>= 1;
                    } else {
                        distribute(c2, ci[pow2], ci[ciPos - pow2], ci[ciPos], cLen);
                    }
                } else {
                    IntArray p = new IntArray(cLen);
                    System.arraycopy(c2, ci[1], p.m_ints, 0, cLen);
                    return p;
                }
            }
        }
    }

    public void reduce(int m, int[] ks) {
        int len = getUsedLength();
        int mLen = (m + 31) >>> 5;
        if (len >= mLen) {
            int _2m = m << 1;
            int pos = Math.min(_2m - 2, (len << 5) - 1);
            int kMax = ks[ks.length - 1];
            if (kMax < m - 31) {
                reduceWordWise(pos, m, ks);
            } else {
                reduceBitWise(pos, m, ks);
            }
            int partial = m & 31;
            if (partial != 0) {
                int[] iArr = this.m_ints;
                int i = mLen - 1;
                iArr[i] = iArr[i] & ((1 << partial) - 1);
            }
            if (len > mLen) {
                this.m_ints = resizedInts(mLen);
            }
        }
    }

    private void reduceBitWise(int from, int m, int[] ks) {
        for (int i = from; i >= m; i--) {
            if (testBit(i)) {
                int bit = i - m;
                flipBit(bit);
                int j = ks.length;
                while (true) {
                    j--;
                    if (j >= 0) {
                        flipBit(ks[j] + bit);
                    }
                }
            }
        }
    }

    private void reduceWordWise(int from, int m, int[] ks) {
        int pos = m + ((from - m) & (-32));
        for (int i = pos; i >= m; i -= 32) {
            int word = getWord(i);
            if (word != 0) {
                int bit = i - m;
                flipWord(bit, word);
                int j = ks.length;
                while (true) {
                    j--;
                    if (j >= 0) {
                        flipWord(ks[j] + bit, word);
                    }
                }
            }
        }
    }

    public IntArray square(int m) {
        int len = getUsedLength();
        if (len != 0) {
            int _2len = len << 1;
            int[] r = new int[_2len];
            int pos = 0;
            while (pos < _2len) {
                int mi = this.m_ints[pos >>> 1];
                int pos2 = pos + 1;
                r[pos] = interleave16(65535 & mi);
                pos = pos2 + 1;
                r[pos2] = interleave16(mi >>> 16);
            }
            return new IntArray(r);
        }
        return this;
    }

    private static void interleave(int[] x, int xOff, int[] z, int zOff, int count, int rounds) {
        for (int i = 0; i < count; i++) {
            z[zOff + i] = interleave(x[xOff + i], rounds);
        }
    }

    private static int interleave(int x, int rounds) {
        while (true) {
            rounds--;
            if (rounds >= 0) {
                x = interleave16(65535 & x) | (interleave16(x >>> 16) << 1);
            } else {
                return x;
            }
        }
    }

    private static int interleave16(int n) {
        return INTERLEAVE_TABLE[n & 255] | (INTERLEAVE_TABLE[n >>> 8] << 16);
    }

    public IntArray modInverse(int m, int[] ks) {
        int uzDegree = degree();
        if (uzDegree != 1) {
            IntArray uz = (IntArray) clone();
            int t = getWordLength(m);
            IntArray vz = new IntArray(t);
            vz.setBit(m);
            vz.setBit(0);
            vz.setBit(ks[0]);
            if (ks.length > 1) {
                vz.setBit(ks[1]);
                vz.setBit(ks[2]);
            }
            IntArray g1z = new IntArray(t);
            g1z.setBit(0);
            IntArray g2z = new IntArray(t);
            while (uzDegree != 0) {
                int j = uzDegree - vz.degree();
                if (j < 0) {
                    IntArray uzCopy = uz;
                    uz = vz;
                    vz = uzCopy;
                    IntArray g1zCopy = g1z;
                    g1z = g2z;
                    g2z = g1zCopy;
                    j = -j;
                }
                uz.addShiftedByBits(vz, j);
                uzDegree = uz.degree();
                if (uzDegree != 0) {
                    g1z.addShiftedByBits(g2z, j);
                }
            }
            return g2z;
        }
        return this;
    }

    public boolean equals(Object o) {
        if (!(o instanceof IntArray)) {
            return false;
        }
        IntArray other = (IntArray) o;
        int usedLen = getUsedLength();
        if (other.getUsedLength() != usedLen) {
            return false;
        }
        for (int i = 0; i < usedLen; i++) {
            if (this.m_ints[i] != other.m_ints[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int usedLen = getUsedLength();
        int hash = 1;
        for (int i = 0; i < usedLen; i++) {
            hash = (hash * 31) ^ this.m_ints[i];
        }
        return hash;
    }

    public Object clone() {
        return new IntArray(Arrays.clone(this.m_ints));
    }

    public String toString() {
        int i = getUsedLength();
        if (i == 0) {
            return "0";
        }
        int i2 = i - 1;
        StringBuffer sb = new StringBuffer(Integer.toBinaryString(this.m_ints[i2]));
        while (true) {
            i2--;
            if (i2 >= 0) {
                String s = Integer.toBinaryString(this.m_ints[i2]);
                int len = s.length();
                if (len < 32) {
                    sb.append(ZEROES.substring(len));
                }
                sb.append(s);
            } else {
                return sb.toString();
            }
        }
    }
}
