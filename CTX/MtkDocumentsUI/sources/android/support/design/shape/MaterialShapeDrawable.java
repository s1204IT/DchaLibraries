package android.support.design.shape;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.TintAwareDrawable;

public class MaterialShapeDrawable extends Drawable implements TintAwareDrawable {
    private int alpha;
    private final ShapePath[] cornerPaths;
    private final Matrix[] cornerTransforms;
    private final Matrix[] edgeTransforms;
    private float interpolation;
    private final Matrix matrix;
    private final Paint paint;
    private Paint.Style paintStyle;
    private final Path path;
    private final PointF pointF;
    private float scale;
    private final float[] scratch;
    private final float[] scratch2;
    private final Region scratchRegion;
    private int shadowColor;
    private int shadowElevation;
    private boolean shadowEnabled;
    private int shadowRadius;
    private final ShapePath shapePath;
    private ShapePathModel shapedViewModel;
    private float strokeWidth;
    private PorterDuffColorFilter tintFilter;
    private ColorStateList tintList;
    private PorterDuff.Mode tintMode;
    private final Region transparentRegion;
    private boolean useTintColorForShadow;

    @Override
    public void setTintList(ColorStateList tintList) {
        this.tintList = tintList;
        updateTintFilter();
        invalidateSelf();
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        this.tintMode = tintMode;
        updateTintFilter();
        invalidateSelf();
    }

    @Override
    public void setTint(int tintColor) {
        setTintList(ColorStateList.valueOf(tintColor));
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public Region getTransparentRegion() {
        Rect bounds = getBounds();
        this.transparentRegion.set(bounds);
        getPath(bounds.width(), bounds.height(), this.path);
        this.scratchRegion.setPath(this.path, this.transparentRegion);
        this.transparentRegion.op(this.scratchRegion, Region.Op.DIFFERENCE);
        return this.transparentRegion;
    }

    public void setInterpolation(float interpolation) {
        this.interpolation = interpolation;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        this.paint.setColorFilter(this.tintFilter);
        int prevAlpha = this.paint.getAlpha();
        this.paint.setAlpha(modulateAlpha(prevAlpha, this.alpha));
        this.paint.setStrokeWidth(this.strokeWidth);
        this.paint.setStyle(this.paintStyle);
        if (this.shadowElevation > 0 && this.shadowEnabled) {
            this.paint.setShadowLayer(this.shadowRadius, this.shadowElevation, this.shadowElevation, this.shadowColor);
        }
        if (this.shapedViewModel != null) {
            getPath(canvas.getWidth(), canvas.getHeight(), this.path);
            canvas.drawPath(this.path, this.paint);
        } else {
            canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), this.paint);
        }
        this.paint.setAlpha(prevAlpha);
    }

    public void getPathForSize(int width, int height, Path path) {
        path.rewind();
        if (this.shapedViewModel == null) {
            return;
        }
        for (int index = 0; index < 4; index++) {
            setCornerPathAndTransform(index, width, height);
            setEdgeTransform(index, width, height);
        }
        for (int index2 = 0; index2 < 4; index2++) {
            appendCornerPath(index2, path);
            appendEdgePath(index2, path);
        }
        path.close();
    }

    private void setCornerPathAndTransform(int index, int width, int height) {
        getCoordinatesOfCorner(index, width, height, this.pointF);
        float angle = angleOfCorner(index, width, height);
        getCornerTreatmentForIndex(index).getCornerPath(angle, this.interpolation, this.cornerPaths[index]);
        float prevEdgeAngle = angleOfEdge(((index - 1) + 4) % 4, width, height) + 1.5707964f;
        this.cornerTransforms[index].reset();
        this.cornerTransforms[index].setTranslate(this.pointF.x, this.pointF.y);
        this.cornerTransforms[index].preRotate((float) Math.toDegrees(prevEdgeAngle));
    }

    private void setEdgeTransform(int index, int width, int height) {
        this.scratch[0] = this.cornerPaths[index].endX;
        this.scratch[1] = this.cornerPaths[index].endY;
        this.cornerTransforms[index].mapPoints(this.scratch);
        float edgeAngle = angleOfEdge(index, width, height);
        this.edgeTransforms[index].reset();
        this.edgeTransforms[index].setTranslate(this.scratch[0], this.scratch[1]);
        this.edgeTransforms[index].preRotate((float) Math.toDegrees(edgeAngle));
    }

    private void appendCornerPath(int index, Path path) {
        this.scratch[0] = this.cornerPaths[index].startX;
        this.scratch[1] = this.cornerPaths[index].startY;
        this.cornerTransforms[index].mapPoints(this.scratch);
        if (index == 0) {
            path.moveTo(this.scratch[0], this.scratch[1]);
        } else {
            path.lineTo(this.scratch[0], this.scratch[1]);
        }
        this.cornerPaths[index].applyToPath(this.cornerTransforms[index], path);
    }

    private void appendEdgePath(int index, Path path) {
        int nextIndex = (index + 1) % 4;
        this.scratch[0] = this.cornerPaths[index].endX;
        this.scratch[1] = this.cornerPaths[index].endY;
        this.cornerTransforms[index].mapPoints(this.scratch);
        this.scratch2[0] = this.cornerPaths[nextIndex].startX;
        this.scratch2[1] = this.cornerPaths[nextIndex].startY;
        this.cornerTransforms[nextIndex].mapPoints(this.scratch2);
        float edgeLength = (float) Math.hypot(this.scratch[0] - this.scratch2[0], this.scratch[1] - this.scratch2[1]);
        this.shapePath.reset(0.0f, 0.0f);
        getEdgeTreatmentForIndex(index).getEdgePath(edgeLength, this.interpolation, this.shapePath);
        this.shapePath.applyToPath(this.edgeTransforms[index], path);
    }

    private CornerTreatment getCornerTreatmentForIndex(int index) {
        switch (index) {
            case 1:
                return this.shapedViewModel.getTopRightCorner();
            case 2:
                return this.shapedViewModel.getBottomRightCorner();
            case 3:
                return this.shapedViewModel.getBottomLeftCorner();
            default:
                return this.shapedViewModel.getTopLeftCorner();
        }
    }

    private EdgeTreatment getEdgeTreatmentForIndex(int index) {
        switch (index) {
            case 1:
                return this.shapedViewModel.getRightEdge();
            case 2:
                return this.shapedViewModel.getBottomEdge();
            case 3:
                return this.shapedViewModel.getLeftEdge();
            default:
                return this.shapedViewModel.getTopEdge();
        }
    }

    private void getCoordinatesOfCorner(int index, int width, int height, PointF pointF) {
        switch (index) {
            case 1:
                pointF.set(width, 0.0f);
                break;
            case 2:
                pointF.set(width, height);
                break;
            case 3:
                pointF.set(0.0f, height);
                break;
            default:
                pointF.set(0.0f, 0.0f);
                break;
        }
    }

    private float angleOfCorner(int index, int width, int height) {
        getCoordinatesOfCorner(((index - 1) + 4) % 4, width, height, this.pointF);
        float prevCornerCoordX = this.pointF.x;
        float prevCornerCoordY = this.pointF.y;
        getCoordinatesOfCorner((index + 1) % 4, width, height, this.pointF);
        float nextCornerCoordX = this.pointF.x;
        float nextCornerCoordY = this.pointF.y;
        getCoordinatesOfCorner(index, width, height, this.pointF);
        float cornerCoordX = this.pointF.x;
        float cornerCoordY = this.pointF.y;
        float prevVectorX = prevCornerCoordX - cornerCoordX;
        float prevVectorY = prevCornerCoordY - cornerCoordY;
        float nextVectorX = nextCornerCoordX - cornerCoordX;
        float nextVectorY = nextCornerCoordY - cornerCoordY;
        float prevAngle = (float) Math.atan2(prevVectorY, prevVectorX);
        float nextAngle = (float) Math.atan2(nextVectorY, nextVectorX);
        float angle = prevAngle - nextAngle;
        if (angle < 0.0f) {
            return (float) (((double) angle) + 6.283185307179586d);
        }
        return angle;
    }

    private float angleOfEdge(int index, int width, int height) {
        int endCornerPoisition = (index + 1) % 4;
        getCoordinatesOfCorner(index, width, height, this.pointF);
        float startCornerCoordX = this.pointF.x;
        float startCornerCoordY = this.pointF.y;
        getCoordinatesOfCorner(endCornerPoisition, width, height, this.pointF);
        float endCornerCoordX = this.pointF.x;
        float endCornerCoordY = this.pointF.y;
        float edgeVectorX = endCornerCoordX - startCornerCoordX;
        float edgeVectorY = endCornerCoordY - startCornerCoordY;
        return (float) Math.atan2(edgeVectorY, edgeVectorX);
    }

    private static int modulateAlpha(int paintAlpha, int alpha) {
        int scale = (alpha >>> 7) + alpha;
        return (paintAlpha * scale) >>> 8;
    }

    private void getPath(int width, int height, Path path) {
        getPathForSize(width, height, path);
        if (this.scale == 1.0f) {
            return;
        }
        this.matrix.reset();
        this.matrix.setScale(this.scale, this.scale, width / 2, height / 2);
        path.transform(this.matrix);
    }

    private void updateTintFilter() {
        if (this.tintList == null || this.tintMode == null) {
            this.tintFilter = null;
            return;
        }
        int color = this.tintList.getColorForState(getState(), 0);
        this.tintFilter = new PorterDuffColorFilter(color, this.tintMode);
        if (this.useTintColorForShadow) {
            this.shadowColor = color;
        }
    }
}
