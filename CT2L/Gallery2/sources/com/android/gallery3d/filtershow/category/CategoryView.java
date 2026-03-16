package com.android.gallery3d.filtershow.category;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.ui.SelectionRenderer;

public class CategoryView extends IconView implements View.OnClickListener, SwipableView {
    private Action mAction;
    CategoryAdapter mAdapter;
    private Paint mBorderPaint;
    private int mBorderStroke;
    private boolean mCanBeRemoved;
    private float mDeleteSlope;
    private long mDoubleActionLast;
    private long mDoubleTapDelay;
    private Paint mPaint;
    private Paint mSelectPaint;
    private int mSelectionColor;
    private int mSelectionStroke;
    private int mSpacerColor;
    private float mStartTouchX;
    private float mStartTouchY;

    public CategoryView(Context context) {
        super(context);
        this.mPaint = new Paint();
        this.mStartTouchX = 0.0f;
        this.mStartTouchY = 0.0f;
        this.mDeleteSlope = 20.0f;
        this.mSelectionColor = -1;
        this.mSpacerColor = -1;
        this.mCanBeRemoved = false;
        this.mDoubleActionLast = 0L;
        this.mDoubleTapDelay = 250L;
        setOnClickListener(this);
        Resources res = getResources();
        this.mSelectionStroke = res.getDimensionPixelSize(R.dimen.thumbnail_margin);
        this.mSelectPaint = new Paint();
        this.mSelectPaint.setStyle(Paint.Style.FILL);
        this.mSelectionColor = res.getColor(R.color.filtershow_category_selection);
        this.mSpacerColor = res.getColor(R.color.filtershow_categoryview_text);
        this.mSelectPaint.setColor(this.mSelectionColor);
        this.mBorderPaint = new Paint(this.mSelectPaint);
        this.mBorderPaint.setColor(-16777216);
        this.mBorderStroke = this.mSelectionStroke / 3;
    }

    @Override
    public boolean isHalfImage() {
        if (this.mAction == null) {
            return false;
        }
        return this.mAction.getType() == 1 || this.mAction.getType() == 2;
    }

    private boolean canBeRemoved() {
        return this.mCanBeRemoved;
    }

    private void drawSpacer(Canvas canvas) {
        this.mPaint.reset();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setColor(this.mSpacerColor);
        if (getOrientation() == 0) {
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, getHeight() / 5, this.mPaint);
        } else {
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 5, this.mPaint);
        }
    }

    @Override
    public boolean needsCenterText() {
        if (this.mAction == null || this.mAction.getType() != 2) {
            return super.needsCenterText();
        }
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.mAction != null) {
            if (this.mAction.getType() == 3) {
                drawSpacer(canvas);
                return;
            } else if (!this.mAction.isDoubleAction()) {
                this.mAction.setImageFrame(new Rect(0, 0, getWidth(), getHeight()), getOrientation());
                if (this.mAction.getImage() != null) {
                    setBitmap(this.mAction.getImage());
                }
            } else {
                return;
            }
        }
        super.onDraw(canvas);
        if (this.mAdapter.isSelected(this)) {
            SelectionRenderer.drawSelection(canvas, 0, 0, getWidth(), getHeight(), this.mSelectionStroke, this.mSelectPaint, this.mBorderStroke, this.mBorderPaint);
        }
    }

    public void setAction(Action action, CategoryAdapter adapter) {
        this.mAction = action;
        setText(this.mAction.getName());
        this.mAdapter = adapter;
        this.mCanBeRemoved = action.canBeRemoved();
        setUseOnlyDrawable(false);
        if (this.mAction.getType() == 2) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.filtershow_add);
            setBitmap(bitmap);
            setUseOnlyDrawable(true);
            setText(getResources().getString(R.string.filtershow_add_button_looks));
        } else {
            setBitmap(this.mAction.getImage());
        }
        invalidate();
    }

    @Override
    public void onClick(View view) {
        FilterShowActivity activity = (FilterShowActivity) getContext();
        if (this.mAction.getType() == 2) {
            activity.addNewPreset();
            return;
        }
        if (this.mAction.getType() != 3) {
            if (this.mAction.isDoubleAction()) {
                long current = System.currentTimeMillis() - this.mDoubleActionLast;
                if (current < this.mDoubleTapDelay) {
                    activity.showRepresentation(this.mAction.getRepresentation());
                }
                this.mDoubleActionLast = System.currentTimeMillis();
            } else {
                activity.showRepresentation(this.mAction.getRepresentation());
            }
            this.mAdapter.setSelected(this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = super.onTouchEvent(event);
        FilterShowActivity activity = (FilterShowActivity) getContext();
        if (event.getActionMasked() == 1) {
            activity.startTouchAnimation(this, event.getX(), event.getY());
        }
        if (canBeRemoved()) {
            if (event.getActionMasked() == 0) {
                this.mStartTouchY = event.getY();
                this.mStartTouchX = event.getX();
            }
            if (event.getActionMasked() == 1) {
                setTranslationX(0.0f);
                setTranslationY(0.0f);
            }
            if (event.getActionMasked() == 2) {
                float delta = event.getY() - this.mStartTouchY;
                if (getOrientation() == 0) {
                    delta = event.getX() - this.mStartTouchX;
                }
                if (Math.abs(delta) > this.mDeleteSlope) {
                    activity.setHandlesSwipeForView(this, this.mStartTouchX, this.mStartTouchY);
                }
            }
            return true;
        }
        return ret;
    }

    @Override
    public void delete() {
        this.mAdapter.remove(this.mAction);
    }
}
