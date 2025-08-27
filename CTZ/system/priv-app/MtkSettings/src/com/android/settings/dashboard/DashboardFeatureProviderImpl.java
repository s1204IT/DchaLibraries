package com.android.settings.dashboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.drawer.CategoryManager;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.ProfileSelectDialog;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;
import com.android.settingslib.utils.ThreadUtils;
import java.util.List;

/* loaded from: classes.dex */
public class DashboardFeatureProviderImpl implements DashboardFeatureProvider {
    static final String META_DATA_KEY_ORDER = "com.android.settings.order";
    private final CategoryManager mCategoryManager;
    protected final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final PackageManager mPackageManager;

    public DashboardFeatureProviderImpl(Context context) {
        this.mContext = context.getApplicationContext();
        this.mCategoryManager = CategoryManager.get(context, getExtraIntentAction());
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        this.mPackageManager = context.getPackageManager();
    }

    @Override // com.android.settings.dashboard.DashboardFeatureProvider
    public DashboardCategory getTilesForCategory(String str) {
        return this.mCategoryManager.getTilesByCategory(this.mContext, str);
    }

    @Override // com.android.settings.dashboard.DashboardFeatureProvider
    public List<DashboardCategory> getAllCategories() {
        return this.mCategoryManager.getCategories(this.mContext);
    }

    @Override // com.android.settings.dashboard.DashboardFeatureProvider
    public String getDashboardKeyForTile(Tile tile) {
        if (tile == null || tile.intent == null) {
            return null;
        }
        if (!TextUtils.isEmpty(tile.key)) {
            return tile.key;
        }
        return "dashboard_tile_pref_" + tile.intent.getComponent().getClassName();
    }

    @Override // com.android.settings.dashboard.DashboardFeatureProvider
    public void bindPreferenceToTile(final Activity activity, final int i, Preference preference, final Tile tile, String str, int i2) {
        String string;
        String string2;
        if (preference == null) {
            return;
        }
        preference.setTitle(tile.title);
        if (!TextUtils.isEmpty(str)) {
            preference.setKey(str);
        } else {
            preference.setKey(getDashboardKeyForTile(tile));
        }
        bindSummary(preference, tile);
        bindIcon(preference, tile);
        Bundle bundle = tile.metaData;
        Integer numValueOf = null;
        if (bundle != null) {
            string = bundle.getString("com.android.settings.FRAGMENT_CLASS");
            string2 = bundle.getString("com.android.settings.intent.action");
            if (bundle.containsKey(META_DATA_KEY_ORDER) && (bundle.get(META_DATA_KEY_ORDER) instanceof Integer)) {
                numValueOf = Integer.valueOf(bundle.getInt(META_DATA_KEY_ORDER));
            }
        } else {
            string = null;
            string2 = null;
        }
        if (!TextUtils.isEmpty(string)) {
            preference.setFragment(string);
        } else if (tile.intent != null) {
            final Intent intent = new Intent(tile.intent);
            intent.putExtra(":settings:source_metrics", i);
            if (string2 != null) {
                intent.setAction(string2);
            }
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.android.settings.dashboard.-$$Lambda$DashboardFeatureProviderImpl$EctMPOsKyfRtceDMH6yiU0UQS8U
                @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
                public final boolean onPreferenceClick(Preference preference2) {
                    return DashboardFeatureProviderImpl.lambda$bindPreferenceToTile$0(this.f$0, activity, tile, intent, i, preference2);
                }
            });
        }
        String packageName = activity.getPackageName();
        if (numValueOf == null && tile.priority != 0) {
            numValueOf = Integer.valueOf(-tile.priority);
        }
        if (numValueOf != null) {
            boolean zEquals = false;
            if (tile.intent != null) {
                zEquals = TextUtils.equals(packageName, tile.intent.getComponent().getPackageName());
            }
            if (zEquals || i2 == Integer.MAX_VALUE) {
                preference.setOrder(numValueOf.intValue());
            } else {
                preference.setOrder(numValueOf.intValue() + i2);
            }
        }
    }

    public static /* synthetic */ boolean lambda$bindPreferenceToTile$0(DashboardFeatureProviderImpl dashboardFeatureProviderImpl, Activity activity, Tile tile, Intent intent, int i, Preference preference) {
        dashboardFeatureProviderImpl.launchIntentOrSelectProfile(activity, tile, intent, i);
        return true;
    }

    public String getExtraIntentAction() {
        return null;
    }

    @Override // com.android.settings.dashboard.DashboardFeatureProvider
    public void openTileIntent(Activity activity, Tile tile) {
        if (tile == null) {
            this.mContext.startActivity(new Intent("android.settings.SETTINGS").addFlags(32768));
        } else {
            if (tile.intent == null) {
                return;
            }
            launchIntentOrSelectProfile(activity, tile, new Intent(tile.intent).putExtra(":settings:source_metrics", 35).addFlags(32768), 35);
        }
    }

    private void bindSummary(final Preference preference, final Tile tile) {
        if (tile.summary != null) {
            preference.setSummary(tile.summary);
        } else if (tile.metaData != null && tile.metaData.containsKey("com.android.settings.summary_uri")) {
            preference.setSummary(R.string.summary_placeholder);
            ThreadUtils.postOnBackgroundThread(new Runnable() { // from class: com.android.settings.dashboard.-$$Lambda$DashboardFeatureProviderImpl$eT0JYpovsB0-eUpWXkBH1qYJv_I
                @Override // java.lang.Runnable
                public final void run() {
                    DashboardFeatureProviderImpl.lambda$bindSummary$2(this.f$0, tile, preference);
                }
            });
        } else {
            preference.setSummary(R.string.summary_placeholder);
        }
    }

    public static /* synthetic */ void lambda$bindSummary$2(DashboardFeatureProviderImpl dashboardFeatureProviderImpl, Tile tile, final Preference preference) {
        ArrayMap arrayMap = new ArrayMap();
        final String textFromUri = TileUtils.getTextFromUri(dashboardFeatureProviderImpl.mContext, tile.metaData.getString("com.android.settings.summary_uri"), arrayMap, "com.android.settings.summary");
        ThreadUtils.postOnMainThread(new Runnable() { // from class: com.android.settings.dashboard.-$$Lambda$DashboardFeatureProviderImpl$f6w3zqqhleyaUiHJCm70VP43jfI
            @Override // java.lang.Runnable
            public final void run() {
                preference.setSummary(textFromUri);
            }
        });
    }

    void bindIcon(final Preference preference, final Tile tile) {
        if (tile.icon != null) {
            preference.setIcon(tile.icon.loadDrawable(preference.getContext()));
        } else if (tile.metaData != null && tile.metaData.containsKey("com.android.settings.icon_uri")) {
            ThreadUtils.postOnBackgroundThread(new Runnable() { // from class: com.android.settings.dashboard.-$$Lambda$DashboardFeatureProviderImpl$6nCUbNprlrw--1aNwFQYcoGh4Oc
                @Override // java.lang.Runnable
                public final void run() {
                    DashboardFeatureProviderImpl.lambda$bindIcon$4(this.f$0, tile, preference);
                }
            });
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:10:0x0025  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static /* synthetic */ void lambda$bindIcon$4(DashboardFeatureProviderImpl dashboardFeatureProviderImpl, Tile tile, final Preference preference) {
        String packageName;
        if (tile.intent != null) {
            Intent intent = tile.intent;
            if (!TextUtils.isEmpty(intent.getPackage())) {
                packageName = intent.getPackage();
            } else if (intent.getComponent() != null) {
                packageName = intent.getComponent().getPackageName();
            } else {
                packageName = null;
            }
        }
        ArrayMap arrayMap = new ArrayMap();
        String string = tile.metaData.getString("com.android.settings.icon_uri");
        Pair<String, Integer> iconFromUri = TileUtils.getIconFromUri(dashboardFeatureProviderImpl.mContext, packageName, string, arrayMap);
        if (iconFromUri == null) {
            Log.w("DashboardFeatureImpl", "Failed to get icon from uri " + string);
            return;
        }
        final Icon iconCreateWithResource = Icon.createWithResource((String) iconFromUri.first, ((Integer) iconFromUri.second).intValue());
        ThreadUtils.postOnMainThread(new Runnable() { // from class: com.android.settings.dashboard.-$$Lambda$DashboardFeatureProviderImpl$GREAS10FflfW9_XoMVZ4GOVTVF8
            @Override // java.lang.Runnable
            public final void run() {
                Preference preference2 = preference;
                preference2.setIcon(iconCreateWithResource.loadDrawable(preference2.getContext()));
            }
        });
    }

    private void launchIntentOrSelectProfile(Activity activity, Tile tile, Intent intent, int i) {
        if (!isIntentResolvable(intent)) {
            Log.w("DashboardFeatureImpl", "Cannot resolve intent, skipping. " + intent);
            return;
        }
        ProfileSelectDialog.updateUserHandlesIfNeeded(this.mContext, tile);
        if (tile.userHandle == null) {
            this.mMetricsFeatureProvider.logDashboardStartIntent(this.mContext, intent, i);
            activity.startActivityForResult(intent, 0);
        } else if (tile.userHandle.size() == 1) {
            this.mMetricsFeatureProvider.logDashboardStartIntent(this.mContext, intent, i);
            activity.startActivityForResultAsUser(intent, 0, tile.userHandle.get(0));
        } else {
            ProfileSelectDialog.show(activity.getFragmentManager(), tile);
        }
    }

    private boolean isIntentResolvable(Intent intent) {
        return this.mPackageManager.resolveActivity(intent, 0) != null;
    }
}
