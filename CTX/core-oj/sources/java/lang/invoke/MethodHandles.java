package java.lang.invoke;

import dalvik.system.VMStack;
import java.lang.invoke.Transformers;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import sun.invoke.util.VerifyAccess;
import sun.invoke.util.Wrapper;

public class MethodHandles {
    private MethodHandles() {
    }

    public static Lookup lookup() {
        return new Lookup(VMStack.getStackClass1());
    }

    public static Lookup publicLookup() {
        return Lookup.PUBLIC_LOOKUP;
    }

    public static <T extends Member> T reflectAs(Class<T> cls, MethodHandle methodHandle) {
        return cls.cast(getMethodHandleImpl(methodHandle).getMemberInternal());
    }

    public static final class Lookup {
        static final boolean $assertionsDisabled = false;
        private static final int ALL_MODES = 15;
        public static final int PACKAGE = 8;
        public static final int PRIVATE = 2;
        public static final int PROTECTED = 4;
        public static final int PUBLIC = 1;
        private final int allowedModes;
        private final Class<?> lookupClass;
        static final Lookup PUBLIC_LOOKUP = new Lookup(Object.class, 1);
        static final Lookup IMPL_LOOKUP = new Lookup(Object.class, 15);

        private static int fixmods(int i) {
            int i2 = i & 7;
            if (i2 != 0) {
                return i2;
            }
            return 8;
        }

        public Class<?> lookupClass() {
            return this.lookupClass;
        }

        public int lookupModes() {
            return this.allowedModes & 15;
        }

        Lookup(Class<?> cls) {
            this(cls, 15);
            checkUnprivilegedlookupClass(cls, 15);
        }

        private Lookup(Class<?> cls, int i) {
            this.lookupClass = cls;
            this.allowedModes = i;
        }

        public Lookup in(Class<?> cls) {
            cls.getClass();
            if (cls == this.lookupClass) {
                return this;
            }
            int i = this.allowedModes & 11;
            if ((i & 8) != 0 && !VerifyAccess.isSamePackage(this.lookupClass, cls)) {
                i &= -11;
            }
            if ((i & 2) != 0 && !VerifyAccess.isSamePackageMember(this.lookupClass, cls)) {
                i &= -3;
            }
            if ((i & 1) != 0 && !VerifyAccess.isClassAccessible(cls, this.lookupClass, this.allowedModes)) {
                i = 0;
            }
            checkUnprivilegedlookupClass(cls, i);
            return new Lookup(cls, i);
        }

        private static void checkUnprivilegedlookupClass(Class<?> cls, int i) {
            String name = cls.getName();
            if (name.startsWith("java.lang.invoke.")) {
                throw MethodHandleStatics.newIllegalArgumentException("illegal lookupClass: " + ((Object) cls));
            }
            if (i == 15 && cls.getClassLoader() == Object.class.getClassLoader()) {
                if (name.startsWith("java.") || (name.startsWith("sun.") && !name.startsWith("sun.invoke.") && !name.equals("sun.reflect.ReflectionFactory"))) {
                    throw MethodHandleStatics.newIllegalArgumentException("illegal lookupClass: " + ((Object) cls));
                }
            }
        }

        public String toString() {
            String name = this.lookupClass.getName();
            int i = this.allowedModes;
            if (i == 9) {
                return name + "/package";
            }
            if (i == 11) {
                return name + "/private";
            }
            if (i != 15) {
                switch (i) {
                    case 0:
                        return name + "/noaccess";
                    case 1:
                        return name + "/public";
                    default:
                        return name + "/" + Integer.toHexString(this.allowedModes);
                }
            }
            return name;
        }

        public MethodHandle findStatic(Class<?> cls, String str, MethodType methodType) throws IllegalAccessException, NoSuchMethodException {
            Method declaredMethod = cls.getDeclaredMethod(str, methodType.ptypes());
            int modifiers = declaredMethod.getModifiers();
            if (!Modifier.isStatic(modifiers)) {
                throw new IllegalAccessException("Method" + ((Object) declaredMethod) + " is not static");
            }
            checkReturnType(declaredMethod, methodType);
            checkAccess(cls, declaredMethod.getDeclaringClass(), modifiers, declaredMethod.getName());
            return createMethodHandle(declaredMethod, 3, methodType);
        }

        private MethodHandle findVirtualForMH(String str, MethodType methodType) {
            if ("invoke".equals(str)) {
                return MethodHandles.invoker(methodType);
            }
            if ("invokeExact".equals(str)) {
                return MethodHandles.exactInvoker(methodType);
            }
            return null;
        }

        private MethodHandle findVirtualForVH(String str, MethodType methodType) {
            try {
                return MethodHandles.varHandleInvoker(VarHandle.AccessMode.valueFromMethodName(str), methodType);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private static MethodHandle createMethodHandle(Method method, int i, MethodType methodType) {
            MethodHandleImpl methodHandleImpl = new MethodHandleImpl(method.getArtMethod(), i, methodType);
            if (method.isVarArgs()) {
                return new Transformers.VarargsCollector(methodHandleImpl);
            }
            return methodHandleImpl;
        }

        public MethodHandle findVirtual(Class<?> cls, String str, MethodType methodType) throws IllegalAccessException, NoSuchMethodException {
            MethodHandle methodHandleFindVirtualForVH;
            if (cls == MethodHandle.class) {
                MethodHandle methodHandleFindVirtualForMH = findVirtualForMH(str, methodType);
                if (methodHandleFindVirtualForMH != null) {
                    return methodHandleFindVirtualForMH;
                }
            } else if (cls == VarHandle.class && (methodHandleFindVirtualForVH = findVirtualForVH(str, methodType)) != null) {
                return methodHandleFindVirtualForVH;
            }
            Method instanceMethod = cls.getInstanceMethod(str, methodType.ptypes());
            if (instanceMethod == null) {
                try {
                    Method declaredMethod = cls.getDeclaredMethod(str, methodType.ptypes());
                    if (Modifier.isStatic(declaredMethod.getModifiers())) {
                        throw new IllegalAccessException("Method" + ((Object) declaredMethod) + " is static");
                    }
                } catch (NoSuchMethodException e) {
                }
                throw new NoSuchMethodException(str + " " + Arrays.toString(methodType.ptypes()));
            }
            checkReturnType(instanceMethod, methodType);
            checkAccess(cls, instanceMethod.getDeclaringClass(), instanceMethod.getModifiers(), instanceMethod.getName());
            return createMethodHandle(instanceMethod, 0, methodType.insertParameterTypes(0, cls));
        }

        public MethodHandle findConstructor(Class<?> cls, MethodType methodType) throws IllegalAccessException, NoSuchMethodException {
            if (cls.isArray()) {
                throw new NoSuchMethodException("no constructor for array class: " + cls.getName());
            }
            Constructor<?> declaredConstructor = cls.getDeclaredConstructor(methodType.ptypes());
            if (declaredConstructor == null) {
                throw new NoSuchMethodException("No constructor for " + ((Object) declaredConstructor.getDeclaringClass()) + " matching " + ((Object) methodType));
            }
            checkAccess(cls, declaredConstructor.getDeclaringClass(), declaredConstructor.getModifiers(), declaredConstructor.getName());
            return createMethodHandleForConstructor(declaredConstructor);
        }

        private MethodHandle createMethodHandleForConstructor(Constructor constructor) {
            MethodHandle construct;
            Class declaringClass = constructor.getDeclaringClass();
            MethodType methodType = MethodType.methodType((Class<?>) declaringClass, constructor.getParameterTypes());
            if (declaringClass == String.class) {
                construct = new MethodHandleImpl(constructor.getArtMethod(), 2, methodType);
            } else {
                construct = new Transformers.Construct(new MethodHandleImpl(constructor.getArtMethod(), 2, initMethodType(methodType)), methodType);
            }
            if (constructor.isVarArgs()) {
                return new Transformers.VarargsCollector(construct);
            }
            return construct;
        }

        private static MethodType initMethodType(MethodType methodType) {
            Class[] clsArr = new Class[methodType.ptypes().length + 1];
            clsArr[0] = methodType.rtype();
            System.arraycopy(methodType.ptypes(), 0, clsArr, 1, methodType.ptypes().length);
            return MethodType.methodType(Void.TYPE, (Class<?>[]) clsArr);
        }

        public MethodHandle findSpecial(Class<?> cls, String str, MethodType methodType, Class<?> cls2) throws IllegalAccessException, NoSuchMethodException {
            if (cls2 == null) {
                throw new NullPointerException("specialCaller == null");
            }
            if (methodType == null) {
                throw new NullPointerException("type == null");
            }
            if (str == null) {
                throw new NullPointerException("name == null");
            }
            if (cls == null) {
                throw new NullPointerException("ref == null");
            }
            checkSpecialCaller(cls2);
            if (str.startsWith("<")) {
                throw new NoSuchMethodException(str + " is not a valid method name.");
            }
            Method declaredMethod = cls.getDeclaredMethod(str, methodType.ptypes());
            checkReturnType(declaredMethod, methodType);
            return findSpecial(declaredMethod, methodType, cls, cls2);
        }

        private MethodHandle findSpecial(Method method, MethodType methodType, Class<?> cls, Class<?> cls2) throws IllegalAccessException {
            if (Modifier.isStatic(method.getModifiers())) {
                throw new IllegalAccessException("expected a non-static method:" + ((Object) method));
            }
            if (Modifier.isPrivate(method.getModifiers())) {
                if (cls != lookupClass()) {
                    throw new IllegalAccessException("no private access for invokespecial : " + ((Object) cls) + ", from" + ((Object) this));
                }
                return createMethodHandle(method, 2, methodType.insertParameterTypes(0, cls));
            }
            if (!method.getDeclaringClass().isAssignableFrom(cls2)) {
                throw new IllegalAccessException(((Object) cls) + "is not assignable from " + ((Object) cls2));
            }
            return createMethodHandle(method, 1, methodType.insertParameterTypes(0, cls2));
        }

        public MethodHandle findGetter(Class<?> cls, String str, Class<?> cls2) throws IllegalAccessException, NoSuchFieldException {
            return findAccessor(cls, str, cls2, 9);
        }

        private MethodHandle findAccessor(Class<?> cls, String str, Class<?> cls2, int i) throws IllegalAccessException, NoSuchFieldException {
            return findAccessor(findFieldOfType(cls, str, cls2), cls, cls2, i, true);
        }

        private MethodHandle findAccessor(Field field, Class<?> cls, Class<?> cls2, int i, boolean z) throws IllegalAccessException {
            MethodType methodType;
            boolean z2 = i == 10 || i == 12;
            commonFieldChecks(field, cls, cls2, i == 11 || i == 12, z);
            if (z) {
                int modifiers = field.getModifiers();
                if (z2 && Modifier.isFinal(modifiers)) {
                    throw new IllegalAccessException("Field " + ((Object) field) + " is final");
                }
            }
            switch (i) {
                case 9:
                    methodType = MethodType.methodType(cls2, cls);
                    break;
                case 10:
                    methodType = MethodType.methodType(Void.TYPE, cls, cls2);
                    break;
                case 11:
                    methodType = MethodType.methodType(cls2);
                    break;
                case 12:
                    methodType = MethodType.methodType(Void.TYPE, cls2);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid kind " + i);
            }
            return new MethodHandleImpl(field.getArtField(), i, methodType);
        }

        public MethodHandle findSetter(Class<?> cls, String str, Class<?> cls2) throws IllegalAccessException, NoSuchFieldException {
            return findAccessor(cls, str, cls2, 10);
        }

        public VarHandle findVarHandle(Class<?> cls, String str, Class<?> cls2) throws IllegalAccessException, NoSuchFieldException {
            Field fieldFindFieldOfType = findFieldOfType(cls, str, cls2);
            commonFieldChecks(fieldFindFieldOfType, cls, cls2, $assertionsDisabled, true);
            return FieldVarHandle.create(fieldFindFieldOfType);
        }

        private Field findFieldOfType(Class<?> cls, String str, Class<?> cls2) throws NoSuchFieldException {
            Field declaredField;
            Class<?> superclass = cls;
            while (true) {
                if (superclass != null) {
                    try {
                        declaredField = superclass.getDeclaredField(str);
                        break;
                    } catch (NoSuchFieldException e) {
                        superclass = superclass.getSuperclass();
                    }
                } else {
                    declaredField = null;
                    break;
                }
            }
            if (declaredField == null) {
                declaredField = cls.getDeclaredField(str);
            }
            if (declaredField.getType() != cls2) {
                throw new NoSuchFieldException(str);
            }
            return declaredField;
        }

        private void commonFieldChecks(Field field, Class<?> cls, Class<?> cls2, boolean z, boolean z2) throws IllegalAccessException {
            int modifiers = field.getModifiers();
            if (z2) {
                checkAccess(cls, field.getDeclaringClass(), modifiers, field.getName());
            }
            if (Modifier.isStatic(modifiers) != z) {
                StringBuilder sb = new StringBuilder();
                sb.append("Field ");
                sb.append((Object) field);
                sb.append(" is ");
                sb.append(z ? "not " : "");
                sb.append("static");
                throw new IllegalAccessException(sb.toString());
            }
        }

        public MethodHandle findStaticGetter(Class<?> cls, String str, Class<?> cls2) throws IllegalAccessException, NoSuchFieldException {
            return findAccessor(cls, str, cls2, 11);
        }

        public MethodHandle findStaticSetter(Class<?> cls, String str, Class<?> cls2) throws IllegalAccessException, NoSuchFieldException {
            return findAccessor(cls, str, cls2, 12);
        }

        public VarHandle findStaticVarHandle(Class<?> cls, String str, Class<?> cls2) throws IllegalAccessException, NoSuchFieldException {
            Field fieldFindFieldOfType = findFieldOfType(cls, str, cls2);
            commonFieldChecks(fieldFindFieldOfType, cls, cls2, true, true);
            return FieldVarHandle.create(fieldFindFieldOfType);
        }

        public MethodHandle bind(Object obj, String str, MethodType methodType) throws IllegalAccessException, NoSuchMethodException {
            MethodHandle methodHandleFindVirtual = findVirtual(obj.getClass(), str, methodType);
            MethodHandle methodHandleBindTo = methodHandleFindVirtual.bindTo(obj);
            MethodType methodTypeType = methodHandleBindTo.type();
            if (methodHandleFindVirtual.isVarargsCollector()) {
                return methodHandleBindTo.asVarargsCollector(methodTypeType.parameterType(methodTypeType.parameterCount() - 1));
            }
            return methodHandleBindTo;
        }

        public MethodHandle unreflect(Method method) throws IllegalAccessException {
            if (method == null) {
                throw new NullPointerException("m == null");
            }
            MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
            if (!method.isAccessible()) {
                checkAccess(method.getDeclaringClass(), method.getDeclaringClass(), method.getModifiers(), method.getName());
            }
            if (Modifier.isStatic(method.getModifiers())) {
                return createMethodHandle(method, 3, methodType);
            }
            return createMethodHandle(method, 0, methodType.insertParameterTypes(0, method.getDeclaringClass()));
        }

        public MethodHandle unreflectSpecial(Method method, Class<?> cls) throws IllegalAccessException {
            if (method == null) {
                throw new NullPointerException("m == null");
            }
            if (cls == null) {
                throw new NullPointerException("specialCaller == null");
            }
            if (!method.isAccessible()) {
                checkSpecialCaller(cls);
            }
            return findSpecial(method, MethodType.methodType(method.getReturnType(), method.getParameterTypes()), method.getDeclaringClass(), cls);
        }

        public MethodHandle unreflectConstructor(Constructor<?> constructor) throws IllegalAccessException {
            if (constructor == null) {
                throw new NullPointerException("c == null");
            }
            if (!constructor.isAccessible()) {
                checkAccess(constructor.getDeclaringClass(), constructor.getDeclaringClass(), constructor.getModifiers(), constructor.getName());
            }
            return createMethodHandleForConstructor(constructor);
        }

        public MethodHandle unreflectGetter(Field field) throws IllegalAccessException {
            return findAccessor(field, field.getDeclaringClass(), field.getType(), Modifier.isStatic(field.getModifiers()) ? 11 : 9, !field.isAccessible());
        }

        public MethodHandle unreflectSetter(Field field) throws IllegalAccessException {
            return findAccessor(field, field.getDeclaringClass(), field.getType(), Modifier.isStatic(field.getModifiers()) ? 12 : 10, !field.isAccessible());
        }

        public VarHandle unreflectVarHandle(Field field) throws IllegalAccessException {
            commonFieldChecks(field, field.getDeclaringClass(), field.getType(), Modifier.isStatic(field.getModifiers()), true);
            return FieldVarHandle.create(field);
        }

        public MethodHandleInfo revealDirect(MethodHandle methodHandle) {
            MethodHandleInfo methodHandleInfoReveal = MethodHandles.getMethodHandleImpl(methodHandle).reveal();
            try {
                checkAccess(lookupClass(), methodHandleInfoReveal.getDeclaringClass(), methodHandleInfoReveal.getModifiers(), methodHandleInfoReveal.getName());
                return methodHandleInfoReveal;
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Unable to access memeber.", e);
            }
        }

        private boolean hasPrivateAccess() {
            if ((this.allowedModes & 2) != 0) {
                return true;
            }
            return $assertionsDisabled;
        }

        void checkAccess(Class<?> cls, Class<?> cls2, int i, String str) throws IllegalAccessException {
            int i2 = this.allowedModes;
            if (Modifier.isProtected(i) && cls2 == Object.class && "clone".equals(str) && cls.isArray()) {
                i ^= 5;
            }
            if (Modifier.isProtected(i) && Modifier.isConstructor(i)) {
                i ^= 4;
            }
            if (Modifier.isPublic(i) && Modifier.isPublic(cls.getModifiers()) && i2 != 0) {
                return;
            }
            int iFixmods = fixmods(i);
            if ((iFixmods & i2) != 0) {
                if (VerifyAccess.isMemberAccessible(cls, cls2, i, lookupClass(), i2)) {
                    return;
                }
            } else if ((iFixmods & 4) != 0 && (i2 & 8) != 0 && VerifyAccess.isSamePackage(cls2, lookupClass())) {
                return;
            }
            throwMakeAccessException(accessFailedMessage(cls, cls2, i), this);
        }

        String accessFailedMessage(Class<?> cls, Class<?> cls2, int i) {
            boolean z = Modifier.isPublic(cls2.getModifiers()) && (cls2 == cls || Modifier.isPublic(cls.getModifiers()));
            if (!z && (this.allowedModes & 8) != 0) {
                z = VerifyAccess.isClassAccessible(cls2, lookupClass(), 15) && (cls2 == cls || VerifyAccess.isClassAccessible(cls, lookupClass(), 15));
            }
            if (!z) {
                return "class is not public";
            }
            if (Modifier.isPublic(i)) {
                return "access to public member failed";
            }
            if (Modifier.isPrivate(i)) {
                return "member is private";
            }
            if (Modifier.isProtected(i)) {
                return "member is protected";
            }
            return "member is private to package";
        }

        private void checkSpecialCaller(Class<?> cls) throws IllegalAccessException {
            if (!hasPrivateAccess() || cls != lookupClass()) {
                throw new IllegalAccessException("no private access for invokespecial : " + ((Object) cls) + ", from" + ((Object) this));
            }
        }

        private void throwMakeAccessException(String str, Object obj) throws IllegalAccessException {
            String str2 = str + ": " + toString();
            if (obj != null) {
                str2 = str2 + ", from " + obj;
            }
            throw new IllegalAccessException(str2);
        }

        private void checkReturnType(Method method, MethodType methodType) throws NoSuchMethodException {
            if (method.getReturnType() != methodType.rtype()) {
                throw new NoSuchMethodException(method.getName() + ((Object) methodType));
            }
        }
    }

    private static MethodHandleImpl getMethodHandleImpl(MethodHandle methodHandle) {
        if (methodHandle instanceof Transformers.Construct) {
            methodHandle = ((Transformers.Construct) methodHandle).getConstructorHandle();
        }
        if (methodHandle instanceof Transformers.VarargsCollector) {
            methodHandle = methodHandle.asFixedArity();
        }
        if (methodHandle instanceof MethodHandleImpl) {
            return (MethodHandleImpl) methodHandle;
        }
        throw new IllegalArgumentException(((Object) methodHandle) + " is not a direct handle");
    }

    private static void checkClassIsArray(Class<?> cls) {
        if (!cls.isArray()) {
            throw new IllegalArgumentException("Not an array type: " + ((Object) cls));
        }
    }

    private static void checkTypeIsViewable(Class<?> cls) {
        if (cls == Short.TYPE || cls == Character.TYPE || cls == Integer.TYPE || cls == Long.TYPE || cls == Float.TYPE || cls == Double.TYPE) {
            return;
        }
        throw new UnsupportedOperationException("Component type not supported: " + ((Object) cls));
    }

    public static MethodHandle arrayElementGetter(Class<?> cls) throws IllegalArgumentException {
        checkClassIsArray(cls);
        Class<?> componentType = cls.getComponentType();
        if (componentType.isPrimitive()) {
            try {
                return Lookup.PUBLIC_LOOKUP.findStatic(MethodHandles.class, "arrayElementGetter", MethodType.methodType(componentType, cls, Integer.TYPE));
            } catch (IllegalAccessException | NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }
        return new Transformers.ReferenceArrayElementGetter(cls);
    }

    public static byte arrayElementGetter(byte[] bArr, int i) {
        return bArr[i];
    }

    public static boolean arrayElementGetter(boolean[] zArr, int i) {
        return zArr[i];
    }

    public static char arrayElementGetter(char[] cArr, int i) {
        return cArr[i];
    }

    public static short arrayElementGetter(short[] sArr, int i) {
        return sArr[i];
    }

    public static int arrayElementGetter(int[] iArr, int i) {
        return iArr[i];
    }

    public static long arrayElementGetter(long[] jArr, int i) {
        return jArr[i];
    }

    public static float arrayElementGetter(float[] fArr, int i) {
        return fArr[i];
    }

    public static double arrayElementGetter(double[] dArr, int i) {
        return dArr[i];
    }

    public static MethodHandle arrayElementSetter(Class<?> cls) throws IllegalArgumentException {
        checkClassIsArray(cls);
        Class<?> componentType = cls.getComponentType();
        if (componentType.isPrimitive()) {
            try {
                return Lookup.PUBLIC_LOOKUP.findStatic(MethodHandles.class, "arrayElementSetter", MethodType.methodType(Void.TYPE, cls, Integer.TYPE, componentType));
            } catch (IllegalAccessException | NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }
        return new Transformers.ReferenceArrayElementSetter(cls);
    }

    public static void arrayElementSetter(byte[] bArr, int i, byte b) {
        bArr[i] = b;
    }

    public static void arrayElementSetter(boolean[] zArr, int i, boolean z) {
        zArr[i] = z;
    }

    public static void arrayElementSetter(char[] cArr, int i, char c) {
        cArr[i] = c;
    }

    public static void arrayElementSetter(short[] sArr, int i, short s) {
        sArr[i] = s;
    }

    public static void arrayElementSetter(int[] iArr, int i, int i2) {
        iArr[i] = i2;
    }

    public static void arrayElementSetter(long[] jArr, int i, long j) {
        jArr[i] = j;
    }

    public static void arrayElementSetter(float[] fArr, int i, float f) {
        fArr[i] = f;
    }

    public static void arrayElementSetter(double[] dArr, int i, double d) {
        dArr[i] = d;
    }

    public static VarHandle arrayElementVarHandle(Class<?> cls) throws IllegalArgumentException {
        checkClassIsArray(cls);
        return ArrayElementVarHandle.create(cls);
    }

    public static VarHandle byteArrayViewVarHandle(Class<?> cls, ByteOrder byteOrder) throws IllegalArgumentException {
        checkClassIsArray(cls);
        checkTypeIsViewable(cls.getComponentType());
        return ByteArrayViewVarHandle.create(cls, byteOrder);
    }

    public static VarHandle byteBufferViewVarHandle(Class<?> cls, ByteOrder byteOrder) throws IllegalArgumentException {
        checkClassIsArray(cls);
        checkTypeIsViewable(cls.getComponentType());
        return ByteBufferViewVarHandle.create(cls, byteOrder);
    }

    public static MethodHandle spreadInvoker(MethodType methodType, int i) {
        if (i < 0 || i > methodType.parameterCount()) {
            throw MethodHandleStatics.newIllegalArgumentException("bad argument count", Integer.valueOf(i));
        }
        return invoker(methodType).asSpreader(Object[].class, methodType.parameterCount() - i);
    }

    public static MethodHandle exactInvoker(MethodType methodType) {
        return new Transformers.Invoker(methodType, true);
    }

    public static MethodHandle invoker(MethodType methodType) {
        return new Transformers.Invoker(methodType, false);
    }

    private static MethodHandle methodHandleForVarHandleAccessor(VarHandle.AccessMode accessMode, MethodType methodType, boolean z) {
        try {
            return new MethodHandleImpl(VarHandle.class.getDeclaredMethod(accessMode.methodName(), Object[].class).getArtMethod(), z ? 8 : 7, methodType.insertParameterTypes(0, VarHandle.class));
        } catch (NoSuchMethodException e) {
            throw new InternalError("No method for AccessMode " + ((Object) accessMode), e);
        }
    }

    public static MethodHandle varHandleExactInvoker(VarHandle.AccessMode accessMode, MethodType methodType) {
        return methodHandleForVarHandleAccessor(accessMode, methodType, true);
    }

    public static MethodHandle varHandleInvoker(VarHandle.AccessMode accessMode, MethodType methodType) {
        return methodHandleForVarHandleAccessor(accessMode, methodType, false);
    }

    public static MethodHandle explicitCastArguments(MethodHandle methodHandle, MethodType methodType) {
        explicitCastArgumentsChecks(methodHandle, methodType);
        MethodType methodTypeType = methodHandle.type();
        if (methodTypeType == methodType) {
            return methodHandle;
        }
        if (methodTypeType.explicitCastEquivalentToAsType(methodType)) {
            return methodHandle.asFixedArity().asType(methodType);
        }
        return new Transformers.ExplicitCastArguments(methodHandle, methodType);
    }

    private static void explicitCastArgumentsChecks(MethodHandle methodHandle, MethodType methodType) {
        if (methodHandle.type().parameterCount() != methodType.parameterCount()) {
            throw new WrongMethodTypeException("cannot explicitly cast " + ((Object) methodHandle) + " to " + ((Object) methodType));
        }
    }

    public static MethodHandle permuteArguments(MethodHandle methodHandle, MethodType methodType, int... iArr) {
        int[] iArr2 = (int[]) iArr.clone();
        permuteArgumentChecks(iArr2, methodType, methodHandle.type());
        return new Transformers.PermuteArguments(methodType, methodHandle, iArr2);
    }

    private static boolean permuteArgumentChecks(int[] iArr, MethodType methodType, MethodType methodType2) {
        if (methodType.returnType() != methodType2.returnType()) {
            throw MethodHandleStatics.newIllegalArgumentException("return types do not match", methodType2, methodType);
        }
        if (iArr.length == methodType2.parameterCount()) {
            int iParameterCount = methodType.parameterCount();
            boolean z = false;
            for (int i = 0; i < iArr.length; i++) {
                int i2 = iArr[i];
                if (i2 >= 0 && i2 < iParameterCount) {
                    if (methodType.parameterType(i2) != methodType2.parameterType(i)) {
                        throw MethodHandleStatics.newIllegalArgumentException("parameter types do not match after reorder", methodType2, methodType);
                    }
                } else {
                    z = true;
                    break;
                }
            }
            if (!z) {
                return true;
            }
        }
        throw MethodHandleStatics.newIllegalArgumentException("bad reorder array: " + Arrays.toString(iArr));
    }

    public static MethodHandle constant(Class<?> cls, Object obj) {
        if (cls.isPrimitive()) {
            if (cls == Void.TYPE) {
                throw MethodHandleStatics.newIllegalArgumentException("void type");
            }
            obj = Wrapper.forPrimitiveType(cls).convert(obj, cls);
        }
        return new Transformers.Constant(cls, obj);
    }

    public static MethodHandle identity(Class<?> cls) {
        if (cls == null) {
            throw new NullPointerException("type == null");
        }
        if (cls.isPrimitive()) {
            try {
                return Lookup.PUBLIC_LOOKUP.findStatic(MethodHandles.class, "identity", MethodType.methodType(cls, cls));
            } catch (IllegalAccessException | NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }
        return new Transformers.ReferenceIdentity(cls);
    }

    public static byte identity(byte b) {
        return b;
    }

    public static boolean identity(boolean z) {
        return z;
    }

    public static char identity(char c) {
        return c;
    }

    public static short identity(short s) {
        return s;
    }

    public static int identity(int i) {
        return i;
    }

    public static long identity(long j) {
        return j;
    }

    public static float identity(float f) {
        return f;
    }

    public static double identity(double d) {
        return d;
    }

    public static MethodHandle insertArguments(MethodHandle methodHandle, int i, Object... objArr) {
        int length = objArr.length;
        Class<?>[] clsArrInsertArgumentsChecks = insertArgumentsChecks(methodHandle, length, i);
        if (length == 0) {
            return methodHandle;
        }
        for (int i2 = 0; i2 < length; i2++) {
            int i3 = i + i2;
            Class<?> cls = clsArrInsertArgumentsChecks[i3];
            if (!cls.isPrimitive()) {
                clsArrInsertArgumentsChecks[i3].cast(objArr[i2]);
            } else {
                objArr[i2] = Wrapper.forPrimitiveType(cls).convert(objArr[i2], cls);
            }
        }
        return new Transformers.InsertArguments(methodHandle, i, objArr);
    }

    private static Class<?>[] insertArgumentsChecks(MethodHandle methodHandle, int i, int i2) throws RuntimeException {
        MethodType methodTypeType = methodHandle.type();
        int iParameterCount = methodTypeType.parameterCount() - i;
        if (iParameterCount < 0) {
            throw MethodHandleStatics.newIllegalArgumentException("too many values to insert");
        }
        if (i2 < 0 || i2 > iParameterCount) {
            throw MethodHandleStatics.newIllegalArgumentException("no argument type to append");
        }
        return methodTypeType.ptypes();
    }

    public static MethodHandle dropArguments(MethodHandle methodHandle, int i, List<Class<?>> list) {
        List<Class<?>> listCopyTypes = copyTypes(list);
        MethodType methodTypeType = methodHandle.type();
        int iDropArgumentChecks = dropArgumentChecks(methodTypeType, i, listCopyTypes);
        MethodType methodTypeInsertParameterTypes = methodTypeType.insertParameterTypes(i, listCopyTypes);
        if (iDropArgumentChecks == 0) {
            return methodHandle;
        }
        return new Transformers.DropArguments(methodTypeInsertParameterTypes, methodHandle, i, listCopyTypes.size());
    }

    private static List<Class<?>> copyTypes(List<Class<?>> list) {
        Object[] array = list.toArray();
        return Arrays.asList((Class[]) Arrays.copyOf(array, array.length, Class[].class));
    }

    private static int dropArgumentChecks(MethodType methodType, int i, List<Class<?>> list) {
        int size = list.size();
        MethodType.checkSlotCount(size);
        int iParameterCount = methodType.parameterCount();
        int i2 = iParameterCount + size;
        if (i < 0 || i > iParameterCount) {
            throw MethodHandleStatics.newIllegalArgumentException("no argument type to remove" + ((Object) Arrays.asList(methodType, Integer.valueOf(i), list, Integer.valueOf(i2), Integer.valueOf(iParameterCount))));
        }
        return size;
    }

    public static MethodHandle dropArguments(MethodHandle methodHandle, int i, Class<?>... clsArr) {
        return dropArguments(methodHandle, i, (List<Class<?>>) Arrays.asList(clsArr));
    }

    public static MethodHandle filterArguments(MethodHandle methodHandle, int i, MethodHandle... methodHandleArr) {
        filterArgumentsCheckArity(methodHandle, i, methodHandleArr);
        for (int i2 = 0; i2 < methodHandleArr.length; i2++) {
            filterArgumentChecks(methodHandle, i2 + i, methodHandleArr[i2]);
        }
        return new Transformers.FilterArguments(methodHandle, i, methodHandleArr);
    }

    private static void filterArgumentsCheckArity(MethodHandle methodHandle, int i, MethodHandle[] methodHandleArr) {
        if (i + methodHandleArr.length > methodHandle.type().parameterCount()) {
            throw MethodHandleStatics.newIllegalArgumentException("too many filters");
        }
    }

    private static void filterArgumentChecks(MethodHandle methodHandle, int i, MethodHandle methodHandle2) throws RuntimeException {
        MethodType methodTypeType = methodHandle.type();
        MethodType methodTypeType2 = methodHandle2.type();
        if (methodTypeType2.parameterCount() != 1 || methodTypeType2.returnType() != methodTypeType.parameterType(i)) {
            throw MethodHandleStatics.newIllegalArgumentException("target and filter types do not match", methodTypeType, methodTypeType2);
        }
    }

    public static MethodHandle collectArguments(MethodHandle methodHandle, int i, MethodHandle methodHandle2) {
        return new Transformers.CollectArguments(methodHandle, methodHandle2, i, collectArgumentsChecks(methodHandle, i, methodHandle2));
    }

    private static MethodType collectArgumentsChecks(MethodHandle methodHandle, int i, MethodHandle methodHandle2) throws RuntimeException {
        MethodType methodTypeType = methodHandle.type();
        MethodType methodTypeType2 = methodHandle2.type();
        Class<?> clsReturnType = methodTypeType2.returnType();
        List<Class<?>> listParameterList = methodTypeType2.parameterList();
        if (clsReturnType == Void.TYPE) {
            return methodTypeType.insertParameterTypes(i, listParameterList);
        }
        if (clsReturnType != methodTypeType.parameterType(i)) {
            throw MethodHandleStatics.newIllegalArgumentException("target and filter types do not match", methodTypeType, methodTypeType2);
        }
        return methodTypeType.dropParameterTypes(i, i + 1).insertParameterTypes(i, listParameterList);
    }

    public static MethodHandle filterReturnValue(MethodHandle methodHandle, MethodHandle methodHandle2) {
        filterReturnValueChecks(methodHandle.type(), methodHandle2.type());
        return new Transformers.FilterReturnValue(methodHandle, methodHandle2);
    }

    private static void filterReturnValueChecks(MethodType methodType, MethodType methodType2) throws RuntimeException {
        Class<?> clsReturnType = methodType.returnType();
        int iParameterCount = methodType2.parameterCount();
        if (iParameterCount == 0) {
            if (clsReturnType == Void.TYPE) {
                return;
            }
        } else if (clsReturnType == methodType2.parameterType(0) && iParameterCount == 1) {
            return;
        }
        throw MethodHandleStatics.newIllegalArgumentException("target and filter types do not match", methodType, methodType2);
    }

    public static MethodHandle foldArguments(MethodHandle methodHandle, MethodHandle methodHandle2) {
        foldArgumentChecks(0, methodHandle.type(), methodHandle2.type());
        return new Transformers.FoldArguments(methodHandle, methodHandle2);
    }

    private static Class<?> foldArgumentChecks(int i, MethodType methodType, MethodType methodType2) {
        int iParameterCount = methodType2.parameterCount();
        Class<?> clsReturnType = methodType2.returnType();
        int i2 = clsReturnType == Void.TYPE ? 0 : 1;
        int i3 = i + i2;
        int i4 = iParameterCount + i3;
        boolean z = methodType.parameterCount() >= i4;
        if (z && !methodType2.parameterList().equals(methodType.parameterList().subList(i3, i4))) {
            z = false;
        }
        if (z && i2 != 0 && methodType2.returnType() != methodType.parameterType(0)) {
            z = false;
        }
        if (!z) {
            throw misMatchedTypes("target and combiner types", methodType, methodType2);
        }
        return clsReturnType;
    }

    public static MethodHandle guardWithTest(MethodHandle methodHandle, MethodHandle methodHandle2, MethodHandle methodHandle3) {
        MethodType methodTypeType = methodHandle.type();
        MethodType methodTypeType2 = methodHandle2.type();
        MethodType methodTypeType3 = methodHandle3.type();
        if (!methodTypeType2.equals((Object) methodTypeType3)) {
            throw misMatchedTypes("target and fallback types", methodTypeType2, methodTypeType3);
        }
        if (methodTypeType.returnType() != Boolean.TYPE) {
            throw MethodHandleStatics.newIllegalArgumentException("guard type is not a predicate " + ((Object) methodTypeType));
        }
        List<Class<?>> listParameterList = methodTypeType2.parameterList();
        List<Class<?>> listParameterList2 = methodTypeType.parameterList();
        if (!listParameterList.equals(listParameterList2)) {
            int size = listParameterList2.size();
            int size2 = listParameterList.size();
            if (size >= size2 || !listParameterList.subList(0, size).equals(listParameterList2)) {
                throw misMatchedTypes("target and test types", methodTypeType2, methodTypeType);
            }
            methodHandle = dropArguments(methodHandle, size, listParameterList.subList(size, size2));
            methodHandle.type();
        }
        return new Transformers.GuardWithTest(methodHandle, methodHandle2, methodHandle3);
    }

    static RuntimeException misMatchedTypes(String str, MethodType methodType, MethodType methodType2) {
        return MethodHandleStatics.newIllegalArgumentException(str + " must match: " + ((Object) methodType) + " != " + ((Object) methodType2));
    }

    public static MethodHandle catchException(MethodHandle methodHandle, Class<? extends Throwable> cls, MethodHandle methodHandle2) {
        int size;
        MethodType methodTypeType = methodHandle.type();
        MethodType methodTypeType2 = methodHandle2.type();
        if (methodTypeType2.parameterCount() < 1 || !methodTypeType2.parameterType(0).isAssignableFrom(cls)) {
            throw MethodHandleStatics.newIllegalArgumentException("handler does not accept exception type " + ((Object) cls));
        }
        if (methodTypeType2.returnType() != methodTypeType.returnType()) {
            throw misMatchedTypes("target and handler return types", methodTypeType, methodTypeType2);
        }
        List<Class<?>> listParameterList = methodTypeType.parameterList();
        List<Class<?>> listParameterList2 = methodTypeType2.parameterList();
        List<Class<?>> listSubList = listParameterList2.subList(1, listParameterList2.size());
        if (!listParameterList.equals(listSubList) && ((size = listSubList.size()) >= listParameterList.size() || !listParameterList.subList(0, size).equals(listSubList))) {
            throw misMatchedTypes("target and handler types", methodTypeType, methodTypeType2);
        }
        return new Transformers.CatchException(methodHandle, methodHandle2, cls);
    }

    public static MethodHandle throwException(Class<?> cls, Class<? extends Throwable> cls2) {
        if (!Throwable.class.isAssignableFrom(cls2)) {
            throw new ClassCastException(cls2.getName());
        }
        return new Transformers.AlwaysThrow(cls, cls2);
    }
}
