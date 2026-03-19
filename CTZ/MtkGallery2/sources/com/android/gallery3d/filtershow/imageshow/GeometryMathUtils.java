package com.android.gallery3d.filtershow.imageshow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation;
import com.android.gallery3d.filtershow.filters.FilterStraightenRepresentation;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import java.util.Collection;
import java.util.Iterator;

public final class GeometryMathUtils {

    public static final class GeometryHolder {
        public FilterRotateRepresentation.Rotation rotation = FilterRotateRepresentation.getNil();
        public float straighten = FilterStraightenRepresentation.getNil();
        public RectF crop = FilterCropRepresentation.getNil();
        public FilterMirrorRepresentation.Mirror mirror = FilterMirrorRepresentation.getNil();

        public void set(GeometryHolder geometryHolder) {
            this.rotation = geometryHolder.rotation;
            this.straighten = geometryHolder.straighten;
            this.crop.set(geometryHolder.crop);
            this.mirror = geometryHolder.mirror;
        }

        public void wipe() {
            this.rotation = FilterRotateRepresentation.getNil();
            this.straighten = FilterStraightenRepresentation.getNil();
            this.crop = FilterCropRepresentation.getNil();
            this.mirror = FilterMirrorRepresentation.getNil();
        }

        public boolean isNil() {
            return this.rotation == FilterRotateRepresentation.getNil() && this.straighten == FilterStraightenRepresentation.getNil() && this.crop.equals(FilterCropRepresentation.getNil()) && this.mirror == FilterMirrorRepresentation.getNil();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof GeometryHolder)) {
                return false;
            }
            GeometryHolder geometryHolder = (GeometryHolder) obj;
            return this.rotation == geometryHolder.rotation && this.straighten == geometryHolder.straighten && ((this.crop == null && geometryHolder.crop == null) || (this.crop != null && this.crop.equals(geometryHolder.crop))) && this.mirror == geometryHolder.mirror;
        }

        public String toString() {
            return getClass().getSimpleName() + "[rotation:" + this.rotation.value() + ",straighten:" + this.straighten + ",crop:" + this.crop.toString() + ",mirror:" + this.mirror.value() + "]";
        }
    }

    public static float clamp(float f, float f2, float f3) {
        return Math.max(Math.min(f, f3), f2);
    }

    public static float[] lineIntersect(float[] fArr, float[] fArr2) {
        float f = fArr[0];
        float f2 = fArr[1];
        float f3 = fArr[2];
        float f4 = fArr[3];
        float f5 = fArr2[0];
        float f6 = fArr2[1];
        float f7 = fArr2[2];
        float f8 = fArr2[3];
        float f9 = f - f3;
        float f10 = f2 - f4;
        float f11 = f3 - f7;
        float f12 = f8 - f4;
        float f13 = f5 - f7;
        float f14 = f6 - f8;
        float f15 = (f10 * f13) - (f9 * f14);
        if (f15 == 0.0f) {
            return null;
        }
        float f16 = ((f12 * f13) + (f14 * f11)) / f15;
        return new float[]{f3 + (f9 * f16), f4 + (f16 * f10)};
    }

    public static float[] shortestVectorFromPointToLine(float[] fArr, float[] fArr2) {
        float f = fArr2[0];
        float f2 = fArr2[2];
        float f3 = fArr2[1];
        float f4 = f2 - f;
        float f5 = fArr2[3] - f3;
        if (f4 != 0.0f || f5 != 0.0f) {
            float f6 = (((fArr[0] - f) * f4) + ((fArr[1] - f3) * f5)) / ((f4 * f4) + (f5 * f5));
            float[] fArr3 = {f + (f4 * f6), f3 + (f6 * f5)};
            return new float[]{fArr3[0] - fArr[0], fArr3[1] - fArr[1]};
        }
        return null;
    }

    public static float dotProduct(float[] fArr, float[] fArr2) {
        return (fArr[0] * fArr2[0]) + (fArr[1] * fArr2[1]);
    }

    public static float[] normalize(float[] fArr) {
        float fHypot = (float) Math.hypot(fArr[0], fArr[1]);
        return new float[]{fArr[0] / fHypot, fArr[1] / fHypot};
    }

    public static float scalarProjection(float[] fArr, float[] fArr2) {
        return dotProduct(fArr, fArr2) / ((float) Math.hypot(fArr2[0], fArr2[1]));
    }

    public static void scaleRect(RectF rectF, float f) {
        rectF.set(rectF.left * f, rectF.top * f, rectF.right * f, rectF.bottom * f);
    }

    public static float vectorLength(float[] fArr) {
        return (float) Math.hypot(fArr[0], fArr[1]);
    }

    public static float scale(float f, float f2, float f3, float f4) {
        if (f2 == 0.0f || f == 0.0f) {
            return 1.0f;
        }
        if (f == f3 && f2 == f4) {
            return 1.0f;
        }
        return Math.min(f3 / f, f4 / f2);
    }

    private static void concatMirrorMatrix(Matrix matrix, GeometryHolder geometryHolder) {
        FilterMirrorRepresentation.Mirror mirror = geometryHolder.mirror;
        if (mirror == FilterMirrorRepresentation.Mirror.HORIZONTAL) {
            if (geometryHolder.rotation.value() == 90 || geometryHolder.rotation.value() == 270) {
                mirror = FilterMirrorRepresentation.Mirror.VERTICAL;
            }
        } else if (mirror == FilterMirrorRepresentation.Mirror.VERTICAL && (geometryHolder.rotation.value() == 90 || geometryHolder.rotation.value() == 270)) {
            mirror = FilterMirrorRepresentation.Mirror.HORIZONTAL;
        }
        if (mirror == FilterMirrorRepresentation.Mirror.HORIZONTAL) {
            matrix.postScale(-1.0f, 1.0f);
            return;
        }
        if (mirror == FilterMirrorRepresentation.Mirror.VERTICAL) {
            matrix.postScale(1.0f, -1.0f);
        } else if (mirror == FilterMirrorRepresentation.Mirror.BOTH) {
            matrix.postScale(1.0f, -1.0f);
            matrix.postScale(-1.0f, 1.0f);
        }
    }

    private static int getRotationForOrientation(int i) {
        if (i == 3) {
            return 180;
        }
        if (i == 6) {
            return 90;
        }
        if (i == 8) {
            return 270;
        }
        return 0;
    }

    public static GeometryHolder unpackGeometry(Collection<FilterRepresentation> collection) {
        GeometryHolder geometryHolder = new GeometryHolder();
        unpackGeometry(geometryHolder, collection);
        return geometryHolder;
    }

    public static void unpackGeometry(GeometryHolder geometryHolder, Collection<FilterRepresentation> collection) {
        geometryHolder.wipe();
        for (FilterRepresentation filterRepresentation : collection) {
            if (!filterRepresentation.isNil()) {
                if (filterRepresentation.getSerializationName() == "ROTATION") {
                    geometryHolder.rotation = ((FilterRotateRepresentation) filterRepresentation).getRotation();
                } else if (filterRepresentation.getSerializationName() == "STRAIGHTEN") {
                    geometryHolder.straighten = ((FilterStraightenRepresentation) filterRepresentation).getStraighten();
                } else if (filterRepresentation.getSerializationName() == "CROP") {
                    ((FilterCropRepresentation) filterRepresentation).getCrop(geometryHolder.crop);
                } else if (filterRepresentation.getSerializationName() == "MIRROR") {
                    geometryHolder.mirror = ((FilterMirrorRepresentation) filterRepresentation).getMirror();
                }
            }
        }
    }

    public static void replaceInstances(Collection<FilterRepresentation> collection, FilterRepresentation filterRepresentation) {
        Iterator<FilterRepresentation> it = collection.iterator();
        while (it.hasNext()) {
            if (ImagePreset.sameSerializationName(filterRepresentation, it.next())) {
                it.remove();
            }
        }
        if (!filterRepresentation.isNil()) {
            collection.add(filterRepresentation);
        }
    }

    public static void initializeHolder(GeometryHolder geometryHolder, FilterRepresentation filterRepresentation) {
        Collection<FilterRepresentation> geometryFilters = MasterImage.getImage().getPreset().getGeometryFilters();
        replaceInstances(geometryFilters, filterRepresentation);
        unpackGeometry(geometryHolder, geometryFilters);
    }

    public static Rect finalGeometryRect(int i, int i2, Collection<FilterRepresentation> collection) {
        RectF trueCropRect = getTrueCropRect(unpackGeometry(collection), i, i2);
        Rect rect = new Rect();
        trueCropRect.roundOut(rect);
        return rect;
    }

    private static Bitmap applyFullGeometryMatrix(Bitmap bitmap, GeometryHolder geometryHolder) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        RectF trueCropRect = getTrueCropRect(geometryHolder, width, height);
        Rect rect = new Rect();
        trueCropRect.roundOut(rect);
        if (rect.height() == 0) {
            rect.inset(0, -1);
        }
        if (rect.width() == 0) {
            rect.inset(-1, 0);
        }
        Matrix cropSelectionToScreenMatrix = getCropSelectionToScreenMatrix(null, geometryHolder, width, height, rect.width(), rect.height());
        Bitmap bitmap2 = MasterImage.getImage().getBitmapCache().getBitmap(rect.width(), rect.height(), 7);
        bitmap2.eraseColor(0);
        Canvas canvas = new Canvas(bitmap2);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawBitmap(bitmap, cropSelectionToScreenMatrix, paint);
        return bitmap2;
    }

    public static Matrix getImageToScreenMatrix(Collection<FilterRepresentation> collection, boolean z, Rect rect, float f, float f2) {
        return getOriginalToScreen(unpackGeometry(collection), z, rect.width(), rect.height(), f, f2);
    }

    public static Matrix getOriginalToScreen(GeometryHolder geometryHolder, boolean z, float f, float f2, float f3, float f4) {
        int rotationForOrientation = getRotationForOrientation(MasterImage.getImage().getZoomOrientation());
        FilterRotateRepresentation.Rotation rotation = geometryHolder.rotation;
        geometryHolder.rotation = FilterRotateRepresentation.Rotation.fromValue((rotationForOrientation + rotation.value()) % 360);
        Matrix cropSelectionToScreenMatrix = getCropSelectionToScreenMatrix(null, geometryHolder, (int) f, (int) f2, (int) f3, (int) f4);
        geometryHolder.rotation = rotation;
        return cropSelectionToScreenMatrix;
    }

    public static Bitmap applyGeometryRepresentations(Collection<FilterRepresentation> collection, Bitmap bitmap) {
        GeometryHolder geometryHolderUnpackGeometry = unpackGeometry(collection);
        if (!geometryHolderUnpackGeometry.isNil()) {
            Bitmap bitmapApplyFullGeometryMatrix = applyFullGeometryMatrix(bitmap, geometryHolderUnpackGeometry);
            if (bitmapApplyFullGeometryMatrix == bitmap) {
                return bitmapApplyFullGeometryMatrix;
            }
            MasterImage.getImage().getBitmapCache().cache(bitmap);
            return bitmapApplyFullGeometryMatrix;
        }
        return bitmap;
    }

    public static RectF drawTransformedCropped(GeometryHolder geometryHolder, Canvas canvas, Bitmap bitmap, int i, int i2) {
        if (bitmap == null) {
            return null;
        }
        RectF rectF = new RectF();
        Matrix cropSelectionToScreenMatrix = getCropSelectionToScreenMatrix(rectF, geometryHolder, bitmap.getWidth(), bitmap.getHeight(), i, i2);
        canvas.save();
        canvas.clipRect(rectF);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawBitmap(bitmap, cropSelectionToScreenMatrix, paint);
        canvas.restore();
        return rectF;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$gallery3d$filtershow$filters$FilterRotateRepresentation$Rotation = new int[FilterRotateRepresentation.Rotation.values().length];

        static {
            try {
                $SwitchMap$com$android$gallery3d$filtershow$filters$FilterRotateRepresentation$Rotation[FilterRotateRepresentation.Rotation.NINETY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$gallery3d$filtershow$filters$FilterRotateRepresentation$Rotation[FilterRotateRepresentation.Rotation.TWO_SEVENTY.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public static boolean needsDimensionSwap(FilterRotateRepresentation.Rotation rotation) {
        switch (AnonymousClass1.$SwitchMap$com$android$gallery3d$filtershow$filters$FilterRotateRepresentation$Rotation[rotation.ordinal()]) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    private static Matrix getFullGeometryMatrix(GeometryHolder geometryHolder, int i, int i2) {
        Matrix matrix = new Matrix();
        matrix.setTranslate(-(i / 2.0f), -(i2 / 2.0f));
        matrix.postRotate(geometryHolder.straighten + geometryHolder.rotation.value());
        concatMirrorMatrix(matrix, geometryHolder);
        return matrix;
    }

    public static Matrix getFullGeometryToScreenMatrix(GeometryHolder geometryHolder, int i, int i2, int i3, int i4) {
        int i5;
        int i6;
        if (needsDimensionSwap(geometryHolder.rotation)) {
            i6 = i;
            i5 = i2;
        } else {
            i5 = i;
            i6 = i2;
        }
        float f = i3;
        float f2 = i4;
        float fScale = scale(i5, i6, f, f2) * 0.9f;
        Math.min(f / i, f2 / i2);
        Matrix fullGeometryMatrix = getFullGeometryMatrix(geometryHolder, i, i2);
        fullGeometryMatrix.postScale(fScale, fScale);
        fullGeometryMatrix.postTranslate(f / 2.0f, f2 / 2.0f);
        return fullGeometryMatrix;
    }

    public static RectF getTrueCropRect(GeometryHolder geometryHolder, int i, int i2) {
        RectF rectF = new RectF(geometryHolder.crop);
        FilterCropRepresentation.findScaledCrop(rectF, i, i2);
        float f = geometryHolder.straighten;
        geometryHolder.straighten = 0.0f;
        Matrix fullGeometryMatrix = getFullGeometryMatrix(geometryHolder, i, i2);
        geometryHolder.straighten = f;
        fullGeometryMatrix.mapRect(rectF);
        return rectF;
    }

    public static Matrix getCropSelectionToScreenMatrix(RectF rectF, GeometryHolder geometryHolder, int i, int i2, int i3, int i4) {
        Matrix fullGeometryMatrix = getFullGeometryMatrix(geometryHolder, i, i2);
        RectF trueCropRect = getTrueCropRect(geometryHolder, i, i2);
        float f = i3;
        float f2 = i4;
        float fScale = scale(trueCropRect.width(), trueCropRect.height(), f, f2);
        fullGeometryMatrix.postScale(fScale, fScale);
        scaleRect(trueCropRect, fScale);
        float f3 = f / 2.0f;
        float f4 = f2 / 2.0f;
        fullGeometryMatrix.postTranslate(f3 - trueCropRect.centerX(), f4 - trueCropRect.centerY());
        if (rectF != null) {
            trueCropRect.offset(f3 - trueCropRect.centerX(), f4 - trueCropRect.centerY());
            rectF.set(trueCropRect);
        }
        return fullGeometryMatrix;
    }
}
