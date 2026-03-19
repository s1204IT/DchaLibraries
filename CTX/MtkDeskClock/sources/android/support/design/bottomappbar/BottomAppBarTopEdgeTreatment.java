package android.support.design.bottomappbar;

import android.support.design.shape.EdgeTreatment;
import android.support.design.shape.ShapePath;

public class BottomAppBarTopEdgeTreatment extends EdgeTreatment {
    private final float cradleDiameter;
    private float cradleVerticalOffset;
    private float horizontalOffset;
    private final float roundedCornerRadius;

    public BottomAppBarTopEdgeTreatment(float cradleDiameter, float roundedCornerRadius, float cradleVerticalOffset) {
        this.cradleDiameter = cradleDiameter;
        this.roundedCornerRadius = roundedCornerRadius;
        this.cradleVerticalOffset = cradleVerticalOffset;
        if (cradleVerticalOffset < 0.0f) {
            throw new IllegalArgumentException("cradleVerticalOffset must be positive.");
        }
        this.horizontalOffset = 0.0f;
    }

    public void setHorizontalOffset(float horizontalOffset) {
        this.horizontalOffset = horizontalOffset;
    }

    public float getHorizontalOffset() {
        return this.horizontalOffset;
    }

    @Override
    public void getEdgePath(float length, float interpolation, ShapePath shapePath) {
        float cradleRadius = (this.cradleDiameter * interpolation) / 2.0f;
        float roundedCornerOffset = interpolation * this.roundedCornerRadius;
        float middle = (length / 2.0f) + this.horizontalOffset;
        float verticalOffset = interpolation * this.cradleVerticalOffset;
        float verticalOffsetRatio = verticalOffset / cradleRadius;
        if (verticalOffsetRatio >= 1.0f) {
            shapePath.lineTo(length, 0.0f);
            return;
        }
        float offsetSquared = verticalOffset * verticalOffset;
        float cutWidth = (float) Math.sqrt((cradleRadius * cradleRadius) - offsetSquared);
        float lowerCurveLeft = middle - cutWidth;
        float lineLeft = lowerCurveLeft - roundedCornerOffset;
        float lowerCurveRight = middle + cutWidth;
        float lineRight = lowerCurveRight + roundedCornerOffset;
        shapePath.lineTo(lineLeft, 0.0f);
        shapePath.addArc(lineLeft, 0.0f, lowerCurveLeft, roundedCornerOffset, 270.0f, 90.0f);
        float top = (-cradleRadius) - verticalOffset;
        float bottom = cradleRadius - verticalOffset;
        shapePath.addArc(middle - cradleRadius, top, middle + cradleRadius, bottom, 180.0f, -180.0f);
        shapePath.addArc(lowerCurveRight, 0.0f, lineRight, roundedCornerOffset, 180.0f, 90.0f);
        shapePath.lineTo(length, 0.0f);
    }

    public float getCradleVerticalOffset() {
        return this.cradleVerticalOffset;
    }

    public void setCradleVerticalOffset(float cradleVerticalOffset) {
        this.cradleVerticalOffset = cradleVerticalOffset;
    }
}
