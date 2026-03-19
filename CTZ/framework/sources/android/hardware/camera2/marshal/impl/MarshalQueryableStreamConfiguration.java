package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.params.StreamConfiguration;
import android.hardware.camera2.utils.TypeReference;
import java.nio.ByteBuffer;

public class MarshalQueryableStreamConfiguration implements MarshalQueryable<StreamConfiguration> {
    private static final int SIZE = 16;

    private class MarshalerStreamConfiguration extends Marshaler<StreamConfiguration> {
        protected MarshalerStreamConfiguration(TypeReference<StreamConfiguration> typeReference, int i) {
            super(MarshalQueryableStreamConfiguration.this, typeReference, i);
        }

        @Override
        public void marshal(StreamConfiguration streamConfiguration, ByteBuffer byteBuffer) {
            byteBuffer.putInt(streamConfiguration.getFormat());
            byteBuffer.putInt(streamConfiguration.getWidth());
            byteBuffer.putInt(streamConfiguration.getHeight());
            byteBuffer.putInt(streamConfiguration.isInput() ? 1 : 0);
        }

        @Override
        public StreamConfiguration unmarshal(ByteBuffer byteBuffer) {
            return new StreamConfiguration(byteBuffer.getInt(), byteBuffer.getInt(), byteBuffer.getInt(), byteBuffer.getInt() != 0);
        }

        @Override
        public int getNativeSize() {
            return 16;
        }
    }

    @Override
    public Marshaler<StreamConfiguration> createMarshaler(TypeReference<StreamConfiguration> typeReference, int i) {
        return new MarshalerStreamConfiguration(typeReference, i);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<StreamConfiguration> typeReference, int i) {
        return i == 1 && typeReference.getType().equals(StreamConfiguration.class);
    }
}
