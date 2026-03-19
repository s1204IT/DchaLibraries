package com.android.server.pm;

import android.app.admin.SecurityLog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
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
    public void handleMessage(Message message) {
        switch (message.what) {
            case 1:
                Bundle data = message.getData();
                String string = data.getString("processName");
                int i = data.getInt(WatchlistLoggingHandler.WatchlistEventKeys.UID);
                String string2 = data.getString("seinfo");
                String string3 = data.getString("apkFile");
                int i2 = data.getInt("pid");
                SecurityLog.writeEvent(210005, new Object[]{string, Long.valueOf(data.getLong("startTimestamp")), Integer.valueOf(i), Integer.valueOf(i2), string2, computeStringHashOfApk(string3)});
                break;
            case 2:
                this.mProcessLoggingBaseApkHashes.remove(message.getData().getString("apkFile"));
                break;
        }
    }

    void invalidateProcessLoggingBaseApkHash(String str) {
        Bundle bundle = new Bundle();
        bundle.putString("apkFile", str);
        Message messageObtainMessage = obtainMessage(2);
        messageObtainMessage.setData(bundle);
        sendMessage(messageObtainMessage);
    }

    private String computeStringHashOfApk(String str) {
        String string;
        if (str == null) {
            return "No APK";
        }
        String str2 = this.mProcessLoggingBaseApkHashes.get(str);
        if (str2 == null) {
            try {
                byte[] bArrComputeHashOfApkFile = computeHashOfApkFile(str);
                StringBuilder sb = new StringBuilder();
                for (byte b : bArrComputeHashOfApkFile) {
                    sb.append(String.format("%02x", Byte.valueOf(b)));
                }
                string = sb.toString();
            } catch (IOException | NoSuchAlgorithmException e) {
                e = e;
            }
            try {
                this.mProcessLoggingBaseApkHashes.put(str, string);
                str2 = string;
            } catch (IOException | NoSuchAlgorithmException e2) {
                e = e2;
                str2 = string;
                Slog.w(TAG, "computeStringHashOfApk() failed", e);
            }
        }
        return str2 != null ? str2 : "Failed to count APK hash";
    }

    private byte[] computeHashOfApkFile(String str) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        FileInputStream fileInputStream = new FileInputStream(new File(str));
        byte[] bArr = new byte[65536];
        while (true) {
            int i = fileInputStream.read(bArr);
            if (i > 0) {
                messageDigest.update(bArr, 0, i);
            } else {
                fileInputStream.close();
                return messageDigest.digest();
            }
        }
    }
}
