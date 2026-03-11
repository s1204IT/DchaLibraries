package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import java.util.Objects;

public class QSIconView extends ViewGroup {
    private boolean mAnimationEnabled;
    protected final View mIcon;
    protected final int mIconSizePx;
    protected final int mTilePaddingBelowIconPx;

    public QSIconView(Context context) {
        super(context);
        this.mAnimationEnabled = true;
        Resources res = context.getResources();
        this.mIconSizePx = res.getDimensionPixelSize(R.dimen.qs_tile_icon_size);
        this.mTilePaddingBelowIconPx = res.getDimensionPixelSize(R.dimen.qs_tile_padding_below_icon);
        this.mIcon = createIcon();
        addView(this.mIcon);
    }

    public void disableAnimation() {
        this.mAnimationEnabled = false;
    }

    public View getIconView() {
        return this.mIcon;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = View.MeasureSpec.getSize(widthMeasureSpec);
        int iconSpec = exactly(this.mIconSizePx);
        this.mIcon.measure(View.MeasureSpec.makeMeasureSpec(w, getIconMeasureMode()), iconSpec);
        setMeasuredDimension(w, this.mIcon.getMeasuredHeight() + this.mTilePaddingBelowIconPx);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = getMeasuredWidth();
        getMeasuredHeight();
        int iconLeft = (w - this.mIcon.getMeasuredWidth()) / 2;
        layout(this.mIcon, iconLeft, 0);
    }

    public void setIcon(QSTile.State state) {
        setIcon((ImageView) this.mIcon, state);
    }

    protected void setIcon(ImageView iv, QSTile.State state) {
        Drawable d;
        if (!Objects.equals(state.icon, iv.getTag(R.id.qs_icon_tag))) {
            if (state.icon != null) {
                d = (iv.isShown() && this.mAnimationEnabled) ? state.icon.getDrawable(this.mContext) : state.icon.getInvisibleDrawable(this.mContext);
            } else {
                d = null;
            }
            int padding = state.icon != null ? state.icon.getPadding() : 0;
            if (d != null && state.autoMirrorDrawable) {
                d.setAutoMirrored(true);
            }
            iv.setImageDrawable(d);
            iv.setTag(R.id.qs_icon_tag, state.icon);
            iv.setPadding(0, padding, 0, padding);
            if ((d instanceof Animatable) && iv.isShown()) {
                Animatable a = (Animatable) d;
                a.start();
                if (!iv.isShown()) {
                    a.stop();
                }
            }
        }
        if (state.disabledByPolicy) {
            iv.setColorFilter(getContext().getColor(R.color.qs_tile_disabled_color));
        } else {
            iv.clearColorFilter();
        }
    }

    protected int getIconMeasureMode() {
        return 1073741824;
    }

    protected View createIcon() {
        ImageView icon = new ImageView(this.mContext);
        icon.setId(android.R.id.icon);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return icon;
    }

    protected final int exactly(int size) {
        return View.MeasureSpec.makeMeasureSpec(size, 1073741824);
    }

    protected final void layout(View child, int left, int top) {
        child.layout(left, top, child.getMeasuredWidth() + left, child.getMeasuredHeight() + top);
    }
}
