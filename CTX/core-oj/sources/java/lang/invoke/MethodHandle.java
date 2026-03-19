package java.lang.invoke;

import dalvik.system.EmulatedStackFrame;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.Transformers;
import java.util.List;

public abstract class MethodHandle {
    static final boolean $assertionsDisabled = false;
    public static final int IGET = 9;
    public static final int INVOKE_CALLSITE_TRANSFORM = 6;
    public static final int INVOKE_DIRECT = 2;
    public static final int INVOKE_INTERFACE = 4;
    public static final int INVOKE_STATIC = 3;
    public static final int INVOKE_SUPER = 1;
    public static final int INVOKE_TRANSFORM = 5;
    public static final int INVOKE_VAR_HANDLE = 7;
    public static final int INVOKE_VAR_HANDLE_EXACT = 8;
    public static final int INVOKE_VIRTUAL = 0;
    public static final int IPUT = 10;
    public static final int SGET = 11;
    public static final int SPUT = 12;
    protected final long artFieldOrMethod;
    private MethodHandle cachedSpreadInvoker;
    protected final int handleKind;
    private MethodType nominalType;
    private final MethodType type;

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PolymorphicSignature {
    }

    @PolymorphicSignature
    public final native Object invoke(Object... objArr) throws Throwable;

    @PolymorphicSignature
    public final native Object invokeExact(Object... objArr) throws Throwable;

    protected MethodHandle(long j, int i, MethodType methodType) {
        this.artFieldOrMethod = j;
        this.handleKind = i;
        this.type = methodType;
    }

    public MethodType type() {
        if (this.nominalType != null) {
            return this.nominalType;
        }
        return this.type;
    }

    public Object invokeWithArguments(Object... objArr) throws Throwable {
        MethodHandle methodHandle;
        synchronized (this) {
            if (this.cachedSpreadInvoker == null) {
                this.cachedSpreadInvoker = MethodHandles.spreadInvoker(type(), 0);
            }
            methodHandle = this.cachedSpreadInvoker;
        }
        return (Object) methodHandle.invoke(this, objArr);
    }

    public Object invokeWithArguments(List<?> list) throws Throwable {
        return invokeWithArguments(list.toArray());
    }

    public MethodHandle asType(MethodType methodType) {
        if (methodType == this.type) {
            return this;
        }
        if (!this.type.isConvertibleTo(methodType)) {
            throw new WrongMethodTypeException("cannot convert " + ((Object) this) + " to " + ((Object) methodType));
        }
        MethodHandle methodHandleDuplicate = duplicate();
        methodHandleDuplicate.nominalType = methodType;
        return methodHandleDuplicate;
    }

    public MethodHandle asSpreader(Class<?> cls, int i) {
        MethodType methodTypeAsSpreaderChecks = asSpreaderChecks(cls, i);
        int iParameterCount = methodTypeAsSpreaderChecks.parameterCount();
        return new Transformers.Spreader(this, methodTypeAsSpreaderChecks.dropParameterTypes(iParameterCount - i, iParameterCount).appendParameterTypes(cls), i);
    }

    private MethodType asSpreaderChecks(Class<?> cls, int i) {
        spreadArrayChecks(cls, i);
        int iParameterCount = type().parameterCount();
        if (iParameterCount < i || i < 0) {
            throw MethodHandleStatics.newIllegalArgumentException("bad spread array length");
        }
        Class<?> componentType = cls.getComponentType();
        MethodType methodTypeType = type();
        int i2 = iParameterCount - i;
        boolean z = true;
        boolean z2 = true;
        while (true) {
            if (i2 < iParameterCount) {
                Class<?> clsParameterType = methodTypeType.parameterType(i2);
                if (clsParameterType != componentType) {
                    if (MethodType.canConvert(componentType, clsParameterType)) {
                        z2 = false;
                    } else {
                        z2 = false;
                        break;
                    }
                }
                i2++;
            } else {
                z = false;
                break;
            }
        }
        if (z2) {
            return methodTypeType;
        }
        MethodType methodTypeAsSpreaderType = methodTypeType.asSpreaderType(cls, i);
        if (!z) {
            return methodTypeAsSpreaderType;
        }
        asType(methodTypeAsSpreaderType);
        throw MethodHandleStatics.newInternalError("should not return", null);
    }

    private void spreadArrayChecks(Class<?> cls, int i) {
        Class<?> componentType = cls.getComponentType();
        if (componentType == null) {
            throw MethodHandleStatics.newIllegalArgumentException("not an array type", cls);
        }
        if ((i & 127) != i) {
            if ((i & 255) != i) {
                throw MethodHandleStatics.newIllegalArgumentException("array length is not legal", Integer.valueOf(i));
            }
            if (componentType == Long.TYPE || componentType == Double.TYPE) {
                throw MethodHandleStatics.newIllegalArgumentException("array length is not legal for long[] or double[]", Integer.valueOf(i));
            }
        }
    }

    public MethodHandle asCollector(Class<?> cls, int i) {
        asCollectorChecks(cls, i);
        return new Transformers.Collector(this, cls, i);
    }

    boolean asCollectorChecks(Class<?> cls, int i) {
        spreadArrayChecks(cls, i);
        int iParameterCount = type().parameterCount();
        if (iParameterCount != 0) {
            Class<?> clsParameterType = type().parameterType(iParameterCount - 1);
            if (clsParameterType == cls) {
                return true;
            }
            if (clsParameterType.isAssignableFrom(cls)) {
                return $assertionsDisabled;
            }
        }
        throw MethodHandleStatics.newIllegalArgumentException("array type not assignable to trailing argument", this, cls);
    }

    public MethodHandle asVarargsCollector(Class<?> cls) {
        cls.getClass();
        boolean zAsCollectorChecks = asCollectorChecks(cls, 0);
        if (isVarargsCollector() && zAsCollectorChecks) {
            return this;
        }
        return new Transformers.VarargsCollector(this);
    }

    public boolean isVarargsCollector() {
        return $assertionsDisabled;
    }

    public MethodHandle asFixedArity() {
        if (isVarargsCollector()) {
            return ((Transformers.VarargsCollector) this).asFixedArity();
        }
        return this;
    }

    public MethodHandle bindTo(Object obj) {
        return new Transformers.BindTo(this, this.type.leadingReferenceParameter().cast(obj));
    }

    public String toString() {
        return "MethodHandle" + ((Object) this.type);
    }

    public int getHandleKind() {
        return this.handleKind;
    }

    protected void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
        throw new AssertionError((Object) "MethodHandle.transform should never be called.");
    }

    protected MethodHandle duplicate() {
        try {
            return (MethodHandle) clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError((Object) "Subclass of Transformer is not cloneable");
        }
    }

    private void transformInternal(EmulatedStackFrame emulatedStackFrame) throws Throwable {
        transform(emulatedStackFrame);
    }
}
