package com.android.gallery3d.filtershow.state;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.category.SwipableView;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

public class StateView extends View implements SwipableView {
    private float mAlpha;
    private int mBackgroundColor;
    private float mDeleteSlope;
    private int mDirection;
    private boolean mDuplicateButton;
    private int mEndsBackgroundColor;
    private int mEndsTextColor;
    private int mOrientation;
    private Paint mPaint;
    private Path mPath;
    private int mSelectedBackgroundColor;
    private int mSelectedTextColor;
    private float mStartTouchX;
    private float mStartTouchY;
    private State mState;
    private String mText;
    private Rect mTextBounds;
    private int mTextColor;
    private float mTextSize;
    private int mType;
    public static int DEFAULT = 0;
    public static int BEGIN = 1;
    public static int END = 2;
    public static int UP = 1;
    public static int DOWN = 2;
    public static int LEFT = 3;
    public static int RIGHT = 4;
    private static int sMargin = 16;
    private static int sArrowHeight = 16;
    private static int sArrowWidth = 8;

    public StateView(Context context) {
        this(context, DEFAULT);
    }

    public StateView(Context context, int i) {
        super(context);
        this.mPath = new Path();
        this.mPaint = new Paint();
        this.mType = DEFAULT;
        this.mAlpha = 1.0f;
        this.mText = "Default";
        this.mTextSize = 32.0f;
        this.mStartTouchX = 0.0f;
        this.mStartTouchY = 0.0f;
        this.mDeleteSlope = 20.0f;
        this.mOrientation = 1;
        this.mDirection = DOWN;
        this.mTextBounds = new Rect();
        this.mType = i;
        Resources resources = getResources();
        this.mEndsBackgroundColor = resources.getColor(R.color.filtershow_stateview_end_background);
        this.mEndsTextColor = resources.getColor(R.color.filtershow_stateview_end_text);
        this.mBackgroundColor = resources.getColor(R.color.filtershow_stateview_background);
        this.mTextColor = resources.getColor(R.color.filtershow_stateview_text);
        this.mSelectedBackgroundColor = resources.getColor(R.color.filtershow_stateview_selected_background);
        this.mSelectedTextColor = resources.getColor(R.color.filtershow_stateview_selected_text);
        this.mTextSize = resources.getDimensionPixelSize(R.dimen.state_panel_text_size);
    }

    public void setType(int i) {
        this.mType = i;
        invalidate();
    }

    @Override
    public void setSelected(boolean z) {
        super.setSelected(z);
        if (!z) {
            this.mDuplicateButton = false;
        }
        invalidate();
    }

    public void drawText(Canvas canvas) {
        if (this.mText == null) {
            return;
        }
        this.mPaint.reset();
        if (isSelected()) {
            this.mPaint.setColor(this.mSelectedTextColor);
        } else {
            this.mPaint.setColor(this.mTextColor);
        }
        if (this.mType == BEGIN) {
            this.mPaint.setColor(this.mEndsTextColor);
        }
        this.mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setTextSize(this.mTextSize);
        this.mPaint.getTextBounds(this.mText, 0, this.mText.length(), this.mTextBounds);
        canvas.drawText(this.mText, (canvas.getWidth() - this.mTextBounds.width()) / 2, this.mTextBounds.height() + ((canvas.getHeight() - this.mTextBounds.height()) / 2), this.mPaint);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawARGB(0, 0, 0, 0);
        this.mPaint.reset();
        this.mPath.reset();
        float width = canvas.getWidth();
        float height = canvas.getHeight();
        float f = sArrowHeight;
        float f2 = sArrowWidth;
        if (this.mOrientation == 0) {
            drawHorizontalPath(width, height, f, f2);
        } else if (this.mDirection == DOWN) {
            drawVerticalDownPath(width, height, f, f2);
        } else {
            drawVerticalPath(width, height, f, f2);
        }
        if (this.mType == DEFAULT || this.mType == END) {
            if (this.mDuplicateButton) {
                this.mPaint.setARGB(255, 200, 0, 0);
            } else if (isSelected()) {
                this.mPaint.setColor(this.mSelectedBackgroundColor);
            } else {
                this.mPaint.setColor(this.mBackgroundColor);
            }
        } else {
            this.mPaint.setColor(this.mEndsBackgroundColor);
        }
        canvas.drawPath(this.mPath, this.mPaint);
        drawText(canvas);
    }

    private void drawHorizontalPath(float f, float f2, float f3, float f4) {
        if (getLayoutDirection() == 1) {
            this.mPath.moveTo(f, 0.0f);
            if (this.mType == END) {
                this.mPath.lineTo(0.0f, 0.0f);
                this.mPath.lineTo(0.0f, f2);
            } else {
                this.mPath.lineTo(f4, 0.0f);
                this.mPath.lineTo(f4, f3);
                float f5 = f3 + f4;
                this.mPath.lineTo(0.0f, f5);
                this.mPath.lineTo(f4, f5 + f3);
                this.mPath.lineTo(f4, f2);
            }
            this.mPath.lineTo(f, f2);
            if (this.mType != BEGIN) {
                float f6 = f3 + f4;
                this.mPath.lineTo(f, f6 + f3);
                this.mPath.lineTo(f - f4, f6);
                this.mPath.lineTo(f, f3);
            }
        } else {
            this.mPath.moveTo(0.0f, 0.0f);
            if (this.mType == END) {
                this.mPath.lineTo(f, 0.0f);
                this.mPath.lineTo(f, f2);
            } else {
                float f7 = f - f4;
                this.mPath.lineTo(f7, 0.0f);
                this.mPath.lineTo(f7, f3);
                float f8 = f3 + f4;
                this.mPath.lineTo(f, f8);
                this.mPath.lineTo(f7, f8 + f3);
                this.mPath.lineTo(f7, f2);
            }
            this.mPath.lineTo(0.0f, f2);
            if (this.mType != BEGIN) {
                float f9 = f3 + f4;
                this.mPath.lineTo(0.0f, f9 + f3);
                this.mPath.lineTo(f4, f9);
                this.mPath.lineTo(0.0f, f3);
            }
        }
        this.mPath.close();
    }

    private void drawVerticalPath(float f, float f2, float f3, float f4) {
        if (this.mType == BEGIN) {
            this.mPath.moveTo(0.0f, 0.0f);
            this.mPath.lineTo(f, 0.0f);
        } else {
            this.mPath.moveTo(0.0f, f4);
            this.mPath.lineTo(f3, f4);
            float f5 = f3 + f4;
            this.mPath.lineTo(f5, 0.0f);
            this.mPath.lineTo(f5 + f3, f4);
            this.mPath.lineTo(f, f4);
        }
        this.mPath.lineTo(f, f2);
        if (this.mType != END) {
            float f6 = f3 + f4;
            this.mPath.lineTo(f6 + f3, f2);
            this.mPath.lineTo(f6, f2 - f4);
            this.mPath.lineTo(f3, f2);
        }
        this.mPath.lineTo(0.0f, f2);
        this.mPath.close();
    }

    private void drawVerticalDownPath(float f, float f2, float f3, float f4) {
        this.mPath.moveTo(0.0f, 0.0f);
        if (this.mType != BEGIN) {
            this.mPath.lineTo(f3, 0.0f);
            float f5 = f3 + f4;
            this.mPath.lineTo(f5, f4);
            this.mPath.lineTo(f5 + f3, 0.0f);
        }
        this.mPath.lineTo(f, 0.0f);
        if (this.mType != END) {
            float f6 = f2 - f4;
            this.mPath.lineTo(f, f6);
            float f7 = f4 + f3;
            this.mPath.lineTo(f7 + f3, f6);
            this.mPath.lineTo(f7, f2);
            this.mPath.lineTo(f3, f6);
            this.mPath.lineTo(0.0f, f6);
        } else {
            this.mPath.lineTo(f, f2);
            this.mPath.lineTo(0.0f, f2);
        }
        this.mPath.close();
    }

    public void setBackgroundAlpha(float f) {
        if (this.mType == BEGIN) {
            return;
        }
        this.mAlpha = f;
        setAlpha(f);
        invalidate();
    }

    public float getBackgroundAlpha() {
        return this.mAlpha;
    }

    public void setOrientation(int i) {
        this.mOrientation = i;
    }

    public State getState() {
        return this.mState;
    }

    public void setState(State state) {
        this.mState = state;
        this.mText = this.mState.getText().toUpperCase();
        this.mType = this.mState.getType();
        invalidate();
    }

    public void resetPosition() {
        setTranslationX(0.0f);
        setTranslationY(0.0f);
        setBackgroundAlpha(1.0f);
    }

    @Override
    public void delete() {
        ((FilterShowActivity) getContext()).removeFilterRepresentation(getState().getFilterRepresentation());
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        FilterShowActivity filterShowActivity = (FilterShowActivity) getContext();
        if (motionEvent.getActionMasked() == 1) {
            filterShowActivity.startTouchAnimation(this, motionEvent.getX(), motionEvent.getY());
        }
        if (motionEvent.getActionMasked() == 0) {
            this.mStartTouchY = motionEvent.getY();
            this.mStartTouchX = motionEvent.getX();
            if (this.mType == BEGIN) {
                MasterImage.getImage().setShowsOriginal(true);
            }
        }
        if (motionEvent.getActionMasked() == 1 || motionEvent.getActionMasked() == 3) {
            setTranslationX(0.0f);
            setTranslationY(0.0f);
            MasterImage.getImage().setShowsOriginal(false);
            if (this.mType != BEGIN && motionEvent.getActionMasked() == 1) {
                setSelected(true);
                FilterRepresentation filterRepresentation = getState().getFilterRepresentation();
                MasterImage image = MasterImage.getImage();
                ImagePreset currentPreset = image != null ? image.getCurrentPreset() : null;
                if (getTranslationY() == 0.0f && image != null && currentPreset != null && filterRepresentation != image.getCurrentFilterRepresentation() && currentPreset.getRepresentation(filterRepresentation) != null) {
                    filterShowActivity.showRepresentation(filterRepresentation);
                    setSelected(false);
                }
            }
        }
        if (this.mType != BEGIN && motionEvent.getActionMasked() == 2 && Math.abs(motionEvent.getY() - this.mStartTouchY) > this.mDeleteSlope) {
            filterShowActivity.setHandlesSwipeForView(this, this.mStartTouchX, this.mStartTouchY);
        }
        return true;
    }
}
