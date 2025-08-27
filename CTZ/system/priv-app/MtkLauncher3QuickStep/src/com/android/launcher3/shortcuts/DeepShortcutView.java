package com.android.launcher3.shortcuts;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.popup.PopupContainerWithArrow;
import com.android.launcher3.touch.ItemClickHandler;

/* loaded from: classes.dex */
public class DeepShortcutView extends FrameLayout {
    private static final Point sTempPoint = new Point();
    private BubbleTextView mBubbleText;
    private ShortcutInfoCompat mDetail;
    private View mDivider;
    private View mIconView;
    private ShortcutInfo mInfo;
    private final Rect mPillRect;

    public DeepShortcutView(Context context) {
        this(context, null, 0);
    }

    public DeepShortcutView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public DeepShortcutView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mPillRect = new Rect();
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mBubbleText = (BubbleTextView) findViewById(R.id.bubble_text);
        this.mIconView = findViewById(R.id.icon);
        this.mDivider = findViewById(R.id.divider);
    }

    public void setDividerVisibility(int i) {
        this.mDivider.setVisibility(i);
    }

    public BubbleTextView getBubbleText() {
        return this.mBubbleText;
    }

    public void setWillDrawIcon(boolean z) {
        this.mIconView.setVisibility(z ? 0 : 4);
    }

    public boolean willDrawIcon() {
        return this.mIconView.getVisibility() == 0;
    }

    public Point getIconCenter() {
        Point point = sTempPoint;
        Point point2 = sTempPoint;
        int measuredHeight = getMeasuredHeight() / 2;
        point2.x = measuredHeight;
        point.y = measuredHeight;
        if (Utilities.isRtl(getResources())) {
            sTempPoint.x = getMeasuredWidth() - sTempPoint.x;
        }
        return sTempPoint;
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        this.mPillRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }

    public void applyShortcutInfo(ShortcutInfo shortcutInfo, ShortcutInfoCompat shortcutInfoCompat, PopupContainerWithArrow popupContainerWithArrow) {
        this.mInfo = shortcutInfo;
        this.mDetail = shortcutInfoCompat;
        this.mBubbleText.applyFromShortcutInfo(shortcutInfo);
        this.mIconView.setBackground(this.mBubbleText.getIcon());
        CharSequence longLabel = this.mDetail.getLongLabel();
        boolean z = !TextUtils.isEmpty(longLabel) && this.mBubbleText.getPaint().measureText(longLabel.toString()) <= ((float) ((this.mBubbleText.getWidth() - this.mBubbleText.getTotalPaddingLeft()) - this.mBubbleText.getTotalPaddingRight()));
        BubbleTextView bubbleTextView = this.mBubbleText;
        if (!z) {
            longLabel = this.mDetail.getShortLabel();
        }
        bubbleTextView.setText(longLabel);
        this.mBubbleText.setOnClickListener(ItemClickHandler.INSTANCE);
        this.mBubbleText.setOnLongClickListener(popupContainerWithArrow);
        this.mBubbleText.setOnTouchListener(popupContainerWithArrow);
    }

    public ShortcutInfo getFinalInfo() {
        ShortcutInfo shortcutInfo = new ShortcutInfo(this.mInfo);
        Launcher.getLauncher(getContext()).getModel().updateAndBindShortcutInfo(shortcutInfo, this.mDetail);
        return shortcutInfo;
    }

    public View getIconView() {
        return this.mIconView;
    }
}
