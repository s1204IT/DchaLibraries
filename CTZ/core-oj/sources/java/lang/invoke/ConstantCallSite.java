package java.lang.invoke;

public class ConstantCallSite extends CallSite {
    private final boolean isFrozen;

    public ConstantCallSite(MethodHandle methodHandle) {
        super(methodHandle);
        this.isFrozen = true;
    }

    protected ConstantCallSite(MethodType methodType, MethodHandle methodHandle) throws Throwable {
        super(methodType, methodHandle);
        this.isFrozen = true;
    }

    @Override
    public final MethodHandle getTarget() {
        if (!this.isFrozen) {
            throw new IllegalStateException();
        }
        return this.target;
    }

    @Override
    public final void setTarget(MethodHandle methodHandle) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final MethodHandle dynamicInvoker() {
        return getTarget();
    }
}
