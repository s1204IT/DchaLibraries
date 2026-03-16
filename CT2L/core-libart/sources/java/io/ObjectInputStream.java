package java.io;

import dalvik.bytecode.Opcodes;
import dalvik.system.VMStack;
import java.io.EmulatedFields;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import libcore.util.EmptyArray;

public class ObjectInputStream extends InputStream implements ObjectInput, ObjectStreamConstants {
    private static final ClassLoader bootstrapLoader;
    private static final ClassLoader systemLoader;
    private HashMap<Class<?>, List<Class<?>>> cachedSuperclasses;
    private ClassLoader callerClassLoader;
    private ObjectStreamClass currentClass;
    private Object currentObject;
    private int descriptorHandle;
    private InputStream emptyStream;
    private boolean enableResolve;
    private boolean hasPushbackTC;
    private DataInputStream input;
    private boolean mustResolve;
    private int nestedLevels;
    private int nextHandle;
    private ArrayList<Object> objectsRead;
    private InputStream primitiveData;
    private DataInputStream primitiveTypes;
    private byte pushbackTC;
    private boolean subclassOverridingImplementation;
    private InputValidationDesc[] validations;
    private static final Object UNSHARED_OBJ = new Object();
    private static final HashMap<String, Class<?>> PRIMITIVE_CLASSES = new HashMap<>();

    public static abstract class GetField {
        public abstract boolean defaulted(String str) throws IOException, IllegalArgumentException;

        public abstract byte get(String str, byte b) throws IOException, IllegalArgumentException;

        public abstract char get(String str, char c) throws IOException, IllegalArgumentException;

        public abstract double get(String str, double d) throws IOException, IllegalArgumentException;

        public abstract float get(String str, float f) throws IOException, IllegalArgumentException;

        public abstract int get(String str, int i) throws IOException, IllegalArgumentException;

        public abstract long get(String str, long j) throws IOException, IllegalArgumentException;

        public abstract Object get(String str, Object obj) throws IOException, IllegalArgumentException;

        public abstract short get(String str, short s) throws IOException, IllegalArgumentException;

        public abstract boolean get(String str, boolean z) throws IOException, IllegalArgumentException;

        public abstract ObjectStreamClass getObjectStreamClass();
    }

    static {
        PRIMITIVE_CLASSES.put("boolean", Boolean.TYPE);
        PRIMITIVE_CLASSES.put("byte", Byte.TYPE);
        PRIMITIVE_CLASSES.put("char", Character.TYPE);
        PRIMITIVE_CLASSES.put("double", Double.TYPE);
        PRIMITIVE_CLASSES.put("float", Float.TYPE);
        PRIMITIVE_CLASSES.put("int", Integer.TYPE);
        PRIMITIVE_CLASSES.put("long", Long.TYPE);
        PRIMITIVE_CLASSES.put("short", Short.TYPE);
        PRIMITIVE_CLASSES.put("void", Void.TYPE);
        bootstrapLoader = Object.class.getClassLoader();
        systemLoader = ClassLoader.getSystemClassLoader();
    }

    static class InputValidationDesc {
        int priority;
        ObjectInputValidation validator;

        InputValidationDesc() {
        }
    }

    protected ObjectInputStream() throws IOException {
        this.emptyStream = new ByteArrayInputStream(EmptyArray.BYTE);
        this.primitiveData = this.emptyStream;
        this.mustResolve = true;
        this.descriptorHandle = -1;
        this.cachedSuperclasses = new HashMap<>();
        this.subclassOverridingImplementation = true;
    }

    public ObjectInputStream(InputStream input) throws IOException {
        this.emptyStream = new ByteArrayInputStream(EmptyArray.BYTE);
        this.primitiveData = this.emptyStream;
        this.mustResolve = true;
        this.descriptorHandle = -1;
        this.cachedSuperclasses = new HashMap<>();
        this.input = input instanceof DataInputStream ? (DataInputStream) input : new DataInputStream(input);
        this.primitiveTypes = new DataInputStream(this);
        this.enableResolve = false;
        this.subclassOverridingImplementation = false;
        resetState();
        this.nestedLevels = 0;
        this.primitiveData = this.input;
        readStreamHeader();
        this.primitiveData = this.emptyStream;
    }

    @Override
    public int available() throws IOException {
        checkReadPrimitiveTypes();
        return this.primitiveData.available();
    }

    private void checkReadPrimitiveTypes() throws IOException {
        if (this.primitiveData == this.input || this.primitiveData.available() > 0) {
            return;
        }
        while (true) {
            int next = 0;
            if (this.hasPushbackTC) {
                this.hasPushbackTC = false;
            } else {
                next = this.input.read();
                this.pushbackTC = (byte) next;
            }
            switch (this.pushbackTC) {
                case Opcodes.OP_INVOKE_STATIC_RANGE:
                    this.primitiveData = new ByteArrayInputStream(readBlockData());
                    return;
                case Opcodes.OP_INVOKE_INTERFACE_RANGE:
                default:
                    if (next != -1) {
                        pushbackTC();
                        return;
                    }
                    return;
                case 121:
                    resetState();
                    break;
                case 122:
                    this.primitiveData = new ByteArrayInputStream(readBlockDataLong());
                    return;
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.input.close();
    }

    public void defaultReadObject() throws IOException, ClassNotFoundException {
        if (this.currentObject != null || !this.mustResolve) {
            readFieldValues(this.currentObject, this.currentClass);
            return;
        }
        throw new NotActiveException();
    }

    protected boolean enableResolveObject(boolean enable) {
        boolean originalValue = this.enableResolve;
        this.enableResolve = enable;
        return originalValue;
    }

    private int nextHandle() {
        int i = this.nextHandle;
        this.nextHandle = i + 1;
        return i;
    }

    private byte nextTC() throws IOException {
        if (this.hasPushbackTC) {
            this.hasPushbackTC = false;
        } else {
            this.pushbackTC = this.input.readByte();
        }
        return this.pushbackTC;
    }

    private void pushbackTC() {
        this.hasPushbackTC = true;
    }

    @Override
    public int read() throws IOException {
        checkReadPrimitiveTypes();
        return this.primitiveData.read();
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        Arrays.checkOffsetAndCount(buffer.length, byteOffset, byteCount);
        if (byteCount == 0) {
            return 0;
        }
        checkReadPrimitiveTypes();
        return this.primitiveData.read(buffer, byteOffset, byteCount);
    }

    private byte[] readBlockData() throws IOException {
        byte[] result = new byte[this.input.readByte() & Character.DIRECTIONALITY_UNDEFINED];
        this.input.readFully(result);
        return result;
    }

    private byte[] readBlockDataLong() throws IOException {
        byte[] result = new byte[this.input.readInt()];
        this.input.readFully(result);
        return result;
    }

    @Override
    public boolean readBoolean() throws IOException {
        return this.primitiveTypes.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return this.primitiveTypes.readByte();
    }

    @Override
    public char readChar() throws IOException {
        return this.primitiveTypes.readChar();
    }

    private void discardData() throws IOException, ClassNotFoundException {
        this.primitiveData = this.emptyStream;
        boolean resolve = this.mustResolve;
        this.mustResolve = false;
        while (true) {
            byte tc = nextTC();
            if (tc == 120) {
                this.mustResolve = resolve;
                return;
            }
            readContent(tc);
        }
    }

    private ObjectStreamClass readClassDesc() throws IOException, ClassNotFoundException {
        byte tc = nextTC();
        switch (tc) {
            case 112:
                return null;
            case Opcodes.OP_INVOKE_STATIC:
                return (ObjectStreamClass) readCyclicReference();
            case Opcodes.OP_INVOKE_INTERFACE:
                return readNewClassDesc(false);
            case Opcodes.OP_NEG_LONG:
                Class<?> proxyClass = readNewProxyClassDesc();
                ObjectStreamClass streamClass = ObjectStreamClass.lookup(proxyClass);
                streamClass.setLoadFields(ObjectStreamClass.NO_FIELDS);
                registerObjectRead(streamClass, nextHandle(), false);
                checkedSetSuperClassDesc(streamClass, readClassDesc());
                return streamClass;
            default:
                throw corruptStream(tc);
        }
    }

    private StreamCorruptedException corruptStream(byte tc) throws StreamCorruptedException {
        throw new StreamCorruptedException("Wrong format: " + Integer.toHexString(tc & Character.DIRECTIONALITY_UNDEFINED));
    }

    private Object readContent(byte tc) throws ClassNotFoundException, IOException {
        switch (tc) {
            case 112:
                return null;
            case Opcodes.OP_INVOKE_STATIC:
                return readCyclicReference();
            case Opcodes.OP_INVOKE_INTERFACE:
                return readNewClassDesc(false);
            case 115:
                return readNewObject(false);
            case Opcodes.OP_INVOKE_VIRTUAL_RANGE:
                return readNewString(false);
            case Opcodes.OP_INVOKE_SUPER_RANGE:
                return readNewArray(false);
            case Opcodes.OP_INVOKE_DIRECT_RANGE:
                return readNewClass(false);
            case Opcodes.OP_INVOKE_STATIC_RANGE:
                return readBlockData();
            case Opcodes.OP_INVOKE_INTERFACE_RANGE:
            default:
                throw corruptStream(tc);
            case 121:
                resetState();
                return null;
            case 122:
                return readBlockDataLong();
            case Opcodes.OP_NEG_INT:
                Exception exc = readException();
                throw new WriteAbortedException("Read an exception", exc);
            case Opcodes.OP_NOT_INT:
                return readNewLongString(false);
        }
    }

    private Object readNonPrimitiveContent(boolean unshared) throws IOException, ClassNotFoundException {
        checkReadPrimitiveTypes();
        if (this.primitiveData.available() > 0) {
            OptionalDataException e = new OptionalDataException();
            e.length = this.primitiveData.available();
            throw e;
        }
        while (true) {
            byte tc = nextTC();
            switch (tc) {
                case 112:
                    return null;
                case Opcodes.OP_INVOKE_STATIC:
                    if (unshared) {
                        readNewHandle();
                        throw new InvalidObjectException("Unshared read of back reference");
                    }
                    return readCyclicReference();
                case Opcodes.OP_INVOKE_INTERFACE:
                    return readNewClassDesc(unshared);
                case 115:
                    return readNewObject(unshared);
                case Opcodes.OP_INVOKE_VIRTUAL_RANGE:
                    return readNewString(unshared);
                case Opcodes.OP_INVOKE_SUPER_RANGE:
                    return readNewArray(unshared);
                case Opcodes.OP_INVOKE_DIRECT_RANGE:
                    return readNewClass(unshared);
                case Opcodes.OP_INVOKE_STATIC_RANGE:
                case 122:
                case Opcodes.OP_NEG_LONG:
                default:
                    throw corruptStream(tc);
                case Opcodes.OP_INVOKE_INTERFACE_RANGE:
                    pushbackTC();
                    OptionalDataException e2 = new OptionalDataException();
                    e2.eof = true;
                    throw e2;
                case 121:
                    resetState();
                    break;
                case Opcodes.OP_NEG_INT:
                    Exception exc = readException();
                    throw new WriteAbortedException("Read an exception", exc);
                case Opcodes.OP_NOT_INT:
                    return readNewLongString(unshared);
                case Opcodes.OP_NOT_LONG:
                    return readEnum(unshared);
            }
        }
    }

    private Object readCyclicReference() throws IOException {
        return registeredObjectRead(readNewHandle());
    }

    @Override
    public double readDouble() throws IOException {
        return this.primitiveTypes.readDouble();
    }

    private Exception readException() throws ClassNotFoundException, IOException {
        resetSeenObjects();
        Exception exc = (Exception) readObject();
        resetSeenObjects();
        return exc;
    }

    private void readFieldDescriptors(ObjectStreamClass cDesc) throws IOException, ClassNotFoundException {
        String classSig;
        int i = this.input.readShort();
        ObjectStreamField[] fields = new ObjectStreamField[i];
        cDesc.setLoadFields(fields);
        for (short i2 = 0; i2 < i; i2 = (short) (i2 + 1)) {
            char typecode = (char) this.input.readByte();
            String fieldName = this.input.readUTF();
            boolean isPrimType = ObjectStreamClass.isPrimitiveType(typecode);
            if (isPrimType) {
                classSig = String.valueOf(typecode);
            } else {
                boolean old = this.enableResolve;
                try {
                    this.enableResolve = false;
                    classSig = (String) readObject();
                } finally {
                    this.enableResolve = old;
                }
            }
            ObjectStreamField f = new ObjectStreamField(formatClassSig(classSig), fieldName);
            fields[i2] = f;
        }
    }

    private static String formatClassSig(String classSig) {
        int start = 0;
        int end = classSig.length();
        if (end > 0) {
            while (classSig.startsWith("[L", start) && classSig.charAt(end - 1) == ';') {
                start += 2;
                end--;
            }
            if (start > 0) {
                return classSig.substring(start - 2, end + 1);
            }
            return classSig;
        }
        return classSig;
    }

    public GetField readFields() throws IOException, ClassNotFoundException {
        if (this.currentObject == null) {
            throw new NotActiveException();
        }
        EmulatedFieldsForLoading result = new EmulatedFieldsForLoading(this.currentClass);
        readFieldValues(result);
        return result;
    }

    private void readFieldValues(EmulatedFieldsForLoading emulatedFields) throws IOException {
        EmulatedFields.ObjectSlot[] slots = emulatedFields.emulatedFields().slots();
        for (EmulatedFields.ObjectSlot element : slots) {
            element.defaulted = false;
            Class<?> type = element.field.getType();
            if (type == Integer.TYPE) {
                element.fieldValue = Integer.valueOf(this.input.readInt());
            } else if (type == Byte.TYPE) {
                element.fieldValue = Byte.valueOf(this.input.readByte());
            } else if (type == Character.TYPE) {
                element.fieldValue = Character.valueOf(this.input.readChar());
            } else if (type == Short.TYPE) {
                element.fieldValue = Short.valueOf(this.input.readShort());
            } else if (type == Boolean.TYPE) {
                element.fieldValue = Boolean.valueOf(this.input.readBoolean());
            } else if (type == Long.TYPE) {
                element.fieldValue = Long.valueOf(this.input.readLong());
            } else if (type == Float.TYPE) {
                element.fieldValue = Float.valueOf(this.input.readFloat());
            } else if (type == Double.TYPE) {
                element.fieldValue = Double.valueOf(this.input.readDouble());
            } else {
                try {
                    element.fieldValue = readObject();
                } catch (ClassNotFoundException cnf) {
                    throw new InvalidClassException(cnf.toString());
                }
            }
        }
    }

    private void readFieldValues(Object obj, ObjectStreamClass classDesc) throws ClassNotFoundException, IOException {
        ObjectStreamField[] fields = classDesc.getLoadFields();
        if (fields == null) {
            fields = ObjectStreamClass.NO_FIELDS;
        }
        Class<?> declaringClass = classDesc.forClass();
        if (declaringClass == null && this.mustResolve) {
            throw new ClassNotFoundException(classDesc.getName());
        }
        ObjectStreamField[] arr$ = fields;
        for (ObjectStreamField fieldDesc : arr$) {
            Field field = classDesc.checkAndGetReflectionField(fieldDesc);
            try {
                Class<?> type = fieldDesc.getTypeInternal();
                if (type == Byte.TYPE) {
                    byte b = this.input.readByte();
                    if (field != null) {
                        field.setByte(obj, b);
                    }
                } else if (type == Character.TYPE) {
                    char c = this.input.readChar();
                    if (field != null) {
                        field.setChar(obj, c);
                    }
                } else if (type == Double.TYPE) {
                    double d = this.input.readDouble();
                    if (field != null) {
                        field.setDouble(obj, d);
                    }
                } else if (type == Float.TYPE) {
                    float f = this.input.readFloat();
                    if (field != null) {
                        field.setFloat(obj, f);
                    }
                } else if (type == Integer.TYPE) {
                    int i = this.input.readInt();
                    if (field != null) {
                        field.setInt(obj, i);
                    }
                } else if (type == Long.TYPE) {
                    long j = this.input.readLong();
                    if (field != null) {
                        field.setLong(obj, j);
                    }
                } else if (type == Short.TYPE) {
                    short s = this.input.readShort();
                    if (field != null) {
                        field.setShort(obj, s);
                    }
                } else if (type == Boolean.TYPE) {
                    boolean z = this.input.readBoolean();
                    if (field != null) {
                        field.setBoolean(obj, z);
                    }
                } else {
                    Object toSet = fieldDesc.isUnshared() ? readUnshared() : readObject();
                    if (toSet != null) {
                        String fieldName = fieldDesc.getName();
                        ObjectStreamField localFieldDesc = classDesc.getField(fieldName);
                        Class<?> fieldType = localFieldDesc.getTypeInternal();
                        Class<?> valueType = toSet.getClass();
                        if (!fieldType.isAssignableFrom(valueType)) {
                            throw new ClassCastException(classDesc.getName() + "." + fieldName + " - " + fieldType + " not compatible with " + valueType);
                        }
                        if (field != null) {
                            field.set(obj, toSet);
                        }
                    } else {
                        continue;
                    }
                }
            } catch (IllegalAccessException iae) {
                throw new AssertionError(iae);
            } catch (NoSuchFieldError e) {
            }
        }
    }

    @Override
    public float readFloat() throws IOException {
        return this.primitiveTypes.readFloat();
    }

    @Override
    public void readFully(byte[] dst) throws IOException {
        this.primitiveTypes.readFully(dst);
    }

    @Override
    public void readFully(byte[] dst, int offset, int byteCount) throws IOException {
        this.primitiveTypes.readFully(dst, offset, byteCount);
    }

    private void readHierarchy(Object object, ObjectStreamClass classDesc) throws Throwable {
        if (object == null && this.mustResolve) {
            throw new NotActiveException();
        }
        List<ObjectStreamClass> streamClassList = classDesc.getHierarchy();
        if (object == null) {
            for (ObjectStreamClass objectStreamClass : streamClassList) {
                readObjectForClass(null, objectStreamClass);
            }
            return;
        }
        List<Class<?>> superclasses = this.cachedSuperclasses.get(object.getClass());
        if (superclasses == null) {
            superclasses = cacheSuperclassesFor(object.getClass());
        }
        int lastIndex = 0;
        int end = superclasses.size();
        for (int i = 0; i < end; i++) {
            Class<?> superclass = superclasses.get(i);
            int index = findStreamSuperclass(superclass, streamClassList, lastIndex);
            if (index == -1) {
                readObjectNoData(object, superclass, ObjectStreamClass.lookupStreamClass(superclass));
            } else {
                for (int j = lastIndex; j <= index; j++) {
                    readObjectForClass(object, streamClassList.get(j));
                }
                lastIndex = index + 1;
            }
        }
    }

    private List<Class<?>> cacheSuperclassesFor(Class<?> c) {
        ArrayList<Class<?>> result = new ArrayList<>();
        Class<?> nextClass = c;
        while (nextClass != null) {
            Class<?> testClass = nextClass.getSuperclass();
            if (testClass != null) {
                result.add(0, nextClass);
            }
            nextClass = testClass;
        }
        this.cachedSuperclasses.put(c, result);
        return result;
    }

    private int findStreamSuperclass(Class<?> cl, List<ObjectStreamClass> classList, int lastIndex) {
        int end = classList.size();
        for (int i = lastIndex; i < end; i++) {
            ObjectStreamClass objCl = classList.get(i);
            String forName = objCl.forClass().getName();
            if (objCl.getName().equals(forName)) {
                if (cl.getName().equals(objCl.getName())) {
                    return i;
                }
            } else if (cl.getName().equals(forName)) {
                return i;
            }
        }
        return -1;
    }

    private void readObjectNoData(Object object, Class<?> cl, ObjectStreamClass classDesc) throws Throwable {
        if (classDesc.isSerializable() && classDesc.hasMethodReadObjectNoData()) {
            Method readMethod = classDesc.getMethodReadObjectNoData();
            try {
                readMethod.invoke(object, new Object[0]);
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
                throw ((ObjectStreamException) ex);
            }
        }
    }

    private void readObjectForClass(Object object, ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
        Method readMethod;
        this.currentObject = object;
        this.currentClass = classDesc;
        boolean hadWriteMethod = (classDesc.getFlags() & 1) != 0;
        Class<?> targetClass = classDesc.forClass();
        if (targetClass == null || !this.mustResolve) {
            readMethod = null;
        } else {
            readMethod = classDesc.getMethodReadObject();
        }
        try {
            if (readMethod != null) {
                readMethod.setAccessible(true);
                try {
                    try {
                        readMethod.invoke(object, this);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e.toString());
                    }
                } catch (InvocationTargetException e2) {
                    Throwable ex = e2.getTargetException();
                    if (ex instanceof ClassNotFoundException) {
                        throw ((ClassNotFoundException) ex);
                    }
                    if (ex instanceof RuntimeException) {
                        throw ((RuntimeException) ex);
                    }
                    if (ex instanceof Error) {
                        throw ((Error) ex);
                    }
                    throw ((IOException) ex);
                }
            } else {
                defaultReadObject();
            }
            if (hadWriteMethod) {
                discardData();
            }
        } finally {
            this.currentObject = null;
            this.currentClass = null;
        }
    }

    @Override
    public int readInt() throws IOException {
        return this.primitiveTypes.readInt();
    }

    @Override
    @Deprecated
    public String readLine() throws IOException {
        return this.primitiveTypes.readLine();
    }

    @Override
    public long readLong() throws IOException {
        return this.primitiveTypes.readLong();
    }

    private Object readNewArray(boolean unshared) throws IOException, ClassNotFoundException {
        ObjectStreamClass classDesc = readClassDesc();
        if (classDesc == null) {
            throw missingClassDescriptor();
        }
        int newHandle = nextHandle();
        int size = this.input.readInt();
        Class<?> arrayClass = classDesc.forClass();
        Class<?> componentType = arrayClass.getComponentType();
        Object result = Array.newInstance(componentType, size);
        registerObjectRead(result, newHandle, unshared);
        if (componentType.isPrimitive()) {
            if (componentType == Integer.TYPE) {
                int[] intArray = (int[]) result;
                for (int i = 0; i < size; i++) {
                    intArray[i] = this.input.readInt();
                }
            } else if (componentType == Byte.TYPE) {
                byte[] byteArray = (byte[]) result;
                this.input.readFully(byteArray, 0, size);
            } else if (componentType == Character.TYPE) {
                char[] charArray = (char[]) result;
                for (int i2 = 0; i2 < size; i2++) {
                    charArray[i2] = this.input.readChar();
                }
            } else if (componentType == Short.TYPE) {
                short[] shortArray = (short[]) result;
                for (int i3 = 0; i3 < size; i3++) {
                    shortArray[i3] = this.input.readShort();
                }
            } else if (componentType == Boolean.TYPE) {
                boolean[] booleanArray = (boolean[]) result;
                for (int i4 = 0; i4 < size; i4++) {
                    booleanArray[i4] = this.input.readBoolean();
                }
            } else if (componentType == Long.TYPE) {
                long[] longArray = (long[]) result;
                for (int i5 = 0; i5 < size; i5++) {
                    longArray[i5] = this.input.readLong();
                }
            } else if (componentType == Float.TYPE) {
                float[] floatArray = (float[]) result;
                for (int i6 = 0; i6 < size; i6++) {
                    floatArray[i6] = this.input.readFloat();
                }
            } else if (componentType == Double.TYPE) {
                double[] doubleArray = (double[]) result;
                for (int i7 = 0; i7 < size; i7++) {
                    doubleArray[i7] = this.input.readDouble();
                }
            } else {
                throw new ClassNotFoundException("Wrong base type in " + classDesc.getName());
            }
        } else {
            Object[] objectArray = (Object[]) result;
            for (int i8 = 0; i8 < size; i8++) {
                objectArray[i8] = readObject();
            }
        }
        if (this.enableResolve) {
            Object result2 = resolveObject(result);
            registerObjectRead(result2, newHandle, false);
            return result2;
        }
        return result;
    }

    private Class<?> readNewClass(boolean unshared) throws IOException, ClassNotFoundException {
        ObjectStreamClass classDesc = readClassDesc();
        if (classDesc == null) {
            throw missingClassDescriptor();
        }
        Class<?> localClass = classDesc.forClass();
        if (localClass != null) {
            registerObjectRead(localClass, nextHandle(), unshared);
        }
        return localClass;
    }

    private ObjectStreamClass readEnumDesc() throws IOException, ClassNotFoundException {
        byte tc = nextTC();
        switch (tc) {
            case 112:
                return null;
            case Opcodes.OP_INVOKE_STATIC:
                return (ObjectStreamClass) readCyclicReference();
            case Opcodes.OP_INVOKE_INTERFACE:
                return readEnumDescInternal();
            default:
                throw corruptStream(tc);
        }
    }

    private ObjectStreamClass readEnumDescInternal() throws IOException, ClassNotFoundException {
        this.primitiveData = this.input;
        int oldHandle = this.descriptorHandle;
        this.descriptorHandle = nextHandle();
        ObjectStreamClass classDesc = readClassDescriptor();
        registerObjectRead(classDesc, this.descriptorHandle, false);
        this.descriptorHandle = oldHandle;
        this.primitiveData = this.emptyStream;
        classDesc.setClass(resolveClass(classDesc));
        discardData();
        ObjectStreamClass superClass = readClassDesc();
        checkedSetSuperClassDesc(classDesc, superClass);
        if (0 != classDesc.getSerialVersionUID() || 0 != superClass.getSerialVersionUID()) {
            throw new InvalidClassException(superClass.getName(), "Incompatible class (SUID): " + superClass + " but expected " + superClass);
        }
        byte tc = nextTC();
        if (tc == 120) {
            superClass.setSuperclass(readClassDesc());
        } else {
            pushbackTC();
        }
        return classDesc;
    }

    private Object readEnum(boolean unshared) throws IOException, ClassNotFoundException {
        String name;
        ObjectStreamClass classDesc = readEnumDesc();
        Class<?> clsCheckAndGetTcEnumClass = classDesc.checkAndGetTcEnumClass();
        int newHandle = nextHandle();
        byte tc = nextTC();
        switch (tc) {
            case Opcodes.OP_INVOKE_STATIC:
                if (unshared) {
                    readNewHandle();
                    throw new InvalidObjectException("Unshared read of back reference");
                }
                name = (String) readCyclicReference();
                break;
                break;
            case Opcodes.OP_INVOKE_INTERFACE:
            case 115:
            default:
                throw corruptStream(tc);
            case Opcodes.OP_INVOKE_VIRTUAL_RANGE:
                name = (String) readNewString(unshared);
                break;
        }
        try {
            Enum<?> result = Enum.valueOf(clsCheckAndGetTcEnumClass, name);
            registerObjectRead(result, newHandle, unshared);
            return result;
        } catch (IllegalArgumentException e) {
            InvalidObjectException ioe = new InvalidObjectException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }

    private ObjectStreamClass readNewClassDesc(boolean unshared) throws IOException, ClassNotFoundException {
        this.primitiveData = this.input;
        int oldHandle = this.descriptorHandle;
        this.descriptorHandle = nextHandle();
        ObjectStreamClass newClassDesc = readClassDescriptor();
        registerObjectRead(newClassDesc, this.descriptorHandle, unshared);
        this.descriptorHandle = oldHandle;
        this.primitiveData = this.emptyStream;
        try {
            newClassDesc.setClass(resolveClass(newClassDesc));
            verifyAndInit(newClassDesc);
        } catch (ClassNotFoundException e) {
            if (this.mustResolve) {
                throw e;
            }
        }
        ObjectStreamField[] fields = newClassDesc.getLoadFields();
        if (fields == null) {
            fields = ObjectStreamClass.NO_FIELDS;
        }
        ClassLoader loader = newClassDesc.forClass() == null ? this.callerClassLoader : newClassDesc.forClass().getClassLoader();
        ObjectStreamField[] arr$ = fields;
        for (ObjectStreamField element : arr$) {
            element.resolve(loader);
        }
        discardData();
        checkedSetSuperClassDesc(newClassDesc, readClassDesc());
        return newClassDesc;
    }

    private Class<?> readNewProxyClassDesc() throws IOException, ClassNotFoundException {
        int count = this.input.readInt();
        String[] interfaceNames = new String[count];
        for (int i = 0; i < count; i++) {
            interfaceNames[i] = this.input.readUTF();
        }
        Class<?> proxy = resolveProxyClass(interfaceNames);
        discardData();
        return proxy;
    }

    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass newClassDesc = new ObjectStreamClass();
        String name = this.input.readUTF();
        if (name.length() == 0) {
            throw new IOException("The stream is corrupted");
        }
        newClassDesc.setName(name);
        newClassDesc.setSerialVersionUID(this.input.readLong());
        newClassDesc.setFlags(this.input.readByte());
        if (this.descriptorHandle == -1) {
            this.descriptorHandle = nextHandle();
        }
        registerObjectRead(newClassDesc, this.descriptorHandle, false);
        readFieldDescriptors(newClassDesc);
        return newClassDesc;
    }

    protected Class<?> resolveProxyClass(String[] interfaceNames) throws ClassNotFoundException, IOException {
        ClassLoader loader = this.callerClassLoader;
        Class<?>[] interfaces = new Class[interfaceNames.length];
        for (int i = 0; i < interfaceNames.length; i++) {
            interfaces[i] = Class.forName(interfaceNames[i], false, loader);
        }
        try {
            return Proxy.getProxyClass(loader, interfaces);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(e.toString(), e);
        }
    }

    private int readNewHandle() throws IOException {
        return this.input.readInt();
    }

    private Object readNewObject(boolean unshared) throws Throwable {
        Object result;
        ObjectStreamClass classDesc = readClassDesc();
        if (classDesc == null) {
            throw missingClassDescriptor();
        }
        Class<?> objectClass = classDesc.checkAndGetTcObjectClass();
        int newHandle = nextHandle();
        Object registeredResult = null;
        if (objectClass != null) {
            result = classDesc.newInstance(objectClass);
            registerObjectRead(result, newHandle, unshared);
            registeredResult = result;
        } else {
            result = null;
        }
        try {
            this.currentObject = result;
            this.currentClass = classDesc;
            boolean wasExternalizable = (classDesc.getFlags() & 4) != 0;
            if (wasExternalizable) {
                boolean blockData = (classDesc.getFlags() & 8) != 0;
                if (!blockData) {
                    this.primitiveData = this.input;
                }
                if (this.mustResolve) {
                    Externalizable extern = (Externalizable) result;
                    extern.readExternal(this);
                }
                if (blockData) {
                    discardData();
                } else {
                    this.primitiveData = this.emptyStream;
                }
            } else {
                readHierarchy(result, classDesc);
            }
            if (objectClass != null && classDesc.hasMethodReadResolve()) {
                Method methodReadResolve = classDesc.getMethodReadResolve();
                try {
                    result = methodReadResolve.invoke(result, (Object[]) null);
                } catch (IllegalAccessException e) {
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
            }
            if (result != null && this.enableResolve) {
                result = resolveObject(result);
            }
            if (registeredResult != result) {
                registerObjectRead(result, newHandle, unshared);
            }
            return result;
        } finally {
            this.currentObject = null;
            this.currentClass = null;
        }
    }

    private InvalidClassException missingClassDescriptor() throws InvalidClassException {
        throw new InvalidClassException("Read null attempting to read class descriptor for object");
    }

    private Object readNewString(boolean unshared) throws IOException {
        Object utf = this.input.readUTF();
        if (this.enableResolve) {
            utf = resolveObject(utf);
        }
        registerObjectRead(utf, nextHandle(), unshared);
        return utf;
    }

    private Object readNewLongString(boolean unshared) throws IOException {
        long length = this.input.readLong();
        Object objDecodeUTF = this.input.decodeUTF((int) length);
        if (this.enableResolve) {
            objDecodeUTF = resolveObject(objDecodeUTF);
        }
        registerObjectRead(objDecodeUTF, nextHandle(), unshared);
        return objDecodeUTF;
    }

    @Override
    public final Object readObject() throws ClassNotFoundException, IOException {
        return readObject(false);
    }

    public Object readUnshared() throws IOException, ClassNotFoundException {
        return readObject(true);
    }

    private Object readObject(boolean z) throws ClassNotFoundException, IOException {
        boolean z2 = this.primitiveData == this.input;
        if (z2) {
            this.primitiveData = this.emptyStream;
        }
        if (this.subclassOverridingImplementation && !z) {
            return readObjectOverride();
        }
        try {
            int i = this.nestedLevels + 1;
            this.nestedLevels = i;
            if (i == 1) {
                this.callerClassLoader = VMStack.getClosestUserClassLoader(bootstrapLoader, systemLoader);
            }
            Object nonPrimitiveContent = readNonPrimitiveContent(z);
            if (z2) {
                this.primitiveData = this.input;
            }
            if (this.nestedLevels == 0 && this.validations != null) {
                try {
                    for (InputValidationDesc inputValidationDesc : this.validations) {
                        inputValidationDesc.validator.validateObject();
                    }
                    return nonPrimitiveContent;
                } finally {
                    this.validations = null;
                }
            }
            return nonPrimitiveContent;
        } finally {
            int i2 = this.nestedLevels - 1;
            this.nestedLevels = i2;
            if (i2 == 0) {
                this.callerClassLoader = null;
            }
        }
    }

    protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        if (this.input == null) {
            return null;
        }
        throw new IOException();
    }

    @Override
    public short readShort() throws IOException {
        return this.primitiveTypes.readShort();
    }

    protected void readStreamHeader() throws IOException {
        if (this.input.readShort() == -21267 && this.input.readShort() == 5) {
        } else {
            throw new StreamCorruptedException();
        }
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return this.primitiveTypes.readUnsignedByte();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return this.primitiveTypes.readUnsignedShort();
    }

    @Override
    public String readUTF() throws IOException {
        return this.primitiveTypes.readUTF();
    }

    private Object registeredObjectRead(int handle) throws InvalidObjectException {
        Object res = this.objectsRead.get(handle - ObjectStreamConstants.baseWireHandle);
        if (res == UNSHARED_OBJ) {
            throw new InvalidObjectException("Cannot read back reference to unshared object");
        }
        return res;
    }

    private void registerObjectRead(Object obj, int handle, boolean unshared) throws IOException {
        if (unshared) {
            obj = UNSHARED_OBJ;
        }
        int index = handle - ObjectStreamConstants.baseWireHandle;
        int size = this.objectsRead.size();
        while (index > size) {
            this.objectsRead.add(null);
            size++;
        }
        if (index == size) {
            this.objectsRead.add(obj);
        } else {
            this.objectsRead.set(index, obj);
        }
    }

    public synchronized void registerValidation(ObjectInputValidation object, int priority) throws InvalidObjectException, NotActiveException {
        Object instanceBeingRead = this.currentObject;
        if (instanceBeingRead == null && this.nestedLevels == 0) {
            throw new NotActiveException();
        }
        if (object == null) {
            throw new InvalidObjectException("Callback object cannot be null");
        }
        InputValidationDesc desc = new InputValidationDesc();
        desc.validator = object;
        desc.priority = priority;
        if (this.validations == null) {
            this.validations = new InputValidationDesc[1];
            this.validations[0] = desc;
        } else {
            int i = 0;
            while (i < this.validations.length) {
                InputValidationDesc validation = this.validations[i];
                if (priority >= validation.priority) {
                    break;
                } else {
                    i++;
                }
            }
            InputValidationDesc[] oldValidations = this.validations;
            int currentSize = oldValidations.length;
            this.validations = new InputValidationDesc[currentSize + 1];
            System.arraycopy(oldValidations, 0, this.validations, 0, i);
            System.arraycopy(oldValidations, i, this.validations, i + 1, currentSize - i);
            this.validations[i] = desc;
        }
    }

    private void resetSeenObjects() {
        this.objectsRead = new ArrayList<>();
        this.nextHandle = ObjectStreamConstants.baseWireHandle;
        this.primitiveData = this.emptyStream;
    }

    private void resetState() {
        resetSeenObjects();
        this.hasPushbackTC = false;
        this.pushbackTC = (byte) 0;
    }

    protected Class<?> resolveClass(ObjectStreamClass osClass) throws IOException, ClassNotFoundException {
        Class<?> cls = osClass.forClass();
        if (cls == null) {
            String className = osClass.getName();
            Class<?> cls2 = PRIMITIVE_CLASSES.get(className);
            if (cls2 == null) {
                return Class.forName(className, false, this.callerClassLoader);
            }
            return cls2;
        }
        return cls;
    }

    protected Object resolveObject(Object object) throws IOException {
        return object;
    }

    @Override
    public int skipBytes(int length) throws IOException {
        if (this.input == null) {
            throw new NullPointerException("source stream is null");
        }
        int offset = 0;
        while (offset < length) {
            checkReadPrimitiveTypes();
            long skipped = this.primitiveData.skip(length - offset);
            if (skipped != 0) {
                offset += (int) skipped;
            } else {
                return offset;
            }
        }
        return length;
    }

    private void verifyAndInit(ObjectStreamClass loadedStreamClass) throws InvalidClassException {
        Class<?> localClass = loadedStreamClass.forClass();
        ObjectStreamClass localStreamClass = ObjectStreamClass.lookupStreamClass(localClass);
        if (loadedStreamClass.getSerialVersionUID() != localStreamClass.getSerialVersionUID()) {
            throw new InvalidClassException(loadedStreamClass.getName(), "Incompatible class (SUID): " + loadedStreamClass + " but expected " + localStreamClass);
        }
        String loadedClassBaseName = getBaseName(loadedStreamClass.getName());
        String localClassBaseName = getBaseName(localStreamClass.getName());
        if (!loadedClassBaseName.equals(localClassBaseName)) {
            throw new InvalidClassException(loadedStreamClass.getName(), String.format("Incompatible class (base name): %s but expected %s", loadedClassBaseName, localClassBaseName));
        }
        loadedStreamClass.initPrivateFields(localStreamClass);
    }

    private static String getBaseName(String fullName) {
        int k = fullName.lastIndexOf(46);
        return (k == -1 || k == fullName.length() + (-1)) ? fullName : fullName.substring(k + 1);
    }

    private static void checkedSetSuperClassDesc(ObjectStreamClass desc, ObjectStreamClass superDesc) throws StreamCorruptedException {
        if (desc.equals(superDesc)) {
            throw new StreamCorruptedException();
        }
        desc.setSuperclass(superDesc);
    }
}
