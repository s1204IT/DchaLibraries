package com.android.managedprovisioning.common;

import android.app.Activity;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import com.android.managedprovisioning.R;

public class AccessibilityContextMenuMaker {
    private final Activity mActivity;

    public AccessibilityContextMenuMaker(Activity activity) {
        this.mActivity = activity;
    }

    public void registerWithActivity(final TextView textView) {
        if (getSpans(getText(textView)).length == 0) {
            this.mActivity.unregisterForContextMenu(textView);
            textView.setAccessibilityDelegate(null);
            textView.setClickable(false);
            textView.setLongClickable(false);
            return;
        }
        this.mActivity.registerForContextMenu(textView);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                view.showContextMenu();
            }
        });
        textView.setLongClickable(false);
        textView.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
                accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.getId(), textView.getContext().getString(R.string.access_list_of_links)));
            }
        });
    }

    public void populateMenuContent(final ContextMenu contextMenu, final TextView textView) {
        if (!isScreenReaderEnabled()) {
            return;
        }
        Spanned text = getText(textView);
        ClickableSpan[] spans = getSpans(text);
        if (text == null || spans.length == 0) {
            return;
        }
        for (final ClickableSpan clickableSpan : spans) {
            contextMenu.add(text.subSequence(text.getSpanStart(clickableSpan), text.getSpanEnd(clickableSpan))).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public final boolean onMenuItemClick(MenuItem menuItem) {
                    return AccessibilityContextMenuMaker.lambda$populateMenuContent$0(clickableSpan, textView, menuItem);
                }
            });
        }
        contextMenu.add(R.string.close_list).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public final boolean onMenuItemClick(MenuItem menuItem) {
                return AccessibilityContextMenuMaker.lambda$populateMenuContent$1(contextMenu, menuItem);
            }
        });
    }

    static boolean lambda$populateMenuContent$0(ClickableSpan clickableSpan, TextView textView, MenuItem menuItem) {
        clickableSpan.onClick(textView);
        return false;
    }

    static boolean lambda$populateMenuContent$1(ContextMenu contextMenu, MenuItem menuItem) {
        contextMenu.close();
        return false;
    }

    private boolean isScreenReaderEnabled() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) this.mActivity.getSystemService(AccessibilityManager.class);
        return accessibilityManager.isEnabled() && accessibilityManager.isTouchExplorationEnabled();
    }

    private Spanned getText(TextView textView) {
        CharSequence text = textView.getText();
        if (text instanceof Spanned) {
            return (Spanned) text;
        }
        return null;
    }

    private ClickableSpan[] getSpans(Spanned spanned) {
        if (spanned != null) {
            ClickableSpan[] clickableSpanArr = (ClickableSpan[]) spanned.getSpans(0, spanned.length(), ClickableSpan.class);
            return clickableSpanArr.length == 0 ? new ClickableSpan[0] : clickableSpanArr;
        }
        return new ClickableSpan[0];
    }
}
