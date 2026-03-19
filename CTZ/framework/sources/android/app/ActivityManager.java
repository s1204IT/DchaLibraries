package android.app;

import android.annotation.SystemApi;
import android.app.IActivityManager;
import android.app.IAppTask;
import android.app.IUidObserver;
import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.GraphicBuffer;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.BatteryStats;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.SettingsStringUtil;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Singleton;
import android.util.Size;
import com.android.internal.R;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.os.RoSystemProperties;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.MemInfoReader;
import com.android.server.LocalServices;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlSerializer;

public class ActivityManager {
    public static final String ACTION_REPORT_HEAP_LIMIT = "android.app.action.REPORT_HEAP_LIMIT";
    public static final int APP_START_MODE_DELAYED = 1;
    public static final int APP_START_MODE_DELAYED_RIGID = 2;
    public static final int APP_START_MODE_DISABLED = 3;
    public static final int APP_START_MODE_NORMAL = 0;
    public static final int ASSIST_CONTEXT_AUTOFILL = 2;
    public static final int ASSIST_CONTEXT_BASIC = 0;
    public static final int ASSIST_CONTEXT_FULL = 1;
    public static final int BROADCAST_FAILED_USER_STOPPED = -2;
    public static final int BROADCAST_STICKY_CANT_HAVE_PERMISSION = -1;
    public static final int BROADCAST_SUCCESS = 0;
    public static final int BUGREPORT_OPTION_FULL = 0;
    public static final int BUGREPORT_OPTION_INTERACTIVE = 1;
    public static final int BUGREPORT_OPTION_REMOTE = 2;
    public static final int BUGREPORT_OPTION_TELEPHONY = 4;
    public static final int BUGREPORT_OPTION_WEAR = 3;
    public static final int BUGREPORT_OPTION_WIFI = 5;
    public static final int COMPAT_MODE_ALWAYS = -1;
    public static final int COMPAT_MODE_DISABLED = 0;
    public static final int COMPAT_MODE_ENABLED = 1;
    public static final int COMPAT_MODE_NEVER = -2;
    public static final int COMPAT_MODE_TOGGLE = 2;
    public static final int COMPAT_MODE_UNKNOWN = -3;
    private static final int FIRST_START_FATAL_ERROR_CODE = -100;
    private static final int FIRST_START_NON_FATAL_ERROR_CODE = 100;
    private static final int FIRST_START_SUCCESS_CODE = 0;
    public static final int FLAG_AND_LOCKED = 2;
    public static final int FLAG_AND_UNLOCKED = 4;
    public static final int FLAG_AND_UNLOCKING_OR_UNLOCKED = 8;
    public static final int FLAG_OR_STOPPED = 1;
    public static final int INTENT_SENDER_ACTIVITY = 2;
    public static final int INTENT_SENDER_ACTIVITY_RESULT = 3;
    public static final int INTENT_SENDER_BROADCAST = 1;
    public static final int INTENT_SENDER_FOREGROUND_SERVICE = 5;
    public static final int INTENT_SENDER_SERVICE = 4;
    private static final int LAST_START_FATAL_ERROR_CODE = -1;
    private static final int LAST_START_NON_FATAL_ERROR_CODE = 199;
    private static final int LAST_START_SUCCESS_CODE = 99;
    public static final int LOCK_TASK_MODE_LOCKED = 1;
    public static final int LOCK_TASK_MODE_NONE = 0;
    public static final int LOCK_TASK_MODE_PINNED = 2;
    public static final int MAX_PROCESS_STATE = 19;
    public static final String META_HOME_ALTERNATE = "android.app.home.alternate";
    public static final int MIN_PROCESS_STATE = 0;
    public static final int MOVE_TASK_NO_USER_ACTION = 2;
    public static final int MOVE_TASK_WITH_HOME = 1;
    public static final int PROCESS_STATE_BACKUP = 8;
    public static final int PROCESS_STATE_BOUND_FOREGROUND_SERVICE = 4;
    public static final int PROCESS_STATE_CACHED_ACTIVITY = 15;
    public static final int PROCESS_STATE_CACHED_ACTIVITY_CLIENT = 16;
    public static final int PROCESS_STATE_CACHED_EMPTY = 18;
    public static final int PROCESS_STATE_CACHED_RECENT = 17;
    public static final int PROCESS_STATE_FOREGROUND_SERVICE = 3;
    public static final int PROCESS_STATE_HEAVY_WEIGHT = 12;
    public static final int PROCESS_STATE_HOME = 13;
    public static final int PROCESS_STATE_IMPORTANT_BACKGROUND = 6;
    public static final int PROCESS_STATE_IMPORTANT_FOREGROUND = 5;
    public static final int PROCESS_STATE_LAST_ACTIVITY = 14;
    public static final int PROCESS_STATE_NONEXISTENT = 19;
    public static final int PROCESS_STATE_PERSISTENT = 0;
    public static final int PROCESS_STATE_PERSISTENT_UI = 1;
    public static final int PROCESS_STATE_RECEIVER = 10;
    public static final int PROCESS_STATE_SERVICE = 9;
    public static final int PROCESS_STATE_TOP = 2;
    public static final int PROCESS_STATE_TOP_SLEEPING = 11;
    public static final int PROCESS_STATE_TRANSIENT_BACKGROUND = 7;
    public static final int PROCESS_STATE_UNKNOWN = -1;
    public static final int RECENT_IGNORE_UNAVAILABLE = 2;
    public static final int RECENT_WITH_EXCLUDED = 1;
    public static final int RESIZE_MODE_FORCED = 2;
    public static final int RESIZE_MODE_PRESERVE_WINDOW = 1;
    public static final int RESIZE_MODE_SYSTEM = 0;
    public static final int RESIZE_MODE_SYSTEM_SCREEN_ROTATION = 1;
    public static final int RESIZE_MODE_USER = 1;
    public static final int RESIZE_MODE_USER_FORCED = 3;
    public static final int SPLIT_SCREEN_CREATE_MODE_BOTTOM_OR_RIGHT = 1;
    public static final int SPLIT_SCREEN_CREATE_MODE_TOP_OR_LEFT = 0;
    public static final int START_ABORTED = 102;
    public static final int START_ASSISTANT_HIDDEN_SESSION = -90;
    public static final int START_ASSISTANT_NOT_ACTIVE_SESSION = -89;
    public static final int START_CANCELED = -96;
    public static final int START_CLASS_NOT_FOUND = -92;
    public static final int START_DELIVERED_TO_TOP = 3;
    public static final int START_FLAG_DEBUG = 2;
    public static final int START_FLAG_NATIVE_DEBUGGING = 8;
    public static final int START_FLAG_ONLY_IF_NEEDED = 1;
    public static final int START_FLAG_TRACK_ALLOCATION = 4;
    public static final int START_FORWARD_AND_REQUEST_CONFLICT = -93;
    public static final int START_INTENT_NOT_RESOLVED = -91;
    public static final int START_NOT_ACTIVITY = -95;
    public static final int START_NOT_CURRENT_USER_ACTIVITY = -98;
    public static final int START_NOT_VOICE_COMPATIBLE = -97;
    public static final int START_PERMISSION_DENIED = -94;
    public static final int START_RETURN_INTENT_TO_CALLER = 1;
    public static final int START_RETURN_LOCK_TASK_MODE_VIOLATION = 101;
    public static final int START_SUCCESS = 0;
    public static final int START_SWITCHES_CANCELED = 100;
    public static final int START_TASK_TO_FRONT = 2;
    public static final int START_VOICE_HIDDEN_SESSION = -100;
    public static final int START_VOICE_NOT_ACTIVE_SESSION = -99;
    public static final int UID_OBSERVER_ACTIVE = 8;
    public static final int UID_OBSERVER_CACHED = 16;
    public static final int UID_OBSERVER_GONE = 2;
    public static final int UID_OBSERVER_IDLE = 4;
    public static final int UID_OBSERVER_PROCSTATE = 1;
    public static final int USER_OP_ERROR_IS_SYSTEM = -3;
    public static final int USER_OP_ERROR_RELATED_USERS_CANNOT_STOP = -4;
    public static final int USER_OP_IS_CURRENT = -2;
    public static final int USER_OP_SUCCESS = 0;
    public static final int USER_OP_UNKNOWN_USER = -1;
    Point mAppTaskThumbnailSize;
    private final Context mContext;
    final ArrayMap<OnUidImportanceListener, UidObserver> mImportanceListeners = new ArrayMap<>();
    private static String TAG = "ActivityManager";
    private static int gMaxRecentTasks = -1;
    private static volatile boolean sSystemReady = false;
    private static final boolean DEVELOPMENT_FORCE_LOW_RAM = SystemProperties.getBoolean("debug.force_low_ram", false);
    private static final Singleton<IActivityManager> IActivityManagerSingleton = new Singleton<IActivityManager>() {
        @Override
        protected IActivityManager create() {
            return IActivityManager.Stub.asInterface(ServiceManager.getService(Context.ACTIVITY_SERVICE));
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface BugreportMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface MoveTaskFlags {
    }

    @SystemApi
    public interface OnUidImportanceListener {
        void onUidImportance(int i, int i2);
    }

    static final class UidObserver extends IUidObserver.Stub {
        final Context mContext;
        final OnUidImportanceListener mListener;

        UidObserver(OnUidImportanceListener onUidImportanceListener, Context context) {
            this.mListener = onUidImportanceListener;
            this.mContext = context;
        }

        @Override
        public void onUidStateChanged(int i, int i2, long j) {
            this.mListener.onUidImportance(i, RunningAppProcessInfo.procStateToImportanceForClient(i2, this.mContext));
        }

        @Override
        public void onUidGone(int i, boolean z) {
            this.mListener.onUidImportance(i, 1000);
        }

        @Override
        public void onUidActive(int i) {
        }

        @Override
        public void onUidIdle(int i, boolean z) {
        }

        @Override
        public void onUidCachedChanged(int i, boolean z) {
        }
    }

    public static final int processStateAmToProto(int i) {
        switch (i) {
            case -1:
                return 999;
            case 0:
                return 1000;
            case 1:
                return 1001;
            case 2:
                return 1002;
            case 3:
                return 1003;
            case 4:
                return 1004;
            case 5:
                return 1005;
            case 6:
                return 1006;
            case 7:
                return 1007;
            case 8:
                return 1008;
            case 9:
                return 1009;
            case 10:
                return 1010;
            case 11:
                return 1011;
            case 12:
                return 1012;
            case 13:
                return 1013;
            case 14:
                return 1014;
            case 15:
                return 1015;
            case 16:
                return 1016;
            case 17:
                return 1017;
            case 18:
                return 1018;
            case 19:
                return 1019;
            default:
                return 998;
        }
    }

    public static final boolean isProcStateBackground(int i) {
        return i >= 7;
    }

    ActivityManager(Context context, Handler handler) {
        this.mContext = context;
    }

    public static final boolean isStartResultSuccessful(int i) {
        return i >= 0 && i <= 99;
    }

    public static final boolean isStartResultFatalError(int i) {
        return -100 <= i && i <= -1;
    }

    public static class StackId {
        public static final int INVALID_STACK_ID = -1;

        private StackId() {
        }
    }

    public int getFrontActivityScreenCompatMode() {
        try {
            return getService().getFrontActivityScreenCompatMode();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setFrontActivityScreenCompatMode(int i) {
        try {
            getService().setFrontActivityScreenCompatMode(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getPackageScreenCompatMode(String str) {
        try {
            return getService().getPackageScreenCompatMode(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setPackageScreenCompatMode(String str, int i) {
        try {
            getService().setPackageScreenCompatMode(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean getPackageAskScreenCompat(String str) {
        try {
            return getService().getPackageAskScreenCompat(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setPackageAskScreenCompat(String str, boolean z) {
        try {
            getService().setPackageAskScreenCompat(str, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getMemoryClass() {
        return staticGetMemoryClass();
    }

    public static int staticGetMemoryClass() {
        String str = SystemProperties.get("dalvik.vm.heapgrowthlimit", "");
        if (str != null && !"".equals(str)) {
            return Integer.parseInt(str.substring(0, str.length() - 1));
        }
        return staticGetLargeMemoryClass();
    }

    public int getLargeMemoryClass() {
        return staticGetLargeMemoryClass();
    }

    public static int staticGetLargeMemoryClass() {
        return Integer.parseInt(SystemProperties.get("dalvik.vm.heapsize", "16m").substring(0, r0.length() - 1));
    }

    public boolean isLowRamDevice() {
        return isLowRamDeviceStatic();
    }

    public static boolean isLowRamDeviceStatic() {
        return RoSystemProperties.CONFIG_LOW_RAM || (Build.IS_DEBUGGABLE && DEVELOPMENT_FORCE_LOW_RAM);
    }

    public static boolean isSmallBatteryDevice() {
        return RoSystemProperties.CONFIG_SMALL_BATTERY;
    }

    public static boolean isHighEndGfx() {
        return (isLowRamDeviceStatic() || RoSystemProperties.CONFIG_AVOID_GFX_ACCEL || Resources.getSystem().getBoolean(R.bool.config_avoidGfxAccel)) ? false : true;
    }

    public long getTotalRam() {
        MemInfoReader memInfoReader = new MemInfoReader();
        memInfoReader.readMemInfo();
        return memInfoReader.getTotalSize();
    }

    public static int getMaxRecentTasksStatic() {
        if (gMaxRecentTasks < 0) {
            int i = isLowRamDeviceStatic() ? 36 : 48;
            gMaxRecentTasks = i;
            return i;
        }
        return gMaxRecentTasks;
    }

    public static int getDefaultAppRecentsLimitStatic() {
        return getMaxRecentTasksStatic() / 6;
    }

    public static int getMaxAppRecentsLimitStatic() {
        return getMaxRecentTasksStatic() / 2;
    }

    public static boolean supportsMultiWindow(Context context) {
        return (!isLowRamDeviceStatic() || context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH)) && Resources.getSystem().getBoolean(R.bool.config_supportsMultiWindow);
    }

    public static boolean supportsSplitScreenMultiWindow(Context context) {
        return supportsMultiWindow(context) && Resources.getSystem().getBoolean(R.bool.config_supportsSplitScreenMultiWindow);
    }

    @Deprecated
    public static int getMaxNumPictureInPictureActions() {
        return 3;
    }

    public static class TaskDescription implements Parcelable {
        private static final String ATTR_TASKDESCRIPTIONCOLOR_BACKGROUND = "task_description_colorBackground";
        private static final String ATTR_TASKDESCRIPTIONCOLOR_PRIMARY = "task_description_color";
        private static final String ATTR_TASKDESCRIPTIONICON_FILENAME = "task_description_icon_filename";
        private static final String ATTR_TASKDESCRIPTIONICON_RESOURCE = "task_description_icon_resource";
        private static final String ATTR_TASKDESCRIPTIONLABEL = "task_description_label";
        public static final String ATTR_TASKDESCRIPTION_PREFIX = "task_description_";
        public static final Parcelable.Creator<TaskDescription> CREATOR = new Parcelable.Creator<TaskDescription>() {
            @Override
            public TaskDescription createFromParcel(Parcel parcel) {
                return new TaskDescription(parcel);
            }

            @Override
            public TaskDescription[] newArray(int i) {
                return new TaskDescription[i];
            }
        };
        private int mColorBackground;
        private int mColorPrimary;
        private Bitmap mIcon;
        private String mIconFilename;
        private int mIconRes;
        private String mLabel;
        private int mNavigationBarColor;
        private int mStatusBarColor;

        @Deprecated
        public TaskDescription(String str, Bitmap bitmap, int i) {
            this(str, bitmap, 0, null, i, 0, 0, 0);
            if (i != 0 && Color.alpha(i) != 255) {
                throw new RuntimeException("A TaskDescription's primary color should be opaque");
            }
        }

        public TaskDescription(String str, int i, int i2) {
            this(str, null, i, null, i2, 0, 0, 0);
            if (i2 != 0 && Color.alpha(i2) != 255) {
                throw new RuntimeException("A TaskDescription's primary color should be opaque");
            }
        }

        @Deprecated
        public TaskDescription(String str, Bitmap bitmap) {
            this(str, bitmap, 0, null, 0, 0, 0, 0);
        }

        public TaskDescription(String str, int i) {
            this(str, null, i, null, 0, 0, 0, 0);
        }

        public TaskDescription(String str) {
            this(str, null, 0, null, 0, 0, 0, 0);
        }

        public TaskDescription() {
            this(null, null, 0, null, 0, 0, 0, 0);
        }

        public TaskDescription(String str, Bitmap bitmap, int i, String str2, int i2, int i3, int i4, int i5) {
            this.mLabel = str;
            this.mIcon = bitmap;
            this.mIconRes = i;
            this.mIconFilename = str2;
            this.mColorPrimary = i2;
            this.mColorBackground = i3;
            this.mStatusBarColor = i4;
            this.mNavigationBarColor = i5;
        }

        public TaskDescription(TaskDescription taskDescription) {
            copyFrom(taskDescription);
        }

        public void copyFrom(TaskDescription taskDescription) {
            this.mLabel = taskDescription.mLabel;
            this.mIcon = taskDescription.mIcon;
            this.mIconRes = taskDescription.mIconRes;
            this.mIconFilename = taskDescription.mIconFilename;
            this.mColorPrimary = taskDescription.mColorPrimary;
            this.mColorBackground = taskDescription.mColorBackground;
            this.mStatusBarColor = taskDescription.mStatusBarColor;
            this.mNavigationBarColor = taskDescription.mNavigationBarColor;
        }

        public void copyFromPreserveHiddenFields(TaskDescription taskDescription) {
            this.mLabel = taskDescription.mLabel;
            this.mIcon = taskDescription.mIcon;
            this.mIconRes = taskDescription.mIconRes;
            this.mIconFilename = taskDescription.mIconFilename;
            this.mColorPrimary = taskDescription.mColorPrimary;
            if (taskDescription.mColorBackground != 0) {
                this.mColorBackground = taskDescription.mColorBackground;
            }
            if (taskDescription.mStatusBarColor != 0) {
                this.mStatusBarColor = taskDescription.mStatusBarColor;
            }
            if (taskDescription.mNavigationBarColor != 0) {
                this.mNavigationBarColor = taskDescription.mNavigationBarColor;
            }
        }

        private TaskDescription(Parcel parcel) {
            readFromParcel(parcel);
        }

        public void setLabel(String str) {
            this.mLabel = str;
        }

        public void setPrimaryColor(int i) {
            if (i != 0 && Color.alpha(i) != 255) {
                throw new RuntimeException("A TaskDescription's primary color should be opaque");
            }
            this.mColorPrimary = i;
        }

        public void setBackgroundColor(int i) {
            if (i != 0 && Color.alpha(i) != 255) {
                throw new RuntimeException("A TaskDescription's background color should be opaque");
            }
            this.mColorBackground = i;
        }

        public void setStatusBarColor(int i) {
            this.mStatusBarColor = i;
        }

        public void setNavigationBarColor(int i) {
            this.mNavigationBarColor = i;
        }

        public void setIcon(Bitmap bitmap) {
            this.mIcon = bitmap;
        }

        public void setIcon(int i) {
            this.mIconRes = i;
        }

        public void setIconFilename(String str) {
            this.mIconFilename = str;
            this.mIcon = null;
        }

        public String getLabel() {
            return this.mLabel;
        }

        public Bitmap getIcon() {
            if (this.mIcon != null) {
                return this.mIcon;
            }
            return loadTaskDescriptionIcon(this.mIconFilename, UserHandle.myUserId());
        }

        public int getIconResource() {
            return this.mIconRes;
        }

        public String getIconFilename() {
            return this.mIconFilename;
        }

        public Bitmap getInMemoryIcon() {
            return this.mIcon;
        }

        public static Bitmap loadTaskDescriptionIcon(String str, int i) {
            if (str != null) {
                try {
                    return ActivityManager.getService().getTaskDescriptionIcon(str, i);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            return null;
        }

        public int getPrimaryColor() {
            return this.mColorPrimary;
        }

        public int getBackgroundColor() {
            return this.mColorBackground;
        }

        public int getStatusBarColor() {
            return this.mStatusBarColor;
        }

        public int getNavigationBarColor() {
            return this.mNavigationBarColor;
        }

        public void saveToXml(XmlSerializer xmlSerializer) throws IOException {
            if (this.mLabel != null) {
                xmlSerializer.attribute(null, ATTR_TASKDESCRIPTIONLABEL, this.mLabel);
            }
            if (this.mColorPrimary != 0) {
                xmlSerializer.attribute(null, ATTR_TASKDESCRIPTIONCOLOR_PRIMARY, Integer.toHexString(this.mColorPrimary));
            }
            if (this.mColorBackground != 0) {
                xmlSerializer.attribute(null, ATTR_TASKDESCRIPTIONCOLOR_BACKGROUND, Integer.toHexString(this.mColorBackground));
            }
            if (this.mIconFilename != null) {
                xmlSerializer.attribute(null, ATTR_TASKDESCRIPTIONICON_FILENAME, this.mIconFilename);
            }
            if (this.mIconRes != 0) {
                xmlSerializer.attribute(null, ATTR_TASKDESCRIPTIONICON_RESOURCE, Integer.toString(this.mIconRes));
            }
        }

        public void restoreFromXml(String str, String str2) {
            if (ATTR_TASKDESCRIPTIONLABEL.equals(str)) {
                setLabel(str2);
                return;
            }
            if (ATTR_TASKDESCRIPTIONCOLOR_PRIMARY.equals(str)) {
                setPrimaryColor((int) Long.parseLong(str2, 16));
                return;
            }
            if (ATTR_TASKDESCRIPTIONCOLOR_BACKGROUND.equals(str)) {
                setBackgroundColor((int) Long.parseLong(str2, 16));
            } else if (ATTR_TASKDESCRIPTIONICON_FILENAME.equals(str)) {
                setIconFilename(str2);
            } else if (ATTR_TASKDESCRIPTIONICON_RESOURCE.equals(str)) {
                setIcon(Integer.parseInt(str2, 10));
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            if (this.mLabel == null) {
                parcel.writeInt(0);
            } else {
                parcel.writeInt(1);
                parcel.writeString(this.mLabel);
            }
            if (this.mIcon == null) {
                parcel.writeInt(0);
            } else {
                parcel.writeInt(1);
                this.mIcon.writeToParcel(parcel, 0);
            }
            parcel.writeInt(this.mIconRes);
            parcel.writeInt(this.mColorPrimary);
            parcel.writeInt(this.mColorBackground);
            parcel.writeInt(this.mStatusBarColor);
            parcel.writeInt(this.mNavigationBarColor);
            if (this.mIconFilename == null) {
                parcel.writeInt(0);
            } else {
                parcel.writeInt(1);
                parcel.writeString(this.mIconFilename);
            }
        }

        public void readFromParcel(Parcel parcel) {
            this.mLabel = parcel.readInt() > 0 ? parcel.readString() : null;
            this.mIcon = parcel.readInt() > 0 ? Bitmap.CREATOR.createFromParcel(parcel) : null;
            this.mIconRes = parcel.readInt();
            this.mColorPrimary = parcel.readInt();
            this.mColorBackground = parcel.readInt();
            this.mStatusBarColor = parcel.readInt();
            this.mNavigationBarColor = parcel.readInt();
            this.mIconFilename = parcel.readInt() > 0 ? parcel.readString() : null;
        }

        public String toString() {
            return "TaskDescription Label: " + this.mLabel + " Icon: " + this.mIcon + " IconRes: " + this.mIconRes + " IconFilename: " + this.mIconFilename + " colorPrimary: " + this.mColorPrimary + " colorBackground: " + this.mColorBackground + " statusBarColor: " + this.mColorBackground + " navigationBarColor: " + this.mNavigationBarColor;
        }
    }

    public static class RecentTaskInfo implements Parcelable {
        public static final Parcelable.Creator<RecentTaskInfo> CREATOR = new Parcelable.Creator<RecentTaskInfo>() {
            @Override
            public RecentTaskInfo createFromParcel(Parcel parcel) {
                return new RecentTaskInfo(parcel);
            }

            @Override
            public RecentTaskInfo[] newArray(int i) {
                return new RecentTaskInfo[i];
            }
        };
        public int affiliatedTaskColor;
        public int affiliatedTaskId;
        public ComponentName baseActivity;
        public Intent baseIntent;
        public Rect bounds;
        public final Configuration configuration;
        public CharSequence description;
        public long firstActiveTime;
        public int id;
        public long lastActiveTime;
        public int numActivities;
        public ComponentName origActivity;
        public int persistentId;
        public ComponentName realActivity;
        public int resizeMode;
        public int stackId;
        public boolean supportsSplitScreenMultiWindow;
        public TaskDescription taskDescription;
        public ComponentName topActivity;
        public int userId;

        public RecentTaskInfo() {
            this.configuration = new Configuration();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.id);
            parcel.writeInt(this.persistentId);
            if (this.baseIntent != null) {
                parcel.writeInt(1);
                this.baseIntent.writeToParcel(parcel, 0);
            } else {
                parcel.writeInt(0);
            }
            ComponentName.writeToParcel(this.origActivity, parcel);
            ComponentName.writeToParcel(this.realActivity, parcel);
            TextUtils.writeToParcel(this.description, parcel, 1);
            if (this.taskDescription != null) {
                parcel.writeInt(1);
                this.taskDescription.writeToParcel(parcel, 0);
            } else {
                parcel.writeInt(0);
            }
            parcel.writeInt(this.stackId);
            parcel.writeInt(this.userId);
            parcel.writeLong(this.lastActiveTime);
            parcel.writeInt(this.affiliatedTaskId);
            parcel.writeInt(this.affiliatedTaskColor);
            ComponentName.writeToParcel(this.baseActivity, parcel);
            ComponentName.writeToParcel(this.topActivity, parcel);
            parcel.writeInt(this.numActivities);
            if (this.bounds != null) {
                parcel.writeInt(1);
                this.bounds.writeToParcel(parcel, 0);
            } else {
                parcel.writeInt(0);
            }
            parcel.writeInt(this.supportsSplitScreenMultiWindow ? 1 : 0);
            parcel.writeInt(this.resizeMode);
            this.configuration.writeToParcel(parcel, i);
        }

        public void readFromParcel(Parcel parcel) {
            this.id = parcel.readInt();
            this.persistentId = parcel.readInt();
            this.baseIntent = parcel.readInt() > 0 ? Intent.CREATOR.createFromParcel(parcel) : null;
            this.origActivity = ComponentName.readFromParcel(parcel);
            this.realActivity = ComponentName.readFromParcel(parcel);
            this.description = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            this.taskDescription = parcel.readInt() > 0 ? TaskDescription.CREATOR.createFromParcel(parcel) : null;
            this.stackId = parcel.readInt();
            this.userId = parcel.readInt();
            this.lastActiveTime = parcel.readLong();
            this.affiliatedTaskId = parcel.readInt();
            this.affiliatedTaskColor = parcel.readInt();
            this.baseActivity = ComponentName.readFromParcel(parcel);
            this.topActivity = ComponentName.readFromParcel(parcel);
            this.numActivities = parcel.readInt();
            this.bounds = parcel.readInt() > 0 ? Rect.CREATOR.createFromParcel(parcel) : null;
            this.supportsSplitScreenMultiWindow = parcel.readInt() == 1;
            this.resizeMode = parcel.readInt();
            this.configuration.readFromParcel(parcel);
        }

        private RecentTaskInfo(Parcel parcel) {
            this.configuration = new Configuration();
            readFromParcel(parcel);
        }
    }

    @Deprecated
    public List<RecentTaskInfo> getRecentTasks(int i, int i2) throws SecurityException {
        try {
            if (i < 0) {
                throw new IllegalArgumentException("The requested number of tasks should be >= 0");
            }
            return getService().getRecentTasks(i, i2, this.mContext.getUserId()).getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static class RunningTaskInfo implements Parcelable {
        public static final Parcelable.Creator<RunningTaskInfo> CREATOR = new Parcelable.Creator<RunningTaskInfo>() {
            @Override
            public RunningTaskInfo createFromParcel(Parcel parcel) {
                return new RunningTaskInfo(parcel);
            }

            @Override
            public RunningTaskInfo[] newArray(int i) {
                return new RunningTaskInfo[i];
            }
        };
        public ComponentName baseActivity;
        public final Configuration configuration;
        public CharSequence description;
        public int id;
        public long lastActiveTime;
        public int numActivities;
        public int numRunning;
        public int resizeMode;
        public int stackId;
        public boolean supportsSplitScreenMultiWindow;
        public Bitmap thumbnail;
        public ComponentName topActivity;

        public RunningTaskInfo() {
            this.configuration = new Configuration();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.id);
            parcel.writeInt(this.stackId);
            ComponentName.writeToParcel(this.baseActivity, parcel);
            ComponentName.writeToParcel(this.topActivity, parcel);
            if (this.thumbnail != null) {
                parcel.writeInt(1);
                this.thumbnail.writeToParcel(parcel, 0);
            } else {
                parcel.writeInt(0);
            }
            TextUtils.writeToParcel(this.description, parcel, 1);
            parcel.writeInt(this.numActivities);
            parcel.writeInt(this.numRunning);
            parcel.writeInt(this.supportsSplitScreenMultiWindow ? 1 : 0);
            parcel.writeInt(this.resizeMode);
            this.configuration.writeToParcel(parcel, i);
        }

        public void readFromParcel(Parcel parcel) {
            this.id = parcel.readInt();
            this.stackId = parcel.readInt();
            this.baseActivity = ComponentName.readFromParcel(parcel);
            this.topActivity = ComponentName.readFromParcel(parcel);
            if (parcel.readInt() != 0) {
                this.thumbnail = Bitmap.CREATOR.createFromParcel(parcel);
            } else {
                this.thumbnail = null;
            }
            this.description = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
            this.numActivities = parcel.readInt();
            this.numRunning = parcel.readInt();
            this.supportsSplitScreenMultiWindow = parcel.readInt() != 0;
            this.resizeMode = parcel.readInt();
            this.configuration.readFromParcel(parcel);
        }

        private RunningTaskInfo(Parcel parcel) {
            this.configuration = new Configuration();
            readFromParcel(parcel);
        }
    }

    public List<AppTask> getAppTasks() {
        ArrayList arrayList = new ArrayList();
        try {
            List<IBinder> appTasks = getService().getAppTasks(this.mContext.getPackageName());
            int size = appTasks.size();
            for (int i = 0; i < size; i++) {
                arrayList.add(new AppTask(IAppTask.Stub.asInterface(appTasks.get(i))));
            }
            return arrayList;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Size getAppTaskThumbnailSize() {
        Size size;
        synchronized (this) {
            ensureAppTaskThumbnailSizeLocked();
            size = new Size(this.mAppTaskThumbnailSize.x, this.mAppTaskThumbnailSize.y);
        }
        return size;
    }

    private void ensureAppTaskThumbnailSizeLocked() {
        if (this.mAppTaskThumbnailSize == null) {
            try {
                this.mAppTaskThumbnailSize = getService().getAppTaskThumbnailSize();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int addAppTask(Activity activity, Intent intent, TaskDescription taskDescription, Bitmap bitmap) {
        Point point;
        float f;
        float f2;
        synchronized (this) {
            ensureAppTaskThumbnailSizeLocked();
            point = this.mAppTaskThumbnailSize;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width != point.x || height != point.y) {
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(point.x, point.y, bitmap.getConfig());
            if (point.x * width > point.y * height) {
                f = point.x / height;
                f2 = (point.y - (width * f)) * 0.5f;
            } else {
                f = point.y / width;
                int i = point.x;
                f2 = 0.0f;
            }
            Matrix matrix = new Matrix();
            matrix.setScale(f, f);
            matrix.postTranslate((int) (f2 + 0.5f), 0.0f);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            canvas.drawBitmap(bitmap, matrix, null);
            canvas.setBitmap(null);
            bitmap = bitmapCreateBitmap;
        }
        if (taskDescription == null) {
            taskDescription = new TaskDescription();
        }
        try {
            return getService().addAppTask(activity.getActivityToken(), intent, taskDescription, bitmap);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public List<RunningTaskInfo> getRunningTasks(int i) throws SecurityException {
        try {
            return getService().getTasks(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTaskWindowingMode(int i, int i2, boolean z) throws SecurityException {
        try {
            getService().setTaskWindowingMode(i, i2, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTaskWindowingModeSplitScreenPrimary(int i, int i2, boolean z, boolean z2, Rect rect, boolean z3) throws SecurityException {
        try {
            getService().setTaskWindowingModeSplitScreenPrimary(i, i2, z, z2, rect, z3);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void resizeStack(int i, Rect rect) throws SecurityException {
        try {
            getService().resizeStack(i, rect, false, false, false, -1);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeStacksInWindowingModes(int[] iArr) throws SecurityException {
        try {
            getService().removeStacksInWindowingModes(iArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeStacksWithActivityTypes(int[] iArr) throws SecurityException {
        try {
            getService().removeStacksWithActivityTypes(iArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static class TaskSnapshot implements Parcelable {
        public static final Parcelable.Creator<TaskSnapshot> CREATOR = new Parcelable.Creator<TaskSnapshot>() {
            @Override
            public TaskSnapshot createFromParcel(Parcel parcel) {
                return new TaskSnapshot(parcel);
            }

            @Override
            public TaskSnapshot[] newArray(int i) {
                return new TaskSnapshot[i];
            }
        };
        private final Rect mContentInsets;
        private final boolean mIsRealSnapshot;
        private final boolean mIsTranslucent;
        private final int mOrientation;
        private final boolean mReducedResolution;
        private final float mScale;
        private final GraphicBuffer mSnapshot;
        private final int mSystemUiVisibility;
        private final int mWindowingMode;

        public TaskSnapshot(GraphicBuffer graphicBuffer, int i, Rect rect, boolean z, float f, boolean z2, int i2, int i3, boolean z3) {
            this.mSnapshot = graphicBuffer;
            this.mOrientation = i;
            this.mContentInsets = new Rect(rect);
            this.mReducedResolution = z;
            this.mScale = f;
            this.mIsRealSnapshot = z2;
            this.mWindowingMode = i2;
            this.mSystemUiVisibility = i3;
            this.mIsTranslucent = z3;
        }

        private TaskSnapshot(Parcel parcel) {
            this.mSnapshot = (GraphicBuffer) parcel.readParcelable(null);
            this.mOrientation = parcel.readInt();
            this.mContentInsets = (Rect) parcel.readParcelable(null);
            this.mReducedResolution = parcel.readBoolean();
            this.mScale = parcel.readFloat();
            this.mIsRealSnapshot = parcel.readBoolean();
            this.mWindowingMode = parcel.readInt();
            this.mSystemUiVisibility = parcel.readInt();
            this.mIsTranslucent = parcel.readBoolean();
        }

        public GraphicBuffer getSnapshot() {
            return this.mSnapshot;
        }

        public int getOrientation() {
            return this.mOrientation;
        }

        public Rect getContentInsets() {
            return this.mContentInsets;
        }

        public boolean isReducedResolution() {
            return this.mReducedResolution;
        }

        public boolean isRealSnapshot() {
            return this.mIsRealSnapshot;
        }

        public boolean isTranslucent() {
            return this.mIsTranslucent;
        }

        public int getWindowingMode() {
            return this.mWindowingMode;
        }

        public int getSystemUiVisibility() {
            return this.mSystemUiVisibility;
        }

        public float getScale() {
            return this.mScale;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeParcelable(this.mSnapshot, 0);
            parcel.writeInt(this.mOrientation);
            parcel.writeParcelable(this.mContentInsets, 0);
            parcel.writeBoolean(this.mReducedResolution);
            parcel.writeFloat(this.mScale);
            parcel.writeBoolean(this.mIsRealSnapshot);
            parcel.writeInt(this.mWindowingMode);
            parcel.writeInt(this.mSystemUiVisibility);
            parcel.writeBoolean(this.mIsTranslucent);
        }

        public String toString() {
            return "TaskSnapshot{mSnapshot=" + this.mSnapshot + " (" + (this.mSnapshot != null ? this.mSnapshot.getWidth() : 0) + "x" + (this.mSnapshot != null ? this.mSnapshot.getHeight() : 0) + ") mOrientation=" + this.mOrientation + " mContentInsets=" + this.mContentInsets.toShortString() + " mReducedResolution=" + this.mReducedResolution + " mScale=" + this.mScale + " mIsRealSnapshot=" + this.mIsRealSnapshot + " mWindowingMode=" + this.mWindowingMode + " mSystemUiVisibility=" + this.mSystemUiVisibility + " mIsTranslucent=" + this.mIsTranslucent;
        }
    }

    public void moveTaskToFront(int i, int i2) {
        moveTaskToFront(i, i2, null);
    }

    public void moveTaskToFront(int i, int i2, Bundle bundle) {
        try {
            getService().moveTaskToFront(i, i2, bundle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static class RunningServiceInfo implements Parcelable {
        public static final Parcelable.Creator<RunningServiceInfo> CREATOR = new Parcelable.Creator<RunningServiceInfo>() {
            @Override
            public RunningServiceInfo createFromParcel(Parcel parcel) {
                return new RunningServiceInfo(parcel);
            }

            @Override
            public RunningServiceInfo[] newArray(int i) {
                return new RunningServiceInfo[i];
            }
        };
        public static final int FLAG_FOREGROUND = 2;
        public static final int FLAG_PERSISTENT_PROCESS = 8;
        public static final int FLAG_STARTED = 1;
        public static final int FLAG_SYSTEM_PROCESS = 4;
        public long activeSince;
        public int clientCount;
        public int clientLabel;
        public String clientPackage;
        public int crashCount;
        public int flags;
        public boolean foreground;
        public long lastActivityTime;
        public int pid;
        public String process;
        public long restarting;
        public ComponentName service;
        public boolean started;
        public int uid;

        public RunningServiceInfo() {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            ComponentName.writeToParcel(this.service, parcel);
            parcel.writeInt(this.pid);
            parcel.writeInt(this.uid);
            parcel.writeString(this.process);
            parcel.writeInt(this.foreground ? 1 : 0);
            parcel.writeLong(this.activeSince);
            parcel.writeInt(this.started ? 1 : 0);
            parcel.writeInt(this.clientCount);
            parcel.writeInt(this.crashCount);
            parcel.writeLong(this.lastActivityTime);
            parcel.writeLong(this.restarting);
            parcel.writeInt(this.flags);
            parcel.writeString(this.clientPackage);
            parcel.writeInt(this.clientLabel);
        }

        public void readFromParcel(Parcel parcel) {
            this.service = ComponentName.readFromParcel(parcel);
            this.pid = parcel.readInt();
            this.uid = parcel.readInt();
            this.process = parcel.readString();
            this.foreground = parcel.readInt() != 0;
            this.activeSince = parcel.readLong();
            this.started = parcel.readInt() != 0;
            this.clientCount = parcel.readInt();
            this.crashCount = parcel.readInt();
            this.lastActivityTime = parcel.readLong();
            this.restarting = parcel.readLong();
            this.flags = parcel.readInt();
            this.clientPackage = parcel.readString();
            this.clientLabel = parcel.readInt();
        }

        private RunningServiceInfo(Parcel parcel) {
            readFromParcel(parcel);
        }
    }

    @Deprecated
    public List<RunningServiceInfo> getRunningServices(int i) throws SecurityException {
        try {
            return getService().getServices(i, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public PendingIntent getRunningServiceControlPanel(ComponentName componentName) throws SecurityException {
        try {
            return getService().getRunningServiceControlPanel(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static class MemoryInfo implements Parcelable {
        public static final Parcelable.Creator<MemoryInfo> CREATOR = new Parcelable.Creator<MemoryInfo>() {
            @Override
            public MemoryInfo createFromParcel(Parcel parcel) {
                return new MemoryInfo(parcel);
            }

            @Override
            public MemoryInfo[] newArray(int i) {
                return new MemoryInfo[i];
            }
        };
        public long availMem;
        public long foregroundAppThreshold;
        public long hiddenAppThreshold;
        public boolean lowMemory;
        public long secondaryServerThreshold;
        public long threshold;
        public long totalMem;
        public long visibleAppThreshold;

        public MemoryInfo() {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(this.availMem);
            parcel.writeLong(this.totalMem);
            parcel.writeLong(this.threshold);
            parcel.writeInt(this.lowMemory ? 1 : 0);
            parcel.writeLong(this.hiddenAppThreshold);
            parcel.writeLong(this.secondaryServerThreshold);
            parcel.writeLong(this.visibleAppThreshold);
            parcel.writeLong(this.foregroundAppThreshold);
        }

        public void readFromParcel(Parcel parcel) {
            this.availMem = parcel.readLong();
            this.totalMem = parcel.readLong();
            this.threshold = parcel.readLong();
            this.lowMemory = parcel.readInt() != 0;
            this.hiddenAppThreshold = parcel.readLong();
            this.secondaryServerThreshold = parcel.readLong();
            this.visibleAppThreshold = parcel.readLong();
            this.foregroundAppThreshold = parcel.readLong();
        }

        private MemoryInfo(Parcel parcel) {
            readFromParcel(parcel);
        }
    }

    public void getMemoryInfo(MemoryInfo memoryInfo) {
        try {
            getService().getMemoryInfo(memoryInfo);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static class StackInfo implements Parcelable {
        public static final Parcelable.Creator<StackInfo> CREATOR = new Parcelable.Creator<StackInfo>() {
            @Override
            public StackInfo createFromParcel(Parcel parcel) {
                return new StackInfo(parcel);
            }

            @Override
            public StackInfo[] newArray(int i) {
                return new StackInfo[i];
            }
        };
        public Rect bounds;
        public final Configuration configuration;
        public int displayId;
        public int position;
        public int stackId;
        public Rect[] taskBounds;
        public int[] taskIds;
        public String[] taskNames;
        public int[] taskUserIds;
        public ComponentName topActivity;
        public int userId;
        public boolean visible;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            int length;
            parcel.writeInt(this.stackId);
            parcel.writeInt(this.bounds.left);
            parcel.writeInt(this.bounds.top);
            parcel.writeInt(this.bounds.right);
            parcel.writeInt(this.bounds.bottom);
            parcel.writeIntArray(this.taskIds);
            parcel.writeStringArray(this.taskNames);
            if (this.taskBounds != null) {
                length = this.taskBounds.length;
            } else {
                length = 0;
            }
            parcel.writeInt(length);
            for (int i2 = 0; i2 < length; i2++) {
                parcel.writeInt(this.taskBounds[i2].left);
                parcel.writeInt(this.taskBounds[i2].top);
                parcel.writeInt(this.taskBounds[i2].right);
                parcel.writeInt(this.taskBounds[i2].bottom);
            }
            parcel.writeIntArray(this.taskUserIds);
            parcel.writeInt(this.displayId);
            parcel.writeInt(this.userId);
            parcel.writeInt(this.visible ? 1 : 0);
            parcel.writeInt(this.position);
            if (this.topActivity != null) {
                parcel.writeInt(1);
                this.topActivity.writeToParcel(parcel, 0);
            } else {
                parcel.writeInt(0);
            }
            this.configuration.writeToParcel(parcel, i);
        }

        public void readFromParcel(Parcel parcel) {
            this.stackId = parcel.readInt();
            this.bounds = new Rect(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
            this.taskIds = parcel.createIntArray();
            this.taskNames = parcel.createStringArray();
            int i = parcel.readInt();
            if (i > 0) {
                this.taskBounds = new Rect[i];
                for (int i2 = 0; i2 < i; i2++) {
                    this.taskBounds[i2] = new Rect();
                    this.taskBounds[i2].set(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                }
            } else {
                this.taskBounds = null;
            }
            this.taskUserIds = parcel.createIntArray();
            this.displayId = parcel.readInt();
            this.userId = parcel.readInt();
            this.visible = parcel.readInt() > 0;
            this.position = parcel.readInt();
            if (parcel.readInt() > 0) {
                this.topActivity = ComponentName.readFromParcel(parcel);
            }
            this.configuration.readFromParcel(parcel);
        }

        public StackInfo() {
            this.bounds = new Rect();
            this.configuration = new Configuration();
        }

        private StackInfo(Parcel parcel) {
            this.bounds = new Rect();
            this.configuration = new Configuration();
            readFromParcel(parcel);
        }

        public String toString(String str) {
            StringBuilder sb = new StringBuilder(256);
            sb.append(str);
            sb.append("Stack id=");
            sb.append(this.stackId);
            sb.append(" bounds=");
            sb.append(this.bounds.toShortString());
            sb.append(" displayId=");
            sb.append(this.displayId);
            sb.append(" userId=");
            sb.append(this.userId);
            sb.append("\n");
            sb.append(" configuration=");
            sb.append(this.configuration);
            sb.append("\n");
            String str2 = str + "  ";
            for (int i = 0; i < this.taskIds.length; i++) {
                sb.append(str2);
                sb.append("taskId=");
                sb.append(this.taskIds[i]);
                sb.append(": ");
                sb.append(this.taskNames[i]);
                if (this.taskBounds != null) {
                    sb.append(" bounds=");
                    sb.append(this.taskBounds[i].toShortString());
                }
                sb.append(" userId=");
                sb.append(this.taskUserIds[i]);
                sb.append(" visible=");
                sb.append(this.visible);
                if (this.topActivity != null) {
                    sb.append(" topActivity=");
                    sb.append(this.topActivity);
                }
                sb.append("\n");
            }
            return sb.toString();
        }

        public String toString() {
            return toString("");
        }
    }

    public boolean clearApplicationUserData(String str, IPackageDataObserver iPackageDataObserver) {
        try {
            return getService().clearApplicationUserData(str, false, iPackageDataObserver, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean clearApplicationUserData() {
        return clearApplicationUserData(this.mContext.getPackageName(), null);
    }

    public ParceledListSlice<GrantedUriPermission> getGrantedUriPermissions(String str) {
        try {
            return getService().getGrantedUriPermissions(str, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearGrantedUriPermissions(String str) {
        try {
            getService().clearGrantedUriPermissions(str, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static class ProcessErrorStateInfo implements Parcelable {
        public static final int CRASHED = 1;
        public static final Parcelable.Creator<ProcessErrorStateInfo> CREATOR = new Parcelable.Creator<ProcessErrorStateInfo>() {
            @Override
            public ProcessErrorStateInfo createFromParcel(Parcel parcel) {
                return new ProcessErrorStateInfo(parcel);
            }

            @Override
            public ProcessErrorStateInfo[] newArray(int i) {
                return new ProcessErrorStateInfo[i];
            }
        };
        public static final int NOT_RESPONDING = 2;
        public static final int NO_ERROR = 0;
        public int condition;
        public byte[] crashData;
        public String longMsg;
        public int pid;
        public String processName;
        public String shortMsg;
        public String stackTrace;
        public String tag;
        public int uid;

        public ProcessErrorStateInfo() {
            this.crashData = null;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.condition);
            parcel.writeString(this.processName);
            parcel.writeInt(this.pid);
            parcel.writeInt(this.uid);
            parcel.writeString(this.tag);
            parcel.writeString(this.shortMsg);
            parcel.writeString(this.longMsg);
            parcel.writeString(this.stackTrace);
        }

        public void readFromParcel(Parcel parcel) {
            this.condition = parcel.readInt();
            this.processName = parcel.readString();
            this.pid = parcel.readInt();
            this.uid = parcel.readInt();
            this.tag = parcel.readString();
            this.shortMsg = parcel.readString();
            this.longMsg = parcel.readString();
            this.stackTrace = parcel.readString();
        }

        private ProcessErrorStateInfo(Parcel parcel) {
            this.crashData = null;
            readFromParcel(parcel);
        }
    }

    public List<ProcessErrorStateInfo> getProcessesInErrorState() {
        try {
            return getService().getProcessesInErrorState();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static class RunningAppProcessInfo implements Parcelable {
        public static final Parcelable.Creator<RunningAppProcessInfo> CREATOR = new Parcelable.Creator<RunningAppProcessInfo>() {
            @Override
            public RunningAppProcessInfo createFromParcel(Parcel parcel) {
                return new RunningAppProcessInfo(parcel);
            }

            @Override
            public RunningAppProcessInfo[] newArray(int i) {
                return new RunningAppProcessInfo[i];
            }
        };
        public static final int FLAG_CANT_SAVE_STATE = 1;
        public static final int FLAG_HAS_ACTIVITIES = 4;
        public static final int FLAG_PERSISTENT = 2;
        public static final int IMPORTANCE_BACKGROUND = 400;
        public static final int IMPORTANCE_CACHED = 400;
        public static final int IMPORTANCE_CANT_SAVE_STATE = 350;
        public static final int IMPORTANCE_CANT_SAVE_STATE_PRE_26 = 170;

        @Deprecated
        public static final int IMPORTANCE_EMPTY = 500;
        public static final int IMPORTANCE_FOREGROUND = 100;
        public static final int IMPORTANCE_FOREGROUND_SERVICE = 125;
        public static final int IMPORTANCE_GONE = 1000;
        public static final int IMPORTANCE_PERCEPTIBLE = 230;
        public static final int IMPORTANCE_PERCEPTIBLE_PRE_26 = 130;
        public static final int IMPORTANCE_SERVICE = 300;
        public static final int IMPORTANCE_TOP_SLEEPING = 325;

        @Deprecated
        public static final int IMPORTANCE_TOP_SLEEPING_PRE_28 = 150;
        public static final int IMPORTANCE_VISIBLE = 200;
        public static final int REASON_PROVIDER_IN_USE = 1;
        public static final int REASON_SERVICE_IN_USE = 2;
        public static final int REASON_UNKNOWN = 0;
        public int flags;
        public int importance;
        public int importanceReasonCode;
        public ComponentName importanceReasonComponent;
        public int importanceReasonImportance;
        public int importanceReasonPid;
        public int lastTrimLevel;
        public int lru;
        public int pid;
        public String[] pkgList;
        public String processName;
        public int processState;
        public int uid;

        @Retention(RetentionPolicy.SOURCE)
        public @interface Importance {
        }

        public static int procStateToImportance(int i) {
            if (i == 19) {
                return 1000;
            }
            if (i >= 13) {
                return 400;
            }
            if (i == 12) {
                return 350;
            }
            if (i >= 11) {
                return 325;
            }
            if (i >= 9) {
                return 300;
            }
            if (i >= 7) {
                return 230;
            }
            if (i >= 5) {
                return 200;
            }
            if (i >= 3) {
                return 125;
            }
            return 100;
        }

        public static int procStateToImportanceForClient(int i, Context context) {
            return procStateToImportanceForTargetSdk(i, context.getApplicationInfo().targetSdkVersion);
        }

        public static int procStateToImportanceForTargetSdk(int i, int i2) {
            int iProcStateToImportance = procStateToImportance(i);
            if (i2 < 26) {
                if (iProcStateToImportance == 230) {
                    return 130;
                }
                if (iProcStateToImportance == 325) {
                    return 150;
                }
                if (iProcStateToImportance == 350) {
                    return 170;
                }
            }
            return iProcStateToImportance;
        }

        public static int importanceToProcState(int i) {
            if (i == 1000) {
                return 19;
            }
            if (i >= 400) {
                return 13;
            }
            if (i >= 350) {
                return 12;
            }
            if (i >= 325) {
                return 11;
            }
            if (i >= 300) {
                return 9;
            }
            if (i >= 230) {
                return 7;
            }
            if (i >= 200 || i >= 150) {
                return 5;
            }
            if (i >= 125) {
                return 3;
            }
            return 2;
        }

        public RunningAppProcessInfo() {
            this.importance = 100;
            this.importanceReasonCode = 0;
            this.processState = 5;
        }

        public RunningAppProcessInfo(String str, int i, String[] strArr) {
            this.processName = str;
            this.pid = i;
            this.pkgList = strArr;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.processName);
            parcel.writeInt(this.pid);
            parcel.writeInt(this.uid);
            parcel.writeStringArray(this.pkgList);
            parcel.writeInt(this.flags);
            parcel.writeInt(this.lastTrimLevel);
            parcel.writeInt(this.importance);
            parcel.writeInt(this.lru);
            parcel.writeInt(this.importanceReasonCode);
            parcel.writeInt(this.importanceReasonPid);
            ComponentName.writeToParcel(this.importanceReasonComponent, parcel);
            parcel.writeInt(this.importanceReasonImportance);
            parcel.writeInt(this.processState);
        }

        public void readFromParcel(Parcel parcel) {
            this.processName = parcel.readString();
            this.pid = parcel.readInt();
            this.uid = parcel.readInt();
            this.pkgList = parcel.readStringArray();
            this.flags = parcel.readInt();
            this.lastTrimLevel = parcel.readInt();
            this.importance = parcel.readInt();
            this.lru = parcel.readInt();
            this.importanceReasonCode = parcel.readInt();
            this.importanceReasonPid = parcel.readInt();
            this.importanceReasonComponent = ComponentName.readFromParcel(parcel);
            this.importanceReasonImportance = parcel.readInt();
            this.processState = parcel.readInt();
        }

        private RunningAppProcessInfo(Parcel parcel) {
            readFromParcel(parcel);
        }
    }

    public List<ApplicationInfo> getRunningExternalApplications() {
        try {
            return getService().getRunningExternalApplications();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isBackgroundRestricted() {
        try {
            return getService().isBackgroundRestricted(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setProcessMemoryTrimLevel(String str, int i, int i2) {
        try {
            return getService().setProcessMemoryTrimLevel(str, i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<RunningAppProcessInfo> getRunningAppProcesses() {
        try {
            return getService().getRunningAppProcesses();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int getPackageImportance(String str) {
        try {
            return RunningAppProcessInfo.procStateToImportanceForClient(getService().getPackageProcessState(str, this.mContext.getOpPackageName()), this.mContext);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int getUidImportance(int i) {
        try {
            return RunningAppProcessInfo.procStateToImportanceForClient(getService().getUidProcessState(i, this.mContext.getOpPackageName()), this.mContext);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void addOnUidImportanceListener(OnUidImportanceListener onUidImportanceListener, int i) {
        synchronized (this) {
            if (this.mImportanceListeners.containsKey(onUidImportanceListener)) {
                throw new IllegalArgumentException("Listener already registered: " + onUidImportanceListener);
            }
            UidObserver uidObserver = new UidObserver(onUidImportanceListener, this.mContext);
            try {
                getService().registerUidObserver(uidObserver, 3, RunningAppProcessInfo.importanceToProcState(i), this.mContext.getOpPackageName());
                this.mImportanceListeners.put(onUidImportanceListener, uidObserver);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @SystemApi
    public void removeOnUidImportanceListener(OnUidImportanceListener onUidImportanceListener) {
        synchronized (this) {
            UidObserver uidObserverRemove = this.mImportanceListeners.remove(onUidImportanceListener);
            if (uidObserverRemove == null) {
                throw new IllegalArgumentException("Listener not registered: " + onUidImportanceListener);
            }
            try {
                getService().unregisterUidObserver(uidObserverRemove);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public static void getMyMemoryState(RunningAppProcessInfo runningAppProcessInfo) {
        try {
            getService().getMyMemoryState(runningAppProcessInfo);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Debug.MemoryInfo[] getProcessMemoryInfo(int[] iArr) {
        try {
            return getService().getProcessMemoryInfo(iArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void restartPackage(String str) {
        killBackgroundProcesses(str);
    }

    public void killBackgroundProcesses(String str) {
        try {
            getService().killBackgroundProcesses(str, this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void killUid(int i, String str) {
        try {
            getService().killUid(UserHandle.getAppId(i), UserHandle.getUserId(i), str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void forceStopPackageAsUser(String str, int i) {
        try {
            getService().forceStopPackage(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void forceStopPackage(String str) {
        forceStopPackageAsUser(str, this.mContext.getUserId());
    }

    public ConfigurationInfo getDeviceConfigurationInfo() {
        try {
            return getService().getDeviceConfigurationInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getLauncherLargeIconDensity() {
        Resources resources = this.mContext.getResources();
        int i = resources.getDisplayMetrics().densityDpi;
        if (resources.getConfiguration().smallestScreenWidthDp < 600) {
            return i;
        }
        if (i == 120) {
            return 160;
        }
        if (i == 160) {
            return 240;
        }
        if (i == 213 || i == 240) {
            return 320;
        }
        if (i == 320) {
            return 480;
        }
        if (i == 480) {
            return 640;
        }
        return (int) ((i * 1.5f) + 0.5f);
    }

    public int getLauncherLargeIconSize() {
        return getLauncherLargeIconSizeInner(this.mContext);
    }

    static int getLauncherLargeIconSizeInner(Context context) {
        Resources resources = context.getResources();
        int dimensionPixelSize = resources.getDimensionPixelSize(17104896);
        if (resources.getConfiguration().smallestScreenWidthDp < 600) {
            return dimensionPixelSize;
        }
        int i = resources.getDisplayMetrics().densityDpi;
        if (i == 120) {
            return (dimensionPixelSize * 160) / 120;
        }
        if (i == 160) {
            return (dimensionPixelSize * 240) / 160;
        }
        if (i == 213) {
            return (dimensionPixelSize * 320) / 240;
        }
        if (i == 240) {
            return (dimensionPixelSize * 320) / 240;
        }
        if (i == 320) {
            return (dimensionPixelSize * 480) / 320;
        }
        if (i == 480) {
            return ((dimensionPixelSize * 320) * 2) / 480;
        }
        return (int) ((dimensionPixelSize * 1.5f) + 0.5f);
    }

    public static boolean isUserAMonkey() {
        try {
            return getService().isUserAMonkey();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static boolean isRunningInTestHarness() {
        return SystemProperties.getBoolean("ro.test_harness", false);
    }

    public void alwaysShowUnsupportedCompileSdkWarning(ComponentName componentName) {
        try {
            getService().alwaysShowUnsupportedCompileSdkWarning(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static int checkComponentPermission(String str, int i, int i2, boolean z) {
        int appId = UserHandle.getAppId(i);
        if (appId == 0 || appId == 1000) {
            return 0;
        }
        if (UserHandle.isIsolated(i)) {
            return -1;
        }
        if (i2 >= 0 && UserHandle.isSameApp(i, i2)) {
            return 0;
        }
        if (!z) {
            return -1;
        }
        if (str == null) {
            return 0;
        }
        try {
            return AppGlobals.getPackageManager().checkUidPermission(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static int checkUidPermission(String str, int i) {
        try {
            return AppGlobals.getPackageManager().checkUidPermission(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static int handleIncomingUser(int i, int i2, int i3, boolean z, boolean z2, String str, String str2) {
        if (UserHandle.getUserId(i2) == i3) {
            return i3;
        }
        try {
            return getService().handleIncomingUser(i, i2, i3, z, z2, str, str2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public static int getCurrentUser() {
        try {
            UserInfo currentUser = getService().getCurrentUser();
            if (currentUser != null) {
                return currentUser.id;
            }
            return 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean switchUser(int i) {
        try {
            return getService().switchUser(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static void logoutCurrentUser() {
        int currentUser = getCurrentUser();
        if (currentUser != 0) {
            try {
                getService().switchUser(0);
                getService().stopUser(currentUser, false, null);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
    }

    public boolean isUserRunning(int i) {
        try {
            return getService().isUserRunning(i, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isVrModePackageEnabled(ComponentName componentName) {
        try {
            return getService().isVrModePackageEnabled(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void dumpPackageState(FileDescriptor fileDescriptor, String str) {
        dumpPackageStateStatic(fileDescriptor, str);
    }

    public static void dumpPackageStateStatic(FileDescriptor fileDescriptor, String str) {
        FastPrintWriter fastPrintWriter = new FastPrintWriter(new FileOutputStream(fileDescriptor));
        dumpService(fastPrintWriter, fileDescriptor, "package", new String[]{str});
        fastPrintWriter.println();
        dumpService(fastPrintWriter, fileDescriptor, Context.ACTIVITY_SERVICE, new String[]{"-a", "package", str});
        fastPrintWriter.println();
        dumpService(fastPrintWriter, fileDescriptor, "meminfo", new String[]{"--local", "--package", str});
        fastPrintWriter.println();
        dumpService(fastPrintWriter, fileDescriptor, ProcessStats.SERVICE_NAME, new String[]{str});
        fastPrintWriter.println();
        dumpService(fastPrintWriter, fileDescriptor, Context.USAGE_STATS_SERVICE, new String[]{str});
        fastPrintWriter.println();
        dumpService(fastPrintWriter, fileDescriptor, BatteryStats.SERVICE_NAME, new String[]{str});
        fastPrintWriter.flush();
    }

    public static boolean isSystemReady() {
        if (!sSystemReady) {
            if (ActivityThread.isSystem()) {
                sSystemReady = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).isSystemReady();
            } else {
                sSystemReady = true;
            }
        }
        return sSystemReady;
    }

    public static void broadcastStickyIntent(Intent intent, int i) {
        broadcastStickyIntent(intent, -1, i);
    }

    public static void broadcastStickyIntent(Intent intent, int i, int i2) {
        try {
            getService().broadcastIntent(null, intent, null, null, -1, null, null, null, i, null, false, true, i2);
        } catch (RemoteException e) {
        }
    }

    public static void noteWakeupAlarm(PendingIntent pendingIntent, WorkSource workSource, int i, String str, String str2) {
        try {
            getService().noteWakeupAlarm(pendingIntent != null ? pendingIntent.getTarget() : null, workSource, i, str, str2);
        } catch (RemoteException e) {
        }
    }

    public static void noteAlarmStart(PendingIntent pendingIntent, WorkSource workSource, int i, String str) {
        try {
            getService().noteAlarmStart(pendingIntent != null ? pendingIntent.getTarget() : null, workSource, i, str);
        } catch (RemoteException e) {
        }
    }

    public static void noteAlarmFinish(PendingIntent pendingIntent, WorkSource workSource, int i, String str) {
        try {
            getService().noteAlarmFinish(pendingIntent != null ? pendingIntent.getTarget() : null, workSource, i, str);
        } catch (RemoteException e) {
        }
    }

    public static IActivityManager getService() {
        return IActivityManagerSingleton.get();
    }

    private static void dumpService(PrintWriter printWriter, FileDescriptor fileDescriptor, String str, String[] strArr) {
        TransferPipe transferPipe;
        printWriter.print("DUMP OF SERVICE ");
        printWriter.print(str);
        printWriter.println(SettingsStringUtil.DELIMITER);
        IBinder iBinderCheckService = ServiceManager.checkService(str);
        if (iBinderCheckService == null) {
            printWriter.println("  (Service not found)");
            printWriter.flush();
            return;
        }
        printWriter.flush();
        if (iBinderCheckService instanceof Binder) {
            try {
                iBinderCheckService.dump(fileDescriptor, strArr);
                return;
            } catch (Throwable th) {
                printWriter.println("Failure dumping service:");
                th.printStackTrace(printWriter);
                printWriter.flush();
                return;
            }
        }
        try {
            printWriter.flush();
            transferPipe = new TransferPipe();
            try {
                transferPipe.setBufferPrefix("  ");
                iBinderCheckService.dumpAsync(transferPipe.getWriteFd().getFileDescriptor(), strArr);
                transferPipe.go(fileDescriptor, JobInfo.MIN_BACKOFF_MILLIS);
            } catch (Throwable th2) {
                th = th2;
                if (transferPipe != null) {
                    transferPipe.kill();
                }
                printWriter.println("Failure dumping service:");
                th.printStackTrace(printWriter);
            }
        } catch (Throwable th3) {
            th = th3;
            transferPipe = null;
        }
    }

    public void setWatchHeapLimit(long j) {
        try {
            getService().setDumpHeapDebugLimit(null, 0, j, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearWatchHeapLimit() {
        try {
            getService().setDumpHeapDebugLimit(null, 0, 0L, null);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean isInLockTaskMode() {
        return getLockTaskModeState() != 0;
    }

    public int getLockTaskModeState() {
        try {
            return getService().getLockTaskModeState();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static void setVrThread(int i) {
        try {
            getService().setVrThread(i);
        } catch (RemoteException e) {
        }
    }

    public static void setPersistentVrThread(int i) {
        try {
            getService().setPersistentVrThread(i);
        } catch (RemoteException e) {
        }
    }

    public static class AppTask {
        private IAppTask mAppTaskImpl;

        public AppTask(IAppTask iAppTask) {
            this.mAppTaskImpl = iAppTask;
        }

        public void finishAndRemoveTask() {
            try {
                this.mAppTaskImpl.finishAndRemoveTask();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public RecentTaskInfo getTaskInfo() {
            try {
                return this.mAppTaskImpl.getTaskInfo();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public void moveToFront() {
            try {
                this.mAppTaskImpl.moveToFront();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public void startActivity(Context context, Intent intent, Bundle bundle) {
            ActivityThread activityThreadCurrentActivityThread = ActivityThread.currentActivityThread();
            activityThreadCurrentActivityThread.getInstrumentation().execStartActivityFromAppTask(context, activityThreadCurrentActivityThread.getApplicationThread(), this.mAppTaskImpl, intent, bundle);
        }

        public void setExcludeFromRecents(boolean z) {
            try {
                this.mAppTaskImpl.setExcludeFromRecents(z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void setAalMode(int i) {
        try {
            getService().setAalMode(i);
        } catch (RemoteException e) {
        }
    }

    public void setAalEnabled(boolean z) {
        try {
            getService().setAalEnabled(z);
        } catch (RemoteException e) {
        }
    }
}
