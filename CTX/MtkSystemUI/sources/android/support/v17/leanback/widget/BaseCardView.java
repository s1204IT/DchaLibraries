package android.support.v17.leanback.widget;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import java.util.ArrayList;

public class BaseCardView extends FrameLayout {
    private static final int[] LB_PRESSED_STATE_SET = {R.attr.state_pressed};
    private final int mActivatedAnimDuration;
    private Animation mAnim;
    private final Runnable mAnimationTrigger;
    private int mCardType;
    private boolean mDelaySelectedAnim;
    ArrayList<View> mExtraViewList;
    private int mExtraVisibility;
    float mInfoAlpha;
    float mInfoOffset;
    ArrayList<View> mInfoViewList;
    float mInfoVisFraction;
    private int mInfoVisibility;
    private ArrayList<View> mMainViewList;
    private int mMeasuredHeight;
    private int mMeasuredWidth;
    private final int mSelectedAnimDuration;
    private int mSelectedAnimationDelay;

    public BaseCardView(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v17.leanback.R.attr.baseCardViewStyle);
    }

    public BaseCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mAnimationTrigger = new Runnable() {
            @Override
            public void run() {
                BaseCardView.this.animateInfoOffset(true);
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, android.support.v17.leanback.R.styleable.lbBaseCardView, defStyleAttr, 0);
        try {
            this.mCardType = a.getInteger(android.support.v17.leanback.R.styleable.lbBaseCardView_cardType, 0);
            Drawable cardForeground = a.getDrawable(android.support.v17.leanback.R.styleable.lbBaseCardView_cardForeground);
            if (cardForeground != null) {
                setForeground(cardForeground);
            }
            Drawable cardBackground = a.getDrawable(android.support.v17.leanback.R.styleable.lbBaseCardView_cardBackground);
            if (cardBackground != null) {
                setBackground(cardBackground);
            }
            this.mInfoVisibility = a.getInteger(android.support.v17.leanback.R.styleable.lbBaseCardView_infoVisibility, 1);
            this.mExtraVisibility = a.getInteger(android.support.v17.leanback.R.styleable.lbBaseCardView_extraVisibility, 2);
            if (this.mExtraVisibility < this.mInfoVisibility) {
                this.mExtraVisibility = this.mInfoVisibility;
            }
            this.mSelectedAnimationDelay = a.getInteger(android.support.v17.leanback.R.styleable.lbBaseCardView_selectedAnimationDelay, getResources().getInteger(android.support.v17.leanback.R.integer.lb_card_selected_animation_delay));
            this.mSelectedAnimDuration = a.getInteger(android.support.v17.leanback.R.styleable.lbBaseCardView_selectedAnimationDuration, getResources().getInteger(android.support.v17.leanback.R.integer.lb_card_selected_animation_duration));
            this.mActivatedAnimDuration = a.getInteger(android.support.v17.leanback.R.styleable.lbBaseCardView_activatedAnimationDuration, getResources().getInteger(android.support.v17.leanback.R.integer.lb_card_activated_animation_duration));
            a.recycle();
            this.mDelaySelectedAnim = true;
            this.mMainViewList = new ArrayList<>();
            this.mInfoViewList = new ArrayList<>();
            this.mExtraViewList = new ArrayList<>();
            this.mInfoOffset = 0.0f;
            this.mInfoVisFraction = getFinalInfoVisFraction();
            this.mInfoAlpha = getFinalInfoAlpha();
        } catch (Throwable th) {
            a.recycle();
            throw th;
        }
    }

    final float getFinalInfoVisFraction() {
        return (this.mCardType == 2 && this.mInfoVisibility == 2 && !isSelected()) ? 0.0f : 1.0f;
    }

    final float getFinalInfoAlpha() {
        return (this.mCardType == 1 && this.mInfoVisibility == 2 && !isSelected()) ? 0.0f : 1.0f;
    }

    @Override
    public void setActivated(boolean activated) {
        if (activated != isActivated()) {
            super.setActivated(activated);
            applyActiveState(isActivated());
        }
    }

    @Override
    public void setSelected(boolean selected) {
        if (selected != isSelected()) {
            super.setSelected(selected);
            applySelectedState(isSelected());
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int state;
        int infoHeight;
        boolean infoAnimating = false;
        this.mMeasuredWidth = 0;
        this.mMeasuredHeight = 0;
        int mainHeight = 0;
        int extraHeight = 0;
        findChildrenViews();
        int unspecifiedSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        int state2 = 0;
        for (int state3 = 0; state3 < this.mMainViewList.size(); state3++) {
            View mainView = this.mMainViewList.get(state3);
            if (mainView.getVisibility() != 8) {
                measureChild(mainView, unspecifiedSpec, unspecifiedSpec);
                this.mMeasuredWidth = Math.max(this.mMeasuredWidth, mainView.getMeasuredWidth());
                mainHeight += mainView.getMeasuredHeight();
                state2 = View.combineMeasuredStates(state2, mainView.getMeasuredState());
            }
        }
        int i = this.mMeasuredWidth;
        setPivotX(i / 2);
        setPivotY(mainHeight / 2);
        int cardWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(this.mMeasuredWidth, 1073741824);
        if (hasInfoRegion()) {
            state = state2;
            infoHeight = 0;
            for (int infoHeight2 = 0; infoHeight2 < this.mInfoViewList.size(); infoHeight2++) {
                View infoView = this.mInfoViewList.get(infoHeight2);
                if (infoView.getVisibility() != 8) {
                    measureChild(infoView, cardWidthMeasureSpec, unspecifiedSpec);
                    if (this.mCardType != 1) {
                        infoHeight += infoView.getMeasuredHeight();
                    }
                    state = View.combineMeasuredStates(state, infoView.getMeasuredState());
                }
            }
            if (hasExtraRegion()) {
                for (int i2 = 0; i2 < this.mExtraViewList.size(); i2++) {
                    View extraView = this.mExtraViewList.get(i2);
                    if (extraView.getVisibility() != 8) {
                        measureChild(extraView, cardWidthMeasureSpec, unspecifiedSpec);
                        extraHeight += extraView.getMeasuredHeight();
                        state = View.combineMeasuredStates(state, extraView.getMeasuredState());
                    }
                }
            }
        } else {
            state = state2;
            infoHeight = 0;
        }
        if (hasInfoRegion() && this.mInfoVisibility == 2) {
            infoAnimating = true;
        }
        this.mMeasuredHeight = (int) (((mainHeight + (infoAnimating ? infoHeight * this.mInfoVisFraction : infoHeight)) + extraHeight) - (infoAnimating ? 0.0f : this.mInfoOffset));
        setMeasuredDimension(View.resolveSizeAndState(this.mMeasuredWidth + getPaddingLeft() + getPaddingRight(), widthMeasureSpec, state), View.resolveSizeAndState(this.mMeasuredHeight + getPaddingTop() + getPaddingBottom(), heightMeasureSpec, state << 16));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        float currBottom = getPaddingTop();
        float currBottom2 = currBottom;
        for (int i = 0; i < this.mMainViewList.size(); i++) {
            View mainView = this.mMainViewList.get(i);
            if (mainView.getVisibility() != 8) {
                mainView.layout(getPaddingLeft(), (int) currBottom2, this.mMeasuredWidth + getPaddingLeft(), (int) (mainView.getMeasuredHeight() + currBottom2));
                currBottom2 += mainView.getMeasuredHeight();
            }
        }
        if (hasInfoRegion()) {
            float infoHeight = 0.0f;
            for (int i2 = 0; i2 < this.mInfoViewList.size(); i2++) {
                infoHeight += this.mInfoViewList.get(i2).getMeasuredHeight();
            }
            int i3 = this.mCardType;
            if (i3 == 1) {
                currBottom2 -= infoHeight;
                if (currBottom2 < 0.0f) {
                    currBottom2 = 0.0f;
                }
            } else if (this.mCardType == 2) {
                if (this.mInfoVisibility == 2) {
                    infoHeight *= this.mInfoVisFraction;
                }
            } else {
                currBottom2 -= this.mInfoOffset;
            }
            for (int i4 = 0; i4 < this.mInfoViewList.size(); i4++) {
                View infoView = this.mInfoViewList.get(i4);
                if (infoView.getVisibility() != 8) {
                    int viewHeight = infoView.getMeasuredHeight();
                    if (viewHeight > infoHeight) {
                        viewHeight = (int) infoHeight;
                    }
                    infoView.layout(getPaddingLeft(), (int) currBottom2, this.mMeasuredWidth + getPaddingLeft(), (int) (viewHeight + currBottom2));
                    currBottom2 += viewHeight;
                    infoHeight -= viewHeight;
                    if (infoHeight <= 0.0f) {
                        break;
                    }
                }
            }
            if (hasExtraRegion()) {
                for (int i5 = 0; i5 < this.mExtraViewList.size(); i5++) {
                    View extraView = this.mExtraViewList.get(i5);
                    if (extraView.getVisibility() != 8) {
                        extraView.layout(getPaddingLeft(), (int) currBottom2, this.mMeasuredWidth + getPaddingLeft(), (int) (extraView.getMeasuredHeight() + currBottom2));
                        currBottom2 += extraView.getMeasuredHeight();
                    }
                }
            }
        }
        onSizeChanged(0, 0, right - left, bottom - top);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this.mAnimationTrigger);
        cancelAnimations();
    }

    private boolean hasInfoRegion() {
        return this.mCardType != 0;
    }

    private boolean hasExtraRegion() {
        return this.mCardType == 3;
    }

    private boolean isRegionVisible(int regionVisibility) {
        switch (regionVisibility) {
            case 0:
                return true;
            case 1:
                return isActivated();
            case 2:
                return isSelected();
            default:
                return false;
        }
    }

    private boolean isCurrentRegionVisible(int regionVisibility) {
        switch (regionVisibility) {
            case 2:
                if (this.mCardType != 2) {
                    break;
                } else if (this.mInfoVisFraction <= 0.0f) {
                    break;
                }
                break;
        }
        return false;
    }

    private void findChildrenViews() {
        this.mMainViewList.clear();
        this.mInfoViewList.clear();
        this.mExtraViewList.clear();
        int count = getChildCount();
        boolean infoVisible = hasInfoRegion() && isCurrentRegionVisible(this.mInfoVisibility);
        boolean extraVisible = hasExtraRegion() && this.mInfoOffset > 0.0f;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child != null) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.viewType == 1) {
                    child.setAlpha(this.mInfoAlpha);
                    this.mInfoViewList.add(child);
                    child.setVisibility(infoVisible ? 0 : 8);
                } else if (lp.viewType == 2) {
                    this.mExtraViewList.add(child);
                    child.setVisibility(extraVisible ? 0 : 8);
                } else {
                    this.mMainViewList.add(child);
                    child.setVisibility(0);
                }
            }
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] s = super.onCreateDrawableState(extraSpace);
        int N = s.length;
        boolean pressed = false;
        boolean enabled = false;
        for (int i = 0; i < N; i++) {
            if (s[i] == 16842919) {
                pressed = true;
            }
            if (s[i] == 16842910) {
                enabled = true;
            }
        }
        if (pressed && enabled) {
            return View.PRESSED_ENABLED_STATE_SET;
        }
        if (pressed) {
            return LB_PRESSED_STATE_SET;
        }
        if (enabled) {
            return View.ENABLED_STATE_SET;
        }
        return View.EMPTY_STATE_SET;
    }

    private void applyActiveState(boolean active) {
        if (hasInfoRegion() && this.mInfoVisibility == 1) {
            setInfoViewVisibility(isRegionVisible(this.mInfoVisibility));
        }
    }

    private void setInfoViewVisibility(boolean visible) {
        int i = 0;
        if (this.mCardType == 3) {
            if (visible) {
                for (int i2 = 0; i2 < this.mInfoViewList.size(); i2++) {
                    this.mInfoViewList.get(i2).setVisibility(0);
                }
                return;
            }
            for (int i3 = 0; i3 < this.mInfoViewList.size(); i3++) {
                this.mInfoViewList.get(i3).setVisibility(8);
            }
            while (true) {
                int i4 = i;
                if (i4 < this.mExtraViewList.size()) {
                    this.mExtraViewList.get(i4).setVisibility(8);
                    i = i4 + 1;
                } else {
                    this.mInfoOffset = 0.0f;
                    return;
                }
            }
        } else {
            if (this.mCardType == 2) {
                if (this.mInfoVisibility == 2) {
                    animateInfoHeight(visible);
                    return;
                }
                for (int i5 = 0; i5 < this.mInfoViewList.size(); i5++) {
                    this.mInfoViewList.get(i5).setVisibility(visible ? 0 : 8);
                }
                return;
            }
            int i6 = this.mCardType;
            if (i6 == 1) {
                animateInfoAlpha(visible);
            }
        }
    }

    private void applySelectedState(boolean focused) {
        removeCallbacks(this.mAnimationTrigger);
        if (this.mCardType == 3) {
            if (focused) {
                if (!this.mDelaySelectedAnim) {
                    post(this.mAnimationTrigger);
                    this.mDelaySelectedAnim = true;
                    return;
                } else {
                    postDelayed(this.mAnimationTrigger, this.mSelectedAnimationDelay);
                    return;
                }
            }
            animateInfoOffset(false);
            return;
        }
        if (this.mInfoVisibility == 2) {
            setInfoViewVisibility(focused);
        }
    }

    public void cancelAnimations() {
        if (this.mAnim != null) {
            this.mAnim.cancel();
            this.mAnim = null;
            clearAnimation();
        }
    }

    void animateInfoOffset(boolean shown) {
        cancelAnimations();
        int i = 0;
        if (shown) {
            int widthSpec = View.MeasureSpec.makeMeasureSpec(this.mMeasuredWidth, 1073741824);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            int extraHeight = 0;
            for (int extraHeight2 = 0; extraHeight2 < this.mExtraViewList.size(); extraHeight2++) {
                View extraView = this.mExtraViewList.get(extraHeight2);
                extraView.setVisibility(0);
                extraView.measure(widthSpec, heightSpec);
                extraHeight = Math.max(extraHeight, extraView.getMeasuredHeight());
            }
            i = extraHeight;
        }
        this.mAnim = new InfoOffsetAnimation(this.mInfoOffset, shown ? i : 0.0f);
        this.mAnim.setDuration(this.mSelectedAnimDuration);
        this.mAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        this.mAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (BaseCardView.this.mInfoOffset == 0.0f) {
                    for (int i2 = 0; i2 < BaseCardView.this.mExtraViewList.size(); i2++) {
                        BaseCardView.this.mExtraViewList.get(i2).setVisibility(8);
                    }
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        startAnimation(this.mAnim);
    }

    private void animateInfoHeight(boolean shown) {
        cancelAnimations();
        if (shown) {
            for (int i = 0; i < this.mInfoViewList.size(); i++) {
                View extraView = this.mInfoViewList.get(i);
                extraView.setVisibility(0);
            }
        }
        float targetFraction = shown ? 1.0f : 0.0f;
        if (this.mInfoVisFraction == targetFraction) {
            return;
        }
        this.mAnim = new InfoHeightAnimation(this.mInfoVisFraction, targetFraction);
        this.mAnim.setDuration(this.mSelectedAnimDuration);
        this.mAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        this.mAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (BaseCardView.this.mInfoVisFraction == 0.0f) {
                    for (int i2 = 0; i2 < BaseCardView.this.mInfoViewList.size(); i2++) {
                        BaseCardView.this.mInfoViewList.get(i2).setVisibility(8);
                    }
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        startAnimation(this.mAnim);
    }

    private void animateInfoAlpha(boolean shown) {
        cancelAnimations();
        if (shown) {
            for (int i = 0; i < this.mInfoViewList.size(); i++) {
                this.mInfoViewList.get(i).setVisibility(0);
            }
        }
        float targetAlpha = shown ? 1.0f : 0.0f;
        if (targetAlpha == this.mInfoAlpha) {
            return;
        }
        this.mAnim = new InfoAlphaAnimation(this.mInfoAlpha, shown ? 1.0f : 0.0f);
        this.mAnim.setDuration(this.mActivatedAnimDuration);
        this.mAnim.setInterpolator(new DecelerateInterpolator());
        this.mAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (BaseCardView.this.mInfoAlpha == 0.0d) {
                    for (int i2 = 0; i2 < BaseCardView.this.mInfoViewList.size(); i2++) {
                        BaseCardView.this.mInfoViewList.get(i2).setVisibility(8);
                    }
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        startAnimation(this.mAnim);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    @Override
    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {

        @ViewDebug.ExportedProperty(category = "layout", mapping = {@ViewDebug.IntToString(from = 0, to = "MAIN"), @ViewDebug.IntToString(from = 1, to = "INFO"), @ViewDebug.IntToString(from = 2, to = "EXTRA")})
        public int viewType;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.viewType = 0;
            TypedArray a = c.obtainStyledAttributes(attrs, android.support.v17.leanback.R.styleable.lbBaseCardView_Layout);
            this.viewType = a.getInt(android.support.v17.leanback.R.styleable.lbBaseCardView_Layout_layout_viewType, 0);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.viewType = 0;
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
            this.viewType = 0;
        }

        public LayoutParams(LayoutParams source) {
            super((ViewGroup.MarginLayoutParams) source);
            this.viewType = 0;
            this.viewType = source.viewType;
        }
    }

    class AnimationBase extends Animation {
        AnimationBase() {
        }

        final void mockStart() {
            getTransformation(0L, null);
        }

        final void mockEnd() {
            applyTransformation(1.0f, null);
            BaseCardView.this.cancelAnimations();
        }
    }

    final class InfoOffsetAnimation extends AnimationBase {
        private float mDelta;
        private float mStartValue;

        public InfoOffsetAnimation(float start, float end) {
            super();
            this.mStartValue = start;
            this.mDelta = end - start;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            BaseCardView.this.mInfoOffset = this.mStartValue + (this.mDelta * interpolatedTime);
            BaseCardView.this.requestLayout();
        }
    }

    final class InfoHeightAnimation extends AnimationBase {
        private float mDelta;
        private float mStartValue;

        public InfoHeightAnimation(float start, float end) {
            super();
            this.mStartValue = start;
            this.mDelta = end - start;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            BaseCardView.this.mInfoVisFraction = this.mStartValue + (this.mDelta * interpolatedTime);
            BaseCardView.this.requestLayout();
        }
    }

    final class InfoAlphaAnimation extends AnimationBase {
        private float mDelta;
        private float mStartValue;

        public InfoAlphaAnimation(float start, float end) {
            super();
            this.mStartValue = start;
            this.mDelta = end - start;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            BaseCardView.this.mInfoAlpha = this.mStartValue + (this.mDelta * interpolatedTime);
            for (int i = 0; i < BaseCardView.this.mInfoViewList.size(); i++) {
                BaseCardView.this.mInfoViewList.get(i).setAlpha(BaseCardView.this.mInfoAlpha);
            }
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
