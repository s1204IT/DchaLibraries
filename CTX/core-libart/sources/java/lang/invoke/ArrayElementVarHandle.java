package java.lang.invoke;

final class ArrayElementVarHandle extends VarHandle {
    private ArrayElementVarHandle(Class<?> cls) {
        super(cls.getComponentType(), cls, false, cls, Integer.TYPE);
    }

    static ArrayElementVarHandle create(Class<?> cls) {
        return new ArrayElementVarHandle(cls);
    }
}
