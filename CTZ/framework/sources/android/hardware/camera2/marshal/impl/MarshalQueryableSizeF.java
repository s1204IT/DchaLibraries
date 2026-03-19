package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.utils.TypeReference;
import android.util.SizeF;
import java.nio.ByteBuffer;

public class MarshalQueryableSizeF implements MarshalQueryable<SizeF> {
    private static final int SIZE = 8;

    private class MarshalerSizeF extends Marshaler<SizeF> {
        protected MarshalerSizeF(TypeReference<SizeF> typeReference, int i) {
            super(MarshalQueryableSizeF.this, typeReference, i);
        }

        @Override
        public void marshal(SizeF sizeF, ByteBuffer byteBuffer) {
            byteBuffer.putFloat(sizeF.getWidth());
            byteBuffer.putFloat(sizeF.getHeight());
        }

        @Override
        public SizeF unmarshal(ByteBuffer byteBuffer) {
            return new SizeF(byteBuffer.getFloat(), byteBuffer.getFloat());
        }

        @Override
        public int getNativeSize() {
            return 8;
        }
    }

    @Override
    public Marshaler<SizeF> createMarshaler(TypeReference<SizeF> typeReference, int i) {
        return new MarshalerSizeF(typeReference, i);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<SizeF> typeReference, int i) {
        return i == 2 && SizeF.class.equals(typeReference.getType());
    }
}
