package java.io;

import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;
import libcore.io.Memory;
import libcore.util.EmptyArray;

public class ObjectStreamClass implements Serializable {
    static final Class<?> ARRAY_OF_FIELDS;
    static final Class<?> CLASSCLASS;
    private static final int CLASS_MODIFIERS_MASK = 1553;
    private static final int CLINIT_MODIFIERS = 8;
    private static final String CLINIT_NAME = "<clinit>";
    private static final String CLINIT_SIGNATURE = "()V";
    static final long CONSTRUCTOR_IS_NOT_RESOLVED = -1;
    private static final Class<Externalizable> EXTERNALIZABLE;
    private static final int FIELD_MODIFIERS_MASK = 223;
    private static final int METHOD_MODIFIERS_MASK = 3391;
    static final Class<ObjectStreamClass> OBJECTSTREAMCLASSCLASS;
    private static final Class<Serializable> SERIALIZABLE;
    static final Class<String> STRINGCLASS;
    private static final String UID_FIELD_NAME = "serialVersionUID";
    private static final long serialVersionUID = -6120832682080437368L;
    private static SoftReference<ThreadLocal<WeakHashMap<Class<?>, ObjectStreamClass>>> storage;
    private transient boolean arePropertiesResolved;
    private volatile transient List<ObjectStreamClass> cachedHierarchy;
    private transient String className;
    private transient ObjectStreamField[] fields;
    private transient byte flags;
    private transient boolean isEnum;
    private transient boolean isExternalizable;
    private transient boolean isProxy;
    private transient boolean isSerializable;
    private transient ObjectStreamField[] loadFields;
    private transient Method methodReadObject;
    private transient Method methodReadObjectNoData;
    private transient Method methodReadResolve;
    private transient Method methodWriteObject;
    private transient Method methodWriteReplace;
    private transient Class<?> resolvedClass;
    private transient Class<?> resolvedConstructorClass;
    private transient long resolvedConstructorMethodId;
    private transient ObjectStreamClass superclass;
    private transient long svUID;
    private static final Class<?>[] READ_PARAM_TYPES = {ObjectInputStream.class};
    private static final Class<?>[] WRITE_PARAM_TYPES = {ObjectOutputStream.class};
    public static final ObjectStreamField[] NO_FIELDS = new ObjectStreamField[0];
    private transient HashMap<ObjectStreamField, Field> reflectionFields = new HashMap<>();
    private transient long constructor = -1;

    private static native long getConstructorId(Class<?> cls);

    static native String getConstructorSignature(Constructor<?> constructor);

    private static native String getFieldSignature(Field field);

    static native String getMethodSignature(Method method);

    private static native boolean hasClinit(Class<?> cls);

    private static native Object newInstance(Class<?> cls, long j);

    static {
        try {
            ARRAY_OF_FIELDS = Class.forName("[Ljava.io.ObjectStreamField;");
            SERIALIZABLE = Serializable.class;
            EXTERNALIZABLE = Externalizable.class;
            STRINGCLASS = String.class;
            CLASSCLASS = Class.class;
            OBJECTSTREAMCLASSCLASS = ObjectStreamClass.class;
            storage = new SoftReference<>(null);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    void setConstructor(long newConstructor) {
        this.constructor = newConstructor;
    }

    long getConstructor() {
        return this.constructor;
    }

    Field checkAndGetReflectionField(ObjectStreamField osf) {
        Field field;
        synchronized (this.reflectionFields) {
            field = this.reflectionFields.get(osf);
            if (field == null && !this.reflectionFields.containsKey(osf)) {
                try {
                    Class<?> declaringClass = forClass();
                    field = declaringClass.getDeclaredField(osf.getName());
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                        field = null;
                    } else {
                        field.setAccessible(true);
                    }
                } catch (NoSuchFieldException e) {
                    field = null;
                }
                synchronized (this.reflectionFields) {
                    this.reflectionFields.put(osf, field);
                }
            }
        }
        return field;
    }

    ObjectStreamClass() {
    }

    private static ObjectStreamClass createClassDesc(Class<?> cl) {
        ObjectStreamClass result = new ObjectStreamClass();
        boolean isArray = cl.isArray();
        boolean serializable = isSerializable(cl);
        boolean externalizable = isExternalizable(cl);
        result.isSerializable = serializable;
        result.isExternalizable = externalizable;
        result.setName(cl.getName());
        result.setClass(cl);
        Class<?> superclass = cl.getSuperclass();
        if (superclass != null) {
            result.setSuperclass(lookup(superclass));
        }
        Field[] declaredFields = null;
        if (serializable || externalizable) {
            if (result.isEnum() || result.isProxy()) {
                result.setSerialVersionUID(0L);
            } else {
                declaredFields = cl.getDeclaredFields();
                result.setSerialVersionUID(computeSerialVersionUID(cl, declaredFields));
            }
        }
        if (serializable && !isArray) {
            if (declaredFields == null) {
                declaredFields = cl.getDeclaredFields();
            }
            result.buildFieldDescriptors(declaredFields);
        } else {
            result.setFields(NO_FIELDS);
        }
        ObjectStreamField[] fields = result.getFields();
        if (fields != null) {
            ObjectStreamField[] loadFields = new ObjectStreamField[fields.length];
            for (int i = 0; i < fields.length; i++) {
                loadFields[i] = new ObjectStreamField(fields[i].getName(), fields[i].getType(), fields[i].isUnshared());
                loadFields[i].getTypeString();
            }
            result.setLoadFields(loadFields);
        }
        byte flags = 0;
        if (externalizable) {
            byte flags2 = (byte) 4;
            flags = (byte) (flags2 | 8);
        } else if (serializable) {
            flags = (byte) 2;
        }
        result.methodWriteReplace = findMethod(cl, "writeReplace");
        result.methodReadResolve = findMethod(cl, "readResolve");
        result.methodWriteObject = findPrivateMethod(cl, "writeObject", WRITE_PARAM_TYPES);
        result.methodReadObject = findPrivateMethod(cl, "readObject", READ_PARAM_TYPES);
        result.methodReadObjectNoData = findPrivateMethod(cl, "readObjectNoData", EmptyArray.CLASS);
        if (result.hasMethodWriteObject()) {
            flags = (byte) (flags | 1);
        }
        result.setFlags(flags);
        return result;
    }

    void buildFieldDescriptors(Field[] declaredFields) {
        ObjectStreamField[] _fields;
        Field f = fieldSerialPersistentFields(forClass());
        boolean useReflectFields = f == null;
        if (!useReflectFields) {
            f.setAccessible(true);
            try {
                _fields = (ObjectStreamField[]) f.get(null);
            } catch (IllegalAccessException ex) {
                throw new AssertionError(ex);
            }
        } else {
            List<ObjectStreamField> serializableFields = new ArrayList<>(declaredFields.length);
            for (Field declaredField : declaredFields) {
                int modifiers = declaredField.getModifiers();
                if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
                    ObjectStreamField field = new ObjectStreamField(declaredField.getName(), declaredField.getType());
                    serializableFields.add(field);
                }
            }
            if (serializableFields.size() == 0) {
                _fields = NO_FIELDS;
            } else {
                _fields = (ObjectStreamField[]) serializableFields.toArray(new ObjectStreamField[serializableFields.size()]);
            }
        }
        Arrays.sort(_fields);
        int primOffset = 0;
        int objectOffset = 0;
        for (int i = 0; i < _fields.length; i++) {
            Class<?> type = _fields[i].getType();
            if (type.isPrimitive()) {
                _fields[i].offset = primOffset;
                primOffset += primitiveSize(type);
            } else {
                _fields[i].offset = objectOffset;
                objectOffset++;
            }
        }
        this.fields = _fields;
    }

    private static long computeSerialVersionUID(Class<?> cl, Field[] fields) {
        for (Field field : fields) {
            if (field.getType() == Long.TYPE) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && UID_FIELD_NAME.equals(field.getName())) {
                    field.setAccessible(true);
                    try {
                        return field.getLong(null);
                    } catch (IllegalAccessException iae) {
                        throw new RuntimeException("Error fetching SUID: " + iae);
                    }
                }
            }
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            ByteArrayOutputStream sha = new ByteArrayOutputStream();
            try {
                DataOutputStream output = new DataOutputStream(sha);
                output.writeUTF(cl.getName());
                int classModifiers = cl.getModifiers() & CLASS_MODIFIERS_MASK;
                boolean isArray = cl.isArray();
                if (isArray) {
                    classModifiers |= 1024;
                }
                if (cl.isInterface() && !Modifier.isPublic(classModifiers)) {
                    classModifiers &= -1025;
                }
                output.writeInt(classModifiers);
                if (!isArray) {
                    Class<?>[] interfaces = cl.getInterfaces();
                    if (interfaces.length > 1) {
                        Comparator<Class<?>> interfaceComparator = new Comparator<Class<?>>() {
                            @Override
                            public int compare(Class<?> itf1, Class<?> itf2) {
                                return itf1.getName().compareTo(itf2.getName());
                            }
                        };
                        Arrays.sort(interfaces, interfaceComparator);
                    }
                    for (Class<?> cls : interfaces) {
                        output.writeUTF(cls.getName());
                    }
                }
                if (fields.length > 1) {
                    Comparator<Field> fieldComparator = new Comparator<Field>() {
                        @Override
                        public int compare(Field field1, Field field2) {
                            return field1.getName().compareTo(field2.getName());
                        }
                    };
                    Arrays.sort(fields, fieldComparator);
                }
                for (Field field2 : fields) {
                    int modifiers2 = field2.getModifiers() & 223;
                    boolean skip = Modifier.isPrivate(modifiers2) && (Modifier.isTransient(modifiers2) || Modifier.isStatic(modifiers2));
                    if (!skip) {
                        output.writeUTF(field2.getName());
                        output.writeInt(modifiers2);
                        output.writeUTF(descriptorForFieldSignature(getFieldSignature(field2)));
                    }
                }
                if (hasClinit(cl)) {
                    output.writeUTF(CLINIT_NAME);
                    output.writeInt(8);
                    output.writeUTF(CLINIT_SIGNATURE);
                }
                Constructor<?>[] constructors = cl.getDeclaredConstructors();
                if (constructors.length > 1) {
                    Comparator<Constructor<?>> constructorComparator = new Comparator<Constructor<?>>() {
                        @Override
                        public int compare(Constructor<?> ctr1, Constructor<?> ctr2) {
                            return ObjectStreamClass.getConstructorSignature(ctr1).compareTo(ObjectStreamClass.getConstructorSignature(ctr2));
                        }
                    };
                    Arrays.sort(constructors, constructorComparator);
                }
                for (Constructor<?> constructor : constructors) {
                    int modifiers3 = constructor.getModifiers() & METHOD_MODIFIERS_MASK;
                    boolean isPrivate = Modifier.isPrivate(modifiers3);
                    if (!isPrivate) {
                        output.writeUTF("<init>");
                        output.writeInt(modifiers3);
                        output.writeUTF(descriptorForSignature(getConstructorSignature(constructor)).replace('/', '.'));
                    }
                }
                Method[] methods = cl.getDeclaredMethods();
                if (methods.length > 1) {
                    Comparator<Method> methodComparator = new Comparator<Method>() {
                        @Override
                        public int compare(Method m1, Method m2) {
                            int result = m1.getName().compareTo(m2.getName());
                            if (result == 0) {
                                return ObjectStreamClass.getMethodSignature(m1).compareTo(ObjectStreamClass.getMethodSignature(m2));
                            }
                            return result;
                        }
                    };
                    Arrays.sort(methods, methodComparator);
                }
                for (Method method : methods) {
                    int modifiers4 = method.getModifiers() & METHOD_MODIFIERS_MASK;
                    boolean isPrivate2 = Modifier.isPrivate(modifiers4);
                    if (!isPrivate2) {
                        output.writeUTF(method.getName());
                        output.writeInt(modifiers4);
                        output.writeUTF(descriptorForSignature(getMethodSignature(method)).replace('/', '.'));
                    }
                }
                byte[] hash = digest.digest(sha.toByteArray());
                return Memory.peekLong(hash, 0, ByteOrder.LITTLE_ENDIAN);
            } catch (IOException e) {
                throw new RuntimeException(e + " computing SHA-1/SUID");
            }
        } catch (NoSuchAlgorithmException e2) {
            throw new Error(e2);
        }
    }

    private static String descriptorForFieldSignature(String signature) {
        return signature.replace('.', '/');
    }

    private static String descriptorForSignature(String signature) {
        return signature.substring(signature.indexOf("("));
    }

    static Field fieldSerialPersistentFields(Class<?> cl) {
        try {
            Field f = cl.getDeclaredField("serialPersistentFields");
            int modifiers = f.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isPrivate(modifiers) && Modifier.isFinal(modifiers)) {
                if (f.getType() == ARRAY_OF_FIELDS) {
                    return f;
                }
            }
        } catch (NoSuchFieldException e) {
        }
        return null;
    }

    public Class<?> forClass() {
        return this.resolvedClass;
    }

    Object newInstance(Class<?> instantiationClass) throws InvalidClassException {
        resolveConstructorClass(instantiationClass);
        return newInstance(instantiationClass, this.resolvedConstructorMethodId);
    }

    private Class<?> resolveConstructorClass(Class<?> objectClass) throws InvalidClassException {
        if (this.resolvedConstructorClass != null) {
            return this.resolvedConstructorClass;
        }
        Class<?> constructorClass = objectClass;
        boolean wasSerializable = (this.flags & 2) != 0;
        if (wasSerializable) {
            while (constructorClass != null && isSerializable(constructorClass)) {
                constructorClass = constructorClass.getSuperclass();
            }
        }
        Constructor<?> constructor = null;
        if (constructorClass != null) {
            try {
                constructor = constructorClass.getDeclaredConstructor(EmptyArray.CLASS);
            } catch (NoSuchMethodException e) {
            }
        }
        if (constructor == null) {
            String className = constructorClass != null ? constructorClass.getName() : null;
            throw new InvalidClassException(className, "IllegalAccessException");
        }
        int constructorModifiers = constructor.getModifiers();
        boolean isPublic = Modifier.isPublic(constructorModifiers);
        boolean isProtected = Modifier.isProtected(constructorModifiers);
        boolean isPrivate = Modifier.isPrivate(constructorModifiers);
        boolean wasExternalizable = (this.flags & 4) != 0;
        if (isPrivate || (wasExternalizable && !isPublic)) {
            throw new InvalidClassException(constructorClass.getName(), "IllegalAccessException");
        }
        if (!isPublic && !isProtected && !inSamePackage(constructorClass, objectClass)) {
            throw new InvalidClassException(constructorClass.getName(), "IllegalAccessException");
        }
        this.resolvedConstructorClass = constructorClass;
        this.resolvedConstructorMethodId = getConstructorId(this.resolvedConstructorClass);
        return constructorClass;
    }

    private boolean inSamePackage(Class<?> c1, Class<?> c2) {
        String nameC1 = c1.getName();
        String nameC2 = c2.getName();
        int indexDotC1 = nameC1.lastIndexOf(46);
        int indexDotC2 = nameC2.lastIndexOf(46);
        if (indexDotC1 != indexDotC2) {
            return false;
        }
        if (indexDotC1 == -1) {
            return true;
        }
        return nameC1.regionMatches(0, nameC2, 0, indexDotC1);
    }

    public ObjectStreamField getField(String name) {
        ObjectStreamField[] allFields = getFields();
        for (ObjectStreamField f : allFields) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }

    ObjectStreamField[] fields() {
        if (this.fields == null) {
            Class<?> forCl = forClass();
            if (forCl != null && isSerializable() && !forCl.isArray()) {
                buildFieldDescriptors(forCl.getDeclaredFields());
            } else {
                setFields(NO_FIELDS);
            }
        }
        return this.fields;
    }

    public ObjectStreamField[] getFields() {
        copyFieldAttributes();
        return this.loadFields == null ? (ObjectStreamField[]) fields().clone() : (ObjectStreamField[]) this.loadFields.clone();
    }

    List<ObjectStreamClass> getHierarchy() {
        List<ObjectStreamClass> result = this.cachedHierarchy;
        if (result == null) {
            List<ObjectStreamClass> result2 = makeHierarchy();
            this.cachedHierarchy = result2;
            return result2;
        }
        return result;
    }

    private List<ObjectStreamClass> makeHierarchy() {
        ArrayList<ObjectStreamClass> result = new ArrayList<>();
        for (ObjectStreamClass osc = this; osc != null; osc = osc.getSuperclass()) {
            result.add(0, osc);
        }
        return result;
    }

    private void copyFieldAttributes() {
        if (this.loadFields != null && this.fields != null) {
            for (int i = 0; i < this.loadFields.length; i++) {
                ObjectStreamField loadField = this.loadFields[i];
                String name = loadField.getName();
                int j = 0;
                while (true) {
                    if (j < this.fields.length) {
                        ObjectStreamField field = this.fields[j];
                        if (!name.equals(field.getName())) {
                            j++;
                        } else {
                            loadField.setUnshared(field.isUnshared());
                            loadField.setOffset(field.getOffset());
                            break;
                        }
                    }
                }
            }
        }
    }

    ObjectStreamField[] getLoadFields() {
        return this.loadFields;
    }

    byte getFlags() {
        return this.flags;
    }

    public String getName() {
        return this.className;
    }

    public long getSerialVersionUID() {
        return this.svUID;
    }

    ObjectStreamClass getSuperclass() {
        return this.superclass;
    }

    static boolean isExternalizable(Class<?> cl) {
        return EXTERNALIZABLE.isAssignableFrom(cl);
    }

    static boolean isPrimitiveType(char typecode) {
        return (typecode == '[' || typecode == 'L') ? false : true;
    }

    static boolean isSerializable(Class<?> cl) {
        return SERIALIZABLE.isAssignableFrom(cl);
    }

    private void resolveProperties() {
        if (!this.arePropertiesResolved) {
            Class<?> cl = forClass();
            this.isProxy = Proxy.isProxyClass(cl);
            this.isEnum = Enum.class.isAssignableFrom(cl);
            this.isSerializable = isSerializable(cl);
            this.isExternalizable = isExternalizable(cl);
            this.arePropertiesResolved = true;
        }
    }

    boolean isSerializable() {
        resolveProperties();
        return this.isSerializable;
    }

    boolean isExternalizable() {
        resolveProperties();
        return this.isExternalizable;
    }

    boolean isProxy() {
        resolveProperties();
        return this.isProxy;
    }

    boolean isEnum() {
        resolveProperties();
        return this.isEnum;
    }

    public static ObjectStreamClass lookup(Class<?> cl) {
        ObjectStreamClass osc = lookupStreamClass(cl);
        if (osc.isSerializable() || osc.isExternalizable()) {
            return osc;
        }
        return null;
    }

    public static ObjectStreamClass lookupAny(Class<?> cl) {
        return lookupStreamClass(cl);
    }

    static ObjectStreamClass lookupStreamClass(Class<?> cl) {
        WeakHashMap<Class<?>, ObjectStreamClass> tlc = getCache();
        ObjectStreamClass cachedValue = tlc.get(cl);
        if (cachedValue == null) {
            ObjectStreamClass cachedValue2 = createClassDesc(cl);
            tlc.put(cl, cachedValue2);
            return cachedValue2;
        }
        return cachedValue;
    }

    private static WeakHashMap<Class<?>, ObjectStreamClass> getCache() {
        ThreadLocal<WeakHashMap<Class<?>, ObjectStreamClass>> tls = storage.get();
        if (tls == null) {
            tls = new ThreadLocal<WeakHashMap<Class<?>, ObjectStreamClass>>() {
                @Override
                public WeakHashMap<Class<?>, ObjectStreamClass> initialValue() {
                    return new WeakHashMap<>();
                }
            };
            storage = new SoftReference<>(tls);
        }
        return tls.get();
    }

    static Method findMethod(Class<?> cl, String methodName) {
        for (Class<?> search = cl; search != null; search = search.getSuperclass()) {
            try {
                Method method = search.getDeclaredMethod(methodName, (Class[]) null);
                if (search == cl || (method.getModifiers() & 2) == 0) {
                    method.setAccessible(true);
                    return method;
                }
            } catch (NoSuchMethodException e) {
            }
        }
        return null;
    }

    static Method findPrivateMethod(Class<?> cl, String methodName, Class<?>[] param) {
        try {
            Method method = cl.getDeclaredMethod(methodName, param);
            if (Modifier.isPrivate(method.getModifiers()) && method.getReturnType() == Void.TYPE) {
                method.setAccessible(true);
                return method;
            }
        } catch (NoSuchMethodException e) {
        }
        return null;
    }

    boolean hasMethodWriteReplace() {
        return this.methodWriteReplace != null;
    }

    Method getMethodWriteReplace() {
        return this.methodWriteReplace;
    }

    boolean hasMethodReadResolve() {
        return this.methodReadResolve != null;
    }

    Method getMethodReadResolve() {
        return this.methodReadResolve;
    }

    boolean hasMethodWriteObject() {
        return this.methodWriteObject != null;
    }

    Method getMethodWriteObject() {
        return this.methodWriteObject;
    }

    boolean hasMethodReadObject() {
        return this.methodReadObject != null;
    }

    Method getMethodReadObject() {
        return this.methodReadObject;
    }

    boolean hasMethodReadObjectNoData() {
        return this.methodReadObjectNoData != null;
    }

    Method getMethodReadObjectNoData() {
        return this.methodReadObjectNoData;
    }

    void initPrivateFields(ObjectStreamClass desc) {
        this.methodWriteReplace = desc.methodWriteReplace;
        this.methodReadResolve = desc.methodReadResolve;
        this.methodWriteObject = desc.methodWriteObject;
        this.methodReadObject = desc.methodReadObject;
        this.methodReadObjectNoData = desc.methodReadObjectNoData;
    }

    void setClass(Class<?> c) {
        this.resolvedClass = c;
    }

    void setFields(ObjectStreamField[] f) {
        this.fields = f;
    }

    void setLoadFields(ObjectStreamField[] f) {
        this.loadFields = f;
    }

    void setFlags(byte b) {
        this.flags = b;
    }

    void setName(String newName) {
        this.className = newName;
    }

    void setSerialVersionUID(long l) {
        this.svUID = l;
    }

    void setSuperclass(ObjectStreamClass c) {
        this.superclass = c;
    }

    private int primitiveSize(Class<?> type) {
        if (type == Byte.TYPE || type == Boolean.TYPE) {
            return 1;
        }
        if (type == Short.TYPE || type == Character.TYPE) {
            return 2;
        }
        if (type == Integer.TYPE || type == Float.TYPE) {
            return 4;
        }
        if (type == Long.TYPE || type == Double.TYPE) {
            return 8;
        }
        throw new AssertionError();
    }

    public String toString() {
        return getName() + ": static final long serialVersionUID =" + getSerialVersionUID() + "L;";
    }

    public Class<?> checkAndGetTcObjectClass() throws InvalidClassException {
        boolean wasSerializable = (this.flags & 2) != 0;
        boolean wasExternalizable = (this.flags & 4) != 0;
        if (wasSerializable == wasExternalizable) {
            throw new InvalidClassException(getName() + " stream data is corrupt: SC_SERIALIZABLE=" + wasSerializable + " SC_EXTERNALIZABLE=" + wasExternalizable + ", classDescFlags must have one or the other");
        }
        if (isEnum()) {
            throw new InvalidClassException(getName() + " local class is incompatible: Local class is an enum, streamed data is tagged with TC_OBJECT");
        }
        if (!isSerializable()) {
            throw new InvalidClassException(getName() + " local class is incompatible: Not Serializable");
        }
        if (wasExternalizable != isExternalizable()) {
            throw new InvalidClassException(getName() + " local class is incompatible: Local class is Serializable, stream data requires Externalizable");
        }
        return forClass();
    }

    public Class<?> checkAndGetTcEnumClass() throws InvalidClassException {
        if (!isEnum()) {
            throw new InvalidClassException(getName() + " local class is incompatible: Local class is not an enum, streamed data is tagged with TC_ENUM");
        }
        return forClass();
    }
}
