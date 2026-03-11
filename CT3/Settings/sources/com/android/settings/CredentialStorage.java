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
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;

public final class CredentialStorage extends Activity {

    private static final int[] f4androidsecurityKeyStore$StateSwitchesValues = null;
    private static AlertDialog sConfigureKeyGuardDialog = null;
    private static AlertDialog sResetDialog = null;
    private static AlertDialog sUnlockDialog = null;
    private Bundle mInstallBundle;
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private int mRetriesRemaining = -1;

    private static int[] m310getandroidsecurityKeyStore$StateSwitchesValues() {
        if (f4androidsecurityKeyStore$StateSwitchesValues != null) {
            return f4androidsecurityKeyStore$StateSwitchesValues;
        }
        int[] iArr = new int[KeyStore.State.values().length];
        try {
            iArr[KeyStore.State.LOCKED.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[KeyStore.State.UNINITIALIZED.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[KeyStore.State.UNLOCKED.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f4androidsecurityKeyStore$StateSwitchesValues = iArr;
        return iArr;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sConfigureKeyGuardDialog = null;
        sResetDialog = null;
        sUnlockDialog = null;
    }

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
        if ("com.android.credentials.UNLOCK".equals(action) && this.mKeyStore.state() == KeyStore.State.UNINITIALIZED) {
            ensureKeyGuard();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (sConfigureKeyGuardDialog != null) {
            sConfigureKeyGuardDialog = null;
        }
        if (sResetDialog != null) {
            sResetDialog = null;
        }
        if (sUnlockDialog != null) {
            sUnlockDialog = null;
        }
        super.onDestroy();
    }

    public void handleUnlockOrInstall() {
        UnlockDialog unlockDialog = null;
        Object[] objArr = 0;
        if (isFinishing()) {
        }
        switch (m310getandroidsecurityKeyStore$StateSwitchesValues()[this.mKeyStore.state().ordinal()]) {
            case DefaultWfcSettingsExt.PAUSE:
                new UnlockDialog(this, unlockDialog);
                break;
            case DefaultWfcSettingsExt.CREATE:
                ensureKeyGuard();
                break;
            case DefaultWfcSettingsExt.DESTROY:
                if (!checkKeyGuardQuality()) {
                    new ConfigureKeyGuardDialog(this, objArr == true ? 1 : 0);
                } else {
                    installIfAvailable();
                    finish();
                }
                break;
        }
    }

    public void ensureKeyGuard() {
        if (!checkKeyGuardQuality()) {
            new ConfigureKeyGuardDialog(this, null);
        } else {
            if (confirmKeyGuard(1)) {
                return;
            }
            finish();
        }
    }

    private boolean checkKeyGuardQuality() {
        int credentialOwner = UserManager.get(this).getCredentialOwnerProfile(UserHandle.myUserId());
        int quality = new LockPatternUtils(this).getActivePasswordQuality(credentialOwner);
        return quality >= 65536;
    }

    private boolean isHardwareBackedKey(byte[] keyData) {
        try {
            ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(keyData));
            PrivateKeyInfo pki = PrivateKeyInfo.getInstance(bIn.readObject());
            String algOid = pki.getAlgorithmId().getAlgorithm().getId();
            String algName = new AlgorithmId(new ObjectIdentifier(algOid)).getName();
            return KeyChain.isBoundKeyAlgorithm(algName);
        } catch (IOException e) {
            Log.e("CredentialStorage", "Failed to parse key data");
            return false;
        }
    }

    private void installIfAvailable() {
        if (this.mInstallBundle == null || this.mInstallBundle.isEmpty()) {
            return;
        }
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
        if (bundle.containsKey("wapi_user_certificate_name")) {
            String caListName2 = bundle.getString("wapi_user_certificate_name");
            byte[] caListData2 = bundle.getByteArray("wapi_user_certificate_data");
            if (caListName2 != null && !this.mKeyStore.put(caListName2, caListData2, uid, 1)) {
                Log.d("CredentialStorage", "Failed to install " + caListName2 + " as user " + uid);
                return;
            }
        }
        if (bundle.containsKey("wapi_server_certificate_name")) {
            String caListName3 = bundle.getString("wapi_server_certificate_name");
            byte[] caListData3 = bundle.getByteArray("wapi_server_certificate_data");
            if (caListName3 != null && !this.mKeyStore.put(caListName3, caListData3, uid, 1)) {
                Log.d("CredentialStorage", "Failed to install " + caListName3 + " as user " + uid);
                return;
            }
        }
        setResult(-1);
    }

    private class ResetDialog implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
        private boolean mResetConfirmed;

        ResetDialog(CredentialStorage this$0, ResetDialog resetDialog) {
            this();
        }

        private ResetDialog() {
            if (CredentialStorage.sResetDialog == null) {
                AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this).setTitle(android.R.string.dialog_alert_title).setMessage(R.string.credentials_reset_hint).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
                AlertDialog unused = CredentialStorage.sResetDialog = dialog;
                dialog.setOnDismissListener(this);
                dialog.show();
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int button) {
            this.mResetConfirmed = button == -1;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
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
        ResetKeyStoreAndKeyChain(CredentialStorage this$0, ResetKeyStoreAndKeyChain resetKeyStoreAndKeyChain) {
            this();
        }

        private ResetKeyStoreAndKeyChain() {
        }

        @Override
        public Boolean doInBackground(Void... unused) {
            new LockPatternUtils(CredentialStorage.this).resetKeyStore(UserHandle.myUserId());
            try {
                KeyChain.KeyChainConnection keyChainConnection = KeyChain.bind(CredentialStorage.this);
                try {
                    try {
                        return Boolean.valueOf(keyChainConnection.getService().reset());
                    } catch (RemoteException e) {
                        return false;
                    }
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

        ConfigureKeyGuardDialog(CredentialStorage this$0, ConfigureKeyGuardDialog configureKeyGuardDialog) {
            this();
        }

        private ConfigureKeyGuardDialog() {
            if (CredentialStorage.sConfigureKeyGuardDialog == null) {
                AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this).setTitle(android.R.string.dialog_alert_title).setMessage(R.string.credentials_configure_lock_screen_hint).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
                AlertDialog unused = CredentialStorage.sConfigureKeyGuardDialog = dialog;
                dialog.setOnDismissListener(this);
                dialog.show();
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int button) {
            this.mConfigureConfirmed = button == -1;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            AlertDialog unused = CredentialStorage.sConfigureKeyGuardDialog = null;
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
        if (TextUtils.equals("com.android.certinstaller", getCallingPackage())) {
            return getPackageManager().checkSignatures(getCallingPackage(), getPackageName()) == 0;
        }
        try {
            int launchedFromUid = ActivityManagerNative.getDefault().getLaunchedFromUid(getActivityToken());
            if (launchedFromUid == -1) {
                Log.e("CredentialStorage", "com.android.credentials.INSTALL must be started with startActivityForResult");
                return false;
            }
            if (!UserHandle.isSameApp(launchedFromUid, Process.myUid())) {
                return false;
            }
            int launchedFromUserId = UserHandle.getUserId(launchedFromUid);
            UserManager userManager = (UserManager) getSystemService("user");
            UserInfo parentInfo = userManager.getProfileParent(launchedFromUserId);
            return parentInfo != null && parentInfo.id == UserHandle.myUserId();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean confirmKeyGuard(int requestCode) {
        Resources res = getResources();
        boolean launched = new ChooseLockSettingsHelper(this).launchConfirmationActivity(requestCode, res.getText(R.string.credentials_title), true);
        return launched;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == -1) {
                String password = data.getStringExtra("password");
                if (!TextUtils.isEmpty(password)) {
                    this.mKeyStore.unlock(password);
                    return;
                }
            }
            finish();
            return;
        }
        if (requestCode != 2) {
            return;
        }
        if (resultCode == -1) {
            new ResetKeyStoreAndKeyChain(this, null).execute(new Void[0]);
        } else {
            finish();
        }
    }

    private class UnlockDialog implements TextWatcher, DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
        private final Button mButton;
        private final TextView mError;
        private final TextView mOldPassword;
        private boolean mUnlockConfirmed;

        UnlockDialog(CredentialStorage this$0, UnlockDialog unlockDialog) {
            this();
        }

        private UnlockDialog() {
            View view = View.inflate(CredentialStorage.this, R.layout.credentials_dialog, null);
            CharSequence text = CredentialStorage.this.mRetriesRemaining == -1 ? CredentialStorage.this.getResources().getText(R.string.credentials_unlock_hint) : CredentialStorage.this.mRetriesRemaining > 3 ? CredentialStorage.this.getResources().getText(R.string.credentials_wrong_password) : CredentialStorage.this.mRetriesRemaining == 1 ? CredentialStorage.this.getResources().getText(R.string.credentials_reset_warning) : CredentialStorage.this.getString(R.string.credentials_reset_warning_plural, new Object[]{Integer.valueOf(CredentialStorage.this.mRetriesRemaining)});
            ((TextView) view.findViewById(R.id.hint)).setText(text);
            this.mOldPassword = (TextView) view.findViewById(R.id.old_password);
            this.mOldPassword.setVisibility(0);
            this.mOldPassword.addTextChangedListener(this);
            this.mError = (TextView) view.findViewById(R.id.error);
            if (CredentialStorage.sUnlockDialog == null) {
                AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this).setView(view).setTitle(R.string.credentials_unlock).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
                AlertDialog unused = CredentialStorage.sUnlockDialog = dialog;
                dialog.setOnDismissListener(this);
                dialog.show();
            }
            this.mButton = CredentialStorage.sUnlockDialog.getButton(-1);
            this.mButton.setEnabled(false);
        }

        @Override
        public void afterTextChanged(Editable editable) {
            boolean z = true;
            Button button = this.mButton;
            if (this.mOldPassword != null && this.mOldPassword.getText().length() <= 0) {
                z = false;
            }
            button.setEnabled(z);
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
            AlertDialog unused = CredentialStorage.sUnlockDialog = null;
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
