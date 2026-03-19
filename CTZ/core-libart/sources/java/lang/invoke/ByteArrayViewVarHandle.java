package java.lang.invoke;

import java.nio.ByteOrder;

final class ByteArrayViewVarHandle extends VarHandle {
    private boolean nativeByteOrder;

    private ByteArrayViewVarHandle(Class<?> cls, ByteOrder byteOrder) {
        super(cls.getComponentType(), byte[].class, false, byte[].class, Integer.TYPE);
        this.nativeByteOrder = byteOrder.equals(ByteOrder.nativeOrder());
    }

    static ByteArrayViewVarHandle create(Class<?> cls, ByteOrder byteOrder) {
        return new ByteArrayViewVarHandle(cls, byteOrder);
    }
}
