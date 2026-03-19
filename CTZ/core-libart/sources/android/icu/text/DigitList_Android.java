package android.icu.text;

import android.icu.math.BigDecimal;
import java.math.BigInteger;

public final class DigitList_Android {
    public static final int DBL_DIG = 17;
    private static byte[] LONG_MIN_REP = null;
    public static final int MAX_LONG_DIGITS = 19;
    public int decimalAt = 0;
    public int count = 0;
    public byte[] digits = new byte[19];
    private boolean didRound = false;

    private final void ensureCapacity(int i, int i2) {
        if (i > this.digits.length) {
            byte[] bArr = new byte[i * 2];
            System.arraycopy(this.digits, 0, bArr, 0, i2);
            this.digits = bArr;
        }
    }

    boolean isZero() {
        for (int i = 0; i < this.count; i++) {
            if (this.digits[i] != 48) {
                return false;
            }
        }
        return true;
    }

    public void append(int i) {
        ensureCapacity(this.count + 1, this.count);
        byte[] bArr = this.digits;
        int i2 = this.count;
        this.count = i2 + 1;
        bArr[i2] = (byte) i;
    }

    public byte getDigitValue(int i) {
        return (byte) (this.digits[i] - 48);
    }

    public final double getDouble() {
        if (this.count == 0) {
            return 0.0d;
        }
        StringBuilder sb = new StringBuilder(this.count);
        sb.append('.');
        for (int i = 0; i < this.count; i++) {
            sb.append((char) this.digits[i]);
        }
        sb.append('E');
        sb.append(Integer.toString(this.decimalAt));
        return Double.valueOf(sb.toString()).doubleValue();
    }

    public final long getLong() {
        if (this.count == 0) {
            return 0L;
        }
        if (isLongMIN_VALUE()) {
            return Long.MIN_VALUE;
        }
        StringBuilder sb = new StringBuilder(this.count);
        int i = 0;
        while (i < this.decimalAt) {
            sb.append(i < this.count ? (char) this.digits[i] : '0');
            i++;
        }
        return Long.parseLong(sb.toString());
    }

    public BigInteger getBigInteger(boolean z) {
        int i;
        if (isZero()) {
            return BigInteger.valueOf(0L);
        }
        int i2 = this.decimalAt > this.count ? this.decimalAt : this.count;
        if (!z) {
            i2++;
        }
        char[] cArr = new char[i2];
        int i3 = 0;
        if (!z) {
            cArr[0] = '-';
            while (i3 < this.count) {
                int i4 = i3 + 1;
                cArr[i4] = (char) this.digits[i3];
                i3 = i4;
            }
            i = this.count + 1;
        } else {
            while (i3 < this.count) {
                cArr[i3] = (char) this.digits[i3];
                i3++;
            }
            i = this.count;
        }
        while (i < cArr.length) {
            cArr[i] = '0';
            i++;
        }
        return new BigInteger(new String(cArr));
    }

    private String getStringRep(boolean z) {
        if (isZero()) {
            return AndroidHardcodedSystemProperties.JAVA_VERSION;
        }
        StringBuilder sb = new StringBuilder(this.count + 1);
        if (!z) {
            sb.append('-');
        }
        int i = this.decimalAt;
        if (i < 0) {
            sb.append('.');
            while (i < 0) {
                sb.append('0');
                i++;
            }
            i = -1;
        }
        for (int i2 = 0; i2 < this.count; i2++) {
            if (i == i2) {
                sb.append('.');
            }
            sb.append((char) this.digits[i2]);
        }
        while (true) {
            int i3 = i - 1;
            if (i > this.count) {
                sb.append('0');
                i = i3;
            } else {
                return sb.toString();
            }
        }
    }

    public BigDecimal getBigDecimalICU(boolean z) {
        if (isZero()) {
            return BigDecimal.valueOf(0L);
        }
        long j = ((long) this.count) - ((long) this.decimalAt);
        if (j > 0) {
            int i = this.count;
            if (j > 2147483647L) {
                long j2 = j - 2147483647L;
                if (j2 < this.count) {
                    i = (int) (((long) i) - j2);
                } else {
                    return new BigDecimal(0);
                }
            }
            StringBuilder sb = new StringBuilder(i + 1);
            if (!z) {
                sb.append('-');
            }
            for (int i2 = 0; i2 < i; i2++) {
                sb.append((char) this.digits[i2]);
            }
            return new BigDecimal(new BigInteger(sb.toString()), (int) j);
        }
        return new BigDecimal(getStringRep(z));
    }

    boolean isIntegral() {
        while (this.count > 0 && this.digits[this.count - 1] == 48) {
            this.count--;
        }
        return this.count == 0 || this.decimalAt >= this.count;
    }

    final void set(double d, int i, boolean z) {
        if (d == 0.0d) {
            d = 0.0d;
        }
        String string = Double.toString(d);
        this.didRound = false;
        set(string, 19);
        if (z) {
            if ((-this.decimalAt) > i) {
                this.count = 0;
                return;
            }
            if ((-this.decimalAt) == i) {
                if (shouldRoundUp(0)) {
                    this.count = 1;
                    this.decimalAt++;
                    this.digits[0] = 49;
                    return;
                }
                this.count = 0;
                return;
            }
        }
        while (this.count > 1 && this.digits[this.count - 1] == 48) {
            this.count--;
        }
        if (z) {
            i += this.decimalAt;
        } else if (i == 0) {
            i = -1;
        }
        round(i);
    }

    private void set(String str, int i) {
        int i2;
        int i3;
        boolean z;
        this.decimalAt = -1;
        int iIntValue = 0;
        this.count = 0;
        if (str.charAt(0) != '-') {
            i2 = 0;
            i3 = 0;
            z = false;
        } else {
            i3 = 0;
            z = false;
            i2 = 1;
        }
        while (i2 < str.length()) {
            char cCharAt = str.charAt(i2);
            if (cCharAt == '.') {
                this.decimalAt = this.count;
            } else {
                if (cCharAt == 'e' || cCharAt == 'E') {
                    int i4 = i2 + 1;
                    if (str.charAt(i4) == '+') {
                        i4++;
                    }
                    iIntValue = Integer.valueOf(str.substring(i4)).intValue();
                    if (this.decimalAt == -1) {
                        this.decimalAt = this.count;
                    }
                    this.decimalAt += iIntValue - i3;
                }
                if (this.count < i) {
                    if (!z) {
                        z = cCharAt != '0';
                        if (!z && this.decimalAt != -1) {
                            i3++;
                        }
                    }
                    if (z) {
                        ensureCapacity(this.count + 1, this.count);
                        byte[] bArr = this.digits;
                        int i5 = this.count;
                        this.count = i5 + 1;
                        bArr[i5] = (byte) cCharAt;
                    }
                }
            }
            i2++;
        }
        if (this.decimalAt == -1) {
        }
        this.decimalAt += iIntValue - i3;
    }

    private boolean shouldRoundUp(int i) {
        if (i < this.count) {
            if (this.digits[i] > 53) {
                return true;
            }
            if (this.digits[i] == 53) {
                for (int i2 = i + 1; i2 < this.count; i2++) {
                    if (this.digits[i2] != 48) {
                        return true;
                    }
                }
                return i > 0 && this.digits[i - 1] % 2 != 0;
            }
        }
        return false;
    }

    public final void round(int i) {
        if (i >= 0 && i < this.count) {
            if (shouldRoundUp(i)) {
                while (true) {
                    i--;
                    if (i < 0) {
                        this.digits[0] = 49;
                        this.decimalAt++;
                        this.didRound = true;
                        i = 0;
                        break;
                    }
                    byte[] bArr = this.digits;
                    bArr[i] = (byte) (bArr[i] + 1);
                    this.didRound = true;
                    if (this.digits[i] <= 57) {
                        break;
                    }
                }
                i++;
            }
            this.count = i;
        }
        while (this.count > 1 && this.digits[this.count - 1] == 48) {
            this.count--;
        }
    }

    public boolean wasRounded() {
        return this.didRound;
    }

    public final void set(long j) {
        set(j, 0);
    }

    public final void set(long j, int i) {
        this.didRound = false;
        if (j > 0) {
            int i2 = 19;
            while (j > 0) {
                i2--;
                this.digits[i2] = (byte) (48 + (j % 10));
                j /= 10;
            }
            this.decimalAt = 19 - i2;
            int i3 = 18;
            while (this.digits[i3] == 48) {
                i3--;
            }
            this.count = (i3 - i2) + 1;
            System.arraycopy(this.digits, i2, this.digits, 0, this.count);
        } else if (j == Long.MIN_VALUE) {
            this.count = 19;
            this.decimalAt = 19;
            System.arraycopy(LONG_MIN_REP, 0, this.digits, 0, this.count);
        } else {
            this.count = 0;
            this.decimalAt = 0;
        }
        if (i > 0) {
            round(i);
        }
    }

    public final void set(BigInteger bigInteger, int i) {
        int i2;
        String string = bigInteger.toString();
        int length = string.length();
        this.decimalAt = length;
        this.count = length;
        this.didRound = false;
        while (true) {
            i2 = 1;
            if (this.count <= 1 || string.charAt(this.count - 1) != '0') {
                break;
            } else {
                this.count--;
            }
        }
        if (string.charAt(0) == '-') {
            this.count--;
            this.decimalAt--;
        } else {
            i2 = 0;
        }
        ensureCapacity(this.count, 0);
        for (int i3 = 0; i3 < this.count; i3++) {
            this.digits[i3] = (byte) string.charAt(i3 + i2);
        }
        if (i > 0) {
            round(i);
        }
    }

    private void setBigDecimalDigits(String str, int i, boolean z) {
        this.didRound = false;
        set(str, str.length());
        if (z) {
            i += this.decimalAt;
        } else if (i == 0) {
            i = -1;
        }
        round(i);
    }

    public final void set(java.math.BigDecimal bigDecimal, int i, boolean z) {
        setBigDecimalDigits(bigDecimal.toString(), i, z);
    }

    public final void set(BigDecimal bigDecimal, int i, boolean z) {
        setBigDecimalDigits(bigDecimal.toString(), i, z);
    }

    private boolean isLongMIN_VALUE() {
        if (this.decimalAt != this.count || this.count != 19) {
            return false;
        }
        for (int i = 0; i < this.count; i++) {
            if (this.digits[i] != LONG_MIN_REP[i]) {
                return false;
            }
        }
        return true;
    }

    static {
        String string = Long.toString(Long.MIN_VALUE);
        LONG_MIN_REP = new byte[19];
        int i = 0;
        while (i < 19) {
            int i2 = i + 1;
            LONG_MIN_REP[i] = (byte) string.charAt(i2);
            i = i2;
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DigitList_Android)) {
            return false;
        }
        DigitList_Android digitList_Android = (DigitList_Android) obj;
        if (this.count != digitList_Android.count || this.decimalAt != digitList_Android.decimalAt) {
            return false;
        }
        for (int i = 0; i < this.count; i++) {
            if (this.digits[i] != digitList_Android.digits[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int i = this.decimalAt;
        for (int i2 = 0; i2 < this.count; i2++) {
            i = (i * 37) + this.digits[i2];
        }
        return i;
    }

    public String toString() {
        if (isZero()) {
            return AndroidHardcodedSystemProperties.JAVA_VERSION;
        }
        StringBuilder sb = new StringBuilder("0.");
        for (int i = 0; i < this.count; i++) {
            sb.append((char) this.digits[i]);
        }
        sb.append("x10^");
        sb.append(this.decimalAt);
        return sb.toString();
    }
}
