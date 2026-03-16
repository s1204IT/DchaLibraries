package com.android.managedprovisioning;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.io.IOException;

public class ProfileOwnerProvisioningActivity extends Activity {
    private AccountManager mAccountManager;
    private BroadcastReceiver mServiceMessageReceiver;
    private int mCancelStatus = 1;
    private Intent mPendingProvisioningResult = null;
    private ProgressDialog mCancelProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ProvisionLogger.logd("Profile owner provisioning activity ONCREATE");
        this.mAccountManager = (AccountManager) getSystemService("account");
        if (savedInstanceState != null) {
            this.mCancelStatus = savedInstanceState.getInt("cancelstatus", 1);
            this.mPendingProvisioningResult = (Intent) savedInstanceState.getParcelable("pending_intent");
        }
        LayoutInflater inflater = getLayoutInflater();
        View contentView = inflater.inflate(R.layout.progress, (ViewGroup) null);
        setContentView(contentView);
        TextView textView = (TextView) findViewById(R.id.prog_text);
        if (textView != null) {
            textView.setText(getString(R.string.setting_up_workspace));
        }
        if (this.mCancelStatus == 2) {
            showCancelProvisioningDialog();
        } else if (this.mCancelStatus == 3) {
            showCancelProgressDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mServiceMessageReceiver = new ServiceMessageReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.managedprovisioning.provisioning_success");
        filter.addAction("com.android.managedprovisioning.error");
        filter.addAction("com.android.managedprovisioning.cancelled");
        LocalBroadcastManager.getInstance(this).registerReceiver(this.mServiceMessageReceiver, filter);
        Handler handler = new Handler(getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(ProfileOwnerProvisioningActivity.this, (Class<?>) ProfileOwnerProvisioningService.class);
                intent.putExtras(ProfileOwnerProvisioningActivity.this.getIntent());
                ProfileOwnerProvisioningActivity.this.startService(intent);
            }
        });
    }

    class ServiceMessageReceiver extends BroadcastReceiver {
        ServiceMessageReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ProfileOwnerProvisioningActivity.this.mCancelStatus == 2) {
                ProfileOwnerProvisioningActivity.this.mPendingProvisioningResult = intent;
            } else {
                ProfileOwnerProvisioningActivity.this.handleProvisioningResult(intent);
            }
        }
    }

    private void handleProvisioningResult(Intent intent) {
        String action = intent.getAction();
        if ("com.android.managedprovisioning.provisioning_success".equals(action)) {
            if (this.mCancelStatus != 3) {
                ProvisionLogger.logd("Successfully provisioned.Finishing ProfileOwnerProvisioningActivity");
                Intent pendingIntent = (Intent) intent.getParcelableExtra("com.android.managedprovisioning.extra.pending_success_intent");
                int serialNumber = intent.getIntExtra("com.android.managedprovisioning.extra.profile_user_serial_number", -1);
                int userId = intent.getIntExtra("com.android.managedprovisioning.extra.profile_user_id", -1);
                onProvisioningSuccess(pendingIntent, userId, serialNumber);
            } else {
                return;
            }
        } else if ("com.android.managedprovisioning.error".equals(action)) {
            if (this.mCancelStatus != 3) {
                String errorLogMessage = intent.getStringExtra("ProvisioingErrorLogMessage");
                ProvisionLogger.logd("Error reported: " + errorLogMessage);
                error(R.string.managed_provisioning_error_text, errorLogMessage);
            } else {
                return;
            }
        }
        if ("com.android.managedprovisioning.cancelled".equals(action) && this.mCancelStatus == 3) {
            this.mCancelProgressDialog.dismiss();
            setResult(0);
            stopService(new Intent(this, (Class<?>) ProfileOwnerProvisioningService.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (this.mCancelStatus == 1) {
            showCancelProvisioningDialog();
        }
    }

    private void showCancelProvisioningDialog() {
        this.mCancelStatus = 2;
        AlertDialog alertDialog = new AlertDialog.Builder(this).setCancelable(false).setTitle(R.string.profile_owner_cancel_title).setMessage(R.string.profile_owner_cancel_message).setNegativeButton(R.string.profile_owner_cancel_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                ProfileOwnerProvisioningActivity.this.mCancelStatus = 1;
                if (ProfileOwnerProvisioningActivity.this.mPendingProvisioningResult != null) {
                    ProfileOwnerProvisioningActivity.this.handleProvisioningResult(ProfileOwnerProvisioningActivity.this.mPendingProvisioningResult);
                }
            }
        }).setPositiveButton(R.string.profile_owner_cancel_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                ProfileOwnerProvisioningActivity.this.confirmCancel();
            }
        }).create();
        alertDialog.show();
    }

    protected void showCancelProgressDialog() {
        this.mCancelProgressDialog = new ProgressDialog(this);
        this.mCancelProgressDialog.setMessage(getText(R.string.profile_owner_cancelling));
        this.mCancelProgressDialog.setCancelable(false);
        this.mCancelProgressDialog.setCanceledOnTouchOutside(false);
        this.mCancelProgressDialog.show();
    }

    public void error(int resourceId, String logText) {
        ProvisionLogger.loge(logText);
        new AlertDialog.Builder(this).setTitle(R.string.provisioning_error_title).setMessage(getString(resourceId)).setCancelable(false).setPositiveButton(R.string.device_owner_error_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                ProfileOwnerProvisioningActivity.this.confirmCancel();
            }
        }).show();
    }

    private void confirmCancel() {
        this.mCancelStatus = 3;
        Intent intent = new Intent(this, (Class<?>) ProfileOwnerProvisioningService.class);
        intent.setAction("com.android.managedprovisioning.CANCEL_PROVISIONING");
        startService(intent);
        showCancelProgressDialog();
    }

    private void onProvisioningSuccess(Intent pendingSuccessIntent, int userId, int serialNumber) {
        this.mCancelStatus = 4;
        Settings.Secure.putIntForUser(getContentResolver(), "user_setup_complete", 1, userId);
        UserManager userManager = (UserManager) getSystemService("user");
        UserHandle userHandle = userManager.getUserForSerialNumber(serialNumber);
        BroadcastReceiver mdmReceivedSuccessReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ProvisionLogger.logd("ACTION_PROFILE_PROVISIONING_COMPLETE broadcast received by mdm");
                ProfileOwnerProvisioningActivity.this.setResult(-1);
                if (ProfileOwnerProvisioningActivity.this.getIntent().hasExtra("android.app.extra.PROVISIONING_ACCOUNT_TO_MIGRATE")) {
                    ProvisionLogger.logd("Cleaning up account from the primary user.");
                    final Account account = (Account) ProfileOwnerProvisioningActivity.this.getIntent().getParcelableExtra("android.app.extra.PROVISIONING_ACCOUNT_TO_MIGRATE");
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            ProfileOwnerProvisioningActivity.this.removeAccount(account);
                            return null;
                        }
                    }.execute(new Void[0]);
                }
                ProfileOwnerProvisioningActivity.this.finish();
                ProfileOwnerProvisioningActivity.this.stopService(new Intent(ProfileOwnerProvisioningActivity.this, (Class<?>) ProfileOwnerProvisioningService.class));
            }
        };
        sendOrderedBroadcastAsUser(pendingSuccessIntent, userHandle, null, mdmReceivedSuccessReceiver, null, -1, null, null);
        ProvisionLogger.logd("Provisioning complete broadcast has been sent to user " + userHandle.getIdentifier());
    }

    private void removeAccount(Account account) {
        try {
            AccountManagerFuture<Bundle> bundle = this.mAccountManager.removeAccount(account, this, null, null);
            if (bundle.getResult().getBoolean("booleanResult", false)) {
                ProvisionLogger.logw("Account removed from the primary user.");
            } else {
                ProvisionLogger.logw("Could not remove account from the primary user.");
            }
        } catch (AuthenticatorException | OperationCanceledException | IOException e) {
            ProvisionLogger.logw("Exception removing account from the primary user.", e);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("cancelstatus", this.mCancelStatus);
        outState.putParcelable("pending_intent", this.mPendingProvisioningResult);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.mServiceMessageReceiver);
        super.onPause();
    }
}
