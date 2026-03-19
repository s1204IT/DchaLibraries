package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.params.HighSpeedVideoConfiguration;
import android.hardware.camera2.utils.TypeReference;
import java.nio.ByteBuffer;

public class MarshalQueryableHighSpeedVideoConfiguration implements MarshalQueryable<HighSpeedVideoConfiguration> {
    private static final int SIZE = 20;

    private class MarshalerHighSpeedVideoConfiguration extends Marshaler<HighSpeedVideoConfiguration> {
        protected MarshalerHighSpeedVideoConfiguration(TypeReference<HighSpeedVideoConfiguration> typeReference, int nativeType) {
            super(MarshalQueryableHighSpeedVideoConfiguration.this, typeReference, nativeType);
        }

        @Override
        public void marshal(HighSpeedVideoConfiguration value, ByteBuffer buffer) {
            buffer.putInt(value.getWidth());
            buffer.putInt(value.getHeight());
            buffer.putInt(value.getFpsMin());
            buffer.putInt(value.getFpsMax());
            buffer.putInt(value.getBatchSizeMax());
        }

        @Override
        public HighSpeedVideoConfiguration unmarshal(ByteBuffer buffer) {
            int width = buffer.getInt();
            int height = buffer.getInt();
            int fpsMin = buffer.getInt();
            int fpsMax = buffer.getInt();
            int batchSizeMax = buffer.getInt();
            return new HighSpeedVideoConfiguration(width, height, fpsMin, fpsMax, batchSizeMax);
        }

        @Override
        public int getNativeSize() {
            return 20;
        }
    }

    @Override
    public Marshaler<HighSpeedVideoConfiguration> createMarshaler(TypeReference<HighSpeedVideoConfiguration> managedType, int nativeType) {
        return new MarshalerHighSpeedVideoConfiguration(managedType, nativeType);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<HighSpeedVideoConfiguration> managedType, int nativeType) {
        if (nativeType == 1) {
            return managedType.getType().equals(HighSpeedVideoConfiguration.class);
        }
        return false;
    }
}
