package com.android.certinstaller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
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
        } catch (Throwable t) {
            Log.w("CertInstaller", "createCredentialHelper", t);
            toastErrorAndFinish(R.string.invalid_cert);
            return new CredentialHelper();
        }
    }

    @Override
    protected void onCreate(Bundle savedStates) {
        super.onCreate(savedStates);
        this.mCredentials = createCredentialHelper(getIntent());
        this.mState = savedStates == null ? 1 : 2;
        if (this.mState == 1) {
            if (!this.mCredentials.containsAnyRawData()) {
                toastErrorAndFinish(R.string.no_cert_to_saved);
                finish();
                return;
            } else {
                if (this.mCredentials.hasPkcs12KeyStore()) {
                    showDialog(2);
                    return;
                }
                MyAction action = new InstallOthersAction();
                if (needsKeyStoreAccess()) {
                    sendUnlockKeyStoreIntent();
                    this.mNextAction = action;
                    return;
                } else {
                    action.run(this);
                    return;
                }
            }
        }
        this.mCredentials.onRestoreStates(savedStates);
        this.mNextAction = (MyAction) savedStates.getSerializable("na");
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
    protected void onSaveInstanceState(Bundle outStates) {
        super.onSaveInstanceState(outStates);
        this.mCredentials.onSaveStates(outStates);
        if (this.mNextAction != null) {
            outStates.putSerializable("na", this.mNextAction);
        }
    }

    @Override
    protected Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case 1:
                return createNameCredentialDialog();
            case 2:
                return createPkcs12PasswordDialog();
            case 3:
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setMessage(getString(R.string.extracting_pkcs12));
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                return dialog;
            default:
                return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == -1) {
                Log.d("CertInstaller", "credential is added: " + this.mCredentials.getName());
                Toast.makeText(this, getString(R.string.cert_is_added, new Object[]{this.mCredentials.getName()}), 1).show();
                if (this.mCredentials.hasCaCerts() && this.mCredentials.getInstallAsUid() == -1) {
                    new InstallCaCertsToKeyChainTask().execute(new Void[0]);
                    return;
                }
                setResult(-1);
            } else {
                Log.d("CertInstaller", "credential not saved, err: " + resultCode);
                toastErrorAndFinish(R.string.cert_not_saved);
            }
        } else {
            Log.w("CertInstaller", "unknown request code: " + requestCode);
        }
        finish();
    }

    private class InstallCaCertsToKeyChainTask extends AsyncTask<Void, Void, Boolean> {
        private InstallCaCertsToKeyChainTask() {
        }

        @Override
        protected Boolean doInBackground(Void... unused) {
            try {
                KeyChain.KeyChainConnection keyChainConnection = KeyChain.bind(CertInstaller.this);
                try {
                    return Boolean.valueOf(CertInstaller.this.mCredentials.installCaCertsToKeyChain(keyChainConnection.getService()));
                } finally {
                    keyChainConnection.close();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success.booleanValue()) {
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
        X509Certificate cert = this.mCredentials.getUserCertificate();
        if (cert != null) {
            String key = Util.toMd5(cert.getPublicKey().getEncoded());
            Map<String, byte[]> map = getPkeyMap();
            byte[] privatekey = map.get(key);
            if (privatekey != null) {
                Log.d("CertInstaller", "found matched key: " + privatekey);
                map.remove(key);
                savePkeyMap(map);
                this.mCredentials.setPrivateKey(privatekey);
            } else {
                Log.d("CertInstaller", "didn't find matched private key: " + key);
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
        byte[] privatekey = this.mCredentials.getData("PKEY");
        String key = Util.toMd5(this.mCredentials.getData("KEY"));
        Map<String, byte[]> map = getPkeyMap();
        map.put(key, privatekey);
        savePkeyMap(map);
        Log.d("CertInstaller", "save privatekey: " + key + " --> #keys:" + map.size());
    }

    private void savePkeyMap(Map<String, byte[]> map) {
        if (map.isEmpty()) {
            if (!this.mKeyStore.delete("PKEY_MAP")) {
                Log.w("CertInstaller", "savePkeyMap(): failed to delete pkey map");
            }
        } else {
            byte[] bytes = Util.toBytes(map);
            if (!this.mKeyStore.put("PKEY_MAP", bytes, -1, 1)) {
                Log.w("CertInstaller", "savePkeyMap(): failed to write pkey map");
            }
        }
    }

    private Map<String, byte[]> getPkeyMap() {
        Map<String, byte[]> map;
        byte[] bytes = this.mKeyStore.get("PKEY_MAP");
        return (bytes == null || (map = (Map) Util.fromBytes(bytes)) == null) ? new MyMap() : map;
    }

    void extractPkcs12InBackground(final String password) {
        showDialog(3);
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... unused) {
                return Boolean.valueOf(CertInstaller.this.mCredentials.extractPkcs12(password));
            }

            @Override
            protected void onPostExecute(Boolean success) {
                MyAction action = new OnExtractionDoneAction(success.booleanValue());
                if (CertInstaller.this.mState == 3) {
                    CertInstaller.this.mNextAction = action;
                } else {
                    action.run(CertInstaller.this);
                }
            }
        }.execute(new Void[0]);
    }

    void onExtractionDone(boolean success) {
        this.mNextAction = null;
        removeDialog(3);
        if (success) {
            removeDialog(2);
            nameCredential();
        } else {
            this.mView.setText(R.id.credential_password, "");
            this.mView.showError(R.string.password_error);
            showDialog(2);
        }
    }

    private Dialog createPkcs12PasswordDialog() {
        View view = View.inflate(this, R.layout.password_dialog, null);
        this.mView.setView(view);
        if (this.mView.getHasEmptyError()) {
            this.mView.showError(R.string.password_empty_error);
            this.mView.setHasEmptyError(false);
        }
        String title = this.mCredentials.getName();
        Dialog d = new AlertDialog.Builder(this).setView(view).setTitle(TextUtils.isEmpty(title) ? getString(R.string.pkcs12_password_dialog_title) : getString(R.string.pkcs12_file_password_dialog_title, new Object[]{title})).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                String password = CertInstaller.this.mView.getText(R.id.credential_password);
                CertInstaller.this.mNextAction = new Pkcs12ExtractAction(password);
                CertInstaller.this.mNextAction.run(CertInstaller.this);
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                CertInstaller.this.toastErrorAndFinish(R.string.cert_not_saved);
            }
        }).create();
        d.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                CertInstaller.this.toastErrorAndFinish(R.string.cert_not_saved);
            }
        });
        return d;
    }

    private Dialog createNameCredentialDialog() {
        ViewGroup view = (ViewGroup) View.inflate(this, R.layout.name_credential_dialog, null);
        this.mView.setView(view);
        if (this.mView.getHasEmptyError()) {
            this.mView.showError(R.string.name_empty_error);
            this.mView.setHasEmptyError(false);
        }
        this.mView.setText(R.id.credential_info, this.mCredentials.getDescription(this).toString());
        EditText nameInput = (EditText) view.findViewById(R.id.credential_name);
        if (this.mCredentials.isInstallAsUidSet()) {
            view.findViewById(R.id.credential_usage_group).setVisibility(8);
        } else {
            Spinner usageSpinner = (Spinner) view.findViewById(R.id.credential_usage);
            usageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view2, int position, long id) {
                    switch ((int) id) {
                        case 0:
                            CertInstaller.this.mCredentials.setInstallAsUid(-1);
                            break;
                        case 1:
                            CertInstaller.this.mCredentials.setInstallAsUid(1010);
                            break;
                        default:
                            Log.w("CertInstaller", "Unknown selection for scope: " + id);
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        nameInput.setText(getDefaultName());
        nameInput.selectAll();
        Dialog d = new AlertDialog.Builder(this).setView(view).setTitle(R.string.name_credential_dialog_title).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                String name = CertInstaller.this.mView.getText(R.id.credential_name);
                if (TextUtils.isEmpty(name)) {
                    CertInstaller.this.mView.setHasEmptyError(true);
                    CertInstaller.this.removeDialog(1);
                    CertInstaller.this.showDialog(1);
                    return;
                }
                CertInstaller.this.removeDialog(1);
                CertInstaller.this.mCredentials.setName(name);
                if (BenesseExtension.getDchaState() == 0) {
                    try {
                        CertInstaller.this.startActivityForResult(CertInstaller.this.mCredentials.createSystemInstallIntent(), 1);
                    } catch (ActivityNotFoundException e) {
                        Log.w("CertInstaller", "systemInstall(): " + e);
                        CertInstaller.this.toastErrorAndFinish(R.string.cert_not_saved);
                    }
                }
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                CertInstaller.this.toastErrorAndFinish(R.string.cert_not_saved);
            }
        }).create();
        d.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                CertInstaller.this.toastErrorAndFinish(R.string.cert_not_saved);
            }
        });
        return d;
    }

    private String getDefaultName() {
        String name = this.mCredentials.getName();
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        int index = name.lastIndexOf(".");
        if (index > 0) {
            name = name.substring(0, index);
        }
        return name;
    }

    private void toastErrorAndFinish(int msgId) {
        Toast.makeText(this, msgId, 0).show();
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

        Pkcs12ExtractAction(String password) {
            this.mPassword = password;
        }

        @Override
        public void run(CertInstaller host) {
            if (!this.hasRun) {
                this.hasRun = true;
                host.extractPkcs12InBackground(this.mPassword);
            }
        }
    }

    private static class InstallOthersAction implements MyAction {
        private InstallOthersAction() {
        }

        @Override
        public void run(CertInstaller host) {
            host.mNextAction = null;
            host.installOthers();
        }
    }

    private static class OnExtractionDoneAction implements MyAction {
        private final boolean mSuccess;

        OnExtractionDoneAction(boolean success) {
            this.mSuccess = success;
        }

        @Override
        public void run(CertInstaller host) {
            host.onExtractionDone(this.mSuccess);
        }
    }
}
