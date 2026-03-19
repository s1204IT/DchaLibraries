package android.support.v17.leanback.widget;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v17.leanback.R;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class SearchOrbView extends FrameLayout implements View.OnClickListener {
    private boolean mAttachedToWindow;
    private boolean mColorAnimationEnabled;
    private ValueAnimator mColorAnimator;
    private final ArgbEvaluator mColorEvaluator;
    private Colors mColors;
    private final ValueAnimator.AnimatorUpdateListener mFocusUpdateListener;
    private final float mFocusedZ;
    private final float mFocusedZoom;
    private ImageView mIcon;
    private Drawable mIconDrawable;
    private View.OnClickListener mListener;
    private final int mPulseDurationMs;
    private View mRootView;
    private final int mScaleDurationMs;
    private View mSearchOrbView;
    private ValueAnimator mShadowFocusAnimator;
    private final float mUnfocusedZ;
    private final ValueAnimator.AnimatorUpdateListener mUpdateListener;

    public static class Colors {
        public int brightColor;
        public int color;
        public int iconColor;

        public Colors(int color, int brightColor, int iconColor) {
            this.color = color;
            this.brightColor = brightColor == color ? getBrightColor(color) : brightColor;
            this.iconColor = iconColor;
        }

        public static int getBrightColor(int color) {
            int red = (int) ((Color.red(color) * 0.85f) + 38.25f);
            int green = (int) ((Color.green(color) * 0.85f) + 38.25f);
            int blue = (int) ((Color.blue(color) * 0.85f) + 38.25f);
            int alpha = (int) ((Color.alpha(color) * 0.85f) + 38.25f);
            return Color.argb(alpha, red, green, blue);
        }
    }

    void setSearchOrbZ(float fraction) {
        ViewCompat.setZ(this.mSearchOrbView, this.mUnfocusedZ + ((this.mFocusedZ - this.mUnfocusedZ) * fraction));
    }

    public SearchOrbView(Context context) {
        this(context, null);
    }

    public SearchOrbView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.searchOrbViewStyle);
    }

    public SearchOrbView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mColorEvaluator = new ArgbEvaluator();
        this.mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                Integer color = (Integer) animator.getAnimatedValue();
                SearchOrbView.this.setOrbViewColor(color.intValue());
            }
        };
        this.mFocusUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                SearchOrbView.this.setSearchOrbZ(animation.getAnimatedFraction());
            }
        };
        Resources res = context.getResources();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mRootView = inflater.inflate(getLayoutResourceId(), (ViewGroup) this, true);
        this.mSearchOrbView = this.mRootView.findViewById(R.id.search_orb);
        this.mIcon = (ImageView) this.mRootView.findViewById(R.id.icon);
        this.mFocusedZoom = context.getResources().getFraction(R.fraction.lb_search_orb_focused_zoom, 1, 1);
        this.mPulseDurationMs = context.getResources().getInteger(R.integer.lb_search_orb_pulse_duration_ms);
        this.mScaleDurationMs = context.getResources().getInteger(R.integer.lb_search_orb_scale_duration_ms);
        this.mFocusedZ = context.getResources().getDimensionPixelSize(R.dimen.lb_search_orb_focused_z);
        this.mUnfocusedZ = context.getResources().getDimensionPixelSize(R.dimen.lb_search_orb_unfocused_z);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lbSearchOrbView, defStyleAttr, 0);
        Drawable img = a.getDrawable(R.styleable.lbSearchOrbView_searchOrbIcon);
        setOrbIcon(img == null ? res.getDrawable(R.drawable.lb_ic_in_app_search) : img);
        int defColor = res.getColor(R.color.lb_default_search_color);
        int color = a.getColor(R.styleable.lbSearchOrbView_searchOrbColor, defColor);
        int brightColor = a.getColor(R.styleable.lbSearchOrbView_searchOrbBrightColor, color);
        int iconColor = a.getColor(R.styleable.lbSearchOrbView_searchOrbIconColor, 0);
        setOrbColors(new Colors(color, brightColor, iconColor));
        a.recycle();
        setFocusable(true);
        setClipChildren(false);
        setOnClickListener(this);
        setSoundEffectsEnabled(false);
        setSearchOrbZ(0.0f);
        ViewCompat.setZ(this.mIcon, this.mFocusedZ);
    }

    int getLayoutResourceId() {
        return R.layout.lb_search_orb;
    }

    void scaleOrbViewOnly(float scale) {
        this.mSearchOrbView.setScaleX(scale);
        this.mSearchOrbView.setScaleY(scale);
    }

    float getFocusedZoom() {
        return this.mFocusedZoom;
    }

    @Override
    public void onClick(View view) {
        if (this.mListener != null) {
            this.mListener.onClick(view);
        }
    }

    private void startShadowFocusAnimation(boolean gainFocus, int duration) {
        if (this.mShadowFocusAnimator == null) {
            this.mShadowFocusAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
            this.mShadowFocusAnimator.addUpdateListener(this.mFocusUpdateListener);
        }
        if (gainFocus) {
            this.mShadowFocusAnimator.start();
        } else {
            this.mShadowFocusAnimator.reverse();
        }
        this.mShadowFocusAnimator.setDuration(duration);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        animateOnFocus(gainFocus);
    }

    void animateOnFocus(boolean hasFocus) {
        float zoom = hasFocus ? this.mFocusedZoom : 1.0f;
        this.mRootView.animate().scaleX(zoom).scaleY(zoom).setDuration(this.mScaleDurationMs).start();
        startShadowFocusAnimation(hasFocus, this.mScaleDurationMs);
        enableOrbColorAnimation(hasFocus);
    }

    public void setOrbIcon(Drawable icon) {
        this.mIconDrawable = icon;
        this.mIcon.setImageDrawable(this.mIconDrawable);
    }

    public void setOnOrbClickedListener(View.OnClickListener listener) {
        this.mListener = listener;
    }

    public void setOrbColors(Colors colors) {
        this.mColors = colors;
        this.mIcon.setColorFilter(this.mColors.iconColor);
        if (this.mColorAnimator == null) {
            setOrbViewColor(this.mColors.color);
        } else {
            enableOrbColorAnimation(true);
        }
    }

    public void enableOrbColorAnimation(boolean enable) {
        this.mColorAnimationEnabled = enable;
        updateColorAnimator();
    }

    private void updateColorAnimator() {
        if (this.mColorAnimator != null) {
            this.mColorAnimator.end();
            this.mColorAnimator = null;
        }
        if (this.mColorAnimationEnabled && this.mAttachedToWindow) {
            this.mColorAnimator = ValueAnimator.ofObject(this.mColorEvaluator, Integer.valueOf(this.mColors.color), Integer.valueOf(this.mColors.brightColor), Integer.valueOf(this.mColors.color));
            this.mColorAnimator.setRepeatCount(-1);
            this.mColorAnimator.setDuration(this.mPulseDurationMs * 2);
            this.mColorAnimator.addUpdateListener(this.mUpdateListener);
            this.mColorAnimator.start();
        }
    }

    void setOrbViewColor(int color) {
        if (this.mSearchOrbView.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) this.mSearchOrbView.getBackground()).setColor(color);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAttachedToWindow = true;
        updateColorAnimator();
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mAttachedToWindow = false;
        updateColorAnimator();
        super.onDetachedFromWindow();
    }
}
