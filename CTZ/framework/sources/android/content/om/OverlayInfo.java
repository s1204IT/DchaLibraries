package android.content.om;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class OverlayInfo implements Parcelable {
    public static final String CATEGORY_THEME = "android.theme";
    public static final Parcelable.Creator<OverlayInfo> CREATOR = new Parcelable.Creator<OverlayInfo>() {
        @Override
        public OverlayInfo createFromParcel(Parcel parcel) {
            return new OverlayInfo(parcel);
        }

        @Override
        public OverlayInfo[] newArray(int i) {
            return new OverlayInfo[i];
        }
    };
    public static final int STATE_DISABLED = 2;
    public static final int STATE_ENABLED = 3;
    public static final int STATE_ENABLED_STATIC = 6;
    public static final int STATE_MISSING_TARGET = 0;
    public static final int STATE_NO_IDMAP = 1;
    public static final int STATE_OVERLAY_UPGRADING = 5;
    public static final int STATE_TARGET_UPGRADING = 4;
    public static final int STATE_UNKNOWN = -1;
    public final String baseCodePath;
    public final String category;
    public final boolean isStatic;
    public final String packageName;
    public final int priority;
    public final int state;
    public final String targetPackageName;
    public final int userId;

    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    public OverlayInfo(OverlayInfo overlayInfo, int i) {
        this(overlayInfo.packageName, overlayInfo.targetPackageName, overlayInfo.category, overlayInfo.baseCodePath, i, overlayInfo.userId, overlayInfo.priority, overlayInfo.isStatic);
    }

    public OverlayInfo(String str, String str2, String str3, String str4, int i, int i2, int i3, boolean z) {
        this.packageName = str;
        this.targetPackageName = str2;
        this.category = str3;
        this.baseCodePath = str4;
        this.state = i;
        this.userId = i2;
        this.priority = i3;
        this.isStatic = z;
        ensureValidState();
    }

    public OverlayInfo(Parcel parcel) {
        this.packageName = parcel.readString();
        this.targetPackageName = parcel.readString();
        this.category = parcel.readString();
        this.baseCodePath = parcel.readString();
        this.state = parcel.readInt();
        this.userId = parcel.readInt();
        this.priority = parcel.readInt();
        this.isStatic = parcel.readBoolean();
        ensureValidState();
    }

    private void ensureValidState() {
        if (this.packageName == null) {
            throw new IllegalArgumentException("packageName must not be null");
        }
        if (this.targetPackageName == null) {
            throw new IllegalArgumentException("targetPackageName must not be null");
        }
        if (this.baseCodePath == null) {
            throw new IllegalArgumentException("baseCodePath must not be null");
        }
        switch (this.state) {
            case -1:
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                return;
            default:
                throw new IllegalArgumentException("State " + this.state + " is not a valid state");
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.packageName);
        parcel.writeString(this.targetPackageName);
        parcel.writeString(this.category);
        parcel.writeString(this.baseCodePath);
        parcel.writeInt(this.state);
        parcel.writeInt(this.userId);
        parcel.writeInt(this.priority);
        parcel.writeBoolean(this.isStatic);
    }

    public boolean isEnabled() {
        int i = this.state;
        if (i == 3 || i == 6) {
            return true;
        }
        return false;
    }

    public static String stateToString(int i) {
        switch (i) {
            case -1:
                return "STATE_UNKNOWN";
            case 0:
                return "STATE_MISSING_TARGET";
            case 1:
                return "STATE_NO_IDMAP";
            case 2:
                return "STATE_DISABLED";
            case 3:
                return "STATE_ENABLED";
            case 4:
                return "STATE_TARGET_UPGRADING";
            case 5:
                return "STATE_OVERLAY_UPGRADING";
            case 6:
                return "STATE_ENABLED_STATIC";
            default:
                return "<unknown state>";
        }
    }

    public int hashCode() {
        return (31 * (((((((((this.userId + 31) * 31) + this.state) * 31) + (this.packageName == null ? 0 : this.packageName.hashCode())) * 31) + (this.targetPackageName == null ? 0 : this.targetPackageName.hashCode())) * 31) + (this.category == null ? 0 : this.category.hashCode()))) + (this.baseCodePath != null ? this.baseCodePath.hashCode() : 0);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OverlayInfo overlayInfo = (OverlayInfo) obj;
        if (this.userId == overlayInfo.userId && this.state == overlayInfo.state && this.packageName.equals(overlayInfo.packageName) && this.targetPackageName.equals(overlayInfo.targetPackageName) && this.category.equals(overlayInfo.category) && this.baseCodePath.equals(overlayInfo.baseCodePath)) {
            return true;
        }
        return false;
    }

    public String toString() {
        return "OverlayInfo { overlay=" + this.packageName + ", target=" + this.targetPackageName + ", state=" + this.state + " (" + stateToString(this.state) + "), userId=" + this.userId + " }";
    }
}
