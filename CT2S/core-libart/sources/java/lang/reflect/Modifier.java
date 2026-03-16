package java.lang.reflect;

import dalvik.bytecode.Opcodes;

public class Modifier {
    public static final int ABSTRACT = 1024;
    static final int ANNOTATION = 8192;
    static final int BRIDGE = 64;
    public static final int CONSTRUCTOR = 65536;
    static final int ENUM = 16384;
    public static final int FINAL = 16;
    public static final int INTERFACE = 512;
    public static final int MIRANDA = 2097152;
    public static final int NATIVE = 256;
    public static final int PRIVATE = 2;
    public static final int PROTECTED = 4;
    public static final int PUBLIC = 1;
    public static final int STATIC = 8;
    public static final int STRICT = 2048;
    public static final int SYNCHRONIZED = 32;
    public static final int SYNTHETIC = 4096;
    public static final int TRANSIENT = 128;
    static final int VARARGS = 128;
    public static final int VOLATILE = 64;

    public static int classModifiers() {
        return 3103;
    }

    public static int constructorModifiers() {
        return 7;
    }

    public static int fieldModifiers() {
        return Opcodes.OP_XOR_INT_LIT8;
    }

    public static int interfaceModifiers() {
        return 3087;
    }

    public static int methodModifiers() {
        return 3391;
    }

    public static boolean isAbstract(int modifiers) {
        return (modifiers & 1024) != 0;
    }

    public static boolean isFinal(int modifiers) {
        return (modifiers & 16) != 0;
    }

    public static boolean isInterface(int modifiers) {
        return (modifiers & 512) != 0;
    }

    public static boolean isNative(int modifiers) {
        return (modifiers & 256) != 0;
    }

    public static boolean isPrivate(int modifiers) {
        return (modifiers & 2) != 0;
    }

    public static boolean isProtected(int modifiers) {
        return (modifiers & 4) != 0;
    }

    public static boolean isPublic(int modifiers) {
        return (modifiers & 1) != 0;
    }

    public static boolean isStatic(int modifiers) {
        return (modifiers & 8) != 0;
    }

    public static boolean isStrict(int modifiers) {
        return (modifiers & 2048) != 0;
    }

    public static boolean isSynchronized(int modifiers) {
        return (modifiers & 32) != 0;
    }

    public static boolean isTransient(int modifiers) {
        return (modifiers & 128) != 0;
    }

    public static boolean isVolatile(int modifiers) {
        return (modifiers & 64) != 0;
    }

    public static boolean isConstructor(int modifiers) {
        return (65536 & modifiers) != 0;
    }

    public static String toString(int modifiers) {
        StringBuilder buf = new StringBuilder();
        if (isPublic(modifiers)) {
            buf.append("public ");
        }
        if (isProtected(modifiers)) {
            buf.append("protected ");
        }
        if (isPrivate(modifiers)) {
            buf.append("private ");
        }
        if (isAbstract(modifiers)) {
            buf.append("abstract ");
        }
        if (isStatic(modifiers)) {
            buf.append("static ");
        }
        if (isFinal(modifiers)) {
            buf.append("final ");
        }
        if (isTransient(modifiers)) {
            buf.append("transient ");
        }
        if (isVolatile(modifiers)) {
            buf.append("volatile ");
        }
        if (isSynchronized(modifiers)) {
            buf.append("synchronized ");
        }
        if (isNative(modifiers)) {
            buf.append("native ");
        }
        if (isStrict(modifiers)) {
            buf.append("strictfp ");
        }
        if (isInterface(modifiers)) {
            buf.append("interface ");
        }
        if (buf.length() == 0) {
            return "";
        }
        buf.setLength(buf.length() - 1);
        return buf.toString();
    }
}
