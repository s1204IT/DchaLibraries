package java.math;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

public class BigInteger extends Number implements Comparable<BigInteger>, Serializable {
    private static final long serialVersionUID = -8287574255936472291L;
    private transient BigInt bigInt;
    transient int[] digits;
    private transient int firstNonzeroDigit;
    private transient int hashCode;
    private transient boolean javaIsValid;
    private byte[] magnitude;
    private transient boolean nativeIsValid;
    transient int numberLength;
    transient int sign;
    private int signum;
    public static final BigInteger ZERO = new BigInteger(0, 0);
    public static final BigInteger ONE = new BigInteger(1, 1);
    public static final BigInteger TEN = new BigInteger(1, 10);
    static final BigInteger MINUS_ONE = new BigInteger(-1, 1);
    static final BigInteger[] SMALL_VALUES = {ZERO, ONE, new BigInteger(1, 2), new BigInteger(1, 3), new BigInteger(1, 4), new BigInteger(1, 5), new BigInteger(1, 6), new BigInteger(1, 7), new BigInteger(1, 8), new BigInteger(1, 9), TEN};

    BigInteger(BigInt bigInt) {
        this.nativeIsValid = false;
        this.javaIsValid = false;
        this.firstNonzeroDigit = -2;
        this.hashCode = 0;
        if (bigInt == null || !bigInt.hasNativeBignum()) {
            throw new AssertionError();
        }
        setBigInt(bigInt);
    }

    BigInteger(int i, long j) {
        this.nativeIsValid = false;
        this.javaIsValid = false;
        this.firstNonzeroDigit = -2;
        this.hashCode = 0;
        BigInt bigInt = new BigInt();
        bigInt.putULongInt(j, i < 0);
        setBigInt(bigInt);
    }

    BigInteger(int i, int i2, int[] iArr) {
        this.nativeIsValid = false;
        this.javaIsValid = false;
        this.firstNonzeroDigit = -2;
        this.hashCode = 0;
        setJavaRepresentation(i, i2, iArr);
    }

    public BigInteger(int i, Random random) {
        this.nativeIsValid = false;
        this.javaIsValid = false;
        this.firstNonzeroDigit = -2;
        this.hashCode = 0;
        if (i < 0) {
            throw new IllegalArgumentException("numBits < 0: " + i);
        }
        if (i == 0) {
            setJavaRepresentation(0, 1, new int[]{0});
        } else {
            int i2 = (i + 31) >> 5;
            int[] iArr = new int[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                iArr[i3] = random.nextInt();
            }
            int i4 = i2 - 1;
            iArr[i4] = iArr[i4] >>> ((-i) & 31);
            setJavaRepresentation(1, i2, iArr);
        }
        this.javaIsValid = true;
    }

    public BigInteger(int i, int i2, Random random) {
        int iNextInt;
        this.nativeIsValid = false;
        this.javaIsValid = false;
        this.firstNonzeroDigit = -2;
        this.hashCode = 0;
        if (i < 2) {
            throw new ArithmeticException("bitLength < 2: " + i);
        }
        if (i < 16) {
            do {
                iNextInt = (random.nextInt() & ((1 << i) - 1)) | (1 << (i - 1));
                iNextInt = i > 2 ? iNextInt | 1 : iNextInt;
            } while (!isSmallPrime(iNextInt));
            BigInt bigInt = new BigInt();
            bigInt.putULongInt(iNextInt, false);
            setBigInt(bigInt);
            return;
        }
        do {
            setBigInt(BigInt.generatePrimeDefault(i));
        } while (bitLength() != i);
    }

    private static boolean isSmallPrime(int i) {
        if (i == 2) {
            return true;
        }
        if (i % 2 == 0) {
            return false;
        }
        int iSqrt = (int) Math.sqrt(i);
        for (int i2 = 3; i2 <= iSqrt; i2 += 2) {
            if (i % i2 == 0) {
                return false;
            }
        }
        return true;
    }

    public BigInteger(String str) {
        this.nativeIsValid = false;
        this.javaIsValid = false;
        this.firstNonzeroDigit = -2;
        this.hashCode = 0;
        BigInt bigInt = new BigInt();
        bigInt.putDecString(str);
        setBigInt(bigInt);
    }

    public BigInteger(String str, int i) {
        this.nativeIsValid = false;
        this.javaIsValid = false;
        this.firstNonzeroDigit = -2;
        this.hashCode = 0;
        if (str == null) {
            throw new NullPointerException("value == null");
        }
        if (i == 10) {
            BigInt bigInt = new BigInt();
            bigInt.putDecString(str);
            setBigInt(bigInt);
        } else if (i == 16) {
            BigInt bigInt2 = new BigInt();
            bigInt2.putHexString(str);
            setBigInt(bigInt2);
        } else {
            if (i < 2 || i > 36) {
                throw new NumberFormatException("Invalid radix: " + i);
            }
            if (str.isEmpty()) {
                throw new NumberFormatException("value.isEmpty()");
            }
            parseFromString(this, str, i);
        }
    }

    public BigInteger(int i, byte[] bArr) {
        this.nativeIsValid = false;
        this.javaIsValid = false;
        this.firstNonzeroDigit = -2;
        this.hashCode = 0;
        if (bArr == null) {
            throw new NullPointerException("magnitude == null");
        }
        if (i < -1 || i > 1) {
            throw new NumberFormatException("Invalid signum: " + i);
        }
        if (i == 0) {
            for (byte b : bArr) {
                if (b != 0) {
                    throw new NumberFormatException("signum-magnitude mismatch");
                }
            }
        }
        BigInt bigInt = new BigInt();
        bigInt.putBigEndian(bArr, i < 0);
        setBigInt(bigInt);
    }

    public BigInteger(byte[] bArr) {
        this.nativeIsValid = false;
        this.javaIsValid = false;
        this.firstNonzeroDigit = -2;
        this.hashCode = 0;
        if (bArr.length == 0) {
            throw new NumberFormatException("value.length == 0");
        }
        BigInt bigInt = new BigInt();
        bigInt.putBigEndianTwosComplement(bArr);
        setBigInt(bigInt);
    }

    BigInt getBigInt() {
        if (this.nativeIsValid) {
            return this.bigInt;
        }
        synchronized (this) {
            if (this.nativeIsValid) {
                return this.bigInt;
            }
            BigInt bigInt = new BigInt();
            bigInt.putLittleEndianInts(this.digits, this.sign < 0);
            setBigInt(bigInt);
            return bigInt;
        }
    }

    private void setBigInt(BigInt bigInt) {
        this.bigInt = bigInt;
        this.nativeIsValid = true;
    }

    private void setJavaRepresentation(int i, int i2, int[] iArr) {
        while (i2 > 0) {
            i2--;
            if (iArr[i2] != 0) {
                break;
            }
        }
        int i3 = i2 + 1;
        if (iArr[i2] == 0) {
            i = 0;
        }
        this.sign = i;
        this.digits = iArr;
        this.numberLength = i3;
        this.javaIsValid = true;
    }

    void prepareJavaRepresentation() {
        if (this.javaIsValid) {
            return;
        }
        synchronized (this) {
            if (this.javaIsValid) {
                return;
            }
            int iSign = this.bigInt.sign();
            int[] iArrLittleEndianIntsMagnitude = iSign != 0 ? this.bigInt.littleEndianIntsMagnitude() : new int[]{0};
            setJavaRepresentation(iSign, iArrLittleEndianIntsMagnitude.length, iArrLittleEndianIntsMagnitude);
        }
    }

    public static BigInteger valueOf(long j) {
        if (j < 0) {
            if (j != -1) {
                return new BigInteger(-1, -j);
            }
            return MINUS_ONE;
        }
        if (j < SMALL_VALUES.length) {
            return SMALL_VALUES[(int) j];
        }
        return new BigInteger(1, j);
    }

    public byte[] toByteArray() {
        return twosComplement();
    }

    public BigInteger abs() {
        BigInt bigInt = getBigInt();
        if (bigInt.sign() >= 0) {
            return this;
        }
        BigInt bigIntCopy = bigInt.copy();
        bigIntCopy.setSign(1);
        return new BigInteger(bigIntCopy);
    }

    public BigInteger negate() {
        BigInt bigInt = getBigInt();
        int iSign = bigInt.sign();
        if (iSign == 0) {
            return this;
        }
        BigInt bigIntCopy = bigInt.copy();
        bigIntCopy.setSign(-iSign);
        return new BigInteger(bigIntCopy);
    }

    public BigInteger add(BigInteger bigInteger) {
        BigInt bigInt = getBigInt();
        BigInt bigInt2 = bigInteger.getBigInt();
        if (bigInt2.sign() == 0) {
            return this;
        }
        if (bigInt.sign() == 0) {
            return bigInteger;
        }
        return new BigInteger(BigInt.addition(bigInt, bigInt2));
    }

    public BigInteger subtract(BigInteger bigInteger) {
        BigInt bigInt = getBigInt();
        BigInt bigInt2 = bigInteger.getBigInt();
        if (bigInt2.sign() == 0) {
            return this;
        }
        return new BigInteger(BigInt.subtraction(bigInt, bigInt2));
    }

    public int signum() {
        if (this.javaIsValid) {
            return this.sign;
        }
        return getBigInt().sign();
    }

    public BigInteger shiftRight(int i) {
        return shiftLeft(-i);
    }

    public BigInteger shiftLeft(int i) {
        int iSignum;
        if (i == 0 || (iSignum = signum()) == 0) {
            return this;
        }
        if (iSignum > 0 || i >= 0) {
            return new BigInteger(BigInt.shift(getBigInt(), i));
        }
        return BitLevel.shiftRight(this, -i);
    }

    BigInteger shiftLeftOneBit() {
        return signum() == 0 ? this : BitLevel.shiftLeftOneBit(this);
    }

    public int bitLength() {
        if (!this.nativeIsValid && this.javaIsValid) {
            return BitLevel.bitLength(this);
        }
        return getBigInt().bitLength();
    }

    public boolean testBit(int i) {
        if (i < 0) {
            throw new ArithmeticException("n < 0: " + i);
        }
        int iSignum = signum();
        if (iSignum > 0 && this.nativeIsValid && !this.javaIsValid) {
            return getBigInt().isBitSet(i);
        }
        prepareJavaRepresentation();
        if (i == 0) {
            return (this.digits[0] & 1) != 0;
        }
        int i2 = i >> 5;
        if (i2 >= this.numberLength) {
            return iSignum < 0;
        }
        int i3 = this.digits[i2];
        int i4 = 1 << (i & 31);
        if (iSignum < 0) {
            int firstNonzeroDigit = getFirstNonzeroDigit();
            if (i2 < firstNonzeroDigit) {
                return false;
            }
            if (firstNonzeroDigit == i2) {
                i3 = -i3;
            } else {
                i3 = ~i3;
            }
        }
        return (i4 & i3) != 0;
    }

    public BigInteger setBit(int i) {
        prepareJavaRepresentation();
        if (!testBit(i)) {
            return BitLevel.flipBit(this, i);
        }
        return this;
    }

    public BigInteger clearBit(int i) {
        prepareJavaRepresentation();
        if (testBit(i)) {
            return BitLevel.flipBit(this, i);
        }
        return this;
    }

    public BigInteger flipBit(int i) {
        prepareJavaRepresentation();
        if (i < 0) {
            throw new ArithmeticException("n < 0: " + i);
        }
        return BitLevel.flipBit(this, i);
    }

    public int getLowestSetBit() {
        prepareJavaRepresentation();
        if (this.sign == 0) {
            return -1;
        }
        int firstNonzeroDigit = getFirstNonzeroDigit();
        return (firstNonzeroDigit << 5) + Integer.numberOfTrailingZeros(this.digits[firstNonzeroDigit]);
    }

    public int bitCount() {
        prepareJavaRepresentation();
        return BitLevel.bitCount(this);
    }

    public BigInteger not() {
        prepareJavaRepresentation();
        return Logical.not(this);
    }

    public BigInteger and(BigInteger bigInteger) {
        prepareJavaRepresentation();
        bigInteger.prepareJavaRepresentation();
        return Logical.and(this, bigInteger);
    }

    public BigInteger or(BigInteger bigInteger) {
        prepareJavaRepresentation();
        bigInteger.prepareJavaRepresentation();
        return Logical.or(this, bigInteger);
    }

    public BigInteger xor(BigInteger bigInteger) {
        prepareJavaRepresentation();
        bigInteger.prepareJavaRepresentation();
        return Logical.xor(this, bigInteger);
    }

    public BigInteger andNot(BigInteger bigInteger) {
        prepareJavaRepresentation();
        bigInteger.prepareJavaRepresentation();
        return Logical.andNot(this, bigInteger);
    }

    @Override
    public int intValue() {
        if (this.nativeIsValid && this.bigInt.twosCompFitsIntoBytes(4)) {
            return (int) this.bigInt.longInt();
        }
        prepareJavaRepresentation();
        return this.sign * this.digits[0];
    }

    @Override
    public long longValue() {
        long j;
        if (this.nativeIsValid && this.bigInt.twosCompFitsIntoBytes(8)) {
            return this.bigInt.longInt();
        }
        prepareJavaRepresentation();
        if (this.numberLength > 1) {
            j = (((long) this.digits[0]) & 4294967295L) | (((long) this.digits[1]) << 32);
        } else {
            j = ((long) this.digits[0]) & 4294967295L;
        }
        return ((long) this.sign) * j;
    }

    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    @Override
    public double doubleValue() {
        return Conversion.bigInteger2Double(this);
    }

    @Override
    public int compareTo(BigInteger bigInteger) {
        return BigInt.cmp(getBigInt(), bigInteger.getBigInt());
    }

    public BigInteger min(BigInteger bigInteger) {
        return compareTo(bigInteger) == -1 ? this : bigInteger;
    }

    public BigInteger max(BigInteger bigInteger) {
        return compareTo(bigInteger) == 1 ? this : bigInteger;
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            prepareJavaRepresentation();
            int i = 0;
            for (int i2 = 0; i2 < this.numberLength; i2++) {
                i = (i * 33) + this.digits[i2];
            }
            this.hashCode = i * this.sign;
        }
        return this.hashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof BigInteger) && compareTo((BigInteger) obj) == 0;
    }

    public String toString() {
        return getBigInt().decString();
    }

    public String toString(int i) {
        if (i == 10) {
            return getBigInt().decString();
        }
        prepareJavaRepresentation();
        return Conversion.bigInteger2String(this, i);
    }

    public BigInteger gcd(BigInteger bigInteger) {
        return new BigInteger(BigInt.gcd(getBigInt(), bigInteger.getBigInt()));
    }

    public BigInteger multiply(BigInteger bigInteger) {
        return new BigInteger(BigInt.product(getBigInt(), bigInteger.getBigInt()));
    }

    public BigInteger pow(int i) {
        if (i < 0) {
            throw new ArithmeticException("exp < 0: " + i);
        }
        return new BigInteger(BigInt.exp(getBigInt(), i));
    }

    public BigInteger[] divideAndRemainder(BigInteger bigInteger) {
        BigInt bigInt = bigInteger.getBigInt();
        BigInt bigInt2 = new BigInt();
        BigInt bigInt3 = new BigInt();
        BigInt.division(getBigInt(), bigInt, bigInt2, bigInt3);
        return new BigInteger[]{new BigInteger(bigInt2), new BigInteger(bigInt3)};
    }

    public BigInteger divide(BigInteger bigInteger) {
        BigInt bigInt = new BigInt();
        BigInt.division(getBigInt(), bigInteger.getBigInt(), bigInt, null);
        return new BigInteger(bigInt);
    }

    public BigInteger remainder(BigInteger bigInteger) {
        BigInt bigInt = new BigInt();
        BigInt.division(getBigInt(), bigInteger.getBigInt(), null, bigInt);
        return new BigInteger(bigInt);
    }

    public BigInteger modInverse(BigInteger bigInteger) {
        if (bigInteger.signum() <= 0) {
            throw new ArithmeticException("modulus not positive");
        }
        return new BigInteger(BigInt.modInverse(getBigInt(), bigInteger.getBigInt()));
    }

    public BigInteger modPow(BigInteger bigInteger, BigInteger bigInteger2) {
        if (bigInteger2.signum() <= 0) {
            throw new ArithmeticException("modulus.signum() <= 0");
        }
        int iSignum = bigInteger.signum();
        if (iSignum == 0) {
            return ONE.mod(bigInteger2);
        }
        return new BigInteger(BigInt.modExp((iSignum < 0 ? modInverse(bigInteger2) : this).getBigInt(), bigInteger.getBigInt(), bigInteger2.getBigInt()));
    }

    public BigInteger mod(BigInteger bigInteger) {
        if (bigInteger.signum() <= 0) {
            throw new ArithmeticException("m.signum() <= 0");
        }
        return new BigInteger(BigInt.modulus(getBigInt(), bigInteger.getBigInt()));
    }

    public boolean isProbablePrime(int i) {
        if (i <= 0) {
            return true;
        }
        return getBigInt().isPrime(i);
    }

    public BigInteger nextProbablePrime() {
        if (this.sign < 0) {
            throw new ArithmeticException("sign < 0");
        }
        return Primality.nextProbablePrime(this);
    }

    public static BigInteger probablePrime(int i, Random random) {
        return new BigInteger(i, 100, random);
    }

    private byte[] twosComplement() {
        int i;
        int i2;
        prepareJavaRepresentation();
        if (this.sign == 0) {
            return new byte[]{0};
        }
        int iBitLength = bitLength();
        int firstNonzeroDigit = getFirstNonzeroDigit();
        int i3 = (iBitLength >> 3) + 1;
        byte[] bArr = new byte[i3];
        if (i3 - (this.numberLength << 2) != 1) {
            i = i3 & 3;
            if (i == 0) {
                i = 4;
            }
            i2 = 0;
        } else {
            bArr[0] = (byte) (this.sign < 0 ? -1 : 0);
            i2 = 1;
            i = 4;
        }
        int i4 = i3 - (firstNonzeroDigit << 2);
        if (this.sign < 0) {
            int i5 = -this.digits[firstNonzeroDigit];
            int i6 = firstNonzeroDigit + 1;
            i = i6 == this.numberLength ? i : 4;
            int i7 = i5;
            int i8 = i4;
            int i9 = 0;
            while (i9 < i) {
                i8--;
                bArr[i8] = (byte) i7;
                i9++;
                i7 >>= 8;
            }
            while (i8 > i2) {
                int i10 = ~this.digits[i6];
                i6++;
                if (i6 == this.numberLength) {
                    i = i;
                }
                int i11 = i10;
                int i12 = 0;
                while (i12 < i) {
                    i8--;
                    bArr[i8] = (byte) i11;
                    i12++;
                    i11 >>= 8;
                }
            }
        } else {
            while (i4 > i2) {
                int i13 = this.digits[firstNonzeroDigit];
                firstNonzeroDigit++;
                if (firstNonzeroDigit == this.numberLength) {
                    i = i;
                }
                int i14 = i13;
                int i15 = i4;
                int i16 = 0;
                while (i16 < i) {
                    i15--;
                    bArr[i15] = (byte) i14;
                    i16++;
                    i14 >>= 8;
                }
                i4 = i15;
            }
        }
        return bArr;
    }

    static int multiplyByInt(int[] iArr, int[] iArr2, int i, int i2) {
        long j = 0;
        for (int i3 = 0; i3 < i; i3++) {
            long j2 = j + ((((long) iArr2[i3]) & 4294967295L) * (4294967295L & ((long) i2)));
            iArr[i3] = (int) j2;
            j = j2 >>> 32;
        }
        return (int) j;
    }

    static int inplaceAdd(int[] iArr, int i, int i2) {
        long j = ((long) i2) & 4294967295L;
        for (int i3 = 0; j != 0 && i3 < i; i3++) {
            long j2 = j + (((long) iArr[i3]) & 4294967295L);
            iArr[i3] = (int) j2;
            j = j2 >> 32;
        }
        return (int) j;
    }

    private static void parseFromString(BigInteger bigInteger, String str, int i) {
        int i2;
        int i3;
        int length = str.length();
        int i4 = 0;
        int i5 = 1;
        if (str.charAt(0) == '-') {
            i2 = length - 1;
            i3 = -1;
        } else {
            i2 = length;
            i3 = 1;
            i5 = 0;
        }
        int i6 = Conversion.digitFitInInt[i];
        int i7 = i2 / i6;
        int i8 = i2 % i6;
        if (i8 != 0) {
            i7++;
        }
        int[] iArr = new int[i7];
        int i9 = Conversion.bigRadices[i - 2];
        if (i8 == 0) {
            i8 = i6;
        }
        int i10 = i8 + i5;
        while (i5 < length) {
            iArr[i4] = multiplyByInt(iArr, iArr, i4, i9) + inplaceAdd(iArr, i4, Integer.parseInt(str.substring(i5, i10), i));
            int i11 = i10;
            i10 += i6;
            i4++;
            i5 = i11;
        }
        bigInteger.setJavaRepresentation(i3, i4, iArr);
    }

    int getFirstNonzeroDigit() {
        int i;
        if (this.firstNonzeroDigit == -2) {
            if (this.sign == 0) {
                i = -1;
            } else {
                i = 0;
                while (this.digits[i] == 0) {
                    i++;
                }
            }
            this.firstNonzeroDigit = i;
        }
        return this.firstNonzeroDigit;
    }

    BigInteger copy() {
        prepareJavaRepresentation();
        int[] iArr = new int[this.numberLength];
        System.arraycopy(this.digits, 0, iArr, 0, this.numberLength);
        return new BigInteger(this.sign, this.numberLength, iArr);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        BigInt bigInt = new BigInt();
        bigInt.putBigEndian(this.magnitude, this.signum < 0);
        setBigInt(bigInt);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        BigInt bigInt = getBigInt();
        this.signum = bigInt.sign();
        this.magnitude = bigInt.bigEndianMagnitude();
        objectOutputStream.defaultWriteObject();
    }
}
