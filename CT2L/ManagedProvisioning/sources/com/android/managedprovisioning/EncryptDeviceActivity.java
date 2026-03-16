package com.android.managedprovisioning;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class EncryptDeviceActivity extends Activity {
    private Button mCancelButton;
    private Button mEncryptButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View contentView = getLayoutInflater().inflate(R.layout.encrypt_device, (ViewGroup) null);
        this.mEncryptButton = (Button) contentView.findViewById(R.id.accept_button);
        this.mCancelButton = (Button) contentView.findViewById(R.id.cancel_button);
        setContentView(contentView);
        this.mEncryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle resumeInfo = EncryptDeviceActivity.this.getIntent().getBundleExtra("com.android.managedprovisioning.RESUME");
                BootReminder.setProvisioningReminder(EncryptDeviceActivity.this, resumeInfo);
                Intent intent = new Intent();
                intent.setAction("android.app.action.START_ENCRYPTION");
                EncryptDeviceActivity.this.startActivity(intent);
            }
        });
        this.mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EncryptDeviceActivity.this.finish();
            }
        });
    }

    public static boolean isDeviceEncrypted() {
        IMountService mountService = IMountService.Stub.asInterface(ServiceManager.getService("mount"));
        try {
            return mountService.getEncryptionState() == 0;
        } catch (RemoteException e) {
            return false;
        }
    }
}
