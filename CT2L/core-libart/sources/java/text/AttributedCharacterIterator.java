package java.text;

import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

public interface AttributedCharacterIterator extends CharacterIterator {
    Set<Attribute> getAllAttributeKeys();

    Object getAttribute(Attribute attribute);

    Map<Attribute, Object> getAttributes();

    int getRunLimit();

    int getRunLimit(Attribute attribute);

    int getRunLimit(Set<? extends Attribute> set);

    int getRunStart();

    int getRunStart(Attribute attribute);

    int getRunStart(Set<? extends Attribute> set);

    public static class Attribute implements Serializable {
        public static final Attribute INPUT_METHOD_SEGMENT = new Attribute("input_method_segment");
        public static final Attribute LANGUAGE = new Attribute("language");
        public static final Attribute READING = new Attribute("reading");
        private static final long serialVersionUID = -9142742483513960612L;
        private String name;

        protected Attribute(String name) {
            this.name = name;
        }

        public final boolean equals(Object object) {
            return this == object;
        }

        protected String getName() {
            return this.name;
        }

        public final int hashCode() {
            return super.hashCode();
        }

        protected Object readResolve() throws InvalidObjectException {
            try {
                Field[] arr$ = getClass().getFields();
                for (Field field : arr$) {
                    if (field.getType() == getClass() && Modifier.isStatic(field.getModifiers())) {
                        Attribute candidate = (Attribute) field.get(null);
                        if (this.name.equals(candidate.name)) {
                            return candidate;
                        }
                    }
                }
            } catch (IllegalAccessException e) {
            }
            throw new InvalidObjectException("Failed to resolve " + this);
        }

        public String toString() {
            return getClass().getName() + '(' + getName() + ')';
        }
    }
}
