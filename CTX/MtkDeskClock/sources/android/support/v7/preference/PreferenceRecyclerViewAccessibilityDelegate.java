package android.support.v7.preference;

import android.os.Bundle;
import android.support.annotation.RestrictTo;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerViewAccessibilityDelegate;
import android.view.View;

@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class PreferenceRecyclerViewAccessibilityDelegate extends RecyclerViewAccessibilityDelegate {
    final AccessibilityDelegateCompat mDefaultItemDelegate;
    final AccessibilityDelegateCompat mItemDelegate;
    final RecyclerView mRecyclerView;

    public PreferenceRecyclerViewAccessibilityDelegate(RecyclerView recyclerView) {
        super(recyclerView);
        this.mDefaultItemDelegate = super.getItemDelegate();
        this.mItemDelegate = new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                PreferenceRecyclerViewAccessibilityDelegate.this.mDefaultItemDelegate.onInitializeAccessibilityNodeInfo(host, info);
                int position = PreferenceRecyclerViewAccessibilityDelegate.this.mRecyclerView.getChildAdapterPosition(host);
                RecyclerView.Adapter adapter = PreferenceRecyclerViewAccessibilityDelegate.this.mRecyclerView.getAdapter();
                if (!(adapter instanceof PreferenceGroupAdapter)) {
                    return;
                }
                PreferenceGroupAdapter preferenceGroupAdapter = (PreferenceGroupAdapter) adapter;
                Preference preference = preferenceGroupAdapter.getItem(position);
                if (preference == null) {
                    return;
                }
                preference.onInitializeAccessibilityNodeInfo(info);
            }

            @Override
            public boolean performAccessibilityAction(View host, int action, Bundle args) {
                return PreferenceRecyclerViewAccessibilityDelegate.this.mDefaultItemDelegate.performAccessibilityAction(host, action, args);
            }
        };
        this.mRecyclerView = recyclerView;
    }

    @Override
    public AccessibilityDelegateCompat getItemDelegate() {
        return this.mItemDelegate;
    }
}
