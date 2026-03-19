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

    public void setAllCorners(CornerTreatment cornerTreatment) {
        this.topLeftCorner = cornerTreatment;
        this.topRightCorner = cornerTreatment;
        this.bottomRightCorner = cornerTreatment;
        this.bottomLeftCorner = cornerTreatment;
    }

    public void setAllEdges(EdgeTreatment edgeTreatment) {
        this.leftEdge = edgeTreatment;
        this.topEdge = edgeTreatment;
        this.rightEdge = edgeTreatment;
        this.bottomEdge = edgeTreatment;
    }

    public void setCornerTreatments(CornerTreatment topLeftCorner, CornerTreatment topRightCorner, CornerTreatment bottomRightCorner, CornerTreatment bottomLeftCorner) {
        this.topLeftCorner = topLeftCorner;
        this.topRightCorner = topRightCorner;
        this.bottomRightCorner = bottomRightCorner;
        this.bottomLeftCorner = bottomLeftCorner;
    }

    public void setEdgeTreatments(EdgeTreatment leftEdge, EdgeTreatment topEdge, EdgeTreatment rightEdge, EdgeTreatment bottomEdge) {
        this.leftEdge = leftEdge;
        this.topEdge = topEdge;
        this.rightEdge = rightEdge;
        this.bottomEdge = bottomEdge;
    }

    public CornerTreatment getTopLeftCorner() {
        return this.topLeftCorner;
    }

    public void setTopLeftCorner(CornerTreatment topLeftCorner) {
        this.topLeftCorner = topLeftCorner;
    }

    public CornerTreatment getTopRightCorner() {
        return this.topRightCorner;
    }

    public void setTopRightCorner(CornerTreatment topRightCorner) {
        this.topRightCorner = topRightCorner;
    }

    public CornerTreatment getBottomRightCorner() {
        return this.bottomRightCorner;
    }

    public void setBottomRightCorner(CornerTreatment bottomRightCorner) {
        this.bottomRightCorner = bottomRightCorner;
    }

    public CornerTreatment getBottomLeftCorner() {
        return this.bottomLeftCorner;
    }

    public void setBottomLeftCorner(CornerTreatment bottomLeftCorner) {
        this.bottomLeftCorner = bottomLeftCorner;
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

    public void setRightEdge(EdgeTreatment rightEdge) {
        this.rightEdge = rightEdge;
    }

    public EdgeTreatment getBottomEdge() {
        return this.bottomEdge;
    }

    public void setBottomEdge(EdgeTreatment bottomEdge) {
        this.bottomEdge = bottomEdge;
    }

    public EdgeTreatment getLeftEdge() {
        return this.leftEdge;
    }

    public void setLeftEdge(EdgeTreatment leftEdge) {
        this.leftEdge = leftEdge;
    }
}
