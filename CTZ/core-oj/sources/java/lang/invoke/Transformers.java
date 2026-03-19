package java.lang.invoke;

import dalvik.system.EmulatedStackFrame;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.sql.Types;
import sun.invoke.util.Wrapper;
import sun.misc.Unsafe;

public class Transformers {
    private static final Method TRANSFORM_INTERNAL;

    private Transformers() {
    }

    static {
        try {
            TRANSFORM_INTERNAL = MethodHandle.class.getDeclaredMethod("transformInternal", EmulatedStackFrame.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        }
    }

    public static abstract class Transformer extends MethodHandle implements Cloneable {
        protected Transformer(MethodType methodType) {
            super(Transformers.TRANSFORM_INTERNAL.getArtMethod(), 5, methodType);
        }

        protected Transformer(MethodType methodType, int i) {
            super(Transformers.TRANSFORM_INTERNAL.getArtMethod(), i, methodType);
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    public static class AlwaysThrow extends Transformer {
        private final Class<? extends Throwable> exceptionType;

        public AlwaysThrow(Class<?> cls, Class<? extends Throwable> cls2) {
            super(MethodType.methodType(cls, cls2));
            this.exceptionType = cls2;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            throw ((Throwable) emulatedStackFrame.getReference(0, this.exceptionType));
        }
    }

    public static class DropArguments extends Transformer {
        private final MethodHandle delegate;
        private final EmulatedStackFrame.Range range1;
        private final EmulatedStackFrame.Range range2;

        public DropArguments(MethodType methodType, MethodHandle methodHandle, int i, int i2) {
            super(methodType);
            this.delegate = methodHandle;
            this.range1 = EmulatedStackFrame.Range.of(methodType, 0, i);
            int length = methodType.ptypes().length;
            int i3 = i + i2;
            if (i3 < length) {
                this.range2 = EmulatedStackFrame.Range.of(methodType, i3, length);
            } else {
                this.range2 = null;
            }
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.delegate.type());
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.range1, 0, 0);
            if (this.range2 != null) {
                emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.range2, this.range1.numReferences, this.range1.numBytes);
            }
            (void) this.delegate.invoke(emulatedStackFrameCreate);
            emulatedStackFrameCreate.copyReturnValueTo(emulatedStackFrame);
        }
    }

    public static class CatchException extends Transformer {
        private final Class<?> exType;
        private final MethodHandle handler;
        private final EmulatedStackFrame.Range handlerArgsRange;
        private final MethodHandle target;

        public CatchException(MethodHandle methodHandle, MethodHandle methodHandle2, Class<?> cls) {
            super(methodHandle.type());
            this.target = methodHandle;
            this.handler = methodHandle2;
            this.exType = cls;
            this.handlerArgsRange = EmulatedStackFrame.Range.of(methodHandle.type(), 0, methodHandle2.type().parameterCount() - 1);
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            try {
                (void) this.target.invoke(emulatedStackFrame);
            } catch (Throwable th) {
                if (th.getClass() == this.exType) {
                    EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.handler.type());
                    emulatedStackFrameCreate.setReference(0, th);
                    emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.handlerArgsRange, 1, 0);
                    (void) this.handler.invoke(emulatedStackFrameCreate);
                    emulatedStackFrameCreate.copyReturnValueTo(emulatedStackFrame);
                    return;
                }
                throw th;
            }
        }
    }

    public static class GuardWithTest extends Transformer {
        private final MethodHandle fallback;
        private final MethodHandle target;
        private final MethodHandle test;
        private final EmulatedStackFrame.Range testArgsRange;

        public GuardWithTest(MethodHandle methodHandle, MethodHandle methodHandle2, MethodHandle methodHandle3) {
            super(methodHandle2.type());
            this.test = methodHandle;
            this.target = methodHandle2;
            this.fallback = methodHandle3;
            this.testArgsRange = EmulatedStackFrame.Range.of(methodHandle2.type(), 0, methodHandle.type().parameterCount());
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.test.type());
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.testArgsRange, 0, 0);
            if ((boolean) this.test.invoke(emulatedStackFrameCreate)) {
                (void) this.target.invoke(emulatedStackFrame);
            } else {
                (void) this.fallback.invoke(emulatedStackFrame);
            }
        }
    }

    public static class ReferenceArrayElementGetter extends Transformer {
        private final Class<?> arrayClass;

        public ReferenceArrayElementGetter(Class<?> cls) {
            super(MethodType.methodType(cls.getComponentType(), (Class<?>[]) new Class[]{cls, Integer.TYPE}));
            this.arrayClass = cls;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame.StackFrameReader stackFrameReader = new EmulatedStackFrame.StackFrameReader();
            stackFrameReader.attach(emulatedStackFrame);
            Object[] objArr = (Object[]) stackFrameReader.nextReference(this.arrayClass);
            int iNextInt = stackFrameReader.nextInt();
            EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
            stackFrameWriter.attach(emulatedStackFrame);
            stackFrameWriter.makeReturnValueAccessor();
            stackFrameWriter.putNextReference(objArr[iNextInt], this.arrayClass.getComponentType());
        }
    }

    public static class ReferenceArrayElementSetter extends Transformer {
        private final Class<?> arrayClass;

        public ReferenceArrayElementSetter(Class<?> cls) {
            super(MethodType.methodType(Void.TYPE, (Class<?>[]) new Class[]{cls, Integer.TYPE, cls.getComponentType()}));
            this.arrayClass = cls;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame.StackFrameReader stackFrameReader = new EmulatedStackFrame.StackFrameReader();
            stackFrameReader.attach(emulatedStackFrame);
            ((Object[]) stackFrameReader.nextReference(this.arrayClass))[stackFrameReader.nextInt()] = stackFrameReader.nextReference(this.arrayClass.getComponentType());
        }
    }

    public static class ReferenceIdentity extends Transformer {
        private final Class<?> type;

        public ReferenceIdentity(Class<?> cls) {
            super(MethodType.methodType(cls, cls));
            this.type = cls;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame.StackFrameReader stackFrameReader = new EmulatedStackFrame.StackFrameReader();
            stackFrameReader.attach(emulatedStackFrame);
            EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
            stackFrameWriter.attach(emulatedStackFrame);
            stackFrameWriter.makeReturnValueAccessor();
            stackFrameWriter.putNextReference(stackFrameReader.nextReference(this.type), this.type);
        }
    }

    public static class Constant extends Transformer {
        private double asDouble;
        private float asFloat;
        private int asInt;
        private long asLong;
        private Object asReference;
        private final Class<?> type;
        private char typeChar;

        public Constant(Class<?> cls, Object obj) {
            super(MethodType.methodType(cls));
            this.type = cls;
            if (!cls.isPrimitive()) {
                this.asReference = obj;
                this.typeChar = 'L';
                return;
            }
            if (cls == Integer.TYPE) {
                this.asInt = ((Integer) obj).intValue();
                this.typeChar = 'I';
                return;
            }
            if (cls == Character.TYPE) {
                this.asInt = ((Character) obj).charValue();
                this.typeChar = 'C';
                return;
            }
            if (cls == Short.TYPE) {
                this.asInt = ((Short) obj).shortValue();
                this.typeChar = 'S';
                return;
            }
            if (cls == Byte.TYPE) {
                this.asInt = ((Byte) obj).byteValue();
                this.typeChar = 'B';
                return;
            }
            if (cls == Boolean.TYPE) {
                this.asInt = ((Boolean) obj).booleanValue() ? 1 : 0;
                this.typeChar = 'Z';
                return;
            }
            if (cls == Long.TYPE) {
                this.asLong = ((Long) obj).longValue();
                this.typeChar = 'J';
                return;
            }
            if (cls == Float.TYPE) {
                this.asFloat = ((Float) obj).floatValue();
                this.typeChar = 'F';
            } else if (cls == Double.TYPE) {
                this.asDouble = ((Double) obj).doubleValue();
                this.typeChar = 'D';
            } else {
                throw new AssertionError((Object) ("unknown type: " + this.typeChar));
            }
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
            stackFrameWriter.attach(emulatedStackFrame);
            stackFrameWriter.makeReturnValueAccessor();
            char c = this.typeChar;
            if (c == 'F') {
                stackFrameWriter.putNextFloat(this.asFloat);
                return;
            }
            if (c == 'L') {
                stackFrameWriter.putNextReference(this.asReference, this.type);
                return;
            }
            if (c == 'S') {
                stackFrameWriter.putNextShort((short) this.asInt);
                return;
            }
            if (c != 'Z') {
                switch (c) {
                    case 'B':
                        stackFrameWriter.putNextByte((byte) this.asInt);
                        return;
                    case 'C':
                        stackFrameWriter.putNextChar((char) this.asInt);
                        return;
                    case 'D':
                        stackFrameWriter.putNextDouble(this.asDouble);
                        return;
                    default:
                        switch (c) {
                            case 'I':
                                stackFrameWriter.putNextInt(this.asInt);
                                return;
                            case 'J':
                                stackFrameWriter.putNextLong(this.asLong);
                                return;
                            default:
                                throw new AssertionError((Object) ("Unexpected typeChar: " + this.typeChar));
                        }
                }
            }
            stackFrameWriter.putNextBoolean(this.asInt == 1);
        }
    }

    static class Construct extends Transformer {
        private final EmulatedStackFrame.Range callerRange;
        private final MethodHandle constructorHandle;

        Construct(MethodHandle methodHandle, MethodType methodType) {
            super(methodType);
            this.constructorHandle = methodHandle;
            this.callerRange = EmulatedStackFrame.Range.all(type());
        }

        MethodHandle getConstructorHandle() {
            return this.constructorHandle;
        }

        private static boolean isAbstract(Class<?> cls) {
            return (cls.getModifiers() & 1024) == 1024;
        }

        private static void checkInstantiable(Class<?> cls) throws InstantiationException {
            if (isAbstract(cls)) {
                throw new InstantiationException("Can't instantiate " + (cls.isInterface() ? "interface " : "abstract class ") + ((Object) cls));
            }
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            Class<?> clsRtype = type().rtype();
            checkInstantiable(clsRtype);
            Object objAllocateInstance = Unsafe.getUnsafe().allocateInstance(clsRtype);
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.constructorHandle.type());
            emulatedStackFrameCreate.setReference(0, objAllocateInstance);
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.callerRange, 1, 0);
            (void) this.constructorHandle.invoke(emulatedStackFrameCreate);
            emulatedStackFrame.setReturnValueTo(objAllocateInstance);
        }
    }

    public static class BindTo extends Transformer {
        private final MethodHandle delegate;
        private final EmulatedStackFrame.Range range;
        private final Object receiver;

        public BindTo(MethodHandle methodHandle, Object obj) {
            super(methodHandle.type().dropParameterTypes(0, 1));
            this.delegate = methodHandle;
            this.receiver = obj;
            this.range = EmulatedStackFrame.Range.all(type());
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.delegate.type());
            emulatedStackFrameCreate.setReference(0, this.receiver);
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.range, 1, 0);
            (void) this.delegate.invoke(emulatedStackFrameCreate);
            emulatedStackFrameCreate.copyReturnValueTo(emulatedStackFrame);
        }
    }

    public static class FilterReturnValue extends Transformer {
        private final EmulatedStackFrame.Range allArgs;
        private final MethodHandle filter;
        private final MethodHandle target;

        public FilterReturnValue(MethodHandle methodHandle, MethodHandle methodHandle2) {
            super(MethodType.methodType(methodHandle2.type().rtype(), methodHandle.type().ptypes()));
            this.target = methodHandle;
            this.filter = methodHandle2;
            this.allArgs = EmulatedStackFrame.Range.all(type());
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.target.type());
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.allArgs, 0, 0);
            (void) this.target.invoke(emulatedStackFrameCreate);
            EmulatedStackFrame.StackFrameReader stackFrameReader = new EmulatedStackFrame.StackFrameReader();
            stackFrameReader.attach(emulatedStackFrameCreate);
            stackFrameReader.makeReturnValueAccessor();
            EmulatedStackFrame emulatedStackFrameCreate2 = EmulatedStackFrame.create(this.filter.type());
            EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
            stackFrameWriter.attach(emulatedStackFrameCreate2);
            Class<?> clsRtype = this.target.type().rtype();
            if (!clsRtype.isPrimitive()) {
                stackFrameWriter.putNextReference(stackFrameReader.nextReference(clsRtype), clsRtype);
            } else if (clsRtype == Boolean.TYPE) {
                stackFrameWriter.putNextBoolean(stackFrameReader.nextBoolean());
            } else if (clsRtype == Byte.TYPE) {
                stackFrameWriter.putNextByte(stackFrameReader.nextByte());
            } else if (clsRtype == Character.TYPE) {
                stackFrameWriter.putNextChar(stackFrameReader.nextChar());
            } else if (clsRtype == Short.TYPE) {
                stackFrameWriter.putNextShort(stackFrameReader.nextShort());
            } else if (clsRtype == Integer.TYPE) {
                stackFrameWriter.putNextInt(stackFrameReader.nextInt());
            } else if (clsRtype == Long.TYPE) {
                stackFrameWriter.putNextLong(stackFrameReader.nextLong());
            } else if (clsRtype == Float.TYPE) {
                stackFrameWriter.putNextFloat(stackFrameReader.nextFloat());
            } else if (clsRtype == Double.TYPE) {
                stackFrameWriter.putNextDouble(stackFrameReader.nextDouble());
            }
            (void) this.filter.invoke(emulatedStackFrameCreate2);
            emulatedStackFrameCreate2.copyReturnValueTo(emulatedStackFrame);
        }
    }

    public static class PermuteArguments extends Transformer {
        private final int[] reorder;
        private final MethodHandle target;

        public PermuteArguments(MethodType methodType, MethodHandle methodHandle, int[] iArr) {
            super(methodType);
            this.target = methodHandle;
            this.reorder = iArr;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame.StackFrameReader stackFrameReader = new EmulatedStackFrame.StackFrameReader();
            stackFrameReader.attach(emulatedStackFrame);
            Object[] objArr = new Object[this.reorder.length];
            Class<?>[] clsArrPtypes = type().ptypes();
            for (int i = 0; i < clsArrPtypes.length; i++) {
                Class<?> cls = clsArrPtypes[i];
                if (!cls.isPrimitive()) {
                    objArr[i] = stackFrameReader.nextReference(cls);
                } else if (cls == Boolean.TYPE) {
                    objArr[i] = Boolean.valueOf(stackFrameReader.nextBoolean());
                } else if (cls == Byte.TYPE) {
                    objArr[i] = Byte.valueOf(stackFrameReader.nextByte());
                } else if (cls == Character.TYPE) {
                    objArr[i] = Character.valueOf(stackFrameReader.nextChar());
                } else if (cls == Short.TYPE) {
                    objArr[i] = Short.valueOf(stackFrameReader.nextShort());
                } else if (cls == Integer.TYPE) {
                    objArr[i] = Integer.valueOf(stackFrameReader.nextInt());
                } else if (cls == Long.TYPE) {
                    objArr[i] = Long.valueOf(stackFrameReader.nextLong());
                } else if (cls == Float.TYPE) {
                    objArr[i] = Float.valueOf(stackFrameReader.nextFloat());
                } else if (cls == Double.TYPE) {
                    objArr[i] = Double.valueOf(stackFrameReader.nextDouble());
                } else {
                    throw new AssertionError((Object) ("Unexpected type: " + ((Object) cls)));
                }
            }
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.target.type());
            EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
            stackFrameWriter.attach(emulatedStackFrameCreate);
            for (int i2 = 0; i2 < clsArrPtypes.length; i2++) {
                int i3 = this.reorder[i2];
                Class<?> cls2 = clsArrPtypes[i3];
                Object obj = objArr[i3];
                if (!cls2.isPrimitive()) {
                    stackFrameWriter.putNextReference(obj, cls2);
                } else if (cls2 == Boolean.TYPE) {
                    stackFrameWriter.putNextBoolean(((Boolean) obj).booleanValue());
                } else if (cls2 == Byte.TYPE) {
                    stackFrameWriter.putNextByte(((Byte) obj).byteValue());
                } else if (cls2 == Character.TYPE) {
                    stackFrameWriter.putNextChar(((Character) obj).charValue());
                } else if (cls2 == Short.TYPE) {
                    stackFrameWriter.putNextShort(((Short) obj).shortValue());
                } else if (cls2 == Integer.TYPE) {
                    stackFrameWriter.putNextInt(((Integer) obj).intValue());
                } else if (cls2 == Long.TYPE) {
                    stackFrameWriter.putNextLong(((Long) obj).longValue());
                } else if (cls2 == Float.TYPE) {
                    stackFrameWriter.putNextFloat(((Float) obj).floatValue());
                } else if (cls2 == Double.TYPE) {
                    stackFrameWriter.putNextDouble(((Double) obj).doubleValue());
                } else {
                    throw new AssertionError((Object) ("Unexpected type: " + ((Object) cls2)));
                }
            }
            (void) this.target.invoke(emulatedStackFrameCreate);
            emulatedStackFrameCreate.copyReturnValueTo(emulatedStackFrame);
        }
    }

    static class VarargsCollector extends Transformer {
        final MethodHandle target;

        VarargsCollector(MethodHandle methodHandle) {
            super(methodHandle.type(), 6);
            if (!lastParameterTypeIsAnArray(methodHandle.type().ptypes())) {
                throw new IllegalArgumentException("target does not have array as last parameter");
            }
            this.target = methodHandle;
        }

        private static boolean lastParameterTypeIsAnArray(Class<?>[] clsArr) {
            if (clsArr.length == 0) {
                return false;
            }
            return clsArr[clsArr.length - 1].isArray();
        }

        @Override
        public boolean isVarargsCollector() {
            return true;
        }

        @Override
        public MethodHandle asFixedArity() {
            return this.target;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            MethodType methodType = emulatedStackFrame.getMethodType();
            Class<?>[] clsArrPtypes = methodType.ptypes();
            Class<?>[] clsArrPtypes2 = type().ptypes();
            int length = clsArrPtypes2.length - 1;
            if (clsArrPtypes.length == clsArrPtypes2.length && clsArrPtypes2[length].isAssignableFrom(clsArrPtypes[length])) {
                (void) this.target.invoke(emulatedStackFrame);
                return;
            }
            if (clsArrPtypes.length < clsArrPtypes2.length - 1) {
                throwWrongMethodTypeException(methodType, type());
            }
            if (!MethodType.canConvert(type().rtype(), methodType.rtype())) {
                throwWrongMethodTypeException(methodType, type());
            }
            if (!arityArgumentsConvertible(clsArrPtypes, length, clsArrPtypes2[length].getComponentType())) {
                throwWrongMethodTypeException(methodType, type());
            }
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(makeTargetFrameType(methodType, type()));
            prepareFrame(emulatedStackFrame, emulatedStackFrameCreate);
            (void) this.target.invoke(emulatedStackFrameCreate);
            emulatedStackFrameCreate.copyReturnValueTo(emulatedStackFrame);
        }

        private static void throwWrongMethodTypeException(MethodType methodType, MethodType methodType2) {
            throw new WrongMethodTypeException("Cannot convert " + ((Object) methodType) + " to " + ((Object) methodType2));
        }

        private static boolean arityArgumentsConvertible(Class<?>[] clsArr, int i, Class<?> cls) {
            if (clsArr.length - 1 == i && clsArr[i].isArray() && clsArr[i].getComponentType() == cls) {
                return true;
            }
            while (i < clsArr.length) {
                if (MethodType.canConvert(clsArr[i], cls)) {
                    i++;
                } else {
                    return false;
                }
            }
            return true;
        }

        private static Object referenceArray(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?>[] clsArr, Class<?> cls, int i, int i2) {
            Object objNewInstance = Array.newInstance(cls, i2);
            for (int i3 = 0; i3 < i2; i3++) {
                Class<?> cls2 = clsArr[i3 + i];
                Object objValueOf = null;
                char cBasicTypeChar = Wrapper.basicTypeChar(cls2);
                if (cBasicTypeChar == 'F') {
                    objValueOf = Float.valueOf(stackFrameReader.nextFloat());
                } else if (cBasicTypeChar == 'L') {
                    objValueOf = stackFrameReader.nextReference(cls2);
                } else if (cBasicTypeChar == 'S') {
                    objValueOf = Short.valueOf(stackFrameReader.nextShort());
                } else if (cBasicTypeChar != 'Z') {
                    switch (cBasicTypeChar) {
                        case 'B':
                            objValueOf = Byte.valueOf(stackFrameReader.nextByte());
                            break;
                        case 'C':
                            objValueOf = Character.valueOf(stackFrameReader.nextChar());
                            break;
                        case 'D':
                            objValueOf = Double.valueOf(stackFrameReader.nextDouble());
                            break;
                        default:
                            switch (cBasicTypeChar) {
                                case 'I':
                                    objValueOf = Integer.valueOf(stackFrameReader.nextInt());
                                    break;
                                case 'J':
                                    objValueOf = Long.valueOf(stackFrameReader.nextLong());
                                    break;
                            }
                            break;
                    }
                } else {
                    objValueOf = Boolean.valueOf(stackFrameReader.nextBoolean());
                }
                Array.set(objNewInstance, i3, cls.cast(objValueOf));
            }
            return objNewInstance;
        }

        private static Object intArray(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?>[] clsArr, int i, int i2) {
            int[] iArr = new int[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                Class<?> cls = clsArr[i3 + i];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'B') {
                    iArr[i3] = stackFrameReader.nextByte();
                } else if (cBasicTypeChar == 'I') {
                    iArr[i3] = stackFrameReader.nextInt();
                } else if (cBasicTypeChar == 'S') {
                    iArr[i3] = stackFrameReader.nextShort();
                } else {
                    iArr[i3] = ((Integer) stackFrameReader.nextReference(cls)).intValue();
                }
            }
            return iArr;
        }

        private static Object longArray(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?>[] clsArr, int i, int i2) {
            long[] jArr = new long[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                Class<?> cls = clsArr[i3 + i];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'B') {
                    jArr[i3] = stackFrameReader.nextByte();
                } else if (cBasicTypeChar != 'S') {
                    switch (cBasicTypeChar) {
                        case 'I':
                            jArr[i3] = stackFrameReader.nextInt();
                            break;
                        case 'J':
                            jArr[i3] = stackFrameReader.nextLong();
                            break;
                        default:
                            jArr[i3] = ((Long) stackFrameReader.nextReference(cls)).longValue();
                            break;
                    }
                } else {
                    jArr[i3] = stackFrameReader.nextShort();
                }
            }
            return jArr;
        }

        private static Object byteArray(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?>[] clsArr, int i, int i2) {
            byte[] bArr = new byte[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                Class<?> cls = clsArr[i3 + i];
                if (Wrapper.basicTypeChar(cls) == 'B') {
                    bArr[i3] = stackFrameReader.nextByte();
                } else {
                    bArr[i3] = ((Byte) stackFrameReader.nextReference(cls)).byteValue();
                }
            }
            return bArr;
        }

        private static Object shortArray(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?>[] clsArr, int i, int i2) {
            short[] sArr = new short[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                Class<?> cls = clsArr[i3 + i];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'B') {
                    sArr[i3] = stackFrameReader.nextByte();
                } else if (cBasicTypeChar == 'S') {
                    sArr[i3] = stackFrameReader.nextShort();
                } else {
                    sArr[i3] = ((Short) stackFrameReader.nextReference(cls)).shortValue();
                }
            }
            return sArr;
        }

        private static Object charArray(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?>[] clsArr, int i, int i2) {
            char[] cArr = new char[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                Class<?> cls = clsArr[i3 + i];
                if (Wrapper.basicTypeChar(cls) == 'C') {
                    cArr[i3] = stackFrameReader.nextChar();
                } else {
                    cArr[i3] = ((Character) stackFrameReader.nextReference(cls)).charValue();
                }
            }
            return cArr;
        }

        private static Object booleanArray(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?>[] clsArr, int i, int i2) {
            boolean[] zArr = new boolean[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                Class<?> cls = clsArr[i3 + i];
                if (Wrapper.basicTypeChar(cls) == 'Z') {
                    zArr[i3] = stackFrameReader.nextBoolean();
                } else {
                    zArr[i3] = ((Boolean) stackFrameReader.nextReference(cls)).booleanValue();
                }
            }
            return zArr;
        }

        private static Object floatArray(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?>[] clsArr, int i, int i2) {
            float[] fArr = new float[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                Class<?> cls = clsArr[i3 + i];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'B') {
                    fArr[i3] = stackFrameReader.nextByte();
                } else if (cBasicTypeChar == 'F') {
                    fArr[i3] = stackFrameReader.nextFloat();
                } else if (cBasicTypeChar != 'S') {
                    switch (cBasicTypeChar) {
                        case 'I':
                            fArr[i3] = stackFrameReader.nextInt();
                            break;
                        case 'J':
                            fArr[i3] = stackFrameReader.nextLong();
                            break;
                        default:
                            fArr[i3] = ((Float) stackFrameReader.nextReference(cls)).floatValue();
                            break;
                    }
                } else {
                    fArr[i3] = stackFrameReader.nextShort();
                }
            }
            return fArr;
        }

        private static Object doubleArray(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?>[] clsArr, int i, int i2) {
            double[] dArr = new double[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                Class<?> cls = clsArr[i3 + i];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'B') {
                    dArr[i3] = stackFrameReader.nextByte();
                } else if (cBasicTypeChar == 'D') {
                    dArr[i3] = stackFrameReader.nextDouble();
                } else if (cBasicTypeChar == 'F') {
                    dArr[i3] = stackFrameReader.nextFloat();
                } else if (cBasicTypeChar != 'S') {
                    switch (cBasicTypeChar) {
                        case 'I':
                            dArr[i3] = stackFrameReader.nextInt();
                            break;
                        case 'J':
                            dArr[i3] = stackFrameReader.nextLong();
                            break;
                        default:
                            dArr[i3] = ((Double) stackFrameReader.nextReference(cls)).doubleValue();
                            break;
                    }
                } else {
                    dArr[i3] = stackFrameReader.nextShort();
                }
            }
            return dArr;
        }

        private static Object makeArityArray(MethodType methodType, EmulatedStackFrame.StackFrameReader stackFrameReader, int i, Class<?> cls) {
            int length = methodType.ptypes().length - i;
            Class<?> componentType = cls.getComponentType();
            Class<?>[] clsArrPtypes = methodType.ptypes();
            char cBasicTypeChar = Wrapper.basicTypeChar(componentType);
            if (cBasicTypeChar == 'F') {
                return floatArray(stackFrameReader, clsArrPtypes, i, length);
            }
            if (cBasicTypeChar == 'L') {
                return referenceArray(stackFrameReader, clsArrPtypes, componentType, i, length);
            }
            if (cBasicTypeChar == 'S') {
                return shortArray(stackFrameReader, clsArrPtypes, i, length);
            }
            if (cBasicTypeChar != 'Z') {
                switch (cBasicTypeChar) {
                    case 'B':
                        return byteArray(stackFrameReader, clsArrPtypes, i, length);
                    case 'C':
                        return charArray(stackFrameReader, clsArrPtypes, i, length);
                    case 'D':
                        return doubleArray(stackFrameReader, clsArrPtypes, i, length);
                    default:
                        switch (cBasicTypeChar) {
                            case 'I':
                                return intArray(stackFrameReader, clsArrPtypes, i, length);
                            case 'J':
                                return longArray(stackFrameReader, clsArrPtypes, i, length);
                            default:
                                throw new InternalError("Unexpected type: " + ((Object) componentType));
                        }
                }
            }
            return booleanArray(stackFrameReader, clsArrPtypes, i, length);
        }

        public static Object collectArguments(char c, Class<?> cls, EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?>[] clsArr, int i, int i2) {
            if (c == 'F') {
                return floatArray(stackFrameReader, clsArr, i, i2);
            }
            if (c == 'L') {
                return referenceArray(stackFrameReader, clsArr, cls, i, i2);
            }
            if (c == 'S') {
                return shortArray(stackFrameReader, clsArr, i, i2);
            }
            if (c != 'Z') {
                switch (c) {
                    case 'B':
                        return byteArray(stackFrameReader, clsArr, i, i2);
                    case 'C':
                        return charArray(stackFrameReader, clsArr, i, i2);
                    case 'D':
                        return doubleArray(stackFrameReader, clsArr, i, i2);
                    default:
                        switch (c) {
                            case 'I':
                                return intArray(stackFrameReader, clsArr, i, i2);
                            case 'J':
                                return longArray(stackFrameReader, clsArr, i, i2);
                            default:
                                throw new InternalError("Unexpected type: " + c);
                        }
                }
            }
            return booleanArray(stackFrameReader, clsArr, i, i2);
        }

        private static void copyParameter(EmulatedStackFrame.StackFrameReader stackFrameReader, EmulatedStackFrame.StackFrameWriter stackFrameWriter, Class<?> cls) {
            char cBasicTypeChar = Wrapper.basicTypeChar(cls);
            if (cBasicTypeChar == 'F') {
                stackFrameWriter.putNextFloat(stackFrameReader.nextFloat());
                return;
            }
            if (cBasicTypeChar == 'L') {
                stackFrameWriter.putNextReference(stackFrameReader.nextReference(cls), cls);
                return;
            }
            if (cBasicTypeChar == 'S') {
                stackFrameWriter.putNextShort(stackFrameReader.nextShort());
                return;
            }
            if (cBasicTypeChar != 'Z') {
                switch (cBasicTypeChar) {
                    case 'B':
                        stackFrameWriter.putNextByte(stackFrameReader.nextByte());
                        return;
                    case 'C':
                        stackFrameWriter.putNextChar(stackFrameReader.nextChar());
                        return;
                    case 'D':
                        stackFrameWriter.putNextDouble(stackFrameReader.nextDouble());
                        return;
                    default:
                        switch (cBasicTypeChar) {
                            case 'I':
                                stackFrameWriter.putNextInt(stackFrameReader.nextInt());
                                return;
                            case 'J':
                                stackFrameWriter.putNextLong(stackFrameReader.nextLong());
                                return;
                            default:
                                throw new InternalError("Unexpected type: " + ((Object) cls));
                        }
                }
            }
            stackFrameWriter.putNextBoolean(stackFrameReader.nextBoolean());
        }

        private static void prepareFrame(EmulatedStackFrame emulatedStackFrame, EmulatedStackFrame emulatedStackFrame2) {
            EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
            stackFrameWriter.attach(emulatedStackFrame2);
            EmulatedStackFrame.StackFrameReader stackFrameReader = new EmulatedStackFrame.StackFrameReader();
            stackFrameReader.attach(emulatedStackFrame);
            MethodType methodType = emulatedStackFrame2.getMethodType();
            int length = methodType.ptypes().length - 1;
            for (int i = 0; i < length; i++) {
                copyParameter(stackFrameReader, stackFrameWriter, methodType.ptypes()[i]);
            }
            Class<?> cls = methodType.ptypes()[length];
            stackFrameWriter.putNextReference(makeArityArray(emulatedStackFrame.getMethodType(), stackFrameReader, length, cls), cls);
        }

        private static MethodType makeTargetFrameType(MethodType methodType, MethodType methodType2) {
            int length = methodType2.ptypes().length;
            Class[] clsArr = new Class[length];
            int i = length - 1;
            System.arraycopy(methodType.ptypes(), 0, clsArr, 0, i);
            clsArr[i] = methodType2.ptypes()[i];
            return MethodType.methodType(methodType.rtype(), (Class<?>[]) clsArr);
        }
    }

    static class Invoker extends Transformer {
        private final EmulatedStackFrame.Range copyRange;
        private final boolean isExactInvoker;
        private final MethodType targetType;

        Invoker(MethodType methodType, boolean z) {
            super(methodType.insertParameterTypes(0, MethodHandle.class));
            this.targetType = methodType;
            this.isExactInvoker = z;
            this.copyRange = EmulatedStackFrame.Range.of(type(), 1, type().parameterCount());
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            if (this.isExactInvoker) {
                MethodType methodTypeDropParameterTypes = emulatedStackFrame.getCallsiteType().dropParameterTypes(0, 1);
                if (!this.targetType.equals((Object) methodTypeDropParameterTypes)) {
                    throw new WrongMethodTypeException("Wrong type, Expected: " + ((Object) this.targetType) + " was: " + ((Object) methodTypeDropParameterTypes));
                }
            }
            MethodHandle methodHandle = (MethodHandle) emulatedStackFrame.getReference(0, MethodHandle.class);
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.targetType);
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.copyRange, 0, 0);
            (void) methodHandle.invoke(emulatedStackFrameCreate);
            emulatedStackFrameCreate.copyReturnValueTo(emulatedStackFrame);
        }
    }

    static class Spreader extends Transformer {
        private final int arrayOffset;
        private final char arrayTypeChar;
        private final EmulatedStackFrame.Range copyRange;
        private final int numArrayArgs;
        private final MethodHandle target;

        Spreader(MethodHandle methodHandle, MethodType methodType, int i) {
            super(methodType);
            this.target = methodHandle;
            this.arrayOffset = methodType.parameterCount() - 1;
            Class<?> componentType = methodType.ptypes()[this.arrayOffset].getComponentType();
            if (componentType == null) {
                throw new AssertionError((Object) "Trailing argument must be an array.");
            }
            this.arrayTypeChar = Wrapper.basicTypeChar(componentType);
            this.numArrayArgs = i;
            this.copyRange = EmulatedStackFrame.Range.of(methodType, 0, this.arrayOffset);
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.target.type());
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.copyRange, 0, 0);
            EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
            stackFrameWriter.attach(emulatedStackFrameCreate, this.arrayOffset, this.copyRange.numReferences, this.copyRange.numBytes);
            Object reference = emulatedStackFrame.getReference(this.copyRange.numReferences, type().ptypes()[this.arrayOffset]);
            int length = Array.getLength(reference);
            if (length != this.numArrayArgs) {
                throw new IllegalArgumentException("Invalid array length: " + length);
            }
            MethodType methodTypeType = this.target.type();
            char c = this.arrayTypeChar;
            if (c == 'F') {
                spreadArray((float[]) reference, stackFrameWriter, methodTypeType, this.numArrayArgs, this.arrayOffset);
            } else if (c == 'L') {
                spreadArray((Object[]) reference, stackFrameWriter, methodTypeType, this.numArrayArgs, this.arrayOffset);
            } else if (c == 'S') {
                spreadArray((short[]) reference, stackFrameWriter, methodTypeType, this.numArrayArgs, this.arrayOffset);
            } else if (c != 'Z') {
                switch (c) {
                    case 'B':
                        spreadArray((byte[]) reference, stackFrameWriter, methodTypeType, this.numArrayArgs, this.arrayOffset);
                        break;
                    case 'C':
                        spreadArray((char[]) reference, stackFrameWriter, methodTypeType, this.numArrayArgs, this.arrayOffset);
                        break;
                    case 'D':
                        spreadArray((double[]) reference, stackFrameWriter, methodTypeType, this.numArrayArgs, this.arrayOffset);
                        break;
                    default:
                        switch (c) {
                            case 'I':
                                spreadArray((int[]) reference, stackFrameWriter, methodTypeType, this.numArrayArgs, this.arrayOffset);
                                break;
                            case 'J':
                                spreadArray((long[]) reference, stackFrameWriter, methodTypeType, this.numArrayArgs, this.arrayOffset);
                                break;
                        }
                        break;
                }
            } else {
                spreadArray((boolean[]) reference, stackFrameWriter, methodTypeType, this.numArrayArgs, this.arrayOffset);
            }
            (void) this.target.invoke(emulatedStackFrameCreate);
            emulatedStackFrameCreate.copyReturnValueTo(emulatedStackFrame);
        }

        public static void spreadArray(Object[] objArr, EmulatedStackFrame.StackFrameWriter stackFrameWriter, MethodType methodType, int i, int i2) {
            Class<?>[] clsArrPtypes = methodType.ptypes();
            for (int i3 = 0; i3 < i; i3++) {
                Class<?> cls = clsArrPtypes[i3 + i2];
                Object obj = objArr[i3];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'F') {
                    stackFrameWriter.putNextFloat(((Float) obj).floatValue());
                } else if (cBasicTypeChar == 'L') {
                    stackFrameWriter.putNextReference(obj, cls);
                } else if (cBasicTypeChar == 'S') {
                    stackFrameWriter.putNextShort(((Short) obj).shortValue());
                } else if (cBasicTypeChar != 'Z') {
                    switch (cBasicTypeChar) {
                        case 'B':
                            stackFrameWriter.putNextByte(((Byte) obj).byteValue());
                            break;
                        case 'C':
                            stackFrameWriter.putNextChar(((Character) obj).charValue());
                            break;
                        case 'D':
                            stackFrameWriter.putNextDouble(((Double) obj).doubleValue());
                            break;
                        default:
                            switch (cBasicTypeChar) {
                                case 'I':
                                    stackFrameWriter.putNextInt(((Integer) obj).intValue());
                                    break;
                                case 'J':
                                    stackFrameWriter.putNextLong(((Long) obj).longValue());
                                    break;
                            }
                            break;
                    }
                } else {
                    stackFrameWriter.putNextBoolean(((Boolean) obj).booleanValue());
                }
            }
        }

        public static void spreadArray(int[] iArr, EmulatedStackFrame.StackFrameWriter stackFrameWriter, MethodType methodType, int i, int i2) {
            Class<?>[] clsArrPtypes = methodType.ptypes();
            for (int i3 = 0; i3 < i; i3++) {
                Class<?> cls = clsArrPtypes[i3 + i2];
                int i4 = iArr[i3];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'D') {
                    stackFrameWriter.putNextDouble(i4);
                } else if (cBasicTypeChar == 'F') {
                    stackFrameWriter.putNextFloat(i4);
                } else if (cBasicTypeChar == 'L') {
                    stackFrameWriter.putNextReference(Integer.valueOf(i4), cls);
                } else {
                    switch (cBasicTypeChar) {
                        case 'I':
                            stackFrameWriter.putNextInt(i4);
                            break;
                        case 'J':
                            stackFrameWriter.putNextLong(i4);
                            break;
                        default:
                            throw new AssertionError();
                    }
                }
            }
        }

        public static void spreadArray(long[] jArr, EmulatedStackFrame.StackFrameWriter stackFrameWriter, MethodType methodType, int i, int i2) {
            Class<?>[] clsArrPtypes = methodType.ptypes();
            for (int i3 = 0; i3 < i; i3++) {
                Class<?> cls = clsArrPtypes[i3 + i2];
                long j = jArr[i3];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'D') {
                    stackFrameWriter.putNextDouble(j);
                } else if (cBasicTypeChar == 'F') {
                    stackFrameWriter.putNextFloat(j);
                } else if (cBasicTypeChar == 'J') {
                    stackFrameWriter.putNextLong(j);
                } else if (cBasicTypeChar == 'L') {
                    stackFrameWriter.putNextReference(Long.valueOf(j), cls);
                } else {
                    throw new AssertionError();
                }
            }
        }

        public static void spreadArray(byte[] bArr, EmulatedStackFrame.StackFrameWriter stackFrameWriter, MethodType methodType, int i, int i2) {
            Class<?>[] clsArrPtypes = methodType.ptypes();
            for (int i3 = 0; i3 < i; i3++) {
                Class<?> cls = clsArrPtypes[i3 + i2];
                byte b = bArr[i3];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'B') {
                    stackFrameWriter.putNextByte(b);
                } else if (cBasicTypeChar == 'D') {
                    stackFrameWriter.putNextDouble(b);
                } else if (cBasicTypeChar == 'F') {
                    stackFrameWriter.putNextFloat(b);
                } else if (cBasicTypeChar == 'L') {
                    stackFrameWriter.putNextReference(Byte.valueOf(b), cls);
                } else if (cBasicTypeChar != 'S') {
                    switch (cBasicTypeChar) {
                        case 'I':
                            stackFrameWriter.putNextInt(b);
                            break;
                        case 'J':
                            stackFrameWriter.putNextLong(b);
                            break;
                        default:
                            throw new AssertionError();
                    }
                } else {
                    stackFrameWriter.putNextShort(b);
                }
            }
        }

        public static void spreadArray(short[] sArr, EmulatedStackFrame.StackFrameWriter stackFrameWriter, MethodType methodType, int i, int i2) {
            Class<?>[] clsArrPtypes = methodType.ptypes();
            for (int i3 = 0; i3 < i; i3++) {
                Class<?> cls = clsArrPtypes[i3 + i2];
                short s = sArr[i3];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'D') {
                    stackFrameWriter.putNextDouble(s);
                } else if (cBasicTypeChar == 'F') {
                    stackFrameWriter.putNextFloat(s);
                } else if (cBasicTypeChar == 'L') {
                    stackFrameWriter.putNextReference(Short.valueOf(s), cls);
                } else if (cBasicTypeChar != 'S') {
                    switch (cBasicTypeChar) {
                        case 'I':
                            stackFrameWriter.putNextInt(s);
                            break;
                        case 'J':
                            stackFrameWriter.putNextLong(s);
                            break;
                        default:
                            throw new AssertionError();
                    }
                } else {
                    stackFrameWriter.putNextShort(s);
                }
            }
        }

        public static void spreadArray(char[] cArr, EmulatedStackFrame.StackFrameWriter stackFrameWriter, MethodType methodType, int i, int i2) {
            Class<?>[] clsArrPtypes = methodType.ptypes();
            for (int i3 = 0; i3 < i; i3++) {
                Class<?> cls = clsArrPtypes[i3 + i2];
                char c = cArr[i3];
                switch (Wrapper.basicTypeChar(cls)) {
                    case 'C':
                        stackFrameWriter.putNextChar(c);
                        break;
                    case 'D':
                        stackFrameWriter.putNextDouble(c);
                        break;
                    case 'E':
                    case 'G':
                    case 'H':
                    case 'K':
                    default:
                        throw new AssertionError();
                    case Types.DATALINK:
                        stackFrameWriter.putNextFloat(c);
                        break;
                    case 'I':
                        stackFrameWriter.putNextInt(c);
                        break;
                    case 'J':
                        stackFrameWriter.putNextLong(c);
                        break;
                    case 'L':
                        stackFrameWriter.putNextReference(Character.valueOf(c), cls);
                        break;
                }
            }
        }

        public static void spreadArray(boolean[] zArr, EmulatedStackFrame.StackFrameWriter stackFrameWriter, MethodType methodType, int i, int i2) {
            Class<?>[] clsArrPtypes = methodType.ptypes();
            for (int i3 = 0; i3 < i; i3++) {
                Class<?> cls = clsArrPtypes[i3 + i2];
                boolean z = zArr[i3];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'L') {
                    stackFrameWriter.putNextReference(Boolean.valueOf(z), cls);
                } else if (cBasicTypeChar == 'Z') {
                    stackFrameWriter.putNextBoolean(z);
                } else {
                    throw new AssertionError();
                }
            }
        }

        public static void spreadArray(double[] dArr, EmulatedStackFrame.StackFrameWriter stackFrameWriter, MethodType methodType, int i, int i2) {
            Class<?>[] clsArrPtypes = methodType.ptypes();
            for (int i3 = 0; i3 < i; i3++) {
                Class<?> cls = clsArrPtypes[i3 + i2];
                double d = dArr[i3];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'D') {
                    stackFrameWriter.putNextDouble(d);
                } else if (cBasicTypeChar == 'L') {
                    stackFrameWriter.putNextReference(Double.valueOf(d), cls);
                } else {
                    throw new AssertionError();
                }
            }
        }

        public static void spreadArray(float[] fArr, EmulatedStackFrame.StackFrameWriter stackFrameWriter, MethodType methodType, int i, int i2) {
            Class<?>[] clsArrPtypes = methodType.ptypes();
            for (int i3 = 0; i3 < i; i3++) {
                Class<?> cls = clsArrPtypes[i3 + i2];
                float f = fArr[i3];
                char cBasicTypeChar = Wrapper.basicTypeChar(cls);
                if (cBasicTypeChar == 'D') {
                    stackFrameWriter.putNextDouble(f);
                } else if (cBasicTypeChar == 'F') {
                    stackFrameWriter.putNextFloat(f);
                } else if (cBasicTypeChar == 'L') {
                    stackFrameWriter.putNextReference(Float.valueOf(f), cls);
                } else {
                    throw new AssertionError();
                }
            }
        }
    }

    static class Collector extends Transformer {
        private final int arrayOffset;
        private final char arrayTypeChar;
        private final EmulatedStackFrame.Range copyRange;
        private final int numArrayArgs;
        private final MethodHandle target;

        Collector(MethodHandle methodHandle, Class<?> cls, int i) {
            super(methodHandle.type().asCollectorType(cls, i));
            this.target = methodHandle;
            this.arrayOffset = methodHandle.type().parameterCount() - 1;
            this.arrayTypeChar = Wrapper.basicTypeChar(cls.getComponentType());
            this.numArrayArgs = i;
            this.copyRange = EmulatedStackFrame.Range.of(methodHandle.type(), 0, this.arrayOffset);
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.target.type());
            int i = 0;
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.copyRange, 0, 0);
            EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
            stackFrameWriter.attach(emulatedStackFrameCreate, this.arrayOffset, this.copyRange.numReferences, this.copyRange.numBytes);
            EmulatedStackFrame.StackFrameReader stackFrameReader = new EmulatedStackFrame.StackFrameReader();
            stackFrameReader.attach(emulatedStackFrame, this.arrayOffset, this.copyRange.numReferences, this.copyRange.numBytes);
            char c = this.arrayTypeChar;
            if (c == 'F') {
                float[] fArr = new float[this.numArrayArgs];
                while (i < this.numArrayArgs) {
                    fArr[i] = stackFrameReader.nextFloat();
                    i++;
                }
                stackFrameWriter.putNextReference(fArr, float[].class);
            } else if (c == 'L') {
                Class<?> cls = this.target.type().ptypes()[this.arrayOffset];
                Class<?> componentType = cls.getComponentType();
                Class<?> clsLastParameterType = type().lastParameterType();
                Object[] objArr = (Object[]) Array.newInstance(componentType, this.numArrayArgs);
                while (i < this.numArrayArgs) {
                    objArr[i] = stackFrameReader.nextReference(clsLastParameterType);
                    i++;
                }
                stackFrameWriter.putNextReference(objArr, cls);
            } else if (c == 'S') {
                short[] sArr = new short[this.numArrayArgs];
                while (i < this.numArrayArgs) {
                    sArr[i] = stackFrameReader.nextShort();
                    i++;
                }
                stackFrameWriter.putNextReference(sArr, short[].class);
            } else if (c != 'Z') {
                switch (c) {
                    case 'B':
                        byte[] bArr = new byte[this.numArrayArgs];
                        while (i < this.numArrayArgs) {
                            bArr[i] = stackFrameReader.nextByte();
                            i++;
                        }
                        stackFrameWriter.putNextReference(bArr, byte[].class);
                        break;
                    case 'C':
                        char[] cArr = new char[this.numArrayArgs];
                        while (i < this.numArrayArgs) {
                            cArr[i] = stackFrameReader.nextChar();
                            i++;
                        }
                        stackFrameWriter.putNextReference(cArr, char[].class);
                        break;
                    case 'D':
                        double[] dArr = new double[this.numArrayArgs];
                        while (i < this.numArrayArgs) {
                            dArr[i] = stackFrameReader.nextDouble();
                            i++;
                        }
                        stackFrameWriter.putNextReference(dArr, double[].class);
                        break;
                    default:
                        switch (c) {
                            case 'I':
                                int[] iArr = new int[this.numArrayArgs];
                                while (i < this.numArrayArgs) {
                                    iArr[i] = stackFrameReader.nextInt();
                                    i++;
                                }
                                stackFrameWriter.putNextReference(iArr, int[].class);
                                break;
                            case 'J':
                                long[] jArr = new long[this.numArrayArgs];
                                while (i < this.numArrayArgs) {
                                    jArr[i] = stackFrameReader.nextLong();
                                    i++;
                                }
                                stackFrameWriter.putNextReference(jArr, long[].class);
                                break;
                        }
                        break;
                }
            } else {
                boolean[] zArr = new boolean[this.numArrayArgs];
                while (i < this.numArrayArgs) {
                    zArr[i] = stackFrameReader.nextBoolean();
                    i++;
                }
                stackFrameWriter.putNextReference(zArr, boolean[].class);
            }
            (void) this.target.invoke(emulatedStackFrameCreate);
            emulatedStackFrameCreate.copyReturnValueTo(emulatedStackFrame);
        }
    }

    static class FilterArguments extends Transformer {
        private final MethodHandle[] filters;
        private final int pos;
        private final MethodHandle target;

        FilterArguments(MethodHandle methodHandle, int i, MethodHandle[] methodHandleArr) {
            super(deriveType(methodHandle, i, methodHandleArr));
            this.target = methodHandle;
            this.pos = i;
            this.filters = methodHandleArr;
        }

        private static MethodType deriveType(MethodHandle methodHandle, int i, MethodHandle[] methodHandleArr) {
            Class<?>[] clsArr = new Class[methodHandleArr.length];
            for (int i2 = 0; i2 < methodHandleArr.length; i2++) {
                clsArr[i2] = methodHandleArr[i2].type().parameterType(0);
            }
            return methodHandle.type().replaceParameterTypes(i, methodHandleArr.length + i, clsArr);
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame.StackFrameReader stackFrameReader = new EmulatedStackFrame.StackFrameReader();
            stackFrameReader.attach(emulatedStackFrame);
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.target.type());
            EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
            stackFrameWriter.attach(emulatedStackFrameCreate);
            Class<?>[] clsArrPtypes = this.target.type().ptypes();
            for (int i = 0; i < clsArrPtypes.length; i++) {
                Class<?> cls = clsArrPtypes[i];
                MethodHandle methodHandle = null;
                if (i >= this.pos && i < this.pos + this.filters.length) {
                    methodHandle = this.filters[i - this.pos];
                }
                if (methodHandle != null) {
                    EmulatedStackFrame emulatedStackFrameCreate2 = EmulatedStackFrame.create(methodHandle.type());
                    EmulatedStackFrame.StackFrameWriter stackFrameWriter2 = new EmulatedStackFrame.StackFrameWriter();
                    stackFrameWriter2.attach(emulatedStackFrameCreate2);
                    EmulatedStackFrame.StackFrameAccessor.copyNext(stackFrameReader, stackFrameWriter2, methodHandle.type().ptypes()[0]);
                    (void) methodHandle.invoke(emulatedStackFrameCreate2);
                    EmulatedStackFrame.StackFrameReader stackFrameReader2 = new EmulatedStackFrame.StackFrameReader();
                    stackFrameReader2.attach(emulatedStackFrameCreate2);
                    stackFrameReader2.makeReturnValueAccessor();
                    EmulatedStackFrame.StackFrameAccessor.copyNext(stackFrameReader2, stackFrameWriter, cls);
                } else {
                    EmulatedStackFrame.StackFrameAccessor.copyNext(stackFrameReader, stackFrameWriter, cls);
                }
            }
            (void) this.target.invoke(emulatedStackFrameCreate);
            emulatedStackFrameCreate.copyReturnValueTo(emulatedStackFrame);
        }
    }

    static class CollectArguments extends Transformer {
        private final MethodHandle collector;
        private final EmulatedStackFrame.Range collectorRange;
        private final int pos;
        private final EmulatedStackFrame.Range range1;
        private final EmulatedStackFrame.Range range2;
        private final int referencesOffset;
        private final int stackFrameOffset;
        private final MethodHandle target;

        CollectArguments(MethodHandle methodHandle, MethodHandle methodHandle2, int i, MethodType methodType) {
            super(methodType);
            this.target = methodHandle;
            this.collector = methodHandle2;
            this.pos = i;
            int iParameterCount = methodHandle2.type().parameterCount();
            int iParameterCount2 = type().parameterCount();
            int i2 = iParameterCount + i;
            this.collectorRange = EmulatedStackFrame.Range.of(type(), i, i2);
            this.range1 = EmulatedStackFrame.Range.of(type(), 0, i);
            if (i2 < iParameterCount2) {
                this.range2 = EmulatedStackFrame.Range.of(type(), i2, iParameterCount2);
            } else {
                this.range2 = null;
            }
            Class<?> clsRtype = methodHandle2.type().rtype();
            if (clsRtype == Void.TYPE) {
                this.stackFrameOffset = 0;
                this.referencesOffset = 0;
            } else if (clsRtype.isPrimitive()) {
                this.stackFrameOffset = EmulatedStackFrame.getSize(clsRtype);
                this.referencesOffset = 0;
            } else {
                this.stackFrameOffset = 0;
                this.referencesOffset = 1;
            }
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.collector.type());
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.collectorRange, 0, 0);
            (void) this.collector.invoke(emulatedStackFrameCreate);
            EmulatedStackFrame emulatedStackFrameCreate2 = EmulatedStackFrame.create(this.target.type());
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate2, this.range1, 0, 0);
            if (this.referencesOffset != 0 || this.stackFrameOffset != 0) {
                EmulatedStackFrame.StackFrameReader stackFrameReader = new EmulatedStackFrame.StackFrameReader();
                stackFrameReader.attach(emulatedStackFrameCreate).makeReturnValueAccessor();
                EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
                stackFrameWriter.attach(emulatedStackFrameCreate2, this.pos, this.range1.numReferences, this.range1.numBytes);
                EmulatedStackFrame.StackFrameAccessor.copyNext(stackFrameReader, stackFrameWriter, this.target.type().ptypes()[0]);
            }
            if (this.range2 != null) {
                emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate2, this.range2, this.range1.numReferences + this.referencesOffset, this.range2.numBytes + this.stackFrameOffset);
            }
            (void) this.target.invoke(emulatedStackFrameCreate2);
            emulatedStackFrameCreate2.copyReturnValueTo(emulatedStackFrame);
        }
    }

    static class FoldArguments extends Transformer {
        private final MethodHandle combiner;
        private final EmulatedStackFrame.Range combinerArgs;
        private final int referencesOffset;
        private final int stackFrameOffset;
        private final MethodHandle target;
        private final EmulatedStackFrame.Range targetArgs;

        FoldArguments(MethodHandle methodHandle, MethodHandle methodHandle2) {
            super(deriveType(methodHandle, methodHandle2));
            this.target = methodHandle;
            this.combiner = methodHandle2;
            this.combinerArgs = EmulatedStackFrame.Range.all(methodHandle2.type());
            this.targetArgs = EmulatedStackFrame.Range.all(type());
            Class<?> clsRtype = methodHandle2.type().rtype();
            if (clsRtype == Void.TYPE) {
                this.stackFrameOffset = 0;
                this.referencesOffset = 0;
            } else if (clsRtype.isPrimitive()) {
                this.stackFrameOffset = EmulatedStackFrame.getSize(clsRtype);
                this.referencesOffset = 0;
            } else {
                this.stackFrameOffset = 0;
                this.referencesOffset = 1;
            }
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.combiner.type());
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.combinerArgs, 0, 0);
            (void) this.combiner.invoke(emulatedStackFrameCreate);
            EmulatedStackFrame emulatedStackFrameCreate2 = EmulatedStackFrame.create(this.target.type());
            if (this.referencesOffset != 0 || this.stackFrameOffset != 0) {
                EmulatedStackFrame.StackFrameReader stackFrameReader = new EmulatedStackFrame.StackFrameReader();
                stackFrameReader.attach(emulatedStackFrameCreate).makeReturnValueAccessor();
                EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
                stackFrameWriter.attach(emulatedStackFrameCreate2);
                EmulatedStackFrame.StackFrameAccessor.copyNext(stackFrameReader, stackFrameWriter, this.target.type().ptypes()[0]);
            }
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate2, this.targetArgs, this.referencesOffset, this.stackFrameOffset);
            (void) this.target.invoke(emulatedStackFrameCreate2);
            emulatedStackFrameCreate2.copyReturnValueTo(emulatedStackFrame);
        }

        private static MethodType deriveType(MethodHandle methodHandle, MethodHandle methodHandle2) {
            if (methodHandle2.type().rtype() == Void.TYPE) {
                return methodHandle.type();
            }
            return methodHandle.type().dropParameterTypes(0, 1);
        }
    }

    static class InsertArguments extends Transformer {
        private final int pos;
        private final EmulatedStackFrame.Range range1;
        private final EmulatedStackFrame.Range range2;
        private final MethodHandle target;
        private final Object[] values;

        InsertArguments(MethodHandle methodHandle, int i, Object[] objArr) {
            super(methodHandle.type().dropParameterTypes(i, objArr.length + i));
            this.target = methodHandle;
            this.pos = i;
            this.values = objArr;
            MethodType methodTypeType = type();
            this.range1 = EmulatedStackFrame.Range.of(methodTypeType, 0, i);
            this.range2 = EmulatedStackFrame.Range.of(methodTypeType, i, methodTypeType.parameterCount());
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.target.type());
            emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.range1, 0, 0);
            EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
            stackFrameWriter.attach(emulatedStackFrameCreate, this.pos, this.range1.numReferences, this.range1.numBytes);
            Class<?>[] clsArrPtypes = this.target.type().ptypes();
            int i = 0;
            int size = 0;
            for (int i2 = 0; i2 < this.values.length; i2++) {
                Class<?> cls = clsArrPtypes[this.pos + i2];
                if (cls.isPrimitive()) {
                    if (cls == Boolean.TYPE) {
                        stackFrameWriter.putNextBoolean(((Boolean) this.values[i2]).booleanValue());
                    } else if (cls == Byte.TYPE) {
                        stackFrameWriter.putNextByte(((Byte) this.values[i2]).byteValue());
                    } else if (cls == Character.TYPE) {
                        stackFrameWriter.putNextChar(((Character) this.values[i2]).charValue());
                    } else if (cls == Short.TYPE) {
                        stackFrameWriter.putNextShort(((Short) this.values[i2]).shortValue());
                    } else if (cls == Integer.TYPE) {
                        stackFrameWriter.putNextInt(((Integer) this.values[i2]).intValue());
                    } else if (cls == Long.TYPE) {
                        stackFrameWriter.putNextLong(((Long) this.values[i2]).longValue());
                    } else if (cls == Float.TYPE) {
                        stackFrameWriter.putNextFloat(((Float) this.values[i2]).floatValue());
                    } else if (cls == Double.TYPE) {
                        stackFrameWriter.putNextDouble(((Double) this.values[i2]).doubleValue());
                    }
                    size += EmulatedStackFrame.getSize(cls);
                } else {
                    stackFrameWriter.putNextReference(this.values[i2], cls);
                    i++;
                }
            }
            if (this.range2 != null) {
                emulatedStackFrame.copyRangeTo(emulatedStackFrameCreate, this.range2, this.range1.numReferences + i, this.range1.numBytes + size);
            }
            (void) this.target.invoke(emulatedStackFrameCreate);
            emulatedStackFrameCreate.copyReturnValueTo(emulatedStackFrame);
        }
    }

    public static class ExplicitCastArguments extends Transformer {
        private final MethodHandle target;

        public ExplicitCastArguments(MethodHandle methodHandle, MethodType methodType) {
            super(methodType);
            this.target = methodHandle;
        }

        @Override
        public void transform(EmulatedStackFrame emulatedStackFrame) throws Throwable {
            EmulatedStackFrame emulatedStackFrameCreate = EmulatedStackFrame.create(this.target.type());
            explicitCastArguments(emulatedStackFrame, emulatedStackFrameCreate);
            (void) this.target.invoke(emulatedStackFrameCreate);
            explicitCastReturnValue(emulatedStackFrame, emulatedStackFrameCreate);
        }

        private void explicitCastArguments(EmulatedStackFrame emulatedStackFrame, EmulatedStackFrame emulatedStackFrame2) {
            EmulatedStackFrame.StackFrameReader stackFrameReader = new EmulatedStackFrame.StackFrameReader();
            stackFrameReader.attach(emulatedStackFrame);
            EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
            stackFrameWriter.attach(emulatedStackFrame2);
            Class<?>[] clsArrPtypes = type().ptypes();
            Class<?>[] clsArrPtypes2 = this.target.type().ptypes();
            for (int i = 0; i < clsArrPtypes.length; i++) {
                explicitCast(stackFrameReader, clsArrPtypes[i], stackFrameWriter, clsArrPtypes2[i]);
            }
        }

        private void explicitCastReturnValue(EmulatedStackFrame emulatedStackFrame, EmulatedStackFrame emulatedStackFrame2) {
            Class<?> clsRtype = this.target.type().rtype();
            Class<?> clsRtype2 = type().rtype();
            if (clsRtype2 != Void.TYPE) {
                EmulatedStackFrame.StackFrameWriter stackFrameWriter = new EmulatedStackFrame.StackFrameWriter();
                stackFrameWriter.attach(emulatedStackFrame);
                stackFrameWriter.makeReturnValueAccessor();
                if (clsRtype == Void.TYPE) {
                    if (clsRtype2.isPrimitive()) {
                        unboxNull(stackFrameWriter, clsRtype2);
                        return;
                    } else {
                        stackFrameWriter.putNextReference((Object) null, clsRtype2);
                        return;
                    }
                }
                EmulatedStackFrame.StackFrameReader stackFrameReader = new EmulatedStackFrame.StackFrameReader();
                stackFrameReader.attach(emulatedStackFrame2);
                stackFrameReader.makeReturnValueAccessor();
                explicitCast(stackFrameReader, this.target.type().rtype(), stackFrameWriter, type().rtype());
            }
        }

        private static void throwUnexpectedType(Class<?> cls) {
            throw new InternalError("Unexpected type: " + ((Object) cls));
        }

        private static void explicitCastFromBoolean(boolean z, EmulatedStackFrame.StackFrameWriter stackFrameWriter, Class<?> cls) {
            if (cls == Byte.TYPE) {
                stackFrameWriter.putNextByte(z ? (byte) 1 : (byte) 0);
                return;
            }
            if (cls == Character.TYPE) {
                stackFrameWriter.putNextChar(z ? (char) 1 : (char) 0);
                return;
            }
            if (cls == Short.TYPE) {
                stackFrameWriter.putNextShort(z ? (short) 1 : (short) 0);
                return;
            }
            if (cls == Integer.TYPE) {
                stackFrameWriter.putNextInt(z ? 1 : 0);
                return;
            }
            if (cls == Long.TYPE) {
                stackFrameWriter.putNextLong(z ? 1L : 0L);
                return;
            }
            if (cls == Float.TYPE) {
                stackFrameWriter.putNextFloat(z ? 1.0f : 0.0f);
            } else if (cls == Double.TYPE) {
                stackFrameWriter.putNextDouble(z ? 1.0d : 0.0d);
            } else {
                throwUnexpectedType(cls);
            }
        }

        private static boolean toBoolean(byte b) {
            return (b & 1) == 1;
        }

        private static byte readPrimitiveAsByte(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?> cls) {
            if (cls == Byte.TYPE) {
                return stackFrameReader.nextByte();
            }
            if (cls == Character.TYPE) {
                return (byte) stackFrameReader.nextChar();
            }
            if (cls == Short.TYPE) {
                return (byte) stackFrameReader.nextShort();
            }
            if (cls == Integer.TYPE) {
                return (byte) stackFrameReader.nextInt();
            }
            if (cls == Long.TYPE) {
                return (byte) stackFrameReader.nextLong();
            }
            if (cls == Float.TYPE) {
                return (byte) stackFrameReader.nextFloat();
            }
            if (cls == Double.TYPE) {
                return (byte) stackFrameReader.nextDouble();
            }
            throwUnexpectedType(cls);
            return (byte) 0;
        }

        private static char readPrimitiveAsChar(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?> cls) {
            if (cls == Byte.TYPE) {
                return (char) stackFrameReader.nextByte();
            }
            if (cls == Character.TYPE) {
                return stackFrameReader.nextChar();
            }
            if (cls == Short.TYPE) {
                return (char) stackFrameReader.nextShort();
            }
            if (cls == Integer.TYPE) {
                return (char) stackFrameReader.nextInt();
            }
            if (cls == Long.TYPE) {
                return (char) stackFrameReader.nextLong();
            }
            if (cls == Float.TYPE) {
                return (char) stackFrameReader.nextFloat();
            }
            if (cls == Double.TYPE) {
                return (char) stackFrameReader.nextDouble();
            }
            throwUnexpectedType(cls);
            return (char) 0;
        }

        private static short readPrimitiveAsShort(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?> cls) {
            if (cls == Byte.TYPE) {
                return stackFrameReader.nextByte();
            }
            if (cls == Character.TYPE) {
                return (short) stackFrameReader.nextChar();
            }
            if (cls == Short.TYPE) {
                return stackFrameReader.nextShort();
            }
            if (cls == Integer.TYPE) {
                return (short) stackFrameReader.nextInt();
            }
            if (cls == Long.TYPE) {
                return (short) stackFrameReader.nextLong();
            }
            if (cls == Float.TYPE) {
                return (short) stackFrameReader.nextFloat();
            }
            if (cls == Double.TYPE) {
                return (short) stackFrameReader.nextDouble();
            }
            throwUnexpectedType(cls);
            return (short) 0;
        }

        private static int readPrimitiveAsInt(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?> cls) {
            if (cls == Byte.TYPE) {
                return stackFrameReader.nextByte();
            }
            if (cls == Character.TYPE) {
                return stackFrameReader.nextChar();
            }
            if (cls == Short.TYPE) {
                return stackFrameReader.nextShort();
            }
            if (cls == Integer.TYPE) {
                return stackFrameReader.nextInt();
            }
            if (cls == Long.TYPE) {
                return (int) stackFrameReader.nextLong();
            }
            if (cls == Float.TYPE) {
                return (int) stackFrameReader.nextFloat();
            }
            if (cls == Double.TYPE) {
                return (int) stackFrameReader.nextDouble();
            }
            throwUnexpectedType(cls);
            return 0;
        }

        private static long readPrimitiveAsLong(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?> cls) {
            if (cls == Byte.TYPE) {
                return stackFrameReader.nextByte();
            }
            if (cls == Character.TYPE) {
                return stackFrameReader.nextChar();
            }
            if (cls == Short.TYPE) {
                return stackFrameReader.nextShort();
            }
            if (cls == Integer.TYPE) {
                return stackFrameReader.nextInt();
            }
            if (cls == Long.TYPE) {
                return stackFrameReader.nextLong();
            }
            if (cls == Float.TYPE) {
                return (long) stackFrameReader.nextFloat();
            }
            if (cls == Double.TYPE) {
                return (long) stackFrameReader.nextDouble();
            }
            throwUnexpectedType(cls);
            return 0L;
        }

        private static float readPrimitiveAsFloat(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?> cls) {
            if (cls == Byte.TYPE) {
                return stackFrameReader.nextByte();
            }
            if (cls == Character.TYPE) {
                return stackFrameReader.nextChar();
            }
            if (cls == Short.TYPE) {
                return stackFrameReader.nextShort();
            }
            if (cls == Integer.TYPE) {
                return stackFrameReader.nextInt();
            }
            if (cls == Long.TYPE) {
                return stackFrameReader.nextLong();
            }
            if (cls == Float.TYPE) {
                return stackFrameReader.nextFloat();
            }
            if (cls == Double.TYPE) {
                return (float) stackFrameReader.nextDouble();
            }
            throwUnexpectedType(cls);
            return 0.0f;
        }

        private static double readPrimitiveAsDouble(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?> cls) {
            if (cls == Byte.TYPE) {
                return stackFrameReader.nextByte();
            }
            if (cls == Character.TYPE) {
                return stackFrameReader.nextChar();
            }
            if (cls == Short.TYPE) {
                return stackFrameReader.nextShort();
            }
            if (cls == Integer.TYPE) {
                return stackFrameReader.nextInt();
            }
            if (cls == Long.TYPE) {
                return stackFrameReader.nextLong();
            }
            if (cls == Float.TYPE) {
                return stackFrameReader.nextFloat();
            }
            if (cls == Double.TYPE) {
                return stackFrameReader.nextDouble();
            }
            throwUnexpectedType(cls);
            return 0.0d;
        }

        private static void explicitCastToBoolean(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?> cls, EmulatedStackFrame.StackFrameWriter stackFrameWriter) {
            stackFrameWriter.putNextBoolean(toBoolean(readPrimitiveAsByte(stackFrameReader, cls)));
        }

        private static void explicitCastPrimitives(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?> cls, EmulatedStackFrame.StackFrameWriter stackFrameWriter, Class<?> cls2) {
            if (cls2 == Byte.TYPE) {
                stackFrameWriter.putNextByte(readPrimitiveAsByte(stackFrameReader, cls));
                return;
            }
            if (cls2 == Character.TYPE) {
                stackFrameWriter.putNextChar(readPrimitiveAsChar(stackFrameReader, cls));
                return;
            }
            if (cls2 == Short.TYPE) {
                stackFrameWriter.putNextShort(readPrimitiveAsShort(stackFrameReader, cls));
                return;
            }
            if (cls2 == Integer.TYPE) {
                stackFrameWriter.putNextInt(readPrimitiveAsInt(stackFrameReader, cls));
                return;
            }
            if (cls2 == Long.TYPE) {
                stackFrameWriter.putNextLong(readPrimitiveAsLong(stackFrameReader, cls));
                return;
            }
            if (cls2 == Float.TYPE) {
                stackFrameWriter.putNextFloat(readPrimitiveAsFloat(stackFrameReader, cls));
            } else if (cls2 == Double.TYPE) {
                stackFrameWriter.putNextDouble(readPrimitiveAsDouble(stackFrameReader, cls));
            } else {
                throwUnexpectedType(cls2);
            }
        }

        private static void unboxNull(EmulatedStackFrame.StackFrameWriter stackFrameWriter, Class<?> cls) {
            if (cls == Boolean.TYPE) {
                stackFrameWriter.putNextBoolean(false);
                return;
            }
            if (cls == Byte.TYPE) {
                stackFrameWriter.putNextByte((byte) 0);
                return;
            }
            if (cls == Character.TYPE) {
                stackFrameWriter.putNextChar((char) 0);
                return;
            }
            if (cls == Short.TYPE) {
                stackFrameWriter.putNextShort((short) 0);
                return;
            }
            if (cls == Integer.TYPE) {
                stackFrameWriter.putNextInt(0);
                return;
            }
            if (cls == Long.TYPE) {
                stackFrameWriter.putNextLong(0L);
                return;
            }
            if (cls == Float.TYPE) {
                stackFrameWriter.putNextFloat(0.0f);
            } else if (cls == Double.TYPE) {
                stackFrameWriter.putNextDouble(0.0d);
            } else {
                throwUnexpectedType(cls);
            }
        }

        private static void unboxNonNull(Object obj, Class<?> cls, EmulatedStackFrame.StackFrameWriter stackFrameWriter, Class<?> cls2) {
            if (cls2 == Boolean.TYPE) {
                if (cls == Boolean.class) {
                    stackFrameWriter.putNextBoolean(((Boolean) obj).booleanValue());
                    return;
                } else if (cls == Float.class || cls == Double.class) {
                    stackFrameWriter.putNextBoolean(toBoolean((byte) ((Double) obj).doubleValue()));
                    return;
                } else {
                    stackFrameWriter.putNextBoolean(toBoolean((byte) ((Long) obj).longValue()));
                    return;
                }
            }
            if (cls2 == Byte.TYPE) {
                stackFrameWriter.putNextByte(((Byte) obj).byteValue());
                return;
            }
            if (cls2 == Character.TYPE) {
                stackFrameWriter.putNextChar(((Character) obj).charValue());
                return;
            }
            if (cls2 == Short.TYPE) {
                stackFrameWriter.putNextShort(((Short) obj).shortValue());
                return;
            }
            if (cls2 == Integer.TYPE) {
                stackFrameWriter.putNextInt(((Integer) obj).intValue());
                return;
            }
            if (cls2 == Long.TYPE) {
                stackFrameWriter.putNextLong(((Long) obj).longValue());
                return;
            }
            if (cls2 == Float.TYPE) {
                stackFrameWriter.putNextFloat(((Float) obj).floatValue());
            } else if (cls2 == Double.TYPE) {
                stackFrameWriter.putNextDouble(((Double) obj).doubleValue());
            } else {
                throwUnexpectedType(cls2);
            }
        }

        private static void unbox(Object obj, Class<?> cls, EmulatedStackFrame.StackFrameWriter stackFrameWriter, Class<?> cls2) {
            if (obj == null) {
                unboxNull(stackFrameWriter, cls2);
            } else {
                unboxNonNull(obj, cls, stackFrameWriter, cls2);
            }
        }

        private static void box(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?> cls, EmulatedStackFrame.StackFrameWriter stackFrameWriter, Class<?> cls2) {
            Object objValueOf;
            if (cls == Boolean.TYPE) {
                objValueOf = Boolean.valueOf(stackFrameReader.nextBoolean());
            } else if (cls == Byte.TYPE) {
                objValueOf = Byte.valueOf(stackFrameReader.nextByte());
            } else if (cls == Character.TYPE) {
                objValueOf = Character.valueOf(stackFrameReader.nextChar());
            } else if (cls == Short.TYPE) {
                objValueOf = Short.valueOf(stackFrameReader.nextShort());
            } else if (cls == Integer.TYPE) {
                objValueOf = Integer.valueOf(stackFrameReader.nextInt());
            } else if (cls == Long.TYPE) {
                objValueOf = Long.valueOf(stackFrameReader.nextLong());
            } else if (cls == Float.TYPE) {
                objValueOf = Float.valueOf(stackFrameReader.nextFloat());
            } else if (cls == Double.TYPE) {
                objValueOf = Double.valueOf(stackFrameReader.nextDouble());
            } else {
                throwUnexpectedType(cls);
                objValueOf = null;
            }
            stackFrameWriter.putNextReference(cls2.cast(objValueOf), cls2);
        }

        private static void explicitCast(EmulatedStackFrame.StackFrameReader stackFrameReader, Class<?> cls, EmulatedStackFrame.StackFrameWriter stackFrameWriter, Class<?> cls2) {
            if (cls.equals(cls2)) {
                EmulatedStackFrame.StackFrameAccessor.copyNext(stackFrameReader, stackFrameWriter, cls);
                return;
            }
            if (!cls.isPrimitive()) {
                Object objNextReference = stackFrameReader.nextReference(cls);
                if (cls2.isInterface()) {
                    stackFrameWriter.putNextReference(objNextReference, cls2);
                    return;
                } else if (!cls2.isPrimitive()) {
                    stackFrameWriter.putNextReference(cls2.cast(objNextReference), cls2);
                    return;
                } else {
                    unbox(objNextReference, cls, stackFrameWriter, cls2);
                    return;
                }
            }
            if (cls2.isPrimitive()) {
                if (cls == Boolean.TYPE) {
                    explicitCastFromBoolean(stackFrameReader.nextBoolean(), stackFrameWriter, cls2);
                    return;
                } else if (cls2 == Boolean.TYPE) {
                    explicitCastToBoolean(stackFrameReader, cls, stackFrameWriter);
                    return;
                } else {
                    explicitCastPrimitives(stackFrameReader, cls, stackFrameWriter, cls2);
                    return;
                }
            }
            box(stackFrameReader, cls, stackFrameWriter, cls2);
        }
    }
}
