package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.utils.TypeReference;
import android.os.BatteryStats;
import java.nio.ByteBuffer;

public class MarshalQueryableNativeByteToInteger implements MarshalQueryable<Integer> {
    private static final int UINT8_MASK = 255;

    private class MarshalerNativeByteToInteger extends Marshaler<Integer> {
        protected MarshalerNativeByteToInteger(TypeReference<Integer> typeReference, int nativeType) {
            super(MarshalQueryableNativeByteToInteger.this, typeReference, nativeType);
        }

        @Override
        public void marshal(Integer value, ByteBuffer buffer) {
            buffer.put((byte) value.intValue());
        }

        @Override
        public Integer unmarshal(ByteBuffer buffer) {
            return Integer.valueOf(buffer.get() & BatteryStats.HistoryItem.CMD_NULL);
        }

        @Override
        public int getNativeSize() {
            return 1;
        }
    }

    @Override
    public Marshaler<Integer> createMarshaler(TypeReference<Integer> managedType, int nativeType) {
        return new MarshalerNativeByteToInteger(managedType, nativeType);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<Integer> managedType, int nativeType) {
        return (Integer.class.equals(managedType.getType()) || Integer.TYPE.equals(managedType.getType())) && nativeType == 0;
    }
}
