package java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import sun.misc.Unsafe;

public abstract class VarHandle {
    private static final int ALL_MODES_BIT_MASK;
    private static final int ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
    private static final int BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
    private static final int NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
    private static final int READ_ACCESS_MODES_BIT_MASK;
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int WRITE_ACCESS_MODES_BIT_MASK;
    private final int accessModesBitMask;
    private final Class<?> coordinateType0;
    private final Class<?> coordinateType1;
    private final Class<?> varType;

    @MethodHandle.PolymorphicSignature
    public final native Object compareAndExchange(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object compareAndExchangeAcquire(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object compareAndExchangeRelease(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native boolean compareAndSet(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object get(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAcquire(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndAdd(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndAddAcquire(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndAddRelease(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndBitwiseAnd(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndBitwiseAndAcquire(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndBitwiseAndRelease(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndBitwiseOr(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndBitwiseOrAcquire(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndBitwiseOrRelease(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndBitwiseXor(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndBitwiseXorAcquire(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndBitwiseXorRelease(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndSet(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndSetAcquire(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getAndSetRelease(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getOpaque(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native Object getVolatile(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native void set(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native void setOpaque(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native void setRelease(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native void setVolatile(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native boolean weakCompareAndSet(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native boolean weakCompareAndSetAcquire(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native boolean weakCompareAndSetPlain(Object... objArr);

    @MethodHandle.PolymorphicSignature
    public final native boolean weakCompareAndSetRelease(Object... objArr);

    static {
        if (AccessMode.values().length > 32) {
            throw new InternalError("accessModes overflow");
        }
        READ_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.GET));
        WRITE_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.SET));
        ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.COMPARE_AND_EXCHANGE, AccessType.COMPARE_AND_SWAP, AccessType.GET_AND_UPDATE));
        NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.GET_AND_UPDATE_NUMERIC));
        BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK = accessTypesToBitMask(EnumSet.of(AccessType.GET_AND_UPDATE_BITWISE));
        ALL_MODES_BIT_MASK = READ_ACCESS_MODES_BIT_MASK | WRITE_ACCESS_MODES_BIT_MASK | ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK | NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK | BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
    }

    enum AccessType {
        GET,
        SET,
        COMPARE_AND_SWAP,
        COMPARE_AND_EXCHANGE,
        GET_AND_UPDATE,
        GET_AND_UPDATE_BITWISE,
        GET_AND_UPDATE_NUMERIC;

        MethodType accessModeType(Class<?> cls, Class<?> cls2, Class<?>... clsArr) {
            switch (this) {
                case GET:
                    Class<?>[] clsArrAllocateParameters = allocateParameters(0, cls, clsArr);
                    fillParameters(clsArrAllocateParameters, cls, clsArr);
                    return MethodType.methodType(cls2, clsArrAllocateParameters);
                case SET:
                    Class<?>[] clsArrAllocateParameters2 = allocateParameters(1, cls, clsArr);
                    clsArrAllocateParameters2[fillParameters(clsArrAllocateParameters2, cls, clsArr)] = cls2;
                    return MethodType.methodType(Void.TYPE, clsArrAllocateParameters2);
                case COMPARE_AND_SWAP:
                    Class<?>[] clsArrAllocateParameters3 = allocateParameters(2, cls, clsArr);
                    int iFillParameters = fillParameters(clsArrAllocateParameters3, cls, clsArr);
                    clsArrAllocateParameters3[iFillParameters] = cls2;
                    clsArrAllocateParameters3[iFillParameters + 1] = cls2;
                    return MethodType.methodType(Boolean.TYPE, clsArrAllocateParameters3);
                case COMPARE_AND_EXCHANGE:
                    Class<?>[] clsArrAllocateParameters4 = allocateParameters(2, cls, clsArr);
                    int iFillParameters2 = fillParameters(clsArrAllocateParameters4, cls, clsArr);
                    clsArrAllocateParameters4[iFillParameters2] = cls2;
                    clsArrAllocateParameters4[iFillParameters2 + 1] = cls2;
                    return MethodType.methodType(cls2, clsArrAllocateParameters4);
                case GET_AND_UPDATE:
                case GET_AND_UPDATE_BITWISE:
                case GET_AND_UPDATE_NUMERIC:
                    Class<?>[] clsArrAllocateParameters5 = allocateParameters(1, cls, clsArr);
                    clsArrAllocateParameters5[fillParameters(clsArrAllocateParameters5, cls, clsArr)] = cls2;
                    return MethodType.methodType(cls2, clsArrAllocateParameters5);
                default:
                    throw new InternalError("Unknown AccessType");
            }
        }

        private static Class<?>[] allocateParameters(int i, Class<?> cls, Class<?>... clsArr) {
            return new Class[(cls != null ? 1 : 0) + clsArr.length + i];
        }

        private static int fillParameters(Class<?>[] clsArr, Class<?> cls, Class<?>... clsArr2) {
            int i;
            int i2 = 0;
            if (cls == null) {
                i = 0;
            } else {
                clsArr[0] = cls;
                i = 1;
            }
            while (i2 < clsArr2.length) {
                clsArr[i] = clsArr2[i2];
                i2++;
                i++;
            }
            return i;
        }
    }

    public enum AccessMode {
        GET("get", AccessType.GET),
        SET("set", AccessType.SET),
        GET_VOLATILE("getVolatile", AccessType.GET),
        SET_VOLATILE("setVolatile", AccessType.SET),
        GET_ACQUIRE("getAcquire", AccessType.GET),
        SET_RELEASE("setRelease", AccessType.SET),
        GET_OPAQUE("getOpaque", AccessType.GET),
        SET_OPAQUE("setOpaque", AccessType.SET),
        COMPARE_AND_SET("compareAndSet", AccessType.COMPARE_AND_SWAP),
        COMPARE_AND_EXCHANGE("compareAndExchange", AccessType.COMPARE_AND_EXCHANGE),
        COMPARE_AND_EXCHANGE_ACQUIRE("compareAndExchangeAcquire", AccessType.COMPARE_AND_EXCHANGE),
        COMPARE_AND_EXCHANGE_RELEASE("compareAndExchangeRelease", AccessType.COMPARE_AND_EXCHANGE),
        WEAK_COMPARE_AND_SET_PLAIN("weakCompareAndSetPlain", AccessType.COMPARE_AND_SWAP),
        WEAK_COMPARE_AND_SET("weakCompareAndSet", AccessType.COMPARE_AND_SWAP),
        WEAK_COMPARE_AND_SET_ACQUIRE("weakCompareAndSetAcquire", AccessType.COMPARE_AND_SWAP),
        WEAK_COMPARE_AND_SET_RELEASE("weakCompareAndSetRelease", AccessType.COMPARE_AND_SWAP),
        GET_AND_SET("getAndSet", AccessType.GET_AND_UPDATE),
        GET_AND_SET_ACQUIRE("getAndSetAcquire", AccessType.GET_AND_UPDATE),
        GET_AND_SET_RELEASE("getAndSetRelease", AccessType.GET_AND_UPDATE),
        GET_AND_ADD("getAndAdd", AccessType.GET_AND_UPDATE_NUMERIC),
        GET_AND_ADD_ACQUIRE("getAndAddAcquire", AccessType.GET_AND_UPDATE_NUMERIC),
        GET_AND_ADD_RELEASE("getAndAddRelease", AccessType.GET_AND_UPDATE_NUMERIC),
        GET_AND_BITWISE_OR("getAndBitwiseOr", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_OR_RELEASE("getAndBitwiseOrRelease", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_OR_ACQUIRE("getAndBitwiseOrAcquire", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_AND("getAndBitwiseAnd", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_AND_RELEASE("getAndBitwiseAndRelease", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_AND_ACQUIRE("getAndBitwiseAndAcquire", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_XOR("getAndBitwiseXor", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_XOR_RELEASE("getAndBitwiseXorRelease", AccessType.GET_AND_UPDATE_BITWISE),
        GET_AND_BITWISE_XOR_ACQUIRE("getAndBitwiseXorAcquire", AccessType.GET_AND_UPDATE_BITWISE);

        static final Map<String, AccessMode> methodNameToAccessMode = new HashMap(values().length);
        final AccessType at;
        final String methodName;

        static {
            for (AccessMode accessMode : values()) {
                methodNameToAccessMode.put(accessMode.methodName, accessMode);
            }
        }

        AccessMode(String str, AccessType accessType) {
            this.methodName = str;
            this.at = accessType;
        }

        public String methodName() {
            return this.methodName;
        }

        public static AccessMode valueFromMethodName(String str) {
            AccessMode accessMode = methodNameToAccessMode.get(str);
            if (accessMode != null) {
                return accessMode;
            }
            throw new IllegalArgumentException("No AccessMode value for method name " + str);
        }
    }

    public final Class<?> varType() {
        return this.varType;
    }

    public final List<Class<?>> coordinateTypes() {
        if (this.coordinateType0 == null) {
            return Collections.EMPTY_LIST;
        }
        return this.coordinateType1 == null ? Collections.singletonList(this.coordinateType0) : Collections.unmodifiableList(Arrays.asList(this.coordinateType0, this.coordinateType1));
    }

    public final MethodType accessModeType(AccessMode accessMode) {
        return this.coordinateType1 == null ? accessMode.at.accessModeType(this.coordinateType0, this.varType, new Class[0]) : accessMode.at.accessModeType(this.coordinateType0, this.varType, this.coordinateType1);
    }

    public final boolean isAccessModeSupported(AccessMode accessMode) {
        int iOrdinal = 1 << accessMode.ordinal();
        return (this.accessModesBitMask & iOrdinal) == iOrdinal;
    }

    public final MethodHandle toMethodHandle(AccessMode accessMode) {
        return MethodHandles.varHandleExactInvoker(accessMode, accessModeType(accessMode)).bindTo(this);
    }

    public static void fullFence() {
        UNSAFE.fullFence();
    }

    public static void acquireFence() {
        UNSAFE.loadFence();
    }

    public static void releaseFence() {
        UNSAFE.storeFence();
    }

    public static void loadLoadFence() {
        UNSAFE.loadFence();
    }

    public static void storeStoreFence() {
        UNSAFE.storeFence();
    }

    VarHandle(Class<?> cls, boolean z) {
        this.varType = (Class) Objects.requireNonNull(cls);
        this.coordinateType0 = null;
        this.coordinateType1 = null;
        this.accessModesBitMask = alignedAccessModesBitMask(cls, z);
    }

    VarHandle(Class<?> cls, boolean z, Class<?> cls2) {
        this.varType = (Class) Objects.requireNonNull(cls);
        this.coordinateType0 = (Class) Objects.requireNonNull(cls2);
        this.coordinateType1 = null;
        this.accessModesBitMask = alignedAccessModesBitMask(cls, z);
    }

    VarHandle(Class<?> cls, Class<?> cls2, boolean z, Class<?> cls3, Class<?> cls4) {
        this.varType = (Class) Objects.requireNonNull(cls);
        this.coordinateType0 = (Class) Objects.requireNonNull(cls3);
        this.coordinateType1 = (Class) Objects.requireNonNull(cls4);
        Objects.requireNonNull(cls2);
        Class<?> componentType = cls2.getComponentType();
        if (componentType != cls && componentType != Byte.TYPE) {
            throw new InternalError("Unsupported backingArrayType: " + ((Object) cls2));
        }
        if (cls2.getComponentType() == cls) {
            this.accessModesBitMask = alignedAccessModesBitMask(cls, z);
        } else {
            this.accessModesBitMask = unalignedAccessModesBitMask(cls);
        }
    }

    static int accessTypesToBitMask(EnumSet<AccessType> enumSet) {
        int iOrdinal = 0;
        for (AccessMode accessMode : AccessMode.values()) {
            if (enumSet.contains(accessMode.at)) {
                iOrdinal |= 1 << accessMode.ordinal();
            }
        }
        return iOrdinal;
    }

    static int alignedAccessModesBitMask(Class<?> cls, boolean z) {
        int i = ALL_MODES_BIT_MASK;
        if (z) {
            i &= READ_ACCESS_MODES_BIT_MASK;
        }
        if (cls != Byte.TYPE && cls != Short.TYPE && cls != Character.TYPE && cls != Integer.TYPE && cls != Long.TYPE && cls != Float.TYPE && cls != Double.TYPE) {
            i &= ~NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
        }
        if (cls != Boolean.TYPE && cls != Byte.TYPE && cls != Short.TYPE && cls != Character.TYPE && cls != Integer.TYPE && cls != Long.TYPE) {
            return i & (~BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK);
        }
        return i;
    }

    static int unalignedAccessModesBitMask(Class<?> cls) {
        int i = READ_ACCESS_MODES_BIT_MASK | WRITE_ACCESS_MODES_BIT_MASK;
        if (cls == Integer.TYPE || cls == Long.TYPE || cls == Float.TYPE || cls == Double.TYPE) {
            i |= ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
        }
        if (cls == Integer.TYPE || cls == Long.TYPE) {
            i |= NUMERIC_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
        }
        if (cls == Integer.TYPE || cls == Long.TYPE) {
            return i | BITWISE_ATOMIC_UPDATE_ACCESS_MODES_BIT_MASK;
        }
        return i;
    }
}
