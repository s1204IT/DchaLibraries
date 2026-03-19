package android.os;

import android.annotation.SystemApi;
import android.content.Context;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.IDeviceIdentifiersPolicyService;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.telephony.TelephonyProperties;
import dalvik.system.VMRuntime;
import java.util.Objects;

public class Build {

    @Deprecated
    public static final String CPU_ABI;

    @Deprecated
    public static final String CPU_ABI2;
    public static final String FINGERPRINT;
    public static final String HOST;
    public static final boolean IS_CONTAINER;
    public static final boolean IS_DEBUGGABLE;
    public static final boolean IS_ENG;
    public static final boolean IS_TREBLE_ENABLED;
    public static final boolean IS_USER;
    public static final boolean IS_USERDEBUG;

    @SystemApi
    public static final boolean PERMISSIONS_REVIEW_REQUIRED;
    private static final String TAG = "Build";
    public static final String TAGS;
    public static final long TIME;
    public static final String TYPE;
    public static final String UNKNOWN = "unknown";
    public static final String USER;
    public static final String ID = getString("ro.build.id");
    public static final String DISPLAY = getString("ro.build.display.id");
    public static final String PRODUCT = getString("ro.product.name");
    public static final String DEVICE = getString("ro.product.device");
    public static final String BOARD = getString("ro.product.board");
    public static final String MANUFACTURER = getString("ro.product.manufacturer");
    public static final String BRAND = getString("ro.product.brand");
    public static final String MODEL = getString("ro.product.model");
    public static final String BOOTLOADER = getString("ro.bootloader");

    @Deprecated
    public static final String RADIO = getString(TelephonyProperties.PROPERTY_BASEBAND_VERSION);
    public static final String HARDWARE = getString("ro.hardware");
    public static final boolean IS_EMULATOR = getString("ro.kernel.qemu").equals(WifiEnterpriseConfig.ENGINE_ENABLE);

    @Deprecated
    public static final String SERIAL = getString("no.such.thing");
    public static final String[] SUPPORTED_ABIS = getStringList("ro.product.cpu.abilist", ",");
    public static final String[] SUPPORTED_32_BIT_ABIS = getStringList("ro.product.cpu.abilist32", ",");
    public static final String[] SUPPORTED_64_BIT_ABIS = getStringList("ro.product.cpu.abilist64", ",");

    public static class VERSION_CODES {
        public static final int BASE = 1;
        public static final int BASE_1_1 = 2;
        public static final int CUPCAKE = 3;
        public static final int CUR_DEVELOPMENT = 10000;
        public static final int DONUT = 4;
        public static final int ECLAIR = 5;
        public static final int ECLAIR_0_1 = 6;
        public static final int ECLAIR_MR1 = 7;
        public static final int FROYO = 8;
        public static final int GINGERBREAD = 9;
        public static final int GINGERBREAD_MR1 = 10;
        public static final int HONEYCOMB = 11;
        public static final int HONEYCOMB_MR1 = 12;
        public static final int HONEYCOMB_MR2 = 13;
        public static final int ICE_CREAM_SANDWICH = 14;
        public static final int ICE_CREAM_SANDWICH_MR1 = 15;
        public static final int JELLY_BEAN = 16;
        public static final int JELLY_BEAN_MR1 = 17;
        public static final int JELLY_BEAN_MR2 = 18;
        public static final int KITKAT = 19;
        public static final int KITKAT_WATCH = 20;
        public static final int L = 21;
        public static final int LOLLIPOP = 21;
        public static final int LOLLIPOP_MR1 = 22;
        public static final int M = 23;
        public static final int N = 24;
        public static final int N_MR1 = 25;
        public static final int O = 26;
        public static final int O_MR1 = 27;
        public static final int P = 28;
    }

    static {
        String[] strArr;
        if (VMRuntime.getRuntime().is64Bit()) {
            strArr = SUPPORTED_64_BIT_ABIS;
        } else {
            strArr = SUPPORTED_32_BIT_ABIS;
        }
        CPU_ABI = strArr[0];
        if (strArr.length > 1) {
            CPU_ABI2 = strArr[1];
        } else {
            CPU_ABI2 = "";
        }
        TYPE = getString("ro.build.type");
        TAGS = getString("ro.build.tags");
        FINGERPRINT = deriveFingerprint();
        IS_TREBLE_ENABLED = SystemProperties.getBoolean("ro.treble.enabled", false);
        TIME = getLong("ro.build.date.utc") * 1000;
        USER = getString("ro.build.user");
        HOST = getString("ro.build.host");
        IS_DEBUGGABLE = SystemProperties.getInt("ro.debuggable", 0) == 1;
        IS_ENG = "eng".equals(TYPE);
        IS_USERDEBUG = "userdebug".equals(TYPE);
        IS_USER = "user".equals(TYPE);
        IS_CONTAINER = SystemProperties.getBoolean("ro.boot.container", false);
        PERMISSIONS_REVIEW_REQUIRED = SystemProperties.getInt("ro.permission_review_required", 0) == 1;
    }

    public static String getSerial() {
        try {
            return IDeviceIdentifiersPolicyService.Stub.asInterface(ServiceManager.getService(Context.DEVICE_IDENTIFIERS_SERVICE)).getSerial();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return "unknown";
        }
    }

    public static class VERSION {
        public static final String[] ACTIVE_CODENAMES;
        public static final int MIN_SUPPORTED_TARGET_SDK_INT;
        public static final int RESOURCES_SDK_INT;
        public static final String INCREMENTAL = Build.getString("ro.build.version.incremental");
        public static final String RELEASE = Build.getString("ro.build.version.release");
        public static final String BASE_OS = SystemProperties.get("ro.build.version.base_os", "");
        public static final String SECURITY_PATCH = SystemProperties.get("ro.build.version.security_patch", "");

        @Deprecated
        public static final String SDK = Build.getString("ro.build.version.sdk");
        public static final int SDK_INT = SystemProperties.getInt("ro.build.version.sdk", 0);
        public static final int FIRST_SDK_INT = SystemProperties.getInt("ro.product.first_api_level", 0);
        public static final int PREVIEW_SDK_INT = SystemProperties.getInt("ro.build.version.preview_sdk", 0);
        public static final String CODENAME = Build.getString("ro.build.version.codename");
        private static final String[] ALL_CODENAMES = Build.getStringList("ro.build.version.all_codenames", ",");

        static {
            ACTIVE_CODENAMES = "REL".equals(ALL_CODENAMES[0]) ? new String[0] : ALL_CODENAMES;
            RESOURCES_SDK_INT = SDK_INT + ACTIVE_CODENAMES.length;
            MIN_SUPPORTED_TARGET_SDK_INT = SystemProperties.getInt("ro.build.version.min_supported_target_sdk", 0);
        }
    }

    private static String deriveFingerprint() {
        String str = SystemProperties.get("ro.build.fingerprint");
        if (TextUtils.isEmpty(str)) {
            return getString("ro.product.brand") + '/' + getString("ro.product.name") + '/' + getString("ro.product.device") + ':' + getString("ro.build.version.release") + '/' + getString("ro.build.id") + '/' + getString("ro.build.version.incremental") + ':' + getString("ro.build.type") + '/' + getString("ro.build.tags");
        }
        return str;
    }

    public static void ensureFingerprintProperty() {
        if (TextUtils.isEmpty(SystemProperties.get("ro.build.fingerprint"))) {
            try {
                SystemProperties.set("ro.build.fingerprint", FINGERPRINT);
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "Failed to set fingerprint property", e);
            }
        }
    }

    public static boolean isBuildConsistent() {
        if (IS_ENG) {
            return true;
        }
        if (IS_TREBLE_ENABLED) {
            int iVerifyWithoutAvb = VintfObject.verifyWithoutAvb();
            if (iVerifyWithoutAvb != 0) {
                Slog.e(TAG, "Vendor interface is incompatible, error=" + String.valueOf(iVerifyWithoutAvb));
            }
            return iVerifyWithoutAvb == 0;
        }
        String str = SystemProperties.get("ro.build.fingerprint");
        String str2 = SystemProperties.get("ro.vendor.build.fingerprint");
        SystemProperties.get("ro.bootimage.build.fingerprint");
        SystemProperties.get("ro.build.expect.bootloader");
        SystemProperties.get("ro.bootloader");
        SystemProperties.get("ro.build.expect.baseband");
        SystemProperties.get(TelephonyProperties.PROPERTY_BASEBAND_VERSION);
        if (TextUtils.isEmpty(str)) {
            Slog.e(TAG, "Required ro.build.fingerprint is empty!");
            return false;
        }
        if (TextUtils.isEmpty(str2) || Objects.equals(str, str2)) {
            return true;
        }
        Slog.e(TAG, "Mismatched fingerprints; system reported " + str + " but vendor reported " + str2);
        return false;
    }

    public static String getRadioVersion() {
        return null;
    }

    private static String getString(String str) {
        return SystemProperties.get(str, "unknown");
    }

    private static String[] getStringList(String str, String str2) {
        String str3 = SystemProperties.get(str);
        if (str3.isEmpty()) {
            return new String[0];
        }
        return str3.split(str2);
    }

    private static long getLong(String str) {
        try {
            return Long.parseLong(SystemProperties.get(str));
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
