package java.lang;

import dalvik.annotation.optimization.FastNative;
import dalvik.system.ClassExt;
import dalvik.system.VMStack;
import java.awt.font.NumericShaper;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import libcore.reflect.GenericSignatureParser;
import libcore.reflect.Types;
import libcore.util.BasicLruCache;
import libcore.util.CollectionUtils;
import libcore.util.EmptyArray;
import sun.reflect.CallerSensitive;

public final class Class<T> implements Serializable, GenericDeclaration, Type, AnnotatedElement {
    private static final int ANNOTATION = 8192;
    private static final int ENUM = 16384;
    private static final int FINALIZABLE = Integer.MIN_VALUE;
    private static final int SYNTHETIC = 4096;
    private static final long serialVersionUID = 3206093459760846163L;
    private transient int accessFlags;
    private transient int classFlags;
    private transient ClassLoader classLoader;
    private transient int classSize;
    private transient int clinitThreadId;
    private transient Class<?> componentType;
    private transient short copiedMethodsOffset;
    private transient Object dexCache;
    private transient int dexClassDefIndex;
    private volatile transient int dexTypeIndex;
    private transient ClassExt extData;
    private transient long iFields;
    private transient Object[] ifTable;
    private transient long methods;
    private transient String name;
    private transient int numReferenceInstanceFields;
    private transient int numReferenceStaticFields;
    private transient int objectSize;
    private transient int objectSizeAllocFastPath;
    private transient int primitiveType;
    private transient int referenceInstanceOffsets;
    private transient long sFields;
    private transient int status;
    private transient Class<? super T> superClass;
    private transient short virtualMethodsOffset;
    private transient Object vtable;

    @FastNative
    static native Class<?> classForName(String str, boolean z, ClassLoader classLoader) throws ClassNotFoundException;

    @FastNative
    private native Constructor<T> getDeclaredConstructorInternal(Class<?>[] clsArr);

    @FastNative
    private native Constructor<?>[] getDeclaredConstructorsInternal(boolean z);

    @FastNative
    private native Method getDeclaredMethodInternal(String str, Class<?>[] clsArr);

    @FastNative
    private native Constructor<?> getEnclosingConstructorNative();

    @FastNative
    private native Method getEnclosingMethodNative();

    @FastNative
    private native int getInnerClassFlags(int i);

    @FastNative
    private native String getInnerClassName();

    @FastNative
    private native Class<?>[] getInterfacesInternal();

    @FastNative
    private native String getNameNative();

    @FastNative
    static native Class<?> getPrimitiveClass(String str);

    @FastNative
    private native Field[] getPublicDeclaredFields();

    @FastNative
    private native Field getPublicFieldRecursive(String str);

    @FastNative
    private native String[] getSignatureAnnotation();

    @FastNative
    private native boolean isDeclaredAnnotationPresent(Class<? extends Annotation> cls);

    @Override
    @FastNative
    public native <A extends Annotation> A getDeclaredAnnotation(Class<A> cls);

    @Override
    @FastNative
    public native Annotation[] getDeclaredAnnotations();

    @FastNative
    public native Class<?>[] getDeclaredClasses();

    @FastNative
    public native Field getDeclaredField(String str) throws NoSuchFieldException;

    @FastNative
    public native Field[] getDeclaredFields();

    @FastNative
    public native Field[] getDeclaredFieldsUnchecked(boolean z);

    @FastNative
    public native Method[] getDeclaredMethodsUnchecked(boolean z);

    @FastNative
    public native Class<?> getDeclaringClass();

    @FastNative
    public native Class<?> getEnclosingClass();

    @FastNative
    public native boolean isAnonymousClass();

    @FastNative
    public native T newInstance() throws IllegalAccessException, InstantiationException;

    private Class() {
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(isInterface() ? "interface " : isPrimitive() ? "" : "class ");
        sb.append(getName());
        return sb.toString();
    }

    public String toGenericString() {
        if (isPrimitive()) {
            return toString();
        }
        StringBuilder sb = new StringBuilder();
        int modifiers = getModifiers() & Modifier.classModifiers();
        if (modifiers != 0) {
            sb.append(Modifier.toString(modifiers));
            sb.append(' ');
        }
        if (isAnnotation()) {
            sb.append('@');
        }
        if (isInterface()) {
            sb.append("interface");
        } else if (isEnum()) {
            sb.append("enum");
        } else {
            sb.append("class");
        }
        sb.append(' ');
        sb.append(getName());
        TypeVariable<Class<T>>[] typeParameters = getTypeParameters();
        if (typeParameters.length > 0) {
            sb.append('<');
            int length = typeParameters.length;
            boolean z = true;
            int i = 0;
            while (i < length) {
                TypeVariable<Class<T>> typeVariable = typeParameters[i];
                if (!z) {
                    sb.append(',');
                }
                sb.append(typeVariable.getTypeName());
                i++;
                z = false;
            }
            sb.append('>');
        }
        return sb.toString();
    }

    @CallerSensitive
    public static Class<?> forName(String str) throws ClassNotFoundException {
        return forName(str, true, VMStack.getCallingClassLoader());
    }

    @CallerSensitive
    public static Class<?> forName(String str, boolean z, ClassLoader classLoader) throws ClassNotFoundException {
        if (classLoader == null) {
            classLoader = BootClassLoader.getInstance();
        }
        try {
            return classForName(str, z, classLoader);
        } catch (ClassNotFoundException e) {
            Throwable cause = e.getCause();
            if (cause instanceof LinkageError) {
                throw ((LinkageError) cause);
            }
            throw e;
        }
    }

    public boolean isInstance(Object obj) {
        if (obj == null) {
            return false;
        }
        return isAssignableFrom(obj.getClass());
    }

    public boolean isAssignableFrom(Class<?> cls) {
        if (this == cls) {
            return true;
        }
        if (this == Object.class) {
            return !cls.isPrimitive();
        }
        if (isArray()) {
            return cls.isArray() && this.componentType.isAssignableFrom(cls.componentType);
        }
        if (isInterface()) {
            Object[] objArr = cls.ifTable;
            if (objArr != null) {
                for (int i = 0; i < objArr.length; i += 2) {
                    if (objArr[i] == this) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (!cls.isInterface()) {
            do {
                cls = cls.superClass;
                if (cls != null) {
                }
            } while (cls != this);
            return true;
        }
        return false;
    }

    public boolean isInterface() {
        return (this.accessFlags & 512) != 0;
    }

    public boolean isArray() {
        return getComponentType() != null;
    }

    public boolean isPrimitive() {
        return (this.primitiveType & 65535) != 0;
    }

    public boolean isFinalizable() {
        return (getModifiers() & Integer.MIN_VALUE) != 0;
    }

    public boolean isAnnotation() {
        return (getModifiers() & 8192) != 0;
    }

    public boolean isSynthetic() {
        return (getModifiers() & 4096) != 0;
    }

    public String getName() {
        String str = this.name;
        if (str == null) {
            String nameNative = getNameNative();
            this.name = nameNative;
            return nameNative;
        }
        return str;
    }

    public ClassLoader getClassLoader() {
        if (isPrimitive()) {
            return null;
        }
        return this.classLoader == null ? BootClassLoader.getInstance() : this.classLoader;
    }

    @Override
    public synchronized TypeVariable<Class<T>>[] getTypeParameters() {
        String signatureAttribute = getSignatureAttribute();
        if (signatureAttribute == null) {
            return EmptyArray.TYPE_VARIABLE;
        }
        GenericSignatureParser genericSignatureParser = new GenericSignatureParser(getClassLoader());
        genericSignatureParser.parseForClass(this, signatureAttribute);
        return genericSignatureParser.formalTypeParameters;
    }

    public Class<? super T> getSuperclass() {
        if (isInterface()) {
            return null;
        }
        return this.superClass;
    }

    public Type getGenericSuperclass() {
        Type superclass = getSuperclass();
        if (superclass == null) {
            return null;
        }
        String signatureAttribute = getSignatureAttribute();
        if (signatureAttribute != null) {
            GenericSignatureParser genericSignatureParser = new GenericSignatureParser(getClassLoader());
            genericSignatureParser.parseForClass(this, signatureAttribute);
            superclass = genericSignatureParser.superclassType;
        }
        return Types.getType(superclass);
    }

    public Package getPackage() {
        String packageName$;
        ClassLoader classLoader = getClassLoader();
        if (classLoader == null || (packageName$ = getPackageName$()) == null) {
            return null;
        }
        return classLoader.getPackage(packageName$);
    }

    public String getPackageName$() {
        String name = getName();
        int iLastIndexOf = name.lastIndexOf(46);
        if (iLastIndexOf == -1) {
            return null;
        }
        return name.substring(0, iLastIndexOf);
    }

    public Class<?>[] getInterfaces() {
        if (isArray()) {
            return new Class[]{Cloneable.class, Serializable.class};
        }
        Class<?>[] interfacesInternal = getInterfacesInternal();
        if (interfacesInternal == null) {
            return EmptyArray.CLASS;
        }
        return interfacesInternal;
    }

    public Type[] getGenericInterfaces() {
        Type[] typeArray;
        synchronized (Caches.genericInterfaces) {
            typeArray = (Type[]) Caches.genericInterfaces.get(this);
            if (typeArray == null) {
                String signatureAttribute = getSignatureAttribute();
                if (signatureAttribute == null) {
                    typeArray = getInterfaces();
                } else {
                    GenericSignatureParser genericSignatureParser = new GenericSignatureParser(getClassLoader());
                    genericSignatureParser.parseForClass(this, signatureAttribute);
                    typeArray = Types.getTypeArray(genericSignatureParser.interfaceTypes, false);
                }
                Caches.genericInterfaces.put(this, typeArray);
            }
        }
        return typeArray.length == 0 ? typeArray : (Type[]) typeArray.clone();
    }

    public Class<?> getComponentType() {
        return this.componentType;
    }

    public int getModifiers() {
        if (isArray()) {
            int modifiers = getComponentType().getModifiers();
            if ((modifiers & 512) != 0) {
                modifiers &= -521;
            }
            return modifiers | 1040;
        }
        return 65535 & getInnerClassFlags(this.accessFlags & 65535);
    }

    public Object[] getSigners() {
        return null;
    }

    public Method getEnclosingMethod() {
        if (classNameImpliesTopLevel()) {
            return null;
        }
        return getEnclosingMethodNative();
    }

    public Constructor<?> getEnclosingConstructor() {
        if (classNameImpliesTopLevel()) {
            return null;
        }
        return getEnclosingConstructorNative();
    }

    private boolean classNameImpliesTopLevel() {
        return !getName().contains("$");
    }

    public String getSimpleName() {
        if (isArray()) {
            return getComponentType().getSimpleName() + "[]";
        }
        if (isAnonymousClass()) {
            return "";
        }
        if (isMemberClass() || isLocalClass()) {
            return getInnerClassName();
        }
        String name = getName();
        if (name.lastIndexOf(".") > 0) {
            return name.substring(name.lastIndexOf(".") + 1);
        }
        return name;
    }

    @Override
    public String getTypeName() {
        if (isArray()) {
            Class componentType = this;
            int i = 0;
            while (componentType.isArray()) {
                try {
                    i++;
                    componentType = componentType.getComponentType();
                } catch (Throwable th) {
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append(componentType.getName());
            for (int i2 = 0; i2 < i; i2++) {
                sb.append("[]");
            }
            return sb.toString();
        }
        return getName();
    }

    public String getCanonicalName() {
        if (isArray()) {
            String canonicalName = getComponentType().getCanonicalName();
            if (canonicalName == null) {
                return null;
            }
            return canonicalName + "[]";
        }
        if (isLocalOrAnonymousClass()) {
            return null;
        }
        Class<?> enclosingClass = getEnclosingClass();
        if (enclosingClass == null) {
            return getName();
        }
        String canonicalName2 = enclosingClass.getCanonicalName();
        if (canonicalName2 == null) {
            return null;
        }
        return canonicalName2 + "." + getSimpleName();
    }

    public boolean isLocalClass() {
        return ((getEnclosingMethod() == null && getEnclosingConstructor() == null) || isAnonymousClass()) ? false : true;
    }

    public boolean isMemberClass() {
        return getDeclaringClass() != null;
    }

    private boolean isLocalOrAnonymousClass() {
        return isLocalClass() || isAnonymousClass();
    }

    @CallerSensitive
    public Class<?>[] getClasses() {
        ArrayList arrayList = new ArrayList();
        for (Class cls = this; cls != null; cls = cls.superClass) {
            for (Class<?> cls2 : cls.getDeclaredClasses()) {
                if (Modifier.isPublic(cls2.getModifiers())) {
                    arrayList.add(cls2);
                }
            }
        }
        return (Class[]) arrayList.toArray(new Class[arrayList.size()]);
    }

    @CallerSensitive
    public Field[] getFields() throws SecurityException {
        List<Field> arrayList = new ArrayList<>();
        getPublicFieldsRecursive(arrayList);
        return (Field[]) arrayList.toArray(new Field[arrayList.size()]);
    }

    private void getPublicFieldsRecursive(List<Field> list) {
        for (Class cls = this; cls != null; cls = cls.superClass) {
            Collections.addAll(list, cls.getPublicDeclaredFields());
        }
        Object[] objArr = this.ifTable;
        if (objArr != null) {
            for (int i = 0; i < objArr.length; i += 2) {
                Collections.addAll(list, ((Class) objArr[i]).getPublicDeclaredFields());
            }
        }
    }

    @CallerSensitive
    public Method[] getMethods() throws SecurityException {
        List<Method> arrayList = new ArrayList<>();
        getPublicMethodsInternal(arrayList);
        CollectionUtils.removeDuplicates(arrayList, Method.ORDER_BY_SIGNATURE);
        return (Method[]) arrayList.toArray(new Method[arrayList.size()]);
    }

    private void getPublicMethodsInternal(List<Method> list) {
        Collections.addAll(list, getDeclaredMethodsUnchecked(true));
        if (!isInterface()) {
            for (Class<? super T> cls = this.superClass; cls != null; cls = cls.superClass) {
                Collections.addAll(list, cls.getDeclaredMethodsUnchecked(true));
            }
        }
        Object[] objArr = this.ifTable;
        if (objArr != null) {
            for (int i = 0; i < objArr.length; i += 2) {
                Collections.addAll(list, ((Class) objArr[i]).getDeclaredMethodsUnchecked(true));
            }
        }
    }

    @CallerSensitive
    public Constructor<?>[] getConstructors() throws SecurityException {
        return getDeclaredConstructorsInternal(true);
    }

    public Field getField(String str) throws NoSuchFieldException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        Field publicFieldRecursive = getPublicFieldRecursive(str);
        if (publicFieldRecursive == null) {
            throw new NoSuchFieldException(str);
        }
        return publicFieldRecursive;
    }

    @CallerSensitive
    public Method getMethod(String str, Class<?>... clsArr) throws NoSuchMethodException, SecurityException {
        return getMethod(str, clsArr, true);
    }

    public Constructor<T> getConstructor(Class<?>... clsArr) throws NoSuchMethodException, SecurityException {
        return getConstructor0(clsArr, 0);
    }

    public Method[] getDeclaredMethods() throws SecurityException {
        Method[] declaredMethodsUnchecked = getDeclaredMethodsUnchecked(false);
        for (Method method : declaredMethodsUnchecked) {
            method.getReturnType();
            method.getParameterTypes();
        }
        return declaredMethodsUnchecked;
    }

    public Constructor<?>[] getDeclaredConstructors() throws SecurityException {
        return getDeclaredConstructorsInternal(false);
    }

    @CallerSensitive
    public Method getDeclaredMethod(String str, Class<?>... clsArr) throws NoSuchMethodException, SecurityException {
        return getMethod(str, clsArr, false);
    }

    private Method getMethod(String str, Class<?>[] clsArr, boolean z) throws NoSuchMethodException {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        if (clsArr == null) {
            clsArr = EmptyArray.CLASS;
        }
        for (Class<?> cls : clsArr) {
            if (cls == null) {
                throw new NoSuchMethodException("parameter type is null");
            }
        }
        Method publicMethodRecursive = z ? getPublicMethodRecursive(str, clsArr) : getDeclaredMethodInternal(str, clsArr);
        if (publicMethodRecursive == null || (z && !Modifier.isPublic(publicMethodRecursive.getAccessFlags()))) {
            throw new NoSuchMethodException(str + " " + Arrays.toString(clsArr));
        }
        return publicMethodRecursive;
    }

    private Method getPublicMethodRecursive(String str, Class<?>[] clsArr) {
        for (Class<T> superclass = this; superclass != null; superclass = superclass.getSuperclass()) {
            Method declaredMethodInternal = superclass.getDeclaredMethodInternal(str, clsArr);
            if (declaredMethodInternal != null && Modifier.isPublic(declaredMethodInternal.getAccessFlags())) {
                return declaredMethodInternal;
            }
        }
        return findInterfaceMethod(str, clsArr);
    }

    public Method getInstanceMethod(String str, Class<?>[] clsArr) throws IllegalAccessException, NoSuchMethodException {
        for (Class<T> superclass = this; superclass != null; superclass = superclass.getSuperclass()) {
            Method declaredMethodInternal = superclass.getDeclaredMethodInternal(str, clsArr);
            if (declaredMethodInternal != null && !Modifier.isStatic(declaredMethodInternal.getModifiers())) {
                return declaredMethodInternal;
            }
        }
        return findInterfaceMethod(str, clsArr);
    }

    private Method findInterfaceMethod(String str, Class<?>[] clsArr) {
        Object[] objArr = this.ifTable;
        if (objArr != null) {
            for (int length = objArr.length - 2; length >= 0; length -= 2) {
                Method publicMethodRecursive = ((Class) objArr[length]).getPublicMethodRecursive(str, clsArr);
                if (publicMethodRecursive != null && Modifier.isPublic(publicMethodRecursive.getAccessFlags())) {
                    return publicMethodRecursive;
                }
            }
            return null;
        }
        return null;
    }

    @CallerSensitive
    public Constructor<T> getDeclaredConstructor(Class<?>... clsArr) throws NoSuchMethodException, SecurityException {
        return getConstructor0(clsArr, 1);
    }

    public InputStream getResourceAsStream(String str) {
        String strResolveName = resolveName(str);
        ClassLoader classLoader = getClassLoader();
        if (classLoader == null) {
            return ClassLoader.getSystemResourceAsStream(strResolveName);
        }
        return classLoader.getResourceAsStream(strResolveName);
    }

    public URL getResource(String str) {
        String strResolveName = resolveName(str);
        ClassLoader classLoader = getClassLoader();
        if (classLoader == null) {
            return ClassLoader.getSystemResource(strResolveName);
        }
        return classLoader.getResource(strResolveName);
    }

    public ProtectionDomain getProtectionDomain() {
        return null;
    }

    private String resolveName(String str) {
        if (str == null) {
            return str;
        }
        if (!str.startsWith("/")) {
            Class componentType = this;
            while (componentType.isArray()) {
                componentType = componentType.getComponentType();
            }
            String name = componentType.getName();
            int iLastIndexOf = name.lastIndexOf(46);
            if (iLastIndexOf != -1) {
                return name.substring(0, iLastIndexOf).replace('.', '/') + "/" + str;
            }
            return str;
        }
        return str.substring(1);
    }

    private Constructor<T> getConstructor0(Class<?>[] clsArr, int i) throws NoSuchMethodException {
        if (clsArr == null) {
            clsArr = EmptyArray.CLASS;
        }
        for (Class<?> cls : clsArr) {
            if (cls == null) {
                throw new NoSuchMethodException("parameter type is null");
            }
        }
        Constructor<T> declaredConstructorInternal = getDeclaredConstructorInternal(clsArr);
        if (declaredConstructorInternal == null || (i == 0 && !Modifier.isPublic(declaredConstructorInternal.getAccessFlags()))) {
            throw new NoSuchMethodException("<init> " + Arrays.toString(clsArr));
        }
        return declaredConstructorInternal;
    }

    public boolean desiredAssertionStatus() {
        return false;
    }

    public boolean isEnum() {
        return (getModifiers() & 16384) != 0 && getSuperclass() == Enum.class;
    }

    public T[] getEnumConstants() {
        T[] enumConstantsShared = getEnumConstantsShared();
        if (enumConstantsShared != null) {
            return (T[]) ((Object[]) enumConstantsShared.clone());
        }
        return null;
    }

    public T[] getEnumConstantsShared() {
        if (isEnum()) {
            return (T[]) Enum.getSharedConstants(this);
        }
        return null;
    }

    public T cast(Object obj) {
        if (obj != 0 && !isInstance(obj)) {
            throw new ClassCastException(cannotCastMsg(obj));
        }
        return obj;
    }

    private String cannotCastMsg(Object obj) {
        return "Cannot cast " + obj.getClass().getName() + " to " + getName();
    }

    public <U> Class<? extends U> asSubclass(Class<U> cls) {
        if (cls.isAssignableFrom(this)) {
            return this;
        }
        throw new ClassCastException(toString() + " cannot be cast to " + cls.getName());
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> cls) {
        Objects.requireNonNull(cls);
        A a = (A) getDeclaredAnnotation(cls);
        if (a != null) {
            return a;
        }
        if (cls.isDeclaredAnnotationPresent(Inherited.class)) {
            for (Class<? super T> superclass = getSuperclass(); superclass != null; superclass = superclass.getSuperclass()) {
                A a2 = (A) superclass.getDeclaredAnnotation(cls);
                if (a2 != null) {
                    return a2;
                }
            }
            return null;
        }
        return null;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> cls) {
        if (cls == null) {
            throw new NullPointerException("annotationClass == null");
        }
        if (isDeclaredAnnotationPresent(cls)) {
            return true;
        }
        if (cls.isDeclaredAnnotationPresent(Inherited.class)) {
            for (Class<? super T> superclass = getSuperclass(); superclass != null; superclass = superclass.getSuperclass()) {
                if (superclass.isDeclaredAnnotationPresent(cls)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> cls) {
        Class<? super T> superclass;
        A[] aArr = (A[]) super.getAnnotationsByType(cls);
        if (aArr.length != 0) {
            return aArr;
        }
        if (cls.isDeclaredAnnotationPresent(Inherited.class) && (superclass = getSuperclass()) != null) {
            return (A[]) superclass.getAnnotationsByType(cls);
        }
        return (A[]) ((Annotation[]) Array.newInstance((Class<?>) cls, 0));
    }

    @Override
    public Annotation[] getAnnotations() {
        HashMap map = new HashMap();
        for (Annotation annotation : getDeclaredAnnotations()) {
            map.put(annotation.annotationType(), annotation);
        }
        for (Class<? super T> superclass = getSuperclass(); superclass != null; superclass = superclass.getSuperclass()) {
            for (Annotation annotation2 : superclass.getDeclaredAnnotations()) {
                Class<? extends Annotation> clsAnnotationType = annotation2.annotationType();
                if (!map.containsKey(clsAnnotationType) && clsAnnotationType.isDeclaredAnnotationPresent(Inherited.class)) {
                    map.put(clsAnnotationType, annotation2);
                }
            }
        }
        Collection collectionValues = map.values();
        return (Annotation[]) collectionValues.toArray(new Annotation[collectionValues.size()]);
    }

    private String getSignatureAttribute() {
        String[] signatureAnnotation = getSignatureAnnotation();
        if (signatureAnnotation == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String str : signatureAnnotation) {
            sb.append(str);
        }
        return sb.toString();
    }

    public boolean isProxy() {
        return (this.accessFlags & NumericShaper.MONGOLIAN) != 0;
    }

    public int getAccessFlags() {
        return this.accessFlags;
    }

    private static class Caches {
        private static final BasicLruCache<Class, Type[]> genericInterfaces = new BasicLruCache<>(8);

        private Caches() {
        }
    }
}
