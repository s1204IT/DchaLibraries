package com.android.settings.deviceinfo;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import com.android.settings.R;

public class StorageWizardInit extends StorageWizardBase {
    private boolean mIsPermittedToAdopt;
    private RadioButton mRadioExternal;
    private RadioButton mRadioInternal;
    private final CompoundButton.OnCheckedChangeListener mRadioListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                return;
            }
            if (buttonView == StorageWizardInit.this.mRadioExternal) {
                StorageWizardInit.this.mRadioInternal.setChecked(false);
                StorageWizardInit.this.setIllustrationType(2);
            } else if (buttonView == StorageWizardInit.this.mRadioInternal) {
                StorageWizardInit.this.mRadioExternal.setChecked(false);
                StorageWizardInit.this.setIllustrationType(1);
            }
            StorageWizardInit.this.getNextButton().setEnabled(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_init);
        boolean z = UserManager.get(this).isAdminUser() && !ActivityManager.isUserAMonkey();
        this.mIsPermittedToAdopt = z;
        setIllustrationType(0);
        setHeaderText(R.string.storage_wizard_init_title, this.mDisk.getDescription());
        this.mRadioExternal = (RadioButton) findViewById(R.id.storage_wizard_init_external_title);
        this.mRadioInternal = (RadioButton) findViewById(R.id.storage_wizard_init_internal_title);
        this.mRadioExternal.setOnCheckedChangeListener(this.mRadioListener);
        this.mRadioInternal.setOnCheckedChangeListener(this.mRadioListener);
        findViewById(R.id.storage_wizard_init_external_summary).setPadding(this.mRadioExternal.getCompoundPaddingLeft(), 0, this.mRadioExternal.getCompoundPaddingRight(), 0);
        findViewById(R.id.storage_wizard_init_internal_summary).setPadding(this.mRadioExternal.getCompoundPaddingLeft(), 0, this.mRadioExternal.getCompoundPaddingRight(), 0);
        getNextButton().setEnabled(false);
        if (!this.mDisk.isAdoptable()) {
            this.mRadioExternal.setChecked(true);
            onNavigateNext();
            finish();
        }
        this.mRadioInternal.setEnabled(false);
    }

    @Override
    public void onNavigateNext() {
        if (this.mRadioExternal.isChecked()) {
            if (this.mVolume != null && this.mVolume.getType() == 0 && this.mVolume.getState() != 6) {
                this.mStorage.setVolumeInited(this.mVolume.getFsUuid(), true);
                Intent intent = new Intent(this, (Class<?>) StorageWizardReady.class);
                intent.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
                startActivity(intent);
                return;
            }
            Intent intent2 = new Intent(this, (Class<?>) StorageWizardFormatConfirm.class);
            intent2.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
            intent2.putExtra("format_private", false);
            startActivity(intent2);
            return;
        }
        if (!this.mRadioInternal.isChecked()) {
            return;
        }
        Intent intent3 = new Intent(this, (Class<?>) StorageWizardFormatConfirm.class);
        intent3.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
        intent3.putExtra("format_private", true);
        startActivity(intent3);
    }
}
