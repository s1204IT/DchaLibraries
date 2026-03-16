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
import android.support.v4.app.NotificationCompat;
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
        this.redHistogram = new int[NotificationCompat.FLAG_LOCAL_ONLY];
        this.greenHistogram = new int[NotificationCompat.FLAG_LOCAL_ONLY];
        this.blueHistogram = new int[NotificationCompat.FLAG_LOCAL_ONLY];
        this.gHistoPath = new Path();
        this.mDoingTouchMove = false;
        setLayerType(1, this.gPaint);
        resetCurve();
    }

    @Override
    protected boolean enableComparison() {
        return false;
    }

    private void showPopupMenu(LinearLayout accessoryViewList) {
        final Button button = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        if (button != null) {
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
                public boolean onMenuItemClick(MenuItem item) {
                    ImageCurves.this.setChannel(item.getItemId());
                    button.setText(ImageCurves.this.mIdStrLut.get(Integer.valueOf(item.getItemId())));
                    return true;
                }
            });
            Editor.hackFixStrings(popupMenu.getMenu());
            popupMenu.show();
            ((FilterShowActivity) getContext()).onShowMenu(popupMenu);
        }
    }

    @Override
    public void openUtilityPanel(final LinearLayout accessoryViewList) {
        Context context = accessoryViewList.getContext();
        Button view = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        view.setText(context.getString(R.string.curves_channel_rgb));
        view.setVisibility(0);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ImageCurves.this.showPopupMenu(accessoryViewList);
            }
        });
        if (view != null) {
            view.setVisibility(0);
        }
    }

    private ImageFilterCurves curves() {
        getFilterName();
        ImagePreset p = getImagePreset();
        if (p != null) {
            return (ImageFilterCurves) FiltersManager.getManager().getFilter(ImageFilterCurves.class);
        }
        return null;
    }

    private Spline getSpline(int index) {
        return this.mFilterCurvesRepresentation.getSpline(index);
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
        if (this.mFilterCurvesRepresentation != null) {
            this.gPaint.setAntiAlias(true);
            if (getImagePreset() != this.mLastPreset && getFilteredImage() != null) {
                new ComputeHistogramTask().execute(getFilteredImage());
                this.mLastPreset = getImagePreset();
            }
            if (curves() != null) {
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
        }
    }

    private int pickControlPoint(float x, float y) {
        int pick = 0;
        Spline spline = getSpline(this.mCurrentCurveIndex);
        float px = spline.getPoint(0).x;
        float py = spline.getPoint(0).y;
        double delta = Math.sqrt(((px - x) * (px - x)) + ((py - y) * (py - y)));
        for (int i = 1; i < spline.getNbPoints(); i++) {
            float px2 = spline.getPoint(i).x;
            float py2 = spline.getPoint(i).y;
            double currentDelta = Math.sqrt(((px2 - x) * (px2 - x)) + ((py2 - y) * (py2 - y)));
            if (currentDelta < delta) {
                delta = currentDelta;
                pick = i;
            }
        }
        if (!this.mDidAddPoint && ((double) getWidth()) * delta > 100.0d && spline.getNbPoints() < 10) {
            return -1;
        }
        return pick;
    }

    private String getFilterName() {
        return "Curves";
    }

    @Override
    public synchronized boolean onTouchEvent(MotionEvent e) {
        if (e.getPointerCount() == 1 && !didFinishScalingOperation()) {
            float margin = Spline.curveHandleSize() / 2;
            float posX = e.getX();
            if (posX < margin) {
                posX = margin;
            }
            float posY = e.getY();
            if (posY < margin) {
                posY = margin;
            }
            if (posX > getWidth() - margin) {
                posX = getWidth() - margin;
            }
            if (posY > getHeight() - margin) {
                posY = getHeight() - margin;
            }
            float posX2 = (posX - margin) / (getWidth() - (2.0f * margin));
            float posY2 = (posY - margin) / (getHeight() - (2.0f * margin));
            if (e.getActionMasked() == 1) {
                this.mCurrentControlPoint = null;
                this.mCurrentPick = -1;
                updateCachedImage();
                this.mDidAddPoint = false;
                if (this.mDidDelete) {
                    this.mDidDelete = false;
                }
                this.mDoingTouchMove = false;
            } else if (!this.mDidDelete && curves() != null && e.getActionMasked() == 2) {
                this.mDoingTouchMove = true;
                Spline spline = getSpline(this.mCurrentCurveIndex);
                int pick = this.mCurrentPick;
                if (this.mCurrentControlPoint == null) {
                    pick = pickControlPoint(posX2, posY2);
                    if (pick == -1) {
                        this.mCurrentControlPoint = new ControlPoint(posX2, posY2);
                        pick = spline.addPoint(this.mCurrentControlPoint);
                        this.mDidAddPoint = true;
                    } else {
                        this.mCurrentControlPoint = spline.getPoint(pick);
                    }
                    this.mCurrentPick = pick;
                }
                if (spline.isPointContained(posX2, pick)) {
                    spline.movePoint(pick, posX2, posY2);
                } else if (pick != -1 && spline.getNbPoints() > 2) {
                    spline.deletePoint(pick);
                    this.mDidDelete = true;
                }
                updateCachedImage();
                invalidate();
            }
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
        protected int[] doInBackground(Bitmap... params) {
            int[] histo = new int[768];
            Bitmap bitmap = params[0];
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    int index = (j * w) + i;
                    int r = Color.red(pixels[index]);
                    int g = Color.green(pixels[index]);
                    int b = Color.blue(pixels[index]);
                    histo[r] = histo[r] + 1;
                    int i2 = g + NotificationCompat.FLAG_LOCAL_ONLY;
                    histo[i2] = histo[i2] + 1;
                    int i3 = b + NotificationCompat.FLAG_GROUP_SUMMARY;
                    histo[i3] = histo[i3] + 1;
                }
            }
            return histo;
        }

        @Override
        protected void onPostExecute(int[] result) {
            System.arraycopy(result, 0, ImageCurves.this.redHistogram, 0, NotificationCompat.FLAG_LOCAL_ONLY);
            System.arraycopy(result, NotificationCompat.FLAG_LOCAL_ONLY, ImageCurves.this.greenHistogram, 0, NotificationCompat.FLAG_LOCAL_ONLY);
            System.arraycopy(result, NotificationCompat.FLAG_GROUP_SUMMARY, ImageCurves.this.blueHistogram, 0, NotificationCompat.FLAG_LOCAL_ONLY);
            ImageCurves.this.invalidate();
        }
    }

    private void drawHistogram(Canvas canvas, int[] histogram, int color, PorterDuff.Mode mode) {
        int max = 0;
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > max) {
                max = histogram[i];
            }
        }
        float w = getWidth() - Spline.curveHandleSize();
        float h = getHeight() - (Spline.curveHandleSize() / 2.0f);
        float dx = Spline.curveHandleSize() / 2.0f;
        float wl = w / histogram.length;
        float wh = (0.3f * h) / max;
        Paint paint = new Paint();
        paint.setARGB(100, 255, 255, 255);
        paint.setStrokeWidth((int) Math.ceil(wl));
        Paint paint2 = new Paint();
        paint2.setColor(color);
        paint2.setStrokeWidth(6.0f);
        paint2.setXfermode(new PorterDuffXfermode(mode));
        this.gHistoPath.reset();
        this.gHistoPath.moveTo(dx, h);
        boolean firstPointEncountered = false;
        float prev = 0.0f;
        float last = 0.0f;
        for (int i2 = 0; i2 < histogram.length; i2++) {
            float x = (i2 * wl) + dx;
            float l = histogram[i2] * wh;
            if (l != 0.0f) {
                float v = h - ((l + prev) / 2.0f);
                if (!firstPointEncountered) {
                    this.gHistoPath.lineTo(x, h);
                    firstPointEncountered = true;
                }
                this.gHistoPath.lineTo(x, v);
                prev = l;
                last = x;
            }
        }
        this.gHistoPath.lineTo(last, h);
        this.gHistoPath.lineTo(w, h);
        this.gHistoPath.close();
        canvas.drawPath(this.gHistoPath, paint2);
        paint2.setStrokeWidth(2.0f);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setARGB(255, 200, 200, 200);
        canvas.drawPath(this.gHistoPath, paint2);
    }

    public void setChannel(int itemId) {
        switch (itemId) {
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

    public void setFilterDrawRepresentation(FilterCurvesRepresentation drawRep) {
        this.mFilterCurvesRepresentation = drawRep;
    }
}
