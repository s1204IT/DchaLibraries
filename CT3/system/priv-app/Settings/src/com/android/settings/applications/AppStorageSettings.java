package com.android.settings.applications;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.format.Formatter;
import android.util.Log;
import android.util.MutableInt;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.deviceinfo.StorageWizardMoveConfirm;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.ApplicationsState;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
/* loaded from: classes.dex */
public class AppStorageSettings extends AppInfoWithHeader implements View.OnClickListener, ApplicationsState.Callbacks, DialogInterface.OnClickListener {
    private static final String TAG = AppStorageSettings.class.getSimpleName();
    private Preference mAppSize;
    private Preference mCacheSize;
    private VolumeInfo[] mCandidates;
    private Button mChangeStorageButton;
    private Button mClearCacheButton;
    private ClearCacheObserver mClearCacheObserver;
    private Button mClearDataButton;
    private ClearUserDataObserver mClearDataObserver;
    private LayoutPreference mClearUri;
    private Button mClearUriButton;
    private CharSequence mComputingStr;
    private Preference mDataSize;
    private AlertDialog.Builder mDialogBuilder;
    private Preference mExternalCodeSize;
    private Preference mExternalDataSize;
    private CharSequence mInvalidSizeStr;
    private Preference mStorageUsed;
    private Preference mTotalSize;
    private PreferenceCategory mUri;
    private boolean mCanClearData = true;
    private boolean mHaveSizes = false;
    private long mLastCodeSize = -1;
    private long mLastDataSize = -1;
    private long mLastExternalCodeSize = -1;
    private long mLastExternalDataSize = -1;
    private long mLastCacheSize = -1;
    private long mLastTotalSize = -1;
    private final Handler mHandler = new Handler() { // from class: com.android.settings.applications.AppStorageSettings.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (AppStorageSettings.this.getView() == null) {
                return;
            }
            switch (msg.what) {
                case 1:
                    AppStorageSettings.this.processClearMsg(msg);
                    return;
                case 2:
                default:
                    return;
                case 3:
                    AppStorageSettings.this.mState.requestSize(AppStorageSettings.this.mPackageName, AppStorageSettings.this.mUserId);
                    return;
            }
        }
    };

    @Override // com.android.settings.applications.AppInfoBase, com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_storage_settings);
        setupViews();
        initMoveDialog();
    }

    @Override // com.android.settings.applications.AppInfoBase, com.android.settings.SettingsPreferenceFragment, com.android.settings.InstrumentedPreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mState.requestSize(this.mPackageName, this.mUserId);
    }

    private void setupViews() {
        this.mComputingStr = getActivity().getText(R.string.computing_size);
        this.mInvalidSizeStr = getActivity().getText(R.string.invalid_size_value);
        this.mTotalSize = findPreference("total_size");
        this.mAppSize = findPreference("app_size");
        this.mDataSize = findPreference("data_size");
        this.mExternalCodeSize = findPreference("external_code_size");
        this.mExternalDataSize = findPreference("external_data_size");
        if (Environment.isExternalStorageEmulated()) {
            PreferenceCategory category = (PreferenceCategory) findPreference("storage_category");
            category.removePreference(this.mExternalCodeSize);
            category.removePreference(this.mExternalDataSize);
        }
        this.mClearDataButton = (Button) ((LayoutPreference) findPreference("clear_data_button")).findViewById(R.id.button);
        this.mStorageUsed = findPreference("storage_used");
        this.mChangeStorageButton = (Button) ((LayoutPreference) findPreference("change_storage_button")).findViewById(R.id.button);
        this.mChangeStorageButton.setText(R.string.change);
        this.mChangeStorageButton.setOnClickListener(this);
        this.mCacheSize = findPreference("cache_size");
        this.mClearCacheButton = (Button) ((LayoutPreference) findPreference("clear_cache_button")).findViewById(R.id.button);
        this.mClearCacheButton.setText(R.string.clear_cache_btn_text);
        this.mUri = (PreferenceCategory) findPreference("uri_category");
        this.mClearUri = (LayoutPreference) this.mUri.findPreference("clear_uri_button");
        this.mClearUriButton = (Button) this.mClearUri.findViewById(R.id.button);
        this.mClearUriButton.setText(R.string.clear_uri_btn_text);
        this.mClearUriButton.setOnClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View v) {
        if (v == this.mClearCacheButton) {
            if (this.mAppsControlDisallowedAdmin != null && !this.mAppsControlDisallowedBySystem) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), this.mAppsControlDisallowedAdmin);
                return;
            }
            if (this.mClearCacheObserver == null) {
                this.mClearCacheObserver = new ClearCacheObserver();
            }
            this.mPm.deleteApplicationCacheFiles(this.mPackageName, this.mClearCacheObserver);
        } else if (v == this.mClearDataButton) {
            if (this.mAppsControlDisallowedAdmin != null && !this.mAppsControlDisallowedBySystem) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), this.mAppsControlDisallowedAdmin);
            } else if (this.mAppEntry.info.manageSpaceActivityName != null) {
                if (Utils.isMonkeyRunning()) {
                    return;
                }
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setClassName(this.mAppEntry.info.packageName, this.mAppEntry.info.manageSpaceActivityName);
                startActivityForResult(intent, 2);
            } else {
                showDialogInner(1, 0);
            }
        } else if (v == this.mChangeStorageButton && this.mDialogBuilder != null && !isMoveInProgress()) {
            this.mDialogBuilder.show();
        } else if (v != this.mClearUriButton) {
        } else {
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
    public void onClick(DialogInterface dialog, int which) {
        Context context = getActivity();
        VolumeInfo targetVol = this.mCandidates[which];
        VolumeInfo currentVol = context.getPackageManager().getPackageCurrentVolume(this.mAppEntry.info);
        if (!Objects.equals(targetVol, currentVol)) {
            Intent intent = new Intent(context, StorageWizardMoveConfirm.class);
            intent.putExtra("android.os.storage.extra.VOLUME_ID", targetVol.getId());
            intent.putExtra("android.intent.extra.PACKAGE_NAME", this.mAppEntry.info.packageName);
            startActivity(intent);
        }
        dialog.dismiss();
    }

    private String getSizeStr(long size) {
        if (size == -1) {
            return this.mInvalidSizeStr.toString();
        }
        return Formatter.formatFileSize(getActivity(), size);
    }

    private void refreshSizeInfo() {
        if (this.mAppEntry.size == -2 || this.mAppEntry.size == -1) {
            this.mLastTotalSize = -1L;
            this.mLastCacheSize = -1L;
            this.mLastDataSize = -1L;
            this.mLastCodeSize = -1L;
            if (!this.mHaveSizes) {
                this.mAppSize.setSummary(this.mComputingStr);
                this.mDataSize.setSummary(this.mComputingStr);
                this.mCacheSize.setSummary(this.mComputingStr);
                this.mTotalSize.setSummary(this.mComputingStr);
            }
            this.mClearDataButton.setEnabled(false);
            this.mClearCacheButton.setEnabled(false);
        } else {
            this.mHaveSizes = true;
            long codeSize = this.mAppEntry.codeSize;
            long dataSize = this.mAppEntry.dataSize;
            if (Environment.isExternalStorageEmulated()) {
                codeSize += this.mAppEntry.externalCodeSize;
                dataSize += this.mAppEntry.externalDataSize;
            } else {
                if (this.mLastExternalCodeSize != this.mAppEntry.externalCodeSize) {
                    this.mLastExternalCodeSize = this.mAppEntry.externalCodeSize;
                    this.mExternalCodeSize.setSummary(getSizeStr(this.mAppEntry.externalCodeSize));
                }
                if (this.mLastExternalDataSize != this.mAppEntry.externalDataSize) {
                    this.mLastExternalDataSize = this.mAppEntry.externalDataSize;
                    this.mExternalDataSize.setSummary(getSizeStr(this.mAppEntry.externalDataSize));
                }
            }
            if (this.mLastCodeSize != codeSize) {
                this.mLastCodeSize = codeSize;
                this.mAppSize.setSummary(getSizeStr(codeSize));
            }
            if (this.mLastDataSize != dataSize) {
                this.mLastDataSize = dataSize;
                this.mDataSize.setSummary(getSizeStr(dataSize));
            }
            long cacheSize = this.mAppEntry.cacheSize + this.mAppEntry.externalCacheSize;
            if (this.mLastCacheSize != cacheSize) {
                this.mLastCacheSize = cacheSize;
                this.mCacheSize.setSummary(getSizeStr(cacheSize));
            }
            if (this.mLastTotalSize != this.mAppEntry.size) {
                this.mLastTotalSize = this.mAppEntry.size;
                this.mTotalSize.setSummary(getSizeStr(this.mAppEntry.size));
            }
            if (this.mAppEntry.dataSize + this.mAppEntry.externalDataSize <= 0 || !this.mCanClearData) {
                this.mClearDataButton.setEnabled(false);
            } else {
                this.mClearDataButton.setEnabled(true);
                this.mClearDataButton.setOnClickListener(this);
            }
            if (cacheSize <= 0) {
                this.mClearCacheButton.setEnabled(false);
            } else {
                this.mClearCacheButton.setEnabled(true);
                this.mClearCacheButton.setOnClickListener(this);
            }
        }
        if (!this.mAppsControlDisallowedBySystem) {
            return;
        }
        this.mClearCacheButton.setEnabled(false);
        this.mClearDataButton.setEnabled(false);
    }

    @Override // com.android.settings.applications.AppInfoBase
    protected boolean refreshUi() {
        retrieveAppEntry();
        if (this.mAppEntry == null) {
            return false;
        }
        refreshSizeInfo();
        refreshGrantedUriPermissions();
        VolumeInfo currentVol = getActivity().getPackageManager().getPackageCurrentVolume(this.mAppEntry.info);
        StorageManager storage = (StorageManager) getContext().getSystemService(StorageManager.class);
        this.mStorageUsed.setSummary(storage.getBestVolumeDescription(currentVol));
        refreshButtons();
        return true;
    }

    private void refreshButtons() {
        initMoveDialog();
        initDataButtons();
    }

    private void initDataButtons() {
        if (this.mAppEntry.info.manageSpaceActivityName == null && ((this.mAppEntry.info.flags & 65) == 1 || this.mDpm.packageHasActiveAdmins(this.mPackageName))) {
            this.mClearDataButton.setText(R.string.clear_user_data_text);
            this.mClearDataButton.setEnabled(false);
            this.mCanClearData = false;
        } else {
            if (this.mAppEntry.info.manageSpaceActivityName != null) {
                this.mClearDataButton.setText(R.string.manage_space_text);
                StorageManager storageManager = (StorageManager) getActivity().getApplicationContext().getSystemService("storage");
                String extStoragePath = Environment.getLegacyExternalStorageDirectory().getPath();
                if (!storageManager.getVolumeState(extStoragePath).equals("mounted")) {
                    Log.d(TAG, "/mnt/sdcard is not mounted.");
                    if ((this.mAppEntry.info.flags & 262144) != 0) {
                        Log.d(TAG, "ApplicationInfo.FLAG_EXTERNAL_STORAGE");
                        this.mClearDataButton.setEnabled(false);
                        this.mCanClearData = false;
                    }
                } else {
                    Log.d(TAG, "/mnt/sdcard is mounted.");
                    this.mClearDataButton.setEnabled(true);
                    this.mCanClearData = true;
                }
            } else {
                this.mClearDataButton.setText(R.string.clear_user_data_text);
            }
            this.mClearDataButton.setOnClickListener(this);
        }
        if (!this.mAppsControlDisallowedBySystem) {
            return;
        }
        this.mClearDataButton.setEnabled(false);
    }

    private void initMoveDialog() {
        Context context = getActivity();
        StorageManager storage = (StorageManager) context.getSystemService(StorageManager.class);
        List<VolumeInfo> candidates = context.getPackageManager().getPackageCandidateVolumes(this.mAppEntry.info);
        if (candidates.size() > 1) {
            Collections.sort(candidates, VolumeInfo.getDescriptionComparator());
            CharSequence[] labels = new CharSequence[candidates.size()];
            int current = -1;
            for (int i = 0; i < candidates.size(); i++) {
                String volDescrip = storage.getBestVolumeDescription(candidates.get(i));
                if (Objects.equals(volDescrip, this.mStorageUsed.getSummary())) {
                    current = i;
                }
                labels[i] = volDescrip;
            }
            this.mCandidates = (VolumeInfo[]) candidates.toArray(new VolumeInfo[candidates.size()]);
            this.mDialogBuilder = new AlertDialog.Builder(getContext()).setTitle(R.string.change_storage).setSingleChoiceItems(labels, current, this).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
            return;
        }
        removePreference("storage_used");
        removePreference("change_storage_button");
        removePreference("storage_space");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initiateClearUserData() {
        this.mClearDataButton.setEnabled(false);
        String packageName = this.mAppEntry.info.packageName;
        Log.i(TAG, "Clearing user data for package : " + packageName);
        if (this.mClearDataObserver == null) {
            this.mClearDataObserver = new ClearUserDataObserver();
        }
        ActivityManager am = (ActivityManager) getActivity().getSystemService("activity");
        boolean res = am.clearApplicationUserData(packageName, this.mClearDataObserver);
        if (!res) {
            Log.i(TAG, "Couldnt clear application user data for package:" + packageName);
            showDialogInner(2, 0);
            return;
        }
        this.mClearDataButton.setText(R.string.recompute_size);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processClearMsg(Message msg) {
        int result = msg.arg1;
        String packageName = this.mAppEntry.info.packageName;
        this.mClearDataButton.setText(R.string.clear_user_data_text);
        if (result == 1) {
            Log.i(TAG, "Cleared user data for package : " + packageName);
            this.mState.requestSize(this.mPackageName, this.mUserId);
            Intent packageDataCleared = new Intent("com.mediatek.intent.action.SETTINGS_PACKAGE_DATA_CLEARED");
            packageDataCleared.putExtra("packageName", packageName);
            getActivity().sendBroadcast(packageDataCleared);
            return;
        }
        this.mClearDataButton.setEnabled(true);
    }

    private void refreshGrantedUriPermissions() {
        removeUriPermissionsFromUi();
        ActivityManager am = (ActivityManager) getActivity().getSystemService("activity");
        List<UriPermission> perms = am.getGrantedUriPermissions(this.mAppEntry.info.packageName).getList();
        if (perms.isEmpty()) {
            this.mClearUriButton.setVisibility(8);
            return;
        }
        PackageManager pm = getActivity().getPackageManager();
        Map<CharSequence, MutableInt> uriCounters = new TreeMap<>();
        for (UriPermission perm : perms) {
            String authority = perm.getUri().getAuthority();
            ProviderInfo provider = pm.resolveContentProvider(authority, 0);
            CharSequence app = provider.applicationInfo.loadLabel(pm);
            MutableInt count = uriCounters.get(app);
            if (count == null) {
                uriCounters.put(app, new MutableInt(1));
            } else {
                count.value++;
            }
        }
        for (Map.Entry<CharSequence, MutableInt> entry : uriCounters.entrySet()) {
            int numberResources = entry.getValue().value;
            Preference pref = new Preference(getPrefContext());
            pref.setTitle(entry.getKey());
            pref.setSummary(getPrefContext().getResources().getQuantityString(R.plurals.uri_permissions_text, numberResources, Integer.valueOf(numberResources)));
            pref.setSelectable(false);
            pref.setLayoutResource(R.layout.horizontal_preference);
            pref.setOrder(0);
            Log.v(TAG, "Adding preference '" + pref + "' at order 0");
            this.mUri.addPreference(pref);
        }
        if (this.mAppsControlDisallowedBySystem) {
            this.mClearUriButton.setEnabled(false);
        }
        this.mClearUri.setOrder(0);
        this.mClearUriButton.setVisibility(0);
    }

    private void clearUriPermissions() {
        ActivityManager am = (ActivityManager) getActivity().getSystemService("activity");
        am.clearGrantedUriPermissions(this.mAppEntry.info.packageName);
        refreshGrantedUriPermissions();
    }

    private void removeUriPermissionsFromUi() {
        int count = this.mUri.getPreferenceCount();
        for (int i = count - 1; i >= 0; i--) {
            Preference pref = this.mUri.getPreference(i);
            if (pref != this.mClearUri) {
                this.mUri.removePreference(pref);
            }
        }
    }

    @Override // com.android.settings.applications.AppInfoBase
    protected AlertDialog createDialog(int id, int errorCode) {
        switch (id) {
            case 1:
                return new AlertDialog.Builder(getActivity()).setTitle(getActivity().getText(R.string.clear_data_dlg_title)).setMessage(getActivity().getText(R.string.clear_data_dlg_text)).setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() { // from class: com.android.settings.applications.AppStorageSettings.2
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        AppStorageSettings.this.initiateClearUserData();
                    }
                }).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            case 2:
                return new AlertDialog.Builder(getActivity()).setTitle(getActivity().getText(R.string.clear_failed_dlg_title)).setMessage(getActivity().getText(R.string.clear_failed_dlg_text)).setNeutralButton(R.string.dlg_ok, new DialogInterface.OnClickListener() { // from class: com.android.settings.applications.AppStorageSettings.3
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        AppStorageSettings.this.mClearDataButton.setEnabled(false);
                        AppStorageSettings.this.setIntentAndFinish(false, false);
                    }
                }).create();
            default:
                return null;
        }
    }

    @Override // com.android.settings.applications.AppInfoBase, com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageSizeChanged(String packageName) {
        if (!packageName.equals(this.mAppEntry.info.packageName)) {
            return;
        }
        refreshSizeInfo();
    }

    public static CharSequence getSummary(ApplicationsState.AppEntry appEntry, Context context) {
        int i;
        if (appEntry.size == -2 || appEntry.size == -1) {
            return context.getText(R.string.computing_size);
        }
        if ((appEntry.info.flags & 262144) != 0) {
            i = R.string.storage_type_external;
        } else {
            i = R.string.storage_type_internal;
        }
        CharSequence storageType = context.getString(i);
        return context.getString(R.string.storage_summary_format, getSize(appEntry, context), storageType);
    }

    private static CharSequence getSize(ApplicationsState.AppEntry appEntry, Context context) {
        long size = appEntry.size;
        if (size == -1) {
            return context.getText(R.string.invalid_size_value);
        }
        return Formatter.formatFileSize(context, size);
    }

    @Override // com.android.settings.InstrumentedPreferenceFragment
    protected int getMetricsCategory() {
        return 19;
    }

    /* loaded from: classes.dex */
    class ClearCacheObserver extends IPackageDataObserver.Stub {
        ClearCacheObserver() {
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) {
            Message msg = AppStorageSettings.this.mHandler.obtainMessage(3);
            msg.arg1 = succeeded ? 1 : 2;
            AppStorageSettings.this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ClearUserDataObserver extends IPackageDataObserver.Stub {
        ClearUserDataObserver() {
        }

        public void onRemoveCompleted(String packageName, boolean succeeded) {
            Message msg = AppStorageSettings.this.mHandler.obtainMessage(1);
            msg.arg1 = succeeded ? 1 : 2;
            AppStorageSettings.this.mHandler.sendMessage(msg);
        }
    }
}
