package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.AsyncTask;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.editors.Editor;
import com.android.gallery3d.filtershow.editors.EditorCurves;
import com.android.gallery3d.filtershow.filters.FilterCurvesRepresentation;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.filters.ImageFilterCurves;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import java.util.HashMap;

public class ImageCurves extends ImageShow {
    int[] blueHistogram;
    Path gHistoPath;
    Paint gPaint;
    Path gPathSpline;
    int[] greenHistogram;
    private ControlPoint mCurrentControlPoint;
    private int mCurrentCurveIndex;
    private int mCurrentPick;
    private boolean mDidAddPoint;
    private boolean mDidDelete;
    boolean mDoingTouchMove;
    private EditorCurves mEditorCurves;
    private FilterCurvesRepresentation mFilterCurvesRepresentation;
    HashMap<Integer, String> mIdStrLut;
    private ImagePreset mLastPreset;
    int[] redHistogram;

    public ImageCurves(Context context) {
        super(context);
        this.gPaint = new Paint();
        this.gPathSpline = new Path();
        this.mCurrentCurveIndex = 0;
        this.mDidAddPoint = false;
        this.mDidDelete = false;
        this.mCurrentControlPoint = null;
        this.mCurrentPick = -1;
        this.mLastPreset = null;
        this.redHistogram = new int[256];
        this.greenHistogram = new int[256];
        this.blueHistogram = new int[256];
        this.gHistoPath = new Path();
        this.mDoingTouchMove = false;
        setLayerType(1, this.gPaint);
        resetCurve();
    }

    @Override
    protected boolean enableComparison() {
        return false;
    }

    private void showPopupMenu(LinearLayout linearLayout) {
        final Button button = (Button) linearLayout.findViewById(R.id.applyEffect);
        if (button == null) {
            return;
        }
        if (this.mIdStrLut == null) {
            this.mIdStrLut = new HashMap<>();
            this.mIdStrLut.put(Integer.valueOf(R.id.curve_menu_rgb), getContext().getString(R.string.curves_channel_rgb));
            this.mIdStrLut.put(Integer.valueOf(R.id.curve_menu_red), getContext().getString(R.string.curves_channel_red));
            this.mIdStrLut.put(Integer.valueOf(R.id.curve_menu_green), getContext().getString(R.string.curves_channel_green));
            this.mIdStrLut.put(Integer.valueOf(R.id.curve_menu_blue), getContext().getString(R.string.curves_channel_blue));
        }
        PopupMenu popupMenu = new PopupMenu(getActivity(), button);
        popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_curves, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                ImageCurves.this.setChannel(menuItem.getItemId());
                button.setText(ImageCurves.this.mIdStrLut.get(Integer.valueOf(menuItem.getItemId())));
                return true;
            }
        });
        Editor.hackFixStrings(popupMenu.getMenu());
        popupMenu.show();
        ((FilterShowActivity) getContext()).onShowMenu(popupMenu);
    }

    @Override
    public void openUtilityPanel(final LinearLayout linearLayout) {
        Context context = linearLayout.getContext();
        Button button = (Button) linearLayout.findViewById(R.id.applyEffect);
        button.setText(context.getString(R.string.curves_channel_rgb));
        button.setVisibility(0);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageCurves.this.showPopupMenu(linearLayout);
            }
        });
        if (button != null) {
            button.setVisibility(0);
        }
    }

    private ImageFilterCurves curves() {
        getFilterName();
        if (getImagePreset() != null) {
            return (ImageFilterCurves) FiltersManager.getManager().getFilter(ImageFilterCurves.class);
        }
        return null;
    }

    private Spline getSpline(int i) {
        return this.mFilterCurvesRepresentation.getSpline(i);
    }

    @Override
    public void resetParameter() {
        super.resetParameter();
        resetCurve();
        this.mLastPreset = null;
        invalidate();
    }

    public void resetCurve() {
        if (this.mFilterCurvesRepresentation != null) {
            this.mFilterCurvesRepresentation.reset();
            updateCachedImage();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mFilterCurvesRepresentation == null) {
            return;
        }
        this.gPaint.setAntiAlias(true);
        if (getImagePreset() != this.mLastPreset && getFilteredImage() != null) {
            new ComputeHistogramTask().execute(getFilteredImage());
            this.mLastPreset = getImagePreset();
        }
        if (curves() == null) {
            return;
        }
        if (this.mCurrentCurveIndex == 0 || this.mCurrentCurveIndex == 1) {
            drawHistogram(canvas, this.redHistogram, -65536, PorterDuff.Mode.SCREEN);
        }
        if (this.mCurrentCurveIndex == 0 || this.mCurrentCurveIndex == 2) {
            drawHistogram(canvas, this.greenHistogram, -16711936, PorterDuff.Mode.SCREEN);
        }
        if (this.mCurrentCurveIndex == 0 || this.mCurrentCurveIndex == 3) {
            drawHistogram(canvas, this.blueHistogram, -16776961, PorterDuff.Mode.SCREEN);
        }
        if (this.mCurrentCurveIndex == 0) {
            for (int i = 0; i < 4; i++) {
                Spline spline = getSpline(i);
                if (i != this.mCurrentCurveIndex && !spline.isOriginal()) {
                    spline.draw(canvas, Spline.colorForCurve(i), getWidth(), getHeight(), false, this.mDoingTouchMove);
                }
            }
        }
        getSpline(this.mCurrentCurveIndex).draw(canvas, Spline.colorForCurve(this.mCurrentCurveIndex), getWidth(), getHeight(), true, this.mDoingTouchMove);
    }

    private int pickControlPoint(float f, float f2) {
        Spline spline = getSpline(this.mCurrentCurveIndex);
        int i = 0;
        double dHypot = Math.hypot(spline.getPoint(0).x - f, spline.getPoint(0).y - f2);
        for (int i2 = 1; i2 < spline.getNbPoints(); i2++) {
            double dHypot2 = Math.hypot(spline.getPoint(i2).x - f, spline.getPoint(i2).y - f2);
            if (dHypot2 < dHypot) {
                i = i2;
                dHypot = dHypot2;
            }
        }
        if (!this.mDidAddPoint && dHypot * ((double) getWidth()) > 100.0d && spline.getNbPoints() < 10) {
            return -1;
        }
        return i;
    }

    private String getFilterName() {
        return "Curves";
    }

    @Override
    public synchronized boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mFilterCurvesRepresentation == null) {
            return true;
        }
        if (motionEvent.getPointerCount() != 1) {
            return true;
        }
        if (didFinishScalingOperation()) {
            return true;
        }
        float fCurveHandleSize = Spline.curveHandleSize() / 2;
        float x = motionEvent.getX();
        if (x < fCurveHandleSize) {
            x = fCurveHandleSize;
        }
        float y = motionEvent.getY();
        if (y < fCurveHandleSize) {
            y = fCurveHandleSize;
        }
        if (x > getWidth() - fCurveHandleSize) {
            x = getWidth() - fCurveHandleSize;
        }
        if (y > getHeight() - fCurveHandleSize) {
            y = getHeight() - fCurveHandleSize;
        }
        float f = 2.0f * fCurveHandleSize;
        float width = (x - fCurveHandleSize) / (getWidth() - f);
        float height = (y - fCurveHandleSize) / (getHeight() - f);
        if (motionEvent.getActionMasked() == 1) {
            this.mCurrentControlPoint = null;
            this.mCurrentPick = -1;
            updateCachedImage();
            this.mDidAddPoint = false;
            if (this.mDidDelete) {
                this.mDidDelete = false;
            }
            this.mDoingTouchMove = false;
            return true;
        }
        if (this.mDidDelete) {
            return true;
        }
        if (curves() == null) {
            return true;
        }
        if (motionEvent.getActionMasked() == 2) {
            this.mDoingTouchMove = true;
            Spline spline = getSpline(this.mCurrentCurveIndex);
            int iPickControlPoint = this.mCurrentPick;
            if (this.mCurrentControlPoint == null) {
                iPickControlPoint = pickControlPoint(width, height);
                if (iPickControlPoint == -1) {
                    this.mCurrentControlPoint = new ControlPoint(width, height);
                    iPickControlPoint = spline.addPoint(this.mCurrentControlPoint);
                    this.mDidAddPoint = true;
                } else {
                    this.mCurrentControlPoint = spline.getPoint(iPickControlPoint);
                }
                this.mCurrentPick = iPickControlPoint;
            }
            if (spline.isPointContained(width, iPickControlPoint)) {
                spline.movePoint(iPickControlPoint, width, height);
            } else if (iPickControlPoint != -1 && spline.getNbPoints() > 2) {
                spline.deletePoint(iPickControlPoint);
                this.mDidDelete = true;
            }
            updateCachedImage();
            invalidate();
        }
        return true;
    }

    public synchronized void updateCachedImage() {
        if (getImagePreset() != null) {
            resetImageCaches(this);
            if (this.mEditorCurves != null) {
                this.mEditorCurves.commitLocalRepresentation();
            }
            invalidate();
        }
    }

    class ComputeHistogramTask extends AsyncTask<Bitmap, Void, int[]> {
        ComputeHistogramTask() {
        }

        @Override
        protected int[] doInBackground(Bitmap... bitmapArr) {
            int[] iArr = new int[768];
            Bitmap bitmap = bitmapArr[0];
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] iArr2 = new int[width * height];
            bitmap.getPixels(iArr2, 0, width, 0, 0, width, height);
            for (int i = 0; i < width; i++) {
                for (int i2 = 0; i2 < height; i2++) {
                    int i3 = (i2 * width) + i;
                    int iRed = Color.red(iArr2[i3]);
                    int iGreen = Color.green(iArr2[i3]);
                    int iBlue = Color.blue(iArr2[i3]);
                    iArr[iRed] = iArr[iRed] + 1;
                    int i4 = 256 + iGreen;
                    iArr[i4] = iArr[i4] + 1;
                    int i5 = 512 + iBlue;
                    iArr[i5] = iArr[i5] + 1;
                }
            }
            return iArr;
        }

        @Override
        protected void onPostExecute(int[] iArr) {
            System.arraycopy(iArr, 0, ImageCurves.this.redHistogram, 0, 256);
            System.arraycopy(iArr, 256, ImageCurves.this.greenHistogram, 0, 256);
            System.arraycopy(iArr, 512, ImageCurves.this.blueHistogram, 0, 256);
            ImageCurves.this.invalidate();
        }
    }

    private void drawHistogram(Canvas canvas, int[] iArr, int i, PorterDuff.Mode mode) {
        int i2 = 0;
        for (int i3 = 0; i3 < iArr.length; i3++) {
            if (iArr[i3] > i2) {
                i2 = iArr[i3];
            }
        }
        float width = getWidth() - Spline.curveHandleSize();
        float height = getHeight() - (Spline.curveHandleSize() / 2.0f);
        float fCurveHandleSize = Spline.curveHandleSize() / 2.0f;
        float length = width / iArr.length;
        float f = (0.3f * height) / i2;
        Paint paint = new Paint();
        paint.setARGB(100, 255, 255, 255);
        paint.setStrokeWidth((int) Math.ceil(length));
        Paint paint2 = new Paint();
        paint2.setColor(i);
        paint2.setStrokeWidth(6.0f);
        paint2.setXfermode(new PorterDuffXfermode(mode));
        this.gHistoPath.reset();
        this.gHistoPath.moveTo(fCurveHandleSize, height);
        boolean z = false;
        float f2 = 0.0f;
        float f3 = 0.0f;
        for (int i4 = 0; i4 < iArr.length; i4++) {
            float f4 = (i4 * length) + fCurveHandleSize;
            float f5 = iArr[i4] * f;
            if (f5 != 0.0f) {
                float f6 = height - ((f3 + f5) / 2.0f);
                if (!z) {
                    this.gHistoPath.lineTo(f4, height);
                    z = true;
                }
                this.gHistoPath.lineTo(f4, f6);
                f3 = f5;
                f2 = f4;
            }
        }
        this.gHistoPath.lineTo(f2, height);
        this.gHistoPath.lineTo(width, height);
        this.gHistoPath.close();
        canvas.drawPath(this.gHistoPath, paint2);
        paint2.setStrokeWidth(2.0f);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setARGB(255, 200, 200, 200);
        canvas.drawPath(this.gHistoPath, paint2);
    }

    public void setChannel(int i) {
        switch (i) {
            case R.id.curve_menu_rgb:
                this.mCurrentCurveIndex = 0;
                break;
            case R.id.curve_menu_red:
                this.mCurrentCurveIndex = 1;
                break;
            case R.id.curve_menu_green:
                this.mCurrentCurveIndex = 2;
                break;
            case R.id.curve_menu_blue:
                this.mCurrentCurveIndex = 3;
                break;
        }
        this.mEditorCurves.commitLocalRepresentation();
        invalidate();
    }

    public void setEditor(EditorCurves editorCurves) {
        this.mEditorCurves = editorCurves;
    }

    public void setFilterDrawRepresentation(FilterCurvesRepresentation filterCurvesRepresentation) {
        this.mFilterCurvesRepresentation = filterCurvesRepresentation;
    }
}
