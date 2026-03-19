package java.lang.invoke;

import java.lang.invoke.MethodHandles;

public abstract class CallSite {
    private static MethodHandle GET_TARGET = null;
    private static final long TARGET_OFFSET;
    MethodHandle target;

    public abstract MethodHandle dynamicInvoker();

    public abstract MethodHandle getTarget();

    public abstract void setTarget(MethodHandle methodHandle);

    CallSite(MethodType methodType) {
        this.target = MethodHandles.throwException(methodType.returnType(), IllegalStateException.class);
        this.target = MethodHandles.insertArguments(this.target, 0, new IllegalStateException("uninitialized call site"));
        if (methodType.parameterCount() > 0) {
            this.target = MethodHandles.dropArguments(this.target, 0, methodType.ptypes());
        }
        initializeGetTarget();
    }

    CallSite(MethodHandle methodHandle) {
        methodHandle.type();
        this.target = methodHandle;
        initializeGetTarget();
    }

    CallSite(MethodType methodType, MethodHandle methodHandle) throws Throwable {
        this(methodType);
        MethodHandle methodHandle2 = (MethodHandle) methodHandle.invokeWithArguments((ConstantCallSite) this);
        checkTargetChange(this.target, methodHandle2);
        this.target = methodHandle2;
        initializeGetTarget();
    }

    public MethodType type() {
        return this.target.type();
    }

    void checkTargetChange(MethodHandle methodHandle, MethodHandle methodHandle2) {
        MethodType methodTypeType = methodHandle.type();
        if (!methodHandle2.type().equals((Object) methodTypeType)) {
            throw wrongTargetType(methodHandle2, methodTypeType);
        }
    }

    private static WrongMethodTypeException wrongTargetType(MethodHandle methodHandle, MethodType methodType) {
        return new WrongMethodTypeException(String.valueOf(methodHandle) + " should be of type " + ((Object) methodType));
    }

    MethodHandle makeDynamicInvoker() {
        return MethodHandles.foldArguments(MethodHandles.exactInvoker(type()), GET_TARGET.bindTo(this));
    }

    static {
        try {
            TARGET_OFFSET = MethodHandleStatics.UNSAFE.objectFieldOffset(CallSite.class.getDeclaredField("target"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private void initializeGetTarget() {
        synchronized (CallSite.class) {
            if (GET_TARGET == null) {
                try {
                    GET_TARGET = MethodHandles.Lookup.IMPL_LOOKUP.findVirtual(CallSite.class, "getTarget", MethodType.methodType(MethodHandle.class));
                } catch (ReflectiveOperationException e) {
                    throw new InternalError(e);
                }
            }
        }
    }

    void setTargetNormal(MethodHandle methodHandle) {
        this.target = methodHandle;
    }

    MethodHandle getTargetVolatile() {
        return (MethodHandle) MethodHandleStatics.UNSAFE.getObjectVolatile(this, TARGET_OFFSET);
    }

    void setTargetVolatile(MethodHandle methodHandle) {
        MethodHandleStatics.UNSAFE.putObjectVolatile(this, TARGET_OFFSET, methodHandle);
    }
}
