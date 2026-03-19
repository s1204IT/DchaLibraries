package com.android.documentsui.dirlist;

import android.os.Bundle;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerViewAccessibilityDelegate;
import android.view.View;
import java.util.function.Function;

public class AccessibilityEventRouter extends RecyclerViewAccessibilityDelegate {
    private final Function<View, Boolean> mClickCallback;
    private final RecyclerViewAccessibilityDelegate.ItemDelegate mItemDelegate;

    public AccessibilityEventRouter(RecyclerView recyclerView, Function<View, Boolean> function) {
        super(recyclerView);
        this.mClickCallback = function;
        this.mItemDelegate = new RecyclerViewAccessibilityDelegate.ItemDelegate(this) {
            @Override
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfoCompat);
                accessibilityNodeInfoCompat.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);
                accessibilityNodeInfoCompat.setSelected(view.isActivated());
            }

            @Override
            public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
                if (i == 16) {
                    return ((Boolean) AccessibilityEventRouter.this.mClickCallback.apply(view)).booleanValue();
                }
                return super.performAccessibilityAction(view, i, bundle);
            }
        };
    }

    @Override
    public AccessibilityDelegateCompat getItemDelegate() {
        return this.mItemDelegate;
    }
}
