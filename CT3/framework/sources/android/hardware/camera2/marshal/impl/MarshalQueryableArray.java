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
        public void marshal(T value, ByteBuffer buffer) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                marshalArrayElement(this.mComponentMarshaler, buffer, value, i);
            }
        }

        @Override
        public T unmarshal(ByteBuffer buffer) {
            Object array;
            int elementSize = this.mComponentMarshaler.getNativeSize();
            if (elementSize != Marshaler.NATIVE_SIZE_DYNAMIC) {
                int remaining = buffer.remaining();
                int arraySize = remaining / elementSize;
                if (remaining % elementSize != 0) {
                    throw new UnsupportedOperationException("Arrays for " + this.mTypeReference + " must be packed tighly into a multiple of " + elementSize + "; but there are " + (remaining % elementSize) + " left over bytes");
                }
                array = Array.newInstance(this.mComponentClass, arraySize);
                for (int i = 0; i < arraySize; i++) {
                    Object elem = this.mComponentMarshaler.unmarshal(buffer);
                    Array.set(array, i, elem);
                }
            } else {
                ArrayList<?> arrayList = new ArrayList<>();
                while (buffer.hasRemaining()) {
                    Object elem2 = this.mComponentMarshaler.unmarshal(buffer);
                    arrayList.add(elem2);
                }
                array = copyListToArray(arrayList, Array.newInstance(this.mComponentClass, arrayList.size()));
            }
            if (buffer.remaining() != 0) {
                Log.e(MarshalQueryableArray.TAG, "Trailing bytes (" + buffer.remaining() + ") left over after unpacking " + this.mClass);
            }
            return this.mClass.cast(array);
        }

        @Override
        public int getNativeSize() {
            return NATIVE_SIZE_DYNAMIC;
        }

        @Override
        public int calculateMarshalSize(T value) {
            int elementSize = this.mComponentMarshaler.getNativeSize();
            int arrayLength = Array.getLength(value);
            if (elementSize != Marshaler.NATIVE_SIZE_DYNAMIC) {
                return elementSize * arrayLength;
            }
            int size = 0;
            for (int i = 0; i < arrayLength; i++) {
                size += calculateElementMarshalSize(this.mComponentMarshaler, value, i);
            }
            return size;
        }

        private <TElem> void marshalArrayElement(Marshaler<TElem> marshaler, ByteBuffer buffer, Object array, int index) {
            marshaler.marshal(Array.get(array, index), buffer);
        }

        private Object copyListToArray(ArrayList<?> arrayList, Object arrayDest) {
            return arrayList.toArray((Object[]) arrayDest);
        }

        private <TElem> int calculateElementMarshalSize(Marshaler<TElem> marshaler, Object array, int index) {
            Object elem = Array.get(array, index);
            return marshaler.calculateMarshalSize(elem);
        }
    }

    @Override
    public Marshaler<T> createMarshaler(TypeReference<T> managedType, int nativeType) {
        return new MarshalerArray(managedType, nativeType);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<T> managedType, int nativeType) {
        return managedType.getRawType().isArray();
    }
}
