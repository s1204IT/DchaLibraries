package com.android.certinstaller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.security.Credentials;
import android.security.KeyChain;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;

public class CertInstaller extends Activity {
    private CredentialHelper mCredentials;
    private MyAction mNextAction;
    private int mState;
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private final ViewHelper mView = new ViewHelper();

    private interface MyAction extends Serializable {
        void run(CertInstaller certInstaller);
    }

    private CredentialHelper createCredentialHelper(Intent intent) {
        try {
            return new CredentialHelper(intent);
        } catch (Throwable th) {
            Log.w("CertInstaller", "createCredentialHelper", th);
            toastErrorAndFinish(R.string.invalid_cert);
            return new CredentialHelper();
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addPrivateFlags(524288);
        this.mCredentials = createCredentialHelper(getIntent());
        this.mState = bundle == null ? 1 : 2;
        if (this.mState == 1) {
            if (!this.mCredentials.containsAnyRawData()) {
                toastErrorAndFinish(R.string.no_cert_to_saved);
                finish();
                return;
            } else {
                if (this.mCredentials.hasCaCerts()) {
                    Intent intentCreateConfirmDeviceCredentialIntent = ((KeyguardManager) getSystemService(KeyguardManager.class)).createConfirmDeviceCredentialIntent(null, null);
                    if (intentCreateConfirmDeviceCredentialIntent == null) {
                        onScreenlockOk();
                        return;
                    } else {
                        startActivityForResult(intentCreateConfirmDeviceCredentialIntent, 2);
                        return;
                    }
                }
                onScreenlockOk();
                return;
            }
        }
        this.mCredentials.onRestoreStates(bundle);
        this.mNextAction = (MyAction) bundle.getSerializable("na");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mState == 1) {
            this.mState = 2;
        } else if (this.mNextAction != null) {
            this.mNextAction.run(this);
        }
    }

    private boolean needsKeyStoreAccess() {
        return (this.mCredentials.hasKeyPair() || this.mCredentials.hasUserCertificate()) && !this.mKeyStore.isUnlocked();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mState = 3;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mCredentials.onSaveStates(bundle);
        if (this.mNextAction != null) {
            bundle.putSerializable("na", this.mNextAction);
        }
    }

    @Override
    protected Dialog onCreateDialog(int i) {
        switch (i) {
            case 1:
                return createNameCredentialDialog();
            case 2:
                return createPkcs12PasswordDialog();
            case 3:
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage(getString(R.string.extracting_pkcs12));
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                return progressDialog;
            default:
                return null;
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i == 1) {
            if (i2 == -1) {
                Log.d("CertInstaller", "credential is added: " + this.mCredentials.getName());
                Toast.makeText(this, getString(R.string.cert_is_added, this.mCredentials.getName()), 1).show();
                if (this.mCredentials.includesVpnAndAppsTrustAnchors()) {
                    new InstallVpnAndAppsTrustAnchorsTask().execute(new Void[0]);
                    return;
                }
                setResult(-1);
            } else {
                Log.d("CertInstaller", "credential not saved, err: " + i2);
                toastErrorAndFinish(R.string.cert_not_saved);
            }
        } else if (i == 2) {
            if (i2 == -1) {
                onScreenlockOk();
                return;
            }
        } else {
            Log.w("CertInstaller", "unknown request code: " + i);
        }
        finish();
    }

    private void onScreenlockOk() {
        if (this.mCredentials.hasPkcs12KeyStore()) {
            if (this.mCredentials.hasPassword()) {
                showDialog(2);
                return;
            } else {
                new Pkcs12ExtractAction("").run(this);
                return;
            }
        }
        InstallOthersAction installOthersAction = new InstallOthersAction();
        if (needsKeyStoreAccess()) {
            sendUnlockKeyStoreIntent();
            this.mNextAction = installOthersAction;
        } else {
            installOthersAction.run(this);
        }
    }

    private class InstallVpnAndAppsTrustAnchorsTask extends AsyncTask<Void, Void, Boolean> {
        private InstallVpnAndAppsTrustAnchorsTask() {
        }

        @Override
        protected Boolean doInBackground(Void... voidArr) {
            try {
                KeyChain.KeyChainConnection keyChainConnectionBind = KeyChain.bind(CertInstaller.this);
                try {
                    return Boolean.valueOf(CertInstaller.this.mCredentials.installVpnAndAppsTrustAnchors(CertInstaller.this, keyChainConnectionBind.getService()));
                } finally {
                    keyChainConnectionBind.close();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (bool.booleanValue()) {
                CertInstaller.this.setResult(-1);
            }
            CertInstaller.this.finish();
        }
    }

    void installOthers() {
        if (this.mCredentials.hasKeyPair()) {
            saveKeyPair();
            finish();
            return;
        }
        X509Certificate userCertificate = this.mCredentials.getUserCertificate();
        if (userCertificate != null) {
            String md5 = Util.toMd5(userCertificate.getPublicKey().getEncoded());
            Map<String, byte[]> pkeyMap = getPkeyMap();
            byte[] bArr = pkeyMap.get(md5);
            if (bArr != null) {
                Log.d("CertInstaller", "found matched key: " + bArr);
                pkeyMap.remove(md5);
                savePkeyMap(pkeyMap);
                this.mCredentials.setPrivateKey(bArr);
            } else {
                Log.d("CertInstaller", "didn't find matched private key: " + md5);
            }
        }
        nameCredential();
    }

    private void sendUnlockKeyStoreIntent() {
        Credentials.getInstance().unlock(this);
    }

    private void nameCredential() {
        if (!this.mCredentials.hasAnyForSystemInstall()) {
            toastErrorAndFinish(R.string.no_cert_to_saved);
        } else {
            showDialog(1);
        }
    }

    private void saveKeyPair() {
        byte[] data = this.mCredentials.getData("PKEY");
        String md5 = Util.toMd5(this.mCredentials.getData("KEY"));
        Map<String, byte[]> pkeyMap = getPkeyMap();
        pkeyMap.put(md5, data);
        savePkeyMap(pkeyMap);
        Log.d("CertInstaller", "save privatekey: " + md5 + " --> #keys:" + pkeyMap.size());
    }

    private void savePkeyMap(Map<String, byte[]> map) {
        if (map.isEmpty()) {
            if (!this.mKeyStore.delete("PKEY_MAP")) {
                Log.w("CertInstaller", "savePkeyMap(): failed to delete pkey map");
            }
        } else {
            if (!this.mKeyStore.put("PKEY_MAP", Util.toBytes(map), -1, 1)) {
                Log.w("CertInstaller", "savePkeyMap(): failed to write pkey map");
            }
        }
    }

    private Map<String, byte[]> getPkeyMap() {
        Map<String, byte[]> map;
        byte[] bArr = this.mKeyStore.get("PKEY_MAP");
        return (bArr == null || (map = (Map) Util.fromBytes(bArr)) == null) ? new MyMap() : map;
    }

    void extractPkcs12InBackground(final String str) {
        showDialog(3);
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voidArr) {
                return Boolean.valueOf(CertInstaller.this.mCredentials.extractPkcs12(str));
            }

            @Override
            protected void onPostExecute(Boolean bool) {
                OnExtractionDoneAction onExtractionDoneAction = new OnExtractionDoneAction(bool.booleanValue());
                if (CertInstaller.this.mState == 3) {
                    CertInstaller.this.mNextAction = onExtractionDoneAction;
                } else {
                    onExtractionDoneAction.run(CertInstaller.this);
                }
            }
        }.execute(new Void[0]);
    }

    void onExtractionDone(boolean z) {
        this.mNextAction = null;
        removeDialog(3);
        if (z) {
            removeDialog(2);
            nameCredential();
        } else {
            showDialog(2);
            this.mView.setText(R.id.credential_password, "");
            this.mView.showError(R.string.password_error);
        }
    }

    private Dialog createPkcs12PasswordDialog() {
        String string;
        View viewInflate = View.inflate(this, R.layout.password_dialog, null);
        this.mView.setView(viewInflate);
        if (this.mView.getHasEmptyError()) {
            this.mView.showError(R.string.password_empty_error);
            this.mView.setHasEmptyError(false);
        }
        String name = this.mCredentials.getName();
        if (TextUtils.isEmpty(name)) {
            string = getString(R.string.pkcs12_password_dialog_title);
        } else {
            string = getString(R.string.pkcs12_file_password_dialog_title, name);
        }
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setView(viewInflate).setTitle(string).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String text = CertInstaller.this.mView.getText(R.id.credential_password);
                CertInstaller.this.mNextAction = new Pkcs12ExtractAction(text);
                CertInstaller.this.mNextAction.run(CertInstaller.this);
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                CertInstaller.this.toastErrorAndFinish(R.string.cert_not_saved);
            }
        }).create();
        alertDialogCreate.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                CertInstaller.this.toastErrorAndFinish(R.string.cert_not_saved);
            }
        });
        return alertDialogCreate;
    }

    private Dialog createNameCredentialDialog() {
        ViewGroup viewGroup = (ViewGroup) View.inflate(this, R.layout.name_credential_dialog, null);
        this.mView.setView(viewGroup);
        if (this.mView.getHasEmptyError()) {
            this.mView.showError(R.string.name_empty_error);
            this.mView.setHasEmptyError(false);
        }
        this.mView.setText(R.id.credential_info, this.mCredentials.getDescription(this).toString());
        EditText editText = (EditText) viewGroup.findViewById(R.id.credential_name);
        if (this.mCredentials.isInstallAsUidSet()) {
            viewGroup.findViewById(R.id.credential_usage_group).setVisibility(8);
        } else {
            Spinner spinner = (Spinner) viewGroup.findViewById(R.id.credential_usage);
            final View viewFindViewById = viewGroup.findViewById(R.id.credential_capabilities_warning);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                    switch ((int) j) {
                        case 0:
                            viewFindViewById.setVisibility(CertInstaller.this.mCredentials.includesVpnAndAppsTrustAnchors() ? 0 : 8);
                            CertInstaller.this.mCredentials.setInstallAsUid(-1);
                            break;
                        case 1:
                            viewFindViewById.setVisibility(8);
                            CertInstaller.this.mCredentials.setInstallAsUid(1010);
                            break;
                        default:
                            Log.w("CertInstaller", "Unknown selection for scope: " + j);
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
        }
        editText.setText(getDefaultName());
        editText.selectAll();
        final Context applicationContext = getApplicationContext();
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this).setView(viewGroup).setTitle(R.string.name_credential_dialog_title).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String text = CertInstaller.this.mView.getText(R.id.credential_name);
                if (TextUtils.isEmpty(text)) {
                    CertInstaller.this.mView.setHasEmptyError(true);
                    CertInstaller.this.removeDialog(1);
                    CertInstaller.this.showDialog(1);
                    return;
                }
                CertInstaller.this.removeDialog(1);
                CertInstaller.this.mCredentials.setName(text);
                if (BenesseExtension.getDchaState() != 0) {
                    return;
                }
                try {
                    CertInstaller.this.startActivityForResult(CertInstaller.this.mCredentials.createSystemInstallIntent(applicationContext), 1);
                } catch (ActivityNotFoundException e) {
                    Log.w("CertInstaller", "systemInstall(): " + e);
                    CertInstaller.this.toastErrorAndFinish(R.string.cert_not_saved);
                }
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                CertInstaller.this.toastErrorAndFinish(R.string.cert_not_saved);
            }
        }).create();
        alertDialogCreate.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                CertInstaller.this.toastErrorAndFinish(R.string.cert_not_saved);
            }
        });
        return alertDialogCreate;
    }

    private String getDefaultName() {
        String name = this.mCredentials.getName();
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        int iLastIndexOf = name.lastIndexOf(".");
        return iLastIndexOf > 0 ? name.substring(0, iLastIndexOf) : name;
    }

    private void toastErrorAndFinish(int i) {
        Toast.makeText(this, i, 0).show();
        finish();
    }

    private static class MyMap extends LinkedHashMap<String, byte[]> implements Serializable {
        private static final long serialVersionUID = 1;

        private MyMap() {
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> entry) {
            return size() > 3;
        }
    }

    private static class Pkcs12ExtractAction implements MyAction {
        private transient boolean hasRun;
        private final String mPassword;

        Pkcs12ExtractAction(String str) {
            this.mPassword = str;
        }

        @Override
        public void run(CertInstaller certInstaller) {
            if (this.hasRun) {
                return;
            }
            this.hasRun = true;
            certInstaller.extractPkcs12InBackground(this.mPassword);
        }
    }

    private static class InstallOthersAction implements MyAction {
        private InstallOthersAction() {
        }

        @Override
        public void run(CertInstaller certInstaller) {
            certInstaller.mNextAction = null;
            certInstaller.installOthers();
        }
    }

    private static class OnExtractionDoneAction implements MyAction {
        private final boolean mSuccess;

        OnExtractionDoneAction(boolean z) {
            this.mSuccess = z;
        }

        @Override
        public void run(CertInstaller certInstaller) {
            certInstaller.onExtractionDone(this.mSuccess);
        }
    }
}
