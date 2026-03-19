package sun.misc;

import dalvik.annotation.optimization.FastNative;
import dalvik.system.VMStack;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class Unsafe {
    public static final int INVALID_FIELD_OFFSET = -1;
    private static final Unsafe THE_ONE = new Unsafe();
    private static final Unsafe theUnsafe = THE_ONE;

    @FastNative
    private static native int getArrayBaseOffsetForComponentType(Class cls);

    @FastNative
    private static native int getArrayIndexScaleForComponentType(Class cls);

    @FastNative
    public native int addressSize();

    public native Object allocateInstance(Class<?> cls);

    @FastNative
    public native long allocateMemory(long j);

    @FastNative
    public native boolean compareAndSwapInt(Object obj, long j, int i, int i2);

    @FastNative
    public native boolean compareAndSwapLong(Object obj, long j, long j2, long j3);

    @FastNative
    public native boolean compareAndSwapObject(Object obj, long j, Object obj2, Object obj3);

    @FastNative
    public native void copyMemory(long j, long j2, long j3);

    @FastNative
    public native void copyMemoryFromPrimitiveArray(Object obj, long j, long j2, long j3);

    @FastNative
    public native void copyMemoryToPrimitiveArray(long j, Object obj, long j2, long j3);

    @FastNative
    public native void freeMemory(long j);

    @FastNative
    public native void fullFence();

    @FastNative
    public native boolean getBoolean(Object obj, long j);

    @FastNative
    public native byte getByte(long j);

    @FastNative
    public native byte getByte(Object obj, long j);

    @FastNative
    public native char getChar(long j);

    @FastNative
    public native char getChar(Object obj, long j);

    @FastNative
    public native double getDouble(long j);

    @FastNative
    public native double getDouble(Object obj, long j);

    @FastNative
    public native float getFloat(long j);

    @FastNative
    public native float getFloat(Object obj, long j);

    @FastNative
    public native int getInt(long j);

    @FastNative
    public native int getInt(Object obj, long j);

    @FastNative
    public native int getIntVolatile(Object obj, long j);

    @FastNative
    public native long getLong(long j);

    @FastNative
    public native long getLong(Object obj, long j);

    @FastNative
    public native long getLongVolatile(Object obj, long j);

    @FastNative
    public native Object getObject(Object obj, long j);

    @FastNative
    public native Object getObjectVolatile(Object obj, long j);

    @FastNative
    public native short getShort(long j);

    @FastNative
    public native short getShort(Object obj, long j);

    @FastNative
    public native void loadFence();

    @FastNative
    public native int pageSize();

    @FastNative
    public native void putBoolean(Object obj, long j, boolean z);

    @FastNative
    public native void putByte(long j, byte b);

    @FastNative
    public native void putByte(Object obj, long j, byte b);

    @FastNative
    public native void putChar(long j, char c);

    @FastNative
    public native void putChar(Object obj, long j, char c);

    @FastNative
    public native void putDouble(long j, double d);

    @FastNative
    public native void putDouble(Object obj, long j, double d);

    @FastNative
    public native void putFloat(long j, float f);

    @FastNative
    public native void putFloat(Object obj, long j, float f);

    @FastNative
    public native void putInt(long j, int i);

    @FastNative
    public native void putInt(Object obj, long j, int i);

    @FastNative
    public native void putIntVolatile(Object obj, long j, int i);

    @FastNative
    public native void putLong(long j, long j2);

    @FastNative
    public native void putLong(Object obj, long j, long j2);

    @FastNative
    public native void putLongVolatile(Object obj, long j, long j2);

    @FastNative
    public native void putObject(Object obj, long j, Object obj2);

    @FastNative
    public native void putObjectVolatile(Object obj, long j, Object obj2);

    @FastNative
    public native void putOrderedInt(Object obj, long j, int i);

    @FastNative
    public native void putOrderedLong(Object obj, long j, long j2);

    @FastNative
    public native void putOrderedObject(Object obj, long j, Object obj2);

    @FastNative
    public native void putShort(long j, short s);

    @FastNative
    public native void putShort(Object obj, long j, short s);

    @FastNative
    public native void setMemory(long j, long j2, byte b);

    @FastNative
    public native void storeFence();

    private Unsafe() {
    }

    public static Unsafe getUnsafe() {
        ClassLoader callingClassLoader = VMStack.getCallingClassLoader();
        if (callingClassLoader != null && callingClassLoader != Unsafe.class.getClassLoader()) {
            throw new SecurityException("Unsafe access denied");
        }
        return THE_ONE;
    }

    public long objectFieldOffset(Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("valid for instance fields only");
        }
        return field.getOffset();
    }

    public int arrayBaseOffset(Class cls) {
        Class<?> componentType = cls.getComponentType();
        if (componentType == null) {
            throw new IllegalArgumentException("Valid for array classes only: " + ((Object) cls));
        }
        return getArrayBaseOffsetForComponentType(componentType);
    }

    public int arrayIndexScale(Class cls) {
        Class<?> componentType = cls.getComponentType();
        if (componentType == null) {
            throw new IllegalArgumentException("Valid for array classes only: " + ((Object) cls));
        }
        return getArrayIndexScaleForComponentType(componentType);
    }

    public void park(boolean z, long j) {
        if (z) {
            Thread.currentThread().parkUntil$(j);
        } else {
            Thread.currentThread().parkFor$(j);
        }
    }

    public void unpark(Object obj) {
        if (obj instanceof Thread) {
            ((Thread) obj).unpark$();
            return;
        }
        throw new IllegalArgumentException("valid for Threads only");
    }

    public final int getAndAddInt(Object obj, long j, int i) {
        int intVolatile;
        do {
            intVolatile = getIntVolatile(obj, j);
        } while (!compareAndSwapInt(obj, j, intVolatile, intVolatile + i));
        return intVolatile;
    }

    public final long getAndAddLong(Object obj, long j, long j2) {
        long longVolatile;
        do {
            longVolatile = getLongVolatile(obj, j);
        } while (!compareAndSwapLong(obj, j, longVolatile, longVolatile + j2));
        return longVolatile;
    }

    public final int getAndSetInt(Object obj, long j, int i) {
        int intVolatile;
        do {
            intVolatile = getIntVolatile(obj, j);
        } while (!compareAndSwapInt(obj, j, intVolatile, i));
        return intVolatile;
    }

    public final long getAndSetLong(Object obj, long j, long j2) {
        long longVolatile;
        do {
            longVolatile = getLongVolatile(obj, j);
        } while (!compareAndSwapLong(obj, j, longVolatile, j2));
        return longVolatile;
    }

    public final Object getAndSetObject(Object obj, long j, Object obj2) {
        Object objectVolatile;
        do {
            objectVolatile = getObjectVolatile(obj, j);
        } while (!compareAndSwapObject(obj, j, objectVolatile, obj2));
        return objectVolatile;
    }
}
