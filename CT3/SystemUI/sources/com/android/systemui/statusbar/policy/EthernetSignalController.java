package com.android.systemui.statusbar.policy;

import android.content.Context;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SignalController;
import java.util.BitSet;

public class EthernetSignalController extends SignalController<SignalController.State, SignalController.IconGroup> {
    public EthernetSignalController(Context context, CallbackHandler callbackHandler, NetworkControllerImpl networkController) {
        super("EthernetSignalController", context, 3, callbackHandler, networkController);
        T t = this.mCurrentState;
        SignalController.IconGroup iconGroup = new SignalController.IconGroup("Ethernet Icons", EthernetIcons.ETHERNET_ICONS, null, AccessibilityContentDescriptions.ETHERNET_CONNECTION_VALUES, 0, 0, 0, 0, AccessibilityContentDescriptions.ETHERNET_CONNECTION_VALUES[0]);
        this.mLastState.iconGroup = iconGroup;
        t.iconGroup = iconGroup;
    }

    @Override
    public void updateConnectivity(BitSet connectedTransports, BitSet validatedTransports) {
        this.mCurrentState.connected = connectedTransports.get(this.mTransportType);
        super.updateConnectivity(connectedTransports, validatedTransports);
    }

    @Override
    public void notifyListeners(NetworkController.SignalCallback callback) {
        boolean ethernetVisible = this.mCurrentState.connected;
        String contentDescription = getStringIfExists(getContentDescription());
        callback.setEthernetIndicators(new NetworkController.IconState(ethernetVisible, getCurrentIconId(), contentDescription));
    }

    @Override
    public SignalController.State cleanState() {
        return new SignalController.State();
    }
}
