package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.MathUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import java.util.Objects;

public class QSTileView extends ViewGroup {
    private static final Typeface CONDENSED = Typeface.create("sans-serif-condensed", 0);
    private View.OnClickListener mClickPrimary;
    private View.OnClickListener mClickSecondary;
    protected final Context mContext;
    private final View mDivider;
    private boolean mDual;
    private QSDualTileLabel mDualLabel;
    private final int mDualTileVerticalPaddingPx;
    private final H mHandler;
    private final View mIcon;
    private final int mIconSizePx;
    private TextView mLabel;
    private View.OnLongClickListener mLongClick;
    private RippleDrawable mRipple;
    private Drawable mTileBackground;
    private final int mTilePaddingBelowIconPx;
    private int mTilePaddingTopPx;
    private final int mTileSpacingPx;
    private final View mTopBackgroundView;

    public QSTileView(Context context) {
        super(context);
        this.mHandler = new H();
        this.mContext = context;
        Resources res = context.getResources();
        this.mIconSizePx = res.getDimensionPixelSize(R.dimen.qs_tile_icon_size);
        this.mTileSpacingPx = res.getDimensionPixelSize(R.dimen.qs_tile_spacing);
        this.mTilePaddingBelowIconPx = res.getDimensionPixelSize(R.dimen.qs_tile_padding_below_icon);
        this.mDualTileVerticalPaddingPx = res.getDimensionPixelSize(R.dimen.qs_dual_tile_padding_vertical);
        this.mTileBackground = newTileBackground();
        recreateLabel();
        setClipChildren(false);
        this.mTopBackgroundView = new View(context);
        addView(this.mTopBackgroundView);
        this.mIcon = createIcon();
        addView(this.mIcon);
        this.mDivider = new View(this.mContext);
        this.mDivider.setBackgroundColor(res.getColor(R.color.qs_tile_divider));
        int dh = res.getDimensionPixelSize(R.dimen.qs_tile_divider_height);
        this.mDivider.setLayoutParams(new ViewGroup.LayoutParams(-1, dh));
        addView(this.mDivider);
        setClickable(true);
        updateTopPadding();
    }

    private void updateTopPadding() {
        Resources res = getResources();
        int padding = res.getDimensionPixelSize(R.dimen.qs_tile_padding_top);
        int largePadding = res.getDimensionPixelSize(R.dimen.qs_tile_padding_top_large_text);
        float largeFactor = (MathUtils.constrain(getResources().getConfiguration().fontScale, 1.0f, 1.3f) - 1.0f) / 0.29999995f;
        this.mTilePaddingTopPx = Math.round(((1.0f - largeFactor) * padding) + (largePadding * largeFactor));
        requestLayout();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateTopPadding();
        FontSizeUtils.updateFontSize(this.mLabel, R.dimen.qs_tile_text_size);
        if (this.mDualLabel != null) {
            this.mDualLabel.setTextSize(0, getResources().getDimensionPixelSize(R.dimen.qs_tile_text_size));
        }
    }

    private void recreateLabel() {
        CharSequence labelText = null;
        CharSequence labelDescription = null;
        if (this.mLabel != null) {
            labelText = this.mLabel.getText();
            removeView(this.mLabel);
            this.mLabel = null;
        }
        if (this.mDualLabel != null) {
            labelText = this.mDualLabel.getText();
            labelDescription = this.mLabel.getContentDescription();
            removeView(this.mDualLabel);
            this.mDualLabel = null;
        }
        Resources res = this.mContext.getResources();
        if (this.mDual) {
            this.mDualLabel = new QSDualTileLabel(this.mContext);
            this.mDualLabel.setId(android.R.id.title);
            this.mDualLabel.setBackgroundResource(R.drawable.btn_borderless_rect);
            this.mDualLabel.setFirstLineCaret(res.getDrawable(R.drawable.qs_dual_tile_caret));
            this.mDualLabel.setTextColor(res.getColor(R.color.qs_tile_text));
            this.mDualLabel.setPadding(0, this.mDualTileVerticalPaddingPx, 0, this.mDualTileVerticalPaddingPx);
            this.mDualLabel.setTypeface(CONDENSED);
            this.mDualLabel.setTextSize(0, res.getDimensionPixelSize(R.dimen.qs_tile_text_size));
            this.mDualLabel.setClickable(true);
            this.mDualLabel.setOnClickListener(this.mClickSecondary);
            this.mDualLabel.setFocusable(true);
            if (labelText != null) {
                this.mDualLabel.setText(labelText);
            }
            if (labelDescription != null) {
                this.mDualLabel.setContentDescription(labelDescription);
            }
            addView(this.mDualLabel);
            return;
        }
        this.mLabel = new TextView(this.mContext);
        this.mLabel.setId(android.R.id.title);
        this.mLabel.setTextColor(res.getColor(R.color.qs_tile_text));
        this.mLabel.setGravity(1);
        this.mLabel.setMinLines(2);
        this.mLabel.setPadding(0, 0, 0, 0);
        this.mLabel.setTypeface(CONDENSED);
        this.mLabel.setTextSize(0, res.getDimensionPixelSize(R.dimen.qs_tile_text_size));
        this.mLabel.setClickable(false);
        if (labelText != null) {
            this.mLabel.setText(labelText);
        }
        addView(this.mLabel);
    }

    public boolean setDual(boolean dual) {
        boolean changed = dual != this.mDual;
        this.mDual = dual;
        if (changed) {
            recreateLabel();
        }
        if (this.mTileBackground instanceof RippleDrawable) {
            setRipple((RippleDrawable) this.mTileBackground);
        }
        if (dual) {
            this.mTopBackgroundView.setOnClickListener(this.mClickPrimary);
            setOnClickListener(null);
            setClickable(false);
            setImportantForAccessibility(2);
            this.mTopBackgroundView.setBackground(this.mTileBackground);
        } else {
            this.mTopBackgroundView.setOnClickListener(null);
            this.mTopBackgroundView.setClickable(false);
            setOnClickListener(this.mClickPrimary);
            setOnLongClickListener(this.mLongClick);
            setImportantForAccessibility(1);
            setBackground(this.mTileBackground);
        }
        this.mTopBackgroundView.setFocusable(dual);
        setFocusable(dual ? false : true);
        this.mDivider.setVisibility(dual ? 0 : 8);
        postInvalidate();
        return changed;
    }

    private void setRipple(RippleDrawable tileBackground) {
        this.mRipple = tileBackground;
        if (getWidth() != 0) {
            updateRippleSize(getWidth(), getHeight());
        }
    }

    public void init(View.OnClickListener clickPrimary, View.OnClickListener clickSecondary, View.OnLongClickListener longClick) {
        this.mClickPrimary = clickPrimary;
        this.mClickSecondary = clickSecondary;
        this.mLongClick = longClick;
    }

    protected View createIcon() {
        ImageView icon = new ImageView(this.mContext);
        icon.setId(android.R.id.icon);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        return icon;
    }

    private Drawable newTileBackground() {
        int[] attrs = {android.R.attr.selectableItemBackgroundBorderless};
        TypedArray ta = this.mContext.obtainStyledAttributes(attrs);
        Drawable d = ta.getDrawable(0);
        ta.recycle();
        return d;
    }

    private View labelView() {
        return this.mDual ? this.mDualLabel : this.mLabel;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = View.MeasureSpec.getSize(widthMeasureSpec);
        int h = View.MeasureSpec.getSize(heightMeasureSpec);
        int iconSpec = exactly(this.mIconSizePx);
        this.mIcon.measure(View.MeasureSpec.makeMeasureSpec(w, Integer.MIN_VALUE), iconSpec);
        labelView().measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(h, Integer.MIN_VALUE));
        if (this.mDual) {
            this.mDivider.measure(widthMeasureSpec, exactly(this.mDivider.getLayoutParams().height));
        }
        int heightSpec = exactly(this.mIconSizePx + this.mTilePaddingBelowIconPx + this.mTilePaddingTopPx);
        this.mTopBackgroundView.measure(widthMeasureSpec, heightSpec);
        setMeasuredDimension(w, h);
    }

    private static int exactly(int size) {
        return View.MeasureSpec.makeMeasureSpec(size, 1073741824);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        layout(this.mTopBackgroundView, 0, this.mTileSpacingPx);
        int top = 0 + this.mTileSpacingPx;
        int top2 = top + this.mTilePaddingTopPx;
        int iconLeft = (w - this.mIcon.getMeasuredWidth()) / 2;
        layout(this.mIcon, iconLeft, top2);
        if (this.mRipple != null) {
            updateRippleSize(w, h);
        }
        int top3 = this.mIcon.getBottom();
        int top4 = top3 + this.mTilePaddingBelowIconPx;
        if (this.mDual) {
            layout(this.mDivider, 0, top4);
            top4 = this.mDivider.getBottom();
        }
        layout(labelView(), 0, top4);
    }

    private void updateRippleSize(int width, int height) {
        int cx = width / 2;
        int cy = this.mDual ? this.mIcon.getTop() + (this.mIcon.getHeight() / 2) : height / 2;
        int rad = (int) (this.mIcon.getHeight() * 1.25f);
        this.mRipple.setHotspotBounds(cx - rad, cy - rad, cx + rad, cy + rad);
    }

    private static void layout(View child, int left, int top) {
        child.layout(left, top, child.getMeasuredWidth() + left, child.getMeasuredHeight() + top);
    }

    protected void handleStateChanged(QSTile.State state) {
        if (this.mIcon instanceof ImageView) {
            setIcon((ImageView) this.mIcon, state);
        }
        if (this.mDual) {
            this.mDualLabel.setText(state.label);
            this.mDualLabel.setContentDescription(state.dualLabelContentDescription);
            this.mTopBackgroundView.setContentDescription(state.contentDescription);
        } else {
            this.mLabel.setText(state.label);
            setContentDescription(state.contentDescription);
        }
    }

    protected void setIcon(ImageView imageView, QSTile.State state) {
        if (!Objects.equals(state.icon, imageView.getTag(R.id.qs_icon_tag))) {
            Drawable drawable = state.icon != null ? state.icon.getDrawable(this.mContext) : 0;
            if (drawable != 0 && state.autoMirrorDrawable) {
                drawable.setAutoMirrored(true);
            }
            imageView.setImageDrawable(drawable);
            imageView.setTag(R.id.qs_icon_tag, state.icon);
            if ((drawable instanceof Animatable) && !imageView.isShown()) {
                ((Animatable) drawable).stop();
            }
        }
    }

    public void onStateChanged(QSTile.State state) {
        this.mHandler.obtainMessage(1, state).sendToTarget();
    }

    private class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                QSTileView.this.handleStateChanged((QSTile.State) msg.obj);
            }
        }
    }
}
