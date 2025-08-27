package com.android.settings.display;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;

/* loaded from: classes.dex */
public class ConversationMessageView extends FrameLayout {
    private TextView mContactIconView;
    private final int mIconBackgroundColor;
    private final CharSequence mIconText;
    private final int mIconTextColor;
    private final boolean mIncoming;
    private LinearLayout mMessageBubble;
    private final CharSequence mMessageText;
    private ViewGroup mMessageTextAndInfoView;
    private TextView mMessageTextView;
    private TextView mStatusTextView;
    private final CharSequence mTimestampText;

    public ConversationMessageView(Context context) {
        this(context, null);
    }

    public ConversationMessageView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ConversationMessageView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public ConversationMessageView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ConversationMessageView);
        this.mIncoming = typedArrayObtainStyledAttributes.getBoolean(3, true);
        this.mMessageText = typedArrayObtainStyledAttributes.getString(4);
        this.mTimestampText = typedArrayObtainStyledAttributes.getString(5);
        this.mIconText = typedArrayObtainStyledAttributes.getString(1);
        this.mIconTextColor = typedArrayObtainStyledAttributes.getColor(2, 0);
        this.mIconBackgroundColor = typedArrayObtainStyledAttributes.getColor(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        LayoutInflater.from(context).inflate(R.layout.conversation_message_icon, this);
        LayoutInflater.from(context).inflate(R.layout.conversation_message_content, this);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        this.mMessageBubble = (LinearLayout) findViewById(R.id.message_content);
        this.mMessageTextAndInfoView = (ViewGroup) findViewById(R.id.message_text_and_info);
        this.mMessageTextView = (TextView) findViewById(R.id.message_text);
        this.mStatusTextView = (TextView) findViewById(R.id.message_status);
        this.mContactIconView = (TextView) findViewById(R.id.conversation_icon);
        updateViewContent();
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) throws Resources.NotFoundException {
        updateViewAppearance();
        int size = View.MeasureSpec.getSize(i);
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(0, 0);
        this.mContactIconView.measure(iMakeMeasureSpec2, iMakeMeasureSpec2);
        int iMakeMeasureSpec3 = View.MeasureSpec.makeMeasureSpec(Math.max(this.mContactIconView.getMeasuredWidth(), this.mContactIconView.getMeasuredHeight()), 1073741824);
        this.mContactIconView.measure(iMakeMeasureSpec3, iMakeMeasureSpec3);
        this.mMessageBubble.measure(View.MeasureSpec.makeMeasureSpec((((size - (this.mContactIconView.getMeasuredWidth() * 2)) - getResources().getDimensionPixelSize(R.dimen.message_bubble_arrow_width)) - getPaddingLeft()) - getPaddingRight(), AccessPoint.UNREACHABLE_RSSI), iMakeMeasureSpec);
        setMeasuredDimension(size, Math.max(this.mContactIconView.getMeasuredHeight(), this.mMessageBubble.getMeasuredHeight()) + getPaddingBottom() + getPaddingTop());
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int paddingRight;
        int i5;
        boolean zIsLayoutRtl = isLayoutRtl(this);
        int measuredWidth = this.mContactIconView.getMeasuredWidth();
        int measuredHeight = this.mContactIconView.getMeasuredHeight();
        int paddingTop = getPaddingTop();
        int i6 = i3 - i;
        int paddingLeft = ((i6 - measuredWidth) - getPaddingLeft()) - getPaddingRight();
        int measuredHeight2 = this.mMessageBubble.getMeasuredHeight();
        if (this.mIncoming) {
            if (zIsLayoutRtl) {
                paddingRight = (i6 - getPaddingRight()) - measuredWidth;
                i5 = paddingRight - paddingLeft;
            } else {
                paddingRight = getPaddingLeft();
                i5 = paddingRight + measuredWidth;
            }
        } else if (zIsLayoutRtl) {
            paddingRight = getPaddingLeft();
            i5 = paddingRight + measuredWidth;
        } else {
            paddingRight = (i6 - getPaddingRight()) - measuredWidth;
            i5 = paddingRight - paddingLeft;
        }
        this.mContactIconView.layout(paddingRight, paddingTop, measuredWidth + paddingRight, measuredHeight + paddingTop);
        this.mMessageBubble.layout(i5, paddingTop, paddingLeft + i5, measuredHeight2 + paddingTop);
    }

    private static boolean isLayoutRtl(View view) {
        return 1 == view.getLayoutDirection();
    }

    private void updateViewContent() {
        this.mMessageTextView.setText(this.mMessageText);
        this.mStatusTextView.setText(this.mTimestampText);
        this.mContactIconView.setText(this.mIconText);
        this.mContactIconView.setTextColor(this.mIconTextColor);
        this.mContactIconView.setBackground(getTintedDrawable(getContext(), getContext().getDrawable(R.drawable.conversation_message_icon), this.mIconBackgroundColor));
    }

    private void updateViewAppearance() throws Resources.NotFoundException {
        int i;
        Resources resources = getResources();
        int dimensionPixelOffset = resources.getDimensionPixelOffset(R.dimen.message_bubble_arrow_width);
        int dimensionPixelOffset2 = resources.getDimensionPixelOffset(R.dimen.message_text_left_right_padding);
        int dimensionPixelOffset3 = resources.getDimensionPixelOffset(R.dimen.message_text_top_padding);
        int dimensionPixelOffset4 = resources.getDimensionPixelOffset(R.dimen.message_text_bottom_padding);
        if (this.mIncoming) {
            dimensionPixelOffset2 = dimensionPixelOffset + dimensionPixelOffset2;
            i = dimensionPixelOffset2;
        } else {
            i = dimensionPixelOffset + dimensionPixelOffset2;
        }
        int i2 = this.mIncoming ? 8388627 : 8388629;
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.message_padding_default);
        int dimensionPixelOffset5 = resources.getDimensionPixelOffset(R.dimen.message_metadata_top_padding);
        int i3 = this.mIncoming ? R.drawable.msg_bubble_incoming : R.drawable.msg_bubble_outgoing;
        int i4 = this.mIncoming ? R.color.message_bubble_incoming : R.color.message_bubble_outgoing;
        Context context = getContext();
        this.mMessageTextAndInfoView.setBackground(getTintedDrawable(context, context.getDrawable(i3), context.getColor(i4)));
        if (isLayoutRtl(this)) {
            this.mMessageTextAndInfoView.setPadding(i, dimensionPixelOffset3 + dimensionPixelOffset5, dimensionPixelOffset2, dimensionPixelOffset4);
        } else {
            this.mMessageTextAndInfoView.setPadding(dimensionPixelOffset2, dimensionPixelOffset3 + dimensionPixelOffset5, i, dimensionPixelOffset4);
        }
        setPadding(getPaddingLeft(), dimensionPixelSize, getPaddingRight(), 0);
        this.mMessageBubble.setGravity(i2);
        updateTextAppearance();
    }

    private void updateTextAppearance() {
        int i = this.mIncoming ? R.color.message_text_incoming : R.color.message_text_outgoing;
        int i2 = this.mIncoming ? R.color.timestamp_text_incoming : R.color.timestamp_text_outgoing;
        int color = getContext().getColor(i);
        this.mMessageTextView.setTextColor(color);
        this.mMessageTextView.setLinkTextColor(color);
        this.mStatusTextView.setTextColor(i2);
    }

    private static Drawable getTintedDrawable(Context context, Drawable drawable, int i) {
        Drawable.ConstantState constantState = drawable.getConstantState();
        if (constantState != null) {
            drawable = constantState.newDrawable(context.getResources()).mutate();
        }
        drawable.setColorFilter(i, PorterDuff.Mode.SRC_ATOP);
        return drawable;
    }
}
