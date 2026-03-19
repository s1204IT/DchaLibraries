package com.android.backupconfirm;

import android.app.Activity;
import android.app.backup.IBackupManager;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IStorageManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Slog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BackupRestoreConfirmation extends Activity {
    String mAction;
    Button mAllowButton;
    IBackupManager mBackupManager;
    TextView mCurPassword;
    Button mDenyButton;
    boolean mDidAcknowledge;
    TextView mEncPassword;
    Handler mHandler;
    boolean mIsEncrypted;
    FullObserver mObserver;
    TextView mStatusView;
    IStorageManager mStorageManager;
    int mToken;

    class ObserverHandler extends Handler {
        Context mContext;

        ObserverHandler(Context context) {
            this.mContext = context;
            BackupRestoreConfirmation.this.mDidAcknowledge = false;
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 100) {
                switch (i) {
                    case 1:
                        Toast.makeText(this.mContext, R.string.toast_backup_started, 1).show();
                        break;
                    case 2:
                        BackupRestoreConfirmation.this.mStatusView.setText((String) message.obj);
                        break;
                    case 3:
                        Toast.makeText(this.mContext, R.string.toast_backup_ended, 1).show();
                        BackupRestoreConfirmation.this.finish();
                        break;
                    default:
                        switch (i) {
                            case 11:
                                Toast.makeText(this.mContext, R.string.toast_restore_started, 1).show();
                                break;
                            case 12:
                                BackupRestoreConfirmation.this.mStatusView.setText((String) message.obj);
                                break;
                            case 13:
                                Toast.makeText(this.mContext, R.string.toast_restore_ended, 0).show();
                                BackupRestoreConfirmation.this.finish();
                                break;
                        }
                        break;
                }
            }
            Toast.makeText(this.mContext, R.string.toast_timeout, 1).show();
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        if (!setTokenOrFinish(intent, bundle)) {
            return;
        }
        this.mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        this.mStorageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
        this.mHandler = new ObserverHandler(getApplicationContext());
        Object lastNonConfigurationInstance = getLastNonConfigurationInstance();
        if (lastNonConfigurationInstance == null) {
            this.mObserver = new FullObserver(this.mHandler);
        } else {
            this.mObserver = (FullObserver) lastNonConfigurationInstance;
            this.mObserver.setHandler(this.mHandler);
        }
        setViews(intent, bundle);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!setTokenOrFinish(intent, null)) {
            return;
        }
        setViews(intent, null);
    }

    private boolean setTokenOrFinish(Intent intent, Bundle bundle) {
        this.mToken = intent.getIntExtra("conftoken", -1);
        if (bundle != null) {
            this.mToken = bundle.getInt("token", this.mToken);
        }
        if (this.mToken < 0) {
            Slog.e("BackupRestoreConfirmation", "Backup/restore confirmation requested but no token passed!");
            finish();
            return false;
        }
        return true;
    }

    private void setViews(Intent intent, Bundle bundle) {
        int i;
        int i2;
        this.mAction = intent.getAction();
        if (bundle != null) {
            this.mAction = bundle.getString("action", this.mAction);
        }
        if (this.mAction.equals("fullback")) {
            i2 = R.string.backup_confirm_title;
            i = R.layout.confirm_backup;
        } else if (this.mAction.equals("fullrest")) {
            i = R.layout.confirm_restore;
            i2 = R.string.restore_confirm_title;
        } else {
            Slog.w("BackupRestoreConfirmation", "Backup/restore confirmation activity launched with invalid action!");
            finish();
            return;
        }
        setTitle(i2);
        setContentView(i);
        this.mStatusView = (TextView) findViewById(R.id.package_name);
        this.mAllowButton = (Button) findViewById(R.id.button_allow);
        this.mDenyButton = (Button) findViewById(R.id.button_deny);
        this.mCurPassword = (TextView) findViewById(R.id.password);
        this.mEncPassword = (TextView) findViewById(R.id.enc_password);
        TextView textView = (TextView) findViewById(R.id.password_desc);
        this.mAllowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BackupRestoreConfirmation.this.sendAcknowledgement(BackupRestoreConfirmation.this.mToken, true, BackupRestoreConfirmation.this.mObserver);
                BackupRestoreConfirmation.this.mAllowButton.setEnabled(false);
                BackupRestoreConfirmation.this.mDenyButton.setEnabled(false);
            }
        });
        this.mDenyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BackupRestoreConfirmation.this.sendAcknowledgement(BackupRestoreConfirmation.this.mToken, false, BackupRestoreConfirmation.this.mObserver);
                BackupRestoreConfirmation.this.mAllowButton.setEnabled(false);
                BackupRestoreConfirmation.this.mDenyButton.setEnabled(false);
                BackupRestoreConfirmation.this.finish();
            }
        });
        if (bundle != null) {
            this.mDidAcknowledge = bundle.getBoolean("did_acknowledge", false);
            this.mAllowButton.setEnabled(!this.mDidAcknowledge);
            this.mDenyButton.setEnabled(!this.mDidAcknowledge);
        }
        this.mIsEncrypted = deviceIsEncrypted();
        if (!haveBackupPassword()) {
            textView.setVisibility(8);
            this.mCurPassword.setVisibility(8);
            if (i == R.layout.confirm_backup) {
                TextView textView2 = (TextView) findViewById(R.id.enc_password_desc);
                if (this.mIsEncrypted) {
                    textView2.setText(R.string.backup_enc_password_required);
                    monitorEncryptionPassword();
                } else {
                    textView2.setText(R.string.backup_enc_password_optional);
                }
            }
        }
    }

    private void monitorEncryptionPassword() {
        this.mAllowButton.setEnabled(false);
        this.mEncPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                BackupRestoreConfirmation.this.mAllowButton.setEnabled(BackupRestoreConfirmation.this.mEncPassword.getText().length() > 0);
            }
        });
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return this.mObserver;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putBoolean("did_acknowledge", this.mDidAcknowledge);
        bundle.putInt("token", this.mToken);
        bundle.putString("action", this.mAction);
    }

    void sendAcknowledgement(int i, boolean z, IFullBackupRestoreObserver iFullBackupRestoreObserver) {
        if (!this.mDidAcknowledge) {
            this.mDidAcknowledge = true;
            try {
                this.mBackupManager.acknowledgeFullBackupOrRestore(this.mToken, z, String.valueOf(this.mCurPassword.getText()), String.valueOf(this.mEncPassword.getText()), this.mObserver);
            } catch (RemoteException e) {
            }
        }
    }

    boolean deviceIsEncrypted() {
        try {
            if (this.mStorageManager.getEncryptionState() != 1) {
                if (this.mStorageManager.getPasswordType() != 1) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Slog.e("BackupRestoreConfirmation", "Unable to communicate with storagemanager service: " + e.getMessage());
            return true;
        }
    }

    boolean haveBackupPassword() {
        try {
            return this.mBackupManager.hasBackupPassword();
        } catch (RemoteException e) {
            return true;
        }
    }

    class FullObserver extends IFullBackupRestoreObserver.Stub {
        private Handler mHandler;

        public FullObserver(Handler handler) {
            this.mHandler = handler;
        }

        public void setHandler(Handler handler) {
            this.mHandler = handler;
        }

        public void onStartBackup() throws RemoteException {
            this.mHandler.sendEmptyMessage(1);
        }

        public void onBackupPackage(String str) throws RemoteException {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(2, str));
        }

        public void onEndBackup() throws RemoteException {
            this.mHandler.sendEmptyMessage(3);
        }

        public void onStartRestore() throws RemoteException {
            this.mHandler.sendEmptyMessage(11);
        }

        public void onRestorePackage(String str) throws RemoteException {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(12, str));
        }

        public void onEndRestore() throws RemoteException {
            this.mHandler.sendEmptyMessage(13);
        }

        public void onTimeout() throws RemoteException {
            this.mHandler.sendEmptyMessage(100);
        }
    }
}
