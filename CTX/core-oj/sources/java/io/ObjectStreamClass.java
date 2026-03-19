package java.io;

import dalvik.system.VMRuntime;
import dalvik.system.VMStack;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.misc.Unsafe;
import sun.reflect.CallerSensitive;
import sun.reflect.misc.ReflectUtil;

public class ObjectStreamClass implements Serializable {
    static final int MAX_SDK_TARGET_FOR_CLINIT_UIDGEN_WORKAROUND = 23;
    public static final ObjectStreamField[] NO_FIELDS = new ObjectStreamField[0];
    private static final ObjectStreamField[] serialPersistentFields = NO_FIELDS;
    private static final long serialVersionUID = -6120832682080437368L;
    private Class<?> cl;
    private Constructor<?> cons;
    private volatile ClassDataSlot[] dataLayout;
    private ExceptionInfo defaultSerializeEx;
    private ExceptionInfo deserializeEx;
    private boolean externalizable;
    private FieldReflector fieldRefl;
    private ObjectStreamField[] fields;
    private boolean hasBlockExternalData = true;
    private boolean hasWriteObjectData;
    private boolean initialized;
    private boolean isEnum;
    private boolean isProxy;
    private ObjectStreamClass localDesc;
    private String name;
    private int numObjFields;
    private int primDataSize;
    private Method readObjectMethod;
    private Method readObjectNoDataMethod;
    private Method readResolveMethod;
    private ClassNotFoundException resolveEx;
    private boolean serializable;
    private ExceptionInfo serializeEx;
    private volatile Long suid;
    private ObjectStreamClass superDesc;
    private Method writeObjectMethod;
    private Method writeReplaceMethod;

    private static native boolean hasStaticInitializer(Class<?> cls, boolean z);

    private static class Caches {
        static final ConcurrentMap<WeakClassKey, Reference<?>> localDescs = new ConcurrentHashMap();
        static final ConcurrentMap<FieldReflectorKey, Reference<?>> reflectors = new ConcurrentHashMap();
        private static final ReferenceQueue<Class<?>> localDescsQueue = new ReferenceQueue<>();
        private static final ReferenceQueue<Class<?>> reflectorsQueue = new ReferenceQueue<>();

        private Caches() {
        }
    }

    private static class ExceptionInfo {
        private final String className;
        private final String message;

        ExceptionInfo(String str, String str2) {
            this.className = str;
            this.message = str2;
        }

        InvalidClassException newInvalidClassException() {
            return new InvalidClassException(this.className, this.message);
        }
    }

    public static ObjectStreamClass lookup(Class<?> cls) {
        return lookup(cls, false);
    }

    public static ObjectStreamClass lookupAny(Class<?> cls) {
        return lookup(cls, true);
    }

    public String getName() {
        return this.name;
    }

    public long getSerialVersionUID() {
        if (this.suid == null) {
            this.suid = (Long) AccessController.doPrivileged(new PrivilegedAction<Long>() {
                @Override
                public Long run() {
                    return Long.valueOf(ObjectStreamClass.computeDefaultSUID(ObjectStreamClass.this.cl));
                }
            });
        }
        return this.suid.longValue();
    }

    @CallerSensitive
    public Class<?> forClass() {
        if (this.cl == null) {
            return null;
        }
        requireInitialized();
        if (System.getSecurityManager() != null && ReflectUtil.needsPackageAccessCheck(VMStack.getCallingClassLoader(), this.cl.getClassLoader())) {
            ReflectUtil.checkPackageAccess(this.cl);
        }
        return this.cl;
    }

    public ObjectStreamField[] getFields() {
        return getFields(true);
    }

    public ObjectStreamField getField(String str) {
        return getField(str, null);
    }

    public String toString() {
        return this.name + ": static final long serialVersionUID = " + getSerialVersionUID() + "L;";
    }

    static ObjectStreamClass lookup(Class<?> cls, boolean z) {
        Object obj;
        EntryFuture entryFuture;
        Object objectStreamClass = null;
        Object[] objArr = 0;
        if (z || Serializable.class.isAssignableFrom(cls)) {
            processQueue(Caches.localDescsQueue, Caches.localDescs);
            WeakClassKey weakClassKey = new WeakClassKey(cls, Caches.localDescsQueue);
            Reference<?> referencePutIfAbsent = Caches.localDescs.get(weakClassKey);
            if (referencePutIfAbsent != null) {
                obj = referencePutIfAbsent.get();
            } else {
                obj = null;
            }
            if (obj == null) {
                entryFuture = new EntryFuture();
                SoftReference softReference = new SoftReference(entryFuture);
                do {
                    if (referencePutIfAbsent != null) {
                        Caches.localDescs.remove(weakClassKey, referencePutIfAbsent);
                    }
                    referencePutIfAbsent = Caches.localDescs.putIfAbsent(weakClassKey, softReference);
                    if (referencePutIfAbsent != null) {
                        obj = referencePutIfAbsent.get();
                    }
                    if (referencePutIfAbsent == null) {
                        break;
                    }
                } while (obj == null);
                if (obj != null) {
                    entryFuture = null;
                }
            }
            if (obj instanceof ObjectStreamClass) {
                return (ObjectStreamClass) obj;
            }
            if (obj instanceof EntryFuture) {
                entryFuture = (EntryFuture) obj;
                if (entryFuture.getOwner() != Thread.currentThread()) {
                    objectStreamClass = entryFuture.get();
                }
            } else {
                objectStreamClass = obj;
            }
            if (objectStreamClass == null) {
                try {
                    objectStreamClass = new ObjectStreamClass(cls);
                } catch (Throwable th) {
                    objectStreamClass = th;
                }
                if (entryFuture.set(objectStreamClass)) {
                    Caches.localDescs.put(weakClassKey, new SoftReference(objectStreamClass));
                } else {
                    objectStreamClass = entryFuture.get();
                }
            }
            if (objectStreamClass instanceof ObjectStreamClass) {
                return (ObjectStreamClass) objectStreamClass;
            }
            if (objectStreamClass instanceof RuntimeException) {
                throw ((RuntimeException) objectStreamClass);
            }
            if (objectStreamClass instanceof Error) {
                throw ((Error) objectStreamClass);
            }
            throw new InternalError("unexpected entry: " + objectStreamClass);
        }
        return null;
    }

    private static class EntryFuture {
        private static final Object unset = new Object();
        private Object entry;
        private final Thread owner;

        private EntryFuture() {
            this.owner = Thread.currentThread();
            this.entry = unset;
        }

        synchronized boolean set(Object obj) {
            if (this.entry != unset) {
                return false;
            }
            this.entry = obj;
            notifyAll();
            return true;
        }

        synchronized Object get() {
            boolean z = false;
            while (this.entry == unset) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    z = true;
                }
            }
            if (z) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                });
            }
            return this.entry;
        }

        Thread getOwner() {
            return this.owner;
        }
    }

    private ObjectStreamClass(final Class<?> cls) {
        this.cl = cls;
        this.name = cls.getName();
        this.isProxy = Proxy.isProxyClass(cls);
        this.isEnum = Enum.class.isAssignableFrom(cls);
        this.serializable = Serializable.class.isAssignableFrom(cls);
        this.externalizable = Externalizable.class.isAssignableFrom(cls);
        Class<? super Object> superclass = cls.getSuperclass();
        this.superDesc = superclass != null ? lookup(superclass, false) : null;
        this.localDesc = this;
        if (this.serializable) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    if (ObjectStreamClass.this.isEnum) {
                        ObjectStreamClass.this.suid = 0L;
                        ObjectStreamClass.this.fields = ObjectStreamClass.NO_FIELDS;
                        return null;
                    }
                    if (cls.isArray()) {
                        ObjectStreamClass.this.fields = ObjectStreamClass.NO_FIELDS;
                        return null;
                    }
                    ObjectStreamClass.this.suid = ObjectStreamClass.getDeclaredSUID(cls);
                    try {
                        ObjectStreamClass.this.fields = ObjectStreamClass.getSerialFields(cls);
                        ObjectStreamClass.this.computeFieldOffsets();
                    } catch (InvalidClassException e) {
                        ObjectStreamClass.this.serializeEx = ObjectStreamClass.this.deserializeEx = new ExceptionInfo(e.classname, e.getMessage());
                        ObjectStreamClass.this.fields = ObjectStreamClass.NO_FIELDS;
                    }
                    if (ObjectStreamClass.this.externalizable) {
                        ObjectStreamClass.this.cons = ObjectStreamClass.getExternalizableConstructor(cls);
                    } else {
                        ObjectStreamClass.this.cons = ObjectStreamClass.getSerializableConstructor(cls);
                        ObjectStreamClass.this.writeObjectMethod = ObjectStreamClass.getPrivateMethod(cls, "writeObject", new Class[]{ObjectOutputStream.class}, Void.TYPE);
                        ObjectStreamClass.this.readObjectMethod = ObjectStreamClass.getPrivateMethod(cls, "readObject", new Class[]{ObjectInputStream.class}, Void.TYPE);
                        ObjectStreamClass.this.readObjectNoDataMethod = ObjectStreamClass.getPrivateMethod(cls, "readObjectNoData", null, Void.TYPE);
                        ObjectStreamClass.this.hasWriteObjectData = ObjectStreamClass.this.writeObjectMethod != null;
                    }
                    ObjectStreamClass.this.writeReplaceMethod = ObjectStreamClass.getInheritableMethod(cls, "writeReplace", null, Object.class);
                    ObjectStreamClass.this.readResolveMethod = ObjectStreamClass.getInheritableMethod(cls, "readResolve", null, Object.class);
                    return null;
                }
            });
        } else {
            this.suid = 0L;
            this.fields = NO_FIELDS;
        }
        try {
            this.fieldRefl = getReflector(this.fields, this);
            if (this.deserializeEx == null) {
                if (this.isEnum) {
                    this.deserializeEx = new ExceptionInfo(this.name, "enum type");
                } else if (this.cons == null) {
                    this.deserializeEx = new ExceptionInfo(this.name, "no valid constructor");
                }
            }
            for (int i = 0; i < this.fields.length; i++) {
                if (this.fields[i].getField() == null) {
                    this.defaultSerializeEx = new ExceptionInfo(this.name, "unmatched serializable field(s) declared");
                }
            }
            this.initialized = true;
        } catch (InvalidClassException e) {
            throw new InternalError(e);
        }
    }

    ObjectStreamClass() {
    }

    void initProxy(Class<?> cls, ClassNotFoundException classNotFoundException, ObjectStreamClass objectStreamClass) throws InvalidClassException {
        ObjectStreamClass objectStreamClassLookup;
        if (cls != null) {
            objectStreamClassLookup = lookup(cls, true);
            if (!objectStreamClassLookup.isProxy) {
                throw new InvalidClassException("cannot bind proxy descriptor to a non-proxy class");
            }
        } else {
            objectStreamClassLookup = null;
        }
        this.cl = cls;
        this.resolveEx = classNotFoundException;
        this.superDesc = objectStreamClass;
        this.isProxy = true;
        this.serializable = true;
        this.suid = 0L;
        this.fields = NO_FIELDS;
        if (objectStreamClassLookup != null) {
            this.localDesc = objectStreamClassLookup;
            this.name = this.localDesc.name;
            this.externalizable = this.localDesc.externalizable;
            this.writeReplaceMethod = this.localDesc.writeReplaceMethod;
            this.readResolveMethod = this.localDesc.readResolveMethod;
            this.deserializeEx = this.localDesc.deserializeEx;
            this.cons = this.localDesc.cons;
        }
        this.fieldRefl = getReflector(this.fields, this.localDesc);
        this.initialized = true;
    }

    void initNonProxy(ObjectStreamClass objectStreamClass, Class<?> cls, ClassNotFoundException classNotFoundException, ObjectStreamClass objectStreamClass2) throws InvalidClassException {
        ObjectStreamClass objectStreamClassLookup;
        long jLongValue = Long.valueOf(objectStreamClass.getSerialVersionUID()).longValue();
        if (cls != null) {
            objectStreamClassLookup = lookup(cls, true);
            if (objectStreamClassLookup.isProxy) {
                throw new InvalidClassException("cannot bind non-proxy descriptor to a proxy class");
            }
            if (objectStreamClass.isEnum != objectStreamClassLookup.isEnum) {
                throw new InvalidClassException(objectStreamClass.isEnum ? "cannot bind enum descriptor to a non-enum class" : "cannot bind non-enum descriptor to an enum class");
            }
            if (objectStreamClass.serializable == objectStreamClassLookup.serializable && !cls.isArray() && jLongValue != objectStreamClassLookup.getSerialVersionUID()) {
                throw new InvalidClassException(objectStreamClassLookup.name, "local class incompatible: stream classdesc serialVersionUID = " + jLongValue + ", local class serialVersionUID = " + objectStreamClassLookup.getSerialVersionUID());
            }
            if (!classNamesEqual(objectStreamClass.name, objectStreamClassLookup.name)) {
                throw new InvalidClassException(objectStreamClassLookup.name, "local class name incompatible with stream class name \"" + objectStreamClass.name + "\"");
            }
            if (!objectStreamClass.isEnum) {
                if (objectStreamClass.serializable == objectStreamClassLookup.serializable && objectStreamClass.externalizable != objectStreamClassLookup.externalizable) {
                    throw new InvalidClassException(objectStreamClassLookup.name, "Serializable incompatible with Externalizable");
                }
                if (objectStreamClass.serializable != objectStreamClassLookup.serializable || objectStreamClass.externalizable != objectStreamClassLookup.externalizable || (!objectStreamClass.serializable && !objectStreamClass.externalizable)) {
                    this.deserializeEx = new ExceptionInfo(objectStreamClassLookup.name, "class invalid for deserialization");
                }
            }
        } else {
            objectStreamClassLookup = null;
        }
        this.cl = cls;
        this.resolveEx = classNotFoundException;
        this.superDesc = objectStreamClass2;
        this.name = objectStreamClass.name;
        this.suid = Long.valueOf(jLongValue);
        this.isProxy = false;
        this.isEnum = objectStreamClass.isEnum;
        this.serializable = objectStreamClass.serializable;
        this.externalizable = objectStreamClass.externalizable;
        this.hasBlockExternalData = objectStreamClass.hasBlockExternalData;
        this.hasWriteObjectData = objectStreamClass.hasWriteObjectData;
        this.fields = objectStreamClass.fields;
        this.primDataSize = objectStreamClass.primDataSize;
        this.numObjFields = objectStreamClass.numObjFields;
        if (objectStreamClassLookup != null) {
            this.localDesc = objectStreamClassLookup;
            this.writeObjectMethod = this.localDesc.writeObjectMethod;
            this.readObjectMethod = this.localDesc.readObjectMethod;
            this.readObjectNoDataMethod = this.localDesc.readObjectNoDataMethod;
            this.writeReplaceMethod = this.localDesc.writeReplaceMethod;
            this.readResolveMethod = this.localDesc.readResolveMethod;
            if (this.deserializeEx == null) {
                this.deserializeEx = this.localDesc.deserializeEx;
            }
            this.cons = this.localDesc.cons;
        }
        this.fieldRefl = getReflector(this.fields, this.localDesc);
        this.fields = this.fieldRefl.getFields();
        this.initialized = true;
    }

    void readNonProxy(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        this.name = objectInputStream.readUTF();
        this.suid = Long.valueOf(objectInputStream.readLong());
        this.isProxy = false;
        byte b = objectInputStream.readByte();
        this.hasWriteObjectData = (b & 1) != 0;
        this.hasBlockExternalData = (b & 8) != 0;
        this.externalizable = (b & 4) != 0;
        boolean z = (b & 2) != 0;
        if (this.externalizable && z) {
            throw new InvalidClassException(this.name, "serializable and externalizable flags conflict");
        }
        this.serializable = this.externalizable || z;
        this.isEnum = (b & 16) != 0;
        if (this.isEnum && this.suid.longValue() != 0) {
            throw new InvalidClassException(this.name, "enum descriptor has non-zero serialVersionUID: " + ((Object) this.suid));
        }
        int i = objectInputStream.readShort();
        if (this.isEnum && i != 0) {
            throw new InvalidClassException(this.name, "enum descriptor has non-zero field count: " + i);
        }
        this.fields = i > 0 ? new ObjectStreamField[i] : NO_FIELDS;
        for (int i2 = 0; i2 < i; i2++) {
            char c = (char) objectInputStream.readByte();
            String utf = objectInputStream.readUTF();
            try {
                this.fields[i2] = new ObjectStreamField(utf, (c == 'L' || c == '[') ? objectInputStream.readTypeString() : new String(new char[]{c}), false);
            } catch (RuntimeException e) {
                throw ((IOException) new InvalidClassException(this.name, "invalid descriptor for field " + utf).initCause(e));
            }
        }
        computeFieldOffsets();
    }

    void writeNonProxy(ObjectOutputStream objectOutputStream) throws IOException {
        byte b;
        objectOutputStream.writeUTF(this.name);
        objectOutputStream.writeLong(getSerialVersionUID());
        if (this.externalizable) {
            b = (byte) 4;
            if (objectOutputStream.getProtocolVersion() != 1) {
                b = (byte) (b | 8);
            }
        } else if (this.serializable) {
            b = (byte) 2;
        } else {
            b = 0;
        }
        if (this.hasWriteObjectData) {
            b = (byte) (b | 1);
        }
        if (this.isEnum) {
            b = (byte) (b | 16);
        }
        objectOutputStream.writeByte(b);
        objectOutputStream.writeShort(this.fields.length);
        for (int i = 0; i < this.fields.length; i++) {
            ObjectStreamField objectStreamField = this.fields[i];
            objectOutputStream.writeByte(objectStreamField.getTypeCode());
            objectOutputStream.writeUTF(objectStreamField.getName());
            if (!objectStreamField.isPrimitive()) {
                objectOutputStream.writeTypeString(objectStreamField.getTypeString());
            }
        }
    }

    ClassNotFoundException getResolveException() {
        return this.resolveEx;
    }

    private final void requireInitialized() {
        if (!this.initialized) {
            throw new InternalError("Unexpected call when not initialized");
        }
    }

    void checkDeserialize() throws InvalidClassException {
        requireInitialized();
        if (this.deserializeEx != null) {
            throw this.deserializeEx.newInvalidClassException();
        }
    }

    void checkSerialize() throws InvalidClassException {
        requireInitialized();
        if (this.serializeEx != null) {
            throw this.serializeEx.newInvalidClassException();
        }
    }

    void checkDefaultSerialize() throws InvalidClassException {
        requireInitialized();
        if (this.defaultSerializeEx != null) {
            throw this.defaultSerializeEx.newInvalidClassException();
        }
    }

    ObjectStreamClass getSuperDesc() {
        requireInitialized();
        return this.superDesc;
    }

    ObjectStreamClass getLocalDesc() {
        requireInitialized();
        return this.localDesc;
    }

    ObjectStreamField[] getFields(boolean z) {
        return z ? (ObjectStreamField[]) this.fields.clone() : this.fields;
    }

    ObjectStreamField getField(String str, Class<?> cls) {
        for (int i = 0; i < this.fields.length; i++) {
            ObjectStreamField objectStreamField = this.fields[i];
            if (objectStreamField.getName().equals(str)) {
                if (cls == null || (cls == Object.class && !objectStreamField.isPrimitive())) {
                    return objectStreamField;
                }
                Class<?> type = objectStreamField.getType();
                if (type != null && cls.isAssignableFrom(type)) {
                    return objectStreamField;
                }
            }
        }
        return null;
    }

    boolean isProxy() {
        requireInitialized();
        return this.isProxy;
    }

    boolean isEnum() {
        requireInitialized();
        return this.isEnum;
    }

    boolean isExternalizable() {
        requireInitialized();
        return this.externalizable;
    }

    boolean isSerializable() {
        requireInitialized();
        return this.serializable;
    }

    boolean hasBlockExternalData() {
        requireInitialized();
        return this.hasBlockExternalData;
    }

    boolean hasWriteObjectData() {
        requireInitialized();
        return this.hasWriteObjectData;
    }

    boolean isInstantiable() {
        requireInitialized();
        return this.cons != null;
    }

    boolean hasWriteObjectMethod() {
        requireInitialized();
        return this.writeObjectMethod != null;
    }

    boolean hasReadObjectMethod() {
        requireInitialized();
        return this.readObjectMethod != null;
    }

    boolean hasReadObjectNoDataMethod() {
        requireInitialized();
        return this.readObjectNoDataMethod != null;
    }

    boolean hasWriteReplaceMethod() {
        requireInitialized();
        return this.writeReplaceMethod != null;
    }

    boolean hasReadResolveMethod() {
        requireInitialized();
        return this.readResolveMethod != null;
    }

    Object newInstance() throws UnsupportedOperationException, InstantiationException, InvocationTargetException {
        requireInitialized();
        if (this.cons != null) {
            try {
                return this.cons.newInstance(new Object[0]);
            } catch (IllegalAccessException e) {
                throw new InternalError(e);
            }
        }
        throw new UnsupportedOperationException();
    }

    void invokeWriteObject(Object obj, ObjectOutputStream objectOutputStream) throws UnsupportedOperationException, IOException {
        requireInitialized();
        if (this.writeObjectMethod != null) {
            try {
                this.writeObjectMethod.invoke(obj, objectOutputStream);
                return;
            } catch (IllegalAccessException e) {
                throw new InternalError(e);
            } catch (InvocationTargetException e2) {
                Throwable targetException = e2.getTargetException();
                if (targetException instanceof IOException) {
                    throw ((IOException) targetException);
                }
                throwMiscException(targetException);
                return;
            }
        }
        throw new UnsupportedOperationException();
    }

    void invokeReadObject(Object obj, ObjectInputStream objectInputStream) throws UnsupportedOperationException, IOException, ClassNotFoundException {
        requireInitialized();
        if (this.readObjectMethod != null) {
            try {
                this.readObjectMethod.invoke(obj, objectInputStream);
                return;
            } catch (IllegalAccessException e) {
                throw new InternalError(e);
            } catch (InvocationTargetException e2) {
                Throwable targetException = e2.getTargetException();
                if (targetException instanceof ClassNotFoundException) {
                    throw ((ClassNotFoundException) targetException);
                }
                if (targetException instanceof IOException) {
                    throw ((IOException) targetException);
                }
                throwMiscException(targetException);
                return;
            }
        }
        throw new UnsupportedOperationException();
    }

    void invokeReadObjectNoData(Object obj) throws UnsupportedOperationException, IOException {
        requireInitialized();
        if (this.readObjectNoDataMethod != null) {
            try {
                this.readObjectNoDataMethod.invoke(obj, (Object[]) null);
                return;
            } catch (IllegalAccessException e) {
                throw new InternalError(e);
            } catch (InvocationTargetException e2) {
                Throwable targetException = e2.getTargetException();
                if (targetException instanceof ObjectStreamException) {
                    throw ((ObjectStreamException) targetException);
                }
                throwMiscException(targetException);
                return;
            }
        }
        throw new UnsupportedOperationException();
    }

    Object invokeWriteReplace(Object obj) throws UnsupportedOperationException, IOException {
        requireInitialized();
        if (this.writeReplaceMethod != null) {
            try {
                return this.writeReplaceMethod.invoke(obj, (Object[]) null);
            } catch (IllegalAccessException e) {
                throw new InternalError(e);
            } catch (InvocationTargetException e2) {
                Throwable targetException = e2.getTargetException();
                if (targetException instanceof ObjectStreamException) {
                    throw ((ObjectStreamException) targetException);
                }
                throwMiscException(targetException);
                throw new InternalError(targetException);
            }
        }
        throw new UnsupportedOperationException();
    }

    Object invokeReadResolve(Object obj) throws UnsupportedOperationException, IOException {
        requireInitialized();
        if (this.readResolveMethod != null) {
            try {
                return this.readResolveMethod.invoke(obj, (Object[]) null);
            } catch (IllegalAccessException e) {
                throw new InternalError(e);
            } catch (InvocationTargetException e2) {
                Throwable targetException = e2.getTargetException();
                if (targetException instanceof ObjectStreamException) {
                    throw ((ObjectStreamException) targetException);
                }
                throwMiscException(targetException);
                throw new InternalError(targetException);
            }
        }
        throw new UnsupportedOperationException();
    }

    static class ClassDataSlot {
        final ObjectStreamClass desc;
        final boolean hasData;

        ClassDataSlot(ObjectStreamClass objectStreamClass, boolean z) {
            this.desc = objectStreamClass;
            this.hasData = z;
        }
    }

    ClassDataSlot[] getClassDataLayout() throws InvalidClassException {
        if (this.dataLayout == null) {
            this.dataLayout = getClassDataLayout0();
        }
        return this.dataLayout;
    }

    private ClassDataSlot[] getClassDataLayout0() throws InvalidClassException {
        ArrayList arrayList = new ArrayList();
        Class<?> cls = this.cl;
        Class<?> superclass = this.cl;
        while (superclass != null && Serializable.class.isAssignableFrom(superclass)) {
            superclass = superclass.getSuperclass();
        }
        HashSet hashSet = new HashSet(3);
        Class<?> superclass2 = cls;
        for (ObjectStreamClass objectStreamClass = this; objectStreamClass != null; objectStreamClass = objectStreamClass.superDesc) {
            if (hashSet.contains(objectStreamClass.name)) {
                throw new InvalidClassException("Circular reference.");
            }
            hashSet.add(objectStreamClass.name);
            String name = objectStreamClass.cl != null ? objectStreamClass.cl.getName() : objectStreamClass.name;
            Class<?> cls2 = null;
            Class<?> superclass3 = superclass2;
            while (true) {
                if (superclass3 == superclass) {
                    break;
                }
                if (!name.equals(superclass3.getName())) {
                    superclass3 = superclass3.getSuperclass();
                } else {
                    cls2 = superclass3;
                    break;
                }
            }
            if (cls2 != null) {
                while (superclass2 != cls2) {
                    arrayList.add(new ClassDataSlot(lookup(superclass2, true), false));
                    superclass2 = superclass2.getSuperclass();
                }
                superclass2 = cls2.getSuperclass();
            }
            arrayList.add(new ClassDataSlot(objectStreamClass.getVariantFor(cls2), true));
        }
        while (superclass2 != superclass) {
            arrayList.add(new ClassDataSlot(lookup(superclass2, true), false));
            superclass2 = superclass2.getSuperclass();
        }
        Collections.reverse(arrayList);
        return (ClassDataSlot[]) arrayList.toArray(new ClassDataSlot[arrayList.size()]);
    }

    int getPrimDataSize() {
        return this.primDataSize;
    }

    int getNumObjFields() {
        return this.numObjFields;
    }

    void getPrimFieldValues(Object obj, byte[] bArr) {
        this.fieldRefl.getPrimFieldValues(obj, bArr);
    }

    void setPrimFieldValues(Object obj, byte[] bArr) {
        this.fieldRefl.setPrimFieldValues(obj, bArr);
    }

    void getObjFieldValues(Object obj, Object[] objArr) {
        this.fieldRefl.getObjFieldValues(obj, objArr);
    }

    void setObjFieldValues(Object obj, Object[] objArr) {
        this.fieldRefl.setObjFieldValues(obj, objArr);
    }

    private void computeFieldOffsets() throws InvalidClassException {
        this.primDataSize = 0;
        this.numObjFields = 0;
        int i = -1;
        for (int i2 = 0; i2 < this.fields.length; i2++) {
            ObjectStreamField objectStreamField = this.fields[i2];
            switch (objectStreamField.getTypeCode()) {
                case 'B':
                case 'Z':
                    int i3 = this.primDataSize;
                    this.primDataSize = i3 + 1;
                    objectStreamField.setOffset(i3);
                    break;
                case 'C':
                case 'S':
                    objectStreamField.setOffset(this.primDataSize);
                    this.primDataSize += 2;
                    break;
                case 'D':
                case 'J':
                    objectStreamField.setOffset(this.primDataSize);
                    this.primDataSize += 8;
                    break;
                case Types.DATALINK:
                case 'I':
                    objectStreamField.setOffset(this.primDataSize);
                    this.primDataSize += 4;
                    break;
                case 'L':
                case Types.DATE:
                    int i4 = this.numObjFields;
                    this.numObjFields = i4 + 1;
                    objectStreamField.setOffset(i4);
                    if (i == -1) {
                        i = i2;
                    }
                    break;
                default:
                    throw new InternalError();
            }
        }
        if (i != -1 && i + this.numObjFields != this.fields.length) {
            throw new InvalidClassException(this.name, "illegal field order");
        }
    }

    private ObjectStreamClass getVariantFor(Class<?> cls) throws InvalidClassException {
        if (this.cl == cls) {
            return this;
        }
        ObjectStreamClass objectStreamClass = new ObjectStreamClass();
        if (this.isProxy) {
            objectStreamClass.initProxy(cls, null, this.superDesc);
        } else {
            objectStreamClass.initNonProxy(this, cls, null, this.superDesc);
        }
        return objectStreamClass;
    }

    private static Constructor<?> getExternalizableConstructor(Class<?> cls) {
        try {
            Constructor<?> declaredConstructor = cls.getDeclaredConstructor((Class[]) null);
            declaredConstructor.setAccessible(true);
            if ((1 & declaredConstructor.getModifiers()) != 0) {
                return declaredConstructor;
            }
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Constructor<?> getSerializableConstructor(Class<?> cls) {
        Class<?> superclass = cls;
        while (Serializable.class.isAssignableFrom(superclass)) {
            superclass = superclass.getSuperclass();
            if (superclass == null) {
                return null;
            }
        }
        try {
            Constructor<?> declaredConstructor = superclass.getDeclaredConstructor((Class[]) null);
            int modifiers = declaredConstructor.getModifiers();
            if ((modifiers & 2) == 0 && ((modifiers & 5) != 0 || packageEquals(cls, superclass))) {
                if (declaredConstructor.getDeclaringClass() != cls) {
                    declaredConstructor = declaredConstructor.serializationCopy(declaredConstructor.getDeclaringClass(), cls);
                }
                declaredConstructor.setAccessible(true);
                return declaredConstructor;
            }
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method getInheritableMethod(Class<?> cls, String str, Class<?>[] clsArr, Class<?> cls2) {
        Method declaredMethod;
        Class<?> superclass = cls;
        while (true) {
            if (superclass != null) {
                try {
                    declaredMethod = superclass.getDeclaredMethod(str, clsArr);
                    break;
                } catch (NoSuchMethodException e) {
                    superclass = superclass.getSuperclass();
                }
            } else {
                declaredMethod = null;
                break;
            }
        }
        if (declaredMethod == null || declaredMethod.getReturnType() != cls2) {
            return null;
        }
        declaredMethod.setAccessible(true);
        int modifiers = declaredMethod.getModifiers();
        if ((modifiers & 1032) != 0) {
            return null;
        }
        if ((modifiers & 5) != 0) {
            return declaredMethod;
        }
        if ((modifiers & 2) != 0) {
            if (cls == superclass) {
                return declaredMethod;
            }
            return null;
        }
        if (packageEquals(cls, superclass)) {
            return declaredMethod;
        }
        return null;
    }

    private static Method getPrivateMethod(Class<?> cls, String str, Class<?>[] clsArr, Class<?> cls2) {
        try {
            Method declaredMethod = cls.getDeclaredMethod(str, clsArr);
            declaredMethod.setAccessible(true);
            int modifiers = declaredMethod.getModifiers();
            if (declaredMethod.getReturnType() == cls2 && (modifiers & 8) == 0 && (modifiers & 2) != 0) {
                return declaredMethod;
            }
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static boolean packageEquals(Class<?> cls, Class<?> cls2) {
        return cls.getClassLoader() == cls2.getClassLoader() && getPackageName(cls).equals(getPackageName(cls2));
    }

    private static String getPackageName(Class<?> cls) {
        String name = cls.getName();
        int iLastIndexOf = name.lastIndexOf(91);
        if (iLastIndexOf >= 0) {
            name = name.substring(iLastIndexOf + 2);
        }
        int iLastIndexOf2 = name.lastIndexOf(46);
        return iLastIndexOf2 >= 0 ? name.substring(0, iLastIndexOf2) : "";
    }

    private static boolean classNamesEqual(String str, String str2) {
        return str.substring(str.lastIndexOf(46) + 1).equals(str2.substring(str2.lastIndexOf(46) + 1));
    }

    private static String getClassSignature(Class<?> cls) {
        StringBuilder sb = new StringBuilder();
        while (cls.isArray()) {
            sb.append('[');
            cls = cls.getComponentType();
        }
        if (cls.isPrimitive()) {
            if (cls == Integer.TYPE) {
                sb.append('I');
            } else if (cls == Byte.TYPE) {
                sb.append('B');
            } else if (cls == Long.TYPE) {
                sb.append('J');
            } else if (cls == Float.TYPE) {
                sb.append('F');
            } else if (cls == Double.TYPE) {
                sb.append('D');
            } else if (cls == Short.TYPE) {
                sb.append('S');
            } else if (cls == Character.TYPE) {
                sb.append('C');
            } else if (cls == Boolean.TYPE) {
                sb.append('Z');
            } else if (cls == Void.TYPE) {
                sb.append('V');
            } else {
                throw new InternalError();
            }
        } else {
            sb.append('L' + cls.getName().replace('.', '/') + ';');
        }
        return sb.toString();
    }

    private static String getMethodSignature(Class<?>[] clsArr, Class<?> cls) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (Class<?> cls2 : clsArr) {
            sb.append(getClassSignature(cls2));
        }
        sb.append(')');
        sb.append(getClassSignature(cls));
        return sb.toString();
    }

    private static void throwMiscException(Throwable th) throws IOException {
        if (th instanceof RuntimeException) {
            throw ((RuntimeException) th);
        }
        if (th instanceof Error) {
            throw ((Error) th);
        }
        IOException iOException = new IOException("unexpected exception type");
        iOException.initCause(th);
        throw iOException;
    }

    private static ObjectStreamField[] getSerialFields(Class<?> cls) throws InvalidClassException {
        if (Serializable.class.isAssignableFrom(cls) && !Externalizable.class.isAssignableFrom(cls) && !Proxy.isProxyClass(cls) && !cls.isInterface()) {
            ObjectStreamField[] declaredSerialFields = getDeclaredSerialFields(cls);
            if (declaredSerialFields == null) {
                declaredSerialFields = getDefaultSerialFields(cls);
            }
            Arrays.sort(declaredSerialFields);
            return declaredSerialFields;
        }
        return NO_FIELDS;
    }

    private static ObjectStreamField[] getDeclaredSerialFields(Class<?> cls) throws InvalidClassException {
        ObjectStreamField[] objectStreamFieldArr;
        try {
            Field declaredField = cls.getDeclaredField("serialPersistentFields");
            if ((declaredField.getModifiers() & 26) == 26) {
                declaredField.setAccessible(true);
                objectStreamFieldArr = (ObjectStreamField[]) declaredField.get(null);
            } else {
                objectStreamFieldArr = null;
            }
        } catch (Exception e) {
            objectStreamFieldArr = null;
        }
        if (objectStreamFieldArr == null) {
            return null;
        }
        if (objectStreamFieldArr.length == 0) {
            return NO_FIELDS;
        }
        ObjectStreamField[] objectStreamFieldArr2 = new ObjectStreamField[objectStreamFieldArr.length];
        HashSet hashSet = new HashSet(objectStreamFieldArr.length);
        for (int i = 0; i < objectStreamFieldArr.length; i++) {
            ObjectStreamField objectStreamField = objectStreamFieldArr[i];
            String name = objectStreamField.getName();
            if (hashSet.contains(name)) {
                throw new InvalidClassException("multiple serializable fields named " + name);
            }
            hashSet.add(name);
            try {
                Field declaredField2 = cls.getDeclaredField(name);
                if (declaredField2.getType() == objectStreamField.getType() && (declaredField2.getModifiers() & 8) == 0) {
                    objectStreamFieldArr2[i] = new ObjectStreamField(declaredField2, objectStreamField.isUnshared(), true);
                }
            } catch (NoSuchFieldException e2) {
            }
            if (objectStreamFieldArr2[i] == null) {
                objectStreamFieldArr2[i] = new ObjectStreamField(name, objectStreamField.getType(), objectStreamField.isUnshared());
            }
        }
        return objectStreamFieldArr2;
    }

    private static ObjectStreamField[] getDefaultSerialFields(Class<?> cls) {
        Field[] declaredFields = cls.getDeclaredFields();
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < declaredFields.length; i++) {
            if ((declaredFields[i].getModifiers() & 136) == 0) {
                arrayList.add(new ObjectStreamField(declaredFields[i], false, true));
            }
        }
        int size = arrayList.size();
        return size == 0 ? NO_FIELDS : (ObjectStreamField[]) arrayList.toArray(new ObjectStreamField[size]);
    }

    private static Long getDeclaredSUID(Class<?> cls) {
        try {
            Field declaredField = cls.getDeclaredField("serialVersionUID");
            if ((declaredField.getModifiers() & 24) == 24) {
                declaredField.setAccessible(true);
                return Long.valueOf(declaredField.getLong(null));
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static long computeDefaultSUID(Class<?> cls) {
        long j = 0;
        if (!Serializable.class.isAssignableFrom(cls) || Proxy.isProxyClass(cls)) {
            return 0L;
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeUTF(cls.getName());
            int modifiers = cls.getModifiers() & 1553;
            Method[] declaredMethods = cls.getDeclaredMethods();
            if ((modifiers & 512) != 0) {
                if (declaredMethods.length > 0) {
                    modifiers |= 1024;
                } else {
                    modifiers &= -1025;
                }
            }
            dataOutputStream.writeInt(modifiers);
            if (!cls.isArray()) {
                Class<?>[] interfaces = cls.getInterfaces();
                String[] strArr = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    strArr[i] = interfaces[i].getName();
                }
                Arrays.sort(strArr);
                for (String str : strArr) {
                    dataOutputStream.writeUTF(str);
                }
            }
            Field[] declaredFields = cls.getDeclaredFields();
            MemberSignature[] memberSignatureArr = new MemberSignature[declaredFields.length];
            for (int i2 = 0; i2 < declaredFields.length; i2++) {
                memberSignatureArr[i2] = new MemberSignature(declaredFields[i2]);
            }
            Arrays.sort(memberSignatureArr, new Comparator<MemberSignature>() {
                @Override
                public int compare(MemberSignature memberSignature, MemberSignature memberSignature2) {
                    return memberSignature.name.compareTo(memberSignature2.name);
                }
            });
            for (MemberSignature memberSignature : memberSignatureArr) {
                int modifiers2 = memberSignature.member.getModifiers() & 223;
                if ((modifiers2 & 2) == 0 || (modifiers2 & 136) == 0) {
                    dataOutputStream.writeUTF(memberSignature.name);
                    dataOutputStream.writeInt(modifiers2);
                    dataOutputStream.writeUTF(memberSignature.signature);
                }
            }
            if (hasStaticInitializer(cls, VMRuntime.getRuntime().getTargetSdkVersion() > MAX_SDK_TARGET_FOR_CLINIT_UIDGEN_WORKAROUND)) {
                dataOutputStream.writeUTF("<clinit>");
                dataOutputStream.writeInt(8);
                dataOutputStream.writeUTF("()V");
            }
            Constructor<?>[] declaredConstructors = cls.getDeclaredConstructors();
            MemberSignature[] memberSignatureArr2 = new MemberSignature[declaredConstructors.length];
            for (int i3 = 0; i3 < declaredConstructors.length; i3++) {
                memberSignatureArr2[i3] = new MemberSignature(declaredConstructors[i3]);
            }
            Arrays.sort(memberSignatureArr2, new Comparator<MemberSignature>() {
                @Override
                public int compare(MemberSignature memberSignature2, MemberSignature memberSignature3) {
                    return memberSignature2.signature.compareTo(memberSignature3.signature);
                }
            });
            for (MemberSignature memberSignature2 : memberSignatureArr2) {
                int modifiers3 = memberSignature2.member.getModifiers() & 3391;
                if ((modifiers3 & 2) == 0) {
                    dataOutputStream.writeUTF("<init>");
                    dataOutputStream.writeInt(modifiers3);
                    dataOutputStream.writeUTF(memberSignature2.signature.replace('/', '.'));
                }
            }
            MemberSignature[] memberSignatureArr3 = new MemberSignature[declaredMethods.length];
            for (int i4 = 0; i4 < declaredMethods.length; i4++) {
                memberSignatureArr3[i4] = new MemberSignature(declaredMethods[i4]);
            }
            Arrays.sort(memberSignatureArr3, new Comparator<MemberSignature>() {
                @Override
                public int compare(MemberSignature memberSignature3, MemberSignature memberSignature4) {
                    int iCompareTo = memberSignature3.name.compareTo(memberSignature4.name);
                    if (iCompareTo == 0) {
                        return memberSignature3.signature.compareTo(memberSignature4.signature);
                    }
                    return iCompareTo;
                }
            });
            for (MemberSignature memberSignature3 : memberSignatureArr3) {
                int modifiers4 = memberSignature3.member.getModifiers() & 3391;
                if ((modifiers4 & 2) == 0) {
                    dataOutputStream.writeUTF(memberSignature3.name);
                    dataOutputStream.writeInt(modifiers4);
                    dataOutputStream.writeUTF(memberSignature3.signature.replace('/', '.'));
                }
            }
            dataOutputStream.flush();
            byte[] bArrDigest = MessageDigest.getInstance("SHA").digest(byteArrayOutputStream.toByteArray());
            for (int iMin = Math.min(bArrDigest.length, 8) - 1; iMin >= 0; iMin--) {
                j = (j << 8) | ((long) (bArrDigest[iMin] & Character.DIRECTIONALITY_UNDEFINED));
            }
            return j;
        } catch (IOException e) {
            throw new InternalError(e);
        } catch (NoSuchAlgorithmException e2) {
            throw new SecurityException(e2.getMessage());
        }
    }

    private static class MemberSignature {
        public final Member member;
        public final String name;
        public final String signature;

        public MemberSignature(Field field) {
            this.member = field;
            this.name = field.getName();
            this.signature = ObjectStreamClass.getClassSignature(field.getType());
        }

        public MemberSignature(Constructor<?> constructor) {
            this.member = constructor;
            this.name = constructor.getName();
            this.signature = ObjectStreamClass.getMethodSignature(constructor.getParameterTypes(), Void.TYPE);
        }

        public MemberSignature(Method method) {
            this.member = method;
            this.name = method.getName();
            this.signature = ObjectStreamClass.getMethodSignature(method.getParameterTypes(), method.getReturnType());
        }
    }

    private static class FieldReflector {
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        private final ObjectStreamField[] fields;
        private final int numPrimFields;
        private final int[] offsets;
        private final long[] readKeys;
        private final char[] typeCodes;
        private final Class<?>[] types;
        private final long[] writeKeys;

        FieldReflector(ObjectStreamField[] objectStreamFieldArr) {
            long jObjectFieldOffset;
            this.fields = objectStreamFieldArr;
            int length = objectStreamFieldArr.length;
            this.readKeys = new long[length];
            this.writeKeys = new long[length];
            this.offsets = new int[length];
            this.typeCodes = new char[length];
            ArrayList arrayList = new ArrayList();
            HashSet hashSet = new HashSet();
            for (int i = 0; i < length; i++) {
                ObjectStreamField objectStreamField = objectStreamFieldArr[i];
                Field field = objectStreamField.getField();
                if (field != null) {
                    jObjectFieldOffset = unsafe.objectFieldOffset(field);
                } else {
                    jObjectFieldOffset = -1;
                }
                this.readKeys[i] = jObjectFieldOffset;
                this.writeKeys[i] = hashSet.add(Long.valueOf(jObjectFieldOffset)) ? jObjectFieldOffset : -1L;
                this.offsets[i] = objectStreamField.getOffset();
                this.typeCodes[i] = objectStreamField.getTypeCode();
                if (!objectStreamField.isPrimitive()) {
                    arrayList.add(field != null ? field.getType() : null);
                }
            }
            this.types = (Class[]) arrayList.toArray(new Class[arrayList.size()]);
            this.numPrimFields = length - this.types.length;
        }

        ObjectStreamField[] getFields() {
            return this.fields;
        }

        void getPrimFieldValues(Object obj, byte[] bArr) {
            if (obj == null) {
                throw new NullPointerException();
            }
            for (int i = 0; i < this.numPrimFields; i++) {
                long j = this.readKeys[i];
                int i2 = this.offsets[i];
                char c = this.typeCodes[i];
                if (c != 'F') {
                    if (c != 'S') {
                        if (c == 'Z') {
                            Bits.putBoolean(bArr, i2, unsafe.getBoolean(obj, j));
                        } else {
                            switch (c) {
                                case 'B':
                                    bArr[i2] = unsafe.getByte(obj, j);
                                    break;
                                case 'C':
                                    Bits.putChar(bArr, i2, unsafe.getChar(obj, j));
                                    break;
                                case 'D':
                                    Bits.putDouble(bArr, i2, unsafe.getDouble(obj, j));
                                    break;
                                default:
                                    switch (c) {
                                        case 'I':
                                            Bits.putInt(bArr, i2, unsafe.getInt(obj, j));
                                            break;
                                        case 'J':
                                            Bits.putLong(bArr, i2, unsafe.getLong(obj, j));
                                            break;
                                        default:
                                            throw new InternalError();
                                    }
                                    break;
                            }
                        }
                    } else {
                        Bits.putShort(bArr, i2, unsafe.getShort(obj, j));
                    }
                } else {
                    Bits.putFloat(bArr, i2, unsafe.getFloat(obj, j));
                }
            }
        }

        void setPrimFieldValues(Object obj, byte[] bArr) {
            if (obj == null) {
                throw new NullPointerException();
            }
            for (int i = 0; i < this.numPrimFields; i++) {
                long j = this.writeKeys[i];
                if (j != -1) {
                    int i2 = this.offsets[i];
                    char c = this.typeCodes[i];
                    if (c != 'F') {
                        if (c != 'S') {
                            if (c == 'Z') {
                                unsafe.putBoolean(obj, j, Bits.getBoolean(bArr, i2));
                            } else {
                                switch (c) {
                                    case 'B':
                                        unsafe.putByte(obj, j, bArr[i2]);
                                        break;
                                    case 'C':
                                        unsafe.putChar(obj, j, Bits.getChar(bArr, i2));
                                        break;
                                    case 'D':
                                        unsafe.putDouble(obj, j, Bits.getDouble(bArr, i2));
                                        break;
                                    default:
                                        switch (c) {
                                            case 'I':
                                                unsafe.putInt(obj, j, Bits.getInt(bArr, i2));
                                                break;
                                            case 'J':
                                                unsafe.putLong(obj, j, Bits.getLong(bArr, i2));
                                                break;
                                            default:
                                                throw new InternalError();
                                        }
                                        break;
                                }
                            }
                        } else {
                            unsafe.putShort(obj, j, Bits.getShort(bArr, i2));
                        }
                    } else {
                        unsafe.putFloat(obj, j, Bits.getFloat(bArr, i2));
                    }
                }
            }
        }

        void getObjFieldValues(Object obj, Object[] objArr) {
            if (obj == null) {
                throw new NullPointerException();
            }
            for (int i = this.numPrimFields; i < this.fields.length; i++) {
                char c = this.typeCodes[i];
                if (c == 'L' || c == '[') {
                    objArr[this.offsets[i]] = unsafe.getObject(obj, this.readKeys[i]);
                } else {
                    throw new InternalError();
                }
            }
        }

        void setObjFieldValues(Object obj, Object[] objArr) {
            if (obj == null) {
                throw new NullPointerException();
            }
            for (int i = this.numPrimFields; i < this.fields.length; i++) {
                long j = this.writeKeys[i];
                if (j != -1) {
                    char c = this.typeCodes[i];
                    if (c == 'L' || c == '[') {
                        Object obj2 = objArr[this.offsets[i]];
                        if (obj2 != null && !this.types[i - this.numPrimFields].isInstance(obj2)) {
                            Field field = this.fields[i].getField();
                            throw new ClassCastException("cannot assign instance of " + obj2.getClass().getName() + " to field " + field.getDeclaringClass().getName() + "." + field.getName() + " of type " + field.getType().getName() + " in instance of " + obj.getClass().getName());
                        }
                        unsafe.putObject(obj, j, obj2);
                    } else {
                        throw new InternalError();
                    }
                }
            }
        }
    }

    private static FieldReflector getReflector(ObjectStreamField[] objectStreamFieldArr, ObjectStreamClass objectStreamClass) throws InvalidClassException {
        Class<?> cls;
        Object fieldReflector;
        EntryFuture entryFuture = 0;
        entryFuture = 0;
        if (objectStreamClass != null && objectStreamFieldArr.length > 0) {
            cls = objectStreamClass.cl;
        } else {
            cls = null;
        }
        processQueue(Caches.reflectorsQueue, Caches.reflectors);
        FieldReflectorKey fieldReflectorKey = new FieldReflectorKey(cls, objectStreamFieldArr, Caches.reflectorsQueue);
        Reference<?> referencePutIfAbsent = Caches.reflectors.get(fieldReflectorKey);
        if (referencePutIfAbsent != null) {
            fieldReflector = referencePutIfAbsent.get();
        } else {
            fieldReflector = null;
        }
        if (fieldReflector == null) {
            EntryFuture entryFuture2 = new EntryFuture();
            SoftReference softReference = new SoftReference(entryFuture2);
            do {
                if (referencePutIfAbsent != null) {
                    Caches.reflectors.remove(fieldReflectorKey, referencePutIfAbsent);
                }
                referencePutIfAbsent = Caches.reflectors.putIfAbsent(fieldReflectorKey, softReference);
                if (referencePutIfAbsent != null) {
                    fieldReflector = referencePutIfAbsent.get();
                }
                if (referencePutIfAbsent == null) {
                    break;
                }
            } while (fieldReflector == null);
            if (fieldReflector == null) {
                entryFuture = entryFuture2;
            }
        }
        if (fieldReflector instanceof FieldReflector) {
            return (FieldReflector) fieldReflector;
        }
        if (fieldReflector instanceof EntryFuture) {
            fieldReflector = ((EntryFuture) fieldReflector).get();
        } else if (fieldReflector == null) {
            try {
                fieldReflector = new FieldReflector(matchFields(objectStreamFieldArr, objectStreamClass));
            } catch (Throwable th) {
                fieldReflector = th;
            }
            entryFuture.set(fieldReflector);
            Caches.reflectors.put(fieldReflectorKey, new SoftReference(fieldReflector));
        }
        if (fieldReflector instanceof FieldReflector) {
            return (FieldReflector) fieldReflector;
        }
        if (fieldReflector instanceof InvalidClassException) {
            throw ((InvalidClassException) fieldReflector);
        }
        if (fieldReflector instanceof RuntimeException) {
            throw ((RuntimeException) fieldReflector);
        }
        if (fieldReflector instanceof Error) {
            throw ((Error) fieldReflector);
        }
        throw new InternalError("unexpected entry: " + fieldReflector);
    }

    private static class FieldReflectorKey extends WeakReference<Class<?>> {
        private final int hash;
        private final boolean nullClass;
        private final String sigs;

        FieldReflectorKey(Class<?> cls, ObjectStreamField[] objectStreamFieldArr, ReferenceQueue<Class<?>> referenceQueue) {
            super(cls, referenceQueue);
            this.nullClass = cls == null;
            StringBuilder sb = new StringBuilder();
            for (ObjectStreamField objectStreamField : objectStreamFieldArr) {
                sb.append(objectStreamField.getName());
                sb.append(objectStreamField.getSignature());
            }
            this.sigs = sb.toString();
            this.hash = System.identityHashCode(cls) + this.sigs.hashCode();
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            Class<?> cls;
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof FieldReflectorKey)) {
                return false;
            }
            FieldReflectorKey fieldReflectorKey = (FieldReflectorKey) obj;
            if (!this.nullClass ? !((cls = get()) == null || cls != fieldReflectorKey.get()) : fieldReflectorKey.nullClass) {
                if (this.sigs.equals(fieldReflectorKey.sigs)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static ObjectStreamField[] matchFields(ObjectStreamField[] objectStreamFieldArr, ObjectStreamClass objectStreamClass) throws InvalidClassException {
        ObjectStreamField[] objectStreamFieldArr2 = objectStreamClass != null ? objectStreamClass.fields : NO_FIELDS;
        ObjectStreamField[] objectStreamFieldArr3 = new ObjectStreamField[objectStreamFieldArr.length];
        for (int i = 0; i < objectStreamFieldArr.length; i++) {
            ObjectStreamField objectStreamField = objectStreamFieldArr[i];
            ObjectStreamField objectStreamField2 = null;
            for (ObjectStreamField objectStreamField3 : objectStreamFieldArr2) {
                if (objectStreamField.getName().equals(objectStreamField3.getName()) && objectStreamField.getSignature().equals(objectStreamField3.getSignature())) {
                    if (objectStreamField3.getField() != null) {
                        objectStreamField2 = new ObjectStreamField(objectStreamField3.getField(), objectStreamField3.isUnshared(), false);
                    } else {
                        objectStreamField2 = new ObjectStreamField(objectStreamField3.getName(), objectStreamField3.getSignature(), objectStreamField3.isUnshared());
                    }
                }
            }
            if (objectStreamField2 == null) {
                objectStreamField2 = new ObjectStreamField(objectStreamField.getName(), objectStreamField.getSignature(), false);
            }
            objectStreamField2.setOffset(objectStreamField.getOffset());
            objectStreamFieldArr3[i] = objectStreamField2;
        }
        return objectStreamFieldArr3;
    }

    private static long getConstructorId(Class<?> cls) {
        int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
        if (targetSdkVersion > 0 && targetSdkVersion <= 24) {
            System.logE("WARNING: ObjectStreamClass.getConstructorId(Class<?>) is private API andwill be removed in a future Android release.");
            return 1189998819991197253L;
        }
        throw new UnsupportedOperationException("ObjectStreamClass.getConstructorId(Class<?>) is not supported on SDK " + targetSdkVersion);
    }

    private static Object newInstance(Class<?> cls, long j) {
        int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
        if (targetSdkVersion > 0 && targetSdkVersion <= 24) {
            System.logE("WARNING: ObjectStreamClass.newInstance(Class<?>, long) is private API andwill be removed in a future Android release.");
            return Unsafe.getUnsafe().allocateInstance(cls);
        }
        throw new UnsupportedOperationException("ObjectStreamClass.newInstance(Class<?>, long) is not supported on SDK " + targetSdkVersion);
    }

    static void processQueue(ReferenceQueue<Class<?>> referenceQueue, ConcurrentMap<? extends WeakReference<Class<?>>, ?> concurrentMap) {
        while (true) {
            Reference<? extends Class<?>> referencePoll = referenceQueue.poll();
            if (referencePoll != null) {
                concurrentMap.remove(referencePoll);
            } else {
                return;
            }
        }
    }

    static class WeakClassKey extends WeakReference<Class<?>> {
        private final int hash;

        WeakClassKey(Class<?> cls, ReferenceQueue<Class<?>> referenceQueue) {
            super(cls, referenceQueue);
            this.hash = System.identityHashCode(cls);
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof WeakClassKey)) {
                return false;
            }
            Class<?> cls = get();
            return cls != null && cls == ((WeakClassKey) obj).get();
        }
    }
}
