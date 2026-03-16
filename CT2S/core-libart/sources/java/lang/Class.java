package java.lang;

import com.android.dex.Dex;
import dalvik.system.VMStack;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ArtField;
import java.lang.reflect.ArtMethod;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import libcore.reflect.AnnotationAccess;
import libcore.reflect.GenericSignatureParser;
import libcore.reflect.InternalNames;
import libcore.reflect.Types;
import libcore.util.BasicLruCache;
import libcore.util.CollectionUtils;
import libcore.util.EmptyArray;
import libcore.util.SneakyThrow;

public final class Class<T> implements Serializable, AnnotatedElement, GenericDeclaration, Type {
    private static final long serialVersionUID = 3206093459760846163L;
    private transient int accessFlags;
    private transient ClassLoader classLoader;
    private transient int classSize;
    private transient int clinitThreadId;
    private transient Class<?> componentType;
    private transient DexCache dexCache;
    private transient String[] dexCacheStrings;
    private transient int dexClassDefIndex;
    private volatile transient int dexTypeIndex;
    private transient ArtMethod[] directMethods;
    private transient ArtField[] iFields;
    private transient Object[] ifTable;
    private transient String name;
    private transient int numReferenceInstanceFields;
    private transient int numReferenceStaticFields;
    private transient int objectSize;
    private transient int primitiveType;
    private transient int referenceInstanceOffsets;
    private transient int referenceStaticOffsets;
    private transient ArtField[] sFields;
    private transient int status;
    private transient Class<? super T> superClass;
    private transient Class<?> verifyErrorClass;
    private transient ArtMethod[] virtualMethods;
    private transient ArtMethod[] vtable;

    static native Class<?> classForName(String str, boolean z, ClassLoader classLoader) throws ClassNotFoundException;

    private native String getNameNative();

    private native Class<?>[] getProxyInterfaces();

    private Class() {
    }

    public static Class<?> forName(String className) throws ClassNotFoundException {
        return forName(className, true, VMStack.getCallingClassLoader());
    }

    public static Class<?> forName(String className, boolean shouldInitialize, ClassLoader classLoader) throws Throwable {
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        try {
            Class<?> result = classForName(className, shouldInitialize, classLoader);
            return result;
        } catch (ClassNotFoundException e) {
            Throwable cause = e.getCause();
            if (cause instanceof LinkageError) {
                throw ((LinkageError) cause);
            }
            throw e;
        }
    }

    public Class<?>[] getClasses() {
        List<Class<?>> result = new ArrayList<>();
        for (Class cls = this; cls != null; cls = cls.superClass) {
            for (Class<?> member : cls.getDeclaredClasses()) {
                if (Modifier.isPublic(member.getModifiers())) {
                    result.add(member);
                }
            }
        }
        return (Class[]) result.toArray(new Class[result.size()]);
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> cls) {
        return (A) AnnotationAccess.getAnnotation((Class<?>) this, (Class) cls);
    }

    @Override
    public Annotation[] getAnnotations() {
        return AnnotationAccess.getAnnotations(this);
    }

    public String getCanonicalName() {
        if (isLocalClass() || isAnonymousClass()) {
            return null;
        }
        if (isArray()) {
            String name = getComponentType().getCanonicalName();
            if (name != null) {
                return name + "[]";
            }
            return null;
        }
        if (isMemberClass()) {
            String name2 = getDeclaringClass().getCanonicalName();
            if (name2 != null) {
                return name2 + "." + getSimpleName();
            }
            return null;
        }
        return getName();
    }

    public ClassLoader getClassLoader() {
        if (isPrimitive()) {
            return null;
        }
        ClassLoader loader = getClassLoaderImpl();
        if (loader == null) {
            return BootClassLoader.getInstance();
        }
        return loader;
    }

    ClassLoader getClassLoaderImpl() {
        ClassLoader loader = this.classLoader;
        return loader == null ? BootClassLoader.getInstance() : loader;
    }

    public Class<?> getComponentType() {
        return this.componentType;
    }

    public Dex getDex() {
        if (this.dexCache == null) {
            return null;
        }
        return this.dexCache.getDex();
    }

    public String getDexCacheString(Dex dex, int dexStringIndex) {
        String s = this.dexCacheStrings[dexStringIndex];
        if (s == null) {
            String s2 = dex.strings().get(dexStringIndex).intern();
            this.dexCacheStrings[dexStringIndex] = s2;
            return s2;
        }
        return s;
    }

    public Class<?> getDexCacheType(Dex dex, int dexTypeIndex) {
        Class<?>[] dexCacheResolvedTypes = this.dexCache.resolvedTypes;
        Class<?> resolvedType = dexCacheResolvedTypes[dexTypeIndex];
        if (resolvedType == null) {
            int descriptorIndex = dex.typeIds().get(dexTypeIndex).intValue();
            String descriptor = getDexCacheString(dex, descriptorIndex);
            Class<?> resolvedType2 = InternalNames.getClass(getClassLoader(), descriptor);
            dexCacheResolvedTypes[dexTypeIndex] = resolvedType2;
            return resolvedType2;
        }
        return resolvedType;
    }

    public Constructor<T> getConstructor(Class<?>... parameterTypes) throws NoSuchMethodException {
        return getConstructor(parameterTypes, true);
    }

    public Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes) throws NoSuchMethodException {
        return getConstructor(parameterTypes, false);
    }

    private Constructor<T> getConstructor(Class<?>[] parameterTypes, boolean publicOnly) throws NoSuchMethodException {
        if (parameterTypes == null) {
            parameterTypes = EmptyArray.CLASS;
        }
        for (Class<?> c : parameterTypes) {
            if (c == null) {
                throw new NoSuchMethodException("parameter type is null");
            }
        }
        Constructor<T> result = getDeclaredConstructorInternal(parameterTypes);
        if (result == null || (publicOnly && !Modifier.isPublic(result.getAccessFlags()))) {
            throw new NoSuchMethodException("<init> " + Arrays.toString(parameterTypes));
        }
        return result;
    }

    private Constructor<T> getDeclaredConstructorInternal(Class<?>[] args) {
        if (this.directMethods != null) {
            ArtMethod[] arr$ = this.directMethods;
            for (ArtMethod m : arr$) {
                int modifiers = m.getAccessFlags();
                if (!Modifier.isStatic(modifiers) && Modifier.isConstructor(modifiers) && ArtMethod.equalConstructorParameters(m, args)) {
                    return new Constructor<>(m);
                }
            }
        }
        return null;
    }

    public Constructor<?>[] getConstructors() {
        ArrayList<Constructor<T>> constructors = new ArrayList<>();
        getDeclaredConstructors(true, constructors);
        return (Constructor[]) constructors.toArray(new Constructor[constructors.size()]);
    }

    public Constructor<?>[] getDeclaredConstructors() {
        ArrayList<Constructor<T>> constructors = new ArrayList<>();
        getDeclaredConstructors(false, constructors);
        return (Constructor[]) constructors.toArray(new Constructor[constructors.size()]);
    }

    private void getDeclaredConstructors(boolean publicOnly, List<Constructor<T>> constructors) {
        if (this.directMethods != null) {
            ArtMethod[] arr$ = this.directMethods;
            for (ArtMethod m : arr$) {
                int modifiers = m.getAccessFlags();
                if ((!publicOnly || Modifier.isPublic(modifiers)) && !Modifier.isStatic(modifiers) && Modifier.isConstructor(modifiers)) {
                    constructors.add(new Constructor<>(m));
                }
            }
        }
    }

    public Method getDeclaredMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return getMethod(name, parameterTypes, false);
    }

    public Method getMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return getMethod(name, parameterTypes, true);
    }

    private Method getMethod(String name, Class<?>[] parameterTypes, boolean recursivePublicMethods) throws NoSuchMethodException {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (parameterTypes == null) {
            parameterTypes = EmptyArray.CLASS;
        }
        for (Class<?> c : parameterTypes) {
            if (c == null) {
                throw new NoSuchMethodException("parameter type is null");
            }
        }
        Method result = recursivePublicMethods ? getPublicMethodRecursive(name, parameterTypes) : getDeclaredMethodInternal(name, parameterTypes);
        if (result == null || (recursivePublicMethods && !Modifier.isPublic(result.getAccessFlags()))) {
            throw new NoSuchMethodException(name + " " + Arrays.toString(parameterTypes));
        }
        return result;
    }

    private Method getPublicMethodRecursive(String name, Class<?>[] parameterTypes) {
        for (Class<T> superclass = this; superclass != null; superclass = superclass.getSuperclass()) {
            Method result = superclass.getDeclaredMethodInternal(name, parameterTypes);
            if (result != null && Modifier.isPublic(result.getAccessFlags())) {
                return result;
            }
        }
        Object[] iftable = this.ifTable;
        if (iftable != null) {
            for (int i = 0; i < iftable.length; i += 2) {
                Class<?> ifc = (Class) iftable[i];
                Method result2 = ifc.getPublicMethodRecursive(name, parameterTypes);
                if (result2 != null && Modifier.isPublic(result2.getAccessFlags())) {
                    return result2;
                }
            }
        }
        return null;
    }

    private Method getDeclaredMethodInternal(String name, Class<?>[] args) {
        ArtMethod artMethodResult = null;
        if (this.virtualMethods != null) {
            ArtMethod[] arr$ = this.virtualMethods;
            for (ArtMethod m : arr$) {
                String methodName = ArtMethod.getMethodName(m);
                if (name.equals(methodName) && ArtMethod.equalMethodParameters(m, args)) {
                    int modifiers = m.getAccessFlags();
                    if ((modifiers & 2101248) == 0) {
                        return new Method(m);
                    }
                    if ((2097152 & modifiers) == 0) {
                        artMethodResult = m;
                    }
                }
            }
        }
        if (artMethodResult == null && this.directMethods != null) {
            ArtMethod[] arr$2 = this.directMethods;
            for (ArtMethod m2 : arr$2) {
                int modifiers2 = m2.getAccessFlags();
                if (!Modifier.isConstructor(modifiers2)) {
                    String methodName2 = ArtMethod.getMethodName(m2);
                    if (name.equals(methodName2) && ArtMethod.equalMethodParameters(m2, args)) {
                        if ((modifiers2 & 2101248) == 0) {
                            return new Method(m2);
                        }
                        artMethodResult = m2;
                    }
                }
            }
        }
        if (artMethodResult == null) {
            return null;
        }
        return new Method(artMethodResult);
    }

    public Method[] getDeclaredMethods() {
        int initial_size = this.virtualMethods == null ? 0 : this.virtualMethods.length;
        ArrayList<Method> methods = new ArrayList<>(initial_size + (this.directMethods == null ? 0 : this.directMethods.length));
        getDeclaredMethodsUnchecked(false, methods);
        Method[] result = (Method[]) methods.toArray(new Method[methods.size()]);
        for (Method m : result) {
            m.getReturnType();
            m.getParameterTypes();
        }
        return result;
    }

    public void getDeclaredMethodsUnchecked(boolean publicOnly, List<Method> methods) {
        if (this.virtualMethods != null) {
            ArtMethod[] arr$ = this.virtualMethods;
            for (ArtMethod m : arr$) {
                int modifiers = m.getAccessFlags();
                if ((!publicOnly || Modifier.isPublic(modifiers)) && (2097152 & modifiers) == 0) {
                    methods.add(new Method(m));
                }
            }
        }
        if (this.directMethods != null) {
            ArtMethod[] arr$2 = this.directMethods;
            for (ArtMethod m2 : arr$2) {
                int modifiers2 = m2.getAccessFlags();
                if ((!publicOnly || Modifier.isPublic(modifiers2)) && !Modifier.isConstructor(modifiers2)) {
                    methods.add(new Method(m2));
                }
            }
        }
    }

    public Method[] getMethods() {
        List<Method> methods = new ArrayList<>();
        getPublicMethodsInternal(methods);
        CollectionUtils.removeDuplicates(methods, Method.ORDER_BY_SIGNATURE);
        return (Method[]) methods.toArray(new Method[methods.size()]);
    }

    private void getPublicMethodsInternal(List<Method> result) {
        getDeclaredMethodsUnchecked(true, result);
        if (!isInterface()) {
            for (Class<?> c = this.superClass; c != null; c = c.superClass) {
                c.getDeclaredMethodsUnchecked(true, result);
            }
        }
        Object[] iftable = this.ifTable;
        if (iftable != null) {
            for (int i = 0; i < iftable.length; i += 2) {
                Class<?> ifc = (Class) iftable[i];
                ifc.getDeclaredMethodsUnchecked(true, result);
            }
        }
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        List<Annotation> result = AnnotationAccess.getDeclaredAnnotations(this);
        return (Annotation[]) result.toArray(new Annotation[result.size()]);
    }

    public Class<?>[] getDeclaredClasses() {
        return AnnotationAccess.getMemberClasses(this);
    }

    public Field getDeclaredField(String name) throws NoSuchFieldException {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        Field result = getDeclaredFieldInternal(name);
        if (result == null) {
            throw new NoSuchFieldException(name);
        }
        result.getType();
        return result;
    }

    public Field[] getDeclaredFields() {
        int initial_size = this.sFields == null ? 0 : this.sFields.length;
        ArrayList<Field> fields = new ArrayList<>(initial_size + (this.iFields == null ? 0 : this.iFields.length));
        getDeclaredFieldsUnchecked(false, fields);
        Field[] result = (Field[]) fields.toArray(new Field[fields.size()]);
        for (Field f : result) {
            f.getType();
        }
        return result;
    }

    public void getDeclaredFieldsUnchecked(boolean publicOnly, List<Field> fields) {
        if (this.iFields != null) {
            ArtField[] arr$ = this.iFields;
            for (ArtField f : arr$) {
                if (!publicOnly || Modifier.isPublic(f.getAccessFlags())) {
                    fields.add(new Field(f));
                }
            }
        }
        if (this.sFields != null) {
            ArtField[] arr$2 = this.sFields;
            for (ArtField f2 : arr$2) {
                if (!publicOnly || Modifier.isPublic(f2.getAccessFlags())) {
                    fields.add(new Field(f2));
                }
            }
        }
    }

    private Field getDeclaredFieldInternal(String name) {
        ArtField matched;
        ArtField matched2;
        if (this.iFields != null && (matched2 = findByName(name, this.iFields)) != null) {
            return new Field(matched2);
        }
        if (this.sFields != null && (matched = findByName(name, this.sFields)) != null) {
            return new Field(matched);
        }
        return null;
    }

    private static ArtField findByName(String name, ArtField[] fields) {
        int low = 0;
        int high = fields.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            ArtField f = fields[mid];
            int result = f.getName().compareTo(name);
            if (result < 0) {
                low = mid + 1;
            } else if (result != 0) {
                high = mid - 1;
            } else {
                return f;
            }
        }
        return null;
    }

    public Class<?> getDeclaringClass() {
        if (AnnotationAccess.isAnonymousClass(this)) {
            return null;
        }
        return AnnotationAccess.getEnclosingClass(this);
    }

    public Class<?> getEnclosingClass() {
        Class<?> declaringClass = getDeclaringClass();
        if (declaringClass == null) {
            Object enclosingMethodOrConstructor = AnnotationAccess.getEnclosingMethodOrConstructor(this);
            if (enclosingMethodOrConstructor != null) {
                return ((Member) enclosingMethodOrConstructor).getDeclaringClass();
            }
            return AnnotationAccess.getEnclosingClass(this);
        }
        return declaringClass;
    }

    public Constructor<?> getEnclosingConstructor() {
        if (classNameImpliesTopLevel()) {
            return null;
        }
        AccessibleObject result = AnnotationAccess.getEnclosingMethodOrConstructor(this);
        return result instanceof Constructor ? (Constructor) result : null;
    }

    public Method getEnclosingMethod() {
        if (classNameImpliesTopLevel()) {
            return null;
        }
        AccessibleObject result = AnnotationAccess.getEnclosingMethodOrConstructor(this);
        return result instanceof Method ? (Method) result : null;
    }

    private boolean classNameImpliesTopLevel() {
        return !getName().contains("$");
    }

    public T[] getEnumConstants() {
        if (isEnum()) {
            return (T[]) ((Object[]) Enum.getSharedConstants(this).clone());
        }
        return null;
    }

    public Field getField(String name) throws NoSuchFieldException {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        Field result = getPublicFieldRecursive(name);
        if (result == null) {
            throw new NoSuchFieldException(name);
        }
        result.getType();
        return result;
    }

    private Field getPublicFieldRecursive(String name) {
        for (Class cls = this; cls != null; cls = cls.superClass) {
            Field result = cls.getDeclaredFieldInternal(name);
            if (result != null && (result.getModifiers() & 1) != 0) {
                return result;
            }
        }
        if (this.ifTable != null) {
            for (int i = 0; i < this.ifTable.length; i += 2) {
                Class<?> ifc = (Class) this.ifTable[i];
                Field result2 = ifc.getPublicFieldRecursive(name);
                if (result2 != null && (result2.getModifiers() & 1) != 0) {
                    return result2;
                }
            }
        }
        return null;
    }

    public Field[] getFields() {
        List<Field> fields = new ArrayList<>();
        getPublicFieldsRecursive(fields);
        Field[] result = (Field[]) fields.toArray(new Field[fields.size()]);
        for (Field f : result) {
            f.getType();
        }
        return result;
    }

    private void getPublicFieldsRecursive(List<Field> result) {
        for (Class cls = this; cls != null; cls = cls.superClass) {
            cls.getDeclaredFieldsUnchecked(true, result);
        }
        Object[] iftable = this.ifTable;
        if (iftable != null) {
            for (int i = 0; i < iftable.length; i += 2) {
                Class<?> ifc = (Class) iftable[i];
                ifc.getDeclaredFieldsUnchecked(true, result);
            }
        }
    }

    public Type[] getGenericInterfaces() {
        Type[] result;
        synchronized (Caches.genericInterfaces) {
            result = (Type[]) Caches.genericInterfaces.get(this);
            if (result == null) {
                String annotationSignature = AnnotationAccess.getSignature(this);
                if (annotationSignature == null) {
                    result = getInterfaces();
                } else {
                    GenericSignatureParser parser = new GenericSignatureParser(getClassLoader());
                    parser.parseForClass(this, annotationSignature);
                    result = Types.getTypeArray(parser.interfaceTypes, false);
                }
                Caches.genericInterfaces.put(this, result);
            }
        }
        return result.length == 0 ? result : (Type[]) result.clone();
    }

    public Type getGenericSuperclass() {
        Type genericSuperclass = getSuperclass();
        if (genericSuperclass == null) {
            return null;
        }
        String annotationSignature = AnnotationAccess.getSignature(this);
        if (annotationSignature != null) {
            GenericSignatureParser parser = new GenericSignatureParser(getClassLoader());
            parser.parseForClass(this, annotationSignature);
            genericSuperclass = parser.superclassType;
        }
        return Types.getType(genericSuperclass);
    }

    public Class<?>[] getInterfaces() {
        if (isArray()) {
            return new Class[]{Cloneable.class, Serializable.class};
        }
        if (isProxy()) {
            return getProxyInterfaces();
        }
        Dex dex = getDex();
        if (dex == null) {
            return EmptyArray.CLASS;
        }
        short[] interfaces = dex.interfaceTypeIndicesFromClassDefIndex(this.dexClassDefIndex);
        Class<?>[] result = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            result[i] = getDexCacheType(dex, interfaces[i]);
        }
        return result;
    }

    public int getModifiers() {
        if (isArray()) {
            int componentModifiers = getComponentType().getModifiers();
            if ((componentModifiers & 512) != 0) {
                componentModifiers &= -521;
            }
            return componentModifiers | 1040;
        }
        int modifiers = AnnotationAccess.getInnerClassFlags(this, this.accessFlags & 65535);
        return modifiers & 65535;
    }

    public String getName() {
        String result = this.name;
        if (result != null) {
            return result;
        }
        String result2 = getNameNative();
        this.name = result2;
        return result2;
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
        int dot = name.lastIndexOf(46);
        if (dot != -1) {
            return name.substring(dot + 1);
        }
        return name;
    }

    private String getInnerClassName() {
        return AnnotationAccess.getInnerClassName(this);
    }

    public ProtectionDomain getProtectionDomain() {
        return null;
    }

    public URL getResource(String resourceName) {
        String pkg;
        String resourceName2;
        if (resourceName.startsWith("/")) {
            resourceName2 = resourceName.substring(1);
        } else {
            String pkg2 = getName();
            int dot = pkg2.lastIndexOf(46);
            if (dot != -1) {
                pkg = pkg2.substring(0, dot).replace('.', '/');
            } else {
                pkg = "";
            }
            resourceName2 = pkg + "/" + resourceName;
        }
        ClassLoader loader = getClassLoader();
        if (loader != null) {
            return loader.getResource(resourceName2);
        }
        return ClassLoader.getSystemResource(resourceName2);
    }

    public InputStream getResourceAsStream(String resourceName) {
        String pkg;
        String resourceName2;
        if (resourceName.startsWith("/")) {
            resourceName2 = resourceName.substring(1);
        } else {
            String pkg2 = getName();
            int dot = pkg2.lastIndexOf(46);
            if (dot != -1) {
                pkg = pkg2.substring(0, dot).replace('.', '/');
            } else {
                pkg = "";
            }
            resourceName2 = pkg + "/" + resourceName;
        }
        ClassLoader loader = getClassLoader();
        if (loader != null) {
            return loader.getResourceAsStream(resourceName2);
        }
        return ClassLoader.getSystemResourceAsStream(resourceName2);
    }

    public Object[] getSigners() {
        return null;
    }

    public Class<? super T> getSuperclass() {
        if (isInterface()) {
            return null;
        }
        return this.superClass;
    }

    @Override
    public synchronized TypeVariable<Class<T>>[] getTypeParameters() {
        TypeVariable<Class<T>>[] typeVariableArr;
        String annotationSignature = AnnotationAccess.getSignature(this);
        if (annotationSignature == null) {
            typeVariableArr = EmptyArray.TYPE_VARIABLE;
        } else {
            GenericSignatureParser parser = new GenericSignatureParser(getClassLoader());
            parser.parseForClass(this, annotationSignature);
            typeVariableArr = parser.formalTypeParameters;
        }
        return typeVariableArr;
    }

    public boolean isAnnotation() {
        return (this.accessFlags & 8192) != 0;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return AnnotationAccess.isAnnotationPresent(this, annotationType);
    }

    public boolean isAnonymousClass() {
        return AnnotationAccess.isAnonymousClass(this);
    }

    public boolean isArray() {
        return getComponentType() != null;
    }

    public boolean isProxy() {
        return (this.accessFlags & 262144) != 0;
    }

    public boolean isAssignableFrom(Class<?> c) {
        if (this == c) {
            return true;
        }
        if (this == Object.class) {
            return !c.isPrimitive();
        }
        if (isArray()) {
            return c.isArray() && this.componentType.isAssignableFrom(c.componentType);
        }
        if (isInterface()) {
            Object[] iftable = c.ifTable;
            if (iftable != null) {
                for (int i = 0; i < iftable.length; i += 2) {
                    if (((Class) iftable[i]) == this) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (!c.isInterface()) {
            for (Class<?> c2 = c.superClass; c2 != null; c2 = c2.superClass) {
                if (c2 == this) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEnum() {
        return getSuperclass() == Enum.class && (this.accessFlags & 16384) != 0;
    }

    public boolean isInstance(Object object) {
        if (object == null) {
            return false;
        }
        return isAssignableFrom(object.getClass());
    }

    public boolean isInterface() {
        return (this.accessFlags & 512) != 0;
    }

    public boolean isLocalClass() {
        return (classNameImpliesTopLevel() || AnnotationAccess.getEnclosingMethodOrConstructor(this) == null || isAnonymousClass()) ? false : true;
    }

    public boolean isMemberClass() {
        return getDeclaringClass() != null;
    }

    public boolean isPrimitive() {
        return this.primitiveType != 0;
    }

    public boolean isSynthetic() {
        return (this.accessFlags & 4096) != 0;
    }

    public boolean isFinalizable() {
        return (this.accessFlags & Integer.MIN_VALUE) != 0;
    }

    public T newInstance() throws IllegalAccessException, InstantiationException {
        if (isPrimitive() || isInterface() || isArray() || Modifier.isAbstract(this.accessFlags)) {
            throw new InstantiationException(this + " cannot be instantiated");
        }
        Class<?> caller = VMStack.getStackClass1();
        if (!caller.canAccess(this)) {
            throw new IllegalAccessException(this + " is not accessible from " + caller);
        }
        try {
            Constructor<T> init = getDeclaredConstructor(new Class[0]);
            if (!caller.canAccessMember(this, init.getAccessFlags())) {
                throw new IllegalAccessException(init + " is not accessible from " + caller);
            }
            try {
                return init.newInstance(null, init.isAccessible());
            } catch (InvocationTargetException e) {
                SneakyThrow.sneakyThrow(e.getCause());
                return null;
            }
        } catch (NoSuchMethodException e2) {
            InstantiationException t = new InstantiationException(this + " has no zero argument constructor");
            t.initCause(e2);
            throw t;
        }
    }

    private boolean canAccess(Class<?> c) {
        if (Modifier.isPublic(c.accessFlags)) {
            return true;
        }
        return inSamePackage(c);
    }

    private boolean canAccessMember(Class<?> memberClass, int memberModifiers) {
        if (memberClass == this || Modifier.isPublic(memberModifiers)) {
            return true;
        }
        if (Modifier.isPrivate(memberModifiers)) {
            return false;
        }
        if (Modifier.isProtected(memberModifiers)) {
            for (Class<?> parent = this.superClass; parent != null; parent = parent.superClass) {
                if (parent == memberClass) {
                    return true;
                }
            }
        }
        return inSamePackage(memberClass);
    }

    private boolean inSamePackage(Class<?> c) {
        if (this.classLoader != c.classLoader) {
            return false;
        }
        String packageName1 = getPackageName$();
        String packageName2 = c.getPackageName$();
        if (packageName1 == null) {
            return packageName2 == null;
        }
        if (packageName2 != null) {
            return packageName1.equals(packageName2);
        }
        return false;
    }

    public String toString() {
        if (isPrimitive()) {
            return getSimpleName();
        }
        return (isInterface() ? "interface " : "class ") + getName();
    }

    public Package getPackage() {
        String packageName;
        ClassLoader loader = getClassLoader();
        if (loader == null || (packageName = getPackageName$()) == null) {
            return null;
        }
        return loader.getPackage(packageName);
    }

    public String getPackageName$() {
        String name = getName();
        int last = name.lastIndexOf(46);
        if (last == -1) {
            return null;
        }
        return name.substring(0, last);
    }

    public boolean desiredAssertionStatus() {
        return false;
    }

    public <U> Class<? extends U> asSubclass(Class<U> c) {
        if (c.isAssignableFrom(this)) {
            return this;
        }
        String actualClassName = getName();
        String desiredClassName = c.getName();
        throw new ClassCastException(actualClassName + " cannot be cast to " + desiredClassName);
    }

    public T cast(Object obj) {
        if (obj == 0) {
            return null;
        }
        if (isInstance(obj)) {
            return obj;
        }
        String actualClassName = obj.getClass().getName();
        String desiredClassName = getName();
        throw new ClassCastException(actualClassName + " cannot be cast to " + desiredClassName);
    }

    public int getDexClassDefIndex() {
        if (this.dexClassDefIndex == 65535) {
            return -1;
        }
        return this.dexClassDefIndex;
    }

    public int getDexTypeIndex() {
        int typeIndex;
        int typeIndex2 = this.dexTypeIndex;
        if (typeIndex2 != 65535) {
            return typeIndex2;
        }
        synchronized (this) {
            typeIndex = this.dexTypeIndex;
            if (typeIndex == 65535) {
                if (this.dexClassDefIndex >= 0) {
                    typeIndex = getDex().typeIndexFromClassDefIndex(this.dexClassDefIndex);
                } else {
                    typeIndex = getDex().findTypeIndex(InternalNames.getInternalName(this));
                    if (typeIndex < 0) {
                        typeIndex = -1;
                    }
                }
                this.dexTypeIndex = typeIndex;
            }
        }
        return typeIndex;
    }

    public int getDexAnnotationDirectoryOffset() {
        int classDefIndex;
        Dex dex = getDex();
        if (dex != null && (classDefIndex = getDexClassDefIndex()) >= 0) {
            return dex.annotationDirectoryOffsetFromClassDefIndex(classDefIndex);
        }
        return 0;
    }

    private static class Caches {
        private static final BasicLruCache<Class, Type[]> genericInterfaces = new BasicLruCache<>(8);

        private Caches() {
        }
    }
}
