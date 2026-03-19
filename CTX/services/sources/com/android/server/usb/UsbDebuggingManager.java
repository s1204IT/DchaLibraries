package com.android.server.usb;

import android.R;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Base64;
import android.util.Slog;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.dump.DumpUtils;
import com.android.server.FgThread;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public class UsbDebuggingManager {
    private static final String ADBD_SOCKET = "adbd";
    private static final String ADB_DIRECTORY = "misc/adb";
    private static final String ADB_KEYS_FILE = "adb_keys";
    private static final int BUFFER_SIZE = 4096;
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbDebuggingManager";
    private final Context mContext;
    private String mFingerprints;
    private UsbDebuggingThread mThread;
    private boolean mAdbEnabled = false;
    private final Handler mHandler = new UsbDebuggingHandler(FgThread.get().getLooper());

    public UsbDebuggingManager(Context context) {
        this.mContext = context;
    }

    class UsbDebuggingThread extends Thread {
        private InputStream mInputStream;
        private OutputStream mOutputStream;
        private LocalSocket mSocket;
        private boolean mStopped;

        UsbDebuggingThread() {
            super(UsbDebuggingManager.TAG);
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    if (this.mStopped) {
                        return;
                    }
                    try {
                        openSocketLocked();
                    } catch (Exception e) {
                        SystemClock.sleep(1000L);
                    }
                }
                try {
                    listenToSocket();
                } catch (Exception e2) {
                    SystemClock.sleep(1000L);
                }
            }
        }

        private void openSocketLocked() throws IOException {
            try {
                LocalSocketAddress localSocketAddress = new LocalSocketAddress(UsbDebuggingManager.ADBD_SOCKET, LocalSocketAddress.Namespace.RESERVED);
                this.mInputStream = null;
                this.mSocket = new LocalSocket();
                this.mSocket.connect(localSocketAddress);
                this.mOutputStream = this.mSocket.getOutputStream();
                this.mInputStream = this.mSocket.getInputStream();
            } catch (IOException e) {
                closeSocketLocked();
                throw e;
            }
        }

        private void listenToSocket() throws IOException {
            try {
                byte[] bArr = new byte[4096];
                while (true) {
                    int i = this.mInputStream.read(bArr);
                    if (i < 0) {
                        break;
                    }
                    if (bArr[0] != 80 || bArr[1] != 75) {
                        break;
                    }
                    String str = new String(Arrays.copyOfRange(bArr, 2, i));
                    Slog.d(UsbDebuggingManager.TAG, "Received public key: " + str);
                    Message messageObtainMessage = UsbDebuggingManager.this.mHandler.obtainMessage(5);
                    messageObtainMessage.obj = str;
                    UsbDebuggingManager.this.mHandler.sendMessage(messageObtainMessage);
                }
                Slog.e(UsbDebuggingManager.TAG, "Wrong message: " + new String(Arrays.copyOfRange(bArr, 0, 2)));
                synchronized (this) {
                    closeSocketLocked();
                }
            } catch (Throwable th) {
                synchronized (this) {
                    closeSocketLocked();
                    throw th;
                }
            }
        }

        private void closeSocketLocked() {
            try {
                if (this.mOutputStream != null) {
                    this.mOutputStream.close();
                    this.mOutputStream = null;
                }
            } catch (IOException e) {
                Slog.e(UsbDebuggingManager.TAG, "Failed closing output stream: " + e);
            }
            try {
                if (this.mSocket != null) {
                    this.mSocket.close();
                    this.mSocket = null;
                }
            } catch (IOException e2) {
                Slog.e(UsbDebuggingManager.TAG, "Failed closing socket: " + e2);
            }
        }

        void stopListening() {
            synchronized (this) {
                this.mStopped = true;
                closeSocketLocked();
            }
        }

        void sendResponse(String str) {
            synchronized (this) {
                if (!this.mStopped && this.mOutputStream != null) {
                    try {
                        this.mOutputStream.write(str.getBytes());
                    } catch (IOException e) {
                        Slog.e(UsbDebuggingManager.TAG, "Failed to write response:", e);
                    }
                }
            }
        }
    }

    class UsbDebuggingHandler extends Handler {
        private static final int MESSAGE_ADB_ALLOW = 3;
        private static final int MESSAGE_ADB_CLEAR = 6;
        private static final int MESSAGE_ADB_CONFIRM = 5;
        private static final int MESSAGE_ADB_DENY = 4;
        private static final int MESSAGE_ADB_DISABLED = 2;
        private static final int MESSAGE_ADB_ENABLED = 1;

        public UsbDebuggingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    if (!UsbDebuggingManager.this.mAdbEnabled) {
                        UsbDebuggingManager.this.mAdbEnabled = true;
                        UsbDebuggingManager.this.mThread = UsbDebuggingManager.this.new UsbDebuggingThread();
                        UsbDebuggingManager.this.mThread.start();
                        break;
                    }
                    break;
                case 2:
                    if (UsbDebuggingManager.this.mAdbEnabled) {
                        UsbDebuggingManager.this.mAdbEnabled = false;
                        if (UsbDebuggingManager.this.mThread != null) {
                            UsbDebuggingManager.this.mThread.stopListening();
                            UsbDebuggingManager.this.mThread = null;
                        }
                        break;
                    }
                    break;
                case 3:
                    String str = (String) message.obj;
                    String fingerprints = UsbDebuggingManager.this.getFingerprints(str);
                    if (!fingerprints.equals(UsbDebuggingManager.this.mFingerprints)) {
                        Slog.e(UsbDebuggingManager.TAG, "Fingerprints do not match. Got " + fingerprints + ", expected " + UsbDebuggingManager.this.mFingerprints);
                    } else {
                        if (message.arg1 == 1) {
                            UsbDebuggingManager.this.writeKey(str);
                        }
                        if (UsbDebuggingManager.this.mThread != null) {
                            UsbDebuggingManager.this.mThread.sendResponse("OK");
                        }
                    }
                    break;
                case 4:
                    if (UsbDebuggingManager.this.mThread != null) {
                        UsbDebuggingManager.this.mThread.sendResponse("NO");
                    }
                    break;
                case 5:
                    if ("trigger_restart_min_framework".equals(SystemProperties.get("vold.decrypt"))) {
                        Slog.d(UsbDebuggingManager.TAG, "Deferring adb confirmation until after vold decrypt");
                        if (UsbDebuggingManager.this.mThread != null) {
                            UsbDebuggingManager.this.mThread.sendResponse("NO");
                        }
                    } else {
                        String str2 = (String) message.obj;
                        String fingerprints2 = UsbDebuggingManager.this.getFingerprints(str2);
                        if (BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(fingerprints2)) {
                            if (UsbDebuggingManager.this.mThread != null) {
                                UsbDebuggingManager.this.mThread.sendResponse("NO");
                            }
                        } else {
                            UsbDebuggingManager.this.mFingerprints = fingerprints2;
                            UsbDebuggingManager.this.startConfirmation(str2, UsbDebuggingManager.this.mFingerprints);
                        }
                    }
                    break;
                case 6:
                    UsbDebuggingManager.this.deleteKeyFile();
                    break;
            }
        }
    }

    private String getFingerprints(String str) {
        StringBuilder sb = new StringBuilder();
        if (str == null) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        try {
            try {
                byte[] bArrDigest = MessageDigest.getInstance("MD5").digest(Base64.decode(str.split("\\s+")[0].getBytes(), 0));
                for (int i = 0; i < bArrDigest.length; i++) {
                    sb.append("0123456789ABCDEF".charAt((bArrDigest[i] >> 4) & 15));
                    sb.append("0123456789ABCDEF".charAt(bArrDigest[i] & UsbDescriptor.DESCRIPTORTYPE_BOS));
                    if (i < bArrDigest.length - 1) {
                        sb.append(":");
                    }
                }
                return sb.toString();
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "error doing base64 decoding", e);
                return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
        } catch (Exception e2) {
            Slog.e(TAG, "Error getting digester", e2);
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
    }

    private void startConfirmation(String str, String str2) {
        String string;
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(ActivityManager.getCurrentUser());
        if (userInfo.isAdmin()) {
            string = Resources.getSystem().getString(R.string.accessibility_system_action_media_play_pause_label);
        } else {
            string = Resources.getSystem().getString(R.string.accessibility_system_action_menu_label);
        }
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(string);
        if (startConfirmationActivity(componentNameUnflattenFromString, userInfo.getUserHandle(), str, str2) || startConfirmationService(componentNameUnflattenFromString, userInfo.getUserHandle(), str, str2)) {
            return;
        }
        Slog.e(TAG, "unable to start customAdbPublicKeyConfirmation[SecondaryUser]Component " + string + " as an Activity or a Service");
    }

    private boolean startConfirmationActivity(ComponentName componentName, UserHandle userHandle, String str, String str2) {
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent intentCreateConfirmationIntent = createConfirmationIntent(componentName, str, str2);
        intentCreateConfirmationIntent.addFlags(268435456);
        if (packageManager.resolveActivity(intentCreateConfirmationIntent, 65536) != null) {
            try {
                this.mContext.startActivityAsUser(intentCreateConfirmationIntent, userHandle);
                return true;
            } catch (ActivityNotFoundException e) {
                Slog.e(TAG, "unable to start adb whitelist activity: " + componentName, e);
                return false;
            }
        }
        return false;
    }

    private boolean startConfirmationService(ComponentName componentName, UserHandle userHandle, String str, String str2) {
        try {
            if (this.mContext.startServiceAsUser(createConfirmationIntent(componentName, str, str2), userHandle) != null) {
                return true;
            }
            return false;
        } catch (SecurityException e) {
            Slog.e(TAG, "unable to start adb whitelist service: " + componentName, e);
            return false;
        }
    }

    private Intent createConfirmationIntent(ComponentName componentName, String str, String str2) {
        Intent intent = new Intent();
        intent.setClassName(componentName.getPackageName(), componentName.getClassName());
        intent.putExtra("key", str);
        intent.putExtra("fingerprints", str2);
        return intent;
    }

    private File getUserKeyFile() {
        File file = new File(Environment.getDataDirectory(), ADB_DIRECTORY);
        if (!file.exists()) {
            Slog.e(TAG, "ADB data directory does not exist");
            return null;
        }
        return new File(file, ADB_KEYS_FILE);
    }

    private void writeKey(String str) {
        try {
            File userKeyFile = getUserKeyFile();
            if (userKeyFile == null) {
                return;
            }
            if (!userKeyFile.exists()) {
                userKeyFile.createNewFile();
                FileUtils.setPermissions(userKeyFile.toString(), 416, -1, -1);
            }
            FileOutputStream fileOutputStream = new FileOutputStream(userKeyFile, true);
            fileOutputStream.write(str.getBytes());
            fileOutputStream.write(10);
            fileOutputStream.close();
        } catch (IOException e) {
            Slog.e(TAG, "Error writing key:" + e);
        }
    }

    private void deleteKeyFile() {
        File userKeyFile = getUserKeyFile();
        if (userKeyFile != null) {
            userKeyFile.delete();
        }
    }

    public void setAdbEnabled(boolean z) {
        this.mHandler.sendEmptyMessage(z ? 1 : 2);
    }

    public void allowUsbDebugging(boolean z, String str) {
        Message messageObtainMessage = this.mHandler.obtainMessage(3);
        messageObtainMessage.arg1 = z ? 1 : 0;
        messageObtainMessage.obj = str;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void denyUsbDebugging() {
        this.mHandler.sendEmptyMessage(4);
    }

    public void clearUsbDebuggingKeys() {
        this.mHandler.sendEmptyMessage(6);
    }

    public void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
        long jStart = dualDumpOutputStream.start(str, j);
        dualDumpOutputStream.write("connected_to_adb", 1133871366145L, this.mThread != null);
        DumpUtils.writeStringIfNotNull(dualDumpOutputStream, "last_key_received", 1138166333442L, this.mFingerprints);
        try {
            dualDumpOutputStream.write("user_keys", 1138166333443L, FileUtils.readTextFile(new File("/data/misc/adb/adb_keys"), 0, null));
        } catch (IOException e) {
            Slog.e(TAG, "Cannot read user keys", e);
        }
        try {
            dualDumpOutputStream.write("system_keys", 1138166333444L, FileUtils.readTextFile(new File("/adb_keys"), 0, null));
        } catch (IOException e2) {
            Slog.e(TAG, "Cannot read system keys", e2);
        }
        dualDumpOutputStream.end(jStart);
    }
}
