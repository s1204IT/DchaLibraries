package androidx.car.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.car.R;

public class PagedScrollBarView extends ViewGroup {
    private final TextView mAlphaJumpButton;
    private final AlphaJumpButtonClickListener mAlphaJumpButtonClickListener;
    private int mCustomThumbBackgroundResId;
    private int mDayNightStyle;
    private final ImageView mDownButton;
    private final PaginateButtonClickListener mDownButtonClickListener;
    private final Interpolator mPaginationInterpolator;
    private final int mScrollBarThumbWidth;
    private final View mScrollThumb;
    private int mScrollThumbTrackHeight;
    private final int mSeparatingMargin;
    private final ImageView mUpButton;
    private final PaginateButtonClickListener mUpButtonClickListener;
    private boolean mUseCustomThumbBackground;

    public interface PaginationListener {
        void onAlphaJump();

        void onPaginate(int i);
    }

    public PagedScrollBarView(Context context) {
        super(context);
        this.mPaginationInterpolator = new AccelerateDecelerateInterpolator();
        Resources res = getResources();
        this.mSeparatingMargin = res.getDimensionPixelSize(R.dimen.car_padding_2);
        this.mScrollBarThumbWidth = res.getDimensionPixelSize(R.dimen.car_scroll_bar_thumb_width);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        inflater.inflate(R.layout.car_paged_scrollbar_buttons, (ViewGroup) this, true);
        this.mUpButtonClickListener = new PaginateButtonClickListener(0);
        this.mDownButtonClickListener = new PaginateButtonClickListener(1);
        this.mAlphaJumpButtonClickListener = new AlphaJumpButtonClickListener();
        this.mUpButton = (ImageView) findViewById(R.id.page_up);
        this.mUpButton.setOnClickListener(this.mUpButtonClickListener);
        this.mDownButton = (ImageView) findViewById(R.id.page_down);
        this.mDownButton.setOnClickListener(this.mDownButtonClickListener);
        this.mAlphaJumpButton = (TextView) findViewById(R.id.alpha_jump);
        this.mAlphaJumpButton.setOnClickListener(this.mAlphaJumpButtonClickListener);
        this.mScrollThumb = findViewById(R.id.scrollbar_thumb);
    }

    public PagedScrollBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPaginationInterpolator = new AccelerateDecelerateInterpolator();
        Resources res = getResources();
        this.mSeparatingMargin = res.getDimensionPixelSize(R.dimen.car_padding_2);
        this.mScrollBarThumbWidth = res.getDimensionPixelSize(R.dimen.car_scroll_bar_thumb_width);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        inflater.inflate(R.layout.car_paged_scrollbar_buttons, (ViewGroup) this, true);
        this.mUpButtonClickListener = new PaginateButtonClickListener(0);
        this.mDownButtonClickListener = new PaginateButtonClickListener(1);
        this.mAlphaJumpButtonClickListener = new AlphaJumpButtonClickListener();
        this.mUpButton = (ImageView) findViewById(R.id.page_up);
        this.mUpButton.setOnClickListener(this.mUpButtonClickListener);
        this.mDownButton = (ImageView) findViewById(R.id.page_down);
        this.mDownButton.setOnClickListener(this.mDownButtonClickListener);
        this.mAlphaJumpButton = (TextView) findViewById(R.id.alpha_jump);
        this.mAlphaJumpButton.setOnClickListener(this.mAlphaJumpButtonClickListener);
        this.mScrollThumb = findViewById(R.id.scrollbar_thumb);
    }

    public PagedScrollBarView(Context context, AttributeSet attrs, int defStyleAttrs) {
        super(context, attrs, defStyleAttrs);
        this.mPaginationInterpolator = new AccelerateDecelerateInterpolator();
        Resources res = getResources();
        this.mSeparatingMargin = res.getDimensionPixelSize(R.dimen.car_padding_2);
        this.mScrollBarThumbWidth = res.getDimensionPixelSize(R.dimen.car_scroll_bar_thumb_width);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        inflater.inflate(R.layout.car_paged_scrollbar_buttons, (ViewGroup) this, true);
        this.mUpButtonClickListener = new PaginateButtonClickListener(0);
        this.mDownButtonClickListener = new PaginateButtonClickListener(1);
        this.mAlphaJumpButtonClickListener = new AlphaJumpButtonClickListener();
        this.mUpButton = (ImageView) findViewById(R.id.page_up);
        this.mUpButton.setOnClickListener(this.mUpButtonClickListener);
        this.mDownButton = (ImageView) findViewById(R.id.page_down);
        this.mDownButton.setOnClickListener(this.mDownButtonClickListener);
        this.mAlphaJumpButton = (TextView) findViewById(R.id.alpha_jump);
        this.mAlphaJumpButton.setOnClickListener(this.mAlphaJumpButtonClickListener);
        this.mScrollThumb = findViewById(R.id.scrollbar_thumb);
    }

    public PagedScrollBarView(Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        super(context, attrs, defStyleAttrs, defStyleRes);
        this.mPaginationInterpolator = new AccelerateDecelerateInterpolator();
        Resources res = getResources();
        this.mSeparatingMargin = res.getDimensionPixelSize(R.dimen.car_padding_2);
        this.mScrollBarThumbWidth = res.getDimensionPixelSize(R.dimen.car_scroll_bar_thumb_width);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        inflater.inflate(R.layout.car_paged_scrollbar_buttons, (ViewGroup) this, true);
        this.mUpButtonClickListener = new PaginateButtonClickListener(0);
        this.mDownButtonClickListener = new PaginateButtonClickListener(1);
        this.mAlphaJumpButtonClickListener = new AlphaJumpButtonClickListener();
        this.mUpButton = (ImageView) findViewById(R.id.page_up);
        this.mUpButton.setOnClickListener(this.mUpButtonClickListener);
        this.mDownButton = (ImageView) findViewById(R.id.page_down);
        this.mDownButton.setOnClickListener(this.mDownButtonClickListener);
        this.mAlphaJumpButton = (TextView) findViewById(R.id.alpha_jump);
        this.mAlphaJumpButton.setOnClickListener(this.mAlphaJumpButtonClickListener);
        this.mScrollThumb = findViewById(R.id.scrollbar_thumb);
    }

    public void setUpButtonIcon(Drawable icon) {
        this.mUpButton.setImageDrawable(icon);
    }

    public void setDownButtonIcon(Drawable icon) {
        this.mDownButton.setImageDrawable(icon);
    }

    public void setPaginationListener(PaginationListener listener) {
        this.mUpButtonClickListener.setPaginationListener(listener);
        this.mDownButtonClickListener.setPaginationListener(listener);
        this.mAlphaJumpButtonClickListener.setPaginationListener(listener);
    }

    public boolean isUpPressed() {
        return this.mUpButton.isPressed();
    }

    public boolean isDownPressed() {
        return this.mDownButton.isPressed();
    }

    void setShowAlphaJump(boolean show) {
        this.mAlphaJumpButton.setVisibility(show ? 0 : 8);
    }

    public void setParameters(int range, int offset, int extent, boolean animate) {
        if (!isLaidOut() || getVisibility() == 8 || range == 0) {
            return;
        }
        int thumbLength = calculateScrollThumbLength(range, extent);
        int thumbOffset = calculateScrollThumbOffset(range, offset, thumbLength);
        ViewGroup.LayoutParams lp = this.mScrollThumb.getLayoutParams();
        if (lp.height != thumbLength) {
            lp.height = thumbLength;
            this.mScrollThumb.requestLayout();
        }
        moveY(this.mScrollThumb, thumbOffset, animate);
    }

    void setParametersInLayout(int range, int offset, int extent) {
        if (getVisibility() == 8 || range == 0) {
            return;
        }
        int thumbLength = calculateScrollThumbLength(range, extent);
        int thumbOffset = calculateScrollThumbOffset(range, offset, thumbLength);
        ViewGroup.LayoutParams lp = this.mScrollThumb.getLayoutParams();
        if (lp.height != thumbLength) {
            lp.height = thumbLength;
            measureAndLayoutScrollThumb();
        }
        this.mScrollThumb.setY(thumbOffset);
    }

    public void setDayNightStyle(int dayNightStyle) {
        this.mDayNightStyle = dayNightStyle;
        reloadColors();
    }

    public void setUpEnabled(boolean enabled) {
        this.mUpButton.setEnabled(enabled);
        this.mUpButton.setAlpha(enabled ? 1.0f : 0.2f);
    }

    public void setDownEnabled(boolean enabled) {
        this.mDownButton.setEnabled(enabled);
        this.mDownButton.setAlpha(enabled ? 1.0f : 0.2f);
    }

    public boolean isDownEnabled() {
        return this.mDownButton.isEnabled();
    }

    public void setThumbColor(int color) {
        this.mUseCustomThumbBackground = true;
        this.mCustomThumbBackgroundResId = color;
        reloadColors();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int requestedWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int requestedHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        int wrapMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        this.mUpButton.measure(wrapMeasureSpec, wrapMeasureSpec);
        this.mDownButton.measure(wrapMeasureSpec, wrapMeasureSpec);
        measureScrollThumb();
        if (this.mAlphaJumpButton.getVisibility() != 8) {
            this.mAlphaJumpButton.measure(wrapMeasureSpec, wrapMeasureSpec);
        }
        setMeasuredDimension(requestedWidth, requestedHeight);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        layoutViewCenteredFromTop(this.mUpButton, getPaddingTop(), width);
        int layoutTop = this.mUpButton.getBottom();
        if (this.mAlphaJumpButton.getVisibility() != 8) {
            layoutViewCenteredFromTop(this.mAlphaJumpButton, layoutTop + this.mSeparatingMargin, width);
            layoutTop = this.mAlphaJumpButton.getBottom();
        }
        layoutViewCenteredFromTop(this.mScrollThumb, layoutTop + this.mSeparatingMargin, width);
        int downBottom = height - getPaddingBottom();
        layoutViewCenteredFromBottom(this.mDownButton, downBottom, width);
        calculateScrollThumbTrackHeight();
    }

    private void calculateScrollThumbTrackHeight() {
        this.mScrollThumbTrackHeight = this.mDownButton.getTop() - (2 * this.mSeparatingMargin);
        if (this.mAlphaJumpButton.getVisibility() != 8) {
            this.mScrollThumbTrackHeight -= this.mAlphaJumpButton.getBottom();
        } else {
            this.mScrollThumbTrackHeight -= this.mUpButton.getBottom();
        }
    }

    private void measureScrollThumb() {
        int scrollWidth = View.MeasureSpec.makeMeasureSpec(this.mScrollBarThumbWidth, 1073741824);
        int scrollHeight = View.MeasureSpec.makeMeasureSpec(this.mScrollThumb.getLayoutParams().height, 1073741824);
        this.mScrollThumb.measure(scrollWidth, scrollHeight);
    }

    private void measureAndLayoutScrollThumb() {
        measureScrollThumb();
        int layoutTop = this.mScrollThumb.getTop();
        layoutViewCenteredFromTop(this.mScrollThumb, layoutTop, getMeasuredWidth());
    }

    private void layoutViewCenteredFromTop(View view, int top, int availableWidth) {
        int viewWidth = view.getMeasuredWidth();
        int viewLeft = (availableWidth - viewWidth) / 2;
        view.layout(viewLeft, top, viewLeft + viewWidth, view.getMeasuredHeight() + top);
    }

    private void layoutViewCenteredFromBottom(View view, int bottom, int availableWidth) {
        int viewWidth = view.getMeasuredWidth();
        int viewLeft = (availableWidth - viewWidth) / 2;
        view.layout(viewLeft, bottom - view.getMeasuredHeight(), viewLeft + viewWidth, bottom);
    }

    private void reloadColors() {
        int tintResId;
        int thumbColorResId;
        int upDownBackgroundResId;
        switch (this.mDayNightStyle) {
            case 0:
                tintResId = R.color.car_tint;
                thumbColorResId = R.color.car_scrollbar_thumb;
                upDownBackgroundResId = R.drawable.car_button_ripple_background;
                break;
            case 1:
                tintResId = R.color.car_tint_inverse;
                thumbColorResId = R.color.car_scrollbar_thumb_inverse;
                upDownBackgroundResId = R.drawable.car_button_ripple_background_inverse;
                break;
            case 2:
            case 4:
                tintResId = R.color.car_tint_light;
                thumbColorResId = R.color.car_scrollbar_thumb_light;
                upDownBackgroundResId = R.drawable.car_button_ripple_background_night;
                break;
            case 3:
            case 5:
                tintResId = R.color.car_tint_dark;
                thumbColorResId = R.color.car_scrollbar_thumb_dark;
                upDownBackgroundResId = R.drawable.car_button_ripple_background_day;
                break;
            default:
                throw new IllegalArgumentException("Unknown DayNightStyle: " + this.mDayNightStyle);
        }
        if (this.mUseCustomThumbBackground) {
            thumbColorResId = this.mCustomThumbBackgroundResId;
        }
        setScrollbarThumbColor(thumbColorResId);
        int tint = ContextCompat.getColor(getContext(), tintResId);
        this.mUpButton.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        this.mUpButton.setBackgroundResource(upDownBackgroundResId);
        this.mDownButton.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        this.mDownButton.setBackgroundResource(upDownBackgroundResId);
        this.mAlphaJumpButton.setBackgroundResource(upDownBackgroundResId);
    }

    private void setScrollbarThumbColor(int color) {
        GradientDrawable background = (GradientDrawable) this.mScrollThumb.getBackground();
        background.setColor(getContext().getColor(color));
    }

    int getScrollbarThumbColor() {
        return ((GradientDrawable) this.mScrollThumb.getBackground()).getColor().getDefaultColor();
    }

    private int calculateScrollThumbLength(int range, int extent) {
        return Math.round((extent / range) * this.mScrollThumbTrackHeight);
    }

    private int calculateScrollThumbOffset(int range, int offset, int thumbLength) {
        return this.mScrollThumb.getTop() + (isDownEnabled() ? Math.round((offset / range) * this.mScrollThumbTrackHeight) : this.mScrollThumbTrackHeight - thumbLength);
    }

    private void moveY(View view, float newPosition, boolean animate) {
        int duration = animate ? 200 : 0;
        view.animate().y(newPosition).setDuration(duration).setInterpolator(this.mPaginationInterpolator).start();
    }

    private static class PaginateButtonClickListener implements View.OnClickListener {
        private final int mPaginateDirection;
        private PaginationListener mPaginationListener;

        PaginateButtonClickListener(int paginateDirection) {
            this.mPaginateDirection = paginateDirection;
        }

        public void setPaginationListener(PaginationListener listener) {
            this.mPaginationListener = listener;
        }

        @Override
        public void onClick(View v) {
            if (this.mPaginationListener != null) {
                this.mPaginationListener.onPaginate(this.mPaginateDirection);
            }
        }
    }

    private static class AlphaJumpButtonClickListener implements View.OnClickListener {
        private PaginationListener mPaginationListener;

        private AlphaJumpButtonClickListener() {
        }

        public void setPaginationListener(PaginationListener listener) {
            this.mPaginationListener = listener;
        }

        @Override
        public void onClick(View v) {
            if (this.mPaginationListener != null) {
                this.mPaginationListener.onAlphaJump();
            }
        }
    }
}
