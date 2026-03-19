package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.backup.BackupManagerConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class HdmiUtils {
    private static final int[] ADDRESS_TO_TYPE = {0, 1, 1, 3, 4, 5, 3, 3, 4, 1, 3, 4, 2, 2, 0};
    private static final String[] DEFAULT_NAMES = {"TV", "Recorder_1", "Recorder_2", "Tuner_1", "Playback_1", "AudioSystem", "Tuner_2", "Tuner_3", "Playback_2", "Recorder_3", "Tuner_4", "Playback_3", "Reserved_1", "Reserved_2", "Secondary_TV"};

    private HdmiUtils() {
    }

    static boolean isValidAddress(int i) {
        return i >= 0 && i <= 14;
    }

    static int getTypeFromAddress(int i) {
        if (isValidAddress(i)) {
            return ADDRESS_TO_TYPE[i];
        }
        return -1;
    }

    static String getDefaultDeviceName(int i) {
        if (isValidAddress(i)) {
            return DEFAULT_NAMES[i];
        }
        return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    }

    static void verifyAddressType(int i, int i2) {
        int typeFromAddress = getTypeFromAddress(i);
        if (typeFromAddress != i2) {
            throw new IllegalArgumentException("Device type missmatch:[Expected:" + i2 + ", Actual:" + typeFromAddress);
        }
    }

    static boolean checkCommandSource(HdmiCecMessage hdmiCecMessage, int i, String str) {
        int source = hdmiCecMessage.getSource();
        if (source != i) {
            Slog.w(str, "Invalid source [Expected:" + i + ", Actual:" + source + "]");
            return false;
        }
        return true;
    }

    static boolean parseCommandParamSystemAudioStatus(HdmiCecMessage hdmiCecMessage) {
        return hdmiCecMessage.getParams()[0] == 1;
    }

    static boolean isAudioStatusMute(HdmiCecMessage hdmiCecMessage) {
        return (hdmiCecMessage.getParams()[0] & 128) == 128;
    }

    static int getAudioStatusVolume(HdmiCecMessage hdmiCecMessage) {
        int i = hdmiCecMessage.getParams()[0] & 127;
        if (i < 0 || 100 < i) {
            return -1;
        }
        return i;
    }

    static List<Integer> asImmutableList(int[] iArr) {
        ArrayList arrayList = new ArrayList(iArr.length);
        for (int i : iArr) {
            arrayList.add(Integer.valueOf(i));
        }
        return Collections.unmodifiableList(arrayList);
    }

    static int twoBytesToInt(byte[] bArr) {
        return (bArr[1] & 255) | ((bArr[0] & 255) << 8);
    }

    static int twoBytesToInt(byte[] bArr, int i) {
        return (bArr[i + 1] & 255) | ((bArr[i] & 255) << 8);
    }

    static int threeBytesToInt(byte[] bArr) {
        return (bArr[2] & 255) | ((bArr[0] & 255) << 16) | ((bArr[1] & 255) << 8);
    }

    static <T> List<T> sparseArrayToList(SparseArray<T> sparseArray) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < sparseArray.size(); i++) {
            arrayList.add(sparseArray.valueAt(i));
        }
        return arrayList;
    }

    static <T> List<T> mergeToUnmodifiableList(List<T> list, List<T> list2) {
        if (list.isEmpty() && list2.isEmpty()) {
            return Collections.emptyList();
        }
        if (list.isEmpty()) {
            return Collections.unmodifiableList(list2);
        }
        if (list2.isEmpty()) {
            return Collections.unmodifiableList(list);
        }
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(list);
        arrayList.addAll(list2);
        return Collections.unmodifiableList(arrayList);
    }

    static boolean isAffectingActiveRoutingPath(int i, int i2) {
        int i3 = 0;
        while (true) {
            if (i3 > 12) {
                break;
            }
            if (((i2 >> i3) & 15) == 0) {
                i3 += 4;
            } else {
                i2 &= 65520 << i3;
                break;
            }
        }
        if (i2 == 0) {
            return true;
        }
        return isInActiveRoutingPath(i, i2);
    }

    static boolean isInActiveRoutingPath(int i, int i2) {
        int i3;
        for (int i4 = 12; i4 >= 0; i4 -= 4) {
            int i5 = (i >> i4) & 15;
            if (i5 != 0 && (i3 = (i2 >> i4) & 15) != 0) {
                if (i5 != i3) {
                    return false;
                }
            } else {
                return true;
            }
        }
        return true;
    }

    static HdmiDeviceInfo cloneHdmiDeviceInfo(HdmiDeviceInfo hdmiDeviceInfo, int i) {
        return new HdmiDeviceInfo(hdmiDeviceInfo.getLogicalAddress(), hdmiDeviceInfo.getPhysicalAddress(), hdmiDeviceInfo.getPortId(), hdmiDeviceInfo.getDeviceType(), hdmiDeviceInfo.getVendorId(), hdmiDeviceInfo.getDisplayName(), i);
    }
}
