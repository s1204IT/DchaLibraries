package java.lang;

import dalvik.bytecode.Opcodes;

@FindBugsSuppressWarnings({"DM_NUMBER_CTOR"})
public final class Short extends Number implements Comparable<Short> {
    public static final short MAX_VALUE = Short.MAX_VALUE;
    public static final short MIN_VALUE = Short.MIN_VALUE;
    public static final int SIZE = 16;
    private static final long serialVersionUID = 7515723908773894738L;
    private final short value;
    public static final Class<Short> TYPE = short[].class.getComponentType();
    private static final Short[] SMALL_VALUES = new Short[256];

    static {
        for (int i = -128; i < 128; i++) {
            SMALL_VALUES[i + 128] = new Short((short) i);
        }
    }

    public Short(String string) throws NumberFormatException {
        this(parseShort(string));
    }

    public Short(short value) {
        this.value = value;
    }

    @Override
    public byte byteValue() {
        return (byte) this.value;
    }

    @Override
    public int compareTo(Short object) {
        return compare(this.value, object.value);
    }

    public static int compare(short lhs, short rhs) {
        if (lhs > rhs) {
            return 1;
        }
        return lhs < rhs ? -1 : 0;
    }

    public static Short decode(String string) throws NumberFormatException {
        int intValue = Integer.decode(string).intValue();
        short result = (short) intValue;
        if (result == intValue) {
            return valueOf(result);
        }
        throw new NumberFormatException("Value out of range for short: \"" + string + "\"");
    }

    @Override
    public double doubleValue() {
        return this.value;
    }

    public boolean equals(Object object) {
        return (object instanceof Short) && ((Short) object).value == this.value;
    }

    @Override
    public float floatValue() {
        return this.value;
    }

    public int hashCode() {
        return this.value;
    }

    @Override
    public int intValue() {
        return this.value;
    }

    @Override
    public long longValue() {
        return this.value;
    }

    public static short parseShort(String string) throws NumberFormatException {
        return parseShort(string, 10);
    }

    public static short parseShort(String string, int radix) throws NumberFormatException {
        int intValue = Integer.parseInt(string, radix);
        short result = (short) intValue;
        if (result == intValue) {
            return result;
        }
        throw new NumberFormatException("Value out of range for short: \"" + string + "\"");
    }

    @Override
    public short shortValue() {
        return this.value;
    }

    public String toString() {
        return Integer.toString(this.value);
    }

    public static String toString(short value) {
        return Integer.toString(value);
    }

    public static Short valueOf(String string) throws NumberFormatException {
        return valueOf(parseShort(string));
    }

    public static Short valueOf(String string, int radix) throws NumberFormatException {
        return valueOf(parseShort(string, radix));
    }

    public static short reverseBytes(short s) {
        return (short) ((s << 8) | ((s >>> 8) & Opcodes.OP_CONST_CLASS_JUMBO));
    }

    public static Short valueOf(short s) {
        return (s < -128 || s >= 128) ? new Short(s) : SMALL_VALUES[s + 128];
    }
}
