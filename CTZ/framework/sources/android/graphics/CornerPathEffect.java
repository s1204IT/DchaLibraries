package android.graphics;

public class CornerPathEffect extends PathEffect {
    private static native long nativeCreate(float f);

    public CornerPathEffect(float f) {
        this.native_instance = nativeCreate(f);
    }
}
