package android.graphics;

@Deprecated
public class AvoidXfermode extends Xfermode {
    private static native long nativeCreate(int i, int i2, int i3);

    public enum Mode {
        AVOID(0),
        TARGET(1);

        final int nativeInt;

        Mode(int nativeInt) {
            this.nativeInt = nativeInt;
        }
    }

    public AvoidXfermode(int opColor, int tolerance, Mode mode) {
        if (tolerance < 0 || tolerance > 255) {
            throw new IllegalArgumentException("tolerance must be 0..255");
        }
        this.native_instance = nativeCreate(opColor, tolerance, mode.nativeInt);
    }
}
