package com.android.gallery3d.filtershow.imageshow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.app.FragmentManagerImpl;
import android.support.v4.app.NotificationCompat;
import com.android.gallery3d.filtershow.cache.BitmapCache;
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

        public void set(GeometryHolder h) {
            this.rotation = h.rotation;
            this.straighten = h.straighten;
            this.crop.set(h.crop);
            this.mirror = h.mirror;
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

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GeometryHolder)) {
                return false;
            }
            GeometryHolder h = (GeometryHolder) o;
            return this.rotation == h.rotation && this.straighten == h.straighten && ((this.crop == null && h.crop == null) || (this.crop != null && this.crop.equals(h.crop))) && this.mirror == h.mirror;
        }

        public String toString() {
            return getClass().getSimpleName() + "[rotation:" + this.rotation.value() + ",straighten:" + this.straighten + ",crop:" + this.crop.toString() + ",mirror:" + this.mirror.value() + "]";
        }
    }

    public static float clamp(float i, float low, float high) {
        return Math.max(Math.min(i, high), low);
    }

    public static float[] lineIntersect(float[] line1, float[] line2) {
        float a0 = line1[0];
        float a1 = line1[1];
        float b0 = line1[2];
        float b1 = line1[3];
        float c0 = line2[0];
        float c1 = line2[1];
        float d0 = line2[2];
        float d1 = line2[3];
        float t0 = a0 - b0;
        float t1 = a1 - b1;
        float t2 = b0 - d0;
        float t3 = d1 - b1;
        float t4 = c0 - d0;
        float t5 = c1 - d1;
        float denom = (t1 * t4) - (t0 * t5);
        if (denom == 0.0f) {
            return null;
        }
        float u = ((t3 * t4) + (t5 * t2)) / denom;
        return new float[]{(u * t0) + b0, (u * t1) + b1};
    }

    public static float[] shortestVectorFromPointToLine(float[] point, float[] line) {
        float x1 = line[0];
        float x2 = line[2];
        float y1 = line[1];
        float y2 = line[3];
        float xdelt = x2 - x1;
        float ydelt = y2 - y1;
        if (xdelt == 0.0f && ydelt == 0.0f) {
            return null;
        }
        float u = (((point[0] - x1) * xdelt) + ((point[1] - y1) * ydelt)) / ((xdelt * xdelt) + (ydelt * ydelt));
        float[] ret = {((x2 - x1) * u) + x1, ((y2 - y1) * u) + y1};
        return new float[]{ret[0] - point[0], ret[1] - point[1]};
    }

    public static float dotProduct(float[] a, float[] b) {
        return (a[0] * b[0]) + (a[1] * b[1]);
    }

    public static float[] normalize(float[] a) {
        float length = (float) Math.sqrt((a[0] * a[0]) + (a[1] * a[1]));
        float[] b = {a[0] / length, a[1] / length};
        return b;
    }

    public static float scalarProjection(float[] a, float[] b) {
        float length = (float) Math.sqrt((b[0] * b[0]) + (b[1] * b[1]));
        return dotProduct(a, b) / length;
    }

    public static void scaleRect(RectF r, float scale) {
        r.set(r.left * scale, r.top * scale, r.right * scale, r.bottom * scale);
    }

    public static float vectorLength(float[] a) {
        return (float) Math.sqrt((a[0] * a[0]) + (a[1] * a[1]));
    }

    public static float scale(float oldWidth, float oldHeight, float newWidth, float newHeight) {
        if (oldHeight == 0.0f || oldWidth == 0.0f || (oldWidth == newWidth && oldHeight == newHeight)) {
            return 1.0f;
        }
        return Math.min(newWidth / oldWidth, newHeight / oldHeight);
    }

    private static void concatMirrorMatrix(Matrix m, GeometryHolder holder) {
        FilterMirrorRepresentation.Mirror type = holder.mirror;
        if (type == FilterMirrorRepresentation.Mirror.HORIZONTAL) {
            if (holder.rotation.value() == 90 || holder.rotation.value() == 270) {
                type = FilterMirrorRepresentation.Mirror.VERTICAL;
            }
        } else if (type == FilterMirrorRepresentation.Mirror.VERTICAL && (holder.rotation.value() == 90 || holder.rotation.value() == 270)) {
            type = FilterMirrorRepresentation.Mirror.HORIZONTAL;
        }
        if (type == FilterMirrorRepresentation.Mirror.HORIZONTAL) {
            m.postScale(-1.0f, 1.0f);
            return;
        }
        if (type == FilterMirrorRepresentation.Mirror.VERTICAL) {
            m.postScale(1.0f, -1.0f);
        } else if (type == FilterMirrorRepresentation.Mirror.BOTH) {
            m.postScale(1.0f, -1.0f);
            m.postScale(-1.0f, 1.0f);
        }
    }

    private static int getRotationForOrientation(int orientation) {
        switch (orientation) {
            case 3:
                return 180;
            case 4:
            case 5:
            case 7:
            default:
                return 0;
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                return 90;
            case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                return 270;
        }
    }

    public static GeometryHolder unpackGeometry(Collection<FilterRepresentation> geometry) {
        GeometryHolder holder = new GeometryHolder();
        unpackGeometry(holder, geometry);
        return holder;
    }

    public static void unpackGeometry(GeometryHolder out, Collection<FilterRepresentation> geometry) {
        out.wipe();
        for (FilterRepresentation r : geometry) {
            if (!r.isNil()) {
                if (r.getSerializationName() == "ROTATION") {
                    out.rotation = ((FilterRotateRepresentation) r).getRotation();
                } else if (r.getSerializationName() == "STRAIGHTEN") {
                    out.straighten = ((FilterStraightenRepresentation) r).getStraighten();
                } else if (r.getSerializationName() == "CROP") {
                    ((FilterCropRepresentation) r).getCrop(out.crop);
                } else if (r.getSerializationName() == "MIRROR") {
                    out.mirror = ((FilterMirrorRepresentation) r).getMirror();
                }
            }
        }
    }

    public static void replaceInstances(Collection<FilterRepresentation> geometry, FilterRepresentation rep) {
        Iterator<FilterRepresentation> iter = geometry.iterator();
        while (iter.hasNext()) {
            FilterRepresentation r = iter.next();
            if (ImagePreset.sameSerializationName(rep, r)) {
                iter.remove();
            }
        }
        if (!rep.isNil()) {
            geometry.add(rep);
        }
    }

    public static void initializeHolder(GeometryHolder outHolder, FilterRepresentation currentLocal) {
        Collection<FilterRepresentation> geometry = MasterImage.getImage().getPreset().getGeometryFilters();
        replaceInstances(geometry, currentLocal);
        unpackGeometry(outHolder, geometry);
    }

    public static Rect finalGeometryRect(int width, int height, Collection<FilterRepresentation> geometry) {
        GeometryHolder holder = unpackGeometry(geometry);
        RectF crop = getTrueCropRect(holder, width, height);
        Rect frame = new Rect();
        crop.roundOut(frame);
        return frame;
    }

    private static Bitmap applyFullGeometryMatrix(Bitmap image, GeometryHolder holder) {
        int width = image.getWidth();
        int height = image.getHeight();
        RectF crop = getTrueCropRect(holder, width, height);
        Rect frame = new Rect();
        crop.roundOut(frame);
        Matrix m = getCropSelectionToScreenMatrix(null, holder, width, height, frame.width(), frame.height());
        BitmapCache bitmapCache = MasterImage.getImage().getBitmapCache();
        Bitmap temp = bitmapCache.getBitmap(frame.width(), frame.height(), 7);
        Canvas canvas = new Canvas(temp);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawBitmap(image, m, paint);
        return temp;
    }

    public static Matrix getImageToScreenMatrix(Collection<FilterRepresentation> geometry, boolean reflectRotation, Rect bmapDimens, float viewWidth, float viewHeight) {
        GeometryHolder h = unpackGeometry(geometry);
        return getOriginalToScreen(h, reflectRotation, bmapDimens.width(), bmapDimens.height(), viewWidth, viewHeight);
    }

    public static Matrix getOriginalToScreen(GeometryHolder holder, boolean rotate, float originalWidth, float originalHeight, float viewWidth, float viewHeight) {
        int orientation = MasterImage.getImage().getZoomOrientation();
        int rotation = getRotationForOrientation(orientation);
        FilterRotateRepresentation.Rotation prev = holder.rotation;
        holder.rotation = FilterRotateRepresentation.Rotation.fromValue((prev.value() + rotation) % 360);
        Matrix m = getCropSelectionToScreenMatrix(null, holder, (int) originalWidth, (int) originalHeight, (int) viewWidth, (int) viewHeight);
        holder.rotation = prev;
        return m;
    }

    public static Bitmap applyGeometryRepresentations(Collection<FilterRepresentation> res, Bitmap image) {
        GeometryHolder holder = unpackGeometry(res);
        Bitmap bmap = image;
        if (!holder.isNil() && (bmap = applyFullGeometryMatrix(bmap, holder)) != image) {
            BitmapCache cache = MasterImage.getImage().getBitmapCache();
            cache.cache(image);
        }
        return bmap;
    }

    public static RectF drawTransformedCropped(GeometryHolder holder, Canvas canvas, Bitmap photo, int viewWidth, int viewHeight) {
        if (photo == null) {
            return null;
        }
        RectF crop = new RectF();
        Matrix m = getCropSelectionToScreenMatrix(crop, holder, photo.getWidth(), photo.getHeight(), viewWidth, viewHeight);
        canvas.save();
        canvas.clipRect(crop);
        Paint p = new Paint();
        p.setAntiAlias(true);
        canvas.drawBitmap(photo, m, p);
        canvas.restore();
        return crop;
    }

    public static boolean needsDimensionSwap(FilterRotateRepresentation.Rotation rotation) {
        switch (rotation) {
            case NINETY:
            case TWO_SEVENTY:
                return true;
            default:
                return false;
        }
    }

    private static Matrix getFullGeometryMatrix(GeometryHolder holder, int bitmapWidth, int bitmapHeight) {
        float centerX = bitmapWidth / 2.0f;
        float centerY = bitmapHeight / 2.0f;
        Matrix m = new Matrix();
        m.setTranslate(-centerX, -centerY);
        m.postRotate(holder.straighten + holder.rotation.value());
        concatMirrorMatrix(m, holder);
        return m;
    }

    public static Matrix getFullGeometryToScreenMatrix(GeometryHolder holder, int bitmapWidth, int bitmapHeight, int viewWidth, int viewHeight) {
        int bh = bitmapHeight;
        int bw = bitmapWidth;
        if (needsDimensionSwap(holder.rotation)) {
            bh = bitmapWidth;
            bw = bitmapHeight;
        }
        float scale = scale(bw, bh, viewWidth, viewHeight) * 0.9f;
        Math.min(viewWidth / bitmapWidth, viewHeight / bitmapHeight);
        Matrix m = getFullGeometryMatrix(holder, bitmapWidth, bitmapHeight);
        m.postScale(scale, scale);
        m.postTranslate(viewWidth / 2.0f, viewHeight / 2.0f);
        return m;
    }

    public static RectF getTrueCropRect(GeometryHolder holder, int bitmapWidth, int bitmapHeight) {
        RectF r = new RectF(holder.crop);
        FilterCropRepresentation.findScaledCrop(r, bitmapWidth, bitmapHeight);
        float s = holder.straighten;
        holder.straighten = 0.0f;
        Matrix m1 = getFullGeometryMatrix(holder, bitmapWidth, bitmapHeight);
        holder.straighten = s;
        m1.mapRect(r);
        return r;
    }

    public static Matrix getCropSelectionToScreenMatrix(RectF outCrop, GeometryHolder holder, int bitmapWidth, int bitmapHeight, int viewWidth, int viewHeight) {
        Matrix m = getFullGeometryMatrix(holder, bitmapWidth, bitmapHeight);
        RectF crop = getTrueCropRect(holder, bitmapWidth, bitmapHeight);
        float scale = scale(crop.width(), crop.height(), viewWidth, viewHeight);
        m.postScale(scale, scale);
        scaleRect(crop, scale);
        m.postTranslate((viewWidth / 2.0f) - crop.centerX(), (viewHeight / 2.0f) - crop.centerY());
        if (outCrop != null) {
            crop.offset((viewWidth / 2.0f) - crop.centerX(), (viewHeight / 2.0f) - crop.centerY());
            outCrop.set(crop);
        }
        return m;
    }
}
