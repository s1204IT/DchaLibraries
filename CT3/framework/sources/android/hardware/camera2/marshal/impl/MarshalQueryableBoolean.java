package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.utils.TypeReference;
import java.nio.ByteBuffer;

public class MarshalQueryableBoolean implements MarshalQueryable<Boolean> {

    private class MarshalerBoolean extends Marshaler<Boolean> {
        protected MarshalerBoolean(TypeReference<Boolean> typeReference, int nativeType) {
            super(MarshalQueryableBoolean.this, typeReference, nativeType);
        }

        @Override
        public void marshal(Boolean value, ByteBuffer buffer) {
            boolean unboxValue = value.booleanValue();
            buffer.put((byte) (unboxValue ? 1 : 0));
        }

        @Override
        public Boolean unmarshal(ByteBuffer buffer) {
            return Boolean.valueOf(buffer.get() != 0);
        }

        @Override
        public int getNativeSize() {
            return 1;
        }
    }

    @Override
    public Marshaler<Boolean> createMarshaler(TypeReference<Boolean> managedType, int nativeType) {
        return new MarshalerBoolean(managedType, nativeType);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<Boolean> managedType, int nativeType) {
        return (Boolean.class.equals(managedType.getType()) || Boolean.TYPE.equals(managedType.getType())) && nativeType == 0;
    }
}
