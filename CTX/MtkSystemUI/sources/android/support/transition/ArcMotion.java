package android.support.transition;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.AttributeSet;
import org.xmlpull.v1.XmlPullParser;

public class ArcMotion extends PathMotion {
    private static final float DEFAULT_MAX_TANGENT = (float) Math.tan(Math.toRadians(35.0d));
    private float mMaximumAngle;
    private float mMaximumTangent;
    private float mMinimumHorizontalAngle;
    private float mMinimumHorizontalTangent;
    private float mMinimumVerticalAngle;
    private float mMinimumVerticalTangent;

    public ArcMotion() {
        this.mMinimumHorizontalAngle = 0.0f;
        this.mMinimumVerticalAngle = 0.0f;
        this.mMaximumAngle = 70.0f;
        this.mMinimumHorizontalTangent = 0.0f;
        this.mMinimumVerticalTangent = 0.0f;
        this.mMaximumTangent = DEFAULT_MAX_TANGENT;
    }

    public ArcMotion(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMinimumHorizontalAngle = 0.0f;
        this.mMinimumVerticalAngle = 0.0f;
        this.mMaximumAngle = 70.0f;
        this.mMinimumHorizontalTangent = 0.0f;
        this.mMinimumVerticalTangent = 0.0f;
        this.mMaximumTangent = DEFAULT_MAX_TANGENT;
        TypedArray a = context.obtainStyledAttributes(attrs, Styleable.ARC_MOTION);
        XmlPullParser parser = (XmlPullParser) attrs;
        float minimumVerticalAngle = TypedArrayUtils.getNamedFloat(a, parser, "minimumVerticalAngle", 1, 0.0f);
        setMinimumVerticalAngle(minimumVerticalAngle);
        float minimumHorizontalAngle = TypedArrayUtils.getNamedFloat(a, parser, "minimumHorizontalAngle", 0, 0.0f);
        setMinimumHorizontalAngle(minimumHorizontalAngle);
        float maximumAngle = TypedArrayUtils.getNamedFloat(a, parser, "maximumAngle", 2, 70.0f);
        setMaximumAngle(maximumAngle);
        a.recycle();
    }

    public void setMinimumHorizontalAngle(float angleInDegrees) {
        this.mMinimumHorizontalAngle = angleInDegrees;
        this.mMinimumHorizontalTangent = toTangent(angleInDegrees);
    }

    public void setMinimumVerticalAngle(float angleInDegrees) {
        this.mMinimumVerticalAngle = angleInDegrees;
        this.mMinimumVerticalTangent = toTangent(angleInDegrees);
    }

    public void setMaximumAngle(float angleInDegrees) {
        this.mMaximumAngle = angleInDegrees;
        this.mMaximumTangent = toTangent(angleInDegrees);
    }

    private static float toTangent(float arcInDegrees) {
        if (arcInDegrees < 0.0f || arcInDegrees > 90.0f) {
            throw new IllegalArgumentException("Arc must be between 0 and 90 degrees");
        }
        return (float) Math.tan(Math.toRadians(arcInDegrees / 2.0f));
    }

    @Override
    public Path getPath(float startX, float startY, float endX, float endY) {
        float ey;
        float ex;
        float minimumArcDist2;
        float ey2;
        float ex2;
        Path path = new Path();
        path.moveTo(startX, startY);
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        float h2 = (deltaX * deltaX) + (deltaY * deltaY);
        float dx = (startX + endX) / 2.0f;
        float dy = (startY + endY) / 2.0f;
        float midDist2 = h2 * 0.25f;
        boolean isMovingUpwards = startY > endY;
        if (Math.abs(deltaX) < Math.abs(deltaY)) {
            float eDistY = Math.abs(h2 / (2.0f * deltaY));
            if (isMovingUpwards) {
                ey2 = endY + eDistY;
                ex2 = endX;
            } else {
                ey2 = startY + eDistY;
                ex2 = startX;
            }
            minimumArcDist2 = this.mMinimumVerticalTangent * midDist2 * this.mMinimumVerticalTangent;
            float f = ex2;
            ex = ey2;
            ey = f;
        } else {
            float eDistX = h2 / (2.0f * deltaX);
            if (isMovingUpwards) {
                ey = startX + eDistX;
                ex = startY;
            } else {
                ey = endX - eDistX;
                ex = endY;
            }
            minimumArcDist2 = this.mMinimumHorizontalTangent * midDist2 * this.mMinimumHorizontalTangent;
        }
        float minimumArcDist22 = minimumArcDist2;
        float arcDistX = dx - ey;
        float arcDistY = dy - ex;
        float arcDist2 = (arcDistX * arcDistX) + (arcDistY * arcDistY);
        float maximumArcDist2 = this.mMaximumTangent * midDist2 * this.mMaximumTangent;
        float newArcDistance2 = 0.0f;
        if (arcDist2 < minimumArcDist22) {
            newArcDistance2 = minimumArcDist22;
        } else if (arcDist2 > maximumArcDist2) {
            newArcDistance2 = maximumArcDist2;
        }
        float newArcDistance22 = newArcDistance2;
        if (newArcDistance22 != 0.0f) {
            float ratio2 = newArcDistance22 / arcDist2;
            float ratio = (float) Math.sqrt(ratio2);
            ey = dx + ((ey - dx) * ratio);
            ex = dy + ((ex - dy) * ratio);
        }
        float ex3 = ey;
        float ey3 = ex;
        float control1X = (startX + ex3) / 2.0f;
        float control1Y = (startY + ey3) / 2.0f;
        float control2X = (ex3 + endX) / 2.0f;
        float control2Y = (ey3 + endY) / 2.0f;
        path.cubicTo(control1X, control1Y, control2X, control2Y, endX, endY);
        return path;
    }
}
