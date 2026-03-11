package com.android.settings.deviceinfo;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.SparseArray;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.StorageItemPreferenceController;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.List;

public class StorageProfileFragment extends DashboardFragment implements LoaderManager.LoaderCallbacks<SparseArray<StorageAsyncLoader.AppsStorageResult>> {
    private StorageItemPreferenceController mPreferenceController;
    private int mUserId;
    private VolumeInfo mVolume;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Bundle arguments = getArguments();
        this.mVolume = Utils.maybeInitializeVolume((StorageManager) getActivity().getSystemService(StorageManager.class), arguments);
        if (this.mVolume == null) {
            getActivity().finish();
            return;
        }
        this.mPreferenceController.setVolume(this.mVolume);
        this.mUserId = arguments.getInt("userId", UserHandle.myUserId());
        this.mPreferenceController.setUserId(UserHandle.of(this.mUserId));
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(0, Bundle.EMPTY, this);
    }

    @Override
    public int getMetricsCategory() {
        return 845;
    }

    @Override
    protected String getLogTag() {
        return "StorageProfileFragment";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.storage_profile_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        this.mPreferenceController = new StorageItemPreferenceController(context, this, this.mVolume, new StorageManagerVolumeProvider((StorageManager) context.getSystemService(StorageManager.class)), true);
        arrayList.add(this.mPreferenceController);
        return arrayList;
    }

    @Override
    public Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> onCreateLoader(int i, Bundle bundle) {
        Context context = getContext();
        return new StorageAsyncLoader(context, (UserManager) context.getSystemService(UserManager.class), this.mVolume.fsUuid, new StorageStatsSource(context), new PackageManagerWrapper(context.getPackageManager()));
    }

    @Override
    public void onLoadFinished(Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> loader, SparseArray<StorageAsyncLoader.AppsStorageResult> sparseArray) {
        this.mPreferenceController.onLoadFinished(sparseArray, this.mUserId);
    }

    @Override
    public void onLoaderReset(Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> loader) {
    }

    void setPreferenceController(StorageItemPreferenceController storageItemPreferenceController) {
        this.mPreferenceController = storageItemPreferenceController;
    }
}
