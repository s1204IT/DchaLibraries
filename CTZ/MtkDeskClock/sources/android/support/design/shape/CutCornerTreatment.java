package android.support.design.shape;

public class CutCornerTreatment extends CornerTreatment {
    private final float size;

    public CutCornerTreatment(float size) {
        this.size = size;
    }

    @Override
    public void getCornerPath(float angle, float interpolation, ShapePath shapePath) {
        shapePath.reset(0.0f, this.size * interpolation);
        shapePath.lineTo((float) (Math.sin(angle) * ((double) this.size) * ((double) interpolation)), (float) (Math.cos(angle) * ((double) this.size) * ((double) interpolation)));
    }
}
