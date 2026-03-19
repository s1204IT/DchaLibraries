package java.lang;

import java.lang.ThreadLocal;

public class InheritableThreadLocal<T> extends ThreadLocal<T> {
    @Override
    protected T childValue(T t) {
        return t;
    }

    @Override
    ThreadLocal.ThreadLocalMap getMap(Thread thread) {
        return thread.inheritableThreadLocals;
    }

    @Override
    void createMap(Thread thread, T t) {
        thread.inheritableThreadLocals = new ThreadLocal.ThreadLocalMap(this, t);
    }
}
