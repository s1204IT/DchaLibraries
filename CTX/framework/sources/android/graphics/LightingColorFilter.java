package android.graphics;

public class LightingColorFilter extends ColorFilter {
    private int mAdd;
    private int mMul;

    private static native long native_CreateLightingFilter(int i, int i2);

    public LightingColorFilter(int i, int i2) {
        this.mMul = i;
        this.mAdd = i2;
    }

    public int getColorMultiply() {
        return this.mMul;
    }

    public void setColorMultiply(int i) {
        if (this.mMul != i) {
            this.mMul = i;
            discardNativeInstance();
        }
    }

    public int getColorAdd() {
        return this.mAdd;
    }

    public void setColorAdd(int i) {
        if (this.mAdd != i) {
            this.mAdd = i;
            discardNativeInstance();
        }
    }

    @Override
    long createNativeInstance() {
        return native_CreateLightingFilter(this.mMul, this.mAdd);
    }
}
