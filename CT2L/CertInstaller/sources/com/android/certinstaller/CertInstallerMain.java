package com.android.certinstaller;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import libcore.io.IoUtils;
import libcore.io.Streams;

public class CertInstallerMain extends PreferenceActivity {
    private static final String[] ACCEPT_MIME_TYPES = {"application/x-pkcs12", "application/x-x509-ca-cert", "application/x-x509-user-cert", "application/x-x509-server-cert", "application/x-pem-file", "application/pkix-cert"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(0);
        UserManager userManager = (UserManager) getSystemService("user");
        if (userManager.hasUserRestriction("no_config_credentials")) {
            finish();
            return;
        }
        Intent intent = getIntent();
        String action = intent.getAction();
        if ("android.credentials.INSTALL".equals(action) || "android.credentials.INSTALL_AS_USER".equals(action)) {
            Bundle bundle = intent.getExtras();
            String calledClass = intent.getComponent().getClassName();
            String installAsUserClassName = getPackageName() + ".InstallCertAsUser";
            if (bundle != null && !installAsUserClassName.equals(calledClass)) {
                bundle.remove("install_as_uid");
            }
            if (bundle == null || bundle.isEmpty() || (bundle.size() == 1 && (bundle.containsKey("name") || bundle.containsKey("install_as_uid")))) {
                Intent openIntent = new Intent("android.intent.action.OPEN_DOCUMENT");
                openIntent.setType("*/*");
                openIntent.putExtra("android.intent.extra.MIME_TYPES", ACCEPT_MIME_TYPES);
                openIntent.putExtra("android.content.extra.SHOW_ADVANCED", true);
                startActivityForResult(openIntent, 2);
                return;
            }
            Intent installIntent = new Intent(this, (Class<?>) CertInstaller.class);
            installIntent.putExtras(intent);
            startActivityForResult(installIntent, 1);
            return;
        }
        if ("android.intent.action.VIEW".equals(action)) {
            startInstallActivity(intent.getType(), intent.getData());
        }
    }

    private void startInstallActivity(String mimeType, Uri uri) {
        if (mimeType == null) {
            mimeType = getContentResolver().getType(uri);
        }
        InputStream in = null;
        try {
            in = getContentResolver().openInputStream(uri);
            byte[] raw = Streams.readFully(in);
            startInstallActivity(mimeType, raw);
        } catch (IOException e) {
            Log.e("CertInstaller", "Failed to read certificate: " + e);
            Toast.makeText(this, R.string.cert_read_error, 1).show();
        } finally {
            IoUtils.closeQuietly(in);
        }
    }

    private void startInstallActivity(String mimeType, byte[] value) {
        Intent intent = new Intent(this, (Class<?>) CertInstaller.class);
        if ("application/x-pkcs12".equals(mimeType)) {
            intent.putExtra("PKCS12", value);
        } else if ("application/x-x509-ca-cert".equals(mimeType) || "application/x-x509-user-cert".equals(mimeType) || "application/x-x509-server-cert".equals(mimeType) || "application/x-pem-file".equals(mimeType) || "application/pkix-cert".equals(mimeType)) {
            intent.putExtra("CERT", value);
        } else {
            throw new IllegalArgumentException("Unknown MIME type: " + mimeType);
        }
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2) {
            if (resultCode == -1) {
                startInstallActivity((String) null, data.getData());
                return;
            } else {
                finish();
                return;
            }
        }
        if (requestCode == 1) {
            setResult(resultCode);
            finish();
        } else {
            Log.w("CertInstaller", "unknown request code: " + requestCode);
        }
    }
}
