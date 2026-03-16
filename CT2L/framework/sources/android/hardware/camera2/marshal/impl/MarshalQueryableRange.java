package android.hardware.camera2.marshal.impl;

import android.hardware.camera2.marshal.MarshalQueryable;
import android.hardware.camera2.marshal.MarshalRegistry;
import android.hardware.camera2.marshal.Marshaler;
import android.hardware.camera2.utils.TypeReference;
import android.util.Range;
import java.lang.Comparable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;

public class MarshalQueryableRange<T extends Comparable<? super T>> implements MarshalQueryable<Range<T>> {
    private static final int RANGE_COUNT = 2;

    private class MarshalerRange extends Marshaler<Range<T>> {
        private final Class<? super Range<T>> mClass;
        private final Constructor<Range<T>> mConstructor;
        private final Marshaler<T> mNestedTypeMarshaler;

        protected MarshalerRange(TypeReference<Range<T>> typeReference, int i) {
            super(MarshalQueryableRange.this, typeReference, i);
            this.mClass = typeReference.getRawType();
            try {
                this.mNestedTypeMarshaler = MarshalRegistry.getMarshaler(TypeReference.createSpecializedTypeReference(((ParameterizedType) typeReference.getType()).getActualTypeArguments()[0]), this.mNativeType);
                try {
                    this.mConstructor = this.mClass.getConstructor(Comparable.class, Comparable.class);
                } catch (NoSuchMethodException e) {
                    throw new AssertionError(e);
                }
            } catch (ClassCastException e2) {
                throw new AssertionError("Raw use of Range is not supported", e2);
            }
        }

        @Override
        public void marshal(Range<T> range, ByteBuffer byteBuffer) {
            this.mNestedTypeMarshaler.marshal((T) range.getLower(), byteBuffer);
            this.mNestedTypeMarshaler.marshal((T) range.getUpper(), byteBuffer);
        }

        @Override
        public Range<T> unmarshal(ByteBuffer buffer) {
            T lower = this.mNestedTypeMarshaler.unmarshal(buffer);
            T upper = this.mNestedTypeMarshaler.unmarshal(buffer);
            try {
                return this.mConstructor.newInstance(lower, upper);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (IllegalArgumentException e2) {
                throw new AssertionError(e2);
            } catch (InstantiationException e3) {
                throw new AssertionError(e3);
            } catch (InvocationTargetException e4) {
                throw new AssertionError(e4);
            }
        }

        @Override
        public int getNativeSize() {
            int nestedSize = this.mNestedTypeMarshaler.getNativeSize();
            return nestedSize != NATIVE_SIZE_DYNAMIC ? nestedSize * 2 : NATIVE_SIZE_DYNAMIC;
        }

        @Override
        public int calculateMarshalSize(Range<T> range) {
            int nativeSize = getNativeSize();
            return nativeSize != NATIVE_SIZE_DYNAMIC ? nativeSize : this.mNestedTypeMarshaler.calculateMarshalSize((T) range.getLower()) + this.mNestedTypeMarshaler.calculateMarshalSize((T) range.getUpper());
        }
    }

    @Override
    public Marshaler<Range<T>> createMarshaler(TypeReference<Range<T>> managedType, int nativeType) {
        return new MarshalerRange(managedType, nativeType);
    }

    @Override
    public boolean isTypeMappingSupported(TypeReference<Range<T>> managedType, int nativeType) {
        return Range.class.equals(managedType.getRawType());
    }
}
