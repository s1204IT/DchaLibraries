package com.android.server.pm;

import android.app.admin.SecurityLog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.pm.PackageManagerService;
import com.mediatek.appworkingset.AWSDBHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public final class ProcessLoggingHandler extends Handler {
    static final int INVALIDATE_BASE_APK_HASH_MSG = 2;
    static final int LOG_APP_PROCESS_START_MSG = 1;
    private static final String TAG = "ProcessLoggingHandler";
    private final HashMap<String, String> mProcessLoggingBaseApkHashes;

    ProcessLoggingHandler() {
        super(BackgroundThread.getHandler().getLooper());
        this.mProcessLoggingBaseApkHashes = new HashMap<>();
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                Bundle bundle = msg.getData();
                String processName = bundle.getString("processName");
                int uid = bundle.getInt(AWSDBHelper.PackageProcessList.KEY_UID);
                String seinfo = bundle.getString("seinfo");
                String apkFile = bundle.getString("apkFile");
                int pid = bundle.getInt("pid");
                long startTimestamp = bundle.getLong("startTimestamp");
                String apkHash = computeStringHashOfApk(apkFile);
                SecurityLog.writeEvent(210005, new Object[]{processName, Long.valueOf(startTimestamp), Integer.valueOf(uid), Integer.valueOf(pid), seinfo, apkHash});
                break;
            case 2:
                this.mProcessLoggingBaseApkHashes.remove(msg.getData().getString("apkFile"));
                break;
        }
    }

    void invalidateProcessLoggingBaseApkHash(String apkPath) {
        Bundle data = new Bundle();
        data.putString("apkFile", apkPath);
        Message msg = obtainMessage(2);
        msg.setData(data);
        sendMessage(msg);
    }

    private String computeStringHashOfApk(String apkFile) {
        if (apkFile == null) {
            return "No APK";
        }
        String apkHash = this.mProcessLoggingBaseApkHashes.get(apkFile);
        if (apkHash == null) {
            try {
                byte[] hash = computeHashOfApkFile(apkFile);
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) {
                    sb.append(String.format("%02x", Byte.valueOf(b)));
                }
                apkHash = sb.toString();
                this.mProcessLoggingBaseApkHashes.put(apkFile, apkHash);
            } catch (IOException | NoSuchAlgorithmException e) {
                Slog.w(TAG, "computeStringHashOfApk() failed", e);
            }
        }
        return apkHash != null ? apkHash : "Failed to count APK hash";
    }

    private byte[] computeHashOfApkFile(String packageArchiveLocation) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        FileInputStream input = new FileInputStream(new File(packageArchiveLocation));
        byte[] buffer = new byte[PackageManagerService.DumpState.DUMP_INSTALLS];
        while (true) {
            int size = input.read(buffer);
            if (size > 0) {
                md.update(buffer, 0, size);
            } else {
                input.close();
                return md.digest();
            }
        }
    }
}
