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
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.FgThread;
import com.android.server.pm.PackageManagerService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public class UsbDebuggingManager {
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbDebuggingManager";
    private final Context mContext;
    private String mFingerprints;
    private UsbDebuggingThread mThread;
    private final String ADBD_SOCKET = "adbd";
    private final String ADB_DIRECTORY = "misc/adb";
    private final String ADB_KEYS_FILE = "adb_keys";
    private final int BUFFER_SIZE = 4096;
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
                LocalSocketAddress address = new LocalSocketAddress("adbd", LocalSocketAddress.Namespace.RESERVED);
                this.mInputStream = null;
                this.mSocket = new LocalSocket();
                this.mSocket.connect(address);
                this.mOutputStream = this.mSocket.getOutputStream();
                this.mInputStream = this.mSocket.getInputStream();
            } catch (IOException ioe) {
                closeSocketLocked();
                throw ioe;
            }
        }

        private void listenToSocket() throws IOException {
            try {
                byte[] buffer = new byte[4096];
                while (true) {
                    int count = this.mInputStream.read(buffer);
                    if (count < 0) {
                        break;
                    }
                    if (buffer[0] != 80 || buffer[1] != 75) {
                        break;
                    }
                    String key = new String(Arrays.copyOfRange(buffer, 2, count));
                    Slog.d(UsbDebuggingManager.TAG, "Received public key: " + key);
                    Message msg = UsbDebuggingManager.this.mHandler.obtainMessage(5);
                    msg.obj = key;
                    UsbDebuggingManager.this.mHandler.sendMessage(msg);
                }
                Slog.e(UsbDebuggingManager.TAG, "Wrong message: " + new String(Arrays.copyOfRange(buffer, 0, 2)));
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
                if (this.mSocket == null) {
                    return;
                }
                this.mSocket.close();
                this.mSocket = null;
            } catch (IOException ex) {
                Slog.e(UsbDebuggingManager.TAG, "Failed closing socket: " + ex);
            }
        }

        void stopListening() {
            synchronized (this) {
                this.mStopped = true;
                closeSocketLocked();
            }
        }

        void sendResponse(String msg) {
            synchronized (this) {
                if (!this.mStopped && this.mOutputStream != null) {
                    try {
                        this.mOutputStream.write(msg.getBytes());
                    } catch (IOException ex) {
                        Slog.e(UsbDebuggingManager.TAG, "Failed to write response:", ex);
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
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!UsbDebuggingManager.this.mAdbEnabled) {
                        UsbDebuggingManager.this.mAdbEnabled = true;
                        UsbDebuggingManager.this.mThread = UsbDebuggingManager.this.new UsbDebuggingThread();
                        UsbDebuggingManager.this.mThread.start();
                    }
                    break;
                case 2:
                    if (UsbDebuggingManager.this.mAdbEnabled) {
                        UsbDebuggingManager.this.mAdbEnabled = false;
                        if (UsbDebuggingManager.this.mThread != null) {
                            UsbDebuggingManager.this.mThread.stopListening();
                            UsbDebuggingManager.this.mThread = null;
                        }
                    }
                    break;
                case 3:
                    String key = (String) msg.obj;
                    String fingerprints = UsbDebuggingManager.this.getFingerprints(key);
                    if (!fingerprints.equals(UsbDebuggingManager.this.mFingerprints)) {
                        Slog.e(UsbDebuggingManager.TAG, "Fingerprints do not match. Got " + fingerprints + ", expected " + UsbDebuggingManager.this.mFingerprints);
                    } else {
                        if (msg.arg1 == 1) {
                            UsbDebuggingManager.this.writeKey(key);
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
                        String key2 = (String) msg.obj;
                        String fingerprints2 = UsbDebuggingManager.this.getFingerprints(key2);
                        if ("".equals(fingerprints2)) {
                            if (UsbDebuggingManager.this.mThread != null) {
                                UsbDebuggingManager.this.mThread.sendResponse("NO");
                            }
                        } else {
                            UsbDebuggingManager.this.mFingerprints = fingerprints2;
                            UsbDebuggingManager.this.startConfirmation(key2, UsbDebuggingManager.this.mFingerprints);
                        }
                    }
                    break;
                case 6:
                    UsbDebuggingManager.this.deleteKeyFile();
                    break;
            }
        }
    }

    private String getFingerprints(String key) {
        StringBuilder sb = new StringBuilder();
        if (key == null) {
            return "";
        }
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            byte[] base64_data = key.split("\\s+")[0].getBytes();
            try {
                byte[] digest = digester.digest(Base64.decode(base64_data, 0));
                for (int i = 0; i < digest.length; i++) {
                    sb.append("0123456789ABCDEF".charAt((digest[i] >> 4) & 15));
                    sb.append("0123456789ABCDEF".charAt(digest[i] & 15));
                    if (i < digest.length - 1) {
                        sb.append(":");
                    }
                }
                return sb.toString();
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "error doing base64 decoding", e);
                return "";
            }
        } catch (Exception ex) {
            Slog.e(TAG, "Error getting digester", ex);
            return "";
        }
    }

    private void startConfirmation(String key, String fingerprints) {
        String componentString;
        int currentUserId = ActivityManager.getCurrentUser();
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(currentUserId);
        if (userInfo.isAdmin()) {
            componentString = Resources.getSystem().getString(R.string.NetworkPreferenceSwitchTitle);
        } else {
            componentString = Resources.getSystem().getString(R.string.Noon);
        }
        ComponentName componentName = ComponentName.unflattenFromString(componentString);
        if (startConfirmationActivity(componentName, userInfo.getUserHandle(), key, fingerprints) || startConfirmationService(componentName, userInfo.getUserHandle(), key, fingerprints)) {
            return;
        }
        Slog.e(TAG, "unable to start customAdbPublicKeyConfirmation[SecondaryUser]Component " + componentString + " as an Activity or a Service");
    }

    private boolean startConfirmationActivity(ComponentName componentName, UserHandle userHandle, String key, String fingerprints) {
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent intent = createConfirmationIntent(componentName, key, fingerprints);
        intent.addFlags(268435456);
        if (packageManager.resolveActivity(intent, PackageManagerService.DumpState.DUMP_INSTALLS) != null) {
            try {
                this.mContext.startActivityAsUser(intent, userHandle);
                return true;
            } catch (ActivityNotFoundException e) {
                Slog.e(TAG, "unable to start adb whitelist activity: " + componentName, e);
                return false;
            }
        }
        return false;
    }

    private boolean startConfirmationService(ComponentName componentName, UserHandle userHandle, String key, String fingerprints) {
        Intent intent = createConfirmationIntent(componentName, key, fingerprints);
        try {
            if (this.mContext.startServiceAsUser(intent, userHandle) != null) {
                return true;
            }
            return false;
        } catch (SecurityException e) {
            Slog.e(TAG, "unable to start adb whitelist service: " + componentName, e);
            return false;
        }
    }

    private Intent createConfirmationIntent(ComponentName componentName, String key, String fingerprints) {
        Intent intent = new Intent();
        intent.setClassName(componentName.getPackageName(), componentName.getClassName());
        intent.putExtra("key", key);
        intent.putExtra("fingerprints", fingerprints);
        return intent;
    }

    private File getUserKeyFile() {
        File dataDir = Environment.getDataDirectory();
        File adbDir = new File(dataDir, "misc/adb");
        if (!adbDir.exists()) {
            Slog.e(TAG, "ADB data directory does not exist");
            return null;
        }
        return new File(adbDir, "adb_keys");
    }

    private void writeKey(String key) {
        try {
            File keyFile = getUserKeyFile();
            if (keyFile == null) {
                return;
            }
            if (!keyFile.exists()) {
                keyFile.createNewFile();
                FileUtils.setPermissions(keyFile.toString(), 416, -1, -1);
            }
            FileOutputStream fo = new FileOutputStream(keyFile, true);
            fo.write(key.getBytes());
            fo.write(10);
            fo.close();
        } catch (IOException ex) {
            Slog.e(TAG, "Error writing key:" + ex);
        }
    }

    private void deleteKeyFile() {
        File keyFile = getUserKeyFile();
        if (keyFile == null) {
            return;
        }
        keyFile.delete();
    }

    public void setAdbEnabled(boolean enabled) {
        this.mHandler.sendEmptyMessage(enabled ? 1 : 2);
    }

    public void allowUsbDebugging(boolean alwaysAllow, String publicKey) {
        Message msg = this.mHandler.obtainMessage(3);
        msg.arg1 = alwaysAllow ? 1 : 0;
        msg.obj = publicKey;
        this.mHandler.sendMessage(msg);
    }

    public void denyUsbDebugging() {
        this.mHandler.sendEmptyMessage(4);
    }

    public void clearUsbDebuggingKeys() {
        this.mHandler.sendEmptyMessage(6);
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("USB Debugging State:");
        pw.println("  Connected to adbd: " + (this.mThread != null));
        pw.println("  Last key received: " + this.mFingerprints);
        pw.println("  User keys:");
        try {
            pw.println(FileUtils.readTextFile(new File("/data/misc/adb/adb_keys"), 0, null));
        } catch (IOException e) {
            pw.println("IOException: " + e);
        }
        pw.println("  System keys:");
        try {
            pw.println(FileUtils.readTextFile(new File("/adb_keys"), 0, null));
        } catch (IOException e2) {
            pw.println("IOException: " + e2);
        }
    }
}
