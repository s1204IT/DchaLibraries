package java.lang.invoke;

import sun.misc.Unsafe;

class MethodHandleStatics {
    static final Unsafe UNSAFE = Unsafe.getUnsafe();

    private MethodHandleStatics() {
    }

    static InternalError newInternalError(String str) {
        return new InternalError(str);
    }

    static InternalError newInternalError(String str, Throwable th) {
        return new InternalError(str, th);
    }

    static InternalError newInternalError(Throwable th) {
        return new InternalError(th);
    }

    static RuntimeException newIllegalStateException(String str) {
        return new IllegalStateException(str);
    }

    static RuntimeException newIllegalStateException(String str, Object obj) {
        return new IllegalStateException(message(str, obj));
    }

    static RuntimeException newIllegalArgumentException(String str) {
        return new IllegalArgumentException(str);
    }

    static RuntimeException newIllegalArgumentException(String str, Object obj) {
        return new IllegalArgumentException(message(str, obj));
    }

    static RuntimeException newIllegalArgumentException(String str, Object obj, Object obj2) {
        return new IllegalArgumentException(message(str, obj, obj2));
    }

    static Error uncaughtException(Throwable th) {
        if (th instanceof Error) {
            throw ((Error) th);
        }
        if (th instanceof RuntimeException) {
            throw ((RuntimeException) th);
        }
        throw newInternalError("uncaught exception", th);
    }

    static Error NYI() {
        throw new AssertionError((Object) "NYI");
    }

    private static String message(String str, Object obj) {
        if (obj == null) {
            return str;
        }
        return str + ": " + obj;
    }

    private static String message(String str, Object obj, Object obj2) {
        if (obj == null && obj2 == null) {
            return str;
        }
        return str + ": " + obj + ", " + obj2;
    }
}
