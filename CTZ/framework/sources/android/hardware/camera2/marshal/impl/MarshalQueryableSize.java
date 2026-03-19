package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.utils.TypeReference;
import android.util.Size;
import java.nio.ByteBuffer;

public class MarshalQueryableSize implements MarshalQueryable<Size> {
    private static final int SIZE = 8;

    private class MarshalerSize extends Marshaler<Size> {
        protected MarshalerSize(TypeReference<Size> typeReference, int i) {
            super(MarshalQueryableSize.this, typeReference, i);
        }

        @Override
        public void marshal(Size size, ByteBuffer byteBuffer) {
            byteBuffer.putInt(size.getWidth());
            byteBuffer.putInt(size.getHeight());
        }

        @Override
        public Size unmarshal(ByteBuffer byteBuffer) {
            return new Size(byteBuffer.getInt(), byteBuffer.getInt());
        }

        @Override
        public int getNativeSize() {
            return 8;
        }
    }

    @Override
    public Marshaler<Size> createMarshaler(TypeReference<Size> typeReference, int i) {
        return new MarshalerSize(typeReference, i);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<Size> typeReference, int i) {
        return i == 1 && Size.class.equals(typeReference.getType());
    }
}
