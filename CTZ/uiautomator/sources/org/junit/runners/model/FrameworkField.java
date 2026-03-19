package org.junit.runners.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class FrameworkField extends FrameworkMember<FrameworkField> {
    private final Field field;

    FrameworkField(Field field) {
        if (field == null) {
            throw new NullPointerException("FrameworkField cannot be created without an underlying field.");
        }
        this.field = field;
    }

    @Override
    public String getName() {
        return getField().getName();
    }

    @Override
    public Annotation[] getAnnotations() {
        return this.field.getAnnotations();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> cls) {
        return (T) this.field.getAnnotation(cls);
    }

    @Override
    public boolean isShadowedBy(FrameworkField frameworkField) {
        return frameworkField.getName().equals(getName());
    }

    @Override
    protected int getModifiers() {
        return this.field.getModifiers();
    }

    public Field getField() {
        return this.field;
    }

    @Override
    public Class<?> getType() {
        return this.field.getType();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return this.field.getDeclaringClass();
    }

    public Object get(Object obj) throws IllegalAccessException, IllegalArgumentException {
        return this.field.get(obj);
    }

    public String toString() {
        return this.field.toString();
    }
}
