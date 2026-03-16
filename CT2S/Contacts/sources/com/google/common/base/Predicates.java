package com.google.common.base;

import java.io.Serializable;

public final class Predicates {
    private static final Joiner COMMA_JOINER = Joiner.on(",");

    public static Predicate<Object> instanceOf(Class<?> clazz) {
        return new InstanceOfPredicate(clazz);
    }

    private static class InstanceOfPredicate implements Predicate<Object>, Serializable {
        private static final long serialVersionUID = 0;
        private final Class<?> clazz;

        private InstanceOfPredicate(Class<?> clazz) {
            this.clazz = (Class) Preconditions.checkNotNull(clazz);
        }

        @Override
        public boolean apply(Object o) {
            return this.clazz.isInstance(o);
        }

        public int hashCode() {
            return this.clazz.hashCode();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof InstanceOfPredicate)) {
                return false;
            }
            InstanceOfPredicate that = (InstanceOfPredicate) obj;
            return this.clazz == that.clazz;
        }

        public String toString() {
            return "IsInstanceOf(" + this.clazz.getName() + ")";
        }
    }
}
