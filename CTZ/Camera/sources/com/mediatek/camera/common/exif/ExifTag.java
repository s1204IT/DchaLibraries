package com.mediatek.camera.common.exif;

import com.mediatek.camera.common.device.v2.Camera2Proxy;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class ExifTag {
    private static final SimpleDateFormat TIME_FORMAT;
    private int mComponentCountActual;
    private final short mDataType;
    private boolean mHasDefinedDefaultComponentCount;
    private int mIfd;
    private int mOffset;
    private final short mTagId;
    private Object mValue = null;
    private static final Charset US_ASCII = Charset.forName("US-ASCII");
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

    public boolean setValue(long[] jArr) {
        if (checkBadComponentCount(jArr.length) || this.mDataType != 4 || checkOverflowForUnsignedLong(jArr)) {
            return false;
        }
        this.mValue = jArr;
        this.mComponentCountActual = jArr.length;
        return true;
    }

    public boolean setValue(String str) {
        if (this.mDataType != 2 && this.mDataType != 7) {
            return false;
        }
        byte[] bytes = str.getBytes(US_ASCII);
        if (bytes.length > 0) {
            if (bytes[bytes.length - 1] != 0 && this.mDataType != 7) {
                bytes = Arrays.copyOf(bytes, bytes.length + 1);
            }
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
            case Camera2Proxy.TEMPLATE_PREVIEW:
                return "UNSIGNED_BYTE";
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                return "ASCII";
            case Camera2Proxy.TEMPLATE_RECORD:
                return "UNSIGNED_SHORT";
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                return "UNSIGNED_LONG";
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                return "UNSIGNED_RATIONAL";
            case Camera2Proxy.TEMPLATE_MANUAL:
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
            if (i > 65535 || i < 0) {
                return true;
            }
        }
        return false;
    }

    private boolean checkOverflowForUnsignedLong(long[] jArr) {
        for (long j : jArr) {
            if (j < 0 || j > 4294967295L) {
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
            if (rational.getNumerator() < 0 || rational.getDenominator() < 0 || rational.getNumerator() > 4294967295L || rational.getDenominator() > 4294967295L) {
                return true;
            }
        }
        return false;
    }

    private boolean checkOverflowForRational(Rational[] rationalArr) {
        for (Rational rational : rationalArr) {
            if (rational.getNumerator() < -2147483648L || rational.getDenominator() < -2147483648L || rational.getNumerator() > 2147483647L || rational.getDenominator() > 2147483647L) {
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
