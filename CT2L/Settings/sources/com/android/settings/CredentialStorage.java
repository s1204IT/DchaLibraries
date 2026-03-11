package com.android.settings;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.KeyChain;
import android.security.KeyStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.widget.LockPatternUtils;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.harmony.security.utils.AlgNameMapper;

public final class CredentialStorage extends Activity {
    private Bundle mInstallBundle;
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private int mRetriesRemaining = -1;

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String action = intent.getAction();
        UserManager userManager = (UserManager) getSystemService("user");
        if (!userManager.hasUserRestriction("no_config_credentials")) {
            if ("com.android.credentials.RESET".equals(action)) {
                new ResetDialog(this, null);
                return;
            }
            if ("com.android.credentials.INSTALL".equals(action) && checkCallerIsCertInstallerOrSelfInProfile()) {
                this.mInstallBundle = intent.getExtras();
            }
            handleUnlockOrInstall();
            return;
        }
        finish();
    }

    public void handleUnlockOrInstall() {
        AnonymousClass1 anonymousClass1 = null;
        if (!isFinishing()) {
            switch (AnonymousClass1.$SwitchMap$android$security$KeyStore$State[this.mKeyStore.state().ordinal()]) {
                case 1:
                    ensureKeyGuard();
                    break;
                case 2:
                    new UnlockDialog(this, anonymousClass1);
                    break;
                case 3:
                    if (!checkKeyGuardQuality()) {
                        new ConfigureKeyGuardDialog(this, anonymousClass1);
                    } else {
                        installIfAvailable();
                        finish();
                    }
                    break;
            }
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$security$KeyStore$State = new int[KeyStore.State.values().length];

        static {
            try {
                $SwitchMap$android$security$KeyStore$State[KeyStore.State.UNINITIALIZED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$security$KeyStore$State[KeyStore.State.LOCKED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$security$KeyStore$State[KeyStore.State.UNLOCKED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public void ensureKeyGuard() {
        if (!checkKeyGuardQuality()) {
            new ConfigureKeyGuardDialog(this, null);
        } else if (!confirmKeyGuard()) {
            finish();
        }
    }

    private boolean checkKeyGuardQuality() {
        int quality = new LockPatternUtils(this).getActivePasswordQuality();
        return quality >= 65536;
    }

    private boolean isHardwareBackedKey(byte[] keyData) {
        try {
            ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(keyData));
            PrivateKeyInfo pki = PrivateKeyInfo.getInstance(bIn.readObject());
            String algId = pki.getAlgorithmId().getAlgorithm().getId();
            String algName = AlgNameMapper.map2AlgName(algId);
            return KeyChain.isBoundKeyAlgorithm(algName);
        } catch (IOException e) {
            Log.e("CredentialStorage", "Failed to parse key data");
            return false;
        }
    }

    private void installIfAvailable() {
        if (this.mInstallBundle != null && !this.mInstallBundle.isEmpty()) {
            Bundle bundle = this.mInstallBundle;
            this.mInstallBundle = null;
            int uid = bundle.getInt("install_as_uid", -1);
            if (uid != -1 && !UserHandle.isSameUser(uid, Process.myUid())) {
                int dstUserId = UserHandle.getUserId(uid);
                UserHandle.myUserId();
                if (uid != 1010) {
                    Log.e("CredentialStorage", "Failed to install credentials as uid " + uid + ": cross-user installs may only target wifi uids");
                    return;
                } else {
                    Intent installIntent = new Intent("com.android.credentials.INSTALL").setFlags(33554432).putExtras(bundle);
                    startActivityAsUser(installIntent, new UserHandle(dstUserId));
                    return;
                }
            }
            if (bundle.containsKey("user_private_key_name")) {
                String key = bundle.getString("user_private_key_name");
                byte[] value = bundle.getByteArray("user_private_key_data");
                int flags = 1;
                if (uid == 1010 && isHardwareBackedKey(value)) {
                    Log.d("CredentialStorage", "Saving private key with FLAG_NONE for WIFI_UID");
                    flags = 0;
                }
                if (!this.mKeyStore.importKey(key, value, uid, flags)) {
                    Log.e("CredentialStorage", "Failed to install " + key + " as uid " + uid);
                    return;
                }
            }
            int flags2 = uid == 1010 ? 0 : 1;
            if (bundle.containsKey("user_certificate_name")) {
                String certName = bundle.getString("user_certificate_name");
                byte[] certData = bundle.getByteArray("user_certificate_data");
                if (!this.mKeyStore.put(certName, certData, uid, flags2)) {
                    Log.e("CredentialStorage", "Failed to install " + certName + " as uid " + uid);
                    return;
                }
            }
            if (bundle.containsKey("ca_certificates_name")) {
                String caListName = bundle.getString("ca_certificates_name");
                byte[] caListData = bundle.getByteArray("ca_certificates_data");
                if (!this.mKeyStore.put(caListName, caListData, uid, flags2)) {
                    Log.e("CredentialStorage", "Failed to install " + caListName + " as uid " + uid);
                    return;
                }
            }
            setResult(-1);
        }
    }

    private class ResetDialog implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
        private boolean mResetConfirmed;

        ResetDialog(CredentialStorage x0, AnonymousClass1 x1) {
            this();
        }

        private ResetDialog() {
            AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this).setTitle(android.R.string.dialog_alert_title).setMessage(R.string.credentials_reset_hint).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
            dialog.setOnDismissListener(this);
            dialog.show();
        }

        @Override
        public void onClick(DialogInterface dialog, int button) {
            this.mResetConfirmed = button == -1;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (this.mResetConfirmed) {
                this.mResetConfirmed = false;
                new ResetKeyStoreAndKeyChain(CredentialStorage.this, null).execute(new Void[0]);
            } else {
                CredentialStorage.this.finish();
            }
        }
    }

    private class ResetKeyStoreAndKeyChain extends AsyncTask<Void, Void, Boolean> {
        private ResetKeyStoreAndKeyChain() {
        }

        ResetKeyStoreAndKeyChain(CredentialStorage x0, AnonymousClass1 x1) {
            this();
        }

        @Override
        public Boolean doInBackground(Void... unused) {
            CredentialStorage.this.mKeyStore.reset();
            try {
                KeyChain.KeyChainConnection keyChainConnection = KeyChain.bind(CredentialStorage.this);
                try {
                    return Boolean.valueOf(keyChainConnection.getService().reset());
                } catch (RemoteException e) {
                    return false;
                } finally {
                    keyChainConnection.close();
                }
            } catch (InterruptedException e2) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        public void onPostExecute(Boolean success) {
            if (success.booleanValue()) {
                Toast.makeText(CredentialStorage.this, R.string.credentials_erased, 0).show();
            } else {
                Toast.makeText(CredentialStorage.this, R.string.credentials_not_erased, 0).show();
            }
            CredentialStorage.this.finish();
        }
    }

    private class ConfigureKeyGuardDialog implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
        private boolean mConfigureConfirmed;

        ConfigureKeyGuardDialog(CredentialStorage x0, AnonymousClass1 x1) {
            this();
        }

        private ConfigureKeyGuardDialog() {
            AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this).setTitle(android.R.string.dialog_alert_title).setMessage(R.string.credentials_configure_lock_screen_hint).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
            dialog.setOnDismissListener(this);
            dialog.show();
        }

        @Override
        public void onClick(DialogInterface dialog, int button) {
            this.mConfigureConfirmed = button == -1;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (this.mConfigureConfirmed) {
                this.mConfigureConfirmed = false;
                Intent intent = new Intent("android.app.action.SET_NEW_PASSWORD");
                intent.putExtra("minimum_quality", 65536);
                CredentialStorage.this.startActivity(intent);
                return;
            }
            CredentialStorage.this.finish();
        }
    }

    private boolean checkCallerIsCertInstallerOrSelfInProfile() {
        boolean z = true;
        if (TextUtils.equals("com.android.certinstaller", getCallingPackage())) {
            return true;
        }
        try {
            int launchedFromUid = ActivityManagerNative.getDefault().getLaunchedFromUid(getActivityToken());
            if (launchedFromUid == -1) {
                Log.e("CredentialStorage", "com.android.credentials.INSTALL must be started with startActivityForResult");
                z = false;
            } else if (UserHandle.isSameApp(launchedFromUid, Process.myUid())) {
                int launchedFromUserId = UserHandle.getUserId(launchedFromUid);
                UserManager userManager = (UserManager) getSystemService("user");
                UserInfo parentInfo = userManager.getProfileParent(launchedFromUserId);
                if (parentInfo == null || parentInfo.id != UserHandle.myUserId()) {
                    z = false;
                }
            } else {
                z = false;
            }
            return z;
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean confirmKeyGuard() {
        Resources res = getResources();
        boolean launched = new ChooseLockSettingsHelper(this).launchConfirmationActivity(1, null, res.getText(R.string.credentials_install_gesture_explanation), true);
        return launched;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == -1) {
                String password = data.getStringExtra("password");
                if (!TextUtils.isEmpty(password)) {
                    this.mKeyStore.password(password);
                    return;
                }
            }
            finish();
        }
    }

    private class UnlockDialog implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener, TextWatcher {
        private final Button mButton;
        private final TextView mError;
        private final TextView mOldPassword;
        private boolean mUnlockConfirmed;

        UnlockDialog(CredentialStorage x0, AnonymousClass1 x1) {
            this();
        }

        private UnlockDialog() {
            CharSequence text;
            View view = View.inflate(CredentialStorage.this, R.layout.credentials_dialog, null);
            if (CredentialStorage.this.mRetriesRemaining != -1) {
                if (CredentialStorage.this.mRetriesRemaining <= 3) {
                    if (CredentialStorage.this.mRetriesRemaining == 1) {
                        text = CredentialStorage.this.getResources().getText(R.string.credentials_reset_warning);
                    } else {
                        text = CredentialStorage.this.getString(R.string.credentials_reset_warning_plural, new Object[]{Integer.valueOf(CredentialStorage.this.mRetriesRemaining)});
                    }
                } else {
                    text = CredentialStorage.this.getResources().getText(R.string.credentials_wrong_password);
                }
            } else {
                text = CredentialStorage.this.getResources().getText(R.string.credentials_unlock_hint);
            }
            ((TextView) view.findViewById(R.id.hint)).setText(text);
            this.mOldPassword = (TextView) view.findViewById(R.id.old_password);
            this.mOldPassword.setVisibility(0);
            this.mOldPassword.addTextChangedListener(this);
            this.mError = (TextView) view.findViewById(R.id.error);
            AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this).setView(view).setTitle(R.string.credentials_unlock).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
            dialog.setOnDismissListener(this);
            dialog.show();
            this.mButton = dialog.getButton(-1);
            this.mButton.setEnabled(false);
        }

        @Override
        public void afterTextChanged(Editable editable) {
            this.mButton.setEnabled(this.mOldPassword == null || this.mOldPassword.getText().length() > 0);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void onClick(DialogInterface dialog, int button) {
            this.mUnlockConfirmed = button == -1;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (this.mUnlockConfirmed) {
                this.mUnlockConfirmed = false;
                this.mError.setVisibility(0);
                CredentialStorage.this.mKeyStore.unlock(this.mOldPassword.getText().toString());
                int error = CredentialStorage.this.mKeyStore.getLastError();
                if (error == 1) {
                    CredentialStorage.this.mRetriesRemaining = -1;
                    Toast.makeText(CredentialStorage.this, R.string.credentials_enabled, 0).show();
                    CredentialStorage.this.ensureKeyGuard();
                    return;
                } else if (error == 3) {
                    CredentialStorage.this.mRetriesRemaining = -1;
                    Toast.makeText(CredentialStorage.this, R.string.credentials_erased, 0).show();
                    CredentialStorage.this.handleUnlockOrInstall();
                    return;
                } else {
                    if (error >= 10) {
                        CredentialStorage.this.mRetriesRemaining = (error - 10) + 1;
                        CredentialStorage.this.handleUnlockOrInstall();
                        return;
                    }
                    return;
                }
            }
            CredentialStorage.this.finish();
        }
    }
}
