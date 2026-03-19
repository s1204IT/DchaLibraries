package android.hardware.usb;

import android.app.slice.Slice;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;

public final class UsbPort implements Parcelable {
    public static final Parcelable.Creator<UsbPort> CREATOR = new Parcelable.Creator<UsbPort>() {
        @Override
        public UsbPort createFromParcel(Parcel parcel) {
            return new UsbPort(parcel.readString(), parcel.readInt());
        }

        @Override
        public UsbPort[] newArray(int i) {
            return new UsbPort[i];
        }
    };
    public static final int DATA_ROLE_DEVICE = 2;
    public static final int DATA_ROLE_HOST = 1;
    public static final int DATA_ROLE_NONE = 0;
    public static final int MODE_AUDIO_ACCESSORY = 4;
    public static final int MODE_DEBUG_ACCESSORY = 8;
    public static final int MODE_DFP = 2;
    public static final int MODE_DUAL = 3;
    public static final int MODE_NONE = 0;
    public static final int MODE_UFP = 1;
    private static final int NUM_DATA_ROLES = 3;
    public static final int POWER_ROLE_NONE = 0;
    private static final int POWER_ROLE_OFFSET = 0;
    public static final int POWER_ROLE_SINK = 2;
    public static final int POWER_ROLE_SOURCE = 1;
    private final String mId;
    private final int mSupportedModes;

    public UsbPort(String str, int i) {
        this.mId = str;
        this.mSupportedModes = i;
    }

    public String getId() {
        return this.mId;
    }

    public int getSupportedModes() {
        return this.mSupportedModes;
    }

    public static int combineRolesAsBit(int i, int i2) {
        checkRoles(i, i2);
        return 1 << (((i + 0) * 3) + i2);
    }

    public static String modeToString(int i) {
        StringBuilder sb = new StringBuilder();
        if (i == 0) {
            return "none";
        }
        if ((i & 3) == 3) {
            sb.append("dual, ");
        } else if ((i & 2) == 2) {
            sb.append("dfp, ");
        } else if ((i & 1) == 1) {
            sb.append("ufp, ");
        }
        if ((i & 4) == 4) {
            sb.append("audio_acc, ");
        }
        if ((i & 8) == 8) {
            sb.append("debug_acc, ");
        }
        if (sb.length() == 0) {
            return Integer.toString(i);
        }
        return sb.substring(0, sb.length() - 2);
    }

    public static String powerRoleToString(int i) {
        switch (i) {
            case 0:
                return "no-power";
            case 1:
                return Slice.SUBTYPE_SOURCE;
            case 2:
                return "sink";
            default:
                return Integer.toString(i);
        }
    }

    public static String dataRoleToString(int i) {
        switch (i) {
            case 0:
                return "no-data";
            case 1:
                return "host";
            case 2:
                return UsbManager.EXTRA_DEVICE;
            default:
                return Integer.toString(i);
        }
    }

    public static String roleCombinationsToString(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean z = true;
        while (i != 0) {
            int iNumberOfTrailingZeros = Integer.numberOfTrailingZeros(i);
            i &= ~(1 << iNumberOfTrailingZeros);
            int i2 = (iNumberOfTrailingZeros / 3) + 0;
            int i3 = iNumberOfTrailingZeros % 3;
            if (!z) {
                sb.append(", ");
            } else {
                z = false;
            }
            sb.append(powerRoleToString(i2));
            sb.append(':');
            sb.append(dataRoleToString(i3));
        }
        sb.append("]");
        return sb.toString();
    }

    public static void checkMode(int i) {
        Preconditions.checkArgumentInRange(i, 0, 3, "portMode");
    }

    public static void checkPowerRole(int i) {
        Preconditions.checkArgumentInRange(i, 0, 2, "powerRole");
    }

    public static void checkDataRole(int i) {
        Preconditions.checkArgumentInRange(i, 0, 2, "powerRole");
    }

    public static void checkRoles(int i, int i2) {
        Preconditions.checkArgumentInRange(i, 0, 2, "powerRole");
        Preconditions.checkArgumentInRange(i2, 0, 2, "dataRole");
    }

    public boolean isModeSupported(int i) {
        return (this.mSupportedModes & i) == i;
    }

    public String toString() {
        return "UsbPort{id=" + this.mId + ", supportedModes=" + modeToString(this.mSupportedModes) + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mId);
        parcel.writeInt(this.mSupportedModes);
    }
}
