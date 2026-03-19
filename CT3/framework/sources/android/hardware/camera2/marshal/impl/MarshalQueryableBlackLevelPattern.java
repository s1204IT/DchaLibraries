package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.params.BlackLevelPattern;
import android.hardware.camera2.utils.TypeReference;
import java.nio.ByteBuffer;

public class MarshalQueryableBlackLevelPattern implements MarshalQueryable<BlackLevelPattern> {
    private static final int SIZE = 16;

    private class MarshalerBlackLevelPattern extends Marshaler<BlackLevelPattern> {
        protected MarshalerBlackLevelPattern(TypeReference<BlackLevelPattern> typeReference, int nativeType) {
            super(MarshalQueryableBlackLevelPattern.this, typeReference, nativeType);
        }

        @Override
        public void marshal(BlackLevelPattern value, ByteBuffer buffer) {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    buffer.putInt(value.getOffsetForIndex(j, i));
                }
            }
        }

        @Override
        public BlackLevelPattern unmarshal(ByteBuffer buffer) {
            int[] channelOffsets = new int[4];
            for (int i = 0; i < 4; i++) {
                channelOffsets[i] = buffer.getInt();
            }
            return new BlackLevelPattern(channelOffsets);
        }

        @Override
        public int getNativeSize() {
            return 16;
        }
    }

    @Override
    public Marshaler<BlackLevelPattern> createMarshaler(TypeReference<BlackLevelPattern> managedType, int nativeType) {
        return new MarshalerBlackLevelPattern(managedType, nativeType);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<BlackLevelPattern> managedType, int nativeType) {
        if (nativeType == 1) {
            return BlackLevelPattern.class.equals(managedType.getType());
        }
        return false;
    }
}
