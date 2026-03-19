package java.text;

import java.io.InvalidObjectException;
import java.io.Serializable;
import java.util.HashMap;
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
        private static final long serialVersionUID = -9142742483513960612L;
        private String name;
        private static final Map<String, Attribute> instanceMap = new HashMap(7);
        public static final Attribute LANGUAGE = new Attribute("language");
        public static final Attribute READING = new Attribute("reading");
        public static final Attribute INPUT_METHOD_SEGMENT = new Attribute("input_method_segment");

        protected Attribute(String str) {
            this.name = str;
            if (getClass() == Attribute.class) {
                instanceMap.put(str, this);
            }
        }

        public final boolean equals(Object obj) {
            return super.equals(obj);
        }

        public final int hashCode() {
            return super.hashCode();
        }

        public String toString() {
            return getClass().getName() + "(" + this.name + ")";
        }

        protected String getName() {
            return this.name;
        }

        protected Object readResolve() throws InvalidObjectException {
            if (getClass() != Attribute.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }
            Attribute attribute = instanceMap.get(getName());
            if (attribute != null) {
                return attribute;
            }
            throw new InvalidObjectException("unknown attribute name");
        }
    }
}
