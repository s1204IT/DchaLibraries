package com.android.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
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
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.security.ConfigureKeyGuardDialog;
import com.android.settings.vpn2.VpnUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;

public final class CredentialStorage extends Activity {
    private static AlertDialog sResetDialog = null;
    private static AlertDialog sUnlockDialog = null;
    private Bundle mInstallBundle;
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private int mRetriesRemaining = -1;
    private boolean mIsMarkKeyAsUserSelectable = false;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        sResetDialog = null;
        sUnlockDialog = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String action = intent.getAction();
        if (!((UserManager) getSystemService("user")).hasUserRestriction("no_config_credentials")) {
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
        if ("com.android.credentials.UNLOCK".equals(action) && this.mKeyStore.state() == KeyStore.State.UNINITIALIZED) {
            ensureKeyGuard();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (sResetDialog != null) {
            sResetDialog.dismiss();
            sResetDialog = null;
        }
        if (sUnlockDialog != null) {
            sUnlockDialog.dismiss();
            sUnlockDialog = null;
        }
        super.onDestroy();
    }

    private void handleUnlockOrInstall() {
        if (isFinishing()) {
        }
        switch (AnonymousClass1.$SwitchMap$android$security$KeyStore$State[this.mKeyStore.state().ordinal()]) {
            case 1:
                ensureKeyGuard();
                break;
            case 2:
                new UnlockDialog(this, null);
                break;
            case 3:
                if (!checkKeyGuardQuality()) {
                    new ConfigureKeyGuardDialog().show(getFragmentManager(), "ConfigureKeyGuardDialog");
                } else {
                    installIfAvailable();
                    if (!this.mIsMarkKeyAsUserSelectable) {
                        Log.e("CredentialStorage", "handleUnlockOrInstall, mIsMarkKeyAsUserSelectable");
                        finish();
                    }
                }
                break;
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

    private void ensureKeyGuard() {
        if (!checkKeyGuardQuality()) {
            new ConfigureKeyGuardDialog().show(getFragmentManager(), "ConfigureKeyGuardDialog");
        } else {
            if (confirmKeyGuard(1)) {
                return;
            }
            finish();
        }
    }

    private boolean checkKeyGuardQuality() {
        return new LockPatternUtils(this).getActivePasswordQuality(UserManager.get(this).getCredentialOwnerProfile(UserHandle.myUserId())) >= 65536;
    }

    private boolean isHardwareBackedKey(byte[] bArr) {
        try {
            return KeyChain.isBoundKeyAlgorithm(new AlgorithmId(new ObjectIdentifier(PrivateKeyInfo.getInstance(new ASN1InputStream(new ByteArrayInputStream(bArr)).readObject()).getAlgorithmId().getAlgorithm().getId())).getName());
        } catch (IOException e) {
            Log.e("CredentialStorage", "Failed to parse key data");
            return false;
        }
    }

    private void installIfAvailable() {
        int i;
        if (this.mInstallBundle == null || this.mInstallBundle.isEmpty()) {
            return;
        }
        Bundle bundle = this.mInstallBundle;
        this.mInstallBundle = null;
        int i2 = bundle.getInt("install_as_uid", -1);
        if (i2 != -1 && !UserHandle.isSameUser(i2, Process.myUid())) {
            int userId = UserHandle.getUserId(i2);
            UserHandle.myUserId();
            if (i2 != 1010) {
                Log.e("CredentialStorage", "Failed to install credentials as uid " + i2 + ": cross-user installs may only target wifi uids");
                return;
            }
            startActivityAsUser(new Intent("com.android.credentials.INSTALL").setFlags(33554432).putExtras(bundle), new UserHandle(userId));
            return;
        }
        if (bundle.containsKey("user_private_key_name")) {
            String string = bundle.getString("user_private_key_name");
            byte[] byteArray = bundle.getByteArray("user_private_key_data");
            if (i2 == 1010 && isHardwareBackedKey(byteArray)) {
                Log.d("CredentialStorage", "Saving private key with FLAG_NONE for WIFI_UID");
                i = 0;
            } else {
                i = 1;
            }
            if (!this.mKeyStore.importKey(string, byteArray, i2, i)) {
                Log.e("CredentialStorage", "Failed to install " + string + " as uid " + i2);
                return;
            }
            if (i2 == 1000 || i2 == -1) {
                Log.e("CredentialStorage", "installIfAvailable, MarkKeyAsUserSelectable");
                new MarkKeyAsUserSelectable(string.replaceFirst("^USRPKEY_", "")).execute(new Void[0]);
                this.mIsMarkKeyAsUserSelectable = true;
            }
        }
        if (bundle.containsKey("user_certificate_name")) {
            String string2 = bundle.getString("user_certificate_name");
            if (!this.mKeyStore.put(string2, bundle.getByteArray("user_certificate_data"), i2, 0)) {
                Log.e("CredentialStorage", "Failed to install " + string2 + " as uid " + i2);
                return;
            }
        }
        if (bundle.containsKey("ca_certificates_name")) {
            String string3 = bundle.getString("ca_certificates_name");
            if (!this.mKeyStore.put(string3, bundle.getByteArray("ca_certificates_data"), i2, 0)) {
                Log.e("CredentialStorage", "Failed to install " + string3 + " as uid " + i2);
                return;
            }
        }
        if (bundle.containsKey("wapi_user_certificate_name")) {
            String string4 = bundle.getString("wapi_user_certificate_name");
            byte[] byteArray2 = bundle.getByteArray("wapi_user_certificate_data");
            if (string4 != null && !this.mKeyStore.put(string4, byteArray2, i2, 1)) {
                Log.d("CredentialStorage", "Failed to install " + string4 + " as user " + i2);
                return;
            }
        }
        if (bundle.containsKey("wapi_server_certificate_name")) {
            String string5 = bundle.getString("wapi_server_certificate_name");
            byte[] byteArray3 = bundle.getByteArray("wapi_server_certificate_data");
            if (string5 != null && !this.mKeyStore.put(string5, byteArray3, i2, 1)) {
                Log.d("CredentialStorage", "Failed to install " + string5 + " as user " + i2);
                return;
            }
        }
        sendBroadcast(new Intent("android.security.action.KEYCHAIN_CHANGED"));
        setResult(-1);
    }

    private class ResetDialog implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
        private boolean mResetConfirmed;

        ResetDialog(CredentialStorage credentialStorage, AnonymousClass1 anonymousClass1) {
            this();
        }

        private ResetDialog() {
            if (CredentialStorage.sResetDialog == null) {
                AlertDialog alertDialogCreate = new AlertDialog.Builder(CredentialStorage.this).setTitle(android.R.string.dialog_alert_title).setMessage(R.string.credentials_reset_hint).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
                AlertDialog unused = CredentialStorage.sResetDialog = alertDialogCreate;
                alertDialogCreate.setOnDismissListener(this);
                alertDialogCreate.show();
            }
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            this.mResetConfirmed = i == -1;
        }

        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            AlertDialog unused = CredentialStorage.sResetDialog = null;
            if (this.mResetConfirmed) {
                this.mResetConfirmed = false;
                if (CredentialStorage.this.confirmKeyGuard(2)) {
                    return;
                }
            }
            CredentialStorage.this.finish();
        }
    }

    private class ResetKeyStoreAndKeyChain extends AsyncTask<Void, Void, Boolean> {
        private ResetKeyStoreAndKeyChain() {
        }

        ResetKeyStoreAndKeyChain(CredentialStorage credentialStorage, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        protected Boolean doInBackground(Void... voidArr) {
            new LockPatternUtils(CredentialStorage.this).resetKeyStore(UserHandle.myUserId());
            try {
                KeyChain.KeyChainConnection keyChainConnectionBind = KeyChain.bind(CredentialStorage.this);
                try {
                    try {
                        return Boolean.valueOf(keyChainConnectionBind.getService().reset());
                    } finally {
                        keyChainConnectionBind.close();
                    }
                } catch (RemoteException e) {
                    return false;
                }
            } catch (InterruptedException e2) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (bool.booleanValue()) {
                Toast.makeText(CredentialStorage.this, R.string.credentials_erased, 0).show();
                CredentialStorage.this.clearLegacyVpnIfEstablished();
            } else {
                Toast.makeText(CredentialStorage.this, R.string.credentials_not_erased, 0).show();
            }
            CredentialStorage.this.finish();
        }
    }

    private void clearLegacyVpnIfEstablished() {
        if (VpnUtils.disconnectLegacyVpn(getApplicationContext())) {
            Toast.makeText(this, R.string.vpn_disconnected, 0).show();
        }
    }

    private class MarkKeyAsUserSelectable extends AsyncTask<Void, Void, Boolean> {
        final String mAlias;

        public MarkKeyAsUserSelectable(String str) {
            this.mAlias = str;
        }

        @Override
        protected Boolean doInBackground(Void... voidArr) {
            try {
                KeyChain.KeyChainConnection keyChainConnectionBind = KeyChain.bind(CredentialStorage.this);
                Throwable th = null;
                try {
                    keyChainConnectionBind.getService().setUserSelectable(this.mAlias, true);
                    if (keyChainConnectionBind != null) {
                        keyChainConnectionBind.close();
                    }
                    return true;
                } finally {
                }
            } catch (RemoteException e) {
                Log.w("CredentialStorage", "Failed to mark key " + this.mAlias + " as user-selectable.");
                return false;
            } catch (InterruptedException e2) {
                Log.w("CredentialStorage", "Failed to mark key " + this.mAlias + " as user-selectable.");
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            Log.e("CredentialStorage", "onPostExecute ");
            CredentialStorage.this.mIsMarkKeyAsUserSelectable = false;
            CredentialStorage.this.finish();
        }
    }

    private boolean checkCallerIsCertInstallerOrSelfInProfile() {
        if (TextUtils.equals("com.android.certinstaller", getCallingPackage())) {
            return getPackageManager().checkSignatures(getCallingPackage(), getPackageName()) == 0;
        }
        try {
            int launchedFromUid = ActivityManager.getService().getLaunchedFromUid(getActivityToken());
            if (launchedFromUid == -1) {
                Log.e("CredentialStorage", "com.android.credentials.INSTALL must be started with startActivityForResult");
                return false;
            }
            if (!UserHandle.isSameApp(launchedFromUid, Process.myUid())) {
                return false;
            }
            UserInfo profileParent = ((UserManager) getSystemService("user")).getProfileParent(UserHandle.getUserId(launchedFromUid));
            return profileParent != null && profileParent.id == UserHandle.myUserId();
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean confirmKeyGuard(int i) {
        return new ChooseLockSettingsHelper(this).launchConfirmationActivity(i, getResources().getText(R.string.credentials_title), true);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 1) {
            if (i2 == -1) {
                String stringExtra = intent.getStringExtra("password");
                if (!TextUtils.isEmpty(stringExtra)) {
                    this.mKeyStore.unlock(stringExtra);
                    return;
                }
            }
            finish();
            return;
        }
        if (i == 2) {
            if (i2 == -1) {
                new ResetKeyStoreAndKeyChain(this, null).execute(new Void[0]);
            } else {
                finish();
            }
        }
    }

    private class UnlockDialog implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener, TextWatcher {
        private final Button mButton;
        private final TextView mError;
        private final TextView mOldPassword;
        private boolean mUnlockConfirmed;

        UnlockDialog(CredentialStorage credentialStorage, AnonymousClass1 anonymousClass1) {
            this();
        }

        private UnlockDialog() {
            CharSequence string;
            View viewInflate = View.inflate(CredentialStorage.this, R.layout.credentials_dialog, null);
            if (CredentialStorage.this.mRetriesRemaining != -1) {
                if (CredentialStorage.this.mRetriesRemaining <= 3) {
                    if (CredentialStorage.this.mRetriesRemaining == 1) {
                        string = CredentialStorage.this.getResources().getText(R.string.credentials_reset_warning);
                    } else {
                        string = CredentialStorage.this.getString(R.string.credentials_reset_warning_plural, new Object[]{Integer.valueOf(CredentialStorage.this.mRetriesRemaining)});
                    }
                } else {
                    string = CredentialStorage.this.getResources().getText(R.string.credentials_wrong_password);
                }
            } else {
                string = CredentialStorage.this.getResources().getText(R.string.credentials_unlock_hint);
            }
            ((TextView) viewInflate.findViewById(R.id.hint)).setText(string);
            this.mOldPassword = (TextView) viewInflate.findViewById(R.id.old_password);
            this.mOldPassword.setVisibility(0);
            this.mOldPassword.addTextChangedListener(this);
            this.mError = (TextView) viewInflate.findViewById(R.id.error);
            if (CredentialStorage.sUnlockDialog == null) {
                AlertDialog alertDialogCreate = new AlertDialog.Builder(CredentialStorage.this).setView(viewInflate).setTitle(R.string.credentials_unlock).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
                AlertDialog unused = CredentialStorage.sUnlockDialog = alertDialogCreate;
                alertDialogCreate.setOnDismissListener(this);
                alertDialogCreate.show();
            }
            this.mButton = CredentialStorage.sUnlockDialog.getButton(-1);
            this.mButton.setEnabled(false);
        }

        @Override
        public void afterTextChanged(Editable editable) {
            this.mButton.setEnabled(this.mOldPassword == null || this.mOldPassword.getText().length() > 0);
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            this.mUnlockConfirmed = i == -1;
        }

        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            AlertDialog unused = CredentialStorage.sUnlockDialog = null;
            if (this.mUnlockConfirmed) {
                this.mUnlockConfirmed = false;
                this.mError.setVisibility(0);
                CredentialStorage.this.mKeyStore.unlock(this.mOldPassword.getText().toString());
                int lastError = CredentialStorage.this.mKeyStore.getLastError();
                if (lastError == 1) {
                    CredentialStorage.this.mRetriesRemaining = -1;
                    Toast.makeText(CredentialStorage.this, R.string.credentials_enabled, 0).show();
                    CredentialStorage.this.ensureKeyGuard();
                    return;
                } else if (lastError == 3) {
                    CredentialStorage.this.mRetriesRemaining = -1;
                    Toast.makeText(CredentialStorage.this, R.string.credentials_erased, 0).show();
                    CredentialStorage.this.handleUnlockOrInstall();
                    return;
                } else {
                    if (lastError >= 10) {
                        CredentialStorage.this.mRetriesRemaining = (lastError - 10) + 1;
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
