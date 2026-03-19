package com.android.server.telecom;

import android.net.Uri;
import android.telecom.ParcelableCall;
import android.telecom.ParcelableRttCall;
import com.mediatek.server.telecom.ext.ExtensionManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParcelableCallUtils {
    private static final int[] CONNECTION_TO_CALL_CAPABILITY = {1, 1, 2, 2, 4, 4, 8, 8, 32, 32, 64, 64, 128, 128, 256, 256, 512, 512, 768, 768, 1024, 1024, 2048, 2048, 3072, 3072, 4096, 4096, 8192, 8192, 524288, 524288, 1048576, 1048576, 4194304, 2097152, 8388608, 4194304, 16777216, 8388608, 33554432, 16777216, 1073741824, 16777216, 268435456, 67108864, 134217728, 33554432, 536870912, 134217728, Integer.MIN_VALUE, 268435456};
    private static final int[] CONNECTION_TO_CALL_PROPERTIES = {4, 16, 8, 8, 2, 2, 1, 4, 16, 64, 32, 128, 128, 256, 512, 512, 256, 1024, 131072, 262144, 32768, 65536, 65536, 131072, 262144, 2097152};

    public static class Converter {
        public ParcelableCall toParcelableCall(Call call, boolean z, PhoneAccountRegistrar phoneAccountRegistrar) {
            return ParcelableCallUtils.toParcelableCall(call, z, phoneAccountRegistrar, false, false);
        }
    }

    public static ParcelableCall toParcelableCall(Call call, boolean z, PhoneAccountRegistrar phoneAccountRegistrar, boolean z2, boolean z3) {
        return toParcelableCall(call, z, phoneAccountRegistrar, z2, -1, z3);
    }

    public static ParcelableCall toParcelableCall(Call call, boolean z, PhoneAccountRegistrar phoneAccountRegistrar, boolean z2, int i, boolean z3) {
        boolean z4;
        long j;
        Uri handle;
        String callerDisplayName;
        int parcelableState = i;
        if (parcelableState == -1) {
            parcelableState = getParcelableState(call, z2);
        }
        int i2 = parcelableState;
        int iConvertConnectionToCallCapabilities = convertConnectionToCallCapabilities(call.getConnectionCapabilities());
        int iConvertConnectionToCallProperties = convertConnectionToCallProperties(call.getConnectionProperties());
        int supportedAudioRoutes = call.getSupportedAudioRoutes();
        if (call.isConference()) {
            iConvertConnectionToCallProperties |= 1;
        }
        if (call.isWorkCall()) {
            iConvertConnectionToCallProperties |= 32;
        }
        int gttProperties = iConvertConnectionToCallProperties | call.getGttEventExt().getGttProperties() | call.getRttEventExt().getRttProperties();
        if (phoneAccountRegistrar == null || !phoneAccountRegistrar.isUserSelectedSmsPhoneAccount(call.getTargetPhoneAccount())) {
            z4 = false;
        } else {
            z4 = true;
        }
        if (call.isRespondViaSmsCapable() && z4) {
            iConvertConnectionToCallCapabilities |= 32;
        }
        if (call.isEmergencyCall()) {
            iConvertConnectionToCallCapabilities = removeCapability(iConvertConnectionToCallCapabilities, 64);
        }
        if (i2 == 1) {
            iConvertConnectionToCallCapabilities = removeCapability(removeCapability(iConvertConnectionToCallCapabilities, 768), 3072);
        }
        int iBuildCallCapabilities = iConvertConnectionToCallCapabilities | ExtensionManager.getCallMgrExt().buildCallCapabilities(call.isRespondViaSmsCapable());
        Call parentCall = call.getParentCall();
        String id = parentCall != null ? parentCall.getId() : null;
        long connectTimeMillis = call.getConnectTimeMillis();
        List<Call> childCalls = call.getChildCalls();
        ArrayList arrayList = new ArrayList();
        if (!childCalls.isEmpty()) {
            long jMin = Long.MAX_VALUE;
            for (Call call2 : childCalls) {
                if (call2.getConnectTimeMillis() > 0) {
                    jMin = Math.min(call2.getConnectTimeMillis(), jMin);
                }
                arrayList.add(call2.getId());
            }
            j = (jMin == Long.MAX_VALUE || (connectTimeMillis != 0 && jMin >= connectTimeMillis)) ? connectTimeMillis : jMin;
        }
        if (call.getHandlePresentation() == 1) {
            handle = call.getHandle();
        } else {
            handle = null;
        }
        if (call.getCallerDisplayNamePresentation() == 1) {
            callerDisplayName = call.getCallerDisplayName();
        } else {
            callerDisplayName = null;
        }
        List<Call> conferenceableCalls = call.getConferenceableCalls();
        ArrayList arrayList2 = new ArrayList(conferenceableCalls.size());
        Iterator<Call> it = conferenceableCalls.iterator();
        while (it.hasNext()) {
            arrayList2.add(it.next().getId());
        }
        return new ParcelableCall(call.getId(), i2, call.getDisconnectCause(), call.getCannedSmsResponses(), iBuildCallCapabilities, gttProperties, supportedAudioRoutes, j, handle, call.getHandlePresentation(), callerDisplayName, call.getCallerDisplayNamePresentation(), call.getGatewayInfo(), call.getTargetPhoneAccount(), z, z ? call.getVideoProvider() : null, z3, z3 ? getParcelableRttCall(call) : null, id, arrayList, call.getStatusHints(), call.getVideoState(), arrayList2, call.getIntentExtras(), call.getExtras(), call.getCreationTimeMillis());
    }

    private static int getParcelableState(Call call, boolean z) {
        int i;
        switch (call.getState()) {
            case CallState.NEW:
            default:
                i = 0;
                break;
            case 1:
                i = 9;
                break;
            case CallState.SELECT_PHONE_ACCOUNT:
                i = 8;
                break;
            case CallState.DIALING:
                i = 1;
                break;
            case CallState.RINGING:
                i = 2;
                break;
            case CallState.ACTIVE:
                i = 4;
                break;
            case CallState.ON_HOLD:
                i = 3;
                break;
            case CallState.DISCONNECTED:
            case CallState.ABORTED:
                i = 7;
                break;
            case 9:
                i = 10;
                break;
            case CallState.PULLING:
                if (z) {
                    i = 11;
                    break;
                }
                break;
        }
        if (!call.isLocallyDisconnecting() || i == 7) {
            return i;
        }
        return 10;
    }

    private static int convertConnectionToCallCapabilities(int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < CONNECTION_TO_CALL_CAPABILITY.length; i3 += 2) {
            if ((CONNECTION_TO_CALL_CAPABILITY[i3] & i) == CONNECTION_TO_CALL_CAPABILITY[i3]) {
                i2 |= CONNECTION_TO_CALL_CAPABILITY[i3 + 1];
            }
        }
        return i2;
    }

    private static int convertConnectionToCallProperties(int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < CONNECTION_TO_CALL_PROPERTIES.length; i3 += 2) {
            if ((CONNECTION_TO_CALL_PROPERTIES[i3] & i) == CONNECTION_TO_CALL_PROPERTIES[i3]) {
                i2 |= CONNECTION_TO_CALL_PROPERTIES[i3 + 1];
            }
        }
        return i2;
    }

    private static int removeCapability(int i, int i2) {
        return i & (~i2);
    }

    private static ParcelableRttCall getParcelableRttCall(Call call) {
        if (!call.isRttCall()) {
            return null;
        }
        return new ParcelableRttCall(call.getRttMode(), call.getInCallToCsRttPipeForInCall(), call.getCsToInCallRttPipeForInCall());
    }
}
