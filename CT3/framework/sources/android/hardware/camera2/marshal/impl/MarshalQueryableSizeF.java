package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.utils.TypeReference;
import android.util.SizeF;
import java.nio.ByteBuffer;

public class MarshalQueryableSizeF implements MarshalQueryable<SizeF> {
    private static final int SIZE = 8;

    private class MarshalerSizeF extends Marshaler<SizeF> {
        protected MarshalerSizeF(TypeReference<SizeF> typeReference, int nativeType) {
            super(MarshalQueryableSizeF.this, typeReference, nativeType);
        }

        @Override
        public void marshal(SizeF value, ByteBuffer buffer) {
            buffer.putFloat(value.getWidth());
            buffer.putFloat(value.getHeight());
        }

        @Override
        public SizeF unmarshal(ByteBuffer buffer) {
            float width = buffer.getFloat();
            float height = buffer.getFloat();
            return new SizeF(width, height);
        }

        @Override
        public int getNativeSize() {
            return 8;
        }
    }

    @Override
    public Marshaler<SizeF> createMarshaler(TypeReference<SizeF> managedType, int nativeType) {
        return new MarshalerSizeF(managedType, nativeType);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<SizeF> managedType, int nativeType) {
        if (nativeType == 2) {
            return SizeF.class.equals(managedType.getType());
        }
        return false;
    }
}
