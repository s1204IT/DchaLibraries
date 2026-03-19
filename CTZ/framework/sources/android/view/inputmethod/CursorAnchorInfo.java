package android.view.inputmethod;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannedString;
import android.text.TextUtils;
import android.view.inputmethod.SparseRectFArray;
import java.util.Arrays;
import java.util.Objects;

public final class CursorAnchorInfo implements Parcelable {
    public static final Parcelable.Creator<CursorAnchorInfo> CREATOR = new Parcelable.Creator<CursorAnchorInfo>() {
        @Override
        public CursorAnchorInfo createFromParcel(Parcel parcel) {
            return new CursorAnchorInfo(parcel);
        }

        @Override
        public CursorAnchorInfo[] newArray(int i) {
            return new CursorAnchorInfo[i];
        }
    };
    public static final int FLAG_HAS_INVISIBLE_REGION = 2;
    public static final int FLAG_HAS_VISIBLE_REGION = 1;
    public static final int FLAG_IS_RTL = 4;
    private final SparseRectFArray mCharacterBoundsArray;
    private final CharSequence mComposingText;
    private final int mComposingTextStart;
    private final int mHashCode;
    private final float mInsertionMarkerBaseline;
    private final float mInsertionMarkerBottom;
    private final int mInsertionMarkerFlags;
    private final float mInsertionMarkerHorizontal;
    private final float mInsertionMarkerTop;
    private final float[] mMatrixValues;
    private final int mSelectionEnd;
    private final int mSelectionStart;

    public CursorAnchorInfo(Parcel parcel) {
        this.mHashCode = parcel.readInt();
        this.mSelectionStart = parcel.readInt();
        this.mSelectionEnd = parcel.readInt();
        this.mComposingTextStart = parcel.readInt();
        this.mComposingText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        this.mInsertionMarkerFlags = parcel.readInt();
        this.mInsertionMarkerHorizontal = parcel.readFloat();
        this.mInsertionMarkerTop = parcel.readFloat();
        this.mInsertionMarkerBaseline = parcel.readFloat();
        this.mInsertionMarkerBottom = parcel.readFloat();
        this.mCharacterBoundsArray = (SparseRectFArray) parcel.readParcelable(SparseRectFArray.class.getClassLoader());
        this.mMatrixValues = parcel.createFloatArray();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mHashCode);
        parcel.writeInt(this.mSelectionStart);
        parcel.writeInt(this.mSelectionEnd);
        parcel.writeInt(this.mComposingTextStart);
        TextUtils.writeToParcel(this.mComposingText, parcel, i);
        parcel.writeInt(this.mInsertionMarkerFlags);
        parcel.writeFloat(this.mInsertionMarkerHorizontal);
        parcel.writeFloat(this.mInsertionMarkerTop);
        parcel.writeFloat(this.mInsertionMarkerBaseline);
        parcel.writeFloat(this.mInsertionMarkerBottom);
        parcel.writeParcelable(this.mCharacterBoundsArray, i);
        parcel.writeFloatArray(this.mMatrixValues);
    }

    public int hashCode() {
        return this.mHashCode;
    }

    private static boolean areSameFloatImpl(float f, float f2) {
        return (Float.isNaN(f) && Float.isNaN(f2)) || f == f2;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CursorAnchorInfo)) {
            return false;
        }
        CursorAnchorInfo cursorAnchorInfo = (CursorAnchorInfo) obj;
        if (hashCode() != cursorAnchorInfo.hashCode() || this.mSelectionStart != cursorAnchorInfo.mSelectionStart || this.mSelectionEnd != cursorAnchorInfo.mSelectionEnd || this.mInsertionMarkerFlags != cursorAnchorInfo.mInsertionMarkerFlags || !areSameFloatImpl(this.mInsertionMarkerHorizontal, cursorAnchorInfo.mInsertionMarkerHorizontal) || !areSameFloatImpl(this.mInsertionMarkerTop, cursorAnchorInfo.mInsertionMarkerTop) || !areSameFloatImpl(this.mInsertionMarkerBaseline, cursorAnchorInfo.mInsertionMarkerBaseline) || !areSameFloatImpl(this.mInsertionMarkerBottom, cursorAnchorInfo.mInsertionMarkerBottom) || !Objects.equals(this.mCharacterBoundsArray, cursorAnchorInfo.mCharacterBoundsArray) || this.mComposingTextStart != cursorAnchorInfo.mComposingTextStart || !Objects.equals(this.mComposingText, cursorAnchorInfo.mComposingText) || this.mMatrixValues.length != cursorAnchorInfo.mMatrixValues.length) {
            return false;
        }
        for (int i = 0; i < this.mMatrixValues.length; i++) {
            if (this.mMatrixValues[i] != cursorAnchorInfo.mMatrixValues[i]) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "CursorAnchorInfo{mHashCode=" + this.mHashCode + " mSelection=" + this.mSelectionStart + "," + this.mSelectionEnd + " mComposingTextStart=" + this.mComposingTextStart + " mComposingText=" + Objects.toString(this.mComposingText) + " mInsertionMarkerFlags=" + this.mInsertionMarkerFlags + " mInsertionMarkerHorizontal=" + this.mInsertionMarkerHorizontal + " mInsertionMarkerTop=" + this.mInsertionMarkerTop + " mInsertionMarkerBaseline=" + this.mInsertionMarkerBaseline + " mInsertionMarkerBottom=" + this.mInsertionMarkerBottom + " mCharacterBoundsArray=" + Objects.toString(this.mCharacterBoundsArray) + " mMatrix=" + Arrays.toString(this.mMatrixValues) + "}";
    }

    public static final class Builder {
        private int mSelectionStart = -1;
        private int mSelectionEnd = -1;
        private int mComposingTextStart = -1;
        private CharSequence mComposingText = null;
        private float mInsertionMarkerHorizontal = Float.NaN;
        private float mInsertionMarkerTop = Float.NaN;
        private float mInsertionMarkerBaseline = Float.NaN;
        private float mInsertionMarkerBottom = Float.NaN;
        private int mInsertionMarkerFlags = 0;
        private SparseRectFArray.SparseRectFArrayBuilder mCharacterBoundsArrayBuilder = null;
        private float[] mMatrixValues = null;
        private boolean mMatrixInitialized = false;

        public Builder setSelectionRange(int i, int i2) {
            this.mSelectionStart = i;
            this.mSelectionEnd = i2;
            return this;
        }

        public Builder setComposingText(int i, CharSequence charSequence) {
            this.mComposingTextStart = i;
            if (charSequence == null) {
                this.mComposingText = null;
            } else {
                this.mComposingText = new SpannedString(charSequence);
            }
            return this;
        }

        public Builder setInsertionMarkerLocation(float f, float f2, float f3, float f4, int i) {
            this.mInsertionMarkerHorizontal = f;
            this.mInsertionMarkerTop = f2;
            this.mInsertionMarkerBaseline = f3;
            this.mInsertionMarkerBottom = f4;
            this.mInsertionMarkerFlags = i;
            return this;
        }

        public Builder addCharacterBounds(int i, float f, float f2, float f3, float f4, int i2) {
            if (i < 0) {
                throw new IllegalArgumentException("index must not be a negative integer.");
            }
            if (this.mCharacterBoundsArrayBuilder == null) {
                this.mCharacterBoundsArrayBuilder = new SparseRectFArray.SparseRectFArrayBuilder();
            }
            this.mCharacterBoundsArrayBuilder.append(i, f, f2, f3, f4, i2);
            return this;
        }

        public Builder setMatrix(Matrix matrix) {
            if (this.mMatrixValues == null) {
                this.mMatrixValues = new float[9];
            }
            if (matrix == null) {
                matrix = Matrix.IDENTITY_MATRIX;
            }
            matrix.getValues(this.mMatrixValues);
            this.mMatrixInitialized = true;
            return this;
        }

        public CursorAnchorInfo build() {
            if (!this.mMatrixInitialized) {
                if (((this.mCharacterBoundsArrayBuilder == null || this.mCharacterBoundsArrayBuilder.isEmpty()) ? false : true) || !Float.isNaN(this.mInsertionMarkerHorizontal) || !Float.isNaN(this.mInsertionMarkerTop) || !Float.isNaN(this.mInsertionMarkerBaseline) || !Float.isNaN(this.mInsertionMarkerBottom)) {
                    throw new IllegalArgumentException("Coordinate transformation matrix is required when positional parameters are specified.");
                }
            }
            return new CursorAnchorInfo(this);
        }

        public void reset() {
            this.mSelectionStart = -1;
            this.mSelectionEnd = -1;
            this.mComposingTextStart = -1;
            this.mComposingText = null;
            this.mInsertionMarkerFlags = 0;
            this.mInsertionMarkerHorizontal = Float.NaN;
            this.mInsertionMarkerTop = Float.NaN;
            this.mInsertionMarkerBaseline = Float.NaN;
            this.mInsertionMarkerBottom = Float.NaN;
            this.mMatrixInitialized = false;
            if (this.mCharacterBoundsArrayBuilder != null) {
                this.mCharacterBoundsArrayBuilder.reset();
            }
        }
    }

    private CursorAnchorInfo(Builder builder) {
        this.mSelectionStart = builder.mSelectionStart;
        this.mSelectionEnd = builder.mSelectionEnd;
        this.mComposingTextStart = builder.mComposingTextStart;
        this.mComposingText = builder.mComposingText;
        this.mInsertionMarkerFlags = builder.mInsertionMarkerFlags;
        this.mInsertionMarkerHorizontal = builder.mInsertionMarkerHorizontal;
        this.mInsertionMarkerTop = builder.mInsertionMarkerTop;
        this.mInsertionMarkerBaseline = builder.mInsertionMarkerBaseline;
        this.mInsertionMarkerBottom = builder.mInsertionMarkerBottom;
        this.mCharacterBoundsArray = builder.mCharacterBoundsArrayBuilder != null ? builder.mCharacterBoundsArrayBuilder.build() : null;
        this.mMatrixValues = new float[9];
        if (builder.mMatrixInitialized) {
            System.arraycopy(builder.mMatrixValues, 0, this.mMatrixValues, 0, 9);
        } else {
            Matrix.IDENTITY_MATRIX.getValues(this.mMatrixValues);
        }
        this.mHashCode = (Objects.hashCode(this.mComposingText) * 31) + Arrays.hashCode(this.mMatrixValues);
    }

    public int getSelectionStart() {
        return this.mSelectionStart;
    }

    public int getSelectionEnd() {
        return this.mSelectionEnd;
    }

    public int getComposingTextStart() {
        return this.mComposingTextStart;
    }

    public CharSequence getComposingText() {
        return this.mComposingText;
    }

    public int getInsertionMarkerFlags() {
        return this.mInsertionMarkerFlags;
    }

    public float getInsertionMarkerHorizontal() {
        return this.mInsertionMarkerHorizontal;
    }

    public float getInsertionMarkerTop() {
        return this.mInsertionMarkerTop;
    }

    public float getInsertionMarkerBaseline() {
        return this.mInsertionMarkerBaseline;
    }

    public float getInsertionMarkerBottom() {
        return this.mInsertionMarkerBottom;
    }

    public RectF getCharacterBounds(int i) {
        if (this.mCharacterBoundsArray == null) {
            return null;
        }
        return this.mCharacterBoundsArray.get(i);
    }

    public int getCharacterBoundsFlags(int i) {
        if (this.mCharacterBoundsArray == null) {
            return 0;
        }
        return this.mCharacterBoundsArray.getFlags(i, 0);
    }

    public Matrix getMatrix() {
        Matrix matrix = new Matrix();
        matrix.setValues(this.mMatrixValues);
        return matrix;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
