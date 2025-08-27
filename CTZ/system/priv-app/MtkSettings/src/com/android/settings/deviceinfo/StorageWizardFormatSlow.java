package com.android.settings.deviceinfo;

import android.content.Intent;
import android.os.Bundle;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

/* loaded from: classes.dex */
public class StorageWizardFormatSlow extends StorageWizardBase {
    private boolean mFormatPrivate;

    @Override // com.android.settings.deviceinfo.StorageWizardBase, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (this.mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_generic);
        this.mFormatPrivate = getIntent().getBooleanExtra("format_private", false);
        setHeaderText(R.string.storage_wizard_slow_v2_title, getDiskShortDescription());
        setBodyText(R.string.storage_wizard_slow_v2_body, getDiskDescription(), getDiskShortDescription(), getDiskShortDescription(), getDiskShortDescription());
        setBackButtonText(R.string.storage_wizard_slow_v2_start_over, new CharSequence[0]);
        setNextButtonText(R.string.storage_wizard_slow_v2_continue, new CharSequence[0]);
        if (!getIntent().getBooleanExtra("format_slow", false)) {
            onNavigateNext(null);
        }
    }

    @Override // com.android.settings.deviceinfo.StorageWizardBase
    public void onNavigateBack(View view) {
        FeatureFactory.getFactory(this).getMetricsFeatureProvider().action(this, 1411, new Pair[0]);
        startActivity(new Intent(this, (Class<?>) StorageWizardInit.class));
        finishAffinity();
    }

    @Override // com.android.settings.deviceinfo.StorageWizardBase
    public void onNavigateNext(View view) {
        VolumeInfo primaryStorageCurrentVolume;
        boolean z = false;
        if (view != null) {
            FeatureFactory.getFactory(this).getMetricsFeatureProvider().action(this, 1410, new Pair[0]);
        } else {
            FeatureFactory.getFactory(this).getMetricsFeatureProvider().action(this, 1409, new Pair[0]);
        }
        String stringExtra = getIntent().getStringExtra("format_forget_uuid");
        if (!TextUtils.isEmpty(stringExtra)) {
            this.mStorage.forgetVolume(stringExtra);
        }
        if (this.mFormatPrivate && (primaryStorageCurrentVolume = getPackageManager().getPrimaryStorageCurrentVolume()) != null && "private".equals(primaryStorageCurrentVolume.getId())) {
            z = true;
        }
        if (z) {
            Intent intent = new Intent(this, (Class<?>) StorageWizardMigrateConfirm.class);
            intent.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
            startActivity(intent);
        } else {
            Intent intent2 = new Intent(this, (Class<?>) StorageWizardReady.class);
            intent2.putExtra("android.os.storage.extra.DISK_ID", this.mDisk.getId());
            startActivity(intent2);
        }
        finishAffinity();
    }
}
