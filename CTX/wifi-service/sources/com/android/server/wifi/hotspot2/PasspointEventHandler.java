package com.android.server.wifi.hotspot2;

import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PasspointEventHandler {
    private final Callbacks mCallbacks;
    private final WifiNative mSupplicantHook;

    public interface Callbacks {
        void onANQPResponse(long j, Map<Constants.ANQPElementType, ANQPElement> map);

        void onIconResponse(long j, String str, byte[] bArr);

        void onWnmFrameReceived(WnmData wnmData);
    }

    public PasspointEventHandler(WifiNative wifiNative, Callbacks callbacks) {
        this.mSupplicantHook = wifiNative;
        this.mCallbacks = callbacks;
    }

    public boolean requestANQP(long j, List<Constants.ANQPElementType> list) {
        Pair<Set<Integer>, Set<Integer>> pairBuildAnqpIdSet = buildAnqpIdSet(list);
        if (j == 0 || pairBuildAnqpIdSet == null) {
            return false;
        }
        if (!this.mSupplicantHook.requestAnqp(this.mSupplicantHook.getClientInterfaceName(), Utils.macToString(j), (Set) pairBuildAnqpIdSet.first, (Set) pairBuildAnqpIdSet.second)) {
            Log.d(Utils.hs2LogTag(getClass()), "ANQP failed on " + Utils.macToString(j));
            return false;
        }
        Log.d(Utils.hs2LogTag(getClass()), "ANQP initiated on " + Utils.macToString(j));
        return true;
    }

    public boolean requestIcon(long j, String str) {
        if (j == 0 || str == null) {
            return false;
        }
        return this.mSupplicantHook.requestIcon(this.mSupplicantHook.getClientInterfaceName(), Utils.macToString(j), str);
    }

    public void notifyANQPDone(AnqpEvent anqpEvent) {
        if (anqpEvent == null) {
            return;
        }
        this.mCallbacks.onANQPResponse(anqpEvent.getBssid(), anqpEvent.getElements());
    }

    public void notifyIconDone(IconEvent iconEvent) {
        if (iconEvent == null) {
            return;
        }
        this.mCallbacks.onIconResponse(iconEvent.getBSSID(), iconEvent.getFileName(), iconEvent.getData());
    }

    public void notifyWnmFrameReceived(WnmData wnmData) {
        this.mCallbacks.onWnmFrameReceived(wnmData);
    }

    private static Pair<Set<Integer>, Set<Integer>> buildAnqpIdSet(List<Constants.ANQPElementType> list) {
        HashSet hashSet = new HashSet();
        HashSet hashSet2 = new HashSet();
        for (Constants.ANQPElementType aNQPElementType : list) {
            Integer aNQPElementID = Constants.getANQPElementID(aNQPElementType);
            if (aNQPElementID != null) {
                hashSet.add(aNQPElementID);
            } else {
                hashSet2.add(Constants.getHS20ElementID(aNQPElementType));
            }
        }
        return Pair.create(hashSet, hashSet2);
    }
}
