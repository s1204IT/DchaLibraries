package java.math;

import dalvik.annotation.optimization.ReachabilitySensitive;
import libcore.util.NativeAllocationRegistry;

final class BigInt {
    private static NativeAllocationRegistry registry = new NativeAllocationRegistry(BigInt.class.getClassLoader(), NativeBN.getNativeFinalizer(), NativeBN.size());

    @ReachabilitySensitive
    private transient long bignum = 0;

    BigInt() {
    }

    public String toString() {
        return decString();
    }

    boolean hasNativeBignum() {
        return this.bignum != 0;
    }

    private void makeValid() {
        if (this.bignum == 0) {
            this.bignum = NativeBN.BN_new();
            registry.registerNativeAllocation(this, this.bignum);
        }
    }

    private static BigInt newBigInt() {
        BigInt bigInt = new BigInt();
        bigInt.bignum = NativeBN.BN_new();
        registry.registerNativeAllocation(bigInt, bigInt.bignum);
        return bigInt;
    }

    static int cmp(BigInt bigInt, BigInt bigInt2) {
        return NativeBN.BN_cmp(bigInt.bignum, bigInt2.bignum);
    }

    void putCopy(BigInt bigInt) {
        makeValid();
        NativeBN.BN_copy(this.bignum, bigInt.bignum);
    }

    BigInt copy() {
        BigInt bigInt = new BigInt();
        bigInt.putCopy(this);
        return bigInt;
    }

    void putLongInt(long j) {
        makeValid();
        NativeBN.putLongInt(this.bignum, j);
    }

    void putULongInt(long j, boolean z) {
        makeValid();
        NativeBN.putULongInt(this.bignum, j, z);
    }

    private NumberFormatException invalidBigInteger(String str) {
        throw new NumberFormatException("Invalid BigInteger: " + str);
    }

    void putDecString(String str) {
        String strCheckString = checkString(str, 10);
        makeValid();
        if (NativeBN.BN_dec2bn(this.bignum, strCheckString) < strCheckString.length()) {
            throw invalidBigInteger(str);
        }
    }

    void putHexString(String str) {
        String strCheckString = checkString(str, 16);
        makeValid();
        if (NativeBN.BN_hex2bn(this.bignum, strCheckString) < strCheckString.length()) {
            throw invalidBigInteger(str);
        }
    }

    String checkString(String str, int i) {
        String str2;
        int i2;
        if (str == null) {
            throw new NullPointerException("s == null");
        }
        int length = str.length();
        boolean z = false;
        if (length <= 0) {
            str2 = str;
            i2 = 0;
        } else {
            char cCharAt = str.charAt(0);
            if (cCharAt == '+') {
                str = str.substring(1);
                length--;
            } else if (cCharAt == '-') {
                str2 = str;
                i2 = 1;
            }
            str2 = str;
            i2 = 0;
        }
        if (length - i2 == 0) {
            throw invalidBigInteger(str2);
        }
        while (i2 < length) {
            char cCharAt2 = str2.charAt(i2);
            if (Character.digit(cCharAt2, i) == -1) {
                throw invalidBigInteger(str2);
            }
            if (cCharAt2 > 128) {
                z = true;
            }
            i2++;
        }
        return z ? toAscii(str2, i) : str2;
    }

    private static String toAscii(String str, int i) {
        int length = str.length();
        StringBuilder sb = new StringBuilder(length);
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            int iDigit = Character.digit(cCharAt, i);
            if (iDigit >= 0 && iDigit <= 9) {
                cCharAt = (char) (48 + iDigit);
            }
            sb.append(cCharAt);
        }
        return sb.toString();
    }

    void putBigEndian(byte[] bArr, boolean z) {
        makeValid();
        NativeBN.BN_bin2bn(bArr, bArr.length, z, this.bignum);
    }

    void putLittleEndianInts(int[] iArr, boolean z) {
        makeValid();
        NativeBN.litEndInts2bn(iArr, iArr.length, z, this.bignum);
    }

    void putBigEndianTwosComplement(byte[] bArr) {
        makeValid();
        NativeBN.twosComp2bn(bArr, bArr.length, this.bignum);
    }

    long longInt() {
        return NativeBN.longInt(this.bignum);
    }

    String decString() {
        return NativeBN.BN_bn2dec(this.bignum);
    }

    String hexString() {
        return NativeBN.BN_bn2hex(this.bignum);
    }

    byte[] bigEndianMagnitude() {
        return NativeBN.BN_bn2bin(this.bignum);
    }

    int[] littleEndianIntsMagnitude() {
        return NativeBN.bn2litEndInts(this.bignum);
    }

    int sign() {
        return NativeBN.sign(this.bignum);
    }

    void setSign(int i) {
        if (i > 0) {
            NativeBN.BN_set_negative(this.bignum, 0);
        } else if (i < 0) {
            NativeBN.BN_set_negative(this.bignum, 1);
        }
    }

    boolean twosCompFitsIntoBytes(int i) {
        return (NativeBN.bitLength(this.bignum) + 7) / 8 <= i;
    }

    int bitLength() {
        return NativeBN.bitLength(this.bignum);
    }

    boolean isBitSet(int i) {
        return NativeBN.BN_is_bit_set(this.bignum, i);
    }

    static BigInt shift(BigInt bigInt, int i) {
        BigInt bigIntNewBigInt = newBigInt();
        NativeBN.BN_shift(bigIntNewBigInt.bignum, bigInt.bignum, i);
        return bigIntNewBigInt;
    }

    void shift(int i) {
        NativeBN.BN_shift(this.bignum, this.bignum, i);
    }

    void addPositiveInt(int i) {
        NativeBN.BN_add_word(this.bignum, i);
    }

    void multiplyByPositiveInt(int i) {
        NativeBN.BN_mul_word(this.bignum, i);
    }

    static int remainderByPositiveInt(BigInt bigInt, int i) {
        return NativeBN.BN_mod_word(bigInt.bignum, i);
    }

    static BigInt addition(BigInt bigInt, BigInt bigInt2) {
        BigInt bigIntNewBigInt = newBigInt();
        NativeBN.BN_add(bigIntNewBigInt.bignum, bigInt.bignum, bigInt2.bignum);
        return bigIntNewBigInt;
    }

    void add(BigInt bigInt) {
        NativeBN.BN_add(this.bignum, this.bignum, bigInt.bignum);
    }

    static BigInt subtraction(BigInt bigInt, BigInt bigInt2) {
        BigInt bigIntNewBigInt = newBigInt();
        NativeBN.BN_sub(bigIntNewBigInt.bignum, bigInt.bignum, bigInt2.bignum);
        return bigIntNewBigInt;
    }

    static BigInt gcd(BigInt bigInt, BigInt bigInt2) {
        BigInt bigIntNewBigInt = newBigInt();
        NativeBN.BN_gcd(bigIntNewBigInt.bignum, bigInt.bignum, bigInt2.bignum);
        return bigIntNewBigInt;
    }

    static BigInt product(BigInt bigInt, BigInt bigInt2) {
        BigInt bigIntNewBigInt = newBigInt();
        NativeBN.BN_mul(bigIntNewBigInt.bignum, bigInt.bignum, bigInt2.bignum);
        return bigIntNewBigInt;
    }

    static BigInt bigExp(BigInt bigInt, BigInt bigInt2) {
        BigInt bigIntNewBigInt = newBigInt();
        NativeBN.BN_exp(bigIntNewBigInt.bignum, bigInt.bignum, bigInt2.bignum);
        return bigIntNewBigInt;
    }

    static BigInt exp(BigInt bigInt, int i) {
        BigInt bigInt2 = new BigInt();
        bigInt2.putLongInt(i);
        return bigExp(bigInt, bigInt2);
    }

    static void division(BigInt bigInt, BigInt bigInt2, BigInt bigInt3, BigInt bigInt4) {
        long j;
        long j2 = 0;
        if (bigInt3 != null) {
            bigInt3.makeValid();
            j = bigInt3.bignum;
        } else {
            j = 0;
        }
        if (bigInt4 != null) {
            bigInt4.makeValid();
            j2 = bigInt4.bignum;
        }
        NativeBN.BN_div(j, j2, bigInt.bignum, bigInt2.bignum);
    }

    static BigInt modulus(BigInt bigInt, BigInt bigInt2) {
        BigInt bigIntNewBigInt = newBigInt();
        NativeBN.BN_nnmod(bigIntNewBigInt.bignum, bigInt.bignum, bigInt2.bignum);
        return bigIntNewBigInt;
    }

    static BigInt modExp(BigInt bigInt, BigInt bigInt2, BigInt bigInt3) {
        BigInt bigIntNewBigInt = newBigInt();
        NativeBN.BN_mod_exp(bigIntNewBigInt.bignum, bigInt.bignum, bigInt2.bignum, bigInt3.bignum);
        return bigIntNewBigInt;
    }

    static BigInt modInverse(BigInt bigInt, BigInt bigInt2) {
        BigInt bigIntNewBigInt = newBigInt();
        NativeBN.BN_mod_inverse(bigIntNewBigInt.bignum, bigInt.bignum, bigInt2.bignum);
        return bigIntNewBigInt;
    }

    static BigInt generatePrimeDefault(int i) {
        BigInt bigIntNewBigInt = newBigInt();
        NativeBN.BN_generate_prime_ex(bigIntNewBigInt.bignum, i, false, 0L, 0L);
        return bigIntNewBigInt;
    }

    boolean isPrime(int i) {
        return NativeBN.BN_primality_test(this.bignum, i, false);
    }
}
