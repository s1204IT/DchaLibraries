package java.io;

import dalvik.system.VMStack;
import java.io.ObjectStreamClass;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.reflect.misc.ReflectUtil;
import sun.security.util.DerValue;

public class ObjectInputStream extends InputStream implements ObjectInput, ObjectStreamConstants {
    private static final int NULL_HANDLE = -1;
    private final BlockDataInputStream bin;
    private boolean closed;
    private SerialCallbackContext curContext;
    private int depth;
    private final boolean enableOverride;
    private boolean enableResolve;
    private final HandleTable handles;
    private byte[] primVals;
    private final ValidationList vlist;
    private static final Object unsharedMarker = new Object();
    private static final HashMap<String, Class<?>> primClasses = new HashMap<>(8, 1.0f);
    private int passHandle = -1;
    private boolean defaultDataEnd = false;

    public static abstract class GetField {
        public abstract boolean defaulted(String str) throws IOException;

        public abstract byte get(String str, byte b) throws IOException;

        public abstract char get(String str, char c) throws IOException;

        public abstract double get(String str, double d) throws IOException;

        public abstract float get(String str, float f) throws IOException;

        public abstract int get(String str, int i) throws IOException;

        public abstract long get(String str, long j) throws IOException;

        public abstract Object get(String str, Object obj) throws IOException;

        public abstract short get(String str, short s) throws IOException;

        public abstract boolean get(String str, boolean z) throws IOException;

        public abstract ObjectStreamClass getObjectStreamClass();
    }

    private static native void bytesToDoubles(byte[] bArr, int i, double[] dArr, int i2, int i3);

    private static native void bytesToFloats(byte[] bArr, int i, float[] fArr, int i2, int i3);

    static {
        primClasses.put("boolean", Boolean.TYPE);
        primClasses.put("byte", Byte.TYPE);
        primClasses.put("char", Character.TYPE);
        primClasses.put("short", Short.TYPE);
        primClasses.put("int", Integer.TYPE);
        primClasses.put("long", Long.TYPE);
        primClasses.put("float", Float.TYPE);
        primClasses.put("double", Double.TYPE);
        primClasses.put("void", Void.TYPE);
    }

    private static class Caches {
        static final ConcurrentMap<ObjectStreamClass.WeakClassKey, Boolean> subclassAudits = new ConcurrentHashMap();
        static final ReferenceQueue<Class<?>> subclassAuditsQueue = new ReferenceQueue<>();

        private Caches() {
        }
    }

    public ObjectInputStream(InputStream inputStream) throws IOException {
        verifySubclass();
        this.bin = new BlockDataInputStream(inputStream);
        this.handles = new HandleTable(10);
        this.vlist = new ValidationList();
        this.enableOverride = false;
        readStreamHeader();
        this.bin.setBlockDataMode(true);
    }

    protected ObjectInputStream() throws IOException, SecurityException {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
        }
        this.bin = null;
        this.handles = null;
        this.vlist = null;
        this.enableOverride = true;
    }

    @Override
    public final Object readObject() throws IOException, ClassNotFoundException {
        if (this.enableOverride) {
            return readObjectOverride();
        }
        int i = this.passHandle;
        try {
            Object object0 = readObject0(false);
            this.handles.markDependency(i, this.passHandle);
            ClassNotFoundException classNotFoundExceptionLookupException = this.handles.lookupException(this.passHandle);
            if (classNotFoundExceptionLookupException != null) {
                throw classNotFoundExceptionLookupException;
            }
            if (this.depth == 0) {
                this.vlist.doCallbacks();
            }
            return object0;
        } finally {
            this.passHandle = i;
            if (this.closed && this.depth == 0) {
                clear();
            }
        }
    }

    protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        return null;
    }

    public Object readUnshared() throws IOException, ClassNotFoundException {
        int i = this.passHandle;
        try {
            Object object0 = readObject0(true);
            this.handles.markDependency(i, this.passHandle);
            ClassNotFoundException classNotFoundExceptionLookupException = this.handles.lookupException(this.passHandle);
            if (classNotFoundExceptionLookupException != null) {
                throw classNotFoundExceptionLookupException;
            }
            if (this.depth == 0) {
                this.vlist.doCallbacks();
            }
            return object0;
        } finally {
            this.passHandle = i;
            if (this.closed && this.depth == 0) {
                clear();
            }
        }
    }

    public void defaultReadObject() throws IOException, ClassNotFoundException {
        SerialCallbackContext serialCallbackContext = this.curContext;
        if (serialCallbackContext == null) {
            throw new NotActiveException("not in call to readObject");
        }
        Object obj = serialCallbackContext.getObj();
        ObjectStreamClass desc = serialCallbackContext.getDesc();
        this.bin.setBlockDataMode(false);
        defaultReadFields(obj, desc);
        this.bin.setBlockDataMode(true);
        if (!desc.hasWriteObjectData()) {
            this.defaultDataEnd = true;
        }
        ClassNotFoundException classNotFoundExceptionLookupException = this.handles.lookupException(this.passHandle);
        if (classNotFoundExceptionLookupException != null) {
            throw classNotFoundExceptionLookupException;
        }
    }

    public GetField readFields() throws IOException, ClassNotFoundException {
        SerialCallbackContext serialCallbackContext = this.curContext;
        if (serialCallbackContext == null) {
            throw new NotActiveException("not in call to readObject");
        }
        serialCallbackContext.getObj();
        ObjectStreamClass desc = serialCallbackContext.getDesc();
        this.bin.setBlockDataMode(false);
        GetFieldImpl getFieldImpl = new GetFieldImpl(desc);
        getFieldImpl.readFields();
        this.bin.setBlockDataMode(true);
        if (!desc.hasWriteObjectData()) {
            this.defaultDataEnd = true;
        }
        return getFieldImpl;
    }

    public void registerValidation(ObjectInputValidation objectInputValidation, int i) throws InvalidObjectException, NotActiveException {
        if (this.depth == 0) {
            throw new NotActiveException("stream inactive");
        }
        this.vlist.register(objectInputValidation, i);
    }

    protected Class<?> resolveClass(ObjectStreamClass objectStreamClass) throws ClassNotFoundException, IOException {
        String name = objectStreamClass.getName();
        try {
            return Class.forName(name, false, latestUserDefinedLoader());
        } catch (ClassNotFoundException e) {
            Class<?> cls = primClasses.get(name);
            if (cls != null) {
                return cls;
            }
            throw e;
        }
    }

    protected Class<?> resolveProxyClass(String[] strArr) throws ClassNotFoundException, IOException {
        ClassLoader classLoaderLatestUserDefinedLoader = latestUserDefinedLoader();
        Class[] clsArr = new Class[strArr.length];
        ClassLoader classLoader = null;
        boolean z = false;
        for (int i = 0; i < strArr.length; i++) {
            Class<?> cls = Class.forName(strArr[i], false, classLoaderLatestUserDefinedLoader);
            if ((cls.getModifiers() & 1) == 0) {
                if (z) {
                    if (classLoader != cls.getClassLoader()) {
                        throw new IllegalAccessError("conflicting non-public interface class loaders");
                    }
                } else {
                    classLoader = cls.getClassLoader();
                    z = true;
                }
            }
            clsArr[i] = cls;
        }
        if (z) {
            classLoaderLatestUserDefinedLoader = classLoader;
        }
        try {
            return Proxy.getProxyClass(classLoaderLatestUserDefinedLoader, clsArr);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }

    protected Object resolveObject(Object obj) throws IOException {
        return obj;
    }

    protected boolean enableResolveObject(boolean z) throws SecurityException {
        SecurityManager securityManager;
        if (z == this.enableResolve) {
            return z;
        }
        if (z && (securityManager = System.getSecurityManager()) != null) {
            securityManager.checkPermission(SUBSTITUTION_PERMISSION);
        }
        this.enableResolve = z;
        return !this.enableResolve;
    }

    protected void readStreamHeader() throws IOException {
        short s = this.bin.readShort();
        short s2 = this.bin.readShort();
        if (s != -21267 || s2 != 5) {
            throw new StreamCorruptedException(String.format("invalid stream header: %04X%04X", Short.valueOf(s), Short.valueOf(s2)));
        }
    }

    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass objectStreamClass = new ObjectStreamClass();
        objectStreamClass.readNonProxy(this);
        return objectStreamClass;
    }

    @Override
    public int read() throws IOException {
        return this.bin.read();
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (bArr == null) {
            throw new NullPointerException();
        }
        int i3 = i + i2;
        if (i < 0 || i2 < 0 || i3 > bArr.length || i3 < 0) {
            throw new IndexOutOfBoundsException();
        }
        return this.bin.read(bArr, i, i2, false);
    }

    @Override
    public int available() throws IOException {
        return this.bin.available();
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        if (this.depth == 0) {
            clear();
        }
        this.bin.close();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return this.bin.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return this.bin.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return this.bin.readUnsignedByte();
    }

    @Override
    public char readChar() throws IOException {
        return this.bin.readChar();
    }

    @Override
    public short readShort() throws IOException {
        return this.bin.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return this.bin.readUnsignedShort();
    }

    @Override
    public int readInt() throws IOException {
        return this.bin.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return this.bin.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return this.bin.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return this.bin.readDouble();
    }

    @Override
    public void readFully(byte[] bArr) throws IOException {
        this.bin.readFully(bArr, 0, bArr.length, false);
    }

    @Override
    public void readFully(byte[] bArr, int i, int i2) throws IOException {
        int i3 = i + i2;
        if (i < 0 || i2 < 0 || i3 > bArr.length || i3 < 0) {
            throw new IndexOutOfBoundsException();
        }
        this.bin.readFully(bArr, i, i2, false);
    }

    @Override
    public int skipBytes(int i) throws IOException {
        return this.bin.skipBytes(i);
    }

    @Override
    @Deprecated
    public String readLine() throws IOException {
        return this.bin.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return this.bin.readUTF();
    }

    private void verifySubclass() {
        SecurityManager securityManager;
        Class<?> cls = getClass();
        if (cls == ObjectInputStream.class || (securityManager = System.getSecurityManager()) == null) {
            return;
        }
        ObjectStreamClass.processQueue(Caches.subclassAuditsQueue, Caches.subclassAudits);
        ObjectStreamClass.WeakClassKey weakClassKey = new ObjectStreamClass.WeakClassKey(cls, Caches.subclassAuditsQueue);
        Boolean boolValueOf = Caches.subclassAudits.get(weakClassKey);
        if (boolValueOf == null) {
            boolValueOf = Boolean.valueOf(auditSubclass(cls));
            Caches.subclassAudits.putIfAbsent(weakClassKey, boolValueOf);
        }
        if (boolValueOf.booleanValue()) {
            return;
        }
        securityManager.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
    }

    private static boolean auditSubclass(final Class<?> cls) {
        return ((Boolean) AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                for (Class superclass = cls; superclass != ObjectInputStream.class; superclass = superclass.getSuperclass()) {
                    try {
                        superclass.getDeclaredMethod("readUnshared", (Class[]) null);
                        return Boolean.FALSE;
                    } catch (NoSuchMethodException e) {
                        try {
                            superclass.getDeclaredMethod("readFields", (Class[]) null);
                            return Boolean.FALSE;
                        } catch (NoSuchMethodException e2) {
                        }
                    }
                }
                return Boolean.TRUE;
            }
        })).booleanValue();
    }

    private void clear() {
        this.handles.clear();
        this.vlist.clear();
    }

    private Object readObject0(boolean z) throws IOException {
        byte bPeekByte;
        boolean blockDataMode = this.bin.getBlockDataMode();
        if (blockDataMode) {
            int iCurrentBlockRemaining = this.bin.currentBlockRemaining();
            if (iCurrentBlockRemaining > 0) {
                throw new OptionalDataException(iCurrentBlockRemaining);
            }
            if (this.defaultDataEnd) {
                throw new OptionalDataException(true);
            }
            this.bin.setBlockDataMode(false);
        }
        while (true) {
            bPeekByte = this.bin.peekByte();
            if (bPeekByte != 121) {
                break;
            }
            this.bin.readByte();
            handleReset();
        }
        this.depth++;
        try {
            switch (bPeekByte) {
                case 112:
                    return readNull();
                case 113:
                    return readHandle(z);
                case 114:
                case 125:
                    return readClassDesc(z);
                case 115:
                    return checkResolve(readOrdinaryObject(z));
                case 116:
                case 124:
                    return checkResolve(readString(z));
                case 117:
                    return checkResolve(readArray(z));
                case 118:
                    return readClass(z);
                case 119:
                case 122:
                    if (!blockDataMode) {
                        throw new StreamCorruptedException("unexpected block data");
                    }
                    this.bin.setBlockDataMode(true);
                    this.bin.peek();
                    throw new OptionalDataException(this.bin.currentBlockRemaining());
                case 120:
                    if (blockDataMode) {
                        throw new OptionalDataException(true);
                    }
                    throw new StreamCorruptedException("unexpected end of block data");
                case 121:
                default:
                    throw new StreamCorruptedException(String.format("invalid type code: %02X", Byte.valueOf(bPeekByte)));
                case 123:
                    throw new WriteAbortedException("writing aborted", readFatalException());
                case 126:
                    return checkResolve(readEnum(z));
            }
        } finally {
            this.depth--;
            this.bin.setBlockDataMode(blockDataMode);
        }
    }

    private Object checkResolve(Object obj) throws IOException {
        if (!this.enableResolve || this.handles.lookupException(this.passHandle) != null) {
            return obj;
        }
        Object objResolveObject = resolveObject(obj);
        if (objResolveObject != obj) {
            this.handles.setObject(this.passHandle, objResolveObject);
        }
        return objResolveObject;
    }

    String readTypeString() throws IOException {
        int i = this.passHandle;
        try {
            byte bPeekByte = this.bin.peekByte();
            if (bPeekByte == 116 || bPeekByte == 124) {
                return readString(false);
            }
            switch (bPeekByte) {
                case 112:
                    return (String) readNull();
                case 113:
                    return (String) readHandle(false);
                default:
                    throw new StreamCorruptedException(String.format("invalid type code: %02X", Byte.valueOf(bPeekByte)));
            }
        } finally {
            this.passHandle = i;
        }
    }

    private Object readNull() throws IOException {
        if (this.bin.readByte() != 112) {
            throw new InternalError();
        }
        this.passHandle = -1;
        return null;
    }

    private Object readHandle(boolean z) throws IOException {
        if (this.bin.readByte() != 113) {
            throw new InternalError();
        }
        this.passHandle = this.bin.readInt() - ObjectStreamConstants.baseWireHandle;
        if (this.passHandle < 0 || this.passHandle >= this.handles.size()) {
            throw new StreamCorruptedException(String.format("invalid handle value: %08X", Integer.valueOf(this.passHandle + ObjectStreamConstants.baseWireHandle)));
        }
        if (z) {
            throw new InvalidObjectException("cannot read back reference as unshared");
        }
        Object objLookupObject = this.handles.lookupObject(this.passHandle);
        if (objLookupObject == unsharedMarker) {
            throw new InvalidObjectException("cannot read back reference to unshared object");
        }
        return objLookupObject;
    }

    private Class<?> readClass(boolean z) throws IOException {
        if (this.bin.readByte() != 118) {
            throw new InternalError();
        }
        ObjectStreamClass classDesc = readClassDesc(false);
        Class<?> clsForClass = classDesc.forClass();
        this.passHandle = this.handles.assign(z ? unsharedMarker : clsForClass);
        ClassNotFoundException resolveException = classDesc.getResolveException();
        if (resolveException != null) {
            this.handles.markException(this.passHandle, resolveException);
        }
        this.handles.finish(this.passHandle);
        return clsForClass;
    }

    private ObjectStreamClass readClassDesc(boolean z) throws IOException {
        byte bPeekByte = this.bin.peekByte();
        if (bPeekByte != 125) {
            switch (bPeekByte) {
                case 112:
                    return (ObjectStreamClass) readNull();
                case 113:
                    return (ObjectStreamClass) readHandle(z);
                case 114:
                    return readNonProxyDesc(z);
                default:
                    throw new StreamCorruptedException(String.format("invalid type code: %02X", Byte.valueOf(bPeekByte)));
            }
        }
        return readProxyDesc(z);
    }

    private boolean isCustomSubclass() {
        return getClass().getClassLoader() != ObjectInputStream.class.getClassLoader();
    }

    private ObjectStreamClass readProxyDesc(boolean z) throws IOException {
        Class<?> clsResolveProxyClass;
        if (this.bin.readByte() != 125) {
            throw new InternalError();
        }
        ObjectStreamClass objectStreamClass = new ObjectStreamClass();
        int iAssign = this.handles.assign(z ? unsharedMarker : objectStreamClass);
        this.passHandle = -1;
        int i = this.bin.readInt();
        String[] strArr = new String[i];
        for (int i2 = 0; i2 < i; i2++) {
            strArr[i2] = this.bin.readUTF();
        }
        this.bin.setBlockDataMode(true);
        ClassNotFoundException e = null;
        try {
            clsResolveProxyClass = resolveProxyClass(strArr);
            try {
                if (clsResolveProxyClass == null) {
                    e = new ClassNotFoundException("null class");
                } else {
                    if (!Proxy.isProxyClass(clsResolveProxyClass)) {
                        throw new InvalidClassException("Not a proxy");
                    }
                    ReflectUtil.checkProxyPackageAccess(getClass().getClassLoader(), clsResolveProxyClass.getInterfaces());
                }
            } catch (ClassNotFoundException e2) {
                e = e2;
            }
        } catch (ClassNotFoundException e3) {
            clsResolveProxyClass = null;
            e = e3;
        }
        skipCustomData();
        objectStreamClass.initProxy(clsResolveProxyClass, e, readClassDesc(false));
        this.handles.finish(iAssign);
        this.passHandle = iAssign;
        return objectStreamClass;
    }

    private ObjectStreamClass readNonProxyDesc(boolean z) throws IOException {
        Class<?> clsResolveClass;
        if (this.bin.readByte() != 114) {
            throw new InternalError();
        }
        ObjectStreamClass objectStreamClass = new ObjectStreamClass();
        int iAssign = this.handles.assign(z ? unsharedMarker : objectStreamClass);
        this.passHandle = -1;
        try {
            ObjectStreamClass classDescriptor = readClassDescriptor();
            this.bin.setBlockDataMode(true);
            boolean zIsCustomSubclass = isCustomSubclass();
            ClassNotFoundException classNotFoundException = null;
            try {
                clsResolveClass = resolveClass(classDescriptor);
            } catch (ClassNotFoundException e) {
                e = e;
                clsResolveClass = null;
            }
            try {
            } catch (ClassNotFoundException e2) {
                e = e2;
                classNotFoundException = e;
            }
            if (clsResolveClass == null) {
                classNotFoundException = new ClassNotFoundException("null class");
            } else {
                if (zIsCustomSubclass) {
                    ReflectUtil.checkPackageAccess(clsResolveClass);
                }
                skipCustomData();
                objectStreamClass.initNonProxy(classDescriptor, clsResolveClass, classNotFoundException, readClassDesc(false));
                this.handles.finish(iAssign);
                this.passHandle = iAssign;
                return objectStreamClass;
            }
            skipCustomData();
            objectStreamClass.initNonProxy(classDescriptor, clsResolveClass, classNotFoundException, readClassDesc(false));
            this.handles.finish(iAssign);
            this.passHandle = iAssign;
            return objectStreamClass;
        } catch (ClassNotFoundException e3) {
            throw ((IOException) new InvalidClassException("failed to read class descriptor").initCause(e3));
        }
    }

    private String readString(boolean z) throws IOException {
        String utf;
        byte b = this.bin.readByte();
        if (b == 116) {
            utf = this.bin.readUTF();
        } else if (b == 124) {
            utf = this.bin.readLongUTF();
        } else {
            throw new StreamCorruptedException(String.format("invalid type code: %02X", Byte.valueOf(b)));
        }
        this.passHandle = this.handles.assign(z ? unsharedMarker : utf);
        this.handles.finish(this.passHandle);
        return utf;
    }

    private Object readArray(boolean z) throws IOException {
        Object objNewInstance;
        if (this.bin.readByte() != 117) {
            throw new InternalError();
        }
        ObjectStreamClass classDesc = readClassDesc(false);
        int i = this.bin.readInt();
        Class<?> clsForClass = classDesc.forClass();
        Class<?> componentType = null;
        if (clsForClass != null) {
            componentType = clsForClass.getComponentType();
            objNewInstance = Array.newInstance(componentType, i);
        } else {
            objNewInstance = null;
        }
        int iAssign = this.handles.assign(z ? unsharedMarker : objNewInstance);
        ClassNotFoundException resolveException = classDesc.getResolveException();
        if (resolveException != null) {
            this.handles.markException(iAssign, resolveException);
        }
        if (componentType == null) {
            for (int i2 = 0; i2 < i; i2++) {
                readObject0(false);
            }
        } else if (componentType.isPrimitive()) {
            if (componentType == Integer.TYPE) {
                this.bin.readInts((int[]) objNewInstance, 0, i);
            } else if (componentType == Byte.TYPE) {
                this.bin.readFully((byte[]) objNewInstance, 0, i, true);
            } else if (componentType == Long.TYPE) {
                this.bin.readLongs((long[]) objNewInstance, 0, i);
            } else if (componentType == Float.TYPE) {
                this.bin.readFloats((float[]) objNewInstance, 0, i);
            } else if (componentType == Double.TYPE) {
                this.bin.readDoubles((double[]) objNewInstance, 0, i);
            } else if (componentType == Short.TYPE) {
                this.bin.readShorts((short[]) objNewInstance, 0, i);
            } else if (componentType == Character.TYPE) {
                this.bin.readChars((char[]) objNewInstance, 0, i);
            } else if (componentType == Boolean.TYPE) {
                this.bin.readBooleans((boolean[]) objNewInstance, 0, i);
            } else {
                throw new InternalError();
            }
        } else {
            Object[] objArr = (Object[]) objNewInstance;
            for (int i3 = 0; i3 < i; i3++) {
                objArr[i3] = readObject0(false);
                this.handles.markDependency(iAssign, this.passHandle);
            }
        }
        this.handles.finish(iAssign);
        this.passHandle = iAssign;
        return objNewInstance;
    }

    private Enum<?> readEnum(boolean z) throws IOException {
        if (this.bin.readByte() != 126) {
            throw new InternalError();
        }
        ObjectStreamClass classDesc = readClassDesc(false);
        if (!classDesc.isEnum()) {
            throw new InvalidClassException("non-enum class: " + ((Object) classDesc));
        }
        Enum<?> enumValueOf = null;
        int iAssign = this.handles.assign(z ? unsharedMarker : null);
        ClassNotFoundException resolveException = classDesc.getResolveException();
        if (resolveException != null) {
            this.handles.markException(iAssign, resolveException);
        }
        String string = readString(false);
        Class<?> clsForClass = classDesc.forClass();
        if (clsForClass != null) {
            try {
                enumValueOf = Enum.valueOf(clsForClass, string);
                if (!z) {
                    this.handles.setObject(iAssign, enumValueOf);
                }
            } catch (IllegalArgumentException e) {
                throw ((IOException) new InvalidObjectException("enum constant " + string + " does not exist in " + ((Object) clsForClass)).initCause(e));
            }
        }
        this.handles.finish(iAssign);
        this.passHandle = iAssign;
        return enumValueOf;
    }

    private Object readOrdinaryObject(boolean z) throws IOException {
        Object objCloneArray;
        if (this.bin.readByte() != 115) {
            throw new InternalError();
        }
        ObjectStreamClass classDesc = readClassDesc(false);
        classDesc.checkDeserialize();
        Class<?> clsForClass = classDesc.forClass();
        if (clsForClass == String.class || clsForClass == Class.class || clsForClass == ObjectStreamClass.class) {
            throw new InvalidClassException("invalid class descriptor");
        }
        try {
            Object objNewInstance = classDesc.isInstantiable() ? classDesc.newInstance() : null;
            this.passHandle = this.handles.assign(z ? unsharedMarker : objNewInstance);
            ClassNotFoundException resolveException = classDesc.getResolveException();
            if (resolveException != null) {
                this.handles.markException(this.passHandle, resolveException);
            }
            if (classDesc.isExternalizable()) {
                readExternalData((Externalizable) objNewInstance, classDesc);
            } else {
                readSerialData(objNewInstance, classDesc);
            }
            this.handles.finish(this.passHandle);
            if (objNewInstance != null && this.handles.lookupException(this.passHandle) == null && classDesc.hasReadResolveMethod()) {
                Object objInvokeReadResolve = classDesc.invokeReadResolve(objNewInstance);
                if (z && objInvokeReadResolve.getClass().isArray()) {
                    objCloneArray = cloneArray(objInvokeReadResolve);
                } else {
                    objCloneArray = objInvokeReadResolve;
                }
                if (objCloneArray != objNewInstance) {
                    this.handles.setObject(this.passHandle, objCloneArray);
                    return objCloneArray;
                }
            }
            return objNewInstance;
        } catch (Exception e) {
            throw ((IOException) new InvalidClassException(classDesc.forClass().getName(), "unable to create instance").initCause(e));
        }
    }

    private void readExternalData(Externalizable externalizable, ObjectStreamClass objectStreamClass) throws IOException {
        SerialCallbackContext serialCallbackContext = this.curContext;
        if (serialCallbackContext != null) {
            serialCallbackContext.check();
        }
        this.curContext = null;
        try {
            boolean zHasBlockExternalData = objectStreamClass.hasBlockExternalData();
            if (zHasBlockExternalData) {
                this.bin.setBlockDataMode(true);
            }
            if (externalizable != null) {
                try {
                    externalizable.readExternal(this);
                } catch (ClassNotFoundException e) {
                    this.handles.markException(this.passHandle, e);
                }
            }
            if (zHasBlockExternalData) {
                skipCustomData();
            }
        } finally {
            if (serialCallbackContext != null) {
                serialCallbackContext.check();
            }
            this.curContext = serialCallbackContext;
        }
    }

    private void readSerialData(Object obj, ObjectStreamClass objectStreamClass) throws IOException {
        ObjectStreamClass.ClassDataSlot[] classDataLayout = objectStreamClass.getClassDataLayout();
        for (int i = 0; i < classDataLayout.length; i++) {
            ObjectStreamClass objectStreamClass2 = classDataLayout[i].desc;
            if (classDataLayout[i].hasData) {
                if (obj == null || this.handles.lookupException(this.passHandle) != null) {
                    defaultReadFields(null, objectStreamClass2);
                } else if (objectStreamClass2.hasReadObjectMethod()) {
                    SerialCallbackContext serialCallbackContext = this.curContext;
                    if (serialCallbackContext != null) {
                        serialCallbackContext.check();
                    }
                    try {
                        try {
                            this.curContext = new SerialCallbackContext(obj, objectStreamClass2);
                            this.bin.setBlockDataMode(true);
                            objectStreamClass2.invokeReadObject(obj, this);
                            this.curContext.setUsed();
                        } catch (ClassNotFoundException e) {
                            this.handles.markException(this.passHandle, e);
                            this.curContext.setUsed();
                            if (serialCallbackContext != null) {
                            }
                        }
                        if (serialCallbackContext != null) {
                            serialCallbackContext.check();
                        }
                        this.curContext = serialCallbackContext;
                        this.defaultDataEnd = false;
                    } catch (Throwable th) {
                        this.curContext.setUsed();
                        if (serialCallbackContext != null) {
                            serialCallbackContext.check();
                        }
                        this.curContext = serialCallbackContext;
                        throw th;
                    }
                } else {
                    defaultReadFields(obj, objectStreamClass2);
                }
                if (objectStreamClass2.hasWriteObjectData()) {
                    skipCustomData();
                } else {
                    this.bin.setBlockDataMode(false);
                }
            } else if (obj != null && objectStreamClass2.hasReadObjectNoDataMethod() && this.handles.lookupException(this.passHandle) == null) {
                objectStreamClass2.invokeReadObjectNoData(obj);
            }
        }
    }

    private void skipCustomData() throws IOException {
        int i = this.passHandle;
        while (true) {
            if (this.bin.getBlockDataMode()) {
                this.bin.skipBlockData();
                this.bin.setBlockDataMode(false);
            }
            switch (this.bin.peekByte()) {
                case 119:
                case 122:
                    this.bin.setBlockDataMode(true);
                    break;
                case 120:
                    this.bin.readByte();
                    this.passHandle = i;
                    return;
                case 121:
                default:
                    readObject0(false);
                    break;
            }
        }
    }

    private void defaultReadFields(Object obj, ObjectStreamClass objectStreamClass) throws IOException {
        Class<?> clsForClass = objectStreamClass.forClass();
        if (clsForClass != null && obj != null && !clsForClass.isInstance(obj)) {
            throw new ClassCastException();
        }
        int primDataSize = objectStreamClass.getPrimDataSize();
        if (this.primVals == null || this.primVals.length < primDataSize) {
            this.primVals = new byte[primDataSize];
        }
        this.bin.readFully(this.primVals, 0, primDataSize, false);
        if (obj != null) {
            objectStreamClass.setPrimFieldValues(obj, this.primVals);
        }
        int i = this.passHandle;
        ObjectStreamField[] fields = objectStreamClass.getFields(false);
        Object[] objArr = new Object[objectStreamClass.getNumObjFields()];
        int length = fields.length - objArr.length;
        for (int i2 = 0; i2 < objArr.length; i2++) {
            ObjectStreamField objectStreamField = fields[length + i2];
            objArr[i2] = readObject0(objectStreamField.isUnshared());
            if (objectStreamField.getField() != null) {
                this.handles.markDependency(i, this.passHandle);
            }
        }
        if (obj != null) {
            objectStreamClass.setObjFieldValues(obj, objArr);
        }
        this.passHandle = i;
    }

    private IOException readFatalException() throws IOException {
        if (this.bin.readByte() != 123) {
            throw new InternalError();
        }
        clear();
        IOException iOException = (IOException) readObject0(false);
        clear();
        return iOException;
    }

    private void handleReset() throws StreamCorruptedException {
        if (this.depth > 0) {
            throw new StreamCorruptedException("unexpected reset; recursion depth: " + this.depth);
        }
        clear();
    }

    private static ClassLoader latestUserDefinedLoader() {
        return VMStack.getClosestUserClassLoader();
    }

    private class GetFieldImpl extends GetField {
        private final ObjectStreamClass desc;
        private final int[] objHandles;
        private final Object[] objVals;
        private final byte[] primVals;

        GetFieldImpl(ObjectStreamClass objectStreamClass) {
            this.desc = objectStreamClass;
            this.primVals = new byte[objectStreamClass.getPrimDataSize()];
            this.objVals = new Object[objectStreamClass.getNumObjFields()];
            this.objHandles = new int[this.objVals.length];
        }

        @Override
        public ObjectStreamClass getObjectStreamClass() {
            return this.desc;
        }

        @Override
        public boolean defaulted(String str) throws IOException {
            return getFieldOffset(str, null) < 0;
        }

        @Override
        public boolean get(String str, boolean z) throws IOException {
            int fieldOffset = getFieldOffset(str, Boolean.TYPE);
            return fieldOffset >= 0 ? Bits.getBoolean(this.primVals, fieldOffset) : z;
        }

        @Override
        public byte get(String str, byte b) throws IOException {
            int fieldOffset = getFieldOffset(str, Byte.TYPE);
            return fieldOffset >= 0 ? this.primVals[fieldOffset] : b;
        }

        @Override
        public char get(String str, char c) throws IOException {
            int fieldOffset = getFieldOffset(str, Character.TYPE);
            return fieldOffset >= 0 ? Bits.getChar(this.primVals, fieldOffset) : c;
        }

        @Override
        public short get(String str, short s) throws IOException {
            int fieldOffset = getFieldOffset(str, Short.TYPE);
            return fieldOffset >= 0 ? Bits.getShort(this.primVals, fieldOffset) : s;
        }

        @Override
        public int get(String str, int i) throws IOException {
            int fieldOffset = getFieldOffset(str, Integer.TYPE);
            return fieldOffset >= 0 ? Bits.getInt(this.primVals, fieldOffset) : i;
        }

        @Override
        public float get(String str, float f) throws IOException {
            int fieldOffset = getFieldOffset(str, Float.TYPE);
            return fieldOffset >= 0 ? Bits.getFloat(this.primVals, fieldOffset) : f;
        }

        @Override
        public long get(String str, long j) throws IOException {
            int fieldOffset = getFieldOffset(str, Long.TYPE);
            return fieldOffset >= 0 ? Bits.getLong(this.primVals, fieldOffset) : j;
        }

        @Override
        public double get(String str, double d) throws IOException {
            int fieldOffset = getFieldOffset(str, Double.TYPE);
            return fieldOffset >= 0 ? Bits.getDouble(this.primVals, fieldOffset) : d;
        }

        @Override
        public Object get(String str, Object obj) throws IOException {
            int fieldOffset = getFieldOffset(str, Object.class);
            if (fieldOffset >= 0) {
                int i = this.objHandles[fieldOffset];
                ObjectInputStream.this.handles.markDependency(ObjectInputStream.this.passHandle, i);
                if (ObjectInputStream.this.handles.lookupException(i) == null) {
                    return this.objVals[fieldOffset];
                }
                return null;
            }
            return obj;
        }

        void readFields() throws IOException {
            ObjectInputStream.this.bin.readFully(this.primVals, 0, this.primVals.length, false);
            int i = ObjectInputStream.this.passHandle;
            ObjectStreamField[] fields = this.desc.getFields(false);
            int length = fields.length - this.objVals.length;
            for (int i2 = 0; i2 < this.objVals.length; i2++) {
                this.objVals[i2] = ObjectInputStream.this.readObject0(fields[length + i2].isUnshared());
                this.objHandles[i2] = ObjectInputStream.this.passHandle;
            }
            ObjectInputStream.this.passHandle = i;
        }

        private int getFieldOffset(String str, Class<?> cls) {
            ObjectStreamField field = this.desc.getField(str, cls);
            if (field != null) {
                return field.getOffset();
            }
            if (this.desc.getLocalDesc().getField(str, cls) != null) {
                return -1;
            }
            throw new IllegalArgumentException("no such field " + str + " with type " + ((Object) cls));
        }
    }

    private static class ValidationList {
        private Callback list;

        private static class Callback {
            final AccessControlContext acc;
            Callback next;
            final ObjectInputValidation obj;
            final int priority;

            Callback(ObjectInputValidation objectInputValidation, int i, Callback callback, AccessControlContext accessControlContext) {
                this.obj = objectInputValidation;
                this.priority = i;
                this.next = callback;
                this.acc = accessControlContext;
            }
        }

        ValidationList() {
        }

        void register(ObjectInputValidation objectInputValidation, int i) throws InvalidObjectException {
            Callback callback;
            if (objectInputValidation == null) {
                throw new InvalidObjectException("null callback");
            }
            Callback callback2 = null;
            Callback callback3 = this.list;
            while (true) {
                Callback callback4 = callback3;
                callback = callback2;
                callback2 = callback4;
                if (callback2 == null || i >= callback2.priority) {
                    break;
                } else {
                    callback3 = callback2.next;
                }
            }
            AccessControlContext context = AccessController.getContext();
            if (callback != null) {
                callback.next = new Callback(objectInputValidation, i, callback2, context);
            } else {
                this.list = new Callback(objectInputValidation, i, this.list, context);
            }
        }

        void doCallbacks() throws InvalidObjectException {
            while (this.list != null) {
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                        @Override
                        public Void run() throws InvalidObjectException {
                            ValidationList.this.list.obj.validateObject();
                            return null;
                        }
                    }, this.list.acc);
                    this.list = this.list.next;
                } catch (PrivilegedActionException e) {
                    this.list = null;
                    throw ((InvalidObjectException) e.getException());
                }
            }
        }

        public void clear() {
            this.list = null;
        }
    }

    private static class PeekInputStream extends InputStream {
        private final InputStream in;
        private int peekb = -1;

        PeekInputStream(InputStream inputStream) {
            this.in = inputStream;
        }

        int peek() throws IOException {
            if (this.peekb >= 0) {
                return this.peekb;
            }
            int i = this.in.read();
            this.peekb = i;
            return i;
        }

        @Override
        public int read() throws IOException {
            if (this.peekb >= 0) {
                int i = this.peekb;
                this.peekb = -1;
                return i;
            }
            return this.in.read();
        }

        @Override
        public int read(byte[] bArr, int i, int i2) throws IOException {
            if (i2 == 0) {
                return 0;
            }
            if (this.peekb < 0) {
                return this.in.read(bArr, i, i2);
            }
            bArr[i] = (byte) this.peekb;
            this.peekb = -1;
            int i3 = this.in.read(bArr, i + 1, i2 - 1);
            if (i3 >= 0) {
                return 1 + i3;
            }
            return 1;
        }

        void readFully(byte[] bArr, int i, int i2) throws IOException {
            int i3 = 0;
            while (i3 < i2) {
                int i4 = read(bArr, i + i3, i2 - i3);
                if (i4 < 0) {
                    throw new EOFException();
                }
                i3 += i4;
            }
        }

        @Override
        public long skip(long j) throws IOException {
            if (j <= 0) {
                return 0L;
            }
            int i = 0;
            if (this.peekb >= 0) {
                this.peekb = -1;
                i = 1;
                j--;
            }
            return ((long) i) + skip(j);
        }

        @Override
        public int available() throws IOException {
            return this.in.available() + (this.peekb >= 0 ? 1 : 0);
        }

        @Override
        public void close() throws IOException {
            this.in.close();
        }
    }

    private class BlockDataInputStream extends InputStream implements DataInput {
        private static final int CHAR_BUF_SIZE = 256;
        private static final int HEADER_BLOCKED = -2;
        private static final int MAX_BLOCK_SIZE = 1024;
        private static final int MAX_HEADER_SIZE = 5;
        private final PeekInputStream in;
        private final byte[] buf = new byte[1024];
        private final byte[] hbuf = new byte[5];
        private final char[] cbuf = new char[256];
        private boolean blkmode = false;
        private int pos = 0;
        private int end = -1;
        private int unread = 0;
        private final DataInputStream din = new DataInputStream(this);

        BlockDataInputStream(InputStream inputStream) {
            this.in = new PeekInputStream(inputStream);
        }

        boolean setBlockDataMode(boolean z) throws IOException {
            if (this.blkmode == z) {
                return this.blkmode;
            }
            if (z) {
                this.pos = 0;
                this.end = 0;
                this.unread = 0;
            } else if (this.pos < this.end) {
                throw new IllegalStateException("unread block data");
            }
            this.blkmode = z;
            return !this.blkmode;
        }

        boolean getBlockDataMode() {
            return this.blkmode;
        }

        void skipBlockData() throws IOException {
            if (!this.blkmode) {
                throw new IllegalStateException("not in block data mode");
            }
            while (this.end >= 0) {
                refill();
            }
        }

        private int readBlockHeader(boolean z) throws IOException {
            if (ObjectInputStream.this.defaultDataEnd) {
                return -1;
            }
            while (true) {
                int iAvailable = z ? Integer.MAX_VALUE : this.in.available();
                if (iAvailable == 0) {
                    return -2;
                }
                try {
                    int iPeek = this.in.peek();
                    switch (iPeek) {
                        case 119:
                            if (iAvailable < 2) {
                                return -2;
                            }
                            this.in.readFully(this.hbuf, 0, 2);
                            return this.hbuf[1] & Character.DIRECTIONALITY_UNDEFINED;
                        case 120:
                        default:
                            if (iPeek >= 0 && (iPeek < 112 || iPeek > 126)) {
                                throw new StreamCorruptedException(String.format("invalid type code: %02X", Integer.valueOf(iPeek)));
                            }
                            return -1;
                        case 121:
                            this.in.read();
                            ObjectInputStream.this.handleReset();
                            break;
                        case 122:
                            if (iAvailable < 5) {
                                return -2;
                            }
                            this.in.readFully(this.hbuf, 0, 5);
                            int i = Bits.getInt(this.hbuf, 1);
                            if (i < 0) {
                                throw new StreamCorruptedException("illegal block data header length: " + i);
                            }
                            return i;
                    }
                } catch (EOFException e) {
                    throw new StreamCorruptedException("unexpected EOF while reading block data header");
                }
            }
        }

        private void refill() throws IOException {
            do {
                try {
                    this.pos = 0;
                    if (this.unread > 0) {
                        int i = this.in.read(this.buf, 0, Math.min(this.unread, 1024));
                        if (i >= 0) {
                            this.end = i;
                            this.unread -= i;
                        } else {
                            throw new StreamCorruptedException("unexpected EOF in middle of data block");
                        }
                    } else {
                        int blockHeader = readBlockHeader(true);
                        if (blockHeader >= 0) {
                            this.end = 0;
                            this.unread = blockHeader;
                        } else {
                            this.end = -1;
                            this.unread = 0;
                        }
                    }
                } catch (IOException e) {
                    this.pos = 0;
                    this.end = -1;
                    this.unread = 0;
                    throw e;
                }
            } while (this.pos == this.end);
        }

        int currentBlockRemaining() {
            if (this.blkmode) {
                if (this.end >= 0) {
                    return (this.end - this.pos) + this.unread;
                }
                return 0;
            }
            throw new IllegalStateException();
        }

        int peek() throws IOException {
            if (this.blkmode) {
                if (this.pos == this.end) {
                    refill();
                }
                if (this.end >= 0) {
                    return this.buf[this.pos] & Character.DIRECTIONALITY_UNDEFINED;
                }
                return -1;
            }
            return this.in.peek();
        }

        byte peekByte() throws IOException {
            int iPeek = peek();
            if (iPeek < 0) {
                throw new EOFException();
            }
            return (byte) iPeek;
        }

        @Override
        public int read() throws IOException {
            if (this.blkmode) {
                if (this.pos == this.end) {
                    refill();
                }
                if (this.end < 0) {
                    return -1;
                }
                byte[] bArr = this.buf;
                int i = this.pos;
                this.pos = i + 1;
                return bArr[i] & Character.DIRECTIONALITY_UNDEFINED;
            }
            return this.in.read();
        }

        @Override
        public int read(byte[] bArr, int i, int i2) throws IOException {
            return read(bArr, i, i2, false);
        }

        @Override
        public long skip(long j) throws IOException {
            long j2 = j;
            while (j2 > 0) {
                if (this.blkmode) {
                    if (this.pos == this.end) {
                        refill();
                    }
                    if (this.end < 0) {
                        break;
                    }
                    int iMin = (int) Math.min(j2, this.end - this.pos);
                    j2 -= (long) iMin;
                    this.pos += iMin;
                } else {
                    int i = this.in.read(this.buf, 0, (int) Math.min(j2, 1024L));
                    if (i < 0) {
                        break;
                    }
                    j2 -= (long) i;
                }
            }
            return j - j2;
        }

        @Override
        public int available() throws IOException {
            int iMin;
            int blockHeader;
            if (this.blkmode) {
                if (this.pos == this.end && this.unread == 0) {
                    do {
                        blockHeader = readBlockHeader(false);
                    } while (blockHeader == 0);
                    switch (blockHeader) {
                        case -2:
                            break;
                        case -1:
                            this.pos = 0;
                            this.end = -1;
                            break;
                        default:
                            this.pos = 0;
                            this.end = 0;
                            this.unread = blockHeader;
                            break;
                    }
                }
                if (this.unread > 0) {
                    iMin = Math.min(this.in.available(), this.unread);
                } else {
                    iMin = 0;
                }
                if (this.end >= 0) {
                    return (this.end - this.pos) + iMin;
                }
                return 0;
            }
            return this.in.available();
        }

        @Override
        public void close() throws IOException {
            if (this.blkmode) {
                this.pos = 0;
                this.end = -1;
                this.unread = 0;
            }
            this.in.close();
        }

        int read(byte[] bArr, int i, int i2, boolean z) throws IOException {
            if (i2 == 0) {
                return 0;
            }
            if (this.blkmode) {
                if (this.pos == this.end) {
                    refill();
                }
                if (this.end < 0) {
                    return -1;
                }
                int iMin = Math.min(i2, this.end - this.pos);
                System.arraycopy(this.buf, this.pos, bArr, i, iMin);
                this.pos += iMin;
                return iMin;
            }
            if (z) {
                int i3 = this.in.read(this.buf, 0, Math.min(i2, 1024));
                if (i3 > 0) {
                    System.arraycopy(this.buf, 0, bArr, i, i3);
                }
                return i3;
            }
            return this.in.read(bArr, i, i2);
        }

        @Override
        public void readFully(byte[] bArr) throws IOException {
            readFully(bArr, 0, bArr.length, false);
        }

        @Override
        public void readFully(byte[] bArr, int i, int i2) throws IOException {
            readFully(bArr, i, i2, false);
        }

        public void readFully(byte[] bArr, int i, int i2, boolean z) throws IOException {
            while (i2 > 0) {
                int i3 = read(bArr, i, i2, z);
                if (i3 < 0) {
                    throw new EOFException();
                }
                i += i3;
                i2 -= i3;
            }
        }

        @Override
        public int skipBytes(int i) throws IOException {
            return this.din.skipBytes(i);
        }

        @Override
        public boolean readBoolean() throws IOException {
            int i = read();
            if (i >= 0) {
                return i != 0;
            }
            throw new EOFException();
        }

        @Override
        public byte readByte() throws IOException {
            int i = read();
            if (i < 0) {
                throw new EOFException();
            }
            return (byte) i;
        }

        @Override
        public int readUnsignedByte() throws IOException {
            int i = read();
            if (i < 0) {
                throw new EOFException();
            }
            return i;
        }

        @Override
        public char readChar() throws IOException {
            if (!this.blkmode) {
                this.pos = 0;
                this.in.readFully(this.buf, 0, 2);
            } else if (this.end - this.pos < 2) {
                return this.din.readChar();
            }
            char c = Bits.getChar(this.buf, this.pos);
            this.pos += 2;
            return c;
        }

        @Override
        public short readShort() throws IOException {
            if (!this.blkmode) {
                this.pos = 0;
                this.in.readFully(this.buf, 0, 2);
            } else if (this.end - this.pos < 2) {
                return this.din.readShort();
            }
            short s = Bits.getShort(this.buf, this.pos);
            this.pos += 2;
            return s;
        }

        @Override
        public int readUnsignedShort() throws IOException {
            if (!this.blkmode) {
                this.pos = 0;
                this.in.readFully(this.buf, 0, 2);
            } else if (this.end - this.pos < 2) {
                return this.din.readUnsignedShort();
            }
            int i = Bits.getShort(this.buf, this.pos) & 65535;
            this.pos += 2;
            return i;
        }

        @Override
        public int readInt() throws IOException {
            if (!this.blkmode) {
                this.pos = 0;
                this.in.readFully(this.buf, 0, 4);
            } else if (this.end - this.pos < 4) {
                return this.din.readInt();
            }
            int i = Bits.getInt(this.buf, this.pos);
            this.pos += 4;
            return i;
        }

        @Override
        public float readFloat() throws IOException {
            if (!this.blkmode) {
                this.pos = 0;
                this.in.readFully(this.buf, 0, 4);
            } else if (this.end - this.pos < 4) {
                return this.din.readFloat();
            }
            float f = Bits.getFloat(this.buf, this.pos);
            this.pos += 4;
            return f;
        }

        @Override
        public long readLong() throws IOException {
            if (!this.blkmode) {
                this.pos = 0;
                this.in.readFully(this.buf, 0, 8);
            } else if (this.end - this.pos < 8) {
                return this.din.readLong();
            }
            long j = Bits.getLong(this.buf, this.pos);
            this.pos += 8;
            return j;
        }

        @Override
        public double readDouble() throws IOException {
            if (!this.blkmode) {
                this.pos = 0;
                this.in.readFully(this.buf, 0, 8);
            } else if (this.end - this.pos < 8) {
                return this.din.readDouble();
            }
            double d = Bits.getDouble(this.buf, this.pos);
            this.pos += 8;
            return d;
        }

        @Override
        public String readUTF() throws IOException {
            return readUTFBody(readUnsignedShort());
        }

        @Override
        public String readLine() throws IOException {
            return this.din.readLine();
        }

        void readBooleans(boolean[] zArr, int i, int i2) throws IOException {
            int iMin;
            int i3 = i2 + i;
            while (i < i3) {
                if (!this.blkmode) {
                    int iMin2 = Math.min(i3 - i, 1024);
                    this.in.readFully(this.buf, 0, iMin2);
                    iMin = iMin2 + i;
                    this.pos = 0;
                } else if (this.end - this.pos < 1) {
                    zArr[i] = this.din.readBoolean();
                    i++;
                } else {
                    iMin = Math.min(i3, (this.end + i) - this.pos);
                }
                while (i < iMin) {
                    byte[] bArr = this.buf;
                    int i4 = this.pos;
                    this.pos = i4 + 1;
                    zArr[i] = Bits.getBoolean(bArr, i4);
                    i++;
                }
            }
        }

        void readChars(char[] cArr, int i, int i2) throws IOException {
            int iMin;
            int i3 = i2 + i;
            while (i < i3) {
                if (!this.blkmode) {
                    int iMin2 = Math.min(i3 - i, 512);
                    this.in.readFully(this.buf, 0, iMin2 << 1);
                    iMin = iMin2 + i;
                    this.pos = 0;
                } else if (this.end - this.pos < 2) {
                    cArr[i] = this.din.readChar();
                    i++;
                } else {
                    iMin = Math.min(i3, ((this.end - this.pos) >> 1) + i);
                }
                while (i < iMin) {
                    cArr[i] = Bits.getChar(this.buf, this.pos);
                    this.pos += 2;
                    i++;
                }
            }
        }

        void readShorts(short[] sArr, int i, int i2) throws IOException {
            int iMin;
            int i3 = i2 + i;
            while (i < i3) {
                if (!this.blkmode) {
                    int iMin2 = Math.min(i3 - i, 512);
                    this.in.readFully(this.buf, 0, iMin2 << 1);
                    iMin = iMin2 + i;
                    this.pos = 0;
                } else if (this.end - this.pos < 2) {
                    sArr[i] = this.din.readShort();
                    i++;
                } else {
                    iMin = Math.min(i3, ((this.end - this.pos) >> 1) + i);
                }
                while (i < iMin) {
                    sArr[i] = Bits.getShort(this.buf, this.pos);
                    this.pos += 2;
                    i++;
                }
            }
        }

        void readInts(int[] iArr, int i, int i2) throws IOException {
            int iMin;
            int i3 = i2 + i;
            while (i < i3) {
                if (!this.blkmode) {
                    int iMin2 = Math.min(i3 - i, 256);
                    this.in.readFully(this.buf, 0, iMin2 << 2);
                    iMin = iMin2 + i;
                    this.pos = 0;
                } else if (this.end - this.pos < 4) {
                    iArr[i] = this.din.readInt();
                    i++;
                } else {
                    iMin = Math.min(i3, ((this.end - this.pos) >> 2) + i);
                }
                while (i < iMin) {
                    iArr[i] = Bits.getInt(this.buf, this.pos);
                    this.pos += 4;
                    i++;
                }
            }
        }

        void readFloats(float[] fArr, int i, int i2) throws IOException {
            int iMin;
            int i3 = i2 + i;
            while (i < i3) {
                if (!this.blkmode) {
                    iMin = Math.min(i3 - i, 256);
                    this.in.readFully(this.buf, 0, iMin << 2);
                    this.pos = 0;
                } else if (this.end - this.pos < 4) {
                    fArr[i] = this.din.readFloat();
                    i++;
                } else {
                    iMin = Math.min(i3 - i, (this.end - this.pos) >> 2);
                }
                ObjectInputStream.bytesToFloats(this.buf, this.pos, fArr, i, iMin);
                i += iMin;
                this.pos += iMin << 2;
            }
        }

        void readLongs(long[] jArr, int i, int i2) throws IOException {
            int iMin;
            int i3 = i2 + i;
            while (i < i3) {
                if (!this.blkmode) {
                    int iMin2 = Math.min(i3 - i, 128);
                    this.in.readFully(this.buf, 0, iMin2 << 3);
                    iMin = iMin2 + i;
                    this.pos = 0;
                } else if (this.end - this.pos < 8) {
                    jArr[i] = this.din.readLong();
                    i++;
                } else {
                    iMin = Math.min(i3, ((this.end - this.pos) >> 3) + i);
                }
                while (i < iMin) {
                    jArr[i] = Bits.getLong(this.buf, this.pos);
                    this.pos += 8;
                    i++;
                }
            }
        }

        void readDoubles(double[] dArr, int i, int i2) throws IOException {
            int iMin;
            int i3 = i2 + i;
            while (i < i3) {
                if (!this.blkmode) {
                    iMin = Math.min(i3 - i, 128);
                    this.in.readFully(this.buf, 0, iMin << 3);
                    this.pos = 0;
                } else if (this.end - this.pos < 8) {
                    dArr[i] = this.din.readDouble();
                    i++;
                } else {
                    iMin = Math.min(i3 - i, (this.end - this.pos) >> 3);
                }
                ObjectInputStream.bytesToDoubles(this.buf, this.pos, dArr, i, iMin);
                i += iMin;
                this.pos += iMin << 3;
            }
        }

        String readLongUTF() throws IOException {
            return readUTFBody(readLong());
        }

        private String readUTFBody(long j) throws IOException {
            StringBuilder sb = new StringBuilder();
            if (!this.blkmode) {
                this.pos = 0;
                this.end = 0;
            }
            while (j > 0) {
                int i = this.end - this.pos;
                if (i >= 3 || i == j) {
                    j -= readUTFSpan(sb, j);
                } else if (this.blkmode) {
                    j -= (long) readUTFChar(sb, j);
                } else {
                    if (i > 0) {
                        System.arraycopy(this.buf, this.pos, this.buf, 0, i);
                    }
                    this.pos = 0;
                    this.end = (int) Math.min(1024L, j);
                    this.in.readFully(this.buf, i, this.end - i);
                }
            }
            return sb.toString();
        }

        private long readUTFSpan(StringBuilder sb, long j) throws IOException {
            int i = this.pos;
            int iMin = Math.min(this.end - this.pos, 256);
            int i2 = this.pos + (j > ((long) iMin) ? iMin - 2 : (int) j);
            int i3 = 0;
            while (this.pos < i2) {
                try {
                    byte[] bArr = this.buf;
                    int i4 = this.pos;
                    this.pos = i4 + 1;
                    int i5 = bArr[i4] & Character.DIRECTIONALITY_UNDEFINED;
                    int i6 = i5 >> 4;
                    switch (i6) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                            int i7 = i3 + 1;
                            this.cbuf[i3] = (char) i5;
                            i3 = i7;
                            break;
                        default:
                            switch (i6) {
                                case 12:
                                case 13:
                                    byte[] bArr2 = this.buf;
                                    int i8 = this.pos;
                                    this.pos = i8 + 1;
                                    byte b = bArr2[i8];
                                    if ((b & DerValue.TAG_PRIVATE) == 128) {
                                        this.cbuf[i3] = (char) (((i5 & 31) << 6) | ((b & 63) << 0));
                                        i3++;
                                    } else {
                                        throw new UTFDataFormatException();
                                    }
                                    break;
                                case 14:
                                    byte b2 = this.buf[this.pos + 1];
                                    byte b3 = this.buf[this.pos + 0];
                                    this.pos += 2;
                                    if ((b3 & DerValue.TAG_PRIVATE) == 128 && (b2 & DerValue.TAG_PRIVATE) == 128) {
                                        this.cbuf[i3] = (char) (((i5 & 15) << 12) | ((b3 & 63) << 6) | ((b2 & 63) << 0));
                                        i3++;
                                    } else {
                                        throw new UTFDataFormatException();
                                    }
                                    break;
                                default:
                                    throw new UTFDataFormatException();
                            }
                            break;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    this.pos = i + ((int) j);
                    throw new UTFDataFormatException();
                } catch (Throwable th) {
                    if (this.pos - i <= j) {
                        throw th;
                    }
                    this.pos = i + ((int) j);
                    throw new UTFDataFormatException();
                }
            }
            if (this.pos - i <= j) {
                sb.append(this.cbuf, 0, i3);
                return this.pos - i;
            }
            this.pos = i + ((int) j);
            throw new UTFDataFormatException();
        }

        private int readUTFChar(StringBuilder sb, long j) throws IOException {
            int i = readByte() & Character.DIRECTIONALITY_UNDEFINED;
            int i2 = i >> 4;
            switch (i2) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    sb.append((char) i);
                    return 1;
                default:
                    switch (i2) {
                        case 12:
                        case 13:
                            if (j < 2) {
                                throw new UTFDataFormatException();
                            }
                            byte b = readByte();
                            if ((b & DerValue.TAG_PRIVATE) != 128) {
                                throw new UTFDataFormatException();
                            }
                            sb.append((char) (((b & 63) << 0) | ((i & 31) << 6)));
                            return 2;
                        case 14:
                            if (j < 3) {
                                if (j == 2) {
                                    readByte();
                                }
                                throw new UTFDataFormatException();
                            }
                            byte b2 = readByte();
                            byte b3 = readByte();
                            if ((b2 & DerValue.TAG_PRIVATE) != 128 || (b3 & DerValue.TAG_PRIVATE) != 128) {
                                throw new UTFDataFormatException();
                            }
                            sb.append((char) (((b2 & 63) << 6) | ((i & 15) << 12) | ((b3 & 63) << 0)));
                            return 3;
                        default:
                            throw new UTFDataFormatException();
                    }
            }
        }
    }

    private static class HandleTable {
        private static final byte STATUS_EXCEPTION = 3;
        private static final byte STATUS_OK = 1;
        private static final byte STATUS_UNKNOWN = 2;
        HandleList[] deps;
        Object[] entries;
        int lowDep = -1;
        int size = 0;
        byte[] status;

        HandleTable(int i) {
            this.status = new byte[i];
            this.entries = new Object[i];
            this.deps = new HandleList[i];
        }

        int assign(Object obj) {
            if (this.size >= this.entries.length) {
                grow();
            }
            this.status[this.size] = 2;
            this.entries[this.size] = obj;
            int i = this.size;
            this.size = i + 1;
            return i;
        }

        void markDependency(int i, int i2) {
            if (i == -1 || i2 == -1) {
                return;
            }
            switch (this.status[i]) {
                case 2:
                    switch (this.status[i2]) {
                        case 1:
                            return;
                        case 2:
                            if (this.deps[i2] == null) {
                                this.deps[i2] = new HandleList();
                            }
                            this.deps[i2].add(i);
                            if (this.lowDep < 0 || this.lowDep > i2) {
                                this.lowDep = i2;
                                return;
                            }
                            return;
                        case 3:
                            markException(i, (ClassNotFoundException) this.entries[i2]);
                            return;
                        default:
                            throw new InternalError();
                    }
                case 3:
                    return;
                default:
                    throw new InternalError();
            }
        }

        void markException(int i, ClassNotFoundException classNotFoundException) {
            switch (this.status[i]) {
                case 2:
                    this.status[i] = 3;
                    this.entries[i] = classNotFoundException;
                    HandleList handleList = this.deps[i];
                    if (handleList != null) {
                        int size = handleList.size();
                        for (int i2 = 0; i2 < size; i2++) {
                            markException(handleList.get(i2), classNotFoundException);
                        }
                        this.deps[i] = null;
                        return;
                    }
                    return;
                case 3:
                    return;
                default:
                    throw new InternalError();
            }
        }

        void finish(int i) {
            int i2;
            if (this.lowDep < 0) {
                i2 = i + 1;
            } else if (this.lowDep >= i) {
                i2 = this.size;
                this.lowDep = -1;
            } else {
                return;
            }
            while (i < i2) {
                switch (this.status[i]) {
                    case 1:
                    case 3:
                        break;
                    case 2:
                        this.status[i] = 1;
                        this.deps[i] = null;
                        break;
                    default:
                        throw new InternalError();
                }
                i++;
            }
        }

        void setObject(int i, Object obj) {
            switch (this.status[i]) {
                case 1:
                case 2:
                    this.entries[i] = obj;
                    return;
                case 3:
                    return;
                default:
                    throw new InternalError();
            }
        }

        Object lookupObject(int i) {
            if (i == -1 || this.status[i] == 3) {
                return null;
            }
            return this.entries[i];
        }

        ClassNotFoundException lookupException(int i) {
            if (i == -1 || this.status[i] != 3) {
                return null;
            }
            return (ClassNotFoundException) this.entries[i];
        }

        void clear() {
            Arrays.fill(this.status, 0, this.size, (byte) 0);
            Arrays.fill(this.entries, 0, this.size, (Object) null);
            Arrays.fill(this.deps, 0, this.size, (Object) null);
            this.lowDep = -1;
            this.size = 0;
        }

        int size() {
            return this.size;
        }

        private void grow() {
            int length = (this.entries.length << 1) + 1;
            byte[] bArr = new byte[length];
            Object[] objArr = new Object[length];
            HandleList[] handleListArr = new HandleList[length];
            System.arraycopy(this.status, 0, bArr, 0, this.size);
            System.arraycopy(this.entries, 0, objArr, 0, this.size);
            System.arraycopy(this.deps, 0, handleListArr, 0, this.size);
            this.status = bArr;
            this.entries = objArr;
            this.deps = handleListArr;
        }

        private static class HandleList {
            private int[] list = new int[4];
            private int size = 0;

            public void add(int i) {
                if (this.size >= this.list.length) {
                    int[] iArr = new int[this.list.length << 1];
                    System.arraycopy((Object) this.list, 0, (Object) iArr, 0, this.list.length);
                    this.list = iArr;
                }
                int[] iArr2 = this.list;
                int i2 = this.size;
                this.size = i2 + 1;
                iArr2[i2] = i;
            }

            public int get(int i) {
                if (i >= this.size) {
                    throw new ArrayIndexOutOfBoundsException();
                }
                return this.list[i];
            }

            public int size() {
                return this.size;
            }
        }
    }

    private static Object cloneArray(Object obj) {
        if (obj instanceof Object[]) {
            return ((Object[]) obj).clone();
        }
        if (obj instanceof boolean[]) {
            return ((boolean[]) obj).clone();
        }
        if (obj instanceof byte[]) {
            return ((byte[]) obj).clone();
        }
        if (obj instanceof char[]) {
            return ((char[]) obj).clone();
        }
        if (obj instanceof double[]) {
            return ((double[]) obj).clone();
        }
        if (obj instanceof float[]) {
            return ((float[]) obj).clone();
        }
        if (obj instanceof int[]) {
            return ((int[]) obj).clone();
        }
        if (obj instanceof long[]) {
            return ((long[]) obj).clone();
        }
        if (obj instanceof short[]) {
            return ((short[]) obj).clone();
        }
        throw new AssertionError();
    }
}
