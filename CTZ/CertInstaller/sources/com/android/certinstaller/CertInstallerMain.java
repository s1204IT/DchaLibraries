package com.android.certinstaller;

import android.content.Intent;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import libcore.io.IoUtils;

public class CertInstallerMain extends PreferenceActivity {
    private static Map<String, String> MIME_MAPPINGS = new HashMap();

    static {
        MIME_MAPPINGS.put("application/x-x509-ca-cert", "CERT");
        MIME_MAPPINGS.put("application/x-x509-user-cert", "CERT");
        MIME_MAPPINGS.put("application/x-x509-server-cert", "CERT");
        MIME_MAPPINGS.put("application/x-pem-file", "CERT");
        MIME_MAPPINGS.put("application/pkix-cert", "CERT");
        MIME_MAPPINGS.put("application/x-pkcs12", "PKCS12");
        MIME_MAPPINGS.put("application/x-wifi-config", "wifi-config");
    }

    @Override
    protected void onCreate(Bundle bundle) throws Throwable {
        super.onCreate(bundle);
        setResult(0);
        if (((UserManager) getSystemService("user")).hasUserRestriction("no_config_credentials")) {
            finish();
            return;
        }
        Intent intent = getIntent();
        String action = intent.getAction();
        if ("android.credentials.INSTALL".equals(action) || "android.credentials.INSTALL_AS_USER".equals(action)) {
            Bundle extras = intent.getExtras();
            String className = intent.getComponent().getClassName();
            String str = getPackageName() + ".InstallCertAsUser";
            if (extras != null && !str.equals(className)) {
                extras.remove("install_as_uid");
            }
            if (extras == null || extras.isEmpty() || (extras.size() == 1 && (extras.containsKey("name") || extras.containsKey("install_as_uid")))) {
                String[] strArr = (String[]) MIME_MAPPINGS.keySet().toArray(new String[0]);
                Intent intent2 = new Intent("android.intent.action.OPEN_DOCUMENT");
                intent2.setType("*/*");
                intent2.putExtra("android.intent.extra.MIME_TYPES", strArr);
                intent2.putExtra("android.content.extra.SHOW_ADVANCED", true);
                startActivityForResult(intent2, 2);
                return;
            }
            Intent intent3 = new Intent(this, (Class<?>) CertInstaller.class);
            intent3.putExtras(intent);
            startActivityForResult(intent3, 1);
            return;
        }
        if (!"android.intent.action.VIEW".equals(action) || BenesseExtension.getDchaState() != 0) {
            return;
        }
        startInstallActivity(intent.getType(), intent.getData());
    }

    private static byte[] readWithLimit(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[1024];
        int i = 0;
        do {
            int i2 = inputStream.read(bArr);
            if (i2 != -1) {
                byteArrayOutputStream.write(bArr, 0, i2);
                i += i2;
            } else {
                return byteArrayOutputStream.toByteArray();
            }
        } while (i <= 10485760);
        throw new IOException("Data file exceeded maximum size.");
    }

    private void startInstallActivity(String str, Uri uri) throws Throwable {
        Throwable th;
        InputStream inputStreamOpenInputStream;
        IOException e;
        if (str == null) {
            str = getContentResolver().getType(uri);
        }
        String str2 = MIME_MAPPINGS.get(str);
        if (str2 == null) {
            throw new IllegalArgumentException("Unknown MIME type: " + str);
        }
        if ("wifi-config".equals(str2)) {
            startWifiInstallActivity(str, uri);
            return;
        }
        try {
            try {
                inputStreamOpenInputStream = getContentResolver().openInputStream(uri);
                try {
                    startInstallActivity(str2, readWithLimit(inputStreamOpenInputStream));
                    uri = inputStreamOpenInputStream;
                } catch (IOException e2) {
                    e = e2;
                    Log.e("CertInstaller", "Failed to read certificate: " + e);
                    Toast.makeText(this, R.string.cert_read_error, 1).show();
                    uri = inputStreamOpenInputStream;
                }
            } catch (Throwable th2) {
                th = th2;
                IoUtils.closeQuietly((AutoCloseable) uri);
                throw th;
            }
        } catch (IOException e3) {
            inputStreamOpenInputStream = null;
            e = e3;
        } catch (Throwable th3) {
            uri = 0;
            th = th3;
            IoUtils.closeQuietly((AutoCloseable) uri);
            throw th;
        }
        IoUtils.closeQuietly((AutoCloseable) uri);
    }

    private void startInstallActivity(String str, byte[] bArr) {
        Intent intent = new Intent(this, (Class<?>) CertInstaller.class);
        intent.putExtra(str, bArr);
        startActivityForResult(intent, 1);
    }

    private void startWifiInstallActivity(String str, Uri uri) {
        Intent intent = new Intent(this, (Class<?>) WiFiInstaller.class);
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(getContentResolver().openInputStream(uri));
            try {
                byte[] withLimit = readWithLimit(bufferedInputStream);
                intent.putExtra("wifi-config-file", uri.toString());
                intent.putExtra("wifi-config-data", withLimit);
                intent.putExtra("wifi-config", str);
                startActivityForResult(intent, 1);
                bufferedInputStream.close();
            } finally {
            }
        } catch (IOException e) {
            Log.e("CertInstaller", "Failed to read wifi config: " + e);
            Toast.makeText(this, R.string.cert_read_error, 1).show();
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) throws Throwable {
        if (i == 2) {
            if (i2 == -1) {
                startInstallActivity((String) null, intent.getData());
                return;
            } else {
                finish();
                return;
            }
        }
        if (i == 1) {
            setResult(i2);
            finish();
        } else {
            Log.w("CertInstaller", "unknown request code: " + i);
        }
    }
}
