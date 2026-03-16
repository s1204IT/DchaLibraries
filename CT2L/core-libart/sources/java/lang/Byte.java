package java.lang;

@FindBugsSuppressWarnings({"DM_NUMBER_CTOR"})
public final class Byte extends Number implements Comparable<Byte> {
    public static final byte MAX_VALUE = 127;
    public static final byte MIN_VALUE = -128;
    public static final int SIZE = 8;
    public static final Class<Byte> TYPE = byte[].class.getComponentType();
    private static final Byte[] VALUES = new Byte[256];
    private static final long serialVersionUID = -7183698231559129828L;
    private final byte value;

    static {
        for (int i = -128; i < 128; i++) {
            VALUES[i + 128] = new Byte((byte) i);
        }
    }

    public Byte(byte value) {
        this.value = value;
    }

    public Byte(String string) throws NumberFormatException {
        this(parseByte(string));
    }

    @Override
    public byte byteValue() {
        return this.value;
    }

    @Override
    public int compareTo(Byte object) {
        return compare(this.value, object.value);
    }

    public static int compare(byte lhs, byte rhs) {
        if (lhs > rhs) {
            return 1;
        }
        return lhs < rhs ? -1 : 0;
    }

    public static Byte decode(String string) throws NumberFormatException {
        int intValue = Integer.decode(string).intValue();
        byte result = (byte) intValue;
        if (result == intValue) {
            return valueOf(result);
        }
        throw new NumberFormatException("Value out of range for byte: \"" + string + "\"");
    }

    @Override
    public double doubleValue() {
        return this.value;
    }

    @FindBugsSuppressWarnings({"RC_REF_COMPARISON"})
    public boolean equals(Object object) {
        return object == this || ((object instanceof Byte) && ((Byte) object).value == this.value);
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

    public static byte parseByte(String string) throws NumberFormatException {
        return parseByte(string, 10);
    }

    public static byte parseByte(String string, int radix) throws NumberFormatException {
        int intValue = Integer.parseInt(string, radix);
        byte result = (byte) intValue;
        if (result == intValue) {
            return result;
        }
        throw new NumberFormatException("Value out of range for byte: \"" + string + "\"");
    }

    @Override
    public short shortValue() {
        return this.value;
    }

    public String toString() {
        return Integer.toString(this.value);
    }

    public static String toHexString(byte b, boolean upperCase) {
        return IntegralToString.byteToHexString(b, upperCase);
    }

    public static String toString(byte value) {
        return Integer.toString(value);
    }

    public static Byte valueOf(String string) throws NumberFormatException {
        return valueOf(parseByte(string));
    }

    public static Byte valueOf(String string, int radix) throws NumberFormatException {
        return valueOf(parseByte(string, radix));
    }

    public static Byte valueOf(byte b) {
        return VALUES[b + MIN_VALUE];
    }
}
