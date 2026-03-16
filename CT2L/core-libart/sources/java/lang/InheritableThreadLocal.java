package java.lang;

import java.lang.ThreadLocal;

public class InheritableThreadLocal<T> extends ThreadLocal<T> {
    protected T childValue(T parentValue) {
        return parentValue;
    }

    @Override
    ThreadLocal.Values values(Thread current) {
        return current.inheritableValues;
    }

    @Override
    ThreadLocal.Values initializeValues(Thread current) {
        ThreadLocal.Values values = new ThreadLocal.Values();
        current.inheritableValues = values;
        return values;
    }
}
