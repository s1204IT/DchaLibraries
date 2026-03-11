package com.android.settings.shortcut;

import android.app.LauncherActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;
import com.mediatek.settings.FeatureOption;
import java.util.ArrayList;
import java.util.List;

public class CreateShortcut extends LauncherActivity {
    static final String SHORTCUT_ID_PREFIX = "component-shortcut-";

    @Override
    protected Intent getTargetIntent() {
        return getBaseIntent().addFlags(268435456);
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int i, long j) {
        LauncherActivity.ListItem listItemItemForPosition = itemForPosition(i);
        logCreateShortcut(listItemItemForPosition.resolveInfo);
        setResult(-1, createResultIntent(intentForPosition(i), listItemItemForPosition.resolveInfo, listItemItemForPosition.label));
        finish();
    }

    Intent createResultIntent(Intent intent, ResolveInfo resolveInfo, CharSequence charSequence) {
        Icon iconCreateWithResource;
        intent.setFlags(335544320);
        ShortcutManager shortcutManager = (ShortcutManager) getSystemService(ShortcutManager.class);
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        if (activityInfo.icon != 0) {
            iconCreateWithResource = Icon.createWithAdaptiveBitmap(createIcon(activityInfo.icon, R.layout.shortcut_badge_maskable, getResources().getDimensionPixelSize(R.dimen.shortcut_size_maskable)));
        } else {
            iconCreateWithResource = Icon.createWithResource(this, R.drawable.ic_launcher_settings);
        }
        Intent intentCreateShortcutResultIntent = shortcutManager.createShortcutResultIntent(new ShortcutInfo.Builder(this, SHORTCUT_ID_PREFIX + intent.getComponent().flattenToShortString()).setShortLabel(charSequence).setIntent(intent).setIcon(iconCreateWithResource).build());
        if (intentCreateShortcutResultIntent == null) {
            intentCreateShortcutResultIntent = new Intent();
        }
        intentCreateShortcutResultIntent.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher_settings));
        intentCreateShortcutResultIntent.putExtra("android.intent.extra.shortcut.INTENT", intent);
        intentCreateShortcutResultIntent.putExtra("android.intent.extra.shortcut.NAME", charSequence);
        if (activityInfo.icon != 0) {
            intentCreateShortcutResultIntent.putExtra("android.intent.extra.shortcut.ICON", createIcon(activityInfo.icon, R.layout.shortcut_badge, getResources().getDimensionPixelSize(R.dimen.shortcut_size)));
        }
        return intentCreateShortcutResultIntent;
    }

    private void logCreateShortcut(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return;
        }
        FeatureFactory.getFactory(this).getMetricsFeatureProvider().action(this, 829, resolveInfo.activityInfo.name, new Pair[0]);
    }

    private Bitmap createIcon(int i, int i2, int i3) {
        View viewInflate = LayoutInflater.from(new ContextThemeWrapper(this, android.R.style.Theme.Material)).inflate(i2, (ViewGroup) null);
        Drawable drawable = getDrawable(i);
        if (drawable instanceof LayerDrawable) {
            drawable = ((LayerDrawable) drawable).getDrawable(1);
        }
        ((ImageView) viewInflate.findViewById(android.R.id.icon)).setImageDrawable(drawable);
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(i3, 1073741824);
        viewInflate.measure(iMakeMeasureSpec, iMakeMeasureSpec);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(viewInflate.getMeasuredWidth(), viewInflate.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        viewInflate.layout(0, 0, viewInflate.getMeasuredWidth(), viewInflate.getMeasuredHeight());
        viewInflate.draw(canvas);
        return bitmapCreateBitmap;
    }

    protected boolean onEvaluateShowIcons() {
        return false;
    }

    @Override
    protected void onSetContentView() {
        setContentView(R.layout.activity_list);
    }

    @Override
    protected List<ResolveInfo> onQueryPackageManager(Intent intent) {
        List<ResolveInfo> listQueryIntentActivities = getPackageManager().queryIntentActivities(intent, 128);
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService("connectivity");
        if (listQueryIntentActivities == null) {
            return null;
        }
        for (int size = listQueryIntentActivities.size() - 1; size >= 0; size--) {
            ResolveInfo resolveInfo = listQueryIntentActivities.get(size);
            if (resolveInfo.activityInfo.name.endsWith(Settings.TetherSettingsActivity.class.getSimpleName())) {
                if (!connectivityManager.isTetheringSupported() || Utils.isWifiOnly(this)) {
                    listQueryIntentActivities.remove(size);
                }
            } else if (resolveInfo.activityInfo.name.endsWith(Settings.DreamSettingsActivity.class.getSimpleName()) && FeatureOption.MTK_GMO_RAM_OPTIMIZE) {
                listQueryIntentActivities.remove(size);
            }
        }
        return listQueryIntentActivities;
    }

    static Intent getBaseIntent() {
        return new Intent("android.intent.action.MAIN").addCategory("com.android.settings.SHORTCUT");
    }

    public static class ShortcutsUpdateTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;

        public ShortcutsUpdateTask(Context context) {
            this.mContext = context;
        }

        @Override
        public Void doInBackground(Void... voidArr) {
            ShortcutManager shortcutManager = (ShortcutManager) this.mContext.getSystemService(ShortcutManager.class);
            PackageManager packageManager = this.mContext.getPackageManager();
            ArrayList arrayList = new ArrayList();
            for (ShortcutInfo shortcutInfo : shortcutManager.getPinnedShortcuts()) {
                if (shortcutInfo.getId().startsWith(CreateShortcut.SHORTCUT_ID_PREFIX)) {
                    ResolveInfo resolveInfoResolveActivity = packageManager.resolveActivity(CreateShortcut.getBaseIntent().setComponent(ComponentName.unflattenFromString(shortcutInfo.getId().substring(CreateShortcut.SHORTCUT_ID_PREFIX.length()))), 0);
                    if (resolveInfoResolveActivity != null) {
                        arrayList.add(new ShortcutInfo.Builder(this.mContext, shortcutInfo.getId()).setShortLabel(resolveInfoResolveActivity.loadLabel(packageManager)).build());
                    }
                }
            }
            if (!arrayList.isEmpty()) {
                shortcutManager.updateShortcuts(arrayList);
                return null;
            }
            return null;
        }
    }
}
