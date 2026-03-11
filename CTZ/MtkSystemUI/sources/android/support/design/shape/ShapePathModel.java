package android.support.design.shape;

public class ShapePathModel {
    private static final CornerTreatment DEFAULT_CORNER_TREATMENT = new CornerTreatment();
    private static final EdgeTreatment DEFAULT_EDGE_TREATMENT = new EdgeTreatment();
    private CornerTreatment topLeftCorner = DEFAULT_CORNER_TREATMENT;
    private CornerTreatment topRightCorner = DEFAULT_CORNER_TREATMENT;
    private CornerTreatment bottomRightCorner = DEFAULT_CORNER_TREATMENT;
    private CornerTreatment bottomLeftCorner = DEFAULT_CORNER_TREATMENT;
    private EdgeTreatment topEdge = DEFAULT_EDGE_TREATMENT;
    private EdgeTreatment rightEdge = DEFAULT_EDGE_TREATMENT;
    private EdgeTreatment bottomEdge = DEFAULT_EDGE_TREATMENT;
    private EdgeTreatment leftEdge = DEFAULT_EDGE_TREATMENT;

    public CornerTreatment getTopLeftCorner() {
        return this.topLeftCorner;
    }

    public CornerTreatment getTopRightCorner() {
        return this.topRightCorner;
    }

    public CornerTreatment getBottomRightCorner() {
        return this.bottomRightCorner;
    }

    public CornerTreatment getBottomLeftCorner() {
        return this.bottomLeftCorner;
    }

    public EdgeTreatment getTopEdge() {
        return this.topEdge;
    }

    public void setTopEdge(EdgeTreatment topEdge) {
        this.topEdge = topEdge;
    }

    public EdgeTreatment getRightEdge() {
        return this.rightEdge;
    }

    public EdgeTreatment getBottomEdge() {
        return this.bottomEdge;
    }

    public EdgeTreatment getLeftEdge() {
        return this.leftEdge;
    }
}
