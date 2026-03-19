package android.hardware.display;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public final class WifiDisplayStatus implements Parcelable {
    public static final Parcelable.Creator<WifiDisplayStatus> CREATOR = new Parcelable.Creator<WifiDisplayStatus>() {
        @Override
        public WifiDisplayStatus createFromParcel(Parcel parcel) {
            WifiDisplay wifiDisplayCreateFromParcel;
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            int i3 = parcel.readInt();
            if (parcel.readInt() != 0) {
                wifiDisplayCreateFromParcel = WifiDisplay.CREATOR.createFromParcel(parcel);
            } else {
                wifiDisplayCreateFromParcel = null;
            }
            WifiDisplay wifiDisplay = wifiDisplayCreateFromParcel;
            WifiDisplay[] wifiDisplayArrNewArray = WifiDisplay.CREATOR.newArray(parcel.readInt());
            for (int i4 = 0; i4 < wifiDisplayArrNewArray.length; i4++) {
                wifiDisplayArrNewArray[i4] = WifiDisplay.CREATOR.createFromParcel(parcel);
            }
            return new WifiDisplayStatus(i, i2, i3, wifiDisplay, wifiDisplayArrNewArray, WifiDisplaySessionInfo.CREATOR.createFromParcel(parcel));
        }

        @Override
        public WifiDisplayStatus[] newArray(int i) {
            return new WifiDisplayStatus[i];
        }
    };
    public static final int DISPLAY_STATE_CONNECTED = 2;
    public static final int DISPLAY_STATE_CONNECTING = 1;
    public static final int DISPLAY_STATE_NOT_CONNECTED = 0;
    public static final int FEATURE_STATE_DISABLED = 1;
    public static final int FEATURE_STATE_OFF = 2;
    public static final int FEATURE_STATE_ON = 3;
    public static final int FEATURE_STATE_UNAVAILABLE = 0;
    public static final int SCAN_STATE_NOT_SCANNING = 0;
    public static final int SCAN_STATE_SCANNING = 1;
    private final WifiDisplay mActiveDisplay;
    private final int mActiveDisplayState;
    private final WifiDisplay[] mDisplays;
    private final int mFeatureState;
    private final int mScanState;
    private final WifiDisplaySessionInfo mSessionInfo;

    public WifiDisplayStatus() {
        this(0, 0, 0, null, WifiDisplay.EMPTY_ARRAY, null);
    }

    public WifiDisplayStatus(int i, int i2, int i3, WifiDisplay wifiDisplay, WifiDisplay[] wifiDisplayArr, WifiDisplaySessionInfo wifiDisplaySessionInfo) {
        if (wifiDisplayArr == null) {
            throw new IllegalArgumentException("displays must not be null");
        }
        this.mFeatureState = i;
        this.mScanState = i2;
        this.mActiveDisplayState = i3;
        this.mActiveDisplay = wifiDisplay;
        this.mDisplays = wifiDisplayArr;
        this.mSessionInfo = wifiDisplaySessionInfo == null ? new WifiDisplaySessionInfo() : wifiDisplaySessionInfo;
    }

    public int getFeatureState() {
        return this.mFeatureState;
    }

    public int getScanState() {
        return this.mScanState;
    }

    public int getActiveDisplayState() {
        return this.mActiveDisplayState;
    }

    public WifiDisplay getActiveDisplay() {
        return this.mActiveDisplay;
    }

    public WifiDisplay[] getDisplays() {
        return this.mDisplays;
    }

    public WifiDisplaySessionInfo getSessionInfo() {
        return this.mSessionInfo;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mFeatureState);
        parcel.writeInt(this.mScanState);
        parcel.writeInt(this.mActiveDisplayState);
        if (this.mActiveDisplay != null) {
            parcel.writeInt(1);
            this.mActiveDisplay.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.mDisplays.length);
        for (WifiDisplay wifiDisplay : this.mDisplays) {
            wifiDisplay.writeToParcel(parcel, i);
        }
        this.mSessionInfo.writeToParcel(parcel, i);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "WifiDisplayStatus{featureState=" + this.mFeatureState + ", scanState=" + this.mScanState + ", activeDisplayState=" + this.mActiveDisplayState + ", activeDisplay=" + this.mActiveDisplay + ", displays=" + Arrays.toString(this.mDisplays) + ", sessionInfo=" + this.mSessionInfo + "}";
    }
}
