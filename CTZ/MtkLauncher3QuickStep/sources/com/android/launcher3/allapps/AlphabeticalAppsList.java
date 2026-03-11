package com.android.launcher3.allapps;

import android.content.Context;
import com.android.launcher3.AppInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.compat.AlphabeticIndexCompat;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.LabelComparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class AlphabeticalAppsList implements AllAppsStore.OnUpdateListener {
    private static final int FAST_SCROLL_FRACTION_DISTRIBUTE_BY_NUM_SECTIONS = 1;
    private static final int FAST_SCROLL_FRACTION_DISTRIBUTE_BY_ROWS_FRACTION = 0;
    public static final String TAG = "AlphabeticalAppsList";
    private AllAppsGridAdapter mAdapter;
    private final AllAppsStore mAllAppsStore;
    private AppInfoComparator mAppNameComparator;
    private AlphabeticIndexCompat mIndexer;
    private final boolean mIsWork;
    private ItemInfoMatcher mItemFilter;
    private final Launcher mLauncher;
    private int mNumAppRowsInAdapter;
    private final int mNumAppsPerRow;
    private ArrayList<ComponentKey> mSearchResults;
    private final int mFastScrollDistributionMode = 1;
    private final List<AppInfo> mApps = new ArrayList();
    private final List<AppInfo> mFilteredApps = new ArrayList();
    private final ArrayList<AdapterItem> mAdapterItems = new ArrayList<>();
    private final List<FastScrollSectionInfo> mFastScrollerSections = new ArrayList();
    private HashMap<CharSequence, String> mCachedSectionNames = new HashMap<>();

    public static class FastScrollSectionInfo {
        public AdapterItem fastScrollToItem;
        public String sectionName;
        public float touchFraction;

        public FastScrollSectionInfo(String str) {
            this.sectionName = str;
        }
    }

    public static class AdapterItem {
        public int position;
        public int rowAppIndex;
        public int rowIndex;
        public int viewType;
        public String sectionName = null;
        public AppInfo appInfo = null;
        public int appIndex = -1;

        public static AdapterItem asApp(int i, String str, AppInfo appInfo, int i2) {
            AdapterItem adapterItem = new AdapterItem();
            adapterItem.viewType = 2;
            adapterItem.position = i;
            adapterItem.sectionName = str;
            adapterItem.appInfo = appInfo;
            adapterItem.appIndex = i2;
            return adapterItem;
        }

        public static AdapterItem asEmptySearch(int i) {
            AdapterItem adapterItem = new AdapterItem();
            adapterItem.viewType = 4;
            adapterItem.position = i;
            return adapterItem;
        }

        public static AdapterItem asAllAppsDivider(int i) {
            AdapterItem adapterItem = new AdapterItem();
            adapterItem.viewType = 16;
            adapterItem.position = i;
            return adapterItem;
        }

        public static AdapterItem asMarketSearch(int i) {
            AdapterItem adapterItem = new AdapterItem();
            adapterItem.viewType = 8;
            adapterItem.position = i;
            return adapterItem;
        }

        public static AdapterItem asWorkTabFooter(int i) {
            AdapterItem adapterItem = new AdapterItem();
            adapterItem.viewType = 32;
            adapterItem.position = i;
            return adapterItem;
        }
    }

    public AlphabeticalAppsList(Context context, AllAppsStore allAppsStore, boolean z) {
        this.mAllAppsStore = allAppsStore;
        this.mLauncher = Launcher.getLauncher(context);
        this.mIndexer = new AlphabeticIndexCompat(context);
        this.mAppNameComparator = new AppInfoComparator(context);
        this.mIsWork = z;
        this.mNumAppsPerRow = this.mLauncher.getDeviceProfile().inv.numColumns;
        this.mAllAppsStore.addUpdateListener(this);
    }

    public void updateItemFilter(ItemInfoMatcher itemInfoMatcher) {
        this.mItemFilter = itemInfoMatcher;
        onAppsUpdated();
    }

    public void setAdapter(AllAppsGridAdapter allAppsGridAdapter) {
        this.mAdapter = allAppsGridAdapter;
    }

    public List<AppInfo> getApps() {
        return this.mApps;
    }

    public List<FastScrollSectionInfo> getFastScrollerSections() {
        return this.mFastScrollerSections;
    }

    public List<AdapterItem> getAdapterItems() {
        return this.mAdapterItems;
    }

    public int getNumAppRows() {
        return this.mNumAppRowsInAdapter;
    }

    public int getNumFilteredApps() {
        return this.mFilteredApps.size();
    }

    public boolean hasFilter() {
        return this.mSearchResults != null;
    }

    public boolean hasNoFilteredResults() {
        return this.mSearchResults != null && this.mFilteredApps.isEmpty();
    }

    public boolean setOrderedFilter(ArrayList<ComponentKey> arrayList) {
        boolean z = false;
        if (this.mSearchResults == arrayList) {
            return false;
        }
        if (this.mSearchResults != null && this.mSearchResults.equals(arrayList)) {
            z = true;
        }
        this.mSearchResults = arrayList;
        onAppsUpdated();
        return !z;
    }

    @Override
    public void onAppsUpdated() {
        this.mApps.clear();
        for (AppInfo appInfo : this.mAllAppsStore.getApps()) {
            if (this.mItemFilter == null || this.mItemFilter.matches(appInfo, null) || hasFilter()) {
                this.mApps.add(appInfo);
            }
        }
        Collections.sort(this.mApps, this.mAppNameComparator);
        if (this.mLauncher.getResources().getConfiguration().locale.equals(Locale.SIMPLIFIED_CHINESE)) {
            TreeMap treeMap = new TreeMap(new LabelComparator());
            for (AppInfo appInfo2 : this.mApps) {
                String andUpdateCachedSectionName = getAndUpdateCachedSectionName(appInfo2.title);
                ArrayList arrayList = (ArrayList) treeMap.get(andUpdateCachedSectionName);
                if (arrayList == null) {
                    arrayList = new ArrayList();
                    treeMap.put(andUpdateCachedSectionName, arrayList);
                }
                arrayList.add(appInfo2);
            }
            this.mApps.clear();
            Iterator it = treeMap.entrySet().iterator();
            while (it.hasNext()) {
                this.mApps.addAll((Collection) ((Map.Entry) it.next()).getValue());
            }
        } else {
            Iterator<AppInfo> it2 = this.mApps.iterator();
            while (it2.hasNext()) {
                getAndUpdateCachedSectionName(it2.next().title);
            }
        }
        updateAdapterItems();
    }

    private void updateAdapterItems() {
        refillAdapterItems();
        refreshRecyclerView();
    }

    private void refreshRecyclerView() {
        if (this.mAdapter != null) {
            this.mAdapter.notifyDataSetChanged();
        }
    }

    private void refillAdapterItems() {
        int i;
        this.mFilteredApps.clear();
        this.mFastScrollerSections.clear();
        this.mAdapterItems.clear();
        ?? r1 = 0;
        FastScrollSectionInfo fastScrollSectionInfo = null;
        int i2 = 0;
        int i3 = 0;
        for (AppInfo appInfo : getFiltersAppInfos()) {
            ?? andUpdateCachedSectionName = getAndUpdateCachedSectionName(appInfo.title);
            boolean zEquals = andUpdateCachedSectionName.equals(r1);
            ?? r12 = r1;
            if (!zEquals) {
                FastScrollSectionInfo fastScrollSectionInfo2 = new FastScrollSectionInfo(andUpdateCachedSectionName);
                this.mFastScrollerSections.add(fastScrollSectionInfo2);
                fastScrollSectionInfo = fastScrollSectionInfo2;
                r12 = andUpdateCachedSectionName;
            }
            int i4 = i2 + 1;
            int i5 = i3 + 1;
            AdapterItem adapterItemAsApp = AdapterItem.asApp(i2, andUpdateCachedSectionName, appInfo, i3);
            if (fastScrollSectionInfo.fastScrollToItem == null) {
                fastScrollSectionInfo.fastScrollToItem = adapterItemAsApp;
            }
            this.mAdapterItems.add(adapterItemAsApp);
            this.mFilteredApps.add(appInfo);
            i2 = i4;
            i3 = i5;
            r1 = r12;
        }
        if (hasFilter()) {
            if (hasNoFilteredResults()) {
                i = i2 + 1;
                this.mAdapterItems.add(AdapterItem.asEmptySearch(i2));
            } else {
                i = i2 + 1;
                this.mAdapterItems.add(AdapterItem.asAllAppsDivider(i2));
            }
            i2 = i + 1;
            this.mAdapterItems.add(AdapterItem.asMarketSearch(i));
        }
        if (this.mNumAppsPerRow != 0) {
            int i6 = -1;
            int i7 = 0;
            int i8 = 0;
            for (AdapterItem adapterItem : this.mAdapterItems) {
                adapterItem.rowIndex = 0;
                if (!AllAppsGridAdapter.isDividerViewType(adapterItem.viewType)) {
                    if (AllAppsGridAdapter.isIconViewType(adapterItem.viewType)) {
                        if (i7 % this.mNumAppsPerRow == 0) {
                            i6++;
                            i8 = 0;
                        }
                        adapterItem.rowIndex = i6;
                        adapterItem.rowAppIndex = i8;
                        i7++;
                        i8++;
                    }
                } else {
                    i7 = 0;
                }
            }
            this.mNumAppRowsInAdapter = i6 + 1;
            switch (1) {
                case 0:
                    float f = 1.0f / this.mNumAppRowsInAdapter;
                    for (FastScrollSectionInfo fastScrollSectionInfo3 : this.mFastScrollerSections) {
                        if (!AllAppsGridAdapter.isIconViewType(fastScrollSectionInfo3.fastScrollToItem.viewType)) {
                            fastScrollSectionInfo3.touchFraction = 0.0f;
                        } else {
                            fastScrollSectionInfo3.touchFraction = (r5.rowIndex * f) + (r5.rowAppIndex * (f / this.mNumAppsPerRow));
                        }
                    }
                    break;
                case 1:
                    float size = 1.0f / this.mFastScrollerSections.size();
                    float f2 = 0.0f;
                    for (FastScrollSectionInfo fastScrollSectionInfo4 : this.mFastScrollerSections) {
                        if (!AllAppsGridAdapter.isIconViewType(fastScrollSectionInfo4.fastScrollToItem.viewType)) {
                            fastScrollSectionInfo4.touchFraction = 0.0f;
                        } else {
                            fastScrollSectionInfo4.touchFraction = f2;
                            f2 += size;
                        }
                    }
                    break;
            }
        }
        if (shouldShowWorkFooter()) {
            this.mAdapterItems.add(AdapterItem.asWorkTabFooter(i2));
        }
    }

    private boolean shouldShowWorkFooter() {
        return this.mIsWork && Utilities.ATLEAST_P && (DeepShortcutManager.getInstance(this.mLauncher).hasHostPermission() || this.mLauncher.checkSelfPermission("android.permission.MODIFY_QUIET_MODE") == 0);
    }

    private List<AppInfo> getFiltersAppInfos() {
        if (this.mSearchResults == null) {
            return this.mApps;
        }
        ArrayList arrayList = new ArrayList();
        Iterator<ComponentKey> it = this.mSearchResults.iterator();
        while (it.hasNext()) {
            AppInfo app = this.mAllAppsStore.getApp(it.next());
            if (app != null) {
                arrayList.add(app);
            }
        }
        return arrayList;
    }

    private String getAndUpdateCachedSectionName(CharSequence charSequence) {
        String str = this.mCachedSectionNames.get(charSequence);
        if (str == null) {
            String strComputeSectionName = this.mIndexer.computeSectionName(charSequence);
            this.mCachedSectionNames.put(charSequence, strComputeSectionName);
            return strComputeSectionName;
        }
        return str;
    }
}
