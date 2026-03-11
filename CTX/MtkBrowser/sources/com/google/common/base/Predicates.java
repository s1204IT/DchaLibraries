package com.google.common.base;

import java.io.Serializable;
import java.util.Collection;

public final class Predicates {
    private static final Joiner COMMA_JOINER = Joiner.on(',');

    private static class InPredicate<T> implements Predicate<T>, Serializable {
        private static final long serialVersionUID = 0;
        private final Collection<?> target;

        private InPredicate(Collection<?> collection) {
            this.target = (Collection) Preconditions.checkNotNull(collection);
        }

        @Override
        public boolean apply(T t) {
            try {
                return this.target.contains(t);
            } catch (ClassCastException e) {
                return false;
            } catch (NullPointerException e2) {
                return false;
            }
        }

        public boolean equals(Object obj) {
            if (obj instanceof InPredicate) {
                return this.target.equals(((InPredicate) obj).target);
            }
            return false;
        }

        public int hashCode() {
            return this.target.hashCode();
        }

        public String toString() {
            return "Predicates.in(" + this.target + ")";
        }
    }

    private static class IsEqualToPredicate<T> implements Predicate<T>, Serializable {
        private static final long serialVersionUID = 0;
        private final T target;

        private IsEqualToPredicate(T t) {
            this.target = t;
        }

        @Override
        public boolean apply(T t) {
            return this.target.equals(t);
        }

        public boolean equals(Object obj) {
            if (obj instanceof IsEqualToPredicate) {
                return this.target.equals(((IsEqualToPredicate) obj).target);
            }
            return false;
        }

        public int hashCode() {
            return this.target.hashCode();
        }

        public String toString() {
            return "Predicates.equalTo(" + this.target + ")";
        }
    }

    enum ObjectPredicate implements Predicate<Object> {
        ALWAYS_TRUE {
            @Override
            public boolean apply(Object obj) {
                return true;
            }

            @Override
            public String toString() {
                return "Predicates.alwaysTrue()";
            }
        },
        ALWAYS_FALSE {
            @Override
            public boolean apply(Object obj) {
                return false;
            }

            @Override
            public String toString() {
                return "Predicates.alwaysFalse()";
            }
        },
        IS_NULL {
            @Override
            public boolean apply(Object obj) {
                return obj == null;
            }

            @Override
            public String toString() {
                return "Predicates.isNull()";
            }
        },
        NOT_NULL {
            @Override
            public boolean apply(Object obj) {
                return obj != null;
            }

            @Override
            public String toString() {
                return "Predicates.notNull()";
            }
        };

        <T> Predicate<T> withNarrowedType() {
            return this;
        }
    }

    public static <T> Predicate<T> equalTo(T t) {
        return t == null ? isNull() : new IsEqualToPredicate(t);
    }

    public static <T> Predicate<T> in(Collection<? extends T> collection) {
        return new InPredicate(collection);
    }

    public static <T> Predicate<T> isNull() {
        return ObjectPredicate.IS_NULL.withNarrowedType();
    }
}
