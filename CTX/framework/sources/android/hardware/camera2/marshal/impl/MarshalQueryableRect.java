package android.hardware.camera2.marshal.impl;

import android.graphics.Rect;
import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.utils.TypeReference;
import java.nio.ByteBuffer;

public class MarshalQueryableRect implements MarshalQueryable<Rect> {
    private static final int SIZE = 16;

    private class MarshalerRect extends Marshaler<Rect> {
        protected MarshalerRect(TypeReference<Rect> typeReference, int i) {
            super(MarshalQueryableRect.this, typeReference, i);
        }

        @Override
        public void marshal(Rect rect, ByteBuffer byteBuffer) {
            byteBuffer.putInt(rect.left);
            byteBuffer.putInt(rect.top);
            byteBuffer.putInt(rect.width());
            byteBuffer.putInt(rect.height());
        }

        @Override
        public Rect unmarshal(ByteBuffer byteBuffer) {
            int i = byteBuffer.getInt();
            int i2 = byteBuffer.getInt();
            return new Rect(i, i2, byteBuffer.getInt() + i, byteBuffer.getInt() + i2);
        }

        @Override
        public int getNativeSize() {
            return 16;
        }
    }

    @Override
    public Marshaler<Rect> createMarshaler(TypeReference<Rect> typeReference, int i) {
        return new MarshalerRect(typeReference, i);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<Rect> typeReference, int i) {
        return i == 1 && Rect.class.equals(typeReference.getType());
    }
}
