package java.lang.invoke;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.invoke.util.BytecodeDescriptor;
import sun.invoke.util.Wrapper;

public final class MethodType implements Serializable {
    static final boolean $assertionsDisabled = false;
    static final int MAX_JVM_ARITY = 255;
    static final int MAX_MH_ARITY = 254;
    static final int MAX_MH_INVOKER_ARITY = 253;
    private static final long ptypesOffset;
    private static final long rtypeOffset;
    private static final long serialVersionUID = 292;

    @Stable
    private MethodTypeForm form;

    @Stable
    private String methodDescriptor;
    private final Class<?>[] ptypes;
    private final Class<?> rtype;

    @Stable
    private MethodType wrapAlt;
    static final ConcurrentWeakInternSet<MethodType> internTable = new ConcurrentWeakInternSet<>();
    static final Class<?>[] NO_PTYPES = new Class[0];
    private static final MethodType[] objectOnlyTypes = new MethodType[20];
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0];

    static {
        try {
            rtypeOffset = MethodHandleStatics.UNSAFE.objectFieldOffset(MethodType.class.getDeclaredField("rtype"));
            ptypesOffset = MethodHandleStatics.UNSAFE.objectFieldOffset(MethodType.class.getDeclaredField("ptypes"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private MethodType(Class<?> cls, Class<?>[] clsArr, boolean z) {
        checkRtype(cls);
        checkPtypes(clsArr);
        this.rtype = cls;
        this.ptypes = z ? clsArr : (Class[]) Arrays.copyOf(clsArr, clsArr.length);
    }

    private MethodType(Class<?>[] clsArr, Class<?> cls) {
        this.rtype = cls;
        this.ptypes = clsArr;
    }

    MethodTypeForm form() {
        return this.form;
    }

    public Class<?> rtype() {
        return this.rtype;
    }

    public Class<?>[] ptypes() {
        return this.ptypes;
    }

    private static void checkRtype(Class<?> cls) {
        Objects.requireNonNull(cls);
    }

    private static void checkPtype(Class<?> cls) {
        Objects.requireNonNull(cls);
        if (cls == Void.TYPE) {
            throw MethodHandleStatics.newIllegalArgumentException("parameter type cannot be void");
        }
    }

    private static int checkPtypes(Class<?>[] clsArr) {
        int i = 0;
        for (Class<?> cls : clsArr) {
            checkPtype(cls);
            if (cls == Double.TYPE || cls == Long.TYPE) {
                i++;
            }
        }
        checkSlotCount(clsArr.length + i);
        return i;
    }

    static void checkSlotCount(int i) {
        if ((i & MAX_JVM_ARITY) != i) {
            throw MethodHandleStatics.newIllegalArgumentException("bad parameter count " + i);
        }
    }

    private static IndexOutOfBoundsException newIndexOutOfBoundsException(Object obj) {
        if (obj instanceof Integer) {
            obj = "bad index: " + obj;
        }
        return new IndexOutOfBoundsException(obj.toString());
    }

    public static MethodType methodType(Class<?> cls, Class<?>[] clsArr) {
        return makeImpl(cls, clsArr, $assertionsDisabled);
    }

    public static MethodType methodType(Class<?> cls, List<Class<?>> list) {
        return makeImpl(cls, listToArray(list), $assertionsDisabled);
    }

    private static Class<?>[] listToArray(List<Class<?>> list) {
        checkSlotCount(list.size());
        return (Class[]) list.toArray(NO_PTYPES);
    }

    public static MethodType methodType(Class<?> cls, Class<?> cls2, Class<?>... clsArr) {
        Class[] clsArr2 = new Class[clsArr.length + 1];
        clsArr2[0] = cls2;
        System.arraycopy(clsArr, 0, clsArr2, 1, clsArr.length);
        return makeImpl(cls, clsArr2, true);
    }

    public static MethodType methodType(Class<?> cls) {
        return makeImpl(cls, NO_PTYPES, true);
    }

    public static MethodType methodType(Class<?> cls, Class<?> cls2) {
        return makeImpl(cls, new Class[]{cls2}, true);
    }

    public static MethodType methodType(Class<?> cls, MethodType methodType) {
        return makeImpl(cls, methodType.ptypes, true);
    }

    static MethodType makeImpl(Class<?> cls, Class<?>[] clsArr, boolean z) {
        MethodType methodType = internTable.get(new MethodType(clsArr, cls));
        if (methodType != null) {
            return methodType;
        }
        if (clsArr.length == 0) {
            clsArr = NO_PTYPES;
            z = true;
        }
        MethodType methodType2 = new MethodType(cls, clsArr, z);
        methodType2.form = MethodTypeForm.findForm(methodType2);
        return internTable.add(methodType2);
    }

    public static MethodType genericMethodType(int i, boolean z) {
        MethodType methodType;
        checkSlotCount(i);
        int i2 = (i * 2) + (z ? 1 : 0);
        if (i2 < objectOnlyTypes.length && (methodType = objectOnlyTypes[i2]) != null) {
            return methodType;
        }
        Class[] clsArr = new Class[i + (z ? 1 : 0)];
        Arrays.fill(clsArr, Object.class);
        if (z) {
            clsArr[i] = Object[].class;
        }
        MethodType methodTypeMakeImpl = makeImpl(Object.class, clsArr, true);
        if (i2 < objectOnlyTypes.length) {
            objectOnlyTypes[i2] = methodTypeMakeImpl;
        }
        return methodTypeMakeImpl;
    }

    public static MethodType genericMethodType(int i) {
        return genericMethodType(i, $assertionsDisabled);
    }

    public MethodType changeParameterType(int i, Class<?> cls) {
        if (parameterType(i) == cls) {
            return this;
        }
        checkPtype(cls);
        Class[] clsArr = (Class[]) this.ptypes.clone();
        clsArr[i] = cls;
        return makeImpl(this.rtype, clsArr, true);
    }

    public MethodType insertParameterTypes(int i, Class<?>... clsArr) {
        int length = this.ptypes.length;
        if (i < 0 || i > length) {
            throw newIndexOutOfBoundsException(Integer.valueOf(i));
        }
        checkSlotCount(parameterSlotCount() + clsArr.length + checkPtypes(clsArr));
        int length2 = clsArr.length;
        if (length2 == 0) {
            return this;
        }
        Class[] clsArr2 = (Class[]) Arrays.copyOfRange(this.ptypes, 0, length + length2);
        System.arraycopy(clsArr2, i, clsArr2, i + length2, length - i);
        System.arraycopy(clsArr, 0, clsArr2, i, length2);
        return makeImpl(this.rtype, clsArr2, true);
    }

    public MethodType appendParameterTypes(Class<?>... clsArr) {
        return insertParameterTypes(parameterCount(), clsArr);
    }

    public MethodType insertParameterTypes(int i, List<Class<?>> list) {
        return insertParameterTypes(i, listToArray(list));
    }

    public MethodType appendParameterTypes(List<Class<?>> list) {
        return insertParameterTypes(parameterCount(), list);
    }

    MethodType replaceParameterTypes(int i, int i2, Class<?>... clsArr) {
        if (i == i2) {
            return insertParameterTypes(i, clsArr);
        }
        int length = this.ptypes.length;
        if (i < 0 || i > i2 || i2 > length) {
            throw newIndexOutOfBoundsException("start=" + i + " end=" + i2);
        }
        if (clsArr.length == 0) {
            return dropParameterTypes(i, i2);
        }
        return dropParameterTypes(i, i2).insertParameterTypes(i, clsArr);
    }

    MethodType asSpreaderType(Class<?> cls, int i) {
        int length = this.ptypes.length - i;
        if (i == 0) {
            return this;
        }
        if (cls == Object[].class) {
            if (isGeneric()) {
                return this;
            }
            if (length == 0) {
                MethodType methodTypeGenericMethodType = genericMethodType(i);
                if (this.rtype != Object.class) {
                    return methodTypeGenericMethodType.changeReturnType(this.rtype);
                }
                return methodTypeGenericMethodType;
            }
        }
        Class<?> componentType = cls.getComponentType();
        while (length < this.ptypes.length) {
            if (this.ptypes[length] == componentType) {
                length++;
            } else {
                Class[] clsArr = (Class[]) this.ptypes.clone();
                Arrays.fill(clsArr, length, this.ptypes.length, componentType);
                return methodType(this.rtype, (Class<?>[]) clsArr);
            }
        }
        return this;
    }

    Class<?> leadingReferenceParameter() {
        if (this.ptypes.length != 0) {
            Class<?> cls = this.ptypes[0];
            if (!cls.isPrimitive()) {
                return cls;
            }
        }
        throw MethodHandleStatics.newIllegalArgumentException("no leading reference parameter");
    }

    MethodType asCollectorType(Class<?> cls, int i) {
        MethodType methodType;
        if (cls == Object[].class) {
            methodType = genericMethodType(i);
            if (this.rtype != Object.class) {
                methodType = methodType.changeReturnType(this.rtype);
            }
        } else {
            methodType = methodType(this.rtype, (List<Class<?>>) Collections.nCopies(i, cls.getComponentType()));
        }
        if (this.ptypes.length == 1) {
            return methodType;
        }
        return methodType.insertParameterTypes(0, parameterList().subList(0, this.ptypes.length - 1));
    }

    public MethodType dropParameterTypes(int i, int i2) {
        Class<?>[] clsArr;
        int length = this.ptypes.length;
        if (i < 0 || i > i2 || i2 > length) {
            throw newIndexOutOfBoundsException("start=" + i + " end=" + i2);
        }
        if (i == i2) {
            return this;
        }
        if (i == 0) {
            if (i2 == length) {
                clsArr = NO_PTYPES;
            } else {
                clsArr = (Class[]) Arrays.copyOfRange(this.ptypes, i2, length);
            }
        } else if (i2 == length) {
            clsArr = (Class[]) Arrays.copyOfRange(this.ptypes, 0, i);
        } else {
            int i3 = length - i2;
            Class<?>[] clsArr2 = (Class[]) Arrays.copyOfRange(this.ptypes, 0, i + i3);
            System.arraycopy(this.ptypes, i2, clsArr2, i, i3);
            clsArr = clsArr2;
        }
        return makeImpl(this.rtype, clsArr, true);
    }

    public MethodType changeReturnType(Class<?> cls) {
        return returnType() == cls ? this : makeImpl(cls, this.ptypes, true);
    }

    public boolean hasPrimitives() {
        return this.form.hasPrimitives();
    }

    public boolean hasWrappers() {
        if (unwrap() != this) {
            return true;
        }
        return $assertionsDisabled;
    }

    public MethodType erase() {
        return this.form.erasedType();
    }

    MethodType basicType() {
        return this.form.basicType();
    }

    MethodType invokerType() {
        return insertParameterTypes(0, MethodHandle.class);
    }

    public MethodType generic() {
        return genericMethodType(parameterCount());
    }

    boolean isGeneric() {
        if (this != erase() || hasPrimitives()) {
            return $assertionsDisabled;
        }
        return true;
    }

    public MethodType wrap() {
        return hasPrimitives() ? wrapWithPrims(this) : this;
    }

    public MethodType unwrap() {
        MethodType methodTypeWrapWithPrims;
        if (hasPrimitives()) {
            methodTypeWrapWithPrims = wrapWithPrims(this);
        } else {
            methodTypeWrapWithPrims = this;
        }
        return unwrapWithNoPrims(methodTypeWrapWithPrims);
    }

    private static MethodType wrapWithPrims(MethodType methodType) {
        MethodType methodType2 = methodType.wrapAlt;
        if (methodType2 == null) {
            MethodType methodTypeCanonicalize = MethodTypeForm.canonicalize(methodType, 2, 2);
            methodType.wrapAlt = methodTypeCanonicalize;
            return methodTypeCanonicalize;
        }
        return methodType2;
    }

    private static MethodType unwrapWithNoPrims(MethodType methodType) {
        MethodType methodTypeCanonicalize = methodType.wrapAlt;
        if (methodTypeCanonicalize == null) {
            methodTypeCanonicalize = MethodTypeForm.canonicalize(methodType, 3, 3);
            if (methodTypeCanonicalize == null) {
                methodTypeCanonicalize = methodType;
            }
            methodType.wrapAlt = methodTypeCanonicalize;
        }
        return methodTypeCanonicalize;
    }

    public Class<?> parameterType(int i) {
        return this.ptypes[i];
    }

    public int parameterCount() {
        return this.ptypes.length;
    }

    public Class<?> returnType() {
        return this.rtype;
    }

    public List<Class<?>> parameterList() {
        return Collections.unmodifiableList(Arrays.asList((Class[]) this.ptypes.clone()));
    }

    Class<?> lastParameterType() {
        int length = this.ptypes.length;
        return length == 0 ? Void.TYPE : this.ptypes[length - 1];
    }

    public Class<?>[] parameterArray() {
        return (Class[]) this.ptypes.clone();
    }

    public boolean equals(Object obj) {
        if (this == obj || ((obj instanceof MethodType) && equals((MethodType) obj))) {
            return true;
        }
        return $assertionsDisabled;
    }

    private boolean equals(MethodType methodType) {
        if (this.rtype == methodType.rtype && Arrays.equals(this.ptypes, methodType.ptypes)) {
            return true;
        }
        return $assertionsDisabled;
    }

    public int hashCode() {
        int iHashCode = this.rtype.hashCode() + 31;
        for (Class<?> cls : this.ptypes) {
            iHashCode = (iHashCode * 31) + cls.hashCode();
        }
        return iHashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < this.ptypes.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(this.ptypes[i].getSimpleName());
        }
        sb.append(")");
        sb.append(this.rtype.getSimpleName());
        return sb.toString();
    }

    boolean isConvertibleTo(MethodType methodType) {
        MethodTypeForm methodTypeFormForm = form();
        MethodTypeForm methodTypeFormForm2 = methodType.form();
        if (methodTypeFormForm == methodTypeFormForm2) {
            return true;
        }
        if (!canConvert(returnType(), methodType.returnType())) {
            return $assertionsDisabled;
        }
        Class<?>[] clsArr = methodType.ptypes;
        Class<?>[] clsArr2 = this.ptypes;
        if (clsArr == clsArr2) {
            return true;
        }
        int length = clsArr.length;
        if (length != clsArr2.length) {
            return $assertionsDisabled;
        }
        if (length <= 1) {
            if (length != 1 || canConvert(clsArr[0], clsArr2[0])) {
                return true;
            }
            return $assertionsDisabled;
        }
        if ((methodTypeFormForm.primitiveParameterCount() == 0 && methodTypeFormForm.erasedType == this) || (methodTypeFormForm2.primitiveParameterCount() == 0 && methodTypeFormForm2.erasedType == methodType)) {
            return true;
        }
        return canConvertParameters(clsArr, clsArr2);
    }

    boolean explicitCastEquivalentToAsType(MethodType methodType) {
        if (this == methodType) {
            return true;
        }
        if (!explicitCastEquivalentToAsType(this.rtype, methodType.rtype)) {
            return $assertionsDisabled;
        }
        Class<?>[] clsArr = methodType.ptypes;
        Class<?>[] clsArr2 = this.ptypes;
        if (clsArr2 == clsArr) {
            return true;
        }
        for (int i = 0; i < clsArr2.length; i++) {
            if (!explicitCastEquivalentToAsType(clsArr[i], clsArr2[i])) {
                return $assertionsDisabled;
            }
        }
        return true;
    }

    private static boolean explicitCastEquivalentToAsType(Class<?> cls, Class<?> cls2) {
        if (cls == cls2 || cls2 == Object.class || cls2 == Void.TYPE) {
            return true;
        }
        if (cls.isPrimitive() && cls != Void.TYPE) {
            return canConvert(cls, cls2);
        }
        if (cls2.isPrimitive()) {
            return $assertionsDisabled;
        }
        if (!cls2.isInterface() || cls2.isAssignableFrom(cls)) {
            return true;
        }
        return $assertionsDisabled;
    }

    private boolean canConvertParameters(Class<?>[] clsArr, Class<?>[] clsArr2) {
        for (int i = 0; i < clsArr.length; i++) {
            if (!canConvert(clsArr[i], clsArr2[i])) {
                return $assertionsDisabled;
            }
        }
        return true;
    }

    static boolean canConvert(Class<?> cls, Class<?> cls2) {
        if (cls == cls2 || cls == Object.class || cls2 == Object.class) {
            return true;
        }
        if (cls.isPrimitive()) {
            if (cls == Void.TYPE) {
                return true;
            }
            Wrapper wrapperForPrimitiveType = Wrapper.forPrimitiveType(cls);
            if (cls2.isPrimitive()) {
                return Wrapper.forPrimitiveType(cls2).isConvertibleFrom(wrapperForPrimitiveType);
            }
            return cls2.isAssignableFrom(wrapperForPrimitiveType.wrapperType());
        }
        if (!cls2.isPrimitive() || cls2 == Void.TYPE) {
            return true;
        }
        Wrapper wrapperForPrimitiveType2 = Wrapper.forPrimitiveType(cls2);
        if (cls.isAssignableFrom(wrapperForPrimitiveType2.wrapperType())) {
            return true;
        }
        if (Wrapper.isWrapperType(cls) && wrapperForPrimitiveType2.isConvertibleFrom(Wrapper.forWrapperType(cls))) {
            return true;
        }
        return $assertionsDisabled;
    }

    int parameterSlotCount() {
        return this.form.parameterSlotCount();
    }

    public static MethodType fromMethodDescriptorString(String str, ClassLoader classLoader) throws TypeNotPresentException, IllegalArgumentException {
        if (!str.startsWith("(") || str.indexOf(41) < 0 || str.indexOf(46) >= 0) {
            throw MethodHandleStatics.newIllegalArgumentException("not a method descriptor: " + str);
        }
        List<Class<?>> method = BytecodeDescriptor.parseMethod(str, classLoader);
        Class<?> clsRemove = method.remove(method.size() - 1);
        checkSlotCount(method.size());
        return makeImpl(clsRemove, listToArray(method), true);
    }

    public String toMethodDescriptorString() {
        String str = this.methodDescriptor;
        if (str == null) {
            String strUnparse = BytecodeDescriptor.unparse(this);
            this.methodDescriptor = strUnparse;
            return strUnparse;
        }
        return str;
    }

    static String toFieldDescriptorString(Class<?> cls) {
        return BytecodeDescriptor.unparse(cls);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(returnType());
        objectOutputStream.writeObject(parameterArray());
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        Class<?> cls = (Class) objectInputStream.readObject();
        Class[] clsArr = (Class[]) objectInputStream.readObject();
        checkRtype(cls);
        checkPtypes(clsArr);
        MethodType_init(cls, (Class[]) clsArr.clone());
    }

    private MethodType() {
        this.rtype = null;
        this.ptypes = null;
    }

    private void MethodType_init(Class<?> cls, Class<?>[] clsArr) {
        checkRtype(cls);
        checkPtypes(clsArr);
        MethodHandleStatics.UNSAFE.putObject(this, rtypeOffset, cls);
        MethodHandleStatics.UNSAFE.putObject(this, ptypesOffset, clsArr);
    }

    private Object readResolve() {
        return methodType(this.rtype, this.ptypes);
    }

    private static class ConcurrentWeakInternSet<T> {
        private final ConcurrentMap<WeakEntry<T>, WeakEntry<T>> map = new ConcurrentHashMap();
        private final ReferenceQueue<T> stale = new ReferenceQueue<>();

        public T get(T t) {
            T t2;
            if (t == null) {
                throw new NullPointerException();
            }
            expungeStaleElements();
            WeakEntry<T> weakEntry = this.map.get(new WeakEntry(t));
            if (weakEntry != null && (t2 = weakEntry.get()) != null) {
                return t2;
            }
            return null;
        }

        public T add(T t) {
            T t2;
            if (t == null) {
                throw new NullPointerException();
            }
            WeakEntry<T> weakEntry = new WeakEntry<>(t, this.stale);
            do {
                expungeStaleElements();
                WeakEntry<T> weakEntryPutIfAbsent = this.map.putIfAbsent(weakEntry, weakEntry);
                if (weakEntryPutIfAbsent != null) {
                    t2 = weakEntryPutIfAbsent.get();
                } else {
                    t2 = t;
                }
            } while (t2 == null);
            return t2;
        }

        private void expungeStaleElements() {
            while (true) {
                Reference<? extends T> referencePoll = this.stale.poll();
                if (referencePoll != null) {
                    this.map.remove(referencePoll);
                } else {
                    return;
                }
            }
        }

        private static class WeakEntry<T> extends WeakReference<T> {
            public final int hashcode;

            public WeakEntry(T t, ReferenceQueue<T> referenceQueue) {
                super(t, referenceQueue);
                this.hashcode = t.hashCode();
            }

            public WeakEntry(T t) {
                super(t);
                this.hashcode = t.hashCode();
            }

            public boolean equals(Object obj) {
                if (!(obj instanceof WeakEntry)) {
                    return MethodType.$assertionsDisabled;
                }
                T t = ((WeakEntry) obj).get();
                T t2 = get();
                if (t != null && t2 != null) {
                    return t2.equals(t);
                }
                if (this == obj) {
                    return true;
                }
                return MethodType.$assertionsDisabled;
            }

            public int hashCode() {
                return this.hashcode;
            }
        }
    }
}
