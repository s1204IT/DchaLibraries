package android.support.v7.widget;

import android.R;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class CardView extends FrameLayout {
    private static final int[] COLOR_BACKGROUND_ATTR = {R.attr.colorBackground};
    private static final CardViewImpl IMPL;
    private final CardViewDelegate mCardViewDelegate;
    private boolean mCompatPadding;
    final Rect mContentPadding;
    private boolean mPreventCornerOverlap;
    final Rect mShadowBounds;
    int mUserSetMinHeight;
    int mUserSetMinWidth;

    static {
        if (Build.VERSION.SDK_INT >= 21) {
            IMPL = new CardViewApi21Impl();
        } else if (Build.VERSION.SDK_INT >= 17) {
            IMPL = new CardViewApi17Impl();
        } else {
            IMPL = new CardViewBaseImpl();
        }
        IMPL.initStatic();
    }

    public CardView(Context context) {
        this(context, null);
    }

    public CardView(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.cardview.R.attr.cardViewStyle);
    }

    public CardView(Context context, AttributeSet attrs, int defStyleAttr) {
        int color;
        ColorStateList colorStateListValueOf;
        super(context, attrs, defStyleAttr);
        this.mContentPadding = new Rect();
        this.mShadowBounds = new Rect();
        this.mCardViewDelegate = new CardViewDelegate() {
            private Drawable mCardBackground;

            @Override
            public void setCardBackground(Drawable drawable) {
                this.mCardBackground = drawable;
                CardView.this.setBackgroundDrawable(drawable);
            }

            @Override
            public boolean getUseCompatPadding() {
                return CardView.this.getUseCompatPadding();
            }

            @Override
            public boolean getPreventCornerOverlap() {
                return CardView.this.getPreventCornerOverlap();
            }

            @Override
            public void setShadowPadding(int left, int top, int right, int bottom) {
                CardView.this.mShadowBounds.set(left, top, right, bottom);
                CardView.super.setPadding(CardView.this.mContentPadding.left + left, CardView.this.mContentPadding.top + top, CardView.this.mContentPadding.right + right, CardView.this.mContentPadding.bottom + bottom);
            }

            @Override
            public void setMinWidthHeightInternal(int width, int height) {
                if (width > CardView.this.mUserSetMinWidth) {
                    CardView.super.setMinimumWidth(width);
                }
                if (height > CardView.this.mUserSetMinHeight) {
                    CardView.super.setMinimumHeight(height);
                }
            }

            @Override
            public Drawable getCardBackground() {
                return this.mCardBackground;
            }

            @Override
            public View getCardView() {
                return CardView.this;
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, android.support.v7.cardview.R.styleable.CardView, defStyleAttr, android.support.v7.cardview.R.style.CardView);
        if (a.hasValue(android.support.v7.cardview.R.styleable.CardView_cardBackgroundColor)) {
            colorStateListValueOf = a.getColorStateList(android.support.v7.cardview.R.styleable.CardView_cardBackgroundColor);
        } else {
            TypedArray aa = getContext().obtainStyledAttributes(COLOR_BACKGROUND_ATTR);
            int themeColorBackground = aa.getColor(0, 0);
            aa.recycle();
            float[] hsv = new float[3];
            Color.colorToHSV(themeColorBackground, hsv);
            if (hsv[2] > 0.5f) {
                color = getResources().getColor(android.support.v7.cardview.R.color.cardview_light_background);
            } else {
                color = getResources().getColor(android.support.v7.cardview.R.color.cardview_dark_background);
            }
            colorStateListValueOf = ColorStateList.valueOf(color);
        }
        ColorStateList backgroundColor = colorStateListValueOf;
        float radius = a.getDimension(android.support.v7.cardview.R.styleable.CardView_cardCornerRadius, 0.0f);
        float elevation = a.getDimension(android.support.v7.cardview.R.styleable.CardView_cardElevation, 0.0f);
        float maxElevation = a.getDimension(android.support.v7.cardview.R.styleable.CardView_cardMaxElevation, 0.0f);
        this.mCompatPadding = a.getBoolean(android.support.v7.cardview.R.styleable.CardView_cardUseCompatPadding, false);
        this.mPreventCornerOverlap = a.getBoolean(android.support.v7.cardview.R.styleable.CardView_cardPreventCornerOverlap, true);
        int defaultPadding = a.getDimensionPixelSize(android.support.v7.cardview.R.styleable.CardView_contentPadding, 0);
        this.mContentPadding.left = a.getDimensionPixelSize(android.support.v7.cardview.R.styleable.CardView_contentPaddingLeft, defaultPadding);
        this.mContentPadding.top = a.getDimensionPixelSize(android.support.v7.cardview.R.styleable.CardView_contentPaddingTop, defaultPadding);
        this.mContentPadding.right = a.getDimensionPixelSize(android.support.v7.cardview.R.styleable.CardView_contentPaddingRight, defaultPadding);
        this.mContentPadding.bottom = a.getDimensionPixelSize(android.support.v7.cardview.R.styleable.CardView_contentPaddingBottom, defaultPadding);
        float maxElevation2 = elevation > maxElevation ? elevation : maxElevation;
        this.mUserSetMinWidth = a.getDimensionPixelSize(android.support.v7.cardview.R.styleable.CardView_android_minWidth, 0);
        this.mUserSetMinHeight = a.getDimensionPixelSize(android.support.v7.cardview.R.styleable.CardView_android_minHeight, 0);
        a.recycle();
        IMPL.initialize(this.mCardViewDelegate, context, backgroundColor, radius, elevation, maxElevation2);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
    }

    public boolean getUseCompatPadding() {
        return this.mCompatPadding;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!(IMPL instanceof CardViewApi21Impl)) {
            int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
            if (widthMode == Integer.MIN_VALUE || widthMode == 1073741824) {
                int minWidth = (int) Math.ceil(IMPL.getMinWidth(this.mCardViewDelegate));
                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(Math.max(minWidth, View.MeasureSpec.getSize(widthMeasureSpec)), widthMode);
            }
            int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
            if (heightMode == Integer.MIN_VALUE || heightMode == 1073741824) {
                int minHeight = (int) Math.ceil(IMPL.getMinHeight(this.mCardViewDelegate));
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(Math.max(minHeight, View.MeasureSpec.getSize(heightMeasureSpec)), heightMode);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setMinimumWidth(int minWidth) {
        this.mUserSetMinWidth = minWidth;
        super.setMinimumWidth(minWidth);
    }

    @Override
    public void setMinimumHeight(int minHeight) {
        this.mUserSetMinHeight = minHeight;
        super.setMinimumHeight(minHeight);
    }

    public boolean getPreventCornerOverlap() {
        return this.mPreventCornerOverlap;
    }
}
