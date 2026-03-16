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
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import java.util.Vector;

public class ImageFilterDraw extends ImageFilter {
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
    public void useRepresentation(FilterRepresentation representation) {
        FilterDrawRepresentation parameters = (FilterDrawRepresentation) representation;
        this.mParameters = parameters;
    }

    class SimpleDraw implements DrawStyle {
        int mMode;
        byte mType;

        public SimpleDraw(int mode) {
            this.mMode = mode;
        }

        @Override
        public void setType(byte type) {
            this.mType = type;
        }

        @Override
        public void paint(FilterDrawRepresentation.StrokeData sd, Canvas canvas, Matrix toScrMatrix, int quality) {
            if (sd != null && sd.mPath != null) {
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                if (this.mMode == 0) {
                    paint.setStrokeCap(Paint.Cap.SQUARE);
                } else {
                    paint.setStrokeCap(Paint.Cap.ROUND);
                }
                paint.setAntiAlias(true);
                paint.setColor(sd.mColor);
                paint.setStrokeWidth(toScrMatrix.mapRadius(sd.mRadius));
                Path mCacheTransPath = new Path();
                mCacheTransPath.addPath(sd.mPath, toScrMatrix);
                canvas.drawPath(mCacheTransPath, paint);
            }
        }
    }

    class Brush implements DrawStyle {
        Bitmap mBrush;
        int mBrushID;
        byte mType;

        public Brush(int brushID) {
            this.mBrushID = brushID;
        }

        public Bitmap getBrush() {
            if (this.mBrush == null) {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inPreferredConfig = Bitmap.Config.ALPHA_8;
                this.mBrush = BitmapFactory.decodeResource(MasterImage.getImage().getActivity().getResources(), this.mBrushID, opt);
                this.mBrush = this.mBrush.extractAlpha();
            }
            return this.mBrush;
        }

        @Override
        public void paint(FilterDrawRepresentation.StrokeData sd, Canvas canvas, Matrix toScrMatrix, int quality) {
            if (sd != null && sd.mPath != null) {
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setAntiAlias(true);
                Path mCacheTransPath = new Path();
                mCacheTransPath.addPath(sd.mPath, toScrMatrix);
                draw(canvas, paint, sd.mColor, toScrMatrix.mapRadius(sd.mRadius) * 2.0f, mCacheTransPath);
            }
        }

        public Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
            Matrix m = new Matrix();
            m.setScale(dstWidth / src.getWidth(), dstHeight / src.getHeight());
            Bitmap result = Bitmap.createBitmap(dstWidth, dstHeight, src.getConfig());
            Canvas canvas = new Canvas(result);
            Paint paint = new Paint();
            paint.setFilterBitmap(filter);
            canvas.drawBitmap(src, m, paint);
            return result;
        }

        void draw(Canvas canvas, Paint paint, int color, float size, Path path) {
            PathMeasure mPathMeasure = new PathMeasure();
            float[] mPosition = new float[2];
            float[] mTan = new float[2];
            mPathMeasure.setPath(path, false);
            paint.setAntiAlias(true);
            paint.setColor(color);
            paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
            Bitmap brush = createScaledBitmap(getBrush(), (int) size, (int) size, true);
            float len = mPathMeasure.getLength();
            float s2 = size / 2.0f;
            float step = s2 / 8.0f;
            for (float i = 0.0f; i < len; i += step) {
                mPathMeasure.getPosTan(i, mPosition, mTan);
                canvas.drawBitmap(brush, mPosition[0] - s2, mPosition[1] - s2, paint);
            }
        }

        @Override
        public void setType(byte type) {
            this.mType = type;
        }
    }

    void paint(FilterDrawRepresentation.StrokeData sd, Canvas canvas, Matrix toScrMatrix, int quality) {
        this.mDrawingsTypes[sd.mType].paint(sd, canvas, toScrMatrix, quality);
    }

    public void drawData(Canvas canvas, Matrix originalRotateToScreen, int quality) {
        Paint paint = new Paint();
        if (quality == 2) {
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
        if (quality == 2) {
            for (FilterDrawRepresentation.StrokeData strokeData : this.mParameters.getDrawing()) {
                paint(strokeData, canvas, originalRotateToScreen, quality);
            }
            return;
        }
        if (this.mOverlayBitmap == null || this.mOverlayBitmap.getWidth() != canvas.getWidth() || this.mOverlayBitmap.getHeight() != canvas.getHeight() || this.mParameters.getDrawing().size() < this.mCachedStrokes) {
            this.mOverlayBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            this.mCachedStrokes = 0;
        }
        if (this.mCachedStrokes < this.mParameters.getDrawing().size()) {
            fillBuffer(originalRotateToScreen);
        }
        canvas.drawBitmap(this.mOverlayBitmap, 0.0f, 0.0f, paint);
        FilterDrawRepresentation.StrokeData stroke = this.mParameters.getCurrentDrawing();
        if (stroke != null) {
            paint(stroke, canvas, originalRotateToScreen, quality);
        }
    }

    public void fillBuffer(Matrix originalRotateToScreen) {
        Canvas drawCache = new Canvas(this.mOverlayBitmap);
        Vector<FilterDrawRepresentation.StrokeData> v = this.mParameters.getDrawing();
        int n = v.size();
        for (int i = this.mCachedStrokes; i < n; i++) {
            paint(v.get(i), drawCache, originalRotateToScreen, 1);
        }
        this.mCachedStrokes = n;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix m = getOriginalToScreenMatrix(w, h);
        drawData(new Canvas(bitmap), m, quality);
        return bitmap;
    }
}
