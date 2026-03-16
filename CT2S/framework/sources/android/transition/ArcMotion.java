package android.transition;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.FloatMath;
import com.android.internal.R;

public class ArcMotion extends PathMotion {
    private static final float DEFAULT_MAX_ANGLE_DEGREES = 70.0f;
    private static final float DEFAULT_MAX_TANGENT = (float) Math.tan(Math.toRadians(35.0d));
    private static final float DEFAULT_MIN_ANGLE_DEGREES = 0.0f;
    private float mMaximumAngle;
    private float mMaximumTangent;
    private float mMinimumHorizontalAngle;
    private float mMinimumHorizontalTangent;
    private float mMinimumVerticalAngle;
    private float mMinimumVerticalTangent;

    public ArcMotion() {
        this.mMinimumHorizontalAngle = 0.0f;
        this.mMinimumVerticalAngle = 0.0f;
        this.mMaximumAngle = DEFAULT_MAX_ANGLE_DEGREES;
        this.mMinimumHorizontalTangent = 0.0f;
        this.mMinimumVerticalTangent = 0.0f;
        this.mMaximumTangent = DEFAULT_MAX_TANGENT;
    }

    public ArcMotion(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMinimumHorizontalAngle = 0.0f;
        this.mMinimumVerticalAngle = 0.0f;
        this.mMaximumAngle = DEFAULT_MAX_ANGLE_DEGREES;
        this.mMinimumHorizontalTangent = 0.0f;
        this.mMinimumVerticalTangent = 0.0f;
        this.mMaximumTangent = DEFAULT_MAX_TANGENT;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ArcMotion);
        float minimumVerticalAngle = a.getFloat(1, 0.0f);
        setMinimumVerticalAngle(minimumVerticalAngle);
        float minimumHorizontalAngle = a.getFloat(0, 0.0f);
        setMinimumHorizontalAngle(minimumHorizontalAngle);
        float maximumAngle = a.getFloat(2, DEFAULT_MAX_ANGLE_DEGREES);
        setMaximumAngle(maximumAngle);
        a.recycle();
    }

    public void setMinimumHorizontalAngle(float angleInDegrees) {
        this.mMinimumHorizontalAngle = angleInDegrees;
        this.mMinimumHorizontalTangent = toTangent(angleInDegrees);
    }

    public float getMinimumHorizontalAngle() {
        return this.mMinimumHorizontalAngle;
    }

    public void setMinimumVerticalAngle(float angleInDegrees) {
        this.mMinimumVerticalAngle = angleInDegrees;
        this.mMinimumVerticalTangent = toTangent(angleInDegrees);
    }

    public float getMinimumVerticalAngle() {
        return this.mMinimumVerticalAngle;
    }

    public void setMaximumAngle(float angleInDegrees) {
        this.mMaximumAngle = angleInDegrees;
        this.mMaximumTangent = toTangent(angleInDegrees);
    }

    public float getMaximumAngle() {
        return this.mMaximumAngle;
    }

    private static float toTangent(float arcInDegrees) {
        if (arcInDegrees < 0.0f || arcInDegrees > 90.0f) {
            throw new IllegalArgumentException("Arc must be between 0 and 90 degrees");
        }
        return (float) Math.tan(Math.toRadians(arcInDegrees / 2.0f));
    }

    @Override
    public Path getPath(float startX, float startY, float endX, float endY) {
        float ex;
        float ey;
        float minimumArcDist2;
        Path path = new Path();
        path.moveTo(startX, startY);
        if (startY == endY) {
            ex = (startX + endX) / 2.0f;
            ey = startY + ((this.mMinimumHorizontalTangent * Math.abs(endX - startX)) / 2.0f);
        } else if (startX == endX) {
            ex = startX + ((this.mMinimumVerticalTangent * Math.abs(endY - startY)) / 2.0f);
            ey = (startY + endY) / 2.0f;
        } else {
            float deltaX = endX - startX;
            float deltaY = startY - endY;
            float h2 = (deltaX * deltaX) + (deltaY * deltaY);
            float dx = (startX + endX) / 2.0f;
            float dy = (startY + endY) / 2.0f;
            float midDist2 = h2 * 0.25f;
            if (Math.abs(deltaX) < Math.abs(deltaY)) {
                float eDistY = h2 / (2.0f * deltaY);
                ey = endY + eDistY;
                ex = endX;
                minimumArcDist2 = this.mMinimumVerticalTangent * midDist2 * this.mMinimumVerticalTangent;
            } else {
                float eDistX = h2 / (2.0f * deltaX);
                ex = endX + eDistX;
                ey = endY;
                minimumArcDist2 = this.mMinimumHorizontalTangent * midDist2 * this.mMinimumHorizontalTangent;
            }
            float arcDistX = dx - ex;
            float arcDistY = dy - ey;
            float arcDist2 = (arcDistX * arcDistX) + (arcDistY * arcDistY);
            float maximumArcDist2 = this.mMaximumTangent * midDist2 * this.mMaximumTangent;
            float newArcDistance2 = 0.0f;
            if (arcDist2 < minimumArcDist2) {
                newArcDistance2 = minimumArcDist2;
            } else if (arcDist2 > maximumArcDist2) {
                newArcDistance2 = maximumArcDist2;
            }
            if (newArcDistance2 != 0.0f) {
                float ratio2 = newArcDistance2 / arcDist2;
                float ratio = FloatMath.sqrt(ratio2);
                ex = dx + ((ex - dx) * ratio);
                ey = dy + ((ey - dy) * ratio);
            }
        }
        float controlX1 = (startX + ex) / 2.0f;
        float controlY1 = (startY + ey) / 2.0f;
        float controlX2 = (ex + endX) / 2.0f;
        float controlY2 = (ey + endY) / 2.0f;
        path.cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY);
        return path;
    }
}
