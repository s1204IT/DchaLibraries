package com.android.managedprovisioning;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.persistentdata.PersistentDataBlockManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.managedprovisioning.MessageParser;
import com.android.managedprovisioning.UserConsentDialog;
import com.android.managedprovisioning.task.AddWifiNetworkTask;
import java.util.ArrayList;

public class DeviceOwnerProvisioningActivity extends Activity implements UserConsentDialog.ConsentCallback {
    private ProvisioningParams mParams;
    private TextView mProgressTextView;
    private BroadcastReceiver mServiceMessageReceiver;
    private boolean mUserConsented = false;
    private boolean mCancelDialogShown = false;
    private ArrayList<Intent> mPendingProvisioningIntents = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.mUserConsented = savedInstanceState.getBoolean("user_consented", false);
            this.mCancelDialogShown = savedInstanceState.getBoolean("cancel_dialog_shown", false);
            this.mPendingProvisioningIntents = savedInstanceState.getParcelableArrayList("pending_intents");
        }
        LayoutInflater inflater = getLayoutInflater();
        View contentView = inflater.inflate(R.layout.progress, (ViewGroup) null);
        setContentView(contentView);
        this.mProgressTextView = (TextView) findViewById(R.id.prog_text);
        TextView titleText = (TextView) findViewById(R.id.title);
        if (titleText != null) {
            titleText.setText(getString(R.string.setup_device));
        }
        if (this.mCancelDialogShown) {
            showCancelResetDialog();
        }
        if (Settings.Global.getInt(getContentResolver(), "device_provisioned", 0) != 0) {
            ProvisionLogger.loge("Device already provisioned.");
            error(R.string.device_owner_error_already_provisioned, false);
            return;
        }
        if (UserHandle.myUserId() != 0) {
            ProvisionLogger.loge("Device owner can only be set up for USER_OWNER.");
            error(R.string.device_owner_error_general, false);
            return;
        }
        if (factoryResetProtected()) {
            ProvisionLogger.loge("Factory reset protection blocks provisioning.");
            error(R.string.device_owner_error_already_provisioned, false);
            return;
        }
        this.mServiceMessageReceiver = new ServiceMessageReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.managedprovisioning.provisioning_success");
        filter.addAction("com.android.managedprovisioning.error");
        filter.addAction("com.android.managedprovisioning.progress_update");
        LocalBroadcastManager.getInstance(this).registerReceiver(this.mServiceMessageReceiver, filter);
        MessageParser parser = new MessageParser();
        try {
            this.mParams = parser.parseIntent(getIntent());
            if (!EncryptDeviceActivity.isDeviceEncrypted() && !SystemProperties.getBoolean("persist.sys.no_req_encrypt", false)) {
                requestEncryption(parser, this.mParams);
                finish();
            } else if (!AddWifiNetworkTask.isConnectedToWifi(this) && TextUtils.isEmpty(this.mParams.mWifiSsid)) {
                requestWifiPick();
            } else {
                showInterstitialAndProvision(this.mParams);
            }
        } catch (MessageParser.ParseException e) {
            ProvisionLogger.loge("Could not read data from intent", e);
            error(e.getErrorMessageId(), false);
        }
    }

    private boolean factoryResetProtected() {
        PersistentDataBlockManager pdbManager = (PersistentDataBlockManager) getSystemService("persistent_data_block");
        if (pdbManager != null) {
            return pdbManager.getDataBlockSize() > 0;
        }
        ProvisionLogger.loge("Unable to get persistent data block service");
        return false;
    }

    private void showInterstitialAndProvision(ProvisioningParams params) {
        if (this.mUserConsented || params.mStartedByNfc) {
            startDeviceOwnerProvisioningService(params);
        } else {
            UserConsentDialog.newInstance(2).show(getFragmentManager(), "UserConsentDialogFragment");
        }
    }

    @Override
    public void onDialogConsent() {
        this.mUserConsented = true;
        startDeviceOwnerProvisioningService(this.mParams);
    }

    @Override
    public void onDialogCancel() {
        finish();
    }

    private void startDeviceOwnerProvisioningService(ProvisioningParams params) {
        Intent intent = new Intent(this, (Class<?>) DeviceOwnerProvisioningService.class);
        intent.putExtra("ProvisioningParams", params);
        intent.putExtras(getIntent());
        startService(intent);
    }

    class ServiceMessageReceiver extends BroadcastReceiver {
        ServiceMessageReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DeviceOwnerProvisioningActivity.this.mCancelDialogShown) {
                DeviceOwnerProvisioningActivity.this.mPendingProvisioningIntents.add(intent);
            } else {
                DeviceOwnerProvisioningActivity.this.handleProvisioningIntent(intent);
            }
        }
    }

    private void handleProvisioningIntent(Intent intent) {
        int progressMessage;
        String action = intent.getAction();
        if (action.equals("com.android.managedprovisioning.provisioning_success")) {
            onProvisioningSuccess();
            return;
        }
        if (action.equals("com.android.managedprovisioning.error")) {
            int errorMessageId = intent.getIntExtra("UserVisibleErrorMessage-Id", R.string.device_owner_error_general);
            error(errorMessageId, true);
        } else if (action.equals("com.android.managedprovisioning.progress_update") && (progressMessage = intent.getIntExtra("ProgressMessageId", -1)) >= 0) {
            progressUpdate(progressMessage);
        }
    }

    private void onProvisioningSuccess() {
        Settings.Global.putInt(getContentResolver(), "device_provisioned", 1);
        Settings.Secure.putInt(getContentResolver(), "user_setup_complete", 1);
        Settings.Secure.putInt(getContentResolver(), "user_setup_device_owner", 1);
        Intent intent = new Intent("com.android.launcher.action.COMPLETE_SETUP_DEVICE_OWNER");
        sendBroadcast(intent);
        setResult(-1);
        finish();
    }

    private void requestEncryption(MessageParser messageParser, ProvisioningParams params) {
        Intent encryptIntent = new Intent(this, (Class<?>) EncryptDeviceActivity.class);
        Bundle resumeExtras = new Bundle();
        resumeExtras.putString("com.android.managedprovisioning.RESUME_TARGET", "device_owner");
        messageParser.addProvisioningParamsToBundle(resumeExtras, params);
        encryptIntent.putExtra("com.android.managedprovisioning.RESUME", resumeExtras);
        startActivityForResult(encryptIntent, 1);
    }

    private void requestWifiPick() {
        startActivityForResult(AddWifiNetworkTask.getWifiPickIntent(), 2);
    }

    @Override
    public void onBackPressed() {
        if (!this.mCancelDialogShown) {
            this.mCancelDialogShown = true;
            showCancelResetDialog();
        }
    }

    private void showCancelResetDialog() {
        new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string.device_owner_cancel_title).setMessage(R.string.device_owner_cancel_message).setNegativeButton(R.string.device_owner_cancel_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                DeviceOwnerProvisioningActivity.this.handlePendingIntents();
                DeviceOwnerProvisioningActivity.this.mCancelDialogShown = false;
            }
        }).setPositiveButton(R.string.device_owner_error_reset, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
                intent.addFlags(268435456);
                intent.putExtra("android.intent.extra.REASON", "DeviceOwnerProvisioningActivity.showCancelResetDialog()");
                DeviceOwnerProvisioningActivity.this.sendBroadcast(intent);
                DeviceOwnerProvisioningActivity.this.stopService(new Intent(DeviceOwnerProvisioningActivity.this, (Class<?>) DeviceOwnerProvisioningService.class));
                DeviceOwnerProvisioningActivity.this.finish();
            }
        }).show();
    }

    private void handlePendingIntents() {
        for (Intent intent : this.mPendingProvisioningIntents) {
            handleProvisioningIntent(intent);
        }
        this.mPendingProvisioningIntents.clear();
    }

    private void progressUpdate(int progressMessage) {
        this.mProgressTextView.setText(progressMessage);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == 0) {
                ProvisionLogger.loge("User canceled device encryption.");
                finish();
                return;
            }
            return;
        }
        if (requestCode == 2) {
            if (resultCode == 0) {
                ProvisionLogger.loge("User canceled wifi picking.");
                stopService(new Intent(this, (Class<?>) DeviceOwnerProvisioningService.class));
                finish();
            } else if (resultCode == -1) {
                if (AddWifiNetworkTask.isConnectedToWifi(this)) {
                    showInterstitialAndProvision(this.mParams);
                } else {
                    requestWifiPick();
                }
            }
        }
    }

    private void error(int dialogMessage, boolean resetRequired) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this).setTitle(R.string.provisioning_error_title).setMessage(dialogMessage).setCancelable(false);
        if (resetRequired) {
            alertBuilder.setPositiveButton(R.string.device_owner_error_reset, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
                    intent.addFlags(268435456);
                    intent.putExtra("android.intent.extra.REASON", "DeviceOwnerProvisioningActivity.error()");
                    DeviceOwnerProvisioningActivity.this.sendBroadcast(intent);
                    DeviceOwnerProvisioningActivity.this.stopService(new Intent(DeviceOwnerProvisioningActivity.this, (Class<?>) DeviceOwnerProvisioningService.class));
                    DeviceOwnerProvisioningActivity.this.finish();
                }
            });
        } else {
            alertBuilder.setPositiveButton(R.string.device_owner_error_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    DeviceOwnerProvisioningActivity.this.stopService(new Intent(DeviceOwnerProvisioningActivity.this, (Class<?>) DeviceOwnerProvisioningService.class));
                    DeviceOwnerProvisioningActivity.this.finish();
                }
            });
        }
        alertBuilder.show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("user_consented", this.mUserConsented);
        outState.putBoolean("cancel_dialog_shown", this.mCancelDialogShown);
        outState.putParcelableArrayList("pending_intents", this.mPendingProvisioningIntents);
    }

    @Override
    public void onDestroy() {
        if (this.mServiceMessageReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(this.mServiceMessageReceiver);
            this.mServiceMessageReceiver = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
