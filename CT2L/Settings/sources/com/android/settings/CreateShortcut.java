package com.android.settings;

import android.app.LauncherActivity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.view.View;
import android.widget.ListView;
import com.android.settings.Settings;
import java.util.List;

public class CreateShortcut extends LauncherActivity {
    @Override
    protected Intent getTargetIntent() {
        Intent targetIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        targetIntent.addCategory("com.android.settings.SHORTCUT");
        targetIntent.addFlags(268435456);
        return targetIntent;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent shortcutIntent = intentForPosition(position);
        shortcutIntent.setFlags(2097152);
        Intent intent = new Intent();
        intent.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher_settings));
        intent.putExtra("android.intent.extra.shortcut.INTENT", shortcutIntent);
        intent.putExtra("android.intent.extra.shortcut.NAME", itemForPosition(position).label);
        setResult(-1, intent);
        finish();
    }

    protected boolean onEvaluateShowIcons() {
        return false;
    }

    @Override
    protected List<ResolveInfo> onQueryPackageManager(Intent queryIntent) {
        List<ResolveInfo> activities = super.onQueryPackageManager(queryIntent);
        if (activities == null) {
            return null;
        }
        for (int i = activities.size() - 1; i >= 0; i--) {
            ResolveInfo info = activities.get(i);
            if (info.activityInfo.name.endsWith(Settings.TetherSettingsActivity.class.getSimpleName()) && !TetherSettings.showInShortcuts(this)) {
                activities.remove(i);
            }
        }
        return activities;
    }
}
