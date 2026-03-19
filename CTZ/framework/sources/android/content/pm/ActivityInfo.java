package android.content.pm;

import android.content.res.TypedArray;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Printer;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ActivityInfo extends ComponentInfo implements Parcelable {
    public static final int COLOR_MODE_DEFAULT = 0;
    public static final int COLOR_MODE_HDR = 2;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT = 1;
    public static final int CONFIG_ASSETS_PATHS = Integer.MIN_VALUE;
    public static final int CONFIG_COLOR_MODE = 16384;
    public static final int CONFIG_DENSITY = 4096;
    public static final int CONFIG_FONT_SCALE = 1073741824;
    public static final int CONFIG_KEYBOARD = 16;
    public static final int CONFIG_KEYBOARD_HIDDEN = 32;
    public static final int CONFIG_LAYOUT_DIRECTION = 8192;
    public static final int CONFIG_LOCALE = 4;
    public static final int CONFIG_MCC = 1;
    public static final int CONFIG_MNC = 2;
    public static final int CONFIG_NAVIGATION = 64;
    public static final int CONFIG_ORIENTATION = 128;
    public static final int CONFIG_SCREEN_LAYOUT = 256;
    public static final int CONFIG_SCREEN_SIZE = 1024;
    public static final int CONFIG_SMALLEST_SCREEN_SIZE = 2048;
    public static final int CONFIG_TOUCHSCREEN = 8;
    public static final int CONFIG_UI_MODE = 512;
    public static final int CONFIG_WINDOW_CONFIGURATION = 536870912;
    public static final int DOCUMENT_LAUNCH_ALWAYS = 2;
    public static final int DOCUMENT_LAUNCH_INTO_EXISTING = 1;
    public static final int DOCUMENT_LAUNCH_NEVER = 3;
    public static final int DOCUMENT_LAUNCH_NONE = 0;
    public static final int FLAG_ALLOW_EMBEDDED = Integer.MIN_VALUE;
    public static final int FLAG_ALLOW_TASK_REPARENTING = 64;
    public static final int FLAG_ALWAYS_FOCUSABLE = 262144;
    public static final int FLAG_ALWAYS_RETAIN_TASK_STATE = 8;
    public static final int FLAG_AUTO_REMOVE_FROM_RECENTS = 8192;
    public static final int FLAG_CLEAR_TASK_ON_LAUNCH = 4;
    public static final int FLAG_ENABLE_VR_MODE = 32768;
    public static final int FLAG_EXCLUDE_FROM_RECENTS = 32;
    public static final int FLAG_FINISH_ON_CLOSE_SYSTEM_DIALOGS = 256;
    public static final int FLAG_FINISH_ON_TASK_LAUNCH = 2;
    public static final int FLAG_HARDWARE_ACCELERATED = 512;
    public static final int FLAG_IMMERSIVE = 2048;
    public static final int FLAG_IMPLICITLY_VISIBLE_TO_INSTANT_APP = 2097152;
    public static final int FLAG_MULTIPROCESS = 1;
    public static final int FLAG_NO_HISTORY = 128;
    public static final int FLAG_RELINQUISH_TASK_IDENTITY = 4096;
    public static final int FLAG_RESUME_WHILE_PAUSING = 16384;
    public static final int FLAG_SHOW_FOR_ALL_USERS = 1024;
    public static final int FLAG_SHOW_WHEN_LOCKED = 8388608;
    public static final int FLAG_SINGLE_USER = 1073741824;
    public static final int FLAG_STATE_NOT_NEEDED = 16;
    public static final int FLAG_SUPPORTS_PICTURE_IN_PICTURE = 4194304;
    public static final int FLAG_SYSTEM_USER_ONLY = 536870912;
    public static final int FLAG_TURN_SCREEN_ON = 16777216;
    public static final int FLAG_VISIBLE_TO_INSTANT_APP = 1048576;
    public static final int LAUNCH_MULTIPLE = 0;
    public static final int LAUNCH_SINGLE_INSTANCE = 3;
    public static final int LAUNCH_SINGLE_TASK = 2;
    public static final int LAUNCH_SINGLE_TOP = 1;
    public static final int LOCK_TASK_LAUNCH_MODE_ALWAYS = 2;
    public static final int LOCK_TASK_LAUNCH_MODE_DEFAULT = 0;
    public static final int LOCK_TASK_LAUNCH_MODE_IF_WHITELISTED = 3;
    public static final int LOCK_TASK_LAUNCH_MODE_NEVER = 1;
    public static final int PERSIST_ACROSS_REBOOTS = 2;
    public static final int PERSIST_NEVER = 1;
    public static final int PERSIST_ROOT_ONLY = 0;
    public static final int RESIZE_MODE_FORCE_RESIZABLE_LANDSCAPE_ONLY = 5;
    public static final int RESIZE_MODE_FORCE_RESIZABLE_PORTRAIT_ONLY = 6;
    public static final int RESIZE_MODE_FORCE_RESIZABLE_PRESERVE_ORIENTATION = 7;
    public static final int RESIZE_MODE_FORCE_RESIZEABLE = 4;
    public static final int RESIZE_MODE_RESIZEABLE = 2;
    public static final int RESIZE_MODE_RESIZEABLE_AND_PIPABLE_DEPRECATED = 3;
    public static final int RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION = 1;
    public static final int RESIZE_MODE_UNRESIZEABLE = 0;
    public static final int SCREEN_ORIENTATION_BEHIND = 3;
    public static final int SCREEN_ORIENTATION_FULL_SENSOR = 10;
    public static final int SCREEN_ORIENTATION_FULL_USER = 13;
    public static final int SCREEN_ORIENTATION_LANDSCAPE = 0;
    public static final int SCREEN_ORIENTATION_LOCKED = 14;
    public static final int SCREEN_ORIENTATION_NOSENSOR = 5;
    public static final int SCREEN_ORIENTATION_PORTRAIT = 1;
    public static final int SCREEN_ORIENTATION_REVERSE_LANDSCAPE = 8;
    public static final int SCREEN_ORIENTATION_REVERSE_PORTRAIT = 9;
    public static final int SCREEN_ORIENTATION_SENSOR = 4;
    public static final int SCREEN_ORIENTATION_SENSOR_LANDSCAPE = 6;
    public static final int SCREEN_ORIENTATION_SENSOR_PORTRAIT = 7;
    public static final int SCREEN_ORIENTATION_UNSET = -2;
    public static final int SCREEN_ORIENTATION_UNSPECIFIED = -1;
    public static final int SCREEN_ORIENTATION_USER = 2;
    public static final int SCREEN_ORIENTATION_USER_LANDSCAPE = 11;
    public static final int SCREEN_ORIENTATION_USER_PORTRAIT = 12;
    public static final int UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW = 1;
    public int colorMode;
    public int configChanges;
    public int documentLaunchMode;
    public int flags;
    public int launchMode;
    public String launchToken;
    public int lockTaskLaunchMode;
    public float maxAspectRatio;
    public int maxRecents;
    public String parentActivityName;
    public String permission;
    public int persistableMode;
    public String requestedVrComponent;
    public int resizeMode;
    public int rotationAnimation;
    public int screenOrientation;
    public int softInputMode;
    public String targetActivity;
    public String taskAffinity;
    public int theme;
    public int uiOptions;
    public WindowLayout windowLayout;
    public static int[] CONFIG_NATIVE_BITS = {2, 1, 4, 8, 16, 32, 64, 128, 2048, 4096, 512, 8192, 256, 16384, 65536};
    public static final Parcelable.Creator<ActivityInfo> CREATOR = new Parcelable.Creator<ActivityInfo>() {
        @Override
        public ActivityInfo createFromParcel(Parcel parcel) {
            return new ActivityInfo(parcel);
        }

        @Override
        public ActivityInfo[] newArray(int i) {
            return new ActivityInfo[i];
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface ColorMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Config {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ScreenOrientation {
    }

    public static int activityInfoConfigJavaToNative(int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < CONFIG_NATIVE_BITS.length; i3++) {
            if (((1 << i3) & i) != 0) {
                i2 |= CONFIG_NATIVE_BITS[i3];
            }
        }
        return i2;
    }

    public static int activityInfoConfigNativeToJava(int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < CONFIG_NATIVE_BITS.length; i3++) {
            if ((CONFIG_NATIVE_BITS[i3] & i) != 0) {
                i2 |= 1 << i3;
            }
        }
        return i2;
    }

    public int getRealConfigChanged() {
        if (this.applicationInfo.targetSdkVersion < 13) {
            return this.configChanges | 1024 | 2048;
        }
        return this.configChanges;
    }

    public static final String lockTaskLaunchModeToString(int i) {
        switch (i) {
            case 0:
                return "LOCK_TASK_LAUNCH_MODE_DEFAULT";
            case 1:
                return "LOCK_TASK_LAUNCH_MODE_NEVER";
            case 2:
                return "LOCK_TASK_LAUNCH_MODE_ALWAYS";
            case 3:
                return "LOCK_TASK_LAUNCH_MODE_IF_WHITELISTED";
            default:
                return "unknown=" + i;
        }
    }

    public ActivityInfo() {
        this.resizeMode = 2;
        this.colorMode = 0;
        this.screenOrientation = -1;
        this.uiOptions = 0;
        this.rotationAnimation = -1;
    }

    public ActivityInfo(ActivityInfo activityInfo) {
        super(activityInfo);
        this.resizeMode = 2;
        this.colorMode = 0;
        this.screenOrientation = -1;
        this.uiOptions = 0;
        this.rotationAnimation = -1;
        this.theme = activityInfo.theme;
        this.launchMode = activityInfo.launchMode;
        this.documentLaunchMode = activityInfo.documentLaunchMode;
        this.permission = activityInfo.permission;
        this.taskAffinity = activityInfo.taskAffinity;
        this.targetActivity = activityInfo.targetActivity;
        this.flags = activityInfo.flags;
        this.screenOrientation = activityInfo.screenOrientation;
        this.configChanges = activityInfo.configChanges;
        this.softInputMode = activityInfo.softInputMode;
        this.uiOptions = activityInfo.uiOptions;
        this.parentActivityName = activityInfo.parentActivityName;
        this.maxRecents = activityInfo.maxRecents;
        this.lockTaskLaunchMode = activityInfo.lockTaskLaunchMode;
        this.windowLayout = activityInfo.windowLayout;
        this.resizeMode = activityInfo.resizeMode;
        this.requestedVrComponent = activityInfo.requestedVrComponent;
        this.rotationAnimation = activityInfo.rotationAnimation;
        this.colorMode = activityInfo.colorMode;
        this.maxAspectRatio = activityInfo.maxAspectRatio;
    }

    public final int getThemeResource() {
        return this.theme != 0 ? this.theme : this.applicationInfo.theme;
    }

    private String persistableModeToString() {
        switch (this.persistableMode) {
            case 0:
                return "PERSIST_ROOT_ONLY";
            case 1:
                return "PERSIST_NEVER";
            case 2:
                return "PERSIST_ACROSS_REBOOTS";
            default:
                return "UNKNOWN=" + this.persistableMode;
        }
    }

    boolean isFixedOrientation() {
        return isFixedOrientationLandscape() || isFixedOrientationPortrait() || this.screenOrientation == 14;
    }

    boolean isFixedOrientationLandscape() {
        return isFixedOrientationLandscape(this.screenOrientation);
    }

    public static boolean isFixedOrientationLandscape(int i) {
        return i == 0 || i == 6 || i == 8 || i == 11;
    }

    boolean isFixedOrientationPortrait() {
        return isFixedOrientationPortrait(this.screenOrientation);
    }

    public static boolean isFixedOrientationPortrait(int i) {
        return i == 1 || i == 7 || i == 9 || i == 12;
    }

    public boolean supportsPictureInPicture() {
        return (this.flags & 4194304) != 0;
    }

    public static boolean isResizeableMode(int i) {
        return i == 2 || i == 4 || i == 6 || i == 5 || i == 7 || i == 1;
    }

    public static boolean isPreserveOrientationMode(int i) {
        return i == 6 || i == 5 || i == 7;
    }

    public static String resizeModeToString(int i) {
        switch (i) {
            case 0:
                return "RESIZE_MODE_UNRESIZEABLE";
            case 1:
                return "RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION";
            case 2:
                return "RESIZE_MODE_RESIZEABLE";
            case 3:
            default:
                return "unknown=" + i;
            case 4:
                return "RESIZE_MODE_FORCE_RESIZEABLE";
            case 5:
                return "RESIZE_MODE_FORCE_RESIZABLE_LANDSCAPE_ONLY";
            case 6:
                return "RESIZE_MODE_FORCE_RESIZABLE_PORTRAIT_ONLY";
            case 7:
                return "RESIZE_MODE_FORCE_RESIZABLE_PRESERVE_ORIENTATION";
        }
    }

    public void dump(Printer printer, String str) {
        dump(printer, str, 3);
    }

    public void dump(Printer printer, String str, int i) {
        super.dumpFront(printer, str);
        if (this.permission != null) {
            printer.println(str + "permission=" + this.permission);
        }
        int i2 = i & 1;
        if (i2 != 0) {
            printer.println(str + "taskAffinity=" + this.taskAffinity + " targetActivity=" + this.targetActivity + " persistableMode=" + persistableModeToString());
        }
        if (this.launchMode != 0 || this.flags != 0 || this.theme != 0) {
            printer.println(str + "launchMode=" + this.launchMode + " flags=0x" + Integer.toHexString(this.flags) + " theme=0x" + Integer.toHexString(this.theme));
        }
        if (this.screenOrientation != -1 || this.configChanges != 0 || this.softInputMode != 0) {
            printer.println(str + "screenOrientation=" + this.screenOrientation + " configChanges=0x" + Integer.toHexString(this.configChanges) + " softInputMode=0x" + Integer.toHexString(this.softInputMode));
        }
        if (this.uiOptions != 0) {
            printer.println(str + " uiOptions=0x" + Integer.toHexString(this.uiOptions));
        }
        if (i2 != 0) {
            printer.println(str + "lockTaskLaunchMode=" + lockTaskLaunchModeToString(this.lockTaskLaunchMode));
        }
        if (this.windowLayout != null) {
            printer.println(str + "windowLayout=" + this.windowLayout.width + "|" + this.windowLayout.widthFraction + ", " + this.windowLayout.height + "|" + this.windowLayout.heightFraction + ", " + this.windowLayout.gravity);
        }
        printer.println(str + "resizeMode=" + resizeModeToString(this.resizeMode));
        if (this.requestedVrComponent != null) {
            printer.println(str + "requestedVrComponent=" + this.requestedVrComponent);
        }
        if (this.maxAspectRatio != 0.0f) {
            printer.println(str + "maxAspectRatio=" + this.maxAspectRatio);
        }
        super.dumpBack(printer, str, i);
    }

    public String toString() {
        return "ActivityInfo{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.name + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(this.theme);
        parcel.writeInt(this.launchMode);
        parcel.writeInt(this.documentLaunchMode);
        parcel.writeString(this.permission);
        parcel.writeString(this.taskAffinity);
        parcel.writeString(this.targetActivity);
        parcel.writeString(this.launchToken);
        parcel.writeInt(this.flags);
        parcel.writeInt(this.screenOrientation);
        parcel.writeInt(this.configChanges);
        parcel.writeInt(this.softInputMode);
        parcel.writeInt(this.uiOptions);
        parcel.writeString(this.parentActivityName);
        parcel.writeInt(this.persistableMode);
        parcel.writeInt(this.maxRecents);
        parcel.writeInt(this.lockTaskLaunchMode);
        if (this.windowLayout != null) {
            parcel.writeInt(1);
            parcel.writeInt(this.windowLayout.width);
            parcel.writeFloat(this.windowLayout.widthFraction);
            parcel.writeInt(this.windowLayout.height);
            parcel.writeFloat(this.windowLayout.heightFraction);
            parcel.writeInt(this.windowLayout.gravity);
            parcel.writeInt(this.windowLayout.minWidth);
            parcel.writeInt(this.windowLayout.minHeight);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.resizeMode);
        parcel.writeString(this.requestedVrComponent);
        parcel.writeInt(this.rotationAnimation);
        parcel.writeInt(this.colorMode);
        parcel.writeFloat(this.maxAspectRatio);
    }

    public static boolean isTranslucentOrFloating(TypedArray typedArray) {
        return typedArray.getBoolean(4, false) || typedArray.getBoolean(5, false) || (!typedArray.hasValue(5) && typedArray.getBoolean(25, false));
    }

    public static String screenOrientationToString(int i) {
        switch (i) {
            case -2:
                return "SCREEN_ORIENTATION_UNSET";
            case -1:
                return "SCREEN_ORIENTATION_UNSPECIFIED";
            case 0:
                return "SCREEN_ORIENTATION_LANDSCAPE";
            case 1:
                return "SCREEN_ORIENTATION_PORTRAIT";
            case 2:
                return "SCREEN_ORIENTATION_USER";
            case 3:
                return "SCREEN_ORIENTATION_BEHIND";
            case 4:
                return "SCREEN_ORIENTATION_SENSOR";
            case 5:
                return "SCREEN_ORIENTATION_NOSENSOR";
            case 6:
                return "SCREEN_ORIENTATION_SENSOR_LANDSCAPE";
            case 7:
                return "SCREEN_ORIENTATION_SENSOR_PORTRAIT";
            case 8:
                return "SCREEN_ORIENTATION_REVERSE_LANDSCAPE";
            case 9:
                return "SCREEN_ORIENTATION_REVERSE_PORTRAIT";
            case 10:
                return "SCREEN_ORIENTATION_FULL_SENSOR";
            case 11:
                return "SCREEN_ORIENTATION_USER_LANDSCAPE";
            case 12:
                return "SCREEN_ORIENTATION_USER_PORTRAIT";
            case 13:
                return "SCREEN_ORIENTATION_FULL_USER";
            case 14:
                return "SCREEN_ORIENTATION_LOCKED";
            default:
                return Integer.toString(i);
        }
    }

    public static String colorModeToString(int i) {
        switch (i) {
            case 0:
                return "COLOR_MODE_DEFAULT";
            case 1:
                return "COLOR_MODE_WIDE_COLOR_GAMUT";
            case 2:
                return "COLOR_MODE_HDR";
            default:
                return Integer.toString(i);
        }
    }

    private ActivityInfo(Parcel parcel) {
        super(parcel);
        this.resizeMode = 2;
        this.colorMode = 0;
        this.screenOrientation = -1;
        this.uiOptions = 0;
        this.rotationAnimation = -1;
        this.theme = parcel.readInt();
        this.launchMode = parcel.readInt();
        this.documentLaunchMode = parcel.readInt();
        this.permission = parcel.readString();
        this.taskAffinity = parcel.readString();
        this.targetActivity = parcel.readString();
        this.launchToken = parcel.readString();
        this.flags = parcel.readInt();
        this.screenOrientation = parcel.readInt();
        this.configChanges = parcel.readInt();
        this.softInputMode = parcel.readInt();
        this.uiOptions = parcel.readInt();
        this.parentActivityName = parcel.readString();
        this.persistableMode = parcel.readInt();
        this.maxRecents = parcel.readInt();
        this.lockTaskLaunchMode = parcel.readInt();
        if (parcel.readInt() == 1) {
            this.windowLayout = new WindowLayout(parcel);
        }
        this.resizeMode = parcel.readInt();
        this.requestedVrComponent = parcel.readString();
        this.rotationAnimation = parcel.readInt();
        this.colorMode = parcel.readInt();
        this.maxAspectRatio = parcel.readFloat();
    }

    public static final class WindowLayout {
        public final int gravity;
        public final int height;
        public final float heightFraction;
        public final int minHeight;
        public final int minWidth;
        public final int width;
        public final float widthFraction;

        public WindowLayout(int i, float f, int i2, float f2, int i3, int i4, int i5) {
            this.width = i;
            this.widthFraction = f;
            this.height = i2;
            this.heightFraction = f2;
            this.gravity = i3;
            this.minWidth = i4;
            this.minHeight = i5;
        }

        WindowLayout(Parcel parcel) {
            this.width = parcel.readInt();
            this.widthFraction = parcel.readFloat();
            this.height = parcel.readInt();
            this.heightFraction = parcel.readFloat();
            this.gravity = parcel.readInt();
            this.minWidth = parcel.readInt();
            this.minHeight = parcel.readInt();
        }
    }
}
