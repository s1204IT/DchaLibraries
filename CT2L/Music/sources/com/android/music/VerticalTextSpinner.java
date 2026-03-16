package com.android.music;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public class VerticalTextSpinner extends View {
    private static int SCROLL_DISTANCE;
    private static int TEXT1_Y;
    private static int TEXT2_Y;
    private static int TEXT3_Y;
    private static int TEXT4_Y;
    private static int TEXT5_Y;
    private static int TEXT_MARGIN_RIGHT;
    private static int TEXT_SIZE;
    private static int TEXT_SPACING;
    private boolean isDraggingSelector;
    private final Drawable mBackgroundFocused;
    private int mCurrentSelectedPos;
    private long mDelayBetweenAnimations;
    private int mDistanceOfEachAnimation;
    private int mDownY;
    private boolean mIsAnimationRunning;
    private OnChangedListener mListener;
    private int mNumberOfAnimations;
    private long mScrollInterval;
    private int mScrollMode;
    private Drawable mSelector;
    private final int mSelectorDefaultY;
    private final Drawable mSelectorFocused;
    private final int mSelectorHeight;
    private final int mSelectorMaxY;
    private final int mSelectorMinY;
    private final Drawable mSelectorNormal;
    private int mSelectorY;
    private boolean mStopAnimation;
    private String mText1;
    private String mText2;
    private String mText3;
    private String mText4;
    private String mText5;
    private String[] mTextList;
    private final TextPaint mTextPaintDark;
    private final TextPaint mTextPaintLight;
    private int mTotalAnimatedDistance;
    private boolean mWrapAround;

    public interface OnChangedListener {
        void onChanged(VerticalTextSpinner verticalTextSpinner, int i, int i2, String[] strArr);
    }

    public VerticalTextSpinner(Context context) {
        this(context, null);
    }

    public VerticalTextSpinner(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalTextSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mWrapAround = true;
        float scale = getResources().getDisplayMetrics().density;
        TEXT_SPACING = (int) (18.0f * scale);
        TEXT_MARGIN_RIGHT = (int) (25.0f * scale);
        TEXT_SIZE = (int) (22.0f * scale);
        SCROLL_DISTANCE = TEXT_SIZE + TEXT_SPACING;
        TEXT1_Y = (TEXT_SIZE * 0) + (TEXT_SPACING * (-1));
        TEXT2_Y = (TEXT_SIZE * 1) + (TEXT_SPACING * 0);
        TEXT3_Y = (TEXT_SIZE * 2) + (TEXT_SPACING * 1);
        TEXT4_Y = (TEXT_SIZE * 3) + (TEXT_SPACING * 2);
        TEXT5_Y = (TEXT_SIZE * 4) + (TEXT_SPACING * 3);
        this.mBackgroundFocused = context.getResources().getDrawable(R.drawable.pickerbox_background);
        this.mSelectorFocused = context.getResources().getDrawable(R.drawable.pickerbox_selected);
        this.mSelectorNormal = context.getResources().getDrawable(R.drawable.pickerbox_unselected);
        this.mSelectorHeight = this.mSelectorFocused.getIntrinsicHeight();
        this.mSelectorDefaultY = (this.mBackgroundFocused.getIntrinsicHeight() - this.mSelectorHeight) / 2;
        this.mSelectorMinY = 0;
        this.mSelectorMaxY = this.mBackgroundFocused.getIntrinsicHeight() - this.mSelectorHeight;
        this.mSelector = this.mSelectorNormal;
        this.mSelectorY = this.mSelectorDefaultY;
        this.mTextPaintDark = new TextPaint(1);
        this.mTextPaintDark.setTextSize(TEXT_SIZE);
        this.mTextPaintDark.setColor(context.getResources().getColor(android.R.color.primary_text_light));
        this.mTextPaintLight = new TextPaint(1);
        this.mTextPaintLight.setTextSize(TEXT_SIZE);
        this.mTextPaintLight.setColor(context.getResources().getColor(android.R.color.secondary_text_dark));
        this.mScrollMode = 0;
        this.mScrollInterval = 400L;
        calculateAnimationValues();
    }

    public void setItems(String[] textList) {
        this.mTextList = textList;
        calculateTextPositions();
    }

    public void setSelectedPos(int selectedPos) {
        this.mCurrentSelectedPos = selectedPos;
        calculateTextPositions();
        postInvalidate();
    }

    public void setScrollInterval(long interval) {
        this.mScrollInterval = interval;
        calculateAnimationValues();
    }

    public void setWrapAround(boolean wrap) {
        this.mWrapAround = wrap;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 19 && canScrollDown()) {
            this.mScrollMode = 2;
            scroll();
            this.mStopAnimation = true;
            return true;
        }
        if (keyCode == 20 && canScrollUp()) {
            this.mScrollMode = 1;
            scroll();
            this.mStopAnimation = true;
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean canScrollDown() {
        return this.mCurrentSelectedPos > 0 || this.mWrapAround;
    }

    private boolean canScrollUp() {
        return this.mCurrentSelectedPos < this.mTextList.length + (-1) || this.mWrapAround;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (gainFocus) {
            setBackgroundDrawable(this.mBackgroundFocused);
            this.mSelector = this.mSelectorFocused;
        } else {
            setBackgroundDrawable(null);
            this.mSelector = this.mSelectorNormal;
            this.mSelectorY = this.mSelectorDefaultY;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean z = false;
        int action = event.getAction();
        int y = (int) event.getY();
        switch (action) {
            case 0:
                requestFocus();
                this.mDownY = y;
                if (y >= this.mSelectorY && y <= this.mSelectorY + this.mSelector.getIntrinsicHeight()) {
                    z = true;
                }
                this.isDraggingSelector = z;
                return true;
            case 1:
            default:
                this.mSelectorY = this.mSelectorDefaultY;
                this.mStopAnimation = true;
                invalidate();
                return true;
            case 2:
                if (this.isDraggingSelector) {
                    int top = this.mSelectorDefaultY + (y - this.mDownY);
                    if (top <= this.mSelectorMinY && canScrollDown()) {
                        this.mSelectorY = this.mSelectorMinY;
                        this.mStopAnimation = false;
                        if (this.mScrollMode != 2) {
                            this.mScrollMode = 2;
                            scroll();
                        }
                    } else if (top >= this.mSelectorMaxY && canScrollUp()) {
                        this.mSelectorY = this.mSelectorMaxY;
                        this.mStopAnimation = false;
                        if (this.mScrollMode != 1) {
                            this.mScrollMode = 1;
                            scroll();
                        }
                    } else {
                        this.mSelectorY = top;
                        this.mStopAnimation = true;
                    }
                }
                return true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int selectorTop = this.mSelectorY;
        int selectorRight = getWidth();
        int selectorBottom = this.mSelectorY + this.mSelectorHeight;
        this.mSelector.setBounds(0, selectorTop, selectorRight, selectorBottom);
        this.mSelector.draw(canvas);
        if (this.mTextList != null) {
            TextPaint textPaintDark = this.mTextPaintDark;
            if (hasFocus()) {
                int topBottom = selectorTop + 15;
                String text1 = this.mText1;
                String text2 = this.mText2;
                String text3 = this.mText3;
                String text4 = this.mText4;
                String text5 = this.mText5;
                TextPaint textPaintLight = this.mTextPaintLight;
                canvas.save();
                canvas.clipRect(0, 0, selectorRight, topBottom);
                drawText(canvas, text1, TEXT1_Y + this.mTotalAnimatedDistance, textPaintLight);
                drawText(canvas, text2, TEXT2_Y + this.mTotalAnimatedDistance, textPaintLight);
                drawText(canvas, text3, TEXT3_Y + this.mTotalAnimatedDistance, textPaintLight);
                canvas.restore();
                canvas.save();
                canvas.clipRect(0, selectorTop + 15, selectorRight, selectorBottom - 15);
                drawText(canvas, text2, TEXT2_Y + this.mTotalAnimatedDistance, textPaintDark);
                drawText(canvas, text3, TEXT3_Y + this.mTotalAnimatedDistance, textPaintDark);
                drawText(canvas, text4, TEXT4_Y + this.mTotalAnimatedDistance, textPaintDark);
                canvas.restore();
                int bottomTop = selectorBottom - 15;
                int bottomBottom = getMeasuredHeight();
                canvas.save();
                canvas.clipRect(0, bottomTop, selectorRight, bottomBottom);
                drawText(canvas, text3, TEXT3_Y + this.mTotalAnimatedDistance, textPaintLight);
                drawText(canvas, text4, TEXT4_Y + this.mTotalAnimatedDistance, textPaintLight);
                drawText(canvas, text5, TEXT5_Y + this.mTotalAnimatedDistance, textPaintLight);
                canvas.restore();
            } else {
                drawText(canvas, this.mText3, TEXT3_Y, textPaintDark);
            }
            if (this.mIsAnimationRunning) {
                if (Math.abs(this.mTotalAnimatedDistance) + this.mDistanceOfEachAnimation > SCROLL_DISTANCE) {
                    this.mTotalAnimatedDistance = 0;
                    if (this.mScrollMode == 1) {
                        int oldPos = this.mCurrentSelectedPos;
                        int newPos = getNewIndex(1);
                        if (newPos >= 0) {
                            this.mCurrentSelectedPos = newPos;
                            if (this.mListener != null) {
                                this.mListener.onChanged(this, oldPos, this.mCurrentSelectedPos, this.mTextList);
                            }
                        }
                        if (newPos < 0 || (newPos >= this.mTextList.length - 1 && !this.mWrapAround)) {
                            this.mStopAnimation = true;
                        }
                        calculateTextPositions();
                    } else if (this.mScrollMode == 2) {
                        int oldPos2 = this.mCurrentSelectedPos;
                        int newPos2 = getNewIndex(-1);
                        if (newPos2 >= 0) {
                            this.mCurrentSelectedPos = newPos2;
                            if (this.mListener != null) {
                                this.mListener.onChanged(this, oldPos2, this.mCurrentSelectedPos, this.mTextList);
                            }
                        }
                        if (newPos2 < 0 || (newPos2 == 0 && !this.mWrapAround)) {
                            this.mStopAnimation = true;
                        }
                        calculateTextPositions();
                    }
                    if (this.mStopAnimation) {
                        int previousScrollMode = this.mScrollMode;
                        this.mIsAnimationRunning = false;
                        this.mStopAnimation = false;
                        this.mScrollMode = 0;
                        if ("".equals(this.mTextList[this.mCurrentSelectedPos])) {
                            this.mScrollMode = previousScrollMode;
                            scroll();
                            this.mStopAnimation = true;
                        }
                    }
                } else if (this.mScrollMode == 1) {
                    this.mTotalAnimatedDistance -= this.mDistanceOfEachAnimation;
                } else if (this.mScrollMode == 2) {
                    this.mTotalAnimatedDistance += this.mDistanceOfEachAnimation;
                }
                if (this.mDelayBetweenAnimations > 0) {
                    postInvalidateDelayed(this.mDelayBetweenAnimations);
                } else {
                    invalidate();
                }
            }
        }
    }

    private void calculateTextPositions() {
        this.mText1 = getTextToDraw(-2);
        this.mText2 = getTextToDraw(-1);
        this.mText3 = getTextToDraw(0);
        this.mText4 = getTextToDraw(1);
        this.mText5 = getTextToDraw(2);
    }

    private String getTextToDraw(int offset) {
        int index = getNewIndex(offset);
        return index < 0 ? "" : this.mTextList[index];
    }

    private int getNewIndex(int offset) {
        int index = this.mCurrentSelectedPos + offset;
        if (index < 0) {
            if (!this.mWrapAround) {
                return -1;
            }
            index += this.mTextList.length;
        } else if (index >= this.mTextList.length) {
            if (!this.mWrapAround) {
                return -1;
            }
            index -= this.mTextList.length;
        }
        return index;
    }

    private void scroll() {
        if (!this.mIsAnimationRunning) {
            this.mTotalAnimatedDistance = 0;
            this.mIsAnimationRunning = true;
            invalidate();
        }
    }

    private void calculateAnimationValues() {
        this.mNumberOfAnimations = ((int) this.mScrollInterval) / SCROLL_DISTANCE;
        if (this.mNumberOfAnimations < 4) {
            this.mNumberOfAnimations = 4;
            this.mDistanceOfEachAnimation = SCROLL_DISTANCE / this.mNumberOfAnimations;
            this.mDelayBetweenAnimations = 0L;
        } else {
            this.mDistanceOfEachAnimation = SCROLL_DISTANCE / this.mNumberOfAnimations;
            this.mDelayBetweenAnimations = this.mScrollInterval / ((long) this.mNumberOfAnimations);
        }
    }

    private void drawText(Canvas canvas, String text, int y, TextPaint paint) {
        int width = (int) paint.measureText(text);
        int x = (getMeasuredWidth() - width) - TEXT_MARGIN_RIGHT;
        canvas.drawText(text, x, y, paint);
    }

    public int getCurrentSelectedPos() {
        return this.mCurrentSelectedPos;
    }
}
