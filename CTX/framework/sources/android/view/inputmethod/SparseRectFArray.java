package android.view.inputmethod;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public final class SparseRectFArray implements Parcelable {
    public static final Parcelable.Creator<SparseRectFArray> CREATOR = new Parcelable.Creator<SparseRectFArray>() {
        @Override
        public SparseRectFArray createFromParcel(Parcel parcel) {
            return new SparseRectFArray(parcel);
        }

        @Override
        public SparseRectFArray[] newArray(int i) {
            return new SparseRectFArray[i];
        }
    };
    private final float[] mCoordinates;
    private final int[] mFlagsArray;
    private final int[] mKeys;

    public SparseRectFArray(Parcel parcel) {
        this.mKeys = parcel.createIntArray();
        this.mCoordinates = parcel.createFloatArray();
        this.mFlagsArray = parcel.createIntArray();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeIntArray(this.mKeys);
        parcel.writeFloatArray(this.mCoordinates);
        parcel.writeIntArray(this.mFlagsArray);
    }

    public int hashCode() {
        if (this.mKeys == null || this.mKeys.length == 0) {
            return 0;
        }
        int length = this.mKeys.length;
        for (int i = 0; i < 4; i++) {
            length = (int) ((length * 31) + this.mCoordinates[i]);
        }
        return (length * 31) + this.mFlagsArray[0];
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SparseRectFArray)) {
            return false;
        }
        SparseRectFArray sparseRectFArray = (SparseRectFArray) obj;
        if (!Arrays.equals(this.mKeys, sparseRectFArray.mKeys) || !Arrays.equals(this.mCoordinates, sparseRectFArray.mCoordinates) || !Arrays.equals(this.mFlagsArray, sparseRectFArray.mFlagsArray)) {
            return false;
        }
        return true;
    }

    public String toString() {
        if (this.mKeys == null || this.mCoordinates == null || this.mFlagsArray == null) {
            return "SparseRectFArray{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("SparseRectFArray{");
        for (int i = 0; i < this.mKeys.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            int i2 = i * 4;
            sb.append(this.mKeys[i]);
            sb.append(":[");
            sb.append(this.mCoordinates[i2 + 0]);
            sb.append(",");
            sb.append(this.mCoordinates[i2 + 1]);
            sb.append("],[");
            sb.append(this.mCoordinates[i2 + 2]);
            sb.append(",");
            sb.append(this.mCoordinates[i2 + 3]);
            sb.append("]:flagsArray=");
            sb.append(this.mFlagsArray[i]);
        }
        sb.append("}");
        return sb.toString();
    }

    public static final class SparseRectFArrayBuilder {
        private static int INITIAL_SIZE = 16;
        private int mCount = 0;
        private int[] mKeys = null;
        private float[] mCoordinates = null;
        private int[] mFlagsArray = null;

        private void checkIndex(int i) {
            if (this.mCount != 0 && this.mKeys[this.mCount - 1] >= i) {
                throw new IllegalArgumentException("key must be greater than all existing keys.");
            }
        }

        private void ensureBufferSize() {
            if (this.mKeys == null) {
                this.mKeys = new int[INITIAL_SIZE];
            }
            if (this.mCoordinates == null) {
                this.mCoordinates = new float[INITIAL_SIZE * 4];
            }
            if (this.mFlagsArray == null) {
                this.mFlagsArray = new int[INITIAL_SIZE];
            }
            int i = this.mCount + 1;
            if (this.mKeys.length <= i) {
                int[] iArr = new int[i * 2];
                System.arraycopy(this.mKeys, 0, iArr, 0, this.mCount);
                this.mKeys = iArr;
            }
            int i2 = (this.mCount + 1) * 4;
            if (this.mCoordinates.length <= i2) {
                float[] fArr = new float[i2 * 2];
                System.arraycopy(this.mCoordinates, 0, fArr, 0, this.mCount * 4);
                this.mCoordinates = fArr;
            }
            if (this.mFlagsArray.length <= i) {
                int[] iArr2 = new int[i * 2];
                System.arraycopy(this.mFlagsArray, 0, iArr2, 0, this.mCount);
                this.mFlagsArray = iArr2;
            }
        }

        public SparseRectFArrayBuilder append(int i, float f, float f2, float f3, float f4, int i2) {
            checkIndex(i);
            ensureBufferSize();
            int i3 = this.mCount * 4;
            this.mCoordinates[i3 + 0] = f;
            this.mCoordinates[i3 + 1] = f2;
            this.mCoordinates[i3 + 2] = f3;
            this.mCoordinates[i3 + 3] = f4;
            this.mFlagsArray[this.mCount] = i2;
            this.mKeys[this.mCount] = i;
            this.mCount++;
            return this;
        }

        public boolean isEmpty() {
            return this.mCount <= 0;
        }

        public SparseRectFArray build() {
            return new SparseRectFArray(this);
        }

        public void reset() {
            if (this.mCount == 0) {
                this.mKeys = null;
                this.mCoordinates = null;
                this.mFlagsArray = null;
            }
            this.mCount = 0;
        }
    }

    private SparseRectFArray(SparseRectFArrayBuilder sparseRectFArrayBuilder) {
        if (sparseRectFArrayBuilder.mCount != 0) {
            this.mKeys = new int[sparseRectFArrayBuilder.mCount];
            this.mCoordinates = new float[sparseRectFArrayBuilder.mCount * 4];
            this.mFlagsArray = new int[sparseRectFArrayBuilder.mCount];
            System.arraycopy(sparseRectFArrayBuilder.mKeys, 0, this.mKeys, 0, sparseRectFArrayBuilder.mCount);
            System.arraycopy(sparseRectFArrayBuilder.mCoordinates, 0, this.mCoordinates, 0, sparseRectFArrayBuilder.mCount * 4);
            System.arraycopy(sparseRectFArrayBuilder.mFlagsArray, 0, this.mFlagsArray, 0, sparseRectFArrayBuilder.mCount);
            return;
        }
        this.mKeys = null;
        this.mCoordinates = null;
        this.mFlagsArray = null;
    }

    public RectF get(int i) {
        int iBinarySearch;
        if (this.mKeys == null || i < 0 || (iBinarySearch = Arrays.binarySearch(this.mKeys, i)) < 0) {
            return null;
        }
        int i2 = iBinarySearch * 4;
        return new RectF(this.mCoordinates[i2], this.mCoordinates[i2 + 1], this.mCoordinates[i2 + 2], this.mCoordinates[i2 + 3]);
    }

    public int getFlags(int i, int i2) {
        int iBinarySearch;
        if (this.mKeys == null || i < 0 || (iBinarySearch = Arrays.binarySearch(this.mKeys, i)) < 0) {
            return i2;
        }
        return this.mFlagsArray[iBinarySearch];
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
