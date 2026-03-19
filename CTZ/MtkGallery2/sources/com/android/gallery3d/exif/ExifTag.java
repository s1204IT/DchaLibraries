package com.android.gallery3d.exif;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class ExifTag {
    private static final long LONG_MAX = 2147483647L;
    private static final long LONG_MIN = -2147483648L;
    static final int SIZE_UNDEFINED = 0;
    private static final SimpleDateFormat TIME_FORMAT;
    public static final short TYPE_ASCII = 2;
    public static final short TYPE_LONG = 9;
    public static final short TYPE_RATIONAL = 10;
    public static final short TYPE_UNDEFINED = 7;
    public static final short TYPE_UNSIGNED_BYTE = 1;
    public static final short TYPE_UNSIGNED_LONG = 4;
    public static final short TYPE_UNSIGNED_RATIONAL = 5;
    public static final short TYPE_UNSIGNED_SHORT = 3;
    private static final long UNSIGNED_LONG_MAX = 4294967295L;
    private static final int UNSIGNED_SHORT_MAX = 65535;
    private int mComponentCountActual;
    private final short mDataType;
    private boolean mHasDefinedDefaultComponentCount;
    private int mIfd;
    private int mOffset;
    private final short mTagId;
    private Object mValue = null;
    private static Charset US_ASCII = Charset.forName("US-ASCII");
    private static final int[] TYPE_TO_SIZE_MAP = new int[11];

    static {
        TYPE_TO_SIZE_MAP[1] = 1;
        TYPE_TO_SIZE_MAP[2] = 1;
        TYPE_TO_SIZE_MAP[3] = 2;
        TYPE_TO_SIZE_MAP[4] = 4;
        TYPE_TO_SIZE_MAP[5] = 8;
        TYPE_TO_SIZE_MAP[7] = 1;
        TYPE_TO_SIZE_MAP[9] = 4;
        TYPE_TO_SIZE_MAP[10] = 8;
        TIME_FORMAT = new SimpleDateFormat("yyyy:MM:dd kk:mm:ss");
    }

    public static boolean isValidIfd(int i) {
        return i == 0 || i == 1 || i == 2 || i == 3 || i == 4;
    }

    public static boolean isValidType(short s) {
        return s == 1 || s == 2 || s == 3 || s == 4 || s == 5 || s == 7 || s == 9 || s == 10;
    }

    ExifTag(short s, short s2, int i, int i2, boolean z) {
        this.mTagId = s;
        this.mDataType = s2;
        this.mComponentCountActual = i;
        this.mHasDefinedDefaultComponentCount = z;
        this.mIfd = i2;
    }

    public static int getElementSize(short s) {
        return TYPE_TO_SIZE_MAP[s];
    }

    public int getIfd() {
        return this.mIfd;
    }

    protected void setIfd(int i) {
        this.mIfd = i;
    }

    public short getTagId() {
        return this.mTagId;
    }

    public short getDataType() {
        return this.mDataType;
    }

    public int getDataSize() {
        return getComponentCount() * getElementSize(getDataType());
    }

    public int getComponentCount() {
        return this.mComponentCountActual;
    }

    protected void forceSetComponentCount(int i) {
        this.mComponentCountActual = i;
    }

    public boolean hasValue() {
        return this.mValue != null;
    }

    public boolean setValue(int[] iArr) {
        if (checkBadComponentCount(iArr.length)) {
            return false;
        }
        if (this.mDataType != 3 && this.mDataType != 9 && this.mDataType != 4) {
            return false;
        }
        if (this.mDataType == 3 && checkOverflowForUnsignedShort(iArr)) {
            return false;
        }
        if (this.mDataType == 4 && checkOverflowForUnsignedLong(iArr)) {
            return false;
        }
        long[] jArr = new long[iArr.length];
        for (int i = 0; i < iArr.length; i++) {
            jArr[i] = iArr[i];
        }
        this.mValue = jArr;
        this.mComponentCountActual = iArr.length;
        return true;
    }

    public boolean setValue(int i) {
        return setValue(new int[]{i});
    }

    public boolean setValue(long[] jArr) {
        if (checkBadComponentCount(jArr.length) || this.mDataType != 4 || checkOverflowForUnsignedLong(jArr)) {
            return false;
        }
        this.mValue = jArr;
        this.mComponentCountActual = jArr.length;
        return true;
    }

    public boolean setValue(long j) {
        return setValue(new long[]{j});
    }

    public boolean setValue(String str) {
        if (this.mDataType != 2 && this.mDataType != 7) {
            return false;
        }
        byte[] bytes = str.getBytes(US_ASCII);
        if (bytes.length > 0) {
            int i = 0;
            while (i < bytes.length && bytes[i] != 0) {
                i++;
            }
            int i2 = i + 1;
            byte[] bArrCopyOf = Arrays.copyOf(bytes, i2);
            this.mComponentCountActual -= bytes.length - i2;
            bytes = bArrCopyOf;
        } else if (this.mDataType == 2 && this.mComponentCountActual == 1) {
            bytes = new byte[]{0};
        }
        int length = bytes.length;
        if (checkBadComponentCount(length)) {
            return false;
        }
        this.mComponentCountActual = length;
        this.mValue = bytes;
        return true;
    }

    public boolean setValue(Rational[] rationalArr) {
        if (checkBadComponentCount(rationalArr.length)) {
            return false;
        }
        if (this.mDataType != 5 && this.mDataType != 10) {
            return false;
        }
        if (this.mDataType == 5 && checkOverflowForUnsignedRational(rationalArr)) {
            return false;
        }
        if (this.mDataType == 10 && checkOverflowForRational(rationalArr)) {
            return false;
        }
        this.mValue = rationalArr;
        this.mComponentCountActual = rationalArr.length;
        return true;
    }

    public boolean setValue(Rational rational) {
        return setValue(new Rational[]{rational});
    }

    public boolean setValue(byte[] bArr, int i, int i2) {
        if (checkBadComponentCount(i2)) {
            return false;
        }
        if (this.mDataType != 1 && this.mDataType != 7) {
            return false;
        }
        this.mValue = new byte[i2];
        System.arraycopy(bArr, i, this.mValue, 0, i2);
        this.mComponentCountActual = i2;
        return true;
    }

    public boolean setValue(byte[] bArr) {
        return setValue(bArr, 0, bArr.length);
    }

    public boolean setValue(byte b) {
        return setValue(new byte[]{b});
    }

    public boolean setValue(Object obj) {
        if (obj == 0) {
            return false;
        }
        if (obj instanceof Short) {
            return setValue(((Short) obj).shortValue() & UNSIGNED_SHORT_MAX);
        }
        if (obj instanceof String) {
            return setValue((String) obj);
        }
        if (obj instanceof int[]) {
            return setValue((int[]) obj);
        }
        if (obj instanceof long[]) {
            return setValue((long[]) obj);
        }
        if (obj instanceof Rational) {
            return setValue((Rational) obj);
        }
        if (obj instanceof Rational[]) {
            return setValue((Rational[]) obj);
        }
        if (obj instanceof byte[]) {
            return setValue((byte[]) obj);
        }
        if (obj instanceof Integer) {
            return setValue(obj.intValue());
        }
        if (obj instanceof Long) {
            return setValue(obj.longValue());
        }
        if (obj instanceof Byte) {
            return setValue(obj.byteValue());
        }
        if (obj instanceof Short[]) {
            int[] iArr = new int[obj.length];
            for (int i = 0; i < obj.length; i++) {
                iArr[i] = obj[i] == 0 ? 0 : obj[i].shortValue() & UNSIGNED_SHORT_MAX;
            }
            return setValue(iArr);
        }
        if (obj instanceof Integer[]) {
            int[] iArr2 = new int[obj.length];
            for (int i2 = 0; i2 < obj.length; i2++) {
                iArr2[i2] = obj[i2] == 0 ? 0 : obj[i2].intValue();
            }
            return setValue(iArr2);
        }
        if (obj instanceof Long[]) {
            long[] jArr = new long[obj.length];
            for (int i3 = 0; i3 < obj.length; i3++) {
                jArr[i3] = obj[i3] == 0 ? 0L : obj[i3].longValue();
            }
            return setValue(jArr);
        }
        if (!(obj instanceof Byte[])) {
            return false;
        }
        byte[] bArr = new byte[obj.length];
        for (int i4 = 0; i4 < obj.length; i4++) {
            bArr[i4] = obj[i4] == 0 ? (byte) 0 : obj[i4].byteValue();
        }
        return setValue(bArr);
    }

    public boolean setTimeValue(long j) {
        boolean value;
        synchronized (TIME_FORMAT) {
            value = setValue(TIME_FORMAT.format(new Date(j)));
        }
        return value;
    }

    public String getValueAsString() {
        if (this.mValue == null) {
            return null;
        }
        if (this.mValue instanceof String) {
            return (String) this.mValue;
        }
        if (this.mValue instanceof byte[]) {
            return new String((byte[]) this.mValue, US_ASCII);
        }
        return null;
    }

    public String getValueAsString(String str) {
        String valueAsString = getValueAsString();
        if (valueAsString == null) {
            return str;
        }
        return valueAsString;
    }

    public byte[] getValueAsBytes() {
        if (this.mValue instanceof byte[]) {
            return (byte[]) this.mValue;
        }
        return null;
    }

    public byte getValueAsByte(byte b) {
        byte[] valueAsBytes = getValueAsBytes();
        if (valueAsBytes == null || valueAsBytes.length < 1) {
            return b;
        }
        return valueAsBytes[0];
    }

    public Rational[] getValueAsRationals() {
        if (this.mValue instanceof Rational[]) {
            return (Rational[]) this.mValue;
        }
        return null;
    }

    public Rational getValueAsRational(Rational rational) {
        Rational[] valueAsRationals = getValueAsRationals();
        if (valueAsRationals == null || valueAsRationals.length < 1) {
            return rational;
        }
        return valueAsRationals[0];
    }

    public Rational getValueAsRational(long j) {
        return getValueAsRational(new Rational(j, 1L));
    }

    public int[] getValueAsInts() {
        if (this.mValue == null || !(this.mValue instanceof long[])) {
            return null;
        }
        long[] jArr = (long[]) this.mValue;
        int[] iArr = new int[jArr.length];
        for (int i = 0; i < jArr.length; i++) {
            iArr[i] = (int) jArr[i];
        }
        return iArr;
    }

    public int getValueAsInt(int i) {
        int[] valueAsInts = getValueAsInts();
        if (valueAsInts == null || valueAsInts.length < 1) {
            return i;
        }
        return valueAsInts[0];
    }

    public long[] getValueAsLongs() {
        if (this.mValue instanceof long[]) {
            return (long[]) this.mValue;
        }
        return null;
    }

    public long getValueAsLong(long j) {
        long[] valueAsLongs = getValueAsLongs();
        if (valueAsLongs == null || valueAsLongs.length < 1) {
            return j;
        }
        return valueAsLongs[0];
    }

    public Object getValue() {
        return this.mValue;
    }

    public long forceGetValueAsLong(long j) {
        long[] valueAsLongs = getValueAsLongs();
        if (valueAsLongs != null && valueAsLongs.length >= 1) {
            return valueAsLongs[0];
        }
        byte[] valueAsBytes = getValueAsBytes();
        if (valueAsBytes != null && valueAsBytes.length >= 1) {
            return valueAsBytes[0];
        }
        Rational[] valueAsRationals = getValueAsRationals();
        if (valueAsRationals != null && valueAsRationals.length >= 1 && valueAsRationals[0].getDenominator() != 0) {
            return (long) valueAsRationals[0].toDouble();
        }
        return j;
    }

    public String forceGetValueAsString() {
        if (this.mValue == null) {
            return "";
        }
        if (this.mValue instanceof byte[]) {
            if (this.mDataType == 2) {
                return new String((byte[]) this.mValue, US_ASCII);
            }
            return Arrays.toString((byte[]) this.mValue);
        }
        if (this.mValue instanceof long[]) {
            if (((long[]) this.mValue).length == 1) {
                return String.valueOf(((long[]) this.mValue)[0]);
            }
            return Arrays.toString((long[]) this.mValue);
        }
        if (this.mValue instanceof Object[]) {
            if (((Object[]) this.mValue).length == 1) {
                Object obj = ((Object[]) this.mValue)[0];
                if (obj == null) {
                    return "";
                }
                return obj.toString();
            }
            return Arrays.toString((Object[]) this.mValue);
        }
        return this.mValue.toString();
    }

    protected long getValueAt(int i) {
        if (this.mValue instanceof long[]) {
            return ((long[]) this.mValue)[i];
        }
        if (this.mValue instanceof byte[]) {
            return ((byte[]) this.mValue)[i];
        }
        throw new IllegalArgumentException("Cannot get integer value from " + convertTypeToString(this.mDataType));
    }

    protected String getString() {
        if (this.mDataType != 2) {
            throw new IllegalArgumentException("Cannot get ASCII value from " + convertTypeToString(this.mDataType));
        }
        return new String((byte[]) this.mValue, US_ASCII);
    }

    protected byte[] getStringByte() {
        return (byte[]) this.mValue;
    }

    protected Rational getRational(int i) {
        if (this.mDataType != 10 && this.mDataType != 5) {
            throw new IllegalArgumentException("Cannot get RATIONAL value from " + convertTypeToString(this.mDataType));
        }
        return ((Rational[]) this.mValue)[i];
    }

    protected void getBytes(byte[] bArr) {
        getBytes(bArr, 0, bArr.length);
    }

    protected void getBytes(byte[] bArr, int i, int i2) {
        if (this.mDataType != 7 && this.mDataType != 1) {
            throw new IllegalArgumentException("Cannot get BYTE value from " + convertTypeToString(this.mDataType));
        }
        Object obj = this.mValue;
        if (i2 > this.mComponentCountActual) {
            i2 = this.mComponentCountActual;
        }
        System.arraycopy(obj, 0, bArr, i, i2);
    }

    protected int getOffset() {
        return this.mOffset;
    }

    protected void setOffset(int i) {
        this.mOffset = i;
    }

    protected void setHasDefinedCount(boolean z) {
        this.mHasDefinedDefaultComponentCount = z;
    }

    protected boolean hasDefinedCount() {
        return this.mHasDefinedDefaultComponentCount;
    }

    private boolean checkBadComponentCount(int i) {
        if (this.mHasDefinedDefaultComponentCount && this.mComponentCountActual != i) {
            return true;
        }
        return false;
    }

    private static String convertTypeToString(short s) {
        switch (s) {
            case 1:
                return "UNSIGNED_BYTE";
            case 2:
                return "ASCII";
            case 3:
                return "UNSIGNED_SHORT";
            case 4:
                return "UNSIGNED_LONG";
            case 5:
                return "UNSIGNED_RATIONAL";
            case 6:
            case 8:
            default:
                return "";
            case 7:
                return "UNDEFINED";
            case 9:
                return "LONG";
            case 10:
                return "RATIONAL";
        }
    }

    private boolean checkOverflowForUnsignedShort(int[] iArr) {
        for (int i : iArr) {
            if (i > UNSIGNED_SHORT_MAX || i < 0) {
                return true;
            }
        }
        return false;
    }

    private boolean checkOverflowForUnsignedLong(long[] jArr) {
        for (long j : jArr) {
            if (j < 0 || j > UNSIGNED_LONG_MAX) {
                return true;
            }
        }
        return false;
    }

    private boolean checkOverflowForUnsignedLong(int[] iArr) {
        for (int i : iArr) {
            if (i < 0) {
                return true;
            }
        }
        return false;
    }

    private boolean checkOverflowForUnsignedRational(Rational[] rationalArr) {
        for (Rational rational : rationalArr) {
            if (rational.getNumerator() < 0 || rational.getDenominator() < 0 || rational.getNumerator() > UNSIGNED_LONG_MAX || rational.getDenominator() > UNSIGNED_LONG_MAX) {
                return true;
            }
        }
        return false;
    }

    private boolean checkOverflowForRational(Rational[] rationalArr) {
        for (Rational rational : rationalArr) {
            if (rational.getNumerator() < LONG_MIN || rational.getDenominator() < LONG_MIN || rational.getNumerator() > LONG_MAX || rational.getDenominator() > LONG_MAX) {
                return true;
            }
        }
        return false;
    }

    public boolean equals(Object obj) {
        if (obj == 0 || !(obj instanceof ExifTag) || obj.mTagId != this.mTagId || obj.mComponentCountActual != this.mComponentCountActual || obj.mDataType != this.mDataType) {
            return false;
        }
        if (this.mValue == null) {
            return obj.mValue == null;
        }
        if (obj.mValue == null) {
            return false;
        }
        if (this.mValue instanceof long[]) {
            if (!(obj.mValue instanceof long[])) {
                return false;
            }
            return Arrays.equals((long[]) this.mValue, (long[]) obj.mValue);
        }
        if (this.mValue instanceof Rational[]) {
            if (!(obj.mValue instanceof Rational[])) {
                return false;
            }
            return Arrays.equals((Rational[]) this.mValue, (Rational[]) obj.mValue);
        }
        if (this.mValue instanceof byte[]) {
            if (!(obj.mValue instanceof byte[])) {
                return false;
            }
            return Arrays.equals((byte[]) this.mValue, (byte[]) obj.mValue);
        }
        return this.mValue.equals(obj.mValue);
    }

    public String toString() {
        return String.format("tag id: %04X\n", Short.valueOf(this.mTagId)) + "ifd id: " + this.mIfd + "\ntype: " + convertTypeToString(this.mDataType) + "\ncount: " + this.mComponentCountActual + "\noffset: " + this.mOffset + "\nvalue: " + forceGetValueAsString() + "\n";
    }
}
