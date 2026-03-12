package com.android.systemui.statusbar.policy;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothMap;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;
import android.text.TextUtils;

public class BluetoothUtil {

    public interface Profile {
        boolean connect(BluetoothDevice bluetoothDevice);

        boolean disconnect(BluetoothDevice bluetoothDevice);
    }

    public static String profileToString(int profile) {
        return profile == 1 ? "HEADSET" : profile == 2 ? "A2DP" : profile == 11 ? "AVRCP_CONTROLLER" : profile == 5 ? "PAN" : profile == 4 ? "INPUT_DEVICE" : profile == 9 ? "MAP" : "UNKNOWN(" + profile + ")";
    }

    public static String uuidToString(ParcelUuid uuid) {
        if (BluetoothUuid.AudioSink.equals(uuid)) {
            return "AudioSink";
        }
        if (BluetoothUuid.AudioSource.equals(uuid)) {
            return "AudioSource";
        }
        if (BluetoothUuid.AdvAudioDist.equals(uuid)) {
            return "AdvAudioDist";
        }
        if (BluetoothUuid.HSP.equals(uuid)) {
            return "HSP";
        }
        if (BluetoothUuid.HSP_AG.equals(uuid)) {
            return "HSP_AG";
        }
        if (BluetoothUuid.Handsfree.equals(uuid)) {
            return "Handsfree";
        }
        if (BluetoothUuid.Handsfree_AG.equals(uuid)) {
            return "Handsfree_AG";
        }
        if (BluetoothUuid.AvrcpController.equals(uuid)) {
            return "AvrcpController";
        }
        if (BluetoothUuid.AvrcpTarget.equals(uuid)) {
            return "AvrcpTarget";
        }
        if (BluetoothUuid.ObexObjectPush.equals(uuid)) {
            return "ObexObjectPush";
        }
        if (BluetoothUuid.Hid.equals(uuid)) {
            return "Hid";
        }
        if (BluetoothUuid.Hogp.equals(uuid)) {
            return "Hogp";
        }
        if (BluetoothUuid.PANU.equals(uuid)) {
            return "PANU";
        }
        if (BluetoothUuid.NAP.equals(uuid)) {
            return "NAP";
        }
        if (BluetoothUuid.BNEP.equals(uuid)) {
            return "BNEP";
        }
        if (BluetoothUuid.PBAP_PSE.equals(uuid)) {
            return "PBAP_PSE";
        }
        if (BluetoothUuid.MAP.equals(uuid)) {
            return "MAP";
        }
        if (BluetoothUuid.MNS.equals(uuid)) {
            return "MNS";
        }
        if (BluetoothUuid.MAS.equals(uuid)) {
            return "MAS";
        }
        if (uuid != null) {
            return uuid.toString();
        }
        return null;
    }

    public static String connectionStateToString(int connectionState) {
        return connectionState == 0 ? "STATE_DISCONNECTED" : connectionState == 2 ? "STATE_CONNECTED" : connectionState == 3 ? "STATE_DISCONNECTING" : connectionState == 1 ? "STATE_CONNECTING" : "ERROR";
    }

    public static String deviceToString(BluetoothDevice device) {
        if (device == null) {
            return null;
        }
        return device.getAddress() + '[' + device.getAliasName() + ']';
    }

    public static String uuidsToString(BluetoothDevice device) {
        ParcelUuid[] ids;
        if (device == null || (ids = device.getUuids()) == null) {
            return null;
        }
        String[] tokens = new String[ids.length];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = uuidToString(ids[i]);
        }
        return TextUtils.join(",", tokens);
    }

    public static int uuidToProfile(ParcelUuid uuid) {
        if (BluetoothUuid.AudioSink.equals(uuid) || BluetoothUuid.AdvAudioDist.equals(uuid)) {
            return 2;
        }
        if (!BluetoothUuid.HSP.equals(uuid) && !BluetoothUuid.Handsfree.equals(uuid)) {
            if (!BluetoothUuid.MAP.equals(uuid) && !BluetoothUuid.MNS.equals(uuid) && !BluetoothUuid.MAS.equals(uuid)) {
                if (BluetoothUuid.AvrcpController.equals(uuid)) {
                    return 11;
                }
                if (!BluetoothUuid.Hid.equals(uuid) && !BluetoothUuid.Hogp.equals(uuid)) {
                    return BluetoothUuid.NAP.equals(uuid) ? 5 : 0;
                }
                return 4;
            }
            return 9;
        }
        return 1;
    }

    public static Profile getProfile(BluetoothProfile p) {
        if (p instanceof BluetoothA2dp) {
            return newProfile((BluetoothA2dp) p);
        }
        if (p instanceof BluetoothHeadset) {
            return newProfile((BluetoothHeadset) p);
        }
        if (p instanceof BluetoothA2dpSink) {
            return newProfile((BluetoothA2dpSink) p);
        }
        if (p instanceof BluetoothHeadsetClient) {
            return newProfile((BluetoothHeadsetClient) p);
        }
        if (p instanceof BluetoothInputDevice) {
            return newProfile((BluetoothInputDevice) p);
        }
        if (p instanceof BluetoothMap) {
            return newProfile((BluetoothMap) p);
        }
        if (p instanceof BluetoothPan) {
            return newProfile((BluetoothPan) p);
        }
        return null;
    }

    private static Profile newProfile(final BluetoothA2dp a2dp) {
        return new Profile() {
            @Override
            public boolean connect(BluetoothDevice device) {
                return a2dp.connect(device);
            }

            @Override
            public boolean disconnect(BluetoothDevice device) {
                return a2dp.disconnect(device);
            }
        };
    }

    private static Profile newProfile(final BluetoothHeadset headset) {
        return new Profile() {
            @Override
            public boolean connect(BluetoothDevice device) {
                return headset.connect(device);
            }

            @Override
            public boolean disconnect(BluetoothDevice device) {
                return headset.disconnect(device);
            }
        };
    }

    private static Profile newProfile(final BluetoothA2dpSink sink) {
        return new Profile() {
            @Override
            public boolean connect(BluetoothDevice device) {
                return sink.connect(device);
            }

            @Override
            public boolean disconnect(BluetoothDevice device) {
                return sink.disconnect(device);
            }
        };
    }

    private static Profile newProfile(final BluetoothHeadsetClient client) {
        return new Profile() {
            @Override
            public boolean connect(BluetoothDevice device) {
                return client.connect(device);
            }

            @Override
            public boolean disconnect(BluetoothDevice device) {
                return client.disconnect(device);
            }
        };
    }

    private static Profile newProfile(final BluetoothInputDevice input) {
        return new Profile() {
            @Override
            public boolean connect(BluetoothDevice device) {
                return input.connect(device);
            }

            @Override
            public boolean disconnect(BluetoothDevice device) {
                return input.disconnect(device);
            }
        };
    }

    private static Profile newProfile(final BluetoothMap map) {
        return new Profile() {
            @Override
            public boolean connect(BluetoothDevice device) {
                return map.connect(device);
            }

            @Override
            public boolean disconnect(BluetoothDevice device) {
                return map.disconnect(device);
            }
        };
    }

    private static Profile newProfile(final BluetoothPan pan) {
        return new Profile() {
            @Override
            public boolean connect(BluetoothDevice device) {
                return pan.connect(device);
            }

            @Override
            public boolean disconnect(BluetoothDevice device) {
                return pan.disconnect(device);
            }
        };
    }
}
