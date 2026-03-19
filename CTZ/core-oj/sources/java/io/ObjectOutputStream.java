package java.io;

import java.io.ObjectStreamClass;
import java.lang.ref.ReferenceQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.reflect.misc.ReflectUtil;

public class ObjectOutputStream extends OutputStream implements ObjectOutput, ObjectStreamConstants {
    private static final boolean extendedDebugInfo = false;
    private final BlockDataOutputStream bout;
    private SerialCallbackContext curContext;
    private PutFieldImpl curPut;
    private final DebugTraceInfoStack debugInfoStack;
    private int depth;
    private final boolean enableOverride;
    private boolean enableReplace;
    private final HandleTable handles;
    private byte[] primVals;
    private int protocol = 2;
    private final ReplaceTable subs;

    public static abstract class PutField {
        public abstract void put(String str, byte b);

        public abstract void put(String str, char c);

        public abstract void put(String str, double d);

        public abstract void put(String str, float f);

        public abstract void put(String str, int i);

        public abstract void put(String str, long j);

        public abstract void put(String str, Object obj);

        public abstract void put(String str, short s);

        public abstract void put(String str, boolean z);

        @Deprecated
        public abstract void write(ObjectOutput objectOutput) throws IOException;
    }

    private static native void doublesToBytes(double[] dArr, int i, byte[] bArr, int i2, int i3);

    private static native void floatsToBytes(float[] fArr, int i, byte[] bArr, int i2, int i3);

    private static class Caches {
        static final ConcurrentMap<ObjectStreamClass.WeakClassKey, Boolean> subclassAudits = new ConcurrentHashMap();
        static final ReferenceQueue<Class<?>> subclassAuditsQueue = new ReferenceQueue<>();

        private Caches() {
        }
    }

    public ObjectOutputStream(OutputStream outputStream) throws IOException {
        verifySubclass();
        this.bout = new BlockDataOutputStream(outputStream);
        this.handles = new HandleTable(10, 3.0f);
        this.subs = new ReplaceTable(10, 3.0f);
        this.enableOverride = extendedDebugInfo;
        writeStreamHeader();
        this.bout.setBlockDataMode(true);
        this.debugInfoStack = null;
    }

    protected ObjectOutputStream() throws IOException, SecurityException {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
        }
        this.bout = null;
        this.handles = null;
        this.subs = null;
        this.enableOverride = true;
        this.debugInfoStack = null;
    }

    public void useProtocolVersion(int i) throws IOException {
        if (this.handles.size() != 0) {
            throw new IllegalStateException("stream non-empty");
        }
        switch (i) {
            case 1:
            case 2:
                this.protocol = i;
                return;
            default:
                throw new IllegalArgumentException("unknown version: " + i);
        }
    }

    @Override
    public final void writeObject(Object obj) throws IOException {
        if (this.enableOverride) {
            writeObjectOverride(obj);
            return;
        }
        try {
            writeObject0(obj, extendedDebugInfo);
        } catch (IOException e) {
            if (this.depth == 0) {
                try {
                    writeFatalException(e);
                } catch (IOException e2) {
                }
            }
            throw e;
        }
    }

    protected void writeObjectOverride(Object obj) throws IOException {
        if (!this.enableOverride) {
            throw new IOException();
        }
    }

    public void writeUnshared(Object obj) throws IOException {
        try {
            writeObject0(obj, true);
        } catch (IOException e) {
            if (this.depth == 0) {
                writeFatalException(e);
            }
            throw e;
        }
    }

    public void defaultWriteObject() throws IOException {
        SerialCallbackContext serialCallbackContext = this.curContext;
        if (serialCallbackContext == null) {
            throw new NotActiveException("not in call to writeObject");
        }
        Object obj = serialCallbackContext.getObj();
        ObjectStreamClass desc = serialCallbackContext.getDesc();
        this.bout.setBlockDataMode(extendedDebugInfo);
        defaultWriteFields(obj, desc);
        this.bout.setBlockDataMode(true);
    }

    public PutField putFields() throws IOException {
        if (this.curPut == null) {
            SerialCallbackContext serialCallbackContext = this.curContext;
            if (serialCallbackContext == null) {
                throw new NotActiveException("not in call to writeObject");
            }
            serialCallbackContext.getObj();
            this.curPut = new PutFieldImpl(serialCallbackContext.getDesc());
        }
        return this.curPut;
    }

    public void writeFields() throws IOException {
        if (this.curPut == null) {
            throw new NotActiveException("no current PutField object");
        }
        this.bout.setBlockDataMode(extendedDebugInfo);
        this.curPut.writeFields();
        this.bout.setBlockDataMode(true);
    }

    public void reset() throws IOException {
        if (this.depth != 0) {
            throw new IOException("stream active");
        }
        this.bout.setBlockDataMode(extendedDebugInfo);
        this.bout.writeByte(121);
        clear();
        this.bout.setBlockDataMode(true);
    }

    protected void annotateClass(Class<?> cls) throws IOException {
    }

    protected void annotateProxyClass(Class<?> cls) throws IOException {
    }

    protected Object replaceObject(Object obj) throws IOException {
        return obj;
    }

    protected boolean enableReplaceObject(boolean z) throws SecurityException {
        SecurityManager securityManager;
        if (z == this.enableReplace) {
            return z;
        }
        if (z && (securityManager = System.getSecurityManager()) != null) {
            securityManager.checkPermission(SUBSTITUTION_PERMISSION);
        }
        this.enableReplace = z;
        return !this.enableReplace;
    }

    protected void writeStreamHeader() throws IOException {
        this.bout.writeShort(-21267);
        this.bout.writeShort(5);
    }

    protected void writeClassDescriptor(ObjectStreamClass objectStreamClass) throws IOException {
        objectStreamClass.writeNonProxy(this);
    }

    @Override
    public void write(int i) throws IOException {
        this.bout.write(i);
    }

    @Override
    public void write(byte[] bArr) throws IOException {
        this.bout.write(bArr, 0, bArr.length, extendedDebugInfo);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        if (bArr == null) {
            throw new NullPointerException();
        }
        int i3 = i + i2;
        if (i < 0 || i2 < 0 || i3 > bArr.length || i3 < 0) {
            throw new IndexOutOfBoundsException();
        }
        this.bout.write(bArr, i, i2, extendedDebugInfo);
    }

    @Override
    public void flush() throws IOException {
        this.bout.flush();
    }

    protected void drain() throws IOException {
        this.bout.drain();
    }

    @Override
    public void close() throws IOException {
        flush();
        this.bout.close();
    }

    @Override
    public void writeBoolean(boolean z) throws IOException {
        this.bout.writeBoolean(z);
    }

    @Override
    public void writeByte(int i) throws IOException {
        this.bout.writeByte(i);
    }

    @Override
    public void writeShort(int i) throws IOException {
        this.bout.writeShort(i);
    }

    @Override
    public void writeChar(int i) throws IOException {
        this.bout.writeChar(i);
    }

    @Override
    public void writeInt(int i) throws IOException {
        this.bout.writeInt(i);
    }

    @Override
    public void writeLong(long j) throws IOException {
        this.bout.writeLong(j);
    }

    @Override
    public void writeFloat(float f) throws IOException {
        this.bout.writeFloat(f);
    }

    @Override
    public void writeDouble(double d) throws IOException {
        this.bout.writeDouble(d);
    }

    @Override
    public void writeBytes(String str) throws IOException {
        this.bout.writeBytes(str);
    }

    @Override
    public void writeChars(String str) throws IOException {
        this.bout.writeChars(str);
    }

    @Override
    public void writeUTF(String str) throws IOException {
        this.bout.writeUTF(str);
    }

    int getProtocolVersion() {
        return this.protocol;
    }

    void writeTypeString(String str) throws IOException {
        if (str == null) {
            writeNull();
            return;
        }
        int iLookup = this.handles.lookup(str);
        if (iLookup != -1) {
            writeHandle(iLookup);
        } else {
            writeString(str, extendedDebugInfo);
        }
    }

    private void verifySubclass() {
        SecurityManager securityManager;
        Class<?> cls = getClass();
        if (cls == ObjectOutputStream.class || (securityManager = System.getSecurityManager()) == null) {
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
                for (Class superclass = cls; superclass != ObjectOutputStream.class; superclass = superclass.getSuperclass()) {
                    try {
                        superclass.getDeclaredMethod("writeUnshared", Object.class);
                        return Boolean.FALSE;
                    } catch (NoSuchMethodException e) {
                        try {
                            superclass.getDeclaredMethod("putFields", (Class[]) null);
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
        this.subs.clear();
        this.handles.clear();
    }

    private void writeObject0(Object obj, boolean z) throws IOException {
        Object objInvokeWriteReplace;
        int iLookup;
        Class<?> cls;
        int iLookup2;
        boolean blockDataMode = this.bout.setBlockDataMode(extendedDebugInfo);
        this.depth++;
        try {
            Object objLookup = this.subs.lookup(obj);
            if (objLookup == null) {
                writeNull();
                return;
            }
            if (!z && (iLookup2 = this.handles.lookup(objLookup)) != -1) {
                writeHandle(iLookup2);
                return;
            }
            Class<?> cls2 = objLookup.getClass();
            ObjectStreamClass objectStreamClassLookup = ObjectStreamClass.lookup(cls2, true);
            if (objectStreamClassLookup.hasWriteReplaceMethod()) {
                objInvokeWriteReplace = objectStreamClassLookup.invokeWriteReplace(objLookup);
                if (objInvokeWriteReplace != null && (cls = objInvokeWriteReplace.getClass()) != cls2) {
                    objectStreamClassLookup = ObjectStreamClass.lookup(cls, true);
                    cls2 = cls;
                }
            } else {
                objInvokeWriteReplace = objLookup;
            }
            if (this.enableReplace) {
                Object objReplaceObject = replaceObject(objInvokeWriteReplace);
                if (objReplaceObject != objInvokeWriteReplace && objReplaceObject != null) {
                    cls2 = objReplaceObject.getClass();
                    objectStreamClassLookup = ObjectStreamClass.lookup(cls2, true);
                }
                objInvokeWriteReplace = objReplaceObject;
            }
            if (objInvokeWriteReplace != objLookup) {
                this.subs.assign(objLookup, objInvokeWriteReplace);
                if (objInvokeWriteReplace == null) {
                    writeNull();
                    return;
                } else if (!z && (iLookup = this.handles.lookup(objInvokeWriteReplace)) != -1) {
                    writeHandle(iLookup);
                    return;
                }
            }
            if (objInvokeWriteReplace instanceof Class) {
                writeClass((Class) objInvokeWriteReplace, z);
            } else if (objInvokeWriteReplace instanceof ObjectStreamClass) {
                writeClassDesc((ObjectStreamClass) objInvokeWriteReplace, z);
            } else if (objInvokeWriteReplace instanceof String) {
                writeString((String) objInvokeWriteReplace, z);
            } else if (cls2.isArray()) {
                writeArray(objInvokeWriteReplace, objectStreamClassLookup, z);
            } else if (objInvokeWriteReplace instanceof Enum) {
                writeEnum((Enum) objInvokeWriteReplace, objectStreamClassLookup, z);
            } else {
                if (!(objInvokeWriteReplace instanceof Serializable)) {
                    throw new NotSerializableException(cls2.getName());
                }
                writeOrdinaryObject(objInvokeWriteReplace, objectStreamClassLookup, z);
            }
        } finally {
            this.depth--;
            this.bout.setBlockDataMode(blockDataMode);
        }
    }

    private void writeNull() throws IOException {
        this.bout.writeByte(112);
    }

    private void writeHandle(int i) throws IOException {
        this.bout.writeByte(113);
        this.bout.writeInt(ObjectStreamConstants.baseWireHandle + i);
    }

    private void writeClass(Class<?> cls, boolean z) throws IOException {
        this.bout.writeByte(118);
        writeClassDesc(ObjectStreamClass.lookup(cls, true), extendedDebugInfo);
        HandleTable handleTable = this.handles;
        if (z) {
            cls = null;
        }
        handleTable.assign(cls);
    }

    private void writeClassDesc(ObjectStreamClass objectStreamClass, boolean z) throws IOException {
        int iLookup;
        if (objectStreamClass == null) {
            writeNull();
            return;
        }
        if (!z && (iLookup = this.handles.lookup(objectStreamClass)) != -1) {
            writeHandle(iLookup);
        } else if (objectStreamClass.isProxy()) {
            writeProxyDesc(objectStreamClass, z);
        } else {
            writeNonProxyDesc(objectStreamClass, z);
        }
    }

    private boolean isCustomSubclass() {
        if (getClass().getClassLoader() != ObjectOutputStream.class.getClassLoader()) {
            return true;
        }
        return extendedDebugInfo;
    }

    private void writeProxyDesc(ObjectStreamClass objectStreamClass, boolean z) throws IOException {
        this.bout.writeByte(125);
        this.handles.assign(z ? null : objectStreamClass);
        Class<?> clsForClass = objectStreamClass.forClass();
        Class<?>[] interfaces = clsForClass.getInterfaces();
        this.bout.writeInt(interfaces.length);
        for (Class<?> cls : interfaces) {
            this.bout.writeUTF(cls.getName());
        }
        this.bout.setBlockDataMode(true);
        if (clsForClass != null && isCustomSubclass()) {
            ReflectUtil.checkPackageAccess(clsForClass);
        }
        annotateProxyClass(clsForClass);
        this.bout.setBlockDataMode(extendedDebugInfo);
        this.bout.writeByte(120);
        writeClassDesc(objectStreamClass.getSuperDesc(), extendedDebugInfo);
    }

    private void writeNonProxyDesc(ObjectStreamClass objectStreamClass, boolean z) throws IOException {
        this.bout.writeByte(114);
        this.handles.assign(z ? null : objectStreamClass);
        if (this.protocol == 1) {
            objectStreamClass.writeNonProxy(this);
        } else {
            writeClassDescriptor(objectStreamClass);
        }
        Class<?> clsForClass = objectStreamClass.forClass();
        this.bout.setBlockDataMode(true);
        if (clsForClass != null && isCustomSubclass()) {
            ReflectUtil.checkPackageAccess(clsForClass);
        }
        annotateClass(clsForClass);
        this.bout.setBlockDataMode(extendedDebugInfo);
        this.bout.writeByte(120);
        writeClassDesc(objectStreamClass.getSuperDesc(), extendedDebugInfo);
    }

    private void writeString(String str, boolean z) throws IOException {
        this.handles.assign(z ? null : str);
        long uTFLength = this.bout.getUTFLength(str);
        if (uTFLength <= 65535) {
            this.bout.writeByte(116);
            this.bout.writeUTF(str, uTFLength);
        } else {
            this.bout.writeByte(124);
            this.bout.writeLongUTF(str, uTFLength);
        }
    }

    private void writeArray(Object obj, ObjectStreamClass objectStreamClass, boolean z) throws IOException {
        this.bout.writeByte(117);
        writeClassDesc(objectStreamClass, extendedDebugInfo);
        this.handles.assign(z ? null : obj);
        Class<?> componentType = objectStreamClass.forClass().getComponentType();
        if (componentType.isPrimitive()) {
            if (componentType == Integer.TYPE) {
                int[] iArr = (int[]) obj;
                this.bout.writeInt(iArr.length);
                this.bout.writeInts(iArr, 0, iArr.length);
                return;
            }
            if (componentType == Byte.TYPE) {
                byte[] bArr = (byte[]) obj;
                this.bout.writeInt(bArr.length);
                this.bout.write(bArr, 0, bArr.length, true);
                return;
            }
            if (componentType == Long.TYPE) {
                long[] jArr = (long[]) obj;
                this.bout.writeInt(jArr.length);
                this.bout.writeLongs(jArr, 0, jArr.length);
                return;
            }
            if (componentType == Float.TYPE) {
                float[] fArr = (float[]) obj;
                this.bout.writeInt(fArr.length);
                this.bout.writeFloats(fArr, 0, fArr.length);
                return;
            }
            if (componentType == Double.TYPE) {
                double[] dArr = (double[]) obj;
                this.bout.writeInt(dArr.length);
                this.bout.writeDoubles(dArr, 0, dArr.length);
                return;
            }
            if (componentType == Short.TYPE) {
                short[] sArr = (short[]) obj;
                this.bout.writeInt(sArr.length);
                this.bout.writeShorts(sArr, 0, sArr.length);
                return;
            } else if (componentType == Character.TYPE) {
                char[] cArr = (char[]) obj;
                this.bout.writeInt(cArr.length);
                this.bout.writeChars(cArr, 0, cArr.length);
                return;
            } else {
                if (componentType == Boolean.TYPE) {
                    boolean[] zArr = (boolean[]) obj;
                    this.bout.writeInt(zArr.length);
                    this.bout.writeBooleans(zArr, 0, zArr.length);
                    return;
                }
                throw new InternalError();
            }
        }
        Object[] objArr = (Object[]) obj;
        this.bout.writeInt(objArr.length);
        for (Object obj2 : objArr) {
            try {
                writeObject0(obj2, extendedDebugInfo);
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private void writeEnum(Enum<?> r4, ObjectStreamClass objectStreamClass, boolean z) throws IOException {
        this.bout.writeByte(126);
        ObjectStreamClass superDesc = objectStreamClass.getSuperDesc();
        if (superDesc.forClass() != Enum.class) {
            objectStreamClass = superDesc;
        }
        writeClassDesc(objectStreamClass, extendedDebugInfo);
        this.handles.assign(z ? null : r4);
        writeString(r4.name(), extendedDebugInfo);
    }

    private void writeOrdinaryObject(Object obj, ObjectStreamClass objectStreamClass, boolean z) throws IOException {
        objectStreamClass.checkSerialize();
        this.bout.writeByte(115);
        writeClassDesc(objectStreamClass, extendedDebugInfo);
        this.handles.assign(z ? null : obj);
        if (objectStreamClass.isExternalizable() && !objectStreamClass.isProxy()) {
            writeExternalData((Externalizable) obj);
        } else {
            writeSerialData(obj, objectStreamClass);
        }
    }

    private void writeExternalData(Externalizable externalizable) throws IOException {
        PutFieldImpl putFieldImpl = this.curPut;
        this.curPut = null;
        SerialCallbackContext serialCallbackContext = this.curContext;
        try {
            this.curContext = null;
            if (this.protocol == 1) {
                externalizable.writeExternal(this);
            } else {
                this.bout.setBlockDataMode(true);
                externalizable.writeExternal(this);
                this.bout.setBlockDataMode(extendedDebugInfo);
                this.bout.writeByte(120);
            }
            this.curContext = serialCallbackContext;
            this.curPut = putFieldImpl;
        } catch (Throwable th) {
            this.curContext = serialCallbackContext;
            throw th;
        }
    }

    private void writeSerialData(Object obj, ObjectStreamClass objectStreamClass) throws IOException {
        for (ObjectStreamClass.ClassDataSlot classDataSlot : objectStreamClass.getClassDataLayout()) {
            ObjectStreamClass objectStreamClass2 = classDataSlot.desc;
            if (objectStreamClass2.hasWriteObjectMethod()) {
                PutFieldImpl putFieldImpl = this.curPut;
                this.curPut = null;
                SerialCallbackContext serialCallbackContext = this.curContext;
                try {
                    this.curContext = new SerialCallbackContext(obj, objectStreamClass2);
                    this.bout.setBlockDataMode(true);
                    objectStreamClass2.invokeWriteObject(obj, this);
                    this.bout.setBlockDataMode(extendedDebugInfo);
                    this.bout.writeByte(120);
                    this.curContext.setUsed();
                    this.curContext = serialCallbackContext;
                    this.curPut = putFieldImpl;
                } catch (Throwable th) {
                    this.curContext.setUsed();
                    this.curContext = serialCallbackContext;
                    throw th;
                }
            } else {
                defaultWriteFields(obj, objectStreamClass2);
            }
        }
    }

    private void defaultWriteFields(Object obj, ObjectStreamClass objectStreamClass) throws IOException {
        Class<?> clsForClass = objectStreamClass.forClass();
        if (clsForClass != null && obj != null && !clsForClass.isInstance(obj)) {
            throw new ClassCastException();
        }
        objectStreamClass.checkDefaultSerialize();
        int primDataSize = objectStreamClass.getPrimDataSize();
        if (this.primVals == null || this.primVals.length < primDataSize) {
            this.primVals = new byte[primDataSize];
        }
        objectStreamClass.getPrimFieldValues(obj, this.primVals);
        this.bout.write(this.primVals, 0, primDataSize, extendedDebugInfo);
        ObjectStreamField[] fields = objectStreamClass.getFields(extendedDebugInfo);
        Object[] objArr = new Object[objectStreamClass.getNumObjFields()];
        int length = fields.length - objArr.length;
        objectStreamClass.getObjFieldValues(obj, objArr);
        for (int i = 0; i < objArr.length; i++) {
            writeObject0(objArr[i], fields[length + i].isUnshared());
        }
    }

    private void writeFatalException(IOException iOException) throws IOException {
        clear();
        boolean blockDataMode = this.bout.setBlockDataMode(extendedDebugInfo);
        try {
            this.bout.writeByte(123);
            writeObject0(iOException, extendedDebugInfo);
            clear();
        } finally {
            this.bout.setBlockDataMode(blockDataMode);
        }
    }

    private class PutFieldImpl extends PutField {
        private final ObjectStreamClass desc;
        private final Object[] objVals;
        private final byte[] primVals;

        PutFieldImpl(ObjectStreamClass objectStreamClass) {
            this.desc = objectStreamClass;
            this.primVals = new byte[objectStreamClass.getPrimDataSize()];
            this.objVals = new Object[objectStreamClass.getNumObjFields()];
        }

        @Override
        public void put(String str, boolean z) {
            Bits.putBoolean(this.primVals, getFieldOffset(str, Boolean.TYPE), z);
        }

        @Override
        public void put(String str, byte b) {
            this.primVals[getFieldOffset(str, Byte.TYPE)] = b;
        }

        @Override
        public void put(String str, char c) {
            Bits.putChar(this.primVals, getFieldOffset(str, Character.TYPE), c);
        }

        @Override
        public void put(String str, short s) {
            Bits.putShort(this.primVals, getFieldOffset(str, Short.TYPE), s);
        }

        @Override
        public void put(String str, int i) {
            Bits.putInt(this.primVals, getFieldOffset(str, Integer.TYPE), i);
        }

        @Override
        public void put(String str, float f) {
            Bits.putFloat(this.primVals, getFieldOffset(str, Float.TYPE), f);
        }

        @Override
        public void put(String str, long j) {
            Bits.putLong(this.primVals, getFieldOffset(str, Long.TYPE), j);
        }

        @Override
        public void put(String str, double d) {
            Bits.putDouble(this.primVals, getFieldOffset(str, Double.TYPE), d);
        }

        @Override
        public void put(String str, Object obj) {
            this.objVals[getFieldOffset(str, Object.class)] = obj;
        }

        @Override
        public void write(ObjectOutput objectOutput) throws IOException {
            if (ObjectOutputStream.this != objectOutput) {
                throw new IllegalArgumentException("wrong stream");
            }
            objectOutput.write(this.primVals, 0, this.primVals.length);
            ObjectStreamField[] fields = this.desc.getFields(ObjectOutputStream.extendedDebugInfo);
            int length = fields.length - this.objVals.length;
            for (int i = 0; i < this.objVals.length; i++) {
                if (fields[length + i].isUnshared()) {
                    throw new IOException("cannot write unshared object");
                }
                objectOutput.writeObject(this.objVals[i]);
            }
        }

        void writeFields() throws IOException {
            ObjectOutputStream.this.bout.write(this.primVals, 0, this.primVals.length, ObjectOutputStream.extendedDebugInfo);
            ObjectStreamField[] fields = this.desc.getFields(ObjectOutputStream.extendedDebugInfo);
            int length = fields.length - this.objVals.length;
            for (int i = 0; i < this.objVals.length; i++) {
                ObjectOutputStream.this.writeObject0(this.objVals[i], fields[length + i].isUnshared());
            }
        }

        private int getFieldOffset(String str, Class<?> cls) {
            ObjectStreamField field = this.desc.getField(str, cls);
            if (field == null) {
                throw new IllegalArgumentException("no such field " + str + " with type " + ((Object) cls));
            }
            return field.getOffset();
        }
    }

    private static class BlockDataOutputStream extends OutputStream implements DataOutput {
        private static final int CHAR_BUF_SIZE = 256;
        private static final int MAX_BLOCK_SIZE = 1024;
        private static final int MAX_HEADER_SIZE = 5;
        private final OutputStream out;
        private boolean warnOnceWhenWriting;
        private final byte[] buf = new byte[1024];
        private final byte[] hbuf = new byte[5];
        private final char[] cbuf = new char[256];
        private boolean blkmode = ObjectOutputStream.extendedDebugInfo;
        private int pos = 0;
        private final DataOutputStream dout = new DataOutputStream(this);

        BlockDataOutputStream(OutputStream outputStream) {
            this.out = outputStream;
        }

        boolean setBlockDataMode(boolean z) throws IOException {
            if (this.blkmode == z) {
                return this.blkmode;
            }
            drain();
            this.blkmode = z;
            return !this.blkmode;
        }

        boolean getBlockDataMode() {
            return this.blkmode;
        }

        private void warnIfClosed() {
            if (this.warnOnceWhenWriting) {
                System.logW("The app is relying on undefined behavior. Attempting to write to a closed ObjectOutputStream could produce corrupt output in a future release of Android.", new IOException("Stream Closed"));
                this.warnOnceWhenWriting = ObjectOutputStream.extendedDebugInfo;
            }
        }

        @Override
        public void write(int i) throws IOException {
            if (this.pos >= 1024) {
                drain();
            }
            byte[] bArr = this.buf;
            int i2 = this.pos;
            this.pos = i2 + 1;
            bArr[i2] = (byte) i;
        }

        @Override
        public void write(byte[] bArr) throws IOException {
            write(bArr, 0, bArr.length, ObjectOutputStream.extendedDebugInfo);
        }

        @Override
        public void write(byte[] bArr, int i, int i2) throws IOException {
            write(bArr, i, i2, ObjectOutputStream.extendedDebugInfo);
        }

        @Override
        public void flush() throws IOException {
            drain();
            this.out.flush();
        }

        @Override
        public void close() throws IOException {
            flush();
            this.out.close();
            this.warnOnceWhenWriting = true;
        }

        void write(byte[] bArr, int i, int i2, boolean z) throws IOException {
            if (!z && !this.blkmode) {
                drain();
                this.out.write(bArr, i, i2);
                warnIfClosed();
                return;
            }
            while (i2 > 0) {
                if (this.pos >= 1024) {
                    drain();
                }
                if (i2 >= 1024 && !z && this.pos == 0) {
                    writeBlockHeader(1024);
                    this.out.write(bArr, i, 1024);
                    i += 1024;
                    i2 -= 1024;
                } else {
                    int iMin = Math.min(i2, 1024 - this.pos);
                    System.arraycopy(bArr, i, this.buf, this.pos, iMin);
                    this.pos += iMin;
                    i += iMin;
                    i2 -= iMin;
                }
            }
            warnIfClosed();
        }

        void drain() throws IOException {
            if (this.pos == 0) {
                return;
            }
            if (this.blkmode) {
                writeBlockHeader(this.pos);
            }
            this.out.write(this.buf, 0, this.pos);
            this.pos = 0;
            warnIfClosed();
        }

        private void writeBlockHeader(int i) throws IOException {
            if (i <= 255) {
                this.hbuf[0] = ObjectStreamConstants.TC_BLOCKDATA;
                this.hbuf[1] = (byte) i;
                this.out.write(this.hbuf, 0, 2);
            } else {
                this.hbuf[0] = ObjectStreamConstants.TC_BLOCKDATALONG;
                Bits.putInt(this.hbuf, 1, i);
                this.out.write(this.hbuf, 0, 5);
            }
            warnIfClosed();
        }

        @Override
        public void writeBoolean(boolean z) throws IOException {
            if (this.pos >= 1024) {
                drain();
            }
            byte[] bArr = this.buf;
            int i = this.pos;
            this.pos = i + 1;
            Bits.putBoolean(bArr, i, z);
        }

        @Override
        public void writeByte(int i) throws IOException {
            if (this.pos >= 1024) {
                drain();
            }
            byte[] bArr = this.buf;
            int i2 = this.pos;
            this.pos = i2 + 1;
            bArr[i2] = (byte) i;
        }

        @Override
        public void writeChar(int i) throws IOException {
            if (this.pos + 2 <= 1024) {
                Bits.putChar(this.buf, this.pos, (char) i);
                this.pos += 2;
            } else {
                this.dout.writeChar(i);
            }
        }

        @Override
        public void writeShort(int i) throws IOException {
            if (this.pos + 2 <= 1024) {
                Bits.putShort(this.buf, this.pos, (short) i);
                this.pos += 2;
            } else {
                this.dout.writeShort(i);
            }
        }

        @Override
        public void writeInt(int i) throws IOException {
            if (this.pos + 4 <= 1024) {
                Bits.putInt(this.buf, this.pos, i);
                this.pos += 4;
            } else {
                this.dout.writeInt(i);
            }
        }

        @Override
        public void writeFloat(float f) throws IOException {
            if (this.pos + 4 <= 1024) {
                Bits.putFloat(this.buf, this.pos, f);
                this.pos += 4;
            } else {
                this.dout.writeFloat(f);
            }
        }

        @Override
        public void writeLong(long j) throws IOException {
            if (this.pos + 8 <= 1024) {
                Bits.putLong(this.buf, this.pos, j);
                this.pos += 8;
            } else {
                this.dout.writeLong(j);
            }
        }

        @Override
        public void writeDouble(double d) throws IOException {
            if (this.pos + 8 <= 1024) {
                Bits.putDouble(this.buf, this.pos, d);
                this.pos += 8;
            } else {
                this.dout.writeDouble(d);
            }
        }

        @Override
        public void writeBytes(String str) throws IOException {
            int length = str.length();
            int i = 0;
            int i2 = 0;
            int i3 = 0;
            while (i < length) {
                if (i2 >= i3) {
                    int iMin = Math.min(length - i, 256);
                    str.getChars(i, i + iMin, this.cbuf, 0);
                    i3 = iMin;
                    i2 = 0;
                }
                if (this.pos >= 1024) {
                    drain();
                }
                int iMin2 = Math.min(i3 - i2, 1024 - this.pos);
                int i4 = this.pos + iMin2;
                while (this.pos < i4) {
                    byte[] bArr = this.buf;
                    int i5 = this.pos;
                    this.pos = i5 + 1;
                    bArr[i5] = (byte) this.cbuf[i2];
                    i2++;
                }
                i += iMin2;
            }
        }

        @Override
        public void writeChars(String str) throws IOException {
            int length = str.length();
            int i = 0;
            while (i < length) {
                int iMin = Math.min(length - i, 256);
                int i2 = i + iMin;
                str.getChars(i, i2, this.cbuf, 0);
                writeChars(this.cbuf, 0, iMin);
                i = i2;
            }
        }

        @Override
        public void writeUTF(String str) throws IOException {
            writeUTF(str, getUTFLength(str));
        }

        void writeBooleans(boolean[] zArr, int i, int i2) throws IOException {
            int i3 = i2 + i;
            while (i < i3) {
                if (this.pos >= 1024) {
                    drain();
                }
                int iMin = Math.min(i3, (1024 - this.pos) + i);
                while (i < iMin) {
                    byte[] bArr = this.buf;
                    int i4 = this.pos;
                    this.pos = i4 + 1;
                    Bits.putBoolean(bArr, i4, zArr[i]);
                    i++;
                }
            }
        }

        void writeChars(char[] cArr, int i, int i2) throws IOException {
            int i3 = i2 + i;
            while (i < i3) {
                if (this.pos <= 1022) {
                    int iMin = Math.min(i3, ((1024 - this.pos) >> 1) + i);
                    while (i < iMin) {
                        Bits.putChar(this.buf, this.pos, cArr[i]);
                        this.pos += 2;
                        i++;
                    }
                } else {
                    this.dout.writeChar(cArr[i]);
                    i++;
                }
            }
        }

        void writeShorts(short[] sArr, int i, int i2) throws IOException {
            int i3 = i2 + i;
            while (i < i3) {
                if (this.pos <= 1022) {
                    int iMin = Math.min(i3, ((1024 - this.pos) >> 1) + i);
                    while (i < iMin) {
                        Bits.putShort(this.buf, this.pos, sArr[i]);
                        this.pos += 2;
                        i++;
                    }
                } else {
                    this.dout.writeShort(sArr[i]);
                    i++;
                }
            }
        }

        void writeInts(int[] iArr, int i, int i2) throws IOException {
            int i3 = i2 + i;
            while (i < i3) {
                if (this.pos <= 1020) {
                    int iMin = Math.min(i3, ((1024 - this.pos) >> 2) + i);
                    while (i < iMin) {
                        Bits.putInt(this.buf, this.pos, iArr[i]);
                        this.pos += 4;
                        i++;
                    }
                } else {
                    this.dout.writeInt(iArr[i]);
                    i++;
                }
            }
        }

        void writeFloats(float[] fArr, int i, int i2) throws IOException {
            int i3 = i2 + i;
            while (i < i3) {
                if (this.pos <= 1020) {
                    int iMin = Math.min(i3 - i, (1024 - this.pos) >> 2);
                    ObjectOutputStream.floatsToBytes(fArr, i, this.buf, this.pos, iMin);
                    i += iMin;
                    this.pos += iMin << 2;
                } else {
                    this.dout.writeFloat(fArr[i]);
                    i++;
                }
            }
        }

        void writeLongs(long[] jArr, int i, int i2) throws IOException {
            int i3 = i2 + i;
            while (i < i3) {
                if (this.pos <= 1016) {
                    int iMin = Math.min(i3, ((1024 - this.pos) >> 3) + i);
                    while (i < iMin) {
                        Bits.putLong(this.buf, this.pos, jArr[i]);
                        this.pos += 8;
                        i++;
                    }
                } else {
                    this.dout.writeLong(jArr[i]);
                    i++;
                }
            }
        }

        void writeDoubles(double[] dArr, int i, int i2) throws IOException {
            int i3 = i2 + i;
            while (i < i3) {
                if (this.pos <= 1016) {
                    int iMin = Math.min(i3 - i, (1024 - this.pos) >> 3);
                    ObjectOutputStream.doublesToBytes(dArr, i, this.buf, this.pos, iMin);
                    i += iMin;
                    this.pos += iMin << 3;
                } else {
                    this.dout.writeDouble(dArr[i]);
                    i++;
                }
            }
        }

        long getUTFLength(String str) {
            long j;
            int length = str.length();
            long j2 = 0;
            int i = 0;
            while (i < length) {
                int iMin = Math.min(length - i, 256);
                int i2 = i + iMin;
                str.getChars(i, i2, this.cbuf, 0);
                for (int i3 = 0; i3 < iMin; i3++) {
                    char c = this.cbuf[i3];
                    if (c >= 1 && c <= 127) {
                        j = 1;
                    } else if (c > 2047) {
                        j = 3;
                    } else {
                        j = 2;
                    }
                    j2 += j;
                }
                i = i2;
            }
            return j2;
        }

        void writeUTF(String str, long j) throws IOException {
            if (j > 65535) {
                throw new UTFDataFormatException();
            }
            writeShort((int) j);
            if (j == str.length()) {
                writeBytes(str);
            } else {
                writeUTFBody(str);
            }
        }

        void writeLongUTF(String str) throws IOException {
            writeLongUTF(str, getUTFLength(str));
        }

        void writeLongUTF(String str, long j) throws IOException {
            writeLong(j);
            if (j == str.length()) {
                writeBytes(str);
            } else {
                writeUTFBody(str);
            }
        }

        private void writeUTFBody(String str) throws IOException {
            int length = str.length();
            int i = 0;
            while (i < length) {
                int iMin = Math.min(length - i, 256);
                int i2 = i + iMin;
                str.getChars(i, i2, this.cbuf, 0);
                for (int i3 = 0; i3 < iMin; i3++) {
                    char c = this.cbuf[i3];
                    if (this.pos <= 1021) {
                        if (c <= 127 && c != 0) {
                            byte[] bArr = this.buf;
                            int i4 = this.pos;
                            this.pos = i4 + 1;
                            bArr[i4] = (byte) c;
                        } else if (c > 2047) {
                            this.buf[this.pos + 2] = (byte) (((c >> 0) & 63) | 128);
                            this.buf[this.pos + 1] = (byte) (((c >> 6) & 63) | 128);
                            this.buf[this.pos + 0] = (byte) (((c >> '\f') & 15) | 224);
                            this.pos += 3;
                        } else {
                            this.buf[this.pos + 1] = (byte) (((c >> 0) & 63) | 128);
                            this.buf[this.pos + 0] = (byte) (((c >> 6) & 31) | 192);
                            this.pos += 2;
                        }
                    } else if (c <= 127 && c != 0) {
                        write(c);
                    } else if (c > 2047) {
                        write(((c >> '\f') & 15) | 224);
                        write(((c >> 6) & 63) | 128);
                        write(((c >> 0) & 63) | 128);
                    } else {
                        write(((c >> 6) & 31) | 192);
                        write(((c >> 0) & 63) | 128);
                    }
                }
                i = i2;
            }
        }
    }

    private static class HandleTable {
        private final float loadFactor;
        private int[] next;
        private Object[] objs;
        private int size;
        private int[] spine;
        private int threshold;

        HandleTable(int i, float f) {
            this.loadFactor = f;
            this.spine = new int[i];
            this.next = new int[i];
            this.objs = new Object[i];
            this.threshold = (int) (i * f);
            clear();
        }

        int assign(Object obj) {
            if (this.size >= this.next.length) {
                growEntries();
            }
            if (this.size >= this.threshold) {
                growSpine();
            }
            insert(obj, this.size);
            int i = this.size;
            this.size = i + 1;
            return i;
        }

        int lookup(Object obj) {
            if (this.size == 0) {
                return -1;
            }
            int i = this.spine[hash(obj) % this.spine.length];
            while (i >= 0) {
                if (this.objs[i] != obj) {
                    i = this.next[i];
                } else {
                    return i;
                }
            }
            return -1;
        }

        void clear() {
            Arrays.fill(this.spine, -1);
            Arrays.fill(this.objs, 0, this.size, (Object) null);
            this.size = 0;
        }

        int size() {
            return this.size;
        }

        private void insert(Object obj, int i) {
            int iHash = hash(obj) % this.spine.length;
            this.objs[i] = obj;
            this.next[i] = this.spine[iHash];
            this.spine[iHash] = i;
        }

        private void growSpine() {
            this.spine = new int[(this.spine.length << 1) + 1];
            this.threshold = (int) (this.spine.length * this.loadFactor);
            Arrays.fill(this.spine, -1);
            for (int i = 0; i < this.size; i++) {
                insert(this.objs[i], i);
            }
        }

        private void growEntries() {
            int length = (this.next.length << 1) + 1;
            int[] iArr = new int[length];
            System.arraycopy((Object) this.next, 0, (Object) iArr, 0, this.size);
            this.next = iArr;
            Object[] objArr = new Object[length];
            System.arraycopy(this.objs, 0, objArr, 0, this.size);
            this.objs = objArr;
        }

        private int hash(Object obj) {
            return System.identityHashCode(obj) & Integer.MAX_VALUE;
        }
    }

    private static class ReplaceTable {
        private final HandleTable htab;
        private Object[] reps;

        ReplaceTable(int i, float f) {
            this.htab = new HandleTable(i, f);
            this.reps = new Object[i];
        }

        void assign(Object obj, Object obj2) {
            int iAssign = this.htab.assign(obj);
            while (iAssign >= this.reps.length) {
                grow();
            }
            this.reps[iAssign] = obj2;
        }

        Object lookup(Object obj) {
            int iLookup = this.htab.lookup(obj);
            return iLookup >= 0 ? this.reps[iLookup] : obj;
        }

        void clear() {
            Arrays.fill(this.reps, 0, this.htab.size(), (Object) null);
            this.htab.clear();
        }

        int size() {
            return this.htab.size();
        }

        private void grow() {
            Object[] objArr = new Object[(this.reps.length << 1) + 1];
            System.arraycopy(this.reps, 0, objArr, 0, this.reps.length);
            this.reps = objArr;
        }
    }

    private static class DebugTraceInfoStack {
        private final List<String> stack = new ArrayList();

        DebugTraceInfoStack() {
        }

        void clear() {
            this.stack.clear();
        }

        void pop() {
            this.stack.remove(this.stack.size() - 1);
        }

        void push(String str) {
            this.stack.add("\t- " + str);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (!this.stack.isEmpty()) {
                int size = this.stack.size();
                while (size > 0) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(this.stack.get(size - 1));
                    sb2.append(size != 1 ? "\n" : "");
                    sb.append(sb2.toString());
                    size--;
                }
            }
            return sb.toString();
        }
    }
}
