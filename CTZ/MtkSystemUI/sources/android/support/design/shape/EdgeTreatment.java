package android.support.design.shape;

public class EdgeTreatment {
    public void getEdgePath(float length, float interpolation, ShapePath shapePath) {
        shapePath.lineTo(length, 0.0f);
    }
}
