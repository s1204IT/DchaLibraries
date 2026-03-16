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
import android.os.storage.IMountService;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Slog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BackupRestoreConfirmation extends Activity {
    Button mAllowButton;
    IBackupManager mBackupManager;
    TextView mCurPassword;
    Button mDenyButton;
    boolean mDidAcknowledge;
    TextView mEncPassword;
    Handler mHandler;
    boolean mIsEncrypted;
    IMountService mMountService;
    FullObserver mObserver;
    TextView mStatusView;
    int mToken;

    class ObserverHandler extends Handler {
        Context mContext;

        ObserverHandler(Context context) {
            this.mContext = context;
            BackupRestoreConfirmation.this.mDidAcknowledge = false;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Toast.makeText(this.mContext, R.string.toast_backup_started, 1).show();
                    break;
                case 2:
                    String name = (String) msg.obj;
                    BackupRestoreConfirmation.this.mStatusView.setText(name);
                    break;
                case 3:
                    Toast.makeText(this.mContext, R.string.toast_backup_ended, 1).show();
                    BackupRestoreConfirmation.this.finish();
                    break;
                case 11:
                    Toast.makeText(this.mContext, R.string.toast_restore_started, 1).show();
                    break;
                case 12:
                    String name2 = (String) msg.obj;
                    BackupRestoreConfirmation.this.mStatusView.setText(name2);
                    break;
                case 13:
                    Toast.makeText(this.mContext, R.string.toast_restore_ended, 0).show();
                    BackupRestoreConfirmation.this.finish();
                    break;
                case 100:
                    Toast.makeText(this.mContext, R.string.toast_timeout, 1).show();
                    break;
            }
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        int layoutId;
        int titleId;
        super.onCreate(icicle);
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.equals("fullback")) {
            layoutId = R.layout.confirm_backup;
            titleId = R.string.backup_confirm_title;
        } else if (action.equals("fullrest")) {
            layoutId = R.layout.confirm_restore;
            titleId = R.string.restore_confirm_title;
        } else {
            Slog.w("BackupRestoreConfirmation", "Backup/restore confirmation activity launched with invalid action!");
            finish();
            return;
        }
        this.mToken = intent.getIntExtra("conftoken", -1);
        if (this.mToken < 0) {
            Slog.e("BackupRestoreConfirmation", "Backup/restore confirmation requested but no token passed!");
            finish();
            return;
        }
        this.mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        this.mMountService = IMountService.Stub.asInterface(ServiceManager.getService("mount"));
        this.mHandler = new ObserverHandler(getApplicationContext());
        Object oldObserver = getLastNonConfigurationInstance();
        if (oldObserver == null) {
            this.mObserver = new FullObserver(this.mHandler);
        } else {
            this.mObserver = (FullObserver) oldObserver;
            this.mObserver.setHandler(this.mHandler);
        }
        setTitle(titleId);
        setContentView(layoutId);
        this.mStatusView = (TextView) findViewById(R.id.package_name);
        this.mAllowButton = (Button) findViewById(R.id.button_allow);
        this.mDenyButton = (Button) findViewById(R.id.button_deny);
        this.mCurPassword = (TextView) findViewById(R.id.password);
        this.mEncPassword = (TextView) findViewById(R.id.enc_password);
        TextView curPwDesc = (TextView) findViewById(R.id.password_desc);
        this.mAllowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BackupRestoreConfirmation.this.sendAcknowledgement(BackupRestoreConfirmation.this.mToken, true, BackupRestoreConfirmation.this.mObserver);
                BackupRestoreConfirmation.this.mAllowButton.setEnabled(false);
                BackupRestoreConfirmation.this.mDenyButton.setEnabled(false);
            }
        });
        this.mDenyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BackupRestoreConfirmation.this.sendAcknowledgement(BackupRestoreConfirmation.this.mToken, false, BackupRestoreConfirmation.this.mObserver);
                BackupRestoreConfirmation.this.mAllowButton.setEnabled(false);
                BackupRestoreConfirmation.this.mDenyButton.setEnabled(false);
                BackupRestoreConfirmation.this.finish();
            }
        });
        if (icicle != null) {
            this.mDidAcknowledge = icicle.getBoolean("did_acknowledge", false);
            this.mAllowButton.setEnabled(!this.mDidAcknowledge);
            this.mDenyButton.setEnabled(this.mDidAcknowledge ? false : true);
        }
        this.mIsEncrypted = deviceIsEncrypted();
        if (!haveBackupPassword()) {
            curPwDesc.setVisibility(8);
            this.mCurPassword.setVisibility(8);
            if (layoutId == R.layout.confirm_backup) {
                TextView encPwDesc = (TextView) findViewById(R.id.enc_password_desc);
                if (this.mIsEncrypted) {
                    encPwDesc.setText(R.string.backup_enc_password_required);
                    monitorEncryptionPassword();
                } else {
                    encPwDesc.setText(R.string.backup_enc_password_optional);
                }
            }
        }
    }

    private void monitorEncryptionPassword() {
        this.mAllowButton.setEnabled(false);
        this.mEncPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                BackupRestoreConfirmation.this.mAllowButton.setEnabled(BackupRestoreConfirmation.this.mEncPassword.getText().length() > 0);
            }
        });
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return this.mObserver;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("did_acknowledge", this.mDidAcknowledge);
    }

    void sendAcknowledgement(int token, boolean allow, IFullBackupRestoreObserver observer) {
        if (!this.mDidAcknowledge) {
            this.mDidAcknowledge = true;
            try {
                CharSequence encPassword = this.mEncPassword.getText();
                this.mBackupManager.acknowledgeFullBackupOrRestore(this.mToken, allow, String.valueOf(this.mCurPassword.getText()), String.valueOf(encPassword), this.mObserver);
            } catch (RemoteException e) {
            }
        }
    }

    boolean deviceIsEncrypted() {
        try {
            if (this.mMountService.getEncryptionState() != 1) {
                if (this.mMountService.getPasswordType() != 1) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Slog.e("BackupRestoreConfirmation", "Unable to communicate with mount service: " + e.getMessage());
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

        public FullObserver(Handler h) {
            this.mHandler = h;
        }

        public void setHandler(Handler h) {
            this.mHandler = h;
        }

        public void onStartBackup() throws RemoteException {
            this.mHandler.sendEmptyMessage(1);
        }

        public void onBackupPackage(String name) throws RemoteException {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(2, name));
        }

        public void onEndBackup() throws RemoteException {
            this.mHandler.sendEmptyMessage(3);
        }

        public void onStartRestore() throws RemoteException {
            this.mHandler.sendEmptyMessage(11);
        }

        public void onRestorePackage(String name) throws RemoteException {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(12, name));
        }

        public void onEndRestore() throws RemoteException {
            this.mHandler.sendEmptyMessage(13);
        }

        public void onTimeout() throws RemoteException {
            this.mHandler.sendEmptyMessage(100);
        }
    }
}
