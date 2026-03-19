package android.view.animation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.PathParser;
import android.view.InflateException;
import com.android.internal.R;
import com.android.internal.view.animation.HasNativeInterpolator;
import com.android.internal.view.animation.NativeInterpolatorFactory;
import com.android.internal.view.animation.NativeInterpolatorFactoryHelper;

@HasNativeInterpolator
public class PathInterpolator extends BaseInterpolator implements NativeInterpolatorFactory {
    private static final float PRECISION = 0.002f;
    private float[] mX;
    private float[] mY;

    public PathInterpolator(Path path) {
        initPath(path);
    }

    public PathInterpolator(float f, float f2) {
        initQuad(f, f2);
    }

    public PathInterpolator(float f, float f2, float f3, float f4) {
        initCubic(f, f2, f3, f4);
    }

    public PathInterpolator(Context context, AttributeSet attributeSet) {
        this(context.getResources(), context.getTheme(), attributeSet);
    }

    public PathInterpolator(Resources resources, Resources.Theme theme, AttributeSet attributeSet) {
        TypedArray typedArrayObtainAttributes;
        if (theme != null) {
            typedArrayObtainAttributes = theme.obtainStyledAttributes(attributeSet, R.styleable.PathInterpolator, 0, 0);
        } else {
            typedArrayObtainAttributes = resources.obtainAttributes(attributeSet, R.styleable.PathInterpolator);
        }
        parseInterpolatorFromTypeArray(typedArrayObtainAttributes);
        setChangingConfiguration(typedArrayObtainAttributes.getChangingConfigurations());
        typedArrayObtainAttributes.recycle();
    }

    private void parseInterpolatorFromTypeArray(TypedArray typedArray) {
        if (typedArray.hasValue(4)) {
            String string = typedArray.getString(4);
            Path pathCreatePathFromPathData = PathParser.createPathFromPathData(string);
            if (pathCreatePathFromPathData == null) {
                throw new InflateException("The path is null, which is created from " + string);
            }
            initPath(pathCreatePathFromPathData);
            return;
        }
        if (!typedArray.hasValue(0)) {
            throw new InflateException("pathInterpolator requires the controlX1 attribute");
        }
        if (typedArray.hasValue(1)) {
            float f = typedArray.getFloat(0, 0.0f);
            float f2 = typedArray.getFloat(1, 0.0f);
            boolean zHasValue = typedArray.hasValue(2);
            if (zHasValue != typedArray.hasValue(3)) {
                throw new InflateException("pathInterpolator requires both controlX2 and controlY2 for cubic Beziers.");
            }
            if (!zHasValue) {
                initQuad(f, f2);
                return;
            } else {
                initCubic(f, f2, typedArray.getFloat(2, 0.0f), typedArray.getFloat(3, 0.0f));
                return;
            }
        }
        throw new InflateException("pathInterpolator requires the controlY1 attribute");
    }

    private void initQuad(float f, float f2) {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.quadTo(f, f2, 1.0f, 1.0f);
        initPath(path);
    }

    private void initCubic(float f, float f2, float f3, float f4) {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo(f, f2, f3, f4, 1.0f, 1.0f);
        initPath(path);
    }

    private void initPath(Path path) {
        float[] fArrApproximate = path.approximate(PRECISION);
        int length = fArrApproximate.length / 3;
        float f = 0.0f;
        if (fArrApproximate[1] != 0.0f || fArrApproximate[2] != 0.0f || fArrApproximate[fArrApproximate.length - 2] != 1.0f || fArrApproximate[fArrApproximate.length - 1] != 1.0f) {
            throw new IllegalArgumentException("The Path must start at (0,0) and end at (1,1)");
        }
        this.mX = new float[length];
        this.mY = new float[length];
        int i = 0;
        int i2 = 0;
        float f2 = 0.0f;
        while (i < length) {
            int i3 = i2 + 1;
            float f3 = fArrApproximate[i2];
            int i4 = i3 + 1;
            float f4 = fArrApproximate[i3];
            int i5 = i4 + 1;
            float f5 = fArrApproximate[i4];
            if (f3 == f && f4 != f2) {
                throw new IllegalArgumentException("The Path cannot have discontinuity in the X axis.");
            }
            if (f4 < f2) {
                throw new IllegalArgumentException("The Path cannot loop back on itself.");
            }
            this.mX[i] = f4;
            this.mY[i] = f5;
            i++;
            f = f3;
            f2 = f4;
            i2 = i5;
        }
    }

    @Override
    public float getInterpolation(float f) {
        if (f <= 0.0f) {
            return 0.0f;
        }
        if (f >= 1.0f) {
            return 1.0f;
        }
        int i = 0;
        int length = this.mX.length - 1;
        while (length - i > 1) {
            int i2 = (i + length) / 2;
            if (f < this.mX[i2]) {
                length = i2;
            } else {
                i = i2;
            }
        }
        float f2 = this.mX[length] - this.mX[i];
        if (f2 == 0.0f) {
            return this.mY[i];
        }
        float f3 = (f - this.mX[i]) / f2;
        float f4 = this.mY[i];
        return f4 + (f3 * (this.mY[length] - f4));
    }

    @Override
    public long createNativeInterpolator() {
        return NativeInterpolatorFactoryHelper.createPathInterpolator(this.mX, this.mY);
    }
}
