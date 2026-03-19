package sun.invoke.util;

import java.lang.reflect.Array;
import java.util.Arrays;

public enum Wrapper {
    BOOLEAN(Boolean.class, Boolean.TYPE, 'Z', false, new boolean[0], Format.unsigned(1)),
    BYTE(Byte.class, Byte.TYPE, 'B', (byte) 0, new byte[0], Format.signed(8)),
    SHORT(Short.class, Short.TYPE, 'S', (short) 0, new short[0], Format.signed(16)),
    CHAR(Character.class, Character.TYPE, 'C', (char) 0, new char[0], Format.unsigned(16)),
    INT(Integer.class, Integer.TYPE, 'I', 0, new int[0], Format.signed(32)),
    LONG(Long.class, Long.TYPE, 'J', 0L, new long[0], Format.signed(64)),
    FLOAT(Float.class, Float.TYPE, 'F', Float.valueOf(0.0f), new float[0], Format.floating(32)),
    DOUBLE(Double.class, Double.TYPE, 'D', Double.valueOf(0.0d), new double[0], Format.floating(64)),
    OBJECT(Object.class, Object.class, 'L', null, new Object[0], Format.other(1)),
    VOID(Void.class, Void.TYPE, 'V', null, null, Format.other(0));

    static final boolean $assertionsDisabled = false;
    private final char basicTypeChar;
    private final Object emptyArray;
    private final int format;
    private final String primitiveSimpleName;
    private final Class<?> primitiveType;
    private final String wrapperSimpleName;
    private final Class<?> wrapperType;
    private final Object zero;
    private static final Wrapper[] FROM_PRIM = new Wrapper[16];
    private static final Wrapper[] FROM_WRAP = new Wrapper[16];
    private static final Wrapper[] FROM_CHAR = new Wrapper[16];

    static {
        for (Wrapper wrapper : values()) {
            int iHashPrim = hashPrim(wrapper.primitiveType);
            int iHashWrap = hashWrap(wrapper.wrapperType);
            int iHashChar = hashChar(wrapper.basicTypeChar);
            FROM_PRIM[iHashPrim] = wrapper;
            FROM_WRAP[iHashWrap] = wrapper;
            FROM_CHAR[iHashChar] = wrapper;
        }
    }

    Wrapper(Class cls, Class cls2, char c, Object obj, Object obj2, int i) {
        this.wrapperType = cls;
        this.primitiveType = cls2;
        this.basicTypeChar = c;
        this.zero = obj;
        this.emptyArray = obj2;
        this.format = i;
        this.wrapperSimpleName = cls.getSimpleName();
        this.primitiveSimpleName = cls2.getSimpleName();
    }

    public String detailString() {
        return this.wrapperSimpleName + ((Object) Arrays.asList(this.wrapperType, this.primitiveType, Character.valueOf(this.basicTypeChar), this.zero, "0x" + Integer.toHexString(this.format)));
    }

    private static abstract class Format {
        static final boolean $assertionsDisabled = false;
        static final int BOOLEAN = 5;
        static final int CHAR = 65;
        static final int FLOAT = 4225;
        static final int FLOATING = 4096;
        static final int INT = -3967;
        static final int KIND_SHIFT = 12;
        static final int NUM_MASK = -4;
        static final int SHORT = -4031;
        static final int SIGNED = -4096;
        static final int SIZE_MASK = 1023;
        static final int SIZE_SHIFT = 2;
        static final int SLOT_MASK = 3;
        static final int SLOT_SHIFT = 0;
        static final int UNSIGNED = 0;
        static final int VOID = 0;

        private Format() {
        }

        static int format(int i, int i2, int i3) {
            return i | (i2 << 2) | (i3 << 0);
        }

        static int signed(int i) {
            return format(SIGNED, i, i > 32 ? 2 : 1);
        }

        static int unsigned(int i) {
            return format(0, i, i > 32 ? 2 : 1);
        }

        static int floating(int i) {
            return format(4096, i, i > 32 ? 2 : 1);
        }

        static int other(int i) {
            return i << 0;
        }
    }

    public int bitWidth() {
        return (this.format >> 2) & 1023;
    }

    public int stackSlots() {
        return (this.format >> 0) & 3;
    }

    public boolean isSingleWord() {
        return (this.format & 1) != 0;
    }

    public boolean isDoubleWord() {
        return (this.format & 2) != 0;
    }

    public boolean isNumeric() {
        return (this.format & (-4)) != 0;
    }

    public boolean isIntegral() {
        return isNumeric() && this.format < 4225;
    }

    public boolean isSubwordOrInt() {
        return isIntegral() && isSingleWord();
    }

    public boolean isSigned() {
        return this.format < 0;
    }

    public boolean isUnsigned() {
        return this.format >= 5 && this.format < 4225;
    }

    public boolean isFloating() {
        return this.format >= 4225;
    }

    public boolean isOther() {
        return (this.format & (-4)) == 0;
    }

    public boolean isConvertibleFrom(Wrapper wrapper) {
        if (this == wrapper) {
            return true;
        }
        if (compareTo(wrapper) < 0) {
            return false;
        }
        return (((this.format & wrapper.format) & (-4096)) != 0) || isOther() || wrapper.format == 65;
    }

    private static boolean checkConvertibleFrom() {
        for (Wrapper wrapper : values()) {
            if (wrapper != VOID) {
            }
            if (wrapper == CHAR || !wrapper.isConvertibleFrom(INT)) {
            }
            if (wrapper == BOOLEAN || wrapper == VOID || wrapper != OBJECT) {
            }
            if (wrapper.isSigned()) {
                for (Wrapper wrapper2 : values()) {
                    if (wrapper != wrapper2 && !wrapper2.isFloating() && wrapper2.isSigned() && wrapper.compareTo(wrapper2) < 0) {
                    }
                }
            }
            if (wrapper.isFloating()) {
                for (Wrapper wrapper3 : values()) {
                    if (wrapper != wrapper3 && !wrapper3.isSigned() && wrapper3.isFloating() && wrapper.compareTo(wrapper3) < 0) {
                    }
                }
            }
        }
        return true;
    }

    public Object zero() {
        return this.zero;
    }

    public <T> T zero(Class<T> cls) {
        return (T) convert(this.zero, cls);
    }

    public static Wrapper forPrimitiveType(Class<?> cls) {
        Wrapper wrapperFindPrimitiveType = findPrimitiveType(cls);
        if (wrapperFindPrimitiveType != null) {
            return wrapperFindPrimitiveType;
        }
        if (cls.isPrimitive()) {
            throw new InternalError();
        }
        throw newIllegalArgumentException("not primitive: " + ((Object) cls));
    }

    static Wrapper findPrimitiveType(Class<?> cls) {
        Wrapper wrapper = FROM_PRIM[hashPrim(cls)];
        if (wrapper != null && wrapper.primitiveType == cls) {
            return wrapper;
        }
        return null;
    }

    public static Wrapper forWrapperType(Class<?> cls) {
        Wrapper wrapperFindWrapperType = findWrapperType(cls);
        if (wrapperFindWrapperType != null) {
            return wrapperFindWrapperType;
        }
        for (Wrapper wrapper : values()) {
            if (wrapper.wrapperType == cls) {
                throw new InternalError();
            }
        }
        throw newIllegalArgumentException("not wrapper: " + ((Object) cls));
    }

    static Wrapper findWrapperType(Class<?> cls) {
        Wrapper wrapper = FROM_WRAP[hashWrap(cls)];
        if (wrapper != null && wrapper.wrapperType == cls) {
            return wrapper;
        }
        return null;
    }

    public static Wrapper forBasicType(char c) {
        Wrapper wrapper = FROM_CHAR[hashChar(c)];
        if (wrapper != null && wrapper.basicTypeChar == c) {
            return wrapper;
        }
        for (Wrapper wrapper2 : values()) {
            if (wrapper.basicTypeChar == c) {
                throw new InternalError();
            }
        }
        throw newIllegalArgumentException("not basic type char: " + c);
    }

    public static Wrapper forBasicType(Class<?> cls) {
        if (cls.isPrimitive()) {
            return forPrimitiveType(cls);
        }
        return OBJECT;
    }

    private static int hashPrim(Class<?> cls) {
        String name = cls.getName();
        if (name.length() < 3) {
            return 0;
        }
        return (name.charAt(0) + name.charAt(2)) % 16;
    }

    private static int hashWrap(Class<?> cls) {
        String name = cls.getName();
        if (name.length() < 13) {
            return 0;
        }
        return ((3 * name.charAt(11)) + name.charAt(12)) % 16;
    }

    private static int hashChar(char c) {
        return (c + (c >> 1)) % 16;
    }

    public Class<?> primitiveType() {
        return this.primitiveType;
    }

    public Class<?> wrapperType() {
        return this.wrapperType;
    }

    public <T> Class<T> wrapperType(Class<T> cls) {
        if (cls == this.wrapperType) {
            return cls;
        }
        if (cls == this.primitiveType || this.wrapperType == Object.class || cls.isInterface()) {
            return forceType(this.wrapperType, cls);
        }
        throw newClassCastException(cls, this.primitiveType);
    }

    private static ClassCastException newClassCastException(Class<?> cls, Class<?> cls2) {
        return new ClassCastException(((Object) cls) + " is not compatible with " + ((Object) cls2));
    }

    public static <T> Class<T> asWrapperType(Class<T> cls) {
        if (cls.isPrimitive()) {
            return forPrimitiveType(cls).wrapperType(cls);
        }
        return cls;
    }

    public static <T> Class<T> asPrimitiveType(Class<T> cls) {
        Wrapper wrapperFindWrapperType = findWrapperType(cls);
        if (wrapperFindWrapperType != null) {
            return forceType(wrapperFindWrapperType.primitiveType(), cls);
        }
        return cls;
    }

    public static boolean isWrapperType(Class<?> cls) {
        return findWrapperType(cls) != null;
    }

    public static boolean isPrimitiveType(Class<?> cls) {
        return cls.isPrimitive();
    }

    public static char basicTypeChar(Class<?> cls) {
        if (!cls.isPrimitive()) {
            return 'L';
        }
        return forPrimitiveType(cls).basicTypeChar();
    }

    public char basicTypeChar() {
        return this.basicTypeChar;
    }

    public String wrapperSimpleName() {
        return this.wrapperSimpleName;
    }

    public String primitiveSimpleName() {
        return this.primitiveSimpleName;
    }

    public <T> T cast(Object obj, Class<T> cls) {
        return (T) convert(obj, cls, true);
    }

    public <T> T convert(Object obj, Class<T> cls) {
        return (T) convert(obj, cls, false);
    }

    private <T> T convert(Object obj, Class<T> cls, boolean z) {
        if (this == OBJECT) {
            if (!cls.isInterface()) {
                cls.cast(obj);
            }
            return obj;
        }
        Class<T> clsWrapperType = wrapperType(cls);
        if (clsWrapperType.isInstance(obj)) {
            return clsWrapperType.cast(obj);
        }
        if (!z) {
            Class<?> cls2 = obj.getClass();
            Wrapper wrapperFindWrapperType = findWrapperType(cls2);
            if (wrapperFindWrapperType == null || !isConvertibleFrom(wrapperFindWrapperType)) {
                throw newClassCastException(clsWrapperType, cls2);
            }
        } else if (obj == 0) {
            return (T) this.zero;
        }
        return (T) wrap(obj);
    }

    static <T> Class<T> forceType(Class<?> cls, Class<T> cls2) {
        if (!(cls == cls2 || (cls.isPrimitive() && forPrimitiveType(cls) == findWrapperType(cls2)) || ((cls2.isPrimitive() && forPrimitiveType(cls2) == findWrapperType(cls)) || (cls == Object.class && !cls2.isPrimitive())))) {
            System.out.println(((Object) cls) + " <= " + ((Object) cls2));
        }
        return cls;
    }

    public Object wrap(Object obj) {
        char c = this.basicTypeChar;
        if (c == 'L') {
            return obj;
        }
        if (c == 'V') {
            return null;
        }
        Number numberNumberValue = numberValue(obj);
        char c2 = this.basicTypeChar;
        if (c2 == 'F') {
            return Float.valueOf(numberNumberValue.floatValue());
        }
        if (c2 == 'S') {
            return Short.valueOf((short) numberNumberValue.intValue());
        }
        if (c2 != 'Z') {
            switch (c2) {
                case 'B':
                    return Byte.valueOf((byte) numberNumberValue.intValue());
                case 'C':
                    return Character.valueOf((char) numberNumberValue.intValue());
                case 'D':
                    return Double.valueOf(numberNumberValue.doubleValue());
                default:
                    switch (c2) {
                        case 'I':
                            return Integer.valueOf(numberNumberValue.intValue());
                        case 'J':
                            return Long.valueOf(numberNumberValue.longValue());
                        default:
                            throw new InternalError("bad wrapper");
                    }
            }
        }
        return Boolean.valueOf(boolValue(numberNumberValue.byteValue()));
    }

    public Object wrap(int i) {
        if (this.basicTypeChar == 'L') {
            return Integer.valueOf(i);
        }
        char c = this.basicTypeChar;
        if (c == 'F') {
            return Float.valueOf(i);
        }
        if (c == 'L') {
            throw newIllegalArgumentException("cannot wrap to object type");
        }
        if (c == 'S') {
            return Short.valueOf((short) i);
        }
        if (c == 'V') {
            return null;
        }
        if (c != 'Z') {
            switch (c) {
                case 'B':
                    return Byte.valueOf((byte) i);
                case 'C':
                    return Character.valueOf((char) i);
                case 'D':
                    return Double.valueOf(i);
                default:
                    switch (c) {
                        case 'I':
                            return Integer.valueOf(i);
                        case 'J':
                            return Long.valueOf(i);
                        default:
                            throw new InternalError("bad wrapper");
                    }
            }
        }
        return Boolean.valueOf(boolValue((byte) i));
    }

    private static Number numberValue(Object obj) {
        if (obj instanceof Number) {
            return (Number) obj;
        }
        if (obj instanceof Character) {
            return Integer.valueOf(((Character) obj).charValue());
        }
        if (obj instanceof Boolean) {
            return Integer.valueOf(((Boolean) obj).booleanValue() ? 1 : 0);
        }
        return (Number) obj;
    }

    private static boolean boolValue(byte b) {
        return ((byte) (b & 1)) != 0;
    }

    private static RuntimeException newIllegalArgumentException(String str, Object obj) {
        return newIllegalArgumentException(str + obj);
    }

    private static RuntimeException newIllegalArgumentException(String str) {
        return new IllegalArgumentException(str);
    }

    public Object makeArray(int i) {
        return Array.newInstance(this.primitiveType, i);
    }

    public Class<?> arrayType() {
        return this.emptyArray.getClass();
    }

    public void copyArrayUnboxing(Object[] objArr, int i, Object obj, int i2, int i3) {
        if (obj.getClass() != arrayType()) {
            arrayType().cast(obj);
        }
        for (int i4 = 0; i4 < i3; i4++) {
            Array.set(obj, i4 + i2, convert(objArr[i4 + i], this.primitiveType));
        }
    }

    public void copyArrayBoxing(Object obj, int i, Object[] objArr, int i2, int i3) {
        if (obj.getClass() != arrayType()) {
            arrayType().cast(obj);
        }
        for (int i4 = 0; i4 < i3; i4++) {
            objArr[i4 + i2] = Array.get(obj, i4 + i);
        }
    }
}
