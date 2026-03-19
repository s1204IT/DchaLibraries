package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.utils.TypeReference;
import android.util.Size;
import java.nio.ByteBuffer;

public class MarshalQueryableSize implements MarshalQueryable<Size> {
    private static final int SIZE = 8;

    private class MarshalerSize extends Marshaler<Size> {
        protected MarshalerSize(TypeReference<Size> typeReference, int nativeType) {
            super(MarshalQueryableSize.this, typeReference, nativeType);
        }

        @Override
        public void marshal(Size value, ByteBuffer buffer) {
            buffer.putInt(value.getWidth());
            buffer.putInt(value.getHeight());
        }

        @Override
        public Size unmarshal(ByteBuffer buffer) {
            int width = buffer.getInt();
            int height = buffer.getInt();
            return new Size(width, height);
        }

        @Override
        public int getNativeSize() {
            return 8;
        }
    }

    @Override
    public Marshaler<Size> createMarshaler(TypeReference<Size> managedType, int nativeType) {
        return new MarshalerSize(managedType, nativeType);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<Size> managedType, int nativeType) {
        if (nativeType == 1) {
            return Size.class.equals(managedType.getType());
        }
        return false;
    }
}
