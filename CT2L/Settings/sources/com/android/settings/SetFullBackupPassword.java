package com.android.settings;

import android.app.Activity;
import android.app.backup.IBackupManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SetFullBackupPassword extends Activity {
    IBackupManager mBackupManager;
    View.OnClickListener mButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == SetFullBackupPassword.this.mSet) {
                String curPw = SetFullBackupPassword.this.mCurrentPw.getText().toString();
                String newPw = SetFullBackupPassword.this.mNewPw.getText().toString();
                String confirmPw = SetFullBackupPassword.this.mConfirmNewPw.getText().toString();
                if (newPw.equals(confirmPw)) {
                    if (SetFullBackupPassword.this.setBackupPassword(curPw, newPw)) {
                        Log.i("SetFullBackupPassword", "password set successfully");
                        Toast.makeText(SetFullBackupPassword.this, R.string.local_backup_password_toast_success, 1).show();
                        SetFullBackupPassword.this.finish();
                        return;
                    } else {
                        Log.i("SetFullBackupPassword", "failure; password mismatch?");
                        Toast.makeText(SetFullBackupPassword.this, R.string.local_backup_password_toast_validation_failure, 1).show();
                        return;
                    }
                }
                Log.i("SetFullBackupPassword", "password mismatch");
                Toast.makeText(SetFullBackupPassword.this, R.string.local_backup_password_toast_confirmation_mismatch, 1).show();
                return;
            }
            if (v == SetFullBackupPassword.this.mCancel) {
                SetFullBackupPassword.this.finish();
            } else {
                Log.w("SetFullBackupPassword", "Click on unknown view");
            }
        }
    };
    Button mCancel;
    TextView mConfirmNewPw;
    TextView mCurrentPw;
    TextView mNewPw;
    Button mSet;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        setContentView(R.layout.set_backup_pw);
        this.mCurrentPw = (TextView) findViewById(R.id.current_backup_pw);
        this.mNewPw = (TextView) findViewById(R.id.new_backup_pw);
        this.mConfirmNewPw = (TextView) findViewById(R.id.confirm_new_backup_pw);
        this.mCancel = (Button) findViewById(R.id.backup_pw_cancel_button);
        this.mSet = (Button) findViewById(R.id.backup_pw_set_button);
        this.mCancel.setOnClickListener(this.mButtonListener);
        this.mSet.setOnClickListener(this.mButtonListener);
    }

    public boolean setBackupPassword(String currentPw, String newPw) {
        try {
            return this.mBackupManager.setBackupPassword(currentPw, newPw);
        } catch (RemoteException e) {
            Log.e("SetFullBackupPassword", "Unable to communicate with backup manager");
            return false;
        }
    }
}
