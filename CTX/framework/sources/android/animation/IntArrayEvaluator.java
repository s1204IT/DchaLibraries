package android.animation;

public class IntArrayEvaluator implements TypeEvaluator<int[]> {
    private int[] mArray;

    public IntArrayEvaluator() {
    }

    public IntArrayEvaluator(int[] iArr) {
        this.mArray = iArr;
    }

    @Override
    public int[] evaluate(float f, int[] iArr, int[] iArr2) {
        int[] iArr3 = this.mArray;
        if (iArr3 == null) {
            iArr3 = new int[iArr.length];
        }
        for (int i = 0; i < iArr3.length; i++) {
            iArr3[i] = (int) (iArr[i] + ((iArr2[i] - r2) * f));
        }
        return iArr3;
    }
}
