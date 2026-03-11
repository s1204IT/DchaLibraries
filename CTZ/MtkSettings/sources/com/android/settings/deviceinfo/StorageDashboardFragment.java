package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.SearchIndexableResource;
import android.util.SparseArray;
import android.view.View;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.StorageDashboardFragment;
import com.android.settings.deviceinfo.storage.AutomaticStorageManagementSwitchPreferenceController;
import com.android.settings.deviceinfo.storage.CachedStorageValuesHelper;
import com.android.settings.deviceinfo.storage.SecondaryUserController;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.StorageItemPreferenceController;
import com.android.settings.deviceinfo.storage.StorageSummaryDonutPreferenceController;
import com.android.settings.deviceinfo.storage.UserIconLoader;
import com.android.settings.deviceinfo.storage.VolumeSizesLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class StorageDashboardFragment extends DashboardFragment implements LoaderManager.LoaderCallbacks<SparseArray<StorageAsyncLoader.AppsStorageResult>> {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.storage_dashboard_fragment;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            StorageManager storageManager = (StorageManager) context.getSystemService(StorageManager.class);
            UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
            ArrayList arrayList = new ArrayList();
            arrayList.add(new StorageSummaryDonutPreferenceController(context));
            arrayList.add(new StorageItemPreferenceController(context, null, null, new StorageManagerVolumeProvider(storageManager)));
            arrayList.addAll(SecondaryUserController.getSecondaryUserControllers(context, userManager));
            return arrayList;
        }
    };
    private SparseArray<StorageAsyncLoader.AppsStorageResult> mAppsResult;
    private CachedStorageValuesHelper mCachedStorageValuesHelper;
    private PrivateVolumeOptionMenuController mOptionMenuController;
    private StorageItemPreferenceController mPreferenceController;
    private List<AbstractPreferenceController> mSecondaryUsers;
    private PrivateStorageInfo mStorageInfo;
    private StorageSummaryDonutPreferenceController mSummaryController;
    private VolumeInfo mVolume;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        this.mVolume = Utils.maybeInitializeVolume((StorageManager) activity.getSystemService(StorageManager.class), getArguments());
        if (this.mVolume == null) {
            activity.finish();
        } else {
            initializeOptionsMenu(activity);
        }
    }

    void initializeOptionsMenu(Activity activity) {
        this.mOptionMenuController = new PrivateVolumeOptionMenuController(activity, this.mVolume, new PackageManagerWrapper(activity.getPackageManager()));
        getLifecycle().addObserver(this.mOptionMenuController);
        setHasOptionsMenu(true);
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        initializeCacheProvider();
        maybeSetLoading(isQuotaSupported());
        Activity activity = getActivity();
        EntityHeaderController.newInstance(activity, this, null).setRecyclerView(getListView(), getLifecycle()).styleActionBar(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(0, Bundle.EMPTY, this);
        getLoaderManager().restartLoader(2, Bundle.EMPTY, new VolumeSizeCallbacks());
        getLoaderManager().initLoader(1, Bundle.EMPTY, new IconLoaderCallbacks());
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_storage_dashboard;
    }

    public void onReceivedSizes() {
        if (this.mStorageInfo != null) {
            long j = this.mStorageInfo.totalBytes - this.mStorageInfo.freeBytes;
            this.mSummaryController.updateBytes(j, this.mStorageInfo.totalBytes);
            this.mPreferenceController.setVolume(this.mVolume);
            this.mPreferenceController.setUsedSize(j);
            this.mPreferenceController.setTotalSize(this.mStorageInfo.totalBytes);
            int size = this.mSecondaryUsers.size();
            for (int i = 0; i < size; i++) {
                AbstractPreferenceController abstractPreferenceController = this.mSecondaryUsers.get(i);
                if (abstractPreferenceController instanceof SecondaryUserController) {
                    ((SecondaryUserController) abstractPreferenceController).setTotalSize(this.mStorageInfo.totalBytes);
                }
            }
        }
        if (this.mAppsResult == null) {
            return;
        }
        this.mPreferenceController.onLoadFinished(this.mAppsResult, UserHandle.myUserId());
        updateSecondaryUserControllers(this.mSecondaryUsers, this.mAppsResult);
        if (getView().findViewById(R.id.loading_container).getVisibility() == 0) {
            setLoading(false, true);
        }
    }

    @Override
    public int getMetricsCategory() {
        return 745;
    }

    @Override
    protected String getLogTag() {
        return "StorageDashboardFrag";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.storage_dashboard_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        this.mSummaryController = new StorageSummaryDonutPreferenceController(context);
        arrayList.add(this.mSummaryController);
        this.mPreferenceController = new StorageItemPreferenceController(context, this, this.mVolume, new StorageManagerVolumeProvider((StorageManager) context.getSystemService(StorageManager.class)));
        arrayList.add(this.mPreferenceController);
        this.mSecondaryUsers = SecondaryUserController.getSecondaryUserControllers(context, (UserManager) context.getSystemService(UserManager.class));
        arrayList.addAll(this.mSecondaryUsers);
        AutomaticStorageManagementSwitchPreferenceController automaticStorageManagementSwitchPreferenceController = new AutomaticStorageManagementSwitchPreferenceController(context, this.mMetricsFeatureProvider, getFragmentManager());
        getLifecycle().addObserver(automaticStorageManagementSwitchPreferenceController);
        arrayList.add(automaticStorageManagementSwitchPreferenceController);
        return arrayList;
    }

    protected void setVolume(VolumeInfo volumeInfo) {
        this.mVolume = volumeInfo;
    }

    private void updateSecondaryUserControllers(List<AbstractPreferenceController> list, SparseArray<StorageAsyncLoader.AppsStorageResult> sparseArray) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            Object obj = (AbstractPreferenceController) list.get(i);
            if (obj instanceof StorageAsyncLoader.ResultHandler) {
                ((StorageAsyncLoader.ResultHandler) obj).handleResult(sparseArray);
            }
        }
    }

    @Override
    public Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> onCreateLoader(int i, Bundle bundle) {
        Context context = getContext();
        return new StorageAsyncLoader(context, (UserManager) context.getSystemService(UserManager.class), this.mVolume.fsUuid, new StorageStatsSource(context), new PackageManagerWrapper(context.getPackageManager()));
    }

    @Override
    public void onLoadFinished(Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> loader, SparseArray<StorageAsyncLoader.AppsStorageResult> sparseArray) {
        this.mAppsResult = sparseArray;
        maybeCacheFreshValues();
        onReceivedSizes();
    }

    @Override
    public void onLoaderReset(Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> loader) {
    }

    public void setCachedStorageValuesHelper(CachedStorageValuesHelper cachedStorageValuesHelper) {
        this.mCachedStorageValuesHelper = cachedStorageValuesHelper;
    }

    public PrivateStorageInfo getPrivateStorageInfo() {
        return this.mStorageInfo;
    }

    public void setPrivateStorageInfo(PrivateStorageInfo privateStorageInfo) {
        this.mStorageInfo = privateStorageInfo;
    }

    public SparseArray<StorageAsyncLoader.AppsStorageResult> getAppsStorageResult() {
        return this.mAppsResult;
    }

    public void setAppsStorageResult(SparseArray<StorageAsyncLoader.AppsStorageResult> sparseArray) {
        this.mAppsResult = sparseArray;
    }

    public void initializeCachedValues() {
        PrivateStorageInfo cachedPrivateStorageInfo = this.mCachedStorageValuesHelper.getCachedPrivateStorageInfo();
        SparseArray<StorageAsyncLoader.AppsStorageResult> cachedAppsStorageResult = this.mCachedStorageValuesHelper.getCachedAppsStorageResult();
        if (cachedPrivateStorageInfo == null || cachedAppsStorageResult == null) {
            return;
        }
        this.mStorageInfo = cachedPrivateStorageInfo;
        this.mAppsResult = cachedAppsStorageResult;
    }

    public void maybeSetLoading(boolean z) {
        if ((z && (this.mStorageInfo == null || this.mAppsResult == null)) || (!z && this.mStorageInfo == null)) {
            setLoading(true, false);
        }
    }

    private void initializeCacheProvider() {
        this.mCachedStorageValuesHelper = new CachedStorageValuesHelper(getContext(), UserHandle.myUserId());
        initializeCachedValues();
        onReceivedSizes();
    }

    public void maybeCacheFreshValues() {
        if (this.mStorageInfo != null && this.mAppsResult != null) {
            this.mCachedStorageValuesHelper.cacheResult(this.mStorageInfo, this.mAppsResult.get(UserHandle.myUserId()));
        }
    }

    private boolean isQuotaSupported() {
        return ((StorageStatsManager) getActivity().getSystemService(StorageStatsManager.class)).isQuotaSupported(this.mVolume.fsUuid);
    }

    public final class IconLoaderCallbacks implements LoaderManager.LoaderCallbacks<SparseArray<Drawable>> {
        public IconLoaderCallbacks() {
        }

        @Override
        public Loader<SparseArray<Drawable>> onCreateLoader(int i, Bundle bundle) {
            return new UserIconLoader(StorageDashboardFragment.this.getContext(), new UserIconLoader.FetchUserIconTask() {
                @Override
                public final SparseArray getUserIcons() {
                    return UserIconLoader.loadUserIconsWithContext(StorageDashboardFragment.this.getContext());
                }
            });
        }

        @Override
        public void onLoadFinished(Loader<SparseArray<Drawable>> loader, final SparseArray<Drawable> sparseArray) {
            StorageDashboardFragment.this.mSecondaryUsers.stream().filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return StorageDashboardFragment.IconLoaderCallbacks.lambda$onLoadFinished$1((AbstractPreferenceController) obj);
                }
            }).forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((UserIconLoader.UserIconHandler) ((AbstractPreferenceController) obj)).handleUserIcons(sparseArray);
                }
            });
        }

        static boolean lambda$onLoadFinished$1(AbstractPreferenceController abstractPreferenceController) {
            return abstractPreferenceController instanceof UserIconLoader.UserIconHandler;
        }

        @Override
        public void onLoaderReset(Loader<SparseArray<Drawable>> loader) {
        }
    }

    public final class VolumeSizeCallbacks implements LoaderManager.LoaderCallbacks<PrivateStorageInfo> {
        public VolumeSizeCallbacks() {
        }

        @Override
        public Loader<PrivateStorageInfo> onCreateLoader(int i, Bundle bundle) {
            Context context = StorageDashboardFragment.this.getContext();
            return new VolumeSizesLoader(context, new StorageManagerVolumeProvider((StorageManager) context.getSystemService(StorageManager.class)), (StorageStatsManager) context.getSystemService(StorageStatsManager.class), StorageDashboardFragment.this.mVolume);
        }

        @Override
        public void onLoaderReset(Loader<PrivateStorageInfo> loader) {
        }

        @Override
        public void onLoadFinished(Loader<PrivateStorageInfo> loader, PrivateStorageInfo privateStorageInfo) {
            if (privateStorageInfo != null) {
                StorageDashboardFragment.this.mStorageInfo = privateStorageInfo;
                StorageDashboardFragment.this.maybeCacheFreshValues();
                StorageDashboardFragment.this.onReceivedSizes();
                return;
            }
            StorageDashboardFragment.this.getActivity().finish();
        }
    }
}
