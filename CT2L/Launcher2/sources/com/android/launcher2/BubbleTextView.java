package com.android.launcher2;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

public class BubbleTextView extends TextView {
    private Drawable mBackground;
    private boolean mBackgroundSizeChanged;
    private boolean mDidInvalidateForPressedState;
    private int mFocusedGlowColor;
    private int mFocusedOutlineColor;
    private CheckLongPressHelper mLongPressHelper;
    private final HolographicOutlineHelper mOutlineHelper;
    private int mPressedGlowColor;
    private Bitmap mPressedOrFocusedBackground;
    private int mPressedOutlineColor;
    private int mPrevAlpha;
    private boolean mStayPressed;
    private final Canvas mTempCanvas;
    private final Rect mTempRect;

    public BubbleTextView(Context context) {
        super(context);
        this.mPrevAlpha = -1;
        this.mOutlineHelper = new HolographicOutlineHelper();
        this.mTempCanvas = new Canvas();
        this.mTempRect = new Rect();
        init();
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPrevAlpha = -1;
        this.mOutlineHelper = new HolographicOutlineHelper();
        this.mTempCanvas = new Canvas();
        this.mTempRect = new Rect();
        init();
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mPrevAlpha = -1;
        this.mOutlineHelper = new HolographicOutlineHelper();
        this.mTempCanvas = new Canvas();
        this.mTempRect = new Rect();
        init();
    }

    private void init() {
        this.mLongPressHelper = new CheckLongPressHelper(this);
        this.mBackground = getBackground();
        Resources res = getContext().getResources();
        int color = res.getColor(R.color.white);
        this.mPressedGlowColor = color;
        this.mPressedOutlineColor = color;
        this.mFocusedGlowColor = color;
        this.mFocusedOutlineColor = color;
        setShadowLayer(4.0f, 0.0f, 2.0f, -587202560);
    }

    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache) {
        Bitmap b = info.getIcon(iconCache);
        setCompoundDrawablesWithIntrinsicBounds((Drawable) null, new FastBitmapDrawable(b), (Drawable) null, (Drawable) null);
        setText(info.title);
        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }
        setTag(info);
    }

    @Override
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (getLeft() != left || getRight() != right || getTop() != top || getBottom() != bottom) {
            this.mBackgroundSizeChanged = true;
        }
        return super.setFrame(left, top, right, bottom);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == this.mBackground || super.verifyDrawable(who);
    }

    @Override
    public void setTag(Object tag) {
        if (tag != null) {
            LauncherModel.checkItemInfo((ItemInfo) tag);
        }
        super.setTag(tag);
    }

    @Override
    protected void drawableStateChanged() {
        if (isPressed()) {
            if (!this.mDidInvalidateForPressedState) {
                setCellLayoutPressedOrFocusedIcon();
            }
        } else {
            boolean backgroundEmptyBefore = this.mPressedOrFocusedBackground == null;
            if (!this.mStayPressed) {
                this.mPressedOrFocusedBackground = null;
            }
            if (isFocused()) {
                if (getLayout() == null) {
                    this.mPressedOrFocusedBackground = null;
                } else {
                    this.mPressedOrFocusedBackground = createGlowingOutline(this.mTempCanvas, this.mFocusedGlowColor, this.mFocusedOutlineColor);
                }
                this.mStayPressed = false;
                setCellLayoutPressedOrFocusedIcon();
            }
            boolean backgroundEmptyNow = this.mPressedOrFocusedBackground == null;
            if (!backgroundEmptyBefore && backgroundEmptyNow) {
                setCellLayoutPressedOrFocusedIcon();
            }
        }
        Drawable d = this.mBackground;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
        super.drawableStateChanged();
    }

    private void drawWithPadding(Canvas destCanvas, int padding) {
        Rect clipRect = this.mTempRect;
        getDrawingRect(clipRect);
        clipRect.bottom = (getExtendedPaddingTop() - 3) + getLayout().getLineTop(0);
        destCanvas.save();
        destCanvas.scale(getScaleX(), getScaleY(), (getWidth() + padding) / 2, (getHeight() + padding) / 2);
        destCanvas.translate((-getScrollX()) + (padding / 2), (-getScrollY()) + (padding / 2));
        destCanvas.clipRect(clipRect, Region.Op.REPLACE);
        draw(destCanvas);
        destCanvas.restore();
    }

    private Bitmap createGlowingOutline(Canvas canvas, int outlineColor, int glowColor) {
        int padding = HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS;
        Bitmap b = Bitmap.createBitmap(getWidth() + padding, getHeight() + padding, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(b);
        drawWithPadding(canvas, padding);
        this.mOutlineHelper.applyExtraThickExpensiveOutlineWithBlur(b, canvas, glowColor, outlineColor);
        canvas.setBitmap(null);
        return b;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        switch (event.getAction()) {
            case 0:
                if (this.mPressedOrFocusedBackground == null) {
                    this.mPressedOrFocusedBackground = createGlowingOutline(this.mTempCanvas, this.mPressedGlowColor, this.mPressedOutlineColor);
                }
                if (isPressed()) {
                    this.mDidInvalidateForPressedState = true;
                    setCellLayoutPressedOrFocusedIcon();
                } else {
                    this.mDidInvalidateForPressedState = false;
                }
                this.mLongPressHelper.postCheckForLongPress();
                return result;
            case 1:
            case 3:
                if (!isPressed()) {
                    this.mPressedOrFocusedBackground = null;
                }
                this.mLongPressHelper.cancelLongPress();
                return result;
            case 2:
            default:
                return result;
        }
    }

    void setStayPressed(boolean stayPressed) {
        this.mStayPressed = stayPressed;
        if (!stayPressed) {
            this.mPressedOrFocusedBackground = null;
        }
        setCellLayoutPressedOrFocusedIcon();
    }

    void setCellLayoutPressedOrFocusedIcon() {
        ShortcutAndWidgetContainer parent;
        if ((getParent() instanceof ShortcutAndWidgetContainer) && (parent = (ShortcutAndWidgetContainer) getParent()) != null) {
            CellLayout layout = (CellLayout) parent.getParent();
            if (this.mPressedOrFocusedBackground == null) {
                this = null;
            }
            layout.setPressedOrFocusedIcon(this);
        }
    }

    void clearPressedOrFocusedBackground() {
        this.mPressedOrFocusedBackground = null;
        setCellLayoutPressedOrFocusedIcon();
    }

    Bitmap getPressedOrFocusedBackground() {
        return this.mPressedOrFocusedBackground;
    }

    int getPressedOrFocusedBackgroundPadding() {
        return HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS / 2;
    }

    @Override
    public void draw(Canvas canvas) {
        Drawable background = this.mBackground;
        if (background != null) {
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            if (this.mBackgroundSizeChanged) {
                background.setBounds(0, 0, getRight() - getLeft(), getBottom() - getTop());
                this.mBackgroundSizeChanged = false;
            }
            if ((scrollX | scrollY) == 0) {
                background.draw(canvas);
            } else {
                canvas.translate(scrollX, scrollY);
                background.draw(canvas);
                canvas.translate(-scrollX, -scrollY);
            }
        }
        if (getCurrentTextColor() == getResources().getColor(R.color.transparent)) {
            getPaint().clearShadowLayer();
            super.draw(canvas);
            return;
        }
        getPaint().setShadowLayer(4.0f, 0.0f, 2.0f, -587202560);
        super.draw(canvas);
        canvas.save(2);
        canvas.clipRect(getScrollX(), getScrollY() + getExtendedPaddingTop(), getScrollX() + getWidth(), getScrollY() + getHeight(), Region.Op.INTERSECT);
        getPaint().setShadowLayer(1.75f, 0.0f, 0.0f, -872415232);
        super.draw(canvas);
        canvas.restore();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mBackground != null) {
            this.mBackground.setCallback(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mBackground != null) {
            this.mBackground.setCallback(null);
        }
    }

    @Override
    protected boolean onSetAlpha(int alpha) {
        if (this.mPrevAlpha != alpha) {
            this.mPrevAlpha = alpha;
            super.onSetAlpha(alpha);
            return true;
        }
        return true;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        this.mLongPressHelper.cancelLongPress();
    }
}
