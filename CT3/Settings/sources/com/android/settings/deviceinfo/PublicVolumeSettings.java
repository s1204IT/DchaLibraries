package com.android.settings.deviceinfo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.provider.DocumentsContract;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.deviceinfo.StorageSettings;
import java.io.File;
import java.util.Objects;

public class PublicVolumeSettings extends SettingsPreferenceFragment {
    private DiskInfo mDisk;
    private Preference mFormatPrivate;
    private Preference mFormatPublic;
    private boolean mIsPermittedToAdopt;
    private Preference mMount;
    private StorageManager mStorageManager;
    private StorageSummaryPreference mSummary;
    private Button mUnmount;
    private VolumeInfo mVolume;
    private String mVolumeId;
    private final View.OnClickListener mUnmountListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new StorageSettings.UnmountTask(PublicVolumeSettings.this.getActivity(), PublicVolumeSettings.this.mVolume).execute(new Void[0]);
        }
    };
    private final StorageEventListener mStorageListener = new StorageEventListener() {
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (!Objects.equals(PublicVolumeSettings.this.mVolume.getId(), vol.getId())) {
                return;
            }
            PublicVolumeSettings.this.mVolume = vol;
            PublicVolumeSettings.this.update();
        }

        public void onVolumeRecordChanged(VolumeRecord rec) {
            if (!Objects.equals(PublicVolumeSettings.this.mVolume.getFsUuid(), rec.getFsUuid())) {
                return;
            }
            PublicVolumeSettings.this.mVolume = PublicVolumeSettings.this.mStorageManager.findVolumeById(PublicVolumeSettings.this.mVolumeId);
            PublicVolumeSettings.this.update();
        }
    };

    private boolean isVolumeValid() {
        if (this.mVolume == null || this.mVolume.getType() != 0) {
            return false;
        }
        return this.mVolume.isMountedReadable();
    }

    @Override
    protected int getMetricsCategory() {
        return 42;
    }

    @Override
    public void onCreate(Bundle icicle) {
        boolean z = false;
        super.onCreate(icicle);
        Context context = getActivity();
        if (UserManager.get(context).isAdminUser() && !ActivityManager.isUserAMonkey()) {
            z = true;
        }
        this.mIsPermittedToAdopt = z;
        this.mStorageManager = (StorageManager) context.getSystemService(StorageManager.class);
        if ("android.provider.action.DOCUMENT_ROOT_SETTINGS".equals(getActivity().getIntent().getAction())) {
            Uri rootUri = getActivity().getIntent().getData();
            String fsUuid = DocumentsContract.getRootId(rootUri);
            this.mVolume = this.mStorageManager.findVolumeByUuid(fsUuid);
        } else {
            String volId = getArguments().getString("android.os.storage.extra.VOLUME_ID");
            this.mVolume = this.mStorageManager.findVolumeById(volId);
        }
        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }
        this.mDisk = this.mStorageManager.findDiskById(this.mVolume.getDiskId());
        Preconditions.checkNotNull(this.mDisk);
        this.mVolumeId = this.mVolume.getId();
        addPreferencesFromResource(R.xml.device_info_storage_volume);
        getPreferenceScreen().setOrderingAsAdded(true);
        this.mSummary = new StorageSummaryPreference(getPrefContext());
        this.mMount = buildAction(R.string.storage_menu_mount);
        this.mUnmount = new Button(getActivity());
        this.mUnmount.setText(R.string.storage_menu_unmount);
        this.mUnmount.setOnClickListener(this.mUnmountListener);
        this.mFormatPublic = buildAction(R.string.storage_menu_format);
        if (!this.mIsPermittedToAdopt) {
            return;
        }
        this.mFormatPrivate = buildAction(R.string.storage_menu_format_private);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }
        Resources resources = getResources();
        int padding = resources.getDimensionPixelSize(R.dimen.unmount_button_padding);
        ViewGroup buttonBar = getButtonBar();
        buttonBar.removeAllViews();
        buttonBar.setPadding(padding, padding, padding, padding);
        buttonBar.addView(this.mUnmount, new ViewGroup.LayoutParams(-1, -2));
    }

    public void update() {
        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }
        getActivity().setTitle(this.mStorageManager.getBestVolumeDescription(this.mVolume));
        Context context = getActivity();
        PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        if (this.mVolume.isMountedReadable()) {
            addPreference(this.mSummary);
            File file = this.mVolume.getPath();
            long totalBytes = file.getTotalSpace();
            long freeBytes = file.getFreeSpace();
            long usedBytes = totalBytes - freeBytes;
            Formatter.BytesResult result = Formatter.formatBytes(getResources(), usedBytes, 0);
            this.mSummary.setTitle(TextUtils.expandTemplate(getText(R.string.storage_size_large), result.value, result.units));
            this.mSummary.setSummary(getString(R.string.storage_volume_used, new Object[]{Formatter.formatFileSize(context, totalBytes)}));
            this.mSummary.setPercent((int) ((100 * usedBytes) / totalBytes));
        }
        if (this.mVolume.getState() == 0) {
            addPreference(this.mMount);
        }
        if (this.mVolume.isMountedReadable()) {
            getButtonBar().setVisibility(0);
        }
        addPreference(this.mFormatPublic);
    }

    private void addPreference(Preference pref) {
        pref.setOrder(Integer.MAX_VALUE);
        getPreferenceScreen().addPreference(pref);
    }

    private Preference buildAction(int titleRes) {
        Preference pref = new Preference(getPrefContext());
        pref.setTitle(titleRes);
        return pref;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mVolume = this.mStorageManager.findVolumeById(this.mVolumeId);
        if (!isVolumeValid()) {
            getActivity().finish();
        } else {
            this.mStorageManager.registerListener(this.mStorageListener);
            update();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mStorageManager.unregisterListener(this.mStorageListener);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference pref) {
        Context context = getActivity();
        if (pref == this.mMount) {
            new StorageSettings.MountTask(context, this.mVolume).execute(new Void[0]);
        } else if (pref == this.mFormatPublic) {
            Intent intent = new Intent(context, (Class<?>) StorageWizardFormatConfirm.class);
            intent.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
            intent.putExtra("format_private", false);
            startActivity(intent);
        } else if (pref == this.mFormatPrivate) {
            Intent intent2 = new Intent(context, (Class<?>) StorageWizardFormatConfirm.class);
            intent2.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
            intent2.putExtra("format_private", true);
            startActivity(intent2);
        }
        return super.onPreferenceTreeClick(pref);
    }
}
