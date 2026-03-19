package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.MarshalRegistry;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.utils.TypeReference;
import android.util.Log;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MarshalQueryableArray<T> implements MarshalQueryable<T> {
    private static final boolean DEBUG = false;
    private static final String TAG = MarshalQueryableArray.class.getSimpleName();

    private class MarshalerArray extends Marshaler<T> {
        private final Class<T> mClass;
        private final Class<?> mComponentClass;
        private final Marshaler<?> mComponentMarshaler;

        protected MarshalerArray(TypeReference<T> typeReference, int i) {
            super(MarshalQueryableArray.this, typeReference, i);
            this.mClass = typeReference.getRawType();
            TypeReference<?> componentType = typeReference.getComponentType();
            this.mComponentMarshaler = MarshalRegistry.getMarshaler(componentType, this.mNativeType);
            this.mComponentClass = componentType.getRawType();
        }

        @Override
        public void marshal(T t, ByteBuffer byteBuffer) {
            int length = Array.getLength(t);
            for (int i = 0; i < length; i++) {
                marshalArrayElement(this.mComponentMarshaler, byteBuffer, t, i);
            }
        }

        @Override
        public T unmarshal(ByteBuffer byteBuffer) {
            Object objCopyListToArray;
            int nativeSize = this.mComponentMarshaler.getNativeSize();
            if (nativeSize != Marshaler.NATIVE_SIZE_DYNAMIC) {
                int iRemaining = byteBuffer.remaining();
                int i = iRemaining / nativeSize;
                int i2 = iRemaining % nativeSize;
                if (i2 != 0) {
                    throw new UnsupportedOperationException("Arrays for " + this.mTypeReference + " must be packed tighly into a multiple of " + nativeSize + "; but there are " + i2 + " left over bytes");
                }
                objCopyListToArray = Array.newInstance(this.mComponentClass, i);
                for (int i3 = 0; i3 < i; i3++) {
                    Array.set(objCopyListToArray, i3, this.mComponentMarshaler.unmarshal(byteBuffer));
                }
            } else {
                ArrayList<?> arrayList = new ArrayList<>();
                while (byteBuffer.hasRemaining()) {
                    arrayList.add(this.mComponentMarshaler.unmarshal(byteBuffer));
                }
                objCopyListToArray = copyListToArray(arrayList, Array.newInstance(this.mComponentClass, arrayList.size()));
            }
            if (byteBuffer.remaining() != 0) {
                Log.e(MarshalQueryableArray.TAG, "Trailing bytes (" + byteBuffer.remaining() + ") left over after unpacking " + this.mClass);
            }
            return this.mClass.cast(objCopyListToArray);
        }

        @Override
        public int getNativeSize() {
            return NATIVE_SIZE_DYNAMIC;
        }

        @Override
        public int calculateMarshalSize(T t) {
            int nativeSize = this.mComponentMarshaler.getNativeSize();
            int length = Array.getLength(t);
            if (nativeSize != Marshaler.NATIVE_SIZE_DYNAMIC) {
                return nativeSize * length;
            }
            int iCalculateElementMarshalSize = 0;
            for (int i = 0; i < length; i++) {
                iCalculateElementMarshalSize += calculateElementMarshalSize(this.mComponentMarshaler, t, i);
            }
            return iCalculateElementMarshalSize;
        }

        private <TElem> void marshalArrayElement(Marshaler<TElem> marshaler, ByteBuffer byteBuffer, Object obj, int i) {
            marshaler.marshal(Array.get(obj, i), byteBuffer);
        }

        private Object copyListToArray(ArrayList<?> arrayList, Object obj) {
            return arrayList.toArray((Object[]) obj);
        }

        private <TElem> int calculateElementMarshalSize(Marshaler<TElem> marshaler, Object obj, int i) {
            return marshaler.calculateMarshalSize(Array.get(obj, i));
        }
    }

    @Override
    public Marshaler<T> createMarshaler(TypeReference<T> typeReference, int i) {
        return new MarshalerArray(typeReference, i);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<T> typeReference, int i) {
        return typeReference.getRawType().isArray();
    }
}
