package com.android.settings.widget;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import java.util.List;

public class LinkAccessibilityHelper extends ExploreByTouchHelper {
    private final Rect mTempRect;
    private final TextView mView;

    public LinkAccessibilityHelper(TextView view) {
        super(view);
        this.mTempRect = new Rect();
        this.mView = view;
    }

    @Override
    protected int getVirtualViewAt(float x, float y) {
        CharSequence text = this.mView.getText();
        if (text instanceof Spanned) {
            Spanned spannedText = (Spanned) text;
            int offset = this.mView.getOffsetForPosition(x, y);
            ClickableSpan[] linkSpans = (ClickableSpan[]) spannedText.getSpans(offset, offset, ClickableSpan.class);
            if (linkSpans.length == 1) {
                ClickableSpan linkSpan = linkSpans[0];
                return spannedText.getSpanStart(linkSpan);
            }
            return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    @Override
    protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
        CharSequence text = this.mView.getText();
        if (!(text instanceof Spanned)) {
            return;
        }
        Spanned spannedText = (Spanned) text;
        ClickableSpan[] linkSpans = (ClickableSpan[]) spannedText.getSpans(0, spannedText.length(), ClickableSpan.class);
        for (ClickableSpan span : linkSpans) {
            virtualViewIds.add(Integer.valueOf(spannedText.getSpanStart(span)));
        }
    }

    @Override
    protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
        ClickableSpan span = getSpanForOffset(virtualViewId);
        if (span != null) {
            event.setContentDescription(getTextForSpan(span));
        } else {
            Log.e("LinkAccessibilityHelper", "ClickableSpan is null for offset: " + virtualViewId);
            event.setContentDescription(this.mView.getText());
        }
    }

    @Override
    protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfo info) {
        ClickableSpan span = getSpanForOffset(virtualViewId);
        if (span != null) {
            info.setContentDescription(getTextForSpan(span));
        } else {
            Log.e("LinkAccessibilityHelper", "ClickableSpan is null for offset: " + virtualViewId);
            info.setContentDescription(this.mView.getText());
        }
        info.setFocusable(true);
        info.setClickable(true);
        getBoundsForSpan(span, this.mTempRect);
        if (!this.mTempRect.isEmpty()) {
            info.setBoundsInParent(getBoundsForSpan(span, this.mTempRect));
        } else {
            Log.e("LinkAccessibilityHelper", "LinkSpan bounds is empty for: " + virtualViewId);
            this.mTempRect.set(0, 0, 1, 1);
            info.setBoundsInParent(this.mTempRect);
        }
        info.addAction(16);
    }

    @Override
    protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
        if (action == 16) {
            ClickableSpan span = getSpanForOffset(virtualViewId);
            if (span != null) {
                span.onClick(this.mView);
                return true;
            }
            Log.e("LinkAccessibilityHelper", "LinkSpan is null for offset: " + virtualViewId);
            return false;
        }
        return false;
    }

    private ClickableSpan getSpanForOffset(int offset) {
        CharSequence text = this.mView.getText();
        if (text instanceof Spanned) {
            Spanned spannedText = (Spanned) text;
            ClickableSpan[] spans = (ClickableSpan[]) spannedText.getSpans(offset, offset, ClickableSpan.class);
            if (spans.length == 1) {
                return spans[0];
            }
            return null;
        }
        return null;
    }

    private CharSequence getTextForSpan(ClickableSpan span) {
        CharSequence text = this.mView.getText();
        if (text instanceof Spanned) {
            Spanned spannedText = (Spanned) text;
            return spannedText.subSequence(spannedText.getSpanStart(span), spannedText.getSpanEnd(span));
        }
        return text;
    }

    private Rect getBoundsForSpan(ClickableSpan span, Rect outRect) {
        CharSequence text = this.mView.getText();
        outRect.setEmpty();
        if (text instanceof Spanned) {
            Spanned spannedText = (Spanned) text;
            int spanStart = spannedText.getSpanStart(span);
            int spanEnd = spannedText.getSpanEnd(span);
            Layout layout = this.mView.getLayout();
            float xStart = layout.getPrimaryHorizontal(spanStart);
            float xEnd = layout.getPrimaryHorizontal(spanEnd);
            int lineStart = layout.getLineForOffset(spanStart);
            int lineEnd = layout.getLineForOffset(spanEnd);
            layout.getLineBounds(lineStart, outRect);
            outRect.left = (int) xStart;
            if (lineEnd == lineStart) {
                outRect.right = (int) xEnd;
            }
            outRect.offset(this.mView.getTotalPaddingLeft(), this.mView.getTotalPaddingTop());
        }
        return outRect;
    }
}
