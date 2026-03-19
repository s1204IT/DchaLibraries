package com.android.managedprovisioning.preprovisioning;

import android.content.Intent;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.SetupGlifLayoutActivity;
import com.android.managedprovisioning.model.CustomizationParams;
import com.android.managedprovisioning.model.ProvisioningParams;

public class EncryptDeviceActivity extends SetupGlifLayoutActivity {
    private ProvisioningParams mParams;

    protected EncryptionController getEncryptionController() {
        return EncryptionController.getInstance(this);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mParams = (ProvisioningParams) getIntent().getParcelableExtra("provisioningParams");
        if (this.mParams == null) {
            ProvisionLogger.loge("Missing params in EncryptDeviceActivity activity");
            finish();
            return;
        }
        if (getUtils().isProfileOwnerAction(this.mParams.provisioningAction)) {
            initializeUi(R.string.setup_work_profile, R.string.setup_profile_encryption, R.string.encrypt_device_text_for_profile_owner_setup);
        } else if (getUtils().isDeviceOwnerAction(this.mParams.provisioningAction)) {
            initializeUi(R.string.setup_work_device, R.string.setup_device_encryption, R.string.encrypt_device_text_for_device_owner_setup);
        } else {
            ProvisionLogger.loge("Unknown provisioning action: " + this.mParams.provisioningAction);
            finish();
            return;
        }
        ((Button) findViewById(R.id.encrypt_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                EncryptDeviceActivity.lambda$onCreate$0(this.f$0, view);
            }
        });
    }

    public static void lambda$onCreate$0(EncryptDeviceActivity encryptDeviceActivity, View view) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        encryptDeviceActivity.getEncryptionController().setEncryptionReminder(encryptDeviceActivity.mParams);
        encryptDeviceActivity.startActivity(new Intent("android.app.action.START_ENCRYPTION"));
    }

    @Override
    protected int getMetricsCategory() {
        return 521;
    }

    private void initializeUi(int i, int i2, int i3) {
        CustomizationParams customizationParamsCreateInstance = CustomizationParams.createInstance(this.mParams, this, this.mUtils);
        initializeLayoutParams(R.layout.encrypt_device, Integer.valueOf(i), customizationParamsCreateInstance.mainColor, customizationParamsCreateInstance.statusBarColor);
        setTitle(i2);
        ((TextView) findViewById(R.id.encrypt_main_text)).setText(i3);
    }
}
