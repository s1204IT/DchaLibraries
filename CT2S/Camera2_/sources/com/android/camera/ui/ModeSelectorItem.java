package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.camera.util.ApiHelper;
import com.android.camera2.R;

class ModeSelectorItem extends FrameLayout {
    private ModeIconView mIcon;
    private VisibleWidthChangedListener mListener;
    private final int mMinVisibleWidth;
    private int mModeId;
    private TextView mText;
    private int mVisibleWidth;
    private int mWidth;

    public interface VisibleWidthChangedListener {
        void onVisibleWidthChanged(int i);
    }

    public ModeSelectorItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mVisibleWidth = 0;
        this.mListener = null;
        setWillNotDraw(false);
        setClickable(true);
        this.mMinVisibleWidth = getResources().getDimensionPixelSize(R.dimen.mode_selector_icon_block_width);
    }

    @Override
    public void onFinishInflate() {
        Typeface typeface;
        this.mIcon = (ModeIconView) findViewById(R.id.selector_icon);
        this.mText = (TextView) findViewById(R.id.selector_text);
        if (ApiHelper.HAS_ROBOTO_MEDIUM_FONT) {
            typeface = Typeface.create("sans-serif-medium", 0);
        } else {
            typeface = Typeface.createFromAsset(getResources().getAssets(), "Roboto-Medium.ttf");
        }
        this.mText.setTypeface(typeface);
    }

    public void setDefaultBackgroundColor(int color) {
        setBackgroundColor(color);
    }

    public void setVisibleWidthChangedListener(VisibleWidthChangedListener listener) {
        this.mListener = listener;
    }

    @Override
    public void setSelected(boolean selected) {
        this.mIcon.setSelected(selected);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        return false;
    }

    public void onSwipeModeChanged(boolean swipeIn) {
        this.mText.setTranslationX(0.0f);
    }

    public void setText(CharSequence text) {
        this.mText.setText(text);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mWidth = right - left;
        if (changed && this.mVisibleWidth > 0) {
            setVisibleWidth(this.mWidth);
        }
    }

    public void setImageResource(int resource) {
        Drawable drawableIcon = getResources().getDrawable(resource);
        if (drawableIcon != null) {
            drawableIcon = drawableIcon.mutate();
        }
        this.mIcon.setIconDrawable(drawableIcon);
    }

    public void setVisibleWidth(int newWidth) {
        int fullyShownIconWidth = getMaxVisibleWidth();
        int newWidth2 = Math.min(Math.max(newWidth, 0), fullyShownIconWidth);
        if (this.mVisibleWidth != newWidth2) {
            this.mVisibleWidth = newWidth2;
            if (this.mListener != null) {
                this.mListener.onVisibleWidthChanged(newWidth2);
            }
        }
        invalidate();
    }

    public int getVisibleWidth() {
        return this.mVisibleWidth;
    }

    @Override
    public void draw(Canvas canvas) {
        float transX = 0.0f;
        if (this.mVisibleWidth < this.mMinVisibleWidth + this.mIcon.getLeft()) {
            transX = (this.mMinVisibleWidth + this.mIcon.getLeft()) - this.mVisibleWidth;
        }
        canvas.save();
        canvas.translate(-transX, 0.0f);
        super.draw(canvas);
        canvas.restore();
    }

    public void setHighlightColor(int highlightColor) {
        this.mIcon.setHighlightColor(highlightColor);
    }

    public int getHighlightColor() {
        return this.mIcon.getHighlightColor();
    }

    public int getMaxVisibleWidth() {
        return this.mIcon.getLeft() + this.mMinVisibleWidth;
    }

    public void getIconCenterLocationInWindow(int[] loc) {
        this.mIcon.getLocationInWindow(loc);
        loc[0] = loc[0] + (this.mMinVisibleWidth / 2);
        loc[1] = loc[1] + (this.mMinVisibleWidth / 2);
    }

    public void setModeId(int modeId) {
        this.mModeId = modeId;
    }

    public int getModeId() {
        return this.mModeId;
    }

    public ModeIconView getIcon() {
        return this.mIcon;
    }

    public void setTextAlpha(float alpha) {
        this.mText.setAlpha(alpha);
    }
}
