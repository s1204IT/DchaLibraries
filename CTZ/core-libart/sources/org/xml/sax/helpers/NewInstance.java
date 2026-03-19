package org.xml.sax.helpers;

import java.lang.reflect.InvocationTargetException;

class NewInstance {
    NewInstance() {
    }

    static Object newInstance(ClassLoader classLoader, String str) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        Class<?> clsLoadClass;
        if (classLoader == null) {
            clsLoadClass = Class.forName(str);
        } else {
            clsLoadClass = classLoader.loadClass(str);
        }
        return clsLoadClass.newInstance();
    }

    static ClassLoader getClassLoader() {
        try {
            try {
                return (ClassLoader) Thread.class.getMethod("getContextClassLoader", new Class[0]).invoke(Thread.currentThread(), new Object[0]);
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
