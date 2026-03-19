package com.android.deskclock.events;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ShortcutManager;
import android.support.annotation.StringRes;
import android.util.ArraySet;
import com.android.deskclock.R;
import com.android.deskclock.uidata.UiDataModel;
import java.util.Set;

@TargetApi(25)
public final class ShortcutEventTracker implements EventTracker {
    private final ShortcutManager mShortcutManager;
    private final Set<String> shortcuts = new ArraySet(5);

    public ShortcutEventTracker(Context context) {
        this.mShortcutManager = (ShortcutManager) context.getSystemService(ShortcutManager.class);
        UiDataModel uiDataModel = UiDataModel.getUiDataModel();
        this.shortcuts.add(uiDataModel.getShortcutId(R.string.category_alarm, R.string.action_create));
        this.shortcuts.add(uiDataModel.getShortcutId(R.string.category_timer, R.string.action_create));
        this.shortcuts.add(uiDataModel.getShortcutId(R.string.category_stopwatch, R.string.action_pause));
        this.shortcuts.add(uiDataModel.getShortcutId(R.string.category_stopwatch, R.string.action_start));
        this.shortcuts.add(uiDataModel.getShortcutId(R.string.category_screensaver, R.string.action_show));
    }

    @Override
    public void sendEvent(@StringRes int i, @StringRes int i2, @StringRes int i3) {
        String shortcutId = UiDataModel.getUiDataModel().getShortcutId(i, i2);
        if (this.shortcuts.contains(shortcutId)) {
            this.mShortcutManager.reportShortcutUsed(shortcutId);
        }
    }
}
