package com.android.bluetooth.opp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.HashMap;

public class BluetoothOppPreference {
    private static final String TAG = "BluetoothOppPreference";
    private static BluetoothOppPreference sInstance;
    private SharedPreferences mChannelPreference;
    private Context mContext;
    private boolean mInitialized;
    private SharedPreferences mNamePreference;
    private static final boolean V = Constants.VERBOSE;
    private static final Object INSTANCE_LOCK = new Object();
    private HashMap<String, Integer> mChannels = new HashMap<>();
    private HashMap<String, String> mNames = new HashMap<>();

    public static BluetoothOppPreference getInstance(Context context) {
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new BluetoothOppPreference();
            }
            if (!sInstance.init(context)) {
                return null;
            }
            return sInstance;
        }
    }

    private boolean init(Context context) {
        if (this.mInitialized) {
            return true;
        }
        this.mInitialized = true;
        this.mContext = context;
        this.mNamePreference = this.mContext.getSharedPreferences("btopp_names", 0);
        this.mChannelPreference = this.mContext.getSharedPreferences("btopp_channels", 0);
        this.mNames = (HashMap) this.mNamePreference.getAll();
        this.mChannels = (HashMap) this.mChannelPreference.getAll();
        return true;
    }

    private String getChannelKey(BluetoothDevice bluetoothDevice, int i) {
        return bluetoothDevice.getAddress() + "_" + Integer.toHexString(i);
    }

    public String getName(BluetoothDevice bluetoothDevice) {
        String str;
        if (bluetoothDevice.getAddress().equals("FF:FF:FF:00:00:00")) {
            return "localhost";
        }
        if (!this.mNames.isEmpty() && (str = this.mNames.get(bluetoothDevice.getAddress())) != null) {
            return str;
        }
        return null;
    }

    public int getChannel(BluetoothDevice bluetoothDevice, int i) {
        String channelKey = getChannelKey(bluetoothDevice, i);
        if (V) {
            Log.v(TAG, "getChannel " + channelKey);
        }
        Integer num = null;
        if (this.mChannels != null) {
            num = this.mChannels.get(channelKey);
            if (V) {
                Log.v(TAG, "getChannel for " + bluetoothDevice + "_" + Integer.toHexString(i) + " as " + num);
            }
        }
        if (num != null) {
            return num.intValue();
        }
        return -1;
    }

    public void setName(BluetoothDevice bluetoothDevice, String str) {
        if (V) {
            Log.v(TAG, "Setname for " + bluetoothDevice + " to " + str);
        }
        if (str != null && !str.equals(getName(bluetoothDevice))) {
            SharedPreferences.Editor editorEdit = this.mNamePreference.edit();
            editorEdit.putString(bluetoothDevice.getAddress(), str);
            editorEdit.apply();
            this.mNames.put(bluetoothDevice.getAddress(), str);
        }
    }

    public void setChannel(BluetoothDevice bluetoothDevice, int i, int i2) {
        if (V) {
            Log.v(TAG, "Setchannel for " + bluetoothDevice + "_" + Integer.toHexString(i) + " to " + i2);
        }
        if (i2 != getChannel(bluetoothDevice, i)) {
            String channelKey = getChannelKey(bluetoothDevice, i);
            SharedPreferences.Editor editorEdit = this.mChannelPreference.edit();
            editorEdit.putInt(channelKey, i2);
            editorEdit.apply();
            this.mChannels.put(channelKey, Integer.valueOf(i2));
        }
    }

    public void removeChannel(BluetoothDevice bluetoothDevice, int i) {
        String channelKey = getChannelKey(bluetoothDevice, i);
        SharedPreferences.Editor editorEdit = this.mChannelPreference.edit();
        editorEdit.remove(channelKey);
        editorEdit.apply();
        this.mChannels.remove(channelKey);
    }

    public void dump() {
        Log.d(TAG, "Dumping Names:  ");
        Log.d(TAG, this.mNames.toString());
        Log.d(TAG, "Dumping Channels:  ");
        Log.d(TAG, this.mChannels.toString());
    }
}
