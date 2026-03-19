package com.android.server.location;

import android.content.Context;
import android.hardware.contexthub.V1_0.ContextHub;
import android.hardware.contexthub.V1_0.ContextHubMsg;
import android.hardware.contexthub.V1_0.HubAppInfo;
import android.hardware.contexthub.V1_0.NanoAppBinary;
import android.hardware.location.ContextHubInfo;
import android.hardware.location.NanoAppMessage;
import android.hardware.location.NanoAppState;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

class ContextHubServiceUtil {
    private static final String ENFORCE_HW_PERMISSION_MESSAGE = "Permission 'android.permission.LOCATION_HARDWARE' not granted to access ContextHub Hardware";
    private static final String HARDWARE_PERMISSION = "android.permission.LOCATION_HARDWARE";
    private static final String TAG = "ContextHubServiceUtil";

    ContextHubServiceUtil() {
    }

    static HashMap<Integer, ContextHubInfo> createContextHubInfoMap(List<ContextHub> list) {
        HashMap<Integer, ContextHubInfo> map = new HashMap<>();
        for (ContextHub contextHub : list) {
            map.put(Integer.valueOf(contextHub.hubId), new ContextHubInfo(contextHub));
        }
        return map;
    }

    static void copyToByteArrayList(byte[] bArr, ArrayList<Byte> arrayList) {
        arrayList.clear();
        arrayList.ensureCapacity(bArr.length);
        for (byte b : bArr) {
            arrayList.add(Byte.valueOf(b));
        }
    }

    static byte[] createPrimitiveByteArray(ArrayList<Byte> arrayList) {
        byte[] bArr = new byte[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            bArr[i] = arrayList.get(i).byteValue();
        }
        return bArr;
    }

    static int[] createPrimitiveIntArray(Collection<Integer> collection) {
        int[] iArr = new int[collection.size()];
        Iterator<Integer> it = collection.iterator();
        int i = 0;
        while (it.hasNext()) {
            iArr[i] = it.next().intValue();
            i++;
        }
        return iArr;
    }

    static NanoAppBinary createHidlNanoAppBinary(android.hardware.location.NanoAppBinary nanoAppBinary) {
        NanoAppBinary nanoAppBinary2 = new NanoAppBinary();
        nanoAppBinary2.appId = nanoAppBinary.getNanoAppId();
        nanoAppBinary2.appVersion = nanoAppBinary.getNanoAppVersion();
        nanoAppBinary2.flags = nanoAppBinary.getFlags();
        nanoAppBinary2.targetChreApiMajorVersion = nanoAppBinary.getTargetChreApiMajorVersion();
        nanoAppBinary2.targetChreApiMinorVersion = nanoAppBinary.getTargetChreApiMinorVersion();
        try {
            copyToByteArrayList(nanoAppBinary.getBinaryNoHeader(), nanoAppBinary2.customBinary);
        } catch (IndexOutOfBoundsException e) {
            Log.w(TAG, e.getMessage());
        } catch (NullPointerException e2) {
            Log.w(TAG, "NanoApp binary was null");
        }
        return nanoAppBinary2;
    }

    static List<NanoAppState> createNanoAppStateList(List<HubAppInfo> list) {
        ArrayList arrayList = new ArrayList();
        for (HubAppInfo hubAppInfo : list) {
            arrayList.add(new NanoAppState(hubAppInfo.appId, hubAppInfo.version, hubAppInfo.enabled));
        }
        return arrayList;
    }

    static ContextHubMsg createHidlContextHubMessage(short s, NanoAppMessage nanoAppMessage) {
        ContextHubMsg contextHubMsg = new ContextHubMsg();
        contextHubMsg.appName = nanoAppMessage.getNanoAppId();
        contextHubMsg.hostEndPoint = s;
        contextHubMsg.msgType = nanoAppMessage.getMessageType();
        copyToByteArrayList(nanoAppMessage.getMessageBody(), contextHubMsg.msg);
        return contextHubMsg;
    }

    static NanoAppMessage createNanoAppMessage(ContextHubMsg contextHubMsg) {
        return NanoAppMessage.createMessageFromNanoApp(contextHubMsg.appName, contextHubMsg.msgType, createPrimitiveByteArray(contextHubMsg.msg), contextHubMsg.hostEndPoint == -1);
    }

    static void checkPermissions(Context context) {
        context.enforceCallingPermission(HARDWARE_PERMISSION, ENFORCE_HW_PERMISSION_MESSAGE);
    }

    static int toTransactionResult(int i) {
        if (i == 0) {
            return 0;
        }
        if (i != 5) {
            switch (i) {
                case 2:
                    return 2;
                case 3:
                    return 3;
                default:
                    return 1;
            }
        }
        return 4;
    }
}
