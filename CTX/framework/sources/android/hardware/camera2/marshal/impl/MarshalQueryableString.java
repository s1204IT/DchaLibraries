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

    private static class PreloadHolder {
        public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

        private PreloadHolder() {
        }
    }

    private class MarshalerString extends Marshaler<String> {
        protected MarshalerString(TypeReference<String> typeReference, int i) {
            super(MarshalQueryableString.this, typeReference, i);
        }

        @Override
        public void marshal(String str, ByteBuffer byteBuffer) {
            byteBuffer.put(str.getBytes(PreloadHolder.UTF8_CHARSET));
            byteBuffer.put((byte) 0);
        }

        @Override
        public int calculateMarshalSize(String str) {
            return str.getBytes(PreloadHolder.UTF8_CHARSET).length + 1;
        }

        @Override
        public String unmarshal(ByteBuffer byteBuffer) {
            boolean z;
            byteBuffer.mark();
            int i = 0;
            while (true) {
                if (byteBuffer.hasRemaining()) {
                    if (byteBuffer.get() != 0) {
                        i++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                throw new UnsupportedOperationException("Strings must be null-terminated");
            }
            byteBuffer.reset();
            int i2 = i + 1;
            byte[] bArr = new byte[i2];
            byteBuffer.get(bArr, 0, i2);
            return new String(bArr, 0, i, PreloadHolder.UTF8_CHARSET);
        }

        @Override
        public int getNativeSize() {
            return NATIVE_SIZE_DYNAMIC;
        }
    }

    @Override
    public Marshaler<String> createMarshaler(TypeReference<String> typeReference, int i) {
        return new MarshalerString(typeReference, i);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<String> typeReference, int i) {
        return i == 0 && String.class.equals(typeReference.getType());
    }
}
