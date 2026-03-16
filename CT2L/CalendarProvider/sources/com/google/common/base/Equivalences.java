package com.google.common.base;

import java.io.Serializable;

public final class Equivalences {
    public static Equivalence<Object> equals() {
        return Equals.INSTANCE;
    }

    public static Equivalence<Object> identity() {
        return Identity.INSTANCE;
    }

    private static final class Equals extends Equivalence<Object> implements Serializable {
        static final Equals INSTANCE = new Equals();
        private static final long serialVersionUID = 1;

        private Equals() {
        }

        @Override
        protected boolean doEquivalent(Object a, Object b) {
            return a.equals(b);
        }

        @Override
        public int doHash(Object o) {
            return o.hashCode();
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    private static final class Identity extends Equivalence<Object> implements Serializable {
        static final Identity INSTANCE = new Identity();
        private static final long serialVersionUID = 1;

        private Identity() {
        }

        @Override
        protected boolean doEquivalent(Object a, Object b) {
            return false;
        }

        @Override
        protected int doHash(Object o) {
            return System.identityHashCode(o);
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }
}
