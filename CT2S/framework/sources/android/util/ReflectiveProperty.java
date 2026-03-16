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

    public ReflectiveProperty(Class<T> propertyHolder, Class<V> valueType, String name) {
        super(valueType, name);
        char firstLetter = Character.toUpperCase(name.charAt(0));
        String theRest = name.substring(1);
        String capitalizedName = firstLetter + theRest;
        String getterName = PREFIX_GET + capitalizedName;
        try {
            this.mGetter = propertyHolder.getMethod(getterName, (Class[]) null);
        } catch (NoSuchMethodException e) {
            String getterName2 = PREFIX_IS + capitalizedName;
            try {
                this.mGetter = propertyHolder.getMethod(getterName2, (Class[]) null);
            } catch (NoSuchMethodException e2) {
                try {
                    this.mField = propertyHolder.getField(name);
                    Class<?> type = this.mField.getType();
                    if (!typesMatch(valueType, type)) {
                        throw new NoSuchPropertyException("Underlying type (" + type + ") does not match Property type (" + valueType + ")");
                    }
                    return;
                } catch (NoSuchFieldException e3) {
                    throw new NoSuchPropertyException("No accessor method or field found for property with name " + name);
                }
            }
        }
        Class<?> returnType = this.mGetter.getReturnType();
        if (!typesMatch(valueType, returnType)) {
            throw new NoSuchPropertyException("Underlying type (" + returnType + ") does not match Property type (" + valueType + ")");
        }
        String setterName = PREFIX_SET + capitalizedName;
        try {
            this.mSetter = propertyHolder.getMethod(setterName, returnType);
        } catch (NoSuchMethodException e4) {
        }
    }

    private boolean typesMatch(Class<V> valueType, Class getterType) {
        if (getterType == valueType) {
            return true;
        }
        if (getterType.isPrimitive()) {
            return (getterType == Float.TYPE && valueType == Float.class) || (getterType == Integer.TYPE && valueType == Integer.class) || ((getterType == Boolean.TYPE && valueType == Boolean.class) || ((getterType == Long.TYPE && valueType == Long.class) || ((getterType == Double.TYPE && valueType == Double.class) || ((getterType == Short.TYPE && valueType == Short.class) || ((getterType == Byte.TYPE && valueType == Byte.class) || (getterType == Character.TYPE && valueType == Character.class))))));
        }
        return false;
    }

    @Override
    public void set(T object, V value) {
        if (this.mSetter != null) {
            try {
                this.mSetter.invoke(object, value);
                return;
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            } catch (InvocationTargetException e2) {
                throw new RuntimeException(e2.getCause());
            }
        }
        if (this.mField != null) {
            try {
                this.mField.set(object, value);
                return;
            } catch (IllegalAccessException e3) {
                throw new AssertionError();
            }
        }
        throw new UnsupportedOperationException("Property " + getName() + " is read-only");
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
