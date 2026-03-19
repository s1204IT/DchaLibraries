package android.graphics;

public class ColorMatrixColorFilter extends ColorFilter {
    private final ColorMatrix mMatrix = new ColorMatrix();

    private static native long nativeColorMatrixFilter(float[] fArr);

    public ColorMatrixColorFilter(ColorMatrix colorMatrix) {
        this.mMatrix.set(colorMatrix);
    }

    public ColorMatrixColorFilter(float[] fArr) {
        if (fArr.length < 20) {
            throw new ArrayIndexOutOfBoundsException();
        }
        this.mMatrix.set(fArr);
    }

    public void getColorMatrix(ColorMatrix colorMatrix) {
        colorMatrix.set(this.mMatrix);
    }

    public void setColorMatrix(ColorMatrix colorMatrix) {
        discardNativeInstance();
        if (colorMatrix == null) {
            this.mMatrix.reset();
        } else {
            this.mMatrix.set(colorMatrix);
        }
    }

    public void setColorMatrixArray(float[] fArr) {
        discardNativeInstance();
        if (fArr == null) {
            this.mMatrix.reset();
        } else {
            if (fArr.length < 20) {
                throw new ArrayIndexOutOfBoundsException();
            }
            this.mMatrix.set(fArr);
        }
    }

    @Override
    long createNativeInstance() {
        return nativeColorMatrixFilter(this.mMatrix.getArray());
    }
}
