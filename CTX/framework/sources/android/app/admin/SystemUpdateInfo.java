package android.app.admin;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public final class SystemUpdateInfo implements Parcelable {
    private static final String ATTR_ORIGINAL_BUILD = "original-build";
    private static final String ATTR_RECEIVED_TIME = "received-time";
    private static final String ATTR_SECURITY_PATCH_STATE = "security-patch-state";
    public static final Parcelable.Creator<SystemUpdateInfo> CREATOR = new Parcelable.Creator<SystemUpdateInfo>() {
        @Override
        public SystemUpdateInfo createFromParcel(Parcel parcel) {
            return new SystemUpdateInfo(parcel);
        }

        @Override
        public SystemUpdateInfo[] newArray(int i) {
            return new SystemUpdateInfo[i];
        }
    };
    public static final int SECURITY_PATCH_STATE_FALSE = 1;
    public static final int SECURITY_PATCH_STATE_TRUE = 2;
    public static final int SECURITY_PATCH_STATE_UNKNOWN = 0;
    private final long mReceivedTime;
    private final int mSecurityPatchState;

    @Retention(RetentionPolicy.SOURCE)
    public @interface SecurityPatchState {
    }

    private SystemUpdateInfo(long j, int i) {
        this.mReceivedTime = j;
        this.mSecurityPatchState = i;
    }

    private SystemUpdateInfo(Parcel parcel) {
        this.mReceivedTime = parcel.readLong();
        this.mSecurityPatchState = parcel.readInt();
    }

    public static SystemUpdateInfo of(long j) {
        if (j == -1) {
            return null;
        }
        return new SystemUpdateInfo(j, 0);
    }

    public static SystemUpdateInfo of(long j, boolean z) {
        if (j == -1) {
            return null;
        }
        return new SystemUpdateInfo(j, z ? 2 : 1);
    }

    public long getReceivedTime() {
        return this.mReceivedTime;
    }

    public int getSecurityPatchState() {
        return this.mSecurityPatchState;
    }

    public void writeToXml(XmlSerializer xmlSerializer, String str) throws IOException {
        xmlSerializer.startTag(null, str);
        xmlSerializer.attribute(null, ATTR_RECEIVED_TIME, String.valueOf(this.mReceivedTime));
        xmlSerializer.attribute(null, ATTR_SECURITY_PATCH_STATE, String.valueOf(this.mSecurityPatchState));
        xmlSerializer.attribute(null, ATTR_ORIGINAL_BUILD, Build.FINGERPRINT);
        xmlSerializer.endTag(null, str);
    }

    public static SystemUpdateInfo readFromXml(XmlPullParser xmlPullParser) {
        if (Build.FINGERPRINT.equals(xmlPullParser.getAttributeValue(null, ATTR_ORIGINAL_BUILD))) {
            return new SystemUpdateInfo(Long.parseLong(xmlPullParser.getAttributeValue(null, ATTR_RECEIVED_TIME)), Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_SECURITY_PATCH_STATE)));
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(getReceivedTime());
        parcel.writeInt(getSecurityPatchState());
    }

    public String toString() {
        return String.format("SystemUpdateInfo (receivedTime = %d, securityPatchState = %s)", Long.valueOf(this.mReceivedTime), securityPatchStateToString(this.mSecurityPatchState));
    }

    private static String securityPatchStateToString(int i) {
        switch (i) {
            case 0:
                return "unknown";
            case 1:
                return "false";
            case 2:
                return "true";
            default:
                throw new IllegalArgumentException("Unrecognized security patch state: " + i);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SystemUpdateInfo systemUpdateInfo = (SystemUpdateInfo) obj;
        if (this.mReceivedTime == systemUpdateInfo.mReceivedTime && this.mSecurityPatchState == systemUpdateInfo.mSecurityPatchState) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Long.valueOf(this.mReceivedTime), Integer.valueOf(this.mSecurityPatchState));
    }
}
