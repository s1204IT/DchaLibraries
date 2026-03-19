package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.utils.TypeReference;
import java.nio.ByteBuffer;

public class MarshalQueryableRggbChannelVector implements MarshalQueryable<RggbChannelVector> {
    private static final int SIZE = 16;

    private class MarshalerRggbChannelVector extends Marshaler<RggbChannelVector> {
        protected MarshalerRggbChannelVector(TypeReference<RggbChannelVector> typeReference, int i) {
            super(MarshalQueryableRggbChannelVector.this, typeReference, i);
        }

        @Override
        public void marshal(RggbChannelVector rggbChannelVector, ByteBuffer byteBuffer) {
            for (int i = 0; i < 4; i++) {
                byteBuffer.putFloat(rggbChannelVector.getComponent(i));
            }
        }

        @Override
        public RggbChannelVector unmarshal(ByteBuffer byteBuffer) {
            return new RggbChannelVector(byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat(), byteBuffer.getFloat());
        }

        @Override
        public int getNativeSize() {
            return 16;
        }
    }

    @Override
    public Marshaler<RggbChannelVector> createMarshaler(TypeReference<RggbChannelVector> typeReference, int i) {
        return new MarshalerRggbChannelVector(typeReference, i);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<RggbChannelVector> typeReference, int i) {
        return i == 2 && RggbChannelVector.class.equals(typeReference.getType());
    }
}
