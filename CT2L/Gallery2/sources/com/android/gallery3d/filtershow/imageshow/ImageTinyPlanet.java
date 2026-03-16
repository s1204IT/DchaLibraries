package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.android.gallery3d.filtershow.editors.BasicEditor;
import com.android.gallery3d.filtershow.editors.EditorTinyPlanet;
import com.android.gallery3d.filtershow.filters.FilterTinyPlanetRepresentation;

public class ImageTinyPlanet extends ImageShow {
    private float mCenterX;
    private float mCenterY;
    private float mCurrentX;
    private float mCurrentY;
    RectF mDestRect;
    private EditorTinyPlanet mEditorTinyPlanet;
    boolean mInScale;
    private ScaleGestureDetector mScaleGestureDetector;
    ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener;
    private float mStartAngle;
    private FilterTinyPlanetRepresentation mTinyPlanetRep;
    private float mTouchCenterX;
    private float mTouchCenterY;

    public ImageTinyPlanet(Context context) {
        super(context);
        this.mTouchCenterX = 0.0f;
        this.mTouchCenterY = 0.0f;
        this.mCurrentX = 0.0f;
        this.mCurrentY = 0.0f;
        this.mCenterX = 0.0f;
        this.mCenterY = 0.0f;
        this.mStartAngle = 0.0f;
        this.mScaleGestureDetector = null;
        this.mInScale = false;
        this.mDestRect = new RectF();
        this.mScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
            private float mScale = 100.0f;

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                ImageTinyPlanet.this.mInScale = false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                ImageTinyPlanet.this.mInScale = true;
                this.mScale = ImageTinyPlanet.this.mTinyPlanetRep.getValue();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                ImageTinyPlanet.this.mTinyPlanetRep.getValue();
                this.mScale *= detector.getScaleFactor();
                int value = (int) this.mScale;
                ImageTinyPlanet.this.mTinyPlanetRep.setValue(Math.max(ImageTinyPlanet.this.mTinyPlanetRep.getMinimum(), Math.min(ImageTinyPlanet.this.mTinyPlanetRep.getMaximum(), value)));
                ImageTinyPlanet.this.invalidate();
                ImageTinyPlanet.this.mEditorTinyPlanet.commitLocalRepresentation();
                ImageTinyPlanet.this.mEditorTinyPlanet.updateUI();
                return true;
            }
        };
        this.mScaleGestureDetector = new ScaleGestureDetector(context, this.mScaleGestureListener);
    }

    public ImageTinyPlanet(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTouchCenterX = 0.0f;
        this.mTouchCenterY = 0.0f;
        this.mCurrentX = 0.0f;
        this.mCurrentY = 0.0f;
        this.mCenterX = 0.0f;
        this.mCenterY = 0.0f;
        this.mStartAngle = 0.0f;
        this.mScaleGestureDetector = null;
        this.mInScale = false;
        this.mDestRect = new RectF();
        this.mScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
            private float mScale = 100.0f;

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                ImageTinyPlanet.this.mInScale = false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                ImageTinyPlanet.this.mInScale = true;
                this.mScale = ImageTinyPlanet.this.mTinyPlanetRep.getValue();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                ImageTinyPlanet.this.mTinyPlanetRep.getValue();
                this.mScale *= detector.getScaleFactor();
                int value = (int) this.mScale;
                ImageTinyPlanet.this.mTinyPlanetRep.setValue(Math.max(ImageTinyPlanet.this.mTinyPlanetRep.getMinimum(), Math.min(ImageTinyPlanet.this.mTinyPlanetRep.getMaximum(), value)));
                ImageTinyPlanet.this.invalidate();
                ImageTinyPlanet.this.mEditorTinyPlanet.commitLocalRepresentation();
                ImageTinyPlanet.this.mEditorTinyPlanet.updateUI();
                return true;
            }
        };
        this.mScaleGestureDetector = new ScaleGestureDetector(context, this.mScaleGestureListener);
    }

    protected static float angleFor(float dx, float dy) {
        return (float) ((Math.atan2(dx, dy) * 180.0d) / 3.141592653589793d);
    }

    protected float getCurrentTouchAngle() {
        if (this.mCurrentX == this.mTouchCenterX && this.mCurrentY == this.mTouchCenterY) {
            return 0.0f;
        }
        float dX1 = this.mTouchCenterX - this.mCenterX;
        float dY1 = this.mTouchCenterY - this.mCenterY;
        float dX2 = this.mCurrentX - this.mCenterX;
        float dY2 = this.mCurrentY - this.mCenterY;
        float angleA = angleFor(dX1, dY1);
        float angleB = angleFor(dX2, dY2);
        return (float) ((((double) ((angleB - angleA) % 360.0f)) * 3.141592653589793d) / 180.0d);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        this.mCurrentX = x;
        this.mCurrentY = y;
        this.mCenterX = getWidth() / 2;
        this.mCenterY = getHeight() / 2;
        this.mScaleGestureDetector.onTouchEvent(event);
        if (!this.mInScale) {
            switch (event.getActionMasked()) {
                case 0:
                    this.mTouchCenterX = x;
                    this.mTouchCenterY = y;
                    this.mStartAngle = this.mTinyPlanetRep.getAngle();
                    break;
                case 2:
                    this.mTinyPlanetRep.setAngle(this.mStartAngle + getCurrentTouchAngle());
                    break;
            }
            invalidate();
            this.mEditorTinyPlanet.commitLocalRepresentation();
        }
        return true;
    }

    public void setRepresentation(FilterTinyPlanetRepresentation tinyPlanetRep) {
        this.mTinyPlanetRep = tinyPlanetRep;
    }

    public void setEditor(BasicEditor editorTinyPlanet) {
        this.mEditorTinyPlanet = (EditorTinyPlanet) editorTinyPlanet;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Bitmap bitmap = MasterImage.getImage().getHighresImage();
        if (bitmap == null) {
            bitmap = MasterImage.getImage().getFilteredImage();
        }
        if (bitmap != null) {
            display(canvas, bitmap);
        }
    }

    private void display(Canvas canvas, Bitmap bitmap) {
        float sw = canvas.getWidth();
        float sh = canvas.getHeight();
        float iw = bitmap.getWidth();
        float ih = bitmap.getHeight();
        float nsw = sw;
        float nsh = sh;
        if (sw * ih > sh * iw) {
            nsw = (sh * iw) / ih;
        } else {
            nsh = (sw * ih) / iw;
        }
        this.mDestRect.left = (sw - nsw) / 2.0f;
        this.mDestRect.top = (sh - nsh) / 2.0f;
        this.mDestRect.right = sw - this.mDestRect.left;
        this.mDestRect.bottom = sh - this.mDestRect.top;
        canvas.drawBitmap(bitmap, (Rect) null, this.mDestRect, this.mPaint);
    }
}
