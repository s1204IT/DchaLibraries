package com.android.deskclock;

public interface Predicate<T> {
    public static final Predicate TRUE = new Predicate() {
        @Override
        public boolean apply(Object obj) {
            return true;
        }
    };
    public static final Predicate FALSE = new Predicate() {
        @Override
        public boolean apply(Object obj) {
            return false;
        }
    };

    boolean apply(T t);
}
