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

    public ConversationMessageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConversationMessageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ConversationMessageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ConversationMessageView);
        this.mIncoming = a.getBoolean(0, true);
        this.mMessageText = a.getString(1);
        this.mTimestampText = a.getString(2);
        this.mIconText = a.getString(3);
        this.mIconTextColor = a.getColor(4, 0);
        this.mIconBackgroundColor = a.getColor(5, 0);
        LayoutInflater.from(context).inflate(R.layout.conversation_message_icon, this);
        LayoutInflater.from(context).inflate(R.layout.conversation_message_content, this);
    }

    @Override
    protected void onFinishInflate() {
        this.mMessageBubble = (LinearLayout) findViewById(R.id.message_content);
        this.mMessageTextAndInfoView = (ViewGroup) findViewById(R.id.message_text_and_info);
        this.mMessageTextView = (TextView) findViewById(R.id.message_text);
        this.mStatusTextView = (TextView) findViewById(R.id.message_status);
        this.mContactIconView = (TextView) findViewById(R.id.conversation_icon);
        updateViewContent();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        updateViewAppearance();
        int horizontalSpace = View.MeasureSpec.getSize(widthMeasureSpec);
        int unspecifiedMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        int iconMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        this.mContactIconView.measure(iconMeasureSpec, iconMeasureSpec);
        int iconMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(Math.max(this.mContactIconView.getMeasuredWidth(), this.mContactIconView.getMeasuredHeight()), 1073741824);
        this.mContactIconView.measure(iconMeasureSpec2, iconMeasureSpec2);
        int arrowWidth = getResources().getDimensionPixelSize(R.dimen.message_bubble_arrow_width);
        int maxLeftoverSpace = (((horizontalSpace - (this.mContactIconView.getMeasuredWidth() * 2)) - arrowWidth) - getPaddingLeft()) - getPaddingRight();
        int messageContentWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxLeftoverSpace, Integer.MIN_VALUE);
        this.mMessageBubble.measure(messageContentWidthMeasureSpec, unspecifiedMeasureSpec);
        int maxHeight = Math.max(this.mContactIconView.getMeasuredHeight(), this.mMessageBubble.getMeasuredHeight());
        setMeasuredDimension(horizontalSpace, getPaddingBottom() + maxHeight + getPaddingTop());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int iconLeft;
        int contentLeft;
        boolean isRtl = isLayoutRtl(this);
        int iconWidth = this.mContactIconView.getMeasuredWidth();
        int iconHeight = this.mContactIconView.getMeasuredHeight();
        int iconTop = getPaddingTop();
        int contentWidth = (((right - left) - iconWidth) - getPaddingLeft()) - getPaddingRight();
        int contentHeight = this.mMessageBubble.getMeasuredHeight();
        if (this.mIncoming) {
            if (isRtl) {
                iconLeft = ((right - left) - getPaddingRight()) - iconWidth;
                contentLeft = iconLeft - contentWidth;
            } else {
                iconLeft = getPaddingLeft();
                contentLeft = iconLeft + iconWidth;
            }
        } else if (isRtl) {
            iconLeft = getPaddingLeft();
            contentLeft = iconLeft + iconWidth;
        } else {
            iconLeft = ((right - left) - getPaddingRight()) - iconWidth;
            contentLeft = iconLeft - contentWidth;
        }
        this.mContactIconView.layout(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight);
        this.mMessageBubble.layout(contentLeft, iconTop, contentLeft + contentWidth, iconTop + contentHeight);
    }

    private static boolean isLayoutRtl(View view) {
        return 1 == view.getLayoutDirection();
    }

    private void updateViewContent() {
        this.mMessageTextView.setText(this.mMessageText);
        this.mStatusTextView.setText(this.mTimestampText);
        this.mContactIconView.setText(this.mIconText);
        this.mContactIconView.setTextColor(this.mIconTextColor);
        Drawable iconBase = getContext().getDrawable(R.drawable.conversation_message_icon);
        this.mContactIconView.setBackground(getTintedDrawable(getContext(), iconBase, this.mIconBackgroundColor));
    }

    private void updateViewAppearance() {
        int textLeftPadding;
        int textRightPadding;
        Resources res = getResources();
        int arrowWidth = res.getDimensionPixelOffset(R.dimen.message_bubble_arrow_width);
        int messageTextLeftRightPadding = res.getDimensionPixelOffset(R.dimen.message_text_left_right_padding);
        int textTopPadding = res.getDimensionPixelOffset(R.dimen.message_text_top_padding);
        int textBottomPadding = res.getDimensionPixelOffset(R.dimen.message_text_bottom_padding);
        if (this.mIncoming) {
            textLeftPadding = messageTextLeftRightPadding + arrowWidth;
            textRightPadding = messageTextLeftRightPadding;
        } else {
            textLeftPadding = messageTextLeftRightPadding;
            textRightPadding = messageTextLeftRightPadding + arrowWidth;
        }
        int gravity = this.mIncoming ? 8388627 : 8388629;
        int messageTopPadding = res.getDimensionPixelSize(R.dimen.message_padding_default);
        int metadataTopPadding = res.getDimensionPixelOffset(R.dimen.message_metadata_top_padding);
        int bubbleDrawableResId = this.mIncoming ? R.drawable.msg_bubble_incoming : R.drawable.msg_bubble_outgoing;
        int bubbleColorResId = this.mIncoming ? R.color.message_bubble_incoming : R.color.message_bubble_outgoing;
        Context context = getContext();
        Drawable textBackgroundDrawable = getTintedDrawable(context, context.getDrawable(bubbleDrawableResId), context.getColor(bubbleColorResId));
        this.mMessageTextAndInfoView.setBackground(textBackgroundDrawable);
        if (isLayoutRtl(this)) {
            this.mMessageTextAndInfoView.setPadding(textRightPadding, textTopPadding + metadataTopPadding, textLeftPadding, textBottomPadding);
        } else {
            this.mMessageTextAndInfoView.setPadding(textLeftPadding, textTopPadding + metadataTopPadding, textRightPadding, textBottomPadding);
        }
        setPadding(getPaddingLeft(), messageTopPadding, getPaddingRight(), 0);
        this.mMessageBubble.setGravity(gravity);
        updateTextAppearance();
    }

    private void updateTextAppearance() {
        int messageColorResId = this.mIncoming ? R.color.message_text_incoming : R.color.message_text_outgoing;
        int timestampColorResId = this.mIncoming ? R.color.timestamp_text_incoming : R.color.timestamp_text_outgoing;
        int messageColor = getContext().getColor(messageColorResId);
        this.mMessageTextView.setTextColor(messageColor);
        this.mMessageTextView.setLinkTextColor(messageColor);
        this.mStatusTextView.setTextColor(timestampColorResId);
    }

    private static Drawable getTintedDrawable(Context context, Drawable drawable, int color) {
        Drawable retDrawable;
        Drawable.ConstantState constantStateDrawable = drawable.getConstantState();
        if (constantStateDrawable != null) {
            retDrawable = constantStateDrawable.newDrawable(context.getResources()).mutate();
        } else {
            retDrawable = drawable;
        }
        retDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        return retDrawable;
    }
}
