package com.android.server.updates;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Base64;
import android.util.EventLog;
import android.util.Slog;
import com.android.server.EventLogTags;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import libcore.io.IoUtils;
import libcore.io.Streams;

public class ConfigUpdateInstallReceiver extends BroadcastReceiver {
    private static final String EXTRA_CONTENT_PATH = "CONTENT_PATH";
    private static final String EXTRA_REQUIRED_HASH = "REQUIRED_HASH";
    private static final String EXTRA_SIGNATURE = "SIGNATURE";
    private static final String EXTRA_VERSION_NUMBER = "VERSION";
    private static final String TAG = "ConfigUpdateInstallReceiver";
    private static final String UPDATE_CERTIFICATE_KEY = "config_update_certificate";
    protected final File updateContent;
    protected final File updateDir;
    protected final File updateVersion;

    public ConfigUpdateInstallReceiver(String updateDir, String updateContentPath, String updateMetadataPath, String updateVersionPath) {
        this.updateDir = new File(updateDir);
        this.updateContent = new File(updateDir, updateContentPath);
        File updateMetadataDir = new File(updateDir, updateMetadataPath);
        this.updateVersion = new File(updateMetadataDir, updateVersionPath);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread() {
            @Override
            public void run() throws Throwable {
                try {
                    X509Certificate cert = ConfigUpdateInstallReceiver.this.getCert(context.getContentResolver());
                    byte[] altContent = ConfigUpdateInstallReceiver.this.getAltContent(context, intent);
                    int altVersion = ConfigUpdateInstallReceiver.this.getVersionFromIntent(intent);
                    String altRequiredHash = ConfigUpdateInstallReceiver.this.getRequiredHashFromIntent(intent);
                    String altSig = ConfigUpdateInstallReceiver.this.getSignatureFromIntent(intent);
                    int currentVersion = ConfigUpdateInstallReceiver.this.getCurrentVersion();
                    String currentHash = ConfigUpdateInstallReceiver.getCurrentHash(ConfigUpdateInstallReceiver.this.getCurrentContent());
                    if (ConfigUpdateInstallReceiver.this.verifyVersion(currentVersion, altVersion)) {
                        if (ConfigUpdateInstallReceiver.this.verifyPreviousHash(currentHash, altRequiredHash)) {
                            if (!ConfigUpdateInstallReceiver.this.verifySignature(altContent, altVersion, altRequiredHash, altSig, cert)) {
                                EventLog.writeEvent(EventLogTags.CONFIG_INSTALL_FAILED, "Signature did not verify");
                            } else {
                                Slog.i(ConfigUpdateInstallReceiver.TAG, "Found new update, installing...");
                                ConfigUpdateInstallReceiver.this.install(altContent, altVersion);
                                Slog.i(ConfigUpdateInstallReceiver.TAG, "Installation successful");
                                ConfigUpdateInstallReceiver.this.postInstall(context, intent);
                            }
                        } else {
                            EventLog.writeEvent(EventLogTags.CONFIG_INSTALL_FAILED, "Current hash did not match required value");
                        }
                    } else {
                        Slog.i(ConfigUpdateInstallReceiver.TAG, "Not installing, new version is <= current version");
                    }
                } catch (Exception e) {
                    Slog.e(ConfigUpdateInstallReceiver.TAG, "Could not update content!", e);
                    String errMsg = e.toString();
                    if (errMsg.length() > 100) {
                        errMsg = errMsg.substring(0, 99);
                    }
                    EventLog.writeEvent(EventLogTags.CONFIG_INSTALL_FAILED, errMsg);
                }
            }
        }.start();
    }

    private X509Certificate getCert(ContentResolver cr) {
        String cert = Settings.Secure.getString(cr, UPDATE_CERTIFICATE_KEY);
        try {
            byte[] derCert = Base64.decode(cert.getBytes(), 0);
            InputStream istream = new ByteArrayInputStream(derCert);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(istream);
        } catch (CertificateException e) {
            throw new IllegalStateException("Got malformed certificate from settings, ignoring");
        }
    }

    private Uri getContentFromIntent(Intent i) {
        Uri data = i.getData();
        if (data == null) {
            throw new IllegalStateException("Missing required content path, ignoring.");
        }
        return data;
    }

    private int getVersionFromIntent(Intent i) throws NumberFormatException {
        String extraValue = i.getStringExtra(EXTRA_VERSION_NUMBER);
        if (extraValue == null) {
            throw new IllegalStateException("Missing required version number, ignoring.");
        }
        return Integer.parseInt(extraValue.trim());
    }

    private String getRequiredHashFromIntent(Intent i) {
        String extraValue = i.getStringExtra(EXTRA_REQUIRED_HASH);
        if (extraValue == null) {
            throw new IllegalStateException("Missing required previous hash, ignoring.");
        }
        return extraValue.trim();
    }

    private String getSignatureFromIntent(Intent i) {
        String extraValue = i.getStringExtra(EXTRA_SIGNATURE);
        if (extraValue == null) {
            throw new IllegalStateException("Missing required signature, ignoring.");
        }
        return extraValue.trim();
    }

    private int getCurrentVersion() throws NumberFormatException {
        try {
            String strVersion = IoUtils.readFileAsString(this.updateVersion.getCanonicalPath()).trim();
            return Integer.parseInt(strVersion);
        } catch (IOException e) {
            Slog.i(TAG, "Couldn't find current metadata, assuming first update");
            return 0;
        }
    }

    private byte[] getAltContent(Context c, Intent i) throws IOException {
        Uri content = getContentFromIntent(i);
        InputStream is = c.getContentResolver().openInputStream(content);
        try {
            return Streams.readFullyNoClose(is);
        } finally {
            is.close();
        }
    }

    private byte[] getCurrentContent() {
        try {
            return IoUtils.readFileAsByteArray(this.updateContent.getCanonicalPath());
        } catch (IOException e) {
            Slog.i(TAG, "Failed to read current content, assuming first update!");
            return null;
        }
    }

    private static String getCurrentHash(byte[] content) {
        if (content == null) {
            return "0";
        }
        try {
            MessageDigest dgst = MessageDigest.getInstance("SHA512");
            byte[] fingerprint = dgst.digest(content);
            return IntegralToString.bytesToHexString(fingerprint, false);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private boolean verifyVersion(int current, int alternative) {
        return current < alternative;
    }

    private boolean verifyPreviousHash(String current, String required) {
        if (required.equals("NONE")) {
            return true;
        }
        return current.equals(required);
    }

    private boolean verifySignature(byte[] content, int version, String requiredPrevious, String signature, X509Certificate cert) throws Exception {
        Signature signer = Signature.getInstance("SHA512withRSA");
        signer.initVerify(cert);
        signer.update(content);
        signer.update(Long.toString(version).getBytes());
        signer.update(requiredPrevious.getBytes());
        return signer.verify(Base64.decode(signature.getBytes(), 0));
    }

    protected void writeUpdate(File dir, File file, byte[] content) throws Throwable {
        FileOutputStream out = null;
        File tmp = null;
        try {
            File parent = file.getParentFile();
            parent.mkdirs();
            if (!parent.exists()) {
                throw new IOException("Failed to create directory " + parent.getCanonicalPath());
            }
            tmp = File.createTempFile("journal", "", dir);
            tmp.setReadable(true, false);
            FileOutputStream out2 = new FileOutputStream(tmp);
            try {
                out2.write(content);
                out2.getFD().sync();
                if (!tmp.renameTo(file)) {
                    throw new IOException("Failed to atomically rename " + file.getCanonicalPath());
                }
                if (tmp != null) {
                    tmp.delete();
                }
                IoUtils.closeQuietly(out2);
            } catch (Throwable th) {
                th = th;
                out = out2;
                if (tmp != null) {
                    tmp.delete();
                }
                IoUtils.closeQuietly(out);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    protected void install(byte[] content, int version) throws Throwable {
        writeUpdate(this.updateDir, this.updateContent, content);
        writeUpdate(this.updateDir, this.updateVersion, Long.toString(version).getBytes());
    }

    protected void postInstall(Context context, Intent intent) {
    }
}
