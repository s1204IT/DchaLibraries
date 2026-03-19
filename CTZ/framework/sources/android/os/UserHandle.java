package android.os;

import android.annotation.SystemApi;
import android.os.Parcelable;
import android.provider.Telephony;
import android.text.format.DateFormat;
import java.io.PrintWriter;

public final class UserHandle implements Parcelable {
    public static final int AID_APP_END = 19999;
    public static final int AID_APP_START = 10000;
    public static final int AID_CACHE_GID_START = 20000;
    public static final int AID_ROOT = 0;
    public static final int AID_SHARED_GID_START = 50000;
    public static final int ERR_GID = -1;
    public static final boolean MU_ENABLED = true;
    public static final int PER_USER_RANGE = 100000;
    public static final int USER_ALL = -1;
    public static final int USER_CURRENT = -2;
    public static final int USER_CURRENT_OR_SELF = -3;
    public static final int USER_NULL = -10000;

    @Deprecated
    public static final int USER_OWNER = 0;
    public static final int USER_SERIAL_SYSTEM = 0;
    public static final int USER_SYSTEM = 0;
    final int mHandle;
    public static final UserHandle ALL = new UserHandle(-1);
    public static final UserHandle CURRENT = new UserHandle(-2);
    public static final UserHandle CURRENT_OR_SELF = new UserHandle(-3);

    @Deprecated
    public static final UserHandle OWNER = new UserHandle(0);
    public static final UserHandle SYSTEM = new UserHandle(0);
    public static final Parcelable.Creator<UserHandle> CREATOR = new Parcelable.Creator<UserHandle>() {
        @Override
        public UserHandle createFromParcel(Parcel parcel) {
            return new UserHandle(parcel);
        }

        @Override
        public UserHandle[] newArray(int i) {
            return new UserHandle[i];
        }
    };

    public static boolean isSameUser(int i, int i2) {
        return getUserId(i) == getUserId(i2);
    }

    public static boolean isSameApp(int i, int i2) {
        return getAppId(i) == getAppId(i2);
    }

    public static boolean isIsolated(int i) {
        int appId;
        return i > 0 && (appId = getAppId(i)) >= 99000 && appId <= 99999;
    }

    public static boolean isApp(int i) {
        int appId;
        return i > 0 && (appId = getAppId(i)) >= 10000 && appId <= 19999;
    }

    public static boolean isCore(int i) {
        return i >= 0 && getAppId(i) < 10000;
    }

    public static UserHandle getUserHandleForUid(int i) {
        return of(getUserId(i));
    }

    public static int getUserId(int i) {
        return i / PER_USER_RANGE;
    }

    public static int getCallingUserId() {
        return getUserId(Binder.getCallingUid());
    }

    public static int getCallingAppId() {
        return getAppId(Binder.getCallingUid());
    }

    @SystemApi
    public static UserHandle of(int i) {
        return i == 0 ? SYSTEM : new UserHandle(i);
    }

    public static int getUid(int i, int i2) {
        return (i * PER_USER_RANGE) + (i2 % PER_USER_RANGE);
    }

    public static int getAppId(int i) {
        return i % PER_USER_RANGE;
    }

    public static int getUserGid(int i) {
        return getUid(i, Process.SHARED_USER_GID);
    }

    public static int getSharedAppGid(int i) {
        return getSharedAppGid(getUserId(i), getAppId(i));
    }

    public static int getSharedAppGid(int i, int i2) {
        if (i2 >= 10000 && i2 <= 19999) {
            return (i2 - 10000) + 50000;
        }
        if (i2 >= 0 && i2 <= 10000) {
            return i2;
        }
        return -1;
    }

    public static int getAppIdFromSharedAppGid(int i) {
        int appId = (getAppId(i) + 10000) - 50000;
        if (appId < 0 || appId >= 50000) {
            return -1;
        }
        return appId;
    }

    public static int getCacheAppGid(int i) {
        return getCacheAppGid(getUserId(i), getAppId(i));
    }

    public static int getCacheAppGid(int i, int i2) {
        if (i2 >= 10000 && i2 <= 19999) {
            return getUid(i, (i2 - 10000) + 20000);
        }
        return -1;
    }

    public static void formatUid(StringBuilder sb, int i) {
        if (i < 10000) {
            sb.append(i);
            return;
        }
        sb.append('u');
        sb.append(getUserId(i));
        int appId = getAppId(i);
        if (appId >= 99000 && appId <= 99999) {
            sb.append('i');
            sb.append(appId - Process.FIRST_ISOLATED_UID);
        } else if (appId >= 10000) {
            sb.append(DateFormat.AM_PM);
            sb.append(appId - 10000);
        } else {
            sb.append('s');
            sb.append(appId);
        }
    }

    public static String formatUid(int i) {
        StringBuilder sb = new StringBuilder();
        formatUid(sb, i);
        return sb.toString();
    }

    public static void formatUid(PrintWriter printWriter, int i) {
        if (i < 10000) {
            printWriter.print(i);
            return;
        }
        printWriter.print('u');
        printWriter.print(getUserId(i));
        int appId = getAppId(i);
        if (appId >= 99000 && appId <= 99999) {
            printWriter.print('i');
            printWriter.print(appId - Process.FIRST_ISOLATED_UID);
        } else if (appId >= 10000) {
            printWriter.print(DateFormat.AM_PM);
            printWriter.print(appId - 10000);
        } else {
            printWriter.print('s');
            printWriter.print(appId);
        }
    }

    public static int parseUserArg(String str) {
        if ("all".equals(str)) {
            return -1;
        }
        if (Telephony.Carriers.CURRENT.equals(str) || "cur".equals(str)) {
            return -2;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad user number: " + str);
        }
    }

    @SystemApi
    public static int myUserId() {
        return getUserId(Process.myUid());
    }

    @SystemApi
    @Deprecated
    public boolean isOwner() {
        return equals(OWNER);
    }

    @SystemApi
    public boolean isSystem() {
        return equals(SYSTEM);
    }

    public UserHandle(int i) {
        this.mHandle = i;
    }

    @SystemApi
    public int getIdentifier() {
        return this.mHandle;
    }

    public String toString() {
        return "UserHandle{" + this.mHandle + "}";
    }

    public boolean equals(Object obj) {
        if (obj != null) {
            try {
                return this.mHandle == ((UserHandle) obj).mHandle;
            } catch (ClassCastException e) {
            }
        }
        return false;
    }

    public int hashCode() {
        return this.mHandle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mHandle);
    }

    public static void writeToParcel(UserHandle userHandle, Parcel parcel) {
        if (userHandle != null) {
            userHandle.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(-10000);
        }
    }

    public static UserHandle readFromParcel(Parcel parcel) {
        int i = parcel.readInt();
        if (i != -10000) {
            return new UserHandle(i);
        }
        return null;
    }

    public UserHandle(Parcel parcel) {
        this.mHandle = parcel.readInt();
    }
}
