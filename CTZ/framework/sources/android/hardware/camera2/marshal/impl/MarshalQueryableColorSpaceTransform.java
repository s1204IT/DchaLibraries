package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.utils.TypeReference;
import java.nio.ByteBuffer;

public class MarshalQueryableColorSpaceTransform implements MarshalQueryable<ColorSpaceTransform> {
    private static final int ELEMENTS_INT32 = 18;
    private static final int SIZE = 72;

    private class MarshalerColorSpaceTransform extends Marshaler<ColorSpaceTransform> {
        protected MarshalerColorSpaceTransform(TypeReference<ColorSpaceTransform> typeReference, int i) {
            super(MarshalQueryableColorSpaceTransform.this, typeReference, i);
        }

        @Override
        public void marshal(ColorSpaceTransform colorSpaceTransform, ByteBuffer byteBuffer) {
            int[] iArr = new int[18];
            colorSpaceTransform.copyElements(iArr, 0);
            for (int i = 0; i < 18; i++) {
                byteBuffer.putInt(iArr[i]);
            }
        }

        @Override
        public ColorSpaceTransform unmarshal(ByteBuffer byteBuffer) {
            int[] iArr = new int[18];
            for (int i = 0; i < 18; i++) {
                iArr[i] = byteBuffer.getInt();
            }
            return new ColorSpaceTransform(iArr);
        }

        @Override
        public int getNativeSize() {
            return 72;
        }
    }

    @Override
    public Marshaler<ColorSpaceTransform> createMarshaler(TypeReference<ColorSpaceTransform> typeReference, int i) {
        return new MarshalerColorSpaceTransform(typeReference, i);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<ColorSpaceTransform> typeReference, int i) {
        return i == 5 && ColorSpaceTransform.class.equals(typeReference.getType());
    }
}
