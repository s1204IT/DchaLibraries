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
            public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
                ImageTinyPlanet.this.mInScale = false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                ImageTinyPlanet.this.mInScale = true;
                this.mScale = ImageTinyPlanet.this.mTinyPlanetRep.getValue();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                ImageTinyPlanet.this.mTinyPlanetRep.getValue();
                this.mScale *= scaleGestureDetector.getScaleFactor();
                ImageTinyPlanet.this.mTinyPlanetRep.setValue(Math.max(ImageTinyPlanet.this.mTinyPlanetRep.getMinimum(), Math.min(ImageTinyPlanet.this.mTinyPlanetRep.getMaximum(), (int) this.mScale)));
                ImageTinyPlanet.this.invalidate();
                ImageTinyPlanet.this.mEditorTinyPlanet.commitLocalRepresentation();
                ImageTinyPlanet.this.mEditorTinyPlanet.updateUI();
                return true;
            }
        };
        this.mScaleGestureDetector = new ScaleGestureDetector(context, this.mScaleGestureListener);
    }

    public ImageTinyPlanet(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
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
            public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
                ImageTinyPlanet.this.mInScale = false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                ImageTinyPlanet.this.mInScale = true;
                this.mScale = ImageTinyPlanet.this.mTinyPlanetRep.getValue();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                ImageTinyPlanet.this.mTinyPlanetRep.getValue();
                this.mScale *= scaleGestureDetector.getScaleFactor();
                ImageTinyPlanet.this.mTinyPlanetRep.setValue(Math.max(ImageTinyPlanet.this.mTinyPlanetRep.getMinimum(), Math.min(ImageTinyPlanet.this.mTinyPlanetRep.getMaximum(), (int) this.mScale)));
                ImageTinyPlanet.this.invalidate();
                ImageTinyPlanet.this.mEditorTinyPlanet.commitLocalRepresentation();
                ImageTinyPlanet.this.mEditorTinyPlanet.updateUI();
                return true;
            }
        };
        this.mScaleGestureDetector = new ScaleGestureDetector(context, this.mScaleGestureListener);
    }

    protected static float angleFor(float f, float f2) {
        return (float) ((Math.atan2(f, f2) * 180.0d) / 3.141592653589793d);
    }

    protected float getCurrentTouchAngle() {
        if (this.mCurrentX == this.mTouchCenterX && this.mCurrentY == this.mTouchCenterY) {
            return 0.0f;
        }
        float f = this.mTouchCenterX - this.mCenterX;
        float f2 = this.mTouchCenterY - this.mCenterY;
        float f3 = this.mCurrentX - this.mCenterX;
        float f4 = this.mCurrentY - this.mCenterY;
        return (float) ((((double) ((angleFor(f3, f4) - angleFor(f, f2)) % 360.0f)) * 3.141592653589793d) / 180.0d);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        this.mCurrentX = x;
        this.mCurrentY = y;
        this.mCenterX = getWidth() / 2;
        this.mCenterY = getHeight() / 2;
        this.mScaleGestureDetector.onTouchEvent(motionEvent);
        if (this.mInScale) {
            return true;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.mTouchCenterX = x;
            this.mTouchCenterY = y;
            this.mStartAngle = this.mTinyPlanetRep.getAngle();
        } else if (actionMasked == 2) {
            this.mTinyPlanetRep.setAngle(this.mStartAngle + getCurrentTouchAngle());
        }
        invalidate();
        this.mEditorTinyPlanet.commitLocalRepresentation();
        return true;
    }

    public void setRepresentation(FilterTinyPlanetRepresentation filterTinyPlanetRepresentation) {
        this.mTinyPlanetRep = filterTinyPlanetRepresentation;
    }

    public void setEditor(BasicEditor basicEditor) {
        this.mEditorTinyPlanet = (EditorTinyPlanet) basicEditor;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Bitmap highresImage = MasterImage.getImage().getHighresImage();
        if (highresImage == null) {
            highresImage = MasterImage.getImage().getFilteredImage();
        }
        if (highresImage != null) {
            display(canvas, highresImage);
        }
    }

    private void display(Canvas canvas, Bitmap bitmap) {
        float f;
        float f2;
        float width = canvas.getWidth();
        float height = canvas.getHeight();
        float width2 = bitmap.getWidth();
        float height2 = bitmap.getHeight();
        float f3 = width * height2;
        float f4 = height * width2;
        if (f3 > f4) {
            f2 = f4 / height2;
            f = height;
        } else {
            f = f3 / width2;
            f2 = width;
        }
        this.mDestRect.left = (width - f2) / 2.0f;
        this.mDestRect.top = (height - f) / 2.0f;
        this.mDestRect.right = width - this.mDestRect.left;
        this.mDestRect.bottom = height - this.mDestRect.top;
        canvas.drawBitmap(bitmap, (Rect) null, this.mDestRect, this.mPaint);
    }
}
