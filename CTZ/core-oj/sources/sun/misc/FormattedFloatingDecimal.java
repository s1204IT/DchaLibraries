package sun.misc;

import java.util.Arrays;
import sun.misc.FloatingDecimal;

public class FormattedFloatingDecimal {
    static final boolean $assertionsDisabled = false;
    private static final ThreadLocal<Object> threadLocalCharBuffer = new ThreadLocal<Object>() {
        @Override
        protected Object initialValue() {
            return new char[20];
        }
    };
    private int decExponentRounded;
    private char[] exponent;
    private char[] mantissa;

    public enum Form {
        SCIENTIFIC,
        COMPATIBLE,
        DECIMAL_FLOAT,
        GENERAL
    }

    public static FormattedFloatingDecimal valueOf(double d, int i, Form form) {
        return new FormattedFloatingDecimal(i, form, FloatingDecimal.getBinaryToASCIIConverter(d, form == Form.COMPATIBLE));
    }

    private static char[] getBuffer() {
        return (char[]) threadLocalCharBuffer.get();
    }

    private FormattedFloatingDecimal(int i, Form form, FloatingDecimal.BinaryToASCIIConverter binaryToASCIIConverter) {
        if (binaryToASCIIConverter.isExceptional()) {
            this.mantissa = binaryToASCIIConverter.toJavaFormatString().toCharArray();
            this.exponent = null;
        }
        char[] buffer = getBuffer();
        int digits = binaryToASCIIConverter.getDigits(buffer);
        int decimalExponent = binaryToASCIIConverter.getDecimalExponent();
        boolean zIsNegative = binaryToASCIIConverter.isNegative();
        switch (form) {
            case COMPATIBLE:
                this.decExponentRounded = decimalExponent;
                fillCompatible(i, buffer, digits, decimalExponent, zIsNegative);
                break;
            case DECIMAL_FLOAT:
                int iApplyPrecision = applyPrecision(decimalExponent, buffer, digits, decimalExponent + i);
                fillDecimal(i, buffer, digits, iApplyPrecision, zIsNegative);
                this.decExponentRounded = iApplyPrecision;
                break;
            case SCIENTIFIC:
                int iApplyPrecision2 = applyPrecision(decimalExponent, buffer, digits, i + 1);
                fillScientific(i, buffer, digits, iApplyPrecision2, zIsNegative);
                this.decExponentRounded = iApplyPrecision2;
                break;
            case GENERAL:
                int iApplyPrecision3 = applyPrecision(decimalExponent, buffer, digits, i);
                int i2 = iApplyPrecision3 - 1;
                if (i2 < -4 || i2 >= i) {
                    fillScientific(i - 1, buffer, digits, iApplyPrecision3, zIsNegative);
                } else {
                    fillDecimal(i - iApplyPrecision3, buffer, digits, iApplyPrecision3, zIsNegative);
                }
                this.decExponentRounded = iApplyPrecision3;
                break;
        }
    }

    public int getExponentRounded() {
        return this.decExponentRounded - 1;
    }

    public char[] getMantissa() {
        return this.mantissa;
    }

    public char[] getExponent() {
        return this.exponent;
    }

    private static int applyPrecision(int i, char[] cArr, int i2, int i3) {
        if (i3 >= i2 || i3 < 0) {
            return i;
        }
        if (i3 == 0) {
            if (cArr[0] >= '5') {
                cArr[0] = '1';
                Arrays.fill(cArr, 1, i2, '0');
                return i + 1;
            }
            Arrays.fill(cArr, 0, i2, '0');
            return i;
        }
        if (cArr[i3] >= '5') {
            int i4 = i3 - 1;
            char c = cArr[i4];
            if (c == '9') {
                while (c == '9' && i4 > 0) {
                    i4--;
                    c = cArr[i4];
                }
                if (c == '9') {
                    cArr[0] = '1';
                    Arrays.fill(cArr, 1, i2, '0');
                    return i + 1;
                }
            }
            cArr[i4] = (char) (c + 1);
            Arrays.fill(cArr, i4 + 1, i2, '0');
        } else {
            Arrays.fill(cArr, i3, i2, '0');
        }
        return i;
    }

    private void fillCompatible(int i, char[] cArr, int i2, int i3, boolean z) {
        int i4;
        int i5 = 0;
        if (i3 > 0 && i3 < 8) {
            if (i2 < i3) {
                int i6 = i3 - i2;
                this.mantissa = create(z, i2 + i6 + 2);
                System.arraycopy((Object) cArr, 0, (Object) this.mantissa, z ? 1 : 0, i2);
                char[] cArr2 = this.mantissa;
                int i7 = (z ? 1 : 0) + i2;
                int i8 = i6 + i7;
                Arrays.fill(cArr2, i7, i8, '0');
                this.mantissa[i8] = '.';
                this.mantissa[i8 + 1] = '0';
                return;
            }
            if (i3 < i2) {
                int iMin = Math.min(i2 - i3, i);
                this.mantissa = create(z, i3 + 1 + iMin);
                System.arraycopy((Object) cArr, 0, (Object) this.mantissa, z ? 1 : 0, i3);
                char[] cArr3 = this.mantissa;
                int i9 = (z ? 1 : 0) + i3;
                cArr3[i9] = '.';
                System.arraycopy((Object) cArr, i3, (Object) this.mantissa, i9 + 1, iMin);
                return;
            }
            this.mantissa = create(z, i2 + 2);
            System.arraycopy((Object) cArr, 0, (Object) this.mantissa, z ? 1 : 0, i2);
            char[] cArr4 = this.mantissa;
            int i10 = (z ? 1 : 0) + i2;
            cArr4[i10] = '.';
            this.mantissa[i10 + 1] = '0';
            return;
        }
        if (i3 <= 0 && i3 > -3) {
            int iMax = Math.max(0, Math.min(-i3, i));
            int iMax2 = Math.max(0, Math.min(i2, i + i3));
            if (iMax > 0) {
                this.mantissa = create(z, iMax + 2 + iMax2);
                this.mantissa[z ? 1 : 0] = '0';
                this.mantissa[(z ? 1 : 0) + 1] = '.';
                char[] cArr5 = this.mantissa;
                int i11 = (z ? 1 : 0) + 2;
                int i12 = iMax + i11;
                Arrays.fill(cArr5, i11, i12, '0');
                if (iMax2 > 0) {
                    System.arraycopy((Object) cArr, 0, (Object) this.mantissa, i12, iMax2);
                    return;
                }
                return;
            }
            if (iMax2 > 0) {
                this.mantissa = create(z, iMax + 2 + iMax2);
                this.mantissa[z ? 1 : 0] = '0';
                this.mantissa[(z ? 1 : 0) + 1] = '.';
                System.arraycopy((Object) cArr, 0, (Object) this.mantissa, (z ? 1 : 0) + 2, iMax2);
                return;
            }
            this.mantissa = create(z, 1);
            this.mantissa[z ? 1 : 0] = '0';
            return;
        }
        if (i2 > 1) {
            this.mantissa = create(z, i2 + 1);
            this.mantissa[z ? 1 : 0] = cArr[0];
            this.mantissa[(z ? 1 : 0) + 1] = '.';
            System.arraycopy((Object) cArr, 1, (Object) this.mantissa, (z ? 1 : 0) + 2, i2 - 1);
        } else {
            this.mantissa = create(z, 3);
            this.mantissa[z ? 1 : 0] = cArr[0];
            this.mantissa[(z ? 1 : 0) + 1] = '.';
            this.mantissa[(z ? 1 : 0) + 2] = '0';
        }
        boolean z2 = i3 <= 0;
        if (z2) {
            i4 = (-i3) + 1;
            i5 = 1;
        } else {
            i4 = i3 - 1;
        }
        if (i4 <= 9) {
            this.exponent = create(z2, 1);
            this.exponent[i5] = (char) (i4 + 48);
        } else if (i4 <= 99) {
            this.exponent = create(z2, 2);
            this.exponent[i5] = (char) ((i4 / 10) + 48);
            this.exponent[i5 + 1] = (char) ((i4 % 10) + 48);
        } else {
            this.exponent = create(z2, 3);
            this.exponent[i5] = (char) ((i4 / 100) + 48);
            int i13 = i4 % 100;
            this.exponent[i5 + 1] = (char) ((i13 / 10) + 48);
            this.exponent[i5 + 2] = (char) ((i13 % 10) + 48);
        }
    }

    private static char[] create(boolean z, int i) {
        if (z) {
            char[] cArr = new char[i + 1];
            cArr[0] = '-';
            return cArr;
        }
        return new char[i];
    }

    private void fillDecimal(int i, char[] cArr, int i2, int i3, boolean z) {
        if (i3 > 0) {
            if (i2 < i3) {
                this.mantissa = create(z, i3);
                System.arraycopy((Object) cArr, 0, (Object) this.mantissa, z ? 1 : 0, i2);
                Arrays.fill(this.mantissa, i2 + (z ? 1 : 0), (z ? 1 : 0) + i3, '0');
                return;
            }
            int iMin = Math.min(i2 - i3, i);
            this.mantissa = create(z, (iMin > 0 ? iMin + 1 : 0) + i3);
            System.arraycopy((Object) cArr, 0, (Object) this.mantissa, z ? 1 : 0, i3);
            if (iMin > 0) {
                char[] cArr2 = this.mantissa;
                int i4 = (z ? 1 : 0) + i3;
                cArr2[i4] = '.';
                System.arraycopy((Object) cArr, i3, (Object) this.mantissa, i4 + 1, iMin);
                return;
            }
            return;
        }
        if (i3 <= 0) {
            int iMax = Math.max(0, Math.min(-i3, i));
            int iMax2 = Math.max(0, Math.min(i2, i + i3));
            if (iMax > 0) {
                this.mantissa = create(z, iMax + 2 + iMax2);
                this.mantissa[z ? 1 : 0] = '0';
                this.mantissa[(z ? 1 : 0) + 1] = '.';
                char[] cArr3 = this.mantissa;
                int i5 = (z ? 1 : 0) + 2;
                int i6 = iMax + i5;
                Arrays.fill(cArr3, i5, i6, '0');
                if (iMax2 > 0) {
                    System.arraycopy((Object) cArr, 0, (Object) this.mantissa, i6, iMax2);
                    return;
                }
                return;
            }
            if (iMax2 > 0) {
                this.mantissa = create(z, iMax + 2 + iMax2);
                this.mantissa[z ? 1 : 0] = '0';
                this.mantissa[(z ? 1 : 0) + 1] = '.';
                System.arraycopy((Object) cArr, 0, (Object) this.mantissa, (z ? 1 : 0) + 2, iMax2);
                return;
            }
            this.mantissa = create(z, 1);
            this.mantissa[z ? 1 : 0] = '0';
        }
    }

    private void fillScientific(int i, char[] cArr, int i2, int i3, boolean z) {
        char c;
        int i4;
        int iMax = Math.max(0, Math.min(i2 - 1, i));
        if (iMax > 0) {
            this.mantissa = create(z, iMax + 2);
            this.mantissa[z ? 1 : 0] = cArr[0];
            this.mantissa[(z ? 1 : 0) + 1] = '.';
            System.arraycopy((Object) cArr, 1, (Object) this.mantissa, (z ? 1 : 0) + 2, iMax);
        } else {
            this.mantissa = create(z, 1);
            this.mantissa[z ? 1 : 0] = cArr[0];
        }
        if (i3 <= 0) {
            c = '-';
            i4 = (-i3) + 1;
        } else {
            c = '+';
            i4 = i3 - 1;
        }
        if (i4 <= 9) {
            this.exponent = new char[]{c, '0', (char) (i4 + 48)};
        } else {
            if (i4 <= 99) {
                this.exponent = new char[]{c, (char) ((i4 / 10) + 48), (char) ((i4 % 10) + 48)};
                return;
            }
            char c2 = (char) ((i4 / 100) + 48);
            int i5 = i4 % 100;
            this.exponent = new char[]{c, c2, (char) ((i5 / 10) + 48), (char) ((i5 % 10) + 48)};
        }
    }
}
