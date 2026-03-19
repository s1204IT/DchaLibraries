package java.lang.reflect;

import dalvik.annotation.optimization.FastNative;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import libcore.util.EmptyArray;
import sun.reflect.CallerSensitive;

public class Proxy implements Serializable {
    private static final WeakCache<ClassLoader, Class<?>[], Class<?>> proxyClassCache;
    private static final long serialVersionUID = -2222568056686623797L;
    protected InvocationHandler h;
    private static final Class<?>[] constructorParams = {InvocationHandler.class};
    private static final Object key0 = new Object();
    private static final Comparator<Method> ORDER_BY_SIGNATURE_AND_SUBTYPE = new Comparator<Method>() {
        @Override
        public int compare(Method method, Method method2) {
            int iCompare = Method.ORDER_BY_SIGNATURE.compare(method, method2);
            if (iCompare != 0) {
                return iCompare;
            }
            Class<?> declaringClass = method.getDeclaringClass();
            Class<?> declaringClass2 = method2.getDeclaringClass();
            if (declaringClass == declaringClass2) {
                return 0;
            }
            if (declaringClass.isAssignableFrom(declaringClass2)) {
                return 1;
            }
            if (!declaringClass2.isAssignableFrom(declaringClass)) {
                return 0;
            }
            return -1;
        }
    };

    @FastNative
    private static native Class<?> generateProxy(String str, Class<?>[] clsArr, ClassLoader classLoader, Method[] methodArr, Class<?>[][] clsArr2);

    static {
        proxyClassCache = new WeakCache<>(new KeyFactory(), new ProxyClassFactory());
    }

    private Proxy() {
    }

    protected Proxy(InvocationHandler invocationHandler) {
        Objects.requireNonNull(invocationHandler);
        this.h = invocationHandler;
    }

    @CallerSensitive
    public static Class<?> getProxyClass(ClassLoader classLoader, Class<?>... clsArr) throws IllegalArgumentException {
        return getProxyClass0(classLoader, clsArr);
    }

    private static Class<?> getProxyClass0(ClassLoader classLoader, Class<?>... clsArr) {
        if (clsArr.length > 65535) {
            throw new IllegalArgumentException("interface limit exceeded");
        }
        return proxyClassCache.get(classLoader, clsArr);
    }

    private static final class Key1 extends WeakReference<Class<?>> {
        private final int hash;

        Key1(Class<?> cls) {
            super(cls);
            this.hash = cls.hashCode();
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            Class<?> cls;
            return this == obj || (obj != null && obj.getClass() == Key1.class && (cls = get()) != null && cls == ((Key1) obj).get());
        }
    }

    private static final class Key2 extends WeakReference<Class<?>> {
        private final int hash;
        private final WeakReference<Class<?>> ref2;

        Key2(Class<?> cls, Class<?> cls2) {
            super(cls);
            this.hash = (31 * cls.hashCode()) + cls2.hashCode();
            this.ref2 = new WeakReference<>(cls2);
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            Class<?> cls;
            Class<?> cls2;
            if (this != obj) {
                if (obj != null && obj.getClass() == Key2.class && (cls = get()) != null) {
                    Key2 key2 = (Key2) obj;
                    if (cls != key2.get() || (cls2 = this.ref2.get()) == null || cls2 != key2.ref2.get()) {
                    }
                }
                return false;
            }
            return true;
        }
    }

    private static final class KeyX {
        private final int hash;
        private final WeakReference<Class<?>>[] refs;

        KeyX(Class<?>[] clsArr) {
            this.hash = Arrays.hashCode(clsArr);
            this.refs = new WeakReference[clsArr.length];
            for (int i = 0; i < clsArr.length; i++) {
                this.refs[i] = new WeakReference<>(clsArr[i]);
            }
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            return this == obj || (obj != null && obj.getClass() == KeyX.class && equals(this.refs, ((KeyX) obj).refs));
        }

        private static boolean equals(WeakReference<Class<?>>[] weakReferenceArr, WeakReference<Class<?>>[] weakReferenceArr2) {
            if (weakReferenceArr.length != weakReferenceArr2.length) {
                return false;
            }
            for (int i = 0; i < weakReferenceArr.length; i++) {
                Class<?> cls = weakReferenceArr[i].get();
                if (cls == null || cls != weakReferenceArr2[i].get()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class KeyFactory implements BiFunction<ClassLoader, Class<?>[], Object> {
        private KeyFactory() {
        }

        @Override
        public Object apply(ClassLoader classLoader, Class<?>[] clsArr) {
            switch (clsArr.length) {
                case 0:
                    return Proxy.key0;
                case 1:
                    return new Key1(clsArr[0]);
                case 2:
                    return new Key2(clsArr[0], clsArr[1]);
                default:
                    return new KeyX(clsArr);
            }
        }
    }

    private static final class ProxyClassFactory implements BiFunction<ClassLoader, Class<?>[], Class<?>> {
        private static final AtomicLong nextUniqueNumber = new AtomicLong();
        private static final String proxyClassNamePrefix = "$Proxy";

        private ProxyClassFactory() {
        }

        @Override
        public Class<?> apply(ClassLoader classLoader, Class<?>[] clsArr) throws ClassNotFoundException {
            IdentityHashMap identityHashMap = new IdentityHashMap(clsArr.length);
            int length = clsArr.length;
            int i = 0;
            while (true) {
                String str = null;
                Class<?> cls = null;
                if (i < length) {
                    Class<?> cls2 = clsArr[i];
                    try {
                        cls = Class.forName(cls2.getName(), false, classLoader);
                    } catch (ClassNotFoundException e) {
                    }
                    if (cls != cls2) {
                        throw new IllegalArgumentException(((Object) cls2) + " is not visible from class loader");
                    }
                    if (!cls.isInterface()) {
                        throw new IllegalArgumentException(cls.getName() + " is not an interface");
                    }
                    if (identityHashMap.put(cls, Boolean.TRUE) == 0) {
                        i++;
                    } else {
                        throw new IllegalArgumentException("repeated interface: " + cls.getName());
                    }
                } else {
                    for (Class<?> cls3 : clsArr) {
                        if (!Modifier.isPublic(cls3.getModifiers())) {
                            String name = cls3.getName();
                            int iLastIndexOf = name.lastIndexOf(46);
                            String strSubstring = iLastIndexOf == -1 ? "" : name.substring(0, iLastIndexOf + 1);
                            if (str == null) {
                                str = strSubstring;
                            } else if (!strSubstring.equals(str)) {
                                throw new IllegalArgumentException("non-public interfaces from different packages");
                            }
                        }
                    }
                    if (str == null) {
                        str = "";
                    }
                    List methods = Proxy.getMethods(clsArr);
                    Collections.sort(methods, Proxy.ORDER_BY_SIGNATURE_AND_SUBTYPE);
                    Proxy.validateReturnTypes(methods);
                    List listDeduplicateAndGetExceptions = Proxy.deduplicateAndGetExceptions(methods);
                    return Proxy.generateProxy(str + proxyClassNamePrefix + nextUniqueNumber.getAndIncrement(), clsArr, classLoader, (Method[]) methods.toArray(new Method[methods.size()]), (Class[][]) listDeduplicateAndGetExceptions.toArray(new Class[listDeduplicateAndGetExceptions.size()][]));
                }
            }
        }
    }

    private static List<Class<?>[]> deduplicateAndGetExceptions(List<Method> list) {
        ArrayList arrayList = new ArrayList(list.size());
        int i = 0;
        while (i < list.size()) {
            Method method = list.get(i);
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            if (i > 0) {
                int i2 = i - 1;
                if (Method.ORDER_BY_SIGNATURE.compare(method, list.get(i2)) == 0) {
                    arrayList.set(i2, intersectExceptions((Class[]) arrayList.get(i2), exceptionTypes));
                    list.remove(i);
                }
            }
            arrayList.add(exceptionTypes);
            i++;
        }
        return arrayList;
    }

    private static Class<?>[] intersectExceptions(Class<?>[] clsArr, Class<?>[] clsArr2) {
        if (clsArr.length == 0 || clsArr2.length == 0) {
            return EmptyArray.CLASS;
        }
        if (Arrays.equals(clsArr, clsArr2)) {
            return clsArr;
        }
        HashSet hashSet = new HashSet();
        for (Class<?> cls : clsArr) {
            for (Class<?> cls2 : clsArr2) {
                if (cls.isAssignableFrom(cls2)) {
                    hashSet.add(cls2);
                } else if (cls2.isAssignableFrom(cls)) {
                    hashSet.add(cls);
                }
            }
        }
        return (Class[]) hashSet.toArray(new Class[hashSet.size()]);
    }

    private static void validateReturnTypes(List<Method> list) {
        Method method = null;
        for (Method method2 : list) {
            if (method == null || !method.equalNameAndParameters(method2)) {
                method = method2;
            } else {
                Class<?> returnType = method2.getReturnType();
                Class<?> returnType2 = method.getReturnType();
                if (!returnType.isInterface() || !returnType2.isInterface()) {
                    if (!returnType2.isAssignableFrom(returnType)) {
                        if (!returnType.isAssignableFrom(returnType2)) {
                            throw new IllegalArgumentException("proxied interface methods have incompatible return types:\n  " + ((Object) method) + "\n  " + ((Object) method2));
                        }
                    } else {
                        method = method2;
                    }
                }
            }
        }
    }

    private static List<Method> getMethods(Class<?>[] clsArr) {
        ArrayList arrayList = new ArrayList();
        try {
            arrayList.add(Object.class.getMethod("equals", Object.class));
            arrayList.add(Object.class.getMethod("hashCode", EmptyArray.CLASS));
            arrayList.add(Object.class.getMethod("toString", EmptyArray.CLASS));
            getMethodsRecursive(clsArr, arrayList);
            return arrayList;
        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        }
    }

    private static void getMethodsRecursive(Class<?>[] clsArr, List<Method> list) {
        for (Class<?> cls : clsArr) {
            getMethodsRecursive(cls.getInterfaces(), list);
            Collections.addAll(list, cls.getDeclaredMethods());
        }
    }

    @CallerSensitive
    public static Object newProxyInstance(ClassLoader classLoader, Class<?>[] clsArr, InvocationHandler invocationHandler) throws IllegalArgumentException {
        Objects.requireNonNull(invocationHandler);
        Class<?> proxyClass0 = getProxyClass0(classLoader, (Class[]) clsArr.clone());
        try {
            Constructor<?> constructor = proxyClass0.getConstructor(constructorParams);
            if (!Modifier.isPublic(proxyClass0.getModifiers())) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance(invocationHandler);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new InternalError(e.toString(), e);
        } catch (NoSuchMethodException e2) {
            throw new InternalError(e2.toString(), e2);
        } catch (InvocationTargetException e3) {
            Throwable cause = e3.getCause();
            if (cause instanceof RuntimeException) {
                throw ((RuntimeException) cause);
            }
            throw new InternalError(cause.toString(), cause);
        }
    }

    public static boolean isProxyClass(Class<?> cls) {
        return Proxy.class.isAssignableFrom(cls) && proxyClassCache.containsValue(cls);
    }

    @CallerSensitive
    public static InvocationHandler getInvocationHandler(Object obj) throws IllegalArgumentException {
        if (!isProxyClass(obj.getClass())) {
            throw new IllegalArgumentException("not a proxy instance");
        }
        return ((Proxy) obj).h;
    }

    private static Object invoke(Proxy proxy, Method method, Object[] objArr) throws Throwable {
        return proxy.h.invoke(proxy, method, objArr);
    }
}
