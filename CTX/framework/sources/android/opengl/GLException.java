package android.opengl;

public class GLException extends RuntimeException {
    private final int mError;

    public GLException(int i) {
        super(getErrorString(i));
        this.mError = i;
    }

    public GLException(int i, String str) {
        super(str);
        this.mError = i;
    }

    private static String getErrorString(int i) {
        String strGluErrorString = GLU.gluErrorString(i);
        if (strGluErrorString == null) {
            return "Unknown error 0x" + Integer.toHexString(i);
        }
        return strGluErrorString;
    }

    int getError() {
        return this.mError;
    }
}
