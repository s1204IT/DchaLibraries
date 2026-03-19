package android.app.admin;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.util.EventLog;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Objects;

public class SecurityLog {
    public static final int LEVEL_ERROR = 3;
    public static final int LEVEL_INFO = 1;
    public static final int LEVEL_WARNING = 2;
    private static final String PROPERTY_LOGGING_ENABLED = "persist.logd.security";
    public static final int TAG_ADB_SHELL_CMD = 210002;
    public static final int TAG_ADB_SHELL_INTERACTIVE = 210001;
    public static final int TAG_APP_PROCESS_START = 210005;
    public static final int TAG_CERT_AUTHORITY_INSTALLED = 210029;
    public static final int TAG_CERT_AUTHORITY_REMOVED = 210030;
    public static final int TAG_CERT_VALIDATION_FAILURE = 210033;
    public static final int TAG_CRYPTO_SELF_TEST_COMPLETED = 210031;
    public static final int TAG_KEYGUARD_DISABLED_FEATURES_SET = 210021;
    public static final int TAG_KEYGUARD_DISMISSED = 210006;
    public static final int TAG_KEYGUARD_DISMISS_AUTH_ATTEMPT = 210007;
    public static final int TAG_KEYGUARD_SECURED = 210008;
    public static final int TAG_KEY_DESTRUCTION = 210026;
    public static final int TAG_KEY_GENERATED = 210024;
    public static final int TAG_KEY_IMPORT = 210025;
    public static final int TAG_KEY_INTEGRITY_VIOLATION = 210032;
    public static final int TAG_LOGGING_STARTED = 210011;
    public static final int TAG_LOGGING_STOPPED = 210012;
    public static final int TAG_LOG_BUFFER_SIZE_CRITICAL = 210015;
    public static final int TAG_MAX_PASSWORD_ATTEMPTS_SET = 210020;
    public static final int TAG_MAX_SCREEN_LOCK_TIMEOUT_SET = 210019;
    public static final int TAG_MEDIA_MOUNT = 210013;
    public static final int TAG_MEDIA_UNMOUNT = 210014;
    public static final int TAG_OS_SHUTDOWN = 210010;
    public static final int TAG_OS_STARTUP = 210009;
    public static final int TAG_PASSWORD_COMPLEXITY_SET = 210017;
    public static final int TAG_PASSWORD_EXPIRATION_SET = 210016;
    public static final int TAG_PASSWORD_HISTORY_LENGTH_SET = 210018;
    public static final int TAG_REMOTE_LOCK = 210022;
    public static final int TAG_SYNC_RECV_FILE = 210003;
    public static final int TAG_SYNC_SEND_FILE = 210004;
    public static final int TAG_USER_RESTRICTION_ADDED = 210027;
    public static final int TAG_USER_RESTRICTION_REMOVED = 210028;
    public static final int TAG_WIPE_FAILURE = 210023;

    @Retention(RetentionPolicy.SOURCE)
    public @interface SecurityLogLevel {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SecurityLogTag {
    }

    public static native boolean isLoggingEnabled();

    public static native void readEvents(Collection<SecurityEvent> collection) throws IOException;

    public static native void readEventsOnWrapping(long j, Collection<SecurityEvent> collection) throws IOException;

    public static native void readEventsSince(long j, Collection<SecurityEvent> collection) throws IOException;

    public static native void readPreviousEvents(Collection<SecurityEvent> collection) throws IOException;

    public static native int writeEvent(int i, String str);

    public static native int writeEvent(int i, Object... objArr);

    public static void setLoggingEnabledProperty(boolean z) {
        SystemProperties.set(PROPERTY_LOGGING_ENABLED, z ? "true" : "false");
    }

    public static boolean getLoggingEnabledProperty() {
        return SystemProperties.getBoolean(PROPERTY_LOGGING_ENABLED, false);
    }

    public static final class SecurityEvent implements Parcelable {
        public static final Parcelable.Creator<SecurityEvent> CREATOR = new Parcelable.Creator<SecurityEvent>() {
            @Override
            public SecurityEvent createFromParcel(Parcel parcel) {
                return new SecurityEvent(parcel);
            }

            @Override
            public SecurityEvent[] newArray(int i) {
                return new SecurityEvent[i];
            }
        };
        private EventLog.Event mEvent;
        private long mId;

        SecurityEvent(byte[] bArr) {
            this(0L, bArr);
        }

        SecurityEvent(Parcel parcel) {
            this(parcel.readLong(), parcel.createByteArray());
        }

        public SecurityEvent(long j, byte[] bArr) {
            this.mId = j;
            this.mEvent = EventLog.Event.fromBytes(bArr);
        }

        public long getTimeNanos() {
            return this.mEvent.getTimeNanos();
        }

        public int getTag() {
            return this.mEvent.getTag();
        }

        public Object getData() {
            return this.mEvent.getData();
        }

        public void setId(long j) {
            this.mId = j;
        }

        public long getId() {
            return this.mId;
        }

        public int getLogLevel() {
            switch (this.mEvent.getTag()) {
                case 210001:
                case 210002:
                case 210003:
                case 210004:
                case 210005:
                case 210006:
                case 210008:
                case 210009:
                case 210010:
                case 210011:
                case 210012:
                case 210013:
                case 210014:
                case 210016:
                case 210017:
                case 210018:
                case 210019:
                case 210020:
                case 210027:
                case 210028:
                    return 1;
                case 210007:
                case 210024:
                case 210025:
                case 210026:
                case 210029:
                    return getSuccess() ? 1 : 2;
                case 210015:
                case 210023:
                case 210032:
                    return 3;
                case 210021:
                case 210022:
                default:
                    return 1;
                case 210030:
                case 210031:
                    return getSuccess() ? 1 : 3;
                case 210033:
                    return 2;
            }
        }

        private boolean getSuccess() {
            Object data = getData();
            if (data == null || !(data instanceof Object[])) {
                return false;
            }
            Object[] objArr = (Object[]) data;
            return objArr.length >= 1 && (objArr[0] instanceof Integer) && ((Integer) objArr[0]).intValue() != 0;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(this.mId);
            parcel.writeByteArray(this.mEvent.getBytes());
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SecurityEvent securityEvent = (SecurityEvent) obj;
            if (this.mEvent.equals(securityEvent.mEvent) && this.mId == securityEvent.mId) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.mEvent, Long.valueOf(this.mId));
        }
    }
}
