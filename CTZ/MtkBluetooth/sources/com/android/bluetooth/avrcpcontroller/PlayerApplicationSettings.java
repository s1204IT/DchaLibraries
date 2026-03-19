package com.android.bluetooth.avrcpcontroller;

import android.bluetooth.BluetoothAvrcpPlayerSettings;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class PlayerApplicationSettings {
    private static final byte JNI_ATTRIB_EQUALIZER_STATUS = 1;
    private static final byte JNI_ATTRIB_REPEAT_STATUS = 2;
    private static final byte JNI_ATTRIB_SCAN_STATUS = 4;
    private static final byte JNI_ATTRIB_SHUFFLE_STATUS = 3;
    private static final byte JNI_EQUALIZER_STATUS_OFF = 1;
    private static final byte JNI_EQUALIZER_STATUS_ON = 2;
    private static final byte JNI_REPEAT_STATUS_ALL_TRACK_REPEAT = 3;
    private static final byte JNI_REPEAT_STATUS_GROUP_REPEAT = 4;
    private static final byte JNI_REPEAT_STATUS_OFF = 1;
    private static final byte JNI_REPEAT_STATUS_SINGLE_TRACK_REPEAT = 2;
    private static final byte JNI_SCAN_STATUS_ALL_TRACK_SCAN = 2;
    private static final byte JNI_SCAN_STATUS_GROUP_SCAN = 3;
    private static final byte JNI_SCAN_STATUS_OFF = 1;
    private static final byte JNI_SHUFFLE_STATUS_ALL_TRACK_SHUFFLE = 2;
    private static final byte JNI_SHUFFLE_STATUS_GROUP_SHUFFLE = 3;
    private static final byte JNI_SHUFFLE_STATUS_OFF = 1;
    private static final byte JNI_STATUS_INVALID = -1;
    private static final String TAG = "PlayerApplicationSettings";
    private Map<Integer, Integer> mSettings = new HashMap();
    private Map<Integer, ArrayList<Integer>> mSupportedValues = new HashMap();

    PlayerApplicationSettings() {
    }

    static PlayerApplicationSettings makeSupportedSettings(byte[] bArr) {
        PlayerApplicationSettings playerApplicationSettings = new PlayerApplicationSettings();
        int i = 0;
        while (i < bArr.length) {
            try {
                int i2 = i + 1;
                byte b = bArr[i];
                int i3 = i2 + 1;
                byte b2 = bArr[i2];
                ArrayList<Integer> arrayList = new ArrayList<>();
                int i4 = i3;
                int i5 = 0;
                while (i5 < b2) {
                    arrayList.add(Integer.valueOf(mapAttribIdValtoAvrcpPlayerSetting(b, bArr[i4])));
                    i5++;
                    i4++;
                }
                playerApplicationSettings.mSupportedValues.put(Integer.valueOf(mapBTAttribIdToAvrcpPlayerSettings(b)), arrayList);
                i = i4;
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "makeSupportedSettings attributeList index error.");
            }
        }
        return playerApplicationSettings;
    }

    public BluetoothAvrcpPlayerSettings getAvrcpSettings() {
        Iterator<Integer> it = this.mSettings.keySet().iterator();
        int iIntValue = 0;
        while (it.hasNext()) {
            iIntValue |= it.next().intValue();
        }
        BluetoothAvrcpPlayerSettings bluetoothAvrcpPlayerSettings = new BluetoothAvrcpPlayerSettings(iIntValue);
        for (Integer num : this.mSettings.keySet()) {
            bluetoothAvrcpPlayerSettings.addSettingValue(num.intValue(), this.mSettings.get(num).intValue());
        }
        return bluetoothAvrcpPlayerSettings;
    }

    static PlayerApplicationSettings makeSettings(byte[] bArr) {
        PlayerApplicationSettings playerApplicationSettings = new PlayerApplicationSettings();
        int i = 0;
        while (i < bArr.length) {
            try {
                int i2 = i + 1;
                byte b = bArr[i];
                int i3 = i2 + 1;
                playerApplicationSettings.mSettings.put(Integer.valueOf(mapBTAttribIdToAvrcpPlayerSettings(b)), Integer.valueOf(mapAttribIdValtoAvrcpPlayerSetting(b, bArr[i2])));
                i = i3;
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "makeSettings JNI_ATTRIButeList index error.");
            }
        }
        return playerApplicationSettings;
    }

    public void setSupport(PlayerApplicationSettings playerApplicationSettings) {
        this.mSettings = playerApplicationSettings.mSettings;
        this.mSupportedValues = playerApplicationSettings.mSupportedValues;
    }

    public void setValues(BluetoothAvrcpPlayerSettings bluetoothAvrcpPlayerSettings) {
        int settings = bluetoothAvrcpPlayerSettings.getSettings();
        for (int i = 1; i <= 8; i++) {
            if ((i & settings) > 0) {
                this.mSettings.put(Integer.valueOf(i), Integer.valueOf(bluetoothAvrcpPlayerSettings.getSettingValue(i)));
            }
        }
    }

    public boolean supportsSettings(BluetoothAvrcpPlayerSettings bluetoothAvrcpPlayerSettings) {
        int settings = bluetoothAvrcpPlayerSettings.getSettings();
        Iterator<Integer> it = this.mSupportedValues.keySet().iterator();
        int iIntValue = 0;
        while (it.hasNext()) {
            iIntValue |= it.next().intValue();
        }
        if ((iIntValue & settings) == settings) {
            try {
                for (Integer num : this.mSettings.keySet()) {
                    if ((num.intValue() & settings) == num.intValue() && !this.mSupportedValues.get(num).contains(Integer.valueOf(bluetoothAvrcpPlayerSettings.getSettingValue(num.intValue())))) {
                        return false;
                    }
                }
                return true;
            } catch (NullPointerException e) {
                Log.e(TAG, "supportsSettings received a supported setting that has no supported values.");
            }
        }
        return false;
    }

    public ArrayList<Byte> getNativeSettings() {
        ArrayList<Byte> arrayList = new ArrayList<>();
        for (Integer num : this.mSettings.keySet()) {
            int iIntValue = num.intValue();
            if (iIntValue == 4) {
                arrayList.add((byte) 3);
                arrayList.add(Byte.valueOf(mapAvrcpPlayerSettingstoBTattribVal(num.intValue(), this.mSettings.get(num).intValue())));
            } else if (iIntValue != 8) {
                switch (iIntValue) {
                    case 1:
                        arrayList.add((byte) 1);
                        arrayList.add(Byte.valueOf(mapAvrcpPlayerSettingstoBTattribVal(num.intValue(), this.mSettings.get(num).intValue())));
                        break;
                    case 2:
                        arrayList.add((byte) 2);
                        arrayList.add(Byte.valueOf(mapAvrcpPlayerSettingstoBTattribVal(num.intValue(), this.mSettings.get(num).intValue())));
                        break;
                    default:
                        Log.w(TAG, "Unknown setting found in getNativeSettings: " + num);
                        break;
                }
            } else {
                arrayList.add((byte) 4);
                arrayList.add(Byte.valueOf(mapAvrcpPlayerSettingstoBTattribVal(num.intValue(), this.mSettings.get(num).intValue())));
            }
        }
        return arrayList;
    }

    private static int mapAttribIdValtoAvrcpPlayerSetting(byte b, byte b2) {
        if (b == 1) {
            switch (b2) {
                case 1:
                    return 0;
                case 2:
                    return 1;
                default:
                    return -1;
            }
        }
        if (b == 2) {
            switch (b2) {
                case 1:
                    return 0;
                case 2:
                    return 2;
                case 3:
                    return 3;
                case 4:
                    return 4;
                default:
                    return -1;
            }
        }
        if (b != 4) {
            if (b == 3) {
                switch (b2) {
                    case 1:
                        return 0;
                    case 2:
                        return 3;
                    case 3:
                        return 4;
                    default:
                        return -1;
                }
            }
            return -1;
        }
        switch (b2) {
            case 1:
                return 0;
            case 2:
                return 3;
            case 3:
                return 4;
            default:
                return -1;
        }
    }

    private static byte mapAvrcpPlayerSettingstoBTattribVal(int i, int i2) {
        if (i == 1) {
            switch (i2) {
                case 0:
                    return (byte) 1;
                case 1:
                    return (byte) 2;
                default:
                    return JNI_STATUS_INVALID;
            }
        }
        if (i == 2) {
            if (i2 == 0) {
                return (byte) 1;
            }
            switch (i2) {
                case 2:
                    return (byte) 2;
                case 3:
                    return (byte) 3;
                case 4:
                    return (byte) 4;
                default:
                    return JNI_STATUS_INVALID;
            }
        }
        if (i == 4) {
            if (i2 == 0) {
                return (byte) 1;
            }
            switch (i2) {
                case 3:
                    return (byte) 2;
                case 4:
                    return (byte) 3;
                default:
                    return JNI_STATUS_INVALID;
            }
        }
        if (i == 8) {
            if (i2 == 0) {
                return (byte) 1;
            }
            switch (i2) {
                case 3:
                    return (byte) 2;
                case 4:
                    return (byte) 3;
                default:
                    return JNI_STATUS_INVALID;
            }
        }
        return JNI_STATUS_INVALID;
    }

    private static int mapBTAttribIdToAvrcpPlayerSettings(byte b) {
        switch (b) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 4;
            case 4:
                return 8;
            default:
                return -1;
        }
    }
}
