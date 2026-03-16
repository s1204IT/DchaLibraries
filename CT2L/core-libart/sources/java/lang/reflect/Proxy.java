package java.lang.reflect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import libcore.util.EmptyArray;

public class Proxy implements Serializable {
    private static final long serialVersionUID = -2222568056686623797L;
    protected InvocationHandler h;
    private static int nextClassNameIndex = 0;
    private static final Comparator<Method> ORDER_BY_SIGNATURE_AND_SUBTYPE = new Comparator<Method>() {
        @Override
        public int compare(Method a, Method b) {
            int comparison = Method.ORDER_BY_SIGNATURE.compare(a, b);
            if (comparison == 0) {
                Class<?> aClass = a.getDeclaringClass();
                Class<?> bClass = b.getDeclaringClass();
                if (aClass == bClass) {
                    return 0;
                }
                if (aClass.isAssignableFrom(bClass)) {
                    return 1;
                }
                return bClass.isAssignableFrom(aClass) ? -1 : 0;
            }
            return comparison;
        }
    };

    private static native void constructorPrototype(InvocationHandler invocationHandler);

    private static native Class<?> generateProxy(String str, Class<?>[] clsArr, ClassLoader classLoader, ArtMethod[] artMethodArr, Class<?>[][] clsArr2);

    private Proxy() {
    }

    protected Proxy(InvocationHandler h) {
        this.h = h;
    }

    public static Class<?> getProxyClass(ClassLoader loader, Class<?>... interfaces) throws IllegalArgumentException {
        Class<?> result;
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        if (interfaces == null) {
            throw new NullPointerException("interfaces == null");
        }
        List<Class<?>> interfaceList = new ArrayList<>(interfaces.length);
        Collections.addAll(interfaceList, interfaces);
        Set<Class<?>> interfaceSet = new HashSet<>(interfaceList);
        if (interfaceSet.contains(null)) {
            throw new NullPointerException("interface list contains null: " + interfaceList);
        }
        if (interfaceSet.size() != interfaces.length) {
            throw new IllegalArgumentException("duplicate interface in list: " + interfaceList);
        }
        synchronized (loader.proxyCache) {
            Class<?> proxy = loader.proxyCache.get(interfaceList);
            if (proxy == null) {
                String commonPackageName = null;
                for (Class<?> c : interfaces) {
                    if (!c.isInterface()) {
                        throw new IllegalArgumentException(c + " is not an interface");
                    }
                    if (!isVisibleToClassLoader(loader, c)) {
                        throw new IllegalArgumentException(c + " is not visible from class loader");
                    }
                    if (!Modifier.isPublic(c.getModifiers())) {
                        String packageName = c.getPackageName$();
                        if (packageName == null) {
                            packageName = "";
                        }
                        if (commonPackageName != null && !commonPackageName.equals(packageName)) {
                            throw new IllegalArgumentException("non-public interfaces must be in the same package");
                        }
                        commonPackageName = packageName;
                    }
                }
                List<Method> methods = getMethods(interfaces);
                Collections.sort(methods, ORDER_BY_SIGNATURE_AND_SUBTYPE);
                validateReturnTypes(methods);
                List<Class<?>[]> exceptions = deduplicateAndGetExceptions(methods);
                ArtMethod[] methodsArray = new ArtMethod[methods.size()];
                for (int i = 0; i < methodsArray.length; i++) {
                    methodsArray[i] = methods.get(i).getArtMethod();
                }
                Class<?>[][] exceptionsArray = (Class[][]) exceptions.toArray(new Class[exceptions.size()][]);
                String baseName = (commonPackageName == null || commonPackageName.isEmpty()) ? "$Proxy" : commonPackageName + ".$Proxy";
                synchronized (loader.proxyCache) {
                    result = loader.proxyCache.get(interfaceSet);
                    if (result == null) {
                        StringBuilder sbAppend = new StringBuilder().append(baseName);
                        int i2 = nextClassNameIndex;
                        nextClassNameIndex = i2 + 1;
                        String name = sbAppend.append(i2).toString();
                        result = generateProxy(name, interfaces, loader, methodsArray, exceptionsArray);
                        loader.proxyCache.put(interfaceList, result);
                    }
                }
                return result;
            }
            return proxy;
        }
    }

    private static boolean isVisibleToClassLoader(ClassLoader loader, Class<?> c) {
        try {
            if (loader != c.getClassLoader()) {
                if (c != Class.forName(c.getName(), false, loader)) {
                    return false;
                }
            }
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Object newProxyInstance(ClassLoader loader, Class<?>[] interfaces, InvocationHandler invocationHandler) throws IllegalArgumentException {
        Exception cause;
        if (invocationHandler == null) {
            throw new NullPointerException("invocationHandler == null");
        }
        try {
            return getProxyClass(loader, interfaces).getConstructor(InvocationHandler.class).newInstance(invocationHandler);
        } catch (IllegalAccessException e) {
            cause = e;
            AssertionError error = new AssertionError();
            error.initCause(cause);
            throw error;
        } catch (InstantiationException e2) {
            cause = e2;
            AssertionError error2 = new AssertionError();
            error2.initCause(cause);
            throw error2;
        } catch (NoSuchMethodException e3) {
            cause = e3;
            AssertionError error22 = new AssertionError();
            error22.initCause(cause);
            throw error22;
        } catch (InvocationTargetException e4) {
            cause = e4;
            AssertionError error222 = new AssertionError();
            error222.initCause(cause);
            throw error222;
        }
    }

    public static boolean isProxyClass(Class<?> cl) {
        return cl.isProxy();
    }

    public static InvocationHandler getInvocationHandler(Object proxy) throws IllegalArgumentException {
        if (!(proxy instanceof Proxy)) {
            throw new IllegalArgumentException("not a proxy instance");
        }
        return ((Proxy) proxy).h;
    }

    private static List<Method> getMethods(Class<?>[] interfaces) {
        List<Method> result = new ArrayList<>();
        try {
            result.add(Object.class.getMethod("equals", Object.class));
            result.add(Object.class.getMethod("hashCode", EmptyArray.CLASS));
            result.add(Object.class.getMethod("toString", EmptyArray.CLASS));
            getMethodsRecursive(interfaces, result);
            return result;
        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        }
    }

    private static void getMethodsRecursive(Class<?>[] interfaces, List<Method> methods) {
        for (Class<?> i : interfaces) {
            getMethodsRecursive(i.getInterfaces(), methods);
            Collections.addAll(methods, i.getDeclaredMethods());
        }
    }

    private static void validateReturnTypes(List<Method> methods) {
        Method vs = null;
        for (Method method : methods) {
            if (vs == null || !vs.equalNameAndParameters(method)) {
                vs = method;
            } else {
                Class<?> returnType = method.getReturnType();
                Class<?> vsReturnType = vs.getReturnType();
                if (!returnType.isInterface() || !vsReturnType.isInterface()) {
                    if (vsReturnType.isAssignableFrom(returnType)) {
                        vs = method;
                    } else if (!returnType.isAssignableFrom(vsReturnType)) {
                        throw new IllegalArgumentException("proxied interface methods have incompatible return types:\n  " + vs + "\n  " + method);
                    }
                }
            }
        }
    }

    private static List<Class<?>[]> deduplicateAndGetExceptions(List<Method> methods) {
        List<Class<?>[]> exceptions = new ArrayList<>(methods.size());
        int i = 0;
        while (i < methods.size()) {
            Method method = methods.get(i);
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            if (i > 0 && Method.ORDER_BY_SIGNATURE.compare(method, methods.get(i - 1)) == 0) {
                exceptions.set(i - 1, intersectExceptions(exceptions.get(i - 1), exceptionTypes));
                methods.remove(i);
            } else {
                exceptions.add(exceptionTypes);
                i++;
            }
        }
        return exceptions;
    }

    private static Class<?>[] intersectExceptions(Class<?>[] aExceptions, Class<?>[] bExceptions) {
        if (aExceptions.length == 0 || bExceptions.length == 0) {
            return EmptyArray.CLASS;
        }
        if (!Arrays.equals(aExceptions, bExceptions)) {
            Set<Class<?>> intersection = new HashSet<>();
            for (Class<?> a : aExceptions) {
                for (Class<?> b : bExceptions) {
                    if (a.isAssignableFrom(b)) {
                        intersection.add(b);
                    } else if (b.isAssignableFrom(a)) {
                        intersection.add(a);
                    }
                }
            }
            return (Class[]) intersection.toArray(new Class[intersection.size()]);
        }
        return aExceptions;
    }

    static Object invoke(Proxy proxy, ArtMethod method, Object[] args) throws Throwable {
        InvocationHandler h = proxy.h;
        return h.invoke(proxy, new Method(method), args);
    }
}
