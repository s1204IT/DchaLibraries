package com.android.setupwizardlib.util;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeProviderCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import com.android.settingslib.wifi.AccessPoint;
import java.util.List;

/* loaded from: classes.dex */
public class LinkAccessibilityHelper extends AccessibilityDelegateCompat {
    private final AccessibilityDelegateCompat mDelegate;

    public LinkAccessibilityHelper(TextView textView) {
        AccessibilityDelegateCompat preOLinkAccessibilityHelper;
        if (Build.VERSION.SDK_INT >= 26) {
            preOLinkAccessibilityHelper = new AccessibilityDelegateCompat();
        } else {
            preOLinkAccessibilityHelper = new PreOLinkAccessibilityHelper(textView);
        }
        this(preOLinkAccessibilityHelper);
    }

    LinkAccessibilityHelper(AccessibilityDelegateCompat accessibilityDelegateCompat) {
        this.mDelegate = accessibilityDelegateCompat;
    }

    @Override // android.support.v4.view.AccessibilityDelegateCompat
    public void sendAccessibilityEvent(View view, int i) {
        this.mDelegate.sendAccessibilityEvent(view, i);
    }

    @Override // android.support.v4.view.AccessibilityDelegateCompat
    public void sendAccessibilityEventUnchecked(View view, AccessibilityEvent accessibilityEvent) {
        this.mDelegate.sendAccessibilityEventUnchecked(view, accessibilityEvent);
    }

    @Override // android.support.v4.view.AccessibilityDelegateCompat
    public boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        return this.mDelegate.dispatchPopulateAccessibilityEvent(view, accessibilityEvent);
    }

    @Override // android.support.v4.view.AccessibilityDelegateCompat
    public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        this.mDelegate.onPopulateAccessibilityEvent(view, accessibilityEvent);
    }

    @Override // android.support.v4.view.AccessibilityDelegateCompat
    public void onInitializeAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        this.mDelegate.onInitializeAccessibilityEvent(view, accessibilityEvent);
    }

    @Override // android.support.v4.view.AccessibilityDelegateCompat
    public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
        this.mDelegate.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfoCompat);
    }

    @Override // android.support.v4.view.AccessibilityDelegateCompat
    public boolean onRequestSendAccessibilityEvent(ViewGroup viewGroup, View view, AccessibilityEvent accessibilityEvent) {
        return this.mDelegate.onRequestSendAccessibilityEvent(viewGroup, view, accessibilityEvent);
    }

    @Override // android.support.v4.view.AccessibilityDelegateCompat
    public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View view) {
        return this.mDelegate.getAccessibilityNodeProvider(view);
    }

    @Override // android.support.v4.view.AccessibilityDelegateCompat
    public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
        return this.mDelegate.performAccessibilityAction(view, i, bundle);
    }

    public final boolean dispatchHoverEvent(MotionEvent motionEvent) {
        return (this.mDelegate instanceof ExploreByTouchHelper) && ((ExploreByTouchHelper) this.mDelegate).dispatchHoverEvent(motionEvent);
    }

    static class PreOLinkAccessibilityHelper extends ExploreByTouchHelper {
        private final Rect mTempRect;
        private final TextView mView;

        PreOLinkAccessibilityHelper(TextView textView) {
            super(textView);
            this.mTempRect = new Rect();
            this.mView = textView;
        }

        @Override // android.support.v4.widget.ExploreByTouchHelper
        protected int getVirtualViewAt(float f, float f2) {
            CharSequence text = this.mView.getText();
            if (text instanceof Spanned) {
                Spanned spanned = (Spanned) text;
                int offsetForPosition = getOffsetForPosition(this.mView, f, f2);
                ClickableSpan[] clickableSpanArr = (ClickableSpan[]) spanned.getSpans(offsetForPosition, offsetForPosition, ClickableSpan.class);
                if (clickableSpanArr.length == 1) {
                    return spanned.getSpanStart(clickableSpanArr[0]);
                }
                return AccessPoint.UNREACHABLE_RSSI;
            }
            return AccessPoint.UNREACHABLE_RSSI;
        }

        @Override // android.support.v4.widget.ExploreByTouchHelper
        protected void getVisibleVirtualViews(List<Integer> list) {
            CharSequence text = this.mView.getText();
            if (text instanceof Spanned) {
                Spanned spanned = (Spanned) text;
                for (ClickableSpan clickableSpan : (ClickableSpan[]) spanned.getSpans(0, spanned.length(), ClickableSpan.class)) {
                    list.add(Integer.valueOf(spanned.getSpanStart(clickableSpan)));
                }
            }
        }

        @Override // android.support.v4.widget.ExploreByTouchHelper
        protected void onPopulateEventForVirtualView(int i, AccessibilityEvent accessibilityEvent) {
            ClickableSpan spanForOffset = getSpanForOffset(i);
            if (spanForOffset != null) {
                accessibilityEvent.setContentDescription(getTextForSpan(spanForOffset));
                return;
            }
            Log.e("LinkAccessibilityHelper", "LinkSpan is null for offset: " + i);
            accessibilityEvent.setContentDescription(this.mView.getText());
        }

        @Override // android.support.v4.widget.ExploreByTouchHelper
        protected void onPopulateNodeForVirtualView(int i, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
            ClickableSpan spanForOffset = getSpanForOffset(i);
            if (spanForOffset != null) {
                accessibilityNodeInfoCompat.setContentDescription(getTextForSpan(spanForOffset));
            } else {
                Log.e("LinkAccessibilityHelper", "LinkSpan is null for offset: " + i);
                accessibilityNodeInfoCompat.setContentDescription(this.mView.getText());
            }
            accessibilityNodeInfoCompat.setFocusable(true);
            accessibilityNodeInfoCompat.setClickable(true);
            getBoundsForSpan(spanForOffset, this.mTempRect);
            if (this.mTempRect.isEmpty()) {
                Log.e("LinkAccessibilityHelper", "LinkSpan bounds is empty for: " + i);
                this.mTempRect.set(0, 0, 1, 1);
            }
            accessibilityNodeInfoCompat.setBoundsInParent(this.mTempRect);
            accessibilityNodeInfoCompat.addAction(16);
        }

        @Override // android.support.v4.widget.ExploreByTouchHelper
        protected boolean onPerformActionForVirtualView(int i, int i2, Bundle bundle) {
            if (i2 == 16) {
                ClickableSpan spanForOffset = getSpanForOffset(i);
                if (spanForOffset != null) {
                    spanForOffset.onClick(this.mView);
                    return true;
                }
                Log.e("LinkAccessibilityHelper", "LinkSpan is null for offset: " + i);
                return false;
            }
            return false;
        }

        private ClickableSpan getSpanForOffset(int i) {
            CharSequence text = this.mView.getText();
            if (text instanceof Spanned) {
                ClickableSpan[] clickableSpanArr = (ClickableSpan[]) ((Spanned) text).getSpans(i, i, ClickableSpan.class);
                if (clickableSpanArr.length == 1) {
                    return clickableSpanArr[0];
                }
                return null;
            }
            return null;
        }

        private CharSequence getTextForSpan(ClickableSpan clickableSpan) {
            CharSequence text = this.mView.getText();
            if (text instanceof Spanned) {
                Spanned spanned = (Spanned) text;
                return spanned.subSequence(spanned.getSpanStart(clickableSpan), spanned.getSpanEnd(clickableSpan));
            }
            return text;
        }

        private Rect getBoundsForSpan(ClickableSpan clickableSpan, Rect rect) {
            Layout layout;
            CharSequence text = this.mView.getText();
            rect.setEmpty();
            if ((text instanceof Spanned) && (layout = this.mView.getLayout()) != null) {
                Spanned spanned = (Spanned) text;
                int spanStart = spanned.getSpanStart(clickableSpan);
                int spanEnd = spanned.getSpanEnd(clickableSpan);
                float primaryHorizontal = layout.getPrimaryHorizontal(spanStart);
                float primaryHorizontal2 = layout.getPrimaryHorizontal(spanEnd);
                int lineForOffset = layout.getLineForOffset(spanStart);
                int lineForOffset2 = layout.getLineForOffset(spanEnd);
                layout.getLineBounds(lineForOffset, rect);
                if (lineForOffset2 == lineForOffset) {
                    rect.left = (int) Math.min(primaryHorizontal, primaryHorizontal2);
                    rect.right = (int) Math.max(primaryHorizontal, primaryHorizontal2);
                } else if (layout.getParagraphDirection(lineForOffset) == -1) {
                    rect.right = (int) primaryHorizontal;
                } else {
                    rect.left = (int) primaryHorizontal;
                }
                rect.offset(this.mView.getTotalPaddingLeft(), this.mView.getTotalPaddingTop());
            }
            return rect;
        }

        private static int getOffsetForPosition(TextView textView, float f, float f2) {
            if (textView.getLayout() == null) {
                return -1;
            }
            return getOffsetAtCoordinate(textView, getLineAtCoordinate(textView, f2), f);
        }

        private static float convertToLocalHorizontalCoordinate(TextView textView, float f) {
            return Math.min((textView.getWidth() - textView.getTotalPaddingRight()) - 1, Math.max(0.0f, f - textView.getTotalPaddingLeft())) + textView.getScrollX();
        }

        private static int getLineAtCoordinate(TextView textView, float f) {
            return textView.getLayout().getLineForVertical((int) (Math.min((textView.getHeight() - textView.getTotalPaddingBottom()) - 1, Math.max(0.0f, f - textView.getTotalPaddingTop())) + textView.getScrollY()));
        }

        private static int getOffsetAtCoordinate(TextView textView, int i, float f) {
            return textView.getLayout().getOffsetForHorizontal(i, convertToLocalHorizontalCoordinate(textView, f));
        }
    }
}
