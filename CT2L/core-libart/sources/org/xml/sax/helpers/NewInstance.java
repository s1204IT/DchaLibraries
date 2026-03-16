package org.xml.sax.helpers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class NewInstance {
    NewInstance() {
    }

    static Object newInstance(ClassLoader classLoader, String className) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        Class<?> clsLoadClass;
        if (classLoader == null) {
            clsLoadClass = Class.forName(className);
        } else {
            clsLoadClass = classLoader.loadClass(className);
        }
        return clsLoadClass.newInstance();
    }

    static ClassLoader getClassLoader() {
        try {
            Method m = Thread.class.getMethod("getContextClassLoader", new Class[0]);
            try {
                return (ClassLoader) m.invoke(Thread.currentThread(), new Object[0]);
            } catch (IllegalAccessException e) {
                throw new UnknownError(e.getMessage());
            } catch (InvocationTargetException e2) {
                throw new UnknownError(e2.getMessage());
            }
        } catch (NoSuchMethodException e3) {
            return NewInstance.class.getClassLoader();
        }
    }
}
