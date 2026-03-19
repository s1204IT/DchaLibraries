package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.utils.TypeReference;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class MarshalQueryableString implements MarshalQueryable<String> {
    private static final boolean DEBUG = false;
    private static final byte NUL = 0;
    private static final String TAG = MarshalQueryableString.class.getSimpleName();
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private class MarshalerString extends Marshaler<String> {
        protected MarshalerString(TypeReference<String> typeReference, int nativeType) {
            super(MarshalQueryableString.this, typeReference, nativeType);
        }

        @Override
        public void marshal(String value, ByteBuffer buffer) {
            byte[] arr = value.getBytes(MarshalQueryableString.UTF8_CHARSET);
            buffer.put(arr);
            buffer.put((byte) 0);
        }

        @Override
        public int calculateMarshalSize(String value) {
            byte[] arr = value.getBytes(MarshalQueryableString.UTF8_CHARSET);
            return arr.length + 1;
        }

        @Override
        public String unmarshal(ByteBuffer buffer) {
            buffer.mark();
            boolean foundNull = false;
            int stringLength = 0;
            while (true) {
                if (!buffer.hasRemaining()) {
                    break;
                }
                if (buffer.get() == 0) {
                    foundNull = true;
                    break;
                }
                stringLength++;
            }
            if (!foundNull) {
                throw new UnsupportedOperationException("Strings must be null-terminated");
            }
            buffer.reset();
            byte[] strBytes = new byte[stringLength + 1];
            buffer.get(strBytes, 0, stringLength + 1);
            return new String(strBytes, 0, stringLength, MarshalQueryableString.UTF8_CHARSET);
        }

        @Override
        public int getNativeSize() {
            return NATIVE_SIZE_DYNAMIC;
        }
    }

    @Override
    public Marshaler<String> createMarshaler(TypeReference<String> managedType, int nativeType) {
        return new MarshalerString(managedType, nativeType);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<String> managedType, int nativeType) {
        if (nativeType == 0) {
            return String.class.equals(managedType.getType());
        }
        return false;
    }
}
