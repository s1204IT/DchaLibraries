package java.lang.invoke;

public class VolatileCallSite extends CallSite {
    public VolatileCallSite(MethodType methodType) {
        super(methodType);
    }

    public VolatileCallSite(MethodHandle methodHandle) {
        super(methodHandle);
    }

    @Override
    public final MethodHandle getTarget() {
        return getTargetVolatile();
    }

    @Override
    public void setTarget(MethodHandle methodHandle) {
        checkTargetChange(getTargetVolatile(), methodHandle);
        setTargetVolatile(methodHandle);
    }

    @Override
    public final MethodHandle dynamicInvoker() {
        return makeDynamicInvoker();
    }
}
