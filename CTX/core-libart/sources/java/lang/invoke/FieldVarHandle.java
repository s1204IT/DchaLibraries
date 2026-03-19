package java.lang.invoke;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

final class FieldVarHandle extends VarHandle {
    private final long artField;

    private FieldVarHandle(Field field, Class<?> cls) {
        super(field.getType(), Modifier.isFinal(field.getModifiers()), cls);
        this.artField = field.getArtField();
    }

    private FieldVarHandle(Field field) {
        super(field.getType(), Modifier.isFinal(field.getModifiers()));
        this.artField = field.getArtField();
    }

    static FieldVarHandle create(Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            return new FieldVarHandle(field);
        }
        return new FieldVarHandle(field, field.getDeclaringClass());
    }
}
