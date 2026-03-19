package android.bluetooth;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class BluetoothAvrcpPlayerSettings implements Parcelable {
    public static final Parcelable.Creator<BluetoothAvrcpPlayerSettings> CREATOR = new Parcelable.Creator<BluetoothAvrcpPlayerSettings>() {
        @Override
        public BluetoothAvrcpPlayerSettings createFromParcel(Parcel parcel) {
            return new BluetoothAvrcpPlayerSettings(parcel);
        }

        @Override
        public BluetoothAvrcpPlayerSettings[] newArray(int i) {
            return new BluetoothAvrcpPlayerSettings[i];
        }
    };
    public static final int SETTING_EQUALIZER = 1;
    public static final int SETTING_REPEAT = 2;
    public static final int SETTING_SCAN = 8;
    public static final int SETTING_SHUFFLE = 4;
    public static final int STATE_ALL_TRACK = 3;
    public static final int STATE_GROUP = 4;
    public static final int STATE_INVALID = -1;
    public static final int STATE_OFF = 0;
    public static final int STATE_ON = 1;
    public static final int STATE_SINGLE_TRACK = 2;
    public static final String TAG = "BluetoothAvrcpPlayerSettings";
    private int mSettings;
    private Map<Integer, Integer> mSettingsValue;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSettings);
        parcel.writeInt(this.mSettingsValue.size());
        Iterator<Integer> it = this.mSettingsValue.keySet().iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            parcel.writeInt(iIntValue);
            parcel.writeInt(this.mSettingsValue.get(Integer.valueOf(iIntValue)).intValue());
        }
    }

    private BluetoothAvrcpPlayerSettings(Parcel parcel) {
        this.mSettingsValue = new HashMap();
        this.mSettings = parcel.readInt();
        int i = parcel.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            this.mSettingsValue.put(Integer.valueOf(parcel.readInt()), Integer.valueOf(parcel.readInt()));
        }
    }

    public BluetoothAvrcpPlayerSettings(int i) {
        this.mSettingsValue = new HashMap();
        this.mSettings = i;
    }

    public int getSettings() {
        return this.mSettings;
    }

    public void addSettingValue(int i, int i2) {
        if ((this.mSettings & i) == 0) {
            Log.e(TAG, "Setting not supported: " + i + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.mSettings);
            throw new IllegalStateException("Setting not supported: " + i);
        }
        this.mSettingsValue.put(Integer.valueOf(i), Integer.valueOf(i2));
    }

    public int getSettingValue(int i) {
        if ((this.mSettings & i) == 0) {
            Log.e(TAG, "Setting not supported: " + i + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.mSettings);
            throw new IllegalStateException("Setting not supported: " + i);
        }
        Integer num = this.mSettingsValue.get(Integer.valueOf(i));
        if (num == null) {
            return -1;
        }
        return num.intValue();
    }
}
