package com.android.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
/* loaded from: a.zip:com/android/systemui/statusbar/KeyboardShortcutsReceiver.class */
public class KeyboardShortcutsReceiver extends BroadcastReceiver {
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.SHOW_KEYBOARD_SHORTCUTS".equals(intent.getAction())) {
            KeyboardShortcuts.show(context, -1);
        } else if ("android.intent.action.DISMISS_KEYBOARD_SHORTCUTS".equals(intent.getAction())) {
            KeyboardShortcuts.dismiss();
        }
    }
}
