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

    public StateView(Context context, int type) {
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
        this.mType = type;
        Resources res = getResources();
        this.mEndsBackgroundColor = res.getColor(R.color.filtershow_stateview_end_background);
        this.mEndsTextColor = res.getColor(R.color.filtershow_stateview_end_text);
        this.mBackgroundColor = res.getColor(R.color.filtershow_stateview_background);
        this.mTextColor = res.getColor(R.color.filtershow_stateview_text);
        this.mSelectedBackgroundColor = res.getColor(R.color.filtershow_stateview_selected_background);
        this.mSelectedTextColor = res.getColor(R.color.filtershow_stateview_selected_text);
        this.mTextSize = res.getDimensionPixelSize(R.dimen.state_panel_text_size);
    }

    public void setType(int type) {
        this.mType = type;
        invalidate();
    }

    @Override
    public void setSelected(boolean value) {
        super.setSelected(value);
        if (!value) {
            this.mDuplicateButton = false;
        }
        invalidate();
    }

    public void drawText(Canvas canvas) {
        if (this.mText != null) {
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
            int x = (canvas.getWidth() - this.mTextBounds.width()) / 2;
            int y = this.mTextBounds.height() + ((canvas.getHeight() - this.mTextBounds.height()) / 2);
            canvas.drawText(this.mText, x, y, this.mPaint);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawARGB(0, 0, 0, 0);
        this.mPaint.reset();
        this.mPath.reset();
        float w = canvas.getWidth();
        float h = canvas.getHeight();
        float r = sArrowHeight;
        float d = sArrowWidth;
        if (this.mOrientation == 0) {
            drawHorizontalPath(w, h, r, d);
        } else if (this.mDirection == DOWN) {
            drawVerticalDownPath(w, h, r, d);
        } else {
            drawVerticalPath(w, h, r, d);
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

    private void drawHorizontalPath(float w, float h, float r, float d) {
        if (getLayoutDirection() == 1) {
            this.mPath.moveTo(w, 0.0f);
            if (this.mType == END) {
                this.mPath.lineTo(0.0f, 0.0f);
                this.mPath.lineTo(0.0f, h);
            } else {
                this.mPath.lineTo(d, 0.0f);
                this.mPath.lineTo(d, r);
                this.mPath.lineTo(0.0f, r + d);
                this.mPath.lineTo(d, r + d + r);
                this.mPath.lineTo(d, h);
            }
            this.mPath.lineTo(w, h);
            if (this.mType != BEGIN) {
                this.mPath.lineTo(w, r + d + r);
                this.mPath.lineTo(w - d, r + d);
                this.mPath.lineTo(w, r);
            }
        } else {
            this.mPath.moveTo(0.0f, 0.0f);
            if (this.mType == END) {
                this.mPath.lineTo(w, 0.0f);
                this.mPath.lineTo(w, h);
            } else {
                this.mPath.lineTo(w - d, 0.0f);
                this.mPath.lineTo(w - d, r);
                this.mPath.lineTo(w, r + d);
                this.mPath.lineTo(w - d, r + d + r);
                this.mPath.lineTo(w - d, h);
            }
            this.mPath.lineTo(0.0f, h);
            if (this.mType != BEGIN) {
                this.mPath.lineTo(0.0f, r + d + r);
                this.mPath.lineTo(d, r + d);
                this.mPath.lineTo(0.0f, r);
            }
        }
        this.mPath.close();
    }

    private void drawVerticalPath(float w, float h, float r, float d) {
        if (this.mType == BEGIN) {
            this.mPath.moveTo(0.0f, 0.0f);
            this.mPath.lineTo(w, 0.0f);
        } else {
            this.mPath.moveTo(0.0f, d);
            this.mPath.lineTo(r, d);
            this.mPath.lineTo(r + d, 0.0f);
            this.mPath.lineTo(r + d + r, d);
            this.mPath.lineTo(w, d);
        }
        this.mPath.lineTo(w, h);
        if (this.mType != END) {
            this.mPath.lineTo(r + d + r, h);
            this.mPath.lineTo(r + d, h - d);
            this.mPath.lineTo(r, h);
        }
        this.mPath.lineTo(0.0f, h);
        this.mPath.close();
    }

    private void drawVerticalDownPath(float w, float h, float r, float d) {
        this.mPath.moveTo(0.0f, 0.0f);
        if (this.mType != BEGIN) {
            this.mPath.lineTo(r, 0.0f);
            this.mPath.lineTo(r + d, d);
            this.mPath.lineTo(r + d + r, 0.0f);
        }
        this.mPath.lineTo(w, 0.0f);
        if (this.mType != END) {
            this.mPath.lineTo(w, h - d);
            this.mPath.lineTo(r + d + r, h - d);
            this.mPath.lineTo(r + d, h);
            this.mPath.lineTo(r, h - d);
            this.mPath.lineTo(0.0f, h - d);
        } else {
            this.mPath.lineTo(w, h);
            this.mPath.lineTo(0.0f, h);
        }
        this.mPath.close();
    }

    public void setBackgroundAlpha(float alpha) {
        if (this.mType != BEGIN) {
            this.mAlpha = alpha;
            setAlpha(alpha);
            invalidate();
        }
    }

    public float getBackgroundAlpha() {
        return this.mAlpha;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
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
        FilterShowActivity activity = (FilterShowActivity) getContext();
        FilterRepresentation representation = getState().getFilterRepresentation();
        activity.removeFilterRepresentation(representation);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        FilterShowActivity activity = (FilterShowActivity) getContext();
        if (event.getActionMasked() == 1) {
            activity.startTouchAnimation(this, event.getX(), event.getY());
        }
        if (event.getActionMasked() == 0) {
            this.mStartTouchY = event.getY();
            this.mStartTouchX = event.getX();
            if (this.mType == BEGIN) {
                MasterImage.getImage().setShowsOriginal(true);
            }
        }
        if (event.getActionMasked() == 1 || event.getActionMasked() == 3) {
            setTranslationX(0.0f);
            setTranslationY(0.0f);
            MasterImage.getImage().setShowsOriginal(false);
            if (this.mType != BEGIN && event.getActionMasked() == 1) {
                setSelected(true);
                FilterRepresentation representation = getState().getFilterRepresentation();
                MasterImage image = MasterImage.getImage();
                ImagePreset preset = image != null ? image.getCurrentPreset() : null;
                if (getTranslationY() == 0.0f && image != null && preset != null && representation != image.getCurrentFilterRepresentation() && preset.getRepresentation(representation) != null) {
                    activity.showRepresentation(representation);
                    setSelected(false);
                }
            }
        }
        if (this.mType != BEGIN && event.getActionMasked() == 2) {
            float delta = event.getY() - this.mStartTouchY;
            if (Math.abs(delta) > this.mDeleteSlope) {
                activity.setHandlesSwipeForView(this, this.mStartTouchX, this.mStartTouchY);
            }
        }
        return true;
    }
}
