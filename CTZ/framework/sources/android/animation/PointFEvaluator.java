package android.animation;

import android.graphics.PointF;

public class PointFEvaluator implements TypeEvaluator<PointF> {
    private PointF mPoint;

    public PointFEvaluator() {
    }

    public PointFEvaluator(PointF pointF) {
        this.mPoint = pointF;
    }

    @Override
    public PointF evaluate(float f, PointF pointF, PointF pointF2) {
        float f2 = pointF.x + ((pointF2.x - pointF.x) * f);
        float f3 = pointF.y + (f * (pointF2.y - pointF.y));
        if (this.mPoint != null) {
            this.mPoint.set(f2, f3);
            return this.mPoint;
        }
        return new PointF(f2, f3);
    }
}
