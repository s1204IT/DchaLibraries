package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.utils.TypeReference;
import java.nio.ByteBuffer;

public class MarshalQueryableBoolean implements MarshalQueryable<Boolean> {

    private class MarshalerBoolean extends Marshaler<Boolean> {
        protected MarshalerBoolean(TypeReference<Boolean> typeReference, int i) {
            super(MarshalQueryableBoolean.this, typeReference, i);
        }

        @Override
        public void marshal(Boolean bool, ByteBuffer byteBuffer) {
            byteBuffer.put(bool.booleanValue() ? (byte) 1 : (byte) 0);
        }

        @Override
        public Boolean unmarshal(ByteBuffer byteBuffer) {
            return Boolean.valueOf(byteBuffer.get() != 0);
        }

        @Override
        public int getNativeSize() {
            return 1;
        }
    }

    @Override
    public Marshaler<Boolean> createMarshaler(TypeReference<Boolean> typeReference, int i) {
        return new MarshalerBoolean(typeReference, i);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<Boolean> typeReference, int i) {
        return (Boolean.class.equals(typeReference.getType()) || Boolean.TYPE.equals(typeReference.getType())) && i == 0;
    }
}
