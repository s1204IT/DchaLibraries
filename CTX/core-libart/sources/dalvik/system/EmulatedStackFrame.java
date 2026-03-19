package dalvik.system;

import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class EmulatedStackFrame {
    private final MethodType callsiteType;
    private final Object[] references;
    private final byte[] stackFrame;
    private final MethodType type;

    private EmulatedStackFrame(MethodType methodType, MethodType methodType2, Object[] objArr, byte[] bArr) {
        this.type = methodType;
        this.callsiteType = methodType2;
        this.references = objArr;
        this.stackFrame = bArr;
    }

    public final MethodType getMethodType() {
        return this.type;
    }

    public final MethodType getCallsiteType() {
        return this.callsiteType;
    }

    public static final class Range {
        public final int numBytes;
        public final int numReferences;
        public final int referencesStart;
        public final int stackFrameStart;

        private Range(int i, int i2, int i3, int i4) {
            this.referencesStart = i;
            this.numReferences = i2;
            this.stackFrameStart = i3;
            this.numBytes = i4;
        }

        public static Range all(MethodType methodType) {
            return of(methodType, 0, methodType.parameterCount());
        }

        public static Range of(MethodType methodType, int i, int i2) {
            Class[] clsArrPtypes = methodType.ptypes();
            int i3 = 0;
            int i4 = 0;
            int size = 0;
            for (int i5 = 0; i5 < i; i5++) {
                Class cls = clsArrPtypes[i5];
                if (!cls.isPrimitive()) {
                    i4++;
                } else {
                    size += EmulatedStackFrame.getSize(cls);
                }
            }
            int size2 = 0;
            while (i < i2) {
                Class cls2 = clsArrPtypes[i];
                if (!cls2.isPrimitive()) {
                    i3++;
                } else {
                    size2 += EmulatedStackFrame.getSize(cls2);
                }
                i++;
            }
            return new Range(i4, i3, size, size2);
        }
    }

    public static EmulatedStackFrame create(MethodType methodType) {
        int i = 0;
        int size = 0;
        for (Class cls : methodType.ptypes()) {
            if (!cls.isPrimitive()) {
                i++;
            } else {
                size += getSize(cls);
            }
        }
        Class clsRtype = methodType.rtype();
        if (!clsRtype.isPrimitive()) {
            i++;
        } else {
            size += getSize(clsRtype);
        }
        return new EmulatedStackFrame(methodType, methodType, new Object[i], new byte[size]);
    }

    public void setReference(int i, Object obj) {
        Class[] clsArrPtypes = this.type.ptypes();
        if (i < 0 || i >= clsArrPtypes.length) {
            throw new IllegalArgumentException("Invalid index: " + i);
        }
        if (obj != null && !clsArrPtypes[i].isInstance(obj)) {
            throw new IllegalStateException("reference is not of type: " + this.type.ptypes()[i]);
        }
        this.references[i] = obj;
    }

    public <T> T getReference(int i, Class<T> cls) {
        if (cls != this.type.ptypes()[i]) {
            throw new IllegalArgumentException("Argument: " + i + " is of type " + this.type.ptypes()[i] + " expected " + cls + "");
        }
        return (T) this.references[i];
    }

    public void copyRangeTo(EmulatedStackFrame emulatedStackFrame, Range range, int i, int i2) {
        if (range.numReferences > 0) {
            System.arraycopy(this.references, range.referencesStart, emulatedStackFrame.references, i, range.numReferences);
        }
        if (range.numBytes > 0) {
            System.arraycopy(this.stackFrame, range.stackFrameStart, emulatedStackFrame.stackFrame, i2, range.numBytes);
        }
    }

    public void copyReturnValueTo(EmulatedStackFrame emulatedStackFrame) {
        Class<?> clsReturnType = this.type.returnType();
        if (!clsReturnType.isPrimitive()) {
            emulatedStackFrame.references[emulatedStackFrame.references.length - 1] = this.references[this.references.length - 1];
        } else if (!is64BitPrimitive(clsReturnType)) {
            System.arraycopy(this.stackFrame, this.stackFrame.length - 4, emulatedStackFrame.stackFrame, emulatedStackFrame.stackFrame.length - 4, 4);
        } else {
            System.arraycopy(this.stackFrame, this.stackFrame.length - 8, emulatedStackFrame.stackFrame, emulatedStackFrame.stackFrame.length - 8, 8);
        }
    }

    public void setReturnValueTo(Object obj) {
        Class<?> clsReturnType = this.type.returnType();
        if (clsReturnType.isPrimitive()) {
            throw new IllegalStateException("return type is not a reference type: " + clsReturnType);
        }
        if (obj != null && !clsReturnType.isInstance(obj)) {
            throw new IllegalArgumentException("reference is not of type " + clsReturnType);
        }
        this.references[this.references.length - 1] = obj;
    }

    private static boolean is64BitPrimitive(Class<?> cls) {
        return cls == Double.TYPE || cls == Long.TYPE;
    }

    public static int getSize(Class<?> cls) {
        if (!cls.isPrimitive()) {
            throw new IllegalArgumentException("type.isPrimitive() == false: " + cls);
        }
        if (is64BitPrimitive(cls)) {
            return 8;
        }
        return 4;
    }

    public static class StackFrameAccessor {
        private static final int RETURN_VALUE_IDX = -2;
        protected EmulatedStackFrame frame;
        protected int referencesOffset = 0;
        protected int argumentIdx = 0;
        protected ByteBuffer frameBuf = null;
        private int numArgs = 0;

        protected StackFrameAccessor() {
        }

        public StackFrameAccessor attach(EmulatedStackFrame emulatedStackFrame) {
            return attach(emulatedStackFrame, 0, 0, 0);
        }

        public StackFrameAccessor attach(EmulatedStackFrame emulatedStackFrame, int i, int i2, int i3) {
            this.frame = emulatedStackFrame;
            this.frameBuf = ByteBuffer.wrap(this.frame.stackFrame).order(ByteOrder.LITTLE_ENDIAN);
            this.numArgs = this.frame.type.ptypes().length;
            if (i3 != 0) {
                this.frameBuf.position(i3);
            }
            this.referencesOffset = i2;
            this.argumentIdx = i;
            return this;
        }

        protected void checkType(Class<?> cls) {
            if (this.argumentIdx >= this.numArgs || this.argumentIdx == -1) {
                throw new IllegalArgumentException("Invalid argument index: " + this.argumentIdx);
            }
            Class clsRtype = this.argumentIdx == -2 ? this.frame.type.rtype() : this.frame.type.ptypes()[this.argumentIdx];
            if (clsRtype != cls) {
                throw new IllegalArgumentException("Incorrect type: " + cls + ", expected: " + clsRtype);
            }
        }

        public void makeReturnValueAccessor() {
            Class clsRtype = this.frame.type.rtype();
            this.argumentIdx = -2;
            if (!clsRtype.isPrimitive()) {
                this.referencesOffset = this.frame.references.length - 1;
            } else {
                this.frameBuf.position(this.frameBuf.capacity() - EmulatedStackFrame.getSize(clsRtype));
            }
        }

        public static void copyNext(StackFrameReader stackFrameReader, StackFrameWriter stackFrameWriter, Class<?> cls) {
            if (!cls.isPrimitive()) {
                stackFrameWriter.putNextReference(stackFrameReader.nextReference(cls), cls);
                return;
            }
            if (cls == Boolean.TYPE) {
                stackFrameWriter.putNextBoolean(stackFrameReader.nextBoolean());
                return;
            }
            if (cls == Byte.TYPE) {
                stackFrameWriter.putNextByte(stackFrameReader.nextByte());
                return;
            }
            if (cls == Character.TYPE) {
                stackFrameWriter.putNextChar(stackFrameReader.nextChar());
                return;
            }
            if (cls == Short.TYPE) {
                stackFrameWriter.putNextShort(stackFrameReader.nextShort());
                return;
            }
            if (cls == Integer.TYPE) {
                stackFrameWriter.putNextInt(stackFrameReader.nextInt());
                return;
            }
            if (cls == Long.TYPE) {
                stackFrameWriter.putNextLong(stackFrameReader.nextLong());
            } else if (cls == Float.TYPE) {
                stackFrameWriter.putNextFloat(stackFrameReader.nextFloat());
            } else if (cls == Double.TYPE) {
                stackFrameWriter.putNextDouble(stackFrameReader.nextDouble());
            }
        }
    }

    public static class StackFrameWriter extends StackFrameAccessor {
        public void putNextByte(byte b) {
            checkType(Byte.TYPE);
            this.argumentIdx++;
            this.frameBuf.putInt(b);
        }

        public void putNextInt(int i) {
            checkType(Integer.TYPE);
            this.argumentIdx++;
            this.frameBuf.putInt(i);
        }

        public void putNextLong(long j) {
            checkType(Long.TYPE);
            this.argumentIdx++;
            this.frameBuf.putLong(j);
        }

        public void putNextChar(char c) {
            checkType(Character.TYPE);
            this.argumentIdx++;
            this.frameBuf.putInt(c);
        }

        public void putNextBoolean(boolean z) {
            checkType(Boolean.TYPE);
            this.argumentIdx++;
            this.frameBuf.putInt(z ? 1 : 0);
        }

        public void putNextShort(short s) {
            checkType(Short.TYPE);
            this.argumentIdx++;
            this.frameBuf.putInt(s);
        }

        public void putNextFloat(float f) {
            checkType(Float.TYPE);
            this.argumentIdx++;
            this.frameBuf.putFloat(f);
        }

        public void putNextDouble(double d) {
            checkType(Double.TYPE);
            this.argumentIdx++;
            this.frameBuf.putDouble(d);
        }

        public void putNextReference(Object obj, Class<?> cls) {
            checkType(cls);
            this.argumentIdx++;
            Object[] objArr = this.frame.references;
            int i = this.referencesOffset;
            this.referencesOffset = i + 1;
            objArr[i] = obj;
        }
    }

    public static class StackFrameReader extends StackFrameAccessor {
        public byte nextByte() {
            checkType(Byte.TYPE);
            this.argumentIdx++;
            return (byte) this.frameBuf.getInt();
        }

        public int nextInt() {
            checkType(Integer.TYPE);
            this.argumentIdx++;
            return this.frameBuf.getInt();
        }

        public long nextLong() {
            checkType(Long.TYPE);
            this.argumentIdx++;
            return this.frameBuf.getLong();
        }

        public char nextChar() {
            checkType(Character.TYPE);
            this.argumentIdx++;
            return (char) this.frameBuf.getInt();
        }

        public boolean nextBoolean() {
            checkType(Boolean.TYPE);
            this.argumentIdx++;
            return this.frameBuf.getInt() != 0;
        }

        public short nextShort() {
            checkType(Short.TYPE);
            this.argumentIdx++;
            return (short) this.frameBuf.getInt();
        }

        public float nextFloat() {
            checkType(Float.TYPE);
            this.argumentIdx++;
            return this.frameBuf.getFloat();
        }

        public double nextDouble() {
            checkType(Double.TYPE);
            this.argumentIdx++;
            return this.frameBuf.getDouble();
        }

        public <T> T nextReference(Class<T> cls) {
            checkType(cls);
            this.argumentIdx++;
            Object[] objArr = this.frame.references;
            int i = this.referencesOffset;
            this.referencesOffset = i + 1;
            return (T) objArr[i];
        }
    }
}
