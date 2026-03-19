package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.params.BlackLevelPattern;
import android.hardware.camera2.utils.TypeReference;
import java.nio.ByteBuffer;

public class MarshalQueryableBlackLevelPattern implements MarshalQueryable<BlackLevelPattern> {
    private static final int SIZE = 16;

    private class MarshalerBlackLevelPattern extends Marshaler<BlackLevelPattern> {
        protected MarshalerBlackLevelPattern(TypeReference<BlackLevelPattern> typeReference, int i) {
            super(MarshalQueryableBlackLevelPattern.this, typeReference, i);
        }

        @Override
        public void marshal(BlackLevelPattern blackLevelPattern, ByteBuffer byteBuffer) {
            for (int i = 0; i < 2; i++) {
                for (int i2 = 0; i2 < 2; i2++) {
                    byteBuffer.putInt(blackLevelPattern.getOffsetForIndex(i2, i));
                }
            }
        }

        @Override
        public BlackLevelPattern unmarshal(ByteBuffer byteBuffer) {
            int[] iArr = new int[4];
            for (int i = 0; i < 4; i++) {
                iArr[i] = byteBuffer.getInt();
            }
            return new BlackLevelPattern(iArr);
        }

        @Override
        public int getNativeSize() {
            return 16;
        }
    }

    @Override
    public Marshaler<BlackLevelPattern> createMarshaler(TypeReference<BlackLevelPattern> typeReference, int i) {
        return new MarshalerBlackLevelPattern(typeReference, i);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<BlackLevelPattern> typeReference, int i) {
        return i == 1 && BlackLevelPattern.class.equals(typeReference.getType());
    }
}
