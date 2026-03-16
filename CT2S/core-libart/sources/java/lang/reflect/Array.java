package java.lang.reflect;

public final class Array {
    private static native Object createMultiArray(Class<?> cls, int[] iArr) throws NegativeArraySizeException;

    private static native Object createObjectArray(Class<?> cls, int i) throws NegativeArraySizeException;

    private Array() {
    }

    private static IllegalArgumentException notAnArray(Object o) {
        throw new IllegalArgumentException("Not an array: " + o.getClass());
    }

    private static IllegalArgumentException incompatibleType(Object o) {
        throw new IllegalArgumentException("Array has incompatible type: " + o.getClass());
    }

    private static RuntimeException badArray(Object array) {
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        if (!array.getClass().isArray()) {
            throw notAnArray(array);
        }
        throw incompatibleType(array);
    }

    public static Object get(Object array, int index) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof Object[]) {
            return ((Object[]) array)[index];
        }
        if (array instanceof boolean[]) {
            return ((boolean[]) array)[index] ? Boolean.TRUE : Boolean.FALSE;
        }
        if (array instanceof byte[]) {
            return Byte.valueOf(((byte[]) array)[index]);
        }
        if (array instanceof char[]) {
            return Character.valueOf(((char[]) array)[index]);
        }
        if (array instanceof short[]) {
            return Short.valueOf(((short[]) array)[index]);
        }
        if (array instanceof int[]) {
            return Integer.valueOf(((int[]) array)[index]);
        }
        if (array instanceof long[]) {
            return Long.valueOf(((long[]) array)[index]);
        }
        if (array instanceof float[]) {
            return new Float(((float[]) array)[index]);
        }
        if (array instanceof double[]) {
            return new Double(((double[]) array)[index]);
        }
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        throw notAnArray(array);
    }

    public static boolean getBoolean(Object array, int index) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof boolean[]) {
            return ((boolean[]) array)[index];
        }
        throw badArray(array);
    }

    public static byte getByte(Object array, int index) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        }
        throw badArray(array);
    }

    public static char getChar(Object array, int index) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof char[]) {
            return ((char[]) array)[index];
        }
        throw badArray(array);
    }

    public static double getDouble(Object array, int index) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof double[]) {
            return ((double[]) array)[index];
        }
        if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        }
        if (array instanceof char[]) {
            return ((char[]) array)[index];
        }
        if (array instanceof float[]) {
            return ((float[]) array)[index];
        }
        if (array instanceof int[]) {
            return ((int[]) array)[index];
        }
        if (array instanceof long[]) {
            return ((long[]) array)[index];
        }
        if (array instanceof short[]) {
            return ((short[]) array)[index];
        }
        throw badArray(array);
    }

    public static float getFloat(Object array, int index) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof float[]) {
            return ((float[]) array)[index];
        }
        if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        }
        if (array instanceof char[]) {
            return ((char[]) array)[index];
        }
        if (array instanceof int[]) {
            return ((int[]) array)[index];
        }
        if (array instanceof long[]) {
            return ((long[]) array)[index];
        }
        if (array instanceof short[]) {
            return ((short[]) array)[index];
        }
        throw badArray(array);
    }

    public static int getInt(Object array, int index) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof int[]) {
            return ((int[]) array)[index];
        }
        if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        }
        if (array instanceof char[]) {
            return ((char[]) array)[index];
        }
        if (array instanceof short[]) {
            return ((short[]) array)[index];
        }
        throw badArray(array);
    }

    public static int getLength(Object array) {
        if (array instanceof Object[]) {
            return ((Object[]) array).length;
        }
        if (array instanceof boolean[]) {
            return ((boolean[]) array).length;
        }
        if (array instanceof byte[]) {
            return ((byte[]) array).length;
        }
        if (array instanceof char[]) {
            return ((char[]) array).length;
        }
        if (array instanceof double[]) {
            return ((double[]) array).length;
        }
        if (array instanceof float[]) {
            return ((float[]) array).length;
        }
        if (array instanceof int[]) {
            return ((int[]) array).length;
        }
        if (array instanceof long[]) {
            return ((long[]) array).length;
        }
        if (array instanceof short[]) {
            return ((short[]) array).length;
        }
        throw badArray(array);
    }

    public static long getLong(Object array, int index) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof long[]) {
            return ((long[]) array)[index];
        }
        if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        }
        if (array instanceof char[]) {
            return ((char[]) array)[index];
        }
        if (array instanceof int[]) {
            return ((int[]) array)[index];
        }
        if (array instanceof short[]) {
            return ((short[]) array)[index];
        }
        throw badArray(array);
    }

    public static short getShort(Object array, int index) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof short[]) {
            return ((short[]) array)[index];
        }
        if (array instanceof byte[]) {
            return ((byte[]) array)[index];
        }
        throw badArray(array);
    }

    public static Object newInstance(Class<?> componentType, int... dimensions) throws IllegalArgumentException, NegativeArraySizeException {
        if (dimensions.length <= 0 || dimensions.length > 255) {
            throw new IllegalArgumentException("Bad number of dimensions: " + dimensions.length);
        }
        if (componentType == Void.TYPE) {
            throw new IllegalArgumentException("Can't allocate an array of void");
        }
        if (componentType == null) {
            throw new NullPointerException("componentType == null");
        }
        return createMultiArray(componentType, dimensions);
    }

    public static Object newInstance(Class<?> componentType, int size) throws NegativeArraySizeException {
        if (!componentType.isPrimitive()) {
            return createObjectArray(componentType, size);
        }
        if (componentType == Character.TYPE) {
            return new char[size];
        }
        if (componentType == Integer.TYPE) {
            return new int[size];
        }
        if (componentType == Byte.TYPE) {
            return new byte[size];
        }
        if (componentType == Boolean.TYPE) {
            return new boolean[size];
        }
        if (componentType == Short.TYPE) {
            return new short[size];
        }
        if (componentType == Long.TYPE) {
            return new long[size];
        }
        if (componentType == Float.TYPE) {
            return new float[size];
        }
        if (componentType == Double.TYPE) {
            return new double[size];
        }
        if (componentType == Void.TYPE) {
            throw new IllegalArgumentException("Can't allocate an array of void");
        }
        throw new AssertionError();
    }

    public static void set(Object array, int index, Object value) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (!array.getClass().isArray()) {
            throw notAnArray(array);
        }
        if (array instanceof Object[]) {
            if (value != null && !array.getClass().getComponentType().isInstance(value)) {
                throw incompatibleType(array);
            }
            ((Object[]) array)[index] = value;
            return;
        }
        if (value == null) {
            throw new IllegalArgumentException("Primitive array can't take null values.");
        }
        if (value instanceof Boolean) {
            setBoolean(array, index, ((Boolean) value).booleanValue());
            return;
        }
        if (value instanceof Byte) {
            setByte(array, index, ((Byte) value).byteValue());
            return;
        }
        if (value instanceof Character) {
            setChar(array, index, ((Character) value).charValue());
            return;
        }
        if (value instanceof Short) {
            setShort(array, index, ((Short) value).shortValue());
            return;
        }
        if (value instanceof Integer) {
            setInt(array, index, ((Integer) value).intValue());
            return;
        }
        if (value instanceof Long) {
            setLong(array, index, ((Long) value).longValue());
        } else if (value instanceof Float) {
            setFloat(array, index, ((Float) value).floatValue());
        } else if (value instanceof Double) {
            setDouble(array, index, ((Double) value).doubleValue());
        }
    }

    public static void setBoolean(Object array, int index, boolean value) {
        if (array instanceof boolean[]) {
            ((boolean[]) array)[index] = value;
            return;
        }
        throw badArray(array);
    }

    public static void setByte(Object array, int index, byte value) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof byte[]) {
            ((byte[]) array)[index] = value;
            return;
        }
        if (array instanceof double[]) {
            ((double[]) array)[index] = value;
            return;
        }
        if (array instanceof float[]) {
            ((float[]) array)[index] = value;
            return;
        }
        if (array instanceof int[]) {
            ((int[]) array)[index] = value;
        } else if (array instanceof long[]) {
            ((long[]) array)[index] = value;
        } else {
            if (array instanceof short[]) {
                ((short[]) array)[index] = value;
                return;
            }
            throw badArray(array);
        }
    }

    public static void setChar(Object array, int index, char value) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof char[]) {
            ((char[]) array)[index] = value;
            return;
        }
        if (array instanceof double[]) {
            ((double[]) array)[index] = value;
            return;
        }
        if (array instanceof float[]) {
            ((float[]) array)[index] = value;
        } else if (array instanceof int[]) {
            ((int[]) array)[index] = value;
        } else {
            if (array instanceof long[]) {
                ((long[]) array)[index] = value;
                return;
            }
            throw badArray(array);
        }
    }

    public static void setDouble(Object array, int index, double value) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof double[]) {
            ((double[]) array)[index] = value;
            return;
        }
        throw badArray(array);
    }

    public static void setFloat(Object array, int index, float value) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof float[]) {
            ((float[]) array)[index] = value;
        } else {
            if (array instanceof double[]) {
                ((double[]) array)[index] = value;
                return;
            }
            throw badArray(array);
        }
    }

    public static void setInt(Object array, int index, int value) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof int[]) {
            ((int[]) array)[index] = value;
            return;
        }
        if (array instanceof double[]) {
            ((double[]) array)[index] = value;
        } else if (array instanceof float[]) {
            ((float[]) array)[index] = value;
        } else {
            if (array instanceof long[]) {
                ((long[]) array)[index] = value;
                return;
            }
            throw badArray(array);
        }
    }

    public static void setLong(Object array, int index, long value) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof long[]) {
            ((long[]) array)[index] = value;
        } else if (array instanceof double[]) {
            ((double[]) array)[index] = value;
        } else {
            if (array instanceof float[]) {
                ((float[]) array)[index] = value;
                return;
            }
            throw badArray(array);
        }
    }

    public static void setShort(Object array, int index, short value) throws ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (array instanceof short[]) {
            ((short[]) array)[index] = value;
            return;
        }
        if (array instanceof double[]) {
            ((double[]) array)[index] = value;
            return;
        }
        if (array instanceof float[]) {
            ((float[]) array)[index] = value;
        } else if (array instanceof int[]) {
            ((int[]) array)[index] = value;
        } else {
            if (array instanceof long[]) {
                ((long[]) array)[index] = value;
                return;
            }
            throw badArray(array);
        }
    }
}
