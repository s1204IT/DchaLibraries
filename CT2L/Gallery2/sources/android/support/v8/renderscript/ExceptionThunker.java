package android.support.v8.renderscript;

class ExceptionThunker {
    ExceptionThunker() {
    }

    static RuntimeException convertException(RuntimeException e) {
        if (e instanceof android.renderscript.RSIllegalArgumentException) {
            return new RSIllegalArgumentException(e.getMessage());
        }
        if (e instanceof android.renderscript.RSInvalidStateException) {
            return new RSInvalidStateException(e.getMessage());
        }
        if (e instanceof android.renderscript.RSDriverException) {
            return new RSDriverException(e.getMessage());
        }
        if (e instanceof android.renderscript.RSRuntimeException) {
            return new RSRuntimeException(e.getMessage());
        }
        return e;
    }
}
