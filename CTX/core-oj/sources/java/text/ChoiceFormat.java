package java.text;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import sun.misc.DoubleConsts;

public class ChoiceFormat extends NumberFormat {
    static final long EXPONENT = 9218868437227405312L;
    static final long POSITIVEINFINITY = 9218868437227405312L;
    static final long SIGN = Long.MIN_VALUE;
    private static final long serialVersionUID = 1795184449645032964L;
    private String[] choiceFormats;
    private double[] choiceLimits;

    public void applyPattern(String str) {
        StringBuffer[] stringBufferArr = new StringBuffer[2];
        for (int i = 0; i < stringBufferArr.length; i++) {
            stringBufferArr[i] = new StringBuffer();
        }
        double dNextDouble = 0.0d;
        double d = Double.NaN;
        boolean z = false;
        int i2 = 0;
        String[] strArrDoubleArraySize = new String[30];
        double[] dArrDoubleArraySize = new double[30];
        int i3 = 0;
        char c = 0;
        while (i3 < str.length()) {
            char cCharAt = str.charAt(i3);
            if (cCharAt == '\'') {
                int i4 = i3 + 1;
                if (i4 >= str.length() || str.charAt(i4) != cCharAt) {
                    z = !z;
                } else {
                    stringBufferArr[c].append(cCharAt);
                    i3 = i4;
                }
            } else if (z) {
                stringBufferArr[c].append(cCharAt);
            } else if (cCharAt == '<' || cCharAt == '#' || cCharAt == 8804) {
                if (stringBufferArr[0].length() == 0) {
                    throw new IllegalArgumentException();
                }
                try {
                    String string = stringBufferArr[0].toString();
                    dNextDouble = string.equals("∞") ? Double.POSITIVE_INFINITY : string.equals("-∞") ? Double.NEGATIVE_INFINITY : Double.parseDouble(stringBufferArr[0].toString());
                    if (cCharAt == '<' && dNextDouble != Double.POSITIVE_INFINITY && dNextDouble != Double.NEGATIVE_INFINITY) {
                        dNextDouble = nextDouble(dNextDouble);
                    }
                    if (dNextDouble <= d) {
                        throw new IllegalArgumentException();
                    }
                    stringBufferArr[0].setLength(0);
                    c = 1;
                } catch (Exception e) {
                    throw new IllegalArgumentException();
                }
            } else if (cCharAt == '|') {
                if (i2 == dArrDoubleArraySize.length) {
                    dArrDoubleArraySize = doubleArraySize(dArrDoubleArraySize);
                    strArrDoubleArraySize = doubleArraySize(strArrDoubleArraySize);
                }
                dArrDoubleArraySize[i2] = dNextDouble;
                strArrDoubleArraySize[i2] = stringBufferArr[1].toString();
                i2++;
                stringBufferArr[1].setLength(0);
                d = dNextDouble;
                c = 0;
            } else {
                stringBufferArr[c].append(cCharAt);
            }
            i3++;
        }
        if (c == 1) {
            if (i2 == dArrDoubleArraySize.length) {
                dArrDoubleArraySize = doubleArraySize(dArrDoubleArraySize);
                strArrDoubleArraySize = doubleArraySize(strArrDoubleArraySize);
            }
            dArrDoubleArraySize[i2] = dNextDouble;
            strArrDoubleArraySize[i2] = stringBufferArr[1].toString();
            i2++;
        }
        this.choiceLimits = new double[i2];
        System.arraycopy((Object) dArrDoubleArraySize, 0, (Object) this.choiceLimits, 0, i2);
        this.choiceFormats = new String[i2];
        System.arraycopy(strArrDoubleArraySize, 0, this.choiceFormats, 0, i2);
    }

    public String toPattern() {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < this.choiceLimits.length; i++) {
            if (i != 0) {
                stringBuffer.append('|');
            }
            double dPreviousDouble = previousDouble(this.choiceLimits[i]);
            if (Math.abs(Math.IEEEremainder(this.choiceLimits[i], 1.0d)) < Math.abs(Math.IEEEremainder(dPreviousDouble, 1.0d))) {
                stringBuffer.append("" + this.choiceLimits[i]);
                stringBuffer.append('#');
            } else {
                if (this.choiceLimits[i] == Double.POSITIVE_INFINITY) {
                    stringBuffer.append("∞");
                } else if (this.choiceLimits[i] == Double.NEGATIVE_INFINITY) {
                    stringBuffer.append("-∞");
                } else {
                    stringBuffer.append("" + dPreviousDouble);
                }
                stringBuffer.append('<');
            }
            String str = this.choiceFormats[i];
            boolean z = str.indexOf(60) >= 0 || str.indexOf(35) >= 0 || str.indexOf(8804) >= 0 || str.indexOf(124) >= 0;
            if (z) {
                stringBuffer.append('\'');
            }
            if (str.indexOf(39) < 0) {
                stringBuffer.append(str);
            } else {
                for (int i2 = 0; i2 < str.length(); i2++) {
                    char cCharAt = str.charAt(i2);
                    stringBuffer.append(cCharAt);
                    if (cCharAt == '\'') {
                        stringBuffer.append(cCharAt);
                    }
                }
            }
            if (z) {
                stringBuffer.append('\'');
            }
        }
        return stringBuffer.toString();
    }

    public ChoiceFormat(String str) {
        applyPattern(str);
    }

    public ChoiceFormat(double[] dArr, String[] strArr) {
        setChoices(dArr, strArr);
    }

    public void setChoices(double[] dArr, String[] strArr) {
        if (dArr.length != strArr.length) {
            throw new IllegalArgumentException("Array and limit arrays must be of the same length.");
        }
        this.choiceLimits = Arrays.copyOf(dArr, dArr.length);
        this.choiceFormats = (String[]) Arrays.copyOf(strArr, strArr.length);
    }

    public double[] getLimits() {
        return Arrays.copyOf(this.choiceLimits, this.choiceLimits.length);
    }

    public Object[] getFormats() {
        return Arrays.copyOf(this.choiceFormats, this.choiceFormats.length);
    }

    @Override
    public StringBuffer format(long j, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        return format(j, stringBuffer, fieldPosition);
    }

    @Override
    public StringBuffer format(double d, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        int i = 0;
        while (i < this.choiceLimits.length && d >= this.choiceLimits[i]) {
            i++;
        }
        int i2 = i - 1;
        if (i2 < 0) {
            i2 = 0;
        }
        stringBuffer.append(this.choiceFormats[i2]);
        return stringBuffer;
    }

    @Override
    public Number parse(String str, ParsePosition parsePosition) {
        int i = parsePosition.index;
        double d = Double.NaN;
        int i2 = i;
        int i3 = 0;
        while (true) {
            if (i3 >= this.choiceFormats.length) {
                break;
            }
            String str2 = this.choiceFormats[i3];
            if (str.regionMatches(i, str2, 0, str2.length())) {
                parsePosition.index = str2.length() + i;
                double d2 = this.choiceLimits[i3];
                if (parsePosition.index <= i2) {
                    continue;
                } else {
                    i2 = parsePosition.index;
                    if (i2 != str.length()) {
                        d = d2;
                    } else {
                        d = d2;
                        break;
                    }
                }
            }
            i3++;
        }
        parsePosition.index = i2;
        if (parsePosition.index == i) {
            parsePosition.errorIndex = i2;
        }
        return new Double(d);
    }

    public static final double nextDouble(double d) {
        return nextDouble(d, true);
    }

    public static final double previousDouble(double d) {
        return nextDouble(d, false);
    }

    @Override
    public Object clone() {
        ChoiceFormat choiceFormat = (ChoiceFormat) super.clone();
        choiceFormat.choiceLimits = (double[]) this.choiceLimits.clone();
        choiceFormat.choiceFormats = (String[]) this.choiceFormats.clone();
        return choiceFormat;
    }

    @Override
    public int hashCode() {
        int length = this.choiceLimits.length;
        if (this.choiceFormats.length > 0) {
            return length ^ this.choiceFormats[this.choiceFormats.length - 1].hashCode();
        }
        return length;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ChoiceFormat choiceFormat = (ChoiceFormat) obj;
        if (!Arrays.equals(this.choiceLimits, choiceFormat.choiceLimits) || !Arrays.equals(this.choiceFormats, choiceFormat.choiceFormats)) {
            return false;
        }
        return true;
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        if (this.choiceLimits.length != this.choiceFormats.length) {
            throw new InvalidObjectException("limits and format arrays of different length.");
        }
    }

    public static double nextDouble(double d, boolean z) {
        if (Double.isNaN(d)) {
            return d;
        }
        if (d == 0.0d) {
            double dLongBitsToDouble = Double.longBitsToDouble(1L);
            if (z) {
                return dLongBitsToDouble;
            }
            return -dLongBitsToDouble;
        }
        long jDoubleToLongBits = Double.doubleToLongBits(d);
        long j = Long.MAX_VALUE & jDoubleToLongBits;
        if ((jDoubleToLongBits > 0) != z) {
            j--;
        } else if (j != DoubleConsts.EXP_BIT_MASK) {
            j++;
        }
        return Double.longBitsToDouble((jDoubleToLongBits & Long.MIN_VALUE) | j);
    }

    private static double[] doubleArraySize(double[] dArr) {
        int length = dArr.length;
        double[] dArr2 = new double[length * 2];
        System.arraycopy((Object) dArr, 0, (Object) dArr2, 0, length);
        return dArr2;
    }

    private String[] doubleArraySize(String[] strArr) {
        int length = strArr.length;
        String[] strArr2 = new String[length * 2];
        System.arraycopy(strArr, 0, strArr2, 0, length);
        return strArr2;
    }
}
