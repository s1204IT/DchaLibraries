package com.android.settings.dashboard;

import android.R;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerListHelper;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.Tile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class DashboardFragment extends SettingsPreferenceFragment implements SummaryLoader.SummaryConsumer, Indexable, SettingsDrawerActivity.CategoryListener {
    private DashboardFeatureProvider mDashboardFeatureProvider;
    private boolean mListeningToCategoryChange;
    private DashboardTilePlaceholderPreferenceController mPlaceholderPreferenceController;
    private SummaryLoader mSummaryLoader;
    private final Map<Class, List<AbstractPreferenceController>> mPreferenceControllers = new ArrayMap();
    private final Set<String> mDashboardTilePrefKeys = new ArraySet();

    protected abstract String getLogTag();

    @Override
    protected abstract int getPreferenceScreenResId();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mDashboardFeatureProvider = FeatureFactory.getFactory(context).getDashboardFeatureProvider(context);
        ArrayList arrayList = new ArrayList();
        List<AbstractPreferenceController> listCreatePreferenceControllers = createPreferenceControllers(context);
        List<BasePreferenceController> listFilterControllers = PreferenceControllerListHelper.filterControllers(PreferenceControllerListHelper.getPreferenceControllersFromXml(context, getPreferenceScreenResId()), listCreatePreferenceControllers);
        if (listCreatePreferenceControllers != null) {
            arrayList.addAll(listCreatePreferenceControllers);
        }
        arrayList.addAll(listFilterControllers);
        final Lifecycle lifecycle = getLifecycle();
        listFilterControllers.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return DashboardFragment.lambda$onAttach$0((BasePreferenceController) obj);
            }
        }).forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                lifecycle.addObserver((LifecycleObserver) ((BasePreferenceController) obj));
            }
        });
        this.mPlaceholderPreferenceController = new DashboardTilePlaceholderPreferenceController(context);
        arrayList.add(this.mPlaceholderPreferenceController);
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            addPreferenceController((AbstractPreferenceController) it.next());
        }
    }

    static boolean lambda$onAttach$0(BasePreferenceController basePreferenceController) {
        return basePreferenceController instanceof LifecycleObserver;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getPreferenceManager().setPreferenceComparisonCallback(new PreferenceManager.SimplePreferenceComparisonCallback());
        if (bundle != null) {
            updatePreferenceStates();
        }
    }

    @Override
    public void onCategoriesChanged() {
        if (this.mDashboardFeatureProvider.getTilesForCategory(getCategoryKey()) == null) {
            return;
        }
        refreshDashboardTiles(getLogTag());
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String str) {
        refreshAllPreferences(getLogTag());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.mDashboardFeatureProvider.getTilesForCategory(getCategoryKey()) == null) {
            return;
        }
        if (this.mSummaryLoader != null) {
            this.mSummaryLoader.setListening(true);
        }
        Activity activity = getActivity();
        if (activity instanceof SettingsDrawerActivity) {
            this.mListeningToCategoryChange = true;
            ((SettingsDrawerActivity) activity).addCategoryListener(this);
        }
    }

    @Override
    public void notifySummaryChanged(Tile tile) {
        String dashboardKeyForTile = this.mDashboardFeatureProvider.getDashboardKeyForTile(tile);
        Preference preferenceFindPreference = getPreferenceScreen().findPreference(dashboardKeyForTile);
        if (preferenceFindPreference == null) {
            Log.d(getLogTag(), String.format("Can't find pref by key %s, skipping update summary %s/%s", dashboardKeyForTile, tile.title, tile.summary));
        } else {
            preferenceFindPreference.setSummary(tile.summary);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceStates();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        Collection<List<AbstractPreferenceController>> collectionValues = this.mPreferenceControllers.values();
        this.mMetricsFeatureProvider.logDashboardStartIntent(getContext(), preference.getIntent(), getMetricsCategory());
        Iterator<List<AbstractPreferenceController>> it = collectionValues.iterator();
        while (it.hasNext()) {
            Iterator<AbstractPreferenceController> it2 = it.next().iterator();
            while (it2.hasNext()) {
                if (it2.next().handlePreferenceTreeClick(preference)) {
                    return true;
                }
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.mSummaryLoader != null) {
            this.mSummaryLoader.setListening(false);
        }
        if (this.mListeningToCategoryChange) {
            Activity activity = getActivity();
            if (activity instanceof SettingsDrawerActivity) {
                ((SettingsDrawerActivity) activity).remCategoryListener(this);
            }
            this.mListeningToCategoryChange = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mSummaryLoader != null) {
            this.mSummaryLoader.release();
        }
    }

    protected <T extends AbstractPreferenceController> T use(Class<T> cls) {
        List<AbstractPreferenceController> list = this.mPreferenceControllers.get(cls);
        if (list != null) {
            if (list.size() > 1) {
                Log.w("DashboardFragment", "Multiple controllers of Class " + cls.getSimpleName() + " found, returning first one.");
            }
            return (T) list.get(0);
        }
        return null;
    }

    protected void addPreferenceController(AbstractPreferenceController abstractPreferenceController) {
        if (this.mPreferenceControllers.get(abstractPreferenceController.getClass()) == null) {
            this.mPreferenceControllers.put(abstractPreferenceController.getClass(), new ArrayList());
        }
        this.mPreferenceControllers.get(abstractPreferenceController.getClass()).add(abstractPreferenceController);
    }

    public String getCategoryKey() {
        return DashboardFragmentRegistry.PARENT_TO_CATEGORY_KEY_MAP.get(getClass().getName());
    }

    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    protected boolean displayTile(Tile tile) {
        return true;
    }

    boolean tintTileIcon(Tile tile) {
        if (tile.icon == null) {
            return false;
        }
        Bundle bundle = tile.metaData;
        if (bundle != null && bundle.containsKey("com.android.settings.icon_tintable")) {
            return bundle.getBoolean("com.android.settings.icon_tintable");
        }
        String packageName = getContext().getPackageName();
        return (packageName == null || tile.intent == null || packageName.equals(tile.intent.getComponent().getPackageName())) ? false : true;
    }

    private void displayResourceTiles() {
        int preferenceScreenResId = getPreferenceScreenResId();
        if (preferenceScreenResId <= 0) {
            return;
        }
        addPreferencesFromResource(preferenceScreenResId);
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mPreferenceControllers.values().stream().flatMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((List) obj).stream();
            }
        }).forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((AbstractPreferenceController) obj).displayPreference(preferenceScreen);
            }
        });
    }

    protected void updatePreferenceStates() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Iterator<List<AbstractPreferenceController>> it = this.mPreferenceControllers.values().iterator();
        while (it.hasNext()) {
            for (AbstractPreferenceController abstractPreferenceController : it.next()) {
                if (abstractPreferenceController.isAvailable()) {
                    String preferenceKey = abstractPreferenceController.getPreferenceKey();
                    Preference preferenceFindPreference = preferenceScreen.findPreference(preferenceKey);
                    if (preferenceFindPreference == null) {
                        Log.d("DashboardFragment", String.format("Cannot find preference with key %s in Controller %s", preferenceKey, abstractPreferenceController.getClass().getSimpleName()));
                    } else {
                        abstractPreferenceController.updateState(preferenceFindPreference);
                    }
                }
            }
        }
    }

    private void refreshAllPreferences(String str) {
        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
        }
        displayResourceTiles();
        refreshDashboardTiles(str);
    }

    void refreshDashboardTiles(String str) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        DashboardCategory tilesForCategory = this.mDashboardFeatureProvider.getTilesForCategory(getCategoryKey());
        if (tilesForCategory == null) {
            Log.d(str, "NO dashboard tiles for " + str);
            return;
        }
        List<Tile> tiles = tilesForCategory.getTiles();
        if (tiles == null) {
            Log.d(str, "tile list is empty, skipping category " + ((Object) tilesForCategory.title));
            return;
        }
        ArrayList<String> arrayList = new ArrayList(this.mDashboardTilePrefKeys);
        if (this.mSummaryLoader != null) {
            this.mSummaryLoader.release();
        }
        Context context = getContext();
        this.mSummaryLoader = new SummaryLoader(getActivity(), getCategoryKey());
        this.mSummaryLoader.setSummaryConsumer(this);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{R.attr.colorControlNormal});
        int color = typedArrayObtainStyledAttributes.getColor(0, context.getColor(R.color.white));
        typedArrayObtainStyledAttributes.recycle();
        for (Tile tile : tiles) {
            String dashboardKeyForTile = this.mDashboardFeatureProvider.getDashboardKeyForTile(tile);
            if (TextUtils.isEmpty(dashboardKeyForTile)) {
                Log.d(str, "tile does not contain a key, skipping " + tile);
            } else if (displayTile(tile)) {
                if (tintTileIcon(tile)) {
                    tile.icon.setTint(color);
                }
                if (this.mDashboardTilePrefKeys.contains(dashboardKeyForTile)) {
                    this.mDashboardFeatureProvider.bindPreferenceToTile(getActivity(), getMetricsCategory(), preferenceScreen.findPreference(dashboardKeyForTile), tile, dashboardKeyForTile, this.mPlaceholderPreferenceController.getOrder());
                } else {
                    Preference preference = new Preference(getPrefContext());
                    this.mDashboardFeatureProvider.bindPreferenceToTile(getActivity(), getMetricsCategory(), preference, tile, dashboardKeyForTile, this.mPlaceholderPreferenceController.getOrder());
                    preferenceScreen.addPreference(preference);
                    this.mDashboardTilePrefKeys.add(dashboardKeyForTile);
                }
                arrayList.remove(dashboardKeyForTile);
            }
        }
        for (String str2 : arrayList) {
            this.mDashboardTilePrefKeys.remove(str2);
            Preference preferenceFindPreference = preferenceScreen.findPreference(str2);
            if (preferenceFindPreference != null) {
                preferenceScreen.removePreference(preferenceFindPreference);
            }
        }
        this.mSummaryLoader.setListening(true);
    }
}
