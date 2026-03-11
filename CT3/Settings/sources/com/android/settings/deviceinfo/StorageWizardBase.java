package com.android.settings.deviceinfo;

import android.app.Activity;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.view.Illustration;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;

public abstract class StorageWizardBase extends Activity {
    private View mCustomNav;
    private Button mCustomNext;
    protected DiskInfo mDisk;
    protected StorageManager mStorage;
    private final StorageEventListener mStorageListener = new StorageEventListener() {
        public void onDiskDestroyed(DiskInfo disk) {
            if (!StorageWizardBase.this.mDisk.id.equals(disk.id)) {
                return;
            }
            StorageWizardBase.this.finish();
        }
    };
    protected VolumeInfo mVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.isMonkeyRunning()) {
            finish();
        }
        this.mStorage = (StorageManager) getSystemService(StorageManager.class);
        String volumeId = getIntent().getStringExtra("android.os.storage.extra.VOLUME_ID");
        if (!TextUtils.isEmpty(volumeId)) {
            this.mVolume = this.mStorage.findVolumeById(volumeId);
        }
        String diskId = getIntent().getStringExtra("android.os.storage.extra.DISK_ID");
        if (!TextUtils.isEmpty(diskId)) {
            this.mDisk = this.mStorage.findDiskById(diskId);
        } else if (this.mVolume != null) {
            this.mDisk = this.mVolume.getDisk();
        }
        setTheme(R.style.SetupWizardStorageStyle);
        if (this.mDisk == null) {
            return;
        }
        this.mStorage.registerListener(this.mStorageListener);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ViewGroup navParent = (ViewGroup) findViewById(R.id.suw_layout_navigation_bar).getParent();
        this.mCustomNav = getLayoutInflater().inflate(R.layout.storage_wizard_navigation, navParent, false);
        this.mCustomNext = (Button) this.mCustomNav.findViewById(R.id.suw_navbar_next);
        this.mCustomNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StorageWizardBase.this.onNavigateNext();
            }
        });
        for (int i = 0; i < navParent.getChildCount(); i++) {
            if (navParent.getChildAt(i).getId() == R.id.suw_layout_navigation_bar) {
                navParent.removeViewAt(i);
                navParent.addView(this.mCustomNav, i);
                return;
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(-2147417856);
        window.setStatusBarColor(0);
        this.mCustomNav.setSystemUiVisibility(1280);
        View scrollView = findViewById(R.id.suw_bottom_scroll_view);
        scrollView.setVerticalFadingEdgeEnabled(true);
        scrollView.setFadingEdgeLength(scrollView.getVerticalFadingEdgeLength() * 2);
        if (!(findViewById(R.id.suw_layout_decor) instanceof Illustration)) {
            return;
        }
        View title = findViewById(R.id.suw_layout_title);
        title.setPadding(title.getPaddingLeft(), 0, title.getPaddingRight(), title.getPaddingBottom());
    }

    @Override
    protected void onDestroy() {
        this.mStorage.unregisterListener(this.mStorageListener);
        super.onDestroy();
    }

    protected Button getNextButton() {
        return this.mCustomNext;
    }

    protected SetupWizardLayout getSetupWizardLayout() {
        return (SetupWizardLayout) findViewById(R.id.setup_wizard_layout);
    }

    protected ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.storage_wizard_progress);
    }

    protected void setCurrentProgress(int progress) {
        getProgressBar().setProgress(progress);
        ((TextView) findViewById(R.id.storage_wizard_progress_summary)).setText(NumberFormat.getPercentInstance().format(((double) progress) / 100.0d));
    }

    protected void setHeaderText(int resId, String... args) {
        CharSequence headerText = TextUtils.expandTemplate(getText(resId), args);
        getSetupWizardLayout().setHeaderText(headerText);
        setTitle(headerText);
    }

    protected void setBodyText(int resId, String... args) {
        ((TextView) findViewById(R.id.storage_wizard_body)).setText(TextUtils.expandTemplate(getText(resId), args));
    }

    protected void setSecondaryBodyText(int resId, String... args) {
        TextView secondBody = (TextView) findViewById(R.id.storage_wizard_second_body);
        secondBody.setText(TextUtils.expandTemplate(getText(resId), args));
        secondBody.setVisibility(0);
    }

    protected void setIllustrationType(int type) {
        switch (type) {
            case DefaultWfcSettingsExt.RESUME:
                getSetupWizardLayout().setIllustration(R.drawable.bg_setup_header, R.drawable.bg_header_horizontal_tile);
                break;
            case DefaultWfcSettingsExt.PAUSE:
                getSetupWizardLayout().setIllustration(R.drawable.bg_internal_storage_header, R.drawable.bg_header_horizontal_tile);
                break;
            case DefaultWfcSettingsExt.CREATE:
                getSetupWizardLayout().setIllustration(R.drawable.bg_portable_storage_header, R.drawable.bg_header_horizontal_tile);
                break;
        }
    }

    protected void setKeepScreenOn(boolean keepScreenOn) {
        getSetupWizardLayout().setKeepScreenOn(keepScreenOn);
    }

    public void onNavigateNext() {
        throw new UnsupportedOperationException();
    }

    protected VolumeInfo findFirstVolume(int type) {
        List<VolumeInfo> vols = this.mStorage.getVolumes();
        for (VolumeInfo vol : vols) {
            if (Objects.equals(this.mDisk.getId(), vol.getDiskId()) && vol.getType() == type) {
                return vol;
            }
        }
        return null;
    }
}
