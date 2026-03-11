package com.android.settings;

import android.app.LauncherActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import com.android.settings.Settings;
import com.mediatek.settings.FeatureOption;
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
        ResolveInfo resolveInfo = itemForPosition(position).resolveInfo;
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        if (activityInfo.icon != 0) {
            intent.putExtra("android.intent.extra.shortcut.ICON", createIcon(activityInfo.icon));
        }
        setResult(-1, intent);
        finish();
    }

    private Bitmap createIcon(int resource) {
        Context context = new ContextThemeWrapper(this, android.R.style.Theme.Material);
        View view = LayoutInflater.from(context).inflate(R.layout.shortcut_badge, (ViewGroup) null);
        ((ImageView) view.findViewById(android.R.id.icon)).setImageResource(resource);
        int spec = View.MeasureSpec.makeMeasureSpec(0, 0);
        view.measure(spec, spec);
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.draw(canvas);
        return bitmap;
    }

    protected boolean onEvaluateShowIcons() {
        return false;
    }

    @Override
    protected List<ResolveInfo> onQueryPackageManager(Intent queryIntent) {
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(queryIntent, 128);
        ConnectivityManager cm = (ConnectivityManager) getSystemService("connectivity");
        if (activities == null) {
            return null;
        }
        for (int i = activities.size() - 1; i >= 0; i--) {
            ResolveInfo info = activities.get(i);
            if (info.activityInfo.name.endsWith(Settings.TetherSettingsActivity.class.getSimpleName())) {
                if (!cm.isTetheringSupported() || Utils.isWifiOnly(this)) {
                    activities.remove(i);
                }
            } else if (info.activityInfo.name.endsWith(Settings.DreamSettingsActivity.class.getSimpleName()) && FeatureOption.MTK_GMO_RAM_OPTIMIZE) {
                activities.remove(i);
            }
        }
        return activities;
    }
}
