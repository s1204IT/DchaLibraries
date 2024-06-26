package com.android.keyguard;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
/* loaded from: classes.dex */
class KeyguardClockAccessibilityDelegate extends View.AccessibilityDelegate {
    private final String mFancyColon;

    public KeyguardClockAccessibilityDelegate(Context context) {
        this.mFancyColon = context.getString(com.android.systemui.R.string.keyguard_fancy_colon);
    }

    @Override // android.view.View.AccessibilityDelegate
    public void onInitializeAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(view, accessibilityEvent);
        if (TextUtils.isEmpty(this.mFancyColon)) {
            return;
        }
        CharSequence contentDescription = accessibilityEvent.getContentDescription();
        if (!TextUtils.isEmpty(contentDescription)) {
            accessibilityEvent.setContentDescription(replaceFancyColon(contentDescription));
        }
    }

    @Override // android.view.View.AccessibilityDelegate
    public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        if (TextUtils.isEmpty(this.mFancyColon)) {
            super.onPopulateAccessibilityEvent(view, accessibilityEvent);
            return;
        }
        CharSequence text = ((TextView) view).getText();
        if (!TextUtils.isEmpty(text)) {
            accessibilityEvent.getText().add(replaceFancyColon(text));
        }
    }

    @Override // android.view.View.AccessibilityDelegate
    public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
        if (TextUtils.isEmpty(this.mFancyColon)) {
            return;
        }
        if (!TextUtils.isEmpty(accessibilityNodeInfo.getText())) {
            accessibilityNodeInfo.setText(replaceFancyColon(accessibilityNodeInfo.getText()));
        }
        if (!TextUtils.isEmpty(accessibilityNodeInfo.getContentDescription())) {
            accessibilityNodeInfo.setContentDescription(replaceFancyColon(accessibilityNodeInfo.getContentDescription()));
        }
    }

    private CharSequence replaceFancyColon(CharSequence charSequence) {
        if (TextUtils.isEmpty(this.mFancyColon)) {
            return charSequence;
        }
        return charSequence.toString().replace(this.mFancyColon, ":");
    }

    public static boolean isNeeded(Context context) {
        return !TextUtils.isEmpty(context.getString(com.android.systemui.R.string.keyguard_fancy_colon));
    }
}
