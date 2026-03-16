package android.hardware.camera2.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class Decorator<T> implements InvocationHandler {
    private final DecoratorListener mListener;
    private final T mObject;

    public interface DecoratorListener {
        void onAfterInvocation(Method method, Object[] objArr, Object obj);

        void onBeforeInvocation(Method method, Object[] objArr);

        boolean onCatchException(Method method, Object[] objArr, Throwable th);

        void onFinally(Method method, Object[] objArr);
    }

    public static <T> T newInstance(T t, DecoratorListener decoratorListener) {
        return (T) Proxy.newProxyInstance(t.getClass().getClassLoader(), t.getClass().getInterfaces(), new Decorator(t, decoratorListener));
    }

    private Decorator(T obj, DecoratorListener listener) {
        this.mObject = obj;
        this.mListener = listener;
    }

    @Override
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        Object result = null;
        try {
            this.mListener.onBeforeInvocation(m, args);
            result = m.invoke(this.mObject, args);
            this.mListener.onAfterInvocation(m, args, result);
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (!this.mListener.onCatchException(m, args, t)) {
                throw t;
            }
        } finally {
            this.mListener.onFinally(m, args);
        }
        return result;
    }
}
