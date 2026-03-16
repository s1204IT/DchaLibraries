package java.io;

import dalvik.bytecode.Opcodes;
import java.io.EmulatedFields;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteOrder;
import java.nio.charset.ModifiedUtf8;
import java.util.List;
import libcore.io.Memory;

public class ObjectOutputStream extends OutputStream implements ObjectOutput, ObjectStreamConstants {
    private static final byte NOT_SC_BLOCK_DATA = -9;
    private ObjectStreamClass currentClass;
    private int currentHandle;
    private Object currentObject;
    private EmulatedFieldsForDumping currentPutField;
    private boolean enableReplace;
    private int nestedLevels;
    private SerializationHandleMap objectsWritten;
    private DataOutputStream output;
    private DataOutputStream primitiveTypes;
    private ByteArrayOutputStream primitiveTypesBuffer;
    private int protocolVersion;
    private final ObjectStreamClass proxyClassDesc;
    private boolean subclassOverridingImplementation;

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

    protected ObjectOutputStream() throws IOException {
        this.proxyClassDesc = ObjectStreamClass.lookup(Proxy.class);
        this.subclassOverridingImplementation = true;
    }

    public ObjectOutputStream(OutputStream output) throws IOException {
        this.proxyClassDesc = ObjectStreamClass.lookup(Proxy.class);
        this.output = output instanceof DataOutputStream ? (DataOutputStream) output : new DataOutputStream(output);
        this.enableReplace = false;
        this.protocolVersion = 2;
        this.subclassOverridingImplementation = false;
        resetState();
        this.primitiveTypes = this.output;
        writeStreamHeader();
        this.primitiveTypes = null;
    }

    protected void annotateClass(Class<?> aClass) throws IOException {
    }

    protected void annotateProxyClass(Class<?> aClass) throws IOException {
    }

    private void checkWritePrimitiveTypes() {
        if (this.primitiveTypes == null) {
            this.primitiveTypesBuffer = new ByteArrayOutputStream(128);
            this.primitiveTypes = new DataOutputStream(this.primitiveTypesBuffer);
        }
    }

    @Override
    public void close() throws IOException {
        flush();
        this.output.close();
    }

    private void computePutField() {
        this.currentPutField = new EmulatedFieldsForDumping(this, this.currentClass);
    }

    public void defaultWriteObject() throws IOException {
        if (this.currentObject == null) {
            throw new NotActiveException();
        }
        writeFieldValues(this.currentObject, this.currentClass);
    }

    protected void drain() throws IOException {
        if (this.primitiveTypes != null && this.primitiveTypesBuffer != null) {
            int offset = 0;
            byte[] written = this.primitiveTypesBuffer.toByteArray();
            while (offset < written.length) {
                int toWrite = written.length - offset > 1024 ? 1024 : written.length - offset;
                if (toWrite < 256) {
                    this.output.writeByte(Opcodes.OP_INVOKE_STATIC_RANGE);
                    this.output.writeByte((byte) toWrite);
                } else {
                    this.output.writeByte(122);
                    this.output.writeInt(toWrite);
                }
                this.output.write(written, offset, toWrite);
                offset += toWrite;
            }
            this.primitiveTypes = null;
            this.primitiveTypesBuffer = null;
        }
    }

    private int dumpCycle(Object obj) throws IOException {
        int handle = this.objectsWritten.get(obj);
        if (handle == -1) {
            return -1;
        }
        writeCyclicReference(handle);
        return handle;
    }

    protected boolean enableReplaceObject(boolean enable) {
        boolean originalValue = this.enableReplace;
        this.enableReplace = enable;
        return originalValue;
    }

    @Override
    public void flush() throws IOException {
        drain();
        this.output.flush();
    }

    private int nextHandle() {
        int i = this.currentHandle;
        this.currentHandle = i + 1;
        return i;
    }

    public PutField putFields() throws IOException {
        if (this.currentObject == null) {
            throw new NotActiveException();
        }
        if (this.currentPutField == null) {
            computePutField();
        }
        return this.currentPutField;
    }

    private int registerObjectWritten(Object obj) {
        int handle = nextHandle();
        this.objectsWritten.put(obj, handle);
        return handle;
    }

    private void removeUnsharedReference(Object obj, int previousHandle) {
        if (previousHandle != -1) {
            this.objectsWritten.put(obj, previousHandle);
        } else {
            this.objectsWritten.remove(obj);
        }
    }

    protected Object replaceObject(Object object) throws IOException {
        return object;
    }

    public void reset() throws IOException {
        drain();
        this.output.writeByte(121);
        resetState();
    }

    private void resetSeenObjects() {
        this.objectsWritten = new SerializationHandleMap();
        this.currentHandle = ObjectStreamConstants.baseWireHandle;
    }

    private void resetState() {
        resetSeenObjects();
        this.nestedLevels = 0;
    }

    public void useProtocolVersion(int version) throws IOException {
        if (!this.objectsWritten.isEmpty()) {
            throw new IllegalStateException("Cannot set protocol version when stream in use");
        }
        if (version != 1 && version != 2) {
            throw new IllegalArgumentException("Unknown protocol: " + version);
        }
        this.protocolVersion = version;
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.write(buffer, offset, length);
    }

    @Override
    public void write(int value) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.write(value);
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.writeBoolean(value);
    }

    @Override
    public void writeByte(int value) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.writeByte(value);
    }

    @Override
    public void writeBytes(String value) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.writeBytes(value);
    }

    @Override
    public void writeChar(int value) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.writeChar(value);
    }

    @Override
    public void writeChars(String value) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.writeChars(value);
    }

    private int writeClassDesc(ObjectStreamClass classDesc, boolean unshared) throws IOException {
        if (classDesc == null) {
            writeNull();
            return -1;
        }
        int handle = -1;
        if (!unshared) {
            handle = dumpCycle(classDesc);
        }
        if (handle == -1) {
            Class<?> classToWrite = classDesc.forClass();
            int previousHandle = -1;
            if (unshared) {
                previousHandle = this.objectsWritten.get(classDesc);
            }
            int handle2 = registerObjectWritten(classDesc);
            if (classDesc.isProxy()) {
                this.output.writeByte(Opcodes.OP_NEG_LONG);
                Class<?>[] interfaces = classToWrite.getInterfaces();
                this.output.writeInt(interfaces.length);
                for (Class<?> cls : interfaces) {
                    this.output.writeUTF(cls.getName());
                }
                annotateProxyClass(classToWrite);
                this.output.writeByte(Opcodes.OP_INVOKE_INTERFACE_RANGE);
                writeClassDesc(this.proxyClassDesc, false);
                if (unshared) {
                    removeUnsharedReference(classDesc, previousHandle);
                    return handle2;
                }
                return handle2;
            }
            this.output.writeByte(Opcodes.OP_INVOKE_INTERFACE);
            if (this.protocolVersion == 1) {
                writeNewClassDesc(classDesc);
            } else {
                this.primitiveTypes = this.output;
                writeClassDescriptor(classDesc);
                this.primitiveTypes = null;
            }
            annotateClass(classToWrite);
            drain();
            this.output.writeByte(Opcodes.OP_INVOKE_INTERFACE_RANGE);
            writeClassDesc(classDesc.getSuperclass(), unshared);
            if (unshared) {
                removeUnsharedReference(classDesc, previousHandle);
                return handle2;
            }
            return handle2;
        }
        return handle;
    }

    private void writeCyclicReference(int handle) throws IOException {
        this.output.writeByte(Opcodes.OP_INVOKE_STATIC);
        this.output.writeInt(handle);
    }

    @Override
    public void writeDouble(double value) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.writeDouble(value);
    }

    private void writeFieldDescriptors(ObjectStreamClass classDesc, boolean externalizable) throws IOException {
        Class<?> loadedClass = classDesc.forClass();
        ObjectStreamField[] fields = null;
        int fieldCount = 0;
        if (!externalizable && loadedClass != ObjectStreamClass.STRINGCLASS) {
            fields = classDesc.fields();
            fieldCount = fields.length;
        }
        this.output.writeShort(fieldCount);
        for (int i = 0; i < fieldCount; i++) {
            ObjectStreamField f = fields[i];
            boolean wasPrimitive = f.writeField(this.output);
            if (!wasPrimitive) {
                writeObject(f.getTypeString());
            }
        }
    }

    public void writeFields() throws IOException {
        if (this.currentPutField == null) {
            throw new NotActiveException();
        }
        writeFieldValues(this.currentPutField);
    }

    private void writeFieldValues(EmulatedFieldsForDumping emulatedFields) throws IOException {
        EmulatedFields accessibleSimulatedFields = emulatedFields.emulatedFields();
        EmulatedFields.ObjectSlot[] arr$ = accessibleSimulatedFields.slots();
        for (EmulatedFields.ObjectSlot slot : arr$) {
            Object fieldValue = slot.getFieldValue();
            Class<?> type = slot.getField().getType();
            if (type == Integer.TYPE) {
                this.output.writeInt(fieldValue != null ? ((Integer) fieldValue).intValue() : 0);
            } else if (type == Byte.TYPE) {
                this.output.writeByte(fieldValue != null ? ((Byte) fieldValue).byteValue() : (byte) 0);
            } else if (type == Character.TYPE) {
                this.output.writeChar(fieldValue != null ? ((Character) fieldValue).charValue() : (char) 0);
            } else if (type == Short.TYPE) {
                this.output.writeShort(fieldValue != null ? ((Short) fieldValue).shortValue() : (short) 0);
            } else if (type == Boolean.TYPE) {
                this.output.writeBoolean(fieldValue != null ? ((Boolean) fieldValue).booleanValue() : false);
            } else if (type == Long.TYPE) {
                this.output.writeLong(fieldValue != null ? ((Long) fieldValue).longValue() : 0L);
            } else if (type == Float.TYPE) {
                this.output.writeFloat(fieldValue != null ? ((Float) fieldValue).floatValue() : 0.0f);
            } else if (type == Double.TYPE) {
                this.output.writeDouble(fieldValue != null ? ((Double) fieldValue).doubleValue() : 0.0d);
            } else {
                writeObject(fieldValue);
            }
        }
    }

    private void writeFieldValues(Object obj, ObjectStreamClass classDesc) throws IOException {
        ObjectStreamField[] arr$ = classDesc.fields();
        for (ObjectStreamField fieldDesc : arr$) {
            try {
                Class<?> type = fieldDesc.getTypeInternal();
                Field field = classDesc.checkAndGetReflectionField(fieldDesc);
                if (field == null) {
                    throw new InvalidClassException(classDesc.getName() + " doesn't have a serializable field " + fieldDesc.getName() + " of type " + type);
                }
                if (type == Byte.TYPE) {
                    this.output.writeByte(field.getByte(obj));
                } else if (type == Character.TYPE) {
                    this.output.writeChar(field.getChar(obj));
                } else if (type == Double.TYPE) {
                    this.output.writeDouble(field.getDouble(obj));
                } else if (type == Float.TYPE) {
                    this.output.writeFloat(field.getFloat(obj));
                } else if (type == Integer.TYPE) {
                    this.output.writeInt(field.getInt(obj));
                } else if (type == Long.TYPE) {
                    this.output.writeLong(field.getLong(obj));
                } else if (type == Short.TYPE) {
                    this.output.writeShort(field.getShort(obj));
                } else if (type == Boolean.TYPE) {
                    this.output.writeBoolean(field.getBoolean(obj));
                } else {
                    Object objField = field.get(obj);
                    if (fieldDesc.isUnshared()) {
                        writeUnshared(objField);
                    } else {
                        writeObject(objField);
                    }
                }
            } catch (IllegalAccessException iae) {
                throw new AssertionError(iae);
            } catch (NoSuchFieldError e) {
                throw new InvalidClassException(classDesc.getName());
            }
        }
    }

    @Override
    public void writeFloat(float value) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.writeFloat(value);
    }

    private void writeHierarchy(Object object, ObjectStreamClass classDesc) throws IOException {
        if (object == null) {
            throw new NotActiveException();
        }
        List<ObjectStreamClass> hierarchy = classDesc.getHierarchy();
        int end = hierarchy.size();
        for (int i = 0; i < end; i++) {
            ObjectStreamClass osc = hierarchy.get(i);
            this.currentObject = object;
            this.currentClass = osc;
            boolean executed = false;
            try {
                if (osc.hasMethodWriteObject()) {
                    Method method = osc.getMethodWriteObject();
                    try {
                        method.invoke(object, this);
                        executed = true;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e.toString());
                    } catch (InvocationTargetException e2) {
                        Throwable ex = e2.getTargetException();
                        if (ex instanceof RuntimeException) {
                            throw ((RuntimeException) ex);
                        }
                        if (ex instanceof Error) {
                            throw ((Error) ex);
                        }
                        throw ((IOException) ex);
                    }
                }
                if (executed) {
                    drain();
                    this.output.writeByte(Opcodes.OP_INVOKE_INTERFACE_RANGE);
                } else {
                    defaultWriteObject();
                }
                this.currentObject = null;
                this.currentClass = null;
                this.currentPutField = null;
            } catch (Throwable th) {
                this.currentObject = null;
                this.currentClass = null;
                this.currentPutField = null;
                throw th;
            }
        }
    }

    @Override
    public void writeInt(int value) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.writeInt(value);
    }

    @Override
    public void writeLong(long value) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.writeLong(value);
    }

    private int writeNewArray(Object array, Class<?> arrayClass, ObjectStreamClass arrayClDesc, Class<?> componentType, boolean unshared) throws IOException {
        this.output.writeByte(Opcodes.OP_INVOKE_SUPER_RANGE);
        writeClassDesc(arrayClDesc, false);
        int handle = nextHandle();
        if (!unshared) {
            this.objectsWritten.put(array, handle);
        }
        if (componentType.isPrimitive()) {
            if (componentType == Integer.TYPE) {
                int[] intArray = (int[]) array;
                this.output.writeInt(intArray.length);
                for (int i : intArray) {
                    this.output.writeInt(i);
                }
            } else if (componentType == Byte.TYPE) {
                byte[] byteArray = (byte[]) array;
                this.output.writeInt(byteArray.length);
                this.output.write(byteArray, 0, byteArray.length);
            } else if (componentType == Character.TYPE) {
                char[] charArray = (char[]) array;
                this.output.writeInt(charArray.length);
                for (char c : charArray) {
                    this.output.writeChar(c);
                }
            } else if (componentType == Short.TYPE) {
                short[] shortArray = (short[]) array;
                this.output.writeInt(shortArray.length);
                for (short s : shortArray) {
                    this.output.writeShort(s);
                }
            } else if (componentType == Boolean.TYPE) {
                boolean[] booleanArray = (boolean[]) array;
                this.output.writeInt(booleanArray.length);
                for (boolean z : booleanArray) {
                    this.output.writeBoolean(z);
                }
            } else if (componentType == Long.TYPE) {
                long[] longArray = (long[]) array;
                this.output.writeInt(longArray.length);
                for (long j : longArray) {
                    this.output.writeLong(j);
                }
            } else if (componentType == Float.TYPE) {
                float[] floatArray = (float[]) array;
                this.output.writeInt(floatArray.length);
                for (float f : floatArray) {
                    this.output.writeFloat(f);
                }
            } else if (componentType == Double.TYPE) {
                double[] doubleArray = (double[]) array;
                this.output.writeInt(doubleArray.length);
                for (double d : doubleArray) {
                    this.output.writeDouble(d);
                }
            } else {
                throw new InvalidClassException("Wrong base type in " + arrayClass.getName());
            }
        } else {
            Object[] objectArray = (Object[]) array;
            this.output.writeInt(objectArray.length);
            for (Object obj : objectArray) {
                writeObject(obj);
            }
        }
        return handle;
    }

    private int writeNewClass(Class<?> object, boolean unshared) throws IOException {
        this.output.writeByte(Opcodes.OP_INVOKE_DIRECT_RANGE);
        ObjectStreamClass clDesc = ObjectStreamClass.lookupStreamClass(object);
        if (clDesc.isEnum()) {
            writeEnumDesc(clDesc, unshared);
        } else {
            writeClassDesc(clDesc, unshared);
        }
        int handle = nextHandle();
        if (!unshared) {
            this.objectsWritten.put(object, handle);
        }
        return handle;
    }

    private void writeNewClassDesc(ObjectStreamClass classDesc) throws IOException {
        this.output.writeUTF(classDesc.getName());
        this.output.writeLong(classDesc.getSerialVersionUID());
        byte flags = classDesc.getFlags();
        boolean externalizable = classDesc.isExternalizable();
        if (externalizable) {
            if (this.protocolVersion == 1) {
                flags = (byte) (flags & NOT_SC_BLOCK_DATA);
            } else {
                flags = (byte) (flags | 8);
            }
        }
        this.output.writeByte(flags);
        if (18 != classDesc.getFlags()) {
            writeFieldDescriptors(classDesc, externalizable);
        } else {
            this.output.writeShort(0);
        }
    }

    protected void writeClassDescriptor(ObjectStreamClass classDesc) throws IOException {
        writeNewClassDesc(classDesc);
    }

    private void writeNewException(Exception ex) throws IOException {
        this.output.writeByte(Opcodes.OP_NEG_INT);
        resetSeenObjects();
        writeObjectInternal(ex, false, false, false);
        resetSeenObjects();
    }

    private int writeNewObject(Object object, Class<?> theClass, ObjectStreamClass clDesc, boolean unshared) throws IOException {
        EmulatedFieldsForDumping originalCurrentPutField = this.currentPutField;
        this.currentPutField = null;
        boolean externalizable = clDesc.isExternalizable();
        boolean serializable = clDesc.isSerializable();
        if (!externalizable && !serializable) {
            throw new NotSerializableException(theClass.getName());
        }
        this.output.writeByte(115);
        writeClassDesc(clDesc, false);
        int previousHandle = -1;
        if (unshared) {
            previousHandle = this.objectsWritten.get(object);
        }
        int handle = registerObjectWritten(object);
        this.currentObject = object;
        this.currentClass = clDesc;
        try {
            if (externalizable) {
                boolean noBlockData = this.protocolVersion == 1;
                if (noBlockData) {
                    this.primitiveTypes = this.output;
                }
                ((Externalizable) object).writeExternal(this);
                if (noBlockData) {
                    this.primitiveTypes = null;
                } else {
                    drain();
                    this.output.writeByte(Opcodes.OP_INVOKE_INTERFACE_RANGE);
                }
            } else {
                writeHierarchy(object, this.currentClass);
            }
            return handle;
        } finally {
            if (unshared) {
                removeUnsharedReference(object, previousHandle);
            }
            this.currentObject = null;
            this.currentClass = null;
            this.currentPutField = originalCurrentPutField;
        }
    }

    private int writeNewString(String object, boolean unshared) throws IOException {
        byte[] buffer;
        int offset;
        long count = ModifiedUtf8.countBytes(object, false);
        if (count <= 65535) {
            buffer = new byte[((int) count) + 3];
            int offset2 = 0 + 1;
            buffer[0] = ObjectStreamConstants.TC_STRING;
            Memory.pokeShort(buffer, offset2, (short) count, ByteOrder.BIG_ENDIAN);
            offset = offset2 + 2;
        } else {
            buffer = new byte[((int) count) + 9];
            int offset3 = 0 + 1;
            buffer[0] = ObjectStreamConstants.TC_LONGSTRING;
            Memory.pokeLong(buffer, offset3, count, ByteOrder.BIG_ENDIAN);
            offset = offset3 + 8;
        }
        ModifiedUtf8.encode(buffer, offset, object);
        this.output.write(buffer, 0, buffer.length);
        int handle = nextHandle();
        if (!unshared) {
            this.objectsWritten.put(object, handle);
        }
        return handle;
    }

    private void writeNull() throws IOException {
        this.output.writeByte(112);
    }

    @Override
    public final void writeObject(Object object) throws IOException {
        writeObject(object, false);
    }

    public void writeUnshared(Object object) throws IOException {
        writeObject(object, true);
    }

    private void writeObject(Object object, boolean unshared) throws IOException {
        boolean setOutput = this.primitiveTypes == this.output;
        if (setOutput) {
            this.primitiveTypes = null;
        }
        if (this.subclassOverridingImplementation && !unshared) {
            writeObjectOverride(object);
            return;
        }
        try {
            drain();
            writeObjectInternal(object, unshared, true, true);
            if (setOutput) {
                this.primitiveTypes = this.output;
            }
        } catch (IOException ioEx1) {
            if (this.nestedLevels == 0) {
                try {
                    writeNewException(ioEx1);
                } catch (IOException e) {
                }
            }
            throw ioEx1;
        }
    }

    private int writeObjectInternal(Object object, boolean unshared, boolean computeClassBasedReplacement, boolean computeStreamReplacement) throws IOException {
        Object streamReplacement;
        Object replObj;
        int handle;
        if (object == null) {
            writeNull();
            return -1;
        }
        if (!unshared && (handle = dumpCycle(object)) != -1) {
            return handle;
        }
        Class<?> objClass = object.getClass();
        ObjectStreamClass clDesc = ObjectStreamClass.lookupStreamClass(objClass);
        this.nestedLevels++;
        try {
            if (!this.enableReplace || !computeStreamReplacement) {
                if (objClass == ObjectStreamClass.CLASSCLASS) {
                    return writeNewClass((Class) object, unshared);
                }
                if (objClass == ObjectStreamClass.OBJECTSTREAMCLASSCLASS) {
                    return writeClassDesc((ObjectStreamClass) object, unshared);
                }
            }
            if (clDesc.isSerializable() && computeClassBasedReplacement && clDesc.hasMethodWriteReplace()) {
                Method methodWriteReplace = clDesc.getMethodWriteReplace();
                try {
                    replObj = methodWriteReplace.invoke(object, (Object[]) null);
                } catch (IllegalAccessException e) {
                    replObj = object;
                } catch (InvocationTargetException ite) {
                    Throwable target = ite.getTargetException();
                    if (target instanceof ObjectStreamException) {
                        throw ((ObjectStreamException) target);
                    }
                    if (target instanceof Error) {
                        throw ((Error) target);
                    }
                    throw ((RuntimeException) target);
                }
                if (replObj != object) {
                    int replacementHandle = writeObjectInternal(replObj, false, false, computeStreamReplacement);
                    if (replacementHandle != -1) {
                        this.objectsWritten.put(object, replacementHandle);
                    }
                    return replacementHandle;
                }
            }
            if (!this.enableReplace || !computeStreamReplacement || (streamReplacement = replaceObject(object)) == object) {
                return objClass == ObjectStreamClass.CLASSCLASS ? writeNewClass((Class) object, unshared) : objClass == ObjectStreamClass.OBJECTSTREAMCLASSCLASS ? writeClassDesc((ObjectStreamClass) object, unshared) : objClass == ObjectStreamClass.STRINGCLASS ? writeNewString((String) object, unshared) : objClass.isArray() ? writeNewArray(object, objClass, clDesc, objClass.getComponentType(), unshared) : object instanceof Enum ? writeNewEnum(object, objClass, unshared) : writeNewObject(object, objClass, clDesc, unshared);
            }
            int replacementHandle2 = writeObjectInternal(streamReplacement, false, computeClassBasedReplacement, false);
            if (replacementHandle2 != -1) {
                this.objectsWritten.put(object, replacementHandle2);
            }
            return replacementHandle2;
        } finally {
            this.nestedLevels--;
        }
    }

    private ObjectStreamClass writeEnumDesc(ObjectStreamClass classDesc, boolean unshared) throws IOException {
        classDesc.setFlags((byte) 18);
        int previousHandle = -1;
        if (unshared) {
            previousHandle = this.objectsWritten.get(classDesc);
        }
        int handle = -1;
        if (!unshared) {
            handle = dumpCycle(classDesc);
        }
        if (handle == -1) {
            Class<?> classToWrite = classDesc.forClass();
            registerObjectWritten(classDesc);
            this.output.writeByte(Opcodes.OP_INVOKE_INTERFACE);
            if (this.protocolVersion == 1) {
                writeNewClassDesc(classDesc);
            } else {
                this.primitiveTypes = this.output;
                writeClassDescriptor(classDesc);
                this.primitiveTypes = null;
            }
            annotateClass(classToWrite);
            drain();
            this.output.writeByte(Opcodes.OP_INVOKE_INTERFACE_RANGE);
            ObjectStreamClass superClassDesc = classDesc.getSuperclass();
            if (superClassDesc != null) {
                superClassDesc.setFlags((byte) 18);
                writeEnumDesc(superClassDesc, unshared);
            } else {
                this.output.writeByte(112);
            }
            if (unshared) {
                removeUnsharedReference(classDesc, previousHandle);
            }
        }
        return classDesc;
    }

    private int writeNewEnum(Object object, Class<?> theClass, boolean unshared) throws IOException {
        EmulatedFieldsForDumping originalCurrentPutField = this.currentPutField;
        this.currentPutField = null;
        this.output.writeByte(Opcodes.OP_NOT_LONG);
        while (theClass != null && !theClass.isEnum()) {
            theClass = theClass.getSuperclass();
        }
        ObjectStreamClass classDesc = ObjectStreamClass.lookup(theClass);
        writeEnumDesc(classDesc, unshared);
        int previousHandle = -1;
        if (unshared) {
            previousHandle = this.objectsWritten.get(object);
        }
        int handle = registerObjectWritten(object);
        ObjectStreamField[] fields = classDesc.getSuperclass().fields();
        if (fields != null && fields.length > 1) {
            Field field = classDesc.getSuperclass().checkAndGetReflectionField(fields[1]);
            if (field == null) {
                throw new NoSuchFieldError();
            }
            try {
                String str = (String) field.get(object);
                int strHandle = -1;
                if (!unshared) {
                    strHandle = dumpCycle(str);
                }
                if (strHandle == -1) {
                    writeNewString(str, unshared);
                }
            } catch (IllegalAccessException iae) {
                throw new AssertionError(iae);
            }
        }
        if (unshared) {
            removeUnsharedReference(object, previousHandle);
        }
        this.currentPutField = originalCurrentPutField;
        return handle;
    }

    protected void writeObjectOverride(Object object) throws IOException {
        if (!this.subclassOverridingImplementation) {
            throw new IOException();
        }
    }

    @Override
    public void writeShort(int value) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.writeShort(value);
    }

    protected void writeStreamHeader() throws IOException {
        this.output.writeShort(-21267);
        this.output.writeShort(5);
    }

    @Override
    public void writeUTF(String value) throws IOException {
        checkWritePrimitiveTypes();
        this.primitiveTypes.writeUTF(value);
    }
}
