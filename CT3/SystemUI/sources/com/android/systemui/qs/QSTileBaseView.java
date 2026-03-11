package com.android.systemui.qs;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.Switch;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

public class QSTileBaseView extends LinearLayout {
    private String mAccessibilityClass;
    private boolean mCollapsedView;
    private final H mHandler;
    private QSIconView mIcon;
    private RippleDrawable mRipple;
    private Drawable mTileBackground;
    private boolean mTileState;

    public QSTileBaseView(Context context, QSIconView icon, boolean collapsedView) {
        super(context);
        this.mHandler = new H();
        this.mIcon = icon;
        addView(this.mIcon);
        this.mTileBackground = newTileBackground();
        if (this.mTileBackground instanceof RippleDrawable) {
            setRipple((RippleDrawable) this.mTileBackground);
        }
        setImportantForAccessibility(1);
        setBackground(this.mTileBackground);
        int padding = context.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_padding);
        setPadding(0, padding, 0, padding);
        setClipChildren(false);
        setClipToPadding(false);
        this.mCollapsedView = collapsedView;
        setFocusable(true);
    }

    private Drawable newTileBackground() {
        int[] attrs = {android.R.attr.selectableItemBackgroundBorderless};
        TypedArray ta = this.mContext.obtainStyledAttributes(attrs);
        Drawable d = ta.getDrawable(0);
        ta.recycle();
        return d;
    }

    private void setRipple(RippleDrawable tileBackground) {
        this.mRipple = tileBackground;
        if (getWidth() == 0) {
            return;
        }
        updateRippleSize(getWidth(), getHeight());
    }

    private void updateRippleSize(int width, int height) {
        int cx = width / 2;
        int cy = height / 2;
        int rad = (int) (this.mIcon.getHeight() * 0.85f);
        this.mRipple.setHotspotBounds(cx - rad, cy - rad, cx + rad, cy + rad);
    }

    public void init(View.OnClickListener click, View.OnLongClickListener longClick) {
        setClickable(true);
        setOnClickListener(click);
        setOnLongClickListener(longClick);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        if (this.mRipple == null) {
            return;
        }
        updateRippleSize(w, h);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public View updateAccessibilityOrder(View previousView) {
        setAccessibilityTraversalAfter(previousView.getId());
        return this;
    }

    public void onStateChanged(QSTile.State state) {
        this.mHandler.obtainMessage(1, state).sendToTarget();
    }

    protected void handleStateChanged(QSTile.State state) {
        this.mIcon.setIcon(state);
        if (this.mCollapsedView && !TextUtils.isEmpty(state.minimalContentDescription)) {
            setContentDescription(state.minimalContentDescription);
        } else {
            setContentDescription(state.contentDescription);
        }
        if (this.mCollapsedView) {
            this.mAccessibilityClass = state.minimalAccessibilityClassName;
        } else {
            this.mAccessibilityClass = state.expandedAccessibilityClassName;
        }
        if (!(state instanceof QSTile.BooleanState)) {
            return;
        }
        this.mTileState = ((QSTile.BooleanState) state).value;
    }

    public QSIconView getIcon() {
        return this.mIcon;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (TextUtils.isEmpty(this.mAccessibilityClass)) {
            return;
        }
        event.setClassName(this.mAccessibilityClass);
        if (!Switch.class.getName().equals(this.mAccessibilityClass)) {
            return;
        }
        String label = getResources().getString(!this.mTileState ? R.string.switch_bar_on : R.string.switch_bar_off);
        event.setContentDescription(label);
        event.setChecked(!this.mTileState);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (TextUtils.isEmpty(this.mAccessibilityClass)) {
            return;
        }
        info.setClassName(this.mAccessibilityClass);
        if (!Switch.class.getName().equals(this.mAccessibilityClass)) {
            return;
        }
        String label = getResources().getString(this.mTileState ? R.string.switch_bar_on : R.string.switch_bar_off);
        info.setText(label);
        info.setChecked(this.mTileState);
        info.setCheckable(true);
    }

    private class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != 1) {
                return;
            }
            QSTileBaseView.this.handleStateChanged((QSTile.State) msg.obj);
        }
    }
}
