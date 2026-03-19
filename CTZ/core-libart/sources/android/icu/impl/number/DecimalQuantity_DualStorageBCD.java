package android.icu.impl.number;

import android.icu.text.DateFormat;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class DecimalQuantity_DualStorageBCD extends DecimalQuantity_AbstractBCD {
    static final boolean $assertionsDisabled = false;
    private byte[] bcdBytes;
    private long bcdLong = 0;
    private boolean usingBytes = false;

    @Override
    public int maxRepresentableDigits() {
        return Integer.MAX_VALUE;
    }

    public DecimalQuantity_DualStorageBCD() {
        setBcdToZero();
        this.flags = (byte) 0;
    }

    public DecimalQuantity_DualStorageBCD(long j) {
        setToLong(j);
    }

    public DecimalQuantity_DualStorageBCD(int i) {
        setToInt(i);
    }

    public DecimalQuantity_DualStorageBCD(double d) {
        setToDouble(d);
    }

    public DecimalQuantity_DualStorageBCD(BigInteger bigInteger) {
        setToBigInteger(bigInteger);
    }

    public DecimalQuantity_DualStorageBCD(BigDecimal bigDecimal) {
        setToBigDecimal(bigDecimal);
    }

    public DecimalQuantity_DualStorageBCD(DecimalQuantity_DualStorageBCD decimalQuantity_DualStorageBCD) {
        copyFrom(decimalQuantity_DualStorageBCD);
    }

    public DecimalQuantity_DualStorageBCD(Number number) {
        if (number instanceof Long) {
            setToLong(number.longValue());
            return;
        }
        if (number instanceof Integer) {
            setToInt(number.intValue());
            return;
        }
        if (number instanceof Double) {
            setToDouble(number.doubleValue());
            return;
        }
        if (number instanceof BigInteger) {
            setToBigInteger((BigInteger) number);
            return;
        }
        if (number instanceof BigDecimal) {
            setToBigDecimal((BigDecimal) number);
        } else {
            if (number instanceof android.icu.math.BigDecimal) {
                setToBigDecimal(((android.icu.math.BigDecimal) number).toBigDecimal());
                return;
            }
            throw new IllegalArgumentException("Number is of an unsupported type: " + number.getClass().getName());
        }
    }

    @Override
    public DecimalQuantity createCopy() {
        return new DecimalQuantity_DualStorageBCD(this);
    }

    @Override
    protected byte getDigitPos(int i) {
        if (this.usingBytes) {
            if (i < 0 || i > this.precision) {
                return (byte) 0;
            }
            return this.bcdBytes[i];
        }
        if (i < 0 || i >= 16) {
            return (byte) 0;
        }
        return (byte) ((this.bcdLong >>> (i * 4)) & 15);
    }

    @Override
    protected void setDigitPos(int i, byte b) {
        if (this.usingBytes) {
            ensureCapacity(i + 1);
            this.bcdBytes[i] = b;
        } else if (i >= 16) {
            switchStorage();
            ensureCapacity(i + 1);
            this.bcdBytes[i] = b;
        } else {
            int i2 = i * 4;
            this.bcdLong = (((long) b) << i2) | (this.bcdLong & (~(15 << i2)));
        }
    }

    @Override
    protected void shiftLeft(int i) {
        if (!this.usingBytes && this.precision + i > 16) {
            switchStorage();
        }
        if (this.usingBytes) {
            ensureCapacity(this.precision + i);
            int i2 = (this.precision + i) - 1;
            while (i2 >= i) {
                this.bcdBytes[i2] = this.bcdBytes[i2 - i];
                i2--;
            }
            while (i2 >= 0) {
                this.bcdBytes[i2] = 0;
                i2--;
            }
        } else {
            this.bcdLong <<= i * 4;
        }
        this.scale -= i;
        this.precision += i;
    }

    @Override
    protected void shiftRight(int i) {
        if (this.usingBytes) {
            int i2 = 0;
            while (i2 < this.precision - i) {
                this.bcdBytes[i2] = this.bcdBytes[i2 + i];
                i2++;
            }
            while (i2 < this.precision) {
                this.bcdBytes[i2] = 0;
                i2++;
            }
        } else {
            this.bcdLong >>>= i * 4;
        }
        this.scale += i;
        this.precision -= i;
    }

    @Override
    protected void setBcdToZero() {
        if (this.usingBytes) {
            this.bcdBytes = null;
            this.usingBytes = false;
        }
        this.bcdLong = 0L;
        this.scale = 0;
        this.precision = 0;
        this.isApproximate = false;
        this.origDouble = 0.0d;
        this.origDelta = 0;
    }

    @Override
    protected void readIntToBcd(int i) {
        long j = 0;
        int i2 = 16;
        while (i != 0) {
            j = (j >>> 4) + ((((long) i) % 10) << 60);
            i /= 10;
            i2--;
        }
        this.bcdLong = j >>> (i2 * 4);
        this.scale = 0;
        this.precision = 16 - i2;
    }

    @Override
    protected void readLongToBcd(long j) {
        if (j >= 10000000000000000L) {
            ensureCapacity();
            int i = 0;
            while (j != 0) {
                this.bcdBytes[i] = (byte) (j % 10);
                j /= 10;
                i++;
            }
            this.scale = 0;
            this.precision = i;
            return;
        }
        int i2 = 16;
        long j2 = 0;
        while (j != 0) {
            j2 = (j2 >>> 4) + ((j % 10) << 60);
            j /= 10;
            i2--;
        }
        this.bcdLong = j2 >>> (i2 * 4);
        this.scale = 0;
        this.precision = 16 - i2;
    }

    @Override
    protected void readBigIntegerToBcd(BigInteger bigInteger) {
        ensureCapacity();
        int i = 0;
        while (bigInteger.signum() != 0) {
            BigInteger[] bigIntegerArrDivideAndRemainder = bigInteger.divideAndRemainder(BigInteger.TEN);
            int i2 = i + 1;
            ensureCapacity(i2);
            this.bcdBytes[i] = bigIntegerArrDivideAndRemainder[1].byteValue();
            bigInteger = bigIntegerArrDivideAndRemainder[0];
            i = i2;
        }
        this.scale = 0;
        this.precision = i;
    }

    @Override
    protected BigDecimal bcdToBigDecimal() {
        if (this.usingBytes) {
            BigDecimal bigDecimal = new BigDecimal(toNumberString());
            if (isNegative()) {
                return bigDecimal.negate();
            }
            return bigDecimal;
        }
        long digitPos = 0;
        for (int i = this.precision - 1; i >= 0; i--) {
            digitPos = (digitPos * 10) + ((long) getDigitPos(i));
        }
        BigDecimal bigDecimalScaleByPowerOfTen = BigDecimal.valueOf(digitPos).scaleByPowerOfTen(this.scale);
        return isNegative() ? bigDecimalScaleByPowerOfTen.negate() : bigDecimalScaleByPowerOfTen;
    }

    @Override
    protected void compact() {
        if (!this.usingBytes) {
            if (this.bcdLong == 0) {
                setBcdToZero();
                return;
            }
            int iNumberOfTrailingZeros = Long.numberOfTrailingZeros(this.bcdLong) / 4;
            this.bcdLong >>>= iNumberOfTrailingZeros * 4;
            this.scale += iNumberOfTrailingZeros;
            this.precision = 16 - (Long.numberOfLeadingZeros(this.bcdLong) / 4);
            return;
        }
        int i = 0;
        while (i < this.precision && this.bcdBytes[i] == 0) {
            i++;
        }
        if (i == this.precision) {
            setBcdToZero();
            return;
        }
        shiftRight(i);
        int i2 = this.precision - 1;
        while (i2 >= 0 && this.bcdBytes[i2] == 0) {
            i2--;
        }
        this.precision = i2 + 1;
        if (this.precision <= 16) {
            switchStorage();
        }
    }

    private void ensureCapacity() {
        ensureCapacity(40);
    }

    private void ensureCapacity(int i) {
        if (i == 0) {
            return;
        }
        int length = this.usingBytes ? this.bcdBytes.length : 0;
        if (!this.usingBytes) {
            this.bcdBytes = new byte[i];
        } else if (length < i) {
            byte[] bArr = new byte[i * 2];
            System.arraycopy(this.bcdBytes, 0, bArr, 0, length);
            this.bcdBytes = bArr;
        }
        this.usingBytes = true;
    }

    private void switchStorage() {
        if (this.usingBytes) {
            this.bcdLong = 0L;
            for (int i = this.precision - 1; i >= 0; i--) {
                this.bcdLong <<= 4;
                this.bcdLong |= (long) this.bcdBytes[i];
            }
            this.bcdBytes = null;
            this.usingBytes = false;
            return;
        }
        ensureCapacity();
        for (int i2 = 0; i2 < this.precision; i2++) {
            this.bcdBytes[i2] = (byte) (this.bcdLong & 15);
            this.bcdLong >>>= 4;
        }
    }

    @Override
    protected void copyBcdFrom(DecimalQuantity decimalQuantity) {
        DecimalQuantity_DualStorageBCD decimalQuantity_DualStorageBCD = (DecimalQuantity_DualStorageBCD) decimalQuantity;
        setBcdToZero();
        if (decimalQuantity_DualStorageBCD.usingBytes) {
            ensureCapacity(decimalQuantity_DualStorageBCD.precision);
            System.arraycopy(decimalQuantity_DualStorageBCD.bcdBytes, 0, this.bcdBytes, 0, decimalQuantity_DualStorageBCD.precision);
        } else {
            this.bcdLong = decimalQuantity_DualStorageBCD.bcdLong;
        }
    }

    @Deprecated
    public String checkHealth() {
        int i = 0;
        if (this.usingBytes) {
            if (this.bcdLong != 0) {
                return "Value in bcdLong but we are in byte mode";
            }
            if (this.precision == 0) {
                return "Zero precision but we are in byte mode";
            }
            if (this.precision > this.bcdBytes.length) {
                return "Precision exceeds length of byte array";
            }
            if (getDigitPos(this.precision - 1) == 0) {
                return "Most significant digit is zero in byte mode";
            }
            if (getDigitPos(0) == 0) {
                return "Least significant digit is zero in long mode";
            }
            while (i < this.precision) {
                if (getDigitPos(i) >= 10) {
                    return "Digit exceeding 10 in byte array";
                }
                if (getDigitPos(i) < 0) {
                    return "Digit below 0 in byte array";
                }
                i++;
            }
            for (int i2 = this.precision; i2 < this.bcdBytes.length; i2++) {
                if (getDigitPos(i2) != 0) {
                    return "Nonzero digits outside of range in byte array";
                }
            }
            return null;
        }
        if (this.bcdBytes != null) {
            for (int i3 = 0; i3 < this.bcdBytes.length; i3++) {
                if (this.bcdBytes[i3] != 0) {
                    return "Nonzero digits in byte array but we are in long mode";
                }
            }
        }
        if (this.precision == 0 && this.bcdLong != 0) {
            return "Value in bcdLong even though precision is zero";
        }
        if (this.precision > 16) {
            return "Precision exceeds length of long";
        }
        if (this.precision != 0 && getDigitPos(this.precision - 1) == 0) {
            return "Most significant digit is zero in long mode";
        }
        if (this.precision != 0 && getDigitPos(0) == 0) {
            return "Least significant digit is zero in long mode";
        }
        while (i < this.precision) {
            if (getDigitPos(i) >= 10) {
                return "Digit exceeding 10 in long";
            }
            if (getDigitPos(i) < 0) {
                return "Digit below 0 in long (?!)";
            }
            i++;
        }
        for (int i4 = this.precision; i4 < 16; i4++) {
            if (getDigitPos(i4) != 0) {
                return "Nonzero digits outside of range in long";
            }
        }
        return null;
    }

    @Deprecated
    public boolean isUsingBytes() {
        return this.usingBytes;
    }

    public String toString() {
        Object[] objArr = new Object[6];
        objArr[0] = this.lOptPos > 1000 ? "999" : String.valueOf(this.lOptPos);
        objArr[1] = Integer.valueOf(this.lReqPos);
        objArr[2] = Integer.valueOf(this.rReqPos);
        objArr[3] = this.rOptPos < -1000 ? "-999" : String.valueOf(this.rOptPos);
        objArr[4] = this.usingBytes ? "bytes" : "long";
        objArr[5] = toNumberString();
        return String.format("<DecimalQuantity %s:%d:%d:%s %s %s>", objArr);
    }

    public String toNumberString() {
        StringBuilder sb = new StringBuilder();
        if (this.usingBytes) {
            for (int i = this.precision - 1; i >= 0; i--) {
                sb.append((int) this.bcdBytes[i]);
            }
        } else {
            sb.append(Long.toHexString(this.bcdLong));
        }
        sb.append(DateFormat.ABBR_WEEKDAY);
        sb.append(this.scale);
        return sb.toString();
    }
}
