package com.android.server.updates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.util.HexDump;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupManagerConstants;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import libcore.io.IoUtils;
import libcore.io.Streams;

public class ConfigUpdateInstallReceiver extends BroadcastReceiver {
    private static final String EXTRA_REQUIRED_HASH = "REQUIRED_HASH";
    private static final String EXTRA_VERSION_NUMBER = "VERSION";
    private static final String TAG = "ConfigUpdateInstallReceiver";
    protected final File updateContent;
    protected final File updateDir;
    protected final File updateVersion;

    public ConfigUpdateInstallReceiver(String str, String str2, String str3, String str4) {
        this.updateDir = new File(str);
        this.updateContent = new File(str, str2);
        this.updateVersion = new File(new File(str, str3), str4);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread() {
            @Override
            public void run() throws Throwable {
                try {
                    byte[] altContent = ConfigUpdateInstallReceiver.this.getAltContent(context, intent);
                    int versionFromIntent = ConfigUpdateInstallReceiver.this.getVersionFromIntent(intent);
                    String requiredHashFromIntent = ConfigUpdateInstallReceiver.this.getRequiredHashFromIntent(intent);
                    int currentVersion = ConfigUpdateInstallReceiver.this.getCurrentVersion();
                    String currentHash = ConfigUpdateInstallReceiver.getCurrentHash(ConfigUpdateInstallReceiver.this.getCurrentContent());
                    if (ConfigUpdateInstallReceiver.this.verifyVersion(currentVersion, versionFromIntent)) {
                        if (!ConfigUpdateInstallReceiver.this.verifyPreviousHash(currentHash, requiredHashFromIntent)) {
                            EventLog.writeEvent(EventLogTags.CONFIG_INSTALL_FAILED, "Current hash did not match required value");
                        } else {
                            Slog.i(ConfigUpdateInstallReceiver.TAG, "Found new update, installing...");
                            ConfigUpdateInstallReceiver.this.install(altContent, versionFromIntent);
                            Slog.i(ConfigUpdateInstallReceiver.TAG, "Installation successful");
                            ConfigUpdateInstallReceiver.this.postInstall(context, intent);
                        }
                    } else {
                        Slog.i(ConfigUpdateInstallReceiver.TAG, "Not installing, new version is <= current version");
                    }
                } catch (Exception e) {
                    Slog.e(ConfigUpdateInstallReceiver.TAG, "Could not update content!", e);
                    String string = e.toString();
                    if (string.length() > 100) {
                        string = string.substring(0, 99);
                    }
                    EventLog.writeEvent(EventLogTags.CONFIG_INSTALL_FAILED, string);
                }
            }
        }.start();
    }

    private Uri getContentFromIntent(Intent intent) {
        Uri data = intent.getData();
        if (data == null) {
            throw new IllegalStateException("Missing required content path, ignoring.");
        }
        return data;
    }

    private int getVersionFromIntent(Intent intent) throws NumberFormatException {
        String stringExtra = intent.getStringExtra(EXTRA_VERSION_NUMBER);
        if (stringExtra == null) {
            throw new IllegalStateException("Missing required version number, ignoring.");
        }
        return Integer.parseInt(stringExtra.trim());
    }

    private String getRequiredHashFromIntent(Intent intent) {
        String stringExtra = intent.getStringExtra(EXTRA_REQUIRED_HASH);
        if (stringExtra == null) {
            throw new IllegalStateException("Missing required previous hash, ignoring.");
        }
        return stringExtra.trim();
    }

    private int getCurrentVersion() throws NumberFormatException {
        try {
            return Integer.parseInt(IoUtils.readFileAsString(this.updateVersion.getCanonicalPath()).trim());
        } catch (IOException e) {
            Slog.i(TAG, "Couldn't find current metadata, assuming first update");
            return 0;
        }
    }

    private byte[] getAltContent(Context context, Intent intent) throws IOException {
        InputStream inputStreamOpenInputStream = context.getContentResolver().openInputStream(getContentFromIntent(intent));
        try {
            return Streams.readFullyNoClose(inputStreamOpenInputStream);
        } finally {
            inputStreamOpenInputStream.close();
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

    private static String getCurrentHash(byte[] bArr) {
        if (bArr == null) {
            return "0";
        }
        try {
            return HexDump.toHexString(MessageDigest.getInstance("SHA512").digest(bArr), false);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    protected boolean verifyVersion(int i, int i2) {
        return i < i2;
    }

    private boolean verifyPreviousHash(String str, String str2) {
        if (str2.equals("NONE")) {
            return true;
        }
        return str.equals(str2);
    }

    protected void writeUpdate(File file, File file2, byte[] bArr) throws Throwable {
        File fileCreateTempFile;
        FileOutputStream fileOutputStream = null;
        try {
            File parentFile = file2.getParentFile();
            parentFile.mkdirs();
            if (!parentFile.exists()) {
                throw new IOException("Failed to create directory " + parentFile.getCanonicalPath());
            }
            fileCreateTempFile = File.createTempFile("journal", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, file);
            try {
                fileCreateTempFile.setReadable(true, false);
                FileOutputStream fileOutputStream2 = new FileOutputStream(fileCreateTempFile);
                try {
                    fileOutputStream2.write(bArr);
                    fileOutputStream2.getFD().sync();
                    if (!fileCreateTempFile.renameTo(file2)) {
                        throw new IOException("Failed to atomically rename " + file2.getCanonicalPath());
                    }
                    if (fileCreateTempFile != null) {
                        fileCreateTempFile.delete();
                    }
                    IoUtils.closeQuietly(fileOutputStream2);
                } catch (Throwable th) {
                    th = th;
                    fileOutputStream = fileOutputStream2;
                    if (fileCreateTempFile != null) {
                        fileCreateTempFile.delete();
                    }
                    IoUtils.closeQuietly(fileOutputStream);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Throwable th3) {
            th = th3;
            fileCreateTempFile = null;
        }
    }

    protected void install(byte[] bArr, int i) throws Throwable {
        writeUpdate(this.updateDir, this.updateContent, bArr);
        writeUpdate(this.updateDir, this.updateVersion, Long.toString(i).getBytes());
    }

    protected void postInstall(Context context, Intent intent) {
    }
}
