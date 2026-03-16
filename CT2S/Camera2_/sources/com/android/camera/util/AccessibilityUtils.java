package com.android.camera.util;

import android.content.Context;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

public class AccessibilityUtils {
    public static void makeAnnouncement(View view, CharSequence announcement) {
        if (view != null) {
            if (ApiHelper.HAS_ANNOUNCE_FOR_ACCESSIBILITY) {
                view.announceForAccessibility(announcement);
                return;
            }
            Context ctx = view.getContext();
            AccessibilityManager am = (AccessibilityManager) ctx.getSystemService("accessibility");
            if (am.isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(64);
                AccessibilityRecordCompat arc = new AccessibilityRecordCompat(event);
                arc.setSource(view);
                event.setClassName(view.getClass().getName());
                event.setPackageName(view.getContext().getPackageName());
                event.setEnabled(view.isEnabled());
                event.getText().add(announcement);
                am.sendAccessibilityEvent(event);
            }
        }
    }
}
