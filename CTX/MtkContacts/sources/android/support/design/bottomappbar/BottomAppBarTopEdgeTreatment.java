package android.support.design.bottomappbar;

import android.support.design.shape.EdgeTreatment;
import android.support.design.shape.ShapePath;
import com.android.contacts.ContactPhotoManager;

public class BottomAppBarTopEdgeTreatment extends EdgeTreatment {
    private final float cradleDiameter;
    private float cradleVerticalOffset;
    private float horizontalOffset;
    private final float roundedCornerRadius;

    public void setHorizontalOffset(float horizontalOffset) {
        this.horizontalOffset = horizontalOffset;
    }

    @Override
    public void getEdgePath(float length, float interpolation, ShapePath shapePath) {
        float cradleRadius = (this.cradleDiameter * interpolation) / 2.0f;
        float roundedCornerOffset = interpolation * this.roundedCornerRadius;
        float middle = (length / 2.0f) + this.horizontalOffset;
        float verticalOffset = interpolation * this.cradleVerticalOffset;
        float verticalOffsetRatio = verticalOffset / cradleRadius;
        if (verticalOffsetRatio >= 1.0f) {
            shapePath.lineTo(length, ContactPhotoManager.OFFSET_DEFAULT);
            return;
        }
        float offsetSquared = verticalOffset * verticalOffset;
        float cutWidth = (float) Math.sqrt((cradleRadius * cradleRadius) - offsetSquared);
        float lowerCurveLeft = middle - cutWidth;
        float lineLeft = lowerCurveLeft - roundedCornerOffset;
        float lowerCurveRight = middle + cutWidth;
        float lineRight = lowerCurveRight + roundedCornerOffset;
        shapePath.lineTo(lineLeft, ContactPhotoManager.OFFSET_DEFAULT);
        shapePath.addArc(lineLeft, ContactPhotoManager.OFFSET_DEFAULT, lowerCurveLeft, roundedCornerOffset, 270.0f, 90.0f);
        float top = (-cradleRadius) - verticalOffset;
        float bottom = cradleRadius - verticalOffset;
        shapePath.addArc(middle - cradleRadius, top, middle + cradleRadius, bottom, 180.0f, -180.0f);
        shapePath.addArc(lowerCurveRight, ContactPhotoManager.OFFSET_DEFAULT, lineRight, roundedCornerOffset, 180.0f, 90.0f);
        shapePath.lineTo(length, ContactPhotoManager.OFFSET_DEFAULT);
    }

    public float getCradleVerticalOffset() {
        return this.cradleVerticalOffset;
    }
}
