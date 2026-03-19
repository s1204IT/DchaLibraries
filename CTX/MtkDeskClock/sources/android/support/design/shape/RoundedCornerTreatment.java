package android.support.design.shape;

public class RoundedCornerTreatment extends CornerTreatment {
    private final float radius;

    public RoundedCornerTreatment(float radius) {
        this.radius = radius;
    }

    @Override
    public void getCornerPath(float angle, float interpolation, ShapePath shapePath) {
        shapePath.reset(0.0f, this.radius * interpolation);
        shapePath.addArc(0.0f, 0.0f, this.radius * 2.0f * interpolation, 2.0f * this.radius * interpolation, angle + 180.0f, 90.0f);
    }
}
