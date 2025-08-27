package com.android.settings.applications;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.GrantedUriPermission;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.util.Log;
import android.util.MutableInt;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppStorageSizesController;
import com.android.settings.deviceinfo.StorageWizardMoveConfirm;
import com.android.settings.widget.ActionButtonPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.StorageStatsSource;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/* loaded from: classes.dex */
public class AppStorageSettings extends AppInfoWithHeader implements LoaderManager.LoaderCallbacks<StorageStatsSource.AppStorageStats>, DialogInterface.OnClickListener, View.OnClickListener, ApplicationsState.Callbacks {
    private static final String TAG = AppStorageSettings.class.getSimpleName();
    ActionButtonPreference mButtonsPref;
    private boolean mCacheCleared;
    private VolumeInfo[] mCandidates;
    private Button mChangeStorageButton;
    private ClearCacheObserver mClearCacheObserver;
    private ClearUserDataObserver mClearDataObserver;
    private LayoutPreference mClearUri;
    private Button mClearUriButton;
    private boolean mDataCleared;
    private AlertDialog.Builder mDialogBuilder;
    private ApplicationInfo mInfo;
    AppStorageSizesController mSizeController;
    private Preference mStorageUsed;
    private PreferenceCategory mUri;
    private boolean mCanClearData = true;
    private final Handler mHandler = new Handler() { // from class: com.android.settings.applications.AppStorageSettings.3
        @Override // android.os.Handler
        public void handleMessage(Message message) {
            if (AppStorageSettings.this.getView() == null) {
                return;
            }
            int i = message.what;
            if (i == 1) {
                AppStorageSettings.this.mDataCleared = true;
                AppStorageSettings.this.mCacheCleared = true;
                AppStorageSettings.this.processClearMsg(message);
            } else if (i == 3) {
                AppStorageSettings.this.mCacheCleared = true;
                AppStorageSettings.this.updateSize();
            }
        }
    };

    @Override // com.android.settings.applications.AppInfoBase, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            this.mCacheCleared = bundle.getBoolean("cache_cleared", false);
            this.mDataCleared = bundle.getBoolean("data_cleared", false);
            this.mCacheCleared = this.mCacheCleared || this.mDataCleared;
        }
        addPreferencesFromResource(R.xml.app_storage_settings);
        setupViews();
        initMoveDialog();
    }

    @Override // com.android.settings.applications.AppInfoBase, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        updateSize();
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("cache_cleared", this.mCacheCleared);
        bundle.putBoolean("data_cleared", this.mDataCleared);
    }

    private void setupViews() {
        this.mSizeController = new AppStorageSizesController.Builder().setTotalSizePreference(findPreference("total_size")).setAppSizePreference(findPreference("app_size")).setDataSizePreference(findPreference("data_size")).setCacheSizePreference(findPreference("cache_size")).setComputingString(R.string.computing_size).setErrorString(R.string.invalid_size_value).build();
        this.mButtonsPref = ((ActionButtonPreference) findPreference("header_view")).setButton1Positive(false).setButton2Positive(false);
        this.mStorageUsed = findPreference("storage_used");
        this.mChangeStorageButton = (Button) ((LayoutPreference) findPreference("change_storage_button")).findViewById(R.id.button);
        this.mChangeStorageButton.setText(R.string.change);
        this.mChangeStorageButton.setOnClickListener(this);
        this.mButtonsPref.setButton2Text(R.string.clear_cache_btn_text);
        this.mUri = (PreferenceCategory) findPreference("uri_category");
        this.mClearUri = (LayoutPreference) this.mUri.findPreference("clear_uri_button");
        this.mClearUriButton = (Button) this.mClearUri.findViewById(R.id.button);
        this.mClearUriButton.setText(R.string.clear_uri_btn_text);
        this.mClearUriButton.setOnClickListener(this);
    }

    void handleClearCacheClick() {
        if (this.mAppsControlDisallowedAdmin != null && !this.mAppsControlDisallowedBySystem) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), this.mAppsControlDisallowedAdmin);
            return;
        }
        if (this.mClearCacheObserver == null) {
            this.mClearCacheObserver = new ClearCacheObserver();
        }
        this.mMetricsFeatureProvider.action(getContext(), 877, new Pair[0]);
        this.mPm.deleteApplicationCacheFiles(this.mPackageName, this.mClearCacheObserver);
    }

    void handleClearDataClick() {
        if (this.mAppsControlDisallowedAdmin != null && !this.mAppsControlDisallowedBySystem) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), this.mAppsControlDisallowedAdmin);
            return;
        }
        if (this.mAppEntry.info.manageSpaceActivityName != null) {
            if (!Utils.isMonkeyRunning()) {
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setClassName(this.mAppEntry.info.packageName, this.mAppEntry.info.manageSpaceActivityName);
                startActivityForResult(intent, 2);
                return;
            }
            return;
        }
        showDialogInner(1, 0);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.mChangeStorageButton && this.mDialogBuilder != null && !isMoveInProgress()) {
            this.mDialogBuilder.show();
            return;
        }
        if (view == this.mClearUriButton) {
            if (this.mAppsControlDisallowedAdmin != null && !this.mAppsControlDisallowedBySystem) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), this.mAppsControlDisallowedAdmin);
            } else {
                clearUriPermissions();
            }
        }
    }

    private boolean isMoveInProgress() {
        try {
            AppGlobals.getPackageManager().checkPackageStartable(this.mPackageName, UserHandle.myUserId());
            return false;
        } catch (RemoteException | SecurityException e) {
            return true;
        }
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        Activity activity = getActivity();
        VolumeInfo volumeInfo = this.mCandidates[i];
        if (!Objects.equals(volumeInfo, activity.getPackageManager().getPackageCurrentVolume(this.mAppEntry.info))) {
            Intent intent = new Intent(activity, (Class<?>) StorageWizardMoveConfirm.class);
            intent.putExtra("android.os.storage.extra.VOLUME_ID", volumeInfo.getId());
            intent.putExtra("android.intent.extra.PACKAGE_NAME", this.mAppEntry.info.packageName);
            startActivity(intent);
        }
        dialogInterface.dismiss();
    }

    @Override // com.android.settings.applications.AppInfoBase
    protected boolean refreshUi() {
        retrieveAppEntry();
        if (this.mAppEntry == null) {
            return false;
        }
        updateUiWithSize(this.mSizeController.getLastResult());
        refreshGrantedUriPermissions();
        VolumeInfo packageCurrentVolume = getActivity().getPackageManager().getPackageCurrentVolume(this.mAppEntry.info);
        this.mStorageUsed.setSummary(((StorageManager) getContext().getSystemService(StorageManager.class)).getBestVolumeDescription(packageCurrentVolume));
        refreshButtons();
        return true;
    }

    private void refreshButtons() {
        initMoveDialog();
        initDataButtons();
    }

    private void initDataButtons() {
        boolean z = this.mAppEntry.info.manageSpaceActivityName != null;
        boolean z2 = ((this.mAppEntry.info.flags & 65) == 1) || this.mDpm.packageHasActiveAdmins(this.mPackageName);
        Intent intent = new Intent("android.intent.action.VIEW");
        if (z) {
            intent.setClassName(this.mAppEntry.info.packageName, this.mAppEntry.info.manageSpaceActivityName);
        }
        boolean z3 = getPackageManager().resolveActivity(intent, 0) != null;
        if ((!z && z2) || !z3) {
            this.mButtonsPref.setButton1Text(R.string.clear_user_data_text).setButton1Enabled(false);
            this.mCanClearData = false;
        } else {
            if (z) {
                this.mButtonsPref.setButton1Text(R.string.manage_space_text);
            } else {
                this.mButtonsPref.setButton1Text(R.string.clear_user_data_text);
            }
            this.mButtonsPref.setButton1Text(R.string.clear_user_data_text).setButton1OnClickListener(new View.OnClickListener() { // from class: com.android.settings.applications.-$$Lambda$AppStorageSettings$uXyfUeZFqT2Ct1euRP3fPo2Es3o
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.handleClearDataClick();
                }
            });
        }
        if (this.mAppsControlDisallowedBySystem) {
            this.mButtonsPref.setButton1Enabled(false);
        }
    }

    private void initMoveDialog() {
        Activity activity = getActivity();
        StorageManager storageManager = (StorageManager) activity.getSystemService(StorageManager.class);
        List packageCandidateVolumes = activity.getPackageManager().getPackageCandidateVolumes(this.mAppEntry.info);
        if (packageCandidateVolumes.size() > 1) {
            Collections.sort(packageCandidateVolumes, VolumeInfo.getDescriptionComparator());
            CharSequence[] charSequenceArr = new CharSequence[packageCandidateVolumes.size()];
            int i = -1;
            for (int i2 = 0; i2 < packageCandidateVolumes.size(); i2++) {
                String bestVolumeDescription = storageManager.getBestVolumeDescription((VolumeInfo) packageCandidateVolumes.get(i2));
                if (Objects.equals(bestVolumeDescription, this.mStorageUsed.getSummary())) {
                    i = i2;
                }
                charSequenceArr[i2] = bestVolumeDescription;
            }
            this.mCandidates = (VolumeInfo[]) packageCandidateVolumes.toArray(new VolumeInfo[packageCandidateVolumes.size()]);
            this.mDialogBuilder = new AlertDialog.Builder(getContext()).setTitle(R.string.change_storage).setSingleChoiceItems(charSequenceArr, i, this).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
            return;
        }
        removePreference("storage_used");
        removePreference("change_storage_button");
        removePreference("storage_space");
    }

    private void initiateClearUserData() {
        this.mMetricsFeatureProvider.action(getContext(), 876, new Pair[0]);
        this.mButtonsPref.setButton1Enabled(false);
        String str = this.mAppEntry.info.packageName;
        Log.i(TAG, "Clearing user data for package : " + str);
        if (this.mClearDataObserver == null) {
            this.mClearDataObserver = new ClearUserDataObserver();
        }
        if (!((ActivityManager) getActivity().getSystemService("activity")).clearApplicationUserData(str, this.mClearDataObserver)) {
            Log.i(TAG, "Couldn't clear application user data for package:" + str);
            showDialogInner(2, 0);
            return;
        }
        this.mButtonsPref.setButton1Text(R.string.recompute_size);
    }

    private void processClearMsg(Message message) {
        int i = message.arg1;
        String str = this.mAppEntry.info.packageName;
        this.mButtonsPref.setButton1Text(R.string.clear_user_data_text);
        if (i != 1) {
            this.mButtonsPref.setButton1Enabled(true);
            return;
        }
        Log.i(TAG, "Cleared user data for package : " + str);
        updateSize();
        Intent intent = new Intent("com.mediatek.intent.action.SETTINGS_PACKAGE_DATA_CLEARED");
        intent.putExtra("packageName", str);
        getActivity().sendBroadcast(intent);
    }

    private void refreshGrantedUriPermissions() {
        removeUriPermissionsFromUi();
        List list = ((ActivityManager) getActivity().getSystemService("activity")).getGrantedUriPermissions(this.mAppEntry.info.packageName).getList();
        if (list.isEmpty()) {
            this.mClearUriButton.setVisibility(8);
            return;
        }
        PackageManager packageManager = getActivity().getPackageManager();
        TreeMap treeMap = new TreeMap();
        Iterator it = list.iterator();
        while (it.hasNext()) {
            CharSequence charSequenceLoadLabel = packageManager.resolveContentProvider(((GrantedUriPermission) it.next()).uri.getAuthority(), 0).applicationInfo.loadLabel(packageManager);
            MutableInt mutableInt = (MutableInt) treeMap.get(charSequenceLoadLabel);
            if (mutableInt == null) {
                treeMap.put(charSequenceLoadLabel, new MutableInt(1));
            } else {
                mutableInt.value++;
            }
        }
        for (Map.Entry entry : treeMap.entrySet()) {
            int i = ((MutableInt) entry.getValue()).value;
            Preference preference = new Preference(getPrefContext());
            preference.setTitle((CharSequence) entry.getKey());
            preference.setSummary(getPrefContext().getResources().getQuantityString(R.plurals.uri_permissions_text, i, Integer.valueOf(i)));
            preference.setSelectable(false);
            preference.setLayoutResource(R.layout.horizontal_preference);
            preference.setOrder(0);
            Log.v(TAG, "Adding preference '" + preference + "' at order 0");
            this.mUri.addPreference(preference);
        }
        if (this.mAppsControlDisallowedBySystem) {
            this.mClearUriButton.setEnabled(false);
        }
        this.mClearUri.setOrder(0);
        this.mClearUriButton.setVisibility(0);
    }

    private void clearUriPermissions() {
        Activity activity = getActivity();
        String str = this.mAppEntry.info.packageName;
        ((ActivityManager) activity.getSystemService("activity")).clearGrantedUriPermissions(str);
        Uri uriBuild = new Uri.Builder().scheme("content").authority("com.android.documentsui.scopedAccess").appendPath("permissions").appendPath("*").build();
        Log.v(TAG, "Asking " + uriBuild + " to delete permissions for " + str);
        int iDelete = activity.getContentResolver().delete(uriBuild, null, new String[]{str});
        Log.d(TAG, "Deleted " + iDelete + " entries for package " + str);
        refreshGrantedUriPermissions();
    }

    private void removeUriPermissionsFromUi() {
        for (int preferenceCount = this.mUri.getPreferenceCount() - 1; preferenceCount >= 0; preferenceCount--) {
            Preference preference = this.mUri.getPreference(preferenceCount);
            if (preference != this.mClearUri) {
                this.mUri.removePreference(preference);
            }
        }
    }

    @Override // com.android.settings.applications.AppInfoBase
    protected AlertDialog createDialog(int i, int i2) {
        switch (i) {
            case 1:
                return new AlertDialog.Builder(getActivity()).setTitle(getActivity().getText(R.string.clear_data_dlg_title)).setMessage(getActivity().getText(R.string.clear_data_dlg_text)).setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() { // from class: com.android.settings.applications.AppStorageSettings.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i3) {
                        AppStorageSettings.this.initiateClearUserData();
                    }
                }).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            case 2:
                return new AlertDialog.Builder(getActivity()).setTitle(getActivity().getText(R.string.clear_user_data_text)).setMessage(getActivity().getText(R.string.clear_failed_dlg_text)).setNeutralButton(R.string.dlg_ok, new DialogInterface.OnClickListener() { // from class: com.android.settings.applications.AppStorageSettings.2
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i3) {
                        AppStorageSettings.this.mButtonsPref.setButton1Enabled(false);
                        AppStorageSettings.this.setIntentAndFinish(false, false);
                    }
                }).create();
            default:
                return null;
        }
    }

    @Override // com.android.settings.applications.AppInfoBase, com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageSizeChanged(String str) {
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public Loader<StorageStatsSource.AppStorageStats> onCreateLoader(int i, Bundle bundle) {
        Context context = getContext();
        return new FetchPackageStorageAsyncLoader(context, new StorageStatsSource(context), this.mInfo, UserHandle.of(this.mUserId));
    }

    /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoadFinished(Loader<StorageStatsSource.AppStorageStats> loader, StorageStatsSource.AppStorageStats appStorageStats) {
        this.mSizeController.setResult(appStorageStats);
        updateUiWithSize(appStorageStats);
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoaderReset(Loader<StorageStatsSource.AppStorageStats> loader) {
    }

    private void updateSize() {
        try {
            this.mInfo = getPackageManager().getApplicationInfo(this.mPackageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find package", e);
        }
        if (this.mInfo == null) {
            return;
        }
        getLoaderManager().restartLoader(1, Bundle.EMPTY, this);
    }

    void updateUiWithSize(StorageStatsSource.AppStorageStats appStorageStats) {
        if (this.mCacheCleared) {
            this.mSizeController.setCacheCleared(true);
        }
        if (this.mDataCleared) {
            this.mSizeController.setDataCleared(true);
        }
        this.mSizeController.updateUi(getContext());
        if (appStorageStats == null) {
            this.mButtonsPref.setButton1Enabled(false).setButton2Enabled(false);
        } else {
            long cacheBytes = appStorageStats.getCacheBytes();
            if (appStorageStats.getDataBytes() - cacheBytes <= 0 || !this.mCanClearData || this.mDataCleared) {
                this.mButtonsPref.setButton1Enabled(false);
            } else {
                this.mButtonsPref.setButton1Enabled(true).setButton1OnClickListener(new View.OnClickListener() { // from class: com.android.settings.applications.-$$Lambda$AppStorageSettings$n1EpAla7gNI7Nnl-O3UD0UWSgTo
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        this.f$0.handleClearDataClick();
                    }
                });
            }
            if (cacheBytes <= 0 || this.mCacheCleared) {
                this.mButtonsPref.setButton2Enabled(false);
            } else {
                this.mButtonsPref.setButton2Enabled(true).setButton2OnClickListener(new View.OnClickListener() { // from class: com.android.settings.applications.-$$Lambda$AppStorageSettings$DjRyx_XFfzsxe3o1nZS2usao_fc
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        this.f$0.handleClearCacheClick();
                    }
                });
            }
        }
        if (this.mAppsControlDisallowedBySystem) {
            this.mButtonsPref.setButton1Enabled(false).setButton2Enabled(false);
        }
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 19;
    }

    class ClearCacheObserver extends IPackageDataObserver.Stub {
        ClearCacheObserver() {
        }

        public void onRemoveCompleted(String str, boolean z) {
            Message messageObtainMessage = AppStorageSettings.this.mHandler.obtainMessage(3);
            messageObtainMessage.arg1 = z ? 1 : 2;
            AppStorageSettings.this.mHandler.sendMessage(messageObtainMessage);
        }
    }

    class ClearUserDataObserver extends IPackageDataObserver.Stub {
        ClearUserDataObserver() {
        }

        public void onRemoveCompleted(String str, boolean z) {
            Message messageObtainMessage = AppStorageSettings.this.mHandler.obtainMessage(1);
            messageObtainMessage.arg1 = z ? 1 : 2;
            AppStorageSettings.this.mHandler.sendMessage(messageObtainMessage);
        }
    }
}
