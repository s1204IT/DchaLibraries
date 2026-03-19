package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

public class ImageFilterDraw extends ImageFilter {
    private static boolean sIsCacheDisabled;
    private static boolean sIsMirrorChanged = false;
    Bitmap mOverlayBitmap;
    int mCachedStrokes = -1;
    int mCurrentStyle = 0;
    FilterDrawRepresentation mParameters = new FilterDrawRepresentation();
    DrawStyle[] mDrawingsTypes = {new SimpleDraw(0), new SimpleDraw(1), new Brush(R.drawable.brush_gauss), new Brush(R.drawable.brush_marker), new Brush(R.drawable.brush_spatter)};

    public interface DrawStyle {
        void paint(FilterDrawRepresentation.StrokeData strokeData, Canvas canvas, Matrix matrix, int i);

        void setType(byte b);
    }

    public ImageFilterDraw() {
        for (int i = 0; i < this.mDrawingsTypes.length; i++) {
            this.mDrawingsTypes[i].setType((byte) i);
        }
        this.mName = "Image Draw";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterDrawRepresentation();
    }

    @Override
    public void useRepresentation(FilterRepresentation filterRepresentation) {
        this.mParameters = (FilterDrawRepresentation) filterRepresentation;
    }

    class SimpleDraw implements DrawStyle {
        int mMode;
        byte mType;

        public SimpleDraw(int i) {
            this.mMode = i;
        }

        @Override
        public void setType(byte b) {
            this.mType = b;
        }

        @Override
        public void paint(FilterDrawRepresentation.StrokeData strokeData, Canvas canvas, Matrix matrix, int i) {
            if (strokeData == null || strokeData.mPath == null) {
                return;
            }
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            if (this.mMode == 0) {
                paint.setStrokeCap(Paint.Cap.SQUARE);
            } else {
                paint.setStrokeCap(Paint.Cap.ROUND);
            }
            paint.setAntiAlias(true);
            paint.setColor(strokeData.mColor);
            paint.setStrokeWidth(matrix.mapRadius(strokeData.mRadius));
            Path path = new Path();
            path.addPath(strokeData.mPath, matrix);
            canvas.drawPath(path, paint);
        }
    }

    class Brush implements DrawStyle {
        Bitmap mBrush;
        int mBrushID;
        byte mType;

        public Brush(int i) {
            this.mBrushID = i;
        }

        public Bitmap getBrush() {
            if (this.mBrush == null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ALPHA_8;
                this.mBrush = BitmapFactory.decodeResource(MasterImage.getImage().getActivity().getResources(), this.mBrushID, options);
                this.mBrush = this.mBrush.extractAlpha();
            }
            return this.mBrush;
        }

        @Override
        public void paint(FilterDrawRepresentation.StrokeData strokeData, Canvas canvas, Matrix matrix, int i) {
            if (strokeData == null || strokeData.mPath == null) {
                return;
            }
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
            Path path = new Path();
            path.addPath(strokeData.mPath, matrix);
            draw(canvas, paint, strokeData.mColor, matrix.mapRadius(strokeData.mRadius) * 2.0f, path);
        }

        public Bitmap createScaledBitmap(Bitmap bitmap, int i, int i2, boolean z) {
            Matrix matrix = new Matrix();
            matrix.setScale(i / bitmap.getWidth(), i2 / bitmap.getHeight());
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, bitmap.getConfig());
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            Paint paint = new Paint();
            paint.setFilterBitmap(z);
            canvas.drawBitmap(bitmap, matrix, paint);
            return bitmapCreateBitmap;
        }

        void draw(Canvas canvas, Paint paint, int i, float f, Path path) {
            PathMeasure pathMeasure = new PathMeasure();
            float[] fArr = new float[2];
            float[] fArr2 = new float[2];
            pathMeasure.setPath(path, false);
            paint.setAntiAlias(true);
            paint.setColor(i);
            paint.setColorFilter(new PorterDuffColorFilter(i, PorterDuff.Mode.MULTIPLY));
            int i2 = (int) f;
            if (i2 == 0) {
                i2 = 1;
            }
            Bitmap bitmapCreateScaledBitmap = createScaledBitmap(getBrush(), i2, i2, true);
            float length = pathMeasure.getLength();
            float f2 = f / 2.0f;
            float f3 = f2 / 8.0f;
            for (float f4 = 0.0f; f4 < length; f4 += f3) {
                pathMeasure.getPosTan(f4, fArr, fArr2);
                canvas.drawBitmap(bitmapCreateScaledBitmap, fArr[0] - f2, fArr[1] - f2, paint);
            }
        }

        @Override
        public void setType(byte b) {
            this.mType = b;
        }
    }

    void paint(FilterDrawRepresentation.StrokeData strokeData, Canvas canvas, Matrix matrix, int i) {
        this.mDrawingsTypes[strokeData.mType].paint(strokeData, canvas, matrix, i);
    }

    public void drawData(Canvas canvas, Matrix matrix, int i) {
        Paint paint = new Paint();
        if (i == 2) {
            paint.setAntiAlias(true);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(-65536);
        paint.setStrokeWidth(40.0f);
        if (this.mParameters.getDrawing().isEmpty() && this.mParameters.getCurrentDrawing() == null) {
            this.mOverlayBitmap = null;
            this.mCachedStrokes = -1;
            return;
        }
        if (i == 2) {
            Iterator<FilterDrawRepresentation.StrokeData> it = this.mParameters.getDrawing().iterator();
            while (it.hasNext()) {
                paint(it.next(), canvas, matrix, i);
            }
            return;
        }
        if (sIsMirrorChanged || sIsCacheDisabled || this.mOverlayBitmap == null || this.mOverlayBitmap.getWidth() != canvas.getWidth() || this.mOverlayBitmap.getHeight() != canvas.getHeight() || this.mParameters.getDrawing().size() < this.mCachedStrokes) {
            this.mOverlayBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            this.mCachedStrokes = 0;
        }
        if (this.mCachedStrokes < this.mParameters.getDrawing().size()) {
            fillBuffer(matrix);
        }
        canvas.drawBitmap(this.mOverlayBitmap, 0.0f, 0.0f, paint);
        FilterDrawRepresentation.StrokeData currentDrawing = this.mParameters.getCurrentDrawing();
        if (currentDrawing != null) {
            paint(currentDrawing, canvas, matrix, i);
        }
    }

    public void fillBuffer(Matrix matrix) {
        Canvas canvas = new Canvas(this.mOverlayBitmap);
        Vector<FilterDrawRepresentation.StrokeData> drawing = this.mParameters.getDrawing();
        int size = drawing.size();
        for (int i = this.mCachedStrokes; i < size; i++) {
            paint(drawing.get(i), canvas, matrix, 1);
        }
        this.mCachedStrokes = size;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        if (MasterImage.sIsRenderFilters) {
            return applyForFilterRender(bitmap, f, i);
        }
        drawData(new Canvas(bitmap), getOriginalToScreenMatrix(bitmap.getWidth(), bitmap.getHeight()), i);
        return bitmap;
    }

    public Bitmap applyForFilterRender(Bitmap bitmap, float f, int i) {
        drawData(new Canvas(bitmap), getOriginalToScreenMatrixForFilterRender(bitmap.getWidth(), bitmap.getHeight()), i);
        return bitmap;
    }

    private Matrix getOriginalToScreenMatrixForFilterRender(int i, int i2) {
        return getImageToScreenMatrixForFilterRender(getEnvironment().getImagePreset().getGeometryFilters(), true, MasterImage.getImage().getOriginalBounds(), i, i2);
    }

    private static Matrix getImageToScreenMatrixForFilterRender(Collection<FilterRepresentation> collection, boolean z, Rect rect, float f, float f2) {
        GeometryMathUtils.GeometryHolder geometryHolderUnpackGeometry = GeometryMathUtils.unpackGeometry(collection);
        FilterRotateRepresentation.Rotation rotation = geometryHolderUnpackGeometry.rotation;
        FilterMirrorRepresentation.Mirror mirror = geometryHolderUnpackGeometry.mirror;
        geometryHolderUnpackGeometry.rotation = FilterRotateRepresentation.Rotation.fromValue(0);
        geometryHolderUnpackGeometry.mirror = FilterMirrorRepresentation.Mirror.NONE;
        Matrix originalToScreen = GeometryMathUtils.getOriginalToScreen(geometryHolderUnpackGeometry, z, rect.width(), rect.height(), f, f2);
        geometryHolderUnpackGeometry.rotation = rotation;
        geometryHolderUnpackGeometry.mirror = mirror;
        return originalToScreen;
    }

    public static void disableCache(boolean z) {
        sIsCacheDisabled = z;
    }

    public static void mirrorChanged(boolean z) {
        sIsMirrorChanged = z;
    }
}
