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
/* loaded from: classes.dex */
public class SetFullBackupPassword extends Activity {
    IBackupManager mBackupManager;
    View.OnClickListener mButtonListener = new View.OnClickListener() { // from class: com.android.settings.SetFullBackupPassword.1
        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            if (view != SetFullBackupPassword.this.mSet) {
                if (view == SetFullBackupPassword.this.mCancel) {
                    SetFullBackupPassword.this.finish();
                    return;
                } else {
                    Log.w("SetFullBackupPassword", "Click on unknown view");
                    return;
                }
            }
            String charSequence = SetFullBackupPassword.this.mCurrentPw.getText().toString();
            String charSequence2 = SetFullBackupPassword.this.mNewPw.getText().toString();
            if (charSequence2.equals(SetFullBackupPassword.this.mConfirmNewPw.getText().toString())) {
                if (SetFullBackupPassword.this.setBackupPassword(charSequence, charSequence2)) {
                    Log.i("SetFullBackupPassword", "password set successfully");
                    Toast.makeText(SetFullBackupPassword.this, (int) R.string.local_backup_password_toast_success, 1).show();
                    SetFullBackupPassword.this.finish();
                    return;
                }
                Log.i("SetFullBackupPassword", "failure; password mismatch?");
                Toast.makeText(SetFullBackupPassword.this, (int) R.string.local_backup_password_toast_validation_failure, 1).show();
                return;
            }
            Log.i("SetFullBackupPassword", "password mismatch");
            Toast.makeText(SetFullBackupPassword.this, (int) R.string.local_backup_password_toast_confirmation_mismatch, 1).show();
        }
    };
    Button mCancel;
    TextView mConfirmNewPw;
    TextView mCurrentPw;
    TextView mNewPw;
    Button mSet;

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
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

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setBackupPassword(String str, String str2) {
        try {
            return this.mBackupManager.setBackupPassword(str, str2);
        } catch (RemoteException e) {
            Log.e("SetFullBackupPassword", "Unable to communicate with backup manager");
            return false;
        }
    }
}
