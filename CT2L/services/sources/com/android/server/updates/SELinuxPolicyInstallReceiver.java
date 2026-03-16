package com.android.server.updates;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Base64;
import android.util.Slog;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import libcore.io.IoUtils;

public class SELinuxPolicyInstallReceiver extends ConfigUpdateInstallReceiver {
    private static final String TAG = "SELinuxPolicyInstallReceiver";
    private static final String fileContextsPath = "file_contexts";
    private static final String macPermissionsPath = "mac_permissions.xml";
    private static final String propertyContextsPath = "property_contexts";
    private static final String seappContextsPath = "seapp_contexts";
    private static final String sepolicyPath = "sepolicy";
    private static final String serviceContextsPath = "service_contexts";
    private static final String versionPath = "selinux_version";

    public SELinuxPolicyInstallReceiver() {
        super("/data/security/bundle", "sepolicy_bundle", "metadata/", "version");
    }

    private void backupContexts(File contexts) {
        new File(contexts, versionPath).renameTo(new File(contexts, "selinux_version_backup"));
        new File(contexts, macPermissionsPath).renameTo(new File(contexts, "mac_permissions.xml_backup"));
        new File(contexts, seappContextsPath).renameTo(new File(contexts, "seapp_contexts_backup"));
        new File(contexts, propertyContextsPath).renameTo(new File(contexts, "property_contexts_backup"));
        new File(contexts, fileContextsPath).renameTo(new File(contexts, "file_contexts_backup"));
        new File(contexts, sepolicyPath).renameTo(new File(contexts, "sepolicy_backup"));
        new File(contexts, serviceContextsPath).renameTo(new File(contexts, "service_contexts_backup"));
    }

    private void copyUpdate(File contexts) {
        new File(this.updateDir, versionPath).renameTo(new File(contexts, versionPath));
        new File(this.updateDir, macPermissionsPath).renameTo(new File(contexts, macPermissionsPath));
        new File(this.updateDir, seappContextsPath).renameTo(new File(contexts, seappContextsPath));
        new File(this.updateDir, propertyContextsPath).renameTo(new File(contexts, propertyContextsPath));
        new File(this.updateDir, fileContextsPath).renameTo(new File(contexts, fileContextsPath));
        new File(this.updateDir, sepolicyPath).renameTo(new File(contexts, sepolicyPath));
        new File(this.updateDir, serviceContextsPath).renameTo(new File(contexts, serviceContextsPath));
    }

    private int readInt(BufferedInputStream reader) throws IOException {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value = (value << 8) | reader.read();
        }
        return value;
    }

    private int[] readChunkLengths(BufferedInputStream bundle) throws IOException {
        int[] chunks = {readInt(bundle), readInt(bundle), readInt(bundle), readInt(bundle), readInt(bundle), readInt(bundle), readInt(bundle)};
        return chunks;
    }

    private void installFile(File destination, BufferedInputStream stream, int length) throws IOException {
        byte[] chunk = new byte[length];
        stream.read(chunk, 0, length);
        writeUpdate(this.updateDir, destination, Base64.decode(chunk, 0));
    }

    private void unpackBundle() throws IOException {
        BufferedInputStream stream = new BufferedInputStream(new FileInputStream(this.updateContent));
        try {
            int[] chunkLengths = readChunkLengths(stream);
            installFile(new File(this.updateDir, versionPath), stream, chunkLengths[0]);
            installFile(new File(this.updateDir, macPermissionsPath), stream, chunkLengths[1]);
            installFile(new File(this.updateDir, seappContextsPath), stream, chunkLengths[2]);
            installFile(new File(this.updateDir, propertyContextsPath), stream, chunkLengths[3]);
            installFile(new File(this.updateDir, fileContextsPath), stream, chunkLengths[4]);
            installFile(new File(this.updateDir, sepolicyPath), stream, chunkLengths[5]);
            installFile(new File(this.updateDir, serviceContextsPath), stream, chunkLengths[6]);
        } finally {
            IoUtils.closeQuietly(stream);
        }
    }

    private void applyUpdate() throws IOException, ErrnoException {
        Slog.i(TAG, "Applying SELinux policy");
        File contexts = new File(this.updateDir.getParentFile(), "contexts");
        File current = new File(this.updateDir.getParentFile(), "current");
        File update = new File(this.updateDir.getParentFile(), "update");
        File tmp = new File(this.updateDir.getParentFile(), "tmp");
        if (current.exists()) {
            Os.symlink(this.updateDir.getPath(), update.getPath());
            Os.rename(update.getPath(), current.getPath());
        } else {
            Os.symlink(this.updateDir.getPath(), current.getPath());
        }
        contexts.mkdirs();
        backupContexts(contexts);
        copyUpdate(contexts);
        Os.symlink(contexts.getPath(), tmp.getPath());
        Os.rename(tmp.getPath(), current.getPath());
        SystemProperties.set("selinux.reload_policy", "1");
    }

    @Override
    protected void postInstall(Context context, Intent intent) {
        try {
            unpackBundle();
            applyUpdate();
        } catch (ErrnoException e) {
            Slog.e(TAG, "Could not update selinux policy: ", e);
        } catch (IOException e2) {
            Slog.e(TAG, "Could not update selinux policy: ", e2);
        } catch (IllegalArgumentException e3) {
            Slog.e(TAG, "SELinux policy update malformed: ", e3);
        }
    }
}
