package android.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ReflectiveProperty<T, V> extends Property<T, V> {
    private static final String PREFIX_GET = "get";
    private static final String PREFIX_IS = "is";
    private static final String PREFIX_SET = "set";
    private Field mField;
    private Method mGetter;
    private Method mSetter;

    public ReflectiveProperty(Class<T> cls, Class<V> cls2, String str) {
        super(cls2, str);
        String str2 = Character.toUpperCase(str.charAt(0)) + str.substring(1);
        try {
            this.mGetter = cls.getMethod(PREFIX_GET + str2, (Class[]) null);
        } catch (NoSuchMethodException e) {
            try {
                this.mGetter = cls.getMethod(PREFIX_IS + str2, (Class[]) null);
            } catch (NoSuchMethodException e2) {
                try {
                    this.mField = cls.getField(str);
                    Class<?> type = this.mField.getType();
                    if (!typesMatch(cls2, type)) {
                        throw new NoSuchPropertyException("Underlying type (" + type + ") does not match Property type (" + cls2 + ")");
                    }
                    return;
                } catch (NoSuchFieldException e3) {
                    throw new NoSuchPropertyException("No accessor method or field found for property with name " + str);
                }
            }
        }
        Class<?> returnType = this.mGetter.getReturnType();
        if (!typesMatch(cls2, returnType)) {
            throw new NoSuchPropertyException("Underlying type (" + returnType + ") does not match Property type (" + cls2 + ")");
        }
        try {
            this.mSetter = cls.getMethod(PREFIX_SET + str2, returnType);
        } catch (NoSuchMethodException e4) {
        }
    }

    private boolean typesMatch(Class<V> cls, Class cls2) {
        if (cls2 == cls) {
            return true;
        }
        if (!cls2.isPrimitive()) {
            return false;
        }
        if (cls2 == Float.TYPE && cls == Float.class) {
            return true;
        }
        if (cls2 == Integer.TYPE && cls == Integer.class) {
            return true;
        }
        if (cls2 == Boolean.TYPE && cls == Boolean.class) {
            return true;
        }
        if (cls2 == Long.TYPE && cls == Long.class) {
            return true;
        }
        if (cls2 == Double.TYPE && cls == Double.class) {
            return true;
        }
        if (cls2 == Short.TYPE && cls == Short.class) {
            return true;
        }
        if (cls2 == Byte.TYPE && cls == Byte.class) {
            return true;
        }
        return cls2 == Character.TYPE && cls == Character.class;
    }

    @Override
    public void set(T t, V v) {
        if (this.mSetter != null) {
            try {
                this.mSetter.invoke(t, v);
                return;
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            } catch (InvocationTargetException e2) {
                throw new RuntimeException(e2.getCause());
            }
        }
        if (this.mField != null) {
            try {
                this.mField.set(t, v);
            } catch (IllegalAccessException e3) {
                throw new AssertionError();
            }
        } else {
            throw new UnsupportedOperationException("Property " + getName() + " is read-only");
        }
    }

    @Override
    public V get(T t) {
        if (this.mGetter != null) {
            try {
                return (V) this.mGetter.invoke(t, (Object[]) null);
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            } catch (InvocationTargetException e2) {
                throw new RuntimeException(e2.getCause());
            }
        }
        if (this.mField != null) {
            try {
                return (V) this.mField.get(t);
            } catch (IllegalAccessException e3) {
                throw new AssertionError();
            }
        }
        throw new AssertionError();
    }

    @Override
    public boolean isReadOnly() {
        return this.mSetter == null && this.mField == null;
    }
}
