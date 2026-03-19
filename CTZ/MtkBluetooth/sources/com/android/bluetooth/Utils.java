package com.android.bluetooth;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Build;
import android.os.ParcelUuid;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class Utils {
    static final int BD_ADDR_LEN = 6;
    static final int BD_UUID_LEN = 16;
    private static final int MICROS_PER_UNIT = 625;
    private static final String PTS_TEST_MODE_PROPERTY = "persist.bluetooth.pts";
    private static final String TAG = "BluetoothUtils";
    static int sSystemUiUid = -10000;
    static int sForegroundUserId = -10000;

    public static String getAddressStringFromByte(byte[] bArr) {
        if (bArr == null || bArr.length != 6) {
            return null;
        }
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X", Byte.valueOf(bArr[0]), Byte.valueOf(bArr[1]), Byte.valueOf(bArr[2]), Byte.valueOf(bArr[3]), Byte.valueOf(bArr[4]), Byte.valueOf(bArr[5]));
    }

    public static byte[] getByteAddress(BluetoothDevice bluetoothDevice) {
        return getBytesFromAddress(bluetoothDevice.getAddress());
    }

    public static byte[] getBytesFromAddress(String str) {
        byte[] bArr = new byte[6];
        int i = 0;
        int i2 = 0;
        while (i < str.length()) {
            if (str.charAt(i) != ':') {
                bArr[i2] = (byte) Integer.parseInt(str.substring(i, i + 2), 16);
                i2++;
                i++;
            }
            i++;
        }
        return bArr;
    }

    public static int byteArrayToInt(byte[] bArr) {
        return byteArrayToInt(bArr, 0);
    }

    public static short byteArrayToShort(byte[] bArr) {
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        byteBufferWrap.order(ByteOrder.nativeOrder());
        return byteBufferWrap.getShort();
    }

    public static int byteArrayToInt(byte[] bArr, int i) {
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        byteBufferWrap.order(ByteOrder.nativeOrder());
        return byteBufferWrap.getInt(i);
    }

    public static String byteArrayToString(byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bArr.length; i++) {
            if (i != 0) {
                sb.append(" ");
            }
            sb.append(String.format("%02x", Byte.valueOf(bArr[i])));
        }
        return sb.toString();
    }

    public static byte[] intToByteArray(int i) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(4);
        byteBufferAllocate.order(ByteOrder.nativeOrder());
        byteBufferAllocate.putInt(i);
        return byteBufferAllocate.array();
    }

    public static byte[] uuidToByteArray(ParcelUuid parcelUuid) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(16);
        byteBufferAllocate.order(ByteOrder.BIG_ENDIAN);
        UUID uuid = parcelUuid.getUuid();
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();
        byteBufferAllocate.putLong(mostSignificantBits);
        byteBufferAllocate.putLong(8, leastSignificantBits);
        return byteBufferAllocate.array();
    }

    public static byte[] uuidsToByteArray(ParcelUuid[] parcelUuidArr) {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(parcelUuidArr.length * 16);
        byteBufferAllocate.order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < parcelUuidArr.length; i++) {
            UUID uuid = parcelUuidArr[i].getUuid();
            long mostSignificantBits = uuid.getMostSignificantBits();
            long leastSignificantBits = uuid.getLeastSignificantBits();
            int i2 = i * 16;
            byteBufferAllocate.putLong(i2, mostSignificantBits);
            byteBufferAllocate.putLong(i2 + 8, leastSignificantBits);
        }
        return byteBufferAllocate.array();
    }

    public static ParcelUuid[] byteArrayToUuid(byte[] bArr) {
        int length = bArr.length / 16;
        ParcelUuid[] parcelUuidArr = new ParcelUuid[length];
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        byteBufferWrap.order(ByteOrder.BIG_ENDIAN);
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            parcelUuidArr[i2] = new ParcelUuid(new UUID(byteBufferWrap.getLong(i), byteBufferWrap.getLong(i + 8)));
            i += 16;
        }
        return parcelUuidArr;
    }

    public static String debugGetAdapterStateString(int i) {
        switch (i) {
            case 10:
                return "STATE_OFF";
            case 11:
                return "STATE_TURNING_ON";
            case 12:
                return "STATE_ON";
            case 13:
                return "STATE_TURNING_OFF";
            default:
                return "UNKNOWN";
        }
    }

    public static String ellipsize(String str) {
        if (!Build.TYPE.equals("user")) {
            return str;
        }
        if (str == null) {
            return null;
        }
        if (str.length() < 3) {
            return str;
        }
        return str.charAt(0) + "⋯" + str.charAt(str.length() - 1);
    }

    public static void copyStream(InputStream inputStream, OutputStream outputStream, int i) throws IOException {
        if (inputStream != null && outputStream != null) {
            byte[] bArr = new byte[i];
            while (true) {
                int i2 = inputStream.read(bArr);
                if (i2 >= 0) {
                    outputStream.write(bArr, 0, i2);
                } else {
                    return;
                }
            }
        }
    }

    public static void safeCloseStream(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Throwable th) {
                Log.d(TAG, "Error closing stream", th);
            }
        }
    }

    public static void safeCloseStream(OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (Throwable th) {
                Log.d(TAG, "Error closing stream", th);
            }
        }
    }

    public static void setSystemUiUid(int i) {
        sSystemUiUid = i;
    }

    public static void setForegroundUserId(int i) {
        sForegroundUserId = i;
    }

    public static boolean checkCaller() {
        int callingUserId = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        return sForegroundUserId == callingUserId || sSystemUiUid == callingUid || 1000 == callingUid;
    }

    public static boolean checkCallerAllowManagedProfiles(Context context) {
        if (context == null) {
            return checkCaller();
        }
        int callingUserId = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        boolean z = false;
        try {
            UserInfo profileParent = ((UserManager) context.getSystemService("user")).getProfileParent(callingUserId);
            int i = profileParent != null ? profileParent.id : -10000;
            if (sForegroundUserId == callingUserId || sForegroundUserId == i) {
                z = true;
            } else if (sSystemUiUid == callingUid || 1000 == callingUid) {
            }
            return z;
        } catch (Exception e) {
            Log.e(TAG, "checkCallerAllowManagedProfiles: Exception ex=" + e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public static void enforceAdminPermission(ContextWrapper contextWrapper) {
        contextWrapper.enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
    }

    public static boolean checkCallerHasLocationPermission(Context context, AppOpsManager appOpsManager, String str) {
        if (context.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == 0 && isAppOppAllowed(appOpsManager, 1, str)) {
            return true;
        }
        if (context.checkCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == 0 && isAppOppAllowed(appOpsManager, 0, str)) {
            return true;
        }
        if (isMApp(context, str)) {
            if (!checkCallerHasPeersMacAddressPermission(context)) {
                throw new SecurityException("Need ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission to get scan results");
            }
        } else {
            if (isForegroundApp(context, str)) {
                return true;
            }
            Log.e(TAG, "Permission denial: Need ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission to get scan results");
        }
        return false;
    }

    public static boolean checkCallerHasPeersMacAddressPermission(Context context) {
        return context.checkCallingOrSelfPermission("android.permission.PEERS_MAC_ADDRESS") == 0;
    }

    public static boolean isLegacyForegroundApp(Context context, String str) {
        return !isMApp(context, str) && isForegroundApp(context, str);
    }

    private static boolean isMApp(Context context, String str) {
        try {
            return context.getPackageManager().getApplicationInfo(str, 0).targetSdkVersion >= 23;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    private static boolean isForegroundApp(Context context, String str) {
        List<ActivityManager.RunningTaskInfo> runningTasks = ((ActivityManager) context.getSystemService("activity")).getRunningTasks(1);
        return !runningTasks.isEmpty() && str.equals(runningTasks.get(0).topActivity.getPackageName());
    }

    private static boolean isAppOppAllowed(AppOpsManager appOpsManager, int i, String str) {
        return appOpsManager.noteOp(i, Binder.getCallingUid(), str) == 0;
    }

    public static int millsToUnit(int i) {
        return (int) (TimeUnit.MILLISECONDS.toMicros(i) / 625);
    }

    public static boolean isInstrumentationTestMode() {
        try {
            return Class.forName("com.android.bluetooth.FileSystemWriteTest") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void enforceInstrumentationTestMode() {
        if (!isInstrumentationTestMode()) {
            throw new IllegalStateException("Not in BluetoothInstrumentationTest");
        }
    }

    public static boolean isPtsTestMode() {
        return SystemProperties.getBoolean(PTS_TEST_MODE_PROPERTY, false);
    }

    public static String getUidPidString() {
        return "uid/pid=" + Binder.getCallingUid() + "/" + Binder.getCallingPid();
    }
}
