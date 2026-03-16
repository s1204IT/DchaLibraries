package com.android.server.usb;

import android.R;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Slog;
import com.android.server.FgThread;
import com.android.server.pm.PackageManagerService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.Arrays;

public class UsbDebuggingManager implements Runnable {
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbDebuggingManager";
    private final Context mContext;
    private String mFingerprints;
    private Thread mThread;
    private final String ADBD_SOCKET = "adbd";
    private final String ADB_DIRECTORY = "misc/adb";
    private final String ADB_KEYS_FILE = "adb_keys";
    private final int BUFFER_SIZE = PackageManagerService.DumpState.DUMP_VERSION;
    private boolean mAdbEnabled = DEBUG;
    private LocalSocket mSocket = null;
    private OutputStream mOutputStream = null;
    private final Handler mHandler = new UsbDebuggingHandler(FgThread.get().getLooper());

    public UsbDebuggingManager(Context context) {
        this.mContext = context;
    }

    private void listenToSocket() throws IOException {
        try {
            byte[] buffer = new byte[PackageManagerService.DumpState.DUMP_VERSION];
            LocalSocketAddress address = new LocalSocketAddress("adbd", LocalSocketAddress.Namespace.RESERVED);
            this.mSocket = new LocalSocket();
            this.mSocket.connect(address);
            this.mOutputStream = this.mSocket.getOutputStream();
            InputStream inputStream = this.mSocket.getInputStream();
            while (true) {
                int count = inputStream.read(buffer);
                if (count < 0) {
                    break;
                }
                if (buffer[0] != 80 || buffer[1] != 75) {
                    break;
                }
                String key = new String(Arrays.copyOfRange(buffer, 2, count));
                Slog.d(TAG, "Received public key: " + key);
                Message msg = this.mHandler.obtainMessage(5);
                msg.obj = key;
                this.mHandler.sendMessage(msg);
            }
            Slog.e(TAG, "Wrong message: " + new String(Arrays.copyOfRange(buffer, 0, 2)));
        } finally {
            closeSocket();
        }
    }

    @Override
    public void run() {
        while (this.mAdbEnabled) {
            try {
                listenToSocket();
            } catch (Exception e) {
                SystemClock.sleep(1000L);
            }
        }
    }

    private void closeSocket() {
        try {
            this.mOutputStream.close();
        } catch (IOException e) {
            Slog.e(TAG, "Failed closing output stream: " + e);
        }
        try {
            this.mSocket.close();
        } catch (IOException ex) {
            Slog.e(TAG, "Failed closing socket: " + ex);
        }
    }

    private void sendResponse(String msg) {
        if (this.mOutputStream != null) {
            try {
                this.mOutputStream.write(msg.getBytes());
            } catch (IOException ex) {
                Slog.e(TAG, "Failed to write response:", ex);
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
                        UsbDebuggingManager.this.mThread = new Thread(UsbDebuggingManager.this, UsbDebuggingManager.TAG);
                        UsbDebuggingManager.this.mThread.start();
                    }
                    break;
                case 2:
                    if (UsbDebuggingManager.this.mAdbEnabled) {
                        UsbDebuggingManager.this.mAdbEnabled = UsbDebuggingManager.DEBUG;
                        UsbDebuggingManager.this.closeSocket();
                        try {
                            UsbDebuggingManager.this.mThread.join();
                            break;
                        } catch (Exception e) {
                        }
                        UsbDebuggingManager.this.mThread = null;
                        UsbDebuggingManager.this.mOutputStream = null;
                        UsbDebuggingManager.this.mSocket = null;
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
                        UsbDebuggingManager.this.sendResponse("OK");
                    }
                    break;
                case 4:
                    UsbDebuggingManager.this.sendResponse("NO");
                    break;
                case 5:
                    String key2 = (String) msg.obj;
                    String fingerprints2 = UsbDebuggingManager.this.getFingerprints(key2);
                    if ("".equals(fingerprints2)) {
                        UsbDebuggingManager.this.sendResponse("NO");
                    } else {
                        UsbDebuggingManager.this.mFingerprints = fingerprints2;
                        UsbDebuggingManager.this.startConfirmation(key2, UsbDebuggingManager.this.mFingerprints);
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
        String nameString = Resources.getSystem().getString(R.string.config_systemBluetoothStack);
        ComponentName componentName = ComponentName.unflattenFromString(nameString);
        if (!startConfirmationActivity(componentName, key, fingerprints) && !startConfirmationService(componentName, key, fingerprints)) {
            Slog.e(TAG, "unable to start customAdbPublicKeyConfirmationComponent " + nameString + " as an Activity or a Service");
        }
    }

    private boolean startConfirmationActivity(ComponentName componentName, String key, String fingerprints) {
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent intent = createConfirmationIntent(componentName, key, fingerprints);
        intent.addFlags(268435456);
        if (packageManager.resolveActivity(intent, 65536) != null) {
            try {
                this.mContext.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Slog.e(TAG, "unable to start adb whitelist activity: " + componentName, e);
            }
        }
        return DEBUG;
    }

    private boolean startConfirmationService(ComponentName componentName, String key, String fingerprints) {
        Intent intent = createConfirmationIntent(componentName, key, fingerprints);
        try {
        } catch (SecurityException e) {
            Slog.e(TAG, "unable to start adb whitelist service: " + componentName, e);
        }
        if (this.mContext.startService(intent) != null) {
            return true;
        }
        return DEBUG;
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
        if (adbDir.exists()) {
            return new File(adbDir, "adb_keys");
        }
        Slog.e(TAG, "ADB data directory does not exist");
        return null;
    }

    private void writeKey(String key) {
        try {
            File keyFile = getUserKeyFile();
            if (keyFile != null) {
                if (!keyFile.exists()) {
                    keyFile.createNewFile();
                    FileUtils.setPermissions(keyFile.toString(), 416, -1, -1);
                }
                FileOutputStream fo = new FileOutputStream(keyFile, true);
                fo.write(key.getBytes());
                fo.write(10);
                fo.close();
            }
        } catch (IOException ex) {
            Slog.e(TAG, "Error writing key:" + ex);
        }
    }

    private void deleteKeyFile() {
        File keyFile = getUserKeyFile();
        if (keyFile != null) {
            keyFile.delete();
        }
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

    public void dump(FileDescriptor fd, PrintWriter pw) {
        boolean z = DEBUG;
        pw.println("  USB Debugging State:");
        StringBuilder sbAppend = new StringBuilder().append("    Connected to adbd: ");
        if (this.mOutputStream != null) {
            z = true;
        }
        pw.println(sbAppend.append(z).toString());
        pw.println("    Last key received: " + this.mFingerprints);
        pw.println("    User keys:");
        try {
            pw.println(FileUtils.readTextFile(new File("/data/misc/adb/adb_keys"), 0, null));
        } catch (IOException e) {
            pw.println("IOException: " + e);
        }
        pw.println("    System keys:");
        try {
            pw.println(FileUtils.readTextFile(new File("/adb_keys"), 0, null));
        } catch (IOException e2) {
            pw.println("IOException: " + e2);
        }
    }
}
